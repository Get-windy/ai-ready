<template>
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
        <a-card class="kpi-card" :bordered="false" role="region" aria-label="今日销售额：125,680.00 元，较昨日上涨 12%">
          <a-statistic title="今日销售额" :value="125680" prefix="¥" :precision="2" :value-style="{ color: '#3f8600' }">
            <template #suffix><span class="trend up" aria-hidden="true">↑12%</span></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :md="8" :lg="6">
        <a-card class="kpi-card" :bordered="false" role="region" aria-label="今日采购额：89,420.00 元，较昨日上涨 8%">
          <a-statistic title="今日采购额" :value="89420" prefix="¥" :precision="2" :value-style="{ color: '#1890ff' }">
            <template #suffix><span class="trend up" aria-hidden="true">↑8%</span></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :md="8" :lg="6">
        <a-card class="kpi-card" :bordered="false" role="region" aria-label="待审批单据：23 项，需要处理">
          <a-statistic title="待审批单据" :value="23" :value-style="{ color: '#faad14' }">
            <template #suffix><span class="trend warn" aria-hidden="true">需处理</span></template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :md="8" :lg="6">
        <a-card class="kpi-card" :bordered="false" role="region" aria-label="库存预警项：7 项，较上次减少 3 项">
          <a-statistic title="库存预警项" :value="7" :value-style="{ color: '#ff4d4f' }">
            <template #suffix><span class="trend down" aria-hidden="true">↓3项</span></template>
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

const router = useRouter()

/** 页面级加载状态 */
const pageLoading = ref(true)

const dateRange = ref<any>(null)
const trendChartRef = ref<HTMLElement | null>(null)
let chartInstance: any = null

// ── 待办 ──────────────────────────────────────────────
const todos = [
  { id: 1, title: '采购订单 PO-2024-0128 待审批', time: '10分钟前', type: 'approval' },
  { id: 2, title: '销售退货 SR-2024-0056 待处理', time: '30分钟前', type: 'approval' },
  { id: 3, title: '库存预警：物料 M08001 低于安全库存', time: '1小时前', type: 'alert' },
  { id: 4, title: '付款申请 PAY-2024-0089 待审核', time: '2小时前', type: 'approval' },
  { id: 5, title: '客户 深圳科技 信用额度即将超限', time: '3小时前', type: 'alert' }
]

// ── 快速入口 ──────────────────────────────────────────
const quickEntries = [
  { key: 'purchase', label: '采购管理', icon: ShoppingCartOutlined, color: '#1890ff', path: '/purchase' },
  { key: 'sale', label: '销售管理', icon: ShoppingOutlined, color: '#52c41a', path: '/sale' },
  { key: 'stock', label: '库存管理', icon: ContainerOutlined, color: '#faad14', path: '/stock' },
  { key: 'finance', label: '财务管理', icon: DollarOutlined, color: '#722ed1', path: '/finance' },
  { key: 'customer', label: '客户管理', icon: TeamOutlined, color: '#eb2f96', path: '/crm/customer' },
  { key: 'order', label: '订单中心', icon: FileTextOutlined, color: '#13c2c2', path: '/order-center' }
]

// ── 库存预警 ──────────────────────────────────────────
const alertColumns = [
  { title: '物料编码', dataIndex: 'code', key: 'code', width: 120 },
  { title: '物料名称', dataIndex: 'name', key: 'name' },
  { title: '当前库存', dataIndex: 'current', key: 'current', width: 80 },
  { title: '安全库存', dataIndex: 'safe', key: 'safe', width: 80 },
  { title: '状态', key: 'level', width: 80 }
]

const stockAlerts = [
  { id: 1, code: 'M08001', name: '电子元器件A', current: 12, safe: 50, level: 'high' },
  { id: 2, code: 'M08015', name: '包装材料B', current: 8, safe: 30, level: 'high' },
  { id: 3, code: 'M08023', name: '五金配件C', current: 45, safe: 100, level: 'low' },
  { id: 4, code: 'M08042', name: '橡胶密封圈D', current: 22, safe: 60, level: 'low' }
]

// ── ECharts 趋势图 ────────────────────────────────────
const initChart = async () => {
  await nextTick()
  if (!trendChartRef.value) return

  try {
    const echartsModule: any = await import('echarts')
    const echartsInst = echartsModule.default || echartsModule
    chartInstance = echartsInst.init(trendChartRef.value)
    chartInstance.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['销售额', '采购额', '利润'], bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
      xAxis: { type: 'category', boundaryGap: false, data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
      yAxis: { type: 'value', axisLabel: { formatter: '¥{value}' } },
      series: [
        { name: '销售额', type: 'line', smooth: true, data: [18200, 23400, 16200, 28400, 22100, 19300, 25800], itemStyle: { color: '#1890ff' }, areaStyle: { color: 'rgba(24,144,255,0.08)' } },
        { name: '采购额', type: 'line', smooth: true, data: [12300, 15800, 11200, 19200, 14500, 13800, 17600], itemStyle: { color: '#faad14' }, areaStyle: { color: 'rgba(250,173,20,0.08)' } },
        { name: '利润', type: 'line', smooth: true, data: [5900, 7600, 5000, 9200, 7600, 5500, 8200], itemStyle: { color: '#52c41a' }, areaStyle: { color: 'rgba(82,196,26,0.08)' } }
      ]
    })
  } catch {
    // ECharts not available - show fallback
    if (trendChartRef.value) {
      trendChartRef.value.innerHTML = '<div style="display:flex;align-items:center;justify-content:center;height:100%;color:#999">图表加载中...</div>'
    }
  }
}

// ── 操作 ──────────────────────────────────────────────
const handleRefresh = () => { pageLoading.value = true; setTimeout(() => { pageLoading.value = false; nextTick(() => initChart()); }, 800); message.success('数据已刷新'); }
const handleViewAll = () => router.push('/notification')
const handleQuickNav = (path: string) => router.push(path)

onMounted(() => {
  // 模拟数据加载（后续替换为真实 API 调用）
  setTimeout(() => {
    pageLoading.value = false
    nextTick(() => initChart())
  }, 1200)
})
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

@media (max-width: 768px) {
  .dashboard { padding: var(--spacing-lg); }
  .dashboard-header { flex-direction: column; align-items: flex-start; }
  .kpi-card :deep(.ant-card-body) { padding: var(--spacing-lg); }
}
</style>
