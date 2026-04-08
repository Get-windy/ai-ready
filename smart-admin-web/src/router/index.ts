import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

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
 * 路由懒加载工厂函数
 * 添加loading状态和错误处理
 */
const lazyLoad = (componentPath: string) => {
  return () => ({
    component: import(/* webpackChunkName: "[request]" */ `@/views/${componentPath}`),
    loading: () => import('@/components/Loading/RouteLoading.vue'),
    error: () => import('@/components/Loading/RouteError.vue'),
    delay: 200,
    timeout: 30000
  })
}

/**
 * 基础路由（无需权限）
 */
export const constantRoutes: RouteRecordRaw[] = [
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
      // 仪表盘
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
      
      // ========== ERP模块 ==========
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
            path: 'purchase/detail/:id?',
            name: 'PurchaseOrderDetail',
            component: () => import('@/views/erp/purchase/detail.vue'),
            meta: { 
              title: '采购订单详情',
              hidden: true,
              permissions: ['erp:purchase:view']
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
            path: 'sale/detail/:id?',
            name: 'SaleOrderDetail',
            component: () => import('@/views/erp/sale/detail.vue'),
            meta: { 
              title: '销售订单详情',
              hidden: true,
              permissions: ['erp:sale:view']
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
          },
          {
            path: 'stock/inbound',
            name: 'InboundOrder',
            component: () => import('@/views/erp/stock/inbound.vue'),
            meta: { 
              title: '入库管理',
              permissions: ['erp:stock:inbound']
            }
          },
          {
            path: 'stock/outbound',
            name: 'OutboundOrder',
            component: () => import('@/views/erp/stock/outbound.vue'),
            meta: { 
              title: '出库管理',
              permissions: ['erp:stock:outbound']
            }
          },
          {
            path: 'warehouse',
            name: 'Warehouse',
            component: () => import('@/views/erp/warehouse/index.vue'),
            meta: { 
              title: '仓库管理', 
              icon: 'HomeOutlined',
              permissions: ['erp:warehouse:view']
            }
          }
        ]
      },
      
      // ========== CRM模块 ==========
      {
        path: 'crm',
        name: 'CrmModule',
        redirect: '/crm/customer',
        meta: { title: 'CRM管理', icon: 'TeamOutlined' },
        children: [
          {
            path: 'lead',
            name: 'Lead',
            component: () => import('@/views/crm/lead/index.vue'),
            meta: { 
              title: '线索管理', 
              icon: 'UserAddOutlined',
              permissions: ['crm:lead:view'],
              keepAlive: true
            }
          },
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
          },
          {
            path: 'customer/detail/:id?',
            name: 'CustomerDetail',
            component: () => import('@/views/crm/customer/detail.vue'),
            meta: { 
              title: '客户详情',
              hidden: true,
              permissions: ['crm:customer:view']
            }
          },
          {
            path: 'opportunity',
            name: 'Opportunity',
            component: () => import('@/views/crm/opportunity/index.vue'),
            meta: { 
              title: '商机管理', 
              icon: 'AimOutlined',
              permissions: ['crm:opportunity:view']
            }
          },
          {
            path: 'activity',
            name: 'Activity',
            component: () => import('@/views/crm/activity/index.vue'),
            meta: { 
              title: '跟进记录', 
              icon: 'FormOutlined',
              permissions: ['crm:activity:view']
            }
          }
        ]
      },
      
      // ========== 系统设置 ==========
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
            path: 'dept',
            name: 'SystemDept',
            component: () => import('@/views/system/dept/index.vue'),
            meta: { 
              title: '部门管理', 
              icon: 'ApartmentOutlined',
              permissions: ['system:dept:view']
            }
          },
          {
            path: 'dict',
            name: 'SystemDict',
            component: () => import('@/views/system/dict/index.vue'),
            meta: { 
              title: '字典管理', 
              icon: 'BookOutlined',
              permissions: ['system:dict:view']
            }
          },
          {
            path: 'log',
            name: 'SystemLog',
            component: () => import('@/views/system/log/index.vue'),
            meta: { 
              title: '操作日志', 
              icon: 'FileTextOutlined',
              permissions: ['system:log:view']
            }
          }
        ]
      },
      
      // ========== 个人中心 ==========
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/index.vue'),
        meta: { 
          title: '个人中心',
          hidden: true
        }
      },
      {
        path: 'password',
        name: 'ChangePassword',
        component: () => import('@/views/profile/password.vue'),
        meta: { 
          title: '修改密码',
          hidden: true
        }
      }
    ]
  }
]

/**
 * 创建路由实例
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [...constantRoutes, ...asyncRoutes],
  scrollBehavior: (to, from, savedPosition) => {
    if (savedPosition) {
      return savedPosition
    }
    if (to.hash) {
      return { el: to.hash, behavior: 'smooth' }
    }
    return { top: 0, behavior: 'smooth' }
  }
})

/**
 * 重置路由
 * 用于动态路由重新加载
 */
export function resetRouter() {
  const newRouter = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [...constantRoutes, ...asyncRoutes]
  })
  ;(router as any).matcher = (newRouter as any).matcher
}

/**
 * 路由守卫
 */
router.beforeEach(async (to, from, next) => {
  // 开始进度条
  NProgress.start()
  
  const token = localStorage.getItem('token')
  const userStore = useUserStore()

  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - AI-Ready` : 'AI-Ready'

  // 不需要认证的页面直接放行
  if (to.meta.requiresAuth === false) {
    next()
    return
  }

  // 未登录，跳转登录页
  if (!token) {
    next({ 
      path: '/login', 
      query: { redirect: to.fullPath } 
    })
    NProgress.done()
    return
  }

  // 已登录但用户信息未加载
  if (!userStore.userId) {
    try {
      await userStore.getUserInfo()
    } catch (error) {
      // Token失效，清除并跳转登录
      userStore.logout()
      next({ 
        path: '/login', 
        query: { redirect: to.fullPath } 
      })
      NProgress.done()
      return
    }
  }

  // 权限检查
  const permissions = to.meta.permissions as string[] | undefined
  if (permissions && permissions.length > 0) {
    const hasPermission = permissions.some(p => userStore.permissions.includes(p))
    if (!hasPermission) {
      next({ path: '/403' })
      NProgress.done()
      return
    }
  }

  next()
})

router.afterEach((to, from) => {
  // 结束进度条
  NProgress.done()
})

router.onError((error) => {
  console.error('路由错误:', error)
  NProgress.done()
})

export default router

// 导入类型（需要放在文件末尾避免循环依赖）
import { useUserStore } from '@/stores/user'