import axios from 'axios'
import request from '@/utils/request'
import { getToken } from '@/utils/tokenRefresher'

/**
 * 分析报表域 API 封装
 *
 * 覆盖「分析」菜单下各报表页使用的后端端点。
 * 响应经 request 拦截器解包（code===200 时直接返回 data），因此：
 *  - MyBatis-Plus 分页端点 → PageResult<T>（records/total/current/size）
 *  - docquery 风格分页端点 → DocQueryPage<T>（list/total/page/size，可选 summary）
 *  - 聚合端点 → 聚合 DTO 本身
 *
 * 已有封装直接重导出复用，不重复造：
 *  stockApi / batchApi / preOrderApi / saleOrderApi（./erp）
 *  receivableApi / payableApi / capitalFlowApi / reportApi（./finance）
 *  dashboardApi（./dashboard）、shopUserApi（./erp/mall）
 *  invoiceApi（./crm）、feeStatisticsApi（./erp/expense）
 */

// ── 重导出已有封装 ──────────────────────────────────────
export { stockApi, batchApi, preOrderApi, saleOrderApi } from './erp'
export { receivableApi, payableApi, capitalFlowApi, reportApi } from './finance'
export { dashboardApi } from './dashboard'
export { shopUserApi } from './erp/mall'
export { invoiceApi } from './crm'
export { feeStatisticsApi } from './erp/expense'
// 批次查询专用封装：后端 /erp/batch-sn 返回 BatchApiResponse（code 为字符串 "SUCCESS"），
// 标准 request 拦截器（仅认数字 code===200）会误判为失败；
// ./erp/batch 中的实现用原生 axios 自行解包，是仓库内既定的兼容方式。
export { batchApi as batchSnApi } from './erp/batch'
export type { BatchNumber } from './erp/batch'

// ── 通用类型 ────────────────────────────────────────────

/** docquery 风格分页响应（{list,total,page,size,summary:{amount}}） */
export interface DocQueryPage<T = any, S = any> {
  list: T[]
  total: number
  page: number
  size: number
  /** 合计行口径：对当前过滤范围求和 */
  summary?: S
}

/** docquery 分页查询参数（逐项对应对标查询区） */
export interface DocQueryParams {
  docType?: string
  docNo?: string
  partnerName?: string
  startDate?: string
  endDate?: string
  /** 仓库模糊 */
  warehouseName?: string
  /** 所属区域模糊 */
  region?: string
  /** 经手人模糊 */
  handlerName?: string
  /** 部门模糊 */
  departmentName?: string
  /** 制单人模糊 */
  creatorName?: string
  /** 单据备注模糊 */
  remark?: string
  /** 来源订单号模糊 */
  sourceOrderNo?: string
  /** 账期比较符：ge(≥) / le(≤) / eq(=) / gt(>) / lt(<) */
  accountPeriodOp?: string
  /** 账期天数（收/付款截止日 − 单据日期） */
  accountPeriodDays?: number
  /** 显示红冲：false（默认）排除已取消单据 */
  includeReversed?: boolean
  /** 排序字段：bizDate（默认）/ amount */
  sortField?: string
  /** 排序方向：desc（默认）/ asc */
  sortOrder?: string
  page?: number
  size?: number
}

/** 日期范围查询参数 */
export interface DateRangeParams {
  startDate?: string
  endDate?: string
}

// ═══ 综合单据查询（/docquery） ═══

/** 经营历程/待审批/草稿 单据行（23 列口径，见 DocQueryService 列说明） */
export interface DocHistoryItem {
  docTypeCode: string
  docType: string
  /** 源单据主键（雪花 ID，务必按字符串使用，勿转 Number） */
  docId: string
  docNo: string
  bizDate: string
  partnerName: string
  amount: number
  status: string
  statusText: string
  createBy: string
  creatorName: string
  sourceOrderNo: string
  warehouseName: string
  region: string
  handlerName: string
  departmentName: string
  bookkeeperName: string
  summary: string
  remark: string
  hasAttachment: boolean
  createTime: string
  bookkeepingTime: string
  printCount: number
  accountPeriodDays: number
}

