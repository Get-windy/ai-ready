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
              <a-select-option value="">--查询方案--</a-select-option>
            </a-select>
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

        <!-- ═══ 工具栏右侧：页面配置/图表/刷新/导出（列配置见表头齿轮） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button size="small" @click="showPageConfig = true">
              <SettingOutlined />
            </a-button>
            <!-- 提示下置（placement=bottom）避免浮层压住按钮/标签栏；0.4s 延迟避免扫过工具栏时误弹 -->
            <a-tooltip
              :title="showCharts ? '隐藏图表' : '显示图表'"
              placement="bottom"
              :mouse-enter-delay="0.4"
              :mouse-leave-delay="0.05"
            >
              <a-button :type="showCharts ? 'primary' : 'default'" size="small" @click="toggleCharts">
                <BarChartOutlined />
              </a-button>
            </a-tooltip>
            <a-button v-if="btnEnabled('refresh')" size="small" @click="handleSearch">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button v-if="btnEnabled('export')" size="small" @click="handleExport">
              <ExportOutlined /> 导出
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 搜索区域（按页面配置动态渲染） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-container">
              <div ref="searchGridRef" class="search-grid">
                <div v-if="fieldVisible('date')" class="search-field-item">
                  <a-range-picker
                    v-model:value="dateRange"
                    size="small"
                    style="width:100%"
                    @change="handleDateChange"
                  />
                </div>
                <div v-if="fieldVisible('docNo')" class="search-field-item">
                  <a-input
                    v-model:value="search.docNo"
                    placeholder="单据编号"
                    allow-clear
                    size="small"
                    @press-enter="handleSearch"
                  />
                </div>
                <div v-if="fieldVisible('partnerName')" class="search-field-item">
                  <a-input
                    v-model:value="search.partnerName"
                    placeholder="往来单位"
                    allow-clear
                    size="small"
                    @press-enter="handleSearch"
                  />
                </div>
                <div v-if="fieldVisible('handlerName')" class="search-field-item">
                  <a-input
                    v-model:value="search.handlerName"
                    placeholder="经手人"
                    allow-clear
                    size="small"
                    @press-enter="handleSearch"
                  />
                </div>
                <div v-if="fieldVisible('deptName')" class="search-field-item">
                  <a-input
                    v-model:value="search.deptName"
                    placeholder="部门"
                    allow-clear
                    size="small"
                    @press-enter="handleSearch"
                  />
                </div>
                <div v-if="fieldVisible('expenseName')" class="search-field-item">
                  <a-input
                    v-model:value="search.expenseName"
                    placeholder="费用名称"
                    allow-clear
                    size="small"
                    @press-enter="handleSearch"
                  />
                </div>
                <div v-if="fieldVisible('creatorName')" class="search-field-item">
                  <a-input
                    v-model:value="search.creatorName"
                    placeholder="制单人"
                    allow-clear
                    size="small"
                    @press-enter="handleSearch"
                  />
                </div>
                <div v-if="fieldVisible('expenseType')" class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">费用类别</span>
                    <a-select v-model:value="search.expenseType" size="small" allow-clear>
                      <a-select-option value="">全部</a-select-option>
                      <a-select-option value="0">往来单位费用</a-select-option>
                      <a-select-option value="1">内部费用</a-select-option>
                    </a-select>
                  </div>
                </div>
                <div v-if="fieldVisible('approvalStatus')" class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">审批状态</span>
                    <a-select v-model:value="search.approvalStatus" size="small" allow-clear>
                      <a-select-option value="">全部</a-select-option>
                      <a-select-option value="0">未提交</a-select-option>
                      <a-select-option value="1">审批中</a-select-option>
                      <a-select-option value="2">审批通过</a-select-option>
                      <a-select-option value="3">审批驳回</a-select-option>
                    </a-select>
                  </div>
                </div>
                <div v-if="fieldVisible('status')" class="search-field-item">
                  <div class="search-select-wrap">
                    <span class="search-select-label">单据状态</span>
                    <a-select v-model:value="search.status" size="small" allow-clear>
                      <a-select-option value="">全部</a-select-option>
                      <a-select-option value="0">草稿</a-select-option>
                      <a-select-option value="1">已记账</a-select-option>
                      <a-select-option value="2">已取消</a-select-option>
                    </a-select>
                  </div>
                </div>
                <div v-if="fieldVisible('summary')" class="search-field-item">
                  <a-input
                    v-model:value="search.summary"
                    placeholder="摘要"
                    allow-clear
                    size="small"
                    @press-enter="handleSearch"
                  />
                </div>
                <div
                  ref="searchActionRef"
                  class="search-action-group"
                  :style="{ gridColumn: 'span ' + actionSpan }"
                >
                  <div class="search-field-item search-action-item">
                    <a-button type="primary" size="small" @click="handleSearch">
                      查询
                    </a-button>
                  </div>
                  <div class="search-field-item">
                    <a-button size="small" @click="handleReset">
                      重置
                    </a-button>
                  </div>
                  <div v-if="fieldVisible('showRed')" class="search-field-item">
                    <a-checkbox v-model:checked="search.showRed">显示红冲</a-checkbox>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 统计卡片 + 图表 + 统计表 ═══ -->
        <template #table>
          <div class="stats-body">
            <ARStatCards :items="statCards" :loading="loading" class="stats-cards" />

            <!-- 口径说明条：申请口径 vs 记账（实付）口径，与《费用单/费用审批》同一数据源 -->
            <div class="stats-meta">
              <span class="meta-item">
                <span class="meta-label">已记账（实付）</span>
                <span class="meta-value">¥{{ formatMoney(summary.paidAmount) }}</span>
              </span>
              <span class="meta-item">
                <span class="meta-label">未记账（待付）</span>
                <span class="meta-value">¥{{ formatMoney(summary.unpaidAmount) }}</span>
              </span>
              <span class="meta-item">
                <span class="meta-label">费用笔数</span>
                <span class="meta-value">{{ summary.itemCount || 0 }} 笔</span>
              </span>
              <span class="meta-item">
                <span class="meta-label">往来单位费用</span>
                <span class="meta-value">¥{{ formatMoney(summary.partnerAmount) }}</span>
              </span>
              <span class="meta-item">
                <span class="meta-label">内部费用</span>
                <span class="meta-value">¥{{ formatMoney(summary.internalAmount) }}</span>
              </span>
              <span class="meta-item meta-tip">已取消单据不计入统计；点击图表/部门行可下钻至费用明细</span>
            </div>

            <!-- 图表区（可显隐）：按费用类型 / 按部门 / 月度趋势 -->
            <a-row v-if="showCharts" :gutter="12" class="chart-row">
              <a-col :span="8">
                <ARReportChart
                  title="按费用类型"
                  :option="typeChartOption"
                  :loading="loading"
                  :height="200"
                  empty-text="暂无费用类型数据"
                  @point-click="handleChartClick('type', $event)"
                />
              </a-col>
              <a-col :span="8">
                <ARReportChart
                  title="按部门"
                  :option="deptChartOption"
                  :loading="loading"
                  :height="200"
                  empty-text="暂无部门费用数据"
                  @point-click="handleChartClick('dept', $event)"
                />
              </a-col>
              <a-col :span="8">
                <ARReportChart
                  title="月度趋势"
                  :option="trendChartOption"
                  :loading="loading"
                  :height="200"
                  empty-text="暂无月度费用数据"
                />
              </a-col>
            </a-row>

            <!-- 统计明细表（三视图共用列/页面配置能力） -->
            <div class="table-area">
              <BillTableList
                :columns="currentColumns"
                :data-source="currentRows"
                :loading="loading"
                :pagination="billPagination"
                :show-toolbar="false"
                :show-search="false"
                :show-add="false"
                :show-export="false"
                :show-batch-delete="false"
                :selectable="false"
                :row-key="tableRowKey"
                :storage-key="'expense-stats-columns-' + activeTab"
                @page-change="handlePageChange"
                @sort-change="handleSortChange"
              >
                <template #groupKeyCell="{ record }">
                  <a-button type="link" size="small" class="drill-link" @click="drillToDetail(record)">
                    {{ record.groupKey || '-' }}
                  </a-button>
                </template>
                <template #docNoCell="{ record }">
                  <a-button type="link" size="small" @click="openExpenseDoc(record)">
                    {{ record.docNo }}
                  </a-button>
                </template>
                <template #approvalStatusCell="{ record }">
                  <a-tag :color="approvalStatusColor(record.approvalStatus)">
                    {{ approvalStatusText(record) }}
                  </a-tag>
                </template>
                <template #statusCell="{ record }">
                  <a-tag :color="docStatusColor(record.status)">
                    {{ docStatusText(record.status) }}
                  </a-tag>
                </template>
              </BillTableList>
            </div>
          </div>
        </template>

        <!-- ═══ 底部合计（与卡片「费用总额」同口径） ═══ -->
        <template #table-footer>
          <div class="table-footer">
            <span v-if="currentTabTotal.scoped" class="footer-scope">（{{ currentTabTotal.scoped }}）</span>
            <span class="footer-item">合计单据：<b>{{ currentTabTotal.docCount }}</b> 张</span>
            <span class="footer-item">费用笔数：<b>{{ currentTabTotal.itemCount }}</b> 笔</span>
            <span class="footer-item">
              费用金额：<b class="currency-value">¥{{ formatMoney(currentTabTotal.totalAmount) }}</b>
            </span>
            <span class="footer-item">
              其中已审批：<b class="currency-value">¥{{ formatMoney(currentTabTotal.approvedAmount) }}</b>
            </span>
          </div>
        </template>
      </CategoryListLayout>
    </PageContainer>

    <!-- ═══ 页面配置弹窗（查询条件 + 功能按钮） ═══ -->
    <PageConfigPanel
      :open="showPageConfig"
      :query-fields-config="queryFieldsConfig"
      :function-buttons-config="functionButtonsConfig"
      storage-key="expense-stats-page-config"
      @update:open="showPageConfig = $event"
      @change="handlePageConfigChange"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined, SettingOutlined, ExportOutlined, BarChartOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import ARReportChart from '@/components/ARReportChart/ARReportChart.vue'
