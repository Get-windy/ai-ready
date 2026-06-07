<template>
  <div class="sale-module">
    <!-- 顶部统计栏 -->
    <div class="sale-module-header">
      <div class="sale-module-header-left">
        <h1 class="sale-module-title">{{ route.meta?.title || '销售管理' }}</h1>
      </div>
      <div class="sale-module-header-right">
        <!-- 加载态：骨架屏 -->
        <template v-if="statsLoading">
          <a-skeleton
            active
            :paragraph="{ rows: 0 }"
            :title="{ width: 280 }"
            style="padding: 4px 0"
          />
        </template>
        <!-- 错误态：Alert 横幅 -->
        <template v-else-if="statsError">
          <a-alert
            type="warning"
            message="统计数据加载失败"
            show-icon
            closable
            :style="{ marginBottom: 0, padding: '4px 12px' }"
          >
            <template #action>
              <a-button size="small" @click="fetchStats">重试</a-button>
            </template>
          </a-alert>
        </template>
        <!-- 正常态 -->
        <template v-else>
          <a-space size="middle" wrap>
            <a-statistic title="本月订单" :value="stats.monthOrderCount" />
            <a-divider type="vertical" />
            <a-statistic
              title="本月金额"
              :value="stats.monthAmount"
              :precision="2"
              :formatter="() => formatCurrency(stats.monthAmount)"
            />
            <a-divider type="vertical" />
            <a-statistic title="待审批" :value="stats.pendingCount">
              <template #suffix>
                <a-badge
                  v-if="stats.pendingCount > 0"
                  :count="stats.pendingCount > 99 ? '99+' : stats.pendingCount"
                  :number-style="{ backgroundColor: '#faad14', fontSize: '10px', minWidth: '16px', height: '16px', lineHeight: '16px' }"
                />
              </template>
            </a-statistic>
          </a-space>
          <span v-if="lastStatsUpdate" class="stats-update-time" :title="`最后更新: ${dayjs(lastStatsUpdate).format('YYYY-MM-DD HH:mm:ss')}`">
            更新 {{ dayjs(lastStatsUpdate).format('HH:mm') }}
          </span>
        </template>
      </div>
    </div>

    <!-- 首次使用引导 -->
    <a-alert
      v-if="showFirstTimeGuide"
      type="info"
      message="欢迎使用销售管理"
      description="当前暂无销售数据。您可以通过「新建订单」开始第一笔销售，或参考帮助文档了解更多功能。"
      show-icon
      closable
      :style="{ margin: '16px 16px 0' }"
    >
      <template #action>
        <a-button size="small" type="primary" @click="handleFirstTimeCreate">新建订单</a-button>
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
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import dayjs from 'dayjs'
import OrdersTab from './tabs/Orders.vue'
import QuotationTab from './tabs/Quotation.vue'
import OutboundTab from './tabs/Outbound.vue'
import ReturnTab from './tabs/Return.vue'
import ExchangeTab from './tabs/Exchange.vue'
import ReceiptTab from './tabs/Receipt.vue'
import CustomersTab from './tabs/Customers.vue'
import request from '@/utils/request'
import { useIntervalRefresh } from '@/composables/useIntervalRefresh'

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

// 显示所有 Tab（模块内权限由各 tab 内部自行控制）
const visibleTabs = computed(() => allTabs)

const activeTab = ref<string>('orders')

const statsLoading = ref(false)
const statsError = ref(false)
const lastStatsUpdate = ref<string | null>(null)
const stats = reactive({
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
  if (statsLoading.value) return
  statsLoading.value = true
  statsError.value = false
  try {
    const res = await request.get('/erp/sale/order/stats')
    const data = (res as any).data ?? res
    if (data) {
      stats.monthOrderCount = data.monthOrderCount ?? 0
      stats.monthAmount = data.monthAmount ?? 0
      stats.pendingCount = data.pendingCount ?? 0
    }
  } catch {
    statsError.value = true
  } finally {
    statsLoading.value = false
    lastStatsUpdate.value = new Date().toISOString()
  }
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
const { start: startStatsRefresh, stop: stopStatsRefresh } = useIntervalRefresh(fetchStats, 30000)

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    fetchStats()
    window.dispatchEvent(new CustomEvent('sale:refresh'))
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
  startStatsRefresh()

  window.addEventListener('popstate', handlePopState)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  stopStatsRefresh()
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
  padding: 16px 24px;
  background-color: var(--color-bg-container, #fff);
  border-bottom: 1px solid var(--color-border-secondary, #e8e8e8);
}

.sale-module-header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.sale-module-header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
  flex-wrap: wrap;
}

.sale-module-title {
  font-size: 20px;
  font-weight: 500;
  color: var(--color-text-primary, #303133);
  margin: 0;
  white-space: nowrap;
}

.stats-update-time {
  font-size: 12px;
  color: #bbb;
  white-space: nowrap;
  cursor: help;
  vertical-align: bottom;
  line-height: 40px;
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
}
</style>