/** 待审批单据按类型计数 */
export interface PendingDocSummaryItem {
  docTypeCode: string
  docType: string
  count: number
}

/** 综合单据合计行 */
export interface DocQuerySummary {
  amount: number
}

export const docQueryApi = {
  /** 经营历程分页（全部状态的各类单据，按单据日期倒序；默认排除已取消） */
  businessHistoryPage(params?: DocQueryParams): Promise<DocQueryPage<DocHistoryItem, DocQuerySummary>> {
    return request.get('/docquery/business-history/page', params)
  },
  /** 待审批单据分页（pendingSummary 返回每类待审批数量） */
  pendingDocsPage(params?: DocQueryParams): Promise<
    DocQueryPage<DocHistoryItem, DocQuerySummary> & { pendingSummary?: PendingDocSummaryItem[] }
  > {
    return request.get('/docquery/pending-docs/page', params)
  },
  /** 业务草稿分页 */
  draftDocsPage(params?: DocQueryParams): Promise<DocQueryPage<DocHistoryItem, DocQuerySummary>> {
    return request.get('/docquery/draft-docs/page', params)
  }
}

/**
 * 综合单据行级 / 批量动作
 *
 * 按单据类型分发到各业务域**既有**端点（能力矩阵见 views/analytics/shared/docTypes.ts），
 * 不新建审批通道、不直改状态列。
 */
export const docActionApi = {
  /** 提交记账 */
  submit(base: string, id: string) {
    return request.post(`${base}/${id}/submit`)
  },
  /** 审核通过 */
  approve(base: string, id: string, note?: string) {
    return request.post(`${base}/${id}/approve`, null, note ? { params: { note } } : undefined)
  },
  /** 驳回（reason 为驳回原因） */
  reject(base: string, id: string, reason?: string) {
    return request.post(`${base}/${id}/reject`, null, reason ? { params: { reason } } : undefined)
  },
  /** 删除草稿 */
  remove(base: string, id: string) {
    return request.delete(`${base}/${id}`)
  },
  /** 复制 */
  copy(base: string, id: string) {
    return request.post(`${base}/${id}/copy`)
  },
  /** 红冲 / 作废 */
  cancel(base: string, id: string, reason?: string) {
    return request.post(`${base}/${id}/cancel`, null, reason ? { params: { reason } } : undefined)
  },
  /**
   * 更新单据备注（走各域 PUT /{id}；MyBatis-Plus 默认 NOT_NULL 策略只更新非 null 字段，
   * 故仅传 remark 不会覆盖其余字段 —— 但本次也回传 id 以兼容按 id 校验的实现）
   */
  updateRemark(base: string, id: string, remark: string) {
    return request.put(`${base}/${id}`, { id, remark })
  },
  /**
   * 费用申请单审批（JPA 审批流，入参为 applicationId + action，不适用 /{id}/approve 约定）
   */
  expenseApproval(applicationId: string, action: 'APPROVE' | 'REJECT', comment?: string) {
    return request.post('/erp/expense/approval/process', { applicationId, action, comment })
  }
}

// ═══ 库存分析报表（/erp/stock） ═══

/** 进销存汇总行（每商品+仓库一行） */
export interface InvSummaryItem {
  productId: number
  productCode: string
  productName: string
  warehouseId: number
  warehouseName: string
  openingQty: number
  openingAmt: number
  inQty: number
  inAmt: number
  outQty: number
  outAmt: number
  closingQty: number
  closingAmt: number
}

/** 库存变动流水行 */
export interface StockFlowItem {
  moveTime: string
  docType: string
  docTypeName: string
  docNo: string
  productId: number
  productCode: string
  productName: string
  warehouseId: number
  warehouseName: string
  qty: number
  unitCost: number
  amount: number
  balanceAfter: number
  operatorId: number
  operatorName: string
}

