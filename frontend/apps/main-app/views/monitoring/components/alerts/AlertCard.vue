<template>
  <div 
    class="alert-card"
    :class="{
      'alert-card--expandable': expandable,
      'alert-card--expanded': expanded,
      'alert-card--compact': compact,
      'alert-card--blinking': blinking && alert.status === 'active',
      [`alert-card--severity-${alert.severity}`]: alert.severity,
      [`alert-card--status-${alert.status}`]: alert.status
    }"
  >
    <!-- 告警头部 -->
    <div class="alert-card__header" @click="toggleExpand">
      <div class="alert-card__severity-badge" :class="`severity-${alert.severity}`">
        <span class="alert-card__severity-icon">
          <v-icon :name="severityIcon" />
        </span>
        <span class="alert-card__severity-text">{{ severityText }}</span>
      </div>
      
      <div class="alert-card__header-content">
        <div class="alert-card__title">
          <h3 class="alert-card__title-text">{{ alert.title }}</h3>
          <span v-if="alert.source" class="alert-card__source">{{ alert.source }}</span>
        </div>
        
        <div class="alert-card__meta">
          <span class="alert-card__time">
            <v-icon name="clock" />
            {{ formatTime(alert.timestamp) }}
          </span>
          <span v-if="alert.duration" class="alert-card__duration">
            <v-icon name="time" />
            {{ alert.duration }}
          </span>
          <span v-if="alert.environment" class="alert-card__environment">
            <v-icon name="environment" />
            {{ alert.environment }}
          </span>
        </div>
      </div>
      
      <div class="alert-card__header-actions">
        <button
          v-if="expandable"
          class="alert-card__expand-button"
          @click.stop="toggleExpand"
          :title="expanded ? '收起详情' : '展开详情'"
        >
          <v-icon :name="expanded ? 'arrow-up' : 'arrow-down'" />
        </button>
      </div>
    </div>

    <!-- 告警内容 -->
    <div class="alert-card__content">
      <p class="alert-card__description">{{ alert.description }}</p>
      
      <!-- 详细信息（可展开） -->
      <transition name="alert-details">
        <div v-if="expanded" class="alert-card__details">
          <div v-if="alert.assignedTo" class="alert-card__detail-item">
            <span class="alert-card__detail-label">负责人:</span>
            <span class="alert-card__detail-value">{{ alert.assignedTo }}</span>
          </div>
          
          <div v-if="alert.details" class="alert-card__detail-list">
            <div
              v-for="(value, key) in alert.details"
              :key="key"
              class="alert-card__detail-item"
            >
              <span class="alert-card__detail-label">{{ formatDetailKey(key) }}:</span>
              <span class="alert-card__detail-value">{{ formatDetailValue(value) }}</span>
            </div>
          </div>
        </div>
      </transition>
    </div>

    <!-- 操作按钮 -->
    <div v-if="showActions" class="alert-card__actions">
      <template v-if="alert.status === 'active'">
        <button
          class="alert-card__action-button alert-card__action--acknowledge"
          @click="handleAcknowledge"
          :disabled="processing"
        >
          <v-icon name="check" />
          <span>确认</span>
        </button>
        
        <button
          class="alert-card__action-button alert-card__action--resolve"
          @click="handleResolve"
          :disabled="processing"
        >
          <v-icon name="passed" />
          <span>解决</span>
        </button>
      </template>
      
      <template v-else-if="alert.status === 'acknowledged'">
        <button
          class="alert-card__action-button alert-card__action--resolve"
          @click="handleResolve"
          :disabled="processing"
        >
          <v-icon name="passed" />
          <span>标记解决</span>
        </button>
      </template>
      
      <template v-else>
        <span class="alert-card__status-text">
          <v-icon name="success" />
          已解决
        </span>
      </template>
      
      <button
        class="alert-card__action-button alert-card__action--mute"
        @click="handleMute"
        :disabled="processing"
      >
        <v-icon name="volume-off" />
        <span>静音</span>
      </button>
      
      <button
        class="alert-card__action-button alert-card__action--details"
        @click="toggleExpand"
      >
        <v-icon :name="expanded ? 'eye-close' : 'eye'" />
        <span>{{ expanded ? '收起' : '详情' }}</span>
      </button>
    </div>

    <!-- 处理状态指示器 -->
    <div v-if="processing" class="alert-card__processing">
      <div class="alert-card__processing-spinner"></div>
      <span class="alert-card__processing-text">处理中...</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Icon as VIcon } from 'vant'

interface AlertInfo {
  id: string
  title: string
  description: string
  severity: 'critical' | 'high' | 'medium' | 'low'
  status: 'active' | 'acknowledged' | 'resolved'
  source: string
  environment: string
  timestamp: string
  duration?: string
  assignedTo?: string
  details?: Record<string, any>
}

interface Props {
  alert: AlertInfo
  showActions?: boolean
  expandable?: boolean
  blinking?: boolean
  compact?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showActions: true,
  expandable: false,
  blinking: false,
  compact: false
})

