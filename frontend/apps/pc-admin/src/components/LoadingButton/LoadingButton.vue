<template>
  <a-button
    :type="type"
    :size="size"
    :danger="danger"
    :ghost="ghost"
    :disabled="disabled || loading"
    :loading="loading"
    :html-type="htmlType"
    :block="block"
    @click="handleClick"
  >
    <slot />
  </a-button>
</template>

<script setup lang="ts">
import { type PropType } from 'vue'

const props = defineProps({
  type: {
    type: String as PropType<'default' | 'primary' | 'dashed' | 'link' | 'text'>,
    default: 'primary'
  },
  size: {
    type: String as PropType<'small' | 'middle' | 'large'>,
    default: 'middle'
  },
  danger: {
    type: Boolean,
    default: false
  },
  ghost: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  htmlType: {
    type: String as PropType<'button' | 'submit' | 'reset'>,
    default: 'button'
  },
  block: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits<{
  'click': [event: MouseEvent]
}>()

const handleClick = (event: MouseEvent) => {
  if (!props.loading && !props.disabled) {
    emit('click', event)
  }
}
</script>