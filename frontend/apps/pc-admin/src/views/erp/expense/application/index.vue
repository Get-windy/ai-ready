<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="page-header">
        <div class="page-header__left">
          <span class="page-header__breadcrumb">ERP / 费用管理 / 费用申请</span>
          <h2 class="page-header__title">费用申请</h2>
        </div>
        <div class="page-header__right">
          <span v-if="lastUpdateTime" style="font-size: 12px; color: #999;">更新于: {{ lastUpdateTime }}</span>
          <a-space :size="12">
            <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
              <SyncOutlined /> {{ autoRefreshCountdown }}s
            </span>
            <a-button size="small" @click="debounceClick('refresh', fetchData)">
              <ReloadOutlined /> 刷新
            </a-button>
          </a-space>
        </div>
      </div>
    </template>

    <!-- 统计卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="6">
        <a-card size="small">
          <stat-card title="申请总数" :value="stats.totalCount" :amount="stats.totalAmount" color="#1890ff" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card size="small">
          <stat-card title="待审批" :value="stats.pendingCount" :amount="stats.pendingAmount" color="#faad14" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card size="small">
          <stat-card title="已通过" :value="stats.approvedCount" :amount="stats.approvedAmount" color="#52c41a" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card size="small">
          <stat-card title="总金额(含其他)" :value="stats.otherCount" :amount="stats.totalAmountAll" color="#722ed1" />
        </a-card>
      </a-col>
    </a-row>

    <div class="expense-layout">
      <!-- 左侧快捷筛选 -->
      <div class="expense-sidebar">
        <div class="sidebar-section">
          <div class="sidebar-title">筛选</div>
          <a-menu
            :selected-keys="[activeFilter]"
            mode="inline"
            :inline-collapsed="false"
            @click="({ key }) => { activeFilter = key; pagination.current = 1; fetchData() }"
          >
            <a-menu-item key="all">全部申请</a-menu-item>
            <a-menu-item key="DRAFT">草稿</a-menu-item>
            <a-menu-item key="SUBMITTED">待审批</a-menu-item>
            <a-menu-item key="APPROVED">已通过</a-menu-item>
            <a-menu-item key="REJECTED">已拒绝</a-menu-item>
            <a-menu-item key="CANCELLED">已取消</a-menu-item>
          </a-menu>
        </div>
      </div>

      <!-- 右侧列表 -->
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
              <a-button size="small" @click="handleExport">
                <template #icon><ExportOutlined /></template>
              </a-button>
            </a-tooltip>
          </a-space>
          <a-space>
            <a-button v-permission="'erp:expense:application:create'" type="primary" size="small" @click="showCreate">
              <PlusOutlined /> 新增申请
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
          @page-change="onPageChange"
        >
          <template #statusCell="{ record }">
            <StatusTag :status="record.status" :map="EXPENSE_APPLICATION_STATUS" />
          </template>
          <template #empty>
            <EmptyState v-if="loading" image="no-data" title="加载中..." description="" :show-actions="false" size="small" />
            <EmptyState v-else image="no-data" title="暂无费用申请" description="当前没有费用申请数据" add-text="新增申请" size="small" @refresh="fetchData" @add="showCreate" />
          </template>
          <template #action="{ record }">
            <a-space :size="4">
              <PrintButton :record="record" :business-id="record.id" business-type="expense_application" button-type="link" button-size="small" tooltip="打印" />
              <a-button type="link" size="small" @click="viewDetail(record.id)">查看</a-button>
              <a-button v-if="record.status === 'DRAFT'" v-permission="'erp:expense:application:edit'" type="link" size="small" @click="editItem(record.id)">编辑</a-button>
              <a-button v-if="record.status === 'DRAFT'" v-permission="'erp:expense:application:submit'" type="link" size="small" @click="handleSubmit(record.id)">提交</a-button>
              <a-button v-if="record.status === 'DRAFT'" v-permission="'erp:expense:application:delete'" type="link" size="small" danger @click="confirmDelete(record.id)">删除</a-button>
            </a-space>
          </template>
        </VxeTableList>
      </div>
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
defineOptions({ name: 'ExpenseApplicationList' })

import { ref, computed, onMounted, onUnmounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined, ExportOutlined, SyncOutlined } from '@ant-design/icons-vue'
import { PageContainer, SearchBar, EmptyState } from '@/components'
import type { SearchField } from '@/components/SearchBar/SearchBar.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import StatusTag from '@/components/StatusTag/StatusTag.vue'
import { EXPENSE_APPLICATION_STATUS } from '@/utils/statusConfig'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import request from '@/utils/request'
import { feeApplicationApi } from '@/api/erp/expense'
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

// ── 类型定义 ──────────────────────────────────────────
interface ExpenseApplication {
  id: number
  applicationNo: string
  applicationTitle: string
  applicantName: string
  departmentName: string
  expenseTypeDesc: string
  totalAmount: number
  applyDate: string
  status: string
  remark?: string
}

const loading = ref(false)
const dataList = ref<ExpenseApplication[]>([])

