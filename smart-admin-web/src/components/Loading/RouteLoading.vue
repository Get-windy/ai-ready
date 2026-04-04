<template>
  <div class="route-loading">
    <div class="loading-spinner">
      <a-spin size="large" />
      <p class="loading-text">页面加载中...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.route-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #f0f2f5;
}

.loading-spinner {
  text-align: center;
}

.loading-text {
  margin-top: 16px;
  color: #666;
}
</style>