<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- 已记账锁定提示 -->
      <a-alert
        v-if="isLocked"
        :message="`当前单据状态为「${statusText}」，已进入记账流程，内容不可修改`"
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
        <!-- ═══ 费用项明细表（5 列） ═══ -->
        <template #detail-table="{ onExpandChange }">
          <BillDetailTable
            :columns="detailColumns"
            v-model:data-source="formData.products"
            :summary-columns="tableSummaryColumns"
            :storage-key="'expense-doc-form-columns'"
            @cell-change="handleCellChange"
            @expand-change="onExpandChange"
          >
            <template #actionCell="{ index, empty }">
              <template v-if="!empty">
                <a-space :size="2">
                  <a-button type="link" size="small" class="action-add-btn" @click="handleInsertRow(index)"><PlusCircleOutlined /></a-button>
                  <a-button type="link" size="small" class="action-del-btn" @click="handleRemoveRow(index)"><MinusCircleOutlined /></a-button>
                </a-space>
              </template>
              <template v-else>
                <a-space :size="2">
                  <a-button type="link" size="small" class="action-add-btn" @click="handleAddRow()"><PlusCircleOutlined /></a-button>
                  <a-button type="link" size="small" class="action-del-btn" disabled><MinusCircleOutlined /></a-button>
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
              <a-input v-model:value="formData.summary" size="small" class="remark-input" placeholder="请输入摘要" :disabled="isLocked" />
            </div>
            <div class="remark-row">
              <span class="remark-label">单据备注</span>
              <a-input v-model:value="formData.remark" size="small" class="remark-input" placeholder="请输入单据备注" :disabled="isLocked" />
            </div>
          </div>
          <div class="doc-info-row">
            <span class="doc-info-item">单据状态 <a-tag :color="statusColor">{{ statusText }}</a-tag></span>
            <span class="doc-info-item">审批状态 <a-tag :color="approvalStatusColor">{{ approvalStatusText }}</a-tag></span>
            <span v-if="currentApprovalStatus === '1'" class="doc-info-item">当前审批人 <a-tag color="orange">{{ formData.currentApproverName || '-' }}</a-tag></span>
            <span class="doc-info-item">制单人 <a-tag color="blue">{{ formData.creatorName || currentUserName || '系统' }}</a-tag></span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
            <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
            <span v-if="formData.bookkeepingTime" class="doc-info-item">记账时间 {{ formData.bookkeepingTime }}</span>
          </div>
        </template>
      </BillFormPage>

      <!-- ═══ 配置弹窗：页面配置/打印设置 ═══ -->
      <a-modal v-model:open="showFormConfig" title="配置" :width="760" :footer="null" destroy-on-close>
        <a-tabs v-model:active-key="configModalTab" size="small">
          <!-- Tab 1: 页面配置 -->
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
          <!-- Tab 2: 打印设置 -->
          <a-tab-pane key="printSettings" tab="打印设置">
            <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-form-item label="打印模板">
                <a-select v-model:value="formData.printTemplate" size="small" style="width:100%" @change="saveFormConfig">
                  <a-select-option value="standard">标准模板</a-select-option>
                  <a-select-option value="simple">简化模板</a-select-option>
                  <a-select-option value="detailed">详细模板</a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="打印选项">
                <a-checkbox v-model:checked="formData.printAlwaysLastTemplate" @change="saveFormConfig">始终使用最后一次打印的模板，打印时不再选择</a-checkbox>
                <br />
                <a-checkbox v-model:checked="formData.printAfterSubmit" @change="saveFormConfig">记账后立即打印</a-checkbox>
              </a-form-item>
            </a-form>
          </a-tab-pane>
        </a-tabs>
      </a-modal>

      <!-- ═══ 提交审批弹窗（与费用审批工作台共用） ═══ -->
      <SubmitApprovalModal
        v-model:open="showSubmitApproval"
        :record="approvalRecord"
        @success="handleSubmitApprovalSuccess"
      />

      <!-- ═══ 更多账户弹窗（付款账户2-4） ═══ -->
      <a-modal v-model:open="showMoreAccount" title="更多账户" :width="720" ok-text="确定" cancel-text="取消" @ok="handleMoreAccountOk">
        <div class="more-account-hint">最多可配置 3 个（付款账户2/3/4），本单金额 = Σ费用项金额 = Σ付款金额。</div>
        <a-table :columns="moreAccountColumns" :data-source="moreAccounts" :pagination="false" size="small" row-key="rowKey">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'payAccount'">
              <a-select v-model:value="record.accountId" show-search size="small" style="width:100%" :options="accountOptions" placeholder="选择付款账户" @change="(v:any) => handleMoreAccountChange(record, v)" />
            </template>
            <template v-if="column.key === 'payAmount'">
              <a-input-number v-model:value="record.amount" size="small" :min="0" :precision="2" style="width:100%" placeholder="金额" />
            </template>
            <template v-if="column.key === 'action'">
              <a-button type="link" size="small" @click="removeMoreAccount(record)">删除</a-button>
            </template>
          </template>
        </a-table>
      </a-modal>
    </PageContainer>
  <!-- 打印：按单据打印 -->
  <PrintDialog
    ref="printDialogRef"
    page-code="finance-expense-doc"
    :document-id="printData.id"
    :print-data="printData"
  />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, ref, reactive, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ClockCircleOutlined, PrinterOutlined, SettingOutlined, AuditOutlined,
  MinusCircleOutlined, PlusCircleOutlined,
} from '@ant-design/icons-vue'
import SubmitApprovalModal from '@/views/finance/expense-approval/SubmitApprovalModal.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { expenseDocApi } from '@/api/finance'
import { userPageConfigApi } from '@/api/erp'
import optionsApi from '@/api/options'
import { useUserStore } from '@/stores/user'
import PrintDialog from '@/components/PrintDialog/index.vue'

