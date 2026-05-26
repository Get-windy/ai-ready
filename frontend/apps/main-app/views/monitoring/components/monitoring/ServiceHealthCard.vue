<template>
  <van-cell-group inset>
    <van-cell :title="item.name" :value="statusText" :label="`版本 ${item.version}`">
      <template #icon>
        <div class="status-indicator" :class="statusClass"></div>
      </template>
      <template #right-icon>
        <van-icon name="arrow" />
      </template>
    </van-cell>
    
    <div class="service-metrics">
      <div class="metric-row">
        <div class="metric-item">
          <div class="metric-label">实例</div>
          <div class="metric-value">
            <span class="healthy-count">{{ item.healthyInstances }}</span>
            <span class="total-count">/{{ item.instances }}</span>
          </div>
        </div>
        
        <div class="metric-item">
          <div class="metric-label">响应时间</div>
          <div class="metric-value" :class="getResponseTimeClass(item.responseTime)">
            {{ item.responseTime }}ms
          </div>
        </div>
        
        <div class="metric-item">
          <div class="metric-label">错误率</div>
          <div class="metric-value" :class="getErrorRateClass(item.errorRate)">
            {{ item.errorRate.toFixed(1) }}%
          </div>
        </div>
      </div>
      
      <div class="metric-row">
        <div class="metric-item">
          <div class="metric-label">吞吐量</div>
          <div class="metric-value throughput">
            {{ formatThroughput(item.throughput) }}
          </div>
        </div>
      </div>
    </div>
    
    <div v-if="item.dependencies && item.dependencies.length > 0" class="dependencies">
      <div class="dependencies-label">依赖服务：</div>
      <div class="dependencies-list">
        <van-tag 
          v-for="dep in item.dependencies" 
          :key="dep" 
          type="primary" 
          size="small"
        >
          {{ dep }}
        </van-tag>
      </div>
    </div>
    
    <div class="last-updated">
      <van-tag type="default" size="small">
        更新于 {{ formatTime(item.lastUpdated) }}
      </van-tag>
    </div>
  </van-cell-group>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ServiceHealth } from '@/types/monitoring'

interface Props {
  item: ServiceHealth
  showDetails?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showDetails: true
})

const statusText = computed(() => {
  const statusMap = {
    up: '运行正常',
    degraded: '性能下降',
    down: '服务异常'
  }
  return statusMap[props.item.status] || props.item.status
})

const statusClass = computed(() => `status-${props.item.status}`)

const getResponseTimeClass = (responseTime: number) => {
  if (responseTime >= 1000) return 'response-critical'
  if (responseTime >= 500) return 'response-warning'
  return 'response-normal'
}

const getErrorRateClass = (errorRate: number) => {
  if (errorRate >= 5) return 'error-critical'
  if (errorRate >= 2) return 'error-warning'
  return 'error-normal'
}

const formatThroughput = (throughput: number) => {
  if (throughput >= 1000) return `${(throughput / 1000).toFixed(1)}K/s`
  return `${throughput}/s`
}

const formatTime = (timestamp: string) => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return `${Math.floor(diff / 86400000)}天前`
}
</script>

<style lang="less" scoped>
.status-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
  display: inline-block;
  
  &.status-up {
    background-color: #07c160;
  }
  
  &.status-degraded {
    background-color: #ff976a;
  }
  
  &.status-down {
    background-color: #ee0a24;
  }
}

.service-metrics {
  padding: 12px 16px;
  
  .metric-row {
    display: flex;
    justify-content: space-between;
    margin-bottom: 12px;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
  
  .metric-item {
    flex: 1;
    text-align: center;
    
    .metric-label {
      font-size: 12px;
      color: #969799;
      margin-bottom: 4px;
    }
    
    .metric-value {
      font-size: 16px;
      font-weight: 600;
      
      .healthy-count {
        color: #07c160;
      }
      
      .total-count {
        color: #969799;
        font-size: 14px;
      }
      
      &.response-normal {
        color: #07c160;
      }
      
      &.response-warning {
        color: #ff976a;
      }
      
      &.response-critical {
        color: #ee0a24;
      }
      
      &.error-normal {
        color: #07c160;
      }
      
      &.error-warning {
        color: #ff976a;
      }
      
      &.error-critical {
        color: #ee0a24;
      }
      
      &.throughput {
        color: #1989fa;
      }
    }
  }
}

.dependencies {
  padding: 0 16px 12px;
  
  .dependencies-label {
    font-size: 12px;
    color: #969799;
    margin-bottom: 8px;
  }
  
  .dependencies-list {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    
    .van-tag {
      font-size: 11px;
    }
  }
}

.last-updated {
  padding: 0 16px 12px;
  
  .van-tag {
    font-size: 11px;
  }
}
</style>