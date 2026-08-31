/**
 * ERP 统一 API 模块
 * 涵盖采购/销售/库存各子模块的 API 接口
 */
import request, { type ApiResponse, type PageResponse } from '@/utils/request'

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
/** 采购入库明细（对应后端 PurchaseInboundItem） */
export interface PurchaseInboundItem {
  id?: number; inboundId?: number; lineNo?: number
  productId: number; productCode?: string; productName?: string
  productSpec?: string; productUnit?: string
  orderItemId?: number; orderQuantity?: number; inboundQuantity?: number; pendingQuantity?: number
  unitPrice?: number; unitCost?: number; lineAmount?: number
  taxRate?: number; taxAmount?: number; lineTotal?: number
  batchNo?: string; productionDate?: string; validityDate?: string
  qualityStatus?: string; qualityNote?: string; remark?: string
}
/** 采购入库单创建/更新载荷（对应后端 PurchaseInboundCreateDTO） */
export interface PurchaseInboundPayload {
  orderId?: number; orderNo?: string
  supplierId?: number; supplierName?: string
  contractId?: number; contractNo?: string
  inboundDate?: string; inboundType?: number
  warehouseId?: number; warehouseName?: string
  purchaserId?: number; purchaserName?: string
  departmentId?: number; departmentName?: string
  trackingNumber?: string; logisticsCompany?: string
  remark?: string; internalNote?: string
  summary?: string
  extNum1?: number; extNum2?: number
  extText1?: string; extText2?: string; extText3?: string
  discountAmount?: number; fee?: number
  items?: Partial<PurchaseInboundItem>[]
}
export const inboundApi = {
  page(params: PageQuery): Promise<PageResult<PurchaseInbound>> {
    return request.get('/erp/purchase/inbound/page', params)
  },
  getById(id: number) { return request.get(`/erp/purchase/inbound/${id}`) },
  getItems(id: number): Promise<PurchaseInboundItem[]> { return request.get(`/erp/purchase/inbound/${id}/items`) },
  listByOrder(orderId: number) { return request.get(`/erp/purchase/inbound/order/${orderId}`) },
  create(data: PurchaseInboundPayload) { return request.post('/erp/purchase/inbound', data) },
  createFromOrder(orderId: number) { return request.post(`/erp/purchase/inbound/from-order/${orderId}`) },
  update(id: number, data: PurchaseInboundPayload) { return request.put(`/erp/purchase/inbound/${id}`, data) },
  delete(id: number) { return request.delete(`/erp/purchase/inbound/${id}`) },
  submit(id: number) { return request.post(`/erp/purchase/inbound/${id}/submit`) },
  approve(id: number, note?: string) { return request.post(`/erp/purchase/inbound/${id}/approve`, null, { params: { note } }) },
  reject(id: number, reason: string) { return request.post(`/erp/purchase/inbound/${id}/reject`, null, { params: { reason } }) },
  receive(id: number) { return request.post(`/erp/purchase/inbound/${id}/receive`) },
  qualityCheck(id: number, result: string) { return request.post(`/erp/purchase/inbound/${id}/quality-check`, null, { params: { result } }) },
  confirmWarehouse(id: number) { return request.post(`/erp/purchase/inbound/${id}/warehouse-confirm`) },
  complete(id: number) { return request.post(`/erp/purchase/inbound/${id}/complete`) },
  cancel(id: number, reason: string) { return request.post(`/erp/purchase/inbound/${id}/cancel`, null, { params: { reason } }) },
  statistics() { return request.get('/erp/purchase/inbound/statistics') },
}

// ── 采购费用分摊 ────────────────────────────────────────
export interface CostSharingPageItem {
  id: number; sharingDate: string; sharingNo: string; status: string
  accountTime?: string; createTime: string; totalAmount?: number
  handlerName?: string; departmentName?: string; createByName?: string
  bookkeeperName?: string; summary?: string; remark?: string; attachment?: number
  allocationMethod?: string; expenseType?: string; supplierName?: string
}
export interface CostSharingExpenseItem {
  id?: number; expenseNo?: string
  partnerId?: number; partnerName?: string; partnerCode?: string
  settleUnitId?: string; settleUnit?: string
  expenseType?: string; expenseAmount?: number; remark?: string
}
export interface CostSharingItem {
  id?: number; inboundOrderId?: number; inboundNo?: string
  supplierId?: number; supplierName?: string; supplierCode?: string
  settleUnitId?: string; settleUnit?: string
  productId?: number; productName?: string; pricingUnit?: string
  quantity?: number; discountedUnitPrice?: number; discountedAmount?: number
  allocatedCost?: number
}
export interface CostSharingDetail {
  sharing: {
    id: number; sharingNo: string; sharingDate?: string; status: number
    handlerId?: number; handlerName?: string; departmentId?: number; departmentName?: string
    allocationMethod?: string; summary?: string; remark?: string
    createByName?: string; createTime?: string; totalAmount?: number
    accountTime?: string; bookkeeperName?: string
  }
  expenseItems: CostSharingExpenseItem[]
  items: CostSharingItem[]
}
export interface CostSharingPayload {
  handlerId?: number; handlerName?: string
  departmentId?: number; departmentName?: string
  sharingDate?: string; sharingMethod?: string
  summary?: string; remark?: string; createByName?: string
  expenseItems: CostSharingExpenseItem[]
  details: CostSharingItem[]
}
export const costSharingApi = {
  page(params: PageQuery): Promise<PageResult<CostSharingPageItem>> {
    return request.get('/erp/purchase/cost-sharing/page', params)
  },
  nextNo(): Promise<string> { return request.get('/erp/purchase/cost-sharing/next-no') },
  getDetail(id: number): Promise<CostSharingDetail> {
    return request.get(`/erp/purchase/cost-sharing/${id}`)
  },
  create(data: CostSharingPayload) { return request.post('/erp/purchase/cost-sharing', data) },
  complete(id: number) { return request.post(`/erp/purchase/cost-sharing/${id}/complete`) },
  cancel(id: number) { return request.post(`/erp/purchase/cost-sharing/${id}/cancel`) },
}

