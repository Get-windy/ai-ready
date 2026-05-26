<template>
  <div :class="formItemClass">
    <label v-if="label || $slots.label" :class="labelClass" :style="labelStyle">
      <slot name="label">{{ label }}</label>
    </label>
    <div :class="contentClass">
      <slot />
      <transition name="ar-zoom-in-top">
        <div v-if="validateState === 'error' && showMessage && !inlineMessage" class="ar-form-item__error">
          {{ validateMessage }}
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, inject, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import type { PropType } from 'vue'

defineOptions({
  name: 'ARFormItem',
})

export interface FormItemRule {
  required?: boolean
  message?: string
  trigger?: string | string[]
  min?: number
  max?: number
  len?: number
  pattern?: RegExp
  validator?: (rule: any, value: any, callback: any) => void
  type?: 'string' | 'number' | 'boolean' | 'method' | 'regexp' | 'integer' | 'float' | 'array' | 'object' | 'enum' | 'date' | 'url' | 'hex' | 'email'
}

export interface FormItemProps {
  label?: string
  prop?: string
  required?: boolean
  rules?: FormItemRule[] | FormItemRule
  error?: string
  validateStatus?: '' | 'success' | 'warning' | 'error' | 'validating'
  showMessage?: boolean
  inlineMessage?: boolean
  size?: 'large' | 'default' | 'small'
}

const props = withDefaults(defineProps<FormItemProps>(), {
  label: '',
  prop: '',
  required: false,
  rules: () => [],
  error: '',
  validateStatus: '',
  showMessage: true,
  inlineMessage: false,
  size: 'default',
})

const formContext: any = inject('arForm', null)

const validateState = ref<'success' | 'warning' | 'error' | 'validating' | ''>(props.validateStatus)
const validateMessage = ref(props.error)
const isRequired = ref(false)

const formItemClass = computed(() => [
  'ar-form-item',
  `ar-form-item--${props.size || formContext?.props?.size || 'default'}`,
  {
    'is-error': validateState.value === 'error',
    'is-success': validateState.value === 'success',
    'is-warning': validateState.value === 'warning',
    'is-validating': validateState.value === 'validating',
    'is-required': isRequired.value,
  },
])

const labelClass = computed(() => [
  'ar-form-item__label',
  {
    'is-required': isRequired.value && !formContext?.props?.hideRequiredAsterisk,
  },
])

const labelStyle = computed(() => {
  const labelWidth = formContext?.labelWidth?.value || '100px'
  const labelPosition = formContext?.props?.labelPosition || 'right'

  if (labelPosition === 'top') {
    return {
      'text-align': 'left',
      'width': '100%',
    }
  }

  return {
    'text-align': labelPosition === 'left' ? 'left' : 'right',
    width: labelWidth,
  }
})

const contentClass = computed(() => {
  const labelPosition = formContext?.props?.labelPosition || 'right'
  return {
    'ar-form-item__content': true,
    'is-inline': labelPosition === 'top',
  }
})

const getRules = () => {
  const formRules = formContext?.props?.rules || {}
  const selfRules = props.rules
  const requiredRule = props.required ? { required: true, message: `${props.label} 是必填项` } : []

  const propRules = formRules[props.prop || ''] || []

  return [].concat(selfRules || propRules || [], requiredRule).filter(Boolean)
}

const getFilteredRule = (trigger: string) => {
  const rules = getRules()
  return rules
    .filter((rule: FormItemRule) => {
      if (!rule.trigger || trigger === '') return true
      if (Array.isArray(rule.trigger)) {
        return rule.trigger.includes(trigger)
      }
      return rule.trigger === trigger
    })
    .map((rule: FormItemRule) => ({ ...rule }))
}

const validate = (trigger: string = '', callback?: (error?: any) => void) => {
  if (!props.prop) {
    return
  }

  const rules = getFilteredRule(trigger)

  if (!rules || rules.length === 0) {
    callback && callback()
    return
  }

  validateState.value = 'validating'

  const descriptor: any = {}

  descriptor[props.prop] = rules

  const validator = new Map()

  rules.forEach((rule: FormItemRule, index: number) => {
    rule.validator = rule.validator || customValidator
    validator.set(index, rule)
  })

  let hasError = false
  let errorMessages: string[] = []

  const executeValidation = async () => {
    for (const [index, rule] of validator) {
      try {
        await rule.validator(rule, formContext?.props?.model[props.prop || ''], (error?: any) => {
          if (error) {
            hasError = true
            errorMessages.push(error.message || error)
          }
        })
      } catch (error: any) {
        hasError = true
        errorMessages.push(error.message || error)
      }
    }

    validateState.value = hasError ? 'error' : 'success'
    validateMessage.value = hasError ? errorMessages[0] : ''

    formContext?.emit('validate', props.prop, !hasError, validateMessage.value)

    if (callback) {
      callback(hasError ? { message: errorMessages[0] } : undefined)
    }
  }

  executeValidation()
}

