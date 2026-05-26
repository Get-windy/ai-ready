import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/home/index.vue'),
    meta: { title: '首页', showTabBar: true }
  },
  {
    path: '/approval',
    name: 'Approval',
    component: () => import('@/views/approval/index.vue'),
    meta: { title: '审批中心', showTabBar: true }
  },
  {
    path: '/approval/:id',
    name: 'ApprovalDetail',
    component: () => import('@/views/approval/detail.vue'),
    meta: { title: '审批详情', showTabBar: false }
  },
  {
    path: '/customer',
    name: 'Customer',
    component: () => import('@/views/customer/index.vue'),
    meta: { title: '客户管理', showTabBar: true }
  },
  {
    path: '/customer/:id',
    name: 'CustomerDetail',
    component: () => import('@/views/customer/detail.vue'),
    meta: { title: '客户详情', showTabBar: false }
  },
  {
    path: '/customer/:id/follow-up',
    name: 'FollowUp',
    component: () => import('@/views/customer/follow-up.vue'),
    meta: { title: '跟进记录', showTabBar: false }
  },
  {
    path: '/order',
    name: 'Order',
    component: () => import('@/views/order/index.vue'),
    meta: { title: '订单管理', showTabBar: true }
  },
  {
    path: '/order/:id',
    name: 'OrderDetail',
    component: () => import('@/views/order/detail.vue'),
    meta: { title: '订单详情', showTabBar: false }
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/index.vue'),
    meta: { title: '数据看板', showTabBar: false }
  },
  {
    path: '/statistics',
    name: 'Statistics',
    component: () => import('@/views/statistics/index.vue'),
    meta: { title: '统计分析', showTabBar: false }
  },
  {
    path: '/user',
    name: 'User',
    component: () => import('@/views/user/index.vue'),
    meta: { title: '个人中心', showTabBar: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', showTabBar: false }
  },
  {
    path: '/report',
    name: 'Report',
    component: () => import('@/views/report/index.vue'),
    meta: { title: '报表查看', showTabBar: false }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/settings/index.vue'),
    meta: { title: '设置', showTabBar: false }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/profile/index.vue'),
    meta: { title: '个人信息', showTabBar: false }
  },
  {
    path: '/notification',
    name: 'Notification',
    component: () => import('@/views/notification/index.vue'),
    meta: { title: '通知中心', showTabBar: false }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title as string || '智企连管理端'
  next()
})

export default router