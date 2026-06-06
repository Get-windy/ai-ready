<template>
  <div class="user-management">
    <TableList
      ref="tableRef"
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      :table-key="'system-user-list'"
      :filter-fields="filterFields"
      :show-search="false"
      add-text="新增用户"
      @add="handleAdd"
      @edit="handleEdit"
      @delete="handleDelete"
      @batch-delete="handleBatchDelete"
      @refresh="fetchData"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @selection-change="(keys: any) => { selectedRowKeys.value = keys as number[] }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'username'">
          <a-space>
            <a-avatar
              :src="record.avatar"
              :size="32"
            >
              {{ record.nickname?.charAt(0) || record.username?.charAt(0) }}
            </a-avatar>
            <div>
              <div class="user-name">
                {{ record.username }}
              </div>
              <div class="user-nickname">
                {{ record.nickname }}
              </div>
            </div>
          </a-space>
        </template>

        <template v-else-if="column.key === 'status'">
          <a-tag :color="record.status === 0 ? 'success' : 'warning'">
            {{ record.status === 0 ? '正常' : '停用' }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'userType'">
          <a-tag :color="getUserTypeColor(record.userType)">
            {{ getUserTypeName(record.userType) }}
          </a-tag>
        </template>

        <template v-else-if="column.key === 'tenantName'">
          <span>{{ tenantMap[record.tenantId] || `租户${record.tenantId}` }}</span>
        </template>

        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button
              type="link"
              size="small"
              @click="handleEdit(record)"
            >
              编辑
            </a-button>
            <a-button
              type="link"
              size="small"
              @click="handleAssignRole(record)"
            >
              分配角色
            </a-button>
            <a-dropdown>
              <a-button
                type="link"
                size="small"
              >
                更多<DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="handleResetPassword(record)">
                    <KeyOutlined /> 重置密码
                  </a-menu-item>
                  <a-menu-item @click="handleToggleStatus(record)">
                    <StopOutlined /> {{ record.status === 0 ? '停用' : '启用' }}
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item
                    danger
                    @click="handleDelete(record)"
                  >
                    <DeleteOutlined /> 删除
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </template>
    </TableList>

    <!-- 用户表单弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirm-loading="submittingLoading"
      width="600px"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item
          label="用户名"
          name="username"
        >
          <a-input
            v-model:value="formState.username"
            placeholder="请输入用户名"
            :disabled="isEdit"
          />
        </a-form-item>
        <a-form-item
          label="昵称"
          name="nickname"
        >
          <a-input
            v-model:value="formState.nickname"
            placeholder="请输入昵称"
          />
        </a-form-item>
        <a-form-item
          v-if="!isEdit"
          label="密码"
          name="password"
        >
          <a-input-password
            v-model:value="formState.password"
            placeholder="请输入密码"
          />
        </a-form-item>
        <a-form-item
          label="邮箱"
          name="email"
        >
          <a-input
            v-model:value="formState.email"
            placeholder="请输入邮箱"
          />
        </a-form-item>
        <a-form-item
          label="手机号"
          name="phone"
        >
          <a-input
            v-model:value="formState.phone"
            placeholder="请输入手机号"
          />
        </a-form-item>
        <a-form-item
          label="性别"
          name="gender"
        >
          <a-radio-group v-model:value="formState.gender">
            <a-radio :value="0">
              未知
            </a-radio>
            <a-radio :value="1">
              男
            </a-radio>
            <a-radio :value="2">
              女
            </a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item
          label="用户类型"
          name="userType"
        >
          <a-select
            v-model:value="formState.userType"
            placeholder="请选择用户类型"
          >
            <a-select-option :value="0">
              系统用户
            </a-select-option>
            <a-select-option :value="1">
              企业用户
            </a-select-option>
            <a-select-option :value="2">
              代理用户
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          v-if="userStore.isSystemUser"
          label="所属租户"
          name="tenantId"
        >
          <a-select
            v-model:value="formState.tenantId"
            placeholder="请选择租户"
          >
            <a-select-option
              v-for="tenant in tenantList"
              :key="tenant.id"
              :value="tenant.id"
            >
              {{ tenant.tenantName }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          label="状态"
          name="status"
        >
          <a-radio-group v-model:value="formState.status">
            <a-radio :value="0">
              正常
            </a-radio>
            <a-radio :value="1">
              停用
            </a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 分配角色弹窗 -->
    <a-modal
      v-model:open="roleModalVisible"
      title="分配角色"
      :confirm-loading="roleModalLoading"
      @ok="handleRoleModalOk"
    >
      <a-transfer
        v-model:target-keys="targetRoleKeys"
        :data-source="roleList"
        :titles="['可选角色', '已选角色']"
        :render="(item: any) => item.title"
        show-search
        :filter-option="filterRoleOption"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import {
  DownOutlined,
  KeyOutlined,
  StopOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import TableList, { type FilterField } from '@/components/TableList/TableList.vue'
import { userApi, type UserInfo, type TenantInfo } from '@/api/user'
import { roleApi, type RoleInfo } from '@/api/role'
import { useSubmitLock, useOptimisticUpdate } from '@/composables'
import { useUserStore } from '@/stores/user'

// 搜索表单
const userStore = useUserStore()
const searchForm = reactive({
  username: '',
  phone: '',
  status: undefined as number | undefined,
  tenantId: undefined as number | undefined
})

// 表格数据
const tableData = ref<UserInfo[]>([])
const loading = ref(false)
const selectedRowKeys = ref<number[]>([])
const { isSubmitting: batchDeleteLoading } = useSubmitLock()

const { executeOptimistic, isUndoing: isUndoInProgress } = useOptimisticUpdate<UserInfo>({
  dataList: tableData,
  showUndo: true,
  undoTimeout: 5000,
})

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// 表格列定义
const columns: any[] = [
  { title: '用户信息', key: 'username', width: 200 },
  { title: '手机号', dataIndex: 'phone', width: 120 },
  { title: '邮箱', dataIndex: 'email', width: 180, ellipsis: true },
  { title: '用户类型', key: 'userType', width: 100 },
  { title: '所属租户', key: 'tenantName', width: 120 },
  { title: '状态', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]

// 筛选字段
const tenantFilterOptions = computed(() =>
  tenantList.value.map(t => ({ label: t.tenantName, value: t.id }))
)

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

// 弹窗相关
const modalVisible = ref(false)
const { isSubmitting: submittingLoading, withSubmitLock } = useSubmitLock()
const modalTitle = computed(() => isEdit.value ? '编辑用户' : '新增用户')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const formState = reactive({
  id: 0,
  username: '',
  nickname: '',
  password: '',
  email: '',
  phone: '',
  gender: 0,
  userType: 2,
  tenantId: userStore.tenantId,
  status: 0
})

const formRules = {
  username: { required: true, message: '请输入用户名', trigger: 'blur' },
  nickname: { required: true, message: '请输入昵称', trigger: 'blur' },
  password: { required: true, message: '请输入密码', min: 6, trigger: 'blur' },
  email: [
    { required: false, type: 'email', message: '请输入有效邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { required: false, pattern: /^1[3-9]\d{9}$/, message: '请输入有效手机号', trigger: 'blur' }
  ],
}

// 角色分配相关
const roleModalVisible = ref(false)
const roleModalLoading = ref(false)
const roleList = ref<{ key: string; title: string }[]>([])
const targetRoleKeys = ref<string[]>([])
const currentUserId = ref(0)

// 租户相关
const tenantList = ref<TenantInfo[]>([])
const tenantMap = computed(() => {
  const map: Record<number, string> = {}
  tenantList.value.forEach((t) => {
    map[t.id] = t.tenantName
  })
  return map
})

const loadTenants = async () => {
  try {
    const res = await userApi.getTenants()
    if (res.data) {
      tenantList.value = res.data
    }
  } catch {
    // 静默失败，租户列表为空不影响主要功能
  }
}

// 数据加载
const fetchData = async () => {
  loading.value = true
  try {
    const res = await userApi.getPage({
      tenantId: userStore.tenantId,
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (error) {
    message.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索相关
const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  Object.assign(searchForm, { username: '', phone: '', status: undefined, tenantId: undefined })
  handleSearch()
}

// 筛选变化
const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) {
    Object.assign(searchForm, { username: '', phone: '', status: undefined, tenantId: undefined })
  } else {
    Object.assign(searchForm, filters)
  }
  pagination.current = 1
  fetchData()
}

// 分页变化
const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// 新增用户
const handleAdd = () => {
  isEdit.value = false
  Object.assign(formState, {
    id: 0,
    username: '',
    nickname: '',
    password: '',
    email: '',
    phone: '',
    gender: 0,
    userType: 2,
    tenantId: userStore.tenantId,
    status: 0
  })
  modalVisible.value = true
}

// 编辑用户
const handleEdit = (record: UserInfo) => {
  isEdit.value = true
  Object.assign(formState, {
    id: record.id,
    username: record.username,
    nickname: record.nickname,
    email: record.email,
    phone: record.phone,
    gender: record.gender,
    userType: record.userType,
    tenantId: record.tenantId,
    status: record.status
  })
  modalVisible.value = true
}

// 提交表单
const handleModalOk = async () => {
  try {
    const result = await withSubmitLock(async () => {
      await formRef.value?.validate()

      if (isEdit.value) {
        await userApi.update(formState.id, formState)
        message.success('更新成功')
      } else {
        await userApi.create(formState as any)
        message.success('创建成功')
      }

      modalVisible.value = false
      fetchData()
    })
    // result 为 undefined 表示操作被锁定（防重复提交），无需提示错误
    void result
  } catch (error: any) {
    // 跳过因锁定而提前返回的情况
    if (error) {
      message.error(error?.message || '操作失败')
    }
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
  formRef.value?.resetFields()
}

// 删除用户
const handleDelete = (record: UserInfo) => {
  // 深拷贝一份记录数据，用于撤销时重新创建
  const savedRecord = { ...record }

  Modal.confirm({
    title: '确认删除',
    content: `确定要删除用户 "${record.username}" 吗？`,
    async onOk() {
      return executeOptimistic(
        // 1) 乐观变更：从列表中移除该项
        (list) => list.filter((item) => item.id !== record.id),
        // 2) 回滚：恢复原始列表
        (originalList) => {
          tableData.value = originalList
        },
        // 3) API 调用
        () => userApi.delete(record.id),
        // 4) 撤销恢复：重新创建用户
        async () => {
          const createData = {
            ...savedRecord,
            password: '123456',
          } as Record<string, any>
          delete createData.id
          await userApi.create(createData as any)
          await fetchData()
        },
        // 5) 覆盖 actionName 以显示正确文案
        '删除'
      )
    },
  })
}

// 批量删除（乐观更新 + 撤销支持）
const handleBatchDelete = (deleteKeys?: number[]) => {
  if (batchDeleteLoading.value) return

  const idsToDelete = deleteKeys || [...selectedRowKeys.value] as number[]
  // 保存被删除的完整记录，用于撤销时恢复
  const deletedItems = tableData.value.filter((item) =>
    idsToDelete.includes(item.id)
  )

  Modal.confirm({
    title: '确认删除',
    content: `确定要删除选中的 ${idsToDelete.length} 个用户吗？`,
    async onOk() {
      batchDeleteLoading.value = true

      const result = await executeOptimistic(
        // 1) 乐观变更：批量移除选中项
        (list) => list.filter((item) => !idsToDelete.includes(item.id)),
        // 2) 回滚：恢复原始列表
        (originalList) => {
          tableData.value = originalList
        },
        // 3) API 调用
        () => userApi.batchDelete(idsToDelete),
        // 4) 撤销恢复：逐个重新创建被删用户
        async () => {
          for (const item of deletedItems) {
            try {
              const createData = {
                ...item,
                password: '123456',
              } as Record<string, any>
              delete createData.id
              await userApi.create(createData as any)
            } catch {
              // 单个恢复失败不中断其他恢复
            }
          }
          await fetchData()
        },
        // 5) 覆盖 actionName 以显示正确文案
        '删除'
      )

      if (result) {
        selectedRowKeys.value = []
      }

      batchDeleteLoading.value = false
    },
  })
}

// 重置密码
const handleResetPassword = (record: UserInfo) => {
  const genPwd = (len = 12) => {
    const uppers = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
    const lowers = 'abcdefghijklmnopqrstuvwxyz'
    const digits = '0123456789'
    const specials = '!@#$%'
    const all = uppers + lowers + digits + specials
    let pwd = ''
    pwd += uppers[Math.floor(Math.random() * uppers.length)]
    pwd += lowers[Math.floor(Math.random() * lowers.length)]
    pwd += digits[Math.floor(Math.random() * digits.length)]
    pwd += specials[Math.floor(Math.random() * specials.length)]
    for (let i = pwd.length; i < len; i++) {
      pwd += all[Math.floor(Math.random() * all.length)]
    }
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
    okText: '确认重置',
    cancelText: '取消',
    async onOk() {
      await userApi.resetPassword(record.id, newPassword)
      message.success('密码已重置')
    }
  })
}

// 切换状态（乐观更新：立即变更 UI，失败则回滚）
const handleToggleStatus = async (record: UserInfo) => {
  const newStatus = record.status === 0 ? 1 : 0

  await executeOptimistic(
    // 1) 乐观变更：立即更新列表中该项的状态
    (list) =>
      list.map((item) =>
        item.id === record.id ? { ...item, status: newStatus } : item
      ),
    // 2) 回滚：恢复原始状态
    (originalList) => {
      tableData.value = originalList
    },
    // 3) API 调用
    () => userApi.updateStatus(record.id, newStatus),
    // 4) 不需要撤销恢复（toggle 操作可逆），只需 actionName 覆盖
    undefined,
    '更新状态'
  )
}

// 分配角色
const handleAssignRole = async (record: UserInfo) => {
  currentUserId.value = record.id
  // 加载角色列表
  const res = await roleApi.getPage({ tenantId: userStore.tenantId, size: 100 })
  if (res.data) {
    roleList.value = res.data.records.map((r: RoleInfo) => ({
      key: String(r.id),
      title: r.roleName
    }))
  }
  try {
    const userRes = await userApi.getById(record.id)
    targetRoleKeys.value = userRes.data?.roleIds ? userRes.data.roleIds.map(String) : []
  } catch {
    targetRoleKeys.value = []
  }
  roleModalVisible.value = true
}

const handleRoleModalOk = async () => {
  roleModalLoading.value = true
  try {
    await userApi.assignRoles(currentUserId.value, targetRoleKeys.value)
    message.success('分配成功')
    roleModalVisible.value = false
  } finally {
    roleModalLoading.value = false
  }
}

const filterRoleOption = (input: string, option: any) => {
  return option.title.toLowerCase().includes(input.toLowerCase())
}

// 辅助函数
const getUserTypeColor = (type: number) => {
  const colors: Record<number, string> = { 0: 'gold', 1: 'blue', 2: 'green' }
  return colors[type] || 'default'
}

const getUserTypeName = (type: number) => {
  const names: Record<number, string> = { 0: '系统用户', 1: '企业用户', 2: '代理用户' }
  return names[type] || '未知'
}

onMounted(() => {
  fetchData()
  loadTenants()
})
</script>

<style scoped>
.user-management {
  padding: 0;
}

.user-name {
  font-weight: 500;
}

.user-nickname {
  font-size: 12px;
  color: #999;
}
</style>
