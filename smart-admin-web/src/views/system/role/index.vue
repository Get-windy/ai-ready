<template>
  <div class="role-management">
    <!-- 搜索区域 -->
    <a-card class="search-card" :bordered="false">
      <a-form layout="inline" :model="searchForm" class="search-form">
        <a-row :gutter="16" style="width: 100%">
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="角色名称">
              <a-input v-model:value="searchForm.roleName" placeholder="请输入角色名称" allow-clear />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :sm="12" :md="6">
            <a-form-item label="角色编码">
              <a-input v-model:value="searchForm.roleCode" placeholder="请输入角色编码" allow-clear />
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
          <span class="title">角色列表</span>
          <a-space>
            <a-button type="primary" @click="handleAdd">
              <template #icon><PlusOutlined /></template>
              新增角色
            </a-button>
          </a-space>
        </div>
      </template>
      
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'roleName'">
            <a-space>
              <a-tag :color="getRoleTypeColor(record.roleType)">
                {{ getRoleTypeName(record.roleType) }}
              </a-tag>
              <span>{{ record.roleName }}</span>
            </a-space>
          </template>
          
          <template v-else-if="column.key === 'status'">
            <a-switch
              :checked="record.status === 0"
              checked-children="启用"
              un-checked-children="停用"
              @change="(checked: boolean) => handleStatusChange(record, checked)"
            />
          </template>
          
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
              <a-button type="link" size="small" @click="handlePermission(record)">权限</a-button>
              <a-button type="link" size="small" @click="handleMenu(record)">菜单</a-button>
              <a-popconfirm
                title="确定要删除此角色吗？"
                @confirm="handleDelete(record)"
              >
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 角色表单弹窗 -->
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
    <a-modal
      v-model:open="permissionModalVisible"
      title="配置权限"
      width="500px"
      :confirm-loading="permissionLoading"
      @ok="handlePermissionOk"
    >
      <a-alert message="勾选需要分配给该角色的权限" type="info" show-icon style="margin-bottom: 16px" />
      <a-tree
        v-model:checked-keys="checkedPermissionKeys"
        :tree-data="permissionTree"
        checkable
        :default-expand-all="true"
        :selectable="false"
      />
    </a-modal>

    <!-- 菜单配置弹窗 -->
    <a-modal
      v-model:open="menuModalVisible"
      title="配置菜单"
      width="500px"
      :confirm-loading="menuLoading"
      @ok="handleMenuOk"
    >
      <a-alert message="勾选需要分配给该角色的菜单" type="info" show-icon style="margin-bottom: 16px" />
      <a-tree
        v-model:checked-keys="checkedMenuKeys"
        :tree-data="menuTree"
        checkable
        :default-expand-all="true"
        :selectable="false"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { TableProps, FormInstance } from 'ant-design-vue'
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined
} from '@ant-design/icons-vue'
import { roleApi, type RoleInfo } from '@/api/role'

// 搜索表单
const searchForm = reactive({
  roleName: '',
  roleCode: '',
  status: undefined as number | undefined
})

// 表格数据
const tableData = ref<RoleInfo[]>([])
const loading = ref(false)

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
  { title: '角色信息', key: 'roleName', width: 200 },
  { title: '角色编码', dataIndex: 'roleCode', width: 150 },
  { title: '排序', dataIndex: 'sort', width: 80 },
  { title: '状态', key: 'status', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '备注', dataIndex: 'remark', ellipsis: true },
  { title: '操作', key: 'action', width: 220, fixed: 'right' }
]

// 弹窗相关
const modalVisible = ref(false)
const modalLoading = ref(false)
const modalTitle = computed(() => isEdit.value ? '编辑角色' : '新增角色')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const formState = reactive({
  id: 0,
  roleName: '',
  roleCode: '',
  roleType: 1,
  sort: 0,
  status: 0,
  remark: ''
})

const formRules = {
  roleName: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  roleCode: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/, message: '编码只能包含字母、数字和下划线', trigger: 'blur' }
  ]
}

// 权限配置相关
const permissionModalVisible = ref(false)
const permissionLoading = ref(false)
const checkedPermissionKeys = ref<number[]>([])
const permissionTree = ref<any[]>([])
const currentRoleId = ref(0)

// 菜单配置相关
const menuModalVisible = ref(false)
const menuLoading = ref(false)
const checkedMenuKeys = ref<number[]>([])
const menuTree = ref<any[]>([])

