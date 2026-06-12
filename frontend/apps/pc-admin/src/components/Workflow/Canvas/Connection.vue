<template>
  <g
    class="flow-connection"
    :class="{
      'flow-connection-selected': connection.selected
    }"
    @click="handleClick"
    @contextmenu="handleContextMenu"
    @mousedown="handleMouseDown"
  >
    <!-- 连线路径 -->
    <path
      :d="connectionPath"
      :stroke="strokeColor"
      :stroke-width="strokeWidth"
      fill="none"
      :stroke-dasharray="strokeDasharray"
      class="connection-path"
    />

    <!-- 连线箭头 -->
    <defs v-if="connectionStyle.showArrow">
      <marker
        :id="markerId"
        :markerWidth="connectionStyle.arrowSize"
        :markerHeight="connectionStyle.arrowSize"
        :refX="connectionStyle.arrowSize"
        :refY="connectionStyle.arrowSize / 2"
        orient="auto"
      >
        <path
          :d="arrowPath"
          :fill="strokeColor"
        />
      </marker>
    </defs>

    <!-- 选中状态高亮 -->
    <path
      v-if="connection.selected"
      :d="connectionPath"
      :stroke="connectionStyle.selectedColor"
      :stroke-width="connectionStyle.width + 4"
      fill="none"
      class="connection-highlight"
      style="opacity: 0.3"
    />

    <!-- 连线标签 -->
    <text
      v-if="connection.label"
      :x="midPoint.x"
      :y="midPoint.y - 5"
      text-anchor="middle"
      :font-size="11"
      fill="#666"
      class="connection-label"
    >
      {{ connection.label }}
    </text>
  </g>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Connection, ConnectionStyle } from '@/types/workflow/connection'
import { DEFAULT_CONNECTION_STYLE } from '@/types/workflow/connection'

/**
 * Props
 */
interface Props {
  /** 连线数据 */
  connection: Connection
}

const props = defineProps<Props>()

/**
 * Emits
 */
const emit = defineEmits<{
  /** 连线点击 */
  click: [connectionId: string]
  /** 连线右键点击 */
  contextMenu: [connectionId: string, event: MouseEvent]
  /** 连线鼠标按下 */
  mouseDown: [connectionId: string, event: MouseEvent]
}>()

/**
 * 计算属性
 */

/** 连线样式 */
const connectionStyle = computed<ConnectionStyle>(() => {
  return {
    ...DEFAULT_CONNECTION_STYLE,
    ...props.connection.style
  }
})

/** 连线颜色 */
const strokeColor = computed(() => {
  return props.connection.selected
    ? connectionStyle.value.selectedColor
    : connectionStyle.value.color
})

/** 连线宽度 */
const strokeWidth = computed(() => {
  return props.connection.selected
    ? connectionStyle.value.width + 2
    : connectionStyle.value.width
})

/** 虚线样式 */
const strokeDasharray = computed(() => {
  return connectionStyle.value.strokeDasharray || ''
})

/** 连线路径 */
const connectionPath = computed(() => {
  const source = props.connection.sourcePosition
  const target = props.connection.targetPosition

  switch (connectionStyle.value.type) {
    case 'straight':
      return createStraightPath(source, target)
    case 'step':
      return createStepPath(source, target)
    case 'bezier':
    default:
      return createBezierPath(source, target)
  }
})

/** 连线箭头路径 */
const arrowPath = computed(() => {
  const size = connectionStyle.value.arrowSize
  return `M 0 0 L ${size} ${size / 2} L 0 ${size} Z`
})

/** 箭头标记ID */
const markerId = computed(() => `arrow-${props.connection.id}`)

/** 连线中点（用于显示标签） */
const midPoint = computed(() => {
  const source = props.connection.sourcePosition
  const target = props.connection.targetPosition

  const controlPoints = getBezierControlPoints(source, target)
  const t = 0.5

  const x =
    Math.pow(1 - t, 3) * source.x +
    3 * Math.pow(1 - t, 2) * t * controlPoints.cp1.x +
    3 * (1 - t) * Math.pow(t, 2) * controlPoints.cp2.x +
    Math.pow(t, 3) * target.x

  const y =
    Math.pow(1 - t, 3) * source.y +
    3 * Math.pow(1 - t, 2) * t * controlPoints.cp1.y +
    3 * (1 - t) * Math.pow(t, 2) * controlPoints.cp2.y +
    Math.pow(t, 3) * target.y

  return { x, y }
})

/**
 * 方法
 */

/**
 * 创建直线路径
 */
function createStraightPath(
  source: { x: number; y: number },
  target: { x: number; y: number }
): string {
  return `M ${source.x} ${source.y} L ${target.x} ${target.y}`
}

/**
 * 创建折线路径
 */
function createStepPath(
  source: { x: number; y: number },
  target: { x: number; y: number }
): string {
  const midX = (source.x + target.x) / 2
  return `M ${source.x} ${source.y} L ${midX} ${source.y} L ${midX} ${target.y} L ${target.x} ${target.y}`
}

/**
 * 创建贝塞尔曲线路径
 */
function createBezierPath(
  source: { x: number; y: number },
  target: { x: number; y: number }
): string {
  const { cp1, cp2 } = getBezierControlPoints(source, target)
  return `M ${source.x} ${source.y} C ${cp1.x} ${cp1.y}, ${cp2.x} ${cp2.y}, ${target.x} ${target.y}`
}

/**
 * 获取贝塞尔曲线控制点
 */
function getBezierControlPoints(
  source: { x: number; y: number },
  target: { x: number; y: number }
): { cp1: { x: number; y: number }; cp2: { x: number; y: number } } {
  const dx = Math.abs(target.x - source.x)
  const curvature = Math.max(dx * 0.5, 50)

  return {
    cp1: {
      x: source.x + curvature,
      y: source.y
    },
    cp2: {
      x: target.x - curvature,
      y: target.y
    }
  }
}

/**
 * 事件处理
 */

/**
 * 处理连线点击
 */
function handleClick(event: MouseEvent): void {
  event.stopPropagation()
  emit('click', props.connection.id)
}

/**
 * 处理连线右键点击
 */
function handleContextMenu(event: MouseEvent): void {
  event.stopPropagation()
  event.preventDefault()
  emit('contextMenu', props.connection.id, event)
}

/**
 * 处理连线鼠标按下
 */
function handleMouseDown(event: MouseEvent): void {
  event.stopPropagation()
  emit('mouseDown', props.connection.id, event)
}
</script>

<style scoped>
.flow-connection {
  cursor: pointer;
  transition: opacity 0.2s;
}

.flow-connection:hover {
  opacity: 0.8;
}

.flow-connection-selected {
  filter: drop-shadow(0 0 4px rgba(24, 144, 255, 0.5));
}

.connection-path {
  pointer-events: stroke;
}

.connection-highlight {
  pointer-events: none;
}

.connection-label {
  pointer-events: none;
  font-weight: 500;
  user-select: none;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.8);
}
</style>