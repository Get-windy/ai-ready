<template>
  <div class="error-page">
    <a-result
      status="500"
      title="500"
      sub-title="抱歉，服务器出错了"
    >
      <template #extra>
        <a-button
          type="primary"
          @click="goHome"
        >
          返回首页
        </a-button>
        <a-button
          style="margin-left: 8px"
          @click="refresh"
        >
          刷新页面
        </a-button>
      </template>
    </a-result>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

const refresh = () => {
  location.reload()
}
</script>

<style scoped>
.error-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f0f2f5;
}
</style>