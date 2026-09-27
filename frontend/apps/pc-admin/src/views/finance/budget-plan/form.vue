<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- 已审批锁定提示 -->
      <a-alert
        v-if="isLocked"
        :message="`当前单据状态为「${statusText}」，已进入审批流程，内容不可修改`"
        type="warning"
        show-icon
        banner
        style="flex-shrink:0"
      />

      <BillFormPage
        v-model="formData"
        :header="headerConfig"
        :basic-info-fields="basicInfoFields"
        :summary="summaryConfig"
        :footer="footerConfig"
        @action="handleAction"
        @field-change="handleFieldChange"
        @search-btn="handleSearchBtn"
        @draft="handleSaveDraft"
        @submit="handlePrimarySubmit"
      >
        <!-- ═══ 预算科目明细表 ═══ -->
        <template #detail-table="{ onExpandChange }">
          <BillDetailTable
            :columns="detailColumns"
            v-model:data-source="formData.products"
            :summary-columns="tableSummaryColumns"
            :storage-key="'budget-plan-form-columns'"
            @cell-change="handleCellChange"
            @expand-change="onExpandChange"
          >
            <template #actionCell="{ index, empty }">
              <template v-if="!empty">
                <a-space :size="2">
                  <a-button
                    type="link"
                    size="small"
                    class="action-add-btn"
                    @click="handleInsertRow(index)"
                  >
                    <PlusCircleOutlined />
                  </a-button>
                  <a-button
                    type="link"
                    size="small"
                    class="action-del-btn"
                    @click="handleRemoveRow(index)"
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
                    @click="handleAddRow()"
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

        <!-- ═══ 备注 + 单据信息 ═══ -->
        <template #bottom-extra>
          <div class="remark-section">
            <div class="remark-row">
              <span class="remark-label">摘要</span>
              <a-input
                v-model:value="formData.description"
                size="small"
                class="remark-input"
                placeholder="请输入摘要"
                :disabled="isLocked"
              />
            </div>
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
            <span class="doc-info-item">单据状态 <a-tag :color="statusColor">{{ statusText }}</a-tag></span>
            <span class="doc-info-item">制单人 <a-tag color="blue">{{ formData.creatorName || currentUserName || '系统' }}</a-tag></span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
            <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
            <span
              v-if="formData.auditorName"
              class="doc-info-item"
            >审核人 {{ formData.auditorName }}</span>
            <span
              v-if="formData.auditTime"
              class="doc-info-item"
            >审核时间 {{ formData.auditTime }}</span>
          </div>
        </template>
      </BillFormPage>

      <!-- ═══ 配置弹窗：页面配置 / 录单默认值 / 打印设置（三 Tab） ═══ -->
      <a-modal
        v-model:open="showFormConfig"
        title="配置"
        :width="820"
        :footer="null"
        destroy-on-close
      >
        <a-tabs
          v-model:active-key="configModalTab"
          size="small"
        >
          <!-- Tab 1: 页面配置 -->
          <a-tab-pane
            key="pageConfig"
            tab="页面配置"
          >
            <p class="config-hint">
              勾选后自动保存，该设置对所有操作员生效
            </p>
            <a-table
              :columns="pageConfigTableColumns"
              :data-source="pageConfigFields"
              :pagination="false"
              size="small"
              row-key="key"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'displayName'">
                  <a-input
                    v-model:value="record.displayName"
                    size="small"
                    @blur="saveFormConfig"
                  />
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

          <!-- Tab 2: 录单默认值 -->
          <a-tab-pane
            key="defaults"
            tab="录单默认值"
          >
            <p class="config-hint">
              新增单据时的默认值（仅对新建生效）
            </p>
            <a-form
              layout="horizontal"
              :label-col="{ span: 6 }"
              :wrapper-col="{ span: 16 }"
            >
              <a-form-item label="默认财政年度">
                <a-select
                  v-model:value="formDefaults.fiscalYear"
                  size="small"
                  style="width:100%"
                  @change="saveFormConfig"
                >
                  <a-select-option
                    v-for="y in YEAR_OPTIONS"
                    :key="y.value"
                    :value="y.value"
                  >
                    {{ y.label }}
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="默认部门">
                <a-select
                  v-model:value="formDefaults.departmentId"
                  size="small"
                  allow-clear
                  style="width:100%"
                  @change="onDefaultDepartmentChange"
                >
                  <a-select-option
                    v-for="d in departmentOptions"
                    :key="d.id"
                    :value="d.id"
                  >
                    {{ d.name }}
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="默认经手人">
                <a-select
                  v-model:value="formDefaults.handlerId"
                  size="small"
                  allow-clear
                  style="width:100%"
                  @change="onDefaultHandlerChange"
                >
                  <a-select-option
                    v-for="u in optionRefs.users"
                    :key="u.id"
                    :value="u.id"
                  >
                    {{ u.name }}
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="默认明细行数">
                <a-input-number
                  v-model:value="formDefaults.detailRows"
                  :precision="0"
                  :min="1"
                  :max="50"
                  size="small"
                  style="width:100%"
                  @change="saveFormConfig"
                />
              </a-form-item>
              <a-form-item label="默认摘要">
                <a-input
                  v-model:value="formDefaults.description"
                  size="small"
                  style="width:100%"
                  @blur="saveFormConfig"
                />
              </a-form-item>
            </a-form>
          </a-tab-pane>

          <!-- Tab 3: 打印设置 -->
          <a-tab-pane
            key="printSettings"
            tab="打印设置"
          >
            <a-form
              layout="horizontal"
              :label-col="{ span: 6 }"
              :wrapper-col="{ span: 16 }"
            >
              <a-form-item label="打印模板">
                <a-select
                  v-model:value="formData.printTemplate"
                  size="small"
                  style="width:100%"
                  @change="saveFormConfig"
                >
                  <a-select-option value="standard">
                    标准模板
                  </a-select-option>
                  <a-select-option value="simple">
                    简化模板
                  </a-select-option>
                  <a-select-option value="detailed">
                    详细模板
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="打印份数">
                <a-input-number
                  v-model:value="formData.printCopies"
                  :precision="0"
                  :min="1"
                  size="small"
                  style="width:100%"
                  @change="saveFormConfig"
                />
              </a-form-item>
              <a-form-item label="纸张大小">
                <a-select
                  v-model:value="formData.printPaperSize"
                  size="small"
                  style="width:100%"
                  @change="saveFormConfig"
                >
                  <a-select-option value="A4">
                    A4
                  </a-select-option>
                  <a-select-option value="A5">
                    A5
                  </a-select-option>
                  <a-select-option value="B5">
                    B5
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="打印选项">
                <a-checkbox
                  v-model:checked="formData.printAlwaysLastTemplate"
                  @change="saveFormConfig"
                >
                  始终使用最后一次打印的模板，打印时不再选择
                </a-checkbox>
                <a-checkbox
                  v-model:checked="formData.printAfterSubmit"
                  @change="saveFormConfig"
                >
                  提交后立即打印
                </a-checkbox>
              </a-form-item>
            </a-form>
          </a-tab-pane>
        </a-tabs>
      </a-modal>
    </PageContainer>
  <!-- 打印：按单据打印 -->
  <PrintDialog
    ref="printDialogRef"
    page-code="finance-budget-plan"
    :document-id="printData.id"
    :print-data="printData"
  />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ClockCircleOutlined, PrinterOutlined, SettingOutlined,
  MinusCircleOutlined, PlusCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { annualBudgetApi, budgetTemplateApi } from '@/api/budget'
