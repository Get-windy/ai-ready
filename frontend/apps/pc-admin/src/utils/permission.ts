/**
 * 权限工具函数
 * 提供权限检查的工具方法，用于在代码中进行权限控制
 */

import { useUserStore } from '@/stores/user'

/**
 * 检查是否有指定权限
 * @param permission 权限标识
 * @returns 是否有权限
 */
export function hasPermission(permission: string): boolean {
  const userStore = useUserStore()
  return userStore.hasPermission(permission)
}

/**
 * 检查是否有任意一个权限
 * @param permissions 权限数组
 * @returns 是否有权限
 */
export function hasAnyPermission(permissions: string[]): boolean {
  const userStore = useUserStore()
  return userStore.hasAnyPermission(permissions)
}

/**
 * 检查是否有所有权限
 * @param permissions 权限数组
 * @returns 是否有所有权限
 */
export function hasAllPermissions(permissions: string[]): boolean {
  const userStore = useUserStore()
  return userStore.hasAllPermissions(permissions)
}

/**
 * 检查是否有指定角色
 * @param role 角色标识
 * @returns 是否有角色
 */
export function hasRole(role: string): boolean {
  const userStore = useUserStore()
  return userStore.hasRole(role)
}

/**
 * 检查是否有任意一个角色
 * @param roles 角色数组
 * @returns 是否有角色
 */
export function hasAnyRole(roles: string[]): boolean {
  const userStore = useUserStore()
  return userStore.hasAnyRole(roles)
}

/**
 * 检查是否为超级管理员
 * @returns 是否为超级管理员
 */
export function isSuperAdmin(): boolean {
  const userStore = useUserStore()
  return userStore.userType === 0
}

/**
 * 检查是否为管理员
 * @returns 是否为管理员
 */
export function isAdmin(): boolean {
  const userStore = useUserStore()
  return userStore.userType === 0 || userStore.userType === 1
}

/**
 * 获取当前用户的所有权限
 * @returns 权限列表
 */
export function getCurrentPermissions(): string[] {
  const userStore = useUserStore()
  return userStore.permissions
}

/**
 * 获取当前用户的所有角色
 * @returns 角色列表
 */
export function getCurrentRoles(): string[] {
  const userStore = useUserStore()
  return userStore.roles
}

/**
 * 检查菜单是否可访问
 * @param menuCode 菜单编码
 * @returns 是否可访问
 */
export function canAccessMenu(menuCode: string): boolean {
  const userStore = useUserStore()
  return userStore.permissions.includes(`menu:${menuCode}`) || userStore.permissions.includes('*')
}

/**
 * 检查按钮是否可操作
 * @param buttonCode 按钮权限编码
 * @returns 是否可操作
 */
export function canOperate(buttonCode: string): boolean {
  const userStore = useUserStore()
  return userStore.permissions.includes(buttonCode) || userStore.permissions.includes('*')
}

/**
 * 根据权限过滤菜单
 * @param menus 菜单列表
 * @returns 过滤后的菜单列表
 */
export function filterMenusByPermission<T extends { permissions?: string[] }>(
  menus: T[]
): T[] {
  const userStore = useUserStore()
  const userPermissions = userStore.permissions

  if (userPermissions.includes('*')) {
    return menus
  }

  return menus.filter(menu => {
    if (!menu.permissions || menu.permissions.length === 0) {
      return true
    }
    return menu.permissions.some(perm => userPermissions.includes(perm))
  })
}

/**
 * 权限装饰器（用于类方法）
 * @param permission 权限标识
 */
export function RequirePermission(permission: string) {
  return function (
    target: any,
    propertyKey: string,
    descriptor: PropertyDescriptor
  ) {
    const originalMethod = descriptor.value

    descriptor.value = function (...args: any[]) {
      if (!hasPermission(permission)) {
        console.warn(`没有权限执行此操作: ${permission}`)
        return false
      }
      return originalMethod.apply(this, args)
    }

    return descriptor
  }
}

/**
 * 角色装饰器（用于类方法）
 * @param role 角色标识
 */
export function RequireRole(role: string) {
  return function (
    target: any,
    propertyKey: string,
    descriptor: PropertyDescriptor
  ) {
    const originalMethod = descriptor.value

    descriptor.value = function (...args: any[]) {
      if (!hasRole(role)) {
        console.warn(`没有角色权限执行此操作: ${role}`)
        return false
      }
      return originalMethod.apply(this, args)
    }

    return descriptor
  }
}

/**
 * 权限过滤器（用于模板）
 * @param value 权限值
 * @param userPermissions 用户权限列表
 * @returns 是否有权限
 */
export function permissionFilter(
  value: string | string[],
  userPermissions: string[]
): boolean {
  if (!userPermissions || userPermissions.length === 0) {
    return false
  }

  if (userPermissions.includes('*')) {
    return true
  }

  if (typeof value === 'string') {
    return userPermissions.includes(value)
  }

  if (Array.isArray(value)) {
    return value.some(perm => userPermissions.includes(perm))
  }

  return false
}

export default {
  hasPermission,
  hasAnyPermission,
  hasAllPermissions,
  hasRole,
  hasAnyRole,
  isSuperAdmin,
  isAdmin,
  getCurrentPermissions,
  getCurrentRoles,
  canAccessMenu,
  canOperate,
  filterMenusByPermission,
  RequirePermission,
  RequireRole,
  permissionFilter
}