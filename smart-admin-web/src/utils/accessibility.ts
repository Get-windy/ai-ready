/**
 * 无障碍访问工具
 * 实现ARIA标签、键盘导航、屏幕阅读器兼容、高对比度模式
 */

import { ref, onMounted, onUnmounted, type Ref } from 'vue'

// 焦点管理
export class FocusManager {
  private focusableElements = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
  private focusStack: HTMLElement[][] = []
  
  /**
   * 获取所有可聚焦元素
   */
  getFocusableElements(container: HTMLElement = document.body): HTMLElement[] {
    return Array.from(container.querySelectorAll(this.focusableElements))
      .filter(el => {
        const htmlEl = el as HTMLElement
        return !htmlEl.hasAttribute('disabled') && 
               !htmlEl.hasAttribute('hidden') &&
               htmlEl.offsetParent !== null
      }) as HTMLElement[]
  }
  
  /**
   * 聚焦第一个可聚焦元素
   */
  focusFirst(container: HTMLElement = document.body): void {
    const elements = this.getFocusableElements(container)
    if (elements.length > 0) {
      elements[0].focus()
    }
  }
  
  /**
   * 聚焦最后一个可聚焦元素
   */
  focusLast(container: HTMLElement = document.body): void {
    const elements = this.getFocusableElements(container)
    if (elements.length > 0) {
      elements[elements.length - 1].focus()
    }
  }
  
  /**
   * 保存当前焦点
   */
  saveFocus(): void {
    const activeElement = document.activeElement as HTMLElement
    if (activeElement) {
      this.focusStack.push([activeElement])
    }
  }
  
  /**
   * 恢复焦点
   */
  restoreFocus(): void {
    const lastFocus = this.focusStack.pop()
    if (lastFocus && lastFocus[0]) {
      lastFocus[0].focus()
    }
  }
  
  /**
   * 陷阱焦点（用于模态框）
   */
  trapFocus(container: HTMLElement): () => void {
    const elements = this.getFocusableElements(container)
    const firstFocusable = elements[0]
    const lastFocusable = elements[elements.length - 1]
    
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key !== 'Tab') return
      
      if (e.shiftKey) {
        if (document.activeElement === firstFocusable) {
          lastFocusable?.focus()
          e.preventDefault()
        }
      } else {
        if (document.activeElement === lastFocusable) {
          firstFocusable?.focus()
          e.preventDefault()
        }
      }
    }
    
    container.addEventListener('keydown', handleKeyDown)
    firstFocusable?.focus()
    
    return () => {
      container.removeEventListener('keydown', handleKeyDown)
    }
  }
}

// 全局焦点管理器
export const focusManager = new FocusManager()

/**
 * 键盘导航 Hook
 */
export function useKeyboardNavigation(
  container: Ref<HTMLElement | null>,
  options?: {
    onEnter?: () => void
    onEscape?: () => void
    onArrowUp?: () => void
    onArrowDown?: () => void
    onArrowLeft?: () => void
    onArrowRight?: () => void
  }
) {
  const handleKeyDown = (e: KeyboardEvent) => {
    switch (e.key) {
      case 'Enter':
        options?.onEnter?.()
        break
      case 'Escape':
        options?.onEscape?.()
        break
      case 'ArrowUp':
        e.preventDefault()
        options?.onArrowUp?.()
        break
      case 'ArrowDown':
        e.preventDefault()
        options?.onArrowDown?.()
        break
      case 'ArrowLeft':
        e.preventDefault()
        options?.onArrowLeft?.()
        break
      case 'ArrowRight':
        e.preventDefault()
        options?.onArrowRight?.()
        break
    }
  }
  
  onMounted(() => {
    container.value?.addEventListener('keydown', handleKeyDown)
  })
  
  onUnmounted(() => {
    container.value?.removeEventListener('keydown', handleKeyDown)
  })
}

/**
 * 高对比度模式 Hook
 */
