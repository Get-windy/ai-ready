<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { useCartStore } from '@/stores/cart'
import TabBar from '@/components/layout/TabBar.vue'

// 初始化商店（从 localStorage 恢复）
const userStore = useUserStore()
const cartStore = useCartStore()
userStore.init()
cartStore.loadCart()

const tabBarItems = [
  { key: 'home', icon: 'wap-home-o', text: '首页', path: '/' },
  { key: 'category', icon: 'apps-o', text: '分类', path: '/category' },
  { key: 'cart', icon: 'shopping-cart-o', text: '购物车', path: '/cart' },
  { key: 'user', icon: 'user-o', text: '我的', path: '/user' }
]
</script>

<template>
  <div class="mall-app">
    <router-view />
    <TabBar :items="tabBarItems" />
  </div>
</template>

<style lang="scss">
.mall-app {
  min-height: 100vh;
  background-color: #f7f8fa;
  padding-bottom: 50px;
}
</style>
