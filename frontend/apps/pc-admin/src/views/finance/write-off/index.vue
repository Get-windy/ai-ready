<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="writeoff-page-header">
        <div class="writeoff-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>财务管理</a-breadcrumb-item>
            <a-breadcrumb-item>收付款核销</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="writeoff-page-header-title">收付款核销</h2>
        </div>
        <div class="writeoff-page-header-right">
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
          </span>
        </div>
      </div>
    </template>

    <div class="finance-writeoff-page">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.totalAmount) }}</div>
            <div class="stat-card-label">{{ activeTab === 'receipt' ? '收款' : '付款' }}总额</div>
          </div>
          <DollarOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-written-off">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.writtenOffAmount) }}</div>
            <div class="stat-card-label">已核销</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-balance">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.balanceAmount) }}</div>
            <div class="stat-card-label">未核销</div>
          </div>
          <ExclamationCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-count">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pagination.total }}</div>
            <div class="stat-card-label">{{ activeTab === 'receipt' ? '收款' : '付款' }}笔数</div>
          </div>
          <FileTextOutlined class="stat-card-icon" />
        </div>
      </div>

      <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
        <!-- 收款核销 -->
        <a-tab-pane key="receipt" tab="收款核销">
          <VxeTableList
            ref="tableRef"
            :min-empty-rows="12"
            :columns="receiptColumns"
            :data-source="receiptTableData"
            :loading="loading"
            :pagination="pagination"
            :row-key="'id'"
            :filter-fields="receiptFilterFields"
            :show-search="false"
            export-permission="finance:writeoff:list"
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
                    暂无收款核销数据
                  </p>
                </template>
              </div>
            </template>

            <template #receiptAction="{ record }">
              <a-space :size="0" class="action-cell-inner">
                <a-tooltip :title="(record.unwrittenOff || 0) <= 0 ? '已全额核销' : '核销'">
                  <a-button type="link" size="small" v-permission="'finance:writeoff:edit'" :disabled="(record.unwrittenOff || 0) <= 0" @click="handleWriteOff(record, 'receipt')">
                    <template #icon><CheckCircleOutlined /></template>
                    核销
                  </a-button>
                </a-tooltip>
                <PrintButton
                  template-type="receipt"
                  :business-id="record.id"
                  business-type="receipt"
                  button-text=""
                />
              </a-space>
            </template>
          </VxeTableList>
        </a-tab-pane>

        <!-- 付款核销 -->
        <a-tab-pane key="payment" tab="付款核销">
          <VxeTableList
            ref="tableRef"
            :columns="paymentColumns"
            :data-source="paymentTableData"
            :loading="loading"
            :pagination="pagination"
            :row-key="'id'"
            :filter-fields="paymentFilterFields"
            :show-search="false"
            export-permission="finance:writeoff:list"
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
                    暂无付款核销数据
                  </p>
                </template>
              </div>
            </template>

            <template #paymentAction="{ record }">
              <a-space :size="0" class="action-cell-inner">
                <a-tooltip :title="(record.unwrittenOff || 0) <= 0 ? '已全额核销' : '核销'">
                  <a-button type="link" size="small" v-permission="'finance:writeoff:edit'" :disabled="(record.unwrittenOff || 0) <= 0" @click="handleWriteOff(record, 'payment')">
                    <template #icon><CheckCircleOutlined /></template>
                    核销
                  </a-button>
                </a-tooltip>
                <PrintButton
                  template-type="payment"
                  :business-id="record.id"
                  business-type="payment"
                  button-text=""
                />
              </a-space>
            </template>
          </VxeTableList>
        </a-tab-pane>
      </a-tabs>

      <!-- 核销弹窗 -->
      <FullScreenDetail
        :visible="writeOffVisible"
        :title="writeOffType === 'receipt' ? '收款核销' : '付款核销'"
        :save-loading="writeOffLoading"
        :dirty="writeOffFormDirty"
        @save="handleWriteOffConfirm"
        @close="handleWriteOffCancel"
      >
        <a-descriptions v-if="writeOffTarget" :column="1" bordered size="small">
          <a-descriptions-item :label="writeOffType === 'receipt' ? '客户名称' : '供应商名称'">
            {{ writeOffType === 'receipt' ? writeOffTarget.customerName : writeOffTarget.supplierName }}
          </a-descriptions-item>
          <a-descriptions-item :label="writeOffType === 'receipt' ? '收款单号' : '付款单号'">
            {{ writeOffType === 'receipt' ? writeOffTarget.receiptNo : writeOffTarget.paymentNo }}
          </a-descriptions-item>
          <a-descriptions-item :label="writeOffType === 'receipt' ? '收款金额' : '付款金额'">
            {{ formatAmount(writeOffTarget.amount) }}
          </a-descriptions-item>
          <a-descriptions-item label="已核销金额">
            {{ formatAmount(writeOffTarget.writtenOff || 0) }}
          </a-descriptions-item>
          <a-descriptions-item label="未核销金额">
            {{ formatAmount(writeOffTarget.unwrittenOff || 0) }}
          </a-descriptions-item>
        </a-descriptions>
        <a-form layout="vertical" style="margin-top: 16px">
          <a-form-item label="核销金额" required>
            <a-input-number
              v-model:value="writeOffAmount"
              :min="0.01"
              :max="writeOffTarget?.unwrittenOff || 0"
              :precision="2"
              size="small"
              style="width: 100%"
              placeholder="请输入核销金额"
            />
          </a-form-item>
        </a-form>
      </FullScreenDetail>
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined, CheckCircleOutlined, InboxOutlined,
  DollarOutlined, ExclamationCircleOutlined, WarningOutlined, FileTextOutlined,
  SyncOutlined, ReloadOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import { receiptApi, paymentApi } from '@/api/finance'
