<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Tabs, Tab, Card, Button, Tag, Empty, showLoadingToast, closeToast, Dialog } from 'vant'
import { api } from '@/api'

const router = useRouter()
const activeTab = ref('pending')

interface CheckTask {
  id: string
  taskNo: string
  warehouse: string
  location: string
  status: 'pending' | 'in_progress' | 'completed'
  itemCount: number
  checkedCount: number
  differenceCount: number
  createTime: string
  deadline: string
}

const pendingTasks = ref<CheckTask[]>([])
const inProgressTasks = ref<CheckTask[]>([])
const completedTasks = ref<CheckTask[]>([])

const statusMap = {
  pending: { label: '待盘点', color: '#969799' },
  in_progress: { label: '盘点中', color: '#ff976a' },
  completed: { label: '已完成', color: '#07c160' }
}

onMounted(async () => {
  loadTasks()
})

const loadTasks = async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const res = await api.check.getList()
    const tasks = res.data || []
    
    pendingTasks.value = tasks.filter(t => t.status === 'pending')
    inProgressTasks.value = tasks.filter(t => t.status === 'in_progress')
    completedTasks.value = tasks.filter(t => t.status === 'completed')
  } finally {
    closeToast()
  }
}

const handleTaskClick = (task: CheckTask) => {
  router.push(`/check/${task.id}`)
}

const handleStartCheck = async (task: CheckTask) => {
  Dialog.confirm({
    title: '开始盘点',
    message: `确定开始盘点 ${task.taskNo} 吗？`
  }).then(async () => {
    showLoadingToast({ message: '开始盘点...', forbidClick: true })
    try {
      await api.task.start(task.id)
      router.push(`/check/${task.id}`)
    } finally {
      closeToast()
    }
  }).catch(() => {})
}

const handleScanCheck = () => {
  router.push('/check/scan')
}

const getProgress = (task: CheckTask) => {
  return Math.round((task.checkedCount / task.itemCount) * 100)
}
</script>

<template>
  <div class="check-page">
    <NavBar title="盘点任务" />
    
    <Tabs v-model:active="activeTab" sticky>
      <Tab name="pending" title="待盘点">
        <div class="task-list">
          <div v-if="pendingTasks.length === 0" class="empty-container">
            <Empty description="暂无待盘点任务" />
          </div>
          
          <Card 
            v-for="task in pendingTasks"
            :key="task.id"
            class="task-card"
          >
            <template #title>
              <div class="task-header">
                <span class="task-no">{{ task.taskNo }}</span>
                <Tag :color="statusMap[task.status].color">
                  {{ statusMap[task.status].label }}
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
                  <span class="value highlight">{{ task.location }}</span>
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
              <div class="task-actions">
                <Button 
                  type="primary" 
                  size="small"
                  @click="handleStartCheck(task)"
                >
                  开始盘点
                </Button>
                <Button 
                  type="default" 
                  size="small"
                  icon="scan"
                  @click="handleScanCheck"
                >
                  扫码盘点
                </Button>
              </div>
            </template>
          </Card>
        </div>
      </Tab>
      
      <Tab name="in_progress" title="盘点中">
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
                <Tag color="#ff976a">盘点中</Tag>
              </div>
            </template>
            
            <template #desc>
              <div class="difference-info" v-if="task.differenceCount > 0">
                <Tag color="#f44">差异 {{ task.differenceCount }} 件</Tag>
              </div>
            </template>
            
            <template #footer>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: getProgress(task) + '%' }"></div>
              </div>
              <span class="progress-text">{{ task.checkedCount }}/{{ task.itemCount }}</span>
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
            
            <template #desc>
              <div class="difference-info" v-if="task.differenceCount > 0">
                <Tag color="#f44">差异 {{ task.differenceCount }} 件</Tag>
              </div>
            </template>
          </Card>
        </div>
      </Tab>
    </Tabs>
  </div>
</template>

<style lang="scss" scoped>
.check-page {
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
        width: 70px;
        color: #969799;
      }
      
      .value {
        color: #333;
        
        &.highlight {
          color: #1988fa;
          font-weight: 600;
        }
      }
    }
  }
  
  .difference-info {
    margin-top: 8px;
  }
  
  .task-actions {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
  }
  
  .progress-bar {
    height: 4px;
    background: #ebedf0;
    border-radius: 2px;
    overflow: hidden;
    
    .progress-fill {
      height: 100%;
      background: #ff976a;
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