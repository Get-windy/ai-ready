/**
 * 响应式布局工具
 * 支持移动端、平板、桌面端适配
 */

import { ref, onMounted, onUnmounted, computed } from 'vue'

// 断点配置
export const breakpoints = {
  xs: 480,   // 手机竖屏
  sm: 576,   // 手机横屏
  md: 768,   // 平板竖屏
  lg: 992,   // 平板横屏/小型笔记本
  xl: 1200,  // 桌面
  xxl: 1600  // 大屏幕
}

// 设备类型
export type DeviceType = 'mobile' | 'tablet' | 'desktop'

// 响应式状态
export interface ResponsiveState {
  width: number
  height: number
  deviceType: DeviceType
  isMobile: boolean
  isTablet: boolean
  isDesktop: boolean
  breakpoint: keyof typeof breakpoints | 'xxl'
  orientation: 'portrait' | 'landscape'
}

/**
 * 响应式 Hook
 */
export function useResponsive() {
  const width = ref(window.innerWidth)
  const height = ref(window.innerHeight)
  
  // 更新尺寸
  const updateSize = () => {
    width.value = window.innerWidth
    height.value = window.innerHeight
  }
  
  // 监听窗口变化
  onMounted(() => {
    window.addEventListener('resize', updateSize)
  })
  
  onUnmounted(() => {
    window.removeEventListener('resize', updateSize)
  })
  
  // 计算设备类型
  const deviceType = computed<DeviceType>(() => {
    if (width.value < breakpoints.md) return 'mobile'
    if (width.value < breakpoints.xl) return 'tablet'
    return 'desktop'
  })
  
  // 计算断点
  const breakpoint = computed(() => {
    const w = width.value
    if (w < breakpoints.xs) return 'xs'
    if (w < breakpoints.sm) return 'sm'
    if (w < breakpoints.md) return 'md'
    if (w < breakpoints.lg) return 'lg'
    if (w < breakpoints.xl) return 'xl'
    if (w < breakpoints.xxl) return 'xxl'
    return 'xxl'
  })
  
  // 计算屏幕方向
  const orientation = computed<'portrait' | 'landscape'>(() => {
    return width.value > height.value ? 'landscape' : 'portrait'
  })
  
  return {
    width,
    height,
    deviceType,
    breakpoint,
    orientation,
    isMobile: computed(() => deviceType.value === 'mobile'),
    isTablet: computed(() => deviceType.value === 'tablet'),
    isDesktop: computed(() => deviceType.value === 'desktop')
  }
}

/**
 * 栅格配置
 */
export interface GridConfig {
  gutter: number
  columns: number
}

export const gridConfigs: Record<DeviceType, GridConfig> = {
  mobile: { gutter: 16, columns: 4 },
  tablet: { gutter: 20, columns: 8 },
  desktop: { gutter: 24, columns: 12 }
}

/**
 * 响应式栅格 Hook
 */
export function useResponsiveGrid() {
  const { deviceType } = useResponsive()
  
  const gridConfig = computed(() => gridConfigs[deviceType.value])
  
  return {
    gridConfig,
    gutter: computed(() => gridConfig.value.gutter),
    columns: computed(() => gridConfig.value.columns)
  }
}

/**
 * 媒体查询工具
 */
export function useMediaQuery(query: string) {
  const matches = ref(false)
  
  const updateMatch = () => {
    matches.value = window.matchMedia(query).matches
  }
  
  onMounted(() => {
    updateMatch()
    window.matchMedia(query).addEventListener('change', updateMatch)
  })
  
  onUnmounted(() => {
    window.matchMedia(query).removeEventListener('change', updateMatch)
  })
  
  return matches
}

/**
 * 断点媒体查询
 */
export function useBreakpoint(breakpoint: keyof typeof breakpoints) {
  return useMediaQuery(`(min-width: ${breakpoints[breakpoint]}px)`)
}

export default {
  breakpoints,
  useResponsive,
  useResponsiveGrid,
  useMediaQuery,
  useBreakpoint
}