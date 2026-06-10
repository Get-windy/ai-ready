/**
 * 费用管理 API 模块
 * 遵循现有 API 模式: request.get/post/put/delete
 */
import request from '@/utils/request'
import type { ApiResponse, PageResponse } from '@/utils/request'

// ════════════════════════════════════════════════════════════
// 费用申请
// ════════════════════════════════════════════════════════════
export interface FeeApplication {
  id: number
  applicationNo: string
  applicationTitle: string
  applicantId: number
  applicantName: string
  departmentId: number
  departmentName: string
  expenseType: string
  expenseTypeDesc: string
  totalAmount: number
  currency: string
  budgetAmount: number
  budgetUsageRate: number
  exceedBudget: number
  applyDate: string
  purpose: string
  description: string
  isUrgent: number
  status: string
  currentApproverId: number
  currentApproverName: string
  currentApprovalLevel: number
  totalApprovalLevel: number
  approvalComment: string
  rejectReason: string
  reimbursementStatus: string
  reimbursedAmount: number
  paymentStatus: string
  paidAmount: number
  remark: string
  createTime: string
  items?: FeeApplicationItem[]
  approvalRecords?: FeeApprovalRecord[]
}

export interface FeeApplicationItem {
  id: number
  itemName: string
  description: string
  expenseDate: string
  amount: number
  quantity: number
  unitPrice: number
  unit: string
  vendorName: string
  taxRate: number
  taxAmount: number
  totalAmountWithTax: number
  hasInvoice: number
  invoiceNumber: string
  projectCode: string
  costCenter: string
  sequenceNumber: number
  remark: string
}

export const feeApplicationApi = {
  page(params: Record<string, any>): Promise<ApiResponse<PageResponse<FeeApplication>>> {
    return request.get('/erp/expense/application/page', params)
  },
  getById(id: number): Promise<ApiResponse<FeeApplication>> {
    return request.get(`/erp/expense/application/${id}`)
  },
  create(data: Record<string, any>): Promise<ApiResponse<number>> {
    return request.post('/erp/expense/application', data)
  },
  update(id: number, data: Record<string, any>): Promise<ApiResponse<boolean>> {
    return request.put(`/erp/expense/application/${id}`, data)
  },
  delete(id: number): Promise<ApiResponse<boolean>> {
    return request.delete(`/erp/expense/application/${id}`)
  },
  submit(id: number): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/expense/application/${id}/submit`)
  },
  withdraw(id: number): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/expense/application/${id}/withdraw`)
  }
}

// ════════════════════════════════════════════════════════════
// 费用报销
// ════════════════════════════════════════════════════════════
export interface FeeReimbursement {
  id: number
  reimbursementNo: string
  reimbursementTitle: string
  applicantId: number
  applicantName: string
  departmentId: number
  departmentName: string
  applicationId: number
  applicationNo: string
  totalAmount: number
  currency: string
  reimbursementDate: string
  purpose: string
  description: string
  status: string
  currentApproverId: number
  currentApproverName: string
  paymentMethod: string
  paymentStatus: string
  paidAmount: number
  paymentDate: string
  paymentVoucherNo: string
  remark: string
  createTime: string
  items?: FeeReimbursementItem[]
  approvalRecords?: FeeApprovalRecord[]
}

export interface FeeReimbursementItem {
  id: number
  applicationItemId: number
  itemName: string
  description: string
  expenseDate: string
  amount: number
  quantity: number
  unitPrice: number
  unit: string
  vendorName: string
  hasInvoice: number
  invoiceNumber: string
  projectCode: string
  costCenter: string
  sequenceNumber: number
  remark: string
}

export const feeReimbursementApi = {
  page(params: Record<string, any>): Promise<ApiResponse<PageResponse<FeeReimbursement>>> {
    return request.get('/erp/expense/reimbursement/page', params)
  },
  getById(id: number): Promise<ApiResponse<FeeReimbursement>> {
    return request.get(`/erp/expense/reimbursement/${id}`)
  },
  create(data: Record<string, any>): Promise<ApiResponse<number>> {
    return request.post('/erp/expense/reimbursement', data)
  },
  update(id: number, data: Record<string, any>): Promise<ApiResponse<boolean>> {
    return request.put(`/erp/expense/reimbursement/${id}`, data)
  },
  delete(id: number): Promise<ApiResponse<boolean>> {
    return request.delete(`/erp/expense/reimbursement/${id}`)
  },
  submit(id: number): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/expense/reimbursement/${id}/submit`)
  },
  withdraw(id: number): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/expense/reimbursement/${id}/withdraw`)
  }
}

