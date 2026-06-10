<template>
  <div class="permission-config-panel">
    <!-- 左侧：角色列表 -->
    <div class="role-sidebar">
      <div class="role-sidebar-header">
        <h3>角色列表</h3>
        <a-button size="small" type="primary" ghost @click="loadRoles">
          <template #icon><ReloadOutlined /></template>
        </a-button>
      </div>
      <div class="role-search">
        <a-input v-model:value="roleSearchKeyword" placeholder="搜索角色..." size="small" allow-clear>
          <template #prefix><SearchOutlined /></template>
        </a-input>
      </div>
      <div class="role-list" v-if="filteredRoles.length > 0">
        <div
          v-for="role in filteredRoles"
          :key="role.id"
          class="role-item"
          :class="{ 'role-item-active': selectedRole?.id === role.id }"
          @click="handleSelectRole(role)"
        >
          <div class="role-item-info">
            <span class="role-item-name">{{ role.roleName }}</span>
            <a-tag size="small" :color="role.scope === 'PLATFORM' ? 'purple' : 'blue'" style="font-size: 11px; line-height: 18px; padding: 0 4px;">
              {{ role.scope === 'PLATFORM' ? '平台' : '租户' }}
            </a-tag>
          </div>
          <div class="role-item-code">{{ role.roleCode }}</div>
        </div>
      </div>
      <div class="role-list-empty" v-else>
        <a-empty description="暂无角色" />
      </div>
    </div>

    <!-- 右侧：权限配置 -->
    <div class="permission-config-main">
      <template v-if="!selectedRole">
        <div class="no-role-selected">
          <SafetyOutlined style="font-size: 48px; color: #d9d9d9;" />
          <p>请从左侧选择一个角色进行权限配置</p>
        </div>
      </template>

      <template v-else>
        <!-- 配置头部 -->
        <div class="config-header">
          <div class="config-header-left">
            <h3>
              <SafetyOutlined />
              {{ selectedRole.roleName }}
              <a-tag :color="selectedRole.scope === 'PLATFORM' ? 'purple' : 'blue'">
                {{ selectedRole.scope === 'PLATFORM' ? '平台级' : '租户级' }}
              </a-tag>
            </h3>
            <span class="config-header-code">{{ selectedRole.roleCode }}</span>
          </div>
          <div class="config-header-actions">
            <a-button size="small" @click="handleCopyPermission" :disabled="!selectedRole">
              <template #icon><CopyOutlined /></template>
              从其他角色复制权限
            </a-button>
          </div>
        </div>

        <!-- 配置主体：模块侧栏 + 分组表格 -->
        <div class="config-body">
          <!-- 模块分类侧栏 -->
          <div class="module-sidebar">
            <div class="module-sidebar-title">功能分类</div>
            <div class="module-sidebar-search">
              <a-input v-model:value="moduleSearchKeyword" placeholder="搜索..." size="small" allow-clear />
            </div>
            <div class="module-sidebar-list">
              <div
                v-for="mod in filteredModuleCategories"
                :key="mod.key"
                class="module-sidebar-item"
                :class="{ 'module-sidebar-item-active': activeModule === mod.key }"
                @click="activeModule = mod.key"
              >
                <span class="module-sidebar-item-label">{{ mod.label }}</span>
                <span class="module-sidebar-item-count">{{ mod.groupCount }}</span>
              </div>
            </div>
          </div>

          <!-- 分组权限表格区域 -->
          <div class="grouped-tables-area">
            <div class="grouped-tables-toolbar">
              <span class="grouped-tables-title">权限分组</span>
              <a-space size="small">
                <a-button size="small" @click="expandAll" :disabled="allExpanded">
                  <template #icon><FolderOpenOutlined /></template>
                  展开全部
                </a-button>
                <a-button size="small" @click="collapseAll" :disabled="allCollapsed">
                  <template #icon><FolderOutlined /></template>
                  收起全部
                </a-button>
              </a-space>
            </div>

            <div class="tree-loading" v-if="loading">
              <a-spin tip="加载权限数据..." />
            </div>

            <template v-else>
              <div v-if="visibleGroups.length > 0" class="grouped-table-scroll">
                <div v-for="group in visibleGroups" :key="group.id" class="group-card">
                  <div class="group-header" @click="toggleGroupExpand(group.id)">
                    <a-checkbox
                      :checked="getGroupCheckedState(group).checked"
                      :indeterminate="getGroupCheckedState(group).indeterminate"
                      :disabled="readonly"
                      @change="toggleGroup(group)"
                      @click.stop
                    >
                      <span class="group-header-name">{{ group.name }}</span>
                    </a-checkbox>
                    <div class="group-header-right">
                      <span class="group-header-stats">{{ getGroupCheckedCount(group) }}/{{ group.permissions.length }}</span>
                      <CaretDownOutlined
                        class="group-collapse-icon"
                        :class="{ 'group-collapse-icon-collapsed': !expandedGroupIds.has(group.id) }"
                      />
                    </div>
                  </div>
                  <div v-show="expandedGroupIds.has(group.id)" class="group-table-body">
                    <a-table
                      :columns="groupColumns"
                      :data-source="group.permissions"
                      :pagination="false"
                      :show-header="true"
                      size="small"
                      row-key="id"
                      :scroll="{ x: 700 }"
                      :locale="{ emptyText: '暂无权限项' }"
                    >
                      <template #bodyCell="{ column, record }">
                        <template v-if="column.key === 'name'">
                          <div class="perm-name-cell" :class="{ 'cell-changed': changedIds.has(record.id) }">
                            <span class="perm-name-text">{{ record.name }}</span>
                            <code class="perm-code-hint">{{ record.code }}</code>
                          </div>
                        </template>
                        <template v-else-if="column.key === record.operationType">
                          <div :class="{ 'cell-changed': changedIds.has(record.id) }" class="perm-check-cell">
                            <a-checkbox
                              :checked="isChecked(record.id)"
                              :disabled="readonly"
                              @change="onCheckboxChange(record.id, $event)"
                            />
                          </div>
                        </template>
                      </template>
                    </a-table>
                  </div>
                </div>
              </div>
              <div v-else class="no-permissions">
                <a-empty :description="activeModule ? '该分类下没有权限项' : '暂无可配置的权限数据'" />
              </div>
            </template>
          </div>
        </div>

        <!-- 底部操作栏 -->
        <div class="bottom-action-bar" v-if="hasChanges && !readonly">
          <div class="bottom-action-bar-inner">
            <span class="change-summary">
              已修改 <b>{{ pendingChangeCount }}</b> 项权限
            </span>
            <div class="bottom-action-bar-buttons">
              <a-space>
                <a-button @click="handleResetPermissions">重置</a-button>
                <a-button type="primary" :loading="saveLoading" @click="handleSavePermissions">
                  <template #icon><SaveOutlined /></template>
                  保存权限
                </a-button>
              </a-space>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 从其他角色复制权限弹窗 -->
    <a-modal v-model:open="copyModalVisible" title="从其他角色复制权限" width="400px" :confirm-loading="copyLoading" @ok="handleCopyConfirm">
      <a-alert message="将把选中角色的权限完全覆盖当前角色的权限配置" type="warning" show-icon style="margin-bottom: 16px" />
      <a-select
        v-model:value="copySourceRoleId"
        style="width: 100%"
        placeholder="请选择要复制权限的源角色"
        :options="copyRoleOptions"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined, SafetyOutlined, SaveOutlined,
  CopyOutlined, SearchOutlined,
  FolderOpenOutlined, FolderOutlined, CaretDownOutlined,
} from '@ant-design/icons-vue'
import type { PermissionInfo } from '@/api/permission'
import type { RoleInfo } from '@/api/role'

