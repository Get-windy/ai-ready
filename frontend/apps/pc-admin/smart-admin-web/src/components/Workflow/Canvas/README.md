# 工作流画布组件使用文档

## 概述

工作流画布组件提供了可视化流程设计器的基础功能，包括画布渲染、节点显示、缩放平移等功能。

## 组件列表

### 1. CanvasBoard - 画布容器组件

#### 更新说明
- 新增 `connections` 插槽，用于放置连线层
- 新增 `connectionSelect` 事件，用于连线选中
- 新增 `connectionDelete` 事件，用于连线删除
- 新增 `canvasClick` 事件，用于画布点击

#### 功能
- SVG画布渲染
- 背景网格显示
- 缩放和平移功能
- 放大/缩小/重置视图控制

#### 使用示例

```vue
<template>
  <CanvasBoard
    ref="canvasRef"
    :config="canvasConfig"
    @state-change="handleCanvasStateChange"
  >
    <template #nodes>
      <!-- 节点内容 -->
    </template>
  </CanvasBoard>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { CanvasBoard } from '@/components/Workflow/Canvas'
import type { CanvasConfig } from '@/types/workflow/canvas'

const canvasRef = ref<InstanceType<typeof CanvasBoard>>()

const canvasConfig: Partial<CanvasConfig> = {
  minScale: 0.2,
  maxScale: 3,
  scaleStep: 0.1,
  grid: {
    show: true,
    size: 20,
    color: '#d9d9d9'
  }
}

function handleCanvasStateChange(state) {
  console.log('画布状态变化:', state)
}
</script>
```

#### Props
| 参数 | 说明 | 类型 | 默认值 |
|-----|------|------|--------|
| config | 画布配置 | `Partial<CanvasConfig>` | - |

#### CanvasConfig 配置项
| 参数 | 说明 | 类型 | 默认值 |
|-----|------|------|--------|
| minScale | 最小缩放比例 | `number` | 0.1 |
| maxScale | 最大缩放比例 | `number` | 5 |
| scaleStep | 缩放步长 | `number` | 0.1 |
| grid | 网格配置 | `GridConfig` | - |

#### Events
| 事件名 | 说明 | 回调参数 |
|-------|------|----------|
| state-change | 画布状态变化 | `(state: CanvasState) => void` |
| node-select | 节点选中 | `(nodeId: string) => void` |
| connection-select | 连线选中 | `(connectionId: string) => void` |
| connection-delete | 连线删除 | `(connectionId: string) => void` |
| canvas-click | 画布点击 | `(event: MouseEvent) => void` |

#### 暴露方法
| 方法名 | 说明 | 参数 |
|-------|------|------|
| zoomIn | 放大 | - |
| zoomOut | 缩小 | - |
| resetView | 重置视图 | - |
| getState | 获取画布状态 | - |
| setState | 设置画布状态 | `Partial<CanvasState>` |

#### 操作说明
- **缩放**: 鼠标滚轮或点击右上角的缩放按钮
- **平移**: 按住空格键 + 鼠标拖动，或按住鼠标中键拖动

---

### 2. Connection - 连线组件

#### 功能
- 支持贝塞尔曲线、直线、折线三种连接类型
- 支持箭头显示
- 支持选中状态高亮
- 支持连线标签显示
- 支持点击和右键点击事件

#### 使用示例

```vue
<template>
  <Connection
    :connection="connectionData"
    @click="handleConnectionClick"
    @context-menu="handleConnectionContextMenu"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Connection } from '@/components/Workflow/Canvas'
import type { Connection } from '@/types/workflow/connection'

const connectionData = {
  id: 'conn-1',
  sourceNodeId: 'node-1',
  targetNodeId: 'node-2',
  sourcePortId: 'output-1',
  targetPortId: 'input-1',
  sourcePosition: { x: 100, y: 100 },
  targetPosition: { x: 300, y: 100 },
  selected: false,
  style: {
    type: 'bezier',
    showArrow: true
  }
}

function handleConnectionClick(connectionId: string) {
  console.log('连线被点击:', connectionId)
}

function handleConnectionContextMenu(connectionId: string, event: MouseEvent) {
  console.log('连线右键点击:', connectionId)
}
</script>
```

#### Props
| 参数 | 说明 | 类型 | 默认值 |
|-----|------|------|--------|
| connection | 连线数据 | `Connection` | - |

#### Events
| 事件名 | 说明 | 回调参数 |
|-------|------|----------|
| click | 连线点击 | `(connectionId: string) => void` |
| context-menu | 连线右键点击 | `(connectionId: string, event: MouseEvent) => void` |
| mouse-down | 连线鼠标按下 | `(connectionId: string, event: MouseEvent) => void` |

