/**
 * 性能监控工具
 * 用于追踪和报告前端性能指标
 */

/**
 * 性能指标类型
 */
export interface PerformanceMetrics {
  /** 页面加载时间（ms） */
  pageLoadTime?: number
  /** DOM 内容加载完成时间（ms） */
  domContentLoadedTime?: number
  /** 首次内容绘制（ms） */
  firstContentfulPaint?: number
  /** 首次绘制（ms） */
  firstPaint?: number
  /** 首次有意义的绘制（ms） */
  firstMeaningfulPaint?: number
  /** 首次输入延迟（ms） */
  firstInputDelay?: number
  /** 最大内容绘制（ms） */
  largestContentfulPaint?: number
  /** 累积布局偏移 */
  cumulativeLayoutShift?: number
  /** 首次字节时间（ms） */
  timeToFirstByte?: number
  /** 自定义指标 */
  customMetrics?: Record<string, number>
}

/**
 * 性能数据
 */
export interface PerformanceData {
  /** 指标名称 */
  name: string
  /** 指标值 */
  value: number
  /** 单位 */
  unit: 'ms' | 'score' | 'bytes' | 'ratio'
  /** 评级 */
  rating?: 'good' | 'needs-improvement' | 'poor'
  /** 时间戳 */
  timestamp: number
}

/**
 * 性能监控类
 */
class PerformanceMonitor {
  private metrics: PerformanceMetrics = {}
  private observers: PerformanceObserver[] = []
  private customMetrics: Map<string, number[]> = new Map()

  /**
   * 初始化性能监控
   */
  init() {
    // 等待页面加载完成后收集指标
    if (document.readyState === 'complete') {
      this.collectMetrics()
    } else {
      window.addEventListener('load', () => this.collectMetrics())
    }

    // 监听性能指标
    this.observePerformanceEntries()
  }

  /**
   * 收集基础性能指标
   */
  private collectMetrics() {
    const timing = performance.timing
    const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming

    if (navigation) {
      this.metrics.pageLoadTime = navigation.loadEventEnd - navigation.fetchStart
      this.metrics.domContentLoadedTime = navigation.domContentLoadedEventEnd - navigation.fetchStart
      this.metrics.timeToFirstByte = navigation.responseStart - navigation.fetchStart
    } else if (timing) {
      this.metrics.pageLoadTime = timing.loadEventEnd - timing.fetchStart
      this.metrics.domContentLoadedTime = timing.domContentLoadedEventEnd - timing.fetchStart
      this.metrics.timeToFirstByte = timing.responseStart - timing.fetchStart
    }

    // 获取 Paint 指标
    const paintEntries = performance.getEntriesByType('paint')
    paintEntries.forEach((entry) => {
      const paintEntry = entry as PerformancePaintTiming
      if (paintEntry.name === 'first-contentful-paint') {
        this.metrics.firstContentfulPaint = paintEntry.startTime
      } else if (paintEntry.name === 'first-paint') {
        this.metrics.firstPaint = paintEntry.startTime
      }
    })
  }

  /**
   * 监听性能条目
   */
  private observePerformanceEntries() {
    try {
      // 监听 LCP（最大内容绘制）
      if ('PerformanceObserver' in window) {
        const lcpObserver = new PerformanceObserver((list) => {
          const entries = list.getEntries()
          if (entries.length > 0) {
            const lastEntry = entries[entries.length - 1] as any
            this.metrics.largestContentfulPaint = lastEntry.startTime
          }
        })
        lcpObserver.observe({ type: 'largest-contentful-paint', buffered: true })
        this.observers.push(lcpObserver)

        // 监听 CLS（累积布局偏移）
        const clsObserver = new PerformanceObserver((list) => {
          let clsValue = 0
          for (const entry of list.getEntries() as any[]) {
            if (!entry.hadRecentInput) {
              clsValue += entry.value
            }
          }
          this.metrics.cumulativeLayoutShift = clsValue
        })
        clsObserver.observe({ type: 'layout-shift', buffered: true })
        this.observers.push(clsObserver)

        // 监听 FID（首次输入延迟）
        const fidObserver = new PerformanceObserver((list) => {
          const entries = list.getEntries()
          if (entries.length > 0) {
            const fidEntry = entries[0] as any
            this.metrics.firstInputDelay = fidEntry.processingStart - fidEntry.startTime
          }
        })
        fidObserver.observe({ type: 'first-input', buffered: true })
        this.observers.push(fidObserver)
      }
    } catch (error) {
      console.warn('PerformanceObserver not supported or failed to initialize:', error)
    }
  }

  /**
   * 记录自定义指标
   * @param name 指标名称
   * @param value 指标值
   */
  recordMetric(name: string, value: number) {
    if (!this.customMetrics.has(name)) {
      this.customMetrics.set(name, [])
    }
    this.customMetrics.get(name)!.push(value)

    if (!this.metrics.customMetrics) {
      this.metrics.customMetrics = {}
    }
    this.metrics.customMetrics[name] = value
  }

  /**
   * 获取平均指标
   * @param name 指标名称
   */
  getAverageMetric(name: string): number | null {
    const values = this.customMetrics.get(name)
    if (!values || values.length === 0) return null

    const sum = values.reduce((acc, val) => acc + val, 0)
    return sum / values.length
  }