defineOptions({ name: 'ExpenseDocForm' })

const router = useRouter()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
const currentUserId = computed(() => userStore?.userInfo?.id)

// ═══ 状态枚举（0草稿 1已记账 2已取消） ═══
const STATUS_MAP: Record<string, { text: string; color: string }> = {
  '0': { text: '草稿', color: 'default' },
  '1': { text: '已记账', color: 'blue' },
  '2': { text: '已取消', color: 'red' },
}

// ═══ 审批状态枚举（0未提交 1审批中 2审批通过 3审批驳回） ═══
const APPROVAL_STATUS_MAP: Record<string, { text: string; color: string }> = {
  '0': { text: '未提交', color: 'default' },
  '1': { text: '审批中', color: 'orange' },
  '2': { text: '审批通过', color: 'green' },
  '3': { text: '审批驳回', color: 'red' },
}

function currentOperator() {
  return {
    id: userStore?.userId || userStore?.userInfo?.id || 0,
    name: currentUserName.value || '系统',
  }
}

function toDateStr(v: any): string {
  if (!v) return ''
  if (typeof v === 'string') return v.slice(0, 10)
  return String(v).slice(0, 10)
}

// 本单金额 = Σ费用项明细金额
const billTotal = computed(() =>
  formData.products.reduce((s: number, p: any) => s + (Number(p.amount) || 0), 0)
)

// ═══ useBillForm ═══
async function createWithStatus(data: any) {
  const { status, ...payload } = data
  const res: any = await expenseDocApi.saveDraft({ ...payload })
  const created = res?.data || res
  if (status >= 1 && created?.id) {
    const u = currentOperator()
    await expenseDocApi.confirm(created.id, u.id, u.name)
  }
  return created
}

