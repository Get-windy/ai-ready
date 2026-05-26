<template>
  <div class="user-list-container">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="用户名">
          <el-input
            v-model="searchForm.username"
            placeholder="请输入用户名"
            clearable
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select
            v-model="searchForm.role"
            placeholder="请选择角色"
            clearable
          >
            <el-option label="全部" value="" />
            <el-option label="管理员" value="admin" />
            <el-option label="普通用户" value="user" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 -->
    <el-card class="action-card">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增用户
      </el-button>
      <el-button
        type="danger"
        :disabled="selectedRows.length === 0"
        @click="handleBatchDelete"
      >
        <el-icon><Delete /></el-icon>
        批量删除
      </el-button>
      <el-button @click="handleExport">
        <el-icon><Download /></el-icon>
        导出
      </el-button>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card">
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.role === 'admin'" type="danger">管理员</el-tag>
            <el-tag v-else type="success">普通用户</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'active'" type="success">启用</el-tag>
            <el-tag v-else type="info">禁用</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">
              查看
            </el-button>
            <el-button link type="primary" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ElTable } from 'element-plus'
import { Search, Refresh, Plus, Delete, Download } from '@element-plus/icons-vue'

// 组件名称
defineOptions({
  name: 'UserList',
})

// Props定义
interface User {
  id: number
  username: string
  realName: string
  email: string
  phone: string
  role: 'admin' | 'user'
  status: 'active' | 'inactive'
  createTime: string
}

interface Props {
  users?: User[]
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  users: () => [],
  loading: false,
})

// Events定义
interface Emits {
  (e: 'user-click', user: User): void
  (e: 'user-add'): void
  (e: 'user-edit', user: User): void
  (e: 'user-delete', userId: number): void
  (e: 'user-batch-delete', userIds: number[]): void
}

const emit = defineEmits<Emits>()

// 搜索表单
const searchForm = reactive({
  username: '',
  role: '',
})

// 表格数据
const tableData = ref<User[]>(props.users)
const selectedRows = ref<User[]>([])

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

// 方法
const handleSearch = () => {
  console.log('搜索:', searchForm)
  emit('user-click', tableData.value[0])
}

const handleReset = () => {
  searchForm.username = ''
  searchForm.role = ''
  handleSearch()
}

const handleAdd = () => {
  emit('user-add')
}

const handleEdit = (row: User) => {
  emit('user-edit', row)
}

const handleDelete = (row: User) => {
  ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    emit('user-delete', row.id)
    ElMessage.success('删除成功')
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

const handleBatchDelete = () => {
  const userIds = selectedRows.value.map(row => row.id)
  ElMessageBox.confirm(
    `确定要删除选中的 ${userIds.length} 个用户吗？`,
    '批量删除',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    },
  ).then(() => {
    emit('user-batch-delete', userIds)
    ElMessage.success('批量删除成功')
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

const handleView = (row: User) => {
  console.log('查看用户:', row)
}

const handleExport = () => {
  ElMessage.success('导出成功')
}

const handleSelectionChange = (selection: User[]) => {
  selectedRows.value = selection
}

const handleSizeChange = (size: number) => {
  pagination.pageSize = size
  fetchData()
}

const handleCurrentChange = (current: number) => {
  pagination.current = current
  fetchData()
}

const fetchData = () => {
  // 模拟数据加载
  console.log('加载数据')
}

// 生命周期
onMounted(() => {
  fetchData()
})
</script>

<style scoped lang="scss">
.user-list-container {
  padding: 20px;

  .search-card,
  .action-card,
  .table-card {
    margin-bottom: 20px;
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>