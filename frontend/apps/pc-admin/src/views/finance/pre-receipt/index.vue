<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="pre-receipt-page-header">
        <div class="pre-receipt-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>财务管理</a-breadcrumb-item>
            <a-breadcrumb-item>预收款管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="pre-receipt-page-header-title">预收款管理</h2>
        </div>
        <div class="pre-receipt-page-header-right">
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
            <span class="shortcut-hint"><kbd>Ctrl+N</kbd> 新增</span>
          </span>
        </div>
      </div>
    </template>

    <div class="finance-pre-receipt-page">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.totalAmount) }}</div>
            <div class="stat-card-label">预收总额</div>
          </div>
          <DollarOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-used">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.usedAmount) }}</div>
            <div class="stat-card-label">已使用金额</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-balance">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(stats.remainingAmount) }}</div>
            <div class="stat-card-label">剩余金额</div>
          </div>
          <ExclamationCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-count">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pagination.total }}</div>
            <div class="stat-card-label">笔数</div>
          </div>
          <FileTextOutlined class="stat-card-icon" />
        </div>
      </div>

      <VxeTableList
        ref="tableRef"
        :min-empty-rows="12"
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="pagination"
        :row-key="'id'"
        :filter-fields="filterFields"
        :show-search="false"
        export-permission="finance:pre-receipt:list"
        :show-add="true"
        add-text="新增预收款"
        :show-edit="false"
        :show-delete="false"
        :selectable="true"
        @refresh="fetchData"
        @add="handleAdd"
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
                没有符合条件的预收款记录，<a @click="handleResetFilters">清除筛选</a>
              </p>
              <p v-else class="table-empty-text">
                暂无预收款数据
              </p>
            </template>
          </div>
        </template>

        <template #statusCell="{ record }">
          <a-tag :color="statusColorMap[record.status] || 'default'">{{ statusLabelMap[record.status] || '未知' }}</a-tag>
        </template>
        <template #action="{ record }">
          <a-space :size="0" class="action-cell-inner">
            <PrintButton
              template-type="pre_receipt"
              :business-id="record.id"
              business-type="pre_receipt"
              button-text=""
              button-size="small"
              button-type="link"
              tooltip="打印"
            />
            <a-tooltip title="查看">
              <a-button type="link" size="small" v-permission="'finance:pre-receipt:view'" @click="handleView(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip :title="record.status !== 'received' ? '' : '转收款'">
              <a-button type="link" size="small" v-permission="'finance:pre-receipt:offset'" :disabled="record.status !== 'received'" @click="handleOffsetToReceipt(record)">
                <template #icon><SwapOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip :title="record.status !== 'received' ? '' : '没收'">
              <a-button type="link" size="small" v-permission="'finance:pre-receipt:forfeit'" :disabled="record.status !== 'received'" @click="handleForfeit(record)">
                <template #icon><StopOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip :title="record.status !== 'received' ? '' : '退款'">
              <a-button type="link" size="small" v-permission="'finance:pre-receipt:refund'" :disabled="record.status !== 'received'" @click="handleRefund(record)">
                <template #icon><RollbackOutlined /></template>
              </a-button>
            </a-tooltip>
          </a-space>
        </template>
      </VxeTableList>

      <!-- 转收款弹窗 -->
      <a-modal
        :open="offsetVisible"
        title="转收款"
        :confirm-loading="offsetLoading"
        @ok="handleOffsetConfirm"
        @cancel="handleOffsetCancel"
      >
        <a-descriptions v-if="offsetTarget" :column="1" bordered size="small">
          <a-descriptions-item label="客户名称">{{ offsetTarget.customerName }}</a-descriptions-item>
          <a-descriptions-item label="预收单号">{{ offsetTarget.preReceiptNo }}</a-descriptions-item>
          <a-descriptions-item label="预收金额">{{ formatAmount(offsetTarget.amount) }}</a-descriptions-item>
          <a-descriptions-item label="已使用">{{ formatAmount(offsetTarget.usedAmount) }}</a-descriptions-item>
          <a-descriptions-item label="剩余金额">{{ formatAmount(offsetTarget.remainingAmount) }}</a-descriptions-item>
        </a-descriptions>
        <a-form layout="vertical" style="margin-top: 16px">
          <a-form-item label="收款金额" required>
            <a-input-number
              v-model:value="offsetAmount"
              :min="0.01"
              :max="offsetTarget?.remainingAmount || 0"
              :precision="2"
              size="small"
              style="width: 100%"
              placeholder="请输入收款金额"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 没收弹窗 -->
      <a-modal
        :open="forfeitVisible"
        title="没收预收款"
        :confirm-loading="forfeitLoading"
        @ok="handleForfeitConfirm"
        @cancel="handleForfeitCancel"
      >
        <a-descriptions v-if="forfeitTarget" :column="1" bordered size="small">
          <a-descriptions-item label="客户名称">{{ forfeitTarget.customerName }}</a-descriptions-item>
          <a-descriptions-item label="预收单号">{{ forfeitTarget.preReceiptNo }}</a-descriptions-item>
          <a-descriptions-item label="剩余金额">{{ formatAmount(forfeitTarget.remainingAmount) }}</a-descriptions-item>
        </a-descriptions>
        <a-form layout="vertical" style="margin-top: 16px">
          <a-form-item label="没收原因" required>
            <a-textarea
              v-model:value="forfeitReason"
              :rows="3"
              placeholder="请输入没收原因"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 退款弹窗 -->
      <a-modal
        :open="refundVisible"
        title="预收款退款"
        :confirm-loading="refundLoading"
        @ok="handleRefundConfirm"
        @cancel="handleRefundCancel"
      >
        <a-descriptions v-if="refundTarget" :column="1" bordered size="small">
          <a-descriptions-item label="客户名称">{{ refundTarget.customerName }}</a-descriptions-item>
          <a-descriptions-item label="预收单号">{{ refundTarget.preReceiptNo }}</a-descriptions-item>
          <a-descriptions-item label="剩余金额">{{ formatAmount(refundTarget.remainingAmount) }}</a-descriptions-item>
        </a-descriptions>
        <a-form layout="vertical" style="margin-top: 16px">
          <a-form-item label="退款原因" required>
            <a-textarea
              v-model:value="refundReason"
              :rows="3"
              placeholder="请输入退款原因"
            />
          </a-form-item>
        </a-form>
      </a-modal>

      <!-- 新增预收款弹窗 -->
      <FullScreenDetail
        :visible="addVisible"
        title="新增预收款"
        :dirty="formDirty"
        :save-loading="addLoading"
        @save="handleAddConfirm"
        @close="handleAddCancel"
      >
        <a-form
          ref="addFormRef"
          :model="addForm"
          :rules="addFormRules"
          :label-col="{ span: 6 }"
          :wrapper-col="{ span: 16 }"
        >
          <a-form-item label="客户名称" name="customerName">
            <a-input v-model:value="addForm.customerName" placeholder="请输入客户名称" />
          </a-form-item>
          <a-form-item label="预收金额" name="amount">
            <a-input-number
              v-model:value="addForm.amount"
              :min="0.01"
              :precision="2"
              style="width: 100%"
              placeholder="请输入预收金额"
            />
          </a-form-item>
          <a-form-item label="收款日期" name="dueDate">
            <a-date-picker
              v-model:value="addForm.dueDate"
              style="width: 100%"
              placeholder="请选择收款日期"
            />
          </a-form-item>
          <a-form-item label="备注" name="remark">
            <a-textarea v-model:value="addForm.remark" :rows="3" placeholder="备注信息" />
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
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined, CheckCircleOutlined, InboxOutlined, PlusOutlined,
  DollarOutlined, ExclamationCircleOutlined, WarningOutlined, FileTextOutlined,
  SyncOutlined, ReloadOutlined, EyeOutlined, SwapOutlined, StopOutlined, RollbackOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import { preReceiptApi } from '@/api/finance'
