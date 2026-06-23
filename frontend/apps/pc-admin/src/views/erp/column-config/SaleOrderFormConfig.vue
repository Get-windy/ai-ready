<template>
  <a-modal
    v-model:open="visible"
    title="配置"
    :width="800"
    :footer="null"
    :closable="true"
    :maskClosable="false"
    wrapClassName="sale-order-form-config-wrap"
    @cancel="handleClose"
  >
    <a-tabs v-model:activeKey="activeTab" class="config-tabs">
      <!-- ═══════════════════════════════════════
           Tab 1: 页面配置
           ═══════════════════════════════════════ -->
      <a-tab-pane key="page" tab="页面配置">
        <div class="tab-tip">勾选后自动保存，该设置对所有操作员生效</div>
        <div class="page-config-table-wrap">
          <table class="page-config-table">
            <thead>
              <tr>
                <th style="width: 50px;"></th>
                <th style="width: 160px;">名称</th>
                <th>显示名</th>
                <th style="width: 80px;">显示</th>
                <th style="width: 100px;">回车键跳转</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(field, index) in pageFields"
                :key="field.key"
                :class="{ 'row-highlight': field.visible }"
              >
                <td class="row-index">{{ index + 1 }}</td>
                <td class="field-name">{{ field.label }}</td>
                <td>
                  <a-input
                    v-model:value="field.displayName"
                    size="small"
                    @change="handlePageFieldChange"
                  />
                </td>
                <td class="cell-center">
                  <a-checkbox
                    v-model:checked="field.visible"
                    @change="handlePageFieldChange"
                  />
                </td>
                <td class="cell-center">
                  <a-checkbox
                    v-model:checked="field.enterJump"
                    @change="handlePageFieldChange"
                  />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="tab-footer">
          <a-button @click="handleResetPageConfig">恢复默认值</a-button>
          <a-button type="primary" danger @click="handleClose" style="margin-left: 8px;">关闭</a-button>
        </div>
      </a-tab-pane>

      <!-- ══════════════════════════════════════
           Tab 2: 录单默认值
           ═══════════════════════════════════════ -->
      <a-tab-pane key="default" tab="录单默认值">
        <div class="default-value-section">
          <h3 class="section-title">字段默认值设置</h3>
          <div class="default-field-list">
            <div
              v-for="df in defaultFields"
              :key="df.key"
              class="default-field-row"
            >
              <a-input
                :value="getDefaultFieldDisplay(df)"
                size="small"
                class="default-field-input"
                readonly
              />
              <a-button
                type="link"
                size="small"
                class="default-field-add-btn"
                @click="handleAddDefault(df.key)"
              >
                +
              </a-button>
              <a-button
                type="link"
                size="small"
                class="default-field-search-btn"
                @click="handleSearchDefault(df.key)"
              >
                Q
              </a-button>
            </div>
          </div>
          <div class="default-priority-row">
            <a-checkbox v-model:checked="defaultPriority" @change="handleDefaultChange">
              录单默认值优先
            </a-checkbox>
          </div>
        </div>
        <div class="tab-footer tab-footer-right">
          <a-button @click="handleConfigDefault">配置</a-button>
        </div>
        <div class="tab-footer-bottom">
          <a-button type="primary" danger @click="handleSaveDefault" style="margin-right: 8px;">保存</a-button>
          <a-button @click="handleClose">取消</a-button>
        </div>
      </a-tab-pane>

      <!-- ═══════════════════════════════════════
           Tab 3: 打印设置
           ═══════════════════════════════════════ -->
      <a-tab-pane key="print" tab="打印设置">
        <div class="tab-tip">打印配置设置后只针对当前操作员有效</div>
        <div class="print-settings">
          <a-checkbox v-model:checked="printSettings.alwaysLastTemplate" @change="handlePrintChange">
            始终使用最后一次打印的模板，打印时不再选择
          </a-checkbox>
          <a-checkbox v-model:checked="printSettings.printAfterSubmit" @change="handlePrintChange">
            提单后立即打印
          </a-checkbox>
        </div>
        <div class="tab-footer-bottom">
          <a-button type="primary" danger @click="handleSavePrint" style="margin-right: 8px;">保存</a-button>
          <a-button @click="handleClose">取消</a-button>
        </div>
      </a-tab-pane>
    </a-tabs>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'

// ── Props & Emits ──
const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  'change': [config: SaleOrderFormConfig]
}>()

// ── Types ──
interface PageField {
  key: string
  label: string
  displayName: string
  visible: boolean
  enterJump: boolean
}

