<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/api'

const router = useRouter()

interface DeliveryOrder {
  id: string
  orderNo: string
  customerName: string
  customerPhone: string
  address: string
  latitude: number
  longitude: number
  items: DeliveryItem[]
  status: 'pending' | 'in_progress' | 'completed' | 'failed'
  priority: number
  estimatedTime: number
  distance: number
  remark: string
}

interface DeliveryItem {
  productName: string
  quantity: number
  unit: string
}

interface RoutePoint {
  order: DeliveryOrder
  sequence: number
  estimatedArrival: string
  distanceFromPrev: number
}

const orders = ref<DeliveryOrder[]>([])
const optimizedRoute = ref<RoutePoint[]>([])
const loading = ref(false)
const optimizing = ref(false)
const currentLocation = ref({ latitude: 0, longitude: 0 })
const totalDistance = ref(0)
const totalTime = ref(0)

onMounted(async () => {
  loadOrders()
  getCurrentLocation()
})

const loadOrders = async () => {
  loading.value = true
  try {
    const res: any = await api.order.getList({ status: 'pending', pageSize: 50 })
    orders.value = res?.records || res?.data || (Array.isArray(res) ? res : [])
  } catch {
    orders.value = []
  } finally {
    loading.value = false
  }
}

const handleNavigate = (order: DeliveryOrder) => {
  router.push({ path: '/navigation', query: { lat: order.latitude, lng: order.longitude, orderId: order.id } })
}

const handleViewDetail = (order: DeliveryOrder) => {
  router.push(`/delivery/${order.id}`)
}

const getCurrentLocation = async () => {
  try {
    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          currentLocation.value = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude
          }
        },
        () => {
          currentLocation.value = { latitude: 39.9042, longitude: 116.4074 }
        }
      )
    }
  } catch {
    currentLocation.value = { latitude: 39.9042, longitude: 116.4074 }
  }
}

const optimizeRoute = async () => {
  optimizing.value = true
  try {
    const pendingOrders = orders.value.filter(o => o.status === 'pending')
    
    const sortedOrders = [...pendingOrders].sort((a, b) => {
      const scoreA = a.priority * 10 + (100 - a.distance)
      const scoreB = b.priority * 10 + (100 - b.distance)
      return scoreB - scoreA
    })
    
    optimizedRoute.value = sortedOrders.map((order, index) => ({
      order,
      sequence: index + 1,
      estimatedArrival: calculateEstimatedArrival(index),
      distanceFromPrev: index === 0 ? order.distance : calculateDistance(index)
    }))
    
    totalDistance.value = optimizedRoute.value.reduce((sum, p) => sum + p.distanceFromPrev, 0)
    totalTime.value = optimizedRoute.value.reduce((sum, p) => sum + p.order.estimatedTime, 0)
    
    await window.electronAPI?.delivery?.saveOptimizedRoute?.(optimizedRoute.value)
  } finally {
    optimizing.value = false
  }
}

