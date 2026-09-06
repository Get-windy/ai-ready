<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <BillFormPage
        v-model="formData"
        :header="headerConfig"
        :basic-info-fields="basicInfoFields"
        :summary="summaryConfig"
        :footer="footerConfig"
        @action="handleAction"
        @field-change="handleFieldChange"
        @draft="handleSaveDraft"
        @submit="handleSubmit"
      >
        <!-- ═══ 检验项目明细 ═══ -->
        <template #detail-table="{ onExpandChange }">
          <BillDetailTable
            :columns="detailColumns"
            :data-source="formData.products"
            :max-height="tableMaxHeight"
            :storage-key="'quality-standard-form-columns'"
            @cell-change="handleCellChange"
            @expand-change="onExpandChange"
          >
            <template #actionCell="{ index, empty }">
              <template v-if="!empty">
                <a-space :size="2">
                  <a-button type="link" size="small" class="action-add-btn" @click="handleInsertItem(index)">
                    <PlusCircleOutlined />
                  </a-button>
                  <a-button type="link" size="small" class="action-del-btn" @click="handleRemoveItem(index)">
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
              <span class="remark-label">描述</span>
              <a-input
                v-model:value="formData.description"
                size="small"
                class="remark-input"
                placeholder="请输入描述"
              />
            </div>
          </div>
          <div class="doc-info-row">
            <span class="doc-info-item">
              状态 <a-tag :color="statusColor">{{ statusText }}</a-tag>
            </span>
            <span class="doc-info-item">抽检比例 {{ formData.sampleRate != null ? formData.sampleRate + '%' : '-' }}</span>
            <span class="doc-info-item">合格阈值 {{ formData.passThreshold != null ? formData.passThreshold + '%' : '-' }}</span>
            <span class="doc-info-item">制单人 <a-tag color="blue">{{ creatorName }}</a-tag></span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
          </div>
        </template>
      </BillFormPage>

      <!-- ═══ 配置弹窗：页面配置/录单默认值/打印设置 ═══ -->
      <a-modal
        v-model:open="showFormConfig"
        title="配置"
        :width="760"
        :footer="null"
        destroy-on-close
      >
        <a-tabs v-model:active-key="configModalTab" size="small">
          <a-tab-pane key="pageConfig" tab="页面配置">
            <p class="config-hint">勾选后自动保存，该设置对所有操作员生效</p>
            <a-table
              :columns="pageConfigTableColumns"
              :data-source="pageConfigFields"
              :pagination="false"
              size="small"
              row-key="key"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'displayName'">
                  <a-input v-model:value="record.displayName" size="small" @blur="saveFormConfig" />
                </template>
                <template v-if="column.key === 'visible'">
                  <a-checkbox
                    :checked="record.visible"
                    @change="(e: any) => handlePageConfigFieldVisibleChange(record.key, e.target.checked)"
                  />
                </template>
                <template v-if="column.key === 'enterJump'">
                  <a-checkbox
                    :checked="record.enterJump"
                    @change="(e: any) => handlePageConfigEnterJumpChange(record.key, e.target.checked)"
                  />
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="defaultValues" tab="录单默认值">
            <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-form-item label="默认抽检比例">
                <a-input-number v-model:value="formData.defaultSampleRate" :min="0" :max="100" size="small" style="width:100%" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认合格阈值">
                <a-input-number v-model:value="formData.defaultPassThreshold" :min="0" :max="100" size="small" style="width:100%" @change="saveFormConfig" />
              </a-form-item>
            </a-form>
          </a-tab-pane>
          <a-tab-pane key="printSettings" tab="打印设置">
            <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-form-item label="打印模板">
                <a-select v-model:value="formData.printTemplate" size="small" style="width:100%" @change="saveFormConfig">
                  <a-select-option value="standard">标准模板</a-select-option>
                  <a-select-option value="simple">简化模板</a-select-option>
                  <a-select-option value="detailed">详细模板</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="打印份数">
                <a-input-number v-model:value="formData.printCopies" :precision="0" :min="1" size="small" style="width:100%" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="纸张大小">
                <a-select v-model:value="formData.printPaperSize" size="small" style="width:100%" @change="saveFormConfig">
                  <a-select-option value="A4">A4</a-select-option>
                  <a-select-option value="A5">A5</a-select-option>
                  <a-select-option value="B5">B5</a-select-option>
                </a-select>
              </a-form-item>
            </a-form>
          </a-tab-pane>
        </a-tabs>
      </a-modal>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ClockCircleOutlined, MinusCircleOutlined, PlusCircleOutlined, PrinterOutlined, SettingOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BasicInfoField, BillFooterConfig, BillHeaderConfig, SummaryRow } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { qualityStandardApi } from '@/api/quality'
