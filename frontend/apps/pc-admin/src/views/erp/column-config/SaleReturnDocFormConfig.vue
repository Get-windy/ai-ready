<template>
  <a-modal
    v-model:open="visible"
    title="配置"
    :width="800"
    :footer="null"
    :closable="true"
    :mask-closable="false"
    wrap-class-name="sale-return-doc-form-config-wrap"
    @cancel="handleClose"
  >
    <a-tabs v-model:active-key="activeTab" class="config-tabs">
      <!-- ═══ Tab 1: 页面配置（勾选即时落库并联动表单字段显隐） ═══ -->
      <a-tab-pane key="page" tab="页面配置">
        <div class="tab-tip">勾选后自动保存，该设置对所有操作员生效</div>
        <div class="page-config-table-wrap">
          <table class="page-config-table">
            <thead>
              <tr>
                <th style="width: 50px;">
                  序号
                </th>
                <th style="width: 160px;">
                  名称
                </th>
                <th>显示名</th>
                <th style="width: 80px;">
                  显示
                </th>
                <th style="width: 100px;">
                  回车键跳转
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(field, index) in pageFields" :key="field.key" :class="{ 'row-highlight': field.visible }">
                <td class="row-index">
                  {{ index + 1 }}
                </td>
                <td class="field-name">
                  {{ field.label }}
                </td>
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
          <a-button type="primary" style="margin-left: 8px;" @click="handleClose">关闭</a-button>
        </div>
      </a-tab-pane>

      <!-- ═══ Tab 2: 录单默认值 ═══ -->
      <a-tab-pane key="default" tab="录单设置">
        <div class="default-value-section">
          <h3 class="section-title">字段默认值设置</h3>
          <div class="default-field-list">
            <div v-for="df in defaultFields" :key="df.key" class="default-field-row">
              <span class="default-field-label">{{ df.label }}</span>
              <a-input :value="getDefaultFieldDisplay(df)" size="small" class="default-field-input" readonly />
              <a-button type="link" size="small" class="default-field-add-btn" @click="handleOpenSelector(df.key)">
                +
              </a-button>
              <a-button type="link" size="small" class="default-field-search-btn" @click="handleOpenSelector(df.key)">
                Q
              </a-button>
            </div>
          </div>
          <a-checkbox v-model:checked="defaultPriority" style="margin-top: 12px;">
            录单默认值优先
          </a-checkbox>
        </div>
        <div class="tab-footer-bottom">
          <a-button type="primary" style="margin-right: 8px;" @click="handleSaveDefault">保存</a-button>
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
            保存后立即打印
          </a-checkbox>
        </div>
        <div class="tab-footer-bottom">
          <a-button type="primary" style="margin-right: 8px;" @click="handleSavePrint">保存</a-button>
          <a-button @click="handleClose">取消</a-button>
        </div>
      </a-tab-pane>
    </a-tabs>
  </a-modal>

  <!-- ── 默认值选择器弹窗 ── -->
  <a-modal
    v-model:open="selectorVisible"
    :title="`选择${selectorFieldLabel}`"
    width="480px"
    @ok="handleSelectorConfirm"
    @cancel="selectorVisible = false"
  >
    <div style="margin-bottom: 12px;">
      <a-input v-model:value="selectorSearchText" placeholder="搜索..." allow-clear />
    </div>
    <div style="max-height: 300px; overflow-y: auto;">
      <a-radio-group v-model:value="selectorSelectedId" style="width: 100%;">
        <div v-for="opt in filteredSelectorOptions" :key="opt.id" style="padding: 6px 8px; border-bottom: 1px solid #f0f0f0;">
          <a-radio :value="opt.id">
            {{ opt.name }}
          </a-radio>
        </div>
        <div v-if="!filteredSelectorOptions.length" style="text-align: center; color: #999; padding: 20px;">
          暂无数据
        </div>
      </a-radio-group>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import optionsApi from '@/api/options'
import {
  SALE_RETURN_DOC_PAGE_FIELDS as DEFAULT_PAGE_FIELDS,
  SRD_FORM_CONFIG_KEY as STORAGE_KEY_PAGE,
  SRD_FORM_DEFAULTS_KEY as STORAGE_KEY_DEFAULT,
  SRD_FORM_PRINT_KEY as STORAGE_KEY_PRINT,
  type SaleReturnDocPageField as PageField,
} from './saleReturnDocFormDefaults'

defineOptions({ name: 'SaleReturnDocFormConfig' })

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{
  'update:open': [value: boolean]
  'change': [config: any]
}>()

interface DefaultValueField { key: string; label: string; value: string }
interface PrintSettings { alwaysLastTemplate: boolean; printAfterSubmit: boolean }