#### 连线类型
| 类型 | 说明 |
|-----|------|
| bezier | 贝塞尔曲线（默认，平滑美观） |
| straight | 直线（简单直接） |
| step | 折线（水平和垂直线段） |

### 3. ConnectionManager - 连线管理组件

#### 功能
- 管理连线列表
- 支持创建连线时的临时连线显示
- 连线事件统一处理

#### 使用示例

```vue
<template>
  <ConnectionManager
    :connections="connections"
    :is-creating-connection="isCreatingConnection"
    :temp-connection-start="tempConnectionStart"
    :temp-connection-end="tempConnectionEnd"
    @connection-click="handleConnectionClick"
    @connection-context-menu="handleConnectionContextMenu"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ConnectionManager } from '@/components/Workflow/Canvas'
import type { Connection } from '@/types/workflow/connection'

const connections = ref<Connection[]>([])
const isCreatingConnection = ref(false)
const tempConnectionStart = ref({ x: 0, y: 0 })
const tempConnectionEnd = ref({ x: 0, y: 0 })

function handleConnectionClick(connectionId: string) {
  console.log('连线被点击:', connectionId)
}

function handleConnectionContextMenu(connectionId: string, event: MouseEvent) {
  // 显示右键菜单
}
</script>
```

#### Props
| 参数 | 说明 | 类型 | 默认值 |
|-----|------|------|--------|
| connections | 连线列表 | `Connection[]` | [] |
| isCreatingConnection | 是否正在创建连线 | `boolean` | false |
| tempConnectionStart | 临时连线起点 | `NodePosition` | - |
| tempConnectionEnd | 临时连线终点 | `NodePosition` | - |

#### Events
| 事件名 | 说明 | 回调参数 |
|-------|------|----------|
| connection-click | 连线点击 | `(connectionId: string) => void` |
| connection-context-menu | 连线右键点击 | `(connectionId: string, event: MouseEvent) => void` |

### 4. NodeLibrary - 节点库组件

#### 功能
- 显示所有可用的节点类型
- 分类显示（基础节点、高级节点、网关节点）
- 支持节点搜索
- 支持拖拽节点到画布

#### 使用示例

```vue
<template>
  <NodeLibrary
    @drag-start="handleLibraryNodeDragStart"
    @drag-end="handleLibraryNodeDragEnd"
  />
</template>

<script setup lang="ts">
import { NodeLibrary } from '@/components/Workflow/Canvas'
import type { NodeType } from '@/types/workflow/node'

function handleLibraryNodeDragStart(nodeType: NodeType, event: DragEvent) {
  console.log('节点库拖拽开始:', nodeType)
}

function handleLibraryNodeDragEnd() {
  console.log('节点库拖拽结束')
}
</script>
```

#### Events
| 事件名 | 说明 | 回调参数 |
|-------|------|----------|
| drag-start | 节点拖拽开始 | `(nodeType: NodeType, event: DragEvent) => void` |
| drag-end | 节点拖拽结束 | `() => void` |

#### 节点分类
| 分类 | 节点类型 |
|-----|---------|
| 基础节点 | 开始节点、结束节点、用户任务 |
| 高级节点 | 条件分支、并行节点、子流程、会签节点、或签节点 |
| 网关节点 | 网关节点 |

#### 使用方法
1. 在画布区域添加 `dragover` 事件处理
2. 在画布区域添加 `drop` 事件处理
3. 在 `drop` 事件中获取 `nodeType` 数据
4. 根据节点类型创建新节点并添加到节点列表

示例代码：

```vue
<template>
  <div
    class="canvas-container"
    @dragover="handleCanvasDragOver"
    @drop="handleCanvasDrop"
  >
    <CanvasBoard>
      <!-- 画布内容 -->
    </CanvasBoard>
  </div>
</template>

<script setup lang="ts">
function handleCanvasDragOver(event: DragEvent): void {
  event.preventDefault()
  if (event.dataTransfer) {
    event.dataTransfer.dropEffect = 'copy'
  }
}

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

  // 计算放置位置
  const state = canvasRef.value.getState()
  const dropX = (event.clientX - state.offsetX) / state.scale
  const dropY = (event.clientY - state.offsetY) / state.scale

  // 创建节点
  const node = createBaseNode(
    nodeType,
    nodeName || getNodeTypeName(nodeType),
    getNodeIcon(nodeType),
    dropX - 60,
    dropY - 30
  )

  nodes.value.push(node)
}
</script>
```

