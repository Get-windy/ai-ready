<template>
  <button
    :class="buttonClasses"
    :style="buttonStyles"
    :disabled="disabled || loading"
    @click="handleClick"
    @touchstart="handleTouchStart"
    @touchend="handleTouchEnd"
  >
    <!-- 加载图标 -->
    <span v-if="loading" class="ar-button__loading">
      <svg class="ar-button__loading-icon" viewBox="0 0 24 24">
        <circle
          cx="12"
          cy="12"
          r="10"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-dasharray="31.41592653589793"
          stroke-dashoffset="10"
        >
          <animateTransform
            attributeName="transform"
            type="rotate"
            from="0 12 12"
            to="360 12 12"
            dur="1s"
            repeatCount="indefinite"
          />
        </circle>
      </svg>
    </span>

    <!-- 图标 - 左侧 -->
    <span
      v-if="icon && iconPosition === 'left' && !loading"
      class="ar-button__icon ar-button__icon--left"
    >
      <svg v-if="isSvgIcon" class="ar-button__icon-svg" viewBox="0 0 24 24">
        <path :d="icon" fill="currentColor" />
      </svg>
      <span v-else>{{ icon }}</span>
    </span>

    <!-- 内容 -->
    <span class="ar-button__content">
      <slot />
    </span>

    <!-- 图标 - 右侧 -->
    <span
      v-if="icon && iconPosition === 'right' && !loading"
      class="ar-button__icon ar-button__icon--right"
    >
      <svg v-if="isSvgIcon" class="ar-button__icon-svg" viewBox="0 0 24 24">
        <path :d="icon" fill="currentColor" />
      </svg>
      <span v-else>{{ icon }}</span>
    </span>
  </button>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { PropType } from 'vue'

type ThemeType = 'primary' | 'success' | 'warning' | 'danger' | 'info'
type ButtonVariant = 'solid' | 'outline' | 'ghost' | 'text'
type ButtonSize = 'large' | 'normal' | 'small' | 'mini'
type ButtonShape = 'default' | 'round' | 'circle'
type IconPosition = 'left' | 'right'

const props = defineProps({
  /** 按钮类型 */
  type: {
    type: String as PropType<ThemeType>,
    default: 'primary',
  },
  /** 按钮变体 */
  variant: {
    type: String as PropType<ButtonVariant>,
    default: 'solid',
  },
  /** 按钮尺寸 */
  size: {
    type: String as PropType<ButtonSize>,
    default: 'normal',
  },
  /** 按钮形状 */
  shape: {
    type: String as PropType<ButtonShape>,
    default: 'default',
  },
  /** 是否禁用 */
  disabled: {
    type: Boolean,
    default: false,
  },
  /** 是否加载中 */
  loading: {
    type: Boolean,
    default: false,
  },
  /** 是否块级按钮 */
  block: {
    type: Boolean,
    default: false,
  },
  /** 图标 */
  icon: {
    type: String,
    default: '',
  },
  /** 图标位置 */
  iconPosition: {
    type: String as PropType<IconPosition>,
    default: 'left',
  },
  /** 自定义类名 */
  customClass: {
    type: String,
    default: '',
  },
  /** 自定义样式 */
  customStyle: {
    type: Object as PropType<Record<string, string | number>>,
    default: () => ({}),
  },
})

const emit = defineEmits<{
  (e: 'click', event: MouseEvent): void
  (e: 'touchstart', event: TouchEvent): void
  (e: 'touchend', event: TouchEvent): void
}>()

// 是否SVG图标
const isSvgIcon = computed(() => props.icon?.startsWith('<') || props.icon?.startsWith('M'))

// 按钮类名
const buttonClasses = computed(() => {
  const classes = [
    'ar-button',
    `ar-button--${props.type}`,
    `ar-button--${props.variant}`,
    `ar-button--${props.size}`,
    `ar-button--${props.shape}`,
    {
      'ar-button--disabled': props.disabled,
      'ar-button--loading': props.loading,
      'ar-button--block': props.block,
      'ar-button--with-icon': props.icon,
    },
    props.customClass,
  ]
  return classes
})

// 按钮样式
const buttonStyles = computed(() => {
  return {
    ...props.customStyle,
  }
})

// 点击处理
const handleClick = (event: MouseEvent) => {
  if (props.disabled || props.loading) return
  emit('click', event)
}

// 触摸处理
const handleTouchStart = (event: TouchEvent) => {
  emit('touchstart', event)
}

const handleTouchEnd = (event: TouchEvent) => {
  emit('touchend', event)
}
</script>

