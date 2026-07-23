import type { CSSProperties } from 'vue'

/**
 * ARReportPage / ARStatCards 共享类型定义
 */

/** 统计卡片项 */
export interface StatCardItem {
  /** 指标名称 */
  label: string
  /** 指标值（数字按 precision 格式化，字符串原样显示） */
  value: number | string
  /** 小数位（仅数字生效），默认不格式化 */
  precision?: number
  /** 前缀（如 ¥） */
  prefix?: string
  /** 后缀（如 元/单/%） */
  suffix?: string
  /** 同比变化（百分比数值，正绿负红，可选） */
  trend?: number
  /** 自定义值样式 */
  valueStyle?: CSSProperties
}

/** 查询字段配置 */
export interface ReportQueryField {
  /** 字段键（同时作为请求参数名，可用 paramKey 覆盖） */
  key: string
  /** 字段类型 */
  type: 'input' | 'select' | 'date-range'
  /** 标签（select 显示为前缀标签，input 显示在 placeholder 前缀） */
  label?: string
  /** 占位提示 */
  placeholder?: string
  /** select 选项 */
  options?: { label: string; value: string | number }[]
  /** 控件宽度 px，默认 input 200 / select 180 / date-range 240 */
  width?: number
  /** 请求参数名覆盖（默认用 key） */
  paramKey?: string
  /** date-range 起始参数名，默认 startDate */
  startKey?: string
  /** date-range 截止参数名，默认 endDate */
  endKey?: string
  /** 是否可清空，默认 true */
  allowClear?: boolean
}

/** fetcher 归一化后的结果 */
export interface ReportFetchResult<T = any> {
  list: T[]
  total: number
  /** 原始响应（含 summary 等额外字段） */
  raw?: any
}

/** 数据请求函数：params 含分页参数 + 查询字段参数 */
export type ReportFetcher = (params: Record<string, any>) => Promise<any>

/** 导出处理上下文 */
export interface ReportExportContext {
  rows: any[]
  columns: any[]
  params: Record<string, any>
}
