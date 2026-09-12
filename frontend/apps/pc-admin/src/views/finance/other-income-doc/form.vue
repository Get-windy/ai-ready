<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <a-alert
        v-if="isLocked"
        :message="`当前单据状态为「${statusText}」，已记账，内容不可修改`"
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
        @draft="handleSaveDraft"
        @submit="handlePrimarySubmit"
      >
        <!-- ═══ 收入项明细 ═══ -->
        <template #detail-table>
          <BillDetailTable
            :columns="detailColumns"
            v-model:data-source="formData.items"
            :summary-columns="detailSummaryColumns"
            :storage-key="'other-income-doc-form-columns'"
            :view-mode="isLocked"
            @cell-change="handleCellChange"
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
            <span class="doc-info-item">制单人 <a-tag color="blue">{{ formData.creatorName || currentUserName || '系统' }}</a-tag></span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
            <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
          </div>
        </template>
      </BillFormPage>

      <!-- ═══ 配置弹窗：页面配置/打印设置 ═══ -->
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
          <a-tab-pane key="printSettings" tab="打印设置">
            <a-form layout="horizontal" :label-col="{ span: 8 }" :wrapper-col="{ span: 14 }">
              <a-form-item label="始终使用最后一次打印的模板，打印时不再选择">
                <a-switch v-model:checked="printSettings.useLastTemplate" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="记账后立即打印">
                <a-switch v-model:checked="printSettings.printAfterPost" @change="saveFormConfig" />
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
import { otherIncomeApi } from '@/api/finance'
import { userPageConfigApi } from '@/api/erp'
import optionsApi from '@/api/options'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

defineOptions({ name: 'FinanceOtherIncomeDocForm' })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const idParam = computed(() => route.query.id as string)
const isNew = computed(() => !idParam.value || idParam.value === 'new' || idParam.value === '0')
const currentUserName = computed(() => userStore.nickname || userStore.username || '')

// ═══ 状态字典 ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '已记账', color: 'green' },
}
const statusText = computed(() => STATUS_MAP[formData.status ?? 0]?.text || '未知')
const statusColor = computed(() => STATUS_MAP[formData.status ?? 0]?.color || 'default')
const isLocked = computed(() => !isNew.value && formData.status !== 0)

const incomeTypeOptions = [
  { label: '往来单位收入', value: 1 },
  { label: '内部收入', value: 2 },
]

// ═══ 下拉选项 ═══
const partnerOptions = ref<any[]>([])
const userOptions = ref<any[]>([])
const departmentOptions = ref<any[]>([])
const accountOptions = ref<any[]>([])
const loadingOptions = ref(false)

const partnerSelectOptions = computed(() =>
  partnerOptions.value.map((c: any) => ({ label: `${c.name || ''}${c.code ? '(' + c.code + ')' : ''}`, value: c.id }))
)
const userSelectOptions = computed(() =>
  userOptions.value.map((u: any) => ({ label: u.name, value: u.id }))
)
const departmentSelectOptions = computed(() =>
  departmentOptions.value.map((d: any) => ({ label: d.name, value: d.id }))
)
const accountSelectOptions = computed(() =>
  accountOptions.value.map((a: any) => ({ label: a.name + (a.code ? '(' + a.code + ')' : ''), value: a.name }))
)

const filterOption = (input: string, option: any) => {
  const text = option?.label || option?.name || ''
  return text.toString().toLowerCase().includes(input.toLowerCase())
}

async function loadOptions() {
  loadingOptions.value = true
  try {
    const [ps, us, ds, as_] = await Promise.all([
      optionsApi.getCustomers().catch(() => []),
      optionsApi.getUsers().catch(() => []),
      optionsApi.getDepartments().catch(() => []),
      optionsApi.getAccounts().catch(() => []),
    ])
    partnerOptions.value = ps || []
    userOptions.value = us || []
    departmentOptions.value = ds || []
    accountOptions.value = as_ || []
  } finally {
    loadingOptions.value = false
  }
}

// ═══ 表单数据 ═══
const formData = reactive<any>({
  id: undefined,
  docNo: '',
  incomeType: 1,
  partnerId: undefined,
  partnerName: '',
  partnerCode: '',
  handlerId: undefined,
  handlerName: undefined,
  departmentId: undefined,
  departmentName: undefined,
  incomeDate: dayjs().format('YYYY-MM-DD'),
  receiptAccount1: undefined,
  receiptAmount1: 0,
  moreAccount: '',
  accountSubjectCode: undefined,
  summary: '',
  remark: '',
  creatorName: '',
  createTime: '',
  printCount: 0,
  status: 0,
  items: [] as any[],
})

