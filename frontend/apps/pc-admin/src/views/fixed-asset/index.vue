<template>
  <PageContainer title="固定资产管理" full-height>
    <template #headerExtra>
      <a-space :size="12">
        <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
          <SyncOutlined /> {{ autoRefreshCountdown }}s
        </span>
        <span class="data-status">
          <a-badge :status="loading ? 'processing' : 'success'" />
          <span v-if="lastUpdateTime" class="update-time">
            数据更新: {{ lastUpdateTime }}
          </span>
        </span>
        <a-button size="small" :loading="refreshLoading" @click="handleRefresh">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </template>

    <div class="fixed-asset-module">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-blue">
          <div class="stat-card-icon"><FileTextOutlined /></div>
          <div class="stat-card-content">
            <div class="stat-card-title">资产总数</div>
            <div class="stat-card-value">{{ assetCount }}</div>
          </div>
        </div>
        <div class="stat-card stat-green">
          <div class="stat-card-icon"><CalculatorOutlined /></div>
          <div class="stat-card-content">
            <div class="stat-card-title">待折旧</div>
            <div class="stat-card-value">{{ depreciationCount }}</div>
          </div>
        </div>
        <div class="stat-card stat-orange">
          <div class="stat-card-icon"><DeleteOutlined /></div>
          <div class="stat-card-content">
            <div class="stat-card-title">待处置</div>
            <div class="stat-card-value">{{ disposalCount }}</div>
          </div>
        </div>
        <div class="stat-card stat-purple">
          <div class="stat-card-icon"><CheckSquareOutlined /></div>
          <div class="stat-card-content">
            <div class="stat-card-title">待盘点</div>
            <div class="stat-card-value">{{ inventoryCount }}</div>
          </div>
        </div>
      </div>

      <a-tabs
        v-model:activeKey="activeKey"
        class="fixed-asset-tabs"
        type="card"
        animated
        @change="onTabChange"
      >
        <a-tab-pane key="asset">
          <template #tab>
            <span><FileTextOutlined /> 资产列表</span>
            <a-badge :count="assetCount" :overflow-count="99" :number-style="{ backgroundColor: '#1890ff' }" style="margin-left: 8px" />
          </template>
        </a-tab-pane>
        <a-tab-pane key="category">
          <template #tab>
            <span><FolderOutlined /> 分类管理</span>
          </template>
        </a-tab-pane>
        <a-tab-pane key="depreciation">
          <template #tab>
            <span><CalculatorOutlined /> 折旧管理</span>
            <a-badge :count="depreciationCount" :overflow-count="99" :number-style="{ backgroundColor: '#52c41a' }" style="margin-left: 8px" />
          </template>
        </a-tab-pane>
        <a-tab-pane key="transfer">
          <template #tab>
            <span><SwapOutlined /> 资产转移</span>
          </template>
        </a-tab-pane>
        <a-tab-pane key="disposal">
          <template #tab>
            <span><DeleteOutlined /> 资产处置</span>
            <a-badge :count="disposalCount" :overflow-count="99" :number-style="{ backgroundColor: '#faad14' }" style="margin-left: 8px" />
          </template>
        </a-tab-pane>
        <a-tab-pane key="inventory">
          <template #tab>
            <span><CheckSquareOutlined /> 资产盘点</span>
          </template>
        </a-tab-pane>
        <a-tab-pane key="report">
          <template #tab>
            <span><BarChartOutlined /> 统计报表</span>
          </template>
        </a-tab-pane>
      </a-tabs>

      <!-- Tab 内容区（KeepAlive 缓存） -->
      <div class="tab-content-area">
        <ErrorBoundary @reset="handleRefresh">
          <KeepAlive>
            <AssetList v-if="activeKey === 'asset'" ref="assetRef" @update-count="updateAssetCount" />
            <CategoryList v-else-if="activeKey === 'category'" ref="categoryRef" />
            <DepreciationList v-else-if="activeKey === 'depreciation'" ref="depreciationRef" @update-count="updateDepreciationCount" />
            <TransferList v-else-if="activeKey === 'transfer'" ref="transferRef" />
            <DisposalList v-else-if="activeKey === 'disposal'" ref="disposalRef" @update-count="updateDisposalCount" />
            <InventoryList v-else-if="activeKey === 'inventory'" ref="inventoryRef" />
            <ReportPage v-else-if="activeKey === 'report'" ref="reportRef" />
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
import AssetList from './asset/index.vue'
import CategoryList from './category/index.vue'
import DepreciationList from './depreciation/index.vue'
import TransferList from './transfer/index.vue'
import DisposalList from './disposal/index.vue'
import InventoryList from './inventory/index.vue'
import ReportPage from './report/index.vue'
import {
  ReloadOutlined, SyncOutlined, FileTextOutlined, FolderOutlined, CalculatorOutlined,
  SwapOutlined, DeleteOutlined, CheckSquareOutlined, BarChartOutlined
} from '@ant-design/icons-vue'

