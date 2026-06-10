<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import StatusIndicator from '@/components/common/StatusIndicator.vue'

const props = defineProps<{
  connectionStatus: 'connected' | 'disconnected' | 'connecting'
}>()

const router = useRouter()
const currentTime = ref('')

onMounted(() => {
  updateTime()
  setInterval(updateTime, 30000)
})

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const handleMinimize = () => {
  window.electronAPI.app.minimize()
}

const handleQuit = () => {
  window.electronAPI.app.quit()
}

const handleGoSettings = () => {
  router.push('/settings')
}
</script>

<template>
  <header class="header">
    <div class="header-left">
      <StatusIndicator :status="connectionStatus" />
    </div>
    <div class="header-center">
      <span class="time">{{ currentTime }}</span>
    </div>
    <div class="header-right">
      <button class="header-btn" title="设置" @click="handleGoSettings">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M12 1v2m0 18v2M4.22 4.22l1.42 1.42m12.72 12.72l1.42 1.42M1 12h2m18 0h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42"/></svg>
      </button>
      <button class="header-btn" title="最小化" @click="handleMinimize">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="5" y1="12" x2="19" y2="12"/></svg>
      </button>
      <button class="header-btn header-btn-close" title="关闭" @click="handleQuit">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
      </button>
    </div>
  </header>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid #ebedf0;
  -webkit-app-region: drag;
  flex-shrink: 0;
}

.header-left,
.header-center,
.header-right {
  display: flex;
  align-items: center;
  -webkit-app-region: no-drag;
}

.header-center {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

.time {
  font-size: 13px;
  color: #969799;
}

.header-right {
  gap: 4px;
  margin-left: auto;
}

.header-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  border-radius: 6px;
  color: #666;
  cursor: pointer;
  transition: all 0.15s;
}

.header-btn:hover {
  background: #f5f5f5;
}

.header-btn-close:hover {
  background: #f44;
  color: #fff;
}
</style>
