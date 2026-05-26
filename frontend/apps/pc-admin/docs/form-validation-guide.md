# AI-Ready 前端表单验证优化文档

## 概述

本项目实现了完整的表单验证优化系统，包括：
- 增强的表单验证规则配置
- 实时验证反馈
- 表单提交防抖/节流
- 表单数据持久化
- 表单草稿箱功能

## 核心文件

```
src/
├── utils/
│   ├── formValidation.ts          # 表单验证核心逻辑
│   └── formPersistence.ts         # 表单持久化核心逻辑
├── components/Form/
│   ├── FormEnhanced.vue           # 增强表单组件
│   ├── FormFieldEnhanced.vue      # 增强表单字段组件
│   └── index.ts                   # 导出索引
```

## 使用方法

### 1. 基本表单验证

```vue
<template>
  <FormEnhanced
    v-model="formData"
    :rules="rules"
    :field-labels="fieldLabels"
    @submit="handleSubmit"
  >
    <a-input v-model:value="formData.username" placeholder="用户名" />
    <a-input v-model:value="formData.email" placeholder="邮箱" />
    <a-input-password v-model:value="formData.password" placeholder="密码" />
  </FormEnhanced>
</template>

<script setup>
import { ref } from 'vue'
import { FormEnhanced, commonRules } from '@/components/Form'

const formData = ref({
  username: '',
  email: '',
  password: ''
})

const rules = {
  username: [
    commonRules.required('请输入用户名'),
    commonRules.minLength(3, '用户名至少3个字符'),
    commonRules.maxLength(20, '用户名最多20个字符')
  ],
  email: [
    commonRules.required('请输入邮箱'),
    commonRules.email('请输入有效的邮箱地址')
  ],
  password: [
    commonRules.required('请输入密码'),
    commonRules.password('密码6-20位，需包含字母和数字')
  ]
}

const fieldLabels = {
  username: '用户名',
  email: '邮箱',
  password: '密码'
}

const handleSubmit = async (data) => {
  console.log('提交数据:', data)
  // 调用 API...
}
</script>
```

### 2. 增强表单字段（实时验证反馈）

```vue
<template>
  <a-form :model="formData">
    <FormFieldEnhanced
      name="username"
      label="用户名"
      v-model="formData.username"
      :rules="usernameRules"
      :show-inline-feedback="true"
      :show-progress="true"
    >
      <template #default="{ value, onChange }">
        <a-input :value="value" @change="onChange" placeholder="请输入用户名" />
      </template>
    </FormFieldEnhanced>
    
    <FormFieldEnhanced
      name="email"
      label="邮箱"
      v-model="formData.email"
      :rules="emailRules"
      :show-inline-feedback="true"
    >
      <template #default="{ value, onChange }">
        <a-input :value="value" @change="onChange" placeholder="请输入邮箱" />
      </template>
    </FormFieldEnhanced>
  </a-form>
</template>

<script setup>
import { ref } from 'vue'
import { FormFieldEnhanced, commonRules } from '@/components/Form'

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
</script>
```

### 3. 表单提交防抖

```vue
<template>
  <a-button 
    type="primary" 
    :loading="submitting"
    @click="submit"
  >
    提交
  </a-button>
</template>

<script setup>
import { useDebouncedSubmit } from '@/components/Form'

const handleSubmit = async (data) => {
  await api.submitForm(data)
}

const { submit, submitting, cancel } = useDebouncedSubmit(
  handleSubmit,
  {
    delay: 500,
    leading: false,
    onError: (err) => {
      console.error('提交失败:', err)
    }
  }
)
</script>
```

### 4. 表单数据持久化

```vue
<template>
  <a-form :model="formData">
    <!-- 表单内容 -->
  </a-form>
  
  <a-space>
    <a-button @click="persistence.save()">手动保存</a-button>
    <a-button @click="persistence.restore()">恢复数据</a-button>
    <a-button @click="persistence.clear()">清除存储</a-button>
  </a-space>
</template>

<script setup>
import { ref } from 'vue'
import { useFormPersistence } from '@/components/Form'

const formData = ref({
  username: '',
  email: ''
})

const persistence = useFormPersistence(formData, {
  key: 'user_form_data',
  storage: 'localStorage',
  expire: 24 * 60 * 60 * 1000, // 24小时过期
  exclude: ['password'] // 排除密码字段不保存
})
</script>
```

### 5. 表单草稿箱

