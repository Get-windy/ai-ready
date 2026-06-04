/**
 * 预算管理 API 模块
 */
import request, { type ApiResponse } from '@/utils/request'

// ── 通用分页类型 ──────────────────────────────────────────
export interface PageQuery {
  pageNum?: number
  pageSize?: number
  keyword?: string
  fiscalYear?: number
  departmentId?: string
  status?: string
  [key: string]: any
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
}

// ── 预算模板 ──────────────────────────────────────────────
export interface BudgetTemplate {
  id: number
  templateCode: string
  templateName: string
  fiscalYear: number
  totalAmount: number
  status: string
  description: string
  createdBy: string
  createdAt: string
  updatedBy: string
  updatedAt: string
  items: BudgetTemplateItem[]
}

export interface BudgetTemplateItem {
  id: number
  templateId: number
  subjectCode: string
  subjectName: string
  budgetAmount: number
  sortOrder: number
}

export const budgetTemplateApi = {
  create(data: BudgetTemplate): Promise<ApiResponse<BudgetTemplate>> {
    return request.post('/erp/budget/template', data)
  },
  update(id: number, data: BudgetTemplate): Promise<ApiResponse<BudgetTemplate>> {
    return request.put(`/erp/budget/template/${id}`, data)
  },
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/budget/template/${id}`)
  },
  getById(id: number): Promise<ApiResponse<BudgetTemplate>> {
    return request.get(`/erp/budget/template/${id}`)
  },
  page(params: PageQuery): Promise<ApiResponse<PageResult<BudgetTemplate>>> {
    return request.get('/erp/budget/template/page', params)
  },
  publish(id: number): Promise<ApiResponse<BudgetTemplate>> {
    return request.post(`/erp/budget/template/${id}/publish`)
  },
  listByYear(fiscalYear: number): Promise<ApiResponse<BudgetTemplate[]>> {
    return request.get('/erp/budget/template/list-by-year', { fiscalYear })
  },
}

// ── 年度预算 ──────────────────────────────────────────────
export interface AnnualBudget {
  id: number
  budgetNo: string
  templateId: number
  templateName: string
  fiscalYear: number
  departmentId: string
  departmentName: string
  totalAmount: number
  status: string
  totalApprovedAmount: number
  totalUsedAmount: number
  totalRemainingAmount: number
  executionRate: number
  description: string
  remark: string
  createdBy: string
  createdAt: string
  updatedBy: string
  updatedAt: string
  items: BudgetItem[]
}

export interface BudgetItem {
  id: number
  budgetId: number
  subjectCode: string
  subjectName: string
  budgetAmount: number
  usedAmount: number
  remainingAmount: number
  frozenAmount: number
  executionRate: number
  sortOrder: number
}

export const annualBudgetApi = {
  create(data: AnnualBudget): Promise<ApiResponse<AnnualBudget>> {
    return request.post('/erp/budget/annual', data)
  },
  update(id: number, data: AnnualBudget): Promise<ApiResponse<AnnualBudget>> {
    return request.put(`/erp/budget/annual/${id}`, data)
  },
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/budget/annual/${id}`)
  },
  getById(id: number): Promise<ApiResponse<AnnualBudget>> {
    return request.get(`/erp/budget/annual/${id}`)
  },
  page(params: PageQuery): Promise<ApiResponse<PageResult<AnnualBudget>>> {
    return request.get('/erp/budget/annual/page', params)
  },
  submit(id: number): Promise<ApiResponse<AnnualBudget>> {
    return request.post(`/erp/budget/annual/${id}/submit`)
  },
  approve(id: number): Promise<ApiResponse<AnnualBudget>> {
    return request.post(`/erp/budget/annual/${id}/approve`)
  },
  reject(id: number): Promise<ApiResponse<AnnualBudget>> {
    return request.post(`/erp/budget/annual/${id}/reject`)
  },
  close(id: number): Promise<ApiResponse<AnnualBudget>> {
    return request.post(`/erp/budget/annual/${id}/close`)
  },
}

// ── 预算科目 ──────────────────────────────────────────────
export const budgetItemApi = {
  listByBudget(budgetId: number): Promise<ApiResponse<BudgetItem[]>> {
    return request.get(`/erp/budget/item/list-by-budget/${budgetId}`)
  },
  update(id: number, data: BudgetItem): Promise<ApiResponse<BudgetItem>> {
    return request.put(`/erp/budget/item/${id}`, data)
  },
  getById(id: number): Promise<ApiResponse<BudgetItem>> {
    return request.get(`/erp/budget/item/${id}`)
  },
}

// ── 预算调整 ──────────────────────────────────────────────
export interface BudgetAdjustment {
  id: number
  adjustmentNo: string
  budgetId: number
  adjustmentType: string
  amount: number
  sourceSubjectId: number
  sourceSubjectName: string
  targetSubjectId: number
  targetSubjectName: string
  reason: string
  status: string
  applicantId: string
  applicantName: string
  approverId: string
  approverName: string
  approvalComment: string
  applyDate: string
  approvalDate: string
  createdBy: string
  createdAt: string
  updatedBy: string
  updatedAt: string
}

export const budgetAdjustmentApi = {
  create(data: BudgetAdjustment): Promise<ApiResponse<BudgetAdjustment>> {
    return request.post('/erp/budget/adjustment', data)
  },
  update(id: number, data: BudgetAdjustment): Promise<ApiResponse<BudgetAdjustment>> {
    return request.put(`/erp/budget/adjustment/${id}`, data)
  },
  getById(id: number): Promise<ApiResponse<BudgetAdjustment>> {
    return request.get(`/erp/budget/adjustment/${id}`)
  },
  page(params: PageQuery): Promise<ApiResponse<PageResult<BudgetAdjustment>>> {
    return request.get('/erp/budget/adjustment/page', params)
  },
  submit(id: number): Promise<ApiResponse<BudgetAdjustment>> {
    return request.post(`/erp/budget/adjustment/${id}/submit`)
  },
  approve(id: number, comment?: string): Promise<ApiResponse<BudgetAdjustment>> {
    return request.post(`/erp/budget/adjustment/${id}/approve`, null, { params: { comment } })
  },
  reject(id: number, comment: string): Promise<ApiResponse<BudgetAdjustment>> {
    return request.post(`/erp/budget/adjustment/${id}/reject`, null, { params: { comment } })
  },
}

// ── 预算报表 ──────────────────────────────────────────────
export interface BudgetStatistics {
  totalBudgetAmount: number
  totalUsedAmount: number
  totalRemainingAmount: number
  totalFrozenAmount: number
  executionRate: number
  totalBudgetCount: number
  executingCount: number
  closedCount: number
  draftCount: number
  increaseAmount: number
  decreaseAmount: number
}

export const budgetReportApi = {
  executionSummary(fiscalYear?: number): Promise<ApiResponse<BudgetStatistics>> {
    return request.get('/erp/budget/report/execution-summary', { fiscalYear })
  },
  departmentSummary(fiscalYear?: number): Promise<ApiResponse<any[]>> {
    return request.get('/erp/budget/report/department-summary', { fiscalYear })
  },
  subjectSummary(fiscalYear?: number, budgetId?: number): Promise<ApiResponse<any[]>> {
    return request.get('/erp/budget/report/subject-summary', { fiscalYear, budgetId })
  },
  varianceAnalysis(fiscalYear?: number): Promise<ApiResponse<any[]>> {
    return request.get('/erp/budget/report/variance-analysis', { fiscalYear })
  },
  trend(fiscalYear?: number): Promise<ApiResponse<any[]>> {
    return request.get('/erp/budget/report/trend', { fiscalYear })
  },
}
