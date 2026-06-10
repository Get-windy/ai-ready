/**
 * 打印客户端 HTTP API 客户端
 *
 * 用于客户端与服务端之间的 REST API 通信：
 * - 心跳上报
 * - 任务轮询
 * - 状态上报
 * - 截图确认
 */
import axios, { AxiosInstance } from 'axios'
import Store from 'electron-store'

interface ApiClientConfig {
  baseUrl: string
  clientId: number
  authKey: string
}

interface ClientTaskItem {
  taskId: number
  taskCode: string
  renderedHtml?: string
  templateJson?: any
  dataJson?: any
  printerName?: string
  copies?: number
  paperSize?: string
  paperMargins?: any
}

interface PollResponse {
  hasMore: boolean
  tasks: ClientTaskItem[]
}

class ApiClient {
  private http: AxiosInstance
  private store: Store
  private config: ApiClientConfig | null = null

  constructor() {
    this.store = new Store()
    this.http = axios.create({
      timeout: 30000,
      headers: { 'Content-Type': 'application/json' }
    })
    this.loadConfig()
  }

  private loadConfig(): void {
    const baseUrl = this.store.get('serverUrl', 'http://localhost:5655') as string
    const clientId = this.store.get('clientId', 0) as number
    const authKey = this.store.get('authKey', '') as string

    if (clientId && authKey) {
      this.config = { baseUrl, clientId, authKey }
    }
  }

  public setConfig(baseUrl: string, clientId: number, authKey: string): void {
    this.config = { baseUrl, clientId, authKey }
    this.store.set('serverUrl', baseUrl)
    this.store.set('clientId', clientId)
    this.store.set('authKey', authKey)
  }

  /**
   * 心跳上报
   */
  public async heartbeat(clientVersion?: string, defaultPrinter?: string): Promise<boolean> {
    if (!this.config) return false
    try {
      await this.http.post(`${this.config.baseUrl}/api/v2/print/client/heartbeat`, {
        clientId: this.config.clientId,
        authKey: this.config.authKey,
        clientVersion,
        defaultPrinter
      })
      return true
    } catch (error: any) {
      console.error('[ApiClient] Heartbeat failed:', error.message)
      return false
    }
  }

  /**
   * 轮询拉取待处理任务
   */
  public async pollTasks(limit: number = 10): Promise<PollResponse> {
    if (!this.config) return { hasMore: false, tasks: [] }
    try {
      const { data } = await this.http.post(
        `${this.config.baseUrl}/api/v2/print/client/tasks/poll`,
        null,
        {
          params: {
            clientId: this.config.clientId,
            authKey: this.config.authKey,
            limit
          }
        }
      )
      return data?.data || { hasMore: false, tasks: [] }
    } catch (error: any) {
      console.error('[ApiClient] Poll tasks failed:', error.message)
      return { hasMore: false, tasks: [] }
    }
  }

  /**
   * 上报任务状态
   */
  public async reportTaskStatus(
    taskId: number,
    status: string,
    errorMessage?: string,
    resultLog?: string
  ): Promise<boolean> {
    if (!this.config) return false
    try {
      await this.http.put(
        `${this.config.baseUrl}/api/v2/print/client/tasks/status`,
        { taskId, status, errorMessage, resultLog },
        {
          params: {
            clientId: this.config.clientId,
            authKey: this.config.authKey
          }
        }
      )
      return true
    } catch (error: any) {
      console.error('[ApiClient] Report status failed:', error.message)
      return false
    }
  }

  /**
   * 截图确认回调
   */
  public async confirmScreenshot(
    screenshotId: number,
    imageBase64: string
  ): Promise<boolean> {
    if (!this.config) return false
    try {
      await this.http.post(
        `${this.config.baseUrl}/api/v2/print/client/screenshots/${screenshotId}/confirm`,
        { imageBase64 },
        {
          params: {
            clientId: this.config.clientId,
            authKey: this.config.authKey
          }
        }
      )
      return true
    } catch (error: any) {
      console.error('[ApiClient] Confirm screenshot failed:', error.message)
      return false
    }
  }

  /**
   * 获取客户端配置信息
   */
  public async getClientInfo(): Promise<any> {
    if (!this.config) return null
    try {
      const { data } = await this.http.get(
        `${this.config.baseUrl}/api/v2/print/clients/${this.config.clientId}`,
        { headers: { tenantId: this.store.get('tenantId', 1) } }
      )
      return data?.data || null
    } catch {
      return null
    }
  }
}

export default new ApiClient()
