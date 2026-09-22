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
      @tab-suffix-btn="handleTabSuffixBtn"
      @draft="handleSaveDraft"
      @submit="handleSubmit"
    >
      <!-- ═══ Zone 3: 商品明细表格 ═══ -->
      <template #detail-table="{ onExpandChange }">
        <BillDetailTable
          :columns="detailColumns"
          v-model:data-source="formData.products"
          :summary-columns="tableSummaryColumns"
          storage-key="sale-outbound-form-detail-columns"
          global-config-key="sale-outbound-form-detail-columns"
          @cell-change="handleCellChange"
          @expand-change="onExpandChange"
          @open-select-modal="handleOpenProductSelectModal"
        >
          <!-- 自定义：操作列 -->
          <template #actionCell="{ index, empty }">
            <template v-if="!empty">
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
            <template v-else>
              <a-space :size="2">
                <a-button
                  type="link"
                  size="small"
                  class="action-add-btn"
                  @click="handleAddProduct()"
                >
                  <PlusCircleOutlined />
                </a-button>
                <a-button
                  type="link"
                  size="small"
                  class="action-del-btn"
                  disabled
                >
                  <MinusCircleOutlined />
                </a-button>
              </a-space>
            </template>
          </template>
        </BillDetailTable>
      </template>

      <!-- ══ Zone 4 补充: 备注 + 单据信息 ═══ -->
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
          <div class="remark-row">
            <span class="remark-label">买家备注</span>
            <a-input
              v-model:value="formData.buyerRemark"
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
          <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
          <a-button
            type="link"
            size="small"
            class="doc-info-link"
          >
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

    <!-- ═══ 表单页面配置弹窗 ═══ -->
    <SaleOutboundFormConfig
      :open="showFormConfig"
      @update:open="showFormConfig = $event"
      @change="handleFormConfigChange"
    />

    <!-- ═══ 快速搜索弹窗（+Q按钮） ═══ -->
    <a-modal
      v-model:open="showQuickSearch"
      :title="quickSearchTitle"
      width="500px"
      :footer="null"
      destroy-on-close
    >
      <a-select
        v-model:value="quickSearchValue"
        show-search
        style="width:100%"
        :placeholder="`搜索${quickSearchTitle}`"
        :options="quickSearchOptions"
        :filter-option="filterOption"
        @change="handleQuickSearchConfirm"
      />
    </a-modal>

  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'

defineOptions({ name: 'SaleOutboundForm' })
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PrinterOutlined,
  ClockCircleOutlined,
  ImportOutlined,
  PlusCircleOutlined,
  MinusCircleOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import ProductSelectModal from '@/components/ProductSelectModal/index.vue'
import SaleOutboundFormConfig from '@/views/erp/column-config/SaleOutboundFormConfig.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, BillTabConfig, SummaryRow, BillFooterConfig, TabField } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { outboundApi } from '@/api/erp'
import { PRODUCT_PACK_DEFAULTS } from '@/utils/productDefaults'
import optionsApi from '@/api/options'
import request from '@/utils/request'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')

// ═══ 配置弹窗状态 ═══
const showProductSelect = ref(false)
const showFormConfig = ref(false)
const currentSelectRowIndex = ref(-1)

// ═══ 快速搜索（+Q按钮） ═══
const showQuickSearch = ref(false)
const quickSearchTitle = ref('')
const quickSearchFieldKey = ref('')
const quickSearchOptions = ref<{label:string;value:any}[]>([])
const quickSearchValue = ref<any>(undefined)

// ═══ 页面配置联动 ═══
/** 收款 Tab：「更多账户」展开收款账户2~4 */
const showMoreAccounts = ref(false)
const STORAGE_KEY_PAGE = 'sale-outbound-form-page-config'
const pageConfigFields = ref<any[]>([])

// 加载页面配置
function loadPageConfig() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY_PAGE)
    if (raw) {
      pageConfigFields.value = JSON.parse(raw)
    }
  } catch {
    pageConfigFields.value = []
  }
}

// 检查字段是否可见
function isFieldVisible(fieldKey: string): boolean {
  if (pageConfigFields.value.length === 0) return true
  const field = pageConfigFields.value.find((f: any) => f.key === fieldKey)
  return field ? field.visible !== false : true
}

/** 页面配置里的「显示名」覆盖字段标签（配置面板可改名，改名需真实生效） */
function fieldLabel(fieldKey: string, fallback: string): string {
  const field = pageConfigFields.value.find((f: any) => f.key === fieldKey)
  const name = field?.displayName
  return name && String(name).trim() ? String(name) : fallback
}

// 初始化加载
loadPageConfig()

// ═══════════════════════════════════════
// useBillForm composable
// ═══════════════════════════════════════

