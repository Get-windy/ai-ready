import { app, BrowserWindow, ipcMain } from 'electron'
import Store from 'electron-store'
import PrinterService from './printer'
import { renderTemplateToHtml, TemplateJson } from './formatEngine'

interface PrintTask {
  id: string
  taskId?: number
  taskCode?: string
  templateId?: string
  templateName?: string
  templateJson?: TemplateJson
  renderedHtml?: string
  printerId?: string
  printerName?: string
  documentId?: string
  documentNo?: string
  documentType?: string
  dataJson?: Record<string, any>
  content?: string
  status: 'pending' | 'queued' | 'printing' | 'completed' | 'failed' | 'cancelled' | 'waiting_confirm'
  priority: number
  copies: number
  retryCount: number
  maxRetry: number
  createTime: number
  startTime?: number
  completeTime?: number
  error?: string
  screenshotMode?: string
  screenshotId?: number
  chainId?: number
  stepOrder?: number
}

interface TaskQueueConfig {
  maxConcurrent: number
  retryInterval: number
  autoPrint: boolean
  notifyOnComplete: boolean
  notifyOnError: boolean
}

class TaskService {
  private mainWindow: BrowserWindow | null = null
  private store: Store
  private taskQueue: PrintTask[] = []
  private currentTask: PrintTask | null = null
  private completedTasks: PrintTask[] = []
  /** 等待用户确认截图的的任务（不占用 currentTask，允许队列继续处理） */
  private waitingConfirmTasks: PrintTask[] = []
  private config: TaskQueueConfig
  private isProcessing: boolean = false
  private retryTimers: Map<string, NodeJS.Timeout> = new Map()

  constructor() {
    this.store = new Store()
    this.config = this.loadConfig()
    this.loadPersistedTasks()
    this.setupIpcHandlers()
  }

  private loadConfig(): TaskQueueConfig {
    return {
      maxConcurrent: this.store.get('task.maxConcurrent', 1) as number,
      retryInterval: this.store.get('task.retryInterval', 5000) as number,
      autoPrint: this.store.get('task.autoPrint', true) as boolean,
      notifyOnComplete: this.store.get('task.notifyOnComplete', true) as boolean,
      notifyOnError: this.store.get('task.notifyOnError', true) as boolean
    }
  }

  private loadPersistedTasks(): void {
    const persistedQueue = this.store.get('task.queue', []) as PrintTask[]
    const persistedCompleted = this.store.get('task.completed', []) as PrintTask[]
    const persistedWaiting = this.store.get('task.waitingConfirm', []) as PrintTask[]
    this.taskQueue = persistedQueue.filter(t => t.status === 'pending' || t.status === 'queued')
    this.completedTasks = persistedCompleted.slice(-100)
    this.waitingConfirmTasks = persistedWaiting.filter(t => t.status === 'waiting_confirm')
  }

  private persistTasks(): void {
    this.store.set('task.queue', this.taskQueue)
    this.store.set('task.completed', this.completedTasks.slice(-100))
    this.store.set('task.waitingConfirm', this.waitingConfirmTasks)
  }

  public setMainWindow(window: BrowserWindow): void {
    this.mainWindow = window
  }

  public addTask(task: PrintTask): void {
    task.status = 'pending'
    task.retryCount = 0
    task.createTime = Date.now()
    this.taskQueue.push(task)
    this.sortQueue()
    this.persistTasks()
    this.sendToRenderer('task-added', task)

    if (this.config.autoPrint && !this.isProcessing) {
      this.processNextTask()
    }
  }

  public addTasks(tasks: PrintTask[]): void {
    tasks.forEach(task => {
      task.status = 'pending'
      task.retryCount = 0
      task.createTime = Date.now()
      this.taskQueue.push(task)
    })
    this.sortQueue()
    this.persistTasks()
    this.sendToRenderer('tasks-added', { count: tasks.length })

    if (this.config.autoPrint && !this.isProcessing) {
      this.processNextTask()
    }
  }

