import { ref, onMounted, onUnmounted } from 'vue'

export interface ShortcutConfig {
  key: string
  ctrl?: boolean
  alt?: boolean
  shift?: boolean
  meta?: boolean
  description: string
  action: () => void
}

export function useKeyboardShortcuts() {
  const shortcuts = ref<ShortcutConfig[]>([])
  const isListening = ref(false)

  const registerShortcut = (config: ShortcutConfig) => {
    const exists = shortcuts.value.find(
      s => s.key === config.key && 
           s.ctrl === config.ctrl && 
           s.alt === config.alt && 
           s.shift === config.shift &&
           s.meta === config.meta
    )
    if (!exists) {
      shortcuts.value.push(config)
    }
  }

  const unregisterShortcut = (key: string, modifiers?: { ctrl?: boolean; alt?: boolean; shift?: boolean; meta?: boolean }) => {
    shortcuts.value = shortcuts.value.filter(
      s => !(s.key === key && 
             s.ctrl === modifiers?.ctrl && 
             s.alt === modifiers?.alt && 
             s.shift === modifiers?.shift &&
             s.meta === modifiers?.meta)
    )
  }

  const handleKeyDown = (event: KeyboardEvent) => {
    // Check for Ctrl+K search shortcut
    if (event.ctrlKey && event.key.toLowerCase() === 'k') {
      event.preventDefault()
      const searchShortcut = shortcuts.value.find(
        s => s.key === 'k' && s.ctrl && !s.alt && !s.shift && !s.meta
      )
      if (searchShortcut) {
        searchShortcut.action()
      }
      return
    }

    // Check for R refresh shortcut
    if (event.key.toLowerCase() === 'r' && !event.ctrlKey && !event.altKey && !event.metaKey) {
      // Only trigger if not in input/textarea
      const target = event.target as HTMLElement
      if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable) {
        return
      }
      event.preventDefault()
      const refreshShortcut = shortcuts.value.find(
        s => s.key.toLowerCase() === 'r' && !s.ctrl && !s.alt && !s.shift && !s.meta
      )
      if (refreshShortcut) {
        refreshShortcut.action()
      }
      return
    }

    // Check for Escape close shortcut
    if (event.key === 'Escape') {
      const escapeShortcut = shortcuts.value.find(
        s => s.key === 'Escape' && !s.ctrl && !s.alt && !s.shift && !s.meta
      )
      if (escapeShortcut) {
        escapeShortcut.action()
      }
      return
    }

    // Check other registered shortcuts
    const matched = shortcuts.value.find(s => {
      const keyMatch = s.key.toLowerCase() === event.key.toLowerCase()
      const ctrlMatch = s.ctrl === event.ctrlKey
      const altMatch = s.alt === event.altKey
      const shiftMatch = s.shift === event.shiftKey
      const metaMatch = s.meta === event.metaKey
      return keyMatch && ctrlMatch && altMatch && shiftMatch && metaMatch
    })

    if (matched) {
      event.preventDefault()
      matched.action()
    }
  }

  const startListening = () => {
    if (!isListening.value) {
      document.addEventListener('keydown', handleKeyDown)
      isListening.value = true
    }
  }

  const stopListening = () => {
    if (isListening.value) {
      document.removeEventListener('keydown', handleKeyDown)
      isListening.value = false
    }
  }

  onMounted(() => {
    startListening()
  })

  onUnmounted(() => {
    stopListening()
  })

  return {
    shortcuts,
    registerShortcut,
    unregisterShortcut,
    startListening,
    stopListening,
    isListening
  }
}

// Common shortcuts presets
export const createCommonShortcuts = (actions: {
  onRefresh?: () => void
  onSearch?: () => void
  onClose?: () => void
  onSave?: () => void
  onHelp?: () => void
}) => {
  const shortcuts: ShortcutConfig[] = []

  if (actions.onRefresh) {
    shortcuts.push({
      key: 'r',
      description: '刷新数据',
      action: actions.onRefresh
    })
  }

  if (actions.onSearch) {
    shortcuts.push({
      key: 'k',
      ctrl: true,
      description: '打开搜索 (Ctrl+K)',
      action: actions.onSearch
    })
  }

  if (actions.onClose) {
    shortcuts.push({
      key: 'Escape',
      description: '关闭弹窗/取消',
      action: actions.onClose
    })
  }

  if (actions.onSave) {
    shortcuts.push({
      key: 's',
      ctrl: true,
      description: '保存 (Ctrl+S)',
      action: actions.onSave
    })
  }

  if (actions.onHelp) {
    shortcuts.push({
      key: '?',
      shift: true,
      description: '显示快捷键帮助 (Shift+?)',
      action: actions.onHelp
    })
  }

  return shortcuts
}
