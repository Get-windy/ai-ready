import request, { type ApiResponse } from '@/utils/request'

// ── 基础数据类型 ──────────────────────────────────────────

export interface SalesStats {
  totalSales: number
  orderCount: number
  avgOrderValue: number
  returnAmount: number
}

export interface CustomerRankItem {
  rank: number
  name: string
  orderCount: number
  totalAmount: number
  growth: number
  customerType?: string
}

export interface ProductRankItem {
  rank: number
  name: string
  code?: string
  volume: number
  totalAmount: number
  margin: number
}

export interface TrendData {
  date: string
  sales: number
  orders: number
}

// ── 增强类型定义 ──────────────────────────────────────────

/** 报表摘要数据 */
export interface ReportSummaryData {
  monthAmount: number
  monthOrders: number
  activeCustomers: number
  growthRate: number
  monthAmountGrowth?: number
  monthOrdersGrowth?: number
  activeCustomersGrowth?: number
  // 上期值（环比用）
  prevMonthAmount?: number
  prevMonthOrders?: number
  prevActiveCustomers?: number
  // 同比数据
  yoyAmount?: number
  yoyOrders?: number
  yoyCustomers?: number
  // 环比数据
  momAmount?: number
  momOrders?: number
  momCustomers?: number
  // 迷你趋势数据
  amountSparkline?: number[]
  ordersSparkline?: number[]
}

/** 导出文件格式 */
export type ExportFileFormat = 'csv' | 'xlsx' | 'pdf'

/** 导出配置 */
export interface ExportOptions {
  format: ExportFileFormat
  dateRange?: [string, string]
  scope: 'current' | 'all'
  includeDetail: boolean
}

/** 查询参数 */
export interface SalesReportQuery {
  dateRange?: [string, string]
  keyword?: string
  startDate?: string
  endDate?: string
  cardFilter?: string
  compareMode?: CompareMode
  displayMode?: DisplayMode
}

/** 比较模式 */
export type CompareMode = 'mom' | 'yoy'

/** 显示模式 */
export type DisplayMode = 'absolute' | 'growth'

/** 各分区错误状态 */
export interface SectionErrors {
  summary: boolean
  statistics: boolean
  customer: boolean
  product: boolean
  trend: boolean
  [key: string]: boolean
}

/** 卡片筛选键 */
export type CardFilterKey = 'amount' | 'orders' | 'customers' | 'growth' | null

// ── API 方法 ──────────────────────────────────────────────

export const salesReportApi = {
  getStatistics(params?: Record<string, unknown>): Promise<ApiResponse<SalesStats>> {
    return request.get('/erp/metrics/dashboard', params)
  },
  getCustomerRanking(params?: Record<string, unknown>): Promise<ApiResponse<CustomerRankItem[]>> {
    return request.get('/erp/metrics/history', { ...params, type: 'customer_ranking' })
  },
  getProductRanking(params?: Record<string, unknown>): Promise<ApiResponse<ProductRankItem[]>> {
    return request.get('/erp/metrics/history', { ...params, type: 'product_ranking' })
  },
  getSalesTrend(params?: Record<string, unknown>): Promise<ApiResponse<TrendData[]>> {
    return request.get('/erp/metrics/type/sales', params)
  },
  /**
   * 导出报表数据（服务端导出，可能不支持所有格式）
   * 如果服务端导出不支持，则回退到客户端导出
   */
  exportReport(params?: Record<string, unknown>): Promise<Blob> {
    return request.get('/erp/report/export', params, { responseType: 'blob' })
  }
}
