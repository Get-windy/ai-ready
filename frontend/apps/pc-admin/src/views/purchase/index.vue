<template>
  <div class="purchase-module">
    <div class="purchase-module-header">
      <div class="purchase-module-header-left">
        <h1 class="purchase-module-title">{{ route.meta?.title || '采购管理' }}</h1>
      </div>
    </div>

    <div class="purchase-module-tabs-wrapper">
      <a-tabs
        v-model:activeKey="activeTab"
        class="purchase-module-tabs"
        type="card"
        animated
        @change="handleTabChange"
      >
        <a-tab-pane key="orders" tab="采购订单" />
        <a-tab-pane key="inquiry" tab="询价单" />
        <a-tab-pane key="inbound" tab="入库单" />
        <a-tab-pane key="return" tab="退货单" />
        <a-tab-pane key="exchange" tab="换货单" />
        <a-tab-pane key="payment" tab="付款单" />
        <a-tab-pane key="suppliers" tab="供应商" />
      </a-tabs>

      <!-- Tab 内容区（KeepAlive 缓存） -->
      <div class="tab-content-area">
        <KeepAlive>
          <OrdersTab v-if="activeTab === 'orders'" />
          <InquiryTab v-else-if="activeTab === 'inquiry'" />
          <InboundTab v-else-if="activeTab === 'inbound'" />
          <ReturnTab v-else-if="activeTab === 'return'" />
          <ExchangeTab v-else-if="activeTab === 'exchange'" />
          <PaymentTab v-else-if="activeTab === 'payment'" />
          <SuppliersTab v-else-if="activeTab === 'suppliers'" />
        </KeepAlive>
      </div>
    </div>

    <!-- 快捷键提示条 -->
    <div class="purchase-module-footer-hint">
      <a-space size="middle">
        <span><kbd>Ctrl+N</kbd> 新建</span>
        <span><kbd>Ctrl+F</kbd> 筛选</span>
        <span><kbd>F5</kbd> 刷新</span>
      </a-space>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import OrdersTab from './tabs/Orders.vue'
import InquiryTab from './tabs/Inquiry.vue'
import InboundTab from './tabs/Inbound.vue'
import ReturnTab from './tabs/Return.vue'
import ExchangeTab from './tabs/Exchange.vue'
import PaymentTab from './tabs/Payment.vue'
import SuppliersTab from './tabs/Suppliers.vue'

const router = useRouter()
const route = useRoute()

const VALID_TABS = ['orders', 'inquiry', 'inbound', 'return', 'exchange', 'payment', 'suppliers'] as const
const activeTab = ref<string>('orders')

function handleTabChange(key: string) {
  router.replace({ query: { ...route.query, tab: key } })
}

function handlePopState() {
  const tabFromQuery = route.query.tab as string
  if (tabFromQuery && (VALID_TABS as readonly string[]).includes(tabFromQuery)) {
    activeTab.value = tabFromQuery
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    window.dispatchEvent(new CustomEvent('purchase:refresh'))
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
  window.addEventListener('popstate', handlePopState)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('popstate', handlePopState)
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.purchase-module {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: var(--color-bg-layout, #f0f2f5);
}

.purchase-module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: var(--color-bg-container, #fff);
  border-bottom: 1px solid var(--color-border-secondary, #e8e8e8);
}

.purchase-module-header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.purchase-module-title {
  font-size: 20px;
  font-weight: 500;
  color: var(--color-text-primary, #303133);
  margin: 0;
}

/* Tab 容器 */
.purchase-module-tabs-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: var(--color-bg-container, #fff);
  margin: 16px;
  border-radius: var(--border-radius-lg, 8px);
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.03), 0 1px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px 0 rgba(0, 0, 0, 0.02);
  overflow: hidden;
}

.purchase-module-tabs {
  flex-shrink: 0;
}

:deep(.purchase-module-tabs .ant-tabs-nav) {
  margin-bottom: 0;
  padding: 0 24px;
  background-color: var(--color-bg-container, #fff);
}

:deep(.purchase-module-tabs.ant-tabs-card > .ant-tabs-nav .ant-tabs-tab) {
  border-bottom: none;
  padding: 8px 16px;
  transition: all 0.2s;
}

:deep(.purchase-module-tabs.ant-tabs-card > .ant-tabs-nav .ant-tabs-tab-active) {
  background-color: var(--color-primary-1, #e6f4ff);
  border-bottom-color: transparent;
}

:deep(.purchase-module-tabs.ant-tabs-card > .ant-tabs-nav::before) {
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
.purchase-module-footer-hint {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding: 4px 24px;
  background: var(--color-bg-container, #fff);
  border-top: 1px solid var(--color-border-secondary, #e8e8e8);
  font-size: 12px;
  color: #999;
}

.purchase-module-footer-hint kbd {
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
  .purchase-module-footer-hint {
    display: none;
  }
  .purchase-module-tabs-wrapper {
    margin: 8px;
    border-radius: 6px;
  }
  .purchase-module-header {
    padding: 12px 16px;
  }
}
</style>
