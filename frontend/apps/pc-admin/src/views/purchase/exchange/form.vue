<template>
  <div
    class="form-page-container"
    style="height:100%;display:flex;flex-direction:column;"
  >
    <BillFormPage
      v-model="formData"
      :header="headerConfig"
      :basic-info-fields="basicInfoFields"
      :tabs="tabsConfig"
      :summary="summaryConfig"
      :footer="footerConfig"
      @action="handleAction"
      @field-change="handleFieldChange"
      @search-btn="handleSearchBtn"
      @draft="handleSaveDraft"
      @submit="handleSubmit"
    >
      <!-- ═══ 换入仓库数据表格 ═══ -->
      <template #detail-table="{ onExpandChange }">
        <div class="warehouse-section">
          <div class="warehouse-title">
            <span>换入仓库</span>
            <span class="warehouse-count">{{ formData.inWarehouseItems?.length || 0 }} 行</span>
          </div>
          <BillDetailTable
            :columns="inWarehouseColumns"
            v-model:data-source="formData.inWarehouseItems"
            :summary-columns="inWarehouseSummaryColumns"
            :view-mode="isLocked"
            @cell-change="handleInWarehouseCellChange"
            @expand-change="onExpandChange"
            @open-select-modal="handleOpenInWarehouseProductModal"
          >
            <template #actionCell="{ index, empty }">
              <template v-if="isLocked"></template>
              <template v-else-if="!empty">
                <a-space :size="2">
                  <a-button type="link" size="small" class="action-add-btn" @click="handleInsertInWarehouseProduct(index)">
                    <PlusCircleOutlined />
                  </a-button>
                  <a-button type="link" size="small" class="action-del-btn" @click="handleRemoveInWarehouseProduct(index)">
                    <MinusCircleOutlined />
                  </a-button>
                </a-space>
              </template>
              <template v-else>
                <a-space :size="2">
                  <a-button type="link" size="small" class="action-add-btn" @click="handleAddInWarehouseProduct()">
                    <PlusCircleOutlined />
                  </a-button>
                  <a-button type="link" size="small" class="action-del-btn" disabled>
                    <MinusCircleOutlined />
                  </a-button>
                </a-space>
              </template>
            </template>
          </BillDetailTable>
        </div>

        <!-- ═══ 换出仓库数据表格 ═══ -->
        <div class="warehouse-section">
          <div class="warehouse-title">
            <span>换出仓库</span>
            <span class="warehouse-count">{{ formData.outWarehouseItems?.length || 0 }} 行</span>
          </div>
          <BillDetailTable
            :columns="outWarehouseColumns"
            v-model:data-source="formData.outWarehouseItems"
            :summary-columns="outWarehouseSummaryColumns"
            :view-mode="isLocked"
            @cell-change="handleOutWarehouseCellChange"
            @expand-change="onExpandChange"
            @open-select-modal="handleOpenOutWarehouseProductModal"
          >
            <template #actionCell="{ index, empty }">
              <template v-if="isLocked"></template>
              <template v-else-if="!empty">
                <a-space :size="2">
                  <a-button type="link" size="small" class="action-add-btn" @click="handleInsertOutWarehouseProduct(index)">
                    <PlusCircleOutlined />
                  </a-button>
                  <a-button type="link" size="small" class="action-del-btn" @click="handleRemoveOutWarehouseProduct(index)">
                    <MinusCircleOutlined />
                  </a-button>
                </a-space>
              </template>
              <template v-else>
                <a-space :size="2">
                  <a-button type="link" size="small" class="action-add-btn" @click="handleAddOutWarehouseProduct()">
                    <PlusCircleOutlined />
                  </a-button>
                  <a-button type="link" size="small" class="action-del-btn" disabled>
                    <MinusCircleOutlined />
                  </a-button>
                </a-space>
              </template>
            </template>
          </BillDetailTable>
        </div>
      </template>

      <!-- ═══ 备注 + 单据信息 ═══ -->
      <template #bottom-extra>
        <div class="remark-section">
          <div class="remark-row">
            <span class="remark-label">单据备注</span>
            <a-input v-model:value="formData.remark" size="small" class="remark-input" placeholder="请输入单据备注" :disabled="isLocked" />
          </div>
          <div class="remark-row">
            <span class="remark-label">供应商备注</span>
            <a-input v-model:value="formData.supplierRemark" size="small" class="remark-input" placeholder="供应商备注" :disabled="isLocked" />
          </div>
        </div>
        <div class="doc-info-row">
          <span class="doc-info-item">单据状态 <a-tag color="blue" size="small">{{ statusText }}</a-tag></span>
          <span class="doc-info-item">制单人 <a-tag color="blue">{{ currentUserName || '系统' }}</a-tag></span>
          <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
          <span class="doc-info-item">
            源单 <a-tag>{{ formData.sourceOrderNo || '-' }}</a-tag>
          </span>
          <a-button type="link" size="small" class="doc-info-link" @click="handlePrintRecord">
            打印记录
          </a-button>
        </div>
      </template>
    </BillFormPage>

    <!-- ═══ 产品选择弹窗 ═══ -->
    <ProductSelectModal
      v-model:open="showProductSelect"
      :multiple="true"
      @confirm="handleProductSelectConfirm"
    />

    <!-- ═══ 配置弹窗（顶部齿轮触发）：页面配置 / 录单默认值 / 打印设置 ═══ -->
    <FormPageConfigModal
      v-model:open="showFormConfig"
      :fields="pageConfigFields"
      :table-columns="pageConfigTableColumns"
      v-model:print-config="printConfig"
      @field-visible-change="setFieldVisible"
      @field-enter-jump-change="setEnterJump"
      @field-display-name-change="setDisplayName"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PrinterOutlined,
  ClockCircleOutlined,
  ImportOutlined,
  ExportOutlined,
  SettingOutlined,
  PlusCircleOutlined,
  MinusCircleOutlined,
} from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import ProductSelectModal from '@/components/ProductSelectModal/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, BillTabConfig, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { useFormPageConfig } from '@/components/BillFormPage/useFormPageConfig'
import FormPageConfigModal from '@/components/BillFormPage/FormPageConfigModal.vue'
import { purchaseExchangeApi } from '@/api/purchase-exchange'
import { PRODUCT_PURCHASE_DEFAULTS } from '@/utils/productDefaults'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'PurchaseExchangeForm' })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')

