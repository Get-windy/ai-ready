<template>
  <ErrorBoundary @error="handleError">
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
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            <span class="shortcut-hint"><kbd>Ctrl</kbd> + <kbd>N</kbd> 新增</span>
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)">
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

      <a-skeleton active v-if="loading && tableData.length === 0" :paragraph="{ rows: 8 }" style="padding: 24px;" />

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :row-key="'id'"
      :min-empty-rows="12"
      :filter-fields="filterFields"
      :show-search="false"
      :selectable="false"
      add-text="新增角色"
      add-permission="system:role:create"
      @add="handleAdd"
      @edit="handleEdit"
      @delete="handleDeleteConfirm"
      @refresh="debounceClick('refresh', fetchData)"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @cell-dblclick="handleView"
    >
      <template #empty>
      <a-empty v-if="!hasError" description="暂无数据" />
      <a-result v-else status="error" title="数据加载失败">
        <template #extra>
          <a-button type="primary" @click="debounceClick('refresh', fetchData)">
            <template #icon><ReloadOutlined /></template>
            重新加载
          </a-button>
        </template>
      </a-result>
    </template>

    <template #roleNameCell="{ record }">
        <a-space>
          <a-tag :color="getRoleTypeColor(record.roleType)">
            {{ getRoleTypeName(record.roleType) }}
          </a-tag>
          <span>{{ record.roleName }}</span>
        </a-space>
      </template>
      <template #scopeCell="{ record }">
        <a-tag :color="record.scope === 'PLATFORM' ? 'purple' : 'blue'">
          {{ record.scope === 'PLATFORM' ? '平台级' : '租户级' }}
        </a-tag>
      </template>
      <template #statusCell="{ record }">
        <a-switch :checked="record.status === 0" checked-children="启用" un-checked-children="停用" @change="(checked: string | boolean) => { if (typeof checked === 'boolean') handleStatusChange(record, checked) }" />
      </template>

      <template #action="{ record }">
        <a-space>
          <a-button type="link" size="small" v-permission="'system:role:update'" @click="handleEdit(record)">编辑</a-button>
          <a-button type="link" size="small" v-permission="'system:permission:assign'" @click="handlePermission(record)">权限</a-button>
          <a-button type="link" size="small" v-permission="'system:role:update'" @click="handleMenu(record)">菜单</a-button>
          <a-button type="link" size="small" v-permission="'system:role:update'" @click="handleBillType(record)">单据权限</a-button>
          <a-button type="link" size="small" danger v-permission="'system:role:delete'" @click="handleDeleteConfirm(record)">删除</a-button>
        </a-space>
      </template>
    </VxeTableList>

    <!-- 角色表单弹窗 -->
    <FullScreenDetail :visible="modalVisible" :title="modalTitle" :dirty="formDirty" :save-loading="submittingLoading" :show-save-and-new="!isEdit" @save="handleModalOk" @close="handleFormClose" @save-and-new="handleFormSaveAndNew">
      <a-form ref="formRef" :model="formState" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="角色名称" name="roleName">
          <a-input v-model:value="formState.roleName" placeholder="请输入角色名称" />
        </a-form-item>
        <a-form-item label="角色编码" name="roleCode">
          <a-input v-model:value="formState.roleCode" placeholder="请输入角色编码" :disabled="isEdit" />
        </a-form-item>
        <a-form-item label="角色类型" name="roleType">
          <a-select v-model:value="formState.roleType" size="small" placeholder="请选择角色类型">
            <a-select-option
              v-for="opt in roleTypeOptions"
              :key="opt.value"
              :value="opt.value"
            >{{ opt.label }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="作用域" name="scope">
          <a-select v-model:value="formState.scope" size="small" placeholder="请选择作用域">
            <a-select-option value="TENANT">租户级</a-select-option>
            <a-select-option value="PLATFORM">平台级</a-select-option>
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
    </FullScreenDetail>

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

    <!-- 单据类型权限配置弹窗 -->
    <a-modal v-model:open="billTypeModalVisible" title="配置单据类型权限" width="550px" :confirm-loading="billTypeLoading" @ok="handleBillTypeOk">
      <a-alert message="设置角色对每种单据类型的操作权限级别" type="info" show-icon style="margin-bottom: 16px" />
      <a-table :data-source="billTypeData" :columns="billTypeColumns" :pagination="false as any" size="small" bordered>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'level'">
            <a-select v-model:value="record.permissionLevel" style="width: 120px" size="small">
              <a-select-option :value="0">无权限</a-select-option>
              <a-select-option :value="1">查看</a-select-option>
              <a-select-option :value="2">编辑</a-select-option>
              <a-select-option :value="3">审核</a-select-option>
            </a-select>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { SafetyOutlined, CheckCircleOutlined, StopOutlined, ReloadOutlined, SyncOutlined, WarningOutlined } from '@ant-design/icons-vue'
import { roleApi, type RoleInfo } from '@/api/role'
import menuApi from '@/api/menu'
import { roleBillTypeApi, type BillTypeDetail } from '@/api/roleBillType'
import { dictItemApi } from '@/api/dict'
import { useSubmitLock } from '@/composables'
import { useUserStore } from '@/stores/user'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'

const userStore = useUserStore()
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
const hasError = ref(false)
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
  { field: 'scope', title: '作用域', width: 100, slotName: 'scopeCell' },
  { field: 'sort', title: '排序', width: 80 },
  { field: 'status', title: '状态', width: 100, slotName: 'statusCell' },
  { field: 'createTime', title: '创建时间', width: 160 },
  { field: 'remark', title: '备注', showOverflow: 'tooltip' },
  { type: 'action', title: '操作', width: 220, fixed: 'right' }
])

