/**
 * AuthService 单元测试
 *
 * 覆盖：getMachineId / getSavedToken / hasRememberedLogin / login / register /
 *       checkToken / getMe / logout / tryAutoLogin / getAuthState / clearAuth /
 *       setAutoStart / getAutoStart
 */
import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest'
import { mockStore } from '../test-setup'

let AuthService: any
let service: any

beforeAll(async () => {
  const mod = await import('./auth')
  AuthService = mod.default
})

beforeEach(() => {
  mockStore.clear()
  vi.clearAllMocks()
  service = AuthService
})

// ── helpers ──────────────────────────────────────────────────

function mockLoginSuccess(loginData?: any) {
  service.http.post.mockResolvedValue({
    data: {
      code: 0,
      data: loginData || {
        accessToken: 'token-123',
        tenantId: 1,
        tenantName: '测试租户',
        userId: 42,
        username: 'testuser',
        client: { clientId: 1, clientName: 'PC1', clientCode: 'C001', authKey: 'ak-01', status: 'ONLINE' }
      }
    }
  })
}

function mockLoginSuccessWithCode200() {
  service.http.post.mockResolvedValue({
    data: {
      code: 200,
      data: { accessToken: 'token-200', tenantId: 2, tenantName: '租户B', userId: 99, username: 'user200' }
    }
  })
}

function mockLoginFailure(message = '密码错误') {
  service.http.post.mockResolvedValue({
    data: { code: 401, message }
  })
}

function mockCheckToken(valid: boolean) {
  service.http.get.mockResolvedValueOnce({
    data: { data: { valid } }
  })
}

function mockGetMe(userData?: any) {
  service.http.get.mockResolvedValueOnce({
    data: {
      data: userData || { id: 42, username: 'testuser', tenantName: '测试租户' }
    }
  })
}

// ── getMachineId ─────────────────────────────────────────────

describe('getMachineId()', () => {
  it('生成并持久化机器标识', () => {
    const id = service.getMachineId()
    expect(id).toBeTruthy()
    expect(id.length).toBe(16)

    // 再次调用应返回相同值
    expect(service.getMachineId()).toBe(id)
  })

  it('已存在 machineId 时直接返回', () => {
    mockStore.set('machineId', 'CACHEDID00000001')
    expect(service.getMachineId()).toBe('CACHEDID00000001')
  })
})

// ── getSavedToken / hasRememberedLogin ───────────────────────

describe('getSavedToken() / hasRememberedLogin()', () => {
  it('无 token 时返回 undefined', () => {
    expect(service.getSavedToken()).toBeUndefined()
  })

  it('返回已保存的 token', () => {
    mockStore.set('auth.token', 'test-token')
    expect(service.getSavedToken()).toBe('test-token')
  })

  it('rememberMe 默认为 false', () => {
    expect(service.hasRememberedLogin()).toBe(false)
  })

  it('返回 rememberMe 状态', () => {
    mockStore.set('auth.rememberMe', true)
    expect(service.hasRememberedLogin()).toBe(true)
  })
})

// ── login ────────────────────────────────────────────────────

describe('login()', () => {
  it('登录成功返回 loginData', async () => {
    mockLoginSuccess()
    const result = await service.login('http://server:8080', '租户A', 'user', 'pass', false)

    expect(result.accessToken).toBe('token-123')
    expect(result.tenantName).toBe('测试租户')
  })

  it('支持 code === 200 的响应', async () => {
    mockLoginSuccessWithCode200()
    const result = await service.login('http://server:8080', '租户B', 'user2', 'pass2', false)

    expect(result.accessToken).toBe('token-200')
  })

  it('登录失败时抛异常', async () => {
    mockLoginFailure('密码错误')
    await expect(service.login('http://server:8080', '租户A', 'user', 'wrong', false)).rejects.toThrow('密码错误')
  })

  it('rememberMe 时加密保存凭据', async () => {
    mockLoginSuccess()
    await service.login('http://server:8080', '租户A', 'testuser', 'my-password', true)

    const credentials = mockStore.get('auth.credentials')
    expect(credentials).toBeDefined()
    expect(credentials.username).toBe('testuser')
    expect(credentials.encryptedPassword).toBeDefined()
    expect(mockStore.get('auth.rememberMe')).toBe(true)
  })

  it('rememberMe=false 时不保存凭据', async () => {
    mockLoginSuccess()
    await service.login('http://server:8080', '租户A', 'testuser', 'pass', false)

    expect(mockStore.get('auth.credentials')).toBeUndefined()
  })
})