// ═══ 弹窗状态 ═══
const showProductSelect = ref(false)
const currentSelectRowIndex = ref(-1)
const currentWarehouseType = ref<'in' | 'out'>('in')
// ═══════════════════════════════════════
// useBillForm composable（仅负责状态/选项/编号）
// ═══════════════════════════════════════
const {
  formData,
  loadingOptions,
  saving,
  optionRefs,
  handleFieldChange: baseFieldChange,
  effectiveMode,
} = useBillForm({
  billPrefix: 'CGHD',
  api: {
    create: purchaseExchangeApi.create,
    update: purchaseExchangeApi.update,
    getById: purchaseExchangeApi.getById,
  },
  redirectPath: '/purchase/exchange',
  optionTypes: ['suppliers', 'warehouses', 'users', 'products'],
  productDefaults: PRODUCT_PURCHASE_DEFAULTS,
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'supplierId') {
      const s = optionRefs.suppliers.find((x: any) => x.id === val)
      if (s) {
        fd.supplierName = s.name || ''
        fd.supplierCode = s.code || s.supplierCode || ''
        fd.contactName = s.contactName || s.contact || ''
        fd.contactPhone = s.contactPhone || s.phone || ''
        fd.contactAddress = s.address || s.contactAddress || ''
        fd.bankName = s.bankName || ''
        fd.bankAccount = s.bankAccount || ''
        fd.taxNo = s.taxNo || ''
        fd.supplierRemark = s.remark || ''
        fd.prevPrepaid = s.balance || s.prepaidBalance || 0
        fd.prevDebt = s.balance || 0
      }
    }
    if (fieldKey === 'handlerId') {
      const u = optionRefs.users.find((x: any) => x.id === val)
      if (u) {
        fd.handlerName = u.name || ''
        fd.deptId = u.deptId
        fd.deptName = u.deptName || ''
      }
    }
    if (fieldKey === 'inWarehouseId') {
      const w = optionRefs.warehouses.find((x: any) => x.id === val)
      if (w) fd.inWarehouseName = w.name || ''
    }
    if (fieldKey === 'outWarehouseId') {
      const w = optionRefs.warehouses.find((x: any) => x.id === val)
      if (w) fd.outWarehouseName = w.name || ''
    }
  },
  // 详情加载后把嵌套DTO映射到顶层（exchangeNo→orderNo、items→双仓库）
  onDetailLoaded: (data, fd) => {
    if (data.exchangeNo) fd.orderNo = data.exchangeNo
    if (data.sourceOrderNo) fd.sourceOrderNo = data.sourceOrderNo
    if (data.sourceOrderId) fd.sourceOrderId = data.sourceOrderId
    if (Array.isArray(data.items)) {
      fd.inWarehouseItems = data.items
        .filter((it: any) => (it.warehouseType === 2 ? false : true))
        .map((it: any) => ({ ...createEmptyItem(), ...it, warehouseType: 1 }))
      fd.outWarehouseItems = data.items
        .filter((it: any) => it.warehouseType === 2)
        .map((it: any) => ({ ...createEmptyItem(), ...it, warehouseType: 2 }))
    }
  },
})

// ── 初始化采购换货单特有字段 ──
if (!('supplierId' in formData)) {
  Object.assign(formData, {
    supplierId: undefined, supplierName: '', supplierCode: '', supplierRemark: '',
    inWarehouseId: undefined, inWarehouseName: '',
    outWarehouseId: undefined, outWarehouseName: '',
    handlerId: undefined, handlerName: '',
    deptId: undefined, deptName: '',
    exchangeDate: new Date().toISOString().slice(0, 10),
    bankName: '', bankAccount: '', taxNo: '',
    contactName: '', contactPhone: '', contactAddress: '',
    paymentAccount: undefined, paidAmount: 0,
    moreAccounts: '', prevPrepaid: 0, usePrepaid: 0, prepaidBalance: 0,
    prevDebt: 0, currentDebt: 0, debtBalance: 0, paymentDeadline: '',
    sourceOrderId: undefined, sourceOrderNo: '', sourceOrderType: '',
    remark: '', summary: '', printCount: 0, attachment: '',
    inWarehouseItems: [] as any[],
    outWarehouseItems: [] as any[],
  })
}

// 状态枚举（0 草稿 / 1 待审批 / 2 已审批 / 3 换货中 / 4 已完成 / 5 已拒绝 / 6 已取消）
const EXCHANGE_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'blue' },
  3: { text: '换货中', color: 'processing' },
  4: { text: '已完成', color: 'success' },
  5: { text: '已拒绝', color: 'error' },
  6: { text: '已取消', color: 'error' },
}

