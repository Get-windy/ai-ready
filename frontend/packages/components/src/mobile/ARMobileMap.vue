<template>
  <div class="ar-mobile-map">
    <div class="map-header" v-if="showHeader">
      <div class="header-content">
        <span class="title">{{ title }}</span>
        <span class="subtitle" v-if="subtitle">{{ subtitle }}</span>
      </div>
      <div class="header-actions">
        <button class="action-btn" @click="handleRefresh">
          <svg viewBox="0 0 24 24" width="16" height="16">
            <path fill="currentColor" d="M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z"/>
          </svg>
        </button>
        <button class="action-btn" @click="handleFullscreen">
          <svg viewBox="0 0 24 24" width="16" height="16">
            <path fill="currentColor" d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z"/>
          </svg>
        </button>
      </div>
    </div>
    
    <div class="map-container" ref="mapContainerRef">
      <div v-if="loading" class="map-loading">
        <span>地图加载中...</span>
      </div>
      
      <div v-if="error" class="map-error">
        <span>{{ error }}</span>
        <button @click="handleRefresh">重新加载</button>
      </div>
    </div>
    
    <div class="map-controls" v-if="showControls">
      <button class="control-btn zoom-in" @click="handleZoomIn">
        <svg viewBox="0 0 24 24" width="20" height="20">
          <path fill="currentColor" d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>
        </svg>
      </button>
      <button class="control-btn zoom-out" @click="handleZoomOut">
        <svg viewBox="0 0 24 24" width="20" height="20">
          <path fill="currentColor" d="M19 13H5v-2h14v2z"/>
        </svg>
      </button>
      <button class="control-btn locate" @click="handleLocate">
        <svg viewBox="0 0 24 24" width="20" height="20">
          <path fill="currentColor" d="M12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm8.94 3c-.46-4.17-3.77-7.48-7.94-7.94V1h-2v2.06C6.83 3.52 3.52 6.83 3.06 11H1v2h2.06c.46 4.17 3.77 7.48 7.94 7.94V23h2v-2.06c4.17-.46 7.48-3.77 7.94-7.94H23v-2h-2.06zM12 19c-3.87 0-7-3.13-7-7s3.13-7 7-7 7 3.13 7 7-3.13 7-7 7z"/>
        </svg>
      </button>
    </div>
    
    <div class="map-info" v-if="currentLocation">
      <div class="location-info">
        <span class="label">当前位置:</span>
        <span class="value">{{ formatLocation(currentLocation) }}</span>
      </div>
    </div>
    
    <div class="markers-list" v-if="markers.length > 0 && showMarkersList">
      <div 
        v-for="marker in markers"
        :key="marker.id"
        class="marker-item"
        :class="{ active: activeMarkerId === marker.id }"
        @click="handleMarkerClick(marker)"
      >
        <div class="marker-icon">
          <svg viewBox="0 0 24 24" width="16" height="16">
            <path fill="currentColor" d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>
          </svg>
        </div>
        <div class="marker-info">
          <span class="marker-title">{{ marker.title }}</span>
          <span class="marker-address" v-if="marker.address">{{ marker.address }}</span>
        </div>
        <span class="marker-distance" v-if="marker.distance">
          {{ formatDistance(marker.distance) }}
        </span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'

interface Marker {
  id: string
  title: string
  address?: string
  location: { lat: number; lng: number }
  distance?: number
  type?: string
}

interface Props {
  title?: string
  subtitle?: string
  center?: { lat: number; lng: number }
  zoom?: number
  markers?: Marker[]
  showHeader?: boolean
  showControls?: boolean
  showMarkersList?: boolean
  enableNavigation?: boolean
  mapKey?: string
}

const props = withDefaults(defineProps<Props>(), {
  title: '地图',
  zoom: 15,
  markers: () => [],
  showHeader: true,
  showControls: true,
  showMarkersList: true,
  enableNavigation: true
})

const emit = defineEmits<{
  (e: 'ready', map: any): void
  (e: 'marker-click', marker: Marker): void
  (e: 'location-change', location: { lat: number; lng: number }): void
  (e: 'navigate', from: any, to: any): void
}>()

const mapContainerRef = ref<HTMLElement | null>(null)
const loading = ref(true)
const error = ref('')
const currentLocation = ref<{ lat: number; lng: number } | null>(null)
const activeMarkerId = ref('')
const mapInstance = ref<any>(null)

let AMap: any = null

onMounted(async () => {
  await initMap()
})

onUnmounted(() => {
  if (mapInstance.value) {
    mapInstance.value.destroy()
  }
})

watch(() => props.markers, () => {
  updateMarkers()
}, { deep: true })

const initMap = async () => {
  loading.value = true
  error.value = ''
  
  try {
    AMap = await loadAMapScript()
    
    if (!mapContainerRef.value) return
    
    const center = props.center || [116.397428, 39.90923]
    
    mapInstance.value = new AMap.Map(mapContainerRef.value, {
      zoom: props.zoom,
      center: center,
      viewMode: '2D'
    })
    
    AMap.plugin(['AMap.Geolocation', 'AMap.Marker', 'AMap.Navigation'], () => {
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
          emit('location-change', currentLocation.value)
        }
      })
      
      mapInstance.value.addControl(geolocation)
    })
    
    updateMarkers()
    
    loading.value = false
    emit('ready', mapInstance.value)
  } catch (err) {
    loading.value = false
    error.value = '地图加载失败'
    console.error('地图初始化失败:', err)
  }
}

