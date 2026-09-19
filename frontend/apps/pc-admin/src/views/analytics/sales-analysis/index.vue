<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        销售分析（分析 → 采销分析 → 销售分析，菜单 80413）
        对标 ql361「销售分析」：8 个视图 Tab（按时间/按商品/按品牌/按客户/按区域/按仓库/按职员/按来源），
        逐 Tab 独立列配置（storage-key + global-config-key 同值、按 Tab 换 key）；
        量本利全链路列（数量/金额/退货/收入/成本/毛利/费用/优惠/费销比/利润/利润率），
        列名逐字取自《销售分析开发文档》§3 的 8 张列表，defaultHidden 个数 = 全部列数 − 默认列数。
        查询区：查询方案 + 时间快捷段 8 段 + 日期范围 + 商品/客户/经手人/仓库（横向网格）；
        Tab 专属：按时间→粒度（按天/周/月）、按品牌→区域/品牌/产生方式/显示销售数量大于0、按来源→销售类型。
        工具栏：图形（表格↔图表）｜刷新｜打印(F8)｜导出。
        本页对标无「页面配置」弹窗（已核 8 张列配置截图，无页面配置弹窗实据）→ 不接页面配置面板，查询区为固定项。
        取数：/erp/sale/analysis/sales-analysis/page?tab=<维度>（后端 SaleAnalysisReportController）。
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="onTabChange"
      >
        <!-- ═══ 工具栏左侧：查询方案 + 时间快捷段 ═══ -->
        <template #toolbar-left>
          <QuerySchemeBar
            storage-key="analytics-sales-analysis\index.vue-query-scheme"
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

        <!-- ═══ 工具栏右侧：图形｜刷新｜打印(F8)｜导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button size="small" :type="chartView ? 'primary' : 'default'" @click="chartView = !chartView">
              <BarChartOutlined /> 图形
            </a-button>
            <a-button size="small" :loading="loading" @click="fetchData">
              <ReloadOutlined /> 刷新
            </a-button>
            <a-button size="small" @click="handlePrint">
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button size="small" :loading="exporting" @click="handleExport">
              <DownloadOutlined /> 导出
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
              <div class="search-item">
                <span class="search-label">商品</span>
                <a-input
                  v-model:value="query.keyword"
                  size="small"
                  placeholder="商品名称/货号/条码"
                  allow-clear
                  style="width: 190px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">客户</span>
                <a-input
                  v-model:value="query.customerName"
                  size="small"
                  placeholder="客户名称"
                  allow-clear
                  style="width: 150px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">经手人</span>
                <a-input
                  v-model:value="query.salesmanName"
                  size="small"
                  placeholder="经手人"
                  allow-clear
                  style="width: 130px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">仓库</span>
                <a-input
                  v-model:value="query.warehouseName"
                  size="small"
                  placeholder="仓库"
                  allow-clear
                  style="width: 130px"
                  @press-enter="handleSearch"
                />
              </div>

              <!-- ── Tab 专属：按时间 → 粒度 ── -->
              <div v-if="activeTab === 'time'" class="search-item">
                <span class="search-label">粒度</span>
                <a-radio-group v-model:value="query.granularity" size="small" button-style="solid" @change="handleSearch">
                  <a-radio-button value="day">按天</a-radio-button>
                  <a-radio-button value="week">按周</a-radio-button>
                  <a-radio-button value="month">按月</a-radio-button>
                </a-radio-group>
              </div>

              <!-- ── Tab 专属：按品牌 → 区域/品牌/产生方式/显示销售数量大于0 ── -->
              <template v-if="activeTab === 'brand'">
                <div class="search-item">
                  <span class="search-label">区域</span>
                  <a-input v-model:value="query.region" size="small" placeholder="区域" allow-clear style="width: 120px" @press-enter="handleSearch" />
                </div>
                <div class="search-item">
                  <span class="search-label">品牌</span>
                  <a-input v-model:value="query.brand" size="small" placeholder="品牌" allow-clear style="width: 120px" @press-enter="handleSearch" />
                </div>
                <div class="search-item">
                  <span class="search-label">产生方式</span>
                  <a-input v-model:value="query.source" size="small" placeholder="全部" allow-clear style="width: 120px" @press-enter="handleSearch" />
                </div>
              </template>

              <!-- ── Tab 专属：按来源 → 销售类型 ── -->
              <div v-if="activeTab === 'source'" class="search-item">
                <span class="search-label">销售类型</span>
                <a-select
                  v-model:value="query.saleType"
                  size="small"
                  style="width: 120px"
                  :options="SALE_TYPE_OPTIONS"
                  allow-clear
                  placeholder="全部"
                  @change="handleSearch"
                />
              </div>

              <div class="search-item search-actions">
                <a-checkbox
                  v-if="activeTab === 'brand'"
                  v-model:checked="query.onlyPositiveQty"
                  @change="handleSearch"
                >
                  显示销售数量大于0
                </a-checkbox>
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格 / 图表（表头齿轮列配置，逐 Tab 一套） ═══ -->
        <template #table>
          <div class="table-area" :data-cols="colSummary">
            <ARReportChart
              v-if="chartView"
              :option="chartOption"
              title="销售额（当前页）"
              :loading="loading"
              :height="420"
            />
            <BillDetailTable
              v-else
              :data-source="rows"
              :columns="activeColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              :summary-columns="summaryColumns"
              row-key="dimKey"
              :storage-key="`analytics-sales-analysis-columns-${activeTab}`"
              :global-config-key="`analytics-sales-analysis-columns-${activeTab}`"
            >
              <template #actionCell="{ record }">
                <a-space :size="2">
                  <a-button type="link" size="small" @click="drillToDetail(record, '商品')">商品</a-button>
                  <a-button type="link" size="small" @click="drillToDetail(record, '品牌')">品牌</a-button>
                </a-space>
              </template>
              <template #sourceCell="{ record }">
                {{ record.dimLabel ?? record.dimKey }}
              </template>
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
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  BarChartOutlined, DownloadOutlined, PrinterOutlined, ReloadOutlined
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import ARReportChart from '@/components/ARReportChart/ARReportChart.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { saleAnalysisReportApi } from '@/api/analytics-sales'
import type { SaleAnalysisQuery } from '@/api/analytics-sales'
import { useExport } from '@/composables/useExport'
import { QUICK_DATES, quickDateRange } from '../shared/docTypes'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'

