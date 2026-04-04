<template>
  <slot v-if="!hasError" />
  <div v-else class="error-boundary-fallback">
    <a-result
      status="error"
      :title="errorTitle"
      :sub-title="errorSubtitle"
    >
      <template #extra>
        <a-space>
          <a-button type="primary" @click="handleRetry">
            <template #icon>
              <ReloadOutlined />
            </template>
            重新尝试
          </a-button>
          <a-button @click="handleGoHome">
            <template #icon>
              <HomeOutlined />
            </template>
            返回首页
          </a-button>
          <a-button type="link" @click="handleShowDetails">
            查看详情
          </a-button>
        </a-space>
      </template>
    </a-result>
    
    <!-- 错误详情弹窗 -->
    <a-modal
      v-model:open="showDetails"
      title="错误详情"
      :footer="null"
      width="600px"
    >
      <div class="error-details">
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="错误类型">
            {{ errorInfo?.name || 'Unknown' }}
          </a-descriptions-item>
          <a-descriptions-item label="错误消息">
            {{ errorInfo?.message || '无' }}
          </a-descriptions-item>
          <a-descriptions-item label="发生时间">
            {{ errorTime }}
          </a-descriptions-item>
          <a-descriptions-item label="组件路径">
            {{ componentPath || '未知' }}
          </a-descriptions-item>
        </a-descriptions>
        
        <div class="error-stack" v-if="errorInfo?.stack">
          <a-divider>错误堆栈</a-divider>
          <pre class="stack-trace">{{ errorInfo.stack }}</pre>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onErrorCaptured, type ComponentPublicInstance } from 'vue'
import { useRouter } from 'vue-router'
import { ReloadOutlined, HomeOutlined } from '@ant-design/icons-vue'
import { reportError } from '@/utils/errorReporter'
import { message } from 'ant-design-vue'

// Props
interface Props {
  fallbackTitle?: string
  fallbackSubtitle?: string
  onError?: (error: Error, instance: ComponentPublicInstance, info: string) => void
  retryable?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  fallbackTitle: '页面出现异常',
  fallbackSubtitle: '抱歉，页面遇到了一些问题，请尝试重新加载',
  retryable: true
})

// Emits
const emit = defineEmits<{
  error: [error: Error, instance: ComponentPublicInstance, info: string]
  retry: []
}>()

// State
const hasError = ref(false)
const errorInfo = ref<Error | null>(null)
const errorTime = ref('')
const componentPath = ref('')
const showDetails = ref(false)

const router = useRouter()

// 错误标题和副标题
const errorTitle = ref(props.fallbackTitle)
const errorSubtitle = ref(props.fallbackSubtitle)

// 错误捕获钩子
onErrorCaptured((error: Error, instance: ComponentPublicInstance, info: string) => {
  // 设置错误状态
  hasError.value = true
  errorInfo.value = error
  errorTime.value = new Date().toLocaleString('zh-CN')
  componentPath.value = info
  
  // 根据错误类型调整显示信息
  if (error.message.includes('network') || error.message.includes('Network')) {
    errorTitle.value = '网络连接异常'
    errorSubtitle.value = '请检查网络连接后重试'
  } else if (error.message.includes('timeout') || error.message.includes('Timeout')) {
    errorTitle.value = '请求超时'
    errorSubtitle.value = '服务响应时间过长，请稍后重试'
  } else if (error.message.includes('auth') || error.message.includes('Unauthorized')) {
    errorTitle.value = '权限不足'
    errorSubtitle.value = '您可能需要重新登录'
  }
  
  // 上报错误
  reportError({
    type: 'component',
    error: error,
    componentInfo: info,
    timestamp: new Date().toISOString(),
    componentInstance: instance.$options?.name || 'Unknown'
  })
  
  // 触发回调
  props.onError?.(error, instance, info)
  emit('error', error, instance, info)
  
  // 显示错误提示
  message.error(`组件错误: ${error.message}`)
  
  // 返回 false 阻止错误继续向上传播
  return false
})

// 重试处理
const handleRetry = () => {
  hasError.value = false
  errorInfo.value = null
  errorTime.value = ''
  componentPath.value = ''
  showDetails.value = false
  
  // 重置标题
  errorTitle.value = props.fallbackTitle
  errorSubtitle.value = props.fallbackSubtitle
  
  emit('retry')
  message.success('正在重新加载...')
}

// 返回首页
const handleGoHome = () => {
  router.push('/')
  // 重置错误状态
  hasError.value = false
}

// 显示详情
const handleShowDetails = () => {
  showDetails.value = true
}
</script>

<style scoped>
.error-boundary-fallback {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  padding: 40px 20px;
  background: #fff;
  border-radius: 8px;
}

.error-details {
  padding: 16px 0;
}

.error-stack {
  margin-top: 16px;
}

.stack-trace {
  background: #f5f5f5;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 12px;
  font-size: 12px;
  line-height: 1.6;
  overflow-x: auto;
  max-height: 300px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>