import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '@/api'

interface Task {
  id: string
  taskNo: string
  taskType: 'receive' | 'pick' | 'check'
  status: number
  priority: number
  itemCount: number
  completedCount: number
  createTime: string
  deadline: string
  warehouse: string
  location: string
}

interface TaskStats {
  todayTotal: number
  todayCompleted: number
  pending: number
  inProgress: number
}

export const useTaskStore = defineStore('warehouse-task', () => {
  const tasks = ref<Task[]>([])
  const currentTask = ref<Task | null>(null)
  const loading = ref(false)
  const stats = ref<TaskStats>({
    todayTotal: 0,
    todayCompleted: 0,
    pending: 0,
    inProgress: 0
  })

  const pendingTasks = computed(() => 
    tasks.value.filter(t => t.status === 0)
  )

  const inProgressTasks = computed(() => 
    tasks.value.filter(t => t.status === 1)
  )

  const completedTasks = computed(() => 
    tasks.value.filter(t => t.status === 2)
  )

  const urgentTasks = computed(() => 
    tasks.value.filter(t => t.priority === 1 && t.status !== 2)
  )

  const loadTasks = async () => {
    loading.value = true
    try {
      const res = await api.task.getList()
      tasks.value = res.data || []
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
      const res = await api.task.getDetail(taskId)
      currentTask.value = res.data || null
    } catch {
      currentTask.value = null
    } finally {
      loading.value = false
    }
  }

  const startTask = async (taskId: string) => {
    try {
      await api.task.start(taskId)
      const task = tasks.value.find(t => t.id === taskId)
      if (task) {
        task.status = 1
      }
      calculateStats()
    } catch {
      throw new Error('开始任务失败')
    }
  }

  const completeTask = async (taskId: string) => {
    try {
      await api.task.complete(taskId)
      const task = tasks.value.find(t => t.id === taskId)
      if (task) {
        task.status = 2
        task.completedCount = task.itemCount
      }
      calculateStats()
    } catch {
      throw new Error('完成任务失败')
    }
  }

  const calculateStats = () => {
    stats.value = {
      todayTotal: tasks.value.length,
      todayCompleted: tasks.value.filter(t => t.status === 2).length,
      pending: tasks.value.filter(t => t.status === 0).length,
      inProgress: tasks.value.filter(t => t.status === 1).length
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
    inProgressTasks,
    completedTasks,
    urgentTasks,
    loadTasks,
    loadTaskDetail,
    startTask,
    completeTask,
    clearCurrentTask
  }
})