import request, { type ApiResponse, type PageResponse } from '@/utils/request'

/**
 * 应收账款
 */
export interface AccountsReceivable {
  id: number
  customerName: string
  orderNo: string
  amount: number
  paidAmount: number
  unpaidAmount: number
  status: number
  dueDate: string
  remark: string
}

/**
 * 应付账款
 */
export interface AccountsPayable {
  id: number
  supplierName: string
  orderNo: string
  amount: number
  paidAmount: number
  unpaidAmount: number
  status: number
  dueDate: string
  remark: string
}

/**
 * 收款记录
 */
export interface PaymentRecord {
  id: number
  customerName: string
  orderNo: string
  amount: number
  paymentMethod: string
  paymentDate: string
  operator: string
  remark: string
}

/**
 * 付款记录
 */
export interface PaymentRecordPayable {
  id: number
  supplierName: string
  orderNo: string
  amount: number
  paymentMethod: string
  paymentDate: string
  operator: string
  remark: string
}

/**
 * 查询参数
 */
export interface FinanceQuery {
  tenantId?: number
  customerName?: string
  supplierName?: string
  orderNo?: string
  status?: number
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

/**
 * 应收账款API
 * @deprecated 路径不匹配后端, 请使用 receivableApi
 */
export const accountsReceivableApi = {
  /**
   * 分页查询应收账款
   */
  getPage(params: FinanceQuery): Promise<ApiResponse<PageResponse<AccountsReceivable>>> {
    return request.get('/erp/finance/accounts-receivable/page', params)
  },

  /**
   * 创建应收账款
   */
  create(data: Partial<AccountsReceivable>): Promise<ApiResponse<boolean>> {
    return request.post('/erp/finance/accounts-receivable', data)
  },

  /**
   * 更新应收账款
   */
  update(id: number, data: Partial<AccountsReceivable>): Promise<ApiResponse<boolean>> {
    return request.put(`/erp/finance/accounts-receivable/${id}`, data)
  },

  /**
   * 删除应收账款
   */
  delete(id: number): Promise<ApiResponse<boolean>> {
    return request.delete(`/erp/finance/accounts-receivable/${id}`)
  },

  /**
   * 收款
   */
  payment(id: number, amount: number, method: string): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/finance/accounts-receivable/${id}/payment`, null, { params: { amount, method } })
  }
}

/**
 * 应收账款（旧版，与receivable页面兼容，使用/erp/finance/receivable/接口）
 */
export const receivableApi = {
  getPage: (params: any) => request.get('/erp/finance/receivable/list', params),
  getById: (id: number) => request.get(`/erp/finance/receivable/${id}`),
  getAging: () => request.get('/erp/finance/receivable/aging'),
  create: (data: any) => request.post('/erp/finance/receivable', data),
  writeOff: (id: number, amount: number) => request.put(`/erp/finance/receivable/${id}/write-off`, { amount }),
  markBadDebt: (id: number) => request.put(`/erp/finance/receivable/${id}/bad-debt`)
}

/**
 * 应付账款API
 * @deprecated 路径不匹配后端, 请使用 payableApi
 */
export const accountsPayableApi = {
  /**
   * 分页查询应付账款
   */
  getPage(params: FinanceQuery): Promise<ApiResponse<PageResponse<AccountsPayable>>> {
    return request.get('/erp/finance/accounts-payable/page', params)
  },

  /**
   * 创建应付账款
   */
  create(data: Partial<AccountsPayable>): Promise<ApiResponse<boolean>> {
    return request.post('/erp/finance/accounts-payable', data)
  },

  /**
   * 更新应付账款
   */
  update(id: number, data: Partial<AccountsPayable>): Promise<ApiResponse<boolean>> {
    return request.put(`/erp/finance/accounts-payable/${id}`, data)
  },

  /**
   * 删除应付账款
   */
  delete(id: number): Promise<ApiResponse<boolean>> {
    return request.delete(`/erp/finance/accounts-payable/${id}`)
  },

  /**
   * 付款
   */
  payment(id: number, amount: number, method: string): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/finance/accounts-payable/${id}/payment`, null, { params: { amount, method } })
  },

  /**
   * 付款审批
   */
  approve(id: number, approved: boolean, comment?: string): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/finance/accounts-payable/${id}/approve`, null, { params: { approved, comment } })
  }
}

/**
 * 应付账款（旧版，与payable页面兼容，使用/erp/finance/payable/接口）
 */
export const payableApi = {
  getPage: (params: any) => request.get('/erp/finance/payable/list', params),
  getById: (id: number) => request.get(`/erp/finance/payable/${id}`),
  getAging: () => request.get('/erp/finance/payable/aging'),
  create: (data: any) => request.post('/erp/finance/payable', data),
  writeOff: (id: number, amount: number) => request.put(`/erp/finance/payable/${id}/write-off`, { amount })
}

/**
 * 会计科目API
 */
export const accountSubjectApi = {
  getTree: (params?: any) => request.get('/erp/finance/subject/tree', params),
  getList: (params?: any) => request.get('/erp/finance/subject/list', params),
  getById: (id: number) => request.get(`/erp/finance/subject/${id}`),
  getByType: (type: number) => request.get(`/erp/finance/subject/type/${type}`),
  create: (data: any) => request.post('/erp/finance/subject', data),
  update: (id: number, data: any) => request.put(`/erp/finance/subject/${id}`, data),
  delete: (id: number) => request.delete(`/erp/finance/subject/${id}`),
  toggleEnabled: (id: number, enabled?: boolean) => request.put(`/erp/finance/subject/${id}/enable`, null, { params: { enabled } })
}

/**
 * 凭证API
 */
export const voucherApi = {
  getPage: (params: any) => request.get('/erp/finance/voucher/list', params),
  getById: (id: number) => request.get(`/erp/finance/voucher/${id}`),
  getByVoucherNo: (no: string) => request.get(`/erp/finance/voucher/no/${no}`),
  create: (data: any) => request.post('/erp/finance/voucher', data),
  audit: (id: number) => request.put(`/erp/finance/voucher/${id}/audit`),
  post: (id: number) => request.put(`/erp/finance/voucher/${id}/post`),
  reverse: (id: number, reason: string) => request.post(`/erp/finance/voucher/${id}/reverse`, { reason })
}

/**
 * 财务报表API v2
 */
export const reportApi = {
  getTrialBalance: (params: any) => request.get('/erp/finance/report/v2/trial-balance', params),
  getBalanceSheet: (params: any) => request.get('/erp/finance/report/v2/balance-sheet', params),
  getIncomeStatement: (params: any) => request.get('/erp/finance/report/v2/income-statement', params),
  getDashboard: () => request.get('/erp/finance/report/v2/dashboard')
}

/**
 * 财务对账API
 * 后端控制器: core-api @RequestMapping("/api/finance/reconciliation")
 */
export const reconciliationApi = {
  /**
   * 获取对账统计数据
   */
  getStats(): Promise<ApiResponse<any>> {
    return request.get('/finance/reconciliation/stats')
  },

  /**
   * 创建对账记录
   */
  create(params: any): Promise<ApiResponse<number>> {
    return request.post('/finance/reconciliation/create', params)
  },

  /**
   * 更新对账记录
   */
  update(params: any): Promise<ApiResponse<void>> {
    return request.put('/finance/reconciliation/update', params)
  },

  /**
   * 根据ID获取对账记录详情
   */
  getById(id: number): Promise<ApiResponse<any>> {
    return request.get(`/finance/reconciliation/${id}`)
  },

  /**
   * 删除对账记录
   */
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/finance/reconciliation/${id}`)
  },

  /**
   * 分页查询对账记录
   */
  page(params: any): Promise<ApiResponse<any>> {
    return request.post('/finance/reconciliation/list', params)
  },

  /**
   * 执行对账
   */
  reconcile(id: number): Promise<ApiResponse<void>> {
    return request.post(`/finance/reconciliation/reconcile/${id}`)
  },

  /**
   * 处理差异
   */
  handleDifference(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.post(`/finance/reconciliation/handle-difference/${id}`, null, { params: { differenceReason: reason } })
  },

  /**
   * 批量删除对账记录
   */
  deleteBatch(ids: number[]): Promise<ApiResponse<void>> {
    return request.delete('/finance/reconciliation/batch', { data: ids })
  },

  /**
   * 导出对账记录列表
   */
  exportList(params: any): Promise<ApiResponse<any[]>> {
    return request.get('/finance/reconciliation/export', { params })
  },

  // ── 以下为旧版API，兼容现有组件 ──
  /** @deprecated 使用 create() 或 page() */
  bankReconciliation(params: any): Promise<ApiResponse<any>> {
    return request.post('/finance/reconciliation/create', params)
  },
  /** @deprecated 使用 page() */
  customerReconciliation(params: any): Promise<ApiResponse<any>> {
    return request.post('/finance/reconciliation/list', params)
  },
  /** @deprecated 使用 page() */
  supplierReconciliation(params: any): Promise<ApiResponse<any>> {
    return request.post('/finance/reconciliation/list', params)
  }
}