const returnQty = ref(0)
const promoDiscount = ref(0)
const discountAmount = ref(0)
const otherFee = ref(0)
const {
  formData,
  loadingOptions,
  saving,
  optionRefs,
  filterOption,
  handleAddProduct,
  handleRemoveProduct,
  handleFieldChange: baseFieldChange,
  handleSaveDraft,
  handleSubmit,
  totalQuantity,
  totalAmount,
} = useBillForm({
  billPrefix: 'XSCK',
  api: {
    create: outboundApi.create,
    update: outboundApi.update,
    getById: outboundApi.getById,
  },
  // 单号来自后端号段（SaleOutboundController#nextNo），前端不再使用演示生成器
  codeApiPath: '/erp/sale/outbound/next-no',
  redirectPath: '/sales/outbound',
  optionTypes: ['customers', 'warehouses', 'users', 'products'],
  productDefaults: PRODUCT_PACK_DEFAULTS,
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'customerId') {
      const c = optionRefs.customers.find((x: any) => x.id === val)
      if (c) {
        fd.customerName = c.name
        fd.customerCode = c.code || ''
        fd.receiverName = c.contactName || ''
        fd.receiverPhone = c.contactPhone || ''
        fd.shippingAddress = c.address || ''
      }
    }
    // 仓库/经手人名称快照：列表「仓库/经手人」列与查询口径均取快照字段
    if (fieldKey === 'warehouseId') {
      const w = optionRefs.warehouses.find((x: any) => x.id === val)
      fd.warehouseName = w ? (w.warehouseName || w.name || '') : ''
    }
    if (fieldKey === 'salesPersonId') {
      const u = optionRefs.users.find((x: any) => x.id === val)
      fd.salesPersonName = u ? (u.nickname || u.realName || u.name || u.username || '') : ''
    }
  },
  transformPayload: (fd, status) => ({
    // ═══ 基本信息 ═══
    status,
    // 单号来自后端号段（useBillForm.codeApiPath → /erp/sale/outbound/next-no）
    outboundNo: fd.outboundNo || fd.orderNo || '',
    // 来源订单：前端「源单」字段 sourceOrder，落库到 erp_sale_outbound.order_no
    orderNo: fd.sourceOrder || '',
    sourceOrder: fd.sourceOrder || '',
    outboundType: fd.outboundType || 0,
    outboundDate: fd.outboundDate,
    orderId: fd.orderId,
    generationMethod: fd.generationMethod || '',
    summary: fd.summary || '',
    totalAmount: totalAmount.value,
    totalQuantity: totalQuantity.value,
    printCount: fd.printCount || 0,
    // ═══ 客户快照 ═══
    customerId: fd.customerId,
    customerName: fd.customerName || '',
    customerCode: fd.customerCode || '',
    customerNumber: fd.customerNumber || fd.customerCode,
    customerLevel: fd.customerLevel || '',
    contactId: fd.contactId,
    contactName: fd.contactName || '',
    customerRemark: fd.customerRemark || '',
    bankName: fd.bankName || '',
    bankAccount: fd.bankAccount || '',
    taxNo: fd.taxNo || '',
    // ═══ 仓库/经手人 ═══
    warehouseId: fd.warehouseId,
    warehouseName: fd.warehouseName || '',
    salesPersonId: fd.salesPersonId,
    salesPersonName: fd.salesPersonName || '',
    departmentId: fd.departmentId,
    departmentName: fd.departmentName || '',
    location: fd.location || '',
    region: fd.region || '',
    // ═══ 收货 ═══
    receiverName: fd.receiverName || '',
    receiverPhone: fd.receiverPhone || '',
    shippingAddress: fd.shippingAddress || '',
    // ═══ 金额/费用 ═══
    promoDiscount: fd.promoDiscount || 0,
    couponAmount: fd.couponAmount || 0,
    directDiscount: fd.directDiscount || 0,
    otherFee: fd.otherFee || 0,
    roundingAmount: fd.roundingAmount || 0,
    // ═══ 结算 ═══
    settlementMethod: fd.settlementMethod || '',
    settledAmount: fd.settledAmount || 0,
    settlementStatus: fd.settlementStatus || '',
    // ═══ 收款账户 ═══
    paymentAccount1: fd.paymentAccount1 || '',
    paymentAccount2: fd.paymentAccount2 || '',
    paymentAccount3: fd.paymentAccount3 || '',
    paymentAccount4: fd.paymentAccount4 || '',
    // ═══ 物流 ═══
    deliveryMethod: fd.deliveryMethod || '',
    logisticsCompany: fd.logisticsCompany || '',
    logisticsBranch: fd.logisticsBranch || '',
    freightPayer: fd.freightPayer || '',
    freight: fd.freight || 0,
    // 运单号：表单只有一个「运单号」输入框，双写 trackingNumber/waybillNo 两个列保持口径一致
    trackingNumber: fd.waybillNo || fd.trackingNumber || '',
    waybillNo: fd.waybillNo || fd.trackingNumber || '',
    codAmount: fd.codAmount || 0,
    // ═══ 物流扩展 ═══
    deliveryOrderNo: fd.deliveryOrderNo || '',
    deliveryDriver: fd.deliveryDriver || '',
    expectedShipTime: fd.expectedShipTime || null,
    // ═══ 预收款 ═══
    advancePaymentAmount: fd.useDepositPayment ? (fd.usedAdvancePayment || 0) : 0,
    prevAdvancePayment: fd.prevAdvancePayment || 0,
    usedAdvancePayment: fd.usedAdvancePayment || 0,
    orderDeposit: fd.orderDeposit || 0,
    availableAdvancePayment: fd.availableAdvancePayment || 0,
    advancePaymentBalance: fd.advancePaymentBalance || 0,
    // ═══ 信用 ═══
    creditLimit: fd.creditLimit || 0,
    availableCredit: fd.availableCredit || 0,
    prevArrears: fd.prevArrears || 0,
    currentArrears: fd.currentArrears || 0,
    arrearsBalance: fd.arrearsBalance || 0,
    // ═══ 会员/积分 ═══
    memberCardNo: fd.memberCardNo || '',
    prevPoints: fd.prevPoints || 0,
    memberGeneratedPoints: fd.memberGeneratedPoints || 0,
    memberExchangePoints: fd.memberExchangePoints || 0,
    memberUsedPoints: fd.memberUsedPoints || 0,
    currentPoints: fd.currentPoints || 0,
    // ═══ 收款日/对账日 ═══
    paymentDate: fd.paymentDate || null,
    reconciliationDate: fd.reconciliationDate || null,
    // ═══ 审核信息 ═══
    approverId: fd.approverId,
    // ═══ 备注 ═══
    remark: fd.remark || '',
    internalNote: fd.internalNote || '',
    buyerRemark: fd.buyerRemark || '',
    // ═══ 制单人（列表「制单人」列与查询条件口径） ═══
    creatorName: fd.creatorName || currentUserName.value || '',
    // ═══ 表头自定义字段 ═══
    extNum1: fd.extNum1 || 0, extNum2: fd.extNum2 || 0, extNum3: fd.extNum3 || 0,
    extNum4: fd.extNum4 || 0, extNum5: fd.extNum5 || 0,
    extText1: fd.extText1 || '', extText2: fd.extText2 || '', extText3: fd.extText3 || '',
    extText4: fd.extText4 || '', extText5: fd.extText5 || '',
    extPartner: fd.extPartner, extStaff: fd.extStaff, extDept: fd.extDept,
    // ═══ 表尾自定义字段 ═══
    footerExtText1: fd.footerExtText1 || '',
    footerExtText2: fd.footerExtText2 || '',
    // ═══ 明细列表 ═══
    items: fd.products.filter((p: any) => p.productId != null).map((p: any) => ({
      productId: p.productId,
      productName: p.productName,
      productCode: p.productCode || '',
      imageUrl: p.imageUrl || '',
      barcode: p.barcode || '',
      smallUnitBarcode: p.smallUnitBarcode || '',
      specification: p.specification,
      model: p.model || '',
      origin: p.origin || '',
      brand: p.brand || '',
      unit: p.unit,
      productAttribute: p.productAttribute || '',
      location: p.location || '',
      region: p.region || '',
      orderNo: p.orderNo || '',
      pieceQuantity: p.pieceQuantity || 0,
      quantity: p.quantity,
      bigPack: p.bigPack || 0,
      midPack: p.midPack || 0,
      smallPack: p.smallPack || 0,
      conversionRelation: p.conversionRelation || '',
      conversionResult: p.conversionResult || 0,
      smallUnit: p.smallUnit || '',
      smallUnitPrice: p.smallUnitPrice || 0,
      smallUnitQuantity: p.smallUnitQuantity || 0,
      unitPrice: p.unitPrice,
      amount: (p.quantity || 0) * (p.unitPrice || 0),
      taxRate: p.taxRate || 0,
      discountRate: p.discountRate || 0,
      discountedAmount: p.discountedAmount || 0,
      discountedPrice: p.discountedPrice || 0,
      favorableDiscountRate: p.favorableDiscountRate || 0,
      favorableUnitPrice: p.favorableUnitPrice || 0,
      favorableAmount: p.favorableAmount || 0,
      costPrice: p.costPrice || 0,
      costAmount: p.costAmount || 0,
      grossProfit: p.grossProfit || 0,
      retailPrice: p.retailPrice || 0,
      wholesalePrice: p.wholesalePrice || 0,
      minSalePrice: p.minSalePrice || 0,
      lastSalePrice: p.lastSalePrice || 0,
      lastSaleDate: p.lastSaleDate || null,
      priceLevel1: p.priceLevel1 || 0, priceLevel2: p.priceLevel2 || 0,
      priceLevel3: p.priceLevel3 || 0, priceLevel4: p.priceLevel4 || 0,
      priceLevel5: p.priceLevel5 || 0, priceLevel6: p.priceLevel6 || 0,
      priceLevel7: p.priceLevel7 || 0, priceLevel8: p.priceLevel8 || 0,
      availableStock: p.availableStock || 0,
      bookStock: p.bookStock || 0,
      batchNo: p.batchNo || '',
      productionDate: p.productionDate || null,
      shelfLife: p.shelfLife || '',
      expiryDate: p.expiryDate || null,
      volume: p.volume || 0,
      weight: p.weight || 0,
      gift: p.gift || false,
      giftItem: p.giftItem || '',
      exchangePoints: p.exchangePoints || 0,
      usedPoints: p.usedPoints || 0,
      generatedPoints: p.generatedPoints || 0,
      boxNo: p.boxNo || '',
      remark: p.remark || '',
      originalPrice: p.originalPrice || 0,
      availableStockConverted: p.availableStockConverted || 0,
      // 表体自定义字段
      extNum1: p.extNum1 || 0, extNum2: p.extNum2 || 0, extNum3: p.extNum3 || 0,
      extNum4: p.extNum4 || 0, extNum5: p.extNum5 || 0, extNum6: p.extNum6 || 0, extNum7: p.extNum7 || 0,
      extText1: p.extText1 || '', extText2: p.extText2 || '',
      extPartner: p.extPartner, extStaff: p.extStaff, extDept: p.extDept,
    })),
  }),
})

