<template>
  <div class="ar-mobile-scanner">
    <div class="scanner-container">
      <video 
        ref="videoRef"
        class="scanner-video"
        autoplay
        playsinline
      ></video>
      
      <div class="scanner-overlay">
        <div class="scan-frame">
          <div class="corner top-left"></div>
          <div class="corner top-right"></div>
          <div class="corner bottom-left"></div>
          <div class="corner bottom-right"></div>
          <div class="scan-line" :class="{ scanning: isScanning }"></div>
        </div>
        
        <div class="scan-tip">
          {{ tip }}
        </div>
      </div>
      
      <div v-if="lastResult" class="scan-result">
        <div class="result-content">
          <span class="result-label">扫描结果:</span>
          <span class="result-value">{{ lastResult }}</span>
        </div>
      </div>
    </div>
    
    <div class="scanner-controls">
      <button 
        class="control-btn"
        :class="{ active: isScanning }"
        @click="toggleScanning"
      >
        <svg viewBox="0 0 24 24" width="24" height="24">
          <path fill="currentColor" d="M9.5 6.5v3h-3v-3h3M11 5H5v6h6V5zm-1.5 9.5v3h-3v-3h3M11 13H5v6h6v-6zm6.5-6.5v3h-3v-3h3M19 5h-6v6h6V5zm-6 8h1.5v1.5H13V13zm1.5 1.5H16V16h-1.5v-1.5zM16 13h1.5v1.5H16V13zm-3 3h1.5v1.5H13V16zm1.5 1.5H16V19h-1.5v-1.5zM16 16h1.5v1.5H16V16zm1.5-1.5H19V16h-1.5v-1.5zm0 3H19V19h-1.5v-1.5zM19 16h.5v.5H19V16z"/>
        </svg>
        <span>{{ isScanning ? '扫描中' : '开始扫描' }}</span>
      </button>
      
      <button 
        class="control-btn"
        @click="toggleFlash"
      >
        <svg viewBox="0 0 24 24" width="24" height="24">
          <path fill="currentColor" d="M7 2v11h3v9l7-12h-4l4-8z"/>
        </svg>
        <span>{{ flashOn ? '关闭闪光' : '打开闪光' }}</span>
      </button>
      
      <button 
        class="control-btn"
        @click="handleManualInput"
      >
        <svg viewBox="0 0 24 24" width="24" height="24">
          <path fill="currentColor" d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/>
        </svg>
        <span>手动输入</span>
      </button>
    </div>
    
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="showManualInput" class="manual-input-overlay" @click="showManualInput = false">
          <div class="manual-input-panel" @click.stop>
            <div class="panel-header">
              <span>手动输入条码</span>
              <button @click="showManualInput = false">
                <svg viewBox="0 0 24 24" width="20" height="20">
                  <path fill="currentColor" d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
                </svg>
              </button>
            </div>
            <div class="panel-body">
              <input 
                v-model="manualValue"
                type="text"
                placeholder="请输入条码/二维码内容"
                class="manual-input"
                @keyup.enter="handleManualSubmit"
              />
              <button class="submit-btn" @click="handleManualSubmit">
                确定
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

interface Props {
  tip?: string
  scanTypes?: ('barcode' | 'qrcode')[]
  vibrate?: boolean
  beep?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  tip: '将条码/二维码放入框内即可自动扫描',
  scanTypes: () => ['barcode', 'qrcode'],
  vibrate: true,
  beep: true
})

const emit = defineEmits<{
  (e: 'scan', result: string, type: string): void
  (e: 'error', error: Error): void
}>()

const videoRef = ref<HTMLVideoElement | null>(null)
const isScanning = ref(false)
const flashOn = ref(false)
const lastResult = ref<string>('')
const showManualInput = ref(false)
const manualValue = ref<string>('')

let mediaStream: MediaStream | null = null
let animationFrameId: number | null = null
let lastScanTime: number = 0

onMounted(async () => {
  await initCamera()
})

onUnmounted(() => {
  stopScanning()
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop())
  }
})

const initCamera = async () => {
  try {
    const constraints = {
      video: {
        facingMode: 'environment',
        width: { ideal: 1280 },
        height: { ideal: 720 }
      }
    }
    
    mediaStream = await navigator.mediaDevices.getUserMedia(constraints)
    
    if (videoRef.value) {
      videoRef.value.srcObject = mediaStream
      await videoRef.value.play()
    }
  } catch (error) {
    emit('error', error as Error)
  }
}

const toggleScanning = () => {
  if (isScanning.value) {
    stopScanning()
  } else {
    startScanning()
  }
}

const startScanning = () => {
  isScanning.value = true
  scanLoop()
}

const stopScanning = () => {
  isScanning.value = false
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = null
  }
}

const scanLoop = () => {
  if (!isScanning.value || !videoRef.value) return
  
  const now = Date.now()
  if (now - lastScanTime > 500) {
    lastScanTime = now
    performScan()
  }
  
  animationFrameId = requestAnimationFrame(scanLoop)
}

