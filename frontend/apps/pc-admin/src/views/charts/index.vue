<template>
  <div class="sales-analysis">
    <!-- 筛选栏 -->
    <a-card class="filter-card" :bordered="false">
      <a-space wrap>
        <a-range-picker
          v-model:value="dateRange"
          value-format="YYYY-MM-DD"
          style="width: 240px"
          @change="handleFilterChange"
        />
        <a-select
          v-model:value="warehouseId"
          placeholder="选择仓库"
          allow-clear
          style="width: 160px"
          @change="handleFilterChange"
        >
          <a-select-option
            v-for="w in warehouses"
            :key="w.id"
            :value="w.id"
          >
            {{ w.name }}
          </a-select-option>
        </a-select>
        <a-button type="primary" :loading="loading" @click="loadAll">查询</a-button>
      </a-space>
    </a-card>

    <!-- KPI 概览 -->
    <a-row :gutter="16" class="kpi-row">
      <a-col :xs="12" :sm="12" :md="6">
        <a-card class="kpi-card" :bordered="false" :loading="overviewLoading">
          <a-statistic title="销售总额" :value="overview?.totalAmount || 0" prefix="¥" :precision="2" />
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <a-card class="kpi-card" :bordered="false" :loading="overviewLoading">
          <a-statistic title="订单数量" :value="overview?.orderCount || 0" />
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <a-card class="kpi-card" :bordered="false" :loading="overviewLoading">
          <a-statistic title="平均客单价" :value="overview?.avgOrderAmount || 0" prefix="¥" :precision="2" />
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <a-card class="kpi-card" :bordered="false" :loading="overviewLoading">
          <a-statistic title="毛利率" :value="(overview?.grossMargin || 0) * 100" suffix="%" :precision="1" />
        </a-card>
      </a-col>
    </a-row>

    <!-- 图表区域 -->
    <a-row :gutter="16" class="chart-row">
      <a-col :xs="24" :md="16">
        <a-card :bordered="false" title="销售趋势" class="chart-card">
          <div ref="trendChartRef" style="height: 320px" />
          <div v-if="!trendData?.length" class="chart-empty">暂无趋势数据</div>
        </a-card>
      </a-col>
      <a-col :xs="24" :md="8">
        <a-card :bordered="false" title="渠道分布" class="chart-card">
          <div ref="pieChartRef" style="height: 320px" />
          <div v-if="!channelData?.length" class="chart-empty">暂无渠道数据</div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 排行榜 -->
    <a-row :gutter="16" class="rank-row">
      <a-col :xs="24" :md="12">
        <a-card :bordered="false" title="客户排行 TOP10" class="rank-card">
          <a-table
            :columns="customerRankCols"
            :data-source="customerRankData"
            :pagination="false"
            :loading="rankLoading"
            size="small"
            row-key="rank"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === 'rank'">
                <a-tag :color="index < 3 ? 'gold' : 'default'">{{ record.rank }}</a-tag>
              </template>
              <template v-else-if="column.key === 'growth'">
                <span :style="{ color: record.growth >= 0 ? '#52c41a' : '#f5222d' }">
                  {{ record.growth >= 0 ? '+' : '' }}{{ (record.growth * 100).toFixed(1) }}%
                </span>
              </template>
              <template v-else-if="column.key === 'totalAmount'">
                ¥{{ record.totalAmount.toFixed(2) }}
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
      <a-col :xs="24" :md="12">
        <a-card :bordered="false" title="产品排行 TOP10" class="rank-card">
          <a-table
            :columns="productRankCols"
            :data-source="productRankData"
            :pagination="false"
            :loading="rankLoading"
            size="small"
            row-key="rank"
          >
            <template #bodyCell="{ column, record, index }">
              <template v-if="column.key === 'rank'">
                <a-tag :color="index < 3 ? 'gold' : 'default'">{{ record.rank }}</a-tag>
              </template>
              <template v-else-if="column.key === 'margin'">
                <span :style="{ color: record.margin >= 0.2 ? '#52c41a' : '#faad14' }">
                  {{ (record.margin * 100).toFixed(1) }}%
                </span>
              </template>
              <template v-else-if="column.key === 'totalAmount'">
                ¥{{ record.totalAmount.toFixed(2) }}
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import { salesAnalysisApi, type SalesOverview, type TrendDataPoint, type ChannelDistribution, type CustomerRankItem, type ProductRankItem } from '@/api/sales-analysis'

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

// ── 渠道饼图 ──
const channelData = ref<ChannelDistribution[]>([])
const pieChartRef = ref<HTMLElement | null>(null)
let pieChartInstance: any = null

// ── 排行榜 ──
const customerRankData = ref<CustomerRankItem[]>([])
const productRankData = ref<ProductRankItem[]>([])
const rankLoading = ref(false)

const customerRankCols = [
  { title: '排名', key: 'rank', width: 60 },
  { title: '客户名称', dataIndex: 'name', ellipsis: true },
  { title: '订单数', dataIndex: 'orderCount', width: 80 },
  { title: '销售额', key: 'totalAmount', width: 130, align: 'right' as const },
  { title: '增长率', key: 'growth', width: 100, align: 'right' as const }
]

const productRankCols = [
  { title: '排名', key: 'rank', width: 60 },
  { title: '产品名称', dataIndex: 'name', ellipsis: true },
  { title: '销量', dataIndex: 'volume', width: 80 },
  { title: '销售额', key: 'totalAmount', width: 130, align: 'right' as const },
  { title: '利润率', key: 'margin', width: 90, align: 'right' as const }
]

const loading = ref(false)

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
  await nextTick()
  initCharts()
}

async function loadOverview(params: any) {
  overviewLoading.value = true
  try {
    const res = await salesAnalysisApi.getOverview(params)
    overview.value = res.data || null
  } catch {
    overview.value = null
  } finally {
    overviewLoading.value = false
  }
}

async function loadTrend(params: any) {
  try {
    const res = await salesAnalysisApi.getTrend(params)
    trendData.value = res.data || []
  } catch {
    trendData.value = []
  }
}

async function loadChannel(params: any) {
  try {
    const res = await salesAnalysisApi.getChannelDistribution(params)
    channelData.value = res.data || []
  } catch {
    channelData.value = []
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
  } catch {
    // 静默失败
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
  } catch {
    // 图表加载失败不阻塞页面
  }
}

// ── 加载仓库列表 ──
async function loadWarehouses() {
  try {
    const res = await salesAnalysisApi.getWarehouses()
    warehouses.value = res.data || []
  } catch {
    warehouses.value = []
  }
}

// ── 生命周期 ──
onMounted(() => {
  loadWarehouses()
  loadAll()
})

onBeforeUnmount(() => {
  trendChartInstance?.dispose()
  pieChartInstance?.dispose()
})
</script>

<style scoped>
.sales-analysis {
  padding: 16px;
}

.filter-card {
  margin-bottom: 16px;
}

.kpi-row {
  margin-bottom: 16px;
}

.kpi-card {
  border-radius: 8px;
}

.chart-row {
  margin-bottom: 16px;
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

.rank-row {
  margin-bottom: 16px;
}

.rank-card {
  border-radius: 8px;
}
</style>