defineOptions({ name: 'AnalyticsSalesAnalysis' })

// ═══ 视图 Tab（顺序逐字取自对标实测：按时间/按商品/按品牌/按客户/按区域/按仓库/按职员/按来源） ═══
const TABS = [
  { key: 'time', label: '按时间' },
  { key: 'product', label: '按商品' },
  { key: 'brand', label: '按品牌' },
  { key: 'customer', label: '按客户' },
  { key: 'region', label: '按区域' },
  { key: 'warehouse', label: '按仓库' },
  { key: 'staff', label: '按职员' },
  { key: 'source', label: '按来源' }
]
const activeTab = ref('time')
const chartView = ref(false)
const quickDate = ref('month')

const SALE_TYPE_OPTIONS = [
  { label: '销售', value: 1 },
  { label: '退货', value: 2 },
  { label: '换货', value: 3 }
]

const dateRange = ref<[string, string]>(quickDateRange('month') as [string, string])
const query = reactive({
  scheme: '',
  keyword: '',
  customerName: '',
  salesmanName: '',
  warehouseName: '',
  region: '',
  brand: '',
  source: '',
  saleType: undefined as number | undefined,
  onlyPositiveQty: false,
  granularity: 'day' as 'day' | 'week' | 'month'
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
    keyword: query.keyword || undefined,
    customerName: query.customerName || undefined,
    salesmanName: query.salesmanName || undefined,
    warehouseName: query.warehouseName || undefined,
    region: query.region || undefined,
    brand: query.brand || undefined,
    source: query.source || undefined,
    saleType: query.saleType,
    onlyPositiveQty: activeTab.value === 'brand' ? query.onlyPositiveQty : undefined,
    granularity: activeTab.value === 'time' ? query.granularity : undefined,
    page: pagination.page,
    size: pagination.size
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await saleAnalysisReportApi.analysis(buildParams())
    rows.value = res?.records || []
    pagination.total = res?.total || 0
    summary.value = res?.summary || {}
  } catch (e) {
    console.warn('[销售分析] 取数失败', e)
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
    scheme: '',
    keyword: '',
    customerName: '',
    salesmanName: '',
    warehouseName: '',
    region: '',
    brand: '',
    source: '',
    saleType: undefined,
    onlyPositiveQty: false,
    granularity: 'day'
  })
  dateRange.value = quickDateRange('month') as [string, string]
  quickDate.value = 'month'
  handleSearch()
}

function setQuickDate(key: string) {
  quickDate.value = key
  dateRange.value = quickDateRange(key) as [string, string]
  handleSearch()
}

function onTabChange(key: string) {
  activeTab.value = key
  chartView.value = false
  pagination.page = 1
  fetchData()
}

/**
 * 行内「商品 / 品牌」钻取（对标：按品牌/按来源 的操作列）
 * 本系统无独立商品品牌钻取页，故按行维度切换视图并提示，不做无落点的假跳转。
 */
function drillToDetail(record: any, target: string) {
  if (activeTab.value !== 'brand') {
    activeTab.value = 'brand'
    query.brand = target === '品牌' ? String(record.dimLabel ?? '') : ''
    if (target === '商品') query.keyword = String(record.dimLabel ?? '')
    chartView.value = false
    pagination.page = 1
    fetchData()
  }
  message.info(`${target}钻取：已按「${record.dimLabel ?? record.dimKey}」切换视图`)
}

// ═══ 列定义辅助（列名逐字取自开发文档 §3；defaultHidden 个数 = 全部列数 − 默认列数） ═══
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

/** 可配置数据列（rowNo/操作/来源 等系统列另行构造，不计入对标列数） */
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

