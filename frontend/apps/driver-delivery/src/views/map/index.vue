<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Card, Tag, Empty, showLoadingToast, closeToast } from 'vant'
import { api } from '@/api'

const router = useRouter()

interface DeliveryPoint {
  id: string
  orderNo: string
  customerName: string
  address: string
  location: { lat: number; lng: number }
  status: string
  priority: string
}

const deliveryPoints = ref<DeliveryPoint[]>([])
const mapCenter = ref({ lat: 39.9, lng: 116.4 })
const loading = ref(false)

onMounted(async () => {
  loadDeliveryPoints()
})

const loadDeliveryPoints = async () => {
  loading.value = true
  showLoadingToast({ message: '加载中...', forbidClick: true })
  
  try {
    const res = await api.delivery.getMapPoints()
    deliveryPoints.value = res.data || [
      {
        id: '1',
        orderNo: 'DL202401001',
        customerName: '张三',
        address: '北京市朝阳区建国路88号',
        location: { lat: 39.9, lng: 116.4 },
        status: 'pending',
        priority: 'high'
      },
      {
        id: '2',
        orderNo: 'DL202401002',
        customerName: '李四',
        address: '北京市海淀区中关村大街1号',
        location: { lat: 39.98, lng: 116.31 },
        status: 'in_progress',
        priority: 'normal'
      },
      {
        id: '3',
        orderNo: 'DL202401003',
        customerName: '王五',
        address: '北京市西城区西单北大街',
        location: { lat: 39.91, lng: 116.37 },
        status: 'pending',
        priority: 'low'
      }
    ]
    
    if (deliveryPoints.value.length > 0) {
      mapCenter.value = deliveryPoints.value[0].location
    }
  } finally {
    loading.value = false
    closeToast()
  }
}

const handlePointClick = (point: DeliveryPoint) => {
  router.push(`/delivery/${point.id}`)
}

const handleNavigate = (point: DeliveryPoint) => {
  router.push(`/map/navigation/${point.id}`)
}

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
</script>

<template>
  <div class="map-page">
    <NavBar title="配送地图" />
    
    <div class="map-container">
      <div class="map-placeholder">
        <div class="map-info">
          <div class="map-title">配送路线总览</div>
          <div class="map-desc">点击下方任务卡片查看详情或导航</div>
        </div>
        
        <div class="map-stats">
          <div class="stat-item">
            <div class="stat-value">{{ deliveryPoints.length }}</div>
            <div class="stat-label">配送点</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ deliveryPoints.filter(p => p.status === 'pending').length }}</div>
            <div class="stat-label">待配送</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ deliveryPoints.filter(p => p.status === 'in_progress').length }}</div>
            <div class="stat-label">配送中</div>
          </div>
        </div>
      </div>
    </div>
    
    <div class="delivery-list">
      <div class="list-header">
        <span class="header-title">配送任务列表</span>
      </div>
      
      <Empty v-if="deliveryPoints.length === 0 && !loading" description="暂无配送任务" />
      
      <Card 
        v-for="point in deliveryPoints"
        :key="point.id"
        class="point-card"
        @click="handlePointClick(point)"
      >
        <template #title>
          <div class="point-header">
            <span class="point-no">{{ point.orderNo }}</span>
            <Tag :color="statusMap[point.status].color">
              {{ statusMap[point.status].label }}
            </Tag>
            <Tag :color="priorityMap[point.priority].color">
              {{ priorityMap[point.priority].label }}
            </Tag>
          </div>
        </template>
        
        <template #desc>
          <div class="point-info">
            <div class="info-row">
              <span class="label">客户:</span>
              <span class="value">{{ point.customerName }}</span>
            </div>
            <div class="info-row">
              <span class="label">地址:</span>
              <span class="value">{{ point.address }}</span>
            </div>
          </div>
        </template>
        
        <template #footer>
          <div class="point-actions">
            <button class="action-btn primary" @click.stop="handleNavigate(point)">
              导航
            </button>
            <button class="action-btn" @click.stop="handlePointClick(point)">
              详情
            </button>
          </div>
        </template>
      </Card>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.map-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 60px;
}

.map-container {
  height: 200px;
  background: linear-gradient(135deg, #1988fa, #4facfe);
  
  .map-placeholder {
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #fff;
    
    .map-info {
      text-align: center;
      
      .map-title {
        font-size: 18px;
        font-weight: 600;
      }
      
      .map-desc {
        font-size: 14px;
        margin-top: 8px;
        opacity: 0.8;
      }
    }
    
    .map-stats {
      display: flex;
      gap: 24px;
      margin-top: 16px;
      
      .stat-item {
        text-align: center;
        
        .stat-value {
          font-size: 24px;
          font-weight: 600;
        }
        
        .stat-label {
          font-size: 12px;
          margin-top: 4px;
          opacity: 0.8;
        }
      }
    }
  }
}

.delivery-list {
  padding: 12px;
  
  .list-header {
    margin-bottom: 12px;
    
    .header-title {
      font-size: 16px;
      font-weight: 600;
      color: #333;
    }
  }
}

.point-card {
  margin-bottom: 12px;
  
  .point-header {
    display: flex;
    align-items: center;
    gap: 8px;
    
    .point-no {
      font-size: 14px;
      font-weight: 600;
    }
  }
  
  .point-info {
    .info-row {
      display: flex;
      margin-top: 4px;
      
      .label {
        width: 50px;
        color: #969799;
      }
      
      .value {
        color: #333;
      }
    }
  }
  
  .point-actions {
    display: flex;
    gap: 8px;
    justify-content: flex-end;
    
    .action-btn {
      padding: 4px 12px;
      font-size: 12px;
      border-radius: 4px;
      background: #fff;
      border: 1px solid #ddd;
      color: #333;
      
      &.primary {
        background: #1988fa;
        border-color: #1988fa;
        color: #fff;
      }
    }
  }
}
</style>