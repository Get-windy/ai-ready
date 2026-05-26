<template>
  <el-form
    ref="formRef"
    :model="model"
    :rules="rules"
    :label-position="labelPosition"
    :label-width="labelWidth"
    :label-suffix="labelSuffix"
    :inline="inline"
    :inline-message="inlineMessage"
    :status-icon="statusIcon"
    :show-message="showMessage"
    :size="size"
    :disabled="disabled"
    :validate-on-rule-change="validateOnRuleChange"
    :hide-required-asterisk="hideRequiredAsterisk"
    @validate="emit('validate', $event)"
    @submit.prevent="handleSubmit"
  >
    <slot />
  </el-form>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { ElForm } from 'element-plus'
import type { FormRules, FormItemProp } from 'element-plus'

interface Props {
  model?: Record<string, any>
  rules?: FormRules
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
  (e: 'validate', prop: FormItemProp, isValid: boolean, message: string): void
  (e: 'submit'): void
}>()

const formRef = ref<InstanceType<typeof ElForm>>()

// 表单验证方法
const validate = async (callback?: (isValid: boolean, invalidFields?: Record<string, any>) => void) => {
  if (!formRef.value) return
  return formRef.value.validate(callback)
}

const validateField = async (props: FormItemProp | FormItemProp[], callback?: (isValid: boolean, invalidFields?: Record<string, any>) => void) => {
  if (!formRef.value) return
  return formRef.value.validateField(props, callback)
}

const resetFields = (props?: FormItemProp | FormItemProp[]) => {
  if (!formRef.value) return
  formRef.value.resetFields(props)
}

const clearValidate = (props?: FormItemProp | FormItemProp[]) => {
  if (!formRef.value) return
  formRef.value.clearValidate(props)
}

const scrollToField = (prop: FormItemProp) => {
  if (!formRef.value) return
  formRef.value.scrollToField(prop)
}

const handleSubmit = () => {
  emit('submit')
}

// 暴露方法给模板
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
    .el-form-item {
      margin-right: var(--ar-spacing-lg, 16px);
    }
  }
  
  &--disabled {
    opacity: 0.6;
    pointer-events: none;
  }
}
</style>