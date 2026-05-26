<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NavBar, Card, Button, Tag, Steps, Step, Cell, CellGroup, Image, showLoadingToast, closeToast } from 'vant'
import { api } from '@/api'

const router = useRouter()
const route = useRoute()

const orderId = route.params.id as string
const order = ref<any>(null)

onMounted(async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const res = await api.order.getDetail(orderId)
    order.value = res.data
  } finally {
    closeToast()
  }
})

const handleNavigate = () => {
  router.push(`/map/navigation/${orderId}`)
}

const handleCallCustomer = () => {
  if (order.value?.customerPhone) {
    window.location.href = `tel:${order.value.customerPhone}`
  }
}

const handleStartDelivery = async () => {
  showLoadingToast({ message: '开始配送...', forbidClick: true })
  try {
    await api.order.startDelivery(orderId)
    router.push(`/delivery/${orderId}`)
  } finally {
    closeToast()
  }
}

const handleSign = () => {
  router.push(`/delivery/${orderId}/sign`)
}

const handleCollect = () => {
  router.push(`/delivery/${orderId}/collect`)
}

const getStepStatus = () => {
  const statusMap = {
    pending: 0,
    accepted: 1,
    delivering: 2,
    completed: 3
  }
  return statusMap[order.value?.status] || 0
}
</script>

<template>
  <div class="order-detail-page">
    <NavBar 
      title="订单详情" 
      left-arrow
      @click-left="router.back()"
    />
    
    <div v-if="order" class="detail-content">
      <div class="status-section">
        <Steps :active="getStepStatus()" active-color="#ff976a">
          <Step>待接单</Step>
          <Step>已接单</Step>
          <Step>配送中</Step>
          <Step>已完成</Step>
        </Steps>
      </div>
      
      <CellGroup inset title="订单信息">
        <Cell title="订单编号" :value="order.orderNo" />
        <Cell title="订单状态">
          <template #value>
            <Tag color="#ff976a">{{ order.status }}</Tag>
          </template>
        </Cell>
        <Cell title="创建时间" :value="order.createTime" />
        <Cell title="截止时间" :value="order.deadline" />
      </CellGroup>
      
      <CellGroup inset title="客户信息">
        <Cell title="客户名称" :value="order.customerName" />
        <Cell title="联系电话" :value="order.customerPhone">
          <template #right-icon>
            <Button size="small" type="primary" @click="handleCallCustomer">
              拨打
            </Button>
          </template>
        </Cell>
        <Cell title="配送地址" :value="order.address" :label="order.remark">
          <template #right-icon>
            <Button size="small" type="primary" @click="handleNavigate">
              导航
            </Button>
          </template>
        </Cell>
      </CellGroup>
      
      <CellGroup inset title="商品信息">
        <div class="items-list">
          <div 
            v-for="item in order.items"
            :key="item.id"
            class="item-row"
          >
            <Image 
              :src="item.image" 
              width="60"
              height="60"
              fit="cover"
            />
            <div class="item-info">
              <div class="item-name">{{ item.name }}</div>
              <div class="item-spec">{{ item.spec }}</div>
              <div class="item-quantity">x{{ item.quantity }}</div>
            </div>
            <div class="item-price">¥{{ item.price }}</div>
          </div>
        </div>
      </CellGroup>
      
      <CellGroup inset title="金额信息">
        <Cell title="订单金额" :value="`¥${order.totalAmount}`" />
        <Cell title="待收金额" :value="`¥${order.collectAmount}`" value-class="amount-value" />
      </CellGroup>
      
      <div class="action-buttons">
        <Button 
          v-if="order.status === 'accepted'"
          type="primary" 
          size="large"
          block
          @click="handleStartDelivery"
        >
          开始配送
        </Button>
        
        <template v-if="order.status === 'delivering'">
          <Button 
            type="primary" 
            size="large"
            block
            @click="handleSign"
          >
            签收
          </Button>
          <Button 
            v-if="order.collectAmount > 0"
            type="warning" 
            size="large"
            block
            @click="handleCollect"
          >
            收款
          </Button>
        </template>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.order-detail-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 20px;
}

.status-section {
  padding: 16px;
  background: #fff;
}

.items-list {
  padding: 12px;
  
  .item-row {
    display: flex;
    align-items: center;
    padding: 8px 0;
    
    .item-info {
      flex: 1;
      margin-left: 12px;
      
      .item-name {
        font-size: 14px;
        color: #333;
      }
      
      .item-spec {
        font-size: 12px;
        color: #969799;
        margin-top: 2px;
      }
      
      .item-quantity {
        font-size: 12px;
        color: #969799;
        margin-top: 2px;
      }
    }
    
    .item-price {
      font-size: 14px;
      color: #333;
    }
  }
}

.amount-value {
  color: #f44;
  font-weight: 600;
}

.action-buttons {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>