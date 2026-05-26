<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  type?: 'network' | 'server' | 'permission' | 'notfound' | 'generic'
  title?: string
  message?: string
  showRetry?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  type: 'generic',
  title: '',
  message: '',
  showRetry: true
})

const emit = defineEmits<{
  retry: []
}>()

const errorConfig = {
  network: { icon: '📡', defaultTitle: '网络错误', defaultMessage: '网络连接失败，请检查网络设置' },
  server: { icon: '🖥', defaultTitle: '服务器错误', defaultMessage: '服务器响应异常，请稍后重试' },
  permission: { icon: '🔒', defaultTitle: '权限不足', defaultMessage: '您没有权限访问此内容' },
  notfound: { icon: '🔍', defaultTitle: '内容不存在', defaultMessage: '您访问的内容不存在或已删除' },
  generic: { icon: '⚠', defaultTitle: '出错了', defaultMessage: '发生未知错误' }
}

const config = computed(() => errorConfig[props.type])

const displayTitle = computed(() => props.title || config.value.defaultTitle)
const displayMessage = computed(() => props.message || config.value.defaultMessage)

const handleRetry = () => {
  emit('retry')
}
</script>

<template>
  <div class="ar-error">
    <div class="ar-error-icon">{{ config.icon }}</div>
    
    <div class="ar-error-content">
      <div class="ar-error-title">{{ displayTitle }}</div>
      <div class="ar-error-message">{{ displayMessage }}</div>
      <slot />
    </div>
    
    <button 
      v-if="showRetry"
      class="ar-error-retry"
      @click="handleRetry"
    >
      重试
    </button>
  </div>
</template>

<style lang="scss" scoped>
.ar-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 16px;
  text-align: center;
  
  .ar-error-icon {
    font-size: 48px;
    margin-bottom: 16px;
    opacity: 0.8;
  }
  
  .ar-error-content {
    max-width: 300px;
    
    .ar-error-title {
      font-size: 16px;
      font-weight: 600;
      color: #333;
      margin-bottom: 8px;
    }
    
    .ar-error-message {
      font-size: 14px;
      color: #969799;
      line-height: 1.5;
    }
  }
  
  .ar-error-retry {
    margin-top: 16px;
    padding: 8px 24px;
    background: #1988fa;
    color: #fff;
    border: none;
    border-radius: 4px;
    font-size: 14px;
    cursor: pointer;
    
    &:hover {
      background: #0e7cd3;
    }
  }
}
</style>