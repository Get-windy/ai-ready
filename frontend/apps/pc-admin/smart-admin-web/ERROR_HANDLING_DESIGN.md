# AI-Ready 前端错误处理机制设计文档

> **文档版本**: v1.0
> **创建日期**: 2026-04-12
> **负责人**: 前端开发工程师

---

## 一、错误分类体系

### 1.1 错误类型定义

```typescript
/**
 * 错误类型枚举
 */
export enum ErrorType {
  // 网络错误
  NETWORK = 'NETWORK',           // 网络连接失败
  TIMEOUT = 'TIMEOUT',           // 请求超时
  OFFLINE = 'OFFLINE',           // 离线状态

  // HTTP 错误
  HTTP_ERROR = 'HTTP_ERROR',     // HTTP 状态码错误
  HTTP_400 = 'HTTP_400',         // 错误请求
  HTTP_401 = 'HTTP_401',         // 未授权
  HTTP_403 = 'HTTP_403',         // 禁止访问
  HTTP_404 = 'HTTP_404',         // 资源不存在
  HTTP_500 = 'HTTP_500',         // 服务器内部错误
  HTTP_502 = 'HTTP_502',         // 网关错误
  HTTP_503 = 'HTTP_503',         // 服务不可用

  // 业务错误
  BUSINESS_ERROR = 'BUSINESS_ERROR', // 通用业务错误
  VALIDATION_ERROR = 'VALIDATION_ERROR', // 参数验证错误
  AUTH_ERROR = 'AUTH_ERROR',         // 权限错误
  DUPLICATE_ERROR = 'DUPLICATE_ERROR', // 重复数据错误

  // 系统错误
  COMPONENT_ERROR = 'COMPONENT_ERROR',     // 组件渲染错误
  RUNTIME_ERROR = 'RUNTIME_ERROR',         // 运行时错误
  REFERENCE_ERROR = 'REFERENCE_ERROR',     // 引用错误
  TYPE_ERROR = 'TYPE_ERROR',               // 类型错误
  SYNTAX_ERROR = 'SYNTAX_ERROR',           // 语法错误
  PROMISE_ERROR = 'PROMISE_ERROR',         // Promise 错误
  RESOURCE_ERROR = 'RESOURCE_ERROR',       // 资源加载错误

  // 未知错误
  UNKNOWN = 'UNKNOWN'
}

/**
 * 错误级别
 */
export enum ErrorLevel {
  INFO = 'INFO',       // 信息
  WARNING = 'WARNING', // 警告
  ERROR = 'ERROR',     // 错误
  FATAL = 'FATAL'      // 致命错误
}

/**
 * 错误信息接口
 */
export interface ErrorInfo {
  type: ErrorType
  level: ErrorLevel
  code?: string
  message: string
  detail?: string
  timestamp: string
  stack?: string
  url?: string
  userAgent?: string
  userId?: string
  extra?: Record<string, any>
}
```

### 1.2 错误分类映射

| 错误源 | 错误类型 | 错误级别 | 处理方式 |
|--------|----------|----------|----------|
| **网络请求** | NETWORK/TIMEOUT/OFFLINE | WARNING | Toast 提示 + 重试 |
| **HTTP 4xx** | HTTP_400/HTTP_401/HTTP_403/HTTP_404 | ERROR | Toast/Modal + 相应操作 |
| **HTTP 5xx** | HTTP_500/HTTP_502/HTTP_503 | FATAL | Modal + 错误上报 |
| **业务逻辑** | BUSINESS_ERROR/VALIDATION_ERROR/AUTH_ERROR | WARNING | Toast + 表单验证 |
| **组件渲染** | COMPONENT_ERROR | ERROR | ErrorBoundary + 上报 |
| **JavaScript 运行时** | RUNTIME_ERROR/REFERENCE_ERROR/TYPE_ERROR | FATAL | Global Handler + 上报 |
| **Promise** | PROMISE_ERROR | ERROR | UnhandledRejection + 上报 |
| **资源加载** | RESOURCE_ERROR | WARNING | Console + 上报 |

---

## 二、全局错误拦截

### 2.1 Axios 拦截器（已实现，需增强）

