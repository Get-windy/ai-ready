<template>
  <div
    class="form-page-container"
    style="height:100%;display:flex;flex-direction:column;"
  >
    <BillFormPage
      v-model="formData"
      :header="headerConfig"
      :basic-info-fields="visibleBasicInfoFields"
      :tabs="tabsConfig"
      :summary="summaryConfig"
      :footer="footerConfig"
      @action="handleAction"
      @field-change="handleFieldChange"
      @search-btn="handleSearchBtn"
      @draft="handleFormDraft"
      @submit="handleFormSubmit"
    >
      <!-- ═══ 商品明细表格 ═══ -->
      <template #detail-table="{ onExpandChange }">
        <div class="detail-table-header">
          <a-tooltip title="明细列配置">
            <a-button size="small" @click="showDetailColumnConfig = true">
              <SettingOutlined />
            </a-button>
          </a-tooltip>
        </div>
        <BillDetailTable
          :columns="visibleDetailColumns"
          :data-source="formData.products"
          :max-height="tableMaxHeight"
          :summary-columns="tableSummaryColumns"
          :enter-jump-columns="enterJumpColumns"
          :formulas="detailFormulas"
          @cell-change="handleCellChange"
          @expand-change="onExpandChange"
          @open-select-modal="handleOpenProductSelectModal"
        >
          <!-- 操作列 -->
          <template #actionCell="{ index, empty }">
            <template v-if="!empty">
              <a-space :size="2">
                <a-button type="link" size="small" class="action-add-btn" @click="handleInsertProduct(index)">
                  <PlusCircleOutlined />
                </a-button>
                <a-button type="link" size="small" class="action-del-btn" @click="handleRemoveProduct(index)">
                  <MinusCircleOutlined />
                </a-button>
              </a-space>
            </template>
            <template v-else>
              <a-space :size="2">
                <a-button type="link" size="small" class="action-add-btn" @click="handleAddProduct()">
                  <PlusCircleOutlined />
                </a-button>
                <a-button type="link" size="small" class="action-del-btn" disabled>
                  <MinusCircleOutlined />
                </a-button>
              </a-space>
            </template>
          </template>
        </BillDetailTable>
      </template>

      <!-- ═══ 备注 + 单据信息 ═══ -->
      <template #bottom-extra>
        <div class="remark-section">
          <div class="remark-row">
            <span class="remark-label">单据备注</span>
            <a-input v-model:value="formData.remark" size="small" class="remark-input" />
          </div>
          <div class="remark-row">
            <span class="remark-label">买家备注</span>
            <a-input v-model:value="formData.buyerRemark" size="small" class="remark-input" />
          </div>
        </div>
        <div class="doc-info-row">
          <span class="doc-info-item">制单人 <a-tag color="blue" size="small">{{ currentUserName || '系统' }}</a-tag></span>
          <span class="doc-info-item">制单时间 {{ formatNow() }}</span>
          <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
          <a-button type="link" size="small" class="doc-info-link">打印记录</a-button>
        </div>
      </template>
    </BillFormPage>

    <!-- ═══ 产品选择弹窗 ═══ -->
    <ProductSelectModal
      v-model:open="showProductSelect"
      :multiple="true"
      @confirm="handleProductSelectConfirm"
    />

    <!-- ═══ 表单配置弹窗 ═══ -->
    <SaleReturnApplyFormConfig
      :open="showFormConfig"
      @update:open="showFormConfig = $event"
      @change="handleFormConfigChange"
    />

    <!-- ═══ 明细列配置弹窗 ═══ -->
    <ColumnConfigPanel
      :open="showDetailColumnConfig"
      :settings-columns="detailSettingsColumns"
      :is-locked-column="isLockedColumn"
      @update:open="showDetailColumnConfig = $event"
      @change="handleDetailColumnConfigChange"
      @reset="handleDetailColumnConfigReset"
      @drag-end="handleDetailColumnConfigChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted, nextTick } from 'vue'

defineOptions({ name: 'SaleReturnApplyForm' })
import { useRouter, useRoute, onBeforeRouteLeave } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  PrinterOutlined,
  ClockCircleOutlined,
  ImportOutlined,
  ExportOutlined,
  PlusCircleOutlined,
  MinusCircleOutlined,
  SettingOutlined,
} from '@ant-design/icons-vue'
import { Modal } from 'ant-design-vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import ProductSelectModal from '@/components/ProductSelectModal/index.vue'
import ColumnConfigPanel from '@/components/ColumnConfigPanel/index.vue'
import SaleReturnApplyFormConfig from '@/views/erp/column-config/SaleReturnApplyFormConfig.vue'
import { useColumnConfig, isLockedColumn } from '@/composables/useColumnConfig'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, BillTabConfig, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { saleReturnApi } from '@/api/erp'
import optionsApi from '@/api/options'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')

// ═══ 弹窗状态 ═══
const showProductSelect = ref(false)
const showFormConfig = ref(false)
const showDetailColumnConfig = ref(false)
const currentSelectRowIndex = ref(-1)
const tableMaxHeight = ref(400)

// ═══════════════════════════════════════
// useBillForm composable
// ═══════════════════════════════════════