import type { StatCardItem } from '@/components/ARReportPage/types'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import { useAutoGridSpan } from '@/composables/useAutoGridSpan'
import { expenseStatsApi, expenseDocApi } from '@/api/finance'

defineOptions({ name: 'FinanceExpenseStats' })

const router = useRouter()

// ═══ 三视图 Tab：按部门 / 按费用类型 / 按明细（下钻） ═══
const tabs = [
  { key: 'dept', label: '按部门' },
  { key: 'type', label: '按费用类型' },
  { key: 'doc', label: '按明细' },
]
const activeTab = ref<'dept' | 'type' | 'doc'>('dept')

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
const quickDate = ref('last3Month')
const queryScheme = ref('')

// ═══ 状态 ═══
const loading = ref(false)
const summary = ref<Record<string, any>>({})
const groupRows = ref<any[]>([])          // 按部门 / 按费用类型（后端一次返回全量分组）
const detailRows = ref<any[]>([])         // 按明细（后端分页）
const showPageConfig = ref(false)
const showCharts = ref(true)
const searchGridRef = ref<HTMLElement | null>(null)
const searchActionRef = ref<HTMLElement | null>(null)
const { span: actionSpan } = useAutoGridSpan(searchActionRef, searchGridRef)

// ═══ 搜索条件（三视图共用过滤口径） ═══
const dateRange = ref<[Dayjs, Dayjs] | null>([dayjs().subtract(3, 'month'), dayjs()])
const search = reactive({
  docNo: '',
  partnerName: '',
  handlerName: '',
  deptName: '',
  expenseName: '',
  creatorName: '',
  expenseType: '' as string | undefined,
  approvalStatus: '' as string | undefined,
  status: '' as string | undefined,
  summary: '',
  showRed: false,
})

