<template>
  <ErrorBoundary
    :fallback-title="fallbackTitle"
    :fallback-subtitle="fallbackSubtitle"
    @error="handleGlobalError"
  >
    <a-config-provider :locale="zhCN">
      <router-view />
    </a-config-provider>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { type ComponentPublicInstance } from 'vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import { ErrorBoundary } from '@/components/ErrorBoundary'
import { reportError } from '@/utils/errorReporter'
import { message } from 'ant-design-vue'

// 错误提示文本
const fallbackTitle = '全局异常'
const fallbackSubtitle = '应用遇到了未处理的异常，请尝试刷新页面'

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
