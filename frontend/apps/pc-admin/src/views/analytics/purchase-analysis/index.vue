<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        采购分析（分析 → 采销分析 → 采购分析，菜单 80421）
        对标 ql361「采购分析」：3 个视图 Tab（按时间 / 按商品 28 列·默认 11 / 按供应商 17 列·默认 11），
        逐 Tab 独立列配置；「按时间」为多级表头（日期 + 采订统计 / 采购统计 / 退货统计 三组 + 实采金额 / 退货率）+ 粒度段（按天/周/月）。
        查询区：查询方案 + 8 段时间快捷段 + 日期范围 + 供应商/商品/经手人/仓库/品牌/部门（横向网格）；
        工具栏 刷新｜打印(F8)｜导出｜页面配置。
        取数：/erp/purchase/analytics/page?tab=time|product|supplier（采购订单 + 采购入库单 + 采购退货单三段量额）。
        ⚠️ 无数据源列（后端返回 null、本页显示 -，不填 0 冒充）：
           浮动单位 / 采订浮动数量 / 采购浮动数量（无浮动单位主数据与浮动数量列）、
           采购入库\采购费用分摊（费用分摊只有整单级金额，无行级分摊口径）。
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
          <!-- 查询方案（对标 `--查询方案--` 下拉 + 保存，落本机 localStorage） -->
          <QuerySchemeBar
            storage-key="analytics-purchase-analysis-query-scheme"
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
            <a-button v-if="isButtonEnabled('refresh')" size="small" :loading="loading" @click="handleRefresh">
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

        <!-- ═══ 查询区（对标 6 项业务筛选 + 查询按钮，横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div v-if="isQueryVisible('purchase.dateRange')" class="search-item">
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
              <div v-if="isQueryVisible('purchase.supplierName')" class="search-item">
                <span class="search-label">供应商</span>
                <a-input
                  v-model:value="query.supplierName"
                  size="small"
                  placeholder="供应商名称"
                  allow-clear
                  style="width: 160px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('purchase.keyword')" class="search-item">
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
              <div v-if="isQueryVisible('purchase.handlerName')" class="search-item">
                <span class="search-label">经手人</span>
                <a-input
                  v-model:value="query.handlerName"
                  size="small"
                  placeholder="经手人"
                  allow-clear
                  style="width: 130px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('purchase.warehouseName')" class="search-item">
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
              <div v-if="isQueryVisible('purchase.brand')" class="search-item">
                <span class="search-label">品牌</span>
                <a-input
                  v-model:value="query.brand"
                  size="small"
                  placeholder="品牌"
                  allow-clear
                  style="width: 120px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('purchase.deptName')" class="search-item">
                <span class="search-label">部门</span>
                <a-input
                  v-model:value="query.deptName"
                  size="small"
                  placeholder="部门"
                  allow-clear
                  style="width: 120px"
                  @press-enter="handleSearch"
                />
              </div>
              <!-- Tab 专属：按时间 → 粒度（对标「按天/按周/按月」） -->
              <div v-if="activeTab === 'time'" class="search-item">
                <span class="search-label">粒度</span>
                <a-radio-group v-model:value="query.granularity" size="small" button-style="solid" @change="handleSearch">
                  <a-radio-button value="day">按天</a-radio-button>
                  <a-radio-button value="week">按周</a-radio-button>
                  <a-radio-button value="month">按月</a-radio-button>
                </a-radio-group>
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
              row-key="dimKey"
              :storage-key="`analytics-purchase-analysis-columns-${activeTab}`"
              :global-config-key="`analytics-purchase-analysis-columns-${activeTab}`"
            >
              <template #imageCell="{ record }">
                <span v-if="record.image" class="cell-image">{{ record.image }}</span>
                <span v-else class="cell-empty">-</span>
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
import { purchaseAnalysisApi } from '@/api/analytics-supply'
import { useExport } from '@/composables/useExport'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'
import { QUICK_DATES, quickDateRange } from '../shared/docTypes'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'

