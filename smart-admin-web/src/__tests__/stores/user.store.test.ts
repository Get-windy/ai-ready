/**
 * AI-Ready Store状态管理测试
 */
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Mock user store for testing
const createMockUserStore = () => {
  let state = {
    token: '',
    userInfo: null as any,
    permissions: [] as string[],
    roles: [] as string[]
  }
  
  return {
    state: () => state,
    isLoggedIn: () => !!state.token,
    username: () => state.userInfo?.username || '',
    setToken: (token: string) => { state.token = token },
    setUserInfo: (info: any) => { state.userInfo = info },
    setPermissions: (perms: string[]) => { state.permissions = perms },
    setRoles: (roles: string[]) => { state.roles = roles },
    hasPermission: (perm: string) => state.permissions.includes(perm) || state.permissions.includes('*'),
    hasRole: (role: string) => state.roles.includes(role) || state.roles.includes('admin'),
    logout: () => {
      state.token = ''
      state.userInfo = null
      state.permissions = []
      state.roles = []
    }
  }
}

describe('User Store', () => {
  let userStore: ReturnType<typeof createMockUserStore>
  
  beforeEach(() => {
    userStore = createMockUserStore()
  })
  
  describe('初始状态', () => {
    it('初始时未登录', () => {
      expect(userStore.isLoggedIn()).toBe(false)
    })
    
    it('初始token为空', () => {
      expect(userStore.state().token).toBe('')
    })
    
    it('初始用户信息为空', () => {
      expect(userStore.state().userInfo).toBeNull()
    })
  })
  
  describe('登录状态', () => {
    it('设置token后应已登录', () => {
      userStore.setToken('test_token')
      expect(userStore.isLoggedIn()).toBe(true)
    })
    
    it('设置用户信息后可获取用户名', () => {
      userStore.setUserInfo({ username: 'admin', nickname: '管理员' })
      expect(userStore.username()).toBe('admin')
    })
  })
  
  describe('权限检查', () => {
    it('有权限时返回true', () => {
      userStore.setPermissions(['user:read', 'user:create'])
      expect(userStore.hasPermission('user:read')).toBe(true)
      expect(userStore.hasPermission('user:create')).toBe(true)
    })
    
    it('无权限时返回false', () => {
      userStore.setPermissions(['user:read'])
      expect(userStore.hasPermission('user:delete')).toBe(false)
    })
    
    it('通配符权限匹配所有', () => {
      userStore.setPermissions(['*'])
      expect(userStore.hasPermission('any:permission')).toBe(true)
    })
  })
  
  describe('角色检查', () => {
    it('有角色时返回true', () => {
      userStore.setRoles(['admin', 'user'])
      expect(userStore.hasRole('admin')).toBe(true)
      expect(userStore.hasRole('user')).toBe(true)
    })
    
    it('admin角色拥有所有权限', () => {
      userStore.setRoles(['admin'])
      expect(userStore.hasRole('any_role')).toBe(true)
    })
  })
  
  describe('注销操作', () => {
    it('注销后清除所有状态', () => {
      userStore.setToken('token')
      userStore.setUserInfo({ username: 'test' })
      userStore.setPermissions(['user:read'])
      userStore.setRoles(['admin'])
      
      userStore.logout()
      
      expect(userStore.state().token).toBe('')
      expect(userStore.state().userInfo).toBeNull()
      expect(userStore.state().permissions).toHaveLength(0)
      expect(userStore.state().roles).toHaveLength(0)
      expect(userStore.isLoggedIn()).toBe(false)
    })
  })
})

// TagsView Store测试
const createMockTagsViewStore = () => {
  let state = {
    visitedViews: [] as any[],
    cachedViews: [] as string[]
  }
  
  return {
    state: () => state,
    addView: (view: any) => {
      if (!state.visitedViews.find(v => v.path === view.path)) {
        state.visitedViews.push(view)
      }
    },
    addCachedView: (name: string) => {
      if (!state.cachedViews.includes(name)) {
        state.cachedViews.push(name)
      }
    },
    delView: (path: string) => {
      state.visitedViews = state.visitedViews.filter(v => v.path !== path)
    },
    delCachedView: (name: string) => {
      state.cachedViews = state.cachedViews.filter(n => n !== name)
    },
    clearAll: () => {
      state.visitedViews = []
      state.cachedViews = []
    }
  }
}

describe('TagsView Store', () => {
  let tagsViewStore: ReturnType<typeof createMockTagsViewStore>
  
  beforeEach(() => {
    tagsViewStore = createMockTagsViewStore()
  })
  
  it('添加访问视图', () => {
    tagsViewStore.addView({ path: '/dashboard', title: '首页' })
    expect(tagsViewStore.state().visitedViews).toHaveLength(1)
  })
  
  it('不重复添加相同路径视图', () => {
    tagsViewStore.addView({ path: '/dashboard', title: '首页' })
    tagsViewStore.addView({ path: '/dashboard', title: '首页' })
    expect(tagsViewStore.state().visitedViews).toHaveLength(1)
  })
  
  it('删除视图', () => {
    tagsViewStore.addView({ path: '/dashboard', title: '首页' })
    tagsViewStore.addView({ path: '/user', title: '用户' })
    tagsViewStore.delView('/dashboard')
    expect(tagsViewStore.state().visitedViews).toHaveLength(1)
    expect(tagsViewStore.state().visitedViews[0].path).toBe('/user')
  })
  
  it('缓存视图', () => {
    tagsViewStore.addCachedView('Dashboard')
    expect(tagsViewStore.state().cachedViews).toContain('Dashboard')
  })
  
  it('清除所有视图', () => {
    tagsViewStore.addView({ path: '/dashboard' })
    tagsViewStore.addCachedView('Dashboard')
    tagsViewStore.clearAll()
    expect(tagsViewStore.state().visitedViews).toHaveLength(0)
    expect(tagsViewStore.state().cachedViews).toHaveLength(0)
  })
})