<template>
  <PageContainer title="库存管理" full-height>
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

    <div class="stock-module">
      <!-- 统计卡片 -->
      <div class="stat-cards" style="margin-bottom: 16px;">
        <div class="stat-card stat-blue">
          <div class="stat-card-icon"><DatabaseOutlined /></div>
          <div class="stat-card-content">
            <div class="stat-card-title">库存总量</div>
            <div class="stat-card-value">{{ stockCount }}</div>
          </div>
        </div>
        <div class="stat-card stat-green">
          <div class="stat-card-icon"><LoginOutlined /></div>
          <div class="stat-card-content">
            <div class="stat-card-title">入库单</div>
            <div class="stat-card-value">{{ inboundCount }}</div>
          </div>
        </div>
        <div class="stat-card stat-orange">
          <div class="stat-card-icon"><LogoutOutlined /></div>
          <div class="stat-card-content">
            <div class="stat-card-title">出库单</div>
            <div class="stat-card-value">{{ outboundCount }}</div>
          </div>
        </div>
        <div class="stat-card stat-purple">
          <div class="stat-card-icon"><TagOutlined /></div>
          <div class="stat-card-content">
            <div class="stat-card-title">批次数</div>
            <div class="stat-card-value">{{ batchCount }}</div>
          </div>
        </div>
      </div>

      <a-tabs
        v-model:activeKey="activeTab"
        class="stock-module-tabs"
        type="card"
        animated
        @change="handleTabChange"
      >
        <a-tab-pane key="stock">
          <template #tab>
            <span><DatabaseOutlined /> 库存查询</span>
            <a-badge :count="stockCount" :overflow-count="99" :number-style="{ backgroundColor: '#1890ff' }" style="margin-left: 8px" />
          </template>
        </a-tab-pane>
        <a-tab-pane key="inbound">
          <template #tab>
            <span><LoginOutlined /> 入库管理</span>
            <a-badge :count="inboundCount" :overflow-count="99" :number-style="{ backgroundColor: '#52c41a' }" style="margin-left: 8px" />
          </template>
        </a-tab-pane>
        <a-tab-pane key="outbound">
          <template #tab>
            <span><LogoutOutlined /> 出库管理</span>
            <a-badge :count="outboundCount" :overflow-count="99" :number-style="{ backgroundColor: '#faad14' }" style="margin-left: 8px" />
          </template>
        </a-tab-pane>
        <a-tab-pane key="check">
          <template #tab>
            <span><CheckSquareOutlined /> 库存盘点</span>
          </template>
        </a-tab-pane>
        <a-tab-pane key="transfer">
          <template #tab>
            <span><SwapOutlined /> 库存调拨</span>
          </template>
        </a-tab-pane>
        <a-tab-pane key="batch">
          <template #tab>
            <span><TagOutlined /> 批次管理</span>
            <a-badge :count="batchCount" :overflow-count="99" :number-style="{ backgroundColor: '#722ed1' }" style="margin-left: 8px" />
          </template>
        </a-tab-pane>
      </a-tabs>

      <!-- Tab 内容区（KeepAlive 缓存） -->
      <div class="tab-content-area">
        <ErrorBoundary @reset="handleRefresh">
          <KeepAlive>
            <StockTab v-if="activeTab === 'stock'" ref="stockRef" @update-count="updateStockCount" />
            <InboundTab v-else-if="activeTab === 'inbound'" ref="inboundRef" @update-count="updateInboundCount" />
            <OutboundTab v-else-if="activeTab === 'outbound'" ref="outboundRef" @update-count="updateOutboundCount" />
            <CheckTab v-else-if="activeTab === 'check'" ref="checkRef" />
            <TransferTab v-else-if="activeTab === 'transfer'" ref="transferRef" />
            <BatchTab v-else-if="activeTab === 'batch'" ref="batchRef" @update-count="updateBatchCount" />
          </KeepAlive>
        </ErrorBoundary>
      </div>
    </div>

    <!-- 快捷键提示条 -->
    <div class="footer-hint">
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
import StockTab from './tabs/Stock.vue'
import InboundTab from './tabs/Inbound.vue'
import OutboundTab from './tabs/Outbound.vue'
import CheckTab from './tabs/Check.vue'
import TransferTab from './tabs/Transfer.vue'
import BatchTab from './tabs/Batch.vue'
import {
  ReloadOutlined, DatabaseOutlined, LoginOutlined, LogoutOutlined,
  CheckSquareOutlined, SwapOutlined, TagOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()

const VALID_TABS = ['stock', 'inbound', 'outbound', 'check', 'transfer', 'batch'] as const
const activeTab = ref<string>('stock')
const loading = ref(false)
const lastUpdateTime = ref('')

// Tab 引用
const stockRef = ref()
const inboundRef = ref()
const outboundRef = ref()
const checkRef = ref()
const transferRef = ref()
const batchRef = ref()

// Tab 统计数据
const stockCount = ref(0)
const inboundCount = ref(0)
const outboundCount = ref(0)
const batchCount = ref(0)

function handleTabChange(key: string) {
  router.replace({ query: { ...route.query, tab: key } })
  lastUpdateTime.value = ''
}

function handleRefresh() {
  lastUpdateTime.value = ''
  loading.value = true

  // 刷新当前 Tab
  const refMap: Record<string, any> = {
    stock: stockRef.value,
    inbound: inboundRef.value,
    outbound: outboundRef.value,
    check: checkRef.value,
    transfer: transferRef.value,
    batch: batchRef.value
  }

  const currentRef = refMap[activeTab.value]
  if (currentRef?.fetchData) {
    currentRef.fetchData()
  }

  setTimeout(() => {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  }, 500)
}

function updateStockCount(count: number) {
  stockCount.value = count
}

function updateInboundCount(count: number) {
  inboundCount.value = count
}

function updateOutboundCount(count: number) {
  outboundCount.value = count
}

function updateBatchCount(count: number) {
  batchCount.value = count
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
    window.dispatchEvent(new CustomEvent('stock:create'))
  }
  // Ctrl+F 筛选
  if ((e.ctrlKey || e.metaKey) && e.key === 'f' && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    window.dispatchEvent(new CustomEvent('stock:filter'))
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

.stock-module {
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

.stock-module-tabs {
  flex-shrink: 0;
}

:deep(.stock-module-tabs .ant-tabs-nav) {
  margin-bottom: 0;
  padding: 0 24px;
  background-color: #fff;
}

:deep(.stock-module-tabs.ant-tabs-card > .ant-tabs-nav .ant-tabs-tab) {
  border-bottom: none;
  padding: 8px 16px;
  transition: all 0.2s;
}

:deep(.stock-module-tabs.ant-tabs-card > .ant-tabs-nav .ant-tabs-tab-active) {
  background-color: #e6f4ff;
  border-bottom-color: transparent;
}

:deep(.stock-module-tabs.ant-tabs-card > .ant-tabs-nav::before) {
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
.footer-hint {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding: 4px 24px;
  background: #fff;
  border-top: 1px solid #e8e8e8;
  font-size: 12px;
  color: #999;
}

.footer-hint kbd {
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
  .footer-hint {
    display: none;
  }
  .stock-module {
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