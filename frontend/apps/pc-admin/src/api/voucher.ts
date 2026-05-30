import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// 会计凭证
export interface VoucherInfo {
  id: number
  voucherNo: string
  voucherDate: string
  summary: string
  debitAmount: number
  creditAmount: number
  creatorName: string
  auditStatus: number // 0: 待审核, 1: 已审核, 2: 已驳回
  postStatus: number // 0: 未过账, 1: 已过账
  createTime?: string
  updateTime?: string
  entries?: VoucherEntry[]
}

// 凭证分录
export interface VoucherEntry {
  id?: number
  voucherId?: number
  accountCode: string
  accountName: string
  summary: string
  debitAmount: number
  creditAmount: number
  sortOrder: number
}

// 凭证查询参数
export interface VoucherQuery {
  voucherNo?: string
  startDate?: string
  endDate?: string
  auditStatus?: number
  postStatus?: number
  pageNum?: number
  pageSize?: number
}

// 凭证保存请求
export interface VoucherSaveRequest {
  voucherDate: string
  summary: string
  entries: VoucherEntry[]
}

// 凭证更新请求
export interface VoucherUpdateRequest extends VoucherSaveRequest {
  id: number
}

// 凭证API
export const voucherApi = {
  // 分页查询凭证
  getPage(params: VoucherQuery): Promise<ApiResponse<PageResponse<VoucherInfo>>> {
    return request.get('/voucher/page', { params })
  },

  // 获取凭证详情
  getById(id: number): Promise<ApiResponse<VoucherInfo>> {
    return request.get(`/voucher/${id}`)
  },

  // 创建凭证
  create(data: VoucherSaveRequest): Promise<ApiResponse<VoucherInfo>> {
    return request.post('/voucher', data)
  },

  // 更新凭证
  update(data: VoucherUpdateRequest): Promise<ApiResponse<VoucherInfo>> {
    return request.put(`/voucher/${data.id}`, data)
  },

  // 删除凭证
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/voucher/${id}`)
  },

  // 批量删除
  batchDelete(ids: number[]): Promise<ApiResponse<void>> {
    return request.delete('/voucher/batch', { data: ids })
  },

  // 审核凭证
  audit(id: number): Promise<ApiResponse<void>> {
    return request.post(`/voucher/${id}/audit`)
  },

  // 驳回凭证
  reject(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.post(`/voucher/${id}/reject`, null, { params: { reason } })
  },

  // 过账
  post(id: number): Promise<ApiResponse<void>> {
    return request.post(`/voucher/${id}/post`)
  },

  // 反过账
  unpost(id: number): Promise<ApiResponse<void>> {
    return request.post(`/voucher/${id}/unpost`)
  },

  // 获取凭证号
  generateVoucherNo(): Promise<ApiResponse<string>> {
    return request.get('/voucher/generate-no')
  }
}

export default voucherApi
