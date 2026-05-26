<template>
  <div :class="alertClass" :style="alertStyle">
    <!-- 告警图标 -->
    <div v-if="showIcon" class="ar-alert__icon" :style="iconStyle">
      <el-icon v-if="severity === 'critical'">
        <CircleCloseFilled />
      </el-icon>
      <el-icon v-else-if="severity === 'high'">
        <WarningFilled />
      </el-icon>
      <el-icon v-else-if="severity === 'medium'">
        <Warning />
      </el-icon>
      <el-icon v-else-if="severity === 'low'">
        <InfoFilled />
      </el-icon>
      <el-icon v-else>
        <Bell />
      </el-icon>
    </div>
    
    <!-- 告警内容 -->
    <div class="ar-alert__content">
      <!-- 告警标题 -->
      <div v-if="title" class="ar-alert__title">
        {{ title }}
        <span v-if="count > 1" class="ar-alert__count">
          ({{ count }})
        </span>
      </div>
      
      <!-- 告警描述 -->
      <div v-if="description" class="ar-alert__description">
        {{ description }}
      </div>
      
      <!-- 告警时间 -->
      <div v-if="timestamp" class="ar-alert__timestamp">
        {{ formattedTimestamp }}
      </div>
      
      <!-- 告警来源 -->
      <div v-if="source" class="ar-alert__source">
        来源: {{ source }}
      </div>
      
      <!-- 自定义内容 -->
      <slot v-if="$slots.default" />
    </div>
    
    <!-- 关闭按钮 -->
    <div v-if="closable" class="ar-alert__close" @click="handleClose">
      <el-icon>
        <Close />
      </el-icon>
    </div>
    
    <!-- 操作按钮 -->
    <div v-if="showActions" class="ar-alert__actions">
      <el-button 
        v-if="showAcknowledge" 
        size="small" 
        :type="acknowledged ? 'success' : 'default'"
        @click="handleAcknowledge"
      >
        {{ acknowledged ? '已确认' : '确认' }}
      </el-button>
      
      <el-button 
        v-if="showMute" 
        size="small" 
        :type="muted ? 'warning' : 'default'"
        @click="handleMute"
      >
        {{ muted ? '已静音' : '静音' }}
      </el-button>
      
      <el-button 
        v-if="showDetails" 
        size="small" 
        text 
        @click="handleDetails"
      >
        详情
      </el-button>
      
      <!-- 自定义操作 -->
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, CSSProperties, ref } from 'vue'
import { 
  CircleCloseFilled, 
  WarningFilled, 
  Warning, 
  InfoFilled, 
  Bell,
  Close
} from '@element-plus/icons-vue'

defineOptions({
  name: 'ARAlert',
})

export interface AlertProps {
  /** 告警严重性级别 */
  severity?: 'critical' | 'high' | 'medium' | 'low' | 'info'
  /** 告警标题 */
  title?: string
  /** 告警描述 */
  description?: string
  /** 告警时间戳 */
  timestamp?: string | number | Date
  /** 告警来源 */
  source?: string
  /** 告警数量 */
  count?: number
  /** 是否显示图标 */
  showIcon?: boolean
  /** 是否可关闭 */
  closable?: boolean
  /** 是否显示确认按钮 */
  showAcknowledge?: boolean
  /** 是否显示静音按钮 */
  showMute?: boolean
  /** 是否显示详情按钮 */
  showDetails?: boolean
  /** 是否已确认 */
  acknowledged?: boolean
  /** 是否已静音 */
  muted?: boolean
  /** 是否显示边框 */
  border?: boolean
  /** 是否显示为横幅 */
  banner?: boolean
  /** 是否显示为紧凑模式 */
  compact?: boolean
  /** 自定义类名 */
  customClass?: string
}

const props = withDefaults(defineProps<AlertProps>(), {
  severity: 'info',
  title: '',
  description: '',
  timestamp: '',
  source: '',
  count: 1,
  showIcon: true,
  closable: true,
  showAcknowledge: false,
  showMute: false,
  showDetails: false,
  acknowledged: false,
  muted: false,
  border: false,
  banner: false,
  compact: false,
})

const emit = defineEmits<{
  close: []
  acknowledge: [acknowledged: boolean]
  mute: [muted: boolean]
  details: []
}>()

// 本地状态
const localAcknowledged = ref(props.acknowledged)
const localMuted = ref(props.muted)
const localClosed = ref(false)

