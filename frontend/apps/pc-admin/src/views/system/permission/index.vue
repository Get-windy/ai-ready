<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="permission-page-header">
        <div class="permission-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>权限配置</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="permission-page-header-title">权限配置</h2>
        </div>
        <div class="permission-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchDefData)()">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <a-button size="small" v-permission="'system:permission:create'" @click="showPermissionDefDrawer = true">
            <template #icon><SettingOutlined /></template>
            管理权限定义
          </a-button>
        </div>
      </div>
    </template>

    <!-- 权限配置面板（通用组件） -->
    <PermissionConfigPanel
      :fetch-roles="fetchRoles"
      :fetch-permission-tree="fetchPermissionTree"
      :fetch-role-permissions="fetchRolePermissions"
      :save-role-permissions="saveRolePermissions"
      @save-success="onSaveSuccess"
      @loaded="onPanelLoaded"
    />

    <!-- 权限定义管理抽屉（系统级功能，保留在原页面） -->
    <a-drawer
      v-model:open="showPermissionDefDrawer"
      title="权限定义管理"
      placement="right"
      width="80%"
      :styles="{ body: { padding: 0, height: 'calc(100vh - 55px)', overflow: 'hidden' } }"
    >
      <div class="permission-def-drawer-body">
        <!-- 统计卡片 -->
        <div class="stat-cards">
          <div class="stat-card stat-total">
            <div class="stat-card-body">
              <div class="stat-card-value">{{ permissionDefCount }}</div>
              <div class="stat-card-label">权限总数</div>
            </div>
            <SafetyOutlined class="stat-card-icon" />
          </div>
          <div class="stat-card stat-menu">
            <div class="stat-card-body">
              <div class="stat-card-value">{{ menuDefCount }}</div>
              <div class="stat-card-label">菜单权限</div>
            </div>
            <MenuOutlined class="stat-card-icon" />
          </div>
          <div class="stat-card stat-button">
            <div class="stat-card-body">
              <div class="stat-card-value">{{ buttonDefCount }}</div>
              <div class="stat-card-label">按钮权限</div>
            </div>
            <ControlOutlined class="stat-card-icon" />
          </div>
          <div class="stat-card stat-api">
            <div class="stat-card-body">
              <div class="stat-card-value">{{ apiDefCount }}</div>
              <div class="stat-card-label">API权限</div>
            </div>
            <ApiOutlined class="stat-card-icon" />
          </div>
        </div>

        <a-skeleton active v-if="defLoading && permissionDefData.length === 0" :paragraph="{ rows: 8 }" style="padding: 24px;" />

        <VxeTableList
          ref="defTableRef"
          :columns="defVxeColumns"
          :data-source="permissionDefData"
          :loading="defLoading"
          :pagination="null as any"
          row-key="id"
          :min-empty-rows="12"
          :filter-fields="defFilterFields"
          :show-search="false"
          :show-add="false"
          :show-edit="false"
          :show-delete="false"
          :show-batch-delete="false"
          :selectable="false"
          @refresh="fetchDefData"
          @filter-change="handleDefFilterChange"
        >
          <template #toolbar-actions>
            <a-button type="primary" v-permission="'system:permission:create'" @click="handleDefAdd(null)">
              <template #icon><PlusOutlined /></template>
              新增顶级权限
            </a-button>
            <a-button @click="handleDefExpandAll">
              <template #icon><ExpandOutlined /></template>
              展开/折叠
            </a-button>
          </template>

          <template #empty>
            <a-empty v-if="!defHasError" description="暂无数据" />
            <a-result v-else status="error" title="数据加载失败">
              <template #extra>
                <a-button type="primary" @click="fetchDefData">
                  <template #icon><ReloadOutlined /></template>
                  重新加载
                </a-button>
              </template>
            </a-result>
          </template>

          <template #permissionNameCell="{ record }">
            <a-space>
              <component :is="getIcon(record.icon)" v-if="record.icon" />
              <span>{{ record.permissionName }}</span>
              <a-tag v-if="record.permissionType === 0" color="blue">目录</a-tag>
              <a-tag v-else-if="record.permissionType === 1" color="green">菜单</a-tag>
              <a-tag v-else-if="record.permissionType === 2" color="orange">按钮</a-tag>
              <a-tag v-else-if="record.permissionType === 3" color="purple">API</a-tag>
            </a-space>
          </template>
          <template #statusCell="{ record }">
            <a-tag :color="record.status === 0 ? 'success' : 'error'">{{ record.status === 0 ? '启用' : '停用' }}</a-tag>
          </template>
          <template #visibleCell="{ record }">
            <a-tag :color="record.visible === 1 ? 'success' : 'default'">{{ record.visible === 1 ? '显示' : '隐藏' }}</a-tag>
          </template>

          <template #action="{ record }">
            <a-space>
              <a-button type="link" size="small" v-permission="'system:permission:create'" @click="handleDefAdd(record)">新增子权限</a-button>
              <a-button type="link" size="small" v-permission="'system:permission:update'" @click="handleDefEdit(record)">编辑</a-button>
              <a-button type="link" size="small" danger v-permission="'system:permission:delete'" @click="handleDefDelete(record)">删除</a-button>
            </a-space>
          </template>
        </VxeTableList>
      </div>

      <!-- 权限定义表单抽屉 -->
      <a-drawer
        v-model:open="defFormVisible"
        :title="defIsEdit ? '编辑权限' : '新增权限'"
        placement="right"
        width="480px"
        :footer-style="{ textAlign: 'right' }"
        :closable="true"
        @close="handleDefFormClose"
      >
        <a-form ref="defFormRef" :model="defFormState" :rules="defFormRules" layout="vertical">
          <a-form-item v-if="!defIsTopLevel" label="父级权限" name="parentId">
            <a-tree-select
              v-model:value="defFormState.parentId"
              :tree-data="defParentTreeData"
              placeholder="请选择父级权限"
              :field-names="{ label: 'permissionName', value: 'id' }"
              tree-default-expand-all
            />
          </a-form-item>
          <a-form-item label="权限名称" name="permissionName">
            <a-input v-model:value="defFormState.permissionName" placeholder="请输入权限名称" />
          </a-form-item>
          <a-form-item label="权限编码" name="permissionCode">
            <a-input v-model:value="defFormState.permissionCode" placeholder="如：system:user:list" />
          </a-form-item>
          <a-form-item label="权限类型" name="permissionType">
            <a-select v-model:value="defFormState.permissionType" placeholder="请选择权限类型" size="small">
              <a-select-option :value="0">目录</a-select-option>
              <a-select-option :value="1">菜单</a-select-option>
              <a-select-option :value="2">按钮</a-select-option>
              <a-select-option :value="3">API</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="defFormState.permissionType <= 1" label="路由路径" name="path">
            <a-input v-model:value="defFormState.path" placeholder="/system/user" />
          </a-form-item>
          <a-form-item v-if="defFormState.permissionType <= 1" label="组件路径" name="component">
            <a-input v-model:value="defFormState.component" placeholder="@/views/system/user/index" />
          </a-form-item>
          <a-form-item v-if="defFormState.permissionType === 3" label="API路径" name="apiPath">
            <a-input v-model:value="defFormState.apiPath" placeholder="/api/user/list" />
          </a-form-item>
          <a-form-item v-if="defFormState.permissionType === 3" label="请求方法" name="method">
            <a-select v-model:value="defFormState.method" placeholder="请选择请求方法" size="small">
              <a-select-option value="GET">GET</a-select-option>
              <a-select-option value="POST">POST</a-select-option>
              <a-select-option value="PUT">PUT</a-select-option>
              <a-select-option value="DELETE">DELETE</a-select-option>
              <a-select-option value="PATCH">PATCH</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="defFormState.permissionType <= 1" label="图标" name="icon">
            <a-input v-model:value="defFormState.icon" placeholder="UserOutlined" />
          </a-form-item>
          <a-form-item v-if="defFormState.permissionType <= 1" label="是否显示" name="visible">
            <a-radio-group v-model:value="defFormState.visible">
              <a-radio :value="1">显示</a-radio>
              <a-radio :value="0">隐藏</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="排序" name="sort">
            <a-input-number v-model:value="defFormState.sort" :min="0" :max="9999" style="width: 100%" />
          </a-form-item>
          <a-form-item label="状态" name="status">
            <a-radio-group v-model:value="defFormState.status">
              <a-radio :value="0">启用</a-radio>
              <a-radio :value="1">停用</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-form>
        <template #footer>
          <a-space>
            <a-button @click="handleDefFormClose">取消</a-button>
            <a-button type="primary" :loading="defFormLoading" @click="handleDefFormOk">保存</a-button>
          </a-space>
        </template>
      </a-drawer>
    </a-drawer>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  PlusOutlined, ExpandOutlined, SafetyOutlined,
  MenuOutlined, ControlOutlined, ApiOutlined, SettingOutlined,
  SyncOutlined, ReloadOutlined,
  UserOutlined, DashboardOutlined, FileTextOutlined
} from '@ant-design/icons-vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import PermissionConfigPanel from '@/components/PermissionConfigPanel/index.vue'
import { permissionApi, type PermissionInfo } from '@/api/permission'
import { roleApi, type RoleInfo } from '@/api/role'
import { useUserStore } from '@/stores/user'
import PageContainer from '@/components/PageContainer/PageContainer.vue'

