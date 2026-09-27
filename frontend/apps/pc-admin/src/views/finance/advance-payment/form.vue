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
        <!-- ═══ 付款账户明细表（5 列） ═══ -->
        <template #detail-table="{ onExpandChange }">
          <BillDetailTable
            :columns="detailColumns"
            v-model:data-source="formData.products"
            :summary-columns="tableSummaryColumns"
            :storage-key="'advance-payment-form-columns'"
            @cell-change="handleCellChange"
            @expand-change="onExpandChange"
          >
            <template #actionCell="{ index, empty }">
              <template v-if="!empty">
                <a-space :size="2">
                  <a-button type="link" size="small" class="action-add-btn" @click="handleInsertRow(index)">
                    <PlusCircleOutlined />
                  </a-button>
                  <a-button type="link" size="small" class="action-del-btn" @click="handleRemoveRow(index)">
                    <MinusCircleOutlined />
                  </a-button>
                </a-space>
              </template>
              <template v-else>
                <a-space :size="2">
                  <a-button type="link" size="small" class="action-add-btn" @click="handleAddRow()">
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
              <span class="remark-label">摘要</span>
              <a-input
                v-model:value="formData.summary"
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
            <span class="doc-info-item">
              单据状态 <a-tag :color="statusColor">{{ statusText }}</a-tag>
            </span>
            <span class="doc-info-item">制单人 <a-tag color="blue">{{ formData.creatorName || currentUserName || '系统' }}</a-tag></span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
            <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
            <span v-if="formData.bookkeepingTime" class="doc-info-item">记账时间 {{ formData.bookkeepingTime }}</span>
          </div>
        </template>
      </BillFormPage>

      <!-- ═══ 配置弹窗：页面配置/打印设置（双 Tab） ═══ -->
      <a-modal
        v-model:open="showFormConfig"
        title="配置"
        :width="760"
        :footer="null"
        destroy-on-close
      >
        <a-tabs v-model:active-key="configModalTab" size="small">
          <!-- Tab 1: 页面配置 -->
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
              <a-form-item label="打印选项">
                <a-checkbox v-model:checked="formData.printAlwaysLastTemplate" @change="saveFormConfig">始终使用最后一次打印的模板，打印时不再选择</a-checkbox>
                <a-checkbox v-model:checked="formData.printAfterSubmit" @change="saveFormConfig">记账后立即打印</a-checkbox>
              </a-form-item>
            </a-form>
          </a-tab-pane>
        </a-tabs>
      </a-modal>
    </PageContainer>
  <!-- 打印：按单据打印 -->
  <PrintDialog
    ref="printDialogRef"
    page-code="finance-advance-payment"
    :document-id="printData.id"
    :print-data="printData"
  />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
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
import { prePaymentApi } from '@/api/finance'
import { userPageConfigApi } from '@/api/erp'
import optionsApi from '@/api/options'
import { useUserStore } from '@/stores/user'
import PrintDialog from '@/components/PrintDialog/index.vue'

defineOptions({ name: 'AdvancePaymentForm' })

const router = useRouter()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
const currentUserId = computed(() => userStore?.userInfo?.id)

