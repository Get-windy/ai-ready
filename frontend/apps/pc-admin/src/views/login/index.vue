<template>
  <ErrorBoundary>
    <div class="login-container">
      <div
        class="login-background"
        aria-hidden="true"
      >
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
              <div
                class="logo-icon"
                aria-hidden="true"
              >
                <span class="logo-initials">AR</span>
              </div>
              <h1>企智连·AI-Ready</h1>
            </div>
            <p role="doc-subtitle">
              企业智能管理系统
            </p>
          </div>
        
          <!-- 登录方式切换：手机号是国内用户的首选，账号密码作为备选 -->
          <div class="login-tabs">
            <button
              type="button"
              class="login-tab"
              :class="{ 'is-active': loginTab === 'sms' }"
              @click="switchTab('sms')"
            >
              手机号登录
            </button>
            <button
              type="button"
              class="login-tab"
              :class="{ 'is-active': loginTab === 'password' }"
              @click="switchTab('password')"
            >
              账号密码
            </button>
          </div>

          <a-form
            v-show="loginTab === 'password'"
            ref="formRef"
            :model="formState"
            :rules="rules"
            layout="vertical"
            role="form"
            :aria-label="t('login.title')"
            @finish="handleSubmit"
          >
            <a-form-item
              id="form-item-username"
              name="username"
            >
              <a-input
                v-model:value="formState.username"
                size="large"
                placeholder="请输入用户名"
                aria-required="true"
                :aria-describedby="formState.username ? '' : 'form-item-username-error'"
                @keyup.enter="focusNextInput('password')"
              >
                <template #prefix>
                  <UserOutlined aria-hidden="true" />
                </template>
              </a-input>
            </a-form-item>

            <a-form-item
              id="form-item-password"
              name="password"
            >
              <a-input-password
                ref="passwordInput"
                v-model:value="formState.password"
                size="large"
                placeholder="请输入密码"
                aria-required="true"
                :aria-describedby="formState.password ? '' : 'form-item-password-error'"
                @keyup.enter="focusNextInput('captcha')"
              >
                <template #prefix>
                  <LockOutlined aria-hidden="true" />
                </template>
              </a-input-password>
            </a-form-item>

            <!-- 验证码区域 -->
            <a-form-item
              id="form-item-captcha"
              name="captcha"
            >
              <div class="captcha-container">
                <a-input
                  ref="captchaInput"
                  v-model:value="formState.captcha"
                  size="large"
                  placeholder="请输入验证码"
                  class="captcha-input"
                  aria-required="true"
                  :aria-describedby="formState.captcha ? '' : 'form-item-captcha-error'"
                >
                  <template #prefix>
                    <SafetyOutlined aria-hidden="true" />
                  </template>
                </a-input>
                <div
                  class="captcha-image"
                  role="button"
                  aria-label="刷新验证码"
                  tabindex="0"
                  @click="refreshCaptcha"
                  @keydown.enter="refreshCaptcha"
                  @keydown.space.prevent="refreshCaptcha"
                >
                  <img
                    v-if="captchaUrl"
                    :src="captchaUrl"
                    alt="验证码图片，点击刷新"
                  >
                  <div
                    v-else
                    class="captcha-loading"
                    role="status"
                    aria-label="验证码加载中"
                  >
                    <LoadingOutlined aria-hidden="true" />
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
                role="button"
                aria-label="忘记密码"
                tabindex="0"
                @click="handleForgotPassword"
                @keydown.enter="handleForgotPassword"
              >忘记密码？</a>
            </div>

            <a-form-item>
              <a-button
                type="primary"
                html-type="submit"
                size="large"
                :loading="loading"
                :aria-busy="loading"
                block
                class="login-button"
              >
                {{ loading ? '登录中...' : '登 录' }}
              </a-button>
            </a-form-item>

          </a-form>

          <!-- 手机号验证码登录（国内主流方式） -->
          <a-form
            v-show="loginTab === 'sms'"
            ref="smsFormRef"
            :model="smsFormState"
            :rules="smsRules"
            layout="vertical"
            role="form"
            aria-label="手机号登录"
            @finish="handleSmsSubmit"
          >
            <a-form-item
              id="form-item-phone"
              name="phone"
            >
              <a-input
                v-model:value="smsFormState.phone"
                size="large"
                placeholder="请输入手机号"
                aria-required="true"
                :maxlength="11"
              >
                <template #prefix>
                  <MobileOutlined aria-hidden="true" />
                </template>
              </a-input>
            </a-form-item>

            <a-form-item
              id="form-item-sms-code"
              name="smsCode"
            >
              <div class="captcha-container">
                <a-input
                  v-model:value="smsFormState.smsCode"
                  size="large"
                  placeholder="请输入短信验证码"
                  class="captcha-input"
                  aria-required="true"
                  :maxlength="6"
                >
                  <template #prefix>
                    <SafetyOutlined aria-hidden="true" />
                  </template>
                </a-input>
                <a-button
                  size="large"
                  class="sms-code-btn"
                  :disabled="smsCountdown > 0"
                  :loading="sendingCode"
                  @click="handleSendSmsCode"
                >
                  {{ smsCountdown > 0 ? `${smsCountdown}s 后重发` : '获取验证码' }}
                </a-button>
              </div>
            </a-form-item>

            <a-form-item>
              <a-button
                type="primary"
                html-type="submit"
                size="large"
                :loading="loading"
                :aria-busy="loading"
                block
                class="login-button"
              >
                {{ loading ? '登录中...' : '登 录' }}
              </a-button>
            </a-form-item>
          </a-form>

          <!-- 三方登录：按后端已配置的平台渲染，未配置则不显示（不做点了没反应的死按钮） -->
          <div
            v-if="socialProviders.length"
            class="social-login"
          >
            <div class="social-login-divider">
              <span>其他登录方式</span>
            </div>
            <div class="social-login-list">
              <button
                v-for="p in socialProviders"
                :key="p.platform"
                type="button"
                class="social-login-item"
                :title="`使用${p.name}登录`"
                @click="handleSocialLogin(p.platform)"
              >
                {{ p.name }}
              </button>
            </div>
          </div>

          <!-- 注册入口：不属于任一登录表单，故置于表单之外 -->
          <div class="login-footer">
            <span class="footer-text">还没有账号？</span>
            <a-button
              type="link"
              :loading="registerLoading"
              class="register-link"
              @click="handleRegister"
            >
              立即注册
            </a-button>
          </div>
          <div
            class="login-footer"
            style="margin-top: 4px;"
          >
            <span class="footer-text">企业用户？</span>
            <router-link
              to="/tenant-register"
              class="enterprise-link"
            >
              企业注册
            </router-link>
          </div>

          <!-- 多企业账号：登录第一步只验身份，此处选择本次要登录的企业 -->
          <a-modal
            v-model:open="showTenantPicker"
            title="选择要登录的企业"
            :footer="null"
            :closable="!selecting"
            :mask-closable="false"
            :keyboard="!selecting"
            width="420px"
            centered
          >
            <p class="tenant-picker-tip">
              该账号关联多个企业，请选择本次要登录的企业
            </p>
            <div class="tenant-picker-list">
              <button
                v-for="tenant in tenantOptions"
                :key="tenant.id"
                type="button"
                class="tenant-picker-item"
                :disabled="selecting"
                @click="handlePickTenant(tenant.id)"
              >
                <span class="tenant-picker-name">{{ tenant.tenantName }}</span>
                <span
                  v-if="tenant.id === lastLoginTenantId"
                  class="tenant-picker-badge"
                >上次登录</span>
              </button>
            </div>
          </a-modal>
        </div>
      </div>
    </div>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { useI18n } from 'vue-i18n'