```vue
<template>
  <FormEnhanced
    v-model="formData"
    :rules="rules"
    :enable-draft="true"
    :draft-key="user_form"
    :show-restore-draft="true"
    @draft-saved="handleDraftSaved"
    @draft-applied="handleDraftApplied"
  >
    <!-- 表单内容 -->
  </FormEnhanced>
</template>

<script setup>
import { ref } from 'vue'
import { FormEnhanced } from '@/components/Form'

const formData = ref({
  title: '',
  content: ''
})

const handleDraftSaved = (draftId) => {
  console.log('草稿已保存:', draftId)
}

const handleDraftApplied = (draftId) => {
  console.log('草稿已恢复:', draftId)
}
</script>
```

## 验证规则预设

`commonRules` 提供常用验证规则：

| 规则名 | 用途 | 示例 |
|--------|------|------|
| required | 必填验证 | `commonRules.required('此项必填')` |
| email | 邮箱验证 | `commonRules.email('请输入有效邮箱')` |
| phone | 手机号验证 | `commonRules.phone('请输入有效手机号')` |
| password | 密码验证 | `commonRules.password()` |
| url | URL验证 | `commonRules.url()` |
| number | 数字验证 | `commonRules.number()` |
| minLength | 最小长度 | `commonRules.minLength(6)` |
| maxLength | 最大长度 | `commonRules.maxLength(20)` |
| min | 最小值 | `commonRules.min(0)` |
| max | 最大值 | `commonRules.max(100)` |
| range | 数值范围 | `commonRules.range(0, 100)` |
| unique | 异步唯一性验证 | `commonRules.unique(checkFn, '值已存在')` |

## 自定义验证器

```typescript
// 异步验证示例
const rules = {
  username: [
    commonRules.required(),
    commonRules.unique(
      async (value) => {
        const result = await api.checkUsername(value)
        return result.available
      },
      '用户名已被使用'
    )
  ]
}

// 自定义复杂验证
const customValidator = (value) => {
  if (value.includes('admin')) {
    return '不能包含敏感词'
  }
  if (!/^[a-zA-Z0-9]+$/.test(value)) {
    return '只能包含字母和数字'
  }
  return true // 验证通过
}
```

## 表单持久化配置

```typescript
interface PersistenceOptions {
  key: string                    // 存储键名
  storage?: 'localStorage'       // 存储类型
              | 'sessionStorage'
              | 'memory'
  debounceTime?: number          // 防抖时间(ms)
  include?: string[]             // 包含字段
  exclude?: string[]             // 排除字段
  expire?: number                // 过期时间(ms)
  encrypt?: boolean              // 是否加密
  onRestore?: (data: any) => any // 恢复回调
  onSave?: (data: any) => any    // 保存回调
}
```

## 表单草稿配置

```typescript
interface FormDraftOptions {
  maxDrafts?: number   // 最大草稿数(默认10)
  expireDays?: number  // 过期天数(默认7)
}
```

## 最佳实践

1. **组合使用**: 结合 FormEnhanced 和 FormFieldEnhanced 实现最佳体验
2. **实时反馈**: 对重要字段开启 showInlineFeedback 和 showProgress
3. **防抖提交**: 对提交按钮使用防抖避免重复提交
4. **持久化**: 对长表单使用持久化防止数据丢失
5. **草稿箱**: 对编辑类表单提供草稿保存功能
6. **验证规则**: 复用 commonRules 保持验证逻辑一致

## Props 说明

### FormEnhanced

| Prop | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| modelValue | Object | required | 表单数据 |
| rules | Object | {} | 验证规则 |
| submitText | String | '提交' | 提交按钮文字 |
| showActions | Boolean | true | 显示操作按钮 |
| showValidationSummary | Boolean | false | 显示验证摘要 |
| enableDraft | Boolean | false | 启用草稿功能 |
| debounceSubmit | Number | 500 | 提交防抖时间 |

### FormFieldEnhanced

| Prop | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| name | String | required | 字段名 |
| label | String | '' | 标签文字 |
| modelValue | Any | undefined | 字段值 |
| rules | Array | [] | 验证规则 |
| showFeedback | Boolean | true | 显示验证反馈 |
| showInlineFeedback | Boolean | false | 显示内联反馈 |
| showProgress | Boolean | false | 显示验证进度 |
| debounceTime | Number | 300 | 验证防抖时间 |

## 注意事项

1. FormEnhanced 内部集成了 Ant Design 表单，无需额外配置
2. 验证规则同时应用于内部状态和 Ant Design 规则
3. 持久化数据默认存储在 localStorage，注意敏感数据排除
4. 草稿功能独立于持久化，可同时使用
5. 防抖提交在组件卸载时会自动取消未完成的提交

---

_表单验证优化完成。如有问题请查看组件源码或联系开发团队。_