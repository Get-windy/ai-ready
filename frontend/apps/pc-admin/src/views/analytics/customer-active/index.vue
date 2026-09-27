<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        客户活跃分析（分析 → 采销分析 → 销售分析 → 客户活跃分析，菜单 80417）
        对标 ql361：单视图台账（8 列 / 默认 8，全可见，无默认隐藏列），无左侧分类树，
        工具栏 导出｜打印(F8)｜刷新；无「页面配置」弹窗（pageConfig.found=false 且 shots 下无该图）→ 查询区为固定项。
        取数：/erp/sale/analysis/customer-active/page（每客户一行：分层/最近交易/近窗口下单金额与次数）。
        ⚠️ 列口径与后端差异（本轮如实上报，未动后端）：
          ① 对标列「客户编号」「最近拜访日期」后端 DTO 未返回（CustomerActiveAnalysisDTO 无此二字段），
             列位按对标保留但恒显示 '-'（不伪造数据）；
          ② 「未交易天数」无后端字段，按 当前日期 − 最近交易日期 前端推算（与文档 §4 口径一致）；
          ③ 对标分层为「活跃客户 / 即将流失客户」，本系统后端 activityLevel 为「活跃 / 一般 / 沉默」，照实展示不映射；
          ④ 后端无合计字段（无 summary），故本页不做合计行（不拿当前页求和冒充全量合计）。
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏右侧：刷新｜打印(F8)｜导出 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button size="small" :loading="loading" @click="handleRefresh">
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

        <!-- ═══ 查询区（对标 客户 + 客户类型；客户类型后端无对应参数，故只保留可生效项，横向网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div class="search-item">
                <span class="search-label">客户</span>
                <a-input
                  v-model:value="query.keyword"
                  size="small"
                  placeholder="客户名称"
                  allow-clear
                  style="width: 200px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item search-actions">
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格（对标 8 列 / 默认 8，表头齿轮列配置） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              :data-source="dataSource"
              :columns="columns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              :summary-columns="summaryColumns"
              row-key="rowKey"
              storage-key="analytics-customer-active-columns"
              global-config-key="analytics-customer-active-columns"
            >
              <template #activityLevelCell="{ record }">
                <span :class="levelClass(record.activityLevel)">{{ record.activityLevel || '-' }}</span>
              </template>
              <template #customerCodeCell="{ record }">
                <span>{{ customerCode(record) }}</span>
              </template>
              <template #lastVisitCell>
                <span class="cell-empty">-</span>
              </template>
              <template #idleDaysCell="{ record }">
                <span>{{ idleDays(record.lastOrderTime) }}</span>
              </template>
              <template #amountCell="{ record }">
                <span>{{ formatMoney(record.recentOrderAmount) }}</span>
              </template>
              <template #countCell="{ record }">
                <span>{{ formatNumber(record.recentOrderCount) }}</span>
              </template>
            </BillDetailTable>
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
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="analytics-customer-active"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DownloadOutlined, PrinterOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import { saleAnalyticsApi } from '@/api/analytics'
import { useExport } from '@/composables/useExport'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'AnalyticsCustomerActive' })

const query = reactive({ keyword: '' })

/**
 * 近窗口天数：对标列配置弹窗口径为「近半年」、表头渲染为「近90天」（文档 §3 记两处冲突、待复核）。
 * 本页列名逐字取列配置弹窗口径（近半年），故窗口天数固定 180，保证「列名与数据口径自洽」。
 */
const WINDOW_DAYS = 180

const loading = ref(false)
const dataSource = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 列定义（对标 8 列 / 默认 8，全可见 → 无 defaultHidden） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
  { key: 'activityLevel', title: '客户分类', type: 'slot', slotName: 'activityLevelCell', width: 120 },
  { key: 'customerName', title: '客户名称', width: 220 },
  { key: 'customerCode', title: '客户编号', type: 'slot', slotName: 'customerCodeCell', width: 140 },
  { key: 'lastOrderTime', title: '最近交易日期', width: 130 },
  { key: 'lastVisitTime', title: '最近拜访日期', type: 'slot', slotName: 'lastVisitCell', width: 130 },
  { key: 'idleDays', title: '未交易天数', type: 'slot', slotName: 'idleDaysCell', width: 110, align: 'right' },
  { key: 'recentOrderAmount', title: '近半年下单金额', type: 'slot', slotName: 'amountCell', width: 140, align: 'right' },
  { key: 'recentOrderCount', title: '近半年下单次数', type: 'slot', slotName: 'countCell', width: 140, align: 'right' }
]

/** 合计行：后端未返回 summary，故不显示合计（不以当前页求和冒充全量合计） */
const summaryColumns = computed(() => [])

