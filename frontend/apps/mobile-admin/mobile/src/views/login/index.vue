<template>
  <div class="login-page">
    <div class="login-header">
      <img src="@/assets/logo.png" alt="Logo" class="logo" />
      <h1 class="title">智企连·AI-Ready</h1>
      <p class="subtitle">企业管理移动端</p>
    </div>

    <van-form @submit="handleLogin" class="login-form">
      <van-cell-group inset>
        <van-field
          v-model="form.username"
          name="username"
          label="用户名"
          placeholder="请输入用户名"
          :rules="[{ required: true, message: '请输入用户名' }]"
        />
        <van-field
          v-model="form.password"
          type="password"
          name="password"
          label="密码"
          placeholder="请输入密码"
          :rules="[{ required: true, message: '请输入密码' }]"
        />
        <van-field name="tenant" label="租户">
          <template #input>
            <van-dropdown-menu>
              <van-dropdown-item v-model="form.tenantId" :options="tenantOptions" />
            </van-dropdown-menu>
          </template>
        </van-field>
      </van-cell-group>

      <div class="login-options">
        <van-checkbox v-model="form.rememberMe">记住密码</van-checkbox>
        <router-link to="/forgot-password" class="forgot-link">忘记密码?</router-link>
      </div>

      <div class="login-actions">
        <van-button
          round
          block
          type="primary"
          native-type="submit"
          :loading="loading"
        >
          登录
        </van-button>
      </div>
    </van-form>

    <div class="login-footer">
      <p>其他登录方式</p>
      <div class="other-login">
        <van-icon name="scan" size="24" @click="handleScanLogin" />
        <van-icon name="phone-o" size="24" @click="handleSmsLogin" />
        <van-icon name="wechat" size="24" @click="handleWechatLogin" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast, showSuccessToast } from 'vant'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  tenantId: 1,
  rememberMe: false
})

const tenantOptions = [
  { text: '默认租户', value: 1 },
  { text: '演示租户', value: 2 }
]

onMounted(() => {
  const savedUsername = localStorage.getItem('remembered_username')
  if (savedUsername) {
    form.username = savedUsername
    form.rememberMe = true
  }
})

const handleLogin = async () => {
  loading.value = true
  try {
    await userStore.login({
      username: form.username,
      password: form.password,
      tenantId: form.tenantId
    })

    if (form.rememberMe) {
      localStorage.setItem('remembered_username', form.username)
    } else {
      localStorage.removeItem('remembered_username')
    }

    showSuccessToast('登录成功')
    router.replace('/')
  } catch (error: any) {
    showToast(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}

const handleScanLogin = () => {
  showToast('扫码登录功能开发中')
}

const handleSmsLogin = () => {
  router.push('/login-sms')
}

const handleWechatLogin = () => {
  showToast('微信登录功能开发中')
}
</script>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 60px 20px 20px;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;

  .logo {
    width: 80px;
    height: 80px;
    margin-bottom: 16px;
  }

  .title {
    color: #fff;
    font-size: 24px;
    margin: 0 0 8px;
  }

  .subtitle {
    color: rgba(255, 255, 255, 0.8);
    font-size: 14px;
    margin: 0;
  }
}

.login-form {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.login-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 16px 0;

  .forgot-link {
    color: #1989fa;
    font-size: 14px;
  }
}

.login-actions {
  padding: 24px 16px 8px;
}

.login-footer {
  text-align: center;
  margin-top: 32px;
  color: rgba(255, 255, 255, 0.8);

  p {
    font-size: 14px;
    margin-bottom: 16px;
  }

  .other-login {
    display: flex;
    justify-content: center;
    gap: 32px;

    .van-icon {
      color: #fff;
      cursor: pointer;
      transition: transform 0.2s;

      &:hover {
        transform: scale(1.1);
      }
    }
  }
}
</style>