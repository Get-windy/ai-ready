// 表单增强组件导出
export { default as FormEnhanced } from './FormEnhanced.vue'
export { default as FormFieldEnhanced } from './FormFieldEnhanced.vue'

// 类型导出
export type { ValidationRule, FieldRules, FormRules, ValidationResult, FormValidationState } from '@/utils/formValidation'
export type { PersistenceOptions } from '@/utils/formPersistence'

// 工具函数导出
export { useFormValidation, commonRules, useDebouncedSubmit, useThrottledSubmit } from '@/utils/formValidation'
export { useFormPersistence, createFormPersistenceManager, useFormDraft } from '@/utils/formPersistence'

// 默认导出
import FormEnhanced from './FormEnhanced.vue'
import FormFieldEnhanced from './FormFieldEnhanced.vue'

export default {
  FormEnhanced,
  FormFieldEnhanced
}