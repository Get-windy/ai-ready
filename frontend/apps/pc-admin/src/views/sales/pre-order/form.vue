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
      <!-- ═══ Zone 3: 商品明细表格 ═══ -->
      <template #detail-table="{ onExpandChange }">
        <BillDetailTable
          :columns="visibleDetailColumns"
          v-model:data-source="formData.products"
          :summary-columns="tableSummaryColumns"
          @cell-change="handleCellChange"
          @expand-change="onExpandChange"
          @open-select-modal="handleOpenProductSelectModal"
        >
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

      <!-- ═══ Zone 4 补充: 备注 + 单据信息 ═══ -->
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
            <span class="remark-label">摘要</span>
            <a-input
              v-model:value="formData.summary"
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
    <PreOrderFormConfig
      :open="showFormConfig"
      @update:open="showFormConfig = $event"
      @change="handleFormConfigChange"
    />

    <!-- ═══ 数据表格列配置弹窗 ═══ -->
    <ColumnConfigPanel
      :open="showFormColumnConfig"
      :settings-columns="formSettingsColumns"
      :is-locked-column="isLockedColumn"
      @update:open="showFormColumnConfig = $event"
      @change="handleFormColumnConfigChange"
      @reset="handleFormColumnConfigReset"
      @drag-end="handleFormColumnConfigChange"
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
import { computed, ref, onMounted, onUnmounted, nextTick } from 'vue'
import { h } from 'vue'

defineOptions({ name: 'PreOrderForm' })
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { PrinterOutlined, ClockCircleOutlined, ImportOutlined, PlusCircleOutlined, MinusCircleOutlined, SettingOutlined } from '@ant-design/icons-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import ProductSelectModal from '@/components/ProductSelectModal/index.vue'
import PreOrderFormConfig from '@/views/erp/column-config/PreOrderFormConfig.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, BillTabConfig, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { useColumnConfig, isLockedColumn } from '@/composables/useColumnConfig'
import { preOrderApi } from '@/api/erp'
import { PRODUCT_EXTEND_DEFAULTS } from '@/utils/productDefaults'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')

// ═══ 配置弹窗状态 ═══
const showProductSelect = ref(false)
const showFormConfig = ref(false)
const showFormColumnConfig = ref(false)
const currentSelectRowIndex = ref(-1)
// ═══ 快速搜索（+Q按钮） ═══
const showQuickSearch = ref(false)
const quickSearchTitle = ref('')
const quickSearchFieldKey = ref('')
const quickSearchOptions = ref<{label:string;value:any}[]>([])
const quickSearchValue = ref<any>(undefined)

// ═══ 页面配置联动 ═══
const STORAGE_KEY_PAGE = 'sale-pre-order-form-page-config'
const pageConfigFields = ref<any[]>([])

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

function isFieldVisible(fieldKey: string): boolean {
  if (pageConfigFields.value.length === 0) return true
  const field = pageConfigFields.value.find((f: any) => f.key === fieldKey)
  return field ? field.visible !== false : true
}

// 初始化加载
loadPageConfig()

