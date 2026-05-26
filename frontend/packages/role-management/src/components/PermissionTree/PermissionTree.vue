<template>
  <div class="permission-tree-container">
    <!-- 搜索和过滤 -->
    <div class="tree-header">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索权限名称或编码"
        clearable
        @input="handleSearch"
        class="search-input"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      
      <div class="tree-actions">
        <el-button size="small" @click="expandAll">
          <el-icon><Expand /></el-icon> 展开全部
        </el-button>
        <el-button size="small" @click="collapseAll">
          <el-icon><Fold /></el-icon> 收起全部
        </el-button>
        <el-button size="small" @click="selectAll">
          <el-icon><Check /></el-icon> 全选
        </el-button>
        <el-button size="small" @click="clearAll">
          <el-icon><Close /></el-icon> 清空
        </el-button>
      </div>
    </div>

    <!-- 权限树 -->
    <div class="tree-container">
      <el-tree
        ref="treeRef"
        :data="filteredTreeData"
        :props="treeProps"
        node-key="id"
        show-checkbox
        :default-expand-all="defaultExpandAll"
        :expand-on-click-node="false"
        :filter-node-method="filterNode"
        :check-strictly="checkStrictly"
        @check="handleCheckChange"
      >
        <template #default="{ node, data }">
          <div class="tree-node-content">
            <div class="node-info">
              <el-icon v-if="data.icon" :size="16" class="node-icon">
                <component :is="data.icon" />
              </el-icon>
              <span class="node-label">{{ data.name }}</span>
              <el-tag v-if="data.type" size="small" :type="getPermissionTypeTag(data.type)">
                {{ getPermissionTypeText(data.type) }}
              </el-tag>
              <el-tag v-if="!data.enabled" size="small" type="danger">禁用</el-tag>
            </div>
            <div class="node-meta">
              <span class="node-code">{{ data.code }}</span>
              <span v-if="data.description" class="node-desc">{{ data.description }}</span>
            </div>
          </div>
        </template>
      </el-tree>

      <!-- 空状态 -->
      <div v-if="!filteredTreeData.length" class="empty-state">
        <el-empty description="暂无权限数据" />
      </div>
    </div>

    <!-- 已选权限统计 -->
    <div class="selection-stats">
      <div class="stats-item">
        <span class="stats-label">已选权限：</span>
        <span class="stats-value">{{ selectedCount }} / {{ totalCount }}</span>
      </div>
      <div class="stats-item">
        <span class="stats-label">菜单权限：</span>
        <span class="stats-value">{{ stats.menu }}</span>
      </div>
      <div class="stats-item">
        <span class="stats-label">按钮权限：</span>
        <span class="stats-value">{{ stats.button }}</span>
      </div>
      <div class="stats-item">
        <span class="stats-label">API权限：</span>
        <span class="stats-value">{{ stats.api }}</span>
      </div>
      <div class="stats-item">
        <span class="stats-label">数据权限：</span>
        <span class="stats-value">{{ stats.data }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { Search, Expand, Fold, Check, Close } from '@element-plus/icons-vue'
import type { ElTree } from 'element-plus'
import type { PermissionTreeNode, Permission } from '../types'

interface Props {
  treeData: PermissionTreeNode[]
  selectedKeys?: string[]
  checkStrictly?: boolean
  defaultExpandAll?: boolean
  readonly?: boolean
}

interface Emits {
  (e: 'update:selectedKeys', keys: string[]): void
  (e: 'change', keys: string[]): void
}

const props = withDefaults(defineProps<Props>(), {
  selectedKeys: () => [],
  checkStrictly: false,
  defaultExpandAll: true,
  readonly: false
})

const emit = defineEmits<Emits>()

// Refs
const treeRef = ref<InstanceType<typeof ElTree>>()
const searchKeyword = ref('')

// 计算属性
const filteredTreeData = computed(() => {
  if (!searchKeyword.value) {
    return props.treeData
  }
  
  const filter = (nodes: PermissionTreeNode[]): PermissionTreeNode[] => {
    return nodes.filter(node => {
      const match = node.name.includes(searchKeyword.value) || 
                   node.code.includes(searchKeyword.value) ||
                   node.description?.includes(searchKeyword.value)
      
      // 如果有子节点，递归过滤
      if (node.children && node.children.length > 0) {
        const filteredChildren = filter(node.children)
        node.children = filteredChildren
        return match || filteredChildren.length > 0
      }
      
      return match
    })
  }
  
  return filter([...props.treeData])
})

const selectedCount = computed(() => {
  return props.selectedKeys.length
})

const totalCount = computed(() => {
  const countNodes = (nodes: PermissionTreeNode[]): number => {
    return nodes.reduce((total, node) => {
      let count = 1 // 当前节点
      if (node.children && node.children.length > 0) {
        count += countNodes(node.children)
      }
      return total + count
    }, 0)
  }
  return countNodes(props.treeData)
})

const stats = computed(() => {
  const stats = { menu: 0, button: 0, api: 0, data: 0 }
  
  const countStats = (nodes: PermissionTreeNode[]) => {
    nodes.forEach(node => {
      if (props.selectedKeys.includes(node.id)) {
        stats[node.type] = (stats[node.type] || 0) + 1
      }
      if (node.children && node.children.length > 0) {
        countStats(node.children)
      }
    })
  }
  
  countStats(props.treeData)
  return stats
})

// 树配置
const treeProps = {
  label: 'name',
  children: 'children'
}

// 方法
function getPermissionTypeTag(type: string): string {
  const typeMap: Record<string, string> = {
    menu: 'primary',
    button: 'success',
    api: 'warning',
    data: 'danger'
  }
  return typeMap[type] || 'info'
}

function getPermissionTypeText(type: string): string {
  const typeMap: Record<string, string> = {
    menu: '菜单',
    button: '按钮',
    api: '接口',
    data: '数据'
  }
  return typeMap[type] || type
}

function filterNode(value: string, data: PermissionTreeNode): boolean {
  if (!value) return true
  return data.name.includes(value) || 
         data.code.includes(value) || 
         data.description?.includes(value) || false
}

function handleSearch() {
  if (treeRef.value) {
    treeRef.value.filter(searchKeyword.value)
  }
}

function expandAll() {
  if (treeRef.value) {
    const nodes = treeRef.value.store.nodesMap
    Object.values(nodes).forEach(node => {
      node.expanded = true
    })
  }
}

function collapseAll() {
  if (treeRef.value) {
    const nodes = treeRef.value.store.nodesMap
    Object.values(nodes).forEach(node => {
      node.expanded = false
    })
  }
}

function selectAll() {
  if (treeRef.value) {
    const allKeys = getAllKeys(props.treeData)
    treeRef.value.setCheckedKeys(allKeys)
    emitSelectedKeys(allKeys)
  }
}

function clearAll() {
  if (treeRef.value) {
    treeRef.value.setCheckedKeys([])
    emitSelectedKeys([])
  }
}

function handleCheckChange(checkedData: PermissionTreeNode, checkedState: {
  checkedKeys: string[]
  halfCheckedKeys: string[]
}) {
  emitSelectedKeys(checkedState.checkedKeys)
}

function emitSelectedKeys(keys: string[]) {
  emit('update:selectedKeys', keys)
  emit('change', keys)
}

function getAllKeys(nodes: PermissionTreeNode[]): string[] {
  const keys: string[] = []
  
  const traverse = (nodes: PermissionTreeNode[]) => {
    nodes.forEach(node => {
      if (node.enabled !== false) { // 只选择启用的权限
        keys.push(node.id)
      }
      if (node.children && node.children.length > 0) {
        traverse(node.children)
      }
    })
  }
  
  traverse(nodes)
  return keys
}

// 监听外部选中的变化
watch(
  () => props.selectedKeys,
  (newKeys) => {
    if (treeRef.value) {
      treeRef.value.setCheckedKeys(newKeys, props.checkStrictly)
    }
  },
  { immediate: true }
)

// 初始化
onMounted(() => {
  if (treeRef.value && props.defaultExpandAll) {
    expandAll()
  }
})
</script>

<style scoped>
.permission-tree-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  background: white;
}

.tree-header {
  padding: 12px;
  border-bottom: 1px solid #ebeef5;
  background: #fafafa;
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-input {
  flex: 1;
  max-width: 300px;
}

.tree-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tree-container {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  min-height: 300px;
}

.tree-node-content {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.node-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.node-icon {
  color: #409eff;
}

.node-label {
  font-weight: 500;
  color: #303133;
}

.node-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.node-code {
  font-family: monospace;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  border: 1px solid #ebeef5;
}

.node-desc {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.selection-stats {
  padding: 12px;
  border-top: 1px solid #ebeef5;
  background: #fafafa;
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.stats-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.stats-label {
  font-size: 14px;
  color: #606266;
}

.stats-value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

:deep(.el-tree-node__content) {
  height: auto;
  min-height: 40px;
  padding: 8px 0;
}

:deep(.el-tree-node__expand-icon) {
  font-size: 16px;
}
</style>