const {
  formData,
  loadingOptions,
  saving,
  optionRefs,
  filterOption,
  effectiveMode,
  handleAddProduct,
  handleRemoveProduct,
  handleFieldChange: baseFieldChange,
  handleSaveDraft,
  handleSubmit,
  totalQuantity,
  totalAmount,
} = useBillForm({
  billPrefix: 'XSTHSQD',
  api: {
    create: saleReturnApi.create,
    update: saleReturnApi.update,
    getById: saleReturnApi.getById,
  },
  redirectPath: '/sales/return-apply',
  optionTypes: ['customers', 'warehouses', 'users', 'products'],
  fields: [
    { key: 'customerId', label: '客户', type: 'select', required: true },
    { key: 'warehouseId', label: '入库仓库', type: 'select', required: true },
    { key: 'handlerId', label: '经手人', type: 'select', required: true },
    { key: 'orderDate', label: '单据日期', type: 'date', required: true },
    { key: 'returnApplyType', label: '销售类型', type: 'select', required: true },
  ],
  productDefaults: {
    itemCode: '', barcode: '', specification: '',
    unit: '', batchCode: '',
    conversionRelation: '', pieceQuantity: 0,
    bigPack: 0, midPack: 0, smallPack: 0,
    unitPrice: 0, taxRate: 13, scanMode: false,
    exchangeGift: '', exchangePoints: 0,
  },
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'customerId') {
      const c = optionRefs.customers.find((x: any) => x.id === val)
      if (c) {
        fd.customerName = c.name
        fd.customerCode = c.code || ''
        fd.customerLevel = c.level || ''
        fd.contactName = c.contactName || ''
        fd.contactPhone = c.contactPhone || ''
        fd.contactAddress = c.address || ''
        fd.bankName = c.bankName || ''
        fd.bankAccount = c.bankAccount || ''
        fd.taxNo = c.taxNo || ''
        // 信用额度联动（对标金蝶/用友/管家婆标准）
        fd.creditLimit = c.creditLimit || 0
        fd.availableCredit = c.availableCredit ?? (c.creditLimit || 0)
        fd.prevDebt = c.currentDebt || c.prevDebt || 0
        fd.debtBalance = c.debtBalance ?? (c.creditLimit || 0) - (c.currentDebt || 0)
      }
    }
  },
  transformPayload: (fd, status) => ({
    ...fd,
    status,
    totalAmount: totalAmount.value,
    returnType: fd.returnApplyType ?? fd.returnType ?? 0,
    items: fd.products.filter((p: any) => p.productId != null).map((p: any) => ({
      productId: p.productId,
      productCode: p.itemCode || p.productCode,
      productName: p.productName,
      barcode: p.barcode,
      specification: p.specification,
      unit: p.unit,
      returnQuantity: p.quantity,
      conversionRelation: p.conversionRelation,
      pieceQuantity: p.pieceQuantity,
      bigPack: p.bigPack || 0,
      midPack: p.midPack || 0,
      smallPack: p.smallPack || 0,
      unitPrice: p.unitPrice,
      lineAmount: (p.quantity || 0) * (p.unitPrice || 0),
      discountRate: p.discountRate || 0,
      discountedPrice: p.discountedPrice || 0,
      discountedAmount: p.discountedAmount || 0,
      exchangeGift: p.exchangeGift,
      exchangePoints: p.exchangePoints || 0,
      remark: p.remark,
      itemRemark: p.itemRemark,
      productLineAttr: p.productLineAttr,
      isGift: p.isGift || false,
      priceLevel1: p.priceLevel1 || 0,
      priceLevel2: p.priceLevel2 || 0,
      priceLevel3: p.priceLevel3 || 0,
      priceLevel4: p.priceLevel4 || 0,
      priceLevel5: p.priceLevel5 || 0,
      priceLevel6: p.priceLevel6 || 0,
      priceLevel7: p.priceLevel7 || 0,
      priceLevel8: p.priceLevel8 || 0,
    })),
  }),
  afterSave: () => resetDirty(),
})

// ═══ 表单 Dirty 标记（对标金蝶/用友/管家婆：未保存修改离开时提示） ═══
const isDirty = ref(false)
let _dirtyInitialized = false
watch(formData, () => {
  if (_dirtyInitialized) isDirty.value = true
  else _dirtyInitialized = true
}, { deep: true })
function resetDirty() { isDirty.value = false }

function handleBeforeUnload(e: BeforeUnloadEvent) {
  if (isDirty.value) { e.preventDefault(); e.returnValue = '' }
}
onMounted(() => window.addEventListener('beforeunload', handleBeforeUnload))

onBeforeRouteLeave((_to, _from, next) => {
  if (isDirty.value && effectiveMode.value !== 'view') {
    Modal.confirm({
      title: '确认离开',
      content: '当前单据有未保存的修改，离开将丢失所有更改，是否继续？',
      okText: '离开',
      okType: 'danger',
      cancelText: '取消',
      onOk: () => next(),
      onCancel: () => next(false),
    })
  } else {
    next()
  }
})

// 初始化销售退货申请特有字段
if (!('customerId' in formData)) {Object.assign(formData, {
  customerId: undefined, customerName: '', customerCode: '', customerLevel: '',
  warehouseId: undefined, warehouseName: '',
  handlerId: undefined, handlerName: '',
  deptId: undefined, deptName: '',
  orderDate: new Date().toISOString().slice(0, 10),
  returnApplyType: 0, salesType: '正常销售',
  contactName: '', contactPhone: '', contactAddress: '',
  expectedReceiveDate: '',
  auditor: '', auditorId: undefined, auditorName: '',
  remark: '', buyerRemark: '', internalNote: '', summary: '',
  printCount: 0,
  // 银行/税务
  bankName: '', bankAccount: '', taxNo: '',
  // 信用额度
  creditLimit: 0, availableCredit: 0, currentDebt: 0, prevDebt: 0, debtBalance: 0,
  collectionDeadline: '',
  settlementMethod: '', settledAmount: 0,
  // 物流
  deliveryMethod: '', deliveryRoute: '', deliveryRouteId: undefined,
  logisticsCompany: '', waybillNo: '', shippingFee: 0,
  deliveryNo: '', freightPayer: '',
  // 会员积分
  memberCardNo: '', prevPoints: 0, memberGeneratedPoints: 0,
  memberExchangePoints: 0, memberUsedPoints: 0, currentPoints: 0,
  // 源单
  sourceOrder: '', sourceOrderId: undefined,
  // 自定义字段
  extNum1: 0, extNum2: 0, extNum3: 0, extNum4: 0, extNum5: 0,
  extText1: '', extText2: '', extText3: '', extText4: '', extText5: '',
  footerExtText1: '', footerExtText2: '',
})}

