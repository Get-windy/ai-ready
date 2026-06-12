<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <span class="page-header__breadcrumb">ERP / 费用管理 / 审批中心</span>
          <h2 class="page-header__title">审批中心</h2>
        </div>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="page-header__update-time">更新于: {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-space :size="8">
            <span class="shortcut-hints">
              <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
              <span class="shortcut-hint"><kbd>Ctrl</kbd>+<kbd>E</kbd> 导出</span>
            </span>
            <a-tooltip title="导出">
              <a-button size="small" @click="debounceClick('export', handleExport)">
                <template #icon><ExportOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-button size="small" @click="debounceClick('refresh', fetchData)">
              <ReloadOutlined /> 刷新
            </a-button>
          </a-space>
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
          <StatCard title="待审批" :value="stats.pendingCount" :amount="stats.pendingAmount" color="#faad14" />
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <a-card size="small" :bordered="true">
          <StatCard title="已通过" :value="stats.approvedCount" :amount="stats.approvedAmount" color="#52c41a" />
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <a-card size="small" :bordered="true">
          <StatCard title="已拒绝" :value="stats.rejectedCount" :amount="stats.rejectedAmount" color="#ff4d4f" />
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <a-card size="small" :bordered="true">
          <StatCard title="合计笔数" :value="stats.totalCount" :amount="stats.totalAmount" color="#1890ff" />
        </a-card>
      </a-col>
    </a-row>

    <a-tabs v-model:activeKey="activeTab" @change="handleTabChange">
      <a-tab-pane key="application" tab="费用申请待审批" />
      <a-tab-pane key="reimbursement" tab="费用报销待审批" />
    </a-tabs>

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
      <template #statusCell="{ record }">
        <StatusTag :status="record.status" :map="EXPENSE_APPROVAL_STATUS" />
      </template>
      <template #empty>
        <EmptyState v-if="!loading" title="暂无数据" :description="activeTab === 'application' ? '暂无待审批费用申请' : '暂无待审批费用报销'" size="small" :show-actions="false" />
      </template>
      <template #action="{ record }">
        <a-space :size="4">
          <PrintButton :record="record" :business-id="record.id" :business-type="activeTab === 'application' ? 'expense_application' : 'expense_reimbursement'" button-type="link" button-size="small" tooltip="打印" />
          <a-button type="link" size="small" @click="viewDetail(record)">查看</a-button>
          <a-button v-permission="'erp:expense:approval:process'" type="primary" size="small" @click="showApprove(record)">审批</a-button>
        </a-space>
      </template>
    </VxeTableList>

    <!-- 审批弹窗 -->
    <a-modal v-model:open="approveModalVisible" title="审批处理" width="500px" :confirm-loading="approveLoading" @ok="handleApproveOk" @cancel="handleApproveCancel">
      <a-form :model="approveForm" :label-col="{ span: 4 }" :wrapper-col="{ span: 18 }">
        <a-form-item label="当前单据">
          <span>{{ currentRecord?.applicationTitle || currentRecord?.reimbursementTitle }}</span>
        </a-form-item>
        <a-form-item label="审批结果">
          <a-radio-group v-model:value="approveForm.action">
            <a-radio value="APPROVE">通过</a-radio>
            <a-radio value="REJECT">拒绝</a-radio>
            <a-radio value="RETURN">退回</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="审批意见">
          <a-textarea v-model:value="approveForm.comment" :rows="3" placeholder="请输入审批意见" size="small" />
        </a-form-item>
      </a-form>
    </a-modal>
    </template>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
defineOptions({ name: 'ExpenseApprovalCenter' })

import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { ReloadOutlined, ExportOutlined, SyncOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import StatCard from '@/components/business/StatCard/StatCard.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import EmptyState from '@/components/EmptyState/EmptyState.vue'
import { EXPENSE_APPROVAL_STATUS } from '@/utils/statusConfig'
import request from '@/utils/request'
import { feeApplicationApi, feeReimbursementApi, feeApprovalApi } from '@/api/erp/expense'
import type { FeeApplication, FeeReimbursement } from '@/api/erp/expense'

// ── 类型定义 ─────────────────────────────────────────────

interface ApprovalStats {
  pendingCount: number
  pendingAmount: number
  approvedCount: number
  approvedAmount: number
  rejectedCount: number
  rejectedAmount: number
  totalCount: number
  totalAmount: number
}

interface ApproveFormData {
  action: 'APPROVE' | 'REJECT' | 'RETURN'
  comment: string
}

interface SearchParams {
  keyword: string
  dateRange: string[] | null
}

type ApprovalRecord = (FeeApplication | FeeReimbursement) & {
  applicationTitle?: string
  reimbursementTitle?: string
}

// ── 格式化工具 ───────────────────────────────────────────

function formatAmount(v: unknown): string {
  return v != null ? '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '¥0.00'
}

// ── 状态管理 ─────────────────────────────────────────────

const router = useRouter()
const loading = ref(false)
const refreshLoading = ref(false)
const dataList = ref<ApprovalRecord[]>([])
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)

const stats = reactive<ApprovalStats>({
  pendingCount: 0, pendingAmount: 0,
  approvedCount: 0, approvedAmount: 0,
  rejectedCount: 0, rejectedAmount: 0,
  totalCount: 0, totalAmount: 0
})

const activeTab = ref<'application' | 'reimbursement'>('application')
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
    placeholder: '单号 / 标题 / 申请人',
    allowClear: true,
    initial: ''
  },
  {
    name: 'dateRange',
    label: '申请日期',
    type: 'date-range',
    allowClear: true
  }
]

// ── 审批弹窗 ─────────────────────────────────────────────

