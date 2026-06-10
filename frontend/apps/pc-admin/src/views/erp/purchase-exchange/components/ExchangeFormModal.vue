<template>
  <FullScreenDetail
    :visible="open"
    :title="isEdit ? '编辑换货单' : '新建换货单'"
    :save-loading="submitting"
    :show-save-and-new="!isEdit"
    @close="handleFormClose"
    @save="handleSubmit"
    @save-and-new="handleFormSaveAndNew"
  >
    <a-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      layout="vertical"
    >
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="原采购订单" name="originalOrderId">
            <a-select
              size="small"
              v-model:value="formData.originalOrderId"
              placeholder="请选择原采购订单"
              show-search
              :filter-option="false"
              @search="handleOrderSearch"
              @change="handleOrderChange"
            >
              <a-select-option
                v-for="order in orderOptions"
                :key="order.id"
                :value="order.id"
              >
                {{ order.orderNo }} - {{ order.supplierName }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="换货日期" name="exchangeDate">
            <a-date-picker size="small"
              v-model:value="formData.exchangeDate"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="换货类型" name="exchangeType">
            <a-select size="small" v-model:value="formData.exchangeType" placeholder="请选择换货类型">
              <a-select-option :value="1">质量问题</a-select-option>
              <a-select-option :value="2">规格不符</a-select-option>
              <a-select-option :value="3">数量错误</a-select-option>
              <a-select-option :value="4">其他</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="供应商">
            <a-input size="small" v-model:value="selectedOrder.supplierName" disabled />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="换货原因" name="exchangeReason">
        <a-textarea size="small"
          v-model:value="formData.exchangeReason"
          :rows="3"
          placeholder="请输入换货原因"
        />
      </a-form-item>

      <a-form-item label="备注" name="remark">
        <a-textarea size="small"
          v-model:value="formData.remark"
          :rows="2"
          placeholder="请输入备注信息"
        />
      </a-form-item>

      <!-- 换货商品明细 -->
      <a-divider>换货商品明细</a-divider>

      <VxeTableList
        :columns="itemVxeColumns"
        :data-source="formData.items"
        :pagination="false"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #exchangeQuantityCell="{ record }">
            <a-input-number size="small"
              v-model:value="record.exchangeQuantity"
              :min="1"
              :max="record.originalQuantity"
              style="width: 100px"
              @change="calculateTotal"
            />
        </template>
        <template #exchangePriceCell="{ record }">
            <a-input-number size="small"
              v-model:value="record.exchangePrice"
              :min="0"
              :precision="2"
              style="width: 100px"
              @change="calculateTotal"
            />
        </template>
        <template #subtotalCell="{ record }">
            ¥{{ (record.exchangeQuantity * record.exchangePrice).toFixed(2) }}
        </template>
        <template #action="{ index }">
            <a-button type="link" danger @click="removeItem(index)">删除</a-button>
        </template>
      </VxeTableList>

      <div class="total-amount">
        <span>换货总金额：</span>
        <span class="amount">¥{{ totalAmount.toFixed(2) }}</span>
      </div>
    </a-form>
  </FullScreenDetail>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick } from 'vue'
import { message, Modal } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { FullScreenDetail } from '@/components'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import type { FormInstance } from 'ant-design-vue'
import {
  purchaseExchangeApi,
  type PurchaseExchange,
  type CreateExchangeRequest
} from '@/api/purchase-exchange'
import { purchaseOrderApi, type PurchaseOrder } from '@/api/purchase'
import { useUserStore } from '@/stores/user'

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

interface FormItem {
  id?: number
  originalItemId: number
  productId: number
  productName: string
  productCode: string
  originalQuantity: number
  exchangeQuantity: number
  originalPrice: number
  exchangePrice: number
  unit: string
  warehouseId: number
  warehouseName: string
}

interface Props {
  open: boolean
  record: PurchaseExchange | null
}

const userStore = useUserStore()
const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
  (e: 'success'): void
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)
const orderOptions = ref<PurchaseOrder[]>([])
const selectedOrder = reactive({
  id: 0,
  supplierName: ''
})

const isEdit = computed(() => !!props.record)

// ── 表单脏状态追踪 ──────────────────────────────────
const initialFormSnapshot = ref('')

function saveFormSnapshot() {
  initialFormSnapshot.value = JSON.stringify(formData, (key, value) => {
    if (key === 'exchangeDate' && value && typeof (value as any).format === 'function') {
      return (value as any).format('YYYY-MM-DD')
    }
    return value
  })
}

