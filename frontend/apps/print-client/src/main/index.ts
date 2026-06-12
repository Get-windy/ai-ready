import { app, BrowserWindow, ipcMain, Tray, Menu, nativeImage, safeStorage } from 'electron'
import { join } from 'path'
import { electronApp, optimizer, is } from '@electron-toolkit/utils'
import Store from 'electron-store'
import ElectronLog from 'electron-log'
import PrinterService from './services/printer'
import WebSocketService from './services/websocket'
import TaskService from './services/task'
import AuthService from './services/auth'

ElectronLog.initialize()

const store = new Store()

let mainWindow: BrowserWindow | null = null
let tray: Tray | null = null

function createWindow(): void {
  mainWindow = new BrowserWindow({
    width: 900,
    height: 670,
    show: false,
    autoHideMenuBar: true,
    frame: false,
    titleBarStyle: 'hiddenInset',
    icon: join(__dirname, '../../resources/icon.png'),
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      sandbox: false,
      contextIsolation: true,
      nodeIntegration: false
    }
  })

  mainWindow.on('ready-to-show', () => {
    mainWindow?.show()
  })

  mainWindow.on('close', (event) => {
    if (!app.isQuitting) {
      event.preventDefault()
      mainWindow?.hide()
    }
  })

  if (is.dev && process.env['ELECTRON_RENDERER_URL']) {
    mainWindow.loadURL(process.env['ELECTRON_RENDERER_URL'])
  } else {
    mainWindow.loadFile(join(__dirname, '../renderer/index.html'))
  }
}

function createTray(): void {
  const icon = nativeImage.createFromPath(join(__dirname, '../../resources/icon.png'))
  tray = new Tray(icon.resize({ width: 16, height: 16 }))

  const contextMenu = Menu.buildFromTemplate([
    { label: '显示主窗口', click: () => mainWindow?.show() },
    { label: '打印队列', click: () => mainWindow?.webContents.send('navigate', '/queue') },
    { label: '打印机设置', click: () => mainWindow?.webContents.send('navigate', '/settings') },
    { type: 'separator' },
    { label: '退出', click: () => {
      app.isQuitting = true
      app.quit()
    }}
  ])

  tray.setToolTip('智企连打印客户端')
  tray.setContextMenu(contextMenu)

  tray.on('click', () => {
    mainWindow?.show()
  })
}

function setupServices(): void {
  if (!mainWindow) return

  // 将 mainWindow 注入各服务（用于向渲染进程发送事件）
  PrinterService.setMainWindow(mainWindow)
  WebSocketService.setMainWindow(mainWindow)
  TaskService.setMainWindow(mainWindow)

  // WebSocket 收到打印任务 → TaskService 处理
  WebSocketService.onTask((task: any) => {
    TaskService.handleServerTask(task)
    try {
      mainWindow?.webContents.send('task-received', task)
    } catch { /* ignore if window destroyed */ }
  })

  // 启动 WebSocket 连接（如已配置 serverUrl）
  const savedUrl = store.get('serverUrl') as string
  if (savedUrl) {
    WebSocketService.connect()
  }
}