// ═══════════════════════════════════════
// useBillForm composable
// ═══════════════════════════════════════

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
  billPrefix: 'YDHD',
  api: { create: preOrderApi.create, update: preOrderApi.update, getById: preOrderApi.getById },
  redirectPath: '/sales/pre-order',
  optionTypes: ['customers', 'warehouses', 'users', 'products'],
  productDefaults: PRODUCT_EXTEND_DEFAULTS,
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'customerId') {
      const c = optionRefs.customers.find((x: any) => x.id === val)
      if (c) {
        fd.customerName = c.name
        fd.customerCode = c.code || ''
        fd.bankName = c.bankName || ''
        fd.bankAccount = c.bankAccount || ''
        fd.taxNo = c.taxNo || ''
        fd.customerLevel = c.level || ''
        fd.receiverName = c.contactName || ''
        fd.receiverPhone = c.contactPhone || ''
        fd.shippingAddress = c.address || ''
        // 生产级ERP: 自动填充客户信用额度和收款期限
        fd.creditLimit = c.creditLimit || 0
        if (c.depositDeadline) fd.depositDeadline = c.depositDeadline
        // 生产级ERP: 根据客户等级自动选择对应价格等级
        if (c.level && fd.products?.length) {
          const priceLevelMap: Record<string, string> = {
            '餐饮店': 'priceLevel1', '食堂团餐': 'priceLevel2', '外围餐饮店': 'priceLevel3',
            '自助vip': 'priceLevel4', '大团餐': 'priceLevel5', '重点|vip01': 'priceLevel6',
            '连锁|vip': 'priceLevel7', '特价客户': 'priceLevel8',
          }
          const priceField = priceLevelMap[c.level]
          if (priceField) {
            fd.products.forEach((p: any) => {
              if (p[priceField] != null && p[priceField] > 0) {
                p.unitPrice = p[priceField]
                recalcLineAmount(p)
              }
            })
          }
        }
      }
    }
    if (fieldKey === 'warehouseId') {
      const w = optionRefs.warehouses.find((x: any) => x.id === val)
      if (w) fd.warehouseName = w.name
    }
    if (fieldKey === 'handlerId') {
      const u = optionRefs.users.find((x: any) => x.id === val)
      if (u) fd.handlerName = u.name
    }
  },
  transformPayload: (fd, status) => ({
    // ═══ 基本信息 ═══
    status,
    orderDate: fd.orderDate,
    saleType: fd.saleType || 0,
    summary: fd.summary || '',
    totalAmount: totalAmount.value,
    discountedAmount: fd.products.reduce((s: number, p: any) => s + (p.discountedAmount || 0), 0),
    orderAmount: totalWithTax.value,
    totalQuantity: totalQuantity.value,
    // ═══ 客户快照 ═══
    customerId: fd.customerId,
    customerName: fd.customerName || '',
    customerCode: fd.customerCode || '',
    customerLevel: fd.customerLevel || '',
    customerRemark: fd.customerRemark || '',
    customerTicket: fd.customerTicket || '',
    // ═══ 银行/税务 ═══
    bankName: fd.bankName || '',
    bankAccount: fd.bankAccount || '',
    taxNo: fd.taxNo || '',
    // ═══ 仓库/经手人 ═══
    warehouseId: fd.warehouseId,
    warehouseName: fd.warehouseName || '',
    handlerId: fd.handlerId,
    handlerName: fd.handlerName || '',
    deptId: fd.deptId,
    deptName: fd.deptName || '',
    // ═══ 收货 ═══
    receiverName: fd.receiverName || '',
    receiverPhone: fd.receiverPhone || '',
    shippingAddress: fd.shippingAddress || '',
    // ═══ 预订金/信用 ═══
    depositAccount1: fd.depositAccount1 || '',
    depositAccount2: fd.depositAccount2 || '',
    depositAccount3: fd.depositAccount3 || '',
    depositAccount4: fd.depositAccount4 || '',
    depositAmount: fd.depositAmount || 0,
    creditLimit: fd.creditLimit || 0,
    depositDeadline: fd.depositDeadline || '',
    // ═══ 备注 ═══
    remark: fd.remark || '',
    // ═══ 表头自定义字段 ═══
    extNum1: fd.extNum1 || 0, extNum2: fd.extNum2 || 0,
    extText1: fd.extText1 || '', extText2: fd.extText2 || '', extText3: fd.extText3 || '',
    // ═══ 表尾自定义字段 ═══
    footerExtText1: fd.footerExtText1 || '',
    footerExtText2: fd.footerExtText2 || '',
    // ═══ 明细列表 ═══
    items: fd.products.filter((p: any) => p.productId != null).map((p: any) => ({
      productId: p.productId,
      productName: p.productName,
      productCode: p.itemCode || p.productCode || '',
      imageUrl: p.imageUrl || '',
      barcode: p.barcode || '',
      specification: p.specification || '',
      model: p.model || '',
      origin: p.origin || '',
      brand: p.brand || '',
      unit: p.unit,
      pricingUnit: p.pricingUnit || p.unit,
      smallUnit: p.smallUnit || '',
      smallUnitPrice: p.smallUnitPrice || 0,
      smallUnitQuantity: p.smallUnitQuantity || 0,
      conversionRelation: p.conversionRelation || '',
      quantity: p.quantity,
      pieceQuantity: p.pieceQuantity || 0,
      bigPack: p.bigPack || 0,
      midPack: p.midPack || 0,
      smallPack: p.smallPack || 0,
      unitPrice: p.unitPrice,
      amount: (p.quantity || 0) * (p.unitPrice || 0),
      discountRate: p.discountRate || 0,
      discountedPrice: p.discountedPrice || 0,
      discountedAmount: p.discountedAmount || 0,
      costPrice: p.costPrice || 0,
      costAmount: p.costAmount || 0,
      grossProfit: p.grossProfit || 0,
      retailPrice: p.retailPrice || 0,
      wholesalePrice: p.wholesalePrice || 0,
      minSalePrice: p.minSalePrice || 0,
      lastSaleDate: p.lastSaleDate || '',
      priceLevel1: p.priceLevel1 || 0, priceLevel2: p.priceLevel2 || 0,
      priceLevel3: p.priceLevel3 || 0, priceLevel4: p.priceLevel4 || 0,
      priceLevel5: p.priceLevel5 || 0, priceLevel6: p.priceLevel6 || 0,
      priceLevel7: p.priceLevel7 || 0, priceLevel8: p.priceLevel8 || 0,
      availableStock: p.availableStock || 0,
      bookStock: p.bookStock || 0,
      volume: p.volume || 0,
      weight: p.weight || 0,
      region: p.region || '',
      location: p.location || '',
      productAttribute: p.productAttribute || '',
      gift: p.gift || false,
      remark: p.remark || '',
      // 表体自定义字段
      extNum1: p.extNum1 || 0, extNum2: p.extNum2 || 0, extNum3: p.extNum3 || 0,
      extText1: p.extText1 || '', extText2: p.extText2 || '',
      extNum4: p.extNum4 || 0, extNum5: p.extNum5 || 0,
      extPartner: p.extPartner, extStaff: p.extStaff, extDept: p.extDept,
    })),
  }),
})

