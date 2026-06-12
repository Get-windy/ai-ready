<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="adjustment-page-header">
        <div class="adjustment-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>预算调整</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="adjustment-page-header-title">预算调整</h2>
        </div>
        <div class="adjustment-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', loadData)">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
                <span class="shortcut-hints">
                  <span class="shortcut-hint"><kbd>Ctrl+R</kbd> 刷新</span>
                  <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
                </span>
        </div>

      </div>
    </template>

    <div class="adjustment-management">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-draft">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ draftCount }}</div>
            <div class="stat-card-label">草稿</div>
          </div>
          <FileOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-pending">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pendingCount }}</div>
            <div class="stat-card-label">待审批</div>
          </div>
          <ClockCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-approved">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ approvedCount }}</div>
            <div class="stat-card-label">已通过</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-amount">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(totalAmount) }}</div>
            <div class="stat-card-label">调整总额</div>
          </div>
          <DollarOutlined class="stat-card-icon" />
        </div>
      </div>

      <!-- 骨架屏 -->
      <a-skeleton v-if="loading && tableData.length === 0" active :paragraph="{ rows: 8 }" style="padding: 20px;" />

      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableDataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="'id'"
        :filter-fields="filterFields"
        :selectable="true"
        :show-export="true"
        :show-summary="true"
        :summary-data="summaryData"
        :min-empty-rows="12"
        add-text="新建调整"
        @add="handleAdd"
        @cell-dblclick="handleView"
        @refresh="debounceClick('refresh', loadData)"
        @search="handleSearch"
        @export="handleExport"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
        @selection-change="handleSelectionChange"
        @batch-delete="handleBatchDelete"
      >
        <template #batch-actions="{ selectedRows: rows }">
          <a-button size="small" v-permission="'budget:plan:batchsubmit'" @click="handleBatchSubmit(rows)" :disabled="!canBatchSubmit(rows)">
            <template #icon><CheckCircleOutlined /></template>
            批量提交
          </a-button>
        </template>

        <template #empty>
          <div v-if="hasError" class="table-empty table-empty-error">
            <WarningOutlined class="table-empty-icon table-empty-icon-error" />
            <p class="table-empty-text">数据加载失败，请重试</p>
            <a-button size="small" @click="loadData">
              <template #icon><ReloadOutlined /></template>
              重试
            </a-button>
          </div>
          <div v-else class="table-empty">
            <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
            <InboxOutlined v-else class="table-empty-icon" />
            <p v-if="hasActiveFilters" class="table-empty-text">
              没有符合条件的调整记录，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无预算调整记录，点击「新建调整」开始创建
            </p>
          </div>
        </template>

        <template #action="{ record }">
          <a-space :size="0" class="action-cell-inner">
            <a-tooltip title="查看">
              <a-button type="link" size="small" v-permission="'budget:plan:view'" @click="handleView(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 'draft'" title="编辑">
              <a-button type="link" size="small" v-permission="'budget:plan:edit'" @click="handleEdit(record)">
                <template #icon><EditOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="更多操作">
              <a-dropdown trigger="click">
                <a-button type="link" size="small" class="action-more-btn">
                  <template #icon><EllipsisOutlined /></template>
                </a-button>
                <template #overlay>
                  <a-menu @click="({ key }) => handleActionMenuClick(key as string, record)">
                    <a-menu-item v-if="record.status === 'draft'" key="submit">
                      <CheckCircleOutlined /> 提交
                    </a-menu-item>
                    <a-menu-item v-if="record.status === 'submitted'" key="approve">
                      <AuditOutlined /> 通过
                    </a-menu-item>
                    <a-menu-item v-if="record.status === 'submitted'" key="reject">
                      <CloseCircleOutlined /> 拒绝
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-tooltip>
          </a-space>
        </template>
      </VxeTableList>

      <!-- 全屏详情抽屉（新建/编辑） -->
      <FullScreenDetail
        :visible="formVisible"
        :title="isEdit ? '编辑预算调整' : '新建预算调整'"
        :save-loading="submitLoading"
        :show-save-and-new="!isEdit"
        :dirty="formDirty"
        @close="handleFormClose"
        @save="handleFormSubmit"
        @save-and-new="handleFormSaveAndNew"
      >
        <a-form :model="formData" :rules="formRules" ref="formRef" layout="vertical">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="预算" name="budgetId">
                <a-select v-model:value="formData.budgetId" placeholder="请选择预算" size="small" show-search :filter-option="budgetFilterOption">
                  <a-select-option v-for="b in budgetOptions" :key="b.id" :value="b.id">{{ b.budgetNo }} - {{ b.departmentName }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="调整类型" name="adjustmentType">
                <a-select v-model:value="formData.adjustmentType" placeholder="请选择" size="small">
                  <a-select-option value="increase">增加</a-select-option>
                  <a-select-option value="decrease">减少</a-select-option>
                  <a-select-option value="transfer">调剂</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="金额" name="amount">
                <a-input-number v-model:value="formData.amount" :min="0" :precision="2" style="width: 100%" placeholder="请输入金额" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="申请日期">
                <a-date-picker v-model:value="formData.applyDate" style="width: 100%" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="源科目" v-if="formData.adjustmentType === 'decrease' || formData.adjustmentType === 'transfer'">
            <a-input v-model:value="formData.sourceSubjectName" placeholder="减少/调出科目" size="small" />
          </a-form-item>
          <a-form-item label="目标科目" v-if="formData.adjustmentType === 'increase' || formData.adjustmentType === 'transfer'">
            <a-input v-model:value="formData.targetSubjectName" placeholder="增加/调入科目" size="small" />
          </a-form-item>
          <a-form-item label="调整原因" name="reason">
            <a-textarea v-model:value="formData.reason" :rows="3" placeholder="请输入调整原因" size="small" />
          </a-form-item>
          <a-form-item label="申请人">
            <a-input v-model:value="formData.applicantName" placeholder="申请人" size="small" />
          </a-form-item>
        </a-form>
      </FullScreenDetail>
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  EyeOutlined, EditOutlined, EllipsisOutlined, CheckCircleOutlined, AuditOutlined, CloseCircleOutlined, SearchOutlined, InboxOutlined,
  FileOutlined, ClockCircleOutlined, DollarOutlined, SyncOutlined, ReloadOutlined, WarningOutlined, DownloadOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import { budgetAdjustmentApi, annualBudgetApi, type BudgetAdjustment } from '@/api/budget'

const searchFilters = reactive<Record<string, any>>({})

const tableData = ref<BudgetAdjustment[]>([])
const loading = ref(false)
const tableRef = ref()
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
const hasError = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null
const selectedRows = ref<BudgetAdjustment[]>([])
const selectedIds = ref<number[]>([])

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// ── 预算选项 ────────────────────────────────────────────
const budgetOptions = ref<{ id: number; budgetNo: string; departmentName: string }[]>([])
const statLoading = ref(false)
const statError = ref(false)

function budgetFilterOption(input: string, option: any) {
  return option.children?.toLowerCase().includes(input.toLowerCase())
}

// ── debounceClick ──────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: () => void) {
  if (clickLocks.get(key)) return
  clickLocks.set(key, true)
  try { fn() } finally { setTimeout(() => clickLocks.set(key, false), 300) }
}

