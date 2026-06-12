<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <span class="page-header__breadcrumb">ERP / 费用管理 / 费用报销</span>
          <h2 class="page-header__title">费用报销</h2>
        </div>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" class="page-header__update-time">更新于: {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-space :size="12">
            <span class="shortcut-hints">
              <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
              <span class="shortcut-hint"><kbd>Ctrl</kbd>+<kbd>N</kbd> 新增</span>
              <span class="shortcut-hint"><kbd>Ctrl</kbd>+<kbd>E</kbd> 导出</span>
            </span>
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
        <a-card size="small">
          <stat-card title="报销总数" :value="stats.totalCount" :amount="stats.totalAmount" color="#1890ff" />
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <a-card size="small">
          <stat-card title="待审批" :value="stats.pendingCount" :amount="stats.pendingAmount" color="#faad14" />
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <a-card size="small">
          <stat-card title="已通过" :value="stats.approvedCount" :amount="stats.approvedAmount" color="#52c41a" />
        </a-card>
      </a-col>
      <a-col :xs="12" :sm="12" :md="6">
        <a-card size="small">
          <stat-card title="已拒绝" :value="stats.rejectedCount" :amount="stats.rejectedAmount" color="#ff4d4f" />
        </a-card>
      </a-col>
    </a-row>

    <div class="expense-layout">
      <div class="expense-sidebar">
        <div class="sidebar-section">
          <div class="sidebar-title">筛选</div>
          <a-menu
            :selected-keys="[activeFilter]"
            mode="inline"
            @click="(e) => { activeFilter = String(e.key); pagination.current = 1; fetchData() }"
          >
            <a-menu-item key="all">全部报销</a-menu-item>
            <a-menu-item key="DRAFT">草稿</a-menu-item>
            <a-menu-item key="SUBMITTED">待审批</a-menu-item>
            <a-menu-item key="APPROVED">已通过</a-menu-item>
            <a-menu-item key="REJECTED">已拒绝</a-menu-item>
          </a-menu>
        </div>
      </div>

      <div class="expense-main">
        <div class="expense-toolbar">
          <a-space>
            <SearchBar
              :fields="searchFields"
              :loading="loading"
              @search="handleSearch"
              @reset="handleReset"
            />
            <a-tooltip title="导出">
              <a-button v-permission="'erp:expense:reimbursement:list'" size="small" @click="debounceClick('export', handleExport)">
                <template #icon><ExportOutlined /></template>
              </a-button>
            </a-tooltip>
          </a-space>
          <a-space>
            <a-button v-permission="'erp:expense:reimbursement:create'" type="primary" size="small" @click="showCreate">
              <PlusOutlined /> 新增报销
            </a-button>
          </a-space>
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
          <template #statusCell="{ record }">
            <StatusTag :status="record.status" :map="EXPENSE_REIMBURSEMENT_STATUS" />
          </template>
          <template #action="{ record }">
            <a-space :size="4">
              <PrintButton :record="record" :business-id="record.id" business-type="expense_reimbursement" button-type="link" button-size="small" tooltip="打印" />
              <a-button type="link" size="small" @click="viewDetail(record.id)">查看</a-button>
              <a-button v-if="record.status === 'DRAFT'" v-permission="'erp:expense:reimbursement:edit'" type="link" size="small" @click="editItem(record.id)">编辑</a-button>
              <a-button v-if="record.status === 'DRAFT'" v-permission="'erp:expense:reimbursement:submit'" type="link" size="small" @click="handleSubmit(record.id)">提交</a-button>
              <a-button v-if="record.status === 'DRAFT'" v-permission="'erp:expense:reimbursement:delete'" type="link" size="small" danger @click="confirmDelete(record.id)">删除</a-button>
            </a-space>
          </template>
          <template #empty>
            <EmptyState v-if="!loading" title="暂无数据" description="暂无报销记录" size="small" :show-actions="false" />
          </template>
        </VxeTableList>
      </div>
    </div>
    </template>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
defineOptions({ name: 'ExpenseReimbursementList' })