async function updateWithStatus(id: number, data: any) {
  const { status, ...payload } = data
  const res: any = await expenseDocApi.update({ ...payload, id })
  if (status >= 1) {
    const u = currentOperator()
    await expenseDocApi.confirm(id, u.id, u.name)
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
} = useBillForm({
  billPrefix: 'YBFYD',
  api: {
    create: createWithStatus,
    update: updateWithStatus,
    getById: async (id: number | string) => {
      const res: any = await expenseDocApi.getById(id)
      const data = res?.data || res || {}
      return {
        ...data,
        orderNo: data.docNo || '',
        date: toDateStr(data.docDate) || '',
        partnerId: data.partnerId,
        partnerCode: data.partnerCode || '',
        partnerName: data.partnerName || '',
        handlerId: data.handlerId,
        handlerName: data.handlerName || '',
        deptId: data.deptId || undefined,
        deptName: data.deptName || '',
        payAccountId: data.payAccountId,
        payAccountName: data.payAccountName || '',
        payAmount: data.payAmount ?? 0,
        summary: data.summary || '',
        remark: data.remark || '',
        creatorName: data.creatorName || '',
        createTime: data.createTime || '',
        printCount: data.printCount || 0,
        approvalStatus: data.approvalStatus ?? 0,
        approvalLevel: data.approvalLevel ?? 0,
        totalApprovalLevel: data.totalApprovalLevel ?? 3,
        currentApproverName: data.currentApproverName || '',
        submitTime: data.submitTime || '',
        billTotal: data.totalAmount ?? 0,
        items: (data.items || []).map((d: any, i: number) => ({
          id: d.id || `detail-${i}`,
          lineNo: d.lineNo || i + 1,
          expenseCode: d.expenseCode || '',
          expenseName: d.expenseName || '',
          amount: d.amount ?? 0,
          remark: d.remark || '',
        })),
      }
    },
  },
  redirectPath: '/finance/expense-doc/index',
  codeApiPath: '/erp/finance/expense-doc/next-no',
  optionTypes: ['customers', 'users'],
  fields: [
    { key: 'expenseType', label: '费用类型', type: 'select', required: true },
    { key: 'partnerId', label: '往来单位', type: 'select' },
    { key: 'handlerId', label: '经手人', type: 'select', required: true },
    { key: 'date', label: '单据日期', type: 'date', required: true },
  ],
  productDefaults: {
    expenseCode: '', expenseName: '', amount: 0, remark: '',
  },
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'expenseType') {
      // 往来单位费用 ⇄ 内部费用
      if (Number(val) === 1) {
        fd.partnerId = undefined
        fd.partnerCode = ''
        fd.partnerName = ''
      }
    }
    if (fieldKey === 'partnerId') {
      const c = optionRefs.customers.find((x: any) => x.id === val)
      fd.partnerName = c?.name || ''
      fd.partnerCode = c?.code || ''
    }
    if (fieldKey === 'handlerId') {
      const u = optionRefs.users.find((x: any) => x.id === val)
      fd.handlerName = u?.name || ''
    }
    if (fieldKey === 'deptId') {
      const d = departmentOptions.value.find((x: any) => x.id === val)
      fd.deptName = d?.name || ''
    }
    if (fieldKey === 'payAccountId') {
      const acc = accountOptions.value.find((x: any) => x.value === val)
      fd.payAccountName = acc?.label || ''
    }
  },
  transformPayload: (fd, status) => ({
    status,
    id: fd.id || undefined,
    docNo: fd.orderNo || undefined,
    expenseType: Number(fd.expenseType) || 0,
    docDate: fd.date || undefined,
    partnerId: fd.partnerId,
    partnerCode: fd.partnerCode || undefined,
    partnerName: fd.partnerName || undefined,
    handlerId: fd.handlerId,
    handlerName: fd.handlerName || undefined,
    deptId: fd.deptId || undefined,
    deptName: fd.deptName || undefined,
    payAccountId: fd.payAccountId,
    payAccountName: fd.payAccountName || undefined,
    payAmount: Number(fd.payAmount) || 0,
    payAccount2Id: fd.payAccount2Id,
    payAccount2Name: fd.payAccount2Name || undefined,
    payAmount2: Number(fd.payAmount2) || 0,
    payAccount3Id: fd.payAccount3Id,
    payAccount3Name: fd.payAccount3Name || undefined,
    payAmount3: Number(fd.payAmount3) || 0,
    payAccount4Id: fd.payAccount4Id,
    payAccount4Name: fd.payAccount4Name || undefined,
    payAmount4: Number(fd.payAmount4) || 0,
    summary: fd.summary || undefined,
    remark: fd.remark || undefined,
    creatorName: fd.creatorName || undefined,
    items: (fd.products || [])
      .filter((p: any) => Number(p.amount) > 0 || (p.expenseName || '').trim())
      .map((p: any, i: number) => ({
        lineNo: i + 1,
        expenseCode: p.expenseCode || '',
        expenseName: p.expenseName || '',
        amount: Number(p.amount) || 0,
        remark: p.remark || '',
      })),
  }),
})

