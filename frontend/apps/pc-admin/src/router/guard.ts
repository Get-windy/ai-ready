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
    console.log('[路由守卫] 开始:', { 
      to: to.path, 
      from: from.path, 
      requiresAuth: to.meta.requiresAuth,
      name: to.name,
      matched: to.matched.length,
      dynamicRoutesLoaded 
    })
    
    try {
      const userStore = useUserStore()
      
      // 如果是登录页，直接放行
      if (to.path === '/login' || to.path === '/register') {
        console.log('[路由守卫] 登录/注册页，直接放行')
        next()
        return
      }
      
      // 检查是否已登录
      if (!userStore.isLoggedIn) {
        console.log('[路由守卫] 未登录，跳转到登录页')
        message.warning('请先登录')
        next({
          path: '/login',
          query: { redirect: to.fullPath }
        })
        return
      }

      // 检查用户信息是否已加载
      if (!userStore.userInfo) {
        console.log('[路由守卫] 加载用户信息...')
        try {
          await userStore.getUserInfo()
          console.log('[路由守卫] 用户信息加载完成:', userStore.userInfo)
        } catch (error) {
          console.error('[路由守卫] 用户信息加载失败:', error)
          // 用户信息加载失败，可能是token过期，清除登录状态
          userStore.logout()
          next({ path: '/login', replace: true })
          return
        }
      }

      // 加载动态路由（首次或刷新页面时）
      if (!dynamicRoutesLoaded) {
        console.log('[路由守卫] 开始加载动态路由...')
        try {
          const dynamicRoutes = await loadDynamicRoutes()
          console.log('[路由守卫] 动态路由加载完成:', dynamicRoutes)
          
          dynamicRoutes.forEach(route => {
            const children = route.children
            if (children && children.length > 0) {
              const { children: _, ...parentRoute } = route
              console.log('[路由守卫] 注册父路由:', parentRoute.path, parentRoute.name)
              router.addRoute(parentRoute as any)
              children.forEach((child: any) => {
                console.log('[路由守卫] 注册子路由:', child.path, child.name)
                router.addRoute(route.name as string, child)
              })
            } else {
              console.log('[路由守卫] 注册路由:', route.path, route.name)
              router.addRoute(route)
            }
          })
          
          dynamicRoutesLoaded = true
          console.log('[路由守卫] 所有路由已处理，重新导航到:', to.path)
          next({ path: to.path, replace: true })
          return
        } catch (error) {
          console.error('[路由守卫] 动态路由加载失败:', error)
          dynamicRoutesLoaded = true // 标记为已加载，避免无限循环
          next({ path: '/dashboard', replace: true })
          return
        }
      }

      // 如果匹配到 404 路由，说明路由不存在
      if (to.matched.length === 0) {
        console.log('[路由守卫] 路由不存在，跳转到首页')
        next({ path: '/dashboard', replace: true })
        return
      }

      console.log('[路由守卫] 动态路由已加载，检查权限...')
      
      // 检查路由权限
      if (!checkRouteAccess(to)) {
        console.log('[路由守卫] 无权限访问:', to.path)
        message.error('您没有权限访问此页面')
        next({ path: '/403' })
        return
      }

      // 设置页面标题
      document.title = to.meta.title ? `${to.meta.title} - AI-Ready` : 'AI-Ready'

      // 执行自定义前置守卫
      if (options?.beforeEach) {
        console.log('[路由守卫] 执行自定义前置守卫')
        await options.beforeEach(to, from, next)
        return
      }

      console.log('[路由守卫] 放行到:', to.path)
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