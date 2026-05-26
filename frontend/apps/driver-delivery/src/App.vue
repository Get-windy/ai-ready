<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'
import TabBar from '@/components/layout/TabBar.vue'

const userStore = useUserStore()

const tabBarItems = [
  { key: 'order', icon: 'todo-list-o', text: '订单', path: '/order' },
  { key: 'delivery', icon: 'logistics', text: '配送', path: '/delivery' },
  { key: 'map', icon: 'location-o', text: '地图', path: '/map' },
  { key: 'user', icon: 'user-o', text: '我的', path: '/user' }
]
</script>

<template>
  <div class="delivery-app">
    <router-view v-slot="{ Component }">
      <keep-alive>
        <component :is="Component" />
      </keep-alive>
    </router-view>
    <TabBar :items="tabBarItems" />
  </div>
</template>

<style lang="scss">
.delivery-app {
  min-height: 100vh;
  background-color: #f7f8fa;
  padding-bottom: 50px;
}
</style>