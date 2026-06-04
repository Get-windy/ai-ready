/**
 * 财务管理 - 会计科目/总账/试算平衡 API
 */
import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// ========== 会计科目 ==========

export interface AccountSubject {
  id: number
  subjectCode: string
  subjectName: string
  subjectType: number // 1:资产 2:负债 3:权益 4:成本 5:损益
  subjectTypeDesc?: string
  subjectLevel: number
  parentId?: number
  balanceDirection: number // 1:借 2:贷
  balanceDirectionDesc?: string
  openingBalance: number
  currentBalance: number
  isDetail: number // 0:否 1:是
  auxiliaryTypes?: string
  status: number // 0:禁用 1:启用
  statusDesc?: string
  sortOrder: number
  remark?: string
  children?: AccountSubject[]
}

export interface AccountSubjectQuery {
  subjectCode?: string
  subjectName?: string
  subjectType?: number
  parentId?: number
  isDetail?: number
  status?: number
  current?: number
  size?: number
}

export interface AccountSubjectSave {
  subjectCode: string
  subjectName: string
  subjectType: number
  subjectLevel: number
  parentId?: number
  balanceDirection: number
  openingBalance?: number
  isDetail?: number
  auxiliaryTypes?: string
  status?: number
  sortOrder?: number
  remark?: string
}

// ========== 账簿 ==========

export interface LedgerRecord {
  id: number
  ledgerType: number // 1:明细账 2:总账
  ledgerTypeDesc?: string
  subjectId: number
  subjectCode: string
  subjectName: string
  accountingPeriod: string
  businessDate: string
  voucherId: number
  voucherNo: string
  summary: string
  debitAmount: number
  creditAmount: number
  balance: number
  balanceDirection: number
  balanceDirectionDesc?: string
}

export interface LedgerQuery {
  subjectId?: number
  subjectCode?: string
  accountingPeriod?: string
  startDate?: string
  endDate?: string
  current?: number
  size?: number
}

// ========== 试算平衡 ==========

export interface TrialBalanceItem {
  subjectCode: string
  subjectName: string
  openingDebit: number
  openingCredit: number
  periodDebit: number
  periodCredit: number
  closingDebit: number
  closingCredit: number
}

export interface TrialBalanceQuery {
  accountingPeriod: string
}

// ========== API ==========

export const accountingApi = {
  // ===== 科目管理 =====
  getSubjectTree(): Promise<ApiResponse<AccountSubject[]>> {
    return request.get('/erp/finance/subject/tree')
  },
  querySubjects(params: AccountSubjectQuery): Promise<ApiResponse<PageResponse<AccountSubject>>> {
    return request.get('/erp/finance/subject/page', params)
  },
  getSubjectById(id: number): Promise<ApiResponse<AccountSubject>> {
    return request.get(`/erp/finance/subject/${id}`)
  },
  getSubjectByCode(code: string): Promise<ApiResponse<AccountSubject>> {
    return request.get(`/erp/finance/subject/code/${code}`)
  },
  createSubject(data: AccountSubjectSave): Promise<ApiResponse<AccountSubject>> {
    return request.post('/erp/finance/subject', data)
  },
  updateSubject(id: number, data: AccountSubjectSave): Promise<ApiResponse<AccountSubject>> {
    return request.put(`/erp/finance/subject/${id}`, data)
  },
  deleteSubject(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/finance/subject/${id}`)
  },
  batchDeleteSubjects(ids: number[]): Promise<ApiResponse<void>> {
    return request.delete('/erp/finance/subject/batch', { data: ids })
  },
  getDetailSubjects(): Promise<ApiResponse<AccountSubject[]>> {
    return request.get('/erp/finance/subject/detail-list')
  },

  // ===== 总账/明细账 =====
  queryDetailLedger(params: LedgerQuery): Promise<ApiResponse<PageResponse<LedgerRecord>>> {
    return request.get('/erp/finance/ledger/detail', params)
  },
  queryGeneralLedger(params: LedgerQuery): Promise<ApiResponse<PageResponse<LedgerRecord>>> {
    return request.get('/erp/finance/ledger/general', params)
  },
  getSubjectBalance(subjectId: number, accountingPeriod?: string): Promise<ApiResponse<number>> {
    return request.get('/erp/finance/ledger/balance', { subjectId, accountingPeriod })
  },

  // ===== 试算平衡 =====
  getTrialBalance(params: TrialBalanceQuery): Promise<ApiResponse<{
    items: TrialBalanceItem[]
    totalOpeningDebit: number
    totalOpeningCredit: number
    totalPeriodDebit: number
    totalPeriodCredit: number
    totalClosingDebit: number
    totalClosingCredit: number
  }>> {
    return request.get('/erp/finance/reports/trial-balance', params)
  },

  // ===== 财务报表 =====
  getBalanceSheet(accountingPeriod: string): Promise<ApiResponse<any>> {
    return request.get('/erp/finance/reports/balance-sheet', { accountingPeriod })
  },
  getIncomeStatement(startPeriod: string, endPeriod: string): Promise<ApiResponse<any>> {
    return request.get('/erp/finance/reports/income-statement', { startPeriod, endPeriod })
  },
  getCashFlowStatement(startPeriod: string, endPeriod: string): Promise<ApiResponse<any>> {
    return request.get('/erp/finance/reports/cash-flow', { startPeriod, endPeriod })
  }
}
