<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        销售业绩（分析 → 采销分析 → 销售业绩，菜单 80411）
        对标 ql361「销售业绩」：2 个视图 Tab（按时间 27/11、按职员 34/12），逐 Tab 独立列配置；
        漏斗全链路列（拓客 → 拜访 → 订货 → 销售 → 退货 → 回款 → 利润），列名逐字取自《销售业绩开发文档》§3；
        查询区：查询方案 + 时间快捷段 8 段 + 日期范围 + 客户/经手人/区域 + 不显示停用职员（横向网格）；
        左侧部门分类树（对标实测：全部/管理部/客服部/配送部/仓管）；
        工具栏：刷新｜打印(F8)｜导出｜页面配置（对标有「按时间/按职员-页面配置弹窗」实据 → 接 PageConfigPanel，逐 Tab 独立 storage-key）。
        取数：/erp/sale/analysis/sales-performance/page?tab=time|staff。
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="true"
        category-title="部门"
        :category-tree-data="categoryTree"
        :category-loading="categoryLoading"
        :category-editable="false"
        :selected-category-id="selectedCategoryId"
        :current-path="currentPath"
        :show-table-footer="true"
        @tab-change="onTabChange"
        @category-select="onCategorySelect"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 时间快捷段 ═══ -->
        <template #toolbar-left>
          <QuerySchemeBar
            storage-key="analytics-sales-performance\index.vue-query-scheme"
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
                <a-input
                  v-model:value="query.customerName"
                  size="small"
                  placeholder="客户名称"
                  allow-clear
                  style="width: 170px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible(`${activeTab}.salesmanName`)" class="search-item">
                <span class="search-label">经手人</span>
                <a-input
                  v-model:value="query.salesmanName"
                  size="small"
                  placeholder="经手人"
                  allow-clear
                  style="width: 140px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible(`${activeTab}.region`)" class="search-item">
                <span class="search-label">区域</span>
                <a-input
                  v-model:value="query.region"
                  size="small"
                  placeholder="区域"
                  allow-clear
                  style="width: 130px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item search-actions">
                <a-checkbox v-model:checked="query.hideDisabledStaff" @change="handleSearch">不显示停用职员</a-checkbox>
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
              :storage-key="`analytics-sales-performance-columns-${activeTab}`"
              :global-config-key="`analytics-sales-performance-columns-${activeTab}`"
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

      <!-- ═══ 页面配置（对标有「按时间/按职员-页面配置弹窗」实据 → 逐 Tab 独立 storage-key） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="defaultQueryFields"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="`analytics-sales-performance-page-config-${activeTab}`"
        :print-config-items="PRINT_ITEMS"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>

    <!-- 打印：结果集打印 -->
    <PrintDialog ref="printDialogRef" page-code="analytics-sales-performance" :print-data="printData" />
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
import { useExport } from '@/composables/useExport'
import { QUICK_DATES, quickDateRange } from '../shared/docTypes'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'AnalyticsSalesPerformance' })

// ═══ 视图 Tab（对标实测顺序：按时间 / 按职员） ═══
const TABS = [
  { key: 'time', label: '按时间' },
  { key: 'staff', label: '按职员' }
]
const activeTab = ref('time')
const quickDate = ref('month')
const dateRange = ref<[string, string]>(quickDateRange('month') as [string, string])

