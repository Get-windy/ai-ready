<template>
  <PageContainer full-height>
    <template #header>
      <div class="purchase-header">
        <div class="purchase-header-left">
          <a-breadcrumb class="purchase-breadcrumb">
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>采购管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="purchase-header-title">采购管理</h2>
        </div>
        <div class="purchase-header-right">
          <span v-if="lastUpdateTime" class="update-time">
            更新于 {{ lastUpdateTime }}
          </span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="handleRefresh">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <div class="purchase-tabs-wrapper">
      <!-- 统计卡片 -->
      <template v-if="loading && !refreshLoading">
        <div class="stat-cards" style="margin-bottom: 16px;">
          <a-card v-for="i in 4" :key="i" :bordered="false" class="stat-skeleton">
            <a-skeleton active :paragraph="{ rows: 1 }" :title="{ width: '60%' }" style="min-height: 80px;" />
          </a-card>
        </div>
      </template>
      <template v-else-if="statsError">
        <a-alert type="warning" message="统计数据加载失败" show-icon closable style="margin-bottom: 16px;">
          <template #action>
            <a-button size="small" @click="handleRefresh">重试</a-button>
          </template>
        </a-alert>
      </template>
      <div v-else class="stat-cards" style="margin-bottom: 16px;">
        <div class="stat-card stat-blue">
          <div class="stat-card-icon"><FileTextOutlined /></div>
          <div class="stat-card-content">
            <div class="stat-card-title">采购订单</div>
            <div class="stat-card-value">{{ orderCount }}</div>
          </div>
        </div>
        <div class="stat-card stat-orange">
          <div class="stat-card-icon"><SearchOutlined /></div>
          <div class="stat-card-content">
            <div class="stat-card-title">待询价</div>
            <div class="stat-card-value">{{ inquiryCount }}</div>
          </div>
        </div>
        <div class="stat-card stat-green">
          <div class="stat-card-icon"><ImportOutlined /></div>
          <div class="stat-card-content">
            <div class="stat-card-title">待入库</div>
            <div class="stat-card-value">{{ inboundCount }}</div>
          </div>
        </div>
        <div class="stat-card stat-purple">
          <div class="stat-card-icon"><DollarOutlined /></div>
          <div class="stat-card-content">
            <div class="stat-card-title">待付款</div>
            <div class="stat-card-value">{{ paymentCount }}</div>
          </div>
        </div>
      </div>

      <a-tabs
        v-model:activeKey="activeTab"
        class="purchase-tabs"
        type="card"
        animated
        @change="handleTabChange"
      >
        <a-tab-pane v-for="tab in visibleTabs" :key="tab.key">
          <template #tab>
            <span v-if="tab.key === 'orders'"><FileTextOutlined /> 采购订单<a-badge :count="orderCount" :overflow-count="99" :number-style="{ backgroundColor: '#1890ff' }" style="margin-left: 8px" /></span>
            <span v-else-if="tab.key === 'inquiry'"><SearchOutlined /> 询价单<a-badge :count="inquiryCount" :overflow-count="99" :number-style="{ backgroundColor: '#faad14' }" style="margin-left: 8px" /></span>
            <span v-else-if="tab.key === 'inbound'"><ImportOutlined /> 入库单<a-badge :count="inboundCount" :overflow-count="99" :number-style="{ backgroundColor: '#52c41a' }" style="margin-left: 8px" /></span>
            <span v-else-if="tab.key === 'return'"><RollbackOutlined /> 退货单</span>
            <span v-else-if="tab.key === 'exchange'"><SwapOutlined /> 换货单</span>
            <span v-else-if="tab.key === 'payment'"><DollarOutlined /> 付款单</span>
            <span v-else-if="tab.key === 'suppliers'"><TeamOutlined /> 供应商</span>
          </template>
        </a-tab-pane>
      </a-tabs>

      <!-- Tab 内容区（KeepAlive 缓存） -->
      <div class="tab-content-area">
        <ErrorBoundary @reset="handleRefresh">
          <KeepAlive>
            <OrdersTab v-if="activeTab === 'orders'" ref="ordersRef" />
            <InquiryTab v-else-if="activeTab === 'inquiry'" ref="inquiryRef" />
            <InboundTab v-else-if="activeTab === 'inbound'" ref="inboundRef" />
            <ReturnTab v-else-if="activeTab === 'return'" ref="returnRef" />
            <ExchangeTab v-else-if="activeTab === 'exchange'" ref="exchangeRef" />
            <PaymentTab v-else-if="activeTab === 'payment'" ref="paymentRef" />
            <SuppliersTab v-else-if="activeTab === 'suppliers'" ref="suppliersRef" />
          </KeepAlive>
        </ErrorBoundary>
      </div>
    </div>

    <!-- 快捷键提示条 -->
    <div class="purchase-footer-hint">
      <a-space size="middle">
        <span><kbd>Ctrl+N</kbd> 新建</span>
        <span><kbd>Ctrl+F</kbd> 筛选</span>
        <span><kbd>F5</kbd> 刷新</span>
      </a-space>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import dayjs from 'dayjs'
