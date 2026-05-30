/**
 * @ai-ready/config
 * 共享应用配置
 */
import type { App, InjectionKey, Plugin } from 'vue'

// ── 类型定义 ────────────────────────────────────────────

export interface AppConfig {
  apiBaseURL: string
  timeout: number
  pagination: {
    defaultPageSize: number
    pageSizeOptions: number[]
  }
  locale: {
    default: string
    supported: string[]
  }
  sentry?: {
    dsn: string
    environment: string
  }
}

// ── 默认配置 ────────────────────────────────────────────

export const defaultConfig: AppConfig = {
  apiBaseURL: '/api',
  timeout: 30000,
  pagination: {
    defaultPageSize: 10,
    pageSizeOptions: [10, 20, 50, 100]
  },
  locale: {
    default: 'zh-CN',
    supported: ['zh-CN', 'en-US', 'ja-JP', 'ko-KR']
  }
}

// ── Injection Key ───────────────────────────────────────

export const CONFIG_KEY: InjectionKey<AppConfig> = Symbol('ai-ready-config')

// ── 合并工具 ────────────────────────────────────────────

/** 深度合并多个配置片段（浅合并，最后传入的值优先生效） */
export function mergeConfig(...configs: Partial<AppConfig>[]): AppConfig {
  return configs.reduce(
    (merged, config) => ({ ...merged, ...config }),
    { ...defaultConfig }
  ) as AppConfig
}

// ── ConfigProvider（Vue 插件） ──────────────────────────

export const ConfigProvider: Plugin = {
  install(app: App, options?: Partial<AppConfig>) {
    const resolved = mergeConfig(options ?? {})
    app.provide(CONFIG_KEY, resolved)
    // 同时挂载为全局属性，方便 Options API 使用
    app.config.globalProperties.$appConfig = resolved
  },
}

// 声明全局属性类型扩展
declare module 'vue' {
  interface ComponentCustomProperties {
    $appConfig: AppConfig
  }
}

export { ConfigProvider as default }
