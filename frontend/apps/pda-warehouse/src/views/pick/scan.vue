<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NavBar, Button, showToast, showLoadingToast, closeToast } from 'vant'
import { api } from '@/api'
import QrScanner from 'qr-scanner'

const router = useRouter()
const route = useRoute()

const videoRef = ref<HTMLVideoElement | null>(null)
const scanner = ref<QrScanner | null>(null)
const scannedCode = ref<string>('')
const isScanning = ref(true)

const taskId = Number(route.query.taskId) || 0
const itemId = Number(route.query.itemId) || 0

onMounted(async () => {
  if (videoRef.value) {
    scanner.value = new QrScanner(
      videoRef.value,
      (result) => {
        handleScanResult(result)
      },
      {
        highlightScanRegion: true,
        highlightCodeOutline: true
      }
    )
    scanner.value.start()
  }
})

onUnmounted(() => {
  if (scanner.value) {
    scanner.value.stop()
    scanner.value.destroy()
  }
})

const handleScanResult = async (result: QrScanner.ScanResult) => {
  if (!isScanning.value) return

  isScanning.value = false
  scannedCode.value = result.data

  showLoadingToast({ message: '验证中...', forbidClick: true })

  try {
    const res: any = await api.pick.verifyScan(taskId, itemId, result.data)

    if (res.valid) {
      showToast('扫码成功')
      router.back()
    } else {
      showToast(res.message || '商品不匹配')
      isScanning.value = true
    }
  } catch (error) {
    showToast('验证失败')
    isScanning.value = true
  } finally {
    closeToast()
  }
}

const handleManualInput = () => {
  router.push({
    path: '/pick/manual',
    query: { taskId: String(taskId), itemId: String(itemId) }
  })
}

const handleFlashToggle = () => {
  if (scanner.value) {
    scanner.value.toggleFlash()
  }
}
</script>

<template>
  <div class="scan-page">
    <NavBar
      title="扫码拣货"
      left-arrow
      @click-left="router.back()"
    />

    <div class="scanner-container">
      <video ref="videoRef" class="scanner-video"></video>

      <div class="scan-overlay">
        <div class="scan-region"></div>
        <div class="scan-tip">请扫描商品条码</div>
      </div>

      <div v-if="scannedCode" class="scanned-result">
        <div class="result-label">扫描结果:</div>
        <div class="result-code">{{ scannedCode }}</div>
      </div>
    </div>

    <div class="action-buttons">
      <Button
        type="primary"
        size="large"
        icon="flash-o"
        @click="handleFlashToggle"
      >
        切换闪光灯
      </Button>
      <Button
        type="default"
        size="large"
        icon="edit"
        @click="handleManualInput"
      >
        手动输入
      </Button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.scan-page {
  min-height: 100vh;
  background: #000;
}

.scanner-container {
  position: relative;
  height: 60vh;

  .scanner-video {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  .scan-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    justify-content: center;
    align-items: center;

    .scan-region {
      width: 250px;
      height: 250px;
      border: 2px solid #07c160;
      border-radius: 8px;
      position: relative;

      &::before,
      &::after {
        content: '';
        position: absolute;
        width: 30px;
        height: 30px;
        border: 3px solid #07c160;
      }

      &::before {
        top: -3px;
        left: -3px;
        border-right: none;
        border-bottom: none;
      }

      &::after {
        bottom: -3px;
        right: -3px;
        border-left: none;
        border-top: none;
      }
    }

    .scan-tip {
      position: absolute;
      bottom: 20px;
      color: #fff;
      font-size: 14px;
      text-align: center;
    }
  }

  .scanned-result {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 16px;
    background: rgba(0, 0, 0, 0.8);
    color: #fff;

    .result-label {
      font-size: 12px;
      color: #969799;
    }

    .result-code {
      font-size: 18px;
      font-weight: 600;
      margin-top: 4px;
    }
  }
}

.action-buttons {
  padding: 16px;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