import {
  UserOutlined,
  LockOutlined,
  SafetyOutlined,
  LoadingOutlined,
  MobileOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { userApi, type TenantInfo } from '@/api/user'
import { socialApi, type SocialProvider } from '@/api/social'
import { resetDynamicRoutesLoaded } from '@/router/guard'
import { useSubmitLock } from '@/composables'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 表单引用
const formRef = ref<FormInstance>()
const passwordInput = ref()
const captchaInput = ref()

// 状态管理
const loading = ref(false)
const { isSubmitting: registerLoading } = useSubmitLock()
const rememberMe = ref(false)
const captchaUrl = ref('')
const captchaKey = ref('')

// 多企业账号的「选择企业」弹窗
const showTenantPicker = ref(false)
const tenantOptions = ref<TenantInfo[]>([])
const lastLoginTenantId = ref<number | undefined>()
const selectToken = ref('')
const selecting = ref(false)

// 登录方式：sms = 手机号验证码（默认，国内用户首选）；password = 账号密码
const loginTab = ref<'sms' | 'password'>('sms')
const smsFormRef = ref<FormInstance>()
const smsFormState = reactive({ phone: '', smsCode: '' })
const sendingCode = ref(false)
const smsCountdown = ref(0)
let smsTimer: ReturnType<typeof setInterval> | null = null

// 三方登录（钉钉/企业微信/飞书）：按后端已配置的平台渲染
const socialProviders = ref<SocialProvider[]>([])

const smsRules: any = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码', trigger: 'blur' }
  ],
  smsCode: [
    { required: true, message: '请输入短信验证码', trigger: 'blur' },
    { len: 6, message: '请输入 6 位验证码', trigger: 'blur' }
  ]
}