const customValidator = (rule: any, value: any, callback: any) => {
  const { type, required, min, max, len, pattern, message } = rule

  if (required && (value === undefined || value === null || value === '')) {
    callback(new Error(message || `${props.label} 是必填项`))
    return
  }

  if (type === 'string') {
    if (len && value.length !== len) {
      callback(new Error(message || `长度必须为 ${len}`))
      return
    }
    if (min && value.length < min) {
      callback(new Error(message || `长度不能少于 ${min}`))
      return
    }
    if (max && value.length > max) {
      callback(new Error(message || `长度不能超过 ${max}`))
      return
    }
  }

  if (type === 'number') {
    if (min !== undefined && value < min) {
      callback(new Error(message || `不能小于 ${min}`))
      return
    }
    if (max !== undefined && value > max) {
      callback(new Error(message || `不能大于 ${max}`))
      return
    }
  }

  if (pattern && !pattern.test(value)) {
    callback(new Error(message || '格式不正确'))
    return
  }

  callback()
}

const resetField = () => {
  validateState.value = ''
  validateMessage.value = ''
}

const clearValidate = () => {
  validateState.value = ''
  validateMessage.value = ''
}

// 监听模型变化
watch(
  () => formContext?.props?.model?.[props.prop || ''],
  () => {
    if (props.prop) {
      validate('blur')
    }
  }
)

onMounted(() => {
  if (props.prop) {
    formContext?.addField({
      prop: props.prop,
      validate,
      resetField,
      clearValidate,
    })
    isRequired.value = getRules().some((rule: FormItemRule) => rule.required)
  }
})

onUnmounted(() => {
  if (props.prop) {
    formContext?.removeField({ prop: props.prop })
  }
})

defineExpose({
  validate,
  resetField,
  clearValidate,
})
</script>

<style scoped>
.ar-form-item {
  display: flex;
  margin-bottom: 24px;
  font-size: 14px;
}

/* 标签 */
.ar-form-item__label {
  width: 100px;
  padding-right: 12px;
  box-sizing: border-box;
  color: #595959;
  line-height: 32px;
  flex-shrink: 0;
}

.ar-form-item__label.is-required::before {
  content: '*';
  color: #f5222d;
  margin-right: 4px;
}

/* 内容区域 */
.ar-form-item__content {
  flex: 1;
  position: relative;
}

.ar-form-item__content.is-inline {
  width: 100%;
}

/* 错误信息 */
.ar-form-item__error {
  color: #f5222d;
  font-size: 12px;
  line-height: 1;
  padding-top: 4px;
  position: absolute;
  top: 100%;
  left: 0;
}

/* 状态 */
.ar-form-item.is-error .ar-form-item__label {
  color: #f5222d;
}

.ar-form-item.is-success .ar-form-item__label {
  color: #52c41a;
}

.ar-form-item.is-warning .ar-form-item__label {
  color: #faad14;
}

/* 尺寸 */
.ar-form-item--large .ar-form-item__label {
  line-height: 40px;
}

.ar-form-item--small .ar-form-item__label {
  line-height: 24px;
}

/* 过渡动画 */
.ar-zoom-in-top-enter-active,
.ar-zoom-in-top-leave-active {
  opacity: 1;
  transform: scaleY(1);
  transition: transform 0.2s, opacity 0.2s;
  transform-origin: center top;
}

.ar-zoom-in-top-enter-from,
.ar-zoom-in-top-leave-to {
  opacity: 0;
  transform: scaleY(0.8);
}

/* 深色模式 */
[data-theme='dark'] .ar-form-item__label {
  color: #bfbfbf;
}

[data-theme='dark'] .ar-form-item.is-error .ar-form-item__label {
  color: #ff6e76;
}

[data-theme='dark'] .ar-form-item.is-success .ar-form-item__label {
  color: #7cffb2;
}

[data-theme='dark'] .ar-form-item.is-warning .ar-form-item__label {
  color: #fddd60;
}

[data-theme='dark'] .ar-form-item__error {
  color: #ff6e76;
}
</style>