const stats = reactive({
  totalCount: 0, totalAmount: 0,
  pendingCount: 0, pendingAmount: 0,
  approvedCount: 0, approvedAmount: 0,
  otherCount: 0, totalAmountAll: 0
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
const searchFields: SearchField[] = [
  { name: 'keyword', label: '标题/单号', type: 'input', placeholder: '请输入标题或单号' },
  { name: 'status', label: '状态', type: 'select', placeholder: '请选择状态', options: [
    { label: '全部', value: '' },
    { label: '草稿', value: 'DRAFT' },
    { label: '待审批', value: 'SUBMITTED' },
    { label: '审批中', value: 'APPROVING' },
    { label: '已通过', value: 'APPROVED' },
    { label: '已拒绝', value: 'REJECTED' },
    { label: '已取消', value: 'CANCELLED' },
    { label: '已撤回', value: 'WITHDRAWN' },
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

const vxeColumns = computed(() => [
  { field: 'applicationNo', title: '申请单号', width: 160 },
  { field: 'applicationTitle', title: '申请标题', width: 200, minWidth: 140 },
  { field: 'applicantName', title: '申请人', width: 80 },
  { field: 'departmentName', title: '部门', width: 100 },
  { field: 'expenseTypeDesc', title: '费用类型', width: 90 },
  { field: 'totalAmount', title: '金额', width: 120, align: 'right', formatter: ({ cellValue }: any) => formatAmount(cellValue) },
  { field: 'applyDate', title: '申请日期', width: 100 },
  { field: 'status', title: '状态', width: 80, align: 'center', slots: { default: 'statusCell' } },
  { field: '_action', title: '操作', width: 200, fixed: 'right', slots: { default: 'action' } }
])

function statusColor(s: string) {
  const map: Record<string, string> = { DRAFT: 'default', SUBMITTED: 'blue', APPROVING: 'orange', APPROVED: 'green', REJECTED: 'red', CANCELLED: 'default', WITHDRAWN: 'default' }
  return map[s] || 'default'
}
function statusLabel(s: string) {
  const map: Record<string, string> = { DRAFT: '草稿', SUBMITTED: '待审批', APPROVING: '审批中', APPROVED: '已通过', REJECTED: '已拒绝', CANCELLED: '已取消', WITHDRAWN: '已撤回' }
  return map[s] || s
}

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = { pageNum: pagination.current, pageSize: pagination.pageSize }
    if (searchKeyword.value) params.applicationTitle = searchKeyword.value
    if (activeFilter.value !== 'all') params.status = activeFilter.value
    else if (statusFilter.value) params.status = statusFilter.value

    const res = await feeApplicationApi.page(params)
    dataList.value = (res as any).records || (res as any).data?.records || []
    pagination.total = (res as any).total || (res as any).data?.total || 0
    lastUpdateTime.value = new Date().toLocaleString('zh-CN')
  } catch (e) {
    console.error('[费用申请] 加载失败', e)
    message.error('加载失败')
  } finally { loading.value = false }
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
    const res = await feeApplicationApi.page({ pageNum: 1, pageSize: 1 }) as any
    stats.totalCount = res.total || res.data?.total || 0
    // 后续可通过专门的统计接口填充更详细数据
  } catch { /* ignore */ }
}

async function handleExport() {
  try {
    const blob = await request.get('/erp/expense/application/export', {
      params: {
        applicationTitle: searchKeyword.value,
        status: activeFilter.value !== 'all' ? activeFilter.value : statusFilter.value || undefined
      },
      responseType: 'blob'
    })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `费用申请_${new Date().toISOString().slice(0, 10)}.xlsx`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('导出成功')
  } catch (error) {
    message.error('导出失败')
  }
}
function showCreate() { router.push('/erp/expense/application/create') }
function viewDetail(id: number) { router.push(`/erp/expense/application/${id}`) }
function editItem(id: number) { router.push(`/erp/expense/application/${id}?edit=1`) }

async function handleSubmit(id: number) {
  try { await feeApplicationApi.submit(id); message.success('提交成功'); fetchData() }
  catch { message.error('提交失败') }
}
function confirmDelete(id: number) {
  Modal.confirm({
    title: '确认删除',
    content: '确定要删除该费用申请吗？此操作不可恢复。',
    okText: '确认删除',
    okType: 'danger',
    onOk: async () => {
      try { await feeApplicationApi.delete(id); message.success('删除成功'); fetchData() }
      catch { message.error('删除失败') }
    }
  })
}

function handleError(err: any) { console.warn('[ExpenseApplication]', err) }

const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') {
    e.preventDefault(); showCreate()
  }
  if (e.key === 'F5' && !(e.target instanceof HTMLInputElement) && !(e.target instanceof HTMLTextAreaElement)) {
    e.preventDefault(); debounceClick('refresh', fetchData)
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'e') {
    e.preventDefault(); debounceClick('export', handleExport)
  }
}

onMounted(() => {
  fetchData()
  fetchStats()
  autoRefreshCountdown.value = 300
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 300
  }, 300000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  document.addEventListener('keydown', handleKeydown)
})
onUnmounted(() => {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
  document.removeEventListener('keydown', handleKeydown)
})
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

.auto-refresh-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #52c41a;
  white-space: nowrap;
}
</style>
