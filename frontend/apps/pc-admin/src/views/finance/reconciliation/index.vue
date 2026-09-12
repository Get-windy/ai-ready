<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!-- ═══ 统计卡片（4张，30s轮询） ═══ -->
      <div class="recon-stat-strip">
        <div class="recon-stat-card stat-bank">
          <div class="recon-stat-body">
            <div class="recon-stat-value">{{ stats.bankPending }}</div>
            <div class="recon-stat-label">待银行对账</div>
          </div>
          <BankOutlined class="recon-stat-icon" />
        </div>
        <div class="recon-stat-card stat-customer">
          <div class="recon-stat-body">
            <div class="recon-stat-value">{{ stats.customerPending }}</div>
            <div class="recon-stat-label">待客户对账</div>
          </div>
          <UserOutlined class="recon-stat-icon" />
        </div>
        <div class="recon-stat-card stat-supplier">
          <div class="recon-stat-body">
            <div class="recon-stat-value">{{ stats.supplierPending }}</div>
            <div class="recon-stat-label">待供应商对账</div>
          </div>
          <TeamOutlined class="recon-stat-icon" />
        </div>
        <div class="recon-stat-card stat-difference">
          <div class="recon-stat-body">
            <div class="recon-stat-value">{{ stats.differenceCount }}</div>
            <div class="recon-stat-label">差异待处理</div>
          </div>
          <WarningOutlined class="recon-stat-icon" />
        </div>
      </div>

      <CategoryListLayout
        :show-category-panel="false"
        :show-table-footer="false"
        :tabs="tabs"
        :active-tab="activeTab"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏右侧：刷新 / 新增对账 / 页面配置（列配置走数据表表头齿轮） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button v-if="fnEnabled('refresh')" size="small" :loading="refreshLoading" @click="handleRefresh">
              <ReloadOutlined /> 刷新
              <span v-if="autoRefreshCountdown > 0" class="refresh-countdown">{{ autoRefreshCountdown }}s</span>
            </a-button>
            <a-button v-if="fnEnabled('add')" type="primary" size="small" @click="handleAdd">
              <PlusOutlined /> 新增对账
            </a-button>
            <template v-if="fnEnabled('config')">
              <a-tooltip title="页面配置">
                <a-button size="small" @click="openPageConfig"><SettingOutlined /></a-button>
              </a-tooltip>
            </template>
          </a-space>
        </template>

        <!-- ═══ 查询区 ═══ -->
        <template #search-fields>
          <div class="recon-search-area">
            <div class="recon-search-grid">
              <template v-for="field in visibleSearchFields" :key="field.key">
                <div v-if="field.type === 'dateRange'" class="recon-search-item">
                  <a-range-picker
                    v-model:value="rangeValue[field.key]"
                    size="small"
                    style="width: 220px"
                    @change="(v: any) => onRangeChange(field.key, v)"
                  />
                </div>
                <div v-else-if="field.type === 'select'" class="recon-search-item recon-select-wrap">
                  <span class="recon-select-label">{{ field.label }}</span>
                  <a-select
                    v-model:value="searchParams[field.key]"
                    placeholder="全部"
                    allow-clear
                    size="small"
                    @change="handleSearch"
                  >
                    <a-select-option v-for="opt in field.options" :key="opt.value" :value="opt.value">
                      {{ opt.label }}
                    </a-select-option>
                  </a-select>
                </div>
                <div v-else class="recon-search-item">
                  <a-input
                    v-model:value="searchParams[field.key]"
                    :placeholder="field.placeholder || field.label"
                    allow-clear
                    size="small"
                    @press-enter="handleSearch"
                  />
                </div>
              </template>
              <div class="recon-search-item">
                <a-button type="primary" size="small" @click="handleSearch">
                  <SearchOutlined /> 查询
                </a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格 ═══ -->
        <template #table>
          <BillTableList
            :columns="currentColumns"
            :data-source="tableData"
            :storage-key="tableStorageKey"
            :loading="loading"
            :pagination="billPagination"
            :show-toolbar="false"
            :show-search="false"
            :show-add="false"
            :show-export="false"
            :show-batch-delete="false"
            :selectable="false"
            :summary-data="footerColumns"
            row-key="id"
            @page-change="handlePageChange"
          >
            <template #periodCell="{ record }">
              <span>{{ record.startDate }} ~ {{ record.endDate }}</span>
            </template>
            <template #systemBalanceCell="{ record }">
              <span class="num-value">¥{{ fmt(record.systemBalance) }}</span>
            </template>
            <template #actualBalanceCell="{ record }">
              <span class="num-value">¥{{ fmt(record.actualBalance) }}</span>
            </template>
            <template #differenceCell="{ record }">
              <span :class="['num-value', Number(record.difference) !== 0 ? 'diff-red' : 'diff-green']">
                ¥{{ fmt(record.difference) }}
              </span>
            </template>
            <template #statusCell="{ record }">
              <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
            </template>
            <template #typeCell="{ record }">
              <a-tag>{{ getTypeText(record.reconciliationType) }}</a-tag>
            </template>
            <template #reasonCell="{ record }">
              <span :title="record.differenceReason">{{ record.differenceReason || '—' }}</span>
            </template>
            <template #actionCell="{ record }">
              <template v-if="activeTab === 'difference'">
                <a-button type="link" size="small" @click="handleResolveDifference(record)">处理差异</a-button>
              </template>
              <template v-else-if="record.status === 0">
                <a-button type="link" size="small" @click="handleReconcile(record)">对账</a-button>
              </template>
              <span v-else class="no-action">—</span>
            </template>
          </BillTableList>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 新增对账弹窗 ═══ -->
    <FullScreenDetail
      :visible="addVisible"
      title="新增对账"
      :dirty="addFormDirty"
      :save-loading="addLoading"
      @save="handleAddConfirm"
      @close="handleAddCancel"
    >
      <a-form ref="addFormRef" :model="addForm" :rules="addFormRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="对账类型" name="type">
          <a-radio-group v-model:value="addForm.type" @change="handleTypeChange">
            <a-radio value="bank">银行对账</a-radio>
            <a-radio value="customer">客户对账</a-radio>
            <a-radio value="supplier">供应商对账</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="对方" name="targetId" required>
          <a-select
            v-model:value="addForm.targetId"
            :placeholder="addForm.type === 'bank' ? '请选择银行账户' : '请选择对方'"
            show-search
            :filter-option="false"
            :loading="targetLoading"
            option-filter-prop="label"
            style="width: 100%"
            @search="handleTargetSearch"
            @change="handleTargetChange"
          >
            <a-select-option v-for="o in targetOptions" :key="o.value" :value="o.value">
              <span>{{ o.code ? `${o.code} · ` : '' }}{{ o.label }}</span>
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="对账起止" name="dateRange">
          <a-range-picker v-model:value="addForm.dateRange" value-format="YYYY-MM-DD" style="width: 100%" />
        </a-form-item>
        <a-form-item label="系统余额" name="systemBalance">
          <a-input-number v-model:value="addForm.systemBalance" :min="0" :precision="2" style="width: 100%" />
          <div class="balance-hint">按对方自动带出应收/应付/账户余额，可手动调整</div>
        </a-form-item>
        <a-form-item label="实际余额" name="actualBalance">
          <a-input-number v-model:value="addForm.actualBalance" :min="0" :precision="2" style="width: 100%" />
          <div class="balance-hint">外部（银行/客户/供应商）认定的金额</div>
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="addForm.remark" :rows="3" placeholder="备注信息" />
        </a-form-item>
      </a-form>
    </FullScreenDetail>

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldsConfig"
      :function-buttons-config="functionButtonConfig"
      :storage-key="pageConfigStorageKey"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />

  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined, BankOutlined, UserOutlined, TeamOutlined, WarningOutlined,
  ReloadOutlined, SettingOutlined, SearchOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { reconciliationApi } from '@/api/finance'
