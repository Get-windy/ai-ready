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
      :global-config-key="'sale-pre-order-form-detail-columns'"
      :is-locked-column="isLockedColumn"
      @update:open="showFormColumnConfig = $event"
      @change="handleFormColumnConfigChange"
      @reset="handleFormColumnConfigReset"
      @drag-end="handleFormColumnConfigChange"
      @global-config-change="handleGlobalColumnConfigChange"
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

    <!-- ═══ 更多账户弹窗（预订金账户2~4） ═══ -->
    <a-modal
      v-model:open="showMoreAccounts"
      title="更多预订金账户"
      width="520px"
      :footer="null"
      destroy-on-close
    >
      <div
        v-for="acc in MORE_ACCOUNT_FIELDS"
        :key="acc.key"
        class="more-account-row"
      >
        <span class="more-account-label">{{ acc.label }}</span>
        <a-select
          v-model:value="formData[acc.key]"
          show-search
          allow-clear
          size="small"
          placeholder="请选择账户"
          style="flex:1"
          :options="accountOptions"
          :filter-option="filterOption"
        />
      </div>
      <div class="more-account-footer">
        <a-button
          type="primary"
          size="small"
          @click="showMoreAccounts = false"
        >
          确定
        </a-button>
      </div>
    </a-modal>

    <!-- ═══ 批量导入隐藏文件输入（Excel/CSV 明细回填） ═══ -->
    <input
      ref="fileInputRef"
      type="file"
      accept=".xlsx,.xls,.csv"
      style="display:none"
      @change="handleImportFileChange"
    >
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
import { preOrderApi, userPageConfigApi } from '@/api/erp'
import optionsApi from '@/api/options'
import { PRODUCT_EXTEND_DEFAULTS } from '@/utils/productDefaults'
import { useUserStore } from '@/stores/user'
import * as XLSX from 'xlsx'

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
// ═══ 更多预订金账户弹窗 ═══
const showMoreAccounts = ref(false)
const accountOptions = ref<{ label: string; value: string; code?: string }[]>([])
const MORE_ACCOUNT_FIELDS = [
  { key: 'depositAccount2', label: '预订金账户2' },
  { key: 'depositAccount3', label: '预订金账户3' },
  { key: 'depositAccount4', label: '预订金账户4' },
]
// ═══ 批量导入 ═══
const fileInputRef = ref<HTMLInputElement | null>(null)

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

