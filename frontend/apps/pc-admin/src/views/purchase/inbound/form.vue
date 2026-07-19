<template>
  <ErrorBoundary @error="handleError">
    <PageContainer title="采购入库单">
      <template #headerExtra>
        <a-button @click="goBack">返回列表</a-button>
      </template>
      <a-result
        status="info"
        title="采购入库单表单页开发中"
        sub-title="该表单页面正在开发中，请先在列表页中操作"
      >
        <template #extra>
          <a-button type="primary" @click="goBack">返回列表</a-button>
        </template>
      </a-result>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { useRouter } from 'vue-router'

const router = useRouter()

function handleError() {
  router.push('/purchase/inbound')
}

function goBack() {
  router.push('/purchase/inbound')
}
</script>
