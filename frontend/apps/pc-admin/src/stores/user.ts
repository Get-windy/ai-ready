import { defineStore } from 'pinia'
import { userApi, type UserInfo, type LoginForm, type LoginResponse } from '@/api/user'
import { menuApi, type MenuInfo } from '@/api/menu'
import { tenantModuleApi } from '@/api/tenantModule'
import { getSseClient, destroySseClient } from '@/utils/sseClient'

/** 超级管理员角色编码列表（与后端 system.super-admin.role-codes 配置保持一致） */
const SUPER_ADMIN_ROLES = ['SUPER_ADMIN', 'admin', 'super_admin']

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
  /**
   * 是否已尝试加载过模块授权数据。
   * 用来区分「尚未加载 / 加载失败」与「确实一个模块都没授权」——
   * 前者不该放行（fail-closed 前必须先能区分），后者本就该拒绝。见 hasValidModule 注释。
   */
  moduleCodesLoaded: boolean
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
    validModuleCodes: [],
    // 注意：该字段刻意**不持久化**（见 persist.paths）——每次会话都要重新确认模块授权
    moduleCodesLoaded: false
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
      sse.on('permission-change', (data: any) => {
        console.info('[SSE] 收到权限变更通知，清除菜单缓存并重新加载权限...')
        // 清除菜单本地缓存
        this.clearMenuCache()
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
        // 登录前先清除旧的登录状态，避免残留token干扰
        this.token = ''
        localStorage.removeItem('token')
        localStorage.removeItem('tenantId')
        localStorage.removeItem('tenantName')
        localStorage.removeItem('userTenants')

        const res = await userApi.login(loginForm) as any
        if (res && res.token) {
          this.token = res.token
          this.userId = res.userId || 0
          this.tenantId = res.tenantId || 1
          this.tenantName = res.tenantName || ''
          this.userTenants = res.tenants || []
          // 立即同步存储token（localStorage.setItem是同步操作）
          localStorage.setItem('token', res.token)
          localStorage.setItem('tenantId', String(res.tenantId || 1))
          localStorage.setItem('tenantName', res.tenantName || '')
          localStorage.setItem('userTenants', JSON.stringify(res.tenants || []))
          console.info('[登录] Token已存储:', res.token.substring(0, 8) + '...')
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
        const res = await userApi.getUserInfo() as any
        if (res) {
          this.userInfo = res
          this.userId = res.userId
          this.permissions = res.permissions || []
          this.roles = res.roles || []
          this.billTypes = (res as Record<string, any>).billTypes || []
        }
        // 菜单由 loadDynamicRoutes() 负责加载，避免重复赋值导致 a-menu 重渲染崩溃

        // 加载当前租户的有效模块编码（用于路由守卫模块校验）
        // ⚠️ 必须 await：不等待的话路由守卫会在数据到位前就执行模块校验，
        //    而 hasValidModule 现在是 fail-closed，会误把正常页面判成无权访问。
        await this.loadValidModuleCodes()

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
      } finally {
        // 无论成功失败都标记「已尝试」——hasValidModule 据此区分
        // 「尚未加载」（拒绝）与「已加载但确实没授权」（也拒绝，但语义不同）。
        this.moduleCodesLoaded = true
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
        this.moduleCodesLoaded = false
        this.menus = []
        localStorage.removeItem('token')
        localStorage.removeItem('tenantId')
        localStorage.removeItem('tenantName')
        localStorage.removeItem('userTenants')
        // 清除菜单缓存
        this.clearMenuCache()
        // 断开 SSE 通知连接
        this.disconnectSse()
      }
    },

    /** 清除菜单数据缓存 */
    clearMenuCache() {
      const keysToRemove: string[] = []
      for (let i = 0; i < localStorage.length; i++) {
        const key = localStorage.key(i)
        if (key && (key.startsWith('menu_cache_') || key.startsWith('menu_cache_expiry_'))) {
          keysToRemove.push(key)
        }
      }
      keysToRemove.forEach(key => localStorage.removeItem(key))
    },

    hasPermission(permission: string): boolean {
      return this.permissions.includes(permission) || this.permissions.includes('*')
    },

    hasRole(role: string): boolean {
      return this.roles.includes(role) || this.roles.some(r => SUPER_ADMIN_ROLES.includes(r))
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
    hasValidModule(moduleCode: string): boolean {
      // 系统用户不限制模块
      if (this.isSystemUser) return true

      // 不受模块授权管辖的路径（getRouteModule 返回 undefined）不会走到这里；
      // 走到这里的都是已登记在 MODULE_ROUTE_MAP 里的模块码。
      if (!moduleCode) return true

      // ⚠️ 模块授权数据尚未就绪（首次进入的竞态、或接口失败）→ **拒绝**，不静默放行。
      //    旧实现是 `length === 0 → return true`（fail-open），与后端语义相反：
      //    TenantModuleService.hasModuleAccess 是 `validCodes.isEmpty() → false`（fail-closed）。
      //    两边语义不一致时，前端成了唯一的宽松方——「接口挂了 = 全模块解锁」。
      if (!this.moduleCodesLoaded) return false

      // 精确匹配
      if (this.validModuleCodes.includes(moduleCode)) return true

      // 前缀匹配（如 "sale:order" 匹配 "sale"）
      let prefix = moduleCode
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