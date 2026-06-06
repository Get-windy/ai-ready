import { defineStore } from 'pinia'
import { userApi, type UserInfo, type LoginForm, type LoginResponse } from '@/api/user'
import { menuApi, type MenuInfo } from '@/api/menu'

interface UserState {
  token: string
  userId: number
  tenantId: number
  tenantName: string
  userTenants: { id: number; tenantName: string; tenantCode: string; status: number }[]
  userInfo: UserInfo | null
  permissions: string[]
  roles: string[]
  menus: MenuInfo[]
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: localStorage.getItem('token') || '',
    userId: 0,
    tenantId: Number(localStorage.getItem('tenantId')) || 1,
    tenantName: localStorage.getItem('tenantName') || '',
    userTenants: JSON.parse(localStorage.getItem('userTenants') || '[]'),
    userInfo: null,
    permissions: [],
    roles: [],
    menus: []
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    username: (state) => state.userInfo?.username || '',
    nickname: (state) => state.userInfo?.nickname || '',
    userType: (state) => state.userInfo?.userType || 2,
    avatar: (state) => state.userInfo?.avatar || '',
    isSystemUser: (state) => (state.userInfo?.userType ?? 2) === 0,
    isEnterpriseAdmin: (state) => (state.userInfo?.userType ?? 2) <= 1
  },

  actions: {
    /** 清除本地缓存的陈旧会话数据，强制下次从后端重新获取 */
    clearStaleSessionData() {
      this.userInfo = null
      this.permissions = []
      this.roles = []
      this.menus = []
    },

    async login(loginForm: LoginForm) {
      try {
        const res = await userApi.login(loginForm)
        if (res.data && res.data.token) {
          this.token = res.data.token
          this.userId = res.data.userId || 0
          this.tenantId = res.data.tenantId || 1
          this.tenantName = res.data.tenantName || ''
          this.userTenants = res.data.tenants || []
          localStorage.setItem('token', res.data.token)
          localStorage.setItem('tenantId', String(res.data.tenantId || 1))
          localStorage.setItem('tenantName', res.data.tenantName || '')
          localStorage.setItem('userTenants', JSON.stringify(res.data.tenants || []))
          return true
        }
        return false
      } catch (error) {
        console.error('登录失败:', error)
        return false
      }
    },

    async getUserInfo() {
      try {
        const res = await userApi.getUserInfo()
        if (res.data) {
          this.userInfo = res.data
          this.userId = res.data.userId
          this.permissions = res.data.permissions || []
          this.roles = res.data.roles || []
        }
        // 菜单由 loadDynamicRoutes() 负责加载，避免重复赋值导致 a-menu 重渲染崩溃
      } catch (error) {
        console.error('获取用户信息失败:', error)
        this.logout()
      }
    },

    async logout() {
      try {
        await userApi.logout()
      } finally {
        this.token = ''
        this.userId = 0
        this.tenantId = 1
        this.tenantName = ''
        this.userTenants = []
        this.userInfo = null
        this.permissions = []
        this.roles = []
        this.menus = []
        localStorage.removeItem('token')
        localStorage.removeItem('tenantId')
        localStorage.removeItem('tenantName')
        localStorage.removeItem('userTenants')
      }
    },

    hasPermission(permission: string): boolean {
      return this.permissions.includes(permission) || this.permissions.includes('*')
    },

    hasRole(role: string): boolean {
      return this.roles.includes(role) || this.roles.includes('admin')
    },

    hasAnyPermission(permissions: string[]): boolean {
      return permissions.some(p => this.hasPermission(p))
    },

    hasAllPermissions(permissions: string[]): boolean {
      return permissions.every(p => this.hasPermission(p))
    },

    hasAnyRole(roles: string[]): boolean {
      return roles.some(r => this.hasRole(r))
    }
  },

  persist: {
    key: 'user-store',
    storage: localStorage,
    // 只持久化凭据和租户信息，不持久化用户数据和权限
    // userInfo/permissions/roles/menus 每次页面加载从后端重新获取
    paths: ['token', 'userId', 'tenantId', 'tenantName', 'userTenants']
  }
})