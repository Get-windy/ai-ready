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
  bizFlowTag?: string
  displayGroup?: number
  linkIcon?: string
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
  bizFlowTag?: string
  displayGroup?: number
  linkIcon?: string
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

// 菜单API - 对齐后端 RESTful Controller
export const menuApi = {
  // 获取菜单树（管理用）
  getTree(params?: MenuQuery): Promise<ApiResponse<MenuInfo[]>> {
    const userStore = useUserStore()
    return request.get('/menu/tree', { ...params, tenantId: userStore.tenantId })
  },

  // 获取菜单列表（扁平）
  getList(tenantId: number): Promise<ApiResponse<MenuInfo[]>> {
    return request.get('/menu/list', { tenantId })
  },

  // 获取菜单详情
  getById(id: number): Promise<ApiResponse<MenuInfo>> {
    return request.get(`/menu/${id}`)
  },

  // 创建菜单
  create(data: MenuSaveRequest): Promise<ApiResponse<MenuInfo>> {
    return request.post('/menu', data)
  },

  // 更新菜单
  update(id: number, data: MenuSaveRequest): Promise<ApiResponse<MenuInfo>> {
    return request.put(`/menu/${id}`, data)
  },

  // 删除菜单
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/menu/${id}`)
  },

  // 更新菜单状态
  updateStatus(id: number, status: number): Promise<ApiResponse<void>> {
    return request.put(`/menu/${id}/status`, null, { params: { status } })
  },

  // 更新菜单排序
  updateSort(id: number, sort: number): Promise<ApiResponse<void>> {
    return request.put(`/menu/${id}/sort`, null, { params: { sort } })
  },

  // 验证菜单编码是否存在
  validateMenuCode(menuCode: string, tenantId: number, excludeId?: number): Promise<ApiResponse<boolean>> {
    const params: Record<string, any> = { menuCode, tenantId }
    if (excludeId !== undefined) params.excludeId = excludeId
    return request.get('/menu/check-code', params)
  },

  // 获取用户菜单树
  getUserMenus(userId?: number): Promise<ApiResponse<MenuInfo[]>> {
    const params: Record<string, any> = {}
    if (userId) params.userId = userId
    return request.get('/menu/user/tree', params)
  },

  // 按客户端类型获取用户菜单（动态路由用）
  getUserClientMenus(clientType: string, userId?: number, tenantId?: number): Promise<ApiResponse<MenuInfo[]>> {
    const params: Record<string, any> = {}
    if (userId) params.userId = userId
    if (tenantId) params.tenantId = tenantId
    return request.get(`/menu/user/client/${clientType}`, params)
  },

  // 获取子菜单列表
  getChildren(parentId: number, tenantId: number): Promise<ApiResponse<MenuInfo[]>> {
    return request.get(`/menu/children/${parentId}`, { tenantId })
  }
}

export default menuApi