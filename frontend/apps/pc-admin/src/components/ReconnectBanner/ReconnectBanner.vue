<template>
  <transition name="slide-down">
    <div v-if="!online" class="reconnect-banner" role="alert">
      <WifiOutlined class="reconnect-banner__icon" />
      <span class="reconnect-banner__text">
        网络连接已断开，正在尝试重新连接{{ retryCount > 0 ? `（第 ${retryCount} 次重试）` : '...' }}
      </span>
      <a-button size="small" type="link" class="reconnect-banner__dismiss" @click="dismiss">
        忽略
      </a-button>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { WifiOutlined } from '@ant-design/icons-vue'

const online = ref(navigator.onLine)
const retryCount = ref(0)
let retryTimer: ReturnType<typeof setInterval> | null = null

function handleOnline() {
  online.value = true
  retryCount.value = 0
  if (retryTimer) {
    clearInterval(retryTimer)
    retryTimer = null
  }
}

function handleOffline() {
  online.value = false
  retryCount.value = 0
  // 自动重试检测
  retryTimer = setInterval(() => {
    retryCount.value++
    if (navigator.onLine) {
      handleOnline()
    }
  }, 5000)
}

function dismiss() {
  online.value = true // 临时隐藏
  if (retryTimer) {
    clearInterval(retryTimer)
    retryTimer = null
  }
}

onMounted(() => {
  window.addEventListener('online', handleOnline)
  window.addEventListener('offline', handleOffline)
})

onUnmounted(() => {
  window.removeEventListener('online', handleOnline)
  window.removeEventListener('offline', handleOffline)
  if (retryTimer) {
    clearInterval(retryTimer)
  }
})
</script>

<style scoped>
.reconnect-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #fff7e6;
  border-bottom: 1px solid #ffd591;
  font-size: 13px;
  color: #d46b08;
}

.reconnect-banner__icon {
  font-size: 16px;
  flex-shrink: 0;
}

.reconnect-banner__text {
  flex: 1;
}

.reconnect-banner__dismiss {
  flex-shrink: 0;
  color: #d46b08;
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
}

.slide-down-enter-from,
.slide-down-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}
</style>