  /**
   * 接收服务端分发的任务（新格式）
   */
  public handleServerTask(serverTask: any): void {
    const task: PrintTask = {
      id: serverTask.taskCode || `task-${Date.now()}`,
      taskId: serverTask.taskId,
      taskCode: serverTask.taskCode,
      templateJson: serverTask.templateJson,
      renderedHtml: serverTask.renderedHtml,
      dataJson: serverTask.dataJson,
      printerName: serverTask.printerName,
      copies: serverTask.copies || 1,
      status: 'pending',
      priority: serverTask.priority || 5,
      retryCount: 0,
      maxRetry: 3,
      createTime: Date.now(),
      screenshotMode: serverTask.screenshotMode,
      screenshotId: serverTask.screenshotId,
      chainId: serverTask.chainId,
      stepOrder: serverTask.stepOrder
    }

    if (serverTask.renderedHtml) {
      task.content = serverTask.renderedHtml
    }

    this.addTask(task)
  }

  private sortQueue(): void {
    this.taskQueue.sort((a, b) => {
      if (a.priority !== b.priority) return b.priority - a.priority
      return a.createTime - b.createTime
    })
  }

  public async processNextTask(): Promise<void> {
    if (this.isProcessing || this.taskQueue.length === 0) return

    this.isProcessing = true
    this.currentTask = this.taskQueue.shift()!
    this.currentTask.status = 'printing'
    this.currentTask.startTime = Date.now()
    this.persistTasks()
    this.sendToRenderer('task-started', this.currentTask)

    try {
      // 使用模板渲染或已渲染的 HTML
      let htmlContent = this.currentTask.content || ''

      if (!htmlContent && this.currentTask.templateJson && this.currentTask.dataJson) {
        htmlContent = renderTemplateToHtml(
          this.currentTask.templateJson,
          this.currentTask.dataJson
        )
      }

      if (!htmlContent) {
        throw new Error('没有可打印的内容')
      }

      // 截图确认模式 —— 移到等待队列，继续处理下一个任务
      if (this.currentTask.screenshotMode === 'MANUAL_CONFIRM' ||
          this.currentTask.screenshotMode === 'AUTO_CONFIRM') {
        this.currentTask.status = 'waiting_confirm'
        this.sendToRenderer('task-waiting-confirm', this.currentTask)

        // 保存到等待队列，释放 currentTask 以便继续处理其他任务
        this.waitingConfirmTasks.push(this.currentTask)
        this.currentTask = null
        this.isProcessing = false
        this.persistTasks()

        // 继续处理下一个任务
        if (this.taskQueue.length > 0) {
          setTimeout(() => this.processNextTask(), 100)
        }
        return
      }

      const result = await PrinterService.printHTML(
        this.currentTask.id,
        htmlContent,
        { copies: this.currentTask.copies, silent: true }
      )

      if (result.success) {
        this.completeTask(this.currentTask, '打印完成')
      } else {
        this.handleTaskError(this.currentTask, result.error || '打印失败')
      }
    } catch (error: any) {
      this.handleTaskError(this.currentTask, error.message)
    }

    this.currentTask = null
    this.isProcessing = false
    this.persistTasks()

    if (this.config.autoPrint && this.taskQueue.length > 0) {
      setTimeout(() => this.processNextTask(), 1000)
    }
  }

