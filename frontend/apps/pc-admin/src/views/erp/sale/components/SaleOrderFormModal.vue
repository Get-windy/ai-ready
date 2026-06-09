<template>
  <a-modal
    v-model:open="visible"
    :title="isEdit ? '编辑销售订单' : '新建销售订单'"
    :width="1000"
    :confirm-loading="loading"
    :mask-closable="false"
    :closable="!hasUnsavedChanges"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <!-- 离开拦截提示 -->
    <template #footer>
      <a-space>
        <a-button @click="handleCancel">取消</a-button>
        <a-button type="primary" :loading="loading" @click="handleOk">
          {{ isEdit ? '保存修改' : '提交订单' }}
        </a-button>
      </a-space>
    </template>

    <a-spin :spinning="optionsLoading" tip="正在加载选项数据...">
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 18 }"
      >
        <!-- 基础信息区 -->
        <a-card title="基础信息" size="small" :bordered="false" class="form-section">
          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="订单号" name="orderNo">
                <a-input v-model:value="formData.orderNo" placeholder="系统自动生成" disabled>
                  <template #suffix>
                    <a-tooltip title="订单号由系统自动生成，不可修改">
                      <InfoCircleOutlined style="color: #999" />
                    </a-tooltip>
                  </template>
                </a-input>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="客户" name="customerId">
                <a-select
                  v-model:value="formData.customerId"
                  placeholder="请选择客户"
                  show-search
                  :filter-option="filterOption"
                  :loading="optionsLoading"
                  @change="handleCustomerChange"
                >
                  <a-select-option v-for="customer in customerList" :key="customer.id" :value="customer.id">
                    {{ customer.name }}
                    <span v-if="customer.creditLevel" style="color: #999; font-size: 12px;">
                      (信用等级: {{ customer.creditLevel }})
                    </span>
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
                  :disabled-date="disabledDeliveryDate"
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="销售员" name="salesmanId">
                <a-select
                  v-model:value="formData.salesmanId"
                  placeholder="请选择销售员"
                  show-search
                  :filter-option="filterOption"
                  :loading="optionsLoading"
                  @change="handleSalesmanChange"
                >
                  <a-select-option v-for="user in userList" :key="user.id" :value="user.id">
                    {{ user.name }}
                    <span v-if="user.department" style="color: #999; font-size: 12px;">
                      ({{ user.department }})
                    </span>
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="付款方式" name="paymentMethod">
                <a-select v-model:value="formData.paymentMethod" placeholder="请选择付款方式">
                  <a-select-option :value="1">
                    预付全款
                    <span style="color: #52c41a; font-size: 12px;">(需付款后才发货)</span>
                  </a-select-option>
                  <a-select-option :value="2">货到付款</a-select-option>
                  <a-select-option :value="3">分期付款</a-select-option>
                  <a-select-option :value="4">
                    月结
                    <span style="color: #999; font-size: 12px;">(需签约客户)</span>
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="结算币种" name="currency">
                <a-select v-model:value="formData.currency" placeholder="请选择币种">
                  <a-select-option value="CNY">人民币 (CNY)</a-select-option>
                  <a-select-option value="USD">美元 (USD)</a-select-option>
                  <a-select-option value="EUR">欧元 (EUR)</a-select-option>
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
                >
                  <template #addonAfter>
                    <a-tooltip title="税率将计入价税合计">
                      <QuestionCircleOutlined />
                    </a-tooltip>
                  </template>
                </a-input-number>
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="24">
            <a-col :span="12">
              <a-form-item label="发货仓库" name="warehouseId">
                <a-select
                  v-model:value="formData.warehouseId"
                  placeholder="请选择发货仓库"
                  :loading="optionsLoading"
                >
                  <a-select-option v-for="warehouse in warehouseList" :key="warehouse.id" :value="warehouse.id">
                    {{ warehouse.name }}
                    <span v-if="warehouse.address" style="color: #999; font-size: 12px;">
                      ({{ warehouse.address }})
                    </span>
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="收货地址" name="shippingAddress">
                <a-input v-model:value="formData.shippingAddress" placeholder="请输入收货地址">
                  <template #suffix>
                    <a-tooltip v-if="formData.customerId" title="点击复制客户地址">
                      <CopyOutlined @click="copyCustomerAddress" style="color: #1890ff; cursor: pointer;" />
                    </a-tooltip>
                  </template>
                </a-input>
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item label="备注" name="remark" :label-col="{ span: 3 }" :wrapper-col="{ span: 21 }">
            <a-textarea
              v-model:value="formData.remark"
              placeholder="请输入备注信息（最多500字）"
              :rows="2"
              :maxlength="500"
              show-count
            />
          </a-form-item>
        </a-card>

        <!-- 商品明细区 -->
        <a-card title="商品明细" size="small" :bordered="false" class="form-section">
          <template #extra>
            <a-space>
              <a-tooltip title="快捷键: Ctrl+N 添加商品">
                <a-button type="link" size="small" @click="handleAddItem">
                  <template #icon><PlusOutlined /></template>
                  添加商品
                </a-button>
              </a-tooltip>
              <a-tooltip title="快捷键: Ctrl+D 清空明细">
                <a-button type="link" size="small" danger @click="handleClearItems">
                  <template #icon><ClearOutlined /></template>
                  清空
                </a-button>
              </a-tooltip>
              <span class="item-count">共 {{ formData.items.length }} 项</span>
            </a-space>
          </template>

          <div class="product-table-container">
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
              <!-- 商品选择 -->
              <template #productIdCell="{ record, index }">
                <a-select
                  v-model:value="record.productId"
                  placeholder="请选择商品"
                  show-search
                  :filter-option="filterOption"
                  :loading="optionsLoading"
                  style="width: 100%"
                  @change="(val) => handleProductChange(val, index)"
                >
                  <a-select-option v-for="product in productList" :key="product.id" :value="product.id">
                    {{ product.name }} ({{ product.code }})
                    <span style="color: #999; font-size: 12px;">
                      ¥{{ product.salePrice || product.price || 0 }}/{{ product.unit }}
                    </span>
                  </a-select-option>
                </a-select>
              </template>
              <!-- 数量 -->
              <template #quantityCell="{ record, index }">
                <a-input-number
                  v-model:value="record.quantity"
                  :min="1"
                  :max="record.stockQuantity || 99999"
                  :step="1"
                  style="width: 100%"
                  @change="debouncedCalculate(index)"
                />
                <span v-if="record.stockQuantity" class="stock-tip">
                  库存: {{ record.stockQuantity }}{{ record.unit }}
                </span>
              </template>
              <!-- 单价 -->
              <template #unitPriceCell="{ record, index }">
                <a-input-number
                  v-model:value="record.unitPrice"
                  :min="0"
                  :step="0.01"
                  :precision="2"
                  style="width: 100%"
                  @change="debouncedCalculate(index)"
                />
              </template>
              <!-- 折扣 -->
              <template #discountCell="{ record, index }">
                <a-input-number
                  v-model:value="record.discount"
                  :min="0"
                  :max="100"
                  :step="1"
                  :precision="2"
                  style="width: 100%"
                  @change="debouncedCalculate(index)"
                />
              </template>
              <!-- 金额 -->
              <template #amountCell="{ record }">
                <span class="amount-text">¥{{ calculateItemAmount(record).toFixed(2) }}</span>
              </template>
              <!-- 操作 -->
              <template #action="{ index }">
                <a-space>
                  <a-tooltip title="复制此行">
                    <a-button type="link" size="small" @click="handleCopyItem(index)">
                      <template #icon><CopyOutlined /></template>
                    </a-button>
                  </a-tooltip>
                  <a-popconfirm
                    title="确定要删除此商品吗？"
                    ok-text="删除"
                    cancel-text="取消"
                    @confirm="handleDeleteItem(index)"
                  >
                    <a-tooltip title="删除此行">
                      <a-button type="link" size="small" danger>
                        <template #icon><DeleteOutlined /></template>
                      </a-button>
                    </a-tooltip>
                  </a-popconfirm>
                </a-space>
              </template>
            </VxeTableList>
          </div>

          <!-- 汇总区域 -->
          <div class="amount-summary">
            <a-row :gutter="24">
              <a-col :span="6">
                <div class="summary-item">
                  <span class="summary-label">商品数量</span>
                  <span class="summary-value">{{ totalQuantity }} 件</span>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="summary-item">
                  <span class="summary-label">商品金额</span>
                  <span class="summary-value currency">¥{{ totalAmount.toFixed(2) }}</span>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="summary-item">
                  <span class="summary-label">折扣金额</span>
                  <span class="summary-value discount">¥{{ discountAmount.toFixed(2) }}</span>
                </div>
              </a-col>
              <a-col :span="6">
                <div class="summary-item highlight">
                  <span class="summary-label">价税合计</span>
                  <span class="summary-value currency">¥{{ totalAmountWithTax.toFixed(2) }}</span>
                </div>
              </a-col>
            </a-row>
          </div>
        </a-card>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import {
  PlusOutlined,
  DeleteOutlined,
  CopyOutlined,
  ClearOutlined,
  InfoCircleOutlined,
  QuestionCircleOutlined
} from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import dayjs from 'dayjs'
import { debounce } from 'lodash-es'
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
  stockQuantity?: number
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
const optionsLoading = ref(false)
const formRef = ref<FormInstance>()
// 表单初始数据（用于判断是否有修改）
const initialFormData = ref<string>('')

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

