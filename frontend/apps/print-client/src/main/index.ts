import { app, BrowserWindow, ipcMain, Tray, Menu, nativeImage } from 'electron'
import { join } from 'path'
import { electronApp, optimizer, is } from '@electron-toolkit/runtime'
import ElectronStore from 'electron-store'
import ElectronLog from 'electron-log'
import PrinterService from './services/printer'
import WebSocketService from './services/websocket'
import TaskService from './services/task'

ElectronLog.initialize()

const store = new ElectronStore()

let mainWindow: BrowserWindow | null = null
let tray: Tray | null = null
let printerService: PrinterService | null = null
let websocketService: WebSocketService | null = null
let taskService: TaskService | null = null

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

function initServices(): void {
  printerService = new PrinterService()
  websocketService = new WebSocketService(store.get('serverUrl') as string)
  taskService = new TaskService(printerService, websocketService)
  
  websocketService.on('task', (task: any) => {
    taskService.handleTask(task)
    mainWindow?.webContents.send('task-received', task)
  })
  
  websocketService.on('status', (status: any) => {
    mainWindow?.webContents.send('status-update', status)
  })
}

function setupIpcHandlers(): void {
  ipcMain.handle('get-printers', async () => {
    return printerService?.getPrinters() || []
  })
  
  ipcMain.handle('get-default-printer', async () => {
    return printerService?.getDefaultPrinter()
  })
  
  ipcMain.handle('set-default-printer', async (_, printerName: string) => {
    store.set('defaultPrinter', printerName)
    return true
  })
  
  ipcMain.handle('get-settings', async () => {
    return store.store
  })
  
  ipcMain.handle('save-settings', async (_, settings: any) => {
    store.set(settings)
    if (settings.serverUrl) {
      websocketService?.connect(settings.serverUrl)
    }
    return true
  })
  
  ipcMain.handle('get-tasks', async (_, params?: any) => {
    return taskService?.getTasks(params) || []
  })
  
  ipcMain.handle('get-task', async (_, taskId: string) => {
    return taskService?.getTask(taskId)
  })
  
  ipcMain.handle('print-task', async (_, taskId: string) => {
    return taskService?.printTask(taskId)
  })
  
  ipcMain.handle('cancel-task', async (_, taskId: string) => {
    return taskService?.cancelTask(taskId)
  })
  
  ipcMain.handle('retry-task', async (_, taskId: string) => {
    return taskService?.retryTask(taskId)
  })
  
  ipcMain.handle('get-templates', async () => {
    return taskService?.getTemplates() || []
  })
  
  ipcMain.handle('test-print', async (_, printerName: string) => {
    return printerService?.testPrint(printerName)
  })
  
  ipcMain.handle('connect-server', async (_, serverUrl: string) => {
    websocketService?.connect(serverUrl)
    return true
  })
  
  ipcMain.handle('disconnect-server', async () => {
    websocketService?.disconnect()
    return true
  })
  
  ipcMain.handle('get-connection-status', async () => {
    return websocketService?.getStatus()
  })
}

app.whenReady().then(() => {
  electronApp.setAppUserModelId('com.aiedge.print-client')
  
  app.on('browser-window-created', (_, window) => {
    optimizer.watchWindowShortcuts(window)
  })
  
  createWindow()
  createTray()
  initServices()
  setupIpcHandlers()
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('before-quit', () => {
  app.isQuitting = true
  websocketService?.disconnect()
})

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow()
  }
})