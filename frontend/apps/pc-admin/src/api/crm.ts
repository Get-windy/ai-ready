/**
 * CRM 模块 API
 * 线索/商机/合同/发票
 */
import axios from 'axios'
import request, { type ApiResponse, type PageResponse } from '@/utils/request'
import { getToken } from '@/utils/tokenRefresher'

// ── 线索（对齐后端 CustomerLead 实体） ──────────────
export interface Lead {
  id: number
  leadCode?: string
  leadName?: string
  name?: string
  contactName: string
  contactPhone?: string
  phone?: string
  contactEmail?: string
  email?: string
  companyName: string
  industryType?: number
  leadSource?: number
  source?: string
  leadStatus?: number
  status?: number
  score?: number
  leadLevel?: number
  estimatedAmount?: number
  province?: string
  city?: string
  address?: string
  requirement?: string
  salesPersonId?: number
  salesPersonName?: string
  remark?: string
  createdBy?: string
  createTime: string
  updateTime?: string
}

export interface LeadQuery {
  keyword?: string
  source?: string
  leadStatus?: number
  leadLevel?: number
  salesPersonId?: number
  pageNum?: number
  pageSize?: number
}

export const leadApi = {
  page(params: LeadQuery): Promise<PageResponse<Lead>> {
    return request.get('/crm/lead/page', params)
  },
  getById(id: number): Promise<ApiResponse<Lead>> {
    return request.get(`/crm/lead/${id}`)
  },
  create(data: Partial<Lead>): Promise<ApiResponse<Lead>> {
    return request.post('/crm/lead', data)
  },
  update(id: number, data: Partial<Lead>): Promise<ApiResponse<Lead>> {
    return request.put(`/crm/lead/${id}`, data)
  },
  delete(id: number): Promise<ApiResponse<boolean>> {
    return request.delete(`/crm/lead/${id}`)
  },
  convertToCustomer(id: number): Promise<ApiResponse<any>> {
    return request.post(`/crm/lead/${id}/convert`)
  },
  batchConvert(ids: number[]): Promise<ApiResponse<any>> {
    return request.post('/crm/lead/batch-convert', ids)
  }
}

// ── 商机 ──────────────────────────────────────────
export interface Opportunity {
  id: number
  opportunityCode?: string
  name: string
  customerId?: number
  customerName: string
  stage?: number
  stageLabel?: string
  expectedAmount?: number
  actualAmount?: number
  winProbability?: number
  priority?: string
  priorityLabel?: string
  ownerId?: number
  ownerName?: string
  expectedCloseDate?: string
  status?: number
  remark?: string
  createTime: string
  updateTime?: string
}

export interface OpportunityQuery {
  keyword?: string
  customerId?: number
  opportunityStage?: number
  status?: number
  salesPersonId?: number
  pageNum?: number
  pageSize?: number
}

export const opportunityApi = {
  page(params: OpportunityQuery): Promise<PageResponse<Opportunity>> {
    return request.get('/crm/opportunity/page', params)
  },
  getById(id: number): Promise<ApiResponse<Opportunity>> {
    return request.get(`/crm/opportunity/${id}`)
  },
  create(data: Partial<Opportunity>): Promise<ApiResponse<Opportunity>> {
    return request.post('/crm/opportunity', data)
  },
  update(id: number, data: Partial<Opportunity>): Promise<ApiResponse<Opportunity>> {
    return request.put(`/crm/opportunity/${id}`, data)
  },
  delete(id: number): Promise<ApiResponse<boolean>> {
    return request.delete(`/crm/opportunity/${id}`)
  },
  updateStage(id: number, stage: number | string): Promise<ApiResponse<Opportunity>> {
    return request.put(`/crm/opportunity/${id}/stage`, null, { params: { stage } })
  },
  advanceStage(id: number): Promise<ApiResponse<Opportunity>> {
    return request.post(`/crm/opportunity/${id}/advance`)
  },
  win(id: number, actualAmount: number): Promise<ApiResponse<Opportunity>> {
    return request.post(`/crm/opportunity/${id}/win`, null, { params: { actualAmount } })
  },
  lose(id: number, loseReason: string): Promise<ApiResponse<Opportunity>> {
    return request.post(`/crm/opportunity/${id}/lose`, null, { params: { loseReason } })
  },
  getStatistics(salesPersonId?: number): Promise<ApiResponse<any>> {
    return request.get('/crm/opportunity/statistics', { salesPersonId })
  },
  listByCustomer(customerId: number): Promise<ApiResponse<Opportunity[]>> {
    return request.get(`/crm/opportunity/customer/${customerId}`)
  }
}