// 初始化默认值
if (!formData.date) formData.date = new Date().toISOString().slice(0, 10)
if (formData.expenseType === undefined) formData.expenseType = 0
for (const key of ['partnerName', 'partnerCode', 'handlerName', 'deptName', 'summary', 'remark', 'sourceNo', 'creatorName', 'createTime', 'payAccountName']) {
  if (formData[key] === undefined) formData[key] = ''
}
for (const key of ['payAmount', 'payAmount2', 'payAmount3', 'payAmount4', 'printCount']) {
  if (formData[key] === undefined) formData[key] = 0
}
for (const key of ['printAlwaysLastTemplate', 'printAfterSubmit']) {
  if (formData[key] === undefined) formData[key] = false
}
if (formData.status === undefined) formData.status = '0'
if (formData.approvalStatus === undefined) formData.approvalStatus = 0
if (formData.totalApprovalLevel === undefined) formData.totalApprovalLevel = 3

// 同步「本单金额」表头字段
watch(billTotal, (val) => {
  formData.billTotal = Number(val.toFixed(2))
}, { immediate: true })
if (formData.billTotal === undefined) formData.billTotal = 0

// ═══ 计算属性 ═══
const currentStatus = computed(() => String(formData.status ?? '0'))
const statusText = computed(() => STATUS_MAP[currentStatus.value]?.text || '草稿')
const statusColor = computed(() => STATUS_MAP[currentStatus.value]?.color || 'default')
const currentApprovalStatus = computed(() => String(formData.approvalStatus ?? '0'))
const approvalStatusText = computed(() => APPROVAL_STATUS_MAP[currentApprovalStatus.value]?.text || '未提交')
const approvalStatusColor = computed(() => APPROVAL_STATUS_MAP[currentApprovalStatus.value]?.color || 'default')
// 已记账 或 审批中 的单据锁定编辑；已驳回可继续修改后重新提交审批
const isLocked = computed(() =>
  effectiveMode.value === 'edit' && (currentStatus.value !== '0' || currentApprovalStatus.value === '1'))

// 可提交审批：草稿 且 未提交/已驳回
const canSubmitApproval = computed(() =>
  effectiveMode.value === 'edit' && !isLocked.value
  && currentStatus.value === '0'
  && (currentApprovalStatus.value === '0' || currentApprovalStatus.value === '3'))

const showSubmitApproval = ref(false)
const approvalRecord = computed(() => ({
  id: formData.id,
  docNo: formData.orderNo,
  totalAmount: billTotal.value,
  totalApprovalLevel: formData.totalApprovalLevel,
}))

function handleSubmitApprovalSuccess() {
  reloadApprovalState()
}

