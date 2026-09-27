<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        经营分析（分析 → 财务分析 → 经营分析，菜单 80451）
        对标 ql361：**期间损益账表**（单视图、无页内 Tab）——16 列 / 默认 10，多级表头
        `日期 | 收入[销售收入 其他业务收入 其他收入 投资收益 营业外收入 收入合计] |
         支出[销售成本 其他业务成本 营业外支出 销售费用 管理费用 财务费用 其他费用 支出合计] | 营业利润`；
        底部合计行、粒度切换 按天/按周/按月、时间快捷段、行级无操作。
        列名与顺序逐字取自《经营分析开发文档》§3，defaultHidden 个数 = 16 − 10 = 6。

        取数（真实接口，无硬编码数据）：
          · reportApi.getIncomeStatementReport → /erp/finance/report/v2/income-statement-report
            逐会计月（期间）取损益：revenueTotal / otherIncomeTotal / costTotal / expenseTotal /
            nonOperatingIncomeTotal / nonOperatingExpenseTotal / operatingProfit，并从 rows 取
            6601/6602/6603 三费（科目层级 1，无父子重复累加）。行以会计月为粒度。
        后端缺口（见开发文档 §5，已在汇报中列出）：
          · 无「按天 / 按周」损益聚合端点（现有端点最小粒度为会计月）→ 粒度按钮中「按天/按周」置灰；
          · 无部门 / 经手人 / 「包含手工会计凭证」入参 → 对应查询项置灰（不做静默失效的假查询）；
          · 无「其他收入 / 投资收益 / 其他业务成本 / 其他费用 / 优惠」等明细列的数据源 → 列以空白呈现。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 时间快捷段 ═══ -->
        <template #toolbar-left>
          <QuerySchemeBar
            storage-key="analytics-business-analysis\index.vue-query-scheme"
            :snapshot="querySnapshot"
            @apply="applyQuerySnapshot"
          />
          <a-space :size="4" class="quick-dates">
            <a-button
              v-for="d in QUICK_DATES"
              :key="d.key"
              :type="quickDate === d.key ? 'primary' : 'link'"
              size="small"
              @click="setQuickDate(d.key)"
            >
              {{ d.label }}
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 工具栏右侧：刷新｜打印(F8)｜导出｜页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button v-if="isButtonEnabled('refresh')" size="small" :loading="loading" @click="handleRefresh">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button v-if="isButtonEnabled('printF8')" size="small" @click="handlePrint">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button v-if="isButtonEnabled('export')" size="small" :loading="exporting" @click="handleExport">
              <DownloadOutlined /> 导出
            </a-button>
            <a-button size="small" @click="showPageConfig = true">
              <SettingOutlined /> 页面配置
            </a-button>
          </a-space>
        </template>

        <!-- ═══ 查询区（对标：日期范围 + 部门 + 经手人 + 包含手工会计凭证，横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div v-if="isQueryVisible('analysis.dateRange')" class="search-item">
                <span class="search-label">日期</span>
                <a-range-picker
                  v-model:value="query.dateRange"
                  size="small"
                  value-format="YYYY-MM-DD"
                  :allow-clear="false"
                  style="width: 230px"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('analysis.departmentName')" class="search-item">
                <span class="search-label">部门</span>
                <a-input
                  v-model:value="query.departmentName"
                  size="small"
                  placeholder="部门"
                  allow-clear
                  disabled
                  style="width: 140px"
                  title="后端损益端点（/erp/finance/report/v2/income-statement-report）暂无部门条件，待补"
                />
              </div>
              <div v-if="isQueryVisible('analysis.handlerName')" class="search-item">
                <span class="search-label">经手人</span>
                <a-input
                  v-model:value="query.handlerName"
                  size="small"
                  placeholder="经手人"
                  allow-clear
                  disabled
                  style="width: 140px"
                  title="后端损益端点（/erp/finance/report/v2/income-statement-report）暂无经手人条件，待补"
                />
              </div>
              <div class="search-item search-actions">
                <a-checkbox
                  v-model:checked="query.includeManualVoucher"
                  disabled
                  title="后端损益端点暂无「包含手工会计凭证」入参，待补"
                >
                  包含手工会计凭证
                </a-checkbox>
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格（多级表头 + 合计行；表头齿轮列配置） ═══ -->
        <template #table>
          <div class="table-area">
            <!-- 粒度切换（对标：表格上方三段按钮） -->
            <div class="granularity-bar">
              <a-radio-group v-model:value="granularity" size="small" button-style="solid">
                <a-radio-button value="day" disabled title="后端暂无按天损益聚合端点，待补">按天</a-radio-button>
                <a-radio-button value="week" disabled title="后端暂无按周损益聚合端点，待补">按周</a-radio-button>
                <a-radio-button value="month">按月</a-radio-button>
              </a-radio-group>
              <span class="granularity-hint">当前按会计月（会计期间）汇总；按天/按周端点待补</span>
            </div>
            <BillDetailTable
              v-model:data-source="dataSource"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              :summary-columns="summaryColumns"
              row-key="rowKey"
              storage-key="analytics-business-analysis-columns"
              global-config-key="analytics-business-analysis-columns"
            />
          </div>
        </template>

        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.current"
            :page-size="pagination.pageSize"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 页面配置（对标有「经营分析-页面配置弹窗」实测截图 → 接 PageConfigPanel） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="pageConfigStorageKey"
        :print-config-items="PRINT_ITEMS"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="analytics-business-analysis"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  DownloadOutlined, PrinterOutlined, ReloadOutlined, SettingOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { reportApi } from '@/api/analytics'
