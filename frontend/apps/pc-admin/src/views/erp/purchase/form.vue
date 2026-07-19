<template>
  <BillFormPage
    v-model="formData"
    :header="headerConfig"
    :basic-info-fields="basicInfoFields"
    :tabs="tabsConfig"
    :summary="summaryConfig"
    :footer="footerConfig"
    @action="handleAction"
    @field-change="handleFieldChange"
    @draft="handleSaveDraft"
    @submit="handleSubmit"
  >
    <!-- 商品明细表格 -->
    <template #detail-table="{ onExpandChange }">
      <BillDetailTable
        :columns="detailColumns"
        :data-source="formData.products"
        :max-height="tableMaxHeight"
        :summary-columns="tableSummaryColumns"
        @cell-change="handleCellChange"
        @expand-change="onExpandChange"
      >
        <template #actionCell="{ index, empty }">
          <a-space :size="2">
            <a-button
              type="link"
              size="small"
              class="action-add-btn"
              @click="handleInsertProduct(index)"
            >
              <PlusCircleOutlined />
            </a-button>
            <a-button
              type="link"
              size="small"
              class="action-del-btn"
              @click="handleRemoveProduct(index)"
            >
              <MinusCircleOutlined />
            </a-button>
          </a-space>
        </template>
        <template #productCell="{ record, index }">
          <a-select
            v-model:value="record.productId"
            placeholder="搜索选择产品"
            show-search
            :filter-option="filterOption"
            style="width:100%"
            :loading="loadingOptions"
            size="small"
            @change="(val: number) => handleProductChange(val, index)"
          >
            <a-select-option
              v-for="p in optionRefs.products"
              :key="p.id"
              :value="p.id"
            >
              {{ p.name }}
            </a-select-option>
          </a-select>
        </template>
      </BillDetailTable>
    </template>

    <!-- 底部备注 -->
    <template #bottom-extra>
      <div class="remark-section">
        <div class="remark-row">
          <span class="remark-label">单据备注</span>
          <a-input
            v-model:value="formData.remark"
            size="small"
            class="remark-input"
          />
        </div>
      </div>
      <div class="doc-info-row">
        <span class="doc-info-item">制单人 <a-tag
          color="blue"
          size="small"
        >{{ currentUserName || '系统' }}</a-tag></span>
        <span class="doc-info-item">制单时间 {{ formatNow() }}</span>
      </div>
    </template>
  </BillFormPage>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import {
  PrinterOutlined, ClockCircleOutlined, ImportOutlined,
  PlusCircleOutlined, MinusCircleOutlined, SettingOutlined,
} from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, BillTabConfig, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

defineOptions({ name: 'PurchaseForm' })
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
const tableMaxHeight = ref(400)

// ── useBillForm ──
const {
  formData,
  loadingOptions,
  saving,
  optionRefs,
  filterOption,
  handleAddProduct,
  handleRemoveProduct,
  handleProductChange: baseProductChange,
  handleFieldChange: baseFieldChange,
  handleSaveDraft,
  handleSubmit,
  totalQuantity,
  totalAmount,
  totalTaxAmount,
  totalWithTax,
} = useBillForm({
  billPrefix: 'PO',
  // Ref: Odoo 18.0 Purchase Order - Form View, API pattern
  api: {
    create: (data: any) => request.post('/erp/purchase/order', data),
    update: (id: number, data: any) => request.put(`/erp/purchase/order/${id}`, data),
    getById: (id: number) => request.get(`/erp/purchase/order/${id}`),
  },
  redirectPath: '/purchase/order',
  optionTypes: ['suppliers', 'warehouses', 'users', 'products'],
  productDefaults: {
    itemCode: '', barcode: '', specification: '', unit: '',
    quantity: 0, unitPrice: 0, taxRate: 13, brand: '',
  },
  transformPayload: (fd, status) => ({
    supplierId: fd.supplierId,
    supplierName: fd.supplierName,
    warehouseId: fd.warehouseId,
    warehouseName: fd.warehouseName,
    purchaserId: fd.buyerId,
    purchaserName: fd.buyerName,
    orderDate: fd.date,
    deliveryDate: fd.deliveryDate || null,
    paymentMethod: fd.paymentMethod,
    deliveryAddress: fd.deliveryAddress,
    remark: fd.remark,
    status,
    totalAmount: totalAmount.value,
    taxAmount: totalTaxAmount.value,
    totalAmountWithTax: totalWithTax.value,
    totalQuantity: totalQuantity.value,
    items: fd.products.filter((p: any) => p.productId != null).map((p: any) => ({
      productId: p.productId,
      materialName: p.productName,
      productCode: p.itemCode,
      specification: p.specification,
      unit: p.unit,
      quantity: p.quantity,
      unitPrice: p.unitPrice,
      amount: (p.quantity || 0) * (p.unitPrice || 0),
      taxRate: p.taxRate,
      taxAmount: (p.quantity || 0) * (p.unitPrice || 0) * (p.taxRate || 0) / 100,
      brand: p.brand,
      itemNote: p.remark,
    })),
  }),
})

// 初始化采购订单特有字段
if (!('supplierId' in formData)) {Object.assign(formData, {
  supplierId: undefined, supplierName: '',
  warehouseId: undefined, warehouseName: '',
  buyerId: undefined, buyerName: '',
  date: '', purchaseType: 1,
  deliveryDate: '',
  contactName: '', contactPhone: '',
  paymentMethod: undefined, paymentAccount: '',
  deliveryAddress: '', logisticsCompany: '', freight: 0,
  remark: '',
})}

