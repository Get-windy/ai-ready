import request, { type ApiResponse, type PageResponse } from '@/utils/request'

// 角色信息
export interface RoleInfo {
  id: number
  tenantId: number
  roleName: string
  roleCode: string
  roleType: number | string
  scope?: string
  dataScope: number
  sort: number
  status: number
  remark: string
  createTime: string
  userCount?: number
}

// 角色查询参数
export interface RoleQuery {
  tenantId?: number
  scope?: string
  roleName?: string
  roleCode?: string
  status?: number
  pageNum?: number
  pageSize?: number
}

// 角色API - 修复版
export const roleApi = {
  // 分页查询角色
  getPage(params: RoleQuery): Promise<ApiResponse<PageResponse<RoleInfo>>> {
    return request.get('/role/page', params)
  },

  // 获取角色详情
  getById(id: number): Promise<ApiResponse<RoleInfo>> {
    return request.get(`/role/${id}`)
  },

  // 创建角色
  create(data: Partial<RoleInfo>): Promise<ApiResponse<boolean>> {
    return request.post('/role', data)
  },

  // 更新角色
  update(id: number, data: Partial<RoleInfo>): Promise<ApiResponse<boolean>> {
    return request.put(`/role/${id}`, data)
  },

  // 删除角色
  delete(id: number): Promise<ApiResponse<boolean>> {
    return request.delete(`/role/${id}`)
  },

  // 更新角色状态
  updateStatus(id: number, status: number): Promise<ApiResponse<boolean>> {
    return request.put(`/role/${id}/status`, null, { params: { status } })
  },

  // 分配权限
  assignPermissions(id: number, permissionIds: number[]): Promise<ApiResponse<boolean>> {
    return request.post(`/role/${id}/permissions`, permissionIds)
  },

  // 分配菜单
  assignMenus(id: number, menuIds: number[]): Promise<ApiResponse<boolean>> {
    return request.post(`/role/${id}/menus`, menuIds)
  },

  // 获取角色权限
  getPermissions(id: number): Promise<ApiResponse<number[]>> {
    return request.get(`/role/${id}/permissions`)
  },

  // 获取角色菜单
  getMenus(id: number): Promise<ApiResponse<number[]>> {
    return request.get(`/role/${id}/menus`)
  },

  // 获取所有角色列表（scope 可选，后端会根据用户上下文自动过滤）
  listAll(scope?: string): Promise<ApiResponse<RoleInfo[]>> {
    const params: Record<string, any> = {}
    if (scope) params.scope = scope
    return request.get('/role/list', params)
  },

  // 从源角色复制权限和菜单
  copyFromRole(id: number, sourceRoleId: number): Promise<ApiResponse<boolean>> {
    return request.post(`/role/${id}/copy-from/${sourceRoleId}`)
  }
}

export default roleApi