/**
 * 财务报表API (v1)
 * @deprecated 路径不匹配后端, 请使用 reportApi v2
 */
export const financialReportsApi = {
  /**
   * 资产负债表
   */
  getBalanceSheet(month: string): Promise<ApiResponse<any>> {
    return request.get('/erp/finance/reports/balance-sheet', { month })
  },

  /**
   * 利润表
   */
  getProfitStatement(month: string): Promise<ApiResponse<any>> {
    return request.get('/erp/finance/reports/profit-statement', { month })
  },

  /**
   * 现金流量表
   */
  getCashFlowStatement(month: string): Promise<ApiResponse<any>> {
    return request.get('/erp/finance/reports/cash-flow', { month })
  }
}

/**
 * 预收款
 */
export interface PreReceipt {
  id: number
  preReceiptNo: string
  sourceType: string
  sourceId: number
  sourceNo: string
  customerId: number
  customerName: string
  amount: number
  usedAmount: number
  remainingAmount: number
  depositType: number
  depositFlag: number
  receiptDate: string
  status: string
  paymentMethod: string
  bankAccount: string
  bankName: string
  transactionNo: string
  remark: string
  createTime: string
}

/**
 * 预付款
 */
export interface PrePayment {
  id: number
  prePaymentNo: string
  sourceType: string
  sourceId: number
  sourceNo: string
  supplierId: number
  supplierName: string
  amount: number
  usedAmount: number
  remainingAmount: number
  depositType: number
  depositFlag: number
  paymentDate: string
  status: string
  paymentMethod: string
  bankAccount: string
  bankName: string
  transactionNo: string
  remark: string
  createTime: string
}

