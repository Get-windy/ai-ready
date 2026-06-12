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
    path: '/receive/scan',
    name: 'ReceiveScan',
    component: () => import('@/views/receive/scan.vue'),
    meta: { title: '扫码收货', showTabBar: false }
  },
  {
    path: '/receive/:id',
    name: 'ReceiveDetail',
    component: () => import('@/views/receive/detail.vue'),
    meta: { title: '收货详情', showTabBar: false }
  },
  {
    path: '/pick',
    name: 'Pick',
    component: () => import('@/views/pick/index.vue'),
    meta: { title: '拣货任务', showTabBar: true }
  },
  {
    path: '/pick/scan',
    name: 'PickScan',
    component: () => import('@/views/pick/scan.vue'),
    meta: { title: '扫码拣货', showTabBar: false }
  },
  {
    path: '/pick/ship-list',
    name: 'PickShipList',
    component: () => import('@/views/pick/ship-list.vue'),
    meta: { title: '待复核', showTabBar: false }
  },
  {
    path: '/pick/:id',
    name: 'PickDetail',
    component: () => import('@/views/pick/detail.vue'),
    meta: { title: '拣货详情', showTabBar: false }
  },
  {
    path: '/check',
    name: 'Check',
    component: () => import('@/views/check/index.vue'),
    meta: { title: '盘点任务', showTabBar: true }
  },
  {
    path: '/check/scan',
    name: 'CheckScan',
    component: () => import('@/views/check/scan.vue'),
    meta: { title: '扫码盘点', showTabBar: false }
  },
  {
    path: '/check/:id',
    name: 'CheckDetail',
    component: () => import('@/views/check/detail.vue'),
    meta: { title: '盘点详情', showTabBar: false }
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
    path: '/putaway',
    name: 'Putaway',
    component: () => import('@/views/putaway/index.vue'),
    meta: { title: '上架任务', showTabBar: true }
  },
  {
    path: '/putaway/scan',
    name: 'PutawayScan',
    component: () => import('@/views/putaway/scan.vue'),
    meta: { title: '扫码上架', showTabBar: false }
  },
  {
    path: '/putaway/:id',
    name: 'PutawayDetail',
    component: () => import('@/views/putaway/detail.vue'),
    meta: { title: '上架详情', showTabBar: false }
  },
  {
    path: '/ship',
    name: 'Ship',
    component: () => import('@/views/ship/index.vue'),
    meta: { title: '发货复核', showTabBar: true }
  },
  {
    path: '/ship/scan',
    name: 'ShipScan',
    component: () => import('@/views/ship/scan.vue'),
    meta: { title: '扫码复核', showTabBar: false }
  },
  {
    path: '/ship/:id',
    name: 'ShipDetail',
    component: () => import('@/views/ship/detail.vue'),
    meta: { title: '复核详情', showTabBar: false }
  },
  {
    path: '/move',
    name: 'Move',
    component: () => import('@/views/move/index.vue'),
    meta: { title: '移库任务', showTabBar: true }
  },
  {
    path: '/move/create',
    name: 'MoveCreate',
    component: () => import('@/views/move/create.vue'),
    meta: { title: '创建移库', showTabBar: false }
  },
  {
    path: '/move/:id',
    name: 'MoveDetail',
    component: () => import('@/views/move/detail.vue'),
    meta: { title: '移库详情', showTabBar: false }
  },
  {
    path: '/inventory',
    name: 'Inventory',
    component: () => import('@/views/inventory/index.vue'),
    meta: { title: '库存查询', showTabBar: false }
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

router.beforeEach((to, _from, next) => {
  document.title = to.meta.title as string || '智企连仓库作业'
  next()
})

export default router