import { userPageConfigApi } from '@/api/erp'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'QualityStandardForm' })

const router = useRouter()
const userStore = useUserStore()
const creatorName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
const tableMaxHeight = ref(400)

// ═══ 检验类型 / 状态字典 ═══
const INSPECTION_TYPE_OPTIONS = [
  { label: '入库检验', value: 'INBOUND' },
  { label: '出库检验', value: 'OUTBOUND' },
  { label: '过程检验', value: 'PROCESS' },
]
const STATUS_OPTIONS = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
]
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  1: { text: '启用', color: 'success' },
  0: { text: '停用', color: 'default' },
}

// ═══ useBillForm ═══
function parseInspectionItems(json?: string): any[] {
  if (!json) return []
  try {
    const arr = JSON.parse(json)
    if (!Array.isArray(arr)) return []
    return arr.filter((i: any) => i && typeof i === 'object')
  } catch {
    return []
  }
}

const {
  formData,
  saving,
  effectiveMode,
  handleAddProduct,
  handleRemoveProduct,
  handleFieldChange: baseFieldChange,
  handleSaveDraft,
  handleSubmit,
} = useBillForm({
  billPrefix: 'QSTD',
  api: {
    create: async (data: any) => {
      const { status, ...payload } = data
      const res: any = await qualityStandardApi.create({ ...payload, status: Number(status ?? 1) })
      return res?.data || res
    },
    update: async (id: number, data: any) => {
      const { status, ...payload } = data
      const res: any = await qualityStandardApi.update(id, { ...payload, status: Number(status ?? 1) })
      return res?.data || res
    },
    getById: async (id: number) => {
      const res: any = await qualityStandardApi.get(id)
      return res?.data || res || {}
    },
  },
  redirectPath: '/quality/standard/list',
  codeApiPath: '/quality/standard/next-no',
  fields: [
    { key: 'standardName', label: '标准名称', type: 'input', required: true },
    { key: 'inspectionType', label: '检验类型', type: 'select', required: true },
    { key: 'sampleRate', label: '抽检比例', type: 'number' },
    { key: 'passThreshold', label: '合格阈值', type: 'number' },
    { key: 'status', label: '状态', type: 'select' },
  ],
  productDefaults: {
    name: '', method: '', standard: '', aql: undefined,
  },
  onDetailLoaded: (data: any, fd: Record<string, any>) => {
    fd.orderNo = data.standardCode || ''
    fd.standardName = data.standardName || ''
    fd.inspectionType = data.inspectionType || ''
    fd.sampleRate = data.sampleRate ?? 100
    fd.passThreshold = data.passThreshold ?? 95
    fd.status = data.status ?? 1
    fd.description = data.description || ''
    const items = parseInspectionItems(data.inspectionItems)
    fd.products = items.length > 0
      ? items.map((it: any, i: number) => ({ id: `item-${i}`, name: it.name || '', method: it.method || '', standard: it.standard || '', aql: it.aql }))
      : [{ id: 'item-0', name: '', method: '', standard: '', aql: undefined }]
  },
  transformPayload: (fd: Record<string, any>) => {
    const items = (fd.products || [])
      .filter((p: any) => p.name && p.name.trim())
      .map((p: any) => ({ name: p.name.trim(), method: p.method || '', standard: p.standard || '', aql: p.aql }))
    return {
      standardCode: fd.orderNo || '',
      standardName: (fd.standardName || '').trim(),
      inspectionType: fd.inspectionType || '',
      sampleRate: fd.sampleRate ?? 100,
      passThreshold: fd.passThreshold ?? 95,
      status: Number(fd.status ?? 1),
      description: fd.description || '',
      inspectionItems: JSON.stringify(items),
    }
  },
})

