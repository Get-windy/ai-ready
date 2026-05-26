/**
 * 权限指令
 * 用于按钮级别的权限控制
 * 使用方法：v-permission="['system:user:add']" 或 v-permission="'system:user:add'"
 */

import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 检查权限
 * @param permissions 需要的权限列表或单个权限
 * @param userPermissions 用户拥有的权限列表
 * @returns 是否有权限
 */
function checkPermission(
  permissions: string | string[],
  userPermissions: string[]
): boolean {
  if (!userPermissions || userPermissions.length === 0) {
    return false
  }

  // 超级管理员拥有所有权限
  if (userPermissions.includes('*')) {
    return true
  }

  if (typeof permissions === 'string') {
    return userPermissions.includes(permissions)
  }

  if (Array.isArray(permissions)) {
    // 只要有一个权限满足即可
    return permissions.some(permission => userPermissions.includes(permission))
  }

  return false
}

/**
 * 权限指令实现
 */
export const permission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding
    const userStore = useUserStore()
    const userPermissions = userStore.permissions

    if (value) {
      const hasPermission = checkPermission(value, userPermissions)
      if (!hasPermission) {
        // 移除元素
        el.parentNode?.removeChild(el)
        // 或使用样式隐藏（推荐这种方式，避免频繁的DOM操作）
        // el.style.display = 'none'
      }
    } else {
      throw new Error('需要指定权限值，例如：v-permission="\'system:user:add\'"')
    }
  },

  updated(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding
    const userStore = useUserStore()
    const userPermissions = userStore.permissions

    if (value) {
      const hasPermission = checkPermission(value, userPermissions)
      if (!hasPermission) {
        el.style.display = 'none'
      } else {
        el.style.display = ''
      }
    }
  }
}

/**
 * 角色指令
 * 用于角色级别的权限控制
 * 使用方法：v-role="['admin']" 或 v-role="'admin'"
 */
export const role: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding
    const userStore = useUserStore()
    const userRoles = userStore.roles

    if (value) {
      const hasRole = checkRole(value, userRoles)
      if (!hasRole) {
        el.parentNode?.removeChild(el)
      }
    } else {
      throw new Error('需要指定角色值，例如：v-role="\'admin\'"')
    }
  },

  updated(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding
    const userStore = useUserStore()
    const userRoles = userStore.roles

    if (value) {
      const hasRole = checkRole(value, userRoles)
      if (!hasRole) {
        el.style.display = 'none'
      } else {
        el.style.display = ''
      }
    }
  }
}

/**
 * 检查角色
 * @param roles 需要的角色列表或单个角色
 * @param userRoles 用户拥有的角色列表
 * @returns 是否有角色
 */
function checkRole(roles: string | string[], userRoles: string[]): boolean {
  if (!userRoles || userRoles.length === 0) {
    return false
  }

  // 超级管理员角色
  if (userRoles.includes('admin') || userRoles.includes('super_admin')) {
    return true
  }

  if (typeof roles === 'string') {
    return userRoles.includes(roles)
  }

  if (Array.isArray(roles)) {
    return roles.some(role => userRoles.includes(role))
  }

  return false
}

/**
 * 默认导出权限指令
 */
export default {
  permission,
  role
}