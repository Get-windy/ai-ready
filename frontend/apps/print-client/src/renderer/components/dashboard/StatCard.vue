<script setup lang="ts">
defineProps<{
  title: string
  value: number | string
  color?: string
  icon?: string
}>()

const emit = defineEmits<{
  click: []
}>()
</script>

<template>
  <div class="stat-card" :style="{ '--accent': color || '#1988fa' }" @click="emit('click')">
    <div class="stat-value">{{ value }}</div>
    <div class="stat-title">{{ title }}</div>
  </div>
</template>

<style scoped>
.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
  border-left: 3px solid var(--accent);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--accent);
  line-height: 1.2;
}

.stat-title {
  font-size: 14px;
  color: #969799;
  margin-top: 4px;
}
</style>
