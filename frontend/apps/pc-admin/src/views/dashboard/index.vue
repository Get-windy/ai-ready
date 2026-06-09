<template>
  <PageContainer body-padding>
    <!-- 骨架屏/内容 切换 -->
    <Transition name="dash-fade" mode="out-in">
      <SkeletonDashboard v-if="pageLoading" key="skeleton" />
      <div v-else key="content" class="dashboard-content">
        <!-- 面包屑导航 -->
        <a-breadcrumb class="dashboard-breadcrumb">
          <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
          <a-breadcrumb-item>工作台</a-breadcrumb-item>
        </a-breadcrumb>

        <!-- 欢迎栏 -->
        <div class="dashboard-header">
          <div>
            <h1 class="dashboard-title">工作台</h1>
            <p class="dashboard-subtitle">欢迎回来，这是您的业务概览</p>
          </div>
          <a-space>
            <span v-if="lastUpdated" class="update-timestamp" :title="lastUpdated">
              更新于: {{ lastUpdated }}
            </span>
            <template v-if="autoRefreshEnabled">
              <a-tooltip title="自动刷新中，点击关闭">
                <span class="auto-refresh-badge" @click="toggleAutoRefresh">
                  <SyncOutlined :spin="refreshLoading" /> {{ autoRefreshCountdown }}s
                </span>
              </a-tooltip>
            </template>
            <a-range-picker
              v-model:value="dateRange"
              size="small"
              style="width: 240px"
              :allow-clear="true"
              @change="handleDateChange"
            />
            <a-button
              size="small"
              :loading="refreshLoading"
              @click="handleRefresh"
            >刷新数据</a-button>
          </a-space>
        </div>

      <!-- KPI 卡片 -->
      <a-row :gutter="16" class="kpi-row">
        <a-col :xs="24" :sm="12" :md="8" :lg="6">
          <a-card class="kpi-card" :bordered="false">
            <template v-if="statsLoading">
              <a-skeleton active :paragraph="{ rows: 1 }" />
            </template>
            <template v-else-if="kpiError">
              <div class="kpi-error" @click="handleRefresh">加载失败，点击重试</div>
            </template>
            <a-statistic v-else title="今日销售额" :value="stats?.todaySales?.value ?? 0" prefix="¥" :precision="2" :value-style="{ color: '#3f8600' }">
              <template #suffix>
                <span v-if="stats?.todaySales" :class="`trend ${stats.todaySales.trendType}`">
                  {{ stats.todaySales.trendType === 'up' ? '↑' : stats.todaySales.trendType === 'down' ? '↓' : '' }}{{ stats.todaySales.trend }}%
                </span>
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :md="8" :lg="6">
          <a-card class="kpi-card" :bordered="false">
            <template v-if="statsLoading">
              <a-skeleton active :paragraph="{ rows: 1 }" />
            </template>
            <a-statistic v-else title="今日采购额" :value="stats?.todayPurchase?.value ?? 0" prefix="¥" :precision="2" :value-style="{ color: '#1890ff' }">
              <template #suffix>
                <span v-if="stats?.todayPurchase" :class="`trend ${stats.todayPurchase.trendType}`">
                  {{ stats.todayPurchase.trendType === 'up' ? '↑' : stats.todayPurchase.trendType === 'down' ? '↓' : '' }}{{ stats.todayPurchase.trend }}%
                </span>
              </template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :md="8" :lg="6">
          <a-card class="kpi-card" :bordered="false">
            <template v-if="statsLoading">
              <a-skeleton active :paragraph="{ rows: 1 }" />
            </template>
            <a-statistic v-else title="待审批单据" :value="stats?.pendingApprovals?.value ?? 0" :value-style="{ color: '#faad14' }">
              <template #suffix><span class="trend warn">需处理</span></template>
            </a-statistic>
          </a-card>
        </a-col>
        <a-col :xs="24" :sm="12" :md="8" :lg="6">
          <a-card class="kpi-card" :bordered="false">
            <template v-if="statsLoading">
              <a-skeleton active :paragraph="{ rows: 1 }" />
            </template>
            <a-statistic v-else title="库存预警项" :value="stats?.stockAlerts?.value ?? 0" :value-style="{ color: '#ff4d4f' }">
              <template #suffix>
                <span v-if="stats?.stockAlerts" :class="`trend ${stats.stockAlerts.trendType}`">
                  {{ stats.stockAlerts.trendType === 'down' ? '↓' : '' }}{{ stats.stockAlerts.trend }}项
                </span>
              </template>
            </a-statistic>
          </a-card>
        </a-col>
      </a-row>

      <!-- 图表 + 待办 -->
      <a-row :gutter="16" class="content-row">
        <a-col :xs="24" :md="16">
          <a-card :bordered="false" class="chart-card">
            <template #title><h2 class="card-heading">销售趋势 (近7天)</h2></template>
            <div ref="trendChartRef" class="chart-container">
              <div v-if="chartError" class="chart-error" @click="retryChart">
                <p>图表加载失败</p>
                <a-button size="small">点击重试</a-button>
              </div>
              <div v-else-if="chartEmpty" class="chart-empty">
                <BarChartOutlined style="font-size: 36px; color: #d9d9d9; margin-bottom: 8px;" />
                <p>暂无销售趋势数据</p>
              </div>
            </div>
          </a-card>
        </a-col>
        <a-col :xs="24" :md="8">
          <a-card :bordered="false" class="todo-card">
            <template #title><h2 class="card-heading">待办事项</h2></template>
            <template #extra><a @click="handleViewAll">全部</a></template>
            <a-list :data-source="todos" size="small" :locale="{ emptyText: '暂无待办事项' }">
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta>
                    <template #avatar>
                      <a-badge :status="item.type === 'approval' ? 'warning' : item.type === 'alert' ? 'error' : 'default'" />
                    </template>
                    <template #title>{{ item.title }}</template>
                    <template #description>{{ item.time }}</template>
                  </a-list-item-meta>
                </a-list-item>
              </template>
              <template #header>
                <div v-if="todos.length === 0" class="empty-guide">
                  <InboxOutlined style="margin-right: 6px; color: #909399;" />
                  暂无待办事项，您可以在采购/销售等模块创建单据来发起审批流程
                </div>
              </template>
            </a-list>
          </a-card>
        </a-col>
      </a-row>

      <!-- 快速入口 + 库存预警 -->
      <a-row :gutter="16" class="content-row">
        <a-col :xs="24" :md="12">
          <a-card :bordered="false">
            <template #title><h2 class="card-heading">快速入口</h2></template>
            <a-row :gutter="[16, 16]">
              <a-col :span="8" v-for="entry in quickEntries" :key="entry.key">
                <div
                  v-permission.disabled="entry.permission || ''"
                  class="quick-entry"
                  tabindex="0"
                  @click="handleQuickNav(entry.path)"
                  @keydown.enter="handleQuickNav(entry.path)"
                  @keydown.space.prevent="handleQuickNav(entry.path)"
                >
                  <component :is="entry.icon" class="quick-entry-icon" :style="{ color: entry.color }" />
                  <span class="quick-entry-label">{{ entry.label }}</span>
                </div>
              </a-col>
            </a-row>
          </a-card>
        </a-col>
        <a-col :xs="24" :md="12">
          <a-card :bordered="false">
            <template #title><h2 class="card-heading">库存预警</h2></template>
            <VxeTableList
              :columns="alertVxeColumns"
              :data-source="stockAlerts"
              :pagination="false"
              row-key="id"
              :show-toolbar="false"
              :selectable="false"
              :show-add="false"
              :show-search="false"
              :show-export="false"
              :show-batch-delete="false"
            >
              <template #levelCell="{ record }">
                <a-tag :color="record.level === 'high' ? 'red' : 'orange'">{{ record.level === 'high' ? '缺货' : '低库存' }}</a-tag>
              </template>
              <template #empty>
                <div class="stock-empty">
                  <CheckCircleOutlined style="margin-right: 6px; color: #52c41a;" />
                  所有库存均处于安全水平，无需处理
                </div>
              </template>
            </VxeTableList>
          </a-card>
        </a-col>
      </a-row>
      </div>
    </Transition>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ShoppingCartOutlined, ShoppingOutlined, ContainerOutlined,
  DollarOutlined, TeamOutlined, FileTextOutlined,
  SyncOutlined, InboxOutlined, CheckCircleOutlined, BarChartOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import { SkeletonDashboard } from '@/components/Skeleton'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { dashboardApi, type DashboardStats, type TrendChartData, type TodoItem, type StockAlertItem } from '@/api/dashboard'

