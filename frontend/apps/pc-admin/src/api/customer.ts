import request, { type ApiResponse, type PageResponse } from '@/utils/request'

/**
 * 客户信息
 */
export interface CustomerInfo {
  id: number
  tenantId: number
  name: string
  code: string
  contactPerson: string
  phone: string
  email: string
  address: string
  level: number
  industry: string
  status: number
  description: string
  createTime: string
  updateTime: string
}

/**
 * 跟进记录
 */
export interface FollowRecord {
  id: number
  customerId: number
  customerName: string
  followType: number
  content: string
  result: number
  nextFollowTime: string
  followUser: string
  createTime: string
}

/**
 * 客户查询参数
 */
export interface CustomerQuery {
  tenantId?: number
  name?: string
  code?: string
  contactPerson?: string
  phone?: string
  level?: number
  industry?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

/**
 * 跟进记录查询参数
 */
export interface FollowQuery {
  customerId?: number
  followType?: number
  result?: number
  pageNum?: number
  pageSize?: number
}

/**
 * 客户API
 */
export const customerApi = {
  /**
   * 分页查询客户
   */
  getPage(params: CustomerQuery): Promise<ApiResponse<PageResponse<CustomerInfo>>> {
    return request.get('/customer/page', params)
  },

  /**
   * 获取客户详情
   */
  getById(id: number): Promise<ApiResponse<CustomerInfo>> {
    return request.get(`/customer/${id}`)
  },

  /**
   * 创建客户
   */
  create(data: Partial<CustomerInfo>): Promise<ApiResponse<boolean>> {
    return request.post('/customer', data)
  },

  /**
   * 更新客户
   */
  update(id: number, data: Partial<CustomerInfo>): Promise<ApiResponse<boolean>> {
    return request.put(`/customer/${id}`, data)
  },

  /**
   * 删除客户
   */
  delete(id: number): Promise<ApiResponse<boolean>> {
    return request.delete(`/customer/${id}`)
  },

  /**
   * 批量删除客户
   */
  batchDelete(ids: number[]): Promise<ApiResponse<boolean>> {
    return request.delete('/customer/batch', { data: ids })
  },

  /**
   * 更新客户状态
   */
  updateStatus(id: number, status: number): Promise<ApiResponse<boolean>> {
    return request.patch(`/customer/${id}/status`, null, { params: { status } })
  },

  /**
   * 导出客户
   */
  export(params: CustomerQuery): Promise<Blob> {
    return request.get('/customer/export', params, { responseType: 'blob' })
  },

  // ===== 跟进记录相关 =====

  /**
   * 获取客户跟进记录列表
   */
  getFollowRecords(customerId: number, params: FollowQuery): Promise<ApiResponse<PageResponse<FollowRecord>>> {
    return request.get(`/customer/${customerId}/follows`, params)
  },

  /**
   * 添加跟进记录
   */
  addFollowRecord(customerId: number, data: Partial<FollowRecord>): Promise<ApiResponse<boolean>> {
    return request.post(`/customer/${customerId}/follow`, data)
  },

  /**
   * 获取客户订单记录
   */
  getOrderRecords(customerId: number): Promise<ApiResponse<any[]>> {
    return request.get(`/customer/${customerId}/orders`)
  },

  /**
   * 导入客户（CSV文件上传）
   */
  importCustomers(data: { file: File; mapping: Record<string, string> }): Promise<ApiResponse<any>> {
    const formData = new FormData()
    formData.append('file', data.file)
    formData.append('mapping', JSON.stringify(data.mapping))
    return request.post('/customer/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

export default customerApi