<script setup lang="ts">
defineProps<{
  printers: any[]
}>()
</script>

<template>
  <div class="printer-status-list">
    <div v-if="printers.length === 0" class="empty">暂无打印机</div>
    <div
      v-for="printer in printers"
      :key="printer.name"
      class="printer-row"
    >
      <div class="printer-dot" :class="printer.status || 'online'"></div>
      <span class="printer-name">{{ printer.name }}</span>
    </div>
  </div>
</template>

<style scoped>
.printer-status-list {
  min-height: 60px;
}

.empty {
  text-align: center;
  padding: 24px 0;
  color: #969799;
  font-size: 14px;
}

.printer-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
}

.printer-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.printer-dot.online {
  background: #07c160;
}

.printer-dot.offline {
  background: #969799;
}

.printer-dot.error {
  background: #f44;
}

.printer-name {
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