interface DefaultValueField {
  key: string
  label: string
  value: string
}

interface PrintSettings {
  alwaysLastTemplate: boolean
  printAfterSubmit: boolean
}

export interface SaleOrderFormConfig {
  pageFields: PageField[]
  defaultFields: DefaultValueField[]
  defaultPriority: boolean
  printSettings: PrintSettings
}

// ─ Storage Keys ──
const STORAGE_KEY_PAGE = 'sale-order-form-page-config'
const STORAGE_KEY_DEFAULT = 'sale-order-form-default-config'
const STORAGE_KEY_PRINT = 'sale-order-form-print-config'

// ── 68 个页面字段默认数据 ──
const DEFAULT_PAGE_FIELDS: PageField[] = [
  { key: 'orderNo', label: '编号', displayName: '编号', visible: true, enterJump: false },
  { key: 'customerName', label: '客户', displayName: '客户', visible: true, enterJump: true },
  { key: 'customerCode', label: '客户编号', displayName: '客户编号', visible: true, enterJump: false },
  { key: 'bankName', label: '开户行', displayName: '开户行', visible: false, enterJump: false },
  { key: 'bankAccount', label: '银行账号', displayName: '银行账号', visible: false, enterJump: false },
  { key: 'taxNo', label: '税号', displayName: '税号', visible: false, enterJump: false },
  { key: 'customerRemark', label: '客户备注', displayName: '客户备注', visible: false, enterJump: false },
  { key: 'warehouseName', label: '发货仓库', displayName: '发货仓库', visible: true, enterJump: true },
  { key: 'salespersonName', label: '经手人', displayName: '经手人', visible: true, enterJump: true },
  { key: 'departmentName', label: '部门', displayName: '部门', visible: false, enterJump: false },
  { key: 'orderDate', label: '单据日期', displayName: '单据日期', visible: true, enterJump: true },
  { key: 'saleType', label: '销售类型', displayName: '销售类型', visible: true, enterJump: true },
  { key: 'receiverName', label: '收货人', displayName: '收货人', visible: false, enterJump: false },
  { key: 'receiverPhone', label: '联系电话', displayName: '联系电话', visible: false, enterJump: false },
  { key: 'receiverAddress', label: '收货地址', displayName: '收货地址', visible: false, enterJump: false },
  { key: 'customerLevel', label: '客户级别', displayName: '客户级别', visible: false, enterJump: false },
  { key: 'customField1Num', label: '自定义字段1(数字)', displayName: '自定义字段1(数字)', visible: false, enterJump: false },
  { key: 'customField2Num', label: '自定义字段2(数字)', displayName: '自定义字段2(数字)', visible: false, enterJump: false },
  { key: 'customField3Text', label: '自定义字段3(文本)', displayName: '自定义字段3(文本)', visible: false, enterJump: false },
  { key: 'customField4Text', label: '自定义字段4(文本)', displayName: '自定义字段4(文本)', visible: false, enterJump: false },
  { key: 'customField5Text', label: '自定义字段5(文本)', displayName: '自定义字段5(文本)', visible: false, enterJump: false },
  { key: 'auditorName', label: '审核人', displayName: '审核人', visible: false, enterJump: false },
  { key: 'summary', label: '摘要', displayName: '摘要', visible: false, enterJump: false },
  { key: 'depositAccount', label: '订金账户', displayName: '订金账户', visible: false, enterJump: false },
  { key: 'depositAmount', label: '订金金额', displayName: '订金金额', visible: false, enterJump: false },
  { key: 'moreAccounts', label: '更多账户', displayName: '更多账户', visible: false, enterJump: false },
  { key: 'useAdvancePayment', label: '使用预订货款', displayName: '使用预订货款', visible: false, enterJump: false },
  { key: 'prevAdvance', label: '此前预收', displayName: '此前预收', visible: false, enterJump: false },
  { key: 'advanceBalance', label: '预收余额', displayName: '预收余额', visible: false, enterJump: false },
  { key: 'creditLimit', label: '信用额度', displayName: '信用额度', visible: false, enterJump: false },
  { key: 'availableCredit', label: '可用额度', displayName: '可用额度', visible: false, enterJump: false },
  { key: 'prevDebt', label: '此前欠款', displayName: '此前欠款', visible: false, enterJump: false },
  { key: 'collectionDate', label: '收款日', displayName: '收款日', visible: false, enterJump: false },
  { key: 'reconciliationDate', label: '对账日', displayName: '对账日', visible: false, enterJump: false },
  { key: 'deliveryMethod', label: '配送方式', displayName: '配送方式', visible: true, enterJump: true },
  { key: 'deliveryRoute', label: '配送线路', displayName: '配送线路', visible: false, enterJump: false },
  { key: 'driverName', label: '司机', displayName: '司机', visible: false, enterJump: false },
  { key: 'logisticsCompany', label: '物流公司', displayName: '物流公司', visible: false, enterJump: false },
  { key: 'freightBearer', label: '运费承担方', displayName: '运费承担方', visible: false, enterJump: false },
  { key: 'shippingFee', label: '运费', displayName: '运费', visible: false, enterJump: false },
  { key: 'trackingNo', label: '运单号', displayName: '运单号', visible: false, enterJump: false },
  { key: 'codAmount', label: '代收货款', displayName: '代收货款', visible: false, enterJump: false },
  { key: 'estimatedShipDate', label: '预计发货', displayName: '预计发货', visible: false, enterJump: false },
  { key: 'contactPerson', label: '联系人', displayName: '联系人', visible: false, enterJump: false },
  { key: 'contactPhone', label: '联系电话', displayName: '联系电话', visible: false, enterJump: false },
  { key: 'pickupAddress', label: '提货地址', displayName: '提货地址', visible: false, enterJump: false },
  { key: 'memberCardNo', label: '会员卡号', displayName: '会员卡号', visible: false, enterJump: false },
  { key: 'prevPoints', label: '此前积分', displayName: '此前积分', visible: false, enterJump: false },
  { key: 'salePoints', label: '销售积分', displayName: '销售积分', visible: false, enterJump: false },
  { key: 'returnPoints', label: '退货积分', displayName: '退货积分', visible: false, enterJump: false },
  { key: 'exchangePoints', label: '兑换积分', displayName: '兑换积分', visible: false, enterJump: false },
  { key: 'usePoints', label: '使用积分', displayName: '使用积分', visible: false, enterJump: false },
  { key: 'currentPoints', label: '当前积分', displayName: '当前积分', visible: false, enterJump: false },
  { key: 'orderRemark', label: '单据备注', displayName: '单据备注', visible: true, enterJump: false },
  { key: 'buyerRemark', label: '买家备注', displayName: '买家备注', visible: false, enterJump: false },
  { key: 'footerCustom1', label: '表尾自定义字段1', displayName: '表尾自定义字段1', visible: false, enterJump: false },
  { key: 'footerCustom2', label: '表尾自定义字段2', displayName: '表尾自定义字段2', visible: false, enterJump: false },
  { key: 'creatorName', label: '制单人', displayName: '制单人', visible: true, enterJump: false },
  { key: 'createTime', label: '制单时间', displayName: '制单时间', visible: true, enterJump: false },
  { key: 'printCount', label: '打印次数', displayName: '打印次数', visible: true, enterJump: false },
  { key: 'sourceOrder', label: '源单', displayName: '源单', visible: true, enterJump: false },
  { key: 'saleQuantity', label: '销售数量', displayName: '销售数量', visible: true, enterJump: false },
  { key: 'returnQuantity', label: '退货数量', displayName: '退货数量', visible: false, enterJump: false },
  { key: 'productAmount', label: '商品金额', displayName: '商品金额', visible: true, enterJump: false },
  { key: 'promoDiscount', label: '促销优惠', displayName: '促销优惠', visible: false, enterJump: false },
  { key: 'discountAmount', label: '优惠金额', displayName: '优惠金额', visible: false, enterJump: false },
  { key: 'otherFee', label: '其他费用', displayName: '其他费用', visible: false, enterJump: false },
  { key: 'orderAmount', label: '本单金额', displayName: '本单金额', visible: true, enterJump: false },
]