// ── Keyboard shortcuts ─────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  if (['INPUT', 'TEXTAREA', 'SELECT'].includes(tag)) return
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) { e.preventDefault(); debounceClick('refresh', loadData) }
}

// ── 汇总行 ──────────────────────────────────────────────
const summaryData = computed(() => {
  if (tableData.value.length === 0) return []
  const totalAmount = tableData.value.reduce((s, r) => s + (r.amount || 0), 0)
  return [
    { label: '调整总额', value: `¥${formatAmount(totalAmount)}`, type: 'currency' }
  ]
})

// ── 统计数据 ────────────────────────────────────────────
const draftCount = computed(() => tableData.value.filter(r => r.status === 'draft').length)
const pendingCount = computed(() => tableData.value.filter(r => r.status === 'submitted').length)
const approvedCount = computed(() => tableData.value.filter(r => r.status === 'approved').length)
const totalAmount = computed(() => tableData.value.reduce((s, r) => s + (r.amount || 0), 0))

// ── 数据源 ────────────────────────────────────────────
const tableDataSource = tableData

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const vxeColumns = computed(() => [
  { field: 'adjustmentNo', title: '调整单号', width: 150 },
  { field: 'budgetId', title: '预算ID', width: 80 },
  { field: 'adjustmentType', title: '类型', width: 80, align: 'center', formatter: ({ cellValue }) => {
    const map: Record<string, string> = { increase: '增加', decrease: '减少', transfer: '调剂' }
    return map[cellValue] || cellValue
  }},
  { field: 'amount', title: '金额', width: 130, align: 'right', formatter: ({ cellValue }) => `¥${formatAmount(cellValue)}` },
  { field: 'status', title: '状态', width: 80, align: 'center', formatter: ({ cellValue }) => statusText(cellValue) },
  { field: 'reason', title: '原因', minWidth: 100, showOverflow: 'tooltip' },
  { field: 'applicantName', title: '申请人', width: 100 },
  { field: 'applyDate', title: '申请日期', width: 110 },
  { type: 'action', title: '操作', width: 140, fixed: 'right' },
])

