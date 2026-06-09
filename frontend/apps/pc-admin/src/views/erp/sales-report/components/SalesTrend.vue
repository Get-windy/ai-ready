<template>
  <div class="sales-trend">
    <!-- 筛选区 -->
    <a-collapse v-model:activeKey="filterExpanded" class="filter-collapse">
      <a-collapse-panel key="1" header="筛选条件">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-form-item label="时间范围">
              <a-range-picker
                v-model:value="queryParams.dateRange"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="统计类型">
              <a-select
                v-model:value="queryParams.type"
                placeholder="按日"
                style="width: 100%"
              >
                <a-select-option value="daily">按日</a-select-option>
                <a-select-option value="weekly">按周</a-select-option>
                <a-select-option value="monthly">按月</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="对比维度">
              <a-select
                v-model:value="queryParams.compareType"
                placeholder="同比"
                style="width: 100%"
              >
                <a-select-option value="yoy">同比</a-select-option>
                <a-select-option value="mom">环比</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="6" class="filter-actions">
            <a-space>
              <a-button type="primary" :loading="loading" @click="handleQuery">查询</a-button>
              <a-button @click="handleReset">重置</a-button>
              <a-button @click="handleExport">
                <template #icon><ExportOutlined /></template>
                导出
              </a-button>
            </a-space>
          </a-col>
        </a-row>
      </a-collapse-panel>
    </a-collapse>

    <!-- 关键指标卡片 -->
    <div class="stats-cards">
      <a-row :gutter="16">
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
              <ArrowUpOutlined />
            </div>
            <div class="stat-content">
              <div class="stat-title">同比增长</div>
              <div class="stat-value" :class="{ positive: stats.yoyGrowth >= 0, negative: stats.yoyGrowth < 0 }">
                {{ stats.yoyGrowth >= 0 ? '+' : '' }}{{ stats.yoyGrowth }}%
              </div>
              <div class="stat-desc">与去年同期对比</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
              <LineChartOutlined />
            </div>
            <div class="stat-content">
              <div class="stat-title">环比增长</div>
              <div class="stat-value" :class="{ positive: stats.momGrowth >= 0, negative: stats.momGrowth < 0 }">
                {{ stats.momGrowth >= 0 ? '+' : '' }}{{ stats.momGrowth }}%
              </div>
              <div class="stat-desc">与上期对比</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
              <DollarOutlined />
            </div>
            <div class="stat-content">
              <div class="stat-title">本期销售</div>
              <div class="stat-value">¥{{ formatAmount(stats.currentPeriod) }}</div>
              <div class="stat-desc">{{ getPeriodDesc() }}</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
              <HistoryOutlined />
            </div>
            <div class="stat-content">
              <div class="stat-title">上期销售</div>
              <div class="stat-value">¥{{ formatAmount(stats.lastPeriod) }}</div>
              <div class="stat-desc">对比基准</div>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>

    <!-- 销售趋势图表 -->
    <a-card title="销售趋势" size="small" :loading="chartLoading">
      <template #extra>
        <a-radio-group v-model:value="chartMode" size="small" @change="initChart">
          <a-radio-button value="line">折线图</a-radio-button>
          <a-radio-button value="bar">柱状图</a-radio-button>
          <a-radio-button value="area">面积图</a-radio-button>
        </a-radio-group>
      </template>
      <div ref="chartRef" class="chart-container"></div>
    </a-card>

    <!-- 趋势明细表格 -->
    <a-card title="趋势明细" size="small" style="margin-top: 16px">
      <div class="table-container">
        <VxeTableList
          :columns="trendVxeColumns"
          :data-source="trendData"
          :loading="loading"
          :pagination="false"
          row-key="period"
          :show-toolbar="false"
          :selectable="false"
          :show-add="false"
          :show-search="false"
          :show-export="false"
          :show-batch-delete="false"
        >
          <template #currentSalesCell="{ record }">
            <span class="amount-cell">¥{{ formatAmount(record.currentSales) }}</span>
          </template>
          <template #lastSalesCell="{ record }">
            <span class="amount-cell">¥{{ formatAmount(record.lastSales) }}</span>
          </template>
          <template #growthCell="{ record }">
            <span :class="['growth-cell', { positive: record.growth > 0, negative: record.growth < 0 }]">
              <ArrowUpOutlined v-if="record.growth > 0" />
              <ArrowDownOutlined v-if="record.growth < 0" />
              {{ Math.abs(record.growth) }}%
            </span>
          </template>
        </VxeTableList>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import * as echarts from 'echarts'
