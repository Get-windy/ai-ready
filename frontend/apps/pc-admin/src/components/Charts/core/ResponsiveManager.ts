/**
 * 企智连数据可视化组件库 - 响应式管理器
 * Author: AI应用开发工程师 | Date: 2026-04-17
 */

import { ref, computed, onMounted, onUnmounted, watch, type Ref } from 'vue'

// 响应式断点
export interface Breakpoints {
  xs: number   // < 576px - 手机
  sm: number   // >= 576px - 大屏手机
  md: number   // >= 768px - 平板
  lg: number   // >= 992px - 桌面
  xl: number   // >= 1200px - 大屏桌面
  xxl: number  // >= 1400px - 超大屏
}

// 默认断点
export const defaultBreakpoints: Breakpoints = {
  xs: 0,
  sm: 576,
  md: 768,
  lg: 992,
  xl: 1200,
  xxl: 1400
}

// 响应式配置
export interface ResponsiveConfig {
  breakpoints: Breakpoints
  resizeDelay: number      // 防抖延迟(ms)
  aspectRatio: number      // 默认宽高比
  minWidth: number         // 最小宽度
  minHeight: number        // 最小高度
  maxWidth: number         // 最大宽度
  maxHeight: number        // 最大高度
}

// 默认配置
export const defaultResponsiveConfig: ResponsiveConfig = {
  breakpoints: defaultBreakpoints,
  resizeDelay: 100,
  aspectRatio: 4 / 3,
  minWidth: 200,
  minHeight: 150,
  maxWidth: 2000,
  maxHeight: 1500
}

// 当前断点类型
export type BreakpointType = 'xs' | 'sm' | 'md' | 'lg' | 'xl' | 'xxl'

// 响应式尺寸
export interface ResponsiveSize {
  width: number
  height: number
  breakpoint: BreakpointType
}

/**
 * 响应式管理器 Composable
 */
export function useChartResponsive(
  containerRef: Ref<HTMLElement | null>,
  config: Partial<ResponsiveConfig> = {}
) {
  const mergedConfig = { ...defaultResponsiveConfig, ...config }
  
  // 当前尺寸
  const width = ref(mergedConfig.minWidth)
  const height = ref(mergedConfig.minHeight)
  const currentBreakpoint = ref<BreakpointType>('md')
  
  // 是否正在调整大小
  const isResizing = ref(false)
  
  // 调整大小定时器
  let resizeTimer: ReturnType<typeof setTimeout> | null = null
  let resizeObserver: ResizeObserver | null = null
  
  /**
   * 获取当前断点
   */
  const getBreakpoint = (w: number): BreakpointType => {
    const { sm, md, lg, xl, xxl } = mergedConfig.breakpoints
    if (w >= xxl) return 'xxl'
    if (w >= xl) return 'xl'
    if (w >= lg) return 'lg'
    if (w >= md) return 'md'
    if (w >= sm) return 'sm'
    return 'xs'
  }
  
  /**
   * 计算高度
   */
  const calculateHeight = (w: number): number => {
    const calculated = w / mergedConfig.aspectRatio
    return Math.max(
      mergedConfig.minHeight,
      Math.min(calculated, mergedConfig.maxHeight)
    )
  }
  
  /**
   * 更新尺寸
   */
  const updateSize = (newWidth: number, newHeight?: number) => {
    // 限制宽度范围
    const clampedWidth = Math.max(
      mergedConfig.minWidth,
      Math.min(newWidth, mergedConfig.maxWidth)
    )
    
    // 计算或限制高度
    const clampedHeight = newHeight !== undefined
      ? Math.max(mergedConfig.minHeight, Math.min(newHeight, mergedConfig.maxHeight))
      : calculateHeight(clampedWidth)
    
    width.value = clampedWidth
    height.value = clampedHeight
    currentBreakpoint.value = getBreakpoint(clampedWidth)
  }
  
  /**
   * 处理容器大小变化
   */
  const handleResize = (entries: ResizeObserverEntry[]) => {
    if (!entries.length) return
    
    const entry = entries[0]
    const { width: newWidth, height: newHeight } = entry.contentRect
    
    isResizing.value = true
    
    // 防抖处理
    if (resizeTimer) {
      clearTimeout(resizeTimer)
    }
    
    resizeTimer = setTimeout(() => {
      updateSize(newWidth, newHeight)
      isResizing.value = false
    }, mergedConfig.resizeDelay)
  }
  
  /**
   * 初始化响应式监听
   */
  const initResponsive = () => {
    if (!containerRef.value) return
    
    // 初始化尺寸
    const rect = containerRef.value.getBoundingClientRect()
    updateSize(rect.width, rect.height)
    
    // 使用 ResizeObserver
    if (typeof ResizeObserver !== 'undefined') {
      resizeObserver = new ResizeObserver(handleResize)
      resizeObserver.observe(containerRef.value)
    } else {
      // 降级方案：监听 window resize
      window.addEventListener('resize', handleWindowResize)
    }
  }
  
  /**
   * window resize 降级处理
   */
  const handleWindowResize = () => {
    if (!containerRef.value) return
    const rect = containerRef.value.getBoundingClientRect()
    updateSize(rect.width, rect.height)
  }
  
  /**
   * 清理监听
   */
  const cleanup = () => {
    if (resizeTimer) {
      clearTimeout(resizeTimer)
      resizeTimer = null
    }
    
    if (resizeObserver) {
      resizeObserver.disconnect()
      resizeObserver = null
    }
    
    window.removeEventListener('resize', handleWindowResize)
  }
  
  // 生命周期
  onMounted(() => {
    initResponsive()
  })
  
  onUnmounted(() => {
    cleanup()
  })
  
  // 监听容器变化
  watch(containerRef, (newVal, oldVal) => {
    if (oldVal && resizeObserver) {
      resizeObserver.unobserve(oldVal)
    }
    if (newVal) {
      initResponsive()
    }
  })
  
  // 计算属性
  const responsiveSize = computed<ResponsiveSize>(() => ({
    width: width.value,
    height: height.value,
    breakpoint: currentBreakpoint.value
  }))
  
  // 是否是移动端
  const isMobile = computed(() => ['xs', 'sm'].includes(currentBreakpoint.value))
  
  // 是否是平板
  const isTablet = computed(() => currentBreakpoint.value === 'md')
  
  // 是否是桌面
  const isDesktop = computed(() => ['lg', 'xl', 'xxl'].includes(currentBreakpoint.value))
  
  return {
    width,
    height,
    currentBreakpoint,
    responsiveSize,
    isResizing,
    isMobile,
    isTablet,
    isDesktop,
    updateSize,
    cleanup
  }
}

