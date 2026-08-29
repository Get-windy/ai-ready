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

// 换货单（丰富模型，字段按业务实体分组）
export interface PurchaseExchange {
  id: number
  tenantId: number
  exchangeNo: string                    // 换货单号
  exchangeDate: string                  // 单据日期
  status: ExchangeStatus               // 状态
  settleStatus?: string                 // 结算状态
  printCount?: number                   // 打印次数
  attachment?: string                   // 附件
  summary?: string                      // 摘要
  // 供应商快照
  supplierId?: number
  supplierName: string
  supplierCode?: string
  supplierRemark?: string
  contactName?: string
  contactPhone?: string
  contactAddress?: string
  bankName?: string
  bankAccount?: string
  taxNo?: string
  // 仓库快照
  inWarehouseId?: number
  inWarehouseName?: string
  outWarehouseId?: number
  outWarehouseName?: string
  // 职员/部门快照
  handlerId?: number
  handlerName?: string
  deptId?: number
  deptName?: string
  bookkeeperName?: string
  bookkeepingTime?: string
  // 付款/结算
  paymentAccount?: string
  paidAmount?: number
  moreAccounts?: string
  prevPrepaid?: number
  usePrepaid?: number
  prepaidBalance?: number
  prevDebt?: number
  currentDebt?: number
  debtBalance?: number
  paymentDeadline?: string
  // 金额计算链
  productAmount?: number
  totalAmount?: number
  discountAmount?: number
  settledAmount?: number
  // 数量汇总
  inQuantityTotal?: number
  outQuantityTotal?: number
  totalWeight?: number
  totalVolume?: number
  // 源单
  originalOrderId?: number
  originalOrderNo?: string
  // 换货业务
  exchangeReason?: string
  exchangeType?: number
  remark?: string
  // 自定义字段
  extNum1?: number
  extNum2?: number
  extNum3?: number
  extNum4?: number
  extNum5?: number
  extText1?: string
  extText2?: string
  extText3?: string
  extText4?: string
  extText5?: string
  // 制单/审批
  createdBy?: number
  createdByName?: string
  approvedBy?: number
  approvedByName?: string
  approvedTime?: string
  completedTime?: string
  createTime?: string
  updateTime?: string
  // 明细
  items?: PurchaseExchangeItem[]
}

// 换货单明细（仓库类型 1=换入 2=换出）
export interface PurchaseExchangeItem {
  id?: number
  exchangeId?: number
  warehouseType: number               // 1=换入 2=换出
  productId?: number
  productName?: string
  productCode?: string
  barcode?: string
  specification?: string
  model?: string
  origin?: string
  brand?: string
  unit?: string
  image?: string
  location?: string
  area?: string
  availableStock?: number
  stockConverted?: number
  bookStock?: number
  batchBarcode?: string
  productionDate?: string
  shelfLife?: number
  expiryDate?: string
  quantity?: number
  conversionRate?: number
  pieceScatterQty?: number
  largePackage?: number
  mediumPackage?: number
  smallPackage?: number
  recentPurchaseDate?: string
  retailPrice?: number
  wholesalePrice?: number
  unitPrice?: number
  amount?: number
  smallUnit?: string
  smallUnitPrice?: number
  smallUnitQty?: number
  costPrice?: number
  costAmount?: number
  discount?: number
  discountPrice?: number
  discountAmount?: number
  volume?: number
  weight?: number
  isGift?: boolean
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
  extNum4?: number
  extNum5?: number
  extNum6?: number
  extNum7?: number
  extNum8?: number
  extNum9?: number
  extNum10?: number
  extText1?: string
  extText2?: string
  extText3?: string
  extText4?: string
  extText5?: string
  extText6?: string
  extPartner?: number
  extStaff?: number
  extDept?: number
}

// 创建/更新请求体（主表字段 + 明细数组）
export interface PurchaseExchangePayload {
  exchangeDate?: string
  status?: number
  settleStatus?: string
  printCount?: number
  attachment?: string
  summary?: string
  supplierId?: number
  supplierName?: string
  supplierCode?: string
  supplierRemark?: string
  contactName?: string
  contactPhone?: string
  contactAddress?: string
  bankName?: string
  bankAccount?: string
  taxNo?: string
  inWarehouseId?: number
  inWarehouseName?: string
  outWarehouseId?: number
  outWarehouseName?: string
  handlerId?: number
  handlerName?: string
  deptId?: number
  deptName?: string
  bookkeeperName?: string
  paymentAccount?: string
  paidAmount?: number
  moreAccounts?: string
  prevPrepaid?: number
  usePrepaid?: number
  prepaidBalance?: number
  prevDebt?: number
  currentDebt?: number
  debtBalance?: number
  paymentDeadline?: string
  productAmount?: number
  totalAmount?: number
  discountAmount?: number
  settledAmount?: number
  inQuantityTotal?: number
  outQuantityTotal?: number
  totalWeight?: number
  totalVolume?: number
  originalOrderId?: number
  originalOrderNo?: string
  exchangeReason?: string
  exchangeType?: number
  remark?: string
  extNum1?: number
  extNum2?: number
  extNum3?: number
  extNum4?: number
  extNum5?: number
  extText1?: string
  extText2?: string
  extText3?: string
  extText4?: string
  extText5?: string
  items?: PurchaseExchangeItem[]
}

