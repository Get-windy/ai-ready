<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        进销存分析（分析 → 仓配分析 → 进销存分析，菜单 80433）
        对标 ql361「进销存分析」：3 个视图 Tab（按商品 38 列·默认 9 / 仓库调拨分析 6 列·默认 6 / 商品调拨分析 11 列·默认 6），
        逐 Tab 独立列配置；「按商品」为多级表头（此前余额 / 五类入库 / 入库合计 / 五类出库 / 出库合计 / 期末余额，仅一层 children）。
        查询区：查询方案 + 8 段时间快捷段 + 日期范围 + 出库仓库 + 入库仓库 + 商品 + 品牌 + 显示红冲（横向网格）；
        工具栏 刷新｜打印(F8)｜导出｜页面配置。
        取数：/erp/stock/analytics/page?tab=product|transferWarehouse|transferProduct。
        ⚠️ 无数据源列（后端返回 null、本页显示 -，不填 0 冒充）：
           按商品「所属供应商」（全库无「商品↔供应商」关联表）、
           「采购入库\\采购费用分摊」（费用分摊仅整单级金额，无行级分摊口径）。
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
            storage-key="analytics-inventory-analysis-query-scheme"
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

        <!-- ═══ 查询区（对标实测：出库仓库 / 入库仓库 / 商品 / 品牌 + 显示红冲，横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div v-if="isQueryVisible('inventory.dateRange')" class="search-item">
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
              <div v-if="isQueryVisible('inventory.outWarehouseName')" class="search-item">
                <span class="search-label">出库仓库</span>
                <a-input
                  v-model:value="query.outWarehouseName"
                  size="small"
                  placeholder="仓库名称"
                  allow-clear
                  style="width: 150px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('inventory.inWarehouseName')" class="search-item">
                <span class="search-label">入库仓库</span>
                <a-input
                  v-model:value="query.inWarehouseName"
                  size="small"
                  placeholder="仓库名称"
                  allow-clear
                  style="width: 150px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('inventory.keyword')" class="search-item">
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
              <div v-if="isQueryVisible('inventory.brand')" class="search-item">
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
              <div class="search-item search-actions">
                <a-checkbox v-model:checked="query.showReversed" @change="handleSearch">显示红冲</a-checkbox>
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
              :storage-key="`analytics-inventory-analysis-columns-${activeTab}`"
              :global-config-key="`analytics-inventory-analysis-columns-${activeTab}`"
            >
              <template #actionCell="{ record }">
                <a-button type="link" size="small" @click="handleReconcile(record)">对账</a-button>
              </template>
              <template #transferActionCell="{ record }">
                <a-button type="link" size="small" @click="drillToProductTab(record)">商品分析</a-button>
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
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
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
import { inventoryAnalysisApi } from '@/api/analytics-supply'
import { useExport } from '@/composables/useExport'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'
import { QUICK_DATES, quickDateRange } from '../shared/docTypes'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'

defineOptions({ name: 'AnalyticsInventoryAnalysis' })

const router = useRouter()

// ═══ 视图 Tab（顺序逐字取自对标实测：按商品 / 仓库调拨分析 / 商品调拨分析） ═══
const TABS = [
  { key: 'product', label: '按商品' },
  { key: 'transferWarehouse', label: '仓库调拨分析' },
  { key: 'transferProduct', label: '商品调拨分析' }
]
const activeTab = ref('product')

