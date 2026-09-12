<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <CategoryListLayout
        :tabs="tabs"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="handleTabChange"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 快捷日期 ═══ -->
        <template #toolbar-left>
          <div class="query-scheme-wrap">
            <a-select
              v-model:value="queryScheme"
              style="width: 140px"
              size="small"
              placeholder="--查询方案--"
            >
              <a-select-option value="">
                --查询方案--
              </a-select-option>
            </a-select>
            <a-button
              type="link"
              size="small"
              style="padding: 0 4px"
            >
              <PlusOutlined />
            </a-button>
          </div>
          <a-space :size="4" class="quick-dates">
            <a-button
              v-for="d in quickDates"
              :key="d.key"
              :type="quickDate === d.key ? 'primary' : 'link'"
              size="small"
              @click="setQuickDate(d.key)"
            >
              {{ d.label }}
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：列配置/页面配置/新增/刷新/批量/打印/导出 ═══ -->
        <!-- 列配置走数据表表头齿轮（BillDetailTable 内置），工具栏不再放重复入口 -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-tooltip v-if="btnEnabled('config')" title="页面配置">
              <a-button
                size="small"
                @click="showPageConfig = true"
              >
                <SettingOutlined />
              </a-button>
            </a-tooltip>
            <a-dropdown v-if="btnEnabled('batch') && selectedRowKeys.length > 0">
              <a-button size="small">
                批量操作 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu @click="handleBatchAction">
                  <a-menu-item key="submit">
                    批量提交审批
                  </a-menu-item>
                  <a-menu-item key="approve">
                    批量审批通过
                  </a-menu-item>
                  <a-menu-item key="reject">
                    批量驳回
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item
                    key="delete"
                    danger
                  >
                    批量删除
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-button
              v-if="btnEnabled('add')"
              type="primary"
              size="small"
              @click="handleAdd"
            >
              <PlusOutlined /> 新增
            </a-button>
            <a-button
              v-if="btnEnabled('refresh')"
              size="small"
              @click="fetchData"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button
              v-if="btnEnabled('printF8')"
              size="small"
              @click="handlePrintF8"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="btnEnabled('export')"
              size="small"
              @click="handleExport"
            >
              <ExportOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 搜索区域（12 查询条件） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-container">
              <div
                ref="searchGridRef"
                class="search-grid"
              >
                <div class="search-field-item">
                  <a-range-picker
                    v-model:value="dateRange"
                    size="small"
                    style="width: 100%"
                    @change="handleDateChange"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.budgetNo"
                    placeholder="单据编号"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.departmentName"
                    placeholder="部门"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">财政年度</span>
                    <a-select
                      v-model:value="searchParams.fiscalYear"
                      size="small"
                      allow-clear
                    >
                      <a-select-option
                        v-for="y in YEAR_OPTIONS"
                        :key="y.value"
                        :value="y.value"
                      >
                        {{ y.label }}
                      </a-select-option>
                    </a-select>
                  </div>
                </div>
                <div class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">单据状态</span>
                    <a-select
                      v-model:value="searchParams.status"
                      size="small"
                      allow-clear
                    >
                      <a-select-option value="">
                        全部
                      </a-select-option>
                      <a-select-option
                        v-for="(v, k) in STATUS_MAP"
                        :key="k"
                        :value="k"
                      >
                        {{ v.text }}
                      </a-select-option>
                    </a-select>
                  </div>
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.creatorName"
                    placeholder="制单人"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.handlerName"
                    placeholder="经手人"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.auditorName"
                    placeholder="审核人"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.description"
                    placeholder="摘要"
                    allow-clear
                    size="small"
                  />
                </div>
                <div class="search-field-item">
                  <a-input
                    v-model:value="searchParams.remark"
                    placeholder="单据备注"
                    allow-clear
                    size="small"
                  />
                </div>
                <div
                  ref="searchActionRef"
                  class="search-action-group"
                  :style="{ gridColumn: 'span ' + actionSpan }"
                >
                  <div class="search-field-item search-action-item">
                    <a-button
                      type="primary"
                      size="small"
                      @click="handleSearch"
                    >
                      查询
                    </a-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 表格区域 ═══ -->
        <template #table>
          <div class="table-area">
            <BillTableList
              :columns="allColumns"
              :data-source="tableData"
              :storage-key="'budget-plan-table-columns'"
              :loading="loading"
              :pagination="billPagination"
              :show-toolbar="false"
              :show-search="false"
              :show-add="false"
              :show-export="false"
              :show-batch-delete="false"
              :selectable="true"
              :row-selection="rowSelection"
              :summary-columns="tableFooterColumns"
              row-key="id"
              @page-change="handlePageChange"
            >
              <template #budgetNoCell="{ record }">
                <a-button
                  type="link"
                  size="small"
                  @click="handleView(record)"
                >
                  {{ record.budgetNo }}
                </a-button>
              </template>
              <template #statusCell="{ record }">
                <a-tag :color="getStatusColor(record.status)">
                  {{ getStatusText(record.status) }}
                </a-tag>
              </template>
              <template #totalAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.totalAmount) }}</span>
              </template>
              <template #totalApprovedAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.totalApprovedAmount) }}</span>
              </template>
              <template #totalUsedAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.totalUsedAmount) }}</span>
              </template>
              <template #totalFrozenAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.totalFrozenAmount) }}</span>
              </template>
              <template #totalRemainingAmountCell="{ record }">
                <span class="currency-value">{{ formatAmount(record.totalRemainingAmount) }}</span>
              </template>
              <template #executionRateCell="{ record }">
                <a-tag :color="getRateColor(record.executionRate)">
                  {{ formatRate(record.executionRate) }}
                </a-tag>
              </template>
              <template #printCountCell="{ record }">
                <span class="currency-value">{{ record.printCount ?? 0 }}</span>
              </template>
              <template #actionCell="{ record }">
                <a-space :size="4">
                  <a-button
                    type="link"
                    size="small"
                    @click="handleView(record)"
                  >
                    查看
                  </a-button>
                  <a-button
                    v-if="['draft', 'rejected'].includes(record.status)"
                    type="link"
                    size="small"
                    @click="handleEdit(record)"
                  >
                    修改
                  </a-button>
                  <a-button
                    v-if="['draft', 'rejected'].includes(record.status)"
                    type="link"
                    size="small"
                    @click="handleSubmit(record)"
                  >
                    提交
                  </a-button>
                  <template v-if="record.status === 'submitted'">
                    <a-button
                      type="link"
                      size="small"
                      @click="handleApprove(record)"
                    >
                      通过
                    </a-button>
                    <a-button
                      type="link"
                      size="small"
                      danger
                      @click="handleReject(record)"
                    >
                      驳回
                    </a-button>
                  </template>
                  <a-button
                    v-if="record.status === 'approved'"
                    type="link"
                    size="small"
                    @click="handleStartExec(record)"
                  >
                    开始执行
                  </a-button>
                  <a-button
                    v-if="record.status === 'executing'"
                    type="link"
                    size="small"
                    @click="handleClose(record)"
                  >
                    关闭
                  </a-button>
                </a-space>
              </template>
            </BillTableList>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置弹窗 ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="activeQueryFields"
      :function-buttons-config="functionButtonConfig"
      :storage-key="pageConfigStorageKey"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import {
  PlusOutlined, ReloadOutlined, PrinterOutlined, SettingOutlined,
  ExportOutlined, DownOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { annualBudgetApi } from '@/api/budget'
import { useUserStore } from '@/stores/user'

defineOptions({ name: 'BudgetPlanIndex' })

const router = useRouter()
const userStore = useUserStore()

// ═══ Tab 配置（单 Tab：预算编制-历史） ═══
const tabs = [{ key: 'doc', label: '预算编制-历史' }]
const activeTab = ref('doc')

// ═══ 快捷日期 ═══
const quickDates = [
  { key: 'yesterday', label: '昨日' },
  { key: 'today', label: '今日' },
  { key: 'week', label: '本周' },
  { key: 'lastWeek', label: '近一周' },
  { key: 'month', label: '本月' },
  { key: 'lastMonth', label: '上月' },
  { key: 'last3Month', label: '近三月' },
  { key: 'year', label: '本年' },
]
const quickDate = ref('year')
const queryScheme = ref('')

// ═══ 财政年度 ═══
const currentYear = new Date().getFullYear()
const YEAR_OPTIONS = [0, 1, 2, 3].map(i => {
  const y = currentYear - i
  return { label: `${y}年`, value: y }
})

// ═══ 状态 ═══
const loading = ref(false)
const tableData = ref<any[]>([])

const searchGridRef = ref<HTMLElement | null>(null)
const searchActionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(searchActionRef, searchGridRef)

// ═══ 日期范围 ═══
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().startOf('year'), dayjs()])

