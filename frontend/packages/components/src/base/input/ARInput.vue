<template>
  <div :class="[
    'ar-input',
    `ar-input--${size}`,
    {
      'ar-input--disabled': disabled,
      'ar-input--error': !!error,
      'ar-input--with-prepend': $slots.prepend,
      'ar-input--with-append': $slots.append,
      'ar-input--prefix-icon': prefixIcon,
      'ar-input--suffix-icon': suffixIcon
    }
  ]">
    <!-- 前置内容 -->
    <div v-if="$slots.prepend" class="ar-input__prepend">
      <slot name="prepend"></slot>
    </div>

    <!-- 输入框主体 -->
    <div class="ar-input__wrapper">
      <!-- 前缀图标 -->
      <span v-if="prefixIcon || $slots.prefix" class="ar-input__prefix">
        <slot name="prefix">
          <i v-if="prefixIcon" :class="prefixIcon" class="ar-input__icon"></i>
        </slot>
      </span>

      <!-- 输入框 -->
      <input
        :id="id"
        ref="inputRef"
        :value="modelValue"
        :type="inputType"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :maxlength="maxlength"
        :minlength="minlength"
        :autocomplete="autocomplete"
        :autofocus="autofocus"
        :name="name"
        :form="form"
        :class="[
          'ar-input__inner',
          {
            'ar-input__inner--clearable': clearable && modelValue,
            'ar-input__inner--password': showPassword
          }
        ]"
        @input="handleInput"
        @change="handleChange"
        @focus="handleFocus"
        @blur="handleBlur"
        @keydown="handleKeydown"
      />

      <!-- 可清除按钮 -->
      <span v-if="clearable && modelValue" class="ar-input__suffix">
        <i class="ar-icon-close" @click="handleClear"></i>
      </span>

      <!-- 密码显示切换 -->
      <span v-else-if="showPassword" class="ar-input__suffix">
        <i
          :class="[passwordVisible ? 'ar-icon-eye' : 'ar-icon-eye-close']"
          @click="togglePasswordVisibility"
        ></i>
      </span>

      <!-- 后缀图标 -->
      <span v-else-if="suffixIcon || $slots.suffix" class="ar-input__suffix">
        <slot name="suffix">
          <i v-if="suffixIcon" :class="suffixIcon" class="ar-input__icon"></i>
        </slot>
      </span>
    </div>

    <!-- 后置内容 -->
    <div v-if="$slots.append" class="ar-input__append">
      <slot name="append"></slot>
    </div>
  </div>

  <!-- 错误提示 -->
  <div v-if="error" class="ar-input__error">
    {{ error }}
  </div>

  <!-- 字数统计 -->
  <div v-if="showWordLimit && maxlength" class="ar-input__word-limit">
    <span :class="[
      'ar-input__word-count',
      { 'ar-input__word-count--exceeded': isExceeded }
    ]">
      {{ currentLength }}/{{ maxlength }}
    </span>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, watch } from 'vue';
import { useVModel } from '@vueuse/core';

interface Props {
  modelValue?: string | number;
  type?: string;
  size?: 'large' | 'default' | 'small' | 'mini';
  placeholder?: string;
  disabled?: boolean;
  readonly?: boolean;
  clearable?: boolean;
  showPassword?: boolean;
  showWordLimit?: boolean;
  prefixIcon?: string;
  suffixIcon?: string;
  error?: string;
  maxlength?: number;
  minlength?: number;
  autocomplete?: string;
  autofocus?: boolean;
  name?: string;
  form?: string;
  id?: string;
}

const props = withDefaults(defineProps<Props>(), {
  type: 'text',
  size: 'default'
});

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | number): void;
  (e: 'input', value: string | number): void;
  (e: 'change', value: string | number): void;
  (e: 'focus', event: FocusEvent): void;
  (e: 'blur', event: FocusEvent): void;
  (e: 'clear'): void;
  (e: 'keydown', event: KeyboardEvent): void;
}>();

const inputRef = ref<HTMLInputElement | null>(null);
const passwordVisible = ref(false);
const isFocused = ref(false);

const model = useVModel(props, 'modelValue', emit);

const inputType = computed(() => {
  if (props.showPassword) {
    return passwordVisible.value ? 'text' : 'password';
  }
  return props.type;
});

const currentLength = computed(() => {
  const value = model.value ?? '';
  return String(value).length;
});

const isExceeded = computed(() => {
  return props.maxlength ? currentLength.value > props.maxlength : false;
});

