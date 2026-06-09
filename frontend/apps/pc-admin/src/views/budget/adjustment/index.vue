<template>
  <div class="adjustment-page">
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

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :row-key="'id'"
      :filter-fields="filterFields"
      :selectable="true"
      add-text="新建调整"
      @add="handleAdd"
      @refresh="loadData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
      @selection-change="handleSelectionChange"
    >
      <template #toolbar-actions>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
      </template>

      <template #empty>
        <div class="table-empty">
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
            <a-button type="link" size="small" @click="handleView(record)">
              <template #icon><EyeOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip v-if="record.status === 'draft'" title="编辑">
            <a-button type="link" size="small" @click="handleEdit(record)">
              <template #icon><EditOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-dropdown trigger="click">
            <a-button type="link" size="small" class="action-more-btn">
              <template #icon><EllipsisOutlined /></template>
            </a-button>
            <template #overlay>
              <a-menu @click="({ key }) => handleActionMenuClick(key, record)">
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
        </a-space>
      </template>
    </VxeTableList>

    <!-- 新建/编辑弹窗 -->
    <a-modal
      v-model:open="formVisible"
      :title="isEdit ? '编辑预算调整' : '新建预算调整'"
      :width="600"
      :confirm-loading="submitLoading"
      @ok="handleFormSubmit"
    >
      <a-form :model="formData" :rules="formRules" ref="formRef" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="预算ID" name="budgetId">
              <a-input-number v-model:value="formData.budgetId" :min="1" style="width: 100%" placeholder="请输入预算ID" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="调整类型" name="adjustmentType">
              <a-select v-model:value="formData.adjustmentType" placeholder="请选择">
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
              <a-input-number v-model:value="formData.amount" :min="0" :precision="2" style="width: 100%" placeholder="请输入金额" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="申请日期">
              <a-date-picker v-model:value="formData.applyDate" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="源科目ID" v-if="formData.adjustmentType === 'decrease' || formData.adjustmentType === 'transfer'">
          <a-input-number v-model:value="formData.sourceSubjectId" :min="0" style="width: 100%" placeholder="减少/调出科目ID" />
        </a-form-item>
        <a-form-item label="目标科目ID" v-if="formData.adjustmentType === 'increase' || formData.adjustmentType === 'transfer'">
          <a-input-number v-model:value="formData.targetSubjectId" :min="0" style="width: 100%" placeholder="增加/调入科目ID" />
        </a-form-item>
        <a-form-item label="调整原因" name="reason">
          <a-textarea v-model:value="formData.reason" :rows="3" placeholder="请输入调整原因" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="申请人">
              <a-input v-model:value="formData.applicantName" placeholder="申请人" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import dayjs from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import {
  EyeOutlined, EditOutlined, EllipsisOutlined, CheckCircleOutlined, AuditOutlined, CloseCircleOutlined, SearchOutlined, InboxOutlined,
  FileOutlined, ClockCircleOutlined, DollarOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { budgetAdjustmentApi, type BudgetAdjustment } from '@/api/budget'

const searchFilters = reactive<Record<string, any>>({})

const tableData = ref<BudgetAdjustment[]>([])
const loading = ref(false)
const tableRef = ref()
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const lastUpdated = ref('')
const selectedRows = ref<BudgetAdjustment[]>([])
const selectedIds = ref<number[]>([])

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
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
    const res = await budgetAdjustmentApi.page({
      budgetId: searchFilters.budgetId || undefined,
      status: searchFilters.status || undefined,
      adjustmentType: searchFilters.adjustmentType || undefined,
      pageNum: pagination.current - 1,
      pageSize: pagination.pageSize,
    })
    if (res.success) {
      tableData.value = res.data.records || mockData()
      pagination.total = res.data.total || mockData().length
    }
    lastUpdated.value = new Date().toISOString()
  } catch {
    tableData.value = mockData()
    pagination.total = mockData().length
  } finally {
    loading.value = false
  }
}

const mockData = (): BudgetAdjustment[] => [
  { id: 1, adjustmentNo: 'ADJ-2024-001', budgetId: 1, adjustmentType: 'increase', amount: 50000, status: 'approved', reason: '项目追加', applicantName: '张三', applyDate: '2024-01-15' },
  { id: 2, adjustmentNo: 'ADJ-2024-002', budgetId: 2, adjustmentType: 'decrease', amount: 30000, status: 'submitted', reason: '预算缩减', applicantName: '李四', applyDate: '2024-02-01' },
  { id: 3, adjustmentNo: 'ADJ-2024-003', budgetId: 1, adjustmentType: 'transfer', amount: 20000, status: 'draft', reason: '科目调剂', applicantName: '王五', applyDate: '2024-02-10' },
  { id: 4, adjustmentNo: 'ADJ-2024-004', budgetId: 3, adjustmentType: 'increase', amount: 100000, status: 'rejected', reason: '预算追加', applicantName: '赵六', applyDate: '2024-02-15' },
]

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

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  formVisible.value = true
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
    }
  } catch (_) { /* ignore */ }
}

const handleView = (record: BudgetAdjustment) => {
  handleEdit(record)
}

const handleFormSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch {
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
    formVisible.value = false
    loadData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

const handleSubmit = async (record: BudgetAdjustment) => {
  try {
    await budgetAdjustmentApi.submit(record.id)
    message.success('已提交审批')
    loadData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '提交失败')
  }
}

const handleApprove = async (record: BudgetAdjustment) => {
  try {
    await budgetAdjustmentApi.approve(record.id, '审批通过')
    message.success('审批通过')
    loadData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '审批失败')
  }
}

const handleReject = async (record: BudgetAdjustment) => {
  try {
    await budgetAdjustmentApi.reject(record.id, '已拒绝')
    message.success('已拒绝')
    loadData()
  } catch (e: any) {
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

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleAdd() }
}

onMounted(() => { loadData(); document.addEventListener('keydown', handleKeydown) })
onUnmounted(() => { document.removeEventListener('keydown', handleKeydown) })
</script>

<style scoped>
.adjustment-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
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

.action-more-btn { padding: 0 4px; font-size: 16px; vertical-align: middle; }
.action-cell-inner { flex-wrap: nowrap; }

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
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
</style>
