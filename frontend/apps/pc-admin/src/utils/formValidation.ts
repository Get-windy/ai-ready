import { ref, watch, type Ref, type UnwrapRef } from 'vue'
import { debounce, throttle } from 'lodash-es'

// 验证规则类型定义
export type ValidationRule = {
  required?: boolean
  message?: string
  pattern?: RegExp
  min?: number
  max?: number
  minLength?: number
  maxLength?: number
  validator?: (value: any) => boolean | string | Promise<boolean | string>
  trigger?: 'change' | 'blur' | 'submit'
}

export type FieldRules = ValidationRule[]

export type FormRules = Record<string, FieldRules>

// 验证结果类型
export type ValidationResult = {
  valid: boolean
  message: string
  field?: string
}

// 表单验证状态
export type FormValidationState = {
  errors: Record<string, string>
  touched: Record<string, boolean>
  dirty: Record<string, boolean>
  validating: Record<string, boolean>
}

/**
 * 创建表单验证器
 */
export function useFormValidation<T extends Record<string, any>>(
  formData: Ref<T>,
  rules: FormRules,
  options?: {
    validateOnChange?: boolean
    validateOnBlur?: boolean
    debounceTime?: number
  }
) {
  const opts = {
    validateOnChange: true,
    validateOnBlur: true,
    debounceTime: 300,
    ...options
  }

  const errors = ref<Record<string, string>>({})
  const touched = ref<Record<string, boolean>>({})
  const dirty = ref<Record<string, boolean>>({})
  const validating = ref<Record<string, boolean>>({})

  // 单字段验证
  const validateField = async (field: string, value: any): Promise<ValidationResult> => {
    const fieldRules = rules[field]
    if (!fieldRules || fieldRules.length === 0) {
      return { valid: true, message: '', field }
    }

    validating.value[field] = true

    for (const rule of fieldRules) {
      // 必填验证
      if (rule.required && (value === undefined || value === null || value === '')) {
        const message = rule.message || `${field}为必填项`
        errors.value[field] = message
        validating.value[field] = false
        return { valid: false, message, field }
      }

      // 模式验证
      if (rule.pattern && value) {
        if (!rule.pattern.test(value)) {
          const message = rule.message || `${field}格式不正确`
          errors.value[field] = message
          validating.value[field] = false
          return { valid: false, message, field }
        }
      }

      // 最小值验证
      if (rule.min !== undefined && typeof value === 'number') {
        if (value < rule.min) {
          const message = rule.message || `${field}不能小于${rule.min}`
          errors.value[field] = message
          validating.value[field] = false
          return { valid: false, message, field }
        }
      }

      // 最大值验证
      if (rule.max !== undefined && typeof value === 'number') {
        if (value > rule.max) {
          const message = rule.message || `${field}不能大于${rule.max}`
          errors.value[field] = message
          validating.value[field] = false
          return { valid: false, message, field }
        }
      }

      // 最小长度验证
      if (rule.minLength !== undefined && typeof value === 'string') {
        if (value.length < rule.minLength) {
          const message = rule.message || `${field}长度不能少于${rule.minLength}个字符`
          errors.value[field] = message
          validating.value[field] = false
          return { valid: false, message, field }
        }
      }

      // 最大长度验证
      if (rule.maxLength !== undefined && typeof value === 'string') {
        if (value.length > rule.maxLength) {
          const message = rule.message || `${field}长度不能超过${rule.maxLength}个字符`
          errors.value[field] = message
          validating.value[field] = false
          return { valid: false, message, field }
        }
      }

      // 自定义验证器
      if (rule.validator) {
        try {
          const result = await rule.validator(value)
          if (result === false || typeof result === 'string') {
            const message = typeof result === 'string' ? result : rule.message || `${field}验证失败`
            errors.value[field] = message
            validating.value[field] = false
            return { valid: false, message, field }
          }
        } catch (err) {
          const message = rule.message || `${field}验证异常`
          errors.value[field] = message
          validating.value[field] = false
          return { valid: false, message, field }
        }
      }
    }

    // 所有规则验证通过
    delete errors.value[field]
    validating.value[field] = false
    return { valid: true, message: '', field }
  }

  // 防抖验证
  const debouncedValidateField = debounce(validateField, opts.debounceTime)

  // 整表验证
  const validateAll = async (): Promise<boolean> => {
    const results: ValidationResult[] = []
    
    for (const field of Object.keys(rules)) {
      const value = formData.value[field]
      const result = await validateField(field, value)
      results.push(result)
    }
    
    return results.every(r => r.valid)
  }

  // 清除验证状态
  const resetValidation = (field?: string) => {
    if (field) {
      delete errors.value[field]
      delete touched.value[field]
      delete dirty.value[field]
      delete validating.value[field]
    } else {
      errors.value = {}
      touched.value = {}
      dirty.value = {}
      validating.value = {}
    }
  }

  // 标记字段已触碰
  const markTouched = (field: string) => {
    touched.value[field] = true
  }

  // 标记字段已修改
  const markDirty = (field: string) => {
    dirty.value[field] = true
  }

  // 监听表单数据变化，自动验证
  if (opts.validateOnChange) {
    watch(formData, (newData) => {
      for (const field of Object.keys(rules)) {
        if (dirty.value[field]) {
          debouncedValidateField(field, newData[field])
        }
      }
    }, { deep: true })
  }

  // 获取字段错误信息
  const getFieldError = (field: string): string | undefined => {
    return errors.value[field]
  }

  // 判断字段是否有错误
  const hasError = (field?: string): boolean => {
    if (field) {
      return !!errors.value[field]
    }
    return Object.keys(errors.value).length > 0
  }

  // 判断表单是否有效
  const isValid = (): boolean => {
    return Object.keys(errors.value).length === 0
  }

  return {
    errors,
    touched,
    dirty,
    validating,
    validateField,
    validateAll,
    resetValidation,
    markTouched,
    markDirty,
    getFieldError,
    hasError,
    isValid
  }
}

