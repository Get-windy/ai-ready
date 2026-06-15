/**
 * DMS 调度管理 API 模块
 */
import request from '@/utils/request'

// ── 调度 ──────────────────────────────────────────
export interface DmsDispatchOrder {
  id: number
  orderNo: string
  riderId: number
  riderName: string
  riderPhone: string
  status: number
  dispatchTime: string
  completeTime: string
  remark: string
  createTime: string
}

export interface DmsDispatchCandidate {
  riderId: number
  riderName: string
  phone: string
  distance: number
  ratingScore: number
  totalOrders: number
  online: boolean
}

export interface DmsDispatchRequest {
  orderId: number
  riderId: number
  remark?: string
}

export const dispatchApi = {
  /** 自动调度 */
  autoDispatch(data: { orderId: number; strategy?: string }) { return request.post('/dms/dispatch/auto', data) },

  /** 指派骑手 */
  assign(data: DmsDispatchRequest) { return request.post('/dms/dispatch/assign', data) },

  /** 重新指派 */
  reassign(data: DmsDispatchRequest) { return request.post('/dms/dispatch/reassign', data) },

  /** 获取候选骑手列表 */
  candidates(params: { orderId: number; lng?: number; lat?: number }) { return request.get('/dms/dispatch/candidates', { params }) },

  /** 电子围栏检测 */
  fenceCheck(data: { riderId: number; lng: number; lat: number; fenceId?: number }) { return request.post('/dms/dispatch/fence/check', data) },
}
