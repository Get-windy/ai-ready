<template>
  <ErrorBoundary 
    :fallback-title="全局异常"
    :fallback-subtitle="应用遇到了未处理的异常，请尝试刷新页面"
    @error="handleGlobalError"
  >
    <a-config-provider :locale="zhCN">
      <router-view v-slot="{ Component, route }">
        <keep-alive :include="cachedViews" :max="maxCacheSize">
          <component 
            :is="Component" 
            v-if="route.meta.keepAlive" 
            :key="route.fullPath" 
          />
        </keep-alive>
        <component 
          :is="Component" 
          v-if="!route.meta.keepAlive" 
          :key="route.fullPath" 
        />
      </router-view>
    </a-config-provider>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, type ComponentPublicInstance } from 'vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import { useTagsViewStore } from '@/stores/tagsView'
import { ErrorBoundary } from '@/components/ErrorBoundary'
import { reportError } from '@/utils/errorReporter'
import { message } from 'ant-design-vue'

// 缓存配置
const maxCacheSize = 15 // 最大缓存组件数量

// 获取缓存的视图名称列表
const tagsViewStore = useTagsViewStore()
const cachedViews = computed(() => {
  // 从 store 中获取需要缓存的页面列表
  return tagsViewStore.cachedViews
})

// 全局错误处理
const handleGlobalError = (error: Error, instance: ComponentPublicInstance, info: string) => {
  console.error('[App Global Error]', error, info)
  
  // 上报全局错误
  reportError({
    type: 'global',
    error,
    componentInfo: info,
    timestamp: new Date().toISOString(),
    componentInstance: instance.$options?.name || 'Unknown'
  })
  
  message.error('页面出现异常，已自动捕获')
}

// 定期清理缓存（防止内存占用过大）
let cleanupTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  // 每30分钟清理一次过期缓存
  cleanupTimer = setInterval(() => {
    tagsViewStore.cleanupCache()
  }, 30 * 60 * 1000)
})

onUnmounted(() => {
  if (cleanupTimer) {
    clearInterval(cleanupTimer)
  }
})
</script>

<style>
#app {
  width: 100%;
  height: 100%;
}

/* 页面切换动画 */
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: opacity 0.3s, transform 0.3s;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(-10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(10px);
}
</style>