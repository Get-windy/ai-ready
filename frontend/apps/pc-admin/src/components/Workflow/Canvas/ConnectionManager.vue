<template>
  <svg class="connection-layer">
    <!-- 已有的连线 -->
    <Connection
      v-for="connection in connections"
      :key="connection.id"
      :connection="connection"
      @click="handleConnectionClick"
      @context-menu="handleConnectionContextMenu"
      @mouse-down="handleConnectionMouseDown"
    />

    <!-- 正在创建的连线（拖拽中） -->
    <path
      v-if="isCreatingConnection"
      :d="tempConnectionPath"
      stroke="#1890ff"
      stroke-width="2"
      fill="none"
      stroke-dasharray="5,5"
      class="temp-connection"
    />
  </svg>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import Connection from './Connection.vue'
import type { Connection as ConnectionType } from '@/types/workflow/connection'
import type { NodePosition } from '@/types/workflow/canvas'

/**
 * Props
 */
interface Props {
  /** 连线列表 */
  connections: ConnectionType[]
  /** 是否正在创建连线 */
  isCreatingConnection: boolean
  /** 临时连线起点 */
  tempConnectionStart?: NodePosition
  /** 临时连线终点（鼠标位置） */
  tempConnectionEnd?: NodePosition
}

const props = defineProps<Props>()

/**
 * Emits
 */
const emit = defineEmits<{
  /** 连线点击 */
  connectionClick: [connectionId: string]
  /** 连线右键点击 */
  connectionContextMenu: [connectionId: string, event: MouseEvent]
  /** 连线删除 */
  connectionDelete: [connectionId: string]
  /** 连线样式配置 */
  connectionStyleConfig: [connectionId: string]
}>()

/**
 * 计算属性
 */

/** 临时连线路径 */
const tempConnectionPath = computed(() => {
  if (!props.isCreatingConnection || !props.tempConnectionStart || !props.tempConnectionEnd) {
    return ''
  }

  const start = props.tempConnectionStart
  const end = props.tempConnectionEnd

  // 使用贝塞尔曲线
  const dx = Math.abs(end.x - start.x)
  const curvature = Math.max(dx * 0.5, 50)

  const cp1 = {
    x: start.x + curvature,
    y: start.y
  }

  const cp2 = {
    x: end.x - curvature,
    y: end.y
  }

  return `M ${start.x} ${start.y} C ${cp1.x} ${cp1.y}, ${cp2.x} ${cp2.y}, ${end.x} ${end.y}`
})

/**
 * 方法
 */

/**
 * 处理连线点击
 */
function handleConnectionClick(connectionId: string): void {
  emit('connectionClick', connectionId)
}

/**
 * 处理连线右键点击
 */
function handleConnectionContextMenu(connectionId: string, event: MouseEvent): void {
  emit('connectionContextMenu', connectionId, event)
}

/**
 * 处理连线鼠标按下
 */
function handleConnectionMouseDown(connectionId: string, event: MouseEvent): void {
  // 如果是右键，可以显示上下文菜单
  if (event.button === 2) {
    emit('connectionContextMenu', connectionId, event)
  }
}
</script>

<style scoped>
.connection-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.connection-layer :deep(.flow-connection) {
  pointer-events: stroke;
}

.temp-connection {
  pointer-events: none;
  animation: dash 1s linear infinite;
}

@keyframes dash {
  to {
    stroke-dashoffset: -10;
  }
}
</style>