import { accountSubjectApi } from '@/api/finance'
import { userPageConfigApi } from '@/api/erp'
import optionsApi from '@/api/options'
import { useUserStore } from '@/stores/user'
import PrintDialog from '@/components/PrintDialog/index.vue'

defineOptions({ name: 'BudgetPlanForm' })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
const currentUserId = computed(() => userStore?.userInfo?.id)

// ═══ 状态枚举 ═══
const STATUS_MAP: Record<string, { text: string; color: string }> = {
  draft: { text: '草稿', color: 'default' },
  submitted: { text: '待审批', color: 'orange' },
  approved: { text: '已审批', color: 'blue' },
  rejected: { text: '已驳回', color: 'red' },
  executing: { text: '执行中', color: 'green' },
  closed: { text: '已关闭', color: 'default' },
}

const currentYear = new Date().getFullYear()
const YEAR_OPTIONS = [0, 1, 2, 3].map(i => {
  const y = currentYear - i
  return { label: `${y}年`, value: y }
})

function currentOperator() {
  return {
    id: userStore?.userId || userStore?.userInfo?.id || 0,
    name: currentUserName.value || '系统',
  }
}

function toDateStr(v: any): string {
  if (!v) return ''
  return String(v).slice(0, 10)
}

// 本单金额 = Σ预算金额
const billTotal = computed(() =>
  formData.products.reduce((s: number, p: any) => s + (Number(p.budgetAmount) || 0), 0)
)