// ── 采购退货 ──────────────────────────────────────────
export interface PurchaseReturn {
  id: number; returnNo: string; purchaseOrderId?: number; purchaseOrderNo?: string
  supplierId?: number; supplierName: string; supplierNo?: string
  bankName?: string; bankAccount?: string; taxNo?: string
  warehouseId?: number; warehouseName?: string
  purchaserId?: number; purchaserName?: string
  departmentId?: number; departmentName?: string
  returnDate: string; contactName?: string; contactPhone?: string; contactAddress?: string; supplierRemark?: string
  returnType?: number; returnTypeDesc?: string
  totalQuantity?: number; totalAmount?: number; discountAmount?: number; taxAmount?: number
  totalAmountWithTax?: number; settledAmount?: number; settleStatus?: number
  weight?: number; volume?: number; summary?: string
  extNum1?: number; extNum2?: number; extText1?: string; extText2?: string; extText3?: string
  paymentAccount?: string; paymentAmount?: number; moreAccounts?: string
  prevPrepaid?: number; refundPrepay?: number; prepaidBalance?: number
  currentDebt?: number; prevDebt?: number; debtBalance?: number; paymentDeadline?: string
  remark?: string; createByName?: string; posterName?: string; postTime?: string
  attachment?: string; printCount?: number; status: number; statusDesc?: string
  createTime: string; items?: PurchaseReturnItem[]
}
/** 采购退货明细（对应后端 PurchaseReturnItem） */
export interface PurchaseReturnItem {
  id?: number; returnId?: number; lineNo?: number
  productId: number; productCode?: string; productName?: string
  productSpec?: string; productUnit?: string
  image?: string; barcode?: string; model?: string; origin?: string; brand?: string; region?: string; location?: string
  availableStock?: number; availableStockConverted?: number; bookStock?: number
  batchNo?: string; batchCode?: string; productionDate?: string; shelfLife?: string; expiryDate?: string
  returnQuantity?: number; conversionRelation?: string; pieceQuantity?: number
  bigPack?: number; midPack?: number; smallPack?: number
  latestPurchaseDate?: string; retailPrice?: number; wholesalePrice?: number
  unitPrice?: number; smallUnit?: string; smallUnitPrice?: number; smallUnitQuantity?: number
  unitCost?: number; costAmount?: number; taxRate?: number; taxAmount?: number; lineTotal?: number
  volume?: number; weight?: number; gift?: boolean
  restaurant?: boolean; canteen?: boolean; outRestaurant?: boolean; vipSelf?: boolean
  largeGroup?: boolean; vipLevel1?: boolean; vipLevel2?: boolean; specialCustomer?: boolean
  customField1?: number; customField2?: number; customField3?: number
  customField4?: string; customField5?: string; customField6?: number; customField7?: number
  customField8?: string; customField9?: string; customField10?: string
  reason?: string; remark?: string
}
export const purchaseReturnApi = {
  page(params: PageQuery): Promise<PageResult<PurchaseReturn>> {
    return request.get('/erp/purchase/return/page', params)
  },
  nextNo() { return request.get('/erp/purchase/return/next-no') },
  getById(id: number) { return request.get(`/erp/purchase/return/${id}`) },
  getItems(id: number): Promise<PurchaseReturnItem[]> { return request.get(`/erp/purchase/return/${id}/items`) },
  listByOrder(orderId: number) { return request.get(`/erp/purchase/return/order/${orderId}`) },
  create(data: any) { return request.post('/erp/purchase/return', data) },
  createFromOrder(orderId: number) { return request.post(`/erp/purchase/return/from-order/${orderId}`) },
  update(id: number, data: any) { return request.put(`/erp/purchase/return/${id}`, data) },
  delete(id: number) { return request.delete(`/erp/purchase/return/${id}`) },
  submit(id: number) { return request.post(`/erp/purchase/return/${id}/submit`) },
  approve(id: number, note?: string) { return request.post(`/erp/purchase/return/${id}/approve`, null, { params: { note } }) },
  reject(id: number, reason: string) { return request.post(`/erp/purchase/return/${id}/reject`, null, { params: { reason } }) },
  complete(id: number) { return request.post(`/erp/purchase/return/${id}/complete`) },
  cancel(id: number, reason: string) { return request.post(`/erp/purchase/return/${id}/cancel`, null, { params: { reason } }) },
  statistics() { return request.get('/erp/purchase/return/statistics') },
  batchPrint(params: { ids: number[]; template?: string }) { return request.post('/erp/purchase/return/batch-print', params) },
  export(params: any) { return request.get('/erp/purchase/return/export', { params }) },
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
  id: number; outboundNo: string; orderNo?: string; outboundDate: string
  outboundType?: number; generationMethod?: string; summary?: string
  status: number; statusDesc?: string
  // 客户快照
  customerId?: number; customerName: string; customerCode?: string; customerLevel?: string
  contactId?: number; contactName?: string; customerRemark?: string
  // 银行/税务
  bankName?: string; bankAccount?: string; taxNo?: string
  // 仓库/经手人
  warehouseId?: number; warehouseName?: string
  salesPersonId?: number; salesPersonName?: string
  departmentId?: number; departmentName?: string
  location?: string; region?: string
  // 收货
  receiverName?: string; receiverPhone?: string; shippingAddress?: string
  // 数量/金额
  totalQuantity?: number; totalAmount?: number
  promoDiscount?: number; couponAmount?: number; directDiscount?: number
  otherFee?: number; roundingAmount?: number
  totalWeight?: number; totalVolume?: number
  returnQuantity?: number; returnAmount?: number; boxCount?: number
  // 结算
  settlementMethod?: string; settledAmount?: number; settlementStatus?: string
  // 收款账户
  paymentAccount1?: string; paymentAccount2?: string; paymentAccount3?: string; paymentAccount4?: string
  // 物流
  deliveryMethod?: string; logisticsCompany?: string; logisticsBranch?: string
  freightPayer?: string; freight?: number; trackingNumber?: string; waybillNo?: string; codAmount?: number
  deliveryDriver?: string
  // 流程
  pickingBy?: number; pickingTime?: string
  packingBy?: number; packingTime?: string
  shippedBy?: number; shippedTime?: string
  approvedBy?: number; approvedTime?: string; approvedNote?: string
  completedBy?: number; completedTime?: string
  expectedShipTime?: string; actualShipTime?: string
  // 会员/积分
  memberCardNo?: string
  prevPoints?: number; memberGeneratedPoints?: number; memberExchangePoints?: number
  memberUsedPoints?: number; currentPoints?: number
  // 收款日/对账日
  paymentDate?: string; reconciliationDate?: string
  // 备注
  remark?: string; internalNote?: string; buyerRemark?: string
  // 列表显示
  bookkeeperName?: string; creatorName?: string; auditorName?: string
  printCount?: number; bookkeepingTime?: string; printTime?: string
  // 表头自定义字段
  extNum1?: number; extNum2?: number; extNum3?: number; extNum4?: number; extNum5?: number
  extText1?: string; extText2?: string; extText3?: string; extText4?: string; extText5?: string
  extPartner?: number; extStaff?: number; extDept?: number
  // 表尾自定义字段
  footerExtText1?: string; footerExtText2?: string
  // 系统
  createTime: string; updateTime?: string; createBy?: number; updateBy?: number
  // 明细
  items?: any[]
}
export const outboundApi = {
  page(params: PageQuery): Promise<PageResult<SaleOutbound>> {
    return request.get('/erp/sale/outbound/page', params)
  },
  pageDetail(params: PageQuery): Promise<PageResult<any>> {
    return request.get('/erp/sale/outbound/page-detail', params)
  },
  getById(id: number) { return request.get(`/erp/sale/outbound/${id}`) },
  create(data: any) { return request.post('/erp/sale/outbound', data) },
  update(id: number, data: any) { return request.put(`/erp/sale/outbound/${id}`, data) },
  delete(id: number) { return request.delete(`/erp/sale/outbound/${id}`) },
  submit(id: number) { return request.post(`/erp/sale/outbound/${id}/submit`) },
  approve(id: number, note?: string) { return request.post(`/erp/sale/outbound/${id}/approve`, null, { params: { note } }) },
  reject(id: number, reason: string) { return request.post(`/erp/sale/outbound/${id}/reject`, null, { params: { reason } }) },
  complete(id: number) { return request.post(`/erp/sale/outbound/${id}/complete`) },
  cancel(id: number, reason?: string) { return request.post(`/erp/sale/outbound/${id}/cancel`, null, { params: { reason } }) },
  getItems(id: number) { return request.get(`/erp/sale/outbound/${id}/items`) },
  export(params: any) { return request.get('/erp/sale/outbound/export', params) },
  // ═══ 流程操作 ═══
  startPicking(id: number) { return request.post(`/erp/sale/outbound/${id}/start-picking`) },
  completePicking(id: number) { return request.post(`/erp/sale/outbound/${id}/complete-picking`) },
  startPacking(id: number) { return request.post(`/erp/sale/outbound/${id}/start-packing`) },
  completePacking(id: number) { return request.post(`/erp/sale/outbound/${id}/complete-packing`) },
  ship(id: number, data?: { trackingNumber?: string; logisticsCompany?: string }) { return request.post(`/erp/sale/outbound/${id}/ship`, null, { params: data }) },
  // ═══ 价格计算 ═══
  calculatePrice(params: { customerId: number; productId: number; quantity?: number; unitPrice?: number }) { return request.get('/erp/sale/outbound/calculate-price', params) },
  // ═══ 批量操作 ═══
  batchPrint(ids: number[]) { return request.post('/erp/sale/outbound/batch-print', ids) },
  batchImport(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/erp/sale/outbound/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  // ═══ 打印/复制 ═══
  print(id: number) { return request.post(`/erp/sale/outbound/${id}/print`) },
  copy(id: number) { return request.post(`/erp/sale/outbound/${id}/copy`) },
  // ═══ 统计 ═══
  statistics() { return request.get('/erp/sale/outbound/statistics') },
  // ═══ 从订单生成 ═══
  createFromOrder(orderId: number) { return request.post(`/erp/sale/outbound/from-order/${orderId}`) },
}

// ─ 销售退货 ──────────────────────────────────────────
export interface SaleReturn {
  id: number; returnNo: string; customerName: string; customerCode?: string
  customerLevel?: string; contactName?: string; contactPhone?: string; contactAddress?: string
  customerRemark?: string; customerTicket?: string
  warehouseId?: number; warehouseName?: string; handlerId?: number; handlerName?: string
  deptId?: number; deptName?: string; region?: string
  receiverName?: string; receiverPhone?: string; shippingAddress?: string
  orderDate?: string; returnType?: number; expectedReceiveDate?: string
  auditor?: string; auditorId?: number; auditorName?: string; auditTime?: string
  totalAmount?: number; totalQuantity?: number; status: number
  // 金额
  productAmount?: number; promoDiscount?: number; couponAmount?: number
  directDiscount?: number; discountAmount?: number; otherFee?: number
  billAmount?: number; settledAmount?: number; freightPayer?: string
  // 数量汇总
  orderedQuantity?: number; receivedQuantity?: number; unreceivedQuantity?: number; returnQuantityTotal?: number
  totalWeight?: number; totalVolume?: number
  // 结算
  settleStatus?: string; settlementMethod?: string
  // 信用额度
  creditLimit?: number; availableCredit?: number; currentDebt?: number; prevDebt?: number; debtBalance?: number; collectionDeadline?: string
  // 物流
  deliveryMethod?: string; deliveryRoute?: string; deliveryRouteId?: number; deliveryOrderNo?: string
  logisticsCompany?: string; logisticsNo?: string; waybillNo?: string; shippingFee?: number; codAmount?: number
  driverName?: string; driverId?: number; deliveryVehicle?: string; deliveryNo?: string
  // 会员积分
  memberCardNo?: string; memberName?: string; memberDiscount?: number
  prevPoints?: number; memberGeneratedPoints?: number; memberExchangePoints?: number
  memberUsedPoints?: number; currentPoints?: number
  // 源单关联
  sourceOrder?: string; sourceOrderId?: number; deliveryOrderId?: number
  // 表头自定义
  extNum1?: number; extNum2?: number; extNum3?: number; extNum4?: number; extNum5?: number
  extText1?: string; extText2?: string; extText3?: string; extText4?: string; extText5?: string
  extPartner?: number; extStaff?: number; extDept?: number
  // 表尾自定义
  footerExtText1?: string; footerExtText2?: string
  // 备注/摘要
  internalNote?: string; buyerRemark?: string; summary?: string; remark?: string
  // 销售类型/商品行属性
  salesType?: string; productLineAttr?: string
  // 流程
  generateType?: string; printCount?: number
  // 提交/审核
  submitBy?: number; submitTime?: string
  // 收款/对账
  paymentDate?: string; reconciliationDate?: string
  // 银行/税务
  bankName?: string; bankAccount?: string; taxNo?: string
  // 收款账户
  paymentAccount1?: string; paymentAccount2?: string; paymentAccount3?: string; paymentAccount4?: string
  // 时间
  bookkeepingTime?: string; printTime?: string
  createTime?: string; creatorName?: string
  items?: SaleReturnItem[]
}
export interface SaleReturnItem {
  id?: number; returnId?: number; lineNo?: number
  productId?: number; productCode?: string; productName?: string
  barcode?: string; specification?: string; productSpec?: string; productUnit?: string; unit?: string
  imageUrl?: string; area?: string; modelNo?: string; originPlace?: string; brand?: string
  // 数量/包装
  returnQuantity?: number; quantity?: number; pieceQuantity?: number
  bigPack?: number; midPack?: number; smallPack?: number
  conversionRelation?: string; conversionResult?: number
  // 小单位
  smallUnit?: string; smallUnitPrice?: number; smallUnitQuantity?: number
  // 库存
  availableStock?: number; availableStockConverted?: number; bookStock?: number
  // 价格
  unitPrice?: number; lineAmount?: number; amount?: number; taxRate?: number
  retailPrice?: number; wholesalePrice?: number; minSalePrice?: number
  lastSaleDate?: string; lastSalePrice?: number
  // 折扣
  discountRate?: number; discountedPrice?: number; discountedAmount?: number
  // 成本
  refCostPrice?: number; refCostAmount?: number
  // 物理属性
  weight?: number; volume?: number
  // 收货/终止
  receivedQuantity?: number; terminatedQuantity?: number; terminatedAmount?: number
  // 行属性
  isGift?: boolean; productLineAttr?: string; itemRemark?: string
  // 积分/兑换
  exchangeGift?: string; exchangePoints?: number
  // 价格等级（8个标准化产品价格等级）
  priceLevel1?: number; priceLevel2?: number; priceLevel3?: number; priceLevel4?: number
  priceLevel5?: number; priceLevel6?: number; priceLevel7?: number; priceLevel8?: number
  // 单据自定义
  extNum1?: number; extNum2?: number; extNum3?: number; extNum4?: number; extNum5?: number; extNum6?: number; extNum7?: number
  extText1?: string; extText2?: string
  extPartner?: number; extStaff?: number; extDept?: number
  reason?: string; remark?: string
}
export const saleReturnApi = {
  page(params: any): Promise<PageResult<SaleReturn>> {
    return request.get('/erp/sale/return/page', params)
  },
  pageDetail(params: any): Promise<PageResult<SaleReturn>> {
    return request.get('/erp/sale/return/page-detail', params)
  },
  getById(id: number) { return request.get(`/erp/sale/return/${id}`) },
  create(data: any) { return request.post('/erp/sale/return', data) },
  update(id: number, data: any) { return request.put(`/erp/sale/return/${id}`, data) },
  delete(id: number) { return request.delete(`/erp/sale/return/${id}`) },
  submit(id: number) { return request.post(`/erp/sale/return/${id}/submit`) },
  approve(id: number, note?: string) { return request.post(`/erp/sale/return/${id}/approve`, null, { params: { note } }) },
  reject(id: number, reason: string) { return request.post(`/erp/sale/return/${id}/reject`, null, { params: { reason } }) },
  complete(id: number) { return request.post(`/erp/sale/return/${id}/complete`) },
  cancel(id: number, reason?: string) { return request.post(`/erp/sale/return/${id}/cancel`, null, { params: { reason } }) },
  batchApprove(ids: number[], note?: string) { return request.post('/erp/sale/return/batch-approve', ids, { params: { note } }) },
  getItems(id: number) { return request.get(`/erp/sale/return/${id}/items`) },
  export(params: any) { return request.get('/erp/sale/return/export', params) },
}

// ── 销售退货单 ──────────────────────────────────────────
export interface SaleReturnDocItem {
  id?: number; returnDocId?: number; lineNo?: number;
  productId?: number; productCode?: string; productName?: string;
  barcode?: string; specification?: string; productSpec?: string; productUnit?: string;
  unit?: string; imageUrl?: string; storageLocation?: string; area?: string;
  modelNo?: string; originPlace?: string; brand?: string;
  availableStock?: number; availableStockConverted?: number; bookStock?: number;
  batchBarcode?: string; productionDate?: string; shelfLife?: string; expiryDate?: string;
  returnQuantity?: number; conversionRelation?: string; pieceQuantity?: number;
  bigPack?: number; midPack?: number; smallPack?: number;
  lastSaleDate?: string; lastSalePrice?: number;
  retailPrice?: number; wholesalePrice?: number; minSalePrice?: number;
  unitPrice?: number; lineAmount?: number;
  smallUnit?: string; smallUnitPrice?: number; smallUnitQuantity?: number; conversionResult?: number;
  refCostPrice?: number; refCostAmount?: number;
  discountRate?: number; discountedPrice?: number; discountedAmount?: number;
  exchangeGift?: string; exchangePoints?: number; generatedPoints?: number; usedPoints?: number;
  productLineAttr?: string; volume?: number; weight?: number;
  isGift?: boolean; remark?: string; itemRemark?: string;
  priceLevel1?: number; priceLevel2?: number; priceLevel3?: number; priceLevel4?: number;
  priceLevel5?: number; priceLevel6?: number; priceLevel7?: number; priceLevel8?: number;
  extNum1?: number; extNum2?: number; extNum3?: number; extNum4?: number;
  extNum5?: number; extNum6?: number; extNum7?: number;
  extText1?: string; extText2?: string;
  extPartner?: number; extStaff?: number; extDept?: number;
  sort?: number;
}

export interface SaleReturnDoc {
  id?: number; tenantId?: number;
  returnDocNo?: string; orderDate?: string; salesType?: string;
  status?: number; generateType?: string; settleStatus?: string;
  printCount?: number; attachment?: string;
  customerId?: number; customerName?: string; customerCode?: string; customerLevel?: string;
  contactName?: string; contactPhone?: string; contactAddress?: string;
  customerRemark?: string; customerTicket?: string;
  bankName?: string; bankAccount?: string; taxNo?: string;
  warehouseId?: number; warehouseName?: string;
  handlerId?: number; handlerName?: string;
  deptId?: number; deptName?: string;
  receiverName?: string; receiverPhone?: string; shippingAddress?: string; region?: string;
  productAmount?: number; promoDiscount?: number; couponAmount?: number;
  directDiscount?: number; discountAmount?: number; otherFee?: number;
  billAmount?: number; totalAmount?: number; discountBillAmount?: number; settledAmount?: number;
  totalQuantity?: number; returnQuantityTotal?: number; productLineCount?: number;
  totalWeight?: number; totalVolume?: number;
  paymentAccount1?: string; paymentAccount2?: string; paymentAccount3?: string; paymentAccount4?: string;
  prevAdvance?: number; returnAdvance?: number; availableAdvance?: number; advanceBalance?: number;
  receivableReduce?: number; creditLimit?: number; availableCredit?: number;
  prevDebt?: number; currentDebt?: number; debtBalance?: number; collectionDeadline?: string;
  paymentDate?: string; reconciliationDate?: string; bookkeepingTime?: string;
  deliveryMethod?: string; deliveryRoute?: string; deliveryRouteId?: number;
  deliveryOrderNo?: string; deliveryNo?: string; waybillNo?: string;
  logisticsCompany?: string; shippingFee?: number; freightPayer?: string;
  codAmount?: number; driverName?: string; driverId?: number; deliveryVehicle?: string;
  memberCardNo?: string; memberName?: string; memberDiscount?: number;
  prevPoints?: number; memberGeneratedPoints?: number; memberExchangePoints?: number;
  memberUsedPoints?: number; currentPoints?: number;
  sourceOrder?: string; sourceOrderId?: number;
  returnApplyId?: number; returnApplyNo?: string;
  returnType?: number; reason?: string;
  extNum1?: number; extNum2?: number; extNum3?: number; extNum4?: number; extNum5?: number;
  extText1?: string; extText2?: string; extText3?: string; extText4?: string; extText5?: string;
  extPartner?: number; extStaff?: number; extDept?: number;
  footerExtText1?: string; footerExtText2?: string;
  summary?: string; productLineAttr?: string; remark?: string;
  internalNote?: string; buyerRemark?: string;
  approvedBy?: number; approvedTime?: string; approvedNote?: string;
  auditor?: string; auditorId?: number; auditorName?: string; auditTime?: string;
  submitBy?: number; submitTime?: string; printTime?: string;
  creatorName?: string; createBy?: number; createTime?: string;
  updateBy?: number; updater?: string; updateTime?: string; version?: number;
  items?: SaleReturnDocItem[];
  productName?: string; itemRemark?: string;
}

export const saleReturnDocApi = {
  page(params: PageQuery): Promise<PageResult<SaleReturnDoc>> {
    return request.get('/erp/sale/return-doc/page', params)
  },
  pageDetail(params: PageQuery): Promise<PageResult<any>> {
    return request.get('/erp/sale/return-doc/page-detail', params)
  },
  getById(id: number) { return request.get(`/erp/sale/return-doc/${id}`) },
  getByReturnDocNo(returnDocNo: string) { return request.get(`/erp/sale/return-doc/returnDocNo/${returnDocNo}`) },
  create(data: any) { return request.post('/erp/sale/return-doc', data) },
  update(id: number, data: any) { return request.put(`/erp/sale/return-doc/${id}`, data) },
  delete(id: number) { return request.delete(`/erp/sale/return-doc/${id}`) },
  batchDelete(ids: number[]) { return request.delete('/erp/sale/return-doc/batch', { data: ids }) },
  submit(id: number) { return request.post(`/erp/sale/return-doc/${id}/submit`) },
  approve(id: number, note?: string) { return request.post(`/erp/sale/return-doc/${id}/approve`, null, { params: { note } }) },
  reject(id: number, reason: string) { return request.post(`/erp/sale/return-doc/${id}/reject`, null, { params: { reason } }) },
  complete(id: number) { return request.post(`/erp/sale/return-doc/${id}/complete`) },
  cancel(id: number, reason?: string) { return request.post(`/erp/sale/return-doc/${id}/cancel`, null, { params: { reason } }) },
  batchApprove(ids: number[], note?: string) { return request.post('/erp/sale/return-doc/batch-approve', ids, { params: { note } }) },
  getItems(id: number) { return request.get(`/erp/sale/return-doc/${id}/items`) },
  export(params: any) { return request.get('/erp/sale/return-doc/export', params) },
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
  convertToOrder(id: number) { return request.post(`/crm/quotation/${id}/convert`) },
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
  approve(id: number, remark?: string) { return request.post(`/erp/sale/exchange/${id}/approve`, null, { params: { remark } }) },
  batchApprove(ids: number[]) { return request.post('/erp/sale/exchange/batch-approve', ids) },
  reject(id: number, remark: string) { return request.post(`/erp/sale/exchange/${id}/reject`, null, { params: { remark } }) },
  cancel(id: number, reason?: string) { return request.post(`/erp/sale/exchange/${id}/cancel`, null, { params: { reason } }) },
  complete(id: number) { return request.post(`/erp/sale/exchange/${id}/complete`) },
  print(id: number) { return request.post(`/erp/sale/exchange/${id}/print`) },
  batchPrint(ids: number[]) { return request.post('/erp/sale/exchange/batch-print', ids) },
  getItems(id: number) { return request.get(`/erp/sale/exchange/${id}/items`) },
  getItemsByWarehouseType(id: number, warehouseType: number) { return request.get(`/erp/sale/exchange/${id}/items/${warehouseType}`) },
  getApprovalRecords(id: number) { return request.get(`/erp/sale/exchange/${id}/approval-records`) },
  export(params?: any) { return request.get('/erp/sale/exchange/export', params) },
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
  update(id: number, data: any) { return request.put(`/erp/stock/check/${id}`, data) },
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
  getPage(params: PageQuery): Promise<PageResult<StockTransfer>> {
    return request.get('/erp/stock/transfer/page', params)
  },
  pageDetail(params: PageQuery): Promise<PageResult<any>> {
    return request.get('/erp/stock/transfer/page-detail', params)
  },
  nextNo(): Promise<string> {
    return request.get('/erp/stock/transfer/next-no')
  },
  getById(id: number) { return request.get(`/erp/stock/transfer/${id}`) },
  getItems(id: number) { return request.get(`/erp/stock/transfer/${id}/items`) },
  create(data: any) { return request.post('/erp/stock/transfer', data) },
  update(id: number, data: any) { return request.put(`/erp/stock/transfer/${id}`, data) },
  delete(id: number) { return request.delete(`/erp/stock/transfer/${id}`) },
  submit(id: number) { return request.post(`/erp/stock/transfer/${id}/submit`) },
  approve(id: number, note?: string) { return request.post(`/erp/stock/transfer/${id}/approve`, null, { params: { note } }) },
  reject(id: number, reason: string) { return request.post(`/erp/stock/transfer/${id}/reject`, null, { params: { reason } }) },
  execute(id: number) { return request.post(`/erp/stock/transfer/${id}/execute`) },
  cancel(id: number, reason?: string) { return request.post(`/erp/stock/transfer/${id}/cancel`, null, { params: { reason } }) },
  batchDelete(ids: number[]) { return request.delete('/erp/stock/transfer/batch', { data: ids }) },
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

// ── 库存组装 ──────────────────────────────────────────
export interface StockAssembleItem {
  id: number
  productId: number
  productName: string
  unit: string
  quantity: number
  batchNo?: string
  producedDate?: string
  expiryDate?: string
  remark?: string
  // Add other properties as needed
}

export interface StockAssemble {
  id: number
  assembleNo: string
  productId: number
  productName: string
  assembleQty: number
  unit: string
  warehouseId: number
  warehouseName: string
  status: number
  remark?: string
  items: StockAssembleItem[]
  createTime: string
  updateTime: string
}

export const stockAssembleApi = {
  page(params: PageQuery): Promise<PageResult<StockAssemble>> {
    return request.get('/erp/stock/assemble/page', params)
  },
  getById(id: number): Promise<ApiResponse<StockAssemble>> {
    return request.get(`/erp/stock/assemble/${id}`)
  },
  create(data: StockAssemble): Promise<ApiResponse<StockAssemble>> {
    return request.post('/erp/stock/assemble', data)
  },
  update(id: number, data: StockAssemble): Promise<ApiResponse<StockAssemble>> {
    return request.put(`/erp/stock/assemble/${id}`, data)
  },
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/stock/assemble/${id}`)
  },
  submit(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/assemble/${id}/submit`)
  },
  approve(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/assemble/${id}/approve`)
  },
  reject(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/assemble/${id}/reject`, null, { params: { reason } })
  },
  execute(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/assemble/${id}/execute`)
  },
  cancel(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/assemble/${id}/cancel`, null, { params: { reason } })
  },
  export(params: any): Promise<Blob> {
    return request.get('/erp/stock/assemble/export', params, { responseType: 'blob' })
  }
}