// ═══ 下拉选项：仓库/用户接口未做字段归一化（返回 warehouseName / nickname），此处统一映射为 label ═══
const warehouseOptions = computed(() => optionRefs.warehouses.map((w: any) => ({
  label: w.warehouseName || w.name || w.warehouseCode || String(w.id),
  value: w.id,
})))
const userOptionsList = computed(() => optionRefs.users.map((u: any) => ({
  label: u.nickname || u.realName || u.name || u.username || String(u.id),
  value: u.id,
})))

// 初始化销售出库单特有字段
if (!('customerId' in formData)) {Object.assign(formData, {
  // ═══ 客户 ═══
  customerId: undefined, customerName: '', customerCode: '', customerNumber: '', customerLevel: '',
  contactId: undefined, contactName: '', customerRemark: '',
  // ═══ 银行/税务 ═══
  bankName: '', bankAccount: '', taxNo: '',
  // ═══ 仓库/经手人 ═══
  warehouseId: undefined, warehouseName: '',
  location: '', region: '',
  salesPersonId: undefined, salesPersonName: '',
  departmentId: undefined, departmentName: '',
  // ═══ 基本信息 ═══
  outboundDate: new Date().toISOString().slice(0, 10), outboundType: 0,
  summary: '', generationMethod: '',
  // ═══ 收货 ═══
  receiverName: '', receiverPhone: '', shippingAddress: '',
  // ═══ 金额/费用 ═══
  promoDiscount: 0, couponAmount: 0, directDiscount: 0, otherFee: 0, roundingAmount: 0,
  // ═══ 结算 ═══
  settlementMethod: '', settledAmount: 0, settlementStatus: '',
  // ═══ 收款账户 ═══
  paymentAccount1: '', paymentAccount2: '', paymentAccount3: '', paymentAccount4: '',
  // ═══ 物流 ═══
  deliveryMethod: '', logisticsCompany: '', logisticsBranch: '',
  freightPayer: '', freight: 0, trackingNumber: '', waybillNo: '', codAmount: 0,
  deliveryOrderNo: '', deliveryDriver: '', expectedShipTime: '',
  // ═══ 代收货款（是否代收） ═══
  codEnabled: 'no',
  // ═══ 预收款 ═══
  useDepositPayment: 0, prevAdvancePayment: 0, usedAdvancePayment: 0,
  orderDeposit: 0, availableAdvancePayment: 0, advancePaymentBalance: 0,
  // ═══ 信用 ═══
  creditLimit: 0, availableCredit: 0, prevArrears: 0, currentArrears: 0, arrearsBalance: 0,
  // ═══ 会员/积分 ═══
  memberCardNo: '',
  prevPoints: 0, memberGeneratedPoints: 0, memberExchangePoints: 0, memberUsedPoints: 0, currentPoints: 0,
  // ═══ 收款日/对账日 ═══
  paymentDate: '', reconciliationDate: '',
  // ═══ 审核 ═══
  approverId: undefined,
  // ═══ 备注 ═══
  remark: '', internalNote: '', buyerRemark: '',
  // ═══ 表头自定义字段 ═══
  extNum1: 0, extNum2: 0, extNum3: 0, extNum4: 0, extNum5: 0,
  extText1: '', extText2: '', extText3: '', extText4: '', extText5: '',
  extPartner: undefined, extStaff: undefined, extDept: undefined,
  // ═══ 表尾自定义字段 ═══
  footerExtText1: '', footerExtText2: '',
  // ═══ 源单 ═══
  sourceOrder: '',
  // ═══ 列表显示/系统 ═══
  printCount: 0,
})}

// ── 计算属性 ──
const totalWithTax = computed(() => formData.products.reduce((s: number, p: any) => s + (p.quantity || 0) * (p.unitPrice || 0) * (1 + (p.taxRate || 0) / 100), 0))

/** 表格合计列定义（传递给 BillDetailTable 的 summaryColumns） */
const tableSummaryColumns = computed(() => [
  { key: 'pieceQuantity', value: formData.products.reduce((s: number, p: any) => s + (p.pieceQuantity || 0), 0), highlight: true },
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'availableStock', value: formData.products.reduce((s: number, p: any) => s + (p.availableStock || 0), 0), highlight: true },
  { key: 'amount', value: totalAmount.value, highlight: true },
  { key: 'discountedAmount', value: formData.products.reduce((s: number, p: any) => s + (p.discountedAmount || 0), 0), highlight: true },
])