const formDirty = computed(() => {
  if (!initialFormSnapshot.value) return false
  const current = JSON.stringify(formData, (key, value) => {
    if (key === 'exchangeDate' && value && typeof (value as any).format === 'function') {
      return (value as any).format('YYYY-MM-DD')
    }
    return value
  })
  return current !== initialFormSnapshot.value
})

const formData = reactive<{
  id?: number
  originalOrderId: number | undefined
  exchangeDate: Dayjs
  exchangeType: number
  exchangeReason: string
  remark: string
  items: FormItem[]
}>({
  originalOrderId: undefined,
  exchangeDate: dayjs(),
  exchangeType: 1,
  exchangeReason: '',
  remark: '',
  items: []
})

const totalAmount = computed(() => {
  return formData.items.reduce((sum, item) => {
    return sum + item.exchangeQuantity * item.exchangePrice
  }, 0)
})

const formRules = {
  originalOrderId: [{ required: true, message: '请选择原采购订单' }],
  exchangeDate: [{ required: true, message: '请选择换货日期' }],
  exchangeType: [{ required: true, message: '请选择换货类型' }],
  exchangeReason: [{ required: true, message: '请输入换货原因' }],
  items: [{ required: true, message: '请添加换货商品', validator: () => formData.items.length > 0 }]
}

const itemVxeColumns = [
  { field: 'productName', title: '商品名称' },
  { field: 'productCode', title: '商品编码' },
  { field: 'unit', title: '单位', width: 80 },
  { field: 'originalQuantity', title: '原数量', width: 100 },
  { field: 'exchangeQuantity', title: '换货数量', width: 120, slotName: 'exchangeQuantityCell' },
  { field: 'originalPrice', title: '原单价', width: 100 },
  { field: 'exchangePrice', title: '换货单价', width: 120, slotName: 'exchangePriceCell' },
  { field: 'subtotal', title: '小计', width: 100, slotName: 'subtotalCell' },
  { field: 'action', title: '操作', width: 80, type: 'action' }
]

// 搜索采购订单
const handleOrderSearch = async (keyword: string) => {
  if (!keyword) return
  try {
    const res = await purchaseOrderApi.page({
      current: 1,
      size: 20,
      tenantId: userStore.tenantId,
      orderNo: keyword
    })
    orderOptions.value = res.data?.records || []
  } catch (error) {
    console.warn('[采购换货] 搜索采购订单失败', error)
  }
}

// 选择采购订单
const handleOrderChange = async (orderId: number) => {
  const order = orderOptions.value.find(o => o.id === orderId)
  if (order) {
    selectedOrder.id = order.id
    selectedOrder.supplierName = order.supplierName

    // 加载订单明细
    try {
      const itemsRes = await purchaseOrderApi.getItems(orderId)
      const items = (itemsRes as any).data || itemsRes || []
      formData.items = items.map((item: any) => ({
        originalItemId: item.id,
        productId: item.productId || 0,
        productName: item.materialName || '',
        productCode: item.productCode || '',
        originalQuantity: item.quantity || 0,
        exchangeQuantity: Math.min(1, item.quantity || 1),
        originalPrice: item.unitPrice ? Number(item.unitPrice) : 0,
        exchangePrice: item.unitPrice ? Number(item.unitPrice) : 0,
        unit: item.unit || '',
        warehouseId: 0,
        warehouseName: ''
      }))
    } catch (error) {
      message.error('获取订单明细失败')
    }
  }
}

// 删除明细项
const removeItem = (index: number) => {
  formData.items.splice(index, 1)
  calculateTotal()
}

// 计算总金额
const calculateTotal = () => {
  // 总金额通过 computed 自动计算
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value?.validate()

    if (formData.items.length === 0) {
      message.error('请添加换货商品')
      return
    }

    submitting.value = true

    const submitData: CreateExchangeRequest = {
      originalOrderId: formData.originalOrderId!,
      exchangeDate: formData.exchangeDate.format('YYYY-MM-DD'),
      exchangeReason: formData.exchangeReason,
      exchangeType: formData.exchangeType,
      remark: formData.remark,
      items: formData.items.map(item => ({
        originalItemId: item.originalItemId,
        productId: item.productId,
        exchangeQuantity: item.exchangeQuantity,
        exchangePrice: item.exchangePrice,
        warehouseId: item.warehouseId,
        remark: ''
      }))
    }

    if (isEdit.value && props.record) {
      await purchaseExchangeApi.update(props.record.id, submitData)
      message.success('更新成功')
    } else {
      await purchaseExchangeApi.create(submitData)
      message.success('创建成功')
    }

    emit('success')
    emit('update:open', false)
  } catch (error) {
    console.warn('[采购换货] 提交失败', error)
    message.error('提交失败，请检查表单')
  } finally {
    submitting.value = false
  }
}

