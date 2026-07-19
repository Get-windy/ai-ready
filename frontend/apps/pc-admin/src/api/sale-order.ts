import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// ═══════════════════════════════════════════
// 列表 DTO (与后端 SaleOrderListDTO 对齐, 65+列)
// ═══════════════════════════════════════════
export interface SaleOrderListItem {
  id: string
  orderNo: string
  orderDate: string
  saleType: number
  status: number
  statusName: string
  // 客户信息
  customerId: number
  customerName: string
  customerCode: string
  customerLevel: string
  customerRemark: string
  customerTicket: string
  // 经手人/部门
  salesmanId: number
  salesmanName: string
  deptName: string
  // 仓库
  warehouseId: number
  warehouseName: string
  // 收货信息
  receiverName: string
  receiverPhone: string
  shippingAddress: string
  // 推广人
  promoterId: number
  promoterName: string
  // 金额
  productAmount: number
  discountAmount: number
  billAmount: number
  settledAmount: number
  receivedAmount: number
  promoDiscount: number
  couponAmount: number
  directDiscount: number
  otherFee: number
  // 运费
  freightPayer: string
  shippingFee: number
  // 数量
  totalQuantity: number
  shippedQuantity: number
  unshippedQuantity: number
  returnQuantity: number
  returnAmount: number
  // 物理汇总
  totalWeight: number
  totalVolume: number
  // 物流
  logisticsCompany: string
  waybillNo: string
  // 订金账户
  depositAccount1: string
  depositAccount2: string
  depositAccount3: string
  depositAccount4: string
  // 区域/销售类型/配送方式
  region: string
  deliveryMethod: string
  // 备注
  buyerRemark: string
  orderRemark: string
  summary: string
  // 附件
  attachment: string
  // 自定义字段
  extNum1: number
  extNum2: number
  extText1: string
  extText2: string
  extText3: string
  footerExtText1: string
  footerExtText2: string
  // 提交/审核/制单
  submitTime: string
  generationMethod: string
  bookkeepingTime: string
  creatorName: string
  submitterName: string
  auditorName: string
  auditTime: string
  printCount: number
  // 第三方/来源
  thirdPartyOrderNo: string
  sourceOrder: string
  // 结款方式
  settlementMethod: string
  // 预计发货时间
  expectedShipTime: string
  // 履约相关
  originalOrderNo: string
  shippedOrderNo: string
  supplementStatus: string
  originalAmount: number
  remainingUnshippedAmount: number
  originalDiscount: number
  originalItemCount: number
  unshippedItemCount: number
  originalQuantity: number
  unshippedQuantityItems: number
  // 拣货/发货
  pickingWarehouse: string
  collectionLocation: string
  pickupAddress: string
  // 时间
  createTime: string
  updateTime: string
}

// ═══════════════════════════════════════════
// 详情 DTO (含子表数据)
// ═══════════════════════════════════════════
export interface SaleOrderDetail {
  id: string
  orderNo: string
  orderDate: string
  saleType: number
  status: number
  statusName: string
  customerId: number
  warehouseId: number
  salesmanId: number
  deptId: number
  productAmount: number
  discountAmount: number
  billAmount: number
  settledAmount: number
  receivedAmount: number
  totalQuantity: number
  expectedShipTime: string
  supplementType: string
  generationMethod: string
  sourceOrder: string
  orderSource: number
  remark: string
  originalOrderId: number
  originalOrderNo: string
  createTime: string
  updateTime: string

  // 子表数据
  partnerSnapshot?: PartnerSnapshot
  deliveryAddresses?: DeliveryAddress[]
  settlement?: Settlement
  logisticsList?: Logistics[]
  deposits?: Deposit[]
  pointsJournal?: PointsJournal
  auditTrails?: AuditTrail[]
  extInfo?: ExtInfo
  items?: SaleOrderItemDTO[]
}

export interface PartnerSnapshot {
  customerName: string
  customerCode: string
  customerLevel: string
  customerGradeCode: string
  customerGradeName: string
  customerTicket: string
  customerRemark: string
  bankName: string
  bankAccount: string
  taxNo: string
  region: string
}

export interface DeliveryAddress {
  addressType: string
  contactName: string
  phone: string
  address: string
  isDefault: boolean
}

export interface Settlement {
  settlementMethod: string
  creditLimit: number
  availableCredit: number
  prevDebt: number
  paymentDate: string
  reconciliationDate: string
  paymentAccountId: number
  paymentMethod: string
  paymentStatus: number
}