import { useExport } from '@/composables/useExport'
import { QUICK_DATES, quickDateRange } from '../shared/docTypes'
import { formatMoney } from '../shared/docActions'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'AnalyticsBusinessAnalysis' })

const quickDate = ref('month')
/** 粒度（对标 按天/按周/按月；后端当前仅支持会计月） */
const granularity = ref<'day' | 'week' | 'month'>('month')

const query = reactive({
  dateRange: quickDateRange('month') as [string, string],
  departmentName: '',
  handlerName: '',
  includeManualVoucher: false
})

const loading = ref(false)
/** 当前页展示行 */
const dataSource = ref<any[]>([])
/** 当前过滤条件下的全量行（会计月粒度，逐月一行） */
const allRows = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

function num(v: any): number {
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}
const pad = (n: number) => String(n).padStart(2, '0')

// ═══ 列定义（对标 16 列 / 默认 10；收入、支出为分组表头，仅一层） ═══
// defaultHidden 个数 = 16 − 10 = 6：收入组 3 列（其他收入/投资收益/营业外收入）+ 支出组 3 列（其他业务成本/营业外支出/其他费用）
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'date', title: '日期', width: 130 },
  {
    key: '__income',
    title: '收入',
    children: [
      { key: 'saleRevenue', title: '销售收入', width: 130, align: 'right', formatter: v => formatMoney(v) },
      { key: 'otherBizRevenue', title: '其他业务收入', width: 140, align: 'right', formatter: v => formatMoney(v) },
      { key: 'otherRevenue', title: '其他收入', width: 120, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
      { key: 'investmentIncome', title: '投资收益', width: 120, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
      { key: 'nonOperatingIncome', title: '营业外收入', width: 130, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
      { key: 'revenueTotal', title: '收入合计', width: 130, align: 'right', formatter: v => formatMoney(v) }
    ]
  },
  {
    key: '__expense',
    title: '支出',
    children: [
      { key: 'saleCost', title: '销售成本', width: 130, align: 'right', formatter: v => formatMoney(v) },
      { key: 'otherBizCost', title: '其他业务成本', width: 140, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
      { key: 'nonOperatingExpense', title: '营业外支出', width: 130, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
      { key: 'saleExpense', title: '销售费用', width: 120, align: 'right', formatter: v => formatMoney(v) },
      { key: 'manageExpense', title: '管理费用', width: 120, align: 'right', formatter: v => formatMoney(v) },
      { key: 'financeExpense', title: '财务费用', width: 120, align: 'right', formatter: v => formatMoney(v) },
      { key: 'otherExpense', title: '其他费用', width: 120, align: 'right', formatter: v => formatMoney(v), defaultHidden: true },
      { key: 'expenseTotal', title: '支出合计', width: 130, align: 'right', formatter: v => formatMoney(v) }
    ]
  },
  { key: 'operatingProfit', title: '营业利润', width: 130, align: 'right', formatter: v => formatMoney(v) }
]

/** 合计行（对标：逐列求和，含隐藏列，含负利润） */
const summaryColumns = computed(() => {
  const sum = (key: string) => allRows.value.reduce((a, r) => a + num(r[key]), 0)
  return [
    { key: 'saleRevenue', value: sum('saleRevenue') },
    { key: 'otherBizRevenue', value: sum('otherBizRevenue') },
    { key: 'otherRevenue', value: sum('otherRevenue') },
    { key: 'investmentIncome', value: sum('investmentIncome') },
    { key: 'nonOperatingIncome', value: sum('nonOperatingIncome') },
    { key: 'revenueTotal', value: sum('revenueTotal') },
    { key: 'saleCost', value: sum('saleCost') },
    { key: 'otherBizCost', value: sum('otherBizCost') },
    { key: 'nonOperatingExpense', value: sum('nonOperatingExpense') },
    { key: 'saleExpense', value: sum('saleExpense') },
    { key: 'manageExpense', value: sum('manageExpense') },
    { key: 'financeExpense', value: sum('financeExpense') },
    { key: 'otherExpense', value: sum('otherExpense') },
    { key: 'expenseTotal', value: sum('expenseTotal') },
    { key: 'operatingProfit', value: sum('operatingProfit') }
  ]
})

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'analysis.dateRange', label: '日期', visible: true },
  { key: 'analysis.departmentName', label: '部门', visible: true },
  { key: 'analysis.handlerName', label: '经手人', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]
const PRINT_ITEMS = [
  { key: 'showCompany', label: '打印抬头显示公司名' },
  { key: 'showSummary', label: '打印底部显示合计行' }
]

const {
  showPageConfig, queryFields, functionButtons,
  isQueryVisible, isButtonEnabled, handlePageConfigChange, pageConfigStorageKey
} = useAnalyticsPageConfig({
  storageKey: 'analytics-business-analysis-page-config',
  defaultQueryFields: DEFAULT_QUERY_FIELDS,
  defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
})

// ═══ 取数（逐会计月调用损益端点，按会计月成行） ═══
/** 日期范围覆盖到的会计月列表 */
function monthsInRange(): { year: number; month: number }[] {
  const [start, end] = query.dateRange
  const s = new Date(String(start).replace(/-/g, '/'))
  const e = new Date(String(end).replace(/-/g, '/'))
  const out: { year: number; month: number }[] = []
  const cur = new Date(s.getFullYear(), s.getMonth(), 1)
  while (cur <= e && out.length < 36) {
    out.push({ year: cur.getFullYear(), month: cur.getMonth() + 1 })
    cur.setMonth(cur.getMonth() + 1)
  }
  return out.length ? out : [{ year: e.getFullYear(), month: e.getMonth() + 1 }]
}

/** 损益报表 → 表格行（列口径见开发文档 §3/§4 的三层公式） */
function buildRow(m: { year: number; month: number }, res: any) {
  const rows: any[] = res?.rows || []
  // 三费按一级科目精确匹配（subjectLevel=1 时不存在父子重复累加）
  const sumBy = (code: string) => rows
    .filter(r => !r?.summaryRow && String(r?.subjectCode || '').startsWith(code))
    .reduce((a, r) => a + num(r.currentAmount), 0)
  const revenueTotal = num(res?.revenueTotal) + num(res?.otherIncomeTotal) + num(res?.nonOperatingIncomeTotal)
  const expenseTotal = num(res?.costTotal) + num(res?.expenseTotal) + num(res?.nonOperatingExpenseTotal)
  return {
    rowKey: `${m.year}-${pad(m.month)}`,
    date: `${m.year}-${pad(m.month)}`,
    saleRevenue: num(res?.revenueTotal),
    otherBizRevenue: num(res?.otherIncomeTotal),
    nonOperatingIncome: num(res?.nonOperatingIncomeTotal),
    revenueTotal,
    saleCost: num(res?.costTotal),
    nonOperatingExpense: num(res?.nonOperatingExpenseTotal),
    saleExpense: sumBy('6601'),
    manageExpense: sumBy('6602'),
    financeExpense: sumBy('6603'),
    expenseTotal,
    operatingProfit: num(res?.operatingProfit)
  }
}

function applyClientPage() {
  const size = pagination.pageSize
  const start = (pagination.current - 1) * size
  dataSource.value = allRows.value.slice(start, start + size)
}

async function fetchData() {
  loading.value = true
  try {
    const months = monthsInRange()
    const rows = await Promise.all(months.map(async m => {
      const res: any = await reportApi.getIncomeStatementReport({
        fiscalYear: m.year,
        periodMode: 'single',
        fiscalPeriod: m.month,
        subjectLevel: 1,
        showZero: false
      })
      return buildRow(m, res)
    }))
    allRows.value = rows
    pagination.total = rows.length
    applyClientPage()
  } catch (e: any) {
    console.warn('[经营分析] 取数失败', e)
    message.error('获取经营分析数据失败')
    allRows.value = []
    dataSource.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

// ═══ 查询交互 ═══
// ═══ 查询方案（存本机，只对当前操作员可见） ═══
function querySnapshot(): Record<string, any> {
  return { ...query }
}

function applyQuerySnapshot(v: Record<string, any>) {
  Object.assign(query, v)
  if (typeof quickDate !== 'undefined') quickDate.value = ''
  handleSearch()
}

function handleSearch() {
  pagination.current = 1
  return fetchData()
}

function handlePageChange(page: number, size: number) {
  const sizeChanged = size !== pagination.pageSize
  pagination.pageSize = size
  pagination.current = sizeChanged ? 1 : page
  applyClientPage()
  return Promise.resolve()
}

function setQuickDate(key: string) {
  quickDate.value = key
  const [start, end] = quickDateRange(key)
  query.dateRange = [start, end]
  handleSearch()
}

function handleRefresh() {
  fetchData()
}

function handleReset() {
  Object.assign(query, {
    dateRange: quickDateRange('month'),
    departmentName: '',
    handlerName: '',
    includeManualVoucher: false
  })
  handleSearch()
}

// ═══ 打印(F8) ═══
/** 叶子列（收入/支出为分组表头，按叶子列展开） */
function leafColumns(cols: DetailColumnConfig[]): DetailColumnConfig[] {
  const out: DetailColumnConfig[] = []
  for (const c of cols) {
    if (c.children?.length) out.push(...leafColumns(c.children))
    else if (c.key !== 'rowNo') out.push(c)
  }
  return out
}

const printColumns = computed(() => leafColumns(columns))

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML + window.open 打印窗口，现在交给 PrintDialog：列与行由页面给，模板负责版式。
// 打印列是 computed（收入/支出分组表头按叶子列展开）→ 冻结不进模板，明确按数据列打。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'analytics-business-analysis',
  title: () => `经营分析（${query.dateRange[0]} ~ ${query.dateRange[1]}，按会计月）`,
  useDataColumns: true,
  columns: () => printColumns.value,
  rows: () => dataSource.value,
  // 原表尾合计按**全量**（非本页）逐列求和，故交给页脚打一行文字
  totalText: () => {
    const titleOf = new Map(printColumns.value.map(c => [c.key, c.title]))
    return '合计：' + summaryColumns.value
      .map(s => `${titleOf.get(s.key) || s.key} ${formatMoney(s.value)}`)
      .join('，')
  },
  emptyTip: '没有可打印的数据',
})

function handleF8Key(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

// ═══ 导出（CSV；会计月为最小粒度，全量即内存中的全量行） ═══
const { execute: executeExport, exporting } = useExport()

function toExportRow(r: any): string[] {
  return printColumns.value.map(c => {
    const v = r[c.key]
    if (v === undefined || v === null) return ''
    return c.align === 'right' ? formatMoney(v) : String(v)
  })
}

function handleExport() {
  executeExport({
    fileName: '经营分析',
    headers: printColumns.value.map(c => c.title),
    total: pagination.total,
    fetchAll: async () => allRows.value,
    mapToRows: (list: any[]) => list.map(toExportRow),
    fallbackRows: () => dataSource.value.map(toExportRow)
  })
}

function handleError(err: any) {
  console.error('[经营分析] 页面异常', err)
}

onMounted(async () => {
  await fetchData()
  window.addEventListener('keydown', handleF8Key)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleF8Key)
})
</script>

<style scoped>
/* 插槽内容不在 scoped 作用域内，样式随页面自带（见《插槽内容的样式必须自备》） */
.search-area { width: 100%; }
.search-grid { display: flex; flex-wrap: wrap; gap: 8px 16px; align-items: center; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { color: #666; font-size: 13px; white-space: nowrap; }
.search-actions { margin-left: auto; }
.quick-dates { margin-left: 8px; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.granularity-bar { display: flex; align-items: center; gap: 10px; padding: 6px 8px; }
.granularity-hint { color: #999; font-size: 12px; }
</style>
