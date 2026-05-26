<template>
  <button
    :class="[
      'ar-button',
      `ar-button--${type}`,
      `ar-button--${size}`,
      { 'ar-button--round': round },
      { 'ar-button--block': block },
      { 'ar-button--loading': loading },
      { 'ar-button--disabled': disabled }
    ]"
    :disabled="disabled || loading"
    :type="nativeType"
    @click="handleClick"
  >
    <span v-if="loading" class="ar-button__loading">
      <slot name="loading">
        <span class="ar-button__loading-dot"></span>
      </slot>
    </span>
    <span v-else class="ar-button__content">
      <slot name="icon">
        <i v-if="icon" :class="icon" class="ar-button__icon"></i>
      </slot>
      <span class="ar-button__text">
        <slot></slot>
      </span>
    </span>
  </button>
</template>

<script setup lang="ts">
import { withDefaults } from 'vue';

interface Props {
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'default';
  size?: 'large' | 'default' | 'small' | 'mini';
  icon?: string;
  loading?: boolean;
  disabled?: boolean;
  round?: boolean;
  block?: boolean;
  nativeType?: 'button' | 'submit' | 'reset';
}

const props = withDefaults(defineProps<Props>(), {
  type: 'default',
  size: 'default',
  nativeType: 'button'
});

const emit = defineEmits<{
  (e: 'click', event: MouseEvent): void;
}>();

const handleClick = (event: MouseEvent) => {
  if (!props.loading && !props.disabled) {
    emit('click', event);
  }
};
</script>

<style lang="scss" scoped>
.ar-button {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 16px;
  border: 1px solid transparent;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.5;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);
  user-select: none;
  outline: none;
  white-space: nowrap;
  vertical-align: middle;

  &--block {
    display: block;
    width: 100%;
  }

  &--round {
    border-radius: 20px;
  }

  &--large {
    padding: 12px 20px;
    font-size: 16px;
  }

  &--small {
    padding: 6px 12px;
    font-size: 13px;
  }

  &--mini {
    padding: 4px 8px;
    font-size: 12px;
  }

  &--default {
    color: var(--ar-button-default-color, #606266);
    background-color: var(--ar-button-default-bg, #fff);
    border-color: var(--ar-button-default-border, #dcdfe6);

    &:hover {
      color: var(--ar-button-default-hover-color, #409eff);
      background-color: var(--ar-button-default-hover-bg, #fff);
      border-color: var(--ar-button-default-hover-border, #c6e2ff);
    }

    &:active {
      color: var(--ar-button-default-active-color, #3a8ee6);
      background-color: var(--ar-button-default-active-bg, #fff);
      border-color: var(--ar-button-default-active-border, #3a8ee6);
    }
  }

  &--primary {
    color: var(--ar-button-primary-color, #fff);
    background-color: var(--ar-button-primary-bg, #409eff);
    border-color: var(--ar-button-primary-border, #409eff);

    &:hover {
      background-color: var(--ar-button-primary-hover-bg, #66b1ff);
      border-color: var(--ar-button-primary-hover-border, #66b1ff);
    }

    &:active {
      background-color: var(--ar-button-primary-active-bg, #3a8ee6);
      border-color: var(--ar-button-primary-active-border, #3a8ee6);
    }
  }

  &--success {
    color: var(--ar-button-success-color, #fff);
    background-color: var(--ar-button-success-bg, #67c23a);
    border-color: var(--ar-button-success-border, #67c23a);

    &:hover {
      background-color: var(--ar-button-success-hover-bg, #85ce61);
      border-color: var(--ar-button-success-hover-border, #85ce61);
    }

    &:active {
      background-color: var(--ar-button-success-active-bg, #5daf34);
      border-color: var(--ar-button-success-active-border, #5daf34);
    }
  }

  &--warning {
    color: var(--ar-button-warning-color, #fff);
    background-color: var(--ar-button-warning-bg, #e6a23c);
    border-color: var(--ar-button-warning-border, #e6a23c);

    &:hover {
      background-color: var(--ar-button-warning-hover-bg, #ebb563);
      border-color: var(--ar-button-warning-hover-border, #ebb563);
    }

    &:active {
      background-color: var(--ar-button-warning-active-bg, #cf9236);
      border-color: var(--ar-button-warning-active-border, #cf9236);
    }
  }

  &--danger {
    color: var(--ar-button-danger-color, #fff);
    background-color: var(--ar-button-danger-bg, #f56c6c);
    border-color: var(--ar-button-danger-border, #f56c6c);

    &:hover {
      background-color: var(--ar-button-danger-hover-bg, #f78989);
      border-color: var(--ar-button-danger-hover-border, #f78989);
    }

    &:active {
      background-color: var(--ar-button-danger-active-bg, #dd6161);
      border-color: var(--ar-button-danger-active-border, #dd6161);
    }
  }

  &--info {
    color: var(--ar-button-info-color, #fff);
    background-color: var(--ar-button-info-bg, #909399);
    border-color: var(--ar-button-info-border, #909399);

    &:hover {
      background-color: var(--ar-button-info-hover-bg, #a6a9ad);
      border-color: var(--ar-button-info-hover-border, #a6a9ad);
    }

    &:active {
      background-color: var(--ar-button-info-active-bg, #82848a);
      border-color: var(--ar-button-info-active-border, #82848a);
    }
  }

  &:disabled,
  &--disabled {
    cursor: not-allowed;
    opacity: 0.6;

    &:hover {
      transform: none;
      box-shadow: none;
    }
  }

  &--loading {
    cursor: default;
  }

  &__loading {
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }

  &__loading-dot {
    display: inline-block;
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background-color: currentColor;
    animation: ar-button-loading 1.2s cubic-bezier(0.5, 0, 0.5, 1) infinite;

    &:nth-child(1) {
      animation-delay: -0.36s;
    }
    &:nth-child(2) {
      animation-delay: -0.24s;
    }
    &:nth-child(3) {
      animation-delay: -0.12s;
    }
    &:nth-child(4) {
      animation-delay: 0;
    }
  }

  &__content {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
  }

  &__icon {
    font-size: 16px;
  }

  &__text {
    line-height: 1;
  }

  @keyframes ar-button-loading {
    0% {
      transform: scale(0.8);
      opacity: 0.6;
    }
    50% {
      transform: scale(1);
      opacity: 1;
    }
    100% {
      transform: scale(0.8);
      opacity: 0.6;
    }
  }
}
</style>