const filterFields = [
  { key: 'budgetId', label: '预算ID', type: 'input' as const, placeholder: '预算ID' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 'draft' },
    { label: '待审批', value: 'submitted' },
    { label: '已通过', value: 'approved' },
    { label: '已拒绝', value: 'rejected' },
  ]},
  { key: 'adjustmentType', label: '类型', type: 'select' as const, options: [
    { label: '增加', value: 'increase' },
    { label: '减少', value: 'decrease' },
    { label: '调剂', value: 'transfer' },
  ]},
]

const statusColor = (s: string) => {
  const map: Record<string, string> = { draft: 'default', submitted: 'blue', approved: 'green', rejected: 'red' }
  return map[s] || 'default'
}
const statusText = (s: string) => {
  const map: Record<string, string> = { draft: '草稿', submitted: '待审批', approved: '已通过', rejected: '已拒绝' }
  return map[s] || s
}

// Form
const formVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const editId = ref<number | null>(null)

// ── 表单脏状态跟踪 ──────────────────────────────────────
const initialFormSnapshot = ref<string>('')
function saveFormSnapshot() {
  initialFormSnapshot.value = JSON.stringify({
    budgetId: formData.budgetId,
    adjustmentType: formData.adjustmentType,
    amount: formData.amount,
    reason: formData.reason,
    applicantName: formData.applicantName,
    applyDate: formData.applyDate,
    sourceSubjectId: formData.sourceSubjectId,
    targetSubjectId: formData.targetSubjectId,
  })
}
const formDirty = computed(() => {
  if (!formVisible.value) return false
  const current = JSON.stringify({
    budgetId: formData.budgetId,
    adjustmentType: formData.adjustmentType,
    amount: formData.amount,
    reason: formData.reason,
    applicantName: formData.applicantName,
    applyDate: formData.applyDate,
    sourceSubjectId: formData.sourceSubjectId,
    targetSubjectId: formData.targetSubjectId,
  })
  return current !== initialFormSnapshot.value
})

const formData = reactive<BudgetAdjustment>({
  id: 0,
  adjustmentNo: '',
  budgetId: 0,
  adjustmentType: 'increase',
  amount: 0,
  sourceSubjectId: 0,
  sourceSubjectName: '',
  targetSubjectId: 0,
  targetSubjectName: '',
  reason: '',
  status: 'draft',
  applicantId: '',
  applicantName: '',
  approverId: '',
  approverName: '',
  approvalComment: '',
  applyDate: new Date().toISOString().slice(0, 10),
  approvalDate: '',
  createdBy: '',
  createdAt: '',
  updatedBy: '',
  updatedAt: '',
})

const formRules = {
  budgetId: [{ required: true, message: '请输入预算ID', type: 'number' as const }],
  adjustmentType: [{ required: true, message: '请选择调整类型' }],
  amount: [{ required: true, message: '请输入金额', type: 'number' as const }],
  reason: [{ required: true, message: '请输入调整原因' }],
}

const loadData = async () => {
  loading.value = true
  try {
    hasError.value = false
    const res = await budgetAdjustmentApi.page({
      budgetId: searchFilters.budgetId || undefined,
      status: searchFilters.status || undefined,
      adjustmentType: searchFilters.adjustmentType || undefined,
      pageNum: pagination.current - 1,
      pageSize: pagination.pageSize,
    })
    if (res.success) {
      tableData.value = res.records || []
      pagination.total = res.total || 0
    }
  } catch {
    hasError.value = true
    console.warn('[预算调整] 加载数据失败')
    tableData.value = []
    pagination.total = 0
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

const handleSearch = () => { pagination.current = 1; loadData() }

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  loadData()
}

const handleSelectionChange = (rows: BudgetAdjustment[], ids: number[]) => {
  selectedRows.value = rows
  selectedIds.value = ids
}

const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
  loadData()
}

