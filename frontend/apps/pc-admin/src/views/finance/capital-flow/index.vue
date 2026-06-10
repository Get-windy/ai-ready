<template>
  <PageContainer full-height>
    <template #header>
      <div class="capital-flow-page-header">
        <div class="capital-flow-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>财务管理</a-breadcrumb-item>
            <a-breadcrumb-item>资金流水台账</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="capital-flow-page-header-title">资金流水台账</h2>
        </div>
        <div class="capital-flow-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <a-button size="small" @click="handleExport">
            <template #icon><DownloadOutlined /></template>
            导出
          </a-button>
        </div>
      </div>
    </template>

    <div class="finance-capital-flow-page">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.inflowAmount) }}</div>
            <div class="stat-card-label">流入总额</div>
          </div>
          <ArrowDownOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-written-off">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.outflowAmount) }}</div>
            <div class="stat-card-label">流出总额</div>
          </div>
          <ArrowUpOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-balance">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.netFlow) }}</div>
            <div class="stat-card-label">净流量</div>
          </div>
          <SwapOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-count">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ stats.totalCount }}</div>
            <div class="stat-card-label">笔数</div>
          </div>
          <FileTextOutlined class="stat-card-icon" />
        </div>
      </div>

      <VxeTableList
        ref="tableRef"
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-key="'id'"
        :filter-fields="filterFields"
        :show-search="false"
        export-permission="finance:capital-flow:list"
        :show-add="false"
        :show-edit="false"
        :show-delete="false"
        :selectable="false"
        @refresh="fetchData"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
      >
        <template #toolbar-actions>
          <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
            更新 {{ dayjs(lastUpdated).format('HH:mm') }}
          </span>
        </template>
        <template #batch-actions>
          <!-- 预留批量操作 -->
        </template>

        <template #empty>
          <div class="table-empty">
            <template v-if="hasError">
              <WarningOutlined class="table-empty-icon" style="color: #faad14" />
              <p class="table-empty-text">加载失败</p>
              <a-button type="primary" size="small" @click="fetchData" class="table-empty-action">
                <ReloadOutlined /> 重试
              </a-button>
            </template>
            <template v-else>
              <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
              <InboxOutlined v-else class="table-empty-icon" />
              <p v-if="hasActiveFilters" class="table-empty-text">
                没有符合条件的资金流水，<a @click="handleResetFilters">清除筛选</a>
              </p>
              <p v-else class="table-empty-text">
                暂无资金流水数据
              </p>
            </template>
          </div>
        </template>

        <template #flowTypeCell="{ record }">
          <span>{{ flowTypeLabelMap[record.flowType] || record.flowType }}</span>
        </template>
        <template #directionCell="{ record }">
          <a-tag :color="record.direction === 'IN' ? 'success' : 'error'">
            {{ record.direction === 'IN' ? '流入' : '流出' }}
          </a-tag>
        </template>
        <template #action="{ record }">
          <a-space :size="0" class="action-cell-inner">
            <a-button type="link" size="small" @click="handleView(record)">
              查看
            </a-button>
          </a-space>
        </template>
      </VxeTableList>

      <!-- 查看详情弹窗 -->
      <FullScreenDetail
        :visible="detailVisible"
        :title="`流水详情 - ${detailData?.flowNo || ''}`"
        @close="handleDetailClose"
      >
        <a-descriptions v-if="detailData" :column="1" bordered size="small">
          <a-descriptions-item label="流水号">{{ detailData.flowNo }}</a-descriptions-item>
          <a-descriptions-item label="业务类型">{{ flowTypeLabelMap[detailData.flowType] || detailData.flowType }}</a-descriptions-item>
          <a-descriptions-item label="方向">
            <a-tag :color="detailData.direction === 'IN' ? 'success' : 'error'">
              {{ detailData.direction === 'IN' ? '流入' : '流出' }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="金额">¥{{ formatAmount(detailData.amount) }}</a-descriptions-item>
          <a-descriptions-item label="余额">¥{{ formatAmount(detailData.balance) }}</a-descriptions-item>
          <a-descriptions-item label="对方名称">{{ detailData.partyName }}</a-descriptions-item>
          <a-descriptions-item label="发生日期">{{ detailData.occurDate }}</a-descriptions-item>
          <a-descriptions-item label="付款方式">{{ detailData.paymentMethod || '-' }}</a-descriptions-item>
          <a-descriptions-item label="交易号">{{ detailData.transactionNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="备注">{{ detailData.remark || '-' }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ detailData.createTime }}</a-descriptions-item>
        </a-descriptions>
      </FullScreenDetail>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  SearchOutlined, InboxOutlined, FileTextOutlined,
  ArrowDownOutlined, ArrowUpOutlined, SwapOutlined,
  SyncOutlined, ReloadOutlined, DownloadOutlined, WarningOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { PageContainer, FullScreenDetail } from '@/components'
import { capitalFlowApi } from '@/api/finance'

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now(); const last = debounceMap.get(key) || 0
  if (now - last < delay) return; debounceMap.set(key, now); fn()
}

const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const refreshLoading = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const searchForm = reactive({
  flowType: undefined as string | undefined,
  direction: undefined as string | undefined,
  dateRange: undefined as [string, string] | undefined,
  partyName: ''
})

const filterFields = [
  { key: 'flowType', label: '流水类型', type: 'select' as const, options: [
    { label: '收款', value: 'receipt' },
    { label: '付款', value: 'payment' },
    { label: '预收款', value: 'pre_receipt' },
    { label: '预付款', value: 'pre_payment' },
    { label: '往来对冲', value: 'offset' },
    { label: '转账', value: 'transfer' }
  ]},
  { key: 'direction', label: '方向', type: 'select' as const, options: [
    { label: '流入', value: 'IN' },
    { label: '流出', value: 'OUT' }
  ]},
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const },
  { key: 'partyName', label: '对方名称', type: 'input' as const, placeholder: '对方名称' }
]

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(searchForm).some(v => v !== undefined && v !== null && v !== '')
})

