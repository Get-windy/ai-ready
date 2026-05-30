<template>
  <div class="ar-kanban-view">
    <div class="ar-kanban-toolbar">
      <div class="ar-kanban-toolbar-left">
        <slot name="toolbar-left" />
      </div>
      <div class="ar-kanban-toolbar-right">
        <button
          class="ar-kanban-refresh-btn"
          @click="handleRefresh"
        >
          <span class="ar-kanban-refresh-icon">🔄</span>
          <span class="ar-kanban-refresh-text">刷新</span>
        </button>
        <slot name="toolbar-right" />
      </div>
    </div>

    <div class="ar-kanban-board">
      <div
        v-for="column in columns"
        :key="column.status"
        class="ar-kanban-column-wrapper"
      >
        <KanbanColumn
          :status="column.status"
          :title="column.title"
          :status-icon="column.icon"
          :cards="getCardsByStatus(column.status)"
          :show-add-button="showAddButton"
          :color="column.color"
          @card-click="handleCardClick"
          @add="handleAdd"
        >
          <template #card="{ card }">
            <slot
              name="card"
              :card="card"
            />
          </template>
        </KanbanColumn>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import KanbanColumn from './KanbanColumn.vue'

interface KanbanCardData {
  id: string | number
  status: number
  [key: string]: any
}

interface KanbanColumnConfig {
  status: number
  title: string
  icon?: string
  color?: string
}

interface Props {
  data?: KanbanCardData[]
  columns?: KanbanColumnConfig[]
  showAddButton?: boolean
  statusField?: string
}

const props = withDefaults(defineProps<Props>(), {
  data: () => [],
  columns: () => [
    { status: 0, title: '草稿', icon: '📝', color: '#909399' },
    { status: 1, title: '待审批', icon: '⏳', color: '#e6a23c' },
    { status: 2, title: '已审批', icon: '✅', color: '#67c23a' },
    { status: 3, title: '进行中', icon: '🔄', color: '#409eff' },
    { status: 4, title: '完成', icon: '🎉', color: '#52c41a' }
  ],
  showAddButton: false,
  statusField: 'status'
})

const emit = defineEmits<{
  cardClick: [card: KanbanCardData]
  add: [status: number]
  refresh: []
}>()

const getCardsByStatus = (status: number) => {
  return props.data.filter(card => card[props.statusField] === status)
}

const handleCardClick = (card: KanbanCardData) => {
  emit('cardClick', card)
}

const handleAdd = (status: number) => {
  emit('add', status)
}

const handleRefresh = () => {
  emit('refresh')
}
</script>

<style lang="scss" scoped>
.ar-kanban-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--ar-bg-color-page, #f2f3f5);
}

.ar-kanban-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--ar-spacing-md, 12px) var(--ar-spacing-lg, 16px);
  background-color: var(--ar-bg-color, #ffffff);
  border-bottom: 1px solid var(--ar-border-color-light, #e4e7ed);
}

.ar-kanban-toolbar-left {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-sm, 8px);
}

.ar-kanban-toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-sm, 8px);
}

.ar-kanban-refresh-btn {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-xs, 4px);
  padding: var(--ar-spacing-xs, 4px) var(--ar-spacing-sm, 8px);
  border: 1px solid var(--ar-border-color-base, #dcdfe6);
  background-color: transparent;
  color: var(--ar-text-color-regular, #606266);
  font-size: var(--ar-font-size-small, 13px);
  border-radius: var(--ar-border-radius-base, 4px);
  cursor: pointer;
  transition: all var(--ar-transition-duration, 0.2s);

  &:hover {
    border-color: var(--ar-color-primary, #409eff);
    color: var(--ar-color-primary, #409eff);
  }
}

.ar-kanban-refresh-icon {
  font-size: var(--ar-font-size-base, 14px);
}

.ar-kanban-refresh-text {
  font-size: var(--ar-font-size-small, 13px);
}

.ar-kanban-board {
  flex: 1;
  display: flex;
  gap: var(--ar-spacing-md, 12px);
  padding: var(--ar-spacing-md, 12px);
  overflow-x: auto;
  overflow-y: hidden;
}

.ar-kanban-column-wrapper {
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .ar-kanban-toolbar {
    padding: var(--ar-spacing-sm, 8px);
  }

  .ar-kanban-board {
    padding: var(--ar-spacing-sm, 8px);
    gap: var(--ar-spacing-sm, 8px);
  }
}
</style>