defineOptions({ name: 'AnalyticsPurchaseAnalysis' })

// ═══ 视图 Tab（顺序逐字取自对标实测：按时间 / 按商品 / 按供应商） ═══
const TABS = [
  { key: 'time', label: '按时间' },
  { key: 'product', label: '按商品' },
  { key: 'supplier', label: '按供应商' }
]
const activeTab = ref('time')

const quickDate = ref('month')
const dateRange = ref<[string, string]>(quickDateRange('month') as [string, string])
const query = reactive({
  supplierName: '',
  keyword: '',
  handlerName: '',
  warehouseName: '',
  brand: '',
  deptName: '',
  granularity: 'day' as 'day' | 'week' | 'month'
})

const loading = ref(false)
const rows = ref<any[]>([])
const pagination = reactive({ page: 1, size: 20, total: 0 })
const summary = ref<Record<string, any>>({})

// ═══ 数值格式化 ═══
function fmtNum(v: any): string {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '-'
  return Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 4 })
}

function fmtMoney(v: any): string {
  if (v === null || v === undefined || v === '' || isNaN(Number(v))) return '-'
  return Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

type Kind = 'money' | 'num' | 'rate' | 'text'

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

/** 无数据源列：仍占列位与列名，取值为空并显示 `-` */
function gap(key: string, title: string, width = 110): DetailColumnConfig {
  return { key, title, width, align: 'right', type: 'slot', slotName: 'emptyCell', defaultHidden: true }
}

const ROW_NO: DetailColumnConfig = { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' }

// ── Tab1「按时间」：日期 + 采订统计 / 采购统计 / 退货统计 三组 + 实采金额 / 退货率（多级表头，仅一层） ──
const timeColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('dimLabel', '日期', 'text', 110),
  {
    key: 'orderGroup',
    title: '采订统计',
    children: [
      col('orderDocCount', '采订单数', 'num', 100),
      col('orderQty', '采订数量', 'num', 100),
      col('orderAmount', '订单优惠后金额', 'money', 130)
    ]
  },
  {
    key: 'purchaseGroup',
    title: '采购统计',
    children: [
      col('purchaseDocCount', '采购单数', 'num', 100),
      col('purchaseQty', '采购数量', 'num', 100),
      col('purchaseAmount', '采购优惠后金额', 'money', 130)
    ]
  },
  {
    key: 'returnGroup',
    title: '退货统计',
    children: [
      col('returnDocCount', '退货单数', 'num', 100),
      col('returnQty', '退货数量', 'num', 100),
      col('returnAmount', '采购退货折后金额', 'money', 140)
    ]
  },
  col('netAmount', '实采金额', 'money', 120),
  col('returnRate', '退货率(%)', 'rate', 100)
]

// ── Tab2「按商品」：全部 28 列 / 默认 11 ──
const productColumns: DetailColumnConfig[] = [
  ROW_NO,
  { key: 'image', title: '图片', type: 'slot', slotName: 'imageCell', width: 80, align: 'center', defaultHidden: true },
  col('dimLabel', '商品名称', 'text', 200),
  col('productCode', '货号', 'text', 110, true),
  col('brand', '品牌', 'text', 100, true),
  col('barcode', '条码', 'text', 130, true),
  col('spec', '规格', 'text', 120, true),
  col('model', '型号', 'text', 110, true),
  col('origin', '产地', 'text', 100, true),
  gap('floatUnit', '浮动单位'),
  gap('orderFloatQty', '采订浮动数量'),
  gap('purchaseFloatQty', '采购浮动数量'),
  col('orderDocCount', '采订单数', 'num', 100),
  col('orderQty', '采订数量', 'num', 100, true),
  col('orderSmallQty', '采订小单位数量', 'num', 120, true),
  col('orderAmount', '订单优惠后金额', 'money', 130),
  col('purchaseDocCount', '采购单数', 'num', 100),
  col('purchaseQty', '采购数量', 'num', 100, true),
  col('purchaseSmallQty', '采购小单位数量', 'num', 120, true),
  col('purchaseAmount', '采购优惠后金额', 'money', 130),
  col('returnDocCount', '退货单数', 'num', 90),
  col('returnQty', '退货数量', 'num', 100),
  col('returnSmallQty', '退货小单位数量', 'num', 120, true),
  col('returnAmount', '采购退货折后金额', 'money', 140),
  col('netQty', '实采数量', 'num', 100, true),
  col('netSmallQty', '实采小单位数量', 'num', 120, true),
  col('netAmount', '实采金额', 'money', 110),
  col('returnRate', '退货率(%)', 'rate', 100),
  col('giftQty', '赠品数量', 'num', 100)
]

// ── Tab3「按供应商」：全部 17 列 / 默认 11 ──
const supplierColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('supplierCode', '供应商编号', 'text', 120, true),
  col('dimLabel', '供应商名称', 'text', 220),
  col('orderDocCount', '采订单数', 'num', 100),
  col('orderQty', '采订数量', 'num', 100, true),
  col('orderSmallQty', '采订小单位数量', 'num', 120, true),
  col('orderAmount', '订单优惠后金额', 'money', 130),
  col('purchaseDocCount', '采购单数', 'num', 100),
  col('purchaseQty', '采购数量', 'num', 100, true),
  col('purchaseSmallQty', '采购小单位数量', 'num', 120, true),
  col('purchaseAmount', '采购优惠后金额', 'money', 130),
  col('returnDocCount', '退货单数', 'num', 90),
  col('returnQty', '退货数量', 'num', 100),
  col('returnSmallQty', '退货小单位数量', 'num', 120, true),
  col('returnAmount', '采购退货折后金额', 'money', 140),
  col('netAmount', '实采金额', 'money', 110),
  col('returnRate', '退货率(%)', 'rate', 100),
  col('giftQty', '赠品数量', 'num', 100)
]

