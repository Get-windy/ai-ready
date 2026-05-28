import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/utils/request'

const CLIENT_TYPE = 'pc-admin'

interface MenuItem {
  id: number
  parentId: number
  menuName: string
  menuCode: string
  menuType: number
  path?: string
  component?: string
  routeName?: string
  redirect?: string
  icon?: string
  sort: number
  isExternal?: number
  isCache?: number
  visible: number
  status: number
  clientType?: string
  children?: MenuItem[]
}

function transformMenuToRoute(menu: MenuItem): RouteRecordRaw {
  const route: RouteRecordRaw = {
    path: menu.path || '',
    name: menu.routeName || menu.menuCode,
    meta: {
      title: menu.menuName,
      icon: menu.icon,
      keepAlive: menu.isCache === 1,
      hidden: menu.visible === 0,
      requiresAuth: true
    }
  }

  if (menu.menuType === 1 && menu.component) {
    const componentPath = menu.component.replace(/^views\//, '').replace(/\.vue$/, '')
    route.component = () => import(/* @vite-ignore */ `../views/${componentPath}.vue`)
  }

  if (menu.children && menu.children.length > 0) {
    route.children = menu.children.map(child => transformMenuToRoute(child))
  }

  if (menu.redirect) {
    route.redirect = menu.redirect
  }

  return route
}

function buildMenuTree(menus: MenuItem[], parentId: number = 0): MenuItem[] {
  return menus
    .filter(menu => menu.parentId === parentId)
    .sort((a, b) => a.sort - b.sort)
    .map(menu => ({
      ...menu,
      children: buildMenuTree(menus, menu.id)
    }))
}

export async function loadDynamicRoutes(): Promise<RouteRecordRaw[]> {
  console.log('[动态路由] 开始加载动态路由...')
  const userStore = useUserStore()
  const userId = userStore.userId
  const tenantId = 1

  console.log('[动态路由] 用户信息:', { userId, tenantId, token: userStore.token })

  try {
    console.log('[动态路由] 调用菜单API...')
    const response = await request.get(`/menu/user/client/${CLIENT_TYPE}`, {
      params: { userId, tenantId }
    })

    console.log('[动态路由] API响应:', response)
    console.log('[动态路由] API响应数据:', response.data)

    if (response.data && response.data.length > 0) {
      console.log('[动态路由] 菜单数据数量:', response.data.length)
      const menuTree = buildMenuTree(response.data)
      console.log('[动态路由] 菜单树:', menuTree)
      const layoutRoute: RouteRecordRaw = {
        path: '/',
        name: 'Layout',
        component: () => import('@/layouts/BasicLayout.vue'),
        redirect: '/dashboard',
        meta: { requiresAuth: true },
        children: menuTree.map(menu => transformMenuToRoute(menu))
      }
      console.log('[动态路由] Layout路由:', layoutRoute)
      return [layoutRoute]
    } else {
      console.log('[动态路由] 菜单数据为空，使用fallback路由')
    }
  } catch (error) {
    console.error('[动态路由] API调用失败:', error)
    console.error('[动态路由] 错误详情:', error?.response?.data || error?.message)
  }

  console.log('[动态路由] 返回fallback路由')
  return getFallbackRoutes()
}

function getFallbackRoutes(): RouteRecordRaw[] {
  return [
    {
      path: '/',
      name: 'Layout',
      component: () => import('@/layouts/BasicLayout.vue'),
      redirect: '/dashboard',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/index.vue'),
          meta: { title: '工作台', icon: 'DashboardOutlined', keepAlive: true, requiresAuth: true }
        }
      ]
    }
  ]
}

export function checkRouteAccess(route: any): boolean {
  const userStore = useUserStore()
  const permissions = route.meta?.permissions as string[] | undefined
  
  if (!permissions || permissions.length === 0) {
    return true
  }
  
  const userPermissions = userStore.permissions || []
  if (userPermissions.includes('*')) {
    return true
  }
  
  return permissions.some(permission => userPermissions.includes(permission))
}

export function filterRoutesByPermission(routes: RouteRecordRaw[]): RouteRecordRaw[] {
  return routes.filter(route => {
    if (!checkRouteAccess(route)) {
      return false
    }
    
    if (route.children) {
      route.children = filterRoutesByPermission(route.children)
    }
    
    return true
  })
}