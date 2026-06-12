<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const emit = defineEmits<{
  complete: [image: string]
}>()

const canvasRef = ref<HTMLCanvasElement | null>(null)
const wrapperRef = ref<HTMLDivElement | null>(null)
let ctx: CanvasRenderingContext2D | null = null
let isDrawing = false

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return
  ctx = canvas.getContext('2d')
  resize()
  window.addEventListener('resize', resize)
})

onUnmounted(() => {
  window.removeEventListener('resize', resize)
})

const resize = () => {
  const wrapper = wrapperRef.value
  const canvas = canvasRef.value
  if (!wrapper || !canvas) return
  canvas.width = wrapper.clientWidth
  canvas.height = 200
  if (ctx) {
    ctx.strokeStyle = '#333'
    ctx.lineWidth = 3
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'
  }
}

const startDrawing = (e: MouseEvent | TouchEvent) => {
  isDrawing = true
  const pos = getPos(e)
  if (pos && ctx) {
    ctx.beginPath()
    ctx.moveTo(pos.x, pos.y)
  }
}

const draw = (e: MouseEvent | TouchEvent) => {
  if (!isDrawing) return
  e.preventDefault()
  const pos = getPos(e)
  if (pos && ctx) {
    ctx.lineTo(pos.x, pos.y)
    ctx.stroke()
  }
}

const stopDrawing = () => {
  if (isDrawing) {
    isDrawing = false
    const canvas = canvasRef.value
    if (canvas) {
      emit('complete', canvas.toDataURL('image/png'))
    }
  }
}

const getPos = (e: MouseEvent | TouchEvent) => {
  const canvas = canvasRef.value
  if (!canvas) return null
  const rect = canvas.getBoundingClientRect()
  if ('touches' in e) {
    const touch = e.touches[0]
    return { x: touch.clientX - rect.left, y: touch.clientY - rect.top }
  }
  return { x: e.clientX - rect.left, y: e.clientY - rect.top }
}

const clear = () => {
  if (ctx && canvasRef.value) {
    ctx.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height)
    emit('complete', '')
  }
}
</script>

<template>
  <div ref="wrapperRef" class="signature-pad">
    <canvas
      ref="canvasRef"
      @mousedown="startDrawing"
      @mousemove="draw"
      @mouseup="stopDrawing"
      @mouseleave="stopDrawing"
      @touchstart="startDrawing"
      @touchmove="draw"
      @touchend="stopDrawing"
    />
    <div class="signature-actions">
      <span class="clear-btn" @click="clear">清除</span>
    </div>
  </div>
</template>

<style scoped>
.signature-pad {
  position: relative;
  border: 1px dashed #ddd;
  border-radius: 4px;
}

canvas {
  display: block;
  width: 100%;
  height: 200px;
  cursor: crosshair;
}

.signature-actions {
  position: absolute;
  top: 8px;
  right: 8px;
}

.clear-btn {
  font-size: 12px;
  color: #999;
  cursor: pointer;
}
</style>
