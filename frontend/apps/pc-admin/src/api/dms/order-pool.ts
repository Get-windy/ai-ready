/**
 * DMS 订单池 API 模块
 */
import request from '@/utils/request'

// ── 订单池 ──────────────────────────────────────────
export interface DmsOrderPool {
  id: number
  orderNo: string
  pickupAddress: string
  deliveryAddress: string
  status: number
  bidCount: number
  grabCount: number
  expectedAmount: number
  createTime: string
  expiredTime: string
}

export interface DmsOrderBid {
  id: number
  orderId: number
  riderId: number
  riderName: string
  bidAmount: number
  status: number
  createTime: string
}

export const orderPoolApi = {
  page(params: any) { return request.get('/api/dms/order-pool/page', { params }) },

  getById(id: number) { return request.get(`/api/dms/order-pool/${id}`) },

  /** 抢单 */
  grab(id: number, data: { riderId: number }) { return request.post(`/api/dms/order-pool/${id}/grab`, data) },

  /** 竞价 */
  bid(id: number, data: { riderId: number; bidAmount: number }) { return request.post(`/api/dms/order-pool/${id}/bid`, data) },

  /** 竞价列表 */
  bidList(id: number) { return request.get(`/api/dms/order-pool/${id}/bids`) },

  /** 取消竞价 */
  cancelBid(id: number, riderId: number) { return request.delete(`/api/dms/order-pool/${id}/bid/${riderId}`) },
}
