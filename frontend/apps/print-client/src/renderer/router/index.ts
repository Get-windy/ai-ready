import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/index.vue'),
    meta: { title: '仪表盘' }
  },
  {
    path: '/queue',
    name: 'Queue',
    component: () => import('@/views/queue/index.vue'),
    meta: { title: '打印队列' }
  },
  {
    path: '/queue/:id',
    name: 'TaskDetail',
    component: () => import('@/views/queue/detail.vue'),
    meta: { title: '任务详情' }
  },
  {
    path: '/templates',
    name: 'Templates',
    component: () => import('@/views/templates/index.vue'),
    meta: { title: '打印模板' }
  },
  {
    path: '/templates/new',
    name: 'TemplateNew',
    component: () => import('@/views/templates/edit.vue'),
    meta: { title: '新建模板' }
  },
  {
    path: '/templates/:id/edit',
    name: 'TemplateEdit',
    component: () => import('@/views/templates/edit.vue'),
    meta: { title: '编辑模板' }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/settings/index.vue'),
    meta: { title: '设置' }
  },
  {
    path: '/printers',
    name: 'Printers',
    component: () => import('@/views/printers/index.vue'),
    meta: { title: '打印机管理' }
  },
  {
    path: '/logs',
    name: 'Logs',
    component: () => import('@/views/logs/index.vue'),
    meta: { title: '打印日志' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title} - 智企连打印客户端`
  next()
})

export default router