// ==================== 通用 ====================
const userStore = useUserStore()
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ── 防抖工具 ────────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

// ==================== PermissionConfigPanel Props ====================
const fetchRoles = async () => {
  const res = await roleApi.listAll()
  return res
}

const fetchPermissionTree = async () => {
  const res = await permissionApi.getTree(userStore.tenantId || 1)
  return res
}

const fetchRolePermissions = async (roleId: number) => {
  const res = await roleApi.getPermissions(roleId)
  return res
}

const saveRolePermissions = async (roleId: number, permissionIds: number[]) => {
  return await roleApi.assignPermissions(roleId, permissionIds)
}

const onSaveSuccess = () => {
  lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
}

const onPanelLoaded = () => {
  lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
}

// ==================== 权限定义管理（抽屉内 CRUD） ====================
const showPermissionDefDrawer = ref(false)
const defLoading = ref(false)
const defHasError = ref(false)
const permissionDefData = ref<PermissionInfo[]>([])
const defExpandedKeys = ref<number[]>([])

const defVxeColumns = computed(() => [
  { field: 'permissionName', title: '权限名称', width: 250, slotName: 'permissionNameCell' },
  { field: 'permissionCode', title: '权限编码', width: 200, showOverflow: 'tooltip' },
  { field: 'path', title: '路由/API路径', width: 200, showOverflow: 'tooltip' },
  { field: 'sort', title: '排序', width: 80 },
  { field: 'status', title: '状态', width: 80, slotName: 'statusCell' },
  { field: 'visible', title: '显示', width: 80, slotName: 'visibleCell' },
  { type: 'action', title: '操作', width: 280, fixed: 'right' }
])

