<template>
  <div 
    :class="['approval-node', sizeClass, statusClass, { 'interactive': interactive }]"
    :style="nodeStyles"
    @click="handleClick"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
  >
    <!-- 节点内容容器 -->
    <div class="node-content">
      <!-- 状态指示器 -->
      <div class="status-indicator" :style="indicatorStyle">
        <span class="status-icon">{{ statusIcon }}</span>
      </div>

      <!-- 节点标题 -->
      <div class="node-title">
        <slot name="title">
          <span class="title-text">{{ title }}</span>
        </slot>
        <span v-if="showBadge && badgeCount > 0" class="badge">{{ badgeCount }}</span>
      </div>

      <!-- 节点副标题 -->
      <div v-if="subtitle" class="node-subtitle">
        <slot name="subtitle">
          <span class="subtitle-text">{{ subtitle }}</span>
        </slot>
      </div>

      <!-- 审批人信息 -->
      <div v-if="approver" class="approver-info">
        <div class="approver-avatar">
          <slot name="avatar">
            <div class="avatar-placeholder">{{ getInitials(approver) }}</div>
          </slot>
        </div>
        <div class="approver-details">
          <span class="approver-name">{{ approver }}</span>
          <span v-if="department" class="approver-department">{{ department }}</span>
        </div>
      </div>

      <!-- 审批时间 -->
      <div v-if="approvalTime" class="approval-time">
        <span class="time-icon">⏰</span>
        <span class="time-text">{{ formatTime(approvalTime) }}</span>
      </div>

      <!-- 自定义内容插槽 -->
      <div v-if="$slots.default" class="custom-content">
        <slot></slot>
      </div>

      <!-- 操作按钮 -->
      <div v-if="showActions && actions.length > 0" class="node-actions">
        <button
          v-for="action in actions"
          :key="action.id"
          :class="['action-button', action.type]"
          @click.stop="handleAction(action.id)"
          :title="action.tooltip"
        >
          <span class="action-icon">{{ action.icon }}</span>
          <span v-if="action.showLabel" class="action-label">{{ action.label }}</span>
        </button>
      </div>

      <!-- 拖拽手柄（如果启用） -->
      <div v-if="draggable" class="drag-handle" @mousedown.stop="handleDragStart">
        <span class="drag-icon">⠿</span>
      </div>
    </div>

    <!-- 节点提示框 -->
    <div 
      v-if="showTooltip && tooltipContent" 
      :class="['node-tooltip', tooltipPosition]"
      :style="tooltipStyles"
    >
      {{ tooltipContent }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { ApprovalDesignTokens, getStatusColor, getStatusText, getStatusIcon } from './design-tokens'

interface Props {
  // 基本属性
  id: string
  title: string
  status: 'pending' | 'inProgress' | 'approved' | 'rejected' | 'executed' | 'cancelled'
  
  // 可选属性
  subtitle?: string
  approver?: string
  department?: string
  approvalTime?: string | Date
  badgeCount?: number
  size?: 'small' | 'medium' | 'large'
  
  // 交互属性
  interactive?: boolean
  selected?: boolean
  disabled?: boolean
  draggable?: boolean
  
  // 操作按钮
  actions?: Array<{
    id: string
    label: string
    icon: string
    type: 'primary' | 'secondary' | 'danger'
    tooltip: string
    showLabel?: boolean
  }>
  
  // 提示框
  tooltipContent?: string
  tooltipPosition?: 'top' | 'bottom' | 'left' | 'right'
  
  // 自定义样式
  customColor?: string
  customBackground?: string
  customBorder?: string
}

interface Emits {
  (e: 'click', nodeId: string): void
  (e: 'action', actionId: string): void
  (e: 'drag-start', nodeId: string): void
  (e: 'mouse-enter', nodeId: string): void
  (e: 'mouse-leave', nodeId: string): void
}

const props = withDefaults(defineProps<Props>(), {
  subtitle: '',
  approver: '',
  department: '',
  approvalTime: '',
  badgeCount: 0,
  size: 'medium',
  interactive: true,
  selected: false,
  disabled: false,
  draggable: false,
  actions: () => [],
  tooltipContent: '',
  tooltipPosition: 'top',
  customColor: '',
  customBackground: '',
  customBorder: '',
})

const emit = defineEmits<Emits>()

// 响应式状态
const showTooltip = ref(false)
const isHovered = ref(false)
const isDragging = ref(false)

// 计算属性
const statusColor = computed(() => getStatusColor(props.status))
const statusText = computed(() => getStatusText(props.status))
const statusIcon = computed(() => getStatusIcon(props.status))

const sizeClass = computed(() => `size-${props.size}`)
const statusClass = computed(() => `status-${props.status}`)

const indicatorStyle = computed(() => ({
  backgroundColor: statusColor.value,
  color: '#ffffff',
  boxShadow: `0 0 0 3px ${statusColor.value}33`, // 添加光晕效果
}))

const nodeStyles = computed(() => {
  const styles: Record<string, string> = {
    '--node-color': props.customColor || statusColor.value,
    '--node-bg': props.customBackground || ApprovalDesignTokens.colors.neutral[50],
    '--node-border': props.customBorder || ApprovalDesignTokens.colors.neutral[200],
  }

  if (props.selected) {
    styles['--node-border'] = statusColor.value
    styles.boxShadow = ApprovalDesignTokens.shadows.md
  }

  if (props.disabled) {
    styles.opacity = '0.5'
    styles.cursor = 'not-allowed'
  }

  return styles
})

const tooltipStyles = computed(() => ({
  backgroundColor: ApprovalDesignTokens.colors.neutral[900],
  color: ApprovalDesignTokens.colors.neutral[50],
  borderRadius: ApprovalDesignTokens.borderRadius.md,
  padding: ApprovalDesignTokens.spacing.sm,
  fontSize: ApprovalDesignTokens.typography.fontSize.xs,
  maxWidth: '200px',
}))

const showActions = computed(() => props.interactive && props.actions.length > 0 && !props.disabled)

// 方法
const handleClick = () => {
  if (!props.disabled && props.interactive) {
    emit('click', props.id)
  }
}

const handleAction = (actionId: string) => {
  if (!props.disabled) {
    emit('action', actionId)
  }
}

const handleDragStart = (event: MouseEvent) => {
  if (props.draggable && !props.disabled) {
    event.preventDefault()
    isDragging.value = true
    emit('drag-start', props.id)
  }
}

const handleMouseEnter = () => {
  isHovered.value = true
  if (props.tooltipContent) {
    showTooltip.value = true
  }
  emit('mouse-enter', props.id)
}

const handleMouseLeave = () => {
  isHovered.value = false
  showTooltip.value = false
  emit('mouse-leave', props.id)
}

const getInitials = (name: string): string => {
  return name
    .split(' ')
    .map(word => word[0])
    .join('')
    .toUpperCase()
    .slice(0, 2)
}

const formatTime = (time: string | Date): string => {
  const date = typeof time === 'string' ? new Date(time) : time
  return date.toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// 生命周期
onMounted(() => {
  // 可以添加动画效果
  console.log(`ApprovalNode ${props.id} mounted`)
})

onUnmounted(() => {
  console.log(`ApprovalNode ${props.id} unmounted`)
})
</script>

<style scoped>
.approval-node {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-duration, 0.3s) var(--transition-easing, ease-in-out);
  cursor: pointer;
  user-select: none;
  background-color: var(--node-bg, #ffffff);
  border: 2px solid var(--node-border, #e5e5e5);
  border-radius: 12px;
  padding: 1rem;
  min-width: 100px;
  font-family: var(--font-family, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif);
}

.approval-node.interactive:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md, 0 4px 6px -1px rgba(0, 0, 0, 0.1));
  border-color: var(--node-color, #0ea5e9);
}

.approval-node:active {
  transform: translateY(0);
}

/* 尺寸变体 */
.approval-node.size-small {
  padding: 0.75rem;
  min-width: 80px;
  font-size: 0.875rem;
}

.approval-node.size-medium {
  padding: 1rem;
  min-width: 100px;
  font-size: 1rem;
}

.approval-node.size-large {
  padding: 1.25rem;
  min-width: 120px;
  font-size: 1.125rem;
}

/* 状态变体 */
.approval-node.status-pending {
  --node-color: #f59e0b;
}

.approval-node.status-inProgress {
  --node-color: #0ea5e9;
}

.approval-node.status-approved {
  --node-color: #22c55e;
}

.approval-node.status-rejected {
  --node-color: #ef4444;
}

.approval-node.status-executed {
  --node-color: #8b5cf6;
}

.approval-node.status-cancelled {
  --node-color: #6b7280;
}

/* 节点内容 */
.node-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  width: 100%;
}

.status-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  font-size: 1rem;
  margin-bottom: 0.5rem;
  transition: all 0.3s ease;
}

