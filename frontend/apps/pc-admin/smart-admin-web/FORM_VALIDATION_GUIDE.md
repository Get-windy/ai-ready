# AI-Ready 表单验证组件使用指南

## 概述

AI-Ready 项目提供了一套完整的表单验证解决方案，包括：
- **FormEnhanced** - 增强型表单容器组件
- **FormFieldEnhanced** - 增强型表单字段组件
- **useFormValidation** - 表单验证 Composable
- **commonRules** - 常用验证规则预设

## 快速开始

### 基础用法

```vue
<template>
  <FormEnhanced
    v-model="formData"
    :rules="formRules"
    @submit="handleSubmit"
  >
    <FormFieldEnhanced
      name="username"
      label="用户名"
      v-model="formData.username"
      :rules="usernameRules"
    >
      <a-input v-model:value="formData.username" />
    </FormFieldEnhanced>

    <FormFieldEnhanced
      name="email"
      label="邮箱"
      v-model="formData.email"
      :rules="emailRules"
    >
      <a-input v-model:value="formData.email" />
    </FormFieldEnhanced>
  </FormEnhanced>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { FormEnhanced, FormFieldEnhanced } from '@/components/Form'
import { commonRules } from '@/utils/formValidation'

const formData = ref({
  username: '',
  email: ''
})

const usernameRules = [
  commonRules.required('请输入用户名'),
  commonRules.minLength(3),
  commonRules.maxLength(20)
]

const emailRules = [
  commonRules.required('请输入邮箱'),
  commonRules.email()
]

const handleSubmit = async (values: any) => {
  console.log('表单提交:', values)
}
</script>
```

## 核心组件

### 1. FormEnhanced 组件

增强型表单容器，提供完整的表单验证和管理功能。

#### Props

| 参数 | 说明 | 类型 | 默认值 |
|------|------|------|--------|
| modelValue | 表单数据 | `Record<string, any>` | 必填 |
| rules | 验证规则 | `FormRules` | `{}` |
| layout | 表单布局 | `'horizontal' \| 'vertical' \| 'inline'` | `'horizontal'` |
| labelCol | 标签布局配置 | `{ span: number; offset?: number }` | `{ span: 4 }` |
| wrapperCol | 包装器布局配置 | `{ span: number; offset?: number }` | `{ span: 20 }` |
| submitText | 提交按钮文本 | `string` | `'提交'` |
| showActions | 是否显示操作按钮 | `boolean` | `true` |
| showValidationSummary | 是否显示验证摘要 | `boolean` | `false` |
| validationSummaryTitle | 验证摘要标题 | `string` | `'表单验证失败'` |
| enableDraft | 是否启用草稿功能 | `boolean` | `false` |
| draftKey | 草稿存储键名 | `string` | `'form-draft'` |

#### Events

| 事件名 | 说明 | 回调参数 |
|--------|------|---------|
| submit | 表单提交 | `(values: any) => void` |
| reset | 表单重置 | `() => void` |
| change | 表单数据变化 | `(values: any) => void` |
| validate | 验证状态变化 | `(isValid: boolean) => void` |

#### 示例

```vue
<template>
  <FormEnhanced
    v-model="formData"
    :rules="formRules"
    layout="vertical"
    show-validation-summary
    @submit="handleSubmit"
  >
    <!-- 表单字段 -->
  </FormEnhanced>
</template>
```

### 2. FormFieldEnhanced 组件

增强型表单字段，提供实时验证和错误提示。

#### Props

| 参数 | 说明 | 类型 | 默认值 |
|------|------|------|--------|
| name | 字段名称 | `string` | 必填 |
| label | 字段标签 | `string` | `''` |
| modelValue | 字段值 | `any` | `undefined` |
| rules | 验证规则 | `ValidationRule[]` | `[]` |
| required | 是否必填 | `boolean` | `false` |
| showFeedback | 是否显示反馈图标 | `boolean` | `true` |
| showInlineFeedback | 是否显示行内反馈 | `boolean` | `false` |
| showProgress | 是否显示验证进度 | `boolean` | `false` |
| progressRules | 进度规则配置 | `ProgressRule[]` | `[]` |
| validateOnBlur | 是否在失焦时验证 | `boolean` | `true` |

#### Slots

| 插槽名 | 说明 | 作用域参数 |
|--------|------|-----------|
| default | 字段内容 | `{ value, onChange, onBlur }` |

#### 示例