const emit = defineEmits<{
  acknowledge: [id: string]
  resolve: [id: string]
  mute: [id: string]
  expand: [id: string, expanded: boolean]
}>()

// 状态
const expanded = ref(false)
const processing = ref(false)

// 计算属性
const severityIcon = computed(() => {
  switch (props.alert.severity) {
    case 'critical': return 'fire'
    case 'high': return 'warning'
    case 'medium': return 'info'
    case 'low': return 'bell'
    default: return 'bell'
  }
})

const severityText = computed(() => {
  switch (props.alert.severity) {
    case 'critical': return '紧急'
    case 'high': return '高'
    case 'medium': return '中'
    case 'low': return '低'
    default: return '未知'
  }
})

// 方法
const formatTime = (timestamp: string) => {
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatDetailKey = (key: string) => {
  const mapping: Record<string, string> = {
    'errorCode': '错误代码',
    'errorMessage': '错误信息',
    'metric': '指标',
    'threshold': '阈值',
    'currentValue': '当前值',
    'recommendation': '建议'
  }
  return mapping[key] || key
}

const formatDetailValue = (value: any) => {
  if (typeof value === 'object') {
    return JSON.stringify(value, null, 2)
  }
  return String(value)
}

const toggleExpand = () => {
  if (props.expandable) {
    expanded.value = !expanded.value
    emit('expand', props.alert.id, expanded.value)
  }
}

const handleAcknowledge = async () => {
  processing.value = true
  try {
    emit('acknowledge', props.alert.id)
  } finally {
    processing.value = false
  }
}

const handleResolve = async () => {
  processing.value = true
  try {
    emit('resolve', props.alert.id)
  } finally {
    processing.value = false
  }
}

const handleMute = async () => {
  processing.value = true
  try {
    emit('mute', props.alert.id)
  } finally {
    processing.value = false
  }
}
</script>

<style scoped>
.alert-card {
  position: relative;
  background: var(--bg-base);
  border-radius: var(--radius-md);
  padding: var(--spacing-card);
  margin-bottom: var(--spacing-inner-md);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--border-light);
  transition: var(--transition-status);
  overflow: hidden;
}

/* 严重度样式 */
.alert-card--severity-critical {
  border-left: 4px solid var(--alert-critical);
  background: linear-gradient(90deg, var(--alert-critical-bg) 0%, transparent 100%);
}

.alert-card--severity-high {
  border-left: 4px solid var(--alert-high);
  background: linear-gradient(90deg, var(--alert-high-bg) 0%, transparent 100%);
}

.alert-card--severity-medium {
  border-left: 4px solid var(--alert-medium);
  background: linear-gradient(90deg, var(--alert-medium-bg) 0%, transparent 100%);
}

.alert-card--severity-low {
  border-left: 4px solid var(--alert-low);
  background: linear-gradient(90deg, var(--alert-low-bg) 0%, transparent 100%);
}

/* 状态样式 */
.alert-card--status-acknowledged {
  opacity: 0.8;
}

.alert-card--status-resolved {
  opacity: 0.6;
  background: linear-gradient(90deg, var(--test-env-success-bg) 0%, transparent 100%);
}

/* 闪烁效果 */
.alert-card--blinking {
  animation: alert-blink 2s ease-in-out infinite;
}

/* 紧凑模式 */
.alert-card--compact {
  padding: var(--spacing-inner-md);
}

.alert-card--compact .alert-card__header {
  margin-bottom: var(--spacing-inner-sm);
}

.alert-card--compact .alert-card__actions {
  padding-top: var(--spacing-inner-sm);
}

/* 头部 */
.alert-card__header {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-inner-md);
  margin-bottom: var(--spacing-inner-lg);
  cursor: pointer;
}

.alert-card--expandable .alert-card__header:hover {
  opacity: 0.9;
}

/* 严重度徽章 */
.alert-card__severity-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 60px;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  text-align: center;
  white-space: nowrap;
}

.alert-card__severity-badge.severity-critical {
  background: var(--alert-critical);
  color: white;
  box-shadow: var(--shadow-alert-critical);
}

.alert-card__severity-badge.severity-high {
  background: var(--alert-high);
  color: white;
  box-shadow: var(--shadow-alert-high);
}

.alert-card__severity-badge.severity-medium {
  background: var(--alert-medium);
  color: var(--text-primary);
  box-shadow: var(--shadow-alert-medium);
}

.alert-card__severity-badge.severity-low {
  background: var(--alert-low);
  color: white;
  box-shadow: var(--shadow-alert-low);
}

.alert-card__severity-icon {
  font-size: 16px;
  margin-bottom: 2px;
}

.alert-card__severity-text {
  font-size: 10px;
  letter-spacing: 0.5px;
}

/* 头部内容 */
.alert-card__header-content {
  flex: 1;
  min-width: 0;
}

.alert-card__title {
  display: flex;
  align-items: baseline;
  gap: var(--spacing-inner-md);
  margin-bottom: var(--spacing-inner-sm);
}