function handleDateChange() {
  handleSearch()
}

function buildParams(): Record<string, any> {
  const params: Record<string, any> = {}
  if (dateRange.value?.[0]) params.dateStart = dateRange.value[0].format('YYYY-MM-DD')
  if (dateRange.value?.[1]) params.dateEnd = dateRange.value[1].format('YYYY-MM-DD')
  if (search.docNo) params.docNo = search.docNo
  if (search.partnerName) params.partnerName = search.partnerName
  if (search.handlerName) params.handlerName = search.handlerName
  if (search.deptName) params.deptName = search.deptName
  if (search.expenseName) params.expenseName = search.expenseName
  if (search.creatorName) params.creatorName = search.creatorName
  if (search.summary) params.summary = search.summary
  if (search.expenseType !== undefined && search.expenseType !== '') params.expenseType = search.expenseType
  if (search.approvalStatus !== undefined && search.approvalStatus !== '') params.approvalStatus = search.approvalStatus
  if (search.status !== undefined && search.status !== '') params.status = search.status
  // 显示红冲：勾选=含红冲（后端不传该参数即为含），未勾选=排除红冲
  if (!search.showRed) params.showRed = false
  return params
}

// ═══ 分页（分组视图内存分页 / 明细视图后端分页） ═══
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const billPagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
}))

/** 分组视图内存排序（后端默认按金额降序，此处支持列头点击排序） */
const sortState = reactive<{ key: string | null; order: 'asc' | 'desc' | null }>({ key: null, order: null })