// 初始化预订货单特有字段
if (!('customerId' in formData)) {Object.assign(formData, {
  customerId: undefined, customerName: '', customerCode: '',
  bankName: '', bankAccount: '', taxNo: '',
  warehouseId: undefined, warehouseName: '',
  handlerId: undefined, handlerName: '',
  deptId: undefined, deptName: '',
  orderDate: new Date().toISOString().slice(0, 10),
  saleType: 0,
  receiverName: '', receiverPhone: '', shippingAddress: '',
  customerLevel: '',
  depositAccount1: '', depositAccount2: '', depositAccount3: '', depositAccount4: '',
  depositAmount: 0,
  creditLimit: 0, depositDeadline: '',
  summary: '', remark: '',
  extNum1: undefined, extNum2: undefined,
  extText1: '', extText2: '', extText3: '',
  footerExtText1: '', footerExtText2: '',
  customerTicket: '', customerRemark: '',
  attachment: '',
  printCount: 0,
})}

// ── 计算属性 ──
/** 本单金额 = 折后金额合计（有折扣取discountedAmount，无折扣取qty×price） */
const totalWithTax = computed(() => formData.products.reduce((s: number, p: any) => {
  const discountedAmt = p.discountedAmount || 0
  const rawAmt = (p.quantity || 0) * (p.unitPrice || 0)
  return s + (discountedAmt > 0 ? discountedAmt : rawAmt)
}, 0))

const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'pieceQuantity', value: formData.products.reduce((s: number, p: any) => s + (p.pieceQuantity || 0), 0) },
  { key: 'amount', value: totalAmount.value, highlight: true },
  { key: 'discountedAmount', value: formData.products.reduce((s: number, p: any) => s + (p.discountedAmount || 0), 0), highlight: true },
])

// ═══════════════════════════════════════
// BillFormPage 配置
// ═══════════════════════════════════════

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '预订货单',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined, children: [
      { key: 'print-pre-order', label: '打印预订货单' },
      { key: 'print-summary', label: '打印汇总' },
    ]},
    { key: 'config', label: '', icon: SettingOutlined, children: [
      { key: 'page-config', label: '页面配置' },
      { key: 'column-config', label: '列配置' },
    ]},
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'import', label: '导入', icon: ImportOutlined },
    { key: 'more', label: '更多', children: [
      { key: 'save-draft', label: '保存草稿' },
      { key: 'copy-pre-order', label: '复制预订货单' },
      { key: 'export', label: '导出' },
    ]},
  ],
}))

