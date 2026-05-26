<template>
  <div class="form-validation-example">
    <a-card
      title="表单验证示例"
      :bordered="false"
    >
      <a-tabs v-model:active-key="activeTab">
        <!-- 基础验证 -->
        <a-tab-pane
          key="basic"
          tab="基础验证"
        >
          <FormEnhanced
            v-model="basicForm"
            :rules="basicRules"
            @submit="handleBasicSubmit"
          >
            <FormFieldEnhanced
              v-model="basicForm.username"
              name="username"
              label="用户名"
              :rules="usernameRules"
            >
              <a-input
                v-model:value="basicForm.username"
                placeholder="请输入用户名（3-20个字符）"
              />
            </FormFieldEnhanced>

            <FormFieldEnhanced
              v-model="basicForm.email"
              name="email"
              label="邮箱"
              :rules="emailRules"
            >
              <a-input
                v-model:value="basicForm.email"
                placeholder="请输入邮箱地址"
              />
            </FormFieldEnhanced>

            <FormFieldEnhanced
              v-model="basicForm.age"
              name="age"
              label="年龄"
              :rules="ageRules"
            >
              <a-input-number
                v-model:value="basicForm.age"
                :min="1"
                :max="120"
                placeholder="请输入年龄"
                style="width: 100%"
              />
            </FormFieldEnhanced>
          </FormEnhanced>
        </a-tab-pane>

        <!-- 密码强度验证 -->
        <a-tab-pane
          key="password"
          tab="密码强度"
        >
          <FormEnhanced
            v-model="passwordForm"
            :rules="passwordRules"
            @submit="handlePasswordSubmit"
          >
            <FormFieldEnhanced
              v-model="passwordForm.password"
              name="password"
              label="密码"
              :rules="passwordValidationRules"
              show-progress
              :progress-rules="passwordProgressRules"
            >
              <a-input-password
                v-model:value="passwordForm.password"
                placeholder="请输入密码"
              />
            </FormFieldEnhanced>

            <FormFieldEnhanced
              v-model="passwordForm.confirmPassword"
              name="confirmPassword"
              label="确认密码"
              :rules="confirmPasswordRules"
            >
              <a-input-password
                v-model:value="passwordForm.confirmPassword"
                placeholder="请再次输入密码"
              />
            </FormFieldEnhanced>
          </FormEnhanced>
        </a-tab-pane>

        <!-- 异步验证 -->
        <a-tab-pane
          key="async"
          tab="异步验证"
        >
          <FormEnhanced
            v-model="asyncForm"
            :rules="asyncRules"
            @submit="handleAsyncSubmit"
          >
            <FormFieldEnhanced
              v-model="asyncForm.uniqueUsername"
              name="uniqueUsername"
              label="用户名（唯一性检查）"
              :rules="uniqueUsernameRules"
            >
              <a-input
                v-model:value="asyncForm.uniqueUsername"
                placeholder="输入用户名检查唯一性"
              />
            </FormFieldEnhanced>

            <FormFieldEnhanced
              v-model="asyncForm.uniqueEmail"
              name="uniqueEmail"
              label="邮箱（唯一性检查）"
              :rules="uniqueEmailRules"
            >
              <a-input
                v-model:value="asyncForm.uniqueEmail"
                placeholder="输入邮箱检查唯一性"
              />
            </FormFieldEnhanced>
          </FormEnhanced>

          <a-alert
            v-if="asyncValidating"
            message="正在验证..."
            type="info"
            show-icon
            style="margin-top: 16px"
          />
        </a-tab-pane>

        <!-- 条件验证 -->
        <a-tab-pane
          key="conditional"
          tab="条件验证"
        >
          <a-form-item label="启用额外验证">
            <a-switch v-model:checked="enableExtraValidation" />
          </a-form-item>

          <FormEnhanced
            v-model="conditionalForm"
            :rules="conditionalRules"
            @submit="handleConditionalSubmit"
          >
            <FormFieldEnhanced
              v-model="conditionalForm.field1"
              name="field1"
              label="基础字段"
              :rules="field1Rules"
            >
              <a-input
                v-model:value="conditionalForm.field1"
                placeholder="必填字段"
              />
            </FormFieldEnhanced>

            <FormFieldEnhanced
              v-model="conditionalForm.field2"
              name="field2"
              label="条件字段"
              :rules="field2Rules"
            >
              <a-input
                v-model:value="conditionalForm.field2"
                :placeholder="enableExtraValidation ? '需满足特殊格式' : '任意内容'"
              />
            </FormFieldEnhanced>
          </FormEnhanced>
        </a-tab-pane>

        <!-- 带草稿功能的表单 -->
        <a-tab-pane
          key="draft"
          tab="草稿功能"
        >
          <FormEnhanced
            v-model="draftForm"
            :rules="draftRules"
            enable-draft
            draft-key="example-draft-form"
            @submit="handleDraftSubmit"
          >
            <FormFieldEnhanced
              v-model="draftForm.title"
              name="title"
              label="标题"
              :rules="titleRules"
            >
              <a-input
                v-model:value="draftForm.title"
                placeholder="请输入标题"
              />
            </FormFieldEnhanced>

            <FormFieldEnhanced
              v-model="draftForm.content"
              name="content"
              label="内容"
              :rules="contentRules"
            >
              <a-textarea
                v-model:value="draftForm.content"
                placeholder="请输入内容"
                :rows="4"
              />
            </FormFieldEnhanced>
          </FormEnhanced>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { FormEnhanced, FormFieldEnhanced } from '@/components/Form'
import { commonRules, type ValidationRule } from '@/utils/formValidation'

