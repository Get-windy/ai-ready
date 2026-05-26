<template>
  <div class="workflow-canvas-page">
    <a-card title="工作流画布示例">
      <template #extra>
        <a-space>
          <a-button @click="addStartNode">
            添加开始节点
          </a-button>
          <a-button @click="addEndNode">
            添加结束节点
          </a-button>
          <a-button @click="addTaskNode">
            添加任务节点
          </a-button>
          <a-button @click="addConditionNode">
            添加条件节点
          </a-button>
          <a-button @click="addCountersignNode">
            添加会签节点
          </a-button>
          <a-button @click="addOrSignNode">
            添加或签节点
          </a-button>
          <a-button @click="clearNodes">
            清空节点
          </a-button>
        </a-space>
      </template>

      <div class="workflow-layout">
        <!-- 左侧节点库 -->
        <div class="node-library-panel">
          <NodeLibrary
            @drag-start="handleLibraryNodeDragStart"
            @drag-end="handleLibraryNodeDragEnd"
          />
        </div>

        <!-- 右侧画布区域 -->
        <div class="canvas-wrapper">
          <div
            class="canvas-container"
            @dragover="handleCanvasDragOver"
            @drop="handleCanvasDrop"
          >
            <CanvasBoard
              ref="canvasRef"
              :config="canvasConfig"
              @state-change="handleCanvasStateChange"
              @canvas-click="handleCanvasClick"
            >
              <template #connections>
                <ConnectionManager
                  :connections="connections"
                  :is-creating-connection="isCreatingConnection"
                  :temp-connection-start="tempConnectionStart"
                  :temp-connection-end="tempConnectionEnd"
                  @connection-click="handleConnectionClick"
                  @connection-context-menu="handleConnectionContextMenu"
                />
              </template>

              <template #nodes>
                <FlowNode
                  v-for="node in nodes"
                  :key="node.id"
                  :node="node"
                  @click="handleNodeClick"
                  @drag-start="handleNodeDragStart"
                  @drag="handleNodeDrag"
                  @drag-end="handleNodeDragEnd"
                />
              </template>
            </CanvasBoard>
          </div>
        </div>
      </div>

      <a-divider />

      <div class="info-panel">
        <a-descriptions
          :column="3"
          bordered
          size="small"
        >
          <a-descriptions-item label="画布缩放">
            {{ Math.round(canvasState.scale * 100) }}%
          </a-descriptions-item>
          <a-descriptions-item label="画布偏移">
            ({{ Math.round(canvasState.offsetX) }}, {{ Math.round(canvasState.offsetY) }})
          </a-descriptions-item>
          <a-descriptions-item label="节点数量">
            {{ nodes.length }}
          </a-descriptions-item>
          <a-descriptions-item label="选中节点">
            {{ selectedNodeId || '无' }}
          </a-descriptions-item>
          <a-descriptions-item label="选中连线">
            {{ selectedConnectionId || '无' }}
          </a-descriptions-item>
          <a-descriptions-item label="连线数量">
            {{ connections.length }}
          </a-descriptions-item>
          <a-descriptions-item label="操作提示">
            滚轮缩放 / 空格+拖动平移 / 点击选中 / 拖拽端口创建连线 / 从节点库拖拽添加节点
          </a-descriptions-item>
          <a-descriptions-item label="快捷键">
            Ctrl + Z: 撤销 | Delete: 删除选中节点
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { CanvasBoard, FlowNode, ConnectionManager, NodeLibrary } from '@/components/Workflow/Canvas'
import type { CanvasState, CanvasConfig, NodePosition } from '@/types/workflow/canvas'
import type { FlowNode as FlowNodeType, NodeType } from '@/types/workflow/node'
import type { Connection, ConnectionType } from '@/types/workflow/connection'
import { DEFAULT_NODE_STYLES } from '@/types/workflow/node'

/**
 * 状态
 */
const canvasRef = ref<InstanceType<typeof CanvasBoard>>()
const nodes = ref<FlowNodeType[]>([])
const connections = ref<Connection[]>([])
const selectedNodeId = ref<string>('')
const selectedConnectionId = ref<string>('')

/** 拖拽相关状态 */
const draggingNodeId = ref<string>('')
const dragOffset = ref<NodePosition>({ x: 0, y: 0 })