const currentStatus = computed(() => Number(formData.status ?? 0))
const statusText = computed(() => EXCHANGE_STATUS_MAP[currentStatus.value]?.text || '草稿')
const isLocked = computed(() => effectiveMode.value === 'edit' && currentStatus.value !== 0)

// ═══════════════════════════════════════
// 明细行构建辅助
// ═══════════════════════════════════════
function createEmptyItem() {
  return {
    id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
    warehouseType: 1,
    productId: undefined,
    productCode: '', productName: '', barcode: '',
    specification: '', model: '', origin: '', brand: '',
    unit: '', image: '', location: '', area: '',
    availableStock: 0, stockConverted: 0, bookStock: 0,
    batchBarcode: '', productionDate: null, shelfLife: 0, expiryDate: null,
    quantity: 0, conversionRate: '', pieceScatterQty: 0,
    largePackage: 0, mediumPackage: 0, smallPackage: 0,
    recentPurchaseDate: null, retailPrice: 0, wholesalePrice: 0,
    unitPrice: 0, amount: 0,
    smallUnit: '', smallUnitPrice: 0, smallUnitQty: 0,
    costPrice: 0, costAmount: 0,
    discount: 100, discountPrice: 0, discountAmount: 0,
    volume: 0, weight: 0,
    isGift: false, remark: '',
    priceLevel1: 0, priceLevel2: 0, priceLevel3: 0, priceLevel4: 0,
    priceLevel5: 0, priceLevel6: 0, priceLevel7: 0, priceLevel8: 0,
    extNum1: 0, extNum2: 0, extNum3: 0, extNum4: 0, extNum5: 0,
    extNum6: 0, extNum7: 0, extNum8: 0, extNum9: 0, extNum10: 0,
    extText1: '', extText2: '', extText3: '', extText4: '', extText5: '', extText6: '',
    extPartner: null, extStaff: null, extDept: null,
  }
}

function fillProductInfo(row: any, p: any) {
  row.productId = p.id
  row.productName = p.name || ''
  row.productCode = p.code || ''
  row.itemCode = p.code || ''
  row.barcode = p.barcode || ''
  row.specification = p.specification || ''
  row.brand = p.brand || ''
  row.origin = p.origin || ''
  row.unit = p.unit || ''
  row.location = p.location || ''
  row.area = p.area || ''
  row.image = p.image || ''
  row.retailPrice = p.retailPrice || p.salePrice || 0
  row.wholesalePrice = p.wholesalePrice || p.salePrice || p.purchasePrice || 0
  row.costPrice = p.purchasePrice || p.costPrice || 0
  row.smallUnit = p.smallUnit || ''
  row.conversionRate = p.conversionRate || ''
  row.unitPrice = p.purchasePrice || p.costPrice || 0
  calcRowAmount(row)
}

function calcRowAmount(row: any) {
  const qty = row.quantity || 0
  const price = row.unitPrice || 0
  row.amount = qty * price
  const disc = row.discount || 100
  row.discountPrice = price * (disc / 100)
  row.discountAmount = qty * row.discountPrice
  row.costAmount = qty * (row.costPrice || 0)
}

// ═══ 双仓库合计 ═══
const inTotalQuantity = computed(() => (formData.inWarehouseItems || []).reduce((s: number, p: any) => s + (p.quantity || 0), 0))
const outTotalQuantity = computed(() => (formData.outWarehouseItems || []).reduce((s: number, p: any) => s + (p.quantity || 0), 0))
const inTotalAmount = computed(() => (formData.inWarehouseItems || []).reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0), 0))
const outTotalAmount = computed(() => (formData.outWarehouseItems || []).reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0), 0))
const inTotalDiscountAmount = computed(() => (formData.inWarehouseItems || []).reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0) * ((p.discount || 100) / 100), 0))
const outTotalDiscountAmount = computed(() => (formData.outWarehouseItems || []).reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0) * ((p.discount || 100) / 100), 0))
const combinedDiscountAmount = computed(() => inTotalDiscountAmount.value + outTotalDiscountAmount.value)
const totalWeight = computed(() => (formData.inWarehouseItems || []).concat(formData.outWarehouseItems || []).reduce((s: number, p: any) => s + (p.weight || 0), 0))
const totalVolume = computed(() => (formData.inWarehouseItems || []).concat(formData.outWarehouseItems || []).reduce((s: number, p: any) => s + (p.volume || 0), 0))

const inWarehouseSummaryColumns = computed(() => [
  { key: 'quantity', value: inTotalQuantity.value, highlight: true },
  { key: 'amount', value: inTotalAmount.value, highlight: true },
  { key: 'discountAmount', value: inTotalDiscountAmount.value, highlight: true },
])
const outWarehouseSummaryColumns = computed(() => [
  { key: 'quantity', value: outTotalQuantity.value, highlight: true },
  { key: 'amount', value: outTotalAmount.value, highlight: true },
  { key: 'discountAmount', value: outTotalDiscountAmount.value, highlight: true },
])

// ═══════════════════════════════════════
// BillFormPage 配置
// ═══════════════════════════════════════
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '采购换货单',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'import', label: '导入', icon: ImportOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
    { key: 'export', label: '导出', icon: ExportOutlined },
  ],
}))

