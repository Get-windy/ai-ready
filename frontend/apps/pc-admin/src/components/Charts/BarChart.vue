<template>
  <div
    ref="containerRef"
    class="bar-chart"
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
            :x="barWidth * i + barWidth / 2"
            y="20"
            text-anchor="middle"
            class="tick-text"
          >
            {{ label }}
          </text>
        </template>
      </g>
      
      <!-- 柱状图 -->
      <g
        class="bars"
        :transform="`translate(${paddingLeft}, ${paddingTop})`"
      >
        <rect
          v-for="(bar, i) in bars"
          :key="i"
          :x="bar.x"
          :y="bar.y"
          :width="bar.width"
          :height="bar.height"
          :fill="bar.color"
          class="bar"
          @mouseover="handleHover($event, bar)"
          @mouseout="handleOut"
          @click="handleClick(bar)"
        />
      </g>
      
      <!-- 图例 -->
      <g
        v-if="showLegend"
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
import { ref, computed, onMounted } from 'vue'
import type { ChartDataPoint, ChartConfig } from './types'

const props = withDefaults(defineProps<{
  title?: string
  data: ChartDataPoint[]
  width?: number
  height?: number
  config?: ChartConfig
  colors?: string[]
  showLegend?: boolean
  showValue?: boolean
}>(), {
  width: 400,
  height: 300,
  showLegend: true,
  showValue: false
})

const emit = defineEmits<{
  click: [data: ChartDataPoint]
  hover: [data: ChartDataPoint]
}>()

const containerRef = ref<HTMLElement>()
const tooltip = ref({ show: false, x: '0px', y: '0px', content: '' })

const paddingLeft = 60
const paddingRight = 120
const paddingTop = 40
const paddingBottom = 40

const chartWidth = computed(() => props.width - paddingLeft - paddingRight)
const chartHeight = computed(() => props.height - paddingTop - paddingBottom)

const maxValue = computed(() => Math.max(...props.data.map(d => d.value)) * 1.2)

const xLabels = computed(() => props.data.map(d => d.name))

const barWidth = computed(() => chartWidth.value / props.data.length)

const defaultColors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272']

const bars = computed(() => {
  return props.data.map((d, i) => {
    const height = (d.value / maxValue.value) * chartHeight.value
    return {
      x: barWidth.value * i + 5,
      y: chartHeight.value - height,
      width: barWidth.value - 10,
      height: height,
      color: d.color || props.colors?.[i] || defaultColors[i % defaultColors.length],
      name: d.name,
      value: d.value,
      data: d
    }
  })
})

const yTicks = computed(() => {
  const ticks = []
  const step = maxValue.value / 5
  for (let i = 0; i <= 5; i++) {
    const value = Math.round(step * i)
    ticks.push({
      value,
      pos: (value / maxValue.value) * chartHeight.value
    })
  }
  return ticks
})

const legendItems = computed(() => {
  return props.data.map((d, i) => ({
    name: d.name,
    color: d.color || props.colors?.[i] || defaultColors[i % defaultColors.length]
  }))
})

const handleHover = (e: MouseEvent, bar: any) => {
  tooltip.value = {
    show: true,
    x: `${e.offsetX + 10}px`,
    y: `${e.offsetY + 10}px`,
    content: `${bar.name}: ${bar.value}`
  }
  emit('hover', bar.data)
}

const handleOut = () => {
  tooltip.value.show = false
}

const handleClick = (bar: any) => {
  emit('click', bar.data)
}
</script>

<style scoped>
.bar-chart {
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

.bar {
  cursor: pointer;
  transition: opacity 0.2s;
}

.bar:hover {
  opacity: 0.8;
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
