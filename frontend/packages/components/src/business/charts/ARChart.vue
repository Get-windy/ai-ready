<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'

interface ChartData {
  labels: string[]
  values: number[]
  colors?: string[]
}

interface ChartProps {
  type: 'line' | 'bar' | 'pie' | 'area'
  data: ChartData
  width?: number
  height?: number
  showLegend?: boolean
  showGrid?: boolean
  animated?: boolean
  title?: string
}

const props = withDefaults(defineProps<ChartProps>(), {
  width: 400,
  height: 300,
  showLegend: true,
  showGrid: true,
  animated: true
})

const canvasRef = ref<HTMLCanvasElement | null>(null)
const ctx = ref<CanvasRenderingContext2D | null>(null)
const animationProgress = ref(0)

const defaultColors = [
  '#1988fa', '#07c160', '#ff976a', '#7232dd', '#f44',
  '#969799', '#ffd21e', '#00bcd4', '#9c27b0', '#4caf50'
]

const colors = computed(() => props.data.colors || defaultColors)

onMounted(() => {
  initCanvas()
  if (props.animated) {
    animateChart()
  } else {
    animationProgress.value = 1
    drawChart()
  }
})

watch(() => props.data, () => {
  if (props.animated) {
    animationProgress.value = 0
    animateChart()
  } else {
    drawChart()
  }
}, { deep: true })

const initCanvas = () => {
  if (!canvasRef.value) return
  
  ctx.value = canvasRef.value.getContext('2d')
  canvasRef.value.width = props.width
  canvasRef.value.height = props.height
}

const animateChart = () => {
  const duration = 500
  const startTime = Date.now()
  
  const animate = () => {
    const elapsed = Date.now() - startTime
    animationProgress.value = Math.min(elapsed / duration, 1)
    drawChart()
    
    if (animationProgress.value < 1) {
      requestAnimationFrame(animate)
    }
  }
  
  animate()
}

const drawChart = () => {
  if (!ctx.value || !canvasRef.value) return
  
  const context = ctx.value
  const width = canvasRef.value.width
  const height = canvasRef.value.height
  
  context.clearRect(0, 0, width, height)
  
  const padding = { top: 40, right: 20, bottom: 40, left: 50 }
  const chartWidth = width - padding.left - padding.right
  const chartHeight = height - padding.top - padding.bottom
  
  if (props.title) {
    context.fillStyle = '#333'
    context.font = '14px sans-serif'
    context.textAlign = 'center'
    context.fillText(props.title, width / 2, 20)
  }
  
  if (props.type === 'line' || props.type === 'bar' || props.type === 'area') {
    drawAxisChart(context, padding, chartWidth, chartHeight)
  } else if (props.type === 'pie') {
    drawPieChart(context, width, height)
  }
  
  if (props.showLegend && props.type !== 'pie') {
    drawLegend(context, width, height)
  }
}

