<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="deposit-page-header">
        <div class="deposit-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>财务管理</a-breadcrumb-item>
            <a-breadcrumb-item>双向定金/押金</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="deposit-page-header-title">双向定金/押金</h2>
        </div>
        <div class="deposit-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
          </span>
        </div>
      </div>
    </template>

    <div class="finance-deposit-page">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.totalAmount) }}</div>
            <div class="stat-card-label">定金总额</div>
          </div>
          <DollarOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-written-off">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.usedAmount) }}</div>
            <div class="stat-card-label">已使用金额</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-balance">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.remainingAmount) }}</div>
            <div class="stat-card-label">剩余金额</div>
          </div>
          <ExclamationCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-count">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pagination.total }}</div>
            <div class="stat-card-label">笔数</div>
          </div>
          <FileTextOutlined class="stat-card-icon" />
        </div>
      </div>

      <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
        <!-- 客户定金 -->
        <a-tab-pane key="customer" tab="客户定金">
          <VxeTableList
            ref="tableRef"
            :min-empty-rows="12"
            :columns="customerColumns"
            :data-source="customerTableData"
            :loading="loading"
            :pagination="pagination"
            :row-key="'id'"
            :filter-fields="customerFilterFields"
            :show-search="false"
            export-permission="finance:deposit:list"
            :show-add="false"
            :show-edit="false"
            :show-delete="false"
            :selectable="true"
            @refresh="fetchData"
            @cell-dblclick="handleView"
            @page-change="handlePageChange"
            @filter-change="handleFilterChange"
            @selection-change="handleSelectionChange"
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
                    没有符合条件的记录，<a @click="handleResetFilters">清除筛选</a>
                  </p>
                  <p v-else class="table-empty-text">
                    暂无客户定金数据
                  </p>
                </template>
              </div>
            </template>

            <template #customerStatusCell="{ record }">
              <a-tag :color="customerStatusColorMap[record.status] || 'default'">{{ customerStatusLabelMap[record.status] || '未知' }}</a-tag>
            </template>
            <template #action="{ record }">
              <a-space :size="0" class="action-cell-inner">
                <a-button type="link" size="small" v-permission="'finance:deposit:view'" @click="handleView(record)">
                  查看
                </a-button>
                <PrintButton
                  template-type="pre_receipt"
                  :business-id="record.id"
                  business-type="pre_receipt"
                  button-text=""
                />
              </a-space>
            </template>
          </VxeTableList>
        </a-tab-pane>

        <!-- 供应商押金 -->
        <a-tab-pane key="supplier" tab="供应商押金">
          <VxeTableList
            ref="tableRef"
            :columns="supplierColumns"
            :data-source="supplierTableData"
            :loading="loading"
            :pagination="pagination"
            :row-key="'id'"
            :filter-fields="supplierFilterFields"
            :show-search="false"
            export-permission="finance:deposit:list"
            :show-add="false"
            :show-edit="false"
            :show-delete="false"
            :selectable="true"
            @refresh="fetchData"
            @cell-dblclick="handleView"
            @page-change="handlePageChange"
            @filter-change="handleFilterChange"
            @selection-change="handleSelectionChange"
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
                    没有符合条件的记录，<a @click="handleResetFilters">清除筛选</a>
                  </p>
                  <p v-else class="table-empty-text">
                    暂无供应商押金数据
                  </p>
                </template>
              </div>
            </template>

            <template #supplierStatusCell="{ record }">
              <a-tag :color="supplierStatusColorMap[record.status] || 'default'">{{ supplierStatusLabelMap[record.status] || '未知' }}</a-tag>
            </template>
            <template #action="{ record }">
              <a-space :size="0" class="action-cell-inner">
                <a-button type="link" size="small" v-permission="'finance:deposit:view'" @click="handleView(record)">
                  查看
                </a-button>
                <PrintButton
                  template-type="pre_payment"
                  :business-id="record.id"
                  business-type="pre_payment"
                  button-text=""
                />
              </a-space>
            </template>
          </VxeTableList>
        </a-tab-pane>
      </a-tabs>
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { message } from 'ant-design-vue'
import {
  SearchOutlined, CheckCircleOutlined, InboxOutlined,
  DollarOutlined, ExclamationCircleOutlined, WarningOutlined, FileTextOutlined,
  SyncOutlined, ReloadOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { preReceiptApi, prePaymentApi } from '@/api/finance'
import PrintButton from '@/components/business/print-button/PrintButton.vue'

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now(); const last = debounceMap.get(key) || 0
  if (now - last < delay) return; debounceMap.set(key, now); fn()
}

