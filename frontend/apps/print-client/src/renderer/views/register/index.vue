<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import message from '@/utils/message'

const router = useRouter()

const clientName = ref('')
const loading = ref(false)
const step = ref<'info' | 'done'>('info')

onMounted(async () => {
  // 检查是否已登录
  const authState = await window.electronAPI.auth.getAuthState()
  if (!authState.isLoggedIn) {
    router.push('/login')
    return
  }
  // 如果已有客户端信息，直接跳到首页
  if (authState.clientId) {
    router.push('/dashboard')
  }
})

const handleRegister = async () => {
  if (!clientName.value.trim()) {
    message.warning('请输入打印客户端名称')
    return
  }

  loading.value = true
  try {
    const serverUrl = await window.electronAPI.settings.getSetting('serverUrl') as string
    await window.electronAPI.auth.register(serverUrl, clientName.value.trim())
    message.success('客户端注册成功！')
    step.value = 'done'
    setTimeout(() => router.push('/dashboard'), 1500)
  } catch (err: any) {
    message.error(err.message || '注册失败')
  } finally {
    loading.value = false
  }
}

const handleSkip = () => {
  router.push('/dashboard')
}
</script>

<template>
  <div class="register-page">
    <div class="register-container">
      <div class="register-header">
        <h1 class="register-title">配置打印客户端</h1>
        <p class="register-subtitle">请为这台电脑设置一个名称，方便在系统中识别</p>
      </div>

      <div class="register-body">
        <div class="info-section">
          <div class="info-icon">
            <svg width="40" height="40" viewBox="0 0 40 40" fill="none">
              <rect width="40" height="40" rx="8" fill="#e6f7ff"/>
              <path d="M20 12v8m0 4v.01" stroke="#1988fa" stroke-width="3" stroke-linecap="round"/>
            </svg>
          </div>
          <p class="info-text">
            客户端名称将显示在打印任务的目标选择中，建议使用有意义的名称（如：一楼前台、财务办公室等）
          </p>
        </div>

        <div class="form-item">
          <label class="form-label">客户端名称</label>
          <input
            v-model="clientName"
            type="text"
            class="form-input"
            placeholder="例如: 一楼前台打印机"
            maxlength="50"
            @keydown.enter="handleRegister"
          />
          <span class="form-hint">建议使用位置 + 用途命名</span>
        </div>

        <button
          class="register-button"
          :class="{ loading }"
          :disabled="loading"
          @click="handleRegister"
        >
          {{ loading ? '注册中...' : '确认注册' }}
        </button>

        <button
          class="skip-button"
          @click="handleSkip"
        >
          稍后配置
        </button>
      </div>

      <div class="register-footer">
        <span class="success-tip" v-if="step === 'done'">
          ✅ 注册成功，即将跳转首页...
        </span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: #f5f7fa;
}

.register-container {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.register-header {
  text-align: center;
  margin-bottom: 28px;
}

.register-title {
  font-size: 22px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px;
}

.register-subtitle {
  font-size: 14px;
  color: #969799;
  margin: 0;
}

.register-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-section {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 10px;
}

.info-icon {
  flex-shrink: 0;
}

.info-text {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  margin: 0;
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
  height: 42px;
  padding: 0 12px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  font-size: 14px;
  color: #333;
  outline: none;
  transition: border-color 0.2s;
}

.form-input:focus {
  border-color: #1988fa;
}

.form-hint {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}

.register-button {
  height: 44px;
  background: linear-gradient(135deg, #07c160, #06ad56);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.2s;
}

.register-button:hover {
  opacity: 0.9;
}

.register-button.loading {
  opacity: 0.7;
  cursor: not-allowed;
}

.register-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.skip-button {
  height: 36px;
  background: transparent;
  color: #969799;
  border: none;
  font-size: 13px;
  cursor: pointer;
}

.skip-button:hover {
  color: #666;
}

.register-footer {
  text-align: center;
  margin-top: 16px;
}

.success-tip {
  font-size: 14px;
  color: #07c160;
}
</style>