// ═══ 明细行：剩余额度回算（预算 − 已执行 − 冻结） ═══
function calcRemaining(row: any) {
  const remain = (Number(row.budgetAmount) || 0) - (Number(row.usedAmount) || 0) - (Number(row.frozenAmount) || 0)
  row.remainingAmount = Number(remain.toFixed(2))
  const base = Number(row.budgetAmount) || 0
  row.executionRate = base > 0 ? Number((((Number(row.usedAmount) || 0) / base) * 100).toFixed(2)) : 0
}

// ═══ useBillForm ═══
async function createWithStatus(data: any) {
  const { status, ...payload } = data
  const res: any = await annualBudgetApi.save({ ...payload })
  const created = res?.data || res
  if (status >= 1 && created?.id) {
    await annualBudgetApi.submit(created.id)
  }
  return created
}

async function updateWithStatus(id: number, data: any) {
  const { status, ...payload } = data
  const res: any = await annualBudgetApi.save({ ...payload, id })
  if (status >= 1 && id) {
    await annualBudgetApi.submit(id)
  }
  return res?.data || res
}

const {
  formData,
  loadingOptions,
  saving,
  optionRefs,
  effectiveMode,
  handleAddProduct,
  handleRemoveProduct,
  handleFieldChange: baseFieldChange,
  handleSaveDraft,
  handleSubmit,
  loadDetail,
} = useBillForm({
  billPrefix: 'YSD',
  api: {
    create: createWithStatus,
    update: updateWithStatus,
    getById: async (id: number) => {
      const res: any = await annualBudgetApi.getById(id)
      const data = res?.data || res || {}
      return {
        ...data,
        orderNo: data.budgetNo || '',
        date: toDateStr(data.budgetDate) || '',
        fiscalYear: data.fiscalYear,
        departmentId: data.departmentId,
        departmentName: data.departmentName || '',
        handlerId: data.handlerId,
        handlerName: data.handlerName || '',
        templateId: data.templateId,
        templateName: data.templateName || '',
        items: (data.items || []).map((d: any, i: number) => ({
          id: d.id || `detail-${i}`,
          lineNo: d.lineNo || i + 1,
          subjectId: d.subjectId,
          subjectCode: d.subjectCode || '',
          subjectName: d.subjectName || '',
          subjectType: d.subjectType || '',
          budgetAmount: Number(d.budgetAmount) || 0,
          usedAmount: Number(d.usedAmount) || 0,
          frozenAmount: Number(d.frozenAmount) || 0,
          remainingAmount: Number(d.remainingAmount) || 0,
          executionRate: Number(d.executionRate) || 0,
          remark: d.remark || '',
        })),
      }
    },
  },
  redirectPath: '/finance/budget-plan/index',
  codeApiPath: '/erp/budget/annual/next-no',
  optionTypes: ['users'],
  fields: [
    { key: 'fiscalYear', label: '财政年度', type: 'number', required: true },
    { key: 'departmentId', label: '部门', type: 'select', required: true },
    { key: 'date', label: '编制日期', type: 'date', required: true },
  ],
  productDefaults: {
    subjectId: undefined, subjectCode: '', subjectName: '', subjectType: '',
    budgetAmount: 0, usedAmount: 0, frozenAmount: 0, remainingAmount: 0, executionRate: 0, remark: '',
  },
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'departmentId') {
      const d = departmentOptions.value.find((x: any) => x.id === val)
      fd.departmentName = d?.name || ''
    }
    if (fieldKey === 'handlerId') {
      const u = optionRefs.users.find((x: any) => x.id === val)
      fd.handlerName = u?.name || ''
    }
    if (fieldKey === 'templateId') {
      const t = templateOptions.value.find((x: any) => x.id === val)
      fd.templateName = t?.templateName || t?.name || ''
      applyTemplate(val)
    }
  },
  transformPayload: (fd) => ({
    id: fd.id || undefined,
    budgetNo: fd.orderNo || undefined,
    fiscalYear: Number(fd.fiscalYear) || undefined,
    departmentId: fd.departmentId ? String(fd.departmentId) : undefined,
    departmentName: fd.departmentName || undefined,
    budgetDate: fd.date || undefined,
    handlerId: fd.handlerId ? Number(fd.handlerId) : undefined,
    handlerName: fd.handlerName || undefined,
    templateId: fd.templateId ? Number(fd.templateId) : undefined,
    templateName: fd.templateName || undefined,
    description: fd.description || undefined,
    remark: fd.remark || undefined,
    creatorName: fd.creatorName || currentUserName.value || undefined,
    totalAmount: Number(billTotal.value.toFixed(2)),
    items: (fd.products || [])
      .filter((p: any) => (Number(p.budgetAmount) > 0) || (p.subjectName || '').trim())
      .map((p: any, i: number) => ({
        lineNo: i + 1,
        subjectId: p.subjectId || undefined,
        subjectCode: p.subjectCode || '',
        subjectName: p.subjectName || '',
        subjectType: p.subjectType || '',
        budgetAmount: Number(p.budgetAmount) || 0,
        remark: p.remark || '',
      })),
  }),
})