// 基本信息字段（静态定义；动态选项由 decorate 注入）
const BASE_INFO_FIELDS: BasicInfoField[] = [
  { key: 'orderNo', label: '编号', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'supplierId', label: '供应商', type: 'select', required: true, inlineLabel: true, width: 300 },
  { key: 'supplierCode', label: '供应商编号', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'bankName', label: '开户行', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'bankAccount', label: '银行账号', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'taxNo', label: '税号', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'inWarehouseId', label: '换入仓库', type: 'select', required: true, inlineLabel: true, width: 210 },
  { key: 'outWarehouseId', label: '换出仓库', type: 'select', required: true, inlineLabel: true, width: 210 },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 210 },
  { key: 'deptName', label: '部门', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'exchangeDate', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 210 },
  { key: 'contactName', label: '联系人', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'contactPhone', label: '联系电话', type: 'input', inlineLabel: true, width: 210, readonly: true },
  { key: 'contactAddress', label: '联系地址', type: 'input', inlineLabel: true, width: 435, readonly: true },
  { key: 'summary', label: '摘要', type: 'input', inlineLabel: true, width: 300 },
]

// ═══ 页面配置弹窗（页面配置/录单默认值/打印设置）═══
// 原先顶部「配置」按钮只弹一句「请使用底部Tab中的列配置功能」，没有任何配置面板；
// 现接入共享的 useFormPageConfig + FormPageConfigModal。
const FORM_CONFIG_MODULE = 'purchase-exchange-form'
const showFormConfig = ref(false)
const printConfig = reactive({
  template: 'standard', copies: 1, paperSize: 'A4',
  alwaysLastTemplate: false, afterSubmit: false,
})

const {
  basicInfoFields,
  pageConfigFields,
  pageConfigTableColumns,
  loadConfig: loadFormConfig,
  setFieldVisible,
  setEnterJump,
  setDisplayName,
} = useFormPageConfig({
  baseFields: BASE_INFO_FIELDS,
  module: FORM_CONFIG_MODULE,
  decorate: (f) => {
    switch (f.key) {
      case 'supplierId':
        return { options: (optionRefs.suppliers || []).map((s: any) => ({ label: s.name, value: s.id })), searchBtn: '+Q', loading: loadingOptions.value }
      case 'inWarehouseId':
      case 'outWarehouseId':
        return { options: (optionRefs.warehouses || []).map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value }
      case 'handlerId':
        return { options: (optionRefs.users || []).map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value }
      default:
        return {}
    }
  },
  collectDefaults: () => ({ ...printConfig }),
  applyDefaults: (d) => {
    if (d.template) printConfig.template = d.template
    if (d.copies != null) printConfig.copies = Number(d.copies)
    if (d.paperSize) printConfig.paperSize = d.paperSize
    printConfig.alwaysLastTemplate = !!d.alwaysLastTemplate
    printConfig.afterSubmit = !!d.afterSubmit
  },
})

const tabsConfig = computed<BillTabConfig[]>(() => [
  {
    key: 'payment',
    tab: '付款',
    fields: [
      { key: 'paymentAccount', label: '付款账户', type: 'select', inlineLabel: true, width: 210 },
      { key: 'paidAmount', label: '付款金额', type: 'number', inlineLabel: true, width: 210, precision: 2 },
      { key: 'moreAccounts', label: '更多账户', type: 'input', inlineLabel: true, width: 210, readonly: true },
      { key: 'prevPrepaid', label: '此前预付', type: 'number', inlineLabel: true, width: 210, precision: 2, readonly: true },
      { key: 'usePrepaid', label: '使用预付款', type: 'number', inlineLabel: true, width: 210, precision: 2 },
      { key: 'prepaidBalance', label: '预付余额', type: 'number', inlineLabel: true, width: 210, precision: 2, readonly: true },
      { key: 'prevDebt', label: '此前欠款', type: 'number', inlineLabel: true, width: 210, precision: 2, readonly: true },
      { key: 'currentDebt', label: '本次欠款', type: 'number', inlineLabel: true, width: 210, precision: 2, readonly: true },
      { key: 'debtBalance', label: '欠款余额', type: 'number', inlineLabel: true, width: 210, precision: 2, readonly: true },
      { key: 'paymentDeadline', label: '付款期限', type: 'date', inlineLabel: true, width: 210 },
    ],
  },
  {
    key: 'source',
    tab: '源单',
    fields: [
      { key: 'sourceOrderType', label: '源单类型', type: 'select', inlineLabel: true, width: 210, options: [
        { label: '采购订单', value: 'purchaseOrder' },
        { label: '采购入库单', value: 'purchaseInbound' },
      ] },
      { key: 'sourceOrderId', label: '源单', type: 'select', inlineLabel: true, width: 435 },
    ],
  },
])

