import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import { VueQueryPlugin } from '@tanstack/vue-query'
import App from './App.vue'
import router from './router'
import { setupI18n } from './locales'
import { initErrorReporter } from './utils/errorReporter'
import { permission, role } from './directives/permission'
import featureFlag from './directives/featureFlag'
import shortcutDirective from './directives/shortcut'
import { initMockServer } from '@/mocks'
import { initFeatureFlags, getFeatureFlagService } from '@/utils/featureFlags'
import { initSentry, setSentryUser, clearSentryUser } from '@/utils/sentry'
import { trackPageLoad, setupRouteTracking } from '@/utils/performanceMonitor'
import { useUserStore } from '@/stores/user'
import { getVueQueryClientConfig } from '@/composables/useQueryConfig'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import 'ant-design-vue/dist/reset.css'
import './styles/variables.css'
import './styles/components.css'
import './styles/index.css'
import './styles/accessibility.css'

// 初始化 Mock Server — 必须在所有其他初始化之前调用，
// 因为后续的 store/router 初始化可能会触发 API 请求。
initMockServer()

// 初始化 Feature Flags
initFeatureFlags()

dayjs.locale('zh-cn')

// 创建 Pinia 实例
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

// 创建 Vue 应用
const app = createApp(App)

// 使用 Pinia
app.use(pinia)

// 注册指令
app.directive('permission', permission)
app.directive('role', role)
app.directive('feature-flag', featureFlag)
app.directive('shortcut', shortcutDirective)

// 提供 Feature Flag 服务（全局注入，组件可通过 inject 获取）
app.provide('featureFlagService', getFeatureFlagService())

// 初始化错误上报器
initErrorReporter(app, {
  enabled: true,
  endpoint: '/api/log/error',
  batchSize: 5,
  flushInterval: 3000
})

// 异步初始化 i18n 并启动应用
async function bootstrap() {
  // 初始化 i18n
  const i18n = await setupI18n()
  app.use(i18n)

  // 使用 Router
  app.use(router)

  // 安装 Vue Query
  app.use(VueQueryPlugin, {
    queryClientConfig: getVueQueryClientConfig(),
  })

  // Sentry RUM 错误监控 & 性能追踪（异步加载，不阻塞启动）
  initSentry(app, router).catch(() => {
    // Sentry 初始化失败不影响应用运行
  })

  // 安装路由切换性能追踪
  setupRouteTracking(router)

  // 注意：Ant Design Vue 已改为按需引入，不需要 app.use(Antd)

  // 挂载应用
  app.mount('#app')

  // 启动 Web Vitals 性能监控
  trackPageLoad()

  // ── Sentry 用户上下文同步 ──
  // 监听 userStore 变化，自动同步 Sentry 用户上下文
  const userStore = useUserStore()

  // 应用启动时检查是否有已登录用户（页面刷新场景）
  if (userStore.isLoggedIn && userStore.userId) {
    setSentryUser({
      id: userStore.userId,
      username: userStore.username,
      tenantId: userStore.tenantId,
    })
  }

  // 订阅 store 变化，在登录/退出时同步 Sentry
  userStore.$subscribe((_mutation, state) => {
    if (state.userId && state.token) {
      setSentryUser({
        id: state.userId,
        username: state.userInfo?.username || state.username,
        tenantId: state.tenantId,
      })
    } else if (!state.token) {
      clearSentryUser()
    }
  })
}

// 启动应用
bootstrap().catch((error) => {
  console.error('Application bootstrap failed:', error)
})