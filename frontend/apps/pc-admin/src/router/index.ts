import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { setupRouterGuard } from './guard'
import { loadDynamicRoutes } from './dynamicRoutes'

// 配置NProgress
NProgress.configure({ showSpinner: false })

/**
 * 路由元信息类型定义
 */
declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    icon?: string
    requiresAuth?: boolean
    permissions?: string[]
    roles?: string[]
    keepAlive?: boolean
    hidden?: boolean
    breadcrumb?: boolean
    affix?: boolean
  }
}

/**
 * 基础路由（无需权限）
 */
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/login',
    meta: { requiresAuth: false }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false },
    beforeEnter: (to, from, next) => {
      const token = localStorage.getItem('token')
      if (token) {
        next({ path: '/' })
      } else {
        next()
      }
    }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/login/register.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无权限', requiresAuth: false }
  },
  {
    path: '/500',
    name: 'ServerError',
    component: () => import('@/views/error/500.vue'),
    meta: { title: '服务器错误', requiresAuth: false }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', requiresAuth: false }
  }
]

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
            path: 'permission-example',
            name: 'PermissionExample',
            component: () => import('@/views/system/permission-example.vue'),
            meta: { 
              title: '权限示例', 
              icon: 'CodeOutlined',
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
          },
          {
            path: 'test-environment',
            name: 'TestEnvironment',
            component: () => import('@/views/test-environment/index.vue'),
            meta: { 
              title: '测试环境管理', 
              icon: 'ExperimentOutlined',
              permissions: ['system:test-environment:view'],
              keepAlive: true
            }
          }
        ]
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [...constantRoutes],
  scrollBehavior: (to, from, savedPosition) => {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash, behavior: 'smooth' }
    return { top: 0, behavior: 'smooth' }
  }
})

export function resetRouter() {
  const newRouter = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [...constantRoutes]
  })
  ;(router as any).matcher = (newRouter as any).matcher
}

export async function setupDynamicRoutes() {
  try {
    resetRouter()
    const dynamicRoutes = await loadDynamicRoutes()
    dynamicRoutes.forEach(route => {
      router.addRoute(route)
    })
    return dynamicRoutes
  } catch (error) {
    console.error('加载动态路由失败:', error)
    return []
  }
}

setupRouterGuard(router, {
  beforeEach: async (to, from, next) => {
    NProgress.start()
    document.title = to.meta.title ? `${to.meta.title} - AI-Ready` : 'AI-Ready'
    next()
  },
  afterEach: (to, from) => {
    NProgress.done()
  },
  onError: (error) => {
    console.error('路由错误:', error)
    NProgress.done()
  }
})

export default router