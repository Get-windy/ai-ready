<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { NavBar, Search, Card, Button, Tag, Empty, showToast, showLoadingToast, closeToast } from 'vant'
import { api } from '@/api'

const router = useRouter()
const searchKeyword = ref('')
const locations = ref<any[]>([])

interface Location {
  code: string
  name: string
  warehouse: string
  zone: string
  row: string
  column: string
  layer: string
  status: 'empty' | 'partial' | 'full'
  productCount: number
  totalQuantity: number
  capacity: number
}

const statusMap: Record<string, { label: string; color: string }> = {
  empty: { label: '空置', color: '#969799' },
  partial: { label: '部分占用', color: '#1988fa' },
  full: { label: '已满', color: '#07c160' }
}

onMounted(async () => {
  loadLocations()
})

const loadLocations = async () => {
  showLoadingToast({ message: '加载中...', forbidClick: true })
  try {
    const res = await api.location.getList({ keyword: searchKeyword.value })
    locations.value = Array.isArray(res) ? res : (res as any)?.records || []
  } catch (err) {
    console.error('[库位] 加载失败', err)
    showToast('加载库位信息失败')
  } finally {
    closeToast()
  }
}

const handleSearch = () => {
  loadLocations()
}

const handleLocationClick = (location: Location) => {
  router.push(`/location/${location.code}`)
}

const handleScanLocation = () => {
  router.push('/location/scan')
}

const getUsagePercent = (location: Location) => {
  return Math.round((location.totalQuantity / location.capacity) * 100)
}
</script>

<template>
  <div class="location-page">
    <NavBar title="库位管理" />

    <div class="search-bar">
      <Search
        v-model:value="searchKeyword"
        placeholder="搜索库位编码"
        @search="handleSearch"
      />
    </div>

    <div class="location-stats">
      <div class="stat-item">
        <div class="stat-value">{{ locations.length }}</div>
        <div class="stat-label">总库位</div>
      </div>
      <div class="stat-item">
        <div class="stat-value">{{ locations.filter((l: any) => l.status === 'empty').length }}</div>
        <div class="stat-label">空置</div>
      </div>
      <div class="stat-item">
        <div class="stat-value">{{ locations.filter((l: any) => l.status === 'full').length }}</div>
        <div class="stat-label">已满</div>
      </div>
    </div>

    <div class="location-list">
      <div v-if="locations.length === 0" class="empty-container">
        <Empty description="暂无库位信息" />
      </div>

      <Card
        v-for="location in locations"
        :key="location.code"
        class="location-card"
        @click="handleLocationClick(location)"
      >
        <template #title>
          <div class="location-header">
            <span class="location-code">{{ location.code }}</span>
            <Tag :color="(statusMap[location.status]?.color) || '#969799'">
              {{ statusMap[location.status]?.label || location.status }}
            </Tag>
          </div>
        </template>

        <template #desc>
          <div class="location-info">
            <div class="info-row">
              <span class="label">仓库:</span>
              <span class="value">{{ location.warehouse }}</span>
            </div>
            <div class="info-row">
              <span class="label">区域:</span>
              <span class="value">{{ location.zone }}</span>
            </div>
            <div class="info-row">
              <span class="label">位置:</span>
              <span class="value">{{ location.row }}排 {{ location.column }}列 {{ location.layer }}层</span>
            </div>
            <div class="info-row">
              <span class="label">商品:</span>
              <span class="value">{{ location.productCount }} 种</span>
            </div>
            <div class="info-row">
              <span class="label">数量:</span>
              <span class="value">{{ location.totalQuantity }} / {{ location.capacity }}</span>
            </div>
          </div>
        </template>

        <template #footer>
          <div class="usage-bar">
            <div class="usage-fill" :style="{ width: getUsagePercent(location) + '%' }"></div>
          </div>
          <span class="usage-text">使用率: {{ getUsagePercent(location) }}%</span>
        </template>
      </Card>
    </div>

    <div class="action-bar">
      <Button
        type="primary"
        size="large"
        icon="scan"
        block
        @click="handleScanLocation"
      >
        扫码查询库位
      </Button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.location-page {
  min-height: 100vh;
  background: #f7f8fa;
  padding-bottom: 80px;
}

.search-bar {
  padding: 12px;
  background: #fff;
}

.location-stats {
  display: flex;
  justify-content: space-around;
  padding: 16px;
  background: #fff;
  margin-top: 8px;

  .stat-item {
    text-align: center;

    .stat-value {
      font-size: 24px;
      font-weight: 600;
      color: #333;
    }

    .stat-label {
      font-size: 12px;
      color: #969799;
      margin-top: 4px;
    }
  }
}

.empty-container {
  padding: 60px 20px;
  text-align: center;
}

.location-list {
  padding: 12px;
}

.location-card {
  margin-bottom: 12px;

  .location-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .location-code {
      font-size: 16px;
      font-weight: 600;
      color: #1988fa;
    }
  }

  .location-info {
    .info-row {
      display: flex;
      margin-top: 8px;

      .label {
        width: 50px;
        color: #969799;
      }

      .value {
        color: #333;
      }
    }
  }

  .usage-bar {
    height: 4px;
    background: #ebedf0;
    border-radius: 2px;
    overflow: hidden;

    .usage-fill {
      height: 100%;
      background: #07c160;
      transition: width 0.3s;
    }
  }

  .usage-text {
    font-size: 12px;
    color: #969799;
    margin-top: 4px;
  }
}

.action-bar {
  position: fixed;
  bottom: 50px;
  left: 0;
  right: 0;
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #ebedf0;
}
</style>
