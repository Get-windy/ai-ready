/**
 * Design Token 常量
 * CSS 变量名称的编程时引用，确保类型安全
 */

export const DESIGN_TOKENS = {
  // 主色
  COLOR_PRIMARY: '--color-primary',
  COLOR_PRIMARY_HOVER: '--color-primary-hover',
  COLOR_PRIMARY_ACTIVE: '--color-primary-active',
  COLOR_PRIMARY_BG: '--color-primary-bg',
  COLOR_PRIMARY_BORDER: '--color-primary-border',

  // 语义色
  COLOR_SUCCESS: '--color-success',
  COLOR_WARNING: '--color-warning',
  COLOR_DANGER: '--color-danger',
  COLOR_INFO: '--color-info',

  // 文字
  COLOR_TEXT_PRIMARY: '--color-text-primary',
  COLOR_TEXT_SECONDARY: '--color-text-secondary',
  COLOR_TEXT_TERTIARY: '--color-text-tertiary',

  // 边框
  COLOR_BORDER_BASE: '--color-border-base',
  COLOR_BORDER_LIGHT: '--color-border-light',

  // 背景
  COLOR_BG_BASE: '--color-bg-base',
  COLOR_BG_LAYOUT: '--color-bg-layout',
  COLOR_BG_CONTAINER: '--color-bg-container',

  // 间距
  SPACING_XS: '--spacing-xs',
  SPACING_SM: '--spacing-sm',
  SPACING_MD: '--spacing-md',
  SPACING_LG: '--spacing-lg',
  SPACING_XL: '--spacing-xl',
  SPACING_XXL: '--spacing-xxl',

  // 圆角
  BORDER_RADIUS_SM: '--border-radius-sm',
  BORDER_RADIUS_BASE: '--border-radius-base',
  BORDER_RADIUS_LG: '--border-radius-lg',

  // 阴影
  SHADOW_SM: '--shadow-sm',
  SHADOW_BASE: '--shadow-base',
  SHADOW_MD: '--shadow-md',
  SHADOW_LG: '--shadow-lg',

  // 字体
  FONT_SIZE_XS: '--font-size-xs',
  FONT_SIZE_SM: '--font-size-sm',
  FONT_SIZE_BASE: '--font-size-base',
  FONT_SIZE_LG: '--font-size-lg',

  // 动画
  DURATION_FAST: '--duration-fast',
  DURATION_BASE: '--duration-base',
  DURATION_SLOW: '--duration-slow',

  // Z-index
  Z_INDEX_DROPDOWN: '--z-index-dropdown',
  Z_INDEX_MODAL: '--z-index-modal',
  Z_INDEX_TOOLTIP: '--z-index-tooltip',
} as const

export type DesignToken = typeof DESIGN_TOKENS[keyof typeof DESIGN_TOKENS]
