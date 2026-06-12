/**
 * WMS 事件 API 模块
 */
import request from '@/utils/request'

// ── 事件发件箱 ──────────────────────────────────────
export interface WmsEventOutbox {
  id: number
  eventType: string
  eventKey: string
  payload: string
  status: number
  retryCount: number
  maxRetries: number
  lastError: string
  createTime: string
  updateTime: string
}

export const eventApi = {
  page(params: any) { return request.get('/wms/event/page', { params }) },
  retry(id: number) { return request.post(`/wms/event/${id}/retry`) },
  processPending() { return request.post('/wms/event/process-pending') },
}
