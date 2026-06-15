<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <template #header>
        <div class="page-header">
          <div class="page-header__left">
            <span class="page-header__breadcrumb">ERP / 采购管理 / 采购退货</span>
            <h2 class="page-header__title">采购退货</h2>
          </div>
        </div>
      </template>

      <PurchaseReturnTab />
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseReturnPage' })

import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import PurchaseReturnTab from '@/views/purchase/tabs/Return.vue'

function handleError(error: any) {
  console.warn('[采购退货] 页面异常', error)
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