/** 页面配置「显示名」生效：配置改名后表单标签同步 */
function fieldLabel(fieldKey: string, fallback: string): string {
  const field = pageConfigFields.value.find((f: any) => f.key === fieldKey)
  return (field && field.displayName) ? field.displayName : fallback
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
  // 单号必须来自后端号段（严禁前端演示自增号）
  codeApiPath: '/erp/sale/pre-order/next-no',
  api: { create: preOrderApi.create, update: preOrderApi.update, getById: preOrderApi.getById },
  redirectPath: '/sales/pre-order',
  optionTypes: ['customers', 'warehouses', 'users', 'products'],
  productDefaults: PRODUCT_EXTEND_DEFAULTS,
  onDetailLoaded: (data, fd) => {
    // 明细：列表用 itemCode 承载「货号」，后端字段为 productCode，编辑回填时补齐
    fd.products.forEach((p: any) => {
      if (!p.itemCode) p.itemCode = p.productCode || ''
      if (p.gift === 1) p.gift = true
    })
    // 主表：预订金账户已由 Object.assign 展开，这里补齐账户下拉显示
    if (data.depositDeadline && typeof data.depositDeadline === 'string') {
      fd.depositDeadline = data.depositDeadline.slice(0, 10)
    }
  },
  onFieldChange: (fieldKey, val, fd) => {
    // 显式回写选择值：BillFormPage 以「新对象 emit」的方式更新，父级 reactive 需显式赋值，
    // 否则仅快照字段（客户名等）落库，ID 字段（customerId/warehouseId/handlerId）会丢失。
    if (['customerId', 'warehouseId', 'handlerId', 'deptId'].includes(fieldKey)) {
      fd[fieldKey] = val
    }
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
    depositDeadline: fd.depositDeadline || null,
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
    { key: 'import', label: '导入', icon: ImportOutlined, children: [
      { key: 'import-excel', label: 'Excel 导入明细' },
      { key: 'download-import-template', label: '下载导入模板' },
    ]},
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
    { key: 'customerId', label: fieldLabel('customerId', '客户'), type: 'select', required: true, inlineLabel: true, width: 320, options: optionRefs.customers.map((c: any) => ({ label: c.name, value: c.id })), searchBtn: '+Q', loading: loadingOptions.value },
    { key: 'warehouseId', label: fieldLabel('warehouseId', '发货仓库'), type: 'select', required: true, inlineLabel: true, width: 220, options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
    { key: 'handlerId', label: fieldLabel('handlerId', '经手人'), type: 'select', required: true, inlineLabel: true, width: 220, options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
    { key: 'orderDate', label: fieldLabel('orderDate', '单据日期'), type: 'date', required: true, inlineLabel: true, width: 200 },
    { key: 'saleType', label: fieldLabel('saleType', '销售类型'), type: 'select', required: true, inlineLabel: true, width: 200, options: [{ label: '正常销售', value: 0 }, { label: '样品销售', value: 1 }, { label: '促销销售', value: 2 }] },
    // ═══ Row 2: 收货信息3字段 ═══
    { key: 'receiverName', label: fieldLabel('receiverName', '收货人'), type: 'input', inlineLabel: true, width: 200 },
    { key: 'receiverPhone', label: fieldLabel('receiverPhone', '联系电话'), type: 'input', inlineLabel: true, width: 220 },
    { key: 'shippingAddress', label: fieldLabel('shippingAddress', '收货地址'), type: 'input', inlineLabel: true, width: 500 },
  ]
  return allFields.filter(f => isFieldVisible(f.key))
})

const tabsConfig = computed<BillTabConfig[]>(() => {
  const allTabs: BillTabConfig[] = [
    // ═══ Tab 1: 收款（预订金 + 客户银行信息） ═══
    { key: 'deposit', tab: '收款', fields: [
      // Row 1: 预订金核心
      { key: 'depositAccount1', label: fieldLabel('depositAccount1', '预订金账户'), type: 'select', placeholder: '请选择', options: accountOptions.value },
      { key: 'depositAmount', label: fieldLabel('depositAmount', '预订金金额'), type: 'number', disabled: true, suffixBtn: '全清', suffixBtnDanger: true },
      { key: 'moreAccounts', label: fieldLabel('moreAccounts', '更多账户'), type: 'input', disabled: true, placeholder: '账户2~4', suffixBtn: '···' },
      { key: 'creditLimit', label: fieldLabel('creditLimit', '信用额度'), type: 'number', precision: 2 },
      { key: 'depositDeadline', label: fieldLabel('depositDeadline', '收款期限'), type: 'date' },
      // Row 2: 客户银行/税务信息（从basicInfo移入）
      { key: 'customerCode', label: fieldLabel('customerCode', '客户编号'), type: 'input' },
      { key: 'bankName', label: fieldLabel('bankName', '开户行'), type: 'input' },
      { key: 'bankAccount', label: fieldLabel('bankAccount', '银行账号'), type: 'input' },
      { key: 'taxNo', label: fieldLabel('taxNo', '税号'), type: 'input' },
      { key: 'customerLevel', label: fieldLabel('customerLevel', '客户级别'), type: 'input' },
    ]},
    // ═══ Tab 2: 扩展信息（部门、自定义字段、审核、摘要、表尾） ═══
    { key: 'extended', tab: '扩展信息', fields: [
      // 组织
      { key: 'deptId', label: fieldLabel('deptId', '部门'), type: 'input' },
      // 自定义字段1-5
      { key: 'extNum1', label: fieldLabel('extNum1', '自定义字段1(数字)'), type: 'number', precision: 2 },
      { key: 'extNum2', label: fieldLabel('extNum2', '自定义字段2(数字)'), type: 'number', precision: 2 },
      { key: 'extText1', label: fieldLabel('extText1', '自定义字段3(文本)'), type: 'input' },
      { key: 'extText2', label: fieldLabel('extText2', '自定义字段4(文本)'), type: 'input' },
      { key: 'extText3', label: fieldLabel('extText3', '自定义字段5(文本)'), type: 'input' },
      // 审核/摘要
      { key: 'auditorName', label: fieldLabel('auditorName', '审核人'), type: 'input' },
      { key: 'summary', label: fieldLabel('summary', '摘要'), type: 'input' },
      // 表尾自定义
      { key: 'footerExtText1', label: fieldLabel('footerExtText1', '表尾自定义字段1'), type: 'input' },
      { key: 'footerExtText2', label: fieldLabel('footerExtText2', '表尾自定义字段2'), type: 'input' },
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

/** 商品主数据 → 明细行字段快照（选品/扫码/导入共用，避免多处重复映射） */
function fillRowFromProduct(row: any, p: any) {
  row.productId = p.id
  row.productName = p.name || ''
  row.productCode = p.code || ''
  row.itemCode = p.code || ''
  row.barcode = p.barcode || ''
  row.specification = p.specification || ''
  row.model = p.model || ''
  row.origin = p.origin || ''
  row.brand = p.brand || ''
  row.unit = p.unit || ''
  row.smallUnit = p.smallUnit || ''
  row.pricingUnit = p.unit || ''
  row.unitPrice = p.salePrice || p.price || p.retailPrice || 0
  row.retailPrice = p.retailPrice || 0
  row.wholesalePrice = p.wholesalePrice || 0
  row.minSalePrice = p.minSalePrice || 0
  row.availableStock = p.stock || 0
  row.bookStock = p.bookStock || 0
  row.costPrice = p.costPrice || 0
  row.volume = p.volume || 0
  row.weight = p.weight || 0
  // 价格等级快照（8 个标准化价格等级）
  for (let i = 1; i <= 8; i++) row[`priceLevel${i}`] = p[`priceLevel${i}`] || 0
}

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
      fillRowFromProduct(record, p)
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
  } else if (fieldKey.startsWith('depositAccount')) {
    formData[fieldKey] = val
  }
  showQuickSearch.value = false
}

/** Tab 字段右侧按钮（全清 / 更多账户 ···） */
function handleTabSuffixBtn(fieldKey: string, btnText: string) {
  if (fieldKey === 'depositAmount' && btnText === '全清') {
    formData.depositAmount = 0
    return
  }
  if (fieldKey === 'moreAccounts') {
    showMoreAccounts.value = true
    return
  }
  if (fieldKey === 'depositAccount1') {
    openAccountQuickSearch('depositAccount1', '预订金账户')
  }
}

/** 账户选择：账户下拉为空时兜底加载 */
async function openAccountQuickSearch(fieldKey: string, label: string) {
  if (!accountOptions.value.length) await loadAccountOptions()
  quickSearchTitle.value = label
  quickSearchFieldKey.value = fieldKey
  quickSearchOptions.value = accountOptions.value.map(a => ({ label: a.code ? `${a.label}[${a.code}]` : a.label, value: a.value }))
  quickSearchValue.value = formData[fieldKey]
  showQuickSearch.value = true
}

async function loadAccountOptions() {
  try {
    const accounts = await optionsApi.getAccounts()
    accountOptions.value = (accounts || []).map((a: any) => ({
      label: a.name || a.accountName || '',
      value: a.name || a.accountName || '',
      code: a.code || '',
    }))
  } catch {
    accountOptions.value = []
  }
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
      fillRowFromProduct(row, p)
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
      // 导出当前单据（按单号过滤），未保存时导出列表查询结果
      window.open(`/api/erp/sale/pre-order/export?keyword=${encodeURIComponent(formData.orderNo || '')}`, '_blank')
      message.success('导出任务已提交')
      break
    case 'import':
    case 'import-excel':
      fileInputRef.value?.click()
      break
    case 'download-import-template':
      downloadImportTemplate()
      break
  }
}

/** 全局列配置落后端（跨浏览器/终端生效） */
async function handleGlobalColumnConfigChange(settings: any[]) {
  try {
    await userPageConfigApi.save('col-config', 'sale-pre-order-form-detail-columns', JSON.stringify(settings))
  } catch (err: any) {
    message.warning(`全局列配置保存失败：${err?.message || ''}`)
  }
}

// ═══════════════════════════════════════
// 批量导入（Excel/CSV → 明细回填，模板可下载）
// ═══════════════════════════════════════
const IMPORT_HEADERS = ['货号', '条码', '商品名称', '数量', '单价', '备注']

/** 下载导入模板（表头 + 示例行） */
function downloadImportTemplate() {
  const ws = XLSX.utils.aoa_to_sheet([
    IMPORT_HEADERS,
    ['SP001', '6901234567890', '示例商品', 10, 12.5, ''],
  ])
  ws['!cols'] = [{ wch: 16 }, { wch: 18 }, { wch: 24 }, { wch: 10 }, { wch: 10 }, { wch: 20 }]
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '预订货明细')
  XLSX.writeFile(wb, '预订货明细导入模板.xlsx')
}

/** 读取导入文件并按「货号/条码/商品名称」匹配商品后回填明细 */
async function handleImportFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    const buf = await file.arrayBuffer()
    const wb = XLSX.read(buf, { type: 'array' })
    const sheet = wb.Sheets[wb.SheetNames[0]]
    if (!sheet) { message.warning('文件中没有可读取的工作表'); return }
    const rows: any[][] = XLSX.utils.sheet_to_json(sheet, { header: 1, blankrows: false })
    if (rows.length < 2) { message.warning('文件中没有数据行'); return }

    const header = (rows[0] || []).map((h: any) => String(h ?? '').trim())
    const idx = (name: string) => header.indexOf(name)
    const iCode = idx('货号')
    const iBarcode = idx('条码')
    const iName = idx('商品名称')
    const iQty = idx('数量')
    const iPrice = idx('单价')
    const iRemark = idx('备注')
    if (iCode < 0 && iBarcode < 0 && iName < 0) {
      message.warning(`表头缺少识别列，需包含「${IMPORT_HEADERS.slice(0, 3).join('/')}」之一`)
      return
    }
    if (!optionRefs.products?.length) {
      try { optionRefs.products = await optionsApi.getProducts() } catch { optionRefs.products = [] }
    }
    const products: any[] = optionRefs.products || []

    let matched = 0
    const unmatched: string[] = []
    for (let r = 1; r < rows.length; r++) {
      const row = rows[r] || []
      const code = iCode >= 0 ? String(row[iCode] ?? '').trim() : ''
      const barcode = iBarcode >= 0 ? String(row[iBarcode] ?? '').trim() : ''
      const pname = iName >= 0 ? String(row[iName] ?? '').trim() : ''
      if (!code && !barcode && !pname) continue
      const p = products.find((x: any) =>
        (code && x.code === code) || (barcode && x.barcode === barcode) || (pname && x.name === pname))
      if (!p) { unmatched.push(code || barcode || pname); continue }

      // 已存在同商品行则累加数量，否则新增行
      let target = formData.products.find((x: any) => x.productId === p.id)
      if (!target) {
        if (formData.products.length === 1 && formData.products[0].productId == null) formData.products.splice(0, 1)
        handleAddProduct()
        target = formData.products[formData.products.length - 1]
      }
      fillRowFromProduct(target, p)
      const qty = iQty >= 0 ? Number(row[iQty]) : NaN
      if (!Number.isNaN(qty) && qty > 0) target.quantity = qty
      const price = iPrice >= 0 ? Number(row[iPrice]) : NaN
      if (!Number.isNaN(price) && price > 0) target.unitPrice = price
      if (iRemark >= 0 && row[iRemark] != null) target.remark = String(row[iRemark])
      recalcLineAmount(target)
      matched++
    }

    if (matched > 0) {
      message.success(`导入完成：回填 ${matched} 行明细`)
    } else {
      message.warning('没有匹配到任何商品，请检查货号/条码/商品名称')
    }
    if (unmatched.length) {
      message.warning(`${unmatched.length} 行未匹配到商品：${unmatched.slice(0, 3).join('、')}${unmatched.length > 3 ? ' 等' : ''}`)
    }
  } catch (err: any) {
    message.error(`导入失败：${err?.message || '文件解析异常'}`)
  } finally {
    input.value = ''
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
      } else if (df.key === 'depositAccount1') {
        formData.depositAccount1 = df.value
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
  // 预订金账户下拉（收款项真实可选）
  loadAccountOptions()
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
