<template>
  <div class="ar-mobile-signature">
    <div class="signature-header">
      <span class="title">{{ title }}</span>
      <div class="header-actions">
        <button class="action-btn" @click="handleClear">
          <svg viewBox="0 0 24 24" width="16" height="16">
            <path fill="currentColor" d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
          </svg>
          清除
        </button>
      </div>
    </div>
    
    <div class="signature-container">
      <canvas 
        ref="canvasRef"
        class="signature-canvas"
        :width="canvasWidth"
        :height="canvasHeight"
        @touchstart="handleTouchStart"
        @touchmove="handleTouchMove"
        @touchend="handleTouchEnd"
        @mousedown="handleMouseDown"
        @mousemove="handleMouseMove"
        @mouseup="handleMouseUp"
        @mouseleave="handleMouseUp"
      ></canvas>
      
      <div v-if="!hasSignature" class="signature-placeholder">
        请在此处签名
      </div>
    </div>
    
    <div class="signature-footer">
      <button class="footer-btn clear" @click="handleClear">
        清除签名
      </button>
      <button class="footer-btn confirm" @click="handleConfirm">
        确认签名
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

interface Props {
  title?: string
  width?: number
  height?: number
  lineWidth?: number
  strokeColor?: string
  backgroundColor?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '签名',
  width: 300,
  height: 150,
  lineWidth: 2,
  strokeColor: '#333',
  backgroundColor: '#fff'
})

const emit = defineEmits<{
  (e: 'complete', image: string): void
  (e: 'clear'): void
}>()

const canvasRef = ref<HTMLCanvasElement | null>(null)
const canvasWidth = ref(props.width)
const canvasHeight = ref(props.height)
const hasSignature = ref(false)

let ctx: CanvasRenderingContext2D | null = null
let isDrawing = false
let lastX = 0
let lastY = 0

onMounted(() => {
  initCanvas()
})

onUnmounted(() => {
  ctx = null
})

const initCanvas = () => {
  if (!canvasRef.value) return
  
  ctx = canvasRef.value.getContext('2d')
  if (!ctx) return
  
  ctx.fillStyle = props.backgroundColor
  ctx.fillRect(0, 0, canvasWidth.value, canvasHeight.value)
  ctx.strokeStyle = props.strokeColor
  ctx.lineWidth = props.lineWidth
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
}

const getCoordinates = (event: TouchEvent | MouseEvent): { x: number; y: number } => {
  if (!canvasRef.value) return { x: 0, y: 0 }
  
  const rect = canvasRef.value.getBoundingClientRect()
  
  if (event instanceof TouchEvent) {
    const touch = event.touches[0] || event.changedTouches[0]
    return {
      x: touch.clientX - rect.left,
      y: touch.clientY - rect.top
    }
  } else {
    return {
      x: event.clientX - rect.left,
      y: event.clientY - rect.top
    }
  }
}

const startDrawing = (x: number, y: number) => {
  isDrawing = true
  lastX = x
  lastY = y
  hasSignature.value = true
}

const draw = (x: number, y: number) => {
  if (!isDrawing || !ctx) return
  
  ctx.beginPath()
  ctx.moveTo(lastX, lastY)
  ctx.lineTo(x, y)
  ctx.stroke()
  
  lastX = x
  lastY = y
}

const stopDrawing = () => {
  isDrawing = false
}

const handleTouchStart = (event: TouchEvent) => {
  event.preventDefault()
  const coords = getCoordinates(event)
  startDrawing(coords.x, coords.y)
}

const handleTouchMove = (event: TouchEvent) => {
  event.preventDefault()
  const coords = getCoordinates(event)
  draw(coords.x, coords.y)
}

const handleTouchEnd = (event: TouchEvent) => {
  event.preventDefault()
  stopDrawing()
}

const handleMouseDown = (event: MouseEvent) => {
  const coords = getCoordinates(event)
  startDrawing(coords.x, coords.y)
}

const handleMouseMove = (event: MouseEvent) => {
  const coords = getCoordinates(event)
  draw(coords.x, coords.y)
}

const handleMouseUp = () => {
  stopDrawing()
}

const handleClear = () => {
  if (!ctx) return
  
  ctx.fillStyle = props.backgroundColor
  ctx.fillRect(0, 0, canvasWidth.value, canvasHeight.value)
  hasSignature.value = false
  emit('clear')
}

const handleConfirm = () => {
  if (!canvasRef.value || !hasSignature.value) return
  
  const imageData = canvasRef.value.toDataURL('image/png')
  emit('complete', imageData)
}
</script>

<style lang="scss" scoped>
.ar-mobile-signature {
  background: #fff;
  border-radius: 8px;
  
  .signature-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    border-bottom: 1px solid #ebedf0;
    
    .title {
      font-size: 16px;
      font-weight: 600;
      color: #333;
    }
    
    .header-actions {
      .action-btn {
        display: flex;
        align-items: center;
        padding: 4px 8px;
        background: none;
        border: none;
        color: #969799;
        cursor: pointer;
        
        svg {
          margin-right: 4px;
        }
        
        &:hover {
          color: #333;
        }
      }
    }
  }
  
  .signature-container {
    position: relative;
    padding: 16px;
    
    .signature-canvas {
      width: 100%;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      touch-action: none;
    }
    
    .signature-placeholder {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      color: #969799;
      font-size: 14px;
      pointer-events: none;
    }
  }
  
  .signature-footer {
    display: flex;
    gap: 12px;
    padding: 12px 16px;
    border-top: 1px solid #ebedf0;
    
    .footer-btn {
      flex: 1;
      padding: 10px;
      border: none;
      border-radius: 4px;
      font-size: 14px;
      cursor: pointer;
      
      &.clear {
        background: #f7f8fa;
        color: #333;
        
        &:hover {
          background: #ebedf0;
        }
      }
      
      &.confirm {
        background: #1988fa;
        color: #fff;
        
        &:hover {
          background: #0e7cd3;
        }
      }
    }
  }
}
</style>