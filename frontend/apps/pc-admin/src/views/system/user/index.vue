<template>
  <PageContainer full-height>
    <template #header>
      <div class="user-page-header">
        <div class="user-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>用户管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="user-page-header-title">用户管理</h2>
        </div>
        <div class="user-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <div class="user-management">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pagination.total }}</div>
            <div class="stat-card-label">用户总数</div>
          </div>
          <TeamOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-active">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ activeCount }}</div>
            <div class="stat-card-label">正常用户</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-disabled">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ disabledCount }}</div>
            <div class="stat-card-label">停用用户</div>
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
      :selectable="true"
      add-text="新增用户"
      add-permission="system:user:create"
      @add="handleAdd"
      @edit="handleEdit"
      @delete="handleDelete"
      @batch-delete="handleBatchDelete"
      @refresh="debounceClick('refresh', fetchData)"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @selection-change="(keys: any) => { selectedRowKeys.value = keys as number[] }"
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

      <template #usernameCell="{ record }">
        <a-space>
          <a-avatar :src="record.avatar" :size="32">
            {{ record.nickname?.charAt(0) || record.username?.charAt(0) }}
          </a-avatar>
          <div>
            <div class="user-name">{{ record.username }}</div>
            <div class="user-nickname">{{ record.nickname }}</div>
          </div>
        </a-space>
      </template>
      <template #statusCell="{ record }">
        <a-tag :color="record.status === 0 ? 'success' : 'warning'">
          {{ record.status === 0 ? '正常' : '停用' }}
        </a-tag>
      </template>
      <template #userTypeCell="{ record }">
        <a-tag :color="getUserTypeColor(record.userType)">
          {{ getUserTypeName(record.userType) }}
        </a-tag>
      </template>
      <template #tenantNameCell="{ record }">
        <span>{{ tenantMap[record.tenantId] || `租户${record.tenantId}` }}</span>
      </template>

      <template #action="{ record }">
        <a-space>
          <a-button type="link" size="small" v-permission="'system:user:update'" @click="handleEdit(record)">编辑</a-button>
          <a-button type="link" size="small" v-permission="'system:role:assign'" @click="handleAssignRole(record)">分配角色</a-button>
          <a-dropdown>
            <a-button type="link" size="small">更多<DownOutlined /></a-button>
            <template #overlay>
              <a-menu>
                <a-menu-item v-permission="'system:user:update'" @click="handleResetPassword(record)">
                  <KeyOutlined /> 重置密码
                </a-menu-item>
                <a-menu-item v-permission="'system:user:update'" @click="handleToggleStatus(record)">
                  <StopOutlined /> {{ record.status === 0 ? '停用' : '启用' }}
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item danger v-permission="'system:user:delete'" @click="handleDelete(record)">
                  <DeleteOutlined /> 删除
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>
    </VxeTableList>

    <!-- 用户表单弹窗 -->
    <FullScreenDetail :visible="modalVisible" :title="modalTitle" :save-loading="submittingLoading" :show-save-and-new="!isEdit" @save="handleModalOk" @close="handleFormClose" @save-and-new="handleFormSaveAndNew">
      <a-form ref="formRef" :model="formState" :rules="formRules" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="formState.username" placeholder="请输入用户名" :disabled="isEdit" />
        </a-form-item>
        <a-form-item label="昵称" name="nickname">
          <a-input v-model:value="formState.nickname" placeholder="请输入昵称" />
        </a-form-item>
        <a-form-item v-if="!isEdit" label="密码" name="password">
          <a-input-password v-model:value="formState.password" placeholder="请输入密码" />
        </a-form-item>
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="formState.email" placeholder="请输入邮箱" />
        </a-form-item>
        <a-form-item label="手机号" name="phone">
          <a-input v-model:value="formState.phone" placeholder="请输入手机号" />
        </a-form-item>
        <a-form-item label="性别" name="gender">
          <a-radio-group v-model:value="formState.gender">
            <a-radio :value="0">未知</a-radio>
            <a-radio :value="1">男</a-radio>
            <a-radio :value="2">女</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="用户类型" name="userType">
          <a-select v-model:value="formState.userType" size="small" placeholder="请选择用户类型">
            <a-select-option
              v-for="opt in userTypeOptions"
              :key="opt.value"
              :value="opt.value"
            >{{ opt.label }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formState.status">
            <a-radio :value="0">正常</a-radio>
            <a-radio :value="1">停用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </FullScreenDetail>

    <!-- 分配角色弹窗 -->
    <a-modal v-model:open="roleModalVisible" title="分配角色" :confirm-loading="roleModalLoading" @ok="handleRoleModalOk">
      <a-transfer v-model:target-keys="targetRoleKeys" :data-source="roleList" :titles="['可选角色', '已选角色']" :render="(item: any) => item.title" show-search :filter-option="filterRoleOption" />
    </a-modal>
  </div>
</PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onUnmounted, h } from 'vue'
import { useRouter, onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { DownOutlined, KeyOutlined, StopOutlined, DeleteOutlined, TeamOutlined, CheckCircleOutlined, ReloadOutlined, SyncOutlined, WarningOutlined } from '@ant-design/icons-vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import { userApi, type UserInfo, type TenantInfo } from '@/api/user'
import { roleApi, type RoleInfo } from '@/api/role'
import { dictItemApi } from '@/api/dict'
import { useSubmitLock, useOptimisticUpdate } from '@/composables'
import { useUserStore } from '@/stores/user'
import { PageContainer } from '@/components'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'

const userStore = useUserStore()
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
const hasError = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null
const searchForm = reactive({ username: '', phone: '', status: undefined as number | undefined, tenantId: undefined as number | undefined })

const tableData = ref<UserInfo[]>([])
const loading = ref(false)
const selectedRowKeys = ref<number[]>([])
const { isSubmitting: batchDeleteLoading } = useSubmitLock()

const { executeOptimistic, isUndoing: isUndoInProgress } = useOptimisticUpdate<UserInfo>({
  dataList: tableData, showUndo: true, undoTimeout: 5000,
})

const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showQuickJumper: true, showTotal: (total: number) => `共 ${total} 条` })

