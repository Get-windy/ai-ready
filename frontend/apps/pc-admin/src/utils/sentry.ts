/**
 * Sentry RUM (Real User Monitoring) & Error Tracking
 *
 * 安装: pnpm add @sentry/vue
 *
 * 功能：错误捕获、性能监控（BrowserTracing）、会话回放（Replay）、
 *       性能剖析（BrowserProfiling）、用户行为追踪、敏感数据过滤
 */
import type { App } from 'vue'
import type { Router } from 'vue-router'

// ── 模块级状态 ──────────────────────────────────────────

/** Sentry SDK 模块引用（动态导入后缓存） */
let SentryModule: any = null

/** Sentry 是否已成功初始化 */
let sentryInitialized = false

// ── 敏感字段列表（beforeSend 时过滤） ──────────────────

const SENSITIVE_HEADERS = [
  'authorization',
  'cookie',
  'set-cookie',
  'x-api-key',
  'x-auth-token',
]

const SENSITIVE_BODY_FIELDS = [
  'password',
  'passwd',
  'oldPassword',
  'newPassword',
  'confirmPassword',
  'token',
  'accessToken',
  'refreshToken',
  'secret',
  'apiKey',
  'creditCard',
  'ssn',
  'idCard',
]

// ── 非可操作错误（不上报 Sentry） ─────────────────────

const NON_ACTIONABLE_ERRORS = [
  'ResizeObserver loop limit exceeded',
  'ResizeObserver loop completed with undelivered notifications',
  'Non-Error promise rejection captured',
  'Non-Error promise rejection captured with object',
  'Request aborted',
  'AbortError',
  'cancel',
  'canceled',
  'Loading chunk',
  'Loading CSS chunk',
  'Failed to fetch',
  'Network Error',
  'Network request failed',
  'Script error',
  'Script error.',
  'Uncaught Error: [courier]', // 第三方埋点 SDK
  'chrome-extension',
  'moz-extension',
]

// ── PII 正则模式（用于消息文本过滤） ──────────────────

const PII_PATTERNS: Array<[RegExp, string]> = [
  // 邮箱
  [/\b[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}\b/g, '[EMAIL_REDACTED]'],
  // 中国大陆手机号
  [/\b1[3-9]\d{9}\b/g, '[PHONE_REDACTED]'],
  // 身份证号（18位）
  [/\b\d{17}[\dXx]\b/g, '[ID_REDACTED]'],
]

// ── 敏感数据过滤 ───────────────────────────────────────

function redactPII(str: string): string {
  let result = str
  for (const [pattern, replacement] of PII_PATTERNS) {
    result = result.replace(pattern, replacement)
  }
  return result
}

function sanitizeRequestData(data: any): any {
  if (!data || typeof data !== 'object') return data
  if (Array.isArray(data)) return data.map(sanitizeRequestData)

  const sanitized: Record<string, any> = {}
  for (const key of Object.keys(data)) {
    const lowerKey = key.toLowerCase()
    if (SENSITIVE_BODY_FIELDS.some(f => lowerKey.includes(f.toLowerCase()))) {
      sanitized[key] = '[FILTERED]'
    } else if (typeof data[key] === 'object' && data[key] !== null) {
      sanitized[key] = sanitizeRequestData(data[key])
    } else {
      sanitized[key] = data[key]
    }
  }
  return sanitized
}

// ── 初始化 Sentry ───────────────────────────────────────

