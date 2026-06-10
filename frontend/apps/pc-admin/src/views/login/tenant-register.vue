<template>
  <div class="register-container">
    <div class="register-background" aria-hidden="true">
      <div class="background-shapes">
        <div class="shape shape-1" />
        <div class="shape shape-2" />
        <div class="shape shape-3" />
      </div>
    </div>

    <div class="register-wrapper">
      <a-card class="register-card" :bordered="false">
        <template #title>
          <div class="register-header">
            <h2>企业租户注册</h2>
            <p>创建您的企业账号，开通专属管理系统</p>
          </div>
        </template>

        <a-steps :current="currentStep" size="small" class="steps">
          <a-step title="企业信息" />
          <a-step title="管理员账号" />
          <a-step title="提交审核" />
        </a-steps>

        <!-- 步骤 1：企业信息 -->
        <div v-show="currentStep === 0" class="step-content">
          <a-form
            ref="formRef1"
            :model="formState"
            :rules="rules1"
            layout="vertical"
          >
            <a-form-item label="企业名称" name="tenantName">
              <a-input v-model:value="formState.tenantName" placeholder="请输入企业名称" size="large" />
            </a-form-item>

            <a-form-item label="企业编码" name="tenantCode">
              <a-input v-model:value="formState.tenantCode" placeholder="英文/数字，用于唯一标识" size="large" />
            </a-form-item>

            <a-form-item label="联系人" name="contactPerson">
              <a-input v-model:value="formState.contactPerson" placeholder="请输入联系人姓名" size="large" />
            </a-form-item>

            <a-form-item label="联系电话" name="contactPhone">
              <a-input v-model:value="formState.contactPhone" placeholder="请输入手机号码" size="large" />
            </a-form-item>

            <a-form-item label="联系邮箱" name="contactEmail">
              <a-input v-model:value="formState.contactEmail" placeholder="请输入邮箱地址" size="large" />
            </a-form-item>
          </a-form>
        </div>

        <!-- 步骤 2：管理员账号 -->
        <div v-show="currentStep === 1" class="step-content">
          <a-form
            ref="formRef2"
            :model="formState"
            :rules="rules2"
            layout="vertical"
          >
            <a-form-item label="管理员用户名" name="adminUsername">
              <a-input v-model:value="formState.adminUsername" placeholder="3-20 位字母/数字" size="large" />
            </a-form-item>

            <a-form-item label="管理员邮箱" name="adminEmail">
              <a-input v-model:value="formState.adminEmail" placeholder="用于接收通知和找回密码" size="large" />
            </a-form-item>

            <a-form-item label="登录密码" name="adminPassword">
              <a-input-password v-model:value="formState.adminPassword" placeholder="至少 6 位" size="large" />
            </a-form-item>

            <a-form-item label="确认密码" name="confirmPassword">
              <a-input-password v-model:value="formState.confirmPassword" placeholder="请再次输入密码" size="large" />
            </a-form-item>
          </a-form>
        </div>

        <!-- 步骤 3：提交审核 -->
        <div v-show="currentStep === 2" class="step-content">
          <a-result status="info" title="确认注册信息">
            <template #subTitle>
              <p>请确认以下信息无误后提交，我们将尽快审核</p>
            </template>
            <template #extra>
              <a-descriptions :column="1" bordered size="small">
                <a-descriptions-item label="企业名称">{{ formState.tenantName }}</a-descriptions-item>
                <a-descriptions-item label="企业编码">{{ formState.tenantCode }}</a-descriptions-item>
                <a-descriptions-item label="联系人">{{ formState.contactPerson }}</a-descriptions-item>
                <a-descriptions-item label="联系电话">{{ formState.contactPhone }}</a-descriptions-item>
                <a-descriptions-item label="联系邮箱">{{ formState.contactEmail }}</a-descriptions-item>
                <a-descriptions-item label="管理员">{{ formState.adminUsername }}</a-descriptions-item>
                <a-descriptions-item label="管理员邮箱">{{ formState.adminEmail }}</a-descriptions-item>
              </a-descriptions>
            </template>
          </a-result>
        </div>

        <!-- 按钮组 -->
        <div class="step-actions">
          <a-button v-if="currentStep > 0 && !submitted" @click="prevStep">
            上一步
          </a-button>
          <div style="flex: 1" />
          <a-button v-if="currentStep < 2 && !submitted" type="primary" @click="nextStep">
            下一步
          </a-button>
          <a-button v-if="currentStep === 2 && !submitted" type="primary" :loading="submitting" @click="handleSubmit">
            提交审核
          </a-button>
        </div>

        <!-- 提交成功 -->
        <div v-if="submitted" class="step-content">
          <a-result
            status="success"
            title="注册申请已提交"
            :sub-title="`${formState.tenantName} 的注册申请已成功提交，请等待管理员审核。审核结果将发送至 ${formState.contactEmail}`"
          >
            <template #extra>
              <a-button type="primary" @click="goToLogin">
                返回登录
              </a-button>
            </template>
          </a-result>
        </div>

        <div class="register-footer">
          已有账号？
          <router-link to="/login">立即登录</router-link>
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { tenantApprovalApi } from '@/api/tenant'

