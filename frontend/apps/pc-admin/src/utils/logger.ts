/**
 * 日志工具类
 * 开发环境输出日志，生产环境自动静默
 */

type LogLevel = 'debug' | 'info' | 'warn' | 'error'

class Logger {
  private isDevelopment: boolean
  private sentryBreadcrumb: ((message: string, data?: Record<string, unknown>) => void) | null = null

  constructor() {
    this.isDevelopment = import.meta.env.DEV
    this.initSentry()
  }

  /**
   * 按需初始化 Sentry 错误监控
   * 仅当配置了 VITE_SENTRY_DSN 环境变量时才启用
   */
  private async initSentry(): Promise<void> {
    const sentryDsn = import.meta.env.VITE_SENTRY_DSN
    if (!sentryDsn) return

    try {
      const Sentry = await import('@sentry/vue')
      // Sentry 初始化在 main.ts 完成，此处仅注册 breadcrumb 辅助方法
      this.sentryBreadcrumb = (message: string, data?: Record<string, unknown>) => {
        try {
          Sentry.addBreadcrumb({ message, data, level: 'error', category: 'logger' })
        } catch { /* sentry 不可用时静默 */ }
      }
      this.debug('Sentry 错误监控已接入')
    } catch {
      // @sentry/vue 未安装
      this.debug('Sentry SDK 未安装，错误监控未启用')
    }
  }
  
  /**
   * 调试日志 - 仅开发环境输出
   */
  debug(...args: unknown[]): void {
    if (this.isDevelopment) {
      console.log('[DEBUG]', ...args)
    }
  }
  
  /**
   * 信息日志 - 仅开发环境输出
   */
  info(...args: unknown[]): void {
    if (this.isDevelopment) {
      console.info('[INFO]', ...args)
    }
  }
  
  /**
   * 警告日志 - 开发和测试环境输出
   */
  warn(...args: unknown[]): void {
    if (this.isDevelopment || import.meta.env.MODE === 'test') {
      console.warn('[WARN]', ...args)
    }
  }
  
  /**
   * 错误日志 - 始终输出
   */
  error(...args: unknown[]): void {
    // 错误日志始终输出
    console.error('[ERROR]', ...args)

    // 启用 Sentry 时发送错误 breadcrumb
    if (this.sentryBreadcrumb && args.length > 0) {
      const message = typeof args[0] === 'string' ? args[0] : String(args[0])
      this.sentryBreadcrumb(message, args.length > 1 ? { detail: args.slice(1) } : undefined)
    }
  }
  
  /**
   * 分组日志
   */
  group(label: string, fn: () => void): void {
    if (this.isDevelopment) {
      console.group(label)
      fn()
      console.groupEnd()
    }
  }
  
  /**
   * 计时日志
   */
  time(label: string): void {
    if (this.isDevelopment) {
      console.time(label)
    }
  }
  
  timeEnd(label: string): void {
    if (this.isDevelopment) {
      console.timeEnd(label)
    }
  }
  
  /**
   * 表格日志
   */
  table(data: unknown): void {
    if (this.isDevelopment) {
      console.table(data)
    }
  }
}

// 导出单例
export const logger = new Logger()

// 默认导出
export default logger

/**
 * 使用示例：
 * 
 * import { logger } from '@/utils/logger'
 * 
 * logger.debug('调试信息', data)
 * logger.info('普通信息', result)
 * logger.warn('警告信息')
 * logger.error('错误信息', error)
 * 
 * logger.group('API请求', () => {
 *   logger.debug('请求参数', params)
 *   logger.debug('响应结果', response)
 * })
 * 
 * logger.time('数据处理')
 * // ... 耗时操作
 * logger.timeEnd('数据处理')
 */
