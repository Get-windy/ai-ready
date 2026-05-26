import { app, BrowserWindow, ipcMain } from 'electron'
import Store from 'electron-store'
import WebSocketService from './websocket'
import PrinterService from './printer'

interface PrintTask {
  id: string
  templateId: string
  templateName: string
  templateType: string
  printerId: string
  printerName: string
  documentId: string
  documentNo: string
  documentType: string
  content: string
  status: 'pending' | 'queued' | 'printing' | 'completed' | 'failed' | 'cancelled'
  priority: number
  copies: number
  retryCount: number
  maxRetry: number
  createTime: number
  startTime?: number
  completeTime?: number
  error?: string
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
    
    this.taskQueue = persistedQueue.filter(t => t.status === 'pending' || t.status === 'queued')
    this.completedTasks = persistedCompleted.slice(-100)
  }

  private persistTasks(): void {
    this.store.set('task.queue', this.taskQueue)
    this.store.set('task.completed', this.completedTasks.slice(-100))
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

  private sortQueue(): void {
    this.taskQueue.sort((a, b) => {
      if (a.priority !== b.priority) {
        return b.priority - a.priority
      }
      return a.createTime - b.createTime
    })
  }

  public async processNextTask(): Promise<void> {
    if (this.isProcessing || this.taskQueue.length === 0) {
      return
    }

    this.isProcessing = true
    this.currentTask = this.taskQueue.shift()!
    
    this.currentTask.status = 'printing'
    this.currentTask.startTime = Date.now()
    this.persistTasks()
    
    this.sendToRenderer('task-started', this.currentTask)
    WebSocketService.sendTaskStatus(this.currentTask.id, 'printing')

    try {
      const result = await PrinterService.printHTML(
        this.currentTask.id,
        this.currentTask.content,
        {
          copies: this.currentTask.copies,
          silent: true
        }
      )

      if (result.success) {
        this.currentTask.status = 'completed'
        this.currentTask.completeTime = Date.now()
        this.completedTasks.push(this.currentTask)
        
        this.sendToRenderer('task-completed', this.currentTask)
        WebSocketService.sendTaskStatus(this.currentTask.id, 'completed')
        
        if (this.config.notifyOnComplete) {
          this.showNotification('打印完成', `${this.currentTask.documentNo} 已成功打印`)
        }
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

  private handleTaskError(task: PrintTask, error: string): void {
    task.error = error
    
    if (task.retryCount < task.maxRetry) {
      task.retryCount++
      task.status = 'queued'
      
      this.sendToRenderer('task-retrying', { task, retryCount: task.retryCount })
      WebSocketService.sendTaskStatus(task.id, 'retrying', task.retryCount)
      
      const timer = setTimeout(() => {
        this.retryTimers.delete(task.id)
        this.taskQueue.unshift(task)
        this.sortQueue()
        this.persistTasks()
        
        if (!this.isProcessing) {
          this.processNextTask()
        }
      }, this.config.retryInterval)
      
      this.retryTimers.set(task.id, timer)
    } else {
      task.status = 'failed'
      task.completeTime = Date.now()
      this.completedTasks.push(task)
      
      this.sendToRenderer('task-failed', task)
      WebSocketService.sendTaskStatus(task.id, 'failed')
      
      if (this.config.notifyOnError) {
        this.showNotification('打印失败', `${task.documentNo} 打印失败: ${error}`)
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
      WebSocketService.sendTaskStatus(taskId, 'cancelled')
      
      return true
    }

    if (this.currentTask && this.currentTask.id === taskId) {
      this.currentTask.status = 'cancelled'
      this.currentTask.completeTime = Date.now()
      this.completedTasks.push(this.currentTask)
      this.persistTasks()
      
      this.sendToRenderer('task-cancelled', this.currentTask)
      WebSocketService.sendTaskStatus(taskId, 'cancelled')
      
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
        
        if (this.config.autoPrint && !this.isProcessing) {
          this.processNextTask()
        }
        
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
    const queueTask = this.taskQueue.find(t => t.id === taskId)
    if (queueTask) return queueTask
    
    const completedTask = this.completedTasks.find(t => t.id === taskId)
    if (completedTask) return completedTask
    
    if (this.currentTask && this.currentTask.id === taskId) return this.currentTask
    
    return null
  }

  public getQueueLength(): number {
    return this.taskQueue.length
  }

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
    
    if (!this.isProcessing && this.taskQueue.length > 0) {
      this.processNextTask()
    }
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

    ipcMain.handle('task-cancel', async (_event, taskId: string) => {
      return { success: this.cancelTask(taskId) }
    })

    ipcMain.handle('task-retry', async (_event, taskId: string) => {
      return { success: this.retryTask(taskId) }
    })

    ipcMain.handle('task-get', async (_event, taskId: string) => {
      return this.getTask(taskId)
    })

    ipcMain.handle('task-queue', async () => {
      return this.getQueue()
    })

    ipcMain.handle('task-completed', async () => {
      return this.getCompletedTasks()
    })

    ipcMain.handle('task-queue-length', async () => {
      return this.getQueueLength()
    })

    ipcMain.handle('task-clear-completed', async () => {
      this.clearCompleted()
      return { success: true }
    })

    ipcMain.handle('task-pause', async () => {
      this.pauseQueue()
      return { success: true }
    })

    ipcMain.handle('task-resume', async () => {
      this.resumeQueue()
      return { success: true }
    })

    ipcMain.handle('task-config-update', async (_event, config: Partial<TaskQueueConfig>) => {
      Object.assign(this.config, config)
      Object.entries(config).forEach(([key, value]) => {
        this.store.set(`task.${key}`, value)
      })
      return { success: true, config: this.config }
    })

    ipcMain.handle('task-config', async () => {
      return this.config
    })

    ipcMain.handle('task-status', async () => {
      return {
        isProcessing: this.isProcessing,
        queueLength: this.taskQueue.length,
        completedCount: this.completedTasks.length,
        currentTask: this.currentTask
      }
    })
  }
}

export default new TaskService()