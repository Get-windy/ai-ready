<template>
  <FullScreenDetail
    :visible="open"
    :title="editData ? '编辑销售订单' : '新建销售订单'"
    :save-loading="loading"
    :show-save-and-new="!isEdit"
    @close="handleFormClose"
    @save="handleOk"
    @save-and-new="handleFormSaveAndNew"
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
            <a-input v-model:value="formData.orderNo" placeholder="系统自动生成" :disabled="true" size="small" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="客户" name="customerId">
            <a-select
              v-model:value="formData.customerId"
              placeholder="请选择客户"
              show-search
              :filter-option="filterOption"
              :loading="loadingOptions"
              size="small"
              @change="handleCustomerChange"
            >
              <a-select-option v-for="item in customerOptions" :key="item.id" :value="item.id">
                {{ item.name }}
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
              size="small"
            />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="预计发货日期" name="deliveryDate">
            <a-date-picker
              v-model:value="formData.deliveryDate"
              placeholder="请选择预计发货日期"
              style="width: 100%"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              size="small"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="24">
        <a-col :span="12">
          <a-form-item label="销售员" name="salespersonId">
            <a-select
              v-model:value="formData.salespersonId"
              placeholder="请选择销售员"
              show-search
              :filter-option="filterOption"
              :loading="loadingOptions"
              size="small"
            >
              <a-select-option v-for="item in userOptions" :key="item.id" :value="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="付款方式" name="paymentMethod">
            <a-select v-model:value="formData.paymentMethod" placeholder="请选择付款方式" size="small">
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
          <a-form-item label="发货仓库" name="warehouseId">
            <a-select v-model:value="formData.warehouseId" placeholder="请选择发货仓库" :loading="loadingOptions" size="small">
              <a-select-option v-for="w in warehouseOptions" :key="w.id" :value="w.id">
                {{ w.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="税率(%)" name="taxRate">
            <a-input-number v-model:value="formData.taxRate" :min="0" :max="100" :step="1" :precision="2" style="width: 100%" size="small" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="收货地址" name="shippingAddress">
        <a-input v-model:value="formData.shippingAddress" placeholder="请输入收货地址" size="small" />
      </a-form-item>

      <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 21 }">
        <a-textarea v-model:value="formData.remark" placeholder="请输入备注" :rows="2" :maxlength="500" show-count size="small" />
      </a-form-item>

      <a-divider>销售商品明细</a-divider>

      <div class="product-table-actions">
        <a-button type="primary" size="small" @click="handleAddItem">
          <template #icon><PlusOutlined /></template>添加商品
        </a-button>
      </div>

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
        <template #productIdCell="{ record, index }">
          <a-select
            v-model:value="record.productId"
            placeholder="请选择商品"
            show-search
            :filter-option="filterOption"
            style="width: 100%"
            :loading="loadingOptions"
            size="small"
            @change="(val: number) => handleProductChange(val, index)"
          >
            <a-select-option v-for="p in productOptions" :key="p.id" :value="p.id">
              {{ p.name }} ({{ p.code || p.productCode }})
            </a-select-option>
          </a-select>
        </template>
        <template #productCodeCell="{ record }">
          {{ record.productCode }}
        </template>
        <template #quantityCell="{ record }">
          <a-input-number v-model:value="record.quantity" :min="1" :max="99999" :step="1" style="width: 100%" size="small" />
        </template>
        <template #unitPriceCell="{ record }">
          <a-input-number v-model:value="record.unitPrice" :min="0" :step="0.01" :precision="2" style="width: 100%" size="small" />
        </template>
        <template #discountCell="{ record }">
          <a-input-number v-model:value="record.discount" :min="0" :max="100" :step="1" :precision="0" style="width: 100%" size="small" />
        </template>
        <template #amountCell="{ record }">
          <span class="amount-text">¥{{ ((record.quantity || 0) * (record.unitPrice || 0) * (1 - (record.discount || 0) / 100)).toFixed(2) }}</span>
        </template>
        <template #unitCell="{ record }">
          {{ record.unit }}
        </template>
        <template #action="{ index }">
          <a-space>
            <a @click="handleCopyItem(index)">复制</a>
            <a-popconfirm title="确定删除？" @confirm="handleDeleteItem(index)">
              <a class="danger">删除</a>
            </a-popconfirm>
          </a-space>
        </template>
      </VxeTableList>

      <div class="amount-summary">
        <a-row :gutter="16">
          <a-col :span="6"><a-statistic title="商品数量" :value="totalQuantity" /></a-col>
          <a-col :span="6"><a-statistic title="商品金额" :value="totalAmount" :precision="2" prefix="¥" /></a-col>
          <a-col :span="6"><a-statistic title="税额" :value="taxAmount" :precision="2" prefix="¥" /></a-col>
          <a-col :span="6"><a-statistic title="价税合计" :value="totalAmountWithTax" :precision="2" prefix="¥" /></a-col>
        </a-row>
      </div>
    </a-form>
  </FullScreenDetail>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick } from 'vue'
