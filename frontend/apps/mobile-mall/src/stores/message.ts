import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 消息中心（底部「消息」Tab 的未读角标 + 消息列表）。
 *
 * <p>数据源：订单状态变更 / 审核结果 / 发货通知 / 活动公告。</p>
 *
 * <p>⚠️ 后端**按 shop_user 维度的 C 端消息接口尚未提供**（设计文档 `F-10`）——
 * 现有 `sys_notification_record` 相关端点都是按 `sys_user` 的（管理端用户），
 * 商城买家是 `shop_user`，两者不是同一套身份。因此本 store 现在只维护本地已读状态，
 * `unreadCount` 在拿到真实接口前为 0（**不造假数据**）。
 * `F-10` 落地后，把 `loadUnread` 换成真实请求即可，角标会自动生效。</p>
 */

export interface MallMessage {
  id: string
  type: 'ORDER' | 'AUDIT' | 'SHIP' | 'PROMO' | 'SYSTEM'
  title: string
  content?: string
  bizId?: string
  read?: boolean
  createTime?: string
}

const READ_KEYS = 'mall_message_read_ids'

export const useMessageStore = defineStore('message', () => {
  const userStore = useUserStore()

  const messages = ref<MallMessage[]>([])
  const loading = ref(false)
  /** 后端接口未就绪时为 true，UI 据此显示"暂无消息"而不是转圈 */
  const pendingBackend = ref(true)

  const readIds = ref<Set<string>>(new Set(loadLocalRead()))

  function loadLocalRead(): string[] {
    try {
      return JSON.parse(localStorage.getItem(READ_KEYS) || '[]')
    } catch {
      return []
    }
  }

  function persistRead() {
    localStorage.setItem(READ_KEYS, JSON.stringify([...readIds.value]))
  }

  const unreadCount = computed(() =>
    userStore.isLoggedIn ? messages.value.filter(m => !m.read && !readIds.value.has(m.id)).length : 0
  )

  /**
   * 拉取消息。后端 `F-10` 未落地前**不发请求**（发了也是 404），
   * 直接置空并标记 pendingBackend，避免控制台刷 404。
   */
  const load = async () => {
    if (!userStore.isLoggedIn) {
      messages.value = []
      return
    }
    if (pendingBackend.value) {
      messages.value = []
      return
    }
    loading.value = true
    try {
      // TODO(F-10): 接入 GET /v1/mall/messages
      messages.value = []
    } finally {
      loading.value = false
    }
  }

  const markRead = (id: string) => {
    readIds.value.add(id)
    persistRead()
    const hit = messages.value.find(m => m.id === id)
    if (hit) hit.read = true
  }

  const markAllRead = () => {
    messages.value.forEach(m => {
      m.read = true
      readIds.value.add(m.id)
    })
    persistRead()
  }

  return { messages, loading, pendingBackend, unreadCount, load, markRead, markAllRead }
})
