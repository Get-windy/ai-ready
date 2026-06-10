<template>
  <div class="stat-card">
    <div class="stat-card__label">{{ title }}</div>
    <div class="stat-card__body">
      <span class="stat-card__value">{{ value }}</span>
      <span v-if="amount !== undefined" class="stat-card__amount" :style="{ color: color }">
        ¥{{ formatAmount(amount) }}
      </span>
    </div>
    <div v-if="subtitle" class="stat-card__subtitle">{{ subtitle }}</div>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'StatCard' })

withDefaults(defineProps<{
  title: string
  value: number | string
  amount?: number
  color?: string
  subtitle?: string
}>(), {
  amount: undefined,
  color: '#1890ff',
  subtitle: ''
})

function formatAmount(v: number): string {
  return (v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>

<style scoped>
.stat-card {
  width: 100%;
}
.stat-card__label {
  font-size: 13px;
  color: #666;
  margin-bottom: 4px;
}
.stat-card__body {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
}
.stat-card__value {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}
.stat-card__amount {
  font-size: 16px;
  font-weight: 500;
}
.stat-card__subtitle {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
</style>
