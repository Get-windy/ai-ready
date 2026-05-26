<template>
  <div class="register-container">
    <a-card
      class="register-card"
      :bordered="false"
    >
      <h2 class="register-title">
        注册账号
      </h2>
      <a-form
        :model="formState"
        :rules="rules"
        layout="vertical"
        @finish="handleRegister"
      >
        <a-form-item
          label="用户名"
          name="username"
        >
          <a-input
            v-model:value="formState.username"
            placeholder="请输入用户名"
            size="large"
          />
        </a-form-item>

        <a-form-item
          label="邮箱"
          name="email"
        >
          <a-input
            v-model:value="formState.email"
            placeholder="请输入邮箱"
            size="large"
          />
        </a-form-item>

        <a-form-item
          label="密码"
          name="password"
        >
          <a-input-password
            v-model:value="formState.password"
            placeholder="请输入密码"
            size="large"
          />
        </a-form-item>

        <a-form-item
          label="确认密码"
          name="confirmPassword"
        >
          <a-input-password
            v-model:value="formState.confirmPassword"
            placeholder="请再次输入密码"
            size="large"
          />
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            :loading="loading"
            block
          >
            注册
          </a-button>
        </a-form-item>

        <div class="register-footer">
          已有账号？
          <router-link to="/login">
            立即登录
          </router-link>
        </div>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'

const router = useRouter()
const loading = ref(false)

const formState = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const validatePassword = async (_rule: Rule, value: string) => {
  if (value === '') {
    return Promise.reject('请输入密码')
  }
  if (value.length < 6) {
    return Promise.reject('密码长度不能少于 6 位')
  }
  return Promise.resolve()
}

const validateConfirmPassword = async (_rule: Rule, value: string) => {
  if (value === '') {
    return Promise.reject('请再次输入密码')
  }
  if (value !== formState.password) {
    return Promise.reject('两次输入的密码不一致')
  }
  return Promise.resolve()
}

const rules: Record<string, Rule[]> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  password: [
    { required: true, validator: validatePassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  loading.value = true
  try {
    // TODO: 实现注册逻辑
    message.success('注册成功，请登录')
    router.push('/login')
  } catch (error) {
    message.error('注册失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-card {
  width: 400px;
  padding: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.register-title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.register-footer {
  text-align: center;
  margin-top: 16px;
  color: #666;
}

.register-footer a {
  color: #1890ff;
  text-decoration: none;
}

.register-footer a:hover {
  text-decoration: underline;
}
</style>