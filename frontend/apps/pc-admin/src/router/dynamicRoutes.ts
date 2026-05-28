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
  const userStore = useUserStore()
  const userId = userStore.userId
  const tenantId = 1

  try {
    const response = await request.get(`/menu/user/client/${CLIENT_TYPE}`, {
      params: { userId, tenantId }
    })

    if (response.data && response.data.length > 0) {
      const menuTree = response.data
      userStore.menus = menuTree as any
      
      const layoutRoute: RouteRecordRaw = {
        path: '/',
        name: 'Layout',
        component: () => import('@/layouts/BasicLayout.vue'),
        redirect: '/dashboard',
        meta: { requiresAuth: true },
        children: menuTree.map(menu => transformMenuToRoute(menu))
      }
      return [layoutRoute]
    }
  } catch (error) {
    console.error('[动态路由] 加载失败:', error)
  }

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