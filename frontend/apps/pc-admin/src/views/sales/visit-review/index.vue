<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        拜访检视（CRM → 外勤拜访 → 拜访检视，菜单 70003）
        · CRM 为本系统独有模块（ql361 无 CRM 域），按业界生产级 CRM 的「计划-执行-检视」模型建模
        · 规格：docs/Yh-Spec/手动整理对标开发文档/crm模块/拜访检视开发文档.md
        · 路线 A：ErrorBoundary > PageContainer(full-height) > CategoryListLayout > BillDetailTable
        · 本页为**纯只读检视页**：无新增/编辑/删除/行操作（不按模板硬加写入口）
        · 本轮由路线 B（ARReportPage）整体重写为路线 A，并补：
            ① 趋势图（/review/page 的 byDate 后端每次都算、前端从未使用，此处落地为柱状趋势）；
            ② 计划覆盖率的分子分母与拜访转化率（planTotal/planExecuted/interested/total 均已返回）；
            ③ 口径标注条：今日/本周/覆盖率为全局口径，不随筛选变化；
            ④ 列配置齿轮 + 页面配置弹窗 + 经典分页栏；
            ⑤ 修复旧实现「onMounted + @loaded 各请求一次 /stats/summary」的双请求。
        · 未接线：按客户/负责人筛选（后端 review/page 的 Controller 未开参，Service 已支持）
                   —— 前端类型虽已声明，直接接线会「看似生效实则无效」，故保留为缺口
      -->
      <CategoryListLayout
        :tabs="[]"
        :show-category-panel="false"
        :show-table-footer="true"
      >
        <!-- ═══ 工具栏左侧：口径说明 ═══ -->
        <template #toolbar-left>
          <span class="toolbar-tip">只读检视：修改拜访数据请到「拜访规划 / 拜访执行」页</span>
        </template>

        <!-- ═══ 工具栏右侧：刷新 / 图表显隐 / 打印(F8) / 导出 / 页面配置 ═══ -->
        <template #toolbar-right>
          <a-space :size="8">
            <a-button
              v-if="isButtonEnabled('refresh')"
              size="small"
              :loading="loading"
              @click="handleRefresh"
            >
              <ReloadOutlined /> 刷新
            </a-button>
            <!-- 提示下置（placement=bottom）避免浮层压住按钮 -->
            <a-tooltip
              v-if="isButtonEnabled('charts')"
              :title="showCharts ? '隐藏图表' : '显示图表'"
              placement="bottom"
            >
              <a-button
                :type="showCharts ? 'primary' : 'default'"
                size="small"
                @click="toggleCharts"
              >
                <BarChartOutlined />
              </a-button>
            </a-tooltip>
            <a-button
              v-if="isButtonEnabled('printF8')"
              size="small"
              @click="handlePrint"
            >
              <PrinterOutlined /> 打印(F8)
            </a-button>
            <a-button
              v-if="isButtonEnabled('export')"
              size="small"
              @click="handleExport"
            >
              <DownloadOutlined /> 导出
            </a-button>
            <a-tooltip
              title="页面配置"
              placement="bottom"
            >
              <a-button
                size="small"
                @click="showPageConfig = true"
              >
                <SettingOutlined />
              </a-button>
            </a-tooltip>
          </a-space>
        </template>

        <!-- ═══ 查询区（横向网格；显隐受页面配置控制） ═══ -->
        <template #search-fields>
          <div class="search-area">
            <div class="search-grid">
              <div
                v-if="isFieldVisible('result')"
                class="search-item"
              >
                <span class="search-label">拜访结果</span>
                <a-select
                  v-model:value="searchForm.result"
                  placeholder="全部结果"
                  size="small"
                  allow-clear
                  :options="resultOptions"
                  @change="handleSearch"
                />
              </div>
              <div
                v-if="isFieldVisible('visitDateRange')"
                class="search-item"
              >
                <span class="search-label">拜访日期</span>
                <a-range-picker
                  v-model:value="searchForm.visitDateRange"
                  size="small"
                  value-format="YYYY-MM-DD"
                  :placeholder="['开始日期', '结束日期']"
                  @change="handleSearch"
                />
              </div>
              <div class="search-item search-actions">
                <a-button
                  type="primary"
                  size="small"
                  @click="handleSearch"
                >
                  查询
                </a-button>
                <a-button
                  size="small"
                  @click="handleReset"
                >
                  重置
                </a-button>
              </div>
            </div>
          </div>
        </template>

        <!-- ═══ 统计卡 + 趋势图 + 检视台账 ═══ -->
        <template #table>
          <div class="review-body">
            <ARStatCards
              :items="statCards"
              :loading="loading"
              class="stats-cards"
            />

            <!-- 指标口径补充：覆盖率分子分母、拜访转化率、口径提示 -->
            <div class="stats-meta">
              <span class="meta-item">
                <span class="meta-label">计划覆盖率</span>
                <span class="meta-value">{{ coverageDetail }}</span>
              </span>
              <span class="meta-item">
                <span class="meta-label">拜访转化率（有意向/总数）</span>
                <span class="meta-value">{{ conversionRate }}</span>
              </span>
              <span class="meta-item meta-tip">
                今日 / 本周 / 计划覆盖率为全局口径，不随下方筛选变化；其余 4 张随拜访日期区间变化（结果筛选不影响分布卡，属有意设计）
              </span>
            </div>

            <!-- 趋势图（可显隐）：按日拜访次数，数据来自 /review/page 的 byDate，随日期区间变化 -->
            <div
              v-if="showCharts"
              class="chart-row"
            >
              <ARReportChart
                title="拜访趋势（按日）"
                :option="trendChartOption"
                :loading="loading"
                :height="200"
                empty-text="暂无拜访趋势数据"
              />
            </div>

            <!-- 检视台账（只读，无操作列） -->
            <div class="table-area">
              <BillDetailTable
                v-model:data-source="tableData"
                :columns="columns"
                :loading="loading"
                :view-mode="true"
                :min-rows="20"
                storage-key="crm-visit-review-table-columns"
                global-config-key="crm-visit-review-table-columns"
              >
                <template #customerCell="{ record }">
                  <a-tooltip
                    v-if="!record.__ghost && record.customerName"
                    :title="record.customerName"
                    placement="bottom"
                  >
                    <span class="cell-ellipsis">{{ record.customerName }}</span>
                  </a-tooltip>
                  <span v-else-if="!record.__ghost">-</span>
                  <span v-else />
                </template>

                <template #visitTypeCell="{ record }">
                  <a-tag
                    v-if="!record.__ghost && record.visitType"
                    :color="VISIT_TYPE_MAP[record.visitType]?.color || 'default'"
                  >
                    {{ VISIT_TYPE_MAP[record.visitType]?.label || '-' }}
                  </a-tag>
                  <span v-else-if="!record.__ghost">-</span>
                  <span v-else />
                </template>

                <template #visitTimeCell="{ record }">
                  <span v-if="record.__ghost" />
                  <span v-else>{{ formatTime(record.visitTime) }}</span>
                </template>

                <template #locationCell="{ record }">
                  <a-tooltip
                    v-if="!record.__ghost && record.location"
                    :title="record.location"
                    placement="bottom"
                  >
                    <span class="cell-ellipsis">{{ record.location }}</span>
                  </a-tooltip>
                  <span v-else-if="!record.__ghost">-</span>
                  <span v-else />
                </template>

                <template #contentCell="{ record }">
                  <a-tooltip
                    v-if="!record.__ghost && record.content"
                    :title="record.content"
                    placement="bottom"
                  >
                    <span class="cell-ellipsis">{{ record.content }}</span>
                  </a-tooltip>
                  <span v-else-if="!record.__ghost">-</span>
                  <span v-else />
                </template>

                <template #resultCell="{ record }">
                  <a-tag
                    v-if="!record.__ghost && record.result"
                    :color="RESULT_MAP[record.result]?.color || 'default'"
                  >
                    {{ RESULT_MAP[record.result]?.label || '-' }}
                  </a-tag>
                  <span v-else-if="!record.__ghost">-</span>
                  <span v-else />
                </template>

                <template #nextActionCell="{ record }">
                  <a-tooltip
                    v-if="!record.__ghost && record.nextAction"
                    :title="record.nextAction"
                    placement="bottom"
                  >
                    <span class="cell-ellipsis">{{ record.nextAction }}</span>
                  </a-tooltip>
                  <span v-else-if="!record.__ghost">-</span>
                  <span v-else />
                </template>
              </BillDetailTable>
            </div>
          </div>
        </template>

        <!-- ═══ 经典分页栏 ═══ -->
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

      <!-- ═══ 页面配置（查询条件显隐 + 功能按钮启用） ═══ -->
      <PageConfigPanel
        :open="showPageConfig"
        :query-fields-config="queryFieldsConfig"
        :function-buttons-config="functionButtonConfig"
        :default-query-fields-config="DEFAULT_QUERY_FIELDS"
        :default-function-buttons-config="DEFAULT_FUNCTION_BUTTONS"
        :storage-key="pageConfigStorageKey"
        @update:open="showPageConfig = $event"
        @change="handlePageConfigChange"
      />
    </PageContainer>
    <!-- 打印：结果集打印 -->
    <PrintDialog
      ref="printDialogRef"
      page-code="sales-visit-review"
      :print-data="printData"
    />
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined,
  PrinterOutlined,
  DownloadOutlined,
  SettingOutlined,
  BarChartOutlined,
} from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import CategoryListLayout from '@/components/CategoryListLayout/CategoryListLayout.vue'
import BillDetailTable from '@/components/BillFormPage/BillDetailTable/index.vue'
import type { DetailColumnConfig } from '@/components/BillFormPage/BillDetailTable/types'
import StandardPagination from '@/components/Pagination/Pagination.vue'
import PageConfigPanel from '@/components/PageConfigPanel/index.vue'
import type { QueryFieldSetting, FunctionButtonSetting } from '@/components/PageConfigPanel/index.vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import ARReportChart from '@/components/ARReportChart/ARReportChart.vue'
import type { StatCardItem } from '@/components/ARReportPage/types'
import {
  visitReviewApi,
  type VisitReviewResponse, type VisitReviewSummary, type VisitStatsSummary, type VisitRecord
} from '@/api/crm'
import PrintDialog from '@/components/PrintDialog/index.vue'
import { useListPrint } from '@/composables/useListPrint'