```typescript
// src/utils/request.ts
import axios from 'axios'
import { message, Modal } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'
import { ErrorType, ErrorLevel, type ErrorInfo } from '@/types/error'
import { errorManager } from '@/utils/errorManager'

// 错误代码映射
const errorCodeMap: Record<number, ErrorType> = {
  400: ErrorType.HTTP_400,
  401: ErrorType.HTTP_401,
  403: ErrorType.HTTP_403,
  404: ErrorType.HTTP_404,
  500: ErrorType.HTTP_500,
  502: ErrorType.HTTP_502,
  503: ErrorType.HTTP_503,
}

// 业务错误代码映射
const businessErrorCodeMap: Record<string, ErrorType> = {
  'VALIDATION_ERROR': ErrorType.VALIDATION_ERROR,
  'AUTH_ERROR': ErrorType.AUTH_ERROR,
  'DUPLICATE_ERROR': ErrorType.DUPLICATE_ERROR,
  'BUSINESS_ERROR': ErrorType.BUSINESS_ERROR,
}

const service = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    errorManager.handleError({
      type: ErrorType.NETWORK,
      level: ErrorLevel.ERROR,
      message: '请求配置错误',
      detail: error.message,
      timestamp: new Date().toISOString(),
      extra: { config }
    })
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: any) => {
    const { code, message: msg, data } = response.data

    // 成功响应
    if (code === 200) {
      return response.data
    }

    // 业务错误
    const errorType = businessErrorCodeMap[code] || ErrorType.BUSINESS_ERROR
    const errorInfo: ErrorInfo = {
      type: errorType,
      level: ErrorLevel.WARNING,
      code: String(code),
      message: msg || '请求失败',
      timestamp: new Date().toISOString(),
      url: response.config?.url,
      extra: { responseData: response.data }
    }

    // 401 未授权 - 跳转登录
    if (code === 401) {
      const userStore = useUserStore()
      userStore.logout()
      Modal.warning({
        title: '登录已过期',
        content: '您的登录已过期，请重新登录',
        okText: '确定',
        onOk: () => {
          window.location.href = '/login'
        }
      })
      return Promise.reject(new Error('登录已过期'))
    }

    // 其他业务错误
    errorManager.handleError(errorInfo)
    return Promise.reject(new Error(msg))
  },
  (error) => {
    // 网络错误
    if (!error.response) {
      // 请求超时
      if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
        errorManager.handleError({
          type: ErrorType.TIMEOUT,
          level: ErrorLevel.WARNING,
          message: '请求超时，请稍后重试',
          timestamp: new Date().toISOString()
        })
      }
      // 离线状态
      else if (!navigator.onLine) {
        errorManager.handleError({
          type: ErrorType.OFFLINE,
          level: ErrorLevel.WARNING,
          message: '网络连接已断开，请检查网络',
          timestamp: new Date().toISOString()
        })
      }
      // 其他网络错误
      else {
        errorManager.handleError({
          type: ErrorType.NETWORK,
          level: ErrorLevel.ERROR,
          message: '网络连接失败，请检查网络设置',
          detail: error.message,
          timestamp: new Date().toISOString()
        })
      }
      return Promise.reject(error)
    }

    // HTTP 错误
    const { status, data } = error.response
    const errorType = errorCodeMap[status] || ErrorType.HTTP_ERROR

    const errorInfo: ErrorInfo = {
      type: errorType,
      level: status >= 500 ? ErrorLevel.FATAL : ErrorLevel.ERROR,
      code: String(status),
      message: data?.message || `请求错误 (${status})`,
      detail: data?.detail,
      timestamp: new Date().toISOString(),
      url: error.config?.url,
      stack: error.stack
    }

    errorManager.handleError(errorInfo)
    return Promise.reject(error)
  }
)

export default service
```

### 2.2 Vue 全局错误处理（已实现，需增强）

