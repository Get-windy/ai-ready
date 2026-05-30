<template>
  <a-modal
    v-model:open="visible"
    :title="title"
    :width="width"
    :mask-closable="closeOnClickModal"
    :keyboard="closeOnPressEscape"
    :destroy-on-close="destroyOnClose"
    :centered="center || alignCenter"
    :footer="$slots.footer ? undefined : null"
    @ok="emit('open')"
    @cancel="handleCancel"
    @after-close="emit('closed')"
  >
    <template v-if="$slots.title" #title>
      <slot name="title" />
    </template>

    <slot />

    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

interface Props {
  modelValue?: boolean
  title?: string
  width?: string | number
  fullscreen?: boolean
  top?: string
  modal?: boolean
  modalClass?: string
  appendToBody?: boolean
  lockScroll?: boolean
  customClass?: string
  openDelay?: number
  closeDelay?: number
  closeOnClickModal?: boolean
  closeOnPressEscape?: boolean
  showClose?: boolean
  beforeClose?: (done: () => void) => void
  center?: boolean
  alignCenter?: boolean
  destroyOnClose?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: false,
  width: '50%',
  modal: true,
  appendToBody: false,
  lockScroll: true,
  closeOnClickModal: true,
  closeOnPressEscape: true,
  showClose: true,
  center: false,
  alignCenter: false,
  destroyOnClose: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'open'): void
  (e: 'opened'): void
  (e: 'close'): void
  (e: 'closed'): void
}>()

const visible = ref(props.modelValue)

// 监听外部modelValue变化
watch(() => props.modelValue, (newVal) => {
  visible.value = newVal
})

// 监听内部visible变化
watch(visible, (newVal) => {
  emit('update:modelValue', newVal)
  if (newVal) {
    emit('open')
    emit('opened')
  } else {
    emit('close')
  }
})

const handleCancel = () => {
  if (props.beforeClose) {
    props.beforeClose(() => {
      visible.value = false
    })
  } else {
    visible.value = false
  }
}

// 对话框操作方法
const open = () => {
  visible.value = true
}

const close = () => {
  visible.value = false
}

// 暴露方法
defineExpose({
  open,
  close
})
</script>

<style lang="scss" scoped>
.ar-dialog {
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px 20px 10px;

    &__title {
      font-size: 18px;
      font-weight: 600;
      color: var(--ar-text-color-primary, #303133);
      margin: 0;
    }

    &__close {
      cursor: pointer;
      color: var(--ar-text-color-secondary, #909399);
      font-size: 16px;

      &:hover {
        color: var(--ar-text-color-primary, #303133);
      }
    }
  }

  &__body {
    padding: 20px;
    color: var(--ar-text-color-regular, #606266);
  }

  &__footer {
    padding: 10px 20px 20px;
    text-align: right;
    border-top: 1px solid var(--ar-border-color-base, #dcdfe6);

    &--center {
      text-align: center;
    }
  }
}
</style>
