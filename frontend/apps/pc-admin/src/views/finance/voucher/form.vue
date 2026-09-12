<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <a-alert
        v-if="isLocked"
        :message="`当前凭证状态为「${statusText}」，已进入审核流程，内容不可修改`"
        type="warning"
        show-icon
        banner
        style="flex-shrink:0"
      />

      <BillFormPage
        :model-value="formData"
        :header="headerConfig"
        :basic-info-fields="basicInfoFields"
        :tabs="tabsConfig"
        :summary="summaryConfig"
        :footer="footerConfig"
        @update:model-value="handleModelUpdate"
        @field-change="handleFieldChange"
        @action="handleHeaderAction"
        @search-btn="handleSearchBtn"
        @draft="handleSaveDraft"
        @submit="handlePrimarySubmit"
      >
        <!-- ═══ 凭证分录明细 ═══ -->
        <template #detail-table="{ onExpandChange }">
          <BillDetailTable
            :columns="detailColumns"
            v-model:data-source="formData.items"
            :summary-columns="detailSummaryColumns"
            :storage-key="'voucher-form-columns'"
            :view-mode="isLocked"
            @cell-change="handleCellChange"
            @expand-change="onExpandChange"
          >
            <template #actionCell="{ index, empty }">
              <a-space :size="2">
                <a-button type="link" size="small" class="action-btn" :disabled="isLocked" @click="handleAddRow()">
                  <PlusCircleOutlined />
                </a-button>
                <a-button v-if="!empty" type="link" size="small" class="action-btn danger" :disabled="isLocked" @click="handleRemoveRow(index)">
                  <MinusCircleOutlined />
                </a-button>
              </a-space>
            </template>
          </BillDetailTable>
        </template>

        <!-- ═══ 备注 + 单据信息 ═══ -->
        <template #bottom-extra>
          <div class="remark-section">
            <div class="remark-row">
              <span class="remark-label">单据备注</span>
              <a-input
                v-model:value="formData.remark"
                size="small"
                class="remark-input"
                placeholder="请输入单据备注"
                :disabled="isLocked"
              />
            </div>
          </div>
          <div class="doc-info-row">
            <span class="doc-info-item">
              单据状态 <a-tag :color="statusColor">{{ statusText }}</a-tag>
            </span>
            <span class="doc-info-item">制单人 <a-tag color="blue">{{ formData.prepBy || currentUserName || '系统' }}</a-tag></span>
            <span class="doc-info-item">制单时间 {{ formData.prepAt || formatNow() }}</span>
            <span v-if="formData.auditBy" class="doc-info-item">审核人 {{ formData.auditBy }}</span>
            <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
          </div>
        </template>
      </BillFormPage>

      <!-- ═══ 配置弹窗：页面配置/录单默认值/打印设置 ═══ -->
      <a-modal v-model:open="showFormConfig" title="配置" :width="760" :footer="null" destroy-on-close>
        <a-tabs v-model:active-key="configModalTab" size="small">
          <a-tab-pane key="pageConfig" tab="页面配置">
            <p class="config-hint">勾选后自动保存，该设置对所有操作员生效</p>
            <a-table :columns="pageConfigTableColumns" :data-source="pageConfigFields" :pagination="false" size="small" row-key="key">
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'displayName'">
                  <a-input v-model:value="record.displayName" size="small" @blur="saveFormConfig" />
                </template>
                <template v-if="column.key === 'visible'">
                  <a-checkbox :checked="record.visible" @change="(e: any) => handlePageConfigFieldVisibleChange(record.key, e.target.checked)" />
                </template>
                <template v-if="column.key === 'enterJump'">
                  <a-checkbox :checked="record.enterJump" @change="(e: any) => handlePageConfigEnterJumpChange(record.key, e.target.checked)" />
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <a-tab-pane key="defaultValues" tab="录单默认值">
            <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-form-item label="默认经手人">
                <a-select v-model:value="formData.defaultHandlerName" show-search size="small" style="width:100%"
                  :filter-option="filterOption" :options="userSelectOptions" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认部门">
                <a-select v-model:value="formData.defaultDeptName" show-search size="small" style="width:100%"
                  :filter-option="filterOption" :options="departmentSelectOptions" @change="saveFormConfig" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  PrinterOutlined, ExportOutlined, SettingOutlined,
  PlusCircleOutlined, MinusCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { BasicInfoField, BillHeaderConfig, BillTabConfig, TabField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { voucherApi, accountSubjectApi } from '@/api/finance'
import { userPageConfigApi } from '@/api/erp'
import optionsApi from '@/api/options'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

defineOptions({ name: 'FinanceVoucherForm' })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const idParam = computed(() => route.query.id as string)
const isNew = computed(() => !idParam.value || idParam.value === 'new' || idParam.value === '0')
const currentUserName = computed(() => userStore.nickname || userStore.username || '')

// ═══ 状态字典（draft/audited/posted/reversed） ═══
const STATUS_MAP: Record<string, { text: string; color: string }> = {
  draft: { text: '草稿', color: 'default' },
  audited: { text: '已审核', color: 'blue' },
  posted: { text: '已记账', color: 'green' },
  reversed: { text: '已冲销', color: 'red' },
}
const statusText = computed(() => STATUS_MAP[formData.status ?? 'draft']?.text || '未知')
const statusColor = computed(() => STATUS_MAP[formData.status ?? 'draft']?.color || 'default')
const isLocked = computed(() => !isNew.value && formData.status !== 'draft')

// ═══ 下拉选项 ═══
const userOptions = ref<any[]>([])
const departmentOptions = ref<any[]>([])
const loadingOptions = ref(false)

const userSelectOptions = computed(() =>
  userOptions.value.map((u: any) => ({ label: u.name, value: u.name }))
)
const departmentSelectOptions = computed(() =>
  departmentOptions.value.map((d: any) => ({ label: d.name, value: d.name }))
)

const filterOption = (input: string, option: any) => {
  const text = option?.title || option?.label || option?.value || ''
  return text.toString().toLowerCase().includes(input.toLowerCase())
}

async function loadOptions() {
  loadingOptions.value = true
  try {
    const [us, ds] = await Promise.all([
      optionsApi.getUsers().catch(() => []),
      optionsApi.getDepartments().catch(() => []),
    ])
    userOptions.value = us || []
    departmentOptions.value = ds || []
  } finally {
    loadingOptions.value = false
  }
}

// ═══ 表单数据 ═══
const emptyForm = () => ({
  id: undefined as number | undefined,
  voucherNo: '',
  voucherDate: dayjs().format('YYYY-MM-DD'),
  fiscalYear: dayjs().year(),
  fiscalPeriod: dayjs().month() + 1,
  voucherType: 'manual',
  summary: '',
  handlerName: undefined,
  deptName: undefined,
  sourceNo: '',
  attachments: 0,
  printCount: 0,
  prepBy: '',
  prepAt: '',
  auditBy: '',
  auditAt: '',
  postBy: '',
  postAt: '',
  status: 'draft',
  remark: '',
  items: [] as any[],
  // 录单默认值
  defaultHandlerName: undefined,
  defaultDeptName: undefined,
  printTemplate: 'standard',
  printCopies: 0,
  printPaperSize: 'A4',
})
const formData = reactive<any>(emptyForm())

// ═══ 会计科目下拉（value=subjectCode 联动回填，对齐 ar-ap-adjust 金标准） ═══
const subjectOptions = ref<{ value: string; label: string }[]>([])
const subjectInfoMap = ref<Record<string, any>>({})
async function loadSubjects() {
  try {
    const res: any = await accountSubjectApi.getList()
    const data = res?.data ?? res ?? []
    const list = Array.isArray(data) ? data : (data?.records || [])
    subjectInfoMap.value = {}
    subjectOptions.value = list.map((s: any) => {
      subjectInfoMap.value[s.subjectCode] = { id: s.id, subjectCode: s.subjectCode, subjectName: s.subjectName }
      return { value: s.subjectCode, label: `${s.subjectCode} ${s.subjectName}` }
    })
  } catch {
    subjectOptions.value = []
  }
}

// ═══ 顶部「表头」基础字段（页面配置可显隐） ═══
const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  { key: 'voucherNo', label: '编号', type: 'input', disabled: true },
  { key: 'voucherDate', label: '单据日期', type: 'date', required: true },
  { key: 'handlerName', label: '经手人', type: 'select', required: true },
  { key: 'deptName', label: '部门', type: 'select', required: true },
  { key: 'custom1', label: '自定义字段1(数字)', type: 'number' },
  { key: 'custom2', label: '自定义字段2(数字)', type: 'number' },
  { key: 'custom3', label: '自定义字段3(文本)', type: 'input' },
  { key: 'custom4', label: '自定义字段4(文本)', type: 'input' },
  { key: 'custom5', label: '自定义字段5(文本)', type: 'input' },
  { key: 'summary', label: '摘要', type: 'input', width: 'wide' },
  { key: 'remark', label: '单据备注', type: 'input', width: 'wide' },
  { key: 'prepBy', label: '制单人', type: 'input', disabled: true },
  { key: 'printCount', label: '打印次数', type: 'number', disabled: true },
  { key: 'sourceNo', label: '源单', type: 'input' },
]

