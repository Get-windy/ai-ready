import axios from 'axios'
import { useUserStore } from '@/stores/user'

const request = axios.create({
  baseURL: '/api/v1/delivery',
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
  order: {
    getList: (params?: any) => request.get('/orders', { params }),
    getDetail: (id: string) => request.get(`/orders/${id}`),
    accept: (id: string) => request.put(`/orders/${id}/accept`),
    reject: (id: string, reason: string) => request.put(`/orders/${id}/reject`, { reason }),
    startDelivery: (id: string) => request.put(`/orders/${id}/start`),
    completeDelivery: (id: string) => request.put(`/orders/${id}/complete`)
  },
  
  delivery: {
    getList: (params?: any) => request.get('/deliveries', { params }),
    getDetail: (id: string) => request.get(`/deliveries/${id}`),
    getRoute: (id: string) => request.get(`/deliveries/${id}/route`),
    updateLocation: (id: string, location: { lat: number; lng: number }) => 
      request.put(`/deliveries/${id}/location`, location),
    getPendingCount: () => request.get('/deliveries/pending-count')
  },
  
  // ⚠️ 签收 / 收款已迁到真实契约 `@/api/dms`（`/api/dms/sign/submit`、`/api/dms/payment/confirm`）。
  //    旧 `api.sign.submit → POST /orders/{id}/sign`、`api.collect.submit → POST /orders/{id}/collect`
  //    指向不存在的端点（后端无该 mapping），2026-09-13 删除。

  map: {
    getDeliveryPoints: () => request.get('/map/delivery-points'),
    getOptimizedRoute: (points: any[]) => request.post('/map/optimize-route', { points }),
    getNavigationUrl: (from: any, to: any) => request.get('/map/navigation-url', { params: { from, to } })
  },
  
  user: {
    getInfo: () => request.get('/user/info'),
    getStatistics: (params?: any) => request.get('/user/statistics', { params }),
    getHistory: (params?: any) => request.get('/user/history', { params }),
    updateProfile: (data: any) => request.put('/user/profile', data)
  },
  
  auth: {
    login: (data: { username: string; password: string }) => request.post('/auth/login', data),
    logout: () => request.post('/auth/logout')
  }
}

export default api