const DEFAULT_DEFAULT_FIELDS: DefaultValueField[] = [
  { key: 'customerId', label: '客户', value: '' },
  { key: 'warehouseId', label: '入库仓库', value: '' },
  { key: 'handlerId', label: '经手人', value: '' },
  { key: 'deptId', label: '部门', value: '' },
  { key: 'salesType', label: '销售类型', value: '' },
  { key: 'paymentAccount1', label: '付款账户', value: '' },
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

// ── 选择器状态 ──
const selectorVisible = ref(false)
const selectorFieldKey = ref('')
const selectorFieldLabel = ref('')
const selectorSearchText = ref('')
const selectorSelectedId = ref<any>(null)
const selectorOptions = ref<any[]>([])
const filteredSelectorOptions = computed(() => {
  if (!selectorSearchText.value) return selectorOptions.value
  return selectorOptions.value.filter((o: any) => (o.name || '').includes(selectorSearchText.value))
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
      const saved = Array.isArray(parsed) ? parsed : parsed.pageFields
      pageFields.value = DEFAULT_PAGE_FIELDS.map(df => {
        const hit = saved?.find((f: PageField) => f.key === df.key)
        return hit ? { ...df, ...hit } : { ...df }
      })
      if (pageFields.value.length === 0) pageFields.value = DEFAULT_PAGE_FIELDS.map(f => ({ ...f }))
      return
    }
  } catch {
    // 存档损坏时回落到默认
  }
  pageFields.value = DEFAULT_PAGE_FIELDS.map(f => ({ ...f }))
}

function loadDefaultConfig() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY_DEFAULT)
    if (raw) {
      const parsed = JSON.parse(raw)
      const saved = parsed.fields as DefaultValueField[] | undefined
      defaultFields.value = DEFAULT_DEFAULT_FIELDS.map(df => {
        const hit = saved?.find(f => f.key === df.key)
        return hit ? { ...df, value: hit.value || '' } : { ...df }
      })
      defaultPriority.value = parsed.priority || false
      return
    }
  } catch {
    // ignore
  }
  defaultFields.value = DEFAULT_DEFAULT_FIELDS.map(f => ({ ...f }))
}

function loadPrintConfig() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY_PRINT)
    if (raw) {
      Object.assign(printSettings, { ...DEFAULT_PRINT_SETTINGS, ...JSON.parse(raw) })
      return
    }
  } catch {
    // ignore
  }
  Object.assign(printSettings, { ...DEFAULT_PRINT_SETTINGS })
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

function handleSaveDefault() {
  persistDefaultConfig()
  emitChange()
  handleClose()
}

function handleSavePrint() {
  persistPrintConfig()
  emitChange()
  handleClose()
}

function handleClose() { emit('update:open', false) }

// ── 默认值选择器（真实后端下拉选项） ──
function getDefaultFieldDisplay(df: DefaultValueField): string {
  if (!df.value) return '[未设置]'
  const hit = selectorCache[df.key]?.find((o: any) => String(o.id) === String(df.value))
  return hit ? `${hit.name}(${df.value})` : df.value
}

const selectorCache: Record<string, any[]> = {}

async function loadSelectorOptions(key: string): Promise<any[]> {
  if (selectorCache[key]) return selectorCache[key]
  let list: any[] = []
  try {
    if (key === 'customerId') list = await optionsApi.getCustomers()
    else if (key === 'warehouseId') list = await optionsApi.getWarehouses()
    else if (key === 'handlerId') list = await optionsApi.getUsers()
    else if (key === 'deptId') list = await optionsApi.getDepartments()
    else if (key === 'salesType') {
      list = [
        { id: 'normal', name: '正常销售' },
        { id: 'return', name: '退货' },
      ]
    }
  } catch {
    list = []
  }
  selectorCache[key] = list || []
  return list
}

async function handleOpenSelector(key: string) {
  selectorFieldKey.value = key
  selectorFieldLabel.value = defaultFields.value.find(f => f.key === key)?.label || key
  selectorSearchText.value = ''
  selectorSelectedId.value = null
  selectorOptions.value = await loadSelectorOptions(key)
  selectorVisible.value = true
}

function handleSelectorConfirm() {
  if (selectorSelectedId.value !== null && selectorSelectedId.value !== undefined && selectorSelectedId.value !== '') {
    const field = defaultFields.value.find(f => f.key === selectorFieldKey.value)
    if (field) field.value = String(selectorSelectedId.value)
  }
  selectorVisible.value = false
}

// 打开时预热一次下拉（用于显示名回显）
watch(() => props.open, (val) => {
  if (val) {
    DEFAULT_DEFAULT_FIELDS.forEach(df => { loadSelectorOptions(df.key) })
  }
})

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
.default-field-label {
  font-size: 13px;
  color: #333;
  min-width: 80px;
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
