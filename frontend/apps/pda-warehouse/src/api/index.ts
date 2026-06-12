/**
 * WMS PDA API 客户端
 * 与后端控制器映射对齐
 * 后端所有 PDA 接口前缀: /api/v1/warehouse
 */
import axios from 'axios'
import { useUserStore } from '@/stores/user'
import type { PdaTaskItem, ReceiptTask, CheckTask, InventoryRecord } from '@/types'

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
  /** PdaTaskController — 任务聚合 */
  task: {
    getList: () => request.get<any, PdaTaskItem[]>('/tasks'),
    getDetail: (id: number, taskType: string) =>
      request.get<any, Record<string, any>>(`/tasks/${id}`, { params: { taskType } }),
    start: (id: number, taskType: string) =>
      request.put(`/tasks/${id}/start`, { taskType }),
    complete: (id: number, taskType: string) =>
      request.put(`/tasks/${id}/complete`, { taskType }),
  },

  /** PdaReceiveController — 收货 */
  receive: {
    getList: () => request.get<any, ReceiptTask[]>('/receive'),
    getDetail: (id: number) => request.get<any, Record<string, any>>(`/receive/${id}`),
    scanProduct: (id: number, barcode: string, locationCode?: string) =>
      request.post(`/receive/${id}/scan`, { barcode, locationCode }),
    confirmReceive: (id: number, locationCode?: string) =>
      request.post(`/receive/${id}/confirm`, { locationCode }),
    reportException: (id: number, data: Record<string, any>) =>
      request.post(`/receive/${id}/exception`, data),
  },

  /** PdaPutawayController — 上架 */
  putaway: {
    getList: () => request.get<any, any[]>('/putaway'),
    getDetail: (id: number) => request.get(`/putaway/${id}`),
    scan: (id: number, barcode: string) =>
      request.post(`/putaway/${id}/scan`, { barcode }),
    confirm: (id: number) =>
      request.post(`/putaway/${id}/confirm`),
  },

  /** PdaPickController — 拣货 */
  pick: {
    getList: () => request.get<any, any[]>('/pick'),
    getDetail: (id: number) => request.get(`/pick/${id}`),
    verifyScan: (id: number, itemId: number, barcode: string) =>
      request.post(`/pick/${id}/verify`, { itemId, barcode }),
    confirmItem: (itemId: number, quantity: number) =>
      request.put(`/pick/item/${itemId}`, { quantity }),
    completePick: (id: number) =>
      request.put(`/pick/${id}/complete`),
    reportException: (id: number, reason: string) =>
      request.post(`/pick/${id}/exception`, { reason }),
  },

  /** PdaCheckController — 盘点 */
  check: {
    getList: () => request.get<any, CheckTask[]>('/check'),
    getDetail: (id: number) => request.get<any, Record<string, any>>(`/check/${id}`),
    scanLocation: (id: number, barcode: string) =>
      request.post(`/check/${id}/scan-location`, { barcode }),
    scanProduct: (id: number, barcode: string, quantity?: number) =>
      request.post(`/check/${id}/scan-product`, { barcode, quantity }),
    submitResult: (id: number) =>
      request.put(`/check/${id}/submit`),
  },

  /** PdaShipController — 发货 */
  ship: {
    getList: () => request.get<any, any[]>('/ship'),
    getDetail: (id: number) => request.get<any, Record<string, any>>(`/ship/${id}`),
    scan: (id: number, barcode: string) =>
      request.post(`/ship/${id}/scan`, { barcode }),
    confirm: (id: number) =>
      request.put(`/ship/${id}/confirm`),
  },

  /** PdaMoveController — 移库 */
  move: {
    create: (data: Record<string, any>) => request.post('/move', data),
    getDetail: (id: number) => request.get(`/move/${id}`),
    execute: (id: number) => request.post(`/move/${id}/execute`),
  },

  /** PdaInventoryController — 库存查询 */
  inventory: {
    query: (params?: { productCode?: string; locationCode?: string }) =>
      request.get<any, InventoryRecord[]>('/inventory/query', { params }),
  },

  /** PdaAuthController — 认证 */
  auth: {
    login: (username: string, password: string) =>
      request.post('/auth/login', null, { params: { username, password } }),
    logout: () => request.post('/auth/logout'),
  },

  /**
   * 库位查询（待后端实现）
   * 临时后端映射: 使用 inventory 接口查询
   */
  location: {
    getList: (params?: any) => request.get('/inventory/query', { params }),
    getProducts: (code: string) => request.get('/inventory/query', { params: { locationCode: code } }),
  },
}

export default api