// ═══ 搜索参数（12 查询条件） ═══
const searchParams = reactive({
  budgetNo: '',
  departmentName: '',
  fiscalYear: undefined as number | undefined,
  status: undefined as string | undefined,
  creatorName: '',
  handlerName: '',
  auditorName: '',
  description: '',
  remark: '',
  dateStart: dayjs().startOf('year').format('YYYY-MM-DD'),
  dateEnd: dayjs().format('YYYY-MM-DD'),
})

// ═══ 分页 ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
}))

// ═══ 多选状态 ═══
const selectedRowKeys = ref<any[]>([])
const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: any[]) => { selectedRowKeys.value = keys },
}))

// ═══ 页面配置弹窗（列配置走数据表表头齿轮，storage-key=budget-plan-table-columns） ═══
const showPageConfig = ref(false)

// ═══ 页面配置（查询条件显隐、功能按钮） ═══
const PAGE_CONFIG_STORAGE_KEY = 'budget-plan-page-config'
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }
interface PageConfigData { queryFields: QueryFieldSetting[]; functionButtons: FunctionButtonSetting[] }

const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '编制日期', visible: true },
  { key: 'budgetNo', label: '单据编号', visible: true },
  { key: 'departmentName', label: '部门', visible: true },
  { key: 'fiscalYear', label: '财政年度', visible: true },
  { key: 'status', label: '单据状态', visible: true },
  { key: 'creatorName', label: '制单人', visible: true },
  { key: 'handlerName', label: '经手人', visible: false },
  { key: 'auditorName', label: '审核人', visible: false },
  { key: 'description', label: '摘要', visible: false },
  { key: 'remark', label: '单据备注', visible: false },
]

