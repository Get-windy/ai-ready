<script setup lang="ts">
defineProps<{
  description?: string
}>()
</script>

<template>
  <div class="empty">
    <div class="empty-icon">
      <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
        <rect x="8" y="16" width="48" height="32" rx="4" stroke="#dcdfe6" stroke-width="2" fill="#f5f7fa"/>
        <line x1="20" y1="28" x2="44" y2="28" stroke="#dcdfe6" stroke-width="2" stroke-linecap="round"/>
        <line x1="20" y1="36" x2="36" y2="36" stroke="#dcdfe6" stroke-width="2" stroke-linecap="round"/>
      </svg>
    </div>
    <p class="empty-desc">{{ description || '暂无数据' }}</p>
  </div>
</template>

<style scoped>
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}

.empty-icon {
  margin-bottom: 12px;
  opacity: 0.6;
}

.empty-desc {
  font-size: 14px;
  color: #969799;
}
</style>
