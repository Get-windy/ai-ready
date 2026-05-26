/**
 * 工作流连线类型定义
 */

import type { NodePosition } from './canvas'

/** 连线类型 */
export type ConnectionType = 'bezier' | 'straight' | 'step'

/** 连线样式配置 */
export interface ConnectionStyle {
  /** 连线颜色 */
  color: string
  /** 连线宽度 */
  width: number
  /** 连线类型 */
  type: ConnectionType
  /** 是否显示箭头 */
  showArrow: boolean
  /** 箭头大小 */
  arrowSize: number
  /** 选中时颜色 */
  selectedColor: string
  /** 虚线样式（如果需要） */
  strokeDasharray?: string
}

/** 连线配置 */
export interface ConnectionConfig {
  /** 连线ID */
  id: string
  /** 源节点ID */
  sourceNodeId: string
  /** 目标节点ID */
  targetNodeId: string
  /** 源端口ID */
  sourcePortId: string
  /** 目标端口ID */
  targetPortId: string
  /** 连线样式 */
  style?: Partial<ConnectionStyle>
  /** 连线标签 */
  label?: string
  /** 自定义数据 */
  data?: Record<string, any>
}

/** 连线实例 */
export interface Connection extends ConnectionConfig {
  /** 源节点位置 */
  sourcePosition: NodePosition
  /** 目标节点位置 */
  targetPosition: NodePosition
  /** 是否选中 */
  selected: boolean
}

/** 连线点配置（用于贝塞尔曲线控制点） */
export interface ConnectionPoint {
  x: number
  y: number
}

/** 连线事件 */
export interface ConnectionEvents {
  /** 连线点击 */
  click: (connectionId: string) => void
  /** 连线右键点击 */
  contextMenu: (connectionId: string, event: MouseEvent) => void
  /** 连线鼠标按下 */
  mouseDown: (connectionId: string, event: MouseEvent) => void
}

/** 默认连线样式配置 */
export const DEFAULT_CONNECTION_STYLE: ConnectionStyle = {
  color: '#999999',
  width: 2,
  type: 'bezier',
  showArrow: true,
  arrowSize: 8,
  selectedColor: '#1890ff'
}

/** 连线类型标签 */
export const CONNECTION_TYPE_LABELS: Record<ConnectionType, string> = {
  bezier: '贝塞尔曲线',
  straight: '直线',
  step: '折线'
}