const handleInput = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const value = target.value;
  
  // 处理最大长度限制
  if (props.maxlength && value.length > props.maxlength) {
    target.value = value.slice(0, props.maxlength);
    model.value = target.value;
  } else {
    model.value = value;
  }
  
  emit('input', value);
};

const handleChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  emit('change', target.value);
};

const handleFocus = (event: FocusEvent) => {
  isFocused.value = true;
  emit('focus', event);
};

const handleBlur = (event: FocusEvent) => {
  isFocused.value = false;
  emit('blur', event);
};

const handleClear = () => {
  model.value = '';
  emit('clear');
  emit('update:modelValue', '');
  emit('input', '');
  emit('change', '');
  nextTick(() => {
    inputRef.value?.focus();
  });
};

const togglePasswordVisibility = () => {
  passwordVisible.value = !passwordVisible.value;
  nextTick(() => {
    inputRef.value?.focus();
  });
};

const handleKeydown = (event: KeyboardEvent) => {
  emit('keydown', event);
};

const focus = () => {
  inputRef.value?.focus();
};

const blur = () => {
  inputRef.value?.blur();
};

const select = () => {
  inputRef.value?.select();
};

defineExpose({
  focus,
  blur,
  select
});
</script>

<style lang="scss" scoped>
.ar-input {
  display: inline-flex;
  align-items: center;
  width: 100%;
  font-size: 14px;
  line-height: 1.5;
  position: relative;

  &--large {
    font-size: 16px;

    .ar-input__inner {
      height: 40px;
      line-height: 40px;
    }
  }

  &--default {
    .ar-input__inner {
      height: 36px;
      line-height: 36px;
    }
  }

  &--small {
    font-size: 13px;

    .ar-input__inner {
      height: 32px;
      line-height: 32px;
    }
  }

  &--mini {
    font-size: 12px;

    .ar-input__inner {
      height: 28px;
      line-height: 28px;
    }
  }

  &--disabled {
    opacity: 0.6;
    cursor: not-allowed;

    .ar-input__inner {
      cursor: not-allowed;
    }
  }

  &--error {
    .ar-input__inner {
      border-color: var(--ar-input-error-border, #f56c6c);
      background-color: var(--ar-input-error-bg, #fff);

      &:focus {
        border-color: var(--ar-input-error-focus-border, #f56c6c);
      }
    }
  }

  &__wrapper {
    position: relative;
    flex: 1;
    display: flex;
    align-items: center;
    width: 100%;
  }

  &__inner {
    flex: 1;
    width: 100%;
    padding: 0 12px;
    color: var(--ar-input-color, #606266);
    background-color: var(--ar-input-bg, #fff);
    border: 1px solid var(--ar-input-border, #dcdfe6);
    border-radius: 4px;
    outline: none;
    transition: border-color 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);

    &:hover {
      border-color: var(--ar-input-hover-border, #c0c4cc);
    }

    &:focus {
      border-color: var(--ar-input-focus-border, #409eff);
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
    }

    &::placeholder {
      color: var(--ar-input-placeholder-color, #c0c4cc);
    }

    &:disabled {
      background-color: var(--ar-input-disabled-bg, #f5f7fa);
      color: var(--ar-input-disabled-color, #c0c4cc);
      cursor: not-allowed;
    }
  }

  &__prefix,
  &__suffix {
    position: absolute;
    top: 0;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--ar-input-icon-color, #c0c4cc);
    cursor: pointer;
    user-select: none;
  }

  &__prefix {
    left: 12px;
  }

  &__suffix {
    right: 12px;
  }

  &__icon {
    font-size: 16px;
  }

  &__prepend,
  &__append {
    background-color: var(--ar-input-prepend-bg, #f5f7fa);
    color: var(--ar-input-prepend-color, #909399);
    border: 1px solid var(--ar-input-border, #dcdfe6);
    padding: 0 20px;
    white-space: nowrap;
  }

  &__prepend {
    border-right: 0;
    border-radius: 4px 0 0 4px;
  }

  &__append {
    border-left: 0;
    border-radius: 0 4px 4px 0;
  }

  &__error {
    color: var(--ar-input-error-color, #f56c6c);
    font-size: 12px;
    line-height: 1;
    padding-top: 4px;
  }

  &__word-limit {
    text-align: right;
    font-size: 12px;
    color: var(--ar-input-word-limit-color, #909399);
    padding-top: 4px;
  }

  &__word-count--exceeded {
    color: var(--ar-input-error-color, #f56c6c);
  }
}
</style>