import { message, Modal } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { FullScreenDetail } from '@/components'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'
import { saleOrderApi } from '@/api/erp'
import optionsApi from '@/api/options'

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

interface OrderItem {
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

interface Props {
  open: boolean
  editData?: any
  isEdit?: boolean
  /** @deprecated 使用 editData 替代 */
  record?: any
}

const props = defineProps<Props>()
const emit = defineEmits<{ (e: 'update:open', val: boolean): void; (e: 'success'): void }>()

const loading = ref(false)
const loadingOptions = ref(false)
const formRef = ref<FormInstance>()

// ── 表单脏状态跟踪 ──────────────────────────────────────
const initialFormSnapshot = ref<string>('')
function saveFormSnapshot() {
  initialFormSnapshot.value = JSON.stringify({ ...formData, items: [...formData.items] })
}
const formDirty = computed(() => {
  if (!props.open) return false
  return JSON.stringify({ ...formData, items: [...formData.items] }) !== initialFormSnapshot.value
})

const formData = reactive({
  orderNo: '',
  customerId: undefined as number | undefined,
  customerName: '',
  orderDate: dayjs().format('YYYY-MM-DD'),
  deliveryDate: '',
  salespersonId: undefined as number | undefined,
  salespersonName: '',
  paymentMethod: 2,
  taxRate: 13,
  warehouseId: undefined as string | undefined,
  shippingAddress: '',
  remark: '',
  items: [] as OrderItem[]
})

const formRules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  orderDate: [{ required: true, message: '请选择订单日期', trigger: 'change' }],
  deliveryDate: [
    {
      validator: (_rule: any, value: string) => {
        if (!value || !formData.orderDate) return Promise.resolve()
        if (dayjs(value).isBefore(dayjs(formData.orderDate), 'day')) {
          return Promise.reject(new Error('预计发货日期不能早于订单日期'))
        }
        return Promise.resolve()
      },
      trigger: 'change'
    }
  ],
  salespersonId: [{ required: true, message: '请选择销售员', trigger: 'change' }]
}

const itemColumns = [
  { title: '商品', dataIndex: 'productId', key: 'productId', width: 180 },
  { title: '编码', dataIndex: 'productCode', key: 'productCode', width: 100 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80 },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100 },
  { title: '折扣%', dataIndex: 'discount', key: 'discount', width: 70 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 100 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 60 },
  { title: '操作', type: 'action', width: 80, fixed: 'right' }
]

const customerOptions = ref<any[]>([])
const userOptions = ref<any[]>([])
const productOptions = ref<any[]>([])
const warehouseOptions = ref<any[]>([])

const totalQuantity = computed(() => formData.items.reduce((s, i) => s + (i.quantity || 0), 0))
const totalAmount = computed(() => formData.items.reduce((s, i) => s + (i.quantity || 0) * (i.unitPrice || 0) * (1 - (i.discount || 0) / 100), 0))
const taxAmount = computed(() => totalAmount.value * (formData.taxRate || 0) / 100)
const totalAmountWithTax = computed(() => totalAmount.value + taxAmount.value)

const filterOption = (input: string, option: any) => {
  const text = option?.label || option?.value?.toString() || ''
  return text.toLowerCase().includes(input.toLowerCase())
}

function handleCustomerChange(val: number) {
  const customer = customerOptions.value.find(c => c.id === val)
  if (customer) {
    formData.customerName = customer.name
    formData.shippingAddress = customer.address || ''
  }
}

function handleProductChange(val: number, index: number) {
  const product = productOptions.value.find(p => p.id === val)
  if (product && formData.items[index]) {
    formData.items[index].productCode = product.code || product.productCode || ''
    formData.items[index].productName = product.name || product.productName || ''
    formData.items[index].unit = product.unit || ''
    formData.items[index].unitPrice = product.salePrice || product.price || 0
  }
}

function handleAddItem() {
  formData.items.push({
    id: Date.now().toString(),
    productId: undefined,
    productCode: '',
    productName: '',
    quantity: 1,
    unitPrice: 0,
    discount: 0,
    unit: '',
    remark: ''
  })
}