import { customerApi } from '@/api/customer'
import { supplierApi } from '@/api/supplier'
import { financeAccountApi } from '@/api/md'

defineOptions({ name: 'FinanceReconciliation' })

// ═══ Tab ═══
const tabs = [
  { key: 'bank', label: '银行对账' },
  { key: 'customer', label: '客户对账' },
  { key: 'supplier', label: '供应商对账' },
  { key: 'difference', label: '差异处理' },
]
const activeTab = ref<'bank' | 'customer' | 'supplier' | 'difference'>('bank')

// ═══ 状态 ═══
const loading = ref(false)
const refreshLoading = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const showPageConfig = ref(false)

// ═══ 统计 ═══
const stats = reactive({ bankPending: 0, customerPending: 0, supplierPending: 0, differenceCount: 0 })

async function loadStats() {
  refreshLoading.value = true
  try {
    const res: any = await reconciliationApi.getStats()
    if (res) {
      stats.bankPending = Number(res.bankPending || 0)
      stats.customerPending = Number(res.customerPending || 0)
      stats.supplierPending = Number(res.supplierPending || 0)
      stats.differenceCount = Number(res.differenceCount || 0)
    }
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (err) {
    console.warn('[对账管理] 获取对账统计数据失败', err)
  } finally {
    refreshLoading.value = false
  }
}

// ═══ 自动刷新（30s） ═══
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ═══ 搜索参数 ═══
interface SearchFieldDef {
  key: string
  label: string
  type: 'input' | 'select' | 'dateRange'
  placeholder?: string
  width?: number
  options?: Array<{ label: string; value: string | number }>
}

type QueryFieldSetting = { key: string; label: string; visible: boolean }
type FunctionButtonSetting = { key: string; label: string; enabled: boolean }

const DOC_SEARCH_FIELDS: SearchFieldDef[] = [
  { key: 'reconciliationNo', label: '对账编号', type: 'input', placeholder: '对账编号' },
  { key: 'targetName', label: '对方名称', type: 'input', placeholder: '对方名称' },
  { key: 'period', label: '对账期间', type: 'dateRange' },
  {
    key: 'status', label: '状态', type: 'select',
    options: [
      { label: '待对账', value: 0 },
      { label: '已对账', value: 1 },
      { label: '有差异', value: 2 },
    ],
  },
]

const DIFF_SEARCH_FIELDS: SearchFieldDef[] = [
  { key: 'reconciliationNo', label: '对账编号', type: 'input', placeholder: '对账编号' },
  { key: 'targetName', label: '对方名称', type: 'input', placeholder: '对方名称' },
  {
    key: 'reconciliationType', label: '类型', type: 'select',
    options: [
      { label: '银行', value: 'BANK' },
      { label: '客户', value: 'CUSTOMER' },
      { label: '供应商', value: 'SUPPLIER' },
    ],
  },
]

const searchParams = reactive<Record<string, any>>({
  reconciliationNo: '',
  targetName: '',
  status: undefined,
  reconciliationType: '',
})
const rangeValue = reactive<Record<string, [Dayjs, Dayjs] | null>>({ period: null })

const currentSearchFields = computed(() => (activeTab.value === 'difference' ? DIFF_SEARCH_FIELDS : DOC_SEARCH_FIELDS))
const visibleSearchFields = computed(() => {
  const config = queryFieldsConfig.value
  return currentSearchFields.value.filter(f => {
    const c = config.find(cf => cf.key === f.key)
    return c ? c.visible : true
  })
})

// ═══ 日期范围 ═══
function onRangeChange(key: string, v: [Dayjs, Dayjs] | null) {
  if (key === 'period' && v && v[0] && v[1]) {
    searchParams.startDateStart = v[0].format('YYYY-MM-DD')
    searchParams.startDateEnd = v[1].format('YYYY-MM-DD')
  } else if (key === 'period') {
    searchParams.startDateStart = undefined
    searchParams.startDateEnd = undefined
  }
}

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
}))

