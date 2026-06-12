import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '@/api'
import type { PdaTaskItem } from '@/types'

export const useTaskStore = defineStore('warehouse-task', () => {
  const tasks = ref<PdaTaskItem[]>([])
  const currentTask = ref<PdaTaskItem | null>(null)
  const loading = ref(false)
  const stats = ref({
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

  const loadTasks = async () => {
    loading.value = true
    try {
      const res = await api.task.getList()
      tasks.value = res || []
      calculateStats()
    } catch {
      tasks.value = []
    } finally {
      loading.value = false
    }
  }

  const loadTaskDetail = async (taskId: number, taskType: string) => {
    loading.value = true
    try {
      const res = await api.task.getDetail(taskId, taskType)
      currentTask.value = res?.task || null
    } catch {
      currentTask.value = null
    } finally {
      loading.value = false
    }
  }

  const startTask = async (taskId: number, taskType: string) => {
    try {
      await api.task.start(taskId, taskType)
      const task = tasks.value.find(t => t.taskId === taskId)
      if (task) {
        task.status = 1
      }
      calculateStats()
    } catch {
      throw new Error('开始任务失败')
    }
  }

  const completeTask = async (taskId: number, taskType: string) => {
    try {
      await api.task.complete(taskId, taskType)
      const task = tasks.value.find(t => t.taskId === taskId)
      if (task) {
        task.status = 2
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
      inProgress: tasks.value.filter(t => t.status === 1).length,
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
    loadTasks,
    loadTaskDetail,
    startTask,
    completeTask,
    clearCurrentTask,
  }
})