// ── 录单默认值字段 ─
const DEFAULT_FIELDS: DefaultValueField[] = [
  { key: 'customerName', label: '客户', value: '' },
  { key: 'warehouseName', label: '发货仓库', value: '' },
  { key: 'salespersonName', label: '经手人', value: '' },
  { key: 'logisticsCompany', label: '物流公司', value: '' },
]

// ── State ──
const visible = ref(false)
const activeTab = ref('page')
const pageFields = ref<PageField[]>([])
const defaultFields = ref<DefaultValueField[]>([])
const defaultPriority = ref(false)
const printSettings = reactive<PrintSettings>({
  alwaysLastTemplate: false,
  printAfterSubmit: false,
})

// ── 同步 open prop ──
watch(() => props.open, (val) => {
  visible.value = val
  if (val) {
    loadConfig()
  }
})
watch(visible, (val) => {
  emit('update:open', val)
})

// ── 本地存储操作 ─
function loadConfig() {
  try {
    const stored = localStorage.getItem(STORAGE_KEY_PAGE)
    if (stored) {
      const parsed = JSON.parse(stored)
      pageFields.value = DEFAULT_PAGE_FIELDS.map(defaultField => {
        const saved = parsed.find((f: PageField) => f.key === defaultField.key)
        return saved ? { ...defaultField, ...saved } : { ...defaultField }
      })
    } else {
      pageFields.value = JSON.parse(JSON.stringify(DEFAULT_PAGE_FIELDS))
    }
  } catch {
    pageFields.value = JSON.parse(JSON.stringify(DEFAULT_PAGE_FIELDS))
  }

  try {
    const stored = localStorage.getItem(STORAGE_KEY_DEFAULT)
    if (stored) {
      const parsed = JSON.parse(stored)
      defaultFields.value = DEFAULT_FIELDS.map(df => {
        const saved = parsed.fields?.find((f: DefaultValueField) => f.key === df.key)
        return saved ? { ...df, ...saved } : { ...df }
      })
      defaultPriority.value = parsed.priority ?? false
    } else {
      defaultFields.value = JSON.parse(JSON.stringify(DEFAULT_FIELDS))
    }
  } catch {
    defaultFields.value = JSON.parse(JSON.stringify(DEFAULT_FIELDS))
  }

  try {
    const stored = localStorage.getItem(STORAGE_KEY_PRINT)
    if (stored) {
      const parsed = JSON.parse(stored)
      printSettings.alwaysLastTemplate = parsed.alwaysLastTemplate ?? false
      printSettings.printAfterSubmit = parsed.printAfterSubmit ?? false
    } else {
      printSettings.alwaysLastTemplate = false
      printSettings.printAfterSubmit = false
    }
  } catch {
    printSettings.alwaysLastTemplate = false
    printSettings.printAfterSubmit = false
  }
}

