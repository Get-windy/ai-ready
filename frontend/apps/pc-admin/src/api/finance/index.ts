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
 * 应付账款API
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
 * 财务对账API
 */
export const reconciliationApi = {
  /**
   * 银行对账
   */
  bankReconciliation(params: any): Promise<ApiResponse<any>> {
    return request.post('/erp/finance/reconciliation/bank', params)
  },

  /**
   * 客户对账
   */
  customerReconciliation(params: any): Promise<ApiResponse<any>> {
    return request.post('/erp/finance/reconciliation/customer', params)
  },

  /**
   * 供应商对账
   */
  supplierReconciliation(params: any): Promise<ApiResponse<any>> {
    return request.post('/erp/finance/reconciliation/supplier', params)
  }
}

/**
 * 财务报表API
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

export default {
  accountsReceivableApi,
  accountsPayableApi,
  reconciliationApi,
  financialReportsApi
}