/**
 * 响应式容器组件 Props
 */
export interface ResponsiveContainerProps {
  aspectRatio?: number
  minWidth?: number
  minHeight?: number
  maxWidth?: number
  maxHeight?: number
  resizeDelay?: number
}

/**
 * 根据断点获取适配的配置
 */
export function getBreakpointConfig<T>(
  breakpoint: BreakpointType,
  configs: Partial<Record<BreakpointType, T>>,
  defaultValue: T
): T {
  // 从当前断点向上查找配置
  const order: BreakpointType[] = ['xs', 'sm', 'md', 'lg', 'xl', 'xxl']
  const currentIndex = order.indexOf(breakpoint)
  
  for (let i = currentIndex; i < order.length; i++) {
    const bp = order[i]
    if (configs[bp] !== undefined) {
      return configs[bp]!
    }
  }
  
  // 向下查找
  for (let i = currentIndex - 1; i >= 0; i--) {
    const bp = order[i]
    if (configs[bp] !== undefined) {
      return configs[bp]!
    }
  }
  
  return defaultValue
}

/**
 * 响应式字体大小
 */
export function getResponsiveFontSize(
  breakpoint: BreakpointType,
  baseSize: number
): number {
  const scales: Record<BreakpointType, number> = {
    xs: 0.8,
    sm: 0.85,
    md: 0.9,
    lg: 1,
    xl: 1.05,
    xxl: 1.1
  }
  return Math.round(baseSize * scales[breakpoint])
}

/**
 * 响应式边距
 */
export function getResponsivePadding(
  breakpoint: BreakpointType
): { top: number; right: number; bottom: number; left: number } {
  const paddings: Record<BreakpointType, { top: number; right: number; bottom: number; left: number }> = {
    xs: { top: 30, right: 20, bottom: 40, left: 40 },
    sm: { top: 40, right: 30, bottom: 50, left: 50 },
    md: { top: 50, right: 40, bottom: 60, left: 60 },
    lg: { top: 60, right: 60, bottom: 70, left: 70 },
    xl: { top: 60, right: 80, bottom: 70, left: 80 },
    xxl: { top: 60, right: 100, bottom: 70, left: 100 }
  }
  return paddings[breakpoint]
}