export interface Logistics {
  logisticsType: string
  deliveryMethod: string
  deliveryRoute: string
  deliveryRouteId: number
  driverId: number
  driverName: string
  deliveryVehicle: string
  logisticsCompany: string
  logisticsNo: string
  waybillNo: string
  freightPayer: string
  shippingFee: number
  codAmount: number
}

export interface Deposit {
  accountName: string
  amount: number
  sequence: number
}

export interface PointsJournal {
  memberCardNo: string
  memberName: string
  memberDiscount: number
  prevPoints: number
  salePoints: number
  returnPoints: number
  exchangePoints: number
  usedPoints: number
  currentPoints: number
}

export interface AuditTrail {
  action: string
  operatorId: number
  operatorName: string
  actionTime: string
  remark: string
}

export interface ExtInfo {
  summary: string
  attachment: string
  extNum1: number
  extNum2: number
  extText1: string
  extText2: string
  extText3: string
  footerExtText1: string
  footerExtText2: string
}

export interface SaleOrderItemDTO {
  id: string
  orderId: string
  lineNo: number

  // ═══ 商品信息 ═══
  productId: number
  productCode: string
  productName: string
  image: string
  itemCode: string
  barcode: string
  smallUnitBarcode: string
  specification: string
  model: string
  origin: string
  brand: string
  shelfLife: string
  unit: string
  pricingUnit: string
  smallUnit: string
  lineAttribute: string
  area: string
  location: string

  // ═══ 批次信息 ═══
  batchCode: string
  productionDate: string
  expiryDate: string

  // ═══ 包装/数量 ═══
  quantity: number
  bigPack: number
  midPack: number
  smallPack: number
  smallUnitQuantity: number

  // ═══ 库存相关 ═══
  availableStock: number
  availableStockConverted: number
  bookStock: number
  conversionRelation: string
  unshippedQuantity: number
  shippedQuantityDetail: number

  // ═══ 价格信息 ═══
  smallUnitPrice: number
  latestSaleDate: string
  latestSalePrice: number
  retailPrice: number
  wholesalePrice: number
  lowestPrice: number
  unitPrice: number
  costPrice: number

  // ═══ 客户类型（价格等级标准化字段） ═══
  restaurant: boolean
  canteen: boolean
  vipSelf: boolean
  largeGroup: boolean
  specialCustomer: boolean
  outRestaurant: boolean
  vipLevel1: boolean
  vipLevel2: boolean

  // ═══ 预订货 ═══
  preOrderNo: string
  usePreOrderAmount: number

  // ═══ 折扣 ═══
  discountRate: number
  discountPercent: number
  originalPrice: number
  discountedUnitPrice: number
  favorableUnitPrice: number

  // ═══ 积分/礼品 ═══
  giftItem: string
  exchangePoints: number
  usedPoints: number

  // ═══ 物理属性 ═══
  volume: number
  weight: number

  // ═══ 赠品 ═══
  gift: boolean

  // ═══ 备注 ══
  remark: string

  // ══ 仓库 ═══
  warehouseId: number

  // ═══ 价格等级（快照） ═══
  customerGradeCode: string
  customerGradeName: string
  priceGradeCode: string
  priceSource: string
  calculatedPrice: number
  discountApplied: string

  // ═══ 自定义字段 ══
  customField1: number
  customField2: number
  customField3: number
  customField4: string
  customField5: string
  customField6: number
  customField7: number
  customField8: number
  customField9: number
  customField10: number

  // ═══ 计算字段（展示用，不入库） ═══
  amount: number
  costAmount: number
  grossProfit: number
  discountedAmount: number
  favorableAmount: number
  taxAmount: number
  unitPriceWithTax: number
  amountWithTax: number
  discountAmount: number
  taxRate: number
  shippedQuantity: number
} // ~85 fields, 对齐后端 SaleOrderItemDTO.java 和 SaleOrderItem.java

