/**
 * WebSocket Composable
 *
 * 通用 WebSocket 连接管理，支持：
 * - 自动重连（指数退避 + 随机抖动）
 * - 心跳保活
 * - 连接状态响应式追踪
 * - 消息队列缓存
 * - Token 认证
 * - 生命周期自动清理
 */
import { ref, shallowRef, computed, onUnmounted, type Ref } from 'vue'
import { getToken } from '@/utils/tokenRefresher'

// ── 类型定义 ──────────────────────────────────────────────

export type WsStatus = 'connecting' | 'connected' | 'disconnected' | 'reconnecting'

export interface UseWebSocketOptions {
  /** WebSocket 路径，如 /ws/metrics */
  path: string
  /** 是否附加认证 Token（从 tokenRefresher 获取） */
  auth?: boolean
  /** 是否自动重连（默认 true） */
  autoReconnect?: boolean
  /** 最大重连次数（默认 10，0 不限制） */
  maxReconnects?: number
  /** 重连基础间隔 ms（默认 3000） */
  reconnectInterval?: number
  /** 心跳间隔 ms（默认 30000，0 禁用） */
  heartbeatInterval?: number
  /** 连接成功回调 */
  onOpen?: (event: Event) => void
  /** 收到消息回调 */
  onMessage?: (data: any, event: MessageEvent) => void
  /** 发生错误回调 */
  onError?: (error: Event) => void
  /** 连接关闭回调 */
  onClose?: (event: CloseEvent) => void
  /** 自动在 onUnmounted 时断开（默认 true） */
  autoDisconnect?: boolean
}

export interface UseWebSocketReturn {
  /** 连接状态 */
  status: Ref<WsStatus>
  /** 是否已连接 */
  isConnected: Ref<boolean>
  /** 最后收到的消息（浅响应） */
  lastMessage: Ref<any>
  /** 重连次数 */
  reconnectCount: Ref<number>
  /** 发送文本/原始数据 */
  send: (data: unknown) => boolean
  /** 发送 JSON 消息 */
  sendJson: (payload: Record<string, unknown>) => boolean
  /** 手动连接 */
  connect: () => void
  /** 手动断开 */
  disconnect: () => void
  /** 手动重连（重置计数器） */
  reconnect: () => void
}

// ── 默认配置 ──────────────────────────────────────────────

const DEFAULTS = {
  auth: false,
  autoReconnect: true,
  maxReconnects: 10,
  reconnectInterval: 3000,
  heartbeatInterval: 30000,
  autoDisconnect: true,
}

// ── 工具函数 ──────────────────────────────────────────────

/**
 * 构建完整的 WebSocket URL
 * 开发环境通过 Vite proxy 转发，生产环境走 nginx
 */
function buildWsUrl(path: string, authToken?: string): string {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  let url = `${protocol}//${window.location.host}${path}`
  if (authToken) {
    const separator = path.includes('?') ? '&' : '?'
    url += `${separator}token=${encodeURIComponent(authToken)}`
  }
  return url
}

/**
 * 创建 WebSocket 连接
 *
 * 每个调用创建独立实例，可同时连接多个端点。
 */
