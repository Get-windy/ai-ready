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
const G = '[DEBUG:guard]'

export function setupRouterGuard(router: Router, options?: RouterGuardOptions) {
  router.beforeEach(async (to, from, next) => {
    console.log(`${G} beforeEach: from="${from.path}" → to="${to.path}", isLoggedIn=${useUserStore().isLoggedIn}, matched=${to.matched.length}`)
    try {
      const userStore = useUserStore()

      // 登录/注册页直接放行
      if (to.path === '/login' || to.path === '/register') {
        console.log(`${G} 放行登录/注册页: path=${to.path}`)
        next()
        return
      }

      // 检查登录状态
      if (!userStore.isLoggedIn) {
        console.log(`${G} 未登录 -> 跳转登录页`)
        message.warning('请先登录')
        next({ path: '/login', query: { redirect: to.fullPath } })
        return
      }

      console.log(`${G} 已登录, userInfo=${!!userStore.userInfo}, dynamicRoutesLoaded=${dynamicRoutesLoaded}`)

      // 加载用户信息
      if (!userStore.userInfo) {
        console.log(`${G} 开始加载用户信息 getUserInfo()`)
        try {
          await userStore.getUserInfo()
          console.log(`${G} getUserInfo() 完成 ✅`)
        } catch (err) {
          console.log(`${G} getUserInfo() 失败 ❌:`, err)
          userStore.logout()
          next({ path: '/login', replace: true })
          return
        }
      }

      // 加载动态路由
      if (!dynamicRoutesLoaded) {
        console.log(`${G} 开始加载动态路由 loadDynamicRoutes()`)
        try {
          const dynamicRoutes = await loadDynamicRoutes()
          console.log(`${G} 动态路由加载完成 ✅, 数量=${dynamicRoutes.length}`)
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
          next()
          return
        } catch (error: any) {
          console.log(`${G} 动态路由加载失败 ❌:`, error?.message)
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
        console.log(`${G} 404 -> 跳转 /dashboard`)
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
