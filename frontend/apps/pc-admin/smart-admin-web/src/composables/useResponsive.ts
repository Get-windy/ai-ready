/**
 * 响应式布局配置
 */

// 断点定义
export const breakpoints = {
  xs: '480px',   // 手机
  sm: '576px',   // 手机
  md: '768px',   // 平板
  lg: '992px',   // 桌面
  xl: '1200px',  // 大桌面
  xxl: '1600px'  // 超大桌面
}

// 布局配置
export const layoutConfig = {
  // 侧边栏
  sidebar: {
    // PC端
    desktop: {
      width: 256,
      collapsedWidth: 64,
      breakpoint: 'lg'
    },
    // 平板
    tablet: {
      width: 200,
      collapsedWidth: 64,
      breakpoint: 'md'
    },
    // 移动端
    mobile: {
      width: 0,     // 移动端默认隐藏侧边栏
      collapsedWidth: 0,
      breakpoint: 'sm'
    }
  },
  
  // 内容区
  content: {
    padding: {
      desktop: '24px',
      tablet: '16px',
      mobile: '12px'
    }
  },
  
  // Header
  header: {
    height: {
      desktop: '64px',
      tablet: '56px',
      mobile: '48px'
    }
  }
}

// 响应式工具函数
export function getBreakpoint(width: number): string {
  const w = parseInt(breakpoints.xs.replace('px', ''))
  const sm = parseInt(breakpoints.sm.replace('px', ''))
  const md = parseInt(breakpoints.md.replace('px', ''))
  const lg = parseInt(breakpoints.lg.replace('px', ''))
  const xl = parseInt(breakpoints.xl.replace('px', ''))
  const xxl = parseInt(breakpoints.xxl.replace('px', ''))
  
  if (width >= xxl) return 'xxl'
  if (width >= xl) return 'xl'
  if (width >= lg) return 'lg'
  if (width >= md) return 'md'
  if (width >= sm) return 'sm'
  return 'xs'
}

export function isMobile(width: number): boolean {
  return width < parseInt(breakpoints.md.replace('px', ''))
}

export function isTablet(width: number): boolean {
  const w = parseInt(breakpoints.md.replace('px', ''))
  const lg = parseInt(breakpoints.lg.replace('px', ''))
  return width >= w && width < lg
}

export function isDesktop(width: number): boolean {
  return width >= parseInt(breakpoints.lg.replace('px', ''))
}

export default {
  breakpoints,
  layoutConfig,
  getBreakpoint,
  isMobile,
  isTablet,
  isDesktop
}