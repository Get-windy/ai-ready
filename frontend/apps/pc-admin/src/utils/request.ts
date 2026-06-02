import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'
import { refreshTokenAndRetry, getToken, isTokenExpired } from './tokenRefresher'
import { trackApiCall, addSentryBreadcrumb } from './performanceMonitor'

// ── 类型定义 ────────────────────────────────────────────

/** 统一响应结构 */
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

/** 分页响应 */
export interface PageResponse<T = any> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

/** 重试配置 */
export interface RetryConfig {
  /** 最大重试次数（快捷别名），默认 3 */
  retry?: number
  /** 最大重试次数，默认 3 */
  maxRetries?: number
  /** 基础延迟（毫秒），默认 1000 */
  retryDelay?: number
  /** 判断是否应重试该请求（默认只重试 GET） */
  shouldRetry?: (config: InternalAxiosRequestConfig) => boolean
  /** 自定义重试条件（默认：5xx 和网络错误） */
  retryCondition?: (error: any) => boolean
}

/** 扩展的 Axios 请求配置（支持重试和跳过刷新） */
interface ExtendedAxiosRequestConfig extends Partial<InternalAxiosRequestConfig> {
  retryConfig?: RetryConfig
  _retryCount?: number
  _skipAuthRefresh?: boolean
}

// ── 默认重试配置 ────────────────────────────────────────

export const defaultRetryConfig: Required<Omit<RetryConfig, 'retry'>> = {
  maxRetries: 3,
  retryDelay: 1000,
  shouldRetry: (config: InternalAxiosRequestConfig) => {
    // 默认只重试 GET 请求，POST/PUT/DELETE 不重试（幂等性考虑）
    const method = config.method?.toUpperCase() || ''
    return method === 'GET'
  },
  retryCondition: (error: any) => {
    // 仅对 5xx 和网络错误重试
    if (!error.response) return true // 网络错误
    const status = error.response.status
    return status >= 500 && status < 600
  }
}

// ── 创建 Axios 实例 ─────────────────────────────────────

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// ── 请求拦截器 ──────────────────────────────────────────

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()

    // 检查 Token 是否即将过期，若是则尝试刷新
    if (token && isTokenExpired(token) && !(config as ExtendedAxiosRequestConfig)._skipAuthRefresh) {
      // Token 已过期，尝试主动刷新
      return refreshTokenAndRetry((newToken) => {
        config.headers.Authorization = `Bearer ${newToken}`
        return config
      })
    }

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 添加请求时间戳用于性能监控
    ;(config as any)._requestStartTime = Date.now()

    // Sentry 面包屑：记录 API 请求开始
    addSentryBreadcrumb(
      `API ${config.method?.toUpperCase() || 'GET'} ${config.url || ''}`,
      {
        method: config.method?.toUpperCase(),
        url: config.url,
        baseURL: config.baseURL,
      },
      'api'
    )

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// ── 响应拦截器 ──────────────────────────────────────────