// ═══ 列定义：单据类型（bank/customer/supplier） + 差异 ═══
interface ColumnDef {
  title: string
  key: string
  field?: string
  type?: string
  slotName?: string
  width?: number
  fixed?: string
  align?: string
  minWidth?: number
  defaultHidden?: boolean
}

function docColumns(nameLabel: string, sysLabel: string, actLabel: string): ColumnDef[] {
  return [
    { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
    { title: '操作', key: 'action', type: 'action', width: 90, fixed: 'right', slotName: 'actionCell' },
    { title: '对账编号', field: 'reconciliationNo', key: 'reconciliationNo', width: 160 },
    { title: nameLabel, field: 'targetName', key: 'targetName', minWidth: 140 },
    { title: '对账期间', field: 'period', key: 'period', width: 200, slotName: 'periodCell' },
    { title: sysLabel, field: 'systemBalance', key: 'systemBalance', width: 120, align: 'right', slotName: 'systemBalanceCell' },
    { title: actLabel, field: 'actualBalance', key: 'actualBalance', width: 120, align: 'right', slotName: 'actualBalanceCell' },
    { title: '差异', field: 'difference', key: 'difference', width: 110, align: 'right', slotName: 'differenceCell' },
    { title: '状态', field: 'status', key: 'status', width: 90, slotName: 'statusCell' },
    { title: '对账日期', field: 'reconciliationDate', key: 'reconciliationDate', width: 110 },
  ]
}

const BANK_COLUMNS = docColumns('账户名称', '系统余额', '实际余额')
const CUSTOMER_COLUMNS = docColumns('客户名称', '系统应收', '客户金额')
const SUPPLIER_COLUMNS = docColumns('供应商名称', '系统应付', '供应商金额')

const DIFF_COLUMNS: ColumnDef[] = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 44, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 100, fixed: 'right', slotName: 'actionCell' },
  { title: '对账编号', field: 'reconciliationNo', key: 'reconciliationNo', width: 160 },
  { title: '类型', field: 'reconciliationType', key: 'reconciliationType', width: 90, slotName: 'typeCell' },
  { title: '对方名称', field: 'targetName', key: 'targetName', minWidth: 140 },
  { title: '差异金额', field: 'difference', key: 'difference', width: 120, align: 'right', slotName: 'differenceCell' },
  { title: '差异原因', field: 'differenceReason', key: 'differenceReason', minWidth: 160, slotName: 'reasonCell' },
]