// ═══════════════════════════════════════════
// 创建/更新 DTO (对齐后端 SaleOrderDTO.java)
// ═══════════════════════════════════════════
export interface SaleOrderCreateDTO {
  orderNo?: string
  orderDate?: string
  saleType?: number
  supplementType?: string
  generationMethod?: string
  sourceOrder?: string
  orderSource?: number
  expectedShipTime?: string
  customerId: number
  warehouseId?: number
  salesmanId?: number
  deptId?: number
  // 金额
  productAmount?: number
  promoDiscount?: number
  couponAmount?: number
  directDiscount?: number
  discountAmount?: number
  otherFee?: number
  billAmount?: number
  shippingFee?: number
  // 数量
  totalQuantity?: number
  // 履约
  originalOrderId?: number
  originalOrderNo?: string
  // 备注
  remark?: string
  buyerRemark?: string
  orderRemark?: string
  // 收货信息
  receiverName?: string
  receiverPhone?: string
  shippingAddress?: string
  // 物流冗余
  logisticsCompany?: string
  freightPayer?: string
  waybillNo?: string
  deliveryMethod?: string
  deliveryRoute?: string
  deliveryRouteId?: number
  settlementMethod?: string
  region?: string
  summary?: string
  attachment?: string
  // 自定义字段
  extNum1?: number
  extNum2?: number
  extText1?: string
  extText2?: string
  extText3?: string
  footerExtText1?: string
  footerExtText2?: string
  // 子表数据
  partnerInfo?: PartnerSnapshot
  deliveryAddresses?: DeliveryAddress[]
  settlementInfo?: Settlement
  logisticsInfoList?: Logistics[]
  deposits?: Deposit[]
  memberInfo?: PointsJournal
  extInfoData?: ExtInfo
  items?: SaleOrderItemDTO[]
}

// ═══════════════════════════════════════════
// 查询参数 (与后端 Controller 搜索字段对齐)
// ═══════════════════════════════════════════
export interface SaleOrderQuery {
  tenantId?: number
  pageNum?: number
  pageSize?: number
  // 基础
  orderNo?: string
  customerId?: number
  customerName?: string
  salesmanId?: number
  salesmanName?: string
  status?: number
  startDate?: string
  endDate?: string
  // 搜索扩展字段
  deliveryRoute?: string
  supplementType?: string
  generationMethod?: string
  settlementMethod?: string
  warehouseName?: string
  warehouseId?: number
  deptName?: string
  deptId?: number
  promoterName?: string
  region?: string
  productName?: string
  orderRemark?: string
  productBrand?: string
  industryCategory?: string
  creatorName?: string
  auditorName?: string
  submitterName?: string
  receiverName?: string
  receiverPhone?: string
  logisticsCompany?: string
  waybillNo?: string
  saleType?: number
  deliveryMethod?: string
  driverName?: string
  deliveryVehicle?: string
  buyerRemark?: string
  summary?: string
  shipDateStart?: string
  shipDateEnd?: string
  billAmountMin?: number
  billAmountMax?: number
  extText1?: string
  extText2?: string
  extText3?: string
  footerExtText1?: string
  footerExtText2?: string
  source?: string
  // 拣货/发货专用
  salespersonName?: string
  // 履约专用
  showDetail?: boolean
  hideReturnRelated?: boolean
  showSelected?: boolean
}

// ═══════════════════════════════════════════
// 订单处理中心统计
// ═══════════════════════════════════════════
export interface OrderCenterStats {
  totalOrders: number
  pendingOutbound: number
  pendingShip: number
  outboundCount: number
  shippedCount: number
  completedCount: number
}

// ═══════════════════════════════════════════
// 状态枚举
// ═══════════════════════════════════════════
export enum SaleOrderStatus {
  DRAFT = 0,
  PENDING_REVIEW = 1,
  PENDING_SHIPMENT = 2,
  PARTIAL_SHIPPED = 3,
  SHIPPED = 4,
  COMPLETED = 5,
  CANCELLED = 6,
}