// ==================== Props & Emits ====================
const props = withDefaults(defineProps<{
  /** 获取角色列表 */
  fetchRoles: () => Promise<{ data?: RoleInfo[] }>
  /** 获取权限树 */
  fetchPermissionTree: () => Promise<{ data?: PermissionInfo[] }>
  /** 获取角色已分配的权限 ID 列表 */
  fetchRolePermissions: (roleId: number) => Promise<{ data?: number[] }>
  /** 保存角色权限 */
  saveRolePermissions: (roleId: number, permissionIds: number[]) => Promise<any>
  /** 可选：分组过滤函数（用于租户模块过滤） */
  groupFilter?: (groups: PermissionGroup[]) => PermissionGroup[]
  /** 只读模式 */
  readonly?: boolean
}>(), {
  readonly: false,
})

const emit = defineEmits<{
  /** 保存成功 */
  'save-success': []
  /** 加载完成 */
  'loaded': []
}>()

// ==================== 操作列常量 ====================
const OPERATION_COLUMNS = ['查看', '新增', '编辑', '删除', '审核', '导出', '其他操作'] as const

const groupColumns: any[] = [
  { title: '权限项', dataIndex: 'name', key: 'name', width: 240, ellipsis: true },
  ...OPERATION_COLUMNS.map(op => ({
    title: op,
    dataIndex: op,
    key: op,
    width: 64,
    align: 'center' as const,
  })),
]