const basicInfoFields = computed<BasicInfoField[]>(() => {
  const allFields: BasicInfoField[] = [
    // ═══ Row 1: 核心6字段（与出库单一致的简洁布局） ═══
    { key: 'customerId', label: '客户', type: 'select', required: true, inlineLabel: true, width: 320, options: optionRefs.customers.map((c: any) => ({ label: c.name, value: c.id })), searchBtn: '+Q', loading: loadingOptions.value },
    { key: 'warehouseId', label: '发货仓库', type: 'select', required: true, inlineLabel: true, width: 220, options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
    { key: 'handlerId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 220, options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
    { key: 'orderDate', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 200 },
    { key: 'saleType', label: '销售类型', type: 'select', required: true, inlineLabel: true, width: 200, options: [{ label: '正常销售', value: 0 }, { label: '样品销售', value: 1 }, { label: '促销销售', value: 2 }] },
    // ═══ Row 2: 收货信息3字段 ═══
    { key: 'receiverName', label: '收货人', type: 'input', inlineLabel: true, width: 200, searchBtn: 'Q' },
    { key: 'receiverPhone', label: '联系电话', type: 'input', inlineLabel: true, width: 220 },
    { key: 'shippingAddress', label: '收货地址', type: 'input', inlineLabel: true, width: 500 },
  ]
  return allFields.filter(f => isFieldVisible(f.key))
})

const tabsConfig = computed<BillTabConfig[]>(() => {
  const allTabs: BillTabConfig[] = [
    // ═══ Tab 1: 收款（预订金 + 客户银行信息） ═══
    { key: 'deposit', tab: '收款', fields: [
      // Row 1: 预订金核心
      { key: 'depositAccount1', label: '预订金账户', type: 'input', placeholder: '请选择', suffixBtn: '+Q' },
      { key: 'depositAmount', label: '预订金金额', type: 'number', disabled: true, suffixBtn: '全清', suffixBtnDanger: true },
      { key: '_moreAccount', label: '更多账户', type: 'input', disabled: true, suffixBtn: '···' },
      { key: 'creditLimit', label: '信用额度', type: 'number', precision: 2 },
      { key: 'depositDeadline', label: '收款期限', type: 'input' },
      // Row 2: 更多预订金账户
      { key: 'depositAccount2', label: '预订金账户2', type: 'input', suffixBtn: '+Q' },
      { key: 'depositAccount3', label: '预订金账户3', type: 'input', suffixBtn: '+Q' },
      { key: 'depositAccount4', label: '预订金账户4', type: 'input', suffixBtn: '+Q' },
      // Row 3: 客户银行/税务信息（从basicInfo移入）
      { key: 'customerCode', label: '客户编号', type: 'input' },
      { key: 'bankName', label: '开户行', type: 'input' },
      { key: 'bankAccount', label: '银行账号', type: 'input' },
      { key: 'taxNo', label: '税号', type: 'input' },
      { key: 'customerLevel', label: '客户级别', type: 'input' },
    ]},
    // ═══ Tab 2: 扩展信息（部门、自定义字段、审核、摘要、表尾） ═══
    { key: 'extended', tab: '扩展信息', fields: [
      // 组织
      { key: 'deptId', label: '部门', type: 'input' },
      // 自定义字段1-5
      { key: 'extNum1', label: '自定义字段1(数字)', type: 'number', precision: 2 },
      { key: 'extNum2', label: '自定义字段2(数字)', type: 'number', precision: 2 },
      { key: 'extText1', label: '自定义字段3(文本)', type: 'input' },
      { key: 'extText2', label: '自定义字段4(文本)', type: 'input' },
      { key: 'extText3', label: '自定义字段5(文本)', type: 'input' },
      // 审核/摘要
      { key: 'auditorName', label: '审核人', type: 'input' },
      { key: 'summary', label: '摘要', type: 'input' },
      // 表尾自定义
      { key: 'footerExtText1', label: '表尾自定义字段1', type: 'input' },
      { key: 'footerExtText2', label: '表尾自定义字段2', type: 'input' },
    ]},
  ]
  return allTabs.map(tab => ({
    ...tab,
    fields: tab.fields.filter((f: any) => isFieldVisible(f.key)),
  })).filter(tab => tab.fields.length > 0)
})

const summaryConfig = computed<SummaryRow[]>(() => {
  const allRows: (SummaryRow & { fieldKey?: string })[] = [
    { fieldKey: 'totalAmount', label: '商品金额', value: totalAmount.value.toFixed(2) },
    { fieldKey: 'orderAmount', label: '本单金额', value: totalWithTax.value.toFixed(2) },
  ]
  return allRows.filter(r => !r.fieldKey || isFieldVisible(r.fieldKey))
})

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

// ═══════════════════════════════════════
// 明细表格列配置（61列）
// ═══════════════════════════════════════

const detailColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 50, fixed: 'left' },
  { key: 'imageUrl', title: '图片', type: 'input', width: 80 },
  { key: 'productId', title: '商品名称', type: 'input', searchable: true,
    options: optionRefs.products.map((p: any) => ({ label: p.name, value: p.id, searchText: `${p.name} ${p.code || ''} ${p.barcode || ''}` })),
    width: 200, showScanToggle: true },
  { key: 'itemCode', title: '货号', type: 'input', width: 100 },
  { key: 'barcode', title: '条码', type: 'input', width: 120 },
  { key: 'region', title: '区域', type: 'input', width: 80 },
  { key: 'location', title: '货位', type: 'input', width: 80 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'model', title: '型号', type: 'input', width: 100 },
  { key: 'origin', title: '产地', type: 'input', width: 80 },
  { key: 'brand', title: '品牌', type: 'input', width: 80 },
  { key: 'pricingUnit', title: '计价单位', type: 'input', width: 80 },
  { key: 'availableStock', title: '可用库存', type: 'number', width: 90, precision: 2 },
  { key: 'availableStockConversion', title: '可用库存换算结果', type: 'number', width: 120, precision: 2 },
  { key: 'bookStock', title: '账面库存', type: 'number', width: 90, precision: 2 },
  { key: 'quantity', title: '数量', type: 'number', width: 80, precision: 2 },
  { key: 'conversionRelation', title: '换算关系', type: 'input', width: 90 },
  { key: 'pieceQuantity', title: '件散数量', type: 'number', width: 90, precision: 2 },
  { key: 'bigPack', title: '大包装', type: 'number', width: 80, precision: 0 },
  { key: 'midPack', title: '中包装', type: 'number', width: 80, precision: 0 },
  { key: 'smallPack', title: '小包装', type: 'number', width: 80, precision: 0 },
  { key: 'lastSaleDate', title: '最近销售日期', type: 'date', width: 110 },
  { key: 'retailPrice', title: '零售价', type: 'number', width: 80, precision: 2 },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 80, precision: 2 },
  { key: 'minSalePrice', title: '最低售价', type: 'number', width: 80, precision: 2 },
  { key: 'unitPrice', title: '单价', type: 'number', width: 80, precision: 2 },
  { key: 'amount', title: '金额', type: 'number', width: 90, precision: 2 },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 70 },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 90, precision: 2 },
  { key: 'smallUnitQuantity', title: '小单位数量', type: 'number', width: 90, precision: 2 },
  { key: 'orderedQuantity', title: '已订数量', type: 'number', width: 80, precision: 2, readonly: true },
  { key: 'shippedQuantity', title: '已发数量', type: 'number', width: 80, precision: 2, readonly: true },
  { key: 'costPrice', title: '参考成本单价', type: 'number', width: 100, precision: 2 },
  { key: 'costAmount', title: '参考成本金额', type: 'number', width: 100, precision: 2 },
  { key: 'grossProfit', title: '参考毛利', type: 'number', width: 80, precision: 2 },
  { key: 'discountRate', title: '折扣(%)', type: 'number', width: 80, precision: 1 },
  { key: 'discountedPrice', title: '折后单价', type: 'number', width: 80, precision: 2 },
  { key: 'discountedAmount', title: '折后金额', type: 'number', width: 90, precision: 2 },
  { key: 'volume', title: '体积（m³）', type: 'number', width: 90, precision: 4 },
  { key: 'weight', title: '重量（kg）', type: 'number', width: 90, precision: 2 },
  { key: 'productAttribute', title: '商品行属性', type: 'input', width: 100 },
  { key: 'gift', title: '赠品', type: 'select', width: 70, options: [{ label: '是', value: 1 }, { label: '否', value: 0 }] },
  { key: 'remark', title: '备注', type: 'input', width: 150 },
  // 8个价格等级（标准化产品价格等级）
  { key: 'priceLevel1', title: '餐饮店', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel2', title: '食堂团餐', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel3', title: '外围餐饮店', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel4', title: '自助vip', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel5', title: '大团餐', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel6', title: '重点|vip01', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel7', title: '连锁|vip', type: 'number', width: 90, precision: 2 },
  { key: 'priceLevel8', title: '特价客户', type: 'number', width: 90, precision: 2 },
  // 10个自定义字段
  { key: 'extNum1', title: '单据自定义1(数字)', type: 'number', width: 120, precision: 2 },
  { key: 'extNum2', title: '单据自定义2(数字)', type: 'number', width: 120, precision: 2 },
  { key: 'extNum3', title: '单据自定义3(数字)', type: 'number', width: 120, precision: 2 },
  { key: 'extText1', title: '单据自定义4(文本)', type: 'input', width: 120 },
  { key: 'extText2', title: '单据自定义5(文本)', type: 'input', width: 120 },
  { key: 'extNum4', title: '单据自定义6(数字)', type: 'number', width: 120, precision: 2 },
  { key: 'extNum5', title: '单据自定义7(数字)', type: 'number', width: 120, precision: 2 },
  { key: 'extPartner', title: '单据自定义8(往来单位)', type: 'input', width: 130 },
  { key: 'extStaff', title: '单据自定义9(职员)', type: 'input', width: 120 },
  { key: 'extDept', title: '单据自定义10(部门)', type: 'input', width: 120 },
])

