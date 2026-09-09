<template>
  <ErrorBoundary>
    <PageContainer title="预收款单">
      <a-result
        status="404"
        :title="'预收款单已迁移至「预收款单-历史」'"
        sub-title="预收款单统一入口为 finance/advance-receipt，本页为兼容重定向。"
      >
        <template #extra>
          <a-button type="primary" @click="router.replace('/finance/advance-receipt/index')">
            前往预收款单-历史
          </a-button>
        </template>
      </a-result>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'

const router = useRouter()
</script>