// ── register ─────────────────────────────────────────────────

describe('register()', () => {
  it('成功注册客户端', async () => {
    mockStore.set('auth.token', 'valid-token')
    service.getMachineId() // ensures machineId in store

    service.http.post.mockResolvedValue({
      data: {
        code: 0,
        data: {
          client: { clientId: 10, clientName: 'MyPC', clientCode: 'PC-001', authKey: 'ak-99', status: 'ONLINE' }
        }
      }
    })

    const result = await service.register('http://server:8080', 'MyPC', '1.0.0')
    expect(result.client.clientId).toBe(10)

    // 验证持久化
    expect(mockStore.get('auth.clientId')).toBe(10)
    expect(mockStore.get('auth.clientName')).toBe('MyPC')
    expect(mockStore.get('auth.clientCode')).toBe('PC-001')
  })

  it('无 token 时抛出异常', async () => {
    // mockStore 中无 auth.token
    await expect(service.register('http://server:8080', 'MyPC')).rejects.toThrow('未登录')
  })
})

// ── checkToken ───────────────────────────────────────────────

describe('checkToken()', () => {
  it('Token 有效返回 true', async () => {
    mockStore.set('auth.token', 'valid-token')
    mockCheckToken(true)

    const result = await service.checkToken('http://server:8080')
    expect(result).toBe(true)
  })

  it('Token 无效返回 false', async () => {
    mockStore.set('auth.token', 'invalid-token')
    mockCheckToken(false)

    const result = await service.checkToken('http://server:8080')
    expect(result).toBe(false)
  })

  it('无 token 时返回 false', async () => {
    expect(await service.checkToken('http://server:8080')).toBe(false)
  })

  it('HTTP 异常时返回 false', async () => {
    mockStore.set('auth.token', 'some-token')
    service.http.get.mockRejectedValue(new Error('Network error'))

    expect(await service.checkToken('http://server:8080')).toBe(false)
  })
})

// ── getMe ────────────────────────────────────────────────────

describe('getMe()', () => {
  it('成功获取用户信息', async () => {
    mockStore.set('auth.token', 'valid-token')
    mockGetMe({ id: 42, username: 'admin', tenantName: '根租户' })

    const result = await service.getMe('http://server:8080')
    expect(result.username).toBe('admin')
  })

  it('无 token 时抛出异常', async () => {
    await expect(service.getMe('http://server:8080')).rejects.toThrow('未登录')
  })
})

// ── logout ───────────────────────────────────────────────────

describe('logout()', () => {
  it('登出成功并清除认证信息', async () => {
    mockStore.set('auth.token', 'some-token')
    mockStore.set('auth.tenantName', '租户A')
    service.http.post.mockResolvedValue({ data: { code: 0 } })

    await service.logout('http://server:8080')

    // 认证信息被清除
    expect(mockStore.get('auth.token')).toBeUndefined()
    expect(mockStore.get('auth.tenantName')).toBeUndefined()
  })

  it('无 token 时不调用 API（不抛异常）', async () => {
    // mockStore 无 auth.token
    await expect(service.logout('http://server:8080')).resolves.toBeUndefined()
  })

  it('API 异常时忽略并继续清理', async () => {
    mockStore.set('auth.token', 'some-token')
    service.http.post.mockRejectedValue(new Error('超时'))

    await expect(service.logout('http://server:8080')).resolves.toBeUndefined()
    expect(mockStore.get('auth.token')).toBeUndefined()
  })
})

// ── tryAutoLogin ─────────────────────────────────────────────

