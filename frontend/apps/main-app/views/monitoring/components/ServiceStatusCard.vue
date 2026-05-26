<template>
  <el-card 
    class="service-status-card" 
    :class="[`status-${service.status}`]"
    shadow="hover"
    @click="handleClick"
  >
    <div class="service-content">
      <div class="service-header">
        <div class="service-icon-wrapper" :class="`type-${service.type}`">
          <el-icon :size="20">
            <component :is="iconComponent" />
          </el-icon>
        </div>
        <StatusBadge :status="service.status" :show-label="false" />
      </div>
      
      <div class="service-body">
        <div class="service-name">{{ service.name }}</div>
        <div class="service-uptime">
          <el-icon><Timer /></el-icon>
          <span>运行 {{ service.uptime }}</span>
        </div>
      </div>
      
      <div class="service-metrics">
        <div v-if="service.latency !== undefined" class="metric-item">
          <span class="metric-label">响应</span>
          <span class="metric-value" :class="getLatencyClass(service.latency)">
            {{ service.latency }}ms
          </span>
        </div>
        <div v-if="service.connections !== undefined" class="metric-item">
          <span class="metric-label">连接</span>
          <span class="metric-value">{{ service.connections }}</span>
        </div>
        <div v-if="service.cpuUsage !== undefined" class="metric-item">
          <span class="metric-label">CPU</span>
          <span class="metric-value" :class="getUsageClass(service.cpuUsage)">
            {{ service.cpuUsage }}%
          </span>
        </div>
        <div v-if="service.memoryUsage !== undefined" class="metric-item">
          <span class="metric-label">内存</span>
          <span class="metric-value" :class="getUsageClass(service.memoryUsage)">
            {{ service.memoryUsage }}%
          </span>
        </div>
      </div>
      
      <div class="service-footer">
        <span class="last-check">
          检查于 {{ formatTime(service.lastCheck) }}
        </span>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import {
  Timer,
  Cpu,
  Collection,
  Box,
  Message,
  Monitor,
  Link
} from '@element-plus/icons-vue';
import StatusBadge from './StatusBadge.vue';
import type { ServiceStatus, ServiceType } from '../types/monitoring';

interface Props {
  service: ServiceStatus;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  click: [service: ServiceStatus];
}>;

// Icon mapping by service type
const iconMap: Record<ServiceType, any> = {
  api: Link,
  database: Collection,
  cache: Box,
  queue: Message,
  other: Monitor
};

const iconComponent = computed(() => {
  return iconMap[props.service.type] || Monitor;
});

const getLatencyClass = (latency: number): string => {
  if (latency < 100) return 'good';
  if (latency < 500) return 'warning';
  return 'danger';
};

const getUsageClass = (usage: number): string => {
  if (usage < 70) return 'good';
  if (usage < 85) return 'warning';
  return 'danger';
};

const formatTime = (timeStr: string): string => {
  const date = new Date(timeStr);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  
  if (diff < 60000) {
    return '刚刚';
  } else if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`;
  } else {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  }
};

const handleClick = () => {
  emit('click', props.service);
};
</script>

<style scoped>
.service-status-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 12px;
  overflow: hidden;
}

.service-status-card:hover {
  transform: translateY(-2px);
}

.service-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.service-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.service-icon-wrapper {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.type-api {
  background: linear-gradient(135deg, #409EFF 0%, #66B1FF 100%);
}

.type-database {
  background: linear-gradient(135deg, #67C23A 0%, #85CE61 100%);
}

.type-cache {
  background: linear-gradient(135deg, #E6A23C 0%, #EBB563 100%);
}

.type-queue {
  background: linear-gradient(135deg, #9B59B6 0%, #BB8FCE 100%);
}

.type-other {
  background: linear-gradient(135deg, #909399 0%, #C0C4CC 100%);
}

.service-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.service-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.service-uptime {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.service-metrics {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  padding-top: 8px;
  border-top: 1px solid #EBEEF5;
}

.metric-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.metric-label {
  color: #909399;
}

.metric-value {
  font-weight: 500;
  color: #606266;
}

.metric-value.good {
  color: #67C23A;
}

.metric-value.warning {
  color: #E6A23C;
}

.metric-value.danger {
  color: #F56C6C;
}

.service-footer {
  display: flex;
  justify-content: flex-end;
}

.last-check {
  font-size: 11px;
  color: #C0C4CC;
}

/* Status-specific border colors */
.status-running {
  border-left: 3px solid #67C23A;
}

.status-warning {
  border-left: 3px solid #E6A23C;
}

.status-error {
  border-left: 3px solid #F56C6C;
}

.status-stopped {
  border-left: 3px solid #909399;
}
</style>
