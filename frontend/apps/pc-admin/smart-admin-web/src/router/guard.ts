/**
 * 路由权限守卫
 * 提供路由级别的权限控制功能
 */

import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { checkRouteAccess, filterRoutesByPermission } from './dynamicRoutes'
import { message } from 'ant-design-vue'

/**
 * 路由守卫选项
 */
export interface RouterGuardOptions {
  // 路由跳转前的回调
  beforeEach?: (to: any, from: any, next: any) => void | Promise<void>
  // 路由跳转后的回调
  afterEach?: (to: any, from: any) => void | Promise<void>
  // 路由跳转失败的回调
  onError?: (error: any) => void | Promise<void>
}

/**
 * 设置路由权限守卫
 * @param router 路由实例
 * @param options 守卫选项
 */
export function setupRouterGuard(router: Router, options?: RouterGuardOptions) {
  // 前置守卫
  router.beforeEach(async (to, from, next) => {
    try {
      // 检查路由是否需要认证
      if (to.meta.requiresAuth === false) {
        next()
        return
      }

      const userStore = useUserStore()
      
      // 检查是否已登录
      if (!userStore.isLoggedIn) {
        message.warning('请先登录')
        next({
          path: '/login',
          query: { redirect: to.fullPath }
        })
        return
      }

      // 检查用户信息是否已加载
      if (!userStore.userInfo) {
        await userStore.getUserInfo()
      }

      // 检查路由权限
      if (!checkRouteAccess(to)) {
        message.error('您没有权限访问此页面')
        next({ path: '/403' })
        return
      }

      // 设置页面标题
      document.title = to.meta.title ? `${to.meta.title} - AI-Ready` : 'AI-Ready'

      // 执行自定义前置守卫
      if (options?.beforeEach) {
        await options.beforeEach(to, from, next)
        return
      }

      next()
    } catch (error) {
      console.error('路由守卫错误:', error)
      if (options?.onError) {
        await options.onError(error)
      }
      next({ path: '/500' })
    }
  })

  // 后置守卫
  router.afterEach((to, from) => {
    // 执行自定义后置守卫
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

/**
 * 检查路由是否需要登录
 * @param route 路由对象
 * @returns 是否需要登录
 */
export function requiresAuth(route: any): boolean {
  if (route.meta?.requiresAuth === false) {
    return false
  }
  return true
}

/**
 * 路由权限检查结果
 */
export interface RouteCheckResult {
  hasAccess: boolean
  reason?: string
}

/**
 * 检查路由访问权限
 * @param route 路由对象
 * @returns 权限检查结果
 */
export function checkRoutePermission(route: any): RouteCheckResult {
  const userStore = useUserStore()
  
  // 检查是否需要登录
  if (requiresAuth(route) && !userStore.isLoggedIn) {
    return {
      hasAccess: false,
      reason: '需要登录'
    }
  }

  // 检查路由权限
  if (!checkRouteAccess(route)) {
    return {
      hasAccess: false,
      reason: '没有访问权限'
    }
  }

  return {
    hasAccess: true
  }
}

/**
 * 批量检查路由权限
 * @param routes 路由列表
 * @returns 权限检查结果对象
 */
export function batchCheckRoutePermissions(
  routes: any[]
): Record<string, RouteCheckResult> {
  const results: Record<string, RouteCheckResult> = {}
  
  const checkRoute = (route: any) => {
    results[route.path] = checkRoutePermission(route)
    
    if (route.children) {
      route.children.forEach((child: any) => checkRoute(child))
    }
  }
  
  routes.forEach(route => checkRoute(route))
  
  return results
}

/**
 * 获取可访问的路由列表
 * @param routes 所有路由
 * @returns 可访问的路由列表
 */
export function getAccessibleRoutes(routes: any[]): any[] {
  const filteredRoutes = filterRoutesByPermission(routes)
  
  const processRoutes = (routes: any[]): any[] => {
    return routes.map(route => {
      const routeCopy = { ...route }
      
      if (route.children) {
        routeCopy.children = processRoutes(route.children)
      }
      
      return routeCopy
    })
  }
  
  return processRoutes(filteredRoutes)
}

/**
 * 路由跳转权限检查
 * @param router 路由实例
 * @param path 目标路径
 * @returns 是否可以跳转
 */
export async function canNavigateTo(
  router: Router,
  path: string
): Promise<boolean> {
  try {
    const route = router.resolve(path)
    const result = checkRoutePermission(route)
    return result.hasAccess
  } catch (error) {
    console.error('路由检查错误:', error)
    return false
  }
}

/**
 * 安全跳转
 * @param router 路由实例
 * @param path 目标路径
 * @param options 跳转选项
 */
export async function safeNavigate(
  router: Router,
  path: string,
  options?: {
    replace?: boolean
    query?: Record<string, any>
  }
): Promise<void> {
  const canAccess = await canNavigateTo(router, path)
  
  if (!canAccess) {
    message.error('您没有权限访问此页面')
    return
  }
  
  if (options?.replace) {
    await router.replace({ path, query: options.query })
  } else {
    await router.push({ path, query: options.query })
  }
}

/**
 * 权限守卫配置
 */
export const guardConfig = {
  // 白名单路径（不需要登录）
  whiteList: ['/login', '/register', '/404', '/403', '/500'],
  
  // 超级管理员权限
  superAdminPermissions: ['*'],
  
  // 默认路由
  defaultRoute: '/dashboard',
  
  // 登录后默认路由
  loginRedirect: '/dashboard',
  
  // 无权限跳转路由
  noPermissionRedirect: '/403'
}

/**
 * 检查路径是否在白名单中
 * @param path 路径
 * @returns 是否在白名单
 */
export function isInWhiteList(path: string): boolean {
  return guardConfig.whiteList.some(item => path.startsWith(item))
}

/**
 * 设置默认路由
 * @param router 路由实例
 */
export function setupDefaultRoute(router: Router) {
  router.addRoute({
    path: '/',
    redirect: guardConfig.defaultRoute
  })
}

export default {
  setupRouterGuard,
  requiresAuth,
  checkRoutePermission,
  batchCheckRoutePermissions,
  getAccessibleRoutes,
  canNavigateTo,
  safeNavigate,
  guardConfig,
  isInWhiteList,
  setupDefaultRoute
}