const COLUMNS_BY_TAB: Record<string, DetailColumnConfig[]> = {
  time: timeColumns,
  product: productColumns,
  supplier: supplierColumns
}

const activeColumns = computed<DetailColumnConfig[]>(() => COLUMNS_BY_TAB[activeTab.value] || timeColumns)

const leafColumns = computed<DetailColumnConfig[]>(() =>
  activeColumns.value.flatMap(c => (c.children?.length ? c.children : [c])))

/** `全部可配置列/默认显示列`（供真机校验对标 28/11、17/11 口径） */
const colSummary = computed(() => {
  const leaf = leafColumns.value.filter(c => c.type !== 'rowNo')
  return `${leaf.length}/${leaf.filter(c => !c.defaultHidden).length}`
})

/** 底部合计行（后端按当前过滤范围 SUM） */
const SUMMARY_KEYS = ['orderDocCount', 'orderQty', 'orderAmount', 'purchaseDocCount',
  'purchaseQty', 'purchaseAmount', 'returnDocCount', 'returnQty', 'returnAmount',
  'netAmount', 'netQty', 'giftQty', 'orderSmallQty', 'purchaseSmallQty', 'returnSmallQty']
const summaryColumns = computed(() => SUMMARY_KEYS
  .filter(k => leafColumns.value.some(c => c.key === k))
  .map(k => ({ key: k, value: Number(summary.value[k]) || 0 })))

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'purchase.dateRange', label: '日期', visible: true },
  { key: 'purchase.supplierName', label: '供应商', visible: true },
  { key: 'purchase.keyword', label: '商品', visible: true },
  { key: 'purchase.handlerName', label: '经手人', visible: true },
  { key: 'purchase.warehouseName', label: '仓库', visible: true },
  { key: 'purchase.brand', label: '品牌', visible: true },
  { key: 'purchase.deptName', label: '部门', visible: true }
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
  storageKey: 'analytics-purchase-analysis-page-config',
  defaultQueryFields: DEFAULT_QUERY_FIELDS,
  defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
})

// ═══ 查询方案（本机 localStorage，方案内容 = 当前查询条件快照） ═══
function schemeSnapshot(): Record<string, any> {
  return { dateRange: [...(dateRange.value || [])], quickDate: quickDate.value, ...query }
}