/** 连线创建相关状态 */
const isCreatingConnection = ref(false)
const tempConnectionStart = ref<NodePosition | null>(null)
const tempConnectionEnd = ref<NodePosition | null>(null)
const creatingConnectionSource = ref<{ nodeId: string; portId: string } | null>(null)

/** 画布状态 */
const canvasState = reactive<CanvasState>({
  scale: 1,
  offsetX: 0,
  offsetY: 0,
  isPanning: false,
  panStartX: 0,
  panStartY: 0
})

/** 画布配置 */
const canvasConfig: Partial<CanvasConfig> = {
  minScale: 0.2,
  maxScale: 3,
  scaleStep: 0.1,
  grid: {
    show: true,
    size: 20,
    color: '#d9d9d9',
    subSize: 5,
    subColor: '#f0f0f0'
  }
}

/**
 * 节点计数器
 */
let nodeIdCounter = 0

/**
 * 方法
 */

/**
 * 生成节点ID
 */
function generateNodeId(): string {
  return `node-${Date.now()}-${++nodeIdCounter}`
}

/**
 * 创建基础节点
 */
function createBaseNode(
  type: FlowNodeType['type'],
  name: string,
  icon?: string,
  x: number = 100 + Math.random() * 400,
  y: number = 100 + Math.random() * 300
): FlowNodeType {
  const nodeId = generateNodeId()

  return {
    type,
    id: nodeId,
    name,
    description: `${name}节点的描述`,
    icon,
    selectable: true,
    draggable: true,
    position: { x, y },
    status: 'idle',
    selected: false,
    inputs: type !== 'start' ? [
      {
        id: `${nodeId}-input`,
        direction: 'input',
        name: '输入',
        position: { x: 0, y: 30 }
      }
    ] : [],
    outputs: type !== 'end' ? [
      {
        id: `${nodeId}-output`,
        direction: 'output',
        name: '输出',
        position: { x: 120, y: 30 }
      }
    ] : []
  }
}

/**
 * 添加开始节点
 */
function addStartNode(): void {
  const node = createBaseNode('start', '开始', '▶️', 100, 150)
  nodes.value.push(node)
  message.success('已添加开始节点')
}

/**
 * 添加结束节点
 */
function addEndNode(): void {
  const node = createBaseNode('end', '结束', '⏹️', 600, 150)
  nodes.value.push(node)
  message.success('已添加结束节点')
}

/**
 * 添加任务节点
 */
function addTaskNode(): void {
  const node = createBaseNode('task', '任务节点', '📝')
  nodes.value.push(node)
  message.success('已添加任务节点')
}

/**
 * 添加条件节点
 */
function addConditionNode(): void {
  const node = createBaseNode('condition', '条件节点', '❓')
  nodes.value.push(node)
  message.success('已添加条件节点')
}

/**
 * 添加会签节点
 */
function addCountersignNode(): void {
  const node = createBaseNode('countersign', '会签节点', '✍️')
  nodes.value.push(node)
  message.success('已添加会签节点')
}

/**
 * 添加或签节点
 */
function addOrSignNode(): void {
  const node = createBaseNode('or-sign', '或签节点', '👥')
  nodes.value.push(node)
  message.success('已添加或签节点')
}

/**
 * 清空节点
 */
function clearNodes(): void {
  nodes.value = []
  selectedNodeId.value = ''
  message.info('已清空所有节点')
}

/**
 * 处理画布状态变化
 */
function handleCanvasStateChange(state: CanvasState): void {
  Object.assign(canvasState, state)
}

/**
 * 处理节点点击
 */
function handleNodeClick(nodeId: string): void {
  // 取消所有选中状态
  nodes.value.forEach(node => node.selected = false)
  connections.value.forEach(conn => conn.selected = false)

  selectedNodeId.value = ''
  selectedConnectionId.value = ''

  // 选中新节点
  const node = nodes.value.find(n => n.id === nodeId)
  if (node) {
    node.selected = true
    selectedNodeId.value = nodeId
  }
}

/**
 * 处理节点开始拖拽
 */