import PrintButton from '@/components/business/print-button/PrintButton.vue'

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now(); const last = debounceMap.get(key) || 0
  if (now - last < delay) return; debounceMap.set(key, now); fn()
}

const tableRef = ref()
const activeTab = ref('receipt')
const loading = ref(false)
const hasError = ref(false)
const refreshLoading = ref(false)

const receiptTableData = ref<any[]>([])
const paymentTableData = ref<any[]>([])
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ── 收款核销搜索 ──────────────────────────────────────
const receiptSearchForm = reactive({
  customerName: '',
  startDate: '',
  endDate: ''
})

const receiptFilterFields = [
  { key: 'customerName', label: '客户', type: 'input' as const, placeholder: '客户名称' },
  { key: 'dateRange', label: '日期范围', type: 'dateRange' as const }
]

// ── 付款核销搜索 ────────────────────────────────────
const paymentSearchForm = reactive({
  supplierName: '',
  startDate: '',
  endDate: ''
})

const paymentFilterFields = [
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
  if (activeTab.value === 'receipt') {
    return Object.values(receiptSearchForm).some(v => v !== undefined && v !== null && v !== '')
  }
  return Object.values(paymentSearchForm).some(v => v !== undefined && v !== null && v !== '')
})

// ── 统计数据 ────────────────────────────────────────────
const stats = reactive({
  totalAmount: 0,
  writtenOffAmount: 0,
  balanceAmount: 0
})

// ── 收款核销列 ──────────────────────────────────────────
const receiptColumns = computed(() => [
  { title: '收款单号', field: 'receiptNo', width: 150 },
  { title: '客户名称', field: 'customerName', width: 150 },
  { title: '收款金额', field: 'amount', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '已核销', field: 'writtenOff', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '未核销', field: 'unwrittenOff', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '收款日期', field: 'receiptDate', width: 110 },
  { title: '操作', type: 'action', width: 120, fixed: 'right' }
])

// ── 付款核销列 ────────────────────────────────────────
const paymentColumns = computed(() => [
  { title: '付款单号', field: 'paymentNo', width: 150 },
  { title: '供应商名称', field: 'supplierName', width: 150 },
  { title: '付款金额', field: 'amount', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '已核销', field: 'writtenOff', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '未核销', field: 'unwrittenOff', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '付款日期', field: 'paymentDate', width: 110 },
  { title: '操作', type: 'action', width: 120, fixed: 'right' }
])

// 核销弹窗
const writeOffVisible = ref(false)
const writeOffLoading = ref(false)
const writeOffTarget = ref<any>(null)
const writeOffAmount = ref(0)
const writeOffType = ref<'receipt' | 'payment'>('receipt')

const initialWriteOffSnapshot = ref('')
const writeOffFormDirty = computed(() => {
  if (!writeOffVisible.value) return false
  return JSON.stringify(writeOffAmount.value) !== initialWriteOffSnapshot.value
})

const fetchData = async () => {
  loading.value = true
  refreshLoading.value = true
  try {
    if (activeTab.value === 'receipt') {
      await fetchReceiptData()
    } else {
      await fetchPaymentData()
    }
    hasError.value = false
  } catch (err) {
    console.warn('[收付款核销] 获取数据失败', err)
    message.error('获取数据失败')
    if (activeTab.value === 'receipt') {
      receiptTableData.value = []
    } else {
      paymentTableData.value = []
    }
    pagination.total = 0
    hasError.value = true
  } finally {
    loading.value = false
    refreshLoading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  }
}

const fetchReceiptData = async () => {
  const params: any = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize
  }
  if (receiptSearchForm.customerName) params.customerName = receiptSearchForm.customerName
  if (receiptSearchForm.startDate) params.startDate = receiptSearchForm.startDate
  if (receiptSearchForm.endDate) params.endDate = receiptSearchForm.endDate

  const res = await receiptApi.getPage(params)
  if (res.data) {
    receiptTableData.value = res.records || (res.data as any).list || []
    pagination.total = res.total || 0
    lastUpdated.value = new Date().toISOString()
    updateStats(receiptTableData.value, res.data)
  }
}

