/**
 * 触摸手势组合式函数
 * @package @ai-ready/mobile-ui
 * @composable useTouch
 */

import { ref, reactive, type Ref } from 'vue'

/**
 * 触摸事件状态接口
 */
export interface ITouchState {
  /** 起始X坐标 */
  startX: number
  /** 起始Y坐标 */
  startY: number
  /** X方向移动距离 */
  deltaX: number
  /** Y方向移动距离 */
  deltaY: number
  /** X方向偏移量 */
  offsetX: number
  /** Y方向偏移量 */
  offsetY: number
  /** 滑动方向 */
  direction: 'horizontal' | 'vertical' | ''
  /** 是否正在触摸 */
  touching: boolean
}

/**
 * useTouch 配置选项
 */
export interface IUseTouchOptions {
  /** 触发阈值 */
  threshold?: number
  /** 是否阻止默认行为 */
  preventDefault?: boolean
  /** 是否阻止冒泡 */
  stopPropagation?: boolean
}

/**
 * useTouch 返回值接口
 */
export interface IUseTouchReturn {
  /** 触摸状态 */
  state: ITouchState
  /** 触摸开始 */
  start: (event: TouchEvent) => void
  /** 触摸移动 */
  move: (event: TouchEvent) => void
  /** 触摸结束 */
  end: (event?: TouchEvent) => void
  /** 重置状态 */
  reset: () => void
}

/**
 * 获取触摸点坐标
 * @param event - 触摸事件
 * @returns 坐标对象
 */
const getTouchPoint = (event: TouchEvent): { x: number; y: number } => {
  const touch = event.touches[0] || event.changedTouches[0]
  return {
    x: touch.clientX,
    y: touch.clientY,
  }
}

/**
 * 获取滑动方向
 * @param deltaX - X方向移动距离
 * @param deltaY - Y方向移动距离
 * @returns 滑动方向
 */
const getDirection = (deltaX: number, deltaY: number): 'horizontal' | 'vertical' | '' => {
  const absX = Math.abs(deltaX)
  const absY = Math.abs(deltaY)

  if (absX === 0 && absY === 0) {
    return ''
  }

  return absX > absY ? 'horizontal' : 'vertical'
}

/**
 * 触摸手势组合式函数
 * @param options - 配置选项
 * @returns 触摸控制方法
 * @example
 * ```vue
 * <template>
 *   <div
 *     @touchstart="touch.start"
 *     @touchmove="touch.move"
 *     @touchend="touch.end"
 *   >
 *     Swipe area
 *   </div>
 * </template>
 *
 * <script setup>
 * const touch = useTouch({ threshold: 50 })
 * </script>
 * ```
 */
export const useTouch = (options: IUseTouchOptions = {}): IUseTouchReturn => {
  const { threshold = 0, preventDefault = false, stopPropagation = false } = options

  const state = reactive<ITouchState>({
    startX: 0,
    startY: 0,
    deltaX: 0,
    deltaY: 0,
    offsetX: 0,
    offsetY: 0,
    direction: '',
    touching: false,
  })

  /**
   * 触摸开始
   * @param event - 触摸事件
   */
  const start = (event: TouchEvent): void => {
    if (preventDefault) {
      event.preventDefault()
    }
    if (stopPropagation) {
      event.stopPropagation()
    }

    const point = getTouchPoint(event)
    state.startX = point.x
    state.startY = point.y
    state.deltaX = 0
    state.deltaY = 0
    state.offsetX = 0
    state.offsetY = 0
    state.direction = ''
    state.touching = true
  }

  /**
   * 触摸移动
   * @param event - 触摸事件
   */
  const move = (event: TouchEvent): void => {
    if (!state.touching) return

    if (preventDefault) {
      event.preventDefault()
    }
    if (stopPropagation) {
      event.stopPropagation()
    }

    const point = getTouchPoint(event)
    state.deltaX = point.x - state.startX
    state.deltaY = point.y - state.startY
    state.offsetX = Math.abs(state.deltaX)
    state.offsetY = Math.abs(state.deltaY)

    if (!state.direction && (state.offsetX > threshold || state.offsetY > threshold)) {
      state.direction = getDirection(state.deltaX, state.deltaY)
    }
  }

  /**
   * 触摸结束
   * @param event - 触摸事件
   */
  const end = (event?: TouchEvent): void => {
    if (event) {
      if (preventDefault) {
        event.preventDefault()
      }
      if (stopPropagation) {
        event.stopPropagation()
      }
    }
    state.touching = false
  }

  /**
   * 重置状态
   */
  const reset = (): void => {
    state.startX = 0
    state.startY = 0
    state.deltaX = 0
    state.deltaY = 0
    state.offsetX = 0
    state.offsetY = 0
    state.direction = ''
    state.touching = false
  }

  return {
    state,
    start,
    move,
    end,
    reset,
  }
}

/**
 * 滑动方向判断
 * @param touchState - 触摸状态
 * @param direction - 期望方向
 * @param threshold - 触发阈值
 * @returns 是否匹配
 */
export const isSwipeDirection = (
  touchState: ITouchState,
  direction: 'left' | 'right' | 'up' | 'down',
  threshold: number = 50
): boolean => {
  if (!touchState.direction) return false

  switch (direction) {
    case 'left':
      return touchState.direction === 'horizontal' && touchState.deltaX < -threshold
    case 'right':
      return touchState.direction === 'horizontal' && touchState.deltaX > threshold
    case 'up':
      return touchState.direction === 'vertical' && touchState.deltaY < -threshold
    case 'down':
      return touchState.direction === 'vertical' && touchState.deltaY > threshold
    default:
      return false
  }
}

export default useTouch