  /**
   * 获取所有性能指标
   */
  getMetrics(): PerformanceMetrics {
    return { ...this.metrics }
  }

  /**
   * 评估性能等级
   * @param name 指标名称
   * @param value 指标值
   */
  private evaluatePerformance(name: string, value: number): 'good' | 'needs-improvement' | 'poor' {
    switch (name) {
      case 'FCP':
        return value <= 1800 ? 'good' : value <= 3000 ? 'needs-improvement' : 'poor'
      case 'LCP':
        return value <= 2500 ? 'good' : value <= 4000 ? 'needs-improvement' : 'poor'
      case 'FID':
        return value <= 100 ? 'good' : value <= 300 ? 'needs-improvement' : 'poor'
      case 'CLS':
        return value <= 0.1 ? 'good' : value <= 0.25 ? 'needs-improvement' : 'poor'
      case 'TTFB':
        return value <= 800 ? 'good' : value <= 1800 ? 'needs-improvement' : 'poor'
      default:
        return 'good'
    }
  }

  /**
   * 获取性能报告
   */
  getReport(): PerformanceData[] {
    const report: PerformanceData[] = []

    if (this.metrics.firstContentfulPaint !== undefined) {
      report.push({
        name: 'FCP',
        value: this.metrics.firstContentfulPaint,
        unit: 'ms',
        rating: this.evaluatePerformance('FCP', this.metrics.firstContentfulPaint),
        timestamp: Date.now()
      })
    }

    if (this.metrics.largestContentfulPaint !== undefined) {
      report.push({
        name: 'LCP',
        value: this.metrics.largestContentfulPaint,
        unit: 'ms',
        rating: this.evaluatePerformance('LCP', this.metrics.largestContentfulPaint),
        timestamp: Date.now()
      })
    }

    if (this.metrics.firstInputDelay !== undefined) {
      report.push({
        name: 'FID',
        value: this.metrics.firstInputDelay,
        unit: 'ms',
        rating: this.evaluatePerformance('FID', this.metrics.firstInputDelay),
        timestamp: Date.now()
      })
    }

    if (this.metrics.cumulativeLayoutShift !== undefined) {
      report.push({
        name: 'CLS',
        value: this.metrics.cumulativeLayoutShift,
        unit: 'ratio',
        rating: this.evaluatePerformance('CLS', this.metrics.cumulativeLayoutShift),
        timestamp: Date.now()
      })
    }

    if (this.metrics.timeToFirstByte !== undefined) {
      report.push({
        name: 'TTFB',
        value: this.metrics.timeToFirstByte,
        unit: 'ms',
        rating: this.evaluatePerformance('TTFB', this.metrics.timeToFirstByte),
        timestamp: Date.now()
      })
    }

    if (this.metrics.pageLoadTime !== undefined) {
      report.push({
        name: 'Page Load',
        value: this.metrics.pageLoadTime,
        unit: 'ms',
        timestamp: Date.now()
      })
    }

    return report
  }

  /**
   * 销毁监控器
   */
  destroy() {
    this.observers.forEach((observer) => observer.disconnect())
    this.observers = []
    this.metrics = {}
    this.customMetrics.clear()
  }
}

/**
 * 创建性能计时器
 */
export class PerformanceTimer {
  private startTime: number = 0
  private endTime: number = 0

  /**
   * 开始计时
   */
  start() {
    this.startTime = performance.now()
  }

  /**
   * 结束计时
   */
  end(): number {
    this.endTime = performance.now()
    return this.endTime - this.startTime
  }

  /**
   * 获取耗时（ms）
   */
  getDuration(): number {
    if (this.startTime === 0) return 0
    return (this.endTime || performance.now()) - this.startTime
  }

  /**
   * 重置计时器
   */
  reset() {
    this.startTime = 0
    this.endTime = 0
  }
}

/**
 * 性能装饰器
 * 用于测量函数执行时间
 */
export function measurePerformance(target: any, propertyKey: string, descriptor: PropertyDescriptor) {
  const originalMethod = descriptor.value

  descriptor.value = async function (...args: any[]) {
    const timer = new PerformanceTimer()
    timer.start()

    try {
      const result = await originalMethod.apply(this, args)
      const duration = timer.end()

      console.warn(`[Performance] ${propertyKey} executed in ${duration.toFixed(2)}ms`)
      return result
    } catch (error) {
      const duration = timer.end()
      console.error(`[Performance] ${propertyKey} failed after ${duration.toFixed(2)}ms`, error)
      throw error
    }
  }

  return descriptor
}

/**
 * 异步性能测量
 * @param fn 要测量的函数
 * @param name 指标名称
 */
export async function measureAsyncPerformance<T>(
  fn: () => Promise<T>,
  name?: string
): Promise<T> {
  const timer = new PerformanceTimer()
  timer.start()

  try {
    const result = await fn()
    const duration = timer.end()

    const label = name || fn.name || 'anonymous'
    console.warn(`[Performance] ${label} executed in ${duration.toFixed(2)}ms`)

    return result
  } catch (error) {
    const duration = timer.end()
    console.error(`[Performance] ${name || fn.name || 'anonymous'} failed after ${duration.toFixed(2)}ms`, error)
    throw error
  }
}

// 创建全局性能监控器实例
export const performanceMonitor = new PerformanceMonitor()

// 自动初始化
if (typeof window !== 'undefined') {
  performanceMonitor.init()
}

export default performanceMonitor