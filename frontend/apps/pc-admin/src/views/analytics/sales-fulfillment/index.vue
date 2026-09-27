<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        销售履约分析（分析 → 采销分析 → 销售履约分析，菜单 80414）
        对标 ql361「销售履约分析」：2 个视图 Tab（按单据 11/9、按客户 12/8），逐 Tab 独立列配置；
        列 = 发货履约率 + 订单价税合计/发货/未发/已结/未结五金额口径，列名逐字取自《销售履约分析开发文档》§3
        （⚠️ 按单据为「发货履约率（%）」全角括号、按客户为「发货履约率(%)」半角括号，取自对标原样，勿统一）；
        查询区：查询方案 + 时间快捷段（本页含「近90天」、无「近一周/近三月/本年」）+ 日期范围 +
        客户/经手人/单据状态/部门/仓库/产生方式/区域（横向网格）；
        左侧客户分类树（对标实测：全部客户/系统用户/餐饮客户/合作商）；
        工具栏：刷新｜打印(F8)｜导出｜页面配置（对标有「按单据/按客户-页面配置弹窗」实据 → 接 PageConfigPanel）。
        取数：/erp/sale/analysis/sales-fulfillment/page?tab=doc|customer。
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="true"
        category-title="客户分类"
        :category-tree-data="categoryTree"
        :category-loading="categoryLoading"
        :category-editable="false"
        :selected-category-id="selectedCategoryId"
        :current-path="currentPath"
        :show-table-footer="true"
        @tab-change="onTabChange"
        @category-select="onCategorySelect"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 时间快捷段（对标本页集合特殊：含近90天） ═══ -->
        <template #toolbar-left>
          <QuerySchemeBar
            storage-key="analytics-sales-fulfillment\index.vue-query-scheme"
            :snapshot="querySnapshot"
            @apply="applyQuerySnapshot"
          />
          <a-space :size="4" class="quick-dates">
            <a-button
              v-for="d in QUICK_DATES_FULFILLMENT"
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

        <!-- ═══ 查询区（对标 10 项，横向自适应网格） ═══ -->
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
              <div v-if="isQueryVisible(`${activeTab}.salesmanName`)" class="search-item">
                <span class="search-label">经手人</span>
                <a-input v-model:value="query.salesmanName" size="small" placeholder="经手人" allow-clear style="width: 130px" @press-enter="handleSearch" />
              </div>
              <div v-if="isQueryVisible(`${activeTab}.docStatus`)" class="search-item">
                <span class="search-label">单据状态</span>
                <a-select
                  v-model:value="query.docStatus"
                  size="small"
                  mode="multiple"
                  :max-tag-count="2"
                  placeholder="审核中,待付款,待发货…"
                  style="width: 220px"
                  :options="DOC_STATUS_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible(`${activeTab}.deptName`)" class="search-item">
                <span class="search-label">部门</span>
                <a-input v-model:value="query.deptName" size="small" placeholder="部门" allow-clear style="width: 130px" @press-enter="handleSearch" />
              </div>
              <div v-if="isQueryVisible(`${activeTab}.warehouseName`)" class="search-item">
                <span class="search-label">仓库</span>
                <a-input v-model:value="query.warehouseName" size="small" placeholder="仓库" allow-clear style="width: 130px" @press-enter="handleSearch" />
              </div>
              <div v-if="isQueryVisible(`${activeTab}.source`)" class="search-item">
                <span class="search-label">产生方式</span>
                <a-input v-model:value="query.source" size="small" placeholder="全部" allow-clear style="width: 120px" @press-enter="handleSearch" />
              </div>
              <div v-if="isQueryVisible(`${activeTab}.region`)" class="search-item">
                <span class="search-label">区域</span>
                <a-input v-model:value="query.region" size="small" placeholder="区域" allow-clear style="width: 120px" @press-enter="handleSearch" />
              </div>
              <div class="search-item search-actions">
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
              :row-key="activeTab === 'doc' ? 'orderId' : 'customerKey'"
              :storage-key="`analytics-sales-fulfillment-columns-${activeTab}`"
              :global-config-key="`analytics-sales-fulfillment-columns-${activeTab}`"
            >
              <template #docNoCell="{ record }">
                <a class="doc-link" @click="openOrderForm(record)">{{ record.docNo }}</a>
              </template>
              <template #statusCell="{ record }">
                <a-tag :color="statusColor(record.orderStatusText)">{{ record.orderStatusText }}</a-tag>
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

      <!-- ═══ 页面配置（对标有「按单据/按客户-页面配置弹窗」实据 → 逐 Tab 独立 storage-key） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFields"
        :function-buttons-config="functionButtons"
        :default-query-fields-config="defaultQueryFields"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="`analytics-sales-fulfillment-page-config-${activeTab}`"
        :print-config-items="PRINT_ITEMS"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>

    <!-- 打印：结果集打印 -->
    <PrintDialog ref="printDialogRef" page-code="analytics-sales-fulfillment" :print-data="printData" />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
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
import { partnerCategoryApi } from '@/api/erp/partner'
import { useExport } from '@/composables/useExport'
import { quickDateRange } from '../shared/docTypes'
import { openDocForm } from '../shared/docActions'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'AnalyticsSalesFulfillment' })

