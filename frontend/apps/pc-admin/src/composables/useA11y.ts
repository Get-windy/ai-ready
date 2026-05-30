/**
 * WCAG 2.1 AA 可访问性 Composable
 */
import { ref, onMounted, onBeforeUnmount, type Ref } from 'vue'

/** 焦点陷阱 — 在模态框内循环 Tab 焦点 */
export function useFocusTrap(containerRef: Ref<HTMLElement | null>, enabled: Ref<boolean> = ref(true)) {
  let previousActiveElement: Element | null = null

  function getFocusableElements(): HTMLElement[] {
    if (!containerRef.value) return []
    const selectors = [
      'a[href]', 'button:not([disabled])', 'textarea:not([disabled])',
      'input:not([disabled]):not([type="hidden"])', 'select:not([disabled])',
      '[tabindex]:not([tabindex="-1"])'
    ]
    return Array.from(
      containerRef.value.querySelectorAll<HTMLElement>(selectors.join(','))
    ).filter(el => el.offsetParent !== null)
  }

  function handleKeyDown(e: KeyboardEvent) {
    if (e.key !== 'Tab' || !enabled.value || !containerRef.value) return

    const focusable = getFocusableElements()
    if (focusable.length === 0) return

    const first = focusable[0]
    const last = focusable[focusable.length - 1]

    if (e.shiftKey) {
      if (document.activeElement === first) {
        e.preventDefault()
        last.focus()
      }
    } else {
      if (document.activeElement === last) {
        e.preventDefault()
        first.focus()
      }
    }
  }

  function activate() {
    previousActiveElement = document.activeElement
    document.addEventListener('keydown', handleKeyDown)
    // 自动聚焦第一个可聚焦元素
    const focusable = getFocusableElements()
    if (focusable.length > 0) focusable[0].focus()
  }

  function deactivate() {
    document.removeEventListener('keydown', handleKeyDown)
    // 恢复之前的焦点
    if (previousActiveElement && previousActiveElement instanceof HTMLElement) {
      previousActiveElement.focus()
    }
  }

  return { activate, deactivate, getFocusableElements }
}

/** ARIA Live 区域公告 — 供屏幕阅读器使用 */
let announcerElement: HTMLElement | null = null

function ensureAnnouncer(): HTMLElement {
  if (!announcerElement) {
    announcerElement = document.createElement('div')
    announcerElement.setAttribute('aria-live', 'polite')
    announcerElement.setAttribute('aria-atomic', 'true')
    announcerElement.className = 'sr-only'
    document.body.appendChild(announcerElement)
  }
  return announcerElement
}

export function useAnnouncer() {
  function announce(message: string, priority: 'polite' | 'assertive' = 'polite') {
    const el = ensureAnnouncer()
    el.setAttribute('aria-live', priority)

    // 清空后重新设置内容以触发屏幕阅读器朗读
    el.textContent = ''
    requestAnimationFrame(() => {
      el.textContent = message
    })
  }

  return { announce }
}

/** 键盘事件处理映射 */
export interface A11yKeyboardHandlers {
  onEscape?: () => void
  onEnter?: () => void
  onArrowUp?: () => void
  onArrowDown?: () => void
  onArrowLeft?: () => void
  onArrowRight?: () => void
  onTab?: (shift: boolean) => void
}

export function useA11yKeyboard(handlers: A11yKeyboardHandlers) {
  function handleKeyDown(e: KeyboardEvent) {
    switch (e.key) {
      case 'Escape':
        handlers.onEscape?.()
        break
      case 'Enter':
        handlers.onEnter?.()
        break
      case 'ArrowUp':
        e.preventDefault()
        handlers.onArrowUp?.()
        break
      case 'ArrowDown':
        e.preventDefault()
        handlers.onArrowDown?.()
        break
      case 'ArrowLeft':
        handlers.onArrowLeft?.()
        break
      case 'ArrowRight':
        handlers.onArrowRight?.()
        break
      case 'Tab':
        handlers.onTab?.(e.shiftKey)
        break
    }
  }

  return { handleKeyDown }
}
