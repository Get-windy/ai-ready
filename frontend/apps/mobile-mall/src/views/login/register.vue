<script setup lang="ts">
/**
 * 入店注册页。
 *
 * ⚠️ 2026-09-26 修正三处与后端**不一致**的问题（此前注册必然失败）：
 *  1. **缺用户名**：`MallAuthServiceImpl.register` 是按 `username` 判断"系统里是否已有
 *     这个自然人"，再据此走「新建」或「用原密码自证后绑定本店」两条分支；
 *     原表单没有 username ⇒ 后端按 null 查不到 ⇒ 走新建 ⇒ 撞 `sys_user.username`
 *     NOT NULL ⇒ 500。现补上「用户名」字段（登录时用的就是它）。
 *  2. **验证码是无源之水**：后端没有发码接口（`MallAuthController` 只有
 *     login/register/logout/refresh-token/identities/switch-identity），
 *     `api.auth.sendVerifyCode` 在 `api/index.ts` 里**根本不存在** ⇒ 点「获取验证码」即报错。
 *     短信验证码属待建能力，本轮**移除该字段与按钮**，不给用户"填了就安全"的错觉。
 *  3. **注册不返回 token**：接口返回 `ApiResponse<Void>`，原代码却读 `res.data.token`
 *     并直接登录 ⇒ 静默失败（人以为登录了，其实没有）。现改为「注册成功 → 去登录」。
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Form, Field, Button, Checkbox, Dialog, showLoadingToast, closeToast } from 'vant'
import { api } from '@/api'

const router = useRouter()

const form = ref({
  username: '',
  phone: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  agree: false
})

const loading = ref(false)
const showPassword = ref(false)

const handleRegister = async () => {
  if (!form.value.username || form.value.username.trim().length < 4) {
    Dialog.alert({ message: '请输入用户名（至少 4 个字符）' })
    return
  }

  if (!/^1[3-9]\d{9}$/.test(form.value.phone)) {
    Dialog.alert({ message: '请输入正确的手机号' })
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

  if (!form.value.agree) {
    Dialog.alert({ message: '请同意用户协议和隐私政策' })
    return
  }

  loading.value = true
  showLoadingToast({ message: '注册中...', forbidClick: true, duration: 0 })

  const username = form.value.username.trim()
  try {
    // 只传后端声明的字段（LoginRequest: username / password / phone）
    await api.auth.register({
      username,
      password: form.value.password,
      phone: form.value.phone,
      nickname: form.value.nickname || undefined
    })

    // 注册接口**不返回 token**，要单独登录。
    // 若本店开了"注册需审核"（reg_audit_required），登录后是「待认证」态，
    // 由 stores/shop.ts 的价格三态负责提示，这里只把用户送到登录页。
    Dialog.alert({ message: '注册成功，请使用刚设置的账号登录' }).then(() => {
      router.replace({ path: '/login', query: { username } })
    })
  } catch (err: any) {
    console.warn('[注册] 注册失败', err)
    // 后端对"用户名已存在 / 等待审核 / 已在本店注册"都有明确中文提示，优先透出，
    // 不要一律回一句"网络错误"（那会把可自助解决的问题变成找客服）
    const msg = err?.response?.data?.message || err?.message || '注册失败，请稍后重试'
    Dialog.alert({ message: msg })
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
      <div class="subtitle">创建您的企智连商城账号</div>
    </div>

    <div class="register-form">
      <Form @submit="handleRegister">
        <Field
          v-model="form.username"
          label="用户名"
          placeholder="4-20 位，登录时使用"
          maxlength="20"
          :rules="[{ required: true, message: '请输入用户名' }]"
        />

        <Field
          v-model="form.phone"
          label="手机号"
          placeholder="请输入手机号"
          type="tel"
          maxlength="11"
          :rules="[{ required: true, message: '请输入手机号' }]"
        />

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
