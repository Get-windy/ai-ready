<script setup lang="ts">
interface TabBarItem {
  key: string
  icon: string
  text: string
  path: string
}

defineProps<{
  items: TabBarItem[]
}>()
</script>

<template>
  <van-tabbar route :border="false" active-color="#ff976a" inactive-color="#969799">
    <van-tabbar-item
      v-for="item in items"
      :key="item.key"
      :icon="item.icon"
      :to="item.path"
    >
      {{ item.text }}
    </van-tabbar-item>
  </van-tabbar>
</template>