.node-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 600;
  color: var(--text-color, #171717);
  text-align: center;
  width: 100%;
  justify-content: center;
}

.badge {
  background-color: var(--node-color, #0ea5e9);
  color: white;
  font-size: 0.75rem;
  padding: 0.125rem 0.375rem;
  border-radius: 9999px;
  min-width: 20px;
  text-align: center;
}

.node-subtitle {
  font-size: 0.875rem;
  color: var(--text-secondary, #737373);
  text-align: center;
}

.approver-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-top: 0.5rem;
  padding: 0.5rem;
  background-color: var(--bg-secondary, #f5f5f5);
  border-radius: 8px;
  width: 100%;
}

.approver-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: var(--node-color, #0ea5e9);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  font-weight: 600;
}

.approver-details {
  display: flex;
  flex-direction: column;
  flex: 1;
  overflow: hidden;
}

.approver-name {
  font-weight: 500;
  font-size: 0.875rem;
  color: var(--text-color, #171717);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.approver-department {
  font-size: 0.75rem;
  color: var(--text-secondary, #737373);
}

.approval-time {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.75rem;
  color: var(--text-secondary, #737373);
  margin-top: 0.25rem;
}

.time-icon {
  font-size: 0.875rem;
}

.custom-content {
  margin-top: 0.5rem;
  width: 100%;
}

/* 操作按钮 */
.node-actions {
  display: flex;
  gap: 0.5rem;
  margin-top: 0.75rem;
  width: 100%;
  justify-content: center;
}

.action-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.25rem;
  padding: 0.25rem 0.5rem;
  border-radius: 6px;
  border: none;
  font-size: 0.875rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  min-height: 28px;
}

.action-button.primary {
  background-color: var(--node-color, #0ea5e9);
  color: white;
}

.action-button.secondary {
  background-color: var(--bg-secondary, #f5f5f5);
  color: var(--text-color, #171717);
}

.action-button.danger {
  background-color: #fee2e2;
  color: #dc2626;
}

.action-button:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.action-button:active {
  transform: translateY(0);
}

.action-icon {
  font-size: 1rem;
}

.action-label {
  font-size: 0.75rem;
}

/* 拖拽手柄 */
.drag-handle {
  position: absolute;
  top: 4px;
  right: 4px;
  cursor: grab;
  color: var(--text-secondary, #737373);
  opacity: 0.3;
  transition: opacity 0.2s ease;
}

.drag-handle:hover {
  opacity: 1;
}

.drag-icon {
  font-size: 1rem;
  display: inline-block;
  transform: rotate(45deg);
}

/* 提示框 */
.node-tooltip {
  position: absolute;
  z-index: 10;
  white-space: nowrap;
  pointer-events: none;
  animation: fadeIn 0.2s ease;
}

.node-tooltip.top {
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  margin-bottom: 8px;
}

.node-tooltip.bottom {
  top: 100%;
  left: 50%;
  transform: translateX(-50%);
  margin-top: 8px;
}

.node-tooltip.left {
  right: 100%;
  top: 50%;
  transform: translateY(-50%);
  margin-right: 8px;
}

.node-tooltip.right {
  left: 100%;
  top: 50%;
  transform: translateY(-50%);
  margin-left: 8px;
}

/* 动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式 */
@media (max-width: 640px) {
  .approval-node {
    width: 100%;
    min-width: auto;
  }
  
  .node-actions {
    flex-wrap: wrap;
  }
}
</style>