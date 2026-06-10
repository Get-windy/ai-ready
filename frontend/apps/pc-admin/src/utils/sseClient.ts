/**
 * SSE 客户端
 *
 * 通过 EventSource 连接后端 SSE 端点，接收服务端推送的实时通知。
 * 自动处理 token 传递、断线重连、事件分发。
 *
 * 支持的事件类型：
 * - `connected` — 连接建立确认
 * - `cache-invalidate` — 缓存失效通知，触发前端重新加载权限
 * - `notification` — 通用通知
 */

const SSE_RECONNECT_DELAY = 3000
const SSE_MAX_RECONNECT_ATTEMPTS = 10

type SseEventHandler = (data: any) => void

class SseClient {
  private eventSource: EventSource | null = null
  private reconnectAttempts = 0
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private handlers: Map<string, Set<SseEventHandler>> = new Map()
  private destroyed = false

  /**
   * 建立 SSE 连接
   * @param token 当前用户的登录 token
   */
  connect(token: string): void {
    if (this.destroyed) return
    this.disconnect()

    const url = `/api/sse/notifications?token=${encodeURIComponent(token)}`
    this.eventSource = new EventSource(url)

    this.eventSource.onopen = () => {
      this.reconnectAttempts = 0
      console.info('[SSE] 连接已建立')
    }

    this.eventSource.addEventListener('connected', (event: MessageEvent) => {
      console.info('[SSE] 连接确认:', event.data)
    })

    this.eventSource.addEventListener('cache-invalidate', (event: MessageEvent) => {
      console.info('[SSE] 收到缓存失效通知:', event.data)
      this.dispatch('cache-invalidate', event.data)
    })

    this.eventSource.addEventListener('notification', (event: MessageEvent) => {
      this.dispatch('notification', event.data)
    })

    this.eventSource.onerror = () => {
      console.warn('[SSE] 连接异常，准备重连...')
      this.eventSource?.close()
      this.scheduleReconnect(token)
    }
  }

  /** 断开 SSE 连接 */
  disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.eventSource) {
      this.eventSource.close()
      this.eventSource = null
    }
  }

  /** 销毁实例，不再重连 */
  destroy(): void {
    this.destroyed = true
    this.disconnect()
    this.handlers.clear()
  }

  /** 注册事件监听 */
  on(event: string, handler: SseEventHandler): void {
    if (!this.handlers.has(event)) {
      this.handlers.set(event, new Set())
    }
    this.handlers.get(event)!.add(handler)
  }

  /** 移除事件监听 */
  off(event: string, handler: SseEventHandler): void {
    const set = this.handlers.get(event)
    if (set) {
      set.delete(handler)
    }
  }

  private dispatch(event: string, data: string): void {
    const set = this.handlers.get(event)
    if (set) {
      let parsed: any
      try {
        parsed = JSON.parse(data)
      } catch {
        parsed = data
      }
      set.forEach(handler => {
        try {
          handler(parsed)
        } catch (e) {
          console.error('[SSE] 事件处理异常:', e)
        }
      })
    }
  }

  private scheduleReconnect(token: string): void {
    if (this.destroyed) return
    if (this.reconnectAttempts >= SSE_MAX_RECONNECT_ATTEMPTS) {
      console.warn('[SSE] 重连次数已达上限，停止重连')
      return
    }
    this.reconnectAttempts++
    const delay = SSE_RECONNECT_DELAY * Math.pow(1.5, this.reconnectAttempts - 1)
    console.info(`[SSE] ${delay}ms 后尝试第 ${this.reconnectAttempts} 次重连...`)
    this.reconnectTimer = setTimeout(() => {
      this.connect(token)
    }, delay)
  }
}

/** 全局 SSE 客户端单例 */
let sseClient: SseClient | null = null

/**
 * 获取 SSE 客户端单例
 */
export function getSseClient(): SseClient {
  if (!sseClient) {
    sseClient = new SseClient()
  }
  return sseClient
}

/**
 * 销毁 SSE 客户端（登出时调用）
 */
export function destroySseClient(): void {
  if (sseClient) {
    sseClient.destroy()
    sseClient = null
  }
}

export default SseClient
