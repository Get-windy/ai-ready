<template>
  <div :class="cardClass" :style="cardStyle">
    <div v-if="header || $slots.header" class="ar-card__header">
      <slot name="header">
        <span class="ar-card__title">{{ header }}</span>
        <div v-if="extra || $slots.extra" class="ar-card__extra">
          <slot name="extra">
            {{ extra }}
          </slot>
        </div>
      </slot>
    </div>
    <div class="ar-card__body" :style="bodyStyle">
      <slot />
    </div>
    <div v-if="footer || $slots.footer" class="ar-card__footer">
      <slot name="footer">
        {{ footer }}
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

defineOptions({
  name: 'ARCard',
})

export interface CardProps {
  header?: string
  footer?: string
  extra?: string
  shadow?: 'always' | 'hover' | 'never'
  bordered?: boolean
  bodyStyle?: Record<string, any>
  size?: 'large' | 'default' | 'small'
  hoverable?: boolean
}

const props = withDefaults(defineProps<CardProps>(), {
  shadow: 'hover',
  bordered: true,
  size: 'default',
  hoverable: false,
})

const cardClass = computed(() => [
  'ar-card',
  `ar-card--${props.size}`,
  `is-${props.shadow}-shadow`,
  {
    'is-bordered': props.bordered,
    'is-hoverable': props.hoverable,
  },
])

const cardStyle = computed(() => ({
  ...props.bodyStyle,
}))
</script>

<style scoped>
.ar-card {
  background-color: #ffffff;
  border-radius: 8px;
  transition: all 0.3s;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.ar-card.is-bordered {
  border: 1px solid #f0f0f0;
}

/* 阴影 */
.ar-card.is-always-shadow {
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.03),
              0 1px 6px -1px rgba(0, 0, 0, 0.02),
              0 2px 4px 0 rgba(0, 0, 0, 0.02);
}

.ar-card.is-hover-shadow:hover,
.ar-card.is-hoverable:hover {
  box-shadow: 0 3px 6px -4px rgba(0, 0, 0, 0.12),
              0 6px 16px 0 rgba(0, 0, 0, 0.08),
              0 9px 28px 8px rgba(0, 0, 0, 0.05);
  transform: translateY(-2px);
}

.ar-card.is-never-shadow {
  box-shadow: none;
}

/* 头部 */
.ar-card__header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.ar-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #262626;
  line-height: 24px;
}

.ar-card__extra {
  font-size: 14px;
  color: #595959;
  line-height: 22px;
}

/* 主体 */
.ar-card__body {
  padding: 24px;
  flex: 1;
  font-size: 14px;
  color: #595959;
  line-height: 22px;
}

/* 底部 */
.ar-card__footer {
  padding: 12px 24px;
  border-top: 1px solid #f0f0f0;
  font-size: 14px;
  color: #595959;
  line-height: 22px;
}

/* 尺寸 */
.ar-card--large .ar-card__header {
  padding: 24px 32px;
}

.ar-card--large .ar-card__title {
  font-size: 18px;
  line-height: 28px;
}

.ar-card--large .ar-card__body {
  padding: 32px;
  font-size: 16px;
  line-height: 24px;
}

.ar-card--large .ar-card__footer {
  padding: 16px 32px;
}

.ar-card--small .ar-card__header {
  padding: 16px 20px;
}

.ar-card--small .ar-card__title {
  font-size: 14px;
  line-height: 22px;
}

.ar-card--small .ar-card__body {
  padding: 16px 20px;
  font-size: 13px;
  line-height: 20px;
}

.ar-card--small .ar-card__footer {
  padding: 10px 20px;
  font-size: 13px;
}

/* 深色模式 */
[data-theme='dark'] .ar-card {
  background-color: #1f1f1f;
}

[data-theme='dark'] .ar-card.is-bordered {
  border-color: #303030;
}

[data-theme='dark'] .ar-card__header {
  border-bottom-color: #303030;
}

[data-theme='dark'] .ar-card__title {
  color: #e8e8e8;
}

[data-theme='dark'] .ar-card__extra {
  color: #bfbfbf;
}

[data-theme='dark'] .ar-card__body {
  color: #bfbfbf;
}

[data-theme='dark'] .ar-card__footer {
  border-top-color: #303030;
  color: #bfbfbf;
}
</style>