/** 采购准备分析汇总 */
export interface PurchasePrepAnalysis {
  alertProductCount: number
  outOfStockSkuCount: number
  inTransitOrderCount: number
  inTransitQuantity: number
  inTransitAmount: number
  suggestReplenishAmount: number
}

export interface StockReportPageParams extends DateRangeParams {
  pageNum?: number
  pageSize?: number
  warehouseId?: number
  keyword?: string
  productId?: number
  docType?: string
}

/** 库存预警商品行（/erp/stock/alert，Stock 实体） */
export interface StockAlertItem {
  id: number
  productId: number
  productCode: string
  productName: string
  warehouseId: number
  warehouseName: string
  quantity: number
  availableQuantity: number
  frozenQuantity: number
  safetyStock: number
  unit: string
  unitPrice: number
}

/** 库存预警补货行（/erp/stock/alert-replenish/page，按商品×仓库一行，25列） */
export interface StockAlertReplenishItem {
  id: string
  warehouseId: number
  warehouseCode: string
  warehouseName: string
  productId: number
  productName: string
  /** 货号（优先 product_code_alias，无则回退 product_code） */
  productCode: string
  brand: string
  weight: number | null
  volume: number | null
  taste: string
  model: string
  barcode: string
  specification: string
  origin: string
  unit: string
  /** 预警类型：缺货/下限预警/超储/正常 */
  alertType: string
  /** 缺货数量 = 库存上限 + 待发货 - 账面库存 - 待收货 */
  shortageQty: number
  maxStock: number
  minStock: number
  remark: string
  pendingQty: number
  bookQty: number
  inTransitQty: number
  lastPurchaseDate: string
  lastSupplierName: string
  lastPurchasePrice: number
}

/** 缺货补货行（按商品×仓库一行，16列） */
export interface ShortageReplenishItem {
  /** 行主键（productId-warehouseId 组合） */
  id: string
  warehouseId: number
  warehouseName: string
  productId: number
  image: string
  productName: string
  /** 商品货号 */
  productCode: string
  specification: string
  model: string
  origin: string
  brand: string
  unit: string
  /** 订单数量 */
  orderQty: number
  /** 价税合计 */
  amountWithTax: number
  /** 已发货数量 */
  shippedQty: number
  /** 待发货数量 */
  unshippedQty: number
  /** 待收货数量 */
  inTransitQty: number
  /** 账面库存 */
  bookQty: number
  /** 缺货数量 */
  shortageQty: number
  remark: string
  supplierName: string
}

/** 智能补货行（每商品一行，25列） */
export interface SmartReplenishItem {
  /** 行主键（productId） */
  id: string
  productId: number
  image: string
  productName: string
  /** 货号（优先 product_code_alias，无则回退 product_code） */
  productCode: string
  unit: string
  barcode: string
  model: string
  origin: string
  brand: string
  remark: string
  specification: string
  /** 销售数量（区间内销售订单明细汇总） */
  salesQty: number
  /** 销售金额 */
  salesAmount: number
  /** 采购金额 */
  purchaseAmount: number
  /** 日均销量 = 销售数量 / 区间天数 */
  avgDailySales: number
  /** 备货天数 */
  stockDays: number
  /** 待收货数量（采购在途） */
  inTransitQty: number
  /** 待发货数量（销售出库未发货） */
  pendingShipQty: number
  /** 采购数量 */
  purchaseQty: number
  /** 账面库存 */
  bookQty: number
  /** 账面库存换算结果 */
  bookQtyConverted: number
  /** 计划采购数量 = 备货天数×日均销量+待发货-待收货-账面库存 */
  planPurchaseQty: number
  /** 可用库存 */
  availableQty: number
  /** 可用库存换算结果 */
  availableQtyConverted: number
  /** 最近销售日期 */
  lastSaleDate: string
  /** 最近进货日期 */
  lastPurchaseDate: string
}