// ═══════════════════════════════════════
// BillFormPage 配置
// ═══════════════════════════════════════

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '销售出库单',
  // 编号字段受页面配置「编号」显隐控制
  orderNo: isFieldVisible('outboundNo') ? (formData.outboundNo || formData.orderNo) : '',
  showAttachment: true,
  actions: [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined, children: [
      { key: 'print-outbound', label: '打印出库单' },
      { key: 'print-summary', label: '打印汇总' },
    ]},
    { key: 'page-config', label: '', icon: SettingOutlined },
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'import', label: '导入', icon: ImportOutlined },
    { key: 'more', label: '更多', children: [
      { key: 'save-draft', label: '保存草稿' },
      { key: 'copy-outbound', label: '复制出库单' },
      { key: 'export', label: '导出' },
    ]},
  ],
}))

const basicInfoFields = computed<BasicInfoField[]>(() => {
  const allFields: BasicInfoField[] = [
    // ═══ Row 1: 核心6字段（按截图） ═══
    { key: 'customerId', label: fieldLabel('customerId', '客户'), type: 'select', required: true, inlineLabel: true, width: 320, options: optionRefs.customers.map((c: any) => ({ label: c.name, value: c.id })), searchBtn: '+Q', loading: loadingOptions.value },
    { key: 'warehouseId', label: fieldLabel('warehouseId', '发货仓库'), type: 'select', required: true, inlineLabel: true, width: 220, options: warehouseOptions.value, searchBtn: '+Q', loading: loadingOptions.value },
    { key: 'location', label: fieldLabel('location', '点位'), type: 'input', inlineLabel: true, width: 140 },
    { key: 'salesPersonId', label: fieldLabel('salesPersonId', '经手人'), type: 'select', required: true, inlineLabel: true, width: 220, options: userOptionsList.value, searchBtn: '+Q', loading: loadingOptions.value },
    { key: 'outboundDate', label: fieldLabel('outboundDate', '单据日期'), type: 'date', required: true, inlineLabel: true, width: 200 },
    { key: 'outboundType', label: fieldLabel('outboundType', '销售类型'), type: 'select', required: true, inlineLabel: true, width: 200, options: [{ label: '正常销售', value: 0 }, { label: '换货', value: 1 }, { label: '调拨', value: 2 }, { label: '其他', value: 3 }] },
    // ══ Row 2: 收货3字段（按截图） ═══
    { key: 'receiverName', label: fieldLabel('receiverName', '收货人'), type: 'input', inlineLabel: true, width: 200, searchBtn: 'Q' },
    { key: 'receiverPhone', label: fieldLabel('receiverPhone', '联系电话'), type: 'input', inlineLabel: true, width: 220 },
    { key: 'shippingAddress', label: fieldLabel('shippingAddress', '收货地址'), type: 'input', inlineLabel: true, width: 500 },
  ]
  // 根据页面配置过滤可见字段
  return allFields.filter(f => isFieldVisible(f.key))
})

