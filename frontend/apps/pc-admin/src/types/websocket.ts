/**
 * WebSocket 消息类型定义
 * 与后端 WebSocket 协议对齐
 */

// ═══════════════════════════════════════════
// 通用类型
// ═══════════════════════════════════════════

/** 服务端推送的响应消息 */
export interface WsResponse<T = any> {
  type: string
  data: T
  timestamp: string
  messageId?: string
  code?: number
  error?: string
}

// ═══════════════════════════════════════════
// 通知 / 仪表盘实时指标
// ═══════════════════════════════════════════

/** 客户端发送的命令 */
export interface WsChatCommand {
  command: 'HEARTBEAT' | 'JOIN_ROOM' | 'LEAVE_ROOM' | 'SEND_MESSAGE' | 'READ_MESSAGE' | 'GET_HISTORY'
  roomId?: string
  messageId?: string
  targetUserId?: number
  content?: string
  messageType?: number
  page?: number
  size?: number
}

/** 服务端推送的通知消息 */
export interface NotificationMessage {
  id: string
  type: 'SYSTEM' | 'BUSINESS' | 'CHAT' | 'BROADCAST'
  title: string
  content: string
  senderId?: number
  senderName?: string
  receiverId: number
  roomId?: string
  readStatus: 0 | 1
  createTime: string
}

/** 未读通知数响应 */
export interface UnreadCountData {
  count: number
  notifications?: NotificationMessage[]
}

// ═══════════════════════════════════════════
// /ws/metrics 协议（指标推送）
// ═══════════════════════════════════════════

/** 客户端发送的指标命令 */
export interface WsMetricsCommand {
  action: 'SUBSCRIBE' | 'UNSUBSCRIBE' | 'REFRESH'
  metrics?: string[]
}

/** 服务端推送的仪表盘数据 */
export interface DashboardMetricsData {
  [key: string]: number | string | Record<string, unknown>
}

/** 单个指标更新 */
export interface MetricUpdate {
  code: string
  name: string
  value: number
  unit?: string
  timestamp: string
}

/** 指标订阅类型 */
export const METRICS_EVENTS = {
  CONNECTED: 'CONNECTED',
  DASHBOARD_UPDATE: 'DASHBOARD_UPDATE',
  METRIC_UPDATE: 'METRIC_UPDATE',
  METRICS_UPDATE: 'METRICS_UPDATE',
  SUBSCRIBED: 'SUBSCRIBED',
  ERROR: 'ERROR',
} as const
