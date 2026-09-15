/**
 * DMS 配送仪表盘 API
 *
 * 后端：`cn.aiedge.dms.dashboard.DashboardController`（/api/dms/dashboard/*）
 * 口径：全部指标由数据库聚合产出，字段与后端 VO 一一对齐，前端不做二次汇总。
 * 时间范围：today / yesterday / last7 / last30 / month / custom（左闭右开）。
 */
import request from '@/utils/request'

// 后端全局 Jackson 将 Long 序列化为字符串（防 JS 精度丢失），
// 计数/金额类字段在前端入口统一归一化为 number，页面只处理真实数值。
function toNum(v: unknown): number {
  if (v === null || v === undefined || v === '') return 0
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

function toNumOrNull(v: unknown): number | null {
  if (v === null || v === undefined || v === '') return null
  const n = Number(v)
  return Number.isFinite(n) ? n : null
}

function normStats(raw: any): DmsDashboardStats {
  if (!raw) return raw
  return {
    ...raw,
    totalRiders: toNum(raw.totalRiders),
    activeRiders: toNum(raw.activeRiders),
    onlineRiders: toNum(raw.onlineRiders),
    totalVehicles: toNum(raw.totalVehicles),
    activeVehicles: toNum(raw.activeVehicles),
    orderCount: toNum(raw.orderCount),
    pendingOrders: toNum(raw.pendingOrders),
    assignedOrders: toNum(raw.assignedOrders),
    inTransitOrders: toNum(raw.inTransitOrders),
    completedOrders: toNum(raw.completedOrders),
    cancelledOrders: toNum(raw.cancelledOrders),
    exceptionOrders: toNum(raw.exceptionOrders),
    overdueOrders: toNum(raw.overdueOrders),
    onTimeRate: toNumOrNull(raw.onTimeRate),
    avgDeliveryMinutes: toNumOrNull(raw.avgDeliveryMinutes),
    deliveryFee: toNum(raw.deliveryFee),
    collectOnDelivery: toNum(raw.collectOnDelivery),
    goodsAmount: toNum(raw.goodsAmount),
    activeBindingCount: toNum(raw.activeBindingCount),
    pendingAlertCount: toNum(raw.pendingAlertCount),
    onlineThresholdMinutes: toNum(raw.onlineThresholdMinutes),
    refreshSeconds: toNum(raw.refreshSeconds),
  }
}

function normTrend(rows: any): DmsTrendPoint[] {
  return (Array.isArray(rows) ? rows : []).map((raw: any) => ({
    ...raw,
    orderCount: toNum(raw.orderCount),
    completedCount: toNum(raw.completedCount),
    onTimeCount: toNum(raw.onTimeCount),
    onTimeBase: toNum(raw.onTimeBase),
    onTimeRate: toNumOrNull(raw.onTimeRate),
    avgMinutes: toNumOrNull(raw.avgMinutes),
    deliveryFee: toNum(raw.deliveryFee),
  }))
}

function normSummary(rows: any): DmsTaskSummaryItem[] {
  return (Array.isArray(rows) ? rows : []).map((raw: any) => ({
    ...raw,
    status: toNum(raw.status),
    count: toNum(raw.count),
    ratio: toNum(raw.ratio),
  }))
}

function normDistribution(rows: any): DmsDistributionItem[] {
  return (Array.isArray(rows) ? rows : []).map((raw: any) => ({
    ...raw,
    orderCount: toNum(raw.orderCount),
    amount: toNum(raw.amount),
  }))
}

function normTopRiders(rows: any): DmsTopRider[] {
  return (Array.isArray(rows) ? rows : []).map((raw: any) => ({
    ...raw,
    ratingScore: toNumOrNull(raw.ratingScore),
    orderCount: toNum(raw.orderCount),
    completedCount: toNum(raw.completedCount),
    onTimeCount: toNum(raw.onTimeCount),
    onTimeBase: toNum(raw.onTimeBase),
    onTimeRate: toNumOrNull(raw.onTimeRate),
    avgMinutes: toNumOrNull(raw.avgMinutes),
  }))
}

/** 仪表盘查询条件（KPI / 趋势 / 分布 / 排行 / 状态分布 共用） */
export interface DmsDashboardQuery {
  /** 时间范围标识 */
  range?: 'today' | 'yesterday' | 'last7' | 'last30' | 'month' | 'custom'
  /** 自定义开始日期（range=custom 必填，YYYY-MM-DD） */
  startDate?: string
  /** 自定义结束日期（YYYY-MM-DD） */
  endDate?: string
  /** 运力渠道ID */
  channelId?: number | string | null
  /** 订单类型：1-销售配送 2-调拨 3-退货 */
  orderType?: number | null
}

/** KPI 聚合 */
export interface DmsDashboardStats {
  totalRiders: number
  activeRiders: number
  onlineRiders: number
  totalVehicles: number
  activeVehicles: number

  orderCount: number
  pendingOrders: number
  assignedOrders: number
  inTransitOrders: number
  completedOrders: number
  cancelledOrders: number
  exceptionOrders: number
  /** 超时未签收（已过截止时间且状态在待分配~配送中） */
  overdueOrders: number

  /** 准时率（%），无有效样本时为 null */
  onTimeRate: number | null
  /** 平均配送时长（分钟） */
  avgDeliveryMinutes: number | null
  deliveryFee: number
  collectOnDelivery: number
  goodsAmount: number

  activeBindingCount: number
  pendingAlertCount: number

  range: string
  rangeLabel: string
  startTime: string
  endTime: string
  onlineThresholdMinutes: number
  refreshSeconds: number
}

/** 按日趋势点 */
export interface DmsTrendPoint {
  statDate: string
  orderCount: number
  completedCount: number
  onTimeCount: number
  onTimeBase: number
  onTimeRate: number | null
  avgMinutes: number | null
  deliveryFee: number
}

/** 任务状态分布项 */
export interface DmsTaskSummaryItem {
  status: number
  statusName: string
  count: number
  ratio: number
}

/** 分布统计项（渠道 / 订单类型） */
export interface DmsDistributionItem {
  itemKey: string
  itemName: string
  orderCount: number
  amount: number
}

/** 配送员绩效排行项 */
export interface DmsTopRider {
  /** 后端 Long → 字符串（防精度丢失） */
  riderId: number | string
  riderName: string
  phone: string | null
  ratingScore: number | null
  orderCount: number
  completedCount: number
  onTimeCount: number
  onTimeBase: number
  onTimeRate: number | null
  avgMinutes: number | null
}

/** 活跃人车绑定（字段与后端 dms_rider_vehicle_binding 实体一致） */
export interface DmsActiveBinding {
  /** 后端 Long → 字符串（防精度丢失） */
  id: number | string
  riderId: number | string
  riderName: string
  riderPhone: string | null
  vehicleId: number | string
  plateNo: string
  bindTime: string
  bindMileage: number | null
  bindReason: string | null
  /** 0-绑定中 1-已交车 2-异常解绑 */
  status: number
}

/** 人车核验预警（字段与后端 dms_verification_alert 实体一致） */
export interface DmsPendingAlert {
  /** 后端 Long → 字符串（防精度丢失） */
  id: number | string
  alertType: number
  alertLevel: number
  riderId: number | string | null
  riderName: string | null
  plateNo: string | null
  taskId: number | string | null
  alertContent: string
  distanceMeters: number | null
  /** 0-未处理 1-已确认 2-已忽略 3-已处理 */
  handleStatus: number
  createTime: string
}

/** MyBatis-Plus 分页响应 */
export interface DmsPage<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export const dashboardApi = {
  /** KPI 聚合（运力规模 / 单量结构 / 时效 / 金额 / 待办计数） */
  stats(params?: DmsDashboardQuery) {
    return request.get<any, any>('/dms/dashboard/stats', { params }).then(normStats)
  },

  /** 活跃人车绑定（绑定中，分页） */
  activeBindings(params?: { page?: number; size?: number }) {
    return request.get<any, DmsPage<DmsActiveBinding>>('/dms/dashboard/active-bindings', { params })
  },

  /** 人车核验预警（默认未处理，分页） */
  pendingAlerts(params?: { page?: number; size?: number; handleStatus?: number }) {
    return request.get<any, DmsPage<DmsPendingAlert>>('/dms/dashboard/pending-alerts', { params })
  },

  /** 任务状态分布（9 态全量） */
  taskSummary(params?: DmsDashboardQuery) {
    return request.get<any, any>('/dms/dashboard/task-summary', { params }).then(normSummary)
  },

  /** 单量时效趋势（按日） */
  trend(params?: DmsDashboardQuery & { days?: number }) {
    return request.get<any, any>('/dms/dashboard/trend', { params }).then(normTrend)
  },

  /** 配送员绩效 Top */
  topRiders(params?: DmsDashboardQuery & { limit?: number }) {
    return request.get<any, any>('/dms/dashboard/top-riders', { params }).then(normTopRiders)
  },

  /** 任务分布（by=channel 按渠道 / by=orderType 按订单类型） */
  distribution(params?: DmsDashboardQuery & { by?: 'channel' | 'orderType' }) {
    return request.get<any, any>('/dms/dashboard/distribution', { params }).then(normDistribution)
  },
}
