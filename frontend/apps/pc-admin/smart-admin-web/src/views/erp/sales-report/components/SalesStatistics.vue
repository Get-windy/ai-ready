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
import { ref, reactive, onMounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import * as echarts from 'echarts'

const chartRef = ref<HTMLElement>()

const queryParams = reactive({
  dateRange: [] as string[]
})

const stats = reactive({
  totalSales: 0,
  orderCount: 0,
  avgOrderValue: 0,
  returnAmount: 0
})

const initChart = () => {
  nextTick(() => {
    if (!chartRef.value) return

    const chart = echarts.init(chartRef.value)
    const option = {
      title: {
        text: '销售趋势'
      },
      tooltip: {
        trigger: 'axis'
      },
      legend: {
        data: ['销售额', '订单数']
      },
      xAxis: {
        type: 'category',
        data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      },
      yAxis: [
        {
          type: 'value',
          name: '销售额（元）',
          position: 'left'
        },
        {
          type: 'value',
          name: '订单数',
          position: 'right'
        }
      ],
      series: [
        {
          name: '销售额',
          type: 'bar',
          data: [12000, 15000, 10000, 18000, 20000, 22000, 19000]
        },
        {
          name: '订单数',
          type: 'line',
          yAxisIndex: 1,
          data: [12, 15, 10, 18, 20, 22, 19]
        }
      ]
    }
    chart.setOption(option)

    window.addEventListener('resize', () => chart.resize())
  })
}

const handleQuery = () => {
  message.info('查询销售统计')
  // 模拟数据更新
  stats.totalSales = 116000
  stats.orderCount = 116
  stats.avgOrderValue = 1000
  stats.returnAmount = 5000
}

onMounted(() => {
  handleQuery()
  initChart()
})
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