const filterFields: FilterField[] = [
  { key: 'roleName', label: '角色名称', type: 'input', placeholder: '请输入角色名称' },
  { key: 'roleCode', label: '角色编码', type: 'input', placeholder: '请输入角色编码' },
  { key: 'scope', label: '作用域', type: 'select', options: [{ label: '平台级', value: 'PLATFORM' }, { label: '租户级', value: 'TENANT' }] },
  { key: 'status', label: '状态', type: 'select', options: [{ label: '正常', value: 0 }, { label: '停用', value: 1 }] },
]

const modalVisible = ref(false)
const { isSubmitting: submittingLoading, withSubmitLock } = useSubmitLock()
const modalTitle = computed(() => isEdit.value ? '编辑角色' : '新增角色')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

// ── 角色类型选项（从API加载） ──────────────────────────
const roleTypeOptions = ref<{ label: string; value: number }[]>([])

async function loadRoleTypeOptions() {
  try {
    const res = await dictItemApi.getByDictCode('ROLE_TYPE')
    if (res.data) {
      roleTypeOptions.value = res.data
        .sort((a, b) => a.sortOrder - b.sortOrder)
        .map(item => ({ label: item.itemText, value: Number(item.itemValue) }))
    }
  } catch (err) {
    console.warn('[角色管理] 加载角色类型失败', err)
  }
}

const formState = reactive({ id: 0, roleName: '', roleCode: '', roleType: 1, scope: 'TENANT', sort: 0, status: 0, remark: '' })
const formRules: any = {
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

// ── 单据类型权限弹窗 ────────────────────────────────────────
const billTypeModalVisible = ref(false)
const billTypeLoading = ref(false)
const billTypeData = ref<BillTypeDetail[]>([])
const billTypeColumns = [
  { title: '单据类型', dataIndex: 'billTypeName', key: 'billTypeName', width: 150 },
  { title: '类型编码', dataIndex: 'billType', key: 'billType', width: 100 },
  { title: '权限级别', key: 'level', width: 180 },
]

// ── debounceClick ──────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: () => void) {
  if (clickLocks.get(key)) return
  clickLocks.set(key, true)
  try { fn() } finally { setTimeout(() => clickLocks.set(key, false), 300) }
}

// ── Keyboard shortcuts ─────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  if (['INPUT', 'TEXTAREA', 'SELECT'].includes(tag)) return
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) { e.preventDefault(); debounceClick('refresh', fetchData) }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleAdd() }
}

// ── Form dirty tracking ────────────────────────────────────
const initialFormSnapshot = ref('')
const watchReady = ref(false)
const formDirty = computed(() => {
  if (!watchReady.value) return false
  return initialFormSnapshot.value !== JSON.stringify(formState)
})
function saveFormSnapshot() {
  initialFormSnapshot.value = JSON.stringify(formState)
}

onBeforeRouteLeave((to, from, next) => {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认离开', content: '当前表单未保存，确定要离开吗？', okText: '确定', cancelText: '取消',
      onOk() { next() }, onCancel() { next(false) }
    })
  } else { next() }
})

