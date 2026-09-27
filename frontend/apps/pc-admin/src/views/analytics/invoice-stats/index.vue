<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        发票统计（分析 → 财务分析 → 发票统计，菜单 80455）
        对标 ql361「发票统计」：单视图增值税进销项月度台账，**多级表头**（仅一层 children）：
          3 个固定列（年度 / 月份 / 抵扣后应交税额）+「销项开票」7 叶子 +「取得进项专票」7 叶子 = 17 列，全部默认可见。
        对标工具栏最简（刷新 / 打印(F8) / 导出），**无「页面配置」弹窗、无「查询方案」下拉**
        （已逐页核文档 §2 与页面配置弹窗实据）→ 查询区为固定项，不接页面配置面板与查询方案条。
        取数：/erp/finance/analytics/invoice-stats/page（后端 InvoiceStatsController，数据源 invoice 表）。
      -->
      <CategoryListLayout
        :active-tab="'main'"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏右侧：刷新｜打印(F8)｜导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
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

        <!-- ═══ 查询区（对标：查询方式 + 月份(起) + 月份(止) + 进销均无发生的不显示；横向自适应网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div class="search-item">
                <span class="search-label">查询方式</span>
                <a-select
                  v-model:value="queryMode"
                  size="small"
                  style="width: 130px"
                  :options="QUERY_MODE_OPTIONS"
                />
              </div>
              <div class="search-item">
                <span class="search-label">月份(起)</span>
                <a-date-picker
                  v-model:value="monthStart"
                  size="small"
                  picker="month"
                  value-format="YYYY-MM"
                  :allow-clear="false"
                  style="width: 130px"
                />
              </div>
              <div class="search-item">
                <span class="search-label">月份(止)</span>
                <a-date-picker
                  v-model:value="monthEnd"
                  size="small"
                  picker="month"
                  value-format="YYYY-MM"
                  :allow-clear="false"
                  style="width: 130px"
                />
              </div>
              <div class="search-item search-actions">
                <a-checkbox v-model:checked="hideEmpty">进销均无发生的不显示</a-checkbox>
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格（多级表头 17 列，一套列配置） ═══ -->
        <template #table>
          <div class="table-area" :data-cols="colSummary">
            <BillDetailTable
              :data-source="rows"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              :summary-columns="summaryColumns"
              row-key="periodLabel"
              storage-key="analytics-invoice-stats-columns-main"
              global-config-key="analytics-invoice-stats-columns-main"
            />
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
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="analytics-invoice-stats"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DownloadOutlined, PrinterOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { invoiceStatsApi } from '@/api/analytics-finance'
import { useExport } from '@/composables/useExport'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'AnalyticsInvoiceStats' })

// ═══ 查询项（对标实测：查询方式 / 月份(起) / 月份(止) / 进销均无发生的不显示） ═══
const QUERY_MODE_OPTIONS = [
  { label: '按月查询', value: 'month' }
]
const queryMode = ref('month')
const monthStart = ref<string>(dayjs().startOf('year').format('YYYY-MM'))
const monthEnd = ref<string>(dayjs().format('YYYY-MM'))
const hideEmpty = ref(false)

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

function col(key: string, title: string, kind: Kind, width = 130): DetailColumnConfig {
  return {
    key,
    title,
    width,
    align: kind === 'text' ? 'left' : 'right',
    formatter: FORMATTER[kind]
  }
}

