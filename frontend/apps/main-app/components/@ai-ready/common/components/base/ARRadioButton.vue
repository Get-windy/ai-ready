<template>
  <label
    :class="[
      'ar-radio',
      {
        'ar-radio--disabled': disabled,
        'ar-radio--checked': isChecked,
        [`ar-radio--${size}`]: size,
        [`ar-radio--${variant}`]: variant,
      }
    ]"
    @click.prevent="handleChange"
  >
    <!-- 单选框 -->
    <span
      :class="[
        'ar-radio__input',
        {
          'ar-radio__input--disabled': disabled,
        }
      ]"
    >
      <span
        :class="[
          'ar-radio__inner',
          {
            'ar-radio__inner--checked': isChecked,
            'ar-radio__inner--disabled': disabled,
          }
        ]"
      >
        <!-- 选中圆点 -->
        <span
          v-if="isChecked"
          class="ar-radio__dot"
        ></span>
      </span>

      <input
        type="radio"
        :class="['ar-radio__original']"
        :value="label"
        :name="name"
        :disabled="disabled"
        :checked="isChecked"
        @change="handleChange"
        @click.stop
      />
    </span>

    <!-- 标签 -->
    <span
      v-if="$slots.default || label"
      :class="[
        'ar-radio__label',
        {
          'ar-radio__label--disabled': disabled,
        }
      ]"
    >
      <slot>{{ label }}</slot>
    </span>
  </label>
</template>

<script setup lang="ts">
import { computed } from 'vue'

// 类型定义
export interface IRadioProps {
  modelValue?: string | number | boolean
  label?: string | number | boolean
  disabled?: boolean
  size?: 'small' | 'medium' | 'large'
  variant?: 'default' | 'button'
  name?: string
}

export interface IRadioEmits {
  (e: 'update:modelValue', value: string | number | boolean): void
  (e: 'change', value: string | number | boolean): void
}

// Props
const props = withDefaults(defineProps<IRadioProps>(), {
  disabled: false,
  size: 'medium',
  variant: 'default',
})

// Emits
const emit = defineEmits<IRadioEmits>()

// 计算属性
const isChecked = computed(() => {
  return props.modelValue === props.label
})

// 方法
const handleChange = () => {
  if (props.disabled) return

  emit('update:modelValue', props.label!)
  emit('change', props.label!)
}
</script>

<style scoped lang="scss">
.ar-radio {
  position: relative;
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  font-size: 14px;
  user-select: none;
  white-space: nowrap;

  &--disabled {
    cursor: not-allowed;
  }

  // 按钮样式
  &--button {
    padding: 8px 16px;
    border: 1px solid var(--ar-color-border);
    border-radius: var(--ar-radius-base);
    transition: all 0.3s;

    &:not(:first-child) {
      margin-left: -1px;
    }

    &:hover:not(.ar-radio--disabled) {
      color: var(--ar-color-primary);
      border-color: var(--ar-color-primary);
    }

    &.ar-radio--checked {
      color: #fff;
      background-color: var(--ar-color-primary);
      border-color: var(--ar-color-primary);
      z-index: 1;
    }

    &.ar-radio--disabled {
      &.ar-radio--checked {
        background-color: var(--ar-color-disabled-bg);
        border-color: var(--ar-color-border);
        color: var(--ar-color-text-disabled);
      }
    }
  }

  &__input {
    position: relative;
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  &__inner {
    position: relative;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 14px;
    height: 14px;
    border: 1px solid var(--ar-color-border);
    border-radius: 50%;
    background-color: #fff;
    transition: all 0.3s;

    &--checked {
      border-color: var(--ar-color-primary);
    }

    &--disabled {
      background-color: var(--ar-color-disabled-bg);
      border-color: var(--ar-color-border);
      cursor: not-allowed;

      &.ar-radio__inner--checked {
        border-color: var(--ar-color-border);
      }
    }
  }

  &--small &__inner {
    width: 12px;
    height: 12px;
  }

  &--large &__inner {
    width: 16px;
    height: 16px;
  }

  &__dot {
    position: absolute;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background-color: var(--ar-color-primary);
    animation: radio-dot 0.2s ease-out;
  }

  &--small &__dot {
    width: 5px;
    height: 5px;
  }

  &--large &__dot {
    width: 7px;
    height: 7px;
  }

  &__original {
    position: absolute;
    top: 0;
    left: 0;
    width: 0;
    height: 0;
    opacity: 0;
    margin: 0;
    cursor: pointer;
  }

  &__label {
    margin-left: 8px;
    color: var(--ar-color-text-regular);

    &:not(:empty) {
      line-height: 1;
    }
  }

  &__label--disabled {
    color: var(--ar-color-text-disabled);
  }

  // Hover 效果
  &:hover:not(.ar-radio--disabled):not(.ar-radio--button) .ar-radio__inner {
    border-color: var(--ar-color-primary);
  }

  // 按钮样式不显示 radio
  &--button &__input {
    display: none;
  }

  &--button &__label {
    margin-left: 0;
  }
}

@keyframes radio-dot {
  from {
    transform: scale(0);
  }
  to {
    transform: scale(1);
  }
}
</style>