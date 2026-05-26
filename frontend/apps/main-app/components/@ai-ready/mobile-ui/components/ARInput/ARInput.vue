<template>
  <div :class="inputWrapperClasses">
    <!-- 前缀区域 -->
    <div v-if="$slots.prefix || prefixIcon" class="ar-input__prefix">
      <slot name="prefix">
        <span v-if="prefixIcon" class="ar-input__prefix-icon">
          <svg class="ar-input__icon-svg" viewBox="0 0 24 24">
            <path :d="prefixIcon" fill="currentColor" />
          </svg>
        </span>
      </slot>
    </div>

    <!-- 输入框主体 -->
    <div class="ar-input__wrapper">
      <input
        ref="inputRef"
        :class="inputClasses"
        :type="inputType"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :maxlength="maxlength"
        :name="name"
        :autofocus="autofocus"
        @input="handleInput"
        @focus="handleFocus"
        @blur="handleBlur"
        @change="handleChange"
        @keydown="handleKeydown"
      />

      <!-- 清空按钮 -->
      <span
        v-if="clearable && modelValue && !disabled && !readonly"
        class="ar-input__clear"
        @click="handleClear"
      >
        <svg viewBox="0 0 24 24" class="ar-input__clear-icon">
          <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" fill="currentColor"/>
        </svg>
      </span>

      <!-- 密码可见切换 -->
      <span
        v-if="type === 'password' && showPassword"
        class="ar-input__password-toggle"
        @click="togglePasswordVisibility"
      >
        <svg v-if="passwordVisible" viewBox="0 0 24 24" class="ar-input__icon-svg">
          <path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z" fill="currentColor"/>
        </svg>
        <svg v-else viewBox="0 0 24 24" class="ar-input__icon-svg">
          <path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z" fill="currentColor"/>
        </svg>
      </span>
    </div>

    <!-- 后缀区域 -->
    <div v-if="$slots.suffix || suffixIcon || showWordLimit" class="ar-input__suffix">
      <slot name="suffix">
        <span v-if="showWordLimit && maxlength" class="ar-input__word-limit">
          {{ modelValue?.toString().length || 0 }}/{{ maxlength }}
        </span>
        <span v-else-if="suffixIcon" class="ar-input__suffix-icon">
          <svg class="ar-input__icon-svg" viewBox="0 0 24 24">
            <path :d="suffixIcon" fill="currentColor" />
          </svg>
        </span>
      </slot>
    </div>

    <!-- 错误提示 -->
    <div v-if="errorMessage" class="ar-input__error">
      {{ errorMessage }}
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { PropType } from 'vue'

type InputType = 'text' | 'password' | 'number' | 'tel' | 'email' | 'url' | 'search'
type InputSize = 'large' | 'normal' | 'small'

const props = defineProps({
  type: { type: String as PropType<InputType>, default: 'text' },
  modelValue: { type: [String, Number], default: '' },
  placeholder: { type: String, default: '' },
  disabled: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false },
  clearable: { type: Boolean, default: false },
  maxlength: { type: Number, default: undefined },
  showWordLimit: { type: Boolean, default: false },
  autofocus: { type: Boolean, default: false },
  name: { type: String, default: '' },
  size: { type: String as PropType<InputSize>, default: 'normal' },
  showPassword: { type: Boolean, default: false },
  prefixIcon: { type: String, default: '' },
  suffixIcon: { type: String, default: '' },
  customClass: { type: String, default: '' },
  error: { type: String, default: '' },
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'input', value: string): void
  (e: 'change', value: string): void
  (e: 'focus', event: FocusEvent): void
  (e: 'blur', event: FocusEvent): void
  (e: 'clear'): void
  (e: 'keydown', event: KeyboardEvent): void
}>()

const inputRef = ref<HTMLInputElement>()
const passwordVisible = ref(false)
const internalError = ref('')
const isFocused = ref(false)

// 优先使用props.error，否则使用内部错误状态
const errorMessage = computed(() => props.error || internalError.value)

const inputType = computed(() => {
  if (props.type === 'password' && passwordVisible.value) return 'text'
  return props.type
})

