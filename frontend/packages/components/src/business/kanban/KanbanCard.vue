<template>
  <div
    class="ar-kanban-card"
    :class="{ 'ar-kanban-card--selected': selected }"
    @click="handleClick"
  >
    <div class="ar-kanban-card-header">
      <div class="ar-kanban-card-title">
        <span class="ar-kanban-card-no">{{ card.no || card.id }}</span>
        <a-tag
          v-if="card.statusText"
          :color="statusColor"
          size="small"
        >
          {{ card.statusText }}
        </a-tag>
      </div>
    </div>

    <div class="ar-kanban-card-body">
      <div
        v-if="card.customerName || card.supplierName"
        class="ar-kanban-card-row"
      >
        <span class="ar-kanban-card-label">{{ card.customerName ? '客户' : '供应商' }}:</span>
        <span class="ar-kanban-card-value">{{ card.customerName || card.supplierName }}</span>
      </div>

      <div
        v-if="card.totalAmount || card.amount"
        class="ar-kanban-card-row"
      >
        <span class="ar-kanban-card-label">金额:</span>
        <span class="ar-kanban-card-value ar-kanban-card-amount">
          ¥{{ (card.totalAmount || card.amount)?.toFixed(2) }}
        </span>
      </div>

      <div
        v-if="card.date || card.orderDate || card.createTime"
        class="ar-kanban-card-row"
      >
        <span class="ar-kanban-card-label">日期:</span>
        <span class="ar-kanban-card-value">{{ formatDate(card.date || card.orderDate || card.createTime) }}</span>
      </div>

      <div
        v-if="card.userName || card.purchaserName || card.salesmanName"
        class="ar-kanban-card-row"
      >
        <span class="ar-kanban-card-label">负责人:</span>
        <span class="ar-kanban-card-value">{{ card.userName || card.purchaserName || card.salesmanName }}</span>
      </div>

      <slot name="extra" />
    </div>

    <div
      v-if="showActions"
      class="ar-kanban-card-footer"
    >
      <slot name="actions">
        <div class="ar-kanban-card-actions">
          <button
            class="ar-kanban-card-action-btn"
            @click.stop="handleView"
          >查看</button>
          <button
            v-if="canEdit"
            class="ar-kanban-card-action-btn"
            @click.stop="handleEdit"
          >编辑</button>
        </div>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface KanbanCardData {
  id: string | number
  no?: string
  title?: string
  status: number
  statusText?: string
  customerName?: string
  supplierName?: string
  totalAmount?: number
  amount?: number
  date?: string
  orderDate?: string
  createTime?: string
  userName?: string
  purchaserName?: string
  salesmanName?: string
  [key: string]: any
}

interface Props {
  card: KanbanCardData
  selected?: boolean
  showActions?: boolean
  canEdit?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  selected: false,
  showActions: true,
  canEdit: false
})

const emit = defineEmits<{
  click: [card: KanbanCardData]
  view: [card: KanbanCardData]
  edit: [card: KanbanCardData]
}>()

const statusColor = computed(() => {
  const status = props.card.status
  const colors: Record<number, string> = {
    0: 'default',
    1: 'orange',
    2: 'green',
    3: 'blue',
    4: 'success',
    5: 'red'
  }
  return colors[status] || 'default'
})

const formatDate = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN')
}

const handleClick = () => {
  emit('click', props.card)
}

const handleView = () => {
  emit('view', props.card)
}

const handleEdit = () => {
  emit('edit', props.card)
}
</script>

<style lang="scss" scoped>
.ar-kanban-card {
  background-color: var(--ar-bg-color, #ffffff);
  border-radius: var(--ar-border-radius-base, 4px);
  padding: var(--ar-spacing-md, 12px);
  cursor: pointer;
  transition: all var(--ar-transition-duration, 0.2s);
  border: 1px solid var(--ar-border-color-lighter, #ebeef5);

  &:hover {
    box-shadow: var(--ar-box-shadow-light, 0 2px 12px 0 rgba(0, 0, 0, 0.1));
    border-color: var(--ar-color-primary-light-5, rgba(64, 158, 255, 0.5));
  }

  &--selected {
    border-color: var(--ar-color-primary, #409eff);
    box-shadow: var(--ar-box-shadow-base, 0 2px 4px rgba(0, 0, 0, .12), 0 0 6px rgba(0, 0, 0, .04));
  }
}

.ar-kanban-card-header {
  margin-bottom: var(--ar-spacing-sm, 8px);
}

.ar-kanban-card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.ar-kanban-card-no {
  font-size: var(--ar-font-size-base, 14px);
  font-weight: var(--ar-font-weight-primary, 500);
  color: var(--ar-text-color-primary, #303133);
}

.ar-kanban-card-body {
  display: flex;
  flex-direction: column;
  gap: var(--ar-spacing-xs, 4px);
}

.ar-kanban-card-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: var(--ar-font-size-small, 13px);
}

.ar-kanban-card-label {
  color: var(--ar-text-color-secondary, #909399);
}

.ar-kanban-card-value {
  color: var(--ar-text-color-regular, #606266);
}

.ar-kanban-card-amount {
  font-weight: var(--ar-font-weight-primary, 500);
  color: var(--ar-color-danger, #f56c6c);
}

.ar-kanban-card-footer {
  margin-top: var(--ar-spacing-sm, 8px);
  padding-top: var(--ar-spacing-sm, 8px);
  border-top: 1px solid var(--ar-border-color-lighter, #ebeef5);
}

.ar-kanban-card-actions {
  display: flex;
  gap: var(--ar-spacing-sm, 8px);
}

.ar-kanban-card-action-btn {
  padding: var(--ar-spacing-xs, 4px) var(--ar-spacing-sm, 8px);
  border: 1px solid var(--ar-border-color-base, #dcdfe6);
  background-color: transparent;
  color: var(--ar-text-color-regular, #606266);
  font-size: var(--ar-font-size-extra-small, 12px);
  border-radius: var(--ar-border-radius-small, 2px);
  cursor: pointer;
  transition: all var(--ar-transition-duration, 0.2s);

  &:hover {
    border-color: var(--ar-color-primary, #409eff);
    color: var(--ar-color-primary, #409eff);
  }
}

@media (max-width: 768px) {
  .ar-kanban-card {
    padding: var(--ar-spacing-sm, 8px);
  }

  .ar-kanban-card-no {
    font-size: var(--ar-font-size-small, 13px);
  }

  .ar-kanban-card-row {
    font-size: var(--ar-font-size-extra-small, 12px);
  }
}
</style>