```typescript
// src/main.ts
import { createApp } from 'vue'
import { initErrorReporter } from '@/utils/errorReporter'
import { errorManager } from '@/utils/errorManager'
import { ErrorType, ErrorLevel } from '@/types/error'

const app = createApp(App)

// 初始化错误上报器
initErrorReporter(app, {
  enabled: import.meta.env.PROD,
  endpoint: '/api/log/error',
  batchSize: 10,
  flushInterval: 5000
})

// 增强的全局错误处理器
app.config.errorHandler = (err, instance, info) => {
  errorManager.handleError({
    type: ErrorType.COMPONENT_ERROR,
    level: ErrorLevel.ERROR,
    message: err instanceof Error ? err.message : String(err),
    detail: info,
    timestamp: new Date().toISOString(),
    stack: err instanceof Error ? err.stack : undefined,
    componentInstance: instance?.$options?.name || 'Unknown'
  })
}

// 捕获未处理的 Promise 错误
window.addEventListener('unhandledrejection', (event) => {
  errorManager.handleError({
    type: ErrorType.PROMISE_ERROR,
    level: ErrorLevel.ERROR,
    message: event.reason instanceof Error ? event.reason.message : String(event.reason),
    timestamp: new Date().toISOString(),
    stack: event.reason instanceof Error ? event.reason.stack : undefined
  })
})

// 捕获资源加载错误
window.addEventListener('error', (event) => {
  if (event.target !== window) {
    const target = event.target as HTMLElement
    errorManager.handleError({
      type: ErrorType.RESOURCE_ERROR,
      level: ErrorLevel.WARNING,
      message: `资源加载失败: ${target.src || target.href}`,
      timestamp: new Date().toISOString(),
      extra: {
        tagName: target.tagName,
        src: target.src,
        href: target.href
      }
    })
  }
}, true)
```

---

## 三、错误提示组件设计

### 3.1 Toast 提示（轻量级错误）

```typescript
// src/utils/errorManager.ts
import { message, notification, Modal } from 'ant-design-vue'
import type { ErrorInfo } from '@/types/error'

/**
 * 错误管理器
 */
class ErrorManager {
  private errorQueue: ErrorInfo[] = []
  private isShowingModal = false

  /**
   * 处理错误
   */
  handleError(errorInfo: ErrorInfo) {
    // 记录错误
    this.errorQueue.push(errorInfo)

    // 根据错误级别和类型选择展示方式
    switch (errorInfo.level) {
      case 'INFO':
        this.showInfo(errorInfo)
        break
      case 'WARNING':
        this.showWarning(errorInfo)
        break
      case 'ERROR':
        this.showError(errorInfo)
        break
      case 'FATAL':
        this.showFatal(errorInfo)
        break
    }
  }

  /**
   * 信息提示
   */
  private showInfo(errorInfo: ErrorInfo) {
    message.info(errorInfo.message, 3)
  }

  /**
   * 警告提示
   */
  private showWarning(errorInfo: ErrorInfo) {
    message.warning(errorInfo.message, 3)
  }

  /**
   * 错误提示
   */
  private showError(errorInfo: ErrorInfo) {
    // 可重试的错误提供重试选项
    if (this.isRetryable(errorInfo.type)) {
      notification.error({
        message: '操作失败',
        description: errorInfo.message,
        duration: 0,
        key: `error-${Date.now()}`,
        btn: () => this.createRetryButton(errorInfo)
      })
    } else {
      message.error(errorInfo.message, 5)
    }
  }

  /**
   * 致命错误 - 使用 Modal
   */
  private showFatal(errorInfo: ErrorInfo) {
    if (this.isShowingModal) return
    this.isShowingModal = true

    Modal.error({
      title: '发生错误',
      content: errorInfo.message,
      okText: '确定',
      onOk: () => {
        this.isShowingModal = false
      },
      // 可选：显示错误详情
      afterClose: () => {
        this.isShowingModal = false
      }
    })
  }

  /**
   * 创建重试按钮
   */
  private createRetryButton(errorInfo: ErrorInfo) {
    return () => {
      return (
        <a-button
          type="primary"
          size="small"
          onClick={() => {
            notification.close(`error-${Date.now()}`)
            // 触发重试逻辑
            this.retry(errorInfo)
          }}
        >
          重试
        </a-button>
      )
    }
  }

  /**
   * 判断是否可重试
   */
  private isRetryable(errorType: ErrorType): boolean {
    return [
      ErrorType.NETWORK,
      ErrorType.TIMEOUT,
      ErrorType.HTTP_500,
      ErrorType.HTTP_502,
      ErrorType.HTTP_503
    ].includes(errorType)
  }

  /**
   * 重试逻辑（由具体业务实现）
   */
  private retry(errorInfo: ErrorInfo) {
    console.log('Retry:', errorInfo)
    // 这里可以触发自定义重试事件
    window.dispatchEvent(new CustomEvent('error-retry', { detail: errorInfo }))
  }

  /**
   * 获取错误历史
   */
  getErrorHistory(): ErrorInfo[] {
    return [...this.errorQueue]
  }

  /**
   * 清空错误历史
   */
  clearErrorHistory() {
    this.errorQueue = []
  }
}

export const errorManager = new ErrorManager()
export default errorManager
```