const router = useRouter()
const currentStep = ref(0)
const submitting = ref(false)
const submitted = ref(false)
const formRef1 = ref()
const formRef2 = ref()

interface FormState {
  tenantName: string
  tenantCode: string
  contactPerson: string
  contactPhone: string
  contactEmail: string
  adminUsername: string
  adminEmail: string
  adminPassword: string
  confirmPassword: string
}

const formState = reactive<FormState>({
  tenantName: '',
  tenantCode: '',
  contactPerson: '',
  contactPhone: '',
  contactEmail: '',
  adminUsername: '',
  adminEmail: '',
  adminPassword: '',
  confirmPassword: ''
})

// 步骤 1 校验规则
const rules1: Record<string, Rule[]> = {
  tenantName: [
    { required: true, message: '请输入企业名称', trigger: 'blur' },
    { min: 2, max: 50, message: '企业名称长度 2-50 个字符', trigger: 'blur' }
  ],
  tenantCode: [
    { required: true, message: '请输入企业编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_-]{2,30}$/, message: '限 2-30 位字母/数字/下划线/中划线', trigger: 'blur' }
  ],
  contactPerson: [
    { required: true, message: '请输入联系人', trigger: 'blur' },
    { max: 20, message: '联系人姓名最长 20 个字符', trigger: 'blur' }
  ],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码', trigger: 'blur' }
  ],
  contactEmail: [
    { required: true, message: '请输入联系邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ]
}

// 步骤 2 校验规则
const validatePassword = async (_rule: Rule, value: string) => {
  if (value === '') return Promise.reject('请输入密码')
  if (value.length < 6) return Promise.reject('密码长度不能少于 6 位')
  return Promise.resolve()
}

const validateConfirmPassword = async (_rule: Rule, value: string) => {
  if (value === '') return Promise.reject('请再次输入密码')
  if (value !== formState.adminPassword) return Promise.reject('两次输入的密码不一致')
  return Promise.resolve()
}

const rules2: Record<string, Rule[]> = {
  adminUsername: [
    { required: true, message: '请输入管理员用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度 3-20 个字符', trigger: 'blur' }
  ],
  adminEmail: [
    { required: true, message: '请输入管理员邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  adminPassword: [
    { required: true, validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const nextStep = async () => {
  try {
    if (currentStep.value === 0) {
      await formRef1.value?.validate()
    } else if (currentStep.value === 1) {
      await formRef2.value?.validate()
    }
    currentStep.value++
  } catch {
    // 表单验证失败，停留在当前步骤
  }
}

const prevStep = () => {
  if (currentStep.value > 0) currentStep.value--
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    await tenantApprovalApi.register({
      tenantName: formState.tenantName,
      tenantCode: formState.tenantCode,
      contactPerson: formState.contactPerson,
      contactPhone: formState.contactPhone,
      contactEmail: formState.contactEmail,
      adminUsername: formState.adminUsername,
      adminPassword: formState.adminPassword,
      adminEmail: formState.adminEmail
    })
    submitted.value = true
    message.success('注册申请已提交，请等待审核')
  } catch (error: any) {
    const msg = error?.response?.data?.message || error?.message || '注册失败，请重试'
    message.error(msg)
  } finally {
    submitting.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<style scoped>
.register-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: 100vh;
  padding: 40px 20px;
  overflow: hidden;
}

.register-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  z-index: 0;
}

.background-shapes {
  position: absolute;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.1;
  animation: float 20s infinite ease-in-out;
}

.shape-1 {
  width: 400px;
  height: 400px;
  top: -100px;
  left: -100px;
  animation-delay: 0s;
}

.shape-2 {
  width: 300px;
  height: 300px;
  bottom: -50px;
  right: -50px;
  animation-delay: 5s;
}

.shape-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  left: 50%;
  animation-delay: 10s;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) rotate(0deg); }
  33% { transform: translate(30px, -30px) rotate(120deg); }
  66% { transform: translate(-20px, 20px) rotate(240deg); }
}

.register-wrapper {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 560px;
}

.register-card {
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
}

.register-header {
  text-align: center;
  padding: 8px 0;
}

.register-header h2 {
  margin: 0 0 4px;
  font-size: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.register-header p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.steps {
  margin: 24px 0;
}

.step-content {
  padding: 16px 0;
  min-height: 280px;
}

.step-actions {
  display: flex;
  margin-top: 24px;
  gap: 12px;
}

.register-footer {
  text-align: center;
  margin-top: 16px;
  color: #666;
}

.register-footer a {
  color: #1890ff;
}
</style>
