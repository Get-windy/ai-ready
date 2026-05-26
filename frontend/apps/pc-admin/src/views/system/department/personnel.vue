<template>
  <div class="department-personnel">
    <!-- 搜索区域 -->
    <a-card
      class="search-card"
      :bordered="false"
    >
      <a-form
        layout="inline"
        :model="searchForm"
        class="search-form"
      >
        <a-row
          :gutter="16"
          style="width: 100%"
        >
          <a-col
            :xs="24"
            :sm="12"
            :md="6"
          >
            <a-form-item label="用户名">
              <a-input
                v-model:value="searchForm.username"
                placeholder="请输入用户名"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col
            :xs="24"
            :sm="12"
            :md="6"
          >
            <a-form-item label="手机号">
              <a-input
                v-model:value="searchForm.phone"
                placeholder="请输入手机号"
                allow-clear
              />
            </a-form-item>
          </a-col>
          <a-col
            :xs="24"
            :sm="12"
            :md="6"
          >
            <a-form-item>
              <a-space>
                <a-button
                  type="primary"
                  @click="handleSearch"
                >
                  <template #icon>
                    <SearchOutlined />
                  </template>
                  搜索
                </a-button>
                <a-button @click="handleReset">
                  <template #icon>
                    <ReloadOutlined />
                  </template>
                  重置
                </a-button>
              </a-space>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <!-- 表格区域 -->
    <a-card
      class="table-card"
      :bordered="false"
    >
      <template #title>
        <div class="table-header">
          <div class="title">
            <a-space>
              <span>部门人员</span>
              <a-tag
                v-if="currentDepartment"
                color="blue"
              >
                {{ currentDepartment.departmentName }}
              </a-tag>
            </a-space>
          </div>
          <a-space>
            <a-button
              type="primary"
              @click="handleAdd"
            >
              <template #icon>
                <PlusOutlined />
              </template>
              添加人员
            </a-button>
            <a-button @click="handleTransfer">
              <template #icon>
                <SwapOutlined />
              </template>
              人员调动
            </a-button>
            <a-button @click="handleBack">
              <template #icon>
                <ArrowLeftOutlined />
              </template>
              返回
            </a-button>
          </a-space>
        </div>
      </template>
      
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-selection="{ selectedRowKeys: selectedRowKeys as any, onChange: onSelectChange as any }"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'userInfo'">
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
            <a-tag :color="record.status === 0 ? 'success' : 'error'">
              {{ record.status === 0 ? '正常' : '停用' }}
            </a-tag>
          </template>
          
          <template v-else-if="column.key === 'position'">
            {{ record.positionName || '-' }}
          </template>
          
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button
                type="link"
                size="small"
                @click="handleTransferSingle(record as UserInfo)"
              >
                调动
              </a-button>
              <a-button
                type="link"
                size="small"
                danger
                @click="handleRemove(record as UserInfo)"
              >
                移除
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 添加人员弹窗 -->
    <a-modal
      v-model:open="addModalVisible"
      title="添加部门人员"
      :confirm-loading="addModalLoading"
      width="600px"
      @ok="handleAddModalOk"
      @cancel="handleAddModalCancel"
    >
      <a-form
        ref="addFormRef"
        :model="addFormState"
        :rules="addFormRules as any"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item
          label="选择人员"
          name="userIds"
        >
          <a-select
            v-model:value="addFormState.userIds"
            mode="multiple"
            placeholder="请选择要添加的人员"
            show-search
            :filter-option="filterUserOption"
          >
            <a-select-option
              v-for="user in availableUsers"
              :key="user.id"
              :value="user.id"
            >
              {{ user.nickname || user.username }} - {{ user.phone || '-' }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item
          label="岗位"
          name="positionId"
        >
          <a-select
            v-model:value="addFormState.positionId"
            placeholder="请选择岗位"
            allow-clear
          >
            <a-select-option
              v-for="pos in positionList"
              :key="pos.id"
              :value="pos.id"
            >
              {{ pos.positionName }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 人员调动弹窗 -->
    <a-modal
      v-model:open="transferModalVisible"
      title="人员调动"
      :confirm-loading="transferModalLoading"
      width="600px"
      @ok="handleTransferModalOk"
      @cancel="handleTransferModalCancel"
    >
      <div class="transfer-modal-content">
        <a-form
          ref="transferFormRef"
          :model="transferFormState"
          :rules="transferFormRules as any"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item
            label="目标部门"
            name="targetDepartmentId"
          >
            <a-tree-select
              v-model:value="transferFormState.targetDepartmentId"
              :tree-data="departmentTreeData"
              :field-names="{ children: 'children', label: 'departmentName', value: 'id' }"
              placeholder="请选择目标部门"
              allow-clear
              show-search
              tree-node-filter-prop="departmentName"
            />
          </a-form-item>
          <a-form-item label="调岗人员">
            <a-list
              size="small"
              :data-source="selectedUsers"
              :grid="{ gutter: 16, column: 2 }"
            >
              <template #renderItem="{ item }">
                <a-list-item>
                  <a-list-item-meta>
                    <template #title>
                      {{ item.nickname || item.username }}
                    </template>
                    <template #description>
                      {{ item.positionName || '-' }}
                    </template>
                  </a-list-item-meta>
                </a-list-item>
              </template>
            </a-list>
          </a-form-item>
          <a-form-item
            label="新岗位"
            name="newPositionId"
          >
            <a-select
              v-model:value="transferFormState.newPositionId"
              placeholder="请选择新岗位（可选）"
              allow-clear
            >
              <a-select-option
                v-for="pos in positionList"
                :key="pos.id"
                :value="pos.id"
              >
                {{ pos.positionName }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
      </div>
    </a-modal>

    <!-- 单人调动弹窗 -->
    <a-modal
      v-model:open="singleTransferModalVisible"
      title="人员调动"
      :confirm-loading="singleTransferModalLoading"
      width="600px"
      @ok="handleSingleTransferModalOk"
      @cancel="handleSingleTransferModalCancel"
    >
      <a-form
        ref="singleTransferFormRef"
        :model="singleTransferFormState"
        :rules="singleTransferFormRules as any"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="当前人员">
          <a-input
            :value="selectedUser?.nickname || selectedUser?.username"
            disabled
          />
        </a-form-item>
        <a-form-item
          label="目标部门"
          name="targetDepartmentId"
        >
          <a-tree-select
            v-model:value="singleTransferFormState.targetDepartmentId"
            :tree-data="departmentTreeData"
            :field-names="{ children: 'children', label: 'departmentName', value: 'id' }"
            placeholder="请选择目标部门"
            allow-clear
            show-search
            tree-node-filter-prop="departmentName"
          />
        </a-form-item>
        <a-form-item
          label="新岗位"
          name="newPositionId"
        >
          <a-select
            v-model:value="singleTransferFormState.newPositionId"
            placeholder="请选择新岗位（可选）"
            allow-clear
          >
            <a-select-option
              v-for="pos in positionList"
              :key="pos.id"
              :value="pos.id"
            >
              {{ pos.positionName }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
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
  SwapOutlined,
  ArrowLeftOutlined
} from '@ant-design/icons-vue'
import { userApi, type UserInfo } from '@/api/user'
import { departmentApi, type DepartmentInfo } from '@/api/department'
import { positionApi, type PositionInfo } from '@/api/position'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

// 当前部门
const currentDepartment = ref<DepartmentInfo | null>(null)

// 搜索表单
const searchForm = reactive({
  username: '',
  phone: ''
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
  { title: '用户信息', key: 'userInfo', width: 200 },
  { title: '手机号', dataIndex: 'phone', width: 120 },
  { title: '邮箱', dataIndex: 'email', width: 180, ellipsis: true },
  { title: '岗位', key: 'position', width: 150 },
  { title: '状态', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

// 添加人员弹窗
const addModalVisible = ref(false)
const addModalLoading = ref(false)
const addFormRef = ref<FormInstance>()

const addFormState = reactive({
  userIds: [] as number[],
  positionId: undefined as number | undefined
})

const addFormRules = {
  userIds: [{ required: true, message: '请选择人员', trigger: 'change' }]
}

// 可用用户列表（不在当前部门的用户）
const availableUsers = ref<UserInfo[]>([])

// 岗位列表
const positionList = ref<PositionInfo[]>([])

// 批量调动弹窗
const transferModalVisible = ref(false)
const transferModalLoading = ref(false)
const transferFormRef = ref<FormInstance>()

const transferFormState = reactive({
  targetDepartmentId: undefined as number | undefined,
  newPositionId: undefined as number | undefined
})

const transferFormRules = {
  targetDepartmentId: [{ required: true, message: '请选择目标部门', trigger: 'change' }]
}

// 单人调动弹窗
const singleTransferModalVisible = ref(false)
const singleTransferModalLoading = ref(false)
const singleTransferFormRef = ref<FormInstance>()
const selectedUser = ref<UserInfo | null>(null)

const singleTransferFormState = reactive({
  targetDepartmentId: undefined as number | undefined,
  newPositionId: undefined as number | undefined
})

const singleTransferFormRules = {
  targetDepartmentId: [{ required: true, message: '请选择目标部门', trigger: 'change' }]
}

// 部门树数据
const departmentTreeData = ref<DepartmentInfo[]>([])

// 数据加载
const fetchData = async () => {
  loading.value = true
  try {
    const res = await userApi.getPage({
      tenantId: 1,
      deptId: currentDepartment.value?.id,
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

// 加载可用用户列表
const fetchAvailableUsers = async () => {
  try {
    const res = await userApi.getList({
      tenantId: 1,
      status: 0,
      pageSize: 1000
    })
    if (res.data) {
      // 过滤掉已在当前部门的用户
      const currentDeptUserIds = tableData.value.map(u => u.id)
      availableUsers.value = res.data.filter(u => !currentDeptUserIds.includes(u.id))
    }
  } catch (error) {
    console.error('加载可用用户失败:', error)
  }
}

// 加载岗位列表
const fetchPositionList = async () => {
  try {
    const res = await positionApi.getList({ tenantId: 1, status: 0 })
    if (res.data) {
      positionList.value = res.data
    }
  } catch (error) {
    console.error('加载岗位列表失败:', error)
  }
}

// 加载部门树
const fetchDepartmentTree = async () => {
  try {
    const res = await departmentApi.getTree({ tenantId: 1, status: 0 })
    if (res.data) {
      departmentTreeData.value = res.data
    }
  } catch (error) {
    console.error('加载部门树失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  Object.assign(searchForm, { username: '', phone: '' })
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

// 获取选中用户
const selectedUsers = computed(() => {
  return tableData.value.filter(u => selectedRowKeys.value.includes(u.id))
})

// 添加人员
const handleAdd = () => {
  addFormState.userIds = []
  addFormState.positionId = undefined
  fetchAvailableUsers()
  addModalVisible.value = true
}

const handleAddModalOk = async () => {
  try {
    await addFormRef.value?.validate()
    addModalLoading.value = true
    
    // TODO: 调用添加部门人员API
    await new Promise(resolve => setTimeout(resolve, 500))
    
    message.success('添加成功')
    addModalVisible.value = false
    fetchData()
  } catch (error) {
    message.error('操作失败')
  } finally {
    addModalLoading.value = false
  }
}

const handleAddModalCancel = () => {
  addModalVisible.value = false
  addFormRef.value?.resetFields()
}

// 批量调动
const handleTransfer = () => {
  if (selectedRowKeys.value.length === 0) {
    message.warning('请先选择要调动的人员')
    return
  }
  transferFormState.targetDepartmentId = undefined
  transferFormState.newPositionId = undefined
  transferModalVisible.value = true
}

const handleTransferModalOk = async () => {
  try {
    await transferFormRef.value?.validate()
    transferModalLoading.value = true
    
    // TODO: 调用批量调动API
    await new Promise(resolve => setTimeout(resolve, 500))
    
    message.success('调动成功')
    transferModalVisible.value = false
    selectedRowKeys.value = []
    fetchData()
  } catch (error) {
    message.error('操作失败')
  } finally {
    transferModalLoading.value = false
  }
}

const handleTransferModalCancel = () => {
  transferModalVisible.value = false
  transferFormRef.value?.resetFields()
}

// 单人调动
const handleTransferSingle = (user: UserInfo) => {
  selectedUser.value = user
  singleTransferFormState.targetDepartmentId = undefined
  singleTransferFormState.newPositionId = undefined
  singleTransferModalVisible.value = true
}

const handleSingleTransferModalOk = async () => {
  if (!selectedUser.value) return
  
  try {
    await singleTransferFormRef.value?.validate()
    singleTransferModalLoading.value = true
    
    // TODO: 调用单人调动API
    await new Promise(resolve => setTimeout(resolve, 500))
    
    message.success('调动成功')
    singleTransferModalVisible.value = false
    fetchData()
  } catch (error) {
    message.error('操作失败')
  } finally {
    singleTransferModalLoading.value = false
  }
}

const handleSingleTransferModalCancel = () => {
  singleTransferModalVisible.value = false
  singleTransferFormRef.value?.resetFields()
}

// 移除人员
const handleRemove = (user: UserInfo) => {
  Modal.confirm({
    title: '确认移除',
    content: `确定要将用户 "${user.nickname || user.username}" 从当前部门移除吗？`,
    async onOk() {
      // TODO: 调用移除部门人员API
      await new Promise(resolve => setTimeout(resolve, 500))
      message.success('移除成功')
      fetchData()
    }
  })
}

// 返回
const handleBack = () => {
  router.push('/system/department')
}

// 过滤用户
const filterUserOption = (input: string, option: any) => {
  const user = availableUsers.value.find(u => u.id === option.value)
  if (!user) return false
  const name = user.nickname || user.username || ''
  return name.toLowerCase().includes(input.toLowerCase())
}

onMounted(() => {
  // 从路由参数获取部门ID
  const deptId = route.query.deptId as string
  if (deptId) {
    departmentApi.getById(Number(deptId)).then(res => {
      if (res.data) {
        currentDepartment.value = res.data
        fetchData()
      }
    })
  }
  fetchPositionList()
  fetchDepartmentTree()
})
</script>

<style scoped>
.department-personnel {
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

.transfer-modal-content {
  padding: 16px 0;
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