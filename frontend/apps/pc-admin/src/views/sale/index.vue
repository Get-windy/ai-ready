<template>
  <PageContainer full-height>
  <ErrorBoundary>
  <div class="sale-module">
    <!-- 顶部统计栏 -->
    <div class="sale-module-header">
      <div class="sale-module-header-left">
        <a-breadcrumb class="sale-breadcrumb">
          <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
          <a-breadcrumb-item>销售管理</a-breadcrumb-item>
        </a-breadcrumb>
        <h1 class="sale-module-title">销售管理</h1>
      </div>
      <!-- 统计卡片 -->
      <div class="sale-stat-cards">
        <!-- 加载态：骨架屏 -->
        <template v-if="statsLoading && !refreshLoading">
          <a-skeleton
            active
            :paragraph="{ rows: 0 }"
            :title="{ width: 280 }"
            style="padding: 4px 0"
          />
        </template>
        <!-- 错误态 -->
        <template v-else-if="statsError">
          <a-alert
            type="warning"
            message="统计数据加载失败"
            show-icon
            closable
            :style="{ marginBottom: 0, padding: '4px 12px' }"
          >
            <template #action>
              <a-button size="small" v-permission="'sale:order:manualrefresh'" @click="handleManualRefresh">重试</a-button>
            </template>
          </a-alert>
        </template>
        <!-- 正常态 -->
        <template v-else>
          <div class="stat-card stat-blue">
            <div class="stat-card-icon"><FileTextOutlined /></div>
            <div class="stat-card-content">
              <div class="stat-card-title">本月订单</div>
              <div class="stat-card-value">{{ stats.monthOrderCount }}</div>
            </div>
          </div>
          <div class="stat-card stat-green">
            <div class="stat-card-icon"><DollarOutlined /></div>
            <div class="stat-card-content">
              <div class="stat-card-title">本月金额</div>
              <div class="stat-card-value">¥{{ formatCurrency(stats.monthAmount) }}</div>
            </div>
          </div>
          <div class="stat-card stat-orange">
            <div class="stat-card-icon"><ClockCircleOutlined /></div>
            <div class="stat-card-content">
              <div class="stat-card-title">待审批</div>
              <div class="stat-card-value">{{ stats.pendingCount }}</div>
            </div>
          </div>
          <span v-if="lastStatsUpdate" class="stats-update-time" :title="`最后更新: ${dayjs(lastStatsUpdate).format('YYYY-MM-DD HH:mm:ss')}`">
            更新 {{ dayjs(lastStatsUpdate).format('HH:mm') }}
          </span>
        </template>
        <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
          <SyncOutlined /> {{ autoRefreshCountdown }}s
        </span>
          <a-button size="small" :loading="refreshLoading" v-permission="'sale:order:manualrefresh'" @click="debounceClick('refresh', handleManualRefresh)">刷新</a-button>
      </div>
    </div>

    <!-- 首次使用引导 -->
    <a-alert
      v-if="showFirstTimeGuide"
      type="info"
      show-icon
      closable
      class="first-time-guide"
    >
      <template #icon><InboxOutlined /></template>
      <template #message>
        <span class="first-time-title">欢迎使用销售管理</span>
      </template>
      <template #description>
        <p class="first-time-desc">当前暂无销售数据。您可以通过「新建订单」开始第一笔销售业务。</p>
      </template>
      <template #action>
        <a-button size="small" type="primary" v-permission="'sale:order:firsttimecreate'" @click="handleFirstTimeCreate">新建订单</a-button>
      </template>
    </a-alert>

    <!-- Tab 导航（根据权限过滤） -->
    <div class="sale-module-tabs-wrapper" :class="{ 'sale-module-tabs-wrapper--no-guide': !showFirstTimeGuide }">
      <a-tabs
        v-model:activeKey="activeTab"
        class="sale-module-tabs"
        type="card"
        animated
        @change="handleTabChange"
      >
        <a-tab-pane
          v-for="tab in visibleTabs"
          :key="tab.key"
          :tab="tab.label"
          :force-render="true"
        />
      </a-tabs>

      <!-- Tab 内容区（KeepAlive 缓存） -->
      <div class="tab-content-area">
        <KeepAlive>
          <ErrorBoundary>
            <OrdersTab v-if="activeTab === 'orders'" />
            <QuotationTab v-else-if="activeTab === 'quotation'" />
            <OutboundTab v-else-if="activeTab === 'outbound'" />
            <ReturnTab v-else-if="activeTab === 'return'" />
            <ExchangeTab v-else-if="activeTab === 'exchange'" />
            <ReceiptTab v-else-if="activeTab === 'receipt'" />
            <CustomersTab v-else-if="activeTab === 'customers'" />
          </ErrorBoundary>
        </KeepAlive>
      </div>
    </div>

    <!-- 快捷键提示条 -->
    <div class="sale-module-footer-hint">
      <a-space size="middle">
        <span><kbd>Ctrl+N</kbd> 新建</span>
        <span><kbd>Ctrl+F</kbd> 筛选</span>
        <span><kbd>F5</kbd> 刷新</span>
      </a-space>
    </div>
  </div>
  </ErrorBoundary>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import dayjs from 'dayjs'
