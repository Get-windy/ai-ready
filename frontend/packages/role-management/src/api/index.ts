/**
 * 用户角色权限管理API接口
 */
import type {
  Role,
  Permission,
  UserRole,
  UserInfo,
  RoleQueryParams,
  PermissionQueryParams,
  UserRoleAssignParams,
  BatchPermissionAssignParams,
  RolePermissionStats,
  UserPermissionPreview,
  PermissionConflict,
  AuthorizationHistory
} from '../types'

export class RoleManagementApi {
  private baseUrl = '/api/role-management'

  // 角色管理API
  async getRoles(params?: RoleQueryParams): Promise<{ data: Role[]; total: number }> {
    const query = new URLSearchParams()
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          query.append(key, String(value))
        }
      })
    }

    const response = await fetch(`${this.baseUrl}/roles?${query}`)
    if (!response.ok) throw new Error('获取角色列表失败')
    return response.json()
  }

  async getRole(id: string): Promise<Role> {
    const response = await fetch(`${this.baseUrl}/roles/${id}`)
    if (!response.ok) throw new Error('获取角色详情失败')
    return response.json()
  }

  async createRole(role: Omit<Role, 'id' | 'createdAt' | 'updatedAt'>): Promise<Role> {
    const response = await fetch(`${this.baseUrl}/roles`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(role)
    })
    if (!response.ok) throw new Error('创建角色失败')
    return response.json()
  }

  async updateRole(id: string, role: Partial<Role>): Promise<Role> {
    const response = await fetch(`${this.baseUrl}/roles/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(role)
    })
    if (!response.ok) throw new Error('更新角色失败')
    return response.json()
  }

  async deleteRole(id: string): Promise<void> {
    const response = await fetch(`${this.baseUrl}/roles/${id}`, {
      method: 'DELETE'
    })
    if (!response.ok) throw new Error('删除角色失败')
  }

  async toggleRoleStatus(id: string, enabled: boolean): Promise<void> {
    const response = await fetch(`${this.baseUrl}/roles/${id}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ enabled })
    })
    if (!response.ok) throw new Error('更新角色状态失败')
  }

  // 权限管理API
  async getPermissions(params?: PermissionQueryParams): Promise<Permission[]> {
    const query = new URLSearchParams()
    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        if (value !== undefined && value !== null) {
          query.append(key, String(value))
        }
      })
    }

    const response = await fetch(`${this.baseUrl}/permissions?${query}`)
    if (!response.ok) throw new Error('获取权限列表失败')
    return response.json()
  }

  async getPermissionTree(): Promise<any[]> {
    const response = await fetch(`${this.baseUrl}/permissions/tree`)
    if (!response.ok) throw new Error('获取权限树失败')
    return response.json()
  }

  // 用户管理API
  async getUsers(): Promise<UserInfo[]> {
    const response = await fetch(`${this.baseUrl}/users`)
    if (!response.ok) throw new Error('获取用户列表失败')
    return response.json()
  }

  // 用户角色分配API
  async getUserRoles(userId?: string): Promise<UserRole[]> {
    const url = userId ? `${this.baseUrl}/users/${userId}/roles` : `${this.baseUrl}/user-roles`
    const response = await fetch(url)
    if (!response.ok) throw new Error('获取用户角色失败')
    return response.json()
  }

  async assignRoleToUser(userId: string, roleId: string): Promise<void> {
    const response = await fetch(`${this.baseUrl}/users/${userId}/roles/${roleId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    })
    if (!response.ok) throw new Error('分配角色失败')
  }

  async removeRoleFromUser(userId: string, roleId: string): Promise<void> {
    const response = await fetch(`${this.baseUrl}/users/${userId}/roles/${roleId}`, {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' }
    })
    if (!response.ok) throw new Error('移除角色失败')
  }

  async batchAssignRoles(userIds: string[], roleId: string): Promise<void> {
    const response = await fetch(`${this.baseUrl}/users/roles/batch-assign`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userIds, roleId })
    })
    if (!response.ok) throw new Error('批量分配角色失败')
  }

  async assignUserRoles(params: UserRoleAssignParams): Promise<void> {
    const response = await fetch(`${this.baseUrl}/users/roles/assign`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params)
    })
    if (!response.ok) throw new Error('分配用户角色失败')
  }

  async batchAssignUserRoles(userIds: string[], roleIds: string[]): Promise<void> {
    const response = await fetch(`${this.baseUrl}/users/roles/batch-assign`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userIds, roleIds })
    })
    if (!response.ok) throw new Error('批量分配用户角色失败')
  }

  // 角色权限分配API
  async getRolePermissions(roleId: string): Promise<string[]> {
    const response = await fetch(`${this.baseUrl}/roles/${roleId}/permissions`)
    if (!response.ok) throw new Error('获取角色权限失败')
    return response.json()
  }

  async assignRolePermissions(roleId: string, permissionIds: string[]): Promise<void> {
    const response = await fetch(`${this.baseUrl}/roles/${roleId}/permissions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ permissionIds })
    })
    if (!response.ok) throw new Error('分配角色权限失败')
  }

  async batchAssignPermissions(params: BatchPermissionAssignParams): Promise<void> {
    const response = await fetch(`${this.baseUrl}/permissions/batch-assign`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params)
    })
    if (!response.ok) throw new Error('批量分配权限失败')
  }

  // 统计分析API
  async getRolePermissionStats(roleId?: string): Promise<RolePermissionStats[]> {
    const url = roleId 
      ? `${this.baseUrl}/stats/role-permissions/${roleId}`
      : `${this.baseUrl}/stats/role-permissions`
    
    const response = await fetch(url)
    if (!response.ok) throw new Error('获取角色权限统计失败')
    return response.json()
  }

  async getUserPermissions(userId: string): Promise<string[]> {
    const response = await fetch(`${this.baseUrl}/users/${userId}/permissions`)
    if (!response.ok) throw new Error('获取用户权限失败')
    return response.json()
  }

  async getUserPermissionPreview(userId: string): Promise<UserPermissionPreview> {
    const response = await fetch(`${this.baseUrl}/users/${userId}/permissions/preview`)
    if (!response.ok) throw new Error('获取用户权限预览失败')
    return response.json()
  }

  async getPermissionConflicts(): Promise<PermissionConflict[]> {
    const response = await fetch(`${this.baseUrl}/permissions/conflicts`)
    if (!response.ok) throw new Error('获取权限冲突失败')
    return response.json()
  }

  async checkPermissionConflicts(userId: string): Promise<PermissionConflict[]> {
    const response = await fetch(`${this.baseUrl}/users/${userId}/permissions/conflicts`)
    if (!response.ok) throw new Error('检测权限冲突失败')
    return response.json()
  }

  async resolvePermissionConflict(conflictId: string, resolution: string): Promise<void> {
    const response = await fetch(`${this.baseUrl}/permissions/conflicts/${conflictId}/resolve`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ resolution })
    })
    if (!response.ok) throw new Error('解决权限冲突失败')
  }

  // 授权历史API
  async getAuthorizationHistory(userId?: string): Promise<AuthorizationHistory[]> {
    const url = userId ? `${this.baseUrl}/users/${userId}/authorization-history` : `${this.baseUrl}/authorization-history`
    const response = await fetch(url)
    if (!response.ok) throw new Error('获取授权历史失败')
    return response.json()
  }

  // 导出API
  async exportRoles(format: 'csv' | 'excel' | 'json' = 'json'): Promise<Blob> {
    const response = await fetch(`${this.baseUrl}/export/roles?format=${format}`)
    if (!response.ok) throw new Error('导出角色数据失败')
    return response.blob()
  }

  async exportPermissions(format: 'csv' | 'excel' | 'json' = 'json'): Promise<Blob> {
    const response = await fetch(`${this.baseUrl}/export/permissions?format=${format}`)
    if (!response.ok) throw new Error('导出权限数据失败')
    return response.blob()
  }
}

// 创建API实例
export const roleManagementApi = new RoleManagementApi()