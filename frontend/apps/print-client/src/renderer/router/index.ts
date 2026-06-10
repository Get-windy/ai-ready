import { createRouter, createWebHashHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue'),
    meta: { title: '配置客户端', requiresAuth: true }
  },
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/index.vue'),
    meta: { title: '仪表盘', requiresAuth: true }
  },
  {
    path: '/queue',
    name: 'Queue',
    component: () => import('@/views/queue/index.vue'),
    meta: { title: '打印队列', requiresAuth: true }
  },
  {
    path: '/queue/:id',
    name: 'TaskDetail',
    component: () => import('@/views/queue/detail.vue'),
    meta: { title: '任务详情', requiresAuth: true }
  },
  {
    path: '/templates',
    name: 'Templates',
    component: () => import('@/views/templates/index.vue'),
    meta: { title: '打印模板', requiresAuth: true }
  },
  {
    path: '/templates/new',
    name: 'TemplateNew',
    component: () => import('@/views/templates/edit.vue'),
    meta: { title: '新建模板', requiresAuth: true }
  },
  {
    path: '/templates/:id/edit',
    name: 'TemplateEdit',
    component: () => import('@/views/templates/edit.vue'),
    meta: { title: '编辑模板', requiresAuth: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/settings/index.vue'),
    meta: { title: '设置', requiresAuth: true }
  },
  {
    path: '/printers',
    name: 'Printers',
    component: () => import('@/views/printers/index.vue'),
    meta: { title: '打印机管理', requiresAuth: true }
  },
  {
    path: '/logs',
    name: 'Logs',
    component: () => import('@/views/logs/index.vue'),
    meta: { title: '打印日志', requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

// ── 路由守卫：检查登录状态 ──────────────────────────
router.beforeEach(async (to, from, next) => {
  document.title = `${to.meta.title || ''} - 智企连打印客户端`

  const requiresAuth = to.meta.requiresAuth !== false

  if (requiresAuth) {
    try {
      const authState = await window.electronAPI.auth.getAuthState()
      if (!authState.isLoggedIn) {
        next('/login')
        return
      }
    } catch {
      next('/login')
      return
    }
  }

  next()
})

export default router
