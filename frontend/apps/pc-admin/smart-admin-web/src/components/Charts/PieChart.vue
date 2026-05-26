<template>
  <div
    ref="containerRef"
    class="pie-chart"
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
      
      <!-- 饼图 -->
      <g
        class="pie-slices"
        :transform="`translate(${cx}, ${cy})`"
      >
        <path
          v-for="(slice, i) in slices"
          :key="i"
          :d="slice.path"
          :fill="slice.color"
          class="pie-slice"
          @mouseover="handleHover($event, slice)"
          @mouseout="handleOut"
          @click="handleClick(slice)"
        />
        
        <!-- 选中效果 -->
        <path
          v-for="(slice, i) in slices"
          :key="'selected-' + i"
          :d="slice.path"
          fill="none"
          stroke="#fff"
          stroke-width="2"
          class="pie-slice-border"
        />
      </g>
      
      <!-- 数据标签 -->
      <g
        class="labels"
        :transform="`translate(${cx}, ${cy})`"
      >
        <text
          v-for="(label, i) in labels"
          :key="i"
          :x="label.x"
          :y="label.y"
          text-anchor="middle"
          class="slice-label"
        >
          {{ label.text }}
        </text>
      </g>
      
      <!-- 图例 -->
      <g
        v-if="showLegend"
        class="legend"
        :transform="`translate(${width - 120}, ${paddingTop})`"
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
import type { ChartDataPoint } from './types'

const props = withDefaults(defineProps<{
  title?: string
  data: ChartDataPoint[]
  width?: number
  height?: number
  colors?: string[]
  showLegend?: boolean
  showLabel?: boolean
  innerRadius?: number
}>(), {
  width: 400,
  height: 300,
  showLegend: true,
  showLabel: true,
  innerRadius: 0
})

const emit = defineEmits<{
  click: [data: ChartDataPoint]
  hover: [data: ChartDataPoint]
}>()

const tooltip = ref({ show: false, x: '0px', y: '0px', content: '' })

const paddingTop = 40
const paddingRight = 140

// 计算圆心位置
const cx = computed(() => (props.width - paddingRight) / 2)
const cy = computed(() => props.height / 2)

// 计算半径
const radius = computed(() => Math.min(cx.value, cy.value) - paddingTop)

const total = computed(() => props.data.reduce((sum, d) => sum + d.value, 0))

const defaultColors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']

const slices = computed(() => {
  let startAngle = -Math.PI / 2 // 从顶部开始
  
  return props.data.map((d, i) => {
    const angle = (d.value / total.value) * Math.PI * 2
    const endAngle = startAngle + angle
    
    // 计算扇形路径
    const x1 = Math.cos(startAngle) * radius.value
    const y1 = Math.sin(startAngle) * radius.value
    const x2 = Math.cos(endAngle) * radius.value
    const y2 = Math.sin(endAngle) * radius.value
    
    // 大弧标志
    const largeArcFlag = angle > Math.PI ? 1 : 0
    
    let path: string
    if (props.innerRadius > 0) {
      // 环形图
      const innerR = radius.value * props.innerRadius
      const ix1 = Math.cos(startAngle) * innerR
      const iy1 = Math.sin(startAngle) * innerR
      const ix2 = Math.cos(endAngle) * innerR
      const iy2 = Math.sin(endAngle) * innerR
      
      path = `M ${x1} ${y1} A ${radius.value} ${radius.value} 0 ${largeArcFlag} 1 ${x2} ${y2} L ${ix2} ${iy2} A ${innerR} ${innerR} 0 ${largeArcFlag} 0 ${ix1} ${iy1} Z`
    } else {
      // 普通饼图
      path = `M 0 0 L ${x1} ${y1} A ${radius.value} ${radius.value} 0 ${largeArcFlag} 1 ${x2} ${y2} Z`
    }
    
    const result = {
      path,
      color: d.color || props.colors?.[i] || defaultColors[i % defaultColors.length],
      name: d.name,
      value: d.value,
      percentage: ((d.value / total.value) * 100).toFixed(1),
      data: d,
      midAngle: startAngle + angle / 2
    }
    
    startAngle = endAngle
    return result
  })
})

const labels = computed(() => {
  if (!props.showLabel) return []
  
  return slices.value.map(slice => {
    const labelRadius = radius.value + 20
    const x = Math.cos(slice.midAngle) * labelRadius
    const y = Math.sin(slice.midAngle) * labelRadius
    return {
      x,
      y,
      text: `${slice.percentage}%`
    }
  })
})

const legendItems = computed(() => {
  return props.data.map((d, i) => ({
    name: `${d.name} (${((d.value / total.value) * 100).toFixed(1)}%)`,
    color: d.color || props.colors?.[i] || defaultColors[i % defaultColors.length]
  }))
})

const handleHover = (e: MouseEvent, slice: any) => {
  tooltip.value = {
    show: true,
    x: `${e.offsetX + 10}px`,
    y: `${e.offsetY + 10}px`,
    content: `${slice.name}: ${slice.value} (${slice.percentage}%)`
  }
  emit('hover', slice.data)
}

const handleOut = () => {
  tooltip.value.show = false
}

const handleClick = (slice: any) => {
  emit('click', slice.data)
}
</script>

<style scoped>
.pie-chart {
  position: relative;
  display: inline-block;
}

.chart-title {
  font-size: 16px;
  font-weight: bold;
  fill: #333;
}

.slice-label {
  font-size: 11px;
  fill: #666;
}

.legend-text {
  font-size: 12px;
  fill: #333;
}

.pie-slice {
  cursor: pointer;
  transition: transform 0.2s, opacity 0.2s;
}

.pie-slice:hover {
  opacity: 0.85;
}

.pie-slice-border {
  pointer-events: none;
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
