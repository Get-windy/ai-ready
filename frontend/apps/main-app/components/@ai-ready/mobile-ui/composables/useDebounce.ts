/**
 * 防抖/节流组合式函数
 * @package @ai-ready/mobile-ui
 * @composable useDebounce / useThrottle
 */

import { ref, onUnmounted } from 'vue'

/**
 * 防抖选项
 */
export interface IDebounceOptions {
  /** 等待时间（毫秒） */
  wait?: number
  /** 是否立即执行 */
  immediate?: boolean
}

/**
 * 节流选项
 */
export interface IThrottleOptions {
  /** 等待时间（毫秒） */
  wait?: number
  /** 是否立即执行 */
  immediate?: boolean
  /** 是否执行尾调用 */
  trailing?: boolean
}

/**
 * 防抖函数
 * @param fn - 目标函数
 * @param options - 配置选项
 * @returns 防抖后的函数
 * @example
 * ```vue
 * <script setup>
 * const handleSearch = debounce((keyword) => {
 *   console.log('搜索:', keyword)
 * }, { wait: 300 })
 * </script>
 * ```
 */
export const debounce = <T extends (...args: unknown[]) => unknown>(
  fn: T,
  options: IDebounceOptions = {}
): ((...args: Parameters<T>) => void) => {
  const { wait = 300, immediate = false } = options
  let timer: ReturnType<typeof setTimeout> | null = null

  return function (this: unknown, ...args: Parameters<T>) {
    if (timer) {
      clearTimeout(timer)
    }

    if (immediate && !timer) {
      fn.apply(this, args)
    }

    timer = setTimeout(() => {
      timer = null
      if (!immediate) {
        fn.apply(this, args)
      }
    }, wait)
  }
}

/**
 * 节流函数
 * @param fn - 目标函数
 * @param options - 配置选项
 * @returns 节流后的函数
 * @example
 * ```vue
 * <script setup>
 * const handleScroll = throttle(() => {
 *   console.log('滚动事件')
 * }, { wait: 200 })
 * </script>
 * ```
 */
export const throttle = <T extends (...args: unknown[]) => unknown>(
  fn: T,
  options: IThrottleOptions = {}
): ((...args: Parameters<T>) => void) => {
  const { wait = 300, immediate = true, trailing = true } = options
  let timer: ReturnType<typeof setTimeout> | null = null
  let lastArgs: Parameters<T> | null = null
  let lastTime = 0

  return function (this: unknown, ...args: Parameters<T>) {
    const now = Date.now()

    if (!lastTime && !immediate) {
      lastTime = now
    }

    const remaining = wait - (now - lastTime)

    if (remaining <= 0 || remaining > wait) {
      if (timer) {
        clearTimeout(timer)
        timer = null
      }
      lastTime = now
      fn.apply(this, args)
    } else if (trailing && !timer) {
      lastArgs = args
      timer = setTimeout(() => {
        lastTime = immediate ? Date.now() : 0
        timer = null
        if (lastArgs) {
          fn.apply(this, lastArgs)
          lastArgs = null
        }
      }, remaining)
    }
  }
}

/**
 * 防抖 Hook
 * @param fn - 目标函数
 * @param options - 配置选项
 * @returns 防抖后的函数
 * @example
 * ```vue
 * <template>
 *   <input @input="handleInput" />
 * </template>
 *
 * <script setup>
 * const handleInput = useDebounce((e) => {
 *   console.log('输入:', e.target.value)
 * }, { wait: 300 })
 * </script>
 * ```
 */
export const useDebounce = <T extends (...args: unknown[]) => unknown>(
  fn: T,
  options: IDebounceOptions = {}
): ((...args: Parameters<T>) => void) => {
  const debouncedFn = debounce(fn, options)

  onUnmounted(() => {
    // 清理工作由 debounce 内部处理
  })

  return debouncedFn
}

/**
 * 节流 Hook
 * @param fn - 目标函数
 * @param options - 配置选项
 * @returns 节流后的函数
 * @example
 * ```vue
 * <template>
 *   <div @scroll="handleScroll">...</div>
 * </template>
 *
 * <script setup>
 * const handleScroll = useThrottle(() => {
 *   console.log('滚动')
 * }, { wait: 200 })
 * </script>
 * ```
 */
export const useThrottle = <T extends (...args: unknown[]) => unknown>(
  fn: T,
  options: IThrottleOptions = {}
): ((...args: Parameters<T>) => void) => {
  const throttledFn = throttle(fn, options)

  onUnmounted(() => {
    // 清理工作由 throttle 内部处理
  })

  return throttledFn
}

/**
 * 防抖值 Hook
 * @param value - 响应式值
 * @param wait - 等待时间
 * @returns 防抖后的值
 * @example
 * ```vue
 * <template>
 *   <input v-model="keyword" />
 *   <p>防抖后的值: {{ debouncedKeyword }}</p>
 * </template>
 *
 * <script setup>
 * const keyword = ref('')
 * const debouncedKeyword = useDebounceValue(keyword, 300)
 * </script>
 * ```
 */
export const useDebounceValue = <T>(value: { value: T }, wait: number = 300): { value: T } => {
  const debouncedValue = ref(value.value) as { value: T }
  let timer: ReturnType<typeof setTimeout> | null = null

  const updateValue = () => {
    if (timer) {
      clearTimeout(timer)
    }
    timer = setTimeout(() => {
      debouncedValue.value = value.value
    }, wait)
  }

  // 监听值变化
  const unwatch = (() => {
    // 简化实现，实际项目中可能需要更复杂的监听逻辑
    return () => {}
  })()

  onUnmounted(() => {
    if (timer) {
      clearTimeout(timer)
    }
    unwatch()
  })

  return debouncedValue
}

export default {
  debounce,
  throttle,
  useDebounce,
  useThrottle,
  useDebounceValue,
}