// 初始化默认值
if (!formData.date) formData.date = new Date().toISOString().slice(0, 10)
if (formData.fiscalYear === undefined || formData.fiscalYear === null) formData.fiscalYear = currentYear
for (const key of ['departmentName', 'handlerName', 'templateName', 'description', 'remark', 'creatorName', 'createTime', 'auditorName', 'auditTime']) {
  if (formData[key] === undefined) formData[key] = ''
}
for (const key of ['printCount', 'printCopies']) {
  if (formData[key] === undefined) formData[key] = 0
}
for (const key of ['printAlwaysLastTemplate', 'printAfterSubmit']) {
  if (formData[key] === undefined) formData[key] = false
}
if (formData.status === undefined) formData.status = 'draft'

// 同步「本单金额」表头字段（只读展示）：明细预算金额变化时联动
watch(billTotal, (val) => {
  formData.billTotal = Number(Number(val).toFixed(2))
}, { immediate: true })

// ═══ 计算属性 ═══
const currentStatus = computed(() => String(formData.status ?? 'draft'))
const statusText = computed(() => STATUS_MAP[currentStatus.value]?.text || '草稿')
const statusColor = computed(() => STATUS_MAP[currentStatus.value]?.color || 'default')
const isLocked = computed(() =>
  effectiveMode.value === 'edit' && !['draft', 'rejected'].includes(currentStatus.value)
)

// ═══ 页眉配置 ═══
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '预算编制',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ],
}))

// ═══ 基本信息字段（14 个，默认显示 11 隐藏 3） ═══
const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  { key: 'orderNo', label: '编号', type: 'input', inlineLabel: true, width: 200, disabled: true },
  { key: 'fiscalYear', label: '财政年度', type: 'select', required: true, inlineLabel: true, width: 130 },
  { key: 'departmentId', label: '部门', type: 'select', required: true, inlineLabel: true, width: 200, searchBtn: '+Q', loading: true },
  { key: 'date', label: '编制日期', type: 'date', required: true, inlineLabel: true, width: 160 },
  { key: 'handlerId', label: '经手人', type: 'select', inlineLabel: true, width: 180, searchBtn: '+Q', loading: true },
  { key: 'templateId', label: '预算模板', type: 'select', inlineLabel: true, width: 200 },
  { key: 'description', label: '摘要', type: 'input', inlineLabel: true, width: 260 },
  { key: 'remark', label: '单据备注', type: 'input', inlineLabel: true, width: 260 },
  { key: 'creatorName', label: '制单人', type: 'display', inlineLabel: true, width: 100 },
  { key: 'createTime', label: '制单时间', type: 'input', inlineLabel: true, width: 200, disabled: true },
  { key: 'auditorName', label: '审核人', type: 'display', inlineLabel: true, width: 100 },
  { key: 'auditTime', label: '审核时间', type: 'input', inlineLabel: true, width: 200, disabled: true },
  { key: 'printCount', label: '打印次数', type: 'display', inlineLabel: true, width: 90 },
  { key: 'billTotal', label: '本单金额', type: 'number', inlineLabel: true, width: 150, precision: 2, disabled: true },
]

