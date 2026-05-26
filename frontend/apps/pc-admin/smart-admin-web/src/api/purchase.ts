import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// 采购订单类型
export interface PurchaseOrder {
  id: number
  tenantId: number
  orderNo: string
  supplierId: number
  supplierName: string
  orderDate: string
  expectedDate: string
  status: number
  totalAmount: number
  taxAmount: number
  totalAmountWithTax: number
  receivedAmount: number
  purchaserId: number
  purchaserName: string
  warehouseId: number
  remark: string
  createTime: string
  updateTime: string
}

// 查询参数
export interface PurchaseOrderQuery {
  current: number
  size: number
  tenantId: number
  orderNo?: string
  supplierId?: number
  status?: number
}

// API接口
export const purchaseOrderApi = {
  // 分页查询
  page(params: PurchaseOrderQuery): Promise<ApiResponse<PageResponse<PurchaseOrder>>> {
    return request.get('/erp/purchase/order/page', params)
  },

  // 获取详情
  get(id: number): Promise<ApiResponse<PurchaseOrder>> {
    return request.get(`/erp/purchase/order/${id}`)
  },

  // 创建
  create(data: Partial<PurchaseOrder>): Promise<ApiResponse<number>> {
    return request.post('/erp/purchase/order', data)
  },

  // 更新
  update(id: number, data: Partial<PurchaseOrder>): Promise<ApiResponse<void>> {
    return request.put(`/erp/purchase/order/${id}`, data)
  },

  // 删除
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/purchase/order/${id}`)
  },

  // 提交审批
  submit(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/purchase/order/${id}/submit`)
  },

  // 审批通过
  approve(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/purchase/order/${id}/approve`)
  },

  // 审批拒绝
  reject(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.post(`/erp/purchase/order/${id}/reject`, null, { params: { reason } })
  },

  // 取消订单
  cancel(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.post(`/erp/purchase/order/${id}/cancel`, null, { params: { reason } })
  }
}