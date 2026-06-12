<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="depreciation-page-header">
        <div class="depreciation-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>固定资产</a-breadcrumb-item>
            <a-breadcrumb-item>折旧记录</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="depreciation-page-header-title">折旧记录</h2>
        </div>
        <div class="depreciation-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <PrintButton :record="{ type: 'depreciation' }" business-type="fixed_asset_depreciation" button-type="link" button-size="small" tooltip="打印折旧记录" />
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)()" v-permission="'erp:fixed-asset:depreciation:list'">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
<span class="shortcut-hints">
                                                <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
                                                <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
                                              </span>
        </div>

      </div>
    </template>

    <div class="depreciation-list-page">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(totalDepreciation) }}</div>
            <div class="stat-card-label">累计折旧总额</div>
          </div>
          <CalculatorOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-period">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(periodDepreciation) }}</div>
            <div class="stat-card-label">本期折旧额</div>
          </div>
          <CalendarOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-count">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pagination.total }}</div>
            <div class="stat-card-label">折旧记录数</div>
          </div>
          <FileTextOutlined class="stat-card-icon" />
        </div>
      </div>

      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableDataSource"
        :loading="loading"
        :pagination="pagination"
        :table-key="'fixed-asset-depreciation-list'"
        :filter-fields="filterFields"
        :show-export="true"
        export-permission="erp:fixed-asset:depreciation:list"
        :selectable="true"
        :min-empty-rows="12"
        @refresh="debounceClick('refresh', fetchData)()"
        @cell-dblclick="handleViewAsset"
        @search="handleSearch"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
        @export="handleExport"
        @selection-change="handleSelectionChange"
      >
        <template #toolbar-actions>
          <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
            更新 {{ dayjs(lastUpdated).format('HH:mm') }}
          </span>
          <a-button type="primary" ghost @click="handleBatchCalculate" v-permission="'erp:fixed-asset:depreciation:calculate'">
            <template #icon><CalculatorOutlined /></template>
            批量计提折旧
          </a-button>
        </template>

        <template #batch-actions="{ selectedRowKeys }: any">
          <span class="batch-info">已选择 {{ selectedRowKeys.length }} 项</span>
        </template>

        <template #empty>
          <div v-if="hasError" class="table-empty table-empty-error">
            <WarningOutlined class="table-empty-icon table-empty-icon-error" />
            <p class="table-empty-text">数据加载失败，请重试</p>
            <a-button size="small" @click="fetchData">
              <template #icon><ReloadOutlined /></template>
              重试
            </a-button>
          </div>
          <div v-else class="table-empty">
            <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
            <InboxOutlined v-else class="table-empty-icon" />
            <p v-if="hasActiveFilters" class="table-empty-text">
              没有符合条件的折旧记录，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无折旧记录，点击「批量计提折旧」开始计提
            </p>
          </div>
        </template>

        <template #assetCodeCell="{ record }">
          <a @click="handleViewAsset(record)" class="asset-code">{{ record.assetCode }}</a>
        </template>
        <template #assetNameCell="{ record }">
          <a @click="handleViewAsset(record)" class="asset-name">{{ record.assetName }}</a>
        </template>
        <template #periodAmountCell="{ record }">
          <span class="amount-cell depreciation">¥{{ formatAmount(record.periodAmount) }}</span>
        </template>
        <template #accumulatedDepreciationCell="{ record }">
          <span class="amount-cell">¥{{ formatAmount(record.accumulatedDepreciation) }}</span>
        </template>
        <template #netValueCell="{ record }">
          <span class="amount-cell success">¥{{ formatAmount(record.netValue) }}</span>
        </template>
        <template #assetOriginalValueCell="{ record }">
          <span class="amount-cell">¥{{ formatAmount(record.assetOriginalValue) }}</span>
        </template>
        <template #statusCell="{ record }">
          <a-tag :color="record.status === 'completed' ? 'green' : 'orange'">
            {{ record.status === 'completed' ? '已完成' : '待处理' }}
          </a-tag>
        </template>
        <template #actionCell="{ record }">
          <a-space>
            <a @click="handleViewAsset(record)">查看资产</a>
          </a-space>
        </template>
      </VxeTableList>
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined, InboxOutlined, CalculatorOutlined, CalendarOutlined,
  FileTextOutlined, SyncOutlined, ReloadOutlined, WarningOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { depreciationApi } from '@/api/fixed-asset'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { useRouter } from 'vue-router'

// ── 防抖工具 ──────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

const emit = defineEmits(['update-count'])
const router = useRouter()

const loading = ref(false)
const tableData = ref<any[]>([])
const tableRef = ref()
const lastUpdated = ref('')
const selectedRowKeys = ref<number[]>([])
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
const hasError = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const searchFilters = reactive<Record<string, any>>({})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})

function handleSelectionChange(keys: number[]) {
  selectedRowKeys.value = keys
}

// ── 统计数据 ────────────────────────────────────────────
const totalDepreciation = computed(() => {
  return tableData.value.reduce((sum, r) => sum + (r.accumulatedDepreciation || 0), 0)
})

const periodDepreciation = computed(() => {
  return tableData.value.reduce((sum, r) => sum + (r.periodAmount || 0), 0)
})

// ── 数据源 ────────────────────────────────────────────
const tableDataSource = tableData

