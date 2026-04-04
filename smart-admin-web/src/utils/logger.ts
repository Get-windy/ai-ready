/**
 * 日志工具类
 * 开发环境输出日志，生产环境自动静默
 */

type LogLevel = 'debug' | 'info' | 'warn' | 'error'

class Logger {
  private isDevelopment: boolean
  
  constructor() {
    this.isDevelopment = import.meta.env.DEV
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
    // 错误日志始终输出，但可接入错误监控系统
    console.error('[ERROR]', ...args)
    
    // TODO: 接入错误监控系统（如 Sentry）
    // if (import.meta.env.PROD) {
    //   Sentry.captureException(args[0])
    // }
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
