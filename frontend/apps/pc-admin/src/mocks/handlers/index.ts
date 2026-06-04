/**
 * Mock API 处理程序
 * 每个处理程序是一个异步函数，接收请求上下文并返回响应数据。
 * 模拟真实后端接口行为，包括随机延迟和 ApiResponse 封套。
 */
import { mockUsers, filterUsers, type MockUser } from '../data/users'
import {
  trendChartData, todos, stockAlerts,
  dashboardStats, type StockAlertItem, type TodoItem
} from '../data/dashboard'

// ── 类型定义 ────────────────────────────────────────────

export interface HandlerContext {
  /** 路径参数，如 /api/user/:id → { id: '1' } */
  params: Record<string, string>
  /** 请求体（已解析的 JSON） */
  body: any
  /** URL 查询参数 */
  query: Record<string, string>
}

export interface MockHandlerDefinition {
  /** HTTP 方法 */
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH'
  /** 路径模式，使用 :param 语法捕获路径参数 */
  path: string
  /** 处理函数，返回将放入 ApiResponse.data 的数据 */
  handler: (ctx: HandlerContext) => Promise<any>
}

// ── 辅助 ────────────────────────────────────────────────

/** 可修改的用户列表副本 */
let userStore: MockUser[] = structuredClone(mockUsers)
let nextUserId = userStore.length + 1

/** 重置用户数据（用于测试） */
export function resetUserStore(): void {
  userStore = structuredClone(mockUsers)
  nextUserId = userStore.length + 1
}

