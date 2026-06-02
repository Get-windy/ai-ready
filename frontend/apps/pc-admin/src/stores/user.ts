import { defineStore } from 'pinia'
import { userApi, type UserInfo, type LoginForm, type LoginResponse } from '@/api/user'
import { menuApi, type MenuInfo } from '@/api/menu'

interface UserState {
  token: string
  userId: number
  tenantId: number
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
    async login(loginForm: LoginForm) {
      try {
        const res = await userApi.login(loginForm)
        if (res.data && res.data.token) {
          this.token = res.data.token
          this.userId = res.data.userId || 0
          this.tenantId = res.data.tenantId || 1
          localStorage.setItem('token', res.data.token)
          localStorage.setItem('tenantId', String(res.data.tenantId || 1))
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
        const menuRes = await menuApi.getUserClientMenus(this.userId, 'pc-admin', this.tenantId)
        if (menuRes.data) {
          this.menus = menuRes.data
        }
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
        this.userInfo = null
        this.permissions = []
        this.roles = []
        this.menus = []
        localStorage.removeItem('token')
        localStorage.removeItem('tenantId')
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
    paths: ['token', 'userInfo', 'userId', 'tenantId', 'permissions', 'roles', 'menus']
  }
})