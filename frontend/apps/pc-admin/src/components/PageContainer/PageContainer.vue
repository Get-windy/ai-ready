<template>
  <div
    ref="containerRef"
    :class="['page-container', {
      'page-container--full-height': fullHeight,
      'page-container--padded': bodyPadding
    }]"
  >
    <!-- 网络重连横幅 -->
    <ReconnectBanner />

    <!-- 顶部区域：面包屑/标题 -->
    <slot name="header">
      <div v-if="title || $slots.headerContent" class="page-container__header">
        <div class="page-container__header-left">
          <h2 v-if="title" class="page-container__title">{{ title }}</h2>
          <slot name="headerContent" />
        </div>
        <div class="page-container__header-right">
          <slot name="headerExtra" />
        </div>
      </div>
    </slot>

    <!-- 搜索/筛选区 -->
    <div v-if="$slots.filter" class="page-container__filter">
      <slot name="filter" />
    </div>

    <!-- 核心内容区：flex-grow 占据剩余所有空间 -->
    <div class="page-container__body">
      <slot />
    </div>

    <!-- 底部区域：分页等 -->
    <div v-if="$slots.footer" class="page-container__footer">
      <slot name="footer" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import ReconnectBanner from '@/components/ReconnectBanner/ReconnectBanner.vue'

defineProps<{
  title?: string
  fullHeight?: boolean
  bodyPadding?: boolean
}>()

const containerRef = ref<HTMLDivElement>()

// 窗口 resize 时触发自定义事件，让子组件（如 TableList）可以重新计算高度
const emit = defineEmits<{
  resize: [height: number]
}>()

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  if (containerRef.value) {
    resizeObserver = new ResizeObserver((entries) => {
      for (const entry of entries) {
        emit('resize', entry.contentRect.height)
      }
    })
    resizeObserver.observe(containerRef.value)
  }
})

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
})
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  overflow: hidden; /* 禁止容器本身滚动，让子区域内部滚动 */
  background: #fff;
}

/* 全高模式：填满整个视口高度 */
.page-container--full-height {
  height: 100vh;
}

/* 内边距模式 */
.page-container--padded {
  padding: 16px;
}

/* ---- 顶部标题/面包屑 ---- */
.page-container__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  min-height: 48px;
}

.page-container__header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-container__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
  line-height: 1.4;
}

.page-container__header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* ---- 搜索/筛选区 ---- */
.page-container__filter {
  flex-shrink: 0;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
}

/* ---- 核心内容区 ---- */
.page-container__body {
  flex: 1;
  overflow: hidden; /* 内容区本身不滚动，让内部表格控制滚动 */
  display: flex;
  flex-direction: column;
  min-height: 0; /* flex 子项收缩所需 */
}

/* ---- 底部栏 ---- */
.page-container__footer {
  flex-shrink: 0;
  background: #fff;
  border-top: 1px solid #f0f0f0;
}
</style>