// ═══ 顶部「表头」基础字段（页面配置可显隐，17字段） ═══
const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  { key: 'docNo', label: '编号', type: 'input', disabled: true },
  { key: 'incomeType', label: '收入类型', type: 'select', options: incomeTypeOptions, required: true },
  { key: 'partnerId', label: '往来单位', type: 'select', searchBtn: '+Q' },
  { key: 'partnerCode', label: '往来编号', type: 'input', disabled: true, width: 'narrow' },
  { key: 'handlerId', label: '经手人', type: 'select', searchBtn: '+Q' },
  { key: 'departmentId', label: '部门', type: 'select', searchBtn: '+Q' },
  { key: 'incomeDate', label: '单据日期', type: 'date', required: true },
  { key: 'receiptAccount1', label: '收款账户', type: 'select' },
  { key: 'receiptAmount1', label: '收款金额', type: 'number', precision: 2, suffixBtn: '全/清' },
  { key: 'moreAccount', label: '更多账户', type: 'input', suffixBtn: '···' },
  { key: 'summary', label: '摘要', type: 'input', width: 'wide' },
  { key: 'creatorName', label: '制单人', type: 'input', disabled: true, width: 'narrow' },
  { key: 'createTime', label: '制单时间', type: 'input', disabled: true, width: 'narrow' },
  { key: 'printCount', label: '打印次数', type: 'input', disabled: true, width: 'narrow' },
]

// ═══ 页面配置（字段显隐 · 17字段） ═══
const PAGE_FIELD_DEFAULTS: Record<string, { visible: boolean; enterJump: boolean }> = {
  docNo: { visible: true, enterJump: false },
  incomeType: { visible: true, enterJump: false },
  partnerId: { visible: true, enterJump: true },
  partnerCode: { visible: true, enterJump: false },
  handlerId: { visible: true, enterJump: true },
  departmentId: { visible: false, enterJump: false },
  incomeDate: { visible: true, enterJump: true },
  receiptAccount1: { visible: true, enterJump: false },
  receiptAmount1: { visible: true, enterJump: false },
  moreAccount: { visible: true, enterJump: false },
  summary: { visible: false, enterJump: false },
  remark: { visible: true, enterJump: false },
  creatorName: { visible: true, enterJump: false },
  createTime: { visible: true, enterJump: false },
  printCount: { visible: true, enterJump: false },
  amount: { visible: true, enterJump: false },
}

const pageConfig = reactive<Record<string, { visible: boolean; enterJump: boolean }>>(
  Object.fromEntries(Object.entries(PAGE_FIELD_DEFAULTS).map(([k, v]) => [k, { ...v }]))
)

// ═══ 顶部「表头」字段渲染：仅基础字段 ═══
const basicInfoFields = computed<BasicInfoField[]>(() =>
  ALL_BASIC_INFO_FIELDS
    .filter(f => pageConfig[f.key]?.visible !== false && f.key !== 'remark')
    .map(f => ({
      ...f,
      options: f.key === 'partnerId'
        ? partnerSelectOptions.value
        : f.key === 'handlerId'
          ? userSelectOptions.value
          : f.key === 'departmentId'
            ? departmentSelectOptions.value
            : f.key === 'receiptAccount1'
              ? accountSelectOptions.value
              : (f as any).options,
      loading: ['partnerId', 'handlerId', 'departmentId', 'receiptAccount1'].includes(f.key)
        ? loadingOptions.value
        : (f as any).loading,
    }))
)

// ═══ 底部 Tab（金额字段放底部，对齐销售订单布局） ═══
const tabsConfig = computed<BillTabConfig[]>(() => [])

