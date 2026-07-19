<template>
  <a-modal
    v-model:open="visible"
    title="配置"
    :width="800"
    :footer="null"
    :closable="true"
    :mask-closable="false"
    wrap-class-name="sale-outbound-form-config-wrap"
    @cancel="handleClose"
  >
    <a-tabs v-model:active-key="activeTab" class="config-tabs">
      <!-- ═══ Tab 1: 页面配置 ═══ -->
      <a-tab-pane key="page" tab="页面配置">
        <div class="tab-tip">勾选后自动保存，该设置对所有操作员生效</div>
        <div class="page-config-table-wrap">
          <table class="page-config-table">
            <thead>
              <tr>
                <th style="width: 50px;" />
                <th style="width: 160px;">名称</th>
                <th>显示名</th>
                <th style="width: 80px;">显示</th>
                <th style="width: 100px;">回车键跳转</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(field, index) in pageFields" :key="field.key" :class="{ 'row-highlight': field.visible }">
                <td class="row-index">{{ index + 1 }}</td>
                <td class="field-name">{{ field.label }}</td>
                <td>
                  <a-input v-model:value="field.displayName" size="small" @change="handlePageFieldChange" />
                </td>
                <td class="cell-center">
                  <a-checkbox v-model:checked="field.visible" @change="handlePageFieldChange" />
                </td>
                <td class="cell-center">
                  <a-checkbox v-model:checked="field.enterJump" @change="handlePageFieldChange" />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="tab-footer">
          <a-button @click="handleResetPageConfig">恢复默认值</a-button>
          <a-button type="primary" danger style="margin-left: 8px;" @click="handleClose">关闭</a-button>
        </div>
      </a-tab-pane>

      <!-- ═══ Tab 2: 录单默认值 ═══ -->
      <a-tab-pane key="default" tab="录单默认值">
        <div class="default-value-section">
          <h3 class="section-title">字段默认值设置</h3>
          <div class="default-field-list">
            <div v-for="df in defaultFields" :key="df.key" class="default-field-row">
              <a-input :value="getDefaultFieldDisplay(df)" size="small" class="default-field-input" readonly />
              <a-button type="link" size="small" class="default-field-add-btn" @click="handleAddDefault(df.key)">+</a-button>
              <a-button type="link" size="small" class="default-field-search-btn" @click="handleSearchDefault(df.key)">Q</a-button>
            </div>
          </div>
        </div>
        <div class="tab-footer tab-footer-right">
          <a-button @click="handleConfigDefault">配置</a-button>
        </div>
        <div class="tab-footer-bottom">
          <a-button type="primary" danger style="margin-right: 8px;" @click="handleSaveDefault">保存</a-button>
          <a-button @click="handleClose">取消</a-button>
        </div>
      </a-tab-pane>

      <!-- ═══ Tab 3: 打印设置 ═══ -->
      <a-tab-pane key="print" tab="打印设置">
        <div class="tab-tip">打印配置设置后只针对当前操作员有效</div>
        <div class="print-settings">
          <a-checkbox v-model:checked="printSettings.alwaysLastTemplate" @change="handlePrintChange">
            始终使用最后一次打印的模板，打印时不再选择
          </a-checkbox>
        </div>
        <div class="tab-footer-bottom">
          <a-button type="primary" danger style="margin-right: 8px;" @click="handleSavePrint">保存</a-button>
          <a-button @click="handleClose">取消</a-button>
        </div>
      </a-tab-pane>
    </a-tabs>
  </a-modal>

  <!-- ── 默认值选择器弹窗 ── -->
  <a-modal v-model:open="selectorVisible" :title="`选择${selectorFieldLabel}`" width="480px" @ok="handleSelectorConfirm" @cancel="selectorVisible = false">
    <div style="margin-bottom: 12px;">
      <a-input v-model:value="selectorSearchText" placeholder="搜索..." allow-clear @input="handleSelectorSearch" />
    </div>
    <div style="max-height: 300px; overflow-y: auto;">
      <a-radio-group v-model:value="selectorSelectedId" style="width: 100%;">
        <div v-for="opt in filteredSelectorOptions" :key="opt.id" style="padding: 6px 8px; border-bottom: 1px solid #f0f0f0;">
          <a-radio :value="opt.id">{{ opt.name }}</a-radio>
        </div>
        <div v-if="!filteredSelectorOptions.length" style="text-align: center; color: #999; padding: 20px;">暂无数据</div>
      </a-radio-group>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import request from '@/utils/request'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{
  'update:open': [value: boolean]
  'change': [config: any]
}>()

