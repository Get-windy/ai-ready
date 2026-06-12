<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="annual-page-header">
        <div class="annual-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>年度预算</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="annual-page-header-title">年度预算</h2>
        </div>
        <div class="annual-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', loadData)">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
            <span class="shortcut-hint"><kbd>Ctrl+F</kbd> 搜索</span>
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            <span class="shortcut-hint"><kbd>F3</kbd> 搜索</span>
          </span>
        </div>
      </div>
    </template>

    <div class="annual-management">
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
        <div class="stat-card stat-executing">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ executingCount }}</div>
            <div class="stat-card-label">执行中</div>
          </div>
          <PlayCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-amount">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(totalBudget) }}</div>
            <div class="stat-card-label">预算总额</div>
          </div>
          <DollarOutlined class="stat-card-icon" />
        </div>
      </div>

      <!-- 骨架屏 -->
      <a-skeleton v-if="loading && tableData.length === 0" active :paragraph="{ rows: 8 }" style="padding: 20px;" />

      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-key="'id'"
        :filter-fields="filterFields"
        :selectable="true"
        :show-export="true"
        :show-summary="true"
        :summary-data="summaryData"
        :min-empty-rows="12"
        add-text="新建预算"
        @add="handleAdd"
        @cell-dblclick="handleView"
        @refresh="loadData"
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
          <a-button size="small" v-permission="'budget:plan:batchdelete'" @click="handleBatchDelete(rows)" :disabled="rows.length === 0">
            <template #icon><DeleteOutlined /></template>
            批量删除
          </a-button>
        </template>

        <template #statusCell="{ record }">
          <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
        </template>
        <template #totalAmountCell="{ record }">
          <span class="amount-cell">¥{{ formatAmount(record.totalAmount) }}</span>
        </template>
        <template #totalUsedAmountCell="{ record }">
          <span class="amount-cell used">¥{{ formatAmount(record.totalUsedAmount) }}</span>
        </template>
        <template #executionRateCell="{ record }">
          <span class="rate-cell">{{ record.executionRate?.toFixed(2) ?? '0.00' }}%</span>
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
                    <a-menu-item v-if="record.status === 'approved'" key="startExec">
                      <PlayCircleOutlined /> 执行
                    </a-menu-item>
                    <a-menu-item v-if="record.status === 'executing'" key="close">
                      <StopOutlined /> 关闭
                    </a-menu-item>
                    <a-menu-divider v-if="record.status === 'draft'" />
                    <a-menu-item v-if="record.status === 'draft'" key="delete" danger>
                      <DeleteOutlined /> 删除
                    </a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-tooltip>
          </a-space>
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
              没有符合条件的预算记录，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无年度预算记录，点击「新建预算」开始创建
            </p>
          </div>
        </template>
      </VxeTableList>

      <!-- 全屏详情抽屉（新建/编辑） -->
      <FullScreenDetail
        :visible="formVisible"
        :title="isEdit ? '编辑年度预算' : '新建年度预算'"
        :save-loading="submitLoading"
        :show-save-and-new="!isEdit"
        :dirty="formDirty"
        @close="handleFormClose"
        @save="handleFormSubmit"
        @save-and-new="handleFormSaveAndNew"
      >
        <a-form :model="formData" :rules="formRules" ref="formRef" layout="vertical">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="预算单号" name="budgetNo">
                <a-input v-model:value="formData.budgetNo" placeholder="自动生成" :disabled="isEdit" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="年度" name="fiscalYear">
                <a-input-number v-model:value="formData.fiscalYear" :min="2020" :max="2099" style="width: 100%" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="部门" name="departmentName">
                <a-input v-model:value="formData.departmentName" placeholder="部门名称" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="部门ID" name="departmentId">
                <a-input v-model:value="formData.departmentId" placeholder="部门ID" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="模板名称" name="templateName">
                <a-input v-model:value="formData.templateName" placeholder="关联模板" :disabled="!!formData.templateId" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="备注" name="remark">
                <a-input v-model:value="formData.remark" placeholder="备注" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="描述" name="description">
            <a-textarea v-model:value="formData.description" :rows="2" placeholder="描述" size="small" />
          </a-form-item>

          <a-divider>预算科目</a-divider>
          <a-button type="dashed" @click="debounceClick('addItem', addItem)" style="width: 100%; margin-bottom: 12px">
            <template #icon><PlusOutlined /></template>添加科目
          </a-button>
          <VxeTableList
            :data-source="formData.items"
            :columns="itemColumns"
            :pagination="false as any"
            row-key="rowKey"
            :show-toolbar="false"
            :selectable="false"
            :show-add="false"
            :show-search="false"
            :show-export="false"
            :show-batch-delete="false"
          >
            <template #subjectCodeCell="{ record }">
              <a-input v-model:value="record.subjectCode" placeholder="科目编码" size="small" />
            </template>
            <template #subjectNameCell="{ record }">
              <a-input v-model:value="record.subjectName" placeholder="科目名称" size="small" />
            </template>
            <template #budgetAmountCell="{ record }">
              <a-input-number v-model:value="record.budgetAmount" :min="0" :precision="2" style="width: 100%" size="small" />
            </template>
            <template #action="{ index }">
              <a-popconfirm title="确定删除？" @confirm="removeItem(index as number)">
                <a class="danger">删除</a>
              </a-popconfirm>
            </template>
          </VxeTableList>
        </a-form>
      </FullScreenDetail>

      <!-- 从模板创建（模态框 保留） -->
      <a-modal
        v-model:open="templateModalVisible"
        title="从模板创建年度预算"
        :width="600"
        @ok="handleTemplateSelectOk"
        :confirm-loading="templateLoading"
      >
        <VxeTableList
          :data-source="templateList"
          :columns="templateVxeColumns"
          :pagination="false as any"
          :loading="templateLoading"
          row-key="id"
          :show-toolbar="false"
          selectable
          :show-add="false"
          :show-search="false"
          :show-export="false"
          :show-batch-delete="false"
          @selection-change="(_: any, keys: any[]) => { templateSelectedKeys = keys }"
        />
      </a-modal>
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { useRoute } from 'vue-router'
import { onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined, DeleteOutlined, EditOutlined, EyeOutlined, EllipsisOutlined,
  CheckCircleOutlined, CloseCircleOutlined, AuditOutlined, StopOutlined, PlayCircleOutlined,
  FileOutlined, ClockCircleOutlined, DollarOutlined, SyncOutlined, ReloadOutlined, WarningOutlined,
  SearchOutlined, InboxOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import { annualBudgetApi, budgetTemplateApi, type AnnualBudget, type BudgetItem } from '@/api/budget'

const route = useRoute()

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

// ── 筛选 ──────────────────────────────────────────────
const filterFields = [
  { key: 'keyword', label: '关键词', type: 'input' as const, placeholder: '预算单号/部门' },
  { key: 'fiscalYear', label: '年度', type: 'input' as const, placeholder: '年度' },
  { key: 'departmentId', label: '部门ID', type: 'input' as const, placeholder: '部门ID' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 'draft' },
    { label: '待审批', value: 'submitted' },
    { label: '已审批', value: 'approved' },
    { label: '已拒绝', value: 'rejected' },
    { label: '执行中', value: 'executing' },
    { label: '已关闭', value: 'closed' },
  ]},
]

