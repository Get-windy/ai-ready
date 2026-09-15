/**
 * DMS 智能调度 API 模块（配送 → 调度管理 → 智能调度，菜单 80850 `dms:dispatch`）
 *
 * 后端：`DispatchController`（`/api/dms/dispatch`）——派单策略配置 / 调度预览与执行 / 候选配送员 / 效果复盘。
 * 口径见《智能调度开发文档》§3.3；本页定位「策略与执行台」，任务列表复用 `/dms/task/page`（不重复造列表）。
 */
import request from '@/utils/request'

/** 派单策略与约束（后端 DispatchStrategyDTO） */
export interface DispatchStrategy {
  /** NEAREST-最近可用 / BALANCED-负载均衡 / SCORE-评分优先 / AREA-区域分包 */
  strategy: string
  weightDistance?: number
  weightLoad?: number
  weightScore?: number
  /** 单配送员最大并接在途单数（0=不限） */
  maxConcurrent?: number
  /** 是否仅向「空闲」配送员派单 */
  requireOnline?: boolean
  maxLoadKg?: number
  maxVolumeM3?: number
  timeoutEscalateMinutes?: number
  /** 区域分包严格模式（仅 AREA 策略生效）：true=任务线路未绑定该配送员即不可派 */
  areaStrict?: boolean
}

/** 派单候选配送员 */
export interface DispatchCandidate {
  riderId: number
  riderName?: string
  riderPhone?: string
  /** 车牌号（自带车辆） */
  vehicleNo?: string
  /** 当前纬度（地图派单落点） */
  currentLat?: number
  /** 当前经度（地图派单落点） */
  currentLng?: number
  /** 状态：0-离线 1-空闲 2-忙碌 3-休息 */
  status?: number
  /** 累计完成单量 */
  totalOrders?: number
  distanceMeters?: number
  activeTasks?: number
  ratingScore?: number
  /** 是否在线（最近位置上报在 dms.tracking.online.minutes 内） */
  online?: boolean
  /** 最近位置上报时间 */
  lastReportTime?: string
  /** 区域分包：是否绑定了任务所属线路（AREA 策略下有效） */
  routeBound?: boolean
  eligible?: boolean
  reason?: string
  score?: number
}

/** 线路档案选项（区域分包绑定的线路选择器） */
export interface RouteOption {
  id: number
  routeCode?: string
  routeName?: string
  routeSelf?: number
  routeLogistics?: number
  expressName?: string
}

/** 配送员选项（区域分包绑定的配送员选择器） */
export interface RiderOption {
  id: number
  riderNo?: string
  realName?: string
  phone?: string
  status?: number
  verifyStatus?: number
}

