/**
 * 多标签页状态管理 (Tab Navigation)
 * 类似 Odoo/浏览器的多标签页管理
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface TabItem {
  /** 路由路径作为唯一标识 */
  path: string
  /** 显示标题 */
  title: string
  /** 图标 */
  icon?: string
  /** 是否固定（不可关闭） */
  pinned?: boolean
  /** 路由名称（用于 keep-alive） */
  routeName?: string
  /** 是否正在加载 */
  loading?: boolean
  /** 是否包含未保存内容 */
  dirty?: boolean
}

export const useTabsStore = defineStore('tabs', () => {
  const tabs = ref<TabItem[]>([])
  const activeTabPath = ref('')

  const activeTab = computed(() => {
    return tabs.value.find(t => t.path === activeTabPath.value)
  })

  const tabCount = computed(() => tabs.value.length)

  const DEFAULT_TABS: TabItem[] = [
    {
      path: '/dashboard',
      title: '工作台',
      icon: 'DashboardOutlined',
      pinned: true,
      routeName: 'Dashboard'
    }
  ]

  /** 初始化或重置标签页 */
  function initTabs() {
    tabs.value = [...DEFAULT_TABS]
    activeTabPath.value = '/dashboard'
  }

  /** 打开或切换到某个标签页 */
  function openTab(tab: TabItem) {
    const existing = tabs.value.find(t => t.path === tab.path)
    if (existing) {
      activeTabPath.value = tab.path
      return existing
    }
    tabs.value.push(tab)
    activeTabPath.value = tab.path
    return tab
  }

  /** 关闭标签页，返回下一个应该激活的路径 */
  function closeTab(path: string): string {
    const idx = tabs.value.findIndex(t => t.path === path)
    if (idx === -1) return activeTabPath.value
    const tab = tabs.value[idx]
    if (tab.pinned) return activeTabPath.value

    tabs.value.splice(idx, 1)

    // 如果关闭的是当前激活的标签，切换到相邻的
    if (path === activeTabPath.value) {
      const newIdx = Math.min(idx, tabs.value.length - 1)
      activeTabPath.value = tabs.value[newIdx]?.path || '/dashboard'
    }
    return activeTabPath.value
  }

  /** 关闭除固定标签外的所有标签 */
  function closeOtherTabs(path: string) {
    tabs.value = tabs.value.filter(t => t.path === path || t.pinned)
    activeTabPath.value = path
  }

  /** 关闭所有标签（保留固定的） */
  function closeAllTabs() {
    tabs.value = tabs.value.filter(t => t.pinned)
    activeTabPath.value = tabs.value[0]?.path || '/dashboard'
  }

  /** 关闭右侧所有标签 */
  function closeRightTabs(path: string) {
    const idx = tabs.value.findIndex(t => t.path === path)
    if (idx === -1) return
    tabs.value = [
      ...tabs.value.slice(0, idx + 1).filter(t => idx === tabs.value.indexOf(t) || t.pinned),
      ...tabs.value.filter(t => t.pinned && tabs.value.indexOf(t) > idx)
    ]
    activeTabPath.value = path
  }

  /** 更新标签标题 */
  function updateTabTitle(path: string, title: string) {
    const tab = tabs.value.find(t => t.path === path)
    if (tab) tab.title = title
  }

  /** 设置标签 dirty 状态 */
  function setTabDirty(path: string, dirty: boolean) {
    const tab = tabs.value.find(t => t.path === path)
    if (tab) tab.dirty = dirty
  }

  return {
    tabs,
    activeTabPath,
    activeTab,
    tabCount,
    initTabs,
    openTab,
    closeTab,
    closeOtherTabs,
    closeAllTabs,
    closeRightTabs,
    updateTabTitle,
    setTabDirty
  }
})
