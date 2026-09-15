/**
 * DMS 轨迹追踪 API 模块（配送 → 配送跟踪 → 配送跟踪，菜单 80870 `dms:tracking`）
 *
 * 后端：`TrackingController`（`/api/dms/tracking`）——台账分页 / 里程聚合 / 轨迹（含抽稀）/ 位置上报 / 导出。
 * 口径见《配送跟踪开发文档》§3.4；与《实时跟踪》（可视化盯盘）分工：本页是**明细台账**。
 */
import request from '@/utils/request'

/** 轨迹台账行（后端 TrackingVO） */
export interface TrackingVO {
  id: number
  riderId?: number
  riderName?: string
  riderPhone?: string
  taskId?: number
  taskNo?: string
  customerName?: string
  lat?: number
  lng?: number
  /** km/h */
  speed?: number
  /** 方向角度 0-360 */
  direction?: number
  /** 方向文案：北 / 东北 / … */
  directionText?: string
  /** 1-APP 2-后台补录 3-渠道回传 */
  source?: number
  sourceText?: string
  /** 定位精度(米) */
  accuracy?: number
  address?: string
  reportTime?: string
  /** 段间里程(米)：与当前返回集合内同配送员上一点的距离（首条为 null） */
  segmentMeters?: number
  speedAbnormal?: boolean
}

/** 台账查询条件（《配送跟踪开发文档》§3.3） */
export interface TrackingQuery {
  pageNum?: number
  pageSize?: number
  riderId?: number | string
  taskId?: number | string
  taskNo?: string
  riderKeyword?: string
  /** 左面板：配送员状态 0-离线 1-空闲 2-忙碌 3-休息 */
  riderStatus?: number
  /** 左面板：仅今日有上报 */
  activeToday?: boolean
  /** 左面板：在线过滤（心跳派生）true-在线 false-离线 */
  online?: boolean
  startTime?: string
  endTime?: string
  /** CSV 多选：1-APP 2-后台 3-渠道 */
  sources?: string
  minSpeed?: number
  onlyWithTask?: boolean
  sortField?: string
  sortOrder?: string
}

/** 里程聚合行 */
export interface TrackingMileage {
  groupKey: string
  groupName: string
  pointCount: number
  mileageMeters: number
  mileageKm: number
  avgSpeed?: number
  firstTime?: string
  lastTime?: string
}

/** 位置上报参数（后端 RequestParam 接收） */
export interface DmsTrackingReport {
  riderId: number
  taskId?: number
  lng: number
  lat: number
  speed?: number
  direction?: number
  /** 定位精度(米) */
  accuracy?: number
  /** 地址（上报端逆编码结果） */
  address?: string
  /** 来源：1-APP 2-后台补录 3-渠道回传 */
  source?: number
}

/** 与后端 DmsTracking 实体一致（兼容旧调用） */
export interface DmsTrackingRecord {
  id: number
  riderId: number
  taskId?: number
  lng: number
  lat: number
  speed?: number
  direction?: number
  reportTime: string
  source?: number
  accuracy?: number
  address?: string
  createTime: string
}

/** 配送员实时位置行（实时跟踪页，后端 RiderLocationVO） */
export interface RiderLocationVO {
  riderId: number
  riderNo?: string
  riderName?: string
  riderPhone?: string
  riderTypeText?: string
  status?: number
  statusText?: string
  /** 在线（最近上报在阈值内） */
  online?: boolean
  onlineMinutes?: number
  lat?: number
  lng?: number
  speed?: number
  direction?: number
  directionText?: string
  accuracy?: number
  address?: string
  lastReportTime?: string
  lastReportAgoSeconds?: number
  todayMileageKm?: number
  todayPointCount?: number
  todayDoneTasks?: number
  /** 在途负载 */
  activeTasks?: number
  vehicleName?: string
}

/** 实时跟踪统计卡（后端 TrackingStatVO） */
export interface TrackingStat {
  riderTotal: number
  onlineCount: number
  offlineCount: number
  withLocationCount: number
  noLocationCount: number
  activeTaskCount: number
  overdueTaskCount: number
  todayPointCount: number
  alertCount: number
  onlineMinutes: number
}

/** 跟踪异常预警（后端 TrackingAlertVO） */
export interface TrackingAlert {
  alertType: 'OVERSPEED' | 'STAY' | 'OVERDUE' | 'DEVIATION'
  alertTypeText: string
  level: 'WARN' | 'DANGER'
  riderId?: number
  riderName?: string
  taskId?: number
  taskNo?: string
  lat?: number
  lng?: number
  eventTime?: string
  value?: number
  alertText: string
}

export const trackingApi = {
  /** 台账分页（多条件；原 `/page` 未实现，本次已补齐） */
  page(params: TrackingQuery) { return request.get('/dms/tracking/page', { params }) },

  // ── 实时跟踪（配送员位置聚合 / 统计 / 预警 / 回放） ──
  /** 配送员实时位置分页（一次聚合，替代 N+1） */
  riderPage(params: { pageNum?: number; pageSize?: number; keyword?: string; status?: number; onlineOnly?: boolean }) {
    return request.get('/dms/tracking/rider-page', { params })
  },

  /** 实时跟踪统计卡（后端聚合） */
  stat(): Promise<TrackingStat> { return request.get('/dms/tracking/stat') },

  /** 异常预警（超速 / 异常停留 / 超时在途） */
  alerts(params?: TrackingQuery): Promise<TrackingAlert[]> {
    return request.get('/dms/tracking/alerts', { params })
  },

  /** 轨迹回放点序列（抽稀；riderId 与 taskId 二选一） */
  replay(params: { riderId?: number | string; taskId?: number | string; startTime?: string; endTime?: string; maxPoints?: number }): Promise<TrackingVO[]> {
    return request.get('/dms/tracking/replay', { params })
  },

  /** 里程聚合：groupBy = rider / task / day */
  mileage(params: TrackingQuery & { groupBy?: string }) {
    return request.get('/dms/tracking/mileage', { params })
  },

  /** 导出真实 xlsx */
  export(params: TrackingQuery): Promise<Blob> {
    return request.get('/dms/tracking/export', { params, responseType: 'blob' })
  },

  /** 按配送员取轨迹（含抽稀，供地图绘制/回放） */
  trackVO(params: { riderId: number | string; startTime?: string; endTime?: string; maxPoints?: number }): Promise<TrackingVO[]> {
    return request.get('/dms/tracking/track-vo', { params })
  },

  /** 按任务取轨迹（含抽稀） */
  trackByTaskVO(taskId: number | string, maxPoints?: number): Promise<TrackingVO[]> {
    return request.get(`/dms/tracking/task-vo/${taskId}`, { params: { maxPoints } })
  },

  /** 上报位置（后端 RequestParam 接收） */
  report(data: DmsTrackingReport) { return request.post('/dms/tracking/report', null, { params: { ...data } }) },

  /** 获取配送员最新位置 */
  latest(riderId: number | string): Promise<DmsTrackingRecord> {
    return request.get(`/dms/tracking/latest/${riderId}`)
  },

  /** 获取轨迹（时间范围，ISO 日期时间格式 YYYY-MM-DDTHH:mm:ss） */
  track(params: { riderId: number; startTime: string; endTime: string }): Promise<DmsTrackingRecord[]> {
    return request.get('/dms/tracking/track', { params })
  },

  /** 根据任务获取轨迹（原始实体） */
  trackByTask(taskId: number | string): Promise<DmsTrackingRecord[]> {
    return request.get(`/dms/tracking/task/${taskId}`)
  },
}
