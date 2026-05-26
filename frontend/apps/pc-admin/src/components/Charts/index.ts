/**
 * AI-Ready 数据可视化组件
 * 
 * 提供柱状图、折线图、饼图、雷达图等图表组件
 * 支持主题配置、交互功能、数据导出、响应式适配
 */

// 类型定义
export * from './types'

// 组件
export { default as BaseChart } from './BaseChart.vue'
export { default as BarChart } from './BarChart.vue'
export { default as LineChart } from './LineChart.vue'
export { default as PieChart } from './PieChart.vue'
export { default as RadarChart } from './RadarChart.vue'

// Composable
export { useChartExport, useChartFilter, useChartZoom } from './composables'

// 核心功能 - 主题、响应式、动画
export * from './core'

// 主题预设
export { ThemeColors } from './types'

// 统一图表组件（根据type自动渲染对应图表）
import { defineComponent, h, computed } from 'vue'
import type { ChartType, ChartDataPoint, ChartSeries, ChartConfig } from './types'
import BarChart from './BarChart.vue'
import LineChart from './LineChart.vue'
import PieChart from './PieChart.vue'
import RadarChart from './RadarChart.vue'

interface ChartProps {
  type: ChartType
  data?: ChartDataPoint[]
  series?: ChartSeries[]
  width?: number
  height?: number
  title?: string
  config?: ChartConfig
  colors?: string[]
  showLegend?: boolean
  showLabel?: boolean
  smooth?: boolean
  innerRadius?: number
}

export const Chart = defineComponent<ChartProps>({
  name: 'AIChart',
  props: {
    type: { type: String as () => ChartType, required: true },
    data: { type: Array as () => ChartDataPoint[], default: () => [] },
    series: { type: Array as () => ChartSeries[], default: () => [] },
    width: { type: Number, default: 400 },
    height: { type: Number, default: 300 },
    title: { type: String, default: '' },
    config: { type: Object as () => ChartConfig },
    colors: { type: Array as () => string[], default: () => [] },
    showLegend: { type: Boolean, default: true },
    showLabel: { type: Boolean, default: true },
    smooth: { type: Boolean, default: false },
    innerRadius: { type: Number, default: 0 }
  },
  setup(props) {
    const component = computed(() => {
      switch (props.type) {
        case 'bar': return BarChart
        case 'line': return LineChart
        case 'pie': return PieChart
        case 'radar': return RadarChart
        default: return BarChart
      }
    })
    
    return () => h(component.value, {
      data: props.data,
      series: props.series,
      width: props.width,
      height: props.height,
      title: props.title,
      config: props.config,
      colors: props.colors,
      showLegend: props.showLegend,
      showLabel: props.showLabel,
      smooth: props.smooth,
      innerRadius: props.innerRadius
    })
  }
})

export default {
  BarChart,
  LineChart,
  PieChart,
  RadarChart,
  Chart,
  useChartExport,
  useChartFilter,
  useChartZoom
}
