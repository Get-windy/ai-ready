import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { setupRouterGuard } from './guard'
import { loadDynamicRoutes, asyncRoutes } from './dynamicRoutes'

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