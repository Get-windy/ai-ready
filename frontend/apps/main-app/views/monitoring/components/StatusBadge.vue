<template>
  <span class="status-badge" :class="[`status-${status}`, { pulse: shouldPulse }]">
    <span class="status-dot"></span>
    <span v-if="showLabel" class="status-label">{{ label }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { ServiceStatusType, AlertLevel } from '../types/monitoring';

interface Props {
  status: ServiceStatusType | AlertLevel | 'success' | 'warning' | 'danger' | 'info';
  showLabel?: boolean;
  customLabel?: string;
}

const props = withDefaults(defineProps<Props>(), {
  showLabel: true,
  customLabel: ''
});

const label = computed(() => {
  if (props.customLabel) return props.customLabel;
  
  const labels: Record<string, string> = {
    // Service statuses
    running: '运行中',
    stopped: '已停止',
    error: '异常',
    warning: '警告',
    // Alert levels
    P0: '紧急',
    P1: '严重',
    P2: '警告',
    P3: '提示',
    // Generic statuses
    success: '正常',
    danger: '危险',
    info: '信息'
  };
  
  return labels[props.status] || props.status;
});

const shouldPulse = computed(() => {
  return ['running', 'error', 'P0', 'P1'].includes(props.status);
});
</script>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}

/* Service Status Colors */
.status-running .status-dot {
  background-color: #67C23A;
}

.status-stopped .status-dot {
  background-color: #909399;
}

.status-error .status-dot {
  background-color: #F56C6C;
}

.status-warning .status-dot {
  background-color: #E6A23C;
}

/* Alert Level Colors */
.status-P0 .status-dot {
  background-color: #F56C6C;
}

.status-P1 .status-dot {
  background-color: #E6A23C;
}

.status-P2 .status-dot {
  background-color: #E6C03C;
}

.status-P3 .status-dot {
  background-color: #909399;
}

/* Generic Status Colors */
.status-success .status-dot {
  background-color: #67C23A;
}

.status-danger .status-dot {
  background-color: #F56C6C;
}

.status-info .status-dot {
  background-color: #409EFF;
}

/* Pulse Animation */
.status-running.pulse .status-dot,
.status-error.pulse .status-dot,
.status-P0.pulse .status-dot,
.status-P1.pulse .status-dot {
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(103, 194, 58, 0.4);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(103, 194, 58, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(103, 194, 58, 0);
  }
}

.status-error.pulse .status-dot,
.status-P0.pulse .status-dot {
  animation: pulse-danger 1.5s infinite;
}

@keyframes pulse-danger {
  0% {
    box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.4);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(245, 108, 108, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(245, 108, 108, 0);
  }
}

.status-label {
  color: #606266;
}
</style>