// ==================== 类型定义 ====================
interface PermissionRow {
  id: number
  name: string
  code: string
  operationType: string
}

interface PermissionGroup {
  id: number
  name: string
  code: string
  moduleKey: string
  permissions: PermissionRow[]
}

interface ModuleCategory {
  key: string
  label: string
  groupCount: number
}

// ==================== 左侧：角色列表 ====================
const roles = ref<RoleInfo[]>([])
const selectedRole = ref<RoleInfo | null>(null)
const roleSearchKeyword = ref('')
const loading = ref(false)
const saveLoading = ref(false)

const filteredRoles = computed(() => {
  const kw = roleSearchKeyword.value?.toLowerCase() || ''
  if (!kw) return roles.value
  return roles.value.filter(r =>
    r.roleName.toLowerCase().includes(kw) ||
    r.roleCode.toLowerCase().includes(kw)
  )
})

const loadRoles = async () => {
  try {
    const res = await props.fetchRoles()
    roles.value = res.data || []
  } catch (err) {
    console.warn('[权限配置] 加载角色列表失败', err)
    message.error('加载角色列表失败')
  }
}

// ==================== 权限分组提取 ====================
function getOperationType(code: string): string {
  const suffix = code.split(':').pop()?.toLowerCase() || ''
  if (['view', 'list', 'page', 'detail', 'get', 'query', 'search'].includes(suffix)) return '查看'
  if (['create', 'add', 'new', 'insert', 'batchAdd', 'batch-add'].includes(suffix)) return '新增'
  if (['update', 'edit', 'modify', 'save', 'batchUpdate', 'batch-update'].includes(suffix)) return '编辑'
  if (['delete', 'remove', 'del', 'batchDelete', 'batch-delete', 'clear'].includes(suffix)) return '删除'
  if (['audit', 'approve', 'review', 'check', 'reject', 'batchAudit', 'batch-audit'].includes(suffix)) return '审核'
  if (['export', 'excel', 'import', 'download', 'upload'].includes(suffix)) return '导出'
  return '其他操作'
}

function collectPermissionRows(node: PermissionInfo, result: PermissionRow[]) {
  if (!node.children) return
  for (const child of node.children) {
    if (child.permissionType === 2 || child.permissionType === 3) {
      result.push({
        id: child.id,
        name: child.permissionName,
        code: child.permissionCode,
        operationType: getOperationType(child.permissionCode),
      })
    } else if (child.children?.length) {
      collectPermissionRows(child, result)
    }
  }
}

