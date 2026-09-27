<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        查费用（分析 → 财务分析 → 查费用，菜单 80454）
        对标 ql361「查费用」：4 个视图 Tab（按部门 / 按职员 / 按明细 / 按往来单位），逐 Tab 独立列配置：
          按部门 3/3、按职员 3/3、按明细 15/9、按往来单位 4/4。
        ⚠️ 按部门 / 按职员的「部门列 / 职员列」为**按主数据动态生成的矩阵列，不进列配置弹窗**（对标实测口径）；
           本页以 key='checkbox'（组件锁定列判定）承载，故弹窗口径恒为 3 列。
        行维度 = 费用科目（费用名称 = 科目名，费用编号 = 科目编码）；按部门/按职员左侧另有费用科目分类树。
        取数：/erp/expense/statistics/matrix|detail|partner（后端 ExpenseAnalyticsController）。
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="activeTab === 'dept' || activeTab === 'staff'"
        :category-title="'费用科目'"
        :category-tree-data="categoryTree"
        :selected-category-id="selectedCategoryId"
        :show-table-footer="true"
        @tab-change="onTabChange"
        @category-select="onCategorySelect"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 时间快捷段 ═══ -->
        <template #toolbar-left>
          <QuerySchemeBar
            storage-key="analytics-check-expense-query-scheme"
            :snapshot="schemeSnapshot"
            @apply="applyScheme"
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
            <a-button v-if="isButtonEnabled('refresh')" size="small" :loading="loading" @click="fetchData">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button v-if="isButtonEnabled('print')" size="small" @click="handlePrint">
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

        <!-- ═══ 查询区（对标：日期范围 / 科目 / 部门 / 往来单位 + 包含红冲；横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div v-if="isQueryVisible('expense.dateRange')" class="search-item">
                <span class="search-label">日期</span>
                <a-range-picker
                  v-model:value="dateRange"
                  size="small"
                  value-format="YYYY-MM-DD"
                  :allow-clear="false"
                  style="width: 240px"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('expense.dateType')" class="search-item">
                <span class="search-label">日期类型</span>
                <a-select
                  v-model:value="query.dateType"
                  size="small"
                  style="width: 120px"
                  :options="[{ label: '单据日期', value: 'docDate' }]"
                />
              </div>
              <div v-if="isQueryVisible('expense.subject')" class="search-item">
                <span class="search-label">科目</span>
                <a-input
                  v-model:value="query.keyword"
                  size="small"
                  placeholder="科目编号/名称"
                  allow-clear
                  style="width: 150px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('expense.dept') && (activeTab === 'dept' || activeTab === 'staff')" class="search-item">
                <span class="search-label">部门</span>
                <a-input
                  v-model:value="query.deptName"
                  size="small"
                  placeholder="部门名称"
                  allow-clear
                  style="width: 130px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('expense.partner')" class="search-item">
                <span class="search-label">往来单位</span>
                <a-input
                  v-model:value="query.partnerName"
                  size="small"
                  placeholder="单位名称"
                  allow-clear
                  style="width: 160px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item search-actions">
                <a-checkbox v-model:checked="query.includeReversed" @change="handleSearch">包含红冲</a-checkbox>
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格（表头齿轮列配置，逐 Tab 一套） ═══ -->
        <template #table>
          <div class="table-area" :data-cols="colSummary">
            <BillDetailTable
              :data-source="rows"
              :columns="activeColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              :summary-columns="summaryColumns"
              row-key="rowKey"
              :storage-key="`analytics-check-expense-columns-${activeTab}`"
              :global-config-key="`analytics-check-expense-columns-${activeTab}`"
            >
              <!-- 矩阵列：按主数据动态生成（不进列配置），列名即维度名，取值走 record.cells -->
              <template #matrixCell="{ record, column }">
                {{ fmtMoney((record.cells || {})[column.title]) }}
              </template>
            </BillDetailTable>
          </div>
        </template>

        <template #table-footer>
          <StandardPagination
            variant="classic"
            :current="pagination.page"
            :page-size="pagination.size"
            :total="pagination.total"
            :page-size-options="[20, 50, 100]"
            @change="handlePageChange"
          />
        </template>
      </CategoryListLayout>

      <!-- ═══ 页面配置（对标有「页面配置」弹窗 → 接 PageConfigPanel） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="pageConfigStorageKey"
        hide-print-config
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="analytics-check-expense"
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
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { expenseAnalyticsApi } from '@/api/analytics-finance'
import { useExport } from '@/composables/useExport'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'
import { QUICK_DATES, quickDateRange } from '../shared/docTypes'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'