interface PageField { key: string; label: string; displayName: string; visible: boolean; enterJump: boolean }
interface DefaultValueField { key: string; label: string; value: string }
interface PrintSettings { alwaysLastTemplate: boolean }

const STORAGE_KEY_PAGE = 'sale-outbound-form-page-config'
const STORAGE_KEY_DEFAULT = 'sale-outbound-form-default-config'
const STORAGE_KEY_PRINT = 'sale-outbound-form-print-config'

// ── 页面字段默认数据（对标开发文档74字段） ──
const DEFAULT_PAGE_FIELDS: PageField[] = [
  // 1 收款方式
  { key: 'settlementMethod', label: '收款方式', displayName: '收款方式', visible: true, enterJump: false },
  // 2 编号（出库单号）
  { key: 'outboundNo', label: '编号', displayName: '编号', visible: true, enterJump: false },
  // 3-5 客户
  { key: 'customerId', label: '客户', displayName: '客户', visible: true, enterJump: true },
  { key: 'customerCode', label: '客户号', displayName: '客户号', visible: true, enterJump: false },
  { key: 'customerNumber', label: '客户编号', displayName: '客户编号', visible: true, enterJump: false },
  // 6-9 银行/税务/备注
  { key: 'bankName', label: '开户行', displayName: '开户行', visible: false, enterJump: false },
  { key: 'bankAccount', label: '银行账号', displayName: '银行账号', visible: false, enterJump: false },
  { key: 'taxNo', label: '税号', displayName: '税号', visible: false, enterJump: false },
  { key: 'customerRemark', label: '客户备注', displayName: '客户备注', visible: false, enterJump: false },
  // 10 发货仓库
  { key: 'warehouseId', label: '发货仓库', displayName: '发货仓库', visible: true, enterJump: true },
  // 11-12 线路/点位
  { key: 'region', label: '线路', displayName: '线路', visible: false, enterJump: false },
  { key: 'location', label: '点位', displayName: '点位', visible: true, enterJump: false },
  // 13-14 经手人/部门
  { key: 'salesPersonId', label: '经手人', displayName: '经手人', visible: true, enterJump: true },
  { key: 'departmentId', label: '部门', displayName: '部门', visible: true, enterJump: false },
  // 15-16 单据日期/销售类型
  { key: 'outboundDate', label: '单据日期', displayName: '单据日期', visible: true, enterJump: true },
  { key: 'outboundType', label: '销售类型', displayName: '销售类型', visible: true, enterJump: true },
  // 17-20 收货/级别
  { key: 'receiverName', label: '收货人', displayName: '收货人', visible: true, enterJump: false },
  { key: 'receiverPhone', label: '联系电话', displayName: '联系电话', visible: true, enterJump: false },
  { key: 'shippingAddress', label: '收货地址', displayName: '收货地址', visible: true, enterJump: false },
  { key: 'customerLevel', label: '客户级别', displayName: '客户级别', visible: true, enterJump: false },
  // 21-25 自定义字段(按文档编号: 1-2数字, 3-5文本)
  { key: 'extNum1', label: '自定义字段1(数字)', displayName: '自定义字段1(数字)', visible: false, enterJump: false },
  { key: 'extNum2', label: '自定义字段2(数字)', displayName: '自定义字段2(数字)', visible: false, enterJump: false },
  { key: 'extText1', label: '自定义字段3(文本)', displayName: '自定义字段3(文本)', visible: false, enterJump: false },
  { key: 'extText2', label: '自定义字段4(文本)', displayName: '自定义字段4(文本)', visible: false, enterJump: false },
  { key: 'extText3', label: '自定义字段5(文本)', displayName: '自定义字段5(文本)', visible: false, enterJump: false },
  // 26-27 审核人/摘要
  { key: 'approverId', label: '审核人', displayName: '审核人', visible: false, enterJump: false },
  { key: 'summary', label: '摘要', displayName: '摘要', visible: false, enterJump: false },
  // 28-30 收款账户
  { key: 'paymentAccount1', label: '收款账户1', displayName: '收款账户1', visible: true, enterJump: false },
  { key: 'settledAmount', label: '收款金额1', displayName: '收款金额1', visible: true, enterJump: false },
  { key: 'useMoreAccount', label: '更多账户', displayName: '更多账户', visible: false, enterJump: false },
  // 31-36 预收款
  { key: 'useDepositPayment', label: '使用预订货款', displayName: '使用预订货款', visible: false, enterJump: false },
  { key: 'prevAdvancePayment', label: '此前预收', displayName: '此前预收', visible: false, enterJump: false },
  { key: 'usedAdvancePayment', label: '使用预收款', displayName: '使用预收款', visible: false, enterJump: false },
  { key: 'orderDeposit', label: '订单已收订金', displayName: '订单已收订金', visible: false, enterJump: false },
  { key: 'availableAdvancePayment', label: '可用预收', displayName: '可用预收', visible: false, enterJump: false },
  { key: 'advancePaymentBalance', label: '预收余额', displayName: '预收余额', visible: false, enterJump: false },
  // 37-41 信用
  { key: 'creditLimit', label: '信用额度', displayName: '信用额度', visible: false, enterJump: false },
  { key: 'availableCredit', label: '可用额度', displayName: '可用额度', visible: false, enterJump: false },
  { key: 'prevArrears', label: '此前欠款', displayName: '此前欠款', visible: false, enterJump: false },
  { key: 'currentArrears', label: '本次欠款', displayName: '本次欠款', visible: false, enterJump: false },
  { key: 'arrearsBalance', label: '欠款余额', displayName: '欠款余额', visible: false, enterJump: false },
  // 42-43 收款日/对账日
  { key: 'paymentDate', label: '收款日', displayName: '收款日', visible: false, enterJump: false },
  { key: 'reconciliationDate', label: '对账日', displayName: '对账日', visible: false, enterJump: false },
  // 44 源单
  { key: 'sourceOrder', label: '源单', displayName: '源单', visible: false, enterJump: false },
  // 45-52 物流
  { key: 'deliveryMethod', label: '配送方式', displayName: '配送方式', visible: true, enterJump: false },
  { key: 'logisticsCompany', label: '物流公司', displayName: '物流公司', visible: true, enterJump: false },
  { key: 'logisticsBranch', label: '物流网点', displayName: '物流网点', visible: false, enterJump: false },
  { key: 'freightPayer', label: '运费承担方', displayName: '运费承担方', visible: false, enterJump: false },
  { key: 'freight', label: '运费', displayName: '运费', visible: false, enterJump: false },
  { key: 'trackingNumber', label: '物流单号', displayName: '物流单号', visible: false, enterJump: false },
  { key: 'waybillNo', label: '运单号', displayName: '运单号', visible: false, enterJump: false },
  { key: 'codAmount', label: '物流公司代收货款', displayName: '物流公司代收货款', visible: false, enterJump: false },
  { key: 'codAmountValue', label: '代收货款', displayName: '代收货款', visible: false, enterJump: false },
  { key: 'codCheckbox', label: '代收货款开关', displayName: '代收货款开关', visible: false, enterJump: false },
  { key: 'deliveryOrderNo', label: '配送单', displayName: '配送单', visible: false, enterJump: false },
  // 54-60 配送扩展/会员信息
  { key: 'expectedShipTime', label: '预计发货', displayName: '预计发货', visible: false, enterJump: false },
  { key: 'memberCardNo', label: '会员卡号', displayName: '会员卡号', visible: false, enterJump: false },
  { key: 'prevPoints', label: '此前积分', displayName: '此前积分', visible: false, enterJump: false },
  { key: 'memberGeneratedPoints', label: '产生积分', displayName: '产生积分', visible: false, enterJump: false },
  { key: 'memberExchangePoints', label: '兑换积分', displayName: '兑换积分', visible: false, enterJump: false },
  { key: 'memberUsedPoints', label: '使用积分', displayName: '使用积分', visible: false, enterJump: false },
  { key: 'currentPoints', label: '当前积分', displayName: '当前积分', visible: false, enterJump: false },
  // 61-64 备注
  { key: 'remark', label: '单据备注', displayName: '单据备注', visible: true, enterJump: false },
  { key: 'buyerRemark', label: '买家备注', displayName: '买家备注', visible: false, enterJump: false },
  { key: 'footerExtText1', label: '表尾自定义字段1', displayName: '表尾自定义字段1', visible: false, enterJump: false },
  { key: 'footerExtText2', label: '表尾自定义字段2', displayName: '表尾自定义字段2', visible: false, enterJump: false },
  // 65-67 单据信息
  { key: 'creatorName', label: '制单人', displayName: '制单人', visible: true, enterJump: false },
  { key: 'createTime', label: '制单时间', displayName: '制单时间', visible: true, enterJump: false },
  { key: 'printCount', label: '打印次数', displayName: '打印次数', visible: true, enterJump: false },
  // 68-74 汇总
  { key: 'totalQuantity', label: '销售数量', displayName: '销售数量', visible: true, enterJump: false },
  { key: 'returnQuantity', label: '退货数量', displayName: '退货数量', visible: true, enterJump: false },
  { key: 'totalAmount', label: '商品金额', displayName: '商品金额', visible: true, enterJump: false },
  { key: 'promoDiscount', label: '促销优惠', displayName: '促销优惠', visible: true, enterJump: false },
  { key: 'directDiscount', label: '优惠金额', displayName: '优惠金额', visible: true, enterJump: false },
  { key: 'otherFee', label: '其他费用', displayName: '其他费用', visible: true, enterJump: false },
  { key: 'netAmount', label: '本单金额', displayName: '本单金额', visible: true, enterJump: false },
]

