<template>
  <a-modal
    v-model:open="visible"
    title="配置"
    :width="800"
    :footer="null"
    :closable="true"
    :mask-closable="false"
    wrap-class-name="pre-order-form-config-wrap"
    @cancel="handleClose"
  >
    <a-tabs v-model:active-key="activeTab" class="config-tabs">
      <!-- Tab 1: 页面配置 -->
      <a-tab-pane key="page" tab="页面配置">
        <div class="tab-tip">勾选后自动保存，该设置对所有操作员生效</div>
        <div class="page-config-table-wrap">
          <table class="page-config-table">
            <thead>
              <tr>
                <th style="width: 50px;" />
                <th style="width: 200px;">名称</th>
                <th>显示名</th>
                <th style="width: 80px;">显示</th>
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
              </tr>
            </tbody>
          </table>
        </div>
        <div class="tab-footer">
          <a-button @click="handleResetPageConfig">恢复默认值</a-button>
          <a-button type="primary" danger style="margin-left: 8px;" @click="handleClose">关闭</a-button>
        </div>
      </a-tab-pane>

      <!-- Tab 2: 录单默认值 -->
      <a-tab-pane key="default" tab="录单默认值">
        <div class="default-value-section">
          <div class="default-tip">
            当前操作员新增单据时，<span class="tip-config-btn">配置</span> 段带出默认值，可更改
          </div>
          <div class="default-grid">
            <div v-for="df in defaultFields" :key="df.key" class="default-grid-item">
              <span class="default-grid-label">{{ df.label }}</span>
              <div class="default-grid-input-wrap">
                <a-input v-model:value="df.value" size="small" class="default-grid-input" placeholder="请选择" allow-clear />
                <a-button type="link" size="small" class="default-grid-btn" @click="handleAddDefault(df.key)">+Q</a-button>
              </div>
            </div>
          </div>
        </div>
        <div class="tab-footer-bottom">
          <a-button type="primary" danger style="margin-right: 8px;" @click="handleSaveDefault">保存</a-button>
          <a-button @click="handleClose">取消</a-button>
        </div>
      </a-tab-pane>

      <!-- Tab 3: 打印设置 -->
      <a-tab-pane key="print" tab="打印设置">
        <div class="tab-tip">打印配置设置后只针对当前操作员有效</div>
        <div class="print-settings">
          <a-checkbox v-model:checked="printSettings.alwaysLastTemplate" @change="handlePrintChange">
            始终使用最后一次打印的模板，打印时不再选择
          </a-checkbox>
          <a-checkbox v-model:checked="printSettings.autoPrintAfterSubmit" @change="handlePrintChange">
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

  <!-- 默认值选择器弹窗 -->
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
import optionsApi from '@/api/options'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{
  'update:open': [value: boolean]
  'change': [config: any]
}>()

interface PageField { key: string; label: string; displayName: string; visible: boolean; enterJump: boolean }
interface DefaultValueField { key: string; label: string; value: string }
interface PrintSettings { alwaysLastTemplate: boolean; autoPrintAfterSubmit: boolean }

const STORAGE_KEY_PAGE = 'sale-pre-order-form-page-config'
const STORAGE_KEY_DEFAULT = 'sale-pre-order-form-default-config'
const STORAGE_KEY_PRINT = 'sale-pre-order-form-print-config'

