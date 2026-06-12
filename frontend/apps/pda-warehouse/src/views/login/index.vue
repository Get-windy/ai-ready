<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Form, Field, Button, Checkbox, Divider, Dialog, showLoadingToast, closeToast, showToast } from 'vant'
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
    Dialog.alert({ message: '请同意用户协议' })
    return
  }
  
  loading.value = true
  showLoadingToast({ message: '登录中...', forbidClick: true, duration: 0 })
  
  try {
    const res = await api.auth.login(form.value.phone, form.value.password) as any

    // 假设后端返回 { data: { token, ...user } }
    const userData = res?.data || res
    if (userData?.token) {
      userStore.setUser(userData)
      userStore.setToken(userData.token)
      showToast('登录成功')
      router.replace('/task')
    } else {
      showToast('登录失败：返回数据异常')
    }
  } catch (err: any) {
    showToast(err?.response?.data?.message || err?.message || '登录失败，请检查网络连接')
  } finally {
    loading.value = false
    closeToast()
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-header">
      <div class="logo">
        <img src="https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=仓库作业logo图标&image_size=square" alt="logo" />
      </div>
      <div class="title">智企连仓库作业</div>
      <div class="subtitle">仓库人员登录</div>
    </div>
    
    <div class="login-form">
      <Form @submit="handleLogin">
        <Field
          v-model:value="form.phone"
          label="手机号"
          placeholder="请输入手机号"
          type="tel"
          maxlength="11"
          :rules="[{ required: true, message: '请输入手机号' }]"
        />
        
        <Field
          v-model:value="form.password"
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
      
      <Divider>其他登录方式</Divider>
      
      <div class="other-login">
        <div class="login-item">
          <img src="https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=微信图标&image_size=square_hd" alt="微信" />
          <span>微信登录</span>
        </div>
      </div>
    </div>
    
    <div class="login-footer">
      <div class="footer-text">如有问题请联系管理员</div>
      <div class="footer-phone">客服电话: 400-888-8888</div>
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

.login-footer {
  text-align: center;
  margin-top: 40px;
  
  .footer-text {
    font-size: 12px;
    color: #969799;
  }
  
  .footer-phone {
    font-size: 14px;
    color: #1988fa;
    margin-top: 8px;
  }
}
</style>