// ═══ 页面配置（字段显隐） ═══
const PAGE_FIELD_DEFAULTS: Record<string, { visible: boolean; enterJump: boolean }> = {
  voucherNo: { visible: true, enterJump: false },
  voucherDate: { visible: true, enterJump: true },
  handlerName: { visible: true, enterJump: true },
  deptName: { visible: true, enterJump: false },
  custom1: { visible: false, enterJump: false },
  custom2: { visible: false, enterJump: false },
  custom3: { visible: false, enterJump: false },
  custom4: { visible: false, enterJump: false },
  custom5: { visible: false, enterJump: false },
  summary: { visible: false, enterJump: false },
  remark: { visible: true, enterJump: false },
  prepBy: { visible: true, enterJump: false },
  printCount: { visible: true, enterJump: false },
  sourceNo: { visible: true, enterJump: false },
}

const pageConfig = reactive<Record<string, { visible: boolean; enterJump: boolean }>>(
  Object.fromEntries(Object.entries(PAGE_FIELD_DEFAULTS).map(([k, v]) => [k, { ...v }]))
)

const basicInfoFields = computed<BasicInfoField[]>(() =>
  ALL_BASIC_INFO_FIELDS
    .filter(f => pageConfig[f.key]?.visible !== false)
    .map(f => ({
      ...f,
      options: f.key === 'handlerName'
        ? userSelectOptions.value
        : f.key === 'deptName'
          ? departmentSelectOptions.value
          : (f as any).options,
      loading: ['handlerName', 'deptName'].includes(f.key) ? loadingOptions.value : (f as any).loading,
      searchBtn: ['handlerName', 'deptName'].includes(f.key) ? '+Q' : (f as any).searchBtn,
    }))
)

