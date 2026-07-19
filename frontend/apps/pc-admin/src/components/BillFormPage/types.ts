/**
 * BillFormPage 通用业务单据表单页 - 类型定义
 *
 * 五区标准布局：
 * 1. 头部操作栏 (Header)
 * 2. 基本信息选择器 (BasicInfo)
 * 3. 明细表格区 (DetailTable) — 由业务页面通过 #detail-table 插槽提供
 * 4. 底部面板 (BottomPanel) — 左标签页 + 右摘要
 * 5. 页脚操作栏 (Footer)
 */

import type { Component } from 'vue'

// ── 头部操作栏 ──
export interface HeaderAction {
  key: string
  label: string
  icon?: Component
  children?: { key: string; label: string }[]
}

export interface BillHeaderConfig {
  /** 单据标题，如 "销售订单" */
  title: string
  /** 单据编号，如 "XSDD-20260621-A1B2" */
  orderNo?: string
  /** 是否显示附件按钮 */
  showAttachment?: boolean
  /** 操作按钮列表（下拉菜单项） */
  actions?: HeaderAction[]
}

// ── 基本信息字段 ──
export type FieldType = 'select' | 'input' | 'date' | 'number' | 'textarea' | 'display'

export interface BasicInfoFieldOption {
  label: string
  value: string | number
}

export interface BasicInfoField {
  /** 唯一标识，对应 modelValue 中的 key */
  key: string
  /** 字段标签 */
  label: string
  /** 是否必填 */
  required?: boolean
  /** 字段类型 */
  type: FieldType
  /** 占位文本 */
  placeholder?: string
  /** 下拉选项（type=select 时使用） */
  options?: BasicInfoFieldOption[]
  /** 日期格式（type=date 时使用） */
  format?: string
  /** 搜索按钮文本（如 "+Q"、"Q"） */
  searchBtn?: string
  /** 字段宽度：'default' | 'narrow' | 'wide' 或具体像素值 */
  width?: 'default' | 'narrow' | 'wide' | number
  /** 是否加载中 */
  loading?: boolean
  /** 数字精度（type=number 时使用） */
  precision?: number
  /** 最小值（type=number 时使用） */
  min?: number
  /** 最大值（type=number 时使用） */
  max?: number
  /** 栅格占列数：12=半行，24=整行（映射到 width='wide'） */
  span?: number
  /** 所在行号（1=第一行，2=第二行等），用于多行布局 */
  row?: number
  /** 内嵌标签模式：true 时隐藏外部 label，将 label 显示在 placeholder 中 */
  inlineLabel?: boolean
  /** 是否禁用 */
  disabled?: boolean
}

// ── 底部标签页字段 ──
export type TabFieldType = 'select' | 'input' | 'number' | 'date' | 'textarea'

export interface TabField {
  /** 字段唯一 key */
  key: string
  /** 字段标签 */
  label: string
  /** 字段类型 */
  type: TabFieldType
  /** 占位文本 */
  placeholder?: string
  /** 是否禁用 */
  disabled?: boolean
  /** 下拉选项（type=select 时使用） */
  options?: BasicInfoFieldOption[]
  /** 右侧按钮文本（如 "全清"、"···"、"+Q"） */
  suffixBtn?: string
  /** 按钮是否为警告样式（红色） */
  suffixBtnDanger?: boolean
  /** 数字精度（type=number 时使用） */
  precision?: number
  /** 最小值（type=number 时使用） */
  min?: number
  /** 最大值（type=number 时使用） */
  max?: number
  /** 字段宽度（px） */
  width?: number
  /** 是否只读 */
  readonly?: boolean
  /** 搜索按钮文本（如 "+Q"） */
  searchBtn?: string
  /** 是否内联标签样式 */
  inlineLabel?: boolean
  /** 加载状态 */
  loading?: boolean
}

export interface BillTabConfig {
  /** 标签页 key */
  key: string
  /** 标签页标题 */
  tab: string
  /** 标签页字段列表 */
  fields: TabField[]
}

// ── 摘要面板 ──
export interface SummaryRow {
  /** 行标签 */
  label: string
  /** 行值（会被 toString 显示） */
  value: string | number
  /** 是否在此行上方加分隔线 */
  divider?: boolean
  /** 是否显示右侧 "···" 按钮 */
  showMore?: boolean
  /** 摘要面板状态角标文本（如 "待结算"），仅首行显示 */
  statusLabel?: string
}

// ── 页脚操作栏 ──
export interface BillFooterConfig {
  /** 金额标签，如 "本单金额" */
  amountLabel?: string
  /** 金额值（格式化后的字符串，如 "¥1,234.56"） */
  amountValue: string
  /** 是否显示为红色大字 */
  amountHighlight?: boolean
  /** 主按钮文本，如 "提交" */
  primaryBtnText?: string
  /** 主按钮是否为审核样式（绿色） */
  primaryBtnAudit?: boolean
  /** 草稿按钮文本，如 "保存草稿" */
  draftBtnText?: string
  /** 草稿快捷键提示 */
  draftShortcut?: string
  /** 主按钮快捷键提示 */
  primaryShortcut?: string
  /** 是否正在提交中 */
  saving?: boolean
}
