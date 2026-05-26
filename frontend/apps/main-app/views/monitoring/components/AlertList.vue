<template>
  <div class="alert-list">
    <div class="alert-list-header">
      <div class="header-title">
        <el-icon><Bell /></el-icon>
        <span>实时告警</span>
        <el-badge :value="unacknowledgedCount" :max="99" v-if="unacknowledgedCount > 0" />
      </div>
      <div class="header-actions">
        <el-radio-group v-model="filterLevel" size="small">
          <el-radio-button label="">全部</el-radio-button>
          <el-radio-button label="P0">P0</el-radio-button>
          <el-radio-button label="P1">P1</el-radio-button>
          <el-radio-button label="P2">P2</el-radio-button>
        </el-radio-group>
        <el-button 
          type="primary" 
          size="small" 
          text
          @click="handleAcknowledgeAll"
          :disabled="unacknowledgedCount === 0"
        >
          全部确认
        </el-button>
      </div>
    </div>
    
    <div class="alert-list-content" v-loading="loading">
      <div v-if="filteredAlerts.length === 0" class="empty-state">
        <el-icon :size="48"><CircleCheck /></el-icon>
        <p>暂无告警</p>
        <span>系统运行正常</span>
      </div>
      
      <div v-else class="alert-items">
        <AlertNotification
          v-for="alert in filteredAlerts"
          :key="alert.id"
          :alert="alert"
          @acknowledge="handleAcknowledge"
          @dismiss="handleDismiss"
        />
      </div>
    </div>
    
    <div class="alert-list-footer" v-if="filteredAlerts.length > 0">
      <span class="alert-stats">
        共 {{ filteredAlerts.length }} 条告警
        <template v-if="unacknowledgedCount > 0">
          ，{{ unacknowledgedCount }} 条未确认
        </template>
      </span>
      <el-button type="primary" link size="small" @click="handleViewAll">
        查看全部
        <el-icon><ArrowRight /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import { Bell, CircleCheck, ArrowRight } from '@element-plus/icons-vue';
import AlertNotification from './AlertNotification.vue';
import type { AlertItem, AlertLevel } from '../types/monitoring';

interface Props {
  alerts: AlertItem[];
  loading?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
});

const emit = defineEmits<{
  acknowledge: [id: string];
  acknowledgeAll: [];
  dismiss: [id: string];
  viewAll: [];
}>();

const filterLevel = ref<AlertLevel | ''>('');

const filteredAlerts = computed(() => {
  let result = props.alerts;
  
  if (filterLevel.value) {
    result = result.filter(alert => alert.level === filterLevel.value);
  }
  
  // Sort by timestamp desc, unacknowledged first
  return result.sort((a, b) => {
    if (a.acknowledged !== b.acknowledged) {
      return a.acknowledged ? 1 : -1;
    }
    return new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime();
  });
});

const unacknowledgedCount = computed(() => {
  return props.alerts.filter(alert => !alert.acknowledged).length;
});

const handleAcknowledge = (id: string) => {
  emit('acknowledge', id);
};

const handleAcknowledgeAll = () => {
  emit('acknowledgeAll');
};

const handleDismiss = (id: string) => {
  emit('dismiss', id);
};

const handleViewAll = () => {
  emit('viewAll');
};
</script>

<style scoped>
.alert-list {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.alert-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #EBEEF5;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.header-title .el-icon {
  color: #E6A23C;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.alert-list-content {
  padding: 16px 20px;
  min-height: 200px;
  max-height: 400px;
  overflow-y: auto;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  color: #67C23A;
}

.empty-state p {
  margin: 12px 0 4px;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}

.empty-state span {
  font-size: 13px;
  color: #909399;
}

.alert-items {
  display: flex;
  flex-direction: column;
}

.alert-list-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-top: 1px solid #EBEEF5;
  background: #F5F7FA;
}

.alert-stats {
  font-size: 13px;
  color: #606266;
}
</style>