const query = reactive({
  scheme: '',
  customerName: '',
  salesmanName: '',
  region: '',
  /** 左侧部门树选中的部门名（后端按 department_name 模糊过滤） */
  deptName: undefined as string | undefined,
  hideDisabledStaff: false
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
    salesmanName: query.salesmanName || undefined,
    region: query.region || undefined,
    deptName: query.deptName || undefined,
    hideDisabledStaff: query.hideDisabledStaff || undefined,
    page: pagination.page,
    size: pagination.size
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await saleAnalysisReportApi.performance(buildParams())
    rows.value = res?.records || []
    pagination.total = res?.total || 0
    summary.value = res?.summary || {}
  } catch (e) {
    console.warn('[销售业绩] 取数失败', e)
    rows.value = []
    pagination.total = 0
    summary.value = {}
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

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
  pagination.page = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.page = page
  pagination.size = size
  fetchData()
}

function handleReset() {
  Object.assign(query, { scheme: '', customerName: '', salesmanName: '', region: '', hideDisabledStaff: false, deptName: undefined })
  dateRange.value = quickDateRange('month') as [string, string]
  quickDate.value = 'month'
  selectedCategoryId.value = '0'
  currentPath.value = '全部'
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

// ═══ 左侧部门分类树（对标实测：全部/管理部/客服部/配送部/仓管） ═══
const categoryTree = ref<any[]>([])
const categoryLoading = ref(false)
const selectedCategoryId = ref<string | number>('0')
const currentPath = ref('全部')

async function fetchCategoryTree() {
  categoryLoading.value = true
  try {
    const res: any = await departmentApi.getTree()
    const list = Array.isArray(res) ? res : res?.data || []
    categoryTree.value = mapDeptTree(list)
  } catch (e) {
    console.warn('[销售业绩] 部门树获取失败', e)
    categoryTree.value = []
  } finally {
    categoryLoading.value = false
  }
}

/** 部门树 → CategoryListLayout 期望的 {id, categoryName, children} 结构 */
function mapDeptTree(list: any[]): any[] {
  return (list || []).map(d => ({
    id: d.id,
    categoryName: d.departmentName,
    children: d.children?.length ? mapDeptTree(d.children) : undefined
  }))
}

function findDeptName(nodes: any[], id: string): string | null {
  for (const n of nodes || []) {
    if (String(n.id) === id) return n.categoryName
    const found = n.children?.length ? findDeptName(n.children, id) : null
    if (found) return found
  }
  return null
}

function onCategorySelect(keys: (string | number)[]) {
  const key = keys?.length ? String(keys[0]) : ''
  selectedCategoryId.value = key || '0'
  const deptName = key ? findDeptName(categoryTree.value, key) : null
  // 部门过滤按名称下推（后端对 department_name 做模糊匹配）
  query.deptName = deptName || undefined
  currentPath.value = deptName || '全部'
  handleSearch()
}

// ═══ 列定义辅助（列名逐字取自《销售业绩开发文档》§3） ═══
type Kind = 'money' | 'num' | 'rate' | 'text'

function fmtNum(v: any): string {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '-'
  return Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 4 })
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

/** 无数据源列（业绩目标/完成率、拜访统计等，详见开发文档「缺口登记」） */
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

// ── Tab1「按时间」：全部 27 / 默认 11 ──
const timeColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('dimLabel', '日期', 'text', 110),
  col('newCustomerCount', '拓客数', 'num', 90),
  {
    key: 'visitGroup',
    title: '拜访统计',
    children: [gap('visitCustomerCount', '拜访客户数', 110), gap('visitCompleteRate', '拜访完成率（%）', 130)]
  },
  {
    key: 'orderGroup',
    title: '订货统计',
    children: [
      col('orderCustomerCount', '订货客户数', 'num', 110),
      col('orderAmount', '订货金额', 'money', 110, true),
      col('netOrderAmount', '实订金额', 'money', 110)
    ]
  },
  {
    key: 'saleGroup',
    title: '销售统计',
    children: [
      col('saleCustomerCount', '销售客户数', 'num', 110, true),
      col('netSaleAmount', '实销金额', 'money', 110),
      col('otherFee', '其他费用', 'money', 100),
      gap('saleTarget', '销售业绩目标', 120, true),
      gap('saleCompleteRate', '销售业绩完成率（%）', 150, true)
    ]
  },
  {
    key: 'returnGroup',
    title: '退货统计',
    children: [
      col('orderReturnAmount', '退订金额', 'money', 110, true),
      col('orderReturnRate', '退订率(%)', 'rate', 100, true),
      col('returnAmount', '退货金额', 'money', 110),
      col('returnRate', '退货率(%)', 'rate', 100)
    ]
  },
  {
    key: 'receiptGroup',
    title: '回款数据',
    children: [
      col('receiptCustomerCount', '回款客户数', 'num', 110, true),
      col('receiptAmount', '回款总额', 'money', 110),
      col('onTimeReceiptAmount', '按期回款总额', 'money', 120, true),
      col('overdueReceiptAmount', '超期回款总额', 'money', 120, true),
      gap('receiptTarget', '回款业绩目标', 120, true),
      gap('receiptCompleteRate', '回款业绩完成率（%）', 150, true)
    ]
  },
  {
    key: 'profitGroup',
    title: '利润统计',
    children: [
      col('revenue', '销售收入', 'money', 110, true),
      col('costAmount', '销售成本', 'money', 110, true),
      col('grossProfit', '销售毛利', 'money', 110, true),
      col('grossMargin', '毛利率（%）', 'rate', 110, true)
    ]
  },
  col('unsettledAmount', '本期未结金额', 'money', 120, true)
]