/**
 * 常用验证规则预设
 */
export const commonRules = {
  required: (message = '此项为必填项'): ValidationRule => ({
    required: true,
    message
  }),

  email: (message = '请输入有效的邮箱地址'): ValidationRule => ({
    pattern: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
    message
  }),

  phone: (message = '请输入有效的手机号码'): ValidationRule => ({
    pattern: /^1[3-9]\d{9}$/,
    message
  }),

  password: (message = '密码长度6-20位，需包含字母和数字'): ValidationRule => ({
    pattern: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,20}$/,
    message
  }),

  url: (message = '请输入有效的URL'): ValidationRule => ({
    pattern: /^(https?:\/\/)?([\da-z.-]+)\.([a-z.]{2,6})([\/\w .-]*)*\/?$/,
    message
  }),

  number: (message = '请输入数字'): ValidationRule => ({
    pattern: /^\d+$/,
    message
  }),

  integer: (message = '请输入整数'): ValidationRule => ({
    pattern: /^-?\d+$/,
    message
  }),

  minLength: (length: number, message?: string): ValidationRule => ({
    minLength: length,
    message: message || `长度不能少于${length}个字符`
  }),

  maxLength: (length: number, message?: string): ValidationRule => ({
    maxLength: length,
    message: message || `长度不能超过${length}个字符`
  }),

  min: (value: number, message?: string): ValidationRule => ({
    min: value,
    message: message || `不能小于${value}`
  }),

  max: (value: number, message?: string): ValidationRule => ({
    max: value,
    message: message || `不能大于${value}`
  }),

  range: (min: number, max: number, message?: string): ValidationRule[] => [
    { min, message: message || `不能小于${min}` },
    { max, message: message || `不能大于${max}` }
  ],

  // 异步验证：检查唯一性
  unique: (
    checkFn: (value: any) => Promise<boolean>,
    message = '该值已存在'
  ): ValidationRule => ({
    validator: async (value) => {
      const isUnique = await checkFn(value)
      return isUnique ? true : message
    },
    trigger: 'blur'
  })
}

/**
 * 创建防抖提交函数
 */
export function useDebouncedSubmit<T extends (...args: any[]) => Promise<any>>(
  submitFn: T,
  options?: {
    delay?: number
    leading?: boolean
    maxWait?: number
    onError?: (error: Error) => void
  }
) {
  const opts = {
    delay: 500,
    leading: false,
    maxWait: 2000,
    ...options
  }

  const submitting = ref(false)
  const lastSubmitTime = ref(0)

  const debouncedFn = debounce(async (...args: Parameters<T>) => {
    submitting.value = true
    lastSubmitTime.value = Date.now()
    
    try {
      const result = await submitFn(...args)
      submitting.value = false
      return result
    } catch (error) {
      submitting.value = false
      opts.onError?.(error as Error)
      throw error
    }
  }, opts.delay, {
    leading: opts.leading,
    maxWait: opts.maxWait
  })

  const cancel = () => {
    debouncedFn.cancel()
    submitting.value = false
  }

  const flush = () => {
    debouncedFn.flush()
  }

  return {
    submit: debouncedFn,
    submitting,
    cancel,
    flush
  }
}

/**
 * 创建节流提交函数
 */
export function useThrottledSubmit<T extends (...args: any[]) => Promise<any>>(
  submitFn: T,
  options?: {
    interval?: number
    leading?: boolean
    trailing?: boolean
    onError?: (error: Error) => void
  }
) {
  const opts = {
    interval: 1000,
    leading: true,
    trailing: false,
    ...options
  }

  const submitting = ref(false)

  const throttledFn = throttle(async (...args: Parameters<T>) => {
    submitting.value = true
    
    try {
      const result = await submitFn(...args)
      submitting.value = false
      return result
    } catch (error) {
      submitting.value = false
      opts.onError?.(error as Error)
      throw error
    }
  }, opts.interval, {
    leading: opts.leading,
    trailing: opts.trailing
  })

  const cancel = () => {
    throttledFn.cancel()
    submitting.value = false
  }

  return {
    submit: throttledFn,
    submitting,
    cancel
  }
}

export default {
  useFormValidation,
  commonRules,
  useDebouncedSubmit,
  useThrottledSubmit
}