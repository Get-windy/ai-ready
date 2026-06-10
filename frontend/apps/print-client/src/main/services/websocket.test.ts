/**
 * WebSocketService 单元测试
 *
 * 覆盖：connect / disconnect / reconnect / send / sendTaskStatus /
 *       onTask / getStatus / 消息处理 / 重连机制 / 心跳 / IPC 处理器
 */
import { describe, it, expect, vi, beforeEach, beforeAll } from 'vitest'
import { mockStore, mockWebContents, mockIpcMain } from '../test-setup'

let WsClass: any  // WebSocketService class（用于测试静态方法）
let service: any  // 单例实例

beforeAll(async () => {
  const mod = await import('./websocket')
  WsClass = mod.WebSocketService
  service = mod.default

  // 验证构造函数已注册所有 IPC 处理器（beforeEach 清理之前执行）
  expect(mockIpcMain.handle).toHaveBeenCalledTimes(6)
  expect(mockIpcMain.handle).toHaveBeenCalledWith('websocket-connect', expect.any(Function))
  expect(mockIpcMain.handle).toHaveBeenCalledWith('websocket-disconnect', expect.any(Function))
  expect(mockIpcMain.handle).toHaveBeenCalledWith('websocket-reconnect', expect.any(Function))
  expect(mockIpcMain.handle).toHaveBeenCalledWith('websocket-status', expect.any(Function))
  expect(mockIpcMain.handle).toHaveBeenCalledWith('websocket-send', expect.any(Function))
  expect(mockIpcMain.handle).toHaveBeenCalledWith('websocket-config-update', expect.any(Function))
})

beforeEach(async () => {
  mockStore.clear()
  vi.clearAllMocks()
  // 手动重置内部状态（避免前一测试遗留的脏数据）
  service.ws = null
  service.isConnected = false
  service.reconnectAttempts = 0
  if (service.heartbeatInterval) clearInterval(service.heartbeatInterval)
  service.heartbeatInterval = null
  if (service.reconnectTimeout) clearTimeout(service.reconnectTimeout)
  service.reconnectTimeout = null
  service.onTaskCallback = null
  service.mainWindow = null
  // 等待 MockWebSocket 定时器平息
  await new Promise(resolve => setTimeout(resolve, 25))
})

// ── getStatus ────────────────────────────────────────────────

describe('getStatus()', () => {
  it('返回默认配置与断开状态', () => {
    const status = service.getStatus()
    expect(status.isConnected).toBe(false)
    expect(status.serverUrl).toBe('ws://localhost:5655/ws/print')
    expect(status.reconnectAttempts).toBe(0)
  })
})

// ── connect ──────────────────────────────────────────────────

describe('connect()', () => {
  it('创建 WebSocket 并在连接后发送身份认证', async () => {
    const mockWindow = { webContents: { send: vi.fn() }, isDestroyed: vi.fn(() => false) }
    service.setMainWindow(mockWindow)

    service.connect()
    expect(service.ws).not.toBeNull()

    await new Promise(resolve => setTimeout(resolve, 20))
    expect(service.isConnected).toBe(true)

    // 连接后应发送健康检查（客户端认证）
    expect(service.ws.send).toHaveBeenCalled()

    // renderer 应收到连接通知
    expect(mockWindow.webContents.send).toHaveBeenCalledWith('websocket-connected', expect.objectContaining({
      serverUrl: 'ws://localhost:5655/ws/print'
    }))
  })

  it('已连接时忽略重复调用', () => {
    service.isConnected = true
    service.ws = { send: vi.fn(), on: vi.fn() }

    service.connect()
    // 不抛异常即通过
    expect(service.isConnected).toBe(true)
  })
})

// ── disconnect ───────────────────────────────────────────────

