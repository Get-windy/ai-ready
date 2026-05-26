/**
 * 动态路由模块
 * 根据用户权限动态生成路由
 */

import type { RouteRecordRaw } from 'vue-router'
import { permissionApi, type PermissionInfo } from '@/api/permission'
import { hasPermission, hasAnyPermission } from '@/utils/permission'
import { useUserStore } from '@/stores/user'

/**
 * 组件映射表
 * 将后端返回的组件路径映射到实际的组件
 */
const componentMap: Record<string, () => Promise<any>> = {
  // 错误页面
  'views/error/404.vue': () => import('@/views/error/404.vue'),
  'views/error/403.vue': () => import('@/views/error/403.vue'),
  'views/error/500.vue': () => import('@/views/error/500.vue'),

  // 系统管理
  'views/system/user/index.vue': () => import('@/views/system/user/index.vue'),
  'views/system/role/index.vue': () => import('@/views/system/role/index.vue'),
  'views/system/permission/index.vue': () => import('@/views/system/permission/index.vue'),
  'views/system/menu/index.vue': () => import('@/views/system/menu/index.vue'),
  'views/system/department/index.vue': () => import('@/views/system/department/index.vue'),
  'views/system/position/index.vue': () => import('@/views/system/position/index.vue'),

  // ERP 模块
  'views/erp/purchase/index.vue': () => import('@/views/erp/purchase/index.vue'),
  'views/erp/purchase-exchange/index.vue': () => import('@/views/erp/purchase-exchange/index.vue'),
  'views/erp/sale/index.vue': () => import('@/views/erp/sale/index.vue'),
  'views/erp/stock/index.vue': () => import('@/views/erp/stock/index.vue'),
  'views/erp/stock-in/index.vue': () => import('@/views/erp/stock-in/index.vue'),
  'views/erp/shipment/index.vue': () => import('@/views/erp/shipment/index.vue'),
  'views/erp/return/index.vue': () => import('@/views/erp/return/index.vue'),
  'views/erp/stocktake/index.vue': () => import('@/views/erp/stocktake/index.vue'),
  'views/erp/stock/replenishment/index.vue': () => import('@/views/erp/stock/replenishment/index.vue'),
  'views/erp/sales-report/index.vue': () => import('@/views/erp/sales-report/index.vue'),
  'views/erp/sales-analysis/index.vue': () => import('@/views/erp/sales-analysis/index.vue'),
  'views/erp/pricing/approval/index.vue': () => import('@/views/erp/pricing/approval/index.vue'),

  // CRM 模块
  'views/crm/customer/index.vue': () => import('@/views/crm/customer/index.vue'),
  'views/crm/lead/index.vue': () => import('@/views/crm/lead/index.vue'),
  'views/crm/opportunity/index.vue': () => import('@/views/crm/opportunity/index.vue'),
  'views/crm/contract/index.vue': () => import('@/views/crm/contract/index.vue'),
  'views/crm/quotation/index.vue': () => import('@/views/crm/quotation/index.vue'),
  'views/crm/invoice/index.vue': () => import('@/views/crm/invoice/index.vue'),
  'views/crm/supplier/index.vue': () => import('@/views/crm/supplier/index.vue'),
  
  // 供应商门户模块
  'views/supplier/index.vue': () => import('@/views/supplier/index.vue'),
  'views/supplier/detail.vue': () => import('@/views/supplier/detail.vue'),
  'views/supplier/create.vue': () => import('@/views/supplier/create.vue'),
  'views/supplier/edit.vue': () => import('@/views/supplier/edit.vue'),
  'views/supplier/performance/index.vue': () => import('@/views/supplier/performance/index.vue'),
  'views/supplier/inquiry/index.vue': () => import('@/views/supplier/inquiry/index.vue'),
  
  // 订单中心模块
  'views/order-center/index.vue': () => import('@/views/order-center/index.vue'),

  // 财务模块
  'views/finance/accounts-payable/index.vue': () => import('@/views/finance/accounts-payable/index.vue'),
  'views/finance/accounts-receivable/index.vue': () => import('@/views/finance/accounts-receivable/index.vue'),
  'views/finance/reconciliation/index.vue': () => import('@/views/finance/reconciliation/index.vue'),
  'views/finance/reports/index.vue': () => import('@/views/finance/reports/index.vue'),

  // 工作流模块
  'views/workflow/instance-monitor.vue': () => import('@/views/workflow/instance-monitor.vue'),
  'views/workflow/process-analysis.vue': () => import('@/views/workflow/process-analysis.vue'),
  'views/workflow/task-management.vue': () => import('@/views/workflow/task-management.vue'),

  // 工作台
  'views/dashboard/index.vue': () => import('@/views/dashboard/index.vue'),

  // 图表展示
  'views/charts/index.vue': () => import('@/views/charts/index.vue')
}