const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'add', label: '新增', enabled: true },
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'batch', label: '批量操作', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'config', label: '配置', enabled: true },
]

const queryConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(f => ({ ...f })))

const activeQueryFields = computed(() => queryConfig.value)
const pageConfigStorageKey = computed(() => PAGE_CONFIG_STORAGE_KEY)

/** 页面配置「功能按钮」开关生效：未配置默认显示 */
function btnEnabled(key: string): boolean {
  return functionButtonConfig.value.find(b => b.key === key)?.enabled !== false
}

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw) as PageConfigData
      if (parsed.queryFields) {
        queryConfig.value = DEFAULT_QUERY_FIELDS.map(df => {
          const saved = parsed.queryFields!.find((f: QueryFieldSetting) => f.key === df.key)
          return saved ? { ...df, ...saved } : { ...df }
        })
      }
      if (parsed.functionButtons) {
        functionButtonConfig.value = DEFAULT_FUNCTION_BUTTONS.map(bf => {
          const saved = parsed.functionButtons!.find((f: FunctionButtonSetting) => f.key === bf.key)
          return saved ? { ...bf, ...saved } : { ...bf }
        })
      }
    }
  } catch { /* ignore */ }
}

function handlePageConfigChange(config: any) {
  localStorage.setItem(PAGE_CONFIG_STORAGE_KEY, JSON.stringify({
    queryFields: config.queryFields || [],
    functionButtons: config.functionButtons || functionButtonConfig.value,
  }))
  loadPageConfig()
}