const DEFAULT_DEFAULT_FIELDS: DefaultValueField[] = [
  { key: 'customerId', label: '客户', value: '' },
  { key: 'warehouseId', label: '发货仓库', value: '' },
  { key: 'salesPersonId', label: '经手人', value: '' },
  { key: 'logisticsCompany', label: '物流公司', value: '' },
]

const DEFAULT_PRINT_SETTINGS: PrintSettings = {
  alwaysLastTemplate: false,
}

// ── State ──
const visible = ref(false)
const activeTab = ref('page')
const pageFields = ref<PageField[]>([])
const defaultFields = ref<DefaultValueField[]>([])
const defaultPriority = ref(false)
const printSettings = reactive<PrintSettings>({ ...DEFAULT_PRINT_SETTINGS })

// ── Selector state ──
const selectorVisible = ref(false)
const selectorFieldKey = ref('')
const selectorFieldLabel = ref('')
const selectorSearchText = ref('')
const selectorSelectedId = ref<number | null>(null)
const selectorOptions = ref<any[]>([])
const filteredSelectorOptions = computed(() => {
  if (!selectorSearchText.value) return selectorOptions.value
  return selectorOptions.value.filter((o: any) =>
    (o.name || '').includes(selectorSearchText.value)
  )
})

watch(() => props.open, (val) => {
  visible.value = val
  if (val) loadConfig()
})
watch(visible, (val) => emit('update:open', val))