const performScan = async () => {
  if (!videoRef.value) return
  
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  
  canvas.width = videoRef.value.videoWidth
  canvas.height = videoRef.value.videoHeight
  
  ctx?.drawImage(videoRef.value, 0, 0)
  
  try {
    const imageData = ctx?.getImageData(0, 0, canvas.width, canvas.height)
    
    if (imageData) {
      const result = await decodeBarcode(imageData)
      
      if (result) {
        handleScanResult(result)
      }
    }
  } catch (error) {
    console.error('扫描错误:', error)
  }
}

const decodeBarcode = async (imageData: ImageData): Promise<string | null> => {
  return null
}

const handleScanResult = (result: string) => {
  lastResult.value = result
  
  if (props.vibrate && navigator.vibrate) {
    navigator.vibrate(100)
  }
  
  if (props.beep) {
    playBeep()
  }
  
  emit('scan', result, 'barcode')
  
  stopScanning()
}

const playBeep = () => {
  const audioContext = new AudioContext()
  const oscillator = audioContext.createOscillator()
  const gainNode = audioContext.createGain()
  
  oscillator.connect(gainNode)
  gainNode.connect(audioContext.destination)
  
  oscillator.frequency.value = 1000
  oscillator.type = 'sine'
  
  gainNode.gain.value = 0.3
  
  oscillator.start()
  oscillator.stop(audioContext.currentTime + 0.1)
}

const toggleFlash = async () => {
  if (!mediaStream) return
  
  const track = mediaStream.getVideoTracks()[0]
  
  try {
    const capabilities = track.getCapabilities()
    
    if ('torch' in capabilities) {
      flashOn.value = !flashOn.value
      await track.applyConstraints({
        advanced: [{ torch: flashOn.value }]
      })
    }
  } catch (error) {
    console.error('闪光灯控制失败:', error)
  }
}

const handleManualInput = () => {
  showManualInput.value = true
  stopScanning()
}

const handleManualSubmit = () => {
  if (manualValue.value.trim()) {
    emit('scan', manualValue.value.trim(), 'manual')
    lastResult.value = manualValue.value.trim()
    showManualInput.value = false
    manualValue.value = ''
  }
}
</script>

<style lang="scss" scoped>
.ar-mobile-scanner {
  position: relative;
  width: 100%;
  height: 100%;
  background: #000;
  
  .scanner-container {
    position: relative;
    width: 100%;
    height: 70vh;
    
    .scanner-video {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    
    .scanner-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      
      .scan-frame {
        width: 250px;
        height: 250px;
        position: relative;
        
        .corner {
          position: absolute;
          width: 20px;
          height: 20px;
          border: 3px solid #07c160;
          
          &.top-left {
            top: 0;
            left: 0;
            border-right: none;
            border-bottom: none;
          }
          
          &.top-right {
            top: 0;
            right: 0;
            border-left: none;
            border-bottom: none;
          }
          
          &.bottom-left {
            bottom: 0;
            left: 0;
            border-right: none;
            border-top: none;
          }
          
          &.bottom-right {
            bottom: 0;
            right: 0;
            border-left: none;
            border-top: none;
          }
        }
        
        .scan-line {
          position: absolute;
          top: 0;
          left: 10px;
          right: 10px;
          height: 2px;
          background: linear-gradient(to right, transparent, #07c160, transparent);
          
          &.scanning {
            animation: scan 2s linear infinite;
          }
        }
      }
      
      .scan-tip {
        position: absolute;
        bottom: 30px;
        color: #fff;
        font-size: 14px;
        text-align: center;
        opacity: 0.8;
      }
    }
    
    .scan-result {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      padding: 16px;
      background: rgba(0, 0, 0, 0.8);
      
      .result-content {
        display: flex;
        align-items: center;
        
        .result-label {
          color: #969799;
          font-size: 12px;
        }
        
        .result-value {
          color: #07c160;
          font-size: 16px;
          font-weight: 600;
          margin-left: 8px;
        }
      }
    }
  }
  
  .scanner-controls {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 16px;
    background: #fff;
    display: flex;
    justify-content: space-around;
    
    .control-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 12px;
      background: none;
      border: none;
      color: #333;
      cursor: pointer;
      
      &.active {
        color: #07c160;
      }
      
      svg {
        margin-bottom: 4px;
      }
      
      span {
        font-size: 12px;
      }
    }
  }
}

@keyframes scan {
  0% {
    top: 0;
  }
  100% {
    top: calc(100% - 2px);
  }
}

.manual-input-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  
  .manual-input-panel {
    width: 90%;
    max-width: 400px;
    background: #fff;
    border-radius: 8px;
    
    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      border-bottom: 1px solid #ebedf0;
      
      span {
        font-size: 16px;
        font-weight: 600;
      }
      
      button {
        background: none;
        border: none;
        color: #969799;
        cursor: pointer;
      }
    }
    
    .panel-body {
      padding: 16px;
      
      .manual-input {
        width: 100%;
        padding: 12px;
        border: 1px solid #dcdfe6;
        border-radius: 4px;
        font-size: 16px;
        
        &:focus {
          border-color: #1988fa;
        }
      }
      
      .submit-btn {
        width: 100%;
        margin-top: 12px;
        padding: 12px;
        background: #1988fa;
        color: #fff;
        border: none;
        border-radius: 4px;
        font-size: 16px;
        cursor: pointer;
        
        &:active {
          background: #0e7cd3;
        }
      }
    }
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>