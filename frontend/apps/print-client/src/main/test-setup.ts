/**
 * Vitest 测试全局 Mock 设置
 *
 * 为 Electron 主进程服务提供模块 mock，
 * 包括 electron、electron-store、axios 等外部依赖。
 */
import { vi } from 'vitest'

// ═══════════════════════════════════════════════════════════════
// electron mock
// ═══════════════════════════════════════════════════════════════
const mockWebContents = {
  send: vi.fn(),
  loadURL: vi.fn(),
  loadFile: vi.fn(),
  print: vi.fn((_options: any, callback?: (success: boolean, error?: string) => void) => {
    callback?.(true)
  }),
  getURL: vi.fn(() => 'about:blank'),
  on: vi.fn(),
  close: vi.fn(),
  destroy: vi.fn(),
  isDestroyed: vi.fn(() => false)
}

const mockBrowserWindow = vi.fn(() => ({
  webContents: mockWebContents,
  show: vi.fn(),
  hide: vi.fn(),
  close: vi.fn(),
  destroy: vi.fn(),
  minimize: vi.fn(),
  loadURL: vi.fn(),
  loadFile: vi.fn(),
  on: vi.fn(),
  isDestroyed: vi.fn(() => false),
  setSize: vi.fn()
}))

const mockIpcMain = {
  handle: vi.fn(),
  on: vi.fn()
}

const mockApp = {
  getPath: vi.fn(() => '/tmp/test-path'),
  getVersion: vi.fn(() => '1.0.0'),
  getName: vi.fn(() => 'print-client'),
  quit: vi.fn(),
  isQuitting: false,
  on: vi.fn(),
  whenReady: vi.fn(() => Promise.resolve()),
  setLoginItemSettings: vi.fn(),
  getLoginItemSettings: vi.fn(() => ({ openAtLogin: false }))
}

const mockSafeStorage = {
  isEncryptionAvailable: vi.fn(() => true),
  encryptString: vi.fn((str: string) => Buffer.from(str)),
  decryptString: vi.fn((buf: Buffer) => buf.toString())
}

const mockNativeImage = {
  createFromPath: vi.fn(() => ({
    resize: vi.fn(() => ({})),
    toDataURL: vi.fn(() => 'data:image/png;base64,test')
  }))
}

const mockTray = vi.fn(() => ({
  setToolTip: vi.fn(),
  setContextMenu: vi.fn(),
  on: vi.fn()
}))

const mockMenu = {
  buildFromTemplate: vi.fn(() => ({
    popup: vi.fn()
  }))
}

vi.mock('electron', () => ({
  app: mockApp,
  BrowserWindow: mockBrowserWindow,
  ipcMain: mockIpcMain,
  safeStorage: mockSafeStorage,
  nativeImage: mockNativeImage,
  Tray: mockTray,
  Menu: mockMenu,
  dialog: {
    showMessageBox: vi.fn(() => Promise.resolve({ response: 0 }))
  },
  screen: {
    getPrimaryDisplay: vi.fn(() => ({
      workArea: { width: 1920, height: 1080 }
    }))
  }
}))

// ═══════════════════════════════════════════════════════════════
// electron-store mock
// ═══════════════════════════════════════════════════════════════
const mockStore = new Map<string, any>()
const MockStore = vi.fn(() => ({
  get: vi.fn((key: string, defaultValue?: any) => {
    const val = mockStore.get(key)
    return val !== undefined ? val : defaultValue
  }),
  set: vi.fn((key: string | Record<string, any>, value?: any) => {
    if (typeof key === 'string') {
      mockStore.set(key, value)
    } else {
      Object.entries(key).forEach(([k, v]) => mockStore.set(k, v))
    }
  }),
  delete: vi.fn((key: string) => mockStore.delete(key)),
  clear: vi.fn(() => mockStore.clear()),
  store: mockStore
}))

vi.mock('electron-store', () => ({
  default: MockStore
}))