const tabsConfig = computed<BillTabConfig[]>(() => {
  const customFields: TabField[] = [
    { key: 'custom1', label: '自定义字段1(数字)', type: 'number', precision: 2 },
    { key: 'custom2', label: '自定义字段2(数字)', type: 'number', precision: 2 },
    { key: 'custom3', label: '自定义字段3(文本)', type: 'input' },
    { key: 'custom4', label: '自定义字段4(文本)', type: 'input' },
    { key: 'custom5', label: '自定义字段5(文本)', type: 'input' },
  ]
  return [{ key: 'custom', tab: '自定义字段', fields: customFields }]
})

// ═══ 页面配置弹窗字段（全量） ═══
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
  { title: '显示名', key: 'displayName', width: 200 },
  { title: '显示', key: 'visible', width: 70, align: 'center' as const },
  { title: '回车键跳转', key: 'enterJump', width: 100, align: 'center' as const },
]

const FORM_CONFIG_MODULE = 'voucher-form'
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
        defaultHandlerName: parsed.defaults.defaultHandlerName,
        defaultDeptName: parsed.defaults.defaultDeptName,
        printTemplate: parsed.defaults.printTemplate,
        printCopies: parsed.defaults.printCopies,
        printPaperSize: parsed.defaults.printPaperSize,
      })
    }
  } catch { /* API 不可用时保持默认 */ }
}

async function saveFormConfig() {
  const payload = {
    fields: Object.fromEntries(Object.entries(pageConfig).map(([k, v]) => [k, { visible: v.visible, enterJump: v.enterJump }])),
    defaults: {
      defaultHandlerName: formData.defaultHandlerName,
      defaultDeptName: formData.defaultDeptName,
      printTemplate: formData.printTemplate,
      printCopies: formData.printCopies,
      printPaperSize: formData.printPaperSize,
    },
  }
  try {
    await userPageConfigApi.save(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE, JSON.stringify(payload))
  } catch { /* 静默失败 */ }
}