// 当前标签页
const activeTab = ref('basic')

// ========== 基础验证表单 ==========
const basicForm = ref({
  username: '',
  email: '',
  age: undefined as number | undefined
})

const usernameRules: ValidationRule[] = [
  commonRules.required('请输入用户名'),
  commonRules.minLength(3, '用户名至少3个字符'),
  commonRules.maxLength(20, '用户名最多20个字符')
]

const emailRules: ValidationRule[] = [
  commonRules.required('请输入邮箱'),
  commonRules.email()
]

const ageRules: ValidationRule[] = [
  commonRules.required('请输入年龄'),
  commonRules.min(1, '年龄不能小于1岁'),
  commonRules.max(120, '年龄不能大于120岁')
]

const basicRules = computed(() => ({
  username: usernameRules,
  email: emailRules,
  age: ageRules
}))

const handleBasicSubmit = async (values: any) => {
  console.log('基础表单提交:', values)
  message.success('提交成功！')
}

// ========== 密码强度验证表单 ==========
const passwordForm = ref({
  password: '',
  confirmPassword: ''
})

const passwordValidationRules: ValidationRule[] = [
  commonRules.required('请输入密码'),
  commonRules.minLength(6, '密码至少6个字符'),
  commonRules.maxLength(20, '密码最多20个字符')
]

const passwordProgressRules = [
  { label: '长度≥6', check: (v: string) => v.length >= 6 },
  { label: '包含字母', check: (v: string) => /[a-zA-Z]/.test(v) },
  { label: '包含数字', check: (v: string) => /\d/.test(v) }
]

const confirmPasswordRules: ValidationRule[] = [
  commonRules.required('请再次输入密码'),
  {
    validator: (value: string) => {
      if (value !== passwordForm.value.password) {
        return '两次输入的密码不一致'
      }
      return true
    },
    message: '确认密码'
  }
]

const passwordRules = computed(() => ({
  password: passwordValidationRules,
  confirmPassword: confirmPasswordRules
}))

const handlePasswordSubmit = async (values: any) => {
  console.log('密码表单提交:', values)
  message.success('密码设置成功！')
}

// ========== 异步验证表单 ==========
const asyncForm = ref({
  uniqueUsername: '',
  uniqueEmail: ''
})

const asyncValidating = ref(false)

const uniqueUsernameRules: ValidationRule[] = [
  commonRules.required('请输入用户名'),
  {
    validator: async (value: string) => {
      if (!value) return true
      asyncValidating.value = true
      
      // 模拟异步验证
      await new Promise(resolve => setTimeout(resolve, 1000))
      
      asyncValidating.value = false
      
      // 模拟：用户名不能包含 "admin"
      if (value.toLowerCase().includes('admin')) {
        return '该用户名已被占用'
      }
      return true
    },
    trigger: 'blur'
  }
]

const uniqueEmailRules: ValidationRule[] = [
  commonRules.required('请输入邮箱'),
  commonRules.email(),
  {
    validator: async (value: string) => {
      if (!value) return true
      asyncValidating.value = true
      
      // 模拟异步验证
      await new Promise(resolve => setTimeout(resolve, 800))
      
      asyncValidating.value = false
      
      // 模拟：test.com 邮箱已被注册
      if (value.includes('test.com')) {
        return '该邮箱已被注册'
      }
      return true
    },
    trigger: 'blur'
  }
]

const asyncRules = computed(() => ({
  uniqueUsername: uniqueUsernameRules,
  uniqueEmail: uniqueEmailRules
}))

const handleAsyncSubmit = async (values: any) => {
  console.log('异步验证表单提交:', values)
  message.success('验证通过，提交成功！')
}

// ========== 条件验证表单 ==========
const enableExtraValidation = ref(false)

const conditionalForm = ref({
  field1: '',
  field2: ''
})

const field1Rules: ValidationRule[] = [
  commonRules.required('此字段为必填项')
]

const field2Rules = computed<ValidationRule[]>(() => {
  const baseRules = [
    commonRules.required('此字段为必填项')
  ]
  
  if (enableExtraValidation.value) {
    baseRules.push({
      validator: (value: string) => {
        return value.startsWith('PREFIX_') || '必须以 PREFIX_ 开头'
      }
    })
  }
  
  return baseRules
})

const conditionalRules = computed(() => ({
  field1: field1Rules,
  field2: field2Rules.value
}))

const handleConditionalSubmit = async (values: any) => {
  console.log('条件验证表单提交:', values)
  message.success('提交成功！')
}

// ========== 草稿功能表单 ==========
const draftForm = ref({
  title: '',
  content: ''
})

const titleRules: ValidationRule[] = [
  commonRules.required('请输入标题'),
  commonRules.minLength(2, '标题至少2个字符'),
  commonRules.maxLength(100, '标题最多100个字符')
]

const contentRules: ValidationRule[] = [
  commonRules.required('请输入内容'),
  commonRules.minLength(10, '内容至少10个字符')
]

const draftRules = computed(() => ({
  title: titleRules,
  content: contentRules
}))

const handleDraftSubmit = async (values: any) => {
  console.log('草稿表单提交:', values)
  message.success('提交成功！')
}
</script>

<style scoped>
.form-validation-example {
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
}

:deep(.ant-form-item) {
  margin-bottom: 20px;
}

:deep(.validation-progress) {
  margin-top: 8px;
}

:deep(.progress-items) {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  font-size: 12px;
}

:deep(.progress-item) {
  color: #d9d9d9;
}

:deep(.progress-item.checked) {
  color: #52c41a;
}
</style>