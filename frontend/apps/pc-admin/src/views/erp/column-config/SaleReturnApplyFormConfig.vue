<template>
  <a-modal
    v-model:open="visible"
    title="配置"
    :width="800"
    :footer="null"
    :closable="true"
    :mask-closable="false"
    wrap-class-name="sale-return-apply-form-config-wrap"
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
          <a-checkbox v-model:checked="defaultPriority" style="margin-top: 12px;">录单默认值优先</a-checkbox>
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
          <a-checkbox v-model:checked="printSettings.printAfterSubmit" @change="handlePrintChange" style="margin-top: 8px;">
            提单后立即打印
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
import { ref, reactive, computed, watch } from 'vue'
import request from '@/utils/request'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{
  'update:open': [value: boolean]
  'change': [config: any]
}>()

interface PageField { key: string; label: string; displayName: string; visible: boolean; enterJump: boolean }
interface DefaultValueField { key: string; label: string; value: string }
interface PrintSettings { alwaysLastTemplate: boolean; printAfterSubmit: boolean }

const STORAGE_KEY_PAGE = 'sale-return-apply-form-page-config-v2'
const STORAGE_KEY_DEFAULT = 'sale-return-apply-form-default-config'
const STORAGE_KEY_PRINT = 'sale-return-apply-form-print-config'

// ── 页面字段默认数据（仅「头部字段区」可选字段；编号/制单信息/本单金额/备注在固定区域渲染，不在此列） ──
// 默认勾选集 = 参考截图头部 2 行：客户/入库仓库/经手人/单据日期/销售类型/联系人 + 联系电话/联系地址/预计收货/审核人
const DEFAULT_PAGE_FIELDS: PageField[] = [
  // 客户
  { key: 'customerId', label: '客户', displayName: '客户', visible: true, enterJump: true },
  { key: 'customerCode', label: '客户编号', displayName: '客户编号', visible: false, enterJump: false },
  { key: 'customerLevel', label: '客户级别', displayName: '客户级别', visible: false, enterJump: false },
  // 银行/税务
  { key: 'bankName', label: '开户行', displayName: '开户行', visible: false, enterJump: false },
  { key: 'bankAccount', label: '银行账号', displayName: '银行账号', visible: false, enterJump: false },
  { key: 'taxNo', label: '税号', displayName: '税号', visible: false, enterJump: false },
  // 仓库/经手人/部门
  { key: 'warehouseId', label: '入库仓库', displayName: '入库仓库', visible: true, enterJump: true },
  { key: 'handlerId', label: '经手人', displayName: '经手人', visible: true, enterJump: true },
  { key: 'deptId', label: '部门', displayName: '部门', visible: false, enterJump: false },
  { key: 'orderDate', label: '单据日期', displayName: '单据日期', visible: true, enterJump: true },
  // 销售类型
  { key: 'returnApplyType', label: '销售类型', displayName: '销售类型', visible: true, enterJump: true },
  // 联系人
  { key: 'contactName', label: '联系人', displayName: '联系人', visible: true, enterJump: false },
  { key: 'contactPhone', label: '联系电话', displayName: '联系电话', visible: true, enterJump: false },
  { key: 'contactAddress', label: '联系地址', displayName: '联系地址', visible: true, enterJump: false },
  { key: 'expectedReceiveDate', label: '预计收货', displayName: '预计收货', visible: true, enterJump: false },
  // 自定义字段（扩展信息 Tab）
  { key: 'extNum1', label: '自定义字段1(数字)', displayName: '自定义字段1(数字)', visible: true, enterJump: false },
  { key: 'extNum2', label: '自定义字段2(数字)', displayName: '自定义字段2(数字)', visible: true, enterJump: false },
  { key: 'extText1', label: '自定义字段3(文本)', displayName: '自定义字段3(文本)', visible: true, enterJump: false },
  { key: 'extText2', label: '自定义字段4(文本)', displayName: '自定义字段4(文本)', visible: true, enterJump: false },
  { key: 'extText3', label: '自定义字段5(文本)', displayName: '自定义字段5(文本)', visible: true, enterJump: false },
  { key: 'footerExtText1', label: '表尾自定义字段1', displayName: '表尾自定义字段1', visible: true, enterJump: false },
  { key: 'footerExtText2', label: '表尾自定义字段2', displayName: '表尾自定义字段2', visible: true, enterJump: false },
  // 审核/摘要
  { key: 'auditor', label: '审核人', displayName: '审核人', visible: true, enterJump: false },
  { key: 'summary', label: '摘要', displayName: '摘要', visible: false, enterJump: false },
  // 信用额度
  { key: 'creditLimit', label: '信用额度', displayName: '信用额度', visible: false, enterJump: false },
  { key: 'availableCredit', label: '可用额度', displayName: '可用额度', visible: false, enterJump: false },
  { key: 'currentDebt', label: '本次欠款', displayName: '本次欠款', visible: false, enterJump: false },
  { key: 'prevDebt', label: '此前欠款', displayName: '此前欠款', visible: false, enterJump: false },
  { key: 'debtBalance', label: '欠款余额', displayName: '欠款余额', visible: false, enterJump: false },
  // 收款期限
  { key: 'collectionDeadline', label: '收款期限', displayName: '收款期限', visible: false, enterJump: false },
  // 源单/配送单
  { key: 'sourceOrder', label: '源单', displayName: '源单', visible: false, enterJump: false },
  { key: 'deliveryNo', label: '配送单', displayName: '配送单', visible: false, enterJump: false },
  // 物流
  { key: 'deliveryMethod', label: '配送方式', displayName: '配送方式', visible: false, enterJump: false },
  { key: 'deliveryRoute', label: '配送线路', displayName: '配送线路', visible: false, enterJump: false },
  { key: 'logisticsCompany', label: '物流公司', displayName: '物流公司', visible: false, enterJump: false },
  { key: 'shippingFee', label: '运费', displayName: '运费', visible: false, enterJump: false },
  { key: 'waybillNo', label: '运单号', displayName: '运单号', visible: false, enterJump: false },
  // 会员积分
  { key: 'memberCardNo', label: '会员卡号', displayName: '会员卡号', visible: false, enterJump: false },
  { key: 'prevPoints', label: '此前积分', displayName: '此前积分', visible: false, enterJump: false },
  { key: 'memberGeneratedPoints', label: '产生积分', displayName: '产生积分', visible: false, enterJump: false },
  { key: 'memberExchangePoints', label: '兑换积分', displayName: '兑换积分', visible: false, enterJump: false },
  { key: 'memberUsedPoints', label: '使用积分', displayName: '使用积分', visible: false, enterJump: false },
  { key: 'currentPoints', label: '剩余积分', displayName: '剩余积分', visible: false, enterJump: false },
]

