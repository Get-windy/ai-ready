<template>
  <PageContainer title="采购管理" full-height>
    <template #headerExtra>
      <a-space :size="12">
        <span class="data-status">
          <a-badge :status="loading ? 'processing' : 'success'" />
          <span v-if="lastUpdateTime" class="update-time">
            数据更新: {{ lastUpdateTime }}
          </span>
        </span>
        <a-button size="small" @click="handleRefresh">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </template>

    <div class="purchase-tabs-wrapper">
      <!-- 统计卡片 -->
      <div class="stat-cards" style="margin-bottom: 16px;">
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
        <a-tab-pane key="orders">
          <template #tab>
            <span><FileTextOutlined /> 采购订单</span>
            <a-badge :count="orderCount" :overflow-count="99" :number-style="{ backgroundColor: '#1890ff' }" style="margin-left: 8px" />
          </template>
        </a-tab-pane>
        <a-tab-pane key="inquiry">
          <template #tab>
            <span><SearchOutlined /> 询价单</span>
            <a-badge :count="inquiryCount" :overflow-count="99" :number-style="{ backgroundColor: '#faad14' }" style="margin-left: 8px" />
          </template>
        </a-tab-pane>
        <a-tab-pane key="inbound">
          <template #tab>
            <span><ImportOutlined /> 入库单</span>
            <a-badge :count="inboundCount" :overflow-count="99" :number-style="{ backgroundColor: '#52c41a' }" style="margin-left: 8px" />
          </template>
        </a-tab-pane>
        <a-tab-pane key="return">
          <template #tab>
            <span><RollbackOutlined /> 退货单</span>
          </template>
        </a-tab-pane>
        <a-tab-pane key="exchange">
          <template #tab>
            <span><SwapOutlined /> 换货单</span>
          </template>
        </a-tab-pane>
        <a-tab-pane key="payment">
          <template #tab>
            <span><DollarOutlined /> 付款单</span>
          </template>
        </a-tab-pane>
        <a-tab-pane key="suppliers">
          <template #tab>
            <span><TeamOutlined /> 供应商</span>
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
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
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
  ReloadOutlined,
  FileTextOutlined,
  SearchOutlined,
  ImportOutlined,
  RollbackOutlined,
  SwapOutlined,
  DollarOutlined,
  TeamOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()

const VALID_TABS = ['orders', 'inquiry', 'inbound', 'return', 'exchange', 'payment', 'suppliers'] as const
const activeTab = ref<string>('orders')
const loading = ref(false)
const lastUpdateTime = ref<string>('')

// Tab 引用
const ordersRef = ref()
const inquiryRef = ref()
const inboundRef = ref()
const returnRef = ref()
const exchangeRef = ref()
const paymentRef = ref()
const suppliersRef = ref()

// Tab 统计数据
const orderCount = ref(15)
const inquiryCount = ref(8)
const inboundCount = ref(12)
const paymentCount = ref(5)

function handleTabChange(key: string) {
  router.replace({ query: { ...route.query, tab: key } })
  lastUpdateTime.value = ''
}

function handleRefresh() {
  lastUpdateTime.value = ''
  loading.value = true

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

  setTimeout(() => {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  }, 500)
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

onMounted(() => {
  initActiveTab()
  lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  window.addEventListener('popstate', handlePopState)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('popstate', handlePopState)
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
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
  padding: 8px 16px;
  transition: all 0.2s;
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