/**
 * 往来对冲
 */
export interface Offset {
  id: number
  offsetNo: string
  partyType: string
  partyId: number
  partyName: string
  receivableAmount: number
  payableAmount: number
  offsetAmount: number
  balanceAmount: number
  offsetDate: string
  status: string
  remark: string
  createTime: string
}

/**
 * 对冲明细
 */
export interface OffsetItem {
  id: number
  offsetId: number
  lineNo: number
  direction: string
  refType: string
  refId: number
  refNo: string
  amount: number
}

/**
 * 资金流水
 */
export interface CapitalFlow {
  id: number
  flowNo: string
  flowType: string
  direction: string
  refId: number
  refNo: string
  refType: string
  amount: number
  balance: number
  partyType: string
  partyId: number
  partyName: string
  businessType: string
  occurDate: string
  paymentMethod: string
  bankAccount: string
  bankName: string
  transactionNo: string
  remark: string
  createTime: string
}

/**
 * 预收款API
 */
export const preReceiptApi = {
  getPage(params: any): Promise<ApiResponse<PageResponse<PreReceipt>>> {
    return request.get('/erp/pre-receipt/page', { params })
  },
  getById(id: number): Promise<ApiResponse<PreReceipt>> {
    return request.get(`/erp/pre-receipt/${id}`)
  },
  create(data: any): Promise<ApiResponse<PreReceipt>> {
    return request.post('/erp/pre-receipt', data)
  },
  offsetToReceipt(id: number, receiptId: number, amount: number): Promise<ApiResponse<PreReceipt>> {
    return request.post(`/erp/pre-receipt/${id}/offset-to-receipt`, { receiptId, amount })
  },
  forfeit(id: number, reason: string): Promise<ApiResponse<PreReceipt>> {
    return request.post(`/erp/pre-receipt/${id}/forfeit`, { reason })
  },
  refund(id: number, reason: string): Promise<ApiResponse<PreReceipt>> {
    return request.post(`/erp/pre-receipt/${id}/refund`, { reason })
  },
  getStats(): Promise<ApiResponse<any>> {
    return request.get('/erp/pre-receipt/statistics')
  }
}

/**
 * 预付款API
 */