// ═══ 数据表格列配置 ═══
const formColumnDefs = computed(() => detailColumns.value.map(col => ({
  title: col.title || col.key,
  field: col.key,
  key: col.key,
  width: col.width || 100,
})))
const {
  visibleColumns: formVisibleColumns,
  showPanel: _formShowPanel,
  onSettingChange: onFormColumnSettingChange,
  resetSettings: resetFormColumnSettings,
  settingsColumns: formSettingsColumns,
} = useColumnConfig(formColumnDefs.value, 'sale-pre-order-form-columns')

const visibleDetailColumns = computed<DetailColumnConfig[]>(() => {
  const visibleKeys = new Set(formVisibleColumns.value.map((c: any) => c.key))
  return detailColumns.value.filter(col => visibleKeys.has(col.key) || col.type === 'action' || col.type === 'rowNo')
})

function handleFormColumnConfigChange() { onFormColumnSettingChange() }
function handleFormColumnConfigReset() { resetFormColumnSettings() }

// ═══════════════════════════════════════
// 重算函数
// ═══════════════════════════════════════

/** 重算行金额 = 数量 × 单价 */
function recalcLineAmount(record: any) {
  const qty = Number(record.quantity) || 0
  const price = Number(record.unitPrice) || 0
  record.amount = +(qty * price).toFixed(2)
  if (record.discountRate) recalcDiscount(record)
  if (record.costPrice) recalcCost(record)
}

