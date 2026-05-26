import { ref, watch, onMounted, onUnmounted, type Ref } from 'vue'
import { debounce, cloneDeep, isEqual } from 'lodash-es'

// 持久化配置类型
export type PersistenceOptions = {
  key: string
  storage?: 'localStorage' | 'sessionStorage' | 'memory'
  debounceTime?: number
  include?: string[]
  exclude?: string[]
  expire?: number // 过期时间（毫秒）
  encrypt?: boolean
  onRestore?: (data: any) => any
  onSave?: (data: any) => any
}

// 存储数据包装类型
type StorageWrapper = {
  data: any
  timestamp: number
  expire?: number
}

/**
 * 创建表单数据持久化
 */
export function useFormPersistence<T extends Record<string, any>>(
  formData: Ref<T>,
  options: PersistenceOptions
) {
  const opts = {
    storage: 'localStorage',
    debounceTime: 500,
    expire: undefined,
    encrypt: false,
    ...options
  }

  const isDirty = ref(false)
  const lastSaved = ref<Date | null>(null)
  const isRestored = ref(false)

  // 获取存储实例
  const getStorage = () => {
    switch (opts.storage) {
      case 'localStorage':
        return window.localStorage
      case 'sessionStorage':
        return window.sessionStorage
      case 'memory':
        // 内存存储
        return createMemoryStorage()
      default:
        return window.localStorage
    }
  }

  // 内存存储实现
  const memoryStorageMap = new Map<string, string>()
  const createMemoryStorage = () => ({
    getItem: (key: string) => memoryStorageMap.get(key) || null,
    setItem: (key: string, value: string) => memoryStorageMap.set(key, value),
    removeItem: (key: string) => memoryStorageMap.delete(key),
    clear: () => memoryStorageMap.clear()
  })

  const storage = getStorage()

  // 过滤字段
  const filterData = (data: T): Partial<T> => {
    let filtered: Partial<T> = {}
    
    if (opts.include && opts.include.length > 0) {
      for (const field of opts.include) {
        if (data.hasOwnProperty(field)) {
          filtered[field] = data[field]
        }
      }
    } else if (opts.exclude && opts.exclude.length > 0) {
      for (const key of Object.keys(data)) {
        if (!opts.exclude.includes(key)) {
          filtered[key] = data[key]
        }
      }
    } else {
      filtered = { ...data }
    }
    
    return filtered
  }

  // 简单加密/解密（可扩展为更复杂的加密方案）
  const encryptData = (data: string): string => {
    if (!opts.encrypt) return data
    // Base64 编码作为简单加密
    return btoa(encodeURIComponent(data))
  }

  const decryptData = (data: string): string => {
    if (!opts.encrypt) return data
    // Base64 解码
    return decodeURIComponent(atob(data))
  }

  // 保存数据
  const save = (data: T) => {
    try {
      const filteredData = filterData(data)
      const processedData = opts.onSave ? opts.onSave(filteredData) : filteredData
      
      const wrapper: StorageWrapper = {
        data: processedData,
        timestamp: Date.now(),
        expire: opts.expire
      }
      
      const serialized = JSON.stringify(wrapper)
      const encrypted = encryptData(serialized)
      
      storage.setItem(opts.key, encrypted)
      lastSaved.value = new Date()
      isDirty.value = false
      
      return true
    } catch (error) {
      console.error('[FormPersistence] Save failed:', error)
      return false
    }
  }

  // 恢复数据
  const restore = (): Partial<T> | null => {
    try {
      const encrypted = storage.getItem(opts.key)
      if (!encrypted) return null
      
      const decrypted = decryptData(encrypted)
      const wrapper: StorageWrapper = JSON.parse(decrypted)
      
      // 检查过期
      if (wrapper.expire && Date.now() - wrapper.timestamp > wrapper.expire) {
        clear()
        return null
      }
      
      const processedData = opts.onRestore ? opts.onRestore(wrapper.data) : wrapper.data
      isRestored.value = true
      
      return processedData as Partial<T>
    } catch (error) {
      console.error('[FormPersistence] Restore failed:', error)
      return null
    }
  }

  // 清除存储
  const clear = () => {
    try {
      storage.removeItem(opts.key)
      isDirty.value = false
      lastSaved.value = null
      isRestored.value = false
      return true
    } catch (error) {
      console.error('[FormPersistence] Clear failed:', error)
      return false
    }
  }

  // 防抖保存
  const debouncedSave = debounce(save, opts.debounceTime)

  // 监听数据变化自动保存
  const stopWatch = watch(formData, (newData) => {
    if (isRestored.value) {
      isDirty.value = true
      debouncedSave(newData)
    }
  }, { deep: true })

  // 初始化时尝试恢复数据
  onMounted(() => {
    const restoredData = restore()
    if (restoredData) {
      Object.assign(formData.value, restoredData)
    }
    // 标记为可保存状态
    isRestored.value = true
  })

  // 组件卸载时停止监听
  onUnmounted(() => {
    stopWatch()
    // 最后一次保存
    if (isDirty.value) {
      debouncedSave.flush()
    }
  })

  // 手动保存
  const manualSave = () => {
    return save(formData.value)
  }

  // 手动恢复
  const manualRestore = () => {
    const restoredData = restore()
    if (restoredData) {
      Object.assign(formData.value, restoredData)
      return true
    }
    return false
  }

  // 重置（清除存储并恢复初始值）
  const reset = (initialData?: Partial<T>) => {
    clear()
    if (initialData) {
      Object.assign(formData.value, initialData)
    }
    isDirty.value = false
  }

  // 获取存储信息
  const getStorageInfo = () => {
    const encrypted = storage.getItem(opts.key)
    if (!encrypted) return null
    
    try {
      const decrypted = decryptData(encrypted)
      const wrapper: StorageWrapper = JSON.parse(decrypted)
      
      return {
        timestamp: new Date(wrapper.timestamp),
        expire: wrapper.expire ? new Date(wrapper.timestamp + wrapper.expire) : null,
        isExpired: wrapper.expire ? Date.now() - wrapper.timestamp > wrapper.expire : false
      }
    } catch {
      return null
    }
  }

  return {
    isDirty,
    lastSaved,
    isRestored,
    save: manualSave,
    restore: manualRestore,
    clear,
    reset,
    getStorageInfo,
    // 底层方法
    _save: save,
    _restore: restore
  }
}