// ── 页眉配置 ──
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '采购订单',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined, children: [
      { key: 'print-order', label: '打印订单' },
    ]},
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'import', label: '导入', icon: ImportOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ],
}))

// ── 基本信息字段 ──
const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'supplierId', label: '供应商', type: 'select', required: true, width: 435, options: (optionRefs.suppliers || []).map((s: any) => ({ label: s.name, value: s.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'warehouseId', label: '入库仓库', type: 'select', required: true, width: 210, options: (optionRefs.warehouses || []).map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'buyerId', label: '采购员', type: 'select', required: true, width: 210, options: (optionRefs.users || []).map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'date', label: '单据日期', type: 'date', required: true, width: 210 },
  { key: 'purchaseType', label: '采购类型', type: 'select', required: true, width: 210, options: [
    { label: '正常采购', value: 1 },
    { label: '紧急采购', value: 2 },
    { label: '样品采购', value: 3 },
  ]},
  { key: 'deliveryDate', label: '交货日期', type: 'date', width: 210 },
  { key: 'contactName', label: '联系人', type: 'input', width: 160 },
  { key: 'contactPhone', label: '联系电话', type: 'input', width: 210 },
])

// ── 底部Tab ──
const tabsConfig = computed<BillTabConfig[]>(() => [
  { key: 'payment', tab: '付款信息', fields: [
    { key: 'paymentMethod', label: '付款方式', type: 'select', options: [
      { label: '现金', value: 'CASH' },
      { label: '银行转账', value: 'BANK_TRANSFER' },
      { label: '支票', value: 'CHECK' },
      { label: '月结', value: 'MONTHLY' },
    ]},
    { key: 'paymentAccount', label: '付款账户', type: 'input' },
  ]},
  { key: 'logistics', tab: '交货信息', fields: [
    { key: 'deliveryAddress', label: '交货地址', type: 'input', placeholder: '请输入交货地址' },
    { key: 'logisticsCompany', label: '物流公司', type: 'input', placeholder: '请输入物流公司' },
    { key: 'freight', label: '运费', type: 'number', precision: 2 },
  ]},
])

// ── 摘要 ──
const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '总数量', value: totalQuantity.value },
  { label: '商品金额', value: totalAmount.value.toFixed(2) },
  { label: '税额', value: totalTaxAmount.value.toFixed(2) },
  { label: '价税合计', value: totalWithTax.value.toFixed(2), divider: true },
])

// ── 页脚 ──
const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: `¥${totalWithTax.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: '保存草稿',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '提交',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ── 明细表格列 ──
const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 50, fixed: 'left' },
  { key: 'productId', title: '商品名称', type: 'slot', slotName: 'productCell', width: 200 },
  { key: 'itemCode', title: '货号', type: 'input', width: 100 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'brand', title: '品牌', type: 'input', width: 80 },
  { key: 'quantity', title: '数量', type: 'number', width: 90, precision: 0 },
  { key: 'unitPrice', title: '单价', type: 'number', width: 100, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'taxRate', title: '税率%', type: 'number', width: 70, precision: 0 },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
]

const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'amount', value: totalAmount.value.toFixed(2), highlight: true },
])

// ── 处理函数 ──
function handleProductChange(val: number, index: number) {
  baseProductChange(val, index)
  const p = optionRefs.products.find((x: any) => x.id === val)
  if (p && formData.products[index]) {
    const row = formData.products[index]
    row.itemCode = p.code || ''
    row.specification = p.specification || ''
    row.unit = p.unit || ''
    row.unitPrice = p.purchasePrice || p.price || 0
  }
}

function handleInsertProduct(index: number) {
  handleAddProduct()
  // 移动新增的行到指定位置之后
  const item = formData.products.pop()
  if (item) formData.products.splice(index + 1, 0, item)
}

function handleCellChange(rowIndex: number, fieldKey: string, value: any) {
  const item = formData.products?.[rowIndex]
  if (!item) return
  if (fieldKey === 'quantity' || fieldKey === 'unitPrice') {
    item.amount = (item.quantity || 0) * (item.unitPrice || 0)
  }
}

function handleAction(actionKey: string, _parentKey?: string) {
  switch (actionKey) {
    case 'history': router.push('/purchase/order'); break
    case 'print-order': message.info('打印功能开发中'); break
    case 'import': message.info('导入功能开发中'); break
    case 'config': message.info('列配置功能开发中'); break
    default: break
  }
}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  // 从查询参数预填供应商信息（如从供应商列表页跳转过来）
  const query = route.query
  if (query.supplierId) {
    formData.supplierId = Number(query.supplierId)
    formData.supplierName = query.supplierName || ''
    if (!formData.date) {
      formData.date = new Date().toISOString().slice(0, 10)
    }
  }
  if (query.warehouseId) {
    formData.warehouseId = Number(query.warehouseId)
    formData.warehouseName = query.warehouseName || ''
  }

  if (formData.products.length === 0) {
    handleAddProduct()
  }
  nextTick(() => {
    tableMaxHeight.value = Math.max(200, window.innerHeight - 420)
  })
})
</script>

<style scoped>
.action-add-btn { color: #1890ff; padding: 0; font-size: 14px; }
.action-del-btn { color: #ff4d4f; padding: 0; font-size: 14px; }
.remark-section { padding: 4px 0; }
.remark-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.remark-label { font-size: 12px; color: #595959; white-space: nowrap; min-width: 60px; }
.remark-input { flex: 1; }
.doc-info-row { display: flex; align-items: center; gap: 16px; padding: 6px 0; font-size: 12px; color: #8c8c8c; border-top: 1px solid #f0f0f0; }
</style>