function loadConfig() {
  loadPageConfig()
  loadDefaultConfig()
  loadPrintConfig()
}

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY_PAGE)
    if (raw) {
      const parsed = JSON.parse(raw)
      pageFields.value = DEFAULT_PAGE_FIELDS.map(df => {
        const saved = parsed.find((f: PageField) => f.key === df.key)
        return saved ? { ...df, ...saved } : { ...df }
      })
    } else {
      pageFields.value = DEFAULT_PAGE_FIELDS.map(f => ({ ...f }))
    }
  } catch {
    pageFields.value = DEFAULT_PAGE_FIELDS.map(f => ({ ...f }))
  }
}

function loadDefaultConfig() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY_DEFAULT)
    if (raw) {
      const parsed = JSON.parse(raw)
      defaultFields.value = DEFAULT_DEFAULT_FIELDS.map(df => {
        const saved = parsed.fields?.find((f: DefaultValueField) => f.key === df.key)
        return saved ? { ...df, value: saved.value || '' } : { ...df }
      })
      defaultPriority.value = parsed.defaultPriority || false
    } else {
      defaultFields.value = DEFAULT_DEFAULT_FIELDS.map(f => ({ ...f }))
    }
  } catch {
    defaultFields.value = DEFAULT_DEFAULT_FIELDS.map(f => ({ ...f }))
  }
}

function loadPrintConfig() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY_PRINT)
    if (raw) {
      const parsed = JSON.parse(raw)
      Object.assign(printSettings, { ...DEFAULT_PRINT_SETTINGS, ...parsed })
    }
  } catch {
    Object.assign(printSettings, { ...DEFAULT_PRINT_SETTINGS })
  }
}

function persistPageConfig() {
  localStorage.setItem(STORAGE_KEY_PAGE, JSON.stringify(pageFields.value))
  emitChange()
}

function persistDefaultConfig() {
  localStorage.setItem(STORAGE_KEY_DEFAULT, JSON.stringify({
    fields: defaultFields.value,
    defaultPriority: defaultPriority.value,
  }))
}

function persistPrintConfig() {
  localStorage.setItem(STORAGE_KEY_PRINT, JSON.stringify({ ...printSettings }))
}

function emitChange() {
  emit('change', {
    pageFields: pageFields.value.map(f => ({ ...f })),
    defaultFields: defaultFields.value.map(f => ({ ...f })),
    printSettings: { ...printSettings },
  })
}

function handlePageFieldChange() { persistPageConfig() }
function handlePrintChange() { persistPrintConfig() }