const router = useRouter()
const route = useRoute()

const VALID_TABS = ['asset', 'category', 'depreciation', 'transfer', 'disposal', 'inventory', 'report'] as const
const activeKey = ref<string>('asset')

let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null
const loading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)

// Tab 引用
const assetRef = ref()
const categoryRef = ref()
const depreciationRef = ref()
const transferRef = ref()
const disposalRef = ref()
const inventoryRef = ref()
const reportRef = ref()

// Tab 统计数据
const assetCount = ref(0)
const depreciationCount = ref(0)
const disposalCount = ref(0)
const inventoryCount = ref(0)

function onTabChange(key: string) {
  router.replace({ query: { ...route.query, tab: key } })
  lastUpdateTime.value = ''
}

function handleRefresh() {
  lastUpdateTime.value = ''
  refreshLoading.value = true
  loading.value = true

  // 刷新当前 Tab
  const refMap: Record<string, any> = {
    asset: assetRef.value,
    category: categoryRef.value,
    depreciation: depreciationRef.value,
    transfer: transferRef.value,
    disposal: disposalRef.value,
    inventory: inventoryRef.value,
    report: reportRef.value
  }

  const currentRef = refMap[activeKey.value]
  if (currentRef?.fetchData) {
    currentRef.fetchData()
  }

  setTimeout(() => {
    loading.value = false
    refreshLoading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  }, 500)
}

function updateAssetCount(count: number) {
  assetCount.value = count
}

function updateDepreciationCount(count: number) {
  depreciationCount.value = count
}

function updateDisposalCount(count: number) {
  disposalCount.value = count
}

function handlePopState() {
  const tabFromQuery = route.query.tab as string
  if (tabFromQuery && (VALID_TABS as readonly string[]).includes(tabFromQuery)) {
    activeKey.value = tabFromQuery
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
    window.dispatchEvent(new CustomEvent('fixed-asset:create'))
  }
  // Ctrl+F 筛选
  if ((e.ctrlKey || e.metaKey) && e.key === 'f' && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    window.dispatchEvent(new CustomEvent('fixed-asset:filter'))
  }
}

function initActiveTab() {
  const tabFromQuery = route.query.tab as string
  if (tabFromQuery && (VALID_TABS as readonly string[]).includes(tabFromQuery)) {
    activeKey.value = tabFromQuery
  }
}

onMounted(() => {
  initActiveTab()
  lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  window.addEventListener('popstate', handlePopState)
  document.addEventListener('keydown', handleKeydown)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    handleRefresh()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  window.removeEventListener('popstate', handlePopState)
  document.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

function fetchData() {
  handleRefresh()
}

defineExpose({ handleQuery: fetchData })
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

.fixed-asset-module {
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
  margin-bottom: 16px;
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

.fixed-asset-tabs {
  flex-shrink: 0;
}

:deep(.fixed-asset-tabs .ant-tabs-nav) {
  margin-bottom: 0;
  padding: 0 16px;
  background-color: #fff;
}

:deep(.fixed-asset-tabs.ant-tabs-card > .ant-tabs-nav .ant-tabs-tab) {
  border-bottom: none;
  padding: 8px 16px;
  transition: all 0.2s;
}

:deep(.fixed-asset-tabs.ant-tabs-card > .ant-tabs-nav .ant-tabs-tab-active) {
  background-color: #e6f4ff;
  border-bottom-color: transparent;
}

:deep(.fixed-asset-tabs.ant-tabs-card > .ant-tabs-nav::before) {
  border-bottom: 1px solid #f0f0f0;
}

/* Tab 内容区 */
.tab-content-area {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
  padding: 0 16px;
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
  padding: 8px 16px;
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
  .fixed-asset-module {
    padding: 8px;
    border-radius: 6px;
  }
  .tab-content-area {
    padding: 0 8px;
  }
}
</style>