// 保存并新增
const handleFormSaveAndNew = async () => {
  try {
    await formRef.value?.validate()

    if (formData.items.length === 0) {
      message.error('请添加换货商品')
      return
    }

    submitting.value = true

    const submitData: CreateExchangeRequest = {
      originalOrderId: formData.originalOrderId!,
      exchangeDate: formData.exchangeDate.format('YYYY-MM-DD'),
      exchangeReason: formData.exchangeReason,
      exchangeType: formData.exchangeType,
      remark: formData.remark,
      items: formData.items.map(item => ({
        originalItemId: item.originalItemId,
        productId: item.productId,
        exchangeQuantity: item.exchangeQuantity,
        exchangePrice: item.exchangePrice,
        warehouseId: item.warehouseId,
        remark: ''
      }))
    }

    if (isEdit.value && props.record) {
      await purchaseExchangeApi.update(props.record.id, submitData)
      message.success('更新成功')
    } else {
      await purchaseExchangeApi.create(submitData)
      message.success('创建成功')
    }

    emit('success')
    resetForm()
    nextTick(() => saveFormSnapshot())
  } catch (error) {
    console.warn('[采购换货] 提交失败', error)
    message.error('提交失败，请检查表单')
  } finally {
    submitting.value = false
  }
}

// 关闭（含脏检查）
const handleFormClose = () => {
  if (formDirty.value) {
    Modal.confirm({
      title: '离开确认',
      content: '当前表单有未保存的更改，确定要关闭吗？',
      okText: '放弃修改',
      okType: 'danger',
      cancelText: '继续编辑',
      onOk: () => {
        resetForm()
        emit('update:open', false)
      }
    })
  } else {
    resetForm()
    emit('update:open', false)
  }
}

// 重置表单
const resetForm = () => {
  formData.originalOrderId = undefined
  formData.exchangeDate = dayjs()
  formData.exchangeType = 1
  formData.exchangeReason = ''
  formData.remark = ''
  formData.items = []
  selectedOrder.id = 0
  selectedOrder.supplierName = ''
  formRef.value?.resetFields()
}

// 监听编辑数据
watch(() => props.record, async (record) => {
  if (record) {
    formData.id = record.id
    formData.originalOrderId = record.originalOrderId
    formData.exchangeDate = dayjs(record.exchangeDate)
    formData.exchangeType = record.exchangeType
    formData.exchangeReason = record.exchangeReason
    formData.remark = record.remark
    selectedOrder.supplierName = record.supplierName
    // 加载明细数据
    await loadExchangeItems(record.id)
    nextTick(() => saveFormSnapshot())
  } else {
    resetForm()
  }
}, { immediate: true })

// 加载换货单明细
const loadExchangeItems = async (exchangeId: number) => {
  try {
    const res = await purchaseExchangeApi.getItems(exchangeId)
    if (res.data) {
      formData.items = res.data.map(item => ({
        id: item.id,
        originalItemId: item.originalItemId,
        productId: item.productId,
        productName: item.productName,
        productCode: item.productCode,
        originalQuantity: item.originalQuantity,
        exchangeQuantity: item.exchangeQuantity,
        originalPrice: item.originalPrice,
        exchangePrice: item.exchangePrice,
        unit: item.unit,
        warehouseId: item.warehouseId,
        warehouseName: item.warehouseName
      }))
    }
  } catch (error) {
    message.error('加载明细失败')
  }
}

// 打开时保存快照（新建模式）
watch(() => props.open, (val) => {
  if (val && !props.record) {
    nextTick(() => saveFormSnapshot())
  }
})
</script>

<style scoped>
.total-amount {
  margin-top: 16px;
  text-align: right;
  font-size: 16px;
}

.total-amount .amount {
  color: #f5222d;
  font-weight: bold;
  font-size: 20px;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}
</style>
