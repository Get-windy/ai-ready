<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NavBar, Tabs, Tab, Card, Button, Tag, Steps, Step, Cell, CellGroup, Empty, showLoadingToast, closeToast } from 'vant'
import { api } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeTab = ref('all')
const orders = ref<any[]>([])

const statusMap = {
  pending: { label: '待付款', color: '#ff976a', step: 0 },
  paid: { label: '待发货', color: '#1988fa', step: 1 },
  shipped: { label: '待收货', color: '#07c160', step: 2 },
  completed: { label: '已完成', color: '#969799', step: 3 },
  cancelled: { label: '已取消', color: '#969799', step: -1 }
}

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    router.push('/login')
    return
  }
  
  loadOrders()
})

const loadOrders = async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const res = await api.order.getList({ status: activeTab.value })
    orders.value = res.data || []
  } finally {
    closeToast()
  }
}

const handleTabChange = () => {
  loadOrders()
}

const handleViewDetail = (order: any) => {
  router.push(`/order/${order.id}`)
}

const handlePay = (order: any) => {
  router.push(`/order/${order.id}/pay`)
}

const handleCancel = async (order: any) => {
  showLoadingToast({ message: '取消中...', forbidClick: true })
  try {
    await api.order.cancel(order.id)
    closeToast()
    loadOrders()
  } finally {
    closeToast()
  }
}

const handleConfirmReceive = async (order: any) => {
  showLoadingToast({ message: '确认中...', forbidClick: true })
  try {
    await api.order.confirm(order.id)
    closeToast()
    loadOrders()
  } finally {
    closeToast()
  }
}

const getStepActive = (status: string) => {
  return statusMap[status]?.step || 0
}
</script>

<template>
  <div class="order-list-page">
    <NavBar 
      title="我的订单" 
      left-arrow
      @click-left="router.back()"
    />
    
    <Tabs v-model:active="activeTab" sticky @change="handleTabChange">
      <Tab name="all" title="全部">
        <div class="order-list">
          <div v-if="orders.length === 0" class="empty-container">
            <Empty description="暂无订单" />
          </div>
          
          <Card 
            v-for="order in orders"
            :key="order.id"
            class="order-card"
            @click="handleViewDetail(order)"
          >
            <template #title>
              <div class="order-header">
                <span class="order-no">{{ order.orderNo }}</span>
                <Tag :color="statusMap[order.status]?.color">
                  {{ statusMap[order.status]?.label }}
                </Tag>
              </div>
            </template>
            
            <template #desc>
              <div class="order-items">
                <div 
                  v-for="item in order.items.slice(0, 2)"
                  :key="item.id"
                  class="item-preview"
                >
                  <img :src="item.image" class="item-image" />
                </div>
                <div v-if="order.items.length > 2" class="more-items">
                  +{{ order.items.length - 2 }}
                </div>
              </div>
              <div class="order-total">
                共 {{ order.items.length }} 件商品，合计 ¥{{ order.totalAmount.toFixed(2) }}
              </div>
            </template>
            
            <template #footer>
              <div class="order-actions">
                <Button 
                  v-if="order.status === 'pending'"
                  type="default" 
                  size="small"
                  @click.stop="handleCancel(order)"
                >
                  取消订单
                </Button>
                <Button 
                  v-if="order.status === 'pending'"
                  type="primary" 
                  size="small"
                  @click.stop="handlePay(order)"
                >
                  立即付款
                </Button>
                <Button 
                  v-if="order.status === 'shipped'"
                  type="primary" 
                  size="small"
                  @click.stop="handleConfirmReceive(order)"
                >
                  确认收货
                </Button>
              </div>
            </template>
          </Card>
        </div>
      </Tab>
      
      <Tab name="pending" title="待付款">
        <div class="order-list">
          <Card 
            v-for="order in orders.filter(o => o.status === 'pending')"
            :key="order.id"
            class="order-card"
          >
            <template #footer>
              <Button type="primary" size="small" @click.stop="handlePay(order)">
                立即付款
              </Button>
            </template>
          </Card>
        </div>
      </Tab>
      
      <Tab name="shipped" title="待收货">
        <div class="order-list">
          <Card 
            v-for="order in orders.filter(o => o.status === 'shipped')"
            :key="order.id"
            class="order-card"
          >
            <template #footer>
              <Button type="primary" size="small" @click.stop="handleConfirmReceive(order)">
                确认收货
              </Button>
            </template>
          </Card>
        </div>
      </Tab>
      
      <Tab name="completed" title="已完成">
        <div class="order-list">
          <Card 
            v-for="order in orders.filter(o => o.status === 'completed')"
            :key="order.id"
            class="order-card completed"
          >
          </Card>
        </div>
      </Tab>
    </Tabs>
  </div>
</template>

<style lang="scss" scoped>
.order-list-page {
  min-height: 100vh;
  background: #f7f8fa;
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
      font-size: 14px;
      color: #333;
    }
  }
  
  .order-items {
    display: flex;
    align-items: center;
    margin-top: 12px;
    
    .item-preview {
      width: 60px;
      height: 60px;
      margin-right: 8px;
      
      .item-image {
        width: 100%;
        height: 100%;
        border-radius: 4px;
        object-fit: cover;
      }
    }
    
    .more-items {
      width: 60px;
      height: 60px;
      background: #f7f8fa;
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      color: #969799;
    }
  }
  
  .order-total {
    font-size: 14px;
    color: #333;
    margin-top: 12px;
  }
  
  .order-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
  }
  
  &.completed {
    opacity: 0.7;
  }
}
</style>