/** 重算折后金额 */
function recalcDiscount(record: any) {
  const amount = Number(record.amount) || 0
  const rate = Number(record.discountRate) || 0
  record.discountedPrice = +(record.unitPrice * (1 - rate / 100)).toFixed(4)
  record.discountedAmount = +(amount * (1 - rate / 100)).toFixed(2)
  if (record.costPrice) recalcCost(record)
}

/** 重算成本金额和毛利 */
function recalcCost(record: any) {
  const qty = Number(record.quantity) || 0
  const cost = Number(record.costPrice) || 0
  record.costAmount = +(qty * cost).toFixed(2)
  const revenue = Number(record.discountedAmount) || Number(record.amount) || 0
  record.grossProfit = +(revenue - record.costAmount).toFixed(2)
}

// ═══════════════════════════════════════
// 事件处理
// ═══════════════════════════════════════

function handleInsertProduct(index: number) {
  const newProduct = {
    id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
    productId: undefined, imageUrl: '',
    itemCode: '', barcode: '', specification: '', model: '', origin: '', brand: '',
    unit: '', batchCode: '', pricingUnit: '', smallUnit: '', smallUnitQuantity: 0,
    conversionRelation: '', conversionResult: 0,
    region: '', location: '',
    unitPrice: 0, taxRate: 0, scanMode: false,
    retailPrice: 0, wholesalePrice: 0, minSalePrice: 0,
    bigPack: 0, midPack: 0, smallPack: 0, pieceQuantity: 0,
    availableStock: 0, availableStockConversion: 0, bookStock: 0,
    productAttribute: '', gift: false,
    costPrice: 0, costAmount: 0, grossProfit: 0,
    volume: 0, weight: 0,
    discountRate: 0, discountedPrice: 0, discountedAmount: 0,
    smallUnitPrice: 0, lastSaleDate: '',
    priceLevel1: 0, priceLevel2: 0, priceLevel3: 0, priceLevel4: 0,
    priceLevel5: 0, priceLevel6: 0, priceLevel7: 0, priceLevel8: 0,
    extNum1: 0, extNum2: 0, extNum3: 0, extText1: '', extText2: '',
    extNum4: 0, extNum5: 0,
    productName: '', quantity: 0, orderedQuantity: 0, shippedQuantity: 0, remark: '',
  }
  formData.products.splice(index + 1, 0, newProduct)
}

