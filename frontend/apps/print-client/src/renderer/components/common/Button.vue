<script setup lang="ts">
defineProps<{
  type?: 'primary' | 'default' | 'danger'
  size?: 'small' | 'medium' | 'large'
  icon?: string
  loading?: boolean
}>()

const emit = defineEmits<{
  click: [e: MouseEvent]
}>()
</script>

<template>
  <button
    class="btn"
    :class="[type || 'default', size || 'medium', { loading }]"
    :disabled="loading"
    @click="emit('click', $event)"
  >
    <span v-if="loading" class="btn-spinner"></span>
    <slot />
  </button>
</template>

<style scoped>
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
  white-space: nowrap;
  user-select: none;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* types */
.btn.default {
  background: #f7f8fa;
  color: #333;
  border: 1px solid #dcdfe6;
}
.btn.default:hover:not(:disabled) {
  background: #ebedf0;
}

.btn.primary {
  background: #1988fa;
  color: #fff;
}
.btn.primary:hover:not(:disabled) {
  background: #0e7cd3;
}

.btn.danger {
  background: #f44;
  color: #fff;
}
.btn.danger:hover:not(:disabled) {
  background: #d63030;
}

/* sizes */
.btn.small {
  height: 28px;
  padding: 0 12px;
  font-size: 12px;
}

.btn.medium {
  height: 36px;
  padding: 0 16px;
}

.btn.large {
  height: 44px;
  padding: 0 24px;
  font-size: 16px;
}

/* loading */
.btn.loading {
  position: relative;
}

.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid currentColor;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