export interface PurchaseExchangeQuery {
  current?: number
  size?: number
  keyword?: string
  supplierId?: number
  inWarehouseId?: number
  outWarehouseId?: number
  handlerId?: number
  status?: ExchangeStatus
  exchangeType?: number
  settleStatus?: string
  startDate?: string
  endDate?: string
}

// 换货审批记录
export interface ExchangeApprovalRecord {
  id: number
  exchangeId: number
  action: string
  actionName: string
  operatorId: number
  operatorName: string
  remark?: string
  createTime: string
}

// API 接口
export const purchaseExchangeApi = {
  page(params: PurchaseExchangeQuery): Promise<ApiResponse<PageResponse<PurchaseExchange>>> {
    return request.get('/erp/purchase/exchange/page', params)
  },
  getById(id: number): Promise<ApiResponse<PurchaseExchange>> {
    return request.get(`/erp/purchase/exchange/${id}`)
  },
  getItems(exchangeId: number): Promise<ApiResponse<PurchaseExchangeItem[]>> {
    return request.get(`/erp/purchase/exchange/${exchangeId}/items`)
  },
  getItemsByWarehouseType(exchangeId: number, warehouseType: number): Promise<ApiResponse<PurchaseExchangeItem[]>> {
    return request.get(`/erp/purchase/exchange/${exchangeId}/items/${warehouseType}`)
  },
  create(data: PurchaseExchangePayload): Promise<ApiResponse<PurchaseExchange>> {
    return request.post('/erp/purchase/exchange', data)
  },
  update(id: number, data: PurchaseExchangePayload): Promise<ApiResponse<PurchaseExchange>> {
    return request.put(`/erp/purchase/exchange/${id}`, data)
  },
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/purchase/exchange/${id}`)
  },
  submit(id: number): Promise<ApiResponse<PurchaseExchange>> {
    return request.post(`/erp/purchase/exchange/${id}/submit`)
  },
  approve(id: number, remark?: string | { remark?: string; approved?: boolean }): Promise<ApiResponse<PurchaseExchange>> {
    const remarkStr = typeof remark === 'string' ? remark : (remark && typeof remark === 'object' ? (remark.remark ?? '') : '')
    return request.post(`/erp/purchase/exchange/${id}/approve`, null, { params: { remark: remarkStr } })
  },
  batchApprove(ids: number[]): Promise<ApiResponse<number>> {
    return request.post('/erp/purchase/exchange/batch-approve', ids)
  },
  reject(id: number, remark: string): Promise<ApiResponse<PurchaseExchange>> {
    return request.post(`/erp/purchase/exchange/${id}/reject`, null, { params: { remark } })
  },
  cancel(id: number, reason?: string): Promise<ApiResponse<PurchaseExchange>> {
    return request.post(`/erp/purchase/exchange/${id}/cancel`, null, { params: { reason } })
  },
  complete(id: number): Promise<ApiResponse<PurchaseExchange>> {
    return request.post(`/erp/purchase/exchange/${id}/complete`)
  },
  print(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/purchase/exchange/${id}/print`)
  },
  batchPrint(ids: number[]): Promise<ApiResponse<void>> {
    return request.post('/erp/purchase/exchange/batch-print', ids)
  },
  getApprovalRecords(exchangeId: number): Promise<ApiResponse<ExchangeApprovalRecord[]>> {
    return request.get(`/erp/purchase/exchange/${exchangeId}/approval-records`)
  },
  getTracking(exchangeId: number): Promise<ApiResponse<{
    exchange: PurchaseExchange
    items: PurchaseExchangeItem[]
    approvalRecords: ExchangeApprovalRecord[]
    timeline: Array<{ time: string; title: string; content: string; status: string }>
  }>> {
    return request.get(`/erp/purchase/exchange/${exchangeId}/tracking`)
  },
  nextNo(): Promise<ApiResponse<string>> {
    return request.get('/erp/purchase/exchange/next-no')
  },
  async export(params: Partial<PurchaseExchangeQuery>): Promise<Blob> {
    const res = await request.get('/erp/purchase/exchange/export', params, { responseType: 'blob' })
    return res as any as Blob
  },
}

export default purchaseExchangeApi
