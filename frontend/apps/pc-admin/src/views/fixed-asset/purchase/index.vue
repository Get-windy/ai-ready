<template>
  <PageContainer full-height>
    <template #header>
      <div class="purchase-page-header">
        <div class="purchase-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>固定资产</a-breadcrumb-item>
            <a-breadcrumb-item>购置申请</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="purchase-page-header-title">购置申请</h2>
        </div>
        <div class="purchase-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <PrintButton business-type="fixed_asset_purchase" button-type="link" button-size="small" tooltip="打印购置申请" />
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)()" v-permission="'erp:fixed-asset:purchase:list'">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <div class="purchase-list-page">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-draft">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statusCounts.draft }}</div>
            <div class="stat-card-label">草稿</div>
          </div>
          <FileTextOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-pending">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statusCounts.pending }}</div>
            <div class="stat-card-label">待审批</div>
          </div>
          <ClockCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-approved">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statusCounts.approved }}</div>
            <div class="stat-card-label">待验收</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-accepted">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statusCounts.accepted }}</div>
            <div class="stat-card-label">已入库</div>
          </div>
          <InboxOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-count">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pagination.total }}</div>
            <div class="stat-card-label">总申请数</div>
          </div>
          <DollarOutlined class="stat-card-icon" />
        </div>
      </div>

      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableDataSource"
        :loading="loading"
        :pagination="pagination"
        :table-key="'fixed-asset-purchase-list'"
        :filter-fields="filterFields"
        :show-export="true"
        export-permission="erp:fixed-asset:purchase:list"
        :selectable="true"
        add-text="新增购置申请"
        add-permission="erp:fixed-asset:purchase:create"
        delete-permission="erp:fixed-asset:purchase:delete"
        @add="showCreateModal"
        @cell-dblclick="viewDetail"
        @edit="editRecord"
        @delete="handleDelete"
        @batch-delete="handleBatchDelete"
        @refresh="debounceClick('refresh', fetchData)"
        @search="handleSearch"
        @page-change="handlePageChange"
        @filter-change="handleFilterChange"
        @export="handleExport"
        @selection-change="handleSelectionChange"
      >
        <template #toolbar-actions>
          <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
            更新 {{ dayjs(lastUpdated).format('HH:mm') }}
          </span>
        </template>

        <template #batch-actions>
          <a-button size="small" type="primary" ghost @click="handleBatchApprove">
            <template #icon><CheckCircleOutlined /></template>
            批量审批
          </a-button>
        </template>

        <template #empty>
          <div v-if="hasError" class="table-empty table-empty-error">
            <WarningOutlined class="table-empty-icon table-empty-icon-error" />
            <p class="table-empty-text">数据加载失败，请重试</p>
            <a-button size="small" @click="debounceClick('refresh', fetchData)()">
              <template #icon><ReloadOutlined /></template>
              重试
            </a-button>
          </div>
          <div v-else class="table-empty">
            <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
            <InboxOutlined v-else class="table-empty-icon" />
            <p v-if="hasActiveFilters" class="table-empty-text">
              没有符合条件的购置申请，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无购置申请，点击右上角「新增购置申请」开始创建
            </p>
          </div>
        </template>

        <template #purchaseNoCell="{ record }">
          <a @click="viewDetail(record)" class="purchase-no">{{ record.purchaseNo }}</a>
        </template>
        <template #statusCell="{ record }">
          <a-tag :color="statusColorMap[record.status]">{{ statusMap[record.status] }}</a-tag>
        </template>
        <template #estimatedAmountCell="{ record }">
          <span class="amount-cell">¥{{ formatAmount(record.estimatedAmount) }}</span>
        </template>
        <template #actualAmountCell="{ record }">
          <span class="amount-cell">¥{{ formatAmount(record.actualAmount) }}</span>
        </template>

        <template #action="{ record }">
          <a-space :size="4">
            <a-tooltip title="查看详情">
              <a-button type="link" size="small" @click="viewDetail(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 'draft'" title="编辑">
              <a-button type="link" size="small" @click="editRecord(record)" v-permission="'erp:fixed-asset:purchase:update'">
                <template #icon><EditOutlined /></template>
              </a-button>
            </a-tooltip>
            <PrintButton
              template-type="purchase"
              :business-id="record.id"
              business-type="fixed_asset_purchase"
              button-text=""
              button-size="small"
              button-type="link"
              tooltip="打印"
            />
            <a-dropdown trigger="click">
              <a-button type="link" size="small" class="action-more-btn">
                <template #icon><EllipsisOutlined /></template>
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
                  <a-menu-item v-if="record.status === 'draft'" key="submit" v-permission="'erp:fixed-asset:purchase:submit'">
                    <SendOutlined /> 提交审批
                  </a-menu-item>
                  <a-menu-item v-if="record.status === 'pending'" key="approve" v-permission="'erp:fixed-asset:purchase:approve'">
                    <CheckCircleOutlined /> 审批通过
                  </a-menu-item>
                  <a-menu-item v-if="record.status === 'pending'" key="reject" v-permission="'erp:fixed-asset:purchase:approve'">
                    <CloseCircleOutlined /> 审批拒绝
                  </a-menu-item>
                  <a-menu-item v-if="record.status === 'approved'" key="accept" v-permission="'erp:fixed-asset:purchase:accept'">
                    <InboxOutlined /> 入库验收
                  </a-menu-item>
                  <a-menu-divider />
                  <a-menu-item v-if="record.status === 'draft'" key="delete" danger>
                    <DeleteOutlined /> 删除
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </VxeTableList>

      <!-- Create/Edit Modal -->
      <FullScreenDetail
        :visible="modalVisible"
        :title="isEdit ? '编辑购置申请' : '新增购置申请'"
        :save-loading="modalLoading"
        :show-save-and-new="!isEdit"
        @save="handleModalOk"
        @close="handleFormClose"
        @save-and-new="handleFormSaveAndNew"
      >
        <a-form :model="formData" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="申请标题" required>
            <a-input v-model:value="formData.title" placeholder="申请标题" size="small" />
          </a-form-item>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="资产名称" required>
                <a-input v-model:value="formData.assetName" placeholder="资产名称" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="规格型号">
                <a-input v-model:value="formData.specification" placeholder="规格型号" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="分类">
                <a-input v-model:value="formData.categoryName" placeholder="分类名称" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="数量">
                <a-input-number v-model:value="formData.quantity" :min="1" style="width: 100%" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="预估金额">
                <a-input-number v-model:value="formData.estimatedAmount" :precision="2" :min="0" style="width: 100%" size="small">
                  <template #addonBefore>¥</template>
                </a-input-number>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="预计交付日期">
                <a-date-picker v-model:value="formData.expectedDeliveryDate" style="width: 100%" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="申请人姓名">
                <a-input v-model:value="formData.applicantName" placeholder="申请人" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="部门名称">
                <a-input v-model:value="formData.departmentName" placeholder="部门" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="供应商">
            <a-input v-model:value="formData.supplierName" placeholder="供应商名称" size="small" />
          </a-form-item>
          <a-form-item label="购置原因">
            <a-textarea v-model:value="formData.purchaseReason" :rows="3" placeholder="请输入购置原因" size="small" />
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea v-model:value="formData.remark" :rows="2" placeholder="备注" size="small" />
          </a-form-item>
        </a-form>
      </FullScreenDetail>

      <!-- 详情弹窗 -->
      <a-drawer
        v-model:open="detailVisible"
        title="购置申请详情"
        placement="right"
        width="80vw"
      >
        <a-descriptions bordered :column="2" v-if="currentRecord">
          <a-descriptions-item label="申请单号">{{ currentRecord.purchaseNo }}</a-descriptions-item>
          <a-descriptions-item label="申请标题">{{ currentRecord.title }}</a-descriptions-item>
          <a-descriptions-item label="资产名称">{{ currentRecord.assetName }}</a-descriptions-item>
          <a-descriptions-item label="规格型号">{{ currentRecord.specification || '-' }}</a-descriptions-item>
          <a-descriptions-item label="分类">{{ currentRecord.categoryName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="数量">{{ currentRecord.quantity }}</a-descriptions-item>
          <a-descriptions-item label="预估金额">
            <span class="amount-cell">¥{{ formatAmount(currentRecord.estimatedAmount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="实际金额">
            <span class="amount-cell">¥{{ formatAmount(currentRecord.actualAmount) }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="申请人">{{ currentRecord.applicantName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="部门">{{ currentRecord.departmentName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="申请日期">{{ currentRecord.applyDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="预计交付日期">{{ currentRecord.expectedDeliveryDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="实际交付日期">{{ currentRecord.actualDeliveryDate || '-' }}</a-descriptions-item>
          <a-descriptions-item label="供应商">{{ currentRecord.supplierName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="statusColorMap[currentRecord.status]">{{ statusMap[currentRecord.status] }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="审批意见">{{ currentRecord.approvalComment || '-' }}</a-descriptions-item>
          <a-descriptions-item label="购置原因" :span="2">{{ currentRecord.purchaseReason || '-' }}</a-descriptions-item>
          <a-descriptions-item v-if="currentRecord.generatedAssetId" label="生成资产" :span="2">
            <a @click="handleViewAsset(currentRecord)">查看资产卡片 (ID: {{ currentRecord.generatedAssetId }})</a>
          </a-descriptions-item>
        </a-descriptions>

        <div class="detail-modal-footer">
          <a-button v-if="currentRecord?.status === 'draft'" type="primary" @click="handleSubmit(currentRecord)">提交审批</a-button>
          <a-button v-if="currentRecord?.status === 'pending'" type="primary" @click="handleApprove(currentRecord)">审批通过</a-button>
          <a-button v-if="currentRecord?.status === 'pending'" @click="handleReject(currentRecord)">审批拒绝</a-button>
          <a-button v-if="currentRecord?.status === 'approved'" type="primary" @click="showAcceptModal(currentRecord)">入库验收</a-button>
          <PrintButton :business-id="currentRecord?.id" business-type="fixed_asset_purchase" button-size="small" tooltip="打印" />
          <a-button @click="detailVisible = false">关闭</a-button>
        </div>
      </a-drawer>

      <!-- 入库验收弹窗 -->
      <a-modal
        v-model:open="acceptModalVisible"
        title="入库验收"
        :confirm-loading="acceptLoading"
        @ok="handleAccept"
        ok-text="确认入库"
      >
        <a-form :model="acceptForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="实际金额">
            <a-input-number v-model:value="acceptForm.actualAmount" :precision="2" :min="0" style="width: 100%" size="small">
              <template #addonBefore>¥</template>
            </a-input-number>
          </a-form-item>
          <a-form-item label="实际交付日期">
            <a-date-picker v-model:value="acceptForm.actualDeliveryDate" style="width: 100%" size="small" />
          </a-form-item>
          <a-form-item label="供应商">
            <a-input v-model:value="acceptForm.supplierName" placeholder="供应商名称" size="small" />
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea v-model:value="acceptForm.remark" :rows="2" placeholder="入库验收备注" size="small" />
          </a-form-item>
        </a-form>
      </a-modal>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined, InboxOutlined, EllipsisOutlined, EyeOutlined, EditOutlined,
  CheckCircleOutlined, CloseCircleOutlined, DeleteOutlined, SendOutlined,
  ClockCircleOutlined, DollarOutlined, FileTextOutlined, SyncOutlined, ReloadOutlined,
  WarningOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { purchaseApi } from '@/api/fixed-asset'
import { PageContainer, FullScreenDetail } from '@/components'
import { useRouter } from 'vue-router'

const emit = defineEmits(['update-count'])
const router = useRouter()

const loading = ref(false)
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const detailVisible = ref(false)
const currentRecord = ref<any>(null)
const tableData = ref<any[]>([])
const tableRef = ref()
const lastUpdated = ref('')
const selectedRowKeys = ref<number[]>([])
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
const hasError = ref(false)
const acceptModalVisible = ref(false)
const acceptLoading = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const searchFilters = reactive<Record<string, any>>({})

// ── 防抖工具 ────────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

// ── 表单数据 ────────────────────────────────────────────
const formData = reactive({
  title: '',
  applicantName: '',
  departmentName: '',
  assetName: '',
  categoryName: '',
  quantity: 1,
  estimatedAmount: undefined as any,
  expectedDeliveryDate: undefined as any,
  specification: '',
  supplierName: '',
  purchaseReason: '',
  remark: '',
})

// ── 表单脏检测 ──────────────────────────────────────────
const initialFormSnapshot = ref('')
let watchReady = false
const formDirty = computed(() => {
  if (!watchReady) return false
  return JSON.stringify(formData) !== initialFormSnapshot.value
})
function saveFormSnapshot() { initialFormSnapshot.value = JSON.stringify(formData) }

// ── 离开守卫 ────────────────────────────────────────────
onBeforeRouteLeave((to, from, next) => {
  if (!formDirty.value) { next(); return }
  Modal.confirm({
    title: '确认离开',
    content: '您有未保存的修改，确定要离开吗？',
    okText: '离开',
    cancelText: '继续编辑',
    onOk: () => next(),
    onCancel: () => next(false),
  })
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})

function handleSelectionChange(keys: number[]) {
  selectedRowKeys.value = keys
}

// ── 统计数据 ────────────────────────────────────────────
const statusCounts = computed(() => {
  const draft = tableData.value.filter(r => r.status === 'draft').length
  const pending = tableData.value.filter(r => r.status === 'pending').length
  const approved = tableData.value.filter(r => r.status === 'approved').length
  const accepted = tableData.value.filter(r => r.status === 'accepted').length
  return { draft, pending, approved, accepted }
})

// ── 数据源 ────────────────────────────────────────────
const tableDataSource = tableData

const vxeColumns = computed(() => [
  { field: 'purchaseNo', title: '申请单号', width: 150, slotName: 'purchaseNoCell' },
  { field: 'title', title: '申请标题', width: 200 },
  { field: 'assetName', title: '资产名称', width: 140 },
  { field: 'specification', title: '规格型号', width: 100 },
  { field: 'quantity', title: '数量', width: 60, align: 'center' },
  { field: 'estimatedAmount', title: '预估金额', width: 120, align: 'right', slotName: 'estimatedAmountCell' },
  { field: 'applicantName', title: '申请人', width: 100 },
  { field: 'departmentName', title: '部门', width: 100 },
  { field: 'status', title: '状态', width: 80, align: 'center', slotName: 'statusCell' },
  { type: 'action', title: '操作', width: 160, fixed: 'right' },
])

const filterFields = [
  { key: 'purchaseNo', label: '申请单号', type: 'input' as const, placeholder: '申请单号' },
  { key: 'title', label: '申请标题', type: 'input' as const, placeholder: '申请标题' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 'draft' },
    { label: '待审批', value: 'pending' },
    { label: '已通过', value: 'approved' },
    { label: '已拒绝', value: 'rejected' },
    { label: '已入库', value: 'accepted' },
  ]},
]

const statusMap: Record<string, string> = {
  draft: '草稿', pending: '待审批', approved: '已通过', rejected: '已拒绝', accepted: '已入库',
}
const statusColorMap: Record<string, string> = {
  draft: 'default', pending: 'orange', approved: 'green', rejected: 'red', accepted: 'blue',
}

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

function handleParentCreate() {
  showCreateModal()
}

// ── 键盘快捷键 ──────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  const isInput = tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT'
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !isInput) {
    e.preventDefault()
    debounceClick('refresh', fetchData)()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n' && !isInput) {
    e.preventDefault()
    showCreateModal()
  }
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('fixed-asset:create', handleParentCreate)
  window.addEventListener('fixed-asset:refresh', fetchData)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

async function fetchData() {
  loading.value = true
  hasError.value = false
  const params: any = {
    page: pagination.current - 1,
    size: pagination.pageSize,
  }
  if (searchFilters.purchaseNo) params.purchaseNo = searchFilters.purchaseNo
  if (searchFilters.title) params.title = searchFilters.title
  if (searchFilters.status) params.status = searchFilters.status

  try {
    const res = await purchaseApi.getPage(params)
    if (res.data) {
      tableData.value = res.data.content || res.data.records || []
      pagination.total = res.data.totalElements || res.data.total || 0
      lastUpdated.value = new Date().toISOString()
      emit('update-count', pagination.total)
    }
  } catch {
    hasError.value = true
    tableData.value = []
    pagination.total = 0
    console.warn('[购置申请] 加载数据失败')
    message.error('加载购置申请数据失败')
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

function handleSearch(keyword: string) {
  searchFilters.keyword = keyword || undefined
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  fetchData()
}

function showCreateModal() {
  isEdit.value = false
  editId.value = null
  Object.assign(formData, {
    title: '', applicantName: '', departmentName: '', assetName: '',
    categoryName: '', quantity: 1, estimatedAmount: undefined,
    expectedDeliveryDate: undefined, specification: '', supplierName: '',
    purchaseReason: '', remark: '',
  })
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

function editRecord(record: any) {
  isEdit.value = true
  editId.value = record.id
  Object.assign(formData, record)
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

async function viewDetail(record: any) {
  detailVisible.value = true
  try {
    const res = await purchaseApi.getById(record.id)
    currentRecord.value = res.data
  } catch {
    message.error('获取购置详情失败')
  }
}

function handleModalOk() {
  modalLoading.value = true
  const apiCall = isEdit.value && editId.value
    ? purchaseApi.update(editId.value, formData)
    : purchaseApi.create(formData)

  apiCall.then(() => {
    message.success(isEdit.value ? '更新成功' : '创建成功')
    modalVisible.value = false
    fetchData()
  }).catch((err: any) => {
    console.warn('[购置申请] 操作失败', err)
    message.error(err.message || '操作失败')
  }).finally(() => {
    modalLoading.value = false
  })
}

function handleFormClose() {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认关闭',
      content: '您有未保存的修改，确定要关闭吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => { modalVisible.value = false },
    })
  } else {
    modalVisible.value = false
  }
}

function handleFormSaveAndNew() {
  handleModalOk()
}

function handleDelete(id: number) {
  purchaseApi.delete(id).then(() => {
    message.success('删除成功')
    fetchData()
  }).catch((err: any) => {
    console.warn('[购置申请] 删除失败', err)
    message.error(err.message || '删除失败')
  })
}

function handleBatchDelete() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) {
    message.warning('请选择要删除的购置申请')
    return
  }
  Modal.confirm({
    title: '批量删除',
    content: `确认删除选中的 ${keys.length} 条购置申请？`,
    okText: '确认删除',
    okType: 'danger',
    centered: true,
    onOk: async () => {
      try {
        await Promise.all(keys.map((id: number) => purchaseApi.delete(id)))
        message.success('批量删除成功')
        fetchData()
      } catch (err: any) {
        message.error(err.message || '批量删除失败')
      }
    }
  })
}

function handleSubmit(record: any) {
  Modal.confirm({
    title: '提交审批',
    content: `提交购置申请 "${record.purchaseNo}" 进行审批？`,
    okText: '确认提交',
    centered: true,
    onOk: async () => {
      try {
        await purchaseApi.submit(record.id)
        message.success('已提交审批')
        fetchData()
        detailVisible.value = false
      } catch {
        console.warn('[购置申请] 提交失败')
        message.error('提交审批失败')
      }
    }
  })
}

function handleApprove(record: any) {
  Modal.confirm({
    title: '审批通过',
    content: `审批通过购置申请 "${record.purchaseNo}"？`,
    okText: '确认通过',
    centered: true,
    onOk: async () => {
      try {
        await purchaseApi.approve(record.id)
        message.success('已审批通过')
        fetchData()
        detailVisible.value = false
      } catch {
        console.warn('[购置申请] 审批失败')
        message.error('审批失败')
      }
    }
  })
}

function handleReject(record: any) {
  Modal.confirm({
    title: '审批拒绝',
    content: `拒绝购置申请 "${record.purchaseNo}"？`,
    okText: '确认拒绝',
    okType: 'danger',
    centered: true,
    onOk: async () => {
      try {
        await purchaseApi.reject(record.id)
        message.success('已拒绝')
        fetchData()
        detailVisible.value = false
      } catch {
        console.warn('[购置申请] 拒绝失败')
        message.error('拒绝失败')
      }
    }
  })
}

// ── 入库验收 ──────────────────────────────────────────
const acceptForm = reactive({
  actualAmount: undefined as any,
  actualDeliveryDate: undefined as any,
  supplierName: '',
  remark: '',
})
const acceptRecord = ref<any>(null)

function showAcceptModal(record: any) {
  acceptRecord.value = record
  Object.assign(acceptForm, {
    actualAmount: record.estimatedAmount || undefined,
    actualDeliveryDate: undefined,
    supplierName: record.supplierName || '',
    remark: '',
  })
  acceptModalVisible.value = true
}

function handleAccept() {
  if (!acceptRecord.value) return
  acceptLoading.value = true
  purchaseApi.accept(acceptRecord.value.id, acceptForm).then(() => {
    message.success('入库验收完成，已生成资产卡片')
    acceptModalVisible.value = false
    detailVisible.value = false
    fetchData()
  }).catch((err: any) => {
    console.warn('[购置申请] 入库验收失败', err)
    message.error(err.message || '入库验收失败')
  }).finally(() => {
    acceptLoading.value = false
  })
}

function handleBatchApprove() {
  const keys = tableRef.value?.selectedRowKeys || []
  if (keys.length === 0) {
    message.warning('请选择要审批的购置申请')
    return
  }
  Modal.confirm({
    title: '批量审批',
    content: `审批通过的 ${keys.length} 条购置申请？`,
    okText: '确认',
    centered: true,
    onOk: async () => {
      try {
        await Promise.all(keys.map((id: number) => purchaseApi.approve(id)))
        message.success(`成功审批 ${keys.length} 条购置申请`)
        fetchData()
      } catch (err: any) {
        message.error(err.message || '批量审批失败')
      }
    }
  })
}

function handleViewAsset(record: any) {
  if (record.generatedAssetId) {
    detailVisible.value = false
    router.push(`/fixed-asset/asset/detail/${record.generatedAssetId}`)
  }
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined })
  pagination.current = 1
  fetchData()
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'submit': handleSubmit(record); break
    case 'approve': handleApprove(record); break
    case 'reject': handleReject(record); break
    case 'accept': showAcceptModal(record); break
    case 'delete':
      Modal.confirm({
        title: '删除购置申请',
        content: `确认删除购置申请 "${record.purchaseNo}"？删除后数据不可恢复。`,
        okText: '确认删除',
        okType: 'danger',
        centered: true,
        onOk: () => handleDelete(record.id)
      })
      break
  }
}

function handleExport() {
  const headers = ['申请单号', '申请标题', '资产名称', '规格型号', '数量', '预估金额', '申请人', '部门', '状态', '购置原因']
  const rows = tableData.value.map((r: any) => [
    r.purchaseNo, r.title, r.assetName, r.specification, r.quantity,
    formatAmount(r.estimatedAmount), r.applicantName, r.departmentName,
    statusMap[r.status], r.purchaseReason
  ])
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `购置申请_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('fixed-asset:create', handleParentCreate)
  window.removeEventListener('fixed-asset:refresh', fetchData)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.purchase-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.purchase-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.purchase-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.purchase-page-header-right {
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

.purchase-list-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.purchase-list-page :deep(.vxe-table) {
  flex: 1;
  min-height: 0;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-draft { background: linear-gradient(135deg, #f0f0f0 0%, #e0e0e0 100%); }
.stat-pending { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-approved { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-accepted { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-count { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
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

.purchase-no {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-weight: 500;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

.action-more-btn {
  padding: 0 4px;
}

.list-update-timestamp {
  font-size: 12px;
  color: var(--color-text-tertiary, #bbb);
  white-space: nowrap;
  cursor: help;
  margin-left: 8px;
  line-height: 32px;
  vertical-align: middle;
}

.detail-modal-footer {
  text-align: right;
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
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

/* ── FullScreenDetail 内部紧凑样式 ────────────────────── */
:deep(.fsd-body .ant-form-item) { margin-bottom: 8px; }
:deep(.fsd-body .ant-form-item-label > label) { font-size: 12px; height: 28px; }
:deep(.fsd-body .ant-input), :deep(.fsd-body .ant-input-number), :deep(.fsd-body .ant-select), :deep(.fsd-body .ant-picker), :deep(.fsd-body .ant-cascader-picker) { font-size: 12px; }
:deep(.fsd-body .ant-input-number-input) { font-size: 12px; }
:deep(.fsd-body .ant-select-selection-item) { font-size: 12px; }
:deep(.fsd-body .ant-btn) { font-size: 12px; }
</style>