// 初始化默认值
if (formData.status === undefined) formData.status = 1
if (formData.sampleRate === undefined) formData.sampleRate = 100
if (formData.passThreshold === undefined) formData.passThreshold = 95
if (formData.standardName === undefined) formData.standardName = ''
if (formData.description === undefined) formData.description = ''

// ═══ 计算属性 ═══
const currentStatus = computed(() => Number(formData.status ?? 1))
const statusText = computed(() => STATUS_MAP[currentStatus.value]?.text || '停用')
const statusColor = computed(() => STATUS_MAP[currentStatus.value]?.color || 'default')
const validItemCount = computed(() => (formData.products || []).filter((p: any) => p.name && p.name.trim()).length)

// ═══ 页眉配置 ═══
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '质检标准',
  orderNo: formData.orderNo,
  showAttachment: false,
  actions: [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ],
}))

// ═══ 基本信息字段 ═══
const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  { key: 'orderNo', label: '标准编码', type: 'input', inlineLabel: true, width: 220, disabled: true },
  { key: 'standardName', label: '标准名称', type: 'input', required: true, inlineLabel: true, width: 260 },
  { key: 'inspectionType', label: '检验类型', type: 'select', required: true, inlineLabel: true, width: 180, options: INSPECTION_TYPE_OPTIONS },
  { key: 'sampleRate', label: '抽检比例', type: 'number', inlineLabel: true, width: 130, precision: 2 },
  { key: 'passThreshold', label: '合格阈值', type: 'number', inlineLabel: true, width: 130, precision: 2 },
  { key: 'status', label: '状态', type: 'select', inlineLabel: true, width: 120, options: STATUS_OPTIONS },
  { key: 'description', label: '描述', type: 'textarea', inlineLabel: true, width: 360 },
]

const DEFAULT_HIDDEN_FIELDS = ['description']
type FieldConfig = { visible: boolean; enterJump: boolean }
const pageConfig = reactive<Record<string, FieldConfig>>({})
for (const f of ALL_BASIC_INFO_FIELDS) {
  pageConfig[f.key] = {
    visible: !DEFAULT_HIDDEN_FIELDS.includes(f.key),
    enterJump: ['select', 'date', 'number', 'input', 'textarea'].includes(f.type),
  }
}

const FORM_CONFIG_MODULE = 'quality-standard-form'
const FORM_CONFIG_PAGE = 'form'

async function loadFormConfig() {
  try {
    const raw = await userPageConfigApi.get(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE)
    if (!raw) return
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw
    if (parsed.fields && typeof parsed.fields === 'object') {
      Object.keys(parsed.fields).forEach((k) => {
        if (pageConfig[k]) {
          pageConfig[k].visible = parsed.fields[k].visible !== false
          pageConfig[k].enterJump = !!parsed.fields[k].enterJump
        }
      })
    }
    if (parsed.defaults) {
      Object.assign(formData, {
        defaultSampleRate: parsed.defaults.defaultSampleRate,
        defaultPassThreshold: parsed.defaults.defaultPassThreshold,
        printTemplate: parsed.defaults.printTemplate,
        printCopies: parsed.defaults.printCopies,
        printPaperSize: parsed.defaults.printPaperSize,
      })
    }
  } catch { /* 保持默认 */ }
}

async function saveFormConfig() {
  const payload = {
    fields: Object.fromEntries(Object.entries(pageConfig).map(([k, v]) => [k, { visible: v.visible, enterJump: v.enterJump }])),
    defaults: {
      defaultSampleRate: formData.defaultSampleRate,
      defaultPassThreshold: formData.defaultPassThreshold,
      printTemplate: formData.printTemplate,
      printCopies: formData.printCopies,
      printPaperSize: formData.printPaperSize,
    },
  }
  try {
    await userPageConfigApi.save(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE, JSON.stringify(payload))
  } catch { /* 静默 */ }
}

const showFormConfig = ref(false)
const configModalTab = ref('pageConfig')

