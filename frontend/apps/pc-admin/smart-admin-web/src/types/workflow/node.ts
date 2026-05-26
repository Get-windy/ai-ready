/**
 * 工作流节点类型定义
 */

import type { NodePosition } from './canvas'

/** 节点类型 */
export type NodeType =
  | 'start' // 开始节点
  | 'end' // 结束节点
  | 'task' // 任务节点
  | 'condition' // 条件节点
  | 'parallel' // 并行节点
  | 'subprocess' // 子流程节点
  | 'gateway' // 网关节点
  | 'countersign' // 会签节点
  | 'or-sign' // 或签节点

/** 节点状态 */
export type NodeStatus =
  | 'idle' // 空闲
  | 'running' // 运行中
  | 'completed' // 已完成
  | 'error' // 错误
  | 'skipped' // 已跳过

/** 节点样式配置 */
export interface NodeStyle {
  /** 背景颜色 */
  backgroundColor: string
  /** 边框颜色 */
  borderColor: string
  /** 边框宽度 */
  borderWidth: number
  /** 文字颜色 */
  textColor: string
  /** 宽度 */
  width: number
  /** 高度 */
  height: number
  /** 圆角半径 */
  borderRadius: number
}

/** 节点配置 */
export interface NodeConfig {
  /** 节点类型 */
  type: NodeType
  /** 节点ID */
  id: string
  /** 节点名称 */
  name: string
  /** 节点描述 */
  description?: string
  /** 节点图标 */
  icon?: string
  /** 自定义样式 */
  style?: Partial<NodeStyle>
  /** 是否可选中 */
  selectable: boolean
  /** 是否可拖拽 */
  draggable: boolean
  /** 自定义数据 */
  data?: Record<string, any>
}

/** 节点实例 */
export interface FlowNode extends NodeConfig {
  /** 节点位置 */
  position: NodePosition
  /** 节点状态 */
  status: NodeStatus
  /** 是否选中 */
  selected: boolean
  /** 输入端口 */
  inputs: Port[]
  /** 输出端口 */
  outputs: Port[]
}

/** 端口方向 */
export type PortDirection = 'input' | 'output'

/** 端口配置 */
export interface Port {
  /** 端口ID */
  id: string
  /** 端口方向 */
  direction: PortDirection
  /** 端口名称 */
  name: string
  /** 端口位置（相对于节点） */
  position: {
    x: number
    y: number
  }
}

/** 默认节点样式配置 */
export const DEFAULT_NODE_STYLES: Record<NodeType, NodeStyle> = {
  start: {
    backgroundColor: '#52c41a',
    borderColor: '#389e0d',
    borderWidth: 2,
    textColor: '#ffffff',
    width: 100,
    height: 50,
    borderRadius: 25
  },
  end: {
    backgroundColor: '#f5222d',
    borderColor: '#cf1322',
    borderWidth: 2,
    textColor: '#ffffff',
    width: 100,
    height: 50,
    borderRadius: 25
  },
  task: {
    backgroundColor: '#ffffff',
    borderColor: '#1890ff',
    borderWidth: 2,
    textColor: '#000000',
    width: 120,
    height: 60,
    borderRadius: 4
  },
  condition: {
    backgroundColor: '#ffffff',
    borderColor: '#fa8c16',
    borderWidth: 2,
    textColor: '#000000',
    width: 120,
    height: 80,
    borderRadius: 4
  },
  parallel: {
    backgroundColor: '#ffffff',
    borderColor: '#722ed1',
    borderWidth: 2,
    textColor: '#000000',
    width: 120,
    height: 60,
    borderRadius: 4
  },
  subprocess: {
    backgroundColor: '#ffffff',
    borderColor: '#eb2f96',
    borderWidth: 2,
    textColor: '#000000',
    width: 140,
    height: 70,
    borderRadius: 4
  },
  gateway: {
    backgroundColor: '#ffffff',
    borderColor: '#13c2c2',
    borderWidth: 2,
    textColor: '#000000',
    width: 80,
    height: 80,
    borderRadius: 4
  },
  countersign: {
    backgroundColor: '#ffffff',
    borderColor: '#faad14',
    borderWidth: 2,
    textColor: '#000000',
    width: 130,
    height: 65,
    borderRadius: 4
  },
  'or-sign': {
    backgroundColor: '#ffffff',
    borderColor: '#fa541c',
    borderWidth: 2,
    textColor: '#000000',
    width: 130,
    height: 65,
    borderRadius: 4
  }
}