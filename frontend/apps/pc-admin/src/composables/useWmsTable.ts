/**
 * WMS 列表页通用 Composable
 *
 * 集成 useCrud、自动刷新、防抖点击、快捷键等功能，
 * 消除各页面重复的 debounceClick/onMounted/onUnmounted/handleKeydown 模板代码。
 *
 * 用法:
 * ```ts
 * const wms = useWmsTable({
 *   fetchFn: warehouseApi.page,
 *   deleteFn: warehouseApi.remove,
 *   defaultSearch: { warehouseCode: undefined },
 *   pageSize: 20,
 *   refreshInterval: 30,
 *   shortcuts: { f5: 'refresh', ctrlN: 'add' },
 * })
 * ```
 */
import { ref, onMounted, onUnmounted, type Ref } from 'vue'
import { message } from 'ant-design-vue'
import { useCrud, type CrudOptions, type CrudState } from './useCrud'

// ── 类型 ──────────────────────────────────────────────

export interface WmsTableOptions<T, Q extends Record<string, any>> extends CrudOptions<T, Q> {
  /** 自动刷新间隔（秒），设为 0 禁用，默认 30 */
  refreshInterval?: number
  /** 防抖延迟（毫秒），默认 300 */
  debounceDelay?: number
  /** 快捷键映射 */
  shortcuts?: {
    /** F5 刷新的回调函数名，默认 'refresh' */
    f5?: string
    /** Ctrl+N 新增的回调函数名，默认 'add' */
    ctrlN?: string
  }
}

export interface WmsTableState<T> extends CrudState<T> {
  /** 上次更新时间 */
  lastUpdateTime: Ref<string>
  /** 自动刷新倒计时 */
  autoRefreshCountdown: Ref<number>
  /** 防抖包装函数 */
  debounce: (key: string, fn: () => void) => void
}

// ── 实现 ──────────────────────────────────────────────

export function useWmsTable<T extends { id?: number; [key: string]: any }, Q extends Record<string, any> = Record<string, any>>(
  options: WmsTableOptions<T, Q>
): WmsTableState<T> {
  const {
    refreshInterval = 30,
    debounceDelay = 300,
    shortcuts = { f5: 'refresh', ctrlN: 'add' },
    immediate = true,
  } = options

  // 基础 CRUD
  const crud = useCrud<T, Q>({ ...options, immediate: false })

  // 自动刷新
  const lastUpdateTime = ref('')
  const autoRefreshCountdown = ref(0)

  let countdownTimer: ReturnType<typeof setInterval> | null = null
  let refreshTimer: ReturnType<typeof setInterval> | null = null

  // 防抖 Map
  const debounceMap = new Map<string, number>()

  function debounce(key: string, fn: () => void) {
    const now = Date.now()
    const last = debounceMap.get(key) || 0
    if (now - last < debounceDelay) return
    debounceMap.set(key, now)
    fn()
  }

  // 快捷键
  function handleKeydown(e: KeyboardEvent) {
    const target = e.target
    const isInput = target instanceof HTMLInputElement || target instanceof HTMLTextAreaElement

    if (e.key === 'F5' && !isInput && shortcuts.f5) {
      e.preventDefault()
      debounce(shortcuts.f5, () => crud.fetchData())
    }
    if ((e.ctrlKey || e.metaKey) && e.key === 'n' && !isInput && shortcuts.ctrlN) {
      e.preventDefault()
      crud.handleAdd()
    }
  }

  // 包装 fetchData 以更新时间戳
  const originalFetchData = crud.fetchData
  const wrappedFetchData = async () => {
    await originalFetchData()
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    if (refreshInterval > 0) {
      autoRefreshCountdown.value = refreshInterval
    }
  }

  onMounted(() => {
    if (immediate) wrappedFetchData()

    if (refreshInterval > 0) {
      autoRefreshCountdown.value = refreshInterval
      refreshTimer = setInterval(() => {
        wrappedFetchData()
        autoRefreshCountdown.value = refreshInterval
      }, refreshInterval * 1000)
      countdownTimer = setInterval(() => {
        if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
      }, 1000)
    }

    document.addEventListener('keydown', handleKeydown)
  })

  onUnmounted(() => {
    if (refreshTimer) clearInterval(refreshTimer)
    if (countdownTimer) clearInterval(countdownTimer)
    document.removeEventListener('keydown', handleKeydown)
  })

  return {
    ...crud,
    lastUpdateTime,
    autoRefreshCountdown,
    debounce,
  }
}