// ═══ 列定义（每 Tab 独立，列显隐走数据表表头齿轮） ═══
const currentColumns = computed(() => {
  if (activeTab.value === 'bank') return BANK_COLUMNS
  if (activeTab.value === 'customer') return CUSTOMER_COLUMNS
  if (activeTab.value === 'supplier') return SUPPLIER_COLUMNS
  return DIFF_COLUMNS
})
const tableStorageKey = computed(() => `finance-reconciliation-table-columns-${activeTab.value}`)

function openPageConfig() { showPageConfig.value = true }

// ═══ 页面配置（查询条件显隐 + 功能按钮） ═══
const FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'add', label: '新增对账', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]

const defaultQueryFields = computed<QueryFieldSetting[]>(() =>
  currentSearchFields.value.map(f => ({ key: f.key, label: f.label, visible: true })),
)
const defaultFunctionButtons = computed<FunctionButtonSetting[]>(() => FUNCTION_BUTTONS.map(b => ({ ...b })))

const queryFieldsConfig = ref<QueryFieldSetting[]>([...defaultQueryFields.value])
const functionButtonConfig = ref<FunctionButtonSetting[]>([...defaultFunctionButtons.value])

const pageConfigStorageKey = computed(() => `finance-reconciliation-page-config-${activeTab.value}`)

function handlePageConfigChange(config: any) {
  queryFieldsConfig.value = config.queryFields || defaultQueryFields.value
  functionButtonConfig.value = config.functionButtons || defaultFunctionButtons.value
}

