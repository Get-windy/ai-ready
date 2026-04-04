<template>
  <div class="route-error">
    <a-result status="error" title="页面加载失败" sub-title="请刷新页面重试">
      <template #extra>
        <a-button type="primary" @click="reload">刷新页面</a-button>
        <a-button @click="goHome">返回首页</a-button>
      </template>
    </a-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const reload = () => {
  window.location.reload()
}

const goHome = () => {
  router.push('/')
}
</script>

<style scoped>
.route-error {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #f0f2f5;
}
</style>