// ── 库存拆分 ──────────────────────────────────────────
export interface StockSplitItem {
  id: number
  productId: number
  productName: string
  unit: string
  quantity: number
  batchNo?: string
  producedDate?: string
  expiryDate?: string
  remark?: string
  // Add other properties as needed
}

export interface StockSplit {
  id: number
  splitNo: string
  productId: number
  productName: string
  splitQty: number
  unit: string
  warehouseId: number
  warehouseName: string
  status: number
  remark?: string
  items: StockSplitItem[]
  createTime: string
  updateTime: string
}

export const stockSplitApi = {
  page(params: PageQuery): Promise<PageResult<StockSplit>> {
    return request.get('/erp/stock/split/page', params)
  },
  getById(id: number): Promise<ApiResponse<StockSplit>> {
    return request.get(`/erp/stock/split/${id}`)
  },
  create(data: StockSplit): Promise<ApiResponse<StockSplit>> {
    return request.post('/erp/stock/split', data)
  },
  update(id: number, data: StockSplit): Promise<ApiResponse<StockSplit>> {
    return request.put(`/erp/stock/split/${id}`, data)
  },
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/stock/split/${id}`)
  },
  submit(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/split/${id}/submit`)
  },
  approve(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/split/${id}/approve`)
  },
  reject(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/split/${id}/reject`, null, { params: { reason } })
  },
  execute(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/split/${id}/execute`)
  },
  cancel(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/split/${id}/cancel`, null, { params: { reason } })
  },
  export(params: any): Promise<Blob> {
    return request.get('/erp/stock/split/export', params, { responseType: 'blob' })
  }
}

