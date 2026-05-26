<template>
  <div class="node-library">
    <div class="node-library-header">
      <h3>节点组件库</h3>
      <a-tooltip title="搜索节点">
        <a-input-search
          v-model:value="searchText"
          placeholder="搜索节点"
          size="small"
          style="width: 150px"
        />
      </a-tooltip>
    </div>

    <div class="node-library-content">
      <a-collapse
        v-model:activeKey="activeKeys"
        :bordered="false"
        size="small"
      >
        <!-- 基础节点 -->
        <a-collapse-panel
          key="basic"
          header="基础节点"
        >
          <div
            v-for="node in basicNodes"
            :key="node.type"
            class="library-node"
            draggable="true"
            @dragstart="handleDragStart($event, node)"
            @dragend="handleDragEnd"
          >
            <div class="library-node-icon">
              {{ node.icon }}
            </div>
            <div class="library-node-info">
              <div class="library-node-name">{{ node.name }}</div>
              <div class="library-node-desc">{{ node.description }}</div>
            </div>
          </div>
        </a-collapse-panel>

        <!-- 高级节点 -->
        <a-collapse-panel
          key="advanced"
          header="高级节点"
        >
          <div
            v-for="node in advancedNodes"
            :key="node.type"
            class="library-node"
            draggable="true"
            @dragstart="handleDragStart($event, node)"
            @dragend="handleDragEnd"
          >
            <div class="library-node-icon">
              {{ node.icon }}
            </div>
            <div class="library-node-info">
              <div class="library-node-name">{{ node.name }}</div>
              <div class="library-node-desc">{{ node.description }}</div>
            </div>
          </div>
        </a-collapse-panel>

        <!-- 网关节点 -->
        <a-collapse-panel
          key="gateway"
          header="网关节点"
        >
          <div
            v-for="node in gatewayNodes"
            :key="node.type"
            class="library-node"
            draggable="true"
            @dragstart="handleDragStart($event, node)"
            @dragend="handleDragEnd"
          >
            <div class="library-node-icon">
              {{ node.icon }}
            </div>
            <div class="library-node-info">
              <div class="library-node-name">{{ node.name }}</div>
              <div class="library-node-desc">{{ node.description }}</div>
            </div>
          </div>
        </a-collapse-panel>
      </a-collapse>
    </div>

    <div class="node-library-footer">
      <a-space direction="vertical" style="width: 100%">
        <a-alert
          message="使用说明"
          description="拖拽节点到画布添加新节点"
          type="info"
          show-icon
          size="small"
        />
      </a-space>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { NodeType } from '@/types/workflow/node'

/**
 * Props
 */
interface Props {
  /** 拖拽开始事件 */
  onDragStart?: (node: LibraryNode) => void
}

/**
 * 节点库项目
 */
interface LibraryNode {
  /** 节点类型 */
  type: NodeType
  /** 节点名称 */
  name: string
  /** 节点描述 */
  description: string
  /** 节点图标 */
  icon: string
  /** 节点分类 */
  category: 'basic' | 'advanced' | 'gateway'
}

/**
 * Emits
 */
const emit = defineEmits<{
  /** 节点拖拽开始 */
  dragStart: [nodeType: NodeType, event: DragEvent]
  /** 节点拖拽结束 */
  dragEnd: []
}>()

/**
 * 状态
 */
const searchText = ref('')
const activeKeys = ref(['basic', 'advanced', 'gateway'])

/**
 * 节点库数据
 */
const allNodes: LibraryNode[] = [
  // 基础节点
  {
    type: 'start',
    name: '开始节点',
    description: '流程的起始点',
    icon: '▶️',
    category: 'basic'
  },
  {
    type: 'end',
    name: '结束节点',
    description: '流程的终止点',
    icon: '⏹️',
    category: 'basic'
  },
  {
    type: 'task',
    name: '用户任务',
    description: '用户任务节点',
    icon: '📝',
    category: 'basic'
  },
  // 高级节点
  {
    type: 'condition',
    name: '条件分支',
    description: '条件判断节点',
    icon: '❓',
    category: 'advanced'
  },
  {
    type: 'parallel',
    name: '并行节点',
    description: '并行执行分支',
    icon: '🔀',
    category: 'advanced'
  },
  {
    type: 'subprocess',
    name: '子流程',
    description: '调用子流程',
    icon: '🔄',
    category: 'advanced'
  },
  {
    type: 'countersign',
    name: '会签节点',
    description: '多人会签审批',
    icon: '✍️',
    category: 'advanced'
  },
  {
    type: 'or-sign',
    name: '或签节点',
    description: '任意一人审批',
    icon: '👥',
    category: 'advanced'
  },
  // 网关节点
  {
    type: 'gateway',
    name: '网关节点',
    description: '条件网关',
    icon: '💎',
    category: 'gateway'
  }
]

/**
 * 计算属性
 */

/** 基础节点 */
const basicNodes = computed(() => {
  return allNodes
    .filter(node => node.category === 'basic')
    .filter(node => filterBySearch(node))
})

/** 高级节点 */
const advancedNodes = computed(() => {
  return allNodes
    .filter(node => node.category === 'advanced')
    .filter(node => filterBySearch(node))
})

/** 网关节点 */
const gatewayNodes = computed(() => {
  return allNodes
    .filter(node => node.category === 'gateway')
    .filter(node => filterBySearch(node))
})

/**
 * 方法
 */

/**
 * 根据搜索文本过滤节点
 */
function filterBySearch(node: LibraryNode): boolean {
  if (!searchText.value) {
    return true
  }
  const search = searchText.value.toLowerCase()
  return (
    node.name.toLowerCase().includes(search) ||
    node.description.toLowerCase().includes(search) ||
    node.type.toLowerCase().includes(search)
  )
}

/**
 * 处理节点拖拽开始
 */
function handleDragStart(event: DragEvent, node: LibraryNode) {
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'copy'
    event.dataTransfer.setData('nodeType', node.type)
    event.dataTransfer.setData('nodeName', node.name)
  }
  emit('dragStart', node.type, event)
}

/**
 * 处理节点拖拽结束
 */
function handleDragEnd() {
  emit('dragEnd')
}
</script>

<style scoped lang="scss">
.node-library {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #ffffff;
  border-right: 1px solid #e8e8e8;
}

.node-library-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid #e8e8e8;

  h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #000000d9;
  }
}

.node-library-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;

  &::-webkit-scrollbar {
    width: 6px;
  }

  &::-webkit-scrollbar-thumb {
    background: #d9d9d9;
    border-radius: 3px;
  }

  &::-webkit-scrollbar-thumb:hover {
    background: #bfbfbf;
  }
}

.library-node {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  margin: 4px 12px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  background: #fafafa;
  cursor: grab;
  transition: all 0.2s;

  &:hover {
    border-color: #1890ff;
    background: #e6f7ff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  &:active {
    cursor: grabbing;
  }
}

.library-node-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  margin-right: 12px;
  font-size: 20px;
  background: #ffffff;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
}

.library-node-info {
  flex: 1;
  min-width: 0;
}

.library-node-name {
  font-size: 14px;
  font-weight: 500;
  color: #000000d9;
  margin-bottom: 4px;
}

.library-node-desc {
  font-size: 12px;
  color: #00000073;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.node-library-footer {
  padding: 12px 16px;
  border-top: 1px solid #e8e8e8;
}
</style>