function delay(min = 200, max = 800): Promise<void> {
  const ms = min + Math.random() * (max - min)
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function paginate<T>(list: T[], current: number, size: number) {
  const start = (current - 1) * size
  const end = start + size
  return {
    records: list.slice(start, end),
    total: list.length,
    current,
    size,
    pages: Math.ceil(list.length / size)
  }
}

/** 从 URL 查询参数中安全解析整数 */
function queryInt(q: Record<string, string>, key: string): number | undefined {
  const v = q[key]
  if (v === undefined || v === '' || v === null) return undefined
  const n = Number(v)
  return Number.isNaN(n) ? undefined : n
}

// ── 处理程序列表 ────────────────────────────────────────

export const handlers: MockHandlerDefinition[] = [
  // ====================================================================
  // 用户管理
  // ====================================================================

  // GET /api/user/page — 分页查询用户
  {
    method: 'GET',
    path: '/api/user/page',
    handler: async (ctx) => {
      await delay()
      const { query } = ctx
      const filtered = filterUsers(userStore, {
        username: query.username,
        nickname: query.nickname,
        phone: query.phone,
        status: queryInt(query, 'status'),
        deptId: queryInt(query, 'deptId')
      })
      const current = queryInt(query, 'pageNum') || 1
      const size = queryInt(query, 'pageSize') || 10
      return paginate(filtered, current, size)
    }
  },

  // GET /api/user/list — 获取用户列表
  {
    method: 'GET',
    path: '/api/user/list',
    handler: async (ctx) => {
      await delay()
      const { query } = ctx
      return filterUsers(userStore, {
        username: query.username,
        nickname: query.nickname,
        phone: query.phone,
        status: queryInt(query, 'status'),
        deptId: queryInt(query, 'deptId')
      })
    }
  },

  // GET /api/user/:id — 获取单用户
  {
    method: 'GET',
    path: '/api/user/:id',
    handler: async (ctx) => {
      await delay()
      const id = Number(ctx.params.id)
      const user = userStore.find((u) => u.id === id)
      if (!user) throw mockError(404, '用户不存在')
      return { ...user }
    }
  },

  // POST /api/user — 创建用户
  {
    method: 'POST',
    path: '/api/user',
    handler: async (ctx) => {
      await delay()
      const { body } = ctx
      const newUser: MockUser = {
        id: nextUserId++,
        tenantId: body.tenantId || 1,
        username: body.username || '',
        nickname: body.nickname || '',
        password: body.password || '123456',
        email: body.email || '',
        phone: body.phone || '',
        gender: body.gender ?? 0,
        userType: body.userType ?? 2,
        status: body.status ?? 0,
        avatar: `https://api.dicebear.com/7.x/initials/svg?seed=${body.username || 'user'}&backgroundColor=bae637`,
        deptId: body.deptId || 1,
        deptName: body.deptName || '未分配',
        roles: [],
        roleIds: [],
        permissions: [],
        createTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
        updateTime: new Date().toISOString().replace('T', ' ').slice(0, 19)
      }
      userStore.unshift(newUser)
      return true
    }
  },

  // PUT /api/user/:id — 更新用户
  {
    method: 'PUT',
    path: '/api/user/:id',
    handler: async (ctx) => {
      await delay()
      const id = Number(ctx.params.id)
      const idx = userStore.findIndex((u) => u.id === id)
      if (idx === -1) throw mockError(404, '用户不存在')
      const { body } = ctx
      userStore[idx] = {
        ...userStore[idx],
        ...body,
        id, // 不允许修改 id
        updateTime: new Date().toISOString().replace('T', ' ').slice(0, 19)
      }
      return true
    }
  },

  // DELETE /api/user/:id — 删除用户
  {
    method: 'DELETE',
    path: '/api/user/:id',
    handler: async (ctx) => {
      await delay()
      const id = Number(ctx.params.id)
      const idx = userStore.findIndex((u) => u.id === id)
      if (idx === -1) throw mockError(404, '用户不存在')
      userStore.splice(idx, 1)
      return true
    }
  },

  // DELETE /api/user/batch — 批量删除用户
  {
    method: 'DELETE',
    path: '/api/user/batch',
    handler: async (ctx) => {
      await delay()
      // 批量删除的请求体为 id 数组
      const ids: number[] = Array.isArray(ctx.body) ? ctx.body : (ctx.body?.ids || [])
      if (ids.length === 0) throw mockError(400, '请选择要删除的用户')
      userStore = userStore.filter((u) => !ids.includes(u.id))
      return true
    }
  },

  // PATCH /api/user/:id/password/reset — 重置密码
  {
    method: 'PATCH',
    path: '/api/user/:id/password/reset',
    handler: async (ctx) => {
      await delay()
      const id = Number(ctx.params.id)
      const idx = userStore.findIndex((u) => u.id === id)
      if (idx === -1) throw mockError(404, '用户不存在')
      const newPassword = ctx.query.newPassword || '123456'
      userStore[idx].password = newPassword
      userStore[idx].updateTime = new Date().toISOString().replace('T', ' ').slice(0, 19)
      return true
    }
  },

  // PATCH /api/user/:id/status — 切换用户状态
  {
    method: 'PATCH',
    path: '/api/user/:id/status',
    handler: async (ctx) => {
      await delay()
      const id = Number(ctx.params.id)
      const idx = userStore.findIndex((u) => u.id === id)
      if (idx === -1) throw mockError(404, '用户不存在')
      const newStatus = queryInt(ctx.query, 'status')
      if (newStatus === undefined) throw mockError(400, 'status 参数缺失')
      userStore[idx].status = newStatus
      userStore[idx].updateTime = new Date().toISOString().replace('T', ' ').slice(0, 19)
      return true
    }
  },

  // POST /api/user/:id/roles — 分配角色
  {
    method: 'POST',
    path: '/api/user/:id/roles',
    handler: async (ctx) => {
      await delay()
      const id = Number(ctx.params.id)
      const idx = userStore.findIndex((u) => u.id === id)
      if (idx === -1) throw mockError(404, '用户不存在')
      const roleIds: number[] = Array.isArray(ctx.body) ? ctx.body : []
      userStore[idx].roleIds = roleIds
      // 简单映射：更新 roles 名称
      const roleMap: Record<number, string> = { 1: 'admin', 2: 'manager', 3: 'operator', 4: 'viewer', 5: 'finance', 6: 'sales' }
      userStore[idx].roles = roleIds.map((rid) => roleMap[rid] || `role_${rid}`)
      userStore[idx].updateTime = new Date().toISOString().replace('T', ' ').slice(0, 19)
      return true
    }
  },

  // PATCH /api/user/:id/password/change — 修改密码
  {
    method: 'PATCH',
    path: '/api/user/:id/password/change',
    handler: async (ctx) => {
      await delay()
      const id = Number(ctx.params.id)
      const idx = userStore.findIndex((u) => u.id === id)
      if (idx === -1) throw mockError(404, '用户不存在')
      const { oldPassword, newPassword } = ctx.query
      if (oldPassword && userStore[idx].password !== oldPassword) {
        throw mockError(400, '原密码错误')
      }
      userStore[idx].password = newPassword || '123456'
      userStore[idx].updateTime = new Date().toISOString().replace('T', ' ').slice(0, 19)
      return true
    }
  },

  // ====================================================================
  // 认证
  // ====================================================================

  // POST /api/auth/login — 登录
  {
    method: 'POST',
    path: '/api/auth/login',
    handler: async (ctx) => {
      await delay()
      const { username, password } = ctx.body || {}
      // 查找匹配用户
      const user = userStore.find(
        (u) => u.username === username && u.password === password
      )
      if (!user) {
        throw mockError(401, '用户名或密码错误')
      }
      if (user.status === 1) {
        throw mockError(403, '账号已被停用，请联系管理员')
      }
      const token =
        'mock-token-' + user.username + '-' + Date.now().toString(36)
      return {
        token,
        tokenName: 'Authorization',
        userId: user.id
      }
    }
  },

  // POST /api/auth/logout — 登出
  {
    method: 'POST',
    path: '/api/auth/logout',
    handler: async () => {
      await delay()
      return null
    }
  },

  // GET /api/auth/userinfo — 获取当前用户信息
  {
    method: 'GET',
    path: '/api/auth/userinfo',
    handler: async () => {
      await delay()
      // 返回默认管理员信息
      const admin = userStore.find((u) => u.username === 'admin')
      if (!admin) {
        return userStore[0]
      }
      return {
        userId: admin.id,
        username: admin.username,
        nickname: admin.nickname,
        avatar: admin.avatar,
        roles: admin.roles,
        permissions: admin.permissions
      }
    }
  },

  // GET /api/auth/captcha — 验证码 SVG
  {
    method: 'GET',
    path: '/api/auth/captcha',
    handler: async () => {
      await delay(150)
      // 返回一个简单的占位 SVG 验证码图片
      const svg =
        `<svg xmlns="http://www.w3.org/2000/svg" width="120" height="40" viewBox="0 0 120 40">
  <rect width="120" height="40" fill="#f0f2f5" rx="4"/>
  <text x="20" y="28" font-family="Arial,sans-serif" font-size="20" fill="#333" font-weight="bold">A3B7X</text>
  <line x1="10" y1="10" x2="50" y2="35" stroke="#d9d9d9" stroke-width="1"/>
  <line x1="60" y1="5" x2="110" y2="30" stroke="#d9d9d9" stroke-width="1"/>
  <circle cx="80" cy="20" r="2" fill="#bbb"/>
  <circle cx="35" cy="15" r="1.5" fill="#bbb"/>
</svg>`
      // 返回 base64 编码的 SVG（匹配常见的验证码接口格式）
      return {
        img: 'data:image/svg+xml;base64,' + btoa(svg),
        uuid: 'mock-captcha-' + Date.now()
      }
    }
  },

  // ====================================================================
  // 仪表盘
  // ====================================================================

  // GET /api/dashboard/stats — KPI 统计数据
  {
    method: 'GET',
    path: '/api/dashboard/stats',
    handler: async () => {
      await delay()
      return { ...dashboardStats }
    }
  },

  // GET /api/dashboard/trend — 趋势图数据
  {
    method: 'GET',
    path: '/api/dashboard/trend',
    handler: async () => {
      await delay()
      return { ...trendChartData }
    }
  },

  // GET /api/dashboard/todos — 待办事项列表
  {
    method: 'GET',
    path: '/api/dashboard/todos',
    handler: async () => {
      await delay()
      return [...todos]
    }
  },

  // GET /api/dashboard/alerts — 库存预警列表
  {
    method: 'GET',
    path: '/api/dashboard/alerts',
    handler: async () => {
      await delay()
      return [...stockAlerts]
    }
  },

  // ====================================================================
  // 菜单
  // ====================================================================

  // GET /api/menu/user/client/:clientType — 获取用户客户端菜单
  {
    method: 'GET',
    path: '/api/menu/user/client/:clientType',
    handler: async (ctx) => {
      await delay()
      const { clientType } = ctx.params
      return [
        {
          id: 1,
          parentId: 0,
          menuName: '工作台',
          menuCode: 'Dashboard',
          menuType: 1,
          path: '/dashboard',
          component: 'views/dashboard/index.vue',
          routeName: 'Dashboard',
          icon: 'DashboardOutlined',
          sort: 1,
          isExternal: 0,
          isCache: 1,
          visible: 1,
          status: 1,
          clientType,
          children: []
        },
        {
          id: 2,
          parentId: 0,
          menuName: '采购管理',
          menuCode: 'Purchase',
          menuType: 1,
          path: '/purchase',
          component: 'views/purchase/index.vue',
          routeName: 'Purchase',
          icon: 'ShoppingCartOutlined',
          sort: 2,
          isExternal: 0,
          isCache: 1,
          visible: 1,
          status: 1,
          clientType,
          children: []
        },
        {
          id: 3,
          parentId: 0,
          menuName: '销售管理',
          menuCode: 'Sale',
          menuType: 1,
          path: '/sale',
          component: 'views/sale/index.vue',
          routeName: 'Sale',
          icon: 'ShoppingOutlined',
          sort: 3,
          isExternal: 0,
          isCache: 1,
          visible: 1,
          status: 1,
          clientType,
          children: []
        },
        {
          id: 5,
          parentId: 0,
          menuName: '客户管理',
          menuCode: 'CrmCustomer',
          menuType: 1,
          path: '/crm/customer',
          component: 'views/crm/customer/index.vue',
          routeName: 'CrmCustomer',
          icon: 'TeamOutlined',
          sort: 5,
          isExternal: 0,
          isCache: 1,
          visible: 1,
          status: 1,
          clientType,
          children: []
        },
        {
          id: 6,
          parentId: 0,
          menuName: '系统管理',
          menuCode: 'System',
          menuType: 0,
          path: '/system',
          component: '',
          routeName: 'System',
          icon: 'SettingOutlined',
          sort: 99,
          isExternal: 0,
          isCache: 0,
          visible: 1,
          status: 1,
          clientType,
          children: [
            {
              id: 61,
              parentId: 6,
              menuName: '用户管理',
              menuCode: 'SystemUser',
              menuType: 1,
              path: '/system/user',
              component: 'views/system/user/index.vue',
              routeName: 'SystemUser',
              icon: 'UserOutlined',
              sort: 1,
              isExternal: 0,
              isCache: 1,
              visible: 1,
              status: 1,
              clientType,
              children: []
            },
            {
              id: 62,
              parentId: 6,
              menuName: '角色管理',
              menuCode: 'SystemRole',
              menuType: 1,
              path: '/system/role',
              component: 'views/system/role/index.vue',
              routeName: 'SystemRole',
              icon: 'TeamOutlined',
              sort: 2,
              isExternal: 0,
              isCache: 1,
              visible: 1,
              status: 1,
              clientType,
              children: []
            },
            {
              id: 63,
              parentId: 6,
              menuName: '菜单管理',
              menuCode: 'SystemMenu',
              menuType: 1,
              path: '/system/menu',
              component: 'views/system/menu/index.vue',
              routeName: 'SystemMenu',
              icon: 'MenuOutlined',
              sort: 3,
              isExternal: 0,
              isCache: 1,
              visible: 1,
              status: 1,
              clientType,
              children: []
            }
          ]
        }
      ]
    }
  },

  // POST /api/menu/tree — 菜单树
  {
    method: 'POST',
    path: '/api/menu/tree',
    handler: async () => {
      await delay()
      return [
        { id: 1, parentId: 0, menuName: '工作台', menuCode: 'Dashboard', menuType: 1, path: '/dashboard', icon: 'DashboardOutlined', sort: 1, status: 1, visible: 1, children: [] },
        { id: 2, parentId: 0, menuName: '采购管理', menuCode: 'Purchase', menuType: 1, path: '/purchase', icon: 'ShoppingCartOutlined', sort: 2, status: 1, visible: 1, children: [] },
        { id: 3, parentId: 0, menuName: '销售管理', menuCode: 'Sale', menuType: 1, path: '/sale', icon: 'ShoppingOutlined', sort: 3, status: 1, visible: 1, children: [] },
        { id: 5, parentId: 0, menuName: '客户管理', menuCode: 'CrmCustomer', menuType: 1, path: '/crm/customer', icon: 'TeamOutlined', sort: 5, status: 1, visible: 1, children: [] },
        { id: 6, parentId: 0, menuName: '系统管理', menuCode: 'System', menuType: 0, path: '/system', icon: 'SettingOutlined', sort: 99, status: 1, visible: 1,
          children: [
            { id: 61, parentId: 6, menuName: '用户管理', menuCode: 'SystemUser', menuType: 1, path: '/system/user', icon: 'UserOutlined', sort: 1, status: 1, visible: 1, children: [] },
            { id: 62, parentId: 6, menuName: '角色管理', menuCode: 'SystemRole', menuType: 1, path: '/system/role', icon: 'TeamOutlined', sort: 2, status: 1, visible: 1, children: [] },
            { id: 63, parentId: 6, menuName: '菜单管理', menuCode: 'SystemMenu', menuType: 1, path: '/system/menu', icon: 'MenuOutlined', sort: 3, status: 1, visible: 1, children: [] }
          ]
        }
      ]
    }
  },

  // GET /api/menu/user/:userId — 获取用户菜单
  {
    method: 'GET',
    path: '/api/menu/user/:userId',
    handler: async (ctx) => {
      await delay()
      const { userId } = ctx.params
      return [
        { id: 1, parentId: 0, menuName: '工作台', menuCode: 'Dashboard', menuType: 1, path: '/dashboard', icon: 'DashboardOutlined', sort: 1, status: 1, visible: 1, children: [], userId: Number(userId) }
      ]
    }
  },

  // GET /api/menu/all — 获取所有菜单
  {
    method: 'GET',
    path: '/api/menu/all',
    handler: async () => {
      await delay()
      return [
        { id: 1, parentId: 0, menuName: '工作台', menuCode: 'Dashboard', menuType: 1, path: '/dashboard', icon: 'DashboardOutlined', sort: 1, status: 1, visible: 1, children: [] }
      ]
    }
  }
]

// ── 错误模拟 ────────────────────────────────────────────

export function mockError(code: number, msg: string): Error {
  const err = new Error(msg) as any
  err.mockCode = code
  err.mockMessage = msg
  return err
}
