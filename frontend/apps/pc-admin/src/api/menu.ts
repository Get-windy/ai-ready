import request, { type ApiResponse } from '@/utils/request'
import { useUserStore } from '@/stores/user'

// 菜单信息
export interface MenuInfo {
  id: number
  parentId: number
  menuName: string
  menuCode: string
  menuType: number // 0: 目录, 1: 菜单, 2: 按钮
  icon?: string
  path?: string
  component?: string
  permissions?: string
  sortOrder: number
  status: number
  visible: number
  keepAlive?: number
  external?: number
  remark?: string
  createTime?: string
  updateTime?: string
  children?: MenuInfo[]
}

// 菜单查询参数
export interface MenuQuery {
  menuName?: string
  menuType?: number
  status?: number
}

// 菜单保存请求
export interface MenuSaveRequest {
  parentId: number
  menuName: string
  menuCode: string
  menuType: number
  icon?: string
  path?: string
  component?: string
  permissions?: string
  sortOrder: number
  status: number
  visible: number
  keepAlive?: number
  external?: number
  remark?: string
}

// 菜单更新请求
export interface MenuUpdateRequest extends MenuSaveRequest {
  id: number
}

// 角色菜单分配请求
export interface RoleMenuAssignRequest {
  roleId: number
  menuIds: number[]
}

// 菜单API
export const menuApi = {
  // 获取菜单树
  getTree(params?: MenuQuery): Promise<ApiResponse<MenuInfo[]>> {
    const userStore = useUserStore()
    return request.get('/menu/tree', { ...params, tenantId: userStore.tenantId })
  },

  // 获取所有菜单
  getAll(): Promise<ApiResponse<MenuInfo[]>> {
    return request.get('/menu/all')
  },

  // 获取菜单详情
  getById(id: number): Promise<ApiResponse<MenuInfo>> {
    return request.get(`/menu/detail/${id}`)
  },

  // 创建菜单
  create(data: MenuSaveRequest, operatorId: number): Promise<ApiResponse<MenuInfo>> {
    return request.post('/menu/create', data, { params: { operatorId } })
  },

  // 更新菜单
  update(data: MenuUpdateRequest, operatorId: number): Promise<ApiResponse<MenuInfo>> {
    return request.put('/menu/update', data, { params: { operatorId } })
  },

  // 删除菜单
  delete(id: number, operatorId: number): Promise<ApiResponse<void>> {
    return request.delete(`/menu/delete/${id}`, { params: { operatorId } })
  },

  // 批量删除菜单
  batchDelete(ids: number[], operatorId: number): Promise<ApiResponse<void>> {
    return request.post('/menu/batch-delete', ids, { params: { operatorId } })
  },

  // 更新菜单状态
  updateStatus(id: number, status: number, operatorId: number): Promise<ApiResponse<void>> {
    return request.post(`/menu/update-status/${id}`, null, { params: { status, operatorId } })
  },

  // 移动菜单
  move(menuId: number, newParentId: number, operatorId: number): Promise<ApiResponse<MenuInfo>> {
    return request.post(`/menu/move/${menuId}`, null, { params: { newParentId, operatorId } })
  },

  // 更新菜单排序
  updateSortOrder(menuIds: number[], operatorId: number): Promise<ApiResponse<void>> {
    return request.post('/menu/update-sort', menuIds, { params: { operatorId } })
  },

  // 验证菜单编码
  validateMenuCode(menuCode: string, excludeId?: number): Promise<ApiResponse<boolean>> {
    return request.get('/menu/validate-code', { menuCode, excludeId })
  },

  // 获取用户菜单
  getUserMenus(userId: number): Promise<ApiResponse<MenuInfo[]>> {
    return request.get(`/menu/user/${userId}`)
  },

  // 获取用户客户端菜单
  getUserClientMenus(userId: number, clientType: string, tenantId: number): Promise<ApiResponse<MenuInfo[]>> {
    return request.get(`/menu/user/client/${clientType}`, { userId, tenantId })
  },

  // 获取角色菜单
  getRoleMenus(roleId: number): Promise<ApiResponse<MenuInfo[]>> {
    return request.get(`/menu/role/${roleId}`)
  },

  // 分配角色菜单
  assignRoleMenus(roleId: number, menuIds: number[], operatorId: number): Promise<ApiResponse<void>> {
    return request.post(`/menu/assign-role-menus/${roleId}`, menuIds, { params: { operatorId } })
  }
}

export default menuApi