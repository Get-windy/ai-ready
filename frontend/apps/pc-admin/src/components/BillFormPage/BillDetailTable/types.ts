/**
 * BillDetailTable — 配置驱动的明细表格组件
 *
 * 根据 columnConfig 自动生成可编辑列，消除大量 slot 样板代码。
 * 支持：select / input / number / date 列类型 + 自定义插槽列 + 合计行。
 * 齿轮设置：列显示/隐藏、列宽调整、列顺序拖拽、前后冻结列。
 */

export type DetailColumnType = 'select' | 'input' | 'number' | 'date' | 'slot' | 'rowNo' | 'action' | 'checkbox' | 'button' | 'boolean'

export interface DetailColumnOption {
  label: string
  value: string | number
  /** 搜索匹配文本（默认使用 label） */
  searchText?: string
}

export interface DetailColumnConfig {
  /** 列 key，对应 record 字段名 */
  key: string
  /** 列标题 */
  title: string
  /** 列宽（px） */
  width?: number
  /** 是否固定 */
  fixed?: 'left' | 'right'
  /** 列类型：默认 input（原生单元格直接输入）；select/number/date/searchable 为其它原生类型；slot=自定义插槽，rowNo=行号，action=操作列；业务页确需非默认类型才改并在列定义处注释原因 */
  type?: DetailColumnType
  /** 是否必填（显示红色星号） */
  required?: boolean
  /** 占位文本 */
  placeholder?: string
  /** 下拉选项（type=select）：静态数组 [{value,label}]，或函数 (record)=>[{value,label}] 支持行级动态 options */
  options?: DetailColumnOption[] | ((record: any) => DetailColumnOption[])
  /** 行级动态 options 字段名：读 record[optionsField] 作为该行下拉选项 */
  optionsField?: string
  /** 数字精度（type=number） */
  precision?: number
  /** 最小值（type=number） */
  min?: number
  /** 最大值（type=number） */
  max?: number
  /** 日期格式（type=date） */
  format?: string
  /** 自定义插槽名（type=slot 时使用） */
  slotName?: string
  /** 是否仅查看 */
  readonly?: boolean
  /** 对齐方式 */
  align?: 'left' | 'center' | 'right'
  /** 商品名称列专用：是否显示扫描枪开关 */
  showScanToggle?: boolean
  /** 是否支持输入搜索（输入时弹出选项下拉，可键盘选择） */
  searchable?: boolean
  /** 按钮配置（type=button 时使用） */
  buttons?: Array<{
    label: string
    type?: 'primary' | 'link' | 'default'
    /** 是否为危险按钮（红色） */
    danger?: boolean
    onClick?: (record: any, index: number) => void
  }>
  /** 操作按钮（type=action 用）：数据驱动渲染；≤4 平铺撑大；>4 自动折叠（显示前 3 个高频按钮 + "更多"下拉），高频按钮放数组前部 */
  actionButtons?: Array<{
    key?: string
    label: string
    type?: 'primary' | 'link' | 'default'
    danger?: boolean
    onClick?: (record: any, index: number) => void
  }>
  /** 自定义格式化函数 */
  formatter?: (value: any, record: any) => string
  /** 是否可排序 */
  sortable?: boolean
  /** 自定义排序函数 */
  sorter?: (a: any, b: any) => number
  /** 列是否可隐藏 */
  hideable?: boolean
  /** 默认是否隐藏 */
  defaultHidden?: boolean
  /** 列标题提示 */
  tooltip?: string
  /** 自定义类名 */
  className?: string
  /** 自定义样式 */
  style?: Record<string, string>
}

/** 列设置项（运行时状态） */
export interface ColumnSetting {
  key: string
  title: string
  /** 显示名（可自定义，默认同 title） */
  displayName?: string
  visible: boolean
  width: number
  fixed: 'left' | 'right' | ''
}

export interface SummaryColumnDef {
  /** 对应列 key */
  key: string
  /** 合计值 */
  value: string | number
  /** 是否红色高亮 */
  highlight?: boolean
}
