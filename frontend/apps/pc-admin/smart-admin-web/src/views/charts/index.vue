<template>
  <div class="charts-demo">
    <a-card title="数据可视化组件示例">
      <!-- 示例1：柱状图 -->
      <a-card
        title="1. 柱状图 - 销售统计"
        style="margin-bottom: 20px"
      >
        <BarChart
          :data="barData"
          title="2024年季度销售"
          :width="600"
          :height="300"
          @click="handleBarClick"
        />
      </a-card>

      <!-- 示例2：折线图 -->
      <a-card
        title="2. 折线图 - 趋势分析"
        style="margin-bottom: 20px"
      >
        <LineChart
          :data="lineData"
          title="月度趋势"
          :smooth="true"
          :width="600"
          :height="300"
          @click="handleLineClick"
        />
      </a-card>

      <!-- 示例3：饼图 -->
      <a-card
        title="3. 饼图 - 占比分布"
        style="margin-bottom: 20px"
      >
        <PieChart
          :data="pieData"
          title="产品类别分布"
          :inner-radius="0.5"
          :show-label="true"
          :width="400"
          :height="400"
          @click="handlePieClick"
        />
      </a-card>

      <!-- 示例4：带筛选功能的图表 -->
      <a-card
        title="4. 交互功能 - 数据筛选"
        style="margin-bottom: 20px"
      >
        <div style="margin-bottom: 16px">
          <a-space>
            <a-button @click="filterByCategory('电子')">
              电子产品
            </a-button>
            <a-button @click="filterByCategory('服装')">
              服装
            </a-button>
            <a-button @click="clearFilter">
              清除筛选
            </a-button>
          </a-space>
        </div>
        <BarChart
          :data="filteredBarData"
          title="分类销售数据"
          :width="600"
          :height="300"
        />
      </a-card>

      <!-- 示例5：带导出功能的图表 -->
      <a-card
        title="5. 导出功能 - 图表导出"
        style="margin-bottom: 20px"
      >
        <div style="margin-bottom: 16px">
          <a-space>
            <a-button
              type="primary"
              @click="exportPNG"
            >
              导出PNG
            </a-button>
            <a-button @click="exportSVG">
              导出SVG
            </a-button>
            <a-button @click="exportData('json')">
              导出JSON
            </a-button>
            <a-button @click="exportData('csv')">
              导出CSV
            </a-button>
          </a-space>
        </div>
        <BarChart
          ref="exportChart"
          :data="barData"
          title="可导出图表"
          :width="600"
          :height="300"
        />
      </a-card>

      <!-- 示例6：动态图表 -->
      <a-card
        title="6. 动态图表 - 类型切换"
        style="margin-bottom: 20px"
      >
        <div style="margin-bottom: 16px">
          <a-radio-group
            v-model:value="chartType"
            button-style="solid"
          >
            <a-radio-button value="bar">
              柱状图
            </a-radio-button>
            <a-radio-button value="line">
              折线图
            </a-radio-button>
            <a-radio-button value="pie">
              饼图
            </a-radio-button>
          </a-radio-group>
        </div>
        <Chart
          :type="chartType"
          :data="dynamicData"
          title="动态图表"
          :width="600"
          :height="300"
          @click="handleDynamicClick"
        />
      </a-card>

      <!-- 示例7：多系列图表 -->
      <a-card
        title="7. 多系列图表 - 对比分析"
        style="margin-bottom: 20px"
      >
        <LineChart
          :series="multiSeries"
          title="年度对比"
          :width="600"
          :height="300"
          @click="handleMultiClick"
        />
      </a-card>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import {
  BarChart,
  LineChart,
  PieChart,
  Chart,
  useChartFilter,
  useChartExport
} from '@/components/Charts'
import type { ChartDataPoint, ChartSeries } from '@/components/Charts/types'