import request from '@/utils/request'

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now(); const last = debounceMap.get(key) || 0
  if (now - last < delay) return; debounceMap.set(key, now); fn()
}

const tableRef = ref()
const loading = ref(false)
const hasError = ref(false)
const refreshLoading = ref(false)
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const searchForm = reactive({
  customerName: '',
  status: undefined as string | undefined
})

const filterFields = [
  { key: 'customerName', label: '客户名称', type: 'input' as const, placeholder: '客户名称' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '已收款', value: 'received' },
    { label: '已转收款', value: 'offset' },
    { label: '已没收', value: 'forfeited' },
    { label: '已退款', value: 'refunded' }
  ]}
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
  return Object.values(searchForm).some(v => v !== undefined && v !== null && v !== '')
})

// ── 统计数据 ────────────────────────────────────────────
const stats = reactive({
  totalAmount: 0,
  usedAmount: 0,
  remainingAmount: 0
})

const statusColorMap: Record<string, string> = {
  received: 'processing',
  offset: 'success',
  forfeited: 'error',
  refunded: 'warning'
}

const statusLabelMap: Record<string, string> = {
  received: '已收款',
  offset: '已转收款',
  forfeited: '已没收',
  refunded: '已退款'
}

const columns = computed(() => [
  { title: '预收单号', field: 'preReceiptNo', width: 150 },
  { title: '客户名称', field: 'customerName', width: 150 },
  { title: '金额', field: 'amount', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '已使用', field: 'usedAmount', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '剩余金额', field: 'remainingAmount', width: 120, align: 'right', formatter: ({ cellValue }: any) => cellValue ? `¥${formatAmount(cellValue)}` : '-' },
  { title: '收款日期', field: 'receiptDate', width: 110 },
  { title: '状态', field: 'status', width: 100, align: 'center', slotName: 'statusCell' },
  { title: '操作', type: 'action', width: 160, fixed: 'right' }
])

