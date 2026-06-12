import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/order'
  },
  {
    path: '/order',
    name: 'Order',
    component: () => import('@/views/order/index.vue'),
    meta: { title: '待配送订单', showTabBar: true }
  },
  {
    path: '/order/:id',
    name: 'OrderDetail',
    component: () => import('@/views/order/detail.vue'),
    meta: { title: '订单详情', showTabBar: false }
  },
  {
    path: '/delivery',
    name: 'Delivery',
    component: () => import('@/views/delivery/index.vue'),
    meta: { title: '配送任务', showTabBar: true }
  },
  {
    path: '/delivery/:id',
    name: 'DeliveryDetail',
    component: () => import('@/views/delivery/detail.vue'),
    meta: { title: '配送详情', showTabBar: false }
  },
  {
    path: '/delivery/:id/sign',
    name: 'Sign',
    component: () => import('@/views/delivery/sign.vue'),
    meta: { title: '签收', showTabBar: false }
  },
  {
    path: '/delivery/:id/collect',
    name: 'Collect',
    component: () => import('@/views/delivery/collect.vue'),
    meta: { title: '收款', showTabBar: false }
  },
  {
    path: '/map',
    name: 'Map',
    component: () => import('@/views/map/index.vue'),
    meta: { title: '配送地图', showTabBar: true }
  },
  {
    path: '/route',
    name: 'Route',
    component: () => import('@/views/route/index.vue'),
    meta: { title: '路线优化', showTabBar: false }
  },
  {
    path: '/navigation',
    name: 'NavigationNew',
    component: () => import('@/views/map/navigation.vue'),
    meta: { title: '导航', showTabBar: false }
  },
  {
    path: '/sign',
    name: 'SignNew',
    component: () => import('@/views/sign/index.vue'),
    meta: { title: '签收确认', showTabBar: false }
  },
  {
    path: '/map/navigation/:id',
    name: 'Navigation',
    component: () => import('@/views/map/navigation.vue'),
    meta: { title: '导航', showTabBar: false }
  },
  {
    path: '/user',
    name: 'User',
    component: () => import('@/views/user/index.vue'),
    meta: { title: '个人中心', showTabBar: true }
  },
  {
    path: '/user/history',
    name: 'History',
    component: () => import('@/views/user/history.vue'),
    meta: { title: '配送历史', showTabBar: false }
  },
  {
    path: '/user/statistics',
    name: 'Statistics',
    component: () => import('@/views/user/statistics.vue'),
    meta: { title: '配送统计', showTabBar: false }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', showTabBar: false }
  },
  {
    path: '/inspection',
    name: 'Inspection',
    component: () => import('@/views/inspection/index.vue'),
    meta: { title: '出车验车', showTabBar: false }
  },
  {
    path: '/binding',
    name: 'Binding',
    component: () => import('@/views/binding/index.vue'),
    meta: { title: '车辆绑定', showTabBar: false }
  },
  {
    path: '/handover',
    name: 'Handover',
    component: () => import('@/views/handover/index.vue'),
    meta: { title: '交车', showTabBar: false }
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title as string || '智企连配送'
  next()
})

export default router