// 数据加载
const fetchData = async () => {
  loading.value = true
  try {
    const res = await roleApi.getPage({
      tenantId: 1,
      ...searchForm,
      current: pagination.current,
      size: pagination.pageSize
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
  Object.assign(searchForm, { roleName: '', roleCode: '', status: undefined })
  handleSearch()
}

// 表格操作
const handleTableChange: TableProps['onChange'] = (pag) => {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 10
  fetchData()
}

// 新增角色
const handleAdd = () => {
  isEdit.value = false
  Object.assign(formState, {
    id: 0,
    roleName: '',
    roleCode: '',
    roleType: 1,
    sort: 0,
    status: 0,
    remark: ''
  })
  modalVisible.value = true
}

// 编辑角色
const handleEdit = (record: RoleInfo) => {
  isEdit.value = true
  Object.assign(formState, {
    id: record.id,
    roleName: record.roleName,
    roleCode: record.roleCode,
    roleType: record.roleType,
    sort: record.sort,
    status: record.status,
    remark: record.remark
  })
  modalVisible.value = true
}

// 提交表单
const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    modalLoading.value = true
    
    if (isEdit.value) {
      await roleApi.update(formState.id, formState)
      message.success('更新成功')
    } else {
      await roleApi.create(formState)
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

// 删除角色
const handleDelete = async (record: RoleInfo) => {
  try {
    await roleApi.delete(record.id)
    message.success('删除成功')
    fetchData()
  } catch (error: any) {
    message.error(error.message || '删除失败')
  }
}

// 状态切换
const handleStatusChange = async (record: RoleInfo, checked: boolean) => {
  const newStatus = checked ? 0 : 1
  try {
    await roleApi.updateStatus(record.id, newStatus)
    message.success('状态更新成功')
    fetchData()
  } catch (error) {
    message.error('状态更新失败')
  }
}

// 权限配置
const handlePermission = async (record: RoleInfo) => {
  currentRoleId.value = record.id
  // 加载权限树
  permissionTree.value = [
    {
      title: '系统管理',
      key: 1,
      children: [
        { title: '用户管理', key: 11 },
        { title: '角色管理', key: 12 },
        { title: '菜单管理', key: 13 },
        { title: '部门管理', key: 14 }
      ]
    },
    {
      title: '业务管理',
      key: 2,
      children: [
        { title: '客户管理', key: 21 },
        { title: '订单管理', key: 22 },
        { title: '产品管理', key: 23 }
      ]
    }
  ]
  // 加载已有权限
  try {
    const res = await roleApi.getPermissions(record.id)
    checkedPermissionKeys.value = res.data || []
  } catch (error) {
    checkedPermissionKeys.value = []
  }
  permissionModalVisible.value = true
}

const handlePermissionOk = async () => {
  permissionLoading.value = true
  try {
    await roleApi.assignPermissions(currentRoleId.value, checkedPermissionKeys.value as number[])
    message.success('权限配置成功')
    permissionModalVisible.value = false
  } finally {
    permissionLoading.value = false
  }
}

// 菜单配置
const handleMenu = async (record: RoleInfo) => {
  currentRoleId.value = record.id
  // 加载菜单树
  menuTree.value = [
    {
      title: '首页',
      key: 1
    },
    {
      title: '系统管理',
      key: 2,
      children: [
        { title: '用户管理', key: 21 },
        { title: '角色管理', key: 22 },
        { title: '菜单管理', key: 23 }
      ]
    },
    {
      title: '业务管理',
      key: 3,
      children: [
        { title: '客户管理', key: 31 },
        { title: '订单管理', key: 32 }
      ]
    }
  ]
  checkedMenuKeys.value = []
  menuModalVisible.value = true
}

const handleMenuOk = async () => {
  menuLoading.value = true
  try {
    await roleApi.assignMenus(currentRoleId.value, checkedMenuKeys.value as number[])
    message.success('菜单配置成功')
    menuModalVisible.value = false
  } finally {
    menuLoading.value = false
  }
}

// 辅助函数
const getRoleTypeColor = (type: number) => {
  return type === 0 ? 'blue' : 'green'
}

const getRoleTypeName = (type: number) => {
  return type === 0 ? '系统' : '自定义'
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.role-management {
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