/**
 * 前端性能监控
 *
 * 基于 Browser Performance API + Sentry Browser Tracing，追踪：
 *   - Web Vitals (LCP, FID, CLS, TTFB)
 *   - API 调用耗时
 *   - 用户行为
 *   - 路由切换
 *   - 自定义业务指标
 */
import type { Router } from 'vue-router'
import { addSentryBreadcrumb, isSentryInitialized, startSentrySpan, finishSentrySpan } from './sentry'

// ── 类型定义 ────────────────────────────────────────────

export interface WebVitalsMetrics {
  LCP?: number  // Largest Contentful Paint (ms)
  FID?: number  // First Input Delay (ms)
  CLS?: number  // Cumulative Layout Shift (score)
  TTFB?: number // Time To First Byte (ms)
}

// ── Web Vitals ───────────────────────────────────────────

/**
 * 启动 Web Vitals 监测 (LCP, FID, CLS, TTFB)。
 * 使用 PerformanceObserver API，不依赖第三方 web-vitals 库。
 *
 * 在页面挂载后调用一次即可。
 */
export function trackPageLoad(): void {
  if (typeof window === 'undefined') return
  if (!('PerformanceObserver' in window)) return

  const reportMetric = (metric: string, value: number, unit: string = 'ms') => {
    // 上报到 Sentry 面包屑
    addSentryBreadcrumb(
      `${metric}: ${unit === 'score' ? value.toFixed(4) : Math.round(value)}${unit}`,
      { metric, value, unit },
      'performance'
    )

    // 开发环境控制台输出
    if (import.meta.env.DEV) {
      const formattedValue = unit === 'score' ? value.toFixed(4) : `${Math.round(value)}${unit}`
      console.warn(`[WebVitals] ${metric}: ${formattedValue}`)
    }
  }

  try {
    // ── LCP: Largest Contentful Paint ──
    const lcpObserver = new PerformanceObserver((list) => {
      const entries = list.getEntries()
      const lastEntry = entries[entries.length - 1]
      if (lastEntry) {
        reportMetric('LCP', lastEntry.startTime)
        lcpObserver.disconnect()
      }
    })
    lcpObserver.observe({ type: 'largest-contentful-paint', buffered: true })

    // ── FID: First Input Delay ──
    const fidObserver = new PerformanceObserver((list) => {
      for (const entry of list.getEntries()) {
        const fidEntry = entry as PerformanceEventTiming
        const delay = fidEntry.processingStart - fidEntry.startTime
        if (delay >= 0) {
          reportMetric('FID', delay)
        }
      }
      fidObserver.disconnect()
    })
    fidObserver.observe({ type: 'first-input', buffered: true })

    // ── CLS: Cumulative Layout Shift ──
    let clsValue = 0
    const clsObserver = new PerformanceObserver((list) => {
      for (const entry of list.getEntries() as any[]) {
        if (!entry.hadRecentInput) {
          clsValue += entry.value
        }
      }
    })
    clsObserver.observe({ type: 'layout-shift', buffered: true })

    // 页面隐藏时上报 CLS
    const onVisibilityChange = () => {
      if (document.visibilityState === 'hidden') {
        if (clsValue > 0) {
          reportMetric('CLS', clsValue, 'score')
        }
        window.removeEventListener('visibilitychange', onVisibilityChange)
      }
    }
    window.addEventListener('visibilitychange', onVisibilityChange)

    // ── TTFB: Time To First Byte ──
    const navigationEntries = performance.getEntriesByType('navigation')
    if (navigationEntries.length > 0) {
      const navTiming = navigationEntries[0] as PerformanceNavigationTiming
      const ttfb = navTiming.responseStart - navTiming.fetchStart
      if (ttfb > 0) {
        reportMetric('TTFB', ttfb)
      }
    } else {
      // 如果 navigation 条目尚未就绪，等 load 后获取
      window.addEventListener('load', () => {
        const entries = performance.getEntriesByType('navigation')
        if (entries.length > 0) {
          const navTiming = entries[0] as PerformanceNavigationTiming
          const ttfb = navTiming.responseStart - navTiming.fetchStart
          if (ttfb > 0) {
            reportMetric('TTFB', ttfb)
          }
        }
      }, { once: true })
    }
  } catch (e) {
    // PerformanceObserver 不支持
  }
}

/**
 * 获取当前 Web Vitals 快照（同步采集 Performance API 中已有数据）。
 */
export function getWebVitalsSnapshot(): WebVitalsMetrics {
  const metrics: WebVitalsMetrics = {}

  try {
    // TTFB
    const navEntries = performance.getEntriesByType('navigation')
    if (navEntries.length > 0) {
      const nav = navEntries[0] as PerformanceNavigationTiming
      metrics.TTFB = nav.responseStart - nav.fetchStart
    }

    // LCP：从 buffered entries 取最新值
    const lcpEntries = performance.getEntriesByType('largest-contentful-paint')
    if (lcpEntries.length > 0) {
      metrics.LCP = lcpEntries[lcpEntries.length - 1].startTime
    }

    // FID
    const fidEntries = performance.getEntriesByType('first-input')
    if (fidEntries.length > 0) {
      const fid = fidEntries[0] as PerformanceEventTiming
      metrics.FID = fid.processingStart - fid.startTime
    }
  } catch {
    // 静默
  }

  return metrics
}

// ── API 调用追踪 ────────────────────────────────────────