function setupIpcHandlers(): void {
  // ── 打印机 ────────────────────────────────
  ipcMain.handle('get-printers', async () => {
    return PrinterService.getPrinterList()
  })

  ipcMain.handle('get-default-printer', async () => {
    return PrinterService.getDefaultPrinter()
  })

  ipcMain.handle('set-default-printer', async (_, printerName: string) => {
    PrinterService.setDefaultPrinter(printerName)
    return true
  })

  ipcMain.handle('test-print', async (_, printerName: string) => {
    return PrinterService.printHTML(
      'test',
      '<h1 style="text-align:center;margin-top:40vh">智企连 · 打印客户端测试页</h1>',
      { printerName, silent: true }
    )
  })

  // ── 设置 ────────────────────────────────
  ipcMain.handle('get-settings', async () => {
    return store.store
  })

  ipcMain.handle('save-settings', async (_, settings: any) => {
    store.set(settings)
    if (settings.serverUrl) {
      store.set('websocket.serverUrl', settings.serverUrl)
    }
    return true
  })

  ipcMain.handle('get-setting', async (_, key: string) => {
    return store.get(key)
  })

  ipcMain.handle('set-setting', async (_, key: string, value: any) => {
    store.set(key, value)
    return true
  })

  // ── 任务 ────────────────────────────────
  ipcMain.handle('get-tasks', async () => {
    return TaskService.getQueue()
  })

  ipcMain.handle('get-task', async (_, taskId: string) => {
    return TaskService.getTask(taskId)
  })

  ipcMain.handle('get-task-by-id', async (_, taskId: string) => {
    return TaskService.getTask(taskId)
  })

  ipcMain.handle('print-task', async () => {
    TaskService.processNextTask()
    return { success: true }
  })

  ipcMain.handle('cancel-task', async (_, taskId: string) => {
    return { success: TaskService.cancelTask(taskId) }
  })

  ipcMain.handle('retry-task', async (_, taskId: string) => {
    return { success: TaskService.retryTask(taskId) }
  })

  ipcMain.handle('reprint-task', async (_, taskId: string) => {
    return { success: TaskService.retryTask(taskId) }
  })

  // ── WebSocket 连接 ──────────────────────
  ipcMain.handle('connect-server', async () => {
    WebSocketService.connect()
    return true
  })

  ipcMain.handle('disconnect-server', async () => {
    WebSocketService.disconnect()
    return true
  })

  ipcMain.handle('get-connection-status', async () => {
    return WebSocketService.getStatus()
  })

  // ── 模板 ────────────────────────────────
  ipcMain.handle('get-templates', async () => {
    return store.get('templates', [])
  })

  ipcMain.handle('get-template', async (_, templateId: string) => {
    const templates = store.get('templates', []) as any[]
    return templates.find((t: any) => t.id === templateId) || null
  })

  ipcMain.handle('create-template', async (_, template: any) => {
    const templates = store.get('templates', []) as any[]
    template.id = template.id || `tpl_${Date.now()}`
    templates.push(template)
    store.set('templates', templates)
    return template
  })

  ipcMain.handle('update-template', async (_, template: any) => {
    const templates = store.get('templates', []) as any[]
    const idx = templates.findIndex((t: any) => t.id === template.id)
    if (idx >= 0) {
      templates[idx] = template
      store.set('templates', templates)
    }
    return true
  })

  ipcMain.handle('delete-template', async (_, templateId: string) => {
    const templates = store.get('templates', []) as any[]
    store.set('templates', templates.filter((t: any) => t.id !== templateId))
    return true
  })

  ipcMain.handle('set-default-template', async (_, templateId: string) => {
    const templates = store.get('templates', []) as any[]
    store.set('templates', templates.map((t: any) => ({ ...t, isDefault: t.id === templateId })))
    return true
  })

  // ── 认证 ────────────────────────────────
  ipcMain.handle('auth-login', async (
    _,
    serverUrl: string,
    tenantName: string,
    username: string,
    password: string,
    rememberMe: boolean
  ) => {
    return AuthService.login(serverUrl, tenantName, username, password, rememberMe)
  })

  ipcMain.handle('auth-register', async (
    _,
    serverUrl: string,
    clientName: string,
    clientVersion?: string
  ) => {
    return AuthService.register(serverUrl, clientName, clientVersion)
  })

  ipcMain.handle('auth-logout', async (_, serverUrl: string) => {
    await AuthService.logout(serverUrl)
    return true
  })

  ipcMain.handle('auth-check-token', async (_, serverUrl: string) => {
    return AuthService.checkToken(serverUrl)
  })

  ipcMain.handle('auth-get-state', async () => {
    return AuthService.getAuthState()
  })

  ipcMain.handle('auth-try-auto-login', async (_, serverUrl: string) => {
    return AuthService.tryAutoLogin(serverUrl)
  })

  ipcMain.handle('auth-get-credentials', async () => {
    if (!store.get('auth.rememberMe')) return null
    const credentials = store.get('auth.credentials') as any
    if (!credentials) return null
    let password = ''
    if (credentials.encryptedPassword && safeStorage.isEncryptionAvailable()) {
      try {
        password = safeStorage.decryptString(Buffer.from(credentials.encryptedPassword, 'base64'))
      } catch { /* decryption failed */ }
    } else if (credentials.password) {
      password = Buffer.from(credentials.password, 'base64').toString('utf-8')
    }
    return {
      tenantName: credentials.tenantName,
      username: credentials.username,
      password
    }
  })

  // ── 应用控制 ────────────────────────────
  ipcMain.handle('app-set-auto-start', async (_, enabled: boolean) => {
    app.setLoginItemSettings({
      openAtLogin: enabled,
      path: process.execPath
    })
    store.set('settings.autoStart', enabled)
    return true
  })

  ipcMain.handle('app-get-auto-start', async () => {
    return store.get('settings.autoStart', false)
  })

  ipcMain.handle('app-quit', async () => {
    app.isQuitting = true
    app.quit()
    return true
  })

  ipcMain.handle('app-minimize', async () => {
    mainWindow?.minimize()
    return true
  })
}

app.whenReady().then(() => {
  electronApp.setAppUserModelId('com.aiedge.print-client')

  app.on('browser-window-created', (_, window) => {
    optimizer.watchWindowShortcuts(window)
  })

  createWindow()
  createTray()
  setupServices()
  setupIpcHandlers()
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('before-quit', () => {
  app.isQuitting = true
  WebSocketService.disconnect()
})

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow()
  }
})