export function useHighContrast() {
  const isHighContrast = ref(false)
  
  const checkHighContrast = () => {
    // 检查系统高对比度设置
    const mediaQuery = window.matchMedia('(prefers-contrast: high)')
    isHighContrast.value = mediaQuery.matches
    
    // 检查用户自定义设置
    const stored = localStorage.getItem('highContrast')
    if (stored === 'true') {
      isHighContrast.value = true
    }
    
    // 应用高对比度类
    if (isHighContrast.value) {
      document.documentElement.classList.add('high-contrast')
    } else {
      document.documentElement.classList.remove('high-contrast')
    }
  }
  
  const toggleHighContrast = () => {
    isHighContrast.value = !isHighContrast.value
    localStorage.setItem('highContrast', String(isHighContrast.value))
    
    if (isHighContrast.value) {
      document.documentElement.classList.add('high-contrast')
    } else {
      document.documentElement.classList.remove('high-contrast')
    }
  }
  
  onMounted(() => {
    checkHighContrast()
    
    // 监听系统设置变化
    const mediaQuery = window.matchMedia('(prefers-contrast: high)')
    mediaQuery.addEventListener('change', checkHighContrast)
  })
  
  return {
    isHighContrast,
    toggleHighContrast
  }
}

/**
 * 跳过导航链接
 */
export function useSkipLink(targetId: string) {
  const skipToContent = () => {
    const target = document.getElementById(targetId)
    if (target) {
      target.setAttribute('tabindex', '-1')
      target.focus()
      target.removeAttribute('tabindex')
    }
  }
  
  return {
    skipToContent
  }
}

/**
 * 屏幕阅读器公告
 */
export function useAnnounce() {
  let announcer: HTMLElement | null = null
  
  const createAnnouncer = () => {
    if (announcer) return
    
    announcer = document.createElement('div')
    announcer.setAttribute('role', 'status')
    announcer.setAttribute('aria-live', 'polite')
    announcer.setAttribute('aria-atomic', 'true')
    announcer.className = 'sr-only'
    document.body.appendChild(announcer)
  }
  
  const announce = (message: string, priority: 'polite' | 'assertive' = 'polite') => {
    createAnnouncer()
    
    if (announcer) {
      announcer.setAttribute('aria-live', priority === 'assertive' ? 'assertive' : 'polite')
      announcer.textContent = ''
      
      // 使用 setTimeout 确保屏幕阅读器能捕捉到变化
      setTimeout(() => {
        if (announcer) {
          announcer.textContent = message
        }
      }, 100)
    }
  }
  
  onUnmounted(() => {
    if (announcer) {
      document.body.removeChild(announcer)
      announcer = null
    }
  })
  
  return {
    announce
  }
}

/**
 * 键盘快捷键
 */
export function useKeyboardShortcuts(
  shortcuts: Array<{
    key: string
    ctrl?: boolean
    alt?: boolean
    shift?: boolean
    handler: () => void
  }>
) {
  const handleKeyDown = (e: KeyboardEvent) => {
    for (const shortcut of shortcuts) {
      const ctrlMatch = shortcut.ctrl ? (e.ctrlKey || e.metaKey) : true
      const altMatch = shortcut.alt ? e.altKey : !e.altKey
      const shiftMatch = shortcut.shift ? e.shiftKey : !e.shiftKey
      const keyMatch = e.key.toLowerCase() === shortcut.key.toLowerCase()
      
      if (ctrlMatch && altMatch && shiftMatch && keyMatch) {
        e.preventDefault()
        shortcut.handler()
        break
      }
    }
  }
  
  onMounted(() => {
    document.addEventListener('keydown', handleKeyDown)
  })
  
  onUnmounted(() => {
    document.removeEventListener('keydown', handleKeyDown)
  })
}

/**
 * 减少动画模式
 */
export function useReducedMotion() {
  const prefersReducedMotion = ref(false)
  
  const checkReducedMotion = () => {
    const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
    prefersReducedMotion.value = mediaQuery.matches
    
    if (prefersReducedMotion.value) {
      document.documentElement.classList.add('reduced-motion')
    } else {
      document.documentElement.classList.remove('reduced-motion')
    }
  }
  
  onMounted(() => {
    checkReducedMotion()
    
    const mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
    mediaQuery.addEventListener('change', checkReducedMotion)
  })
  
  return {
    prefersReducedMotion
  }
}

export default {
  FocusManager,
  focusManager,
  useKeyboardNavigation,
  useHighContrast,
  useSkipLink,
  useAnnounce,
  useKeyboardShortcuts,
  useReducedMotion
}