<template>
  <div class="placeholder-page">
    <a-result
      status="info"
      :title="pageTitle"
      sub-title="该功能正在开发中，敬请期待"
    >
      <template #extra>
        <a-button type="primary" @click="goBack">返回</a-button>
      </template>
    </a-result>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const pageTitle = computed(() => {
  const title = route.meta?.title as string | undefined
  return title ? `"${title}" 功能开发中` : '功能开发中'
})

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}
</script>

<style scoped>
.placeholder-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 60vh;
}
</style>
