<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        销售费用分析（分析 → 采销分析 → 销售分析 → 销售费用分析，菜单 80416）
        对标 ql361：**双视图 Tab 页**（按往来单位 / 按职员，各 11 列 / 默认 11 全可见，逐 Tab 独立列配置）；
        时间快捷段 8 段 + 日期范围 + 维度过滤；工具栏 刷新｜打印(F8)｜导出（对标另有「总额设置」，弹窗字段待复核，未落地）。
        无「页面配置」弹窗实据（pageConfig.found=false 且 shots 下无该图）→ 查询区为固定项。
        ⚠️ 取数口径（本轮如实上报，未动后端）：
          ① 对标 11 列中「销售收入 + 六类费用（赠品/促销活动/优惠/商品兑付/金额兑付/其它费用）+ 费销比」
             在本系统**无任何后端端点**（已核 erp-sales / erp-finance / erp-stock 全部 @GetMapping，无
             按往来单位/职员 归集销售费用的接口；api/analytics.ts 的 expenseStatisticsApi 只有 by-department /
             by-type 且无数据）→ 这些列按对标列位保留但恒显示 '-'，**不编造数值**；
          ② 可落地的是「往来单位 / 职员」两个维度本身：取 /erp/finance/expense-doc/page（费用单，字段含
             partnerCode/partnerName/handlerName/totalAmount）按维度聚合，填 单位/职员 名称与 费用总额；
             ⚠️ 该口径 = 费用单单据口径，与文档定义的「销售让利六类之和」不同，需后端补端点后方可对齐（见汇报）。
      -->
      <CategoryListLayout
        :tabs="TABS"
        :active-tab="activeTab"
        :show-category-panel="false"
        :show-table-footer="true"
        @tab-change="onTabChange"
      >
        <!-- ═══ 工具栏左侧：时间快捷段（对标实测 8 段） ═══ -->
        <template #toolbar-left>
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

        <!-- ═══ 查询区（逐 Tab 各一套：维度过滤项按对标随 Tab 切换，横向网格） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div class="search-item">
                <span class="search-label">日期</span>
                <a-range-picker v-model:value="dateRange" size="small" style="width: 230px" value-format="YYYY-MM-DD" @change="quickDate = ''" />
              </div>
              <div class="search-item">
                <span class="search-label">{{ activeTab === 'byStaff' ? '职员' : '往来单位' }}</span>
                <a-input
                  v-model:value="q.primaryName"
                  size="small"
                  :placeholder="activeTab === 'byStaff' ? '职员' : '往来单位'"
                  allow-clear
                  style="width: 160px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">{{ activeTab === 'byStaff' ? '往来单位' : '经手人' }}</span>
                <a-input
                  v-model:value="q.secondaryName"
                  size="small"
                  :placeholder="activeTab === 'byStaff' ? '往来单位' : '经手人'"
                  allow-clear
                  style="width: 160px"
                  @press-enter="handleSearch"
                />
              </div>
              <div class="search-item">
                <span class="search-label">部门</span>
                <a-input v-model:value="q.deptName" size="small" placeholder="部门" allow-clear style="width: 130px" @press-enter="handleSearch" />
              </div>
              <div class="search-item search-actions">
                <a-button type="primary" size="small" @click="handleSearch">查询</a-button>
                <a-button size="small" @click="handleReset">重置</a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 数据表格（逐 Tab 11 列 / 默认 11，逐 Tab 独立 storage-key） ═══ -->
        <template #table>
          <div class="table-area">
            <BillDetailTable
              :data-source="pagedRows"
              :columns="activeColumns"
              :loading="loading"
              :view-mode="true"
              :min-rows="20"
              row-key="rowKey"
              :storage-key="`analytics-sales-expense-columns-${activeTab}`"
              :global-config-key="`analytics-sales-expense-columns-${activeTab}`"
            >
              <template #dimCodeCell="{ record }">
                <span>{{ record.dimCode || '-' }}</span>
              </template>
              <template #dimNameCell="{ record }">
                <span>{{ record.dimName || '-' }}</span>
              </template>
              <template #moneyCell="{ record, column }">
                <span>{{ formatMoney(record[column.key]) }}</span>
              </template>
              <template #ratioCell="{ record }">
                <span>{{ record.expenseRatio === null ? '-' : record.expenseRatio }}</span>
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
import { expenseDocApi } from '@/api/finance'
import { useExport } from '@/composables/useExport'
import { QUICK_DATES, quickDateRange } from '../shared/docTypes'

