/**
 * 路由权限守卫
 * 提供路由级别的权限控制功能
 */

import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { checkRouteAccess, filterRoutesByPermission, loadDynamicRoutes } from './dynamicRoutes'
import { message } from 'ant-design-vue'

let dynamicRoutesLoaded = false

export function resetDynamicRoutesLoaded() {
  dynamicRoutesLoaded = false
}

export function isDynamicRoutesLoaded() {
  return dynamicRoutesLoaded
}

/**
 * 路由守卫选项
 */
export interface RouterGuardOptions {
  beforeEach?: (to: any, from: any, next: any) => void | Promise<void>
  afterEach?: (to: any, from: any) => void | Promise<void>
  onError?: (error: any) => void | Promise<void>
}

/**
 * 设置路由权限守卫
 */
export function setupRouterGuard(router: Router, options?: RouterGuardOptions) {
  router.beforeEach(async (to, from, next) => {
    try {
      const userStore = useUserStore()

      // 登录/注册页直接放行
      if (to.path === '/login' || to.path === '/register') {
        next()
        return
      }

      // 检查登录状态
      if (!userStore.isLoggedIn) {
        message.warning('请先登录')
        next({ path: '/login', query: { redirect: to.fullPath } })
        return
      }

      // 加载用户信息
      if (!userStore.userInfo) {
        try {
          await userStore.getUserInfo()
        } catch {
          userStore.logout()
          next({ path: '/login', replace: true })
          return
        }
      }

      // 加载动态路由
      if (!dynamicRoutesLoaded) {
        try {
          const dynamicRoutes = await loadDynamicRoutes()
          for (const route of dynamicRoutes) {
            if (route.children && route.children.length > 0) {
              const { children: _, ...parentRoute } = route
              router.addRoute(parentRoute as any)
              for (const child of route.children) {
                router.addRoute(route.name as string, child as any)
              }
            } else {
              router.addRoute(route)
            }
          }
          dynamicRoutesLoaded = true
          next({ path: to.path, replace: true })
          return
        } catch (error: any) {
          if (error?.response?.status === 401 || error?.status === 401) {
            message.warning('登录已过期，请重新登录')
            userStore.logout()
            next({ path: '/login', replace: true })
            return
          }
          dynamicRoutesLoaded = true
          next({ path: '/dashboard', replace: true })
          return
        }
      }

      // 404 检查
      if (to.matched.length === 0) {
        next({ path: '/dashboard', replace: true })
        return
      }

      // 权限检查
      if (!checkRouteAccess(to)) {
        message.error('您没有权限访问此页面')
        next({ path: '/403' })
        return
      }

      // 页面标题
      document.title = to.meta.title ? `${to.meta.title} - AI-Ready` : 'AI-Ready'

      // 自定义前置守卫
      if (options?.beforeEach) {
        await options.beforeEach(to, from, next)
        return
      }

      next()
    } catch (error) {
      console.error('[路由守卫] 错误:', error)
      dynamicRoutesLoaded = false
      if (options?.onError) {
        await options.onError(error)
      }
      next({ path: '/login', replace: true })
    }
  })

  // 后置守卫
  router.afterEach((to, from) => {
    if (options?.afterEach) {
      options.afterEach(to, from)
    }
  })

  // 错误处理
  router.onError((error) => {
    console.error('路由错误:', error)
    if (options?.onError) {
      options.onError(error)
    }
  })
}

export function requiresAuth(route: any): boolean {
  if (route.meta?.requiresAuth === false) return false
  return true
}

export interface RouteCheckResult {
  hasAccess: boolean
  reason?: string
}

export function checkRoutePermission(route: any): RouteCheckResult {
  const userStore = useUserStore()
  if (requiresAuth(route) && !userStore.isLoggedIn) {
    return { hasAccess: false, reason: '需要登录' }
  }
  if (!checkRouteAccess(route)) {
    return { hasAccess: false, reason: '没有访问权限' }
  }
  return { hasAccess: true }
}

export function batchCheckRoutePermissions(routes: any[]): Record<string, RouteCheckResult> {
  const results: Record<string, RouteCheckResult> = {}
  const checkRoute = (route: any) => {
    results[route.path] = checkRoutePermission(route)
    if (route.children) route.children.forEach((child: any) => checkRoute(child))
  }
  routes.forEach(route => checkRoute(route))
  return results
}

export function getAccessibleRoutes(routes: any[]): any[] {
  const filtered = filterRoutesByPermission(routes)
  return filtered.map(route => ({
    ...route,
    children: route.children ? getAccessibleRoutes(route.children) : undefined
  }))
}

export async function canNavigateTo(router: Router, path: string): Promise<boolean> {
  try {
    return checkRoutePermission(router.resolve(path)).hasAccess
  } catch {
    return false
  }
}

export async function safeNavigate(router: Router, path: string, options?: { replace?: boolean; query?: Record<string, any> }): Promise<void> {
  if (!(await canNavigateTo(router, path))) {
    message.error('您没有权限访问此页面')
    return
  }
  if (options?.replace) {
    await router.replace({ path, query: options.query })
  } else {
    await router.push({ path, query: options.query })
  }
}

export const guardConfig = {
  whiteList: ['/login', '/register', '/404', '/403', '/500'],
  superAdminPermissions: ['*'],
  defaultRoute: '/dashboard',
  loginRedirect: '/dashboard',
  noPermissionRedirect: '/403'
}

export function isInWhiteList(path: string): boolean {
  return guardConfig.whiteList.some(item => path.startsWith(item))
}

export function setupDefaultRoute(router: Router) {
  router.addRoute({ path: '/', redirect: guardConfig.defaultRoute })
}

export default {
  setupRouterGuard, requiresAuth, checkRoutePermission,
  batchCheckRoutePermissions, getAccessibleRoutes,
  canNavigateTo, safeNavigate, guardConfig,
  isInWhiteList, setupDefaultRoute
}
