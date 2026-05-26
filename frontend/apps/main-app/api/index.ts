/**
 * API请求实例
 */

import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import type { IApiResponse } from '@/types/api'
import { apiConfig, createRequestConfig } from './config'

// 创建Axios实例
const createAxiosInstance = (config?: AxiosRequestConfig): AxiosInstance => {
  const instance = axios.create({
    ...apiConfig,
    ...config
  })
  
  // 请求拦截器
  instance.interceptors.request.use(
    (config) => {
      // 添加认证令牌
      const token = localStorage.getItem('access_token')
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
      
      // 添加请求ID用于追踪
      config.headers['X-Request-ID'] = generateRequestId()
      
      // 开发环境日志
      if (import.meta.env.VITE_FEATURE_DEBUG === 'true') {
        console.log(`[API Request] ${config.method?.toUpperCase()} ${config.url}`, config)
      }
      
      return config
    },
    (error) => {
      console.error('[API Request Error]', error)
      return Promise.reject(error)
    }
  )
  
  // 响应拦截器
  instance.interceptors.response.use(
    (response: AxiosResponse<IApiResponse>) => {
      // 开发环境日志
      if (import.meta.env.VITE_FEATURE_DEBUG === 'true') {
        console.log(`[API Response] ${response.config.method?.toUpperCase()} ${response.config.url}`, response)
      }
      
      // 处理响应数据格式
      const { data } = response
      
      // 如果响应有错误码，抛出错误
      if (data.code !== 0 && data.code !== 200) {
        const error = new Error(data.message || '请求失败')
        ;(error as any).code = data.code
        ;(error as any).data = data.data
        return Promise.reject(error)
      }
      
      return response
    },
    (error) => {
      // 统一错误处理
      if (error.response) {
        // 服务器响应错误
        const { status, data } = error.response
        
        switch (status) {
          case 401:
            // 未授权，跳转到登录页
            console.error('[API Error 401] 未授权，请重新登录')
            localStorage.removeItem('access_token')
            window.location.href = '/login'
            break
            
          case 403:
            console.error('[API Error 403] 权限不足')
            break
            
          case 404:
            console.error('[API Error 404] 资源不存在')
            break
            
          case 500:
            console.error('[API Error 500] 服务器内部错误')
            break
            
          default:
            console.error(`[API Error ${status}]`, data?.message || '请求失败')
        }
      } else if (error.request) {
        // 请求发送失败（网络错误）
        console.error('[API Network Error] 网络连接失败，请检查网络设置')
      } else {
        // 请求配置错误
        console.error('[API Config Error]', error.message)
      }
      
      return Promise.reject(error)
    }
  )
  
  return instance
}

// 生成请求ID
function generateRequestId(): string {
  return `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

// 创建不同用途的实例
export const api = createAxiosInstance()

// 管理端API实例
export const adminApi = createAxiosInstance({
  baseURL: import.meta.env.VITE_API_ADMIN_URL || '/api/admin'
})

// 移动端API实例
export const mobileApi = createAxiosInstance({
  baseURL: import.meta.env.VITE_API_MOBILE_URL || '/api/mobile'
})

// 文件上传实例
export const uploadApi = createAxiosInstance({
  baseURL: apiConfig.baseURL,
  headers: {
    'Content-Type': 'multipart/form-data'
  },
  timeout: 60000 // 文件上传超时时间更长
})

// API工具函数
export class ApiUtils {
  // 获取完整的URL
  static getFullUrl(path: string, base?: string): string {
    const baseURL = base || apiConfig.baseURL
    return `${baseURL}${path}`
  }
  
  // 构建查询参数
  static buildQueryParams(params: Record<string, any>): string {
    const searchParams = new URLSearchParams()
    
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        if (Array.isArray(value)) {
          value.forEach(item => searchParams.append(`${key}[]`, item))
        } else {
          searchParams.append(key, String(value))
        }
      }
    })
    
    return searchParams.toString()
  }
  
  // 构建URL
  static buildUrl(path: string, params?: Record<string, any>): string {
    let url = path
    
    if (params) {
      const queryString = this.buildQueryParams(params)
      if (queryString) {
        url += `?${queryString}`
      }
    }
    
    return url
  }
  
  // 延迟请求（用于测试）
  static delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms))
  }
  
  // 重试请求
  static async retry<T>(
    fn: () => Promise<T>,
    maxRetries: number = 3,
    delayMs: number = 1000
  ): Promise<T> {
    let lastError: Error
    for (let i = 0; i < maxRetries; i++) {
      try {
        return await fn()
      } catch (error) {
        lastError = error as Error
        console.warn(`[API Retry ${i + 1}/${maxRetries}]`, error)
        
        if (i < maxRetries - 1) {
          await this.delay(delayMs * (i + 1))
        }
      }
    }
    throw lastError!
  }
}

// 导出常用的请求方法
export const request = {
  get: <T = any>(url: string, config?: AxiosRequestConfig) => 
    api.get<IApiResponse<T>>(url, config).then(res => res.data.data),
  
  post: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) =>
    api.post<IApiResponse<T>>(url, data, config).then(res => res.data.data),
  
  put: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) =>
    api.put<IApiResponse<T>>(url, data, config).then(res => res.data.data),
  
  delete: <T = any>(url: string, config?: AxiosRequestConfig) =>
    api.delete<IApiResponse<T>>(url, config).then(res => res.data.data),
  
  patch: <T = any>(url: string, data?: any, config?: AxiosRequestConfig) =>
    api.patch<IApiResponse<T>>(url, data, config).then(res => res.data.data)
}

export default api