// ── 统计数据 ────────────────────────────────────────────
const stats = reactive({
  inflowAmount: 0,
  outflowAmount: 0,
  netFlow: 0,
  totalCount: 0
})

const flowTypeLabelMap: Record<string, string> = {
  receipt: '收款',
  payment: '付款',
  pre_receipt: '预收款',
  pre_payment: '预付款',
  offset: '往来对冲',
  transfer: '转账'
}

const columns = computed(() => [
  { title: '流水号', field: 'flowNo', width: 160 },
  { title: '业务类型', field: 'flowType', width: 100, slotName: 'flowTypeCell' },
  { title: '方向', field: 'direction', width: 70, align: 'center', slotName: 'directionCell' },
  { title: '金额', field: 'amount', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '余额', field: 'balance', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '对方名称', field: 'partyName', width: 150 },
  { title: '发生日期', field: 'occurDate', width: 110 },
  { title: '付款方式', field: 'paymentMethod', width: 100 },
  { title: '交易号', field: 'transactionNo', width: 150 },
  { title: '备注', field: 'remark', width: 150, showOverflow: true },
  { title: '操作', type: 'action', width: 80, fixed: 'right' }
])

// 查看详情
const detailVisible = ref(false)
const detailData = ref<any>(null)

const fetchData = async () => {
  loading.value = true
  refreshLoading.value = true
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    if (searchForm.flowType) params.flowType = searchForm.flowType
    if (searchForm.direction) params.direction = searchForm.direction
    if (searchForm.partyName) params.partyName = searchForm.partyName
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }

    const res = await capitalFlowApi.getPage(params)
    if (res.data) {
      tableData.value = res.data.records || res.data.list || []
      pagination.total = res.data.total || 0
      lastUpdated.value = new Date().toISOString()
      // 更新统计
      const inflow = tableData.value
        .filter((item: any) => item.direction === 'IN')
        .reduce((sum: number, item: any) => sum + (item.amount || 0), 0)
      const outflow = tableData.value
        .filter((item: any) => item.direction === 'OUT')
        .reduce((sum: number, item: any) => sum + (item.amount || 0), 0)
      stats.inflowAmount = inflow
      stats.outflowAmount = outflow
      stats.netFlow = inflow - outflow
      stats.totalCount = pagination.total
    }
    hasError.value = false
  } catch (err) {
    console.warn('[资金流水] 获取数据失败', err)
    message.error('获取资金流水数据失败')
    tableData.value = []
    pagination.total = 0
    hasError.value = true
  } finally {
    loading.value = false
    refreshLoading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  }
}

const handleExport = async () => {
  try {
    const params: any = {}
    if (searchForm.flowType) params.flowType = searchForm.flowType
    if (searchForm.direction) params.direction = searchForm.direction
    if (searchForm.partyName) params.partyName = searchForm.partyName
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }

    const blob = await capitalFlowApi.exportList(params)
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `资金流水台账_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (err) {
    console.warn('[资金流水] 导出失败', err)
    message.error('导出失败')
  }
}

const handleView = (record: any) => {
  detailData.value = record
  detailVisible.value = true
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  searchForm.flowType = undefined
  searchForm.direction = undefined
  searchForm.dateRange = undefined
  searchForm.partyName = ''
  handleSearch()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

const handleFilterChange = (filters: Record<string, any>) => {
  searchForm.flowType = filters.flowType !== undefined ? filters.flowType : undefined
  searchForm.direction = filters.direction !== undefined ? filters.direction : undefined
  searchForm.dateRange = filters.dateRange || undefined
  searchForm.partyName = filters.partyName || ''
  pagination.current = 1
  fetchData()
}

const handleDetailClose = () => {
  detailVisible.value = false
  detailData.value = null
}

function handleResetFilters() {
  Object.keys(searchForm).forEach(k => { (searchForm as any)[k] = undefined })
  pagination.current = 1; fetchData()
}

function isInput(el: Element | null): boolean {
  if (!el) return false
  const tag = el.tagName.toLowerCase()
  return tag === 'input' || tag === 'textarea' || tag === 'select' || (el as HTMLElement)?.isContentEditable
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !isInput(e.target as Element | null)) { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); debounceClick('add', handleAdd); return }
}

function handleAdd() {
  message.info('资金流水为只读台账，无法新增')
}

function handleParentCreate() { handleAdd() }

const formatAmount = (val: number) => {
  if (val === undefined || val === null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('finance:create', handleParentCreate)
  window.addEventListener('finance:refresh', fetchData)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('finance:create', handleParentCreate)
  window.removeEventListener('finance:refresh', fetchData)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.capital-flow-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.capital-flow-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.capital-flow-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.capital-flow-page-header-right {
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

.finance-capital-flow-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  gap: 16px;
}

.finance-capital-flow-page > :deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px;
  border-radius: 8px;
}

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-written-off { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }
.stat-balance { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-count { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 18px;
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
  font-size: 24px;
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

.text-danger {
  color: #ff4d4f;
  font-weight: 600;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}

.amount-cell.success {
  color: #52c41a;
}

.amount-cell.warning {
  color: #faad14;
}

.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}

.action-cell-inner {
  display: inline-flex;
  align-items: center;
}

/* 响应式 */

.table-empty-action {
  margin-top: 12px;
}

@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 30%;
    min-width: 100px;
  }
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
