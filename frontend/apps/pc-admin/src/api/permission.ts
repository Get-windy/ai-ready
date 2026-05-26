import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// 权限信息
export interface PermissionInfo {
  id: number
  tenantId: number
  parentId: number
  permissionName: string
  permissionCode: string
  permissionType: number
  path: string
  component: string
  icon: string
  apiPath: string
  method: string
  sort: number
  visible: number
  status: number
}

// 权限查询参数
export interface PermissionQuery {
  tenantId: number
  permissionName?: string
  permissionType?: number
  status?: number
  current?: number
  size?: number
}

// 权限API
export const permissionApi = {
  // 分页查询权限
  getPage(params: PermissionQuery): Promise<ApiResponse<PageResponse<PermissionInfo>>> {
    return request.get('/permission/page', { params })
  },

  // 获取权限树
  getTree(tenantId: number): Promise<ApiResponse<PermissionInfo[]>> {
    return request.get('/permission/tree', { params: { tenantId } })
  },

  // 获取权限详情
  getById(id: number): Promise<ApiResponse<PermissionInfo>> {
    return request.get(`/permission/${id}`)
  },

  // 创建权限
  create(data: Partial<PermissionInfo>): Promise<ApiResponse<number>> {
    return request.post('/permission', data)
  },

  // 更新权限
  update(id: number, data: Partial<PermissionInfo>): Promise<ApiResponse<void>> {
    return request.put(`/permission/${id}`, data)
  },

  // 删除权限
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/permission/${id}`)
  },

  // 批量删除权限
  batchDelete(ids: number[]): Promise<ApiResponse<void>> {
    return request.delete('/permission/batch', { data: ids })
  },

  // 更新权限状态
  updateStatus(id: number, status: number): Promise<ApiResponse<void>> {
    return request.put(`/permission/${id}/status`, null, { params: { status } })
  },

  // 检查权限编码
  checkCode(code: string, tenantId: number, excludeId?: number): Promise<ApiResponse<boolean>> {
    return request.get('/permission/check-code', { params: { code, tenantId, excludeId } })
  }
}

export default permissionApi