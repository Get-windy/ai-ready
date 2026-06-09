import request, { type ApiResponse } from '@/utils/request'

// ── 类型定义 ────────────────────────────────────────────

/** KPI 统计数据 */
export interface DashboardStats {
  todaySales: { value: number; trend: number; trendType: 'up' | 'down' | 'warn' }
  todayPurchase: { value: number; trend: number; trendType: 'up' | 'down' | 'warn' }
  pendingApprovals: { value: number; trendType: 'up' | 'down' | 'warn' }
  stockAlerts: { value: number; trend: number; trendType: 'up' | 'down' | 'warn' }
}

/** 趋势图数据 */
export interface TrendChartData {
  categories: string[]
  series: TrendSeries[]
}

export interface TrendSeries {
  name: string
  data: number[]
}

/** 待办事项 */
export interface TodoItem {
  id: number
  title: string
  time: string
  type: 'approval' | 'alert' | 'info'
}

/** 库存预警 */
export interface StockAlertItem {
  id: number
  code: string
  name: string
  current: number
  safe: number
  level: 'high' | 'low'
}

// ── API 方法 ────────────────────────────────────────────

export const dashboardApi = {
  /** 获取仪表盘 KPI 统计数据 */
  getStats(params?: { startDate?: string; endDate?: string }): Promise<ApiResponse<DashboardStats>> {
    return request.get('/dashboard/stats', { params })
  },

  /** 获取销售趋势图数据 */
  getTrend(params?: { startDate?: string; endDate?: string }): Promise<ApiResponse<TrendChartData>> {
    return request.get('/dashboard/trend', { params })
  },

  /** 获取待办事项列表 */
  getTodos(): Promise<ApiResponse<TodoItem[]>> {
    return request.get('/dashboard/todos')
  },

  /** 获取库存预警列表 */
  getAlerts(): Promise<ApiResponse<StockAlertItem[]>> {
    return request.get('/dashboard/alerts')
  }
}
