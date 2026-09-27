<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- 已记账/已取消锁定提示 -->
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
        <!-- ═══ 科目明细表格 ═══
             ⚠️ 金标准易错点（勿改）：空行由 BillDetailTable 的 minRows 自动填充 __ghost 占位行；
             表格高度由 flex 链自适应【不要传 :max-height】。 -->
        <template #detail-table="{ onExpandChange }">
          <BillDetailTable
            :columns="detailColumns"
            v-model:data-source="formData.products"
            :storage-key="'ar-ap-adjust-form-columns'"
            @cell-change="handleCellChange"
            @expand-change="onExpandChange"
          >
            <template #actionCell="{ index, empty }">
              <template v-if="!empty">
                <a-space :size="2">
                  <a-button type="link" size="small" class="action-add-btn" @click="handleInsertProduct(index)">
                    <PlusCircleOutlined />
                  </a-button>
                  <a-button type="link" size="small" class="action-del-btn" @click="handleRemoveProduct(index)">
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

        <!-- ═══ 表尾：单据备注 + 单据信息 ═══ -->
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
            <span class="doc-info-item">
              经手人 <a-tag color="blue">{{ formData.handlerName || currentUserName || '系统' }}</a-tag>
            </span>
            <span class="doc-info-item">制单人 <a-tag color="blue">{{ formData.creatorName || currentUserName || '系统' }}</a-tag></span>
            <span class="doc-info-item">制单时间 {{ formData.createTime || formatNow() }}</span>
            <span class="doc-info-item">打印次数 {{ formData.printCount || 0 }}</span>
            <span v-if="formData.bookkeepingTime" class="doc-info-item">记账时间 {{ formData.bookkeepingTime }}</span>
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
                  <a-checkbox :checked="record.visible" @change="(e: any) => handlePageConfigFieldVisibleChange(record.key, e.target.checked)" />
                </template>
                <template v-if="column.key === 'enterJump'">
                  <a-checkbox :checked="record.enterJump" @change="(e: any) => handlePageConfigEnterJumpChange(record.key, e.target.checked)" />
                </template>
              </template>
            </a-table>
          </a-tab-pane>
          <!-- Tab 2: 录单默认值 -->
          <a-tab-pane key="defaultValues" tab="录单默认值">
            <a-form layout="horizontal" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
              <a-form-item label="默认经手人">
                <a-select v-model:value="formData.defaultHandlerId" show-search size="small" style="width:100%" :loading="loadingOptions" :options="(optionRefs.users||[]).map((u:any)=>({label:u.name,value:u.id}))" @change="saveFormConfig" />
              </a-form-item>
              <a-form-item label="默认调整方向">
                <a-select v-model:value="formData.defaultDirection" size="small" style="width:100%" :options="directionOptions" @change="saveFormConfig" />
              </a-form-item>
            </a-form>
          </a-tab-pane>
          <!-- Tab 3: 打印设置 -->
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
  <!-- 打印：按单据打印 -->
  <PrintDialog
    ref="printDialogRef"
    page-code="finance-ar-ap-adjust"
    :document-id="printData.id"
    :print-data="printData"
  />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ClockCircleOutlined, PrinterOutlined, DownloadOutlined, SettingOutlined,
  MinusCircleOutlined, PlusCircleOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillFormPage from '@/components/BillFormPage/index.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import type { BillHeaderConfig, BasicInfoField, SummaryRow, BillFooterConfig } from '@/components/BillFormPage/types'
import { useBillForm } from '@/components/BillFormPage/useBillForm'
import { arApAdjustApi, ADJUST_DIRECTION_OPTIONS, type ArApAdjustItem } from '@/api/finance/ar-ap-adjust'
import { accountSubjectApi } from '@/api/finance'
import { userPageConfigApi } from '@/api/erp'
import optionsApi from '@/api/options'
import { useUserStore } from '@/stores/user'
import PrintDialog from '@/components/PrintDialog/index.vue'

defineOptions({ name: 'FinanceArApAdjustForm' })

const router = useRouter()
const userStore = useUserStore()
const currentUserName = computed(() => userStore?.userInfo?.nickname || userStore?.userInfo?.username || '')
const currentUserId = computed(() => userStore?.userInfo?.id)

// ═══ 状态枚举（应收应付调整：0草稿 1已记账 2已取消） ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '已记账', color: 'blue' },
  2: { text: '已取消', color: 'red' },
}

