<script setup lang="ts">
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import TabBar from '@/components/layout/TabBar.vue'

const userStore = useUserStore()

const tabBarItems = [
  { key: 'task', icon: 'todo-list-o', text: '任务', path: '/task' },
  { key: 'receive', icon: 'logistics', text: '收货', path: '/receive' },
  { key: 'pick', icon: 'goods-collect-o', text: '拣货', path: '/pick' },
  { key: 'check', icon: 'passed', text: '盘点', path: '/check' },
  { key: 'user', icon: 'user-o', text: '我的', path: '/user' }
]
</script>

<template>
  <div class="pda-app">
    <router-view v-slot="{ Component }">
      <keep-alive>
        <component :is="Component" />
      </keep-alive>
    </router-view>
    <TabBar :items="tabBarItems" />
  </div>
</template>

<style lang="scss">
.pda-app {
  min-height: 100vh;
  background-color: #f7f8fa;
  padding-bottom: 50px;
}
</style>