// ════════════════════════════════════════════════════════════
// 审批记录
// ════════════════════════════════════════════════════════════
export interface FeeApprovalRecord {
  id: number
  businessType: string
  businessId: number
  approvalLevel: number
  approverId: number
  approverName: string
  approverDepartmentName: string
  approvalAction: string
  approvalComment: string
  approvalTime: string
  previousStatus: string
  currentStatus: string
}

export const feeApprovalApi = {
  process(data: Record<string, any>): Promise<ApiResponse<boolean>> {
    return request.post('/erp/expense/approval/process', data)
  },
  getRecords(businessType: string, businessId: number): Promise<ApiResponse<FeeApprovalRecord[]>> {
    return request.get('/erp/expense/approval/records', { businessType, businessId })
  },
  getPending(pageNum = 1, pageSize = 20): Promise<ApiResponse<PageResponse<FeeApprovalRecord>>> {
    return request.get('/erp/expense/approval/pending', { pageNum, pageSize })
  }
}

// ════════════════════════════════════════════════════════════
// 付款记录
// ════════════════════════════════════════════════════════════
export interface FeePaymentRecord {
  id: number
  businessType: string
  businessId: number
  businessNo: string
  paymentNo: string
  paymentAmount: number
  currency: string
  paymentMethod: string
  paymentAccount: string
  payeeName: string
  payeeAccount: string
  paymentDate: string
  voucherNo: string
  payerName: string
  status: string
  failReason: string
  confirmTime: string
  confirmUserName: string
  remark: string
  createTime: string
}

export const feePaymentApi = {
  page(params: Record<string, any>): Promise<ApiResponse<PageResponse<FeePaymentRecord>>> {
    return request.get('/erp/expense/payment/page', params)
  },
  getById(id: number): Promise<ApiResponse<FeePaymentRecord>> {
    return request.get(`/erp/expense/payment/${id}`)
  },
  create(data: Record<string, any>): Promise<ApiResponse<number>> {
    return request.post('/erp/expense/payment', data)
  },
  confirmPayment(id: number): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/expense/payment/${id}/confirm`)
  },
  cancelPayment(id: number, reason: string): Promise<ApiResponse<boolean>> {
    return request.post(`/erp/expense/payment/${id}/cancel`, null, { params: { reason } })
  }
}

// ════════════════════════════════════════════════════════════
// 费用统计
// ════════════════════════════════════════════════════════════
export interface FeeStatistics {
  id: number
  statYear: number
  statMonth: number
  statDate: string
  departmentId: number
  departmentName: string
  expenseType: string
  applyCount: number
  applyAmount: number
  approvedCount: number
  approvedAmount: number
  rejectedCount: number
  rejectedAmount: number
  reimbursementCount: number
  reimbursementAmount: number
  paidCount: number
  paidAmount: number
  budgetAmount: number
  budgetUsageRate: number
}

export const feeStatisticsApi = {
  page(params: Record<string, any>): Promise<ApiResponse<PageResponse<FeeStatistics>>> {
    return request.get('/erp/expense/statistics/page', params)
  },
  getSummary(year?: number, month?: number, departmentId?: number): Promise<ApiResponse<Record<string, any>>> {
    return request.get('/erp/expense/statistics/summary', { year, month, departmentId })
  },
  getByDepartment(year?: number, month?: number): Promise<ApiResponse<FeeStatistics[]>> {
    return request.get('/erp/expense/statistics/by-department', { year, month })
  },
  getByType(year?: number, month?: number, departmentId?: number): Promise<ApiResponse<FeeStatistics[]>> {
    return request.get('/erp/expense/statistics/by-type', { year, month, departmentId })
  }
}

export default { feeApplicationApi, feeReimbursementApi, feeApprovalApi, feePaymentApi, feeStatisticsApi }