describe('disconnect()', () => {
  it('断开连接并清理资源', async () => {
    service.connect()
    await new Promise(resolve => setTimeout(resolve, 20))
    expect(service.isConnected).toBe(true)

    service.disconnect()
    expect(service.isConnected).toBe(false)
    expect(service.ws).toBeNull()
    expect(service.heartbeatInterval).toBeNull()
    // note: close 事件会触发 scheduleReconnect 重新设置 reconnectTimeout
  })
})

// ── reconnect ────────────────────────────────────────────────

describe('reconnect()', () => {
  it('重置重连计数并重新连接', async () => {
    service.reconnectAttempts = 5
    service.connect()
    await new Promise(resolve => setTimeout(resolve, 20))

    service.reconnect()
    expect(service.reconnectAttempts).toBe(0)
    expect(service.isConnected).toBe(false)

    await new Promise(resolve => setTimeout(resolve, 20))
    expect(service.ws).not.toBeNull()
  })
})

// ── send ─────────────────────────────────────────────────────

describe('send()', () => {
  it('已连接时发送消息返回 true', async () => {
    service.connect()
    await new Promise(resolve => setTimeout(resolve, 20))

    const result = service.send({ type: 'HEALTH_CHECK', payload: {}, timestamp: Date.now() })
    expect(result).toBe(true)
    expect(service.ws.send).toHaveBeenCalled()
  })

  it('未连接时返回 false', () => {
    const result = service.send({ type: 'HEALTH_CHECK', payload: {}, timestamp: Date.now() })
    expect(result).toBe(false)
  })

  it('ws.send 异常时返回 false', async () => {
    service.connect()
    await new Promise(resolve => setTimeout(resolve, 20))

    service.ws.send.mockImplementation(() => { throw new Error('Send failed') })
    const result = service.send({ type: 'HEALTH_CHECK', payload: {}, timestamp: Date.now() })
    expect(result).toBe(false)
  })
})

// ── sendTaskStatus ───────────────────────────────────────────

describe('sendTaskStatus()', () => {
  it('实例方法发送任务状态', async () => {
    service.connect()
    await new Promise(resolve => setTimeout(resolve, 20))

    service.sendTaskStatus('task-1', 'PRINTING', 50)
    expect(service.ws.send).toHaveBeenCalledTimes(2) // 1st=HEALTH_CHECK, 2nd=sendTaskStatus
    const sentArg = JSON.parse(service.ws.send.mock.calls[1][0])
    expect(sentArg.type).toBe('TASK_STATUS_UPDATE')
    expect(sentArg.payload.taskId).toBe('task-1')
    expect(sentArg.payload.status).toBe('PRINTING')
    expect(sentArg.payload.progress).toBe(50)
  })

  it('静态方法委托给实例方法', () => {
    const spy = vi.spyOn(service, 'sendTaskStatus')
    WsClass.sendTaskStatus('task-2', 'COMPLETED')
    expect(spy).toHaveBeenCalledWith('task-2', 'COMPLETED', undefined)
    spy.mockRestore()
  })
})

// ── onTask ───────────────────────────────────────────────────

describe('onTask()', () => {
  it('注册回调函数', () => {
    const cb = vi.fn()
    service.onTask(cb)
    expect(service.onTaskCallback).toBe(cb)
  })

  it('收到 PRINT_TASK 消息时调用回调并通知渲染进程', async () => {
    const cb = vi.fn()
    service.onTask(cb)

    const mockWindow = { webContents: { send: vi.fn() }, isDestroyed: vi.fn(() => false) }
    service.setMainWindow(mockWindow)

    service.connect()
    await new Promise(resolve => setTimeout(resolve, 20))

    // 模拟 ws.on('message') 收到消息
    const messageData = Buffer.from(JSON.stringify({
      type: 'PRINT_TASK',
      payload: { taskId: 'new-task', taskCode: 'T001' },
      timestamp: Date.now()
    }))
    const handlers = service.ws._eventListeners.get('message')
    handlers.forEach((fn: Function) => fn(messageData))

    expect(cb).toHaveBeenCalledWith({ taskId: 'new-task', taskCode: 'T001' })
    expect(mockWindow.webContents.send).toHaveBeenCalledWith('print-task-received', { taskId: 'new-task', taskCode: 'T001' })
  })
})

