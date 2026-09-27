<script setup lang="ts">
/**
 * 底部主 Tab 栏（5 个主 Tab，见《商城App设计方案》§2.1）。
 *
 * 2026-09-26 重构两点：
 *  1. **角标按 key 判断**：原实现把购物车角标写死在 `index === 2`，
 *     一旦 Tab 顺序或个数变化（本次就新增了"消息"）角标会挂到错的 Tab 上。
 *  2. **高亮按路由 `meta.tab`**：原实现用 if-else 硬编码路径，加一个 Tab 就要改一处。
 *     现在由路由声明自己属于哪个主 Tab，二级页也能正确点亮父 Tab。
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Tabbar, TabbarItem } from 'vant'
import { useCartStore } from '@/stores/cart'
import { useMessageStore } from '@/stores/message'

export interface TabBarItem {
  key: string
  icon: string
  text: string
  path: string
}

const props = defineProps<{ items: TabBarItem[] }>()

const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const messageStore = useMessageStore()

/**
 * 底部吸底样式 —— 用**行内样式**下发。
 *
 * 背景：`:fixed="true"` 已经给元素加了 `van-tabbar--fixed`，Vant 样式表里也确有
 * `.van-tabbar--fixed{position:fixed;bottom:0;left:0}`，但真机上该元素的 computed
 * `position` 始终是 **`relative`**（首页/分类/消息三页实测一致），Tab 栏因此被排在
 * 内容之后 —— 必须滚到页面底部才看得见，完全不是"底部主 Tab"该有的样子。
 * 用组件内的 `:deep(.van-tabbar)` 覆盖**也无效** —— 编译后是
 * `.van-tabbar[data-v-x]`，与 `.van-tabbar--fixed` **特异度相同(0,2,0)**，
 * 而 Vant 的样式在运行时后注入 ⇒ 同分后到者胜，仍被压成 relative。
 *
 * 行内样式是这里能拿到的最高优先级手段（且不依赖 Vant 样式注入链路），
 * 语义与 Vant 的 `--fixed` 完全一致，只是保证它真的生效。
 * 配套：`App.vue` 的 `.mall-app` 有 `padding-bottom: 50px`，内容不会被压住。
 * 实测（首页/分类/消息）：position=fixed、top=794/bottom=844（视口 844）。
 */
const FIXED_STYLE = {
  position: 'fixed',
  left: '0',
  right: '0',
  bottom: '0',
  zIndex: '999'
} as const

/** 当前激活的主 Tab：优先取路由声明的 meta.tab，其次按 path 前缀兜底 */
const activeKey = computed(() => {
  const declared = route.meta?.tab as string | undefined
  if (declared && props.items.some(i => i.key === declared)) return declared
  const hit = props.items.find(i => i.path !== '/' && route.path.startsWith(i.path))
  return hit ? hit.key : 'home'
})

const activeIndex = computed(() => {
  const idx = props.items.findIndex(i => i.key === activeKey.value)
  return idx >= 0 ? idx : 0
})

function badgeOf(key: string): string | undefined {
  if (key === 'cart') {
    const n = cartStore.totalCount
    return n > 0 ? (n > 99 ? '99+' : String(n)) : undefined
  }
  if (key === 'message') {
    const n = messageStore.unreadCount
    return n > 0 ? (n > 99 ? '99+' : String(n)) : undefined
  }
  return undefined
}
</script>

<template>
  <Tabbar
    :model-value="activeIndex"
    :fixed="true"
    :border="true"
    :safe-area-inset-bottom="true"
    active-color="var(--mall-primary, #1988fa)"
    inactive-color="#7d7e80"
    :style="FIXED_STYLE"
    @change="(idx: number) => router.push(props.items[idx].path)"
  >
    <TabbarItem
      v-for="item in items"
      :key="item.key"
      :icon="item.icon"
      :badge="badgeOf(item.key)"
    >
      {{ item.text }}
    </TabbarItem>
  </Tabbar>
</template>

<style lang="scss" scoped>
/**
 * 底部固定（2026-09-26 实测修正）。
 *
 * `:fixed="true"` 会给元素加上 `van-tabbar--fixed` 类，Vant 的样式表里也确实有
 * `.van-tabbar--fixed{position:fixed;bottom:0;left:0}` —— 但真机上该元素的
 * **computed position 仍是 `relative`**，结果 Tab 栏被排在内容之后，
 * 要滚到页面底部才看得见（首页/分类/消息三页均实测如此），完全不像"底部主 Tab"。
 *
 * 与其继续追 Vant 的样式注入链路（dev 下 `document.styleSheets` 里查不到该规则），
 * 这里直接把定位写死：语义与 Vant 的 `--fixed` 完全一致，只是保证它**真的生效**。
 * 配套：`App.vue` 的 `.mall-app` 有 `padding-bottom: 50px`，内容不会被压住。
 */
:deep(.van-tabbar) {
  z-index: 999;
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
}
</style>
