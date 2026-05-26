<template>
  <button
    :class="buttonClass"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <el-icon v-if="loading" class="ar-button__loading">
      <Loading />
    </el-icon>
    <el-icon v-if="icon && !loading" :class="iconClass">
      <component :is="icon" />
    </el-icon>
    <span v-if="$slots.default" :class="{ 'ar-button__text': icon || loading }">
      <slot />
    </span>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Loading } from '@element-plus/icons-vue'

defineOptions({
  name: 'ARButton',
})

export interface ButtonProps {
  type?: 'primary' | 'default' | 'dashed' | 'text' | 'link'
  size?: 'large' | 'default' | 'small'
  plain?: boolean
  round?: boolean
  circle?: boolean
  loading?: boolean
  disabled?: boolean
  icon?: string
  block?: boolean
}

const props = withDefaults(defineProps<ButtonProps>(), {
  type: 'default',
  size: 'default',
  plain: false,
  round: false,
  circle: false,
  loading: false,
  disabled: false,
  block: false,
})

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const buttonClass = computed(() => [
  'ar-button',
  `ar-button--${props.type}`,
  `ar-button--${props.size}`,
  {
    'is-plain': props.plain,
    'is-round': props.round,
    'is-circle': props.circle,
    'is-loading': props.loading,
    'is-disabled': props.disabled,
    'is-block': props.block,
  },
])

const iconClass = computed(() => ({
  'ar-button__icon': true,
  'ar-button__icon--left': true,
}))

const handleClick = (event: MouseEvent) => {
  if (!props.disabled && !props.loading) {
    emit('click', event)
  }
}
</script>

<style scoped>
.ar-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
  text-align: center;
  box-sizing: border-box;
  outline: none;
  transition: all 0.2s;
  user-select: none;
  vertical-align: middle;
  background-color: #ffffff;
  border: 1px solid #d9d9d9;
  color: #262626;
  font-weight: 400;
  font-size: 14px;
  height: 32px;
  padding: 8px 16px;
  border-radius: 6px;
}

.ar-button:hover {
  color: #1890ff;
  border-color: #40a9ff;
  background-color: #ffffff;
  opacity: 0.85;
}

.ar-button:active {
  border-color: #096dd9;
  outline: none;
}

.ar-button:focus {
  border-color: #40a9ff;
  outline: none;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

/* 类型 */
.ar-button--primary {
  color: #ffffff;
  background-color: #1890ff;
  border-color: #1890ff;
}

.ar-button--primary:hover {
  background-color: #40a9ff;
  border-color: #40a9ff;
  color: #ffffff;
}

.ar-button--primary:active {
  background-color: #096dd9;
  border-color: #096dd9;
}

.ar-button--dashed {
  border-style: dashed;
}

.ar-button--text {
  border-color: transparent;
  background: transparent;
  padding-left: 0;
  padding-right: 0;
}

.ar-button--text:hover {
  background: rgba(24, 144, 255, 0.1);
  border-color: transparent;
}

.ar-button--link {
  border-color: transparent;
  background: transparent;
  padding-left: 0;
  padding-right: 0;
  color: #1890ff;
}

.ar-button--link:hover {
  color: #40a9ff;
}

/* Plain */
.ar-button.is-plain {
  color: #1890ff;
  background-color: #ecf5ff;
  border-color: #a3d3ff;
}

.ar-button.is-plain:hover {
  background-color: #1890ff;
  border-color: #1890ff;
  color: #ffffff;
}

/* 尺寸 */
.ar-button--large {
  height: 40px;
  padding: 12px 24px;
  font-size: 16px;
}

.ar-button--small {
  height: 24px;
  padding: 4px 12px;
  font-size: 12px;
}

/* 圆形 */
.ar-button.is-round {
  border-radius: 20px;
  padding: 8px 20px;
}

.ar-button.is-circle {
  border-radius: 50%;
  padding: 8px;
  width: 32px;
  height: 32px;
}

.ar-button--large.is-circle {
  width: 40px;
  height: 40px;
  padding: 12px;
}

.ar-button--small.is-circle {
  width: 24px;
  height: 24px;
  padding: 4px;
}

/* 块级 */
.ar-button.is-block {
  display: flex;
  width: 100%;
}

/* 禁用 */
.ar-button.is-disabled,
.ar-button.is-disabled:hover {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Loading */
.ar-button.is-loading {
  position: relative;
  pointer-events: none;
}

.ar-button__loading {
  margin-right: 8px;
  animation: rotating 2s linear infinite;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.ar-button__icon {
  display: inline-block;
}

.ar-button__icon--left {
  margin-right: 8px;
}

.ar-button__text {
  display: inline-block;
}

/* 深色模式 */
[data-theme='dark'] .ar-button {
  background-color: #1f1f1f;
  border-color: #434343;
  color: #e8e8e8;
}

[data-theme='dark'] .ar-button:hover {
  color: #177ddc;
  border-color: #177ddc;
}

[data-theme='dark'] .ar-button--primary {
  background-color: #177ddc;
  border-color: #177ddc;
}

[data-theme='dark'] .ar-button--primary:hover {
  background-color: #3c9ae8;
  border-color: #3c9ae8;
}

[data-theme='dark'] .ar-button.is-plain {
  color: #177ddc;
  background-color: rgba(23, 125, 220, 0.15);
  border-color: #3c9ae8;
}

[data-theme='dark'] .ar-button--text:hover {
  background: rgba(23, 125, 220, 0.15);
}
</style>