const DEFAULT_HIDDEN_FIELDS = ['templateId', 'auditorName', 'auditTime']
type FieldConfig = { visible: boolean; enterJump: boolean }
const pageConfig = reactive<Record<string, FieldConfig>>({})
for (const f of ALL_BASIC_INFO_FIELDS) {
  pageConfig[f.key] = {
    visible: !DEFAULT_HIDDEN_FIELDS.includes(f.key),
    enterJump: ['select', 'date', 'number', 'input'].includes(f.type),
  }
}

// ═══ 录单默认值 ═══
const formDefaults = reactive({
  fiscalYear: currentYear,
  departmentId: undefined as any,
  departmentName: '',
  handlerId: undefined as any,
  handlerName: '',
  detailRows: 5,
  description: '',
})

const FORM_CONFIG_MODULE = 'budget-plan'
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
      Object.assign(formDefaults, {
        fiscalYear: parsed.defaults.fiscalYear ?? currentYear,
        departmentId: parsed.defaults.departmentId,
        departmentName: parsed.defaults.departmentName || '',
        handlerId: parsed.defaults.handlerId,
        handlerName: parsed.defaults.handlerName || '',
        detailRows: parsed.defaults.detailRows ?? 5,
        description: parsed.defaults.description || '',
      })
      Object.assign(formData, {
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
      fiscalYear: formDefaults.fiscalYear,
      departmentId: formDefaults.departmentId,
      departmentName: formDefaults.departmentName,
      handlerId: formDefaults.handlerId,
      handlerName: formDefaults.handlerName,
      detailRows: formDefaults.detailRows,
      description: formDefaults.description,
      printTemplate: formData.printTemplate,
      printCopies: formData.printCopies,
      printPaperSize: formData.printPaperSize,
    },
  }
  try {
    await userPageConfigApi.save(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE, JSON.stringify(payload))
  } catch { /* 静默失败 */ }
}

const showFormConfig = ref(false)
const configModalTab = ref('pageConfig')
const departmentOptions = ref<any[]>([])
const subjectOptions = ref<any[]>([])
const templateOptions = ref<any[]>([])

async function loadExtraOptions() {
  try {
    departmentOptions.value = await optionsApi.getDepartments()
  } catch {
    departmentOptions.value = []
  }
  try {
    const res: any = await accountSubjectApi.getList({ pageSize: 999 })
    const body = res?.data ?? res
    const list = Array.isArray(body) ? body : (body?.records || [])
    subjectOptions.value = list.map((s: any) => ({
      raw: s,
      label: `${s.code || s.subjectCode || ''} ${s.name || s.subjectName || ''}`.trim(),
      value: s.name || s.subjectName || '',
    }))
  } catch {
    subjectOptions.value = []
  }
}

async function loadTemplateOptions(year?: number) {
  try {
    const res: any = await budgetTemplateApi.listByYear(year || formData.fiscalYear || currentYear)
    const body = res?.data ?? res
    templateOptions.value = Array.isArray(body) ? body : []
  } catch {
    templateOptions.value = []
  }
}

/** 选择模板后带出明细（仅新建且明细为空时） */
async function applyTemplate(templateId: any) {
  if (!templateId) return
  try {
    const res: any = await budgetTemplateApi.getById(Number(templateId))
    const tpl = res?.data || res
    const items = tpl?.items || []
    if (items.length === 0) {
      message.info('该模板下暂无预算科目')
      return
    }
    formData.products = items.map((d: any, i: number) => ({
      id: `detail-${i}`,
      lineNo: i + 1,
      subjectId: d.subjectId,
      subjectCode: d.subjectCode || '',
      subjectName: d.subjectName || '',
      subjectType: d.subjectType || '',
      budgetAmount: Number(d.budgetAmount) || 0,
      usedAmount: 0,
      frozenAmount: 0,
      remainingAmount: Number(d.budgetAmount) || 0,
      executionRate: 0,
      remark: d.remark || '',
    }))
    message.success(`已带入 ${items.length} 条预算科目`)
  } catch {
    message.warning('预算模板明细加载失败')
  }
}