  /**
   * 确认截图并继续打印
   */
  public async confirmScreenshotAndPrint(taskId: string): Promise<boolean> {
    const idx = this.waitingConfirmTasks.findIndex(t => t.id === taskId)
    if (idx === -1) return false

    const task = this.waitingConfirmTasks[idx]
    this.waitingConfirmTasks.splice(idx, 1)
    this.currentTask = task
    this.currentTask.status = 'printing'
    this.sendToRenderer('task-started', this.currentTask)

    try {
      let htmlContent = this.currentTask.content || ''
      if (!htmlContent && this.currentTask.templateJson && this.currentTask.dataJson) {
        htmlContent = renderTemplateToHtml(
          this.currentTask.templateJson,
          this.currentTask.dataJson
        )
      }

      const result = await PrinterService.printHTML(
        this.currentTask.id,
        htmlContent,
        { copies: this.currentTask.copies, silent: true }
      )

      if (result.success) {
        this.completeTask(this.currentTask, '截图已确认，打印完成')
      } else {
        this.handleTaskError(this.currentTask, result.error || '打印失败')
      }
    } catch (error: any) {
      this.handleTaskError(this.currentTask, error.message)
    }

    this.currentTask = null
    this.isProcessing = false
    this.persistTasks()

    if (this.config.autoPrint && this.taskQueue.length > 0) {
      setTimeout(() => this.processNextTask(), 1000)
    }

    return true
  }

  private completeTask(task: PrintTask, message: string): void {
    task.status = 'completed'
    task.completeTime = Date.now()
    task.error = undefined
    this.completedTasks.push(task)
    this.sendToRenderer('task-completed', task)

    if (this.config.notifyOnComplete) {
      this.showNotification('打印完成', `${task.documentNo || task.taskCode} 已成功打印`)
    }
  }

  private handleTaskError(task: PrintTask, error: string): void {
    task.error = error

    if (task.retryCount < task.maxRetry) {
      task.retryCount++
      task.status = 'queued'
      this.sendToRenderer('task-retrying', { task, retryCount: task.retryCount })

      const timer = setTimeout(() => {
        this.retryTimers.delete(task.id)
        this.taskQueue.unshift(task)
        this.sortQueue()
        this.persistTasks()
        if (!this.isProcessing) this.processNextTask()
      }, this.config.retryInterval)

      this.retryTimers.set(task.id, timer)
    } else {
      task.status = 'failed'
      task.completeTime = Date.now()
      this.completedTasks.push(task)
      this.sendToRenderer('task-failed', task)

      if (this.config.notifyOnError) {
        this.showNotification('打印失败', `${task.documentNo || task.taskCode} 打印失败: ${error}`)
      }
    }
  }

  public cancelTask(taskId: string): boolean {
    const timer = this.retryTimers.get(taskId)
    if (timer) {
      clearTimeout(timer)
      this.retryTimers.delete(taskId)
    }

    const queueIndex = this.taskQueue.findIndex(t => t.id === taskId)
    if (queueIndex !== -1) {
      const task = this.taskQueue[queueIndex]
      task.status = 'cancelled'
      task.completeTime = Date.now()
      this.completedTasks.push(task)
      this.taskQueue.splice(queueIndex, 1)
      this.persistTasks()
      this.sendToRenderer('task-cancelled', task)
      return true
    }

    // 检查等待确认的任务
    const waitIdx = this.waitingConfirmTasks.findIndex(t => t.id === taskId)
    if (waitIdx !== -1) {
      const task = this.waitingConfirmTasks[waitIdx]
      task.status = 'cancelled'
      task.completeTime = Date.now()
      this.completedTasks.push(task)
      this.waitingConfirmTasks.splice(waitIdx, 1)
      this.persistTasks()
      this.sendToRenderer('task-cancelled', task)
      return true
    }

    if (this.currentTask && this.currentTask.id === taskId) {
      const task = this.currentTask
      task.status = 'cancelled'
      task.completeTime = Date.now()
      this.completedTasks.push(task)
      this.currentTask = null
      this.isProcessing = false
      this.persistTasks()
      this.sendToRenderer('task-cancelled', task)
      if (this.config.autoPrint && this.taskQueue.length > 0) {
        setTimeout(() => this.processNextTask(), 100)
      }
      return true
    }

    return false
  }

