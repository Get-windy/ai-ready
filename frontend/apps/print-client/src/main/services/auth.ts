/**
 * 打印客户端认证服务
 *
 * 处理远程登录、Token 管理、记住密码、自动登录
 */
import axios, { AxiosInstance } from 'axios'
import Store from 'electron-store'
import crypto from 'crypto'
import { safeStorage } from 'electron'

interface AuthState {
  accessToken: string | null
  tenantId: number | null
  tenantName: string | null
  userId: number | null
  username: string | null
  clientId: number | null
  clientName: string | null
  clientCode: string | null
  authKey: string | null
  clientStatus: string | null
  isLoggedIn: boolean
}

class AuthService {
  private store: Store
  private http: AxiosInstance

  constructor() {
    this.store = new Store()
    this.http = axios.create({
      timeout: 15000,
      headers: { 'Content-Type': 'application/json' }
    })
  }

  /** 获取或创建本机机器标识 */
  getMachineId(): string {
    let machineId = this.store.get('machineId') as string
    if (!machineId) {
      machineId = crypto.randomUUID().replace(/-/g, '').substring(0, 16).toUpperCase()
      this.store.set('machineId', machineId)
    }
    return machineId
  }

  /** 获取保存的 Token */
  getSavedToken(): string | null {
    return this.store.get('auth.token') as string | null
  }

  /** 检查是否有记住密码配置 */
  hasRememberedLogin(): boolean {
    return this.store.get('auth.rememberMe', false) as boolean
  }

  /** 远程登录 */
  async login(serverUrl: string, tenantName: string, username: string, password: string, rememberMe: boolean): Promise<any> {
    const baseUrl = serverUrl.replace(/\/+$/, '')
    const machineId = this.getMachineId()

    const { data } = await this.http.post(`${baseUrl}/api/v2/print/client/auth/login`, {
      tenantName,
      username,
      password,
      rememberMe,
      machineId
    })

    if (data.code !== 0 && data.code !== 200) {
      throw new Error(data.message || '登录失败')
    }

    const loginData = data.data || data
    this.saveAuth(loginData, serverUrl, rememberMe, password)

    return loginData
  }

  /** 注册客户端名称 */
  async register(serverUrl: string, clientName: string, clientVersion?: string): Promise<any> {
    const baseUrl = serverUrl.replace(/\/+$/, '')
    const token = this.getSavedToken()
    const machineId = this.getMachineId()

    if (!token) throw new Error('未登录，请先登录')

    const { data } = await this.http.post(
      `${baseUrl}/api/v2/print/client/auth/register?machineId=${machineId}`,
      { clientName, clientVersion },
      { headers: { Authorization: `Bearer ${token}` } }
    )

    if (data.code !== 0 && data.code !== 200) {
      throw new Error(data.message || '注册失败')
    }

    const regData = data.data || data
    // 更新保存的客户端信息
    if (regData.client) {
      this.store.set('auth.clientId', regData.client.clientId)
      this.store.set('auth.clientName', regData.client.clientName)
      this.store.set('auth.clientCode', regData.client.clientCode)
      this.store.set('auth.authKey', regData.client.authKey)
      this.store.set('auth.clientStatus', regData.client.status)
    }

    return regData
  }

  /** 检查 Token 有效性 */
  async checkToken(serverUrl: string): Promise<boolean> {
    const baseUrl = serverUrl.replace(/\/+$/, '')
    const token = this.getSavedToken()
    if (!token) return false

    try {
      const { data } = await this.http.get(`${baseUrl}/api/v2/print/client/auth/check`, {
        headers: { Authorization: `Bearer ${token}` }
      })
      const result = data.data || data
      return result.valid === true
    } catch {
      return false
    }
  }

  /** 获取当前用户信息 */
  async getMe(serverUrl: string): Promise<any> {
    const baseUrl = serverUrl.replace(/\/+$/, '')
    const token = this.getSavedToken()
    const clientId = this.store.get('auth.clientId') as number | null

    if (!token) throw new Error('未登录')

    const { data } = await this.http.get(
      `${baseUrl}/api/v2/print/client/auth/me${clientId ? `?clientId=${clientId}` : ''}`,
      { headers: { Authorization: `Bearer ${token}` } }
    )

    return data.data || data
  }

