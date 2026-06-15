/**
 * 路由权限守卫
 * 提供路由级别的权限控制功能
 *
 * 使用 vue-router v4 推荐的方式：返回 RouteLocation 替代调用 next()
 * 避免 next({ path }) 导致的同步递归 pushWithRedirect 栈溢出
 */

import type { Router, RouteLocationNormalized } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { checkRouteAccess, filterRoutesByPermission, loadDynamicRoutes } from './dynamicRoutes'
import { message } from 'ant-design-vue'
import { isTokenExpired, isJWT, getToken, verifyToken, clearTokenVerifyCache } from '@/utils/tokenRefresher'

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
  beforeEach?: (to: RouteLocationNormalized, from: RouteLocationNormalized, next: any) => void | Promise<void>
  afterEach?: (to: RouteLocationNormalized, from: RouteLocationNormalized) => void | Promise<void>
  onError?: (error: any) => void | Promise<void>
}

/**
 * 设置路由权限守卫
 * 使用 return 模式代替 next() 回调，避免 vue-router v4 的同步递归问题
 */
export function setupRouterGuard(router: Router, options?: RouterGuardOptions) {
  // 前置守卫
  router.beforeEach(async (to, from) => {
    try {
      const userStore = useUserStore()

      // 登录/注册页直接放行
      if (to.path === '/login' || to.path === '/register') {
        return
      }

      // 检查登录状态
      if (!userStore.isLoggedIn) {
        console.warn('[路由守卫] 未登录 -> 跳转登录页')
        message.warning('请先登录')
        return { path: '/login', query: { redirect: to.fullPath } }
      }

      // Token 过期检查（仅对 JWT 格式有效）
      const token = getToken()
      if (token && isTokenExpired(token)) {
        console.warn('[路由守卫] Token已过期 -> 清除状态并跳转登录页')
        resetDynamicRoutesLoaded()
        message.warning('登录已过期，请重新登录')
        userStore.logout()
        return { path: '/login', query: { redirect: to.fullPath }, replace: true }
      }

      // 非 JWT 格式 token（如 UUID）→ 后端验证
      if (token && !isJWT(token)) {
        let valid = await verifyToken()
        if (!valid) {
          console.warn('[路由守卫] Token首次验证失败，等待500ms后重试...')
          await new Promise(resolve => setTimeout(resolve, 500))
          clearTokenVerifyCache()
          valid = await verifyToken()
        }

        if (!valid) {
          console.warn('[路由守卫] 后端Token验证失败（重试后仍无效） -> 清除状态并跳转登录页')
          resetDynamicRoutesLoaded()
          clearTokenVerifyCache()
          message.warning('登录已过期，请重新登录')
          userStore.logout()
          return { path: '/login', query: { redirect: to.fullPath }, replace: true }
        }
      }

      // 加载用户信息
      if (!userStore.userInfo) {
        try {
          await userStore.getUserInfo()
          if (userStore.userInfo?.passwordExpired) {
            message.warning('您的密码已过期，请及时修改密码', 5)
          }
        } catch (err) {
          console.warn('[路由守卫] getUserInfo() 失败:', err)
          resetDynamicRoutesLoaded()
          userStore.logout()
          return { path: '/login', replace: true }
        }
      }

      // 加载动态路由
      if (!dynamicRoutesLoaded) {
        try {
          const dynamicRoutes = await loadDynamicRoutes()
          for (const route of dynamicRoutes) {
            if (route.name === 'Layout' && route.children) {
              const children = [...route.children]
              const layoutParent: any = {
                path: route.path,
                name: route.name,
                component: route.component,
                meta: route.meta,
              }
              // 注意：不复制 redirect 属性。
              // vue-router v4 的 handleRedirectRecord 会检查 matched routes 最后一个的 redirect，
              // 如果菜单数据中存在自身引用或循环的重定向配置，会导致 pushWithRedirect 无限递归
              // （Maximum call stack size exceeded at handleRedirectRecord）。
              // 路由守卫自身已处理登录后的重定向，不需要路由记录的 redirect 来干扰。
              router.addRoute(layoutParent)
              for (const child of children) {
                // 剥离子路由的 redirect，同样原因
                const { redirect: _r, ...childWithoutRedirect } = child as any
                router.addRoute('Layout', childWithoutRedirect)
              }
            } else {
              const { redirect: _r, ...routeWithoutRedirect } = route as any
              router.addRoute(routeWithoutRedirect)
            }
          }

          dynamicRoutesLoaded = true

          // 返回重定向目标。注意：已剥离所有动态路由的 redirect 属性，
          // 因此 pushWithRedirect 重新解析时 handleRedirectRecord 不会触发递归。
          const redirectPath = to.path === '/' || to.path === '/login' ? '/dashboard' : to.fullPath
          return { path: redirectPath, replace: true }
        } catch (error: any) {
          console.warn('[路由守卫] 动态路由加载失败:', error?.message)
          if (error?.response?.status === 401 || error?.status === 401) {
            message.warning('登录已过期，请重新登录')
            resetDynamicRoutesLoaded()
            userStore.logout()
            return { path: '/login', replace: true }
          }
          dynamicRoutesLoaded = true
          return { path: '/dashboard', replace: true }
        }
      }

      // 404 检查 — 避免跳转目标再次匹配失败导致无限循环
      if (to.matched.length === 0) {
        if (to.path === '/dashboard') {
          console.warn('[路由守卫] 首页 404 -> 跳转 /403')
          return { path: '/403', replace: true }
        } else {
          console.warn('[路由守卫] 404 -> 跳转 /dashboard')
          return { path: '/dashboard', replace: true }
        }
      }

      // 权限检查
      if (!checkRouteAccess(to)) {
        message.error('您没有权限访问此页面')
        return { path: '/403' }
      }

      // 页面标题
      document.title = to.meta.title ? `${to.meta.title} - AI-Ready` : 'AI-Ready'

      // 自定义前置守卫（兼容旧式 next 回调）
      if (options?.beforeEach) {
        return new Promise<void>((resolve) => {
          options.beforeEach!(to, from, () => resolve())
        })
      }

      return // continue
    } catch (error) {
      console.error('[路由守卫] 错误:', error)
      dynamicRoutesLoaded = false
      if (options?.onError) {
        await options.onError(error)
      }
      return { path: '/login', replace: true }
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
