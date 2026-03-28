import { defineStore } from 'pinia'
import { login, logout, getUserInfo } from '@/api/user'
import type { UserInfo, LoginForm } from '@/api/user'

interface UserState {
  token: string
  userInfo: UserInfo | null
  permissions: string[]
  roles: string[]
}

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: localStorage.getItem('token') || '',
    userInfo: null,
    permissions: [],
    roles: []
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    username: (state) => state.userInfo?.username || '',
    nickname: (state) => state.userInfo?.nickname || ''
  },

  actions: {
    async login(loginForm: LoginForm) {
      try {
        const { data } = await login(loginForm)
        this.token = data
        localStorage.setItem('token', data)
        return true
      } catch (error) {
        return false
      }
    },

    async getUserInfo() {
      try {
        const { data } = await getUserInfo()
        this.userInfo = data
        this.permissions = data.permissions || []
        this.roles = data.roles || []
      } catch (error) {
        this.logout()
      }
    },

    async logout() {
      try {
        await logout()
      } finally {
        this.token = ''
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
    }
  }
})