describe('tryAutoLogin()', () => {
  it('有有效 Token 时自动登录成功', async () => {
    mockStore.set('auth.token', 'valid-token')
    mockCheckToken(true)
    mockGetMe({ id: 1, username: 'autologin' })

    const result = await service.tryAutoLogin('http://server:8080')
    expect(result.success).toBe(true)
    expect(result.data.username).toBe('autologin')
  })

  it('Token 过期时清除认证并返回错误', async () => {
    mockStore.set('auth.token', 'expired-token')
    mockStore.set('auth.tenantName', '租户X')
    mockCheckToken(false)

    const result = await service.tryAutoLogin('http://server:8080')
    expect(result.success).toBe(false)
    expect(result.error).toBe('Token 已过期')

    // 认证信息被清除
    expect(mockStore.get('auth.tenantName')).toBeUndefined()
  })

  it('无 Token 时直接返回失败', async () => {
    const result = await service.tryAutoLogin('http://server:8080')
    expect(result.success).toBe(false)
    expect(result.error).toBe('无保存的 Token')
  })
})

// ── getAuthState ─────────────────────────────────────────────

describe('getAuthState()', () => {
  it('未登录时返回 isLoggedIn=false', () => {
    const state = service.getAuthState()
    expect(state.isLoggedIn).toBe(false)
    expect(state.accessToken).toBeUndefined()
  })

  it('已登录时返回完整状态', () => {
    mockStore.set('auth.token', 'tk-1')
    mockStore.set('auth.tenantId', 5)
    mockStore.set('auth.tenantName', '租户A')
    mockStore.set('auth.userId', 100)
    mockStore.set('auth.username', 'admin')
    mockStore.set('auth.clientId', 3)
    mockStore.set('auth.clientName', 'PC-Office')
    mockStore.set('auth.clientCode', 'C003')
    mockStore.set('auth.authKey', 'ak-main')
    mockStore.set('auth.clientStatus', 'ONLINE')

    const state = service.getAuthState()
    expect(state.isLoggedIn).toBe(true)
    expect(state.accessToken).toBe('tk-1')
    expect(state.tenantId).toBe(5)
    expect(state.tenantName).toBe('租户A')
    expect(state.userId).toBe(100)
    expect(state.username).toBe('admin')
    expect(state.clientId).toBe(3)
    expect(state.clientName).toBe('PC-Office')
    expect(state.clientCode).toBe('C003')
    expect(state.authKey).toBe('ak-main')
    expect(state.clientStatus).toBe('ONLINE')
  })
})

// ── clearAuth ────────────────────────────────────────────────

describe('clearAuth()', () => {
  it('清除认证信息但保留 rememberMe 凭据', () => {
    mockStore.set('auth.token', 'tk')
    mockStore.set('auth.tenantName', '租户')
    mockStore.set('auth.rememberMe', true)
    mockStore.set('auth.credentials', { username: 'u', encryptedPassword: 'xxx' })
    mockStore.set('serverUrl', 'http://srv')
    mockStore.set('machineId', 'MACHINE001')

    service.clearAuth()

    // 认证相关被清除
    expect(mockStore.get('auth.token')).toBeUndefined()
    expect(mockStore.get('auth.tenantName')).toBeUndefined()
    // 但 rememberMe 凭据保留
    expect(mockStore.get('auth.credentials')).toBeDefined()
    expect(mockStore.get('auth.rememberMe')).toBe(true)
    // serverUrl 和 machineId 保留
    expect(mockStore.get('serverUrl')).toBe('http://srv')
    expect(mockStore.get('machineId')).toBe('MACHINE001')
  })

  it('rememberMe=false 时也清除凭据', () => {
    mockStore.set('auth.token', 'tk')
    mockStore.set('auth.rememberMe', false)
    mockStore.set('auth.credentials', { username: 'u', password: 'xxx' })

    service.clearAuth()

    expect(mockStore.get('auth.credentials')).toBeUndefined()
    expect(mockStore.get('auth.rememberMe')).toBeUndefined()
  })
})

// ── setAutoStart / getAutoStart ──────────────────────────────

describe('setAutoStart() / getAutoStart()', () => {
  it('默认关闭', () => {
    expect(service.getAutoStart()).toBe(false)
  })

  it('设置并读取', () => {
    service.setAutoStart(true)
    expect(service.getAutoStart()).toBe(true)

    service.setAutoStart(false)
    expect(service.getAutoStart()).toBe(false)
  })
})
