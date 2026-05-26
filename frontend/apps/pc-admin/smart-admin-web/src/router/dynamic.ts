/**
 * 动态路由工具函数
 */
import type { RouteRecordRaw } from 'vue-router'
import router, { constantRoutes, resetRouter } from './index'
import { useUserStore } from '@/stores/user'

/**
 * 菜单数据转路由配置
 */
export function generateRoutes(menus: any[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []

  function transform(menus: any[], parentPath = '') {
    menus.forEach(menu => {
      const route: any = {
        path: menu.path.startsWith('/') ? menu.path : `${parentPath}/${menu.path}`,
        name: menu.name || menu.code,
        meta: {
          title: menu.title,
          icon: menu.icon,
          permissions: menu.permissions,
          keepAlive: menu.keepAlive ?? false,
          hidden: menu.hidden ?? false
        }
      }

      if (menu.component) {
        route.component = () => import(`@/views/${menu.component}.vue`)
      }

      if (menu.children && menu.children.length > 0) {
        route.children = []
        transform(menu.children, route.path)
      }

      routes.push(route)
    })
  }

  transform(menus)
  return routes
}

/**
 * 添加动态路由
 */
export function addRoutes(routes: RouteRecordRaw[]) {
  routes.forEach(route => {
    router.addRoute(route)
  })
}

/**
 * 移除动态路由
 */
export function removeRoutes() {
  resetRouter()
}

/**
 * 根据用户权限过滤路由
 */
export function filterRoutesByPermission(routes: RouteRecordRaw[], permissions: string[]): RouteRecordRaw[] {
  const result: RouteRecordRaw[] = []

  routes.forEach(route => {
    const routePermissions = route.meta?.permissions as string[] | undefined
    
    // 无权限要求或用户有权限
    if (!routePermissions || routePermissions.length === 0 ||
        routePermissions.some(p => permissions.includes(p))) {
      
      // 递归处理子路由
      if (route.children && route.children.length > 0) {
        route.children = filterRoutesByPermission(route.children, permissions)
      }
      
      result.push(route)
    }
  })

  return result
}

/**
 * 初始化动态路由
 */
export async function initDynamicRoutes() {
  const userStore = useUserStore()
  
  // 获取用户权限
  const permissions = userStore.permissions
  
  // 过滤有权限的路由
  const allowedRoutes = filterRoutesByPermission(asyncRoutes, permissions)
  
  // 添加路由
  addRoutes(allowedRoutes)
  
  return allowedRoutes
}

// 需要从router导入asyncRoutes
import { asyncRoutes } from './index'