export const stockReportApi = {
  /** 进销存汇总分页（期初/入库/出库/结存） */
  invSummaryPage(params?: StockReportPageParams) {
    return request.get('/erp/stock/inv-summary/page', params) as Promise<{ records: InvSummaryItem[]; total: number }>
  },
  /** 库存变动流水分页 */
  flowPage(params?: StockReportPageParams) {
    return request.get('/erp/stock/flow/page', params) as Promise<{ records: StockFlowItem[]; total: number }>
  },
  /** 采购准备分析汇总 */
  prepAnalysis(params?: { warehouseId?: number }): Promise<PurchasePrepAnalysis> {
    return request.get('/erp/stock/prep-analysis', params)
  },
  /** 库存预警商品列表（现存量 <= 安全库存，非分页） */
  alertList(): Promise<StockAlertItem[]> {
    return request.get('/erp/stock/alert')
  },
  /** 库存预警补货分页（按商品×仓库一行，25列；支持仓库/关键字/品牌/供应商/备注/只显示下限预警/分类树） */
  alertReplenishPage(params?: {
    pageNum?: number
    pageSize?: number
    warehouseId?: number
    keyword?: string
    brand?: string
    supplierName?: string
    remark?: string
    onlyLowStock?: boolean
    categoryId?: number
  }): Promise<{ records: StockAlertReplenishItem[]; total: number }> {
    return request.get('/erp/stock/alert-replenish/page', params)
  },
  /** 缺货补货分页（按商品×仓库一行，16列；支持日期/商品/仓库/客户/供应商/经手人/单据状态/订单来源/缺货数量口径/仅显示缺货/分类树） */
  shortageReplenishPage(params?: {
    pageNum?: number
    pageSize?: number
    orderStatus?: number
    startDate?: string
    endDate?: string
    customerId?: number
    salesmanId?: number
    warehouseId?: number
    orderSource?: number
    productKeyword?: string
    supplierName?: string
    categoryId?: number
    shortageMode?: number
    onlyShortage?: boolean
  }): Promise<{ records: ShortageReplenishItem[]; total: number }> {
    return request.get('/erp/stock/shortage-replenish/page', params)
  },
  /** 智能补货分页（每商品一行，25列；支持销售日期/备货天数/商品/仓库/供应商/分类树/计划采购数量下限） */
  smartReplenishPage(params?: {
    pageNum?: number
    pageSize?: number
    startDate?: string
    endDate?: string
    stockDays?: number
    warehouseId?: number
    productKeyword?: string
    supplierName?: string
    categoryId?: number
    minPlanQty?: number
  }): Promise<{ records: SmartReplenishItem[]; total: number }> {
    return request.get('/erp/stock/smart-replenish/page', params)
  }
}

// ═══ 财务分析（/erp/finance） ═══

/** 往来单位余额行 */
export interface PartnerBalanceItem {
  partnerType: string
  partnerId: string
  partnerName: string
  receivableBalance: number
  payableBalance: number
  preReceiptBalance: number
  prePaymentBalance: number
  netBalance: number
  lastBizDate: string
}

/** 收款统计汇总 */
export interface CollectionStatsSummary {
  receiptCount: number
  totalAmount: number
  cashAmount: number
  bankAmount: number
  otherAmount: number
}

/** 收款统计分组明细 */
export interface CollectionStatsDetail extends CollectionStatsSummary {
  groupKey: string
  groupName: string
}

/** 收款统计（{summary, details} 聚合风格） */
export interface CollectionStats {
  summary: CollectionStatsSummary
  details: CollectionStatsDetail[]
}

