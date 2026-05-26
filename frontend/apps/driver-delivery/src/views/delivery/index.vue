<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Card, Button, Tag, Empty, showLoadingToast, closeToast, showToast, Dialog } from 'vant'
import { api } from '@/api'

const router = useRouter()

interface DeliveryTask {
  id: string
  orderNo: string
  customerName: string
  customerPhone: string
  address: string
  location: { lat: number; lng: number }
  distance: number
  items: any[]
  totalAmount: number
  collectAmount: number
  status: 'pending' | 'in_progress' | 'completed'
  priority: 'high' | 'normal' | 'low'
  createTime: string
  deadline: string
  remark: string
}

const deliveryTasks = ref<DeliveryTask[]>([])
const loading = ref(false)

const statusMap = {
  pending: { label: '待配送', color: '#969799' },
  in_progress: { label: '配送中', color: '#ff976a' },
  completed: { label: '已完成', color: '#07c160' }
}

const priorityMap = {
  high: { label: '紧急', color: '#f44' },
  normal: { label: '普通', color: '#1988fa' },
  low: { label: '低', color: '#969799' }
}

onMounted(async () => {
  loadDeliveryTasks()
})

const loadDeliveryTasks = async () => {
  loading.value = true
  showLoadingToast({ message: '加载中...', forbidClick: true })
  
  try {
    const res = await api.delivery.getTasks()
    deliveryTasks.value = res.data || [
      {
        id: '1',
        orderNo: 'DL202401001',
        customerName: '张三',
        customerPhone: '13800138000',
        address: '北京市朝阳区建国路88号',
        location: { lat: 39.9, lng: 116.4 },
        distance: 2500,
        items: [{ name: '商品A', quantity: 2 }],
        totalAmount: 299,
        collectAmount: 299,
        status: 'pending',
        priority: 'high',
        createTime: '2024-01-15 10:00',
        deadline: '2024-01-15 18:00',
        remark: '请准时送达'
      },
      {
        id: '2',
        orderNo: 'DL202401002',
        customerName: '李四',
        customerPhone: '13900139000',
        address: '北京市海淀区中关村大街1号',
        location: { lat: 39.98, lng: 116.31 },
        distance: 5000,
        items: [{ name: '商品B', quantity: 1 }],
        totalAmount: 199,
        collectAmount: 199,
        status: 'in_progress',
        priority: 'normal',
        createTime: '2024-01-15 09:00',
        deadline: '2024-01-15 17:00',
        remark: ''
      }
    ]
  } finally {
    loading.value = false
    closeToast()
  }
}

const handleStartDelivery = async (task: DeliveryTask) => {
  Dialog.confirm({
    title: '开始配送',
    message: `确定开始配送 ${task.orderNo} 吗？`
  }).then(async () => {
    showLoadingToast({ message: '开始配送...', forbidClick: true })
    try {
      await api.delivery.start(task.id)
      showToast({ type: 'success', message: '开始配送' })
      router.push(`/delivery/${task.id}`)
    } finally {
      closeToast()
    }
  }).catch(() => {})
}

const handleViewDetail = (task: DeliveryTask) => {
  router.push(`/delivery/${task.id}`)
}

const handleNavigate = (task: DeliveryTask) => {
  router.push(`/map/navigation/${task.id}`)
}

const handleCallCustomer = (task: DeliveryTask) => {
  Dialog.confirm({
    title: '联系客户',
    message: `拨打 ${task.customerPhone}？`
  }).then(() => {
    window.location.href = `tel:${task.customerPhone}`
  }).catch(() => {})
}

const formatDistance = (distance: number) => {
  if (distance < 1000) return `${distance}m`
  return `${(distance / 1000).toFixed(1)}km`
}
</script>

<template>
  <div class="delivery-page">
    <NavBar title="配送任务" />
    
    <div class="delivery-list">
      <Empty v-if="deliveryTasks.length === 0 && !loading" description="暂无配送任务" />
      
      <Card 
        v-for="task in deliveryTasks"
        :key="task.id"
        class="delivery-card"
      >
        <template #title>
          <div class="task-header">
            <span class="task-no">{{ task.orderNo }}</span>
            <Tag :color="statusMap[task.status].color">
              {{ statusMap[task.status].label }}
            </Tag>
            <Tag :color="priorityMap[task.priority].color">
              {{ priorityMap[task.priority].label }}
            </Tag>
          </div>
        </template>
        
        <template #desc>
          <div class="task-info">
            <div class="info-row">
              <span class="label">客户:</span>
              <span class="value">{{ task.customerName }} {{ task.customerPhone }}</span>
            </div>
            <div class="info-row">
              <span class="label">地址:</span>
              <span class="value address">{{ task.address }}</span>
            </div>
            <div class="info-row">
              <span class="label">距离:</span>
              <span class="value">{{ formatDistance(task.distance) }}</span>
            </div>
            <div class="info-row">
              <span class="label">金额:</span>
              <span class="value amount">¥{{ task.collectAmount }}</span>
            </div>
            <div class="info-row">
              <span class="label">截止:</span>
              <span class="value deadline">{{ task.deadline }}</span>
            </div>
            <div v-if="task.remark" class="info-row">
              <span class="label">备注:</span>
              <span class="value remark">{{ task.remark }}</span>
            </div>
          </div>
        </template>
        
        <template #footer>
          <div class="task-actions">
            <Button 
              v-if="task.status === 'pending'"
              type="primary" 
              size="small"
              @click="handleStartDelivery(task)"
            >
              开始配送
            </Button>
            <Button 
              v-if="task.status === 'in_progress'"
              type="primary" 
              size="small"
              @click="router.push(`/delivery/${task.id}/sign`)"
            >
              签收
            </Button>
            <Button 
              type="default" 
              size="small"
              @click="handleNavigate(task)"
            >
              导航
            </Button>
            <Button 
              type="default" 
              size="small"
              @click="handleCallCustomer(task)"
            >
              联系
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
.delivery-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.delivery-list {
  padding: 12px;
}

.delivery-card {
  margin-bottom: 12px;
  
  .task-header {
    display: flex;
    align-items: center;
    gap: 8px;
    
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
        width: 60px;
        color: #969799;
      }
      
      .value {
        color: #333;
        
        &.address {
          flex: 1;
          word-break: break-all;
        }
        
        &.amount {
          color: #f44;
          font-weight: 600;
        }
        
        &.deadline {
          color: #ff976a;
        }
        
        &.remark {
          color: #969799;
        }
      }
    }
  }
  
  .task-actions {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
  }
}
</style>