const tabsConfig = computed<BillTabConfig[]>(() => {
  const allTabs: BillTabConfig[] = [
    // ═══ Tab 1: 收款（按截图） ═══
    { key: 'payment', tab: '收款', fields: [
      // Row 1: 收款核心
      { key: 'settlementMethod', label: '收款方式', type: 'select', width: 140, options: [
        { label: '现金', value: 'cash' }, { label: '转账', value: 'transfer' },
        { label: '微信', value: 'wechat' }, { label: '支付宝', value: 'alipay' },
        { label: '月结', value: 'monthly' }, { label: '货到付款', value: 'cod' },
      ]},
      { key: 'paymentAccount1', label: '收款账户1', type: 'input', placeholder: '请选择', width: 200, suffixBtn: '+Q' },
      { key: 'settledAmount', label: '收款金额1', type: 'number', placeholder: '0.00', width: 140, precision: 2 },
      { key: 'useMoreAccount', label: '更多账户', type: 'input', placeholder: showMoreAccounts.value ? '收起收款账户2~4' : '展开收款账户2~4', readonly: true, width: 100, suffixBtn: showMoreAccounts.value ? '收起' : '展开' },
      ...(showMoreAccounts.value ? ([
        { key: 'paymentAccount2', label: '收款账户2', type: 'input', placeholder: '请选择', width: 200, suffixBtn: '+Q' },
        { key: 'paymentAccount3', label: '收款账户3', type: 'input', placeholder: '请选择', width: 200, suffixBtn: '+Q' },
        { key: 'paymentAccount4', label: '收款账户4', type: 'input', placeholder: '请选择', width: 200, suffixBtn: '+Q' },
      ] as TabField[]) : []),
      { key: 'useDepositPayment', label: '使用预订货款', type: 'number', placeholder: '0.00', width: 140, precision: 2 },
      { key: 'prevAdvancePayment', label: '此前预收', type: 'number', disabled: true, width: 120, precision: 2 },
      // Row 2: 预收/欠款
      { key: 'usedAdvancePayment', label: '使用预收款', type: 'number', width: 120, precision: 2 },
      { key: 'orderDeposit', label: '订单已收订金', type: 'number', disabled: true, width: 130, precision: 2 },
      { key: 'availableAdvancePayment', label: '可用预收', type: 'number', disabled: true, width: 120, precision: 2 },
      { key: 'prevArrears', label: '此前欠款', type: 'number', disabled: true, width: 120, precision: 2 },
      { key: 'currentArrears', label: '本次欠款', type: 'number', width: 120, precision: 2 },
      { key: 'arrearsBalance', label: '欠款余额', type: 'number', disabled: true, width: 120, precision: 2 },
      // Row 3: 客户银行/源单
      { key: 'customerCode', label: '客户号', type: 'input', width: 120 },
      { key: 'bankName', label: '开户行', type: 'input', width: 150 },
      { key: 'bankAccount', label: '银行账号', type: 'input', width: 150 },
      { key: 'taxNo', label: '税号', type: 'input', width: 150 },
      { key: 'customerRemark', label: '客户备注', type: 'input', width: 200 },
      { key: 'sourceOrder', label: '源单', type: 'input', placeholder: '请输入源单编号', width: 200 },
    ]},
    // ═══ Tab 2: 物流信息（按截图，10 字段） ═══
    { key: 'logistics', tab: '物流信息', fields: [
      // Row 1
      { key: 'deliveryMethod', label: '配送方式', type: 'select', width: 140, options: [
        { label: '自提', value: 'self' }, { label: '快递', value: 'express' },
        { label: '物流', value: 'logistics' }, { label: '配送', value: 'delivery' },
      ]},
      { key: 'logisticsCompany', label: '物流公司', type: 'input', placeholder: '请输入物流公司', width: 200, suffixBtn: '+Q' },
      { key: 'logisticsBranch', label: '物流网点', type: 'input', placeholder: '请输入网点', width: 180 },
      { key: 'freightPayer', label: '运费承担方', type: 'select', width: 130, options: [
        { label: '买方承担', value: 'buyer' }, { label: '卖方承担', value: 'seller' }, { label: '第三方承担', value: 'third' },
      ]},
      { key: 'freight', label: '运费', type: 'number', placeholder: '0.00', width: 120, precision: 2 },
      { key: 'waybillNo', label: '运单号', type: 'input', placeholder: '请输入运单号', width: 160 },
      // Row 2
      { key: 'codEnabled', label: '物流公司代收货款', type: 'select', width: 160, options: [
        { label: '否', value: 'no' }, { label: '是', value: 'yes' },
      ]},
      { key: 'codAmount', label: '代收货款', type: 'number', placeholder: '0.00', width: 140, precision: 2 },
      { key: 'deliveryOrderNo', label: '配送单', type: 'input', placeholder: '请输入配送单号', width: 160 },
      { key: 'expectedShipTime', label: '预计发货', type: 'date', width: 140 },
    ]},
    // ═══ Tab 3: 会员信息（按截图） ═══
    { key: 'member', tab: '会员信息', fields: [
      { key: 'memberCardNo', label: '会员卡号', type: 'input', placeholder: '请输入会员卡号', width: 200 },
      { key: 'prevPoints', label: '此前积分', type: 'number', disabled: true, width: 120, precision: 0 },
      { key: 'memberGeneratedPoints', label: '产生积分', type: 'number', width: 120, precision: 0 },
      { key: 'memberExchangePoints', label: '兑换积分', type: 'number', width: 120, precision: 0 },
      { key: 'memberUsedPoints', label: '使用积分', type: 'number', width: 120, precision: 0 },
      { key: 'currentPoints', label: '当前积分', type: 'number', disabled: true, width: 120, precision: 0 },
    ]},
    // ═══ Tab 4: 扩展信息（从Header移下的字段 + 扩展） ═══
    { key: 'extended', tab: '扩展信息', fields: [
      // 组织/区域
      { key: 'region', label: '线路', type: 'input', width: 120 },
      { key: 'departmentId', label: '部门', type: 'input', width: 120 },
      { key: 'customerLevel', label: '客户级别', type: 'input', width: 120 },
      // 自定义字段1-5
      { key: 'extNum1', label: '自定义字段1(数字)', type: 'number', width: 140, precision: 2 },
      { key: 'extNum2', label: '自定义字段2(数字)', type: 'number', width: 140, precision: 2 },
      { key: 'extText1', label: '自定义字段3(文本)', type: 'input', width: 140 },
      { key: 'extText2', label: '自定义字段4(文本)', type: 'input', width: 140 },
      { key: 'extText3', label: '自定义字段5(文本)', type: 'input', width: 140 },
      // 审核/摘要
      { key: 'approverId', label: '审核人', type: 'select', width: 150, options: userOptionsList.value, searchBtn: '+Q', loading: loadingOptions.value },
      { key: 'summary', label: '摘要', type: 'textarea', width: 300 },
      // 表尾自定义
      { key: 'footerExtText1', label: '表尾自定义字段1', type: 'input', width: 200 },
      { key: 'footerExtText2', label: '表尾自定义字段2', type: 'input', width: 200 },
      // 其他扩展
      { key: 'generationMethod', label: '产生方式', type: 'input', width: 120 },
      { key: 'internalNote', label: '内部备注', type: 'textarea', width: 300 },
    ]},
  ]
  // 根据页面配置过滤每个tab的可见字段，并应用配置里的「显示名」
  return allTabs.map(tab => ({
    ...tab,
    fields: tab.fields
      .filter((f: any) => isFieldVisible(f.key))
      .map((f: any) => ({ ...f, label: fieldLabel(f.key, f.label) })),
  })).filter(tab => tab.fields.length > 0)
})