/** 无数据源列：仍占列位与列名，取值为空并显示 `-`（详见开发文档「缺口登记」） */
function gap(key: string, title: string, width = 110): DetailColumnConfig {
  return { key, title, width, align: 'right', type: 'slot', slotName: 'emptyCell', defaultHidden: true }
}

const ROW_NO: DetailColumnConfig = { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' }
const ACTION_COL: DetailColumnConfig = { key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 110, fixed: 'left' }
/**
 * 按来源视图的「来源」为对标固定列（不在 42 列 columnConfig 之内）。
 * BillDetailTable 的「不进列配置」判定按列 key 命中 LOCKED_COLUMNS(rowNo/checkbox/action)，
 * 故此处借 key='checkbox' 复用该锁定判定（渲染由 type:'slot' 决定，不会渲染成勾选框），
 * 使列配置弹窗口径与对标 42 列一致。
 */
const SOURCE_COL: DetailColumnConfig = { key: 'checkbox', title: '来源', type: 'slot', slotName: 'sourceCell', width: 110, align: 'left' }

/** 订货块（各 Tab 逐字一致，全默认隐藏） */
function orderBlock(): DetailColumnConfig[] {
  return [
    col('orderQty', '订货数量', 'num', 100, true),
    col('orderAmount', '订货金额', 'money', 110, true),
    col('orderReturnQty', '退订数量', 'num', 100, true),
    col('orderReturnAmount', '退订金额', 'money', 110, true),
    gap('directOutQty', '直接出库数量', 110),
    gap('directOutAmount', '直接出库金额', 110),
    gap('estSaleQty', '预估销售数量', 110),
    gap('estSaleAmount', '预估销售金额', 110),
    gap('orderFloatQty', '订货浮动数量', 110),
    gap('saleFloatQty', '销售浮动数量', 110)
  ]
}

/** 小/中/大单位数量分组（仅「小」有数据源：small_unit_quantity） */
function unitGroup(key: string, title: string, prefix: string): DetailColumnConfig {
  return {
    key,
    title,
    children: [
      col(`${prefix}Small`, '小', 'num', 90, true),
      gap(`${prefix}Medium`, '中', 90),
      gap(`${prefix}Large`, '大', 90)
    ]
  }
}

/** 利润区（其它费用支出 → 利润率，Tab1/4/5/6/7/8 逐字一致） */
function profitBlock(): DetailColumnConfig[] {
  return [
    col('otherFee', '其他费用支出', 'money', 120),
    col('settleDiscount', '结算优惠', 'money', 110),
    col('expenseRatio', '费销比(%)', 'rate', 100),
    col('saleProfit', '销售利润', 'money', 110),
    col('profitRatio', '利润率(%)', 'rate', 100)
  ]
}

/**
 * 按商品视图的订货块（对标 12-25 共 14 项，与其它 Tab 的订货块不同：
 * 不含订货浮动/销售浮动数量，且把销售客户数、客户数占比(%)、三档客单价、销售单数并入本段）
 */
function productOrderBlock(): DetailColumnConfig[] {
  return [
    col('orderQty', '订货数量', 'num', 100, true),
    col('orderAmount', '订货金额', 'money', 110, true),
    col('orderReturnQty', '退订数量', 'num', 100, true),
    col('orderReturnAmount', '退订金额', 'money', 110, true),
    gap('directOutQty', '直接出库数量', 110),
    gap('directOutAmount', '直接出库金额', 110),
    gap('estSaleQty', '预估销售数量', 110),
    gap('estSaleAmount', '预估销售金额', 110),
    col('saleCustomerCount', '销售客户数', 'num', 110, true),
    col('customerCountRatio', '客户数占比(%)', 'rate', 120, true),
    gap('bigUnitPrice', '大单位客单价', 110),
    gap('midUnitPrice', '中单位客单价', 110),
    gap('smallUnitPrice', '小单位客单价', 110),
    col('saleDocCount', '销售单数', 'num', 100, true)
  ]
}

// ── Tab1「按时间」：全部 50 / 默认 15 ──
const timeColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('dimLabel', '日期', 'text', 110),
  ...orderBlock(),
  col('saleCustomerCount', '销售客户数', 'num', 110, true),
  col('saleDocCount', '销售单数', 'num', 100, true),
  col('saleQty', '销售数量', 'num', 100),
  col('saleWeight', '销售重量', 'num', 100, true),
  unitGroup('saleUnitGroup', '销售小中大单位数量', 'sale'),
  col('saleAmount', '销售额', 'money', 110),
  col('saleAmountRatio', '销售额占比(%)', 'rate', 120),
  col('avgDealAmount', '平均交易额', 'money', 110, true),
  col('returnDocCount', '退货单数', 'num', 90, true),
  col('returnQty', '退货数量', 'num', 100),
  col('returnWeight', '退货重量', 'num', 100, true),
  unitGroup('returnUnitGroup', '退货小中大单位数量', 'return'),
  col('returnAmount', '退货额', 'money', 110),
  col('returnRate', '退货率(%)', 'rate', 100, true),
  col('netSaleAmount', '实销金额', 'money', 110, true),
  col('netSaleQty', '实销数量', 'num', 100, true),
  col('netSaleWeight', '实销重量', 'num', 100, true),
  unitGroup('netUnitGroup', '实销小中大单位数量', 'net'),
  col('revenue', '销售收入', 'money', 110),
  col('revenueRatio', '销售收入占比(%)', 'rate', 130),
  col('costAmount', '销售成本', 'money', 110),
  col('grossProfit', '销售毛利', 'money', 110),
  col('grossMargin', '毛利率(%)', 'rate', 100, true),
  ...profitBlock(),
  col('profitShare', '利润占比(%)', 'rate', 110, true),
  col('giftQty', '销售赠品数量', 'num', 110, true),
  gap('custom1', '自定义字段1', 110),
  gap('custom2', '自定义字段2', 110),
  gap('custom3', '自定义字段3', 110)
]

