<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <span class="page-header__breadcrumb">ERP / 费用管理 / 付款确认</span>
          <h2 class="page-header__title">付款确认</h2>
        </div>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="page-header__update-time">更新于: {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" @click="debounceClick('refresh', fetchData)">
            <ReloadOutlined /> 刷新
          </a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            <span class="shortcut-hint"><kbd>Ctrl</kbd>+<kbd>N</kbd> 新增</span>
            <span class="shortcut-hint"><kbd>Ctrl</kbd>+<kbd>E</kbd> 导出</span>
          </span>
        </div>
      </div>
    </template>

    <!-- 骨架加载 -->
    <a-skeleton :loading="refreshLoading" active :paragraph="{ rows: 8 }">
    </a-skeleton>

    <template v-if="!refreshLoading">
    <!-- 统计卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :xs="12" :sm="12" :md="6">
        <a-card size="small" :bordered="true">
          <StatCard title="待付款" :value="stats.pendingCount" :amount="stats.pendingAmount" color="#faad14" />
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <a-card size="small" :bordered="true">
          <StatCard title="已付款" :value="stats.completedCount" :amount="stats.completedAmount" color="#52c41a" />
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <a-card size="small" :bordered="true">
          <StatCard title="失败" :value="stats.failedCount" :amount="stats.failedAmount" color="#ff4d4f" />
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <a-card size="small" :bordered="true">
          <StatCard title="合计金额" :value="stats.totalCount" :amount="stats.totalAmount" color="#1890ff" />
        </a-card>
      </a-col>
    </a-row>

    <!-- 搜索栏 -->
    <SearchBar
      :fields="searchFields"
      :loading="loading"
      v-model="searchForm"
      :expandable="false"
      :show-result-count="true"
      show-clear
      :total="pagination.total"
      @search="handleSearch"
      @reset="handleReset"
      @clear="handleClear"
    />

    <div class="toolbar">
      <a-space>
        <a-select v-model:value="typeFilter" placeholder="业务类型" style="width: 120px" size="small" allow-clear @change="fetchData">
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="APPLICATION">费用申请</a-select-option>
          <a-select-option value="REIMBURSEMENT">费用报销</a-select-option>
        </a-select>
        <a-select v-model:value="statusFilter" placeholder="状态" style="width: 100px" size="small" allow-clear @change="fetchData">
          <a-select-option value="">全部</a-select-option>
          <a-select-option value="PENDING">待付款</a-select-option>
          <a-select-option value="COMPLETED">已付款</a-select-option>
          <a-select-option value="FAILED">失败</a-select-option>
          <a-select-option value="CANCELLED">已取消</a-select-option>
        </a-select>
        <a-tooltip title="导出">
          <a-button v-permission="'erp:expense:payment:list'" size="small" @click="debounceClick('export', handleExport)">
            <template #icon><ExportOutlined /></template>
          </a-button>
        </a-tooltip>
      </a-space>
      <a-button v-permission="'erp:expense:payment:create'" type="primary" size="small" @click="showCreateModal">
        <PlusOutlined /> 新增付款
      </a-button>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="dataList"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      :show-toolbar="false"
      :show-add="false"
      :show-search="false"
      :min-empty-rows="12"
      @page-change="onPageChange"
    >
      <template #empty>
        <EmptyState v-if="!loading" title="暂无数据" description="暂无付款记录" size="small" :show-actions="false" />
      </template>
      <template #statusCell="{ record }">
        <StatusTag :status="record.status" :map="EXPENSE_PAYMENT_STATUS" />
      </template>
      <template #action="{ record }">
        <a-space :size="4">
          <PrintButton :record="record" :business-id="record.id" business-type="expense_payment" button-type="link" button-size="small" tooltip="打印" />
          <a-button type="link" size="small" @click="viewDetail(record)">查看</a-button>
          <a-button v-if="record.status === 'PENDING'" v-permission="'erp:expense:payment:confirm'" type="primary" size="small" :loading="confirmingId === record.id" @click="handleConfirm(record.id)">确认付款</a-button>
          <a-button v-if="record.status === 'PENDING'" v-permission="'erp:expense:payment:cancel'" type="link" size="small" danger @click="confirmCancel(record.id)">取消</a-button>
        </a-space>
      </template>
    </VxeTableList>

    <!-- 新增付款弹窗 -->
    <a-modal v-model:open="createModalVisible" title="新增付款" width="560px" :confirm-loading="createLoading" :destroy-on-close="true" @ok="handleCreateOk" @cancel="handleCreateCancel">
      <a-form ref="createFormRef" :model="createForm" :rules="createRules" :label-col="{ span: 5 }" :wrapper-col="{ span: 17 }">
        <a-form-item label="业务类型" name="businessType">
          <a-select v-model:value="createForm.businessType" size="small" placeholder="请选择业务类型" @change="handleBusinessTypeChange">
            <a-select-option value="APPLICATION">费用申请</a-select-option>
            <a-select-option value="REIMBURSEMENT">费用报销</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="业务单据" name="businessId">
          <a-select v-model:value="createForm.businessId" size="small" placeholder="请选择业务单据" :options="businessOptions" show-search :filter-option="(input: string, option: any) => option.label?.includes(input)" :loading="businessLoading" />
        </a-form-item>
        <a-form-item label="收款人" name="payeeName">
          <a-input v-model:value="createForm.payeeName" size="small" placeholder="请输入收款人" />
        </a-form-item>
        <a-form-item label="付款金额" name="paymentAmount">
          <a-input-number v-model:value="createForm.paymentAmount" size="small" :min="0.01" :precision="2" style="width: 100%" placeholder="请输入付款金额" />
        </a-form-item>
        <a-form-item label="支付方式" name="paymentMethod">
          <a-select v-model:value="createForm.paymentMethod" size="small" placeholder="请选择支付方式">
            <a-select-option value="BANK_TRANSFER">银行转账</a-select-option>
            <a-select-option value="CASH">现金</a-select-option>
            <a-select-option value="CHECK">支票</a-select-option>
            <a-select-option value="ALIPAY">支付宝</a-select-option>
            <a-select-option value="WECHAT">微信</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="付款日期" name="paymentDate">
          <a-date-picker v-model:value="createForm.paymentDate" size="small" style="width: 100%" placeholder="请选择付款日期" />
        </a-form-item>
        <a-form-item label="备注" name="remark">
          <a-textarea v-model:value="createForm.remark" size="small" :rows="2" placeholder="备注信息（选填）" />
        </a-form-item>
      </a-form>
    </a-modal>
    </template>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
defineOptions({ name: 'ExpensePaymentConfirm' })

import { ref, computed, reactive, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import type { FormInstance } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, ExportOutlined, SyncOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import StatCard from '@/components/business/StatCard/StatCard.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import EmptyState from '@/components/EmptyState/EmptyState.vue'
import { EXPENSE_PAYMENT_STATUS } from '@/utils/statusConfig'
import dayjs from 'dayjs'
import request from '@/utils/request'
import { feePaymentApi, feeApplicationApi, feeReimbursementApi } from '@/api/erp/expense'
import type { FeePaymentRecord } from '@/api/erp/expense'

// ── 类型定义 ─────────────────────────────────────────────

interface PaymentStats {
  pendingCount: number
  pendingAmount: number
  completedCount: number
  completedAmount: number
  failedCount: number
  failedAmount: number
  totalCount: number
  totalAmount: number
}

interface SearchParams {
  keyword: string
  dateRange: string[] | null
}

interface CreatePaymentForm {
  businessType: string
  businessId: number | undefined
  payeeName: string
  paymentAmount: number | undefined
  paymentMethod: string
  paymentDate: string | undefined
  remark: string
}

// ── 格式化 ───────────────────────────────────────────────

function formatAmount(v: unknown): string {
  return v != null ? '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '¥0.00'
}

// ── 状态管理 ─────────────────────────────────────────────

const router = useRouter()
const loading = ref(false)
const refreshLoading = ref(false)
const dataList = ref<FeePaymentRecord[]>([])
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)

const stats = reactive<PaymentStats>({
  pendingCount: 0, pendingAmount: 0,
  completedCount: 0, completedAmount: 0,
  failedCount: 0, failedAmount: 0,
  totalCount: 0, totalAmount: 0
})

const typeFilter = ref('')
const statusFilter = ref('')
const tableRef = ref()
const pagination = reactive({
  current: 1, pageSize: 20, total: 0,
  showSizeChanger: true, showQuickJumper: true,
  pageSizeOptions: ['10', '20', '50', '100']
})

const searchForm = reactive<SearchParams>({
  keyword: '',
  dateRange: null
})

const searchFields: any = [
  {
    name: 'keyword',
    label: '关键词',
    type: 'input',
    placeholder: '付款单号 / 业务单号 / 收款人',
    allowClear: true,
    initial: ''
  },
  {
    name: 'dateRange',
    label: '付款日期',
    type: 'date-range',
    allowClear: true
  }
]

// ── 创建付款 ─────────────────────────────────────────────

const createModalVisible = ref(false)
const createLoading = ref(false)
const createFormRef = ref<FormInstance>()
const businessLoading = ref(false)
const businessOptions = ref<Array<{ label: string; value: number }>>([])
const formDirty = ref(false)

const createForm = reactive<CreatePaymentForm>({
  businessType: '',
  businessId: undefined,
  payeeName: '',
  paymentAmount: undefined,
  paymentMethod: 'BANK_TRANSFER',
  paymentDate: undefined,
  remark: ''
})

const createRules: Record<string, unknown> = {
  businessType: [{ required: true, message: '请选择业务类型' }],
  businessId: [{ required: true, message: '请选择业务单据', type: 'number' }],
  payeeName: [{ required: true, message: '请输入收款人', max: 50 }],
  paymentAmount: [{ required: true, message: '请输入付款金额', type: 'number', min: 0.01 }],
  paymentMethod: [{ required: true, message: '请选择支付方式' }],
  paymentDate: [{ required: true, message: '请选择付款日期' }]
}

async function loadBusinessOptions(): Promise<void> {
  if (!createForm.businessType) {
    businessOptions.value = []
    return
  }
  businessLoading.value = true
  try {
    const api = createForm.businessType === 'APPLICATION' ? feeApplicationApi : feeReimbursementApi
    const res = await api.page({ pageNum: 1, pageSize: 200, status: 'APPROVED' })
    const records = res.records || []
    businessOptions.value = records.map((item: any) => ({
      label: `${item.applicationNo || item.reimbursementNo} - ${item.applicationTitle || item.reimbursementTitle || ''}`,
      value: item.id
    }))
  } catch {
    message.error('加载业务单据失败')
  } finally {
    businessLoading.value = false
  }
}

function handleBusinessTypeChange(): void {
  createForm.businessId = undefined
  loadBusinessOptions()
}

function showCreateModal(): void {
  createForm.businessType = ''
  createForm.businessId = undefined
  createForm.payeeName = ''
  createForm.paymentAmount = undefined
  createForm.paymentMethod = 'BANK_TRANSFER'
  createForm.paymentDate = undefined
  createForm.remark = ''
  formDirty.value = false
  createModalVisible.value = true
}

function onFormChange(): void {
  formDirty.value = true
}

async function handleCreateOk(): Promise<void> {
  try {
    await createFormRef.value?.validate()
  } catch {
    return
  }
  createLoading.value = true
  try {
    const payload: Record<string, unknown> = {
      businessType: createForm.businessType,
      businessId: createForm.businessId,
      payeeName: createForm.payeeName,
      paymentAmount: createForm.paymentAmount,
      paymentMethod: createForm.paymentMethod,
      paymentDate: createForm.paymentDate ? dayjs(createForm.paymentDate).format('YYYY-MM-DD') : undefined,
      remark: createForm.remark || undefined
    }
    await feePaymentApi.create(payload)
    message.success('付款单创建成功')
    createModalVisible.value = false
    fetchData()
    fetchStats()
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '未知错误'
    message.error(`创建失败: ${msg}`)
  } finally {
    createLoading.value = false
  }
}

function handleCreateCancel(): void {
  createModalVisible.value = false
}

// ── 表单脏追踪 ─────────────────────────────────────────────

watch(createForm, () => {
  if (createModalVisible.value) {
    formDirty.value = true
  }
}, { deep: true })

const confirmingId = ref<number | null>(null)

// ── 列定义 ───────────────────────────────────────────────

const vxeColumns: any = computed(() => [
  { field: 'paymentNo', title: '付款单号', width: 160 },
  { field: 'businessNo', title: '业务单号', width: 150 },
  { field: 'businessType', title: '类型', width: 80 },
  { field: 'paymentAmount', title: '付款金额', width: 120, align: 'right', formatter: ({ cellValue }: { cellValue: unknown }) => formatAmount(cellValue) },
  { field: 'paymentDate', title: '付款日期', width: 100 },
  { field: 'payeeName', title: '收款人', width: 100 },
  { field: 'paymentMethod', title: '支付方式', width: 90 },
  { field: 'status', title: '状态', width: 80, align: 'center', slots: { default: 'statusCell' } },
  { field: '_action', title: '操作', width: 180, fixed: 'right', slots: { default: 'action' } }
])

// ── 数据加载 ─────────────────────────────────────────────

async function fetchData(): Promise<void> {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    if (typeFilter.value) params.businessType = typeFilter.value
    if (statusFilter.value) params.status = statusFilter.value
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.dateStart = searchForm.dateRange[0]
      params.dateEnd = searchForm.dateRange[1]
    }
    const res = await feePaymentApi.page(params)
    const data = res.data
    dataList.value = data.records || []
    pagination.total = data.total || 0
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
    autoRefreshCountdown.value = 300
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '未知错误'
    message.error(`加载失败: ${msg}`)
  } finally {
    loading.value = false
  }
}

