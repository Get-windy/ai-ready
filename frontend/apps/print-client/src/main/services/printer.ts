import { app, BrowserWindow, ipcMain } from 'electron'
import Store from 'electron-store'
import { renderTemplateToHtml, TemplateJson } from './formatEngine'

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
  margins?: { top: number; bottom: number; left: number; right: number }
  pageSize?: string
  copies?: number
  duplex?: boolean
  printerName?: string
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
    if (!webContents) return []

    try {
      const printers: any[] = webContents.getPrintersAsync
        ? await webContents.getPrintersAsync()
        : (webContents as any).getPrinters()

      this.printerList = printers.map((p: any) => ({
        name: p.name,
        displayName: p.displayName || p.name,
        description: p.description || '',
        status: this.mapPrinterStatus(p.status),
        isDefault: p.isDefault,
        options: p.options || {}
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

  /**
   * 使用格式化引擎渲染模板 JSON 为 HTML 并打印
   */
  public async printFromTemplate(
    taskId: string,
    templateJson: TemplateJson,
    dataJson: Record<string, any>,
    options?: PrintOptions
  ): Promise<PrintResult> {
    const html = renderTemplateToHtml(templateJson, dataJson)
    return this.printHTML(taskId, html, options)
  }

  public async print(taskId: string, content: string, options?: PrintOptions): Promise<PrintResult> {
    const printerName = options?.silent
      ? (this.defaultPrinter || this.getDefaultPrinterName())
      : undefined

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

      return { success: true, printerName: printerName || 'default', taskId, duration }
    } catch (error: any) {
      const duration = Date.now() - startTime
      this.printingTasks.delete(taskId)
      return { success: false, printerName: printerName || 'default', taskId, error: error.message, duration }
    }
  }

  public async printHTML(taskId: string, htmlContent: string, options?: PrintOptions): Promise<PrintResult> {
    const printerName = this.defaultPrinter || this.getDefaultPrinterName()
    const startTime = Date.now()
    this.printingTasks.set(taskId, { startTime, printerName })

    const win = new BrowserWindow({
      show: false,
      webPreferences: { nodeIntegration: false, contextIsolation: true }
    })

    try {
      await win.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(htmlContent)}`)

      await win.webContents.print({
        silent: true,
        printerName,
        printBackground: true,
        margins: options?.margins ?? { top: 10, bottom: 10, left: 10, right: 10 },
        pageSize: options?.pageSize ?? 'A4',
        copies: options?.copies ?? 1
      })

      const duration = Date.now() - startTime
      this.printingTasks.delete(taskId)
      return { success: true, printerName, taskId, duration }
    } catch (error: any) {
      const duration = Date.now() - startTime
      this.printingTasks.delete(taskId)
      return { success: false, printerName, taskId, error: error.message, duration }
    } finally {
      if (!win.isDestroyed()) {
        win.close()
      }
    }
  }

  /**
   * 截取模板 HTML 渲染截图（用于截图确认流程）
   */
  public async captureScreenshot(
    templateJson: TemplateJson,
    dataJson: Record<string, any>
  ): Promise<string> {
    const html = renderTemplateToHtml(templateJson, dataJson)

    const win = new BrowserWindow({
      show: false,
      webPreferences: { nodeIntegration: false, contextIsolation: true }
    })

    try {
      await win.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(html)}`)

      const image = await win.webContents.capturePage()
      return image.toDataURL()
    } finally {
      if (!win.isDestroyed()) {
        win.close()
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
    return this.printerList.find(p => p.isDefault) || null
  }

  private getDefaultPrinterName(): string {
    return this.getDefaultPrinter()?.name || ''
  }

  public isPrinting(): boolean {
    return this.printingTasks.size > 0
  }

  private setupIpcHandlers(): void {
    ipcMain.handle('printer-list', async () => await this.getPrinterList())
    ipcMain.handle('printer-print-html', async (_event, taskId: string, htmlContent: string, options?: PrintOptions) =>
      await this.printHTML(taskId, htmlContent, options)
    )
    ipcMain.handle('printer-print-template', async (_event, taskId: string, templateJson: TemplateJson, dataJson: Record<string, any>, options?: PrintOptions) =>
      await this.printFromTemplate(taskId, templateJson, dataJson, options)
    )
    ipcMain.handle('printer-set-default', async (_event, printerName: string) => {
      this.setDefaultPrinter(printerName)
      return { success: true, defaultPrinter: printerName }
    })
    ipcMain.handle('printer-get-default', async () => this.getDefaultPrinter())
    ipcMain.handle('printer-is-printing', async () => ({
      isPrinting: this.isPrinting(),
      tasks: Array.from(this.printingTasks.keys())
    }))
    ipcMain.handle('printer-capture', async (_event, templateJson: TemplateJson, dataJson: Record<string, any>) =>
      await this.captureScreenshot(templateJson, dataJson)
    )
  }
}

export default new PrinterService()
