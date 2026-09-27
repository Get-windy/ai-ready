<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        销售欠款分析（分析 → 采销分析 → 销售欠款分析，菜单 80415）
        对标 ql361「销售欠款分析」：3 个视图 Tab（按职员 15/12、按客户 17/16、按区域 17/16），逐 Tab 独立列配置；
        列 = 欠款滚动五要素（此前欠款 → 本期新增欠款 → 本期新增收款 → 结算优惠 → 欠款余额）
        + 超期欠款总额 + 账龄五档 + 信用额度三项，列名逐字取自《销售欠款分析开发文档》§3；
        查询区：时间快捷段 8 段 + 日期范围 + 客户 + 日期类型（默认「单据日期」）+ 显示层次（横向网格）；
        左侧分类树随 Tab 切换（按职员=部门树、按区域=区域树、按客户=客户分类树）；
        工具栏：设置｜刷新｜打印(F8)｜导出｜页面配置（对标有「按职员/按客户/按区域-页面配置弹窗」实据 → 接 PageConfigPanel）。
        取数：/erp/sale/analysis/sales-debt/page?tab=staff|customer|region。
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="true"
        :category-title="categoryTitle"
        :category-tree-data="categoryTree"
        :category-loading="categoryLoading"
        :category-editable="false"
        :selected-category-id="selectedCategoryId"
        :current-path="currentPath"
        :show-table-footer="true"
        @tab-change="onTabChange"
        @category-select="onCategorySelect"
      >
        <!-- ═══ 工具栏右侧：刷新｜打印(F8)｜导出｜页面配置（对标「设置」弹窗口径未抓取，不伪造入口） ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button v-if="isButtonEnabled('refresh')" size="small" :loading="loading" @click="fetchData">
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

        <!-- ═══ 查询区（对标查询项，横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div class="search-item">
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
              <div v-if="isQueryVisible(`${activeTab}.customerName`)" class="search-item">
                <span class="search-label">客户</span>
                <a-input v-model:value="query.customerName" size="small" placeholder="客户名称" allow-clear style="width: 170px" @press-enter="handleSearch" />
              </div>
              <div v-if="isQueryVisible(`${activeTab}.dateType`)" class="search-item">
                <span class="search-label">日期类型</span>
                <a-select v-model:value="query.dateType" size="small" style="width: 130px" :options="DATE_TYPE_OPTIONS" @change="handleSearch" />
              </div>
              <div class="search-item search-actions">
                <a-checkbox v-model:checked="query.showHierarchy" @change="handleSearch">显示层次</a-checkbox>
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
              row-key="dimKey"
              :storage-key="`analytics-sales-debt-columns-${activeTab}`"
              :global-config-key="`analytics-sales-debt-columns-${activeTab}`"
            >
              <template #emptyCell>
                <span class="cell-empty">-</span>
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

      <!-- ═══ 页面配置（对标有「按职员/按客户/按区域-页面配置弹窗」实据 → 逐 Tab 独立 storage-key） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="defaultQueryFields"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="`analytics-sales-debt-page-config-${activeTab}`"
        :print-config-items="PRINT_ITEMS"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>

    <!-- 打印：结果集打印 -->
    <PrintDialog ref="printDialogRef" page-code="analytics-sales-debt" :print-data="printData" />
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
import { saleAnalysisReportApi } from '@/api/analytics-sales'
import type { SaleAnalysisQuery } from '@/api/analytics-sales'
import { departmentApi } from '@/api/department'
import { customerRegionApi, partnerCategoryApi } from '@/api/erp/partner'
import { useExport } from '@/composables/useExport'
import { QUICK_DATES, quickDateRange } from '../shared/docTypes'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'AnalyticsSalesDebt' })

// ═══ 视图 Tab（对标实测顺序：按职员 / 按客户 / 按区域） ═══
const TABS = [
  { key: 'staff', label: '按职员' },
  { key: 'customer', label: '按客户' },
  { key: 'region', label: '按区域' }
]
const activeTab = ref('staff')
const quickDate = ref('month')
const dateRange = ref<[string, string]>(quickDateRange('month') as [string, string])