function extractGroups(tree: PermissionInfo[]): PermissionGroup[] {
  const groups: PermissionGroup[] = []

  const walk = (nodes: PermissionInfo[]) => {
    for (const node of nodes) {
      if (node.permissionType !== 0 && node.permissionType !== 1) continue
      if (!node.children?.length) continue

      const hasButtonChildren = node.children.some(c => c.permissionType === 2 || c.permissionType === 3)
      if (!hasButtonChildren) {
        walk(node.children)
        continue
      }

      const rows: PermissionRow[] = []
      collectPermissionRows(node, rows)
      if (rows.length === 0) continue

      const moduleKey = node.permissionCode.split(':')[0]
      groups.push({
        id: node.id,
        name: node.permissionName,
        code: node.permissionCode,
        moduleKey,
        permissions: rows,
      })

      walk(node.children)
    }
  }

  walk(tree)
  return groups
}

// ==================== 权限树与分组 ====================
const permissionTree = ref<PermissionInfo[]>([])

const allPermissionGroups = computed<PermissionGroup[]>(() => {
  let groups = extractGroups(permissionTree.value)
  if (props.groupFilter) {
    groups = props.groupFilter(groups)
  }
  return groups
})

const activeModule = ref('')
const moduleSearchKeyword = ref('')

const moduleCategories = computed<ModuleCategory[]>(() => {
  const modMap = new Map<string, string>()
  for (const node of permissionTree.value) {
    if (node.permissionType === 0 || node.permissionType === 1) {
      const key = node.permissionCode.split(':')[0]
      if (!modMap.has(key)) {
        modMap.set(key, node.permissionName)
      }
    }
  }
  const mods: ModuleCategory[] = []
  for (const [key, label] of modMap) {
    const count = allPermissionGroups.value.filter(g => g.moduleKey === key).length
    if (count > 0) mods.push({ key, label, groupCount: count })
  }
  mods.sort((a, b) => b.groupCount - a.groupCount)
  return [{ key: '', label: '全部', groupCount: allPermissionGroups.value.length }, ...mods]
})

const filteredModuleCategories = computed(() => {
  const kw = moduleSearchKeyword.value?.toLowerCase() || ''
  if (!kw) return moduleCategories.value
  return moduleCategories.value.filter(m => m.label.toLowerCase().includes(kw))
})

const visibleGroups = computed(() => {
  let groups = allPermissionGroups.value
  if (activeModule.value) {
    groups = groups.filter(g => g.moduleKey === activeModule.value)
  }
  return groups
})

// ==================== 展开/收起 ====================
const expandedGroupIds = ref<Set<number>>(new Set())

watch(visibleGroups, (groups) => {
  // 新加载时默认全部展开
  if (expandedGroupIds.value.size === 0 && groups.length > 0) {
    expandedGroupIds.value = new Set(groups.map(g => g.id))
  }
}, { immediate: true })

const allExpanded = computed(() => {
  return visibleGroups.value.length > 0 && visibleGroups.value.every(g => expandedGroupIds.value.has(g.id))
})

const allCollapsed = computed(() => {
  return visibleGroups.value.length === 0 || visibleGroups.value.every(g => !expandedGroupIds.value.has(g.id))
})

function toggleGroupExpand(groupId: number) {
  const next = new Set(expandedGroupIds.value)
  if (next.has(groupId)) {
    next.delete(groupId)
  } else {
    next.add(groupId)
  }
  expandedGroupIds.value = next
}

function expandAll() {
  expandedGroupIds.value = new Set(visibleGroups.value.map(g => g.id))
}

function collapseAll() {
  expandedGroupIds.value = new Set()
}

const loadPermissionTree = async () => {
  loading.value = true
  try {
    const res = await props.fetchPermissionTree()
    permissionTree.value = res.data || []
  } catch (err) {
    console.warn('[权限配置] 加载权限树失败', err)
    message.error('加载权限数据失败')
  } finally {
    loading.value = false
  }
}

// ==================== 勾选状态管理 ====================
const checkedPermissionKeys = ref<number[]>([])
const originalCheckedKeys = ref<number[]>([])