// ═══ 列定义（24 列，默认显示 12 列） ═══
const allColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '操作', key: 'action', type: 'action', width: 200, fixed: 'right', slotName: 'actionCell' },
  { title: '单据编号', field: 'budgetNo', key: 'budgetNo', width: 170, type: 'slot', slotName: 'budgetNoCell', sortable: true },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '编制日期', field: 'budgetDate', key: 'budgetDate', width: 110, sortable: true },
  { title: '财政年度', field: 'fiscalYear', key: 'fiscalYear', width: 90, align: 'center' },
  { title: '部门', field: 'departmentName', key: 'departmentName', width: 140, sortable: true },
  { title: '预算总额', field: 'totalAmount', key: 'totalAmount', width: 130, align: 'right', type: 'slot', slotName: 'totalAmountCell', sortable: true },
  { title: '批准金额', field: 'totalApprovedAmount', key: 'totalApprovedAmount', width: 130, align: 'right', type: 'slot', slotName: 'totalApprovedAmountCell', defaultHidden: true },
  { title: '已执行', field: 'totalUsedAmount', key: 'totalUsedAmount', width: 130, align: 'right', type: 'slot', slotName: 'totalUsedAmountCell', sortable: true },
  { title: '冻结金额', field: 'totalFrozenAmount', key: 'totalFrozenAmount', width: 120, align: 'right', type: 'slot', slotName: 'totalFrozenAmountCell', defaultHidden: true },
  { title: '剩余额度', field: 'totalRemainingAmount', key: 'totalRemainingAmount', width: 130, align: 'right', type: 'slot', slotName: 'totalRemainingAmountCell', sortable: true },
  { title: '执行率', field: 'executionRate', key: 'executionRate', width: 90, align: 'center', type: 'slot', slotName: 'executionRateCell' },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 90, sortable: true },
  { title: '预算模板', field: 'templateName', key: 'templateName', width: 150, defaultHidden: true },
  { title: '摘要', field: 'description', key: 'description', width: 200, defaultHidden: true },
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 90, defaultHidden: true },
  { title: '审核人', field: 'auditorName', key: 'auditorName', width: 90, defaultHidden: true },
  { title: '审核时间', field: 'auditTime', key: 'auditTime', width: 150, defaultHidden: true },
  { title: '审批意见', field: 'auditRemark', key: 'auditRemark', width: 180, defaultHidden: true },
  { title: '单据备注', field: 'remark', key: 'remark', width: 140, defaultHidden: true },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 150, defaultHidden: true },
  { title: '打印次数', field: 'printCount', key: 'printCount', width: 90, align: 'right', type: 'slot', slotName: 'printCountCell' },
]

// ═══ 状态映射 ═══
const STATUS_MAP: Record<string, { text: string; color: string }> = {
  draft: { text: '草稿', color: 'default' },
  submitted: { text: '待审批', color: 'orange' },
  approved: { text: '已审批', color: 'blue' },
  rejected: { text: '已驳回', color: 'red' },
  executing: { text: '执行中', color: 'green' },
  closed: { text: '已关闭', color: 'default' },
}
function getStatusText(status: string): string {
  return STATUS_MAP[status]?.text || status || '未知'
}
function getStatusColor(status: string): string {
  return STATUS_MAP[status]?.color || 'default'
}
function getRateColor(rate: any): string {
  const v = Number(rate) || 0
  if (v >= 100) return 'red'
  if (v >= 80) return 'orange'
  return 'green'
}

// ═══ 表格底部合计 ═══
const tableFooterColumns = computed(() => {
  const totalAmount = tableData.value.reduce((s: number, r: any) => s + (Number(r.totalAmount) || 0), 0)
  const totalUsed = tableData.value.reduce((s: number, r: any) => s + (Number(r.totalUsedAmount) || 0), 0)
  return [
    { key: 'totalAmount', value: totalAmount, highlight: true },
    { key: 'totalUsedAmount', value: totalUsed },
  ]
})

