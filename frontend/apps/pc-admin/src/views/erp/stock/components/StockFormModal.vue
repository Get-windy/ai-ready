<template>
  <FullScreenDetail
    :visible="open"
    :title="modalTitle"
    :save-loading="loading"
    :show-save-and-new="!isEdit"
    @close="handleFormClose"
    @save="handleOk"
    @save-and-new="handleFormSaveAndNew"
  >
    <a-form ref="formRef" :model="formData" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
      <a-row :gutter="24">
        <a-col :span="12">
          <a-form-item label="单据编号" name="orderNo">
            <a-input size="small" v-model:value="formData.orderNo" placeholder="系统自动生成" :disabled="true" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="仓库" name="warehouseId">
            <a-select size="small" v-model:value="formData.warehouseId" placeholder="请选择仓库">
              <a-select-option v-for="warehouse in warehouseList" :key="warehouse.id" :value="warehouse.id">
                {{ warehouse.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="24">
        <a-col :span="12">
          <a-form-item label="单据日期" name="orderDate">
            <a-date-picker size="small" v-model:value="formData.orderDate" placeholder="请选择日期" style="width: 100%" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="经办人" name="operatorId">
            <a-select size="small" v-model:value="formData.operatorId" placeholder="请选择经办人">
              <a-select-option v-for="user in userList" :key="user.id" :value="user.id">
                {{ user.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="24">
        <a-col :span="12">
          <a-form-item label="关联订单" name="relatedOrderNo">
            <a-input size="small" v-model:value="formData.relatedOrderNo" placeholder="请输入关联订单号" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="供应商/客户" name="partyId" v-if="showPartySelect">
            <a-select size="small" v-model:value="formData.partyId" placeholder="请选择" show-search :filter-option="filterOption">
              <a-select-option v-for="party in partyList" :key="party.id" :value="party.id">
                {{ party.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 21 }">
        <a-textarea size="small" v-model:value="formData.remark" placeholder="请输入备注" :rows="2" :maxlength="500" show-count />
      </a-form-item>

      <a-divider>商品明细</a-divider>

      <div class="product-table-actions">
        <a-button type="primary" size="small" @click="handleAddItem">
          <template #icon><PlusOutlined /></template>
          添加商品
        </a-button>
      </div>

      <VxeTableList
        :columns="itemVxeColumns"
        :data-source="formData.items"
        :pagination="false as any"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #productIdCell="{ record, index }">
          <a-select size="small" v-model:value="record.productId" placeholder="请选择商品" show-search :filter-option="filterOption" style="width: 100%" @change="(val: any) => handleProductChange(val, index)">
            <a-select-option v-for="product in productList" :key="product.id" :value="product.id">
              {{ product.name }} ({{ product.code }})
            </a-select-option>
          </a-select>
        </template>
        <template #batchNoCell="{ record }">
          <a-input size="small" v-model:value="record.batchNo" placeholder="批次号" style="width: 100%" />
        </template>
        <template #quantityCell="{ record }">
          <a-input-number size="small" v-model:value="record.quantity" :min="1" :max="99999" :step="1" style="width: 100%" />
        </template>
        <template #unitPriceCell="{ record }">
          <a-input-number size="small" v-model:value="record.unitPrice" :min="0" :step="0.01" :precision="2" style="width: 100%" />
        </template>
        <template #amountCell="{ record }">
          <span class="amount-text">¥{{ (record.quantity * record.unitPrice).toFixed(2) }}</span>
        </template>
        <template #action="{ index }">
          <a-popconfirm title="确定删除？" @confirm="handleDeleteItem(index)">
            <a class="danger">删除</a>
          </a-popconfirm>
        </template>
      </VxeTableList>

      <div class="amount-summary">
        <a-row :gutter="16">
          <a-col :span="8"><a-statistic title="商品数量" :value="totalQuantity" /></a-col>
          <a-col :span="8"><a-statistic title="商品金额" :value="totalAmount" :precision="2" prefix="¥" /></a-col>
          <a-col :span="8"><a-statistic title="商品种类" :value="formData.items.length" /></a-col>
        </a-row>
      </div>
    </a-form>
  </FullScreenDetail>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick } from 'vue'
import { message, Modal } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'
import { optionsApi } from '@/api/options'

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

interface StockItem {
  id: string
  productId: number | undefined
  productCode: string
  productName: string
  batchNo: string
  quantity: number
  unitPrice: number
  unit: string
  remark: string
}

interface StockFormData {
  orderNo: string
  warehouseId: number | undefined
  orderDate: string
  operatorId: number | undefined
  relatedOrderNo: string
  partyId: number | undefined
  remark: string
  items: StockItem[]
}

const props = defineProps<{
  open: boolean
  type: 'in' | 'out' | 'transfer'
  editData?: any
}>()

const emit = defineEmits<{ (e: 'update:open', val: boolean): void; (e: 'success'): void }>()

const isEdit = computed(() => !!props.editData?.id)
const loading = ref(false)
const formRef = ref<FormInstance>()

// ── 表单脏状态追踪 ──────────────────────────────────
const initialFormSnapshot = ref('')

function saveFormSnapshot() {
  initialFormSnapshot.value = JSON.stringify(formData)
}

const formDirty = computed(() => {
  if (!initialFormSnapshot.value) return false
  const current = JSON.stringify(formData)
  return current !== initialFormSnapshot.value
})

const modalTitle = computed(() => {
  const titles = { in: '入库单', out: '出库单', transfer: '调拨单' }
  return isEdit.value ? `编辑${titles[props.type]}` : `新建${titles[props.type]}`
})

const showPartySelect = computed(() => props.type === 'in' || props.type === 'out')

const formData = reactive<StockFormData>({
  orderNo: '',
  warehouseId: undefined,
  orderDate: dayjs().format('YYYY-MM-DD'),
  operatorId: undefined,
  relatedOrderNo: '',
  partyId: undefined,
  remark: '',
  items: [{ id: '1', productId: undefined, productCode: '', productName: '', batchNo: '', quantity: 1, unitPrice: 0, unit: '', remark: '' }]
})

const formRules: Record<string, any> = {
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  orderDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  operatorId: [{ required: true, message: '请选择经办人', trigger: 'change' }]
}

const itemVxeColumns = [
  { field: 'productId', title: '商品', width: 180, slotName: 'productIdCell' },
  { field: 'productCode', title: '商品编码', width: 100 },
  { field: 'batchNo', title: '批次号', width: 120, slotName: 'batchNoCell' },
  { field: 'quantity', title: '数量', width: 100, slotName: 'quantityCell' },
  { field: 'unitPrice', title: '单价', width: 100, slotName: 'unitPriceCell' },
  { field: 'amount', title: '金额', width: 100, slotName: 'amountCell' },
  { field: 'unit', title: '单位', width: 60 },
  { field: 'action', title: '操作', width: 80, type: 'action' }
]

const warehouseList = ref<any[]>([])
const userList = ref<any[]>([])
const productList = ref<any[]>([])
const partyList = ref<any[]>([])

const totalQuantity = computed(() => formData.items.reduce((sum, item) => sum + item.quantity, 0))
const totalAmount = computed(() => formData.items.reduce((sum, item) => sum + item.quantity * item.unitPrice, 0))

const filterOption = (input: string, option: any) => {
  const text = option.label || option.value?.toString() || ''
  return text.toLowerCase().includes(input.toLowerCase())
}

const handleProductChange = (val: number, index: number) => {
  const product = productList.value.find(p => p.id === val)
  if (product) {
    formData.items[index].productCode = product.code
    formData.items[index].productName = product.name
    formData.items[index].unit = product.unit
    formData.items[index].unitPrice = product.costPrice || 0
  }
}

const handleAddItem = () => {
  formData.items.push({ id: Date.now().toString(), productId: undefined, productCode: '', productName: '', batchNo: '', quantity: 1, unitPrice: 0, unit: '', remark: '' })
}

const handleDeleteItem = (index: number) => {
  if (formData.items.length > 1) {
    formData.items.splice(index, 1)
  } else {
    message.warning('至少保留一条商品明细')
  }
}

const handleOk = async () => {
  try {
    await formRef.value?.validate()
    if (formData.items.some(item => !item.productId)) {
      message.error('请选择所有商品')
      return
    }
    loading.value = true
    // 实际 API 调用，待对接
    message.success(isEdit.value ? '更新成功' : '创建成功')
    emit('success')
    emit('update:open', false)
  } catch (error) {
    console.warn('[库存] 表单验证失败', error)
  } finally {
    loading.value = false
  }
}

const handleFormClose = () => {
  if (formDirty.value) {
    Modal.confirm({
      title: '离开确认',
      content: '当前表单有未保存的更改，确定要关闭吗？',
      okText: '放弃修改',
      okType: 'danger',
      cancelText: '继续编辑',
      onOk: () => {
        emit('update:open', false)
      }
    })
  } else {
    emit('update:open', false)
  }
}

const handleFormSaveAndNew = async () => {
  try {
    await formRef.value?.validate()
    if (formData.items.some(item => !item.productId)) {
      message.error('请选择所有商品')
      return
    }
    loading.value = true
    message.success(isEdit.value ? '更新成功' : '创建成功')
    emit('success')
    // 重置表单以新建
    formData.orderNo = ''
    formData.warehouseId = undefined
    formData.orderDate = dayjs().format('YYYY-MM-DD')
    formData.operatorId = undefined
    formData.relatedOrderNo = ''
    formData.partyId = undefined
    formData.remark = ''
    formData.items = [{ id: '1', productId: undefined, productCode: '', productName: '', batchNo: '', quantity: 1, unitPrice: 0, unit: '', remark: '' }]
    formRef.value?.resetFields()
    nextTick(() => saveFormSnapshot())
  } catch (error) {
    console.warn('[库存] 表单验证失败', error)
  } finally {
    loading.value = false
  }
}

// ── 快捷键 ─────────────────────────────────────────
function handleKeyDown(e: KeyboardEvent) {
  if (!props.open) return
  if (e.ctrlKey && e.key === 'n') {
    e.preventDefault()
    debounceClick('addItem', handleAddItem)
  }
}

import { onMounted, onUnmounted } from 'vue'

onMounted(() => {
  window.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
})

const loadOptions = async () => {
  warehouseList.value = [{ id: 1, name: '北京仓库' }, { id: 2, name: '上海仓库' }]
  partyList.value = [{ id: 1, name: '供应商A' }, { id: 2, name: '客户B' }]
  try {
    const [users, products] = await Promise.all([
      optionsApi.getUsers(),
      optionsApi.getProducts()
    ])
    userList.value = Array.isArray(users) ? users : []
    productList.value = Array.isArray(products) ? products : []
  } catch (err) {
    // 默认空列表
  }
}

watch(() => props.open, (val) => {
  if (val) {
    loadOptions()
    if (props.editData) {
      Object.assign(formData, props.editData)
    } else {
      const prefix = { in: 'IN', out: 'OUT', transfer: 'TR' }
      formData.orderNo = prefix[props.type] + dayjs().format('YYYYMMDDHHmmss')
    }
    nextTick(() => saveFormSnapshot())
  }
})
</script>

<style scoped>
.product-table-actions { margin-bottom: 16px; }
.amount-summary { margin-top: 16px; padding: 16px; background: #f5f5f5; border-radius: 4px; }
.amount-text { font-weight: 600; color: #1890ff; }
.danger { color: #ff4d4f; }

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
