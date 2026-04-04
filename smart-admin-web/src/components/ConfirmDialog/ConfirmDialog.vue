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
        <div class="icon-wrapper" :class="`type-${type}`">
          <component :is="iconComponent" class="dialog-icon" />
        </div>
        <div class="dialog-text">
          <p class="message">{{ message }}</p>
          <p v-if="description" class="description">{{ description }}</p>
        </div>
      </div>
      
      <!-- 动态表单 -->
      <div v-if="showForm" class="dialog-form">
        <slot name="form" />
      </div>
      
      <!-- 自定义内容 -->
      <div v-if="$slots.default" class="custom-content">
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

<style scoped>
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
  background: #e6f7ff;
  color: #1890ff;
}

.icon-wrapper.type-success {
  background: #f6ffed;
  color: #52c41a;
}

.icon-wrapper.type-warning {
  background: #fff7e6;
  color: #faad14;
}

.icon-wrapper.type-error {
  background: #fff1f0;
  color: #ff4d4f;
}

.icon-wrapper.type-question {
  background: #f3f3f3;
  color: #8c8c8c;
}

.dialog-icon {
  font-size: 24px;
}

.dialog-text {
  flex: 1;
}

.message {
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
  line-height: 1.5;
}

.description {
  font-size: 12px;
  color: #666;
  line-height: 1.5;
}

.dialog-form {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.custom-content {
  margin-top: 16px;
}
</style>

/**
 * ConfirmDialog 确认弹窗组件库
 * 
 * 使用方法：
 * 
 * 1. 基本使用
 * ```vue
 * <template>
 *   <ConfirmDialog
 *     v-model:visible="showDialog"
 *     title="确认删除"
 *     message="确定要删除这条记录吗？"
 *     @ok="handleDelete"
 *   />
 * </template>
 * ```
 * 
 * 2. 不同类型的弹窗
 * ```vue
 * <!-- 信息提示 -->
 * <ConfirmDialog
 *   v-model:visible="showInfo"
 *   type="info"
 *   title="系统提示"
 *   message="您的操作已成功提交"
 * />
 * 
 * <!-- 警告提示 -->
 * <ConfirmDialog
 *   v-model:visible="showWarning"
 *   type="warning"
 *   title="警告"
 *   message="此操作不可恢复"
 * />
 * ```
 * 
 * 3. 带表单的确认弹窗
 * ```vue
 * <ConfirmDialog
 *   v-model:visible="showFormDialog"
 *   title="修改设置"
 *   message="请确认以下设置"
 *   :show-form="true"
 *   @ok="handleFormSubmit"
 * >
 *   <a-form :model="formData">
 *     <a-form-item label="备注">
 *       <a-input v-model:value="formData.remark" />
 *     </a-form-item>
 *   </a-form>
 * </ConfirmDialog>
 * ```
 * 
 * 4. 使用 Composition API
 * ```typescript
 * import { ref } from 'vue'
 * import { message } from 'ant-design-vue'
 * 
 * const showDialog = ref(false)
 * 
 * const showDialog = (type: 'confirm' | 'delete' | 'update') => {
 *   showDialog.value = true
 * }
 * 
 * const handleDelete = async () => {
 *   try {
 *     await api.delete(id)
 *     message.success('删除成功')
 *     showDialog.value = false
 *   } catch (error) {
 *     message.error('删除失败')
 *   }
 * }
 * ```
 * 
 * Props说明：
 * - visible: boolean - 控制弹窗显示
 * - title: string - 弹窗标题（默认：'确认操作'）
 * - message: string - 主要消息文字（必填）
 * - description: string - 描述信息（可选）
 * - type: 'info' | 'success' | 'warning' | 'error' | 'question' - 图标类型（默认：'warning'）
 * - cancelText: string - 取消按钮文字（默认：'取消'）
 * - okText: string - 确定按钮文字（默认：'确定'）
 * - confirmLoading: boolean - 确定按钮加载状态（默认：false）
 * - showForm: boolean - 是否显示表单（默认：false）
 * - centered: boolean - 是否居中显示（默认：false）
 * 
 * Events：
 * - ok: 点击确定时触发
 * - cancel: 点击取消时触发
 * - update:visible: visible变化时触发
 * 
 * 最佳实践：
 * 1. 对于重要操作，使用type="warning"或"type="error"
 * 2. 对于确认类操作，提供可选的description说明
 * 3. 避免在确认弹窗中做复杂的表单交互
 * 4. 确保弹窗有明确的关闭途径（取消按钮或关闭图标）
 */