### 3.2 Modal 弹窗（严重错误）

```vue
<!-- src/components/ErrorModal/ErrorModal.vue -->
<template>
  <a-modal
    v-model:open="visible"
    :title="errorInfo?.level === 'FATAL' ? '严重错误' : '错误'"
    :footer="null"
    width="600px"
    @cancel="handleClose"
  >
    <div class="error-modal-content">
      <!-- 错误类型图标 -->
      <div class="error-icon">
        <ExclamationCircleOutlined v-if="errorInfo?.level === 'ERROR'" class="error-error" />
        <WarningOutlined v-else-if="errorInfo?.level === 'WARNING'" class="error-warning" />
        <CloseCircleOutlined v-else class="error-fatal" />
      </div>

      <!-- 错误信息 -->
      <div class="error-message">
        <h3>{{ errorInfo?.message }}</h3>
        <p v-if="errorInfo?.detail" class="error-detail">{{ errorInfo.detail }}</p>
      </div>

      <!-- 错误详情（可折叠） -->
      <a-collapse v-if="showDetails" ghost class="error-details">
        <a-collapse-panel key="1" header="查看详情">
          <a-descriptions :column="1" bordered size="small">
            <a-descriptions-item label="错误类型">
              {{ errorInfo?.type }}
            </a-descriptions-item>
            <a-descriptions-item label="错误代码">
              {{ errorInfo?.code || '-' }}
            </a-descriptions-item>
            <a-descriptions-item label="发生时间">
              {{ errorInfo?.timestamp }}
            </a-descriptions-item>
            <a-descriptions-item label="URL">
              {{ errorInfo?.url || '-' }}
            </a-descriptions-item>
          </a-descriptions>

          <div v-if="errorInfo?.stack" class="error-stack">
            <h4>堆栈信息</h4>
            <pre>{{ errorInfo.stack }}</pre>
          </div>

          <div v-if="errorInfo?.extra" class="error-extra">
            <h4>额外信息</h4>
            <pre>{{ JSON.stringify(errorInfo.extra, null, 2) }}</pre>
          </div>
        </a-collapse-panel>
      </a-collapse>

      <!-- 操作按钮 -->
      <div class="error-actions">
        <a-space>
          <a-button v-if="errorInfo?.level !== 'FATAL'" @click="handleClose">
            关闭
          </a-button>
          <a-button @click="showDetails = !showDetails">
            {{ showDetails ? '隐藏详情' : '查看详情' }}
          </a-button>
          <a-button type="primary" @click="handleCopyError">
            复制错误信息
          </a-button>
        </a-space>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import {
  ExclamationCircleOutlined,
  WarningOutlined,
  CloseCircleOutlined
} from '@ant-design/icons-vue'
import type { ErrorInfo } from '@/types/error'
import { message } from 'ant-design-vue'

interface Props {
  visible: boolean
  errorInfo?: ErrorInfo
}

const props = defineProps<Props>()
const emit = defineEmits<{
  close: []
}>()

const showDetails = ref(false)
const visible = ref(props.visible)

watch(() => props.visible, (val) => {
  visible.value = val
  showDetails.value = false
})

const handleClose = () => {
  visible.value = false
  emit('close')
}

const handleCopyError = async () => {
  if (!props.errorInfo) return

  const errorText = `
