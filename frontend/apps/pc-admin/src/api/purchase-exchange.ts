import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// 换货单状态枚举
export enum ExchangeStatus {
  DRAFT = 0,           // 草稿
  PENDING_APPROVAL = 1, // 待审批
  APPROVED = 2,        // 已审批
  EXCHANGING = 3,      // 换货中
  COMPLETED = 4,       // 已完成
  REJECTED = 5,        // 已拒绝
  CANCELLED = 6        // 已取消
}

// 换货单类型
export interface PurchaseExchange {
  id: number
  tenantId: number
  exchangeNo: string                    // 换货单号
  originalOrderId: number              // 原采购订单ID
  originalOrderNo: string                // 原采购订单号
  supplierId: number                   // 供应商ID
  supplierName: string                 // 供应商名称
  exchangeDate: string                 // 换货日期
  exchangeReason: string               // 换货原因
  exchangeType: number                 // 换货类型：1-质量问题 2-规格不符 3-数量错误 4-其他
  status: ExchangeStatus               // 状态
  remark: string                       // 备注
  totalAmount: number                  // 换货总金额
  createdBy: number                    // 创建人ID
  createdByName: string                // 创建人姓名
  approvedBy: number                   // 审批人ID
  approvedByName: string               // 审批人姓名
  approvedTime: string                 // 审批时间
  completedTime: string                // 完成时间
  createTime: string                   // 创建时间
  updateTime: string                   // 更新时间
}

// 换货单明细
export interface PurchaseExchangeItem {
  id: number
  exchangeId: number                   // 换货单ID
  originalItemId: number              // 原订单明细ID
  productId: number                    // 商品ID
  productName: string                  // 商品名称
  productCode: string                  // 商品编码
  productSpec: string                  // 规格
  originalQuantity: number             // 原数量
  exchangeQuantity: number             // 换货数量
  originalPrice: number                // 原单价
  exchangePrice: number                // 换货单价
  unit: string                         // 单位
  batchNo: string                      // 批次号
  warehouseId: number                  // 仓库ID
  warehouseName: string                // 仓库名称
  remark: string                       // 备注
}

// 换货审批记录
export interface ExchangeApprovalRecord {
  id: number
  exchangeId: number                   // 换货单ID
  action: string                       // 操作：submit/approve/reject/cancel
  actionName: string                   // 操作名称
  operatorId: number                   // 操作人ID
  operatorName: string                 // 操作人姓名
  remark: string                       // 备注
  createTime: string                   // 操作时间
}

// 换货单查询参数
export interface PurchaseExchangeQuery {
  current: number
  size: number
  tenantId?: number
  exchangeNo?: string                  // 换货单号
  originalOrderNo?: string               // 原采购订单号
  supplierId?: number                  // 供应商ID
  status?: ExchangeStatus              // 状态
  exchangeType?: number                  // 换货类型
  startDate?: string                   // 开始日期
  endDate?: string                     // 结束日期
}

// 创建换货单请求
export interface CreateExchangeRequest {
  originalOrderId: number
  exchangeDate: string
  exchangeReason: string
  exchangeType: number
  remark?: string
  items: CreateExchangeItemRequest[]
}

// 创建换货单明细请求
export interface CreateExchangeItemRequest {
  originalItemId: number
  productId: number
  exchangeQuantity: number
  exchangePrice: number
  warehouseId: number
  remark?: string
}

// 审批换货单请求
export interface ApproveExchangeRequest {
  approved: boolean                    // 是否通过
  remark?: string                      // 审批意见
}

// API接口
export const purchaseExchangeApi = {
  // 分页查询换货单
  page(params: PurchaseExchangeQuery): Promise<ApiResponse<PageResponse<PurchaseExchange>>> {
    return request.get('/erp/purchase/exchange/page', { params } as any)
  },

  // 获取换货单详情
  get(id: number): Promise<ApiResponse<PurchaseExchange>> {
    return request.get(`/erp/purchase/exchange/${id}`)
  },

  // 获取换货单明细
  getItems(exchangeId: number): Promise<ApiResponse<PurchaseExchangeItem[]>> {
    return request.get(`/erp/purchase/exchange/${exchangeId}/items`)
  },

  // 创建换货单
  create(data: CreateExchangeRequest): Promise<ApiResponse<number>> {
    return request.post('/erp/purchase/exchange', data)
  },

  // 更新换货单
  update(id: number, data: Partial<CreateExchangeRequest>): Promise<ApiResponse<void>> {
    return request.put(`/erp/purchase/exchange/${id}`, data)
  },

  // 删除换货单
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/purchase/exchange/${id}`)
  },

  // 提交换货单审批
  submit(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/purchase/exchange/${id}/submit`)
  },

  // 审批换货单
  approve(id: number, data: ApproveExchangeRequest): Promise<ApiResponse<void>> {
    return request.post(`/erp/purchase/exchange/${id}/approve`, data)
  },

  // 拒绝换货单
  reject(id: number, remark: string): Promise<ApiResponse<void>> {
    return request.post(`/erp/purchase/exchange/${id}/reject`, { remark })
  },

  // 取消换货单
  cancel(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.post(`/erp/purchase/exchange/${id}/cancel`, { reason })
  },

  // 完成换货单
  complete(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/purchase/exchange/${id}/complete`)
  },

  // 获取审批记录
  getApprovalRecords(exchangeId: number): Promise<ApiResponse<ExchangeApprovalRecord[]>> {
    return request.get(`/erp/purchase/exchange/${exchangeId}/approval-records`)
  },

  // 获取换货单跟踪信息
  getTracking(exchangeId: number): Promise<ApiResponse<{
    exchange: PurchaseExchange
    items: PurchaseExchangeItem[]
    approvalRecords: ExchangeApprovalRecord[]
    timeline: Array<{
      time: string
      title: string
      content: string
      status: 'success' | 'processing' | 'pending' | 'error'
    }>
  }>> {
    return request.get(`/erp/purchase/exchange/${exchangeId}/tracking`)
  },

  // 导出换货单
  async export(params: Omit<PurchaseExchangeQuery, 'current' | 'size'>): Promise<Blob> {
    const res = await request.get('/erp/purchase/exchange/export', { params, responseType: 'blob' } as any)
    return (res as any).data as Blob
  }
}

export default purchaseExchangeApi