// ── Tab2「按商品」：全部 65 / 默认 14 ──
const productColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('rankNo', '排名', 'num', 60, true),
  col('dimLabel', '商品名称', 'text', 220),
  col('productCode', '货号', 'text', 110, true),
  // 图片列对标为缩略图；本系统取商品主数据 image_url 文本展示（不引入额外上传/预览组件）
  col('image', '图片', 'text', 80, true),
  col('brand', '品牌', 'text', 100, true),
  col('barcode', '条码', 'text', 130, true),
  col('spec', '规格', 'text', 110, true),
  col('model', '型号', 'text', 110, true),
  col('origin', '产地', 'text', 100, true),
  col('saleVolume', '销售体积', 'num', 100, true),
  col('saleWeight', '销售重量', 'num', 100, true),
  ...productOrderBlock(),
  col('saleQty', '销售数量', 'num', 100),
  col('conversionResult', '销售数量换算结果', 'text', 150),
  gap('saleBaseQty', '销售基本单位数量', 130),
  unitGroup('saleUnitGroup', '销售小中大单位数量', 'sale'),
  col('saleAmount', '销售额', 'money', 110),
  col('saleAmountRatio', '销售额占比(%)', 'rate', 120),
  col('returnDocCount', '退货单数', 'num', 90, true),
  col('returnQty', '退货数量', 'num', 100),
  gap('returnBaseQty', '退货基本单位数量', 130),
  col('returnConversionResult', '退货数量换算结果', 'text', 150),
  unitGroup('returnUnitGroup', '退货小中大单位数量', 'return'),
  col('returnAmount', '退货额', 'money', 110),
  col('amountReturnRate', '金额退货率(%)', 'rate', 110, true),
  col('qtyReturnRate', '数量退货率(%)', 'rate', 110),
  col('netSaleAmount', '实销金额', 'money', 110, true),
  col('netSaleQty', '实销数量', 'num', 100, true),
  gap('netBaseQty', '实销基本单位数量', 130),
  gap('netConversionResult', '实销数量换算结果', 150),
  unitGroup('netUnitGroup', '实销小中大单位数量', 'net'),
  col('revenue', '销售收入', 'money', 110),
  col('revenueRatio', '销售收入占比(%)', 'rate', 130),
  col('costAmount', '销售成本', 'money', 110),
  col('grossProfit', '销售毛利', 'money', 110),
  col('grossMargin', '毛利率(%)', 'rate', 100),
  col('grossShare', '毛利占比(%)', 'rate', 110, true),
  gap('feeContract', '费用合同兑付', 110),
  col('giftQty', '销售赠品数量', 'num', 110, true),
  gap('giftConversionResult', '销售赠品数量换算结果', 150),
  {
    key: 'giftUnitGroup',
    title: '销售赠品小中大数量',
    children: [gap('giftSmall', '小', 90), gap('giftMedium', '中', 90), gap('giftLarge', '大', 90)]
  },
  gap('custom1', '自定义字段1', 110),
  gap('custom2', '自定义字段2', 110),
  gap('custom3', '自定义字段3', 110)
]