// 页面字段默认数据（对标开发文档35字段）
const DEFAULT_PAGE_FIELDS: PageField[] = [
  { key: 'orderNo', label: '编号', displayName: '编号', visible: true, enterJump: false },
  { key: 'customerId', label: '客户', displayName: '客户', visible: true, enterJump: true },
  { key: 'customerCode', label: '客户编号', displayName: '客户编号', visible: true, enterJump: false },
  { key: 'bankName', label: '开户行', displayName: '开户行', visible: false, enterJump: false },
  { key: 'bankAccount', label: '银行账号', displayName: '银行账号', visible: false, enterJump: false },
  { key: 'taxNo', label: '税号', displayName: '税号', visible: false, enterJump: false },
  { key: 'warehouseId', label: '发货仓库', displayName: '发货仓库', visible: true, enterJump: true },
  { key: 'handlerId', label: '经手人', displayName: '经手人', visible: true, enterJump: true },
  { key: 'deptId', label: '部门', displayName: '部门', visible: true, enterJump: false },
  { key: 'orderDate', label: '单据日期', displayName: '单据日期', visible: true, enterJump: true },
  { key: 'saleType', label: '销售类型', displayName: '销售类型', visible: true, enterJump: true },
  { key: 'receiverName', label: '收货人', displayName: '收货人', visible: false, enterJump: false },
  { key: 'receiverPhone', label: '联系电话', displayName: '联系电话', visible: false, enterJump: false },
  { key: 'shippingAddress', label: '收货地址', displayName: '收货地址', visible: false, enterJump: false },
  { key: 'customerLevel', label: '客户级别', displayName: '客户级别', visible: true, enterJump: false },
  { key: 'extNum1', label: '自定义字段1(数字)', displayName: '自定义字段1(数字)', visible: false, enterJump: false },
  { key: 'extNum2', label: '自定义字段2(数字)', displayName: '自定义字段2(数字)', visible: false, enterJump: false },
  { key: 'extText1', label: '自定义字段3(文本)', displayName: '自定义字段3(文本)', visible: false, enterJump: false },
  { key: 'extText2', label: '自定义字段4(文本)', displayName: '自定义字段4(文本)', visible: false, enterJump: false },
  { key: 'extText3', label: '自定义字段5(文本)', displayName: '自定义字段5(文本)', visible: false, enterJump: false },
  { key: 'auditorName', label: '审核人', displayName: '审核人', visible: false, enterJump: false },
  { key: 'summary', label: '摘要', displayName: '摘要', visible: false, enterJump: false },
  { key: 'depositAccount1', label: '预订金账户', displayName: '预订金账户', visible: true, enterJump: false },
  { key: 'depositAmount', label: '预订金金额', displayName: '预订金金额', visible: true, enterJump: false },
  { key: 'moreAccounts', label: '更多账户', displayName: '更多账户', visible: true, enterJump: false },
  { key: 'creditLimit', label: '信用额度', displayName: '信用额度', visible: false, enterJump: false },
  { key: 'depositDeadline', label: '收款期限', displayName: '收款期限', visible: false, enterJump: false },
  { key: 'remark', label: '单据备注', displayName: '单据备注', visible: true, enterJump: false },
  { key: 'footerExtText1', label: '表尾自定义字段1', displayName: '表尾自定义字段1', visible: false, enterJump: false },
  { key: 'footerExtText2', label: '表尾自定义字段2', displayName: '表尾自定义字段2', visible: false, enterJump: false },
  { key: 'creatorName', label: '制单人', displayName: '制单人', visible: true, enterJump: false },
  { key: 'createTime', label: '制单时间', displayName: '制单时间', visible: true, enterJump: false },
  { key: 'printCount', label: '打印次数', displayName: '打印次数', visible: true, enterJump: false },
  { key: 'totalAmount', label: '商品金额', displayName: '商品金额', visible: true, enterJump: false },
  { key: 'orderAmount', label: '本单金额', displayName: '本单金额', visible: true, enterJump: false },
]

const DEFAULT_DEFAULT_FIELDS: DefaultValueField[] = [
  { key: 'customerId', label: '客户', value: '' },
  { key: 'warehouseId', label: '发货仓库', value: '' },
  { key: 'handlerId', label: '经手人', value: '' },
]

const DEFAULT_PRINT_SETTINGS: PrintSettings = {
  alwaysLastTemplate: false,
  autoPrintAfterSubmit: false,
}

// State
const visible = ref(false)
const activeTab = ref('page')
const pageFields = ref<PageField[]>([])
const defaultFields = ref<DefaultValueField[]>([])
const printSettings = reactive<PrintSettings>({ ...DEFAULT_PRINT_SETTINGS })

// Selector state
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

async function handleAddDefault(key: string) {
  selectorFieldKey.value = key
  const field = defaultFields.value.find(f => f.key === key)
  selectorFieldLabel.value = field?.label || key
  selectorSearchText.value = ''
  selectorSelectedId.value = null
  try {
    if (key === 'customerId') {
      selectorOptions.value = await optionsApi.getCustomers()
    } else if (key === 'warehouseId') {
      selectorOptions.value = await optionsApi.getWarehouses()
    } else if (key === 'handlerId') {
      selectorOptions.value = await optionsApi.getUsers()
    } else if (key === 'depositAccount1') {
      selectorOptions.value = await optionsApi.getAccounts()
    } else {
      selectorOptions.value = []
    }
  } catch {
    selectorOptions.value = []
  }
  selectorVisible.value = true
}

function handleSelectorSearch() {
  // filtering is done via computed
}

function handleSelectorConfirm() {
  if (selectorSelectedId.value != null) {
    const field = defaultFields.value.find(f => f.key === selectorFieldKey.value)
    if (field) {
      const opt = selectorOptions.value.find((o: any) => o.id === selectorSelectedId.value)
      // 账户类字段存名称（单据上落的是账户名），其余存主键ID
      field.value = field.key === 'depositAccount1'
        ? (opt?.name || `${selectorSelectedId.value}`)
        : (opt ? `${opt.id}` : `${selectorSelectedId.value}`)
    }
  }
  selectorVisible.value = false
  persistDefaultConfig()
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
.default-tip { font-size: 12px; color: #595959; margin-bottom: 12px; }
.default-tip .tip-config-btn { color: #fa8c16; font-weight: 600; border: 1px solid #fa8c16; padding: 0 4px; border-radius: 2px; font-size: 11px; }
.default-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 16px; }
.default-grid-item { display: flex; align-items: center; gap: 6px; }
.default-grid-label { font-size: 13px; color: #262626; white-space: nowrap; min-width: 80px; }
.default-grid-input-wrap { display: flex; align-items: center; flex: 1; }
.default-grid-input { flex: 1; }
.default-grid-btn { color: #1890ff; padding: 0 4px; font-size: 12px; flex-shrink: 0; }

.print-settings { display: flex; flex-direction: column; gap: 16px; padding: 8px 0; }
</style>
