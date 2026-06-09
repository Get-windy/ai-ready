<template>
  <PageContainer full-height>
    <template #header>
      <div class="role-page-header">
        <div class="role-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>角色管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="role-page-header-title">角色管理</h2>
        </div>
        <div class="role-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="fetchData">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <div class="role-management">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pagination.total }}</div>
            <div class="stat-card-label">角色总数</div>
          </div>
          <SafetyOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-active">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ activeCount }}</div>
            <div class="stat-card-label">启用角色</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-disabled">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ disabledCount }}</div>
            <div class="stat-card-label">停用角色</div>
          </div>
          <StopOutlined class="stat-card-icon" />
        </div>
      </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :row-key="'id'"
      :filter-fields="filterFields"
      :show-search="false"
      :selectable="false"
      add-text="新增角色"
      @add="handleAdd"
      @edit="handleEdit"
      @delete="handleDeleteConfirm"
      @refresh="fetchData"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
    >
      <template #roleNameCell="{ record }">
        <a-space>
          <a-tag :color="getRoleTypeColor(record.roleType)">
            {{ getRoleTypeName(record.roleType) }}
          </a-tag>
          <span>{{ record.roleName }}</span>
        </a-space>
      </template>
      <template #statusCell="{ record }">
        <a-switch :checked="record.status === 0" checked-children="启用" un-checked-children="停用" @change="(checked: string | boolean) => handleStatusChange(record, checked)" />
      </template>

      <template #action="{ record }">
        <a-space>
          <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
          <a-button type="link" size="small" @click="handlePermission(record)">权限</a-button>
          <a-button type="link" size="small" @click="handleMenu(record)">菜单</a-button>
          <a-button type="link" size="small" danger @click="handleDeleteConfirm(record)">删除</a-button>
        </a-space>
      </template>
    </VxeTableList>

    <!-- 角色表单弹窗 -->
    <a-modal v-model:open="modalVisible" :title="modalTitle" :confirm-loading="submittingLoading" width="600px" @ok="handleModalOk" @cancel="handleModalCancel">
      <a-form ref="formRef" :model="formState" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="角色名称" name="roleName">
          <a-input v-model:value="formState.roleName" placeholder="请输入角色名称" />
        </a-form-item>
        <a-form-item label="角色编码" name="roleCode">
          <a-input v-model:value="formState.roleCode" placeholder="请输入角色编码" :disabled="isEdit" />
        </a-form-item>
        <a-form-item label="角色类型" name="roleType">
          <a-select v-model:value="formState.roleType" placeholder="请选择角色类型">
            <a-select-option :value="0">系统角色</a-select-option>
            <a-select-option :value="1">自定义角色</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="排序" name="sort">
          <a-input-number v-model:value="formState.sort" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formState.status">
            <a-radio :value="0">正常</a-radio>
            <a-radio :value="1">停用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="formState.remark" placeholder="请输入备注" :rows="3" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 权限配置弹窗 -->
    <a-modal v-model:open="permissionModalVisible" title="配置权限" width="500px" :confirm-loading="permissionLoading" @ok="handlePermissionOk">
      <a-alert message="勾选需要分配给该角色的权限" type="info" show-icon style="margin-bottom: 16px" />
      <a-tree v-model:checked-keys="checkedPermissionKeys" :tree-data="permissionTree" checkable :default-expand-all="true" :selectable="false" />
    </a-modal>

    <!-- 菜单配置弹窗 -->
    <a-modal v-model:open="menuModalVisible" title="配置菜单" width="500px" :confirm-loading="menuLoading" @ok="handleMenuOk">
      <a-alert message="勾选需要分配给该角色的菜单" type="info" show-icon style="margin-bottom: 16px" />
      <a-tree v-model:checked-keys="checkedMenuKeys" :tree-data="menuTree" checkable :default-expand-all="true" :selectable="false" />
    </a-modal>
  </div>
</PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { SafetyOutlined, CheckCircleOutlined, StopOutlined, ReloadOutlined, SyncOutlined } from '@ant-design/icons-vue'
import { roleApi, type RoleInfo } from '@/api/role'
import menuApi from '@/api/menu'
import { useSubmitLock } from '@/composables'
import { useUserStore } from '@/stores/user'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import { PageContainer } from '@/components'

const userStore = useUserStore()
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null
const searchForm = reactive({ roleName: '', roleCode: '', status: undefined as number | undefined })

const tableData = ref<RoleInfo[]>([])
const loading = ref(false)

const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showQuickJumper: true, showTotal: (total: number) => `共 ${total} 条` })

// ── 统计数据 ────────────────────────────────────────────
const activeCount = computed(() => tableData.value.filter(r => r.status === 0).length)
const disabledCount = computed(() => tableData.value.filter(r => r.status === 1).length)

// ── 数据源 ────────────────────────────────────────────
const tableDataSource = tableData

const vxeColumns = computed(() => [
  { field: 'roleName', title: '角色信息', width: 200, slotName: 'roleNameCell' },
  { field: 'roleCode', title: '角色编码', width: 150 },
  { field: 'sort', title: '排序', width: 80 },
  { field: 'status', title: '状态', width: 100, slotName: 'statusCell' },
  { field: 'createTime', title: '创建时间', width: 160 },
  { field: 'remark', title: '备注', showOverflow: 'tooltip' },
  { type: 'action', title: '操作', width: 220, fixed: 'right' }
])

const filterFields: FilterField[] = [
  { key: 'roleName', label: '角色名称', type: 'input', placeholder: '请输入角色名称' },
  { key: 'roleCode', label: '角色编码', type: 'input', placeholder: '请输入角色编码' },
  { key: 'status', label: '状态', type: 'select', options: [{ label: '正常', value: 0 }, { label: '停用', value: 1 }] },
]