// ── 统计数据 ────────────────────────────────────────────
const activeCount = computed(() => tableData.value.filter(r => r.status === 0).length)
const disabledCount = computed(() => tableData.value.filter(r => r.status === 1).length)

// ── 数据源 ────────────────────────────────────────────
const tableDataSource = tableData

const vxeColumns = computed(() => [
  { field: 'username', title: '用户信息', width: 200, slotName: 'usernameCell' },
  { field: 'phone', title: '手机号', width: 120 },
  { field: 'email', title: '邮箱', width: 180, showOverflow: 'tooltip' },
  { field: 'userType', title: '用户类型', width: 100, slotName: 'userTypeCell' },
  { field: 'tenantName', title: '所属租户', width: 120, slotName: 'tenantNameCell' },
  { field: 'status', title: '状态', width: 80, align: 'center', slotName: 'statusCell' },
  { field: 'createTime', title: '创建时间', width: 160 },
  { type: 'action', title: '操作', width: 200, fixed: 'right' }
])

const tenantFilterOptions = computed(() => tenantList.value.map(t => ({ label: t.tenantName, value: t.id })))
const filterFields = computed<FilterField[]>(() => {
  const fields: FilterField[] = [
    { key: 'username', label: '用户名', type: 'input', placeholder: '请输入用户名' },
    { key: 'phone', label: '手机号', type: 'input', placeholder: '请输入手机号' },
    { key: 'status', label: '状态', type: 'select', options: [{ label: '正常', value: 0 }, { label: '停用', value: 1 }] },
  ]
  if (userStore.isSystemUser) {
    fields.push({ key: 'tenantId', label: '所属租户', type: 'select', options: tenantFilterOptions.value })
  }
  return fields
})

const modalVisible = ref(false)
const { isSubmitting: submittingLoading, withSubmitLock } = useSubmitLock()
const modalTitle = computed(() => isEdit.value ? '编辑用户' : '新增用户')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

// ── 用户类型选项（从API加载） ──────────────────────────
const userTypeOptions = ref<{ label: string; value: number }[]>([])

async function loadUserTypeOptions() {
  try {
    const res = await dictItemApi.getByDictCode('USER_TYPE')
    if (res.data) {
      userTypeOptions.value = res.data
        .sort((a, b) => a.sortOrder - b.sortOrder)
        .map(item => ({ label: item.itemName, value: Number(item.itemCode) }))
    }
  } catch (err) {
    console.warn('[用户管理] 加载用户类型失败', err)
  }
}

const formState = reactive({ id: 0, username: '', nickname: '', password: '', email: '', phone: '', gender: 0, userType: 2, tenantId: userStore.tenantId, status: 0 })
const formRules = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  nickname: { required: true, message: '请输入昵称', trigger: 'blur' },
  password: { required: true, message: '请输入密码', min: 6, trigger: 'blur' },
  email: [{ required: false, type: 'email', message: '请输入有效邮箱地址', trigger: 'blur' }],
  phone: [{ required: false, pattern: /^1[3-9]\d{9}$/, message: '请输入有效手机号', trigger: 'blur' }],
}

