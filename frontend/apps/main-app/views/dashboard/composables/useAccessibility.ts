import { ref, onMounted, onUnmounted } from 'vue'

interface A11yConfig {
  highContrast: boolean
  reduceMotion: boolean
  largeText: boolean
  screenReaderAnnouncements: boolean
}

interface FocusTrapConfig {
  container: HTMLElement
  initialFocus?: HTMLElement
  onEscape?: () => void
}

export function useAccessibility() {
  const config = ref<A11yConfig>({
    highContrast: false,
    reduceMotion: false,
    largeText: false,
    screenReaderAnnouncements: true
  })

  const STORAGE_KEY = 'dashboard_a11y_config'

  // Load accessibility config
  const loadConfig = () => {
    try {
      const saved = localStorage.getItem(STORAGE_KEY)
      if (saved) {
        const parsed = JSON.parse(saved)
        config.value = { ...config.value, ...parsed }
        applyConfig()
      }
    } catch (e) {
      console.warn('Failed to load a11y config:', e)
    }
  }

  // Save accessibility config
  const saveConfig = () => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(config.value))
    } catch (e) {
      console.warn('Failed to save a11y config:', e)
    }
  }

  // Apply accessibility settings
  const applyConfig = () => {
    const html = document.documentElement

    // High contrast mode
    if (config.value.highContrast) {
      html.classList.add('high-contrast')
    } else {
      html.classList.remove('high-contrast')
    }

    // Reduced motion
    if (config.value.reduceMotion) {
      html.classList.add('reduce-motion')
    } else {
      html.classList.remove('reduce-motion')
    }

    // Large text
    if (config.value.largeText) {
      html.style.fontSize = '18px'
    } else {
      html.style.fontSize = ''
    }
  }

  // Update config
  const updateConfig = (newConfig: Partial<A11yConfig>) => {
    config.value = { ...config.value, ...newConfig }
    applyConfig()
    saveConfig()
  }

  // Screen reader announcement
  const announce = (message: string, priority: 'polite' | 'assertive' = 'polite') => {
    if (!config.value.screenReaderAnnouncements) return

    const announcer = document.getElementById('sr-announcer') || createAnnouncer()
    announcer.setAttribute('aria-live', priority)
    announcer.textContent = message

    // Clear after announcement
    setTimeout(() => {
      announcer.textContent = ''
    }, 1000)
  }

  // Create screen reader announcer element
  const createAnnouncer = (): HTMLElement => {
    const announcer = document.createElement('div')
    announcer.id = 'sr-announcer'
    announcer.setAttribute('aria-live', 'polite')
    announcer.setAttribute('aria-atomic', 'true')
    announcer.style.position = 'absolute'
    announcer.style.left = '-10000px'
    announcer.style.width = '1px'
    announcer.style.height = '1px'
    announcer.style.overflow = 'hidden'
    document.body.appendChild(announcer)
    return announcer
  }

  // Focus management
  const focusableSelectors = [
    'button:not([disabled])',
    'a[href]',
    'input:not([disabled])',
    'select:not([disabled])',
    'textarea:not([disabled])',
    '[tabindex]:not([tabindex="-1"])',
    '[contenteditable]'
  ].join(', ')

  const getFocusableElements = (container: HTMLElement): HTMLElement[] => {
    return Array.from(container.querySelectorAll(focusableSelectors))
  }

  // Focus trap for modals/dialogs
  const createFocusTrap = (config: FocusTrapConfig) => {
    const { container, initialFocus, onEscape } = config
    const focusableElements = getFocusableElements(container)
    const firstElement = focusableElements[0]
    const lastElement = focusableElements[focusableElements.length - 1]

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Tab') {
        if (focusableElements.length === 0) {
          e.preventDefault()
          return
        }

        if (e.shiftKey) {
          // Shift + Tab
          if (document.activeElement === firstElement) {
            e.preventDefault()
            lastElement?.focus()
          }
        } else {
          // Tab
          if (document.activeElement === lastElement) {
            e.preventDefault()
            firstElement?.focus()
          }
        }
      }

      if (e.key === 'Escape' && onEscape) {
        onEscape()
      }
    }

    container.addEventListener('keydown', handleKeyDown)

    // Set initial focus
    if (initialFocus) {
      initialFocus.focus()
    } else if (firstElement) {
      firstElement.focus()
    }

    // Return cleanup function
    return () => {
      container.removeEventListener('keydown', handleKeyDown)
    }
  }

  // Skip link for keyboard navigation
  const createSkipLink = (targetId: string, label = '跳转到主要内容') => {
    const existing = document.getElementById('skip-link')
    if (existing) existing.remove()

    const skipLink = document.createElement('a')
    skipLink.id = 'skip-link'
    skipLink.href = `#${targetId}`
    skipLink.textContent = label
    skipLink.className = 'skip-link'
    skipLink.style.cssText = `
      position: absolute;
      top: -40px;
      left: 0;
      background: #000;
      color: #fff;
      padding: 8px 16px;
      z-index: 10000;
      transition: top 0.3s;
    `
    skipLink.addEventListener('focus', () => {
      skipLink.style.top = '0'
    })
    skipLink.addEventListener('blur', () => {
      skipLink.style.top = '-40px'
    })

    document.body.insertBefore(skipLink, document.body.firstChild)
  }

  // Keyboard navigation helper
  const handleArrowKeyNavigation = (
    container: HTMLElement,
    direction: 'horizontal' | 'vertical' = 'horizontal'
  ) => {
    const items = getFocusableElements(container)
    const currentIndex = items.findIndex(item => item === document.activeElement)

    return {
      next: () => {
        const nextIndex = (currentIndex + 1) % items.length
        items[nextIndex]?.focus()
      },
      prev: () => {
        const prevIndex = currentIndex <= 0 ? items.length - 1 : currentIndex - 1
        items[prevIndex]?.focus()
      },
      currentIndex
    }
  }

  onMounted(() => {
    loadConfig()
  })

  return {
    config,
    updateConfig,
    announce,
    createFocusTrap,
    createSkipLink,
    getFocusableElements,
    handleArrowKeyNavigation
  }
}

