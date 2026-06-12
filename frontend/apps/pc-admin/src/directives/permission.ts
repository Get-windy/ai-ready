/**
 * 权限指令
 * 用于按钮级别的权限控制
 * 使用方法：v-permission="'system:user:add'"
 *          v-permission.disabled="'system:user:add'"  (显示为禁用态而非隐藏)
 */

import type { Directive, DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 检查权限
 */
function checkPermission(
  permissions: string | string[],
  userPermissions: string[]
): boolean {
  if (!userPermissions || userPermissions.length === 0) {
    return false
  }

  if (userPermissions.includes('*')) {
    return true
  }

  if (typeof permissions === 'string') {
    return userPermissions.includes(permissions)
  }

  if (Array.isArray(permissions)) {
    return permissions.some(permission => userPermissions.includes(permission))
  }

  return false
}

function applyNoPermission(el: HTMLElement, showDisabled: boolean) {
  if (showDisabled) {
    // 禁用态示意：置灰+不可交互+title提示
    el.style.opacity = '0.35'
    el.style.pointerEvents = 'none'
    el.style.cursor = 'not-allowed'
    el.classList.add('permission-denied')
    if (!el.hasAttribute('title')) {
      el.setAttribute('title', '暂无操作权限')
    }
  } else {
    // 隐藏态：保留DOM但不可见（兼容KeepAlive）
    el.style.display = 'none'
  }
}

function removeNoPermission(el: HTMLElement, showDisabled: boolean) {
  if (showDisabled) {
    el.style.opacity = ''
    el.style.pointerEvents = ''
    el.style.cursor = ''
    el.classList.remove('permission-denied')
    if (el.getAttribute('title') === '暂无操作权限') {
      el.removeAttribute('title')
    }
  } else {
    el.style.display = ''
  }
}

export const permission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value, modifiers } = binding
    const userStore = useUserStore()

    // 如果 permissions 还未加载，等待并重新检查
    if (!userStore.permissions || userStore.permissions.length === 0) {
      // 暂时隐藏，等待权限加载
      applyNoPermission(el, !!modifiers.disabled)

      // 监听 store 变化
      const unsubscribe = userStore.$subscribe((mutation, state) => {
        if (state.permissions && state.permissions.length > 0) {
          const hasPermission = checkPermission(value, state.permissions)
          if (hasPermission) {
            removeNoPermission(el, !!modifiers.disabled)
          } else {
            applyNoPermission(el, !!modifiers.disabled)
          }
          unsubscribe() // 只检查一次
        }
      })

      return
    }

    const userPermissions = userStore.permissions
    if (value) {
      const hasPermission = checkPermission(value, userPermissions)
      if (!hasPermission) {
        applyNoPermission(el, !!modifiers.disabled)
      }
    } else {
      throw new Error('需要指定权限值，例如：v-permission="\'system:user:add\'"')
    }
  },

  updated(el: HTMLElement, binding: DirectiveBinding) {
    const { value, modifiers } = binding
    const userStore = useUserStore()
    const userPermissions = userStore.permissions

    if (value && userPermissions && userPermissions.length > 0) {
      const hasPermission = checkPermission(value, userPermissions)
      if (!hasPermission) {
        applyNoPermission(el, !!modifiers.disabled)
      } else {
        removeNoPermission(el, !!modifiers.disabled)
      }
    }
  }
}

export const role: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding
    const userStore = useUserStore()
    const userRoles = userStore.roles

    if (value) {
      const hasRole = checkRole(value, userRoles)
      if (!hasRole) {
        el.style.display = 'none'
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

function checkRole(roles: string | string[], userRoles: string[]): boolean {
  if (!userRoles || userRoles.length === 0) {
    return false
  }

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

export default {
  permission,
  role
}
