import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig, AxiosRequestHeaders, ResponseType } from 'axios'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'
import { refreshTokenAndRetry, getToken, isTokenExpired, clearTokenVerifyCache } from './tokenRefresher'
import { trackApiCall, addSentryBreadcrumb } from './performanceMonitor'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

NProgress.configure({ showSpinner: false, minimum: 0.1 })

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

/**
 * 请求选项（调用方可传入的配置）
 * 同时支持重试配置 + Axios 原生配置
 */
export interface RequestOptions extends RetryConfig {
  /** 跳过 Token 刷新逻辑（登录/登出/验证码等场景） */
  _skipAuthRefresh?: boolean
  /** 自定义请求头 */
  headers?: Record<string, string>
  /** 响应类型（如 'blob' 用于文件下载） */
  responseType?: ResponseType
  /** URL 查询参数（仅用于 post/put/patch/delete 的 URL 参数） */
  params?: Record<string, any>
  /** 请求体（用于 delete 方法） */
  data?: any
}

/** 扩展的 Axios 请求配置 */
interface ExtendedAxiosRequestConfig extends Partial<InternalAxiosRequestConfig> {
  retryConfig?: RetryConfig
  _retryCount?: number
  _skipAuthRefresh?: boolean
  _requestStartTime?: number
}

// ── 默认重试配置 ────────────────────────────────────────