const vxeColumns = computed(() => [
  { field: 'assetCode', title: '资产编码', width: 140, slotName: 'assetCodeCell' },
  { field: 'assetName', title: '资产名称', width: 180, slotName: 'assetNameCell' },
  { field: 'period', title: '期间', width: 100 },
  { field: 'depreciationDate', title: '折旧日期', width: 120 },
  { field: 'periodAmount', title: '本期折旧', width: 130, align: 'right', slotName: 'periodAmountCell' },
  { field: 'accumulatedDepreciation', title: '累计折旧', width: 130, align: 'right', slotName: 'accumulatedDepreciationCell' },
  { field: 'netValue', title: '净值', width: 130, align: 'right', slotName: 'netValueCell' },
  { field: 'assetOriginalValue', title: '资产原值', width: 130, align: 'right', slotName: 'assetOriginalValueCell' },
  { field: 'status', title: '状态', width: 80, align: 'center', slotName: 'statusCell' },
  { type: 'action', title: '操作', width: 80, align: 'center', fixed: 'right' },
])

const filterFields = [
  { key: 'assetId', label: '资产ID', type: 'input' as const, placeholder: '资产ID' },
  { key: 'period', label: '期间', type: 'input' as const, placeholder: 'yyyy-MM' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '已完成', value: 'completed' },
    { label: '待处理', value: 'pending' },
  ]},
]

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

function handleParentCreate() {
  handleBatchCalculate()
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('fixed-asset:create', handleParentCreate)
  window.addEventListener('fixed-asset:refresh', fetchData)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

async function fetchData() {
  loading.value = true
  hasError.value = false
  const params: any = {
    page: pagination.current - 1,
    size: pagination.pageSize,
  }
  if (searchFilters.assetId) params.assetId = Number(searchFilters.assetId)
  if (searchFilters.period) params.period = searchFilters.period
  if (searchFilters.status) params.status = searchFilters.status

  try {
    const res = await depreciationApi.getPage(params)
    if (res.data) {
      tableData.value = res.content || res.records || []
      pagination.total = res.totalElements || res.total || 0
      lastUpdated.value = new Date().toISOString()
      emit('update-count', pagination.total)
    }
  } catch {
    hasError.value = true
    tableData.value = []
    pagination.total = 0
    console.warn('[折旧记录] 加载折旧数据失败')
    message.error('加载折旧数据失败')
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

function handleSearch(keyword: string) {
  searchFilters.keyword = keyword || undefined
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  fetchData()
}

function handleBatchCalculate() {
  Modal.confirm({
    title: '批量计提折旧',
    content: '对本期间所有资产进行折旧计提？',
    okText: '确认计提',
    centered: true,
    onOk: async () => {
      try {
        await depreciationApi.batchCalculate({} as any)
        message.success('批量折旧计提完成')
        fetchData()
      } catch {
        console.warn('[折旧记录] 批量折旧失败')
        message.error('批量折旧失败')
      }
    }
  })
}

function handleViewAsset(record: any) {
  if (record.assetId) {
    router.push(`/fixed-asset/asset/detail/${record.assetId}`)
  }
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined })
  pagination.current = 1
  fetchData()
}

function handleExport() {
  const headers = ['资产编码', '资产名称', '期间', '折旧日期', '本期折旧', '累计折旧', '净值', '资产原值', '状态']
  const rows = tableData.value.map(r => [
    r.assetCode, r.assetName, r.period, r.depreciationDate,
    formatAmount(r.periodAmount), formatAmount(r.accumulatedDepreciation),
    formatAmount(r.netValue), formatAmount(r.assetOriginalValue),
    r.status === 'completed' ? '已完成' : '待处理'
  ])
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `折旧记录_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

function handleKeydown(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  const isInput = tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT'
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !isInput) {
    e.preventDefault(); debounceClick('refresh', fetchData)(); return
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n' && !isInput) { e.preventDefault(); handleBatchCalculate() }
}

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('fixed-asset:create', handleParentCreate)
  window.removeEventListener('fixed-asset:refresh', fetchData)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchData })

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
</script>

<style scoped>
.depreciation-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.depreciation-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.depreciation-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.depreciation-page-header-right {
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

.depreciation-list-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.depreciation-list-page :deep(.vxe-table) {
  flex: 1;
  min-height: 0;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-total { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-period { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-count { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

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

/* 空状态 */
.table-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0;
}

.table-empty-icon {
  font-size: 48px;
  color: #d9d9d9;
}

.table-empty-text {
  color: #999;
  margin-top: 12px;
}

.table-empty-error {
  padding: 48px 0;
}

.table-empty-icon-error {
  color: #faad14;
}

.asset-code {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-weight: 500;
}

.asset-name {
  font-weight: 500;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

.amount-cell.depreciation {
  color: #faad14;
}

.amount-cell.success {
  color: #52c41a;
}

.list-update-timestamp {
  font-size: 12px;
  color: var(--color-text-tertiary, #bbb);
  white-space: nowrap;
  cursor: help;
  margin-left: 8px;
  line-height: 32px;
  vertical-align: middle;
}





/* 响应式 */
@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 45%;
    min-width: 120px;
  }
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

/* ── vxe-table 表头边框 ──────────────────────── */
:deep(.vxe-table--header-border) {
  border-bottom: 2px solid #e8e8e8 !important;
}

/* ── 空状态容器 ──────────────────────── */
:deep(.empty-state-wrapper) {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  min-height: 200px;
}

</style>
