<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="purchase-exchange-header">
        <div class="purchase-exchange-header__left">
          <span class="purchase-exchange-header__breadcrumb">ERP / 采购管理 / 采购换货</span>
          <h2 class="purchase-exchange-header__title">采购换货管理</h2>
        </div>
        <div class="purchase-exchange-header__right">
          <a-space :size="12">
            <a-switch size="small" v-model:checked="autoRefreshEnabled" checked-children="自动" un-checked-children="手动" @change="handleAutoRefreshChange" />
            <span v-if="autoRefreshEnabled && autoRefreshCountdown > 0" class="auto-refresh-badge">
              <SyncOutlined /> {{ autoRefreshCountdown }}s
            </span>
            <span class="data-status">
              <a-badge :status="loading ? 'processing' : 'success'" />
              <span v-if="lastUpdateTime" class="update-time">
                数据更新: {{ lastUpdateTime }}
              </span>
            </span>
            <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)">
              <template #icon><ReloadOutlined /></template>
              刷新
            </a-button>
          </a-space>
        </div>
      </div>
    </template>

    <!-- 统计卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px;">
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
            <FileTextOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">换货单总数</div>
            <div class="summary-value">{{ statistics.totalCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
            <ClockCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">待审批</div>
            <div class="summary-value warning">{{ statistics.pendingCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card">
          <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
            <CheckCircleOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">已完成</div>
            <div class="summary-value">{{ statistics.completedCount }}</div>
          </div>
        </div>
      </a-col>
      <a-col :span="6">
        <div class="summary-card highlight">
          <div class="summary-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
            <DollarOutlined />
          </div>
          <div class="summary-content">
            <div class="summary-title">换货金额</div>
            <div class="summary-value">¥{{ formatAmount(statistics.totalAmount) }}</div>
          </div>
        </div>
      </a-col>
    </a-row>

    <a-card title="采购换货管理" style="flex: 1; overflow: hidden;" :bodyStyle="{ display: 'flex', flexDirection: 'column', height: 'calc(100% - 57px)' }">
      <!-- 搜索栏 -->
      <SearchBar
        :fields="searchFields"
        :loading="loading"
        @search="handleSearch"
        @reset="handleReset"
      />

      <!-- 操作按钮 -->
      <div class="action-area">
        <a-space>
          <a-button type="primary" @click="handleCreate">
            <template #icon><PlusOutlined /></template>新建换货单
          </a-button>
          <a-button @click="handleExport">
            <template #icon><ExportOutlined /></template>导出
          </a-button>
        </a-space>
      </div>

      <!-- 数据表格 -->
      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
        @cell-dblclick="handleView"
        @page-change="handlePageChange"
      >
        <template #empty>
          <div v-if="hasError" class="table-empty">
            <WarningOutlined class="table-empty-icon" />
            <p class="table-empty-text">数据加载异常，请重试</p>
            <a-button type="primary" @click="fetchData"><ReloadOutlined /> 重试</a-button>
          </div>
          <EmptyState v-else title="暂无数据" description="暂无采购换货数据" size="small" :show-actions="false" />
        </template>
        <template #statusCell="{ record }">
          <StatusTag :status="record.status" :map="RETURN_EXCHANGE_STATUS" />
        </template>
        <template #exchangeTypeCell="{ record }">{{ getExchangeTypeText(record.exchangeType) }}</template>
        <template #totalAmountCell="{ record }">¥{{ record.totalAmount?.toFixed(2) }}</template>
        <template #action="{ record }">
          <a-space>
            <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
            <a-button v-if="record.status === ExchangeStatus.DRAFT" type="link" size="small" @click="handleEdit(record)">编辑</a-button>
            <a-button v-if="record.status === ExchangeStatus.DRAFT" type="link" size="small" @click="handleSubmit(record)">提交</a-button>
            <a-button v-if="record.status === ExchangeStatus.PENDING_APPROVAL" type="link" size="small" @click="handleApprove(record)">审批</a-button>
            <a-button type="link" size="small" @click="handleTrack(record)">跟踪</a-button>
            <PrintButton
              v-if="record.status === ExchangeStatus.COMPLETED || record.status === ExchangeStatus.APPROVED"
              :record="record"
              business-type="PURCHASE_EXCHANGE"
              button-text="打印"
              button-size="small"
            />
            <a-button type="link" size="small" danger @click="handleDelete(record)">删除</a-button>
          </a-space>
        </template>
      </VxeTableList>
    </a-card>

    <ExchangeFormModal v-model:open="formModalVisible" :record="currentRecord" @success="handleFormSuccess" />
    <ExchangeApproveModal v-model:open="approveModalVisible" :record="currentRecord" @success="handleApproveSuccess" />
    <ExchangeDetailModal v-model:open="detailModalVisible" :record="currentRecord" />
    <ExchangeTrackModal v-model:open="trackModalVisible" :record="currentRecord" />
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, SearchOutlined, ReloadOutlined, SyncOutlined, ExportOutlined, FileTextOutlined, ClockCircleOutlined, CheckCircleOutlined, DollarOutlined, WarningOutlined } from '@ant-design/icons-vue'
import { PageContainer, SearchBar, EmptyState } from '@/components'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import type { Dayjs } from 'dayjs'
import { purchaseExchangeApi, type PurchaseExchange, ExchangeStatus } from '@/api/purchase-exchange'
import ExchangeFormModal from './components/ExchangeFormModal.vue'
import ExchangeApproveModal from './components/ExchangeApproveModal.vue'
import ExchangeDetailModal from './components/ExchangeDetailModal.vue'
import ExchangeTrackModal from './components/ExchangeTrackModal.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import { RETURN_EXCHANGE_STATUS } from '@/utils/statusConfig'
import { getStatusText } from '@/utils/statusConfig'
import { exportCsv } from '@/utils/exportCsv'
import { supplierApi } from '@/api/supplier'
import PrintButton from '@/components/business/print-button/PrintButton.vue'

// ── 类型定义 ──────────────────────────────────────────
export interface PurchaseExchangeItem {
  id?: number
  exchangeId?: number
  productCode: string
  productName: string
  productSpec?: string
  quantity: number
  unitPrice: number
  amount: number
  remark?: string
}

export interface ExchangeStatistics {
  totalCount: number
  pendingCount: number
  completedCount: number
  totalAmount: number
}

export interface ExchangeQueryParams {
  exchangeNo: string
  originalOrderNo: string
  supplierId?: number
  status?: ExchangeStatus
  startDate: string
  endDate: string
  current: number
  size: number
}

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

// ── 键盘快捷键 ──────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleCreate(); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') { e.preventDefault(); handleExport(); return }
}

function handleError(err: any) {
  hasError.value = true
  console.warn('[采购换货] ErrorBoundary 捕获异常:', err)
}

const handleAutoRefreshChange = (checked: boolean) => {
  if (checked) {
    autoRefreshCountdown.value = 30
  } else {
    autoRefreshCountdown.value = 0
  }
}

const loading = ref(false)
const hasError = ref(false)
const refreshLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const autoRefreshEnabled = ref(true)
const dataSource = ref<PurchaseExchange[]>([])
const dateRange = ref<[Dayjs, Dayjs] | null>(null)
const tableRef = ref()
const supplierOptions = ref<{ id: number; name: string }[]>([])

let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// 统计数据（使用 computed 自动计算）
const statistics = computed(() => ({
  totalCount: dataSource.value.length,
  pendingCount: dataSource.value.filter(r => r.status === ExchangeStatus.PENDING_APPROVAL).length,
  completedCount: dataSource.value.filter(r => r.status === ExchangeStatus.COMPLETED).length,
  totalAmount: dataSource.value.reduce((sum, r) => sum + (r.totalAmount || 0), 0)
}))

const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const searchFields: SearchField[] = [
  { name: 'exchangeNo', label: '换货单号', type: 'input', placeholder: '请输入换货单号' },
  { name: 'originalOrderNo', label: '原采购订单', type: 'input', placeholder: '请输入原采购订单号' },
  { name: 'supplierId', label: '供应商', type: 'select', placeholder: '请选择供应商', options: [] },
  { name: 'status', label: '状态', type: 'select', placeholder: '请选择状态', options: [
    { label: '草稿', value: ExchangeStatus.DRAFT },
    { label: '待审批', value: ExchangeStatus.PENDING_APPROVAL },
    { label: '已审批', value: ExchangeStatus.APPROVED },
    { label: '换货中', value: ExchangeStatus.EXCHANGING },
    { label: '已完成', value: ExchangeStatus.COMPLETED },
    { label: '已拒绝', value: ExchangeStatus.REJECTED },
    { label: '已取消', value: ExchangeStatus.CANCELLED },
  ]},
]

const queryParams = reactive({
  exchangeNo: '',
  originalOrderNo: '',
  supplierId: undefined as number | undefined,
  status: undefined as ExchangeStatus | undefined,
  startDate: '',
  endDate: '',
  current: 1,
  size: 10
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const vxeColumns = computed(() => [
  { field: 'exchangeNo', title: '换货单号', width: 180 },
  { field: 'originalOrderNo', title: '原采购订单', width: 180 },
  { field: 'supplierName', title: '供应商', width: 120 },
  { field: 'exchangeDate', title: '换货日期', width: 120 },
  { field: 'exchangeType', title: '换货类型', width: 100, slotName: 'exchangeTypeCell' },
  { field: 'totalAmount', title: '换货金额', width: 120, slotName: 'totalAmountCell' },
  { field: 'status', title: '状态', width: 100, slotName: 'statusCell' },
  { field: 'createdByName', title: '创建人', width: 100 },
  { field: 'createTime', title: '创建时间', width: 130 },
  { field: 'action', title: '操作', width: 250, fixed: 'right', type: 'action' },
])

const getExchangeTypeText = (type: number): string => {
  const texts: Record<number, string> = { 1: '质量问题', 2: '规格不符', 3: '数量错误', 4: '其他' }
  return texts[type] || '未知'
}

const formModalVisible = ref(false)
const approveModalVisible = ref(false)
const detailModalVisible = ref(false)
const trackModalVisible = ref(false)
const currentRecord = ref<PurchaseExchange | null>(null)

const fetchData = async () => {
  hasError.value = false
  loading.value = true
  try {
    const res = await purchaseExchangeApi.page({
      ...queryParams,
      current: pagination.current,
      size: pagination.pageSize
    })
    dataSource.value = res.data?.records || []
    pagination.total = res.data?.total || 0
  } catch (error) {
    hasError.value = true
    console.warn('[采购换货] 获取数据失败', error)
    message.error('获取数据失败')
  } finally {
    loading.value = false
    refreshLoading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  }
}

const handleSearch = (values?: Record<string, any>) => {
  if (values) {
    Object.assign(queryParams, {
      exchangeNo: values.exchangeNo || '',
      originalOrderNo: values.originalOrderNo || '',
      supplierId: values.supplierId,
      status: values.status,
      startDate: values.exchangeDateRange?.[0] || '',
      endDate: values.exchangeDateRange?.[1] || ''
    })
  }
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  queryParams.exchangeNo = ''
  queryParams.originalOrderNo = ''
  queryParams.supplierId = undefined
  queryParams.status = undefined
  queryParams.startDate = ''
  queryParams.endDate = ''
  dateRange.value = null
  pagination.current = 1
  fetchData()
}

const handleDateChange = (dates: [Dayjs, Dayjs] | null) => {
  if (dates) {
    queryParams.startDate = dates[0].format('YYYY-MM-DD')
    queryParams.endDate = dates[1].format('YYYY-MM-DD')
  } else {
    queryParams.startDate = ''
    queryParams.endDate = ''
  }
}

const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

function handleParentCreate() { handleCreate() }

const handleCreate = () => {
  currentRecord.value = null
  formModalVisible.value = true
}

const handleEdit = (record: PurchaseExchange) => {
  currentRecord.value = record
  formModalVisible.value = true
}

const handleView = (record: PurchaseExchange) => {
  currentRecord.value = record
  detailModalVisible.value = true
}

const handleSubmit = (record: PurchaseExchange) => {
  Modal.confirm({
    title: '提交确认',
    content: `确认提交换货单 ${record.exchangeNo} 吗？`,
    okText: '确认提交',
    cancelText: '取消',
    onOk: async () => {
      try {
        await purchaseExchangeApi.submit(record.id)
        message.success('提交成功')
        fetchData()
      } catch (error) {
        console.warn('[采购换货] 提交失败', error)
        message.error('提交失败')
      }
    }
  })
}

const handleApprove = (record: PurchaseExchange) => {
  currentRecord.value = record
  approveModalVisible.value = true
}

const handleTrack = (record: PurchaseExchange) => {
  currentRecord.value = record
  trackModalVisible.value = true
}

const handleDelete = (record: PurchaseExchange) => {
  Modal.confirm({
    title: '删除确认',
    content: `确定要删除换货单 ${record.exchangeNo} 吗？此操作不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    onOk: async () => {
      try {
        await purchaseExchangeApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch (error) {
        console.warn('[采购换货] 删除失败', error)
        message.error('删除失败')
      }
    }
  })
}

const handleExport = () => {
  const headers = ['换货单号', '原采购订单', '供应商', '换货日期', '换货类型', '换货金额', '状态', '创建人', '创建时间']
  const rows = dataSource.value.map((row: PurchaseExchange) => [
    row.exchangeNo || '', row.originalOrderNo || '', row.supplierName || '', row.exchangeDate || '',
    getExchangeTypeText(row.exchangeType), row.totalAmount?.toFixed(2) || '',
    getStatusText(row.status), row.createdByName || '', row.createTime || ''
  ])
  exportCsv(headers, rows, '采购换货单')
}

const handleFormSuccess = () => {
  formModalVisible.value = false
  fetchData()
}

const handleApproveSuccess = () => {
  approveModalVisible.value = false
  fetchData()
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown)
  fetchData()
  loadSuppliers()
  window.addEventListener("erp:create", handleParentCreate)
  window.addEventListener("erp:refresh", fetchData)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    if (autoRefreshEnabled.value) {
      fetchData()
      autoRefreshCountdown.value = 30
    }
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshEnabled.value && autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
  window.removeEventListener("erp:create", handleParentCreate)
  window.removeEventListener("erp:refresh", fetchData)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

async function loadSuppliers() {
  try {
    const res = await supplierApi.page({ pageSize: 200, pageNum: 1 })
    const pageData = (res as any).data ?? res
    supplierOptions.value = (pageData.records || []).map((s: any) => ({ id: s.id, name: s.supplierName }))
    searchFields[2].options = supplierOptions.value.map(s => ({ label: s.name, value: s.id }))
  } catch (e) {
    console.warn('[采购换货] 加载供应商选项失败', e)
    supplierOptions.value = []
  }
}

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.purchase-exchange-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.purchase-exchange-header__left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.purchase-exchange-header__breadcrumb {
  font-size: 12px;
  color: #999;
}

.purchase-exchange-header__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.purchase-exchange-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #52c41a;
  white-space: nowrap;
}

.data-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.update-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}

.search-area {
  margin-bottom: 16px;
}

.action-area {
  margin-bottom: 16px;
}

/* 统计卡片样式 */
.summary-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.summary-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.summary-card.highlight {
  background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%);
  border: 1px solid #d3adf7;
}

.summary-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  margin-right: 16px;
}

.summary-content {
  flex: 1;
}

.summary-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}

.summary-value.warning {
  color: #faad14;
}

/* 表格容器自动撑满 */
:deep(.vxe-table-list-container) {
  flex: 1;
  min-height: 0;
}

.table-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 48px 0;
}

.table-empty-icon {
  font-size: 48px;
  color: #d9d9d9;
  margin-bottom: 12px;
}

.table-empty-text {
  color: #999;
  margin-bottom: 16px;
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