// ── 报损单（库存损耗/报废出库，单号前缀 BSD-，与报溢单互为反向单据） ──
export const stockDamageApi = {
  getPage(params: PageQuery): Promise<PageResult<any>> {
    return request.get('/erp/stock/damage/page', params)
  },
  pageDetail(params: PageQuery): Promise<PageResult<any>> {
    return request.get('/erp/stock/damage/page-detail', params)
  },
  nextNo(): Promise<string> {
    return request.get('/erp/stock/damage/next-no')
  },
  getById(id: number) { return request.get(`/erp/stock/damage/${id}`) },
  create(data: any) { return request.post('/erp/stock/damage', data) },
  update(id: number, data: any) { return request.put(`/erp/stock/damage/${id}`, data) },
  delete(id: number) { return request.delete(`/erp/stock/damage/${id}`) },
  submit(id: number) { return request.post(`/erp/stock/damage/${id}/submit`) },
  approve(id: number, note?: string) { return request.post(`/erp/stock/damage/${id}/approve`, null, { params: { note } }) },
  reject(id: number, reason: string) { return request.post(`/erp/stock/damage/${id}/reject`, null, { params: { reason } }) },
  complete(id: number) { return request.post(`/erp/stock/damage/${id}/complete`) },
  cancel(id: number, reason?: string) { return request.post(`/erp/stock/damage/${id}/cancel`, null, { params: { reason } }) },
  getItems(id: number) { return request.get(`/erp/stock/damage/${id}/items`) },
}

// ── 库存报溢 ──────────────────────────────────────────
export interface StockOverflowItem {
  id: number
  productId: number
  productName: string
  unit: string
  overflowQty: number
  batchNo?: string
  producedDate?: string
  expiryDate?: string
  reason: string
  remark?: string
  // Add other properties as needed
}

export interface StockOverflow {
  id: number
  overflowNo: string
  warehouseId: number
  warehouseName: string
  totalItems: number
  totalOverflowAmount: number
  overflowDate: string
  reasonType: string
  reasonDesc?: string
  status: number
  applicantId?: number
  applicantName?: string
  applyTime?: string
  approvedBy?: number
  approvedTime?: string
  approvedNote?: string
  executedBy?: number
  executedTime?: string
  remark?: string
  items: StockOverflowItem[]
  createTime: string
  updateTime: string
}