const sortedGroupRows = computed(() => {
  const rows = [...groupRows.value]
  const key = sortState.key
  const order = sortState.order
  if (!key || !order) return rows
  const dir = order === 'asc' ? 1 : -1
  return rows.sort((a, b) => {
    const av = a[key]
    const bv = b[key]
    const an = Number(av)
    const bn = Number(bv)
    if (!isNaN(an) && !isNaN(bn) && av !== '' && bv !== '') return (an - bn) * dir
    return String(av ?? '').localeCompare(String(bv ?? ''), 'zh-CN') * dir
  })
})

const currentRows = computed(() => {
  if (activeTab.value === 'doc') return detailRows.value
  const start = (pagination.current - 1) * pagination.pageSize
  return sortedGroupRows.value.slice(start, start + pagination.pageSize)
})

const tableRowKey = computed(() => (activeTab.value === 'doc' ? 'id' : 'groupKey'))

// ═══ 统计卡片（键与后端 ExpenseStatsSummaryVO 一致） ═══
const statCards = computed<StatCardItem[]>(() => [
  { label: '费用总额', value: Number(summary.value.totalAmount) || 0, precision: 2, prefix: '¥' },
  { label: '已审批金额', value: Number(summary.value.approvedAmount) || 0, precision: 2, prefix: '¥' },
  { label: '待审批金额', value: Number(summary.value.pendingAmount) || 0, precision: 2, prefix: '¥' },
  { label: '已拒绝金额', value: Number(summary.value.rejectedAmount) || 0, precision: 2, prefix: '¥' },
  { label: '单据数', value: Number(summary.value.expenseCount) || 0, suffix: '单' },
  { label: '平均单额', value: Number(summary.value.averageAmount) || 0, precision: 2, prefix: '¥' },
])

// ═══ 图表：按费用类型（环形） / 按部门（柱状） / 月度趋势（折线） ═══
function mapToPairs(map: Record<string, number> | undefined): Array<{ name: string; value: number }> {
  return Object.entries(map || {}).map(([name, value]) => ({ name, value: Number(value) || 0 }))
}

const typeChartOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
  legend: { bottom: 0, type: 'scroll', itemHeight: 8, itemWidth: 8 },
  series: [
    {
      name: '费用类型',
      type: 'pie',
      radius: ['38%', '62%'],
      center: ['50%', '44%'],
      data: mapToPairs(summary.value.byType),
    },
  ],
}))

const deptChartOption = computed(() => {
  const pairs = mapToPairs(summary.value.byDepartment)
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 12, right: 16, top: 16, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: pairs.map(p => p.name), axisLabel: { rotate: 20, fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { fontSize: 11 } },
    series: [{ name: '费用金额', type: 'bar', barMaxWidth: 36, data: pairs.map(p => p.value) }],
  }
})

const trendChartOption = computed(() => {
  const rows: any[] = summary.value.monthlyTrend || []
  return {
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, itemHeight: 8, itemWidth: 8 },
    grid: { left: 12, right: 16, top: 20, bottom: 28, containLabel: true },
    xAxis: { type: 'category', data: rows.map(r => r.month), axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { fontSize: 11 } },
    series: [
      { name: '费用金额', type: 'line', smooth: true, data: rows.map(r => Number(r.totalAmount) || 0) },
      { name: '已审批金额', type: 'line', smooth: true, data: rows.map(r => Number(r.approvedAmount) || 0) },
    ],
  }
})

// ═══ 列定义（三视图独立列配置） ═══
/** 单元格金额：空值留白（占位行不显示 0.00），有值按千分位 */
function cellMoney(v: any): string {
  if (v === null || v === undefined || v === '') return ''
  return formatMoney(v)
}
function cellRatio(v: any): string {
  if (v === null || v === undefined || v === '') return ''
  return formatRatio(v) + '%'
}
/** 单元格日期时间：空值留白 */
function cellDateTime(v: any): string {
  if (!v) return ''
  return formatDateTime(v)
}

/** 金额列（右对齐 + 千分位，列配置面板可改显示名/宽度/显隐） */
const MONEY_COL = (title: string, field: string, width = 130, defaultHidden = false) =>
  ({ title, field, key: field, width, align: 'right', formatter: cellMoney, defaultHidden })

