<template>
  <PageContainer full-height>
    <template #header>
      <div class="stock-header">
        <div class="stock-header-left">
          <a-breadcrumb class="stock-breadcrumb">
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>库存管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="stock-header-title">库存管理</h2>
        </div>
        <div class="stock-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0 && autoRefreshEnabled" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-tooltip v-else-if="!autoRefreshEnabled" title="连续失败后自动刷新已暂停">
            <span class="auto-refresh-badge auto-refresh-badge--paused">
              <SyncOutlined /> 暂停
            </span>
          </a-tooltip>
          <a-button size="small" :loading="refreshLoading" v-permission="'stock:item:refresh'" @click="debounceClick('refresh', handleRefresh)">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <div class="stock-module">
      <!-- 统计卡片：加载态 -->
      <template v-if="loading">
        <div class="stat-cards">
          <a-card v-for="i in 4" :key="i" :bordered="false" class="stat-skeleton">
            <a-skeleton active :paragraph="{ rows: 1 }" :title="{ width: '60%' }" />
          </a-card>
        </div>
      </template>
      <!-- 统计卡片：错误态 -->
      <template v-else-if="statError">
        <div class="stat-cards">
          <a-result status="warning" title="统计数据加载失败" sub-title="部分数据可能未更新">
            <template #extra>
              <a-button size="small" @click="initialLoadStats">
                <ReloadOutlined /> 重试
              </a-button>
            </template>
          </a-result>
        </div>
      </template>
      <!-- 统计卡片：正常态 -->
      <div v-else class="stat-cards">
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
        <a-tab-pane v-for="tab in visibleTabs" :key="tab.key">
          <template #tab>
            <span v-if="tab.key === 'stock'"><DatabaseOutlined /> 库存查询<a-badge :count="stockCount" :overflow-count="99" :number-style="{ backgroundColor: '#1890ff' }" style="margin-left: 8px" /></span>
            <span v-else-if="tab.key === 'inbound'"><LoginOutlined /> 入库管理<a-badge :count="inboundCount" :overflow-count="99" :number-style="{ backgroundColor: '#52c41a' }" style="margin-left: 8px" /></span>
            <span v-else-if="tab.key === 'outbound'"><LogoutOutlined /> 出库管理<a-badge :count="outboundCount" :overflow-count="99" :number-style="{ backgroundColor: '#faad14' }" style="margin-left: 8px" /></span>
            <span v-else-if="tab.key === 'check'"><CheckSquareOutlined /> 库存盘点</span>
            <span v-else-if="tab.key === 'transfer'"><SwapOutlined /> 库存调拨</span>
            <span v-else-if="tab.key === 'batch'"><TagOutlined /> 批次管理<a-badge :count="batchCount" :overflow-count="99" :number-style="{ backgroundColor: '#722ed1' }" style="margin-left: 8px" /></span>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import StockTab from './tabs/Stock.vue'
import InboundTab from './tabs/Inbound.vue'
import OutboundTab from './tabs/Outbound.vue'
import CheckTab from './tabs/Check.vue'
import TransferTab from './tabs/Transfer.vue'
import BatchTab from './tabs/Batch.vue'
import {
  ReloadOutlined, DatabaseOutlined, LoginOutlined, LogoutOutlined,
  CheckSquareOutlined, SwapOutlined, TagOutlined, SyncOutlined
} from '@ant-design/icons-vue'
import { hasPermission } from '@/utils/permission'

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

const VALID_TABS = ['stock', 'inbound', 'outbound', 'check', 'transfer', 'batch'] as const
const activeTab = ref<string>('stock')
const loading = ref(true)
const refreshLoading = ref(false)
const statError = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const autoRefreshEnabled = ref(true)
let consecutiveErrors = 0

// Tab 引用
const stockRef = ref()
const inboundRef = ref()
const outboundRef = ref()
const checkRef = ref()
const transferRef = ref()
const batchRef = ref()

// Tab 统计数据（由子组件通过事件更新）
const stockCount = ref(0)
const inboundCount = ref(0)
const outboundCount = ref(0)
const batchCount = ref(0)

