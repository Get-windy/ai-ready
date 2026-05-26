import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

interface Printer {
  id: string
  name: string
  status: 'online' | 'offline' | 'error'
  paperSize: string
  location: string
  lastUsed: string
}

interface PrintTask {
  id: string
  taskNo: string
  templateName: string
  printerName: string
  status: 'pending' | 'printing' | 'completed' | 'failed'
  pageCount: number
  copies: number
  createTime: string
  error?: string
}

interface PrintStats {
  todayTasks: number
  todayCompleted: number
  todayFailed: number
  queueLength: number
}

export const usePrinterStore = defineStore('print-printer', () => {
  const printers = ref<Printer[]>([])
  const loading = ref(false)

  const onlinePrinters = computed(() => 
    printers.value.filter(p => p.status === 'online')
  )

  const offlinePrinters = computed(() => 
    printers.value.filter(p => p.status === 'offline')
  )

  const errorPrinters = computed(() => 
    printers.value.filter(p => p.status === 'error')
  )

  const loadPrinters = async () => {
    loading.value = true
    try {
      const printersData = await window.electronAPI?.printers?.getPrinters()
      printers.value = printersData || []
    } catch {
      printers.value = []
    } finally {
      loading.value = false
    }
  }

  const updatePrinterStatus = (printerId: string, status: string) => {
    const printer = printers.value.find(p => p.id === printerId)
    if (printer) {
      printer.status = status as any
    }
  }

  return {
    printers,
    loading,
    onlinePrinters,
    offlinePrinters,
    errorPrinters,
    loadPrinters,
    updatePrinterStatus
  }
})

export const useQueueStore = defineStore('print-queue', () => {
  const tasks = ref<PrintTask[]>([])
  const currentTask = ref<PrintTask | null>(null)
  const loading = ref(false)
  const stats = ref<PrintStats>({
    todayTasks: 0,
    todayCompleted: 0,
    todayFailed: 0,
    queueLength: 0
  })

  const pendingTasks = computed(() => 
    tasks.value.filter(t => t.status === 'pending')
  )

  const printingTasks = computed(() => 
    tasks.value.filter(t => t.status === 'printing')
  )

  const completedTasks = computed(() => 
    tasks.value.filter(t => t.status === 'completed')
  )

  const failedTasks = computed(() => 
    tasks.value.filter(t => t.status === 'failed')
  )

  const loadTasks = async () => {
    loading.value = true
    try {
      const tasksData = await window.electronAPI?.queue?.getTasks()
      tasks.value = tasksData || []
      calculateStats()
    } catch {
      tasks.value = []
    } finally {
      loading.value = false
    }
  }

  const loadTaskDetail = async (taskId: string) => {
    loading.value = true
    try {
      const task = await window.electronAPI?.tasks?.getTaskById(taskId)
      currentTask.value = task || null
    } catch {
      currentTask.value = null
    } finally {
      loading.value = false
    }
  }

  const reprintTask = async (taskId: string) => {
    try {
      await window.electronAPI?.tasks?.reprint(taskId)
      const task = tasks.value.find(t => t.id === taskId)
      if (task) {
        task.status = 'pending'
      }
    } catch {
      throw new Error('重新打印失败')
    }
  }

  const cancelTask = async (taskId: string) => {
    try {
      await window.electronAPI?.tasks?.cancel(taskId)
      tasks.value = tasks.value.filter(t => t.id !== taskId)
      calculateStats()
    } catch {
      throw new Error('取消任务失败')
    }
  }

  const calculateStats = () => {
    stats.value = {
      todayTasks: tasks.value.length,
      todayCompleted: tasks.value.filter(t => t.status === 'completed').length,
      todayFailed: tasks.value.filter(t => t.status === 'failed').length,
      queueLength: tasks.value.filter(t => t.status === 'pending' || t.status === 'printing').length
    }
  }

  const clearCurrentTask = () => {
    currentTask.value = null
  }

  return {
    tasks,
    currentTask,
    loading,
    stats,
    pendingTasks,
    printingTasks,
    completedTasks,
    failedTasks,
    loadTasks,
    loadTaskDetail,
    reprintTask,
    cancelTask,
    clearCurrentTask
  }
})