const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res = await roleApi.getPage({ tenantId: userStore.tenantId, ...searchForm, current: pagination.current, size: pagination.pageSize } as any)
    if (res) { tableData.value = res.records || []; pagination.total = res.total || 0 }
  } catch (err) {
    hasError.value = true
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
  Object.assign(formState, { id: 0, roleName: '', roleCode: '', roleType: 1, scope: 'TENANT', sort: 0, status: 0, remark: '' })
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady.value = true })
}

const handleEdit = (record: RoleInfo) => {
  isEdit.value = true
  Object.assign(formState, { id: record.id, roleName: record.roleName, roleCode: record.roleCode, roleType: record.roleType, scope: record.scope || 'TENANT', sort: record.sort, status: record.status, remark: record.remark })
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady.value = true })
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

const handleFormClose = () => {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认关闭', content: '当前表单未保存，确定要关闭吗？', okText: '确定', cancelText: '取消',
      onOk() { modalVisible.value = false; watchReady.value = false; formRef.value?.resetFields() }
    })
  } else {
    modalVisible.value = false
    watchReady.value = false
    formRef.value?.resetFields()
  }
}

const handleFormSaveAndNew = async () => {
  try {
    const result = await withSubmitLock(async () => {
      await formRef.value?.validate()
      await roleApi.create(formState)
      message.success('创建成功')
      fetchData()
      handleAdd()
    })
    void result
  } catch (error: any) { if (error) message.error(error?.message || '操作失败') }
}

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

const handleBillType = async (record: RoleInfo) => {
  currentRoleId.value = record.id
  try {
    const res = await roleBillTypeApi.getRoleBillTypes(record.id)
    billTypeData.value = res.data || []
  } catch (err) {
    console.warn('[系统管理] 加载单据类型权限失败', err)
    message.error('加载单据类型权限失败')
    billTypeData.value = []
  }
  billTypeModalVisible.value = true
}

const handleBillTypeOk = async () => {
  billTypeLoading.value = true
  try {
    const assignments = billTypeData.value
      .filter(d => d.permissionLevel > 0)
      .map(d => ({ billType: d.billType, permissionLevel: d.permissionLevel }))
    await roleBillTypeApi.assignBillTypes(currentRoleId.value, assignments)
    message.success('单据类型权限配置成功')
    billTypeModalVisible.value = false
  } catch (err) {
    console.warn('[系统管理] 分配单据类型权限失败', err)
    message.error('分配失败')
  } finally {
    billTypeLoading.value = false
  }
}

const roleTypeColorMap: Record<number, string> = { 0: 'blue', 1: 'green' }
const getRoleTypeColor = (type: number) => roleTypeColorMap[type] || 'default'
const getRoleTypeName = (type: number) => {
  const found = roleTypeOptions.value.find(o => o.value === type)
  return found ? found.label : '未知'
}

onMounted(() => {
  fetchData()
  loadRoleTypeOptions()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  document.removeEventListener('keydown', handleKeydown)
})

defineExpose({ handleQuery: fetchData })

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
// 查看详情
const handleView = (record: any) => {}
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

.role-management > :deep(.vxe-table-list-container) {
  flex: 1;
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





/* ── VxeTable 表头边框线 2px ─────────────────────────── */
.role-management :deep(.vxe-table .vxe-header--row th) {
  border-bottom: 2px solid #e8e8e8 !important;
}
.role-management :deep(.vxe-table .vxe-header--row th:not(:last-child)) {
  border-right: 1px solid #e8e8e8 !important;
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}

/* ── FullScreenDetail form compact overrides ── */
.fsd-body .ant-form-item {
  margin-bottom: 12px !important;
}
.fsd-body .ant-form-item:last-child {
  margin-bottom: 0 !important;
}
.fsd-body .ant-input,
.fsd-body .ant-input-password,
.fsd-body .ant-input-number,
.fsd-body .ant-select,
.fsd-body .ant-picker,
.fsd-body .ant-tree-select,
.fsd-body .ant-cascader-picker {
  min-height: 28px !important;
  font-size: 13px !important;
}
.fsd-body .ant-form-item-label > label {
  font-size: 13px !important;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
.role-management :deep(.ant-input-sm),
.role-management :deep(.ant-input-number-sm),
.role-management :deep(.ant-select-single.ant-select-sm .ant-select-selector),
.role-management :deep(.ant-picker-small),
.role-management :deep(.ant-btn-sm) {
  height: 28px; line-height: 28px;
}
.role-management :deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
.role-management :deep(.ant-input-number-sm input) { height: 26px; }

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

</style>