service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    // 记录 API 调用耗时
    const startTime = (response.config as any)._requestStartTime
    if (startTime) {
      const duration = Date.now() - startTime
      const url = response.config.url || ''
      const method = response.config.method?.toUpperCase() || 'GET'
      const status = response.status

      // Sentry 性能追踪
      trackApiCall(url, method, duration, status)

      if (duration > 2000) {
        console.warn(`[API] 慢请求: ${method} ${url} — ${duration}ms`)
      }
    }

    const { code, message: msg, data } = response.data

    if (code === 200) {
      return response.data as any
    }

    // 业务错误：401 → Token 刷新
    if (code === 401) {
      const config = response.config as ExtendedAxiosRequestConfig
      // 如果是登录请求失败，直接显示后端返回的错误消息
      if (config.url?.includes('/auth/login')) {
        message.error(msg || '登录失败')
        return Promise.reject(new Error(msg || '登录失败'))
      }
      if (!config._skipAuthRefresh) {
        return refreshTokenAndRetry((newToken) => {
          config.headers.Authorization = `Bearer ${newToken}`
          config._skipAuthRefresh = true
          return service(config)
        })
      }
      handleUnauthorized()
      return Promise.reject(new Error('登录已过期'))
    }

    // 403 → 权限不足（不自动登出）
    if (code === 403) {
      message.error(msg || '没有操作权限')
      return Promise.reject(new Error(msg || '权限不足'))
    }

    message.error(msg || '请求失败')
    return Promise.reject(new Error(msg))
  },
  async (error) => {
    const config = error.config as ExtendedAxiosRequestConfig

    // 记录失败的 API 调用耗时
    if (config) {
      const startTime = (config as any)._requestStartTime
      if (startTime) {
        const duration = Date.now() - startTime
        const url = config.url || ''
        const method = config.method?.toUpperCase() || 'GET'
        const status = error.response?.status || 0

        trackApiCall(url, method, duration, status)
      }
    }

    // 网络错误或服务端错误时尝试重试
    if (config && !config._skipAuthRefresh) {
      const userConfig = config.retryConfig || {}
      // 合并重试配置：取 defaultRetryConfig，用用户配置覆盖
      const maxRetries = userConfig.retry ?? userConfig.maxRetries ?? defaultRetryConfig.maxRetries
      const retryDelay = userConfig.retryDelay ?? defaultRetryConfig.retryDelay
      const shouldRetry = userConfig.shouldRetry ?? defaultRetryConfig.shouldRetry
      const retryCondition = userConfig.retryCondition ?? defaultRetryConfig.retryCondition

      const isRetryAllowed = shouldRetry(config)
      const matchesRetryCondition = retryCondition(error)
      const retryCount = config._retryCount || 0

      if (isRetryAllowed && matchesRetryCondition && retryCount < maxRetries) {
        config._retryCount = retryCount + 1

        // 指数退避 + 随机抖动: delay = retryDelay * (2 ^ retryCount) + random jitter
        const delay = retryDelay * Math.pow(2, retryCount) + Math.random() * 1000

        const method = config.method?.toUpperCase() || 'GET'
        console.warn(
          `[API] 重试 ${config._retryCount}/${maxRetries}: ${method} ${config.url} — ${Math.round(delay)}ms 后重试`
        )

        await new Promise((resolve) => setTimeout(resolve, delay))
        return service(config)
      }
    }

    // 处理 HTTP 错误
    if (error.response) {
      const { status } = error.response

      switch (status) {
        case 401:
          // 401: 尝试刷新 Token
          if (!config?._skipAuthRefresh) {
            try {
              return await refreshTokenAndRetry((newToken) => {
                config.headers.Authorization = `Bearer ${newToken}`
                config._skipAuthRefresh = true
                return service(config)
              })
            } catch {
              // 刷新失败，已在 refreshTokenAndRetry 中处理登出
              return Promise.reject(error)
            }
          }
          message.error('登录已过期，请重新登录')
          handleUnauthorized()
          break
        case 403:
          message.error('拒绝访问')
          break
        case 404:
          message.error('请求资源不存在')
          break
        case 500:
          message.error('服务器内部错误')
          break
        default:
          message.error(`请求错误 (${status})`)
      }
    } else if (error.code === 'ECONNABORTED') {
      message.error('请求超时，请稍后重试')
    } else {
      message.error('网络连接异常，请检查网络')
    }

    return Promise.reject(error)
  }
)

// ── 未授权处理 ──────────────────────────────────────────

/** 清除登录状态并跳转登录页。供外部模块（如路由守卫、token 刷新模块）复用。 */
export function handleUnauthorized() {
  try {
    const userStore = useUserStore()
    // 直接清除状态，不调用logout API（避免无限循环）
    userStore.token = ''
    userStore.userId = 0
    userStore.tenantId = 1
    userStore.userInfo = null
    userStore.permissions = []
    userStore.roles = []
    userStore.menus = []
  } catch (e) {
    console.error('清除用户状态失败:', e)
  }
  localStorage.removeItem('token')
  localStorage.removeItem('tenantId')
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

// ── 封装请求方法（支持重试配置） ────────────────────────

export const request = {
  get<T = any>(url: string, params?: object, retryConfig?: RetryConfig): Promise<ApiResponse<T>> {
    const config: ExtendedAxiosRequestConfig = { params }
    if (retryConfig) {
      config.retryConfig = retryConfig
    }
    return service.get(url, config)
  },

  post<T = any>(url: string, data?: object, retryConfig?: RetryConfig): Promise<ApiResponse<T>> {
    const config: ExtendedAxiosRequestConfig = {}
    // POST 默认不重试，除非显式配置
    if (retryConfig) {
      config.retryConfig = retryConfig
    }
    return service.post(url, data, config)
  },

  put<T = any>(url: string, data?: object, retryConfig?: RetryConfig): Promise<ApiResponse<T>> {
    const config: ExtendedAxiosRequestConfig = {}
    if (retryConfig) {
      config.retryConfig = retryConfig
    }
    return service.put(url, data, config)
  },

  delete<T = any>(url: string, params?: object, retryConfig?: RetryConfig): Promise<ApiResponse<T>> {
    const config: ExtendedAxiosRequestConfig = { params }
    if (retryConfig) {
      config.retryConfig = retryConfig
    }
    return service.delete(url, config)
  }
}

export default service