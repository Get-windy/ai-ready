/**
 * AI-Ready Mobile UI 组合式函数入口
 * @package @ai-ready/mobile-ui
 */

// useTouch - 触摸手势
export { useTouch, isSwipeDirection } from './useTouch'
export type {
  ITouchState,
  IUseTouchOptions,
  IUseTouchReturn,
} from './useTouch'

// useCountDown - 倒计时
export { useCountDown, useSmsCountDown, useSimpleCountDown } from './useCountDown'
export type {
  ICountDownOptions,
  IUseCountDownReturn,
} from './useCountDown'

// useDebounce - 防抖/节流
export {
  debounce,
  throttle,
  useDebounce,
  useThrottle,
  useDebounceValue,
} from './useDebounce'
export type {
  IDebounceOptions,
  IThrottleOptions,
} from './useDebounce'
