/**
 * DMS 轨迹追踪 API 模块
 */
import request from '@/utils/request'

// ── 轨迹追踪 ──────────────────────────────────────────
export interface DmsTrackingReport {
  riderId: number
  lng: number
  lat: number
  speed?: number
  direction?: number
  reportTime: string
}

export interface DmsTrackingRecord {
  id: number
  riderId: number
  riderName: string
  taskId?: number
  lng: number
  lat: number
  speed: number
  direction: number
  reportTime: string
  source?: string
  createTime: string
}

export interface DmsTrackPoint {
  lng: number
  lat: number
  speed: number
  direction: number
  reportTime: string
}

export const trackingApi = {
  /** 上报位置 */
  report(data: DmsTrackingReport) { return request.post('/dms/tracking/report', data) },

  /** 获取骑手最新位置 */
  latest(riderId: number) { return request.get(`/dms/tracking/latest/${riderId}`) },

  /** 获取轨迹 */
  track(params: { riderId: number; startTime: string; endTime: string }) { return request.get('/dms/tracking/track', { params }) },

  /** 根据任务获取轨迹 */
  trackByTask(taskId: number) { return request.get(`/dms/tracking/task/${taskId}`) },

  /** 分页查询轨迹记录 */
  page(params: any) { return request.get('/dms/tracking/page', { params }) },
}
