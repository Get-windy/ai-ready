import { defineStore } from 'pinia'
import { userApi, type UserInfo, type LoginForm, type LoginResponse } from '@/api/user'
import { menuApi, type MenuInfo } from '@/api/menu'
import { tenantModuleApi } from '@/api/tenantModule'
import { getSseClient, destroySseClient } from '@/utils/sseClient'

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
  billTypes: string[]
  validModuleCodes: string[]
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
    menus: [],
    billTypes: [],
    validModuleCodes: []
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

    /**
     * 建立 SSE 通知连接，监听服务端推送的缓存失效事件
     */
    connectSse() {
      const token = this.token || localStorage.getItem('token')
      if (!token) return
      const sse = getSseClient()
      sse.on('cache-invalidate', () => {
        console.info('[SSE] 收到缓存失效通知，重新加载权限数据...')
        if (this.userInfo) {
          this.getUserInfo()
        }
      })
      sse.connect(token)
    },

    /** 断开 SSE 连接（登出时调用） */
    disconnectSse() {
      destroySseClient()
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
          // 登录成功后建立 SSE 通知连接
          this.connectSse()
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
          this.billTypes = res.data.billTypes || []
        }
        // 菜单由 loadDynamicRoutes() 负责加载，避免重复赋值导致 a-menu 重渲染崩溃

        // 加载当前租户的有效模块编码（用于路由守卫模块校验）
        this.loadValidModuleCodes()

        // 建立 SSE 通知连接（页面刷新后重新连接）
        this.connectSse()
      } catch (error) {
        console.error('获取用户信息失败:', error)
        this.logout()
      }
    },

    async loadValidModuleCodes() {
      try {
        const res = await tenantModuleApi.getValidModuleCodes(this.tenantId || 1)
        this.validModuleCodes = res.data || []
      } catch {
        this.validModuleCodes = []
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
        this.billTypes = []
        this.validModuleCodes = []
        this.menus = []
        localStorage.removeItem('token')
        localStorage.removeItem('tenantId')
        localStorage.removeItem('tenantName')
        localStorage.removeItem('userTenants')
        // 断开 SSE 通知连接
        this.disconnectSse()
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
    },

    /**
     * 检查路由对应的模块是否对当前租户有效
     * @param routePath 路由路径（如 "sale/order"）
     * @returns true=模块有效或无需校验
     */
    hasValidModule(routePath: string): boolean {
      // 系统用户不限制模块
      if (this.isSystemUser) return true
      // 没有模块限制数据时放行（避免影响未配置模块的租户）
      if (!this.validModuleCodes || this.validModuleCodes.length === 0) return true

      const parts = routePath.replace(/^\/+/, '').split('/').filter(Boolean)
      if (parts.length === 0) return true

      // 取路径首段作为 moduleKey（如 "sale/order" → "sale"）
      const moduleKey = parts[0]

      // 检查精确匹配
      if (this.validModuleCodes.includes(moduleKey)) return true

      // 检查前缀匹配（如 "sale:order" 匹配 "sale"）
      let prefix = moduleKey
      while (prefix.includes(':')) {
        prefix = prefix.substring(0, prefix.lastIndexOf(':'))
        if (this.validModuleCodes.includes(prefix)) return true
      }

      return false
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