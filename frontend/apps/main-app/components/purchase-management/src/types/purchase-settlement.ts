// 采购结算相关类型定义

/**
 * 结算状态枚举
 */
export enum SettlementStatus {
  PENDING = 'pending',      // 待结算
  PARTIAL = 'partial',      // 部分结算
  COMPLETED = 'completed',  // 已结算
  CANCELLED = 'cancelled',  // 已取消
  REJECTED = 'rejected'     // 已驳回
}

/**
 * 付款方式枚举
 */
export enum PaymentMethod {
  BANK_TRANSFER = 'bank_transfer',  // 银行转账
  CASH = 'cash',                   // 现金
  CHECK = 'check',                 // 支票
  OTHER = 'other'                  // 其他
}

/**
 * 付款状态枚举
 */
export enum PaymentStatus {
  PENDING = 'pending',      // 待付款
  IN_PROGRESS = 'in_progress', // 付款中
  COMPLETED = 'completed',  // 已付款
  FAILED = 'failed',        // 付款失败
  CANCELLED = 'cancelled'   // 已取消
}

/**
 * 采购结算单接口
 */
export interface PurchaseSettlement {
  id: string
  settlementNumber: string          // 结算单号
  supplierId: string               // 供应商ID
  supplierName: string             // 供应商名称
  totalAmount: number              // 订单总金额
  returnAmount: number             // 退货金额
  settlementAmount: number         // 应结算金额
  paidAmount: number               // 已付金额
  unpaidAmount: number             // 未付金额
  status: SettlementStatus         // 结算状态
  settlementDate?: string          // 结算日期
  dueDate: string                  // 应付款日期
  currency: string                 // 币种
  paymentMethod?: PaymentMethod    // 付款方式
  paymentStatus?: PaymentStatus    // 付款状态
  createdBy: string               // 创建人
  createdAt: string               // 创建时间
  updatedAt: string               // 更新时间
  notes?: string                  // 备注
}

/**
 * 付款记录接口
 */
export interface PaymentRecord {
  id: string
  settlementId: string           // 关联结算单ID
  paymentNumber: string          // 付款单号
  amount: number                 // 付款金额
  paymentMethod: PaymentMethod   // 付款方式
  status: PaymentStatus          // 付款状态
  paymentDate: string            // 付款日期
  bankAccount?: string           // 银行账户
  referenceNumber?: string       // 参考号/支票号
  voucherUrl?: string            // 凭证URL
  createdBy: string             // 创建人
  createdAt: string             // 创建时间
  confirmedBy?: string          // 确认人
  confirmedAt?: string          // 确认时间
}

/**
 * 采购对账单接口
 */
export interface PurchaseReconciliation {
  id: string
  reconciliationNumber: string   // 对账单号
  supplierId: string            // 供应商ID
  supplierName: string          // 供应商名称
  periodStart: string           // 对账期间开始
  periodEnd: string             // 对账期间结束
  totalSettlements: number      // 结算单总数
  totalAmount: number           // 对账总金额
  status: 'pending' | 'confirmed' | 'disputed' | 'completed'  // 对账状态
  discrepancyAmount?: number    // 差异金额
  discrepancyNotes?: string     // 差异说明
  confirmedBy?: string          // 确认人
  confirmedAt?: string          // 确认时间
  createdBy: string            // 创建人
  createdAt: string            // 创建时间
}

/**
 * 结算统计信息接口
 */
export interface SettlementStatistics {
  period: string                              // 统计期间
  totalSettlements: number                    // 总结算单数
  totalAmount: number                         // 总结算金额
  averagePaymentDays: number                  // 平均付款天数
  paymentMethodDistribution: Record<PaymentMethod, number>  // 付款方式分布
  statusDistribution: Record<SettlementStatus, number>      // 状态分布
  topSuppliers: Array<{
    supplierId: string
    supplierName: string
    settlementCount: number
    totalAmount: number
  }>                                          // 供应商排行
}

/**
 * 筛选条件接口
 */
export interface SettlementFilter {
  settlementNumber?: string
  supplierName?: string
  status?: SettlementStatus | 'all'
  paymentMethod?: PaymentMethod | 'all'
  dateRange?: [string, string]  // 开始日期, 结束日期
  minAmount?: number
  maxAmount?: number
}

/**
 * 分页参数接口
 */
export interface PaginationParams {
  page: number
  pageSize: number
  total: number
}

/**
 * 结算列表响应接口
 */
export interface SettlementListResponse {
  items: PurchaseSettlement[]
  pagination: PaginationParams
  statistics?: {
    totalAmount: number
    pendingCount: number
    overdueCount: number
  }
}

/**
 * 创建结算单参数
 */
export interface CreateSettlementParams {
  supplierId: string
  orderIds: string[]                    // 关联订单ID列表
  settlementAmount: number
  currency: string
  dueDate: string
  paymentMethod?: PaymentMethod
  notes?: string
}

/**
 * 付款申请参数
 */
export interface PaymentApplicationParams {
  settlementId: string
  amount: number
  paymentMethod: PaymentMethod
  paymentDate: string
  bankAccount?: string
  referenceNumber?: string
  voucherFile?: File
  notes?: string
}