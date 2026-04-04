/**
 * 面包屑组件
 */
<template>
  <a-breadcrumb class="breadcrumb">
    <a-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="item.path">
      <router-link v-if="index < breadcrumbs.length - 1" :to="item.path">
        {{ item.title }}
      </router-link>
      <span v-else>{{ item.title }}</span>
    </a-breadcrumb-item>
  </a-breadcrumb>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'

interface Breadcrumb {
  path: string
  title: string
}

const route = useRoute()
const breadcrumbs = ref<Breadcrumb[]>([])

const getBreadcrumbs = () => {
  const matched = route.matched.filter(item => item.meta && item.meta.title)
  
  // 添加首页
  const result: Breadcrumb[] = [
    { path: '/dashboard', title: '首页' }
  ]
  
  matched.forEach(item => {
    if (item.meta?.title && item.meta.title !== '首页') {
      result.push({
        path: item.path,
        title: item.meta.title as string
      })
    }
  })
  
  breadcrumbs.value = result
}

watch(
  () => route.path,
  () => getBreadcrumbs(),
  { immediate: true }
)
</script>

<style scoped>
.breadcrumb {
  margin-left: 16px;
}
</style>