// 判断表单是否有未保存的修改
const hasUnsavedChanges = computed(() => {
  if (!initialFormData.value) return false
  const current = JSON.stringify(formData)
  return current !== initialFormData.value && formData.items.some(item => item.productId)
})

const formRules = {
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  orderDate: [{ required: true, message: '请选择订单日期', trigger: 'change' }],
  salesmanId: [{ required: true, message: '请选择销售员', trigger: 'change' }]
}

const itemVxeColumns = [
  { field: 'productId', title: '商品', width: 200, slotName: 'productIdCell' },
  { field: 'productCode', title: '编码', width: 100 },
  { field: 'quantity', title: '数量', width: 120, slotName: 'quantityCell' },
  { field: 'unitPrice', title: '单价', width: 120, align: 'right', slotName: 'unitPriceCell' },
  { field: 'discount', title: '折扣%', width: 100, slotName: 'discountCell' },
  { field: 'amount', title: '金额', width: 120, align: 'right', slotName: 'amountCell' },
  { field: 'unit', title: '单位', width: 60 },
  { field: 'action', title: '操作', width: 100, fixed: 'right', type: 'action' }
]

const customerList = ref<any[]>([])
const userList = ref<any[]>([])
const productList = ref<any[]>([])
const warehouseList = ref<any[]>([])