const directionOptions = ADJUST_DIRECTION_OPTIONS

function currentOperator() {
  return { id: userStore?.userId || userStore?.userInfo?.id || 0, name: currentUserName.value || '系统' }
}

function toDateStr(v: any): string {
  if (!v) return ''
  if (typeof v === 'string') return v.slice(0, 10)
  return String(v).slice(0, 10)
}

// ═══ 结算单位下拉（客户 + 供应商合并，按调整方向过滤） ═══
interface PartnerOption { id: number; name: string; code: string; type: 'customer' | 'supplier' }
const partnerOptions = ref<PartnerOption[]>([])
const partnerLoading = ref(false)
async function loadPartners() {
  partnerLoading.value = true
  try {
    const [customers, suppliers] = await Promise.all([
      optionsApi.getCustomers().catch(() => []),
      optionsApi.getSuppliers().catch(() => []),
    ])
    const cus = (customers || []).map((c: any) => ({ id: c.id, name: c.name || c.partnerName || '', code: c.code || c.partnerCode || '', type: 'customer' as const }))
    const sup = (suppliers || []).map((s: any) => ({ id: s.id, name: s.name || s.supplierName || '', code: s.code || s.supplierCode || '', type: 'supplier' as const }))
    partnerOptions.value = [...cus, ...sup]
  } catch {
    partnerOptions.value = []
  } finally {
    partnerLoading.value = false
  }
}

/** 依据调整方向返回结算单位类型：1/2应收→customer，3/4应付→supplier */
function partnerTypeForDirection(direction?: number | string): 'customer' | 'supplier' {
  const d = Number(direction)
  return d === 3 || d === 4 ? 'supplier' : 'customer'
}

// ═══ 会计科目下拉 ═══
const subjectOptions = ref<{ value: string; label: string }[]>([])
async function loadSubjects() {
  try {
    const res: any = await accountSubjectApi.getList({ enabled: 1 })
    const data = res?.data ?? res ?? []
    const list = Array.isArray(data) ? data : (data?.records || [])
    subjectOptions.value = list.map((s: any) => ({
      value: s.subjectCode,
      label: `${s.subjectCode} ${s.subjectName}`,
    }))
  } catch {
    subjectOptions.value = []
  }
}

// ═══ useBillForm ═══
async function createWithStatus(data: any) {
  const { status, ...payload } = data
  const res: any = await arApAdjustApi.create({ ...payload, status: 0 })
  const created = res?.data || res
  if (status >= 1 && created?.id) {
    const u = currentOperator()
    await arApAdjustApi.confirm(created.id, u.id, u.name)
  }
  return created
}

async function updateWithStatus(id: number, data: any) {
  const { status, ...payload } = data
  const res: any = await arApAdjustApi.update({ ...payload, id, status: 0 })
  if (status >= 1) {
    const u = currentOperator()
    await arApAdjustApi.confirm(id, u.id, u.name)
  }
  return res?.data || res
}