/**
 * 获取组件
 * @param componentPath 组件路径
 * @returns 组件加载函数
 */
function getComponent(componentPath: string) {
  // 标准化路径
  const normalizedPath = componentPath.replace(/^@\//, '').replace(/^src\//, '')

  // 查找组件映射
  const componentLoader = componentMap[normalizedPath]

  if (componentLoader) {
    return componentLoader
  }

  // 如果找不到，返回 404 页面
  console.warn(`Component not found: ${componentPath}, using 404 page`)
  return componentMap['views/error/404.vue']
}

/**
 * 路由元信息类型
 */
export interface DynamicRouteMeta {
  title: string
  icon?: string
  hidden?: boolean
  keepAlive?: boolean
  affix?: boolean
  breadcrumb?: boolean
  permissions?: string[]
  roles?: string[]
  parent?: string
}

/**
 * 动态路由信息
 */
export interface DynamicRouteInfo {
  path: string
  name: string
  component: string
  meta: DynamicRouteMeta
  children?: DynamicRouteInfo[]
  redirect?: string
}

/**
 * 将后端权限数据转换为路由
 * @param permissions 权限列表
 * @returns 路由列表
 */
export function permissionsToRoutes(permissions: PermissionInfo[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []

  // 过滤出菜单类型的权限（type 0=目录, 1=菜单）
  const menuPermissions = permissions.filter(p =>
    p.permissionType === 0 || p.permissionType === 1
  )

  // 构建路由树
  const routeMap = new Map<string, DynamicRouteInfo>()
  const rootRoutes: DynamicRouteInfo[] = []

  // 首先创建所有路由节点
  menuPermissions.forEach(permission => {
    const routeInfo: DynamicRouteInfo = {
      path: permission.path || `/${permission.permissionCode}`,
      name: permission.permissionCode.replace(/:/g, '-'),
      component: permission.component || 'views/error/404.vue',
      meta: {
        title: permission.permissionName,
        icon: permission.icon,
        hidden: permission.visible !== 1,
        keepAlive: true,
        permissions: [permission.permissionCode]
      },
      children: []
    }

    routeMap.set(permission.permissionCode, routeInfo)

    if (permission.parentId === 0 || !permission.parentId) {
      rootRoutes.push(routeInfo)
    }
  })

  // 构建父子关系
  menuPermissions.forEach(permission => {
    if (permission.parentId && permission.parentId !== 0) {
      const parentPermission = permissions.find(p => p.id === permission.parentId)
      if (parentPermission) {
        const parentRoute = routeMap.get(parentPermission.permissionCode)
        const childRoute = routeMap.get(permission.permissionCode)
        if (parentRoute && childRoute) {
          if (!parentRoute.children) {
            parentRoute.children = []
          }
          parentRoute.children.push(childRoute)

          // 如果是目录类型，设置重定向到第一个子路由
          if (parentPermission.permissionType === 0 && childRoute.children) {
            parentRoute.redirect = childRoute.path
          }
        }
      }
    }
  })

  // 转换为Vue路由格式
  return convertToVueRoutes(rootRoutes)
}

/**
 * 转换为Vue路由格式
 * @param routeInfos 路由信息列表
 * @returns Vue路由列表
 */
function convertToVueRoutes(routeInfos: DynamicRouteInfo[]): RouteRecordRaw[] {
  return routeInfos.map(route => {
    const vueRoute: RouteRecordRaw = {
      path: route.path,
      name: route.name,
      meta: route.meta
    }

    // 设置组件
    if (route.component) {
      vueRoute.component = getComponent(route.component)
    }

    // 设置重定向
    if (route.redirect) {
      vueRoute.redirect = route.redirect
    }

    // 处理子路由
    if (route.children && route.children.length > 0) {
      vueRoute.children = convertToVueRoutes(route.children)
    }

    return vueRoute
  })
}

/**
 * 加载动态路由
 * @returns 路由列表
 */
export async function loadDynamicRoutes(): Promise<RouteRecordRaw[]> {
  try {
    const userStore = useUserStore()
    const tenantId = userStore.userInfo?.tenantId || 1

    // 获取用户权限树
    const response = await permissionApi.getTree(tenantId)

    if (response.data) {
      return permissionsToRoutes(response.data)
    }

    return []
  } catch (error) {
    console.error('加载动态路由失败:', error)
    return []
  }
}

/**
 * 过滤路由权限
 * @param routes 路由列表
 * @returns 过滤后的路由列表
 */
export function filterRoutesByPermission(routes: RouteRecordRaw[]): RouteRecordRaw[] {
  const userStore = useUserStore()
  const userPermissions = userStore.permissions

  // 如果是超级管理员，返回所有路由
  if (userPermissions.includes('*')) {
    return routes
  }

  return routes.filter(route => {
    const meta = route.meta as any

    // 如果路由没有设置权限要求，允许访问
    if (!meta || (!meta.permissions && !meta.roles)) {
      return true
    }

    // 检查权限
    if (meta.permissions && meta.permissions.length > 0) {
      return hasAnyPermission(meta.permissions)
    }

    // 检查角色
    if (meta.roles && meta.roles.length > 0) {
      const userRoles = userStore.roles
      return meta.roles.some((role: string) => userRoles.includes(role))
    }

    return false
  }).map(route => {
    // 递归处理子路由
    if (route.children && route.children.length > 0) {
      route.children = filterRoutesByPermission(route.children)
    }
    return route
  })
}

/**
 * 检查路由是否可访问
 * @param route 路由对象
 * @returns 是否可访问
 */
export function checkRouteAccess(route: RouteRecordRaw): boolean {
  const meta = route.meta as any
  const userStore = useUserStore()

  // 如果是超级管理员，允许访问
  if (userStore.permissions.includes('*')) {
    return true
  }

  // 如果路由没有设置权限要求，允许访问
  if (!meta || (!meta.permissions && !meta.roles)) {
    return true
  }

  // 检查权限
  if (meta.permissions && meta.permissions.length > 0) {
    return hasAnyPermission(meta.permissions)
  }

  // 检查角色
  if (meta.roles && meta.roles.length > 0) {
    const userRoles = userStore.roles
    return meta.roles.some((role: string) => userRoles.includes(role))
  }

  return true
}

/**
 * 生成面包屑导航
 * @param route 当前路由
 * @param routes 所有路由
 * @returns 面包屑数据
 */
export function generateBreadcrumbs(
  route: RouteRecordRaw,
  routes: RouteRecordRaw[]
): Array<{ title: string; path: string }> {
  const breadcrumbs: Array<{ title: string; path: string }> = []
  const path = route.path

  // 查找路由路径上的所有父路由
  const findParentRoutes = (
    currentPath: string,
    allRoutes: RouteRecordRaw[],
    currentBreadcrumbs: Array<{ title: string; path: string }>
  ): boolean => {
    for (const r of allRoutes) {
      if (path.startsWith(r.path) && r.path !== '/') {
        currentBreadcrumbs.push({
          title: (r.meta as any)?.title || r.path,
          path: r.path
        })

        if (r.path === path) {
          return true
        }

        if (r.children && findParentRoutes(currentPath, r.children, currentBreadcrumbs)) {
          return true
        }

        currentBreadcrumbs.pop()
      }
    }
    return false
  }

  findParentRoutes(path, routes, breadcrumbs)
  return breadcrumbs
}

/**
 * 获取首页路由
 * @param routes 所有路由
 * @returns 首页路由
 */
export function getHomeRoute(routes: RouteRecordRaw[]): RouteRecordRaw | null {
  for (const route of routes) {
    const meta = route.meta as any
    if (meta && meta.affix) {
      return route
    }
    if (route.children) {
      const childRoute = getHomeRoute(route.children)
      if (childRoute) return childRoute
    }
  }
  return null
}

export default {
  permissionsToRoutes,
  loadDynamicRoutes,
  filterRoutesByPermission,
  checkRouteAccess,
  generateBreadcrumbs,
  getHomeRoute
}