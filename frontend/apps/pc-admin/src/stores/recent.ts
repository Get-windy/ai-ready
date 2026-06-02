/**
 * 最近浏览 & 收藏功能状态管理
 * 记录用户最近访问的业务单据和功能页面
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface RecentItem {
  /** 唯一标识 */
  id: string
  /** 标题 */
  title: string
  /** 路由路径 */
  path: string
  /** 类型: 单据类型或功能模块 */
  type: 'order' | 'customer' | 'supplier' | 'product' | 'finance' | 'page'
  /** 业务单号（如果是单据） */
  bizNo?: string
  /** 浏览时间戳 */
  timestamp: number
}

export interface FavoriteItem {
  /** 唯一标识 */
  id: string
  /** 标题 */
  title: string
  /** 路由路径 */
  path: string
  /** 图标 */
  icon?: string
  /** 排序 */
  sortOrder: number
}

const MAX_RECENT_ITEMS = 20
const STORAGE_KEY_FAVORITES = 'user-favorites'

export const useRecentStore = defineStore('recent', () => {
  const recentItems = ref<RecentItem[]>([])
  const favoriteItems = ref<FavoriteItem[]>([])

  const recentList = computed(() => {
    return [...recentItems.value].sort((a, b) => b.timestamp - a.timestamp)
  })

  const favoriteList = computed(() => {
    return [...favoriteItems.value].sort((a, b) => a.sortOrder - b.sortOrder)
  })

  /** 添加最近浏览记录 */
  function addRecent(item: Omit<RecentItem, 'timestamp'>) {
    const idx = recentItems.value.findIndex(r => r.id === item.id)
    if (idx !== -1) {
      recentItems.value[idx].timestamp = Date.now()
      // 移到最前
      const [existing] = recentItems.value.splice(idx, 1)
      recentItems.value.unshift(existing)
      return
    }
    recentItems.value.unshift({ ...item, timestamp: Date.now() })
    if (recentItems.value.length > MAX_RECENT_ITEMS) {
      recentItems.value = recentItems.value.slice(0, MAX_RECENT_ITEMS)
    }
  }

  /** 清除最近浏览记录 */
  function clearRecent() {
    recentItems.value = []
  }

  /** 初始化收藏 */
  function initFavorites() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY_FAVORITES)
      if (raw) favoriteItems.value = JSON.parse(raw)
    } catch { /* ignore */ }
  }

  /** 保存收藏到 localStorage */
  function persistFavorites() {
    try {
      localStorage.setItem(STORAGE_KEY_FAVORITES, JSON.stringify(favoriteItems.value))
    } catch { /* ignore */ }
  }

  /** 添加到收藏 */
  function addFavorite(item: Omit<FavoriteItem, 'sortOrder'>) {
    if (favoriteItems.value.some(f => f.id === item.id)) return
    favoriteItems.value.push({
      ...item,
      sortOrder: favoriteItems.value.length
    })
    persistFavorites()
  }

  /** 从收藏移除 */
  function removeFavorite(id: string) {
    favoriteItems.value = favoriteItems.value.filter(f => f.id !== id)
    persistFavorites()
  }

  /** 判断是否已收藏 */
  function isFavorite(id: string): boolean {
    return favoriteItems.value.some(f => f.id === id)
  }

  /** 切换收藏状态 */
  function toggleFavorite(item: Omit<FavoriteItem, 'sortOrder'>) {
    if (isFavorite(item.id)) {
      removeFavorite(item.id)
      return false
    }
    addFavorite(item)
    return true
  }

  /** 更新收藏排序 */
  function reorderFavorites(items: FavoriteItem[]) {
    favoriteItems.value = items.map((item, idx) => ({ ...item, sortOrder: idx }))
    persistFavorites()
  }

  return {
    recentItems,
    recentList,
    favoriteItems,
    favoriteList,
    addRecent,
    clearRecent,
    initFavorites,
    addFavorite,
    removeFavorite,
    isFavorite,
    toggleFavorite,
    reorderFavorites
  }
})
