<template>
  <div class="alert-panel" :class="`theme-${theme}`">
    <div class="alert-header">
      <div class="alert-title">
        <el-icon :size="18">
          <component :is="AlertCircle" />
        </el-icon>
        <span>{{ panel.title }}</span>
        <el-badge 
          v-if="pendingAlertsCount > 0" 
          :value="pendingAlertsCount" 
          :type="pendingAlertsCount > 3 ? 'danger' : 'warning'"
          class="badge"
        />
      </div>
      <div class="alert-actions">
        <el-select 
          v-model="selectedLevel" 
          size="small"
          style="width: 120px"
          @change="handleLevelChange"
        >
          <el-option label="全部级别" value="all" />
          <el-option label="P1 - 紧急" value="P1" />
          <el-option label="P2 - 严重" value="P2" />
          <el-option label="P3 - 警告" value="P3" />
        </el-select>
        
        <el-button 
          :icon="Refresh" 
          circle 
          size="small"
          :loading="isLoading"
          @click="handleRefresh"
        />
      </div>
    </div>
    
    <div class="alert-list">
      <div 
        v-for="alert in filteredAlerts" 
        :key="alert.id" 
        class="alert-item"
        :class="`level-${alert.level.toLowerCase()}`"
        :title="alert.message"
      >
        <div class="alert-item-header">
          <el-icon :class="`level-icon ${alert.level.toLowerCase()}`">
            <component :is="alertIconMap[alert.level as keyof typeof alertIconMap]" />
          </el-icon>
          <div class="alert-info">
            <span class="alert-title">{{ alert.title }}</span>
            <span class="alert-source">{{ alert.source }}</span>
          </div>
          <span class="alert-time">{{ formatTime(alert.timestamp) }}</span>
        </div>
        
        <div class="alert-message">
          {{ alert.message }}
        </div>
        
        <div class="alert-footer" v-if="isEditing">
          <el-button 
            v-if="!alert.acknowledged" 
            type="success" 
            size="small"
            :icon="CircleCheck"
            @click="handleAcknowledge(alert)"
          >
            确认
          </el-button>
          <el-button 
            v-else 
            size="small"
            disabled
          >
            已确认
          </el-button>
        </div>
      </div>
      
      <div v-if="filteredAlerts.length === 0" class="empty-state">
        <el-icon :size="48" color="#909399">
          <component :is="CircleCheck" />
        </el-icon>
        <p class="empty-text">暂无告警</p>
      </div>
    </div>
    
    <div class="alert-footer">
      <el-link type="primary" @click="handleViewAll">
        查看全部告警
        <el-icon><ArrowRight /></el-icon>
      </el-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { 
  AlertCircle, 
  CircleCheck, 
  Refresh, 
  ArrowRight,
  CircleClose,
  Warning
} from '@element-plus/icons-vue';

// Types
interface Alert {
  id: string;
  level: 'P0' | 'P1' | 'P2' | 'P3';
  title: string;
  message: string;
  source: string;
  timestamp: string;
  acknowledged: boolean;
}

interface Props {
  panel: {
    type: string;
    title: string;
    config: {
      limit?: number;
      levels?: string[];
      alerts?: Alert[];
      lastUpdated?: string;
    };
  };
  theme: 'light' | 'dark';
  isEditing?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  isEditing: false
});

// State
const isLoading = ref(false);
const selectedLevel = ref(props.panel.config.levels?.[0] || 'all');

// Computed
const allAlerts = computed(() => {
  if (props.panel.config.alerts) {
    return props.panel.config.alerts;
  }
  
  // Generate mock data
  return mockAlerts;
});

const filteredAlerts = computed(() => {
  let alerts = allAlerts.value;
  
  // Filter by level
  if (selectedLevel.value !== 'all') {
    alerts = alerts.filter(a => a.level === selectedLevel.value);
  }
  
  // Filter by limit
  const limit = props.panel.config.limit || 10;
  return alerts.slice(0, limit);
});

const pendingAlertsCount = computed(() => {
  return allAlerts.value.filter(a => !a.acknowledged).length;
});