function onDefaultDepartmentChange(val: any) {
  const d = departmentOptions.value.find((x: any) => x.id === val)
  formDefaults.departmentName = d?.name || ''
  saveFormConfig()
}

function onDefaultHandlerChange(val: any) {
  const u = optionRefs.users.find((x: any) => x.id === val)
  formDefaults.handlerName = u?.name || ''
  saveFormConfig()
}

const basicInfoFields = computed<BasicInfoField[]>(() =>
  ALL_BASIC_INFO_FIELDS
    .filter(f => pageConfig[f.key]?.visible !== false)
    .map(f => {
      return {
        ...f,
        options: f.key === 'fiscalYear'
          ? YEAR_OPTIONS.map(y => ({ label: y.label, value: y.value }))
          : f.key === 'departmentId'
            ? (departmentOptions.value || []).map((d: any) => ({ label: d.name, value: d.id }))
            : f.key === 'handlerId'
              ? (optionRefs.users || []).map((u: any) => ({ label: u.name, value: u.id }))
              : f.key === 'templateId'
                ? (templateOptions.value || []).map((t: any) => ({ label: t.templateName || t.name, value: t.id }))
                : (f as any).options,
        loading: (f.key === 'departmentId' || f.key === 'handlerId')
          ? loadingOptions.value
          : (f as any).loading,
        searchBtn: (f.key === 'departmentId' || f.key === 'handlerId')
          ? '+Q'
          : (f as any).searchBtn,
      }
    })
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
  { title: '名称', key: 'name', width: 120 },
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

// ═══ 摘要面板 ═══
const summaryConfig = computed<SummaryRow[]>(() => [
  { label: '本单金额', value: `¥${billTotal.value.toFixed(2)}`, statusLabel: statusText.value },
  { label: '预算科目数', value: `${formData.products.length} 项` },
])

// ═══ 页脚 ═══
const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: `¥${billTotal.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: isLocked.value ? undefined : '保存草稿',
  draftShortcut: isLocked.value ? undefined : 'Ctrl+S',
  primaryBtnText: isLocked.value ? undefined : '提交审批',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══ 预算科目明细列（11 列） ═══
const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 70, fixed: 'left' },
  { key: 'subjectCode', title: '科目编号', type: 'input', width: 140 },
  {
    key: 'subjectName',
    title: '预算科目',
    type: 'select',
    width: 240,
    searchable: true,
    options: () => subjectOptions.value.map((s: any) => ({ label: s.label, value: s.value })),
  },
  { key: 'subjectType', title: '科目类别', type: 'input', width: 110 },
  { key: 'budgetAmount', title: '预算金额', type: 'number', width: 140, precision: 2, required: true },
  { key: 'usedAmount', title: '已执行', type: 'number', width: 130, precision: 2, readonly: true },
  { key: 'frozenAmount', title: '冻结金额', type: 'number', width: 120, precision: 2, readonly: true },
  { key: 'remainingAmount', title: '剩余额度', type: 'number', width: 130, precision: 2, readonly: true },
  { key: 'executionRate', title: '执行率', type: 'number', width: 90, precision: 2, readonly: true },
  { key: 'remark', title: '备注', type: 'input', width: 180 },
]

const tableSummaryColumns = computed(() => [
  { key: 'budgetAmount', value: Number(billTotal.value.toFixed(2)), highlight: true },
] as { key: string; value: number; highlight?: boolean }[])

// ═══ 事件处理 ═══
function handleAddRow() {
  handleAddProduct()
}
function handleRemoveRow(index: number) {
  handleRemoveProduct(index)
}
function handleInsertRow(index: number) {
  handleAddProduct()
  const item = formData.products.pop()
  if (item) formData.products.splice(index + 1, 0, item)
}

function handleCellChange(record: any, fieldKey: string, _value: any) {
  if (fieldKey === 'subjectName') {
    const subject = subjectOptions.value.find((s: any) => s.value === record.subjectName)
    if (subject?.raw) {
      record.subjectId = subject.raw.id
      record.subjectCode = subject.raw.code || subject.raw.subjectCode || ''
      record.subjectType = subject.raw.typeName || subject.raw.subjectType || ''
    }
  }
  if (fieldKey === 'budgetAmount' || fieldKey === 'usedAmount' || fieldKey === 'frozenAmount') {
    calcRemaining(record)
  }
}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
  if (fieldKey === 'fiscalYear') {
    loadTemplateOptions(Number(val))
  }
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  message.info(`${fieldKey} 快速查询功能暂不可用`)
}

async function handlePrimarySubmit() {
  if (isLocked.value) return
  handleSubmit()
}

async function handleAction(actionKey: string) {
  switch (actionKey) {
    case 'history':
      router.push('/finance/budget-plan/index')
      break
    case 'config':
      showFormConfig.value = true
      break
    case 'print':
      handlePrint()
      break
    default:
      message.info(`${actionKey} 功能暂不可用`)
  }
}

// ═══ 打印（按单据打印） ═══
// 年度预算单：后端已为 pageCode='finance-budget-plan' 登记装配器并有已发布模板，
// 页面只给单据主键 —— 取数 / 挑模板 / 渲染都在服务端。
// 原先是 window.print() —— 打出来是整个后台界面（菜单、工具栏、翻页都跟着上纸）。
const printDialogRef = ref<InstanceType<typeof PrintDialog> | null>(null)
const printData = ref<Record<string, any>>({})

function handlePrint() {
  if (!formData.id) {
    message.warning('请先保存单据后再打印')
    return
  }
  printData.value = { id: formData.id }
  printDialogRef.value?.open?.()
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function handleError(err: any) {
  console.warn('[预算编制] ErrorBoundary:', err)
}

// ═══ 本单金额同步 ═══
function syncBillTotal() {
  formData.billTotal = Number(billTotal.value.toFixed(2))
}

// ═══ 生命周期 ═══
onMounted(async () => {
  await loadFormConfig()
  await loadExtraOptions()
  await loadTemplateOptions(formData.fiscalYear)

  if (effectiveMode.value !== 'edit' && formData.products.length === 0) {
    // 录单默认值
    if (formDefaults.fiscalYear) formData.fiscalYear = formDefaults.fiscalYear
    if (formDefaults.departmentId) {
      formData.departmentId = formDefaults.departmentId
      formData.departmentName = formDefaults.departmentName
    }
    if (formDefaults.handlerId) {
      formData.handlerId = formDefaults.handlerId
      formData.handlerName = formDefaults.handlerName
    } else if (currentUserId.value) {
      formData.handlerId = currentUserId.value
      const u = optionRefs.users.find((x: any) => x.id === currentUserId.value)
      formData.handlerName = u?.name || currentUserName.value || ''
    }
    if (formDefaults.description) formData.description = formDefaults.description
    formData.creatorName = currentUserName.value || ''
    const rows = Math.max(1, Number(formDefaults.detailRows) || 5)
    for (let i = 0; i < rows; i++) handleAddRow()
  }
  syncBillTotal()
})

// 同路由内 ?id 切换（列表「查看/修改」→ 当前已在表单页）时，组件不会重新挂载，需手动重载详情
watch(() => route.query.id, async (val, oldVal) => {
  if (val && String(val) !== String(oldVal ?? '')) {
    // id 原样透传（雪花 ID 超出 JS Number 安全范围，不做 Number 转换）
    await loadDetail(val as any)
    syncBillTotal()
  }
})
</script>

<style scoped>
.action-add-btn { color: #1890ff; padding: 0; font-size: 14px; }
.action-del-btn { color: #ff4d4f; padding: 0; font-size: 14px; }
.remark-section { padding: 4px 0; }
.remark-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.remark-label { font-size: 12px; color: #595959; white-space: nowrap; min-width: 60px; }
.remark-input { flex: 1; }
.doc-info-row { display: flex; align-items: center; gap: 16px; padding: 6px 0; font-size: 12px; color: #8c8c8c; border-top: 1px solid #f0f0f0; flex-wrap: wrap; }
.config-hint { font-size: 12px; color: #8c8c8c; margin-bottom: 8px; }
</style>
