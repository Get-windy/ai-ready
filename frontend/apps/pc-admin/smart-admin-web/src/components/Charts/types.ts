/**
 * 图表类型定义
 */

// 图表类型
export type ChartType = 'bar' | 'line' | 'pie' | 'radar'

// 图表数据点
export interface ChartDataPoint {
  name: string
  value: number
  category?: string
  color?: string
  extra?: Record<string, any>
}

// 图表系列
export interface ChartSeries {
  name: string
  data: ChartDataPoint[]
  color?: string
  type?: ChartType
}

// 图表配置
export interface ChartConfig {
  // 标题
  title?: string
  subTitle?: string
  
  // 尺寸
  width?: number
  height?: number
  
  // 主题
  theme?: 'default' | 'dark' | 'vintage' | 'colorful'
  
  // 颜色
  colors?: string[]
  
  // 动画
  animation?: boolean
  animationDuration?: number
  
  // 网格
  grid?: {
    top?: number
    right?: number
    bottom?: number
    left?: number
  }
  
  // 提示框
  tooltip?: {
    show?: boolean
    trigger?: 'item' | 'axis' | 'none'
    formatter?: (params: any) => string
  }
  
  // 图例
  legend?: {
    show?: boolean
    position?: 'top' | 'bottom' | 'left' | 'right'
  }
  
  // 坐标轴
  xAxis?: {
    show?: boolean
    type?: 'category' | 'value' | 'time'
    name?: string
    data?: string[]
  }
  yAxis?: {
    show?: boolean
    type?: 'category' | 'value'
    name?: string
  }
  
  // 数据缩放
  dataZoom?: {
    show?: boolean
    type?: 'slider' | 'inside'
    start?: number
    end?: number
  }
  
  // 工具箱
  toolbox?: {
    show?: boolean
    features?: {
      dataZoom?: boolean
      dataView?: boolean
      magicType?: boolean
      restore?: boolean
      saveAsImage?: boolean
    }
  }
}

// 图表交互事件
export interface ChartEvents {
  onClick?: (params: ChartDataPoint) => void
  onHover?: (params: ChartDataPoint) => void
  onSelect?: (params: ChartDataPoint[]) => void
  onZoom?: (start: number, end: number) => void
}

// 图表筛选器
export interface ChartFilter {
  field: string
  operator: 'eq' | 'neq' | 'gt' | 'gte' | 'lt' | 'lte' | 'in' | 'between'
  value: any
}

// 图表导出选项
export interface ExportOptions {
  type: 'png' | 'svg' | 'csv' | 'json'
  filename?: string
  backgroundColor?: string
  pixelRatio?: number
}

// 预设主题颜色
export const ThemeColors = {
  default: ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4'],
  dark: ['#4992ff', '#7cffb2', '#fddd60', '#ff6e76', '#58d9f9', '#05c091', '#ff8a45', '#8d48e3'],
  vintage: ['#d87c7c', '#919e8b', '#d7ab82', '#6e7074', '#61a0a8', '#efa18d', '#787464', '#cc7e63'],
  colorful: ['#ff6b6b', '#4ecdc4', '#45b7d1', '#f9ca24', '#f0932b', '#eb4d4b', '#6ab04c', '#c7ecee']
}