const router = useRouter()

/** 页面级加载状态 */
const pageLoading = ref(true)
/** KPI 统计数据加载状态 */
const statsLoading = ref(true)
/** 刷新按钮 loading */
const refreshLoading = ref(false)
/** KPI 错误状态 */
const kpiError = ref(false)
/** 图表错误 */
const chartError = ref(false)
/** 图表数据为空（已加载但无数据） */
const chartEmpty = computed(() => {
  if (chartError.value) return false
  if (!trendData.value) return false
  return !trendData.value.categories?.length || !trendData.value.series?.length
})
/** 数据更新时间 */
const lastUpdated = ref('')

// ── 自动刷新 ──────────────────────────────
const autoRefreshEnabled = ref(true)
const autoRefreshInterval = 30000
const autoRefreshCountdown = ref(30)
let autoRefreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const dateRange = ref<[dayjs.Dayjs, dayjs.Dayjs] | null>(null)
const trendChartRef = ref<HTMLElement | null>(null)
let chartInstance: any = null

// ── 响应式数据（从 API 获取） ──────────────────────────
const stats = ref<DashboardStats | null>(null)
const todos = ref<TodoItem[]>([])
const stockAlerts = ref<StockAlertItem[]>([])
const trendData = ref<TrendChartData | null>(null)