export const stockOverflowApi = {
  page(params: PageQuery): Promise<PageResult<StockOverflow>> {
    return request.get('/erp/stock/overflow/page', params)
  },
  getById(id: number): Promise<ApiResponse<StockOverflow>> {
    return request.get(`/erp/stock/overflow/${id}`)
  },
  create(data: StockOverflow): Promise<ApiResponse<StockOverflow>> {
    return request.post('/erp/stock/overflow', data)
  },
  update(id: number, data: StockOverflow): Promise<ApiResponse<StockOverflow>> {
    return request.put(`/erp/stock/overflow/${id}`, data)
  },
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/stock/overflow/${id}`)
  },
  submit(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/overflow/${id}/submit`)
  },
  approve(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/overflow/${id}/approve`)
  },
  reject(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/overflow/${id}/reject`, null, { params: { reason } })
  },
  execute(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/overflow/${id}/execute`)
  },
  cancel(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/overflow/${id}/cancel`, null, { params: { reason } })
  },
  export(params: any): Promise<Blob> {
    return request.get('/erp/stock/overflow/export', params, { responseType: 'blob' })
  }
}

// ── 库存成本调整 ──────────────────────────────────────────
export interface StockCostAdjustItem {
  id: number
  productId: number
  productName: string
  unit: string
  quantity: number
  oldUnitCost: number
  newUnitCost: number
  oldTotalCost: number
  newTotalCost: number
  adjustAmount: number
  batchNo?: string
  producedDate?: string
  expiryDate?: string
  remark?: string
  // Add other properties as needed
}

export interface StockCostAdjust {
  id: number
  adjustNo: string
  adjustType: number
  adjustDate: string
  warehouseId?: number
  warehouseName?: string
  totalAdjustAmount: number
  totalItems: number
  reasonType: string
  reasonDesc?: string
  status: number
  applicantId?: number
  applicantName?: string
  applyTime?: string
  approvedBy?: number
  approvedTime?: string
  approvedNote?: string
  executedBy?: number
  executedTime?: string
  remark?: string
  items: StockCostAdjustItem[]
  createTime: string
  updateTime: string
}

export const stockCostAdjustApi = {
  page(params: PageQuery): Promise<PageResult<StockCostAdjust>> {
    return request.get('/erp/stock/cost-adjust/page', params)
  },
  getById(id: number): Promise<ApiResponse<StockCostAdjust>> {
    return request.get(`/erp/stock/cost-adjust/${id}`)
  },
  create(data: StockCostAdjust): Promise<ApiResponse<StockCostAdjust>> {
    return request.post('/erp/stock/cost-adjust', data)
  },
  update(id: number, data: StockCostAdjust): Promise<ApiResponse<StockCostAdjust>> {
    return request.put(`/erp/stock/cost-adjust/${id}`, data)
  },
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/stock/cost-adjust/${id}`)
  },
  submit(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/cost-adjust/${id}/submit`)
  },
  approve(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/cost-adjust/${id}/approve`)
  },
  reject(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/cost-adjust/${id}/reject`, null, { params: { reason } })
  },
  execute(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/cost-adjust/${id}/execute`)
  },
  cancel(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/cost-adjust/${id}/cancel`, null, { params: { reason } })
  },
  export(params: any): Promise<Blob> {
    return request.get('/erp/stock/cost-adjust/export', params, { responseType: 'blob' })
  }
}

// ── 库存预警配置 ──────────────────────────────────────────
export interface StockAlertConfig {
  id: number
  productId: number
  productName: string
  warehouseId: number
  warehouseName: string
  minStockLevel?: number
  maxStockLevel?: number
  reorderPoint?: number
  reorderQuantity?: number
  isActive: number
  alertMethod: string
  alertRecipients?: string
  alertFrequency?: string
  remark?: string
  createTime: string
  updateTime: string
}

export const stockAlertConfigApi = {
  page(params: PageQuery): Promise<PageResult<StockAlertConfig>> {
    return request.get('/erp/stock-alert-config/page', params)
  },
  getById(id: number): Promise<ApiResponse<StockAlertConfig>> {
    return request.get(`/erp/stock-alert-config/${id}`)
  },
  getProductConfig(productId: number, warehouseId: number): Promise<ApiResponse<StockAlertConfig>> {
    return request.get(`/erp/stock-alert-config/product/${productId}/warehouse/${warehouseId}`)
  },
  getWarehouseConfigs(warehouseId: number): Promise<ApiResponse<StockAlertConfig[]>> {
    return request.get(`/erp/stock-alert-config/warehouse/${warehouseId}`)
  },
  getActiveConfigs(): Promise<ApiResponse<StockAlertConfig[]>> {
    return request.get('/erp/stock-alert-config/active')
  },
  create(data: StockAlertConfig): Promise<ApiResponse<StockAlertConfig>> {
    return request.post('/erp/stock-alert-config', data)
  },
  update(id: number, data: StockAlertConfig): Promise<ApiResponse<StockAlertConfig>> {
    return request.put(`/erp/stock-alert-config/${id}`, data)
  },
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/stock-alert-config/${id}`)
  },
  activate(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock-alert-config/${id}/activate`)
  },
  deactivate(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock-alert-config/${id}/deactivate`)
  },
  checkAlerts(): Promise<ApiResponse<any>> {
    return request.get('/erp/stock-alert-config/check')
  },
  getStatistics(): Promise<ApiResponse<any>> {
    return request.get('/erp/stock-alert-config/statistics')
  },
  export(params: any): Promise<Blob> {
    return request.get('/erp/stock-alert-config/export', params, { responseType: 'blob' })
  }
}

// ── BOM管理 ──────────────────────────────────────────
/** BOM明细（对应后端 StockBomItem 实体） */
export interface StockBomItem {
  id?: number
  bomId?: number
  productId: number
  productCode?: string
  productName?: string
  productSpec?: string
  productUnit?: string
  quantity: number
  unitCost?: number
  cost?: number
  remark?: string
}

/** BOM主表（对应后端 StockBom 实体） */
export interface StockBom {
  id: number
  bomNo: string
  bomName: string
  productId: number
  productCode?: string
  productName: string
  productSpec?: string
  productUnit?: string
  outputQuantity?: number
  totalCost?: number
  bomType?: number
  status: number
  effectiveDate?: string
  expireDate?: string
  remark?: string
  items?: StockBomItem[]
  createTime: string
  updateTime: string
}

/** BOM创建/更新载荷（对应后端 StockBomController.CreateBomRequest） */
export interface StockBomPayload {
  bomName: string
  productId: number
  bomType?: number
  outputQuantity?: number
  remark?: string
  items: Partial<StockBomItem>[]
}

export const stockBomApi = {
  page(params: PageQuery): Promise<PageResult<StockBom>> {
    return request.get('/erp/stock/bom/page', params)
  },
  getById(id: number): Promise<ApiResponse<StockBom>> {
    return request.get(`/erp/stock/bom/${id}`)
  },
  getItems(bomId: number): Promise<ApiResponse<StockBomItem[]>> {
    return request.get(`/erp/stock/bom/${bomId}/items`)
  },
  create(data: StockBomPayload): Promise<ApiResponse<StockBom>> {
    return request.post('/erp/stock/bom', data)
  },
  update(id: number, data: StockBomPayload): Promise<ApiResponse<StockBom>> {
    return request.put(`/erp/stock/bom/${id}`, data)
  },
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/stock/bom/${id}`)
  },
  enable(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/bom/${id}/enable`)
  },
  disable(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/stock/bom/${id}/disable`)
  },
  export(params: any): Promise<Blob> {
    return request.get('/erp/stock/bom/export', params, { responseType: 'blob' })
  }
}

// ── 发货管理 ──────────────────────────────────────────
export interface ShipmentOrder {
  id: number; shipmentNo: string; orderNo?: string; customerName: string
  shipmentDate: string; status: number; totalAmount?: number; creatorName?: string; createTime: string
}
export const shipmentApi = {
  page(params: PageQuery): Promise<PageResult<ShipmentOrder>> {
    return request.get('/erp/shipment/page', params)
  },
  getById(id: number) { return request.get(`/erp/shipment/${id}`) },
  create(data: any) { return request.post('/erp/shipment', data) },
  update(id: number, data: any) { return request.put(`/erp/shipment/${id}`, data) },
  delete(id: number) { return request.delete(`/erp/shipment/${id}`) },
  submit(id: number) { return request.post(`/erp/shipment/${id}/submit`) },
  approve(id: number) { return request.post(`/erp/shipment/${id}/approve`) },
}

// ── 退货管理 ──────────────────────────────────────────
export interface ReturnOrder {
  id: number; returnNo: string; orderNo?: string; customerName: string
  returnDate: string; status: number; totalAmount?: number; creatorName?: string; createTime: string
}
export const returnOrderApi = {
  page(params: PageQuery): Promise<PageResult<ReturnOrder>> {
    return request.get('/erp/return/page', params)
  },
  getById(id: number) { return request.get(`/erp/return/${id}`) },
  create(data: any) { return request.post('/erp/return', data) },
  update(id: number, data: any) { return request.put(`/erp/return/${id}`, data) },
  delete(id: number) { return request.delete(`/erp/return/${id}`) },
  submit(id: number) { return request.post(`/erp/return/${id}/submit`) },
  approve(id: number) { return request.post(`/erp/return/${id}/approve`) },
}

