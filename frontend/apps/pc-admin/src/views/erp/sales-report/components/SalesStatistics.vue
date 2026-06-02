<template>
  <div class="sales-statistics">
    <div class="filter-area">
      <a-form layout="inline">
        <a-form-item label="统计周期">
          <a-range-picker
            v-model:value="queryParams.dateRange"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </a-form-item>
        <a-form-item>
          <a-button
            type="primary"
            @click="handleQuery"
          >
            查询
          </a-button>
        </a-form-item>
      </a-form>
    </div>

    <!-- 统计卡片 -->
    <a-row
      :gutter="16"
      class="stats-area"
    >
      <a-col :span="6">
        <a-card>
          <a-statistic
            title="销售总额"
            :value="stats.totalSales"
            :precision="2"
            prefix="¥"
          />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic
            title="订单数量"
            :value="stats.orderCount"
            suffix="单"
          />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic
            title="平均客单价"
            :value="stats.avgOrderValue"
            :precision="2"
            prefix="¥"
          />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card>
          <a-statistic
            title="退货金额"
            :value="stats.returnAmount"
            :precision="2"
            prefix="¥"
            :value-style="{ color: '#ff4d4f' }"
          />
        </a-card>
      </a-col>
    </a-row>

    <!-- 销售趋势图表 -->
    <div class="chart-area">
      <a-card title="销售趋势">
        <div
          ref="chartRef"
          style="height: 400px"
        />
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import * as echarts from 'echarts'
import { salesReportApi, type SalesStats } from '@/api/sales-report'

const chartRef = ref<HTMLElement>()

const queryParams = reactive({ dateRange: [] as string[] })
const stats = reactive<SalesStats>({ totalSales: 0, orderCount: 0, avgOrderValue: 0, returnAmount: 0 })
let chart: echarts.ECharts | null = null

const handleResize = () => { chart?.resize() }

const initChart = (data?: { date: string; sales: number; orders: number }[]) => {
  nextTick(() => {
    if (!chartRef.value) return
    if (!chart) chart = echarts.init(chartRef.value)
    const dates = data?.map(d => d.date) || ['周一','周二','周三','周四','周五','周六','周日']
    const sales = data?.map(d => d.sales) || [0,0,0,0,0,0,0]
    const orders = data?.map(d => d.orders) || [0,0,0,0,0,0,0]
    chart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['销售额','订单数'] },
      xAxis: { type: 'category', data: dates },
      yAxis: [{ type: 'value', name: '销售额(元)' }, { type: 'value', name: '订单数' }],
      series: [
        { name: '销售额', type: 'bar', data: sales },
        { name: '订单数', type: 'line', yAxisIndex: 1, data: orders }
      ]
    })
    window.addEventListener('resize', handleResize)
  })
}

const handleQuery = async () => {
  try {
    const params = queryParams.dateRange.length === 2
      ? { startDate: queryParams.dateRange[0], endDate: queryParams.dateRange[1] } : {}
    const [statsRes, trendRes] = await Promise.allSettled([
      salesReportApi.getStatistics(params),
      salesReportApi.getSalesTrend(params)
    ])
    if (statsRes.status === 'fulfilled' && statsRes.value.data) {
      Object.assign(stats, statsRes.value.data)
    }
    if (trendRes.status === 'fulfilled') {
      initChart(trendRes.value.data || undefined)
    } else { initChart() }
  } catch { message.info('加载统计数据失败') }
}

onMounted(() => handleQuery())
onUnmounted(() => { window.removeEventListener('resize', handleResize); chart?.dispose(); chart = null })
</script>

<style scoped>
.sales-statistics {
  padding: 16px;
}

.filter-area {
  margin-bottom: 16px;
}

.stats-area {
  margin-bottom: 16px;
}

.chart-area {
  margin-bottom: 16px;
}
</style>