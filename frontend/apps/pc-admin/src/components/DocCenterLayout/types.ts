/**
 * DocCenterLayout 类型定义
 * 复用单据处理中心布局组件配置类型
 */

/** 主Tab配置（工作流阶段） */
export interface DocMainTab {
  key: string
  label: string
}

/** 子Tab配置（维度视图） */
export interface DocSubTab {
  key: string
  label: string
}

/** 日期快捷选项 */
export interface DateShortcutItem {
  key: string
  label: string
}

/** 统计卡片配置 */
export interface StatCardItem {
  label: string
  valueKey: string
  /** 左边框颜色 (CSS color) */
  color: string
}

/** 搜索字段类型 */
export type SearchFieldType = 'input' | 'select' | 'dateRange' | 'checkbox'

/** 搜索字段配置 */
export interface SearchFieldItem {
  key: string
  label: string
  type: SearchFieldType
  placeholder?: string | [string, string]
  span?: number
  options?: Array<{ label: string; value: any }>
  /** input 后缀图标, 'search' 显示 SearchOutlined */
  suffix?: 'search'
}

/** 工具栏按钮配置 */
export interface ToolbarButtonItem {
  key: string
  label: string
  type?: 'primary' | 'default' | 'link'
  icon?: string
  danger?: boolean
  /** 显示条件，返回 true 则显示 */
  visibleFor?: (activeMainTab: string, activeSubTab: string) => boolean
  /** 下拉菜单项 */
  dropdownItems?: Array<{ key: string; label: string; danger?: boolean }>
}

/** 履约统计卡片配置 */
export interface FulfillmentStatItem {
  label: string
  valueKey: string
  bgColor?: string
}

/** 图表区域配置 */
export interface ChartConfig {
  visible: boolean
  title?: string
  height?: number
  /** 是否显示按天/按周/按月切换按钮 */
  showTimeToggle?: boolean
  /** 图例项 */
  legend?: Array<{ label: string; color: string }>
}

/** 搜索字段配置映射：key = mainTab 或 "mainTab.subTab" */
export type SearchConfigMap = Record<string, SearchFieldItem[]>

/** 搜索复选框配置项 */
export interface SearchCheckboxItem {
  key: string
  label: string
}

/** 搜索复选框配置映射 */
export type SearchCheckboxConfigMap = Record<string, SearchCheckboxItem[]>

/** 工具栏按钮配置映射 */
export type ToolbarConfigMap = Record<string, ToolbarButtonItem[]>

/**
 * 功能按钮启用态（由页面配置弹窗的「功能按钮」页签产生）。
 *
 * key 必须与 ToolbarConfigMap 里按钮的 key 一致，否则开关不起作用；
 * 未列出的按钮视为启用（fail-open）。与 PageConfigPanel 导出的同名接口同形，
 * 属结构化类型，页面直接把自己的配置对象传进来即可。
 */
export interface FunctionButtonSetting {
  key: string
  label: string
  enabled: boolean
}

/** 统计卡片配置映射 */
export type StatCardConfigMap = Record<string, StatCardItem[]>

/** 履约统计配置映射 */
export type FulfillmentConfigMap = Record<string, FulfillmentStatItem[]>
