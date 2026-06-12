/**
 * DMS 骑手管理 API 模块
 */
import request from '@/utils/request'

// ── 骑手 ──────────────────────────────────────────
export interface DmsRider {
  id: number
  realName: string
  phone: string
  riderType: number
  status: number
  verifyStatus: number
  ratingScore: number
  totalOrders: number
  vehicleId: number
  vehiclePlate: string
  remark: string
  createTime: string
  updateTime: string
}

export interface DmsRiderLocation {
  lng: number
  lat: number
  speed?: number
  direction?: number
}

export const riderApi = {
  page(params: any) { return request.get('/api/dms/rider/page', { params }) },

  getById(id: number) { return request.get(`/api/dms/rider/${id}`) },

  create(data: Partial<DmsRider>) { return request.post('/api/dms/rider', data) },

  update(id: number, data: Partial<DmsRider>) { return request.put(`/api/dms/rider/${id}`, data) },

  updateStatus(id: number, status: number) { return request.put(`/api/dms/rider/${id}/status`, { status }) },

  approve(id: number, data: { verifyStatus: number; remark?: string }) { return request.post(`/api/dms/rider/${id}/approve`, data) },

  reportLocation(id: number, data: DmsRiderLocation) { return request.post(`/api/dms/rider/${id}/location`, data) },
}
