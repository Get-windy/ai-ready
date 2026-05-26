<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Card, Button, Tag, Empty, showLoadingToast, closeToast, showToast } from 'vant'
import { api } from '@/api'

const router = useRouter()

interface PickTask {
  id: string
  taskNo: string
  orderNo: string
  itemCount: number
  pickedCount: number
  status: 'pending' | 'in_progress' | 'completed'
  priority: 'high' | 'normal' | 'low'
  createTime: string
  deadline: string
  warehouse: string
  location: string
}

const pickTasks = ref<PickTask[]>([])
const loading = ref(false)

const statusMap = {
  pending: { label: '待拣货', color: '#969799' },
  in_progress: { label: '拣货中', color: '#1988fa' },
  completed: { label: '已完成', color: '#07c160' }
}

const priorityMap = {
  high: { label: '紧急', color: '#f44' },
  normal: { label: '普通', color: '#1988fa' },
  low: { label: '低', color: '#969799' }
}

onMounted(async () => {
  loadPickTasks()
})

const loadPickTasks = async () => {
  loading.value = true
  showLoadingToast({ message: '加载中...', forbidClick: true })
  
  try {
    const res = await api.pick.getTasks()
    pickTasks.value = res.data || [
      {
        id: '1',
        taskNo: 'PK202401001',
        orderNo: 'SO202401001',
        itemCount: 20,
        pickedCount: 0,
        status: 'pending',
        priority: 'high',
        createTime: '2024-01-15 10:00',
        deadline: '2024-01-15 14:00',
        warehouse: '主仓库',
        location: 'A区'
      },
      {
        id: '2',
        taskNo: 'PK202401002',
        orderNo: 'SO202401002',
        itemCount: 15,
        pickedCount: 8,
        status: 'in_progress',
        priority: 'normal',
        createTime: '2024-01-15 09:00',
        deadline: '2024-01-15 13:00',
        warehouse: '主仓库',
        location: 'B区'
      }
    ]
  } finally {
    loading.value = false
    closeToast()
  }
}

const handleStartPick = async (task: PickTask) => {
  showLoadingToast({ message: '开始拣货...', forbidClick: true })
  try {
    await api.pick.start(task.id)
    showToast({ type: 'success', message: '开始拣货' })
    router.push(`/pick/${task.id}`)
  } finally {
    closeToast()
  }
}

const handleScanPick = () => {
  router.push('/pick/scan')
}

const handleViewDetail = (task: PickTask) => {
  router.push(`/pick/${task.id}`)
}

const getProgress = (task: PickTask) => {
  return Math.round((task.pickedCount / task.itemCount) * 100)
}
</script>

<template>
  <div class="pick-page">
    <NavBar title="拣货任务">
      <template #right>
        <Button size="small" type="primary" @click="handleScanPick">
          扫码
        </Button>
      </template>
    </NavBar>
    
    <div class="pick-list">
      <Empty v-if="pickTasks.length === 0 && !loading" description="暂无拣货任务" />
      
      <Card 
        v-for="task in pickTasks"
        :key="task.id"
        class="pick-card"
      >
        <template #title>
          <div class="card-header">
            <span class="task-no">{{ task.taskNo }}</span>
            <Tag :color="statusMap[task.status].color">
              {{ statusMap[task.status].label }}
            </Tag>
            <Tag :color="priorityMap[task.priority].color">
              {{ priorityMap[task.priority].label }}
            </Tag>
          </div>
        </template>
        
        <template #desc>
          <div class="card-info">
            <div class="info-row">
              <span class="label">订单号:</span>
              <span class="value">{{ task.orderNo }}</span>
            </div>
            <div class="info-row">
              <span class="label">仓库:</span>
              <span class="value">{{ task.warehouse }} {{ task.location }}</span>
            </div>
            <div class="info-row">
              <span class="label">商品数:</span>
              <span class="value">{{ task.itemCount }} 件</span>
            </div>
            <div class="info-row">
              <span class="label">已拣货:</span>
              <span class="value">{{ task.pickedCount }} 件</span>
            </div>
            <div class="info-row">
              <span class="label">截止:</span>
              <span class="value deadline">{{ task.deadline }}</span>
            </div>
          </div>
          
          <div v-if="task.status !== 'pending'" class="progress-bar">
            <div class="progress-fill" :style="{ width: getProgress(task) + '%' }"></div>
          </div>
        </template>
        
        <template #footer>
          <div class="card-actions">
            <Button 
              v-if="task.status === 'pending'"
              type="primary" 
              size="small"
              @click="handleStartPick(task)"
            >
              开始拣货
            </Button>
            <Button 
              v-if="task.status === 'in_progress'"
              type="primary" 
              size="small"
              @click="handleScanPick"
            >
              扫码拣货
            </Button>
            <Button 
              type="default" 
              size="small"
              @click="handleViewDetail(task)"
            >
              详情
            </Button>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.pick-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.pick-list {
  padding: 12px;
}

.pick-card {
  margin-bottom: 12px;
  
  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .task-no {
      font-size: 14px;
      font-weight: 600;
    }
  }
  
  .card-info {
    .info-row {
      display: flex;
      margin-top: 4px;
      
      .label {
        width: 70px;
        color: #969799;
      }
      
      .value {
        color: #333;
        
        &.deadline {
          color: #ff976a;
        }
      }
    }
  }
  
  .progress-bar {
    height: 4px;
    background: #ebedf0;
    border-radius: 2px;
    margin-top: 12px;
    overflow: hidden;
    
    .progress-fill {
      height: 100%;
      background: #07c160;
      transition: width 0.3s;
    }
  }
  
  .card-actions {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
  }
}
</style>