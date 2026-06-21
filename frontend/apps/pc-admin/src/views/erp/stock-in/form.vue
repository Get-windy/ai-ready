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
    <!-- Zone 3: 商品明细表格 -->
    <template #detail-table>
      <BillDetailTable
        :columns="detailColumns"
        :data-source="formData.products"
        :max-height="tableMaxHeight"
        @cell-change="handleCellChange"
      >
        <template #actionCell="{ index }">
          <a-space :size="2">
            <a-button type="link" size="small" class="action-add-btn" @click="handleAddProduct">
              <PlusCircleOutlined />
            </a-button>
            <a-button type="link" size="small" class="action-del-btn" @click="handleRemoveProduct(index)">
              <MinusCircleOutlined />
            </a-button>
          </a-space>
        </template>
        <template #productCell="{ record, index }">
          <div class="product-cell">
            <a-select
              v-model:value="record.productId"
              placeholder="请选择商品"
              show-search
              :filter-option="filterOption"
              style="flex:1"
              :loading="loadingOptions"
              size="small"
              @change="(val: number) => handleProductChange(val, index)"
            >
              <a-select-option v-for="p in optionRefs.products" :key="p.id" :value="p.id">
                {{ p.name }}
              </a-select-option>
            </a-select>
          </div>
        </template>
        <template #summary>
          <div class="table-summary-row">
            <span class="summary-label">合计</span>
            <span v-for="n in 9" :key="n"></span>
            <span class="summary-red">{{ totalQuantity }}</span>
            <span class="summary-red">{{ totalAmount.toFixed(2) }}</span>
            <span class="summary-red">{{ totalWithTax.toFixed(2) }}</span>
          </div>
        </template>
      </BillDetailTable>
    </template>

    <!-- Zone 4: 备注 -->
    <template #bottom-extra>
      <div class="remark-section">
        <div class="remark-row">
          <span class="remark-label">单据备注</span>
          <a-input v-model:value="formData.remark" size="small" class="remark-input" />
        </div>
      </div>
      <div class="doc-info-row">
        <span class="doc-info-item">制单人 <a-tag color="blue" size="small">{{ currentUserName || '系统' }}</a-tag></span>
        <span class="doc-info-item">制单时间 {{ formatNow() }}</span>
      </div>
    </template>
  </BillFormPage>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, nextTick } from 'vue'
import { PlusCircleOutlined, MinusCircleOutlined } from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, BillTabConfig, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { inboundApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
const tableMaxHeight = ref(400)

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
  billPrefix: 'IN',
  api: {
    create: inboundApi.create,
    update: inboundApi.update,
    getById: inboundApi.getById,
  },
  redirectPath: '/erp/stock-in',
  optionTypes: ['suppliers', 'warehouses', 'users', 'products'],
  productDefaults: {
    itemCode: '', barcode: '', specification: '', unit: '',
    quantity: 0, unitPrice: 0, taxRate: 13, batchNo: '',
  },
  transformPayload: (fd, status) => ({
    ...fd,
    status,
    supplierId: fd.supplierId,
    supplierName: fd.supplierName,
    inboundDate: fd.date,
    inboundType: fd.stockInType,
    warehouseId: fd.warehouseId,
    purchaserId: fd.handlerId,
    purchaserName: fd.handlerName,
    orderNo: fd.sourceOrderNo,
    totalAmount: totalAmount.value,
    taxAmount: totalTaxAmount.value,
    totalAmountWithTax: totalWithTax.value,
    remark: fd.remark,
    items: fd.products.map((p: any) => ({
      productId: p.productId,
      productCode: p.itemCode,
      productName: p.productName,
      productSpec: p.specification,
      productUnit: p.unit,
      inboundQuantity: p.quantity,
      unitPrice: p.unitPrice,
      lineAmount: (p.quantity || 0) * (p.unitPrice || 0),
      taxRate: p.taxRate,
      taxAmount: (p.quantity || 0) * (p.unitPrice || 0) * (p.taxRate || 0) / 100,
      lineTotal: (p.quantity || 0) * (p.unitPrice || 0) * (1 + (p.taxRate || 0) / 100),
      batchNo: p.batchNo,
      productionDate: p.productionDate || null,
      remark: p.remark,
    })),
  }),
})