const drawAxisChart = (
  context: CanvasRenderingContext2D,
  padding: { top: number; right: number; bottom: number; left: number },
  chartWidth: number,
  chartHeight: number
) => {
  const maxValue = Math.max(...props.data.values)
  const minValue = Math.min(0, ...props.data.values)
  const valueRange = maxValue - minValue
  
  if (props.showGrid) {
    context.strokeStyle = '#ebedf0'
    context.lineWidth = 1
    
    for (let i = 0; i <= 5; i++) {
      const y = padding.top + (chartHeight / 5) * i
      context.beginPath()
      context.moveTo(padding.left, y)
      context.lineTo(padding.left + chartWidth, y)
      context.stroke()
      
      const value = maxValue - (valueRange / 5) * i
      context.fillStyle = '#969799'
      context.font = '10px sans-serif'
      context.textAlign = 'right'
      context.fillText(value.toFixed(0), padding.left - 5, y + 3)
    }
  }
  
  context.strokeStyle = '#dcdfe6'
  context.beginPath()
  context.moveTo(padding.left, padding.top + chartHeight)
  context.lineTo(padding.left + chartWidth, padding.top + chartHeight)
  context.stroke()
  
  const barWidth = chartWidth / props.data.labels.length
  const points: { x: number; y: number }[] = []
  
  props.data.values.forEach((value, index) => {
    const x = padding.left + barWidth * index + barWidth / 2
    const normalizedValue = (value - minValue) / valueRange
    const y = padding.top + chartHeight - normalizedValue * chartHeight * animationProgress.value
    
    points.push({ x, y })
    
    if (props.type === 'bar') {
      const barHeight = normalizedValue * chartHeight * animationProgress.value
      context.fillStyle = colors.value[index % colors.value.length]
      context.fillRect(
        x - barWidth / 3,
        padding.top + chartHeight - barHeight,
        barWidth / 1.5,
        barHeight
      )
    }
    
    context.fillStyle = '#333'
    context.font = '10px sans-serif'
    context.textAlign = 'center'
    context.fillText(props.data.labels[index], x, padding.top + chartHeight + 15)
  })
  
  if (props.type === 'line' || props.type === 'area') {
    if (props.type === 'area') {
      context.beginPath()
      context.moveTo(points[0].x, padding.top + chartHeight)
      points.forEach(p => context.lineTo(p.x, p.y))
      context.lineTo(points[points.length - 1].x, padding.top + chartHeight)
      context.closePath()
      context.fillStyle = colors.value[0] + '40'
      context.fill()
    }
    
    context.beginPath()
    context.strokeStyle = colors.value[0]
    context.lineWidth = 2
    points.forEach((p, i) => {
      if (i === 0) {
        context.moveTo(p.x, p.y)
      } else {
        context.lineTo(p.x, p.y)
      }
    })
    context.stroke()
    
    points.forEach(p => {
      context.beginPath()
      context.fillStyle = colors.value[0]
      context.arc(p.x, p.y, 3, 0, Math.PI * 2)
      context.fill()
    })
  }
}

const drawPieChart = (context: CanvasRenderingContext2D, width: number, height: number) => {
  const centerX = width / 2
  const centerY = height / 2
  const radius = Math.min(width, height) / 2 - 40
  
  const total = props.data.values.reduce((sum, v) => sum + Math.abs(v), 0)
  let currentAngle = -Math.PI / 2
  
  props.data.values.forEach((value, index) => {
    const sliceAngle = (Math.abs(value) / total) * Math.PI * 2 * animationProgress.value
    
    context.beginPath()
    context.moveTo(centerX, centerY)
    context.arc(centerX, centerY, radius, currentAngle, currentAngle + sliceAngle)
    context.closePath()
    context.fillStyle = colors.value[index % colors.value.length]
    context.fill()
    
    currentAngle += sliceAngle
  })
  
  context.beginPath()
  context.arc(centerX, centerY, radius * 0.4, 0, Math.PI * 2)
  context.fillStyle = '#fff'
  context.fill()
  
  if (props.showLegend) {
    const legendY = height - 20
    const legendWidth = 80
    const startX = (width - props.data.labels.length * legendWidth) / 2
    
    props.data.labels.forEach((label, index) => {
      const x = startX + index * legendWidth
      
      context.fillStyle = colors.value[index % colors.value.length]
      context.fillRect(x, legendY - 8, 12, 12)
      
      context.fillStyle = '#333'
      context.font = '10px sans-serif'
      context.textAlign = 'left'
      context.fillText(label, x + 16, legendY)
    })
  }
}

const drawLegend = (context: CanvasRenderingContext2D, width: number, height: number) => {
  if (props.type === 'line' || props.type === 'area') {
    context.fillStyle = colors.value[0]
    context.fillRect(width - 80, 10, 12, 12)
    
    context.fillStyle = '#333'
    context.font = '10px sans-serif'
    context.textAlign = 'left'
    context.fillText(props.data.labels[0] || '数据', width - 65, 18)
  }
}
</script>

<template>
  <div class="ar-chart">
    <canvas ref="canvasRef" :style="{ width: `${width}px`, height: `${height}px` }"></canvas>
  </div>
</template>

<style lang="scss" scoped>
.ar-chart {
  display: inline-block;
  
  canvas {
    display: block;
  }
}
</style>