// ── 消息处理 ───────────────────────────────────────────────────

describe('消息类型处理', () => {
  function setupConnected() {
    const mockWindow = { webContents: { send: vi.fn() }, isDestroyed: vi.fn(() => false) }
    service.setMainWindow(mockWindow)
    service.connect()
    return new Promise<void>(resolve => setTimeout(resolve, 20))
  }

  function simulateMessage(raw: Record<string, any>) {
    const data = Buffer.from(JSON.stringify(raw))
    const handlers = service.ws._eventListeners.get('message')
    handlers.forEach((fn: Function) => fn(data))
  }

  it('TASK_STATUS_UPDATE 通知渲染进程', async () => {
    await setupConnected()
    simulateMessage({ type: 'TASK_STATUS_UPDATE', payload: { taskId: 't1', status: 'COMPLETED' }, timestamp: Date.now() })
    expect(service.mainWindow.webContents.send).toHaveBeenCalledWith('task-status-updated', { taskId: 't1', status: 'COMPLETED' })
  })

  it('PRINTER_STATUS 通知渲染进程', async () => {
    await setupConnected()
    simulateMessage({ type: 'PRINTER_STATUS', payload: { printerName: 'p1', status: 'offline' }, timestamp: Date.now() })
    expect(service.mainWindow.webContents.send).toHaveBeenCalledWith('printer-status-changed', { printerName: 'p1', status: 'offline' })
  })

  it('HEALTH_CHECK 不触发额外通知', async () => {
    await setupConnected()
    simulateMessage({ type: 'HEALTH_CHECK', payload: {}, timestamp: Date.now() })
    // 连接时已发送 'websocket-connected'，HEALTH_CHECK 不应再增加调用次数
    expect(service.mainWindow.webContents.send).toHaveBeenCalledTimes(1)
  })

  it('未知消息类型不崩溃', async () => {
    await setupConnected()
    simulateMessage({ type: 'UNKNOWN_TYPE', payload: {}, timestamp: Date.now() })
    // 不抛异常即通过
  })

  it('无效 JSON 消息不崩溃', async () => {
    service.connect()
    await new Promise(resolve => setTimeout(resolve, 20))
    const handlers = service.ws._eventListeners.get('message')
    expect(() => handlers.forEach((fn: Function) => fn(Buffer.from('not-json')))).not.toThrow()
  })
})

// ── 重连机制 ─────────────────────────────────────────────────

describe('重连机制', () => {
  it('WebSocket 关闭后调度重连', async () => {
    service.connect()
    await new Promise(resolve => setTimeout(resolve, 20))

    // 手动触发 close 事件
    service.ws._emit('close')

    expect(service.reconnectTimeout).not.toBeNull()
    expect(service.isConnected).toBe(false)
  })

  it('达到最大重连次数后停止', () => {
    service.reconnectAttempts = 10
    service.scheduleReconnect()
    expect(service.reconnectTimeout).toBeNull()
  })
})

// ── 心跳 ─────────────────────────────────────────────────────

describe('心跳', () => {
  it('连接后启动心跳定时器', async () => {
    service.connect()
    await new Promise(resolve => setTimeout(resolve, 20))
    expect(service.heartbeatInterval).not.toBeNull()
  })

  it('断开后停止心跳', async () => {
    service.connect()
    await new Promise(resolve => setTimeout(resolve, 20))
    service.disconnect()
    expect(service.heartbeatInterval).toBeNull()
  })
})

// ── setMainWindow ────────────────────────────────────────────

describe('setMainWindow()', () => {
  it('设置主窗口引用', () => {
    expect(service.mainWindow).toBeNull()
    const mockWin = { webContents: { send: vi.fn() }, isDestroyed: vi.fn(() => false) }
    service.setMainWindow(mockWin)
    expect(service.mainWindow).toBe(mockWin)
  })
})