// ── 合同 ──────────────────────────────────────────
export interface ContractItem {
  id: number
  contractNo: string
  contractName: string
  customerId?: number
  customerName: string
  contractType?: number
  contractTypeLabel?: string
  contractAmount: number
  startDate: string
  endDate: string
  status: number
  statusDesc?: string
  salesPersonId?: number
  salesPersonName?: string
  remark?: string
  createTime: string
  updateTime?: string
}

export interface ContractQuery {
  keyword?: string
  customerId?: number
  status?: number
  contractType?: number
  salesPersonId?: number
  pageNum?: number
  pageSize?: number
}

export const contractApi = {
  page(params: ContractQuery): Promise<PageResponse<ContractItem>> {
    return request.get('/crm/contract/page', params)
  },
  getById(id: number): Promise<ApiResponse<ContractItem>> {
    return request.get(`/crm/contract/${id}`)
  },
  create(data: Partial<ContractItem>): Promise<ApiResponse<ContractItem>> {
    return request.post('/crm/contract', data)
  },
  update(id: number, data: Partial<ContractItem>): Promise<ApiResponse<ContractItem>> {
    return request.put(`/crm/contract/${id}`, data)
  },
  submitForApproval(id: number): Promise<ApiResponse<ContractItem>> {
    return request.post(`/crm/contract/${id}/submit`)
  },
  approve(id: number, note?: string): Promise<ApiResponse<ContractItem>> {
    return request.post(`/crm/contract/${id}/approve`, null, { params: { note } })
  },
  reject(id: number, reason: string): Promise<ApiResponse<ContractItem>> {
    return request.post(`/crm/contract/${id}/reject`, null, { params: { reason } })
  },
  sign(id: number, signMethod: string, location?: string): Promise<ApiResponse<ContractItem>> {
    return request.post(`/crm/contract/${id}/sign`, null, { params: { signMethod, location } })
  },
  terminate(id: number, reason: string): Promise<ApiResponse<ContractItem>> {
    return request.post(`/crm/contract/${id}/terminate`, null, { params: { reason } })
  },
  getStatistics(): Promise<ApiResponse<any>> {
    return request.get('/crm/contract/statistics')
  }
}

// ── 发票 ──────────────────────────────────────────
export interface InvoiceItem {
  id: number
  invoiceNo?: string
  invoiceType?: string
  customerId?: number
  customerName: string
  invoiceDate: string
  amount: number
  taxAmount?: number
  totalAmount?: number
  status?: string
  issuer?: string
  remark?: string
  createTime: string
}

export interface InvoiceQuery {
  keyword?: string
  customerId?: number
  status?: string
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

export const invoiceApi = {
  page(params: InvoiceQuery): Promise<any> {
    return request.get('/erp/invoice/page', params)
  },
  getById(id: number): Promise<ApiResponse<InvoiceItem>> {
    return request.get(`/erp/invoice/${id}`)
  },
  create(data: Partial<InvoiceItem>): Promise<ApiResponse<InvoiceItem>> {
    return request.post('/erp/invoice/create-from-application', data)
  },
  update(id: number, data: Partial<InvoiceItem>): Promise<ApiResponse<InvoiceItem>> {
    return request.put(`/erp/invoice/${id}`, data)
  },
  updateStatus(id: number, newStatus: string, notes?: string): Promise<ApiResponse<boolean>> {
    return request.put(`/erp/invoice/${id}/status`, null, { params: { newStatus, notes } })
  },
  voidInvoice(id: number, reason: string): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/invoice/${id}/void`, null, { params: { reason } })
  },
  sendInvoice(id: number, sendMethod: string): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/invoice/${id}/send`, null, { params: { sendMethod } })
  },
  getStatistics(startDate?: string, endDate?: string): Promise<ApiResponse<any>> {
    return request.get('/erp/invoice/statistics', { startDate, endDate })
  }
}

