import { app, BrowserWindow, ipcMain } from 'electron'
import Store from 'electron-store'

interface PrinterInfo {
  name: string
  displayName: string
  description: string
  status: 'ready' | 'printing' | 'error' | 'offline'
  isDefault: boolean
  options: Record<string, any>
}

interface PrintOptions {
  silent?: boolean
  printBackground?: boolean
  color?: boolean
  margins?: {
    top: number
    bottom: number
    left: number
    right: number
  }
  pageSize?: string
  copies?: number
  duplex?: boolean
}

interface PrintResult {
  success: boolean
  printerName: string
  taskId: string
  error?: string
  duration?: number
}

class PrinterService {
  private mainWindow: BrowserWindow | null = null
  private store: Store
  private defaultPrinter: string | null = null
  private printerList: PrinterInfo[] = []
  private printingTasks: Map<string, { startTime: number; printerName: string }> = new Map()

  constructor() {
    this.store = new Store()
    this.defaultPrinter = this.store.get('printer.default', null) as string | null
    this.setupIpcHandlers()
  }

  public setMainWindow(window: BrowserWindow): void {
    this.mainWindow = window
  }

  public async getPrinterList(): Promise<PrinterInfo[]> {
    const webContents = this.mainWindow?.webContents
    if (!webContents) {
      return []
    }

    try {
      const printers = webContents.getPrintersAsync ? 
        await webContents.getPrintersAsync() : 
        (webContents as any).getPrinters()

      this.printerList = printers.map((printer: any) => ({
        name: printer.name,
        displayName: printer.displayName || printer.name,
        description: printer.description || '',
        status: this.mapPrinterStatus(printer.status),
        isDefault: printer.isDefault,
        options: printer.options || {}
      }))

      return this.printerList
    } catch (error: any) {
      console.error('[PrinterService] Failed to get printer list:', error.message)
      return []
    }
  }

  private mapPrinterStatus(status: number): 'ready' | 'printing' | 'error' | 'offline' {
    switch (status) {
      case 0: return 'ready'
      case 1: return 'printing'
      case 2: return 'error'
      case 3: return 'offline'
      default: return 'ready'
    }
  }

  public async print(taskId: string, content: string, options?: PrintOptions): Promise<PrintResult> {
    const printerName = options?.silent ? 
      (this.defaultPrinter || this.getDefaultPrinterName()) : 
      undefined

    const startTime = Date.now()
    this.printingTasks.set(taskId, { startTime, printerName: printerName || 'default' })

    try {
      const printOptions = {
        silent: options?.silent ?? true,
        printBackground: options?.printBackground ?? true,
        color: options?.color ?? true,
        margins: options?.margins ?? { top: 0, bottom: 0, left: 0, right: 0 },
        pageSize: options?.pageSize ?? 'A4',
        copies: options?.copies ?? 1,
        duplex: options?.duplex ?? false,
        printerName
      }

      if (this.mainWindow && !this.mainWindow.isDestroyed()) {
        await this.mainWindow.webContents.print(printOptions as any)
      }

      const duration = Date.now() - startTime
      this.printingTasks.delete(taskId)

      return {
        success: true,
        printerName: printerName || 'default',
        taskId,
        duration
      }
    } catch (error: any) {
      const duration = Date.now() - startTime
      this.printingTasks.delete(taskId)

      return {
        success: false,
        printerName: printerName || 'default',
        taskId,
        error: error.message,
        duration
      }
    }
  }

  public async printPDF(taskId: string, pdfPath: string, options?: PrintOptions): Promise<PrintResult> {
    const printerName = this.defaultPrinter || this.getDefaultPrinterName()
    const startTime = Date.now()

    try {
      this.printingTasks.set(taskId, { startTime, printerName })

      if (this.mainWindow && !this.mainWindow.isDestroyed()) {
        const win = new BrowserWindow({
          show: false,
          webPreferences: {
            nodeIntegration: false,
            contextIsolation: true
          }
        })

        await win.loadFile(pdfPath)
        
        await win.webContents.print({
          silent: true,
          printerName,
          printBackground: true,
          copies: options?.copies ?? 1
        })

        win.close()
      }

      const duration = Date.now() - startTime
      this.printingTasks.delete(taskId)

      return {
        success: true,
        printerName,
        taskId,
        duration
      }
    } catch (error: any) {
      const duration = Date.now() - startTime
      this.printingTasks.delete(taskId)

      return {
        success: false,
        printerName,
        taskId,
        error: error.message,
        duration
      }
    }
  }