/**
 * 创建多表单持久化管理器
 */
export function createFormPersistenceManager() {
  const forms = new Map<string, {
    formData: Ref<any>
    options: PersistenceOptions
    persistence: ReturnType<typeof useFormPersistence>
  }>()

  // 注册表单
  const register = <T extends Record<string, any>>(
    formId: string,
    formData: Ref<T>,
    options: PersistenceOptions
  ) => {
    const persistence = useFormPersistence(formData, {
      ...options,
      key: options.key || formId
    })
    
    forms.set(formId, {
      formData,
      options,
      persistence
    })
    
    return persistence
  }

  // 取消注册
  const unregister = (formId: string) => {
    const form = forms.get(formId)
    if (form) {
      form.persistence.clear()
      forms.delete(formId)
    }
  }

  // 批量保存
  const saveAll = () => {
    for (const [id, form] of forms) {
      form.persistence.save()
    }
  }

  // 批量恢复
  const restoreAll = () => {
    for (const [id, form] of forms) {
      form.persistence.restore()
    }
  }

  // 批量清除
  const clearAll = () => {
    for (const [id, form] of forms) {
      form.persistence.clear()
    }
  }

  // 获取所有表单状态
  const getAllStatus = () => {
    const status: Record<string, {
      isDirty: boolean
      lastSaved: Date | null
    }> = {}
    
    for (const [id, form] of forms) {
      status[id] = {
        isDirty: form.persistence.isDirty.value,
        lastSaved: form.persistence.lastSaved.value
      }
    }
    
    return status
  }

  return {
    register,
    unregister,
    saveAll,
    restoreAll,
    clearAll,
    getAllStatus
  }
}

/**
 * 表单草稿箱功能
 */
export function useFormDraft<T extends Record<string, any>>(
  formData: Ref<T>,
  draftKey: string,
  options?: {
    maxDrafts?: number
    expireDays?: number
  }
) {
  const opts = {
    maxDrafts: 10,
    expireDays: 7,
    ...options
  }

  const drafts = ref<Array<{
    id: string
    data: Partial<T>
    timestamp: number
    title?: string
  }>>([])

  // 获取所有草稿
  const loadDrafts = () => {
    try {
      const stored = localStorage.getItem(`drafts_${draftKey}`)
      if (stored) {
        const parsed = JSON.parse(stored)
        // 过滤过期草稿
        const now = Date.now()
        const expireMs = opts.expireDays * 24 * 60 * 60 * 1000
        
        drafts.value = parsed.filter((d: any) => 
          now - d.timestamp < expireMs
        )
        
        // 清理超出数量的草稿
        if (drafts.value.length > opts.maxDrafts) {
          drafts.value = drafts.value.slice(-opts.maxDrafts)
        }
        
        // 更新存储
        saveDraftsToStorage()
      }
    } catch (error) {
      console.error('[FormDraft] Load drafts failed:', error)
    }
  }

  // 保存草稿到存储
  const saveDraftsToStorage = () => {
    localStorage.setItem(`drafts_${draftKey}`, JSON.stringify(drafts.value))
  }

  // 创建新草稿
  const createDraft = (title?: string) => {
    const draft = {
      id: `draft_${Date.now()}`,
      data: cloneDeep(formData.value),
      timestamp: Date.now(),
      title: title || `草稿 ${drafts.value.length + 1}`
    }
    
    drafts.value.push(draft)
    
    // 保持最大数量限制
    if (drafts.value.length > opts.maxDrafts) {
      drafts.value.shift()
    }
    
    saveDraftsToStorage()
    return draft.id
  }

  // 应用草稿
  const applyDraft = (draftId: string) => {
    const draft = drafts.value.find(d => d.id === draftId)
    if (draft) {
      Object.assign(formData.value, draft.data)
      return true
    }
    return false
  }

  // 删除草稿
  const deleteDraft = (draftId: string) => {
    const index = drafts.value.findIndex(d => d.id === draftId)
    if (index !== -1) {
      drafts.value.splice(index, 1)
      saveDraftsToStorage()
      return true
    }
    return false
  }

  // 清空所有草稿
  const clearDrafts = () => {
    drafts.value = []
    localStorage.removeItem(`drafts_${draftKey}`)
  }

  // 比较草稿与当前数据是否相同
  const isDraftCurrent = (draftId: string) => {
    const draft = drafts.value.find(d => d.id === draftId)
    if (!draft) return false
    return isEqual(draft.data, formData.value)
  }

  // 初始化加载草稿
  loadDrafts()

  return {
    drafts,
    createDraft,
    applyDraft,
    deleteDraft,
    clearDrafts,
    isDraftCurrent,
    loadDrafts
  }
}

export default {
  useFormPersistence,
  createFormPersistenceManager,
  useFormDraft
}