/**
 * 权限组合式函数
 * 提供在组件中使用的权限检查功能
 */

import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { hasPermission, hasAnyPermission, hasAllPermissions, hasRole, hasAnyRole, isSuperAdmin, isAdmin } from '@/utils/permission'

/**
 * 使用权限检查
 * @returns 权限相关的方法和状态
 */
export function usePermission() {
  const userStore = useUserStore()

  // 用户权限列表
  const permissions = computed(() => userStore.permissions)
  
  // 用户角色列表
  const roles = computed(() => userStore.roles)
  
  // 是否已登录
  const isLoggedIn = computed(() => userStore.isLoggedIn)
  
  // 是否为超级管理员
  const isSuperAdminUser = computed(() => isSuperAdmin())
  
  // 是否为管理员
  const isAdminUser = computed(() => isAdmin())

  /**
   * 检查是否有指定权限
   * @param permission 权限标识
   * @returns 是否有权限
   */
  const checkPermission = (permission: string): boolean => {
    return hasPermission(permission)
  }

  /**
   * 检查是否有任意一个权限
   * @param perms 权限数组
   * @returns 是否有权限
   */
  const checkAnyPermission = (perms: string[]): boolean => {
    return hasAnyPermission(perms)
  }

  /**
   * 检查是否有所有权限
   * @param perms 权限数组
   * @returns 是否有所有权限
   */
  const checkAllPermissions = (perms: string[]): boolean => {
    return hasAllPermissions(perms)
  }

  /**
   * 检查是否有指定角色
   * @param role 角色标识
   * @returns 是否有角色
   */
  const checkRole = (role: string): boolean => {
    return hasRole(role)
  }

  /**
   * 检查是否有任意一个角色
   * @param roleList 角色数组
   * @returns 是否有角色
   */
  const checkAnyRole = (roleList: string[]): boolean => {
    return hasAnyRole(roleList)
  }

  /**
   * 根据权限过滤列表
   * @param list 列表数据
   * @param permissionKey 权限字段名
   * @returns 过滤后的列表
   */
  const filterByPermission = <T extends Record<string, any>>(
    list: T[],
    permissionKey: string = 'permission'
  ): T[] => {
    if (isSuperAdminUser.value) return list
    
    return list.filter(item => {
      const requiredPermission = item[permissionKey]
      if (!requiredPermission) return true
      return checkPermission(requiredPermission)
    })
  }

  /**
   * 执行需要权限的操作
   * @param permission 权限标识
   * @param action 回调函数
   * @param noPermissionAction 无权限时的回调函数
   */
  const withPermission = (
    permission: string,
    action: () => void,
    noPermissionAction?: () => void
  ): void => {
    if (checkPermission(permission)) {
      action()
    } else if (noPermissionAction) {
      noPermissionAction()
    }
  }

  /**
   * 执行需要角色的操作
   * @param role 角色标识
   * @param action 回调函数
   * @param noRoleAction 无角色时的回调函数
   */
  const withRole = (
    role: string,
    action: () => void,
    noRoleAction?: () => void
  ): void => {
    if (checkRole(role)) {
      action()
    } else if (noRoleAction) {
      noRoleAction()
    }
  }

  return {
    // 状态
    permissions,
    roles,
    isLoggedIn,
    isSuperAdminUser,
    isAdminUser,
    
    // 权限检查方法
    checkPermission,
    checkAnyPermission,
    checkAllPermissions,
    checkRole,
    checkAnyRole,
    
    // 工具方法
    filterByPermission,
    withPermission,
    withRole
  }
}

/**
 * 使用菜单权限
 * @returns 菜单权限相关方法
 */