// ── Tab3「按品牌」：全部 41 / 默认 11 ──
const brandColumns: DetailColumnConfig[] = [
  ROW_NO,
  ACTION_COL,
  col('dimLabel', '品牌名称', 'text', 180),
  col('orderQty', '订货数量', 'num', 100, true),
  col('orderAmount', '订货金额', 'money', 110, true),
  col('orderReturnQty', '退订数量', 'num', 100, true),
  col('orderReturnAmount', '退订金额', 'money', 110, true),
  gap('directOutQty', '直接出库数量', 110),
  gap('directOutAmount', '直接出库金额', 110),
  gap('estSaleQty', '预估销售数量', 110),
  gap('estSaleAmount', '预估销售金额', 110),
  col('saleCustomerCount', '销售客户数', 'num', 110, true),
  col('customerCountRatio', '客户数占比(%)', 'rate', 120, true),
  col('saleDocCount', '销售单数', 'num', 100, true),
  col('saleQty', '销售数量', 'num', 100),
  unitGroup('saleUnitGroup', '销售小中大单位数量', 'sale'),
  col('saleAmount', '销售额', 'money', 110),
  col('saleAmountRatio', '销售额占比(%)', 'rate', 120),
  col('returnDocCount', '退货单数', 'num', 90, true),
  col('returnQty', '退货数量', 'num', 100),
  unitGroup('returnUnitGroup', '退货小中大单位数量', 'return'),
  col('returnAmount', '退货额', 'money', 110),
  col('returnRate', '退货率(%)', 'rate', 100, true),
  col('netSaleAmount', '实销金额', 'money', 110, true),
  col('netSaleQty', '实销数量', 'num', 100, true),
  unitGroup('netUnitGroup', '实销小中大单位数量', 'net'),
  col('revenue', '销售收入', 'money', 110),
  col('revenueRatio', '销售收入占比(%)', 'rate', 130),
  col('costAmount', '销售成本', 'money', 110),
  col('grossProfit', '销售毛利', 'money', 110),
  col('grossMargin', '毛利率(%)', 'rate', 100),
  col('grossShare', '毛利占比(%)', 'rate', 110, true),
  gap('feeContract', '费用合同兑付', 110),
  col('giftQty', '销售赠品数量', 'num', 110, true),
  gap('custom1', '自定义字段1', 110),
  gap('custom2', '自定义字段2', 110),
  gap('custom3', '自定义字段3', 110)
]

// ── Tab4「按客户」：全部 53 / 默认 18 ──
const customerColumns: DetailColumnConfig[] = [
  ROW_NO,
  ACTION_COL,
  col('dimLabel', '客户名称', 'text', 200),
  col('customerCode', '客户编号', 'text', 110, true),
  col('defaultHandler', '默认经手人', 'text', 110, true),
  col('settlementType', '结款方式', 'text', 110, true),
  col('saleWeight', '销售重量', 'num', 100, true),
  col('saleVolume', '销售体积', 'num', 100, true),
  gap('saleFloatQty', '销售浮动数量', 110),
  col('orderQty', '订货数量', 'num', 100, true),
  col('orderAmount', '订货金额', 'money', 110, true),
  col('orderReturnQty', '退订数量', 'num', 100, true),
  col('orderReturnAmount', '退订金额', 'money', 110, true),
  gap('directOutQty', '直接出库数量', 110),
  gap('directOutAmount', '直接出库金额', 110),
  gap('estSaleQty', '预估销售数量', 110),
  gap('estSaleAmount', '预估销售金额', 110),
  col('saleDocCount', '销售单数', 'num', 100),
  col('saleQty', '销售数量', 'num', 100),
  unitGroup('saleUnitGroup', '销售小中大单位数量', 'sale'),
  col('saleAmount', '销售额', 'money', 110),
  col('saleAmountRatio', '销售额占比(%)', 'rate', 120),
  col('returnDocCount', '退货单数', 'num', 90),
  col('returnQty', '退货数量', 'num', 100),
  unitGroup('returnUnitGroup', '退货小中大单位数量', 'return'),
  col('returnAmount', '退货额', 'money', 110),
  col('returnRate', '退货率(%)', 'rate', 100, true),
  col('netSaleAmount', '实销金额', 'money', 110, true),
  col('netSaleQty', '实销数量', 'num', 100, true),
  unitGroup('netUnitGroup', '实销小中大单位数量', 'net'),
  col('revenue', '销售收入', 'money', 110),
  col('revenueRatio', '销售收入占比(%)', 'rate', 130),
  col('costAmount', '销售成本', 'money', 110),
  col('grossProfit', '销售毛利', 'money', 110),
  col('grossMargin', '毛利率(%)', 'rate', 100),
  col('grossShare', '毛利占比(%)', 'rate', 110, true),
  ...profitBlock(),
  col('profitShare', '利润占比(%)', 'rate', 110, true),
  col('unsettledAmount', '销售未结金额', 'money', 120, true),
  col('debtRatio', '欠款占比(%)', 'rate', 110, true),
  col('prepaidBalance', '预收余额', 'money', 110, true),
  col('giftQty', '销售赠品数量', 'num', 110, true),
  gap('custom1', '自定义字段1', 110),
  gap('custom2', '自定义字段2', 110),
  gap('custom3', '自定义字段3', 110)
]

