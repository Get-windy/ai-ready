<script setup lang="ts">
defineProps<{
  tasks: any[]
}>()
</script>

<template>
  <div class="task-list">
    <div v-if="tasks.length === 0" class="empty">暂无任务</div>
    <div
      v-for="task in tasks"
      :key="task.id"
      class="task-row"
    >
      <div class="task-info">
        <span class="task-name">{{ task.templateName || task.taskNo || task.id }}</span>
        <span class="task-status" :class="task.status">
          {{ task.status === 'pending' ? '待打印' : task.status === 'printing' ? '打印中' : task.status === 'completed' ? '已完成' : '失败' }}
        </span>
      </div>
      <div class="task-time">{{ task.createTime ? new Date(task.createTime).toLocaleString('zh-CN') : '' }}</div>
    </div>
  </div>
</template>

<style scoped>
.task-list {
  min-height: 100px;
}

.empty {
  text-align: center;
  padding: 32px 0;
  color: #969799;
  font-size: 14px;
}

.task-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}

.task-row:last-child {
  border-bottom: none;
}

.task-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.task-name {
  font-size: 14px;
  color: #333;
}

.task-status {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.task-status.pending {
  background: #fff1e6;
  color: #ff976a;
}

.task-status.printing {
  background: #e6f7ff;
  color: #1988fa;
}

.task-status.completed {
  background: #e6f9ed;
  color: #07c160;
}

.task-status.failed {
  background: #ffe6e6;
  color: #f44;
}

.task-time {
  font-size: 12px;
  color: #c0c4cc;
}
</style>
