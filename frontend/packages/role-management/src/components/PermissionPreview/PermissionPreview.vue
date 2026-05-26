<template>
  <div class="permission-preview">
    <!-- 组件标题和操作 -->
    <div class="preview-header">
      <h3>
        <el-icon><View /></el-icon>
        权限预览 - {{ user?.name || '请选择用户' }}
      </h3>
      <div class="header-actions">
        <el-button 
          v-if="selectedUserId" 
          type="primary" 
          @click="exportPermissionReport"
          :loading="exporting"
        >
          <el-icon><Download /></el-icon>
          导出报告
        </el-button>
        <el-button @click="refreshPreview">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-container">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>加载权限信息中...</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="!selectedUserId" class="empty-state">
      <el-icon><User /></el-icon>
      <p>请先在左侧选择用户以查看权限信息</p>
    </div>

    <!-- 权限预览内容 -->
    <div v-else class="preview-content">
      <!-- 用户基本信息卡片 -->
      <el-card class="user-info-card">
        <div class="user-info">
          <el-avatar :size="64" :src="user?.avatar" class="user-avatar">
            {{ user?.name?.charAt(0) || 'U' }}
          </el-avatar>
          <div class="user-details">
            <h4>{{ user?.name }}</h4>
            <div class="user-meta">
              <span><el-icon><Message /></el-icon> {{ user?.email }}</span>
              <span><el-icon><OfficeBuilding /></el-icon> {{ user?.department }}</span>
              <span><el-icon><UserFilled /></el-icon> {{ user?.position }}</span>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 权限概览 -->
      <div class="permission-overview">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-statistic title="总权限数" :value="permissionSummary.total" />
          </el-col>
          <el-col :span="6">
            <el-statistic 
              title="角色权限" 
              :value="permissionSummary.roleBased" 
              suffix="个"
            />
          </el-col>
          <el-col :span="6">
            <el-statistic 
              title="直接权限" 
              :value="permissionSummary.direct" 
              suffix="个"
            />
          </el-col>
          <el-col :span="6">
            <el-statistic title="冲突权限" :value="permissionSummary.conflicts">
              <template #prefix>
                <el-icon v-if="permissionSummary.conflicts > 0" color="var(--el-color-danger)">
                  <WarningFilled />
                </el-icon>
                <el-icon v-else color="var(--el-color-success)">
                  <CircleCheckFilled />
                </el-icon>
              </template>
            </el-statistic>
          </el-col>
        </el-row>
      </div>

      <!-- 权限来源分析 -->
      <el-tabs v-model="activeTab" type="card" class="permission-tabs">
        <!-- 权限树展示 -->
        <el-tab-pane label="权限树" name="tree">
          <div class="permission-tree-section">
            <el-input
              v-model="treeSearchKeyword"
              placeholder="搜索权限..."
              clearable
              @input="handleTreeSearch"
              style="margin-bottom: 16px"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            
            <div v-if="permissionTree.length === 0" class="empty-permission-tree">
              <el-icon><FolderOpened /></el-icon>
              <p>暂无权限数据</p>
            </div>
            <PermissionTree
              v-else
              :tree-data="permissionTree"
              :show-checkbox="false"
              :default-expand-all="true"
              :highlight-keywords="treeSearchKeyword ? [treeSearchKeyword] : []"
              @node-click="handlePermissionNodeClick"
            />
          </div>
        </el-tab-pane>

        <!-- 权限来源 -->
        <el-tab-pane label="来源分析" name="sources">
          <div class="permission-sources">
            <h4>权限来源分布</h4>
            <el-table :data="permissionSources" style="width: 100%">
              <el-table-column prop="sourceType" label="来源类型" width="120" />
              <el-table-column prop="sourceName" label="来源名称" width="180" />
              <el-table-column prop="permissionCount" label="权限数量" width="100">
                <template #default="{ row }">
                  <el-tag size="small">{{ row.permissionCount }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="permissions" label="权限列表">
                <template #default="{ row }">
                  <el-tag
                    v-for="permission in row.permissions.slice(0, 3)"
                    :key="permission.id"
                    size="small"
                    style="margin-right: 4px; margin-bottom: 4px"
                  >
                    {{ permission.name }}
                  </el-tag>
                  <span v-if="row.permissions.length > 3">
                    等{{ row.permissions.length }}项
                  </span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <!-- 详细列表 -->
        <el-tab-pane label="详细列表" name="details">
          <div class="permission-details">
            <el-table :data="detailedPermissions" style="width: 100%">
              <el-table-column prop="id" label="ID" width="120" />
              <el-table-column prop="name" label="权限名称" width="180" />
              <el-table-column prop="code" label="权限代码" width="150" />
              <el-table-column prop="type" label="类型" width="100">
                <template #default="{ row }">
                  <el-tag :type="getPermissionTypeTag(row.type)">
                    {{ getPermissionTypeText(row.type) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="sources" label="来源">
                <template #default="{ row }">
                  <div class="permission-sources-list">
                    <el-tag
                      v-for="source in row.sources"
                      :key="source.id"
                      size="small"
                      :type="source.type === 'role' ? '' : 'info'"
                      style="margin-right: 4px; margin-bottom: 4px"
                    >
                      {{ source.name }}
                    </el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="inherited" label="继承" width="80">
                <template #default="{ row }">
                  <el-tag v-if="row.inherited" type="success">是</el-tag>
                  <el-tag v-else type="info">否</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>

      <!-- 权限冲突警告 -->
      <div v-if="permissionConflicts.length > 0" class="conflict-warning">
        <el-alert
          title="检测到权限冲突"
          type="warning"
          :closable="false"
          show-icon
        >
          <template #description>
            <div class="conflict-list">
              <p>检测到 {{ permissionConflicts.length }} 个权限冲突，建议及时处理：</p>
              <ul>
                <li v-for="conflict in permissionConflicts.slice(0, 3)" :key="conflict.id">
                  {{ conflict.description }}
                </li>
              </ul>
              <el-button type="warning" size="small" @click="handleViewConflicts">
                查看全部冲突
              </el-button>
            </div>
          </template>
        </el-alert>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import {
  User,
  View,
  Download,
  Refresh,
  Loading,
  Message,
  OfficeBuilding,
  UserFilled,
  WarningFilled,
  CircleCheckFilled,
  Search,
  FolderOpened
} from '@element-plus/icons-vue'
import PermissionTree from '@/components/PermissionTree/PermissionTree.vue'
import { roleManagementApi } from '@/api'
import type { 
  UserInfo, 
  UserPermissionPreview, 
  PermissionTreeNode, 
  PermissionConflict,
  PermissionSource 
} from '@/types'

// Props
interface Props {
  userId?: string
  user?: UserInfo
}

const props = withDefaults(defineProps<Props>(), {
  userId: '',
  user: undefined
})

// Emits
const emit = defineEmits<{
  permissionNodeClick: [permission: PermissionTreeNode]
  viewConflicts: []
}>()

// 状态管理
const loading = ref(false)
const exporting = ref(false)
const permissionPreview = ref<UserPermissionPreview | null>(null)
const permissionTree = ref<PermissionTreeNode[]>([])
const permissionConflicts = ref<PermissionConflict[]>([])
const activeTab = ref('tree')
const treeSearchKeyword = ref('')
const permissionSources = ref<PermissionSource[]>([])

// 计算属性
const selectedUserId = computed(() => props.userId)
const detailedPermissions = computed(() => permissionPreview.value?.detailedPermissions || [])
const permissionSummary = computed(() => ({
  total: permissionPreview.value?.totalPermissions || 0,
  roleBased: permissionPreview.value?.roleBasedPermissions || 0,
  direct: permissionPreview.value?.directPermissions || 0,
  conflicts: permissionPreview.value?.conflictCount || 0
}))

// 权限类型映射
const permissionTypeMap = {
  menu: { text: '菜单', tag: 'primary' },
  button: { text: '按钮', tag: 'success' },
  api: { text: 'API', tag: 'warning' },
  data: { text: '数据', tag: 'info' }
} as const

// 方法
const loadPermissionPreview = async () => {
  if (!selectedUserId.value) return
  
  try {
    loading.value = true
    const preview = await roleManagementApi.getUserPermissionPreview(selectedUserId.value)
    permissionPreview.value = preview
    permissionTree.value = preview.permissionTree || []
    permissionSources.value = preview.sources || []
    
    // 同时获取权限冲突
    await loadPermissionConflicts()
  } catch (error) {
    console.error('加载权限预览失败:', error)
    ElMessage.error('加载权限预览失败')
  } finally {
    loading.value = false
  }
}

const loadPermissionConflicts = async () => {
  if (!selectedUserId.value) return
  
  try {
    const conflicts = await roleManagementApi.checkPermissionConflicts(selectedUserId.value)
    permissionConflicts.value = conflicts
  } catch (error) {
    console.error('加载权限冲突失败:', error)
  }
}

const refreshPreview = async () => {
  await loadPermissionPreview()
  ElMessage.success('权限预览已刷新')
}

const exportPermissionReport = async () => {
  if (!selectedUserId.value) return
  
  try {
    exporting.value = true
    const blob = await roleManagementApi.exportPermissions('excel')
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `permission-report-${selectedUserId.value}-${Date.now()}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('权限报告导出成功')
  } catch (error) {
    console.error('导出权限报告失败:', error)
    ElMessage.error('导出权限报告失败')
  } finally {
    exporting.value = false
  }
}

const handleTreeSearch = () => {
  // 搜索功能已集成到PermissionTree组件中
}

const handlePermissionNodeClick = (node: PermissionTreeNode) => {
  emit('permissionNodeClick', node)
}

const handleViewConflicts = () => {
  emit('viewConflicts')
}

const getPermissionTypeText = (type: string) => {
  return permissionTypeMap[type as keyof typeof permissionTypeMap]?.text || type
}

const getPermissionTypeTag = (type: string) => {
  return permissionTypeMap[type as keyof typeof permissionTypeMap]?.tag || 'info'
}

// 监听userId变化
watch(() => props.userId, (newUserId) => {
  if (newUserId) {
    loadPermissionPreview()
  } else {
    permissionPreview.value = null
    permissionTree.value = []
    permissionConflicts.value = []
  }
})

// 样式
</script>

<style scoped>
.permission-preview {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--el-border-color);
}

.preview-header h3 {
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  color: var(--el-text-color-primary);
}

.header-actions {
  display: flex;
  gap: 12px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 0;
  color: var(--el-text-color-secondary);
}

.loading-icon {
  font-size: 48px;
  margin-bottom: 16px;
  color: var(--el-color-primary);
  animation: rotate 2s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 0;
  color: var(--el-text-color-secondary);
}

.empty-state .el-icon {
  font-size: 64px;
  margin-bottom: 16px;
  color: var(--el-text-color-placeholder);
}

.preview-content {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.user-info-card {
  margin-bottom: 16px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-avatar {
  background-color: var(--el-color-primary);
  color: white;
  font-size: 24px;
  font-weight: bold;
}

.user-details h4 {
  margin: 0 0 8px 0;
  font-size: 20px;
  color: var(--el-text-color-primary);
}

.user-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.user-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.permission-overview {
  margin-bottom: 16px;
}

.permission-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
}

:deep(.el-tabs__content) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

:deep(.el-tab-pane) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.permission-tree-section {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.empty-permission-tree {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
  padding: 64px 0;
}

.empty-permission-tree .el-icon {
  font-size: 64px;
  margin-bottom: 16px;
  color: var(--el-text-color-placeholder);
}

.permission-sources {
  flex: 1;
}

.permission-sources h4 {
  margin: 0 0 16px 0;
  color: var(--el-text-color-primary);
}

.permission-details {
  flex: 1;
}

.conflict-warning {
  margin-top: 16px;
}

.conflict-list {
  font-size: 14px;
}

.conflict-list ul {
  margin: 8px 0;
  padding-left: 20px;
}

.conflict-list li {
  margin-bottom: 4px;
  color: var(--el-color-warning-dark);
}
</style>