export function useWebSocket(options: UseWebSocketOptions): UseWebSocketReturn {
  const status = ref<WsStatus>('disconnected')
  const isConnected = computed(() => status.value === 'connected')
  const lastMessage = shallowRef<any>(null)
  const reconnectCount = ref(0)

  // 实例级状态（每个 composable 独立）
  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null
  const messageQueue: string[] = []
  let reconnectAttempts = 0
  let manualDisconnect = false

  const opts = { ...DEFAULTS, ...options }

  // ── 发送消息 ────────────────────────────

  function send(data: unknown): boolean {
    const raw = typeof data === 'string' ? data : JSON.stringify(data)
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(raw)
      return true
    }
    // 未连接时入队，连接后自动发送
    messageQueue.push(raw)
    return false
  }

  function sendJson(payload: Record<string, unknown>): boolean {
    return send(JSON.stringify(payload))
  }

  // ── 心跳 ────────────────────────────────

  function startHeartbeat() {
    stopHeartbeat()
    if (!opts.heartbeatInterval || opts.heartbeatInterval <= 0) return

    heartbeatTimer = setInterval(() => {
      if (ws?.readyState === WebSocket.OPEN) {
        // command=HEARTBEAT，/ws/metrics 忽略心跳
        ws.send(JSON.stringify({ command: 'HEARTBEAT', timestamp: Date.now() }))
      }
    }, opts.heartbeatInterval)
  }

  function stopHeartbeat() {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  // ── 消息处理 ────────────────────────────

  function handleMessage(event: MessageEvent) {
    try {
      const data = JSON.parse(event.data)
      // 自动过滤心跳回执，不对外暴露
      if (data.type === 'HEARTBEAT' || data.type === 'PONG') return
      lastMessage.value = data
      opts.onMessage?.(data, event)
    } catch {
      lastMessage.value = event.data
      opts.onMessage?.(event.data, event)
    }
  }

  // ── 连接 / 断开 ─────────────────────────

  function clearTimers() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
  }

  function flushQueue() {
    while (messageQueue.length > 0) {
      const msg = messageQueue.shift()
      if (msg && ws?.readyState === WebSocket.OPEN) {
        ws.send(msg)
      }
    }
  }

  function doConnect() {
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
      return
    }

    manualDisconnect = false

    // 构建 URL（需要时附加 token）
    const token = opts.auth ? getToken() : undefined
    const url = buildWsUrl(opts.path, token || undefined)
    status.value = reconnectAttempts > 0 ? 'reconnecting' : 'connecting'

    try {
      ws = new WebSocket(url)
    } catch (err) {
      console.error(`[useWebSocket] 创建连接失败: ${opts.path}`, err)
      scheduleReconnect()
      return
    }

    ws.onopen = () => {
      reconnectAttempts = 0
      reconnectCount.value = 0
      status.value = 'connected'

      // 启动心跳
      startHeartbeat()

      // 发送队列中的消息
      flushQueue()

      opts.onOpen?.(new Event('open'))
    }

    ws.onmessage = handleMessage

    ws.onerror = (event) => {
      console.error(`[useWebSocket] 连接错误: ${opts.path}`, event)
      opts.onError?.(event)
    }

    ws.onclose = (event) => {
      stopHeartbeat()
      status.value = 'disconnected'
      opts.onClose?.(event)

      // 自动重连（非手动断开时）
      if (!manualDisconnect && opts.autoReconnect) {
        scheduleReconnect()
      }
    }
  }

  function scheduleReconnect() {
    if (opts.maxReconnects && opts.maxReconnects > 0 && reconnectAttempts >= opts.maxReconnects) {
      console.warn(`[useWebSocket] 已达最大重连次数 (${opts.maxReconnects})，停止重连: ${opts.path}`)
      return
    }

    reconnectAttempts++
    reconnectCount.value = reconnectAttempts

    // 指数退避 + 随机抖动：3s, 4.5s, 6.75s, ...
    const delay = Math.min(
      opts.reconnectInterval! * Math.pow(1.5, reconnectAttempts - 1) + Math.random() * 1000,
      30000
    )

    console.log(`[useWebSocket] ${delay.toFixed(0)}ms 后重连 (第 ${reconnectAttempts} 次): ${opts.path}`)
    reconnectTimer = setTimeout(() => doConnect(), delay)
  }

  function connect() {
    manualDisconnect = false
    reconnectAttempts = 0
    reconnectCount.value = 0
    doConnect()
  }

  function disconnect() {
    manualDisconnect = true
    clearTimers()
    stopHeartbeat()
    messageQueue.length = 0

    if (ws) {
      ws.onopen = null
      ws.onmessage = null
      ws.onerror = null
      ws.onclose = null
      if (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING) {
        ws.close(1000, 'Manual disconnect')
      }
      ws = null
    }

    status.value = 'disconnected'
  }

  function reconnect() {
    disconnect()
    reconnectAttempts = 0
    reconnectCount.value = 0
    doConnect()
  }

  // ── 自动连接 ────────────────────────────

  connect()

  // ── 生命周期清理 ────────────────────────

  if (opts.autoDisconnect !== false) {
    onUnmounted(() => disconnect())
  }

  return {
    status,
    isConnected,
    lastMessage,
    reconnectCount,
    send,
    sendJson,
    connect,
    disconnect,
    reconnect,
  }
}
