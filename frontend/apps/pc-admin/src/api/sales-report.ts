import request, { type ApiResponse } from '@/utils/request'

export interface SalesStats {
  totalSales: number; orderCount: number; avgOrderValue: number; returnAmount: number
}
export interface CustomerRankItem { rank: number; name: string; orderCount: number; totalAmount: number; growth: number }
export interface ProductRankItem { rank: number; name: string; volume: number; totalAmount: number; margin: number }
export interface TrendData { date: string; sales: number; orders: number }

export const salesReportApi = {
  getStatistics(params?: Record<string, any>): Promise<ApiResponse<SalesStats>> {
    return request.get('/erp/metrics/dashboard', { params })
  },
  getCustomerRanking(params?: Record<string, any>): Promise<ApiResponse<CustomerRankItem[]>> {
    return request.get('/erp/metrics/history', { params: { ...params, type: 'customer_ranking' } })
  },
  getProductRanking(params?: Record<string, any>): Promise<ApiResponse<ProductRankItem[]>> {
    return request.get('/erp/metrics/history', { params: { ...params, type: 'product_ranking' } })
  },
  getSalesTrend(params?: Record<string, any>): Promise<ApiResponse<TrendData[]>> {
    return request.get('/erp/metrics/type/sales', { params })
  }
}