// ── 其他出库单（库存出库：领用/赠送/样品/盘亏/其他） ──
export interface StockOutOrder {
  id: number; stockOutNo: string; stockOutType: number; stockOutTypeName?: string
  warehouseId: number; warehouseName: string; handlerId?: number; handlerName?: string
  stockOutDate: string; status: number; totalQuantity?: number; totalAmount?: number
  summary?: string; remark?: string; creatorName?: string; createTime: string; items?: any[]
}
export const stockOutApi = {
  getPage(params: PageQuery): Promise<PageResult<StockOutOrder>> {
    return request.get('/erp/stock/out/page', params)
  },
  pageDetail(params: PageQuery): Promise<PageResult<any>> {
    return request.get('/erp/stock/out/page-detail', params)
  },
  nextNo(): Promise<string> {
    return request.get('/erp/stock/out/next-no')
  },
  getById(id: number) { return request.get(`/erp/stock/out/${id}`) },
  create(data: any) { return request.post('/erp/stock/out', data) },
  update(id: number, data: any) { return request.put(`/erp/stock/out/${id}`, data) },
  delete(id: number) { return request.delete(`/erp/stock/out/${id}`) },
  submit(id: number) { return request.post(`/erp/stock/out/${id}/submit`) },
  approve(id: number, note?: string) { return request.post(`/erp/stock/out/${id}/approve`, null, { params: { note } }) },
  reject(id: number, reason: string) { return request.post(`/erp/stock/out/${id}/reject`, null, { params: { reason } }) },
  complete(id: number) { return request.post(`/erp/stock/out/${id}/complete`) },
  cancel(id: number, reason?: string) { return request.post(`/erp/stock/out/${id}/cancel`, null, { params: { reason } }) },
  getItems(id: number) { return request.get(`/erp/stock/out/${id}/items`) },
}

// ── 其他入库单（库存入库：盘盈/获赠/退货入库/其他） ──
export interface WarehouseStockInOrder {
  id: number; stockInNo: string; stockInType: number; stockInTypeName?: string
  warehouseId: number; warehouseName: string; handlerId?: number; handlerName?: string
  stockInDate: string; status: number; totalQuantity?: number; totalAmount?: number
  summary?: string; remark?: string; creatorName?: string; createTime: string; items?: any[]
}
export const warehouseStockInApi = {
  getPage(params: PageQuery): Promise<PageResult<WarehouseStockInOrder>> {
    return request.get('/erp/stock/in/page', params)
  },
  getById(id: number) { return request.get(`/erp/stock/in/${id}`) },
  create(data: any) { return request.post('/erp/stock/in', data) },
  update(id: number, data: any) { return request.put(`/erp/stock/in/${id}`, data) },
  delete(id: number) { return request.delete(`/erp/stock/in/${id}`) },
  submit(id: number) { return request.post(`/erp/stock/in/${id}/submit`) },
  approve(id: number, note?: string) { return request.post(`/erp/stock/in/${id}/approve`, null, { params: { note } }) },
  reject(id: number, reason: string) { return request.post(`/erp/stock/in/${id}/reject`, null, { params: { reason } }) },
  complete(id: number) { return request.post(`/erp/stock/in/${id}/complete`) },
  cancel(id: number, reason?: string) { return request.post(`/erp/stock/in/${id}/cancel`, null, { params: { reason } }) },
  getItems(id: number) { return request.get(`/erp/stock/in/${id}/items`) },
}

// ── 其他入库单（与其他出库单对称，单号前缀 QTRKD-） ──
export const stockInApi = {
  getPage(params: PageQuery): Promise<PageResult<any>> {
    return request.get('/erp/stock/in/page', params)
  },
  pageDetail(params: PageQuery): Promise<PageResult<any>> {
    return request.get('/erp/stock/in/page-detail', params)
  },
  nextNo(): Promise<string> {
    return request.get('/erp/stock/in/next-no')
  },
  getById(id: number) { return request.get(`/erp/stock/in/${id}`) },
  create(data: any) { return request.post('/erp/stock/in', data) },
  update(id: number, data: any) { return request.put(`/erp/stock/in/${id}`, data) },
  delete(id: number) { return request.delete(`/erp/stock/in/${id}`) },
  submit(id: number) { return request.post(`/erp/stock/in/${id}/submit`) },
  approve(id: number, note?: string) { return request.post(`/erp/stock/in/${id}/approve`, null, { params: { note } }) },
  reject(id: number, reason: string) { return request.post(`/erp/stock/in/${id}/reject`, null, { params: { reason } }) },
  complete(id: number) { return request.post(`/erp/stock/in/${id}/complete`) },
  cancel(id: number, reason?: string) { return request.post(`/erp/stock/in/${id}/cancel`, null, { params: { reason } }) },
  getItems(id: number) { return request.get(`/erp/stock/in/${id}/items`) },
}

// ── 库存盘点 ──────────────────────────────────────────
export interface StocktakeOrder {
  id: number; checkNo: string; warehouseName: string
  checkDate: string; status: number; creatorName?: string; createTime: string
}
export const stocktakeOrderApi = {
  page(params: PageQuery): Promise<PageResult<StocktakeOrder>> {
    return request.get('/erp/stocktake/page', params)
  },
  getById(id: number) { return request.get(`/erp/stocktake/${id}`) },
  create(data: any) { return request.post('/erp/stocktake', data) },
  update(id: number, data: any) { return request.put(`/erp/stocktake/${id}`, data) },
  delete(id: number) { return request.delete(`/erp/stocktake/${id}`) },
  submit(id: number) { return request.post(`/erp/stocktake/${id}/submit`) },
  approve(id: number) { return request.post(`/erp/stocktake/${id}/approve`) },
}

// ── 销售订单 ──────────────────────────────────────────
/** 销售订单表头 - 完整字段 */
export interface SaleOrder {
  id: number; orderNo: string; customerName: string; orderDate: string
  totalAmount?: number; totalAmountWithTax?: number; status: number; statusName?: string
  salesmanName?: string; remark?: string; createTime: string; updateTime?: string
  // 客户信息
  customerId?: number; customerCode?: string; customerRemark?: string; customerLevel?: string;
  customerGradeCode?: string; customerGradeName?: string; customerTicket?: string;
  // 银行/税务
  bankName?: string; bankAccount?: string; taxNo?: string;
  // 仓库/经手人/部门
  warehouseId?: number; warehouseName?: string; salesmanId?: number;
  deptId?: number; deptName?: string; promoterId?: number; promoterName?: string;
  // 收货
  receiverName?: string; receiverPhone?: string; shippingAddress?: string;
  contactName?: string; contactPhone?: string; pickupAddress?: string;
  // 结款
  settlementMethod?: string;
  // 配送
  deliveryMethod?: string; deliveryRoute?: string; deliveryRouteId?: number;
  driverId?: number; driverName?: string; deliveryVehicle?: string;
  // 物流
  logisticsCompany?: string; logisticsNo?: string; freightPayer?: string;
  shippingFee?: number; waybillNo?: string; codAmount?: number;
  // 金额
  productAmount?: number; promoDiscount?: number; couponAmount?: number;
  directDiscount?: number; discountAmount?: number; otherFee?: number;
  billAmount?: number; settledAmount?: number;
  // 订金/预收
  depositAccount?: string; depositAmount?: number; prevAdvance?: number; advanceBalance?: number;
  depositAccount1?: string; depositAccount2?: string; depositAccount3?: string; depositAccount4?: string;
  // 信用
  creditLimit?: number; availableCredit?: number; prevDebt?: number;
  // 收款日/对账日
  paymentDate?: string; reconciliationDate?: string;
  // 会员/积分
  memberCardNo?: string; memberName?: string; memberDiscount?: number;
  prevPoints?: number; salePoints?: number; returnPoints?: number;
  exchangePoints?: number; usedPoints?: number; currentPoints?: number;
  // 数量
  totalQuantity?: number; shippedQuantity?: number; unshippedQuantity?: number;
  returnQuantity?: number; returnAmount?: number;
  // 物理属性
  totalWeight?: number; totalVolume?: number;
  // 备注
  orderRemark?: string; buyerRemark?: string; summary?: string;
  // 区域/附件
  region?: string; attachment?: string;
  // 自定义字段
  extNum1?: number; extNum2?: number; extText1?: string; extText2?: string; extText3?: string;
  footerExtText1?: string; footerExtText2?: string;
  // 审核
  auditorId?: number; auditorName?: string; auditTime?: string;
  // 提交
  submitterId?: number; submitterName?: string; submitTime?: string;
  // 制单/打印
  printCount?: number; bookkeepingTime?: string; creatorName?: string;
  // 第三方
  thirdPartyOrderNo?: string;
  // 收款账户
  paymentAccountId?: number;
  // 来源
  orderSource?: number;
  // 支付
  paymentMethod?: string; paymentStatus?: number;
  receivedAmount?: number;
  // 品牌/行业
  productBrand?: string; industryCategory?: string;
  // 预计发货
  expectedShipTime?: string;
  // 补单/履约
  supplementType?: string; generationMethod?: string; sourceOrder?: string;
  supplementStatus?: string; originalOrderId?: number; originalOrderNo?: string;
  shippedOrderNo?: string; originalAmount?: number; remainingUnshippedAmount?: number;
  originalDiscount?: number; originalItemCount?: number; unshippedItemCount?: number;
  originalQuantity?: number; unshippedQuantityItems?: number; fulfillmentRate?: number;
  // 拣货
  pickingWarehouse?: string; collectionLocation?: string;
  // 明细
  items?: SaleOrderItem[];
  [key: string]: any;
}

