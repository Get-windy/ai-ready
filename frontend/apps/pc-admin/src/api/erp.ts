/**
 * ERP 统一 API 模块
 * 涵盖采购/销售/库存各子模块的 API 接口
 */
import request from '@/utils/request'

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
    return request.get('/erp/purchase/inquiry/page', { params })
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
    return request.get('/erp/purchase/inbound/page', { params })
  },
  getById(id: number) { return request.get(`/erp/purchase/inbound/${id}`) },
  create(data: any) { return request.post('/erp/purchase/inbound', data) },
  delete(id: number) { return request.delete(`/erp/purchase/inbound/${id}`) },
  approve(id: number) { return request.post(`/erp/purchase/inbound/${id}/approve`) },
}

// ── 采购退货 ──────────────────────────────────────────
export interface PurchaseReturn {
  id: number; returnNo: string; orderNo?: string; supplierName: string
  returnDate: string; totalAmount?: number; status: number; creatorName?: string; createTime: string
}
export const purchaseReturnApi = {
  page(params: PageQuery): Promise<PageResult<PurchaseReturn>> {
    return request.get('/erp/purchase/return/page', { params })
  },
  getById(id: number) { return request.get(`/erp/purchase/return/${id}`) },
  create(data: any) { return request.post('/erp/purchase/return', data) },
  delete(id: number) { return request.delete(`/erp/purchase/return/${id}`) },
}

// ── 付款管理 ──────────────────────────────────────────
export interface Payment {
  id: number; paymentNo: string; orderNo?: string; supplierId?: number
  supplierName: string; paymentDate: string; paymentAmount: number
  paymentMethod: number; status: number; creatorName?: string; createTime: string
}
export const paymentApi = {
  page(params: PageQuery): Promise<PageResult<Payment>> {
    return request.get('/erp/payment/page', { params })
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
    return request.get('/erp/sale/outbound/page', { params })
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
    return request.get('/erp/sale/return/page', { params })
  },
  getById(id: number) { return request.get(`/erp/sale/return/${id}`) },
  create(data: any) { return request.post('/erp/sale/return', data) },
  delete(id: number) { return request.delete(`/erp/sale/return/${id}`) },
  approve(id: number) { return request.post(`/erp/sale/return/${id}/approve`) },
}

// ── 销售收款 ──────────────────────────────────────────
export interface SaleReceipt {
  id: number; receiptNo: string; orderNo?: string; customerName: string
  receiptDate: string; receiptAmount: number; receiptMethod: number
  status: number; creatorName?: string; createTime: string
}
export const receiptApi = {
  page(params: PageQuery): Promise<PageResult<SaleReceipt>> {
    return request.get('/erp/sale/receipt/page', { params })
  },
  getById(id: number) { return request.get(`/erp/sale/receipt/${id}`) },
  create(data: any) { return request.post('/erp/sale/receipt', data) },
  delete(id: number) { return request.delete(`/erp/sale/receipt/${id}`) },
  approve(id: number) { return request.post(`/erp/sale/receipt/${id}/approve`) },
}

// ── 销售报价 ──────────────────────────────────────────
export interface SaleQuotation {
  id: number; quotationNo: string; customerName: string
  quotationDate: string; validDate?: string; totalAmount?: number
  status: number; creatorName?: string; createTime: string
}
export const quotationApi = {
  page(params: PageQuery): Promise<PageResult<SaleQuotation>> {
    return request.get('/erp/sale/quotation/page', { params })
  },
  getById(id: number) { return request.get(`/erp/sale/quotation/${id}`) },
  create(data: any) { return request.post('/erp/sale/quotation', data) },
  update(id: number, data: any) { return request.put(`/erp/sale/quotation/${id}`, data) },
  delete(id: number) { return request.delete(`/erp/sale/quotation/${id}`) },
  send(id: number) { return request.post(`/erp/sale/quotation/${id}/send`) },
  convertToOrder(id: number) { return request.post(`/erp/sale/quotation/${id}/convert-to-order`) },
}

// ── 库存管理 ──────────────────────────────────────────
export interface StockItem {
  id: number; productCode: string; productName: string; specification?: string
  unit?: string; warehouseName?: string; quantity: number
  availableQuantity?: number; lockedQuantity?: number; minStock?: number; maxStock?: number
  lastInboundDate?: string; lastOutboundDate?: string; createTime?: string
}
export const stockApi = {
  page(params: PageQuery): Promise<PageResult<StockItem>> {
    return request.get('/erp/stock/page', { params })
  },
  getById(id: number) { return request.get(`/erp/stock/${id}`) },
}

// ── 库存盘点 ──────────────────────────────────────────
export interface StockCheck {
  id: number; checkNo: string; warehouseName: string; checkDate: string
  status: number; creatorName?: string; createTime: string
}
export const stockCheckApi = {
  page(params: PageQuery): Promise<PageResult<StockCheck>> {
    return request.get('/erp/stock/check/page', { params })
  },
  getById(id: number) { return request.get(`/erp/stock/check/${id}`) },
  create(data: any) { return request.post('/erp/stock/check', data) },
}

// ── 库存调拨 ──────────────────────────────────────────
export interface StockTransfer {
  id: number; transferNo: string; fromWarehouse: string; toWarehouse: string
  transferDate: string; status: number; creatorName?: string; createTime: string
}
export const stockTransferApi = {
  page(params: PageQuery): Promise<PageResult<StockTransfer>> {
    return request.get('/erp/stock/transfer/page', { params })
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
    return request.get('/erp/batch-sn/batch/page', { params })
  },
  getById(id: number) { return request.get(`/erp/batch-sn/batch/${id}`) },
}