function findPartner(id?: number) {
  return partnerOptions.value.find((p) => p.id === id)
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
  billPrefix: 'YSKZJ',
  api: {
    create: createWithStatus,
    update: updateWithStatus,
    getById: async (id: number) => {
      const res: any = await arApAdjustApi.getById(id)
      const data = res?.data || res || {}
      const partner = findPartner(data.partnerId)
      const rawItems = data.items || data.details || []
      return {
        ...data,
        docNo: data.docNo || '',
        date: toDateStr(data.docDate) || '',
        direction: data.direction ?? 1,
        partnerId: data.partnerId,
        partnerType: data.partnerType || (data.direction ? partnerTypeForDirection(data.direction) : 'customer'),
        partnerCode: data.partnerCode || partner?.code || '',
        partnerName: data.partnerName || partner?.name || '',
        handlerId: data.handlerId,
        handlerName: data.handlerName || '',
        deptId: data.deptId || undefined,
        deptName: data.deptName || '',
        items: rawItems.map((d: any, i: number) => ({
          ...d,
          id: d.id || `detail-${i}`,
          subjectCode: d.subjectCode || '',
          subjectName: d.subjectName || '',
          amount: d.amount ?? 0,
          remark: d.remark || '',
        })),
      }
    },
  },
  redirectPath: '/finance/ar-ap-adjust/index',
  codeApiPath: '/erp/finance/ar-ap-adjust/next-no',
  optionTypes: ['users'],
  fields: [
    { key: 'direction', label: '调整方向', type: 'select', required: true },
    { key: 'partnerId', label: '结算单位', type: 'select', required: true },
    { key: 'handlerId', label: '经手人', type: 'select', required: true },
    { key: 'date', label: '单据日期', type: 'date', required: true },
    { key: 'deptId', label: '部门', type: 'select' },
  ],
  productDefaults: {
    subjectCode: '',
    subjectName: '',
    amount: 0,
    remark: '',
  },
  onFieldChange: (fieldKey, val, fd) => {
    if (fieldKey === 'direction') {
      fd.directionName = directionOptions.find((d) => d.value === val)?.label || ''
      fd.partnerType = partnerTypeForDirection(val)
      // 调整方向切换后，若当前结算单位类型不符则清空
      const cur = findPartner(fd.partnerId)
      if (cur && cur.type !== fd.partnerType) {
        fd.partnerId = undefined
        fd.partnerName = ''
        fd.partnerCode = ''
      }
    }
    if (fieldKey === 'partnerId') {
      const p = findPartner(val)
      fd.partnerName = p?.name || ''
      fd.partnerCode = p?.code || ''
      fd.partnerType = p?.type || fd.partnerType || partnerTypeForDirection(Number(fd.direction))
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
  // P1 守恒：本单金额 = Σ科目明细金额。后端 confirm 会再次强校验。
  transformPayload: (fd, status) => {
    const items = (fd.products || [])
      .filter((p: any) => p.subjectCode && Number(p.amount) > 0)
      .map((p: any): ArApAdjustItem => ({
        subjectCode: p.subjectCode,
        subjectName: p.subjectName || '',
        amount: Number(p.amount) || 0,
        remark: p.remark || undefined,
      }))
    const totalAmount = Math.round(items.reduce((s: number, it: any) => s + (Number(it.amount) || 0), 0) * 100) / 100
    const partner = findPartner(fd.partnerId)
    return {
      status,
      docNo: fd.docNo || fd.orderNo || undefined,
      docDate: fd.date || undefined,
      direction: Number(fd.direction) || 1,
      partnerId: fd.partnerId,
      partnerCode: fd.partnerCode || partner?.code || undefined,
      partnerType: fd.partnerType || partner?.type || partnerTypeForDirection(Number(fd.direction)),
      partnerName: fd.partnerName || partner?.name || '',
      handlerId: fd.handlerId,
      handlerName: fd.handlerName,
      deptId: fd.deptId || undefined,
      deptName: fd.deptName || undefined,
      creatorName: currentUserName.value || fd.creatorName || undefined,
      summary: fd.summary || undefined,
      remark: fd.remark || undefined,
      totalAmount,
      items,
    }
  },
})

// 初始化调整单默认值
if (!formData.date) formData.date = new Date().toISOString().slice(0, 10)
for (const key of [
  'docNo', 'directionName', 'partnerType', 'partnerName', 'partnerCode', 'handlerName', 'deptName',
  'summary', 'remark', 'createTime', 'creatorName', 'bookkeepingTime', 'printCount',
  'defaultHandlerId', 'printTemplate', 'printPaperSize',
]) {
  if (formData[key] === undefined) formData[key] = ''
}
if (formData.direction === undefined) formData.direction = 1
if (formData.defaultDirection === undefined) formData.defaultDirection = 1
for (const key of ['printCount', 'printCopies']) {
  if (formData[key] === undefined) formData[key] = 0
}
for (const key of ['printAlwaysLastTemplate', 'printAfterSubmit']) {
  if (formData[key] === undefined) formData[key] = false
}
if (formData.status === undefined) formData.status = 0

// ═══ 计算属性 ═══
const currentStatus = computed(() => Number(formData.status ?? 0))
const statusText = computed(() => STATUS_MAP[currentStatus.value]?.text || '草稿')
const statusColor = computed(() => STATUS_MAP[currentStatus.value]?.color || 'default')
const isLocked = computed(() => effectiveMode.value === 'edit' && currentStatus.value !== 0)

// 本单金额 = Σ科目明细金额
const transferAmount = computed(() => {
  const sum = (formData.products || []).reduce((s: number, p: any) => s + (Number(p.amount) || 0), 0)
  return Math.round(sum * 100) / 100
})

// ═══ 页眉配置 ═══
const headerConfig = computed<BillHeaderConfig>(() => ({
  title: '应收应付调整',
  orderNo: formData.docNo || formData.orderNo,
  showAttachment: true,
  actions: [
    { key: 'print', label: '打印(F8)', icon: PrinterOutlined },
    { key: 'export', label: '导出', icon: DownloadOutlined },
    { key: 'history', label: '历史', icon: ClockCircleOutlined },
    { key: 'config', label: '配置', icon: SettingOutlined },
  ],
}))

// ═══ 基本信息字段（10项：编号/调整方向/单据日期/结算单位/经手人/部门/单据备注/制单人/打印次数/本单金额） ═══
const ALL_BASIC_INFO_FIELDS: BasicInfoField[] = [
  { key: 'orderNo', label: '编号', type: 'input', inlineLabel: true, width: 210, disabled: true },
  { key: 'direction', label: '调整方向', type: 'select', required: true, inlineLabel: true, width: 200, options: directionOptions },
  { key: 'date', label: '单据日期', type: 'date', required: true, inlineLabel: true, width: 180 },
  { key: 'partnerId', label: '结算单位', type: 'select', required: true, inlineLabel: true, width: 260, searchBtn: '+Q', loading: true },
  { key: 'handlerId', label: '经手人', type: 'select', required: true, inlineLabel: true, width: 180, searchBtn: '+Q', loading: true },
  { key: 'deptId', label: '部门', type: 'select', inlineLabel: true, width: 160, searchBtn: '+Q', loading: true },
  { key: 'remark', label: '单据备注', type: 'input', inlineLabel: true, width: 300 },
  { key: 'creatorName', label: '制单人', type: 'display', inlineLabel: true, width: 100 },
  { key: 'printCount', label: '打印次数', type: 'display', inlineLabel: true, width: 90 },
  { key: 'totalAmount', label: '本单金额', type: 'display', inlineLabel: true, width: 150 },
]

const DEFAULT_HIDDEN_FIELDS = ['deptId']
type FieldConfig = { visible: boolean; enterJump: boolean }
const pageConfig = reactive<Record<string, FieldConfig>>({})
for (const f of ALL_BASIC_INFO_FIELDS) {
  pageConfig[f.key] = {
    visible: !DEFAULT_HIDDEN_FIELDS.includes(f.key),
    enterJump: ['select', 'date', 'number', 'input'].includes(f.type),
  }
}

const FORM_CONFIG_MODULE = 'ar-ap-adjust-form'
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
        defaultHandlerId: parsed.defaults.defaultHandlerId,
        defaultDirection: parsed.defaults.defaultDirection,
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
      defaultHandlerId: formData.defaultHandlerId,
      defaultDirection: formData.defaultDirection,
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

// 表尾/单据信息/页脚本单金额已展示的字段，不重复出现在表头
const BOTTOM_FIELDS = ['remark', 'creatorName', 'printCount', 'totalAmount']

// ⚠️ 结算单位下拉：按当前调整方向过滤（应收→客户，应付→供应商）
const filteredPartnerOptions = computed(() => {
  const type = partnerTypeForDirection(formData.direction)
  return partnerOptions.value.filter((p: PartnerOption) => p.type === type)
})

const basicInfoFields = computed<BasicInfoField[]>(() =>
  ALL_BASIC_INFO_FIELDS
    .filter(f => pageConfig[f.key]?.visible !== false && !BOTTOM_FIELDS.includes(f.key))
    .map(f => ({
      ...f,
      options: f.key === 'direction'
        ? directionOptions
        : f.key === 'partnerId'
          ? filteredPartnerOptions.value.map((p) => ({ label: p.name, value: p.id }))
          : f.key === 'handlerId'
            ? (optionRefs.users || []).map((u: any) => ({ label: u.name, value: u.id }))
            : f.key === 'deptId'
              ? (departmentOptions.value || []).map((d: any) => ({ label: d.name, value: d.id }))
              : (f as any).options,
      loading: (f.key === 'partnerId') ? partnerLoading.value
        : (f.key === 'handlerId' || f.key === 'deptId') ? loadingOptions.value
          : (f as any).loading,
      searchBtn: (f.key === 'partnerId' || f.key === 'handlerId' || f.key === 'deptId') ? '+Q' : (f as any).searchBtn,
    }))
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
  { label: '本单金额', value: `¥${transferAmount.value.toFixed(2)}`, statusLabel: statusText.value },
  { label: '调账类型', value: formData.directionName || directionOptions.find((d) => d.value === Number(formData.direction))?.label || '-', divider: true },
])