/** 回填方案：日期区间与快捷段一并还原（方案里存的就是这两者 + 业务筛选） */
function applyScheme(v: Record<string, any>) {
  if (Array.isArray(v.dateRange) && v.dateRange.length === 2) {
    dateRange.value = [String(v.dateRange[0]), String(v.dateRange[1])]
  }
  if (typeof v.quickDate === 'string') quickDate.value = v.quickDate
  Object.keys(query).forEach(k => {
    if (k in v) (query as any)[k] = v[k]
  })
  handleSearch()
}

// ═══ 取数 ═══
async function fetchData() {
  loading.value = true
  try {
    const res = await purchaseAnalysisApi.page({
      tab: activeTab.value,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      supplierName: query.supplierName || undefined,
      keyword: query.keyword || undefined,
      handlerName: query.handlerName || undefined,
      warehouseName: query.warehouseName || undefined,
      brand: query.brand || undefined,
      deptName: query.deptName || undefined,
      granularity: activeTab.value === 'time' ? query.granularity : undefined,
      page: pagination.page,
      size: pagination.size
    })
    rows.value = res?.records || []
    pagination.total = Number(res?.total) || 0
    summary.value = res?.summary || {}
  } catch (e) {
    console.warn('[采购分析] 取数失败', e)
    message.error('获取数据失败')
    rows.value = []
    pagination.total = 0
    summary.value = {}
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleRefresh() {
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.page = page
  pagination.size = size
  fetchData()
}

function handleReset() {
  Object.assign(query, {
    supplierName: '', keyword: '', handlerName: '', warehouseName: '', brand: '',
    deptName: '', granularity: 'day'
  })
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

// ═══ 打印(F8) / 导出（当前 Tab 的默认可见列，所见即所打） ═══
const printableColumns = computed<DetailColumnConfig[]>(() =>
  leafColumns.value.filter(c => c.type !== 'rowNo' && !c.defaultHidden))

function cellText(c: DetailColumnConfig, r: any): string {
  const raw = r[c.key]
  if (c.type === 'slot') return raw === null || raw === undefined || raw === '' ? '-' : String(raw)
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
  const tabLabel = TABS.find(t => t.key === activeTab.value)?.label || '采购分析'
  const html = `<html><head><meta charset="utf-8"><title>采购分析-${tabLabel}</title>
    <style>body{font-family:system-ui,sans-serif;font-size:12px;padding:12px}
    h3{margin:0 0 8px}table{border-collapse:collapse;width:100%}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left;white-space:nowrap}</style></head><body>
    <h3>采购分析 · ${tabLabel}（${dateRange.value?.[0]} ~ ${dateRange.value?.[1]}）</h3>
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
    const res = await purchaseAnalysisApi.page({
      tab: activeTab.value,
      startDate: dateRange.value?.[0],
      endDate: dateRange.value?.[1],
      supplierName: query.supplierName || undefined,
      keyword: query.keyword || undefined,
      handlerName: query.handlerName || undefined,
      warehouseName: query.warehouseName || undefined,
      brand: query.brand || undefined,
      deptName: query.deptName || undefined,
      granularity: activeTab.value === 'time' ? query.granularity : undefined,
      page: p,
      size
    })
    const list = res?.records || []
    all.push(...list)
    if (list.length < size) break
  }
  return all
}

function handleExport() {
  const cols = printableColumns.value
  executeExport({
    fileName: `采购分析-${TABS.find(t => t.key === activeTab.value)?.label || ''}`,
    headers: cols.map(c => c.title),
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: (list: any[]) => list.map(r => cols.map(c => cellText(c, r))),
    fallbackRows: () => rows.value.map(r => cols.map(c => cellText(c, r)))
  })
}

function handleError(err: any) {
  console.error('[采购分析] 页面异常', err)
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
.cell-image { color: #1677ff; }
.cell-empty { color: #bfbfbf; }
</style>
