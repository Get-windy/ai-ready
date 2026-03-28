import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'

// 统一响应结构
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 分页响应
export interface PageResponse<T = any> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

// 创建axios实例
const service: AxiosInstance = axios.create({
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
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const { code, message: msg, data } = response.data

    if (code === 200) {
      return response.data as any
    }

    // 业务错误处理
    if (code === 401) {
      const userStore = useUserStore()
      userStore.logout()
      window.location.href = '/login'
      return Promise.reject(new Error('登录已过期'))
    }

    message.error(msg || '请求失败')
    return Promise.reject(new Error(msg))
  },
  (error) => {
    if (error.response) {
      const { status } = error.response
      switch (status) {
        case 401:
          message.error('未授权，请重新登录')
          break
        case 403:
          message.error('拒绝访问')
          break
        case 404:
          message.error('请求资源不存在')
          break
        case 500:
          message.error('服务器错误')
          break
        default:
          message.error('网络错误')
      }
    }
    return Promise.reject(error)
  }
)

// 封装请求方法
export const request = {
  get<T = any>(url: string, params?: object): Promise<ApiResponse<T>> {
    return service.get(url, { params })
  },

  post<T = any>(url: string, data?: object): Promise<ApiResponse<T>> {
    return service.post(url, data)
  },

  put<T = any>(url: string, data?: object): Promise<ApiResponse<T>> {
    return service.put(url, data)
  },

  delete<T = any>(url: string, params?: object): Promise<ApiResponse<T>> {
    return service.delete(url, { params })
  }
}

export default service