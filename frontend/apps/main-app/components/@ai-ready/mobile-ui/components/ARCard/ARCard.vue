<template>
  <div :class="cardClasses" :style="cardStyles" @click="handleClick">
    <!-- 封面 -->
    <div v-if="cover || $slots.cover" class="ar-card__cover">
      <slot name="cover">
        <img :src="cover" :alt="title" class="ar-card__cover-image" />
      </slot>
    </div>

    <!-- 头部 -->
    <div v-if="title || subtitle || $slots.header" class="ar-card__header">
      <slot name="header">
        <div class="ar-card__header-content">
          <div v-if="title" class="ar-card__title">{{ title }}</div>
          <div v-if="subtitle" class="ar-card__subtitle">{{ subtitle }}</div>
        </div>
        <div v-if="extra || $slots.extra" class="ar-card__extra">
          <slot name="extra">{{ extra }}</slot>
        </div>
      </slot>
    </div>

    <!-- 内容 -->
    <div class="ar-card__body">
      <slot />
    </div>

    <!-- 底部 -->
    <div v-if="actions.length > 0 || $slots.footer" class="ar-card__footer">
      <slot name="footer">
        <div class="ar-card__actions">
          <button
            v-for="(action, index) in actions"
            :key="index"
            :class="['ar-card__action', `ar-card__action--${action.type || 'default'}`]"
            :disabled="action.disabled"
            @click.stop="handleAction(index)"
          >
            <span v-if="action.icon" class="ar-card__action-icon">{{ action.icon }}</span>
            <span class="ar-card__action-text">{{ action.text }}</span>
          </button>
        </div>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PropType } from 'vue'

interface CardAction {
  text: string
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'default'
  icon?: string
  disabled?: boolean
}

const props = defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  extra: { type: String, default: '' },
  cover: { type: String, default: '' },
  bordered: { type: Boolean, default: false },
  hoverable: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  shadow: { type: String as PropType<'none' | 'sm' | 'md' | 'lg'>, default: 'sm' },
  actions: { type: Array as PropType<CardAction[]>, default: () => [] },
  customClass: { type: String, default: '' },
  customStyle: { type: Object as PropType<Record<string, string | number>>, default: () => ({}) },
})

const emit = defineEmits<{
  (e: 'click'): void
  (e: 'action', index: number): void
}>()

const cardClasses = computed(() => [
  'ar-card',
  {
    'ar-card--bordered': props.bordered,
    'ar-card--hoverable': props.hoverable,
    'ar-card--loading': props.loading,
    [`ar-card--shadow-${props.shadow}`]: props.shadow !== 'none',
  },
  props.customClass,
])

const cardStyles = computed(() => ({
  ...props.customStyle,
}))

const handleClick = () => {
  if (props.loading) return
  emit('click')
}

const handleAction = (index: number) => {
  emit('action', index)
}
</script>

<style scoped lang="scss">
.ar-card {
  background: var(--ar-color-background-white);
  border-radius: var(--ar-border-radius-xl);
  overflow: hidden;
  transition: all 0.2s ease;

  &--bordered {
    border: 1px solid var(--ar-color-border);
  }

  &--hoverable {
    cursor: pointer;

    &:active {
      transform: scale(0.98);
    }
  }

  &--shadow-sm {
    box-shadow: var(--ar-box-shadow-sm);
  }

  &--shadow-md {
    box-shadow: var(--ar-box-shadow-md);
  }

  &--shadow-lg {
    box-shadow: var(--ar-box-shadow-lg);
  }

  &--loading {
    opacity: 0.7;
    pointer-events: none;
  }

  &__cover {
    width: 100%;
    overflow: hidden;

    &-image {
      width: 100%;
      height: 180px;
      object-fit: cover;
    }
  }

  &__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    padding: 16px 16px 0;
  }

  &__header-content {
    flex: 1;
    min-width: 0;
  }

  &__title {
    font-size: var(--ar-font-size-lg);
    font-weight: var(--font-weight-bolder);
    color: var(--ar-color-text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__subtitle {
    font-size: var(--ar-font-size-sm);
    color: var(--ar-color-text-secondary);
    margin-top: 4px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__extra {
    flex-shrink: 0;
    margin-left: 8px;
    font-size: var(--ar-font-size-sm);
    color: var(--ar-color-text-tertiary);
  }

  &__body {
    padding: 16px;
    font-size: var(--ar-font-size-md);
    color: var(--ar-color-text-primary);
    line-height: 1.6;
  }

  &__footer {
    padding: 12px 16px;
    border-top: 1px solid var(--ar-color-border);
  }

  &__actions {
    display: flex;
    gap: 8px;
  }

  &__action {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    height: 36px;
    padding: 0 16px;
    border: 1px solid var(--ar-color-border);
    border-radius: var(--ar-border-radius-md);
    background: transparent;
    font-size: var(--ar-font-size-sm);
    cursor: pointer;
    transition: all 0.2s ease;

    &:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    &:not(:disabled):active {
      transform: scale(0.98);
    }

    &--primary {
      background: var(--ar-color-primary);
      border-color: var(--ar-color-primary);
      color: #fff;
    }

    &--success {
      background: var(--ar-color-success);
      border-color: var(--ar-color-success);
      color: #fff;
    }

    &--warning {
      background: var(--ar-color-warning);
      border-color: var(--ar-color-warning);
      color: #fff;
    }

    &--danger {
      background: var(--ar-color-danger);
      border-color: var(--ar-color-danger);
      color: #fff;
    }

    &--default {
      color: var(--ar-color-text-primary);

      &:not(:disabled):hover {
        background: var(--ar-color-background-gray);
      }
    }
  }

  &__action-icon {
    font-size: var(--ar-font-size-md);
  }

  &__action-text {
    font-weight: var(--font-weight-bold);
  }
}
</style>
