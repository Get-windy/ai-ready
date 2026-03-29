import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// 角色信息
export interface RoleInfo {
  id: number
  tenantId: number
  roleName: string
  roleCode: string
  roleType: number
  dataScope: number
  sort: number
  status: number
  remark: string
  createTime: string
}

// 角色查询参数
export interface RoleQuery {
  tenantId: number
  roleName?: string
  status?: number
  current?: number
  size?: number
}

// 角色API
export const roleApi = {
  // 分页查询角色
  getPage(params: RoleQuery): Promise<ApiResponse<PageResponse<RoleInfo>>> {
    return request.get('/role/page', { params })
  },

  // 获取角色详情
  getById(id: number): Promise<ApiResponse<RoleInfo>> {
    return request.get(`/role/${id}`)
  },

  // 创建角色
  create(data: Partial<RoleInfo>): Promise<ApiResponse<number>> {
    return request.post('/role', data)
  },

  // 更新角色
  update(id: number, data: Partial<RoleInfo>): Promise<ApiResponse<void>> {
    return request.put(`/role/${id}`, data)
  },

  // 删除角色
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/role/${id}`)
  },

  // 更新角色状态
  updateStatus(id: number, status: number): Promise<ApiResponse<void>> {
    return request.put(`/role/${id}/status`, null, { params: { status } })
  },

  // 分配权限
  assignPermissions(id: number, permissionIds: number[]): Promise<ApiResponse<void>> {
    return request.post(`/role/${id}/permissions`, permissionIds)
  },

  // 分配菜单
  assignMenus(id: number, menuIds: number[]): Promise<ApiResponse<void>> {
    return request.post(`/role/${id}/menus`, menuIds)
  },

  // 获取角色权限
  getPermissions(id: number): Promise<ApiResponse<number[]>> {
    return request.get(`/role/${id}/permissions`)
  }
}

export default roleApi