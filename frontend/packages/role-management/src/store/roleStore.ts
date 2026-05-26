/**
 * 角色管理Store
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Role, Permission, RoleQueryParams } from '../types'
import { roleManagementApi } from '../api'

export const useRoleStore = defineStore('role', () => {
  // 状态
  const roles = ref<Role[]>([])
  const currentRole = ref<Role | null>(null)
  const permissions = ref<Permission[]>([])
  const rolePermissions = ref<string[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)
  const totalRoles = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(20)

  // Getter
  const enabledRoles = computed(() => roles.value.filter(role => role.enabled))
  const systemRoles = computed(() => roles.value.filter(role => role.type === 'system'))
  const customRoles = computed(() => roles.value.filter(role => role.type === 'custom'))
  const paginatedRoles = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    const end = start + pageSize.value
    return roles.value.slice(start, end)
  })

  // Actions
  async function fetchRoles(params?: RoleQueryParams) {
    try {
      loading.value = true
      error.value = null
      if (params?.page) currentPage.value = params.page
      if (params?.pageSize) pageSize.value = params.pageSize
      
      const result = await roleManagementApi.getRoles(params)
      roles.value = result.data
      totalRoles.value = result.total
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取角色列表失败'
      console.error('获取角色列表失败:', err)
    } finally {
      loading.value = false
    }
  }

  async function fetchRole(id: string) {
    try {
      loading.value = true
      error.value = null
      const role = await roleManagementApi.getRole(id)
      currentRole.value = role
      return role
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取角色详情失败'
      console.error('获取角色详情失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function createRole(roleData: Omit<Role, 'id' | 'createdAt' | 'updatedAt'>) {
    try {
      loading.value = true
      error.value = null
      const newRole = await roleManagementApi.createRole(roleData)
      roles.value.unshift(newRole)
      totalRoles.value += 1
      return newRole
    } catch (err) {
      error.value = err instanceof Error ? err.message : '创建角色失败'
      console.error('创建角色失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function updateRole(id: string, roleData: Partial<Role>) {
    try {
      loading.value = true
      error.value = null
      const updatedRole = await roleManagementApi.updateRole(id, roleData)
      
      // 更新列表中的角色
      const index = roles.value.findIndex(role => role.id === id)
      if (index !== -1) {
        roles.value[index] = { ...roles.value[index], ...updatedRole }
      }
      
      // 更新当前角色
      if (currentRole.value?.id === id) {
        currentRole.value = updatedRole
      }
      
      return updatedRole
    } catch (err) {
      error.value = err instanceof Error ? err.message : '更新角色失败'
      console.error('更新角色失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function deleteRole(id: string) {
    try {
      loading.value = true
      error.value = null
      await roleManagementApi.deleteRole(id)
      
      // 从列表中移除
      const index = roles.value.findIndex(role => role.id === id)
      if (index !== -1) {
        roles.value.splice(index, 1)
        totalRoles.value -= 1
      }
      
      // 清除当前角色
      if (currentRole.value?.id === id) {
        currentRole.value = null
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '删除角色失败'
      console.error('删除角色失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function toggleRoleStatus(id: string, enabled: boolean) {
    try {
      loading.value = true
      error.value = null
      await roleManagementApi.toggleRoleStatus(id, enabled)
      
      // 更新列表中的角色状态
      const role = roles.value.find(role => role.id === id)
      if (role) {
        role.enabled = enabled
      }
      
      // 更新当前角色状态
      if (currentRole.value?.id === id) {
        currentRole.value.enabled = enabled
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '更新角色状态失败'
      console.error('更新角色状态失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchRolePermissions(roleId: string) {
    try {
      loading.value = true
      error.value = null
      const permissionIds = await roleManagementApi.getRolePermissions(roleId)
      rolePermissions.value = permissionIds
      return permissionIds
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取角色权限失败'
      console.error('获取角色权限失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function assignRolePermissions(roleId: string, permissionIds: string[]) {
    try {
      loading.value = true
      error.value = null
      await roleManagementApi.assignRolePermissions(roleId, permissionIds)
      rolePermissions.value = permissionIds
    } catch (err) {
      error.value = err instanceof Error ? err.message : '分配角色权限失败'
      console.error('分配角色权限失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  async function fetchPermissions() {
    try {
      loading.value = true
      error.value = null
      const allPermissions = await roleManagementApi.getPermissions()
      permissions.value = allPermissions
      return allPermissions
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取权限列表失败'
      console.error('获取权限列表失败:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  function clearCurrentRole() {
    currentRole.value = null
  }

  return {
    // State
    roles,
    currentRole,
    permissions,
    rolePermissions,
    loading,
    error,
    totalRoles,
    currentPage,
    pageSize,
    
    // Getters
    enabledRoles,
    systemRoles,
    customRoles,
    paginatedRoles,
    
    // Actions
    fetchRoles,
    fetchRole,
    createRole,
    updateRole,
    deleteRole,
    toggleRoleStatus,
    fetchRolePermissions,
    assignRolePermissions,
    fetchPermissions,
    clearError,
    clearCurrentRole
  }
})