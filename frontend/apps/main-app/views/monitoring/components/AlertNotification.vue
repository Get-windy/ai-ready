<template>
  <transition name="slide">
    <div 
      v-if="visible" 
      class="alert-notification"
      :class="[`level-${alert.level}`, { acknowledged: alert.acknowledged }]"
    >
      <div class="alert-icon">
        <el-icon :size="24">
          <WarningFilled v-if="alert.level === 'P0' || alert.level === 'P1'" />
          <Warning v-else-if="alert.level === 'P2'" />
          <InfoFilled v-else />
        </el-icon>
      </div>
      
      <div class="alert-content">
        <div class="alert-header">
          <span class="alert-level">{{ alert.level }}</span>
          <span class="alert-title">{{ alert.title }}</span>
        </div>
        <div class="alert-message">{{ alert.message }}</div>
        <div class="alert-meta">
          <span class="alert-source">
            <el-icon><Location /></el-icon>
            {{ alert.source }}
          </span>
          <span class="alert-time">{{ formatTime(alert.timestamp) }}</span>
        </div>
      </div>
      
      <div class="alert-actions">
        <el-button 
          v-if="!alert.acknowledged"
          type="primary" 
          size="small"
          @click="handleAcknowledge"
        >
          确认
        </el-button>
        <el-button 
          type="info" 
          size="small"
          text
          @click="handleDismiss"
        >
          忽略
        </el-button>
      </div>
      
      <div class="alert-progress" v-if="autoDismiss && !alert.acknowledged">
        <div class="progress-bar" :style="{ width: `${progress}%` }"></div>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { WarningFilled, Warning, InfoFilled, Location } from '@element-plus/icons-vue';
import type { AlertItem } from '../types/monitoring';

interface Props {
  alert: AlertItem;
  autoDismiss?: boolean;
  dismissDuration?: number;
}

const props = withDefaults(defineProps<Props>(), {
  autoDismiss: false,
  dismissDuration: 5000
});

const emit = defineEmits<{
  acknowledge: [id: string];
  dismiss: [id: string];
  detail: [alert: AlertItem];
}>();

const visible = ref(true);
const progress = ref(100);
let progressTimer: ReturnType<typeof setInterval> | null = null;
let dismissTimer: ReturnType<typeof setTimeout> | null = null;

onMounted(() => {
  if (props.autoDismiss && !props.alert.acknowledged) {
    const interval = 50;
    const decrement = 100 / (props.dismissDuration / interval);
    
    progressTimer = setInterval(() => {
      progress.value -= decrement;
      if (progress.value <= 0) {
        handleDismiss();
      }
    }, interval);
    
    dismissTimer = setTimeout(() => {
      handleDismiss();
    }, props.dismissDuration);
  }
});

onUnmounted(() => {
  if (progressTimer) {
    clearInterval(progressTimer);
  }
  if (dismissTimer) {
    clearTimeout(dismissTimer);
  }
});

const formatTime = (timestamp: string): string => {
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  
  if (diff < 60000) {
    return '刚刚';
  } else if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`;
  } else if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`;
  } else {
    return date.toLocaleDateString('zh-CN');
  }
};

const handleAcknowledge = () => {
  emit('acknowledge', props.alert.id);
};

const handleDismiss = () => {
  visible.value = false;
  setTimeout(() => {
    emit('dismiss', props.alert.id);
  }, 300);
};
</script>

<style scoped>
.alert-notification {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  margin-bottom: 12px;
  position: relative;
  overflow: hidden;
  border-left: 4px solid;
}

/* Level-specific styles */
.level-P0 {
  border-left-color: #F56C6C;
  background: linear-gradient(135deg, #FEF0F0 0%, #FFFFFF 100%);
}

.level-P0 .alert-icon {
  color: #F56C6C;
}

.level-P1 {
  border-left-color: #E6A23C;
  background: linear-gradient(135deg, #FDF6EC 0%, #FFFFFF 100%);
}

.level-P1 .alert-icon {
  color: #E6A23C;
}

.level-P2 {
  border-left-color: #E6C03C;
  background: linear-gradient(135deg, #FDF6EC 0%, #FFFFFF 100%);
}

.level-P2 .alert-icon {
  color: #E6C03C;
}

.level-P3 {
  border-left-color: #909399;
  background: linear-gradient(135deg, #F4F4F5 0%, #FFFFFF 100%);
}

.level-P3 .alert-icon {
  color: #909399;
}

.alert-icon {
  flex-shrink: 0;
  margin-top: 2px;
}

.alert-content {
  flex: 1;
  min-width: 0;
}

.alert-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.alert-level {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
  background: currentColor;
  color: white;
}

.level-P0 .alert-level {
  background: #F56C6C;
}

.level-P1 .alert-level {
  background: #E6A23C;
}

.level-P2 .alert-level {
  background: #E6C03C;
}

.level-P3 .alert-level {
  background: #909399;
}

.alert-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.alert-message {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  margin-bottom: 8px;
}

.alert-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.alert-source {
  display: flex;
  align-items: center;
  gap: 4px;
}

.alert-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
}

.alert-progress {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: #EBEEF5;
}

.progress-bar {
  height: 100%;
  background: currentColor;
  transition: width 0.05s linear;
}

.level-P0 .progress-bar {
  background: #F56C6C;
}

.level-P1 .progress-bar {
  background: #E6A23C;
}

.level-P2 .progress-bar {
  background: #E6C03C;
}

.level-P3 .progress-bar {
  background: #909399;
}

.acknowledged {
  opacity: 0.7;
}

/* Slide animation */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
}

.slide-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.slide-leave-to {
  opacity: 0;
  transform: translateX(100%);
}
</style>