// ═══ 状态枚举 ═══
const STATUS_MAP: Record<string, { text: string; color: string }> = {
  draft: { text: '草稿', color: 'default' },
  confirmed: { text: '已记账', color: 'blue' },
  offset: { text: '已冲抵', color: 'gold' },
  recovered: { text: '已收回', color: 'orange' },
  refunded: { text: '已退款', color: 'red' },
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

// 本单金额 = Σ付款明细金额
const billTotal = computed(() =>
  formData.products.reduce((s: number, p: any) => s + (p.amount || 0), 0)
)

// ═══ useBillForm ═══
async function createWithStatus(data: any) {
  const { status, ...payload } = data
  const res: any = await prePaymentApi.saveDraft({ ...payload })
  const created = res?.data || res
  if (status >= 1 && created?.id) {
    const u = currentOperator()
    await prePaymentApi.confirm(created.id, u.id, u.name)
  }
  return created
}

async function updateWithStatus(id: number, data: any) {
  const { status, ...payload } = data
  const res: any = await prePaymentApi.saveDraft({ ...payload, id })
  if (status >= 1) {
    const u = currentOperator()
    await prePaymentApi.confirm(id, u.id, u.name)
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
  billPrefix: 'YFKD',
  api: {
    create: createWithStatus,
    update: updateWithStatus,
    getById: async (id: number) => {
      const res: any = await prePaymentApi.getById(id)
      const data = res?.data || res || {}
      return {
        ...data,
        orderNo: data.prePaymentNo || '',
        date: toDateStr(data.paymentDate) || '',
        supplierId: data.supplierId,
        supplierName: data.supplierName || '',
        partnerCode: data.partnerCode || '',
        handlerId: data.handlerId,
        handlerName: data.handlerName || '',
        deptId: data.deptId || undefined,
        deptName: data.deptName || '',
        sourceNo: data.sourceNo || '',
        sourceUnsettledAmount: data.sourceUnsettledAmount ?? 0,
        items: (data.items || []).map((d: any, i: number) => ({
          id: d.id || `detail-${i}`,
          lineNo: d.lineNo || i + 1,
          accountNo: d.accountNo || '',
          accountName: d.accountName || '',
          amount: d.amount ?? 0,
          remark: d.remark || '',
        })),
      }
    },
  },
  redirectPath: '/finance/advance-payment/index',
  codeApiPath: '/erp/pre-payment/next-no',
  optionTypes: ['suppliers', 'users'],
  fields: [
    { key: 'supplierId', label: '结算单位', type: 'select', required: true },
    { key: 'handlerId', label: '经手人', type: 'select', required: true },
    { key: 'date', label: '单据日期', type: 'date', required: true },
    { key: 'deptId', label: '部门', type: 'select' },
  ],
  productDefaults: {
    accountNo: '', accountName: '', amount: 0, remark: '',
  },
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'supplierId') {
      const c = optionRefs.suppliers.find((x: any) => x.id === val)
      fd.supplierName = c?.name || ''
      fd.partnerCode = c?.code || ''
      loadAdvanceBalance(val)
    }
    if (fieldKey === 'handlerId') {
      const u = optionRefs.users.find((x: any) => x.id === val)
      fd.handlerName = u?.name || ''
    }
    if (fieldKey === 'deptId') {
      const d = departmentOptions.value.find((x: any) => x.id === val)
      fd.deptName = d?.name || ''
    }
  },
  transformPayload: (fd, status) => ({
    status,
    id: fd.id || undefined,
    prePaymentNo: fd.orderNo || undefined,
    sourceType: fd.sourceType || undefined,
    sourceId: fd.sourceId || undefined,
    sourceNo: fd.sourceNo || undefined,
    sourceUnsettledAmount: fd.sourceUnsettledAmount ?? undefined,
    partnerCode: fd.partnerCode || undefined,
    supplierId: fd.supplierId,
    supplierName: fd.supplierName,
    amount: Number(billTotal.value) || 0,
    prevAmount: Number(fd.prevAmount) || 0,
    paymentDate: fd.date || undefined,
    summary: fd.summary || undefined,
    remark: fd.remark || undefined,
    handlerId: fd.handlerId,
    handlerName: fd.handlerName,
    deptId: fd.deptId || undefined,
    deptName: fd.deptName || undefined,
    items: (fd.products || [])
      .filter((p: any) => Number(p.amount) > 0 || (p.accountName || '').trim())
      .map((p: any, i: number) => ({
        lineNo: i + 1,
        accountNo: p.accountNo || '',
        accountName: p.accountName || '',
        amount: Number(p.amount) || 0,
        remark: p.remark || '',
      })),
  }),
})

// 初始化默认值
if (!formData.date) formData.date = new Date().toISOString().slice(0, 10)
for (const key of ['supplierName', 'partnerCode', 'handlerName', 'deptName', 'summary', 'remark', 'sourceNo', 'creatorName', 'createTime', 'bookkeepingTime']) {
  if (formData[key] === undefined) formData[key] = ''
}
for (const key of ['prevAmount', 'sourceUnsettledAmount', 'printCount', 'printCopies']) {
  if (formData[key] === undefined) formData[key] = 0
}
for (const key of ['printAlwaysLastTemplate', 'printAfterSubmit']) {
  if (formData[key] === undefined) formData[key] = false
}
if (formData.status === undefined) formData.status = 'draft'

// 同步「本单金额」表头字段（只读展示）
watch(billTotal, (val) => {
  formData.billTotal = Number(val.toFixed(2))
}, { immediate: true })
if (formData.billTotal === undefined) formData.billTotal = 0

// ═══ 计算属性 ═══
const currentStatus = computed(() => String(formData.status ?? 'draft'))
const statusText = computed(() => STATUS_MAP[currentStatus.value]?.text || '草稿')
const statusColor = computed(() => STATUS_MAP[currentStatus.value]?.color || 'default')
const isLocked = computed(() => effectiveMode.value === 'edit' && currentStatus.value !== 'draft')

// ═══ 页眉配置 ═══
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '预付款单',
  orderNo: formData.orderNo,
  showAttachment: true,
  actions: [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ],
}))

