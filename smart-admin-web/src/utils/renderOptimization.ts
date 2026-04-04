import { watch, nextTick, type Ref } from 'vue'

/**
 * 虚拟列表 Hook
 * 用于大数据列表的性能优化
 */
export function useVirtualList<T>(
  list: Ref<T[]>,
  options: {
    itemHeight: number
    containerHeight: number
    buffer?: number
  }
) {
  const { itemHeight, containerHeight, buffer = 5 } = options
  
  // 可见区域起始索引
  const startIndex = ref(0)
  // 可见区域结束索引
  const endIndex = ref(0)
  // 偏移量
  const offset = ref(0)
  
  // 计算可见列表
  const visibleList = computed(() => {
    return list.value.slice(startIndex.value, endIndex.value + 1)
  })
  
  // 总高度
  const totalHeight = computed(() => list.value.length * itemHeight)
  
  // 计算可见范围
  const calculateRange = (scrollTop: number) => {
    const start = Math.floor(scrollTop / itemHeight)
    const visibleCount = Math.ceil(containerHeight / itemHeight)
    const end = start + visibleCount + buffer
    
    startIndex.value = Math.max(0, start - buffer)
    endIndex.value = Math.min(list.value.length - 1, end)
    offset.value = startIndex.value * itemHeight
  }
  
  // 滚动处理
  const handleScroll = (e: Event) => {
    const target = e.target as HTMLElement
    calculateRange(target.scrollTop)
  }
  
  // 初始化
  onMounted(() => {
    calculateRange(0)
  })
  
  return {
    startIndex,
    endIndex,
    offset,
    visibleList,
    totalHeight,
    handleScroll
  }
}

/**
 * 防抖 Hook
 */
export function useDebounce<T extends (...args: any[]) => any>(
  fn: T,
  delay: number
) {
  let timer: ReturnType<typeof setTimeout> | null = null
  
  const debounced = (...args: Parameters<T>) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }
  
  const cancel = () => {
    if (timer) clearTimeout(timer)
    timer = null
  }
  
  onUnmounted(cancel)
  
  return { debounced, cancel }
}

/**
 * 节流 Hook
 */
export function useThrottle<T extends (...args: any[]) => any>(
  fn: T,
  delay: number
) {
  let lastTime = 0
  let timer: ReturnType<typeof setTimeout> | null = null
  
  const throttled = (...args: Parameters<T>) => {
    const now = Date.now()
    const remaining = delay - (now - lastTime)
    
    if (remaining <= 0) {
      lastTime = now
      fn(...args)
    } else {
      if (timer) clearTimeout(timer)
      timer = setTimeout(() => {
        lastTime = Date.now()
        fn(...args)
      }, remaining)
    }
  }
  
  const cancel = () => {
    if (timer) clearTimeout(timer)
    timer = null
  }
  
  onUnmounted(cancel)
  
  return { throttled, cancel }
}

/**
 * 请求动画帧 Hook
 */
export function useRAF() {
  let rafId: number | null = null
  
  const requestFrame = (callback: FrameRequestCallback) => {
    rafId = requestAnimationFrame(callback)
    return rafId
  }
  
  const cancelFrame = () => {
    if (rafId !== null) {
      cancelAnimationFrame(rafId)
      rafId = null
    }
  }
  
  onUnmounted(cancelFrame)
  
  return { requestFrame, cancelFrame }
}

/**
 * 批量更新 Hook
 */
export function useBatchUpdate() {
  const queue: Array<() => void> = []
  let pending = false
  
  const flush = () => {
    const callbacks = queue.slice()
    queue.length = 0
    pending = false
    
    callbacks.forEach(cb => cb())
  }
  
  const enqueue = (callback: () => void) => {
    queue.push(callback)
    
    if (!pending) {
      pending = true
      nextTick(flush)
    }
  }
  
  return { enqueue, flush }
}

/**
 * 省略导入
 */
import { ref, computed, onMounted, onUnmounted } from 'vue'

export default {
  useVirtualList,
  useDebounce,
  useThrottle,
  useRAF,
  useBatchUpdate
}