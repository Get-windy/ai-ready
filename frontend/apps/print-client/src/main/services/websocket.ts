import { app, BrowserWindow, ipcMain } from 'electron'
import WebSocket from 'ws'
import Store from 'electron-store'

interface WebSocketConfig {
  serverUrl: string
  reconnectInterval: number
  maxReconnectAttempts: number
}

interface PrintTaskMessage {
  type: 'PRINT_TASK' | 'TASK_STATUS_UPDATE' | 'PRINTER_STATUS' | 'HEALTH_CHECK'
  payload: any
  timestamp: number
}

class WebSocketService {
  private ws: WebSocket | null = null
  private config: WebSocketConfig
  private reconnectAttempts: number = 0
  private isConnected: boolean = false
  private mainWindow: BrowserWindow | null = null
  private store: Store
  private heartbeatInterval: NodeJS.Timeout | null = null
  private reconnectTimeout: NodeJS.Timeout | null = null
  private onTaskCallback: ((task: any) => void) | null = null

  constructor() {
    this.store = new Store()
    this.config = this.loadConfig()
    this.setupIpcHandlers()
  }

  private loadConfig(): WebSocketConfig {
    return {
      serverUrl: this.store.get('websocket.serverUrl', 'ws://localhost:5655/ws/print') as string,
      reconnectInterval: this.store.get('websocket.reconnectInterval', 5000) as number,
      maxReconnectAttempts: this.store.get('websocket.maxReconnectAttempts', 10) as number
    }
  }

  public setMainWindow(window: BrowserWindow): void {
    this.mainWindow = window
  }

  public onTask(callback: (task: any) => void): void {
    this.onTaskCallback = callback
  }

  public connect(): void {
    if (this.ws && this.isConnected) return

    try {
      this.ws = new WebSocket(this.config.serverUrl)

      this.ws.on('open', () => {
        this.isConnected = true
        this.reconnectAttempts = 0
        this.startHeartbeat()
        this.sendToRenderer('websocket-connected', { serverUrl: this.config.serverUrl })

        // 发送客户端身份认证
        this.send({
          type: 'HEALTH_CHECK',
          payload: {
            clientId: this.getClientId(),
            action: 'auth'
          },
          timestamp: Date.now()
        })
      })

      this.ws.on('message', (data: WebSocket.Data) => {
        try {
          const message: PrintTaskMessage = JSON.parse(data.toString())
          this.handleMessage(message)
        } catch (error) {
          console.error('[WebSocket] Failed to parse message:', error)
        }
      })

      this.ws.on('close', () => {
        this.isConnected = false
        this.stopHeartbeat()
        this.sendToRenderer('websocket-disconnected', { reason: 'Connection closed' })
        this.scheduleReconnect()
      })

      this.ws.on('error', (error: Error) => {
        this.isConnected = false
        this.sendToRenderer('websocket-error', { error: error.message })
      })
    } catch (error: any) {
      this.scheduleReconnect()
    }
  }

  public disconnect(): void {
    this.stopHeartbeat()
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout)
      this.reconnectTimeout = null
    }
    if (this.ws) {
      this.ws.close()
      this.ws = null
      this.isConnected = false
    }
  }

  public reconnect(): void {
    this.disconnect()
    this.reconnectAttempts = 0
    this.connect()
  }

  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.config.maxReconnectAttempts) {
      this.sendToRenderer('websocket-reconnect-failed', {
        attempts: this.reconnectAttempts,
        maxAttempts: this.config.maxReconnectAttempts
      })
      return
    }

    this.reconnectAttempts++
    this.sendToRenderer('websocket-reconnecting', {
      attempt: this.reconnectAttempts,
      maxAttempts: this.config.maxReconnectAttempts
    })

    this.reconnectTimeout = setTimeout(() => this.connect(), this.config.reconnectInterval)
  }

  private startHeartbeat(): void {
    this.heartbeatInterval = setInterval(() => {
      if (this.ws && this.isConnected) {
        this.send({
          type: 'HEALTH_CHECK',
          payload: { clientId: this.getClientId() },
          timestamp: Date.now()
        })
      }
    }, 30000)
  }

  private stopHeartbeat(): void {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval)
      this.heartbeatInterval = null
    }
  }

  private handleMessage(message: PrintTaskMessage): void {
    switch (message.type) {
      case 'PRINT_TASK':
        if (this.onTaskCallback) {
          this.onTaskCallback(message.payload)
        }
        this.sendToRenderer('print-task-received', message.payload)
        break
      case 'TASK_STATUS_UPDATE':
        this.sendToRenderer('task-status-updated', message.payload)
        break
      case 'PRINTER_STATUS':
        this.sendToRenderer('printer-status-changed', message.payload)
        break
      case 'HEALTH_CHECK':
        break
      default:
        console.warn('[WebSocket] Unknown message type:', message.type)
    }
  }

  public send(message: PrintTaskMessage): boolean {
    if (!this.ws || !this.isConnected) return false
    try {
      this.ws.send(JSON.stringify(message))
      return true
    } catch (error: any) {
      console.error('[WebSocket] Failed to send message:', error.message)
      return false
    }
  }

  public sendTaskStatus(taskId: string, status: string, progress?: number): void {
    this.send({
      type: 'TASK_STATUS_UPDATE',
      payload: { taskId, status, progress, clientId: this.getClientId() },
      timestamp: Date.now()
    })
  }

  public static sendTaskStatus(taskId: string, status: string, progress?: number): void {
    // 委托给实例方法（保持向后兼容）
    instance.sendTaskStatus(taskId, status, progress)
  }

  private getClientId(): string {
    let clientId = this.store.get('clientId') as string
    if (!clientId) {
      clientId = `print-client-${Date.now()}`
      this.store.set('clientId', clientId)
    }
    return clientId
  }

  private sendToRenderer(channel: string, data: any): void {
    if (this.mainWindow && !this.mainWindow.isDestroyed()) {
      this.mainWindow.webContents.send(channel, data)
    }
  }

  private setupIpcHandlers(): void {
    ipcMain.handle('websocket-connect', async () => {
      this.connect()
      return { success: true, serverUrl: this.config.serverUrl }
    })
    ipcMain.handle('websocket-disconnect', async () => {
      this.disconnect()
      return { success: true }
    })
    ipcMain.handle('websocket-reconnect', async () => {
      this.reconnect()
      return { success: true }
    })
    ipcMain.handle('websocket-status', async () => ({
      isConnected: this.isConnected,
      serverUrl: this.config.serverUrl,
      reconnectAttempts: this.reconnectAttempts
    }))
    ipcMain.handle('websocket-send', async (_event, message: PrintTaskMessage) => ({
      success: this.send(message)
    }))
    ipcMain.handle('websocket-config-update', async (_event, config: Partial<WebSocketConfig>) => {
      if (config.serverUrl) {
        this.store.set('websocket.serverUrl', config.serverUrl)
        this.config.serverUrl = config.serverUrl
      }
      if (config.reconnectInterval) {
        this.store.set('websocket.reconnectInterval', config.reconnectInterval)
        this.config.reconnectInterval = config.reconnectInterval
      }
      if (config.maxReconnectAttempts) {
        this.store.set('websocket.maxReconnectAttempts', config.maxReconnectAttempts)
        this.config.maxReconnectAttempts = config.maxReconnectAttempts
      }
      return { success: true, config: this.config }
    })
  }

  public getStatus() {
    return {
      isConnected: this.isConnected,
      serverUrl: this.config.serverUrl,
      reconnectAttempts: this.reconnectAttempts
    }
  }
}

const instance = new WebSocketService()
export { WebSocketService }
export default instance