/** 线路-配送员绑定（区域分包） */
export interface RouteRiderBinding {
  id: number
  routeId: number
  routeCode?: string
  routeName?: string
  riderId: number
  riderName?: string
  riderPhone?: string
  riderStatus?: number
  priority?: number
  status?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface RouteRiderSaveData {
  routeId?: number
  riderId?: number
  priority?: number
  status?: number
  remark?: string
}

export interface RouteRiderQuery {
  pageNum?: number
  pageSize?: number
  routeId?: number
  riderId?: number
  status?: number
  keyword?: string
}

/** 批量操作结果（后端 BatchResultVO） */
export interface DispatchBatchResult {
  total: number
  success: number
  failed: number
  items?: Array<{ taskId?: number; taskNo?: string; reason?: string }>
}

/** 自动调度预览结果 */
export interface DispatchPreview {
  strategy: string
  strategyText: string
  taskCount: number
  assignableCount: number
  unassignableCount: number
  rows: DispatchPreviewRow[]
}

export interface DispatchPreviewRow {
  taskId: number
  taskNo?: string
  customerName?: string
  status?: number
  totalWeight?: number
  totalVolume?: number
  riderId?: number
  riderName?: string
  riderPhone?: string
  distanceMeters?: number
  /** 命中规则说明 */
  ruleHit?: string
  /** 未命中/被拒原因 */
  failReason?: string
  assignable?: boolean
}

/** 调度效果统计 */
export interface DispatchStat {
  taskTotal: number
  pendingCount: number
  assignedCount: number
  autoCount: number
  manualCount: number
  /** 抢单数（归口《订单池》） */
  grabCount: number
  /** 竞价数（归口《订单池》） */
  bidCount: number
  /** 其他方式（未记方式的历史数据） */
  otherCount: number
  autoRate: number
  avgDispatchSeconds: number
  activeCount: number
  overdueCount: number
  overdueRate: number
  unassignedCount: number
}

export const dispatchApi = {
  // ── 策略配置 ──
  strategy(): Promise<DispatchStrategy> { return request.get('/dms/dispatch/strategy') },

  saveStrategy(data: DispatchStrategy): Promise<DispatchStrategy> {
    return request.put('/dms/dispatch/strategy', data)
  },

  // ── 调度执行 ──
  /** 自动调度预览（不落库） */
  preview(maxTasks?: number): Promise<DispatchPreview> {
    return request.post('/dms/dispatch/auto/preview', { maxTasks })
  },

  /** 执行自动调度（dryRun=true 只预览；返回逐单结果与失败原因） */
  autoDispatch(dryRun = false, maxTasks?: number): Promise<DispatchPreview> {
    return request.post('/dms/dispatch/auto', { dryRun, maxTasks })
  },

  /** 手动指派（统一 body；reason 仅留痕到调度审计） */
  assign(taskId: number | string, riderId: number | string, reason?: string) {
    return request.post(`/dms/dispatch/${taskId}/assign`, { riderId, reason })
  },

  /** 改派（reason 仅留痕到调度审计） */
  reassign(taskId: number | string, fromRiderId: number | string, toRiderId: number | string, reason?: string) {
    return request.post(`/dms/dispatch/${taskId}/reassign`, { fromRiderId, toRiderId, reason })
  },

  /** 候选配送员（含评分/在途负载/约束说明） */
  candidates(taskId: number | string): Promise<DispatchCandidate[]> {
    return request.get('/dms/dispatch/candidates', { params: { taskId } })
  },

  /** 批量指派（逐单结果反馈） */
  batchAssign(taskIds: Array<number | string>, riderId: number, reason?: string): Promise<DispatchBatchResult> {
    return request.post('/dms/dispatch/batch-assign', { taskIds, riderId, reason })
  },

  /** 超时升级扫描（超时未接单自动重派；已超时在途告警） */
  escalateOverdue(): Promise<DispatchBatchResult> {
    return request.post('/dms/dispatch/escalate-overdue')
  },

  /** 电子围栏校验 */
  fenceCheck(taskId: number | string, riderId: number | string): Promise<boolean> {
    return request.post(`/dms/dispatch/${taskId}/fence-check`, { riderId })
  },

  // ── 区域分包绑定（线路档案 × 配送员） ──
  /** 绑定列表（分页） */
  routeRiderPage(query: RouteRiderQuery) {
    return request.get('/dms/dispatch/route-rider/page', { params: query })
  },

  /** 线路档案选择器（只读引用《资料 → 配送管理 → 线路》） */
  routeOptions(keyword?: string): Promise<RouteOption[]> {
    return request.get('/dms/dispatch/route-rider/route-options', { params: { keyword } })
  },

  /** 配送员选择器 */
  riderOptions(keyword?: string): Promise<RiderOption[]> {
    return request.get('/dms/dispatch/route-rider/rider-options', { params: { keyword } })
  },

  createRouteRider(data: RouteRiderSaveData): Promise<RouteRiderBinding> {
    return request.post('/dms/dispatch/route-rider', data)
  },

  updateRouteRider(id: number | string, data: RouteRiderSaveData): Promise<RouteRiderBinding> {
    return request.put(`/dms/dispatch/route-rider/${id}`, data)
  },

  updateRouteRiderStatus(id: number | string, status: number) {
    return request.put(`/dms/dispatch/route-rider/${id}/status`, { status })
  },

  deleteRouteRider(id: number | string) {
    return request.delete(`/dms/dispatch/route-rider/${id}`)
  },

  // ── 效果复盘 ──
  stat(): Promise<DispatchStat> { return request.get('/dms/dispatch/stat') },
}