import {
  ExportOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  DollarOutlined,
  LineChartOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { salesReportApi } from '@/api/sales-report'

// HistoryOutlined 使用 LineChartOutlined 替代
const HistoryOutlined = LineChartOutlined

const chartRef = ref<HTMLElement>()
const loading = ref(false)
const chartLoading = ref(false)
const filterExpanded = ref<string[]>([])
const chartMode = ref<'line' | 'bar' | 'area'>('area')

const queryParams = reactive({
  dateRange: [] as string[],
  type: 'daily',
  compareType: 'yoy'
})

const stats = reactive({
  yoyGrowth: 15.5,
  momGrowth: 8.2,
  currentPeriod: 1160000,
  lastPeriod: 1000000
})

const trendData = ref<any[]>([])

const trendVxeColumns = [
  { field: 'period', title: '周期', width: 120 },
  { field: 'currentSales', title: '本期销售', width: 140, align: 'right', slotName: 'currentSalesCell' },
  { field: 'lastSales', title: '上期销售', width: 140, align: 'right', slotName: 'lastSalesCell' },
  { field: 'growth', title: '增长率', width: 100, align: 'right', slotName: 'growthCell' },
  { field: 'orderCount', title: '订单数', width: 100, align: 'right' }
]

let chart: echarts.ECharts | null = null

const handleResize = () => chart?.resize()

const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const getPeriodDesc = () => {
  const typeMap = { daily: '本日', weekly: '本周', monthly: '本月' }
  return typeMap[queryParams.type] || '本期'
}

const buildParams = () => {
  const params: any = { type: queryParams.type, compareType: queryParams.compareType }
  if (queryParams.dateRange.length === 2) {
    params.startDate = queryParams.dateRange[0]
    params.endDate = queryParams.dateRange[1]
  }
  return params
}

const handleQuery = async () => {
  loading.value = true
  chartLoading.value = true
  try {
    const res = await salesReportApi.getSalesTrend(buildParams())
    if (res.data) {
      const d = res.data
      stats.currentPeriod = d.reduce((s: number, v: any) => s + v.sales, 0)
      trendData.value = []
      initChart()
    } else {
      trendData.value = []
      initChart()
    }
  } catch (err) {
    trendData.value = []
    initChart()
  } finally {
    loading.value = false
    chartLoading.value = false
  }
}

const handleReset = () => {
  queryParams.dateRange = []
  queryParams.type = 'daily'
  queryParams.compareType = 'yoy'
  handleQuery()
}

const handleExport = () => {
  const csvData = trendData.value.map(item =>
    `${item.period},${item.currentSales},${item.lastSales},${item.growth},${item.orderCount}`
  )
  const csv = ['周期,本期销售,上期销售,增长率,订单数', ...csvData].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `销售趋势_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

const initChart = () => {
  nextTick(() => {
    if (!chartRef.value) return
    if (!chart) chart = echarts.init(chartRef.value)

    const periods = trendData.value.map(d => d.period)
    const currentSales = trendData.value.map(d => d.currentSales)
    const lastSales = trendData.value.map(d => d.lastSales)

    const seriesConfig = {
      line: { type: 'line', smooth: true },
      bar: { type: 'bar' },
      area: { type: 'line', smooth: true, areaStyle: { opacity: 0.3 } }
    }

    chart.setOption({
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
        formatter: (params: any[]) => {
          let result = `${params[0].axisValue}<br/>`
          params.forEach(p => {
            result += `${p.marker}${p.seriesName}: ¥${formatAmount(p.value)}<br/>`
          })
          return result
        }
      },
      legend: { data: ['本期销售', '上期销售'], bottom: 0 },
      grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
      xAxis: { type: 'category', data: periods },
      yAxis: {
        type: 'value',
        name: '销售额(万)',
        axisLabel: { formatter: (v: number) => `${v / 10000}` }
      },
      series: [
        {
          name: '本期销售',
          ...seriesConfig[chartMode.value],
          data: currentSales,
          itemStyle: { color: '#1890ff' }
        },
        {
          name: '上期销售',
          ...seriesConfig[chartMode.value],
          data: lastSales,
          itemStyle: { color: '#52c41a' }
        }
      ]
    })
    window.addEventListener('resize', handleResize)
  })
}

onMounted(() => handleQuery())
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
  chart = null
})

defineExpose({ handleQuery })
</script>

<style scoped>
.sales-trend {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  overflow-y: auto;
}

.filter-collapse {
  flex-shrink: 0;
}

.filter-actions {
  display: flex;
  align-items: flex-end;
  padding-bottom: 4px;
}

.stats-cards {
  flex-shrink: 0;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  margin-right: 16px;
}

.stat-content {
  flex: 1;
}

.stat-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}

.stat-value.positive {
  color: #52c41a;
}

.stat-value.negative {
  color: #f5222d;
}

.stat-desc {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.chart-container {
  height: 350px;
}

.table-container {
  max-height: 300px;
  overflow-y: auto;
}





.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

.growth-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.growth-cell.positive {
  color: #52c41a;
}

.growth-cell.negative {
  color: #f5222d;
}

</style>