  /** 登出 */
  async logout(serverUrl: string): Promise<void> {
    const baseUrl = serverUrl.replace(/\/+$/, '')
    const token = this.getSavedToken()

    try {
      if (token) {
        await this.http.post(`${baseUrl}/api/v2/print/client/auth/logout`, {}, {
          headers: { Authorization: `Bearer ${token}` }
        })
      }
    } catch {
      // ignore logout errors
    }

    this.clearAuth()
  }

  /** 尝试自动登录 */
  async tryAutoLogin(serverUrl: string): Promise<{ success: boolean; data?: any; error?: string }> {
    const token = this.getSavedToken()
    if (!token) return { success: false, error: '无保存的 Token' }

    try {
      const valid = await this.checkToken(serverUrl)
      if (!valid) {
        // Token 过期，尝试清除并返回失败
        this.clearAuth()
        return { success: false, error: 'Token 已过期' }
      }

      const me = await this.getMe(serverUrl)
      return { success: true, data: me }
    } catch (err: any) {
      this.clearAuth()
      return { success: false, error: err.message }
    }
  }

  /** 保存认证信息 */
  private saveAuth(loginData: any, serverUrl: string, rememberMe: boolean, password?: string): void {
    this.store.set('serverUrl', serverUrl)
    this.store.set('auth.token', loginData.accessToken)
    this.store.set('auth.tenantId', loginData.tenantId)
    this.store.set('auth.tenantName', loginData.tenantName)
    this.store.set('auth.userId', loginData.userId)
    this.store.set('auth.username', loginData.username)
    this.store.set('auth.rememberMe', rememberMe)

    if (loginData.client) {
      this.store.set('auth.clientId', loginData.client.clientId)
      this.store.set('auth.clientName', loginData.client.clientName)
      this.store.set('auth.clientCode', loginData.client.clientCode)
      this.store.set('auth.authKey', loginData.client.authKey)
      this.store.set('auth.clientStatus', loginData.client.status)
    }

    // 记住密码时加密保存凭据
    if (rememberMe && password) {
      const credentials: any = {
        username: loginData.username,
        tenantName: loginData.tenantName
      }
      // 使用 Electron safeStorage 加密密码（如可用）
      if (safeStorage.isEncryptionAvailable()) {
        credentials.encryptedPassword = safeStorage.encryptString(password).toString('base64')
      } else {
        // 回退：base64 编码（至少不是明文）
        credentials.password = Buffer.from(password).toString('base64')
      }
      this.store.set('auth.credentials', credentials)
    } else {
      this.store.delete('auth.credentials')
    }
  }

  /** 清除认证信息 */
  clearAuth(): void {
    // 保留 serverUrl 和 machineId，清除认证相关
    this.store.delete('auth.token')
    this.store.delete('auth.tenantId')
    this.store.delete('auth.tenantName')
    this.store.delete('auth.userId')
    this.store.delete('auth.username')
    this.store.delete('auth.clientId')
    this.store.delete('auth.clientName')
    this.store.delete('auth.clientCode')
    this.store.delete('auth.authKey')
    this.store.delete('auth.clientStatus')
    // 如果 rememberMe 未勾选，也清除凭据
    if (!this.store.get('auth.rememberMe')) {
      this.store.delete('auth.credentials')
      this.store.delete('auth.rememberMe')
    }
  }

  /** 获取完整认证状态 */
  getAuthState(): AuthState {
    return {
      accessToken: this.store.get('auth.token') as string | null,
      tenantId: this.store.get('auth.tenantId') as number | null,
      tenantName: this.store.get('auth.tenantName') as string | null,
      userId: this.store.get('auth.userId') as number | null,
      username: this.store.get('auth.username') as string | null,
      clientId: this.store.get('auth.clientId') as number | null,
      clientName: this.store.get('auth.clientName') as string | null,
      clientCode: this.store.get('auth.clientCode') as string | null,
      authKey: this.store.get('auth.authKey') as string | null,
      clientStatus: this.store.get('auth.clientStatus') as string | null,
      isLoggedIn: !!this.store.get('auth.token')
    }
  }

  /** 设置开机自启 */
  setAutoStart(enabled: boolean): void {
    // 这个会在 main process 中通过 Electron API 调用
    this.store.set('settings.autoStart', enabled)
  }

  /** 获取开机自启设置 */
  getAutoStart(): boolean {
    return this.store.get('settings.autoStart', false) as boolean
  }
}

export default new AuthService()