错误类型: ${props.errorInfo.type}
错误代码: ${props.errorInfo.code || '-'}
错误消息: ${props.errorInfo.message}
详细信息: ${props.errorInfo.detail || '-'}
发生时间: ${props.errorInfo.timestamp}
URL: ${props.errorInfo.url || '-'}
堆栈信息:
${props.errorInfo.stack || '无'}
  `.trim()

  try {
    await navigator.clipboard.writeText(errorText)
    message.success('错误信息已复制')
  } catch {
    message.error('复制失败')
  }
}
</script>

<style scoped>
.error-modal-content {
  padding: 16px 0;
}

.error-icon {
  text-align: center;
  margin-bottom: 24px;
  font-size: 64px;
}

.error-error {
  color: #ff4d4f;
}

.error-warning {
  color: #faad14;
}

.error-fatal {
  color: #cf1322;
}

.error-message {
  text-align: center;
  margin-bottom: 24px;
}

.error-message h3 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #262626;
}

.error-detail {
  margin: 0;
  color: #8c8c8c;
  font-size: 14px;
}

.error-details {
  margin-bottom: 24px;
}

.error-stack,
.error-extra {
  margin-top: 16px;
}

.error-stack h4,
.error-extra h4 {
  margin: 0 0 8px;
  font-size: 14px;
  color: #595959;
}

.error-stack pre,
.error-extra pre {
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

.error-actions {
  text-align: center;
}
</style>
```

### 3.3 Notification 通知（可操作错误）

```typescript
// src/utils/notificationHelper.ts
import { notification, type NotificationArgsProps } from 'ant-design-vue'
import type { ErrorInfo } from '@/types/error'

/**
 * 创建带操作的通知
 */
export function createErrorNotification(
  errorInfo: ErrorInfo,
  actions?: Array<{
    label: string
    onClick: () => void
    type?: 'primary' | 'default'
  }>
) {
  const key = `error-${Date.now()}`

  const config: NotificationArgsProps = {
    message: errorInfo.level === 'FATAL' ? '严重错误' : '操作失败',
    description: errorInfo.message,
    duration: 0,
    key,
    onClose: () => {
      notification.close(key)
    }
  }

  // 添加操作按钮
  if (actions && actions.length > 0) {
    config.btn = () => {
      return (
        <a-space>
          {actions.map((action, index) => (
            <a-button
              key={index}
              type={action.type || 'default'}
              size="small"
              onClick={() => {
                action.onClick()
                notification.close(key)
              }}
            >
              {action.label}
            </a-button>
          ))}
        </a-space>
      )
    }
  }

  notification.error(config)
}
```

---

## 四、错误上报机制

### 4.1 错误日志收集（已实现）

```typescript
// src/utils/errorReporter.ts (已存在，需增强)
import axios from 'axios'
import type { ErrorInfo } from '@/types/error'

/**
 * 错误上报配置
 */
interface ErrorReporterConfig {
  enabled: boolean
  endpoint: string
  maxRetries: number
  retryDelay: number
  batchSize: number
  flushInterval: number
  // 新增配置
  includeUserAgent?: boolean
  includeUrl?: boolean
  includeLocalStorage?: boolean
}

/**
 * 错误队列
 */
let errorQueue: ErrorInfo[] = []
let flushTimer: ReturnType<typeof setInterval> | null = null

/**
 * 批量上报错误
 */
async function flushErrors() {
  if (errorQueue.length === 0) return

  const errors = [...errorQueue]
  errorQueue = []

  try {
    await axios.post('/api/log/error', {
      errors,
      reportedAt: new Date().toISOString(),
      environment: {
        url: window.location.href,
        userAgent: navigator.userAgent,
        viewport: {
          width: window.innerWidth,
          height: window.innerHeight
        },
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
        // 新增：用户信息
        userId: localStorage.getItem('userId'),
        tenantId: localStorage.getItem('tenantId'),
        // 新增：本地存储摘要
        localStorage: getLocalStorageSummary()
      }
    })

    console.log(`[ErrorReporter] Reported ${errors.length} errors`)
  } catch (err) {
    console.error('[ErrorReporter] Report failed', err)

    // 重试逻辑
    if (errors.length > 0) {
      errorQueue = [...errors, ...errorQueue]
    }
  }
}

/**
 * 获取本地存储摘要（脱敏）
 */
function getLocalStorageSummary(): Record<string, number> {
  const summary: Record<string, number> = {}

  try {
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i)
      if (key) {
        const value = localStorage.getItem(key)
        summary[key] = value?.length || 0
      }
    }
  } catch (err) {
    console.error('Failed to read localStorage:', err)
  }

  return summary
}

/**
 * 页面卸载时上报剩余错误
 */
window.addEventListener('beforeunload', () => {
  if (errorQueue.length === 0) return

  const data = JSON.stringify({
    errors: errorQueue,
    reportedAt: new Date().toISOString()
  })

  if (navigator.sendBeacon) {
    navigator.sendBeacon('/api/log/error', data)
    errorQueue = []
  }
})
```

