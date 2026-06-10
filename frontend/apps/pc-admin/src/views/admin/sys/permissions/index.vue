<template>
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>系统权限配置</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="page-header-title">系统权限配置</h2>
        </div>
        <div class="page-header-right">
          <a-button size="small" v-permission="'system:permission:create'" @click="showPermissionDefDrawer = true">
            <template #icon><SettingOutlined /></template>
            管理权限定义
          </a-button>
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
        </div>
      </div>
    </template>

    <PermissionConfigPanel
      :fetch-roles="fetchRoles"
      :fetch-permission-tree="fetchPermissionTree"
      :fetch-role-permissions="fetchRolePermissions"
      :save-role-permissions="saveRolePermissions"
      @save-success="onSaveSuccess"
      @loaded="onPanelLoaded"
    />

    <!-- 权限定义管理抽屉（系统级功能） -->
    <a-drawer
      v-model:open="showPermissionDefDrawer"
      title="权限定义管理"
      placement="right"
      width="80%"
      :styles="{ body: { padding: 0, height: 'calc(100vh - 55px)', overflow: 'hidden' } }"
    >
      <div class="def-drawer-body">
        <div class="stat-cards">
          <div class="stat-card stat-total">
            <div class="stat-card-value">{{ defCounts.total }}</div>
            <div class="stat-card-label">权限总数</div>
          </div>
          <div class="stat-card stat-menu">
            <div class="stat-card-value">{{ defCounts.menu }}</div>
            <div class="stat-card-label">菜单权限</div>
          </div>
          <div class="stat-card stat-button">
            <div class="stat-card-value">{{ defCounts.button }}</div>
            <div class="stat-card-label">按钮权限</div>
          </div>
          <div class="stat-card stat-api">
            <div class="stat-card-value">{{ defCounts.api }}</div>
            <div class="stat-card-label">API权限</div>
          </div>
        </div>

        <VxeTableList
          ref="defTableRef"
          :columns="defColumns"
          :data-source="defData"
          :loading="defLoading"
          :pagination="null as any"
          row-key="id"
          :filter-fields="defFilterFields"
          :show-search="false"
          :show-add="false"
          :show-edit="false"
          :show-delete="false"
          :show-batch-delete="false"
          :selectable="false"
          @refresh="loadDefData"
          @filter-change="onDefFilter"
        >
          <template #toolbar-actions>
            <a-button type="primary" v-permission="'system:permission:create'" @click="openDefAdd(null)">
              <template #icon><PlusOutlined /></template>新增顶级权限
            </a-button>
            <a-button @click="toggleDefExpand">
              <template #icon><ExpandOutlined /></template>展开/折叠
            </a-button>
          </template>
          <template #empty>
            <a-empty v-if="!defError" />
            <a-result v-else status="error" title="加载失败">
              <template #extra><a-button @click="loadDefData">重新加载</a-button></template>
            </a-result>
          </template>
          <template #nameCell="{ record }">
            <a-space>
              <component :is="getIcon(record.icon)" v-if="record.icon" />
              <span>{{ record.permissionName }}</span>
              <a-tag :color="typeColor(record.permissionType)" size="small">{{ typeLabel(record.permissionType) }}</a-tag>
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
              <a-button type="link" size="small" v-permission="'system:permission:create'" @click="openDefAdd(record)">新增子权限</a-button>
              <a-button type="link" size="small" v-permission="'system:permission:update'" @click="openDefEdit(record)">编辑</a-button>
              <a-button type="link" size="small" danger v-permission="'system:permission:delete'" @click="openDefDelete(record)">删除</a-button>
            </a-space>
          </template>
        </VxeTableList>
      </div>

      <!-- 定义表单抽屉 -->
      <a-drawer
        v-model:open="defFormVisible"
        :title="defIsEdit ? '编辑权限' : '新增权限'"
        placement="right"
        width="480px"
        :footer-style="{ textAlign: 'right' }"
        :closable="true"
        @close="closeDefForm"
      >
        <a-form ref="defFormRef" :model="defForm" :rules="defRules" layout="vertical">
          <a-form-item v-if="!defIsTop" label="父级权限" name="parentId">
            <a-tree-select v-model:value="defForm.parentId" :tree-data="defParentTree" placeholder="请选择父级权限" :field-names="{ label: 'permissionName', value: 'id' }" tree-default-expand-all />
          </a-form-item>
          <a-form-item label="权限名称" name="permissionName">
            <a-input v-model:value="defForm.permissionName" placeholder="请输入权限名称" />
          </a-form-item>
          <a-form-item label="权限编码" name="permissionCode">
            <a-input v-model:value="defForm.permissionCode" placeholder="如：system:user:list" />
          </a-form-item>
          <a-form-item label="权限类型" name="permissionType">
            <a-select v-model:value="defForm.permissionType">
              <a-select-option :value="0">目录</a-select-option>
              <a-select-option :value="1">菜单</a-select-option>
              <a-select-option :value="2">按钮</a-select-option>
              <a-select-option :value="3">API</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="defForm.permissionType <= 1" label="路由路径" name="path">
            <a-input v-model:value="defForm.path" placeholder="/system/user" />
          </a-form-item>
          <a-form-item v-if="defForm.permissionType <= 1" label="组件路径" name="component">
            <a-input v-model:value="defForm.component" placeholder="@/views/system/user/index" />
          </a-form-item>
          <a-form-item v-if="defForm.permissionType === 3" label="API路径" name="apiPath">
            <a-input v-model:value="defForm.apiPath" placeholder="/api/user/list" />
          </a-form-item>
          <a-form-item v-if="defForm.permissionType === 3" label="请求方法" name="method">
            <a-select v-model:value="defForm.method">
              <a-select-option value="GET">GET</a-select-option>
              <a-select-option value="POST">POST</a-select-option>
              <a-select-option value="PUT">PUT</a-select-option>
              <a-select-option value="DELETE">DELETE</a-select-option>
              <a-select-option value="PATCH">PATCH</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item v-if="defForm.permissionType <= 1" label="图标" name="icon">
            <a-input v-model:value="defForm.icon" placeholder="UserOutlined" />
          </a-form-item>
          <a-form-item v-if="defForm.permissionType <= 1" label="是否显示" name="visible">
            <a-radio-group v-model:value="defForm.visible">
              <a-radio :value="1">显示</a-radio>
              <a-radio :value="0">隐藏</a-radio>
            </a-radio-group>
          </a-form-item>
          <a-form-item label="排序" name="sort">
            <a-input-number v-model:value="defForm.sort" :min="0" :max="9999" style="width: 100%" />
          </a-form-item>
          <a-form-item label="状态" name="status">
            <a-radio-group v-model:value="defForm.status">
              <a-radio :value="0">启用</a-radio>
              <a-radio :value="1">停用</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-form>
        <template #footer>
          <a-space>
            <a-button @click="closeDefForm">取消</a-button>
            <a-button type="primary" :loading="defFormLoading" @click="submitDefForm">保存</a-button>
          </a-space>
        </template>
      </a-drawer>
    </a-drawer>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  PlusOutlined, ExpandOutlined, SettingOutlined,
  UserOutlined, DashboardOutlined, FileTextOutlined
} from '@ant-design/icons-vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import PermissionConfigPanel from '@/components/PermissionConfigPanel/index.vue'
import { permissionApi, type PermissionInfo } from '@/api/permission'
import { roleApi } from '@/api/role'
import { useUserStore } from '@/stores/user'
import { PageContainer } from '@/components'

