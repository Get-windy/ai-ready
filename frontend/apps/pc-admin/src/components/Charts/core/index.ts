/**
 * 企智连数据可视化组件库 - 核心层导出
 */

// 主题管理
export {
  useChartTheme,
  getTheme,
  getGlobalThemeManager,
  setGlobalTheme,
  defaultTheme,
  darkTheme,
  vintageTheme,
  colorfulTheme,
  corporateTheme,
  themes,
  type ChartTheme,
  type ThemeColors,
  type ThemeFont,
  type ThemeAnimation
} from './ThemeManager'

// 响应式管理
export {
  useChartResponsive,
  getBreakpointConfig,
  getResponsiveFontSize,
  getResponsivePadding,
  defaultBreakpoints,
  defaultResponsiveConfig,
  type Breakpoints,
  type ResponsiveConfig,
  type ResponsiveSize,
  type BreakpointType
} from './ResponsiveManager'

// 动画引擎
export {
  useChartAnimation,
  animateNumber,
  easingFunctions,
  defaultAnimationConfig,
  type AnimationConfig
} from './AnimationEngine'