import {
  FileTextOutlined, DollarOutlined, ClockCircleOutlined, SyncOutlined, InboxOutlined
} from '@ant-design/icons-vue'
import OrdersTab from './tabs/Orders.vue'
import QuotationTab from './tabs/Quotation.vue'
import OutboundTab from './tabs/Outbound.vue'
import ReturnTab from './tabs/Return.vue'
import ExchangeTab from './tabs/Exchange.vue'
import ReceiptTab from './tabs/Receipt.vue'
import CustomersTab from './tabs/Customers.vue'
import { saleStatsApi, type SaleStats } from '@/api/erp'
import { hasPermission } from '@/utils/permission'
import { useIntervalRefresh } from '@/composables/useIntervalRefresh'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

const router = useRouter()
const route = useRoute()

// Tab 定义（集中管理）
interface TabItem {
  key: string
  label: string
  permission: string
  component: any
}

const allTabs: TabItem[] = [
  { key: 'orders', label: '销售订单', permission: 'sale:order:list', component: OrdersTab },
  { key: 'quotation', label: '报价单', permission: 'sale:quotation:list', component: QuotationTab },
  { key: 'outbound', label: '出库单', permission: 'sale:outbound:list', component: OutboundTab },
  { key: 'return', label: '退货单', permission: 'sale:return:list', component: ReturnTab },
  { key: 'exchange', label: '换货单', permission: 'sale:exchange:list', component: ExchangeTab },
  { key: 'receipt', label: '收款单', permission: 'sale:receipt:list', component: ReceiptTab },
  { key: 'customers', label: '客户', permission: 'crm:customer:list', component: CustomersTab },
]

const allTabKeys = allTabs.map(t => t.key) as string[]

// 根据权限过滤 Tab
const visibleTabs = computed(() => allTabs.filter(t => !t.permission || hasPermission(t.permission)))

const activeTab = ref<string>('orders')

const statsLoading = ref(false)
const statsError = ref(false)
const refreshLoading = ref(false)
const lastStatsUpdate = ref<string | null>(null)
const autoRefreshCountdown = ref(0)
const stats = reactive<SaleStats>({
  monthOrderCount: 0,
  monthAmount: 0,
  pendingCount: 0
})

// 首次使用引导：统计数据全部为零且非加载中且非错误
const showFirstTimeGuide = computed(() => {
  if (statsLoading.value || statsError.value) return false
  return stats.monthOrderCount === 0 && stats.monthAmount === 0 && lastStatsUpdate.value !== null
})

async function fetchStats() {
  if (statsLoading.value && !refreshLoading.value) return
  statsLoading.value = true
  statsError.value = false
  try {
    const res = await saleStatsApi.get()
    const data = res.data
    stats.monthOrderCount = data?.monthOrderCount ?? 0
    stats.monthAmount = data?.monthAmount ?? 0
    stats.pendingCount = data?.pendingCount ?? 0
  } catch (err) {
    console.warn("[销售管理] 加载统计失败", err);
    statsError.value = true
  } finally {
    statsLoading.value = false
    refreshLoading.value = false
    lastStatsUpdate.value = new Date().toISOString()
  }
}

async function handleManualRefresh() {
  refreshLoading.value = true
  await fetchStats()
  window.dispatchEvent(new CustomEvent('sale:refresh'))
}