// 表单数据
const formState = reactive({
  username: '',
  password: '',
  captcha: ''
})

// 表单验证规则
const rules: any = {
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
const fetchCaptcha = async (retryCount = 0) => {
  try {
    const res = await userApi.getCaptcha() as any as { img: string; uuid: string }
    if (res && res.img) {
      captchaUrl.value = res.img
      captchaKey.value = res.uuid
    } else {
      // 响应存在但缺少 img 字段，视为失败
      throw new Error('验证码数据格式异常')
    }
  } catch (error) {
    console.warn('[登录] 获取验证码失败', error)
    // 首次加载时自动重试一次（延迟 500ms），避免后端未就绪导致空白
    if (retryCount === 0) {
      setTimeout(() => fetchCaptcha(1), 500)
    } else {
      message.warning('获取验证码失败，请点击图片刷新')
    }
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

// 处理登录提交（第一步：只验身份，不再需要输入企业名称）
const handleSubmit = async () => {
  if (loading.value) return // 防止重复提交
  try {
    loading.value = true

    // 表单验证
    await formRef.value?.validate()

    const res: any = await userStore.login({
      username: formState.username,
      password: formState.password,
      captcha: formState.captcha,
      captchaKey: captchaKey.value
    })

    // 账号关联多个企业：先弹框选企业，选定后再真正完成登录
    if (res?.needSelectTenant) {
      tenantOptions.value = res.tenants || []
      lastLoginTenantId.value = res.lastLoginTenantId
      selectToken.value = res.selectToken
      showTenantPicker.value = true
      return
    }

    finishLogin()
  } catch (error: any) {
    console.warn('[登录] 登录失败', error)
    const errorMsg = error?.message || error?.response?.data?.message || '登录失败，请稍后重试'
    message.error(errorMsg)
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

// 选定企业，完成登录（第二步）
const handlePickTenant = async (tenantId: number) => {
  if (selecting.value) return
  selecting.value = true
  try {
    await userStore.selectTenant(selectToken.value, tenantId)
    showTenantPicker.value = false
    finishLogin()
  } catch (error: any) {
    console.warn('[登录] 选择企业失败', error)
    const errorMsg = error?.message || error?.response?.data?.message || '选择企业失败，请重新登录'
    message.error(errorMsg)
    // 票据可能已过期/被消费，退回登录表单重新来过
    showTenantPicker.value = false
    refreshCaptcha()
  } finally {
    selecting.value = false
  }
}

// 登录成功后的收尾：记住用户名、重置动态路由、跳转
const finishLogin = () => {
  message.success('登录成功，欢迎回来！')

  // 处理记住我功能
  if (rememberMe.value) {
    localStorage.setItem('rememberedUsername', formState.username)
  } else {
    localStorage.removeItem('rememberedUsername')
  }

  // 重置动态路由加载状态，让路由守卫重新加载
  resetDynamicRoutesLoaded()

  // 直接导航，由路由守卫负责加载用户信息和动态路由
  const redirect = (route.query.redirect as string) || '/dashboard'
  router.replace(redirect)
}

// 切换登录方式（清掉上一种方式的校验残留，避免切换后还挂着红字）
const switchTab = (tab: 'sms' | 'password') => {
  if (loginTab.value === tab) return
  loginTab.value = tab
  if (tab === 'password') formRef.value?.clearValidate?.()
  else smsFormRef.value?.clearValidate?.()
}

// 发送短信验证码（只校验手机号，不要求验证码已填）
const handleSendSmsCode = async () => {
  if (smsCountdown.value > 0 || sendingCode.value) return
  try {
    await smsFormRef.value?.validateFields('phone')
  } catch {
    return
  }

  sendingCode.value = true
  try {
    await userApi.sendSmsCode(smsFormState.phone)
    message.success('验证码已发送')
    startSmsCountdown()
  } catch (error: any) {
    console.warn('[登录] 发送验证码失败', error)
    message.error(error?.message || error?.response?.data?.message || '验证码发送失败，请稍后重试')
  } finally {
    sendingCode.value = false
  }
}

// 60 秒重发倒计时（与后端重发间隔一致）
const startSmsCountdown = () => {
  smsCountdown.value = 60
  smsTimer = setInterval(() => {
    smsCountdown.value--
    if (smsCountdown.value <= 0 && smsTimer) {
      clearInterval(smsTimer)
      smsTimer = null
    }
  }, 1000)
}

// 手机号验证码登录
const handleSmsSubmit = async () => {
  if (loading.value) return
  try {
    loading.value = true
    await smsFormRef.value?.validate()

    const res: any = await userStore.loginBySms({
      phone: smsFormState.phone,
      smsCode: smsFormState.smsCode
    })

    if (res?.needSelectTenant) {
      tenantOptions.value = res.tenants || []
      lastLoginTenantId.value = res.lastLoginTenantId
      selectToken.value = res.selectToken
      showTenantPicker.value = true
      return
    }

    finishLogin()
  } catch (error: any) {
    console.warn('[登录] 短信登录失败', error)
    message.error(error?.message || error?.response?.data?.message || '登录失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 加载可用三方平台（未配置凭据的平台后端不返回，前端也就不显示）
const loadSocialProviders = async () => {
  try {
    const res: any = await socialApi.getProviders()
    socialProviders.value = res || []
  } catch {
    socialProviders.value = []
  }
}

// 发起三方登录：整页跳转到三方授权页
const handleSocialLogin = async (platform: string) => {
  try {
    const url: any = await socialApi.getAuthorizeUrl(platform, 'login')
    if (!url) {
      message.error('未取得授权地址')
      return
    }
    window.location.href = url
  } catch (error: any) {
    message.error(error?.message || '暂时无法发起三方登录')
  }
}

// 清掉地址栏上的三方回调参数，避免刷新时重复处理
const clearSocialQuery = () => {
  router.replace({ path: route.path, query: {} })
}

/**
 * 处理三方授权回调带回的参数（后端 302 回本页时携带）：
 * socialTicket=票据 → 用票据换登录结果；socialError=原因 → 提示
 */
const handleSocialCallback = async () => {
  const errorCode = route.query.socialError as string
  if (errorCode) {
    const errorMap: Record<string, string> = {
      state_expired: '授权已超时，请重新发起',
      cancelled: '已取消授权',
      not_bound: '该账号尚未绑定系统账号，请先用账号密码登录，在「个人中心 → 账号绑定」中绑定后即可扫码登录',
      failed: '三方授权失败，请重试'
    }
    message.error(errorMap[errorCode] || '三方登录失败')
    clearSocialQuery()
    return
  }

  const ticket = route.query.socialTicket as string
  if (!ticket) return

  try {
    const res: any = await socialApi.exchange(ticket)
    if (res?.needSelectTenant) {
      tenantOptions.value = res.tenants || []
      lastLoginTenantId.value = res.lastLoginTenantId
      selectToken.value = res.selectToken
      showTenantPicker.value = true
      clearSocialQuery()
      return
    }
    userStore.applyLoginResult(res)
    clearSocialQuery()
    finishLogin()
  } catch (error: any) {
    message.error(error?.message || '三方登录失败，请重试')
    clearSocialQuery()
  }
}

// 忘记密码
const handleForgotPassword = () => {
  message.info('请联系管理员重置密码')
}

// 注册账号
const handleRegister = async () => {
  if (registerLoading.value) return
  registerLoading.value = true
  try {
    await router.push('/register')
  } finally {
    registerLoading.value = false
  }
}

// ── 键盘快捷键 ──────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    fetchCaptcha()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n' && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    router.push('/register')
  }
}

// 初始化
onMounted(async () => {
  document.addEventListener('keydown', handleKeydown)

  // 检查是否有记住的用户名
  const rememberedUsername = localStorage.getItem('rememberedUsername')
  if (rememberedUsername) {
    formState.username = rememberedUsername
    rememberMe.value = true
  }

  // 获取验证码
  fetchCaptcha()

  // 可用的三方登录平台 + 处理三方授权回调带回的参数
  loadSocialProviders()
  handleSocialCallback()
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  if (smsTimer) {
    clearInterval(smsTimer)
    smsTimer = null
  }
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
  display: flex;
  justify-content: center;
}

.logo-initials {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
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
  padding: 0;
  font-size: 14px;
}

.register-link:hover {
  color: #764ba2;
}

:deep(.register-link.ant-btn-link) {
  padding: 0 0 0 1px;
}

/* ── 登录方式切换（钉钉/企微风格：文字 tab） ─── */
.login-tabs {
  display: flex;
  gap: 24px;
  margin-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.login-tab {
  position: relative;
  padding: 8px 2px 10px;
  font-size: 15px;
  color: #8c8c8c;
  background: none;
  border: none;
  cursor: pointer;
  transition: color 0.2s;
}

.login-tab:hover {
  color: #667eea;
}

.login-tab.is-active {
  color: #1a1a1a;
  font-weight: 600;
}

.login-tab.is-active::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 1px;
}

/* 获取验证码按钮：与验证码输入框同容器排布 */
.sms-code-btn {
  flex-shrink: 0;
  width: 116px;
}

/* ── 三方登录入口 ───────────────────── */
.social-login {
  margin-top: 8px;
}

.social-login-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 16px 0 12px;
  font-size: 12px;
  color: #bfbfbf;
}

.social-login-divider::before,
.social-login-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: #f0f0f0;
}

.social-login-list {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.social-login-item {
  padding: 6px 18px;
  font-size: 14px;
  color: #595959;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.social-login-item:hover {
  color: #667eea;
  border-color: #667eea;
  background: #f7f8ff;
}

/* ── 选择企业弹窗 ───────────────────── */
.tenant-picker-tip {
  margin: 0 0 12px;
  font-size: 13px;
  color: #666;
}

.tenant-picker-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 320px;
  overflow-y: auto;
}

.tenant-picker-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 12px 14px;
  font-size: 14px;
  color: #1a1a1a;
  text-align: left;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.tenant-picker-item:hover:not(:disabled) {
  border-color: #667eea;
  background: #f7f8ff;
}

.tenant-picker-item:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.tenant-picker-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tenant-picker-badge {
  flex-shrink: 0;
  margin-left: 8px;
  padding: 1px 6px;
  font-size: 12px;
  color: #667eea;
  background: #eef0ff;
  border-radius: 4px;
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

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

</style>