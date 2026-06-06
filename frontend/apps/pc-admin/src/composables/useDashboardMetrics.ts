/**
 * useDashboardMetrics - 仪表盘实时指标
 *
 * 通过 WebSocket 连接 /ws/metrics 获取实时仪表盘数据，
 * 替代轮询方案，降低服务器负载。
 *
 * 使用：
 * ```ts
 * const metrics = useDashboardMetrics()
 * watch(() => metrics.data.value, (val) => { dashboardData.value = val })
 * ```
 */
import { ref, shallowRef } from 'vue'
import { useWebSocket } from './useWebSocket'
import type { WsResponse, DashboardMetricsData, MetricUpdate } from '@/types/websocket'

export interface DashboardMetricsState {
  /** 连接状态 */
  connected: boolean
  /** 最新仪表盘数据 */
  data: DashboardMetricsData | null
  /** 指标更新历史（保留最近 100 条） */
  metricHistory: MetricUpdate[]
  /** 最后更新时间 */
  lastUpdate: string | null
  /** 手动刷新 */
  refresh: () => void
  /** 主动断开 */
  disconnect: () => void
  /** 重新连接 */
  reconnect: () => void
}

export function useDashboardMetrics(): DashboardMetricsState {
  const dashboardData = shallowRef<DashboardMetricsData | null>(null)
  const metricHistory = ref<MetricUpdate[]>([])
  const lastUpdate = ref<string | null>(null)

  // ── WebSocket 连接（无需认证） ──────────

  const ws = useWebSocket({
    path: '/ws/metrics',
    auth: false,
    autoReconnect: true,
    maxReconnects: 0, // 持续重连
    heartbeatInterval: 0, // /ws/metrics 无心跳协议
    autoDisconnect: true,
    onMessage: handleMetricsMessage,
  })

  // ── 消息处理 ────────────────────────────

  function handleMetricsMessage(data: WsResponse) {
    switch (data.type) {
      case 'CONNECTED':
        // 连接成功后自动订阅全部指标
        ws.sendJson({ action: 'SUBSCRIBE', metrics: ['ALL'] })
        break

      case 'DASHBOARD_UPDATE':
        dashboardData.value = data.data as DashboardMetricsData
        lastUpdate.value = data.timestamp || new Date().toISOString()
        break

      case 'METRIC_UPDATE':
        // 单个指标更新
        if (data.metric) {
          const metric = data.metric as MetricUpdate
          metricHistory.value.unshift(metric)
          if (metricHistory.value.length > 100) {
            metricHistory.value = metricHistory.value.slice(0, 100)
          }
          // 同时更新仪表盘数据中的对应字段
          if (dashboardData.value) {
            dashboardData.value = { ...dashboardData.value, [metric.code]: metric.value }
          }
        }
        break

      case 'METRICS_UPDATE':
        // 批量指标更新
        if (Array.isArray(data.data)) {
          for (const metric of data.data as MetricUpdate[]) {
            metricHistory.value.unshift(metric)
          }
          if (metricHistory.value.length > 100) {
            metricHistory.value = metricHistory.value.slice(0, 100)
          }
        }
        break

      case 'ERROR':
        console.warn('[仪表盘] WebSocket 错误:', data.error)
        break
    }
  }

  // ── 外部接口 ────────────────────────────

  function refresh() {
    ws.sendJson({ action: 'REFRESH' })
  }

  return {
    connected: ws.isConnected,
    data: dashboardData,
    metricHistory,
    lastUpdate,
    refresh,
    disconnect: ws.disconnect,
    reconnect: ws.reconnect,
  }
}
