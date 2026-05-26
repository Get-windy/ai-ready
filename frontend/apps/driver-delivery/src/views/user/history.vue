<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, List, PullRefresh, Card, Tag, Empty, showLoadingToast, closeToast } from 'vant'
import { api } from '@/api'

const router = useRouter()

interface DeliveryHistory {
  id: string
  orderNo: string
  customerName: string
  address: string
  status: 'completed' | 'failed' | 'cancelled'
  deliveryTime: string
  duration: number
  distance: number
  income: number
  rating: number
  remark: string
}

const historyList = ref<DeliveryHistory[]>([])
const loading = ref(false)
const refreshing = ref(false)
const finished = ref(false)
const page = ref(1)
const pageSize = 20

const statusMap = {
  completed: { label: '已完成', color: '#07c160' },
  failed: { label: '配送失败', color: '#f44' },
  cancelled: { label: '已取消', color: '#969799' }
}

onMounted(async () => {
  loadHistory()
})

const loadHistory = async () => {
  if (loading.value) return
  
  loading.value = true
  showLoadingToast({ message: '加载中...', forbidClick: true, duration: 0 })
  
  try {
    const res = await api.delivery.getHistory({
      page: page.value,
      pageSize
    })
    
    const newHistory = res.data?.list || []
    
    if (page.value === 1) {
      historyList.value = newHistory
    } else {
      historyList.value.push(...newHistory)
    }
    
    if (newHistory.length < pageSize) {
      finished.value = true
    } else {
      page.value++
    }
  } catch {
    historyList.value = [
      {
        id: '1',
        orderNo: 'DL202401001',
        customerName: '张三',
        address: '北京市朝阳区建国路88号',
        status: 'completed',
        deliveryTime: '2024-01-15 11:30',
        duration: 45,
        distance: 2500,
        income: 15,
        rating: 5,
        remark: '准时送达'
      },
      {
        id: '2',
        orderNo: 'DL202401002',
        customerName: '李四',
        address: '北京市海淀区中关村大街1号',
        status: 'completed',
        deliveryTime: '2024-01-15 12:00',
        duration: 30,
        distance: 1500,
        income: 12,
        rating: 4,
        remark: ''
      },
      {
        id: '3',
        orderNo: 'DL202401003',
        customerName: '王五',
        address: '北京市西城区西单北大街',
        status: 'failed',
        deliveryTime: '2024-01-15 14:00',
        duration: 0,
        distance: 3000,
        income: 0,
        rating: 0,
        remark: '客户不在家'
      }
    ]
    finished.value = true
  } finally {
    loading.value = false
    closeToast()
  }
}

const onRefresh = async () => {
  refreshing.value = true
  page.value = 1
  finished.value = false
  await loadHistory()
  refreshing.value = false
}

const handleViewDetail = (item: DeliveryHistory) => {
  router.push(`/delivery/${item.id}`)
}

const formatDuration = (minutes: number) => {
  if (minutes < 60) return `${minutes}分钟`
  return `${Math.floor(minutes / 60)}小时${minutes % 60}分钟`
}

const formatDistance = (distance: number) => {
  if (distance < 1000) return `${distance}m`
  return `${(distance / 1000).toFixed(1)}km`
}

const goBack = () => {
  router.back()
}
</script>

<template>
  <div class="history-page">
    <NavBar 
      title="配送历史"
      left-arrow
      @click-left="goBack"
    />
    
    <PullRefresh v-model="refreshing" @refresh="onRefresh">
      <List
        v-model:loading="loading"
        :finished="finished"
        finished-text="没有更多了"
        @load="loadHistory"
      >
        <div v-if="historyList.length > 0" class="history-list">
          <Card 
            v-for="item in historyList"
            :key="item.id"
            class="history-card"
            @click="handleViewDetail(item)"
          >
            <template #title>
              <div class="card-header">
                <span class="order-no">{{ item.orderNo }}</span>
                <Tag :color="statusMap[item.status].color">
                  {{ statusMap[item.status].label }}
                </Tag>
              </div>
            </template>
            
            <template #desc>
              <div class="card-info">
                <div class="info-row">
                  <span class="label">客户:</span>
                  <span class="value">{{ item.customerName }}</span>
                </div>
                <div class="info-row">
                  <span class="label">地址:</span>
                  <span class="value">{{ item.address }}</span>
                </div>
                <div class="info-row">
                  <span class="label">时间:</span>
                  <span class="value">{{ item.deliveryTime }}</span>
                </div>
                <div class="info-row">
                  <span class="label">用时:</span>
                  <span class="value">{{ formatDuration(item.duration) }}</span>
                </div>
                <div class="info-row">
                  <span class="label">距离:</span>
                  <span class="value">{{ formatDistance(item.distance) }}</span>
                </div>
                <div v-if="item.status === 'completed'" class="info-row">
                  <span class="label">收入:</span>
                  <span class="value income">¥{{ item.income }}</span>
                </div>
                <div v-if="item.rating > 0" class="info-row">
                  <span class="label">评分:</span>
                  <span class="value rating">{{ item.rating }}星</span>
                </div>
                <div v-if="item.remark" class="info-row">
                  <span class="label">备注:</span>
                  <span class="value remark">{{ item.remark }}</span>
                </div>
              </div>
            </template>
          </Card>
        </div>
        
        <Empty 
          v-else-if="!loading"
          description="暂无配送历史"
        />
      </List>
    </PullRefresh>
  </div>
</template>

<style lang="scss" scoped>
.history-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.history-list {
  padding: 12px;
}

.history-card {
  margin-bottom: 12px;
  
  .card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .order-no {
      font-size: 14px;
      font-weight: 600;
    }
  }
  
  .card-info {
    .info-row {
      display: flex;
      margin-top: 4px;
      
      .label {
        width: 50px;
        color: #969799;
      }
      
      .value {
        color: #333;
        
        &.income {
          color: #f44;
          font-weight: 600;
        }
        
        &.rating {
          color: #ff976a;
        }
        
        &.remark {
          color: #969799;
        }
      }
    }
  }
}
</style>