  public async printHTML(taskId: string, htmlContent: string, options?: PrintOptions): Promise<PrintResult> {
    const printerName = this.defaultPrinter || this.getDefaultPrinterName()
    const startTime = Date.now()

    try {
      this.printingTasks.set(taskId, { startTime, printerName })

      const win = new BrowserWindow({
        show: false,
        webPreferences: {
          nodeIntegration: false,
          contextIsolation: true
        }
      })

      await win.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(htmlContent)}`)
      
      await win.webContents.print({
        silent: true,
        printerName,
        printBackground: true,
        margins: options?.margins ?? { top: 10, bottom: 10, left: 10, right: 10 },
        pageSize: options?.pageSize ?? 'A4',
        copies: options?.copies ?? 1
      })

      win.close()

      const duration = Date.now() - startTime
      this.printingTasks.delete(taskId)

      return {
        success: true,
        printerName,
        taskId,
        duration
      }
    } catch (error: any) {
      const duration = Date.now() - startTime
      this.printingTasks.delete(taskId)

      return {
        success: false,
        printerName,
        taskId,
        error: error.message,
        duration
      }
    }
  }

  public setDefaultPrinter(printerName: string): void {
    this.defaultPrinter = printerName
    this.store.set('printer.default', printerName)
  }

  public getDefaultPrinter(): PrinterInfo | null {
    if (this.defaultPrinter) {
      return this.printerList.find(p => p.name === this.defaultPrinter) || null
    }
    
    const systemDefault = this.printerList.find(p => p.isDefault)
    return systemDefault || null
  }

  private getDefaultPrinterName(): string {
    const defaultPrinter = this.getDefaultPrinter()
    return defaultPrinter?.name || ''
  }

  public getPrinterStatus(printerName: string): PrinterInfo | null {
    return this.printerList.find(p => p.name === printerName) || null
  }

  public isPrinting(): boolean {
    return this.printingTasks.size > 0
  }

  public getPrintingTasks(): string[] {
    return Array.from(this.printingTasks.keys())
  }

  public async testPrint(printerName: string): Promise<PrintResult> {
    const testContent = `
      <html>
        <head>
          <style>
            body { font-family: Arial, sans-serif; padding: 20px; }
            h1 { color: #333; }
            p { color: #666; }
            .timestamp { color: #999; font-size: 12px; }
          </style>
        </head>
        <body>
          <h1>打印测试页</h1>
          <p>打印机: ${printerName}</p>
          <p>客户端: ${app.getName()}</p>
          <p class="timestamp">时间: ${new Date().toLocaleString()}</p>
        </body>
      </html>
    `

    return this.printHTML(`test-${Date.now()}`, testContent, { printerName })
  }

  private setupIpcHandlers(): void {
    ipcMain.handle('printer-list', async () => {
      return await this.getPrinterList()
    })

    ipcMain.handle('printer-print', async (_event, taskId: string, content: string, options?: PrintOptions) => {
      return await this.print(taskId, content, options)
    })

    ipcMain.handle('printer-print-pdf', async (_event, taskId: string, pdfPath: string, options?: PrintOptions) => {
      return await this.printPDF(taskId, pdfPath, options)
    })

    ipcMain.handle('printer-print-html', async (_event, taskId: string, htmlContent: string, options?: PrintOptions) => {
      return await this.printHTML(taskId, htmlContent, options)
    })

    ipcMain.handle('printer-set-default', async (_event, printerName: string) => {
      this.setDefaultPrinter(printerName)
      return { success: true, defaultPrinter: printerName }
    })

    ipcMain.handle('printer-get-default', async () => {
      return this.getDefaultPrinter()
    })

    ipcMain.handle('printer-status', async (_event, printerName: string) => {
      return this.getPrinterStatus(printerName)
    })

    ipcMain.handle('printer-is-printing', async () => {
      return { isPrinting: this.isPrinting(), tasks: this.getPrintingTasks() }
    })

    ipcMain.handle('printer-test', async (_event, printerName: string) => {
      return await this.testPrint(printerName)
    })
  }
}

export default new PrinterService()