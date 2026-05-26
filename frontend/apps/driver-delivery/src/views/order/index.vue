<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Tabs, Tab, Card, Button, Tag, Empty, showLoadingToast, closeToast, showToast, Dialog } from 'vant'
import { api } from '@/api'

const router = useRouter()
const activeTab = ref('pending')

interface DeliveryOrder {
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
  status: 'pending' | 'accepted' | 'delivering' | 'completed'
  priority: 'high' | 'normal' | 'low'
  createTime: string
  deadline: string
  remark: string
}

const pendingOrders = ref<DeliveryOrder[]>([])
const acceptedOrders = ref<DeliveryOrder[]>([])
const deliveringOrders = ref<DeliveryOrder[]>([])
const completedOrders = ref<DeliveryOrder[]>([])

const statusMap = {
  pending: { label: '待接单', color: '#969799' },
  accepted: { label: '已接单', color: '#1988fa' },
  delivering: { label: '配送中', color: '#ff976a' },
  completed: { label: '已完成', color: '#07c160' }
}

const priorityMap = {
  high: { label: '紧急', color: '#f44' },
  normal: { label: '普通', color: '#1988fa' },
  low: { label: '低', color: '#969799' }
}

onMounted(async () => {
  loadOrders()
})

const loadOrders = async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const res = await api.order.getList()
    const orders = res.data || []
    
    pendingOrders.value = orders.filter(o => o.status === 'pending')
    acceptedOrders.value = orders.filter(o => o.status === 'accepted')
    deliveringOrders.value = orders.filter(o => o.status === 'delivering')
    completedOrders.value = orders.filter(o => o.status === 'completed')
  } finally {
    closeToast()
  }
}

const handleAccept = async (order: DeliveryOrder) => {
  Dialog.confirm({
    title: '接单确认',
    message: `确定接单 ${order.orderNo} 吗？`
  }).then(async () => {
    showLoadingToast({ message: '接单中...', forbidClick: true })
    try {
      await api.order.accept(order.id)
      showToast({ type: 'success', message: '接单成功' })
      loadOrders()
    } finally {
      closeToast()
    }
  }).catch(() => {})
}

const handleReject = async (order: DeliveryOrder) => {
  Dialog.confirm({
    title: '拒单确认',
    message: `确定拒单 ${order.orderNo} 吗？`
  }).then(async () => {
    showLoadingToast({ message: '拒单中...', forbidClick: true })
    try {
      await api.order.reject(order.id, '司机拒单')
      showToast({ type: 'success', message: '已拒单' })
      loadOrders()
    } finally {
      closeToast()
    }
  }).catch(() => {})
}

const handleStartDelivery = async (order: DeliveryOrder) => {
  showLoadingToast({ message: '开始配送...', forbidClick: true })
  try {
    await api.order.startDelivery(order.id)
    showToast({ type: 'success', message: '开始配送' })
    router.push(`/delivery/${order.id}`)
  } finally {
    closeToast()
  }
}

const handleViewDetail = (order: DeliveryOrder) => {
  router.push(`/order/${order.id}`)
}

const handleNavigate = (order: DeliveryOrder) => {
  router.push(`/map/navigation/${order.id}`)
}

const formatDistance = (distance: number) => {
  if (distance < 1000) return `${distance}m`
  return `${(distance / 1000).toFixed(1)}km`
}
</script>

<template>
  <div class="order-page">
    <NavBar title="待配送订单" />
    
    <Tabs v-model:active="activeTab" sticky>
      <Tab name="pending" title="待接单">
        <div class="order-list">
          <div v-if="pendingOrders.length === 0" class="empty-container">
            <Empty description="暂无待接订单" />
          </div>
          
          <Card 
            v-for="order in pendingOrders"
            :key="order.id"
            class="order-card"
          >
            <template #title>
              <div class="order-header">
                <span class="order-no">{{ order.orderNo }}</span>
                <Tag :color="priorityMap[order.priority].color">
                  {{ priorityMap[order.priority].label }}
                </Tag>
              </div>
            </template>
            
            <template #desc>
              <div class="order-info">
                <div class="info-row">
                  <span class="label">客户:</span>
                  <span class="value">{{ order.customerName }}</span>
                </div>
                <div class="info-row">
                  <span class="label">地址:</span>
                  <span class="value address">{{ order.address }}</span>
                </div>
                <div class="info-row">
                  <span class="label">距离:</span>
                  <span class="value">{{ formatDistance(order.distance) }}</span>
                </div>
                <div class="info-row">
                  <span class="label">金额:</span>
                  <span class="value amount">¥{{ order.collectAmount }}</span>
                </div>
                <div class="info-row">
                  <span class="label">截止:</span>
                  <span class="value">{{ order.deadline }}</span>
                </div>
              </div>
            </template>
            
            <template #footer>
              <div class="order-actions">
                <Button 
                  type="primary" 
                  size="small"
                  @click="handleAccept(order)"
                >
                  接单
                </Button>
                <Button 
                  type="default" 
                  size="small"
                  @click="handleReject(order)"
                >
                  拒单
                </Button>
                <Button 
                  type="default" 
                  size="small"
                  @click="handleViewDetail(order)"
                >
                  详情
                </Button>
              </div>
            </template>
          </Card>
        </div>
      </Tab>
      
      <Tab name="accepted" title="已接单">
        <div class="order-list">
          <Card 
            v-for="order in acceptedOrders"
            :key="order.id"
            class="order-card"
          >
            <template #title>
              <div class="order-header">
                <span class="order-no">{{ order.orderNo }}</span>
                <Tag color="#1988fa">已接单</Tag>
              </div>
            </template>
            
            <template #footer>
              <div class="order-actions">
                <Button 
                  type="primary" 
                  size="small"
                  @click="handleStartDelivery(order)"
                >
                  开始配送
                </Button>
                <Button 
                  type="default" 
                  size="small"
                  @click="handleNavigate(order)"
                >
                  导航
                </Button>
              </div>
            </template>
          </Card>
        </div>
      </Tab>
      
      <Tab name="delivering" title="配送中">
        <div class="order-list">
          <Card 
            v-for="order in deliveringOrders"
            :key="order.id"
            class="order-card delivering"
            @click="router.push(`/delivery/${order.id}`)"
          >
            <template #title>
              <div class="order-header">
                <span class="order-no">{{ order.orderNo }}</span>
                <Tag color="#ff976a">配送中</Tag>
              </div>
            </template>
            
            <template #footer>
              <Button 
                type="primary" 
                size="small"
                block
                @click.stop="router.push(`/delivery/${order.id}/sign`)"
              >
                签收
              </Button>
            </template>
          </Card>
        </div>
      </Tab>
      
      <Tab name="completed" title="已完成">
        <div class="order-list">
          <Card 
            v-for="order in completedOrders"
            :key="order.id"
            class="order-card completed"
          >
            <template #title>
              <div class="order-header">
                <span class="order-no">{{ order.orderNo }}</span>
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
.order-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.empty-container {
  padding: 60px 20px;
  text-align: center;
}

.order-list {
  padding: 12px;
}

.order-card {
  margin-bottom: 12px;
  
  .order-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .order-no {
      font-size: 16px;
      font-weight: 600;
    }
  }
  
  .order-info {
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
      }
    }
  }
  
  .order-actions {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
  }
  
  &.delivering {
    cursor: pointer;
  }
  
  &.completed {
    opacity: 0.7;
  }
}
</style>