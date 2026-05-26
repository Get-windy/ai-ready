<template>
  <label
    :class="[
      'ar-checkbox',
      {
        'ar-checkbox--disabled': disabled,
        'ar-checkbox--checked': isChecked,
        'ar-checkbox--indeterminate': indeterminate,
        [`ar-checkbox--${size}`]: size,
      }
    ]"
    @click.prevent="handleChange"
  >
    <!-- 复选框 -->
    <span
      :class="[
        'ar-checkbox__input',
        {
          'ar-checkbox__input--disabled': disabled,
        }
      ]"
    >
      <span
        :class="[
          'ar-checkbox__inner',
          {
            'ar-checkbox__inner--checked': isChecked,
            'ar-checkbox__inner--indeterminate': indeterminate,
            'ar-checkbox__inner--disabled': disabled,
          }
        ]"
      >
        <!-- 选中图标 -->
        <svg
          v-if="isChecked && !indeterminate"
          class="ar-checkbox__icon"
          viewBox="0 0 1024 1024"
        >
          <path d="M512 64a448 448 0 1 1 0 896 448 448 0 0 1 0-896zm-55.808 736a50.432 50.432 0 0 0 35.712-14.848l304.064-304.64a50.624 50.624 0 0 0-71.552-71.616l-268.8 268.8-155.776-155.712a50.56 50.56 0 1 0-71.552 71.552l191.488 191.488a50.56 50.56 0 0 0 35.84 14.848z" />
        </svg>

        <!-- 半选图标 -->
        <svg
          v-if="indeterminate"
          class="ar-checkbox__icon"
          viewBox="0 0 1024 1024"
        >
          <path d="M512 64a448 448 0 1 1 0 896 448 448 0 0 1 0-896z m0 192a256 256 0 1 0 0 512 256 256 0 0 0 0-512z" />
        </svg>
      </span>

      <input
        type="checkbox"
        :class="['ar-checkbox__original']"
        :value="label"
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
        'ar-checkbox__label',
        {
          'ar-checkbox__label--disabled': disabled,
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
export interface ICheckboxProps {
  modelValue?: boolean | string | number
  label?: string | number | boolean
  disabled?: boolean
  indeterminate?: boolean
  size?: 'small' | 'medium' | 'large'
}

export interface ICheckboxEmits {
  (e: 'update:modelValue', value: boolean | string | number): void
  (e: 'change', value: boolean | string | number): void
}

// Props
const props = withDefaults(defineProps<ICheckboxProps>(), {
  disabled: false,
  indeterminate: false,
  size: 'medium',
})

// Emits
const emit = defineEmits<ICheckboxEmits>()

// 计算属性
const isChecked = computed(() => {
  if (props.label !== undefined) {
    return props.modelValue === props.label
  }
  return !!props.modelValue
})

// 方法
const handleChange = () => {
  if (props.disabled) return

  let value: boolean | string | number

  if (props.label !== undefined) {
    value = props.modelValue === props.label ? false : props.label
  } else {
    value = !props.modelValue
  }

  emit('update:modelValue', value)
  emit('change', value)
}
</script>

<style scoped lang="scss">
.ar-checkbox {
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
    border-radius: 2px;
    background-color: #fff;
    transition: all 0.3s;

    &::after {
      content: '';
      position: absolute;
      width: 100%;
      height: 100%;
      border-radius: 2px;
      transition: all 0.3s;
    }

    &--checked {
      background-color: var(--ar-color-primary);
      border-color: var(--ar-color-primary);
    }

    &--indeterminate {
      background-color: var(--ar-color-primary);
      border-color: var(--ar-color-primary);
    }

    &--disabled {
      background-color: var(--ar-color-disabled-bg);
      border-color: var(--ar-color-border);
      cursor: not-allowed;

      &.ar-checkbox__inner--checked,
      &.ar-checkbox__inner--indeterminate {
        background-color: var(--ar-color-disabled-bg);
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

  &__icon {
    width: 100%;
    height: 100%;
    fill: #fff;
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
  &:hover:not(.ar-checkbox--disabled) .ar-checkbox__inner {
    border-color: var(--ar-color-primary);
  }
}
</style>