const approveModalVisible = ref(false)
const approveLoading = ref(false)
const currentRecord = ref<ApprovalRecord | null>(null)
const approveForm = reactive<ApproveFormData>({ action: 'APPROVE', comment: '' })

// ── 列定义 ───────────────────────────────────────────────

const vxeColumns = computed((): any => {
  const base: any[] = [
    { field: 'applicationNo', title: '单号', width: 160 },
    { field: 'applicantName', title: '申请人', width: 80 },
    { field: 'departmentName', title: '部门', width: 100 },
    { field: 'totalAmount', title: '金额', width: 120, align: 'right', formatter: ({ cellValue }: { cellValue: unknown }) => formatAmount(cellValue) },
    { field: 'applyDate', title: '申请日期', width: 100 },
    { field: 'status', title: '状态', width: 80, align: 'center', slots: { default: 'statusCell' } },
    { field: '_action', title: '操作', width: 130, fixed: 'right', slots: { default: 'action' } }
  ]
  if (activeTab.value === 'application') {
    base.unshift({ field: 'applicationTitle', title: '申请标题', width: 200, minWidth: 140 })
  } else {
    base.unshift({ field: 'reimbursementTitle', title: '报销标题', width: 200, minWidth: 140 })
  }
  return base
})

// ── 数据加载 ─────────────────────────────────────────────

async function fetchData(): Promise<void> {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      status: 'SUBMITTED'
    }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.dateStart = searchForm.dateRange[0]
      params.dateEnd = searchForm.dateRange[1]
    }

    if (activeTab.value === 'application') {
      const res = await feeApplicationApi.page(params)
      const data = res.data
      dataList.value = data.records || []
      pagination.total = data.total || 0
    } else {
      const res = await feeReimbursementApi.page(params)
      const data = res.data
      dataList.value = data.records || []
      pagination.total = data.total || 0
    }
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
    autoRefreshCountdown.value = 300
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : '未知错误'
    message.error(`加载失败: ${msg}`)
  } finally {
    loading.value = false
  }
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
    const pageParams = { pageNum: 1, pageSize: 1 } as Record<string, unknown>
    const [pendingApp, pendingReimb, approvedApp, approvedReimb, rejectedApp, rejectedReimb] = await Promise.all([
      feeApplicationApi.page({ ...pageParams, status: 'SUBMITTED' }),
      feeReimbursementApi.page({ ...pageParams, status: 'SUBMITTED' }),
      feeApplicationApi.page({ ...pageParams, status: 'APPROVED' }),
      feeReimbursementApi.page({ ...pageParams, status: 'APPROVED' }),
      feeApplicationApi.page({ ...pageParams, status: 'REJECTED' }),
      feeReimbursementApi.page({ ...pageParams, status: 'REJECTED' })
    ])
    stats.pendingCount = (pendingApp.data.total || 0) + (pendingReimb.data.total || 0)
    stats.approvedCount = (approvedApp.data.total || 0) + (approvedReimb.data.total || 0)
    stats.rejectedCount = (rejectedApp.data.total || 0) + (rejectedReimb.data.total || 0)
    stats.totalCount = stats.pendingCount + stats.approvedCount + stats.rejectedCount
    // 金额统计由各自业务接口返回，此处使用简化的计数展示
  } catch {
    // 静默处理，统计数据非关键
  }
}

function onPageChange(page: number, pageSize: number): void {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
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

function handleTabChange(): void {
  pagination.current = 1
  fetchData()
}

// ── 导出 ─────────────────────────────────────────────────

async function handleExport(): Promise<void> {
  try {
    const api = activeTab.value === 'application' ? '/erp/expense/application/export' : '/erp/expense/reimbursement/export'
    const blob = await request.get(api, {
      params: { status: 'SUBMITTED' },
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${activeTab.value === 'application' ? '费用申请' : '费用报销'}_待审批_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch {
    message.error('导出失败')
  }
}

// ── 审批交互 ─────────────────────────────────────────────

function viewDetail(record: ApprovalRecord): void {
  if (activeTab.value === 'application') {
    router.push(`/erp/expense/application/${record.id}`)
  } else {
    router.push(`/erp/expense/reimbursement/${record.id}`)
  }
}

function showApprove(record: ApprovalRecord): void {
  currentRecord.value = record
  approveForm.action = 'APPROVE'
  approveForm.comment = ''
  approveModalVisible.value = true
}

async function handleApproveOk(): Promise<void> {
  Modal.confirm({
    title: '确认审批',
    content: `确定要${approveForm.action === 'APPROVE' ? '通过' : approveForm.action === 'REJECT' ? '拒绝' : '退回'}该申请吗？`,
    okText: '确认',
    onOk: async () => {
      approveLoading.value = true
      try {
        const businessType = activeTab.value === 'application' ? 'APPLICATION' : 'REIMBURSEMENT'
        await feeApprovalApi.process({
          businessType,
          businessId: currentRecord.value!.id,
          action: approveForm.action,
          comment: approveForm.comment
        })
        message.success('审批完成')
        approveModalVisible.value = false
        fetchData()
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : '未知错误'
        message.error(`审批失败: ${msg}`)
      } finally {
        approveLoading.value = false
      }
    }
  })
}

function handleApproveCancel(): void {
  approveModalVisible.value = false
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
  console.warn('[ExpenseApproval]', err)
}

function handleKeydown(e: KeyboardEvent): void {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault()
    // 审批中心无创建功能
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') {
    e.preventDefault()
    handleExport()
  }
  if (e.key === 'F5' && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
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

/* ── 更新时间 ─────────────────────────────────── */
.update-time {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
}</style>