const resetForm = () => {
  formData.id = 0
  formData.adjustmentNo = ''
  formData.budgetId = 0
  formData.adjustmentType = 'increase'
  formData.amount = 0
  formData.sourceSubjectId = 0
  formData.targetSubjectId = 0
  formData.reason = ''
  formData.applicantName = ''
  formData.applyDate = new Date().toISOString().slice(0, 10)
  editId.value = null
}

function handleParentCreate() { handleAdd() }

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  formVisible.value = true
  nextTick(() => saveFormSnapshot())
}

const handleEdit = async (record: BudgetAdjustment) => {
  try {
    const res = await budgetAdjustmentApi.getById(record.id)
    if (res.success) {
      const d = res.data
      formData.id = d.id
      formData.adjustmentNo = d.adjustmentNo
      formData.budgetId = d.budgetId
      formData.adjustmentType = d.adjustmentType
      formData.amount = d.amount
      formData.sourceSubjectId = d.sourceSubjectId
      formData.targetSubjectId = d.targetSubjectId
      formData.reason = d.reason
      formData.applicantName = d.applicantName
      formData.applyDate = d.applyDate
      editId.value = d.id
      isEdit.value = true
      formVisible.value = true
      nextTick(() => saveFormSnapshot())
    }
  } catch {
    console.warn('[预算调整] 获取详情失败')
  }
}

const handleView = (record: BudgetAdjustment) => {
  handleEdit(record)
}

const handleSubmit = async (record: BudgetAdjustment) => {
  try {
    await budgetAdjustmentApi.submit(record.id)
    message.success('已提交审批')
    loadData()
  } catch (e: any) {
    console.warn('[预算调整] 提交失败', e)
    message.error(e?.response?.data?.message || '提交失败')
  }
}

const handleApprove = async (record: BudgetAdjustment) => {
  try {
    await budgetAdjustmentApi.approve(record.id, '审批通过')
    message.success('审批通过')
    loadData()
  } catch (e: any) {
    console.warn('[预算调整] 审批失败', e)
    message.error(e?.response?.data?.message || '审批失败')
  }
}

const handleReject = async (record: BudgetAdjustment) => {
  try {
    await budgetAdjustmentApi.reject(record.id, '已拒绝')
    message.success('已拒绝')
    loadData()
  } catch (e: any) {
    console.warn('[预算调整] 拒绝失败', e)
    message.error(e?.response?.data?.message || '拒绝失败')
  }
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.current = 1
  loadData()
}

function handleActionMenuClick(key: string, record: BudgetAdjustment) {
  switch (key) {
    case 'submit': handleSubmit(record); break
    case 'approve': handleApprove(record); break
    case 'reject': handleReject(record); break
  }
}

// ── 导出 ────────────────────────────────────────────────
async function handleExport() {
  try {
    const res = await budgetAdjustmentApi.export({
      budgetId: searchFilters.budgetId || undefined,
      status: searchFilters.status || undefined,
      adjustmentType: searchFilters.adjustmentType || undefined,
    })
    if (res.success && res.data?.length) {
      const csv = res.map((r: any) => `${r.adjustmentNo},${r.adjustmentType},${r.amount},${r.status},${r.reason}`).join('\n')
      const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url; a.download = `预算调整_${new Date().toISOString().slice(0, 10)}.csv`
      a.click(); URL.revokeObjectURL(url)
    }
    message.success('导出成功')
  } catch {
    message.error('导出失败')
  }
}

// ── 批量操作 ────────────────────────────────────────────
function canBatchSubmit(rows: BudgetAdjustment[]) {
  return rows.some(r => r.status === 'draft')
}

async function handleBatchSubmit(rows: BudgetAdjustment[]) {
  const draftIds = rows.filter(r => r.status === 'draft').map(r => r.id)
  if (!draftIds.length) { message.warning('选中的记录中没有可提交的草稿'); return }
  Modal.confirm({
    title: '批量提交',
    content: `确定提交选中的 ${draftIds.length} 项草稿？`,
    onOk: async () => {
      try {
        await Promise.all(draftIds.map(id => budgetAdjustmentApi.submit(id)))
        message.success(`已提交 ${draftIds.length} 项`)
        loadData()
      } catch { message.error('批量提交失败') }
    },
  })
}

