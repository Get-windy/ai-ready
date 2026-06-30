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
            <span v-for="n in 8" :key="n"></span>
            <span class="summary-red">{{ totalQuantity }}</span>
            <span class="summary-red">{{ totalAmount.toFixed(2) }}</span>
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
import { computed, ref, onMounted, nextTick, defineOptions } from 'vue'
import { PlusCircleOutlined, MinusCircleOutlined } from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, BillTabConfig, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { saleReturnApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'ReturnForm' })

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
} = useBillForm({
  billPrefix: 'RT',
  api: {
    create: saleReturnApi.create,
    update: saleReturnApi.update,
    getById: saleReturnApi.getById,
  },
  redirectPath: '/erp/return',
  optionTypes: ['customers', 'warehouses', 'users', 'products'],
  productDefaults: {
    itemCode: '', barcode: '', specification: '', unit: '',
    quantity: 0, unitPrice: 0, reason: '',
  },
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'customerId') {
      const c = optionRefs.customers.find((x: any) => x.id === val)
      if (c) {
        fd.customerName = c.name
      }
    }
  },
  transformPayload: (fd, status) => ({
    ...fd,
    status,
    customerId: fd.customerId,
    customerName: fd.customerName,
    saleOrderId: fd.saleOrderId,
    saleOrderNo: fd.saleOrderNo,
    returnType: fd.returnReason,
    reason: fd.returnReasonText,
    applicantId: fd.handlerId,
    applicantName: fd.handlerName,
    totalQuantity: totalQuantity.value,
    totalAmount: totalAmount.value,
    remark: fd.remark,
    items: fd.products.map((p: any) => ({
      productId: p.productId,
      productCode: p.itemCode,
      productName: p.productName,
      productSpec: p.specification,
      productUnit: p.unit,
      returnQuantity: p.quantity,
      unitPrice: p.unitPrice,
      lineAmount: (p.quantity || 0) * (p.unitPrice || 0),
      reason: p.reason,
      remark: p.remark,
    })),
  }),
})

// 初始化退货单特有字段
if (!('customerId' in formData)) Object.assign(formData, {
  customerId: undefined, customerName: '',
  warehouseId: undefined, warehouseName: '',
  handlerId: undefined, handlerName: '',
  date: '',
  returnReason: undefined,
  saleOrderNo: '',
  returnReasonText: '',
  refundMethod: undefined,
  refundAmount: 0,
  remark: '',
})

// BillFormPage 配置

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '退货单',
  orderNo: formData.orderNo,
  showAttachment: true,
}))

const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'customerId', label: '客户', type: 'select', required: true, placeholder: '请选择客户', options: optionRefs.customers.map((c: any) => ({ label: c.name, value: c.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'warehouseId', label: '退货仓库', type: 'select', required: true, placeholder: '请选择仓库', options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, placeholder: '请选择经手人', options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'date', label: '单据日期', type: 'date', required: true },
  { key: 'returnReason', label: '退货原因', type: 'select', options: [
    { label: '质量问题', value: 1 },
    { label: '错发货物', value: 2 },
    { label: '客户拒收', value: 3 },
    { label: '其他原因', value: 4 },
  ]},
  { key: 'saleOrderNo', label: '原单号', type: 'input', placeholder: '请输入原销售单号' },
])

const tabsConfig = computed<BillTabConfig[]>(() => [
  { key: 'refund', tab: '退款信息', fields: [
    { key: 'refundMethod', label: '退款方式', type: 'select', options: [
      { label: '原路退回', value: 1 },
      { label: '现金退款', value: 2 },
      { label: '冲抵货款', value: 3 },
    ]},
    { key: 'refundAmount', label: '退款金额', type: 'number', precision: 2 },
  ]},
])

const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '总数量', value: totalQuantity.value },
  { label: '退货金额', value: totalAmount.value.toFixed(2), divider: true },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '退货金额',
  amountValue: `${totalAmount.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: '保存草稿',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '提交',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// 明细表格列配置（无税列）
const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 70, fixed: 'left' },
  { key: 'productId', title: '商品名称', type: 'slot', slotName: 'productCell', width: 200 },
  { key: 'itemCode', title: '货号', type: 'input', width: 100 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'quantity', title: '退货数量', type: 'number', width: 100, precision: 2 },
  { key: 'unitPrice', title: '单价', type: 'number', width: 100, precision: 2 },
  { key: 'reason', title: '退货原因', type: 'input', width: 150 },
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
    row.unitPrice = p.salePrice || p.price || 0
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