// ═══ 基本信息字段（15 个，默认显示 11 隐藏 4） ═══
const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  { key: 'orderNo', label: '编号', type: 'input', inlineLabel: true, width: 200, disabled: true },
  { key: 'supplierId', label: '结算单位', type: 'select', required: true, inlineLabel: true, width: 220, searchBtn: '+Q', loading: true },
  { key: 'partnerCode', label: '供应商编号', type: 'input', inlineLabel: true, width: 120, disabled: true },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 180, searchBtn: '+Q', loading: true },
  { key: 'deptId', label: '部门', type: 'select', inlineLabel: true, width: 160, searchBtn: '+Q', loading: true },
  { key: 'date', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 160 },
  { key: 'prevAmount', label: '此前预付', type: 'number', inlineLabel: true, width: 150, precision: 2, disabled: true },
  { key: 'summary', label: '摘要', type: 'input', inlineLabel: true, width: 260 },
  { key: 'remark', label: '单据备注', type: 'input', inlineLabel: true, width: 260 },
  { key: 'creatorName', label: '制单人', type: 'display', inlineLabel: true, width: 100 },
  { key: 'createTime', label: '制单时间', type: 'input', inlineLabel: true, width: 200, disabled: true },
  { key: 'printCount', label: '打印次数', type: 'display', inlineLabel: true, width: 90 },
  { key: 'sourceNo', label: '源单', type: 'input', inlineLabel: true, width: 200, disabled: true },
  { key: 'sourceUnsettledAmount', label: '源单未结金额', type: 'number', inlineLabel: true, width: 150, precision: 2, disabled: true },
  { key: 'billTotal', label: '本单金额', type: 'number', inlineLabel: true, width: 150, precision: 2, disabled: true },
]

const DEFAULT_HIDDEN_FIELDS = [
  'partnerCode', 'deptId', 'summary', 'sourceUnsettledAmount',
]
type FieldConfig = { visible: boolean; enterJump: boolean }
const pageConfig = reactive<Record<string, FieldConfig>>({})
for (const f of ALL_BASIC_INFO_FIELDS) {
  pageConfig[f.key] = {
    visible: !DEFAULT_HIDDEN_FIELDS.includes(f.key),
    enterJump: ['select', 'date', 'number', 'input'].includes(f.type),
  }
}

const FORM_CONFIG_MODULE = 'advance-payment'
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

async function loadExtraOptions() {
  try { departmentOptions.value = await optionsApi.getDepartments() } catch { departmentOptions.value = [] }
}

const basicInfoFields = computed<BasicInfoField[]>(() =>
  ALL_BASIC_INFO_FIELDS
    .filter(f => pageConfig[f.key]?.visible !== false)
    .map(f => {
      return {
        ...f,
        options: f.key === 'supplierId'
          ? (optionRefs.suppliers || []).map((c: any) => ({ label: c.name + (c.code ? `(${c.code})` : ''), value: c.id }))
          : f.key === 'handlerId'
            ? (optionRefs.users || []).map((u: any) => ({ label: u.name, value: u.id }))
            : f.key === 'deptId'
              ? (departmentOptions.value || []).map((d: any) => ({ label: d.name, value: d.id }))
              : (f as any).options,
        loading: (f.key === 'supplierId' || f.key === 'handlerId' || f.key === 'deptId')
          ? loadingOptions.value
          : (f as any).loading,
        searchBtn: (f.key === 'supplierId' || f.key === 'handlerId' || f.key === 'deptId')
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

// ═══ 付款账户明细列（5 列） ═══
const detailColumns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 70, fixed: 'left' },
  { key: 'accountNo', title: '付款账户编号', type: 'input', width: 160 },
  { key: 'accountName', title: '付款账户', type: 'input', width: 220 },
  { key: 'amount', title: '付款金额', type: 'number', width: 140, precision: 2 },
  { key: 'remark', title: '备注', type: 'input', width: 180 },
]

const tableSummaryColumns = computed(() => [
  { key: 'amount', value: Number(billTotal.value.toFixed(2)), highlight: true },
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
  // 占位行(__ghost)提升已由 BillDetailTable 组件内部处理（v-model:data-source 回写），此处无需额外联动
}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
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
      router.push('/finance/advance-payment/index')
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
// 预付款单：后端已为 pageCode='finance-advance-payment' 登记装配器并有已发布模板，
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

// 结算单位变更 → 带出此前预付
async function loadAdvanceBalance(supplierId: number) {
  if (!supplierId) { formData.prevAmount = 0; return }
  try {
    const res: any = await prePaymentApi.getAdvanceBalance(supplierId)
    const val = (res?.data ?? res) ?? 0
    formData.prevAmount = Number(val) || 0
  } catch {
    formData.prevAmount = 0
  }
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function handleError(err: any) {
  console.warn('[预付款单] ErrorBoundary:', err)
}

// ═══ 生命周期 ═══
onMounted(async () => {
  if (effectiveMode.value !== 'edit' && formData.products.length === 0) {
    for (let i = 0; i < 4; i++) handleAddRow()
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
</style>