// ==================== 通用 ====================
const userStore = useUserStore()
const lastUpdateTime = ref('')

// ==================== PermissionConfigPanel Props ====================
const fetchRoles = async () => roleApi.listAll('PLATFORM')
const fetchPermissionTree = async () => permissionApi.getTree(userStore.tenantId || 1)
const fetchRolePermissions = async (roleId: number) => roleApi.getPermissions(roleId)
const saveRolePermissions = async (roleId: number, permissionIds: number[]) => roleApi.assignPermissions(roleId, permissionIds)

const onSaveSuccess = () => { lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN') }
const onPanelLoaded = () => { lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN') }

// ==================== 权限定义管理 ====================
const showPermissionDefDrawer = ref(false)
const defLoading = ref(false)
const defError = ref(false)
const defData = ref<PermissionInfo[]>([])
const defExpanded = ref<number[]>([])
const defTableRef = ref()

const defColumns = [
  { field: 'permissionName', title: '权限名称', width: 250, slotName: 'nameCell' },
  { field: 'permissionCode', title: '权限编码', width: 200, showOverflow: 'tooltip' },
  { field: 'path', title: '路由/API路径', width: 200, showOverflow: 'tooltip' },
  { field: 'sort', title: '排序', width: 80 },
  { field: 'status', title: '状态', width: 80, slotName: 'statusCell' },
  { field: 'visible', title: '显示', width: 80, slotName: 'visibleCell' },
  { type: 'action', title: '操作', width: 280, fixed: 'right' },
]

const defFilterFields: FilterField[] = [
  { key: 'permissionName', label: '权限名称', type: 'input', placeholder: '请输入权限名称' },
  { key: 'permissionType', label: '权限类型', type: 'select', options: [
    { label: '目录', value: 0 }, { label: '菜单', value: 1 }, { label: '按钮', value: 2 }, { label: 'API', value: 3 }
  ]},
  { key: 'status', label: '状态', type: 'select', options: [{ label: '启用', value: 0 }, { label: '停用', value: 1 }] },
]

function typeColor(t: number) {
  return ['blue', 'green', 'orange', 'purple'][t] || 'default'
}
function typeLabel(t: number) {
  return ['目录', '菜单', '按钮', 'API'][t] || ''
}

const loadDefData = async () => {
  defLoading.value = true
  defError.value = false
  try {
    const res = await permissionApi.getTree(userStore.tenantId || 1)
    if (res.data) {
      const seen = new Set<string>()
      let c = 0
      const fix = (items: any[]) => {
        for (const item of items) {
          const s = String(item.id)
          if (seen.has(s)) { item._rawId = item.id; item.id = --c }
          seen.add(s)
          if (item.children?.length) fix(item.children)
        }
      }
      fix(res.data)
      defData.value = res.data
      defExpanded.value = res.data.filter((x: any) => x.permissionType === 0).map((x: any) => x.id)
    }
  } catch {
    defError.value = true
    defData.value = []
  } finally { defLoading.value = false }
}

const onDefFilter = () => { loadDefData() }
const toggleDefExpand = () => {
  if (defExpanded.value.length) { defExpanded.value = [] }
  else { const k: number[] = []; const w = (n: PermissionInfo[]) => { for (const x of n) { k.push(x.id); if (x.children) w(x.children) } }; w(defData.value); defExpanded.value = k }
}

const defCounts = computed(() => {
  const flat: PermissionInfo[] = []; const w = (n: PermissionInfo[]) => { for (const x of n) { flat.push(x); if (x.children) w(x.children) } }; w(defData.value)
  return { total: flat.length, menu: flat.filter(x => x.permissionType <= 1).length, button: flat.filter(x => x.permissionType === 2).length, api: flat.filter(x => x.permissionType === 3).length }
})

// 定义表单
const defFormVisible = ref(false)
const defIsEdit = ref(false)
const defIsTop = ref(false)
const defFormLoading = ref(false)
const defFormRef = ref<FormInstance>()
const defParentTree = ref<PermissionInfo[]>([])

const defForm = reactive({
  id: 0, parentId: 0, tenantId: userStore.tenantId,
  permissionName: '', permissionCode: '', permissionType: 1,
  path: '', component: '', icon: '', apiPath: '', method: 'GET',
  sort: 0, visible: 1, status: 0,
})

const defRules = {
  permissionName: { required: true, message: '请输入权限名称', trigger: 'blur' },
  permissionCode: [{ required: true, message: '请输入权限编码', trigger: 'blur' }, { pattern: /^[a-zA-Z0-9:_-]+$/, message: '编码格式不正确', trigger: 'blur' }],
  permissionType: { required: true, message: '请选择权限类型', trigger: 'change' },
}

const loadParentTree = async () => {
  try { const r = await permissionApi.getTree(userStore.tenantId || 1); if (r.data) defParentTree.value = r.data } catch {}
}

const openDefAdd = (record: PermissionInfo | null) => {
  defIsEdit.value = false; defIsTop.value = record === null
  Object.assign(defForm, {
    id: 0, parentId: record ? (record._rawId ?? record.id) : 0, tenantId: userStore.tenantId,
    permissionName: '', permissionCode: '', permissionType: record ? (record.permissionType === 0 ? 1 : 2) : 0,
    path: '', component: '', icon: '', apiPath: '', method: 'GET', sort: 0, visible: 1, status: 0,
  })
  defFormVisible.value = true; loadParentTree()
}

const openDefEdit = (record: PermissionInfo) => {
  defIsEdit.value = true; defIsTop.value = record.parentId === 0
  Object.assign(defForm, {
    id: record._rawId ?? record.id, parentId: record.parentId, tenantId: record.tenantId,
    permissionName: record.permissionName, permissionCode: record.permissionCode,
    permissionType: record.permissionType, path: record.path, component: record.component,
    icon: record.icon, apiPath: record.apiPath, method: record.method,
    sort: record.sort, visible: record.visible, status: record.status,
  })
  defFormVisible.value = true; loadParentTree()
}

const submitDefForm = async () => {
  try {
    await defFormRef.value?.validate()
    defFormLoading.value = true
    if (defForm.id) { await permissionApi.update(defForm.id, defForm); message.success('更新成功') }
    else { await permissionApi.create(defForm); message.success('创建成功') }
    defFormVisible.value = false; loadDefData()
  } catch { message.error('操作失败') } finally { defFormLoading.value = false }
}

const closeDefForm = () => { defFormVisible.value = false; defFormRef.value?.resetFields() }

const openDefDelete = (record: PermissionInfo) => {
  Modal.confirm({
    title: '确认删除', content: `确定要删除权限 "${record.permissionName}" 及其子权限吗？`,
    okText: '确认删除', okType: 'danger', centered: true,
    async onOk() {
      try { await permissionApi.delete(record._rawId ?? record.id); message.success('删除成功'); loadDefData() }
      catch (err: any) { message.error(err.message || '删除失败') }
    },
  })
}

const iconMap: Record<string, any> = { UserOutlined, SettingOutlined, DashboardOutlined, FileTextOutlined }
const getIcon = (name: string) => iconMap[name] || null
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.page-header-left { display: flex; align-items: center; gap: 12px; }
.page-header-title { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-header-right { display: flex; align-items: center; gap: 12px; }
.update-time { font-size: 12px; color: #999; }
.stat-cards { display: flex; gap: 12px; padding: 12px 16px; }
.stat-card { flex: 1; display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-radius: 8px; }
.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-menu { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-button { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-api { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }
.stat-card-value { font-size: 18px; font-weight: 600; font-family: monospace; color: #333; }
.stat-card-label { font-size: 11px; color: #666; margin-top: 2px; }
.stat-card-icon { font-size: 24px; color: rgba(0,0,0,0.15); }
.def-drawer-body { height: 100%; display: flex; flex-direction: column; }
.def-drawer-body > :deep(.vxe-table-list-container) { flex: 1; min-height: 0; }
</style>
