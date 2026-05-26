import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/task'
  },
  {
    path: '/task',
    name: 'Task',
    component: () => import('@/views/task/index.vue'),
    meta: { title: '任务列表', showTabBar: true }
  },
  {
    path: '/task/:id',
    name: 'TaskDetail',
    component: () => import('@/views/task/detail.vue'),
    meta: { title: '任务详情', showTabBar: false }
  },
  {
    path: '/receive',
    name: 'Receive',
    component: () => import('@/views/receive/index.vue'),
    meta: { title: '收货任务', showTabBar: true }
  },
  {
    path: '/receive/:id',
    name: 'ReceiveDetail',
    component: () => import('@/views/receive/detail.vue'),
    meta: { title: '收货详情', showTabBar: false }
  },
  {
    path: '/receive/scan',
    name: 'ReceiveScan',
    component: () => import('@/views/receive/scan.vue'),
    meta: { title: '扫码收货', showTabBar: false }
  },
  {
    path: '/pick',
    name: 'Pick',
    component: () => import('@/views/pick/index.vue'),
    meta: { title: '拣货任务', showTabBar: true }
  },
  {
    path: '/pick/:id',
    name: 'PickDetail',
    component: () => import('@/views/pick/detail.vue'),
    meta: { title: '拣货详情', showTabBar: false }
  },
  {
    path: '/pick/scan',
    name: 'PickScan',
    component: () => import('@/views/pick/scan.vue'),
    meta: { title: '扫码拣货', showTabBar: false }
  },
  {
    path: '/check',
    name: 'Check',
    component: () => import('@/views/check/index.vue'),
    meta: { title: '盘点任务', showTabBar: true }
  },
  {
    path: '/check/:id',
    name: 'CheckDetail',
    component: () => import('@/views/check/detail.vue'),
    meta: { title: '盘点详情', showTabBar: false }
  },
  {
    path: '/check/scan',
    name: 'CheckScan',
    component: () => import('@/views/check/scan.vue'),
    meta: { title: '扫码盘点', showTabBar: false }
  },
  {
    path: '/location',
    name: 'Location',
    component: () => import('@/views/location/index.vue'),
    meta: { title: '库位管理', showTabBar: false }
  },
  {
    path: '/batch',
    name: 'Batch',
    component: () => import('@/views/batch/index.vue'),
    meta: { title: '批次管理', showTabBar: false }
  },
  {
    path: '/batch/:id',
    name: 'BatchDetail',
    component: () => import('@/views/batch/detail.vue'),
    meta: { title: '批次详情', showTabBar: false }
  },
  {
    path: '/batch/:id/check',
    name: 'BatchCheck',
    component: () => import('@/views/batch/check.vue'),
    meta: { title: '质量检测', showTabBar: false }
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
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title as string || '智企连仓库作业'
  next()
})

export default router