// Tab 权限过滤
interface TabDef { key: string; label: string; permission: string }
const allTabs: TabDef[] = [
  { key: 'stock', label: '库存查询', permission: 'stock:list' },
  { key: 'inbound', label: '入库管理', permission: 'stock:inbound:list' },
  { key: 'outbound', label: '出库管理', permission: 'stock:outbound:list' },
  { key: 'check', label: '库存盘点', permission: 'stock:check:list' },
  { key: 'transfer', label: '库存调拨', permission: 'stock:transfer:list' },
  { key: 'batch', label: '批次管理', permission: 'stock:batch:list' },
]
const visibleTabs = computed(() => allTabs.filter(t => !t.permission || hasPermission(t.permission)))

function handleTabChange(key: string) {
  router.replace({ query: { ...route.query, tab: key } })
}

async function initialLoadStats() {
  loading.value = true
  statError.value = false
  try {
    // 尝试从各 tab 组件获取统计数据
    const refMap: Record<string, any> = {
      stock: stockRef.value,
      inbound: inboundRef.value,
      outbound: outboundRef.value,
      batch: batchRef.value
    }
    const promises = Object.entries(refMap).map(([key, ref]) => {
      if (ref?.fetchData) {
        return ref.fetchData().catch(() => {
          console.warn(`[库存管理] ${key} tab 初始加载失败`)
          return null
        })
      }
      return Promise.resolve()
    })
    await Promise.allSettled(promises)
    statError.value = false
  } catch {
    statError.value = true
  } finally {
    loading.value = false
  }
}

function handleRefresh() {
  refreshLoading.value = true

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

  lastUpdateTime.value = dayjs().format('HH:mm:ss')
  refreshLoading.value = false
}

function onTabFetchError() {
  consecutiveErrors++
  if (consecutiveErrors >= 3) {
    autoRefreshEnabled.value = false
    console.warn('[库存管理] 连续 3 次加载失败，自动刷新已停止')
  }
  statError.value = true
}

function onTabFetchSuccess() {
  consecutiveErrors = 0
  statError.value = false
  lastUpdateTime.value = dayjs().format('HH:mm:ss')
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
    message.info({ content: '正在刷新数据...', key: 'refresh-hint', duration: 1 })
    debounceClick('refresh', handleRefresh)
    return
  }
  // Ctrl+N 新建
  if ((e.ctrlKey || e.metaKey) && e.key === 'n' && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    message.info({ content: '触发新建操作', key: 'create-hint', duration: 1 })
    window.dispatchEvent(new CustomEvent('stock:create'))
  }
  // Ctrl+F 筛选
  if ((e.ctrlKey || e.metaKey) && e.key === 'f' && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    message.info({ content: '打开筛选面板', key: 'filter-hint', duration: 1 })
    window.dispatchEvent(new CustomEvent('stock:filter'))
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
  lastUpdateTime.value = dayjs().format('HH:mm:ss')
  initialLoadStats()

  autoRefreshCountdown.value = 30
  autoRefreshTimer = setInterval(() => {
    if (!autoRefreshEnabled.value) return
    const refMap: Record<string, any> = {
      stock: stockRef.value, inbound: inboundRef.value,
      outbound: outboundRef.value, check: checkRef.value,
      transfer: transferRef.value, batch: batchRef.value
    }
    const currentRef = refMap[activeTab.value]
    if (currentRef?.fetchData) {
      currentRef.fetchData().then(() => {
        lastUpdateTime.value = dayjs().format('HH:mm:ss')
        consecutiveErrors = 0
      }).catch(() => {
        onTabFetchError()
      })
    }
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0 && autoRefreshEnabled.value) autoRefreshCountdown.value--
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
</script>

<style scoped>
.stock-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.stock-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.stock-breadcrumb {
  font-size: 13px;
}
.stock-breadcrumb :deep(li) {
  font-size: 13px;
}
.stock-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.stock-header-right {
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

.auto-refresh-badge--paused {
  color: #e6a23c;
  background: #fdf6ec;
  cursor: help;
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
  margin-bottom: 16px;
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