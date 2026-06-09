/**
 * 供应商管理 API
 */
import request from '@/utils/request'

export interface Supplier {
  id: number
  tenantId: number
  supplierCode: string
  supplierName: string
  shortName?: string
  supplierType?: number
  supplierLevel: string
  cooperationStatus: number
  contactPerson?: string
  contactPhone?: string
  email?: string
  address?: string
  province?: string
  city?: string
  bankName?: string
  bankAccount?: string
  taxNumber?: string
  comprehensiveScore: number
  totalPoints: number
  portalStatus: number
  portalAccountId?: string
  createTime?: string
  updateTime?: string
  remark?: string
}

export interface SupplierQuery {
  tenantId?: number
  pageNum?: number
  pageSize?: number
  keyword?: string
  supplierLevel?: string
  cooperationStatus?: number
  portalStatus?: number
}

export interface SupplierPageResult {
  records: Supplier[]
  total: number
  size: number
  current: number
}

export const supplierApi = {
  /** 分页查询供应商 */
  page(params: SupplierQuery): Promise<SupplierPageResult> {
    return request.post('/supplier/page', params)
  },

  /** 获取供应商详情 */
  getById(id: number): Promise<Supplier> {
    return request.get(`/supplier/${id}`)
  },

  /** 创建供应商 */
  create(data: Partial<Supplier>): Promise<Supplier> {
    return request.post('/supplier', data)
  },

  /** 更新供应商 */
  update(id: number, data: Partial<Supplier>): Promise<Supplier> {
    return request.put(`/supplier/${id}`, data)
  },

  /** 删除供应商 */
  delete(id: number): Promise<void> {
    return request.delete(`/supplier/${id}`)
  },

  /** 激活门户 */
  activatePortal(id: number, portalAccountId?: string): Promise<void> {
    return request.post(`/supplier/${id}/activate-portal`, { portalAccountId })
  },

  /** 禁用门户 */
  disablePortal(id: number, reason?: string): Promise<void> {
    return request.post(`/supplier/${id}/disable-portal`, { reason })
  },

  /** 获取供应商绩效记录 */
  getPerformanceHistory(id: number): Promise<any> {
    return request.get(`/supplier/${id}/performance/history`)
  },

  /** 获取供应商询价记录 */
  getInquiries(id: number): Promise<any> {
    return request.get(`/supplier-portal/inquiries/supplier/${id}`)
  },

  /** 获取供应商积分记录 */
  getPointsRecords(id: number): Promise<any> {
    return request.get(`/supplier-portal/points/${id}/records`)
  },

  /** 获取供应商统计信息 */
  getStatistics(): Promise<{
    totalSuppliers: number
    levelACount: number
    activeCooperationCount: number
    portalActivatedCount: number
  }> {
    return request.get('/supplier/statistics')
  },

  /** 导入供应商（批量上传） */
  importSuppliers(data: FormData | any[]): Promise<void> {
    return request.post('/supplier/import', data)
  }
}