defineOptions({ name: 'SalesVisitReview' })

// ═══ 拜访方式/结果（与后端 VisitRecord 注释一致，与「拜访执行」页同字典） ═══
const VISIT_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '上门', color: 'blue' },
  2: { label: '电话', color: 'cyan' },
  3: { label: '其他', color: 'default' }
}
const RESULT_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '有意向', color: 'green' },
  2: { label: '一般', color: 'orange' },
  3: { label: '无意向', color: 'red' }
}
/** 结果下拉选项由 RESULT_MAP 反推（字典单一真源） */
const resultOptions = Object.entries(RESULT_MAP).map(([value, v]) => ({
  label: v.label,
  value: Number(value)
}))

function formatTime(val: string | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

// ═══ 页面配置 ═══
const DEFAULT_QUERY_FIELDS: QueryFieldSetting[] = [
  { key: 'result', label: '拜访结果', visible: true },
  { key: 'visitDateRange', label: '拜访日期', visible: true }
]
const DEFAULT_FUNCTION_BUTTONS: FunctionButtonSetting[] = [
  { key: 'refresh', label: '刷新', enabled: true },
  { key: 'charts', label: '图表', enabled: true },
  { key: 'printF8', label: '打印(F8)', enabled: true },
  { key: 'export', label: '导出', enabled: true }
]
const showPageConfig = ref(false)
const queryFieldsConfig = ref<QueryFieldSetting[]>(DEFAULT_QUERY_FIELDS.map(f => ({ ...f })))
const functionButtonConfig = ref<FunctionButtonSetting[]>(DEFAULT_FUNCTION_BUTTONS.map(b => ({ ...b })))
const pageConfigStorageKey = 'crm-visit-review-page-config'

function isFieldVisible(key: string): boolean {
  const found = queryFieldsConfig.value.find(f => f.key === key)
  return found ? found.visible : true
}
function isButtonEnabled(key: string): boolean {
  const found = functionButtonConfig.value.find(b => b.key === key)
  return found ? found.enabled : true
}
function handlePageConfigChange(config: any) {
  if (config?.queryFields) queryFieldsConfig.value = config.queryFields
  if (config?.functionButtons) functionButtonConfig.value = config.functionButtons
}

// ═══ 查询条件（后端 review/page 仅接 result + 日期区间） ═══
const searchForm = reactive({
  result: undefined as number | undefined,
  visitDateRange: undefined as [string, string] | undefined,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const loading = ref(false)
const tableData = ref<VisitRecord[]>([])

// ═══ 表格列（rowNo 承载列配置齿轮；只读页无操作列） ═══
const columns: DetailColumnConfig[] = [
  { key: 'rowNo', title: '', type: 'rowNo', width: 40, fixed: 'left' },
  { key: 'customerName', title: '客户', type: 'slot', slotName: 'customerCell', width: 170 },
  { key: 'salesPersonName', title: '负责人', type: 'input', width: 100 },
  { key: 'visitType', title: '拜访方式', type: 'slot', slotName: 'visitTypeCell', width: 90 },
  { key: 'visitTime', title: '打卡时间', type: 'slot', slotName: 'visitTimeCell', width: 130 },
  { key: 'location', title: '拜访地点', type: 'slot', slotName: 'locationCell', width: 180 },
  { key: 'content', title: '拜访内容', type: 'slot', slotName: 'contentCell', width: 220 },
  { key: 'result', title: '拜访结果', type: 'slot', slotName: 'resultCell', width: 90 },
  { key: 'nextAction', title: '下一步行动', type: 'slot', slotName: 'nextActionCell', width: 150 },
  { key: 'nextVisitDate', title: '下次拜访', type: 'input', width: 110 },
]

// ═══ 统计（/crm/visit/stats/summary 全局口径 + /review/page 的 summary 随日期变化） ═══
const stats = ref<VisitStatsSummary | null>(null)
const reviewSummary = ref<VisitReviewSummary | null>(null)

const statCards = computed<StatCardItem[]>(() => [
  { label: '今日拜访', value: stats.value?.todayCount ?? 0, suffix: '次' },
  { label: '本周拜访', value: stats.value?.weekCount ?? 0, suffix: '次' },
  { label: '计划覆盖率', value: Number(stats.value?.coverage ?? 0), precision: 2, suffix: '%' },
  { label: '拜访总数', value: reviewSummary.value?.total ?? 0, suffix: '次' },
  { label: '有意向', value: reviewSummary.value?.interested ?? 0, suffix: '次', valueStyle: { color: '#52c41a' } },
  { label: '一般', value: reviewSummary.value?.normal ?? 0, suffix: '次', valueStyle: { color: '#faad14' } },
  { label: '无意向', value: reviewSummary.value?.noIntention ?? 0, suffix: '次', valueStyle: { color: '#ff4d4f' } }
])

/** 计划覆盖率的分子/分母（接口已返回，旧实现只用了 coverage，看不到 5/10 还是 1/2） */
const coverageDetail = computed(() => {
  const s = stats.value
  if (!s) return '—'
  return `已完成 ${s.planExecuted ?? 0} / 总数 ${s.planTotal ?? 0}`
})

/** 拜访转化率 = 有意向 / 拜访总数（数据已具备，无需新端点） */
const conversionRate = computed(() => {
  const total = reviewSummary.value?.total ?? 0
  if (!total) return '—'
  const rate = (Number(reviewSummary.value?.interested ?? 0) * 100) / total
  return `${rate.toFixed(2)}%`
})

// ═══ 趋势数据（byDate：后端已返回，旧实现从未使用） ═══
const trendData = ref<{ date: string; cnt: number }[]>([])

/**
 * byDate 归一化。
 * ⚠️ 后端 SQL 写的是 `AS visitDate`（未加双引号），PostgreSQL 会把标识符折叠为小写 `visitdate`；
 *    且 `visitdate` 无下划线，map-underscore-to-camel-case 不会转换 → 两种键名都要兜住，避免图表空白。
 */
function normalizeByDate(list: any): { date: string; cnt: number }[] {
  if (!Array.isArray(list)) return []
  return list
    .map((item: any) => {
      const raw = item?.visitDate ?? item?.visitdate ?? item?.VISITDATE
      return { date: String(raw ?? ''), cnt: Number(item?.cnt ?? item?.CNT ?? 0) || 0 }
    })
    .filter(p => p.date)
    // 后端按日期 DESC 返回，趋势图需按日期升序
    .sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : 0))
}

const trendChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 44, right: 16, top: 24, bottom: 30 },
  xAxis: { type: 'category', data: trendData.value.map(p => p.date), boundaryGap: true },
  yAxis: { type: 'value', minInterval: 1 },
  series: [{
    name: '拜访次数',
    type: 'bar',
    barMaxWidth: 28,
    itemStyle: { color: '#1890ff' },
    data: trendData.value.map(p => p.cnt)
  }]
}))

