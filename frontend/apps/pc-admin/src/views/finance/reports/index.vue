<template>
  <PageContainer full-height>
    <template #header>
      <div class="reports-page-header">
        <div class="reports-page-header-left">
          <a-breadcrumb class="reports-breadcrumb">
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>财务管理</a-breadcrumb-item>
            <a-breadcrumb-item>财务报表</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="reports-page-header-title">财务报表</h2>
        </div>
        <div class="reports-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', refreshData)">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>
    <div class="reports-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-assets">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ formatAmount(summaryData.totalAssets) }}</div>
          <div class="stat-card-label">总资产</div>
        </div>
        <FundOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-liabilities">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ formatAmount(summaryData.totalLiabilities) }}</div>
          <div class="stat-card-label">总负债</div>
        </div>
        <CreditCardOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-income">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ formatAmount(summaryData.netIncome) }}</div>
          <div class="stat-card-label">净利润</div>
        </div>
        <DollarOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-date">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ periodDate.format('YYYY-MM') }}</div>
          <div class="stat-card-label">报表期间</div>
        </div>
        <CalendarOutlined class="stat-card-icon" />
      </div>
    </div>

    <a-card title="财务报表">
      <!-- Tab切换 -->
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane
          key="balance-sheet"
          tab="资产负债表"
        >
          <BalanceSheet :period="periodDate.format('YYYY-MM')" @loaded="handleBalanceLoaded" />
        </a-tab-pane>
        <a-tab-pane
          key="profit-statement"
          tab="利润表"
        >
          <ProfitStatement :period="periodDate.format('YYYY-MM')" @loaded="handleProfitLoaded" />
        </a-tab-pane>
        <a-tab-pane
          key="cash-flow"
          tab="现金流量表"
        >
          <CashFlowStatement :period="periodDate.format('YYYY-MM')" />
        </a-tab-pane>
      </a-tabs>
    </a-card>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { FundOutlined, CreditCardOutlined, DollarOutlined, CalendarOutlined, SyncOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { PageContainer } from '@/components'
import BalanceSheet from './components/BalanceSheet.vue'

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now(); const last = debounceMap.get(key) || 0
  if (now - last < delay) return; debounceMap.set(key, now); fn()
}
import ProfitStatement from './components/ProfitStatement.vue'
import CashFlowStatement from './components/CashFlowStatement.vue'

const activeTab = ref('balance-sheet')
const periodDate = ref(dayjs())
const refreshLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)

const summaryData = reactive({
  totalAssets: 0,
  totalLiabilities: 0,
  netIncome: 0
})

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

function handleBalanceLoaded(data: any) {
  if (data) {
    summaryData.totalAssets = data.totalAssets || 0
    summaryData.totalLiabilities = data.totalLiabilities || 0
  }
}

function handleProfitLoaded(data: any) {
  if (data) {
    summaryData.netIncome = data.netProfit || 0
  }
}

function refreshData() {
  refreshLoading.value = true
  try {
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    // 子组件通过 @loaded 事件自动更新统计数据
  } catch (err) {
    console.warn('[财务报表] 刷新失败', err)
    message.error('刷新失败')
  } finally {
    refreshLoading.value = false
  }
}

// 定时刷新（30s）
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('finance:create', handleParentCreate)
  window.addEventListener('finance:refresh', refreshData)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    refreshData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('finance:create', handleParentCreate)
  window.removeEventListener('finance:refresh', refreshData)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

function handleParentCreate() { handleAdd() }
function handleAdd() {
  message.info('创建功能由父组件触发')
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', refreshData); return }
  if (e.ctrlKey && e.key === 'n') { e.preventDefault(); debounceClick('add', handleAdd); return }
}

defineExpose({})
</script>

<style scoped>
.reports-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.reports-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.reports-breadcrumb {
  font-size: 13px;
}
.reports-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.reports-page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.update-time {
  font-size: 12px;
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

.reports-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-assets { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-liabilities { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-income { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-date { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-body {
  flex: 1;
}

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}

/* Compact mode overrides */
:deep(.ant-table-thead > tr > th) {
  padding: 6px 8px !important;
  font-size: 12px;
}
:deep(.ant-table-tbody > tr > td) {
  padding: 4px 8px !important;
  font-size: 12px;
}
:deep(.ant-card-body) {
  padding: 12px;
}
:deep(.ant-form-item) {
  margin-bottom: 8px;
}
</style>