function handlePageConfigFieldVisibleChange(fieldKey: string, visible: boolean) {
  if (pageConfig[fieldKey]) pageConfig[fieldKey].visible = visible
  saveFormConfig()
}
function handlePageConfigEnterJumpChange(fieldKey: string, checked: boolean) {
  if (pageConfig[fieldKey]) pageConfig[fieldKey].enterJump = checked
  saveFormConfig()
}

// ═══ 配置弹窗 ═══
const showFormConfig = ref(false)
const configModalTab = ref('pageConfig')

// ═══ Header 配置 ═══
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '记账凭证',
  orderNo: formData.voucherNo || '待生成',
  actions: [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'export', label: '导出', icon: ExportOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ],
}))

function handleHeaderAction(key: string) {
  if (key === 'print') handlePrint()
  else if (key === 'export') handleExport()
  else if (key === 'config') showFormConfig.value = true
}

// ═══ BillFormPage 协同 ═══
function handleModelUpdate(newVal: Record<string, any>) {
  Object.assign(formData, newVal)
  onFieldValueChange()
}
function handleSearchBtn(_fieldKey: string, _btn: string) {
  // 预留：搜索按钮弹选择器
}
function handleFieldChange(_fieldKey: string, _val: any) {
  onFieldValueChange()
}

function onFieldValueChange() { /* computed 处理借贷合计 */ }

// ═══ 分录明细（8 列：操作/明细摘要/科目编号/科目全名/科目名称/明细科目/借方金额/贷方金额） ═══
const detailColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 56, fixed: 'left' },
  { key: 'summary', title: '明细摘要', type: 'input', width: 170 },
  { key: 'subjectCode', title: '科目编号', type: 'select', width: 220, options: subjectOptions.value, placeholder: '选择科目' },
  { key: 'subjectFullName', title: '科目全名', type: 'input', width: 180, readonly: true },
  { key: 'subjectName', title: '科目名称', type: 'input', width: 140, defaultHidden: true, readonly: true },
  { key: 'detailSubject', title: '明细科目', type: 'input', width: 160 },
  { key: 'debitAmount', title: '借方金额', type: 'number', width: 130, precision: 2, align: 'right' },
  { key: 'creditAmount', title: '贷方金额', type: 'number', width: 130, precision: 2, align: 'right' },
])

const debitTotal = computed(() => formData.items.reduce((s: number, e: any) => s + Number(e.debitAmount || 0), 0))
const creditTotal = computed(() => formData.items.reduce((s: number, e: any) => s + Number(e.creditAmount || 0), 0))
const balanceOk = computed(() => Math.abs(debitTotal.value - creditTotal.value) < 0.001)

const detailSummaryColumns = computed(() => [
  { key: 'debitAmount', value: debitTotal.value.toFixed(2), highlight: true },
  { key: 'creditAmount', value: creditTotal.value.toFixed(2), highlight: true },
])

function handleCellChange(record: any, fieldKey: string, value: any) {
  record[fieldKey] = value
  // 科目选中联动：回填科目ID/名称/全名
  if (fieldKey === 'subjectCode') {
    const info = subjectInfoMap.value[value]
    if (info) {
      record.subjectId = info.id
      record.subjectName = info.subjectName
      record.subjectFullName = info.subjectName
    }
  }
  onFieldValueChange()
}

function handleAddRow() {
  formData.items.push({
    _key: `manual_${Date.now()}`,
    summary: '',
    subjectId: undefined,
    subjectCode: '',
    subjectName: '',
    subjectFullName: '',
    detailSubject: '',
    debitAmount: 0,
    creditAmount: 0,
  })
  onFieldValueChange()
}

function handleRemoveRow(index: number) {
  if (index >= 0 && index < formData.items.length) {
    formData.items.splice(index, 1)
    onFieldValueChange()
  }
}

// ═══ 摘要面板 ═══
const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '借方合计', value: debitTotal.value.toFixed(2) },
  { label: '贷方合计', value: creditTotal.value.toFixed(2) },
  { label: '借贷平衡', value: balanceOk.value ? '已平衡' : '不平衡', statusLabel: statusText.value },
  { label: '本单余额', value: (debitTotal.value - creditTotal.value).toFixed(2), divider: true },
])

