<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Tabbar, TabbarItem } from 'vant'
import { useCartStore } from '@/stores/cart'

interface TabBarItem {
  key: string
  icon: string
  text: string
  path: string
}

const props = defineProps<{
  items: TabBarItem[]
}>()

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()

const active = computed(() => {
  const currentPath = route.path
  if (currentPath === '/') return 0
  if (currentPath.startsWith('/category')) return 1
  if (currentPath.startsWith('/cart')) return 2
  if (currentPath.startsWith('/user')) return 3
  const idx = props.items.findIndex(item => item.path && currentPath.startsWith(item.path) && item.path !== '/')
  return idx >= 0 ? idx : 0
})

const cartBadge = computed(() => {
  const count = cartStore.totalCount
  return count > 0 ? (count > 99 ? '99+' : String(count)) : ''
})
</script>

<template>
  <Tabbar
    :model-value="active"
    :fixed="true"
    :border="true"
    :safe-area-inset-bottom="true"
    active-color="#1988fa"
    inactive-color="#7d7e80"
    @change="(idx: number) => router.push(props.items[idx].path)"
  >
    <TabbarItem
      v-for="(item, index) in items"
      :key="item.key"
      :icon="item.icon"
      :badge="index === 2 ? cartBadge : undefined"
    >
      {{ item.text }}
    </TabbarItem>
  </Tabbar>
</template>

<style lang="scss" scoped>
:deep(.van-tabbar) {
  z-index: 999;
}
</style>
