/**
 * v-feature-flag 指令
 * 根据 Feature Flag 状态控制 DOM 元素的渲染
 *
 * 用法: <a-button v-feature-flag="'EXPORT_FEATURE'">导出</a-button>
 *        <div v-feature-flag:remove="'NEW_DASHBOARD'">新版</div>
 *        <div v-feature-flag:disable="'BATCH_OPERATIONS'">批量</div>
 */
import type { Directive, DirectiveBinding } from 'vue'
import { getFeatureFlagService } from '@/utils/featureFlags'

type FlagArg = 'remove' | 'hide' | 'disable'

const directive: Directive<HTMLElement, string> = {
  mounted(el: HTMLElement, binding: DirectiveBinding<string>) {
    const flagKey = binding.value
    if (!flagKey) return

    const service = getFeatureFlagService()
    const context = service.getContext()
    const enabled = service.isEnabled(flagKey, context)
    const mode = (binding.arg as FlagArg) || 'remove'

    applyFlag(el, enabled, mode, binding)
  },

  updated(el: HTMLElement, binding: DirectiveBinding<string>) {
    const flagKey = binding.value
    if (!flagKey) return

    const service = getFeatureFlagService()
    const context = service.getContext()
    const enabled = service.isEnabled(flagKey, context)
    const mode = (binding.arg as FlagArg) || 'remove'

    applyFlag(el, enabled, mode, binding)
  },
}

function applyFlag(
  el: HTMLElement,
  enabled: boolean,
  mode: FlagArg,
  binding: DirectiveBinding
): void {
  switch (mode) {
    case 'remove':
      // 完全从 DOM 中移除
      if (!enabled) {
        el.style.display = 'none'
        // 存储原始 display 以便恢复
        ;(el as any).__ffOriginalDisplay = el.style.display
      } else {
        const original = (el as any).__ffOriginalDisplay
        el.style.display = original || ''
      }
      break

    case 'hide':
      // 隐藏但保留占位
      if (!enabled) {
        el.style.visibility = 'hidden'
        el.style.pointerEvents = 'none'
      } else {
        el.style.visibility = ''
        el.style.pointerEvents = ''
      }
      break

    case 'disable':
      // 禁用交互
      if (!enabled) {
        el.setAttribute('disabled', 'true')
        el.style.opacity = '0.5'
        el.style.cursor = 'not-allowed'
        el.title = '此功能尚未开放'
      } else {
        el.removeAttribute('disabled')
        el.style.opacity = ''
        el.style.cursor = ''
        el.title = ''
      }
      break
  }
}

export default directive