/** 日期类型（对标实测默认「单据日期」；可选值对标未抓全，本系统仅提供可得口径） */
const DATE_TYPE_OPTIONS = [
  { label: '单据日期', value: 'bizDate' },
  { label: '到期日期', value: 'dueDate' }
]

const query = reactive({
  scheme: '',
  customerName: '',
  dateType: 'bizDate',
  showHierarchy: false,
  /** 左侧分类树选中的节点名（按职员=部门名、按区域=区域名，后端按名称模糊过滤） */
  categoryName: '',
  /** 左侧客户分类树选中的节点及其全部下级ID（逗号分隔） */
  categoryIds: ''
})

const loading = ref(false)
const rows = ref<any[]>([])
const pagination = reactive({ page: 1, size: 20, total: 0 })
const summary = ref<Record<string, any>>({})

function buildParams(): SaleAnalysisQuery {
  return {
    tab: activeTab.value,
    startDate: dateRange.value?.[0],
    endDate: dateRange.value?.[1],
    customerName: query.customerName || undefined,
    // 左侧分类树下推：部门/区域按名称模糊、客户分类按ID集合
    deptName: activeTab.value === 'staff' ? (query.categoryName || undefined) : undefined,
    region: activeTab.value === 'region' ? (query.categoryName || undefined) : undefined,
    customerCategoryIds: activeTab.value === 'customer' ? (query.categoryIds || undefined) : undefined,
    page: pagination.page,
    size: pagination.size
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await saleAnalysisReportApi.debt(buildParams())
    rows.value = res?.records || []
    pagination.total = res?.total || 0
    summary.value = res?.summary || {}
  } catch (e) {
    console.warn('[销售欠款分析] 取数失败', e)
    rows.value = []
    pagination.total = 0
    summary.value = {}
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.page = page
  pagination.size = size
  fetchData()
}

function handleReset() {
  Object.assign(query, { scheme: '', customerName: '', dateType: 'bizDate', showHierarchy: false, categoryName: '', categoryIds: '' })
  dateRange.value = quickDateRange('month') as [string, string]
  quickDate.value = 'month'
  selectedCategoryId.value = '0'
  currentPath.value = categoryRootPath.value
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
  selectedCategoryId.value = '0'
  query.categoryName = ''
  query.categoryIds = ''
  fetchCategoryTree()
  fetchData()
}

// ═══ 左侧分类树（随 Tab 切换：按职员=部门树、按区域=区域树、按客户=客户分类树） ═══
const categoryTree = ref<any[]>([])
const categoryLoading = ref(false)
const selectedCategoryId = ref<string | number>('0')
const currentPath = ref('全部')
const categoryRootPath = computed(() =>
  activeTab.value === 'staff' ? '全部' : activeTab.value === 'region' ? '全部' : '全部往来单位'
)
const categoryTitle = computed(() =>
  activeTab.value === 'staff' ? '部门' : activeTab.value === 'region' ? '区域' : '客户分类'
)

async function fetchCategoryTree() {
  categoryLoading.value = true
  currentPath.value = categoryRootPath.value
  try {
    if (activeTab.value === 'staff') {
      const res: any = await departmentApi.getTree()
      categoryTree.value = mapTree(Array.isArray(res) ? res : res?.data || [], 'departmentName')
    } else if (activeTab.value === 'region') {
      const res: any = await customerRegionApi.list()
      const list = Array.isArray(res) ? res : res?.data || []
      categoryTree.value = buildFlatTree(list.map((r: any) => ({
        id: r.id, categoryName: r.regionName, parentId: r.parentId
      })))
    } else {
      const res: any = await partnerCategoryApi.getTree('CUSTOMER')
      categoryTree.value = mapTree(Array.isArray(res) ? res : res?.data || [], 'categoryName')
    }
  } catch (e) {
    console.warn('[销售欠款分析] 分类树获取失败', e)
    categoryTree.value = []
  } finally {
    categoryLoading.value = false
  }
}

function mapTree(list: any[], nameField: string): any[] {
  return (list || []).map(n => ({
    id: n.id,
    categoryName: n[nameField],
    children: n.children?.length ? mapTree(n.children, nameField) : undefined
  }))
}

/** 扁平列表（parentId 关联）→ 树 */
function buildFlatTree(list: any[]): any[] {
  const map = new Map<string, any>()
  list.forEach(n => map.set(String(n.id), { ...n, children: [] }))
  const roots: any[] = []
  list.forEach(n => {
    const node = map.get(String(n.id))
    const parent = n.parentId != null ? map.get(String(n.parentId)) : null
    if (parent) parent.children.push(node)
    else roots.push(node)
  })
  const prune = (nodes: any[]): any[] => nodes.map(n => ({
    ...n,
    children: n.children.length ? prune(n.children) : undefined
  }))
  return prune(roots)
}

function findNode(nodes: any[], id: string): any | null {
  for (const n of nodes || []) {
    if (String(n.id) === id) return n
    const found = n.children?.length ? findNode(n.children, id) : null
    if (found) return found
  }
  return null
}

function collectIds(nodes: any[], id: string): string[] | null {
  const node = findNode(nodes, id)
  if (!node) return null
  const out: string[] = []
  const walk = (n: any) => {
    out.push(String(n.id))
    ;(n.children || []).forEach(walk)
  }
  walk(node)
  return out
}

function onCategorySelect(keys: (string | number)[]) {
  const key = keys?.length ? String(keys[0]) : ''
  selectedCategoryId.value = key || '0'
  const node = key ? findNode(categoryTree.value, key) : null
  query.categoryName = node ? node.categoryName : ''
  query.categoryIds = activeTab.value === 'customer' && key ? (collectIds(categoryTree.value, key) || []).join(',') : ''
  currentPath.value = node ? node.categoryName : categoryRootPath.value
  handleSearch()
}

// ═══ 列定义辅助（列名逐字取自《销售欠款分析开发文档》§3） ═══
type Kind = 'money' | 'num' | 'rate' | 'text'

function fmtNum(v: any): string {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '-'
  return Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function fmtMoney(v: any): string {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '-'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const FORMATTER: Record<Kind, (v: any) => string> = {
  money: fmtMoney,
  num: fmtNum,
  rate: v => (v === null || v === undefined || v === '' || isNaN(Number(v)) ? '-' : `${fmtNum(v)}%`),
  text: v => (v === null || v === undefined || v === '' ? '-' : String(v))
}

function col(key: string, title: string, kind: Kind, width = 110, hidden = false): DetailColumnConfig {
  return {
    key,
    title,
    width,
    align: kind === 'text' ? 'left' : 'right',
    formatter: FORMATTER[kind],
    ...(hidden ? { defaultHidden: true } : {})
  }
}

/** 无数据源列（职员编号等，详见开发文档「缺口登记」） */
function gap(key: string, title: string, width = 110, hidden = false): DetailColumnConfig {
  return {
    key,
    title,
    width,
    align: 'right',
    type: 'slot',
    slotName: 'emptyCell',
    ...(hidden ? { defaultHidden: true } : {})
  }
}

const ROW_NO: DetailColumnConfig = { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' }

/** 超期欠款分布（账龄五档，逐 Tab 一致，默认全部可见） */
function agingGroup(): DetailColumnConfig {
  return {
    key: 'agingGroup',
    title: '超期欠款分布',
    children: [
      col('agingLt1m', '小于1个月', 'money', 110),
      col('aging1to3m', '1-3个月', 'money', 110),
      col('aging3to6m', '3-6个月', 'money', 110),
      col('aging6to12m', '6-12个月', 'money', 110),
      col('agingGt12m', '12个月以上', 'money', 120)
    ]
  }
}

// ── Tab1「按职员」：全部 15 / 默认 12 ──
const staffColumns: DetailColumnConfig[] = [
  ROW_NO,
  gap('staffCode', '职员编号', 110, true),
  col('dimLabel', '职员名称', 'text', 130),
  col('deptName', '所属部门', 'text', 120, true),
  col('netSaleAmount', '实销金额', 'money', 110),
  col('prevDebt', '此前欠款', 'money', 110),
  col('newDebt', '本期新增欠款', 'money', 120),
  col('settleDiscountInPeriod', '结算优惠', 'money', 110),
  col('receiptInPeriod', '本期新增收款', 'money', 120, true),
  col('debtBalance', '欠款余额', 'money', 110),
  col('overdueDebt', '超期欠款总额', 'money', 120),
  agingGroup()
]

// ── Tab2「按客户」：全部 17 / 默认 16（唯一默认隐藏列：本期新增欠款） ──
const customerColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('dimLabel', '客户名称', 'text', 200),
  col('customerCode', '客户编号', 'text', 110),
  col('netSaleAmount', '实销金额', 'money', 110),
  col('prevDebt', '此前欠款', 'money', 110),
  col('newDebt', '本期新增欠款', 'money', 120, true),
  col('settleDiscountInPeriod', '结算优惠', 'money', 110),
  col('receiptInPeriod', '本期新增收款', 'money', 120),
  col('debtBalance', '欠款余额', 'money', 110),
  col('overdueDebt', '超期欠款总额', 'money', 120),
  agingGroup(),
  col('creditLimit', '信用总额度', 'money', 110),
  col('usedCredit', '已用信用额度', 'money', 120),
  col('creditUsageRate', '信用额度使用率', 'rate', 130)
]

// ── Tab3「按区域」：全部 17 / 默认 16（与按客户同构，仅维度列不同） ──
const regionColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('dimLabel', '区域名称', 'text', 160),
  gap('regionCode', '区域编号', 110),
  col('netSaleAmount', '实销金额', 'money', 110),
  col('prevDebt', '此前欠款', 'money', 110),
  col('newDebt', '本期新增欠款', 'money', 120, true),
  col('settleDiscountInPeriod', '结算优惠', 'money', 110),
  col('receiptInPeriod', '本期新增收款', 'money', 120),
  col('debtBalance', '欠款余额', 'money', 110),
  col('overdueDebt', '超期欠款总额', 'money', 120),
  agingGroup(),
  col('creditLimit', '信用总额度', 'money', 110),
  col('usedCredit', '已用信用额度', 'money', 120),
  col('creditUsageRate', '信用额度使用率', 'rate', 130)
]

const COLUMNS_BY_TAB: Record<string, DetailColumnConfig[]> = {
  staff: staffColumns, customer: customerColumns, region: regionColumns
}
const activeColumns = computed<DetailColumnConfig[]>(() => COLUMNS_BY_TAB[activeTab.value] || staffColumns)
const leafColumns = computed<DetailColumnConfig[]>(() =>
  activeColumns.value.flatMap(c => (c.children?.length ? c.children : [c]))
)
const configurableColumns = computed<DetailColumnConfig[]>(() =>
  leafColumns.value.filter(c => c.key !== 'rowNo')
)
/** `全部可配置列/默认显示列`（供真机校验对标 15/12、17/16、17/16） */
const colSummary = computed(() => {
  const total = configurableColumns.value.length
  const def = configurableColumns.value.filter(c => !c.defaultHidden).length
  return `${total}/${def}`
})

const SUMMARY_KEYS = ['netSaleAmount', 'prevDebt', 'newDebt', 'settleDiscountInPeriod', 'receiptInPeriod',
  'debtBalance', 'overdueDebt', 'agingLt1m', 'aging1to3m', 'aging3to6m', 'aging6to12m', 'agingGt12m',
  'creditLimit', 'usedCredit']
const summaryColumns = computed(() => SUMMARY_KEYS
  .filter(k => configurableColumns.value.some(c => c.key === k))
  .map(k => ({ key: k, value: Number(summary.value[k]) || 0 })))

// ═══ 页面配置（逐 Tab 一套查询条件清单与 storage-key） ═══
const QUERY_FIELDS_BY_TAB: Record<string, QueryFieldSetting[]> = {
  staff: [
    { key: 'staff.customerName', label: '客户', visible: true },
    { key: 'staff.dateType', label: '日期类型', visible: true }
  ],
  customer: [
    { key: 'customer.customerName', label: '客户', visible: true },
    { key: 'customer.dateType', label: '日期类型', visible: true }
  ],
  region: [
    { key: 'region.customerName', label: '客户', visible: true },
    { key: 'region.dateType', label: '日期类型', visible: true }
  ]
}
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]
const PRINT_ITEMS = [
  { key: 'showCompany', label: '打印抬头显示公司名' },
  { key: 'showSummary', label: '打印底部显示合计行' }
]

const configByTab = {
  staff: useAnalyticsPageConfig({
    storageKey: 'analytics-sales-debt-page-config-staff',
    defaultQueryFields: QUERY_FIELDS_BY_TAB.staff,
    defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
  }),
  customer: useAnalyticsPageConfig({
    storageKey: 'analytics-sales-debt-page-config-customer',
    defaultQueryFields: QUERY_FIELDS_BY_TAB.customer,
    defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
  }),
  region: useAnalyticsPageConfig({
    storageKey: 'analytics-sales-debt-page-config-region',
    defaultQueryFields: QUERY_FIELDS_BY_TAB.region,
    defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
  })
}

const activeConfig = computed(() => configByTab[activeTab.value as 'staff' | 'customer' | 'region'])
const queryFields = computed(() => activeConfig.value.queryFields.value)
const functionButtons = computed(() => activeConfig.value.functionButtons.value)
const defaultQueryFields = computed(() => QUERY_FIELDS_BY_TAB[activeTab.value])
const showPageConfig = ref(false)

function isQueryVisible(key: string): boolean {
  return activeConfig.value.isQueryVisible(key)
}

function isButtonEnabled(key: string): boolean {
  return activeConfig.value.isButtonEnabled(key)
}

function handlePageConfigChange(config: any) {
  activeConfig.value.handlePageConfigChange(config)
}

// ═══ 打印(F8) / 导出 ═══
const printableColumns = computed<DetailColumnConfig[]>(() =>
  leafColumns.value.filter(c => c.key !== 'rowNo' && !c.defaultHidden)
)

function cellText(c: DetailColumnConfig, r: any): string {
  const raw = r[c.key]
  return c.formatter ? c.formatter(raw, r) : (raw === null || raw === undefined || raw === '' ? '-' : String(raw))
}

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML 再调浏览器打印，现在交给 PrintDialog：
// 列随 Tab 变化（computed），按当前可见列打，故 useDataColumns。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'analytics-sales-debt',
  title: () => {
    const tabLabel = TABS.find(t => t.key === activeTab.value)?.label || '销售欠款分析'
    return `销售欠款分析 · ${tabLabel}（${dateRange.value?.[0]} ~ ${dateRange.value?.[1]}）`
  },
  rows: () => rows.value,
  columns: () => printableColumns.value,
  useDataColumns: true,
  emptyTip: '没有可打印的数据',
})

/** F8 快捷键（对标工具栏「打印(F8)」） */
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
    const res = await saleAnalysisReportApi.debt({ ...buildParams(), page: p, size })
    const list = res?.records || []
    all.push(...list)
    if (list.length < size) break
  }
  return all
}

function handleExport() {
  const cols = printableColumns.value
  executeExport({
    fileName: `销售欠款分析-${TABS.find(t => t.key === activeTab.value)?.label || ''}`,
    headers: cols.map(c => c.title),
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: (list: any[]) => list.map(r => cols.map(c => cellText(c, r))),
    fallbackRows: () => rows.value.map(r => cols.map(c => cellText(c, r)))
  })
}

function handleError(err: any) {
  console.error('[销售欠款分析] 页面异常', err)
}

onMounted(() => {
  fetchCategoryTree()
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

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-empty { color: #bfbfbf; }
</style>
