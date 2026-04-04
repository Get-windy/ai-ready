<template>
  <a-form
    ref="formRef"
    :model="modelValue"
    :rules="antdRules"
    :layout="layout"
    :label-col="labelCol"
    :wrapper-col="wrapperCol"
    @submit="handleSubmit"
  >
    <slot />
    
    <!-- 实时验证反馈提示 -->
    <div v-if="showValidationSummary && hasAnyError" class="validation-summary">
      <a-alert
        type="error"
        :message="validationSummaryTitle"
        show-icon
      >
        <template #description>
          <ul class="error-list">
            <li v-for="(error, field) in errors" :key="field">
              <span class="field-label">{{ getFieldLabel(field) }}</span>
              : {{ error }}
            </li>
          </ul>
        </template>
      </a-alert>
    </div>
    
    <!-- 表单操作按钮 -->
    <a-form-item v-if="showActions" :wrapper-col="{ offset: labelCol?.span || 4, span: 20 }">
      <a-space>
        <a-button 
          type="primary" 
          html-type="submit"
          :loading="submitting"
          :disabled="!isValid || submitting"
        >
          {{ submitText }}
        </a-button>
        <a-button @click="handleReset">
          重置
        </a-button>
        <a-button v-if="enableDraft" @click="handleSaveDraft">
          保存草稿
        </a-button>
        <a-button v-if="showRestoreDraft && drafts.length > 0" @click="showDraftModal = true">
          恢复草稿 ({{ drafts.length }})
        </a-button>
      </a-space>
    </a-form-item>
  </a-form>
  
  <!-- 草稿选择弹窗 -->
  <a-modal
    v-model:open="showDraftModal"
    title="选择草稿"
    @ok="handleApplyDraft"
  >
    <a-list :data-source="drafts" item-layout="horizontal">
      <template #renderItem="{ item }">
        <a-list-item>
          <a-list-item-meta
            :title="item.title"
            :description="formatDraftTime(item.timestamp)"
          />
          <template #actions>
            <a-button 
              type="link" 
              size="small"
              @click="selectedDraftId = item.id"
            >
              选择
            </a-button>
            <a-button 
              type="link" 
              size="small"
              danger
              @click="handleDeleteDraft(item.id)"
            >
              删除
            </a-button>
          </template>
        </a-list-item>
      </template>
    </a-list>
  </a-modal>
</template>

<script setup lang="ts">
import { 
  ref, 
  computed, 
  watch, 
  onMounted, 
  provide,
  type PropType,
  type FormInstance
} from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { useFormValidation, type FormRules } from '@/utils/formValidation'
import { useFormDraft } from '@/utils/formPersistence'
import { debounce } from 'lodash-es'

// Props
const props = defineProps({
  modelValue: {
    type: Object as PropType<Record<string, any>>,
    required: true
  },
  rules: {
    type: Object as PropType<FormRules>,
    default: () => ({})
  },
  layout: {
    type: String as PropType<'horizontal' | 'vertical' | 'inline'>,
    default: 'horizontal'
  },
  labelCol: {
    type: Object as PropType<{ span: number; offset?: number }>,
    default: () => ({ span: 4 })
  },
  wrapperCol: {
    type: Object as PropType<{ span: number; offset?: number }>,
    default: () => ({ span: 20 })
  },
  submitText: {
    type: String,
    default: '提交'
  },
  showActions: {
    type: Boolean,
    default: true
  },
  showValidationSummary: {
    type: Boolean,
    default: false
  },
  validationSummaryTitle: {
    type: String,
    default: '表单验证失败'
  },
  enableDraft: {
    type: Boolean,
    default: false
  },
  draftKey: {
    type: String,
    default: ''
  },
  showRestoreDraft: {
    type: Boolean,
    default: false
  },
  debounceSubmit: {
    type: Number,
    default: 500
  },
  fieldLabels: {
    type: Object as PropType<Record<string, string>>,
    default: () => ({})
  },
  onSubmit: {
    type: Function as PropType<(data: any) => Promise<any>>,
    default: undefined
  },
  onReset: {
    type: Function as PropType<() => void>,
    default: undefined
  }
})

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: any]
  'submit': [data: any]
  'reset': []
  'draft-saved': [draftId: string]
  'draft-applied': [draftId: string]
  'validation-success': []
  'validation-failed': [errors: Record<string, string>]
}>()

