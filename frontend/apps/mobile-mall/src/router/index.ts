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
    path: '/category',
    name: 'Category',
    component: () => import('@/views/category/index.vue'),
    meta: { title: '分类', showTabBar: true }
  },
  {
    path: '/category/:id',
    name: 'CategoryDetail',
    component: () => import('@/views/category/detail.vue'),
    meta: { title: '分类详情', showTabBar: false }
  },
  {
    path: '/product/:id',
    name: 'ProductDetail',
    component: () => import('@/views/product/detail.vue'),
    meta: { title: '商品详情', showTabBar: false }
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('@/views/cart/index.vue'),
    meta: { title: '购物车', showTabBar: true }
  },
  {
    path: '/order',
    name: 'Order',
    component: () => import('@/views/order/index.vue'),
    meta: { title: '确认订单', showTabBar: false }
  },
  {
    path: '/order/list',
    name: 'OrderList',
    component: () => import('@/views/order/list.vue'),
    meta: { title: '我的订单', showTabBar: false }
  },
  {
    path: '/order/:id',
    name: 'OrderDetail',
    component: () => import('@/views/order/detail.vue'),
    meta: { title: '订单详情', showTabBar: false }
  },
  {
    path: '/user',
    name: 'User',
    component: () => import('@/views/user/index.vue'),
    meta: { title: '个人中心', showTabBar: true }
  },
  {
    path: '/user/address',
    name: 'Address',
    component: () => import('@/views/user/address.vue'),
    meta: { title: '收货地址', showTabBar: false }
  },
  {
    path: '/user/profile',
    name: 'Profile',
    component: () => import('@/views/user/profile.vue'),
    meta: { title: '个人信息', showTabBar: false }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', showTabBar: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/login/register.vue'),
    meta: { title: '注册', showTabBar: false }
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('@/views/search/index.vue'),
    meta: { title: '搜索', showTabBar: false }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title as string || '智企连商城'
  next()
})

export default router