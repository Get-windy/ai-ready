<template>
  <!-- 路由缓存包装器 -->
  <router-view v-slot="{ Component, route }">
    <keep-alive :include="cachedViews" :max="20">
      <component :is="Component" v-if="route.meta.keepAlive" :key="route.fullPath" />
    </keep-alive>
    <component :is="Component" v-if="!route.meta.keepAlive" :key="route.fullPath" />
  </router-view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

// 缓存的视图名称列表
const cachedViews = computed(() => {
  // 可以从store中获取需要缓存的页面列表
  return ['Dashboard', 'Customer', 'SaleOrder', 'PurchaseOrder', 'Stock']
})
</script>