export const prePaymentApi = {
  getPage(params: any): Promise<ApiResponse<PageResponse<PrePayment>>> {
    return request.get('/erp/pre-payment/page', { params })
  },
  getById(id: number): Promise<ApiResponse<PrePayment>> {
    return request.get(`/erp/pre-payment/${id}`)
  },
  create(data: any): Promise<ApiResponse<PrePayment>> {
    return request.post('/erp/pre-payment', data)
  },
  offsetToPayment(id: number, paymentId: number, amount: number): Promise<ApiResponse<PrePayment>> {
    return request.post(`/erp/pre-payment/${id}/offset-to-payment`, { paymentId, amount })
  },
  recover(id: number, reason: string): Promise<ApiResponse<PrePayment>> {
    return request.post(`/erp/pre-payment/${id}/recover`, { reason })
  },
  refund(id: number, reason: string): Promise<ApiResponse<PrePayment>> {
    return request.post(`/erp/pre-payment/${id}/refund`, { reason })
  },
  getStats(): Promise<ApiResponse<any>> {
    return request.get('/erp/pre-payment/statistics')
  }
}

/**
 * 往来对冲API
 */
export const offsetApi = {
  getPage(params: any): Promise<ApiResponse<PageResponse<Offset>>> {
    return request.get('/erp/offset/page', { params })
  },
  getById(id: number): Promise<ApiResponse<Offset>> {
    return request.get(`/erp/offset/${id}`)
  },
  create(data: any): Promise<ApiResponse<Offset>> {
    return request.post('/erp/offset', data)
  },
  complete(id: number): Promise<ApiResponse<Offset>> {
    return request.post(`/erp/offset/${id}/complete`)
  },
  cancel(id: number, reason: string): Promise<ApiResponse<Offset>> {
    return request.post(`/erp/offset/${id}/cancel`, { reason })
  },
  getItems(offsetId: number): Promise<ApiResponse<OffsetItem[]>> {
    return request.get(`/erp/offset/${offsetId}/items`)
  }
}

/**
 * 资金流水API
 */
export const capitalFlowApi = {
  getPage(params: any): Promise<ApiResponse<PageResponse<CapitalFlow>>> {
    return request.get('/erp/capital-flow/page', { params })
  },
  getStats(): Promise<ApiResponse<any>> {
    return request.get('/erp/capital-flow/statistics')
  },
  exportList(params: any): Promise<Blob> {
    return request.get('/erp/capital-flow/export', { params, responseType: 'blob' })
  }
}

/**
 * 收款单API
 */
export const receiptApi = {
  getPage(params: any): Promise<ApiResponse<PageResponse<any>>> {
    return request.get('/erp/receipt/page', { params })
  },
  getById(id: number): Promise<ApiResponse<any>> {
    return request.get(`/erp/receipt/${id}`)
  },
  create(data: any): Promise<ApiResponse<any>> {
    return request.post('/erp/receipt', data)
  },
  update(id: number, data: any): Promise<ApiResponse<any>> {
    return request.put(`/erp/receipt/${id}`, data)
  },
  submit(id: number): Promise<ApiResponse<any>> {
    return request.post(`/erp/receipt/${id}/submit`)
  },
  approve(id: number, note?: string): Promise<ApiResponse<any>> {
    return request.post(`/erp/receipt/${id}/approve`, null, { params: { note } })
  },
  reject(id: number, reason: string): Promise<ApiResponse<any>> {
    return request.post(`/erp/receipt/${id}/reject`, null, { params: { reason } })
  },
  complete(id: number): Promise<ApiResponse<any>> {
    return request.post(`/erp/receipt/${id}/complete`)
  },
  cancel(id: number, reason: string): Promise<ApiResponse<any>> {
    return request.post(`/erp/receipt/${id}/cancel`, null, { params: { reason } })
  },
  /**
   * 核销
   */
  writeOff(id: number, amount: number): Promise<ApiResponse<any>> {
    return request.post(`/erp/receipt/${id}/write-off`, { amount })
  },
  getStatistics(): Promise<ApiResponse<any>> {
    return request.get('/erp/receipt/statistics')
  }
}

/**
 * 付款单API
 */
