/**
 * OrderFormPage 通用单据全屏表单 - 类型定义
 */
import type { VNode } from 'vue'

/** 表单页面模式 */
export type FormMode = 'create' | 'edit' | 'view'

/** 字段类型 */
export type FieldType =
  | 'input'
  | 'select'
  | 'date'
  | 'number'
  | 'textarea'
  | 'slot'

/** 头部字段配置 */
export interface HeaderField {
  /** 字段名（对应 formData 的 key） */
  name: string
  /** 标签文本 */
  label: string
  /** 字段类型 */
  type: FieldType
  /** 占位文本 */
  placeholder?: string
  /** 是否必填 */
  required?: boolean
  /** 下拉选项（type=select 时使用） */
  options?: { label: string; value: string | number }[]
  /** 选项数据源 ref 名称（从 optionsApi 动态加载时使用） */
  optionsRef?: string
  /** 是否支持搜索 */
  showSearch?: boolean
  /** 列宽 span (默认6) */
  span?: number
  /** 自定义插槽名 */
  slotName?: string
  /** 数字字段精度 */
  precision?: number
  /** 数字最小值 */
  min?: number
  /** 数字最大值 */
  max?: number
}

/** 明细表格列配置 */
export interface DetailColumn {
  field: string
  title: string
  width?: number
  /** 固定列 */
  fixed?: 'left' | 'right'
  /** 自定义插槽名 */
  slotName?: string
  /** 是否可编辑 */
  editable?: boolean
}

/** 底部标签页配置 */
export interface TabConfig {
  key: string
  tab: string
  fields: HeaderField[]
}

/** 合计项配置 */
export interface SummaryItem {
  label: string
  /** 计算表达式或字段名 */
  valueKey?: string
  /** 自定义计算函数 */
  compute?: (products: any[]) => number
  /** 是否高亮显示 */
  highlight?: boolean
  /** 格式化函数 */
  format?: (val: number) => string
}

/** 表单 CRUD API 接口 */
export interface FormApi {
  create: (data: any) => Promise<any>
  update?: (id: number, data: any) => Promise<any>
  getById?: (id: number) => Promise<any>
  submit?: (id: number) => Promise<any>
}

/** OrderFormPage Props */
export interface OrderFormPageProps {
  /** 页面标题 */
  title: string
  /** 单据编号前缀（如 'SO', 'PO'） */
  billPrefix?: string
  /** 表单模式 */
  mode?: FormMode
  /** 头部字段配置 */
  headerFields: HeaderField[]
  /** 明细表格列配置 */
  detailColumns: DetailColumn[]
  /** 底部标签页 */
  tabs?: TabConfig[]
  /** 合计项 */
  summaryItems?: SummaryItem[]
  /** CRUD API */
  api: FormApi
  /** 提交后跳转路径 */
  redirectPath?: string
  /** 是否显示税额相关列 */
  showTax?: boolean
}
