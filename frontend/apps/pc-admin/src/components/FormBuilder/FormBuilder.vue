<template>
  <div class="form-builder">
    <a-form
      ref="formRef"
      :model="formModel"
      :rules="formRules"
      :layout="formLayout"
      :label-col="labelCol"
      :wrapper-col="wrapperCol"
    >
      <!-- 动态生成表单项 -->
      <template v-for="field in fields" :key="field.name">
        <a-form-item
          v-if="shouldShowField(field)"
          :name="field.name"
          :label="field.label"
          :rules="field.rules"
          :hidden="field.hidden"
        >
          <!-- 基础输入 -->
          <template v-if="field.type === 'input'">
            <a-input
              v-model:value="formModel[field.name]"
              :placeholder="field.placeholder"
              :allow-clear="field.allowClear !== false"
              :disabled="field.disabled"
              :show-count="field.showCount"
              @change="handleFieldChange(field.name, $event)"
            />
          </template>
          
          <!-- 密码输入 -->
          <template v-else-if="field.type === 'password'">
            <a-input-password
              v-model:value="formModel[field.name]"
              :placeholder="field.placeholder"
              :allow-clear="field.allowClear !== false"
              :disabled="field.disabled"
            />
          </template>
          
          <!-- 数字输入 -->
          <template v-else-if="field.type === 'number'">
            <a-input-number
              v-model:value="formModel[field.name]"
              :placeholder="field.placeholder"
              :min="field.min"
              :max="field.max"
              :step="field.step"
              :precision="field.precision"
              :disabled="field.disabled"
              :style="{ width: field.width || '100%' }"
              @change="handleFieldChange(field.name, $event)"
            />
          </template>
          
          <!-- 文本域 -->
          <template v-else-if="field.type === 'textarea'">
            <a-textarea
              v-model:value="formModel[field.name]"
              :placeholder="field.placeholder"
              :allow-clear="field.allowClear !== false"
              :disabled="field.disabled"
              :rows="field.rows"
              :show-count="field.showCount"
              @change="handleFieldChange(field.name, $event)"
            />
          </template>
          
          <!-- 下拉选择 -->
          <template v-else-if="field.type === 'select'">
            <a-select
              v-model:value="formModel[field.name]"
              :placeholder="field.placeholder"
              :options="field.options"
              :mode="field.mode"
              :allow-clear="field.allowClear !== false"
              :disabled="field.disabled"
              :show-search="field.showSearch"
              :max-tag-count="field.maxTagCount"
              :style="{ width: field.width || '100%' }"
              @change="handleFieldChange(field.name, $event)"
            />
          </template>
          
          <!-- 多选框组 -->
          <template v-else-if="field.type === 'checkbox-group'">
            <a-checkbox-group
              v-model:value="formModel[field.name]"
              :options="field.options"
              :disabled="field.disabled"
              @change="handleFieldChange(field.name, $event)"
            />
          </template>
          
          <!-- 单选框组 -->
          <template v-else-if="field.type === 'radio-group'">
            <a-radio-group
              v-model:value="formModel[field.name]"
              :options="field.options"
              :disabled="field.disabled"
              @change="handleFieldChange(field.name, $event)"
            />
          </template>
          
          <!-- 开关 -->
          <template v-else-if="field.type === 'switch'">
            <a-switch
              v-model:checked="formModel[field.name]"
              :disabled="field.disabled"
              :checked-children="field.checkedText"
              :un-checked-children="field.uncheckedText"
              @change="handleFieldChange(field.name, $event)"
            />
          </template>
          
          <!-- 日期选择 -->
          <template v-else-if="field.type === 'date'">
            <a-date-picker
              v-model:value="formModel[field.name]"
              :placeholder="field.placeholder"
              :disabled="field.disabled"
              :disabled-date="field.disabledDate"
              :format="field.format || 'YYYY-MM-DD'"
              @change="handleFieldChange(field.name, $event)"
            />
          </template>
          
          <!-- 日期时间选择 -->
          <template v-else-if="field.type === 'datetime'">
            <a-date-picker
              v-model:value="formModel[field.name]"
              :placeholder="field.placeholder"
              :disabled="field.disabled"
              :disabled-date="field.disabledDate"
              :format="field.format || 'YYYY-MM-DD HH:mm:ss'"
              show-time
              @change="handleFieldChange(field.name, $event)"
            />
          </template>
          
          <!-- 日期范围 -->
          <template v-else-if="field.type === 'date-range'">
            <a-range-picker
              v-model:value="formModel[field.name]"
              :placeholder="['开始日期', '结束日期']"
              :disabled="field.disabled"
              :disabled-date="field.disabledDate"
              :format="field.format || 'YYYY-MM-DD'"
              @change="handleDateRangeChange(field.name, $event)"
            />
          </template>
          
          <!-- 自定义字段 -->
          <template v-else-if="field.type === 'custom'">
            <slot
              :name="`field-${field.name}`"
              :value="formModel[field.name]"
              :on-change="(value: any) => handleFieldChange(field.name, value)"
            />
          </template>
          
          <!-- 默认：输入框 -->
          <template v-else>
            <a-input
              v-model:value="formModel[field.name]"
              :placeholder="field.placeholder"
              :allow-clear="field.allowClear !== false"
              :disabled="field.disabled"
            />
          </template>
        </a-form-item>
      </template>
      
      <!-- 自定义表单内容 -->
      <slot name="form-body" />
    </a-form>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, type PropType } from 'vue'
