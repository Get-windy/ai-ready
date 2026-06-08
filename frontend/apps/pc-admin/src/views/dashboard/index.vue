<template>
  <div class="dashboard-root">
    <!-- 骨架屏加载状态 -->
    <SkeletonDashboard v-if="pageLoading" />

    <!-- 已加载状态 -->
    <div v-else class="dashboard">
    <!-- 欢迎栏 -->
    <div class="dashboard-header">
      <div>
        <h1 class="dashboard-title">工作台</h1>
        <p class="dashboard-subtitle">欢迎回来，这是您的业务概览</p>
      </div>
      <a-space>
        <a-range-picker v-model:value="dateRange" size="small" style="width: 240px" aria-label="选择日期范围" />
        <a-button size="small" aria-label="刷新数据" @click="handleRefresh">刷新数据</a-button>
      </a-space>
    </div>

    <!-- KPI 卡片 -->
    <a-row :gutter="16" class="kpi-row">
      <a-col :xs="24" :sm="12" :md="8" :lg="6">
        <a-card class="kpi-card" :bordered="false" role="region" :aria-label="`今日销售额：${stats?.todaySales?.value?.toLocaleString() ?? '--'} 元`">
          <a-skeleton v-if="statsLoading" active :paragraph="false" />
          <a-statistic v-else title="今日销售额" :value="stats?.todaySales?.value ?? 0" prefix="¥" :precision="2" :value-style="{ color: '#3f8600' }">
            <template #suffix>
              <span v-if="stats?.todaySales" :class="`trend ${stats.todaySales.trendType}`" aria-hidden="true">
                {{ stats.todaySales.trendType === 'up' ? '↑' : stats.todaySales.trendType === 'down' ? '↓' : '' }}{{ stats.todaySales.trend }}%
              </span>
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :md="8" :lg="6">
        <a-card class="kpi-card" :bordered="false" role="region" :aria-label="`今日采购额：${stats?.todayPurchase?.value?.toLocaleString() ?? '--'} 元`">
          <a-skeleton v-if="statsLoading" active :paragraph="false" />
          <a-statistic v-else title="今日采购额" :value="stats?.todayPurchase?.value ?? 0" prefix="¥" :precision="2" :value-style="{ color: '#1890ff' }">
            <template #suffix>
              <span v-if="stats?.todayPurchase" :class="`trend ${stats.todayPurchase.trendType}`" aria-hidden="true">
                {{ stats.todayPurchase.trendType === 'up' ? '↑' : stats.todayPurchase.trendType === 'down' ? '↓' : '' }}{{ stats.todayPurchase.trend }}%
              </span>
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :md="8" :lg="6">
        <a-card class="kpi-card" :bordered="false" role="region" :aria-label="`待审批单据：${stats?.pendingApprovals?.value ?? '--'} 项`">
          <a-skeleton v-if="statsLoading" active :paragraph="false" />
          <a-statistic v-else title="待审批单据" :value="stats?.pendingApprovals?.value ?? 0" :value-style="{ color: '#faad14' }">
            <template #suffix><span class="trend warn" aria-hidden="true">需处理</span></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :md="8" :lg="6">
        <a-card class="kpi-card" :bordered="false" role="region" :aria-label="`库存预警项：${stats?.stockAlerts?.value ?? '--'} 项`">
          <a-skeleton v-if="statsLoading" active :paragraph="false" />
          <a-statistic v-else title="库存预警项" :value="stats?.stockAlerts?.value ?? 0" :value-style="{ color: '#ff4d4f' }">
            <template #suffix>
              <span v-if="stats?.stockAlerts" :class="`trend ${stats.stockAlerts.trendType}`" aria-hidden="true">
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
          <div ref="trendChartRef" style="height: 300px" role="img" aria-label="销售趋势折线图，展示近7天销售额、采购额和利润数据" />
        </a-card>
      </a-col>
      <a-col :xs="24" :md="8">
        <a-card :bordered="false" class="todo-card">
          <template #title><h2 class="card-heading">待办事项</h2></template>
          <template #extra><a role="button" aria-label="查看全部待办事项" @click="handleViewAll">全部</a></template>
          <a-list :data-source="todos" size="small">
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
                class="quick-entry"
                role="button"
                :aria-label="`${entry.label} 快速入口`"
                tabindex="0"
                @click="handleQuickNav(entry.path)"
                @keydown.enter="handleQuickNav(entry.path)"
                @keydown.space.prevent="handleQuickNav(entry.path)"
              >
                <component :is="entry.icon" class="quick-entry-icon" :style="{ color: entry.color }" aria-hidden="true" />
                <span class="quick-entry-label">{{ entry.label }}</span>
              </div>
            </a-col>
          </a-row>
        </a-card>
      </a-col>
      <a-col :xs="24" :md="12">
        <a-card :bordered="false">
          <template #title><h2 class="card-heading">库存预警</h2></template>
          <template #extra><a-badge count="7" /></template>
          <a-table :columns="alertColumns" :data-source="stockAlerts" :pagination="false" size="small" row-key="id">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'level'">
                <a-tag :color="record.level === 'high' ? 'red' : 'orange'">{{ record.level === 'high' ? '缺货' : '低库存' }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick, h } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  ShoppingCartOutlined, ShoppingOutlined, ContainerOutlined,
  DollarOutlined, TeamOutlined, FileTextOutlined
} from '@ant-design/icons-vue'
import { SkeletonDashboard } from '@/components/Skeleton'
import { dashboardApi, type DashboardStats, type TrendChartData, type TodoItem, type StockAlertItem } from '@/api/dashboard'