// ── Tab2「按职员」：全部 34 / 默认 12 ──
const staffColumns: DetailColumnConfig[] = [
  ROW_NO,
  gap('staffCode', '职员编号', 110, true),
  col('dimLabel', '职员名称', 'text', 130),
  col('deptName', '所属部门', 'text', 120),
  col('newCustomerCount', '拓客数', 'num', 90),
  {
    key: 'visitGroup',
    title: '拜访统计',
    children: [gap('visitCustomerCount', '拜访客户数', 110), gap('visitCompleteRate', '拜访完成率（%）', 130)]
  },
  {
    key: 'orderGroup',
    title: '订货统计',
    children: [
      col('orderCustomerCount', '订货客户数', 'num', 110),
      col('orderDocCount', '订货笔数', 'num', 100, true),
      col('orderAmount', '订货金额', 'money', 110, true),
      col('netOrderAmount', '实订金额', 'money', 110)
    ]
  },
  {
    key: 'saleGroup',
    title: '销售统计',
    children: [
      col('saleCustomerCount', '销售客户数', 'num', 110, true),
      col('saleDocCount', '销售笔数', 'num', 100, true),
      col('saleAmount', '销售金额', 'money', 110, true),
      gap('saleTarget', '销售业绩目标', 120, true),
      gap('saleCompleteRate', '销售业绩完成率（%）', 150, true),
      col('netSaleAmount', '实销金额', 'money', 110),
      col('otherFee', '其他费用', 'money', 100)
    ]
  },
  {
    key: 'returnGroup',
    title: '退货统计',
    children: [
      gap('orderReturnDocCount', '退订单数', 100, true),
      col('orderReturnAmount', '退订金额', 'money', 110, true),
      col('orderReturnRate', '退订率(%)', 'rate', 100, true),
      col('returnDocCount', '退货单数', 'num', 100, true),
      col('returnAmount', '退货金额', 'money', 110),
      col('returnRate', '退货率(%)', 'rate', 100)
    ]
  },
  {
    key: 'receiptGroup',
    title: '回款数据',
    children: [
      col('receiptCustomerCount', '回款客户数', 'num', 110, true),
      col('receiptAmount', '回款总额', 'money', 110),
      col('onTimeReceiptAmount', '按期回款总额', 'money', 120, true),
      col('overdueReceiptAmount', '超期回款总额', 'money', 120, true),
      gap('receiptTarget', '回款业绩目标', 120, true),
      gap('receiptCompleteRate', '回款业绩完成率（%）', 150, true)
    ]
  },
  {
    key: 'profitGroup',
    title: '利润统计',
    children: [
      col('revenue', '销售收入', 'money', 110, true),
      col('costAmount', '销售成本', 'money', 110, true),
      col('grossProfit', '销售毛利', 'money', 110, true),
      col('grossMargin', '毛利率（%）', 'rate', 110, true)
    ]
  },
  col('unsettledAmount', '本期未结金额', 'money', 120, true)
]

