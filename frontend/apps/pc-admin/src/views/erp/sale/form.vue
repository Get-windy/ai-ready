<template>
  <div class="sale-order-form-page">
    <!-- 顶部操作栏 -->
    <div class="form-header">
      <div class="header-left">
        <span class="order-no">NO. {{ formData.orderNo || '待生成' }}</span>
        <a-button type="link" size="small">
          <template #icon><PaperClipOutlined /></template>
          附件
        </a-button>
      </div>
      <div class="header-center">
        <h2 class="form-title">销售订单</h2>
      </div>
      <div class="header-right">
        <a-button size="small" @click="handleSave">
          <template #icon><SaveOutlined /></template>
          保存
        </a-button>
        <a-button size="small" @click="handleSubmit">
          <template #icon><SendOutlined /></template>
          提交
        </a-button>
      </div>
    </div>

    <!-- 基本信息区 -->
    <div class="form-section">
      <a-row :gutter="16">
        <a-col :span="6">
          <a-form-item label="客户" required>
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
        <a-col :span="6">
          <a-form-item label="发货仓库" required>
            <a-select
              v-model:value="formData.warehouseId"
              placeholder="请选择仓库"
              show-search
              :filter-option="filterOption"
              :loading="loadingOptions"
              size="small"
            >
              <a-select-option v-for="w in warehouseOptions" :key="w.id" :value="w.id">
                {{ w.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="经手人" required>
            <a-select
              v-model:value="formData.salespersonId"
              placeholder="请选择经手人"
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
        <a-col :span="6">
          <a-form-item label="单据日期" required>
            <a-date-picker
              v-model:value="formData.orderDate"
              style="width: 100%"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              size="small"
            />
          </a-form-item>
        </a-col>
      </a-row>
      <a-row :gutter="16">
        <a-col :span="6">
          <a-form-item label="销售类型">
            <a-select v-model:value="formData.saleType" placeholder="请选择销售类型" size="small">
              <a-select-option :value="1">正常销售</a-select-option>
              <a-select-option :value="2">样品销售</a-select-option>
              <a-select-option :value="3">促销销售</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="收货人">
            <a-input v-model:value="formData.receiverName" placeholder="请输入收货人" size="small" />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="联系电话">
            <a-input v-model:value="formData.receiverPhone" placeholder="请输入联系电话" size="small" />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="收货地址">
            <a-input v-model:value="formData.receiverAddress" placeholder="请输入收货地址" size="small" />
          </a-form-item>
        </a-col>
      </a-row>
    </div>

    <!-- 商品明细表格 -->
    <div class="form-section product-section">
      <div class="section-header">
        <span class="section-title">商品明细</span>
        <a-button type="primary" size="small" @click="handleAddProduct">
          <template #icon><PlusOutlined /></template>
          添加商品
        </a-button>
      </div>

      <VxeTableList
        :columns="productColumns"
        :data-source="formData.products"
        :pagination="false as any"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #productCell="{ record, index }">
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
              {{ p.name }}
            </a-select-option>
          </a-select>
        </template>
        <template #quantityCell="{ record }">
          <a-input-number v-model:value="record.quantity" :min="0" :precision="2" style="width: 100%" size="small" />
        </template>
        <template #unitPriceCell="{ record }">
          <a-input-number v-model:value="record.unitPrice" :min="0" :precision="2" style="width: 100%" size="small" />
        </template>
        <template #amountCell="{ record }">
          <span class="amount-text">{{ ((record.quantity || 0) * (record.unitPrice || 0)).toFixed(2) }}</span>
        </template>
        <template #taxRateCell="{ record }">
          <a-input-number v-model:value="record.taxRate" :min="0" :max="100" :precision="1" style="width: 100%" size="small" />
        </template>
        <template #taxAmountCell="{ record }">
          <span>{{ ((record.quantity || 0) * (record.unitPrice || 0) * (record.taxRate || 0) / 100).toFixed(2) }}</span>
        </template>
        <template #totalAmountCell="{ record }">
          <span class="amount-text">{{ ((record.quantity || 0) * (record.unitPrice || 0) * (1 + (record.taxRate || 0) / 100)).toFixed(2) }}</span>
        </template>
        <template #action="{ index }">
          <a-button type="link" danger size="small" @click="handleRemoveProduct(index)">
            <template #icon><DeleteOutlined /></template>
          </a-button>
        </template>
      </VxeTableList>

      <div class="product-summary">
        <a-row :gutter="24">
          <a-col :span="6">
            <div class="summary-item">
              <span class="summary-label">总数量：</span>
              <span class="summary-value">{{ totalQuantity }}</span>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="summary-item">
              <span class="summary-label">商品金额：</span>
              <span class="summary-value">¥{{ totalAmount.toFixed(2) }}</span>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="summary-item">
              <span class="summary-label">税额：</span>
              <span class="summary-value">¥{{ taxAmount.toFixed(2) }}</span>
            </div>
          </a-col>
          <a-col :span="6">
            <div class="summary-item">
              <span class="summary-label">价税合计：</span>
              <span class="summary-value highlight">¥{{ totalAmountWithTax.toFixed(2) }}</span>
            </div>
          </a-col>
        </a-row>
      </div>
    </div>

    <!-- 底部标签页：收款/物流/会员 -->
    <div class="form-section bottom-section">
      <a-tabs v-model:activeKey="activeTab" size="small">
        <a-tab-pane key="payment" tab="收款信息">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="收款方式">
                <a-select v-model:value="formData.paymentMethod" placeholder="请选择收款方式" size="small">
                  <a-select-option :value="1">现金</a-select-option>
                  <a-select-option :value="2">银行转账</a-select-option>
                  <a-select-option :value="3">支票</a-select-option>
                  <a-select-option :value="4">其他</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="收款账户">
                <a-input v-model:value="formData.paymentAccount" placeholder="请输入收款账户" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="备注">
                <a-input v-model:value="formData.paymentRemark" placeholder="请输入备注" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-tab-pane>
        <a-tab-pane key="logistics" tab="物流信息">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="物流公司">
                <a-input v-model:value="formData.logisticsCompany" placeholder="请输入物流公司" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="物流单号">
                <a-input v-model:value="formData.logisticsNo" placeholder="请输入物流单号" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="运费">
                <a-input-number v-model:value="formData.shippingFee" :min="0" :precision="2" style="width: 100%" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-tab-pane>
        <a-tab-pane key="member" tab="会员信息">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="会员卡号">
                <a-input v-model:value="formData.memberCardNo" placeholder="请输入会员卡号" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="会员姓名">
                <a-input v-model:value="formData.memberName" placeholder="请输入会员姓名" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="会员折扣">
                <a-input-number v-model:value="formData.memberDiscount" :min="0" :max="100" :precision="1" style="width: 100%" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
        </a-tab-pane>
      </a-tabs>
    </div>

    <!-- 底部操作栏 -->
    <div class="form-footer">
      <div class="footer-left">
        <span class="footer-amount">本单金额：<span class="amount-highlight">¥{{ totalAmountWithTax.toFixed(2) }}</span></span>
      </div>
      <div class="footer-right">
        <a-button size="large" @click="handleSaveDraft">
          <template #icon><SaveOutlined /></template>
          保存草稿
          <span class="shortcut-hint">Ctrl+S</span>
        </a-button>
        <a-button type="primary" size="large" @click="handleSubmit">
          <template #icon><SendOutlined /></template>
          提交
          <span class="shortcut-hint">Ctrl+Enter</span>
        </a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PaperClipOutlined,
  SaveOutlined,
  SendOutlined,
  PlusOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { saleOrderApi } from '@/api/erp'
import optionsApi from '@/api/options'

const router = useRouter()
const route = useRoute()

// 表单数据
const formData = reactive({
  orderNo: '',
  customerId: undefined as number | undefined,
  customerName: '',
  warehouseId: undefined as number | undefined,
  warehouseName: '',
  salespersonId: undefined as number | undefined,
  salespersonName: '',
  orderDate: '',
  saleType: 1,
  receiverName: '',
  receiverPhone: '',
  receiverAddress: '',
  paymentMethod: undefined as number | undefined,
  paymentAccount: '',
  paymentRemark: '',
  logisticsCompany: '',
  logisticsNo: '',
  shippingFee: 0,
  memberCardNo: '',
  memberName: '',
  memberDiscount: 100,
  products: [] as any[]
})

const activeTab = ref('payment')
const loadingOptions = ref(false)

// 下拉选项
const customerOptions = ref<any[]>([])
const warehouseOptions = ref<any[]>([])
const userOptions = ref<any[]>([])
const productOptions = ref<any[]>([])

// 商品明细列配置
const productColumns = [
  { field: 'productCode', title: '商品编码', width: 120 },
  { field: 'productName', title: '商品名称', width: 180 },
  { field: 'specification', title: '规格', width: 120 },
  { field: 'quantity', title: '数量', width: 100, slotName: 'quantityCell' },
  { field: 'unit', title: '单位', width: 80 },
  { field: 'unitPrice', title: '单价', width: 100, slotName: 'unitPriceCell' },
  { field: 'amount', title: '金额', width: 120, slotName: 'amountCell' },
  { field: 'taxRate', title: '税率%', width: 80, slotName: 'taxRateCell' },
  { field: 'taxAmount', title: '税额', width: 100, slotName: 'taxAmountCell' },
  { field: 'totalAmount', title: '价税合计', width: 120, slotName: 'totalAmountCell' },
  { field: 'remark', title: '备注', width: 150 },
  { type: 'action', title: '操作', width: 80, fixed: 'right', slotName: 'action' }
]

// 计算属性
const totalQuantity = computed(() => formData.products.reduce((sum, p) => sum + (p.quantity || 0), 0))
const totalAmount = computed(() => formData.products.reduce((sum, p) => sum + (p.quantity || 0) * (p.unitPrice || 0), 0))
const taxAmount = computed(() => formData.products.reduce((sum, p) => sum + (p.quantity || 0) * (p.unitPrice || 0) * (p.taxRate || 0) / 100, 0))
const totalAmountWithTax = computed(() => formData.products.reduce((sum, p) => sum + (p.quantity || 0) * (p.unitPrice || 0) * (1 + (p.taxRate || 0) / 100), 0))

// 生成订单号
function generateOrderNo() {
  const date = new Date()
  const dateStr = date.toISOString().slice(0, 10).replace(/-/g, '')
  const random = Math.random().toString(36).substring(2, 8).toUpperCase()
  formData.orderNo = `SO${dateStr}${random}`
}

// 加载下拉选项
async function loadOptions() {
  loadingOptions.value = true
  try {
    const [customers, warehouses, users, products] = await Promise.all([
      optionsApi.getCustomers(),
      optionsApi.getWarehouses(),
      optionsApi.getUsers('salesman'),
      optionsApi.getProducts()
    ])
    customerOptions.value = customers?.data || customers || []
    warehouseOptions.value = warehouses?.data || warehouses || []
    userOptions.value = users?.data || users || []
    productOptions.value = products?.data || products || []
  } catch (err) {
    console.error('加载选项失败:', err)
  } finally {
    loadingOptions.value = false
  }
}

// 过滤函数
const filterOption = (input: string, option: any) => {
  const text = option?.label || option?.children?.[0]?.children || ''
  return text.toString().toLowerCase().includes(input.toLowerCase())
}

// 客户选择
function handleCustomerChange(val: number) {
  const customer = customerOptions.value.find(c => c.id === val)
  if (customer) {
    formData.customerName = customer.name
    formData.receiverName = customer.contactName || ''
    formData.receiverPhone = customer.contactPhone || ''
    formData.receiverAddress = customer.address || ''
  }
}

// 商品选择
function handleProductChange(val: number, index: number) {
  const product = productOptions.value.find(p => p.id === val)
  if (product && formData.products[index]) {
    formData.products[index].productCode = product.code || ''
    formData.products[index].productName = product.name || ''
    formData.products[index].specification = product.specification || ''
    formData.products[index].unit = product.unit || ''
    formData.products[index].unitPrice = product.price || 0
  }
}

// 添加商品
function handleAddProduct() {
  formData.products.push({
    id: Date.now().toString(),
    productId: undefined,
    productCode: '',
    productName: '',
    specification: '',
    quantity: 0,
    unit: '',
    unitPrice: 0,
    taxRate: 13,
    remark: ''
  })
}

// 删除商品
function handleRemoveProduct(index: number) {
  formData.products.splice(index, 1)
}

// 保存草稿
async function handleSaveDraft() {
  try {
    if (!formData.customerId) {
      message.warning('请选择客户')
      return
    }
    if (!formData.warehouseId) {
      message.warning('请选择发货仓库')
      return
    }
    if (!formData.salespersonId) {
      message.warning('请选择经手人')
      return
    }
    if (!formData.orderDate) {
      message.warning('请选择单据日期')
      return
    }
    if (formData.products.length === 0) {
      message.warning('请添加商品明细')
      return
    }

    const payload = {
      ...formData,
      status: 0, // 草稿状态
      totalAmount: totalAmount.value,
      taxAmount: taxAmount.value,
      finalAmount: totalAmountWithTax.value,
      details: formData.products.map(p => ({
        productId: p.productId,
        productCode: p.productCode,
        productName: p.productName,
        specification: p.specification,
        quantity: p.quantity,
        unit: p.unit,
        unitPrice: p.unitPrice,
        amount: p.quantity * p.unitPrice,
        taxRate: p.taxRate,
        taxAmount: p.quantity * p.unitPrice * p.taxRate / 100,
        totalAmount: p.quantity * p.unitPrice * (1 + p.taxRate / 100),
        remark: p.remark
      }))
    }

    await saleOrderApi.create(payload)
    message.success('保存草稿成功')
    router.push('/erp/sale')
  } catch (err: any) {
    message.error(err?.message || '保存失败')
  }
}

// 提交订单
async function handleSubmit() {
  try {
    if (!formData.customerId) {
      message.warning('请选择客户')
      return
    }
    if (!formData.warehouseId) {
      message.warning('请选择发货仓库')
      return
    }
    if (!formData.salespersonId) {
      message.warning('请选择经手人')
      return
    }
    if (!formData.orderDate) {
      message.warning('请选择单据日期')
      return
    }
    if (formData.products.length === 0) {
      message.warning('请添加商品明细')
      return
    }

    const payload = {
      ...formData,
      status: 1, // 待审批状态
      totalAmount: totalAmount.value,
      taxAmount: taxAmount.value,
      finalAmount: totalAmountWithTax.value,
      details: formData.products.map(p => ({
        productId: p.productId,
        productCode: p.productCode,
        productName: p.productName,
        specification: p.specification,
        quantity: p.quantity,
        unit: p.unit,
        unitPrice: p.unitPrice,
        amount: p.quantity * p.unitPrice,
        taxRate: p.taxRate,
        taxAmount: p.quantity * p.unitPrice * p.taxRate / 100,
        totalAmount: p.quantity * p.unitPrice * (1 + p.taxRate / 100),
        remark: p.remark
      }))
    }

    await saleOrderApi.create(payload)
    message.success('提交成功')
    router.push('/erp/sale')
  } catch (err: any) {
    message.error(err?.message || '提交失败')
  }
}

// 保存（兼容方法）
function handleSave() {
  handleSaveDraft()
}

// 键盘快捷键
function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 's') {
    e.preventDefault()
    handleSaveDraft()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
    e.preventDefault()
    handleSubmit()
  }
}

