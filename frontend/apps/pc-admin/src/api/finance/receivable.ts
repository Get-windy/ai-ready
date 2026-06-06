/**
 * 应收账款 API（v1 版本）
 * 对接后端 core-api ReceivableController
 */
import request, { type ApiResponse, type PageResponse } from '@/utils/request'

/** 应收账款 */
export interface ReceivableItem {
  id: number
  customerName: string
  orderNo: string
  amount: number
  paidAmount: number
  unpaidAmount: number
  status: number
  dueDate: string
  remark: string
}

/** 应收账款查询参数 */
export interface ReceivableQuery {
  customerName?: string
  orderNo?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

/** 应收账款统计 */
export interface ReceivableStats {
  totalAmount: number
  paidAmount: number
  unpaidAmount: number
}

export const receivableV1Api = {
  /** 分页查询应收账款 */
  getPage(params: ReceivableQuery): Promise<ApiResponse<PageResponse<ReceivableItem>>> {
    return request.get('/v1/finance/receivables/page', params)
  },

  /** 获取应收账款详情 */
  getById(id: number): Promise<ApiResponse<ReceivableItem>> {
    return request.get(`/v1/finance/receivables/${id}`)
  },

  /** 创建应收账款 */
  create(data: Partial<ReceivableItem>): Promise<ApiResponse<number>> {
    return request.post('/v1/finance/receivables', data)
  },

  /** 更新应收账款 */
  update(data: Partial<ReceivableItem> & { id: number }): Promise<ApiResponse<void>> {
    return request.put('/v1/finance/receivables', data)
  },

  /** 删除应收账款 */
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/v1/finance/receivables/${id}`)
  },

  /** 批量删除 */
  batchDelete(ids: number[]): Promise<ApiResponse<void>> {
    return request.delete('/v1/finance/receivables/batch', { data: ids })
  },

  /** 收款操作 */
  receivePayment(data: { receivableId: number; amount: number; paymentMethod: string }): Promise<ApiResponse<void>> {
    return request.post('/v1/finance/receivables/receive-payment', data)
  },

  /** 账龄分析 */
  getAgingAnalysis(): Promise<ApiResponse<any[]>> {
    return request.get('/v1/finance/receivables/aging-analysis')
  },

  /** 根据客户查询应收账款 */
  getByCustomer(customerId: number): Promise<ApiResponse<ReceivableItem[]>> {
    return request.get(`/v1/finance/receivables/customer/${customerId}`)
  },

  /** 导出应收账款 */
  export(params: ReceivableQuery): Promise<Blob> {
    return request.get('/v1/finance/receivables/export', params, { responseType: 'blob' })
  }
}
