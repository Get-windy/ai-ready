/**
 * 全局键盘快捷键指令
 * 提供 v-shortcut 指令用于绑定快捷键
 */
import type { Directive, DirectiveBinding } from 'vue'

interface ShortcutBinding {
  /** 快捷键组合, 如 'ctrl+s', 'alt+n', 'escape' */
  keys: string
  /** 触发回调 */
  handler: (event: KeyboardEvent) => void
  /** 是否精确匹配（默认true，要求修饰键必须完全匹配） */
  exact?: boolean
}

const keyAlias: Record<string, string> = {
  'esc': 'escape',
  'enter': 'enter',
  'del': 'delete',
  'space': ' ',
  'ins': 'insert',
}

function parseKeys(keys: string) {
  const parts = keys.toLowerCase().split('+')
  const result = {
    ctrl: false,
    alt: false,
    shift: false,
    meta: false,
    key: ''
  }
  for (const part of parts) {
    const normalized = keyAlias[part] || part
    switch (normalized) {
      case 'ctrl': case 'control': result.ctrl = true; break
      case 'alt': result.alt = true; break
      case 'shift': result.shift = true; break
      case 'meta': case 'cmd': case 'command': result.meta = true; break
      default: result.key = normalized; break
    }
  }
  return result
}

function matchKey(event: KeyboardEvent, binding: ShortcutBinding): boolean {
  const target = parseKeys(binding.keys)
  const key = event.key.toLowerCase()

  const ctrlMatch = event.ctrlKey || event.metaKey // 跨平台兼容

  if (target.key && key !== target.key) return false
  if (target.shift && !event.shiftKey) return false
  if (target.alt && !event.altKey) return false
  if (target.ctrl && !ctrlMatch) return false
  if (target.meta && !event.metaKey) return false

  if (binding.exact !== false) {
    // 精确匹配：没有额外修饰键
    const hasExtraCtrl = ctrlMatch && !target.ctrl
    const hasExtraShift = event.shiftKey && !target.shift
    const hasExtraAlt = event.altKey && !target.alt
    const hasExtraMeta = event.metaKey && !target.meta && !event.ctrlKey
    if (hasExtraCtrl || hasExtraShift || hasExtraAlt || hasExtraMeta) return false
  }

  return true
}

const shortcutDirective: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding<ShortcutBinding | ShortcutBinding[]>) {
    const bindings = Array.isArray(binding.value) ? binding.value : [binding.value]

    const handler = (event: KeyboardEvent) => {
      // 不在输入框中触发的排除
      const target = event.target as HTMLElement
      const isInput = target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable

      for (const b of bindings) {
        if (matchKey(event, b)) {
          // 对全局快捷键（如Ctrl+S），允许在输入框中触发
          if (isInput && !b.keys.toLowerCase().includes('escape')) {
            // 非 Escape 快捷键在输入框中触发时阻止默认行为
            if (b.keys.toLowerCase().includes('ctrl+s') || b.keys.toLowerCase().includes('cmd+s')) {
              event.preventDefault()
            }
          }
          b.handler(event)
          return
        }
      }
    }

    el._shortcutHandler = handler
    document.addEventListener('keydown', handler)
  },
  unmounted(el: HTMLElement) {
    if (el._shortcutHandler) {
      document.removeEventListener('keydown', el._shortcutHandler)
      delete el._shortcutHandler
    }
  }
}

export default shortcutDirective