// 图表显隐（本地记忆，与费用统计页同口径）
const showCharts = ref(true)
function toggleCharts() {
  showCharts.value = !showCharts.value
  localStorage.setItem('crm-visit-review-chart-visible', showCharts.value ? '1' : '0')
}

// ═══ 数据加载（GET /crm/visit/review/page，裸 Map：summary + byDate + page） ═══
async function fetchData() {
  loading.value = true
  try {
    const res = (await visitReviewApi.reviewPage({
      page: pagination.current,
      size: pagination.pageSize,
      result: searchForm.result,
      visitDateStart: searchForm.visitDateRange?.[0],
      visitDateEnd: searchForm.visitDateRange?.[1],
    })) as VisitReviewResponse
    reviewSummary.value = res?.summary || null
    trendData.value = normalizeByDate(res?.byDate)
    tableData.value = res?.page?.records || []
    pagination.total = Number(res?.page?.total) || 0
  } catch (error: any) {
    console.error('[拜访检视] 加载失败', error)
    message.error(error?.message || '加载失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    stats.value = await visitReviewApi.statsSummary()
  } catch (e) {
    console.warn('[拜访检视] 统计汇总获取失败', e)
    message.warning('统计汇总加载失败，请稍后重试')
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  searchForm.result = undefined
  searchForm.visitDateRange = undefined
  pagination.current = 1
  fetchData()
}

function handleRefresh() {
  fetchData()
  loadStats()
}

function handlePageChange(page: number, pageSize: number) {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// ═══ 导出（套方式/结果的中文文案与时间格式） ═══
function exportCsv() {
  const rows = (tableData.value || []).filter((r: any) => !r.__ghost)
  if (!rows.length) {
    message.warning('没有可导出的数据')
    return
  }
  const headers = ['客户', '负责人', '拜访方式', '打卡时间', '拜访地点', '拜访内容', '拜访结果', '下一步行动', '下次拜访']
  const lines = rows.map((r: any) => [
    r.customerName, r.salesPersonName, VISIT_TYPE_MAP[r.visitType]?.label || '-',
    formatTime(r.visitTime), r.location, r.content,
    RESULT_MAP[r.result]?.label || '-', r.nextAction, r.nextVisitDate
  ])
  const escape = (v: any) => `"${String(v ?? '').replace(/"/g, '""')}"`
  const csv = '\uFEFF' + [headers, ...lines].map(row => row.map(escape).join(',')).join('\r\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `拜访检视_${dayjs().format('YYYYMMDD_HHmmss')}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

function handleExport() {
  exportCsv()
}

// ═══ 打印（结果集打印）：拜访检视 ═══
// 原先是自己拼 HTML + 浏览器打印，现在交给 PrintDialog：列与行由页面给，模板负责版式。
// 打印列与原来的表格逐列对齐（# 行号、拜访方式/结果中文都在 printRows 里先算好）。
const printColumns: any[] = [
  { title: '#', key: '__seq', width: 40, align: 'center' },
  { title: '客户', key: 'customerName' },
  { title: '负责人', key: 'salesPersonName' },
  { title: '拜访方式', key: 'visitTypeText' },
  { title: '打卡时间', key: 'visitTimeText' },
  { title: '拜访地点', key: 'location' },
  { title: '拜访内容', key: 'content' },
  { title: '拜访结果', key: 'resultText' },
  { title: '下一步行动', key: 'nextAction' },
]

/** 打印行：先把单元格文本按原打印口径算好，模板只负责排版 */
function printRows(): any[] {
  return (tableData.value || []).filter((r: any) => !r.__ghost).map((r: any, i: number) => ({
    __seq: i + 1,
    customerName: r.customerName || '',
    salesPersonName: r.salesPersonName || '',
    visitTypeText: VISIT_TYPE_MAP[r.visitType]?.label || '-',
    visitTimeText: formatTime(r.visitTime),
    location: r.location || '',
    content: r.content || '',
    resultText: RESULT_MAP[r.result]?.label || '-',
    nextAction: r.nextAction || '',
  }))
}

const { printDialogRef, printData, handlePrint } = useListPrint({
  pageCode: 'sales-visit-review',
  // 原打印抬头的统计值（记录数/拜访总数/计划覆盖率）并入标题：它们是整段统计，不属于明细列
  title: () => `拜访检视（记录数：${printRows().length}，拜访总数：${reviewSummary.value?.total ?? 0}，计划覆盖率：${coverageDetail.value}）`,
  columns: () => printColumns,
  rows: () => printRows(),
  emptyTip: '没有可打印的数据',
})

function handleF8Key(e: KeyboardEvent) {
  if ((e.key === 'F8' || e.code === 'F8') && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault()
    handlePrint()
  }
}

function handleError(error: Error) {
  console.error('[拜访检视] 页面错误', error)
  message.error(`页面错误: ${error.message}`)
}

// ═══ 初始化（旧实现 onMounted 与 @loaded 各请求一次 /stats/summary → 此处只请求一次） ═══
onMounted(() => {
  const flag = localStorage.getItem('crm-visit-review-chart-visible')
  if (flag !== null) showCharts.value = flag === '1'
  fetchData()
  loadStats()
  window.addEventListener('keydown', handleF8Key)
})
onBeforeUnmount(() => window.removeEventListener('keydown', handleF8Key))
</script>

<style scoped>
.search-area { padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; flex-shrink: 0; }
.search-grid { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.search-item { display: flex; align-items: center; gap: 6px; }
.search-item :deep(.ant-select) { min-width: 150px; }
.search-actions { margin-left: auto; }
.search-label { font-size: 13px; color: #666; white-space: nowrap; }
.toolbar-tip { font-size: 12px; color: #999; }

/* ═══ 复合页容器：统计卡 + 口径条 + 图表 + 表格（表格占剩余高度） ═══ */
.review-body { display: flex; flex-direction: column; height: 100%; min-height: 0; padding: 8px 12px 0; gap: 8px; }
.stats-cards { margin-bottom: 0; }
.stats-cards :deep(.ar-stat-card) { padding: 10px 16px; box-shadow: none; background: #fafafa; border: 1px solid #f0f0f0; }
.stats-cards :deep(.ar-stat-card__value) { font-size: 19px; }
.stats-cards :deep(.ar-stat-card__label) { margin-bottom: 4px; }

.stats-meta { display: flex; flex-wrap: wrap; align-items: center; gap: 16px; padding: 0 2px; font-size: 12px; flex-shrink: 0; }
.meta-item { display: flex; align-items: center; gap: 4px; }
.meta-label { color: #888; }
.meta-value { color: #262626; font-weight: 600; }
.meta-tip { color: #faad14; }

.chart-row { flex-shrink: 0; }
.chart-row :deep(.ar-report-chart) { box-shadow: none; border: 1px solid #f0f0f0; padding: 8px 12px; }

/* ⚠️ 必须是 flex 纵向容器：BillDetailTable 根元素为 flex:1，父级非 flex 时表格高度会塌陷为 0 */
.table-area { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; }
.cell-ellipsis { display: inline-block; max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; vertical-align: bottom; }

:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) { height: 28px; line-height: 28px; }
</style>