const basicInfoFields = computed<BasicInfoField[]>(() =>
  ALL_BASIC_INFO_FIELDS.filter(f => pageConfig[f.key]?.visible !== false)
)

const pageConfigFields = computed(() =>
  ALL_BASIC_INFO_FIELDS.map((f, i) => ({
    key: f.key,
    index: i + 1,
    name: f.label,
    displayName: f.label,
    visible: pageConfig[f.key]?.visible !== false,
    enterJump: pageConfig[f.key]?.enterJump ?? false,
  }))
)

const pageConfigTableColumns = [
  { title: '序号', key: 'index', width: 60 },
  { title: '名称', key: 'name', width: 160 },
  { title: '显示名', key: 'displayName', width: 160 },
  { title: '显示', key: 'visible', width: 70, align: 'center' as const },
  { title: '回车键跳转', key: 'enterJump', width: 100, align: 'center' as const },
]

function handlePageConfigFieldVisibleChange(fieldKey: string, visible: boolean) {
  if (pageConfig[fieldKey]) pageConfig[fieldKey].visible = visible
  saveFormConfig()
}
function handlePageConfigEnterJumpChange(fieldKey: string, checked: boolean) {
  if (pageConfig[fieldKey]) pageConfig[fieldKey].enterJump = checked
  saveFormConfig()
}

// ═══ 摘要 / 页脚 ═══
const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '检验项目', value: validItemCount.value, statusLabel: statusText.value },
  { label: '标准编码', value: formData.orderNo || '-' },
])

const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '检验项目',
  amountValue: String(validItemCount.value),
  amountHighlight: true,
  primaryBtnText: '保存',
  saving: saving.value,
}))

// ═══ 检验项目明细列 ═══
const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 60, fixed: 'left' },
  { key: 'name', title: '检验项目', type: 'input', width: 180 },
  { key: 'method', title: '检验方法', type: 'input', width: 150 },
  { key: 'standard', title: '标准值', type: 'input', width: 180 },
  { key: 'aql', title: '允收%', type: 'number', width: 110, precision: 2 },
]

// ═══ 事件处理 ═══
function handleCellChange(record: any, _fieldKey: string, _value: any) {
  // 预留：字段联动（如允收%<=抽检比例 的提示）
}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
}

// 在明细指定位置插入一行
function handleInsertItem(index: number) {
  handleAddProduct()
  const item = formData.products.pop()
  if (item) formData.products.splice(index + 1, 0, item)
}

async function handleAction(actionKey: string) {
  switch (actionKey) {
    case 'print':
      if (formData.orderNo) {
        window.print()
      } else {
        message.warning('请先保存后再打印')
      }
      break
    case 'history':
      router.push('/quality/standard/list')
      break
    case 'config':
      showFormConfig.value = true
      break
  }
}

function handleError(err: any) {
  console.warn('[质检标准] ErrorBoundary:', err)
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

// ═══ 生命周期 ═══
onMounted(async () => {
  // 无论新增/编辑，确保检验项目明细至少一行空行（允许保存无检验项目的标准）
  if (formData.products.length === 0) {
    handleAddProduct()
  }
  // 应用录单默认值（新增时抽检比例/合格阈值）
  if (effectiveMode.value !== 'edit') {
    if (formData.defaultSampleRate !== undefined) formData.sampleRate = formData.defaultSampleRate
    if (formData.defaultPassThreshold !== undefined) formData.passThreshold = formData.defaultPassThreshold
  }
  nextTick(() => {
    tableMaxHeight.value = Math.max(200, window.innerHeight - 420)
  })
  loadFormConfig()
})
</script>

<style scoped>
.action-add-btn { color: #1890ff; padding: 0; font-size: 14px; }
.action-del-btn { color: #ff4d4f; padding: 0; font-size: 14px; }
.remark-section { padding: 4px 0; }
.remark-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.remark-label { font-size: 12px; color: #595959; white-space: nowrap; min-width: 60px; }
.remark-input { flex: 1; }
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
.config-hint { font-size: 12px; color: #8c8c8c; margin-bottom: 8px; }
</style>