// ═══ 页脚 ═══
const footerConfig = computed<BillFooterConfig>(() => ({
  amountLabel: '本单金额',
  amountValue: `¥${transferAmount.value.toFixed(2)}`,
  amountHighlight: true,
  draftBtnText: isLocked.value ? undefined : '保存草稿',
  draftShortcut: isLocked.value ? undefined : 'Ctrl+S',
  primaryBtnText: isLocked.value ? undefined : '记账',
  primaryShortcut: 'Ctrl+Enter',
  saving: saving.value,
}))

// ═══ 科目明细列（4数据列：科目编号/科目名称/金额/备注 + 操作） ═══
const detailColumns = computed<DetailColumnConfig[]>(() => [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 60, fixed: 'left' },
  { key: 'subjectCode', title: '科目编号', type: 'select', width: 240, options: subjectOptions.value, placeholder: '请选择对转科目' },
  { key: 'subjectName', title: '科目名称', type: 'input', width: 220 },
  { key: 'amount', title: '金额', type: 'number', width: 160, precision: 2 },
  { key: 'remark', title: '备注', type: 'input', width: 200, defaultHidden: true },
])

// ═══ 事件处理 ═══
function handleInsertProduct(index: number) {
  handleAddProduct()
  const item = formData.products.pop()
  if (item) formData.products.splice(index + 1, 0, item)
}