// ═══ 页面配置弹窗字段（17字段） ═══
const pageConfigFields = computed(() =>
  [...ALL_BASIC_INFO_FIELDS, { key: 'remark', label: '单据备注' }, { key: 'amount', label: '本单金额' }]
    .map((f, i) => ({
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
  { title: '名称', key: 'name', width: 120 },
  { title: '显示名', key: 'displayName', width: 160 },
  { title: '显示', key: 'visible', width: 70, align: 'center' as const },
  { title: '回车键跳转', key: 'enterJump', width: 100, align: 'center' as const },
]

const printSettings = reactive({
  useLastTemplate: false,
  printAfterPost: false,
})

const FORM_CONFIG_MODULE = 'other-income-doc-form'
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
    if (parsed.printSettings) {
      printSettings.useLastTemplate = !!parsed.printSettings.useLastTemplate
      printSettings.printAfterPost = !!parsed.printSettings.printAfterPost
    }
  } catch { /* API 不可用时保持默认 */ }
}

async function saveFormConfig() {
  const payload = {
    fields: Object.fromEntries(Object.entries(pageConfig).map(([k, v]) => [k, { visible: v.visible, enterJump: v.enterJump }])),
    printSettings: { ...printSettings },
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

// ═══ 配置弹窗（header 齿轮触发） ═══
const showFormConfig = ref(false)
const configModalTab = ref('pageConfig')

// ═══ Header 配置 ═══
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '其他收入单',
  orderNo: formData.docNo || '待生成',
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
function handleFieldChange(fieldKey: string, val: any) {
  if (fieldKey === 'partnerId') {
    const opt = partnerOptions.value.find((c: any) => c.id === val)
    formData.partnerName = opt?.name || ''
    formData.partnerCode = opt?.code || ''
  } else if (fieldKey === 'handlerId') {
    const opt = userOptions.value.find((u: any) => u.id === val)
    formData.handlerName = opt?.name || ''
  } else if (fieldKey === 'departmentId') {
    const opt = departmentOptions.value.find((d: any) => d.id === val)
    formData.departmentName = opt?.name || ''
  }
  onFieldValueChange()
}

function onFieldValueChange() {
  // 本单金额 = Σ收入项金额
  const total = formData.items.reduce((s: number, r: any) => s + Number(r.amount || 0), 0)
  formData.amount = total
}

// ═══ 收入项明细（5 列） ═══
const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 56, fixed: 'left' },
  { key: 'incomeNo', title: '收入编号', type: 'input', width: 150 },
  { key: 'incomeName', title: '收入名称', type: 'input', width: 220 },
  { key: 'amount', title: '金额', type: 'number', width: 130, precision: 2 },
  { key: 'remark', title: '备注', type: 'input', width: 220 },
]

const detailSummaryColumns = computed(() => {
  const total = formData.items.reduce((s: number, r: any) => s + Number(r.amount || 0), 0)
  return [
    { key: 'amount', value: total.toFixed(2), highlight: true },
  ]
})

function handleCellChange(record: any, fieldKey: string, value: any) {
  record[fieldKey] = value
  if (fieldKey === 'amount') onFieldValueChange()
}

function handleAddRow() {
  formData.items.push({
    _key: `manual_${Date.now()}`,
    incomeNo: '',
    incomeName: '',
    amount: 0,
    remark: '',
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
  { label: '本单金额', value: Number(formData.amount || 0).toFixed(2), statusLabel: statusText.value },
])

// ═══ 页脚 ═══
const saving = ref(false)
const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: `¥${(Number(formData.amount || 0)).toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: isLocked.value ? undefined : '保存草稿',
  primaryBtnText: isLocked.value ? undefined : '记账',
  draftShortcut: isLocked.value ? undefined : 'Ctrl+S',
  primaryShortcut: isLocked.value ? undefined : 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══ 提交流程 ═══
function buildPayload(): any {
  const items = formData.items.map((r: any) => ({
    incomeNo: r.incomeNo || undefined,
    incomeName: r.incomeName || undefined,
    amount: Number(r.amount || 0),
    subjectCode: r.subjectCode || undefined,
    remark: r.remark || undefined,
  }))
  return {
    incomeType: formData.incomeType,
    partnerId: formData.partnerId,
    partnerName: formData.partnerName,
    partnerCode: formData.partnerCode,
    handlerId: formData.handlerId,
    handlerName: formData.handlerName,
    departmentId: formData.departmentId,
    departmentName: formData.departmentName,
    incomeDate: formData.incomeDate,
    receiptAccount1: formData.receiptAccount1,
    receiptAmount1: Number(formData.receiptAmount1 || 0),
    accountSubjectCode: formData.accountSubjectCode || undefined,
    summary: formData.summary || undefined,
    remark: formData.remark || undefined,
    creatorName: formData.creatorName || currentUserName.value,
    items,
  }
}

async function handleSaveDraft() {
  if (!formData.incomeType) { message.warning('请选择收入类型'); return }
  if (formData.incomeType === 1 && !formData.partnerId) { message.warning('请选择往来单位'); return }
  const items = formData.items.filter((r: any) => Number(r.amount) > 0)
  if (items.length === 0) { message.warning('请录入收入项明细'); return }
  saving.value = true
  try {
    const payload = buildPayload()
    if (formData.id) {
      await otherIncomeApi.update(formData.id, payload)
    } else {
      const res: any = await otherIncomeApi.saveDraft(payload)
      if (res?.id) formData.id = res.id
    }
    message.success('保存草稿成功')
    afterSave()
  } catch (e: any) {
    message.error(e?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handlePrimarySubmit() {
  if (!formData.incomeType) { message.warning('请选择收入类型'); return }
  if (formData.incomeType === 1 && !formData.partnerId) { message.warning('请选择往来单位'); return }
  const items = formData.items.filter((r: any) => Number(r.amount) > 0)
  if (items.length === 0) { message.warning('请录入收入项明细'); return }
  saving.value = true
  try {
    let id = formData.id
    if (!id) {
      const res: any = await otherIncomeApi.saveDraft(buildPayload())
      id = res?.id
      if (id) formData.id = id
    }
    if (id) {
      await otherIncomeApi.confirm(id)
    }
    message.success('记账成功')
    afterSave()
  } catch (e: any) {
    message.error(e?.data?.message || '记账失败')
  } finally {
    saving.value = false
  }
}

function afterSave() {
  router.push('/finance/other-income-doc')
}

// ═══ 表头操作 ═══
function handlePrint() {
  message.info('打印(F8)待对接打印模板')
}
async function handleExport() {
  try {
    const res = await request.get('/erp/finance/other-income-doc/export', {
      params: { id: formData.id },
      responseType: 'blob',
    })
    const url = window.URL.createObjectURL(res)
    const a = document.createElement('a')
    a.href = url
    a.download = `其他收入单_${formData.incomeDate || ''}.xlsx`
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
    const res: any = await otherIncomeApi.getById(id)
    if (!res) return
    Object.assign(formData, {
      id: res.id,
      docNo: res.docNo || '',
      incomeType: res.incomeType ?? 1,
      partnerId: res.partnerId,
      partnerName: res.partnerName || '',
      partnerCode: res.partnerCode || '',
      handlerId: res.handlerId,
      handlerName: res.handlerName,
      departmentId: res.departmentId,
      departmentName: res.departmentName || '',
      incomeDate: res.incomeDate || dayjs().format('YYYY-MM-DD'),
      receiptAccount1: res.receiptAccount1,
      receiptAmount1: res.receiptAmount1 || 0,
      moreAccount: '',
      accountSubjectCode: res.accountSubjectCode || undefined,
      summary: res.summary || '',
      remark: res.remark || '',
      creatorName: res.creatorName || '',
      createTime: res.createTime || '',
      printCount: res.printCount || 0,
      amount: res.amount || 0,
      status: res.status ?? 0,
    })
    if (res.items?.length) {
      formData.items = res.items.map((d: any) => ({
        _key: `existing_${d.id}`,
        incomeNo: d.incomeNo || '',
        incomeName: d.incomeName || '',
        amount: Number(d.amount || 0),
        subjectCode: d.subjectCode || undefined,
        remark: d.remark || '',
      }))
      onFieldValueChange()
    }
  } catch (e) {
    console.warn('[其他收入单] 加载失败', e)
  }
}

function generateDocNo() {
  formData.docNo = `QTSRD-${dayjs().format('YYYYMMDD')}-001`
}

function formatNow(): string {
  return dayjs().format('YYYY-MM-DD HH:mm:ss')
}

const handleError = (error: Error) => {
  console.error('[其他收入单] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

onMounted(async () => {
  await loadOptions()
  loadFormConfig()
  const editId = route.query.id ? Number(route.query.id) : undefined
  if (editId && editId > 0) {
    await loadDetail(editId)
  } else {
    generateDocNo()
    formData.creatorName = currentUserName.value
    formData.handlerName = currentUserName.value
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
</style>