.alert-card__title-text {
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
  color: var(--text-primary);
  margin: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.alert-card__source {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
  background: var(--bg-component);
  padding: 2px 6px;
  border-radius: var(--radius-xs);
  white-space: nowrap;
}

.alert-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-inner-lg);
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}

.alert-card__meta > span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.alert-card__meta .v-icon {
  font-size: 12px;
}

/* 头部操作 */
.alert-card__header-actions {
  display: flex;
  align-items: center;
}

.alert-card__expand-button {
  background: none;
  border: none;
  padding: 4px;
  color: var(--text-secondary);
  cursor: pointer;
  border-radius: var(--radius-xs);
  transition: var(--transition-status);
}

.alert-card__expand-button:hover {
  color: var(--test-env-primary);
  background: var(--test-env-primary-bg);
}

/* 内容 */
.alert-card__content {
  margin-bottom: var(--spacing-inner-lg);
}

.alert-card__description {
  font-size: var(--font-size-sm);
  color: var(--text-regular);
  line-height: var(--line-height-sm);
  margin: 0 0 var(--spacing-inner-md) 0;
}

/* 详细信息 */
.alert-card__details {
  padding: var(--spacing-inner-md);
  background: var(--bg-component);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-light);
  margin-top: var(--spacing-inner-md);
}

.alert-card__detail-list {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-inner-sm);
}

.alert-card__detail-item {
  display: flex;
  align-items: flex-start;
  gap: var(--spacing-inner-md);
  font-size: var(--font-size-xs);
  line-height: var(--line-height-xs);
}

.alert-card__detail-label {
  font-weight: var(--font-weight-medium);
  color: var(--text-primary);
  min-width: 80px;
}

.alert-card__detail-value {
  color: var(--text-regular);
  flex: 1;
  word-break: break-word;
  font-family: var(--font-family-code);
  background: var(--bg-page);
  padding: 4px 8px;
  border-radius: var(--radius-xs);
  border: 1px solid var(--border-lighter);
}

/* 操作按钮 */
.alert-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--spacing-inner-sm);
  padding-top: var(--spacing-inner-md);
  border-top: 1px solid var(--border-light);
}

.alert-card__action-button {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 6px 12px;
  border: none;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
  cursor: pointer;
  transition: var(--transition-status);
  min-width: 70px;
}

.alert-card__action-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.alert-card__action-button:not(:disabled):hover {
  transform: translateY(-1px);
}

.alert-card__action-button:not(:disabled):active {
  transform: translateY(0);
}

.alert-card__action--acknowledge {
  background: var(--test-env-primary);
  color: white;
}

.alert-card__action--acknowledge:hover:not(:disabled) {
  background: var(--test-env-primary-dark);
  box-shadow: var(--shadow-card-hover);
}

.alert-card__action--resolve {
  background: var(--test-env-success);
  color: white;
}

.alert-card__action--resolve:hover:not(:disabled) {
  background: var(--test-env-success-dark);
  box-shadow: var(--shadow-card-hover);
}

.alert-card__action--mute {
  background: var(--bg-component);
  color: var(--text-regular);
  border: 1px solid var(--border-base);
}

.alert-card__action--mute:hover:not(:disabled) {
  background: var(--bg-component-light);
  border-color: var(--border-dark);
}

.alert-card__action--details {
  background: transparent;
  color: var(--test-env-info);
  border: 1px solid var(--test-env-info);
}

.alert-card__action--details:hover:not(:disabled) {
  background: var(--test-env-info-bg);
}

.alert-card__status-text {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-xs);
  color: var(--test-env-success);
  padding: 6px 12px;
}

.alert-card__status-text .v-icon {
  font-size: 14px;
}

/* 处理状态 */
.alert-card__processing {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  z-index: 10;
}

.alert-card__processing-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid var(--border-light);
  border-top-color: var(--test-env-primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: var(--spacing-inner-sm);
}

.alert-card__processing-text {
  font-size: var(--font-size-xs);
  color: var(--text-secondary);
}

/* 动画 */
@keyframes alert-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.alert-details-enter-active,
.alert-details-leave-active {
  transition: all 0.3s ease;
  max-height: 500px;
  overflow: hidden;
}

.alert-details-enter-from,
.alert-details-leave-to {
  max-height: 0;
  opacity: 0;
  transform: translateY(-10px);
}

/* 响应式 */
@media (max-width: 768px) {
  .alert-card__header {
    flex-direction: column;
    gap: var(--spacing-inner-sm);
  }
  
  .alert-card__severity-badge {
    align-self: flex-start;
  }
  
  .alert-card__meta {
    gap: var(--spacing-inner-md);
  }
  
  .alert-card__actions {
    flex-direction: column;
  }
  
  .alert-card__action-button {
    width: 100%;
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .alert-card {
    padding: var(--spacing-inner-md);
  }
  
  .alert-card__title {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--spacing-inner-sm);
  }
  
  .alert-card__meta {
    flex-direction: column;
    gap: var(--spacing-inner-sm);
  }
}
</style>