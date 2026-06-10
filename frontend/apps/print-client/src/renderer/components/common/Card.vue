<script setup lang="ts">
defineProps<{
  title?: string
}>()
</script>

<template>
  <div class="card">
    <div class="card-head" v-if="title || $slots.header">
      <slot name="header">
        <span class="card-title">{{ title }}</span>
      </slot>
    </div>
    <div class="card-body">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.card-head {
  padding: 16px 20px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.card-title {
  display: block;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebedf0;
}

.card-body {
  padding: 16px 20px;
}

.card-head + .card-body {
  padding-top: 12px;
}
</style>
