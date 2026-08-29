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

  // 获取订单明细
  getItems(id: number): Promise<ApiResponse<any[]>> {
    return request.get(`/erp/purchase/order/${id}/items`)
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
  },

  // 导出Excel
  exportData(params: Record<string, any>): Promise<Blob> {
    return request.get('/erp/purchase/order/export', params, { responseType: 'blob' })
  }
}

// ═══════════════════════════════════════════
// 采购单据/明细查询
// 后端：PurchaseDocQueryController / PurchaseDetailQueryController
// 响应 ApiResponse<Page<T>>，拦截器 code===200 解包后为 { records, total, current, size }
// 注意：分页参数为 current/size；dateStart/dateEnd 为 LocalDateTime（ISO 格式）
// ═══════════════════════════════════════════

/** 采购单据状态（与后端 OrderStatus 枚举一致） */
export enum PurchaseDocStatus {
  DRAFT = 0,
  PENDING_APPROVAL = 1,
  APPROVED = 2,
  ISSUED = 3,
  IN_PROGRESS = 4,
  PARTIAL_RECEIVED = 5,
  COMPLETED = 6,
  CANCELLED = 7,
}

/** 采购单据查询条件（与后端 PurchaseDocQueryDTO 对齐） */
export interface PurchaseDocQueryParams {
  current?: number
  size?: number
  /** 单据日期起（ISO 日期时间，如 2026-07-01T00:00:00） */
  dateStart?: string
  /** 单据日期止（ISO 日期时间） */
  dateEnd?: string
  orderNo?: string
  supplierName?: string
  purchaserName?: string
  deptName?: string
  createByName?: string
  warehouseName?: string
  status?: number
  remark?: string
  submitterName?: string
  auditorName?: string
  extText1?: string
  extText2?: string
  extText3?: string
}

/** 采购单据查询行（与后端 PurchaseOrderListDTO 对齐，39列取常用） */
export interface PurchaseDocItem {
  id: number
  orderDate: string
  orderNo: string
  sourceBillNo: string
  status: number
  warehouseName: string
  supplierName: string
  supplierCode: string
  contactName: string
  contactPhone: string
  contactAddress: string
  supplierRemark: string
  purchaserName: string
  deptName: string
  productAmount: number
  discountAmount: number
  otherExpense: number
  billAmount: number
  settledAmount: number
  expectedReceiveTime: string
  totalQuantity: number
  receivedQuantity: number
  unreceiveQuantity: number
  returnQuantity: number
  returnAmount: number
  weight: number
  volume: number
  remark: string
  summary: string
  attachment: string
  extNum1: number
  extNum2: number
  extText1: string
  extText2: string
  extText3: string
  submitTime: string
  createByName: string
  submitterName: string
  auditorName: string
  printCount: number
}

/** 采购明细查询条件（与后端 PurchaseDetailQueryDTO 对齐） */
export interface PurchaseDetailQueryParams {
  current?: number
  size?: number
  dateStart?: string
  dateEnd?: string
  orderNo?: string
  sourceBillNo?: string
  productName?: string
  supplierName?: string
  purchaserName?: string
  deptName?: string
  createByName?: string
  auditorName?: string
  status?: number
  warehouseName?: string
  priceStatus?: number
  remark?: string
  itemRemark?: string
  isGift?: number
}

/** 采购明细查询行（与后端 PurchaseDetailListDTO 对齐，59列取常用） */
export interface PurchaseDetailItem {
  itemId: number
  orderId: number
  orderDate: string
  orderNo: string
  status: number
  warehouseName: string
  supplierName: string
  supplierCode: string
  contactName: string
  contactPhone: string
  contactAddress: string
  supplierRemark: string
  purchaserName: string
  deptName: string
  sourceBillNo: string
  productName: string
  itemCode: string
  barcode: string
  specification: string
  model: string
  origin: string
  brand: string
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
  unit: string
  smallUnit: string
  smallUnitQuantity: number
  smallUnitPrice: number
  conversionRelation: string
  convertedQuantity: number
  bigPack: number
  midPack: number
  smallPack: number
  quantity: number
  receivedQuantity: number
  unreceiveQuantity: number
  terminatedQuantity: number
  terminatedAmount: number
  weight: number
  volume: number
  unitPrice: number
  amount: number
  discountRate: number
  discountedUnitPrice: number
  discountedAmount: number
  itemRemark: string
  remark: string
  summary: string
  attachment: string
  createByName: string
  auditorName: string
  createTime: string
  submitTime: string
  printCount: number
}

export const purchaseDocQueryApi = {
  /** 按单据Tab分页查询（采购订单-按单据Tab） */
  docPage(params?: PurchaseDocQueryParams): Promise<PageResponse<PurchaseDocItem>> {
    return request.get('/erp/purchase/order/doc-query/page', params)
  },
  /** 按明细Tab分页查询 */
  detailPage(params?: PurchaseDetailQueryParams): Promise<PageResponse<PurchaseDetailItem>> {
    return request.get('/erp/purchase/order/detail-query/page', params)
  }
}