const searchFilters = reactive<Record<string, any>>({
  keyword: '',
  fiscalYear: undefined,
  departmentId: undefined,
  status: undefined,
})

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const tableRef = ref()
const tableData = ref<AnnualBudget[]>([])
const loading = ref(false)
const pagination = ref({ current: 1, pageSize: 20, total: 0 })
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
const hasError = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null
const selectedRows = ref<AnnualBudget[]>([])
const selectedIds = ref<number[]>([])

// ── 统计数据 ────────────────────────────────────────────
const draftCount = computed(() => tableData.value.filter(r => r.status === 'draft').length)
const pendingCount = computed(() => tableData.value.filter(r => r.status === 'submitted').length)
const executingCount = computed(() => tableData.value.filter(r => r.status === 'executing').length)
const totalBudget = computed(() => tableData.value.reduce((s, r) => s + (r.totalAmount || 0), 0))

// ── 汇总行 ──────────────────────────────────────────────
const summaryData = computed(() => {
  if (tableData.value.length === 0) return []
  const total = tableData.value.reduce((s, r) => s + (r.totalAmount || 0), 0)
  const used = tableData.value.reduce((s, r) => s + (r.totalUsedAmount || 0), 0)
  return [
    { label: '预算总额', value: `¥${formatAmount(total)}`, type: 'currency' },
    { label: '已使用总额', value: `¥${formatAmount(used)}`, type: 'currency' },
  ]
})

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const vxeColumns = [
  { field: 'budgetNo', title: '预算单号', width: 150 },
  { field: 'fiscalYear', title: '年度', width: 70 },
  { field: 'departmentName', title: '部门', width: 120 },
  { field: 'totalAmount', title: '预算总额', width: 130, align: 'right', slotName: 'totalAmountCell' },
  { field: 'totalUsedAmount', title: '已使用', width: 130, align: 'right', slotName: 'totalUsedAmountCell' },
  { field: 'executionRate', title: '执行率', width: 80, align: 'right', slotName: 'executionRateCell' },
  { field: 'status', title: '状态', width: 80, align: 'center', slotName: 'statusCell' },
  { field: 'createdAt', title: '创建时间', width: 170 },
  { type: 'action', title: '操作', width: 140, fixed: 'right' },
]

