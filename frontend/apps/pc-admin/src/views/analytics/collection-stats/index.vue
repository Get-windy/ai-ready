<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        回款统计（分析 → 提成分析 → 回款统计，菜单 80442）
        对标 ql361「回款统计」：2 个视图 Tab（按职员 / 按部门），逐 Tab 独立列配置，各 7 列全可见。
        三口径：收款金额（erp_receipt）+ 预收款金额（erp_pre_receipt）+ 预订货收款金额（erp_sale_pre_order.received_deposit）
        = 回款总金额（逐行与合计均成立）。
        固定列：操作（行级「明细」钻取，不进列配置弹窗）。
        取数：/erp/finance/analytics/collection-stats/page?tab=staff|dept（后端 CollectionStatsAnalyticsController）。
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
            storage-key="analytics-collection-stats-query-scheme"
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

        <!-- ═══ 查询区（横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div v-if="isQueryVisible('collection.dateRange')" class="search-item">
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
              <div v-if="isQueryVisible('collection.dim')" class="search-item">
                <span class="search-label">{{ activeTab === 'staff' ? '职员' : '部门' }}</span>
                <a-input
                  v-model:value="dimName"
                  size="small"
                  :placeholder="activeTab === 'staff' ? '职员名称' : '部门名称'"
                  allow-clear
                  style="width: 150px"
                  @press-enter="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('collection.dateType')" class="search-item">
                <span class="search-label">日期类型</span>
                <a-select
                  v-model:value="query.dateType"
                  size="small"
                  style="width: 130px"
                  :options="DATE_TYPE_OPTIONS"
                  @change="handleSearch"
                />
              </div>
              <div v-if="isQueryVisible('collection.docType')" class="search-item">
                <span class="search-label">单据类型</span>
                <a-select
                  v-model:value="query.docType"
                  size="small"
                  style="width: 140px"
                  :options="DOC_TYPE_FILTER_OPTIONS"
                  @change="handleSearch"
                />
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
              row-key="groupKey"
              :storage-key="`analytics-collection-stats-columns-${activeTab}`"
              :global-config-key="`analytics-collection-stats-columns-${activeTab}`"
            >
              <template #detailCell="{ record }">
                <a-button type="link" size="small" @click="openDetail(record)">明细</a-button>
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

      <!-- ═══ 行级「明细」钻取（该职员/部门的三口径回款流水） ═══ -->
      <a-modal v-model:open="detailVisible" title="回款明细" :width="1000" :footer="null">
        <div class="detail-tip">
          统计对象：<b>{{ detailTitle }}</b>；三口径来源为收款单 / 预收款单 / 预订货单。
        </div>
        <a-table
          :data-source="detailRows"
          :columns="DETAIL_COLUMNS"
          :loading="detailLoading"
          size="small"
          row-key="__key"
          :pagination="{ pageSize: 20, size: 'small' }"
          :scroll="{ y: 420 }"
        />
      </a-modal>

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
import { collectionStatsApi } from '@/api/analytics-finance'
import { useExport } from '@/composables/useExport'
import QuerySchemeBar from '../shared/QuerySchemeBar.vue'
import { QUICK_DATES, quickDateRange } from '../shared/docTypes'
import { useAnalyticsPageConfig } from '../shared/useAnalyticsPageConfig'
import type { FunctionButtonSetting, QueryFieldSetting } from '../shared/useAnalyticsPageConfig'

defineOptions({ name: 'AnalyticsCollectionStats' })

// ═══ 视图 Tab（顺序逐字取自对标实测：按职员 / 按部门） ═══
const TABS = [
  { key: 'staff', label: '按职员' },
  { key: 'dept', label: '按部门' }
]
const activeTab = ref('staff')
const quickDate = ref('month')
const dateRange = ref<[string, string]>(quickDateRange('month') as [string, string])
const dimName = ref('')
const query = reactive({ dateType: 'bizDate', docType: 'ALL' })

const DATE_TYPE_OPTIONS = [
  { label: '单据日期', value: 'bizDate' },
  { label: '制单时间', value: 'createTime' }
]
const DOC_TYPE_FILTER_OPTIONS = [
  { label: '全部单据', value: 'ALL' },
  { label: '收款单', value: 'RECEIPT' },
  { label: '预收款单', value: 'PRE_RECEIPT' },
  { label: '预订货单', value: 'PRE_ORDER' }
]

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
function fmtText(v: any): string {
  return v === null || v === undefined || v === '' ? '-' : String(v)
}