// ═══ 取数 ═══
function normalizeRow(r: any, index: number) {
  return {
    ...r,
    rowKey: String(r.customerId ?? `row-${index}`),
    lastOrderTime: formatDate(r.lastOrderTime)
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await saleAnalyticsApi.customerActivePage({
      page: pagination.current,
      size: pagination.pageSize,
      days: WINDOW_DAYS,
      keyword: query.keyword || undefined
    })
    dataSource.value = (res?.records || []).map(normalizeRow)
    pagination.total = Number(res?.total) || 0
  } catch (e) {
    console.warn('[客户活跃分析] 取数失败', e)
    message.error('获取数据失败')
    dataSource.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  return fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  return fetchData()
}

function handleRefresh() {
  return fetchData()
}

function handleReset() {
  query.keyword = ''
  return handleSearch()
}

// ═══ 格式化 / 派生 ═══
function formatDate(val: any): string {
  if (!val) return '-'
  return String(val).slice(0, 10)
}

/** 未交易天数 = 当前日期 − 最近交易日期（对标文档 §4 口径，已用 4 点实测值交叉验证） */
function idleDays(lastOrderTime: any): string {
  const day = formatDate(lastOrderTime)
  if (day === '-') return '-'
  const diff = Date.now() - new Date(`${day}T00:00:00`).getTime()
  if (isNaN(diff)) return '-'
  return String(Math.max(0, Math.floor(diff / 86400000)))
}

/** 客户编号：后端 DTO 未返回该字段，如实留空（不拿 customerId 冒充编号） */
function customerCode(record: any): string {
  const code = record?.customerCode
  return code === null || code === undefined || code === '' ? '-' : String(code)
}

function formatNumber(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

function formatMoney(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function levelClass(level: string): string {
  if (level === '活跃' || level === '活跃客户') return 'level-active'
  if (level === '一般') return 'level-normal'
  if (level === '沉默' || level === '即将流失客户') return 'level-silent'
  return ''
}

function cellText(col: DetailColumnConfig, r: any): string {
  if (col.key === 'idleDays') return idleDays(r.lastOrderTime)
  if (col.key === 'customerCode') return customerCode(r)
  if (col.key === 'lastVisitTime') return '-'
  if (col.key === 'recentOrderAmount') return formatMoney(r.recentOrderAmount)
  if (col.key === 'recentOrderCount') return formatNumber(r.recentOrderCount)
  const v = r[col.key]
  return v === null || v === undefined || v === '' ? '' : String(v)
}

/** 打印 / 导出列：剔除序号列，其余按当前列口径输出 */
const printableColumns = computed<DetailColumnConfig[]>(() =>
  columns.filter(c => c.key !== 'rowNo')
)

// ═══ 打印（结果集打印） ═══
// 原先是自己拼 HTML + window.open 打印窗口，现在交给 PrintDialog：列与行由页面给，模板负责版式。
// 列已冻结进模板（模板说了算）；columns 只用于把「未交易天数/客户编号」等派生文本喂给行数据。
const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'analytics-customer-active',
  title: '客户活跃分析',
  columns: () => printableColumns.value.map(c => ({ ...c, formatter: (_v: any, r: any) => cellText(c, r) })),
  rows: () => dataSource.value,
  emptyTip: '没有可打印的数据',
})

/** F8 快捷键（对标工具栏「打印(F8)」） */
function handleF8Key(e: KeyboardEvent) {
  if (e.key === 'F8') {
    e.preventDefault()
    handlePrint()
  }
}

// ═══ 导出（逐页取全量 → CSV） ═══
const { execute: executeExport, exporting } = useExport()

async function fetchAllRows(): Promise<any[]> {
  const size = 100
  const all: any[] = []
  const pages = Math.max(1, Math.ceil(pagination.total / size))
  for (let p = 1; p <= pages; p++) {
    const res: any = await saleAnalyticsApi.customerActivePage({
      page: p,
      size,
      days: WINDOW_DAYS,
      keyword: query.keyword || undefined
    })
    const list: any[] = res?.records || []
    all.push(...list.map(normalizeRow))
    if (list.length < size) break
  }
  return all
}

function handleExport() {
  const cols = printableColumns.value
  executeExport({
    fileName: '客户活跃分析',
    headers: cols.map(c => c.title),
    total: pagination.total,
    fetchAll: fetchAllRows,
    mapToRows: (list: any[]) => list.map(r => cols.map(c => cellText(c, r))),
    fallbackRows: () => dataSource.value.map(r => cols.map(c => cellText(c, r)))
  })
}

function handleError(err: any) {
  console.error('[客户活跃分析] 页面异常', err)
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
.cell-empty { color: #bfbfbf; }
.level-active { color: #52c41a; }
.level-normal { color: #fa8c16; }
.level-silent { color: #8c8c8c; }
</style>