const itemColumns = [
  { field: 'subjectCode', title: '科目编码', slotName: 'subjectCodeCell' },
  { field: 'subjectName', title: '科目名称', slotName: 'subjectNameCell' },
  { field: 'budgetAmount', title: '预算金额', slotName: 'budgetAmountCell' },
  { type: 'action', title: '操作', width: 60 },
]

const statusColor = (s: string) => {
  const map: Record<string, string> = { draft: 'default', submitted: 'blue', approved: 'cyan', rejected: 'red', executing: 'green', closed: 'orange' }
  return map[s] || 'default'
}
const statusText = (s: string) => {
  const map: Record<string, string> = { draft: '草稿', submitted: '待审批', approved: '已审批', rejected: '已拒绝', executing: '执行中', closed: '已关闭' }
  return map[s] || s
}

// ── Form ────────────────────────────────────────────────
const formVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const editId = ref<number | null>(null)

const formData = reactive<AnnualBudget>({
  id: 0,
  budgetNo: '',
  templateId: 0,
  templateName: '',
  fiscalYear: new Date().getFullYear(),
  departmentId: '',
  departmentName: '',
  totalAmount: 0,
  status: 'draft',
  totalApprovedAmount: 0,
  totalUsedAmount: 0,
  totalRemainingAmount: 0,
  executionRate: 0,
  description: '',
  remark: '',
  createdBy: '',
  createdAt: '',
  updatedBy: '',
  updatedAt: '',
  items: [],
})

const formRules = {
  departmentName: [{ required: true, message: '请输入部门名称' }],
  fiscalYear: [{ required: true, message: '请选择年度', type: 'number' as const }],
}

// ── 表单脏状态跟踪 ──────────────────────────────────────
const initialFormSnapshot = ref<string>('')
function saveFormSnapshot() {
  initialFormSnapshot.value = JSON.stringify({
    budgetNo: formData.budgetNo,
    fiscalYear: formData.fiscalYear,
    departmentId: formData.departmentId,
    departmentName: formData.departmentName,
    templateName: formData.templateName,
    description: formData.description,
    remark: formData.remark,
    items: formData.items.map((i: any) => ({
      subjectCode: i.subjectCode,
      subjectName: i.subjectName,
      budgetAmount: i.budgetAmount,
    })),
  })
}
const formDirty = computed(() => {
  if (!formVisible.value) return false
  const current = JSON.stringify({
    budgetNo: formData.budgetNo,
    fiscalYear: formData.fiscalYear,
    departmentId: formData.departmentId,
    departmentName: formData.departmentName,
    templateName: formData.templateName,
    description: formData.description,
    remark: formData.remark,
    items: formData.items.map((i: any) => ({
      subjectCode: i.subjectCode,
      subjectName: i.subjectName,
      budgetAmount: i.budgetAmount,
    })),
  })
  return current !== initialFormSnapshot.value
})

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

// ── 内联科目编辑 ────────────────────────────────────────
let rowKeyCounter = 0
const genRowKey = () => `row_${++rowKeyCounter}`

const addItem = () => {
  formData.items.push({
    id: 0,
    budgetId: 0,
    subjectCode: '',
    subjectName: '',
    budgetAmount: 0,
    usedAmount: 0,
    remainingAmount: 0,
    frozenAmount: 0,
    executionRate: 0,
    sortOrder: formData.items.length + 1,
    rowKey: genRowKey(),
  } as any)
}

