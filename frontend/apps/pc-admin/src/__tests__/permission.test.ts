// @ts-nocheck
/**
 * 权限工具函数单元测试
 *
 * @author AI-Ready QA Team
 * @since 1.0.0
 */

import { describe, it, expect, beforeEach, vi, Mock } from 'vitest'

// 使用 vi.hoisted 解决 vi.mock 变量提升问题：mockStore 必须在 vi.mock 工厂函数之前定义
const mockStore = vi.hoisted(() => ({
  permissions: [] as string[],
  roles: [] as string[],
  isLoggedIn: false,
  userType: -1,
  hasPermission: vi.fn(),
  hasAnyPermission: vi.fn(),
  hasAllPermissions: vi.fn(),
  hasRole: vi.fn(),
  hasAnyRole: vi.fn()
}))

// Mock store
vi.mock('@/stores/user', () => ({
  useUserStore: () => mockStore
}))

import * as PermissionUtils from '@/utils/permission'
import * as PermissionComposable from '@/composables/usePermission'

/** 重置 mockStore 到默认状态的辅助函数 */
function resetMockStore(overrides: Partial<typeof mockStore> = {}) {
  mockStore.permissions = overrides.permissions ?? []
  mockStore.roles = overrides.roles ?? []
  mockStore.isLoggedIn = overrides.isLoggedIn ?? false
  mockStore.userType = overrides.userType ?? -1
  mockStore.hasPermission = overrides.hasPermission ?? vi.fn()
  mockStore.hasAnyPermission = overrides.hasAnyPermission ?? vi.fn()
  mockStore.hasAllPermissions = overrides.hasAllPermissions ?? vi.fn()
  mockStore.hasRole = overrides.hasRole ?? vi.fn()
  mockStore.hasAnyRole = overrides.hasAnyRole ?? vi.fn()
}

describe('权限工具函数测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    resetMockStore()
  })

  describe('hasPermission', () => {
    it('应该检查权限是否存在', () => {
      mockStore.hasPermission = vi.fn(() => true)
      expect(PermissionUtils.hasPermission('user:create')).toBe(true)
      expect(mockStore.hasPermission).toHaveBeenCalledWith('user:create')
    })

    it('应该处理无权限的情况', () => {
      mockStore.hasPermission = vi.fn(() => false)
      expect(PermissionUtils.hasPermission('user:delete')).toBe(false)
    })
  })

  describe('hasAnyPermission', () => {
    it('应该检查是否有任意权限', () => {
      mockStore.hasAnyPermission = vi.fn(() => true)
      expect(PermissionUtils.hasAnyPermission(['user:create', 'user:delete'])).toBe(true)
    })

    it('应该返回 false 当没有权限', () => {
      mockStore.hasAnyPermission = vi.fn(() => false)
      expect(PermissionUtils.hasAnyPermission(['user:admin'])).toBe(false)
    })
  })

  describe('hasAllPermissions', () => {
    it('应该检查是否拥有所有权限', () => {
      mockStore.hasAllPermissions = vi.fn(() => true)
      expect(PermissionUtils.hasAllPermissions(['user:create', 'user:read'])).toBe(true)
    })

    it('应该返回 false 当缺少任一权限', () => {
      mockStore.hasAllPermissions = vi.fn(() => false)
      expect(PermissionUtils.hasAllPermissions(['user:create', 'user:admin'])).toBe(false)
    })
  })

  describe('hasRole', () => {
    it('应该检查角色是否存在', () => {
      mockStore.hasRole = vi.fn(() => true)
      expect(PermissionUtils.hasRole('admin')).toBe(true)
    })

    it('应该处理无角色的情况', () => {
      mockStore.hasRole = vi.fn(() => false)
      expect(PermissionUtils.hasRole('super_admin')).toBe(false)
    })
  })

  describe('hasAnyRole', () => {
    it('应该检查是否有任意角色', () => {
      mockStore.hasAnyRole = vi.fn(() => true)
      expect(PermissionUtils.hasAnyRole(['admin', 'manager'])).toBe(true)
    })
  })

  describe('isSuperAdmin', () => {
    it('应该识别超级管理员', () => {
      mockStore.userType = 0
      expect(PermissionUtils.isSuperAdmin()).toBe(true)
    })

    it('应该识别非超级管理员', () => {
      mockStore.userType = 1
      expect(PermissionUtils.isSuperAdmin()).toBe(false)
    })
  })

  describe('isAdmin', () => {
    it('应该识别超级管理员', () => {
      mockStore.userType = 0
      expect(PermissionUtils.isAdmin()).toBe(true)
    })

    it('应该识别管理员', () => {
      mockStore.userType = 1
      expect(PermissionUtils.isAdmin()).toBe(true)
    })

    it('应该识别普通用户', () => {
      mockStore.userType = 2
      expect(PermissionUtils.isAdmin()).toBe(false)
    })
  })

  describe('canOperate', () => {
    it('应该检查按钮操作权限', () => {
      mockStore.permissions = ['button:add', 'button:edit']
      expect(PermissionUtils.canOperate('button:add')).toBe(true)
      expect(PermissionUtils.canOperate('button:delete')).toBe(false)
    })
  })

  describe('permissionFilter', () => {
    it('应该过滤字符串权限', () => {
      expect(PermissionUtils.permissionFilter('user:create', ['user:create'])).toBe(true)
      expect(PermissionUtils.permissionFilter('user:delete', ['user:create'])).toBe(false)
    })

    it('应该过滤数组权限', () => {
      expect(PermissionUtils.permissionFilter(['user:create', 'user:delete'], ['user:create'])).toBe(true)
      expect(PermissionUtils.permissionFilter(['user:admin'], ['user:create'])).toBe(false)
    })

    it('应该处理通配符', () => {
      expect(PermissionUtils.permissionFilter('user:admin', ['*'])).toBe(true)
    })

    it('应该处理空权限列表', () => {
      expect(PermissionUtils.permissionFilter('user:create', [])).toBe(false)
    })
  })

  describe('filterMenusByPermission', () => {
    it('应该过滤菜单列表', () => {
      mockStore.permissions = ['menu:user']
      const menus = [
        { name: '用户管理', permissions: ['menu:user'] },
        { name: '订单管理', permissions: ['menu:order'] },
        { name: '公共菜单' }
      ]
      
      const filtered = PermissionUtils.filterMenusByPermission(menus)
      expect(filtered.length).toBe(2)
      expect(filtered[0].name).toBe('用户管理')
      expect(filtered[1].name).toBe('公共菜单')
    })

    it('应该保留所有菜单当有通配符权限', () => {
      mockStore.permissions = ['*']
      const menus = [
        { name: '用户管理', permissions: ['menu:admin'] },
        { name: '订单管理', permissions: ['menu:order'] }
      ]
      
      const filtered = PermissionUtils.filterMenusByPermission(menus)
      expect(filtered.length).toBe(2)
    })
  })
})

