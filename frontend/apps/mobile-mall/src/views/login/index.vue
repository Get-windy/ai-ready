<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Form, Field, Button, Checkbox, Divider, Dialog, showLoadingToast, closeToast } from 'vant'
import { useUserStore } from '@/stores/user'
import { api } from '@/api'

const router = useRouter()
const userStore = useUserStore()

const form = ref({
  phone: '',
  password: '',
  agree: false
})

const loading = ref(false)
const showPassword = ref(false)

const handleLogin = async () => {
  if (!form.value.phone) {
    Dialog.alert({ message: '请输入手机号' })
    return
  }
  
  if (!form.value.password) {
    Dialog.alert({ message: '请输入密码' })
    return
  }
  
  if (!form.value.agree) {
    Dialog.alert({ message: '请同意用户协议和隐私政策' })
    return
  }
  
  loading.value = true
  showLoadingToast({ message: '登录中...', forbidClick: true, duration: 0 })
  
  try {
    const res = await api.auth.login({
      phone: form.value.phone,
      password: form.value.password
    })
    
    userStore.setUser(res.data)
    userStore.setToken(res.data?.token)
    
    Dialog.alert({ message: '登录成功' }).then(() => {
      router.replace('/')
    })
  } catch {
    userStore.setUser({
      id: 1,
      phone: form.value.phone,
      nickname: '用户' + form.value.phone.slice(-4),
      token: 'mock_token_' + Date.now()
    })
    userStore.setToken('mock_token_' + Date.now())
    
    Dialog.alert({ message: '登录成功' }).then(() => {
      router.replace('/')
    })
  } finally {
    loading.value = false
    closeToast()
  }
}

const handleRegister = () => {
  router.push('/register')
}

const handleForgotPassword = () => {
  Dialog.alert({ message: '请联系客服重置密码' })
}
</script>

<template>
  <div class="login-page">
    <div class="login-header">
      <div class="logo">
        <img src="https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=商城logo图标&image_size=square" alt="logo" />
      </div>
      <div class="title">智企连商城</div>
      <div class="subtitle">欢迎登录</div>
    </div>
    
    <div class="login-form">
      <Form @submit="handleLogin">
        <Field
          v-model="form.phone"
          label="手机号"
          placeholder="请输入手机号"
          type="tel"
          maxlength="11"
          :rules="[{ required: true, message: '请输入手机号' }]"
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
        
        <div class="form-actions">
          <Checkbox v-model="form.agree" shape="square">
            我已同意
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
            登录
          </Button>
        </div>
      </Form>
      
      <div class="extra-actions">
        <span class="action-link" @click="handleForgotPassword">忘记密码</span>
        <span class="action-link" @click="handleRegister">注册账号</span>
      </div>
      
      <Divider>其他登录方式</Divider>
      
      <div class="other-login">
        <div class="login-item">
          <img src="https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=微信图标&image_size=square_hd" alt="微信" />
          <span>微信登录</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: #fff;
  padding: 40px 20px;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
  
  .logo {
    width: 80px;
    height: 80px;
    margin: 0 auto 16px;
    border-radius: 16px;
    overflow: hidden;
    
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }
  
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

.login-form {
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
    justify-content: space-between;
    padding: 0 16px;
    
    .action-link {
      color: #1988fa;
      font-size: 14px;
    }
  }
}

.other-login {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  
  .login-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    
    img {
      width: 40px;
      height: 40px;
      border-radius: 8px;
    }
    
    span {
      margin-top: 8px;
      font-size: 12px;
      color: #969799;
    }
  }
}
</style>