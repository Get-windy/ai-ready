<template>
  <div class="ar-kanban-column">
    <div class="ar-kanban-column-header">
      <div class="ar-kanban-column-title">
        <span class="ar-kanban-column-icon">{{ statusIcon }}</span>
        <span class="ar-kanban-column-text">{{ title }}</span>
      </div>
      <div class="ar-kanban-column-count">
        <span class="ar-kanban-column-badge">{{ cards.length }}</span>
      </div>
    </div>

    <div class="ar-kanban-column-body">
      <div
        v-for="card in cards"
        :key="card.id"
        class="ar-kanban-card-wrapper"
      >
        <slot
          name="card"
          :card="card"
        >
          <KanbanCard
            :card="card"
            @click="handleCardClick(card)"
          />
        </slot>
      </div>

      <div
        v-if="cards.length === 0"
        class="ar-kanban-column-empty"
      >
        <span class="ar-kanban-column-empty-text">暂无数据</span>
      </div>
    </div>

    <div
      v-if="showAddButton"
      class="ar-kanban-column-footer"
    >
      <button
        class="ar-kanban-column-add-btn"
        @click="handleAdd"
      >
        <span class="ar-kanban-column-add-icon">+</span>
        <span class="ar-kanban-column-add-text">添加</span>
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import KanbanCard from './KanbanCard.vue'

interface KanbanCardData {
  id: string | number
  title: string
  status: number
  [key: string]: any
}

interface Props {
  status: number
  title: string
  statusIcon?: string
  cards?: KanbanCardData[]
  showAddButton?: boolean
  color?: string
}

const props = withDefaults(defineProps<Props>(), {
  statusIcon: '',
  cards: () => [],
  showAddButton: false,
  color: '#409eff'
})

const emit = defineEmits<{
  cardClick: [card: KanbanCardData]
  add: [status: number]
}>()

const handleCardClick = (card: KanbanCardData) => {
  emit('cardClick', card)
}

const handleAdd = () => {
  emit('add', props.status)
}
</script>

<style lang="scss" scoped>
.ar-kanban-column {
  display: flex;
  flex-direction: column;
  min-width: 280px;
  max-width: 320px;
  background-color: var(--ar-fill-color-light, #f5f7fa);
  border-radius: var(--ar-border-radius-base, 4px);
  overflow: hidden;
}

.ar-kanban-column-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--ar-spacing-md, 12px) var(--ar-spacing-lg, 16px);
  background-color: var(--ar-bg-color, #ffffff);
  border-bottom: 1px solid var(--ar-border-color-light, #e4e7ed);
}

.ar-kanban-column-title {
  display: flex;
  align-items: center;
  gap: var(--ar-spacing-sm, 8px);
}

.ar-kanban-column-icon {
  font-size: var(--ar-font-size-medium, 16px);
}

.ar-kanban-column-text {
  font-size: var(--ar-font-size-base, 14px);
  font-weight: var(--ar-font-weight-primary, 500);
  color: var(--ar-text-color-primary, #303133);
}

.ar-kanban-column-count {
  display: flex;
  align-items: center;
}

.ar-kanban-column-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 var(--ar-spacing-sm, 8px);
  background-color: var(--ar-fill-color, #f0f2f5);
  color: var(--ar-text-color-regular, #606266);
  font-size: var(--ar-font-size-small, 13px);
  border-radius: var(--ar-border-radius-round, 20px);
}

.ar-kanban-column-body {
  flex: 1;
  padding: var(--ar-spacing-sm, 8px);
  overflow-y: auto;
  max-height: calc(100vh - 200px);
}

.ar-kanban-card-wrapper {
  margin-bottom: var(--ar-spacing-sm, 8px);
}

.ar-kanban-column-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--ar-spacing-xl, 20px);
}

.ar-kanban-column-empty-text {
  font-size: var(--ar-font-size-small, 13px);
  color: var(--ar-text-color-placeholder, #c0c4cc);
}

.ar-kanban-column-footer {
  padding: var(--ar-spacing-sm, 8px);
  background-color: var(--ar-bg-color, #ffffff);
  border-top: 1px solid var(--ar-border-color-light, #e4e7ed);
}

.ar-kanban-column-add-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--ar-spacing-xs, 4px);
  width: 100%;
  padding: var(--ar-spacing-sm, 8px);
  border: 1px dashed var(--ar-border-color-base, #dcdfe6);
  background-color: transparent;
  color: var(--ar-text-color-regular, #606266);
  font-size: var(--ar-font-size-small, 13px);
  border-radius: var(--ar-border-radius-base, 4px);
  cursor: pointer;
  transition: all var(--ar-transition-duration, 0.2s);

  &:hover {
    border-color: var(--ar-color-primary, #409eff);
    color: var(--ar-color-primary, #409eff);
    background-color: var(--ar-color-primary-light-3, rgba(64, 158, 255, 0.3));
  }
}

.ar-kanban-column-add-icon {
  font-size: var(--ar-font-size-medium, 16px);
}

.ar-kanban-column-add-text {
  font-size: var(--ar-font-size-small, 13px);
}

@media (max-width: 768px) {
  .ar-kanban-column {
    min-width: 240px;
    max-width: 280px;
  }

  .ar-kanban-column-header {
    padding: var(--ar-spacing-sm, 8px) var(--ar-spacing-md, 12px);
  }

  .ar-kanban-column-body {
    max-height: calc(100vh - 150px);
  }
}
</style>