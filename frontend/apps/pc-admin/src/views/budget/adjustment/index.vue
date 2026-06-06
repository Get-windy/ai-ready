<template>
  <div class="budget-adjustment-page">
    <TableList
      ref="tableRef"
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      :table-key="'budget-adjustment-list'"
      :filter-fields="filterFields"
      add-text="新建调整"
      @add="handleAdd"
      @edit="handleEdit"
      @refresh="loadData"
      @search="handleSearch"
      @page-change="handlePageChange"
      @filter-change="handleFilterChange"
    >
      <template #toolbar-actions>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
      </template>

      <template #empty>
        <a-empty v-if="hasActiveFilters" description="当前筛选条件下无匹配调整记录">
          <template #image><SearchOutlined style="font-size: 48px; color: #faad14" /></template>
          <a-button @click="handleResetFilters">清除筛选</a-button>
        </a-empty>
        <a-empty v-else description="暂无预算调整记录">
          <template #image><InboxOutlined style="font-size: 48px; color: #d9d9d9" /></template>
          <a-button type="primary" @click="handleAdd">新建调整</a-button>
        </a-empty>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'adjustmentType'">
          <a-tag :color="record.adjustmentType === 'increase' ? 'green' : record.adjustmentType === 'decrease' ? 'red' : 'blue'">
            {{ record.adjustmentType === 'increase' ? '增加' : record.adjustmentType === 'decrease' ? '减少' : '调剂' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'amount'">
          ¥{{ record.amount?.toFixed(2) ?? '0.00' }}
        </template>
        <template v-else-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
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
      </template>
    </TableList>

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
import TableList from '@/components/TableList/TableList.vue'
import { budgetAdjustmentApi, type BudgetAdjustment } from '@/api/budget'
import { PlusOutlined, EyeOutlined, EditOutlined, EllipsisOutlined, CheckCircleOutlined, AuditOutlined, CloseCircleOutlined, SearchOutlined, InboxOutlined } from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'

const searchFilters = reactive<Record<string, any>>({})

const dataSource = ref<BudgetAdjustment[]>([])
const loading = ref(false)
const tableRef = ref()
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const columns = [
  { title: '调整单号', dataIndex: 'adjustmentNo', key: 'adjustmentNo' },
  { title: '预算ID', dataIndex: 'budgetId', key: 'budgetId', width: 80 },
  { title: '类型', dataIndex: 'adjustmentType', key: 'adjustmentType', width: 70 },
  { title: '金额', dataIndex: 'amount', key: 'amount', align: 'right' as const },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '原因', dataIndex: 'reason', key: 'reason', ellipsis: true },
  { title: '申请人', dataIndex: 'applicantName', key: 'applicantName', width: 100 },
  { title: '申请日期', dataIndex: 'applyDate', key: 'applyDate', width: 110 },
  { title: '操作', key: 'action', width: 200 },
]

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
      dataSource.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
    lastUpdated.value = new Date().toISOString()
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.current = 1; loadData() }

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  loadData()
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

onMounted(() => {
  loadData()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.budget-adjustment-page { padding: 16px; }
.action-more-btn { padding: 0 4px; font-size: 16px; vertical-align: middle; }
.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}
.action-cell-inner { flex-wrap: nowrap; }
</style>