// ═══ 页脚 ═══
const saving = ref(false)
const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '借贷差额',
  amountValue: (debitTotal.value - creditTotal.value).toFixed(2),
  amountHighlight: !balanceOk.value,
  draftBtnText: isLocked.value ? undefined : '保存草稿',
  primaryBtnText: isLocked.value ? undefined : '记账',
  draftShortcut: isLocked.value ? undefined : 'Ctrl+S',
  primaryShortcut: isLocked.value ? undefined : 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══ 提交 ═══
function buildPayload(): any {
  const items = formData.items.map((e: any, i: number) => ({
    lineNo: i + 1,
    subjectId: e.subjectId,
    subjectCode: e.subjectCode || '',
    subjectName: e.subjectName || '',
    subjectFullName: e.subjectFullName || '',
    detailSubject: e.detailSubject || '',
    summary: e.summary || '',
    debitAmount: Number(e.debitAmount || 0),
    creditAmount: Number(e.creditAmount || 0),
    sourceType: 'manual',
  }))
  return {
    voucherNo: formData.voucherNo || undefined,
    voucherDate: formData.voucherDate,
    fiscalYear: formData.fiscalYear,
    fiscalPeriod: formData.fiscalPeriod,
    voucherType: 'manual',
    summary: formData.summary || undefined,
    handlerName: formData.handlerName,
    deptName: formData.deptName,
    sourceNo: formData.sourceNo || undefined,
    attachments: formData.attachments || 0,
    printCount: formData.printCount || 0,
    prepBy: currentUserName.value,
    remark: formData.remark || undefined,
    items,
  }
}

function validate() {
  if (!formData.voucherDate) { message.warning('请选择单据日期'); return false }
  if (!formData.handlerName) { message.warning('请选择经手人'); return false }
  if (!formData.deptName) { message.warning('请选择部门'); return false }
  if (formData.items.length === 0) { message.warning('请至少输入一条分录'); return false }
  const noSubject = formData.items.some((e: any) => !e.subjectCode)
  if (noSubject) { message.warning('请为每条分录选择科目'); return false }
  const hasZero = formData.items.every((e: any) => Number(e.debitAmount || 0) === 0 && Number(e.creditAmount || 0) === 0)
  if (hasZero) { message.warning('请输入借贷金额'); return false }
  if (!balanceOk.value) { message.warning('借合计与贷合计不等，无法记账'); return false }
  return true
}