function handleFieldChange(fieldKey: string, val: any) { baseFieldChange(fieldKey, val) }

function handleCellChange(record: any, fieldKey: string, value: any) {
  if (fieldKey === 'productId' && value != null) {
    const p = optionRefs.products.find((x: any) => x.id === value)
    if (p) {
      record.productCode = p.code || ''
      record.productName = p.name || ''
      record.itemCode = p.code || ''
      record.barcode = p.barcode || ''
      record.specification = p.specification || ''
      record.model = p.model || ''
      record.origin = p.origin || ''
      record.brand = p.brand || ''
      record.unit = p.unit || ''
      record.pricingUnit = p.unit || ''
      record.unitPrice = p.salePrice || p.price || 0
      record.retailPrice = p.retailPrice || 0
      record.wholesalePrice = p.wholesalePrice || 0
      record.minSalePrice = p.minSalePrice || 0
      record.availableStock = p.stock || 0
      record.bookStock = p.bookStock || 0
      record.costPrice = p.costPrice || 0
      record.volume = p.volume || 0
      record.weight = p.weight || 0
      // 价格等级快照
      record.priceLevel1 = p.priceLevel1 || 0
      record.priceLevel2 = p.priceLevel2 || 0
      record.priceLevel3 = p.priceLevel3 || 0
      record.priceLevel4 = p.priceLevel4 || 0
      record.priceLevel5 = p.priceLevel5 || 0
      record.priceLevel6 = p.priceLevel6 || 0
      record.priceLevel7 = p.priceLevel7 || 0
      record.priceLevel8 = p.priceLevel8 || 0
      recalcLineAmount(record)
    }
  }
  if (fieldKey === 'quantity' || fieldKey === 'unitPrice') {
    recalcLineAmount(record)
    // 库存预警
    if (fieldKey === 'quantity' && record.quantity != null && record.availableStock != null
        && record.quantity > record.availableStock) {
      message.warning(`商品「${record.productName || ''}」订货数量(${record.quantity})超过可用库存(${record.availableStock})`)
    }
  }
  if (fieldKey === 'discountRate') {
    recalcDiscount(record)
  }
  if (fieldKey === 'costPrice') {
    recalcCost(record)
  }
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
    quickSearchOptions.value = optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id }))
    quickSearchValue.value = formData.warehouseId
    showQuickSearch.value = true
  } else if (fieldKey === 'handlerId' && optionRefs.users?.length) {
    quickSearchTitle.value = '经手人'
    quickSearchFieldKey.value = 'handlerId'
    quickSearchOptions.value = optionRefs.users.map((u: any) => ({ label: `${u.name}${u.deptName ? `(${u.deptName})` : ''}`, value: u.id }))
    quickSearchValue.value = formData.handlerId
    showQuickSearch.value = true
  } else if (fieldKey === 'receiverName') {
    // 'Q'按钮默认聚焦，实际用户可直接输入
    message.info('可直接输入收货人名称')
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
  } else if (fieldKey === 'handlerId') {
    formData.handlerId = val
    handleFieldChange('handlerId', val)
  }
  showQuickSearch.value = false
}

function handleOpenProductSelectModal(_record: any, rowIndex: number, fieldKey: string) {
  if (fieldKey === 'productId') { currentSelectRowIndex.value = rowIndex; showProductSelect.value = true }
}

