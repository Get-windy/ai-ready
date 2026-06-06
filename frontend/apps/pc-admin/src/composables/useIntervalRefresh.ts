import { ref, onUnmounted } from 'vue'

/**
 * 通用定时刷新 composable
 * @param fn 刷新回调函数
 * @param interval 刷新间隔（毫秒），默认 30000
 * @param immediate 是否立即执行
 */
export function useIntervalRefresh(
  fn: () => Promise<void> | void,
  interval = 30000,
  immediate = false
) {
  const timer = ref<ReturnType<typeof setInterval> | null>(null)

  function start() {
    stop()
    if (immediate) {
      fn()
    }
    timer.value = setInterval(() => {
      fn()
    }, interval)
  }

  function stop() {
    if (timer.value) {
      clearInterval(timer.value)
      timer.value = null
    }
  }

  function restart() {
    stop()
    start()
  }

  onUnmounted(() => {
    stop()
  })

  return { start, stop, restart }
}