const DEFAULT_DEFAULT_FIELDS: DefaultValueField[] = [
  { key: 'customerId', label: '客户', value: '' },
  { key: 'warehouseId', label: '入库仓库', value: '' },
  { key: 'handlerId', label: '经手人', value: '' },
  { key: 'returnApplyType', label: '销售类型', value: '' },
  { key: 'logisticsCompany', label: '物流公司', value: '' },
]

const DEFAULT_PRINT_SETTINGS: PrintSettings = {
  alwaysLastTemplate: false,
  printAfterSubmit: false,
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
      defaultPriority.value = parsed.priority || false
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
    priority: defaultPriority.value,
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

function handleConfigDefault() {
  // placeholder
}

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
  try {
    if (key === 'customerId') {
      const res = await request.get('/api/erp/partner/customer/list')
      selectorOptions.value = res || []
    } else if (key === 'warehouseId') {
      const res = await request.get('/api/wms/warehouse/list')
      selectorOptions.value = res || []
    } else if (key === 'handlerId') {
      const res = await request.get('/api/system/user/list')
      selectorOptions.value = res || []
    } else if (key === 'returnApplyType') {
      selectorOptions.value = [
        { id: 0, name: '正常销售' },
        { id: 1, name: '换货' },
        { id: 2, name: '调拨' },
        { id: 3, name: '其他' },
      ]
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
}

// expose
defineExpose({
  getPageFields: () => pageFields.value,
  getDefaultFields: () => defaultFields.value,
  getPrintSettings: () => ({ ...printSettings }),
})
</script>

<style scoped>
.config-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 0;
}
.tab-tip {
  padding: 8px 12px;
  color: #999;
  font-size: 12px;
  border-bottom: 1px solid #f0f0f0;
}
.page-config-table-wrap {
  max-height: 420px;
  overflow-y: auto;
}
.page-config-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.page-config-table th {
  position: sticky;
  top: 0;
  background: #fafafa;
  padding: 8px;
  border-bottom: 1px solid #f0f0f0;
  text-align: left;
  font-weight: 500;
  z-index: 1;
}
.page-config-table td {
  padding: 6px 8px;
  border-bottom: 1px solid #f5f5f5;
}
.page-config-table .row-index {
  text-align: center;
  color: #999;
}
.page-config-table .field-name {
  color: #333;
}
.page-config-table .cell-center {
  text-align: center;
}
.page-config-table .row-highlight td {
  background: #fff7e6;
}
.tab-footer {
  padding: 12px 0;
  border-top: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
}
.tab-footer-right {
  justify-content: flex-end;
}
.tab-footer-bottom {
  padding: 12px 0;
  text-align: right;
}
.section-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 12px;
}
.default-field-list {
  max-height: 350px;
  overflow-y: auto;
}
.default-field-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  border-bottom: 1px solid #f5f5f5;
}
.default-field-input {
  flex: 1;
}
.default-field-add-btn {
  font-size: 16px;
  padding: 0 4px;
}
.default-field-search-btn {
  padding: 0 4px;
}
.print-settings {
  padding: 16px 12px;
}
</style>