// ═══════════════════════════════════════════
// API
// ═══════════════════════════════════════════
export const saleOrderApi = {
  // 基础 CRUD
  page(params: SaleOrderQuery): Promise<ApiResponse<PageResponse<SaleOrderListItem>>> {
    return request.get('/erp/sale/order/page', params)
  },

  getDetail(id: string): Promise<ApiResponse<SaleOrderDetail>> {
    return request.get(`/erp/sale/order/${id}`)
  },

  create(data: SaleOrderCreateDTO): Promise<ApiResponse<string>> {
    return request.post('/erp/sale/order', data)
  },

  update(id: string, data: SaleOrderCreateDTO): Promise<ApiResponse<void>> {
    return request.put(`/erp/sale/order/${id}`, data)
  },

  delete(id: string): Promise<ApiResponse<void>> {
    return request.delete(`/erp/sale/order/${id}`)
  },

  batchDelete(ids: string[]): Promise<ApiResponse<void>> {
    return request.delete('/erp/sale/order/batch', { data: ids })
  },

  // 审批流程
  submit(id: string): Promise<ApiResponse<void>> {
    return request.post(`/erp/sale/order/${id}/submit`)
  },

  approve(id: string): Promise<ApiResponse<void>> {
    return request.post(`/erp/sale/order/${id}/approve`)
  },

  reject(id: string, reason: string): Promise<ApiResponse<void>> {
    return request.post(`/erp/sale/order/${id}/reject`, null, { params: { reason } })
  },

  cancel(id: string, reason?: string): Promise<ApiResponse<void>> {
    return request.post(`/erp/sale/order/${id}/cancel`, null, { params: { reason } })
  },

  confirmShipment(id: string, warehouseId: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/sale/order/${id}/ship`, null, { params: { warehouseId } })
  },

  recordPayment(id: string, amount: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/sale/order/${id}/payment`, null, { params: { amount } })
  },

  getPending(tenantId: number): Promise<ApiResponse<SaleOrderListItem[]>> {
    return request.get('/erp/sale/order/pending', { tenantId })
  },

  // 统计
  getStats(tenantId?: number): Promise<ApiResponse<Record<string, number>>> {
    return request.get('/erp/sale/order/stats', { tenantId })
  },

  // ═══ 订单处理中心 ═══
  centerStats(params: SaleOrderQuery): Promise<ApiResponse<OrderCenterStats>> {
    return request.get('/erp/sale/order/center/stats', params)
  },

  centerPageByDoc(params: SaleOrderQuery): Promise<ApiResponse<PageResponse<SaleOrderListItem>>> {
    return request.get('/erp/sale/order/center/page-by-doc', params)
  },

  centerGroupByDate(params: SaleOrderQuery): Promise<ApiResponse<Record<string, unknown>[]>> {
    return request.get('/erp/sale/order/center/group-by-date', params)
  },

  centerGroupByRoute(params: SaleOrderQuery): Promise<ApiResponse<Record<string, unknown>[]>> {
    return request.get('/erp/sale/order/center/group-by-route', params)
  },

  centerGroupByCustomer(params: SaleOrderQuery): Promise<ApiResponse<Record<string, unknown>[]>> {
    return request.get('/erp/sale/order/center/group-by-customer', params)
  },

  centerFulfillmentPage(params: SaleOrderQuery): Promise<ApiResponse<PageResponse<SaleOrderListItem>>> {
    return request.get('/erp/sale/order/center/fulfillment-page', params)
  },

  centerFulfillmentOverview(params: SaleOrderQuery): Promise<ApiResponse<Record<string, number>>> {
    return request.get('/erp/sale/order/center/fulfillment-overview', params)
  },

  pendingReviewPage(params: SaleOrderQuery): Promise<ApiResponse<PageResponse<SaleOrderListItem>>> {
    return request.get('/erp/sale/order/center/pending-review', params)
  },

  pickingShippingPage(params: SaleOrderQuery): Promise<ApiResponse<PageResponse<SaleOrderListItem>>> {
    return request.get('/erp/sale/order/center/picking-shipping', params)
  },

  // 导出
  export(params: SaleOrderQuery): Promise<ApiResponse<SaleOrderListItem[]>> {
    return request.get('/erp/sale/order/export', params)
  },

  // 信用额度
  getCustomerCredit(customerId: number): Promise<ApiResponse<Record<string, any>>> {
    return request.get(`/erp/sale/order/customer-credit/${customerId}`)
  },

  // 订金余额
  getCustomerDeposits(customerId: number): Promise<ApiResponse<Record<string, any>[]>> {
    return request.get(`/erp/sale/order/customer-deposits/${customerId}`)
  },

  // ═══ 库存查询 ═══
  getStockDetail(productId: number, warehouseId: number): Promise<ApiResponse<Record<string, any>>> {
    return request.get(`/erp/stock/${productId}/${warehouseId}`)
  },

  // ═══ 批量导入 ═══
  batchImport(file: File): Promise<ApiResponse<string>> {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/erp/sale/order/import', formData)
  },

  // ═══ 商品汇总 ═══
  productSummary(params: SaleOrderQuery): Promise<ApiResponse<Record<string, any>[]>> {
    return request.get('/erp/sale/order/product-summary', params)
  },

  // ═══ 批量更新物流备注 ═══
  batchLogisticsRemark(ids: string[], remark: string): Promise<ApiResponse<void>> {
    return request.post('/erp/sale/order/batch-logistics-remark', { ids, remark })
  },
}