---

### 5. FlowNode - 节点组件

#### 功能
- 节点渲染（支持多种形状）
- 节点选中状态
- 端口显示（输入/输出）
- 节点拖拽（基础支持）

#### 使用示例

```vue
<template>
  <FlowNode
    :node="nodeData"
    @click="handleNodeClick"
    @drag-start="handleNodeDragStart"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { FlowNode } from '@/components/Workflow/Canvas'
import type { FlowNode as FlowNodeType } from '@/types/workflow/node'

const nodeData: FlowNodeType = {
  type: 'task',
  id: 'node-1',
  name: '任务节点',
  description: '这是一个任务节点',
  icon: '📝',
  selectable: true,
  draggable: true,
  position: { x: 100, y: 100 },
  status: 'idle',
  selected: false,
  inputs: [
    {
      id: 'input-1',
      direction: 'input',
      name: '输入',
      position: { x: 0, y: 30 }
    }
  ],
  outputs: [
    {
      id: 'output-1',
      direction: 'output',
      name: '输出',
      position: { x: 120, y: 30 }
    }
  ]
}

function handleNodeClick(nodeId: string) {
  console.log('节点被点击:', nodeId)
}

function handleNodeDragStart(nodeId: string, event: MouseEvent) {
  console.log('节点开始拖拽:', nodeId)
}
</script>
```

#### Props
| 参数 | 说明 | 类型 | 默认值 |
|-----|------|------|--------|
| node | 节点数据 | `FlowNode` | - |

#### Events
| 事件名 | 说明 | 回调参数 |
|-------|------|----------|
| click | 节点点击 | `(nodeId: string) => void` |
| drag-start | 节点开始拖拽 | `(nodeId: string, event: MouseEvent) => void` |
| drag | 节点拖拽中 | `(nodeId: string, event: MouseEvent) => void` |
| drag-end | 节点拖拽结束 | `(nodeId: string) => void` |

#### 节点类型
| 类型 | 说明 | 形状 |
|-----|------|------|
| start | 开始节点 | 椭圆形 |
| end | 结束节点 | 椭圆形 |
| task | 任务节点 | 矩形 |
| condition | 条件节点 | 菱形 |
| parallel | 并行节点 | 矩形 |
| subprocess | 子流程节点 | 矩形 |
| gateway | 网关节点 | 菱形 |
| countersign | 会签节点 | 矩形（带装饰条） |
| or-sign | 或签节点 | 矩形（带装饰条） |

#### 节点状态
| 状态 | 说明 |
|-----|------|
| idle | 空闲 |
| running | 运行中 |
| completed | 已完成 |
| error | 错误 |
| skipped | 已跳过 |

---

## 完整示例

请参考 `src/views/WorkflowCanvas.vue` 文件，该文件包含了画布和节点的完整使用示例。

## 验收标准

- ✅ 画布正常渲染
- ✅ 背景网格显示
- ✅ 节点可以在画布上显示
- ✅ 节点可以被选中
- ✅ 画布可以缩放和平移
- ✅ 支持多种节点类型和形状
- ✅ 节点库显示所有可用节点类型
- ✅ 节点库支持搜索功能
- ✅ 节点库支持拖拽添加节点到画布
- ✅ 会签节点和或签节点正常显示
- ✅ 新节点类型样式符合设计规范

## 后续开发计划

1. ~~**节点拖拽功能**~~ - ✅ 已实现完整的节点拖拽逻辑
2. ~~**连线绘制**~~ - ✅ 已实现节点之间的连线
3. ~~**节点组件库**~~ - ✅ 已实现节点组件库，支持拖拽添加节点
4. **属性面板** - 编辑节点和连线属性
5. **流程验证** - 验证流程的合法性
6. **保存和加载** - 流程的保存和加载功能
7. **撤销/重做** - 支持操作历史记录

## 注意事项

1. 画布尺寸默认为 2000x1500，可以根据容器自动调整
2. 节点位置是相对于画布的坐标
3. 缩放和平移会影响所有节点的显示
4. 端口位置是相对于节点的坐标
5. 选中状态会显示虚线边框和控制手柄
6. 从节点库拖拽节点到画布时，节点会放置在鼠标位置
7. 节点库中的节点分类包括：基础节点、高级节点、网关节点
8. 会签节点和或签节点使用矩形形状，并带有左侧装饰条

## 技术栈

- Vue 3 (Composition API)
- TypeScript
- Ant Design Vue
- SVG