```vue
<template>
  <FormFieldEnhanced
    name="password"
    label="密码"
    v-model="formData.password"
    :rules="passwordRules"
    show-progress
    :progress-rules="passwordProgressRules"
  >
    <template #default="{ value, onChange }">
      <a-input-password v-model:value="formData.password" @change="onChange" />
    </template>
  </FormFieldEnhanced>
</template>

<script setup lang="ts">
import { commonRules } from '@/utils/formValidation'

const passwordRules = [
  commonRules.required('请输入密码'),
  commonRules.minLength(6),
  commonRules.maxLength(20)
]

const passwordProgressRules = [
  { label: '长度≥6', check: (v: string) => v.length >= 6 },
  { label: '包含字母', check: (v: string) => /[a-zA-Z]/.test(v) },
  { label: '包含数字', check: (v: string) => /\d/.test(v) }
]
</script>
```

## Composable API

### useFormValidation

表单验证的核心 Composable，提供完整的验证功能。

#### 函数签名

```typescript
function useFormValidation<T extends Record<string, any>>(
  formData: Ref<T>,
  rules: FormRules,
  options?: {
    validateOnChange?: boolean
    validateOnBlur?: boolean
    debounceTime?: number
  }
): {
  errors: Ref<Record<string, string>>
  touched: Ref<Record<string, boolean>>
  dirty: Ref<Record<string, boolean>>
  validating: Ref<Record<string, boolean>>
  validateField: (field: string, value: any) => Promise<ValidationResult>
  validateAll: () => Promise<boolean>
  resetValidation: (field?: string) => void
  markTouched: (field: string) => void
  markDirty: (field: string) => void
  getFieldError: (field: string) => string | undefined
  hasError: (field?: string) => boolean
  isValid: () => boolean
}
```

#### 示例

```typescript
import { ref } from 'vue'
import { useFormValidation } from '@/utils/formValidation'

const formData = ref({
  username: '',
  email: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名' },
    { minLength: 3, message: '用户名至少3个字符' }
  ],
  email: [
    { required: true, message: '请输入邮箱' },
    { pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, message: '邮箱格式不正确' }
  ]
}

const {
  errors,
  validateField,
  validateAll,
  isValid
} = useFormValidation(formData, rules)

// 验证单个字段
await validateField('username', formData.value.username)

// 验证整个表单
const valid = await validateAll()

// 检查表单是否有效
if (isValid()) {
  // 提交表单
}
```

## 验证规则

### ValidationRule 类型

```typescript
type ValidationRule = {
  required?: boolean           // 必填
  message?: string             // 错误消息
  pattern?: RegExp             // 正则表达式
  min?: number                 // 最小值
  max?: number                 // 最大值
  minLength?: number           // 最小长度
  maxLength?: number           // 最大长度
  validator?: (value: any) => boolean | string | Promise<boolean | string>  // 自定义验证器
  trigger?: 'change' | 'blur' | 'submit'  // 触发时机
}
```

### 常用验证规则预设

```typescript
import { commonRules } from '@/utils/formValidation'

// 必填
commonRules.required('此项为必填项')

// 邮箱
commonRules.email('请输入有效的邮箱地址')

// 手机号
commonRules.phone('请输入有效的手机号码')

// 密码
commonRules.password('密码长度6-20位，需包含字母和数字')

// URL
commonRules.url('请输入有效的URL')

// 数字
commonRules.number('请输入数字')

// 整数
commonRules.integer('请输入整数')

// 长度范围
commonRules.minLength(6)
commonRules.maxLength(20)

// 数值范围
commonRules.min(0)
commonRules.max(100)

// 范围（数组）
commonRules.range(0, 100)

// 异步验证：检查唯一性
commonRules.unique(
  async (value) => {
    const response = await checkUsernameExists(value)
    return !response.data.exists
  },
  '该用户名已存在'
)
```

### 自定义验证规则

```typescript
const customRules = [
  {
    validator: (value: any) => {
      // 返回 true 表示验证通过
      if (value.length > 0) {
        return true
      }
      // 返回字符串表示验证失败
      return '至少选择一项'
    },
    trigger: 'change' as const
  },
  {
    validator: async (value: any) => {
      // 异步验证
      const response = await checkEmailUnique(value)
      return response.data.unique || '该邮箱已被注册'
    },
    trigger: 'blur' as const
  }
]
```

## 高级功能

### 1. 异步验证

```typescript
const emailRules = [
  commonRules.required('请输入邮箱'),
  commonRules.email(),
  {
    validator: async (value: string) => {
      const response = await api.checkEmailUnique(value)
      return response.data.unique || '该邮箱已被注册'
    },
    trigger: 'blur'
  }
]
```

