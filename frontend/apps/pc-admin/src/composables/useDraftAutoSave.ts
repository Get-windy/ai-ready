/**
 * 表单草稿自动保存 Composable
 * 自动将表单数据保存到 localStorage，支持草稿恢复和过期清理
 */
import { ref, watch, onUnmounted, type Ref } from 'vue'
import { message } from 'ant-design-vue'

const DRAFT_PREFIX = 'form-draft:'
const DRAFT_EXPIRY = 2 * 60 * 60 * 1000 // 2小时过期
const AUTOSAVE_DEBOUNCE = 2000 // 2秒防抖

interface DraftData {
  data: Record<string, any>
  savedAt: number
  path: string
  title?: string
}

export function useDraftAutoSave(options: {
  /** 草稿唯一标识 */
  draftKey: string
  /** 表单数据（响应式对象） */
  formData: Ref<Record<string, any>> | Record<string, any>
  /** 当前路由路径 */
  currentPath: string
  /** 草稿标题 */
  title?: string
  /** 是否启用 */
  enabled?: Ref<boolean> | boolean
}) {
  const { draftKey, formData, currentPath, title, enabled } = options
  const isEnabled = ref(typeof enabled === 'boolean' ? enabled : (enabled?.value ?? true))

  let saveTimer: ReturnType<typeof setTimeout> | null = null
  let lastSaveData: string = ''

  /** 获取存储键 */
  function getStorageKey(): string {
    return DRAFT_PREFIX + draftKey
  }

  /** 保存草稿 */
  function saveDraft() {
    if (!isEnabled.value) return
    const rawData = JSON.parse(JSON.stringify(formData instanceof Function ? formData.value : formData))
    const dataStr = JSON.stringify(rawData)
    // 跳过无变化保存
    if (dataStr === lastSaveData) return

    const draft: DraftData = {
      data: rawData,
      savedAt: Date.now(),
      path: currentPath,
      title
    }
    try {
      localStorage.setItem(getStorageKey(), JSON.stringify(draft))
      lastSaveData = dataStr
    } catch {
      // localStorage 满时清理旧草稿
      cleanupExpiredDrafts()
    }
  }

  /** 防抖保存 */
  function debouncedSave() {
    if (saveTimer) clearTimeout(saveTimer)
    saveTimer = setTimeout(saveDraft, AUTOSAVE_DEBOUNCE)
  }

  /** 恢复草稿 */
  function restoreDraft(): DraftData | null {
    try {
      const raw = localStorage.getItem(getStorageKey())
      if (!raw) return null
      const draft: DraftData = JSON.parse(raw)
      // 检查过期
      if (Date.now() - draft.savedAt > DRAFT_EXPIRY) {
        localStorage.removeItem(getStorageKey())
        return null
      }
      return draft
    } catch {
      return null
    }
  }

  /** 检查是否有可用草稿 */
  function hasDraft(): boolean {
    const draft = restoreDraft()
    return draft !== null
  }

  /** 应用草稿 */
  function applyDraft(): boolean {
    const draft = restoreDraft()
    if (!draft) return false
    const target = formData instanceof Function ? formData.value : formData
    Object.assign(target, draft.data)
    message.info('已恢复上次编辑的草稿')
    return true
  }

  /** 清除草稿 */
  function clearDraft() {
    try {
      localStorage.removeItem(getStorageKey())
    } catch { /* ignore */ }
    lastSaveData = ''
  }

  /** 清理过期草稿 */
  function cleanupExpiredDrafts() {
    try {
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i)
        if (key?.startsWith(DRAFT_PREFIX)) {
          try {
            const raw = localStorage.getItem(key)
            if (raw) {
              const draft = JSON.parse(raw)
              if (Date.now() - draft.savedAt > DRAFT_EXPIRY) {
                localStorage.removeItem(key)
              }
            }
          } catch {
            localStorage.removeItem(key)
          }
        }
      }
    } catch { /* ignore */ }
  }

  /** 表单未保存离开提示 */
  function setupBeforeUnload() {
    const handler = (e: BeforeUnloadEvent) => {
      const rawData = JSON.parse(JSON.stringify(formData instanceof Function ? formData.value : formData))
      if (JSON.stringify(rawData) !== lastSaveData) {
        e.preventDefault()
        e.returnValue = ''
      }
    }
    window.addEventListener('beforeunload', handler)
    return () => window.removeEventListener('beforeunload', handler)
  }

  // 监听表单数据变化自动保存
  const stopWatch = watch(
    () => formData instanceof Function ? formData.value : formData,
    () => debouncedSave(),
    { deep: true }
  )

  const cleanup = setupBeforeUnload()

  onUnmounted(() => {
    if (saveTimer) clearTimeout(saveTimer)
    stopWatch()
    cleanup()
  })

  return {
    saveDraft,
    hasDraft,
    restoreDraft,
    applyDraft,
    clearDraft,
    setupBeforeUnload
  }
}

/** 清除当前用户的所有草稿 */
export function clearAllDrafts() {
  try {
    for (let i = localStorage.length - 1; i >= 0; i--) {
      const key = localStorage.key(i)
      if (key?.startsWith(DRAFT_PREFIX)) {
        localStorage.removeItem(key)
      }
    }
  } catch { /* ignore */ }
}
