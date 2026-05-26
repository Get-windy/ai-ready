/**
 * 倒计时组合式函数
 * @package @ai-ready/mobile-ui
 * @composable useCountDown
 */

import { ref, computed, onUnmounted } from 'vue'

/**
 * 倒计时选项
 */
export interface ICountDownOptions {
  /** 总时长（秒） */
  total: number
  /** 是否自动开始 */
  autoStart?: boolean
  /** 间隔时间（毫秒） */
  interval?: number
  /** 倒计时结束回调 */
  onFinish?: () => void
  /** 倒计时变化回调 */
  onChange?: (remaining: number) => void
}

/**
 * useCountDown 返回值
 */
export interface IUseCountDownReturn {
  /** 剩余秒数 */
  remaining: ReturnType<typeof ref<number>>
  /** 是否运行中 */
  isRunning: ReturnType<typeof ref<boolean>>
  /** 是否已结束 */
  isFinished: ReturnType<typeof ref<boolean>>
  /** 格式化后的时间 */
  formatted: ReturnType<typeof ref<string>>
  /** 开始倒计时 */
  start: () => void
  /** 暂停倒计时 */
  pause: () => void
  /** 重置倒计时 */
  reset: () => void
}

/**
 * 格式化时间
 * @param seconds - 秒数
 * @returns 格式化字符串
 */
const formatTime = (seconds: number): string => {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60

  if (hours > 0) {
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }
  return `${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

/**
 * 倒计时组合式函数
 * @param options - 配置选项
 * @returns 倒计时控制方法
 * @example
 * ```vue
 * <template>
 *   <div>
 *     <span>{{ countdown.formatted }}</span>
 *     <button @click="countdown.start()">开始</button>
 *     <button @click="countdown.pause()">暂停</button>
 *   </div>
 * </template>
 *
 * <script setup>
 * const countdown = useCountDown({ total: 60, autoStart: true })
 * </script>
 * ```
 */
export const useCountDown = (options: ICountDownOptions): IUseCountDownReturn => {
  const { total, autoStart = false, interval = 1000, onFinish, onChange } = options

  const remaining = ref(total)
  const isRunning = ref(false)
  const isFinished = ref(false)
  const formatted = computed(() => formatTime(remaining.value))

  let timer: ReturnType<typeof setInterval> | null = null

  /**
   * 开始倒计时
   */
  const start = (): void => {
    if (isRunning.value) return
    if (remaining.value <= 0) return

    isRunning.value = true

    timer = setInterval(() => {
      remaining.value--
      onChange?.(remaining.value)

      if (remaining.value <= 0) {
        clearInterval(timer!)
        timer = null
        isRunning.value = false
        isFinished.value = true
        onFinish?.()
      }
    }, interval)
  }

  /**
   * 暂停倒计时
   */
  const pause = (): void => {
    if (!isRunning.value) return

    if (timer) {
      clearInterval(timer)
      timer = null
    }
    isRunning.value = false
  }

  /**
   * 重置倒计时
   */
  const reset = (): void => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    remaining.value = total
    isRunning.value = false
    isFinished.value = false
  }

  // 自动开始
  if (autoStart) {
    start()
  }

  // 组件卸载时清理
  onUnmounted(() => {
    if (timer) {
      clearInterval(timer)
    }
  })

  return {
    remaining,
    isRunning,
    isFinished,
    formatted,
    start,
    pause,
    reset,
  }
}

/**
 * 短信验证码倒计时
 * @param options - 配置选项
 * @returns 倒计时控制方法
 * @example
 * ```vue
 * <template>
 *   <button @click="sendCode" :disabled="sms.isRunning.value">
 *     {{ sms.isRunning.value ? `${sms.formatted.value}后重发` : '发送验证码' }}
 *   </button>
 * </template>
 *
 * <script setup>
 * const sms = useSmsCountDown({ total: 60 })
 *
 * const sendCode = () => {
 *   // 发送验证码请求
 *   sms.start()
 * }
 * </script>
 * ```
 */
export const useSmsCountDown = (options: Omit<ICountDownOptions, 'autoStart'>) => {
  return useCountDown({
    ...options,
    autoStart: false,
  })
}

/**
 * 倒计时 Hook（简化版）
 * @param total - 总秒数
 * @returns 倒计时状态
 */
export const useSimpleCountDown = (total: number) => {
  return useCountDown({ total, autoStart: true })
}

export default useCountDown
