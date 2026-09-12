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
  totalFrozenAmount: number
  executionRate: number
  description: string
  remark: string
  createdBy: string
  createdAt: string
  updatedBy: string
  updatedAt: string
  items: BudgetItem[]
  // ── 金标准编制/审批字段 ──
  budgetDate: string
  handlerId: number | string
  handlerName: string
  creatorName: string
  auditorId: number | string
  auditorName: string
  auditTime: string
  auditRemark: string
  printCount: number
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
  lineNo: number
  subjectId: number | string
  subjectType: string
  remark: string
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
  /** 金标准：多条件分页（并把 records/total 提到顶层，兼容旧调用方） */
  async getPage(params: PageQuery): Promise<any> {
    const res: any = await request.get('/erp/budget/annual/page', params)
    const body = res?.data ?? res
    return {
      ...res,
      records: body?.records ?? [],
      total: Number(body?.total) || 0,
      current: body?.current,
      size: body?.size,
    }
  },
  /** 金标准：生成下一预算编制单号 */
  nextNo(): Promise<ApiResponse<string>> {
    return request.get('/erp/budget/annual/next-no')
  },
  /** 金标准：保存编制单（含预算科目明细） */
  save(data: AnnualBudget): Promise<ApiResponse<AnnualBudget>> {
    return request.post('/erp/budget/annual/save', data)
  },
  /** 金标准：批量提交审批 */
  batchSubmit(ids: number[]): Promise<ApiResponse<number>> {
    return request.post('/erp/budget/annual/batch-submit', { ids })
  },
  /** 金标准：批量审批通过 */
  batchApprove(ids: number[], auditor?: { auditorId?: number | string; auditorName?: string; auditRemark?: string }): Promise<ApiResponse<number>> {
    return request.post('/erp/budget/annual/batch-approve', { ids, ...(auditor || {}) })
  },
  /** 金标准：批量驳回 */
  batchReject(ids: number[], auditor?: { auditorId?: number | string; auditorName?: string; auditRemark?: string }): Promise<ApiResponse<number>> {
    return request.post('/erp/budget/annual/batch-reject', { ids, ...(auditor || {}) })
  },
  /** 金标准：批量删除 */
  batchDelete(ids: number[]): Promise<ApiResponse<number>> {
    return request.post('/erp/budget/annual/batch-delete', { ids })
  },
  /** 金标准：记录打印次数 */
  printDoc(id: number): Promise<ApiResponse<void>> {
    return request.post(`/erp/budget/annual/${id}/print`)
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
  startExec(id: number): Promise<ApiResponse<AnnualBudget>> {
    return request.post(`/erp/budget/annual/${id}/start-exec`)
  },
  close(id: number): Promise<ApiResponse<AnnualBudget>> {
    return request.post(`/erp/budget/annual/${id}/close`)
  },
  export(params: PageQuery): Promise<ApiResponse<AnnualBudget[]>> {
    return request.get('/erp/budget/annual/export', params)
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
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/erp/budget/adjustment/${id}`)
  },
  export(params: PageQuery): Promise<ApiResponse<BudgetAdjustment[]>> {
    return request.get('/erp/budget/adjustment/export', params)
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
  /** 已转入执行的预算单数（已审批/执行中/已关闭） */
  approvedCount?: number
  /** 预算科目数 */
  totalItemCount?: number
  /** 超支科目数（执行进度 > 100%） */
  overBudgetCount?: number
  /** 预警科目数（执行进度 ≥ 90%） */
  warningCount?: number
  /** 超支金额合计 */
  overBudgetAmount?: number
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

// ── 预算执行（只读跟踪 + 超支预警） ─────────────────────────
export interface BudgetExecutionQuery {
  /** 财政年度 */
  fiscalYear?: number
  departmentId?: string
  subjectCode?: string
  /** 预算编号 / 部门 / 预算科目名称 */
  keyword?: string
  /** draft/submitted/approved/executing/rejected/closed，空=已转入执行 */
  status?: string
  /** 仅看超支（执行进度 > 100%） */
  overBudgetOnly?: boolean
  /** 仅看预警（执行进度 ≥ 90%） */
  warningOnly?: boolean
  /** 页码，从 0 开始（后端口径） */
  page?: number
  size?: number
}

export interface BudgetExecutionLog {
  id: number
  budgetId: number
  budgetItemId: number
  executionType: string
  executionTypeName: string
  sourceType: string
  sourceTypeName: string
  sourceNo: string
  amount: number
  executionDate: string
  description: string
}

export const budgetExecutionApi = {
  /** 按预算单维度分页 */
  rows(params: BudgetExecutionQuery): Promise<ApiResponse<PageResult<any>>> {
    return request.get('/erp/budget/execution/rows', params)
  },
  /** 按预算科目明细维度分页 */
  items(params: BudgetExecutionQuery): Promise<ApiResponse<PageResult<any>>> {
    return request.get('/erp/budget/execution/items', params)
  },
  /** 预算执行流水（冻结/释放/消耗） */
  logs(params: { budgetId?: number; budgetItemId?: number }): Promise<ApiResponse<BudgetExecutionLog[]>> {
    return request.get('/erp/budget/execution/logs', params)
  },
  /** 超支预警清单 */
  warnings(fiscalYear?: number): Promise<ApiResponse<any>> {
    return request.get('/erp/budget/execution/warnings', { fiscalYear })
  },
}
