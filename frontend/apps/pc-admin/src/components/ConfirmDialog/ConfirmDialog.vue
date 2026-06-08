<template>
  <div class="confirm-dialog">
    <a-modal
      :open="visible"
      :title="title"
      :closable="closable"
      :confirm-loading="confirmLoading"
      @ok="handleOK"
      @cancel="handleCancel"
    >
      <div class="dialog-content">
        <div
          class="icon-wrapper"
          :class="`type-${type}`"
        >
          <component
            :is="iconComponent"
            class="dialog-icon"
          />
        </div>
        <div class="dialog-text">
          <p class="message">
            {{ message }}
          </p>
          <p
            v-if="description"
            class="description"
          >
            {{ description }}
          </p>
        </div>
      </div>
      
      <!-- 动态表单 -->
      <div
        v-if="showForm"
        class="dialog-form"
      >
        <slot name="form" />
      </div>
      
      <!-- 自定义内容 -->
      <div
        v-if="$slots.default"
        class="custom-content"
      >
        <slot />
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, type PropType } from 'vue'
import { 
  ExclamationCircleFilled,
  QuestionCircleFilled,
  InfoCircleFilled,
  CheckCircleFilled,
  WarningFilled,
  DeleteFilled
} from '@ant-design/icons-vue'

// Props
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '确认操作'
  },
  message: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  },
  type: {
    type: String as PropType<'info' | 'success' | 'warning' | 'error' | 'question'>,
    default: 'warning'
  },
  cancelText: {
    type: String,
    default: '取消'
  },
  okText: {
    type: String,
    default: '确定'
  },
  closable: {
    type: Boolean,
    default: true
  },
  confirmLoading: {
    type: Boolean,
    default: false
  },
  showForm: {
    type: Boolean,
    default: false
  },
  centered: {
    type: Boolean,
    default: false
  }
})

// Emits
const emit = defineEmits<{
  'ok': []
  'cancel': []
  'update:visible': [value: boolean]
}>()

// 计算图标
const iconComponent = computed(() => {
  const icons: Record<string, any> = {
    info: InfoCircleFilled,
    success: CheckCircleFilled,
    warning: WarningFilled,
    error: ExclamationCircleFilled,
    question: QuestionCircleFilled
  }
  return icons[props.type] || WarningFilled
})

// 确定处理
const handleOK = async () => {
  emit('ok')
}

// 取消处理
const handleCancel = () => {
  emit('cancel')
  emit('update:visible', false)
}
</script>

 * <style scoped>
.confirm-dialog {
  text-align: center;
}

.dialog-content {
  display: flex;
  align-items: center;
  text-align: left;
}

.icon-wrapper {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  flex-shrink: 0;
}

.icon-wrapper.type-info {
  background: var(--ar-color-info-lighter, #f4f4f5);
  color: var(--ar-color-primary, #409eff);
}

.icon-wrapper.type-success {
  background: var(--ar-color-success-lighter, #f0f9eb);
  color: var(--ar-color-success, #67c23a);
}

.icon-wrapper.type-warning {
  background: var(--ar-color-warning-lighter, #fdf6ec);
  color: var(--ar-color-warning, #e6a23c);
}

.icon-wrapper.type-error {
  background: var(--ar-color-danger-lighter, #fef0f0);
  color: var(--ar-color-danger, #f56c6c);
}

.icon-wrapper.type-question {
  background: var(--ar-fill-color, #f0f2f5);
  color: var(--ar-text-color-secondary, #909399);
}

.dialog-icon {
  font-size: 24px;
}

.dialog-text {
  flex: 1;
}

.message {
  font-size: 14px;
  color: var(--ar-text-color-primary, #303133);
  margin-bottom: 8px;
  line-height: 1.5;
}

.description {
  font-size: 12px;
  color: var(--ar-text-color-regular, #606266);
  line-height: 1.5;
}

.dialog-form {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--ar-border-color-light, #e4e7ed);
}

.custom-content {
  margin-top: 16px;
}
</style>