<style scoped lang="scss">
.ar-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  border: 1px solid transparent;
  border-radius: var(--ar-border-radius-lg);
  font-weight: var(--font-weight-bold);
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  user-select: none;
  touch-action: manipulation;
  position: relative;
  overflow: hidden;

  // 尺寸
  &--mini {
    height: 24px;
    padding: 0 12px;
    font-size: var(--ar-font-size-xs);
  }

  &--small {
    height: 32px;
    padding: 0 16px;
    font-size: var(--ar-font-size-sm);
  }

  &--normal {
    height: 44px;
    padding: 0 20px;
    font-size: var(--ar-font-size-md);
  }

  &--large {
    height: 50px;
    padding: 0 24px;
    font-size: var(--ar-font-size-lg);
  }

  // 形状
  &--round {
    border-radius: var(--ar-border-radius-max);
  }

  &--circle {
    border-radius: 50%;
    padding: 0;
    width: 44px;
    height: 44px;

    &.ar-button--mini {
      width: 24px;
      height: 24px;
    }

    &.ar-button--small {
      width: 32px;
      height: 32px;
    }

    &.ar-button--large {
      width: 50px;
      height: 50px;
    }
  }

  // 变体 - 实心
  &--solid {
    &.ar-button--primary {
      background: var(--ar-color-primary);
      border-color: var(--ar-color-primary);
      color: #fff;
    }

    &.ar-button--success {
      background: var(--ar-color-success);
      border-color: var(--ar-color-success);
      color: #fff;
    }

    &.ar-button--warning {
      background: var(--ar-color-warning);
      border-color: var(--ar-color-warning);
      color: #fff;
    }

    &.ar-button--danger {
      background: var(--ar-color-danger);
      border-color: var(--ar-color-danger);
      color: #fff;
    }

    &.ar-button--info {
      background: var(--ar-color-info);
      border-color: var(--ar-color-info);
      color: #fff;
    }
  }

  // 变体 - 描边
  &--outline {
    background: transparent;

    &.ar-button--primary {
      color: var(--ar-color-primary);
      border-color: var(--ar-color-primary);
    }

    &.ar-button--success {
      color: var(--ar-color-success);
      border-color: var(--ar-color-success);
    }

    &.ar-button--warning {
      color: var(--ar-color-warning);
      border-color: var(--ar-color-warning);
    }

    &.ar-button--danger {
      color: var(--ar-color-danger);
      border-color: var(--ar-color-danger);
    }

    &.ar-button--info {
      color: var(--ar-color-info);
      border-color: var(--ar-color-info);
    }
  }

  // 变体 - 幽灵
  &--ghost {
    background: transparent;
    border-color: transparent;

    &.ar-button--primary {
      color: var(--ar-color-primary);
    }

    &.ar-button--success {
      color: var(--ar-color-success);
    }

    &.ar-button--warning {
      color: var(--ar-color-warning);
    }

    &.ar-button--danger {
      color: var(--ar-color-danger);
    }

    &.ar-button--info {
      color: var(--ar-color-info);
    }
  }

  // 变体 - 文本
  &--text {
    background: transparent;
    border-color: transparent;
    padding: 0;
    height: auto;
    line-height: inherit;

    &.ar-button--primary {
      color: var(--ar-color-primary);
    }

    &.ar-button--success {
      color: var(--ar-color-success);
    }

    &.ar-button--warning {
      color: var(--ar-color-warning);
    }

    &.ar-button--danger {
      color: var(--ar-color-danger);
    }

    &.ar-button--info {
      color: var(--ar-color-info);
    }

    &:active {
      opacity: 0.6;
    }
  }

  // 禁用状态
  &--disabled,
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  // 加载状态
  &--loading {
    cursor: not-allowed;

    .ar-button__content {
      opacity: 0.7;
    }
  }

  // 块级按钮
  &--block {
    display: flex;
    width: 100%;
  }

  // 点击效果
  &:not(&--disabled):not(&--loading):not(&--text):not(&--ghost):active {
    transform: scale(0.98);
    opacity: 0.9;
  }

  // 加载图标
  &__loading {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__loading-icon {
    width: 16px;
    height: 16px;
    color: inherit;
  }

  // 图标
  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;

    &--left {
      margin-right: 2px;
    }

    &--right {
      margin-left: 2px;
    }
  }

  &__icon-svg {
    width: 16px;
    height: 16px;
    color: inherit;
  }

  // 内容
  &__content {
    display: flex;
    align-items: center;
  }
}
</style>