import { ref, computed, onMounted, onUnmounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, ExportOutlined, SyncOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import SearchBar from '@/components/SearchBar/SearchBar.vue'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import { EXPENSE_REIMBURSEMENT_STATUS } from '@/utils/statusConfig'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import EmptyState from '@/components/EmptyState/EmptyState.vue'
import request from '@/utils/request'
import { feeReimbursementApi } from '@/api/erp/expense'

const router = useRouter()
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

function formatAmount(v: any): string {
  return v != null ? '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '¥0.00'
}

const loading = ref(false)
const refreshLoading = ref(false)
const dataList = ref<any[]>([])

const stats = reactive({
  totalCount: 0, totalAmount: 0,
  pendingCount: 0, pendingAmount: 0,
  approvedCount: 0, approvedAmount: 0,
  rejectedCount: 0, rejectedAmount: 0
})

interface StatCardProps { title: string; value: number; amount: number; color: string }
const StatCard = {
  props: ['title', 'value', 'amount', 'color'],
  template: `
    <div>
      <div style="font-size: 13px; color: #666; margin-bottom: 4px">{{ title }}</div>
      <div style="display: flex; justify-content: space-between; align-items: baseline">
        <span style="font-size: 22px; font-weight: 600; color: #303133">{{ value }}</span>
        <span :style="{ fontSize: '16px', fontWeight: 500, color: color }">¥{{ (amount || 0).toFixed(2) }}</span>
      </div>
    </div>
  `
}
const searchFields: any = [
  { name: 'keyword', label: '标题/单号', type: 'input', placeholder: '请输入标题或单号' },
  { name: 'status', label: '状态', type: 'select', placeholder: '请选择状态', options: [
    { label: '全部', value: '' },
    { label: '草稿', value: 'DRAFT' },
    { label: '待审批', value: 'SUBMITTED' },
    { label: '审批中', value: 'APPROVING' },
    { label: '已通过', value: 'APPROVED' },
    { label: '已拒绝', value: 'REJECTED' },
    { label: '已撤回', value: 'CANCELLED' },
  ]},
]

const searchKeyword = ref('')
const statusFilter = ref('')
const activeFilter = ref('all')
const tableRef = ref()
const pagination = reactive({
  current: 1, pageSize: 20, total: 0,
  showSizeChanger: true, showQuickJumper: true,
  pageSizeOptions: ['10', '20', '50', '100']
})

const vxeColumns: any = computed(() => [
  { field: 'reimbursementNo', title: '报销单号', width: 160 },
  { field: 'reimbursementTitle', title: '报销标题', width: 200, minWidth: 140 },
  { field: 'applicantName', title: '报销人', width: 80 },
  { field: 'departmentName', title: '部门', width: 100 },
  { field: 'totalAmount', title: '金额', width: 120, align: 'right', formatter: ({ cellValue }: any) => formatAmount(cellValue) },
  { field: 'reimbursementDate', title: '报销日期', width: 100 },
  { field: 'status', title: '状态', width: 80, align: 'center', slots: { default: 'statusCell' } },
  { field: '_action', title: '操作', width: 200, fixed: 'right', slots: { default: 'action' } }
])

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (searchKeyword.value) params.reimbursementTitle = searchKeyword.value
    if (activeFilter.value !== 'all') params.status = activeFilter.value
    else if (statusFilter.value) params.status = statusFilter.value
    const res = await feeReimbursementApi.page(params)
    dataList.value = (res as any).records || (res as any).data?.records || []
    pagination.total = (res as any).total || (res as any).data?.total || 0
    lastUpdateTime.value = new Date().toLocaleString('zh-CN'); autoRefreshCountdown.value = 300
  } catch (e) {
    console.error('[费用报销] 加载失败', e)
    message.error('加载失败')
  } finally { loading.value = false }
}

async function handleRefresh() {
  refreshLoading.value = true
  try {
    await fetchData()
    await fetchStats()
  } finally {
    refreshLoading.value = false
  }
}