const inputWrapperClasses = computed(() => [
  'ar-input',
  `ar-input--${props.size}`,
  {
    'ar-input--disabled': props.disabled,
    'ar-input--readonly': props.readonly,
    'ar-input--focused': isFocused.value,
    'ar-input--error': errorMessage.value,
  },
  props.customClass,
])

const inputClasses = computed(() => [
  'ar-input__field',
  { 'ar-input__field--clearable': props.clearable },
])

const handleInput = (event: Event) => {
  const target = event.target as HTMLInputElement
  emit('update:modelValue', target.value)
  emit('input', target.value)
}

const handleFocus = (event: FocusEvent) => {
  isFocused.value = true
  emit('focus', event)
}

const handleBlur = (event: FocusEvent) => {
  isFocused.value = false
  emit('blur', event)
}

const handleChange = (event: Event) => {
  emit('change', (event.target as HTMLInputElement).value)
}

const handleClear = () => {
  emit('update:modelValue', '')
  emit('clear')
  inputRef.value?.focus()
}

const handleKeydown = (event: KeyboardEvent) => {
  emit('keydown', event)
}

const togglePasswordVisibility = () => {
  passwordVisible.value = !passwordVisible.value
}

// 暴露方法
defineExpose({
  focus: () => inputRef.value?.focus(),
  blur: () => inputRef.value?.blur(),
  clear: handleClear,
  setError: (msg: string) => { internalError.value = msg },
  clearError: () => { internalError.value = '' },
})
</script>

<style scoped lang="scss">
.ar-input {
  display: flex;
  align-items: center;
  width: 100%;
  background: var(--ar-color-background-white);
  border: 1px solid var(--ar-color-border);
  border-radius: var(--ar-border-radius-md);
  transition: all 0.2s ease;

  &:hover:not(&--disabled):not(&--readonly) {
    border-color: var(--ar-color-border-dark);
  }

  &--focused {
    border-color: var(--ar-color-primary);
    box-shadow: 0 0 0 2px rgba(25, 137, 250, 0.1);
  }

  &--disabled {
    background: var(--ar-color-background-gray);
    cursor: not-allowed;
  }

  &--readonly {
    background: var(--ar-color-background-gray);
  }

  &--error {
    border-color: var(--ar-color-danger);
  }

  // 尺寸
  &--small {
    height: 36px;
    padding: 0 12px;
    font-size: var(--ar-font-size-sm);
  }

  &--normal {
    height: 44px;
    padding: 0 16px;
    font-size: var(--ar-font-size-md);
  }

  &--large {
    height: 52px;
    padding: 0 20px;
    font-size: var(--ar-font-size-lg);
  }

  &__wrapper {
    flex: 1;
    display: flex;
    align-items: center;
    position: relative;
  }

  &__field {
    flex: 1;
    border: none;
    background: transparent;
    color: var(--ar-color-text-primary);
    outline: none;
    width: 100%;

    &::placeholder {
      color: var(--ar-color-text-tertiary);
    }

    &:disabled {
      cursor: not-allowed;
      color: var(--ar-color-text-quaternary);
    }
  }

  &__prefix,
  &__suffix {
    display: flex;
    align-items: center;
    color: var(--ar-color-text-tertiary);
  }

  &__prefix {
    margin-right: 8px;
  }

  &__suffix {
    margin-left: 8px;
  }

  &__prefix-icon,
  &__suffix-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
  }

  &__icon-svg {
    width: 100%;
    height: 100%;
  }

  &__clear {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    margin-left: 8px;
    cursor: pointer;
    color: var(--ar-color-text-tertiary);
    transition: color 0.2s ease;

    &:hover {
      color: var(--ar-color-text-secondary);
    }
  }

  &__clear-icon {
    width: 16px;
    height: 16px;
  }

  &__password-toggle {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    margin-left: 8px;
    cursor: pointer;
    color: var(--ar-color-text-tertiary);
    transition: color 0.2s ease;

    &:hover {
      color: var(--ar-color-text-secondary);
    }
  }

  &__word-limit {
    font-size: var(--ar-font-size-sm);
    color: var(--ar-color-text-tertiary);
  }

  &__error {
    position: absolute;
    bottom: -20px;
    left: 0;
    font-size: var(--ar-font-size-sm);
    color: var(--ar-color-danger);
  }
}
</style>