const alertIconMap = {
  'P0': CircleClose,
  'P1': CircleClose,
  'P2': Warning,
  'P3': AlertCircle
};

const alertLevelColors = {
  'P0': '#F56C6C',
  'P1': '#F56C6C',
  'P2': '#E6A23C',
  'P3': '#409EFF'
};

// Mock data
const mockAlerts: Alert[] = [
  {
    id: 'alert-1',
    level: 'P1',
    title: 'Redis内存使用率过高',
    message: 'Redis缓存内存使用率达85%，建议立即扩容',
    source: 'Redis监控',
    timestamp: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
    acknowledged: false
  },
  {
    id: 'alert-2',
    level: 'P2',
    title: '数据库连接池告警',
    message: '主数据库连接池使用率达90%，建议优化连接池配置',
    source: '数据库监控',
    timestamp: new Date(Date.now() - 15 * 60 * 1000).toISOString(),
    acknowledged: false
  },
  {
    id: 'alert-3',
    level: 'P3',
    title: 'API响应时间波动',
    message: '过去10分钟内API平均响应时间波动超过20%',
    source: 'API监控',
    timestamp: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
    acknowledged: true
  }
];

// Methods
const handleLevelChange = (level: string) => {
  console.log('Level changed:', level);
};

const handleRefresh = async () => {
  isLoading.value = true;
  
  // Simulate API call
  await new Promise(resolve => setTimeout(resolve, 500));
  
  isLoading.value = false;
};

const handleAcknowledge = (alert: Alert) => {
  // Find and update alert
  const index = allAlerts.value.findIndex(a => a.id === alert.id);
  if (index !== -1) {
    allAlerts.value[index].acknowledged = true;
  }
};

const handleViewAll = () => {
  console.log('View all alerts');
};

const formatTime = (timestamp: string) => {
  const date = new Date(timestamp);
  const now = new Date();
  const diff = now.getTime() - date.getTime();
  
  if (diff < 60000) return '刚刚';
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`;
  return date.toLocaleString('zh-CN');
};

// Lifecycle
onMounted(() => {
  console.log('Alert panel mounted');
});

onUnmounted(() => {
  console.log('Alert panel unmounted');
});
</script>

<style scoped>
.alert-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.alert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.alert-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
  font-size: 14px;
}

.alert-title .el-icon {
  color: var(--el-color-primary);
}

.badge {
  margin-left: 8px;
}

.alert-actions {
  display: flex;
  gap: 8px;
}

.alert-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.alert-item {
  background: var(--el-fill-color-light);
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 8px;
  transition: all 0.3s;
  border-left: 3px solid;
}

.alert-item:hover {
  background: var(--el-fill-color);
  transform: translateX(4px);
}

.level-p0, .level-p1 {
  border-left-color: var(--el-color-danger);
}

.level-p2 {
  border-left-color: var(--el-color-warning);
}

.level-p3 {
  border-left-color: var(--el-color-primary);
}

.alert-item-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.level-icon {
  width: 20px;
  height: 20px;
}

.level-p0 .level-icon, .level-p1 .level-icon {
  color: var(--el-color-danger);
}

.level-p2 .level-icon {
  color: var(--el-color-warning);
}

.level-p3 .level-icon {
  color: var(--el-color-primary);
}

.alert-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.alert-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.alert-source {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.alert-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.alert-message {
  font-size: 13px;
  color: var(--el-text-color-primary);
  line-height: 1.5;
  margin-bottom: 8px;
}

.alert-footer {
  padding: 8px 16px;
  border-top: 1px solid var(--el-border-color-light);
  text-align: center;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: var(--el-text-color-secondary);
}

.empty-text {
  margin-top: 12px;
  font-size: 14px;
}

/* Dark theme */
:deep(.alert-panel.theme-dark) {
  --el-fill-color-light: #1a1a2e;
  --el-fill-color: #16213e;
}

:deep(.alert-panel.theme-dark) .alert-item {
  background: #1a1a2e;
}

:deep(.alert-panel.theme-dark) .alert-item:hover {
  background: #16213e;
}
</style>
