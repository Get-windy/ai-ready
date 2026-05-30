import axios from 'axios'
import { useUserStore } from '@/stores/user'

const request = axios.create({
  baseURL: '/api/v1/mall',
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
    register: (data: any) => request.post('/auth/register', data),
    logout: () => request.post('/auth/logout'),
    refreshToken: () => request.post('/auth/refresh-token')
  },
  
  user: {
    getInfo: () => request.get('/user/info'),
    updateProfile: (data: any) => request.put('/user/profile', data),
    getAddresses: () => request.get('/user/addresses'),
    addAddress: (data: any) => request.post('/user/addresses', data),
    updateAddress: (id: string, data: any) => request.put(`/user/addresses/${id}`, data),
    deleteAddress: (id: string) => request.delete(`/user/addresses/${id}`),
    setDefaultAddress: (id: string) => request.put(`/user/addresses/${id}/default`)
  },
  
  product: {
    getList: (params: any) => request.get('/products', { params }),
    getDetail: (id: string) => request.get(`/products/${id}`),
    getCategories: () => request.get('/products/categories'),
    getCategoryProducts: (categoryId: string, params: any) => 
      request.get(`/products/categories/${categoryId}/products`, { params }),
    search: (keyword: string, params: any) => request.get('/products/search', { params: { keyword, ...params } }),
    getRecommendations: () => request.get('/products/recommendations'),
    getHotProducts: () => request.get('/products/hot'),
    getBanners: () => request.get('/products/banners')
  },
  
  cart: {
    getList: () => request.get('/cart'),
    add: (data: { productId: string; quantity: number }) => request.post('/cart', data),
    update: (id: string, quantity: number) => request.put(`/cart/${id}`, { quantity }),
    remove: (id: string) => request.delete(`/cart/${id}`),
    clear: () => request.delete('/cart'),
    checkStock: (items: any[]) => request.post('/cart/check-stock', { items })
  },
  
  order: {
    create: (data: any) => request.post('/orders', data),
    getList: (params: any) => request.get('/orders', { params }),
    getDetail: (id: string) => request.get(`/orders/${id}`),
    cancel: (id: string) => request.put(`/orders/${id}/cancel`),
    confirm: (id: string) => request.put(`/orders/${id}/confirm`),
    pay: (id: string, data: any) => request.post(`/orders/${id}/pay`, data),
    getPaymentMethods: () => request.get('/orders/payment-methods'),
    track: (id: string) => request.get(`/orders/${id}/track`)
  },
  
  payment: {
    createPayment: (data: any) => request.post('/payments', data),
    getPaymentStatus: (id: string) => request.get(`/payments/${id}/status`),
    callback: (data: any) => request.post('/payments/callback', data)
  }
}

export default api