function handleDeleteItem(index: number) {
  if (formData.items.length > 1) {
    formData.items.splice(index, 1)
  } else {
    message.warning('至少保留一条商品明细')
  }
}

function handleCopyItem(index: number) {
  const item = formData.items[index]
  if (item) {
    formData.items.splice(index + 1, 0, { ...item, id: Date.now().toString() })
  }
}

async function handleOk() {
  try {
    await formRef.value?.validate()
    if (formData.items.length === 0) {
      message.error('请至少添加一条商品明细')
      return
    }
    if (formData.items.some(item => !item.productId)) {
      message.error('请选择所有商品')
      return
    }
    loading.value = true
    const submitData: Record<string, any> = {
      customerId: formData.customerId,
      customerName: formData.customerName,
      orderDate: formData.orderDate,
      deliveryDate: formData.deliveryDate,
      salespersonId: formData.salespersonId,
      salesperson: formData.salespersonName,
      paymentMethod: formData.paymentMethod,
      taxRate: formData.taxRate,
      warehouseId: formData.warehouseId,
      shippingAddress: formData.shippingAddress,
      remark: formData.remark,
      totalAmount: Math.round(totalAmount.value * 100) / 100,
      finalAmount: Math.round(totalAmountWithTax.value * 100) / 100,
      details: formData.items.map(i => ({
        productId: i.productId,
        productCode: i.productCode,
        productName: i.productName,
        quantity: i.quantity,
        unitPrice: i.unitPrice,
        discount: i.discount,
        totalAmount: Math.round((i.quantity || 0) * (i.unitPrice || 0) * (1 - (i.discount || 0) / 100) * 100) / 100,
        remark: i.remark
      }))
    }
    if (props.editData?.id) {
      await saleOrderApi.update(props.editData.id, submitData)
      message.success('更新成功')
    } else {
      await saleOrderApi.create(submitData)
      message.success('创建成功')
    }
    emit('success')
    updateOpen(false)
  } catch (error: any) {
    const errMsg = error?.response?.data?.message || error?.message || '操作失败'
    message.error(errMsg)
  } finally {
    loading.value = false
  }
}

function handleFormClose() {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认关闭',
      content: '当前表单有未保存的内容，确定关闭吗？',
      okText: '确认关闭',
      cancelText: '继续编辑',
      centered: true,
      onOk() { updateOpen(false) }
    })
    return
  }
  updateOpen(false)
}

async function handleFormSaveAndNew() {
  try {
    await formRef.value?.validate()
    if (formData.items.length === 0) { message.error('请至少添加一条商品明细'); return }
    if (formData.items.some(item => !item.productId)) { message.error('请选择所有商品'); return }
    loading.value = true
    const submitData: Record<string, any> = {
      customerId: formData.customerId, customerName: formData.customerName,
      orderDate: formData.orderDate, deliveryDate: formData.deliveryDate,
      salespersonId: formData.salespersonId, salesperson: formData.salespersonName,
      paymentMethod: formData.paymentMethod, taxRate: formData.taxRate,
      warehouseId: formData.warehouseId, shippingAddress: formData.shippingAddress,
      remark: formData.remark, totalAmount: Math.round(totalAmount.value * 100) / 100,
      finalAmount: Math.round(totalAmountWithTax.value * 100) / 100,
      details: formData.items.map(i => ({
        productId: i.productId, productCode: i.productCode, productName: i.productName,
        quantity: i.quantity, unitPrice: i.unitPrice, discount: i.discount,
        totalAmount: Math.round((i.quantity || 0) * (i.unitPrice || 0) * (1 - (i.discount || 0) / 100) * 100) / 100,
        remark: i.remark
      }))
    }
    if (props.editData?.id) {
      await saleOrderApi.update(props.editData.id, submitData)
      message.success('更新成功')
    } else {
      await saleOrderApi.create(submitData)
      message.success('创建成功')
    }
    // 重置表单，保持打开
    Object.assign(formData, {
      orderNo: 'SO' + dayjs().format('YYYYMMDDHHmmss') + Math.random().toString(36).substring(2, 6).toUpperCase(),
      customerId: undefined, customerName: '', orderDate: dayjs().format('YYYY-MM-DD'),
      deliveryDate: '', salespersonId: undefined, salespersonName: '',
      paymentMethod: 2, taxRate: 13, warehouseId: undefined,
      shippingAddress: '', remark: '',
      items: [{ id: '1', productId: undefined, productCode: '', productName: '', quantity: 1, unitPrice: 0, discount: 0, unit: '', remark: '' }]
    })
    emit('success')
    nextTick(() => saveFormSnapshot())
  } catch (error: any) {
    const errMsg = error?.response?.data?.message || error?.message || '操作失败'
    message.error(errMsg)
  } finally {
    loading.value = false
  }
}