/** 销售订单明细 - 完整76字段 */
export interface SaleOrderItem {
  id?: number; orderId?: number; lineNo?: number;
  // 商品
  productId?: number; productCode?: string; productName?: string; image?: string;
  itemCode?: string; barcode?: string; smallUnitBarcode?: string;
  specification?: string; model?: string; origin?: string; brand?: string;
  shelfLife?: string; unit?: string; pricingUnit?: string; smallUnit?: string;
  lineAttribute?: string; area?: string; location?: string;
  // 批次
  batchCode?: string; productionDate?: string; expiryDate?: string;
  // 包装/数量
  quantity?: number; bigPack?: number; midPack?: number; smallPack?: number; smallUnitQuantity?: number;
  // 库存
  availableStock?: number; availableStockConverted?: number; bookStock?: number;
  conversionRelation?: string; unshippedQuantity?: number; shippedQuantityDetail?: number;
  // 价格
  smallUnitPrice?: number; latestSaleDate?: string; latestSalePrice?: number;
  retailPrice?: number; wholesalePrice?: number; lowestPrice?: number;
  unitPrice?: number; costPrice?: number;
  // 客户类型（价格等级标准化字段，非用户昵称）
  restaurant?: boolean; canteen?: boolean; vipSelf?: boolean; largeGroup?: boolean;
  specialCustomer?: boolean; outRestaurant?: boolean; vipLevel1?: boolean; vipLevel2?: boolean;
  // 预订货
  preOrderNo?: string; usePreOrderAmount?: number;
  // 折扣
  discountRate?: number; discountPercent?: number; originalPrice?: number;
  discountedUnitPrice?: number; favorableUnitPrice?: number;
  // 积分/礼品
  giftItem?: string; exchangePoints?: number; usedPoints?: number;
  // 物理属性
  volume?: number; weight?: number;
  // 赠品
  gift?: boolean;
  // 备注
  remark?: string;
  // 仓库
  warehouseId?: number;
  // 价格等级快照
  customerGradeCode?: string; customerGradeName?: string;
  priceGradeCode?: string; priceSource?: string; calculatedPrice?: number; discountApplied?: string;
  // 自定义字段
  customField1?: number; customField2?: number; customField3?: number;
  customField4?: string; customField5?: string; customField6?: number; customField7?: number;
  customField8?: number; customField9?: number; customField10?: number;
  // 计算字段
  amount?: number; costAmount?: number; grossProfit?: number;
  discountedAmount?: number; favorableAmount?: number; taxAmount?: number;
  unitPriceWithTax?: number; amountWithTax?: number; discountAmount?: number;
  taxRate?: number; shippedQuantity?: number;
  [key: string]: any;
}

/** 销售首页统计 */
export interface SaleStats { monthOrderCount: number; monthAmount: number; pendingCount: number }
export const saleStatsApi = {
  get(): Promise<ApiResponse<SaleStats>> { return request.get('/erp/sale/order/stats') }
}

/** 订单处理中心统计卡片（对应后端 /center/stats 返回字段） */
export interface OrderCenterStats {
  totalOrders: number
  pendingOutbound: number
  pendingShip: number
  outboundCount: number
  shippedCount: number
  completedCount: number
}

