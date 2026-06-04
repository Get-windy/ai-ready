/**
 * CRM 模块 API
 * 线索/商机/合同/发票
 */
import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// ── 线索 ──────────────────────────────────────────
export interface Lead {
  id: number
  leadCode?: string
  name: string
  companyName: string
  contactName: string
  phone: string
  email: string
  source?: string
  status?: number
  score?: number
  leadLevel?: number
  salesPersonId?: number
  salesPersonName?: string
  remark?: string
  createdBy?: string
  createTime: string
  updateTime?: string
}

export interface LeadQuery {
  keyword?: string
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
    return request.get('/api/crm/contract/page', params)
  },
  getById(id: number): Promise<ApiResponse<ContractItem>> {
    return request.get(`/api/crm/contract/${id}`)
  },
  create(data: Partial<ContractItem>): Promise<ApiResponse<ContractItem>> {
    return request.post('/api/crm/contract', data)
  },
  update(id: number, data: Partial<ContractItem>): Promise<ApiResponse<ContractItem>> {
    return request.put(`/api/crm/contract/${id}`, data)
  },
  submitForApproval(id: number): Promise<ApiResponse<ContractItem>> {
    return request.post(`/api/crm/contract/${id}/submit`)
  },
  approve(id: number, note?: string): Promise<ApiResponse<ContractItem>> {
    return request.post(`/api/crm/contract/${id}/approve`, null, { params: { note } })
  },
  reject(id: number, reason: string): Promise<ApiResponse<ContractItem>> {
    return request.post(`/api/crm/contract/${id}/reject`, null, { params: { reason } })
  },
  sign(id: number, signMethod: string, location?: string): Promise<ApiResponse<ContractItem>> {
    return request.post(`/api/crm/contract/${id}/sign`, null, { params: { signMethod, location } })
  },
  terminate(id: number, reason: string): Promise<ApiResponse<ContractItem>> {
    return request.post(`/api/crm/contract/${id}/terminate`, null, { params: { reason } })
  },
  getStatistics(): Promise<ApiResponse<any>> {
    return request.get('/api/crm/contract/statistics')
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
    return request.get('/api/erp/invoice/page', params)
  },
  getById(id: number): Promise<ApiResponse<InvoiceItem>> {
    return request.get(`/api/erp/invoice/${id}`)
  },
  create(data: Partial<InvoiceItem>): Promise<ApiResponse<InvoiceItem>> {
    return request.post('/api/erp/invoice/create-from-application', data)
  },
  updateStatus(id: number, newStatus: string, notes?: string): Promise<ApiResponse<boolean>> {
    return request.put(`/api/erp/invoice/${id}/status`, null, { params: { newStatus, notes } })
  },
  voidInvoice(id: number, reason: string): Promise<ApiResponse<boolean>> {
    return request.post(`/api/erp/invoice/${id}/void`, null, { params: { reason } })
  },
  sendInvoice(id: number, sendMethod: string): Promise<ApiResponse<boolean>> {
    return request.post(`/api/erp/invoice/${id}/send`, null, { params: { sendMethod } })
  },
  getStatistics(startDate?: string, endDate?: string): Promise<ApiResponse<any>> {
    return request.get('/api/erp/invoice/statistics', { startDate, endDate })
  }
}
