<template>
  <el-form-item
    :prop="prop"
    :label="label"
    :label-width="labelWidth"
    :required="required"
    :rules="rules"
    :error="error"
    :show-message="showMessage"
    :inline-message="inlineMessage"
    :size="size"
  >
    <template v-if="$slots.label" #label>
      <slot name="label" />
    </template>
    
    <template v-if="$slots.default" #default>
      <slot />
    </template>
    
    <template v-if="$slots.error" #error>
      <slot name="error" />
    </template>
  </el-form-item>
</template>

<script setup lang="ts">
import type { FormItemRule } from 'element-plus'

interface Props {
  prop?: string
  label?: string
  labelWidth?: string | number
  required?: boolean
  rules?: FormItemRule | FormItemRule[]
  error?: string
  showMessage?: boolean
  inlineMessage?: boolean
  size?: 'large' | 'default' | 'small'
}

const props = withDefaults(defineProps<Props>(), {
  showMessage: true,
  inlineMessage: false,
  size: 'default'
})
</script>

<style lang="scss" scoped>
.ar-form-item {
  &__label {
    font-weight: 500;
    color: var(--ar-text-color-primary, #303133);
  }
  
  &__content {
    display: flex;
    align-items: center;
  }
  
  &__error {
    font-size: 12px;
    color: var(--ar-color-danger, #f56c6c);
    margin-top: var(--ar-spacing-xs, 4px);
  }
}
</style>