export const financeAnalyticsApi = {
  /** 往来单位余额分页（应收/应付/预收/预付/净额） */
  partnerBalancePage(params?: {
    partnerType?: string
    keyword?: string
    onlyNonZero?: boolean
    page?: number
    size?: number
  }) {
    return request.get('/erp/finance/partner-balance/page', params) as Promise<{ records: PartnerBalanceItem[]; total: number }>
  },
  /** 收款统计（按 day/week/month/staff/customer 分组） */
  collectionStats(params?: DateRangeParams & { groupBy?: string }): Promise<CollectionStats> {
    return request.get('/erp/finance/collection-stats', params)
  }
}

// ═══ 销售分析（/erp/sale） ═══

/** 客户活跃度分析行 */
export interface CustomerActiveItem {
  customerId: number
  customerName: string
  recentOrderCount: number
  recentOrderAmount: number
  lastOrderTime: string
  totalOrderCount: number
  totalOrderAmount: number
  followCount: number
  activityLevel: string
}

/** 促销活动分析 */
export interface PromotionAnalysis {
  countByStatus: Record<string, any>[]
  countByType: Record<string, any>[]
  monthlyDistribution: Record<string, any>[]
  activities: {
    id: number
    name: string
    type: string
    status: string
    startTime: string
    endTime: string
    discountRate: number
    reductionAmount: number
    createTime: string
  }[]
  discountOverview: {
    orderCount: number
    totalOrderAmount: number
    discountedOrderCount: number
    totalDiscountAmount: number
  }
}

export const saleAnalyticsApi = {
  /** 客户活跃度分析分页 */
  customerActivePage(params?: {
    page?: number
    size?: number
    days?: number
    activeDays?: number
    silentDays?: number
    keyword?: string
  }) {
    return request.get('/erp/sale/analysis/customer-active/page', params) as Promise<{ records: CustomerActiveItem[]; total: number }>
  },
  /** 促销活动分析 */
  promotionAnalysis(params?: DateRangeParams): Promise<PromotionAnalysis> {
    return request.get('/erp/sale/promotion/analysis', params)
  }
}

// ═══ 商城交易分析（/erp/mall/admin） ═══

/** 商城交易分析 */
export interface MallTradeAnalysis {
  summary: {
    totalOrderCount: number
    totalGmv: number
    avgOrderAmount: number
    refundOrderCount: number
    refundRate: number
  }
  daily: {
    day: string
    orderCount: number
    gmv: number
    avgOrderAmount: number
  }[]
  paymentStatusDistribution: {
    paymentStatus: number
    paymentStatusName: string
    count: number
  }[]
}

export const mallAnalyticsApi = {
  /** 商城交易分析（汇总 + 按日明细 + 支付状态分布） */
  tradeAnalysis(params?: DateRangeParams): Promise<MallTradeAnalysis> {
    return request.get('/erp/mall/admin/trade-analysis', params)
  }
}

// ═══ 提成管理（/erp/marketing/commission） ═══

/** 员工提成汇总行 */
export interface StaffCommissionSummaryItem {
  referrerId: number
  staffName: string
  recordCount: number
  orderCount: number
  totalOrderAmount: number
  totalCommissionAmount: number
  settledCommissionAmount: number
  settledCount: number
  unsettledCommissionAmount: number
  unsettledCount: number
}

export const commissionApi = {
  /** 员工提成汇总分页 */
  staffSummaryPage(params?: DateRangeParams & { page?: number; size?: number; keyword?: string }) {
    return request.get('/erp/marketing/commission/staff-summary/page', params) as Promise<{ records: StaffCommissionSummaryItem[]; total: number }>
  },
  /** 提成规则分页 */
  rulePage(params?: { pageNum?: number; pageSize?: number }) {
    return request.get('/erp/marketing/commission/rule/page', params)
  },
  /** 提成记录分页 */
  recordPage(params?: { pageNum?: number; pageSize?: number }) {
    return request.get('/erp/marketing/commission/record/page', params)
  }
}

