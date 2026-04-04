<template>
  <div class="user-management">
    <!-- 搜索区域 -->
    <a-card class="search-card" :bordered="false">
      <a-form layout="inline" :model="searchForm" class="search-form">
        <a-row :gutter="16" style="width: 100%">
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="用户名">
              <a-input v-model:value="searchForm.username" placeholder="请输入用户名" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="手机号">
              <a-input v-model:value="searchForm.phone" placeholder="请输入手机号" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="状态">
              <a-select v-model:value="searchForm.status" placeholder="请选择状态" allow-clear style="width: 100%">
                <a-select-option :value="0">正常</a-select-option>
                <a-select-option :value="1">停用</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item>
              <a-space>
                <a-button type="primary" @click="handleSearch">
                  <template #icon><SearchOutlined /></template>
                  搜索
                </a-button>
                <a-button @click="handleReset">
                  <template #icon><ReloadOutlined /></template>
                  重置
                </a-button>
              </a-space>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <!-- 表格区域 -->
    <a-card class="table-card" :bordered="false">
      <template #title>
        <div class="table-header">
          <span class="title">用户列表</span>
          <a-space>
            <a-button type="primary" @click="handleAdd">
              <template #icon><PlusOutlined /></template>
              新增用户
            </a-button>
            <a-button danger :disabled="!selectedRowKeys.length" @click="handleBatchDelete">
              <template #icon><DeleteOutlined /></template>
              批量删除
            </a-button>
          </a-space>
        </div>
      </template>
      
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-selection="{ selectedRowKeys, onChange: onSelectChange }"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'username'">
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
          
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 0 ? 'success' : 'error'">
              {{ record.status === 0 ? '正常' : '停用' }}
            </a-tag>
          </template>
          
          <template v-else-if="column.key === 'userType'">
            <a-tag :color="getUserTypeColor(record.userType)">
              {{ getUserTypeName(record.userType) }}
            </a-tag>
          </template>
          
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
              <a-button type="link" size="small" @click="handleAssignRole(record)">分配角色</a-button>
              <a-dropdown>
                <a-button type="link" size="small">
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
                    <a-menu-item danger @click="handleDelete(record)">
                      <DeleteOutlined /> 删除
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 用户表单弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirm-loading="modalLoading"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
      width="600px"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
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
          <a-select v-model:value="formState.userType" placeholder="请选择用户类型">
            <a-select-option :value="0">超级管理员</a-select-option>
            <a-select-option :value="1">管理员</a-select-option>
            <a-select-option :value="2">普通用户</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formState.status">
            <a-radio :value="0">正常</a-radio>
            <a-radio :value="1">停用</a-radio>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import type { TableProps, FormInstance } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  DeleteOutlined,
  DownOutlined,
  KeyOutlined,
  StopOutlined
} from '@ant-design/icons-vue'
import { userApi, type UserInfo } from '@/api/user'
import { roleApi, type RoleInfo } from '@/api/role'

// 搜索表单
const searchForm = reactive({
  username: '',
  phone: '',
  status: undefined as number | undefined
})

// 表格数据
const tableData = ref<UserInfo[]>([])
const loading = ref(false)
const selectedRowKeys = ref<number[]>([])

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
const columns: TableProps['columns'] = [
  { title: '用户信息', key: 'username', width: 200 },
  { title: '手机号', dataIndex: 'phone', width: 120 },
  { title: '邮箱', dataIndex: 'email', width: 180, ellipsis: true },
  { title: '用户类型', key: 'userType', width: 100 },
  { title: '状态', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' }
]

// 弹窗相关
const modalVisible = ref(false)
const modalLoading = ref(false)
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
  status: 0
})

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 角色分配相关
const roleModalVisible = ref(false)
const roleModalLoading = ref(false)
const roleList = ref<{ key: number; title: string }[]>([])
const targetRoleKeys = ref<number[]>([])
const currentUserId = ref(0)

// 数据加载
const fetchData = async () => {
  loading.value = true
  try {
    const res = await userApi.getPage({
      tenantId: 1,
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
  Object.assign(searchForm, { username: '', phone: '', status: undefined })
  handleSearch()
}

// 表格操作
const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  fetchData()
}

const onSelectChange = (keys: number[]) => {
  selectedRowKeys.value = keys
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
    status: record.status
  })
  modalVisible.value = true
}

// 提交表单
const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    modalLoading.value = true
    
    if (isEdit.value) {
      await userApi.update(formState.id, formState)
      message.success('更新成功')
    } else {
      await userApi.create(formState as any)
      message.success('创建成功')
    }
    
    modalVisible.value = false
    fetchData()
  } catch (error) {
    message.error('操作失败')
  } finally {
    modalLoading.value = false
  }
}

const handleModalCancel = () => {
  modalVisible.value = false
  formRef.value?.resetFields()
}

// 删除用户
const handleDelete = (record: UserInfo) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除用户 "${record.username}" 吗？`,
    async onOk() {
      await userApi.delete(record.id)
      message.success('删除成功')
      fetchData()
    }
  })
}

// 批量删除
const handleBatchDelete = () => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个用户吗？`,
    async onOk() {
      await userApi.batchDelete(selectedRowKeys.value)
      message.success('删除成功')
      selectedRowKeys.value = []
      fetchData()
    }
  })
}

// 重置密码
const handleResetPassword = (record: UserInfo) => {
  Modal.confirm({
    title: '重置密码',
    content: `确定要重置用户 "${record.username}" 的密码吗？`,
    async onOk() {
      await userApi.resetPassword(record.id, '123456')
      message.success('密码已重置为 123456')
    }
  })
}

// 切换状态
const handleToggleStatus = async (record: UserInfo) => {
  const newStatus = record.status === 0 ? 1 : 0
  await userApi.updateStatus(record.id, newStatus)
  message.success('状态更新成功')
  fetchData()
}

// 分配角色
const handleAssignRole = async (record: UserInfo) => {
  currentUserId.value = record.id
  // 加载角色列表
  const res = await roleApi.getPage({ tenantId: 1, size: 100 })
  if (res.data) {
    roleList.value = res.data.records.map((r: RoleInfo) => ({
      key: r.id,
      title: r.roleName
    }))
  }
  // TODO: 加载用户已有角色
  targetRoleKeys.value = []
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
  const colors: Record<number, string> = { 0: 'red', 1: 'orange', 2: 'blue' }
  return colors[type] || 'default'
}

const getUserTypeName = (type: number) => {
  const names: Record<number, string> = { 0: '超级管理员', 1: '管理员', 2: '普通用户' }
  return names[type] || '未知'
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.user-management {
  padding: 0;
}

.search-card {
  margin-bottom: 16px;
}

.search-form {
  margin-bottom: -24px;
}

.table-card :deep(.ant-card-head) {
  border-bottom: none;
  padding-bottom: 0;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.table-header .title {
  font-size: 16px;
  font-weight: 500;
}

.user-name {
  font-weight: 500;
}

.user-nickname {
  font-size: 12px;
  color: #999;
}

@media (max-width: 768px) {
  .search-form :deep(.ant-form-item) {
    margin-bottom: 16px;
  }
  
  .table-header {
    flex-direction: column;
    gap: 12px;
  }
}
</style>