function syncTabConfig() {
  objectReplace(queryFieldsConfig.value, defaultQueryFields.value)
  objectReplace(functionButtonConfig.value, defaultFunctionButtons.value)
}
function objectReplace(target: any[], source: any[]) {
  target.splice(0, target.length, ...source.map(s => ({ ...s })))
}
function fnEnabled(key: string): boolean {
  const c = functionButtonConfig.value.find(f => f.key === key)
  return c ? c.enabled : true
}

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (activeTab.value !== 'difference') {
      params.reconciliationType = activeTab.value.toUpperCase()
    }
    if (searchParams.reconciliationNo) params.reconciliationNo = searchParams.reconciliationNo
    if (searchParams.targetName) params.targetName = searchParams.targetName
    if (searchParams.status !== undefined && searchParams.status !== null && searchParams.status !== '') params.status = searchParams.status
    if (activeTab.value === 'difference') {
      params.status = 2 // 差异记录
      if (searchParams.reconciliationType) params.reconciliationType = searchParams.reconciliationType
    }
    if (searchParams.startDateStart) params.startDateStart = searchParams.startDateStart
    if (searchParams.startDateEnd) params.startDateEnd = searchParams.startDateEnd

    const res: any = await reconciliationApi.page(params)
    tableData.value = (res?.records || []) as any[]
    pagination.total = Number(res?.total || 0)
  } catch (err) {
    console.warn('[对账管理] 加载失败', err)
    message.error('加载对账记录失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() { pagination.current = 1; fetchData() }
function handleRefresh() { loadStats(); fetchData() }
function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

function handleTabChange(key: string) {
  activeTab.value = key as any
  syncTabConfig()
  pagination.current = 1
  fetchData()
}

// ═══ 行操作：对账 ═══
async function handleReconcile(record: any) {
  try {
    await reconciliationApi.reconcile(record.id)
    message.success(`对账成功：${record.reconciliationNo}`)
    fetchData()
    loadStats()
  } catch (err: any) {
    message.error(err?.message || '对账失败')
  }
}

// ═══ 行操作：处理差异（下推应收应付调整） ═══
function handleResolveDifference(record: any) {
  Modal.confirm({
    title: '处理差异',
    content: `确认处理差异记录 ${record.reconciliationNo}（${record.targetName}）？将下推《应收应付调整》并结案。`,
    okText: '确认处理',
    cancelText: '取消',
    onOk: async () => {
      try {
        await reconciliationApi.handleDifference(record.id, record.differenceReason || '对账差异处理')
        message.success('差异已处理')
        fetchData()
        loadStats()
      } catch (err: any) {
        message.error(err?.message || '处理失败')
      }
    },
  })
}

// ═══ 新增对账 ═══
const addVisible = ref(false)
const addLoading = ref(false)
const addFormRef = ref()
const addFormDirty = ref(false)
const targetOptions = ref<{ value: number; label: string; code?: string }[]>([])
const targetLoading = ref(false)
let targetSearchKey = ''

const addForm = reactive({
  type: 'bank' as 'bank' | 'customer' | 'supplier',
  targetId: undefined as number | undefined,
  targetName: '',
  dateRange: [] as string[],
  systemBalance: 0,
  actualBalance: 0,
  remark: '',
})

const typeMap: Record<string, string> = { bank: 'BANK', customer: 'CUSTOMER', supplier: 'SUPPLIER' }

const addFormRules: any = {
  targetId: [{ required: true, message: '请选择对方', trigger: 'change' }],
  dateRange: [{ required: true, message: '请选择对账起止日期', trigger: 'change', type: 'array' }],
}

async function loadTargetOptions() {
  targetLoading.value = true
  targetOptions.value = []
  try {
    if (addForm.type === 'bank') {
      const res: any = await financeAccountApi.getList({ status: 1 })
      const list: any[] = res?.data || res || []
      targetOptions.value = (Array.isArray(list) ? list : []).map((a: any) => ({
        value: a.id,
        label: a.accountName || a.name || '',
        code: a.bankAccount || '',
      }))
    } else if (addForm.type === 'customer') {
      const res: any = await customerApi.getPage({ pageNum: 1, pageSize: 100, name: targetSearchKey || undefined })
      targetOptions.value = (res?.records || []).map((c: any) => ({ value: c.id, label: c.customerName }))
    } else if (addForm.type === 'supplier') {
      const res: any = await supplierApi.page({ pageNum: 1, pageSize: 100, keyword: targetSearchKey || undefined })
      targetOptions.value = (res?.records || []).map((s: any) => ({ value: s.id, label: s.supplierName }))
    }
  } catch (err) {
    console.warn('[对账管理] 加载对方选项失败', err)
  } finally {
    targetLoading.value = false
  }
}

async function handleTargetSearch(keyword: string) {
  targetSearchKey = keyword
  loadTargetOptions()
}

function handleTypeChange() {
  addForm.targetId = undefined
  addForm.targetName = ''
  addForm.systemBalance = 0
  targetSearchKey = ''
  loadTargetOptions()
}

async function handleTargetChange(val: number) {
  addForm.targetId = val
  const opt = targetOptions.value.find((o) => o.value === val)
  addForm.targetName = opt?.label || ''
  // 自动带出系统余额（应收/应付/银行账户余额）
  try {
    const res: any = await reconciliationApi.getBalance(typeMap[addForm.type], val)
    if (res && (res.balance !== null && res.balance !== undefined)) {
      addForm.systemBalance = Number(res.balance) || 0
    }
  } catch (err) {
    console.warn('[对账管理] 获取余额失败', err)
  }
}

function handleAdd() {
  addForm.type = activeTab.value === 'difference' ? 'bank' : activeTab.value
  addForm.targetId = undefined
  addForm.targetName = ''
  addForm.dateRange = []
  addForm.systemBalance = 0
  addForm.actualBalance = 0
  addForm.remark = ''
  addVisible.value = true
  addFormDirty.value = false
  handleTypeChange()
  setTimeout(() => { addFormDirty.value = true }, 500)
}

async function handleAddConfirm() {
  try {
    await addFormRef.value?.validate()
    addLoading.value = true
    await reconciliationApi.create({
      reconciliationType: typeMap[addForm.type],
      targetId: addForm.targetId,
      targetName: addForm.targetName || '',
      startDate: addForm.dateRange[0],
      endDate: addForm.dateRange[1],
      systemBalance: addForm.systemBalance,
      actualBalance: addForm.actualBalance,
      remark: addForm.remark || '',
    })
    message.success('对账创建成功')
    addVisible.value = false
    loadStats()
    fetchData()
    window.dispatchEvent(new Event('finance:create'))
  } catch (err: any) {
    if (err?.message) message.error(err.message)
  } finally {
    addLoading.value = false
  }
}
function handleAddCancel() { addVisible.value = false }

// ═══ 快捷键 F5 / Ctrl+N ═══
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); handleRefresh(); return }
  if (e.ctrlKey && e.key === 'n') { e.preventDefault(); handleAdd(); return }
}

