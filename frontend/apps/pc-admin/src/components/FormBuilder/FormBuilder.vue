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
  // TODO: 实现字段错误设置
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

/**
 * FormBuilder 动态表单生成器组件库
 * 
 * 使用方法：
 * 
 * 1. 基本使用
 * ```vue
 * <template>
 *   <FormBuilder
 *     v-model="formData"
 *     :fields="fields"
 *     @submit="handleSubmit"
 *   />
 * </template>
 * 
 * <script setup>
 * import { ref } from 'vue'
 * 
 * const formData = ref({
 *   username: '',
 *   email: ''
 * })
 * 
 * const fields = [
 *   {
 *     name: 'username',
 *     label: '用户名',
 *     type: 'input',
 *     placeholder: '请输入用户名',
 *     rules: [{ required: true, message: '请输入用户名' }]
 *   },
 *   {
 *     name: 'email',
 *     label: '邮箱',
 *     type: 'input',
 *     placeholder: '请输入邮箱',
 *     rules: [{ required: true, message: '请输入邮箱' }]
 *   }
 * ]
 * 
 * const handleSubmit = async (data) => {
 *   console.log('表单数据:', data)
 *   await api.submit(data)
 * }
 * </script>
 * ```
 * 
 * 2. 下拉选择
 * ```vue
 * {
 *   name: 'status',
 *   label: '状态',
 *   type: 'select',
 *   options: [
 *     { label: '启用', value: 1 },
 *     { label: '禁用', value: 0 }
 *   ]
 * }
 * ```
 * 
 * 3. 日期范围
 * ```vue
 * {
 *   name: 'dateRange',
 *   label: '日期范围',
 *   type: 'date-range',
 *   format: 'YYYY-MM-DD'
 * }
 * ```
 * 
 * 4. 自定义字段
 * ```vue
 * {
 *   name: 'customField',
 *   label: '自定义字段',
 *   type: 'custom'
 * }
 * 
 * <template #field-customField="{ value, onChange }">
 *   <a-cascader
 *     :options="options"
 *     :value="value"
 *     @change="onChange"
 *   />
 * </template>
 * ```
 * 
 * Props说明：
 * - fields: FormField[] - 字段配置数组（必填）
 * - modelValue: Record<string, any> - 表单数据
 * - rules: Record<string, any[]> - 验证规则
 * - layout: 'horizontal' | 'vertical' | 'inline' - 表单布局（默认：'horizontal'）
 * - labelCol: { span: number } - 标签列配置（默认：{ span: 4 }）
 * - wrapperCol: { span: number } - 表单列配置（默认：{ span: 20 }）
 * 
 * 字段配置说明：
 * - name: string - 字段名（必填）
 * - label: string - 标签文字（必填）
 * - type: string - 字段类型（必填）
 * - value: any - 初始值
 * - placeholder: string - 占位提示
 * - options: Array - 选项数据（select、checkbox-group、radio-group需要）
 * - rules: any[] - 验证规则
 * - min/max: number - 数值范围
 * - step: number - 步长
 * - precision: number - 小数精度
 * - rows: number - 文本域行数
 * - width: string - 宽度
 * - disabled: boolean - 是否禁用
 * - allowClear: boolean - 是否可清空
 * - showCount: boolean - 是否显示字数统计
 * - showSearch: boolean - 是否可搜索
 * - mode: 'tags' | 'multiple' | null - 选择模式
 * - format: string - 日期格式
 * - disabledDate: function - 禁用日期函数
 * - hidden: boolean - 是否隐藏
 * 
 * Events：
 * - update:modelValue: 数据变化时触发
 * - field-change: 字段值变化时触发
 * - submit: 提交时触发
 * 
 * 暴露的方法：
 * - formRef: FormInstance - 表单引用
 * - formModel: object - 表单数据
 * - formRules: object - 表单规则
 * - handleSubmit: function - 提交表单
 * - setFieldValue: function - 设置字段值
 * - setFieldError: function - 设置字段错误
 * - resetFields: function - 重置表单
 * 
 * 支持的字段类型：
 * - input: 普通输入框
 * - password: 密码输入框
 * - number: 数字输入框
 * - textarea: 文本域
 * - select: 下拉选择
 * - checkbox-group: 多选框组
 * - radio-group: 单选框组
 * - switch: 开关
 * - date: 日期选择
 * - datetime: 日期时间选择
 * - date-range: 日期范围选择
 * - custom: 自定义字段
 * 
 * 最佳实践：
 * 1. 复杂表单使用动态配置方式
 * 2. 常用字段类型可以二次封装
 * 3. 日期范围字段会自动拆分为两个字段
 * 4. 使用v-model绑定数据实现双向绑定
 */
