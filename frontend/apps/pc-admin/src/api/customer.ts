import request, { type ApiResponse, type PageResponse } from '@/utils/request'

/**
 * 客户信息（对齐后端 Customer 实体：crm/customer/entity/Customer.java）
 */
export interface CustomerInfo {
  id: number
  tenantId: number
  customerCode: string
  customerName: string
  shortName: string
  customerType: number
  customerSource: number
  industryType: number
  province: string
  city: string
  district: string
  address: string
  phone: string
  fax: string
  email: string
  website: string
  legalPerson: string
  businessContact: string
  businessContactPhone: string
  financeContact: string
  financeContactPhone: string
  taxNumber: string
  bankName: string
  bankAccount: string
  customerLevel: number
  customerLevelDesc: string
  creditLimit: number
  currentDebt: number
  settlementType: number
  settlementDays: number
  status: number
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
    return request.get('/crm/customer/page', params)
  },

  /**
   * 获取客户详情
   */
  getById(id: number): Promise<ApiResponse<CustomerInfo>> {
    return request.get(`/crm/customer/${id}`)
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
    return request.put(`/crm/customer/${id}`, data)
  },

  /**
   * 删除客户
   */
  delete(id: number): Promise<ApiResponse<boolean>> {
    return request.delete(`/crm/customer/${id}`)
  },

  /**
   * 批量删除客户
   */
  batchDelete(ids: number[]): Promise<ApiResponse<boolean>> {
    return request.delete('/crm/customer/batch', { data: ids })
  },

  // ===== 跟进记录相关 =====

  /**
   * 添加跟进记录
   */
  addFollowRecord(customerId: number, data: Partial<FollowRecord>): Promise<ApiResponse<boolean>> {
    return request.post(`/crm/customer/${customerId}/follow`, data)
  },

  /**
   * 导入客户（CSV文件上传）
   */
  importCustomers(data: { file: File; mapping: Record<string, string> }): Promise<ApiResponse<any>> {
    const formData = new FormData()
    formData.append('file', data.file)
    formData.append('mapping', JSON.stringify(data.mapping))
    return request.post('/crm/customer/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

export default customerApi