const calculateEstimatedArrival = (index: number): string => {
  const now = new Date()
  const minutes = optimizedRoute.value.slice(0, index + 1).reduce((sum, p) => sum + p.order.estimatedTime, 0)
  now.setMinutes(now.getMinutes() + minutes)
  return now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const calculateDistance = (index: number): number => {
  if (index === 0) return optimizedRoute.value[0].order.distance
  const prev = optimizedRoute.value[index - 1].order
  const curr = optimizedRoute.value[index].order
  return Math.abs(curr.distance - prev.distance) + 2
}

const startNavigation = (point: RoutePoint) => {
  router.push({
    path: '/navigation',
    query: {
      orderId: point.order.id,
      lat: point.order.latitude,
      lng: point.order.longitude,
      address: point.order.address
    }
  })
}

const handleSignature = (order: DeliveryOrder) => {
  router.push({
    path: '/sign',
    query: { orderId: order.id }
  })
}

const refreshLocation = () => {
  getCurrentLocation()
}

const formatDistance = (distance: number): string => {
  return distance < 1 ? `${Math.round(distance * 1000)}米` : `${distance.toFixed(1)}公里`
}

const formatTime = (minutes: number): string => {
  if (minutes < 60) return `${minutes}分钟`
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  return `${hours}小时${mins}分钟`
}
</script>

<template>
  <div class="route-page">
    <div class="page-header">
      <h1>路线优化</h1>
      <button class="refresh-btn" @click="refreshLocation">
        <svg viewBox="0 0 24 24" width="20" height="20">
          <path fill="currentColor" d="M12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm8.94 3A8.994 8.994 0 0013 3.06V1h-2v2.06A8.994 8.994 0 003.06 11H1v2h2.06A8.994 8.994 0 0011 20.94V23h2v-2.06A8.994 8.994 0 0020.94 13H23v-2h-2.06zM12 19c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z"/>
        </svg>
        定位
      </button>
    </div>
    
    <div class="location-info">
      <div class="current-location">
        <svg viewBox="0 0 24 24" width="16" height="16">
          <path fill="#1988fa" d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
        </svg>
        <span>当前位置: {{ currentLocation.latitude.toFixed(4) }}°N, {{ currentLocation.longitude.toFixed(4) }}°E</span>
      </div>
      <div class="order-count">
        待配送订单: <strong>{{ orders.filter(o => o.status === 'pending').length }}</strong> 个
      </div>
    </div>
    
    <div class="optimize-section" v-if="optimizedRoute.length === 0">
      <button 
        class="optimize-btn"
        :disabled="optimizing || orders.filter(o => o.status === 'pending').length === 0"
        @click="optimizeRoute"
      >
        <svg v-if="!optimizing" viewBox="0 0 24 24" width="24" height="24">
          <path fill="currentColor" d="M19 11h-6V5h-2v6H5v2h6v6h2v-6h6z"/>
        </svg>
        <div v-if="optimizing" class="spinner"></div>
        {{ optimizing ? '正在优化...' : '开始路线优化' }}
      </button>
      
      <div class="tips">
        <p>💡 提示：系统将根据订单优先级、距离和时间自动规划最优配送路线</p>
      </div>
    </div>
    
    <div class="route-result" v-if="optimizedRoute.length > 0">
      <div class="route-summary">
        <div class="summary-item">
          <span class="label">总距离</span>
          <span class="value">{{ formatDistance(totalDistance) }}</span>
        </div>
        <div class="summary-item">
          <span class="label">预计时间</span>
          <span class="value">{{ formatTime(totalTime) }}</span>
        </div>
        <div class="summary-item">
          <span class="label">配送点数</span>
          <span class="value">{{ optimizedRoute.length }}个</span>
        </div>
      </div>
      
      <div class="route-list">
        <div 
          v-for="point in optimizedRoute"
          :key="point.order.id"
          class="route-point"
        >
          <div class="point-header">
            <span class="sequence">{{ point.sequence }}</span>
            <span class="order-no">{{ point.order.orderNo }}</span>
            <span class="priority" v-if="point.order.priority <= 2">优先</span>
          </div>
          
          <div class="point-body">
            <div class="customer-info">
              <span class="name">{{ point.order.customerName }}</span>
              <span class="phone">{{ point.order.customerPhone }}</span>
            </div>
            
            <div class="address-info">
              <svg viewBox="0 0 24 24" width="14" height="14">
                <path fill="#969799" d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7z"/>
              </svg>
              <span>{{ point.order.address }}</span>
            </div>
            
            <div class="items-info">
              商品: {{ point.order.items.map(i => `${i.productName}(${i.quantity}${i.unit})`).join(', ') }}
            </div>
            
            <div class="time-info">
              <span>预计到达: {{ point.estimatedArrival }}</span>
              <span>距离: {{ formatDistance(point.distanceFromPrev) }}</span>
            </div>
          </div>
          
          <div class="point-actions">
            <button class="nav-btn" @click="startNavigation(point)">
              <svg viewBox="0 0 24 24" width="16" height="16">
                <path fill="currentColor" d="M12 2L4.5 20.29l.71.71L12 18l6.79 3 .71-.71z"/>
              </svg>
              导航
            </button>
            <button class="sign-btn" @click="handleSignature(point.order)">
              <svg viewBox="0 0 24 24" width="16" height="16">
                <path fill="currentColor" d="M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z"/>
              </svg>
              签收
            </button>
          </div>
        </div>
      </div>
      
      <button class="reoptimize-btn" @click="optimizedRoute = []">
        重新规划
      </button>
    </div>
    
    <div class="order-list" v-if="!loading && optimizedRoute.length === 0">
      <h3>待配送订单</h3>
      <div 
        v-for="order in orders.filter(o => o.status === 'pending')"
        :key="order.id"
        class="order-card"
      >
        <div class="order-header">
          <span class="order-no">{{ order.orderNo }}</span>
          <span class="priority-badge" v-if="order.priority <= 2">优先配送</span>
        </div>
        
        <div class="order-body">
          <div class="customer">{{ order.customerName }} - {{ order.customerPhone }}</div>
          <div class="address">{{ order.address }}</div>
          <div class="items">{{ order.items.length }}件商品</div>
          <div class="distance">距离: {{ formatDistance(order.distance) }}</div>
        </div>
      </div>
    </div>
    
    <div class="loading-state" v-if="loading">
      <div class="spinner"></div>
      <p>加载订单...</p>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.route-page {
  padding: 16px;
  background: #f7f8fa;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  
  h1 {
    font-size: 18px;
    font-weight: 600;
    color: #333;
  }
  
  .refresh-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 8px 12px;
    background: #fff;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    cursor: pointer;
    font-size: 12px;
  }
}