// ═══ 工具 ═══
const fmt = (v: number | null | undefined) => {
  if (v === null || v === undefined) return '0.00'
  return Number(v).toFixed(2)
}
const getStatusColor = (status: number) => ({ 0: 'warning', 1: 'success', 2: 'error' }[status] || 'default')
const getStatusText = (status: number) => ({ 0: '待对账', 1: '已对账', 2: '有差异' }[status] || '未知')
const getTypeText = (type: string) => ({ BANK: '银行', CUSTOMER: '客户', SUPPLIER: '供应商' }[type] || type || '未知')

// 页脚合计（差异列）
const footerColumns = computed(() => {
  const total = tableData.value.reduce((s, r) => s + Number(r.difference || 0), 0)
  return [{ key: 'difference', value: total, highlight: true }]
})

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }

// ═══ 生命周期 ═══
onMounted(() => {
  loadStats()
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('finance:create', handleReconcileRefresh)
  window.addEventListener('finance:refresh', handleRefresh)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    loadStats()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('finance:create', handleReconcileRefresh)
  window.removeEventListener('finance:refresh', handleRefresh)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

function handleReconcileRefresh() { fetchData() }
</script>

<style scoped>
/* 统计卡片条 */
.recon-stat-strip {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  background: #f0f2f5;
  flex-shrink: 0;
}
.recon-stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border-radius: 8px;
  min-width: 150px;
}
.stat-bank { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-customer { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-supplier { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-difference { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }
.recon-stat-value { font-size: 20px; font-weight: 600; color: #333; font-family: 'SFMono-Regular', Consolas, monospace; }
.recon-stat-label { font-size: 12px; color: #666; margin-top: 2px; }
.recon-stat-icon { font-size: 22px; color: rgba(0, 0, 0, 0.15); }

.refresh-countdown { margin-left: 4px; color: #909399; font-size: 12px; }

/* 搜索区 */
.recon-search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.recon-search-grid { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.recon-search-item { display: flex; align-items: center; min-width: 0; }
.recon-search-item :deep(.ant-input),
.recon-search-item :deep(.ant-select),
.recon-search-item :deep(.ant-picker) { font-size: 13px; }
.recon-search-item :deep(.ant-input) { width: 170px; }
.recon-select-wrap { display: flex; align-items: center; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; }
.recon-select-wrap:hover { border-color: #4096ff; }
.recon-select-label { font-size: 13px; color: rgba(0,0,0,0.65); white-space: nowrap; flex-shrink: 0; padding-left: 8px; }
.recon-select-wrap :deep(.ant-select) { flex: 1; min-width: 110px; }
.recon-select-wrap :deep(.ant-select .ant-select-selector) { border: none !important; box-shadow: none !important; padding-top: 0 !important; padding-bottom: 0 !important; display: flex; align-items: center; }

.num-value { font-family: 'SFMono-Regular', Consolas, monospace; font-variant-numeric: tabular-nums; }
.diff-red { color: #f5222d; }
.diff-green { color: #52c41a; }
.no-action { color: #bbb; }
.balance-hint { font-size: 12px; color: #999; margin-top: 2px; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }

@media (max-width: 768px) {
  .recon-stat-strip { flex-wrap: wrap; }
  .recon-stat-card { flex: 1 1 45%; }
}
</style>
