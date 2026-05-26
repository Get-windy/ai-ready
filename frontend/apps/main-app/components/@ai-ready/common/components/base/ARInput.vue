<template>
  <div :class="wrapperClass">
    <el-icon v-if="prefixIcon" class="ar-input__prefix">
      <component :is="prefixIcon" />
    </el-icon>
    <input
      ref="inputRef"
      :class="inputClass"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :readonly="readonly"
      :maxlength="maxlength"
      :min="min"
      :max="max"
      :step="step"
      @input="handleInput"
      @change="handleChange"
      @focus="handleFocus"
      @blur="handleBlur"
    />
    <el-icon v-if="showClear" class="ar-input__clear" @click="handleClear">
      <CircleCloseFilled />
    </el-icon>
    <el-icon v-if="suffixIcon" class="ar-input__suffix">
      <component :is="suffixIcon" />
    </el-icon>
    <span v-if="showWordLimit && maxlength" class="ar-input__count">
      {{ textLength }}/{{ maxlength }}
    </span>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { CircleCloseFilled } from '@element-plus/icons-vue'

defineOptions({
  name: 'ARInput',
})

export interface InputProps {
  type?: 'text' | 'password' | 'number' | 'email' | 'url' | 'tel'
  modelValue?: string | number
  placeholder?: string
  disabled?: boolean
  readonly?: boolean
  clearable?: boolean
  showPassword?: boolean
  showWordLimit?: boolean
  maxlength?: number
  minlength?: number
  size?: 'large' | 'default' | 'small'
  prefixIcon?: string
  suffixIcon?: string
  rows?: number
  autosize?: boolean | { minRows?: number; maxRows?: number }
  autocomplete?: string
  name?: string
  min?: number | string
  max?: number | string
  step?: number | string
  resize?: 'none' | 'both' | 'horizontal' | 'vertical'
  autofocus?: boolean
  form?: string
  label?: string
  tabindex?: number
  validateEvent?: boolean
  inputStyle?: Record<string, any>
}

const props = withDefaults(defineProps<InputProps>(), {
  type: 'text',
  placeholder: '',
  disabled: false,
  readonly: false,
  clearable: false,
  showPassword: false,
  showWordLimit: false,
  size: 'default',
  autocomplete: 'off',
  resize: 'vertical',
  autofocus: false,
  validateEvent: true,
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'input', value: string): void
  (e: 'change', value: string): void
  (e: 'focus', event: FocusEvent): void
  (e: 'blur', event: FocusEvent): void
  (e: 'clear'): void
  (e: 'keydown', event: KeyboardEvent): void
  (e: 'keyup', event: KeyboardEvent): void
}>()

const inputRef = ref<HTMLInputElement>()
const focused = ref(false)
const hovering = ref(false)

const wrapperClass = computed(() => [
  'ar-input',
  `ar-input--${props.size}`,
  {
    'is-disabled': props.disabled,
    'is-readonly': props.readonly,
    'is-focused': focused.value,
  },
])

const inputClass = computed(() => [
  'ar-input__inner',
  {
    'is-disabled': props.disabled,
  },
])

const showClear = computed(() => {
  return props.clearable && !props.disabled && !props.readonly && modelValue.value
})

const modelValue = computed(() => {
  return props.modelValue?.toString() || ''
})

const textLength = computed(() => {
  return modelValue.value.length
})

const handleInput = (event: Event) => {
  const target = event.target as HTMLInputElement
  emit('update:modelValue', target.value)
  emit('input', target.value)
}

const handleChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  emit('change', target.value)
}

const handleFocus = (event: FocusEvent) => {
  focused.value = true
  emit('focus', event)
}

const handleBlur = (event: FocusEvent) => {
  focused.value = false
  emit('blur', event)
}

const handleClear = () => {
  emit('update:modelValue', '')
  emit('clear')
  inputRef.value?.focus()
}

const focus = () => {
  inputRef.value?.focus()
}

const blur = () => {
  inputRef.value?.blur()
}

const select = () => {
  inputRef.value?.select()
}

defineExpose({
  focus,
  blur,
  select,
  inputRef,
})
</script>

<style scoped>
.ar-input {
  position: relative;
  display: inline-flex;
  width: 100%;
  font-size: 14px;
}

.ar-input__inner {
  width: 100%;
  height: 32px;
  padding: 8px 12px;
  font-size: 14px;
  color: #262626;
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  box-sizing: border-box;
  transition: all 0.2s;
  outline: none;
}

.ar-input__inner::placeholder {
  color: #bfbfbf;
}

.ar-input__inner:hover {
  border-color: #40a9ff;
}

.ar-input__inner:focus {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.ar-input__inner:disabled {
  background-color: #f5f5f5;
  border-color: #d9d9d9;
  color: #d9d9d9;
  cursor: not-allowed;
}

.ar-input__inner[readonly] {
  background-color: #f5f5f5;
  cursor: default;
}

/* 前缀图标 */
.ar-input__prefix {
  position: absolute;
  left: 8px;
  top: 50%;
  transform: translateY(-50%);
  color: #8c8c8c;
  z-index: 1;
}

.ar-input--large .ar-input__prefix {
  font-size: 16px;
}

.ar-input--small .ar-input__prefix {
  font-size: 12px;
}

.ar-input.has-prefix .ar-input__inner {
  padding-left: 32px;
}

/* 后缀图标 */
.ar-input__suffix {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  color: #8c8c8c;
  z-index: 1;
  cursor: pointer;
}

.ar-input__suffix:hover {
  color: #595959;
}

.ar-input--large .ar-input__suffix {
  font-size: 16px;
}

.ar-input--small .ar-input__suffix {
  font-size: 12px;
}

.ar-input.has-suffix .ar-input__inner {
  padding-right: 32px;
}

/* 清除按钮 */
.ar-input__clear {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  color: #8c8c8c;
  cursor: pointer;
  font-size: 14px;
  z-index: 1;
}

.ar-input__clear:hover {
  color: #f5222d;
}

.ar-input.has-clear .ar-input__inner {
  padding-right: 32px;
}

/* 字符计数 */
.ar-input__count {
  position: absolute;
  right: 8px;
  bottom: -20px;
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1;
}

/* 尺寸 */
.ar-input--large .ar-input__inner {
  height: 40px;
  padding: 10px 14px;
  font-size: 16px;
}

.ar-input--small .ar-input__inner {
  height: 24px;
  padding: 6px 10px;
  font-size: 12px;
}

/* 聚焦状态 */
.ar-input.is-focused .ar-input__inner {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

/* 深色模式 */
[data-theme='dark'] .ar-input__inner {
  background-color: #1f1f1f;
  border-color: #434343;
  color: #e8e8e8;
}

[data-theme='dark'] .ar-input__inner::placeholder {
  color: #595959;
}

[data-theme='dark'] .ar-input__inner:hover {
  border-color: #177ddc;
}

[data-theme='dark'] .ar-input__inner:focus {
  border-color: #177ddc;
  box-shadow: 0 0 0 2px rgba(23, 125, 220, 0.15);
}

[data-theme='dark'] .ar-input__inner:disabled {
  background-color: #262626;
  border-color: #434343;
  color: #434343;
}

[data-theme='dark'] .ar-input__inner[readonly] {
  background-color: #262626;
}

[data-theme='dark'] .ar-input__prefix,
[data-theme='dark'] .ar-input__suffix,
[data-theme='dark'] .ar-input__clear {
  color: #8c8c8c;
}

[data-theme='dark'] .ar-input__count {
  color: #8c8c8c;
}
</style>