// 初始化入库单特有字段
if (!('supplierId' in formData)) Object.assign(formData, {
  supplierId: undefined, supplierName: '',
  warehouseId: undefined, warehouseName: '',
  handlerId: undefined, handlerName: '',
  date: '', stockInType: 1,
  sourceOrderNo: '',
  paymentMethod: undefined, paymentAccount: '',
  remark: '',
})

// BillFormPage 配置

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '入库单',
  orderNo: formData.orderNo,
  showAttachment: true,
}))

const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'supplierId', label: '供应商', type: 'select', required: true, placeholder: '请选择供应商', options: optionRefs.suppliers.map((s: any) => ({ label: s.name, value: s.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'warehouseId', label: '入库仓库', type: 'select', required: true, placeholder: '请选择仓库', options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, placeholder: '请选择经手人', options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'date', label: '单据日期', type: 'date', required: true },
  { key: 'stockInType', label: '入库类型', type: 'select', required: true, options: [
    { label: '采购入库', value: 1 },
    { label: '退货入库', value: 2 },
    { label: '调拨入库', value: 3 },
    { label: '其他入库', value: 4 },
  ]},
  { key: 'sourceOrderNo', label: '来源单号', type: 'input', placeholder: '关联采购订单号' },
])

const tabsConfig = computed<BillTabConfig[]>(() => [
  { key: 'payment', tab: '付款信息', fields: [
    { key: 'paymentMethod', label: '付款方式', type: 'select', options: [
      { label: '现金', value: 1 },
      { label: '银行转账', value: 2 },
      { label: '支票', value: 3 },
      { label: '月结', value: 4 },
    ]},
    { key: 'paymentAccount', label: '付款账户', type: 'input' },
  ]},
])

const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '总数量', value: totalQuantity.value },
  { label: '入库金额', value: totalAmount.value.toFixed(2) },
  { label: '价税合计', value: totalWithTax.value.toFixed(2), divider: true },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '入库金额',
  amountValue: `${totalWithTax.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: '保存草稿',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '提交',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// 明细表格列配置
const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 70, fixed: 'left' },
  { key: 'productId', title: '商品名称', type: 'slot', slotName: 'productCell', width: 200 },
  { key: 'itemCode', title: '货号', type: 'input', width: 100 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'quantity', title: '数量', type: 'number', width: 90, precision: 2 },
  { key: 'unitPrice', title: '单价', type: 'number', width: 100, precision: 2 },
  { key: 'taxRate', title: '税率%', type: 'number', width: 80, precision: 1, min: 0, max: 100 },
  { key: 'batchNo', title: '批次号', type: 'input', width: 120 },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 110 },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
]

// 事件处理

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

function handleCellChange(_record: any, _fieldKey: string, _value: any) {}

function handleAction(_actionKey: string) {}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  if (formData.products.length === 0) {
    for (let i = 0; i < 5; i++) handleAddProduct()
  }
  nextTick(() => {
    tableMaxHeight.value = Math.max(200, window.innerHeight - 420)
  })
})
</script>

<style scoped>
.product-cell { display: flex; align-items: center; gap: 4px; }
.action-add-btn { color: #1890ff; padding: 0; font-size: 14px; }
.action-del-btn { color: #ff4d4f; padding: 0; font-size: 14px; }
.table-summary-row { display: flex; align-items: center; padding: 6px 12px; background: #fafafa; border-top: 1px solid #e8e8e8; font-size: 12px; font-weight: 600; flex-shrink: 0; }
.summary-label { font-weight: 700; color: #262626; min-width: 60px; }
.summary-red { color: #ff4d4f; font-weight: 700; min-width: 50px; text-align: right; }
.remark-section { padding: 4px 0; }
.remark-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.remark-label { font-size: 12px; color: #595959; white-space: nowrap; min-width: 60px; }
.remark-input { flex: 1; }
.doc-info-row { display: flex; align-items: center; gap: 16px; padding: 6px 0; font-size: 12px; color: #8c8c8c; border-top: 1px solid #f0f0f0; }
</style>