const handleSelectRole = async (role: RoleInfo) => {
  selectedRole.value = role
  await loadRolePermissions(role.id)
}

const loadRolePermissions = async (roleId: number) => {
  loading.value = true
  try {
    const res = await props.fetchRolePermissions(roleId)
    const keys = res.data || []
    checkedPermissionKeys.value = keys
    originalCheckedKeys.value = [...keys]
  } catch (err) {
    console.warn('[权限配置] 加载角色权限失败', err)
    checkedPermissionKeys.value = []
    originalCheckedKeys.value = []
  } finally {
    loading.value = false
  }
}

function isChecked(id: number): boolean {
  return checkedPermissionKeys.value.includes(id)
}

function togglePermission(id: number, checked: boolean) {
  if (props.readonly) return
  if (checked) {
    if (!checkedPermissionKeys.value.includes(id)) {
      checkedPermissionKeys.value.push(id)
    }
  } else {
    checkedPermissionKeys.value = checkedPermissionKeys.value.filter(k => k !== id)
  }
}

/** Ant Design Checkbox change event wrapper */
function onCheckboxChange(id: number, event: any) {
  const checked = event?.target?.checked ?? event
  togglePermission(id, checked)
}

function toggleGroup(group: PermissionGroup) {
  if (props.readonly) return
  const groupIds = new Set(group.permissions.map(p => p.id))
  const allChecked = group.permissions.every(p => checkedPermissionKeys.value.includes(p.id))
  if (allChecked) {
    checkedPermissionKeys.value = checkedPermissionKeys.value.filter(id => !groupIds.has(id))
  } else {
    const currentSet = new Set(checkedPermissionKeys.value)
    for (const p of group.permissions) currentSet.add(p.id)
    checkedPermissionKeys.value = Array.from(currentSet)
  }
}

function getGroupCheckedState(group: PermissionGroup): { checked: boolean; indeterminate: boolean } {
  if (group.permissions.length === 0) return { checked: false, indeterminate: false }
  const checkedCount = group.permissions.filter(p => checkedPermissionKeys.value.includes(p.id)).length
  return {
    checked: checkedCount === group.permissions.length,
    indeterminate: checkedCount > 0 && checkedCount < group.permissions.length,
  }
}

function getGroupCheckedCount(group: PermissionGroup): number {
  return group.permissions.filter(p => checkedPermissionKeys.value.includes(p.id)).length
}

// ==================== 变更跟踪 ====================
const changedIds = computed(() => {
  const ids = new Set<number>()
  const orig = new Set(originalCheckedKeys.value)
  const curr = new Set(checkedPermissionKeys.value)
  for (const id of orig) if (!curr.has(id)) ids.add(id)
  for (const id of curr) if (!orig.has(id)) ids.add(id)
  return ids
})

const hasChanges = computed(() => changedIds.value.size > 0)
const pendingChangeCount = computed(() => changedIds.value.size)

// ==================== 保存与重置 ====================
const handleSavePermissions = async () => {
  if (!selectedRole.value) return
  saveLoading.value = true
  try {
    await props.saveRolePermissions(selectedRole.value.id, checkedPermissionKeys.value)
    originalCheckedKeys.value = [...checkedPermissionKeys.value]
    message.success('权限配置保存成功')
    emit('save-success')
  } catch (err: any) {
    message.error(err.message || '保存失败')
  } finally {
    saveLoading.value = false
  }
}

const handleResetPermissions = () => {
  checkedPermissionKeys.value = [...originalCheckedKeys.value]
}

// ==================== 角色复制 ====================
const copyModalVisible = ref(false)
const copyLoading = ref(false)
const copySourceRoleId = ref<number | undefined>(undefined)

const copyRoleOptions = computed(() => {
  return roles.value
    .filter(r => r.id !== selectedRole.value?.id)
    .map(r => ({ label: `${r.roleName} (${r.roleCode})`, value: r.id }))
})

