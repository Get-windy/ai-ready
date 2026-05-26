<template>
  <div
    ref="containerRef"
    class="radar-chart"
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
      
      <!-- 雷达图区域 -->
      <g
        class="radar-area"
        :transform="`translate(${cx}, ${cy})`"
      >
        <!-- 背景网格 -->
        <g class="grid">
          <polygon
            v-for="(polygon, i) in gridPolygons"
            :key="'grid-' + i"
            :points="polygon"
            fill="none"
            stroke="#e0e0e0"
            stroke-width="1"
          />
        </g>
        
        <!-- 轴线 -->
        <g class="axes">
          <line
            v-for="(axis, i) in axes"
            :key="'axis-' + i"
            :x1="0"
            :y1="0"
            :x2="axis.x"
            :y2="axis.y"
            stroke="#e0e0e0"
            stroke-width="1"
          />
        </g>
        
        <!-- 数据区域 -->
        <g class="data">
          <!-- 填充区域 -->
          <polygon
            v-for="(area, i) in dataAreas"
            :key="'area-' + i"
            :points="area.points"
            :fill="area.color"
            fill-opacity="0.2"
            :stroke="area.color"
            stroke-width="2"
            class="data-polygon"
          />
          
          <!-- 数据点 -->
          <g
            v-for="(area, ai) in dataAreas"
            :key="'points-' + ai"
          >
            <circle
              v-for="(point, pi) in area.points"
              :key="pi"
              :cx="point.x"
              :cy="point.y"
              r="4"
              :fill="area.color"
              class="data-point"
              @mouseover="handleHover($event, point)"
              @mouseout="handleOut"
              @click="handleClick(point)"
            />
          </g>
        </g>
        
        <!-- 标签 -->
        <g class="labels">
          <text
            v-for="(label, i) in labels"
            :key="i"
            :x="label.x"
            :y="label.y"
            text-anchor="middle"
            class="axis-label"
          >
            {{ label.text }}
          </text>
        </g>
      </g>
      
      <!-- 图例 -->
      <g
        v-if="showLegend && series.length > 1"
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
import type { ChartDataPoint, ChartSeries } from './types'

const props = withDefaults(defineProps<{
  title?: string
  data?: ChartDataPoint[]
  series?: ChartSeries[]
  width?: number
  height?: number
  colors?: string[]
  showLegend?: boolean
}>(), {
  width: 400,
  height: 300,
  series: () => [],
  showLegend: true
})

const emit = defineEmits<{
  click: [data: ChartDataPoint]
  hover: [data: ChartDataPoint]
}>()

const tooltip = ref({ show: false, x: '0px', y: '0px', content: '' })

const paddingTop = 40
const paddingRight = 140

// 计算中心点
const cx = computed(() => (props.width - paddingRight) / 2)
const cy = computed(() => props.height / 2)

// 半径
const radius = computed(() => Math.min(cx.value, cy.value) - paddingTop - 10)

// 数据
const normalizedSeries = computed(() => {
  if (props.series && props.series.length > 0) {
    return props.series
  }
  if (props.data && props.data.length > 0) {
    return [{ name: 'Series 1', data: props.data }]
  }
  return []
})

const categories = computed(() => normalizedSeries.value[0]?.data.map(d => d.name) || [])

const defaultColors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272']

// 计算网格多边形
const gridPolygons = computed(() => {
  const levels = 5
  const polygons = []
  
  for (let level = 1; level <= levels; level++) {
    const r = (radius.value / levels) * level
    const points = categories.value.map((_, i) => {
      const angle = (Math.PI * 2 / categories.value.length) * i - Math.PI / 2
      return {
        x: Math.cos(angle) * r,
        y: Math.sin(angle) * r
      }
    })
    polygons.push(points.map(p => `${p.x},${p.y}`).join(' '))
  }
  
  return polygons
})

// 轴线
const axes = computed(() => {
  return categories.value.map((_, i) => {
    const angle = (Math.PI * 2 / categories.value.length) * i - Math.PI / 2
    return {
      x: Math.cos(angle) * radius.value,
      y: Math.sin(angle) * radius.value
    }
  })
})

// 数据区域
const dataAreas = computed(() => {
  const maxValues = normalizedSeries.value.map(s => Math.max(...s.data.map(d => d.value)))
  const maxValue = Math.max(...maxValues)
  
  return normalizedSeries.value.map((s, si) => {
    const color = s.color || props.colors?.[si] || defaultColors[si % defaultColors.length]
    
    const points = s.data.map((d, i) => {
      const angle = (Math.PI * 2 / categories.value.length) * i - Math.PI / 2
      const r = (d.value / maxValue) * radius.value
      return {
        x: Math.cos(angle) * r,
        y: Math.sin(angle) * r,
        data: d
      }
    })
    
    return {
      name: s.name,
      color,
      points
    }
  })
})

// 标签
const labels = computed(() => {
  return categories.value.map((cat, i) => {
    const angle = (Math.PI * 2 / categories.value.length) * i - Math.PI / 2
    const r = radius.value + 20
    return {
      x: Math.cos(angle) * r,
      y: Math.sin(angle) * r,
      text: cat
    }
  })
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
.radar-chart {
  position: relative;
  display: inline-block;
}

.chart-title {
  font-size: 16px;
  font-weight: bold;
  fill: #333;
}

.axis-label {
  font-size: 12px;
  fill: #333;
}

.legend-text {
  font-size: 12px;
  fill: #333;
}

.data-polygon {
  transition: opacity 0.2s;
}

.data-polygon:hover {
  opacity: 0.8;
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