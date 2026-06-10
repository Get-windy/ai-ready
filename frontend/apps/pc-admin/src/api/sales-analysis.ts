import request, { type ApiResponse } from '@/utils/request'

// ── 类型定义 ────────────────────────────────────────────

export interface SalesOverview {
  totalAmount: number; orderCount: number; avgOrderAmount: number; grossMargin: number
}

export interface TrendDataPoint {
  month: string; salesAmount: number; orderCount: number; profit?: number
}

export interface ChannelDistribution {
  name: string; value: number
}

export interface CustomerRankItem {
  rank: number; name: string; customerType?: string; orderCount: number; totalAmount: number; growth: number; trend?: number
}

export interface ProductRankItem {
  rank: number; name: string; trend?: number; volume: number; totalAmount: number; margin: number
}

export interface SalespersonRankItem {
  rank: number; name: string; trend?: number; orderCount: number; totalAmount: number; targetRate: number
}

export interface RegionItem {
  name: string; orderCount: number; totalAmount: number; growth: number
}

export interface SalesAnalysisQuery {
  warehouseId?: number
  salespersonId?: string
  customerType?: string
  dateRange?: [string, string]
  current?: number
  size?: number
  compareType?: 'prev_month' | 'prev_year'
}

export interface DrillDownItem {
  productName: string
  quantity: number
  amount: number
  margin: number
}

export interface CompareData {
  currentPeriod: SalesOverview
  comparePeriod: SalesOverview
  changes: {
    totalAmountChange: number
    orderCountChange: number
    avgAmountChange: number
    marginChange: number
  }
}

export type ExportFormat = 'xlsx' | 'csv'

export const salesAnalysisApi = {
  getOverview(params?: SalesAnalysisQuery): Promise<ApiResponse<SalesOverview>> {
    return request.get('/erp/metrics/dashboard', params)
  },
  getTrend(params?: SalesAnalysisQuery): Promise<ApiResponse<TrendDataPoint[]>> {
    return request.get('/erp/metrics/type/sales', params)
  },
  getChannelDistribution(params?: SalesAnalysisQuery): Promise<ApiResponse<ChannelDistribution[]>> {
    return request.get('/erp/monitor/sales', params)
  },
  getCustomerRanking(params?: SalesAnalysisQuery): Promise<ApiResponse<CustomerRankItem[]>> {
    return request.get('/erp/metrics/history', { ...params, type: 'customer_ranking' })
  },
  getProductRanking(params?: SalesAnalysisQuery): Promise<ApiResponse<ProductRankItem[]>> {
    return request.get('/erp/metrics/history', { ...params, type: 'product_ranking' })
  },
  getSalespersonRanking(params?: SalesAnalysisQuery): Promise<ApiResponse<SalespersonRankItem[]>> {
    return request.get('/erp/metrics/history', { ...params, type: 'salesperson_ranking' })
  },
  getWarehouses(): Promise<ApiResponse<{ id: number; name: string }[]>> {
    return request.get('/erp/stock/warehouses')
  },
  getSalespersons(): Promise<ApiResponse<{ id: number; name: string }[]>> {
    return request.get('/erp/basic/salespersons')
  },
  /** 导出报表（Blob 下载） */
  exportReport(params?: SalesAnalysisQuery & { format?: ExportFormat; includeTabs?: string[] }): Promise<Blob> {
    return request.get('/erp/sales-analysis/export', { params, responseType: 'blob' })
  },
  /** 获取下钻数据 */
  getDrillDownData(params?: { salespersonId?: string; productName?: string; customerName?: string } & SalesAnalysisQuery): Promise<ApiResponse<DrillDownItem[]>> {
    return request.get('/erp/sales-analysis/drill-down', params)
  },
  /** 获取对比期数据 */
  getCompareData(params?: SalesAnalysisQuery & { compareType?: string }): Promise<ApiResponse<CompareData>> {
    return request.get('/erp/sales-analysis/compare', params)
  }
}
