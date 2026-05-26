<template>
  <GlobalErrorFallback 
    :error="pageError"
    :title="pageTitle"
    :subtitle="pageSubtitle"
  />
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import GlobalErrorFallback from '@/components/ErrorBoundary/GlobalErrorFallback.vue'

const route = useRoute()
const router = useRouter()

// State
const pageError = ref<Error | null>(null)
const pageTitle = ref('页面加载失败')
const pageSubtitle = ref('抱歉，页面遇到了一些问题')

// 初始化
onMounted(() => {
  // 从路由参数获取错误信息
  const errorType = route.query.type as string
  const errorMessage = route.query.message as string
  const errorStack = route.query.stack as string
  
  // 创建错误对象
  if (errorMessage) {
    pageError.value = new Error(errorMessage)
    if (errorStack) {
      pageError.value.stack = errorStack
    }
  }
  
  // 根据错误类型设置标题
  switch (errorType) {
    case 'network':
      pageTitle.value = '网络连接失败'
      pageSubtitle.value = '请检查网络连接后重试'
      break
    case 'timeout':
      pageTitle.value = '请求超时'
      pageSubtitle.value = '服务器响应时间过长，请稍后重试'
      break
    case 'auth':
      pageTitle.value = '访问权限不足'
      pageSubtitle.value = '您可能需要重新登录或联系管理员'
      break
    case 'not-found':
      pageTitle.value = '页面不存在'
      pageSubtitle.value = '您访问的页面可能已被删除或移动'
      break
    case 'server':
      pageTitle.value = '服务器错误'
      pageSubtitle.value = '服务器暂时无法处理您的请求'
      break
    default:
      // 使用默认值
      break
  }
})
</script>

<style scoped>
/* 样式继承自 GlobalErrorFallback */
</style>