export const defaultRetryConfig: Required<Omit<RetryConfig, 'retry'>> = {
  maxRetries: 2,
  retryDelay: 800,
  shouldRetry: (config: InternalAxiosRequestConfig) => {
    const method = config.method?.toUpperCase() || ''
    return method === 'GET'
  },
  retryCondition: (error: any) => {
    if (!error.response) return true
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

const R = '[DEBUG:req]'

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    const url = `${config.baseURL || ''}${config.url || ''}`
    const skipRefresh = !!(config as ExtendedAxiosRequestConfig)._skipAuthRefresh

    console.log(`${R} ➡️ ${config.method?.toUpperCase() || 'GET'} ${url}`, { hasToken: !!token, skipRefresh })

    // 检查 Token 是否即将过期，若是则尝试刷新
    if (token && isTokenExpired(token) && !skipRefresh) {
      console.log(`${R} Token已过期, 尝试刷新: ${url}`)
      return refreshTokenAndRetry((newToken) => {
        config.headers.Authorization = `Bearer ${newToken}`
        return config
      })
    }

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // 请求开始：启动进度条
    NProgress.start()

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
    const url = `${response.config.baseURL || ''}${response.config.url || ''}`
    console.log(`${R} ⬅️ ${response.config.method?.toUpperCase() || 'GET'} ${url} → ${response.status}`)

    // 请求完成：结束进度条
    NProgress.done()

    // 记录 API 调用耗时
    const startTime = (response.config as any)._requestStartTime
    if (startTime) {
      const duration = Date.now() - startTime
      const url = response.config.url || ''
      const method = response.config.method?.toUpperCase() || 'GET'
      const status = response.status
      trackApiCall(url, method, duration, status)
      if (duration > 2000) {
        console.warn(`[API] 慢请求: ${method} ${url} — ${duration}ms`)
      }
    }

    const resData = response.data

    // ── 处理无 wrapper 的原始响应 ──────────────────────
    // 后端某些 Controller（如 StockController）直接返回 Page<T> / List<T>
    // 这些响应没有 {code, message, data} 包装，直接透传
    if (resData && typeof resData === 'object' && !('code' in resData)) {
      console.log(`${R} ⚠️ 检测到无 wrapper 响应, 直接透传`)
      return resData
    }

    // ── 标准 wrapper 响应处理 ──────────────────────────
    const { code, message: msg, data } = response.data

    if (code === 200) {
      return response.data as any
    }

    // 业务错误：401 → Token 刷新
    if (code === 401) {
      const config = response.config as ExtendedAxiosRequestConfig
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
    const url = `${config?.baseURL || ''}${config?.url || ''}`
    console.log(`${R} ❌ ${config?.method?.toUpperCase() || '?'} ${url} → status=${error.response?.status || '网络错误'}`, error.message)

    // 请求失败：结束进度条
    NProgress.done()

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
      const maxRetries = userConfig.retry ?? userConfig.maxRetries ?? defaultRetryConfig.maxRetries
      const retryDelay = userConfig.retryDelay ?? defaultRetryConfig.retryDelay
      const shouldRetry = userConfig.shouldRetry ?? defaultRetryConfig.shouldRetry
      const retryCondition = userConfig.retryCondition ?? defaultRetryConfig.retryCondition

      const isRetryAllowed = shouldRetry(config)
      const matchesRetryCondition = retryCondition(error)
      const retryCount = config._retryCount || 0

      if (isRetryAllowed && matchesRetryCondition && retryCount < maxRetries) {
        config._retryCount = retryCount + 1
        const delay = retryDelay * Math.pow(2, retryCount) + Math.random() * 1000
        console.warn(`[API] 重试 ${config._retryCount}/${maxRetries}: ${config.method?.toUpperCase() || 'GET'} ${config.url} — ${Math.round(delay)}ms 后重试`)
        await new Promise((resolve) => setTimeout(resolve, delay))
        return service(config)
      }
    }

    // 处理 HTTP 错误
    if (error.response) {
      const { status } = error.response
      switch (status) {
        case 401:
          if (!config?._skipAuthRefresh) {
            try {
              return await refreshTokenAndRetry((newToken) => {
                config.headers.Authorization = `Bearer ${newToken}`
                config._skipAuthRefresh = true
                return service(config)
              })
            } catch {
              // refreshTokenAndRetry 内部已处理跳转，此处作为安全保障
              handleUnauthorized()
              return Promise.reject(error)
            }
          }
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

/** 清除登录状态并跳转登录页 */
export function handleUnauthorized() {
  console.log(`${R} 🔴 handleUnauthorized() - 清除登录状态并跳转登录页`)
  clearTokenVerifyCache() // 清除 token 验证缓存，防止使用过期缓存
  try {
    const userStore = useUserStore()
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

// ── 工具：从 RequestOptions 构建 ExtendedAxiosRequestConfig ──

function buildConfig(options?: RequestOptions): ExtendedAxiosRequestConfig {
  const config: ExtendedAxiosRequestConfig = {}
  if (!options) return config

  // 提取重试配置
  if (options.retry !== undefined || options.maxRetries !== undefined ||
      options.retryDelay !== undefined || options.shouldRetry !== undefined ||
      options.retryCondition !== undefined) {
    config.retryConfig = {
      retry: options.retry,
      maxRetries: options.maxRetries,
      retryDelay: options.retryDelay,
      shouldRetry: options.shouldRetry,
      retryCondition: options.retryCondition,
    }
  }

  // 提取 Axios 原生配置
  if (options.headers) config.headers = options.headers as AxiosRequestHeaders
  if (options.responseType) config.responseType = options.responseType
  if (options.params) config.params = options.params
  if (options.data !== undefined) config.data = options.data
  if (options._skipAuthRefresh) config._skipAuthRefresh = true

  return config
}

// ── 封装请求方法 ────────────────────────────────────────

export const request = {
  /**
   * GET 请求
   * 兼容两种调用方式：
   *   request.get(url, params)              — 仅传查询参数
   *   request.get(url, params, options)     — 传查询参数 + 选项（含 _skipAuthRefresh, headers 等）
   *   request.get(url, options)             — 选项作为第二个参数（无查询参数但有选项）
   */
  get<T = any>(url: string, paramsOrOptions?: any, options?: RequestOptions): Promise<any> {
    // 判断 paramsOrOptions 是 params 对象还是 RequestOptions
    let params: any
    let opts: RequestOptions | undefined

    if (paramsOrOptions !== undefined) {
      const po = paramsOrOptions as any
      if (po._skipAuthRefresh !== undefined || po.headers !== undefined ||
          po.responseType !== undefined || po.retry !== undefined ||
          po.maxRetries !== undefined || po.shouldRetry !== undefined) {
        // 这是 options
        opts = paramsOrOptions as RequestOptions
      } else {
        // 这是 params
        params = paramsOrOptions
        opts = options
      }
    } else {
      opts = options
    }

    const config = buildConfig(opts)
    // 如果传入 params，且 config 中还没有 params（options 里可能也带了 params）
    if (params && !opts?.params) {
      config.params = params
    }
    if (opts?._skipAuthRefresh) config._skipAuthRefresh = true

    return service.get(url, config)
  },

  post<T = any>(url: string, data?: any, options?: RequestOptions): Promise<any> {
    const config = buildConfig(options)
    if (options?._skipAuthRefresh) config._skipAuthRefresh = true
    return service.post(url, data, config)
  },

  put<T = any>(url: string, data?: any, options?: RequestOptions): Promise<any> {
    const config = buildConfig(options)
    if (options?._skipAuthRefresh) config._skipAuthRefresh = true
    return service.put(url, data, config)
  },

  delete<T = any>(url: string, options?: RequestOptions): Promise<any> {
    const config = buildConfig(options)
    if (options?._skipAuthRefresh) config._skipAuthRefresh = true
    return service.delete(url, config)
  },

  /** PATCH 方法 */
  patch<T = any>(url: string, data?: any, options?: RequestOptions): Promise<any> {
    const config = buildConfig(options)
    if (options?._skipAuthRefresh) config._skipAuthRefresh = true
    return service.patch(url, data, config)
  }
}

export default request
