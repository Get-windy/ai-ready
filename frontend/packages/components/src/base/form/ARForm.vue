<template>
  <a-form
    ref="formRef"
    :model="model"
    :rules="rules as any"
    :label-col="labelColConfig"
    :wrapper-col="wrapperColConfig"
    :colon="false"
    :layout="inline ? 'inline' : 'horizontal'"
    :size="size as any"
    :disabled="disabled"
    :validate-on-rule-change="validateOnRuleChange"
    @finish="handleFinish"
    @finish-failed="handleFinishFailed"
  >
    <slot />
  </a-form>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { FormInstance } from 'ant-design-vue'

export type { FormInstance }

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
  model?: Record<string, any>
  rules?: Record<string, Rule[]>
  labelPosition?: 'left' | 'right' | 'top'
  labelWidth?: string | number
  labelSuffix?: string
  inline?: boolean
  inlineMessage?: boolean
  statusIcon?: boolean
  showMessage?: boolean
  size?: 'large' | 'default' | 'small'
  disabled?: boolean
  validateOnRuleChange?: boolean
  hideRequiredAsterisk?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  labelPosition: 'right',
  inline: false,
  inlineMessage: false,
  statusIcon: false,
  showMessage: true,
  size: 'default',
  disabled: false,
  validateOnRuleChange: true,
  hideRequiredAsterisk: false
})

const emit = defineEmits<{
  (e: 'validate', name: string, status: boolean, errorMessages: string[]): void
  (e: 'finish', values: Record<string, any>): void
  (e: 'finish-failed', errorInfo: { values: Record<string, any>; errorFields: any[]; outOfDate: boolean }): void
  (e: 'submit'): void
}>()

const formRef = ref<FormInstance>()

const labelColConfig = computed(() => {
  if (props.labelWidth) {
    const width = typeof props.labelWidth === 'number' ? `${props.labelWidth}px` : props.labelWidth
    return { style: { width } }
  }
  return { span: 6 }
})

const wrapperColConfig = computed(() => {
  if (props.labelWidth) return undefined
  return { span: 18 }
})

const handleFinish = (values: Record<string, any>) => {
  emit('finish', values)
  emit('submit')
}

const handleFinishFailed = (errorInfo: any) => {
  emit('finish-failed', errorInfo)
}

const validate = async () => {
  if (!formRef.value) return
  return formRef.value.validate()
}

const validateField = async (name: string | string[]) => {
  if (!formRef.value) return
  return formRef.value.validate(name)
}

const resetFields = (names?: string | string[]) => {
  if (!formRef.value) return
  formRef.value.resetFields(names)
}

const clearValidate = (names?: string | string[]) => {
  if (!formRef.value) return
  formRef.value.clearValidate(names)
}

const scrollToField = (name: string) => {
  if (!formRef.value) return
  formRef.value.scrollToField(name)
}

defineExpose({
  validate,
  validateField,
  resetFields,
  clearValidate,
  scrollToField
})
</script>

<style lang="scss" scoped>
.ar-form {
  &--inline {
    .ant-form-item {
      margin-right: var(--ar-spacing-lg, 16px);
    }
  }

  &--disabled {
    opacity: 0.6;
    pointer-events: none;
  }
}
</style>