const fetchPaymentData = async () => {
  const params: any = {
    pageNum: pagination.current,
    pageSize: pagination.pageSize
  }
  if (paymentSearchForm.supplierName) params.supplierName = paymentSearchForm.supplierName
  if (paymentSearchForm.startDate) params.startDate = paymentSearchForm.startDate
  if (paymentSearchForm.endDate) params.endDate = paymentSearchForm.endDate

  const res = await paymentApi.getPage(params)
  if (res.data) {
    paymentTableData.value = res.records || (res.data as any).list || []
    pagination.total = res.total || 0
    lastUpdated.value = new Date().toISOString()
    updateStats(paymentTableData.value, res.data)
  }
}

const updateStats = (data: any[], backendSummary?: any) => {
  if (backendSummary?.totalAmount !== undefined) {
    stats.totalAmount = backendSummary.totalAmount
    stats.writtenOffAmount = backendSummary.totalWrittenOff || 0
    stats.balanceAmount = backendSummary.totalUnwrittenOff || 0
  } else {
    stats.totalAmount = data.reduce((sum, item) => sum + (item.amount || 0), 0)
    stats.writtenOffAmount = data.reduce((sum, item) => sum + (item.writtenOff || 0), 0)
    stats.balanceAmount = data.reduce((sum, item) => sum + (item.unwrittenOff || 0), 0)
  }
}

const handleTabChange = () => {
  pagination.current = 1
  fetchData()
}

const handleView = (record: any) => {
  const name = activeTab.value === 'receipt' ? record.customerName : record.supplierName
  message.info(`查看详情: ${name}`)
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  if (activeTab.value === 'receipt') {
    receiptSearchForm.customerName = ''
    receiptSearchForm.startDate = ''
    receiptSearchForm.endDate = ''
  } else {
    paymentSearchForm.supplierName = ''
    paymentSearchForm.startDate = ''
    paymentSearchForm.endDate = ''
  }
  handleSearch()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

const handleFilterChange = (filters: Record<string, any>) => {
  if (activeTab.value === 'receipt') {
    receiptSearchForm.customerName = filters.customerName || ''
    if (filters.dateRange) {
      receiptSearchForm.startDate = filters.dateRange[0] || ''
      receiptSearchForm.endDate = filters.dateRange[1] || ''
    } else {
      receiptSearchForm.startDate = ''
      receiptSearchForm.endDate = ''
    }
  } else {
    paymentSearchForm.supplierName = filters.supplierName || ''
    if (filters.dateRange) {
      paymentSearchForm.startDate = filters.dateRange[0] || ''
      paymentSearchForm.endDate = filters.dateRange[1] || ''
    } else {
      paymentSearchForm.startDate = ''
      paymentSearchForm.endDate = ''
    }
  }
  pagination.current = 1
  fetchData()
}

const handleSelectionChange = (_rows: any[], _ids: any[]) => {
  // 预留批量操作
}

function handleResetFilters() {
  if (activeTab.value === 'receipt') {
    Object.keys(receiptSearchForm).forEach(k => { (receiptSearchForm as any)[k] = '' })
  } else {
    Object.keys(paymentSearchForm).forEach(k => { (paymentSearchForm as any)[k] = '' })
  }
  pagination.current = 1
  fetchData()
}

const handleWriteOff = (record: any, type: 'receipt' | 'payment') => {
  writeOffType.value = type
  writeOffTarget.value = record
  writeOffAmount.value = record.unwrittenOff || 0
  initialWriteOffSnapshot.value = JSON.stringify(writeOffAmount.value)
  writeOffVisible.value = true
}

const handleWriteOffConfirm = async () => {
  if (!writeOffAmount.value || writeOffAmount.value <= 0) {
    message.warning('请输入有效的核销金额')
    return
  }
  if (writeOffAmount.value > (writeOffTarget.value?.unwrittenOff || 0)) {
    message.warning('核销金额不能大于未核销金额')
    return
  }
  writeOffLoading.value = true
  try {
    if (writeOffType.value === 'receipt') {
      await receiptApi.writeOff(writeOffTarget.value.id, writeOffAmount.value)
    } else {
      await paymentApi.writeOff(writeOffTarget.value.id, writeOffAmount.value)
    }
    message.success('核销成功')
    writeOffVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[收付款核销] 核销失败', err)
    message.error('核销失败')
  } finally {
    writeOffLoading.value = false
  }
}

const handleWriteOffCancel = () => {
  writeOffVisible.value = false
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

onBeforeRouteLeave((to, from, next) => {
  if (writeOffVisible.value && writeOffFormDirty.value) {
    Modal.confirm({
      title: '确认离开',
      content: '当前表单有未保存的修改，确定要离开吗？',
      onOk: () => next(),
      onCancel: () => next(false)
    })
  } else {
    next()
  }
})

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
.writeoff-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.writeoff-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.writeoff-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.writeoff-page-header-right {
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

.finance-writeoff-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  gap: 16px;
}

.finance-writeoff-page > :deep(.vxe-table-list-container) {
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

.action-cell-inner {
  display: inline-flex;
  align-items: center;
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