function formatCurrency(value: number) {
  if (value == null) return '¥0.00'
  return `¥${value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function handleTabChange(key: string) {
  router.replace({ query: { ...route.query, tab: key } })
}

function handlePopState() {
  const tabFromQuery = route.query.tab as string
  if (tabFromQuery && allTabKeys.includes(tabFromQuery)) {
    activeTab.value = tabFromQuery
  }
}

function handleFirstTimeCreate() {
  // 切换到订单 tab 并触发新建
  activeTab.value = 'orders'
  window.dispatchEvent(new CustomEvent('sale:create'))
}

// 30s 自动刷新统计
let countdownTimer: ReturnType<typeof setInterval> | null = null
const { start: startStatsRefresh, stop: stopStatsRefresh } = useIntervalRefresh(() => {
  fetchStats()
  autoRefreshCountdown.value = 30
}, 30000)

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    debounceClick('refresh', handleManualRefresh)
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n' && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    window.dispatchEvent(new CustomEvent('sale:create'))
  }
}

function initActiveTab() {
  const tabFromQuery = route.query.tab as string
  const firstVisible = visibleTabs.value[0]
  if (tabFromQuery && allTabKeys.includes(tabFromQuery)) {
    activeTab.value = tabFromQuery
  } else if (firstVisible) {
    activeTab.value = firstVisible.key
  }
}

onMounted(() => {
  initActiveTab()
  fetchStats()
  autoRefreshCountdown.value = 30
  startStatsRefresh()
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)

  window.addEventListener('popstate', handlePopState)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  stopStatsRefresh()
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
  window.removeEventListener('popstate', handlePopState)
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.sale-module {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: var(--color-bg-layout, #f0f2f5);
}

.sale-module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px 24px;
  background-color: var(--color-bg-container, #fff);
  border-bottom: 1px solid var(--color-border-secondary, #e8e8e8);
}

.sale-module-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sale-breadcrumb {
  font-size: 13px;
}
.sale-breadcrumb :deep(li) {
  font-size: 13px;
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

/* 首次使用引导 */
.first-time-guide {
  margin: 16px 16px 0;
}
.first-time-title {
  font-weight: 600;
  font-size: 14px;
}
.first-time-desc {
  margin: 4px 0 0;
  font-size: 13px;
  color: #606266;
}

.sale-module-title {
  font-size: 20px;
  font-weight: 500;
  color: var(--color-text-primary, #303133);
  margin: 0;
  white-space: nowrap;
}

.sale-stat-cards {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

/* 统计卡片 */
.stat-card {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 6px;
  min-width: 100px;
}

.stat-blue { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-green { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-orange { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }

.stat-card-icon {
  font-size: 20px;
  margin-right: 8px;
  color: rgba(0, 0, 0, 0.45);
}

.stat-card-content {
  flex: 1;
}

.stat-card-title {
  font-size: 12px;
  color: #666;
}

.stat-card-value {
  font-size: 16px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stats-update-time {
  font-size: 12px;
  color: #bbb;
  white-space: nowrap;
  cursor: help;
}

/* Tab 容器 */
.sale-module-tabs-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: var(--color-bg-container, #fff);
  margin: 16px;
  border-radius: var(--border-radius-lg, 8px);
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.03), 0 1px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px 0 rgba(0, 0, 0, 0.02);
  overflow: hidden;
}

.sale-module-tabs-wrapper--no-guide {
  /* 无引导时样式不变 */
}

.sale-module-tabs {
  flex-shrink: 0;
}

:deep(.sale-module-tabs .ant-tabs-nav) {
  margin-bottom: 0;
  padding: 0 24px;
  background-color: var(--color-bg-container, #fff);
}

:deep(.sale-module-tabs.ant-tabs-card > .ant-tabs-nav .ant-tabs-tab) {
  border-bottom: none;
  padding: 8px 16px;
  transition: all 0.2s;
}

:deep(.sale-module-tabs.ant-tabs-card > .ant-tabs-nav .ant-tabs-tab-active) {
  background-color: var(--color-primary-1, #e6f4ff);
  border-bottom-color: transparent;
}

:deep(.sale-module-tabs.ant-tabs-card > .ant-tabs-nav::before) {
  border-bottom: 1px solid var(--color-border, #f0f0f0);
}

/* Tab 内容区 */
.tab-content-area {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.tab-content-area > :deep(*) {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* 快捷键提示条 */
.sale-module-footer-hint {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding: 4px 24px;
  background: var(--color-bg-container, #fff);
  border-top: 1px solid var(--color-border-secondary, #e8e8e8);
  font-size: 12px;
  color: #999;
}

.sale-module-footer-hint kbd {
  display: inline-block;
  padding: 1px 5px;
  font-size: 11px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  line-height: 1.4;
  color: #555;
  background-color: #f7f7f7;
  border: 1px solid #ccc;
  border-radius: 3px;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.2);
}

/* 响应式：小屏调整 */
@media (max-width: 768px) {
  .sale-module-footer-hint {
    display: none;
  }
  .sale-module-tabs-wrapper {
    margin: 8px;
    border-radius: 6px;
  }
  .sale-module-header {
    padding: 12px 16px;
  }
  .sale-stat-cards {
    gap: 8px;
  }
  .stat-card {
    padding: 6px 10px;
    min-width: 80px;
  }
  .stat-card-icon {
    font-size: 16px;
  }
  .stat-card-value {
    font-size: 14px;
  }
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

</style>
