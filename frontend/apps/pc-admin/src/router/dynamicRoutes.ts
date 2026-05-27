import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

export async function loadDynamicRoutes(): Promise<RouteRecordRaw[]> {
  return []
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