// ═══════════════════════════════════════════
// 采购单据查询（统一：入库/退货/换货合并）
// 后端：UnifiedPurchaseDocQueryController /api/purchase/doc-query
// 服务采购单据查询页 /purchase/doc-query
// ═══════════════════════════════════════════

/** 统一采购单据类型 */
export enum PurchaseUnifiedDocType {
  INBOUND = 'INBOUND',
  RETURN = 'RETURN',
  EXCHANGE = 'EXCHANGE',
}

/** 统一采购单据查询条件（对应 UnifiedPurchaseDocQueryDTO，18个字段） */
export interface PurchaseDocUnifiedParams {
  current?: number
  size?: number
  /** 单据日期起（yyyy-MM-dd） */
  dateStart?: string
  /** 单据日期止（yyyy-MM-dd） */
  dateEnd?: string
  documentNo?: string
  documentType?: string
  supplierName?: string
  supplierCode?: string
  handlerName?: string
  departmentName?: string
  creatorName?: string
  bookkeeperName?: string
  warehouseName?: string
  warehouseId?: number
  settlementStatus?: string
  status?: number
  sourceOrder?: string
  remark?: string
  extNum1Start?: number
  extNum1End?: number
  extNum2Start?: number
  extNum2End?: number
  extText1?: string
  extText2?: string
  extText3?: string
  showRed?: boolean
}

/** 统一采购单据查询行（对应 UnifiedPurchaseDocumentDTO，37列） */
export interface PurchaseDocUnifiedItem {
  id: number
  documentType: string
  documentDate: string
  documentNo: string
  inboundWarehouse: string
  outboundWarehouse: string
  supplierName: string
  supplierCode: string
  contactName: string
  contactPhone: string
  contactAddress: string
  supplierRemark: string
  sourceOrder: string
  handlerName: string
  departmentName: string
  settlementStatus: string
  purchaseQuantity: number
  amount: number
  discountedAmount: number
  favorableAmount: number
  taxAmount: number
  totalAmountWithTax: number
  totalAmount: number
  fee: number
  discountAmount: number
  extNum1: number
  extNum2: number
  extText1: string
  extText2: string
  extText3: string
  remark: string
  summary: string
  attachment: string
  bookkeeperName: string
  creatorName: string
  bookkeepingTime: string
  createTime: string
  printCount: number
  status: number
  statusDesc: string
  supplierId: number
  warehouseId: number
  weight: number
  volume: number
}

export const purchaseDocUnifiedApi = {
  /** 统一采购单据分页查询 */
  page(params?: PurchaseDocUnifiedParams): Promise<PageResponse<PurchaseDocUnifiedItem>> {
    return request.get('/purchase/doc-query/page', params)
  },
  /** 更新整单备注 */
  updateRemark(docType: string, id: number, remark: string): Promise<ApiResponse<any>> {
    return request.put(`/purchase/doc-query/${docType}/${id}/remark`, { remark })
  }
}

// ═══════════════════════════════════════════
// 智能补货（/erp/stock/replenishment）
// 后端 StockReplenishmentController 返回裸 ResponseEntity<Page>，
// 拦截器对 { records, total } 透传不拆包
// ═══════════════════════════════════════════

/** 补货建议优先级 */
export type ReplenishPriority = 'HIGH' | 'MEDIUM' | 'LOW'
/** 补货建议状态：PENDING 待处理 / ORDERED 已转采购订单 / IGNORED 已忽略 */
export type ReplenishStatus = 'PENDING' | 'ORDERED' | 'IGNORED'

/** 补货建议行（与后端 StockReplenishment 实体对齐） */
export interface StockReplenishmentItem {
  id: number
  productCode: string
  productName: string
  productSpec: string
  productUnit: string
  warehouseId: number
  warehouseName: string
  currentQty: number
  safetyStock: number
  shortageQty: number
  avgDailySales: number
  daysOfStock: number
  leadTime: number
  suggestedQty: number
  priority: ReplenishPriority
  reason: string
  status: ReplenishStatus
  supplierId: number
  supplierName: string
  createdOrderNo: string
  remark: string
  createTime: string
  updateTime: string
}

export const replenishmentApi = {
  /** 补货建议分页（keyword/priority/status + pageNum/pageSize） */
  page(params?: { keyword?: string; priority?: string; status?: string; pageNum?: number; pageSize?: number }): Promise<PageResponse<StockReplenishmentItem>> {
    return request.get('/erp/stock/replenishment/list', params)
  },
  /** 扫描库存生成补货建议，返回本次生成的建议列表 */
  generate(): Promise<StockReplenishmentItem[]> {
    return request.post('/erp/stock/replenishment/generate')
  },
  /** 根据补货建议创建采购订单（supplierId 可选，缺省用建议上的供应商） */
  createOrder(id: number, supplierId?: number): Promise<StockReplenishmentItem> {
    return request.post(`/erp/stock/replenishment/${id}/create-order`, supplierId !== null && supplierId !== undefined ? { supplierId } : {})
  },
  /** 忽略补货建议 */
  ignore(id: number, reason?: string): Promise<StockReplenishmentItem> {
    return request.put(`/erp/stock/replenishment/${id}/ignore`, null, { params: { reason } })
  }
}