/**
 * API配置
 */

import type { AxiosRequestConfig } from 'axios'

// API基础配置
export interface IApiConfig {
  baseURL: string
  timeout: number
  withCredentials: boolean
  headers: Record<string, string>
}

// API路径配置
export interface IApiPaths {
  v1: string
  admin: string
  mobile: string
}

// 根据环境获取配置
function getApiConfig(): { config: IApiConfig; paths: IApiPaths } {
  const env = import.meta.env.VITE_APP_ENV || 'development'
  const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'
  const timeout = parseInt(import.meta.env.VITE_API_TIMEOUT || '30000')
  
  const config: IApiConfig = {
    baseURL,
    timeout,
    withCredentials: true,
    headers: {
      'Content-Type': 'application/json',
      'X-Requested-With': 'XMLHttpRequest'
    }
  }
  
  const paths: IApiPaths = {
    v1: import.meta.env.VITE_API_BASE_URL || '/api/v1',
    admin: import.meta.env.VITE_API_ADMIN_URL || '/api/admin',
    mobile: import.meta.env.VITE_API_MOBILE_URL || '/api/mobile'
  }
  
  // 开发环境下添加调试头
  if (env === 'development' || import.meta.env.VITE_FEATURE_DEBUG === 'true') {
    config.headers['X-Debug-Mode'] = 'true'
  }
  
  return { config, paths }
}

// 业务模块配置
export interface IBusinessModule {
  name: string
  prefix: string
  description: string
}

export const businessModules: IBusinessModule[] = [
  {
    name: 'user-management',
    prefix: '/users',
    description: '用户管理模块'
  },
  {
    name: 'order-management',
    prefix: '/orders',
    description: '订单管理模块'
  },
  {
    name: 'finance-management',
    prefix: '/finance',
    description: '财务管理模块'
  },
  {
    name: 'purchase-management',
    prefix: '/purchases',
    description: '采购管理模块'
  },
  {
    name: 'inventory-management',
    prefix: '/inventory',
    description: '库存管理模块'
  },
  {
    name: 'monitoring-system',
    prefix: '/monitoring',
    description: '监控系统模块'
  },
  {
    name: 'report-system',
    prefix: '/reports',
    description: '报表系统模块'
  }
]

// API端点配置
export const apiEndpoints = {
  // 用户管理
  auth: {
    login: '/auth/login',
    logout: '/auth/logout',
    refresh: '/auth/refresh',
    profile: '/auth/profile'
  },
  
  users: {
    list: '/users',
    detail: (id: string | number) => `/users/${id}`,
    create: '/users',
    update: (id: string | number) => `/users/${id}`,
    delete: (id: string | number) => `/users/${id}`,
    roles: '/users/roles',
    permissions: '/users/permissions'
  },
  
  // 订单管理
  orders: {
    list: '/orders',
    detail: (id: string | number) => `/orders/${id}`,
    create: '/orders',
    update: (id: string | number) => `/orders/${id}`,
    delete: (id: string | number) => `/orders/${id}`,
    status: (id: string | number) => `/orders/${id}/status`,
    items: (id: string | number) => `/orders/${id}/items`
  },
  
  // 财务管理
  finance: {
    invoices: {
      list: '/finance/invoices',
      detail: (id: string | number) => `/finance/invoices/${id}`,
      create: '/finance/invoices',
      update: (id: string | number) => `/finance/invoices/${id}`,
      delete: (id: string | number) => `/finance/invoices/${id}`,
      payments: (id: string | number) => `/finance/invoices/${id}/payments`
    },
    payments: {
      list: '/finance/payments',
      detail: (id: string | number) => `/finance/payments/${id}`,
      create: '/finance/payments',
      update: (id: string | number) => `/finance/payments/${id}`,
      delete: (id: string | number) => `/finance/payments/${id}`
    },
    reports: {
      daily: '/finance/reports/daily',
      monthly: '/finance/reports/monthly',
      yearly: '/finance/reports/yearly',
      summary: '/finance/reports/summary'
    }
  },
  
  // 采购管理
  purchases: {
    orders: {
      list: '/purchases/orders',
      detail: (id: string | number) => `/purchases/orders/${id}`,
      create: '/purchases/orders',
      update: (id: string | number) => `/purchases/orders/${id}`,
      delete: (id: string | number) => `/purchases/orders/${id}`,
      approve: (id: string | number) => `/purchases/orders/${id}/approve`
    },
    suppliers: {
      list: '/purchases/suppliers',
      detail: (id: string | number) => `/purchases/suppliers/${id}`,
      create: '/purchases/suppliers',
      update: (id: string | number) => `/purchases/suppliers/${id}`,
      delete: (id: string | number) => `/purchases/suppliers/${id}`
    }
  },
  
  // 监控系统
  monitoring: {
    alerts: {
      list: '/monitoring/alerts',
      detail: (id: string | number) => `/monitoring/alerts/${id}`,
      create: '/monitoring/alerts',
      update: (id: string | number) => `/monitoring/alerts/${id}`,
      delete: (id: string | number) => `/monitoring/alerts/${id}`,
      acknowledge: (id: string | number) => `/monitoring/alerts/${id}/acknowledge`
    },
    metrics: {
      list: '/monitoring/metrics',
      query: '/monitoring/metrics/query',
      history: '/monitoring/metrics/history'
    },
    dashboards: {
      list: '/monitoring/dashboards',
      detail: (id: string | number) => `/monitoring/dashboards/${id}`,
      create: '/monitoring/dashboards',
      update: (id: string | number) => `/monitoring/dashboards/${id}`,
      delete: (id: string | number) => `/monitoring/dashboards/${id}`
    }
  }
}

// 请求配置工厂
export function createRequestConfig(
  config: Partial<AxiosRequestConfig> = {}
): AxiosRequestConfig {
  const apiConfig = getApiConfig()
  
  return {
    baseURL: apiConfig.config.baseURL,
    timeout: apiConfig.config.timeout,
    withCredentials: apiConfig.config.withCredentials,
    headers: {
      ...apiConfig.config.headers,
      ...config.headers
    },
    ...config
  }
}

// 导出配置
export const { config: apiConfig, paths: apiPaths } = getApiConfig()
export default apiConfig