const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '换入数量', value: inTotalQuantity.value },
  { label: '换出数量', value: outTotalQuantity.value },
  { label: '商品金额', value: (inTotalAmount.value + outTotalAmount.value).toFixed(2) },
  { label: '折后金额', value: combinedDiscountAmount.value.toFixed(2) },
  { label: '总重量(kg)', value: totalWeight.value.toFixed(2) },
  { label: '总体积(m³)', value: totalVolume.value.toFixed(2) },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: `\u00a5${combinedDiscountAmount.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: '保存草稿',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '记账',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══════════════════════════════════════
// 换入仓库数据表格列配置（57列，价格等级在前）
// ═══════════════════════════════════════
const inWarehouseColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 50, fixed: 'left' },
  { key: 'image', title: '图片', type: 'input', width: 60 },
  {
    key: 'productId', title: '商品名称', type: 'input', searchable: true,
    options: optionRefs.products.map((p: any) => ({ label: p.name, value: p.id, searchText: `${p.name} ${p.code || ''} ${p.barcode || ''}` })),
    width: 200, showScanToggle: true,
  },
  { key: 'productCode', title: '货号', type: 'input', width: 100 },
  { key: 'barcode', title: '条码', type: 'input', width: 120 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'model', title: '型号', type: 'input', width: 100 },
  { key: 'origin', title: '产地', type: 'input', width: 100 },
  { key: 'brand', title: '品牌', type: 'input', width: 100 },
  { key: 'area', title: '区域', type: 'input', width: 80 },
  { key: 'location', title: '货位', type: 'input', width: 80 },
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'availableStock', title: '可用库存', type: 'number', width: 90, precision: 2, readonly: true },
  { key: 'stockConverted', title: '可用库存换算结果', type: 'number', width: 130, precision: 2, readonly: true },
  { key: 'bookStock', title: '账面库存', type: 'number', width: 90, precision: 2, readonly: true },
  { key: 'batchBarcode', title: '批次条码', type: 'input', width: 110 },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 110 },
  { key: 'shelfLife', title: '保质期', type: 'number', width: 80, precision: 0 },
  { key: 'expiryDate', title: '到期日期', type: 'date', width: 110 },
  { key: 'quantity', title: '数量', type: 'number', width: 90, precision: 2 },
  { key: 'conversionRate', title: '换算关系', type: 'input', width: 80 },
  { key: 'pieceScatterQty', title: '件散数量', type: 'number', width: 90, precision: 2 },
  { key: 'largePackage', title: '大包装', type: 'number', width: 80, precision: 0 },
  { key: 'mediumPackage', title: '中包装', type: 'number', width: 80, precision: 0 },
  { key: 'smallPackage', title: '小包装', type: 'number', width: 80, precision: 0 },
  { key: 'recentPurchaseDate', title: '最近采购日期', type: 'date', width: 120, readonly: true },
  { key: 'retailPrice', title: '零售价', type: 'number', width: 90, precision: 2 },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 90, precision: 2 },
  { key: 'unitPrice', title: '单价', type: 'number', width: 90, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 90, precision: 2, align: 'right', readonly: true },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 80 },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 100, precision: 2 },
  { key: 'smallUnitQty', title: '小单位数量', type: 'number', width: 100, precision: 2 },
  { key: 'costPrice', title: '成本单价', type: 'number', width: 100, precision: 2 },
  { key: 'costAmount', title: '成本金额', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'volume', title: '体积(m³)', type: 'number', width: 90, precision: 4 },
  { key: 'weight', title: '重量(kg)', type: 'number', width: 90, precision: 4 },
  { key: 'isGift', title: '赠品', type: 'checkbox', width: 60 },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
  { key: 'priceLevel1', title: '餐饮店', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel2', title: '食堂团餐', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel3', title: '外围餐饮店', type: 'number', width: 100, precision: 2 },
  { key: 'priceLevel4', title: '自助vip', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel5', title: '大团餐', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel6', title: '重点|vip01', type: 'number', width: 100, precision: 2 },
  { key: 'priceLevel7', title: '连锁|vip', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel8', title: '特价客户', type: 'number', width: 90, precision: 2 },
  { key: 'extNum1', title: '单据自定义1(数字)', type: 'number', width: 140, precision: 2 },
  { key: 'extNum2', title: '单据自定义2(数字)', type: 'number', width: 140, precision: 2 },
  { key: 'extNum3', title: '单据自定义3(数字)', type: 'number', width: 140, precision: 2 },
  { key: 'extText1', title: '单据自定义4(文本)', type: 'input', width: 140 },
  { key: 'extText2', title: '单据自定义5(文本)', type: 'input', width: 140 },
  { key: 'extNum4', title: '单据自定义6(数字)', type: 'number', width: 140, precision: 2 },
  { key: 'extNum5', title: '单据自定义7(数字)', type: 'number', width: 140, precision: 2 },
  { key: 'extPartner', title: '单据自定义8(往来单位)', type: 'select', width: 130 },
  { key: 'extStaff', title: '单据自定义9(职员)', type: 'select', width: 120 },
  { key: 'extDept', title: '单据自定义10(部门)', type: 'select', width: 120 },
])

// ═══════════════════════════════════════
// 换出仓库数据表格列配置（57列，自定义字段在前、价格等级在后）
// ═══════════════════════════════════════
const outWarehouseColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 50, fixed: 'left' },
  { key: 'image', title: '图片', type: 'input', width: 60 },
  {
    key: 'productId', title: '商品名称', type: 'input', searchable: true,
    options: optionRefs.products.map((p: any) => ({ label: p.name, value: p.id, searchText: `${p.name} ${p.code || ''} ${p.barcode || ''}` })),
    width: 200, showScanToggle: true,
  },
  { key: 'productCode', title: '货号', type: 'input', width: 100 },
  { key: 'location', title: '货位', type: 'input', width: 80 },
  { key: 'barcode', title: '条码', type: 'input', width: 120 },
  { key: 'area', title: '区域', type: 'input', width: 80 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'model', title: '型号', type: 'input', width: 100 },
  { key: 'origin', title: '产地', type: 'input', width: 100 },
  { key: 'brand', title: '品牌', type: 'input', width: 100 },
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'availableStock', title: '可用库存', type: 'number', width: 90, precision: 2, readonly: true },
  { key: 'stockConverted', title: '可用库存换算结果', type: 'number', width: 130, precision: 2, readonly: true },
  { key: 'bookStock', title: '账面库存', type: 'number', width: 90, precision: 2, readonly: true },
  { key: 'batchBarcode', title: '批次条码', type: 'input', width: 110 },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 110 },
  { key: 'shelfLife', title: '保质期', type: 'number', width: 80, precision: 0 },
  { key: 'expiryDate', title: '到期日期', type: 'date', width: 110 },
  { key: 'quantity', title: '数量', type: 'number', width: 90, precision: 2 },
  { key: 'conversionRate', title: '换算关系', type: 'input', width: 80 },
  { key: 'pieceScatterQty', title: '件散数量', type: 'number', width: 90, precision: 2 },
  { key: 'largePackage', title: '大包装', type: 'number', width: 80, precision: 0 },
  { key: 'mediumPackage', title: '中包装', type: 'number', width: 80, precision: 0 },
  { key: 'smallPackage', title: '小包装', type: 'number', width: 80, precision: 0 },
  { key: 'recentPurchaseDate', title: '最近采购日期', type: 'date', width: 120, readonly: true },
  { key: 'retailPrice', title: '零售价', type: 'number', width: 90, precision: 2 },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 90, precision: 2 },
  { key: 'unitPrice', title: '单价', type: 'number', width: 90, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 90, precision: 2, align: 'right', readonly: true },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 80 },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 100, precision: 2 },
  { key: 'smallUnitQty', title: '小单位数量', type: 'number', width: 100, precision: 2 },
  { key: 'costPrice', title: '成本单价', type: 'number', width: 100, precision: 2 },
  { key: 'costAmount', title: '成本金额', type: 'number', width: 100, precision: 2, readonly: true },
  { key: 'volume', title: '体积(m³)', type: 'number', width: 90, precision: 4 },
  { key: 'weight', title: '重量(kg)', type: 'number', width: 90, precision: 4 },
  { key: 'isGift', title: '赠品', type: 'checkbox', width: 60 },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
  { key: 'extNum1', title: '单据自定义1(数字)', type: 'number', width: 140, precision: 2 },
  { key: 'extNum2', title: '单据自定义2(数字)', type: 'number', width: 140, precision: 2 },
  { key: 'extNum3', title: '单据自定义3(数字)', type: 'number', width: 140, precision: 2 },
  { key: 'extText1', title: '单据自定义4(文本)', type: 'input', width: 140 },
  { key: 'extText2', title: '单据自定义5(文本)', type: 'input', width: 140 },
  { key: 'extNum4', title: '单据自定义6(数字)', type: 'number', width: 140, precision: 2 },
  { key: 'extNum5', title: '单据自定义7(数字)', type: 'number', width: 140, precision: 2 },
  { key: 'extPartner', title: '单据自定义8(往来单位)', type: 'select', width: 130 },
  { key: 'extStaff', title: '单据自定义9(职员)', type: 'select', width: 120 },
  { key: 'extDept', title: '单据自定义10(部门)', type: 'select', width: 120 },
  { key: 'priceLevel1', title: '餐饮店', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel2', title: '食堂团餐', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel3', title: '外围餐饮店', type: 'number', width: 100, precision: 2 },
  { key: 'priceLevel4', title: '自助vip', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel5', title: '大团餐', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel6', title: '重点|vip01', type: 'number', width: 100, precision: 2 },
  { key: 'priceLevel7', title: '连锁|vip', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel8', title: '特价客户', type: 'number', width: 90, precision: 2 },
])

// ═══════════════════════════════════════
// 提交数据构建与校验
// ═══════════════════════════════════════
function buildItemPayload(p: any, warehouseType: number) {
  return {
    warehouseType,
    productId: p.productId,
    productCode: p.productCode || '',
    productName: p.productName || '',
    barcode: p.barcode || '',
    specification: p.specification || '',
    model: p.model || '',
    origin: p.origin || '',
    brand: p.brand || '',
    unit: p.unit || '',
    image: p.image || '',
    location: p.location || '',
    area: p.area || '',
    availableStock: p.availableStock || 0,
    stockConverted: p.stockConverted || 0,
    bookStock: p.bookStock || 0,
    batchBarcode: p.batchBarcode || '',
    productionDate: p.productionDate || null,
    shelfLife: p.shelfLife || 0,
    expiryDate: p.expiryDate || null,
    quantity: p.quantity || 0,
    conversionRate: p.conversionRate || '',
    pieceScatterQty: p.pieceScatterQty || 0,
    largePackage: p.largePackage || 0,
    mediumPackage: p.mediumPackage || 0,
    smallPackage: p.smallPackage || 0,
    recentPurchaseDate: p.recentPurchaseDate || null,
    retailPrice: p.retailPrice || 0,
    wholesalePrice: p.wholesalePrice || 0,
    unitPrice: p.unitPrice || 0,
    amount: (p.quantity || 0) * (p.unitPrice || 0),
    smallUnit: p.smallUnit || '',
    smallUnitPrice: p.smallUnitPrice || 0,
    smallUnitQty: p.smallUnitQty || 0,
    costPrice: p.costPrice || 0,
    costAmount: (p.quantity || 0) * (p.costPrice || 0),
    discount: p.discount ?? 100,
    discountPrice: (p.unitPrice || 0) * ((p.discount ?? 100) / 100),
    discountAmount: (p.quantity || 0) * (p.unitPrice || 0) * ((p.discount ?? 100) / 100),
    volume: p.volume || 0,
    weight: p.weight || 0,
    isGift: p.isGift || false,
    remark: p.remark || '',
    priceLevel1: p.priceLevel1 || 0,
    priceLevel2: p.priceLevel2 || 0,
    priceLevel3: p.priceLevel3 || 0,
    priceLevel4: p.priceLevel4 || 0,
    priceLevel5: p.priceLevel5 || 0,
    priceLevel6: p.priceLevel6 || 0,
    priceLevel7: p.priceLevel7 || 0,
    priceLevel8: p.priceLevel8 || 0,
    extNum1: p.extNum1 || 0,
    extNum2: p.extNum2 || 0,
    extNum3: p.extNum3 || 0,
    extNum4: p.extNum4 || 0,
    extNum5: p.extNum5 || 0,
    extNum6: p.extNum6 || 0,
    extNum7: p.extNum7 || 0,
    extNum8: p.extNum8 || 0,
    extNum9: p.extNum9 || 0,
    extNum10: p.extNum10 || 0,
    extText1: p.extText1 || '',
    extText2: p.extText2 || '',
    extText3: p.extText3 || '',
    extText4: p.extText4 || '',
    extText5: p.extText5 || '',
    extText6: p.extText6 || '',
    extPartner: p.extPartner || null,
    extStaff: p.extStaff || null,
    extDept: p.extDept || null,
  }
}

function buildPayload(status: number) {
  const inItems = (formData.inWarehouseItems || []).filter((p: any) => p.productId != null).map((p: any) => buildItemPayload(p, 1))
  const outItems = (formData.outWarehouseItems || []).filter((p: any) => p.productId != null).map((p: any) => buildItemPayload(p, 2))
  const total = combinedDiscountAmount.value
  const currentDebt = Math.max(0, total - (formData.paidAmount || 0) - (formData.usePrepaid || 0))
  return {
    status,
    settleStatus: formData.settleStatus || '0',
    settledAmount: 0,
    exchangeDate: formData.exchangeDate,
    supplierId: formData.supplierId,
    supplierName: formData.supplierName,
    supplierCode: formData.supplierCode,
    supplierRemark: formData.supplierRemark,
    contactName: formData.contactName,
    contactPhone: formData.contactPhone,
    contactAddress: formData.contactAddress,
    bankName: formData.bankName,
    bankAccount: formData.bankAccount,
    taxNo: formData.taxNo,
    inWarehouseId: formData.inWarehouseId,
    inWarehouseName: formData.inWarehouseName,
    outWarehouseId: formData.outWarehouseId,
    outWarehouseName: formData.outWarehouseName,
    handlerId: formData.handlerId,
    handlerName: formData.handlerName,
    deptId: formData.deptId,
    deptName: formData.deptName,
    paymentAccount: formData.paymentAccount,
    paidAmount: formData.paidAmount || 0,
    moreAccounts: formData.moreAccounts,
    prevPrepaid: formData.prevPrepaid || 0,
    usePrepaid: formData.usePrepaid || 0,
    prepaidBalance: (formData.prevPrepaid || 0) - (formData.usePrepaid || 0),
    prevDebt: formData.prevDebt || 0,
    currentDebt,
    debtBalance: (formData.prevDebt || 0) + currentDebt,
    paymentDeadline: formData.paymentDeadline,
    originalOrderId: formData.sourceOrderId,
    originalOrderNo: formData.sourceOrderNo,
    exchangeReason: formData.exchangeReason || formData.remark,
    exchangeType: formData.exchangeType || 1,
    remark: formData.remark,
    summary: formData.summary,
    inQuantityTotal: inTotalQuantity.value,
    outQuantityTotal: outTotalQuantity.value,
    productAmount: inTotalAmount.value + outTotalAmount.value,
    discountAmount: total,
    totalAmount: total,
    totalWeight: totalWeight.value,
    totalVolume: totalVolume.value,
    items: [...inItems, ...outItems],
  }
}

function validate(): boolean {
  if (!formData.supplierId) { message.warning('请选择供应商'); return false }
  if (!formData.inWarehouseId) { message.warning('请选择换入仓库'); return false }
  if (!formData.outWarehouseId) { message.warning('请选择换出仓库'); return false }
  if (!formData.handlerId) { message.warning('请选择经手人'); return false }
  if (!formData.exchangeDate) { message.warning('请选择单据日期'); return false }
  const hasItems = (formData.inWarehouseItems || []).some((p: any) => p.productId != null) ||
    (formData.outWarehouseItems || []).some((p: any) => p.productId != null)
  if (!hasItems) { message.warning('请至少添加一条换货商品明细'); return false }
  return true
}

async function doSubmit(status: number) {
  if (isLocked.value) { message.warning('当前单据不可修改'); return }
  if (!validate()) return
  saving.value = true
  try {
    const payload = buildPayload(status)
    const editId = route.params.id || route.query.id
    let createdId: number | undefined
    if (editId && effectiveMode.value === 'edit') {
      const res: any = await purchaseExchangeApi.update(Number(editId), payload)
      createdId = res?.data?.id ?? res?.id ?? Number(editId)
    } else {
      const res: any = await purchaseExchangeApi.create(payload)
      createdId = res?.data?.id ?? res?.id
    }
    if (status === 1 && createdId) {
      await purchaseExchangeApi.submit(createdId)
    }
    message.success(status === 0 ? '保存草稿成功' : '提交成功')
    router.push('/purchase/exchange')
  } catch (err: any) {
    message.error(err?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

function handleSaveDraft() { doSubmit(0) }
function handleSubmit() { doSubmit(1) }

// ═══════════════════════════════════════
// 换入仓库事件
// ═══════════════════════════════════════
function handleInWarehouseCellChange(record: any, fieldKey: string, value: any) {
  if (fieldKey === 'productId' && value != null) {
    const p = optionRefs.products.find((x: any) => x.id === value)
    if (p) fillProductInfo(record, p)
  } else if (['quantity', 'unitPrice', 'discount'].includes(fieldKey)) {
    record[fieldKey] = value
    calcRowAmount(record)
  }
}
function handleOpenInWarehouseProductModal(_record: any, rowIndex: number, fieldKey: string) {
  if (fieldKey === 'productId') {
    currentWarehouseType.value = 'in'
    currentSelectRowIndex.value = rowIndex
    showProductSelect.value = true
  }
}
function handleAddInWarehouseProduct() { formData.inWarehouseItems.push(createEmptyItem()) }
function handleInsertInWarehouseProduct(index: number) { formData.inWarehouseItems.splice(index + 1, 0, createEmptyItem()) }
function handleRemoveInWarehouseProduct(index: number) { formData.inWarehouseItems.splice(index, 1) }

// ═══════════════════════════════════════
// 换出仓库事件
// ═══════════════════════════════════════
function handleOutWarehouseCellChange(record: any, fieldKey: string, value: any) {
  if (fieldKey === 'productId' && value != null) {
    const p = optionRefs.products.find((x: any) => x.id === value)
    if (p) fillProductInfo(record, p)
  } else if (['quantity', 'unitPrice', 'discount'].includes(fieldKey)) {
    record[fieldKey] = value
    calcRowAmount(record)
  }
}
function handleOpenOutWarehouseProductModal(_record: any, rowIndex: number, fieldKey: string) {
  if (fieldKey === 'productId') {
    currentWarehouseType.value = 'out'
    currentSelectRowIndex.value = rowIndex
    showProductSelect.value = true
  }
}
function handleAddOutWarehouseProduct() { formData.outWarehouseItems.push(createEmptyItem()) }
function handleInsertOutWarehouseProduct(index: number) { formData.outWarehouseItems.splice(index + 1, 0, createEmptyItem()) }
function handleRemoveOutWarehouseProduct(index: number) { formData.outWarehouseItems.splice(index, 1) }

// ═══════════════════════════════════════
// 公共事件
// ═══════════════════════════════════════
function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
}
function handleSearchBtn(fieldKey: string, _btnText: string) {
  message.info(`${fieldKey} 快速查询功能待完善`)
}
function handleProductSelectConfirm(products: any[]) {
  const targetArray = currentWarehouseType.value === 'in' ? formData.inWarehouseItems : formData.outWarehouseItems
  const startIndex = currentSelectRowIndex.value >= 0 ? currentSelectRowIndex.value : targetArray.length
  while (targetArray.length < startIndex + products.length) targetArray.push(createEmptyItem())
  products.forEach((p: any, i: number) => {
    const rowIndex = startIndex + i
    if (rowIndex < targetArray.length) fillProductInfo(targetArray[rowIndex], p)
  })
  showProductSelect.value = false
  message.success(`已选择 ${products.length} 个商品`)
}

function handleAction(actionKey: string) {
  switch (actionKey) {
    case 'history':
      router.push('/purchase/exchange')
      break
    case 'print':
      message.info('请先保存换货单后再打印')
      break
    case 'config':
      showFormConfig.value = true
      break
    case 'import':
      message.info('导入功能请使用列表页的导入按钮')
      break
    case 'export':
      message.info('导出功能请使用列表页的导出按钮')
      break
  }
}

function handlePrintRecord() {
  if (formData.printCount) {
    message.info(`已打印 ${formData.printCount} 次`)
  }
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// ═══════════════════════════════════════
// 生命周期
// ═══════════════════════════════════════
onMounted(async () => {
  loadFormConfig()
  const editId = route.params.id || route.query.id
  // 新建：异步获取真实单号并填充默认空行
  if (!editId) {
    try {
      const res: any = await purchaseExchangeApi.nextNo()
      const no = res?.data ?? res
      if (no) formData.orderNo = no
    } catch {
      // 降级：后端编号接口不可用时保留前端生成的编号
    }
    if (formData.inWarehouseItems.length === 0) {
      for (let i = 0; i < 10; i++) formData.inWarehouseItems.push(createEmptyItem())
    }
    if (formData.outWarehouseItems.length === 0) {
      for (let i = 0; i < 10; i++) formData.outWarehouseItems.push(createEmptyItem())
    }
  }
  // 编辑模式：useBillForm 在挂载时自动加载详情，onDetailLoaded 已拆分配置
})
</script>

<style scoped>
.warehouse-section {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
  margin-bottom: 8px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  background: #fff;
}

.warehouse-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  font-size: 13px;
  font-weight: 500;
  color: #262626;
}

.warehouse-count {
  font-size: 12px;
  font-weight: 400;
  color: #8c8c8c;
}

.action-add-btn {
  color: #1890ff;
  padding: 0;
  font-size: 14px;
}

.action-del-btn {
  color: #ff4d4f;
  padding: 0;
  font-size: 14px;
}

.remark-section {
  padding: 4px 0;
}

.remark-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.remark-label {
  font-size: 12px;
  color: #595959;
  white-space: nowrap;
  min-width: 60px;
}

.remark-input {
  flex: 1;
}

.doc-info-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 6px 0;
  font-size: 12px;
  color: #8c8c8c;
  border-top: 1px solid #f0f0f0;
  flex-wrap: wrap;
}

.doc-info-item {
  white-space: nowrap;
}

.doc-info-link {
  color: #1890ff;
  font-size: 12px;
}
</style>