defineOptions({ name: 'AnalyticsCheckExpense' })

// ═══ 视图 Tab（顺序逐字取自对标实测） ═══
const TABS = [
  { key: 'dept', label: '按部门' },
  { key: 'staff', label: '按职员' },
  { key: 'detail', label: '按明细' },
  { key: 'partner', label: '按往来单位' }
]
const activeTab = ref('dept')
const quickDate = ref('month')
const dateRange = ref<[string, string]>(quickDateRange('month') as [string, string])
const query = reactive({
  dateType: 'docDate',
  keyword: '',
  deptName: '',
  partnerName: '',
  includeReversed: false
})

const loading = ref(false)
const rows = ref<any[]>([])
const pagination = reactive({ page: 1, size: 20, total: 0 })
const summary = ref<Record<string, any>>({})
/** 按部门/按职员的动态矩阵列（后端返回的维度名，不进列配置弹窗） */
const matrixColumns = ref<string[]>([])
const selectedCategoryId = ref<string | number>('0')

// ═══ 数值格式化 ═══
function fmtNum(v: any): string {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '-'
  return Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 4 })
}
function fmtMoney(v: any): string {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '-'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function fmtText(v: any): string {
  return v === null || v === undefined || v === '' ? '-' : String(v)
}

type Kind = 'money' | 'num' | 'text' | 'rate'
const FORMATTER: Record<Kind, (v: any) => string> = {
  money: fmtMoney,
  num: fmtNum,
  text: fmtText,
  rate: v => (v === null || v === undefined || v === '' || isNaN(Number(v)) ? '-' : `${fmtNum(v)}%`)
}

function col(key: string, title: string, kind: Kind, width = 120, hidden = false): DetailColumnConfig {
  return {
    key,
    title,
    width,
    align: kind === 'text' ? 'left' : 'right',
    formatter: FORMATTER[kind],
    ...(hidden ? { defaultHidden: true } : {})
  }
}

const ROW_NO: DetailColumnConfig = { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' }

/**
 * 矩阵列：由主数据动态生成、不进列配置弹窗（对标口径）。
 * BillDetailTable 的「锁定列」判定按列 key 命中 LOCKED_COLUMNS(rowNo/checkbox/action)，
 * 故矩阵列统一借 key='checkbox' 复用锁定判定（渲染由 type:'slot' 决定，不会渲染成勾选框）；
 * 各列以 title 区分维度，取值在插槽里读 record.cells[column.title]。
 */
function matrixCol(name: string): DetailColumnConfig {
  return { key: 'checkbox', title: name, type: 'slot', slotName: 'matrixCell', width: 110, align: 'right' }
}

// ── Tab1「按部门」：全部 3 列 / 默认 3（全可见）＋动态部门矩阵列 ──
const deptColumns = computed<DetailColumnConfig[]>(() => [
  ROW_NO,
  col('subjectName', '费用名称', 'text', 200),
  col('subjectCode', '费用编号', 'text', 120),
  col('amount', '费用金额', 'money', 130),
  ...matrixColumns.value.map(matrixCol)
])

// ── Tab2「按职员」：全部 3 列 / 默认 3（全可见）＋动态职员矩阵列 ──
const staffColumns = computed<DetailColumnConfig[]>(() => [
  ROW_NO,
  col('subjectName', '费用名称', 'text', 200),
  col('subjectCode', '费用编号', 'text', 120),
  col('amount', '费用金额', 'money', 130),
  ...matrixColumns.value.map(matrixCol)
])

// ── Tab3「按明细」：全部 15 列 / 默认 9（默认隐藏 6） ──
const detailColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('bizDate', '单据日期', 'text', 110),
  col('docNo', '单据编号', 'text', 190),
  col('docType', '单据类型', 'text', 120),
  col('partnerName', '往来单位', 'text', 180),
  col('partnerCode', '往来单位编号', 'text', 130, true),
  col('subjectName', '科目', 'text', 150),
  col('subjectCode', '科目编号', 'text', 110, true),
  col('amount', '金额', 'money', 120),
  col('handlerName', '经手人', 'text', 110),
  col('deptName', '部门', 'text', 110, true),
  col('creatorName', '制单人', 'text', 110),
  col('docSummary', '单据摘要', 'text', 160, true),
  col('docRemark', '单据备注', 'text', 160, true),
  col('itemRemark', '明细摘要', 'text', 160, true),
  col('bookkeepingTime', '记账时间', 'text', 160)
]

// ── Tab4「按往来单位」：全部 4 列 / 默认 4（全可见） ──
const partnerColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('partnerCode', '单位编号', 'text', 130),
  col('partnerName', '单位名称', 'text', 200),
  col('amount', '费用金额', 'money', 130),
  col('ratio', '占比(%)', 'rate', 110)
]

