/**
 * @ai-ready/api-client
 * 共享 API 客户端 — 统一的 Axios 封装
 */
import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig, AxiosResponse } from 'axios'

// ── 类型定义 ────────────────────────────────────────────

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResponse<T = any> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export interface ApiClientOptions {
  baseURL: string
  timeout?: number
  getToken?: () => string | null
  onUnauthorized?: () => void
  onError?: (error: any) => void
}

// ── 创建 API 客户端 ─────────────────────────────────────

export function createApiClient(options: ApiClientOptions): AxiosInstance {
  const {
    baseURL,
    timeout = 30000,
    getToken,
    onUnauthorized,
    onError
  } = options

  const instance = axios.create({
    baseURL,
    timeout,
    headers: { 'Content-Type': 'application/json' }
  })

  // 请求拦截器
  instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
    const token = getToken?.()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })

  // 响应拦截器
  instance.interceptors.response.use(
    (response: AxiosResponse<ApiResponse>) => {
      const { code, message: msg, data } = response.data
      if (code === 200) return data as any
      if (code === 401) {
        onUnauthorized?.()
        return Promise.reject(new Error('登录已过期'))
      }
      return Promise.reject(new Error(msg || '请求失败'))
    },
    (error) => {
      onError?.(error)
      return Promise.reject(error)
    }
  )

  return instance
}

export default createApiClient