// 汇总计算
const totalQuantity = computed(() => formData.items.reduce((sum, item) => sum + (item.quantity || 0), 0))
const totalAmount = computed(() => formData.items.reduce((sum, item) => sum + calculateItemAmount(item), 0))
const discountAmount = computed(() => formData.items.reduce((sum, item) => sum + (item.quantity || 0) * (item.unitPrice || 0) * (item.discount || 0) / 100, 0))
const totalAmountWithTax = computed(() => totalAmount.value + totalAmount.value * formData.taxRate / 100)



const calculateItemAmount = (item: SaleOrderItem) => {
  return (item.quantity || 0) * (item.unitPrice || 0) * (1 - (item.discount || 0) / 100)
}

// 防抖计算
const debouncedCalculate = debounce((index: number) => {
  // 触发响应式更新
  formData.items[index] = { ...formData.items[index] }
}, 300)

const filterOption = (input: string, option: any) => {
  const text = option.label || option.value?.toString() || ''
  return text.toLowerCase().includes(input.toLowerCase())
}

// 禁止发货日期早于订单日期
const disabledDeliveryDate = (current: dayjs.Dayjs) => {
  return current && current < dayjs(formData.orderDate).startOf('day')
}

const handleCustomerChange = (val: number) => {
  const customer = customerList.value.find(c => c.id === val)
  if (customer) {
    formData.customerName = customer.name
    if (customer.address) {
      formData.shippingAddress = customer.address
    }
  }
}

const handleSalesmanChange = (val: number) => {
  const user = userList.value.find(u => u.id === val)
  if (user) {
    formData.salesmanName = user.name
  }
}

const copyCustomerAddress = () => {
  const customer = customerList.value.find(c => c.id === formData.customerId)
  if (customer?.address) {
    formData.shippingAddress = customer.address
    message.success('已复制客户地址')
  }
}

const handleProductChange = (val: number, index: number) => {
  const product = productList.value.find(p => p.id === val)
  if (product) {
    formData.items[index].productCode = product.code || product.productCode || ''
    formData.items[index].productName = product.name || product.productName || ''
    formData.items[index].unit = product.unit || ''
    formData.items[index].unitPrice = product.salePrice || product.price || 0
    formData.items[index].stockQuantity = product.stockQuantity || product.quantity || 0
  }
}

