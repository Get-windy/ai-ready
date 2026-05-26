<template>
  <a-modal
    v-model:open="visible"
    :title="isEdit ? '编辑采购订单' : '新建采购订单'"
    :width="900"
    :confirm-loading="loading"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <a-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 18 }"
    >
      <a-row :gutter="24">
        <a-col :span="12">
          <a-form-item label="订单号" name="orderNo">
            <a-input
              v-model:value="formData.orderNo"
              placeholder="系统自动生成"
              :disabled="true"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="供应商" name="supplierId">
            <a-select
              v-model:value="formData.supplierId"
              placeholder="请选择供应商"
              show-search
              :filter-option="filterOption"
              @change="handleSupplierChange"
            >
              <a-select-option
                v-for="supplier in supplierList"
                :key="supplier.id"
                :value="supplier.id"
              >
                {{ supplier.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="24">
        <a-col :span="12">
          <a-form-item label="订单日期" name="orderDate">
            <a-date-picker
              v-model:value="formData.orderDate"
              placeholder="请选择订单日期"
              style="width: 100%"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="预计到货日期" name="expectedDeliveryDate">
            <a-date-picker
              v-model:value="formData.expectedDeliveryDate"
              placeholder="请选择预计到货日期"
              style="width: 100%"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="24">
        <a-col :span="12">
          <a-form-item label="采购员" name="purchaserId">
            <a-select
              v-model:value="formData.purchaserId"
              placeholder="请选择采购员"
              show-search
              :filter-option="filterOption"
            >
              <a-select-option
                v-for="user in userList"
                :key="user.id"
                :value="user.id"
              >
                {{ user.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="付款方式" name="paymentMethod">
            <a-select
              v-model:value="formData.paymentMethod"
              placeholder="请选择付款方式"
            >
              <a-select-option :value="1">预付全款</a-select-option>
              <a-select-option :value="2">货到付款</a-select-option>
              <a-select-option :value="3">分期付款</a-select-option>
              <a-select-option :value="4">月结</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="24">
        <a-col :span="12">
          <a-form-item label="结算币种" name="currency">
            <a-select
              v-model:value="formData.currency"
              placeholder="请选择币种"
            >
              <a-select-option value="CNY">人民币(CNY)</a-select-option>
              <a-select-option value="USD">美元(USD)</a-select-option>
              <a-select-option value="EUR">欧元(EUR)</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="税率(%)" name="taxRate">
            <a-input-number
              v-model:value="formData.taxRate"
              :min="0"
              :max="100"
              :step="1"
              :precision="2"
              style="width: 100%"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 21 }">
        <a-textarea
          v-model:value="formData.remark"
          placeholder="请输入备注"
          :rows="3"
          :maxlength="500"
          show-count
        />
      </a-form-item>

      <a-divider>采购商品明细</a-divider>

      <div class="product-table-actions">
        <a-button type="primary" size="small" @click="handleAddItem">
          <template #icon><PlusOutlined /></template>
          添加商品
        </a-button>
        <a-button size="small" @click="handleImportItems">
          <template #icon><ImportOutlined /></template>
          批量导入
        </a-button>
      </div>

      <a-table
        :columns="itemColumns"
        :data-source="formData.items"
        :pagination="false"
        size="small"
        bordered
        row-key="id"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'productId'">
            <a-select
              v-model:value="record.productId"
              placeholder="请选择商品"
              show-search
              :filter-option="filterOption"
              style="width: 100%"
              @change="(val) => handleProductChange(val, index)"
            >
              <a-select-option
                v-for="product in productList"
                :key="product.id"
                :value="product.id"
              >
                {{ product.name }} ({{ product.code }})
              </a-select-option>
            </a-select>
          </template>
          <template v-else-if="column.key === 'quantity'">
            <a-input-number
              v-model:value="record.quantity"
              :min="1"
              :max="99999"
              :step="1"
              style="width: 100%"
              @change="calculateRowAmount(index)"
            />
          </template>
          <template v-else-if="column.key === 'unitPrice'">
            <a-input-number
              v-model:value="record.unitPrice"
              :min="0"
              :step="0.01"
              :precision="2"
              style="width: 100%"
              @change="calculateRowAmount(index)"
            />
          </template>
          <template v-else-if="column.key === 'amount'">
            <span class="amount-text">¥{{ (record.quantity * record.unitPrice).toFixed(2) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleCopyItem(index)">复制</a>
              <a-popconfirm
                title="确定要删除此商品吗？"
                @confirm="handleDeleteItem(index)"
              >
                <a class="danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>

      <div class="amount-summary">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-statistic title="商品数量" :value="totalQuantity" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="商品金额" :value="totalAmount" :precision="2" prefix="¥" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="税额" :value="taxAmount" :precision="2" prefix="¥" />
          </a-col>
          <a-col :span="6">
            <a-statistic title="价税合计" :value="totalAmountWithTax" :precision="2" prefix="¥" />
          </a-col>
        </a-row>
      </div>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ImportOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'

interface PurchaseOrderItem {
  id: string
  productId: number | undefined
  productCode: string
  productName: string
  quantity: number
  unitPrice: number
  unit: string
  remark: string
}

interface PurchaseOrderFormData {
  orderNo: string
  supplierId: number | undefined
  supplierName: string
  orderDate: string
  expectedDeliveryDate: string
  purchaserId: number | undefined
  purchaserName: string
  paymentMethod: number
  currency: string
  taxRate: number
  remark: string
  items: PurchaseOrderItem[]
}

const props = defineProps<{
  open: boolean
  editData?: any
}>()

const emit = defineEmits<{
  (e: 'update:open', val: boolean): void
  (e: 'success'): void
}>()

const visible = computed({
  get: () => props.open,
  set: (val) => emit('update:open', val)
})

const isEdit = computed(() => !!props.editData?.id)
const loading = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive<PurchaseOrderFormData>({
  orderNo: '',
  supplierId: undefined,
  supplierName: '',
  orderDate: dayjs().format('YYYY-MM-DD'),
  expectedDeliveryDate: '',
  purchaserId: undefined,
  purchaserName: '',
  paymentMethod: 2,
  currency: 'CNY',
  taxRate: 13,
  remark: '',
  items: [{ id: '1', productId: undefined, productCode: '', productName: '', quantity: 1, unitPrice: 0, unit: '', remark: '' }]
})

const formRules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  orderDate: [{ required: true, message: '请选择订单日期', trigger: 'change' }],
  purchaserId: [{ required: true, message: '请选择采购员', trigger: 'change' }]
}

const itemColumns = [
  { title: '商品', dataIndex: 'productId', key: 'productId', width: 200 },
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 100 },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 100 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 60 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' }
]

const supplierList = ref<any[]>([])
const userList = ref<any[]>([])
const productList = ref<any[]>([])

const totalQuantity = computed(() => formData.items.reduce((sum, item) => sum + item.quantity, 0))
const totalAmount = computed(() => formData.items.reduce((sum, item) => sum + item.quantity * item.unitPrice, 0))
const taxAmount = computed(() => totalAmount.value * formData.taxRate / 100)
const totalAmountWithTax = computed(() => totalAmount.value + taxAmount.value)

const filterOption = (input: string, option: any) => {
  const text = option.label || option.value?.toString() || ''
  return text.toLowerCase().includes(input.toLowerCase())
}

const handleSupplierChange = (val: number) => {
  const supplier = supplierList.value.find(s => s.id === val)
  if (supplier) {
    formData.supplierName = supplier.name
  }
}

const handleProductChange = (val: number, index: number) => {
  const product = productList.value.find(p => p.id === val)
  if (product) {
    formData.items[index].productCode = product.code
    formData.items[index].productName = product.name
    formData.items[index].unit = product.unit
    formData.items[index].unitPrice = product.purchasePrice || 0
    calculateRowAmount(index)
  }
}

const calculateRowAmount = (index: number) => {
}

const handleAddItem = () => {
  const newItem: PurchaseOrderItem = {
    id: Date.now().toString(),
    productId: undefined,
    productCode: '',
    productName: '',
    quantity: 1,
    unitPrice: 0,
    unit: '',
    remark: ''
  }
  formData.items.push(newItem)
}

const handleDeleteItem = (index: number) => {
  if (formData.items.length > 1) {
    formData.items.splice(index, 1)
  } else {
    message.warning('至少保留一条商品明细')
  }
}

const handleCopyItem = (index: number) => {
  const item = formData.items[index]
  const newItem = { ...item, id: Date.now().toString() }
  formData.items.splice(index + 1, 0, newItem)
}

const handleImportItems = () => {
  message.info('批量导入功能开发中')
}

const handleOk = async () => {
  try {
    await formRef.value?.validate()
    const hasEmptyItem = formData.items.some(item => !item.productId)
    if (hasEmptyItem) {
      message.error('请选择所有商品')
      return
    }
    loading.value = true
    const submitData = {
      ...formData,
      totalAmount: totalAmount.value,
      taxAmount: taxAmount.value,
      totalAmountWithTax: totalAmountWithTax.value
    }
    if (isEdit.value) {
      message.success('更新成功')
    } else {
      message.success('创建成功')
    }
    emit('success')
    visible.value = false
  } catch (error) {
    console.error('表单验证失败:', error)
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  visible.value = false
}

const loadOptions = async () => {
  supplierList.value = [
    { id: 1, name: '供应商A' },
    { id: 2, name: '供应商B' },
    { id: 3, name: '供应商C' }
  ]
  userList.value = [
    { id: 1, name: '张三' },
    { id: 2, name: '李四' },
    { id: 3, name: '王五' }
  ]
  productList.value = [
    { id: 1, code: 'P001', name: '商品A', unit: '件', purchasePrice: 100 },
    { id: 2, code: 'P002', name: '商品B', unit: '箱', purchasePrice: 200 },
    { id: 3, code: 'P003', name: '商品C', unit: '个', purchasePrice: 50 }
  ]
}

watch(visible, (val) => {
  if (val) {
    loadOptions()
    if (props.editData) {
      Object.assign(formData, props.editData)
    } else {
      formData.orderNo = 'PO' + dayjs().format('YYYYMMDDHHmmss')
    }
  }
})
</script>

<style scoped>
.product-table-actions {
  margin-bottom: 16px;
}

.amount-summary {
  margin-top: 16px;
  padding: 16px;
  background: #f5f5f5;
  border-radius: 4px;
}

.amount-text {
  font-weight: 600;
  color: #1890ff;
}

.danger {
  color: #ff4d4f;
}
</style>