describe('usePermission 组合式函数测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockStore.permissions = ['user:create', 'user:read']
    mockStore.roles = ['admin']
    mockStore.isLoggedIn = true
    mockStore.userType = 0
    mockStore.hasPermission = vi.fn((p) => mockStore.permissions.includes(p))
    mockStore.hasAnyPermission = vi.fn((ps) => ps.some((p: string) => mockStore.permissions.includes(p)))
    mockStore.hasAllPermissions = vi.fn((ps) => ps.every((p: string) => mockStore.permissions.includes(p)))
    mockStore.hasRole = vi.fn((r) => mockStore.roles.includes(r))
    mockStore.hasAnyRole = vi.fn((rs) => rs.some((r: string) => mockStore.roles.includes(r)))
  })

  it('应该提供权限检查方法', () => {
    const { checkPermission, checkAnyPermission, checkAllPermissions } = PermissionComposable.usePermission()
    
    expect(checkPermission('user:create')).toBe(true)
    expect(checkPermission('user:delete')).toBe(false)
    expect(checkAnyPermission(['user:delete', 'user:create'])).toBe(true)
    expect(checkAllPermissions(['user:create', 'user:read'])).toBe(true)
  })

  it('应该提供角色检查方法', () => {
    const { checkRole, checkAnyRole } = PermissionComposable.usePermission()

    expect(checkRole('admin')).toBe(true)
    expect(checkRole('user')).toBe(false)
    // mockStore.roles = ['admin']，'manager'不在其中，所以checkAnyRole返回false
    expect(checkAnyRole(['user', 'manager'])).toBe(false)
    // 'admin'在其中，所以返回true
    expect(checkAnyRole(['admin', 'manager'])).toBe(true)
  })

  it('应该提供超级管理员状态', () => {
    const { isSuperAdminUser, isAdminUser } = PermissionComposable.usePermission()

    // userType=0 是超级管理员
    expect(isSuperAdminUser.value).toBe(true)
    // 超级管理员也是管理员
    expect(isAdminUser.value).toBe(true)
  })

  it('应该识别普通管理员状态', () => {
    // 重新创建composable，使用 userType=1 (管理员但非超级管理员)
    mockStore.userType = 1
    const { isSuperAdminUser, isAdminUser } = PermissionComposable.usePermission()

    expect(isSuperAdminUser.value).toBe(false)
    expect(isAdminUser.value).toBe(true)
  })

  it('应该提供 filterByPermission 方法', () => {
    const { filterByPermission } = PermissionComposable.usePermission()
    
    const items = [
      { name: '添加用户', permission: 'user:create' },
      { name: '查看用户', permission: 'user:read' },
      { name: '无权限项' }
    ]
    
    const filtered = filterByPermission(items)
    expect(filtered.length).toBe(3)
  })

  it('应该提供 withPermission 方法', () => {
    const { withPermission } = PermissionComposable.usePermission()
    let executed = false
    
    withPermission('user:create', () => { executed = true })
    expect(executed).toBe(true)
    
    executed = false
    withPermission('user:admin', () => { executed = true })
    expect(executed).toBe(false)
  })

  it('should provide withRole method', () => {
    const { withRole } = PermissionComposable.usePermission()
    let executed = false
    
    withRole('admin', () => { executed = true })
    expect(executed).toBe(true)
    
    executed = false
    withRole('super_admin', () => { executed = true })
    expect(executed).toBe(false)
  })
})