const defFilterFields: FilterField[] = [
  { key: 'permissionName', label: '权限名称', type: 'input', placeholder: '请输入权限名称' },
  { key: 'permissionType', label: '权限类型', type: 'select', options: [
    { label: '目录', value: 0 }, { label: '菜单', value: 1 }, { label: '按钮', value: 2 }, { label: 'API', value: 3 }
  ]},
  { key: 'status', label: '状态', type: 'select', options: [{ label: '启用', value: 0 }, { label: '停用', value: 1 }] },
]

const defSearchForm = ref({ permissionName: '', permissionType: undefined as number | undefined, status: undefined as number | undefined })

const fetchDefData = async () => {
  defLoading.value = true
  defHasError.value = false
  try {
    const res = await permissionApi.getTree(userStore.tenantId || 1)
    if (res.data) {
      // 修复 64 位 Long 精度
      const seenIds = new Set<string>()
      let dupCounter = 0
      const fixIds = (items: any[]) => {
        for (const item of items) {
          const idStr = String(item.id)
          if (seenIds.has(idStr)) { item._rawId = item.id; item.id = --dupCounter }
          seenIds.add(idStr)
          if (item.children?.length) fixIds(item.children)
        }
      }
      fixIds(res.data)
      permissionDefData.value = res.data
      defExpandedKeys.value = res.filter((item: any) => item.permissionType === 0).map((item: any) => item.id)
    }
  } catch (err) {
    defHasError.value = true
    permissionDefData.value = []
    console.warn('[权限定义] 加载失败', err)
    message.error('加载权限数据失败')
  } finally {
    defLoading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

const handleDefFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) Object.assign(defSearchForm.value, { permissionName: '', permissionType: undefined, status: undefined })
  else Object.assign(defSearchForm.value, filters)
  fetchDefData()
}

