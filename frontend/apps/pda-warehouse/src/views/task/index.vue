<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Tabs, Tab, Card, Button, Tag, Empty, showLoadingToast, closeToast } from 'vant'
import { api } from '@/api'

const router = useRouter()
const activeTab = ref('all')

interface Task {
  id: string
  taskNo: string
  type: 'receive' | 'pick' | 'check' | 'transfer'
  status: 'pending' | 'in_progress' | 'completed'
  priority: 'high' | 'medium' | 'low'
  createTime: string
  deadline: string
  itemCount: number
  completedCount: number
  warehouse: string
  location?: string
}

const tasks = ref<Task[]>([])
const pendingTasks = ref<Task[]>([])
const inProgressTasks = ref<Task[]>([])
const completedTasks = ref<Task[]>([])

const taskTypeMap = {
  receive: { label: '收货', color: '#1988fa' },
  pick: { label: '拣货', color: '#07c160' },
  check: { label: '盘点', color: '#ff976a' },
  transfer: { label: '调拨', color: '#7232dd' }
}

const statusMap = {
  pending: { label: '待处理', color: '#969799' },
  in_progress: { label: '进行中', color: '#1988fa' },
  completed: { label: '已完成', color: '#07c160' }
}

const priorityMap = {
  high: { label: '紧急', color: '#f44' },
  medium: { label: '普通', color: '#1988fa' },
  low: { label: '低', color: '#969799' }
}

onMounted(async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const res = await api.task.getList()
    tasks.value = res.data || []
    pendingTasks.value = tasks.value.filter(t => t.status === 'pending')
    inProgressTasks.value = tasks.value.filter(t => t.status === 'in_progress')
    completedTasks.value = tasks.value.filter(t => t.status === 'completed')
  } finally {
    closeToast()
  }
})

const handleTaskClick = (task: Task) => {
  const routeMap = {
    receive: `/receive/${task.id}`,
    pick: `/pick/${task.id}`,
    check: `/check/${task.id}`,
    transfer: `/task/${task.id}`
  }
  router.push(routeMap[task.type])
}

const handleStartTask = async (task: Task) => {
  await api.task.start(task.id)
  router.push(`/pick/${task.id}`)
}

const getProgress = (task: Task) => {
  return Math.round((task.completedCount / task.itemCount) * 100)
}
</script>

<template>
  <div class="task-page">
    <NavBar title="任务列表" />
    
    <Tabs v-model:active="activeTab" sticky>
      <Tab name="all" title="全部">
        <div class="task-list">
          <div v-if="tasks.length === 0" class="empty-container">
            <Empty description="暂无任务" />
          </div>
          
          <Card 
            v-for="task in tasks"
            :key="task.id"
            class="task-card"
            @click="handleTaskClick(task)"
          >
            <template #title>
              <div class="task-header">
                <span class="task-no">{{ task.taskNo }}</span>
                <Tag :color="taskTypeMap[task.type].color">
                  {{ taskTypeMap[task.type].label }}
                </Tag>
              </div>
            </template>
            
            <template #desc>
              <div class="task-info">
                <div class="info-row">
                  <span class="label">仓库:</span>
                  <span class="value">{{ task.warehouse }}</span>
                </div>
                <div class="info-row">
                  <span class="label">库位:</span>
                  <span class="value">{{ task.location || '-' }}</span>
                </div>
                <div class="info-row">
                  <span class="label">商品数:</span>
                  <span class="value">{{ task.itemCount }} 件</span>
                </div>
                <div class="info-row">
                  <span class="label">截止时间:</span>
                  <span class="value">{{ task.deadline }}</span>
                </div>
              </div>
            </template>
            
            <template #footer>
              <div class="task-footer">
                <Tag :color="statusMap[task.status].color">
                  {{ statusMap[task.status].label }}
                </Tag>
                <Tag :color="priorityMap[task.priority].color">
                  {{ priorityMap[task.priority].label }}
                </Tag>
                <div class="progress">
                  进度: {{ getProgress(task) }}%
                </div>
              </div>
            </template>
          </Card>
        </div>
      </Tab>
      
      <Tab name="pending" title="待处理">
        <div class="task-list">
          <Card 
            v-for="task in pendingTasks"
            :key="task.id"
            class="task-card"
          >
            <template #title>
              <div class="task-header">
                <span class="task-no">{{ task.taskNo }}</span>
                <Tag :color="taskTypeMap[task.type].color">
                  {{ taskTypeMap[task.type].label }}
                </Tag>
              </div>
            </template>
            
            <template #footer>
              <Button 
                type="primary" 
                size="small"
                @click="handleStartTask(task)"
              >
                开始处理
              </Button>
            </template>
          </Card>
        </div>
      </Tab>
      
      <Tab name="in_progress" title="进行中">
        <div class="task-list">
          <Card 
            v-for="task in inProgressTasks"
            :key="task.id"
            class="task-card"
            @click="handleTaskClick(task)"
          >
            <template #title>
              <div class="task-header">
                <span class="task-no">{{ task.taskNo }}</span>
                <Tag :color="taskTypeMap[task.type].color">
                  {{ taskTypeMap[task.type].label }}
                </Tag>
              </div>
            </template>
            
            <template #footer>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: getProgress(task) + '%' }"></div>
              </div>
              <span class="progress-text">{{ task.completedCount }}/{{ task.itemCount }}</span>
            </template>
          </Card>
        </div>
      </Tab>
      
      <Tab name="completed" title="已完成">
        <div class="task-list">
          <Card 
            v-for="task in completedTasks"
            :key="task.id"
            class="task-card completed"
          >
            <template #title>
              <div class="task-header">
                <span class="task-no">{{ task.taskNo }}</span>
                <Tag color="#07c160">已完成</Tag>
              </div>
            </template>
          </Card>
        </div>
      </Tab>
    </Tabs>
  </div>
</template>

<style lang="scss" scoped>
.task-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.empty-container {
  padding: 60px 20px;
  text-align: center;
}

.task-list {
  padding: 12px;
}

.task-card {
  margin-bottom: 12px;
  
  .task-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .task-no {
      font-size: 16px;
      font-weight: 600;
    }
  }
  
  .task-info {
    .info-row {
      display: flex;
      margin-top: 8px;
      
      .label {
        width: 80px;
        color: #969799;
      }
      
      .value {
        color: #333;
      }
    }
  }
  
  .task-footer {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .progress {
      margin-left: auto;
      color: #969799;
    }
  }
  
  .progress-bar {
    height: 4px;
    background: #ebedf0;
    border-radius: 2px;
    overflow: hidden;
    
    .progress-fill {
      height: 100%;
      background: #07c160;
      transition: width 0.3s;
    }
  }
  
  .progress-text {
    font-size: 12px;
    color: #969799;
    margin-top: 4px;
  }
  
  &.completed {
    opacity: 0.7;
  }
}
</style>