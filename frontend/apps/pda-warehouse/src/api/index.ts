import axios from 'axios'
import { useUserStore } from '@/stores/user'

const request = axios.create({
  baseURL: '/api/v1/warehouse',
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
  task: {
    getList: (params?: any) => request.get('/tasks', { params }),
    getDetail: (id: string) => request.get(`/tasks/${id}`),
    start: (id: string) => request.put(`/tasks/${id}/start`),
    complete: (id: string) => request.put(`/tasks/${id}/complete`),
    cancel: (id: string) => request.put(`/tasks/${id}/cancel`)
  },
  
  receive: {
    getList: (params?: any) => request.get('/receive', { params }),
    getDetail: (id: string) => request.get(`/receive/${id}`),
    scanProduct: (id: string, barcode: string) => request.post(`/receive/${id}/scan`, { barcode }),
    confirmReceive: (id: string, data: any) => request.post(`/receive/${id}/confirm`, data),
    reportException: (id: string, data: any) => request.post(`/receive/${id}/exception`, data)
  },
  
  pick: {
    getList: (params?: any) => request.get('/pick', { params }),
    getDetail: (id: string) => request.get(`/pick/${id}`),
    verifyScan: (taskId: string, itemId: string, barcode: string) => 
      request.post(`/pick/${taskId}/verify`, { itemId, barcode }),
    confirmItem: (itemId: string, quantity: number) => 
      request.put(`/pick/item/${itemId}`, { quantity }),
    completePick: (id: string) => request.put(`/pick/${id}/complete`),
    reportException: (id: string, data: any) => request.post(`/pick/${id}/exception`, data)
  },
  
  check: {
    getList: (params?: any) => request.get('/check', { params }),
    getDetail: (id: string) => request.get(`/check/${id}`),
    scanLocation: (id: string, locationCode: string) => 
      request.post(`/check/${id}/scan-location`, { locationCode }),
    scanProduct: (id: string, barcode: string, quantity: number) => 
      request.post(`/check/${id}/scan-product`, { barcode, quantity }),
    submitCheck: (id: string) => request.put(`/check/${id}/submit`),
    reportDifference: (id: string, data: any) => request.post(`/check/${id}/difference`, data)
  },
  
  location: {
    getList: (params?: any) => request.get('/locations', { params }),
    getDetail: (code: string) => request.get(`/locations/${code}`),
    getProducts: (code: string) => request.get(`/locations/${code}/products`),
    transfer: (data: any) => request.post('/locations/transfer', data)
  },
  
  user: {
    getInfo: () => request.get('/user/info'),
    getStatistics: () => request.get('/user/statistics'),
    updatePassword: (data: any) => request.put('/user/password', data)
  },
  
  auth: {
    login: (data: { username: string; password: string }) => request.post('/auth/login', data),
    logout: () => request.post('/auth/logout')
  }
}

export default api