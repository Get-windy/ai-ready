<template>
  <div class="user-role-assignment">
    <div class="search-section">
      <el-input
        v-model="searchText"
        placeholder="搜索用户（姓名、邮箱、部门）"
        clearable
        @input="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="refreshUsers">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div class="assignment-container">
      <!-- 用户列表 -->
      <div class="user-list-section">
        <div class="section-header">
          <h3>用户列表</h3>
          <span class="user-count">共 {{ filteredUsers.length }} 个用户</span>
        </div>
        
        <div v-loading="loading" class="user-list">
          <el-table
            :data="filteredUsers"
            stripe
            highlight-current-row
            @row-click="handleUserSelect"
          >
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="email" label="邮箱" width="180" />
            <el-table-column prop="department" label="部门" width="120" />
            <el-table-column prop="position" label="职位" width="120" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'warning'">
                  {{ row.status === 'active' ? '活跃' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  size="small"
                  @click.stop="showRoleSelector(row)"
                >
                  分配角色
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 角色分配面板 -->
      <div class="role-assignment-section">
        <div class="section-header">
          <h3>角色分配</h3>
          <span v-if="selectedUser" class="selected-user">
            {{ selectedUser.name }} 的已分配角色
          </span>
        </div>

        <div v-if="selectedUser" class="role-assignment-content">
          <!-- 角色选择器 -->
          <div class="role-selector">
            <el-select
              v-model="selectedRoleId"
              placeholder="选择要分配的角色"
              clearable
              class="role-select"
            >
              <el-option
                v-for="role in availableRoles"
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
              分配角色
            </el-button>
          </div>

          <!-- 已分配角色列表 -->
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
        </div>

        <div v-else class="no-selection">
          <el-icon><User /></el-icon>
          <p>请在左侧选择用户以查看角色分配</p>
        </div>
      </div>
    </div>

    <!-- 角色选择器对话框 -->
    <el-dialog
      v-model="roleSelectorVisible"
      title="分配角色"
      width="500px"
    >
      <div class="role-selector-dialog">
        <el-select
          v-model="dialogSelectedRoleId"
          placeholder="选择角色"
          style="width: 100%"
        >
          <el-option
            v-for="role in roles"
            :key="role.id"
            :label="role.name"
            :value="role.id"
          />
        </el-select>
        <div class="dialog-actions">
          <el-button @click="roleSelectorVisible = false">取消</el-button>
          <el-button
            type="primary"
            :disabled="!dialogSelectedRoleId"
            @click="confirmRoleAssignment"
          >
            确认分配
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, UserFilled, User } from '@element-plus/icons-vue'
import type { UserInfo, Role, UserRole } from '../../types'

const props = defineProps<{
  users: UserInfo[]
  roles: Role[]
  loading: boolean
}>()

const emit = defineEmits<{
  'assign-role': [userId: string, roleId: string]
  'remove-role': [userId: string, roleId: string]
  'search': [keyword: string]
}>()

const searchText = ref('')
const selectedUser = ref<UserInfo | null>(null)
const selectedRoleId = ref<string>('')
const roleSelectorVisible = ref(false)
const dialogSelectedRoleId = ref<string>('')
const dialogSelectedUser = ref<UserInfo | null>(null)

// 计算属性
const filteredUsers = computed(() => {
  if (!searchText.value.trim()) {
    return props.users
  }
  const keyword = searchText.value.toLowerCase()
  return props.users.filter(user => 
    user.name.toLowerCase().includes(keyword) ||
    user.email?.toLowerCase().includes(keyword) ||
    user.department?.toLowerCase().includes(keyword) ||
    user.position?.toLowerCase().includes(keyword)
  )
})

const userRoles = computed(() => {
  if (!selectedUser.value) return []
  // 这里应该从store中获取用户角色关系，暂时返回模拟数据
  return []
})

const availableRoles = computed(() => {
  if (!selectedUser.value) return props.roles
  
  // 过滤掉用户已经拥有的角色
  const assignedRoleIds = new Set(userRoles.value.map(ur => ur.roleId))
  return props.roles.filter(role => !assignedRoleIds.has(role.id))
})

// 方法
function handleSearch() {
  emit('search', searchText.value)
}

function refreshUsers() {
  searchText.value = ''
  emit('search', '')
}

function handleUserSelect(user: UserInfo) {
  selectedUser.value = user
  selectedRoleId.value = ''
}

function showRoleSelector(user: UserInfo) {
  dialogSelectedUser.value = user
  dialogSelectedRoleId.value = ''
  roleSelectorVisible.value = true
}

function assignRole() {
  if (!selectedUser.value || !selectedRoleId.value) return
  
  emit('assign-role', selectedUser.value.id, selectedRoleId.value)
  selectedRoleId.value = ''
}

function removeRole(roleId: string) {
  if (!selectedUser.value) return
  
  emit('remove-role', selectedUser.value.id, roleId)
}

function confirmRoleAssignment() {
  if (!dialogSelectedUser.value || !dialogSelectedRoleId.value) return
  
  emit('assign-role', dialogSelectedUser.value.id, dialogSelectedRoleId.value)
  roleSelectorVisible.value = false
  dialogSelectedRoleId.value = ''
}

function getRoleName(roleId: string): string {
  const role = props.roles.find(r => r.id === roleId)
  return role?.name || '未知角色'
}

// 监听用户变化
watch(selectedUser, (newUser) => {
  if (newUser) {
    // 这里可以加载用户的角色信息
  }
})
</script>

<style scoped>
.user-role-assignment {
  padding: 20px;
}

.search-section {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.search-section .el-input {
  flex: 1;
}

.assignment-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  height: 600px;
}

.user-list-section,
.role-assignment-section {
  background: var(--el-bg-color);
  border-radius: 8px;
  border: 1px solid var(--el-border-color);
  padding: 16px;
  overflow: hidden;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color);
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.user-count {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.selected-user {
  color: var(--el-color-primary);
  font-weight: 500;
}

.user-list {
  height: calc(100% - 60px);
  overflow-y: auto;
}

.role-assignment-content {
  height: calc(100% - 60px);
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.role-selector {
  display: flex;
  gap: 12px;
}

.role-selector .role-select {
  flex: 1;
}

.assigned-roles h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
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

.role-selector-dialog {
  padding: 20px 0;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}
</style>