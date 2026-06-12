<template>
  <g
    :transform="`translate(${node.position.x}, ${node.position.y})`"
    class="flow-node"
    :class="{
      'flow-node-selected': node.selected,
      'flow-node-draggable': node.draggable
    }"
    @click="handleClick"
    @mousedown="handleMouseDown"
  >
    <!-- 节点主体 -->
    <g
      class="flow-node-body"
      :style="nodeStyle as any"
    >
      <!-- 根据节点类型渲染不同形状 -->
      <template v-if="node.type === 'start' || node.type === 'end'">
        <!-- 椭圆形 -->
        <ellipse
          :cx="nodeStyle.width / 2"
          :cy="nodeStyle.height / 2"
          :rx="nodeStyle.width / 2"
          :ry="nodeStyle.height / 2"
          :fill="nodeStyle.backgroundColor"
          :stroke="nodeStyle.borderColor"
          :stroke-width="nodeStyle.borderWidth"
        />
      </template>
      <template v-else-if="node.type === 'gateway'">
        <!-- 菱形 -->
        <polygon
          :points="diamondPoints"
          :fill="nodeStyle.backgroundColor"
          :stroke="nodeStyle.borderColor"
          :stroke-width="nodeStyle.borderWidth"
        />
      </template>
      <template v-else-if="node.type === 'condition'">
        <!-- 菱形（条件节点） -->
        <polygon
          :points="diamondPoints"
          :fill="nodeStyle.backgroundColor"
          :stroke="nodeStyle.borderColor"
          :stroke-width="nodeStyle.borderWidth"
        />
      </template>
      <template v-else-if="node.type === 'countersign' || node.type === 'or-sign'">
        <!-- 会签/或签节点（矩形 + 边框装饰） -->
        <rect
          :x="0"
          :y="0"
          :width="nodeStyle.width"
          :height="nodeStyle.height"
          :rx="nodeStyle.borderRadius"
          :ry="nodeStyle.borderRadius"
          :fill="nodeStyle.backgroundColor"
          :stroke="nodeStyle.borderColor"
          :stroke-width="nodeStyle.borderWidth"
        />
        <!-- 左侧装饰条 -->
        <rect
          :x="4"
          :y="4"
          :width="6"
          :height="nodeStyle.height - 8"
          :rx="2"
          :ry="2"
          :fill="nodeStyle.borderColor"
        />
      </template>
      <template v-else>
        <!-- 矩形 -->
        <rect
          :x="0"
          :y="0"
          :width="nodeStyle.width"
          :height="nodeStyle.height"
          :rx="nodeStyle.borderRadius"
          :ry="nodeStyle.borderRadius"
          :fill="nodeStyle.backgroundColor"
          :stroke="nodeStyle.borderColor"
          :stroke-width="nodeStyle.borderWidth"
        />
      </template>

      <!-- 选中状态边框 -->
      <g v-if="node.selected">
        <rect
          :x="-4"
          :y="-4"
          :width="nodeStyle.width + 8"
          :height="nodeStyle.height + 8"
          :rx="nodeStyle.borderRadius + 2"
          :ry="nodeStyle.borderRadius + 2"
          fill="none"
          stroke="#1890ff"
          stroke-width="2"
          stroke-dasharray="5,3"
        />
        <!-- 角点控制手柄 -->
        <circle
          v-for="point in cornerPoints"
          :key="point.id"
          :cx="point.x"
          :cy="point.y"
          r="4"
          fill="#1890ff"
          stroke="#ffffff"
          stroke-width="2"
          class="resize-handle"
        />
      </g>

      <!-- 节点内容 -->
      <g
        class="flow-node-content"
        :transform="`translate(${nodeStyle.width / 2}, ${nodeStyle.height / 2})`"
      >
        <!-- 图标 -->
        <text
          v-if="node.icon"
          :x="0"
          :y="-nodeStyle.height / 4"
          text-anchor="middle"
          :font-size="20"
          fill="#000000"
        >
          {{ node.icon }}
        </text>

        <!-- 节点名称 -->
        <text
          :x="0"
          :y="node.icon ? nodeStyle.height / 6 : 0"
          text-anchor="middle"
          dominant-baseline="middle"
          :font-size="12"
          :fill="nodeStyle.textColor"
          class="flow-node-title"
        >
          {{ truncatedName }}
        </text>
      </g>
    </g>

    <!-- 输入端口 -->
    <g
      v-for="port in node.inputs"
      :key="port.id"
      class="flow-port flow-port-input"
      :transform="`translate(${port.position.x}, ${port.position.y})`"
    >
      <circle
        r="6"
        fill="#1890ff"
        stroke="#ffffff"
        stroke-width="2"
        class="port-dot"
      />
      <text
        :x="-10"
        :y="4"
        text-anchor="end"
        :font-size="10"
        fill="#666"
      >
        {{ port.name }}
      </text>
    </g>

    <!-- 输出端口 -->
    <g
      v-for="port in node.outputs"
      :key="port.id"
      class="flow-port flow-port-output"
      :transform="`translate(${port.position.x}, ${port.position.y})`"
    >
      <circle
        r="6"
        fill="#52c41a"
        stroke="#ffffff"
        stroke-width="2"
        class="port-dot"
      />
      <text
        :x="10"
        :y="4"
        text-anchor="start"
        :font-size="10"
        fill="#666"
      >
        {{ port.name }}
      </text>
    </g>
  </g>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FlowNode, NodeStyle } from '@/types/workflow/node'