function handleProductSelectConfirm(products: any[]) {
  const startIndex = currentSelectRowIndex.value >= 0 ? currentSelectRowIndex.value : formData.products.length
  for (let i = startIndex; i < startIndex + products.length; i++) {
    if (i >= formData.products.length) handleAddProduct()
  }
  products.forEach((p: any, i: number) => {
    const rowIndex = startIndex + i
    if (rowIndex < formData.products.length) {
      const row = formData.products[rowIndex]
      row.productId = p.id
      row.productName = p.name || ''
      row.itemCode = p.code || ''
      row.barcode = p.barcode || ''
      row.specification = p.specification || ''
      row.model = p.model || ''
      row.origin = p.origin || ''
      row.brand = p.brand || ''
      row.unit = p.unit || ''
      row.pricingUnit = p.unit || ''
      row.unitPrice = p.retailPrice || p.salePrice || p.price || 0
      row.retailPrice = p.retailPrice || 0
      row.wholesalePrice = p.wholesalePrice || 0
      row.minSalePrice = p.minSalePrice || 0
      row.availableStock = p.stock || 0
      row.bookStock = p.bookStock || 0
      row.costPrice = p.costPrice || 0
      row.volume = p.volume || 0
      row.weight = p.weight || 0
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
    case 'history': router.push('/sales/pre-order'); break
    case 'page-config':
      showFormConfig.value = true
      break
    case 'column-config':
      showFormColumnConfig.value = true
      break
    case 'save-draft': handleSaveDraft(); break
    case 'print-pre-order':
    case 'print-summary':
      if (formData.id) {
        preOrderApi.print(formData.id).then(() => {
          formData.printCount = (formData.printCount || 0) + 1
          message.success('打印计数已递增')
          window.print()
        }).catch(() => {
          window.print()
        })
      } else {
        window.print()
      }
      break
    case 'copy-pre-order':
      const copyData = { ...formData }
      delete copyData.id
      delete copyData.orderNo
      delete copyData.status
      delete copyData.createTime
      delete copyData.submitTime
      delete copyData.printCount
      if (copyData.products) {
        copyData.products = copyData.products.map((p: any) => {
          const { id, ...rest } = p
          return rest
        })
      }
      sessionStorage.setItem('preOrderCopyData', JSON.stringify(copyData))
      router.push('/sales/pre-order/create')
      break
    case 'export':
      if (formData.id) {
        window.open(`/api/erp/sale/pre-order/export?id=${formData.id}`, '_blank')
        message.success('导出任务已提交')
      } else {
        message.warning('请先保存预订货单')
      }
      break
    case 'import':
      message.info('导入功能待完善，可使用新增按钮逐条录入')
      break
  }
}

function handleFormConfigChange(_config: any) {
  loadPageConfig()
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

/** 创建模式时从录单默认值配置加载预设值 */
function loadAndApplyDefaults() {
  try {
    const raw = localStorage.getItem('sale-pre-order-form-default-config')
    if (!raw) return
    const parsed = JSON.parse(raw)
    const fields = parsed?.fields || []
    for (const df of fields) {
      if (!df.value) continue
      if (df.key === 'customerId') {
        formData.customerId = Number(df.value)
        handleFieldChange('customerId', Number(df.value))
      } else if (df.key === 'warehouseId') {
        formData.warehouseId = Number(df.value)
        handleFieldChange('warehouseId', Number(df.value))
      } else if (df.key === 'handlerId') {
        formData.handlerId = Number(df.value)
        handleFieldChange('handlerId', Number(df.value))
      } else if (df.key === 'deptId') {
        formData.deptId = Number(df.value)
      } else if (df.key === 'saleType') {
        formData.saleType = Number(df.value)
      }
    }
  } catch { /* ignore parse errors */ }
}

// ═══ 快捷键 ═══
function onKeyDown(e: KeyboardEvent) {
  if (e.ctrlKey && e.key === 's') {
    e.preventDefault()
    handleSaveDraft()
  }
  if (e.key === 'F8') {
    e.preventDefault()
    handleAction('print-pre-order')
  }
}

onMounted(() => {
  if (formData.products.length === 0) for (let i = 0; i < 20; i++) handleAddProduct()
  // 创建模式时应用录单默认值
  if (!route.params.id) {
    loadAndApplyDefaults()
  }
  nextTick(() => {  window.addEventListener('keydown', onKeyDown)
})
})

onUnmounted(() => {
  window.removeEventListener('keydown', onKeyDown)
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
.doc-info-link { color: #1890ff; font-size: 12px; }
</style>