function onPageChange(page: number, pageSize: number): void {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

async function handleRefresh(): Promise<void> {
  refreshLoading.value = true
  try {
    await Promise.all([fetchData(), fetchStats()])
  } finally {
    refreshLoading.value = false
  }
}

async function fetchStats(): Promise<void> {
  try {
    const [pendingRes, completedRes, failedRes] = await Promise.all([
      feePaymentApi.page({ pageNum: 1, pageSize: 1, status: 'PENDING' }),
      feePaymentApi.page({ pageNum: 1, pageSize: 1, status: 'COMPLETED' }),
      feePaymentApi.page({ pageNum: 1, pageSize: 1, status: 'FAILED' })
    ])
    stats.pendingCount = pendingRes.data.total || 0
    stats.completedCount = completedRes.data.total || 0
    stats.failedCount = failedRes.data.total || 0
    stats.totalCount = stats.pendingCount + stats.completedCount + stats.failedCount
  } catch {
    // 静默处理，统计数据非关键
  }
}

// ── 搜索交互 ─────────────────────────────────────────────

function handleSearch(formData: SearchParams): void {
  Object.assign(searchForm, formData)
  pagination.current = 1
  fetchData()
}

function handleReset(): void {
  searchForm.keyword = ''
  searchForm.dateRange = null
  pagination.current = 1
  fetchData()
}

function handleClear(): void {
  searchForm.keyword = ''
  searchForm.dateRange = null
  pagination.current = 1
  fetchData()
}

// ── 导出 ─────────────────────────────────────────────────

async function handleExport(): Promise<void> {
  try {
    const blob = await request.get('/erp/expense/payment/export', {
      params: {
        businessType: typeFilter.value || undefined,
        status: statusFilter.value || undefined
      },
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `付款确认_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch {
    message.error('导出失败')
  }
}

// ── 操作交互 ─────────────────────────────────────────────

function viewDetail(record: FeePaymentRecord): void {
  if (record.businessType === 'APPLICATION') {
    router.push(`/erp/expense/application/${record.businessId}`)
  } else if (record.businessType === 'REIMBURSEMENT') {
    router.push(`/erp/expense/reimbursement/${record.businessId}`)
  } else {
    message.info(`付款单: ${record.paymentNo}`)
  }
}

async function handleConfirm(id: number): Promise<void> {
  confirmingId.value = id
  try {
    await feePaymentApi.confirmPayment(id)
    message.success('付款确认成功')
    fetchData()
    fetchStats()
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '未知错误'
    message.error(`确认失败: ${msg}`)
  } finally {
    confirmingId.value = null
  }
}

function confirmCancel(id: number): void {
  Modal.confirm({
    title: '确认取消',
    content: '确定要取消该付款单吗？',
    okText: '确认取消',
    okType: 'danger',
    onOk: async () => {
      try {
        await feePaymentApi.cancelPayment(id, '手动取消')
        message.success('已取消')
        fetchData()
        fetchStats()
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : '未知错误'
        message.error(`取消失败: ${msg}`)
      }
    }
  })
}

// ── 工具 ─────────────────────────────────────────────────

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300): void {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

function handleError(err: unknown): void {
  console.warn('[ExpensePayment]', err)
}

function handleKeydown(e: KeyboardEvent): void {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    showCreateModal()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') {
    e.preventDefault()
    handleExport()
  }
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault()
    debounceClick('refresh', fetchData)
  }
}

function startCountdown(): void {
  autoRefreshCountdown.value = 300
  countdownTimer = window.setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
}

let refreshTimer: ReturnType<typeof setInterval>
let countdownTimer: number | undefined

onMounted(() => {
  fetchData()
  fetchStats()
  refreshTimer = setInterval(fetchData, 300000)
  startCountdown()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.page-header__left {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.page-header__breadcrumb {
  font-size: 12px;
  color: #999;
}
.page-header__title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.page-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.page-header__update-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}
.page-header__countdown {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0 12px 0;
  flex-wrap: wrap;
  gap: 8px;
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

/* ── 自动刷新徽章 ────────────────────────────── */
.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #52c41a;
  white-space: nowrap;
}

/* ── vxe-table 表头 2px 边框 ──────────────────── */
:deep(.vxe-header--row) {
  border-top: 2px solid #e8e8e8;
}
:deep(.vxe-header--column) {
  border-bottom: 2px solid #e8e8e8 !important;
}

/* ── 空状态包装样式 ────────────────────────────── */
:deep(.empty-state-wrapper) {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

</style>