import { DEFAULT_NODE_STYLES } from '@/types/workflow/node'

/**
 * Props
 */
interface Props {
  /** 节点数据 */
  node: FlowNode
}

const props = defineProps<Props>()

/**
 * Emits
 */
const emit = defineEmits<{
  /** 节点点击 */
  click: [nodeId: string]
  /** 节点开始拖拽 */
  dragStart: [nodeId: string, event: MouseEvent]
  /** 节点拖拽中 */
  drag: [nodeId: string, event: MouseEvent]
  /** 节点拖拽结束 */
  dragEnd: [nodeId: string]
}>()

/**
 * 计算属性
 */

/** 节点样式 */
const nodeStyle = computed<NodeStyle>(() => {
  const defaultStyle = DEFAULT_NODE_STYLES[props.node.type]
  return {
    ...defaultStyle,
    ...props.node.style
  }
})

/** 截断的节点名称（最大8个字符） */
const truncatedName = computed(() => {
  const name = props.node.name
  return name.length > 8 ? name.substring(0, 8) + '...' : name
})

/** 菱形顶点（网关节点和条件节点） */
const diamondPoints = computed(() => {
  const width = nodeStyle.value.width
  const height = nodeStyle.value.height
  return [
    `${width / 2},${0}`, // 顶
    `${width},${height / 2}`, // 右
    `${width / 2},${height}`, // 底
    `${0},${height / 2}` // 左
  ].join(' ')
})

/** 角点坐标（用于调整大小） */
const cornerPoints = computed(() => {
  const width = nodeStyle.value.width
  const height = nodeStyle.value.height
  return [
    { id: 'tl', x: -4, y: -4 }, // 左上
    { id: 'tr', x: width + 4, y: -4 }, // 右上
    { id: 'bl', x: -4, y: height + 4 }, // 左下
    { id: 'br', x: width + 4, y: height + 4 } // 右下
  ]
})

/**
 * 事件处理
 */

/**
 * 处理节点点击
 */
function handleClick(event: MouseEvent): void {
  event.stopPropagation()
  if (props.node.selectable) {
    emit('click', props.node.id)
  }
}

/**
 * 处理鼠标按下（开始拖拽）
 */
function handleMouseDown(event: MouseEvent): void {
  event.stopPropagation()
  if (props.node.draggable && event.button === 0) {
    emit('dragStart', props.node.id, event)
  }
}
</script>

<style scoped>
.flow-node {
  cursor: pointer;
  transition: opacity 0.2s;
}

.flow-node:hover {
  opacity: 0.9;
}

.flow-node-selected {
  filter: drop-shadow(0 0 4px rgba(24, 144, 255, 0.5));
}

.flow-node-draggable {
  cursor: move;
}

.flow-node-body {
  transition: all 0.2s;
}

.flow-node-content {
  pointer-events: none;
}

.flow-node-title {
  font-weight: 500;
  user-select: none;
}

.flow-port {
  cursor: crosshair;
  opacity: 0;
  transition: opacity 0.2s;
}

.flow-node:hover .flow-port {
  opacity: 1;
}

.port-dot:hover {
  r: 8;
  transition: r 0.2s;
}

.resize-handle {
  cursor: nwse-resize;
}

.resize-handle:hover {
  r: 6;
}
</style>