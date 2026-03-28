import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
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
        meta: { title: '工作台', icon: 'DashboardOutlined' }
      },
      // ERP模块
      {
        path: 'erp/purchase',
        name: 'PurchaseOrder',
        component: () => import('@/views/erp/purchase/index.vue'),
        meta: { title: '采购订单', icon: 'ShoppingOutlined' }
      },
      {
        path: 'erp/sale',
        name: 'SaleOrder',
        component: () => import('@/views/erp/sale/index.vue'),
        meta: { title: '销售订单', icon: 'ShoppingCartOutlined' }
      },
      {
        path: 'erp/stock',
        name: 'Stock',
        component: () => import('@/views/erp/stock/index.vue'),
        meta: { title: '库存管理', icon: 'ContainerOutlined' }
      },
      // CRM模块
      {
        path: 'crm/lead',
        name: 'Lead',
        component: () => import('@/views/crm/lead/index.vue'),
        meta: { title: '线索管理', icon: 'UserAddOutlined' }
      },
      {
        path: 'crm/customer',
        name: 'Customer',
        component: () => import('@/views/crm/customer/index.vue'),
        meta: { title: '客户管理', icon: 'TeamOutlined' }
      },
      // 系统设置
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/user/index.vue'),
        meta: { title: '用户管理', icon: 'UserOutlined' }
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/views/system/role/index.vue'),
        meta: { title: '角色管理', icon: 'SafetyOutlined' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const token = userStore.token

  if (to.meta.requiresAuth !== false && !token) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router