type Kind = 'money' | 'num' | 'text'
const FORMATTER: Record<Kind, (v: any) => string> = {
  money: fmtMoney,
  num: fmtNum,
  text: fmtText
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
/** 对标行级「明细」为固定列（不进列配置弹窗）：key='action' 命中组件锁定列判定 */
const DETAIL_ACTION: DetailColumnConfig = {
  key: 'action', title: '操作', type: 'action', slotName: 'detailCell', width: 90, fixed: 'left'
}

/** 三口径金额块（两 Tab 逐字一致） */
function amountBlock(): DetailColumnConfig[] {
  return [
    col('receiptAmount', '收款金额', 'money', 130),
    col('preReceiptAmount', '预收款金额', 'money', 130),
    col('preOrderDepositAmount', '预订货收款金额', 'money', 150),
    col('totalAmount', '回款总金额', 'money', 130)
  ]
}

// ── Tab1「按职员」：全部 7 列 / 默认 7（全可见） ──
const staffColumns: DetailColumnConfig[] = [
  ROW_NO,
  DETAIL_ACTION,
  col('code', '职员编号', 'text', 110),
  col('name', '职员名称', 'text', 130),
  col('docCount', '单量', 'num', 90),
  ...amountBlock()
]

// ── Tab2「按部门」：全部 7 列 / 默认 7（全可见） ──
const deptColumns: DetailColumnConfig[] = [
  ROW_NO,
  DETAIL_ACTION,
  col('code', '部门编号', 'text', 110),
  col('name', '部门名称', 'text', 130),
  col('docCount', '单量', 'num', 90),
  ...amountBlock()
]

const activeColumns = computed<DetailColumnConfig[]>(() =>
  activeTab.value === 'dept' ? deptColumns : staffColumns)

const leafColumns = computed<DetailColumnConfig[]>(() =>
  activeColumns.value.flatMap(c => (c.children?.length ? c.children : [c])))

/** `全部可配置列/默认显示列`（供真机校验对标 7/7、7/7） */
const colSummary = computed(() => {
  const leaf = leafColumns.value.filter(c => c.type !== 'rowNo' && c.type !== 'action')
  return `${leaf.length}/${leaf.filter(c => !c.defaultHidden).length}`
})

/** 底部合计行：取后端 summary（按当前过滤范围 SUM） */
const SUMMARY_KEYS = ['docCount', 'receiptAmount', 'preReceiptAmount', 'preOrderDepositAmount', 'totalAmount']
const summaryColumns = computed(() =>
  SUMMARY_KEYS.filter(k => leafColumns.value.some(c => c.key === k))
    .map(k => ({ key: k, value: Number(summary.value[k]) || 0 })))

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'collection.dateRange', label: '日期', visible: true },
  { key: 'collection.dim', label: '职员/部门', visible: true },
  { key: 'collection.dateType', label: '日期类型', visible: true },
  { key: 'collection.docType', label: '单据类型', visible: true }
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
  storageKey: 'analytics-collection-stats-page-config',
  defaultQueryFields: DEFAULT_QUERY_FIELDS,
  defaultFunctionButtons: DEFAULT_FUNCTION_BUTTONS
})

// ═══ 查询方案 ═══
function schemeSnapshot(): Record<string, any> {
  return { dateRange: [...(dateRange.value || [])], quickDate: quickDate.value, dimName: dimName.value, ...query }
}
function applyScheme(v: Record<string, any>) {
  if (Array.isArray(v.dateRange) && v.dateRange.length === 2) {
    dateRange.value = [String(v.dateRange[0]), String(v.dateRange[1])]
  }
  if (typeof v.quickDate === 'string') quickDate.value = v.quickDate
  if (typeof v.dimName === 'string') dimName.value = v.dimName
  Object.keys(query).forEach(k => { if (k in v) (query as any)[k] = v[k] })
  handleSearch()
}

