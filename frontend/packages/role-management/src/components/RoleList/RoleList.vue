<template>
  <div class="role-list-container">
    <!-- 搜索和过滤区域 -->
    <div class="filter-section">
      <el-card shadow="never" class="filter-card">
        <el-form :model="filterForm" inline>
          <el-form-item label="角色名称">
            <el-input
              v-model="filterForm.keyword"
              placeholder="请输入角色名称或编码"
              clearable
              @change="handleFilterChange"
            />
          </el-form-item>
          
          <el-form-item label="角色类型">
            <el-select
              v-model="filterForm.type"
              placeholder="请选择角色类型"
              clearable
              @change="handleFilterChange"
            >
              <el-option label="系统角色" value="system" />
              <el-option label="自定义角色" value="custom" />
              <el-option label="部门角色" value="department" />
              <el-option label="项目角色" value="project" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="状态">
            <el-select
              v-model="filterForm.enabled"
              placeholder="请选择状态"
              clearable
              @change="handleFilterChange"
            >
              <el-option label="启用" :value="true" />
              <el-option label="禁用" :value="false" />
            </el-select>
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="handleCreateRole">
              <el-icon><Plus /></el-icon> 新建角色
            </el-button>
            <el-button @click="handleExport">
              <el-icon><Download /></el-icon> 导出
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <!-- 角色列表表格 -->
    <div class="table-section">
      <el-card shadow="never">
        <el-table
          v-loading="loading"
          :data="roleStore.paginatedRoles"
          style="width: 100%"
          row-key="id"
          @sort-change="handleSortChange"
        >
          <el-table-column prop="code" label="角色编码" width="120" sortable />
          <el-table-column prop="name" label="角色名称" width="150" sortable />
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="getRoleTypeTag(row.type)">
                {{ getRoleTypeText(row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="level" label="级别" width="80" sortable>
            <template #default="{ row }">
              <span :class="`level-${row.level}`">L{{ row.level }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="enabled" label="状态" width="80">
            <template #default="{ row }">
              <el-switch
                v-model="row.enabled"
                @change="handleToggleStatus(row.id, row.enabled)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="isDefault" label="默认" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.isDefault" type="success" size="small">是</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="permissionIds.length" label="权限数" width="90" sortable />
          <el-table-column prop="createdAt" label="创建时间" width="160">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                size="small"
                @click="handleView(row.id)"
              >
                查看
              </el-button>
              <el-button
                type="primary"
                link
                size="small"
                @click="handleEdit(row.id)"
              >
                编辑
              </el-button>
              <el-button
                type="primary"
                link
                size="small"
                @click="handleManagePermissions(row.id)"
              >
                权限管理
              </el-button>
              <el-button
                v-if="!row.isDefault && row.type !== 'system'"
                type="danger"
                link
                size="small"
                @click="handleDelete(row.id)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="pagination-container">
          <el-pagination
            v-model:current-page="roleStore.currentPage"
            v-model:page-size="roleStore.pageSize"
            :total="roleStore.totalRoles"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-card>
    </div>

    <!-- 新建/编辑角色对话框 -->
    <role-form-dialog
      v-model="showRoleFormDialog"
      :role-id="editingRoleId"
      @success="handleRoleFormSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoleStore } from '../store/roleStore'
import { Plus, Download } from '@element-plus/icons-vue'
import RoleFormDialog from '../RoleForm/RoleFormDialog.vue'
import type { RoleQueryParams } from '../types'

// Store
const roleStore = useRoleStore()

// 过滤表单
const filterForm = ref({
  keyword: '',
  type: undefined as string | undefined,
  enabled: undefined as boolean | undefined
})

// 对话框状态
const showRoleFormDialog = ref(false)
const editingRoleId = ref<string | null>(null)

// 计算属性
const loading = computed(() => roleStore.loading)

// 生命周期
onMounted(() => {
  fetchRoles()
})

// 方法
async function fetchRoles() {
  const params: RoleQueryParams = {
    page: roleStore.currentPage,
    pageSize: roleStore.pageSize,
    keyword: filterForm.value.keyword || undefined,
    type: filterForm.value.type as any,
    enabled: filterForm.value.enabled
  }
  await roleStore.fetchRoles(params)
}

function getRoleTypeTag(type: string): string {
  const typeMap: Record<string, string> = {
    system: 'danger',
    custom: 'primary',
    department: 'warning',
    project: 'success'
  }
  return typeMap[type] || 'info'
}

function getRoleTypeText(type: string): string {
  const typeMap: Record<string, string> = {
    system: '系统角色',
    custom: '自定义角色',
    department: '部门角色',
    project: '项目角色'
  }
  return typeMap[type] || type
}

function formatDate(dateString: string): string {
  try {
    return new Date(dateString).toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return dateString
  }
}

// 事件处理
function handleFilterChange() {
  roleStore.currentPage = 1
  fetchRoles()
}

function handleSortChange({ prop, order }: { prop: string; order: string }) {
  const orderDirection = order === 'ascending' ? 'asc' : 'desc'
  const params: RoleQueryParams = {
    orderBy: prop,
    orderDirection
  }
  fetchRoles()
}

function handleSizeChange(size: number) {
  roleStore.pageSize = size
  fetchRoles()
}

function handleCurrentChange(page: number) {
  roleStore.currentPage = page
  fetchRoles()
}

function handleCreateRole() {
  editingRoleId.value = null
  showRoleFormDialog.value = true
}

function handleEdit(roleId: string) {
  editingRoleId.value = roleId
  showRoleFormDialog.value = true
}

async function handleView(roleId: string) {
  try {
    const role = await roleStore.fetchRole(roleId)
    // 这里可以打开查看详情的对话框
    console.log('查看角色:', role)
  } catch (error) {
    console.error('查看角色失败:', error)
  }
}

async function handleManagePermissions(roleId: string) {
  try {
    await roleStore.fetchRolePermissions(roleId)
    // 这里可以打开权限管理对话框
    console.log('管理角色权限:', roleId, roleStore.rolePermissions)
  } catch (error) {
    console.error('获取角色权限失败:', error)
  }
}

async function handleToggleStatus(roleId: string, enabled: boolean) {
  try {
    await roleStore.toggleRoleStatus(roleId, enabled)
  } catch (error) {
    console.error('切换角色状态失败:', error)
    // 恢复原来的状态
    const role = roleStore.roles.find(r => r.id === roleId)
    if (role) {
      role.enabled = !enabled
    }
  }
}

async function handleDelete(roleId: string) {
  try {
    await ElMessageBox.confirm('确定要删除这个角色吗？删除后无法恢复。', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await roleStore.deleteRole(roleId)
    ElMessage.success('角色删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除角色失败:', error)
      ElMessage.error('删除角色失败')
    }
  }
}

function handleExport() {
  // 导出功能实现
  console.log('导出角色数据')
}

function handleRoleFormSuccess() {
  fetchRoles()
}
</script>

<style scoped>
.role-list-container {
  padding: 16px;
}

.filter-section {
  margin-bottom: 16px;
}

.filter-card {
  background-color: #fafafa;
}

.table-section {
  margin-bottom: 16px;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.level-1 { color: #f56c6c; font-weight: bold; }
.level-2 { color: #e6a23c; font-weight: bold; }
.level-3 { color: #409eff; font-weight: bold; }
.level-4 { color: #67c23a; font-weight: bold; }
.level-5 { color: #909399; }
</style>