const router = useRouter()

// ═══ 视图 Tab（对标实测顺序：按单据 / 按客户） ═══
const TABS = [
  { key: 'doc', label: '按单据' },
  { key: 'customer', label: '按客户' }
]
const activeTab = ref('doc')
/** 默认选中「本月」（对标截图高亮为「本周」；本页数据量小，默认本月可避免首屏空表） */
const quickDate = ref('month')

/** 本页时间快捷段集合与其它分析页不同：无「近一周/近三月/本年」，含「近90天」（对标实测） */
const QUICK_DATES_FULFILLMENT = [
  { key: 'yesterday', label: '昨日' },
  { key: 'today', label: '今日' },
  { key: 'week', label: '本周' },
  { key: 'month', label: '本月' },
  { key: 'lastMonth', label: '上月' },
  { key: 'last90', label: '近90天' }
]

const DOC_STATUS_OPTIONS = [
  { label: '审核中', value: 1 },
  { label: '待付款', value: 2 },
  { label: '待发货', value: 3 },
  { label: '发货完成', value: 4 },
  { label: '交易完成', value: 5 }
]

/**
 * 时间快捷段 → 日期区间。
 * 「近90天」为对标本页专属段（共享件 quickDateRange 无此键），故在页面内补齐，其余键仍走共享件。
 */
function quickRange(key: string): [string, string] {
  if (key !== 'last90') return quickDateRange(key) as [string, string]
  const pad = (n: number) => String(n).padStart(2, '0')
  const fmt = (d: Date) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
  const end = new Date()
  const start = new Date(end.getFullYear(), end.getMonth(), end.getDate() - 89)
  return [fmt(start), fmt(end)]
}