// ══════════════════════════════════════════════════════════════════
// CRM 辅助域：跟进记录 / 客户分级 / 线索转化 / 商机阶段 / 合同审批
//
// 注：crm 模块后端无统一响应包装（裸 Page / 裸 List / 裸实体）。
// 查询类（Page=records+total / List=数组）走标准 request，拦截器原样透传；
// 变更类（POST/PUT 返回裸实体，无 code 字段）会被标准拦截器误判为失败，
// 参照 @/api/erp/batch 的既定做法：原生 axios + getToken 自行解包。
// ══════════════════════════════════════════════════════════════════

const CRM_BASE = '/api/crm'

function authHeaders(): Record<string, string> {
  const token = getToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}

/** 裸实体响应解包：HTTP 200 即成功；body 带数值 code≠200 视为业务错误 */
function unwrapRaw<T>(body: any): T {
  if (
    body && typeof body === 'object' && !Array.isArray(body) &&
    typeof body.code === 'number' && body.code !== 200 && 'message' in body
  ) {
    throw new Error(body.message || `请求失败(${body.code})`)
  }
  return body as T
}

function extractError(e: any): Error {
  const msg = e?.response?.data?.message || e?.message || '请求失败'
  return new Error(msg)
}

/** POST（裸实体响应） */
async function postRaw<T = any>(url: string, data?: any, params?: Record<string, any>): Promise<T> {
  try {
    const res = await axios.post(url, data ?? null, { headers: authHeaders(), params })
    return unwrapRaw<T>(res.data)
  } catch (e) {
    throw extractError(e)
  }
}

/** PUT（裸实体响应） */
async function putRaw<T = any>(url: string, data?: any, params?: Record<string, any>): Promise<T> {
  try {
    const res = await axios.put(url, data ?? null, { headers: authHeaders(), params })
    return unwrapRaw<T>(res.data)
  } catch (e) {
    throw extractError(e)
  }
}

// ── 跟进记录（/api/crm/followUp） ─────────────────────────────────

/** 跟进记录（与后端 CustomerFollowUp 实体一致） */
export interface FollowUpRecord {
  id: number
  followUpCode?: string
  customerId?: number
  customerName?: string
  opportunityId?: number
  opportunityName?: string
  leadId?: number
  leadName?: string
  /** 跟进类型：1电话 2拜访 3邮件 4微信 5其他 */
  followUpType?: number
  followUpTypeDesc?: string
  contactName?: string
  contactPhone?: string
  followUpDate?: string
  content?: string
  nextAction?: string
  nextFollowUpDate?: string
  /** 跟进结果：1有意向 2无意向 3待跟进 */
  followUpResult?: number
  followUpResultDesc?: string
  salesPersonId?: number
  salesPersonName?: string
  remark?: string
  createdAt?: string
}

