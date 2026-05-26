<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Tabs, Tab, Card, Button, Tag, Empty, showLoadingToast, closeToast } from 'vant'
import { api } from '@/api'

const router = useRouter()
const activeTab = ref('pending')

interface ReceiveTask {
  id: string
  taskNo: string
  purchaseOrderNo: string
  supplierName: string
  status: 'pending' | 'in_progress' | 'completed'
  itemCount: number
  receivedCount: number
  createTime: string
  warehouse: string
}

const pendingTasks = ref<ReceiveTask[]>([])
const inProgressTasks = ref<ReceiveTask[]>([])
const completedTasks = ref<ReceiveTask[]>([])

const statusMap = {
  pending: { label: '待收货', color: '#969799' },
  in_progress: { label: '收货中', color: '#1988fa' },
  completed: { label: '已完成', color: '#07c160' }
}

onMounted(async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const res = await api.receive.getList()
    const tasks = res.data || []
    
    pendingTasks.value = tasks.filter(t => t.status === 'pending')
    inProgressTasks.value = tasks.filter(t => t.status === 'in_progress')
    completedTasks.value = tasks.filter(t => t.status === 'completed')
  } finally {
    closeToast()
  }
})

const handleTaskClick = (task: ReceiveTask) => {
  router.push(`/receive/${task.id}`)
}

const handleStartReceive = async (task: ReceiveTask) => {
  showLoadingToast({ message: '开始收货...', forbidClick: true })
  try {
    await api.task.start(task.id)
    router.push(`/receive/${task.id}`)
  } finally {
    closeToast()
  }
}

const handleScanReceive = () => {
  router.push('/receive/scan')
}

const getProgress = (task: ReceiveTask) => {
  return Math.round((task.receivedCount / task.itemCount) * 100)
}
</script>

<template>
  <div class="receive-page">
    <NavBar title="收货任务" />
    
    <Tabs v-model:active="activeTab" sticky>
      <Tab name="pending" title="待收货">
        <div class="task-list">
          <div v-if="pendingTasks.length === 0" class="empty-container">
            <Empty description="暂无待收货任务" />
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
                  <span class="label">采购单:</span>
                  <span class="value">{{ task.purchaseOrderNo }}</span>
                </div>
                <div class="info-row">
                  <span class="label">供应商:</span>
                  <span class="value">{{ task.supplierName }}</span>
                </div>
                <div class="info-row">
                  <span class="label">商品数:</span>
                  <span class="value">{{ task.itemCount }} 件</span>
                </div>
                <div class="info-row">
                  <span class="label">仓库:</span>
                  <span class="value">{{ task.warehouse }}</span>
                </div>
              </div>
            </template>
            
            <template #footer>
              <div class="task-actions">
                <Button 
                  type="primary" 
                  size="small"
                  @click="handleStartReceive(task)"
                >
                  开始收货
                </Button>
                <Button 
                  type="default" 
                  size="small"
                  icon="scan"
                  @click="handleScanReceive"
                >
                  扫码收货
                </Button>
              </div>
            </template>
          </Card>
        </div>
      </Tab>
      
      <Tab name="in_progress" title="收货中">
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
                <Tag color="#1988fa">收货中</Tag>
              </div>
            </template>
            
            <template #footer>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: getProgress(task) + '%' }"></div>
              </div>
              <span class="progress-text">{{ task.receivedCount }}/{{ task.itemCount }}</span>
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
.receive-page {
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
      }
    }
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
      background: #1988fa;
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