const handleAddItem = () => {
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

const handleCopyItem = (index: number) => {
  const item = formData.items[index]
  formData.items.splice(index + 1, 0, {
    ...item,
    id: Date.now().toString()
  })
  message.success('已复制商品行')
}

const handleDeleteItem = (index: number) => {
  if (formData.items.length > 1) {
    formData.items.splice(index, 1)
    message.success('已删除商品')
  } else {
    message.warning('至少保留一条商品明细')
  }
}

const handleClearItems = () => {
  Modal.confirm({
    title: '清空商品明细',
    content: '确定要清空所有商品明细吗？此操作不可恢复。',
    okText: '确认清空',
    okType: 'danger',
    cancelText: '取消',
    onOk: () => {
      formData.items = [{
        id: '1',
        productId: undefined,
        productCode: '',
        productName: '',
        quantity: 1,
        unitPrice: 0,
        discount: 0,
        unit: '',
        remark: ''
      }]
      message.success('已清空商品明细')
    }
  })
}

const handleOk = async () => {
  try {
    await formRef.value?.validate()
    if (formData.items.some(item => !item.productId)) {
      message.error('请选择所有商品')
      return
    }
    if (formData.items.length === 0) {
      message.error('请添加至少一条商品明细')
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
      message.success('订单更新成功')
    } else {
      await salesOrderApi.create(submitData)
      message.success('订单创建成功')
    }
    emit('success')
    visible.value = false
  } catch (error: any) {
    if (error?.errorFields) return
    const errMsg = error?.response?.data?.message || error?.message || '操作失败'
    message.error(errMsg)
  } finally {
    loading.value = false
  }
}

// 离开拦截
const handleCancel = () => {
  if (hasUnsavedChanges.value) {
    Modal.confirm({
      title: '离开确认',
      content: '当前表单有未保存的更改，确定要关闭吗？',
      okText: '放弃修改',
      okType: 'danger',
      cancelText: '继续编辑',
      onOk: () => {
        visible.value = false
      }
    })
  } else {
    visible.value = false
  }
}

const loadOptions = async () => {
  optionsLoading.value = true
  try {
    const [customers, users, products, warehouses] = await Promise.all([
      optionsApi.getCustomers(),
      optionsApi.getUsers('salesman'),
      optionsApi.getProducts(),
      optionsApi.getWarehouses()
    ])
    customerList.value = Array.isArray(customers) ? customers : []
    userList.value = Array.isArray(users) ? users : []
    productList.value = Array.isArray(products) ? products : []
    warehouseList.value = Array.isArray(warehouses) ? warehouses : []
  } catch (error: any) {
    console.warn('加载下拉选项失败，使用默认数据:', error?.message)
    // 默认数据
    customerList.value = [
      { id: 1, name: '北京科技有限公司', address: '北京市朝阳区建国路88号', creditLevel: 'A' },
      { id: 2, name: '上海贸易集团有限公司', address: '上海市浦东新区陆家嘴金融中心', creditLevel: 'B' },
      { id: 3, name: '广州制造有限公司', address: '广州市天河区体育西路', creditLevel: 'A' }
    ]
    userList.value = [
      { id: 1, name: '张三', department: '销售一部' },
      { id: 2, name: '李四', department: '销售二部' }
    ]
    productList.value = [
      { id: 1, code: 'P001', name: '高精度传感器', unit: '件', salePrice: 150, stockQuantity: 500 },
      { id: 2, code: 'P002', name: '工业控制器', unit: '台', salePrice: 2800, stockQuantity: 120 },
      { id: 3, code: 'P003', name: '连接线缆套装', unit: '套', salePrice: 85, stockQuantity: 1000 }
    ]
    warehouseList.value = [
      { id: 1, name: '北京主仓库', address: '朝阳区' },
      { id: 2, name: '上海分仓库', address: '浦东新区' }
    ]
  } finally {
    optionsLoading.value = false
  }
}

// 快捷键监听
const handleKeyDown = (e: KeyboardEvent) => {
  if (!visible.value) return
  if (e.ctrlKey && e.key === 'n') {
    e.preventDefault()
    handleAddItem()
  }
  if (e.ctrlKey && e.key === 'd') {
    e.preventDefault()
    handleClearItems()
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleKeyDown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeyDown)
})

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
    // 记录初始状态
    initialFormData.value = JSON.stringify(formData)
  } else {
    // 关闭时清除初始状态
    initialFormData.value = ''
  }
})
</script>

<style scoped>
.form-section {
  margin-bottom: 16px;
}

.form-section:last-child {
  margin-bottom: 0;
}

.product-table-container {
  margin-bottom: 16px;
}





/* 空占位行 */

/* 金额列右对齐等宽字体 */
.amount-text {
  font-weight: 600;
  color: #1890ff;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}


/* 库存提示 */
.stock-tip {
  display: block;
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}

/* 商品计数 */
.item-count {
  font-size: 12px;
  color: #666;
  padding: 4px 8px;
  background: #f5f5f5;
  border-radius: 4px;
}

/* 汇总区域 */
.amount-summary {
  margin-top: 0;
  padding: 16px 24px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 8px;
  border: 1px solid #dee2e6;
}

.summary-item {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  padding: 8px 0;
}

.summary-item.highlight {
  background: rgba(24, 144, 255, 0.1);
  border-radius: 6px;
  padding: 12px 16px;
  margin: -4px -16px;
}

.summary-label {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.summary-value.currency {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}

.summary-value.discount {
  color: #ff4d4f;
}

/* 操作按钮间距 */
:deep(.ant-space) {
  gap: 4px !important;
}
</style>