function handleSearch(values?: Record<string, any>) {
  if (values) {
    searchKeyword.value = values.keyword || ''
    statusFilter.value = values.status || ''
  }
  pagination.current = 1
  fetchData()
}
function handleReset() {
  searchKeyword.value = ''
  statusFilter.value = ''
  pagination.current = 1
  fetchData()
}
function onPageChange(page: number, pageSize: number) { pagination.current = page; pagination.pageSize = pageSize; fetchData() }

async function fetchStats() {
  try {
    const pageParams = { pageNum: 1, pageSize: 1 } as Record<string, unknown>
    const [totalRes, pendingRes, approvedRes, rejectedRes] = await Promise.all([
      feeReimbursementApi.page(pageParams),
      feeReimbursementApi.page({ ...pageParams, status: 'SUBMITTED' }),
      feeReimbursementApi.page({ ...pageParams, status: 'APPROVED' }),
      feeReimbursementApi.page({ ...pageParams, status: 'REJECTED' })
    ])
    const getTotal = (res: any) => res.total || res.data?.total || 0
    stats.totalCount = getTotal(totalRes)
    stats.pendingCount = getTotal(pendingRes)
    stats.approvedCount = getTotal(approvedRes)
    stats.rejectedCount = getTotal(rejectedRes)
  } catch { /* ignore */ }
}

async function handleExport() {
  try {
    const blob = await request.get('/erp/expense/reimbursement/export', {
      params: {
        reimbursementTitle: searchKeyword.value,
        status: activeFilter.value !== 'all' ? activeFilter.value : statusFilter.value || undefined
      },
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `费用报销_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch { message.error('导出失败') }
}
function showCreate() { router.push('/erp/expense/reimbursement/create') }
function viewDetail(id: number) { router.push(`/erp/expense/reimbursement/${id}`) }
function editItem(id: number) { router.push(`/erp/expense/reimbursement/${id}?edit=1`) }
async function handleSubmit(id: number) {
  try { await feeReimbursementApi.submit(id); message.success('提交成功'); fetchData() }
  catch { message.error('提交失败') }
}
function confirmDelete(id: number) {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该报销单吗？此操作不可恢复。',
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try { await feeReimbursementApi.delete(id); message.success('删除成功'); fetchData() }
      catch { message.error('删除失败') }
    }
  })
}

function handleError(err: any) { console.warn('[ExpenseReimbursement]', err) }

const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval>
let countdownTimer: number | undefined

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault(); showCreate()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') {
    e.preventDefault(); handleExport()
  }
  if (e.key === 'F5' && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault(); debounceClick('refresh', fetchData)
  }
}

function startCountdown() {
  autoRefreshCountdown.value = 300
  countdownTimer = window.setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
}
onMounted(() => { fetchData(); fetchStats(); refreshTimer = setInterval(fetchData, 300000); startCountdown(); document.addEventListener('keydown', handleKeydown) })
onUnmounted(() => { clearInterval(refreshTimer); if (countdownTimer) clearInterval(countdownTimer); document.removeEventListener('keydown', handleKeydown) })
</script>

<style scoped>
.expense-layout { display: flex; gap: 12px; height: 100%; min-height: 0; }
.expense-sidebar { width: 160px; min-width: 120px; background: #fff; border-radius: 6px; border: 1px solid #e8e8e8; overflow: hidden; }
.sidebar-section { padding: 8px 0; }
.sidebar-title { font-weight: 600; font-size: 13px; color: #303133; padding: 8px 16px; }
.expense-main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.expense-toolbar { display: flex; justify-content: space-between; align-items: center; padding: 0 0 12px 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.page-header__left { display: flex; flex-direction: column; gap: 2px; }
.page-header__breadcrumb { font-size: 12px; color: #999; }
.page-header__title { font-size: 18px; font-weight: 600; color: #303133; margin: 0; }
.page-header__right { display: flex; align-items: center; gap: 12px; }
.page-header__update-time { font-size: 12px; color: #999; white-space: nowrap; }
.page-header__countdown { font-size: 12px; color: #999; white-space: nowrap; }

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