const modalVisible = ref(false)
const { isSubmitting: submittingLoading, withSubmitLock } = useSubmitLock()
const modalTitle = computed(() => isEdit.value ? '编辑角色' : '新增角色')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const formState = reactive({ id: 0, roleName: '', roleCode: '', roleType: 1, sort: 0, status: 0, remark: '' })
const formRules = {
  roleName: { required: true, message: '请输入角色名称', trigger: 'blur' },
  roleCode: [{ required: true, message: '请输入角色编码', trigger: 'blur' }, { pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/, message: '编码只能包含字母、数字和下划线', trigger: 'blur' }]
}

const permissionModalVisible = ref(false)
const permissionLoading = ref(false)
const checkedPermissionKeys = ref<number[]>([])
const permissionTree = ref<any[]>([])
const currentRoleId = ref(0)

const menuModalVisible = ref(false)
const menuLoading = ref(false)
const checkedMenuKeys = ref<number[]>([])
const menuTree = ref<any[]>([])

const fetchData = async () => {
  loading.value = true
  try {
    const res = await roleApi.getPage({ tenantId: userStore.tenantId, ...searchForm, current: pagination.current, size: pagination.pageSize })
    if (res.data) { tableData.value = res.data.records; pagination.total = res.data.total }
  } catch (err) {
    console.warn('[系统管理] 加载角色数据失败', err)
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) Object.assign(searchForm, { roleName: '', roleCode: '', status: undefined })
  else Object.assign(searchForm, filters)
  pagination.current = 1; fetchData()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page; pagination.pageSize = pageSize; fetchData()
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(formState, { id: 0, roleName: '', roleCode: '', roleType: 1, sort: 0, status: 0, remark: '' })
  modalVisible.value = true
}

const handleEdit = (record: RoleInfo) => {
  isEdit.value = true
  Object.assign(formState, { id: record.id, roleName: record.roleName, roleCode: record.roleCode, roleType: record.roleType, sort: record.sort, status: record.status, remark: record.remark })
  modalVisible.value = true
}

const handleModalOk = async () => {
  try {
    const result = await withSubmitLock(async () => {
      await formRef.value?.validate()
      if (isEdit.value) { await roleApi.update(formState.id, formState); message.success('更新成功') }
      else { await roleApi.create(formState); message.success('创建成功') }
      modalVisible.value = false; fetchData()
    })
    void result
  } catch (error: any) { if (error) message.error(error?.message || '操作失败') }
}

const handleModalCancel = () => { modalVisible.value = false; formRef.value?.resetFields() }

const handleDeleteConfirm = (record: RoleInfo) => {
  Modal.confirm({
    title: '确认删除', content: `确定要删除角色 "${record.roleName}" 吗？此操作不可撤销。`, okText: '确认删除', okType: 'danger', cancelText: '取消', centered: true,
    async onOk() {
      try { await roleApi.delete(record.id); message.success('删除成功'); fetchData() } catch (error: any) { message.error(error.message || '删除失败') }
    }
  })
}

const handleStatusChange = async (record: RoleInfo, checked: boolean) => {
  const newStatus = checked ? 0 : 1
  try { await roleApi.updateStatus(record.id, newStatus); message.success('状态更新成功'); fetchData() } catch (err) { console.warn('[系统管理] 更新角色状态失败', err); message.error('状态更新失败') }
}

const handlePermission = async (record: RoleInfo) => {
  currentRoleId.value = record.id
  try {
    const menuRes = await menuApi.getTree({})
    permissionTree.value = menuRes.data ? buildPermissionTree(menuRes.data) : []
  } catch (err) {
    permissionTree.value = []
    console.warn('[系统管理] 加载权限树失败', err)
    message.error('加载权限树失败')
  }
  try { const res = await roleApi.getPermissions(record.id); checkedPermissionKeys.value = res.data || [] } catch (err) { console.warn('[系统管理] 获取权限列表失败', err); checkedPermissionKeys.value = [] }
  permissionModalVisible.value = true
}

const buildPermissionTree = (menus: any[]): any[] => {
  return menus.map((m: any) => ({
    title: m.menuName,
    key: m.id,
    children: m.children?.length ? buildPermissionTree(m.children) : undefined
  }))
}

const handlePermissionOk = async () => {
  permissionLoading.value = true
  try { await roleApi.assignPermissions(currentRoleId.value, checkedPermissionKeys.value as number[]); message.success('权限配置成功'); permissionModalVisible.value = false } finally { permissionLoading.value = false }
}

const handleMenu = async (record: RoleInfo) => {
  currentRoleId.value = record.id
  try {
    const menuRes = await menuApi.getTree({})
    menuTree.value = menuRes.data ? buildPermissionTree(menuRes.data) : []
  } catch (err) {
    menuTree.value = []
    console.warn('[系统管理] 加载菜单树失败', err)
    message.error('加载菜单树失败')
  }
  try { const res = await roleApi.getMenus(record.id); checkedMenuKeys.value = res.data || [] } catch (err) { console.warn('[系统管理] 获取菜单列表失败', err); checkedMenuKeys.value = [] }
  menuModalVisible.value = true
}

const handleMenuOk = async () => {
  menuLoading.value = true
  try { await roleApi.assignMenus(currentRoleId.value, checkedMenuKeys.value as number[]); message.success('菜单配置成功'); menuModalVisible.value = false } finally { menuLoading.value = false }
}

const getRoleTypeColor = (type: number) => type === 0 ? 'blue' : 'green'
const getRoleTypeName = (type: number) => type === 0 ? '系统' : '自定义'

onMounted(() => {
  fetchData()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.role-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.role-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.role-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.role-page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.update-time {
  font-size: 12px;
  color: #999;
}
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

.role-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-active { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-disabled { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}





/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}
</style>