### 2. 条件验证

```typescript
const rules = computed(() => {
  const baseRules = [
    commonRules.required('请输入值')
  ]
  
  // 根据条件添加规则
  if (formData.value.needsValidation) {
    baseRules.push({
      validator: (value: string) => {
        return value.startsWith('PREFIX_') || '必须以 PREFIX_ 开头'
      }
    })
  }
  
  return {
    field: baseRules
  }
})
```

### 3. 交叉字段验证

```typescript
const passwordRules = [
  commonRules.required('请输入密码'),
  commonRules.minLength(6)
]

const confirmPasswordRules = [
  {
    required: true,
    validator: (value: string) => {
      if (value !== formData.value.password) {
        return '两次输入的密码不一致'
      }
      return true
    },
    message: '确认密码'
  }
]
```

### 4. 草稿功能

```vue
<template>
  <FormEnhanced
    v-model="formData"
    :rules="formRules"
    enable-draft
    draft-key="user-form-draft"
  >
    <!-- 表单字段 -->
  </FormEnhanced>
</template>
```

### 5. 实时验证反馈

```vue
<template>
  <FormFieldEnhanced
    name="username"
    label="用户名"
    v-model="formData.username"
    :rules="usernameRules"
    show-inline-feedback
    show-progress
    :progress-rules="progressRules"
  >
    <a-input v-model:value="formData.username" />
  </FormFieldEnhanced>
</template>
```

### 6. 防抖/节流提交

```typescript
import { useDebouncedSubmit, useThrottledSubmit } from '@/utils/formValidation'

// 防抖提交
const { submit: debouncedSubmit, submitting: debouncing } = useDebouncedSubmit(
  async (values) => {
    await api.submitForm(values)
  },
  {
    delay: 500,
    maxWait: 2000,
    onError: (error) => {
      message.error('提交失败: ' + error.message)
    }
  }
)

// 节流提交
const { submit: throttledSubmit, submitting: throttling } = useThrottledSubmit(
  async (values) => {
    await api.submitForm(values)
  },
  {
    interval: 1000,
    onError: (error) => {
      message.error('提交失败: ' + error.message)
    }
  }
)
```

## 完整示例

### 用户注册表单

```vue
<template>
  <div class="register-form">
    <FormEnhanced
      v-model="formData"
      :rules="formRules"
      layout="vertical"
      submit-text="注册"
      show-validation-summary
      @submit="handleSubmit"
    >
      <FormFieldEnhanced
        name="username"
        label="用户名"
        v-model="formData.username"
        :rules="usernameRules"
        show-progress
        :progress-rules="usernameProgressRules"
      >
        <a-input
          v-model:value="formData.username"
          placeholder="请输入用户名"
        />
      </FormFieldEnhanced>

      <FormFieldEnhanced
        name="email"
        label="邮箱"
        v-model="formData.email"
        :rules="emailRules"
      >
        <a-input
          v-model:value="formData.email"
          placeholder="请输入邮箱"
        />
      </FormFieldEnhanced>

      <FormFieldEnhanced
        name="password"
        label="密码"
        v-model="formData.password"
        :rules="passwordRules"
        show-progress
        :progress-rules="passwordProgressRules"
      >
        <a-input-password
          v-model:value="formData.password"
          placeholder="请输入密码"
        />
      </FormFieldEnhanced>

      <FormFieldEnhanced
        name="confirmPassword"
        label="确认密码"
        v-model="formData.confirmPassword"
        :rules="confirmPasswordRules"
      >
        <a-input-password
          v-model:value="formData.confirmPassword"
          placeholder="请再次输入密码"
        />
      </FormFieldEnhanced>

      <FormFieldEnhanced
        name="phone"
        label="手机号"
        v-model="formData.phone"
        :rules="phoneRules"
      >
        <a-input
          v-model:value="formData.phone"
          placeholder="请输入手机号"
        />
      </FormFieldEnhanced>
    </FormEnhanced>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { FormEnhanced, FormFieldEnhanced } from '@/components/Form'
import { commonRules } from '@/utils/formValidation'

const formData = ref({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  phone: ''
})

// 用户名规则
const usernameRules = [
  commonRules.required('请输入用户名'),
  commonRules.minLength(3, '用户名至少3个字符'),
  commonRules.maxLength(20, '用户名最多20个字符')
]

const usernameProgressRules = [
  { label: '长度≥3', check: (v: string) => v.length >= 3 },
  { label: '仅字母数字', check: (v: string) => /^[a-zA-Z0-9]+$/.test(v) }
]

// 邮箱规则
const emailRules = [
  commonRules.required('请输入邮箱'),
  commonRules.email(),
  {
    validator: async (value: string) => {
      // 模拟异步验证
      await new Promise(resolve => setTimeout(resolve, 500))
      return !value.includes('test') || '不能使用 test 邮箱'
    },
    trigger: 'blur'
  }
]

// 密码规则
const passwordRules = [
  commonRules.required('请输入密码'),
  commonRules.minLength(6, '密码至少6个字符'),
  commonRules.maxLength(20, '密码最多20个字符')
]

const passwordProgressRules = [
  { label: '长度≥6', check: (v: string) => v.length >= 6 },
  { label: '包含字母', check: (v: string) => /[a-zA-Z]/.test(v) },
  { label: '包含数字', check: (v: string) => /\d/.test(v) }
]

// 确认密码规则
const confirmPasswordRules = [
  commonRules.required('请再次输入密码'),
  {
    validator: (value: string) => {
      if (value !== formData.value.password) {
        return '两次输入的密码不一致'
      }
      return true
    },
    message: '确认密码'
  }
]

// 手机号规则
const phoneRules = [
  commonRules.required('请输入手机号'),
  commonRules.phone()
]

const formRules = {
  username: usernameRules,
  email: emailRules,
  password: passwordRules,
  confirmPassword: confirmPasswordRules,
  phone: phoneRules
}

const handleSubmit = async (values: any) => {
  try {
    console.log('提交表单:', values)
    message.success('注册成功')
  } catch (error) {
    message.error('注册失败')
  }
}
</script>

<style scoped>
.register-form {
  max-width: 500px;
  margin: 0 auto;
  padding: 24px;
}
</style>
```

