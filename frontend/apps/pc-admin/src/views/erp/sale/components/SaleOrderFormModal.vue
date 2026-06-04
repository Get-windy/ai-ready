<template>
  <a-modal
    v-model:open="visible"
    :title="isEdit ? '编辑销售订单' : '新建销售订单'"
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
            <a-input v-model:value="formData.orderNo" placeholder="系统自动生成" :disabled="true" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="客户" name="customerId">
            <a-select
              v-model:value="formData.customerId"
              placeholder="请选择客户"
              show-search
              :filter-option="filterOption"
              @change="handleCustomerChange"
            >
              <a-select-option v-for="customer in customerList" :key="customer.id" :value="customer.id">
                {{ customer.name }}
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
          <a-form-item label="预计发货日期" name="expectedDeliveryDate">
            <a-date-picker
              v-model:value="formData.expectedDeliveryDate"
              placeholder="请选择预计发货日期"
              style="width: 100%"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="24">
        <a-col :span="12">
          <a-form-item label="销售员" name="salesmanId">
            <a-select v-model:value="formData.salesmanId" placeholder="请选择销售员" show-search :filter-option="filterOption">
              <a-select-option v-for="user in userList" :key="user.id" :value="user.id">
                {{ user.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="付款方式" name="paymentMethod">
            <a-select v-model:value="formData.paymentMethod" placeholder="请选择付款方式">
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
            <a-select v-model:value="formData.currency" placeholder="请选择币种">
              <a-select-option value="CNY">人民币(CNY)</a-select-option>
              <a-select-option value="USD">美元(USD)</a-select-option>
              <a-select-option value="EUR">欧元(EUR)</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="税率(%)" name="taxRate">
            <a-input-number v-model:value="formData.taxRate" :min="0" :max="100" :step="1" :precision="2" style="width: 100%" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="24">
        <a-col :span="12">
          <a-form-item label="发货仓库" name="warehouseId">
            <a-select v-model:value="formData.warehouseId" placeholder="请选择发货仓库">
              <a-select-option v-for="warehouse in warehouseList" :key="warehouse.id" :value="warehouse.id">
                {{ warehouse.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="收货地址" name="shippingAddress">
            <a-input v-model:value="formData.shippingAddress" placeholder="请输入收货地址" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 21 }">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="3" :maxlength="500" show-count />
      </a-form-item>

      <a-divider>销售商品明细</a-divider>

      <div class="product-table-actions">
        <a-button type="primary" size="small" @click="handleAddItem">
          <template #icon><PlusOutlined /></template>
          添加商品
        </a-button>
      </div>

      <a-table :columns="itemColumns" :data-source="formData.items" :pagination="false" size="small" bordered row-key="id">
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
              <a-select-option v-for="product in productList" :key="product.id" :value="product.id">
                {{ product.name }} ({{ product.code }})
              </a-select-option>
            </a-select>
          </template>
          <template v-else-if="column.key === 'quantity'">
            <a-input-number v-model:value="record.quantity" :min="1" :max="99999" :step="1" style="width: 100%" @change="calculateRowAmount(index)" />
          </template>
          <template v-else-if="column.key === 'unitPrice'">
            <a-input-number v-model:value="record.unitPrice" :min="0" :step="0.01" :precision="2" style="width: 100%" @change="calculateRowAmount(index)" />
          </template>
          <template v-else-if="column.key === 'discount'">
            <a-input-number v-model:value="record.discount" :min="0" :max="100" :step="1" :precision="2" style="width: 100%" @change="calculateRowAmount(index)" />
          </template>
          <template v-else-if="column.key === 'amount'">
            <span class="amount-text">¥{{ calculateItemAmount(record).toFixed(2) }}</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-popconfirm title="确定要删除此商品吗？" @confirm="handleDeleteItem(index)">
              <a class="danger">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>

      <div class="amount-summary">
        <a-row :gutter="16">
          <a-col :span="6"><a-statistic title="商品数量" :value="totalQuantity" /></a-col>
          <a-col :span="6"><a-statistic title="商品金额" :value="totalAmount" :precision="2" prefix="¥" /></a-col>
          <a-col :span="6"><a-statistic title="折扣金额" :value="discountAmount" :precision="2" prefix="¥" /></a-col>
          <a-col :span="6"><a-statistic title="价税合计" :value="totalAmountWithTax" :precision="2" prefix="¥" /></a-col>
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
import { salesOrderApi } from '@/api/order'
import optionsApi from '@/api/options'

interface SaleOrderItem {
  id: string
  productId: number | undefined
  productCode: string
  productName: string
  quantity: number
  unitPrice: number
  discount: number
  unit: string
  remark: string
}

interface SaleOrderFormData {
  orderNo: string
  customerId: number | undefined
  customerName: string
  orderDate: string
  expectedDeliveryDate: string
  salesmanId: number | undefined
  salesmanName: string
  paymentMethod: number
  currency: string
  taxRate: number
  warehouseId: number | undefined
  shippingAddress: string
  remark: string
  items: SaleOrderItem[]
}

const props = defineProps<{ open: boolean; editData?: any }>()
const emit = defineEmits<{ (e: 'update:open', val: boolean): void; (e: 'success'): void }>()

const visible = computed({ get: () => props.open, set: (val) => emit('update:open', val) })
const isEdit = computed(() => !!props.editData?.id)
const loading = ref(false)
const formRef = ref<FormInstance>()

const formData = reactive<SaleOrderFormData>({
  orderNo: '',
  customerId: undefined,
  customerName: '',
  orderDate: dayjs().format('YYYY-MM-DD'),
  expectedDeliveryDate: '',
  salesmanId: undefined,
  salesmanName: '',
  paymentMethod: 2,
  currency: 'CNY',
  taxRate: 13,
  warehouseId: undefined,
  shippingAddress: '',
  remark: '',
  items: [{ id: '1', productId: undefined, productCode: '', productName: '', quantity: 1, unitPrice: 0, discount: 0, unit: '', remark: '' }]
})

const formRules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  orderDate: [{ required: true, message: '请选择订单日期', trigger: 'change' }],
  salesmanId: [{ required: true, message: '请选择销售员', trigger: 'change' }]
}

const itemColumns = [
  { title: '商品', dataIndex: 'productId', key: 'productId', width: 180 },
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 100 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100 },
  { title: '折扣%', dataIndex: 'discount', key: 'discount', width: 80 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 100 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 60 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]

const customerList = ref<any[]>([])
const userList = ref<any[]>([])
const productList = ref<any[]>([])
const warehouseList = ref<any[]>([])

const totalQuantity = computed(() => formData.items.reduce((sum, item) => sum + (item.quantity || 0), 0))
const totalAmount = computed(() => formData.items.reduce((sum, item) => sum + calculateItemAmount(item), 0))
const discountAmount = computed(() => formData.items.reduce((sum, item) => sum + (item.quantity || 0) * (item.unitPrice || 0) * (item.discount || 0) / 100, 0))
const totalAmountWithTax = computed(() => totalAmount.value + totalAmount.value * formData.taxRate / 100)

const calculateItemAmount = (item: SaleOrderItem) => {
  return (item.quantity || 0) * (item.unitPrice || 0) * (1 - (item.discount || 0) / 100)
}

const filterOption = (input: string, option: any) => {
  const text = option.label || option.value?.toString() || ''
  return text.toLowerCase().includes(input.toLowerCase())
}

const handleCustomerChange = (val: number) => {
  const customer = customerList.value.find(c => c.id === val)
  if (customer) {
    formData.customerName = customer.name
    formData.shippingAddress = customer.address || ''
  }
}

const handleProductChange = (val: number, index: number) => {
  const product = productList.value.find(p => p.id === val)
  if (product) {
    formData.items[index].productCode = product.code || product.productCode
    formData.items[index].productName = product.name || product.productName
    formData.items[index].unit = product.unit
    formData.items[index].unitPrice = product.salePrice || product.price || 0
  }
}

const handleAddItem = () => {
  formData.items.push({ id: Date.now().toString(), productId: undefined, productCode: '', productName: '', quantity: 1, unitPrice: 0, discount: 0, unit: '', remark: '' })
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
    const submitData: Record<string, any> = {
      customerId: formData.customerId,
      customerName: formData.customerName,
      orderDate: formData.orderDate,
      deliveryDate: formData.expectedDeliveryDate,
      salespersonId: formData.salesmanId,
      salesperson: formData.salesmanName,
      paymentMethod: formData.paymentMethod,
      currency: formData.currency,
      taxRate: formData.taxRate,
      warehouseId: formData.warehouseId,
      shippingAddress: formData.shippingAddress,
      remark: formData.remark,
      totalAmount: Math.round(totalAmount.value * 100) / 100,
      discountAmount: Math.round(discountAmount.value * 100) / 100,
      finalAmount: Math.round(totalAmountWithTax.value * 100) / 100,
      details: formData.items.map(item => ({
        productId: item.productId,
        productCode: item.productCode,
        productName: item.productName,
        quantity: item.quantity,
        unitPrice: item.unitPrice,
        discount: item.discount,
        totalAmount: calculateItemAmount(item),
        remark: item.remark
      }))
    }
    if (isEdit.value && props.editData?.id) {
      await salesOrderApi.update(props.editData.id, submitData)
      message.success('更新成功')
    } else {
      await salesOrderApi.create(submitData)
      message.success('创建成功')
    }
    emit('success')
    visible.value = false
  } catch (error: any) {
    const errMsg = error?.response?.data?.message || error?.message || '操作失败'
    message.error(errMsg)
  } finally {
    loading.value = false
  }
}

const handleCancel = () => { visible.value = false }

const loadOptions = async () => {
  try {
    const [customers, users, products] = await Promise.all([
      optionsApi.getCustomers(),
      optionsApi.getUsers('salesman'),
      optionsApi.getProducts()
    ])
    customerList.value = Array.isArray(customers) ? customers : []
    userList.value = Array.isArray(users) ? users : []
    productList.value = Array.isArray(products) ? products : []
  } catch (error: any) {
    console.warn('加载下拉选项失败，使用默认数据:', error?.message)
    customerList.value = [
      { id: 1, name: '客户A', address: '北京市朝阳区' },
      { id: 2, name: '客户B', address: '上海市浦东新区' }
    ]
    userList.value = [{ id: 1, name: '张三' }, { id: 2, name: '李四' }]
    productList.value = [
      { id: 1, code: 'P001', name: '商品A', unit: '件', salePrice: 150 },
      { id: 2, code: 'P002', name: '商品B', unit: '箱', salePrice: 300 }
    ]
  }
}

watch(visible, (val) => {
  if (val) {
    loadOptions()
    if (props.editData) {
      Object.assign(formData, props.editData)
      if (!props.editData.orderNo) {
        formData.orderNo = 'SO' + dayjs().format('YYYYMMDDHHmmss')
      }
    } else {
      formData.orderNo = 'SO' + dayjs().format('YYYYMMDDHHmmss')
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