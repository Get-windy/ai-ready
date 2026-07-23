/**
 * DMS 轨迹追踪 API 模块
 * 后端: TrackingController (/api/dms/tracking)
 *
 * 注意：后端暂无轨迹分页接口（/dms/tracking/page 未实现），
 * 轨迹查询走 /track（时间范围）或 /task/{taskId}。
 */
import request from '@/utils/request'

// ── 轨迹追踪 ──────────────────────────────────────────
/** 位置上报参数（后端为 RequestParam 接收） */
export interface DmsTrackingReport {
  riderId: number
  taskId?: number
  lng: number
  lat: number
  speed?: number
  direction?: number
}

/** 与后端 DmsTracking 实体一致 */
export interface DmsTrackingRecord {
  id: number
  riderId: number
  taskId?: number
  lng: number
  lat: number
  speed?: number
  direction?: number
  reportTime: string
  /** 数据来源: 1-APP上报 2-后台查询 */
  source?: number
  createTime: string
}

export const trackingApi = {
  /** 上报位置（后端使用 RequestParam 接收） */
  report(data: DmsTrackingReport) { return request.post('/dms/tracking/report', null, { params: { ...data } }) },

  /** 获取骑手最新位置 */
  latest(riderId: number): Promise<DmsTrackingRecord> {
    return request.get(`/dms/tracking/latest/${riderId}`)
  },

  /** 获取轨迹（时间范围，ISO 日期时间格式 YYYY-MM-DDTHH:mm:ss） */
  track(params: { riderId: number; startTime: string; endTime: string }): Promise<DmsTrackingRecord[]> {
    return request.get('/dms/tracking/track', { params })
  },

  /** 根据任务获取轨迹 */
  trackByTask(taskId: number): Promise<DmsTrackingRecord[]> {
    return request.get(`/dms/tracking/task/${taskId}`)
  },

  /**
   * 分页查询轨迹记录
   * @deprecated 后端 /dms/tracking/page 接口未实现，调用会 404；保留仅为兼容既有页面
   */
  page(params: any) { return request.get('/dms/tracking/page', { params }) },
}
