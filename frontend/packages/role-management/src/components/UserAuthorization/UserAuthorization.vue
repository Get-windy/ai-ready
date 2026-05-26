<template>
  <div class="user-authorization">
    <div class="authorization-header">
      <h2>
        <el-icon><User /></el-icon>
        用户授权管理
      </h2>
      <div class="header-actions">
        <el-button type="primary" @click="showBatchAssignDialog">
          <el-icon><Plus /></el-icon>
          批量分配
        </el-button>
      </div>
    </div>

    <div class="authorization-content">
      <!-- 搜索和过滤 -->
      <div class="search-section">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户（姓名、邮箱、部门）"
          clearable
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button @click="refreshData">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <!-- 主内容区域 -->
      <div class="content-grid">
        <!-- 用户列表 -->
        <div class="user-list-section">
          <h3>用户列表</h3>
          <div v-loading="loading" class="user-list">
            <el-table
              :data="filteredUsers"
              highlight-current-row
              @row-click="handleUserSelect"
            >
              <el-table-column prop="name" label="姓名" width="120" />
              <el-table-column prop="email" label="邮箱" width="200" />
              <el-table-column prop="department" label="部门" width="120" />
              <el-table-column label="状态" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'active' ? 'success' : 'warning'">
                    {{ row.status === 'active' ? '活跃' : '禁用' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100">
                <template #default="{ row }">
                  <el-button type="text" @click="showUserRoleDialog(row)">
                    管理角色
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <!-- 用户详情 -->
        <div class="user-detail-section">
          <div v-if="selectedUser" class="user-detail">
            <h3>{{ selectedUser.name }} - 角色分配</h3>
            
            <!-- 角色分配 -->
            <div class="role-assignment">
              <h4>分配新角色</h4>
              <div class="role-selector">
                <el-select v-model="selectedRoleId" placeholder="选择角色" clearable>
                  <el-option
                    v-for="role in roles"
                    :key="role.id"
                    :label="role.name"
                    :value="role.id"
                  />
                </el-select>
                <el-button
                  type="primary"
                  :disabled="!selectedRoleId"
                  @click="assignRole"
                >
                  分配
                </el-button>
              </div>
            </div>

            <!-- 已分配角色 -->
            <div class="assigned-roles">
              <h4>已分配的角色</h4>
              <div v-if="userRoles.length > 0" class="role-tags">
                <el-tag
                  v-for="userRole in userRoles"
                  :key="userRole.roleId"
                  closable
                  type="info"
                  @close="removeRole(userRole.roleId)"
                >
                  {{ getRoleName(userRole.roleId) }}
                </el-tag>
              </div>
              <div v-else class="empty-state">
                <el-icon><UserFilled /></el-icon>
                <p>该用户暂无分配的角色</p>
              </div>
            </div>

            <!-- 权限预览 -->
            <div class="permission-preview">
              <h4>权限预览</h4>
              <el-button
                type="text"
                @click="showPermissionPreview = true"
              >
                查看权限详情
              </el-button>
            </div>
          </div>

          <div v-else class="no-selection">
            <el-icon><User /></el-icon>
            <p>请在左侧选择用户</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 用户角色管理对话框 -->
    <el-dialog
      v-model="userRoleDialogVisible"
      title="管理用户角色"
      width="500px"
    >
      <div v-if="dialogUser" class="user-role-dialog">
        <h4>{{ dialogUser.name }} 的角色管理</h4>
        
        <div class="role-management">
          <el-select
            v-model="dialogSelectedRoleId"
            placeholder="选择角色"
            style="width: 100%; margin-bottom: 16px;"
          >
            <el-option
              v-for="role in roles"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
          
          <div class="dialog-actions">
            <el-button @click="userRoleDialogVisible = false">取消</el-button>
            <el-button
              type="primary"
              :disabled="!dialogSelectedRoleId"
              @click="assignRoleToDialogUser"
            >
              分配角色
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 批量分配对话框 -->
    <el-dialog
      v-model="batchAssignDialogVisible"
      title="批量分配角色"
      width="600px"
    >
      <div class="batch-assign-dialog">
        <el-transfer
          v-model="selectedUserIds"
          :data="usersForTransfer"
          :titles="['待选择用户', '已选择用户']"
          :button-texts="['取消选择', '选择']"
        />
        
        <div class="role-selection">
          <h4>选择要分配的角色</h4>
          <el-select v-model="batchRoleId" placeholder="选择角色" style="width: 100%;">
            <el-option
              v-for="role in roles"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </div>
        
        <div class="dialog-actions">
          <el-button @click="batchAssignDialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            :disabled="selectedUserIds.length === 0 || !batchRoleId"
            @click="handleBatchAssign"
          >
            确认分配（{{ selectedUserIds.length }} 个用户）
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Plus, Search, Refresh, UserFilled } from '@element-plus/icons-vue'
import { useUserAuthorizationStore } from '../../store/userAuthorizationStore'
import { useRoleStore } from '../../store/roleStore'
import type { UserInfo, Role } from '../../types'

const userAuthStore = useUserAuthorizationStore()
const roleStore = useRoleStore()

// 状态
const searchKeyword = ref('')
const selectedUser = ref<UserInfo | null>(null)
const selectedRoleId = ref<string>('')
const userRoleDialogVisible = ref(false)
const batchAssignDialogVisible = ref(false)
const showPermissionPreview = ref(false)
const dialogUser = ref<UserInfo | null>(null)
const dialogSelectedRoleId = ref<string>('')
const selectedUserIds = ref<string[]>([])
const batchRoleId = ref<string>('')

// 计算属性
const loading = computed(() => userAuthStore.loading || roleStore.loading)
const users = computed(() => userAuthStore.users)
const roles = computed(() => roleStore.roles)
const userRoles = computed(() => {
  if (!selectedUser.value) return []
  return userAuthStore.userRoles.filter(ur => ur.userId === selectedUser.value!.id)
})

const filteredUsers = computed(() => {
  if (!searchKeyword.value.trim()) {
    return users.value
  }
  const keyword = searchKeyword.value.toLowerCase()
  return users.value.filter(user => 
    user.name.toLowerCase().includes(keyword) ||
    user.email?.toLowerCase().includes(keyword) ||
    user.department?.toLowerCase().includes(keyword)
  )
})

const usersForTransfer = computed(() => {
  return users.value.map(user => ({
    key: user.id,
    label: `${user.name} (${user.department})`
  }))
})

// 生命周期
onMounted(async () => {
  await loadData()
})

// 方法
async function loadData() {
  try {
    await Promise.all([
      userAuthStore.loadUsers(),
      userAuthStore.loadUserRoles(),
      roleStore.fetchRoles()
    ])
  } catch (error) {
    ElMessage.error('加载数据失败')
  }
}

function handleSearch() {
  // 搜索逻辑已经在计算属性中实现
}

async function refreshData() {
  searchKeyword.value = ''
  await loadData()
}

function handleUserSelect(user: UserInfo) {
  selectedUser.value = user
  selectedRoleId.value = ''
}

function showUserRoleDialog(user: UserInfo) {
  dialogUser.value = user
  dialogSelectedRoleId.value = ''
  userRoleDialogVisible.value = true
}

async function assignRole() {
  if (!selectedUser.value || !selectedRoleId.value) return
  
  try {
    await userAuthStore.assignRoleToUser(selectedUser.value.id, selectedRoleId.value)
    ElMessage.success('角色分配成功')
    selectedRoleId.value = ''
  } catch (error) {
    ElMessage.error('角色分配失败')
  }
}

async function assignRoleToDialogUser() {
  if (!dialogUser.value || !dialogSelectedRoleId.value) return
  
  try {
    await userAuthStore.assignRoleToUser(dialogUser.value.id, dialogSelectedRoleId.value)
    ElMessage.success('角色分配成功')
    userRoleDialogVisible.value = false
    
    // 如果选中的用户是当前用户，刷新显示
    if (selectedUser.value?.id === dialogUser.value.id) {
      await userAuthStore.loadUserRoles()
    }
  } catch (error) {
    ElMessage.error('角色分配失败')
  }
}

async function removeRole(roleId: string) {
  if (!selectedUser.value) return
  
  try {
    await ElMessageBox.confirm('确定要移除该角色吗？', '确认移除', {
      type: 'warning'
    })
    
    await userAuthStore.removeRoleFromUser(selectedUser.value.id, roleId)
    ElMessage.success('角色移除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('角色移除失败')
    }
  }
}

function getRoleName(roleId: string): string {
  const role = roles.value.find(r => r.id === roleId)
  return role?.name || '未知角色'
}

async function handleBatchAssign() {
  if (selectedUserIds.value.length === 0 || !batchRoleId.value) return
  
  try {
    await userAuthStore.batchAssignRoles(selectedUserIds.value, batchRoleId.value)
    ElMessage.success(`成功为 ${selectedUserIds.value.length} 个用户分配角色`)
    batchAssignDialogVisible.value = false
    selectedUserIds.value = []
    batchRoleId.value = ''
    
    // 刷新数据
    await userAuthStore.loadUserRoles()
  } catch (error) {
    ElMessage.error('批量分配失败')
  }
}
</script>

<style scoped>
.user-authorization {
  padding: 20px;
}

.authorization-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--el-border-color);
}