const removeItem = (index: number) => {
  formData.items.splice(index, 1)
}

// ── 数据加载 ────────────────────────────────────────────
const loadData = async () => {
  loading.value = true
  try {
    hasError.value = false
    const res = await annualBudgetApi.page({
      keyword: searchFilters.keyword || undefined,
      fiscalYear: searchFilters.fiscalYear || undefined,
      departmentId: searchFilters.departmentId || undefined,
      status: searchFilters.status || undefined,
      pageNum: pagination.value.current - 1,
      pageSize: pagination.value.pageSize,
    })
    if (res.code === 200) {
      tableData.value = res.records || []
      pagination.value.total = res.total || 0
    }
  } catch {
    hasError.value = true
    console.warn('[年度预算] 加载数据失败')
    tableData.value = []
    pagination.value.total = 0
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

const handleSearch = () => { pagination.value.current = 1; loadData() }

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.value.current = 1
  loadData()
}

const handleSelectionChange = (rows: AnnualBudget[], ids: number[]) => {
  selectedRows.value = rows
  selectedIds.value = ids
}

const handlePageChange = (page: number, size: number) => {
  pagination.value.current = page
  pagination.value.pageSize = size
  loadData()
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  searchFilters.keyword = ''
  pagination.value.current = 1
  loadData()
}

// ── CRUD 操作 ──────────────────────────────────────────
const resetForm = () => {
  formData.id = 0
  formData.budgetNo = ''
  formData.templateId = 0
  formData.templateName = ''
  formData.fiscalYear = new Date().getFullYear()
  formData.departmentId = ''
  formData.departmentName = ''
  formData.totalAmount = 0
  formData.description = ''
  formData.remark = ''
  formData.items = []
  editId.value = null
}

function handleParentCreate() { handleAdd() }

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  formVisible.value = true
  nextTick(() => saveFormSnapshot())
}

const handleEdit = async (record: AnnualBudget) => {
  try {
    const res = await annualBudgetApi.getById(record.id)
    if (res.code === 200) {
      const d = res.data
      formData.id = d.id
      formData.budgetNo = d.budgetNo
      formData.templateId = d.templateId
      formData.templateName = d.templateName
      formData.fiscalYear = d.fiscalYear
      formData.departmentId = d.departmentId
      formData.departmentName = d.departmentName
      formData.totalAmount = d.totalAmount
      formData.description = d.description || ''
      formData.remark = d.remark || ''
      formData.items = (d.items || []).map((item: any) => ({ ...item, rowKey: genRowKey() }))
      editId.value = d.id
      isEdit.value = true
      formVisible.value = true
      nextTick(() => saveFormSnapshot())
    }
  } catch {
    console.warn('[年度预算] 获取详情失败')
  }
}

const handleView = (record: AnnualBudget) => {
  handleEdit(record)
}

// ── 表单操作 ────────────────────────────────────────────
function handleFormClose() {
  // FullScreenDetail handles dirty confirmation via :dirty prop
  formVisible.value = false
}

async function handleFormSaveAndNew() {
  await handleFormSubmit(true)
  if (!submitLoading.value) {
    resetForm()
    isEdit.value = false
    formVisible.value = true
    nextTick(() => saveFormSnapshot())
  }
}

