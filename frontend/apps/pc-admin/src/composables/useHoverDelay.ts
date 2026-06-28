import { ref } from 'vue'
import type { MenuInfo } from '@/api/menu'

/**
 * 悬停延迟开关 composable
 * 用于侧边栏一级菜单 hover → 弹出 MegaMenuPanel 的延迟控制
 */
export interface TriggerPosition {
  top: number
  left: number
  height: number
  width: number
}

export function useHoverDelay(openDelay = 150, closeDelay = 300) {
  const isOpen = ref(false)
  const hoveredItem = ref<MenuInfo | null>(null)
  const triggerPos = ref<TriggerPosition | null>(null)

  let openTimer: ReturnType<typeof setTimeout> | null = null
  let closeTimer: ReturnType<typeof setTimeout> | null = null

  function clearTimers() {
    if (openTimer) { clearTimeout(openTimer); openTimer = null }
    if (closeTimer) { clearTimeout(closeTimer); closeTimer = null }
  }

  function handleTargetEnter(item: MenuInfo, el?: HTMLElement, openDelayOverride?: number) {
    clearTimers()
    hoveredItem.value = item
    if (el) {
      const rect = el.getBoundingClientRect()
      triggerPos.value = {
        top: rect.top,
        left: rect.right,
        height: rect.height,
        width: rect.width,
      }
    }
    openTimer = setTimeout(() => {
      isOpen.value = true
    }, openDelayOverride ?? openDelay)
  }

  function handleTargetLeave() {
    clearTimers()
    closeTimer = setTimeout(() => {
      isOpen.value = false
      hoveredItem.value = null
      triggerPos.value = null
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
      triggerPos.value = null
    }, closeDelay)
  }

  function close() {
    clearTimers()
    isOpen.value = false
    hoveredItem.value = null
    triggerPos.value = null
  }

  return {
    isOpen,
    hoveredItem,
    triggerPos,
    handleTargetEnter,
    handleTargetLeave,
    handlePanelEnter,
    handlePanelLeave,
    close
  }
}
