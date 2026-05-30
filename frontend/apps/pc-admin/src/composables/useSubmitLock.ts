/**
 * 提交锁定 Composable
 * 防止重复提交（双击、快速多次点击）
 */
import { ref } from 'vue'

export interface SubmitLockOptions {
  /** 冷却时间（毫秒），默认 500ms */
  cooldownMs?: number
  /** 提交前验证函数 */
  beforeSubmit?: () => boolean | Promise<boolean>
  /** 提交失败回调 */
  onError?: (error: any) => void
}

export function useSubmitLock(options: SubmitLockOptions = {}) {
  const { cooldownMs = 500, beforeSubmit, onError } = options

  const isSubmitting = ref(false)
  let lastSubmitTime = 0

  async function withSubmitLock<T>(fn: () => Promise<T>): Promise<T | undefined> {
    const now = Date.now()
    if (now - lastSubmitTime < cooldownMs) {
      console.warn('[SubmitLock] 操作过于频繁，已忽略')
      return undefined
    }

    if (isSubmitting.value) {
      console.warn('[SubmitLock] 正在提交中，已忽略重复操作')
      return undefined
    }

    if (beforeSubmit) {
      try {
        const valid = await beforeSubmit()
        if (!valid) return undefined
      } catch (err) {
        onError?.(err)
        return undefined
      }
    }

    isSubmitting.value = true
    lastSubmitTime = now

    try {
      return await fn()
    } catch (error) {
      onError?.(error)
      throw error
    } finally {
      isSubmitting.value = false
    }
  }

  return {
    isSubmitting,
    withSubmitLock,
    reset: () => {
      isSubmitting.value = false
      lastSubmitTime = 0
    }
  }
}