export async function initSentry(app: App, router?: Router): Promise<void> {
  const dsn = import.meta.env.VITE_SENTRY_DSN

  // 未配置 DSN 时跳过
  if (!dsn || dsn === '' || dsn === 'VITE_SENTRY_DSN_PLACEHOLDER') {
    if (import.meta.env.DEV) {
      console.warn('[Sentry] DSN 未配置，跳过初始化。请在 .env.development 中设置 VITE_SENTRY_DSN')
    }
    return
  }

  try {
    const Sentry = await import('@sentry/vue')
    SentryModule = Sentry

    const release = import.meta.env.VITE_APP_VERSION || '1.0.0'
    const environment = import.meta.env.MODE || 'development'

    Sentry.init({
      app,
      dsn,
      environment,
      release,
      integrations: [
        // 浏览器 Tracing：自动创建路由切换、XHR/Fetch 事务
        Sentry.browserTracingIntegration({
          router,
          tracingOrigins: ['localhost', /^\//],
        }),
        // 会话回放
        Sentry.replayIntegration({
          maskAllText: false,
          maskAllInputs: true,
          blockAllMedia: false,
          maskFn: (text: string) => {
            // 对敏感字段做脱敏处理
            return redactPII(text)
          },
        }),
        // 浏览器性能剖析
        Sentry.browserProfilingIntegration(),
      ],

      // 追踪采样率：生产 10%，开发 100%
      tracesSampleRate: import.meta.env.PROD ? 0.1 : 1.0,

      // 回放采样率
      replaysSessionSampleRate: 0.1,
      replaysOnErrorSampleRate: 1.0,

      // 性能剖析采样率
      profilesSampleRate: import.meta.env.PROD ? 0.1 : 1.0,

      // 事件发送前过滤
      beforeSend(event, _hint) {
        // ── 过滤请求中的敏感字段 ──
        if (event.request) {
          // 过滤请求体中的敏感字段
          if (event.request.data && typeof event.request.data === 'object') {
            event.request.data = sanitizeRequestData(event.request.data)
          }

          // 过滤 URL 查询参数中的 token
          if (event.request.url) {
            event.request.url = event.request.url
              .replace(/([?&])(token|access_token|refresh_token|api_key|apikey)=[^&]+/gi, '$1$2=[FILTERED]')
          }

          // 过滤敏感请求头
          if (event.request.headers) {
            const filteredHeaders: Record<string, string> = {}
            for (const key of Object.keys(event.request.headers)) {
              const lowerKey = key.toLowerCase()
              filteredHeaders[key] = SENSITIVE_HEADERS.some(h => lowerKey.includes(h))
                ? '[FILTERED]'
                : (event.request.headers as Record<string, string>)[key]
            }
            event.request.headers = filteredHeaders
          }
        }

        // ── 过滤 breadcrumbs 中的敏感数据 ──
        if (event.breadcrumbs) {
          event.breadcrumbs = event.breadcrumbs.map(breadcrumb => {
            if (breadcrumb.data && typeof breadcrumb.data === 'object') {
              breadcrumb.data = sanitizeRequestData(breadcrumb.data)
            }
            if (breadcrumb.message) {
              breadcrumb.message = redactPII(breadcrumb.message)
            }
            return breadcrumb
          })
        }

        // ── 过滤消息中的 PII ──
        if (event.message) {
          event.message = redactPII(event.message)
        }

        // ── 过滤异常值中的敏感数据 ──
        if (event.exception?.values) {
          for (const value of event.exception.values) {
            if (value.value) {
              value.value = redactPII(value.value)
            }
          }
        }

        return event
      },

      // 忽略非可操作错误
      ignoreErrors: NON_ACTIONABLE_ERRORS,

      // 传输前回调：按环境过滤
      beforeSendTransaction(event) {
        // 生产环境不发送开发事务
        if (import.meta.env.PROD === false && event.transaction?.startsWith('/@')) {
          return null
        }
        return event
      },
    })

    // ── 设置默认标签 ──
    Sentry.setTag('app_version', release)
    Sentry.setTag('environment', environment)

    sentryInitialized = true
    console.warn(`[Sentry] 初始化完成 (env: ${environment}, release: ${release})`)
  } catch (error) {
    console.warn('[Sentry] 初始化失败（SDK 可能未安装，运行 pnpm add @sentry/vue）:', error)
    sentryInitialized = false
  }
}

// ── 用户上下文管理 ────────────────────────────────────

/**
 * 设置 Sentry 用户上下文。
 * 登录成功后调用，将用户信息关联到后续所有错误/性能事件。
 */
export function setSentryUser(user: {
  id: number | string
  username?: string
  tenantId?: number | string
}): void {
  if (!sentryInitialized || !SentryModule) return

  SentryModule.setUser({
    id: String(user.id),
    username: user.username || '',
    ...(user.tenantId !== undefined && user.tenantId !== null
      ? { tenantId: String(user.tenantId) }
      : {}),
  })

  // 设置租户标签，便于 Sentry 按租户筛选
  if (user.tenantId !== undefined && user.tenantId !== null) {
    SentryModule.setTag('tenant', String(user.tenantId))
  }
}

/**
 * 清除 Sentry 用户上下文（退出登录时调用）。
 */
export function clearSentryUser(): void {
  if (!sentryInitialized || !SentryModule) return
  SentryModule.setUser(null)
}

// ── 手动错误上报 ────────────────────────────────────────

/**
 * 手动捕获异常并上报 Sentry。
 * 即使 Sentry 未初始化也不会抛出异常——静默降级。
 *
 * @param error  - Error 对象或错误消息字符串
 * @param context - 附加的上下文数据（会作为 extra 上报）
 */
export function captureException(
  error: Error | string,
  context?: Record<string, any>
): void {
  const errorObj = typeof error === 'string' ? new Error(error) : error

  console.error('[Sentry] captureException:', errorObj, context || '')

  if (!sentryInitialized || !SentryModule) return

  try {
    SentryModule.captureException(errorObj, {
      extra: context,
      tags: {
        captured_manually: true,
      },
    })
  } catch (e) {
    // Sentry 上报自身失败时静默处理
  }
}

// ── Breadcrumb 面包屑 ───────────────────────────────────

/**
 * 添加 Sentry 面包屑（操作轨迹）。
 * 在前端操作、API 调用等关键节点埋点，便于问题排查。
 *
 * @param message  - 面包屑描述
 * @param data     - 附加数据
 * @param category - 分类（custom / navigation / api / user-action / performance）
 */
export function addSentryBreadcrumb(
  message: string,
  data?: Record<string, any>,
  category: string = 'custom'
): void {
  if (!sentryInitialized || !SentryModule) return

  try {
    SentryModule.addBreadcrumb({
      message,
      data: data ? sanitizeRequestData(data) : undefined,
      category,
      level: 'info',
      timestamp: new Date().getTime() / 1000,
    })
  } catch {
    // 静默处理
  }
}

// ── Transaction / Span 追踪 ──────────────────────────────

/**
 * 开启一个 Sentry Span（事务追踪单元）。
 * 用于追踪自定义操作的性能。
 *
 * @param name - Span 名称
 * @param op   - 操作类型
 * @returns Span 对象（可用于 finish），Sentry 未初始化时返回 null
 */
export function startSentrySpan(name: string, op: string = 'custom'): any {
  if (!sentryInitialized || !SentryModule) return null

  try {
    return SentryModule.startSpan({ name, op })
  } catch {
    return null
  }
}

/**
 * 完成一个 Sentry Span。
 */
export function finishSentrySpan(span: any): void {
  if (!span || !sentryInitialized || !SentryModule) return

  try {
    span.end()
  } catch {
    // 静默处理
  }
}

// ── 设置自定义标签 ──────────────────────────────────────

/**
 * 设置全局标签，会对后续所有事件生效。
 */
export function setSentryTag(key: string, value: string): void {
  if (!sentryInitialized || !SentryModule) return
  SentryModule.setTag(key, value)
}

// ── 查询状态 ────────────────────────────────────────────

export function isSentryInitialized(): boolean {
  return sentryInitialized
}

// ── getter 供内部模块使用 ──────────────────────────────

export function getSentryModule(): any {
  return SentryModule
}