// ═══ 采购分析（/erp/purchase/order/statistics + /erp/purchase/inbound + doc-query） ═══

/** 采购订单逐日统计 */
export interface PurchaseDailyStatistic {
  date: string
  orderCount: number
  itemCount: number
  totalAmount: number
  approvedCount: number
  deliveredCount: number
}

/** 供应商采购额排行 */
export interface PurchaseSupplierStatistic {
  supplierId: number
  supplierName: string
  orderCount: number
  itemCount: number
  totalAmount: number
  averageAmount: number
  onTimeDeliveryRate: number
}

/** 采购订单统计（/erp/purchase/order/statistics 响应，ApiResponse 解包后为 DTO 本体） */
export interface PurchaseOrderStatistics {
  totalOrders?: number
  totalItems?: number
  totalAmount?: number
  totalTaxAmount?: number
  totalDiscountAmount?: number
  totalFinalAmount?: number
  ordersByStatus?: Record<string, number>
  amountByStatus?: Record<string, number>
  pendingApprovalCount?: number
  overdueApprovalCount?: number
  onTimeDeliveryRate?: number
  delayedDeliveryCount?: number
  dailyStatistics?: PurchaseDailyStatistic[]
  topSuppliersByAmount?: PurchaseSupplierStatistic[]
}

/** 采购入库单行（/erp/purchase/inbound/page） */
export interface PurchaseInboundItemVO {
  id: number
  inboundNo: string
  orderNo: string
  supplierName: string
  inboundDate: string
  status: number
  statusDesc: string
  totalQuantity: number
  totalAmount: number
  warehouseName: string
}

export const purchaseAnalyticsApi = {
  /**
   * 采购订单统计（startDate/endDate 必填，yyyy-MM-dd）。
   *
   * 2026-09-20 从已下线的旧实现 `/erp/purchase-orders/statistics` 迁到采购模块的统一实现，
   * 参数与返回结构不变。
   */
  orderStatistics(params: { tenantId: number; startDate: string; endDate: string }): Promise<PurchaseOrderStatistics> {
    return request.get('/erp/purchase/order/statistics', params)
  },
  /** 采购入库单分页（裸 Page 响应：records/total，可用于入库汇总） */
  inboundPage(params?: { pageNum?: number; pageSize?: number; keyword?: string; status?: number }) {
    return request.get('/erp/purchase/inbound/page', params) as Promise<{ records: PurchaseInboundItemVO[]; total: number }>
  },
  /** 采购单据分页（doc-query，分页参数为 current/size） */
  docPage(params?: Record<string, any>) {
    return request.get('/erp/purchase/order/doc-query/page', params) as Promise<{ records: any[]; total: number }>
  }
}

// ═══ CRM 营销分析（/api/crm/marketing） ═══

/** 营销活动行（/crm/marketing/page） */
export interface MarketingCampaignItem {
  id: number
  campaignCode: string
  campaignName: string
  campaignType: number
  status: number
  statusDesc: string
  startDate: string
  endDate: string
  budget: number
  actualCost: number
  expectedRevenue: number
  actualRevenue: number
  actualLeads: number
  actualOrders: number
  targetCustomerCount: number
  reachedCustomerCount: number
  respondedCustomerCount: number
  convertedCustomerCount: number
  ownerName: string
  createTime: string
}

export const crmMarketingApi = {
  /**
   * 营销活动分页（裸 Page 响应：records/total）
   * 注：/crm/marketing/statistics 返回裸 Map（无统一响应包装），
   * 会被前端响应拦截器按失败处理，故统计卡片/分布图由本接口聚合得出。
   */
  page(params?: { pageNum?: number; pageSize?: number; keyword?: string; campaignType?: number; status?: number }) {
    return request.get('/crm/marketing/page', params) as Promise<{ records: MarketingCampaignItem[]; total: number }>
  }
}

// ═══ 费用统计（/erp/expense/statistics） ═══