/** 按部门（分组视图：内存分页 + 内存排序） */
const deptColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '部门', field: 'groupKey', key: 'groupKey', width: 200, type: 'slot', slotName: 'groupKeyCell' },
  { title: '单据数', field: 'docCount', key: 'docCount', width: 90, align: 'right', sortable: true },
  { title: '费用笔数', field: 'itemCount', key: 'itemCount', width: 90, align: 'right', sortable: true },
  { ...MONEY_COL('费用金额', 'totalAmount', 140), sortable: true },
  { title: '占比', field: 'ratio', key: 'ratio', width: 100, align: 'right', formatter: cellRatio, sortable: true },
  MONEY_COL('已审批金额', 'approvedAmount', 130),
  MONEY_COL('待审批金额', 'pendingAmount', 130, true),
  MONEY_COL('已驳回金额', 'rejectedAmount', 130, true),
  MONEY_COL('已记账金额', 'paidAmount', 130, true),
  MONEY_COL('单均金额', 'avgAmount', 120, true),
]

/** 按费用类型（分组视图：内存分页 + 内存排序） */
const typeColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '费用名称', field: 'groupKey', key: 'groupKey', width: 180, type: 'slot', slotName: 'groupKeyCell' },
  { title: '费用编号', field: 'groupCode', key: 'groupCode', width: 110 },
  { title: '费用科目', field: 'subjectName', key: 'subjectName', width: 140 },
  { title: '科目编码', field: 'subjectCode', key: 'subjectCode', width: 110, defaultHidden: true },
  { title: '单据数', field: 'docCount', key: 'docCount', width: 90, align: 'right', sortable: true },
  { title: '费用笔数', field: 'itemCount', key: 'itemCount', width: 90, align: 'right', sortable: true },
  { ...MONEY_COL('费用金额', 'totalAmount', 140), sortable: true },
  { title: '占比', field: 'ratio', key: 'ratio', width: 100, align: 'right', formatter: cellRatio, sortable: true },
  MONEY_COL('已审批金额', 'approvedAmount', 130),
  MONEY_COL('待审批金额', 'pendingAmount', 130, true),
  MONEY_COL('已驳回金额', 'rejectedAmount', 130, true),
  MONEY_COL('已记账金额', 'paidAmount', 130, true),
  MONEY_COL('单均金额', 'avgAmount', 120, true),
]

/** 按明细（下钻费用单明细：后端分页，默认按单据日期/单号排序） */
const docColumns = [
  { title: '', key: 'rowNo', type: 'rowNo', width: 40, fixed: 'left' },
  { title: '单据日期', field: 'docDate', key: 'docDate', width: 110 },
  { title: '单据编号', field: 'docNo', key: 'docNo', width: 180, type: 'slot', slotName: 'docNoCell' },
  { title: '审批状态', field: 'approvalStatus', key: 'approvalStatus', width: 110, align: 'center', type: 'slot', slotName: 'approvalStatusCell' },
  { title: '单据状态', field: 'status', key: 'status', width: 100, align: 'center', type: 'slot', slotName: 'statusCell' },
  { title: '往来单位', field: 'partnerName', key: 'partnerName', width: 180 },
  { title: '部门', field: 'deptName', key: 'deptName', width: 120 },
  { title: '经手人', field: 'handlerName', key: 'handlerName', width: 90 },
  { title: '费用名称', field: 'expenseName', key: 'expenseName', width: 160 },
  { title: '费用编号', field: 'expenseCode', key: 'expenseCode', width: 110, defaultHidden: true },
  { title: '科目编码', field: 'subjectCode', key: 'subjectCode', width: 110, defaultHidden: true },
  { title: '科目名称', field: 'subjectName', key: 'subjectName', width: 130, defaultHidden: true },
  MONEY_COL('明细金额', 'amount', 130),
  MONEY_COL('本单金额', 'totalAmount', 130, true),
  { title: '费用类别', field: 'expenseType', key: 'expenseType', width: 120, align: 'center', formatter: (v: any) => expenseTypeText(v) },
  { title: '付款账户', field: 'payAccountName', key: 'payAccountName', width: 140, defaultHidden: true },
  MONEY_COL('付款金额', 'payAmount', 120, true),
  { title: '制单人', field: 'creatorName', key: 'creatorName', width: 90, defaultHidden: true },
  { title: '记账人', field: 'bookkeeperName', key: 'bookkeeperName', width: 90, defaultHidden: true },
  { title: '摘要', field: 'summary', key: 'summary', width: 160, defaultHidden: true },
  { title: '明细备注', field: 'itemRemark', key: 'itemRemark', width: 140, defaultHidden: true },
  { title: '单据备注', field: 'remark', key: 'remark', width: 140, defaultHidden: true },
  { title: '制单时间', field: 'createTime', key: 'createTime', width: 150, formatter: cellDateTime, defaultHidden: true },
  { title: '记账时间', field: 'bookkeepingTime', key: 'bookkeepingTime', width: 150, formatter: cellDateTime, defaultHidden: true },
]