import { PageContainer } from '@/components'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import OrdersTab from './tabs/Orders.vue'
import InquiryTab from './tabs/Inquiry.vue'
import InboundTab from './tabs/Inbound.vue'
import ReturnTab from './tabs/Return.vue'
import ExchangeTab from './tabs/Exchange.vue'
import PaymentTab from './tabs/Payment.vue'
import SuppliersTab from './tabs/Suppliers.vue'
import {
  ReloadOutlined, FileTextOutlined, SearchOutlined,
  ImportOutlined, RollbackOutlined, SwapOutlined,
  DollarOutlined, TeamOutlined, SyncOutlined
} from '@ant-design/icons-vue'
import { purchaseStatsApi } from '@/api/erp'
import { hasPermission } from '@/utils/permission'

const router = useRouter()
const route = useRoute()

const VALID_TABS = ['orders', 'inquiry', 'inbound', 'return', 'exchange', 'payment', 'suppliers'] as const
const activeTab = ref<string>('orders')
const loading = ref(false)
const statsError = ref(false)
const refreshLoading = ref(false)
const lastUpdateTime = ref<string>('')
const autoRefreshCountdown = ref(0)

// Tab 引用
const ordersRef = ref()
const inquiryRef = ref()
const inboundRef = ref()
const returnRef = ref()
const exchangeRef = ref()
const paymentRef = ref()
const suppliersRef = ref()

// Tab 统计数据（从 API 获取）
const orderCount = ref(0)
const inquiryCount = ref(0)
const inboundCount = ref(0)
const paymentCount = ref(0)

// Tab 权限过滤
interface TabDef { key: string; label: string; permission: string }
const allTabs: TabDef[] = [
  { key: 'orders', label: '采购订单', permission: 'purchase:order:list' },
  { key: 'inquiry', label: '询价单', permission: 'purchase:inquiry:list' },
  { key: 'inbound', label: '入库单', permission: 'purchase:inbound:list' },
  { key: 'return', label: '退货单', permission: 'purchase:return:list' },
  { key: 'exchange', label: '换货单', permission: 'purchase:exchange:list' },
  { key: 'payment', label: '付款单', permission: 'purchase:payment:list' },
  { key: 'suppliers', label: '供应商', permission: 'supplier:list' },
]
const visibleTabs = computed(() => allTabs.filter(t => !t.permission || hasPermission(t.permission)))

/** 加载采购统计 */
async function fetchStats() {
  if (loading.value && !refreshLoading.value) return
  loading.value = true
  statsError.value = false
  try {
    const res = await purchaseStatsApi.get()
    const data = res.data
    orderCount.value = data?.totalOrders ?? data?.monthOrderCount ?? 0
    inquiryCount.value = data?.pendingInquiryCount ?? 0
    inboundCount.value = data?.pendingInboundCount ?? 0
    paymentCount.value = data?.pendingPaymentCount ?? 0
  } catch (err) {
    statsError.value = true
    console.warn('[采购] 加载统计数据失败', err)
    message.error('加载统计数据失败')
  } finally {
    loading.value = false
    refreshLoading.value = false
    lastUpdateTime.value = dayjs().format('HH:mm:ss')
  }
}

function handleTabChange(key: string) {
  router.replace({ query: { ...route.query, tab: key } })
  // 切换 Tab 时更新数据时间戳
  lastUpdateTime.value = dayjs().format('HH:mm:ss')
}