const handleDefExpandAll = () => {
  if (defExpandedKeys.value.length === 0) {
    const keys: number[] = []; const walk = (nodes: PermissionInfo[]) => { for (const n of nodes) { keys.push(n.id); if (n.children?.length) walk(n.children) } }; walk(permissionDefData.value); defExpandedKeys.value = keys
  } else { defExpandedKeys.value = [] }
}

// 权限定义表单
const defFormVisible = ref(false)
const defIsEdit = ref(false)
const defIsTopLevel = ref(false)
const defFormLoading = ref(false)
const defFormRef = ref<FormInstance>()
const defParentTreeData = ref<PermissionInfo[]>([])

const defFormState = ref({
  id: 0, parentId: 0, tenantId: userStore.tenantId,
  permissionName: '', permissionCode: '', permissionType: 1,
  path: '', component: '', icon: '', apiPath: '', method: 'GET',
  sort: 0, visible: 1, status: 0
})

const defFormRules: any = {
  permissionName: { required: true, message: '请输入权限名称', trigger: 'blur' },
  permissionCode: [
    { required: true, message: '请输入权限编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9:_-]+$/, message: '编码只能包含字母、数字、冒号、下划线和短横线', trigger: 'blur' }
  ],
  permissionType: { required: true, message: '请选择权限类型', trigger: 'change' },
}

const loadDefParentTree = async () => {
  try { const res = await permissionApi.getTree(userStore.tenantId || 1); if (res.data) defParentTreeData.value = res.data } catch (_) {}
}

const handleDefAdd = (record: PermissionInfo | null) => {
  defIsEdit.value = false; defIsTopLevel.value = record === null
  defFormState.value = {
    id: 0, parentId: record ? (record._rawId ?? record.id) : 0, tenantId: userStore.tenantId,
    permissionName: '', permissionCode: '', permissionType: record ? (record.permissionType === 0 ? 1 : 2) : 0,
    path: '', component: '', icon: '', apiPath: '', method: 'GET', sort: 0, visible: 1, status: 0
  }
  defFormVisible.value = true; loadDefParentTree()
}

const handleDefEdit = (record: PermissionInfo) => {
  defIsEdit.value = true; defIsTopLevel.value = record.parentId === 0
  defFormState.value = {
    id: record._rawId ?? record.id, parentId: record.parentId, tenantId: record.tenantId,
    permissionName: record.permissionName, permissionCode: record.permissionCode,
    permissionType: record.permissionType, path: record.path, component: record.component,
    icon: record.icon, apiPath: record.apiPath, method: record.method,
    sort: record.sort, visible: record.visible, status: record.status
  }
  defFormVisible.value = true; loadDefParentTree()
}

const handleDefFormOk = async () => {
  try {
    await defFormRef.value?.validate()
    defFormLoading.value = true
    if (defFormState.value.id) {
      await permissionApi.update(defFormState.value.id, defFormState.value)
      message.success('更新成功')
    } else {
      await permissionApi.create(defFormState.value)
      message.success('创建成功')
    }
    defFormVisible.value = false
    fetchDefData()
  } catch { message.error('操作失败') } finally { defFormLoading.value = false }
}

const handleDefFormClose = () => { defFormVisible.value = false; defFormRef.value?.resetFields() }

const handleDefDelete = (record: PermissionInfo) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除权限 "${record.permissionName}" 及其子权限吗？此操作不可撤销。`,
    okText: '确认删除', okType: 'danger', cancelText: '取消', centered: true,
    async onOk() {
      try { await permissionApi.delete(record._rawId ?? record.id); message.success('删除成功'); fetchDefData() }
      catch (err: any) { message.error(err.message || '删除失败') }
    }
  })
}

// 统计
const permissionDefCount = computed(() => { const r: PermissionInfo[] = []; const w = (n: PermissionInfo[]) => { for (const x of n) { r.push(x); if (x.children?.length) w(x.children) } }; w(permissionDefData.value); return r.length })
const menuDefCount = computed(() => { const r: PermissionInfo[] = []; const w = (n: PermissionInfo[]) => { for (const x of n) { r.push(x); if (x.children?.length) w(x.children) } }; w(permissionDefData.value); return r.filter(p => p.permissionType === 0 || p.permissionType === 1).length })
const buttonDefCount = computed(() => { const r: PermissionInfo[] = []; const w = (n: PermissionInfo[]) => { for (const x of n) { r.push(x); if (x.children?.length) w(x.children) } }; w(permissionDefData.value); return r.filter(p => p.permissionType === 2).length })
const apiDefCount = computed(() => { const r: PermissionInfo[] = []; const w = (n: PermissionInfo[]) => { for (const x of n) { r.push(x); if (x.children?.length) w(x.children) } }; w(permissionDefData.value); return r.filter(p => p.permissionType === 3).length })

// 图标映射
const iconMap: Record<string, any> = { UserOutlined, SettingOutlined, DashboardOutlined, FileTextOutlined, ApiOutlined }
const getIcon = (iconName: string) => iconMap[iconName] || null

// ── 键盘快捷键 ──────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) {
    e.preventDefault()
    debounceClick('refresh', fetchDefData)()
  }
}

onMounted(() => {
  fetchDefData()
  document.addEventListener('keydown', handleKeydown)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchDefData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
</script>

<style scoped>
.permission-page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.permission-page-header-left { display: flex; align-items: center; gap: 12px; }
.permission-page-header-title { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.permission-page-header-right { display: flex; align-items: center; gap: 12px; }
.update-time { font-size: 12px; color: #999; }
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

/* 统计卡片（抽屉内） */
.stat-cards { display: flex; gap: 12px; padding: 12px 16px; }
.stat-card { flex: 1; display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-radius: 8px; }
.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-menu { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-button { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-api { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }
.stat-card-value { font-size: 18px; font-weight: 600; font-family: monospace; color: #333; }
.stat-card-label { font-size: 11px; color: #666; margin-top: 2px; }
.stat-card-icon { font-size: 24px; color: rgba(0, 0, 0, 0.15); }

.permission-def-drawer-body { height: 100%; display: flex; flex-direction: column; }
.permission-def-drawer-body > :deep(.vxe-table-list-container) { flex: 1; min-height: 0; }

/* ── VxeTable 表头边框线 2px ─────────────────────────── */
.permission-def-drawer-body :deep(.vxe-table .vxe-header--row th) {
  border-bottom: 2px solid #e8e8e8 !important;
}
.permission-def-drawer-body :deep(.vxe-table .vxe-header--row th:not(:last-child)) {
  border-right: 1px solid #e8e8e8 !important;
}

@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

</style>
