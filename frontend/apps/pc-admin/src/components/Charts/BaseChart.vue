<template>
  <div
    ref="chartRef"
    class="base-chart"
    :style="{ width: `${width}px`, height: `${height}px` }"
  >
    <svg
      :width="width"
      :height="height"
      class="chart-svg"
    >
      <!-- 标题 -->
      <text
        v-if="title"
        :x="width / 2"
        :y="20"
        text-anchor="middle"
        class="chart-title"
      >
        {{ title }}
      </text>
      <text
        v-if="subTitle"
        :x="width / 2"
        :y="40"
        text-anchor="middle"
        class="chart-subtitle"
      >
        {{ subTitle }}
      </text>
      
      <!-- 图表内容区域 -->
      <g
        class="chart-content"
        :transform="`translate(${gridLeft}, ${gridTop})`"
      >
        <slot />
      </g>
      
      <!-- 图例 -->
      <g
        v-if="showLegend"
        class="chart-legend"
        :transform="`translate(${width - 100}, ${gridTop})`"
      >
        <template
          v-for="(item, index) in legendData"
          :key="index"
        >
          <rect
            :y="index * 20"
            width="12"
            height="12"
            :fill="item.color"
          />
          <text
            :y="index * 20 + 10"
            x="18"
            class="legend-text"
          >{{ item.name }}</text>
        </template>
      </g>
    </svg>
    
    <!-- Tooltip -->
    <div
      v-if="tooltipVisible"
      class="chart-tooltip"
      :style="tooltipStyle"
    >
      {{ tooltipContent }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, provide } from 'vue'
import type { ChartConfig, ChartDataPoint, ChartSeries } from './types'

const props = withDefaults(defineProps<{
  title?: string
  subTitle?: string
  width?: number
  height?: number
  config?: ChartConfig
  data?: ChartSeries[]
}>(), {
  width: 400,
  height: 300
})

const chartRef = ref<HTMLElement>()
const tooltipVisible = ref(false)
const tooltipContent = ref('')
const tooltipStyle = ref({ left: '0px', top: '0px' })

// 计算网格边距
const gridTop = computed(() => props.config?.grid?.top ?? (props.title ? 60 : 40))
const gridRight = computed(() => props.config?.grid?.right ?? 120)
const gridBottom = computed(() => props.config?.grid?.bottom ?? 40)
const gridLeft = computed(() => props.config?.grid?.left ?? 60)

// 计算内容区域尺寸
const contentWidth = computed(() => props.width - gridLeft.value - gridRight.value)
const contentHeight = computed(() => props.height - gridTop.value - gridBottom.value)

// 显示图例
const showLegend = computed(() => props.config?.legend?.show ?? true)

// 图例数据
const legendData = computed(() => {
  if (!props.data) return []
  return props.data.map((series, index) => ({
    name: series.name,
    color: series.color || getThemeColor(index)
  }))
})

// 获取主题颜色
const getThemeColor = (index: number) => {
  const colors = props.config?.colors || [
    '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272'
  ]
  return colors[index % colors.length]
}

// 提供给子组件
provide('chartConfig', props.config)
provide('getThemeColor', getThemeColor)
provide('contentWidth', contentWidth)
provide('contentHeight', contentHeight)

// 显示tooltip
const showTooltip = (event: MouseEvent, data: ChartDataPoint) => {
  tooltipVisible.value = true
  tooltipContent.value = `${data.name}: ${data.value}`
  tooltipStyle.value = {
    left: `${event.offsetX + 10}px`,
    top: `${event.offsetY + 10}px`
  }
}

// 隐藏tooltip
const hideTooltip = () => {
  tooltipVisible.value = false
}

// 导出函数供子组件使用
defineExpose({ showTooltip, hideTooltip })
</script>

<style scoped>
.base-chart {
  position: relative;
  display: inline-block;
}

.chart-svg {
  overflow: visible;
}

.chart-title {
  font-size: 16px;
  font-weight: bold;
  fill: #333;
}

.chart-subtitle {
  font-size: 12px;
  fill: #666;
}

.legend-text {
  font-size: 12px;
  fill: #333;
}

.chart-tooltip {
  position: absolute;
  padding: 8px 12px;
  background: rgba(0, 0, 0, 0.75);
  color: #fff;
  border-radius: 4px;
  font-size: 12px;
  pointer-events: none;
  white-space: nowrap;
  z-index: 100;
}
</style>