const loadAMapScript = async (): Promise<any> => {
  return new Promise((resolve, reject) => {
    if (window.AMap) {
      resolve(window.AMap)
      return
    }
    
    const script = document.createElement('script')
    script.type = 'text/javascript'
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${props.mapKey || ''}`
    script.onload = () => resolve(window.AMap)
    script.onerror = () => reject(new Error('AMap script load failed'))
    document.head.appendChild(script)
  })
}

const updateMarkers = () => {
  if (!mapInstance.value || !AMap) return
  
  mapInstance.value.clearMap()
  
  props.markers.forEach(marker => {
    const markerInstance = new AMap.Marker({
      position: [marker.location.lng, marker.location.lat],
      title: marker.title,
      content: createMarkerContent(marker)
    })
    
    markerInstance.on('click', () => {
      activeMarkerId.value = marker.id
      emit('marker-click', marker)
    })
    
    mapInstance.value.add(markerInstance)
  })
}

const createMarkerContent = (marker: Marker) => {
  const color = marker.type === 'start' ? '#07c160' : 
                marker.type === 'end' ? '#f44' : '#1988fa'
  
  return `<div style="
    width: 24px;
    height: 34px;
    background: ${color};
    border-radius: 12px 12px 0 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    font-size: 12px;
    font-weight: bold;
  ">${marker.type === 'start' ? '起' : marker.type === 'end' ? '终' : '点'}</div>`
}

const handleRefresh = () => {
  initMap()
}

const handleFullscreen = () => {
  if (mapContainerRef.value) {
    if (document.fullscreenElement) {
      document.exitFullscreen()
    } else {
      mapContainerRef.value.requestFullscreen()
    }
  }
}

const handleZoomIn = () => {
  if (mapInstance.value) {
    mapInstance.value.zoomIn()
  }
}

const handleZoomOut = () => {
  if (mapInstance.value) {
    mapInstance.value.zoomOut()
  }
}

const handleLocate = () => {
  if (mapInstance.value && currentLocation.value) {
    mapInstance.value.setCenter([
      currentLocation.value.lng,
      currentLocation.value.lat
    ])
  }
}

const handleMarkerClick = (marker: Marker) => {
  if (mapInstance.value) {
    mapInstance.value.setCenter([
      marker.location.lng,
      marker.location.lat
    ])
    mapInstance.value.setZoom(16)
  }
  
  emit('marker-click', marker)
  
  if (props.enableNavigation && currentLocation.value) {
    emit('navigate', currentLocation.value, marker.location)
  }
}

const formatLocation = (location: { lat: number; lng: number }) => {
  return `${location.lat.toFixed(4)}, ${location.lng.toFixed(4)}`
}

const formatDistance = (distance: number) => {
  if (distance < 1000) return `${distance}m`
  return `${(distance / 1000).toFixed(1)}km`
}

declare global {
  interface Window {
    AMap: any
  }
}
</script>

<style lang="scss" scoped>
.ar-mobile-map {
  position: relative;
  width: 100%;
  height: 100%;
  background: #f7f8fa;
  
  .map-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    background: #fff;
    border-bottom: 1px solid #ebedf0;
    
    .header-content {
      .title {
        font-size: 16px;
        font-weight: 600;
        color: #333;
      }
      
      .subtitle {
        font-size: 12px;
        color: #969799;
        margin-left: 8px;
      }
    }
    
    .header-actions {
      display: flex;
      gap: 8px;
      
      .action-btn {
        padding: 8px;
        background: #f7f8fa;
        border: none;
        border-radius: 4px;
        cursor: pointer;
        
        &:hover {
          background: #ebedf0;
        }
      }
    }
  }
  
  .map-container {
    width: 100%;
    height: calc(100% - 48px);
    position: relative;
    
    .map-loading, .map-error {
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      text-align: center;
      
      span {
        color: #969799;
      }
      
      button {
        margin-top: 8px;
        padding: 8px 16px;
        background: #1988fa;
        color: #fff;
        border: none;
        border-radius: 4px;
        cursor: pointer;
      }
    }
  }
  
  .map-controls {
    position: absolute;
    right: 16px;
    bottom: 100px;
    display: flex;
    flex-direction: column;
    gap: 8px;
    
    .control-btn {
      width: 40px;
      height: 40px;
      background: #fff;
      border: 1px solid #ebedf0;
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      
      &:hover {
        background: #f7f8fa;
      }
    }
  }
  
  .map-info {
    position: absolute;
    left: 16px;
    bottom: 16px;
    padding: 8px 12px;
    background: #fff;
    border-radius: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    
    .location-info {
      .label {
        color: #969799;
        font-size: 12px;
      }
      
      .value {
        color: #333;
        font-size: 14px;
        margin-left: 4px;
      }
    }
  }
  
  .markers-list {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    max-height: 150px;
    background: #fff;
    overflow-y: auto;
    
    .marker-item {
      display: flex;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid #ebedf0;
      cursor: pointer;
      
      &:hover {
        background: #f7f8fa;
      }
      
      &.active {
        background: #e6f7ff;
      }
      
      .marker-icon {
        width: 24px;
        height: 24px;
        color: #1988fa;
      }
      
      .marker-info {
        flex: 1;
        margin-left: 12px;
        
        .marker-title {
          font-size: 14px;
          color: #333;
        }
        
        .marker-address {
          font-size: 12px;
          color: #969799;
          margin-top: 2px;
        }
      }
      
      .marker-distance {
        font-size: 12px;
        color: #1988fa;
      }
    }
  }
}
</style>