export const saleOrderApi = {
  getPage(params: any): Promise<PageResult<SaleOrder>> { return request.get('/erp/sale/order/page', params) },
  getById(id: number) { return request.get(`/erp/sale/order/${id}`) },
  create(data: any) { return request.post('/erp/sale/order', data) },
  update(id: number, data: any) { return request.put(`/erp/sale/order/${id}`, data) },
  delete(id: number) { return request.delete(`/erp/sale/order/${id}`) },
  batchDelete(ids: number[]) { return request.delete('/erp/sale/order/batch', { data: ids }) },
  submit(id: number) { return request.post(`/erp/sale/order/${id}/submit`) },
  approve(id: number) { return request.post(`/erp/sale/order/${id}/approve`) },
  reject(id: number, reason: string) { return request.post(`/erp/sale/order/${id}/reject`, null, { params: { reason } }) },
  cancel(id: number, reason?: string) { return request.post(`/erp/sale/order/${id}/cancel`, null, { params: { reason } }) },
  ship(id: number, warehouseId: number) { return request.post(`/erp/sale/order/${id}/ship`, null, { params: { warehouseId } }) },
  payment(id: number, amount: number) { return request.post(`/erp/sale/order/${id}/payment`, null, { params: { amount } }) },
  batchApprove(ids: number[]) { return request.post('/erp/sale/order/batch-approve', ids) },
  print(id: number) { return request.get(`/erp/sale/order/${id}/print`) },
  batchPrint(ids: number[]) { return request.post('/erp/sale/order/batch-print', ids) },
  export(params: any) { return request.get('/erp/sale/order/export', params) },
  getStats(params: any) { return request.get('/erp/sale/order/stats', params) },
  getPending(tenantId: number) { return request.get('/erp/sale/order/pending', { tenantId }) },
  // ═══ 订单处理中心 API ══
  /** 订单处理中心统计卡片 */
  getCenterStats(params: any) { return request.get('/erp/sale/order/center/stats', params) },
  /** 按单据tab分页 */
  getCenterPageByDoc(params: any) { return request.get('/erp/sale/order/center/page-by-doc', params) },
  /** 按时间tab分组 */
  getCenterGroupByDate(params: any) { return request.get('/erp/sale/order/center/group-by-date', params) },
  /** 按线路tab分组 */
  getCenterGroupByRoute(params: any) { return request.get('/erp/sale/order/center/group-by-route', params) },
  /** 按客户tab分组 */
  getCenterGroupByCustomer(params: any) { return request.get('/erp/sale/order/center/group-by-customer', params) },
  /** 订单履约tab分页 */
  getCenterFulfillmentPage(params: any) { return request.get('/erp/sale/order/center/fulfillment-page', params) },
  /** 订单履约统计概览 */
  getCenterFulfillmentOverview(params: any) { return request.get('/erp/sale/order/center/fulfillment-overview', params) },
  /** 待审核列表 */
  getPendingReviewPage(params: any) { return request.get('/erp/sale/order/center/pending-review', params) },
  /** 拣货/发货列表 */
  getPickingShippingPage(params: any) { return request.get('/erp/sale/order/center/picking-shipping', params) },
  /** 批量导入 */
  batchImport(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/erp/sale/order/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  /** 客户信用额度 */
  getCustomerCredit(customerId: number) { return request.get(`/erp/sale/order/customer-credit/${customerId}`) },
  /** 客户订金余额 */
  getCustomerDeposits(customerId: number) { return request.get(`/erp/sale/order/customer-deposits/${customerId}`) },
  /** 库存查询 */
  getStockDetail(productId: number, warehouseId: number) { return request.get(`/erp/stock/${productId}/${warehouseId}`) },
  /** 商品汇总 */
  productSummary(params: any) { return request.get('/erp/sale/order/product-summary', params) },
  /** 批量更新物流备注 */
  batchLogisticsRemark(ids: number[], remark: string) { return request.post('/erp/sale/order/batch-logistics-remark', { ids, remark }) },
}

// ── 销售价格跟踪（/api/sales/price-track） ─────────────
// 后端 SalesPriceTrackController 复用销售明细查询，返回裸 Page<Map>，
// 拦截器对 { records, total } 透传不拆包；行字段取 buildRowMap 的常用列

/** 销售价格跟踪查询条件（与后端 SalesDetailQueryDTO 常用字段对齐） */
export interface SalesPriceTrackQuery {
  current?: number
  size?: number
  /** 开始日期 yyyy-MM-dd */
  startDate?: string
  /** 结束日期 yyyy-MM-dd */
  endDate?: string
  /** 商品名称（明细级模糊） */
  productName?: string
  /** 货号 */
  productCode?: string
  /** 客户名称 */
  customerName?: string
  /** 单据编号 */
  documentNo?: string
  /** 最低价 */
  minPrice?: number
  /** 最高价 */
  maxPrice?: number
}

/** 销售价格跟踪行（后端 buildRowMap 常用列，明细级：每行=一条出库明细） */
export interface SalesPriceTrackItem {
  docDate: string
  docNo: string
  docType: string
  warehouseName: string
  customerName: string
  customerCode: string
  productName: string
  productCode: string
  barcode: string
  specification: string
  model: string
  origin: string
  brand: string
  salesQuantity: number
  unitPrice: number
  discountedPrice: number
  discountRate: number
  amount: number
  wholesalePrice: number
  retailPrice: number
  minSalePrice: number
  costPrice: number
  grossProfit: number
  handlerName: string
  createTime: string
}

export const salesPriceTrackApi = {
  /** 分页查询销售价格跟踪（明细级，按单据日期倒序） */
  page(params?: SalesPriceTrackQuery): Promise<PageResponse<SalesPriceTrackItem>> {
    return request.get('/sales/price-track/page', params)
  }
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

// ── 零售单 ────────────────────────────────────────────────
export interface RetailOrder {
  id: number
  retailNo: string
  customerId: number
  customerName: string
  amount: number
  paymentMethod: string
  status: number
  remark?: string
  createTime: string
  warehouseId?: number
  warehouseName?: string
  handlerId?: number
  handlerName?: string
  orderDate?: string
  memberCardNo?: string
  memberName?: string
  directDiscount?: number
  couponDiscount?: number
  promoDiscount?: number
  payableAmount?: number
  cashAmount?: number
  cardAmount?: number
  prepaidAmount?: number
  transferAmount?: number
  combinedPayment?: boolean
  changeAmount?: number
  prepaidBalance?: number
  items?: RetailOrderItem[]
  [key: string]: any
}

export interface RetailOrderItem {
  id?: number
  productId?: number
  productName?: string
  itemCode?: string
  barcode?: string
  unit?: string
  quantity: number
  unitPrice: number
  amount: number
  bigPack?: number
  midPack?: number
  smallPack?: number
  remark?: string
  [key: string]: any
}

export const retailOrderApi = {
  // 按单据分页
  pageByDoc(params: any): Promise<any> {
    return request.get('/sales/retail/page/doc', params)
  },
  // 按明细分页
  pageByDetail(params: any): Promise<any> {
    return request.get('/sales/retail/page/detail', params)
  },
  // 详情（含明细行+支付明细）
  getDetail(id: number | string): Promise<any> {
    return request.get(`/sales/retail/${id}`)
  },
  // 创建
  create(data: any): Promise<any> {
    return request.post('/sales/retail', data)
  },
  // 更新
  update(id: number | string, data: any): Promise<any> {
    return request.put(`/sales/retail/${id}`, data)
  },
  // 复制
  copy(id: number | string): Promise<any> {
    return request.post(`/sales/retail/${id}/copy`)
  },
  // 结算
  settle(id: number | string, payments: any[]): Promise<any> {
    return request.post(`/sales/retail/${id}/settle`, { payments })
  },
  // 挂单
  hold(id: number | string): Promise<any> {
    return request.post(`/sales/retail/${id}/hold`)
  },
  // 取单
  unhold(id: number | string): Promise<any> {
    return request.post(`/sales/retail/${id}/unhold`)
  },
  // 作废
  voidOrder(id: number | string, reason?: string): Promise<any> {
    return request.post(`/sales/retail/${id}/void`, null, { params: { reason } })
  },
  // 打印数据
  getPrintData(id: number | string): Promise<any> {
    return request.get(`/sales/retail/${id}/print-data`)
  },
  // 打印后更新计数
  afterPrint(id: number | string): Promise<any> {
    return request.post(`/sales/retail/${id}/print`)
  },
  // 挂单列表
  holdList(warehouseId?: number): Promise<any> {
    return request.get('/sales/retail/hold-list', { warehouseId })
  },
  // 商品快速查找
  quickSearchProducts(keyword: string, warehouseId?: number): Promise<any> {
    return request.get('/sales/retail/products/quick', { keyword, warehouseId })
  },
  // 明细行
  getItems(id: number | string): Promise<any> {
    return request.get(`/sales/retail/${id}/items`)
  },
  // 删除
  delete(id: number | string): Promise<any> {
    return request.delete(`/sales/retail/${id}`)
  },
}

// ─ 会员（个人客户，party_level=MEMBER）────────────────────────
export const memberApi = {
  /** 搜索会员（按手机号/会员卡号/姓名） */
  search(keyword: string): Promise<any[]> {
    return request.get('/erp/party/search', { keyword, partyType: 1 }).then((res: any) => {
      const list = res?.data || res || []
      // 只返回 party_level='MEMBER' 的记录
      return list.filter((p: any) => p.partyLevel === 'MEMBER' || p.party_level === 'MEMBER')
    })
  },
  /** 获取默认散客 */
  getWalkIn(): Promise<any> {
    return request.get('/erp/party/search', { keyword: '散客', partyType: 1 }).then((res: any) => {
      const list = res?.data || res || []
      return list.find((p: any) => p.partyCode === 'WALKIN' || p.party_code === 'WALKIN') || null
    })
  },
  /** 会员分页列表 */
  list(params: { pageNum?: number; pageSize?: number; keyword?: string }): Promise<PageResult<any>> {
    return request.get('/erp/party/page', params)
  },
  /** 会员分页查询 */
  page(params: { pageNum?: number; pageSize?: number; keyword?: string }): Promise<PageResult<any>> {
    return request.get('/erp/party/page', params)
  },
  /** 根据ID获取会员 */
  getById(id: number): Promise<any> {
    return request.get(`/erp/party/${id}`)
  },
  /** 新增会员 */
  create(data: Partial<any>): Promise<any> {
    return request.post('/erp/party', data)
  },
  /** 更新会员 */
  update(id: number, data: Partial<any>): Promise<any> {
    return request.put(`/erp/party/${id}`, data)
  },
  /** 调整积分（支持对象参数或独立参数） */
  adjustPoints(idOrPayload: number | { memberId: number; points: number; reason?: string }, points?: number, reason?: string): Promise<any> {
    if (typeof idOrPayload === 'object') {
      return request.post(`/erp/party/${idOrPayload.memberId}/adjust-points`, null, { params: { points: idOrPayload.points, reason: idOrPayload.reason } })
    }
    return request.post(`/erp/party/${idOrPayload}/adjust-points`, null, { params: { points, reason } })
  },
}

// ── 预订货单 ──────────────────────────────────────────
export interface PreOrder {
  id: number
  orderNo: string
  customerId?: number
  customerName: string
  customerCode?: string
  bankName?: string
  bankAccount?: string
  taxNo?: string
  orderDate: string
  status: number
  saleType?: number
  warehouseId?: number
  warehouseName?: string
  handlerId?: number
  handlerName?: string
  deptId?: number
  deptName?: string
  receiverName?: string
  receiverPhone?: string
  shippingAddress?: string
  customerLevel?: string
  totalAmount: number
  discountedAmount?: number
  orderAmount?: number
  depositAmount: number
  depositAccount1?: string
  depositAccount2?: string
  depositAccount3?: string
  depositAccount4?: string
  receivedDeposit: number
  unreceivedDeposit: number
  depositBalance: number
  creditLimit?: number
  depositDeadline?: string
  settlementStatus?: number
  preOrderQuantity: number
  orderedQuantity: number
  unOrderedQuantity?: number
  shippedQuantity: number
  unShippedQuantity?: number
  totalWeight?: number
  totalVolume?: number
  region?: string
  summary?: string
  remark?: string
  extNum1?: number
  extNum2?: number
  extText1?: string
  extText2?: string
  extText3?: string
  printCount?: number
  creatorName?: string
  submitterName?: string
  submitTime?: string
  auditorName?: string
  createTime: string
  updateTime?: string
  items?: PreOrderItem[]
}

export interface PreOrderItem {
  id?: number
  orderId?: number
  lineNo?: number
  productId?: number
  imageUrl?: string
  productName?: string
  productCode?: string
  barcode?: string
  specification?: string
  model?: string
  origin?: string
  brand?: string
  unit?: string
  pricingUnit?: string
  smallUnit?: string
  smallUnitQuantity?: number
  conversionRelation?: string
  conversionResult?: number
  region?: string
  location?: string
  availableStock?: number
  availableStockConversion?: number
  bookStock?: number
  quantity: number
  pieceQuantity?: number
  bigPack?: number
  midPack?: number
  smallPack?: number
  orderedQuantity?: number
  unOrderedQuantity?: number
  shippedQuantity?: number
  unShippedQuantity?: number
  terminateQuantity?: number
  terminateAmount?: number
  lastSaleDate?: string
  retailPrice?: number
  wholesalePrice?: number
  minSalePrice?: number
  unitPrice: number
  amount: number
  smallUnitPrice?: number
  discountRate?: number
  discountedPrice?: number
  discountedAmount?: number
  costPrice?: number
  costAmount?: number
  grossProfit?: number
  volume?: number
  weight?: number
  productAttribute?: string
  gift?: boolean
  remark?: string
  priceLevel1?: number
  priceLevel2?: number
  priceLevel3?: number
  priceLevel4?: number
  priceLevel5?: number
  priceLevel6?: number
  priceLevel7?: number
  priceLevel8?: number
  extNum1?: number
  extNum2?: number
  extNum3?: number
  extText1?: string
  extText2?: string
  extNum4?: number
  extNum5?: number
  // 关联快照字段
  warehouseName?: string
  customerName?: string
  customerCode?: string
  customerLevel?: string
  receiverName?: string
  receiverPhone?: string
  shippingAddress?: string
  customerTicket?: string
  customerRemark?: string
  handlerName?: string
  deptName?: string
  saleType?: number
  orderNo?: string
  orderDate?: string
  orderStatus?: number
  orderRemark?: string
  summary?: string
  attachment?: string
  creatorName?: string
  auditorName?: string
  submitTime?: string
}

export const preOrderApi = {
  page(params: PageQuery): Promise<PageResult<PreOrder>> {
    return request.get('/erp/sale/pre-order/page', params)
  },
  pageDetail(params: PageQuery): Promise<PageResult<any>> {
    return request.get('/erp/sale/pre-order/page-detail', params)
  },
  getById(id: number) { return request.get(`/erp/sale/pre-order/${id}`) },
  create(data: any) { return request.post('/erp/sale/pre-order', data) },
  update(id: number, data: any) { return request.put(`/erp/sale/pre-order/${id}`, data) },
  delete(id: number) { return request.delete(`/erp/sale/pre-order/${id}`) },
  submit(id: number) { return request.post(`/erp/sale/pre-order/${id}/submit`) },
  approve(id: number) { return request.post(`/erp/sale/pre-order/${id}/approve`) },
  export(params: any) { return request.get('/erp/sale/pre-order/export', params) },
  batchOrder(ids: number[]) { return request.post('/erp/sale/pre-order/batch-order', { ids }) },
  print(id: number) { return request.post(`/erp/sale/pre-order/${id}/print`) },
}

// ─ 用户页面配置 ────────────────────────────────────────
export const userPageConfigApi = {
  /** 获取用户页面配置 */
  get(module: string, page: string): Promise<string> {
    return request.get(`/system/user-config/${module}/${page}`).then((res: any) => {
      return res?.data || res || ''
    })
  },
  /** 保存用户页面配置 */
  save(module: string, page: string, value: string): Promise<void> {
    return request.post(`/system/user-config/${module}/${page}`, { value })
  },
}