// 转收款弹窗
const offsetVisible = ref(false)
const offsetLoading = ref(false)
const offsetTarget = ref<any>(null)
const offsetAmount = ref(0)

// 没收弹窗
const forfeitVisible = ref(false)
const forfeitLoading = ref(false)
const forfeitTarget = ref<any>(null)
const forfeitReason = ref('')

// 退款弹窗
const refundVisible = ref(false)
const refundLoading = ref(false)
const refundTarget = ref<any>(null)
const refundReason = ref('')

const fetchData = async () => {
  loading.value = true
  refreshLoading.value = true
  try {
    const params: any = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    if (searchForm.customerName) params.customerName = searchForm.customerName
    if (searchForm.status !== undefined) params.status = searchForm.status

    const res = await preReceiptApi.getPage(params)
    if (res.data) {
      tableData.value = res.records || (res.data as any).list || []
      pagination.total = res.total || 0
      lastUpdated.value = new Date().toISOString()
      // 更新统计（优先使用后端汇总数据）
      if ((res.data as any).totalAmount !== undefined) {
        stats.totalAmount = (res.data as any).totalAmount
        stats.usedAmount = (res.data as any).totalUsedAmount || 0
        stats.remainingAmount = (res.data as any).totalRemainingAmount || 0
      } else {
        stats.totalAmount = tableData.value.reduce((sum, item) => sum + (item.amount || 0), 0)
        stats.usedAmount = tableData.value.reduce((sum, item) => sum + (item.usedAmount || 0), 0)
        stats.remainingAmount = tableData.value.reduce((sum, item) => sum + (item.remainingAmount || 0), 0)
      }
    }
    hasError.value = false
  } catch (err) {
    console.warn('[预收款] 获取数据失败', err)
    message.error('获取预收款数据失败')
    tableData.value = []
    pagination.total = 0
    hasError.value = true
  } finally {
    loading.value = false
    refreshLoading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  }
}

const handleView = (record: any) => {
  message.info(`查看详情: ${record.preReceiptNo}`)
}

const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  searchForm.customerName = ''
  searchForm.status = undefined
  handleSearch()
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

const handleFilterChange = (filters: Record<string, any>) => {
  searchForm.customerName = filters.customerName || ''
  searchForm.status = filters.status !== undefined ? filters.status : undefined
  pagination.current = 1
  fetchData()
}

const handleSelectionChange = (rows: any[], ids: any[]) => {
  // 可以在这里处理选中行的逻辑，例如批量操作
}

// 转收款
const handleOffsetToReceipt = (record: any) => {
  offsetTarget.value = record
  offsetAmount.value = record.remainingAmount
  offsetVisible.value = true
}

