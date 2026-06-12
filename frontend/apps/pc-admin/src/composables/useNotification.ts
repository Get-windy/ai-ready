/**
 * useNotification - 通知系统
 *
 * 通过 REST API 轮询获取通知，当后端通知接口就绪后自动工作。
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { notificationApi } from '@/api/notification'
import type { NotificationInfo } from '@/api/notification'

export interface NotificationItem {
  id: string
  type: 'SYSTEM' | 'BUSINESS' | 'CHAT' | 'BROADCAST'
  title: string
  content: string
  time: string
  read: boolean
}

export function useNotification() {
  const router = useRouter()

  // ── 响应式状态 ──────────────────────────

  const notifications = ref<NotificationItem[]>([])
  const unreadCount = ref(0)
  const showDropdown = ref(false)
  const isPolling = ref(false)

  let pollingTimer: ReturnType<typeof setInterval> | null = null
  let pollFailCount = 0

  // ── REST 轮询 ───────────────────────────

  function startPolling() {
    if (isPolling.value || pollingTimer) return
    isPolling.value = true
    pollFailCount = 0
    pollNotify()
    pollingTimer = setInterval(pollNotify, 60000)
  }

  function stopPolling() {
    if (pollingTimer) {
      clearInterval(pollingTimer)
      pollingTimer = null
    }
    isPolling.value = false
  }

  async function pollNotify() {
    try {
      const countRes = await notificationApi.getUnreadCount()
      unreadCount.value = countRes.data?.total ?? 0

      const listRes = await notificationApi.getList({ pageNum: 1, pageSize: 10 })
      if (res && Array.isArray(res)) {
        notifications.value = res.map((n: NotificationInfo) => ({
          id: String(n.id),
          type: mapApiType(n.type),
          title: n.title,
          content: n.content || n.summary,
          time: n.sendTime || n.createTime || '',
          read: n.readStatus === 1,
        }))
      }
      pollFailCount = 0
    } catch {
      pollFailCount++
      if (pollFailCount >= 3) {
        console.warn('[通知] 轮询端点不可用（连续 3 次失败），停止轮询')
        stopPolling()
      }
    }
  }

  function mapApiType(type: number): NotificationItem['type'] {
    const map: Record<number, NotificationItem['type']> = {
      1: 'SYSTEM',
      2: 'BUSINESS',
      3: 'BUSINESS',
    }
    return map[type] || 'SYSTEM'
  }

  // ── 通知操作 ────────────────────────────

  /** 标记单条已读 */
  function markAsRead(id: string) {
    const item = notifications.value.find(n => n.id === id)
    if (item && !item.read) {
      item.read = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
      notificationApi.markAsRead(Number(id)).catch(() => {})
    }
  }

  /** 标记全部已读 */
  function markAllAsRead() {
    notifications.value.forEach(n => { n.read = true })
    unreadCount.value = 0
    notificationApi.markAllAsRead().catch(() => {})
  }

  /** 跳转通知页 */
  function goToNotificationPage() {
    showDropdown.value = false
    router.push('/notification')
  }

  /** 清除所有通知 */
  function clearAll() {
    notifications.value = []
    unreadCount.value = 0
  }

  // ── 生命周期 ────────────────────────────

  onMounted(() => {
    // 请求浏览器通知权限（仅首次）
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission()
    }
    // 启动轮询
    startPolling()
  })

  onUnmounted(() => {
    stopPolling()
  })

  return {
    /** 通知列表 */
    notifications,
    /** 未读计数 */
    unreadCount,
    /** 是否显示下拉 */
    showDropdown,
    /** 是否正在轮询 */
    isPolling,
    /** 标记已读 */
    markAsRead,
    /** 全部已读 */
    markAllAsRead,
    /** 清除全部 */
    clearAll,
    /** 跳转通知页 */
    goToNotificationPage,
  }
}