const summaryConfig = computed<SummaryRow[]>(() => {
  const allRows: (SummaryRow & { fieldKey?: string })[] = [
    { fieldKey: 'totalQuantity', label: '销售数量', value: totalQuantity.value },
    { fieldKey: 'returnQuantity', label: '退货数量', value: returnQty.value },
    { fieldKey: 'totalAmount', label: '商品金额', value: totalAmount.value.toFixed(2) },
    { fieldKey: 'promoDiscount', label: '促销优惠', value: promoDiscount.value.toFixed(2) },
    { fieldKey: 'directDiscount', label: '优惠金额', value: discountAmount.value.toFixed(2), showMore: true },
    { fieldKey: 'otherFee', label: '其他费用', value: otherFee.value.toFixed(2), showMore: true },
    { fieldKey: 'netAmount', label: '本单金额', value: totalWithTax.value.toFixed(2) },
  ]
  return allRows
    .filter(r => !r.fieldKey || isFieldVisible(r.fieldKey))
    .map(r => ({ ...r, label: r.fieldKey ? fieldLabel(r.fieldKey, r.label) : r.label }))
})

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: `¥${totalWithTax.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: '保存草稿',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '记账',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══════════════════════════════════════
// 明细表格列配置（BillDetailTable 驱动）
// ═══════════════════════════════════════

const detailColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 50, fixed: 'left' },
  { key: 'imageUrl', title: '图片', type: 'input', width: 60 },
  { key: 'productCode', title: '货号', type: 'input', width: 100 },
  {
    key: 'productId', title: '商品名称', type: 'input', searchable: true,
    options: optionRefs.products.map((p: any) => ({
      label: p.name, value: p.id,
      searchText: `${p.name} ${p.code || ''} ${p.barcode || ''}`,
    })),
    width: 200, showScanToggle: true,
  },
  { key: 'location', title: '货位', type: 'input', width: 80 },
  { key: 'orderNo', title: '订单编号', type: 'input', width: 130 },
  { key: 'barcode', title: '条码', type: 'input', width: 110 },
  { key: 'smallUnitBarcode', title: '小单位条码', type: 'input', width: 110 },
  { key: 'region', title: '区域', type: 'input', width: 80 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'model', title: '型号', type: 'input', width: 80 },
  { key: 'origin', title: '产地', type: 'input', width: 80 },
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'brand', title: '品牌', type: 'input', width: 80 },
  { key: 'productAttribute', title: '商品行属性', type: 'input', width: 100 },
  { key: 'extNum1', title: '单据自定义1(数字字段)', type: 'number', width: 110, precision: 2 },
  { key: 'extNum2', title: '单据自定义2(数字字段)', type: 'number', width: 110, precision: 2 },
  { key: 'extNum3', title: '单据自定义3(数字字段)', type: 'number', width: 110, precision: 2 },
  { key: 'extText1', title: '单据自定义4(文本字段)', type: 'input', width: 110 },
  { key: 'extText2', title: '单据自定义5(文本字段)', type: 'input', width: 110 },
  { key: 'extNum4', title: '单据自定义6(数字字段)', type: 'number', width: 110, precision: 2 },
  { key: 'extNum5', title: '单据自定义7(数字字段)', type: 'number', width: 110, precision: 2 },
  { key: 'pieceQuantity', title: '件散数量', type: 'number', width: 80, precision: 2 },
  { key: 'quantity', title: '数量', type: 'number', width: 80, precision: 2 },
  { key: 'bigPack', title: '大包装', type: 'number', width: 70, precision: 2 },
  { key: 'midPack', title: '中包装', type: 'number', width: 70, precision: 2 },
  { key: 'smallPack', title: '小包装', type: 'number', width: 70, precision: 2 },
  { key: 'extPartner', title: '单据自定义8(往来单位)', type: 'input', width: 120 },
  { key: 'extStaff', title: '单据自定义9(职员)', type: 'input', width: 100 },
  { key: 'extDept', title: '单据自定义10(部门)', type: 'input', width: 100 },
  { key: 'lastSaleDate', title: '最近销售日期', type: 'date', width: 110 },
  { key: 'lastSalePrice', title: '最近售价', type: 'number', width: 80, precision: 2 },
  { key: 'retailPrice', title: '零售价', type: 'number', width: 80, precision: 2 },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 80, precision: 2 },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 70 },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 90, precision: 2 },
  { key: 'smallUnitQuantity', title: '小单位数量', type: 'number', width: 90, precision: 2 },
  { key: 'minSalePrice', title: '最低售价', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel1', title: '餐饮店', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel2', title: '食堂团餐', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel3', title: '自助vip', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel4', title: '大团餐', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel5', title: '重点|vip01', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel6', title: '连锁|vip', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel7', title: '特价客户', type: 'number', width: 80, precision: 2 },
  { key: 'availableStock', title: '可用库存', type: 'number', width: 90, precision: 2 },
  { key: 'availableStockConverted', title: '可用库存换算结果', type: 'number', width: 110, precision: 2 },
  { key: 'bookStock', title: '账面库存', type: 'number', width: 90, precision: 2 },
  { key: 'batchNo', title: '批次条码', type: 'input', width: 110 },
  { key: 'productionDate', title: '生产日期', type: 'date', width: 110 },
  { key: 'shelfLife', title: '保质期', type: 'input', width: 80 },
  { key: 'expiryDate', title: '到期日期', type: 'date', width: 110 },
  { key: 'conversionRelation', title: '换算关系', type: 'input', width: 100 },
  { key: 'unitPrice', title: '单价', type: 'number', width: 80, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 80, precision: 2 },
  { key: 'discountRate', title: '折扣(%)', type: 'number', width: 70, precision: 1 },
  { key: 'discountedAmount', title: '折后金额', type: 'number', width: 80, precision: 2 },
  { key: 'discountedPrice', title: '折后单价', type: 'number', width: 80, precision: 2 },
  { key: 'costPrice', title: '参考成本单价', type: 'number', width: 100, precision: 2 },
  { key: 'costAmount', title: '参考成本金额', type: 'number', width: 100, precision: 2 },
  { key: 'grossProfit', title: '参考毛利', type: 'number', width: 80, precision: 2 },
  { key: 'originalPrice', title: '折单原价', type: 'number', width: 80, precision: 2 },
  { key: 'favorableDiscountRate', title: '优惠折扣(%)', type: 'number', width: 90, precision: 1 },
  { key: 'priceLevel8', title: '外围餐饮店', type: 'number', width: 80, precision: 2 },
  { key: 'favorableUnitPrice', title: '惠后单价', type: 'number', width: 80, precision: 2 },
  { key: 'favorableAmount', title: '优惠后金额', type: 'number', width: 100, precision: 2 },
  { key: 'giftItem', title: '兑换礼品', type: 'input', width: 100 },
  { key: 'exchangePoints', title: '兑换积分', type: 'number', width: 80, precision: 0 },
  { key: 'generatedPoints', title: '产生积分', type: 'number', width: 80, precision: 0 },
  { key: 'usedPoints', title: '使用积分', type: 'number', width: 80, precision: 0 },
  { key: 'volume', title: '体积（m³）', type: 'number', width: 80, precision: 4 },
  { key: 'weight', title: '重量（kg）', type: 'number', width: 80, precision: 2 },
  { key: 'gift', title: '赠品', type: 'input', width: 60 },
  { key: 'remark', title: '备注', type: 'input', width: 120 },
])

// ═══ 数据表格列配置：走 BillDetailTable 表头齿轮（storage-key/global-config-key 见模板） ═══

// ═══════════════════════════════════════
// 事件处理
// ═══════════════════════════════════════

/** 在当前行后插入一个空行 */
function handleInsertProduct(index: number) {
  const newProduct = {
    id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
    productId: undefined,
    productCode: '', productName: '', imageUrl: '',
    specification: '', location: '', region: '', model: '', origin: '', brand: '',
    unit: '', productAttribute: '', barcode: '', smallUnitBarcode: '',
    orderNo: '',
    pieceQuantity: 0, quantity: 0, bigPack: 0, midPack: 0, smallPack: 0,
    productionDate: '', shelfLife: '', expiryDate: '', batchNo: '',
    conversionRelation: '', conversionResult: 0,
    smallUnit: '', smallUnitPrice: 0, smallUnitQuantity: 0,
    discountRate: 0, discountedAmount: 0, discountedPrice: 0,
    favorableDiscountRate: 0, favorableUnitPrice: 0, favorableAmount: 0,
    unitPrice: 0, amount: 0, taxRate: 13, scanMode: false,
    retailPrice: 0, wholesalePrice: 0, minSalePrice: 0,
    lastSalePrice: 0, lastSaleDate: '',
    costPrice: 0, costAmount: 0, grossProfit: 0,
    availableStock: 0, bookStock: 0,
    volume: 0, weight: 0,
    gift: false, giftItem: '',
    exchangePoints: 0, usedPoints: 0, generatedPoints: 0,
    extNum1: 0, extNum2: 0, extNum3: 0, extNum4: 0, extNum5: 0, extNum6: 0, extNum7: 0,
    extText1: '', extText2: '',
    extPartner: undefined, extStaff: undefined, extDept: undefined,
    priceLevel1: 0, priceLevel2: 0, priceLevel3: 0, priceLevel4: 0,
    priceLevel5: 0, priceLevel6: 0, priceLevel7: 0, priceLevel8: 0,
    boxNo: '', remark: '',
    originalPrice: 0, availableStockConverted: 0,
  }
  formData.products.splice(index + 1, 0, newProduct)
}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
}

function handleCellChange(record: any, fieldKey: string, value: any) {
  if (fieldKey === 'productId' && value != null) {
    const p = optionRefs.products.find((x: any) => x.id === value)
    if (p) {
      record.productName = p.name || ''
      record.productCode = p.code || ''
      record.specification = p.specification || ''
      record.unit = p.unit || ''
      record.barcode = p.barcode || ''
      record.brand = p.brand || ''
      record.origin = p.origin || ''
      record.model = p.model || ''
      record.retailPrice = p.retailPrice || p.salePrice || 0
      record.wholesalePrice = p.wholesalePrice || 0
      record.minSalePrice = p.minSalePrice || 0
      record.unitPrice = p.salePrice || p.price || 0
      record.costPrice = p.costPrice || 0
      record.availableStock = p.stock || 0
      record.bookStock = p.bookStock || 0
      record.weight = p.weight || 0
      record.volume = p.volume || 0
      record.taxRate = 13
      // 价格等级快照
      record.priceLevel1 = p.priceLevel1 || 0
      record.priceLevel2 = p.priceLevel2 || 0
      record.priceLevel3 = p.priceLevel3 || 0
      record.priceLevel4 = p.priceLevel4 || 0
      record.priceLevel5 = p.priceLevel5 || 0
      record.priceLevel6 = p.priceLevel6 || 0
      record.priceLevel7 = p.priceLevel7 || 0
      record.priceLevel8 = p.priceLevel8 || 0
      // 触发金额计算
      recalcLineAmount(record)
    }
  }
  // 数量/单价变更 → 重算金额
  if (fieldKey === 'quantity' || fieldKey === 'unitPrice') {
    recalcLineAmount(record)
  }
  // 折扣率变更 → 重算折后金额
  if (fieldKey === 'discountRate') {
    recalcDiscount(record)
  }
  // 优惠折扣变更 → 重算优惠后金额
  if (fieldKey === 'favorableDiscountRate') {
    recalcFavorable(record)
  }
  // 成本单价变更 → 重算成本金额和毛利
  if (fieldKey === 'costPrice') {
    recalcCost(record)
  }
}

/** 重算行金额 = 数量 × 单价 */
function recalcLineAmount(record: any) {
  const qty = Number(record.quantity) || 0
  const price = Number(record.unitPrice) || 0
  record.amount = +(qty * price).toFixed(2)
  record.originalPrice = record.originalPrice || price
  // 如果已有折扣，重算折后
  if (record.discountRate) recalcDiscount(record)
  // 如果有成本，重算毛利
  if (record.costPrice) recalcCost(record)
}

/** 重算折后金额 = 金额 × (1 - 折扣率/100) */
function recalcDiscount(record: any) {
  const amount = Number(record.amount) || 0
  const rate = Number(record.discountRate) || 0
  record.discountedPrice = +(record.unitPrice * (1 - rate / 100)).toFixed(4)
  record.discountedAmount = +(amount * (1 - rate / 100)).toFixed(2)
  // 折后金额变了，优惠也要重算
  if (record.favorableDiscountRate) recalcFavorable(record)
  // 重算毛利
  if (record.costPrice) recalcCost(record)
}

/** 重算优惠后金额 = 折后金额 × (1 - 优惠折扣率/100) */
function recalcFavorable(record: any) {
  const discAmount = Number(record.discountedAmount) || Number(record.amount) || 0
  const rate = Number(record.favorableDiscountRate) || 0
  record.favorableUnitPrice = +((record.discountedPrice || record.unitPrice) * (1 - rate / 100)).toFixed(4)
  record.favorableAmount = +(discAmount * (1 - rate / 100)).toFixed(2)
}

/** 重算成本金额和毛利 */
function recalcCost(record: any) {
  const qty = Number(record.quantity) || 0
  const cost = Number(record.costPrice) || 0
  record.costAmount = +(qty * cost).toFixed(2)
  const revenue = Number(record.discountedAmount) || Number(record.amount) || 0
  record.grossProfit = +(revenue - record.costAmount).toFixed(2)
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  if (fieldKey === 'customerId' && optionRefs.customers?.length) {
    quickSearchTitle.value = '客户'
    quickSearchFieldKey.value = 'customerId'
    quickSearchOptions.value = optionRefs.customers.map((c: any) => ({ label: `${c.name}${c.code ? `[${c.code}]` : ''}`, value: c.id }))
    quickSearchValue.value = formData.customerId
    showQuickSearch.value = true
  } else if (fieldKey === 'warehouseId' && optionRefs.warehouses?.length) {
    quickSearchTitle.value = '仓库'
    quickSearchFieldKey.value = 'warehouseId'
    quickSearchOptions.value = warehouseOptions.value
    quickSearchValue.value = formData.warehouseId
    showQuickSearch.value = true
  } else if (fieldKey === 'salesPersonId' && optionRefs.users?.length) {
    quickSearchTitle.value = '经手人'
    quickSearchFieldKey.value = 'salesPersonId'
    quickSearchOptions.value = userOptionsList.value.map((o: any, i: number) => ({
      label: `${o.label}${optionRefs.users[i]?.deptName ? `(${optionRefs.users[i].deptName})` : ''}`,
      value: o.value,
    }))
    quickSearchValue.value = formData.salesPersonId
    showQuickSearch.value = true
  } else if (fieldKey === 'receiverName') {
    message.info('可直接输入收货人名称')
  }
}

/** Tab 内后缀按钮（+Q / 展开）：账户、物流公司取自真实主数据；「更多账户」展开收款账户2~4 */
async function handleTabSuffixBtn(fieldKey: string, _btnText: string) {
  if (fieldKey === 'useMoreAccount') {
    showMoreAccounts.value = !showMoreAccounts.value
    return
  }
  if (fieldKey === 'paymentAccount1' || fieldKey === 'paymentAccount2'
    || fieldKey === 'paymentAccount3' || fieldKey === 'paymentAccount4') {
    const accounts = await optionsApi.getAccounts().catch(() => [])
    if (!accounts.length) {
      message.warning('暂无可用收款账户，请先在财务账户中维护')
      return
    }
    quickSearchTitle.value = '收款账户'
    quickSearchFieldKey.value = fieldKey
    quickSearchOptions.value = accounts.map((a: any) => ({
      label: `${a.name}${a.code ? `[${a.code}]` : ''}`,
      value: a.name,
    }))
    quickSearchValue.value = formData[fieldKey]
    showQuickSearch.value = true
    return
  }
  if (fieldKey === 'logisticsCompany') {
    // ⚠️ 原为 `request.get('/md/logistics/list')`：后端**不存在** `/api/md/logistics` 控制器
    // （物流公司是 biz_party 的一种 partnerType，无独立档案控制器），该请求必然 404，
    // 又被 .catch(() => []) 吞掉，表现为固定提示「暂无物流公司主数据」——快速查询实际不可用。
    // 正确来源与 `md/logistics` 页面、`order-center` 一致：/erp/md/customer/list + LOGISTICS。
    const list = await request.get('/erp/md/customer/list', { partnerType: 'LOGISTICS', status: 'ENABLED', pageSize: 500 })
      .then((res: any) => res?.data || res || []).catch(() => [])
    if (!list.length) {
      message.warning('暂无物流公司主数据，请先在资料中维护')
      return
    }
    quickSearchTitle.value = '物流公司'
    quickSearchFieldKey.value = fieldKey
    quickSearchOptions.value = list.map((l: any) => ({
      label: l.name || l.companyName || l.logisticsName || String(l.id),
      value: l.name || l.companyName || l.logisticsName || String(l.id),
    }))
    quickSearchValue.value = formData.logisticsCompany
    showQuickSearch.value = true
  }
}

function handleQuickSearchConfirm(val: any) {
  if (val == null) return
  const fieldKey = quickSearchFieldKey.value
  if (fieldKey === 'customerId') {
    formData.customerId = val
    handleFieldChange('customerId', val)
  } else if (fieldKey === 'warehouseId') {
    formData.warehouseId = val
    handleFieldChange('warehouseId', val)
  } else if (fieldKey === 'salesPersonId') {
    formData.salesPersonId = val
    handleFieldChange('salesPersonId', val)
  } else if (fieldKey === 'paymentAccount1' || fieldKey === 'paymentAccount2'
    || fieldKey === 'paymentAccount3' || fieldKey === 'paymentAccount4') {
    // 选项 value 即账户名称（列表页展示口径）
    formData[fieldKey] = val
  } else if (fieldKey === 'logisticsCompany') {
    formData.logisticsCompany = val
  }
  showQuickSearch.value = false
}

// ═══════════════════════════════════════
// 产品选择弹窗处理
// ═══════════════════════════════════════

function handleOpenProductSelectModal(record: any, rowIndex: number, fieldKey: string) {
  if (fieldKey === 'productId') {
    currentSelectRowIndex.value = rowIndex
    showProductSelect.value = true
  }
}

function handleProductSelectConfirm(products: any[]) {
  const startIndex = currentSelectRowIndex.value >= 0 ? currentSelectRowIndex.value : formData.products.length

  for (let i = startIndex; i < startIndex + products.length; i++) {
    if (i >= formData.products.length) {
      handleAddProduct()
    }
  }

  products.forEach((p: any, i: number) => {
    const rowIndex = startIndex + i
    if (rowIndex < formData.products.length) {
      const row = formData.products[rowIndex]
      row.productId = p.id
      row.productName = p.name || ''
      row.productCode = p.code || ''
      row.specification = p.specification || ''
      row.unit = p.unit || ''
      row.barcode = p.barcode || ''
      row.brand = p.brand || ''
      row.origin = p.origin || ''
      row.model = p.model || ''
      row.retailPrice = p.retailPrice || p.salePrice || 0
      row.wholesalePrice = p.wholesalePrice || 0
      row.minSalePrice = p.minSalePrice || 0
      row.unitPrice = p.retailPrice || p.salePrice || p.price || 0
      row.costPrice = p.costPrice || 0
      row.availableStock = p.stock || 0
      row.bookStock = p.bookStock || 0
      row.weight = p.weight || 0
      row.volume = p.volume || 0
      row.taxRate = 13
      // 价格等级
      row.priceLevel1 = p.priceLevel1 || 0
      row.priceLevel2 = p.priceLevel2 || 0
      row.priceLevel3 = p.priceLevel3 || 0
      row.priceLevel4 = p.priceLevel4 || 0
      row.priceLevel5 = p.priceLevel5 || 0
      row.priceLevel6 = p.priceLevel6 || 0
      row.priceLevel7 = p.priceLevel7 || 0
      row.priceLevel8 = p.priceLevel8 || 0
      recalcLineAmount(row)
    }
  })

  showProductSelect.value = false
  message.success(`已选择 ${products.length} 个商品`)
}

function handleAction(actionKey: string, _parentKey?: string) {
  switch (actionKey) {
    case 'history':
      router.push('/sales/outbound')
      break
    case 'page-config':
      showFormConfig.value = true
      break
    case 'save-draft':
      handleSaveDraft()
      break
    case 'copy-outbound':
      handleCopyOutbound()
      break
    case 'print-outbound':
    case 'print-summary':
      handlePrint()
      break
    case 'import':
      handleImport()
      break
    case 'export':
      handleExportForm()
      break
  }
}

async function handleCopyOutbound() {
  if (!formData.id) {
    message.warning('请先保存出库单')
    return
  }
  try {
    const res = await outboundApi.copy(formData.id)
    message.success('复制成功')
    router.push(`/sales/outbound/form/${res.id}`)
  } catch (error: any) {
    message.error(error?.response?.data?.message || '复制失败')
  }
}

async function handlePrint() {
  if (!formData.id) {
    message.warning('请先保存出库单')
    return
  }
  try {
    await outboundApi.print(formData.id)
    formData.printCount = (formData.printCount || 0) + 1
    message.success('打印成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '打印失败')
  }
}

function handleImport() {
  const input = document.createElement('input')
  input.type = 'file'
  input.accept = '.xlsx,.xls,.csv'
  input.onchange = async (e: any) => {
    const file = e.target.files[0]
    if (!file) return
    try {
      const res = await outboundApi.batchImport(file)
      if (res?.success) {
        message.success(res.message || '导入成功')
      } else {
        message.error(res?.message || '导入失败')
      }
    } catch (error: any) {
      message.error(error?.response?.data?.message || '导入失败')
    }
  }
  input.click()
}

async function handleExportForm() {
  if (!formData.id) return
  try {
    const data = await outboundApi.getById(formData.id)
    if (!data) return
    const headers = ['单据编号', '客户', '金额', '状态']
    const row = [data.outboundNo, data.customerName, data.totalAmount, data.statusDesc]
    const csv = [headers.join(','), row.join(',')]
    const blob = new Blob(['\uFEFF' + csv.join('\n')], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `出库单_${data.outboundNo}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error('导出失败')
  }
}

function handleFormConfigChange(config: any) {
  // 重新加载页面配置，触发 computed 重新计算
  loadPageConfig()
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// ── 生命周期 ──
onMounted(() => {
  if (formData.products.length === 0) {
    for (let i = 0; i < 20; i++) handleAddProduct()
  }
})
</script>

<style scoped>
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
}

.doc-info-link {
  color: #1890ff;
  font-size: 12px;
}
</style>