const router = useRouter()

/** 页面级加载状态 */
const pageLoading = ref(true)
/** KPI 统计数据加载状态 */
const statsLoading = ref(true)

const dateRange = ref<any>(null)
const trendChartRef = ref<HTMLElement | null>(null)
let chartInstance: any = null

// ── 响应式数据（从 API 获取） ──────────────────────────
const stats = ref<DashboardStats | null>(null)
const todos = ref<TodoItem[]>([])
const stockAlerts = ref<StockAlertItem[]>([])
const trendData = ref<TrendChartData | null>(null)

// ── 快速入口（静态，不通过 API 获取）────────────────────
const quickEntries = [
  { key: 'purchase', label: '采购管理', icon: ShoppingCartOutlined, color: '#1890ff', path: '/purchase' },
  { key: 'sale', label: '销售管理', icon: ShoppingOutlined, color: '#52c41a', path: '/sale' },
  { key: 'stock', label: '库存管理', icon: ContainerOutlined, color: '#faad14', path: '/stock' },
  { key: 'finance', label: '财务管理', icon: DollarOutlined, color: '#722ed1', path: '/finance' },
  { key: 'customer', label: '客户管理', icon: TeamOutlined, color: '#eb2f96', path: '/crm/customer' },
  { key: 'order', label: '订单中心', icon: FileTextOutlined, color: '#13c2c2', path: '/order-center' }
]

// ── 库存预警表格列 ──────────────────────────────────────
const alertColumns = [
  { title: '物料编码', dataIndex: 'code', key: 'code', width: 120 },
  { title: '物料名称', dataIndex: 'name', key: 'name' },
  { title: '当前库存', dataIndex: 'current', key: 'current', width: 80 },
  { title: '安全库存', dataIndex: 'safe', key: 'safe', width: 80 },
  { title: '状态', key: 'level', width: 80 }
]

// ── 数据加载 ──────────────────────────────────────────

const loadStats = async () => {
  try {
    const res = await dashboardApi.getStats()
    stats.value = res.data
  } catch (err: any) {
    console.error('加载 KPI 数据失败:', err)
    stats.value = null
  } finally {
    statsLoading.value = false
  }
}

const loadTodos = async () => {
  try {
    const res = await dashboardApi.getTodos()
    todos.value = res.data || []
  } catch (err: any) {
    console.error('加载待办事项失败:', err)
    todos.value = []
  }
}

const loadAlerts = async () => {
  try {
    const res = await dashboardApi.getAlerts()
    stockAlerts.value = res.data || []
  } catch (err: any) {
    console.error('加载库存预警失败:', err)
    stockAlerts.value = []
  }
}

const loadTrend = async () => {
  try {
    const res = await dashboardApi.getTrend()
    trendData.value = res.data
  } catch (err: any) {
    console.error('加载趋势数据失败:', err)
    trendData.value = null
  }
}

