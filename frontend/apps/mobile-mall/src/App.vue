<script setup lang="ts">
/**
 * 商城 App 根组件。
 *
 * 2026-09-26 改动：
 *  1. 主 Tab 由 4 个 → **5 个**（新增「消息」，见《商城App设计方案》§2.1）；
 *     订单按用户口径**并入「我的」**，不再单列。
 *  2. 首屏拉齐店铺配置/标签/公告（三者都是游客可达的只读接口）—— 店铺名、
 *     主题色、价格三态都依赖它们，晚加载会导致首页先按默认样式闪一下。
 *  3. 按路由 `meta.showTabBar` 决定是否显示底部栏：原来这个 meta 定义了却从未被读取，
 *     导致登录页/注册页底部也挂着 Tab（点进去会跳到需要登录的页面）。
 */
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import { useShopStore } from '@/stores/shop'
import { useMessageStore } from '@/stores/message'
import TabBar from '@/components/layout/TabBar.vue'
import { resolveShopTenantId } from '@/utils/shop'

const route = useRoute()
const userStore = useUserStore()
const cartStore = useCartStore()
const shopStore = useShopStore()
const messageStore = useMessageStore()

// 初始化商店（从 localStorage 恢复）
userStore.init()
cartStore.loadCart()

// 店铺识别：URL 参数 ?tenantId= / 上次记住的 / 环境变量兜底（详见 utils/shop.ts）
// 必须在首个请求之前执行，否则游客请求会因缺 X-Tenant-Id 被判「无法确定店铺」
resolveShopTenantId()

const tabBarItems = [
  { key: 'home', icon: 'wap-home-o', text: '首页', path: '/' },
  { key: 'category', icon: 'apps-o', text: '分类', path: '/category' },
  { key: 'message', icon: 'chat-o', text: '消息', path: '/message' },
  { key: 'cart', icon: 'shopping-cart-o', text: '购物车', path: '/cart' },
  { key: 'user', icon: 'user-o', text: '我的', path: '/user' }
]

/** 路由显式声明 showTabBar=false 的页面（登录/注册/商品详情等）不显示底部栏 */
const showTabBar = computed(() => route.meta?.showTabBar !== false)

onMounted(async () => {
  // 店铺配置/标签/公告：三者都失败也不阻塞渲染（loadX 内部已吞异常并打 warning）
  await shopStore.init()
  messageStore.load()
})
</script>

<template>
  <div class="mall-app">
    <router-view />
    <TabBar v-if="showTabBar" :items="tabBarItems" />
  </div>
</template>

<style lang="scss">
:root {
  /* 后台「店铺设置 → 主题色」下发后会被覆盖（见 stores/shop.ts） */
  --mall-primary: #1988fa;
}

.mall-app {
  min-height: 100vh;
  background-color: #f7f8fa;
  padding-bottom: 50px;
}
</style>
