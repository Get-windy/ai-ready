import axios from 'axios'
import { useUserStore } from '@/stores/user'

const request = axios.create({
  baseURL: '/api/v1/admin',
  timeout: 10000
})

request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
    }
    return Promise.reject(error)
  }
)

export const api = {
  auth: {
    login: (data: { username: string; password: string }) => request.post('/auth/login', data),
    logout: () => request.post('/auth/logout'),
    refreshToken: () => request.post('/auth/refresh-token')
  },
  
  approval: {
    getList: (params?: any) => request.get('/approvals', { params }),
    getDetail: (id: string) => request.get(`/approvals/${id}`),
    approve: (id: string, data?: any) => request.put(`/approvals/${id}/approve`, data),
    reject: (id: string, reason: string) => request.put(`/approvals/${id}/reject`, { reason }),
    getPendingCount: () => request.get('/approvals/pending-count'),
    getMyApprovals: (params?: any) => request.get('/approvals/my', { params }),
    getHistory: (params?: any) => request.get('/approvals/history', { params })
  },
  
  customer: {
    getList: (params?: any) => request.get('/customers', { params }),
    getDetail: (id: string) => request.get(`/customers/${id}`),
    getFollowUps: (id: string) => request.get(`/customers/${id}/follow-ups`),
    addFollowUp: (id: string, data: any) => request.post(`/customers/${id}/follow-ups`, data),
    getOpportunities: (id: string) => request.get(`/customers/${id}/opportunities`),
    getOrders: (id: string) => request.get(`/customers/${id}/orders`),
    search: (keyword: string) => request.get('/customers/search', { params: { keyword } })
  },
  
  order: {
    getList: (params?: any) => request.get('/orders', { params }),
    getDetail: (id: string) => request.get(`/orders/${id}`),
    getStatistics: () => request.get('/orders/statistics'),
    getTodayOrders: () => request.get('/orders/today'),
    getPendingOrders: () => request.get('/orders/pending')
  },
  
  dashboard: {
    getKpiData: () => request.get('/dashboard/kpi'),
    getSalesData: (period?: string) => request.get('/dashboard/sales', { params: { period } }),
    getPendingApprovals: () => request.get('/dashboard/pending-approvals'),
    getRecentActivities: () => request.get('/dashboard/recent-activities'),
    getAlerts: () => request.get('/dashboard/alerts')
  },
  
  statistics: {
    getSalesStats: (params?: any) => request.get('/statistics/sales', { params }),
    getCustomerStats: (params?: any) => request.get('/statistics/customers', { params }),
    getOrderStats: (params?: any) => request.get('/statistics/orders', { params }),
    getPerformanceStats: (params?: any) => request.get('/statistics/performance', { params })
  },
  
  user: {
    getInfo: () => request.get('/user/info'),
    updateProfile: (data: any) => request.put('/user/profile', data),
    getPermissions: () => request.get('/user/permissions'),
    getStatistics: () => request.get('/user/statistics')
  }
}

export default api