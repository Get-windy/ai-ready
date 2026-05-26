/**
 * 环境变量类型定义
 */

interface ImportMetaEnv {
  // 环境配置
  readonly VITE_APP_ENV: 'development' | 'test' | 'production' | 'preview'
  readonly VITE_APP_TITLE: string
  readonly VITE_APP_VERSION: string
  readonly VITE_APP_BUILD_TIME: string
  
  // API配置
  readonly VITE_API_BASE_URL: string
  readonly VITE_API_TIMEOUT: string
  readonly VITE_API_ADMIN_URL: string
  readonly VITE_API_MOBILE_URL: string
  
  // 代理配置
  readonly VITE_PROXY_TARGET: string
  readonly VITE_PROXY_CHANGE_ORIGIN: string
  
  // 功能开关
  readonly VITE_FEATURE_DEBUG: string
  readonly VITE_FEATURE_MOCK_API: string
  readonly VITE_FEATURE_ANALYTICS: string
  
  // 第三方服务
  readonly VITE_SENTRY_DSN: string
  readonly VITE_GA_TRACKING_ID: string
  
  // 路径配置
  readonly VITE_PUBLIC_PATH: string
  readonly VITE_OUTPUT_DIR: string
  
  // 开发服务器配置
  readonly VITE_DEV_SERVER_HOST: string
  readonly VITE_DEV_SERVER_PORT: string
  readonly VITE_DEV_SERVER_HTTPS: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}