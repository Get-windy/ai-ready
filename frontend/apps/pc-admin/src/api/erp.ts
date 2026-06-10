/**
 * ERP 统一 API 模块
 * 涵盖采购/销售/库存各子模块的 API 接口
 */
import request, { type ApiResponse } from '@/utils/request'

// ── 通用类型 ──────────────────────────────────────────
export interface PageQuery {
  tenantId?: number
  pageNum?: number
  pageSize?: number
  keyword?: string
  status?: number
  [key: string]: any
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

// ── 采购询价 ──────────────────────────────────────────
export interface PurchaseInquiry {
  id: number; inquiryNo: string; supplierId: number; supplierName: string
  inquiryDate: string; status: number; creatorName?: string; createTime: string
}
export const inquiryApi = {
  page(params: PageQuery): Promise<PageResult<PurchaseInquiry>> {
    return request.get('/erp/purchase/inquiry/page', params)
  },
  getById(id: number) { return request.get(`/erp/purchase/inquiry/${id}`) },
  create(data: any) { return request.post('/erp/purchase/inquiry', data) },
  update(id: number, data: any) { return request.put(`/erp/purchase/inquiry/${id}`, data) },
  delete(id: number) { return request.delete(`/erp/purchase/inquiry/${id}`) },
  send(id: number) { return request.post(`/erp/purchase/inquiry/${id}/send`) },
}

// ── 采购入库 ──────────────────────────────────────────
export interface PurchaseInbound {
  id: number; inboundNo: string; orderNo?: string; supplierName: string
  inboundDate: string; status: number; totalAmount?: number; creatorName?: string; createTime: string
}
export const inboundApi = {
  page(params: PageQuery): Promise<PageResult<PurchaseInbound>> {
    return request.get('/erp/purchase/inbound/page', params)
  },
  getById(id: number) { return request.get(`/erp/purchase/inbound/${id}`) },
  create(data: any) { return request.post('/erp/purchase/inbound', data) },
  delete(id: number) { return request.delete(`/erp/purchase/inbound/${id}`) },
  approve(id: number) { return request.post(`/erp/purchase/inbound/${id}/approve`) },
  confirmWarehouse(id: number) { return request.post(`/erp/purchase/inbound/${id}/warehouse-confirm`) },
}

// ── 采购退货 ──────────────────────────────────────────
export interface PurchaseReturn {
  id: number; returnNo: string; orderNo?: string; supplierName: string
  returnDate: string; totalAmount?: number; status: number; creatorName?: string; createTime: string
}
export const purchaseReturnApi = {
  page(params: PageQuery): Promise<PageResult<PurchaseReturn>> {
    return request.get('/erp/purchase/return/page', params)
  },
  getById(id: number) { return request.get(`/erp/purchase/return/${id}`) },
  create(data: any) { return request.post('/erp/purchase/return', data) },
  delete(id: number) { return request.delete(`/erp/purchase/return/${id}`) },
  approve(id: number) { return request.post(`/erp/purchase/return/${id}/approve`) },
  reject(id: number, reason?: string) { return request.post(`/erp/purchase/return/${id}/reject`, null, { params: { reason } }) },
}

// ── 付款管理 ──────────────────────────────────────────
export interface Payment {
  id: number; paymentNo: string; orderNo?: string; supplierId?: number
  supplierName: string; paymentDate: string; paymentAmount: number
  paymentMethod: number; status: number; creatorName?: string; createTime: string
}
export const paymentApi = {
  page(params: PageQuery): Promise<PageResult<Payment>> {
    return request.get('/erp/payment/page', params)
  },
  getById(id: number) { return request.get(`/erp/payment/${id}`) },
  create(data: any) { return request.post('/erp/payment', data) },
  delete(id: number) { return request.delete(`/erp/payment/${id}`) },
  approve(id: number) { return request.post(`/erp/payment/${id}/approve`) },
}

// ── 销售出库 ──────────────────────────────────────────
export interface SaleOutbound {
  id: number; outboundNo: string; orderNo?: string; customerName: string
  outboundDate: string; status: number; totalAmount?: number; createTime: string
}
export const outboundApi = {
  page(params: PageQuery): Promise<PageResult<SaleOutbound>> {
    return request.get('/erp/sale/outbound/page', params)
  },
  getById(id: number) { return request.get(`/erp/sale/outbound/${id}`) },
  create(data: any) { return request.post('/erp/sale/outbound', data) },
  approve(id: number) { return request.post(`/erp/sale/outbound/${id}/approve`) },
}

// ── 销售退货 ──────────────────────────────────────────
export interface SaleReturn {
  id: number; returnNo: string; orderNo?: string; customerName: string
  returnDate: string; totalAmount?: number; status: number; createTime: string
}
export const saleReturnApi = {
  page(params: PageQuery): Promise<PageResult<SaleReturn>> {
    return request.get('/erp/sale/return/page', params)
  },
  getById(id: number) { return request.get(`/erp/sale/return/${id}`) },
  create(data: any) { return request.post('/erp/sale/return', data) },
  delete(id: number) { return request.delete(`/erp/sale/return/${id}`) },
  approve(id: number) { return request.post(`/erp/sale/return/${id}/approve`) },
  receive(id: number) { return request.post(`/erp/sale/return/${id}/receive`) },
  refund(id: number) { return request.post(`/erp/sale/return/${id}/refund`) },
}

// ── 销售收款 ──────────────────────────────────────────
export interface SaleReceipt {
  id: number; receiptNo: string; orderNo?: string; customerName: string
  receiptDate: string; receiptAmount: number; receiptMethod: number
  status: number; creatorName?: string; createTime: string
}
export const receiptApi = {
  page(params: PageQuery): Promise<PageResult<SaleReceipt>> {
    return request.get('/erp/receipt/page', params)
  },
  getById(id: number) { return request.get(`/erp/receipt/${id}`) },
  create(data: any) { return request.post('/erp/receipt', data) },
  delete(id: number) { return request.delete(`/erp/receipt/${id}`) },
  approve(id: number) { return request.post(`/erp/receipt/${id}/approve`) },
}

// ── 销售报价 ──────────────────────────────────────────
export interface SaleQuotation {
  id: number; quotationNo: string; customerName: string
  quotationDate: string; validDate?: string; totalAmount?: number
  status: number; creatorName?: string; createTime: string
}
export const quotationApi = {
  page(params: PageQuery): Promise<PageResult<SaleQuotation>> {
    return request.get('/crm/quotation/page', params)
  },
  getById(id: number) { return request.get(`/crm/quotation/${id}`) },
  create(data: any) { return request.post('/crm/quotation', data) },
  update(id: number, data: any) { return request.put(`/crm/quotation/${id}`, data) },
  delete(id: number) { return request.delete(`/crm/quotation/${id}`) },
  send(id: number) { return request.post(`/crm/quotation/${id}/send`) },
  convertToOrder(id: number) { return request.post(`/crm/quotation/${id}/convert-to-order`) },
}

// ── 销售换货 ──────────────────────────────────────────
export interface SaleExchange {
  id: number; exchangeNo: string; orderNo?: string; customerName: string
  exchangeDate: string; status: number; creatorName?: string; createTime: string
}
export const saleExchangeApi = {
  page(params: PageQuery): Promise<PageResult<SaleExchange>> {
    return request.get('/erp/sale/exchange/page', params)
  },
  getById(id: number) { return request.get(`/erp/sale/exchange/${id}`) },
  create(data: any) { return request.post('/erp/sale/exchange', data) },
  update(id: number, data: any) { return request.put(`/erp/sale/exchange/${id}`, data) },
  delete(id: number) { return request.delete(`/erp/sale/exchange/${id}`) },
  submit(id: number) { return request.post(`/erp/sale/exchange/${id}/submit`) },
  approve(id: number) { return request.post(`/erp/sale/exchange/${id}/approve`) },
  batchApprove(ids: number[]) { return request.post('/erp/sale/exchange/batch-approve', ids) },
}

// ── 库存管理 ──────────────────────────────────────────
export interface StockItem {
  id: number; productCode: string; productName: string; specification?: string
  unit?: string; warehouseName?: string; quantity: number
  availableQuantity?: number; lockedQuantity?: number; frozenQuantity?: number
  minStock?: number; maxStock?: number
  lastInboundDate?: string; lastOutboundDate?: string; createTime?: string
}
export const stockApi = {
  page(params: PageQuery): Promise<PageResult<StockItem>> {
    return request.get('/erp/stock/page', params)
  },
  getById(id: number) { return request.get(`/erp/stock/${id}`) },
  export(params?: any) { return request.get('/erp/stock/export', params) },
  getWarehouses(): Promise<any> { return request.get('/erp/stock/warehouses') },
}

// ── 库存盘点 ──────────────────────────────────────────
export interface StockCheck {
  id: number; checkNo: string; warehouseName: string; checkDate: string
  status: number; creatorName?: string; createTime: string
}
export const stockCheckApi = {
  page(params: PageQuery): Promise<PageResult<StockCheck>> {
    return request.get('/erp/stock/check/page', params)
  },
  getById(id: number) { return request.get(`/erp/stock/check/${id}`) },
  create(data: any) { return request.post('/erp/stock/check', data) },
  getItems(checkId: number): Promise<ApiResponse<StockCheckItem[]>> {
    return request.get(`/erp/stock/check/${checkId}/items`)
  },
  createWithItems(warehouseId: number): Promise<ApiResponse<StockCheck>> {
    return request.post(`/erp/stock/check/create-with-items/${warehouseId}`)
  },
  startCheck(id: number): Promise<ApiResponse<StockCheck>> {
    return request.post(`/erp/stock/check/${id}/start`)
  },
  checkItem(id: number, itemId: number, actualQuantity: number, note?: string): Promise<ApiResponse<StockCheckItem>> {
    return request.post(`/erp/stock/check/${id}/items/${itemId}/check`, null, { params: { actualQuantity, note } })
  },
  completeCheck(id: number): Promise<ApiResponse<StockCheck>> {
    return request.post(`/erp/stock/check/${id}/complete`)
  },
  submitForApproval(id: number): Promise<ApiResponse<StockCheck>> {
    return request.post(`/erp/stock/check/${id}/submit`)
  },
  cancel(id: number, reason: string): Promise<ApiResponse<StockCheck>> {
    return request.post(`/erp/stock/check/${id}/cancel`, null, { params: { reason } })
  },
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/stock/check/${id}`)
  },
}

export interface StockCheckItem {
  id: number
  productId: number
  productCode: string
  productName: string
  productSpec?: string
  productUnit?: string
  bookQuantity: number
  actualQuantity: number | null
  diffQuantity: number
  checkStatus?: number
}

// ── 库存调拨 ──────────────────────────────────────────
export interface StockTransfer {
  id: number; transferNo: string; fromWarehouse: string; toWarehouse: string
  transferDate: string; status: number; creatorName?: string; createTime: string
}
export const stockTransferApi = {
  page(params: PageQuery): Promise<PageResult<StockTransfer>> {
    return request.get('/erp/stock/transfer/page', params)
  },
  getById(id: number) { return request.get(`/erp/stock/transfer/${id}`) },
  create(data: any) { return request.post('/erp/stock/transfer', data) },
  approve(id: number) { return request.post(`/erp/stock/transfer/${id}/approve`) },
}

// ── 批次管理 ──────────────────────────────────────────
export interface BatchItem {
  id: number; batchNo: string; productCode: string; productName: string
  productionDate?: string; expiryDate: string; status: number
  quantity?: number; warehouseName?: string
}
export const batchApi = {
  page(params: PageQuery): Promise<PageResult<BatchItem>> {
    return request.get('/erp/batch-sn/batches/page', params)
  },
  getById(id: number) { return request.get(`/erp/batch-sn/batches/${id}`) },
}

// ── 智能补货 ──────────────────────────────────────────

export interface ReplenishmentSuggestion {
  id: number; productCode: string; productName: string
  currentQty: number; safetyStock: number; shortageQty: number
  avgDailySales: number; daysOfStock: number; leadTime: number
  suggestedQty: number; priority: number; reason: string; status?: string
}
export const replenishmentApi = {
  list(params: Record<string, any>): Promise<PageResult<ReplenishmentSuggestion>> {
    return request.get('/erp/stock/replenishment/list', params)
  },
  generate(): Promise<ApiResponse<void>> {
    return request.post('/erp/stock/replenishment/generate')
  },
  createOrder(suggestionId: number, supplierId: number): Promise<ApiResponse<any>> {
    return request.post(`/erp/stock/replenishment/${suggestionId}/create-order`, { supplierId })
  },
  ignore(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.put(`/erp/stock/replenishment/${id}/ignore`, null, { params: { reason } })
  }
}

// ── 销售订单 ──────────────────────────────────────────
export interface SaleOrder {
  id: number; orderNo: string; customerName: string; orderDate: string
  totalAmount: number; totalAmountWithTax: number; status: number
  salesmanName: string; remark?: string; createTime: string; updateTime?: string
}

/** 销售首页统计 */
export interface SaleStats {
  monthOrderCount: number
  monthAmount: number
  pendingCount: number
}

export const saleStatsApi = {
  get(): Promise<ApiResponse<SaleStats>> {
    return request.get('/erp/sale/order/stats')
  }
}

export const saleOrderApi = {
  getPage(params: any): Promise<PageResult<SaleOrder>> {
    return request.get('/erp/sale/order/page', params)
  },
  getById(id: number) { return request.get(`/erp/sale/order/${id}`) },
  create(data: any) { return request.post('/erp/sale/order', data) },
  update(id: number, data: any) { return request.put(`/erp/sale/order/${id}`, data) },
  delete(id: number) { return request.delete(`/erp/sale/order/${id}`) },
  batchDelete(ids: number[]) { return request.delete('/erp/sale/order/batch', { data: ids }) },
  submit(id: number) { return request.post(`/erp/sale/order/${id}/submit`) },
  approve(id: number) { return request.post(`/erp/sale/order/${id}/approve`) },
  batchApprove(ids: number[]) { return request.post('/erp/sale/order/batch-approve', ids) },
  print(id: number) { return request.get(`/erp/sale/order/${id}/print`) },
  batchPrint(ids: number[]) { return request.post('/erp/sale/order/batch-print', ids) },
  export(params: any) { return request.get('/erp/sale/order/export', params) },
}

// ── 采购订单 ──────────────────────────────────────────
export interface PurchaseOrder {
  id: number; orderNo: string; supplierName: string; orderDate: string
  totalAmount: number; totalAmountWithTax: number; status: number
  buyerName: string; remark?: string; createTime: string; updateTime?: string
}
export const purchaseOrderApi = {
  getPage(params: any): Promise<PageResult<PurchaseOrder>> {
    return request.get('/erp/purchase/order/page', params)
  },
  getById(id: number) { return request.get(`/erp/purchase/order/${id}`) },
  create(data: any) { return request.post('/erp/purchase/order', data) },
  update(id: number, data: any) { return request.put(`/erp/purchase/order/${id}`, data) },
  delete(id: number) { return request.delete(`/erp/purchase/order/${id}`) },
  batchDelete(ids: number[]) { return request.delete('/erp/purchase/order/batch', { data: ids }) },
  submit(id: number) { return request.post(`/erp/purchase/order/${id}/submit`) },
  approve(id: number) { return request.post(`/erp/purchase/order/${id}/approve`) },
  batchApprove(ids: number[]) { return request.post('/erp/purchase/order/batch-approve', ids) },
  close(id: number) { return request.post(`/erp/purchase/order/${id}/close`) },
  print(id: number) { return request.get(`/erp/purchase/order/${id}/print`) },
  export(params: any) { return request.get('/erp/purchase/order/export', params) },
}

/** 采购首页统计 */
export interface PurchaseStats {
  totalOrders?: number
  totalAmount?: number
  monthOrderCount?: number
  pendingInquiryCount?: number
  pendingInboundCount?: number
  pendingPaymentCount?: number
  [key: string]: any
}

export const purchaseStatsApi = {
  get(tenantId?: number): Promise<ApiResponse<PurchaseStats>> {
    return request.get('/erp/purchase/order/stats', { tenantId: tenantId || 1 })
  }
}