/**
 * 列配置由数据表内置（表头齿轮 → 数据表列配置弹窗，个人/全局配置按 storageKey 持久化），
 * 页面不再另设列配置按钮/弹窗，避免两处配置入口重复。
 */
const currentColumns = computed(() => {
  if (activeTab.value === 'type') return typeColumns.map(col => ({ ...col }))
  if (activeTab.value === 'doc') return docColumns.map(col => ({ ...col }))
  return deptColumns.map(col => ({ ...col }))
})

// ═══ 页面配置（查询条件 + 功能按钮） ═══
interface QueryFieldSetting { key: string; label: string; visible: boolean }
interface FunctionButtonSetting { key: string; label: string; enabled: boolean }

const QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'date', label: '申请日期', visible: true },
  { key: 'docNo', label: '单据编号', visible: true },
  { key: 'partnerName', label: '往来单位', visible: true },
  { key: 'handlerName', label: '经手人', visible: true },
  { key: 'deptName', label: '部门', visible: true },
  { key: 'expenseName', label: '费用名称', visible: true },
  { key: 'creatorName', label: '制单人', visible: false },
  { key: 'expenseType', label: '费用类别', visible: true },
  { key: 'approvalStatus', label: '审批状态', visible: true },
  { key: 'status', label: '单据状态', visible: false },
  { key: 'summary', label: '摘要', visible: false },
  { key: 'showRed', label: '显示红冲', visible: true },
]
const FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'export', label: '导出', enabled: true },
  { key: 'charts', label: '图表', enabled: true },
  { key: 'pageConfig', label: '页面配置', enabled: true },
]
const PAGE_CONFIG_KEY = 'expense-stats-page-config'

