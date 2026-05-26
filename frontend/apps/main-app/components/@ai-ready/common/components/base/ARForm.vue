<template>
  <form :class="formClass" @submit.prevent="handleSubmit">
    <slot />
  </form>
</template>

<script setup lang="ts">
import { provide, computed, reactive, onMounted, onUnmounted } from 'vue'

defineOptions({
  name: 'ARForm',
})

export interface FormProps {
  model?: Record<string, any>
  rules?: Record<string, any[]>
  labelPosition?: 'left' | 'right' | 'top'
  labelWidth?: string | number
  labelSuffix?: string
  hideRequiredAsterisk?: boolean
  showMessage?: boolean
  inlineMessage?: boolean
  statusIcon?: boolean
  validateOnRuleChange?: boolean
  disabled?: boolean
  size?: 'large' | 'default' | 'small'
}

const props = withDefaults(defineProps<FormProps>(), {
  model: () => ({}),
  rules: () => ({}),
  labelPosition: 'right',
  labelWidth: '100px',
  labelSuffix: '',
  hideRequiredAsterisk: false,
  showMessage: true,
  inlineMessage: false,
  statusIcon: false,
  validateOnRuleChange: true,
  disabled: false,
  size: 'default',
})

const emit = defineEmits<{
  validate: (prop: string, isValid: boolean, message: string) => void
}>()

const formClass = computed(() => [
  'ar-form',
  `ar-form--${props.labelPosition}`,
  `ar-form--${props.size}`,
  {
    'is-disabled': props.disabled,
  },
])

const labelWidth = computed(() => {
  if (typeof props.labelWidth === 'number') {
    return `${props.labelWidth}px`
  }
  return props.labelWidth
})

// 表单验证
const fields: any[] = []

const addField = (field: any) => {
  if (field) {
    fields.push(field)
  }
}

const removeField = (field: any) => {
  if (field.prop) {
    fields.splice(fields.indexOf(field), 1)
  }
}

const validate = (callback?: (valid: boolean) => void) => {
  let valid = true
  let count = 0
  let invalidFields: Record<string, any> = {}

  if (fields.length === 0 && callback) {
    callback(true)
    return
  }

  fields.forEach((field) => {
    field.validate('', (errors: any) => {
      if (errors) {
        valid = false
        invalidFields = {
          ...invalidFields,
          ...errors,
        }
      }
      if (++count === fields.length && callback) {
        callback(valid)
      }
    })
  })

  return valid
}

const validateField = (props: string | string[], callback?: (valid: boolean) => void) => {
  const fieldsArr = Array.isArray(props) ? props : [props]
  const fieldList = fields.filter((field) => fieldsArr.includes(field.prop))

  if (!fieldList.length) {
    return false
  }

  let valid = true
  let count = 0

  fieldList.forEach((field) => {
    field.validate('', (errors: any) => {
      if (errors) {
        valid = false
      }
      if (++count === fieldList.length && callback) {
        callback(valid)
      }
    })
  })

  return valid
}

const resetFields = () => {
  fields.forEach((field) => {
    field.resetField()
  })
}

const clearValidate = (props?: string | string[]) => {
  const fieldsArr = props ? (Array.isArray(props) ? props : [props]) : []
  const fieldList = fieldsArr.length
    ? fields.filter((field) => fieldsArr.includes(field.prop))
    : fields

  fieldList.forEach((field) => {
    field.clearValidate()
  })
}

const handleSubmit = () => {
  validate()
}

// 提供给子组件使用
provide('arForm', {
  props,
  emit,
  fields,
  addField,
  removeField,
  labelWidth,
  validate,
  validateField,
  resetFields,
  clearValidate,
})

defineExpose({
  validate,
  validateField,
  resetFields,
  clearValidate,
})
</script>

<style scoped>
.ar-form {
  font-size: 14px;
  color: #595959;
}

/* 标签位置 */
.ar-form--left .ar-form-item__label {
  text-align: left;
}

.ar-form--right .ar-form-item__label {
  text-align: right;
}

.ar-form--top .ar-form-item {
  flex-direction: column;
}

/* 尺寸 */
.ar-form--large {
  font-size: 16px;
}

.ar-form--small {
  font-size: 12px;
}

/* 深色模式 */
[data-theme='dark'] .ar-form {
  color: #bfbfbf;
}
</style>