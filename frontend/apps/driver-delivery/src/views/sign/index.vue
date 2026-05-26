<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const orderId = ref(route.query.orderId as string || '')
const canvasRef = ref<HTMLCanvasElement | null>(null)
const hasSignature = ref(false)
const signerName = ref('')
const signerPhone = ref('')
const signTime = ref('')
const remark = ref('')
const isDrawing = ref(false)
const lastX = ref(0)
const lastY = ref(0)
const ctx = ref<CanvasRenderingContext2D | null>(null)

onMounted(() => {
  initCanvas()
  signTime.value = new Date().toLocaleString('zh-CN')
})

const initCanvas = () => {
  if (!canvasRef.value) return
  
  ctx.value = canvasRef.value.getContext('2d')
  if (!ctx.value) return
  
  ctx.value.fillStyle = '#fff'
  ctx.value.fillRect(0, 0, canvasRef.value.width, canvasRef.value.height)
  ctx.value.strokeStyle = '#333'
  ctx.value.lineWidth = 2
  ctx.value.lineCap = 'round'
  ctx.value.lineJoin = 'round'
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
  isDrawing.value = true
  lastX.value = x
  lastY.value = y
  hasSignature.value = true
}

const draw = (x: number, y: number) => {
  if (!isDrawing.value || !ctx.value) return
  
  ctx.value.beginPath()
  ctx.value.moveTo(lastX.value, lastY.value)
  ctx.value.lineTo(x, y)
  ctx.value.stroke()
  
  lastX.value = x
  lastY.value = y
}