// ── 快速入口（静态，不通过 API 获取）────────────────────
const quickEntries = [
  { key: 'purchase', label: '采购管理', icon: ShoppingCartOutlined, color: '#1890ff', path: '/purchase', permission: 'purchase:order:list' },
  { key: 'sale', label: '销售管理', icon: ShoppingOutlined, color: '#52c41a', path: '/sale', permission: 'sale:order:list' },
  { key: 'stock', label: '库存管理', icon: ContainerOutlined, color: '#faad14', path: '/stock', permission: 'stock:list' },
  { key: 'finance', label: '财务管理', icon: DollarOutlined, color: '#722ed1', path: '/finance', permission: 'finance:list' },
  { key: 'customer', label: '客户管理', icon: TeamOutlined, color: '#eb2f96', path: '/crm/customer', permission: 'crm:customer:list' },
  { key: 'order', label: '订单中心', icon: FileTextOutlined, color: '#13c2c2', path: '/order-center', permission: 'sale:order:list' }
]

// ── 库存预警表格列 ──────────────────────────────────────
const alertVxeColumns = [
  { field: 'code', title: '物料编码', width: 120 },
  { field: 'name', title: '物料名称', width: 120 },
  { field: 'current', title: '当前库存', width: 80, align: 'right' },
  { field: 'safe', title: '安全库存', width: 80, align: 'right' },
  { field: 'level', title: '状态', width: 80, slotName: 'levelCell', align: 'center' },
]

// ── 数据加载 ──────────────────────────────────────────

function updateTimestamp() {
  lastUpdated.value = dayjs().format('HH:mm:ss')
}

// ── 自动刷新 ──────────────────────────────

/** 构建日期查询参数 */
function getDateParams(): { startDate?: string; endDate?: string } | undefined {
  if (!dateRange.value?.[0] || !dateRange.value?.[1]) return undefined
  return {
    startDate: dateRange.value[0].format('YYYY-MM-DD'),
    endDate: dateRange.value[1].format('YYYY-MM-DD')
  }
}

function startAutoRefresh() {
  stopAutoRefresh()
  autoRefreshCountdown.value = autoRefreshInterval / 1000
  autoRefreshTimer = setInterval(() => {
    loadAllData(true)
    autoRefreshCountdown.value = autoRefreshInterval / 1000
  }, autoRefreshInterval)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
}

function stopAutoRefresh() {
  if (autoRefreshTimer) { clearInterval(autoRefreshTimer); autoRefreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
}

function toggleAutoRefresh() {
  autoRefreshEnabled.value = !autoRefreshEnabled.value
  if (autoRefreshEnabled.value) {
    startAutoRefresh()
  } else {
    stopAutoRefresh()
  }
}

const loadStats = async (dateParams?: { startDate?: string; endDate?: string }, silent = false) => {
  if (!silent) kpiError.value = false
  try {
    const res = await dashboardApi.getStats(dateParams)
    stats.value = res.data
  } catch (err: any) {
    console.warn('加载 KPI 数据失败:', err)
    if (!silent) {
      stats.value = null
      kpiError.value = true
    }
  }
}

const loadTodos = async () => {
  try {
    const res = await dashboardApi.getTodos()
    todos.value = res.data || []
  } catch (err: any) {
    console.warn('加载待办事项失败:', err)
    todos.value = []
  }
}

const loadAlerts = async () => {
  try {
    const res = await dashboardApi.getAlerts()
    stockAlerts.value = res.data || []
  } catch (err: any) {
    console.warn('加载库存预警失败:', err)
    stockAlerts.value = []
  }
}

const loadTrend = async (dateParams?: { startDate?: string; endDate?: string }, silent = false) => {
  if (!silent) chartError.value = false
  try {
    const res = await dashboardApi.getTrend(dateParams)
    trendData.value = res.data
  } catch (err: any) {
    console.warn('加载趋势数据失败:', err)
    if (!silent) {
      trendData.value = null
      chartError.value = true
    }
  }
}

const loadAllData = async (silent = false) => {
  const dateParams = getDateParams()

  if (!silent) {
    pageLoading.value = true
    statsLoading.value = true
  }

  // 各 API 独立加载，互不影响
  await Promise.allSettled([
    loadStats(dateParams, silent),
    loadTodos(),
    loadAlerts(),
    loadTrend(dateParams, silent)
  ])

  pageLoading.value = false
  statsLoading.value = false

  // 数据就绪后初始化图表
  await nextTick()
  if (!chartError.value && trendData.value) {
    initChart()
  }

  updateTimestamp()
}

// ── 日期联动 ──────────────────────────────────────────

function handleDateChange() {
  loadAllData(true)
}

// ── ECharts 趋势图 ────────────────────────────────────

const initChart = async () => {
  await nextTick()
  if (!trendChartRef.value) return

  const data = trendData.value
  if (!data) return

  try {
    const echartsModule: any = await import('echarts')
    const echartsInst = echartsModule.default || echartsModule
    if (!chartInstance) {
      chartInstance = echartsInst.init(trendChartRef.value)
    }
    chartInstance.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: data.series.map((s: any) => s.name), bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: data.categories },
      yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
      series: data.series.map((s: any, i: number) => ({
        name: s.name,
        type: 'line',
        smooth: true,
        data: s.data,
        itemStyle: { color: ['#1890ff', '#faad14', '#52c41a'][i] || '#1890ff' },
        areaStyle: {
          color: [
            'rgba(24,144,255,0.08)',
            'rgba(250,173,20,0.08)',
            'rgba(82,196,26,0.08)'
          ][i] || 'rgba(24,144,255,0.08)'
        }
      }))
    })
  } catch {
    chartError.value = true
  }
}

