import { ref } from 'vue'
import type { MenuInfo } from '@/api/menu'

/**
 * 悬停延迟开关 composable
 * 用于侧边栏一级菜单 hover → 弹出 MegaMenuPanel 的延迟控制
 */
export function useHoverDelay(openDelay = 150, closeDelay = 300) {
  const isOpen = ref(false)
  const hoveredItem = ref<MenuInfo | null>(null)

  let openTimer: ReturnType<typeof setTimeout> | null = null
  let closeTimer: ReturnType<typeof setTimeout> | null = null

  function clearTimers() {
    if (openTimer) { clearTimeout(openTimer); openTimer = null }
    if (closeTimer) { clearTimeout(closeTimer); closeTimer = null }
  }

  function handleTargetEnter(item: MenuInfo) {
    clearTimers()
    hoveredItem.value = item
    openTimer = setTimeout(() => {
      isOpen.value = true
    }, openDelay)
  }

  function handleTargetLeave() {
    clearTimers()
    closeTimer = setTimeout(() => {
      isOpen.value = false
      hoveredItem.value = null
    }, closeDelay)
  }

  function handlePanelEnter() {
    clearTimers()
    isOpen.value = true
  }

  function handlePanelLeave() {
    clearTimers()
    closeTimer = setTimeout(() => {
      isOpen.value = false
      hoveredItem.value = null
    }, closeDelay)
  }

  function close() {
    clearTimers()
    isOpen.value = false
    hoveredItem.value = null
  }

  return {
    isOpen,
    hoveredItem,
    handleTargetEnter,
    handleTargetLeave,
    handlePanelEnter,
    handlePanelLeave,
    close
  }
}
