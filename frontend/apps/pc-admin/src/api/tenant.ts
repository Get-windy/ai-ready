import request, { type ApiResponse, type PageResponse } from '@/utils/request'

/** 租户信息 */
export interface TenantInfo {
  id: number
  tenantCode: string
  tenantName: string
  contactName?: string
  contactPhone?: string
  contactEmail?: string
  address?: string
  description?: string
  status: number
  maxUsers?: number
  expireDate?: string
  /** 租户管理员用户ID */
  adminUserId?: number
  /** 租户等级：basic-基础版 professional-专业版 enterprise-企业版 */
  level?: string
  createTime?: string
  updateTime?: string
}

/** 租户查询参数 */
export interface TenantQuery {
  tenantName?: string
  tenantCode?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

/** 租户配置信息 */
export interface TenantConfigInfo {
  id: number
  tenantId: number
  logo?: string
  themeColor?: string
  features?: string
  maxUsers?: number
  expireDate?: string
}

/** 租户 API */
export const tenantApi = {
  /** 分页查询租户 */
  getPage(params: TenantQuery): Promise<ApiResponse<PageResponse<TenantInfo>>> {
    return request.get('/tenant/page', params)
  },

  /** 获取租户详情 */
  getById(id: number): Promise<ApiResponse<TenantInfo>> {
    return request.get(`/tenant/${id}`)
  },

  /** 创建租户 */
  create(data: Partial<TenantInfo>): Promise<ApiResponse<boolean>> {
    return request.post('/tenant', data)
  },

  /** 更新租户 */
  update(id: number, data: Partial<TenantInfo>): Promise<ApiResponse<boolean>> {
    return request.put(`/tenant/${id}`, data)
  },

  /** 删除租户 */
  delete(id: number): Promise<ApiResponse<boolean>> {
    return request.delete(`/tenant/${id}`)
  },

  /** 批量删除租户 */
  batchDelete(ids: number[]): Promise<ApiResponse<boolean>> {
    return request.delete('/tenant/batch', { data: ids })
  },

  /** 更新租户状态 */
  updateStatus(id: number, status: number): Promise<ApiResponse<boolean>> {
    return request.put(`/tenant/${id}/status`, null, { params: { status } })
  },

  /** 获取租户配置 */
  getConfig(tenantId: number): Promise<ApiResponse<TenantConfigInfo>> {
    return request.get(`/tenant/${tenantId}/config`)
  },

  /** 更新租户配置 */
  updateConfig(tenantId: number, data: Partial<TenantConfigInfo>): Promise<ApiResponse<boolean>> {
    return request.put(`/tenant/${tenantId}/config`, data)
  }
}

// ── 租户注册审批 API ──────────────────────────────────────

/** 租户注册请求 */
export interface TenantRegisterForm {
  tenantName: string
  tenantCode: string
  contactPerson: string
  contactPhone: string
  contactEmail: string
  adminUsername: string
  adminPassword: string
  adminEmail: string
}

/** 租户审批 API */
export const tenantApprovalApi = {
  /** 租户自助注册（公开接口） */
  register(data: TenantRegisterForm): Promise<ApiResponse<any>> {
    return request.post('/tenant/register', data, { _skipAuthRefresh: true })
  },

  /** 审批通过租户 */
  approve(id: number, remark?: string): Promise<ApiResponse<void>> {
    return request.post(`/tenant/${id}/approve`, remark ? { remark } : undefined)
  },

  /** 驳回租户注册 */
  reject(id: number, reason: string): Promise<ApiResponse<void>> {
    return request.post(`/tenant/${id}/reject`, { reason })
  },

  /** 查询待审核租户列表 */
  getPending(): Promise<ApiResponse<SysTenant[]>> {
    return request.get('/tenant/pending')
  }
}

/** 待审核租户接口（后端 SysTenant 实体的前端表示） */
export interface SysTenant {
  id: number
  tenantName: string
  tenantCode: string
  contactPerson: string
  contactPhone: string
  contactEmail: string
  address?: string
  adminUserId?: number
  level?: string
  status: number
  remark?: string
  createTime: string
}
