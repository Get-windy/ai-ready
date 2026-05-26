/**
 * 用户授权管理Store
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo, UserRole, AuthorizationHistory, PermissionConflict } from '../types'
import { roleManagementApi } from '../api'

export const useUserAuthorizationStore = defineStore('userAuthorization', () => {
  // 状态
  const users = ref<UserInfo[]>([])
  const userRoles = ref<UserRole[]>([])
  const userPermissions = ref<string[]>([])
  const authorizationHistory = ref<AuthorizationHistory[]>([])
  const permissionConflicts = ref<PermissionConflict[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const currentUser = ref<UserInfo | null>(null)

  // Getter
  const activeUsers = computed(() => users.value.filter(user => user.status === 'active'))
  
  const usersWithRoles = computed(() => {
    return users.value.map(user => ({
      ...user,
      roles: userRoles.value
        .filter(ur => ur.userId === user.id)
        .map(ur => ur.roleId)
    }))
  })

  const currentUserRoles = computed(() => {
    if (!currentUser.value) return []
    return userRoles.value.filter(ur => ur.userId === currentUser.value!.id)
  })

  const currentUserPermissions = computed(() => {
    return userPermissions.value
  })

  // Actions
  async function loadUsers() {
    try {
      loading.value = true
      error.value = null
      const result = await roleManagementApi.getUsers()
      users.value = result
    } catch (err) {
      error.value = err instanceof Error ? err.message : '加载用户列表失败'
      console.error('加载用户列表失败:', err)
    } finally {
      loading.value = false
    }
  }

  async function loadUserRoles() {
    try {
      loading.value = true
      error.value = null
      const result = await roleManagementApi.getUserRoles()
      userRoles.value = result
    } catch (err) {
      error.value = err instanceof Error ? err.message : '加载用户角色关系失败'
      console.error('加载用户角色关系失败:', err)
    } finally {
      loading.value = false
    }
  }

  async function assignRoleToUser(userId: string, roleId: string) {
    try {
      loading.value = true
      error.value = null
      await roleManagementApi.assignRoleToUser(userId, roleId)
      
      // 更新本地状态
      const existingIndex = userRoles.value.findIndex(
        ur => ur.userId === userId && ur.roleId === roleId
      )
      
      if (existingIndex === -1) {
        userRoles.value.push({
          id: `${userId}-${roleId}-${Date.now()}`,
          userId,
          roleId,
          assignedBy: 'system',
          assignedAt: new Date().toISOString()
        })
      }
      
      // 记录到历史
      authorizationHistory.value.unshift({
        id: `hist-${Date.now()}`,
        userId,
        action: 'assign',
        roleId,
        operatorId: 'system',
        operatorName: '系统',
        timestamp: new Date().toISOString(),
        remarks: `分配角色 ${roleId}`
      })
    } catch (err) {
      error.value = err instanceof Error ? err.message : '分配角色失败'
      console.error('分配角色失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function removeRoleFromUser(userId: string, roleId: string) {
    try {
      loading.value = true
      error.value = null
      await roleManagementApi.removeRoleFromUser(userId, roleId)
      
      // 更新本地状态
      const index = userRoles.value.findIndex(
        ur => ur.userId === userId && ur.roleId === roleId
      )
      if (index !== -1) {
        userRoles.value.splice(index, 1)
      }
      
      // 记录到历史
      authorizationHistory.value.unshift({
        id: `hist-${Date.now()}`,
        userId,
        action: 'remove',
        roleId,
        operatorId: 'system',
        operatorName: '系统',
        timestamp: new Date().toISOString(),
        remarks: `移除角色 ${roleId}`
      })
    } catch (err) {
      error.value = err instanceof Error ? err.message : '移除角色失败'
      console.error('移除角色失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function batchAssignRoles(userIds: string[], roleId: string) {
    try {
      loading.value = true
      error.value = null
      await roleManagementApi.batchAssignRoles(userIds, roleId)
      
      // 更新本地状态
      const timestamp = new Date().toISOString()
      userIds.forEach(userId => {
        const existingIndex = userRoles.value.findIndex(
          ur => ur.userId === userId && ur.roleId === roleId
        )
        if (existingIndex === -1) {
          userRoles.value.push({
            id: `${userId}-${roleId}-${Date.now()}`,
            userId,
            roleId,
            assignedBy: 'system',
            assignedAt: timestamp
          })
        }
      })
      
      // 批量记录到历史
      userIds.forEach(userId => {
        authorizationHistory.value.unshift({
          id: `hist-${Date.now()}-${userId}`,
          userId,
          action: 'assign',
          roleId,
          operatorId: 'system',
          operatorName: '系统',
          timestamp,
          remarks: `批量分配角色 ${roleId}`
        })
      })
    } catch (err) {
      error.value = err instanceof Error ? err.message : '批量分配角色失败'
      console.error('批量分配角色失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function loadUserPermissions(userId: string) {
    try {
      loading.value = true
      error.value = null
      const result = await roleManagementApi.getUserPermissions(userId)
      userPermissions.value = result
      currentUser.value = users.value.find(u => u.id === userId) || null
    } catch (err) {
      error.value = err instanceof Error ? err.message : '加载用户权限失败'
      console.error('加载用户权限失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function loadAuthorizationHistory(userId: string) {
    try {
      loading.value = true
      error.value = null
      const result = await roleManagementApi.getAuthorizationHistory(userId)
      authorizationHistory.value = result
      currentUser.value = users.value.find(u => u.id === userId) || null
    } catch (err) {
      error.value = err instanceof Error ? err.message : '加载授权历史失败'
      console.error('加载授权历史失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function loadPermissionConflicts() {
    try {
      loading.value = true
      error.value = null
      const result = await roleManagementApi.getPermissionConflicts()
      permissionConflicts.value = result
    } catch (err) {
      error.value = err instanceof Error ? err.message : '加载权限冲突失败'
      console.error('加载权限冲突失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function resolvePermissionConflict(conflictId: string, resolution: string) {
    try {
      loading.value = true
      error.value = null
      await roleManagementApi.resolvePermissionConflict(conflictId, resolution)
      
      // 更新本地状态
      const index = permissionConflicts.value.findIndex(c => c.id === conflictId)
      if (index !== -1) {
        permissionConflicts.value.splice(index, 1)
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '解决权限冲突失败'
      console.error('解决权限冲突失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  function clearCurrentUser() {
    currentUser.value = null
  }

  return {
    // State
    users,
    userRoles,
    userPermissions,
    authorizationHistory,
    permissionConflicts,
    loading,
    error,
    currentUser,
    
    // Getter
    activeUsers,
    usersWithRoles,
    currentUserRoles,
    currentUserPermissions,
    
    // Actions
    loadUsers,
    loadUserRoles,
    assignRoleToUser,
    removeRoleFromUser,
    batchAssignRoles,
    loadUserPermissions,
    loadAuthorizationHistory,
    loadPermissionConflicts,
    resolvePermissionConflict,
    clearError,
    clearCurrentUser
  }
})