  public retryTask(taskId: string): boolean {
    const completedIndex = this.completedTasks.findIndex(t => t.id === taskId)
    if (completedIndex !== -1) {
      const task = this.completedTasks[completedIndex]
      if (task.status === 'failed' || task.status === 'cancelled') {
        task.status = 'pending'
        task.retryCount = 0
        task.error = undefined
        task.startTime = undefined
        task.completeTime = undefined
        this.completedTasks.splice(completedIndex, 1)
        this.taskQueue.push(task)
        this.sortQueue()
        this.persistTasks()
        this.sendToRenderer('task-retried', task)
        if (this.config.autoPrint && !this.isProcessing) this.processNextTask()
        return true
      }
    }
    return false
  }

  public getQueue(): PrintTask[] {
    return [...this.taskQueue]
  }

  public getCompletedTasks(): PrintTask[] {
    return [...this.completedTasks]
  }

  public getTask(taskId: string): PrintTask | null {
    return this.taskQueue.find(t => t.id === taskId)
      || this.waitingConfirmTasks.find(t => t.id === taskId)
      || this.completedTasks.find(t => t.id === taskId)
      || (this.currentTask && this.currentTask.id === taskId ? this.currentTask : null)
  }

  public getQueueLength(): number { return this.taskQueue.length }
  public clearCompleted(): void {
    this.completedTasks = []
    this.persistTasks()
    this.sendToRenderer('completed-cleared')
  }
  public pauseQueue(): void {
    this.config.autoPrint = false
    this.store.set('task.autoPrint', false)
    this.sendToRenderer('queue-paused')
  }
  public resumeQueue(): void {
    this.config.autoPrint = true
    this.store.set('task.autoPrint', true)
    this.sendToRenderer('queue-resumed')
    if (!this.isProcessing && this.taskQueue.length > 0) this.processNextTask()
  }

  private showNotification(title: string, body: string): void {
    if (this.mainWindow && !this.mainWindow.isDestroyed()) {
      this.mainWindow.webContents.send('show-notification', { title, body })
    }
  }

  private sendToRenderer(channel: string, data: any): void {
    if (this.mainWindow && !this.mainWindow.isDestroyed()) {
      this.mainWindow.webContents.send(channel, data)
    }
  }

  private setupIpcHandlers(): void {
    ipcMain.handle('task-add', async (_event, task: PrintTask) => {
      this.addTask(task)
      return { success: true, taskId: task.id }
    })
    ipcMain.handle('task-add-batch', async (_event, tasks: PrintTask[]) => {
      this.addTasks(tasks)
      return { success: true, count: tasks.length }
    })
    ipcMain.handle('task-server-push', async (_event, serverTask: any) => {
      this.handleServerTask(serverTask)
      return { success: true }
    })
    ipcMain.handle('task-cancel', async (_event, taskId: string) => ({ success: this.cancelTask(taskId) }))
    ipcMain.handle('task-retry', async (_event, taskId: string) => ({ success: this.retryTask(taskId) }))
    ipcMain.handle('task-get', async (_event, taskId: string) => this.getTask(taskId))
    ipcMain.handle('task-queue', async () => this.getQueue())
    ipcMain.handle('task-completed', async () => this.getCompletedTasks())
    ipcMain.handle('task-queue-length', async () => this.getQueueLength())
    ipcMain.handle('task-clear-completed', async () => { this.clearCompleted(); return { success: true } })
    ipcMain.handle('task-pause', async () => { this.pauseQueue(); return { success: true } })
    ipcMain.handle('task-resume', async () => { this.resumeQueue(); return { success: true } })
    ipcMain.handle('task-confirm-screenshot', async (_event, taskId: string) => ({
      success: await this.confirmScreenshotAndPrint(taskId)
    }))
    ipcMain.handle('task-config-update', async (_event, config: Partial<TaskQueueConfig>) => {
      Object.assign(this.config, config)
      Object.entries(config).forEach(([key, value]) => this.store.set(`task.${key}`, value))
      return { success: true, config: this.config }
    })
    ipcMain.handle('task-config', async () => this.config)
    ipcMain.handle('task-status', async () => ({
      isProcessing: this.isProcessing,
      queueLength: this.taskQueue.length,
      completedCount: this.completedTasks.length,
      currentTask: this.currentTask
    }))
  }
}

export default new TaskService()
