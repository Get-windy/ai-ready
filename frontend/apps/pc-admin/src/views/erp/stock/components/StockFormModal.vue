<template>
  <a-modal
    v-model:open="visible"
    :title="modalTitle"
    :width="800"
    :confirm-loading="loading"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <a-form ref="formRef" :model="formData" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
      <a-row :gutter="24">
        <a-col :span="12">
          <a-form-item label="单据编号" name="orderNo">
            <a-input v-model:value="formData.orderNo" placeholder="系统自动生成" :disabled="true" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="仓库" name="warehouseId">
            <a-select v-model:value="formData.warehouseId" placeholder="请选择仓库">
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
            <a-date-picker v-model:value="formData.orderDate" placeholder="请选择日期" style="width: 100%" format="YYYY-MM-DD" value-format="YYYY-MM-DD" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="经办人" name="operatorId">
            <a-select v-model:value="formData.operatorId" placeholder="请选择经办人">
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
            <a-input v-model:value="formData.relatedOrderNo" placeholder="请输入关联订单号" allow-clear />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="供应商/客户" name="partyId" v-if="showPartySelect">
            <a-select v-model:value="formData.partyId" placeholder="请选择" show-search :filter-option="filterOption">
              <a-select-option v-for="party in partyList" :key="party.id" :value="party.id">
                {{ party.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 21 }">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" :maxlength="500" show-count />
      </a-form-item>

      <a-divider>商品明细</a-divider>

      <div class="product-table-actions">
        <a-button type="primary" size="small" @click="handleAddItem">
          <template #icon><PlusOutlined /></template>
          添加商品
        </a-button>
      </div>

      <a-table :columns="itemColumns" :data-source="formData.items" :pagination="false" size="small" bordered row-key="id">
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'productId'">
            <a-select v-model:value="record.productId" placeholder="请选择商品" show-search :filter-option="filterOption" style="width: 100%" @change="(val) => handleProductChange(val, index)">
              <a-select-option v-for="product in productList" :key="product.id" :value="product.id">
                {{ product.name }} ({{ product.code }})
              </a-select-option>
            </a-select>
          </template>
          <template v-else-if="column.key === 'batchNo'">
            <a-input v-model:value="record.batchNo" placeholder="批次号" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'quantity'">
            <a-input-number v-model:value="record.quantity" :min="1" :max="99999" :step="1" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'unitPrice'">
            <a-input-number v-model:value="record.unitPrice" :min="0" :step="0.01" :precision="2" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'amount'">
            <span class="amount-text">¥{{ (record.quantity * record.unitPrice).toFixed(2) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-popconfirm title="确定删除？" @confirm="handleDeleteItem(index)">
              <a class="danger">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>

      <div class="amount-summary">
        <a-row :gutter="16">
          <a-col :span="8"><a-statistic title="商品数量" :value="totalQuantity" /></a-col>
          <a-col :span="8"><a-statistic title="商品金额" :value="totalAmount" :precision="2" prefix="¥" /></a-col>
          <a-col :span="8"><a-statistic title="商品种类" :value="formData.items.length" /></a-col>
        </a-row>
      </div>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'
import { optionsApi } from '@/api/options'

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

const visible = computed({ get: () => props.open, set: (val) => emit('update:open', val) })
const isEdit = computed(() => !!props.editData?.id)
const loading = ref(false)
const formRef = ref<FormInstance>()

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

const formRules = {
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  orderDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  operatorId: [{ required: true, message: '请选择经办人', trigger: 'change' }]
}

const itemColumns = [
  { title: '商品', dataIndex: 'productId', key: 'productId', width: 180 },
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 100 },
  { title: '批次号', dataIndex: 'batchNo', key: 'batchNo', width: 120 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 100 },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 100 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 60 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
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
    message.success(isEdit.value ? '更新成功' : '创建成功')
    emit('success')
    visible.value = false
  } catch (error) {
    console.error('表单验证失败:', error)
  } finally {
    loading.value = false
  }
}

const handleCancel = () => { visible.value = false }

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
  } catch {
    // 默认空列表
  }
}

watch(visible, (val) => {
  if (val) {
    loadOptions()
    if (props.editData) {
      Object.assign(formData, props.editData)
    } else {
      const prefix = { in: 'IN', out: 'OUT', transfer: 'TR' }
      formData.orderNo = prefix[props.type] + dayjs().format('YYYYMMDDHHmmss')
    }
  }
})
</script>

<style scoped>
.product-table-actions { margin-bottom: 16px; }
.amount-summary { margin-top: 16px; padding: 16px; background: #f5f5f5; border-radius: 4px; }
.amount-text { font-weight: 600; color: #1890ff; }
.danger { color: #ff4d4f; }
</style>