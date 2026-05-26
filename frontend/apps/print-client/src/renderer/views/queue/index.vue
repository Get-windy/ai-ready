<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import Card from '@/components/common/Card.vue'
import TaskItem from '@/components/queue/TaskItem.vue'
import Button from '@/components/common/Button.vue'
import Empty from '@/components/common/Empty.vue'

const router = useRouter()

const tasks = ref<any[]>([])
const activeTab = ref('all')
const loading = ref(false)

const filteredTasks = computed(() => {
  if (activeTab.value === 'all') return tasks.value
  return tasks.value.filter(t => t.status === activeTab.value)
})

onMounted(async () => {
  await loadTasks()
  
  window.electronAPI.events.onTaskReceived((task: any) => {
    tasks.value.unshift(task)
  })
})

const loadTasks = async () => {
  loading.value = true
  try {
    tasks.value = await window.electronAPI.tasks.getTasks()
  } finally {
    loading.value = false
  }
}

const handleTaskClick = (task: any) => {
  router.push(`/queue/${task.id}`)
}

const handlePrintTask = async (task: any) => {
  await window.electronAPI.tasks.print(task.id)
  await loadTasks()
}

const handleCancelTask = async (task: any) => {
  await window.electronAPI.tasks.cancel(task.id)
  await loadTasks()
}

const handleRetryTask = async (task: any) => {
  await window.electronAPI.tasks.retry(task.id)
  await loadTasks()
}

const handleRefresh = () => {
  loadTasks()
}

const tabs = [
  { key: 'all', label: '全部' },
  { key: 'pending', label: '待打印' },
  { key: 'printing', label: '打印中' },
  { key: 'completed', label: '已完成' },
  { key: 'failed', label: '失败' }
]
</script>

<template>
  <div class="queue-page">
    <div class="page-header">
      <h1>打印队列</h1>
      <Button icon="refresh" @click="handleRefresh">刷新</Button>
    </div>
    
    <div class="tabs">
      <button 
        v-for="tab in tabs"
        :key="tab.key"
        :class="['tab', { active: activeTab === tab.key }]"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
        <span v-if="tab.key !== 'all'" class="count">
          {{ tasks.filter(t => t.status === tab.key).length }}
        </span>
      </button>
    </div>
    
    <div class="task-list" v-if="!loading">
      <div v-if="filteredTasks.length === 0" class="empty-container">
        <Empty description="暂无打印任务" />
      </div>
      
      <Card v-else>
        <TaskItem 
          v-for="task in filteredTasks"
          :key="task.id"
          :task="task"
          @click="handleTaskClick(task)"
          @print="handlePrintTask(task)"
          @cancel="handleCancelTask(task)"
          @retry="handleRetryTask(task)"
        />
      </Card>
    </div>
    
    <div v-else class="loading-container">
      <span>加载中...</span>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.queue-page {
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    
    h1 {
      font-size: 24px;
      font-weight: 600;
      color: #333;
    }
  }
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  
  .tab {
    padding: 8px 16px;
    background: #fff;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.2s;
    
    &:hover {
      border-color: #1988fa;
    }
    
    &.active {
      background: #1988fa;
      color: #fff;
      border-color: #1988fa;
    }
    
    .count {
      margin-left: 4px;
      padding: 2px 6px;
      background: rgba(0, 0, 0, 0.1);
      border-radius: 10px;
      font-size: 12px;
    }
  }
}

.task-list {
  .task-item {
    padding: 16px;
    border-bottom: 1px solid #ebedf0;
    cursor: pointer;
    
    &:hover {
      background: #f7f8fa;
    }
    
    &:last-child {
      border-bottom: none;
    }
  }
}

.empty-container, .loading-container {
  padding: 60px 20px;
  text-align: center;
  color: #969799;
}
</style>