// 示例数据
const barData = ref<ChartDataPoint[]>([
  { name: '第一季度', value: 1200, category: '电子' },
  { name: '第二季度', value: 1800, category: '电子' },
  { name: '第三季度', value: 1500, category: '电子' },
  { name: '第四季度', value: 2100, category: '电子' }
])

const lineData = ref<ChartDataPoint[]>([
  { name: '1月', value: 100 },
  { name: '2月', value: 200 },
  { name: '3月', value: 150 },
  { name: '4月', value: 300 },
  { name: '5月', value: 250 },
  { name: '6月', value: 400 }
])

const pieData = ref<ChartDataPoint[]>([
  { name: '手机', value: 4500, color: '#5470c6' },
  { name: '电脑', value: 3200, color: '#91cc75' },
  { name: '平板', value: 1800, color: '#fac858' },
  { name: '配件', value: 900, color: '#ee6666' }
])

const dynamicData = ref<ChartDataPoint[]>([
  { name: 'A', value: 100 },
  { name: 'B', value: 200 },
  { name: 'C', value: 150 },
  { name: 'D', value: 300 }
])

const multiSeries = ref<ChartSeries[]>([
  {
    name: '2024年',
    data: [
      { name: 'Q1', value: 1200 },
      { name: 'Q2', value: 1800 },
      { name: 'Q3', value: 1500 },
      { name: 'Q4', value: 2100 }
    ],
    color: '#5470c6'
  },
  {
    name: '2025年',
    data: [
      { name: 'Q1', value: 1400 },
      { name: 'Q2', value: 2000 },
      { name: 'Q3', value: 1700 },
      { name: 'Q4', value: 2300 }
    ],
    color: '#91cc75'
  }
])

// 筛选功能
const { filters, addFilter, clearFilters, applyFilters } = useChartFilter()

const filteredBarData = computed(() => {
  return applyFilters(barData.value)
})

// 导出功能
const exportChart = ref()
const { exportAsImage, exportAsData } = useChartExport()

// 动态图表类型
const chartType = ref<'bar' | 'line' | 'pie'>('bar')

// 事件处理
const handleBarClick = (data: ChartDataPoint) => {
  message.info(`柱状图点击: ${data.name} = ${data.value}`)
}

const handleLineClick = (data: ChartDataPoint) => {
  message.info(`折线图点击: ${data.name} = ${data.value}`)
}

const handlePieClick = (data: ChartDataPoint) => {
  message.info(`饼图点击: ${data.name} = ${data.value}`)
}

const handleDynamicClick = (data: ChartDataPoint) => {
  message.info(`动态图表点击: ${data.name} = ${data.value}`)
}

const handleMultiClick = (data: ChartDataPoint) => {
  message.info(`多系列点击: ${data.name} = ${data.value}`)
}

// 筛选操作
const filterByCategory = (category: string) => {
  addFilter('category', category)
  message.success(`已筛选: ${category}`)
}

const clearFilter = () => {
  clearFilters()
  message.info('已清除筛选')
}

// 导出操作
const exportPNG = async () => {
  const svg = document.querySelector('.bar-chart svg') as SVGElement
  if (svg) {
    await exportAsImage(svg, { type: 'png', filename: 'bar-chart' })
    message.success('PNG导出成功')
  }
}

const exportSVG = async () => {
  const svg = document.querySelector('.bar-chart svg') as SVGElement
  if (svg) {
    await exportAsImage(svg, { type: 'svg', filename: 'bar-chart' })
    message.success('SVG导出成功')
  }
}

const exportData = (type: 'json' | 'csv') => {
  exportAsData(barData.value, { type, filename: 'chart-data' })
  message.success(`${type.toUpperCase()}导出成功`)
}
</script>

<style scoped>
.charts-demo {
  padding: 20px;
}

.tooltip {
  position: absolute;
  background: rgba(0, 0, 0, 0.8);
  color: white;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 12px;
  pointer-events: none;
  z-index: 1000;
}
</style>