<template>
  <a-form-item
    :label="label"
    :name="name"
    :rules="antdRules"
    :required="required"
    :has-feedback="showFeedback"
    :validate-status="validateStatus"
    :help="helpText"
  >
    <slot :value="modelValue" :on-change="handleChange" :on-blur="handleBlur" />
    
    <!-- 实时验证状态提示 -->
    <div v-if="showInlineFeedback && dirty" class="inline-feedback">
      <span v-if="validating" class="validating-text">
        <LoadingOutlined /> 正在验证...
      </span>
      <span v-else-if="!hasFieldError" class="success-text">
        <CheckCircleOutlined /> 验证通过
      </span>
    </div>
    
    <!-- 实时验证进度条 -->
    <div v-if="showProgress && progressRules.length > 0" class="validation-progress">
      <a-progress 
        :percent="progressPercent"
        :status="progressStatus"
        :stroke-color="progressColor"
        size="small"
        :show-info="false"
      />
      <div class="progress-items">
        <span 
          v-for="(item, index) in progressRules"
          :key="index"
          :class="['progress-item', { checked: item.checked }]"
        >
          {{ item.label }}
        </span>
      </div>
    </div>
  </a-form-item>
</template>

<script setup lang="ts">
import { 
  computed, 
  ref, 
  inject, 
  watch,
  type PropType,
  type InjectionKey
} from 'vue'
import { LoadingOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
import { type ValidationRule } from '@/utils/formValidation'

// 注入的验证上下文类型
type FormValidationContext = {
  validateField: (field: string, value: any) => Promise<any>
  errors: Record<string, string>
  touched: Record<string, boolean>
  dirty: Record<string, boolean>
  markTouched: (field: string) => void
  markDirty: (field: string) => void
  hasError: (field?: string) => boolean
}

const formValidationKey: InjectionKey<FormValidationContext> = Symbol('formValidation')

// Props
const props = defineProps({
  name: {
    type: String,
    required: true
  },
  label: {
    type: String,
    default: ''
  },
  modelValue: {
    type: [String, Number, Boolean, Object, Array],
    default: undefined
  },
  rules: {
    type: Array as PropType<ValidationRule[]>,
    default: () => []
  },
  required: {
    type: Boolean,
    default: false
  },
  showFeedback: {
    type: Boolean,
    default: true
  },
  showInlineFeedback: {
    type: Boolean,
    default: false
  },
  showProgress: {
    type: Boolean,
    default: false
  },
  debounceTime: {
    type: Number,
    default: 300
  },
  trigger: {
    type: String as PropType<'change' | 'blur' | 'both'>,
    default: 'change'
  }
})

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: any]
  'change': [value: any]
  'blur': []
  'validate-success': []
  'validate-failed': [error: string]
}>()

// 从父表单注入验证上下文
const formValidation = inject(formValidationKey, null)

// 本地状态
const dirty = ref(false)
const touched = ref(false)
const validating = ref(false)
const localError = ref('')

// 计算验证状态
const hasFieldError = computed(() => {
  if (formValidation) {
    return !!formValidation.errors[props.name]
  }
  return !!localError.value
})

const validateStatus = computed(() => {
  if (!dirty.value && !touched.value) return ''
  if (validating.value) return 'validating'
  if (hasFieldError.value) return 'error'
  return 'success'
})

const helpText = computed(() => {
  if (formValidation) {
    return formValidation.errors[props.name] || ''
  }
  return localError.value || ''
})

// 转换为 Ant Design 规则
const antdRules = computed(() => {
  return props.rules.map(rule => ({
    required: rule.required,
    message: rule.message,
    pattern: rule.pattern,
    min: rule.min,
    max: rule.max,
    validator: rule.validator ? async (value: any, callback: any) => {
      const result = await rule.validator!(value)
      if (result === true) {
        callback()
      } else {
        callback(typeof result === 'string' ? result : rule.message || '验证失败')
      }
    } : undefined,
    trigger: rule.trigger || props.trigger
  }))
})

