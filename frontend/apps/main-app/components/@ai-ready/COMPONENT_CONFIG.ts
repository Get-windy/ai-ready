// AI-Ready 前端组件配置
// Vue 3.4 + Element Plus 2.4 + TypeScript

import type { App } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 主题配置
export const themeConfig = {
  // 主色调
  primaryColor: '#409EFF',
  successColor: '#67C23A',
  warningColor: '#E6A23C',
  dangerColor: '#F56C6C',
  infoColor: '#909399',

  // 中性色
  textColor: '#303133',
  textColorRegular: '#606266',
  textColorSecondary: '#909399',
  textColorPlaceholder: '#A8ABB2',

  // 边框色
  borderColor: '#DCDFE6',
  borderColorLight: '#E4E7ED',
  borderColorLighter: '#EBEEF5',
  borderColorExtraLight: '#F2F6FC',

  // 背景色
  backgroundColor: '#FFFFFF',
  backgroundColorPage: '#F5F7FA',

  // 圆角
  borderRadiusBase: '4px',
  borderRadiusSmall: '2px',
  borderRadiusLarge: '8px',
  borderRadiusRound: '20px',

  // 字体
  fontFamily: '"Helvetica Neue", Helvetica, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", Arial, sans-serif',
  fontSizeBase: '14px',
  fontSizeSmall: '12px',
  fontSizeLarge: '16px',
}

// 全局配置
export const globalConfig = {
  // 尺寸
  size: 'default', // 'large' | 'default' | 'small'

  // zIndex
  zIndex: 2000,

  // 语言
  locale: 'zh-cn',
}

// 组件注册配置
export function setupElementPlus(app: App) {
  // 注册 Element Plus
  app.use(ElementPlus, {
    size: globalConfig.size,
    zIndex: globalConfig.zIndex,
  })

  // 注册所有图标
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
  }

  // 设置主题变量
  app.config.globalProperties.$ELEMENT = globalConfig
}

// Element Plus 按需引入配置
export const elementPlusComponents = [
  // 布局
  'ElContainer',
  'ElHeader',
  'ElAside',
  'ElMain',
  'ElFooter',

  // 基础
  'ElButton',
  'ElLink',
  'ElText',

  // 表单
  'ElInput',
  'ElInputNumber',
  'ElSelect',
  'ElCascader',
  'ElSwitch',
  'ElSlider',
  'ElTimePicker',
  'ElDatePicker',
  'ElUpload',
  'ElRate',
  'ElColorPicker',
  'ElTransfer',

  // 数据
  'ElTable',
  'ElTableColumn',
  'ElTag',
  'ElProgress',
  'ElTree',
  'ElPagination',
  'ElBadge',

  // 反馈
  'ElAlert',
  'ElLoading',
  'ElMessage',
  'ElMessageBox',
  'ElNotification',

  // 其他
  'ElDrawer',
  'ElDialog',
  'ElTooltip',
  'ElPopover',
  'ElPopconfirm',
  'ElCard',
  'ElCollapse',
  'ElCollapseItem',
  'ElTabs',
  'ElTabPane',
  'ElBreadcrumb',
  'ElBreadcrumbItem',
  'ElPageHeader',
  'ElSteps',
  'ElStep',
  'ElMenu',
  'ElMenuItem',
  'ElSubMenu',
  'ElMenuItemGroup',
]

// UniApp 跨平台兼容配置
export const uniAppConfig = {
  // 平台适配
  platform: process.env.UNI_PLATFORM || 'h5',

  // 样式兼容
  style: {
    useRem: false, // 是否使用 rem 单位
    usePx: true,  // 是否使用 px 单位
  },

  // API 兼容
  api: {
    // 是否使用条件编译
    useConditional: true,

    // 是否使用 polyfill
    usePolyfill: true,
  },

  // 组件兼容
  components: {
    // 是否使用 UniApp 原生组件
    useNative: true,

    // 是否使用 Vue 组件
    useVue: true,
  },
}

// TypeScript 类型定义
export type ComponentSize = 'large' | 'default' | 'small'
export type ComponentState = 'default' | 'success' | 'warning' | 'danger' | 'info'
export type ComponentPlacement = 'top' | 'right' | 'bottom' | 'left'

// 响应式断点
export const breakpoints = {
  xs: 480,
  sm: 576,
  md: 768,
  lg: 992,
  xl: 1200,
  xxl: 1600,
}

// 动画配置
export const animationConfig = {
  duration: 300,
  easing: 'ease-in-out',
  delay: 0,
}

// 导出默认配置
export default {
  themeConfig,
  globalConfig,
  setupElementPlus,
  elementPlusComponents,
  uniAppConfig,
  breakpoints,
  animationConfig,
}