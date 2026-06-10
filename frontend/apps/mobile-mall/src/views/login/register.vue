<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { Form, Field, Button, Checkbox, Divider, Dialog, showLoadingToast, closeToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { api } from '@/api'

const router = useRouter()
const userStore = useUserStore()

const form = ref({
  phone: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  verifyCode: '',
  agree: false
})

const loading = ref(false)
const showPassword = ref(false)
const countdown = ref(0)
const sendingCode = ref(false)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const sendVerifyCode = async () => {
  if (!form.value.phone) {
    Dialog.alert({ message: '请输入手机号' })
    return
  }
  
  if (!/^1[3-9]\d{9}$/.test(form.value.phone)) {
    Dialog.alert({ message: '请输入正确的手机号' })
    return
  }
  
  sendingCode.value = true
  showLoadingToast({ message: '发送中...', forbidClick: true, duration: 0 })
  
  try {
    await api.auth.sendVerifyCode(form.value.phone)
    countdown.value = 60
    startCountdown()
    Dialog.alert({ message: '验证码已发送' })
  } catch (err) {
    console.warn('[注册] 发送验证码失败', err)
    Dialog.alert({ message: '验证码发送失败，请稍后重试' })
    return
  } finally {
    sendingCode.value = false
    closeToast()
  }
}

const startCountdown = () => {
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
    }
  }, 1000)
}

onUnmounted(() => {
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
})

const handleRegister = async () => {
  if (!form.value.phone) {
    Dialog.alert({ message: '请输入手机号' })
    return
  }
  
  if (!form.value.password) {
    Dialog.alert({ message: '请输入密码' })
    return
  }
  
  if (form.value.password !== form.value.confirmPassword) {
    Dialog.alert({ message: '两次密码不一致' })
    return
  }
  
  if (!form.value.verifyCode) {
    Dialog.alert({ message: '请输入验证码' })
    return
  }
  
  if (!form.value.agree) {
    Dialog.alert({ message: '请同意用户协议和隐私政策' })
    return
  }
  
  loading.value = true
  showLoadingToast({ message: '注册中...', forbidClick: true, duration: 0 })
  
  try {
    const res = await api.auth.register({
      phone: form.value.phone,
      password: form.value.password,
      nickname: form.value.nickname,
      verifyCode: form.value.verifyCode
    })
    
    userStore.setUser(res.data)
    userStore.setToken(res.data?.token)
    
    Dialog.alert({ message: '注册成功' }).then(() => {
      router.replace('/')
    })
  } catch (err) {
    console.warn('[注册] 注册失败', err)
    Dialog.alert({ message: '注册失败，请检查网络连接' })
  } finally {
    loading.value = false
    closeToast()
  }
}

const goBack = () => {
  router.back()
}
</script>

<template>
  <div class="register-page">
    <div class="register-header">
      <div class="title">注册账号</div>
      <div class="subtitle">创建您的智企连商城账号</div>
    </div>
    
    <div class="register-form">
      <Form @submit="handleRegister">
        <Field
          v-model="form.phone"
          label="手机号"
          placeholder="请输入手机号"
          type="tel"
          maxlength="11"
          :rules="[{ required: true, message: '请输入手机号' }]"
        />
        
        <Field
          v-model="form.verifyCode"
          label="验证码"
          placeholder="请输入验证码"
          maxlength="6"
          :rules="[{ required: true, message: '请输入验证码' }]"
        >
          <template #button>
            <Button 
              size="small" 
              type="primary"
              :disabled="countdown > 0 || sendingCode"
              @click="sendVerifyCode"
            >
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </Button>
          </template>
        </Field>
        
        <Field
          v-model="form.nickname"
          label="昵称"
          placeholder="请输入昵称（可选）"
        />
        
        <Field
          v-model="form.password"
          label="密码"
          placeholder="请输入密码"
          :type="showPassword ? 'text' : 'password'"
          :right-icon="showPassword ? 'eye-o' : 'closed-eye'"
          @click-right-icon="showPassword = !showPassword"
          :rules="[{ required: true, message: '请输入密码' }]"
        />
        
        <Field
          v-model="form.confirmPassword"
          label="确认密码"
          placeholder="请再次输入密码"
          :type="showPassword ? 'text' : 'password'"
          :rules="[{ required: true, message: '请确认密码' }]"
        />
        
        <div class="form-actions">
          <Checkbox v-model="form.agree" shape="square">
            我已阅读并同意
            <span class="link">用户协议</span>
            和
            <span class="link">隐私政策</span>
          </Checkbox>
        </div>
        
        <div class="submit-section">
          <Button 
            block 
            type="primary" 
            native-type="submit"
            :loading="loading"
          >
            注册
          </Button>
        </div>
      </Form>
      
      <div class="extra-actions">
        <span>已有账号？</span>
        <span class="action-link" @click="goBack">立即登录</span>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.register-page {
  min-height: 100vh;
  background: #fff;
  padding: 40px 20px;
}

.register-header {
  text-align: center;
  margin-bottom: 40px;
  
  .title {
    font-size: 24px;
    font-weight: 600;
    color: #333;
  }
  
  .subtitle {
    font-size: 14px;
    color: #969799;
    margin-top: 8px;
  }
}

.register-form {
  .form-actions {
    padding: 16px;
    
    .link {
      color: #1988fa;
    }
  }
  
  .submit-section {
    margin: 16px;
  }
  
  .extra-actions {
    display: flex;
    justify-content: center;
    gap: 4px;
    padding: 16px;
    
    .action-link {
      color: #1988fa;
    }
  }
}
</style>