import dayjs, { Dayjs } from 'dayjs'

// 字段配置类型
export type FormField = {
  name: string
  label: string
  type: 'input' | 'password' | 'number' | 'textarea' | 'select' | 'checkbox-group' | 'radio-group' | 'switch' | 'date' | 'datetime' | 'date-range' | 'custom'
  value?: any
  placeholder?: string
  options?: Array<{ label: string; value: any }>
  rules?: any[]
  min?: number
  max?: number
  step?: number
  precision?: number
  rows?: number
  width?: string
  disabled?: boolean
  allowClear?: boolean
  showCount?: boolean
  showSearch?: boolean
  mode?: 'tags' | 'multiple' | null
  maxTagCount?: number
  checkedText?: string
  uncheckedText?: string
  format?: string
  disabledDate?: (current: Dayjs) => boolean
  hidden?: boolean
  [key: string]: any
}

// Props
const props = defineProps({
  fields: {
    type: Array as PropType<FormField[]>,
    required: true
  },
  modelValue: {
    type: Object as PropType<Record<string, any>>,
    default: () => ({})
  },
  rules: {
    type: Object as PropType<Record<string, any[]>>,
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
  }
})

// Emits
const emit = defineEmits<{
  'update:modelValue': [value: any]
  'field-change': [field: string, value: any]
  'submit': [formData: any]
}>()

// Refs
const formRef = ref()
const formModel = ref<Record<string, any>>({})
const formRules = ref<Record<string, any[]>>({})

// 初始化表单数据
const initFormData = () => {
  formModel.value = { ...props.modelValue }
  formRules.value = props.rules
}

// 初始化
initFormData()

// 监听外部值变化
import { watch } from 'vue'
watch(() => props.modelValue, (newVal) => {
  formModel.value = { ...newVal }
}, { deep: true })

// 监听规则变化
watch(() => props.rules, (newRules) => {
  formRules.value = newRules
})

// 是否显示字段
const shouldShowField = (field: FormField) => {
  if (field.hidden === true) return false
  return true
}

// 字段变化处理
const handleFieldChange = (field: string, value: any) => {
  formModel.value[field] = value
  emit('update:modelValue', formModel.value)
  emit('field-change', field, value)
}

// 日期范围变化处理
const handleDateRangeChange = (field: string, dates: [Dayjs, Dayjs]) => {
  if (dates && dates.length === 2) {
    formModel.value[`${field}_start`] = dates[0]?.toISOString()
    formModel.value[`${field}_end`] = dates[1]?.toISOString()
  } else {
    delete formModel.value[`${field}_start`]
    delete formModel.value[`${field}_end`]
  }
  emit('update:modelValue', formModel.value)
  emit('field-change', field, dates)
}

// 提交
const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    emit('submit', formModel.value)
  } catch (error) {
    console.error('Form validation failed:', error)
  }
}

// 设置字段值
const setFieldValue = (field: string, value: any) => {
  formModel.value[field] = value
  emit('update:modelValue', formModel.value)
}

// 设置字段错误
const setFieldError = (field: string, error: string) => {
  // 通过 Ant Design form rules 系统实现字段验证反馈
  const target = formRef.value?.getFieldInstance?.(field)
  if (target) {
    target.setFields?.([{ name: field, errors: error ? [error] : [], validating: false }])
  }
}

// 重置表单
const resetFields = () => {
  formRef.value?.resetFields()
  initFormData()
}

// 暴露方法
defineExpose({
  formRef,
  formModel,
  formRules,
  handleSubmit,
  setFieldValue,
  setFieldError,
  resetFields
})
</script>

<style scoped>
.form-builder {
  padding: 0;
}
</style>