const tableRef = ref()
const activeTab = ref('customer')
const loading = ref(false)
const hasError = ref(false)
const refreshLoading = ref(false)

const customerTableData = ref<any[]>([])
const supplierTableData = ref<any[]>([])
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ── 客户定金搜索 ──────────────────────────────────────
const customerSearchForm = reactive({
  customerName: '',
  startDate: '',
  endDate: ''
})

const customerFilterFields = [
  { key: 'customerName', label: '客户', type: 'input' as const, placeholder: '客户名称' },
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

// ── 供应商押金搜索 ────────────────────────────────────
const supplierSearchForm = reactive({
  supplierName: '',
  startDate: '',
  endDate: ''
})

const supplierFilterFields = [
  { key: 'supplierName', label: '供应商', type: 'input' as const, placeholder: '供应商名称' },
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
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
  if (activeTab.value === 'customer') {
    return Object.values(customerSearchForm).some(v => v !== undefined && v !== null && v !== '')
  }
  return Object.values(supplierSearchForm).some(v => v !== undefined && v !== null && v !== '')
})

// ── 统计数据 ────────────────────────────────────────────
const stats = reactive({
  totalAmount: 0,
  usedAmount: 0,
  remainingAmount: 0
})

// ── 客户定金状态映射 ──────────────────────────────────
const customerStatusColorMap: Record<string, string> = {
  received: 'success',
  offset: 'processing',
  forfeited: 'default',
  refunded: 'warning'
}

const customerStatusLabelMap: Record<string, string> = {
  received: '已收款',
  offset: '已抵扣',
  forfeited: '已没收',
  refunded: '已退款'
}

// ── 供应商押金状态映射 ────────────────────────────────
const supplierStatusColorMap: Record<string, string> = {
  paid: 'success',
  offset: 'processing',
  recovered: 'default',
  refunded: 'warning'
}

const supplierStatusLabelMap: Record<string, string> = {
  paid: '已付款',
  offset: '已抵扣',
  recovered: '已收回',
  refunded: '已退款'
}

// ── 客户定金列 ──────────────────────────────────────────
const customerColumns = computed(() => [
  { title: '预收单号', field: 'preReceiptNo', width: 150 },
  { title: '客户名称', field: 'customerName', width: 150 },
  { title: '定金金额', field: 'amount', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '已使用', field: 'usedAmount', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '剩余金额', field: 'remainingAmount', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '收款日期', field: 'receiptDate', width: 110 },
  { title: '状态', field: 'status', width: 100, align: 'center', slotName: 'customerStatusCell' },
  { title: '操作', type: 'action', width: 150, fixed: 'right' }
])

// ── 供应商押金列 ────────────────────────────────────────
const supplierColumns = computed(() => [
  { title: '预付单号', field: 'prePaymentNo', width: 150 },
  { title: '供应商名称', field: 'supplierName', width: 150 },
  { title: '押金金额', field: 'amount', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '已使用', field: 'usedAmount', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '剩余金额', field: 'remainingAmount', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '付款日期', field: 'paymentDate', width: 110 },
  { title: '状态', field: 'status', width: 100, align: 'center', slotName: 'supplierStatusCell' },
  { title: '操作', type: 'action', width: 150, fixed: 'right' }
])

const fetchData = async () => {
  loading.value = true
  refreshLoading.value = true
  try {
    if (activeTab.value === 'customer') {
      await fetchCustomerData()
    } else {
      await fetchSupplierData()
    }
    hasError.value = false
  } catch (err) {
    console.warn('[双向定金/押金] 获取数据失败', err)
    message.error('获取数据失败')
    if (activeTab.value === 'customer') {
      customerTableData.value = []
    } else {
      supplierTableData.value = []
    }
    pagination.total = 0
    hasError.value = true
  } finally {
    loading.value = false
    refreshLoading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  }
}