/** 提交审批后回读审批字段（审批状态/级别/当前审批人/提交时间） */
async function reloadApprovalState() {
  const id = formData.id
  if (!id) return
  try {
    const res: any = await expenseDocApi.getById(id)
    const data = res?.data || res || {}
    formData.approvalStatus = data.approvalStatus ?? 0
    formData.approvalLevel = data.approvalLevel ?? 0
    formData.totalApprovalLevel = data.totalApprovalLevel ?? 3
    formData.currentApproverName = data.currentApproverName || ''
    formData.submitTime = data.submitTime || ''
  } catch (error) {
    console.warn('[费用单] 回读审批状态失败', error)
  }
}

// ═══ 页眉配置 ═══
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '费用单',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    ...(canSubmitApproval.value ? [{ key: 'submitApproval', label: '提交审批', icon: AuditOutlined }] : []),
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ],
}))

// ═══ 基本信息字段（页面配置含隐藏 17 项） ═══
const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  { key: 'orderNo', label: '编号', type: 'input', inlineLabel: true, width: 200, disabled: true },
  { key: 'expenseType', label: '费用类型', type: 'select', required: true, inlineLabel: true, width: 180 },
  { key: 'partnerId', label: '往来单位', type: 'select', inlineLabel: true, width: 220, searchBtn: '+Q' },
  { key: 'partnerCode', label: '往来编号', type: 'input', inlineLabel: true, width: 120, disabled: true },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 180, searchBtn: '+Q' },
  { key: 'deptId', label: '部门', type: 'select', inlineLabel: true, width: 160, searchBtn: '+Q' },
  { key: 'date', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 160 },
  { key: 'payAccountId', label: '付款账户', type: 'select', inlineLabel: true, width: 220, searchBtn: '+Q' },
  { key: 'payAmount', label: '付款金额', type: 'number', inlineLabel: true, width: 150, precision: 2 },
  { key: 'moreAccount', label: '更多账户', type: 'input', inlineLabel: true, width: 150, searchBtn: '···' },
  { key: 'auditorName', label: '审核人', type: 'display', inlineLabel: true, width: 100 },
  { key: 'summary', label: '摘要', type: 'input', inlineLabel: true, width: 260 },
  { key: 'remark', label: '单据备注', type: 'input', inlineLabel: true, width: 260 },
  { key: 'creatorName', label: '制单人', type: 'display', inlineLabel: true, width: 100 },
  { key: 'createTime', label: '制单时间', type: 'input', inlineLabel: true, width: 200, disabled: true },
  { key: 'printCount', label: '打印次数', type: 'display', inlineLabel: true, width: 90 },
  { key: 'billTotal', label: '本单金额', type: 'number', inlineLabel: true, width: 150, precision: 2, disabled: true },
]

const DEFAULT_HIDDEN_FIELDS = ['deptId', 'auditorName', 'summary']
type FieldConfig = { visible: boolean; enterJump: boolean }
const pageConfig = reactive<Record<string, FieldConfig>>({})
for (const f of ALL_BASIC_INFO_FIELDS) {
  pageConfig[f.key] = {
    visible: !DEFAULT_HIDDEN_FIELDS.includes(f.key),
    enterJump: ['select', 'date', 'number', 'input'].includes(f.type),
  }
}

const FORM_CONFIG_MODULE = 'expense-doc'
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
  } catch { /* API 不可用时保持默认 */ }
}

async function saveFormConfig() {
  const payload = {
    fields: Object.fromEntries(Object.entries(pageConfig).map(([k, v]) => [k, { visible: v.visible, enterJump: v.enterJump }])),
    defaults: {
      printTemplate: formData.printTemplate,
      printAlwaysLastTemplate: formData.printAlwaysLastTemplate,
      printAfterSubmit: formData.printAfterSubmit,
    },
  }
  try {
    await userPageConfigApi.save(FORM_CONFIG_MODULE, FORM_CONFIG_PAGE, JSON.stringify(payload))
  } catch { /* 静默失败 */ }
}

