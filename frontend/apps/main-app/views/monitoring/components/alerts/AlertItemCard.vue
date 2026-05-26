<template>
  <van-cell-group inset>
    <van-cell 
      :title="alert.title" 
      :value="severityText" 
      :label="formatTime(alert.createdAt)"
    >
      <template #icon>
        <div class="severity-indicator" :class="severityClass"></div>
      </template>
    </van-cell>
    
    <div class="alert-content">
      <div class="alert-description">
        {{ alert.description }}
      </div>
      
      <div class="alert-meta">
        <div class="meta-item">
          <van-icon name="location-o" size="14" />
          <span class="meta-text">{{ alert.source }}</span>
        </div>
        
        <div class="meta-item">
          <van-icon name="clock-o" size="14" />
          <span class="meta-text">{{ formatDuration(alert.createdAt) }}</span>
        </div>
      </div>
      
      <div v-if="alert.tags && alert.tags.length > 0" class="alert-tags">
        <van-tag 
          v-for="tag in alert.tags" 
          :key="tag" 
          type="primary" 
          size="small"
          plain
        >
          {{ tag }}
        </van-tag>
      </div>
    </div>
    
    <div class="alert-actions" v-if="showActions">
      <van-button 
        v-if="alert.status === 'active'" 
        type="primary" 
        size="small" 
        @click="onAcknowledge"
      >
        <van-icon name="passed" />
        确认
      </van-button>
      
      <van-button 
        v-if="alert.status === 'acknowledged'" 
        type="success" 
        size="small" 
        @click="onResolve"
      >
        <van-icon name="success" />
        解决
      </van-button>
      
      <van-button 
        v-if="alert.status === 'resolved'" 
        type="default" 
        size="small" 
        disabled
      >
        <van-icon name="checked" />
        已解决
      </van-button>
      
      <van-button 
        type="default" 
        size="small" 
        @click="onDetails"
      >
        <van-icon name="eye-o" />
        详情
      </van-button>
    </div>
    
    <div v-if="alert.assignedTo" class="alert-assigned">
      <van-tag type="primary" size="small">
        <van-icon name="user-o" />
        处理人: {{ alert.assignedTo }}
      </van-tag>
    </div>
  </van-cell-group>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { showDialog, showToast } from 'vant'
import type { AlertItem } from '@/types/monitoring'

interface Props {
  alert: AlertItem
  showActions?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  showActions: true
})

const emit = defineEmits<{
  acknowledge: [id: string]
  resolve: [id: string]
  details: [id: string]
}>()

const severityText = computed(() => {
  const severityMap = {
    info: '信息',
    warning: '警告',
    error: '错误',
    critical: '严重'
  }
  return severityMap[props.alert.severity] || props.alert.severity
})

const severityClass = computed(() => `severity-${props.alert.severity}`)

const statusText = computed(() => {
  const statusMap = {
    active: '活跃',
    acknowledged: '已确认',
    resolved: '已解决',
    silenced: '已静音'
  }
  return statusMap[props.alert.status] || props.alert.status
})

const formatTime = (timestamp: string) => {
  const date = new Date(timestamp)
  return `${date.getMonth() + 1}/${date.getDate()} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
}

const formatDuration = (timestamp: string) => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return `${Math.floor(diff / 86400000)}天前`
}

const onAcknowledge = () => {
  showDialog({
    title: '确认告警',
    message: `确定要确认告警 "${props.alert.title}" 吗？`,
    showCancelButton: true
  }).then(() => {
    emit('acknowledge', props.alert.id)
    showToast('告警已确认')
  })
}

const onResolve = () => {
  showDialog({
    title: '解决告警',
    message: `确定要标记告警 "${props.alert.title}" 为已解决吗？`,
    showCancelButton: true
  }).then(() => {
    emit('resolve', props.alert.id)
    showToast('告警已解决')
  })
}

const onDetails = () => {
  emit('details', props.alert.id)
}
</script>

<style lang="less" scoped>
.severity-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
  display: inline-block;
  
  &.severity-info {
    background-color: #1989fa;
  }
  
  &.severity-warning {
    background-color: #ff976a;
  }
  
  &.severity-error {
    background-color: #ee0a24;
  }
  
  &.severity-critical {
    background-color: #7232dd;
    animation: pulse 2s infinite;
  }
}

@keyframes pulse {
  0% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
  100% {
    opacity: 1;
  }
}

.alert-content {
  padding: 12px 16px;
  
  .alert-description {
    font-size: 14px;
    color: #333;
    line-height: 1.5;
    margin-bottom: 12px;
  }
  
  .alert-meta {
    display: flex;
    gap: 16px;
    margin-bottom: 12px;
    
    .meta-item {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      color: #969799;
      
      .van-icon {
        flex-shrink: 0;
      }
      
      .meta-text {
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }
  
  .alert-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    
    .van-tag {
      font-size: 11px;
    }
  }
}

.alert-actions {
  padding: 0 16px 12px;
  display: flex;
  gap: 8px;
  
  .van-button {
    flex: 1;
    
    .van-icon {
      margin-right: 4px;
    }
  }
}

.alert-assigned {
  padding: 0 16px 12px;
  
  .van-tag {
    font-size: 11px;
    
    .van-icon {
      margin-right: 4px;
    }
  }
}
</style>