// ═══ 数据加载 ═══
async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchParams.budgetNo) params.budgetNo = searchParams.budgetNo
    if (searchParams.departmentName) params.departmentName = searchParams.departmentName
    if (searchParams.fiscalYear) params.fiscalYear = searchParams.fiscalYear
    if (searchParams.status !== undefined && searchParams.status !== '') params.status = searchParams.status
    if (searchParams.creatorName) params.creatorName = searchParams.creatorName
    if (searchParams.handlerName) params.handlerName = searchParams.handlerName
    if (searchParams.auditorName) params.auditorName = searchParams.auditorName
    if (searchParams.description) params.description = searchParams.description
    if (searchParams.remark) params.remark = searchParams.remark
    if (searchParams.dateStart) params.dateStart = searchParams.dateStart
    if (searchParams.dateEnd) params.dateEnd = searchParams.dateEnd

    const res: any = await annualBudgetApi.getPage(params)
    tableData.value = res?.records || []
    pagination.total = Number(res?.total) || 0
  } catch (error: any) {
    console.warn('[预算编制] 获取列表失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

// ═══ 事件处理 ═══
function handleTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
  fetchData()
}

function setQuickDate(key: string) {
  quickDate.value = key
  const now = dayjs()
  let start: Dayjs, end: Dayjs
  switch (key) {
    case 'yesterday': start = now.subtract(1, 'day'); end = now.subtract(1, 'day'); break
    case 'today': start = now; end = now; break
    case 'week': start = now.startOf('week'); end = now; break
    case 'lastWeek': start = now.subtract(7, 'day'); end = now; break
    case 'month': start = now.startOf('month'); end = now; break
    case 'lastMonth': start = now.subtract(1, 'month').startOf('month'); end = now.subtract(1, 'month').endOf('month'); break
    case 'last3Month': start = now.subtract(3, 'month'); end = now; break
    case 'year': start = now.startOf('year'); end = now; break
    default: start = now.startOf('year'); end = now
  }
  dateRange.value = [start, end]
  searchParams.dateStart = start.format('YYYY-MM-DD')
  searchParams.dateEnd = end.format('YYYY-MM-DD')
  handleSearch()
}

function handleDateChange(dates: [Dayjs, Dayjs] | null) {
  if (dates && dates.length === 2) {
    searchParams.dateStart = dates[0]?.format('YYYY-MM-DD') || ''
    searchParams.dateEnd = dates[1]?.format('YYYY-MM-DD') || ''
  } else {
    searchParams.dateStart = ''
    searchParams.dateEnd = ''
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ═══ 操作 ═══
function currentOperator() {
  return {
    id: userStore?.userId || userStore?.userInfo?.id || 0,
    name: userStore?.userInfo?.nickname || userStore?.userInfo?.username || '系统',
  }
}

function handleAdd() {
  router.push('/finance/budget-plan/form')
}
function handleView(record: any) {
  router.push(`/finance/budget-plan/form?id=${record.id}`)
}
function handleEdit(record: any) {
  router.push(`/finance/budget-plan/form?id=${record.id}`)
}

function handleSubmit(record: any) {
  Modal.confirm({
    title: '提交审批',
    content: `确定提交预算编制单 ${record.budgetNo} 进入审批？`,
    okText: '确认提交',
    cancelText: '取消',
    onOk: async () => {
      try {
        await annualBudgetApi.submit(record.id)
        message.success('已提交审批')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '提交失败')
      }
    },
  })
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '审批通过',
    content: `确定审批通过预算编制单 ${record.budgetNo}？通过后预算可转入执行。`,
    okText: '确认通过',
    cancelText: '取消',
    onOk: async () => {
      try {
        const u = currentOperator()
        await annualBudgetApi.batchApprove([record.id], { auditorId: u.id, auditorName: u.name })
        message.success('已审批通过')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '审批失败')
      }
    },
  })
}

function handleReject(record: any) {
  Modal.confirm({
    title: '驳回预算',
    content: `确定驳回预算编制单 ${record.budgetNo}？`,
    okText: '确认驳回',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        const u = currentOperator()
        await annualBudgetApi.batchReject([record.id], { auditorId: u.id, auditorName: u.name })
        message.success('已驳回')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '驳回失败')
      }
    },
  })
}

function handleStartExec(record: any) {
  Modal.confirm({
    title: '开始执行',
    content: `确定开始执行预算编制单 ${record.budgetNo}？执行后转入《预算执行》跟踪。`,
    okText: '确认执行',
    cancelText: '取消',
    onOk: async () => {
      try {
        await annualBudgetApi.startExec(record.id)
        message.success('已开始执行')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '操作失败')
      }
    },
  })
}

function handleClose(record: any) {
  Modal.confirm({
    title: '关闭预算',
    content: `确定关闭预算编制单 ${record.budgetNo}？`,
    okText: '确认关闭',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await annualBudgetApi.close(record.id)
        message.success('已关闭')
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '操作失败')
      }
    },
  })
}

async function handleBatchAction({ key }: { key: string }) {
  const ids = selectedRowKeys.value.map(k => Number(k))
  if (ids.length === 0) {
    message.warning('请先选择单据')
    return
  }
  const labels: Record<string, string> = {
    submit: '批量提交审批',
    approve: '批量审批通过',
    reject: '批量驳回',
    delete: '批量删除',
  }
  Modal.confirm({
    title: labels[key] || '批量操作',
    content: `确定对已选中的 ${ids.length} 张单据执行「${labels[key]}」？`,
    okText: '确认',
    okType: key === 'delete' || key === 'reject' ? 'danger' : 'primary',
    cancelText: '取消',
    onOk: async () => {
      try {
        const u = currentOperator()
        let affected = 0
        if (key === 'submit') affected = Number((await annualBudgetApi.batchSubmit(ids) as any)?.data) || 0
        else if (key === 'approve') affected = Number((await annualBudgetApi.batchApprove(ids, { auditorId: u.id, auditorName: u.name }) as any)?.data) || 0
        else if (key === 'reject') affected = Number((await annualBudgetApi.batchReject(ids, { auditorId: u.id, auditorName: u.name }) as any)?.data) || 0
        else affected = Number((await annualBudgetApi.batchDelete(ids) as any)?.data) || 0
        message.success(`${labels[key]}完成，成功 ${affected} 张`)
        selectedRowKeys.value = []
        fetchData()
      } catch (error: any) {
        message.error(error?.response?.data?.message || '批量操作失败')
      }
    },
  })
}

