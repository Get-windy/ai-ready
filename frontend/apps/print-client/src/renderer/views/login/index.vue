<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import message from '@/utils/message'

const router = useRouter()
const route = useRoute()

const serverUrl = ref('')
const tenantName = ref('')
const username = ref('')
const password = ref('')
const rememberMe = ref(true)
const loading = ref(false)
const showPassword = ref(false)
const hasSavedServer = ref(false)

onMounted(async () => {
  // 读取已保存的服务器地址和记住的凭据
  const savedUrl = await window.electronAPI.settings.getSetting('serverUrl') as string
  if (savedUrl) {
    serverUrl.value = savedUrl
    hasSavedServer.value = true
  }

  // 如果记住密码，自动填充
  const credentials = await window.electronAPI.auth.getCredentials()
  if (credentials) {
    tenantName.value = credentials.tenantName || ''
    username.value = credentials.username || ''
    password.value = credentials.password || ''
    rememberMe.value = true
  }
})

const handleLogin = async () => {
  if (!serverUrl.value) {
    message.warning('请输入服务器地址')
    return
  }
  if (!tenantName.value) {
    message.warning('请输入租户名称')
    return
  }
  if (!username.value) {
    message.warning('请输入用户名')
    return
  }
  if (!password.value) {
    message.warning('请输入密码')
    return
  }

  loading.value = true
  try {
    const result = await window.electronAPI.auth.login(
      serverUrl.value,
      tenantName.value,
      username.value,
      password.value,
      rememberMe.value
    )

    message.success('登录成功')

    // 判断是否需要注册客户端
    if (result.client && result.client.clientId) {
      // 已注册过，直接跳转首页
      router.push('/dashboard')
    } else {
      // 未注册，跳转注册页面
      router.push('/register')
    }
  } catch (err: any) {
    message.error(err.message || '登录失败，请检查网络连接和账号信息')
  } finally {
    loading.value = false
  }
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter') {
    handleLogin()
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-header">
        <div class="login-logo">
          <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
            <rect width="48" height="48" rx="12" fill="#1988fa"/>
            <path d="M14 24l6 6 14-14" stroke="white" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
        <h1 class="login-title">智企连打印客户端</h1>
        <p class="login-subtitle">请使用租户账号登录</p>
      </div>

      <div class="login-form" @keydown="handleKeydown">
        <div class="form-item">
          <label class="form-label">服务器地址</label>
          <input
            v-model="serverUrl"
            type="url"
            class="form-input"
            placeholder="例如: http://192.168.1.100:8080"
            autocomplete="url"
          />
        </div>

        <div class="form-item">
          <label class="form-label">租户名称</label>
          <input
            v-model="tenantName"
            type="text"
            class="form-input"
            placeholder="请输入租户名称"
            autocomplete="username"
          />
        </div>

        <div class="form-item">
          <label class="form-label">用户名</label>
          <input
            v-model="username"
            type="text"
            class="form-input"
            placeholder="请输入用户名"
            autocomplete="username"
          />
        </div>

        <div class="form-item">
          <label class="form-label">密码</label>
          <div class="password-wrapper">
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              class="form-input"
              placeholder="请输入密码"
              autocomplete="current-password"
            />
            <button class="toggle-password" @click="showPassword = !showPassword">
              {{ showPassword ? '隐藏' : '显示' }}
            </button>
          </div>
        </div>

        <div class="form-item remember-row">
          <label class="checkbox-label">
            <input type="checkbox" v-model="rememberMe" />
            <span class="checkbox-text">记住密码</span>
          </label>
        </div>

        <button
          class="login-button"
          :class="{ loading }"
          :disabled="loading"
          @click="handleLogin"
        >
          {{ loading ? '登录中...' : '登 录' }}
        </button>
      </div>

      <div class="login-footer">
        <span class="version-text">v1.0.0</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-app-region: drag;
}

.login-container {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  -webkit-app-region: no-drag;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-logo {
  margin-bottom: 16px;
}

.login-title {
  font-size: 22px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px;
}

.login-subtitle {
  font-size: 14px;
  color: #969799;
  margin: 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-item {
  display: flex;
  flex-direction: column;
}

.form-label {
  font-size: 13px;
  color: #666;
  margin-bottom: 6px;
  font-weight: 500;
}

.form-input {
  height: 40px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  outline: none;
  transition: border-color 0.2s;
  background: #f8f9fa;
}

.form-input:focus {
  border-color: #1988fa;
  background: #fff;
}

.form-input::placeholder {
  color: #c0c4cc;
}

.password-wrapper {
  display: flex;
  align-items: center;
  position: relative;
}

.password-wrapper .form-input {
  flex: 1;
  padding-right: 60px;
}

.toggle-password {
  position: absolute;
  right: 4px;
  height: 32px;
  padding: 0 10px;
  background: transparent;
  border: none;
  color: #1988fa;
  font-size: 12px;
  cursor: pointer;
}

.remember-row {
  flex-direction: row;
  align-items: center;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  user-select: none;
}

.checkbox-label input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #1988fa;
}

.checkbox-text {
  font-size: 13px;
  color: #666;
}

.login-button {
  height: 44px;
  background: linear-gradient(135deg, #1988fa, #0e7cd3);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
  margin-top: 8px;
}

.login-button:hover {
  opacity: 0.9;
}

.login-button.loading {
  opacity: 0.7;
  cursor: not-allowed;
}

.login-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.login-footer {
  text-align: center;
  margin-top: 24px;
}

.version-text {
  font-size: 12px;
  color: #c0c4cc;
}
</style>