// ─ 计算属性 ──
const totalBigPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.bigPack || 0), 0))
const totalMidPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.midPack || 0), 0))
const totalSmallPack = computed(() => formData.products.reduce((s: number, p: any) => s + (p.smallPack || 0), 0))
const totalPieceQty = computed(() => formData.products.reduce((s: number, p: any) => s + (p.pieceQuantity || 0), 0))
const totalWeight = computed(() => formData.products.reduce((s: number, p: any) => s + (p.weight || 0), 0))
const totalVolume = computed(() => formData.products.reduce((s: number, p: any) => s + (p.volume || 0), 0))
const totalDiscountedAmount = computed(() => formData.products.reduce((s: number, p: any) => s + (p.discountedAmount || 0), 0))

/** 表格合计列定义 */
const tableSummaryColumns = computed(() => [
  { key: 'quantity', value: totalQuantity.value, highlight: true },
  { key: 'pieceQuantity', value: totalPieceQty.value, highlight: true },
  { key: 'bigPack', value: totalBigPack.value, highlight: true },
  { key: 'midPack', value: totalMidPack.value, highlight: true },
  { key: 'smallPack', value: totalSmallPack.value, highlight: true },
  { key: 'amount', value: totalAmount.value, highlight: true },
  { key: 'discountedAmount', value: totalDiscountedAmount.value, highlight: true },
  { key: 'weight', value: totalWeight.value, highlight: false },
  { key: 'volume', value: totalVolume.value, highlight: false },
  { key: 'exchangePoints', value: formData.products.reduce((s: number, p: any) => s + (p.exchangePoints || 0), 0), highlight: false },
])

// ═══════════════════════════════════════
// BillFormPage 配置
// ═══════════════════════════════════════

const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '销售退货申请',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [
    // 工作流按钮（根据状态动态显示，对标金蝶/用友标准）
    ...getWorkflowActions(),
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'import', label: '导入', icon: ImportOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
    { key: 'export', label: '导出', icon: ExportOutlined },
  ],
}))

/** 根据单据状态返回可用工作流操作（草稿→提交→审核→完成/取消） */
function getWorkflowActions(): { key: string; label: string }[] {
  const status = formData.status
  const actions: { key: string; label: string }[] = []
  if (status === undefined || status === 0) {
    actions.push({ key: 'submit', label: '提交审核' })
  }
  if (status === 1) {
    actions.push({ key: 'approve', label: '审核通过' })
    actions.push({ key: 'reject', label: '驳回' })
  }
  if (status === 2) {
    actions.push({ key: 'complete', label: '完成' })
  }
  if (status !== undefined && status >= 0 && status < 3) {
    actions.push({ key: 'cancelOrder', label: '取消' })
  }
  return actions
}