const activeColumns = computed<DetailColumnConfig[]>(() => {
  if (activeTab.value === 'staff') return staffColumns.value
  if (activeTab.value === 'detail') return detailColumns
  if (activeTab.value === 'partner') return partnerColumns
  return deptColumns.value
})

const leafColumns = computed<DetailColumnConfig[]>(() =>
  activeColumns.value.flatMap(c => (c.children?.length ? c.children : [c])))

/** `全部可配置列/默认显示列`（供真机校验对标 3/3、3/3、15/9、4/4） */
const colSummary = computed(() => {
  const leaf = leafColumns.value.filter(c => c.type !== 'rowNo' && c.type !== 'action' && c.key !== 'checkbox')
  return `${leaf.length}/${leaf.filter(c => !c.defaultHidden).length}`
})

/** 费用科目分类树（按部门/按职员两 Tab；取自当前结果集的科目行，非伪造） */
const categoryTree = computed(() => [
  {
    key: '0',
    title: '全部',
    children: rows.value
      .filter(r => r.subjectCode)
      .map(r => ({ key: String(r.subjectCode), title: `${r.subjectCode} ${r.subjectName}` }))
  }
])

/** 底部合计行：取后端 summary（按当前过滤范围 SUM） */
const SUMMARY_KEYS = ['amount']
const summaryColumns = computed(() =>
  SUMMARY_KEYS.filter(k => leafColumns.value.some(c => c.key === k))
    .map(k => ({ key: k, value: Number(summary.value[k]) || 0 })))

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'expense.dateRange', label: '日期', visible: true },
  { key: 'expense.dateType', label: '日期类型', visible: true },
  { key: 'expense.subject', label: '科目', visible: true },
  { key: 'expense.dept', label: '部门', visible: true },
  { key: 'expense.partner', label: '往来单位', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'print', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]
const {
  showPageConfig, queryFields, functionButtons,
  isQueryVisible, isButtonEnabled, handlePageConfigChange, pageConfigStorageKey
} = useAnalyticsPageConfig({
  storageKey: 'analytics-check-expense-page-config',
  defaultQueryFields: DEFAULT_QUERY_FIELDS,
  defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
})

// ═══ 查询方案 ═══
function schemeSnapshot(): Record<string, any> {
  return {
    dateRange: [...(dateRange.value || [])],
    quickDate: quickDate.value,
    selectedCategoryId: selectedCategoryId.value,
    ...query
  }
}
function applyScheme(v: Record<string, any>) {
  if (Array.isArray(v.dateRange) && v.dateRange.length === 2) {
    dateRange.value = [String(v.dateRange[0]), String(v.dateRange[1])]
  }
  if (typeof v.quickDate === 'string') quickDate.value = v.quickDate
  if (v.selectedCategoryId !== undefined) selectedCategoryId.value = v.selectedCategoryId
  Object.keys(query).forEach(k => { if (k in v) (query as any)[k] = v[k] })
  handleSearch()
}