function savePageConfig() {
  localStorage.setItem(STORAGE_KEY_PAGE, JSON.stringify(pageFields.value))
  emitChange()
}

function saveDefaultConfig() {
  localStorage.setItem(STORAGE_KEY_DEFAULT, JSON.stringify({
    fields: defaultFields.value,
    priority: defaultPriority.value,
  }))
  emitChange()
}

function savePrintConfig() {
  localStorage.setItem(STORAGE_KEY_PRINT, JSON.stringify({
    alwaysLastTemplate: printSettings.alwaysLastTemplate,
    printAfterSubmit: printSettings.printAfterSubmit,
  }))
  emitChange()
}

function emitChange() {
  emit('change', {
    pageFields: pageFields.value,
    defaultFields: defaultFields.value,
    defaultPriority: defaultPriority.value,
    printSettings: { ...printSettings },
  })
}

// ── 事件处理 ──
function handlePageFieldChange() {
  savePageConfig()
}

function handleDefaultChange() {
  saveDefaultConfig()
}

function handlePrintChange() {
  // 打印设置不自动保存，需要点保存
}

function handleResetPageConfig() {
  pageFields.value = JSON.parse(JSON.stringify(DEFAULT_PAGE_FIELDS))
  savePageConfig()
}

function handleAddDefault(_key: string) {
  // TODO: 弹出选择器让用户选择默认值
}

function handleSearchDefault(_key: string) {
  // TODO: 弹出搜索选择器
}

function handleConfigDefault() {
  // TODO: 打开默认值详细配置
}

function handleSaveDefault() {
  saveDefaultConfig()
  emit('update:open', false)
}

function handleSavePrint() {
  savePrintConfig()
  emit('update:open', false)
}

function handleClose() {
  emit('update:open', false)
}

function getDefaultFieldDisplay(df: DefaultValueField): string {
  return df.value ? `${df.label}  ${df.value}` : df.label
}

onMounted(() => {
  if (props.open) {
    loadConfig()
  }
})
</script>

<style scoped>
/* ═══ Tabs ═══ */
.config-tabs :deep(.ant-tabs-nav) {
  padding: 16px 16px 0;
  margin-bottom: 0;
}

.config-tabs :deep(.ant-tabs-tab) {
  padding: 8px 28px;
  font-size: 14px;
  border: 1px solid #d9d9d9;
  border-radius: 4px 4px 0 0;
  margin-right: 4px;
  background: #fff;
  transition: all 0.2s;
}