const showFormConfig = ref(false)
const configModalTab = ref('pageConfig')
const departmentOptions = ref<any[]>([])
const accountOptions = ref<any[]>([])

async function loadExtraOptions() {
  try { departmentOptions.value = await optionsApi.getDepartments() } catch { departmentOptions.value = [] }
  try { accountOptions.value = (await optionsApi.getAccounts()).map((a: any) => ({ label: a.name, value: a.id })) } catch { accountOptions.value = [] }
}

const EXPENSE_TYPE_OPTIONS = [
  { label: '往来单位费用', value: 0 },
  { label: '内部费用', value: 1 },
]

const basicInfoFields = computed<BasicInfoField[]>(() =>
  ALL_BASIC_INFO_FIELDS
    .filter(f => pageConfig[f.key]?.visible !== false)
    .map(f => {
      if (f.key === 'expenseType') return { ...f, options: EXPENSE_TYPE_OPTIONS }
      if (f.key === 'partnerId') return { ...f, options: (optionRefs.customers || []).map((c: any) => ({ label: c.name + (c.code ? `(${c.code})` : ''), value: c.id })), loading: loadingOptions.value }
      if (f.key === 'handlerId') return { ...f, options: (optionRefs.users || []).map((u: any) => ({ label: u.name, value: u.id })), loading: loadingOptions.value }
      if (f.key === 'deptId') return { ...f, options: (departmentOptions.value || []).map((d: any) => ({ label: d.name, value: d.id })), loading: loadingOptions.value }
      if (f.key === 'payAccountId') return { ...f, options: accountOptions.value, loading: loadingOptions.value }
      return { ...f }
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
])

// ═══ 页脚 ═══
const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: `¥${billTotal.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: isLocked.value ? undefined : '保存草稿',
  draftShortcut: isLocked.value ? undefined : 'Ctrl+S',
  primaryBtnText: isLocked.value ? undefined : '记账',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══ 费用项明细列（5 列） ═══
const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 70, fixed: 'left' },
  { key: 'expenseCode', title: '费用编号', type: 'input', width: 140 },
  { key: 'expenseName', title: '费用名称', type: 'input', width: 220 },
  { key: 'amount', title: '金额', type: 'number', width: 140, precision: 2 },
  { key: 'remark', title: '备注', type: 'input', width: 180 },
]

const tableSummaryColumns = computed(() => [
  { key: 'amount', value: Number(billTotal.value.toFixed(2)), highlight: true },
] as { key: string; value: number; highlight?: boolean }[])

// ═══ 更多账户 ═══
const showMoreAccount = ref(false)
const moreAccounts = reactive<any[]>([{ rowKey: '2', accountId: undefined, accountName: '', amount: 0 }])
const moreAccountColumns = [
  { title: '付款账户', key: 'payAccount', width: 340 },
  { title: '付款金额', key: 'payAmount', width: 180 },
  { title: '操作', key: 'action', width: 80 },
]

function syncMoreAccountsFromFormData() {
  const rows: any[] = []
  if (formData.payAccount2Name || (formData.payAmount2 ?? 0) > 0) {
    rows.push({ rowKey: '2', accountId: formData.payAccount2Id, accountName: formData.payAccount2Name, amount: Number(formData.payAmount2) || 0 })
  }
  if (formData.payAccount3Name || (formData.payAmount3 ?? 0) > 0) {
    rows.push({ rowKey: '3', accountId: formData.payAccount3Id, accountName: formData.payAccount3Name, amount: Number(formData.payAmount3) || 0 })
  }
  if (formData.payAccount4Name || (formData.payAmount4 ?? 0) > 0) {
    rows.push({ rowKey: '4', accountId: formData.payAccount4Id, accountName: formData.payAccount4Name, amount: Number(formData.payAmount4) || 0 })
  }
  if (rows.length === 0) {
    rows.push({ rowKey: '2', accountId: undefined, accountName: '', amount: 0 })
  }
  moreAccounts.splice(0, moreAccounts.length, ...rows)
}

function handleMoreAccountChange(record: any, accountId: number) {
  const acc = accountOptions.value.find((x: any) => x.value === accountId)
  record.accountName = acc?.label || ''
}

function removeMoreAccount(record: any) {
  const idx = moreAccounts.findIndex((r: any) => r.rowKey === record.rowKey)
  if (idx >= 0) moreAccounts.splice(idx, 1)
}

function handleMoreAccountOk() {
  const raw = moreAccounts.filter((r: any) => r.accountId && Number(r.amount) > 0)
  formData.payAccount2Id = raw[0]?.accountId; formData.payAccount2Name = raw[0]?.accountName || ''; formData.payAmount2 = Number(raw[0]?.amount) || 0
  formData.payAccount3Id = raw[1]?.accountId; formData.payAccount3Name = raw[1]?.accountName || ''; formData.payAmount3 = Number(raw[1]?.amount) || 0
  formData.payAccount4Id = raw[2]?.accountId; formData.payAccount4Name = raw[2]?.accountName || ''; formData.payAmount4 = Number(raw[2]?.amount) || 0
  showMoreAccount.value = false
}

// 编辑模式加载详情后，付款账户2-4 回显到更多账户弹窗（moreAccounts 已定义，无 TDZ）
watch(() => formData.payAccount2Name, () => {
  syncMoreAccountsFromFormData()
}, { immediate: true })

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

function handleCellChange(_record: any, _fieldKey: string, _value: any) {
  // 占位行(__ghost)提升已由 BillDetailTable 组件内部处理
}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  if (fieldKey === 'moreAccount') {
    syncMoreAccountsFromFormData()
    showMoreAccount.value = true
  }
  if (fieldKey === 'orderNo' && !formData.orderNo) {
    message.warning('请先保存生成单据编号')
  }
}

async function handlePrimarySubmit() {
  if (isLocked.value) return
  // 金额守恒校验：本单金额 = Σ付款金额
  const payTotal = (Number(formData.payAmount) || 0)
    + (Number(formData.payAmount2) || 0) + (Number(formData.payAmount3) || 0) + (Number(formData.payAmount4) || 0)
  if (billTotal.value !== payTotal) {
    message.warning(`金额不平衡：费用项合计(${billTotal.value.toFixed(2)})≠付款合计(${payTotal.toFixed(2)})`)
    return
  }
  handleSubmit()
}

async function handleAction(actionKey: string) {
  switch (actionKey) {
    case 'history':
      router.push('/finance/expense-doc/index')
      break
    case 'config':
      showFormConfig.value = true
      break
    case 'submitApproval':
      if (!formData.id) {
        message.warning('请先保存草稿再提交审批')
        break
      }
      showSubmitApproval.value = true
      break
    case 'print':
      handlePrint()
      break
    default:
      message.info(`${actionKey} 功能暂不可用`)
  }
}

// ═══ 打印（按单据打印） ═══
// 费用单：后端已为 pageCode='finance-expense-doc' 登记装配器并有已发布模板，
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
  console.warn('[费用单] ErrorBoundary:', err)
}

// ═══ 生命周期 ═══
onMounted(async () => {
  if (effectiveMode.value !== 'edit' && formData.products.length === 0) {
    for (let i = 0; i < 2; i++) handleAddRow()
    // 经手人默认当前登录人
    if (!formData.handlerId && currentUserId.value) {
      formData.handlerId = currentUserId.value
      const u = optionRefs.users.find((x: any) => x.id === currentUserId.value)
      formData.handlerName = u?.name || currentUserName.value || ''
    }
  }
  loadFormConfig()
  loadExtraOptions()
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
.more-account-hint { font-size: 12px; color: #8c8c8c; margin-bottom: 8px; }
</style>