export function useMenuPermission() {
  const { checkPermission, checkAnyPermission } = usePermission()

  /**
   * 检查菜单是否可访问
   * @param menuCode 菜单编码
   * @returns 是否可访问
   */
  const canAccessMenu = (menuCode: string): boolean => {
    return checkPermission(`menu:${menuCode}`) || checkPermission('*')
  }

  /**
   * 检查任意菜单是否可访问
   * @param menuCodes 菜单编码数组
   * @returns 是否有任一菜单可访问
   */
  const canAccessAnyMenu = (menuCodes: string[]): boolean => {
    return checkAnyPermission(menuCodes.map(code => `menu:${code}`))
  }

  /**
   * 过滤菜单列表
   * @param menus 菜单列表
   * @returns 过滤后的菜单列表
   */
  const filterMenus = <T extends { permissions?: string[] }>(menus: T[]): T[] => {
    const { permissions } = usePermission()
    
    if (permissions.value.includes('*')) return menus
    
    return menus.filter(menu => {
      if (!menu.permissions || menu.permissions.length === 0) return true
      return menu.permissions.some(perm => checkPermission(perm))
    })
  }

  return {
    canAccessMenu,
    canAccessAnyMenu,
    filterMenus
  }
}

/**
 * 使用按钮权限
 * @returns 按钮权限相关方法
 */
export function useButtonPermission() {
  const { checkPermission, checkAnyPermission } = usePermission()

  /**
   * 检查按钮是否可操作
   * @param buttonCode 按钮权限编码
   * @returns 是否可操作
   */
  const canOperate = (buttonCode: string): boolean => {
    return checkPermission(buttonCode) || checkPermission('*')
  }

  /**
   * 检查任意按钮是否可操作
   * @param buttonCodes 按钮权限编码数组
   * @returns 是否有任一按钮可操作
   */
  const canOperateAny = (buttonCodes: string[]): boolean => {
    return checkAnyPermission(buttonCodes)
  }

  /**
   * 获取按钮禁用状态
   * @param buttonCode 按钮权限编码
   * @returns 是否禁用
   */
  const getButtonDisabled = (buttonCode: string): boolean => {
    return !canOperate(buttonCode)
  }

  /**
   * 获取多个按钮的禁用状态
   * @param buttonCodes 按钮权限编码数组
   * @returns 禁用状态对象
   */
  const getButtonsDisabled = (buttonCodes: string[]): Record<string, boolean> => {
    return buttonCodes.reduce((acc, code) => {
      acc[code] = getButtonDisabled(code)
      return acc
    }, {} as Record<string, boolean>)
  }

  return {
    canOperate,
    canOperateAny,
    getButtonDisabled,
    getButtonsDisabled
  }
}

/**
 * 使用数据权限
 * @returns 数据权限相关方法
 */
export function useDataPermission() {
  const { checkPermission, checkAnyPermission, isSuperAdminUser } = usePermission()

  /**
   * 检查数据范围权限
   * @param dataScope 数据范围
   * @returns 是否有权限
   */
  const checkDataScope = (dataScope: string): boolean => {
    // 超级管理员拥有所有数据权限
    if (isSuperAdminUser.value) return true
    
    return checkPermission(`data:${dataScope}`)
  }

  /**
   * 获取数据范围
   * @returns 数据范围（全部、本部门、本部门及以下、仅本人）
   */
  const getDataScope = (): 'all' | 'dept' | 'deptAndChildren' | 'self' => {
    if (checkPermission('data:all')) return 'all'
    if (checkPermission('data:deptAndChildren')) return 'deptAndChildren'
    if (checkPermission('data:dept')) return 'dept'
    return 'self'
  }

  /**
   * 根据数据权限过滤数据
   * @param data 数据列表
   * @param currentUserId 当前用户ID
   * @param currentDeptId 当前部门ID
   * @returns 过滤后的数据
   */
  const filterByDataScope = <T extends { userId?: number; deptId?: number }>(
    data: T[],
    currentUserId: number,
    currentDeptId: number
  ): T[] => {
    const dataScope = getDataScope()
    
    if (dataScope === 'all') return data
    if (dataScope === 'self') {
      return data.filter(item => item.userId === currentUserId)
    }
    if (dataScope === 'dept') {
      return data.filter(item => item.deptId === currentDeptId)
    }
    if (dataScope === 'deptAndChildren') {
      // 这里需要获取部门及其子部门ID列表
      return data.filter(item => item.deptId === currentDeptId)
    }
    
    return data
  }

  return {
    checkDataScope,
    getDataScope,
    filterByDataScope
  }
}

export default {
  usePermission,
  useMenuPermission,
  useButtonPermission,
  useDataPermission
}