const quickDate = ref('month')
const dateRange = ref<[string, string]>(quickDateRange('month') as [string, string])
const query = reactive({
  outWarehouseName: '',
  inWarehouseName: '',
  keyword: '',
  brand: '',
  showReversed: false
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

type Kind = 'money' | 'num' | 'text'

const FORMATTER: Record<Kind, (v: any) => string> = {
  money: fmtMoney,
  num: fmtNum,
  text: (v: any) => (v === null || v === undefined || v === '' ? '-' : String(v))
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
const RECONCILE_ACTION: DetailColumnConfig = {
  key: 'action', title: '操作', type: 'action', slotName: 'actionCell', width: 80, fixed: 'left'
}
const TRANSFER_ACTION: DetailColumnConfig = {
  key: 'action', title: '操作', type: 'action', slotName: 'transferActionCell', width: 90, fixed: 'left'
}

/** 此前余额分组标题的日期 = 期间起始日 − 1 天（对标实测表头「此前余额（2026-09-14之前）」） */
const openingLabel = computed(() => {
  const start = dateRange.value?.[0]
  const d = start ? dayjs(start).subtract(1, 'day') : dayjs().subtract(1, 'day')
  return `此前余额（${d.format('YYYY-MM-DD')}之前）`
})

/** 双列分组（小单位数量 + 金额） */
function group(title: string, qtyKey: string, amtKey: string, qtyTitle = '小单位数量', amtTitle = '金额'): DetailColumnConfig {
  return {
    key: `${qtyKey}Group`,
    title,
    children: [
      col(qtyKey, qtyTitle, 'num', 110, true),
      col(amtKey, amtTitle, 'money', 110, true)
    ]
  }
}

// ── Tab1「按商品」：全部 38 列 / 默认 9（多级表头仅一层） ──
const productColumns = computed<DetailColumnConfig[]>(() => [
  ROW_NO,
  RECONCILE_ACTION,
  col('dimLabel', '商品名称', 'text', 200),
  col('productCode', '货号', 'text', 110, true),
  col('spec', '规格', 'text', 120, true),
  col('model', '型号', 'text', 110, true),
  col('origin', '产地', 'text', 100, true),
  col('barcode', '条码', 'text', 130, true),
  col('unit', '单位', 'text', 70, true),
  col('brand', '品牌', 'text', 100, true),
  gap('supplierName', '所属供应商', 130),
  {
    key: 'openingGroup',
    title: openingLabel.value,
    children: [
      col('openingSmallQty', '小单位数量', 'num', 120),
      col('openingAmount', '金额', 'money', 110)
    ]
  },
  {
    key: 'purchaseInGroup',
    title: '采购入库',
    children: [
      col('purchaseInQty', '小单位数量', 'num', 110, true),
      // 无数据源列：费用分摊仅整单级金额，无行级分摊口径
      gap('purchaseInFeeShare', '采购费用分摊', 110),
      col('purchaseInAmount', '金额', 'money', 110, true)
    ]
  },
  group('调拨入库', 'transferInQty', 'transferInAmount'),
  group('其他入库', 'otherInQty', 'otherInAmount'),
  group('借进入库', 'borrowInQty', 'borrowInAmount'),
  group('销退入库', 'saleReturnInQty', 'saleReturnInAmount'),
  {
    key: 'inTotalGroup',
    title: '入库合计',
    children: [
      col('inTotalQty', '小单位数量', 'num', 120),
      col('inTotalAmount', '金额', 'money', 110)
    ]
  },
  group('销售出库', 'saleOutQty', 'saleOutAmount'),
  group('调拨出库', 'transferOutQty', 'transferOutAmount'),
  group('其他出库', 'otherOutQty', 'otherOutAmount'),
  group('借出出库', 'borrowOutQty', 'borrowOutAmount'),
  group('采退出库', 'purchaseReturnOutQty', 'purchaseReturnOutAmount'),
  {
    key: 'outTotalGroup',
    title: '出库合计',
    children: [
      col('outTotalQty', '出库小单位总量', 'num', 130),
      col('outTotalAmount', '出库金额', 'money', 110)
    ]
  },
  {
    key: 'closingGroup',
    title: '期末余额',
    children: [
      col('closingSmallQty', '期末小单位数量', 'num', 130),
      col('closingAmount', '期末金额', 'money', 110)
    ]
  }
])

// ── Tab2「仓库调拨分析」：全部 6 列 / 默认 6 ──
const transferWarehouseColumns: DetailColumnConfig[] = [
  ROW_NO,
  TRANSFER_ACTION,
  col('outWarehouseName', '出库仓库', 'text', 160),
  col('inWarehouseName', '入库仓库', 'text', 160),
  col('transferQty', '调拨数量', 'num', 110),
  col('transferAmount', '调拨金额', 'money', 120),
  col('costAmount', '成本金额', 'money', 120),
  col('diffAmount', '差异金额', 'money', 110)
]

// ── Tab3「商品调拨分析」：全部 11 列 / 默认 6 ──
const transferProductColumns: DetailColumnConfig[] = [
  ROW_NO,
  col('dimLabel', '商品名称', 'text', 200),
  col('productCode', '货号', 'text', 110, true),
  col('brand', '品牌', 'text', 100, true),
  gap('supplierName', '所属供应商', 130),
  col('unit', '单位', 'text', 70),
  col('transferQty', '调拨数量', 'num', 100),
  col('conversionRelation', '换算关系', 'text', 130, true),
  // 对标「列配置名 = 换算数量、实测表头 = 换算结果」→ 以实测表头为准
  col('conversionResult', '换算结果', 'text', 110, true),
  col('transferAmount', '调拨金额', 'money', 110),
  col('costAmount', '成本金额', 'money', 110),
  col('diffAmount', '差异金额', 'money', 110)
]

const activeColumns = computed<DetailColumnConfig[]>(() => {
  if (activeTab.value === 'transferWarehouse') return transferWarehouseColumns
  if (activeTab.value === 'transferProduct') return transferProductColumns
  return productColumns.value
})

const leafColumns = computed<DetailColumnConfig[]>(() =>
  activeColumns.value.flatMap(c => (c.children?.length ? c.children : [c])))

/** `全部可配置列/默认显示列`（供真机校验对标 38/9、6/6、11/6 口径） */
const colSummary = computed(() => {
  const leaf = leafColumns.value.filter(c => c.type !== 'rowNo' && c.type !== 'action')
  return `${leaf.length}/${leaf.filter(c => !c.defaultHidden).length}`
})

/** 底部合计行（后端按当前过滤范围 SUM） */
const SUMMARY_KEYS = ['openingSmallQty', 'openingAmount', 'purchaseInQty', 'purchaseInAmount',
  'transferInQty', 'transferInAmount', 'otherInQty', 'otherInAmount', 'borrowInQty', 'borrowInAmount',
  'saleReturnInQty', 'saleReturnInAmount', 'inTotalQty', 'inTotalAmount', 'saleOutQty', 'saleOutAmount',
  'transferOutQty', 'transferOutAmount', 'otherOutQty', 'otherOutAmount', 'borrowOutQty', 'borrowOutAmount',
  'purchaseReturnOutQty', 'purchaseReturnOutAmount', 'outTotalQty', 'outTotalAmount',
  'closingSmallQty', 'closingAmount', 'transferQty', 'transferAmount', 'costAmount', 'diffAmount']
const summaryColumns = computed(() => SUMMARY_KEYS
  .filter(k => leafColumns.value.some(c => c.key === k))
  .map(k => ({ key: k, value: Number(summary.value[k]) || 0 })))

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'inventory.dateRange', label: '日期', visible: true },
  { key: 'inventory.outWarehouseName', label: '出库仓库', visible: true },
  { key: 'inventory.inWarehouseName', label: '入库仓库', visible: true },
  { key: 'inventory.keyword', label: '商品', visible: true },
  { key: 'inventory.brand', label: '品牌', visible: true }
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
  storageKey: 'analytics-inventory-analysis-page-config',
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
function buildParams(extra: Record<string, any> = {}) {
  return {
    tab: activeTab.value,
    startDate: dateRange.value?.[0],
    endDate: dateRange.value?.[1],
    outWarehouseName: query.outWarehouseName || undefined,
    inWarehouseName: query.inWarehouseName || undefined,
    keyword: query.keyword || undefined,
    brand: query.brand || undefined,
    showReversed: query.showReversed || undefined,
    page: pagination.page,
    size: pagination.size,
    ...extra
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await inventoryAnalysisApi.page(buildParams())
    rows.value = res?.records || []
    pagination.total = Number(res?.total) || 0
    summary.value = res?.summary || {}
  } catch (e) {
    console.warn('[进销存分析] 取数失败', e)
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
    outWarehouseName: '', inWarehouseName: '', keyword: '', brand: '', showReversed: false
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

/** 按商品行级「对账」→ 该商品期间明细流水（《库存明细》支持 keyword 入参） */
function handleReconcile(record: any) {
  const keyword = record.productCode || record.dimLabel
  if (!keyword) {
    message.warning('该行无商品编码，无法跳转库存明细')
    return
  }
  router.push({ path: '/analytics/stock-detail', query: { keyword: String(keyword) } })
}

/** 仓库调拨分析行级「商品分析」→ 切到商品调拨分析并带出该仓对 */
function drillToProductTab(record: any) {
  query.outWarehouseName = record.outWarehouseName && record.outWarehouseName !== '未指定仓库' ? record.outWarehouseName : ''
  query.inWarehouseName = record.inWarehouseName && record.inWarehouseName !== '未指定仓库' ? record.inWarehouseName : ''
  activeTab.value = 'transferProduct'
  pagination.page = 1
  fetchData()
}

// ═══ 打印(F8) / 导出（当前 Tab 的默认可见列，所见即所打） ═══
const printableColumns = computed<DetailColumnConfig[]>(() =>
  leafColumns.value.filter(c => c.type !== 'rowNo' && c.type !== 'action' && !c.defaultHidden))

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
  const tabLabel = TABS.find(t => t.key === activeTab.value)?.label || '进销存分析'
  const html = `<html><head><meta charset="utf-8"><title>进销存分析-${tabLabel}</title>
    <style>body{font-family:system-ui,sans-serif;font-size:12px;padding:12px}
    h3{margin:0 0 8px}table{border-collapse:collapse;width:100%}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left;white-space:nowrap}</style></head><body>
    <h3>进销存分析 · ${tabLabel}（${dateRange.value?.[0]} ~ ${dateRange.value?.[1]}）</h3>
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
    const res = await inventoryAnalysisApi.page(buildParams({ page: p, size }))
    const list = res?.records || []
    all.push(...list)
    if (list.length < size) break
  }
  return all
}

function handleExport() {
  const cols = printableColumns.value
  executeExport({
    fileName: `进销存分析-${TABS.find(t => t.key === activeTab.value)?.label || ''}`,
    headers: cols.map(c => c.title),
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: (list: any[]) => list.map(r => cols.map(c => cellText(c, r))),
    fallbackRows: () => rows.value.map(r => cols.map(c => cellText(c, r)))
  })
}

function handleError(err: any) {
  console.error('[进销存分析] 页面异常', err)
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