.authorization-header h2 {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.authorization-header .el-icon {
  font-size: 24px;
  color: var(--el-color-primary);
}

.search-section {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.search-section .el-input {
  flex: 1;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.user-list-section,
.user-detail-section {
  background: var(--el-bg-color);
  border-radius: 8px;
  border: 1px solid var(--el-border-color);
  padding: 16px;
  overflow: hidden;
}

.user-list-section h3,
.user-detail-section h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
}

.user-list {
  height: 400px;
  overflow-y: auto;
}

.user-detail {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.role-assignment h4,
.assigned-roles h4,
.permission-preview h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
}

.role-selector {
  display: flex;
  gap: 12px;
}

.role-selector .el-select {
  flex: 1;
}

.role-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.role-tags .el-tag {
  cursor: pointer;
  transition: all 0.2s;
}

.role-tags .el-tag:hover {
  opacity: 0.8;
}

.empty-state,
.no-selection {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: var(--el-text-color-secondary);
  text-align: center;
}

.empty-state .el-icon,
.no-selection .el-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state p,
.no-selection p {
  margin: 0;
  font-size: 14px;
}

.user-role-dialog {
  padding: 20px 0;
}

.role-management {
  margin-top: 16px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.batch-assign-dialog {
  padding: 20px 0;
}

.role-selection {
  margin-top: 24px;
}

.role-selection h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
}
</style>