async function handleFormSubmit(keepOpen?: boolean) {
  try {
    await formRef.value?.validate()
  } catch {
    console.warn('[年度预算] 表单验证失败')
    return
  }
  submitLoading.value = true
  try {
    const total = formData.items.reduce((sum, item) => sum + (item.budgetAmount || 0), 0)
    formData.totalAmount = total

    if (isEdit.value && editId.value) {
      await annualBudgetApi.update(editId.value, { ...formData })
      message.success('更新成功')
    } else {
      await annualBudgetApi.create({ ...formData })
      message.success('创建成功')
    }
    if (!keepOpen) formVisible.value = false
    loadData()
  } catch (e: any) {
    console.warn('[年度预算] 操作失败', e)
    message.error(e?.response?.data?.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// ── 审批操作 ────────────────────────────────────────────
const handleSubmit = async (record: AnnualBudget) => {
  try {
    await annualBudgetApi.submit(record.id)
    message.success('已提交审批')
    loadData()
  } catch (e: any) {
    console.warn('[年度预算] 提交失败', e)
    message.error(e?.response?.data?.message || '提交失败')
  }
}

const handleApprove = async (record: AnnualBudget) => {
  try {
    await annualBudgetApi.approve(record.id)
    message.success('审批通过')
    loadData()
  } catch (e: any) {
    console.warn('[年度预算] 审批失败', e)
    message.error(e?.response?.data?.message || '审批失败')
  }
}

const handleReject = async (record: AnnualBudget) => {
  try {
    await annualBudgetApi.reject(record.id)
    message.success('已拒绝')
    loadData()
  } catch (e: any) {
    console.warn('[年度预算] 拒绝失败', e)
    message.error(e?.response?.data?.message || '拒绝失败')
  }
}

const handleStartExec = async (record: AnnualBudget) => {
  try {
    await annualBudgetApi.startExec(record.id)
    message.success('已开始执行')
    loadData()
  } catch (e: any) {
    console.warn('[年度预算] 执行失败', e)
    message.error(e?.response?.data?.message || '执行失败')
  }
}

const handleClose = async (record: AnnualBudget) => {
  try {
    await annualBudgetApi.close(record.id)
    message.success('已关闭')
    loadData()
  } catch (e: any) {
    console.warn('[年度预算] 关闭失败', e)
    message.error(e?.response?.data?.message || '关闭失败')
  }
}

const handleDelete = async (record: AnnualBudget) => {
  try {
    await annualBudgetApi.delete(record.id)
    message.success('删除成功')
    loadData()
  } catch (e: any) {
    console.warn('[年度预算] 删除失败', e)
    message.error(e?.response?.data?.message || '删除失败')
  }
}

function handleActionMenuClick(key: string, record: AnnualBudget) {
  switch (key) {
    case 'submit': handleSubmit(record); break
    case 'approve': handleApprove(record); break
    case 'reject': handleReject(record); break
    case 'startExec': handleStartExec(record); break
    case 'close': handleClose(record); break
    case 'delete': handleDelete(record); break
  }
}

// ── 导出 ────────────────────────────────────────────────
async function handleExport() {
  try {
    const res = await annualBudgetApi.export({
      keyword: searchFilters.keyword || undefined,
      fiscalYear: searchFilters.fiscalYear || undefined,
      departmentId: searchFilters.departmentId || undefined,
      status: searchFilters.status || undefined,
    })
    if (res.code === 200 && res.data?.length) {
      const csv = res.map((r: any) => `${r.budgetNo},${r.departmentName},${r.fiscalYear},${r.totalAmount},${r.status},${r.createdAt}`).join('\n')
      const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url; a.download = `年度预算_${new Date().toISOString().slice(0, 10)}.csv`
      a.click(); URL.revokeObjectURL(url)
    }
    message.success('导出成功')
  } catch {
    message.error('导出失败')
  }
}

// ── 批量操作 ────────────────────────────────────────────
function canBatchSubmit(rows: AnnualBudget[]) {
  return rows.some(r => r.status === 'draft')
}

async function handleBatchSubmit(rows: AnnualBudget[]) {
  const draftIds = rows.filter(r => r.status === 'draft').map(r => r.id)
  if (!draftIds.length) { message.warning('选中的记录中没有可提交的草稿'); return }
  Modal.confirm({
    title: '批量提交',
    content: `确定提交选中的 ${draftIds.length} 项草稿？`,
    onOk: async () => {
      try {
        await Promise.all(draftIds.map(id => annualBudgetApi.submit(id)))
        message.success(`已提交 ${draftIds.length} 项`)
        loadData()
      } catch { message.error('批量提交失败') }
    },
  })
}

async function handleBatchDelete(ids: number[] | AnnualBudget[]) {
  const actualIds = Array.isArray(ids) ? ids.map(id => typeof id === 'number' ? id : id.id) : []
  if (!actualIds.length) { message.warning('请选择要删除的记录'); return }
  Modal.confirm({
    title: '批量删除',
    content: `确定删除 ${actualIds.length} 项预算？`,
    okType: 'danger',
    onOk: async () => {
      try {
        await Promise.all(actualIds.map(id => annualBudgetApi.delete(id)))
        message.success(`已删除 ${actualIds.length} 项`)
        loadData()
      } catch { message.error('批量删除失败') }
    },
  })
}

// ── Template selection ──────────────────────────────────
const templateModalVisible = ref(false)
const templateList = ref<any[]>([])
const templateLoading = ref(false)
const templateSelectedKeys = ref<number[]>([])

const templateVxeColumns = [
  { field: 'templateCode', title: '模板编码' },
  { field: 'templateName', title: '模板名称' },
  { field: 'fiscalYear', title: '年度', width: 70 },
  { field: 'totalAmount', title: '金额', align: 'right' },
]

const handleCreateFromTemplate = async () => {
  templateLoading.value = true
  templateSelectedKeys.value = []
  try {
    const res = await budgetTemplateApi.listByYear(new Date().getFullYear())
    if (res.code === 200) {
      templateList.value = res.data || []
    }
    templateModalVisible.value = true
  } catch {
    console.warn('[年度预算] 加载模板列表失败')
    templateList.value = []
    templateModalVisible.value = true
  } finally {
    templateLoading.value = false
  }
}

const handleTemplateSelectOk = async () => {
  if (templateSelectedKeys.value.length === 0) {
    message.warning('请选择一个模板')
    return
  }
  const templateId = templateSelectedKeys.value[0]
  templateModalVisible.value = false
  try {
    const res = await budgetTemplateApi.getById(templateId)
    if (res.code === 200) {
      const t = res.data
      resetForm()
      formData.templateId = t.id
      formData.templateName = t.templateName
      formData.fiscalYear = t.fiscalYear
      formData.items = (t.items || []).map((item: any) => ({
        id: 0,
        budgetId: 0,
        subjectCode: item.subjectCode,
        subjectName: item.subjectName,
        budgetAmount: item.budgetAmount,
        usedAmount: 0,
        remainingAmount: item.budgetAmount || 0,
        frozenAmount: 0,
        executionRate: 0,
        sortOrder: item.sortOrder,
        rowKey: genRowKey(),
      }))
      isEdit.value = false
      formVisible.value = true
      nextTick(() => saveFormSnapshot())
    }
  } catch (e: any) {
    console.warn('[年度预算] 获取模板失败', e)
    message.error(e?.response?.data?.message || '获取模板失败')
  }
}

// ── 快捷键 ──────────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5') {
    e.preventDefault()
    debounceClick('refresh', loadData)
    return
  }
  if (e.ctrlKey && e.key === 'n') {
    e.preventDefault()
    debounceClick('add', handleAdd)
    return
  }
  if ((e.ctrlKey && e.key === 'f') || e.key === 'F3') {
    // VxeTableList handles this
  }
}

// ── 生命周期 ────────────────────────────────────────────
onMounted(() => {
  loadData()

  // 检查 URL 参数 edit=xxx
  if (route.query.edit) {
    const editId = Number(route.query.edit)
    if (editId) {
      annualBudgetApi.getById(editId).then(res => {
        if (res.code === 200) handleEdit(res.data)
      })
    }
  }

  window.addEventListener('budget:create', handleParentCreate)
  window.addEventListener('budget:refresh', loadData)
  document.addEventListener('keydown', handleKeydown)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    loadData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  window.removeEventListener('budget:create', handleParentCreate)
  window.removeEventListener('budget:refresh', loadData)
  document.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: loadData })

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
</script>

<style scoped>
.annual-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.annual-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.annual-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.annual-page-header-right {
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

.annual-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

.annual-management :deep(.vxe-table) {
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
.stat-executing { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
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
.table-empty-error { padding: 48px 0; }
.table-empty-icon-error { color: #faad14; }

.action-more-btn { padding: 0 4px; font-size: 16px; vertical-align: middle; }
.action-cell-inner { flex-wrap: nowrap; }

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}
.amount-cell.used { color: #faad14; }
.rate-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
}

.danger { color: #ff4d4f; }

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
.annual-management :deep(.ant-input-sm),
.annual-management :deep(.ant-input-number-sm),
.annual-management :deep(.ant-select-single.ant-select-sm .ant-select-selector),
.annual-management :deep(.ant-picker-small),
.annual-management :deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
.annual-management :deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
.annual-management :deep(.ant-input-number-sm input) {
  height: 26px;
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
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
.annual-management :deep(.vxe-table-list-container .vxe-header--row .vxe-header--column) {
  border-bottom: 2px solid #d0d5dd !important;
}

</style>