function handleRefresh() {
  refreshLoading.value = true
  // 刷新当前 Tab
  const refMap: Record<string, any> = {
    orders: ordersRef.value,
    inquiry: inquiryRef.value,
    inbound: inboundRef.value,
    return: returnRef.value,
    exchange: exchangeRef.value,
    payment: paymentRef.value,
    suppliers: suppliersRef.value
  }
  const currentRef = refMap[activeTab.value]
  if (currentRef?.handleQuery) {
    currentRef.handleQuery()
  }
  fetchStats()
}

function handlePopState() {
  const tabFromQuery = route.query.tab as string
  if (tabFromQuery && (VALID_TABS as readonly string[]).includes(tabFromQuery)) {
    activeTab.value = tabFromQuery
  }
}

function handleKeydown(e: KeyboardEvent) {
  // F5 刷新
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    handleRefresh()
  }
  // Ctrl+N 新建
  if ((e.ctrlKey || e.metaKey) && e.key === 'n' && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    window.dispatchEvent(new CustomEvent('purchase:create'))
  }
  // Ctrl+F 筛选
  if ((e.ctrlKey || e.metaKey) && e.key === 'f' && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    window.dispatchEvent(new CustomEvent('purchase:filter'))
  }
}

function initActiveTab() {
  const tabFromQuery = route.query.tab as string
  if (tabFromQuery && (VALID_TABS as readonly string[]).includes(tabFromQuery)) {
    activeTab.value = tabFromQuery
  }
}

// 自动刷新
let autoRefreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  initActiveTab()
  fetchStats()
  autoRefreshCountdown.value = 30
  autoRefreshTimer = setInterval(() => {
    fetchStats()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)

  window.addEventListener('popstate', handlePopState)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (autoRefreshTimer) { clearInterval(autoRefreshTimer) }
  if (countdownTimer) { clearInterval(countdownTimer) }
  window.removeEventListener('popstate', handlePopState)
  document.removeEventListener('keydown', handleKeydown)
})

defineExpose({ handleQuery: handleRefresh })
</script>

<style scoped>
.purchase-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.purchase-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.purchase-breadcrumb {
  font-size: 13px;
}
.purchase-breadcrumb :deep(li) {
  font-size: 13px;
}
.purchase-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.purchase-header-right {
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

.purchase-tabs-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.03), 0 1px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px 0 rgba(0, 0, 0, 0.02);
  overflow: hidden;
  padding: 16px;
  height: 100%;
}

/* 统计卡片骨架 */
.stat-skeleton {
  flex: 1;
  border-radius: 8px;
}
.stat-skeleton :deep(.ant-card-body) {
  padding: 12px 16px;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  flex-shrink: 0;
}

.stat-card {
  flex: 1;
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-radius: 8px;
  min-width: 150px;
}

.stat-blue { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-green { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-orange { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-purple { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-icon {
  font-size: 24px;
  margin-right: 12px;
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
  font-size: 18px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.purchase-tabs {
  flex-shrink: 0;
}

:deep(.purchase-tabs .ant-tabs-nav) {
  margin-bottom: 0;
  padding: 0 24px;
  background-color: #fff;
}

:deep(.purchase-tabs.ant-tabs-card > .ant-tabs-nav .ant-tabs-tab) {
  border-bottom: none;
  padding: 4px 12px;
  font-size: 12px;
  line-height: 1.4;
  height: 32px;
  transition: all 0.2s;
}

:deep(.purchase-tabs.ant-tabs-card > .ant-tabs-nav .ant-tabs-tab .ant-badge) {
  font-size: 11px;
}

:deep(.purchase-tabs.ant-tabs-card > .ant-tabs-nav .ant-tabs-tab-active) {
  background-color: #e6f4ff;
  border-bottom-color: transparent;
}

:deep(.purchase-tabs.ant-tabs-card > .ant-tabs-nav::before) {
  border-bottom: 1px solid #f0f0f0;
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
.purchase-footer-hint {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding: 4px 24px;
  background: #fff;
  border-top: 1px solid #e8e8e8;
  font-size: 12px;
  color: #999;
}

.purchase-footer-hint kbd {
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

/* 响应式 */
@media (max-width: 768px) {
  .purchase-footer-hint {
    display: none;
  }
  .purchase-tabs-wrapper {
    padding: 8px;
    border-radius: 6px;
  }
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 45%;
  }
}
</style>