function handlePrintF8() {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要打印的预算编制单')
    return
  }
  const id = selectedRowKeys.value[0]
  annualBudgetApi.printDoc(Number(id)).catch(() => { /* 打印计数失败不阻塞 */ })
  router.push(`/finance/budget-plan/form?id=${id}`)
}

async function handleExport() {
  try {
    const res: any = await annualBudgetApi.getPage({
      pageNum: 1,
      pageSize: 9999,
      budgetNo: searchParams.budgetNo || undefined,
      departmentName: searchParams.departmentName || undefined,
      fiscalYear: searchParams.fiscalYear || undefined,
      status: searchParams.status || undefined,
      dateStart: searchParams.dateStart || undefined,
      dateEnd: searchParams.dateEnd || undefined,
    })
    const data = res?.records || []
    if (data.length === 0) {
      message.warning('没有可导出的数据')
      return
    }
    const headers = ['单据编号', '单据状态', '编制日期', '财政年度', '部门', '预算总额', '已执行', '冻结金额', '剩余额度', '执行率', '经手人']
    const rows = data.map((r: any) => [
      r.budgetNo, getStatusText(r.status), r.budgetDate, r.fiscalYear, r.departmentName,
      r.totalAmount, r.totalUsedAmount, r.totalFrozenAmount, r.totalRemainingAmount,
      formatRate(r.executionRate), r.handlerName,
    ])
    const csv = [headers.join(','), ...rows.map((r: any[]) => r.join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `预算编制_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
    a.click()
    URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error: any) {
    message.error(error?.response?.data?.message || '导出失败')
  }
}

const handleError = (error: Error) => {
  console.error('[预算编制] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

function formatAmount(amount: any): string {
  if (amount === undefined || amount === null || amount === '') return '0.00'
  const n = Number(amount)
  if (isNaN(n)) return '0.00'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatRate(rate: any): string {
  if (rate === undefined || rate === null || rate === '') return '0.0%'
  const n = Number(rate)
  if (isNaN(n)) return '0.0%'
  return `${n.toFixed(1)}%`
}

// ═══ 初始化 ═══
onMounted(() => {
  loadPageConfig()
  setQuickDate('year')
})
</script>

<style scoped>
.query-scheme-wrap { display: flex; align-items: center; gap: 2px; }
.quick-dates :deep(.ant-btn) { font-size: 13px; padding: 2px 8px; }
.quick-dates :deep(.ant-btn-primary) { color: #fff; background: #ff7a45; border-color: #ff7a45; }
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-container > .search-grid { max-height: 80px; overflow: hidden; }
.search-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; align-items: center; }
.search-field-item { display: flex; min-width: 0; }
.search-field-item :deep(.ant-input-wrapper),
.search-field-item :deep(.ant-input-affix-wrapper) { width: 100%; font-size: 13px; }
.search-field-item :deep(.ant-select) { width: 100%; }
.search-field-item :deep(.ant-select .ant-select-selector) { font-size: 13px; }
.search-field-item :deep(.ant-picker) { width: 100%; }
.search-select-wrap { display: flex; align-items: center; width: 100%; border: 1px solid #d9d9d9; border-radius: 4px; background: #fff; }
.search-select-wrap:hover { border-color: #4096ff; }
.search-select-label { font-size: 13px; color: rgba(0,0,0,0.65); white-space: nowrap; flex-shrink: 0; padding-left: 8px; }
.search-select-wrap :deep(.ant-select) { flex: 1; min-width: 0; }
.search-select-wrap :deep(.ant-select .ant-select-selector) { border: none !important; border-radius: 0 !important; box-shadow: none !important; padding-top: 0 !important; padding-bottom: 0 !important; display: flex; align-items: center; }
.search-action-item { flex-shrink: 0; }
.search-action-group { display: flex; flex-wrap: nowrap; align-items: center; }
.search-action-group .search-field-item { width: auto; flex: 0 0 auto; margin-right: 4px; }
.search-action-group .search-field-item:last-child { margin-right: 0; }
.table-area { flex: 1; min-height: 0; overflow: hidden; }
.currency-value { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; }
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
