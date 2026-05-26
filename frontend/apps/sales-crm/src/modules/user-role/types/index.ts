/**
 * 用户角色权限管理模块 - 核心类型定义
 */

// 权限类型枚举
export enum PermissionType {
  // 系统级权限
  SYSTEM_ADMIN = 'system_admin',
  SYSTEM_CONFIG = 'system_config',
  SYSTEM_MONITOR = 'system_monitor',
  
  // 模块级权限
  MODULE_ACCESS = 'module_access',
  MODULE_MANAGE = 'module_manage',
  
  // 数据级权限
  DATA_VIEW = 'data_view',
  DATA_CREATE = 'data_create',
  DATA_EDIT = 'data_edit',
  DATA_DELETE = 'data_delete',
  DATA_EXPORT = 'data_export',
  DATA_IMPORT = 'data_import',
  
  // 功能级权限
  FUNCTION_EXECUTE = 'function_execute',
  FUNCTION_APPROVE = 'function_approve',
  FUNCTION_AUDIT = 'function_audit',
  
  // 用户级权限
  USER_MANAGE = 'user_manage',
  ROLE_MANAGE = 'role_manage',
  PERMISSION_MANAGE = 'permission_manage'
}

// 权限作用域枚举
export enum PermissionScope {
  SYSTEM = 'system',       // 系统级权限
  MODULE = 'module',       // 模块级权限
  MENU = 'menu',           // 菜单级权限
  FUNCTION = 'function',   // 功能级权限
  DATA = 'data',           // 数据级权限
  API = 'api'              // API级权限
}

// 权限实体接口
export interface Permission {
  id: string;
  code: string;
  name: string;
  description: string;
  type: PermissionType;
  scope: PermissionScope;
  module?: string;         // 所属模块
  menuCode?: string;       // 关联菜单编码
  apiPath?: string;        // 关联API路径
  parentId?: string;       // 父权限ID
  sortOrder: number;
  enabled: boolean;
  createdAt: Date;
  updatedAt: Date;
}

// 角色类型枚举
export enum RoleType {
  SYSTEM = 'system',       // 系统角色
  BUSINESS = 'business',   // 业务角色
  CUSTOM = 'custom',       // 自定义角色
  TEMPORARY = 'temporary'  // 临时角色
}

// 角色级别枚举
export enum RoleLevel {
  ADMIN = 'admin',         // 管理员级
  MANAGER = 'manager',     // 经理级
  LEADER = 'leader',       // 组长级
  STAFF = 'staff',         // 员工级
  VIEWER = 'viewer'        // 查看者级
}

// 角色实体接口
export interface Role {
  id: string;
  code: string;
  name: string;
  description: string;
  type: RoleType;
  level: RoleLevel;
  permissions: string[];   // 权限ID列表
  userCount: number;       // 关联用户数
  enabled: boolean;
  isDefault: boolean;      // 是否默认角色
  createdAt: Date;
  updatedAt: Date;
}

// 用户角色关联接口
export interface UserRole {
  id: string;
  userId: string;
  roleId: string;
  roleCode: string;
  roleName: string;
  startTime?: Date;        // 生效时间
  endTime?: Date;          // 失效时间
  assignedBy: string;      // 分配人
  assignedAt: Date;        // 分配时间
  isActive: boolean;       // 是否激活
}

// 用户权限摘要接口（用于权限检查）
export interface UserPermissionSummary {
  userId: string;
  username: string;
  roles: string[];         // 角色代码列表
  permissions: string[];   // 权限代码列表
  isAdmin: boolean;        // 是否为管理员
  effectivePermissions: {   // 有效权限详情
    [module: string]: string[];
  };
}

// 角色权限树节点
export interface PermissionTreeNode {
  id: string;
  code: string;
  name: string;
  description: string;
  type: PermissionType;
  scope: PermissionScope;
  enabled: boolean;
  checked?: boolean;       // 用于权限选择
  indeterminate?: boolean; // 用于权限选择（部分选中）
  children?: PermissionTreeNode[];
}

// 角色树节点
export interface RoleTreeNode {
  id: string;
  code: string;
  name: string;
  type: RoleType;
  level: RoleLevel;
  enabled: boolean;
  userCount: number;
  children?: RoleTreeNode[];
}

// 查询过滤器类型
export interface RoleFilter {
  code?: string;
  name?: string;
  type?: RoleType;
  level?: RoleLevel;
  enabled?: boolean;
  page: number;
  pageSize: number;
}

export interface PermissionFilter {
  code?: string;
  name?: string;
  type?: PermissionType;
  scope?: PermissionScope;
  module?: string;
  enabled?: boolean;
  page: number;
  pageSize: number;
}

export interface UserRoleFilter {
  userId?: string;
  roleId?: string;
  isActive?: boolean;
  page: number;
  pageSize: number;
}

// 创建/更新角色表单数据
export interface RoleFormData {
  code: string;
  name: string;
  description: string;
  type: RoleType;
  level: RoleLevel;
  permissions: string[];
  enabled: boolean;
  isDefault: boolean;
}

// 创建/更新权限表单数据
export interface PermissionFormData {
  code: string;
  name: string;
  description: string;
  type: PermissionType;
  scope: PermissionScope;
  module?: string;
  menuCode?: string;
  apiPath?: string;
  parentId?: string;
  sortOrder: number;
  enabled: boolean;
}

// 分配角色表单数据
export interface AssignRoleFormData {
  userId: string;
  roleIds: string[];
  startTime?: Date;
  endTime?: Date;
}

// 权限检查结果
export interface PermissionCheckResult {
  hasPermission: boolean;
  missingPermissions?: string[];
  effectivePermissions: string[];
}

// 模块权限配置
export interface ModulePermissionConfig {
  module: string;
  moduleName: string;
  permissions: Permission[];
}

// 角色统计信息
export interface RoleStatistics {
  totalRoles: number;
  activeRoles: number;
  systemRoles: number;
  businessRoles: number;
  customRoles: number;
  usersPerRole: { roleId: string; roleName: string; userCount: number }[];
}

// 权限统计信息
export interface PermissionStatistics {
  totalPermissions: number;
  enabledPermissions: number;
  permissionsByType: { type: PermissionType; count: number }[];
  permissionsByScope: { scope: PermissionScope; count: number }[];
  topModules: { module: string; count: number }[];
}

// 用户权限变更记录
export interface UserPermissionChangeLog {
  id: string;
  userId: string;
  username: string;
  operation: 'add' | 'remove' | 'update';
  targetType: 'role' | 'permission';
  targetId: string;
  targetName: string;
  operatorId: string;
  operatorName: string;
  operationTime: Date;
  reason?: string;
  details?: Record<string, any>;
}

// 批量操作结果
export interface BatchOperationResult {
  total: number;
  success: number;
  failed: number;
  errors: { id: string; error: string }[];
}

// 权限树过滤选项
export interface PermissionTreeFilterOptions {
  types?: PermissionType[];
  scopes?: PermissionScope[];
  modules?: string[];
  enabledOnly?: boolean;
}

// 角色导出数据
export interface RoleExportData {
  roles: Role[];
  permissions: Permission[];
  userRoleMappings: UserRole[];
  exportTime: Date;
  exportBy: string;
}

// 权限导入数据
export interface PermissionImportData {
  permissions: PermissionFormData[];
  conflictStrategy: 'skip' | 'replace' | 'merge';
  importBy: string;
}