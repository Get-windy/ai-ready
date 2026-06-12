/**
 * DMS 仪表盘 API 模块
 */
import request from '@/utils/request'

// ── 仪表盘统计 ──────────────────────────────────────────
export interface DmsDashboardStats {
  totalRiders: number
  activeRiders: number
  offlineRiders: number
  totalVehicles: number
  activeVehicles: number
  todayOrders: number
  pendingOrders: number
  inProgressOrders: number
  completedOrders: number
  alertCount: number
}

export const dashboardApi = {
  /** 获取仪表盘统计数据 */
  stats(params?: { date?: string }) { return request.get('/api/dms/dashboard/stats', { params }) },

  /** 获取活跃绑定（最近 5 条） */
  activeBindings() { return request.get('/api/dms/dashboard/active-bindings') },

  /** 获取待处理预警（最近 5 条） */
  pendingAlerts() { return request.get('/api/dms/dashboard/pending-alerts') },

  /** 获取任务状态汇总 */
  taskSummary() { return request.get('/api/dms/dashboard/task-summary') },
}
