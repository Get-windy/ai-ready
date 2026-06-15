<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="page-header">
          <div class="page-header__left">
            <span class="page-header__breadcrumb">ERP / 销售管理 / 销售出库</span>
            <h2 class="page-header__title">销售出库</h2>
          </div>
        </div>
      </template>

      <SaleOutboundTab />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
defineOptions({ name: 'SaleOutboundPage' })

import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SaleOutboundTab from '@/views/sale/tabs/Outbound.vue'

function handleError(error: any) {
  console.warn('[销售出库] 页面异常', error)
}
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}
.page-header__left {
  display: flex;
  flex-direction: column;
}
.page-header__breadcrumb {
  font-size: 13px;
  color: #999;
  margin-bottom: 4px;
}
.page-header__title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1f1f1f;
}
</style>