## 最佳实践

### 1. 规则组织

将验证规则集中管理，便于维护：

```typescript
// src/utils/validationRules/userFormRules.ts
import { commonRules } from '@/utils/formValidation'

export const userFormRules = {
  username: [
    commonRules.required('请输入用户名'),
    commonRules.minLength(3),
    commonRules.maxLength(20)
  ],
  email: [
    commonRules.required('请输入邮箱'),
    commonRules.email()
  ],
  // ... 其他字段
}
```

### 2. 错误消息国际化

```typescript
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const rules = {
  username: [
    commonRules.required(t('validation.usernameRequired')),
    commonRules.minLength(3)
  ]
}
```

### 3. 性能优化

- 对于复杂的异步验证，使用防抖
- 避免在每次输入时都触发验证
- 合理设置 debounceTime

```typescript
const {
  validateField
} = useFormValidation(formData, rules, {
  validateOnChange: true,
  debounceTime: 500  // 500ms 防抖
})
```

### 4. 可访问性

- 为所有表单字段提供清晰的标签
- 使用语义化的 HTML
- 确保错误消息明确且有帮助

### 5. 测试

```typescript
import { describe, it, expect } from 'vitest'
import { useFormValidation } from '@/utils/formValidation'

describe('Form Validation', () => {
  it('should validate required field', async () => {
    const formData = ref({ username: '' })
    const rules = {
      username: [{ required: true, message: '必填' }]
    }
    
    const { validateField, hasError } = useFormValidation(formData, rules)
    
    await validateField('username', '')
    expect(hasError('username')).toBe(true)
  })
})
```

## 常见问题

### Q: 如何禁用某个字段的验证？

A: 将该字段的 rules 设置为空数组或使用条件渲染：

```vue
<FormFieldEnhanced
  v-if="!disabled"
  name="field"
  :rules="fieldRules"
>
  <!-- ... -->
</FormFieldEnhanced>
```

### Q: 如何自定义错误消息样式？

A: 使用 `validate-status` 和 `help` prop：

```vue
<a-form-item
  :validate-status="hasError ? 'error' : ''"
  :help="errorMessage"
>
  <!-- ... -->
</a-form-item>
```

### Q: 如何在提交前验证表单？

A: 使用 `validateAll` 方法：

```typescript
const handleSubmit = async () => {
  const isValid = await validateAll()
  if (isValid) {
    // 提交表单
  }
}
```

## 相关资源

- [Ant Design Vue Form](https://antdv.com/components/form-cn)
- [Vue I18n](https://vue-i18n.intlify.dev/)
- [表单验证最佳实践](https://web.dev/form-validation-best-practices/)

---

如有问题，请联系开发团队。