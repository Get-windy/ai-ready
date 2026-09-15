import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

// 类型定义
export interface ApiResponse<T = any> {
  success: boolean
  code: number
  message: string
  data: T
  timestamp: number
}

export interface RequestOptions {
  headers?: Record<string, string>
  params?: Record<string, any>
  data?: any
  responseType?: 'json' | 'blob' | 'text'
}

// 创建 Axios 实例
const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // FormData 上传（签收照片/手写签名、补能凭证等）：必须移除全局默认的 application/json，
    // 交由浏览器写入 multipart/form-data 与 boundary；否则后端按非 multipart 解析，
    // 报 “Current request is not a multipart request”（与 pc-admin 同一处坑）
    if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
      const headers: any = config.headers
      if (typeof headers.delete === 'function') {
        headers.delete('Content-Type')
      } else {
        delete headers['Content-Type']
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    if (response.config.responseType === 'blob') {
      return response.data as any
    }

    const { code, message: msg, data } = response.data

    if (code === 200) {
      return response.data as any
    }

    if (code === 401) {
      localStorage.removeItem('token')
      // 可选：跳转登录页
      return Promise.reject(new Error(msg || '登录已过期'))
    }

    return Promise.reject(new Error(msg || '请求失败'))
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
    }
    return Promise.reject(error)
  }
)

// 封装请求方法
export const request = {
  get<T = any>(url: string, params?: any, options?: RequestOptions): Promise<ApiResponse<T>> {
    const config: AxiosRequestConfig = { params }
    if (options?.headers) config.headers = options.headers as any
    if (options?.responseType) config.responseType = options.responseType
    return service.get(url, config)
  },

  post<T = any>(url: string, data?: any, options?: RequestOptions): Promise<ApiResponse<T>> {
    const config: AxiosRequestConfig = {}
    if (options?.headers) config.headers = options.headers as any
    if (options?.params) config.params = options.params
    if (options?.responseType) config.responseType = options.responseType
    return service.post(url, data, config)
  },

  put<T = any>(url: string, data?: any, options?: RequestOptions): Promise<ApiResponse<T>> {
    const config: AxiosRequestConfig = {}
    if (options?.headers) config.headers = options.headers as any
    if (options?.params) config.params = options.params
    return service.put(url, data, config)
  },

  delete<T = any>(url: string, options?: RequestOptions): Promise<ApiResponse<T>> {
    const config: AxiosRequestConfig = {}
    if (options?.headers) config.headers = options.headers as any
    if (options?.params) config.params = options.params
    if (options?.data !== undefined) config.data = options.data
    return service.delete(url, config)
  }
}

export default request