const handleCopyPermission = () => {
  copySourceRoleId.value = undefined
  copyModalVisible.value = true
}

const handleCopyConfirm = async () => {
  if (!copySourceRoleId.value || !selectedRole.value) {
    message.warning('请选择源角色')
    return
  }
  copyLoading.value = true
  try {
    const res = await props.fetchRolePermissions(copySourceRoleId.value)
    const sourcePermissionIds = res.data || []
    await props.saveRolePermissions(selectedRole.value.id, sourcePermissionIds)
    checkedPermissionKeys.value = sourcePermissionIds
    originalCheckedKeys.value = [...sourcePermissionIds]
    message.success('权限复制成功')
    copyModalVisible.value = false
  } catch (err: any) {
    message.error(err.message || '复制失败')
  } finally {
    copyLoading.value = false
  }
}

// ==================== 初始化 ====================
// 当角色选择被外部重置时重新加载
watch(() => props.fetchRoles, () => {
  loadRoles()
})

// 在组件挂载后或 props 依赖更新时加载数据
const init = async () => {
  await Promise.all([loadRoles(), loadPermissionTree()])
  emit('loaded')
}

init()
</script>

<style scoped>
/* ==================== 左右布局 ==================== */
.permission-config-panel {
  height: 100%; display: flex; padding: 16px; gap: 16px; overflow: hidden;
}

