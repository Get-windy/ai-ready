<template>
  <div class="ar-mobile-table">
    <!-- 卡片式布局（移动端） -->
    <div 
      v-for="row in data" 
      :key="row.id"
      class="ar-mobile-table__card"
    >
      <!-- 卡片头部 -->
      <div class="ar-mobile-table__card-header">
        <span class="ar-mobile-table__card-title">
          {{ getCellValue(row, titleField) }}
        </span>
        <el-tag 
          :type="getStatusType(getCellValue(row, statusField))"
          size="small"
        >
          {{ getCellValue(row, statusField) }}
        </el-tag>
      </div>
      
      <!-- 卡片内容 -->
      <div class="ar-mobile-table__card-body">
        <div 
          v-for="col in displayColumns" 
          :key="col.key"
          class="ar-mobile-table__card-item"
        >
          <span class="ar-mobile-table__card-label">
            {{ col.title }}
          </span>
          <span class="ar-mobile-table__card-value">
            {{ getCellValue(row, col.key) }}
          </span>
        </div>
      </div>
      
      <!-- 卡片操作按钮 -->
      <div class="ar-mobile-table__card-actions">
        <el-button 
          type="text" 
          size="small"
          @click="handleAction('view', row)"
        >
          查看
        </el-button>
        <el-button 
          type="text" 
          size="small"
          @click="handleAction('edit', row)"
        >
          编辑
        </el-button>
        <el-button 
          type="text" 
          size="small"
          @click="handleAction('delete', row)"
          style="color: #f56c6c"
        >
          删除
        </el-button>
      </div>
    </div>
    
    <!-- 空状态 -->
    <ar-empty 
      v-if="data.length === 0" 
      description="暂无数据"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

export interface Column {
  key: string
  title: string
}

const props = defineProps<{
  data: any[]
  columns: Column[]
  titleField?: string
  statusField?: string
}>()

const emit = defineEmits<{
  action: [action: string, row: any]
}>()

// 显示字段（排除标题和状态字段）
const displayColumns = computed(() => {
  return props.columns.filter(
    col => col.key !== props.titleField && col.key !== props.statusField
  )
})

const getCellValue = (row: any, key: string) => {
  return row[key]
}

const getStatusType = (status: string) => {
  const statusMap: Record<string, string> = {
    '正常': 'success',
    'active': 'success',
    '警告': 'warning',
    'error': 'danger',
    'completed': 'success',
    'pending': 'warning',
  }
  return statusMap[status] || 'info'
}

const handleAction = (action: string, row: any) => {
  emit('action', action, row)
}
</script>

<style scoped>
.ar-mobile-table {
  padding: 12px;
}

.ar-mobile-table__card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin-bottom: 12px;
  overflow: hidden;
  transition: transform 0.2s;
}

.ar-mobile-table__card:active {
  transform: scale(0.98);
}

/* 卡片头部 */
.ar-mobile-table__card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid var(--ar-border-light);
}

.ar-mobile-table__card-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--ar-text-primary);
}

/* 卡片内容 */
.ar-mobile-table__card-body {
  padding: 12px;
}

.ar-mobile-table__card-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed var(--ar-border-lighter);
}

.ar-mobile-table__card-item:last-child {
  border-bottom: none;
}

.ar-mobile-table__card-label {
  font-size: 13px;
  color: var(--ar-text-secondary);
}

.ar-mobile-table__card-value {
  font-size: 14px;
  color: var(--ar-text-primary);
  font-weight: 500;
  text-align: right;
}

/* 卡片操作 */
.ar-mobile-table__card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid var(--ar-border-light);
}

/* 深色模式 */
[data-theme='dark'] .ar-mobile-table__card {
  background: #1f1f1f;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

[data-theme='dark'] .ar-mobile-table__card-header,
[data-theme='dark'] .ar-mobile-table__card-actions {
  border-color: #333;
}

[data-theme='dark'] .ar-mobile-table__card-item {
  border-color: #333;
}

[data-theme='dark'] .ar-mobile-table__card-title,
[data-theme='dark'] .ar-mobile-table__card-value {
  color: var(--ar-text-primary);
}

[data-theme='dark'] .ar-mobile-table__card-label {
  color: var(--ar-text-secondary);
}
</style>