### 4.2 错误数据分析

```typescript
// src/utils/errorAnalytics.ts
import type { ErrorInfo } from '@/types/error'

/**
 * 错误分析器
 */
export class ErrorAnalytics {
  private errors: ErrorInfo[] = []

  /**
   * 添加错误
   */
  addError(error: ErrorInfo) {
    this.errors.push(error)
  }

  /**
   * 按类型统计
   */
  getErrorStatsByType(): Record<string, number> {
    const stats: Record<string, number> = {}

    this.errors.forEach(error => {
      stats[error.type] = (stats[error.type] || 0) + 1
    })

    return stats
  }

  /**
   * 按级别统计
   */
  getErrorStatsByLevel(): Record<string, number> {
    const stats: Record<string, number> = {}

    this.errors.forEach(error => {
      stats[error.level] = (stats[error.level] || 0) + 1
    })

    return stats
  }

  /**
   * 获取高频错误
   */
  getTopErrors(limit = 10): Array<{ message: string; count: number }> {
    const messageCounts: Record<string, number> = {}

    this.errors.forEach(error => {
      messageCounts[error.message] = (messageCounts[error.message] || 0) + 1
    })

    return Object.entries(messageCounts)
      .map(([message, count]) => ({ message, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, limit)
  }

  /**
   * 获取错误趋势
   */
  getErrorTrend(hours = 24): Array<{ time: string; count: number }> {
    const now = Date.now()
    const trend: Record<string, number> = {}

    // 初始化时间槽
    for (let i = hours; i >= 0; i--) {
      const time = new Date(now - i * 3600000).toISOString().slice(0, 13)
      trend[time] = 0
    }

    // 统计每个时间槽的错误数量
    this.errors.forEach(error => {
      const errorTime = new Date(error.timestamp).toISOString().slice(0, 13)
      if (trend.hasOwnProperty(errorTime)) {
        trend[errorTime]++
      }
    })

    return Object.entries(trend).map(([time, count]) => ({ time, count }))
  }

  /**
   * 生成报告
   */
  generateReport() {
    return {
      totalErrors: this.errors.length,
      byType: this.getErrorStatsByType(),
      byLevel: this.getErrorStatsByLevel(),
      topErrors: this.getTopErrors(),
      trend: this.getErrorTrend()
    }
  }
}

export const errorAnalytics = new ErrorAnalytics()
```

---

## 五、使用示例

### 5.1 基础使用

```typescript
import { errorManager } from '@/utils/errorManager'
import { ErrorType, ErrorLevel } from '@/types/error'

// 手动触发错误
errorManager.handleError({
  type: ErrorType.VALIDATION_ERROR,
  level: ErrorLevel.WARNING,
  message: '表单验证失败',
  detail: '用户名不能为空',
  timestamp: new Date().toISOString()
})
```

### 5.2 API 错误处理

```typescript
import { customerApi } from '@/api/customer'

try {
  await customerApi.create(customerData)
  message.success('创建成功')
} catch (error) {
  // Axios 拦截器已经处理了错误提示
  // 这里可以做额外的处理
  console.error('创建失败:', error)
}
```

### 5.3 组件错误处理

