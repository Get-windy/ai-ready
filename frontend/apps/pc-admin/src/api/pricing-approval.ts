import request, { type ApiResponse } from '@/utils/request'

// ── 类型定义 ────────────────────────────────────────────

export interface PriceApproval {
  id: number
  productId: number
  productName: string
  productCode: string
  customerId?: number
  customerName?: string
  oldPrice: number
  newPrice: number
  priceChange: number
  priceChangeType: 'increase' | 'decrease'
  approvalType: string
  approvalTypeLabel: string
  applicantId: number
  applicantName: string
  applyTime: string
  status: 'pending' | 'approved' | 'rejected'
  approverId?: number
  approverName?: string
  approveTime?: string
  approvalReason: string
  approveRemark?: string
}

export interface PriceApprovalStatistics {
  totalCount: number
  pendingCount: number
  approvedCount: number
  rejectedCount: number
}

export interface ApplyPriceChangeDTO {
  productId: number
  customerId?: number
  newPrice: number
  approvalType: string
  approvalReason: string
  effectiveStart?: string
  effectiveEnd?: string
}

export interface PriceApprovalQuery {
  current?: number
  size?: number
  status?: string
}

// ── API 方法 ────────────────────────────────────────────

export const priceApprovalApi = {
  /** 获取审批统计 */
  getStatistics(): Promise<ApiResponse<PriceApprovalStatistics>> {
    return request.get('/erp/pricing/approval/statistics')
  },

  /** 获取待审批列表 */
  getPendingList(): Promise<ApiResponse<PriceApproval[]>> {
    return request.get('/erp/pricing/approval/pending')
  },

  /** 按状态获取审批列表 */
  getListByStatus(status: string): Promise<ApiResponse<PriceApproval[]>> {
    return request.get(`/erp/pricing/approval/list/${status}`)
  },

  /** 获取我的申请 */
  getMyApprovals(applicantId: number): Promise<ApiResponse<PriceApproval[]>> {
    return request.get(`/erp/pricing/approval/my/${applicantId}`)
  },

  /** 获取审批详情 */
  getById(id: number): Promise<ApiResponse<PriceApproval>> {
    return request.get(`/erp/pricing/approval/${id}`)
  },

  /** 申请价格变更 */
  apply(data: ApplyPriceChangeDTO): Promise<ApiResponse<PriceApproval>> {
    return request.post('/erp/pricing/approval/apply', data)
  },

  /** 审批通过 */
  approve(id: number, approverId: number, remark?: string): Promise<ApiResponse<PriceApproval>> {
    return request.put(`/erp/pricing/approval/${id}/approve`, null, { params: { approverId, remark } })
  },

  /** 审批拒绝 */
  reject(id: number, approverId: number, remark: string): Promise<ApiResponse<PriceApproval>> {
    return request.put(`/erp/pricing/approval/${id}/reject`, null, { params: { approverId, remark } })
  }
}