// 完整的 basicInfoFields（50个字段）
const allBasicInfoFields = computed<BasicInfoField[]>(() => [
  { key: 'orderNo', label: '编号', type: 'display', inlineLabel: true, width: 210, value: formData.orderNo },
  { key: 'customerId', label: '客户', type: 'select', required: true, inlineLabel: true, width: 435, options: optionRefs.customers.map((c: any) => ({ label: c.name, value: c.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'customerCode', label: '客户编号', type: 'display', inlineLabel: true, width: 150, value: formData.customerCode },
  { key: 'customerLevel', label: '客户级别', type: 'display', inlineLabel: true, width: 100, value: formData.customerLevel },
  { key: 'bankName', label: '开户行', type: 'display', inlineLabel: true, width: 210, value: formData.bankName },
  { key: 'bankAccount', label: '银行账号', type: 'display', inlineLabel: true, width: 210, value: formData.bankAccount },
  { key: 'taxNo', label: '税号', type: 'display', inlineLabel: true, width: 210, value: formData.taxNo },
  { key: 'warehouseId', label: '入库仓库', type: 'select', required: true, inlineLabel: true, width: 210, options: optionRefs.warehouses.map((w: any) => ({ label: w.name, value: w.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 210, options: optionRefs.users.map((u: any) => ({ label: u.name, value: u.id })), searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'deptId', label: '部门', type: 'select', inlineLabel: true, width: 210, options: [], searchBtn: '+Q', loading: loadingOptions.value },
  { key: 'orderDate', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 210 },
  { key: 'returnApplyType', label: '销售类型', type: 'select', required: true, inlineLabel: true, width: 210, options: [{ label: '正常销售', value: 0 }, { label: '换货', value: 1 }, { label: '调拨', value: 2 }, { label: '其他', value: 3 }] },
  { key: 'contactName', label: '联系人', type: 'input', inlineLabel: true, width: 210, searchBtn: 'Q' },
  { key: 'contactPhone', label: '联系电话', type: 'input', inlineLabel: true, width: 435 },
  { key: 'contactAddress', label: '联系地址', type: 'input', inlineLabel: true, width: 435 },
  { key: 'expectedReceiveDate', label: '预计收货', type: 'date', inlineLabel: true, width: 210 },
  { key: 'extNum1', label: '自定义字段1(数字)', type: 'number', inlineLabel: true, width: 210 },
  { key: 'extNum2', label: '自定义字段2(数字)', type: 'number', inlineLabel: true, width: 210 },
  { key: 'extText1', label: '自定义字段3(文本)', type: 'input', inlineLabel: true, width: 210 },
  { key: 'extText2', label: '自定义字段4(文本)', type: 'input', inlineLabel: true, width: 210 },
  { key: 'extText3', label: '自定义字段5(文本)', type: 'input', inlineLabel: true, width: 210 },
  { key: 'auditor', label: '审核人', type: 'input', inlineLabel: true, width: 210, searchBtn: 'Q' },
  { key: 'summary', label: '摘要', type: 'input', inlineLabel: true, width: 435 },
  { key: 'creditLimit', label: '信用额度', type: 'number', inlineLabel: true, width: 210, disabled: true },
  { key: 'availableCredit', label: '可用额度', type: 'number', inlineLabel: true, width: 210, disabled: true },
  { key: 'currentDebt', label: '本次欠款', type: 'number', inlineLabel: true, width: 210, disabled: true },
  { key: 'prevDebt', label: '此前欠款', type: 'number', inlineLabel: true, width: 210, disabled: true },
  { key: 'debtBalance', label: '欠款余额', type: 'number', inlineLabel: true, width: 210, disabled: true },
  { key: 'collectionDeadline', label: '收款期限', type: 'date', inlineLabel: true, width: 210 },
  { key: 'sourceOrder', label: '源单', type: 'input', inlineLabel: true, width: 210, placeholder: '请输入源单编号', searchBtn: '查看' },
  { key: 'deliveryNo', label: '配送单', type: 'input', inlineLabel: true, width: 210 },
  { key: 'deliveryMethod', label: '配送方式', type: 'select', inlineLabel: true, width: 210, options: [{ label: '自提', value: '自提' }, { label: '配送', value: '配送' }, { label: '快递', value: '快递' }] },
  { key: 'deliveryRoute', label: '配送线路', type: 'input', inlineLabel: true, width: 210 },
  { key: 'logisticsCompany', label: '物流公司', type: 'input', inlineLabel: true, width: 210 },
  { key: 'shippingFee', label: '运费', type: 'number', inlineLabel: true, width: 210 },
  { key: 'waybillNo', label: '运单号', type: 'input', inlineLabel: true, width: 210 },
  { key: 'memberCardNo', label: '会员卡号', type: 'input', inlineLabel: true, width: 210 },
  { key: 'prevPoints', label: '此前积分', type: 'number', inlineLabel: true, width: 210, disabled: true },
  { key: 'memberGeneratedPoints', label: '产生积分', type: 'number', inlineLabel: true, width: 210 },
  { key: 'memberExchangePoints', label: '兑换积分', type: 'number', inlineLabel: true, width: 210 },
  { key: 'memberUsedPoints', label: '使用积分', type: 'number', inlineLabel: true, width: 210 },
  { key: 'currentPoints', label: '剩余积分', type: 'number', inlineLabel: true, width: 210, disabled: true },
  { key: 'remark', label: '单据备注', type: 'input', inlineLabel: true, width: 435 },
  { key: 'buyerRemark', label: '买家备注', type: 'input', inlineLabel: true, width: 435 },
  { key: 'footerExtText1', label: '表尾自定义字段1', type: 'input', inlineLabel: true, width: 210 },
  { key: 'footerExtText2', label: '表尾自定义字段2', type: 'input', inlineLabel: true, width: 210 },
  { key: 'creatorName', label: '制单人', type: 'display', inlineLabel: true, width: 100, value: currentUserName.value },
  { key: 'createTime', label: '制单时间', type: 'display', inlineLabel: true, width: 200, value: formatNow() },
  { key: 'printCount', label: '打印次数', type: 'display', inlineLabel: true, width: 100, value: formData.printCount || 0 },
  { key: 'totalAmount', label: '本单金额', type: 'display', inlineLabel: true, width: 150, value: `\u00a5${totalAmount.value.toFixed(2)}` },
])

// 表单字段显隐配置（localStorage key 与 SaleReturnApplyFormConfig 保持一致）
const FORM_CONFIG_KEY = 'sale-return-apply-form-page-config'
const FORM_DEFAULTS_KEY = 'sale-return-apply-form-default-config'
const FORM_PRINT_KEY = 'sale-return-apply-form-print-config'
const formFieldVisibility = ref<Array<{ key: string; visible: boolean }>>([])

// 默认字段显隐映射（对标参考截图：仅显示第一二行核心字段，其余隐藏）
const DEFAULT_FIELD_VISIBLE_MAP: Record<string, boolean> = {
  // Row 1: 客户*, 入库仓库*, 经手人*, 单据日期*, 销售类型*, 联系人
  customerId: true, warehouseId: true, handlerId: true,
  orderDate: true, returnApplyType: true, contactName: true,
  // Row 2: 联系电话, 联系地址, 预计收货, 审核人
  contactPhone: true, contactAddress: true,
  expectedReceiveDate: true, auditor: true,
  // 所有其他字段默认隐藏（编号在header、备注在bottom-extra、信用/物流/会员在tabs、制单信息在footer）
}

// 所有基本信息字段键列表（用于预填充 formFieldVisibility）
const ALL_BASIC_FIELD_KEYS = [
  'orderNo', 'customerId', 'customerCode', 'customerLevel', 'bankName', 'bankAccount', 'taxNo',
  'warehouseId', 'handlerId', 'deptId', 'orderDate', 'returnApplyType',
  'contactName', 'contactPhone', 'contactAddress', 'expectedReceiveDate',
  'extNum1', 'extNum2', 'extText1', 'extText2', 'extText3',
  'auditor', 'summary',
  'creditLimit', 'availableCredit', 'currentDebt', 'prevDebt', 'debtBalance',
  'collectionDeadline', 'sourceOrder', 'deliveryNo',
  'deliveryMethod', 'deliveryRoute', 'logisticsCompany', 'shippingFee', 'waybillNo',
  'memberCardNo', 'prevPoints', 'memberGeneratedPoints', 'memberExchangePoints', 'memberUsedPoints', 'currentPoints',
  'remark', 'buyerRemark', 'footerExtText1', 'footerExtText2',
  'creatorName', 'createTime', 'printCount', 'totalAmount',
]

// 已在 bottom-extra 模板中单独渲染的字段，不在基本信息区重复显示
const BOTTOM_EXTRA_RENDERED_KEYS = new Set(['remark', 'buyerRemark'])

// 从localStorage加载表单字段显隐配置（与 SaleReturnApplyFormConfig 的 pageConfig 格式一致）
function loadFormConfig() {
  try {
    const raw = localStorage.getItem(FORM_CONFIG_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      let savedFields: Array<{ key: string; visible: boolean }> = []
      if (Array.isArray(parsed)) {
        savedFields = parsed
      } else if (parsed.pageFields) {
        savedFields = parsed.pageFields
      }
      if (savedFields.length) {
        // Merge saved visibility over defaults
        const savedMap = new Map(savedFields.map((f: any) => [f.key, f.visible !== false]))
        formFieldVisibility.value = ALL_BASIC_FIELD_KEYS.map(key => ({
          key,
          visible: savedMap.has(key) ? savedMap.get(key)! : (DEFAULT_FIELD_VISIBLE_MAP[key] ?? false),
        }))
        return
      }
    }
  } catch { /* ignore */ }
  // No saved config → pre-populate all fields with defaults
  formFieldVisibility.value = ALL_BASIC_FIELD_KEYS.map(key => ({
    key,
    visible: DEFAULT_FIELD_VISIBLE_MAP[key] ?? false,
  }))
}

// 模块级别同步加载（与 outbound/form.vue 一致，确保首屏渲染时配置已就绪）
loadFormConfig()

// 根据页面配置显示/隐藏字段（formFieldVisibility 始终预填充了所有字段的默认值）
const visibleBasicInfoFields = computed(() => {
  return allBasicInfoFields.value.filter(field => {
    // 已在 bottom-extra 单独渲染的字段，不在基本信息区显示
    if (BOTTOM_EXTRA_RENDERED_KEYS.has(field.key)) return false
    const config = formFieldVisibility.value.find(f => f.key === field.key)
    return config ? config.visible : false
  })
})

// 应用录单默认值（仅新建时）
function applyDefaultValues(defaults: Array<{ key: string; value: any }>) {
  defaults.forEach(d => {
    if (d.value !== undefined && d.value !== null && d.value !== '') {
      ;(formData as any)[d.key] = d.value
    }
  })
}

function loadAndApplyDefaults() {
  try {
    const raw = localStorage.getItem(FORM_DEFAULTS_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      // SaleReturnApplyFormConfig 存储格式为 { fields: [...] }
      const defaults = parsed.fields || parsed
      if (Array.isArray(defaults)) {
        applyDefaultValues(defaults)
      }
    }
  } catch { /* ignore */ }
}

const tabsConfig = computed<BillTabConfig[]>(() => [
  { key: 'payment', tab: '收款', fields: [
    { key: 'creditLimit', label: '信用额度', type: 'number', disabled: true },
    { key: 'availableCredit', label: '可用额度', type: 'number', disabled: true },
    { key: 'currentDebt', label: '本次欠款', type: 'number', disabled: true },
    { key: 'prevDebt', label: '此前欠款', type: 'number', disabled: true },
    { key: 'debtBalance', label: '欠款余额', type: 'number', disabled: true },
    { key: 'collectionDeadline', label: '收款期限', type: 'date' },
    { key: 'settlementMethod', label: '结算方式', type: 'select', options: [{ label: '现金', value: '现金' }, { label: '转账', value: '转账' }, { label: '月结', value: '月结' }, { label: '支票', value: '支票' }] },
    { key: 'settledAmount', label: '已结金额', type: 'number', disabled: true },
  ]},
  { key: 'logistics', tab: '物流信息', fields: [
    { key: 'deliveryMethod', label: '配送方式', type: 'select', options: [{ label: '自提', value: '自提' }, { label: '配送', value: '配送' }, { label: '快递', value: '快递' }] },
    { key: 'deliveryRoute', label: '配送线路', type: 'input' },
    { key: 'freightPayer', label: '运费承担方', type: 'select', options: [{ label: '买方', value: '买方' }, { label: '卖方', value: '卖方' }] },
    { key: 'logisticsCompany', label: '物流公司', type: 'input' },
    { key: 'shippingFee', label: '运费', type: 'number' },
    { key: 'waybillNo', label: '运单号', type: 'input' },
  ]},
  { key: 'member', tab: '会员信息', fields: [
    { key: 'memberCardNo', label: '会员卡号', type: 'input' },
    { key: 'prevPoints', label: '此前积分', type: 'number', disabled: true },
    { key: 'memberGeneratedPoints', label: '产生积分', type: 'number' },
    { key: 'memberExchangePoints', label: '兑换积分', type: 'number' },
    { key: 'memberUsedPoints', label: '使用积分', type: 'number' },
    { key: 'currentPoints', label: '剩余积分', type: 'number', disabled: true },
  ]},
])

const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '退货数量', value: totalQuantity.value },
  { label: '商品金额', value: totalAmount.value.toFixed(2) },
  { label: '折后金额', value: totalDiscountedAmount.value.toFixed(2) },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: `\u00a5${totalAmount.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: '保存草稿',
  draftShortcut: 'Ctrl+S',
  primaryBtnText: '提交',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══════════════════════════════════════
// 明细表格列配置 (61列)
// ═══════════════════════════════════════

const allDetailColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 50, fixed: 'left' },
  { key: 'imageUrl', title: '图片', type: 'image', width: 60 },
  { key: 'productId', title: '商品名称', type: 'input', searchable: true,
    options: optionRefs.products.map((p: any) => ({
      label: p.name,
      value: p.id,
      searchText: `${p.name} ${p.code || ''} ${p.barcode || ''}`,
    })),
    width: 200, showScanToggle: true,
  },
  { key: 'itemCode', title: '货号', type: 'input', width: 100 },
  { key: 'barcode', title: '条码', type: 'input', width: 120 },
  { key: 'area', title: '区域', type: 'input', width: 80 },
  { key: 'specification', title: '规格', type: 'input', width: 100 },
  { key: 'modelNo', title: '型号', type: 'input', width: 80 },
  { key: 'originPlace', title: '产地', type: 'input', width: 80 },
  { key: 'brand', title: '品牌', type: 'input', width: 80 },
  { key: 'unit', title: '计价单位', type: 'input', width: 80 },
  { key: 'availableStock', title: '可用库存', type: 'number', width: 90, precision: 2 },
  { key: 'availableStockConverted', title: '可用库存换算结果', type: 'number', width: 100, precision: 2 },
  { key: 'bookStock', title: '账面库存', type: 'number', width: 90, precision: 2 },
  { key: 'quantity', title: '数量', type: 'number', width: 80, precision: 2 },
  { key: 'conversionRelation', title: '换算关系', type: 'input', width: 100 },
  { key: 'pieceQuantity', title: '件散数量', type: 'number', width: 80, precision: 2 },
  { key: 'bigPack', title: '大包装', type: 'number', width: 70, precision: 0 },
  { key: 'midPack', title: '中包装', type: 'number', width: 70, precision: 0 },
  { key: 'smallPack', title: '小包装', type: 'number', width: 70, precision: 0 },
  { key: 'lastSaleDate', title: '最近销售日期', type: 'date', width: 110 },
  { key: 'lastSalePrice', title: '最近售价', type: 'number', width: 80, precision: 2 },
  { key: 'retailPrice', title: '零售价', type: 'number', width: 80, precision: 2 },
  { key: 'wholesalePrice', title: '批发价', type: 'number', width: 80, precision: 2 },
  { key: 'minSalePrice', title: '最低售价', type: 'number', width: 80, precision: 2 },
  { key: 'unitPrice', title: '单价', type: 'number', width: 80, precision: 2 },
  { key: 'lineAmount', title: '金额', type: 'number', width: 80, precision: 2, align: 'right' },
  { key: 'smallUnit', title: '小单位', type: 'input', width: 70 },
  { key: 'smallUnitPrice', title: '小单位单价', type: 'number', width: 90, precision: 2 },
  { key: 'smallUnitQuantity', title: '小单位数量', type: 'number', width: 90, precision: 2 },
  { key: 'receivedQuantity', title: '已收数量', type: 'number', width: 80, precision: 2 },
  { key: 'refCostPrice', title: '参考成本单价', type: 'number', width: 100, precision: 2 },
  { key: 'refCostAmount', title: '参考成本金额', type: 'number', width: 100, precision: 2 },
  { key: 'discountRate', title: '折扣(%)', type: 'number', width: 70, precision: 2 },
  { key: 'discountedPrice', title: '折后单价', type: 'number', width: 80, precision: 2 },
  { key: 'discountedAmount', title: '折后金额', type: 'number', width: 80, precision: 2 },
  { key: 'exchangeGift', title: '兑换礼品', type: 'input', width: 100 },
  { key: 'exchangePoints', title: '兑换积分', type: 'number', width: 80, precision: 0 },
  { key: 'memberGeneratedPoints', title: '产生积分', type: 'number', width: 80, precision: 0 },
  { key: 'memberUsedPoints', title: '使用积分', type: 'number', width: 80, precision: 0 },
  { key: 'productLineAttr', title: '商品行属性', type: 'select', width: 100, options: [{ label: '正常', value: '正常' }, { label: '赠品', value: '赠品' }] },
  { key: 'isGift', title: '赠品', type: 'checkbox', width: 60 },
  { key: 'itemRemark', title: '备注', type: 'input', width: 120 },
  // 8个标准化产品价格等级（对标开发文档：用户自定义昵称→标准化价格等级）
  { key: 'priceLevel1', title: '餐饮店', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel2', title: '食堂团餐', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel3', title: '自助vip', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel4', title: '大团餐', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel5', title: '特     价', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel6', title: '外围餐饮店', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel7', title: '重点vip01', type: 'number', width: 80, precision: 2 },
  { key: 'priceLevel8', title: '连锁|vip', type: 'number', width: 80, precision: 2 },
  // 单据自定义字段
  { key: 'extNum1', title: '单据自定义1(数字)', type: 'number', width: 110, precision: 2 },
  { key: 'extNum2', title: '单据自定义2(数字)', type: 'number', width: 110, precision: 2 },
  { key: 'extNum3', title: '单据自定义3(数字)', type: 'number', width: 110, precision: 2 },
  { key: 'extText1', title: '单据自定义4(文本)', type: 'input', width: 110 },
  { key: 'extText2', title: '单据自定义5(文本)', type: 'input', width: 110 },
  { key: 'extNum4', title: '单据自定义6(数字)', type: 'number', width: 110, precision: 2 },
  { key: 'extNum5', title: '单据自定义7(数字)', type: 'number', width: 110, precision: 2 },
  { key: 'extPartner', title: '单据自定义8(往来单位)', type: 'select', width: 120, options: [] },
  { key: 'extStaff', title: '单据自定义9(职员)', type: 'select', width: 100, options: [] },
  { key: 'extDept', title: '单据自定义10(部门)', type: 'select', width: 100, options: [] },
])

// 列配置
const detailColumnDefs = computed(() => allDetailColumns.value.map(col => ({ ...col })))
const {
  visibleColumns: visibleDetailColumns,
  showPanel: detailShowPanel,
  onSettingChange: onDetailSettingChange,
  resetSettings: resetDetailSettings,
  settingsColumns: detailSettingsColumns,
} = useColumnConfig(detailColumnDefs.value, 'sale-return-apply-form-detail-columns')

// 从列配置提取回车跳转列和公式配置
const enterJumpColumns = computed(() =>
  detailSettingsColumns.value.filter(s => s.enterJump).map(s => s.key)
)
const detailFormulas = computed(() => {
  const result: Record<string, string> = {}
  detailSettingsColumns.value.forEach(s => {
    if (s.formula) result[s.key] = s.formula
  })
  return result
})

function handleDetailColumnConfigChange() {
  onDetailSettingChange()
}
function handleDetailColumnConfigReset() {
  resetDetailSettings()
}

// ═══════════════════════════════════════
// 事件处理
// ═══════════════════════════════════════

/**
 * 保存前清理空行（对标金蝶/管家婆：不提交空行数据）
 * 如果某行 productId 为空且 quantity 为0，视为无效行
 */
function cleanEmptyRows() {
  const before = formData.products.length
  formData.products = formData.products.filter((p: any) => {
    return p.productId != null && p.productId !== ''
  })
  // 确保至少保留一行空行供继续录入
  if (formData.products.length === 0) {
    for (let i = 0; i < 5; i++) handleAddProduct()
  }
  return before !== (formData.products.length + 5) // 如果新增了空行算无变化
}

/** 保存草稿包装：清理空行后调用原始保存 */
function handleFormDraft() {
  cleanEmptyRows()
  handleSaveDraft()
}

/** 提交包装：校验 + 清理空行 + 信用额度校验后提交 */
function handleFormSubmit() {
  // 清理空行
  cleanEmptyRows()
  // 信用额度校验（对标SAP/金蝶：超出信用额度时警告）
  const editId = route.params.id ? Number(route.params.id) : undefined
  const totalAmt = totalAmount.value
  const availCredit = formData.availableCredit || 0
  if (availCredit > 0 && totalAmt > availCredit) {
    Modal.confirm({
      title: '信用额度预警',
      content: `本单金额 ¥${totalAmt.toFixed(2)} 超出可用信用额度 ¥${availCredit.toFixed(2)}，确定继续提交吗？`,
      okText: '继续提交',
      cancelText: '取消',
      onOk: () => handleSubmit(),
    })
    return
  }
  handleSubmit()
}

/** 插入商品行 */
function handleInsertProduct(index: number) {
  const newProduct = {
    id: Date.now().toString() + Math.random().toString(36).slice(2, 6),
    productId: undefined,
    itemCode: '', barcode: '', specification: '',
    unit: '', batchCode: '',
    conversionRelation: '', pieceQuantity: 0,
    bigPack: 0, midPack: 0, smallPack: 0,
    unitPrice: 0, taxRate: 13, scanMode: false,
    exchangeGift: '', exchangePoints: 0,
    productName: '', quantity: 0, availableStock: 0, remark: '',
    itemRemark: '', productLineAttr: '正常', isGift: false,
    discountRate: 0, discountedPrice: 0, discountedAmount: 0,
    priceLevel1: 0, priceLevel2: 0, priceLevel3: 0, priceLevel4: 0,
    priceLevel5: 0, priceLevel6: 0, priceLevel7: 0, priceLevel8: 0,
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
      record.productCode = p.code || ''
      record.productName = p.name || ''
      record.itemCode = p.code || ''
      record.barcode = p.barcode || ''
      record.specification = p.specification || ''
      record.unit = p.unit || ''
      record.unitPrice = p.salePrice || p.price || 0
      record.availableStock = p.stock || 0
      record.bookStock = p.bookStock || p.stock || 0
      record.taxRate = 13
      record.brand = p.brand || ''
      record.area = p.area || ''
      record.originPlace = p.origin || ''
      record.modelNo = p.model || ''
      // 最近销售信息（对标管家婆/金蝶标准）
      record.lastSaleDate = p.lastSaleDate || ''
      record.lastSalePrice = p.lastSalePrice || 0
      // 参考成本
      record.refCostPrice = p.costPrice || p.avgCost || 0
      record.refCostAmount = record.refCostPrice * (record.quantity || 0)
      // 价格等级
      record.priceLevel1 = p.priceLevel1 || 0
      record.priceLevel2 = p.priceLevel2 || 0
      record.priceLevel3 = p.priceLevel3 || 0
      record.priceLevel4 = p.priceLevel4 || 0
      record.priceLevel5 = p.priceLevel5 || 0
      record.priceLevel6 = p.priceLevel6 || 0
      record.priceLevel7 = p.priceLevel7 || 0
      record.priceLevel8 = p.priceLevel8 || 0
    }
  }
  // 金额计算：单价 × 数量 = 金额
  if (fieldKey === 'quantity' || fieldKey === 'unitPrice') {
    record.lineAmount = (record.quantity || 0) * (record.unitPrice || 0)
    // 参考成本金额联动
    record.refCostAmount = (record.refCostPrice || 0) * (record.quantity || 0)
    // 小单位数量换算（件散数量 = 数量 × 换算关系）
    if (record.conversionRelation) {
      const conv = parseFloat(record.conversionRelation) || 1
      record.pieceQuantity = (record.quantity || 0) * conv
      record.smallUnitQuantity = record.pieceQuantity
    }
    // 未收数量 = 退货数量 - 已收数量（对标金蝶/管家婆：自动计算未收）
    record.receivedQuantity = record.receivedQuantity || 0
    record.unreceivedQuantity = Math.max(0, (record.quantity || 0) - (record.receivedQuantity || 0))
    // 小单位单价联动（单价 ÷ 换算关系 = 小单位单价）
    if (record.conversionRelation) {
      const conv = parseFloat(record.conversionRelation) || 1
      record.smallUnitPrice = conv > 0 ? (record.unitPrice || 0) / conv : 0
    }
    // 折扣联动：数量/单价变更时，如果有折扣率则重算折后金额
    const rate = record.discountRate || 0
    if (rate > 0) {
      record.discountedAmount = record.lineAmount * (rate / 100)
      record.discountedPrice = record.quantity > 0 ? record.discountedAmount / record.quantity : 0
    }
    // 库存校验（对标管家婆/金蝶：数量超出可用库存时警告）
    if (fieldKey === 'quantity' && record.quantity > 0 && record.availableStock > 0 && record.quantity > record.availableStock) {
      message.warn(`商品 "${record.productName || record.itemCode}" 退货数量 ${record.quantity} 超出可用库存 ${record.availableStock}，请确认`)
    }
  }
  // 换算关系变更时重算件散数量
  if (fieldKey === 'conversionRelation') {
    const conv = parseFloat(value) || 1
    record.pieceQuantity = (record.quantity || 0) * conv
    record.smallUnitQuantity = record.pieceQuantity
  }
  // 折扣计算（对标SAP/金蝶标准：折扣率→折后金额→折后单价联动）
  if (fieldKey === 'discountRate' || fieldKey === 'unitPrice' || fieldKey === 'quantity') {
    const rate = record.discountRate || 0
    const amount = record.lineAmount || 0
    record.discountedAmount = amount * (rate / 100)
    record.discountedPrice = record.quantity > 0 ? record.discountedAmount / record.quantity : 0
  }
  // 已收数量变更时重算未收数量
  if (fieldKey === 'receivedQuantity') {
    record.unreceivedQuantity = Math.max(0, (record.quantity || 0) - (record.receivedQuantity || 0))
  }
  // 折后单价变更时反算折扣率（用户手动输入折后单价）
  if (fieldKey === 'discountedPrice') {
    if (record.unitPrice > 0) {
      record.discountRate = ((record.unitPrice - (record.discountedPrice || 0)) / record.unitPrice) * 100
      record.discountedAmount = (record.discountedPrice || 0) * (record.quantity || 0)
    }
  }
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  if (fieldKey === 'sourceOrder') {
    // 源单关联跳转
    const generateType = formData.generateType
    const sourceOrderId = formData.sourceOrderId
    if (!sourceOrderId) {
      message.info('未关联源单')
      return
    }
    // 根据产生方式跳转到对应的源单页面
    if (generateType === '销售订单') {
      router.push(`/sales/order/form/${sourceOrderId}`)
    } else if (generateType === '销售出库') {
      router.push(`/sales/outbound/form/${sourceOrderId}`)
    } else {
      message.info(`源单类型：${generateType || '未知'}，暂不支持跳转`)
    }
    return
  }
  message.info(`${fieldKey} 快速查询功能开发中`)
}

// ═══ 产品选择弹窗 ══
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
      row.itemCode = p.code || ''
      row.barcode = p.barcode || ''
      row.specification = p.specification || ''
      row.unit = p.unit || ''
      row.unitPrice = p.retailPrice || p.salePrice || p.price || 0
      row.availableStock = p.stock || 0
      row.taxRate = 13
      row.brand = p.brand || ''
    }
  })

  showProductSelect.value = false
  message.success(`已选择 ${products.length} 个商品`)
}

async function handleAction(actionKey: string, _parentKey?: string) {
  const editId = route.params.id ? Number(route.params.id) : undefined
  switch (actionKey) {
    case 'history':
      router.push('/sales/return-apply')
      break
    case 'print':
      // 读取打印配置
      try {
        const printConfig = JSON.parse(localStorage.getItem(FORM_PRINT_KEY) || '{}')
        if (printConfig.alwaysLastTemplate) {
          message.info('使用上次打印模板进行打印')
        } else {
          message.info('请选择打印模板')
        }
      } catch {
        message.info('打印功能开发中')
      }
      break
    case 'config':
      showFormConfig.value = true
      break
    case 'import':
    case 'export':
      message.info(`${actionKey === 'import' ? '导入' : '导出'} 功能开发中`)
      break
    // ── 工作流操作 ──
    case 'submit':
      if (!editId) { message.warning('请先保存单据'); return }
      Modal.confirm({
        title: '确认提交', content: '提交后将进入审核流程，确定提交吗？',
        async onOk() {
          await saleReturnApi.submit(editId)
          message.success('已提交审核')
          formData.status = 1
        },
      })
      break
    case 'approve':
      if (!editId) return
      try {
        await saleReturnApi.approve(editId)
        message.success('审核通过')
        formData.status = 2
        formData.auditor = currentUserName.value
        formData.auditTime = new Date().toISOString().slice(0, 19)
      } catch { message.error('审核失败') }
      break
    case 'reject':
      if (!editId) return
      Modal.confirm({
        title: '驳回单据', content: '确定驳回此单据吗？',
        async onOk() {
          await saleReturnApi.cancel(editId, '驳回')
          message.success('已驳回')
          formData.status = 0
        },
      })
      break
    case 'complete':
      if (!editId) return
      try {
        await saleReturnApi.complete(editId)
        message.success('单据已完成')
        formData.status = 3
      } catch { message.error('操作失败') }
      break
    case 'cancelOrder':
      if (!editId) return
      Modal.confirm({
        title: '取消单据', content: '确定取消此单据吗？取消后不可恢复。',
        okType: 'danger',
        async onOk() {
          await saleReturnApi.cancel(editId)
          message.success('已取消')
          router.push('/sales/return-apply')
        },
      })
      break
  }
}

function handleFormConfigChange(config: any) {
  // 深度合并：仅更新用户实际在配置面板中修改的字段
  // 保留 DEFAULT_FIELD_VISIBLE_MAP 未显式配置的默认值
  if (config.pageFields) {
    const incomingMap = new Map(config.pageFields.map((f: any) => [f.key, f.visible]))
    const currentMap = new Map(formFieldVisibility.value.map(f => [f.key, f.visible]))
    // 合并：传入的配置覆盖现有值，但仅覆盖传入的字段
    incomingMap.forEach((visible, key) => currentMap.set(key, visible))
    formFieldVisibility.value = Array.from(currentMap.entries()).map(([key, visible]) => ({ key, visible }))
  }
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// ── 生命周期 ──
onMounted(() => {
  // 初始化空行
  if (formData.products.length === 0) {
    for (let i = 0; i < 15; i++) handleAddProduct()
  }
  // 新建模式时应用录单默认值
  if (!route.params.id) {
    loadAndApplyDefaults()
  }
  nextTick(() => {
    tableMaxHeight.value = Math.max(200, window.innerHeight - 420)
  })
})
</script>

<style scoped>
.detail-table-header {
  display: flex;
  justify-content: flex-end;
  padding: 4px 0;
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
}

.doc-info-link {
  color: #1890ff;
  font-size: 12px;
}
</style>