const dateRange = ref<[string, string]>(quickDateRange('month') as [string, string])
const query = reactive({
  scheme: '',
  customerName: '',
  salesmanName: '',
  deptName: '',
  warehouseName: '',
  source: '',
  region: '',
  docStatus: [] as number[],
  categoryIds: '' as string
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
    deptName: query.deptName || undefined,
    warehouseName: query.warehouseName || undefined,
    source: query.source || undefined,
    region: query.region || undefined,
    docStatus: query.docStatus.length ? query.docStatus.join(',') : undefined,
    customerCategoryIds: query.categoryIds || undefined,
    page: pagination.page,
    size: pagination.size
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await saleAnalysisReportApi.fulfillment(buildParams())
    rows.value = res?.records || []
    pagination.total = res?.total || 0
    summary.value = res?.summary || {}
  } catch (e) {
    console.warn('[销售履约分析] 取数失败', e)
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
  Object.assign(query, {
    scheme: '', customerName: '', salesmanName: '', deptName: '', warehouseName: '',
    source: '', region: '', docStatus: [], categoryIds: ''
  })
  dateRange.value = quickDateRange('month') as [string, string]
  quickDate.value = 'month'
  selectedCategoryId.value = '0'
  currentPath.value = '全部客户'
  handleSearch()
}

function setQuickDate(key: string) {
  quickDate.value = key
  dateRange.value = quickRange(key)
  handleSearch()
}

function onTabChange(key: string) {
  activeTab.value = key
  pagination.page = 1
  fetchData()
}

/** 单据编号链接：跳销售订单表单（对标：单据编号为蓝色链接） */
function openOrderForm(record: any) {
  openDocForm(router, { docTypeCode: 'SALE_ORDER', docId: record.orderId } as any)
}

function statusColor(text: string): string {
  if (text === '交易完成') return 'success'
  if (text === '发货完成') return 'orange'
  if (text === '待发货') return 'processing'
  if (text === '待付款') return 'warning'
  if (text === '已取消') return 'default'
  return 'processing'
}

// ═══ 左侧客户分类树（对标实测：全部客户/系统用户/餐饮客户/合作商） ═══
const categoryTree = ref<any[]>([])
const categoryLoading = ref(false)
const selectedCategoryId = ref<string | number>('0')
const currentPath = ref('全部客户')

async function fetchCategoryTree() {
  categoryLoading.value = true
  try {
    const res: any = await partnerCategoryApi.getTree('CUSTOMER')
    const list = Array.isArray(res) ? res : res?.data || []
    categoryTree.value = mapCategoryTree(list)
  } catch (e) {
    console.warn('[销售履约分析] 客户分类树获取失败', e)
    categoryTree.value = []
  } finally {
    categoryLoading.value = false
  }
}

/** 客户分类树 → CategoryListLayout 期望的 {id, categoryName, children} 结构 */
function mapCategoryTree(list: any[]): any[] {
  return (list || []).map(c => ({
    id: c.id,
    categoryName: c.categoryName,
    children: c.children?.length ? mapCategoryTree(c.children) : undefined
  }))
}

/** 收集所选分类及其全部下级ID（后端按 IN 过滤，父节点自然覆盖子节点） */
function collectCategoryIds(nodes: any[], id: string, trail: string[] = []): string[] | null {
  for (const n of nodes || []) {
    const next = [...trail, String(n.id)]
    if (String(n.id) === id) return next
    const found = n.children?.length ? collectCategoryIds(n.children, id, next) : null
    if (found) return found
  }
  return null
}

function onCategorySelect(keys: (string | number)[]) {
  const key = keys?.length ? String(keys[0]) : ''
  selectedCategoryId.value = key || '0'
  const ids = key ? collectCategoryIds(categoryTree.value, key) : null
  query.categoryIds = ids?.length ? ids.join(',') : ''
  currentPath.value = key ? '所选分类' : '全部客户'
  handleSearch()
}

// ═══ 列定义（列名逐字取自《销售履约分析开发文档》§3，⚠️ 两 Tab 括号形态不同） ═══
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

const ROW_NO: DetailColumnConfig = { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' }

// ── Tab1「按单据」：全部 11 / 默认 9 ──
const docColumns: DetailColumnConfig[] = [
  ROW_NO,
  { key: 'docNo', title: '单据编号', type: 'slot', slotName: 'docNoCell', width: 170, align: 'left' },
  col('orderQty', '订货数量', 'num', 100),
  { key: 'orderStatusText', title: '订单状态', type: 'slot', slotName: 'statusCell', width: 110, align: 'center' },
  col('fulfillmentRate', '发货履约率（%）', 'rate', 140),
  col('taxAmount', '订单价税合计', 'money', 130),
  col('shippedQty', '发货数量', 'num', 100, true),
  col('shippedAmount', '发货金额', 'money', 110),
  col('unshippedQty', '未发数量', 'num', 100, true),
  col('unshippedAmount', '未发金额', 'money', 110),
  col('settledAmount', '已结金额', 'money', 110),
  col('unsettledAmount', '未结金额', 'money', 110)
]

// ── Tab2「按客户」：全部 12 / 默认 8 ──
const customerColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('customerCode', '客户编号', 'text', 110, true),
  col('customerName', '客户名称', 'text', 200),
  col('settlementMethod', '结算方式', 'text', 110, true),
  col('orderDocCount', '订单数', 'num', 90),
  col('fulfillmentRate', '发货履约率(%)', 'rate', 130),
  col('taxAmount', '订单价税合计', 'money', 130),
  col('shippedQty', '发货数量', 'num', 100, true),
  col('shippedAmount', '发货金额', 'money', 110),
  col('unshippedQty', '未发数量', 'num', 100, true),
  col('unshippedAmount', '未发金额', 'money', 110),
  col('settledAmount', '已结金额', 'money', 110),
  col('unsettledAmount', '未结金额', 'money', 110)
]

const COLUMNS_BY_TAB: Record<string, DetailColumnConfig[]> = { doc: docColumns, customer: customerColumns }
const activeColumns = computed<DetailColumnConfig[]>(() => COLUMNS_BY_TAB[activeTab.value] || docColumns)
const configurableColumns = computed<DetailColumnConfig[]>(() =>
  activeColumns.value.filter(c => c.key !== 'rowNo')
)
/** `全部可配置列/默认显示列`（供真机校验对标 11/9、12/8） */
const colSummary = computed(() => {
  const total = configurableColumns.value.length
  const def = configurableColumns.value.filter(c => !c.defaultHidden).length
  return `${total}/${def}`
})

const SUMMARY_KEYS = ['orderDocCount', 'orderQty', 'taxAmount', 'shippedQty', 'shippedAmount',
  'unshippedAmount', 'settledAmount', 'unsettledAmount']
const summaryColumns = computed(() => SUMMARY_KEYS
  .filter(k => configurableColumns.value.some(c => c.key === k))
  .map(k => ({ key: k, value: Number(summary.value[k]) || 0 })))

// ═══ 页面配置（逐 Tab 一套查询条件清单与 storage-key） ═══
const QUERY_FIELDS_DOC: QueryFieldSetting[] = [
  { key: 'doc.customerName', label: '客户', visible: true },
  { key: 'doc.salesmanName', label: '经手人', visible: true },
  { key: 'doc.docStatus', label: '单据状态', visible: true },
  { key: 'doc.deptName', label: '部门', visible: true },
  { key: 'doc.warehouseName', label: '仓库', visible: true },
  { key: 'doc.source', label: '产生方式', visible: true },
  { key: 'doc.region', label: '区域', visible: true }
]
const QUERY_FIELDS_CUSTOMER: QueryFieldSetting[] = [
  { key: 'customer.customerName', label: '客户', visible: true },
  { key: 'customer.salesmanName', label: '经手人', visible: true },
  { key: 'customer.docStatus', label: '单据状态', visible: true },
  { key: 'customer.deptName', label: '部门', visible: true },
  { key: 'customer.warehouseName', label: '仓库', visible: true },
  { key: 'customer.source', label: '产生方式', visible: true },
  { key: 'customer.region', label: '区域', visible: true }
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

const configByTab = {
  doc: useAnalyticsPageConfig({
    storageKey: 'analytics-sales-fulfillment-page-config-doc',
    defaultQueryFields: QUERY_FIELDS_DOC,
    defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
  }),
  customer: useAnalyticsPageConfig({
    storageKey: 'analytics-sales-fulfillment-page-config-customer',
    defaultQueryFields: QUERY_FIELDS_CUSTOMER,
    defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
  })
}

const activeConfig = computed(() => configByTab[activeTab.value as 'doc' | 'customer'])
const queryFields = computed(() => activeConfig.value.queryFields.value)
const functionButtons = computed(() => activeConfig.value.functionButtons.value)
const defaultQueryFields = computed(() => (activeTab.value === 'doc' ? QUERY_FIELDS_DOC : QUERY_FIELDS_CUSTOMER))
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
  activeColumns.value.filter(c => c.key !== 'rowNo' && !c.defaultHidden)
)

function cellText(c: DetailColumnConfig, r: any): string {
  const raw = r[c.key]
  return c.formatter ? c.formatter(raw, r) : (raw === null || raw === undefined || raw === '' ? '-' : String(raw))
}

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML 再调浏览器打印，现在交给 PrintDialog：
// 列随 Tab 变化（computed），按当前可见列打，故 useDataColumns。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'analytics-sales-fulfillment',
  title: () => {
    const tabLabel = TABS.find(t => t.key === activeTab.value)?.label || '销售履约分析'
    return `销售履约分析 · ${tabLabel}（${dateRange.value?.[0]} ~ ${dateRange.value?.[1]}）`
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
    const res = await saleAnalysisReportApi.fulfillment({ ...buildParams(), page: p, size })
    const list = res?.records || []
    all.push(...list)
    if (list.length < size) break
  }
  return all
}

function handleExport() {
  const cols = printableColumns.value
  executeExport({
    fileName: `销售履约分析-${TABS.find(t => t.key === activeTab.value)?.label || ''}`,
    headers: cols.map(c => c.title),
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: (list: any[]) => list.map(r => cols.map(c => cellText(c, r))),
    fallbackRows: () => rows.value.map(r => cols.map(c => cellText(c, r)))
  })
}

function handleError(err: any) {
  console.error('[销售履约分析] 页面异常', err)
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
.doc-link { color: #1677ff; }
.doc-link:hover { text-decoration: underline; }
</style>