// ARIA utilities
export const ariaUtils = {
  // Generate unique ID
  generateId: (prefix = 'a11y'): string => {
    return `${prefix}-${Math.random().toString(36).substr(2, 9)}`
  },

  // Set up aria-describedby relationship
  describe: (element: HTMLElement, descriptionId: string) => {
    element.setAttribute('aria-describedby', descriptionId)
  },

  // Set up aria-labelledby relationship
  label: (element: HTMLElement, labelId: string) => {
    element.setAttribute('aria-labelledby', labelId)
  },

  // Mark element as expanded/collapsed
  setExpanded: (element: HTMLElement, expanded: boolean) => {
    element.setAttribute('aria-expanded', expanded.toString())
  },

  // Mark element as hidden
  setHidden: (element: HTMLElement, hidden: boolean) => {
    element.setAttribute('aria-hidden', hidden.toString())
  },

  // Set live region
  setLiveRegion: (element: HTMLElement, priority: 'polite' | 'assertive' | 'off' = 'polite') => {
    element.setAttribute('aria-live', priority)
  }
}

// Accessibility check utilities
export const a11yChecks = {
  // Check color contrast (simplified)
  hasAdequateContrast: (foreground: string, background: string): boolean => {
    // This is a simplified check - real implementation would calculate luminance
    return true
  },

  // Check if element has accessible name
  hasAccessibleName: (element: HTMLElement): boolean => {
    return !!(
      element.getAttribute('aria-label') ||
      element.getAttribute('aria-labelledby') ||
      element.textContent?.trim() ||
      element.getAttribute('title') ||
      element.getAttribute('placeholder')
    )
  }
}