.location-info {
  background: #fff;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  
  .current-location {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: #666;
    margin-bottom: 8px;
  }
  
  .order-count {
    font-size: 14px;
    color: #333;
    
    strong {
      color: #1988fa;
      font-size: 18px;
    }
  }
}

.optimize-section {
  text-align: center;
  padding: 40px 20px;
  
  .optimize-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 16px 32px;
    background: #1988fa;
    color: #fff;
    border: none;
    border-radius: 8px;
    font-size: 16px;
    cursor: pointer;
    
    &:disabled {
      background: #969799;
      cursor: not-allowed;
    }
    
    .spinner {
      width: 20px;
      height: 20px;
      border: 2px solid #fff;
      border-top-color: transparent;
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }
  }
  
  .tips {
    margin-top: 16px;
    color: #969799;
    font-size: 12px;
  }
}

.route-result {
  .route-summary {
    display: flex;
    gap: 12px;
    margin-bottom: 16px;
    
    .summary-item {
      flex: 1;
      background: #fff;
      padding: 12px;
      border-radius: 8px;
      text-align: center;
      
      .label {
        font-size: 12px;
        color: #969799;
      }
      
      .value {
        font-size: 16px;
        font-weight: 600;
        color: #1988fa;
        margin-top: 4px;
      }
    }
  }
  
  .route-list {
    .route-point {
      background: #fff;
      border-radius: 8px;
      margin-bottom: 12px;
      overflow: hidden;
      
      .point-header {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 12px 16px;
        background: #f7f8fa;
        
        .sequence {
          width: 24px;
          height: 24px;
          background: #1988fa;
          color: #fff;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 12px;
          font-weight: 600;
        }
        
        .order-no {
          font-size: 14px;
          font-weight: 600;
          color: #333;
        }
        
        .priority {
          padding: 2px 8px;
          background: #ff976a;
          color: #fff;
          font-size: 10px;
          border-radius: 4px;
        }
      }
      
      .point-body {
        padding: 16px;
        
        .customer-info {
          margin-bottom: 8px;
          
          .name {
            font-size: 14px;
            font-weight: 600;
            color: #333;
          }
          
          .phone {
            font-size: 12px;
            color: #969799;
            margin-left: 8px;
          }
        }
        
        .address-info {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 12px;
          color: #666;
          margin-bottom: 8px;
        }
        
        .items-info {
          font-size: 12px;
          color: #969799;
          margin-bottom: 8px;
        }
        
        .time-info {
          display: flex;
          gap: 16px;
          font-size: 12px;
          color: #1988fa;
        }
      }
      
      .point-actions {
        display: flex;
        gap: 8px;
        padding: 12px 16px;
        border-top: 1px solid #ebedf0;
        
        .nav-btn, .sign-btn {
          flex: 1;
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 4px;
          padding: 10px;
          border: none;
          border-radius: 4px;
          font-size: 14px;
          cursor: pointer;
        }
        
        .nav-btn {
          background: #1988fa;
          color: #fff;
        }
        
        .sign-btn {
          background: #07c160;
          color: #fff;
        }
      }
    }
  }
  
  .reoptimize-btn {
    width: 100%;
    padding: 12px;
    background: #f7f8fa;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    font-size: 14px;
    color: #666;
    cursor: pointer;
    margin-top: 16px;
  }
}

.order-list {
  h3 {
    font-size: 14px;
    color: #969799;
    margin-bottom: 12px;
  }
  
  .order-card {
    background: #fff;
    border-radius: 8px;
    padding: 16px;
    margin-bottom: 12px;
    
    .order-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 12px;
      
      .order-no {
        font-size: 14px;
        font-weight: 600;
        color: #333;
      }
      
      .priority-badge {
        padding: 2px 8px;
        background: #ff976a;
        color: #fff;
        font-size: 10px;
        border-radius: 4px;
      }
    }
    
    .order-body {
      font-size: 12px;
      color: #666;
      
      .customer { margin-bottom: 4px; }
      .address { margin-bottom: 4px; color: #969799; }
      .items { margin-bottom: 4px; }
      .distance { color: #1988fa; }
    }
  }
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  
  .spinner {
    width: 32px;
    height: 32px;
    border: 3px solid #ebedf0;
    border-top-color: #1988fa;
    border-radius: 50%;
    animation: spin 1s linear infinite;
  }
  
  p {
    margin-top: 12px;
    color: #969799;
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>