/** 费用统计聚合结果（后端 statistics/page 返回的 data Map） */
export interface ExpenseStatisticsResult {
  totalAmount: number
  approvedAmount: number
  pendingAmount: number
  rejectedAmount: number
  expenseCount: number
  averageAmount: number
  /** 按部门聚合：部门名 → 金额 */
  byDepartment: Record<string, number>
  /** 按类型聚合：类型中文名 → 金额 */
  byType: Record<string, number>
  page?: number
  size?: number
}

/**
 * 费用统计分析封装。
 * 后端 cn.aiedge.erp.expense.dto.ApiResponse 的 code 为字符串 "200"，
 * 标准 request 拦截器（仅认数字 code===200）会误判为失败并弹错误提示，
 * 因此参照 ./erp/batch 的既定做法：原生 axios + getToken 自行解包。
 */
export const expenseAnalyticsApi = {
  /** 费用统计分页（聚合口径：总额/状态金额/按部门/按类型；page/size 仅回显） */
  async statisticsPage(params?: DateRangeParams & {
    departmentId?: string
    expenseType?: string
    page?: number
    size?: number
  }): Promise<ExpenseStatisticsResult> {
    const response = await axios.get('/api/erp/expense/statistics/page', {
      headers: getToken() ? { Authorization: `Bearer ${getToken()}` } : {},
      params
    })
    const body = response.data as { code?: string; success?: boolean; message?: string; data?: ExpenseStatisticsResult }
    if (body && (body.code === '200' || body.success === true)) {
      return (body.data || {}) as ExpenseStatisticsResult
    }
    throw new Error(body?.message || '请求失败')
  }
}

// ═══ 发票统计（/erp/invoice） ═══

/** 发票统计汇总（InvoiceService.InvoiceStatistics，仅汇总无按日维度） */
export interface InvoiceStatsSummary {
  totalCount: number
  totalAmount: number
  totalTax: number
  totalPaid: number
  totalUnpaid: number
  overdueCount: number
  overdueAmount: number
  pendingCount: number
  pendingAmount: number
}

/** 发票记录行（Invoice 实体） */
export interface InvoiceRecordItem {
  id: number
  invoiceNumber: string
  invoiceType: string
  invoiceStatus: string
  paymentStatus: string
  invoiceDate: string
  dueDate: string
  customerId: number
  customerName: string
  totalAmount: number
  taxAmount: number
  paidAmount: number
  unpaidAmount: number
}

export const invoiceAnalyticsApi = {
  /**
   * 按日期范围查询发票列表（后端返回原始数组，拦截器直接透传）
   * 注意：startDate/endDate 后端为必填
   */
  dateRangeList(params: { startDate: string; endDate: string }): Promise<InvoiceRecordItem[]> {
    return request.get('/erp/invoice/date-range', params)
  }
}

// ═══ 费用统计分析（/erp/expense/statistics） ═══

/** 费用分布统计结果（byDepartment/byType 为 名称→金额 的映射） */
export interface FeeDistributionResult {
  byDepartment?: Record<string, number>
  byType?: Record<string, number>
  totalAmount: number
  expenseCount: number
}

/**
 * 费用统计分布接口
 * 注意：./erp/expense 中 feeStatisticsApi.getByDepartment/getByType 的 year/month
 * 参数与后端 startDate/endDate 不匹配，此处按后端真实签名另封装
 */
export const expenseStatisticsApi = {
  /** 按部门分布（{byDepartment, totalAmount, expenseCount}） */
  byDepartment(params?: DateRangeParams): Promise<FeeDistributionResult> {
    return request.get('/erp/expense/statistics/by-department', params)
  },
  /** 按费用类型分布（{byType, totalAmount, expenseCount}） */
  byType(params?: DateRangeParams): Promise<FeeDistributionResult> {
    return request.get('/erp/expense/statistics/by-type', params)
  }
}
