<script setup lang="ts">
/**
 * 顶部二级 Tab（吸顶、横滑、选中项自动居中）—— 《商城App设计方案》§2.2。
 *
 * 范式取自手机淘宝首页顶部那一行（关注/推荐/闪购/…）：
 *  · 横向可滑，选中项**自动滚到可视区居中**；
 *  · 选中态 = 主题色粗体 + 2px 下划线（下划线宽度跟随文字）；
 *  · 配合 `van-sticky` 在页面滚动时吸顶；
 *  · 只做"切换"，内容区由父组件按 `modelValue` 渲染 —— 切 Tab **不重建页面**
 *    （父页用 `v-show` 或 keep-alive，各自保留滚动位置）。
 *
 * 与 `van-tabs` 的区别：van-tabs 自带内容区与懒加载，本组件只要表头，
 * 因为首页/分类页的每个 Tab 各有独立的列表状态（分页游标、下拉刷新），
 * 交给父组件掌控更简单。
 */
import { ref, watch, nextTick, onMounted } from 'vue'

export interface TopTabItem {
  key: string
  name: string
}

const props = withDefaults(defineProps<{
  tabs: TopTabItem[]
  modelValue: string
  /** 是否吸顶（默认吸顶） */
  sticky?: boolean
  /** 选中色（默认取 CSS 变量 --mall-primary，回退到蓝色） */
  activeColor?: string
}>(), {
  sticky: true,
  activeColor: ''
})

const emit = defineEmits<{
  'update:modelValue': [key: string]
  change: [key: string]
}>()

const scrollerRef = ref<HTMLElement | null>(null)
const itemRefs = ref<Record<string, HTMLElement | null>>({})

function setItemRef(key: string) {
  return (el: any) => {
    itemRefs.value[key] = el as HTMLElement | null
  }
}

/** 把选中项滚到容器中部（淘宝式"选中自动居中"） */
async function centerActive(key: string) {
  await nextTick()
  const scroller = scrollerRef.value
  const el = itemRefs.value[key]
  if (!scroller || !el) return
  const target = el.offsetLeft - scroller.clientWidth / 2 + el.clientWidth / 2
  scroller.scrollTo({ left: Math.max(0, target), behavior: 'smooth' })
}

function select(key: string) {
  if (key === props.modelValue) return
  emit('update:modelValue', key)
  emit('change', key)
}

watch(() => props.modelValue, (k) => centerActive(k))
watch(() => props.tabs.length, () => centerActive(props.modelValue))
onMounted(() => centerActive(props.modelValue))

defineExpose({ centerActive })
</script>

<template>
  <div class="top-tabs" :class="{ 'is-sticky': sticky }">
    <div ref="scrollerRef" class="top-tabs__scroller">
      <div
        v-for="tab in tabs"
        :key="tab.key"
        :ref="setItemRef(tab.key)"
        class="top-tabs__item"
        :class="{ 'is-active': tab.key === modelValue }"
        :style="tab.key === modelValue && activeColor ? { color: activeColor, borderBottomColor: activeColor } : undefined"
        @click="select(tab.key)"
      >
        {{ tab.name }}
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.top-tabs {
  background: #fff;
  border-bottom: 1px solid #f2f3f5;

  &.is-sticky {
    position: sticky;
    top: 0;
    z-index: 10;
  }
}

.top-tabs__scroller {
  display: flex;
  align-items: center;
  gap: 20px;
  overflow-x: auto;
  padding: 10px 12px;
  /* 隐藏横向滚动条（移动端视觉） */
  scrollbar-width: none;
  -ms-overflow-style: none;
  &::-webkit-scrollbar { display: none; }
}

.top-tabs__item {
  flex: 0 0 auto;
  font-size: 15px;
  color: #646566;
  padding: 4px 2px;
  border-bottom: 2px solid transparent;
  transition: color 0.15s ease;

  &.is-active {
    color: var(--mall-primary, #1988fa);
    border-bottom-color: var(--mall-primary, #1988fa);
    font-weight: 600;
    font-size: 16px;
  }
}
</style>
