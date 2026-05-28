<template>
  <div class="login-container">
    <div class="login-background">
      <div class="background-shapes">
        <div class="shape shape-1" />
        <div class="shape shape-2" />
        <div class="shape shape-3" />
      </div>
    </div>

    <div class="login-wrapper">
      <div class="login-box">
        <div class="login-header">
          <div class="logo">
            <div class="logo-icon">
              🚀
            </div>
            <h1>企智连·AI-Ready</h1>
          </div>
          <p>企业智能管理系统</p>
        </div>
        
        <a-form
          ref="formRef"
          :model="formState"
          :rules="rules"
          layout="vertical"
          @finish="handleSubmit"
        >
          <a-form-item name="username">
            <a-input
              v-model:value="formState.username"
              size="large"
              placeholder="请输入用户名"
              @keyup.enter="focusNextInput('password')"
            >
              <template #prefix>
                <UserOutlined />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item name="password">
            <a-input-password
              ref="passwordInput"
              v-model:value="formState.password"
              size="large"
              placeholder="请输入密码"
              @keyup.enter="focusNextInput('captcha')"
            >
              <template #prefix>
                <LockOutlined />
              </template>
            </a-input-password>
          </a-form-item>

          <!-- 验证码区域 -->
          <a-form-item name="captcha">
            <div class="captcha-container">
              <a-input
                ref="captchaInput"
                v-model:value="formState.captcha"
                size="large"
                placeholder="请输入验证码"
                class="captcha-input"
                @keyup.enter="handleSubmit"
              >
                <template #prefix>
                  <SafetyOutlined />
                </template>
              </a-input>
              <div
                class="captcha-image"
                @click="refreshCaptcha"
              >
                <img
                  v-if="captchaUrl"
                  :src="captchaUrl"
                  alt="验证码"
                >
                <div
                  v-else
                  class="captcha-loading"
                >
                  <LoadingOutlined />
                </div>
              </div>
            </div>
          </a-form-item>

          <div class="login-options">
            <a-checkbox v-model:checked="rememberMe">
              <span class="remember-text">记住我</span>
            </a-checkbox>
            <a
              class="forgot-password"
              @click="handleForgotPassword"
            >忘记密码？</a>
          </div>

          <a-form-item>
            <a-button
              type="primary"
              html-type="submit"
              size="large"
              :loading="loading"
              block
              class="login-button"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </a-button>
          </a-form-item>

          <div class="login-footer">
            <span class="footer-text">还没有账号？</span>
            <a
              class="register-link"
              @click="handleRegister"
            >立即注册</a>
          </div>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  UserOutlined,
  LockOutlined,
  SafetyOutlined,
  LoadingOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { setupDynamicRoutes } from '@/router'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 表单引用
const formRef = ref<FormInstance>()
const passwordInput = ref()
const captchaInput = ref()

// 状态管理
const loading = ref(false)
const rememberMe = ref(false)
const captchaUrl = ref('')
const captchaKey = ref('')

// 表单数据
const formState = reactive({
  username: '',
  password: '',
  captcha: '',
  tenantId: 1
})

// 表单验证规则
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 个字符', trigger: 'blur' }
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 4, message: '请输入 4 位验证码', trigger: 'blur' }
  ]
}

// 获取验证码
const fetchCaptcha = async () => {
  try {
    captchaKey.value = Date.now().toString()
    // 模拟验证码API调用，实际项目中替换为真实的API
    // const response = await axios.get('/api/captcha', {
    //   params: { key: captchaKey.value }
    // })
    // captchaUrl.value = response.data.image
    
    // 临时模拟验证码图片
    captchaUrl.value = `data:image/svg+xml,${encodeURIComponent(`
      <svg xmlns="http://www.w3.org/2000/svg" width="100" height="40">
        <rect width="100%" height="100%" fill="#f0f0f0"/>
        <text x="10" y="25" font-family="Arial" font-size="20" font-weight="bold" fill="#333">
          ${Math.random().toString(36).substring(2, 6).toUpperCase()}
        </text>
      </svg>
    `)}`
    
  } catch (error) {
    console.error('获取验证码失败:', error)
    message.warning('获取验证码失败，请刷新重试')
  }
}

// 刷新验证码
const refreshCaptcha = () => {
  captchaUrl.value = ''
  formState.captcha = ''
  fetchCaptcha()
}

// 聚焦下一个输入框
const focusNextInput = (inputName: string) => {
  if (inputName === 'password' && passwordInput.value) {
    passwordInput.value.focus()
  } else if (inputName === 'captcha' && captchaInput.value) {
    captchaInput.value.focus()
  }
}

