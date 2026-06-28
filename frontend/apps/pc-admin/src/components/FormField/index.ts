/**
 * FormField - 通用表单字段组件
 *
 * 提供两种显示模式：
 * - LabelField: 标签显示在输入框外部（上方）
 * - InlineField: 标签显示在输入框 placeholder 中（内嵌）
 */
export { default as LabelField } from './LabelField.vue'
export { default as InlineField } from './InlineField.vue'

export type FieldType = 'select' | 'tree-select' | 'date' | 'number' | 'input' | 'textarea'

export interface FieldOption {
  label: string
  value: any
}

export interface LabelFieldProps {
  type: FieldType
  label: string
  required?: boolean
  modelValue?: any
  placeholder?: string
  options?: FieldOption[]
  loading?: boolean
  disabled?: boolean
  searchBtn?: string
  width?: 'default' | 'narrow' | 'wide'
  viewMode?: boolean
}

export interface InlineFieldProps {
  type: FieldType
  label: string
  modelValue?: any
  options?: FieldOption[]
  loading?: boolean
  disabled?: boolean
  searchBtn?: string
  width?: 'default' | 'narrow' | 'wide'
  viewMode?: boolean
}