/* ==================== 左侧角色列表 ==================== */
.role-sidebar {
  width: 260px; min-width: 260px;
  display: flex; flex-direction: column;
  border-right: 1px solid #f0f0f0;
  padding-right: 16px;
}
.role-sidebar-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.role-sidebar-header h3 { margin: 0; font-size: 14px; font-weight: 600; }
.role-search { margin-bottom: 12px; }
.role-list { flex: 1; overflow-y: auto; }
.role-list-empty { flex: 1; display: flex; align-items: center; justify-content: center; }
.role-item {
  padding: 10px 12px; border-radius: 6px; cursor: pointer;
  transition: all 0.2s; margin-bottom: 4px;
  border: 1px solid transparent;
}
.role-item:hover { background: #f5f7fa; }
.role-item-active { background: #e6f7ff; border-color: #91d5ff; }
.role-item-info { display: flex; align-items: center; gap: 8px; }
.role-item-name { font-size: 13px; font-weight: 500; color: #303133; }
.role-item-code { font-size: 11px; color: #999; margin-top: 2px; }

/* ==================== 右侧权限配置 ==================== */
.permission-config-main {
  flex: 1; display: flex; flex-direction: column; overflow: hidden;
}
.no-role-selected {
  flex: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center; color: #999;
}
.no-role-selected p { margin-top: 16px; font-size: 14px; }

.config-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 12px; padding-bottom: 12px; border-bottom: 1px solid #f0f0f0;
}
.config-header-left { display: flex; align-items: center; gap: 12px; }
.config-header-left h3 { margin: 0; font-size: 16px; display: flex; align-items: center; gap: 8px; }
.config-header-code { font-size: 12px; color: #999; }
.config-header-actions { display: flex; gap: 8px; }

/* ==================== 配置主体：模块侧栏 + 分组表格 ==================== */
.config-body {
  flex: 1; display: flex; gap: 0; overflow: hidden;
  min-height: 0;
}

/* ---- 模块分类侧栏 ---- */
.module-sidebar {
  width: 160px; min-width: 160px;
  display: flex; flex-direction: column;
  border-right: 1px solid #f0f0f0;
  padding-right: 12px;
  overflow: hidden;
}
.module-sidebar-title {
  font-size: 12px; font-weight: 600; color: #606266;
  margin-bottom: 8px; text-transform: uppercase; letter-spacing: 1px;
}
.module-sidebar-search { margin-bottom: 8px; }
.module-sidebar-list { flex: 1; overflow-y: auto; }
.module-sidebar-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 10px; border-radius: 6px; cursor: pointer;
  transition: all 0.15s; margin-bottom: 2px;
  font-size: 13px; color: #606266;
}
.module-sidebar-item:hover { background: #f5f7fa; color: #303133; }
.module-sidebar-item-active {
  background: #e6f7ff; color: #1890ff; font-weight: 500;
}
.module-sidebar-item-label { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.module-sidebar-item-count {
  font-size: 11px; color: #999; background: #f5f5f5;
  padding: 0 6px; border-radius: 8px; min-width: 18px; text-align: center;
  margin-left: 4px;
}
.module-sidebar-item-active .module-sidebar-item-count {
  background: #bae7ff; color: #1890ff;
}

/* ---- 分组表格区域 ---- */
.grouped-tables-area {
  flex: 1; display: flex; flex-direction: column; overflow: hidden;
  padding-left: 12px;
}
.grouped-tables-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 8px;
}
.grouped-tables-title {
  font-size: 12px; font-weight: 600; color: #606266;
  text-transform: uppercase; letter-spacing: 1px;
}
.grouped-table-scroll {
  flex: 1; overflow-y: auto; overflow-x: hidden;
}

/* ---- 分组卡片 ---- */
.group-card {
  border: 1px solid #e8e8e8; border-radius: 8px;
  margin-bottom: 12px; overflow: hidden;
}
.group-card:last-child { margin-bottom: 0; }

.group-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 16px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  user-select: none;
  transition: background 0.15s;
  cursor: pointer;
}
.group-header:hover { background: #f0f5ff; }
.group-header-name {
  font-size: 14px; font-weight: 500; color: #303133;
  margin-left: 6px;
}
.group-header-right {
  display: flex; align-items: center; gap: 8px;
}
.group-header-stats { font-size: 12px; color: #999; }
.group-collapse-icon {
  font-size: 12px; color: #999;
  transition: transform 0.2s;
}
.group-collapse-icon-collapsed {
  transform: rotate(-90deg);
}
.group-table-body {
  transition: all 0.2s;
}

.group-card :deep(.ant-table) { margin: 0; }
.group-card :deep(.ant-table-thead > tr > th) {
  background: #fafafa; font-size: 12px; color: #606266;
  padding: 6px 8px; white-space: nowrap;
  border-bottom: 1px solid #f0f0f0;
}
.group-card :deep(.ant-table-tbody > tr > td) {
  padding: 6px 8px;
  border-bottom: 1px solid #fafafa;
}
.group-card :deep(.ant-table-tbody > tr:last-child > td) {
  border-bottom: none;
}
.group-card :deep(.ant-table-tbody > tr:hover > td) {
  background: #f5f7fa;
}

.perm-name-cell {
  display: flex; align-items: center; gap: 8px;
  min-height: 28px;
}
.perm-name-text { font-size: 13px; color: #303133; }
.perm-code-hint {
  font-size: 10px; color: #bbb; font-family: monospace;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  max-width: 160px;
}
.perm-check-cell {
  display: flex; justify-content: center; align-items: center;
  min-height: 28px;
}

/* ---- 变更高亮 ---- */
.cell-changed {
  background-color: #fffbe6 !important;
  border-radius: 3px;
}

/* ---- 加载 / 空状态 ---- */
.tree-loading { display: flex; justify-content: center; padding: 60px 0; }
.no-permissions {
  display: flex; justify-content: center; align-items: center;
  padding: 60px 0;
}

/* ==================== 底部操作栏 ==================== */
.bottom-action-bar {
  border-top: 1px solid #e8e8e8;
  background: #fff;
  padding: 0 16px;
  margin: 0 -16px -16px -16px;
  margin-top: 12px;
}
.bottom-action-bar-inner {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 0;
}
.change-summary { font-size: 13px; color: #606266; }
.change-summary b { color: #faad14; font-weight: 600; }
.bottom-action-bar-buttons { display: flex; gap: 8px; }

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .role-sidebar { width: 200px; min-width: 200px; }
  .module-sidebar { width: 120px; min-width: 120px; }
}
</style>
