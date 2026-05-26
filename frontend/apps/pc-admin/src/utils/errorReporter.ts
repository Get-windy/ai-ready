import axios from 'axios'
import dayjs from 'dayjs'

// 错误类型定义
export interface ErrorReport {
  type: 'component' | 'global' | 'promise' | 'resource' | 'ajax'
  error: Error | string
  componentInfo?: string
  timestamp: string
  componentInstance?: string
  url?: string
  userAgent?: string
  stack?: string
  extra?: Record<string, unknown>
}

// 错误上报配置
interface ErrorReporterConfig {
  enabled: boolean
  endpoint: string
  maxRetries: number
  retryDelay: number
  batchSize: number
  flushInterval: number
}

const defaultConfig: ErrorReporterConfig = {
  enabled: true,
  endpoint: '/api/error-report',
  maxRetries: 3,
  retryDelay: 1000,
  batchSize: 10,
  flushInterval: 5000
}

// 错误队列
let errorQueue: ErrorReport[] = []
let flushTimer: ReturnType<typeof setInterval> | null = null

/**
 * 初始化错误上报器
 */
export function initErrorReporter(app: {
  config: {
    errorHandler?: (err: unknown, instance: unknown, info: string) => void
    globalProperties?: Record<string, unknown>
  }
}, config?: Partial<ErrorReporterConfig>) {
  const finalConfig = { ...defaultConfig, ...config }
  
  if (!finalConfig.enabled) {
    console.log('[ErrorReporter] Disabled')
    return
  }
  
  // 设置全局错误处理器
  app.config.errorHandler = (err: unknown, instance: unknown, info: string) => {
    const error = err as Error
    
    console.error('[Global Error]', error, info)
    
    reportError({
      type: 'global',
      error,
      componentInfo: info,
      timestamp: new Date().toISOString(),
      componentInstance: getComponentName(instance),
      url: window.location.href,
      userAgent: navigator.userAgent,
      stack: error.stack
    })
  }
  
  // 捕获未处理的 Promise 错误
  window.addEventListener('unhandledrejection', (event) => {
    console.error('[Unhandled Promise]', event.reason)
    
    reportError({
      type: 'promise',
      error: event.reason instanceof Error 
        ? event.reason 
        : new Error(String(event.reason)),
      timestamp: new Date().toISOString(),
      url: window.location.href,
      userAgent: navigator.userAgent,
      stack: event.reason instanceof Error ? event.reason.stack : undefined
    })
  })
  
  // 捕获资源加载错误
  window.addEventListener('error', (event) => {
    if (event.target !== window) {
      const target = event.target as HTMLElement
      
      console.error('[Resource Error]', target.src || target.href)
      
      reportError({
        type: 'resource',
        error: `Resource load failed: ${target.src || target.href}`,
        timestamp: new Date().toISOString(),
        url: window.location.href,
        userAgent: navigator.userAgent,
        extra: {
          tagName: target.tagName,
          src: target.src,
          href: target.href
        }
      })
    }
  }, true)
  
  // 启动定时上报
  startFlushTimer(finalConfig)
  
  console.log('[ErrorReporter] Initialized')
}

/**
 * 获取组件名称
 */
function getComponentName(instance: unknown): string {
  if (!instance) return 'Unknown'
  
  const vueInstance = instance as { $options?: { name?: string }; constructor?: { name?: string } }
  
  return vueInstance.$options?.name 
    || vueInstance.constructor?.name 
    || 'Unknown'
}

/**
 * 上报单个错误
 */
export function reportError(report: ErrorReport) {
  // 添加到队列
  errorQueue.push({
    ...report,
    timestamp: report.timestamp || new Date().toISOString(),
    url: report.url || window.location.href,
    userAgent: report.userAgent || navigator.userAgent,
    stack: report.stack || (report.error instanceof Error ? report.error.stack : undefined)
  })
  
  // 如果队列达到批量大小，立即上报
  if (errorQueue.length >= defaultConfig.batchSize) {
    flushErrors()
  }
}

/**
 * 批量上报错误
 */
async function flushErrors() {
  if (errorQueue.length === 0) return
  
  const errors = [...errorQueue]
  errorQueue = []
  
  try {
    await axios.post(defaultConfig.endpoint, {
      errors,
      reportedAt: new Date().toISOString(),
      environment: {
        url: window.location.href,
        userAgent: navigator.userAgent,
        viewport: {
          width: window.innerWidth,
          height: window.innerHeight
        },
        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone
      }
    })
    
    console.log(`[ErrorReporter] Reported ${errors.length} errors`)
  } catch (err) {
    console.error('[ErrorReporter] Report failed', err)
    
    // 重试逻辑 - 将失败的错误重新放入队列
    if (errors.length > 0) {
      errorQueue = [...errors, ...errorQueue]
    }
  }
}

/**
 * 启动定时上报
 */
function startFlushTimer(config: ErrorReporterConfig) {
  if (flushTimer) {
    clearInterval(flushTimer)
  }
  
  flushTimer = setInterval(() => {
    flushErrors()
  }, config.flushInterval)
}

/**
 * 手动上报（用于页面卸载前）
 */
export function flushErrorsSync() {
  if (errorQueue.length === 0) return
  
  // 使用 sendBeacon 确保页面关闭时也能上报
  const data = JSON.stringify({
    errors: errorQueue,
    reportedAt: new Date().toISOString()
  })
  
  if (navigator.sendBeacon) {
    navigator.sendBeacon(defaultConfig.endpoint, data)
    errorQueue = []
  }
}

/**
 * 格式化错误信息用于显示
 */
export function formatError(error: Error | unknown): string {
  if (error instanceof Error) {
    return `[${error.name}] ${error.message}`
  }
  
  if (typeof error === 'string') {
    return error
  }
  
  return JSON.stringify(error)
}

/**
 * 获取错误堆栈摘要
 */
export function getStackTraceSnippet(error: Error, maxLines = 10): string {
  if (!error.stack) return ''
  
  const lines = error.stack.split('\n').slice(0, maxLines)
  return lines.join('\n')
}

/**
 * 创建错误分类
 */
export function categorizeError(error: Error): string {
  const message = error.message.toLowerCase()
  
  if (message.includes('network') || message.includes('fetch')) {
    return 'network'
  }
  
  if (message.includes('timeout')) {
    return 'timeout'
  }
  
  if (message.includes('auth') || message.includes('unauthorized')) {
    return 'auth'
  }
  
  if (message.includes('type') || message.includes('undefined') || message.includes('null')) {
    return 'type'
  }
  
  if (message.includes('syntax') || message.includes('parse')) {
    return 'syntax'
  }
  
  return 'unknown'
}

// 页面卸载时上报剩余错误
window.addEventListener('beforeunload', () => {
  flushErrorsSync()
})

export default {
  initErrorReporter,
  reportError,
  flushErrorsSync,
  formatError,
  getStackTraceSnippet,
  categorizeError
}