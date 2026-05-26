<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { NavBar, Button, showToast, showLoadingToast, closeToast } from 'vant'
import AMapLoader from '@amap/amap-jsapi-loader'
import { api } from '@/api'

const router = useRouter()
const route = useRoute()

const orderId = route.params.id as string
const order = ref<any>(null)
const map = ref<any>(null)
const currentLocation = ref<{ lat: number; lng: number } | null>(null)

onMounted(async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const res = await api.order.getDetail(orderId)
    order.value = res.data
    
    await initMap()
  } finally {
    closeToast()
  }
})

const initMap = async () => {
  try {
    const AMap = await AMapLoader.load({
      key: '',
      version: '2.0',
      plugins: ['AMap.Geolocation', 'AMap.Marker', 'AMap.Navigation']
    })
    
    map.value = new AMap.Map('map-container', {
      zoom: 15,
      center: [order.value?.location?.lng || 116.397428, order.value?.location?.lat || 39.90923]
    })
    
    const geolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000
    })
    
    geolocation.getCurrentPosition((status: string, result: any) => {
      if (status === 'complete') {
        currentLocation.value = {
          lat: result.position.lat,
          lng: result.position.lng
        }
        
        new AMap.Marker({
          position: [result.position.lng, result.position.lat],
          title: '当前位置',
          icon: new AMap.Icon({
            size: new AMap.Size(25, 34),
            image: 'https://a.amap.com/jsapi_demos/static/demo-center/icons/poi-marker-red.png'
          })
        }).addTo(map.value)
      }
    })
    
    new AMap.Marker({
      position: [order.value?.location?.lng, order.value?.location?.lat],
      title: order.value?.address,
      icon: new AMap.Icon({
        size: new AMap.Size(25, 34),
        image: 'https://a.amap.com/jsapi_demos/static/demo-center/icons/poi-marker-default.png'
      })
    }).addTo(map.value)
    
    map.value.plugin('AMap.Navigation', () => {
      const navigation = new AMap.Navigation({
        target: [order.value?.location?.lng, order.value?.location?.lat],
        panel: 'navigation-panel'
      })
      map.value.addControl(navigation)
    })
    
  } catch (error) {
    console.error('地图加载失败:', error)
    showToast({ type: 'fail', message: '地图加载失败' })
  }
}

const handleStartNavigation = () => {
  if (!currentLocation.value || !order.value?.location) {
    showToast({ type: 'fail', message: '无法获取位置信息' })
    return
  }
  
  const url = `https://uri.amap.com/navigation?from=${currentLocation.value.lng},${currentLocation.value.lat},当前位置&to=${order.value.location.lng},${order.value.location.lat},${order.value.address}&mode=car&policy=1&src=智企连配送`
  
  window.open(url, '_blank')
}

const handleCallCustomer = () => {
  if (order.value?.customerPhone) {
    window.location.href = `tel:${order.value.customerPhone}`
  }
}
</script>

<template>
  <div class="navigation-page">
    <NavBar 
      title="导航" 
      left-arrow
      @click-left="router.back()"
    />
    
    <div id="map-container" class="map-container"></div>
    
    <div class="navigation-info">
      <div class="info-card">
        <div class="info-title">配送地址</div>
        <div class="info-address">{{ order?.address }}</div>
        <div class="info-customer">
          {{ order?.customerName }} - {{ order?.customerPhone }}
        </div>
      </div>
      
      <div class="action-buttons">
        <Button 
          type="primary" 
          size="large"
          icon="location-o"
          @click="handleStartNavigation"
        >
          开始导航
        </Button>
        <Button 
          type="default" 
          size="large"
          icon="phone-o"
          @click="handleCallCustomer"
        >
          联系客户
        </Button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.navigation-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.map-container {
  flex: 1;
  min-height: 300px;
}

.navigation-info {
  background: #fff;
  padding: 16px;
  
  .info-card {
    padding: 12px;
    background: #f7f8fa;
    border-radius: 8px;
    
    .info-title {
      font-size: 14px;
      color: #969799;
    }
    
    .info-address {
      font-size: 16px;
      color: #333;
      margin-top: 8px;
    }
    
    .info-customer {
      font-size: 14px;
      color: #1988fa;
      margin-top: 4px;
    }
  }
  
  .action-buttons {
    display: flex;
    gap: 12px;
    margin-top: 16px;
  }
}
</style>