// ═══════════════════════════════════════════════════════════════
// axios mock
// ═══════════════════════════════════════════════════════════════
vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => ({
      post: vi.fn(),
      get: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
      interceptors: {
        request: { use: vi.fn() },
        response: { use: vi.fn() }
      }
    })),
    post: vi.fn(),
    get: vi.fn()
  }
}))

// ═══════════════════════════════════════════════════════════════
// node-printer mock
// ═══════════════════════════════════════════════════════════════
vi.mock('node-printer', () => ({
  default: {
    getPrinters: vi.fn(() => []),
    getPrinter: vi.fn(() => null),
    printDirect: vi.fn()
  },
  getPrinters: vi.fn(() => []),
  getPrinter: vi.fn(() => null),
  printDirect: vi.fn()
}))

// ═══════════════════════════════════════════════════════════════
// @electron-toolkit/runtime mock
// ═══════════════════════════════════════════════════════════════
vi.mock('@electron-toolkit/runtime', () => ({
  electronApp: {
    setAppUserModelId: vi.fn()
  },
  optimizer: {
    watchWindowShortcuts: vi.fn()
  },
  is: {
    dev: false
  }
}))

// ═══════════════════════════════════════════════════════════════
// electron-log mock
// ═══════════════════════════════════════════════════════════════
vi.mock('electron-log', () => ({
  default: {
    info: vi.fn(),
    warn: vi.fn(),
    error: vi.fn(),
    debug: vi.fn(),
    initialize: vi.fn()
  },
  info: vi.fn(),
  warn: vi.fn(),
  error: vi.fn(),
  debug: vi.fn(),
  initialize: vi.fn()
}))

// ═══════════════════════════════════════════════════════════════
// crypto mock (保留真实行为)
// ═══════════════════════════════════════════════════════════════
vi.mock('crypto', async () => {
  const actual = await vi.importActual('crypto')
  return actual
})

// ═══════════════════════════════════════════════════════════════
// WebSocket mock (ws)
// ═══════════════════════════════════════════════════════════════
class MockWebSocket {
  public readyState: number = 0
  public onopen: (() => void) | null = null
  public onclose: (() => void) | null = null
  public onerror: ((err: any) => void) | null = null
  public onmessage: ((msg: any) => void) | null = null
  private _eventListeners: Map<string, Set<Function>> = new Map()

  static CONNECTING = 0
  static OPEN = 1
  static CLOSING = 2
  static CLOSED = 3

  constructor(_url: string) {
    this.readyState = MockWebSocket.CONNECTING
    setTimeout(() => {
      this.readyState = MockWebSocket.OPEN
      this._emit('open')
    }, 10)
  }

  on(event: string, handler: Function): void {
    if (!this._eventListeners.has(event)) {
      this._eventListeners.set(event, new Set())
    }
    this._eventListeners.get(event)!.add(handler)
  }

  removeListener(event: string, handler: Function): void {
    this._eventListeners.get(event)?.delete(handler)
  }

  private _emit(event: string, ...args: any[]): void {
    this._eventListeners.get(event)?.forEach(handler => handler(...args))
    const propName = 'on' + event
    const propHandler = (this as any)[propName]
    if (typeof propHandler === 'function') {
      propHandler(...args)
    }
  }

  send = vi.fn()
  close = vi.fn(() => {
    this.readyState = MockWebSocket.CLOSED
    this._emit('close')
  })
  addEventListener = vi.fn()
  removeEventListener = vi.fn()
}

vi.mock('ws', () => ({
  default: MockWebSocket,
  WebSocket: MockWebSocket
}))

// ═══════════════════════════════════════════════════════════════
// pdf-to-printer mock
// ═══════════════════════════════════════════════════════════════
vi.mock('pdf-to-printer', () => ({
  print: vi.fn(() => Promise.resolve({}))
}))

export { mockBrowserWindow, mockWebContents, mockStore, mockIpcMain, mockApp, mockSafeStorage }
