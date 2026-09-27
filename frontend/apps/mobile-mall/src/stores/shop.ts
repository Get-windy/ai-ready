import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '@/api'
import { useUserStore } from '@/stores/user'

/**
 * 店铺配置与「价格三态」。
 *
 * <p>2026-09-26 新建，对应《商城App设计方案》§五（配置落位表）与 §七（三态价格）。
 * 这里是**后台 82 个"只写不读"字段在 C 端的唯一入口** —— 页面不再各读各的，
 * 统一从本 store 取开关，避免出现"某个页面忘了判 guestShowPrice"。
 */

/** 后端下发的店铺配置（白名单字段；不含任何凭据）—— 与 ShopConfigVO 对齐 */
export interface ShopConfig {
  shopName?: string
  shopLogo?: string
  shopDesc?: string
  themeColor?: string
  bannerIds?: string
  templateId?: number
  mainCategory?: string
  officialQrCode?: string
  mallQrCode?: string
  wechatLink?: string
  qualifications?: string
  contacts?: string

  shopEnabled?: number
  openTime?: string
  closeTime?: string
  /** ALLOW = 允许游客进店 */
  allowGuest?: string
  /** SHOW = 游客可见价格 */
  guestShowPrice?: string
  buyerHideLevel?: number

  displayListFields?: string
  displayDetailFields?: string
  showSales?: number
  stockDisplay?: string
  outOfStockDisplay?: string
  quantityScale?: number
  enableSplitUnit?: number
  priceTrack?: number
  priceTrackWithUnit?: number
  enableRetailPrice?: number

  minOrderAmount?: number
  freeShippingAmount?: number
  freightAmount?: number
  enableLogistics?: number
  logisticsMethods?: string
  selfDelivery?: number
  enablePickup?: number
  pickupAddresses?: string
  paymentMethods?: string
  paymentScenes?: string
  defaultWarehouseId?: number
  autoReceiveEnabled?: number
  autoReceiveDays?: number

  enableRegister?: number
  enableJoinApply?: number
  regAuditRequired?: number
  enableAutoAudit?: number
  regGiveCoupon?: number
  registerAgreement?: string

  categoryStyle?: string
  categoryDisplayMode?: string
  categoryDefaultSort?: string
  categoryShowCount?: number
  enableMallCategory?: number
  returnConsignee?: string
  returnPhone?: string
  returnAddress?: string
}

export interface TagItem {
  tagCode: string
  tagName: string
  sortOrder?: number
}

export interface NoticeItem {
  id: string | number
  title: string
  content?: string
  noticeType?: number
  publishTime?: string
  publisher?: string
}

/**
 * 价格三态（《商城App设计方案》§七）。
 * - `GUEST_HIDDEN` 游客 + 店铺不允许游客看价 → 显示「登录可见价」+ 底部去登录条
 * - `PENDING_AUTH` 已登录但未通过本店审核 → 显示「认证后可见价」+ 去认证
 * - `READY`        可以看到价格（游客可见价 or 已认证买家的等级价）
 */
export type PriceMode = 'GUEST_HIDDEN' | 'PENDING_AUTH' | 'READY'

export const useShopStore = defineStore('shop', () => {
  const userStore = useUserStore()

  const config = ref<ShopConfig | null>(null)
  const tags = ref<TagItem[]>([])
  const notices = ref<NoticeItem[]>([])
  const loading = ref(false)
  /** 店铺配置是否已尝试加载过（避免页面重复请求，也便于区分"未加载"与"加载失败"） */
  const loaded = ref(false)

  /**
   * 加载店铺配置。失败时**不抛**（页面仍应能渲染），只记 warning —— 首页若因
   * 配置拿不到就白屏，比"用默认样式渲染"更糟。
   */
  const loadConfig = async () => {
    if (loading.value) return
    loading.value = true
    try {
      const res: any = await api.shop.getConfig()
      config.value = res?.data ?? res ?? null
      // 主题色落到 CSS 变量，供全局使用（theme_color 字段由此真正生效）
      const theme = config.value?.themeColor
      if (theme) {
        document.documentElement.style.setProperty('--mall-primary', theme)
      }
    } catch (err: any) {
      // 常见两类：400「无法确定店铺」（缺 X-Tenant-Id）、403「未开放游客访问」
      console.warn('[店铺] 配置加载失败', err?.response?.data?.message || err?.message)
      config.value = null
    } finally {
      loaded.value = true
      loading.value = false
    }
  }

  const loadTags = async () => {
    try {
      const res: any = await api.tag.getList()
      tags.value = res?.data ?? (Array.isArray(res) ? res : [])
    } catch (err: any) {
      console.warn('[店铺] 标签加载失败', err?.response?.data?.message || err?.message)
      tags.value = []
    }
  }

  const loadNotices = async () => {
    try {
      const res: any = await api.notice.getList(10)
      notices.value = res?.data ?? (Array.isArray(res) ? res : [])
    } catch (err: any) {
      console.warn('[店铺] 公告加载失败', err?.response?.data?.message || err?.message)
      notices.value = []
    }
  }

  /** 首屏一次拉齐：配置 + 标签 + 公告（三者都是游客可达的只读接口） */
  const init = async () => {
    await Promise.all([loadConfig(), loadTags(), loadNotices()])
  }

  // ── 价格三态 ──
  const priceMode = computed<PriceMode>(() => {
    if (!userStore.isLoggedIn) {
      // 游客：店铺允许游客看价才给价，否则一律隐藏
      return config.value?.guestShowPrice === 'SHOW' ? 'READY' : 'GUEST_HIDDEN'
    }
    // 已登录：本店审核未通过（0 待审 / 2 驳回 / 3 解除 / 无关联行）→ 待认证
    const audit = (userStore.user as any)?.auditStatus
    const enabled = (userStore.user as any)?.shopEnabled
    if (enabled === 0) return 'PENDING_AUTH'
    if (audit === undefined || audit === null) return 'PENDING_AUTH'
    return audit === 1 ? 'READY' : 'PENDING_AUTH'
  })

  /** 价格位占位文案（null 表示可显示真实价格） */
  const pricePlaceholder = computed<string | null>(() => {
    switch (priceMode.value) {
      case 'GUEST_HIDDEN':
        return '登录可见价'
      case 'PENDING_AUTH':
        return '认证后可见价'
      default:
        return null
    }
  })

  /** 底部固定提示条文案（null = 不显示） */
  const priceHint = computed<string | null>(() => {
    switch (priceMode.value) {
      case 'GUEST_HIDDEN':
        return '登录认证后可查看商品价格'
      case 'PENDING_AUTH':
        return '您的入店申请审核中，通过后即可查看价格并下单'
      default:
        return null
    }
  })

  /** 当前是否处于营业时间（shopEnabled + openTime/closeTime；未配置一律视为营业） */
  const isOpen = computed(() => {
    const c = config.value
    if (!c) return true
    if (c.shopEnabled === 0) return false
    if (!c.openTime || !c.closeTime) return true
    const now = new Date()
    const cur = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}`
    // 常规区间；跨天（如 22:00~02:00）按"或"处理
    if (c.openTime <= c.closeTime) return cur >= c.openTime && cur <= c.closeTime
    return cur >= c.openTime || cur <= c.closeTime
  })

  /** tagCode → tagName（商品卡角标用） */
  const tagNameOf = (code: string): string => {
    return tags.value.find(t => t.tagCode === code)?.tagName || code
  }

  return {
    config, tags, notices, loading, loaded,
    loadConfig, loadTags, loadNotices, init,
    priceMode, pricePlaceholder, priceHint, isOpen, tagNameOf
  }
})