onMounted(() => {
  generateOrderNo()
  loadOptions()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.sale-order-form-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f5f5;
  overflow: hidden;
}

.form-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.order-no {
  font-size: 16px;
  font-weight: 600;
  color: #1890ff;
}

.header-center {
  flex: 1;
  text-align: center;
}

.form-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #262626;
}

.header-right {
  display: flex;
  gap: 8px;
}

.form-section {
  background: #fff;
  padding: 16px 24px;
  border-bottom: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.product-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.product-section :deep(.vxe-table-list-container) {
  flex: 1;
  overflow: auto;
}

.product-summary {
  margin-top: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
  flex-shrink: 0;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-label {
  font-size: 14px;
  color: #595959;
}

.summary-value {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
}

.summary-value.highlight {
  color: #f5222d;
  font-size: 18px;
}

.bottom-section {
  flex-shrink: 0;
}

.form-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: #fff;
  border-top: 1px solid #e8e8e8;
  flex-shrink: 0;
}

.footer-amount {
  font-size: 14px;
  color: #595959;
}

.amount-highlight {
  font-size: 20px;
  font-weight: 600;
  color: #f5222d;
}

.footer-right {
  display: flex;
  gap: 12px;
}

.shortcut-hint {
  margin-left: 4px;
  font-size: 12px;
  color: #8c8c8c;
}

.amount-text {
  font-weight: 600;
  color: #262626;
}

/* 紧凑尺寸覆盖 */
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
