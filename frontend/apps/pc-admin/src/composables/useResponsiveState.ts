/**
 * 响应式布局 Composable
 */
import { ref, onMounted, onUnmounted } from 'vue'
import { breakpoints, getBreakpoint, isMobile, isTablet, isDesktop } from './useResponsive'

export function useResponsive() {
  const windowWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1200)
  const currentBreakpoint = ref(getBreakpoint(windowWidth.value))
  const isMobileView = ref(isMobile(windowWidth.value))
  const isTabletView = ref(isTablet(windowWidth.value))
  const isDesktopView = ref(isDesktop(windowWidth.value))

  const handleResize = () => {
    const width = window.innerWidth
    windowWidth.value = width
    currentBreakpoint.value = getBreakpoint(width)
    isMobileView.value = isMobile(width)
    isTabletView.value = isTablet(width)
    isDesktopView.value = isDesktop(width)
  }

  onMounted(() => {
    window.addEventListener('resize', handleResize)
    handleResize() // 初始化
  })

  onUnmounted(() => {
    window.removeEventListener('resize', handleResize)
  })

  return {
    windowWidth,
    currentBreakpoint,
    isMobileView,
    isTabletView,
    isDesktopView
  }
}

export default useResponsive