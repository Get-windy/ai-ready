<script setup lang="ts">
interface Option {
  value: string
  label: string
}

defineProps<{
  label: string
  value?: string
  placeholder?: string
  options: Option[]
}>()

const emit = defineEmits<{
  change: [value: string]
}>()
</script>

<template>
  <div class="select-field">
    <label class="select-label">{{ label }}</label>
    <select
      class="select-input"
      :value="value"
      @change="emit('change', ($event.target as HTMLSelectElement).value)"
    >
      <option value="" disabled>{{ placeholder || '请选择' }}</option>
      <option
        v-for="opt in options"
        :key="opt.value"
        :value="opt.value"
      >
        {{ opt.label }}
      </option>
    </select>
  </div>
</template>

<style scoped>
.select-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px 0;
}

.select-label {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.select-input {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  outline: none;
  background: #f8f9fa;
  cursor: pointer;
  transition: border-color 0.2s;
  appearance: auto;
}

.select-input:focus {
  border-color: #1988fa;
  background: #fff;
}
</style>