// ── Tab5「按区域」：全部 50 / 默认 17 ──
const regionColumns: DetailColumnConfig[] = [
  ROW_NO,
  ACTION_COL,
  col('dimLabel', '区域名称', 'text', 160),
  gap('regionCode', '区域编号', 110),
  col('saleWeight', '销售重量', 'num', 100, true),
  col('saleVolume', '销售体积', 'num', 100, true),
  gap('saleFloatQty', '销售浮动数量', 110),
  col('orderQty', '订货数量', 'num', 100, true),
  col('orderAmount', '订货金额', 'money', 110, true),
  col('orderReturnQty', '退订数量', 'num', 100, true),
  col('orderReturnAmount', '退订金额', 'money', 110, true),
  gap('directOutQty', '直接出库数量', 110),
  gap('directOutAmount', '直接出库金额', 110),
  gap('estSaleQty', '预估销售数量', 110),
  gap('estSaleAmount', '预估销售金额', 110),
  col('saleCustomerCount', '销售客户数', 'num', 110, true),
  col('customerCountRatio', '客户数占比(%)', 'rate', 120, true),
  col('saleDocCount', '销售单数', 'num', 100, true),
  col('saleQty', '销售数量', 'num', 100),
  unitGroup('saleUnitGroup', '销售小中大单位数量', 'sale'),
  col('saleAmount', '销售额', 'money', 110),
  col('saleAmountRatio', '销售额占比(%)', 'rate', 120),
  col('returnDocCount', '退货单数', 'num', 90),
  col('returnQty', '退货数量', 'num', 100),
  unitGroup('returnUnitGroup', '退货小中大单位数量', 'return'),
  col('returnAmount', '退货额', 'money', 110),
  col('returnRate', '退货率(%)', 'rate', 100, true),
  col('netSaleAmount', '实销金额', 'money', 110, true),
  col('netSaleQty', '实销数量', 'num', 100, true),
  unitGroup('netUnitGroup', '实销小中大单位数量', 'net'),
  col('revenue', '销售收入', 'money', 110),
  col('revenueRatio', '销售收入占比(%)', 'rate', 130),
  col('costAmount', '销售成本', 'money', 110),
  col('grossProfit', '销售毛利', 'money', 110),
  col('grossMargin', '毛利率(%)', 'rate', 100),
  col('grossShare', '毛利占比(%)', 'rate', 110, true),
  ...profitBlock(),
  col('profitShare', '利润占比(%)', 'rate', 110, true),
  col('giftQty', '销售赠品数量', 'num', 110, true),
  gap('custom1', '自定义字段1', 110),
  gap('custom2', '自定义字段2', 110),
  gap('custom3', '自定义字段3', 110)
]

// ── Tab6「按仓库」：全部 49 / 默认 15 ──
const warehouseColumns: DetailColumnConfig[] = [
  ROW_NO,
  ACTION_COL,
  col('dimLabel', '仓库名称', 'text', 160),
  col('saleWeight', '销售重量', 'num', 100, true),
  col('saleVolume', '销售体积', 'num', 100, true),
  gap('saleFloatQty', '销售浮动数量', 110),
  col('orderQty', '订货数量', 'num', 100, true),
  col('orderAmount', '订货金额', 'money', 110, true),
  col('orderReturnQty', '退订数量', 'num', 100, true),
  col('orderReturnAmount', '退订金额', 'money', 110, true),
  gap('directOutQty', '直接出库数量', 110),
  gap('directOutAmount', '直接出库金额', 110),
  gap('estSaleQty', '预估销售数量', 110),
  gap('estSaleAmount', '预估销售金额', 110),
  col('saleCustomerCount', '销售客户数', 'num', 110, true),
  col('customerCountRatio', '客户数占比(%)', 'rate', 120, true),
  col('saleDocCount', '销售单数', 'num', 100, true),
  col('saleQty', '销售数量', 'num', 100),
  unitGroup('saleUnitGroup', '销售小中大单位数量', 'sale'),
  col('saleAmount', '销售额', 'money', 110),
  col('saleAmountRatio', '销售额占比(%)', 'rate', 120),
  col('returnDocCount', '退货单数', 'num', 90, true),
  col('returnQty', '退货数量', 'num', 100),
  unitGroup('returnUnitGroup', '退货小中大单位数量', 'return'),
  col('returnAmount', '退货额', 'money', 110),
  col('returnRate', '退货率(%)', 'rate', 100, true),
  col('netSaleAmount', '实销金额', 'money', 110, true),
  col('netSaleQty', '实销数量', 'num', 100, true),
  unitGroup('netUnitGroup', '实销小中大单位数量', 'net'),
  col('revenue', '销售收入', 'money', 110),
  col('revenueRatio', '销售收入占比(%)', 'rate', 130),
  col('costAmount', '销售成本', 'money', 110),
  col('grossProfit', '销售毛利', 'money', 110),
  col('grossMargin', '毛利率(%)', 'rate', 100),
  col('grossShare', '毛利占比(%)', 'rate', 110, true),
  gap('feeContract', '费用合同兑付', 110),
  // ⚠️ 按仓库视图无「结算优惠」列（对标 41-46 为 其他费用支出/费销比/销售利润/利润率/利润占比/销售赠品数量）
  col('otherFee', '其他费用支出', 'money', 120),
  col('expenseRatio', '费销比(%)', 'rate', 100),
  col('saleProfit', '销售利润', 'money', 110),
  col('profitRatio', '利润率(%)', 'rate', 100),
  col('profitShare', '利润占比(%)', 'rate', 110, true),
  col('giftQty', '销售赠品数量', 'num', 110, true),
  gap('custom1', '自定义字段1', 110),
  gap('custom2', '自定义字段2', 110),
  gap('custom3', '自定义字段3', 110)
]