function handleResetPageConfig() {
  pageFields.value = DEFAULT_PAGE_FIELDS.map(f => ({ ...f }))
  persistPageConfig()
}

function handleSavePrint() {
  persistPrintConfig()
  handleClose()
}

function handleSaveDefault() {
  persistDefaultConfig()
  handleClose()
}

function handleClose() { emit('update:open', false) }

// ── 默认值选择器 ──
function getDefaultFieldDisplay(df: DefaultValueField): string {
  return df.value || `[未设置]`
}

async function handleAddDefault(key: string) {
  selectorFieldKey.value = key
  const field = defaultFields.value.find(f => f.key === key)
  selectorFieldLabel.value = field?.label || key
  selectorSearchText.value = ''
  selectorSelectedId.value = null
  // Load options based on field type
  try {
    if (key === 'customerId') {
      const res = await request.get('/api/erp/partner/customer/list')
      selectorOptions.value = res || []
    } else if (key === 'warehouseId') {
      const res = await request.get('/api/wms/warehouse/list')
      selectorOptions.value = res || []
    } else if (key === 'salesPersonId') {
      const res = await request.get('/api/system/user/list')
      selectorOptions.value = res || []
    } else if (key === 'logisticsCompany') {
      const res = await request.get('/md/logistics/list')
      selectorOptions.value = res || []
    } else {
      selectorOptions.value = []
    }
  } catch {
    selectorOptions.value = []
  }
  selectorVisible.value = true
}

function handleSearchDefault(key: string) {
  handleAddDefault(key)
}

function handleSelectorSearch() {
  // filtering is done via computed
}

function handleSelectorConfirm() {
  if (selectorSelectedId.value != null) {
    const field = defaultFields.value.find(f => f.key === selectorFieldKey.value)
    if (field) {
      const opt = selectorOptions.value.find((o: any) => o.id === selectorSelectedId.value)
      field.value = opt ? `${opt.id}` : `${selectorSelectedId.value}`
    }
  }
  selectorVisible.value = false
  persistDefaultConfig()
}

function handleConfigDefault() {
  // Placeholder for detailed config
}

onMounted(() => { if (props.open) loadConfig() })
</script>

<style scoped>
.config-tabs :deep(.ant-tabs-nav) { padding: 0 16px; margin-bottom: 0; }
.config-tabs :deep(.ant-tabs-content-holder) { border: 1px solid #d9d9d9; border-top: none; border-radius: 0 0 4px 4px; }
.config-tabs :deep(.ant-tabs-content) { padding: 0 16px 16px; }
.config-tabs :deep(.ant-tabs-ink-bar) { display: none; }
.config-tabs :deep(.ant-tabs-nav::before) { border-bottom: none; }

.tab-tip { font-size: 12px; color: #fa8c16; margin-bottom: 12px; }

.page-config-table-wrap { max-height: 420px; overflow-y: auto; border: 1px solid #e8e8e8; border-radius: 4px; }
.page-config-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.page-config-table thead { position: sticky; top: 0; z-index: 1; background: #fafafa; }
.page-config-table th { padding: 8px 12px; text-align: left; font-weight: 600; color: #262626; border-bottom: 1px solid #e8e8e8; }
.page-config-table td { padding: 6px 12px; border-bottom: 1px solid #f0f0f0; vertical-align: middle; }
.page-config-table tbody tr { transition: background 0.15s; }
.page-config-table tbody tr:hover { background: #e6f7ff; }
.page-config-table tbody tr.row-highlight { background: #fffbe6; }

.row-index { text-align: center; color: #999; }
.field-name { font-weight: 500; color: #262626; }
.cell-center { text-align: center; }

.tab-footer { display: flex; justify-content: flex-end; align-items: center; padding: 12px 0 0; gap: 8px; }
.tab-footer-right { justify-content: flex-start; }
.tab-footer-bottom { display: flex; justify-content: flex-end; align-items: center; padding: 16px 0 0; gap: 8px; border-top: 1px solid #f0f0f0; margin-top: 12px; }

.default-value-section { padding: 0; }
.section-title { font-size: 14px; font-weight: 600; margin-bottom: 12px; }
.default-field-list { display: flex; flex-direction: column; gap: 8px; }
.default-field-row { display: flex; align-items: center; gap: 6px; }
.default-field-input { flex: 1; max-width: 300px; }
.default-field-add-btn { color: #1890ff; padding: 0; }
.default-field-search-btn { color: #1890ff; padding: 0; }

.print-settings { display: flex; flex-direction: column; gap: 16px; padding: 8px 0; }
</style>
