<script setup lang="ts">
defineProps<{
  label: string
  value?: string | number
  placeholder?: string
  type?: 'text' | 'number' | 'url'
}>()

const emit = defineEmits<{
  change: [value: any]
}>()
</script>

<template>
  <div class="field">
    <label class="field-label">{{ label }}</label>
    <input
      class="field-input"
      :type="type || 'text'"
      :value="value"
      :placeholder="placeholder"
      @input="emit('change', ($event.target as HTMLInputElement).value)"
    />
  </div>
</template>

<style scoped>
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 0;
}

.field-label {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.field-input {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  outline: none;
  transition: border-color 0.2s;
  background: #f8f9fa;
}

.field-input:focus {
  border-color: #1988fa;
  background: #fff;
}

.field-input::placeholder {
  color: #c0c4cc;
}
</style>