const retryChart = async () => {
  chartError.value = false
  if (trendChartRef.value) {
    trendChartRef.value.innerHTML = ''
  }
  await loadTrend()
  if (!chartError.value) {
    await nextTick()
    initChart()
  }
}

// ── 操作 ──────────────────────────────────────────────

const handleRefresh = async () => {
  refreshLoading.value = true
  await loadAllData()
  refreshLoading.value = false
  message.success('数据已刷新')
}

const handleViewAll = () => router.push('/notification')
const handleQuickNav = (path: string) => router.push(path)

// ── 生命周期 ──────────────────────────────────────────

const handleResize = () => {
  chartInstance?.resize()
}

onMounted(() => {
  loadAllData()
  if (autoRefreshEnabled.value) {
    startAutoRefresh()
  }
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  stopAutoRefresh()
  chartInstance?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
/* ── 过渡动画 ── */
.dash-fade-enter-active,
.dash-fade-leave-active {
  transition: opacity 0.25s ease;
}
.dash-fade-enter-from,
.dash-fade-leave-to {
  opacity: 0;
}

.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: 0;
  flex: 1;
  overflow: auto;
}

.dashboard-breadcrumb {
  margin-bottom: 12px;
  font-size: 13px;
}
.dashboard-breadcrumb :deep(li) {
  font-size: 13px;
}

.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  cursor: pointer;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
  transition: all 0.2s;
}
.auto-refresh-badge:hover {
  color: #409eff;
  background: #ecf5ff;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
  flex-shrink: 0;
}

.dashboard-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.dashboard-subtitle {
  color: #909399;
  margin: 4px 0 0;
  font-size: 13px;
}

.update-timestamp {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.kpi-row { margin-bottom: 16px; }
.kpi-card { border-radius: 8px; cursor: default; }
.kpi-card :deep(.ant-card-body) { padding: 20px 24px; }

.kpi-error {
  padding: 8px 0;
  color: #ff4d4f;
  font-size: 13px;
  cursor: pointer;
  text-align: center;
}
.kpi-error:hover { color: #cf1322; }

.trend { font-size: 13px; margin-left: 8px; }
.trend.up { color: #52c41a; }
.trend.down { color: #ff4d4f; }
.trend.warn { color: #faad14; }

.content-row { margin-bottom: 16px; }

.chart-card, .todo-card { border-radius: 8px; }
.chart-card :deep(.ant-card-body), .todo-card :deep(.ant-card-body) {
  padding: 16px 24px;
}

.chart-container {
  height: 300px;
  position: relative;
}

.chart-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  gap: 8px;
}
.chart-error p { margin: 0; }

.chart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
}

.card-heading {
  font-size: 14px;
  font-weight: 600;
  margin: 0;
  color: #303133;
}

.quick-entry {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
}
.quick-entry:hover {
  background: #ecf5ff;
  border-color: #b3d8ff;
  transform: translateY(-2px);
}
.quick-entry:focus-visible {
  outline: 2px solid #409eff;
  outline-offset: 2px;
}
.quick-entry-icon { font-size: 28px; margin-bottom: 8px; }
.quick-entry-label { font-size: 13px; color: #606266; }

.empty-guide {
  padding: 24px 0;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

.stock-empty {
  padding: 24px 0;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

@media (max-width: 768px) {
  .dashboard-header { flex-direction: column; align-items: flex-start; }
  .kpi-card :deep(.ant-card-body) { padding: 16px; }
}
</style>
