/**
 * 工作流画布类型定义
 */

import type { Ref } from 'vue'

/** 画布状态 */
export interface CanvasState {
  /** 缩放比例 */
  scale: number
  /** 平移偏移量 */
  offsetX: number
  offsetY: number
  /** 是否正在拖拽画布 */
  isPanning: boolean
  /** 拖拽起始位置 */
  panStartX: number
  panStartY: number
}

/** 背景网格配置 */
export interface GridConfig {
  /** 是否显示网格 */
  show: boolean
  /** 网格大小（像素） */
  size: number
  /** 网格线条颜色 */
  color: string
  /** 次级网格大小（像素） */
  subSize: number
  /** 次级网格线条颜色 */
  subColor: string
}

/** 画布配置 */
export interface CanvasConfig {
  /** 最小缩放比例 */
  minScale: number
  /** 最大缩放比例 */
  maxScale: number
  /** 缩放步长 */
  scaleStep: number
  /** 网格配置 */
  grid: GridConfig
}

/** 节点位置 */
export interface NodePosition {
  x: number
  y: number
}

/** 画布尺寸 */
export interface CanvasSize {
  width: number
  height: number
}

/** 画布引用 */
export interface CanvasRef {
  /** 画布DOM元素 */
  container: HTMLElement | null
  /** SVG元素 */
  svg: SVGSVGElement | null
}