// ── Tab7「按职员」：全部 53 / 默认 17（⚠️ 本 Tab 的 毛利率(%) 默认隐藏，勿套用其它 Tab 的默认集） ──
const staffColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('dimLabel', '职员名称', 'text', 130),
  gap('staffCode', '职员编号', 110),
  col('deptName', '所属部门', 'text', 120, true),
  gap('saleFloatQty', '销售浮动数量', 110),
  col('orderQty', '订货数量', 'num', 100, true),
  col('orderAmount', '订货金额', 'money', 110, true),
  col('orderReturnQty', '退订数量', 'num', 100, true),
  col('orderReturnAmount', '退订金额', 'money', 110, true),
  gap('directOutQty', '直接出库数量', 110),
  gap('directOutAmount', '直接出库金额', 110),
  gap('estSaleQty', '预估销售数量', 110),
  gap('estSaleAmount', '预估销售金额', 110),
  col('saleCustomerCount', '销售客户数', 'num', 110, true),
  col('customerCountRatio', '客户数占比(%)', 'rate', 120, true),
  col('saleDocCount', '销售单数', 'num', 100),
  col('saleQty', '销售数量', 'num', 100),
  col('saleWeight', '销售重量', 'num', 100, true),
  unitGroup('saleUnitGroup', '销售小中大单位数量', 'sale'),
  col('saleAmount', '销售额', 'money', 110),
  col('saleAmountRatio', '销售额占比(%)', 'rate', 120),
  col('returnDocCount', '退货单数', 'num', 90),
  col('returnQty', '退货数量', 'num', 100),
  col('returnWeight', '退货重量', 'num', 100, true),
  unitGroup('returnUnitGroup', '退货小中大单位数量', 'return'),
  col('returnAmount', '退货额', 'money', 110),
  col('returnRate', '退货率(%)', 'rate', 100, true),
  col('netSaleAmount', '实销金额', 'money', 110, true),
  col('netSaleQty', '实销数量', 'num', 100, true),
  col('netSaleWeight', '实销重量', 'num', 100, true),
  unitGroup('netUnitGroup', '实销小中大单位数量', 'net'),
  col('revenue', '销售收入', 'money', 110),
  col('revenueRatio', '销售收入占比(%)', 'rate', 130),
  col('costAmount', '销售成本', 'money', 110),
  col('grossProfit', '销售毛利', 'money', 110),
  col('grossMargin', '毛利率(%)', 'rate', 100, true),
  col('grossShare', '毛利占比(%)', 'rate', 110, true),
  gap('feeContract', '费用合同兑付', 110),
  ...profitBlock(),
  col('profitShare', '利润占比(%)', 'rate', 110, true),
  col('giftQty', '销售赠品数量', 'num', 110, true),
  gap('custom1', '自定义字段1', 110),
  gap('custom2', '自定义字段2', 110),
  gap('custom3', '自定义字段3', 110)
]

// ── Tab8「按来源」：全部 42 / 默认 11（来源列为对标固定列，不在 42 列之内） ──
const sourceColumns: DetailColumnConfig[] = [
  ROW_NO,
  ACTION_COL,
  SOURCE_COL,
  col('saleWeight', '销售重量', 'num', 100, true),
  col('saleVolume', '销售体积', 'num', 100, true),
  gap('saleFloatQty', '销售浮动数量', 110),
  col('orderQty', '订货数量', 'num', 100, true),
  col('orderAmount', '订货金额', 'money', 110, true),
  col('orderReturnQty', '退订数量', 'num', 100, true),
  col('orderReturnAmount', '退订金额', 'money', 110, true),
  gap('directOutQty', '直接出库数量', 110),
  gap('directOutAmount', '直接出库金额', 110),
  gap('estSaleQty', '预估销售数量', 110),
  gap('estSaleAmount', '预估销售金额', 110),
  col('saleCustomerCount', '销售客户数', 'num', 110, true),
  col('customerCountRatio', '客户数占比(%)', 'rate', 120, true),
  col('saleDocCount', '销售单数', 'num', 100, true),
  col('saleQty', '销售数量', 'num', 100),
  unitGroup('saleUnitGroup', '销售小中大单位数量', 'sale'),
  col('saleAmount', '销售额', 'money', 110),
  col('saleAmountRatio', '销售额占比(%)', 'rate', 120),
  col('returnDocCount', '退货单数', 'num', 90),
  col('returnQty', '退货数量', 'num', 100),
  unitGroup('returnUnitGroup', '退货小中大单位数量', 'return'),
  col('returnAmount', '退货额', 'money', 110),
  col('returnRate', '退货率(%)', 'rate', 100, true),
  col('netSaleAmount', '实销金额', 'money', 110, true),
  col('netSaleQty', '实销数量', 'num', 100, true),
  unitGroup('netUnitGroup', '实销小中大单位数量', 'net'),
  col('revenue', '销售收入', 'money', 110),
  col('revenueRatio', '销售收入占比(%)', 'rate', 130),
  col('costAmount', '销售成本', 'money', 110),
  col('grossProfit', '销售毛利', 'money', 110),
  col('grossMargin', '毛利率(%)', 'rate', 100),
  col('grossShare', '毛利占比(%)', 'rate', 110, true),
  col('giftQty', '销售赠品数量', 'num', 110, true),
  gap('custom1', '自定义字段1', 110),
  gap('custom2', '自定义字段2', 110),
  gap('custom3', '自定义字段3', 110)
]