defineOptions({ name: 'AnalyticsSalesExpense' })

/** 视图 Tab（对标实测顺序：按往来单位 / 按职员） */
const TABS = [
  { key: 'byPartner', label: '按往来单位' },
  { key: 'byStaff', label: '按职员' }
]
const activeTab = ref('byPartner')

const quickDate = ref('month')
const dateRange = ref<[string, string] | null>(quickDateRange('month'))

/** 逐 Tab 独立查询条件（对标：两 Tab 的维度过滤项随 Tab 切换） */
const queries = reactive<Record<string, any>>({
  byPartner: { primaryName: '', secondaryName: '', deptName: '' },
  byStaff: { primaryName: '', secondaryName: '', deptName: '' }
})
const q = computed<any>(() => queries[activeTab.value])

const loading = ref(false)
const allRows = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

// ═══ 列定义（对标 11 列 / 默认 11，全可见 → 无 defaultHidden；列名逐字取自文档 §3） ═══
function buildColumns(nameTitle: string, codeTitle: string): DetailColumnConfig[] {
  return [
    { key: 'rowNo', title: '', type: 'rowNo', width: 44, fixed: 'left' },
    { key: 'dimCode', title: codeTitle, type: 'slot', slotName: 'dimCodeCell', width: 150 },
    { key: 'dimName', title: nameTitle, type: 'slot', slotName: 'dimNameCell', width: 220 },
    { key: 'salesRevenue', title: '销售收入', type: 'slot', slotName: 'moneyCell', width: 120, align: 'right' },
    { key: 'gift', title: '赠品', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
    { key: 'promotion', title: '促销活动', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
    { key: 'discount', title: '优惠', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
    { key: 'goodsPay', title: '商品兑付', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
    { key: 'amountPay', title: '金额兑付', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
    { key: 'otherExpense', title: '其它费用', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
    { key: 'totalExpense', title: '费用总额', type: 'slot', slotName: 'moneyCell', width: 110, align: 'right' },
    { key: 'expenseRatio', title: '费销比(%)', type: 'slot', slotName: 'ratioCell', width: 110, align: 'right' }
  ]
}

const partnerColumns = buildColumns('单位名称', '单位编号')
const staffColumns = buildColumns('职员名称', '职员编号')

const activeColumns = computed<DetailColumnConfig[]>(() =>
  activeTab.value === 'byStaff' ? staffColumns : partnerColumns
)

/** 前端聚合后的分页视图（聚合在客户端完成，故分页亦在前端） */
const pagedRows = computed(() => {
  const start = (pagination.current - 1) * pagination.pageSize
  return allRows.value.slice(start, start + pagination.pageSize)
})

// ═══ 取数：费用单全量 → 按当前 Tab 维度聚合 ═══
const FETCH_PAGE_SIZE = 500
const MAX_ROWS = 5000

async function fetchAllExpenseDocs(): Promise<any[]> {
  const params: Record<string, any> = {}
  const [startDate, endDate] = dateRange.value || []
  if (startDate) params.dateStart = startDate
  if (endDate) params.dateEnd = endDate
  const activeQuery = q.value
  if (activeQuery.deptName) params.deptName = activeQuery.deptName
  if (activeTab.value === 'byPartner') {
    if (activeQuery.primaryName) params.partnerName = activeQuery.primaryName
    if (activeQuery.secondaryName) params.handlerName = activeQuery.secondaryName
  } else {
    if (activeQuery.primaryName) params.handlerName = activeQuery.primaryName
    if (activeQuery.secondaryName) params.partnerName = activeQuery.secondaryName
  }

  const all: any[] = []
  for (let p = 1; ; p++) {
    const res: any = await expenseDocApi.getPage({ pageNum: p, pageSize: FETCH_PAGE_SIZE, ...params })
    const recs: any[] = res?.records || []
    all.push(...recs)
    if (recs.length < FETCH_PAGE_SIZE || all.length >= MAX_ROWS) break
  }
  return all
}

/** 按维度聚合（往来单位 = partnerCode/partnerName；职员 = handlerName） */
function aggregate(docs: any[], byStaff: boolean): any[] {
  const map = new Map<string, any>()
  for (const d of docs) {
    const name = byStaff
      ? (d.handlerName || '未指定职员')
      : (d.partnerName || '未指定往来单位')
    const code = byStaff ? '' : (d.partnerCode || '')
    const key = `${code}|${name}`
    const row = map.get(key) || { rowKey: key, dimCode: code, dimName: name, totalExpense: 0 }
    row.totalExpense += Number(d.totalAmount) || 0
    map.set(key, row)
  }
  return [...map.values()]
    .map(r => ({
      ...r,
      // 无数据源的对标列如实留空（不编造 0）
      salesRevenue: null,
      gift: null,
      promotion: null,
      discount: null,
      goodsPay: null,
      amountPay: null,
      otherExpense: null,
      expenseRatio: null
    }))
    .sort((a, b) => b.totalExpense - a.totalExpense)
}

async function fetchData() {
  loading.value = true
  try {
    const docs = await fetchAllExpenseDocs()
    allRows.value = aggregate(docs, activeTab.value === 'byStaff')
    pagination.total = allRows.value.length
    if (docs.length >= MAX_ROWS) {
      message.warning(`数据量过大，仅统计前 ${MAX_ROWS} 条费用单，请缩小日期范围`)
    }
  } catch (e) {
    console.warn('[销售费用分析] 取数失败', e)
    message.error('获取数据失败')
    allRows.value = []
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
}

function handleRefresh() {
  return fetchData()
}

function handleReset() {
  quickDate.value = 'month'
  dateRange.value = quickDateRange('month')
  Object.assign(queries[activeTab.value], { primaryName: '', secondaryName: '', deptName: '' })
  return handleSearch()
}

function setQuickDate(key: string) {
  quickDate.value = key
  dateRange.value = quickDateRange(key)
  handleSearch()
}

function onTabChange(key: string) {
  activeTab.value = key
  pagination.current = 1
  fetchData()
}

// ═══ 格式化 ═══
function formatMoney(val: any): string {
  if (val === null || val === undefined || val === '' || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const MONEY_KEYS = ['salesRevenue', 'gift', 'promotion', 'discount', 'goodsPay', 'amountPay', 'otherExpense', 'totalExpense']

function cellText(col: DetailColumnConfig, r: any): string {
  if (col.key === 'dimCode') return r.dimCode || '-'
  if (col.key === 'dimName') return r.dimName || '-'
  if (col.key === 'expenseRatio') return r.expenseRatio === null ? '-' : String(r.expenseRatio)
  if (MONEY_KEYS.includes(col.key)) return formatMoney(r[col.key])
  return r[col.key] ?? ''
}

/** 打印 / 导出列：剔除序号列 */
const printableColumns = computed<DetailColumnConfig[]>(() =>
  activeColumns.value.filter(c => c.key !== 'rowNo')
)

// ═══ 打印(F8) ═══
function handlePrint() {
  const cols = printableColumns.value
  const header = cols.map(c => c.title)
  const body = pagedRows.value.map(r => cols.map(c => cellText(c, r)))
  const win = window.open('', '_blank', 'width=1400,height=800')
  if (!win) {
    message.warning('浏览器拦截了打印窗口，请允许弹窗后重试')
    return
  }
  const tabLabel = TABS.find(t => t.key === activeTab.value)?.label || '销售费用分析'
  const html = `<html><head><meta charset="utf-8"><title>销售费用分析-${tabLabel}</title>
    <style>body{font-family:system-ui,sans-serif;font-size:12px;padding:12px}
    h3{margin:0 0 8px}table{border-collapse:collapse;width:100%}
    th,td{border:1px solid #999;padding:4px 6px;text-align:left;white-space:nowrap}</style></head><body>
    <h3>销售费用分析 · ${tabLabel}（${dateRange.value?.[0] || ''} ~ ${dateRange.value?.[1] || ''}）</h3>
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

// ═══ 导出（聚合结果全量 → CSV） ═══
const { execute: executeExport, exporting } = useExport()

function handleExport() {
  const cols = printableColumns.value
  const mapRows = (list: any[]) => list.map(r => cols.map(c => cellText(c, r)))
  executeExport({
    fileName: `销售费用分析-${TABS.find(t => t.key === activeTab.value)?.label || ''}`,
    headers: cols.map(c => c.title),
    total: pagination.total,
    fetchAll: async () => allRows.value,
    mapToRows: mapRows,
    fallbackRows: () => mapRows(pagedRows.value)
  })
}

function handleError(err: any) {
  console.error('[销售费用分析] 页面异常', err)
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