// ═══ 取数 ═══
function buildParams(extra: Record<string, any> = {}) {
  return {
    tab: activeTab.value,
    startDate: dateRange.value?.[0],
    endDate: dateRange.value?.[1],
    staffName: activeTab.value === 'staff' ? (dimName.value || undefined) : undefined,
    deptName: activeTab.value === 'dept' ? (dimName.value || undefined) : undefined,
    page: pagination.page,
    size: pagination.size,
    ...extra
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await collectionStatsApi.page(buildParams())
    rows.value = res?.records || []
    pagination.total = Number(res?.total) || 0
    summary.value = res?.summary || {}
  } catch (e) {
    console.warn('[回款统计] 取数失败', e)
    message.error('获取数据失败')
    rows.value = []
    pagination.total = 0
    summary.value = {}
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
  dimName.value = ''
  query.dateType = 'bizDate'
  query.docType = 'ALL'
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
  dimName.value = ''
  pagination.page = 1
  fetchData()
}

// ═══ 行级「明细」钻取 ═══
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailRows = ref<any[]>([])
const detailTitle = ref('')
const DETAIL_COLUMNS = [
  { title: '来源', dataIndex: 'source', width: 100 },
  { title: '单据编号', dataIndex: 'docNo', width: 190 },
  { title: '单据日期', dataIndex: 'bizDate', width: 120 },
  { title: '往来单位', dataIndex: 'partnerName', width: 180 },
  { title: '金额', dataIndex: 'amount', width: 130, align: 'right' as const },
  { title: '经手人', dataIndex: 'staffName', width: 110 },
  { title: '部门', dataIndex: 'deptName', width: 110 }
]

async function openDetail(record: any) {
  detailTitle.value = `${record.name}（${activeTab.value === 'staff' ? '按职员' : '按部门'}）`
  detailVisible.value = true
  detailLoading.value = true
  detailRows.value = []
  try {
    const res = await collectionStatsApi.detail({ ...buildParams(), groupKey: record.groupKey })
    const list: any[] = []
    ;['receipts', 'preReceipts', 'preOrders'].forEach((k, idx) => {
      const label = ['收款单', '预收款单', '预订货单'][idx]
      for (const r of ((res as any)?.[k] || [])) list.push({ ...r, source: label })
    })
    detailRows.value = list.map((r, i) => ({ ...r, __key: `${r.source}-${r.docNo}-${i}` }))
    if (!list.length) message.info('该对象在当前区间内没有回款流水')
  } catch (e) {
    console.warn('[回款统计] 明细取数失败', e)
    message.error('获取明细失败')
  } finally {
    detailLoading.value = false
  }
}

// ═══ 打印(F8) / 导出（当前 Tab 的默认可见列，所见即所打） ═══
const printableColumns = computed<DetailColumnConfig[]>(() =>
  leafColumns.value.filter(c => c.type !== 'rowNo' && c.type !== 'action' && !c.defaultHidden))

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
  const tabLabel = TABS.find(t => t.key === activeTab.value)?.label || '回款统计'
  const html = `<html><head><meta charset="utf-8"><title>回款统计-${tabLabel}</title>
    <style>body{font-family:system-ui,sans-serif;font-size:12px;padding:12px}
    h3{margin:0 0 8px}table{border-collapse:collapse;width:100%}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left;white-space:nowrap}</style></head><body>
    <h3>回款统计 · ${tabLabel}（${dateRange.value?.[0]} ~ ${dateRange.value?.[1]}）</h3>
    <table><thead><tr>${header.map(h => `<th>${h}</th>`).join('')}</tr></thead>
    <tbody>${body.map(row => `<tr>${row.map(v => `<td>${v}</td>`).join('')}</tr>`).join('')}</tbody>
    </table></body></html>`
  win.document.write(html)
  win.document.close()
  win.focus()
  win.print()
}

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
    const res = await collectionStatsApi.page(buildParams({ page: p, size }))
    const list = res?.records || []
    all.push(...list)
    if (list.length < size) break
  }
  return all
}

function handleExport() {
  const cols = printableColumns.value
  executeExport({
    fileName: `回款统计-${TABS.find(t => t.key === activeTab.value)?.label || ''}`,
    headers: cols.map(c => c.title),
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: (list: any[]) => list.map(r => cols.map(c => cellText(c, r))),
    fallbackRows: () => rows.value.map(r => cols.map(c => cellText(c, r)))
  })
}

function handleError(err: any) {
  console.error('[回款统计] 页面异常', err)
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
.detail-tip { margin-bottom: 8px; color: #666; font-size: 12px; }
</style>