// 验证进度计算
const progressRules = computed(() => {
  if (!props.showProgress) return []
  
  const items: Array<{ label: string; checked: boolean }> = []
  const value = props.modelValue
  
  for (const rule of props.rules) {
    if (rule.required) {
      items.push({
        label: '必填',
        checked: value !== undefined && value !== null && value !== ''
      })
    }
    
    if (rule.minLength !== undefined) {
      items.push({
        label: `最少${rule.minLength}字符`,
        checked: typeof value === 'string' && value.length >= rule.minLength
      })
    }
    
    if (rule.maxLength !== undefined) {
      items.push({
        label: `最多${rule.maxLength}字符`,
        checked: typeof value === 'string' && value.length <= rule.maxLength
      })
    }
    
    if (rule.pattern) {
      items.push({
        label: '格式正确',
        checked: typeof value === 'string' && rule.pattern.test(value)
      })
    }
  }
  
  return items
})

const progressPercent = computed(() => {
  if (progressRules.value.length === 0) return 0
  
  const checked = progressRules.value.filter(i => i.checked).length
  return Math.round((checked / progressRules.value.length) * 100)
})

const progressStatus = computed(() => {
  if (progressPercent.value === 100) return 'success'
  if (progressPercent.value < 50) return 'exception'
  return 'normal'
})

const progressColor = computed(() => {
  if (progressPercent.value === 100) return '#52c41a'
  if (progressPercent.value < 50) return '#ff4d4f'
  return '#1890ff'
})

// 验证字段
const validate = async (value: any) => {
  if (formValidation) {
    validating.value = true
    await formValidation.validateField(props.name, value)
    validating.value = false
    
    if (!formValidation.errors[props.name]) {
      emit('validate-success')
    } else {
      emit('validate-failed', formValidation.errors[props.name])
    }
  } else {
    // 本地验证
    validating.value = true
    localError.value = ''
    
    for (const rule of props.rules) {
      if (rule.required && (value === undefined || value === null || value === '')) {
        localError.value = rule.message || `${props.label}为必填项`
        validating.value = false
        emit('validate-failed', localError.value)
        return
      }
      
      if (rule.pattern && value && !rule.pattern.test(value)) {
        localError.value = rule.message || `${props.label}格式不正确`
        validating.value = false
        emit('validate-failed', localError.value)
        return
      }
      
      if (rule.validator) {
        const result = await rule.validator(value)
        if (result !== true) {
          localError.value = typeof result === 'string' ? result : rule.message || '验证失败'
          validating.value = false
          emit('validate-failed', localError.value)
          return
        }
      }
    }
    
    validating.value = false
    emit('validate-success')
  }
}

// 防抖验证
let debounceTimer: ReturnType<typeof setTimeout> | null = null

const debouncedValidate = (value: any) => {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  
  debounceTimer = setTimeout(() => {
    validate(value)
    debounceTimer = null
  }, props.debounceTime)
}

// 处理值变化
const handleChange = (value: any) => {
  dirty.value = true
  emit('update:modelValue', value)
  emit('change', value)
  
  if (formValidation) {
    formValidation.markDirty(props.name)
  }
  
  if (props.trigger === 'change' || props.trigger === 'both') {
    debouncedValidate(value)
  }
}

// 处理失焦
const handleBlur = () => {
  touched.value = true
  emit('blur')
  
  if (formValidation) {
    formValidation.markTouched(props.name)
  }
  
  if (props.trigger === 'blur' || props.trigger === 'both') {
    validate(props.modelValue)
  }
}

// 监听值变化
watch(() => props.modelValue, (newVal) => {
  if (dirty.value && (props.trigger === 'change' || props.trigger === 'both')) {
    debouncedValidate(newVal)
  }
})
</script>

<style scoped>
.inline-feedback {
  margin-top: 4px;
  font-size: 12px;
}

.validating-text {
  color: #1890ff;
}

.success-text {
  color: #52c41a;
}

.validation-progress {
  margin-top: 8px;
}

.progress-items {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.progress-item {
  font-size: 12px;
  color: #999;
  transition: color 0.3s;
}

.progress-item.checked {
  color: #52c41a;
}
</style>