// 告警严重性配置
const severityConfig = computed(() => {
  const configs = {
    critical: {
      color: '#f56c6c',
      bgColor: '#fef0f0',
      borderColor: '#fde2e2',
      icon: CircleCloseFilled,
      label: '严重',
      priority: 1, // P1
    },
    high: {
      color: '#e6a23c',
      bgColor: '#fdf6ec',
      borderColor: '#faecd8',
      icon: WarningFilled,
      label: '高危',
      priority: 2, // P2
    },
    medium: {
      color: '#f4c542',
      bgColor: '#fefce8',
      borderColor: '#fef3c7',
      icon: Warning,
      label: '中危',
      priority: 3, // P3
    },
    low: {
      color: '#909399',
      bgColor: '#f4f4f5',
      borderColor: '#e9e9eb',
      icon: InfoFilled,
      label: '低危',
      priority: 4,
    },
    info: {
      color: '#409eff',
      bgColor: '#f0f9ff',
      borderColor: '#d9ecff',
      icon: Bell,
      label: '信息',
      priority: 5,
    },
  }
  
  return configs[props.severity] || configs.info
})

// 告警类名
const alertClass = computed(() => ({
  'ar-alert': true,
  [`ar-alert--${props.severity}`]: true,
  'ar-alert--with-icon': props.showIcon,
  'ar-alert--border': props.border,
  'ar-alert--banner': props.banner,
  'ar-alert--compact': props.compact,
  'ar-alert--acknowledged': localAcknowledged.value,
  'ar-alert--muted': localMuted.value,
  'ar-alert--closed': localClosed.value,
  [props.customClass || '']: !!props.customClass,
}))

// 告警样式
const alertStyle = computed<CSSProperties>(() => {
  const style: CSSProperties = {}
  
  if (props.banner) {
    style.borderRadius = '0'
    style.margin = '0'
  }
  
  if (localClosed.value) {
    style.display = 'none'
  }
  
  return style
})

// 图标样式
const iconStyle = computed<CSSProperties>(() => ({
  color: severityConfig.value.color,
  fontSize: props.compact ? '16px' : '20px',
}))