// Refs
const formRef = ref<FormInstance>()
const submitting = ref(false)
const showDraftModal = ref(false)
const selectedDraftId = ref('')

// 表单验证
const {
  errors,
  touched,
  dirty,
  validateField,
  validateAll,
  resetValidation,
  markTouched,
  markDirty,
  hasError,
  isValid
} = useFormValidation(
  computed(() => props.modelValue),
  props.rules,
  {
    validateOnChange: true,
    debounceTime: 300
  }
)

// 草稿功能
const drafts = ref<any[]>([])
let formDraft: ReturnType<typeof useFormDraft> | null = null

if (props.enableDraft && props.draftKey) {
  formDraft = useFormDraft(
    computed(() => props.modelValue),
    props.draftKey
  )
  drafts.value = formDraft.drafts.value
}

// 转换为 Ant Design 规则格式
const antdRules = computed(() => {
  const result: Record<string, any[]> = {}
  
  for (const [field, fieldRules] of Object.entries(props.rules)) {
    result[field] = fieldRules.map(rule => ({
      required: rule.required,
      message: rule.message,
      pattern: rule.pattern,
      min: rule.min,
      max: rule.max,
      validator: rule.validator ? async (value: any) => {
        const result = await rule.validator!(value)
        return result === true ? Promise.resolve() : Promise.reject(result)
      } : undefined,
      trigger: rule.trigger || 'change'
    }))
  }
  
  return result
})

// 是否有任何错误
const hasAnyError = computed(() => Object.keys(errors.value).length > 0)

// 获取字段标签
const getFieldLabel = (field: string) => {
  return props.fieldLabels[field] || field
}

// 格式化草稿时间
const formatDraftTime = (timestamp: number) => {
  return dayjs(timestamp).format('YYYY-MM-DD HH:mm:ss')
}

// 防抖提交
const debouncedSubmit = debounce(async () => {
  // 先验证
  const valid = await validateAll()
  
  if (!valid) {
    emit('validation-failed', errors.value)
    message.error('表单验证失败，请检查输入')
    return
  }
  
  submitting.value = true
  emit('validation-success')
  
  try {
    if (props.onSubmit) {
      await props.onSubmit(props.modelValue)
    }
    emit('submit', props.modelValue)
    message.success('提交成功')
  } catch (error) {
    message.error('提交失败，请重试')
    console.error('[FormSubmit]', error)
  } finally {
    submitting.value = false
  }
}, props.debounceSubmit)

// 提交处理
const handleSubmit = (e: Event) => {
  e.preventDefault()
  debouncedSubmit()
}

// 重置处理
const handleReset = () => {
  formRef.value?.resetFields()
  resetValidation()
  
  if (props.onReset) {
    props.onReset()
  }
  emit('reset')
}

// 保存草稿
const handleSaveDraft = () => {
  if (formDraft) {
    const draftId = formDraft.createDraft()
    emit('draft-saved', draftId)
    message.success('草稿已保存')
  }
}

// 应用草稿
const handleApplyDraft = () => {
  if (formDraft && selectedDraftId.value) {
    formDraft.applyDraft(selectedDraftId.value)
    emit('draft-applied', selectedDraftId.value)
    showDraftModal.value = false
    message.success('草稿已恢复')
  }
}

// 删除草稿
const handleDeleteDraft = (draftId: string) => {
  if (formDraft) {
    formDraft.deleteDraft(draftId)
    message.success('草稿已删除')
  }
}

// 提供验证方法给子组件
provide('formValidation', {
  validateField,
  errors,
  touched,
  dirty,
  markTouched,
  markDirty,
  hasError
})

// 暴露方法给父组件
defineExpose({
  validate: validateAll,
  validateField,
  resetFields: handleReset,
  resetValidation,
  errors,
  isValid
})
</script>

<style scoped>
.validation-summary {
  margin-bottom: 16px;
}

.error-list {
  padding-left: 20px;
  margin: 0;
}

.error-list li {
  margin-bottom: 4px;
}

.field-label {
  font-weight: 500;
}
</style>