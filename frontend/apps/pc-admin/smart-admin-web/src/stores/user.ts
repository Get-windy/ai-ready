import { defineStore } from 'pinia'
import { userApi, type UserInfo, type LoginForm } from '@/api/user'

interface UserState {
  token: string
  userId: number
  userInfo: UserInfo | null
  permissions: string[]
  roles: string[]
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: localStorage.getItem('token') || '',
    userId: 0,
    userInfo: null,
    permissions: [],
    roles: []
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    username: (state) => state.userInfo?.username || '',
    nickname: (state) => state.userInfo?.nickname || '',
    userType: (state) => state.userInfo?.userType || 2,
    avatar: (state) => state.userInfo?.avatar || ''
  },

  actions: {
    async login(loginForm: LoginForm) {
      try {
        const res = await userApi.login(loginForm)
        if (res.data) {
          this.token = res.data
          localStorage.setItem('token', res.data)
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
          this.userId = res.data.id
          this.permissions = res.data.permissions || []
          this.roles = res.data.roles || []
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
        this.userInfo = null
        this.permissions = []
        this.roles = []
        localStorage.removeItem('token')
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
    paths: ['token', 'userInfo', 'userId', 'permissions', 'roles']
  }
})