const COLUMNS_BY_TAB: Record<string, DetailColumnConfig[]> = { time: timeColumns, staff: staffColumns }
const activeColumns = computed<DetailColumnConfig[]>(() => COLUMNS_BY_TAB[activeTab.value] || timeColumns)

const leafColumns = computed<DetailColumnConfig[]>(() =>
  activeColumns.value.flatMap(c => (c.children?.length ? c.children : [c]))
)
const configurableColumns = computed<DetailColumnConfig[]>(() =>
  leafColumns.value.filter(c => c.key !== 'rowNo')
)
/** `全部可配置列/默认显示列`（供真机校验对标 27/11、34/12） */
const colSummary = computed(() => {
  const total = configurableColumns.value.length
  const def = configurableColumns.value.filter(c => !c.defaultHidden).length
  return `${total}/${def}`
})

const SUMMARY_KEYS = ['orderAmount', 'netOrderAmount', 'netSaleAmount', 'otherFee', 'returnAmount',
  'receiptAmount', 'revenue', 'costAmount', 'grossProfit']
const summaryColumns = computed(() => SUMMARY_KEYS
  .filter(k => configurableColumns.value.some(c => c.key === k))
  .map(k => ({ key: k, value: Number(summary.value[k]) || 0 })))

// ═══ 页面配置（逐 Tab 一套查询条件清单与 storage-key） ═══
const DEFAULT_QUERY_FIELDS_BY_TAB: Record<string, QueryFieldSetting[]> = {
  time: [
    { key: 'time.customerName', label: '客户', visible: true },
    { key: 'time.salesmanName', label: '经手人', visible: true },
    { key: 'time.region', label: '区域', visible: true }
  ],
  staff: [
    { key: 'staff.customerName', label: '客户', visible: true },
    { key: 'staff.salesmanName', label: '经手人', visible: true },
    { key: 'staff.region', label: '区域', visible: true }
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
  time: useAnalyticsPageConfig({
    storageKey: 'analytics-sales-performance-page-config-time',
    defaultQueryFields: DEFAULT_QUERY_FIELDS_BY_TAB.time,
    defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
  }),
  staff: useAnalyticsPageConfig({
    storageKey: 'analytics-sales-performance-page-config-staff',
    defaultQueryFields: DEFAULT_QUERY_FIELDS_BY_TAB.staff,
    defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
  })
}

const activeConfig = computed(() => configByTab[activeTab.value as 'time' | 'staff'])
const queryFields = computed(() => activeConfig.value.queryFields.value)
const functionButtons = computed(() => activeConfig.value.functionButtons.value)
const defaultQueryFields = computed(() => DEFAULT_QUERY_FIELDS_BY_TAB[activeTab.value])
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
  pageCode: 'analytics-sales-performance',
  title: () => {
    const tabLabel = TABS.find(t => t.key === activeTab.value)?.label || '销售业绩'
    return `销售业绩 · ${tabLabel}（${dateRange.value?.[0]} ~ ${dateRange.value?.[1]}）`
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
    const res = await saleAnalysisReportApi.performance({ ...buildParams(), page: p, size })
    const list = res?.records || []
    all.push(...list)
    if (list.length < size) break
  }
  return all
}

function handleExport() {
  const cols = printableColumns.value
  executeExport({
    fileName: `销售业绩-${TABS.find(t => t.key === activeTab.value)?.label || ''}`,
    headers: cols.map(c => c.title),
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: (list: any[]) => list.map(r => cols.map(c => cellText(c, r))),
    fallbackRows: () => rows.value.map(r => cols.map(c => cellText(c, r)))
  })
}

function handleError(err: any) {
  console.error('[销售业绩] 页面异常', err)
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
.quick-dates { margin-left: 8px; }

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-empty { color: #bfbfbf; }
</style>
