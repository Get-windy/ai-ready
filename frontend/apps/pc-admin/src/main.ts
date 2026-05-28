import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import App from './App.vue'
import router from './router'
import { setupI18n } from './locales'
import { initErrorReporter } from './utils/errorReporter'
import { permission, role } from './directives/permission'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import 'ant-design-vue/dist/reset.css'
import './styles/variables.css'
import './styles/components.css'
import './styles/index.css'

dayjs.locale('zh-cn')

// 创建 Pinia 实例
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

// 创建 Vue 应用
const app = createApp(App)

// 使用 Pinia
app.use(pinia)

// 注册权限指令
app.directive('permission', permission)
app.directive('role', role)

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

  // 注意：Ant Design Vue 已改为按需引入，不需要 app.use(Antd)
  // Components 插件会自动注册组件

  // 挂载应用
  app.mount('#app')
}

// 启动应用
bootstrap().catch((error) => {
  console.error('Application bootstrap failed:', error)
})