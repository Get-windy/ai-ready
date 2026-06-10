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
          <span v-if="autoRefreshCountdown > 0" class="page-header__countdown">{{ autoRefreshCountdown }}s后自动刷新</span>
          <a-space :size="8">
            <a-tooltip title="导出">
              <a-button size="small" @click="handleExport">
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
          <a-textarea v-model:value="approveForm.comment" :rows="3" placeholder="请输入审批意见" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
defineOptions({ name: 'ExpenseApprovalCenter' })

import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { ReloadOutlined, ExportOutlined } from '@ant-design/icons-vue'
import { PageContainer, SearchBar } from '@/components'
import type { SearchField } from '@/components'
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

const searchFields: SearchField[] = [
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

const vxeColumns = computed(() => {
  const base = [
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

async function fetchStats(): Promise<void> {
  try {
    const [appRes, reimbRes] = await Promise.all([
      feeApplicationApi.page({ pageNum: 1, pageSize: 1, status: 'SUBMITTED' }),
      feeReimbursementApi.page({ pageNum: 1, pageSize: 1, status: 'SUBMITTED' })
    ])
    const appTotal = appRes.data.total || 0
    const reimbTotal = reimbRes.data.total || 0
    stats.pendingCount = appTotal + reimbTotal
    stats.totalCount = appTotal + reimbTotal
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
</style>