export const paymentApi = {
  getPage(params: any): Promise<ApiResponse<PageResponse<any>>> {
    return request.get('/erp/payment/page', { params })
  },
  getById(id: number): Promise<ApiResponse<any>> {
    return request.get(`/erp/payment/${id}`)
  },
  create(data: any): Promise<ApiResponse<any>> {
    return request.post('/erp/payment', data)
  },
  update(id: number, data: any): Promise<ApiResponse<any>> {
    return request.put(`/erp/payment/${id}`, data)
  },
  submit(id: number): Promise<ApiResponse<any>> {
    return request.post(`/erp/payment/${id}/submit`)
  },
  approve(id: number, note?: string): Promise<ApiResponse<any>> {
    return request.post(`/erp/payment/${id}/approve`, null, { params: { note } })
  },
  reject(id: number, reason: string): Promise<ApiResponse<any>> {
    return request.post(`/erp/payment/${id}/reject`, null, { params: { reason } })
  },
  complete(id: number): Promise<ApiResponse<any>> {
    return request.post(`/erp/payment/${id}/complete`)
  },
  cancel(id: number, reason: string): Promise<ApiResponse<any>> {
    return request.post(`/erp/payment/${id}/cancel`, null, { params: { reason } })
  },
  /**
   * 核销
   */
  writeOff(id: number, amount: number): Promise<ApiResponse<any>> {
    return request.post(`/erp/payment/${id}/write-off`, { amount })
  },
  getStatistics(): Promise<ApiResponse<any>> {
    return request.get('/erp/payment/statistics')
  }
}

/**
 * 核销记录
 */
export interface WriteOff {
  id: number
  tenantId?: number
  writeOffNo: string
  writeOffType: string
  receiptId?: number
  receiptNo?: string
  paymentId?: number
  paymentNo?: string
  receivableId?: number
  payableId?: number
  customerId?: number
  customerName?: string
  supplierId?: number
  supplierName?: string
  totalAmount: number
  writeOffAmount: number
  remainingAmount: number
  writeOffDate: string
  status: string
  remark?: string
  createTime: string
}

/**
 * 核销记录API
 */
export const writeOffApi = {
  getPage(params: any): Promise<ApiResponse<PageResponse<WriteOff>>> {
    return request.get('/erp/write-off/page', { params })
  },
  getById(id: number): Promise<ApiResponse<WriteOff>> {
    return request.get(`/erp/write-off/${id}`)
  },
  getByReceiptId(receiptId: number): Promise<ApiResponse<WriteOff[]>> {
    return request.get(`/erp/write-off/receipt/${receiptId}`)
  },
  getByPaymentId(paymentId: number): Promise<ApiResponse<WriteOff[]>> {
    return request.get(`/erp/write-off/payment/${paymentId}`)
  }
}

/**
 * 定金条件API
 */
export interface DepositCondition {
  id: number
  preReceiptId?: number
  prePaymentId?: number
  depositType: string
  direction: string
  sourceType: string
  sourceId: number
  sourceNo: string
  conditionDesc: string
  breachClause: string
  agreedDate: string
  expiredDate: string
  amount: number
  forfeitAmount: number
  contactPerson: string
  contactPhone: string
  status: string
  remark: string
}

export const depositConditionApi = {
  getByPreReceiptId(preReceiptId: number): Promise<ApiResponse<DepositCondition>> {
    return request.get(`/erp/deposit-condition/pre-receipt/${preReceiptId}`)
  },
  getByPrePaymentId(prePaymentId: number): Promise<ApiResponse<DepositCondition>> {
    return request.get(`/erp/deposit-condition/pre-payment/${prePaymentId}`)
  },
  getBySource(sourceType: string, sourceId: number): Promise<ApiResponse<DepositCondition[]>> {
    return request.get('/erp/deposit-condition/source', { params: { sourceType, sourceId } })
  },
  create(data: any): Promise<ApiResponse<DepositCondition>> {
    return request.post('/erp/deposit-condition', data)
  },
  convert(id: number): Promise<ApiResponse<DepositCondition>> {
    return request.post(`/erp/deposit-condition/${id}/convert`)
  },
  refund(id: number, reason: string): Promise<ApiResponse<DepositCondition>> {
    return request.post(`/erp/deposit-condition/${id}/refund`, null, { params: { reason } })
  },
  forfeit(id: number, forfeitAmount: number, reason: string): Promise<ApiResponse<DepositCondition>> {
    return request.post(`/erp/deposit-condition/${id}/forfeit`, { forfeitAmount, reason })
  },
  deduct(id: number, deductAmount: number, reason: string): Promise<ApiResponse<DepositCondition>> {
    return request.post(`/erp/deposit-condition/${id}/deduct`, { deductAmount, reason })
  }
}

export default {
  accountsReceivableApi,
  accountsPayableApi,
  reconciliationApi,
  financialReportsApi,
  preReceiptApi,
  prePaymentApi,
  receiptApi,
  paymentApi,
  offsetApi,
  capitalFlowApi,
  writeOffApi,
  depositConditionApi,
  accountSubjectApi,
  voucherApi,
  reportApi,
  receivableApi,
  payableApi
}