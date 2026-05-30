import { addons } from '@storybook/manager-api'
import { create } from '@storybook/theming/create'

/**
 * AI-Ready 品牌主题
 *
 * Primary palette:
 *   Blue  #409eff  – 品牌主色
 *   Dark  #0a1633  – 主文字 / 深色背景
 *   Gray  #1e293b  – 侧栏背景
 */
const aiReadyTheme = create({
  base: 'light',

  // 品牌标识
  brandTitle: 'AI-Ready UI Components',
  brandUrl: 'https://github.com/ai-ready/ai-ready-ui',
  brandImage: undefined,
  brandTarget: '_blank',

  // 主色
  colorPrimary: '#409eff',
  colorSecondary: '#3a8ee6',

  // UI 颜色
  appBg: '#f2f3f5',
  appContentBg: '#ffffff',
  appPreviewBg: '#ffffff',
  appBorderColor: '#dcdfe6',
  appBorderRadius: 4,

  // 文本颜色
  textColor: '#303133',
  textInverseColor: '#ffffff',
  textMutedColor: '#909399',

  // 工具栏颜色
  barTextColor: '#606266',
  barSelectedColor: '#409eff',
  barHoverColor: '#409eff',
  barBg: '#ffffff',

  // 表单颜色
  inputBg: '#ffffff',
  inputBorder: '#dcdfe6',
  inputTextColor: '#303133',
  inputBorderRadius: 4,

  // 布尔控件
  booleanBg: '#f0f2f5',
  booleanSelectedBg: '#ecf5ff',

  // 按钮颜色
  buttonBg: '#f0f2f5',
  buttonBorder: '#dcdfe6',

  // 字体
  fontBase: '"PingFang SC", "Helvetica Neue", Arial, sans-serif',
  fontCode: '"Fira Code", "Cascadia Code", Consolas, monospace',

  // 尺寸
  baseTextWeight: '400',
  baseTextWeightBold: '600',
})

addons.setConfig({
  theme: aiReadyTheme,
  showToolbar: true,
  bottomPanelHeight: 300,
  enableShortcuts: true,
  sidebar: {
    showRoots: true,
    collapsedRoots: ['business', 'feedback', 'mobile'],
  },
  toolbar: {
    title: { hidden: false },
    zoom: { hidden: false },
    eject: { hidden: false },
    copy: { hidden: false },
    fullscreen: { hidden: false },
  },
})