// ═══ 取数 ═══
function buildParams(extra: Record<string, any> = {}) {
  return {
    tab: activeTab.value,
    startDate: dateRange.value?.[0],
    endDate: dateRange.value?.[1],
    keyword: query.keyword || undefined,
    deptName: query.deptName || undefined,
    partnerName: query.partnerName || undefined,
    includeReversed: query.includeReversed || undefined,
    page: pagination.page,
    size: pagination.size,
    ...extra
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = activeTab.value === 'detail'
      ? await expenseAnalyticsApi.detail(buildParams())
      : activeTab.value === 'partner'
        ? await expenseAnalyticsApi.partner(buildParams())
        : await expenseAnalyticsApi.matrix(buildParams())
    rows.value = (res?.records || []).map((r: any, i: number) =>
      ({ ...r, rowKey: `${activeTab.value}-${r.subjectCode || r.docNo || r.partnerName || i}-${i}` }))
    pagination.total = Number(res?.total) || 0
    summary.value = res?.summary || {}
    if (activeTab.value === 'dept' || activeTab.value === 'staff') {
      matrixColumns.value = res?.matrixColumns || []
    } else {
      matrixColumns.value = []
    }
  } catch (e) {
    console.warn('[查费用] 取数失败', e)
    message.error('获取数据失败')
    rows.value = []
    pagination.total = 0
    summary.value = {}
    matrixColumns.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() { pagination.page = 1; fetchData() }
function handlePageChange(page: number, size: number) {
  pagination.page = page
  pagination.size = size
  fetchData()
}
function handleReset() {
  query.keyword = ''
  query.deptName = ''
  query.partnerName = ''
  query.includeReversed = false
  selectedCategoryId.value = '0'
  quickDate.value = 'month'
  dateRange.value = quickDateRange('month') as [string, string]
  handleSearch()
}
function setQuickDate(key: string) {
  quickDate.value = key
  dateRange.value = quickDateRange(key) as [string, string]
  handleSearch()
}
function onTabChange(key: string) {
  activeTab.value = key
  pagination.page = 1
  fetchData()
}
/** 分类树选择 → 以「科目编号」作为服务端过滤条件（合计随之变化，不做前端假过滤） */
function onCategorySelect(keys: (string | number)[]) {
  const k = Array.isArray(keys) ? keys[0] : keys
  selectedCategoryId.value = k === undefined || k === null ? '0' : k
  query.keyword = String(selectedCategoryId.value) === '0' ? '' : String(selectedCategoryId.value)
  query.deptName = ''
  query.partnerName = ''
  handleSearch()
}

// ═══ 打印(F8) / 导出 ═══
const printableColumns = computed<DetailColumnConfig[]>(() =>
  leafColumns.value.filter(c => c.type !== 'rowNo' && !c.defaultHidden))

function cellText(c: DetailColumnConfig, r: any): string {
  const raw = c.type === 'slot' ? (r.cells || {})[c.title] : r[c.key]
  return c.formatter ? c.formatter(raw, r) : fmtText(raw)
}

/**
 * 打印视图：把「屏幕上的这批列 + 这批行」拍平成引擎能画的一张平表。
 *
 * 为什么必须自己拍（透视矩阵在纸上没法原样表达，Jasper crosstab / SSRS 打印时也是拍平）：
 * ① 默认页签是分组透视矩阵（行 = 费用科目，列 = 部门/职员指标列）。矩阵列在组件里统一借
 *    `key='checkbox'` 复用「锁定列」判定，即**多列共用一个 key**。若把这批列原样交给
 *    useListPrint，`toPrintColumns` 会照 key 发 8 条 field 全为 `checkbox` 的列定义，
 *    `normalizeRow` 也只产出 `checkbox` 一个字段 —— 模板逐列读同一个字段，
 *    屏幕上的 1231.04/117.21/39.16 全部丢失，整批矩阵列打出来是空的。
 *    这里按位次给每列一个唯一 key（c0/c1…），彻底避开 key 复用。
 * ② 列 = [分组/维度列（费用名称·费用编号·费用金额），…该透视当前的指标列]，顺序照屏幕；
 *    行 = 叶子行，每行一个分组 + 各指标取值。取值口径逐字沿用改造前自建 HTML 打印所用的
 *    `cellText`（矩阵列读 `record.cells[列名]`、金额走 fmtMoney…），故列数与每格文本与改造前一致。
 * ③ 值在拍平这步就格式化成最终文本（引擎未给 digits 时原样输出）。本页模板没有 agg 合计
 *    （合计走 totalText），故前置格式化不会破坏任何求和。
 */
const printView = computed(() => {
  const cols = printableColumns.value
  const columns = cols.map((c, i) => ({ key: `c${i}`, title: c.title, align: c.align }))
  const rows = rows.value.map(r => {
    const out: Record<string, any> = {}
    cols.forEach((c, i) => { out[`c${i}`] = cellText(c, r) })
    return out
  })
  return { columns, rows }
})

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML + window.open 打印窗口（打当前页签的透视列，行只打当前页）。
// 现在交给 PrintDialog：列/行由页面给（列随页签/筛选变 → 显式 useDataColumns 按数据列打），
// 标题（页签名 + 日期区间）与底部合计分别由模板 pageHeader / pageFooter 承载。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'analytics-check-expense',
  title: () => {
    const tabLabel = TABS.find(t => t.key === activeTab.value)?.label || '查费用'
    return `查费用 · ${tabLabel}（${dateRange.value?.[0]} ~ ${dateRange.value?.[1]}）`
  },
  useDataColumns: true,
  columns: () => printView.value.columns,
  rows: () => printView.value.rows,
  // 屏幕底部合计行（后端 summary，本页仅「费用金额」一列）→ 模板 pageFooter
  totalText: () => `合计：费用金额 ${fmtMoney(Number(summary.value.amount) || 0)}`,
  emptyTip: '没有可打印的数据',
})