const handleOffsetConfirm = async () => {
  if (!offsetAmount.value || offsetAmount.value <= 0) {
    message.warning('请输入有效的收款金额')
    return
  }
  if (offsetAmount.value > (offsetTarget.value?.remainingAmount || 0)) {
    message.warning('收款金额不能大于剩余金额')
    return
  }
  offsetLoading.value = true
  try {
    await preReceiptApi.offsetToReceipt(offsetTarget.value.id, 0, offsetAmount.value)
    message.success('转收款成功')
    offsetVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[预收款] 转收款失败', err)
    message.error('转收款失败')
  } finally {
    offsetLoading.value = false
  }
}

const handleOffsetCancel = () => {
  offsetVisible.value = false
}

// 没收
const handleForfeit = (record: any) => {
  forfeitTarget.value = record
  forfeitReason.value = ''
  forfeitVisible.value = true
}

const handleForfeitConfirm = async () => {
  if (!forfeitReason.value.trim()) {
    message.warning('请输入没收原因')
    return
  }
  forfeitLoading.value = true
  try {
    await preReceiptApi.forfeit(forfeitTarget.value.id, forfeitReason.value)
    message.success('没收成功')
    forfeitVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[预收款] 没收失败', err)
    message.error('没收失败')
  } finally {
    forfeitLoading.value = false
  }
}

const handleForfeitCancel = () => {
  forfeitVisible.value = false
}

// 退款
const handleRefund = (record: any) => {
  refundTarget.value = record
  refundReason.value = ''
  refundVisible.value = true
}

const handleRefundConfirm = async () => {
  if (!refundReason.value.trim()) {
    message.warning('请输入退款原因')
    return
  }
  refundLoading.value = true
  try {
    await preReceiptApi.refund(refundTarget.value.id, refundReason.value)
    message.success('退款成功')
    refundVisible.value = false
    fetchData()
  } catch (err) {
    console.warn('[预收款] 退款失败', err)
    message.error('退款失败')
  } finally {
    refundLoading.value = false
  }
}

const handleRefundCancel = () => {
  refundVisible.value = false
}

function handleResetFilters() {
  Object.keys(searchForm).forEach(k => { (searchForm as any)[k] = undefined })
  pagination.current = 1; fetchData()
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

// ── 新增预收款 ────────────────────────────────────────────
const addVisible = ref(false)
const addLoading = ref(false)
const addFormRef = ref()
const initialAddSnapshot = ref('')
const formDirty = computed(() => {
  if (!addVisible.value) return false
  return JSON.stringify(addForm) !== initialAddSnapshot.value
})
const addForm = reactive({
  customerName: '',
  amount: undefined as number | undefined,
  dueDate: undefined as any,
  remark: ''
})
const addFormRules: any = {
  customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入预收金额', trigger: 'blur' }]
}
function handleAdd() {
  addForm.customerName = ''
  addForm.amount = undefined
  addForm.dueDate = undefined
  addForm.remark = ''
  addVisible.value = true
  initialAddSnapshot.value = JSON.stringify(addForm)
}
async function handleAddConfirm() {
  try {
    await addFormRef.value?.validate()
    addLoading.value = true
    const params: any = {
      customerName: addForm.customerName,
      amount: addForm.amount,
      remark: addForm.remark || ''
    }
    if (addForm.dueDate) {
      params.dueDate = typeof addForm.dueDate === 'string' ? addForm.dueDate : addForm.dueDate?.format?.('YYYY-MM-DD') || ''
    }
    await request.post('/erp/pre-receipt', params)
    message.success('预收款创建成功')
    addVisible.value = false
    fetchData()
  } catch (err: any) {
    if (err?.message) message.error(err.message)
  } finally {
    addLoading.value = false
  }
}
function handleAddCancel() {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认关闭',
      content: '有未保存的修改，确定关闭吗？',
      onOk: () => { addVisible.value = false }
    })
  } else {
    addVisible.value = false
  }
}

function handleParentCreate() {
  handleAdd()
}

const formatAmount = (val: number) => {
  if (val === undefined || val === null) return '0.00'
  return '¥' + Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

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
.pre-receipt-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.pre-receipt-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.pre-receipt-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.pre-receipt-page-header-right {
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

.finance-pre-receipt-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  gap: 16px;
}

.finance-pre-receipt-page > :deep(.vxe-table-list-container) {
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
.stat-used { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
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

.table-empty-action {
  margin-top: 12px;
}

.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
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
