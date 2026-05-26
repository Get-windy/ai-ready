<template>
  <div class="sales-trend">
    <div class="filter-area">
      <a-form layout="inline">
        <a-form-item label="时间范围">
          <a-range-picker
            v-model:value="queryParams.dateRange"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </a-form-item>
        <a-form-item label="统计类型">
          <a-select
            v-model:value="queryParams.type"
            style="width: 120px"
          >
            <a-select-option value="daily">
              按日
            </a-select-option>
            <a-select-option value="weekly">
              按周
            </a-select-option>
            <a-select-option value="monthly">
              按月
            </a-select-option>
          </a-select>
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

    <!-- 销售趋势图表 -->
    <div class="chart-area">
      <a-card title="销售趋势">
        <div
          ref="chartRef"
          style="height: 400px"
        />
      </a-card>
    </div>

    <!-- 关键指标 -->
    <a-row
      :gutter="16"
      class="stats-area"
    >
      <a-col :span="6">
        <a-statistic
          title="同比增长"
          :value="stats.yoyGrowth"
          suffix="%"
          :value-style="{ color: stats.yoyGrowth >= 0 ? '#3f8600' : '#cf1322' }"
        />
      </a-col>
      <a-col :span="6">
        <a-statistic
          title="环比增长"
          :value="stats.momGrowth"
          suffix="%"
          :value-style="{ color: stats.momGrowth >= 0 ? '#3f8600' : '#cf1322' }"
        />
      </a-col>
      <a-col :span="6">
        <a-statistic
          title="本期销售"
          :value="stats.currentPeriod"
          :precision="2"
          prefix="¥"
        />
      </a-col>
      <a-col :span="6">
        <a-statistic
          title="上期销售"
          :value="stats.lastPeriod"
          :precision="2"
          prefix="¥"
        />
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import * as echarts from 'echarts'

const chartRef = ref<HTMLElement>()

const queryParams = reactive({
  dateRange: [] as string[],
  type: 'daily'
})

const stats = reactive({
  yoyGrowth: 15.5,
  momGrowth: 8.2,
  currentPeriod: 116000,
  lastPeriod: 100000
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
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      legend: {
        data: ['本期销售', '上期销售']
      },
      xAxis: {
        type: 'category',
        data: ['1月', '2月', '3月', '4月', '5月', '6月']
      },
      yAxis: {
        type: 'value',
        name: '销售额（元）'
      },
      series: [
        {
          name: '本期销售',
          type: 'line',
          smooth: true,
          data: [50000, 60000, 55000, 70000, 80000, 90000],
          areaStyle: {
            opacity: 0.3
          }
        },
        {
          name: '上期销售',
          type: 'line',
          smooth: true,
          data: [45000, 55000, 50000, 65000, 75000, 85000],
          areaStyle: {
            opacity: 0.3
          }
        }
      ]
    }
    chart.setOption(option)

    window.addEventListener('resize', () => chart.resize())
  })
}

const handleQuery = () => {
  message.info('查询销售趋势')
}

onMounted(() => {
  handleQuery()
  initChart()
})
</script>

<style scoped>
.sales-trend {
  padding: 16px;
}

.filter-area {
  margin-bottom: 16px;
}

.chart-area {
  margin-bottom: 16px;
}

.stats-area {
  margin-bottom: 16px;
}
</style>