function handleF8Key(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

const { execute: executeExport, exporting } = useExport()

async function fetchAllRows(): Promise<any[]> {
  const size = 200
  const all: any[] = []
  const pages = Math.max(1, Math.ceil(pagination.total / size))
  for (let p = 1; p <= pages; p++) {
    const res: any = activeTab.value === 'detail'
      ? await expenseAnalyticsApi.detail(buildParams({ page: p, size }))
      : activeTab.value === 'partner'
        ? await expenseAnalyticsApi.partner(buildParams({ page: p, size }))
        : await expenseAnalyticsApi.matrix(buildParams({ page: p, size }))
    const list = res?.records || []
    all.push(...list)
    if (list.length < size) break
  }
  return all
}

function handleExport() {
  const cols = printableColumns.value
  executeExport({
    fileName: `查费用-${TABS.find(t => t.key === activeTab.value)?.label || ''}`,
    headers: cols.map(c => c.title),
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: (list: any[]) => list.map(r => cols.map(c => cellText(c, r))),
    fallbackRows: () => rows.value.map(r => cols.map(c => cellText(c, r)))
  })
}

function handleError(err: any) {
  console.error('[查费用] 页面异常', err)
}

onMounted(() => {
  fetchData()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleF8Key)
})
</script>

<style scoped>
/* 插槽内容不受宿主 scoped 样式影响，查询区样式随页面自带 */
.search-area { width: 100%; }
.search-grid { display: flex; flex-wrap: wrap; gap: 8px 16px; align-items: center; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-label { color: #666; font-size: 13px; white-space: nowrap; }
.search-actions { margin-left: auto; }
.quick-dates { margin-left: 8px; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
</style>
