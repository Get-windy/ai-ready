<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="sales-analysis-header">
        <div class="sales-analysis-header-left">
          <a-breadcrumb class="sales-analysis-breadcrumb">
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>销售分析</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="sales-analysis-header-title">销售分析</h2>
        </div>
        <div class="sales-analysis-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', handleRefresh)">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          </span>
        </div>
      </div>
    </template>

    <!-- 骨架加载 -->
    <div v-if="loading" class="skeleton-loading">
      <a-skeleton :paragraph="{ rows: 2 }" active />
      <div style="height: 16px" />
      <a-row :gutter="16">
        <a-col :span="6"><a-card><a-skeleton active /></a-card></a-col>
        <a-col :span="6"><a-card><a-skeleton active /></a-card></a-col>
        <a-col :span="6"><a-card><a-skeleton active /></a-card></a-col>
        <a-col :span="6"><a-card><a-skeleton active /></a-card></a-col>
      </a-row>
      <div style="height: 16px" />
      <a-skeleton :paragraph="{ rows: 6 }" active />
    </div>

    <template v-if="!loading">
    <!-- 筛选条件 -->
    <a-card size="small" class="filter-card">
      <a-space wrap>
        <a-date-picker v-model:value="dateRange[0]" picker="month" placeholder="开始月份" size="small" style="width: 140px" @change="handleFilterChange" />
        <a-date-picker v-model:value="dateRange[1]" picker="month" placeholder="结束月份" size="small" style="width: 140px" @change="handleFilterChange" />
        <a-select v-model:value="warehouseId" placeholder="选择仓库" allow-clear size="small" style="width: 160px" @change="handleFilterChange">
          <a-select-option v-for="w in warehouses" :key="w.id" :value="w.id">{{ w.name }}</a-select-option>
        </a-select>
      </a-space>
    </a-card>

    <!-- KPI 卡片 -->
    <a-row :gutter="16" class="stat-row">
      <a-col :span="6">
        <div class="stat-card stat-card--blue">
          <div class="stat-card-icon"><DollarOutlined /></div>
          <div class="stat-card-content">
            <div class="stat-card-title">销售总额</div>
            <div class="stat-card-value">{{ overview?.totalAmount ? formatAmount(overview.totalAmount) : '¥0.00' }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="stat-card stat-card--green">
          <div class="stat-card-icon"><FileTextOutlined /></div>
          <div class="stat-card-content">
            <div class="stat-card-title">订单总数</div>
            <div class="stat-card-value">{{ overview?.orderCount || 0 }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="stat-card stat-card--orange">
          <div class="stat-card-icon"><BarChartOutlined /></div>
          <div class="stat-card-content">
            <div class="stat-card-title">平均客单价</div>
            <div class="stat-card-value">{{ overview?.avgOrderAmount ? formatAmount(overview.avgOrderAmount) : '¥0.00' }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="stat-card stat-card--purple">
          <div class="stat-card-icon"><PercentageOutlined /></div>
          <div class="stat-card-content">
            <div class="stat-card-title">同比增长</div>
            <div class="stat-card-value">{{ overview?.grossMargin ? (overview.grossMargin * 100).toFixed(1) + '%' : '0.0%' }}</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <!-- 图表区域 -->
    <a-row :gutter="16" class="chart-row">
      <a-col :span="16">
        <a-card title="销售趋势" class="chart-card">
          <div ref="trendChartRef" style="height: 280px"></div>
          <div v-if="trendError" class="chart-empty chart-error" @click="loadTrend(buildParams())">
            <WarningOutlined /> 趋势数据加载失败，点击重试
          </div>
          <div v-if="!trendData.length && !trendError" class="chart-empty">暂无趋势数据</div>
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="渠道分布" class="chart-card">
          <div ref="pieChartRef" style="height: 280px"></div>
          <div v-if="channelError" class="chart-empty chart-error" @click="loadChannel(buildParams())">
            <WarningOutlined /> 渠道数据加载失败，点击重试
          </div>
          <div v-if="!channelData.length && !channelError" class="chart-empty">暂无渠道数据</div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 排行榜 -->
    <a-row :gutter="16" class="rank-row">
      <a-col :span="12">
        <a-card title="客户排行 TOP10" class="rank-card">
          <VxeTableList
            :data-source="customerRankData"
            :columns="customerRankVxeCols"
            :loading="rankLoading"
            :pagination="false as any"
            row-key="rank"
            :show-toolbar="false"
            :selectable="false"
            :show-add="false"
            :show-search="false"
            :show-export="false"
            :show-batch-delete="false"
          >
            <template #rankCell="{ record }">
              <span :class="['rank-badge', record.rank <= 3 ? 'rank-top' : '']">{{ record.rank }}</span>
            </template>
            <template #totalAmountCell="{ record }">
              <span class="amount-cell">{{ formatAmount(record.totalAmount) }}</span>
            </template>
            <template #growthCell="{ record }">
              <span :style="{ color: record.growth >= 0 ? '#52c41a' : '#ff4d4f' }">
                {{ record.growth >= 0 ? '+' : '' }}{{ (record.growth * 100).toFixed(1) }}%
              </span>
            </template>
          </VxeTableList>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="产品排行 TOP10" class="rank-card">
          <VxeTableList
            :data-source="productRankData"
            :columns="productRankVxeCols"
            :loading="rankLoading"
            :pagination="false as any"
            row-key="rank"
            :show-toolbar="false"
            :selectable="false"
            :show-add="false"
            :show-search="false"
            :show-export="false"
            :show-batch-delete="false"
          >
            <template #rankCell="{ record }">
              <span :class="['rank-badge', record.rank <= 3 ? 'rank-top' : '']">{{ record.rank }}</span>
            </template>
            <template #totalAmountCell="{ record }">
              <span class="amount-cell">{{ formatAmount(record.totalAmount) }}</span>
            </template>
            <template #marginCell="{ record }">
              <span>{{ (record.margin * 100).toFixed(1) }}%</span>
            </template>
          </VxeTableList>
        </a-card>
      </a-col>
    </a-row>
  </template>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { DollarOutlined, FileTextOutlined, BarChartOutlined, PercentageOutlined, ReloadOutlined, SyncOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { salesAnalysisApi, type SalesOverview, type TrendDataPoint, type ChannelDistribution, type CustomerRankItem, type ProductRankItem } from '@/api/sales-analysis'

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

// ── 筛选 ──
const dateRange = ref<any[]>([])
const warehouseId = ref<number | undefined>(undefined)
const warehouses = ref<{ id: number; name: string }[]>([])

// ── KPI ──
const overview = ref<SalesOverview | null>(null)
const overviewLoading = ref(false)

// ── 趋势图 ──
const trendData = ref<TrendDataPoint[]>([])
const trendChartRef = ref<HTMLElement | null>(null)
let trendChartInstance: any = null
const trendError = ref(false)

// ── 渠道饼图 ──
const channelData = ref<ChannelDistribution[]>([])
const pieChartRef = ref<HTMLElement | null>(null)
let pieChartInstance: any = null
const channelError = ref(false)

// ── 排行榜 ──
const customerRankData = ref<CustomerRankItem[]>([])
const productRankData = ref<ProductRankItem[]>([])
const rankLoading = ref(false)

// ── 页面状态 ──
const loading = ref(false)
const refreshLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)

const customerRankVxeCols = [
  { field: 'rank', title: '排名', width: 60, slotName: 'rankCell' },
  { field: 'name', title: '客户名称', width: 150 },
  { field: 'orderCount', title: '订单数', width: 80 },
  { field: 'totalAmount', title: '销售额', width: 130, align: 'right', slotName: 'totalAmountCell' },
  { field: 'growth', title: '增长率', width: 100, align: 'right', slotName: 'growthCell' },
]

const productRankVxeCols = [
  { field: 'rank', title: '排名', width: 60, slotName: 'rankCell' },
  { field: 'name', title: '产品名称', width: 150 },
  { field: 'volume', title: '销量', width: 80 },
  { field: 'totalAmount', title: '销售额', width: 130, align: 'right', slotName: 'totalAmountCell' },
  { field: 'margin', title: '利润率', width: 90, align: 'right', slotName: 'marginCell' },
]

// ── 辅助函数 ────────────────────────────────────────────────
function formatAmount(value: number): string {
  return `¥${value.toFixed(2)}`
}

// ── 筛选参数 ──
function buildParams() {
  const params: any = {}
  if (dateRange.value?.length === 2) {
    params.dateRange = dateRange.value
  }
  if (warehouseId.value) {
    params.warehouseId = warehouseId.value
  }
  return params
}

function handleFilterChange() {
  debounceClick('query', loadAll)
}

function handleRefresh() {
  refreshLoading.value = true
  loadAll()
}

// ── 数据加载 ──
async function loadAll() {
  loading.value = true
  const params = buildParams()
  await Promise.allSettled([
    loadOverview(params),
    loadTrend(params),
    loadChannel(params),
    loadRankings(params)
  ])
  loading.value = false
  refreshLoading.value = false
  lastUpdateTime.value = dayjs().format('HH:mm:ss')
  await nextTick()
  initCharts()
}

async function loadOverview(params: any) {
  overviewLoading.value = true
  try {
    const res = await salesAnalysisApi.getOverview(params)
    overview.value = res.data || null
  } catch (err) {
    overview.value = null
    console.warn('[销售分析] 加载销售概览失败', err)
  } finally {
    overviewLoading.value = false
  }
}

async function loadTrend(params: any) {
  trendError.value = false
  try {
    const res = await salesAnalysisApi.getTrend(params)
    trendData.value = res.data || []
  } catch (err) {
    trendData.value = []
    trendError.value = true
    console.warn('[销售分析] 加载销售趋势失败', err)
  }
}

async function loadChannel(params: any) {
  channelError.value = false
  try {
    const res = await salesAnalysisApi.getChannelDistribution(params)
    channelData.value = res.data || []
  } catch (err) {
    channelData.value = []
    channelError.value = true
    console.warn('[销售分析] 加载渠道分布失败', err)
  }
}

async function loadRankings(params: any) {
  rankLoading.value = true
  try {
    const [custRes, prodRes] = await Promise.allSettled([
      salesAnalysisApi.getCustomerRanking({ ...params, size: 10 }),
      salesAnalysisApi.getProductRanking({ ...params, size: 10 })
    ])
    if (custRes.status === 'fulfilled') customerRankData.value = custRes.value.data || []
    if (prodRes.status === 'fulfilled') productRankData.value = prodRes.value.data || []
  } catch (err) {
    console.warn('[销售分析] 加载排行榜失败', err)
  } finally {
    rankLoading.value = false
  }
}

// ── ECharts 图表初始化 ──
async function initCharts() {
  await nextTick()
  if (!trendData.value.length && !channelData.value.length) return

  try {
    const echartsModule: any = await import('echarts')
    const echarts = echartsModule.default || echartsModule

    // 趋势折线图
    if (trendChartRef.value && trendData.value.length) {
      if (!trendChartInstance) {
        trendChartInstance = echarts.init(trendChartRef.value)
      }
      trendChartInstance.setOption({
        tooltip: { trigger: 'axis' },
        legend: { data: ['销售额', '订单数'], bottom: 0 },
        grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
        xAxis: { type: 'category', data: trendData.value.map(d => d.month) },
        yAxis: [
          { type: 'value', name: '销售额 (¥)' },
          { type: 'value', name: '订单数' }
        ],
        series: [
          {
            name: '销售额',
            type: 'line',
            smooth: true,
            data: trendData.value.map(d => d.salesAmount),
            itemStyle: { color: '#1890ff' },
            areaStyle: { color: 'rgba(24,144,255,0.08)' }
          },
          {
            name: '订单数',
            type: 'line',
            smooth: true,
            yAxisIndex: 1,
            data: trendData.value.map(d => d.orderCount),
            itemStyle: { color: '#52c41a' },
            areaStyle: { color: 'rgba(82,196,26,0.08)' }
          }
        ]
      })
    }

    // 渠道分布饼图
    if (pieChartRef.value && channelData.value.length) {
      if (!pieChartInstance) {
        pieChartInstance = echarts.init(pieChartRef.value)
      }
      pieChartInstance.setOption({
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0 },
        series: [{
          type: 'pie',
          radius: ['30%', '55%'],
          center: ['50%', '45%'],
          data: channelData.value.map(d => ({ name: d.name, value: d.value })),
          emphasis: {
            itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' }
          }
        }]
      })
    }
  } catch (err) {
    console.warn('[销售分析] 图表加载失败', err)
  }
}

// ── 加载仓库列表 ──
async function loadWarehouses() {
  try {
    const res = await salesAnalysisApi.getWarehouses()
    warehouses.value = res.data || []
  } catch (err) {
    warehouses.value = []
    console.warn('[销售分析] 加载仓库列表失败', err)
  }
}

// ── 自动刷新 ──
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') {
    e.preventDefault()
    debounceClick('refresh', handleRefresh)
  }
}

