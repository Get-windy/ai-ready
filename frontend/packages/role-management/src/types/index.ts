/**
 * 用户角色权限管理类型定义
 */

// 权限类型
export interface Permission {
  id: string
  code: string
  name: string
  description?: string
  type: 'menu' | 'button' | 'api' | 'data'
  parentId?: string
  path?: string
  icon?: string
  order: number
  enabled: boolean
  createdAt: string
  updatedAt: string
}

// 角色类型
export interface Role {
  id: string
  code: string
  name: string
  description?: string
  type: 'system' | 'custom' | 'department' | 'project'
  level: number // 角色级别，数字越小权限越高
  enabled: boolean
  isDefault: boolean
  permissionIds: string[]
  createdAt: string
  updatedAt: string
}

// 用户角色关联
export interface UserRole {
  id: string
  userId: string
  roleId: string
  assignedBy: string
  assignedAt: string
  expiresAt?: string
  enabled: boolean
}

// 权限树节点
export interface PermissionTreeNode extends Permission {
  children?: PermissionTreeNode[]
  isLeaf?: boolean
  disabled?: boolean
  selected?: boolean
  indeterminate?: boolean
}

// 角色查询参数
export interface RoleQueryParams {
  page?: number
  pageSize?: number
  keyword?: string
  type?: Role['type']
  enabled?: boolean
  orderBy?: string
  orderDirection?: 'asc' | 'desc'
}

// 权限查询参数
export interface PermissionQueryParams {
  type?: Permission['type']
  enabled?: boolean
  parentId?: string
  keyword?: string
}

// 用户角色分配参数
export interface UserRoleAssignParams {
  userId: string
  roleIds: string[]
  assignedBy: string
  expiresAt?: string
}

// 批量权限分配参数
export interface BatchPermissionAssignParams {
  roleIds: string[]
  permissionIds: string[]
  operation: 'add' | 'remove' | 'replace'
}

// 角色权限统计
export interface RolePermissionStats {
  roleId: string
  roleName: string
  totalPermissions: number
  menuPermissions: number
  buttonPermissions: number
  apiPermissions: number
  dataPermissions: number
}

// 用户权限预览
export interface UserPermissionPreview {
  userId: string
  userName: string
  directRoles: Role[]
  inheritedRoles: Role[]
  allPermissions: Permission[]
  permissionSources: Array<{
    permission: Permission
    sourceType: 'direct' | 'inherited'
    sourceRole: Role
  }>
}

// 权限冲突检测结果
export interface PermissionConflict {
  type: 'explicit' | 'implicit' | 'inheritance'
  conflictPermissions: Permission[]
  conflictingRoles: Role[]
  description: string
  severity: 'low' | 'medium' | 'high'
}

// 授权历史记录
export interface AuthorizationHistory {
  id: string
  operation: 'assign' | 'revoke' | 'update'
  targetType: 'user' | 'role' | 'permission'
  targetId: string
  targetName: string
  operatorId: string
  operatorName: string
  details: Record<string, any>
  ip?: string
  userAgent?: string
  createdAt: string
}