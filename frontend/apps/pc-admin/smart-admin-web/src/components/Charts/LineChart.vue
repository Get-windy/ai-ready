<template>
  <div
    ref="containerRef"
    class="line-chart"
  >
    <svg
      :width="width"
      :height="height"
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
      
      <!-- Y轴 -->
      <g
        class="y-axis"
        :transform="`translate(${paddingLeft}, ${paddingTop})`"
      >
        <line
          x1="0"
          y1="0"
          x2="0"
          :y2="chartHeight"
          stroke="#e0e0e0"
        />
        <template
          v-for="(tick, i) in yTicks"
          :key="i"
        >
          <text
            x="-10"
            :y="chartHeight - tick.pos"
            text-anchor="end"
            class="tick-text"
          >
            {{ tick.value }}
          </text>
          <line
            x1="0"
            :y1="chartHeight - tick.pos"
            :x2="chartWidth"
            :y2="chartHeight - tick.pos"
            stroke="#f0f0f0"
          />
        </template>
      </g>
      
      <!-- X轴 -->
      <g
        class="x-axis"
        :transform="`translate(${paddingLeft}, ${paddingTop + chartHeight})`"
      >
        <line
          x1="0"
          y1="0"
          :x2="chartWidth"
          y2="0"
          stroke="#e0e0e0"
        />
        <template
          v-for="(label, i) in xLabels"
          :key="i"
        >
          <text
            :x="pointGap * i"
            y="20"
            text-anchor="middle"
            class="tick-text"
          >
            {{ label }}
          </text>
        </template>
      </g>
      
      <!-- 折线 -->
      <g
        class="lines"
        :transform="`translate(${paddingLeft}, ${paddingTop})`"
      >
        <!-- 线条 -->
        <path
          v-for="(line, i) in lines"
          :key="'line-' + i"
          :d="line.path"
          fill="none"
          :stroke="line.color"
          stroke-width="2"
          class="line-path"
        />
        
        <!-- 数据点 -->
        <g
          v-for="(line, li) in lines"
          :key="'points-' + li"
        >
          <circle
            v-for="(point, pi) in line.points"
            :key="pi"
            :cx="point.x"
            :cy="point.y"
            r="4"
            :fill="line.color"
            class="data-point"
            @mouseover="handleHover($event, point)"
            @mouseout="handleOut"
            @click="handleClick(point)"
          />
        </g>
      </g>
      
      <!-- 图例 -->
      <g
        v-if="showLegend && series.length > 1"
        class="legend"
        :transform="`translate(${width - 100}, ${paddingTop})`"
      >
        <template
          v-for="(item, i) in legendItems"
          :key="i"
        >
          <rect
            :y="i * 20"
            width="12"
            height="12"
            :fill="item.color"
          />
          <text
            :y="i * 20 + 10"
            x="18"
            class="legend-text"
          >{{ item.name }}</text>
        </template>
      </g>
    </svg>
    
    <!-- Tooltip -->
    <div
      v-if="tooltip.show"
      class="tooltip"
      :style="{ left: tooltip.x, top: tooltip.y }"
    >
      {{ tooltip.content }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { ChartDataPoint, ChartSeries } from './types'

const props = withDefaults(defineProps<{
  title?: string
  data?: ChartDataPoint[]
  series?: ChartSeries[]
  width?: number
  height?: number
  colors?: string[]
  showLegend?: boolean
  smooth?: boolean
}>(), {
  width: 400,
  height: 300,
  series: () => [],
  showLegend: true,
  smooth: false
})

const emit = defineEmits<{
  click: [data: ChartDataPoint]
  hover: [data: ChartDataPoint]
}>()

const tooltip = ref({ show: false, x: '0px', y: '0px', content: '' })

const paddingLeft = 60
const paddingRight = 120
const paddingTop = 40
const paddingBottom = 40

const chartWidth = computed(() => props.width - paddingLeft - paddingRight)
const chartHeight = computed(() => props.height - paddingTop - paddingBottom)

// 支持单系列和多系列
const normalizedSeries = computed(() => {
  if (props.series && props.series.length > 0) {
    return props.series
  }
  if (props.data && props.data.length > 0) {
    return [{ name: 'Series 1', data: props.data }]
  }
  return []
})

const xLabels = computed(() => {
  const firstSeries = normalizedSeries.value[0]
  return firstSeries?.data.map(d => d.name) || []
})

const allValues = computed(() => {
  return normalizedSeries.value.flatMap(s => s.data.map(d => d.value))
})

const maxValue = computed(() => Math.max(...allValues.value) * 1.2)
const minValue = computed(() => Math.min(0, ...allValues.value))

const pointGap = computed(() => chartWidth.value / Math.max(1, xLabels.value.length - 1))

const defaultColors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272']

const lines = computed(() => {
  return normalizedSeries.value.map((s, si) => {
    const color = s.color || props.colors?.[si] || defaultColors[si % defaultColors.length]
    const points = s.data.map((d, i) => {
      const x = pointGap.value * i
      const y = chartHeight.value - ((d.value - minValue.value) / (maxValue.value - minValue.value)) * chartHeight.value
      return { x, y, data: d }
    })
    
    // 生成路径
    let path = ''
    if (points.length > 0) {
      path = `M ${points[0].x} ${points[0].y}`
      for (let i = 1; i < points.length; i++) {
        if (props.smooth) {
          // 贝塞尔曲线
          const prev = points[i - 1]
          const curr = points[i]
          const cx = (prev.x + curr.x) / 2
          path += ` C ${cx} ${prev.y}, ${cx} ${curr.y}, ${curr.x} ${curr.y}`
        } else {
          path += ` L ${points[i].x} ${points[i].y}`
        }
      }
    }
    
    return { name: s.name, color, points, path }
  })
})

const yTicks = computed(() => {
  const ticks = []
  const range = maxValue.value - minValue.value
  const step = range / 5
  for (let i = 0; i <= 5; i++) {
    const value = Math.round(minValue.value + step * i)
    ticks.push({
      value,
      pos: ((value - minValue.value) / range) * chartHeight.value
    })
  }
  return ticks
})

const legendItems = computed(() => {
  return normalizedSeries.value.map((s, i) => ({
    name: s.name,
    color: s.color || props.colors?.[i] || defaultColors[i % defaultColors.length]
  }))
})

const handleHover = (e: MouseEvent, point: any) => {
  tooltip.value = {
    show: true,
    x: `${e.offsetX + 10}px`,
    y: `${e.offsetY + 10}px`,
    content: `${point.data.name}: ${point.data.value}`
  }
  emit('hover', point.data)
}

const handleOut = () => {
  tooltip.value.show = false
}

const handleClick = (point: any) => {
  emit('click', point.data)
}
</script>

<style scoped>
.line-chart {
  position: relative;
  display: inline-block;
}

.chart-title {
  font-size: 16px;
  font-weight: bold;
  fill: #333;
}

.tick-text {
  font-size: 12px;
  fill: #666;
}

.legend-text {
  font-size: 12px;
  fill: #333;
}

.line-path {
  transition: d 0.3s ease;
}

.data-point {
  cursor: pointer;
  transition: r 0.2s;
}

.data-point:hover {
  r: 6;
}

.tooltip {
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