const COLUMNS_BY_TAB: Record<string, DetailColumnConfig[]> = {
  time: timeColumns,
  product: productColumns,
  brand: brandColumns,
  customer: customerColumns,
  region: regionColumns,
  warehouse: warehouseColumns,
  staff: staffColumns,
  source: sourceColumns
}

const activeColumns = computed<DetailColumnConfig[]>(() => COLUMNS_BY_TAB[activeTab.value] || timeColumns)

/** 系统列（不参与对标列数口径）：序号 / 操作 / 来源固定列（来源列 key 复用 'checkbox'，见 SOURCE_COL 注释） */
const SYSTEM_COLUMN_KEYS = ['rowNo', 'action', 'checkbox']

const leafColumns = computed<DetailColumnConfig[]>(() =>
  activeColumns.value.flatMap(c => (c.children?.length ? c.children : [c]))
)
const configurableColumns = computed<DetailColumnConfig[]>(() =>
  leafColumns.value.filter(c => !SYSTEM_COLUMN_KEYS.includes(c.key))
)
/** `全部可配置列/默认显示列`（供真机校验对标 50/15、65/14 等口径） */
const colSummary = computed(() => {
  const total = configurableColumns.value.length
  const def = configurableColumns.value.filter(c => !c.defaultHidden).length
  return `${total}/${def}`
})

/** 底部合计行：取后端 summary（按当前过滤范围 SUM，非当前页求和） */
const SUMMARY_KEYS = ['saleQty', 'saleAmount', 'returnQty', 'returnAmount', 'revenue', 'costAmount',
  'grossProfit', 'otherFee', 'settleDiscount', 'saleProfit', 'orderQty', 'orderAmount']
const summaryColumns = computed(() => SUMMARY_KEYS
  .filter(k => configurableColumns.value.some(c => c.key === k))
  .map(k => ({ key: k, value: Number(summary.value[k]) || 0 })))

// ═══ 图形视图（表格 ↔ 图表，取当前页行） ═══
const chartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 70, right: 20, top: 40, bottom: 80 },
  xAxis: {
    type: 'category',
    data: rows.value.map(r => String(r.dimLabel ?? r.dimKey)),
    axisLabel: { rotate: 30, fontSize: 11 }
  },
  yAxis: { type: 'value', name: '销售额' },
  series: [{
    type: 'bar',
    name: '销售额',
    data: rows.value.map(r => Number(r.saleAmount) || 0),
    itemStyle: { color: '#1677ff' }
  }]
}))

// ═══ 打印(F8) / 导出（当前 Tab 的默认可见列，等价「所见即所打」） ═══
const printableColumns = computed<DetailColumnConfig[]>(() =>
  leafColumns.value.filter(c => !SYSTEM_COLUMN_KEYS.includes(c.key) && !c.defaultHidden)
)

function cellText(c: DetailColumnConfig, r: any): string {
  const raw = r[c.key]
  return c.formatter ? c.formatter(raw, r) : (raw === null || raw === undefined || raw === '' ? '-' : String(raw))
}

function handlePrint() {
  const cols = printableColumns.value
  const header = cols.map(c => c.title)
  const body = rows.value.map(r => cols.map(c => cellText(c, r)))
  const win = window.open('', '_blank', 'width=1400,height=800')
  if (!win) {
    message.warning('浏览器拦截了打印窗口，请允许弹窗后重试')
    return
  }
  const tabLabel = TABS.find(t => t.key === activeTab.value)?.label || '销售分析'
  const html = `<html><head><meta charset="utf-8"><title>销售分析-${tabLabel}</title>
    <style>body{font-family:system-ui,sans-serif;font-size:12px;padding:12px}
    h3{margin:0 0 8px}table{border-collapse:collapse;width:100%}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left;white-space:nowrap}</style></head><body>
    <h3>销售分析 · ${tabLabel}（${dateRange.value?.[0]} ~ ${dateRange.value?.[1]}）</h3>
    <table><thead><tr>${header.map(h => `<th>${h}</th>`).join('')}</tr></thead>
    <tbody>${body.map(row => `<tr>${row.map(v => `<td>${v}</td>`).join('')}</tr>`).join('')}</tbody>
    </table></body></html>`
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

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
    const res = await saleAnalysisReportApi.analysis({ ...buildParams(), page: p, size })
    const list = res?.records || []
    all.push(...list)
    if (list.length < size) break
  }
  return all
}

function handleExport() {
  const cols = printableColumns.value
  executeExport({
    fileName: `销售分析-${TABS.find(t => t.key === activeTab.value)?.label || ''}`,
    headers: cols.map(c => c.title),
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: (list: any[]) => list.map(r => cols.map(c => cellText(c, r))),
    fallbackRows: () => rows.value.map(r => cols.map(c => cellText(c, r)))
  })
}

function handleError(err: any) {
  console.error('[销售分析] 页面异常', err)
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
.cell-empty { color: #bfbfbf; }
</style>