```vue
<script setup lang="ts">
import { ErrorBoundary } from '@/components/ErrorBoundary'
import { errorManager } from '@/utils/errorManager'

const handleError = (error: Error, instance: any, info: string) => {
  console.error('组件错误:', error)
  // 可以在这里记录日志或上报
}
</script>

<template>
  <ErrorBoundary :on-error="handleError">
    <YourComponent />
  </ErrorBoundary>
</template>
```

### 5.4 监听错误重试事件

```typescript
// 监听全局重试事件
window.addEventListener('error-retry', (event) => {
  const errorInfo = event.detail as ErrorInfo
  console.log('重试:', errorInfo)

  // 根据错误类型执行重试逻辑
  if (errorInfo.type === ErrorType.TIMEOUT) {
    // 重新发起请求
  }
})
```

---

## 六、最佳实践

### 6.1 错误处理原则

1. **不吞噬错误**: 捕获错误后必须处理（上报/提示用户）
2. **不重复提示**: 避免同一错误多次提示用户
3. **分级处理**: 根据错误严重程度选择合适的提示方式
4. **可操作**: 为可恢复的错误提供重试或恢复方案
5. **记录日志**: 所有错误都应记录到日志系统

### 6.2 错误提示建议

| 错误类型 | 提示方式 | 持续时间 | 操作建议 |
|----------|----------|----------|----------|
| 网络错误 | Toast | 3s | 重试 |
| 请求超时 | Notification | 持久 | 重试 |
| 4xx 错误 | Toast | 5s | 无 |
| 5xx 错误 | Modal | 持久 | 联系管理员 |
| 验证错误 | 表单提示 | - | 修正表单 |
| 权限错误 | Toast + 跳转 | 3s | 登录 |

### 6.3 错误上报策略

1. **生产环境**: 全部上报
2. **开发环境**: 仅控制台输出
3. **批量上报**: 避免频繁请求，使用批量队列
4. **关键错误**: 立即上报
5. **脱敏处理**: 避免上报敏感信息

---

## 七、后续优化建议

1. **错误自动分类**: 使用 AI 模型自动识别错误类型
2. **错误聚合**: 将相同或相似错误聚合显示
3. **错误预警**: 当某类错误超过阈值时发送预警
4. **错误恢复**: 自动恢复某些类型的错误
5. **用户反馈**: 允许用户提交错误反馈
6. **错误可视化**: 使用图表展示错误趋势和分布

---

## 附录：完整类型定义

```typescript
// src/types/error.ts
export enum ErrorType {
  NETWORK = 'NETWORK',
  TIMEOUT = 'TIMEOUT',
  OFFLINE = 'OFFLINE',
  HTTP_ERROR = 'HTTP_ERROR',
  HTTP_400 = 'HTTP_400',
  HTTP_401 = 'HTTP_401',
  HTTP_403 = 'HTTP_403',
  HTTP_404 = 'HTTP_404',
  HTTP_500 = 'HTTP_500',
  HTTP_502 = 'HTTP_502',
  HTTP_503 = 'HTTP_503',
  BUSINESS_ERROR = 'BUSINESS_ERROR',
  VALIDATION_ERROR = 'VALIDATION_ERROR',
  AUTH_ERROR = 'AUTH_ERROR',
  DUPLICATE_ERROR = 'DUPLICATE_ERROR',
  COMPONENT_ERROR = 'COMPONENT_ERROR',
  RUNTIME_ERROR = 'RUNTIME_ERROR',
  REFERENCE_ERROR = 'REFERENCE_ERROR',
  TYPE_ERROR = 'TYPE_ERROR',
  SYNTAX_ERROR = 'SYNTAX_ERROR',
  PROMISE_ERROR = 'PROMISE_ERROR',
  RESOURCE_ERROR = 'RESOURCE_ERROR',
  UNKNOWN = 'UNKNOWN'
}

export enum ErrorLevel {
  INFO = 'INFO',
  WARNING = 'WARNING',
  ERROR = 'ERROR',
  FATAL = 'FATAL'
}

export interface ErrorInfo {
  type: ErrorType
  level: ErrorLevel
  code?: string
  message: string
  detail?: string
  timestamp: string
  stack?: string
  url?: string
  userAgent?: string
  userId?: string
  extra?: Record<string, any>
}
```

---

**文档结束**