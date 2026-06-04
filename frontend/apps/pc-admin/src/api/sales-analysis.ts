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
  rank: number; name: string; orderCount: number; totalAmount: number; growth: number
}

export interface ProductRankItem {
  rank: number; name: string; volume: number; totalAmount: number; margin: number
}

export interface SalesAnalysisQuery {
  warehouseId?: number
  salespersonId?: string
  dateRange?: [string, string]
  current?: number
  size?: number
}

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
  getSalespersonRanking(params?: SalesAnalysisQuery): Promise<ApiResponse<any[]>> {
    return request.get('/erp/metrics/history', { ...params, type: 'salesperson_ranking' })
  },
  getWarehouses(): Promise<ApiResponse<{ id: number; name: string }[]>> {
    return request.get('/erp/stock/warehouses')
  }
}
