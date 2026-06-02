<template>
  <a-form-item
    :name="name"
    :label="label"
    :rules="rules"
    :required="required"
    :validate-status="validateStatus"
    :help="help"
  >
    <template v-if="$slots.label" #label>
      <slot name="label" />
    </template>
    <slot />
  </a-form-item>
</template>

<script setup lang="ts">
import { computed } from 'vue'

export interface Rule {
  required?: boolean
  message?: string
  trigger?: string | string[]
  min?: number
  max?: number
  pattern?: RegExp
  validator?: (value: any, callback: (error?: string) => void) => void
}

interface Props {
  name?: string
  label?: string
  labelWidth?: string | number
  required?: boolean
  rules?: Rule | Rule[]
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

const validateStatus = computed(() => {
  return props.error ? 'error' : undefined
})

const help = computed(() => {
  return props.showMessage ? props.error : undefined
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
