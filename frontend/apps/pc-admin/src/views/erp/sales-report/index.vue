<template>
  <PageContainer title="销售报表" full-height>
    <template #headerExtra>
      <a-space :size="12">
        <span class="data-status">
          <a-badge :status="loading ? 'processing' : 'success'" />
          <span v-if="lastUpdateTime" class="update-time">
            数据更新: {{ lastUpdateTime }}
          </span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
        </span>
        <a-button size="small" @click="handleRefresh">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </template>

    <!-- 统计卡片 -->
    <div class="summary-cards" style="padding: 16px 0;">
      <a-row :gutter="16">
        <a-col :span="6">
          <div class="summary-card">
            <div class="summary-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
              <DollarOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">本月销售额</div>
              <div class="summary-value">¥{{ formatAmount(summary.monthAmount) }}</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="summary-card">
            <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
              <ShoppingOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">本月订单数</div>
              <div class="summary-value">{{ summary.monthOrders }}</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="summary-card">
            <div class="summary-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
              <TeamOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">活跃客户</div>
              <div class="summary-value">{{ summary.activeCustomers }}</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="summary-card highlight">
            <div class="summary-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
              <RiseOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">环比增长</div>
              <div class="summary-value">{{ summary.growthRate }}%</div>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>

    <div class="report-content">
      <a-tabs v-model:activeKey="activeTab" type="card" size="small">
        <a-tab-pane key="statistics" tab="销售统计">
          <SalesStatistics ref="statisticsRef" />
        </a-tab-pane>
        <a-tab-pane key="customer" tab="客户排行">
          <CustomerRanking ref="customerRef" />
        </a-tab-pane>
        <a-tab-pane key="product" tab="商品排行">
          <ProductRanking ref="productRef" />
        </a-tab-pane>
        <a-tab-pane key="trend" tab="销售趋势">
          <SalesTrend ref="trendRef" />
        </a-tab-pane>
      </a-tabs>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { ReloadOutlined, SyncOutlined, DollarOutlined, ShoppingOutlined, TeamOutlined, RiseOutlined } from '@ant-design/icons-vue'
import { PageContainer } from '@/components'
import { salesReportApi } from '@/api/sales-report'
import SalesStatistics from './components/SalesStatistics.vue'
import CustomerRanking from './components/CustomerRanking.vue'
import ProductRanking from './components/ProductRanking.vue'
import SalesTrend from './components/SalesTrend.vue'

const activeTab = ref('statistics')
const loading = ref(false)
const lastUpdateTime = ref<string>('')

// 统计数据
const summary = ref({
  monthAmount: 0,
  monthOrders: 0,
  activeCustomers: 0,
  growthRate: 0
})

const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const loadData = async () => {
  try {
    const res = await salesReportApi.getStatistics()
    if (res?.data) {
      summary.value.monthAmount = res.data.totalSales ?? res.data.monthAmount ?? 0
      summary.value.monthOrders = res.data.orderCount ?? res.data.monthOrders ?? 0
    }
  } catch (err) {
    console.warn('[销售报表] 加载统计数据失败', err)
  }
}

const statisticsRef = ref()
const customerRef = ref()
const productRef = ref()
const trendRef = ref()

const handleRefresh = async () => {
  loading.value = true
  lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  // 刷新当前 Tab 的数据
  const refMap: Record<string, any> = {
    statistics: statisticsRef.value,
    customer: customerRef.value,
    product: productRef.value,
    trend: trendRef.value
  }
  const currentRef = refMap[activeTab.value]
  if (currentRef?.handleQuery) {
    await currentRef.handleQuery()
  }
  loading.value = false
}

watch(activeTab, () => {
  lastUpdateTime.value = ''
})

defineExpose({ handleQuery: loadData })

onMounted(() => {
  loadData()
  lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    loadData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
})
</script>

<style scoped>
.report-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.data-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}

.update-time {
  color: #999;
}

.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  user-select: none;
}

/* 统计卡片样式 */
.summary-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.summary-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.summary-card.highlight {
  background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%);
  border: 1px solid #ffd591;
}

.summary-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  margin-right: 16px;
}

.summary-content {
  flex: 1;
}

.summary-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}

:deep(.ant-tabs) {
  height: 100%;
  display: flex;
  flex-direction: column;
}

:deep(.ant-tabs-content) {
  flex: 1;
  overflow: auto;
}

:deep(.ant-tabs-tabpane) {
  height: 100%;
}
</style>