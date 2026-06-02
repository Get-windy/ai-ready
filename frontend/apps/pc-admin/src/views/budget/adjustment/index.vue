<template>
  <div class="budget-adjustment-page">
    <a-card title="预算调整管理">
      <div class="search-area">
        <a-form layout="inline" :model="queryParams">
          <a-form-item label="预算ID">
            <a-input-number v-model:value="queryParams.budgetId" :min="0" placeholder="预算ID" style="width: 120px" />
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="queryParams.status" placeholder="请选择" allow-clear style="width: 120px">
              <a-select-option value="draft">草稿</a-select-option>
              <a-select-option value="submitted">待审批</a-select-option>
              <a-select-option value="approved">已通过</a-select-option>
              <a-select-option value="rejected">已拒绝</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="类型">
            <a-select v-model:value="queryParams.adjustmentType" placeholder="请选择" allow-clear style="width: 120px">
              <a-select-option value="increase">增加</a-select-option>
              <a-select-option value="decrease">减少</a-select-option>
              <a-select-option value="transfer">调剂</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleSearch">查询</a-button>
              <a-button @click="handleReset">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <div class="action-area">
        <a-space>
          <a-button type="primary" @click="handleAdd">
            <template #icon><PlusOutlined /></template>
            新建调整
          </a-button>
        </a-space>
      </div>

      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
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
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a v-if="record.status === 'draft'" @click="handleEdit(record)">编辑</a>
              <a v-if="record.status === 'draft'" @click="handleSubmit(record)">提交</a>
              <a v-if="record.status === 'submitted'" @click="handleApprove(record)">通过</a>
              <a v-if="record.status === 'submitted'" @click="handleReject(record)">拒绝</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

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
import { ref, reactive, onMounted } from 'vue'
import { budgetAdjustmentApi, type BudgetAdjustment } from '@/api/budget'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

const queryParams = reactive({
  budgetId: undefined as number | undefined,
  status: undefined as string | undefined,
  adjustmentType: undefined as string | undefined,
  pageNum: 0,
  pageSize: 20,
})

const dataSource = ref<BudgetAdjustment[]>([])
const loading = ref(false)
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })

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
      budgetId: queryParams.budgetId || undefined,
      status: queryParams.status || undefined,
      adjustmentType: queryParams.adjustmentType || undefined,
      pageNum: pagination.current - 1,
      pageSize: pagination.pageSize,
    })
    if (res.success) {
      dataSource.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.current = 1; loadData() }
const handleReset = () => {
  queryParams.budgetId = undefined
  queryParams.status = undefined
  queryParams.adjustmentType = undefined
  handleSearch()
}
const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
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

onMounted(() => { loadData() })
</script>

<style scoped>
.budget-adjustment-page { padding: 16px; }
.search-area { margin-bottom: 16px; }
.action-area { margin-bottom: 16px; }
</style>