.config-tabs :deep(.ant-tabs-tab-active) {
  background: #1890ff;
  border-color: #1890ff;
}

.config-tabs :deep(.ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: #fff;
}

.config-tabs :deep(.ant-tabs-ink-bar) {
  display: none;
}

.config-tabs :deep(.ant-tabs-nav::before) {
  border-bottom: none;
}

.config-tabs :deep(.ant-tabs-content) {
  padding: 0 16px 16px;
}

/* ═══ Tab 提示文字 ═══ */
.tab-tip {
  font-size: 12px;
  color: #fa8c16;
  margin-bottom: 12px;
  padding: 4px 0;
}

/* ═══ 页面配置表格 ═══ */
.page-config-table-wrap {
  max-height: 420px;
  overflow-y: auto;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
}

.page-config-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.page-config-table thead {
  position: sticky;
  top: 0;
  z-index: 1;
  background: #fafafa;
}

.page-config-table th {
  padding: 8px 12px;
  text-align: left;
  font-weight: 600;
  color: #262626;
  border-bottom: 1px solid #e8e8e8;
  background: #fafafa;
  white-space: nowrap;
}

.page-config-table td {
  padding: 6px 12px;
  border-bottom: 1px solid #f0f0f0;
  vertical-align: middle;
}

.page-config-table tbody tr {
  transition: background 0.15s;
}

.page-config-table tbody tr:nth-child(even) {
  background: #f0f5ff;
}

.page-config-table tbody tr:hover {
  background: #e6f7ff;
}

.page-config-table .row-highlight {
  background: #fff2e8;
}

.page-config-table .row-highlight:nth-child(even) {
  background: #fff2e8;
}

.page-config-table .row-highlight:hover {
  background: #ffe7cc;
}

.page-config-table .row-index {
  color: #8c8c8c;
  font-size: 12px;
  text-align: center;
}

.page-config-table .field-name {
  color: #262626;
  white-space: nowrap;
}

.page-config-table .cell-center {
  text-align: center;
}

.page-config-table :deep(.ant-input) {
  height: 26px;
  line-height: 24px;
}

/* ═══ Tab Footer ═══ */
.tab-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 12px 0 0;
  gap: 8px;
}

.tab-footer-right {
  justify-content: flex-end;
}

.tab-footer-bottom {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 16px 0 0;
  gap: 8px;
}

/* ═══ 录单默认值 ═══ */
.default-value-section {
  padding: 8px 0;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #262626;
  margin: 0 0 16px;
}

.default-field-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 360px;
}

.default-field-row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.default-field-input {
  flex: 1;
}

.default-field-input :deep(.ant-input) {
  background: #fafafa;
}

.default-field-add-btn {
  font-size: 16px;
  color: #1890ff;
  padding: 0 4px;
  flex-shrink: 0;
}

.default-field-search-btn {
  font-size: 12px;
  color: #8c8c8c;
  padding: 0 4px;
  flex-shrink: 0;
}

.default-priority-row {
  margin-top: 16px;
}

/* ═══ 打印设置 ═══ */
.print-settings {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px 0;
}

.print-settings :deep(.ant-checkbox-wrapper) {
  font-size: 13px;
}
</style>

<!-- 非 scoped：Modal 被 teleport 到 body，scoped 样式无法覆盖，需用全局样式 -->
<style>
.sale-order-form-config-wrap .ant-modal-header {
  background: #303030 !important;
  border-radius: 8px 8px 0 0;
  padding: 14px 24px;
  margin-bottom: 0;
}

.sale-order-form-config-wrap .ant-modal-title {
  color: #fff !important;
  font-size: 16px;
  font-weight: 500;
}

.sale-order-form-config-wrap .ant-modal-close {
  color: #fff !important;
  top: 14px;
}

.sale-order-form-config-wrap .ant-modal-close:hover {
  color: #fff !important;
}

.sale-order-form-config-wrap .ant-modal-body {
  padding: 0;
  max-height: 65vh;
  overflow-y: auto;
}

.sale-order-form-config-wrap .ant-modal-content {
  border-radius: 8px;
  overflow: hidden;
}

.sale-order-form-config-wrap .tab-footer-bottom .ant-btn-primary.ant-btn-dangerous {
  background: #ff4d4f;
  border-color: #ff4d4f;
}

.sale-order-form-config-wrap .tab-footer-bottom .ant-btn-primary.ant-btn-dangerous:hover {
  background: #ff7875;
  border-color: #ff7875;
}
</style>