onMounted(() => {
  loadWarehouses()
  loadAll()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    loadAll()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  document.addEventListener('keydown', handleKeydown)
})

onBeforeUnmount(() => {
  trendChartInstance?.dispose()
  pieChartInstance?.dispose()
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  document.removeEventListener('keydown', handleKeydown)
})

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
</script>

<style scoped>
.sales-analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.sales-analysis-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.sales-analysis-breadcrumb {
  font-size: 13px;
}
.sales-analysis-breadcrumb :deep(li) {
  font-size: 13px;
}
.sales-analysis-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.sales-analysis-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.update-time {
  font-size: 12px;
  color: #999;
}
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

.sales-analysis {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.filter-card {
  margin-bottom: 16px;
  flex-shrink: 0;
}

/* 统计卡片骨架 */
.stat-skeleton {
  border-radius: 8px;
}
.stat-skeleton :deep(.ant-card-body) {
  padding: 12px 16px;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
  background: #f5f5f5;
  color: #666;
}

.rank-badge.rank-top {
  background: #fff7e6;
  color: #fa8c16;
}

/* ── 统计卡片（渐变背景） ──────────────────────── */
.stat-row {
  margin-bottom: 16px;
  flex-shrink: 0;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 12px;
  transition: all 0.3s ease;
  cursor: default;
  height: 100%;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-card-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  margin-right: 16px;
}

.stat-card-content {
  flex: 1;
}

.stat-card-title {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  margin-bottom: 4px;
}

.stat-card-value {
  font-family: 'SFMono-Regular', 'SF Mono', 'Fira Code', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 28px;
  font-weight: 600;
  color: #fff;
  line-height: 1.2;
}

/* 渐变背景 */
.stat-card--blue {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
}

.stat-card--green {
  background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
}

.stat-card--orange {
  background: linear-gradient(135deg, #fa8c16 0%, #d46b08 100%);
}

.stat-card--purple {
  background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);
}

.chart-row {
  margin-bottom: 16px;
  flex-shrink: 0;
}

.chart-card {
  border-radius: 8px;
  position: relative;
}

.chart-empty {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #999;
  font-size: 14px;
  z-index: 1;
}
.chart-error {
  color: #ff4d4f;
  cursor: pointer;
}
.chart-error:hover {
  color: #cf1322;
}

.rank-row {
  margin-bottom: 16px;
  flex-shrink: 0;
}

.rank-card {
  border-radius: 8px;
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

/* ── 骨架加载 ────────────────────────────── */
.skeleton-loading {
  padding: 24px;
}

/* ── VxeTable 表头 2px 底部边框 ──────────── */
:deep(.vxe-table .vxe-header--row) {
  border-bottom: 2px solid #e8e8e8;
}

</style>
