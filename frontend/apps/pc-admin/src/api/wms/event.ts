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
  page(params: any) { return request.get('/wms/event/outbox/page', { params }) },
  retry(id: number) { return request.post('/wms/event/outbox/retry', {}, { params: { eventId: id } }) },
  // 「触发处理待处理事件」已于 2026-09-23 移除：原派发逻辑用裸 HttpClient 调 ERP 回调端点
  // 必然 401，且接收端为空壳，整条链路无生产者亦无业务，后端端点一并删除。
}