// 处理登录提交
const handleSubmit = async () => {
  try {
    loading.value = true
    
    // 表单验证
    await formRef.value?.validate()
    
    // 执行登录
    const success = await userStore.login({
      username: formState.username,
      password: formState.password,
      tenantId: formState.tenantId
    })
    
    if (success) {
      message.success('登录成功，欢迎回来！')
      
      // 处理记住我功能
      if (rememberMe.value) {
        localStorage.setItem('rememberedUsername', formState.username)
      } else {
        localStorage.removeItem('rememberedUsername')
      }
      
      // 加载动态路由
      await setupDynamicRoutes()
      
      // 跳转到目标页面或首页
      const redirect = (route.query.redirect as string) || '/dashboard'
      await router.push(redirect)
    } else {
      message.error('登录失败，请检查用户名和密码')
      refreshCaptcha()
    }
  } catch (error: any) {
    console.error('登录失败:', error)
    const errorMsg = error?.message || error?.response?.data?.message || '登录失败，请稍后重试'
    message.error(errorMsg)
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

// 忘记密码
const handleForgotPassword = () => {
  message.info('请联系管理员重置密码')
}

// 注册账号
const handleRegister = () => {
  router.push('/register')
}

// 初始化
onMounted(() => {
  // 检查是否有记住的用户名
  const rememberedUsername = localStorage.getItem('rememberedUsername')
  if (rememberedUsername) {
    formState.username = rememberedUsername
    rememberMe.value = true
  }
  
  // 获取验证码
  fetchCaptcha()
})
</script>

<style scoped>
.login-container {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  overflow: hidden;
}

.login-background {
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
  background: rgba(255, 255, 255, 0.1);
  top: -100px;
  left: -100px;
  animation-delay: 0s;
}

.shape-2 {
  width: 300px;
  height: 300px;
  background: rgba(255, 255, 255, 0.1);
  bottom: -50px;
  right: -50px;
  animation-delay: 5s;
}

.shape-3 {
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.1);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: 10s;
}

@keyframes float {
  0%, 100% {
    transform: translate(0, 0) rotate(0deg);
  }
  33% {
    transform: translate(30px, -30px) rotate(120deg);
  }
  66% {
    transform: translate(-20px, 20px) rotate(240deg);
  }
}

.login-wrapper {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  padding: 20px;
}

.login-box {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  padding: 48px 40px;
  animation: slideUp 0.6s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.logo {
  margin-bottom: 16px;
}

.logo-icon {
  font-size: 48px;
  margin-bottom: 12px;
  animation: bounce 2s infinite;
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-10px);
  }
  60% {
    transform: translateY(-5px);
  }
}

.login-header h1 {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0 0 8px 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.login-header p {
  color: #666;
  font-size: 14px;
  margin: 0;
}

/* 验证码容器 */
.captcha-container {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-input {
  flex: 1;
}

.captcha-image {
  width: 120px;
  height: 40px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  transition: all 0.3s;
}

.captcha-image:hover {
  border-color: #667eea;
  transform: scale(1.02);
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  background: #fff;
}

.captcha-loading {
  color: #999;
  font-size: 20px;
}

/* 登录选项 */
.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.remember-text {
  color: #666;
}

.forgot-password {
  color: #667eea;
  font-weight: 500;
}

.forgot-password:hover {
  color: #764ba2;
}

/* 登录按钮 */
.login-button {
  height: 44px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  transition: all 0.3s;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.login-button:active {
  transform: translateY(0);
}

/* 登录页脚 */
.login-footer {
  text-align: center;
  margin-top: 16px;
}

.footer-text {
  color: #999;
  font-size: 14px;
}

.register-link {
  color: #667eea;
  font-weight: 500;
  margin-left: 4px;
}

.register-link:hover {
  color: #764ba2;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .login-wrapper {
    padding: 16px;
  }

  .login-box {
    padding: 32px 24px;
  }

  .login-header h1 {
    font-size: 24px;
  }

  .captcha-container {
    flex-direction: column;
    align-items: stretch;
  }

  .captcha-image {
    width: 100%;
    height: 48px;
  }
}

/* 输入框样式优化 */
:deep(.ant-input-affix-wrapper),
:deep(.ant-input) {
  border-radius: 8px;
  padding: 10px 12px;
}

:deep(.ant-input-affix-wrapper:focus),
:deep(.ant-input:focus) {
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

:deep(.ant-form-item-explain-error) {
  font-size: 12px;
  padding-top: 4px;
}
</style>