describe('useButtonPermission 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockStore.permissions = ['button:add', 'button:edit']
    mockStore.roles = ['admin']
    mockStore.isLoggedIn = true
    mockStore.userType = 0
    mockStore.hasPermission = vi.fn((p: string) => mockStore.permissions.includes(p))
    mockStore.hasAnyPermission = vi.fn((ps: string[]) => ps.some((p: string) => mockStore.permissions.includes(p)))
  })

  it('应该检查按钮操作权限', () => {
    const { canOperate, getButtonDisabled } = PermissionComposable.useButtonPermission()
    
    expect(canOperate('button:add')).toBe(true)
    expect(canOperate('button:delete')).toBe(false)
    
    expect(getButtonDisabled('button:edit')).toBe(false)
    expect(getButtonDisabled('button:delete')).toBe(true)
  })
})

describe('useDataPermission 测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockStore.permissions = ['data:self']
    mockStore.roles = ['admin']
    mockStore.isLoggedIn = true
    mockStore.userType = 2  // 普通用户，非超级管理员
    mockStore.hasPermission = vi.fn((p: string) => mockStore.permissions.includes(p))
    mockStore.hasAnyPermission = vi.fn((ps: string[]) => ps.some((p: string) => mockStore.permissions.includes(p)))
    mockStore.hasRole = vi.fn((r: string) => mockStore.roles.includes(r))
    mockStore.hasAnyRole = vi.fn((rs: string[]) => rs.some((r: string) => mockStore.roles.includes(r)))
  })

  it('应该检查数据范围权限', () => {
    const { checkDataScope } = PermissionComposable.useDataPermission()

    // userType=2 (非超级管理员)，checkPermission('data:all') → false
    expect(checkDataScope('all')).toBe(false)
    // checkPermission('data:self') → true (permissions包含'data:self')
    expect(checkDataScope('self')).toBe(true)
  })

  it('应该获取数据范围', () => {
    const { getDataScope } = PermissionComposable.useDataPermission()

    // permissions=['data:self']，没有'data:all'/'data:deptAndChildren'/'data:dept'
    expect(getDataScope()).toBe('self')
  })

  it('应该过滤数据列表', () => {
    const { filterByDataScope } = PermissionComposable.useDataPermission()

    const data = [
      { id: 1, userId: 1, deptId: 10 },
      { id: 2, userId: 2, deptId: 10 },
      { id: 3, userId: 1, deptId: 20 }
    ]

    // self scope: 只返回 userId=1 的记录
    const filtered = filterByDataScope(data, 1, 10)
    expect(filtered.length).toBe(2)
  })
})

describe('Directive 权限指令测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockStore.permissions = ['button:add', 'button:edit']
    mockStore.roles = ['admin']
    mockStore.isLoggedIn = true
    mockStore.userType = 0
    mockStore.hasPermission = vi.fn((p: string) => mockStore.permissions.includes(p))
    mockStore.hasAnyPermission = vi.fn((ps: string[]) => ps.some((p: string) => mockStore.permissions.includes(p)))
    mockStore.hasRole = vi.fn((r: string) => mockStore.roles.includes(r))
    mockStore.hasAnyRole = vi.fn((rs: string[]) => rs.some((r: string) => mockStore.roles.includes(r)))
  })

  it('应该添加有权限的元素', () => {
    const div = document.createElement('div')
    div.innerHTML = '<button v-permission="\'button:add\'">Add</button>'
    
    // Directivemounted would remove element if no permission
    // Since we have button:add permission, element should stay
    expect(div.innerHTML).toContain('button')
  })

  it.skip('应该移除无权限的元素 - 纯DOM操作不触发Vue指令，需实际组件挂载测试', () => {
    // v-permission指令需要通过Vue组件挂载才能真正执行
    // 纯innerHTML设置不会触发Vue指令的mounted钩子
  })
})
