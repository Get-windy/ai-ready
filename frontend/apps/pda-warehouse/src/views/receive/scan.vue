<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Card, Button, Tag, Empty, showLoadingToast, closeToast, showToast } from 'vant'
import { api } from '@/api'

const router = useRouter()

interface ReceiveTask {
  id: string
  taskNo: string
  supplierName: string
  purchaseOrderNo: string
  itemCount: number
  receivedCount: number
  status: 'pending' | 'in_progress' | 'completed'
  priority: 'high' | 'normal' | 'low'
  createTime: string
  deadline: string
  warehouse: string
  location: string
}

const receiveTasks = ref<ReceiveTask[]>([])
const loading = ref(false)

const statusMap = {
  pending: { label: '待收货', color: '#969799' },
  in_progress: { label: '收货中', color: '#1988fa' },
  completed: { label: '已完成', color: '#07c160' }
}

const priorityMap = {
  high: { label: '紧急', color: '#f44' },
  normal: { label: '普通', color: '#1988fa' },
  low: { label: '低', color: '#969799' }
}

onMounted(async () => {
  loadReceiveTasks()
})

const loadReceiveTasks = async () => {
  loading.value = true
  showLoadingToast({ message: '加载中...', forbidClick: true })
  
  try {
    const res = await api.receive.getTasks()
    receiveTasks.value = res.data || [
      {
        id: '1',
        taskNo: 'RC202401001',
        supplierName: '供应商A',
        purchaseOrderNo: 'PO202401001',
        itemCount: 50,
        receivedCount: 0,
        status: 'pending',
        priority: 'high',
        createTime: '2024-01-15 10:00',
        deadline: '2024-01-15 18:00',
        warehouse: '主仓库',
        location: 'A区'
      },
      {
        id: '2',
        taskNo: 'RC202401002',
        supplierName: '供应商B',
        purchaseOrderNo: 'PO202401002',
        itemCount: 30,
        receivedCount: 15,
        status: 'in_progress',
        priority: 'normal',
        createTime: '2024-01-15 09:00',
        deadline: '2024-01-15 17:00',
        warehouse: '主仓库',
        location: 'B区'
      }
    ]
  } finally {
    loading.value = false
    closeToast()
  }
}

const handleStartReceive = async (task: ReceiveTask) => {
  showLoadingToast({ message: '开始收货...', forbidClick: true })
  try {
    await api.receive.start(task.id)
    showToast({ type: 'success', message: '开始收货' })
    router.push(`/receive/${task.id}`)
  } finally {
    closeToast()
  }
}

const handleScanReceive = () => {
  router.push('/receive/scan')
}

const handleViewDetail = (task: ReceiveTask) => {
  router.push(`/receive/${task.id}`)
}

const getProgress = (task: ReceiveTask) => {
  return Math.round((task.receivedCount / task.itemCount) * 100)
}
</script>

<template>
  <div class="receive-page">
    <NavBar title="收货任务">
      <template #right>
        <Button size="small" type="primary" @click="handleScanReceive">
          扫码
        </Button>
      </template>
    </NavBar>
    
    <div class="receive-list">
      <Empty v-if="receiveTasks.length === 0 && !loading" description="暂无收货任务" />
      
      <Card 
        v-for="task in receiveTasks"
        :key="task.id"
        class="receive-card"
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
              <span class="label">供应商:</span>
              <span class="value">{{ task.supplierName }}</span>
            </div>
            <div class="info-row">
              <span class="label">采购单:</span>
              <span class="value">{{ task.purchaseOrderNo }}</span>
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
              <span class="label">已收货:</span>
              <span class="value">{{ task.receivedCount }} 件</span>
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
              @click="handleStartReceive(task)"
            >
              开始收货
            </Button>
            <Button 
              v-if="task.status === 'in_progress'"
              type="primary" 
              size="small"
              @click="handleScanReceive"
            >
              扫码收货
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
.receive-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.receive-list {
  padding: 12px;
}

.receive-card {
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