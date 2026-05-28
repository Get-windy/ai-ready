import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

/**
 * 动态路由（需要权限）
 */
export const asyncRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/layouts/BasicLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { 
          title: '工作台', 
          icon: 'DashboardOutlined',
          affix: true,
          keepAlive: true
        }
      },
      {
        path: 'erp',
        name: 'ErpModule',
        redirect: '/erp/purchase',
        meta: { title: 'ERP管理', icon: 'ShopOutlined' },
        children: [
          {
            path: 'purchase',
            name: 'PurchaseOrder',
            component: () => import('@/views/erp/purchase/index.vue'),
            meta: { 
              title: '采购订单', 
              icon: 'ShoppingOutlined',
              permissions: ['erp:purchase:view'],
              keepAlive: true
            }
          },
          {
            path: 'sale',
            name: 'SaleOrder',
            component: () => import('@/views/erp/sale/index.vue'),
            meta: { 
              title: '销售订单', 
              icon: 'ShoppingCartOutlined',
              permissions: ['erp:sale:view'],
              keepAlive: true
            }
          },
          {
            path: 'stock',
            name: 'Stock',
            component: () => import('@/views/erp/stock/index.vue'),
            meta: { 
              title: '库存管理', 
              icon: 'ContainerOutlined',
              permissions: ['erp:stock:view'],
              keepAlive: true
            }
          }
        ]
      },
      {
        path: 'crm',
        name: 'CrmModule',
        redirect: '/crm/customer',
        meta: { title: 'CRM管理', icon: 'TeamOutlined' },
        children: [
          {
            path: 'customer',
            name: 'Customer',
            component: () => import('@/views/crm/customer/index.vue'),
            meta: { 
              title: '客户管理', 
              icon: 'TeamOutlined',
              permissions: ['crm:customer:view'],
              keepAlive: true
            }
          }
        ]
      },
      {
        path: 'system',
        name: 'SystemModule',
        redirect: '/system/user',
        meta: { title: '系统设置', icon: 'SettingOutlined' },
        children: [
          {
            path: 'user',
            name: 'SystemUser',
            component: () => import('@/views/system/user/index.vue'),
            meta: { 
              title: '用户管理', 
              icon: 'UserOutlined',
              permissions: ['system:user:view'],
              keepAlive: true
            }
          },
          {
            path: 'role',
            name: 'SystemRole',
            component: () => import('@/views/system/role/index.vue'),
            meta: { 
              title: '角色管理', 
              icon: 'SafetyOutlined',
              permissions: ['system:role:view']
            }
          },
          {
            path: 'permission',
            name: 'SystemPermission',
            component: () => import('@/views/system/permission/index.vue'),
            meta: { 
              title: '权限管理', 
              icon: 'KeyOutlined',
              permissions: ['system:permission:view']
            }
          },
          {
            path: 'menu',
            name: 'SystemMenu',
            component: () => import('@/views/system/menu/index.vue'),
            meta: { 
              title: '菜单管理', 
              icon: 'MenuOutlined',
              permissions: ['system:menu:view']
            }
          },
          {
            path: 'department',
            name: 'SystemDepartment',
            component: () => import('@/views/system/department/index.vue'),
            meta: { 
              title: '部门管理', 
              icon: 'ApartmentOutlined',
              permissions: ['system:department:view'],
              keepAlive: true
            }
          },
          {
            path: 'position',
            name: 'SystemPosition',
            component: () => import('@/views/system/position/index.vue'),
            meta: { 
              title: '岗位管理', 
              icon: 'IdcardOutlined',
              permissions: ['system:position:view'],
              keepAlive: true
            }
          }
        ]
      }
    ]
  }
]

export async function loadDynamicRoutes(): Promise<RouteRecordRaw[]> {
  const userStore = useUserStore()
  const permissions = userStore.permissions || []
  
  if (permissions.includes('*')) {
    return asyncRoutes
  }
  
  return filterRoutesByPermission(asyncRoutes)
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