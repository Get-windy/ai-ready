/**
 * 企智连数据可视化组件库 - 动画引擎
 * Author: AI应用开发工程师 | Date: 2026-04-17
 */

import { ref } from 'vue'

// 动画配置
export interface AnimationConfig {
  enabled: boolean
  duration: number
  easing: 'linear' | 'ease' | 'ease-in' | 'ease-out' | 'ease-in-out'
  delay: number
  staggerDelay?: number
}

// 默认配置
export const defaultAnimationConfig: AnimationConfig = {
  enabled: true,
  duration: 800,
  easing: 'ease-out',
  delay: 0,
  staggerDelay: 50
}

// 缓动函数
export const easingFunctions: Record<string, (t: number) => number> = {
  linear: t => t,
  ease: t => t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t,
  'ease-in': t => t * t,
  'ease-out': t => t * (2 - t),
  'ease-in-out': t => t < 0.5 ? 2 * t * t : -1 + (4 - 2 * t) * t
}

/**
 * 数字动画
 */
export function animateNumber(
  from: number,
  to: number,
  duration: number,
  easing: string = 'ease-out',
  onUpdate: (value: number) => void,
  onComplete?: () => void
): () => void {
  const startTime = performance.now()
  const easingFn = easingFunctions[easing] || easingFunctions['ease-out']
  let cancelled = false
  
  const tick = (currentTime: number) => {
    if (cancelled) return
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / duration, 1)
    const easedProgress = easingFn(progress)
    const currentValue = from + (to - from) * easedProgress
    onUpdate(currentValue)
    if (progress < 1) {
      requestAnimationFrame(tick)
    } else {
      onComplete?.()
    }
  }
  
  requestAnimationFrame(tick)
  return () => { cancelled = true }
}

/**
 * 动画管理器 Composable
 */
export function useChartAnimation(config: Partial<AnimationConfig> = {}) {
  const mergedConfig = { ...defaultAnimationConfig, ...config }
  const isAnimating = ref(false)
  const animationProgress = ref(0)
  const animations: (() => void)[] = []
  
  const addAnimation = (cancelFn: () => void) => {
    animations.push(cancelFn)
  }
  
  const clearAnimations = () => {
    animations.forEach(cancel => cancel())
    animations.length = 0
    isAnimating.value = false
    animationProgress.value = 0
  }
  
  const animateValue = (
    from: number,
    to: number,
    onUpdate: (value: number) => void,
    onComplete?: () => void
  ): (() => void) => {
    if (!mergedConfig.enabled) {
      onUpdate(to)
      onComplete?.()
      return () => {}
    }
    isAnimating.value = true
    animationProgress.value = 0
    const cancelFn = animateNumber(
      from, to, mergedConfig.duration, mergedConfig.easing,
      (value) => {
        animationProgress.value = (value - from) / (to - from)
        onUpdate(value)
      },
      () => {
        isAnimating.value = false
        animationProgress.value = 1
        onComplete?.()
      }
    )
    addAnimation(cancelFn)
    return cancelFn
  }
  
  return {
    isAnimating,
    animationProgress,
    animateValue,
    clearAnimations
  }
}
