<template>
  <div class="global-error-page">
    <a-result
      :status="status"
      :title="title"
      :sub-title="subtitle"
    >
      <template #extra>
        <a-space direction="vertical" :size="16">
          <a-space>
            <a-button type="primary" size="large" @click="handleRetry">
              <template #icon>
                <ReloadOutlined />
              </template>
              重新加载
            </a-button>
            <a-button size="large" @click="handleGoBack">
              <template #icon>
                <ArrowLeftOutlined />
              </template>
              返回上一页
            </a-button>
          </a-space>
          
          <a-button type="link" @click="handleReport">
            <template #icon>
              <BugOutlined />
            </template>
            报告此问题
          </a-button>
        </a-space>
      </template>
    </a-result>
    
    <!-- 错误信息卡片 -->
    <a-card 
      v-if="showDetails" 
      class="error-card"
      title="错误详情"
      :bordered="false"
    >
      <a-descriptions :column="1" size="small">
        <a-descriptions-item label="错误时间">
          {{ formatTime(errorTimestamp) }}
        </a-descriptions-item>
        <a-descriptions-item label="错误类型">
          <a-tag :color="typeColor">{{ errorType }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="错误信息">
          <span class="error-message">{{ errorMessage }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="页面路径">
          {{ currentPath }}
        </a-descriptions-item>
      </a-descriptions>
      
      <a-collapse v-if="errorStack" ghost class="stack-collapse">
        <a-collapse-panel key="stack" header="错误堆栈">
          <pre class="stack-content">{{ errorStack }}</pre>
        </a-collapse-panel>
      </a-collapse>
    </a-card>
    
    <!-- 展开/收起详情按钮 -->
    <div class="details-toggle">
      <a-button 
        type="text" 
        @click="showDetails = !showDetails"
      >
        {{ showDetails ? '隐藏详情' : '显示详情' }}
        <DownOutlined :class="{ 'rotate': showDetails }" />
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  ReloadOutlined, 
  ArrowLeftOutlined, 
  BugOutlined,
  DownOutlined 
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { reportError, categorizeError } from '@/utils/errorReporter'

// Props
interface Props {
  error?: Error | null
  title?: string
  subtitle?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '系统出现异常',
  subtitle: '抱歉，系统遇到了一些问题，请稍后重试'
})

const router = useRouter()
const route = useRoute()

// State
const showDetails = ref(false)
const errorTimestamp = ref(new Date().toISOString())
const isReporting = ref(false)

// Computed
const status = computed(() => {
  if (!props.error) return 'error'
  
  const category = categorizeError(props.error)
  
  if (category === 'network') return '500'
  if (category === 'auth') return '403'
  if (category === 'timeout') return '500'
  
  return 'error'
})

const title = computed(() => {
  if (!props.error) return props.title
  
  const category = categorizeError(props.error)
  
  if (category === 'network') return '网络连接失败'
  if (category === 'auth') return '访问权限不足'
  if (category === 'timeout') return '请求超时'
  if (category === 'type') return '数据处理错误'
  
  return props.title
})

const subtitle = computed(() => {
  if (!props.error) return props.subtitle
  
  const category = categorizeError(props.error)
  
  if (category === 'network') return '请检查网络连接后重试'
  if (category === 'auth') return '您可能需要重新登录或联系管理员'
  if (category === 'timeout') return '服务器响应时间过长'
  
  return props.subtitle
})

const errorType = computed(() => {
  if (!props.error) return 'unknown'
  return categorizeError(props.error)
})

const typeColor = computed(() => {
  const colors: Record<string, string> = {
    network: 'orange',
    timeout: 'blue',
    auth: 'red',
    type: 'purple',
    syntax: 'cyan',
    unknown: 'default'
  }
  
  return colors[errorType.value] || 'default'
})

const errorMessage = computed(() => {
  if (!props.error) return '未知错误'
  return props.error.message || String(props.error)
})

const errorStack = computed(() => {
  if (!props.error?.stack) return ''
  return props.error.stack
})

const currentPath = computed(() => route.fullPath)

// Methods
const formatTime = (time: string) => {
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

const handleRetry = () => {
  message.loading('正在重新加载...')
  
  // 短暂延迟后刷新页面
  setTimeout(() => {
    window.location.reload()
  }, 500)
}

const handleGoBack = () => {
  router.back()
}

const handleReport = async () => {
  if (isReporting.value) return
  
  isReporting.value = true
  
  try {
    await reportError({
      type: 'manual',
      error: props.error || new Error('User reported error'),
      timestamp: new Date().toISOString(),
      url: window.location.href,
      userAgent: navigator.userAgent,
      extra: {
        reportedBy: 'user',
        path: route.fullPath
      }
    })
    
    message.success('问题已上报，我们会尽快处理')
  } catch {
    message.error('上报失败，请稍后重试')
  } finally {
    isReporting.value = false
  }
}
</script>

<style scoped>
.global-error-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
}

.error-card {
  max-width: 600px;
  width: 100%;
  margin-top: 24px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.error-message {
  color: #ff4d4f;
  font-weight: 500;
}

.stack-collapse {
  margin-top: 16px;
}

.stack-content {
  background: #fafafa;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  line-height: 1.6;
  overflow-x: auto;
  max-height: 200px;
  white-space: pre-wrap;
  word-break: break-all;
}

.details-toggle {
  margin-top: 16px;
}

.rotate {
  transform: rotate(180deg);
  transition: transform 0.3s;
}
</style>