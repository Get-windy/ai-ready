<script setup lang="ts">
import { ref } from 'vue'
import TabBar from '@/components/layout/TabBar.vue'

const tabBarItems = [
  { key: 'home', icon: 'wap-home-o', text: '首页', path: '/' },
  { key: 'approval', icon: 'todo-list-o', text: '审批', path: '/approval' },
  { key: 'customer', icon: 'friends-o', text: '客户', path: '/customer' },
  { key: 'order', icon: 'orders-o', text: '订单', path: '/order' },
  { key: 'user', icon: 'user-o', text: '我的', path: '/user' }
]
</script>

<template>
  <div class="mobile-admin-app">
    <router-view v-slot="{ Component }">
      <keep-alive>
        <component :is="Component" />
      </keep-alive>
    </router-view>
    <TabBar :items="tabBarItems" />
  </div>
</template>

<style lang="scss">
.mobile-admin-app {
  min-height: 100vh;
  background-color: #f7f8fa;
  padding-bottom: 50px;
}
</style>