const roleModalVisible = ref(false)
const roleModalLoading = ref(false)
const roleList = ref<{ key: string; title: string }[]>([])
const targetRoleKeys = ref<string[]>([])
const currentUserId = ref(0)

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

const tenantList = ref<TenantInfo[]>([])
const tenantMap = computed(() => {
  const map: Record<number, string> = {}
  tenantList.value.forEach((t) => { map[t.id] = t.tenantName })
  return map
})

const loadTenants = async () => {
  try { const res = await userApi.getTenants(); if (res.data) tenantList.value = res.data } catch (err) { tenantList.value = []; console.warn('[系统管理] 加载租户列表失败', err); message.error('加载租户列表失败') }
}

const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res = await userApi.getPage({ tenantId: userStore.tenantId, ...searchForm, pageNum: pagination.current, pageSize: pagination.pageSize })
    if (res.data) { tableData.value = res.data.records; pagination.total = res.data.total }
  } catch (err) {
    hasError.value = true
    console.warn('[系统管理] 加载用户数据失败', err)
    tableData.value = []
    pagination.total = 0
  } finally { loading.value = false; lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN'); refreshLoading.value = false }
}

const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) Object.assign(searchForm, { username: '', phone: '', status: undefined, tenantId: undefined })
  else Object.assign(searchForm, filters)
  pagination.current = 1; fetchData()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page; pagination.pageSize = pageSize; fetchData()
}

const handleAdd = () => {
  isEdit.value = false
  Object.assign(formState, { id: 0, username: '', nickname: '', password: '', email: '', phone: '', gender: 0, userType: 2, tenantId: userStore.tenantId, status: 0 })
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady.value = true })
}

const handleEdit = (record: UserInfo) => {
  isEdit.value = true
  Object.assign(formState, { id: record.id, username: record.username, nickname: record.nickname, email: record.email, phone: record.phone, gender: record.gender, userType: record.userType, tenantId: record.tenantId, status: record.status })
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady.value = true })
}

const handleModalOk = async () => {
  try {
    const result = await withSubmitLock(async () => {
      await formRef.value?.validate()
      if (isEdit.value) { await userApi.update(formState.id, formState); message.success('更新成功') }
      else { await userApi.create(formState as any); message.success('创建成功') }
      modalVisible.value = false; fetchData()
    })
    void result
  } catch (error: any) { console.warn('[用户管理] 提交用户表单失败', error); if (error) message.error(error?.message || '操作失败') }
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
      await userApi.create(formState as any)
      message.success('创建成功')
      fetchData()
      handleAdd()
    })
    void result
  } catch (error: any) { if (error) message.error(error?.message || '操作失败') }
}

const handleDelete = (record: UserInfo) => {
  const savedRecord = { ...record }
  Modal.confirm({
    title: '确认删除', content: `确定要删除用户 "${record.username}" 吗？`,
    async onOk() {
      return executeOptimistic(
        (list) => list.filter((item) => item.id !== record.id),
        (originalList) => { tableData.value = originalList },
        () => userApi.delete(record.id),
        async () => { const createData = { ...savedRecord, password: '123456' } as Record<string, any>; delete createData.id; await userApi.create(createData as any); await fetchData() },
        '删除'
      )
    },
  })
}

const handleBatchDelete = (deleteKeys?: number[]) => {
  if (batchDeleteLoading.value) return
  const idsToDelete = deleteKeys || [...selectedRowKeys.value] as number[]
  const deletedItems = tableData.value.filter((item) => idsToDelete.includes(item.id))
  Modal.confirm({
    title: '确认删除', content: `确定要删除选中的 ${idsToDelete.length} 个用户吗？`,
    async onOk() {
      batchDeleteLoading.value = true
      const result = await executeOptimistic(
        (list) => list.filter((item) => !idsToDelete.includes(item.id)),
        (originalList) => { tableData.value = originalList },
        () => userApi.batchDelete(idsToDelete),
        async () => { for (const item of deletedItems) { try { const createData = { ...item, password: '123456' } as Record<string, any>; delete createData.id; await userApi.create(createData as any) } catch (err) { console.warn('[系统管理] 恢复用户失败', err) } }; await fetchData() },
        '删除'
      )
      if (result) selectedRowKeys.value = []
      batchDeleteLoading.value = false
    },
  })
}