const queryFieldsConfig = ref<QueryFieldSetting[]>(QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonsConfig = ref<FunctionButtonSetting[]>(FUNCTION_BUTTONS.map(f => ({ ...f })))

function loadPageConfig() {
  try {
    const raw = localStorage.getItem(PAGE_CONFIG_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw)
    if (parsed.queryFields) {
      queryFieldsConfig.value = QUERY_FIELDS.map(df => {
        const saved = parsed.queryFields.find((f: QueryFieldSetting) => f.key === df.key)
        return saved ? { ...df, ...saved } : { ...df }
      })
    }
    if (parsed.functionButtons) {
      functionButtonsConfig.value = FUNCTION_BUTTONS.map(bf => {
        const saved = parsed.functionButtons.find((f: FunctionButtonSetting) => f.key === bf.key)
        return saved ? { ...bf, ...saved } : { ...bf }
      })
    }
  } catch { /* ignore */ }
}

function handlePageConfigChange(config: any) {
  if (config.queryFields) queryFieldsConfig.value = config.queryFields
  if (config.functionButtons) functionButtonsConfig.value = config.functionButtons
}

function fieldVisible(key: string): boolean {
  return queryFieldsConfig.value.find(f => f.key === key)?.visible !== false
}
function btnEnabled(key: string): boolean {
  return functionButtonsConfig.value.find(b => b.key === key)?.enabled !== false
}

// ═══ 状态映射 ═══
const APPROVAL_STATUS_MAP: Record<string, { text: string; color: string }> = {
  '0': { text: '未提交', color: 'default' },
  '1': { text: '审批中', color: 'orange' },
  '2': { text: '审批通过', color: 'green' },
  '3': { text: '审批驳回', color: 'red' },
}
const DOC_STATUS_MAP: Record<string, { text: string; color: string }> = {
  '0': { text: '草稿', color: 'default' },
  '1': { text: '已记账', color: 'blue' },
  '2': { text: '已取消', color: 'red' },
}

function approvalStatusText(record: any): string {
  return APPROVAL_STATUS_MAP[String(record?.approvalStatus ?? '0')]?.text || '未提交'
}
function approvalStatusColor(status: any): string {
  return APPROVAL_STATUS_MAP[String(status ?? '0')]?.color || 'default'
}
function docStatusText(status: any): string {
  return DOC_STATUS_MAP[String(status ?? '0')]?.text || '-'
}
function docStatusColor(status: any): string {
  return DOC_STATUS_MAP[String(status ?? '0')]?.color || 'default'
}
function expenseTypeText(type: any): string {
  return String(type) === '1' ? '内部费用' : '往来单位费用'
}
function formatMoney(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function formatRatio(val: any): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '0.00'
  return Number(val).toFixed(2)
}
function formatDateTime(value: any): string {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

// ═══ 底部合计（与卡片同口径）；明细视图为「本页」合计 ═══
const currentTabTotal = computed(() => {
  if (activeTab.value === 'doc') {
    const docIds = new Set<string>()
    let itemCount = 0
    let amount = 0
    let approved = 0
    for (const r of detailRows.value) {
      docIds.add(String(r.expenseDocId))
      itemCount += 1
      amount += Number(r.amount) || 0
      if (String(r.approvalStatus) === '2') approved += Number(r.amount) || 0
    }
    return { docCount: docIds.size, itemCount, totalAmount: amount, approvedAmount: approved, scoped: '本页' }
  }
  let docCount = 0
  let itemCount = 0
  let amount = 0
  let approved = 0
  for (const r of groupRows.value) {
    docCount += Number(r.docCount) || 0
    itemCount += Number(r.itemCount) || 0
    amount += Number(r.totalAmount) || 0
    approved += Number(r.approvedAmount) || 0
  }
  return { docCount, itemCount, totalAmount: amount, approvedAmount: approved, scoped: '' }
})

// ═══ 数据加载 ═══
async function fetchSummary() {
  try {
    const res: any = await expenseStatsApi.getSummary(buildParams())
    summary.value = res && typeof res === 'object' ? res : {}
  } catch (error: any) {
    summary.value = {}
    message.error(error?.response?.data?.message || '获取费用统计汇总失败')
  }
}

async function fetchGroupRows() {
  const res: any = activeTab.value === 'type'
    ? await expenseStatsApi.getByType(buildParams())
    : await expenseStatsApi.getByDepartment(buildParams())
  groupRows.value = Array.isArray(res) ? res : []
  pagination.total = groupRows.value.length
}

async function fetchDetailRows() {
  const params = { ...buildParams(), pageNum: pagination.current, pageSize: pagination.pageSize }
  const res: any = await expenseDocApi.getPageDetail(params)
  const body = res ?? {}
  detailRows.value = body?.records || []
  pagination.total = Number(body?.total) || 0
}

async function fetchData() {
  loading.value = true
  try {
    await fetchSummary()
    if (activeTab.value === 'doc') await fetchDetailRows()
    else await fetchGroupRows()
  } catch (error: any) {
    console.warn('[费用统计] 获取数据失败', error)
    message.error(error?.response?.data?.message || '获取数据失败')
    if (activeTab.value === 'doc') detailRows.value = []
    else groupRows.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  search.docNo = ''
  search.partnerName = ''
  search.handlerName = ''
  search.deptName = ''
  search.expenseName = ''
  search.creatorName = ''
  search.expenseType = ''
  search.approvalStatus = ''
  search.status = ''
  search.summary = ''
  search.showRed = false
  quickDate.value = 'last3Month'
  setQuickDateRange('last3Month')
  pagination.current = 1
  fetchData()
}

function handleTabChange(key: string) {
  activeTab.value = key as 'dept' | 'type' | 'doc'
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  if (activeTab.value === 'doc') fetchDetailRows()
}

/** 列头排序：分组视图内存排序（全量分组行）；明细视图由后端固定排序，不提供列排序 */
function handleSortChange(key: string | null, order: 'asc' | 'desc' | null) {
  if (activeTab.value === 'doc') return
  sortState.key = key
  sortState.order = order
  pagination.current = 1
}

// ═══ 快捷日期 ═══
function setQuickDateRange(key: string) {
  const now = dayjs()
  let start: Dayjs
  let end: Dayjs
  switch (key) {
    case 'yesterday': start = now.subtract(1, 'day'); end = now.subtract(1, 'day'); break
    case 'today': start = now; end = now; break
    case 'week': start = now.startOf('week'); end = now; break
    case 'lastWeek': start = now.subtract(7, 'day'); end = now; break
    case 'month': start = now.startOf('month'); end = now; break
    case 'lastMonth': start = now.subtract(1, 'month').startOf('month'); end = now.subtract(1, 'month').endOf('month'); break
    case 'last3Month': start = now.subtract(3, 'month'); end = now; break
    case 'year': start = now.startOf('year'); end = now; break
    default: start = now.subtract(3, 'month'); end = now
  }
  dateRange.value = [start, end]
}

function setQuickDate(key: string) {
  quickDate.value = key
  setQuickDateRange(key)
  handleSearch()
}

// ═══ 图表显隐（记忆上次选择） ═══
function toggleCharts() {
  showCharts.value = !showCharts.value
  try {
    localStorage.setItem('expense-stats-chart-visible', showCharts.value ? '1' : '0')
  } catch { /* ignore */ }
}

// ═══ 下钻：图表 / 分组行 → 按明细 ═══
function handleChartClick(source: 'type' | 'dept', params: any) {
  const name = params?.name
  if (!name) return
  if (source === 'type') search.expenseName = String(name)
  else search.deptName = String(name)
  activeTab.value = 'doc'
  pagination.current = 1
  message.info(`已下钻至明细：${source === 'type' ? '费用名称' : '部门'} = ${name}`)
  fetchData()
}

function drillToDetail(record: any) {
  const name = record?.groupKey
  if (!name) return
  if (activeTab.value === 'type') {
    search.expenseName = name
  } else {
    if (name === '未指定部门') {
      message.info('「未指定部门」为无部门单据汇总，明细按部门过滤条件不可用')
      return
    }
    search.deptName = name
  }
  activeTab.value = 'doc'
  pagination.current = 1
  fetchData()
}

// ═══ 打开费用单（单据编号 → 费用单表单页，与费用单列表同约定） ═══
function openExpenseDoc(record: any) {
  if (!record?.expenseDocId) return
  router.push(`/finance/expense-doc/form?id=${record.expenseDocId}`)
}

// ═══ 导出当前视图（按可见列，复用列 formatter 保证与页面显示一致） ═══
const TAB_LABELS: Record<string, string> = { dept: '按部门', type: '按费用类型', doc: '按明细' }

function handleExport() {
  const rows = activeTab.value === 'doc' ? detailRows.value : groupRows.value
  if (rows.length === 0) {
    message.warning('没有可导出的数据')
    return
  }
  const exportCols = currentColumns.value.filter((c: any) => c.key !== 'rowNo')
  const header = exportCols.map((c: any) => c.title).join(',')
  const lines = rows.map((r: any) => exportCols.map((c: any) => {
    const raw = r[c.field]
    // 金额/占比/日期列走列 formatter，与表格显示保持一致
    if (typeof c.formatter === 'function') return c.formatter(raw, r)
    if (c.slotName === 'approvalStatusCell') return approvalStatusText(r)
    if (c.slotName === 'statusCell') return docStatusText(r.status)
    return raw ?? ''
  }).map((v: any) => `"${String(v).replace(/"/g, '""')}"`).join(','))
  const csv = [header, ...lines].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `费用统计_${TAB_LABELS[activeTab.value]}_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
  a.click()
  URL.revokeObjectURL(url)
  message.success('导出成功')
}

function handleError(error: Error) {
  console.error('[费用统计] 页面错误', error)
}

// ═══ 初始化 ═══
onMounted(() => {
  loadPageConfig()
  try {
    const chartFlag = localStorage.getItem('expense-stats-chart-visible')
    if (chartFlag !== null) showCharts.value = chartFlag === '1'
  } catch { /* ignore */ }
  fetchData()
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

.stats-body { display: flex; flex-direction: column; height: 100%; min-height: 0; padding: 8px 12px 0; gap: 8px; }
.stats-cards { margin-bottom: 0; }
.stats-cards :deep(.ar-stat-card) { padding: 10px 16px; box-shadow: none; background: #fafafa; border: 1px solid #f0f0f0; }
.stats-cards :deep(.ar-stat-card__value) { font-size: 19px; }
.stats-cards :deep(.ar-stat-card__label) { margin-bottom: 4px; }

.stats-meta { display: flex; flex-wrap: wrap; align-items: center; gap: 16px; padding: 0 2px; font-size: 12px; flex-shrink: 0; }
.meta-item { display: inline-flex; align-items: center; gap: 4px; }
.meta-label { color: #8c8c8c; }
.meta-value { color: #262626; font-weight: 600; }
.meta-tip { color: #bfbfbf; }

.chart-row { flex-shrink: 0; }
.chart-row :deep(.ar-report-chart) { box-shadow: none; border: 1px solid #f0f0f0; padding: 8px 12px; }

.table-area { flex: 1; min-height: 0; overflow: hidden; }
.currency-value { font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-variant-numeric: tabular-nums; }
.drill-link { padding: 0; }

.table-footer { display: flex; align-items: center; gap: 24px; font-size: 13px; color: #595959; }
.table-footer .footer-item b { color: #262626; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
