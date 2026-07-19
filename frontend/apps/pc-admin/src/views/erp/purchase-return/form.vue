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
          <span class="remark-label">退货原因</span>
          <a-input
            v-model:value="formData.reason"
            size="small"
            class="remark-input"
            placeholder="请输入退货原因"
          />
        </div>
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
  PrinterOutlined, ClockCircleOutlined,
  PlusCircleOutlined, MinusCircleOutlined, SettingOutlined,
} from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, BillTabConfig, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { useUserStore } from '@/stores/user'
import { purchaseReturnApi } from '@/api/erp'

defineOptions({ name: 'PurchaseReturnForm' })
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
} = useBillForm({
  billPrefix: 'PR',
  // Ref: Odoo 18.0 Purchase Return - Form View
  api: {
    create: (data: any) => purchaseReturnApi.create(data),
    update: (id: number, data: any) => purchaseReturnApi.update(id, data),
    getById: async (id: number) => {
      const header = await purchaseReturnApi.getById(id)
      const items = await purchaseReturnApi.getItems(id).catch(() => [])
      return { ...header, items }
    },
  },
  redirectPath: '/purchase/return',
  optionTypes: ['suppliers', 'products'],
  productDefaults: {
    productCode: '', specification: '', unit: '',
    quantity: 0, unitPrice: 0,
  },
  transformPayload: (fd, status) => ({
    purchaseOrderId: fd.purchaseOrderId,
    purchaseOrderNo: fd.purchaseOrderNo,
    supplierId: fd.supplierId,
    supplierName: fd.supplierName,
    returnType: fd.returnType ?? 1,
    reason: fd.reason || '',
    remark: fd.remark || '',
    status,
    totalQuantity: totalQuantity.value,
    totalAmount: totalAmount.value,
    items: fd.products.filter((p: any) => p.productId != null).map((p: any) => ({
      productId: p.productId,
      productCode: p.productCode || '',
      productName: p.productName,
      productSpec: p.specification || '',
      productUnit: p.unit || '',
      returnQuantity: p.quantity || 0,
      unitPrice: p.unitPrice || 0,
      reason: p.reason || '',
      remark: p.remark || '',
    })),
  }),
})

// 初始化退货单特有字段
if (!('supplierId' in formData)) {Object.assign(formData, {
  supplierId: undefined, supplierName: '',
  purchaseOrderId: undefined, purchaseOrderNo: '',
  returnType: 1,
  date: '',
  reason: '', remark: '',
})}

// ── 页眉配置 ──
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '采购退货单',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined, children: [
      { key: 'print-return', label: '打印退货单' },
    ]},
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ],
}))

// ── 基本信息字段 ──
const basicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'supplierId', label: '供应商', type: 'select', required: true, width: 435, options: (optionRefs.suppliers || []).map((s: any) => ({ label: s.name, value: s.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'purchaseOrderNo', label: '源采购单号', type: 'input', width: 210, placeholder: '关联的采购订单号' },
  { key: 'purchaseOrderId', label: '源采购单ID', type: 'input', width: 100, hidden: true },
  { key: 'date', label: '退货日期', type: 'date', required: true, width: 210 },
  { key: 'returnType', label: '退货类型', type: 'select', required: true, width: 210, options: [
    { label: '质量退货', value: 1 },
    { label: '数量退货', value: 2 },
    { label: '其他退货', value: 3 },
  ]},
])

// ── 底部Tab ──
const tabsConfig = computed<BillTabConfig[]>(() => [
  { key: 'returnInfo', tab: '退货信息', fields: [
    { key: 'reason', label: '退货原因', type: 'input' },
    { key: 'remark', label: '备注', type: 'input' },
  ]},
])

// ── 摘要 ──
const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '退货数量', value: totalQuantity.value },
  { label: '退货金额', value: `¥${totalAmount.value.toFixed(2)}` },
])

// ── 页脚 ──
const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: `¥${totalAmount.value.toFixed(2)}`,
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
  { key: 'productCode', title: '货号', type: 'input', width: 100 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'unit', title: '单位', type: 'input', width: 80 },
  { key: 'quantity', title: '退货数量', type: 'number', width: 100, precision: 0 },
  { key: 'unitPrice', title: '单价', type: 'number', width: 100, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 100, precision: 2, readonly: true },
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
    row.productCode = p.code || ''
    row.specification = p.specification || ''
    row.unit = p.unit || ''
    row.unitPrice = p.purchasePrice || p.price || 0
  }
}

function handleInsertProduct(index: number) {
  handleAddProduct()
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
    case 'history': router.push('/purchase/return'); break
    case 'print-return': message.info('打印功能开发中'); break
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
  }
  if (query.purchaseOrderId) {
    formData.purchaseOrderId = Number(query.purchaseOrderId)
    formData.purchaseOrderNo = query.purchaseOrderNo || ''
  }
  if (!formData.date) {
    formData.date = new Date().toISOString().slice(0, 10)
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