const fetchCustomerData = async () => {
  const params: any = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
    depositFlag: 1
  }
  if (customerSearchForm.customerName) params.customerName = customerSearchForm.customerName
  if (customerSearchForm.startDate) params.startDate = customerSearchForm.startDate
  if (customerSearchForm.endDate) params.endDate = customerSearchForm.endDate

  const res = await preReceiptApi.getPage(params)
  if (res.data) {
    customerTableData.value = res.records || (res.data as any).list || []
    pagination.total = res.total || 0
    lastUpdated.value = new Date().toISOString()
    updateStats(customerTableData.value, res.data)
  }
}

const fetchSupplierData = async () => {
  const params: any = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize,
    depositFlag: 1
  }
  if (supplierSearchForm.supplierName) params.supplierName = supplierSearchForm.supplierName
  if (supplierSearchForm.startDate) params.startDate = supplierSearchForm.startDate
  if (supplierSearchForm.endDate) params.endDate = supplierSearchForm.endDate

  const res = await prePaymentApi.getPage(params)
  if (res.data) {
    supplierTableData.value = res.records || (res.data as any).list || []
    pagination.total = res.total || 0
    lastUpdated.value = new Date().toISOString()
    updateStats(supplierTableData.value, res.data)
  }
}

const updateStats = (data: any[], backendSummary?: any) => {
  if (backendSummary?.totalAmount !== undefined) {
    stats.totalAmount = backendSummary.totalAmount
    stats.usedAmount = backendSummary.totalUsedAmount || 0
    stats.remainingAmount = backendSummary.totalRemainingAmount || 0
  } else {
    stats.totalAmount = data.reduce((sum, item) => sum + (item.amount || 0), 0)
    stats.usedAmount = data.reduce((sum, item) => sum + (item.usedAmount || 0), 0)
    stats.remainingAmount = data.reduce((sum, item) => sum + (item.remainingAmount || 0), 0)
  }
}

const handleTabChange = () => {
  pagination.current = 1
  fetchData()
}

const handleView = (record: any) => {
  const name = activeTab.value === 'customer' ? record.customerName : record.supplierName
  message.info(`查看详情: ${name}`)
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  if (activeTab.value === 'customer') {
    customerSearchForm.customerName = ''
    customerSearchForm.startDate = ''
    customerSearchForm.endDate = ''
  } else {
    supplierSearchForm.supplierName = ''
    supplierSearchForm.startDate = ''
    supplierSearchForm.endDate = ''
  }
  handleSearch()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

const handleFilterChange = (filters: Record<string, any>) => {
  if (activeTab.value === 'customer') {
    customerSearchForm.customerName = filters.customerName || ''
    if (filters.dateRange) {
      customerSearchForm.startDate = filters.dateRange[0] || ''
      customerSearchForm.endDate = filters.dateRange[1] || ''
    } else {
      customerSearchForm.startDate = ''
      customerSearchForm.endDate = ''
    }
  } else {
    supplierSearchForm.supplierName = filters.supplierName || ''
    if (filters.dateRange) {
      supplierSearchForm.startDate = filters.dateRange[0] || ''
      supplierSearchForm.endDate = filters.dateRange[1] || ''
    } else {
      supplierSearchForm.startDate = ''
      supplierSearchForm.endDate = ''
    }
  }
  pagination.current = 1
  fetchData()
}

const handleSelectionChange = (_rows: any[], _ids: any[]) => {
  // 预留批量操作
}

function handleResetFilters() {
  if (activeTab.value === 'customer') {
    Object.keys(customerSearchForm).forEach(k => { (customerSearchForm as any)[k] = '' })
  } else {
    Object.keys(supplierSearchForm).forEach(k => { (supplierSearchForm as any)[k] = '' })
  }
  pagination.current = 1
  fetchData()
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

function handleParentCreate() { handleAdd() }
function handleAdd() {
  message.info('创建功能由父组件触发')
}

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

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
</script>

<style scoped>
.deposit-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.deposit-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.deposit-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.deposit-page-header-right {
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

.finance-deposit-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  gap: 16px;
}

.finance-deposit-page > :deep(.vxe-table-list-container) {
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
.stat-written-off { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-balance { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
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

</style>