const handleResetPassword = (record: UserInfo) => {
  const genPwd = (len = 12) => {
    const uppers = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'; const lowers = 'abcdefghijklmnopqrstuvwxyz'; const digits = '0123456789'; const specials = '!@#$%'; const all = uppers + lowers + digits + specials
    let pwd = ''
    pwd += uppers[Math.floor(Math.random() * uppers.length)]; pwd += lowers[Math.floor(Math.random() * lowers.length)]; pwd += digits[Math.floor(Math.random() * digits.length)]; pwd += specials[Math.floor(Math.random() * specials.length)]
    for (let i = pwd.length; i < len; i++) pwd += all[Math.floor(Math.random() * all.length)]
    return pwd.split('').sort(() => Math.random() - 0.5).join('')
  }
  const newPassword = genPwd()
  Modal.confirm({
    title: '重置密码',
    content: h('div', [
      h('p', { style: { marginBottom: '12px' } }, `确定要重置用户 "${record.username}" 的密码吗？`),
      h('div', { style: { padding: '12px', background: '#f5f5f5', borderRadius: '4px', fontSize: '13px' } }, [
        h('div', { style: { marginBottom: '4px', color: '#999' } }, '新密码（请立即告知用户）：'),
        h('div', { style: { fontFamily: 'monospace', fontSize: '16px', fontWeight: 'bold', color: '#1890ff', letterSpacing: '2px' } }, newPassword),
        h('div', { style: { marginTop: '8px', color: '#f5222d', fontSize: '12px' } }, '此密码仅在此处显示一次，关闭后将无法再次查看')
      ])
    ]),
    okText: '确认重置', cancelText: '取消',
    async onOk() { await userApi.resetPassword(record.id, newPassword); message.success('密码已重置') }
  })
}

const handleToggleStatus = async (record: UserInfo) => {
  const newStatus = record.status === 0 ? 1 : 0
  await executeOptimistic(
    (list) => list.map((item) => item.id === record.id ? { ...item, status: newStatus } : item),
    (originalList) => { tableData.value = originalList },
    () => userApi.updateStatus(record.id, newStatus),
    undefined,
    '更新状态'
  )
}

const handleAssignRole = async (record: UserInfo) => {
  currentUserId.value = record.id
  // 根据当前用户类型过滤角色 scope：系统用户看到 PLATFORM，租户用户看到 TENANT
  const scope = userStore.isSystemUser ? 'PLATFORM' : 'TENANT'
  const res = await roleApi.getPage({ tenantId: userStore.tenantId, scope, size: 100 })
  if (res.data) roleList.value = res.data.records.map((r: RoleInfo) => ({ key: String(r.id), title: r.roleName }))
  try {
    const userRes = await userApi.getById(record.id)
    targetRoleKeys.value = userRes.data?.roleIds ? userRes.data.roleIds.map(String) : []
  } catch (err) { console.warn('[系统管理] 获取用户角色失败', err); targetRoleKeys.value = [] }
  roleModalVisible.value = true
}

const handleRoleModalOk = async () => {
  roleModalLoading.value = true
  try { await userApi.assignRoles(currentUserId.value, targetRoleKeys.value); message.success('分配成功'); roleModalVisible.value = false } finally { roleModalLoading.value = false }
}

const filterRoleOption = (input: string, option: any) => option.title.toLowerCase().includes(input.toLowerCase())
const userTypeColorPalette = ['gold', 'blue', 'green']
const getUserTypeColor = (type: number) => {
  const idx = userTypeOptions.value.findIndex(o => o.value === type)
  return idx >= 0 ? userTypeColorPalette[idx % userTypeColorPalette.length] : 'default'
}
const getUserTypeName = (type: number) => {
  const found = userTypeOptions.value.find(o => o.value === type)
  return found ? found.label : '未知'
}

onMounted(() => {
  fetchData()
  loadTenants()
  loadUserTypeOptions()
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
</script>

<style scoped>
.user-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.user-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.user-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.user-page-header-right {
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

.user-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

.user-management > :deep(.vxe-table-list-container) {
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

.user-name { font-weight: 500; }
.user-nickname { font-size: 12px; color: #999; }




/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
.user-management :deep(.ant-input-sm),
.user-management :deep(.ant-input-number-sm),
.user-management :deep(.ant-select-single.ant-select-sm .ant-select-selector),
.user-management :deep(.ant-picker-small),
.user-management :deep(.ant-btn-sm) {
  height: 28px; line-height: 28px;
}
.user-management :deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
.user-management :deep(.ant-input-number-sm input) { height: 26px; }

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
</style>
