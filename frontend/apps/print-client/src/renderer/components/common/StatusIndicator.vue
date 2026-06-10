<script setup lang="ts">
defineProps<{
  status: 'connected' | 'disconnected' | 'connecting'
}>()
</script>

<template>
  <div class="status-indicator" :class="status">
    <span class="dot"></span>
    <span class="text">
      {{ status === 'connected' ? '已连接' : status === 'connecting' ? '连接中' : '未连接' }}
    </span>
  </div>
</template>

<style scoped>
.status-indicator {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.connected {
  background: #e6f7ff;
  color: #1988fa;
}
.connected .dot {
  background: #1988fa;
}

.connecting {
  background: #fff1e6;
  color: #ff976a;
}
.connecting .dot {
  background: #ff976a;
  animation: pulse 1s ease-in-out infinite;
}

.disconnected {
  background: #ffe6e6;
  color: #f44;
}
.disconnected .dot {
  background: #f44;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
</style>