// 格式化时间戳
const formattedTimestamp = computed(() => {
  if (!props.timestamp) return ''
  
  try {
    const date = new Date(props.timestamp)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffMins = Math.floor(diffMs / 60000)
    const diffHours = Math.floor(diffMs / 3600000)
    const diffDays = Math.floor(diffMs / 86400000)
    
    if (diffMins < 1) return '刚刚'
    if (diffMins < 60) return `${diffMins}分钟前`
    if (diffHours < 24) return `${diffHours}小时前`
    if (diffDays < 7) return `${diffDays}天前`
    
    return date.toLocaleDateString('zh-CN', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return String(props.timestamp)
  }
})

// 是否显示操作按钮
const showActions = computed(() => 
  props.showAcknowledge || 
  props.showMute || 
  props.showDetails ||
  !!props.$slots.actions
)

// 处理关闭
const handleClose = () => {
  localClosed.value = true
  emit('close')
}

// 处理确认
const handleAcknowledge = () => {
  localAcknowledged.value = !localAcknowledged.value
  emit('acknowledge', localAcknowledged.value)
}

// 处理静音
const handleMute = () => {
  localMuted.value = !localMuted.value
  emit('mute', localMuted.value)
}

// 处理详情
const handleDetails = () => {
  emit('details')
}

// 获取严重性标签
const severityLabel = computed(() => severityConfig.value.label)

// 获取优先级
const priority = computed(() => severityConfig.value.priority)
</script>

<style scoped>
.ar-alert {
  position: relative;
  display: flex;
  align-items: flex-start;
  padding: 16px;
  margin-bottom: 12px;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.ar-alert--compact {
  padding: 8px 12px;
}

.ar-alert--border {
  border: 1px solid;
  border-left-width: 4px;
}

.ar-alert--banner {
  border-radius: 0;
  margin: 0;
}

/* 图标区域 */
.ar-alert__icon {
  flex-shrink: 0;
  margin-right: 12px;
  margin-top: 2px;
}

.ar-alert--compact .ar-alert__icon {
  margin-right: 8px;
  margin-top: 0;
}

/* 内容区域 */
.ar-alert__content {
  flex: 1;
  min-width: 0;
}

.ar-alert__title {
  margin-bottom: 4px;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
  color: #303133;
}

.ar-alert--compact .ar-alert__title {
  font-size: 14px;
  margin-bottom: 2px;
}

.ar-alert__count {
  font-size: 12px;
  color: #909399;
  font-weight: normal;
  margin-left: 4px;
}

.ar-alert__description {
  margin-bottom: 8px;
  font-size: 14px;
  line-height: 1.5;
  color: #606266;
}

.ar-alert--compact .ar-alert__description {
  font-size: 13px;
  margin-bottom: 4px;
}

.ar-alert__timestamp {
  font-size: 12px;
  color: #909399;
  margin-bottom: 2px;
}

.ar-alert__source {
  font-size: 12px;
  color: #909399;
  font-style: italic;
}

/* 关闭按钮 */
.ar-alert__close {
  flex-shrink: 0;
  margin-left: 12px;
  cursor: pointer;
  color: #c0c4cc;
  font-size: 16px;
  transition: color 0.3s;
}

.ar-alert__close:hover {
  color: #909399;
}

/* 操作按钮 */
.ar-alert__actions {
  flex-shrink: 0;
  margin-left: 12px;
  display: flex;
  gap: 8px;
  align-items: center;
}

.ar-alert--compact .ar-alert__actions {
  margin-left: 8px;
  gap: 4px;
}

/* 严重性样式 */
.ar-alert--critical {
  background-color: var(--alert-critical-bg, #fef0f0);
  border-color: var(--alert-critical-border, #fde2e2);
  color: var(--alert-critical-color, #f56c6c);
}

.ar-alert--critical.ar-alert--border {
  border-left-color: var(--alert-critical-color, #f56c6c);
}

.ar-alert--high {
  background-color: var(--alert-high-bg, #fdf6ec);
  border-color: var(--alert-high-border, #faecd8);
  color: var(--alert-high-color, #e6a23c);
}

.ar-alert--high.ar-alert--border {
  border-left-color: var(--alert-high-color, #e6a23c);
}

.ar-alert--medium {
  background-color: var(--alert-medium-bg, #fefce8);
  border-color: var(--alert-medium-border, #fef3c7);
  color: var(--alert-medium-color, #f4c542);
}

.ar-alert--medium.ar-alert--border {
  border-left-color: var(--alert-medium-color, #f4c542);
}

.ar-alert--low {
  background-color: var(--alert-low-bg, #f4f4f5);
  border-color: var(--alert-low-border, #e9e9eb);
  color: var(--alert-low-color, #909399);
}

.ar-alert--low.ar-alert--border {
  border-left-color: var(--alert-low-color, #909399);
}

.ar-alert--info {
  background-color: var(--alert-info-bg, #f0f9ff);
  border-color: var(--alert-info-border, #d9ecff);
  color: var(--alert-info-color, #409eff);
}

.ar-alert--info.ar-alert--border {
  border-left-color: var(--alert-info-color, #409eff);
}

/* 已确认状态 */
.ar-alert--acknowledged {
  opacity: 0.7;
}

.ar-alert--acknowledged .ar-alert__title {
  text-decoration: line-through;
}

/* 已静音状态 */
.ar-alert--muted {
  opacity: 0.5;
}

/* 深色模式 */
[data-theme='dark'] .ar-alert--critical {
  background-color: rgba(245, 108, 108, 0.1);
  border-color: rgba(245, 108, 108, 0.3);
}

[data-theme='dark'] .ar-alert--high {
  background-color: rgba(230, 162, 60, 0.1);
  border-color: rgba(230, 162, 60, 0.3);
}

[data-theme='dark'] .ar-alert--medium {
  background-color: rgba(244, 197, 66, 0.1);
  border-color: rgba(244, 197, 66, 0.3);
}

[data-theme='dark'] .ar-alert--low {
  background-color: rgba(144, 147, 153, 0.1);
  border-color: rgba(144, 147, 153, 0.3);
}

[data-theme='dark'] .ar-alert--info {
  background-color: rgba(64, 158, 255, 0.1);
  border-color: rgba(64, 158, 255, 0.3);
}

[data-theme='dark'] .ar-alert__title {
  color: #e8e8e8;
}

[data-theme='dark'] .ar-alert__description {
  color: #a8a8a8;
}

[data-theme='dark'] .ar-alert__timestamp,
[data-theme='dark'] .ar-alert__source {
  color: #909399;
}

[data-theme='dark'] .ar-alert__close {
  color: #666;
}

[data-theme='dark'] .ar-alert__close:hover {
  color: #999;
}

/* 动画效果 */
@keyframes alert-pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.8;
  }
}

.ar-alert--critical:not(.ar-alert--acknowledged):not(.ar-alert--muted) {
  animation: alert-pulse 2s ease-in-out infinite;
}

/* 悬停效果 */
.ar-alert:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

[data-theme='dark'] .ar-alert:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ar-alert {
    flex-wrap: wrap;
  }
  
  .ar-alert__actions {
    margin-left: 0;
    margin-top: 8px;
    width: 100%;
    justify-content: flex-end;
  }
  
  .ar-alert--compact .ar-alert__actions {
    margin-top: 4px;
  }
}
</style>