/**
 * 追踪 API 调用耗时。
 * 在 axios 拦截器中调用，每次请求完成时记录。
 *
 * @param url      - 请求 URL
 * @param method   - HTTP 方法
 * @param duration - 耗时 (ms)
 * @param status   - HTTP 状态码
 */
export function trackApiCall(
  url: string,
  method: string,
  duration: number,
  status: number
): void {
  const roundedDuration = Math.round(duration)
  const isError = status >= 400
  const category = isError ? 'api-error' : 'api'

  addSentryBreadcrumb(
    `${method} ${url} — ${status} (${roundedDuration}ms)`,
    {
      url,
      method: method.toUpperCase(),
      duration: roundedDuration,
      status,
    },
    category
  )

  // 慢请求告警（>2s 在控制台输出）
  if (duration > 2000 && import.meta.env.DEV) {
    console.warn(
      `[Performance] 慢请求: ${method.toUpperCase()} ${url} — ${roundedDuration}ms`
    )
  }

  // 对错误请求额外输出
  if (isError && import.meta.env.DEV) {
    console.error(
      `[API Error] ${method.toUpperCase()} ${url} — status ${status} (${roundedDuration}ms)`
    )
  }
}

// ── 用户行为追踪 ────────────────────────────────────────

/**
 * 追踪用户交互行为。
 * 如按钮点击、表单提交、页面切换等。
 *
 * @param action - 行为描述
 * @param data   - 附加上下文数据
 */
export function trackUserAction(
  action: string,
  data?: Record<string, any>
): void {
  addSentryBreadcrumb(action, data, 'user-action')

  if (import.meta.env.DEV) {
    console.warn(`[UserAction] ${action}`, data || '')
  }
}

// ── 路由切换追踪 ────────────────────────────────────────

let routeSetupDone = false

/**
 * 安装路由切换性能追踪。
 * 挂载 router.afterEach 钩子，自动记录每次路由跳转。
 *
 * @param router - Vue Router 实例
 */
export function setupRouteTracking(router: Router): void {
  if (routeSetupDone) return
  routeSetupDone = true

  let lastRouteChangeTime = 0
  let activeNavSpan: any = null

  router.beforeEach((_to, _from) => {
    lastRouteChangeTime = performance.now()
    // 如果 Sentry 已初始化，开启 navigation span
    if (isSentryInitialized()) {
      activeNavSpan = startSentrySpan('route-navigation', 'navigation')
    }
  })

  router.afterEach((to, from) => {
    const duration = Math.round(performance.now() - lastRouteChangeTime)
    const fromPath = from?.fullPath || '/'
    const toPath = to.fullPath

    addSentryBreadcrumb(
      `路由: ${fromPath} → ${toPath}`,
      {
        from: fromPath,
        to: toPath,
        duration,
      },
      'navigation'
    )

    // 完成 Sentry navigation span
    if (activeNavSpan) {
      finishSentrySpan(activeNavSpan)
      activeNavSpan = null
    }

    if (import.meta.env.DEV) {
      console.warn(`[Route] ${fromPath} → ${toPath} (${duration}ms)`)
    }
  })

  router.onError((error) => {
    addSentryBreadcrumb(
      `路由错误: ${error.message || String(error)}`,
      {
        error: error instanceof Error ? error.message : String(error),
        url: window.location.href,
      },
      'navigation'
    )

    if (activeNavSpan) {
      finishSentrySpan(activeNavSpan)
      activeNavSpan = null
    }
  })
}

/**
 * 手动上报单次路由切换（无需安装全局钩子时使用）。
 */
export function trackRouteChange(from: string, to: string): void {
  addSentryBreadcrumb(
    `路由: ${from} → ${to}`,
    { from, to, timestamp: performance.now() },
    'navigation'
  )
}

// ── 自定义业务指标 ──────────────────────────────────────

/**
 * 上报自定义业务指标。
 * 用于追踪特定的业务 SLA 指标（如页面渲染耗时、数据加载耗时等）。
 *
 * @param name  - 指标名称
 * @param value - 指标值
 * @param unit  - 单位 (ms / count / bytes / score)
 */
export function trackCustomMetric(
  name: string,
  value: number,
  unit: string = 'ms'
): void {
  if (import.meta.env.DEV) {
    console.warn(`[Metric] ${name}: ${value}${unit}`)
  }

  addSentryBreadcrumb(
    `${name}: ${value}${unit}`,
    { metric: name, value, unit },
    'metric'
  )
}

/**
 * 上报聚合指标（多次测量的统计值）。
 */
export function trackAggregatedMetric(
  name: string,
  count: number,
  avg: number,
  min: number,
  max: number,
  p95: number,
  unit: string = 'ms'
): void {
  addSentryBreadcrumb(
    `${name}: avg=${avg.toFixed(2)}${unit}, p95=${p95.toFixed(2)}${unit}, n=${count}`,
    { metric: name, count, avg, min, max, p95, unit },
    'metric'
  )
}

// ── 页面生命周期追踪 ────────────────────────────────────

/**
 * 追踪页面可见性变化（用户切换标签页、锁屏等）。
 */
export function trackVisibilityChanges(): void {
  if (typeof document === 'undefined') return

  document.addEventListener('visibilitychange', () => {
    const state = document.visibilityState
    addSentryBreadcrumb(
      `页面可见性: ${state}`,
      { visibilityState: state, timestamp: Date.now() },
      'navigation'
    )
  }, { passive: true })
}

// 重新从 sentry 导出，方便其他模块统一从 performanceMonitor 引用
export { addSentryBreadcrumb }
