<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NavBar, Card, Cell, CellGroup, Button, Tag, Steps, Image, showLoadingToast, closeToast } from 'vant'
import { api } from '@/api'

const router = useRouter()
const route = useRoute()

const taskId = computed(() => route.params.id as string)

interface DeliveryDetail {
  id: string
  orderNo: string
  customerName: string
  customerPhone: string
  address: string
  location: { lat: number; lng: number }
  items: any[]
  totalAmount: number
  collectAmount: number
  status: string
  priority: string
  createTime: string
  deadline: string
  remark: string
  deliveryLogs: any[]
}

const deliveryDetail = ref<DeliveryDetail | null>(null)
const loading = ref(false)

onMounted(async () => {
  loadDeliveryDetail()
})

const loadDeliveryDetail = async () => {
  loading.value = true
  showLoadingToast({ message: '加载中...', forbidClick: true })
  
  try {
    const res = await api.delivery.getDetail(taskId.value)
    deliveryDetail.value = res.data || {
      id: taskId.value,
      orderNo: 'DL202401001',
      customerName: '张三',
      customerPhone: '13800138000',
      address: '北京市朝阳区建国路88号',
      location: { lat: 39.9, lng: 116.4 },
      items: [
        { id: 1, name: '商品A', quantity: 2, price: 99 },
        { id: 2, name: '商品B', quantity: 1, price: 101 }
      ],
      totalAmount: 299,
      collectAmount: 299,
      status: 'in_progress',
      priority: 'high',
      createTime: '2024-01-15 10:00',
      deadline: '2024-01-15 18:00',
      remark: '请准时送达',
      deliveryLogs: [
        { time: '2024-01-15 10:00', action: '接单', status: 'completed' },
        { time: '2024-01-15 10:30', action: '开始配送', status: 'completed' },
        { time: '2024-01-15 11:00', action: '到达目的地', status: 'process' },
        { time: '', action: '签收完成', status: 'waiting' }
      ]
    }
  } finally {
    loading.value = false
    closeToast()
  }
}

const handleNavigate = () => {
  router.push(`/map/navigation/${taskId.value}`)
}

const handleSign = () => {
  router.push(`/delivery/${taskId.value}/sign`)
}

const handleCollect = () => {
  router.push(`/delivery/${taskId.value}/collect`)
}

const handleCallCustomer = () => {
  if (deliveryDetail.value?.customerPhone) {
    window.location.href = `tel:${deliveryDetail.value.customerPhone}`
  }
}

const goBack = () => {
  router.back()
}
</script>

<template>
  <div class="delivery-detail-page">
    <NavBar 
      title="配送详情"
      left-arrow
      @click-left="goBack"
    />
    
    <div v-if="deliveryDetail" class="detail-content">
      <Card class="info-card">
        <template #title>
          <div class="card-header">
            <span class="order-no">{{ deliveryDetail.orderNo }}</span>
            <Tag type="primary">{{ deliveryDetail.status }}</Tag>
          </div>
        </template>
        
        <template #desc>
          <CellGroup inset>
            <Cell title="客户姓名" :value="deliveryDetail.customerName" />
            <Cell title="联系电话" :value="deliveryDetail.customerPhone" is-link @click="handleCallCustomer" />
            <Cell title="配送地址" :value="deliveryDetail.address" />
            <Cell title="配送距离" :value="`${deliveryDetail.location?.lat}, ${deliveryDetail.location?.lng}`" />
            <Cell title="截止时间" :value="deliveryDetail.deadline" />
            <Cell v-if="deliveryDetail.remark" title="备注" :value="deliveryDetail.remark" />
          </CellGroup>
        </template>
      </Card>
      
      <Card class="items-card">
        <template #title>
          <span class="card-title">配送商品</span>
        </template>
        
        <template #desc>
          <div class="items-list">
            <div 
              v-for="item in deliveryDetail.items"
              :key="item.id"
              class="item-row"
            >
              <div class="item-name">{{ item.name }}</div>
              <div class="item-quantity">x{{ item.quantity }}</div>
              <div class="item-price">¥{{ item.price }}</div>
            </div>
          </div>
          
          <div class="items-total">
            <span>合计:</span>
            <span class="total-amount">¥{{ deliveryDetail.totalAmount }}</span>
          </div>
        </template>
      </Card>
      
      <Card class="amount-card">
        <template #title>
          <span class="card-title">收款信息</span>
        </template>
        
        <template #desc>
          <div class="amount-info">
            <div class="amount-row">
              <span class="label">应收金额:</span>
              <span class="value collect">¥{{ deliveryDetail.collectAmount }}</span>
            </div>
          </div>
        </template>
      </Card>
      
      <Card class="log-card">
        <template #title>
          <span class="card-title">配送进度</span>
        </template>
        
        <template #desc>
          <Steps direction="vertical" :active="2">
            <Step 
              v-for="(log, index) in deliveryDetail.deliveryLogs"
              :key="index"
            >
              <div class="log-content">
                <div class="log-action">{{ log.action }}</div>
                <div class="log-time">{{ log.time }}</div>
              </div>
            </Step>
          </Steps>
        </template>
      </Card>
      
      <div class="action-buttons">
        <Button 
          type="primary" 
          block
          @click="handleNavigate"
        >
          开始导航
        </Button>
        <Button 
          type="success" 
          block
          @click="handleSign"
        >
          签收
        </Button>
        <Button 
          v-if="deliveryDetail.collectAmount > 0"
          type="warning" 
          block
          @click="handleCollect"
        >
          收款
        </Button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.delivery-detail-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.detail-content {
  padding: 12px;
}

.info-card, .items-card, .amount-card, .log-card {
  margin-bottom: 12px;
  
  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .order-no {
      font-size: 16px;
      font-weight: 600;
    }
  }
  
  .card-title {
    font-size: 14px;
    font-weight: 600;
  }
}

.items-list {
  .item-row {
    display: flex;
    padding: 8px 0;
    border-bottom: 1px solid #eee;
    
    .item-name {
      flex: 1;
    }
    
    .item-quantity {
      width: 60px;
      text-align: center;
    }
    
    .item-price {
      width: 80px;
      text-align: right;
      color: #f44;
    }
  }
}

.items-total {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0;
  font-weight: 600;
  
  .total-amount {
    color: #f44;
    margin-left: 8px;
  }
}

.amount-info {
  .amount-row {
    display: flex;
    justify-content: space-between;
    padding: 8px 0;
    
    .label {
      color: #969799;
    }
    
    .value.collect {
      color: #f44;
      font-weight: 600;
      font-size: 18px;
    }
  }
}

.log-content {
  .log-action {
    font-size: 14px;
  }
  
  .log-time {
    font-size: 12px;
    color: #969799;
    margin-top: 4px;
  }
}

.action-buttons {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>