<script setup lang="ts">
const props = defineProps<{
  task: any
}>()

const emit = defineEmits<{
  click: []
  print: []
  cancel: []
  retry: []
}>()

const statusMap: Record<string, { label: string; color: string }> = {
  pending: { label: '待打印', color: '#ff976a' },
  printing: { label: '打印中', color: '#1988fa' },
  completed: { label: '已完成', color: '#07c160' },
  failed: { label: '失败', color: '#f44' }
}
</script>

<template>
  <div class="task-item" @click="emit('click')">
    <div class="task-main">
      <div class="task-title">{{ task.templateName || task.taskNo || task.id }}</div>
      <div class="task-meta">
        <span class="task-status" :style="{ color: (statusMap[task.status] || {}).color || '#969799' }">
          {{ (statusMap[task.status] || { label: task.status }).label }}
        </span>
        <span class="task-printer" v-if="task.printerName">{{ task.printerName }}</span>
        <span class="task-time">{{ task.createTime ? new Date(task.createTime).toLocaleString('zh-CN') : '' }}</span>
      </div>
    </div>
    <div class="task-actions" @click.stop>
      <button
        v-if="task.status === 'pending' || task.status === 'printing'"
        class="action-btn cancel"
        @click="emit('cancel')"
        title="取消"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
      </button>
      <button
        v-if="task.status === 'pending'"
        class="action-btn print"
        @click="emit('print')"
        title="打印"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 18H4a2 2 0 01-2-2v-5a2 2 0 012-2h16a2 2 0 012 2v5a2 2 0 01-2 2h-2"/><path d="M6 9V3h12v6"/><rect x="6" y="14" width="12" height="7" rx="1"/></svg>
      </button>
      <button
        v-if="task.status === 'failed'"
        class="action-btn retry"
        @click="emit('retry')"
        title="重试"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 4v6h6"/><path d="M3.51 15a9 9 0 102.13-9.36L1 10"/></svg>
      </button>
    </div>
  </div>
</template>

<style scoped>
.task-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.15s;
}

.task-item:hover {
  background: #fafafa;
}

.task-item:last-child {
  border-bottom: none;
}

.task-main {
  flex: 1;
  min-width: 0;
}

.task-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  align-items: center;
}

.task-status {
  font-weight: 500;
}

.task-printer {
  color: #969799;
}

.task-time {
  color: #c0c4cc;
}

.task-actions {
  display: flex;
  gap: 4px;
  margin-left: 12px;
  flex-shrink: 0;
}

.action-btn {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}

.action-btn.print {
  background: #e6f7ff;
  color: #1988fa;
}

.action-btn.print:hover {
  background: #1988fa;
  color: #fff;
}

.action-btn.cancel {
  background: #ffe6e6;
  color: #f44;
}

.action-btn.cancel:hover {
  background: #f44;
  color: #fff;
}

.action-btn.retry {
  background: #fff1e6;
  color: #ff976a;
}

.action-btn.retry:hover {
  background: #ff976a;
  color: #fff;
}
</style>
