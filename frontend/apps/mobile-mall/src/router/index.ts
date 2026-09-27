import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

/**
 * 路由表（《商城App设计方案》§2.1 / §2.3）。
 *
 * 2026-09-26 改动：
 *  1. 新增 `/message`（底部第 3 个主 Tab：客服 / 消息）。
 *  2. 每条路由新增 **`meta.tab`** —— 声明自己属于哪个底部主 Tab，
 *     底部栏据此高亮（原实现是在 TabBar 里 if-else 硬编码路径，加 Tab 必漏改）。
 *  3. `meta.showTabBar` 此前**定义了但从未被读取**（底部栏一直常显），本次起真正生效。
 *     取值口径：**浏览类页面保留底部栏**（首页/分类/消息/购物车/我的 + 商品详情/分类详情/搜索，
 *     与手机淘宝一致，方便随时切 Tab）；**流程类页面隐藏**（登录/注册/确认订单/订单列表/
 *     订单详情/地址/资料），避免"下单到一半点走"。
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/home/index.vue'),
    meta: { title: '首页', showTabBar: true, requiresAuth: false, tab: 'home' }
  },
  {
    path: '/category',
    name: 'Category',
    component: () => import('@/views/category/index.vue'),
    meta: { title: '分类', showTabBar: true, requiresAuth: false, tab: 'category' }
  },
  {
    path: '/category/:id',
    name: 'CategoryDetail',
    component: () => import('@/views/category/detail.vue'),
    meta: { title: '分类详情', showTabBar: true, requiresAuth: false, tab: 'category' }
  },
  {
    path: '/message',
    name: 'Message',
    component: () => import('@/views/message/index.vue'),
    meta: { title: '消息', showTabBar: true, requiresAuth: false, tab: 'message' }
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('@/views/product/detail.vue'),
    meta: { title: '商品详情', showTabBar: true, requiresAuth: false, tab: 'home' }
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('@/views/cart/index.vue'),
    meta: { title: '购物车', showTabBar: true, requiresAuth: true, tab: 'cart' }
  },
  {
    path: '/order',
    name: 'Order',
    component: () => import('@/views/order/index.vue'),
    meta: { title: '确认订单', showTabBar: false, requiresAuth: true, tab: 'cart' }
  },
  {
    path: '/order/list',
    name: 'OrderList',
    component: () => import('@/views/order/list.vue'),
    meta: { title: '我的订单', showTabBar: false, requiresAuth: true, tab: 'user' }
  },
  {
    path: '/order/:id',
    name: 'OrderDetail',
    component: () => import('@/views/order/detail.vue'),
    meta: { title: '订单详情', showTabBar: false, requiresAuth: true, tab: 'user' }
  },
  {
    path: '/user',
    name: 'User',
    component: () => import('@/views/user/index.vue'),
    meta: { title: '我的', showTabBar: true, requiresAuth: true, tab: 'user' }
  },
  {
    path: '/user/address',
    name: 'Address',
    component: () => import('@/views/user/address.vue'),
    meta: { title: '收货地址', showTabBar: false, requiresAuth: true, tab: 'user' }
  },
  {
    path: '/user/profile',
    name: 'Profile',
    component: () => import('@/views/user/profile.vue'),
    meta: { title: '个人信息', showTabBar: false, requiresAuth: true, tab: 'user' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', showTabBar: false, requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/login/register.vue'),
    meta: { title: '注册', showTabBar: false, requiresAuth: false }
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/search/index.vue'),
    meta: { title: '搜索', showTabBar: true, requiresAuth: false, tab: 'home' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：需要登录的页面跳转到登录页
router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) || '企智连商城'

  const token = localStorage.getItem('token')
  const requiresAuth = to.meta.requiresAuth !== false

  if (requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