async function handleSaveDraft() {
  if (!formData.handlerName) { message.warning('请选择经手人'); return }
  if (formData.items.length === 0) { message.warning('请至少输入一条分录'); return }
  saving.value = true
  try {
    const payload = buildPayload()
    if (formData.id) {
      await voucherApi.update(formData.id, payload)
    } else {
      await voucherApi.create(payload)
    }
    message.success('保存草稿成功')
    router.push('/finance/voucher')
  } catch (e: any) {
    message.error(e?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handlePrimarySubmit() {
  if (!validate()) return
  saving.value = true
  try {
    const payload = buildPayload()
    let id = formData.id
    if (!id) {
      const res: any = await voucherApi.create(payload)
      id = res?.id
      formData.id = id
    } else {
      await voucherApi.update(id, payload)
    }
    // 记账：审核 + 过账（数据层面三级，前端体验两态）
    await voucherApi.audit(id)
    await voucherApi.post(id)
    message.success('记账成功')
    router.push('/finance/voucher')
  } catch (e: any) {
    message.error(e?.data?.message || '记账失败')
  } finally {
    saving.value = false
  }
}

// ═══ 表头操作 ═══
function handlePrint() {
  message.info('打印(F8)待对接打印模板')
}
async function handleExport() {
  try {
    const res: any = await request.get('/erp/finance/voucher/export', {
      params: { keyword: formData.voucherNo },
      responseType: 'blob',
    })
    const url = window.URL.createObjectURL(res)
    const a = document.createElement('a')
    a.href = url
    a.download = `会计凭证_${formData.voucherNo || dayjs().format('YYYYMMDD')}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch {
    message.error('导出失败')
  }
}

// ═══ 详情加载 ═══
async function loadDetail(id: number) {
  try {
    const res: any = await voucherApi.getById(id)
    if (!res) return
    Object.assign(formData, {
      id: res.id,
      voucherNo: res.voucherNo || '',
      voucherDate: res.voucherDate || dayjs().format('YYYY-MM-DD'),
      fiscalYear: res.fiscalYear || dayjs().year(),
      fiscalPeriod: res.fiscalPeriod || dayjs().month() + 1,
      voucherType: res.voucherType || 'manual',
      summary: res.summary || '',
      handlerName: res.handlerName,
      deptName: res.deptName,
      sourceNo: res.sourceNo || '',
      attachments: res.attachments || 0,
      printCount: res.printCount || 0,
      prepBy: res.prepBy || '',
      prepAt: res.prepAt || '',
      auditBy: res.auditBy || '',
      auditAt: res.auditAt || '',
      postBy: res.postBy || '',
      postAt: res.postAt || '',
      status: res.status || 'draft',
      remark: res.remark || '',
    })
    if (res.items?.length) {
      formData.items = res.items.map((d: any) => ({
        id: d.id,
        summary: d.summary || '',
        subjectId: d.subjectId,
        subjectCode: d.subjectCode || '',
        subjectName: d.subjectName || '',
        subjectFullName: d.subjectFullName || d.subjectName || '',
        detailSubject: d.detailSubject || '',
        debitAmount: Number(d.debitAmount || 0),
        creditAmount: Number(d.creditAmount || 0),
      }))
      onFieldValueChange()
    }
  } catch (e) {
    console.warn('[凭证] 加载失败', e)
  }
}

function formatNow(): string {
  return dayjs().format('YYYY-MM-DD HH:mm:ss')
}

function handleError(error: Error) {
  console.error('[凭证] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// 复制凭证：加载源凭证分录，重置为新建草稿（新单号）
async function loadCopyOf(id: number) {
  try {
    const res: any = await voucherApi.getById(id)
    if (!res) return
    formData.voucherDate = res.voucherDate || dayjs().format('YYYY-MM-DD')
    formData.summary = res.summary || ''
    formData.handlerName = res.handlerName
    formData.deptName = res.deptName
    formData.remark = res.remark || ''
    formData.status = 'draft'
    formData.voucherType = 'manual'
    formData.auditBy = ''
    formData.postBy = ''
    formData.postAt = ''
    formData.auditAt = ''
    try {
      const no: any = await voucherApi.nextNo()
      formData.voucherNo = no || ''
    } catch {
      formData.voucherNo = ''
    }
    formData.items = (res.items || []).map((d: any) => ({
      summary: d.summary || '',
      subjectId: d.subjectId,
      subjectCode: d.subjectCode || '',
      subjectName: d.subjectName || '',
      subjectFullName: d.subjectFullName || d.subjectName || '',
      detailSubject: d.detailSubject || '',
      debitAmount: Number(d.debitAmount || 0),
      creditAmount: Number(d.creditAmount || 0),
    }))
  } catch (e) {
    console.warn('[凭证] 复制加载失败', e)
  }
}

onMounted(async () => {
  await Promise.all([loadOptions(), loadSubjects()])
  loadFormConfig()
  const copyOfId = route.query.copyOf ? Number(route.query.copyOf) : undefined
  const editId = route.query.id ? Number(route.query.id) : undefined
  if (editId && editId > 0) {
    await loadDetail(editId)
  } else if (copyOfId && copyOfId > 0) {
    await loadCopyOf(copyOfId)
  } else {
    // 新凭证：取下一单号 + 默认经手人/部门
    try {
      const no: any = await voucherApi.nextNo()
      formData.voucherNo = no || ''
    } catch {
      formData.voucherNo = ''
    }
    if (formData.defaultHandlerName) formData.handlerName = formData.defaultHandlerName
    if (formData.defaultDeptName) formData.deptName = formData.defaultDeptName
    if (!formData.handlerName) formData.handlerName = userOptions.value[0]?.name
    if (!formData.deptName) formData.deptName = departmentOptions.value[0]?.name
    handleAddRow(); handleAddRow()
  }
})
</script>

<style scoped>
.config-hint { color: #999; font-size: 13px; margin-bottom: 8px; }
.remark-section { display: flex; flex-direction: column; gap: 6px; padding: 6px 0; }
.remark-row { display: flex; align-items: center; gap: 8px; }
.remark-label { font-size: 13px; color: #595959; white-space: nowrap; flex-shrink: 0; }
.remark-input { flex: 1; max-width: 420px; }
.doc-info-row { display: flex; flex-wrap: wrap; gap: 12px; padding: 8px 0; }
.doc-info-item { font-size: 13px; color: #595959; display: inline-flex; align-items: center; gap: 4px; }
.action-btn { padding: 0 4px; }
</style>