const ROW_NO: DetailColumnConfig = { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' }

/** 销项开票分组（7 叶子） */
function salesGroup(): DetailColumnConfig {
  return {
    key: 'salesGroup',
    title: '销项开票',
    children: [
      col('salesPosCount', '正数发票张数', 'num', 120),
      col('salesPosAmount', '正数开票金额', 'money', 130),
      col('salesNegCount', '负数发票张数', 'num', 120),
      col('salesNegAmount', '负数开票金额', 'money', 130),
      col('salesNetAmount', '销项开票金额', 'money', 130),
      col('salesTotalAmount', '销项价税合计', 'money', 130),
      col('salesTaxAmount', '销项税额', 'money', 120)
    ]
  }
}

/** 取得进项专票分组（7 叶子） */
function purchaseGroup(): DetailColumnConfig {
  return {
    key: 'purchaseGroup',
    title: '取得进项专票',
    children: [
      col('purPosCount', '正数发票张数', 'num', 120),
      col('purPosAmount', '正数开票金额', 'money', 130),
      col('purNegCount', '负数发票张数', 'num', 120),
      col('purNegAmount', '负数开票金额', 'money', 130),
      col('purNetAmount', '进项开票金额', 'money', 130),
      col('purTotalAmount', '进项价税合计', 'money', 130),
      col('purTaxAmount', '进项税额', 'money', 120)
    ]
  }
}

// ── 全部 17 列 / 默认 17（全可见），多级表头仅一层 ──
const columns: DetailColumnConfig[] = [
  ROW_NO,
  col('year', '年度', 'text', 90),
  col('month', '月份', 'text', 80),
  col('taxPayable', '抵扣后应交税额', 'money', 140),
  salesGroup(),
  purchaseGroup()
]

const leafColumns = computed<DetailColumnConfig[]>(() =>
  columns.flatMap(c => (c.children?.length ? c.children : [c])))

/** `全部可配置列/默认显示列`（供真机校验对标 17/17） */
const colSummary = computed(() => {
  const leaf = leafColumns.value.filter(c => c.type !== 'rowNo' && c.type !== 'action')
  return `${leaf.length}/${leaf.filter(c => !c.defaultHidden).length}`
})

/** 底部合计行：取后端 summary（按当前过滤范围 SUM） */
const SUMMARY_KEYS = ['salesPosCount', 'salesPosAmount', 'salesNegCount', 'salesNegAmount',
  'salesNetAmount', 'salesTotalAmount', 'salesTaxAmount', 'purPosCount', 'purPosAmount',
  'purNegCount', 'purNegAmount', 'purNetAmount', 'purTotalAmount', 'purTaxAmount', 'taxPayable']
const summaryColumns = computed(() =>
  SUMMARY_KEYS.filter(k => leafColumns.value.some(c => c.key === k))
    .map(k => ({ key: k, value: Number(summary.value[k]) || 0 })))

// ═══ 取数 ═══
function buildParams(extra: Record<string, any> = {}) {
  return {
    queryMode: queryMode.value,
    monthStart: monthStart.value,
    monthEnd: monthEnd.value,
    hideEmpty: hideEmpty.value || undefined,
    page: pagination.page,
    size: pagination.size,
    ...extra
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await invoiceStatsApi.page(buildParams())
    rows.value = res?.records || []
    pagination.total = Number(res?.total) || 0
    summary.value = res?.summary || {}
  } catch (e) {
    console.warn('[发票统计] 取数失败', e)
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
  queryMode.value = 'month'
  monthStart.value = dayjs().startOf('year').format('YYYY-MM')
  monthEnd.value = dayjs().format('YYYY-MM')
  hideEmpty.value = false
  handleSearch()
}

// ═══ 打印(F8) / 导出 ═══
const printableColumns = computed<DetailColumnConfig[]>(() =>
  leafColumns.value.filter(c => c.type !== 'rowNo' && !c.defaultHidden))

function cellText(c: DetailColumnConfig, r: any): string {
  const raw = r[c.key]
  return c.formatter ? c.formatter(raw, r) : fmtText(raw)
}

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML + window.open 打印窗口，现在交给 PrintDialog：列与行由页面给，模板负责版式。
// 打印列是 computed（多级表头叶子列按默认可见列过滤）→ 冻结不进模板，明确按数据列打。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'analytics-invoice-stats',
  title: () => `发票统计（${monthStart.value} ~ ${monthEnd.value}）`,
  useDataColumns: true,
  columns: () => printableColumns.value,
  rows: () => rows.value,
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
    const res = await invoiceStatsApi.page(buildParams({ page: p, size }))
    const list = res?.records || []
    all.push(...list)
    if (list.length < size) break
  }
  return all
}

function handleExport() {
  const cols = printableColumns.value
  executeExport({
    fileName: '发票统计',
    headers: cols.map(c => c.title),
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: (list: any[]) => list.map(r => cols.map(c => cellText(c, r))),
    fallbackRows: () => rows.value.map(r => cols.map(c => cellText(c, r)))
  })
}

function handleError(err: any) {
  console.error('[发票统计] 页面异常', err)
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

.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
</style>