export interface FollowUpQuery {
  customerId?: number
  opportunityId?: number
  leadId?: number
  salesPersonId?: number
  /** 跟进方式 1 电话 / 2 拜访 / 3 邮件 / 4 微信 / 5 其他 */
  followUpType?: number
  /** 跟进结果 1 有意向 / 2 无意向 / 3 待跟进 */
  followUpResult?: number
  followUpDateStart?: string
  followUpDateEnd?: string
  /** 下次跟进日期区间 —— 「待办跟进」口径 */
  nextFollowUpDateStart?: string
  nextFollowUpDateEnd?: string
  /** 关键词：单号 / 客户名 / 内容 */
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export const followUpApi = {
  /** 分页查询跟进记录（裸 Page：records/total） */
  page(params: FollowUpQuery): Promise<PageResponse<FollowUpRecord>> {
    return request.get('/crm/followUp/page', params)
  },
  /** 创建跟进记录（裸实体响应） */
  create(data: Partial<FollowUpRecord>): Promise<FollowUpRecord> {
    return postRaw(`${CRM_BASE}/followUp`, data)
  },
  /** 更新跟进记录（裸实体响应） */
  update(id: number, data: Partial<FollowUpRecord>): Promise<FollowUpRecord> {
    return putRaw(`${CRM_BASE}/followUp/${id}`, data)
  },
  /** 删除跟进记录（逻辑删，返回裸 boolean） */
  delete(id: number): Promise<boolean> {
    return request.delete(`${CRM_BASE}/followUp/${id}`)
  },
  /** 查询客户的跟进记录（裸 List） */
  listByCustomer(customerId: number): Promise<FollowUpRecord[]> {
    return request.get(`/crm/followUp/customer/${customerId}`)
  }
}

// ── 客户（分级/分析，对接 /api/customer） ──────────────────────────

/** CRM 客户（与后端 Customer 实体一致） */
export interface CrmCustomer {
  id: number
  customerCode?: string
  customerName: string
  shortName?: string
  customerType?: number
  customerSource?: number
  industryType?: number
  province?: string
  city?: string
  address?: string
  phone?: string
  email?: string
  businessContact?: string
  businessContactPhone?: string
  /** 客户等级：1VIP客户 2重要客户 3普通客户 4潜在客户 */
  customerLevel?: number
  customerLevelDesc?: string
  creditLimit?: number
  currentDebt?: number
  tradeCount?: number
  tradeAmount?: number
  potentialAmount?: number
  firstTradeDate?: string
  lastTradeDate?: string
  status?: number
  statusDesc?: string
  salesPersonId?: number
  salesPersonName?: string
  remark?: string
  createdAt?: string
}

export interface CrmCustomerQuery {
  keyword?: string
  customerType?: number
  customerLevel?: number
  status?: number
  salesPersonId?: number
  pageNum?: number
  pageSize?: number
}

export const crmCustomerApi = {
  /** 分页查询客户（裸 Page：records/total） */
  page(params: CrmCustomerQuery): Promise<PageResponse<CrmCustomer>> {
    return request.get('/customer/page', params)
  },
  /** 客户全量列表（裸 List，供统计/图表前端聚合） */
  exportList(params?: Omit<CrmCustomerQuery, 'pageNum' | 'pageSize'>): Promise<CrmCustomer[]> {
    return request.get('/customer/export', params)
  },
  /** 客户下拉选项（裸 List<{id, name}>，仅启用客户，上限200） */
  dropdown(keyword?: string): Promise<{ id: number; name: string }[]> {
    return request.get('/customer/dropdown', { keyword })
  },
  /** 更新客户（裸实体响应；MyBatis-Plus updateById 仅更新非空字段） */
  update(id: number, data: Partial<CrmCustomer>): Promise<CrmCustomer> {
    return putRaw(`/api/customer/${id}`, data)
  }
}

// ── 线索转化（/api/crm/lead） ──────────────────────────────────────

/** 线索行（与后端 CustomerLead 实体一致） */
export interface LeadRecord {
  id: number
  leadCode?: string
  leadName?: string
  contactName?: string
  contactPhone?: string
  companyName?: string
  leadSource?: number
  /** 线索状态：0新线索 1跟进中 2无效 3已转化（后端转化后写死为3） */
  leadStatus?: number
  leadStatusDesc?: string
  leadLevel?: number
  estimatedAmount?: number
  requirement?: string
  salesPersonId?: number
  salesPersonName?: string
  expectedCloseDate?: string
  convertedCustomerId?: number
  convertedTime?: string
  createdAt?: string
}

export const leadConvertApi = {
  /** 线索全量列表（裸 List，供转化统计卡片前端聚合） */
  exportList(params?: { keyword?: string; leadStatus?: number; leadLevel?: number; salesPersonId?: number }): Promise<LeadRecord[]> {
    return request.get('/crm/lead/export', params)
  },
  /** 转化线索为客户（裸实体响应；已转化后端抛 400"线索已转化"） */
  convert(id: number): Promise<CrmCustomer> {
    return postRaw(`${CRM_BASE}/lead/${id}/convert`)
  }
}

// ── 商机阶段（/api/crm/opportunity） ───────────────────────────────

/** 商机行（与后端 CustomerOpportunity 实体一致） */
export interface OpportunityRecord {
  id: number
  opportunityCode?: string
  opportunityName?: string
  customerId?: number
  customerName?: string
  /** 商机阶段：1初步接触 2需求确认 3方案报价 4商务谈判 5成交 */
  opportunityStage?: number
  opportunityStageDesc?: string
  estimatedAmount?: number
  actualAmount?: number
  probability?: number
  /** 状态：1跟进中 2赢单 3输单 */
  status?: number
  statusDesc?: string
  loseReason?: string
  salesPersonId?: number
  salesPersonName?: string
  expectedCloseDate?: string
  actualCloseDate?: string
  createdAt?: string
}

export const opportunityStageApi = {
  /** 商机全量列表（裸 List，供漏斗图前端聚合） */
  exportList(params?: { keyword?: string; customerId?: number; opportunityStage?: number; status?: number; salesPersonId?: number }): Promise<OpportunityRecord[]> {
    return request.get('/crm/opportunity/export', params)
  },
  /** 推进商机阶段（裸实体响应；最终阶段后端抛 400） */
  advance(id: number): Promise<OpportunityRecord> {
    return postRaw(`${CRM_BASE}/opportunity/${id}/advance`)
  },
  /** 商机赢单（裸实体响应） */
  win(id: number, actualAmount: number): Promise<OpportunityRecord> {
    return postRaw(`${CRM_BASE}/opportunity/${id}/win`, null, { actualAmount })
  },
  /** 商机输单（裸实体响应） */
  lose(id: number, loseReason: string): Promise<OpportunityRecord> {
    return postRaw(`${CRM_BASE}/opportunity/${id}/lose`, null, { loseReason })
  }
}

// ── 合同审批（/api/crm/contract） ──────────────────────────────────

export const contractApprovalApi = {
  /** 合同全量列表（裸 List，供审批统计卡片前端聚合） */
  exportList(params?: { keyword?: string; customerId?: number; status?: number; contractType?: number; salesPersonId?: number }): Promise<ContractItem[]> {
    return request.get('/crm/contract/export', params)
  },
  /** 提交审批（裸实体响应） */
  submit(id: number): Promise<ContractItem> {
    return postRaw(`${CRM_BASE}/contract/${id}/submit`)
  },
  /** 审批通过（裸实体响应） */
  approve(id: number, note?: string): Promise<ContractItem> {
    return postRaw(`${CRM_BASE}/contract/${id}/approve`, null, { note })
  },
  /** 审批拒绝（裸实体响应） */
  reject(id: number, reason: string): Promise<ContractItem> {
    return postRaw(`${CRM_BASE}/contract/${id}/reject`, null, { reason })
  }
}

// ── 外勤拜访（/api/crm/visit，对接 VisitController） ─────────────────
//
// 计划/记录分页为裸 Page（records+total），走标准 request 透传；
// 创建/更新/删除/取消返回裸实体或裸 boolean，检视/统计返回裸 Map，
// 均无 code 字段，会被标准拦截器误判为失败，统一走原生 axios 解包。

/** GET（裸 Map / 裸实体响应） */
async function getRaw<T = any>(url: string, params?: Record<string, any>): Promise<T> {
  try {
    const res = await axios.get(url, { headers: authHeaders(), params })
    return unwrapRaw<T>(res.data)
  } catch (e) {
    throw extractError(e)
  }
}

/** DELETE（裸 boolean 响应） */
async function deleteRaw<T = any>(url: string, params?: Record<string, any>): Promise<T> {
  try {
    const res = await axios.delete(url, { headers: authHeaders(), params })
    return unwrapRaw<T>(res.data)
  } catch (e) {
    throw extractError(e)
  }
}

/** 拜访计划（与后端 VisitPlan 实体一致，status：0待执行 1执行中 2已完成 3已取消） */
export interface VisitPlan {
  id?: number
  planNo?: string
  customerId?: number
  customerName?: string
  salesPersonId?: number
  salesPersonName?: string
  planDate?: string
  planTime?: string
  purpose?: string
  address?: string
  /** 状态：0待执行 1执行中 2已完成 3已取消 */
  status?: number
  remark?: string
  createdAt?: string
}

export interface VisitPlanQuery {
  customerId?: number
  salesPersonId?: number
  status?: number
  planDateStart?: string
  planDateEnd?: string
  page?: number
  size?: number
}

export const visitPlanApi = {
  /** 分页查询拜访计划（裸 Page：records/total） */
  page(params: VisitPlanQuery): Promise<PageResponse<VisitPlan>> {
    return request.get('/crm/visit/plan/page', params)
  },
  /** 创建拜访计划（裸实体响应，planNo/status 后端自动补默认） */
  create(data: Partial<VisitPlan>): Promise<VisitPlan> {
    return postRaw(`${CRM_BASE}/visit/plan`, data)
  },
  /** 更新拜访计划（裸实体响应） */
  update(id: number, data: Partial<VisitPlan>): Promise<VisitPlan> {
    return putRaw(`${CRM_BASE}/visit/plan/${id}`, data)
  },
  /** 删除拜访计划（裸 boolean 响应） */
  remove(id: number): Promise<boolean> {
    return deleteRaw(`${CRM_BASE}/visit/plan/${id}`)
  },
  /** 取消拜访计划（裸 boolean 响应，后端置 status=3） */
  cancel(id: number): Promise<boolean> {
    return putRaw(`${CRM_BASE}/visit/plan/${id}/cancel`)
  }
}

/** 拜访执行记录（与后端 VisitRecord 实体一致，visitType：1上门 2电话 3其他，result：1有意向 2一般 3无意向） */
export interface VisitRecord {
  id?: number
  planId?: number
  customerId?: number
  customerName?: string
  salesPersonId?: number
  salesPersonName?: string
  visitTime?: string
  /** 拜访方式：1上门 2电话 3其他 */
  visitType?: number
  location?: string
  longitude?: number
  latitude?: number
  content?: string
  /** 拜访结果：1有意向 2一般 3无意向 */
  result?: number
  nextAction?: string
  nextVisitDate?: string
  attachments?: string
  createTime?: string
}

export interface VisitRecordQuery {
  customerId?: number
  salesPersonId?: number
  result?: number
  visitDateStart?: string
  visitDateEnd?: string
  page?: number
  size?: number
}

export const visitRecordApi = {
  /** 分页查询拜访执行记录（裸 Page：records/total） */
  page(params: VisitRecordQuery): Promise<PageResponse<VisitRecord>> {
    return request.get('/crm/visit/record/page', params)
  },
  /** 签到打卡创建拜访记录（裸实体响应；planId 非空时后端联动计划置为已完成） */
  checkIn(data: Partial<VisitRecord>): Promise<VisitRecord> {
    return postRaw(`${CRM_BASE}/visit/record`, data)
  },
  /** 更新拜访记录（裸实体响应） */
  update(id: number, data: Partial<VisitRecord>): Promise<VisitRecord> {
    return putRaw(`${CRM_BASE}/visit/record/${id}`, data)
  },
  /** 删除拜访记录（裸 boolean 响应） */
  remove(id: number): Promise<boolean> {
    return deleteRaw(`${CRM_BASE}/visit/record/${id}`)
  }
}

/** 拜访检视结果分布计数 */
export interface VisitReviewSummary {
  total: number
  interested: number
  normal: number
  noIntention: number
}

/** 拜访检视响应（裸 Map：summary + byDate + page） */
export interface VisitReviewResponse {
  summary: VisitReviewSummary
  byDate: { visitDate: string; cnt: number }[]
  page: PageResponse<VisitRecord>
}

/** 拜访统计汇总（今日/本周拜访数 + 计划覆盖率） */
export interface VisitStatsSummary {
  todayCount: number
  weekCount: number
  planTotal: number
  planExecuted: number
  /** 覆盖率百分比数值（0-100，两位小数） */
  coverage: number
}

export const visitReviewApi = {
  /** 拜访检视：结果分布 + 按日期分组 + 分页列表（裸 Map 响应） */
  reviewPage(params: VisitRecordQuery): Promise<VisitReviewResponse> {
    return getRaw(`${CRM_BASE}/visit/review/page`, params)
  },
  /** 拜访统计汇总（裸 Map 响应） */
  statsSummary(): Promise<VisitStatsSummary> {
    return getRaw(`${CRM_BASE}/visit/stats/summary`)
  }
}

// ── 客户公海池（/api/crm/customer-pool） ───────────────────────────
//
// 后端 `CustomerPoolController` 的 10 个端点此前**前端零引用、无菜单** ——
// 能力齐全（放入/领取/退回/自动回收/过期检查/统计）但用户不可达。
// 本页（CRM → 客户管理 → 客户公海）把它接出来。

/** 公海池条目（与后端 CustomerPool 实体一致） */
export interface CustomerPoolItem {
  id: number
  customerId?: number
  customerCode?: string
  customerName?: string
  poolType?: number
  poolTypeDesc?: string
  poolReason?: number
  poolReasonDesc?: string
  originalSalesPersonId?: number
  originalSalesPersonName?: string
  originalDepartmentName?: string
  poolTime?: string
  poolDays?: number
  expireTime?: string
  /** 1 可领取 / 2 已领取 / 3 已过期 / 4 已退回 */
  status?: number
  statusDesc?: string
  claimSalesPersonId?: number
  claimSalesPersonName?: string
  claimTime?: string
  remark?: string
  createdAt?: string
}

export interface CustomerPoolQuery {
  keyword?: string
  poolType?: number
  status?: number
  pageNum?: number
  pageSize?: number
}

export interface CustomerPoolStatistics {
  availableCount?: number
  myClaimCount?: number
}

export const customerPoolApi = {
  /** 分页查询公海池（裸 Page） */
  page(params: CustomerPoolQuery): Promise<PageResponse<CustomerPoolItem>> {
    // ⚠️ 标准 `request` 实例的 baseURL 已是 `/api`，此处**必须写相对路径**；
    // 写成 `${CRM_BASE}/...` 会合成 `/api/api/...` 直接 404（2026-09-18 真机实测）
    return request.get('/crm/customer-pool/page', params)
  },
  /** 可领取客户列表（裸 List） */
  listAvailable(): Promise<CustomerPoolItem[]> {
    return request.get('/crm/customer-pool/available')
  },
  /** 我领取的客户（裸 List） */
  listMyClaimed(): Promise<CustomerPoolItem[]> {
    return request.get('/crm/customer-pool/my-claimed')
  },
  /** 我放入公海的客户（裸 List） */
  listMyReturned(): Promise<CustomerPoolItem[]> {
    return request.get('/crm/customer-pool/my-returned')
  },
  /** 放入公海池（裸实体） */
  put(customerId: number, poolReason: number, remark?: string): Promise<CustomerPoolItem> {
    return postRaw(`${CRM_BASE}/customer-pool/put/${customerId}`, null, {
      poolReason,
      remark
    })
  },
  /** 领取（裸实体） */
  claim(poolId: number): Promise<CustomerPoolItem> {
    return postRaw(`${CRM_BASE}/customer-pool/claim/${poolId}`)
  },
  /** 退回公海池（裸实体） */
  returnToPool(poolId: number, remark?: string): Promise<CustomerPoolItem> {
    return postRaw(`${CRM_BASE}/customer-pool/return/${poolId}`, null, { remark })
  },
  /** 执行自动回收（无返回体） */
  autoRecovery(noFollowUpDays = 30): Promise<void> {
    return postRaw(`${CRM_BASE}/customer-pool/auto-recovery`, null, { noFollowUpDays })
  },
  /** 检查过期客户（无返回体） */
  checkExpired(): Promise<void> {
    return postRaw(`${CRM_BASE}/customer-pool/check-expired`)
  },
  /** 公海池统计（裸 Map） */
  statistics(): Promise<CustomerPoolStatistics> {
    return getRaw(`${CRM_BASE}/customer-pool/statistics`)
  }
}