const stopDrawing = () => {
  isDrawing.value = false
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

const clearSignature = () => {
  if (!ctx.value || !canvasRef.value) return
  
  ctx.value.fillStyle = '#fff'
  ctx.value.fillRect(0, 0, canvasRef.value.width, canvasRef.value.height)
  hasSignature.value = false
}

const confirmSignature = async () => {
  if (!hasSignature.value) {
    alert('请先签名')
    return
  }
  
  if (!signerName.value) {
    alert('请填写签收人姓名')
    return
  }
  
  if (!canvasRef.value) return
  
  const imageData = canvasRef.value.toDataURL('image/png')
  
  try {
    await window.electronAPI?.delivery?.submitSignature?.({
      orderId: orderId.value,
      signerName: signerName.value,
      signerPhone: signerPhone.value,
      signTime: signTime.value,
      signatureImage: imageData,
      remark: remark.value
    })
    
    alert('签收成功')
    router.push('/order')
  } catch (err) {
    alert('签收失败: ' + err)
  }
}

const takePhoto = async () => {
  try {
    const result = await window.electronAPI?.camera?.takePhoto?.()
    if (result) {
      alert('照片已保存')
    }
  } catch {
    alert('拍照功能暂不可用')
  }
}
</script>

<template>
  <div class="sign-page">
    <div class="page-header">
      <h1>签收确认</h1>
      <span class="order-id">订单: {{ orderId }}</span>
    </div>
    
    <div class="sign-container">
      <div class="sign-header">
        <span class="title">请在下方签名</span>
        <button class="clear-btn" @click="clearSignature">
          <svg viewBox="0 0 24 24" width="16" height="16">
            <path fill="currentColor" d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
          </svg>
          清除
        </button>
      </div>
      
      <div class="canvas-wrapper">
        <canvas 
          ref="canvasRef"
          class="signature-canvas"
          width="300"
          height="150"
          @touchstart="handleTouchStart"
          @touchmove="handleTouchMove"
          @touchend="handleTouchEnd"
          @mousedown="handleMouseDown"
          @mousemove="handleMouseMove"
          @mouseup="handleMouseUp"
          @mouseleave="handleMouseUp"
        ></canvas>
        
        <div v-if="!hasSignature" class="placeholder">
          请在此处签名
        </div>
      </div>
      
      <div class="sign-info">
        <div class="info-row">
          <label>签收人姓名</label>
          <input 
            v-model="signerName"
            type="text"
            placeholder="请输入姓名"
            required
          />
        </div>
        
        <div class="info-row">
          <label>联系电话</label>
          <input 
            v-model="signerPhone"
            type="tel"
            placeholder="请输入电话"
          />
        </div>
        
        <div class="info-row">
          <label>签收时间</label>
          <input 
            v-model="signTime"
            type="text"
            readonly
          />
        </div>
        
        <div class="info-row">
          <label>备注</label>
          <textarea 
            v-model="remark"
            placeholder="如有问题请备注"
            rows="2"
          ></textarea>
        </div>
      </div>
      
      <div class="photo-section">
        <button class="photo-btn" @click="takePhoto">
          <svg viewBox="0 0 24 24" width="20" height="20">
            <path fill="currentColor" d="M12 15.2c2.1 0 3.8-1.7 3.8-3.8s-1.7-3.8-3.8-3.8-3.8 1.7-3.8 3.8 1.7 3.8 3.8 3.8zM9 2L7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2H9zm3 15c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5z"/>
          </svg>
          拍照留证
        </button>
        <p class="photo-tip">可拍摄货物照片作为签收凭证</p>
      </div>
    </div>
    
    <div class="sign-footer">
      <button class="cancel-btn" @click="router.back()">
        取消
      </button>
      <button 
        class="confirm-btn"
        :disabled="!hasSignature || !signerName"
        @click="confirmSignature"
      >
        确认签收
      </button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.sign-page {
  padding: 16px;
  background: #f7f8fa;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  
  h1 {
    font-size: 18px;
    font-weight: 600;
    color: #333;
  }
  
  .order-id {
    font-size: 12px;
    color: #969799;
  }
}

.sign-container {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  
  .sign-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    
    .title {
      font-size: 14px;
      font-weight: 600;
      color: #333;
    }
    
    .clear-btn {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 4px 8px;
      background: none;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      color: #969799;
      font-size: 12px;
      cursor: pointer;
    }
  }
  
  .canvas-wrapper {
    position: relative;
    margin-bottom: 16px;
    
    .signature-canvas {
      width: 100%;
      height: 150px;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      background: #fff;
      touch-action: none;
    }
    
    .placeholder {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      color: #969799;
      font-size: 14px;
      pointer-events: none;
    }
  }
  
  .sign-info {
    .info-row {
      margin-bottom: 12px;
      
      label {
        display: block;
        font-size: 12px;
        color: #666;
        margin-bottom: 4px;
      }
      
      input, textarea {
        width: 100%;
        padding: 10px 12px;
        border: 1px solid #dcdfe6;
        border-radius: 4px;
        font-size: 14px;
        
        &:focus {
          border-color: #1988fa;
        }
        
        &[readonly] {
          background: #f7f8fa;
          color: #969799;
        }
      }
      
      textarea {
        resize: none;
      }
    }
  }
  
  .photo-section {
    margin-top: 16px;
    text-align: center;
    
    .photo-btn {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 12px 24px;
      background: #f7f8fa;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      font-size: 14px;
      color: #333;
      cursor: pointer;
      
      &:active {
        background: #ebedf0;
      }
    }
    
    .photo-tip {
      font-size: 12px;
      color: #969799;
      margin-top: 8px;
    }
  }
}

.sign-footer {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  
  .cancel-btn, .confirm-btn {
    flex: 1;
    padding: 14px;
    border: none;
    border-radius: 8px;
    font-size: 16px;
    cursor: pointer;
  }
  
  .cancel-btn {
    background: #f7f8fa;
    color: #333;
  }
  
  .confirm-btn {
    background: #07c160;
    color: #fff;
    
    &:disabled {
      background: #969799;
      cursor: not-allowed;
    }
    
    &:not(:disabled):active {
      background: #06ad56;
    }
  }
}
</style>