async function handleBatchDelete(ids: number[]) {
  Modal.confirm({
    title: '批量删除',
    content: `确定删除 ${ids.length} 项调整记录？`,
    okType: 'danger',
    onOk: async () => {
      try {
        await Promise.all(ids.map(id => budgetAdjustmentApi.delete(id)))
        message.success(`已删除 ${ids.length} 项`)
        loadData()
      } catch { message.error('批量删除失败') }
    },
  })
}

// ── 表单弹窗增强 ────────────────────────────────────────
function handleFormClose() {
  // FullScreenDetail handles dirty confirmation via :dirty prop
  formVisible.value = false
}

// ── 路由离开守卫 ─────────────────────────────────────────
onBeforeRouteLeave((_to, _from, next) => {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认离开',
      content: '当前表单有未保存的内容，确定离开吗？',
      onOk: () => { next() },
      onCancel: () => { next(false) },
    })
  } else {
    next()
  }
})

async function handleFormSaveAndNew() {
  await handleFormSubmit(true)
  if (!submitLoading.value) {
    resetForm()
    isEdit.value = false
    formVisible.value = true
  }
}

async function handleFormSubmit(keepOpen?: boolean) {
  try {
    await formRef.value?.validate()
  } catch {
    console.warn('[预算调整] 表单验证失败')
    return
  }
  submitLoading.value = true
  try {
    if (isEdit.value && editId.value) {
      await budgetAdjustmentApi.update(editId.value, { ...formData })
      message.success('更新成功')
    } else {
      await budgetAdjustmentApi.create({ ...formData })
      message.success('创建成功')
    }
    if (!keepOpen) formVisible.value = false
    loadData()
  } catch (e: any) {
    console.warn('[预算调整] 操作失败', e)
    message.error(e?.response?.data?.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// ── 加载预算下拉选项 ────────────────────────────────────
async function loadBudgetOptions() {
  try {
    const res = await annualBudgetApi.page({ pageNum: 0, pageSize: 200 })
    if (res.success) {
      budgetOptions.value = (res?.records || []).map((b: any) => ({
        id: b.id, budgetNo: b.budgetNo || `#${b.id}`,
        departmentName: b.departmentName || `预算${b.id}`
      }))
    }
  } catch {
    console.warn('[预算调整] 加载预算列表失败')
    budgetOptions.value = []
  }
}

onMounted(() => {
  loadData()
  loadBudgetOptions()
  window.addEventListener('budget:create', handleParentCreate)
  window.addEventListener('budget:refresh', loadData)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    loadData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('budget:create', handleParentCreate)
  window.removeEventListener('budget:refresh', loadData)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  document.removeEventListener('keydown', handleKeydown)
})

defineExpose({ handleQuery: loadData })

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
</script>

<style scoped>
.adjustment-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.adjustment-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.adjustment-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.adjustment-page-header-right {
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

.adjustment-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

.adjustment-management :deep(.vxe-table) {
  flex: 1;
  min-height: 0;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-draft { background: linear-gradient(135deg, #f5f5f5 0%, #e8e8e8 100%); }
.stat-pending { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-approved { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-amount { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

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

.action-more-btn { padding: 0 4px; font-size: 16px; vertical-align: middle; }
.action-cell-inner { flex-wrap: nowrap; }

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
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

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
.adjustment-management :deep(.ant-input-sm),
.adjustment-management :deep(.ant-input-number-sm),
.adjustment-management :deep(.ant-select-single.ant-select-sm .ant-select-selector),
.adjustment-management :deep(.ant-picker-small),
.adjustment-management :deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}

.adjustment-management :deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}

.adjustment-management :deep(.ant-input-number-sm input) {
  height: 26px;
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

/* ── 空状态包装 ──────────────────────── */
.empty-state-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  min-height: 120px;
}

/* ── 表头 2px 分割线 ──────────────────────── */
.adjustment-management :deep(.vxe-table-list-container .vxe-header--row .vxe-header--column) {
  border-bottom: 2px solid #d0d5dd !important;
}

</style>
