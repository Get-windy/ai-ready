import request, { type ApiResponse, type PageResponse } from '@/utils/request'

/**
 * 订单详情
 */
export interface OrderDetail {
  id: number
  orderId: number
  productId: number
  productName: string
  productCode: string
  quantity: number
  unitPrice: number
  discount: number
  taxRate: number
  taxAmount: number
  totalAmount: number
  remark: string
}

/**
 * 销售订单
 */
export interface SalesOrder {
  id: number
  tenantId: number
  orderNo: string
  customerId: number
  customerName: string
  orderDate: string
  deliveryDate: string
  status: number
  totalAmount: number
  discountAmount: number
  taxAmount: number
  finalAmount: number
  salesperson: string
  salespersonId: number
  remark: string
  createTime: string
  updateTime: string
  details?: OrderDetail[]
}

/**
 * 采购订单
 */
export interface PurchaseOrder {
  id: number
  tenantId: number
  orderNo: string
  supplierId: number
  supplierName: string
  orderDate: string
  deliveryDate: string
  status: number
  totalAmount: number
  purchaser: string
  purchaserId: number
  remark: string
  createTime: string
  updateTime: string
}

/**
 * 订单查询参数
 */
export interface OrderQuery {
  tenantId?: number
  orderNo?: string
  customerId?: number
  supplierId?: number
  status?: number
  startDate?: string
  endDate?: string
  salespersonId?: number
  pageNum?: number
  pageSize?: number
}

/**
 * 订单状态枚举
 */
export enum OrderStatus {
  DRAFT = 0,        // 草稿
  PENDING = 1,      // 待审批
  APPROVED = 2,     // 已审批
  PARTIAL = 3,      // 部分发货
  COMPLETED = 4,    // 完成
  CANCELLED = 5     // 已取消
}

/**
 * 销售订单API
 */
export const salesOrderApi = {
  /**
   * 分页查询销售订单
   */
  getPage(params: OrderQuery): Promise<ApiResponse<PageResponse<SalesOrder>>> {
    return request.get('/erp/sale/order/page', params)
  },

  /**
   * 获取销售订单详情
   */
  getById(id: number): Promise<ApiResponse<SalesOrder>> {
    return request.get(`/erp/sale/order/${id}`)
  },

  /**
   * 创建销售订单
   */
  create(data: Partial<SalesOrder>): Promise<ApiResponse<boolean>> {
    return request.post('/erp/sale/order', data)
  },

  /**
   * 更新销售订单
   */
  update(id: number, data: Partial<SalesOrder>): Promise<ApiResponse<boolean>> {
    return request.put(`/erp/sale/order/${id}`, data)
  },

  /**
   * 删除销售订单
   */
  delete(id: number): Promise<ApiResponse<boolean>> {
    return request.delete(`/erp/sale/order/${id}`)
  },

  /**
   * 批量删除销售订单
   */
  batchDelete(ids: number[]): Promise<ApiResponse<boolean>> {
    return request.delete('/erp/sale/order/batch', { data: ids })
  },

  /**
   * 提交订单审批
   */
  submit(id: number): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/sale/order/${id}/submit`)
  },

  /**
   * 审批订单
   */
  approve(id: number): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/sale/order/${id}/approve`)
  },

  /**
   * 审批拒绝
   */
  reject(id: number, reason: string): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/sale/order/${id}/reject`, null, { params: { reason } })
  },

  /**
   * 取消订单
   */
  cancel(id: number, reason?: string): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/sale/order/${id}/cancel`, null, { params: { reason } })
  },

  /**
   * 确认出库
   */
  confirmShipment(id: number, warehouseId: number): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/sale/order/${id}/ship`, null, { params: { warehouseId } })
  },

  /**
   * 记录收款
   */
  recordPayment(id: number, amount: number): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/sale/order/${id}/payment`, null, { params: { amount } })
  },

  /**
   * 获取待审批订单列表
   */
  getPending(tenantId: number): Promise<ApiResponse<SalesOrder[]>> {
    return request.get('/erp/sale/order/pending', { tenantId })
  },

  /**
   * 导出销售订单（返回 JSON 数据，前端生成 CSV）
   */
  export(params: OrderQuery): Promise<ApiResponse<SalesOrder[]>> {
    return request.get('/erp/sale/order/export', params)
  },

  /**
   * 获取订单统计
   */
  getStatistics(params: { startDate?: string; endDate?: string }): Promise<ApiResponse<any>> {
    return request.get('/erp/sale/order/statistics', params)
  }
}

/**
 * 采购订单API
 */
export const purchaseOrderApi = {
  /**
   * 分页查询采购订单
   */
  getPage(params: OrderQuery): Promise<ApiResponse<PageResponse<PurchaseOrder>>> {
    return request.get('/erp/purchase/order/page', params)
  },

  /**
   * 获取采购订单详情
   */
  getById(id: number): Promise<ApiResponse<PurchaseOrder>> {
    return request.get(`/erp/purchase/order/${id}`)
  },

  /**
   * 创建采购订单
   */
  create(data: Partial<PurchaseOrder>): Promise<ApiResponse<boolean>> {
    return request.post('/erp/purchase/order', data)
  },

  /**
   * 更新采购订单
   */
  update(id: number, data: Partial<PurchaseOrder>): Promise<ApiResponse<boolean>> {
    return request.put(`/erp/purchase/order/${id}`, data)
  },

  /**
   * 删除采购订单
   */
  delete(id: number): Promise<ApiResponse<boolean>> {
    return request.delete(`/erp/purchase/order/${id}`)
  },

  /**
   * 批量删除采购订单
   */
  batchDelete(ids: number[]): Promise<ApiResponse<boolean>> {
    return request.delete('/erp/purchase/order/batch', { data: ids })
  },

  /**
   * 提交订单审批
   */
  submit(id: number): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/purchase/order/${id}/submit`)
  },

  /**
   * 审批订单
   */
  approve(id: number, approved: boolean, comment?: string): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/purchase/order/${id}/approve`, null, {
      params: { approved, comment }
    })
  },

  /**
   * 取消订单
   */
  cancel(id: number, reason?: string): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/purchase/order/${id}/cancel`, null, { params: { reason } })
  },

  /**
   * 导出采购订单
   */
  export(params: OrderQuery): Promise<Blob> {
    return request.get('/erp/purchase/order/export', params, { responseType: 'blob' })
  }
}

export default salesOrderApi