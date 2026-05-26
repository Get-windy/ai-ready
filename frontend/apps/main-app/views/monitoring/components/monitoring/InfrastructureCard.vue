<template>
  <van-cell-group inset>
    <van-cell :title="item.name" :value="statusText" :label="typeLabel">
      <template #icon>
        <div class="status-indicator" :class="statusClass"></div>
      </template>
      <template #right-icon>
        <van-icon name="arrow" />
      </template>
    </van-cell>
    
    <van-grid :column-num="3" :border="false" class="metrics-grid">
      <van-grid-item>
        <div class="metric-item">
          <div class="metric-label">CPU</div>
          <div class="metric-value" :class="getUsageClass(item.cpuUsage)">
            {{ item.cpuUsage }}%
          </div>
        </div>
      </van-grid-item>
      <van-grid-item>
        <div class="metric-item">
          <div class="metric-label">内存</div>
          <div class="metric-value" :class="getUsageClass(item.memoryUsage)">
            {{ item.memoryUsage }}%
          </div>
        </div>
      </van-grid-item>
      <van-grid-item>
        <div class="metric-item">
          <div class="metric-label">磁盘</div>
          <div class="metric-value" :class="getUsageClass(item.diskUsage)">
            {{ item.diskUsage }}%
          </div>
        </div>
      </van-grid-item>
    </van-grid>
    
    <div class="additional-info">
      <van-tag 
        v-if="item.uptime > 0" 
        type="primary" 
        size="small"
      >
        运行 {{ formatUptime(item.uptime) }}
      </van-tag>
      <van-tag 
        v-if="item.networkIn > 0 || item.networkOut > 0" 
        type="success" 
        size="small"
      >
        网络 {{ formatNetwork(item.networkIn) }}/{{ formatNetwork(item.networkOut) }}
      </van-tag>
    </div>
  </van-cell-group>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { InfrastructureStatus } from '@/types/monitoring'

interface Props {
  item: InfrastructureStatus
  showDetails?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showDetails: true
})

const statusText = computed(() => {
  const statusMap = {
    healthy: '健康',
    warning: '警告',
    critical: '严重',
    offline: '离线'
  }
  return statusMap[props.item.status] || props.item.status
})

const statusClass = computed(() => `status-${props.item.status}`)

const typeLabel = computed(() => {
  const typeMap = {
    server: '服务器',
    database: '数据库',
    cache: '缓存',
    network: '网络设备',
    storage: '存储'
  }
  return typeMap[props.item.type] || props.item.type
})

const getUsageClass = (usage: number) => {
  if (usage >= 85) return 'usage-critical'
  if (usage >= 70) return 'usage-warning'
  return 'usage-normal'
}

const formatUptime = (seconds: number) => {
  if (seconds < 60) return `${seconds}秒`
  if (seconds < 3600) return `${Math.floor(seconds / 60)}分钟`
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}小时`
  return `${Math.floor(seconds / 86400)}天`
}

const formatNetwork = (bytes: number) => {
  if (bytes < 1024) return `${bytes}B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)}KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)}MB`
}
</script>

<style lang="less" scoped>
.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
  display: inline-block;
  
  &.status-healthy {
    background-color: #07c160;
  }
  
  &.status-warning {
    background-color: #ff976a;
  }
  
  &.status-critical {
    background-color: #ee0a24;
  }
  
  &.status-offline {
    background-color: #969799;
  }
}

.metrics-grid {
  margin: 8px 0;
  padding: 0 16px;
  
  .metric-item {
    text-align: center;
    
    .metric-label {
      font-size: 12px;
      color: #969799;
      margin-bottom: 4px;
    }
    
    .metric-value {
      font-size: 16px;
      font-weight: 600;
      
      &.usage-normal {
        color: #07c160;
      }
      
      &.usage-warning {
        color: #ff976a;
      }
      
      &.usage-critical {
        color: #ee0a24;
      }
    }
  }
}

.additional-info {
  padding: 8px 16px;
  display: flex;
  gap: 8px;
  
  .van-tag {
    font-size: 11px;
  }
}
</style>