function handleNodeDragStart(nodeId: string, event: MouseEvent): void {
  // 检查是否点击了端口（如果是，开始创建连线）
  const target = event.target as SVGElement
  if (target.classList.contains('port-dot')) {
    // 找到对应的端口信息
    const portElement = target.closest('.flow-port')
    if (portElement) {
      // 获取端口位置（相对于节点）
      const portTransform = portElement.getAttribute('transform')
      const match = portTransform?.match(/translate\(([^,]+),\s*([^)]+)\)/)
      if (match) {
        const portX = parseFloat(match[1])
        const portY = parseFloat(match[2])

        const node = nodes.value.find(n => n.id === nodeId)
        if (node) {
          // 查找对应的端口
          const port = [...(node.inputs || []), ...(node.outputs || [])].find(
            p => p.position.x === portX && p.position.y === portY
          )

          if (port && port.direction === 'output') {
            // 开始创建连线
            isCreatingConnection.value = true
            tempConnectionStart.value = {
              x: node.position.x + port.position.x,
              y: node.position.y + port.position.y
            }
            tempConnectionEnd.value = {
              x: node.position.x + port.position.x,
              y: node.position.y + port.position.y
            }
            creatingConnectionSource.value = {
              nodeId,
              portId: port.id
            }
            return
          }
        }
      }
    }
  }

  // 开始拖拽节点
  draggingNodeId.value = nodeId
  dragOffset.value = {
    x: event.clientX,
    y: event.clientY
  }
}

/**
 * 处理节点拖拽中
 */
function handleNodeDrag(nodeId: string, event: MouseEvent): void {
  if (isCreatingConnection.value) {
    // 更新临时连线终点
    if (canvasRef.value && tempConnectionStart.value) {
      const state = canvasRef.value.getState()
      tempConnectionEnd.value = {
        x: (event.clientX - state.offsetX) / state.scale,
        y: (event.clientY - state.offsetY) / state.scale
      }
    }
    return
  }

  // 拖拽节点
  if (draggingNodeId.value === nodeId && canvasRef.value) {
    const state = canvasRef.value.getState()
    const dx = (event.clientX - dragOffset.value.x) / state.scale
    const dy = (event.clientY - dragOffset.value.y) / state.scale

    const node = nodes.value.find(n => n.id === nodeId)
    if (node) {
      node.position.x += dx
      node.position.y += dy
      dragOffset.value = { x: event.clientX, y: event.clientY }

      // 更新相关连线的位置
      updateConnectionPositions()
    }
  }
}

/**
 * 处理节点拖拽结束
 */
function handleNodeDragEnd(nodeId: string): void {
  if (isCreatingConnection.value) {
    // 结束连线创建
    isCreatingConnection.value = false
    tempConnectionStart.value = null
    tempConnectionEnd.value = null
    creatingConnectionSource.value = null
  } else {
    draggingNodeId.value = ''
  }
}

/**
 * 更新连线位置
 */
function updateConnectionPositions(): void {
  connections.value.forEach(conn => {
    const sourceNode = nodes.value.find(n => n.id === conn.sourceNodeId)
    const targetNode = nodes.value.find(n => n.id === conn.targetNodeId)

    if (sourceNode && targetNode) {
      const sourcePort = sourceNode.outputs?.find(p => p.id === conn.sourcePortId)
      const targetPort = targetNode.inputs?.find(p => p.id === conn.targetPortId)

      if (sourcePort && targetPort) {
        conn.sourcePosition = {
          x: sourceNode.position.x + sourcePort.position.x,
          y: sourceNode.position.y + sourcePort.position.y
        }
        conn.targetPosition = {
          x: targetNode.position.x + targetPort.position.x,
          y: targetNode.position.y + targetPort.position.y
        }
      }
    }
  })
}

/**
 * 处理画布点击
 */
function handleCanvasClick(event: MouseEvent): void {
  // 取消所有选中状态
  nodes.value.forEach(node => node.selected = false)
  connections.value.forEach(conn => conn.selected = false)
  selectedNodeId.value = ''
  selectedConnectionId.value = ''
}

/**
 * 处理连线点击
 */
function handleConnectionClick(connectionId: string): void {
  // 取消所有选中状态
  nodes.value.forEach(node => node.selected = false)
  connections.value.forEach(conn => conn.selected = false)

  selectedNodeId.value = ''
  selectedConnectionId.value = ''

  // 选中新连线
  const conn = connections.value.find(c => c.id === connectionId)
  if (conn) {
    conn.selected = true
    selectedConnectionId.value = connectionId
  }
}

/**
 * 处理连线右键点击
 */