function updateOpen(val: boolean) {
  emit('update:open', val)
}

/** 从响应中安全提取数组数据（兼容标准包装和无包装响应） */
function extractArray(res: any): any[] {
  if (Array.isArray(res)) return res
  if (res?.data && Array.isArray(res.data)) return res.data
  return []
}

const optionsCache = new Map<string, { data: any[]; timestamp: number }>()
const CACHE_TTL = 60000 // 1 minute

async function loadOptions() {
  loadingOptions.value = true
  try {
    const now = Date.now()
    const fetchIfNeeded = async (key: string, fetcher: () => Promise<any>) => {
      const cached = optionsCache.get(key)
      if (cached && now - cached.timestamp < CACHE_TTL) {
        return cached.data
      }
      const res = await fetcher()
      const data = extractArray(res)
      optionsCache.set(key, { data, timestamp: now })
      return data
    }
    const [customers, users, products, warehouses] = await Promise.all([
      fetchIfNeeded('customers', () => optionsApi.getCustomers()),
      fetchIfNeeded('users', () => optionsApi.getUsers('salesman')),
      fetchIfNeeded('products', () => optionsApi.getProducts()),
      fetchIfNeeded('warehouses', () => optionsApi.getWarehouses())
    ])
    customerOptions.value = extractArray(customers)
    userOptions.value = extractArray(users)
    productOptions.value = extractArray(products)
    warehouseOptions.value = extractArray(warehouses)
    if (customerOptions.value.length === 0 || userOptions.value.length === 0 || productOptions.value.length === 0) {
      console.warn('基础选项数据存在空数据，请检查后端配置')
    }
  } catch (error: any) {
    console.warn('[销售订单] 加载下拉选项失败:', error?.message)
    message.error('加载基础数据失败，请检查网络或联系管理员')
  } finally {
    loadingOptions.value = false
  }
}

watch(() => props.open, (val) => {
  if (val) {
    loadOptions()
    const data = props.editData || props.record
    if (data?.id) {
      formData.orderNo = data.orderNo || 'SO' + dayjs().format('YYYYMMDDHHmmss') + Math.random().toString(36).substring(2, 6).toUpperCase()
      formData.customerId = data.customerId
      formData.customerName = data.customerName || ''
      formData.orderDate = data.orderDate || dayjs().format('YYYY-MM-DD')
      formData.deliveryDate = data.deliveryDate || data.expectedDeliveryDate || ''
      formData.salespersonId = data.salespersonId || data.salesmanId
      formData.salespersonName = data.salespersonName || data.salesmanName || ''
      formData.shippingAddress = data.shippingAddress || ''
      formData.remark = data.remark || ''
      if (data.details?.length) {
        formData.items = data.details.map((item: any) => ({
          id: Date.now().toString() + Math.random(),
          productId: item.productId,
          productCode: item.productCode || '',
          productName: item.productName || '',
          quantity: item.quantity || 1,
          unitPrice: item.unitPrice || 0,
          discount: item.discount || 0,
          unit: item.unit || '',
          remark: item.remark || ''
        }))
      } else {
        formData.items = [{ id: '1', productId: undefined, productCode: '', productName: '', quantity: 1, unitPrice: 0, discount: 0, unit: '', remark: '' }]
      }
    } else {
      formData.orderNo = 'SO' + dayjs().format('YYYYMMDDHHmmss') + Math.random().toString(36).substring(2, 6).toUpperCase()
      formData.orderDate = dayjs().format('YYYY-MM-DD')
      formData.customerId = undefined
      formData.deliveryDate = ''
      formData.salespersonId = undefined
      formData.paymentMethod = 2
      formData.taxRate = 13
      formData.warehouseId = data.warehouseId
      formData.shippingAddress = data.shippingAddress || ''
      formData.remark = ''
      formData.items = [{ id: '1', productId: undefined, productCode: '', productName: '', quantity: 1, unitPrice: 0, discount: 0, unit: '', remark: '' }]
    }
    nextTick(() => saveFormSnapshot())
  }
})
</script>

<style scoped>
.product-table-actions { margin-bottom: 16px; }
.amount-summary { margin-top: 16px; padding: 16px; background: var(--color-bg-layout); border-radius: var(--border-radius-lg); }
.amount-text { font-weight: 600; color: var(--color-primary); }
.danger { color: var(--color-danger); }

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