const loadAllData = async () => {
  pageLoading.value = true
  statsLoading.value = true

  // 各 API 独立加载，互不影响
  await Promise.allSettled([loadStats(), loadTodos(), loadAlerts(), loadTrend()])

  pageLoading.value = false
  // 数据就绪后初始化图表
  await nextTick()
  initChart()
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
    if (trendChartRef.value) {
      trendChartRef.value.innerHTML = '<div style="display:flex;align-items:center;justify-content:center;height:100%;color:#999">图表加载中...</div>'
    }
  }
}

// ── 操作 ──────────────────────────────────────────────

const handleRefresh = async () => { await loadAllData(); message.success('数据已刷新') }
const handleViewAll = () => router.push('/notification')
const handleQuickNav = (path: string) => router.push(path)

// ── 生命周期 ──────────────────────────────────────────

onMounted(() => { loadAllData() })
onBeforeUnmount(() => { chartInstance?.dispose() })
</script>

<style scoped>
.dashboard { padding: var(--spacing-xxl); background-color: var(--color-bg-layout); min-height: 100%; }
.dashboard-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--spacing-xxl); flex-wrap: wrap; gap: var(--spacing-md); }
.dashboard-title { font-size: var(--font-size-h3); font-weight: var(--font-weight-semibold); color: var(--color-text-primary); margin: 0; }
.dashboard-subtitle { color: var(--color-text-tertiary); margin: var(--spacing-xs) 0 0; font-size: var(--font-size-sm); }

.kpi-row { margin-bottom: var(--spacing-lg); }
.kpi-card { border-radius: var(--border-radius-lg); transition: box-shadow var(--motion-duration-base); cursor: default; }
.kpi-card :deep(.ant-card-body) { padding: var(--spacing-xl) var(--spacing-xxl); }

.trend { font-size: var(--font-size-sm); margin-left: var(--spacing-sm); }
.trend.up { color: var(--color-success); }
.trend.down { color: var(--color-danger); }
.trend.warn { color: var(--color-warning); }

.content-row { margin-bottom: var(--spacing-lg); }
.chart-card, .todo-card { border-radius: var(--border-radius-lg); height: 380px; }
.chart-card :deep(.ant-card-body), .todo-card :deep(.ant-card-body) { padding: var(--spacing-lg) var(--spacing-xxl); }

.card-heading { font-size: var(--font-size-base); font-weight: var(--font-weight-semibold); margin: 0; color: var(--color-text-primary); }
.quick-entry { display: flex; flex-direction: column; align-items: center; padding: var(--spacing-lg) var(--spacing-sm); border-radius: var(--border-radius-lg); cursor: pointer; transition: all var(--motion-duration-fast); background: var(--color-bg-layout); border: 1px solid var(--color-border-light); }
.quick-entry:hover { background: var(--color-primary-bg); border-color: var(--color-primary-border); transform: translateY(-2px); }
.quick-entry:focus-visible { outline: 2px solid var(--color-primary); outline-offset: 2px; }
.quick-entry-icon { font-size: 28px; margin-bottom: var(--spacing-sm); }
.quick-entry-label { font-size: var(--font-size-sm); color: var(--color-text-secondary); }

/* ── 库存预警表格网格边框 ──────────────────────────────── */
:deep(.ant-table-thead > tr > th) {
  border-top: 2px solid #d9d9d9 !important;
  border-right: 1px solid #d9d9d9 !important;
  border-bottom: 2px solid #d9d9d9 !important;
  background: #fafafa !important;
  padding: 12px 16px !important;
  font-weight: 600 !important;
}

:deep(.ant-table-thead > tr > th:first-child) {
  border-left: 2px solid #d9d9d9 !important;
}

:deep(.ant-table-tbody > tr > td) {
  border-right: 1px solid #e8e8e8 !important;
  border-bottom: 1px solid #e8e8e8 !important;
  padding: 12px 16px !important;
}

:deep(.ant-table-tbody > tr > td:first-child) {
  border-left: 1px solid #e8e8e8 !important;
}

@media (max-width: 768px) {
  .dashboard { padding: var(--spacing-lg); }
  .dashboard-header { flex-direction: column; align-items: flex-start; }
  .kpi-card :deep(.ant-card-body) { padding: var(--spacing-lg); }
}
</style>