function handleConnectionContextMenu(connectionId: string, event: MouseEvent): void {
  event.preventDefault()

  Modal.confirm({
    title: '删除连线',
    content: '确定要删除这条连线吗？',
    onOk: () => {
      connections.value = connections.value.filter(c => c.id !== connectionId)
      if (selectedConnectionId.value === connectionId) {
        selectedConnectionId.value = ''
      }
      message.success('已删除连线')
    }
  })
}

/**
 * 键盘事件
 */
document.addEventListener('keydown', (event) => {
  // Delete键删除选中节点或连线
  if (event.key === 'Delete') {
    if (selectedNodeId.value) {
      // 删除节点及相关连线
      nodes.value = nodes.value.filter(node => node.id !== selectedNodeId.value)
      connections.value = connections.value.filter(
        conn => conn.sourceNodeId !== selectedNodeId.value && conn.targetNodeId !== selectedNodeId.value
      )
      selectedNodeId.value = ''
      message.success('已删除选中节点')
    } else if (selectedConnectionId.value) {
      // 删除选中连线
      connections.value = connections.value.filter(c => c.id !== selectedConnectionId.value)
      selectedConnectionId.value = ''
      message.success('已删除选中连线')
    }
  }

  // Ctrl+Z撤销（暂未实现）
  if (event.ctrlKey && event.key === 'z') {
    message.info('撤销功能暂未实现')
  }
})

/**
 * 节点库拖拽相关
 */

/**
 * 处理节点库节点拖拽开始
 */
function handleLibraryNodeDragStart(nodeType: NodeType, event: DragEvent): void {
  console.log('节点库拖拽开始:', nodeType)
}

/**
 * 处理节点库节点拖拽结束
 */
function handleLibraryNodeDragEnd(): void {
  console.log('节点库拖拽结束')
}

/**
 * 处理画布拖拽悬停
 */
function handleCanvasDragOver(event: DragEvent): void {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'copy'
  }
}

/**
 * 处理画布放下（从节点库添加节点）
 */
function handleCanvasDrop(event: DragEvent): void {
  event.preventDefault()

  if (!event.dataTransfer) {
    return
  }

  const nodeType = event.dataTransfer.getData('nodeType') as NodeType
  const nodeName = event.dataTransfer.getData('nodeName')

  if (!nodeType) {
    return
  }

  // 计算放置位置（转换为画布坐标）
  if (canvasRef.value) {
    const state = canvasRef.value.getState()
    const dropX = (event.clientX - state.offsetX) / state.scale
    const dropY = (event.clientY - state.offsetY) / state.scale

    // 创建节点
    const node = createBaseNode(
      nodeType,
      nodeName || getNodeTypeName(nodeType),
      getNodeIcon(nodeType),
      dropX - 60, // 居中放置
      dropY - 30
    )

    nodes.value.push(node)
    message.success(`已添加${nodeName || getNodeTypeName(nodeType)}`)
  }
}

/**
 * 获取节点类型名称
 */
function getNodeTypeName(type: NodeType): string {
  const nameMap: Record<NodeType, string> = {
    start: '开始节点',
    end: '结束节点',
    task: '用户任务',
    condition: '条件分支',
    parallel: '并行节点',
    subprocess: '子流程',
    gateway: '网关节点',
    countersign: '会签节点',
    'or-sign': '或签节点'
  }
  return nameMap[type] || type
}

/**
 * 获取节点图标
 */
function getNodeIcon(type: NodeType): string {
  const iconMap: Record<NodeType, string> = {
    start: '▶️',
    end: '⏹️',
    task: '📝',
    condition: '❓',
    parallel: '🔀',
    subprocess: '🔄',
    gateway: '💎',
    countersign: '✍️',
    'or-sign': '👥'
  }
  return iconMap[type] || '📦'
}
</script>

<style scoped>
.workflow-canvas-page {
  padding: 16px;
}

.workflow-layout {
  display: flex;
  gap: 16px;
  height: 600px;
}

.node-library-panel {
  width: 280px;
  min-width: 280px;
  height: 100%;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  overflow: hidden;
}

.canvas-wrapper {
  flex: 1;
  min-width: 0;
  height: 100%;
}

.canvas-container {
  height: 100%;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  background: #f5f5f5;
}

.info-panel {
  margin-top: 16px;
}
</style>