function handleCellChange(record: any, fieldKey: string, value: any) {
  if (fieldKey === 'subjectCode') {
    const opt = subjectOptions.value.find((o) => o.value === value)
    record.subjectName = opt?.label?.split(' ').slice(1).join(' ') || ''
  }
}

function handleFieldChange(fieldKey: string, val: any) {
  baseFieldChange(fieldKey, val)
}

function handleSearchBtn(fieldKey: string, _btnText: string) {
  message.info(`${fieldKey} 快速查询功能暂不可用`)
}

async function handlePrimarySubmit() {
  if (isLocked.value) return
  if (!formData.partnerId) {
    message.warning('请选择结算单位')
    return
  }
  const amountLines = (formData.products || []).filter((p: any) => p.subjectCode && Number(p.amount) > 0)
  if (amountLines.length === 0) {
    message.warning('请至少填写一条对转科目明细')
    return
  }
  handleSubmit()
}

async function handleAction(actionKey: string) {
  switch (actionKey) {
    case 'history':
      router.push('/finance/ar-ap-adjust/index')
      break
    case 'config':
      showFormConfig.value = true
      break
    case 'print':
      handlePrint()
      break
    case 'export':
      handleExport()
      break
  }
}

// ═══ 打印（按单据打印） ═══
// 应收应付调整单：后端已为 pageCode='finance-ar-ap-adjust' 登记装配器并有已发布模板，
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

function handleExport() {
  const items = (formData.products || []).filter((p: any) => p.subjectCode)
  if (items.length === 0) {
    message.warning('没有可导出的明细')
    return
  }
  const headers = ['科目编号', '科目名称', '金额', '备注']
  const rows = items.map((p: any) => [
    p.subjectCode || '', p.subjectName || '', p.amount || 0, p.remark || '',
  ])
  const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `应收应付调整_${formData.docNo || new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

function formatNow() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function handleError(err: any) {
  console.warn('[应收应付调整] ErrorBoundary:', err)
}

// ═══ 生命周期 ═══
onMounted(() => {
  loadPartners()
  loadSubjects()
  if (effectiveMode.value !== 'edit' && formData.products.length === 0) {
    // 空行由 BillDetailTable 的 minRows 自动填充，无需手动 push
    if (!formData.handlerId && currentUserId.value) {
      formData.handlerId = currentUserId.value
      const u = optionRefs.users.find((x: any) => x.id === currentUserId.value)
      formData.handlerName = u?.name || currentUserName.value || ''
    }
    if (formData.defaultDirection) {
      formData.direction = formData.defaultDirection
    }
    if (formData.defaultHandlerId) {
      formData.handlerId = formData.defaultHandlerId
      const u = optionRefs.users.find((x: any) => x.id === formData.defaultHandlerId)
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
