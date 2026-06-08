<template>
  <div class="annual-budget-page">
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

    <a-card title="年度预算管理" class="table-card">
      <div class="search-area">
        <a-form layout="inline" :model="queryParams">
          <a-form-item label="关键词">
            <a-input v-model:value="queryParams.keyword" placeholder="预算单号/部门" allow-clear style="width: 180px" />
          </a-form-item>
          <a-form-item label="年度">
            <a-input-number v-model:value="queryParams.fiscalYear" :min="2020" :max="2099" placeholder="年度" style="width: 120px" />
          </a-form-item>
          <a-form-item label="部门">
            <a-input v-model:value="queryParams.departmentId" placeholder="部门ID" allow-clear style="width: 120px" />
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="queryParams.status" placeholder="请选择" allow-clear style="width: 130px">
              <a-select-option value="draft">草稿</a-select-option>
              <a-select-option value="submitted">待审批</a-select-option>
              <a-select-option value="approved">已审批</a-select-option>
              <a-select-option value="rejected">已拒绝</a-select-option>
              <a-select-option value="executing">执行中</a-select-option>
              <a-select-option value="closed">已关闭</a-select-option>
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
            新建预算
          </a-button>
          <a-button @click="handleCreateFromTemplate">
            <template #icon><CopyOutlined /></template>
            从模板创建
          </a-button>
        </a-space>
      </div>

      <a-table
        :columns="columns"
        :data-source="tableDataSource"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="record.__empty_row">
            <span class="empty-placeholder">&nbsp;</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'totalAmount'">
            <span class="amount-cell">¥{{ formatAmount(record.totalAmount) }}</span>
          </template>
          <template v-else-if="column.key === 'totalUsedAmount'">
            <span class="amount-cell used">¥{{ formatAmount(record.totalUsedAmount) }}</span>
          </template>
          <template v-else-if="column.key === 'executionRate'">
            <span class="rate-cell">{{ record.executionRate?.toFixed(2) ?? '0.00' }}%</span>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a v-if="record.status === 'draft'" @click="handleEdit(record)">编辑</a>
              <a v-if="record.status === 'draft'" @click="handleSubmit(record)">提交</a>
              <a v-if="record.status === 'submitted'" @click="handleApprove(record)">通过</a>
              <a v-if="record.status === 'submitted'" @click="handleReject(record)">拒绝</a>
              <a v-if="record.status === 'approved'" @click="handleStartExec(record)">执行</a>
              <a v-if="record.status === 'executing'" @click="handleClose(record)">关闭</a>
              <a-popconfirm v-if="record.status === 'draft'" title="确定删除此预算？" @confirm="handleDelete(record)">
                <a class="danger">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新建/编辑弹窗 -->
    <a-modal
      v-model:open="formVisible"
      :title="isEdit ? '编辑年度预算' : '新建年度预算'"
      :width="1000"
      :confirm-loading="submitLoading"
      @ok="handleFormSubmit"
    >
      <a-form :model="formData" :rules="formRules" ref="formRef" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="预算单号" name="budgetNo">
              <a-input v-model:value="formData.budgetNo" placeholder="自动生成" :disabled="isEdit" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="年度" name="fiscalYear">
              <a-input-number v-model:value="formData.fiscalYear" :min="2020" :max="2099" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="部门" name="departmentName">
              <a-input v-model:value="formData.departmentName" placeholder="部门名称" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="部门ID" name="departmentId">
              <a-input v-model:value="formData.departmentId" placeholder="部门ID" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="模板名称" name="templateName">
              <a-input v-model:value="formData.templateName" placeholder="关联模板" :disabled="!!formData.templateId" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="备注" name="remark">
              <a-input v-model:value="formData.remark" placeholder="备注" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formData.description" :rows="2" placeholder="描述" />
        </a-form-item>
      </a-form>

      <a-divider>预算科目</a-divider>
      <a-button type="dashed" @click="addItem" style="width: 100%; margin-bottom: 12px">
        <template #icon><PlusOutlined /></template>添加科目
      </a-button>
      <a-table
        :data-source="formData.items"
        :columns="itemColumns"
        :pagination="false"
        row-key="rowKey"
        size="small"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'subjectCode'">
            <a-input v-model:value="record.subjectCode" placeholder="科目编码" />
          </template>
          <template v-else-if="column.key === 'subjectName'">
            <a-input v-model:value="record.subjectName" placeholder="科目名称" />
          </template>
          <template v-else-if="column.key === 'budgetAmount'">
            <a-input-number v-model:value="record.budgetAmount" :min="0" :precision="2" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-popconfirm title="确定删除？" @confirm="removeItem(index)">
              <a class="danger">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-modal>

    <!-- 从模板创建 -->
    <a-modal
      v-model:open="templateModalVisible"
      title="从模板创建年度预算"
      :width="600"
      @ok="handleTemplateSelectOk"
      :confirm-loading="templateLoading"
    >
      <a-table
        :data-source="templateList"
        :columns="templateColumns"
        :pagination="false"
        :loading="templateLoading"
        row-key="id"
        :row-selection="{ selectedRowKeys: templateSelectedKeys, onChange: (keys: any[]) => { templateSelectedKeys = keys } }"
        size="small"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined, CopyOutlined, FileOutlined, ClockCircleOutlined,
  PlayCircleOutlined, DollarOutlined
} from '@ant-design/icons-vue'
import { annualBudgetApi, budgetTemplateApi, type AnnualBudget, type BudgetItem } from '@/api/budget'

const queryParams = reactive({
  keyword: '',
  fiscalYear: undefined as number | undefined,
  departmentId: undefined as string | undefined,
  status: undefined as string | undefined,
  pageNum: 0,
  pageSize: 20,
})

const tableData = ref<AnnualBudget[]>([])
const loading = ref(false)
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })

// ── 统计数据 ────────────────────────────────────────────
const draftCount = computed(() => tableData.value.filter(r => r.status === 'draft').length)
const pendingCount = computed(() => tableData.value.filter(r => r.status === 'submitted').length)
const executingCount = computed(() => tableData.value.filter(r => r.status === 'executing').length)
const totalBudget = computed(() => tableData.value.reduce((s, r) => s + (r.totalAmount || 0), 0))

// ── 空行填充 ────────────────────────────────────────────
const MIN_TABLE_ROWS = 20
const tableDataSource = computed(() => {
  const data = [...tableData.value]
  const emptyCount = Math.max(0, MIN_TABLE_ROWS - data.length)
  for (let i = 0; i < emptyCount; i++) {
    data.push({ __empty_row: true, id: `__empty_${i}` })
  }
  return data
})

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const columns = [
  { title: '预算单号', dataIndex: 'budgetNo', key: 'budgetNo', width: 150 },
  { title: '年度', dataIndex: 'fiscalYear', key: 'fiscalYear', width: 70 },
  { title: '部门', dataIndex: 'departmentName', key: 'departmentName', width: 120 },
  { title: '预算总额', key: 'totalAmount', width: 130, align: 'right' as const },
  { title: '已使用', key: 'totalUsedAmount', width: 130, align: 'right' as const },
  { title: '执行率', key: 'executionRate', width: 80, align: 'right' as const },
  { title: '状态', key: 'status', width: 80, align: 'center' },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 280 },
]

const itemColumns = [
  { title: '科目编码', dataIndex: 'subjectCode', key: 'subjectCode' },
  { title: '科目名称', dataIndex: 'subjectName', key: 'subjectName' },
  { title: '预算金额', dataIndex: 'budgetAmount', key: 'budgetAmount' },
  { title: '操作', key: 'action', width: 60 },
]

const statusColor = (s: string) => {
  const map: Record<string, string> = { draft: 'default', submitted: 'blue', approved: 'cyan', rejected: 'red', executing: 'green', closed: 'orange' }
  return map[s] || 'default'
}
const statusText = (s: string) => {
  const map: Record<string, string> = { draft: '草稿', submitted: '待审批', approved: '已审批', rejected: '已拒绝', executing: '执行中', closed: '已关闭' }
  return map[s] || s
}

// Form
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

const loadData = async () => {
  loading.value = true
  try {
    const res = await annualBudgetApi.page({
      keyword: queryParams.keyword || undefined,
      fiscalYear: queryParams.fiscalYear || undefined,
      departmentId: queryParams.departmentId || undefined,
      status: queryParams.status || undefined,
      pageNum: pagination.current - 1,
      pageSize: pagination.pageSize,
    })
    if (res.success) {
      tableData.value = res.data.records || mockData()
      pagination.total = res.data.total || mockData().length
    }
  } catch {
    tableData.value = mockData()
    pagination.total = mockData().length
  } finally {
    loading.value = false
  }
}

const mockData = (): AnnualBudget[] => [
  { id: 1, budgetNo: 'BUD-2024-001', fiscalYear: 2024, departmentName: '财务部', totalAmount: 500000, totalUsedAmount: 200000, executionRate: 40, status: 'executing', createdAt: '2024-01-01 10:00' },
  { id: 2, budgetNo: 'BUD-2024-002', fiscalYear: 2024, departmentName: '市场部', totalAmount: 300000, totalUsedAmount: 50000, executionRate: 16.67, status: 'submitted', createdAt: '2024-01-15 14:00' },
  { id: 3, budgetNo: 'BUD-2024-003', fiscalYear: 2024, departmentName: '研发部', totalAmount: 800000, totalUsedAmount: 0, executionRate: 0, status: 'draft', createdAt: '2024-02-01 09:00' },
  { id: 4, budgetNo: 'BUD-2024-004', fiscalYear: 2024, departmentName: '人事部', totalAmount: 200000, totalUsedAmount: 200000, executionRate: 100, status: 'closed', createdAt: '2024-02-10 16:00' },
]

const handleSearch = () => { pagination.current = 1; loadData() }
const handleReset = () => {
  queryParams.keyword = ''
  queryParams.fiscalYear = undefined
  queryParams.departmentId = undefined
  queryParams.status = undefined
  handleSearch()
}
const handleTableChange = (pag: any) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

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

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  formVisible.value = true
}

const handleEdit = async (record: AnnualBudget) => {
  try {
    const res = await annualBudgetApi.getById(record.id)
    if (res.success) {
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
    }
  } catch (_) { /* ignore */ }
}

const handleView = (record: AnnualBudget) => {
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
    const total = formData.items.reduce((sum, item) => sum + (item.budgetAmount || 0), 0)
    formData.totalAmount = total

    if (isEdit.value && editId.value) {
      await annualBudgetApi.update(editId.value, { ...formData })
      message.success('更新成功')
    } else {
      await annualBudgetApi.create({ ...formData })
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

const handleSubmit = async (record: AnnualBudget) => {
  try {
    await annualBudgetApi.submit(record.id)
    message.success('已提交审批')
    loadData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '提交失败')
  }
}

const handleApprove = async (record: AnnualBudget) => {
  try {
    await annualBudgetApi.approve(record.id)
    message.success('审批通过')
    loadData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '审批失败')
  }
}

const handleReject = async (record: AnnualBudget) => {
  try {
    await annualBudgetApi.reject(record.id)
    message.success('已拒绝')
    loadData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '拒绝失败')
  }
}

const handleStartExec = async (record: AnnualBudget) => {
  message.info('请使用接口将状态推进到执行中')
}

const handleClose = async (record: AnnualBudget) => {
  try {
    await annualBudgetApi.close(record.id)
    message.success('已关闭')
    loadData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '关闭失败')
  }
}

const handleDelete = async (record: AnnualBudget) => {
  try {
    await annualBudgetApi.delete(record.id)
    message.success('删除成功')
    loadData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '删除失败')
  }
}

// Template selection
const templateModalVisible = ref(false)
const templateList = ref<any[]>([])
const templateLoading = ref(false)
const templateSelectedKeys = ref<number[]>([])

const templateColumns = [
  { title: '模板编码', dataIndex: 'templateCode', key: 'templateCode' },
  { title: '模板名称', dataIndex: 'templateName', key: 'templateName' },
  { title: '年度', dataIndex: 'fiscalYear', key: 'fiscalYear', width: 70 },
  { title: '金额', dataIndex: 'totalAmount', key: 'totalAmount', align: 'right' as const },
]

const handleCreateFromTemplate = async () => {
  templateLoading.value = true
  templateSelectedKeys.value = []
  try {
    const res = await budgetTemplateApi.listByYear(new Date().getFullYear())
    if (res.success) {
      templateList.value = res.data || mockTemplateData()
    }
    templateModalVisible.value = true
  } catch {
    templateList.value = mockTemplateData()
    templateModalVisible.value = true
  } finally {
    templateLoading.value = false
  }
}

const mockTemplateData = (): any[] => [
  { id: 1, templateCode: 'TPL-001', templateName: '标准预算模板', fiscalYear: 2024, totalAmount: 1000000 },
  { id: 2, templateCode: 'TPL-002', templateName: '部门预算模板', fiscalYear: 2024, totalAmount: 500000 },
]

const handleTemplateSelectOk = async () => {
  if (templateSelectedKeys.value.length === 0) {
    message.warning('请选择一个模板')
    return
  }
  const templateId = templateSelectedKeys.value[0]
  templateModalVisible.value = false
  try {
    const res = await budgetTemplateApi.getById(templateId)
    if (res.success) {
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
    }
  } catch (e: any) {
    message.error(e?.response?.data?.message || '获取模板失败')
  }
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleAdd() }
}

onMounted(() => { loadData(); document.addEventListener('keydown', handleKeydown) })
onUnmounted(() => { document.removeEventListener('keydown', handleKeydown) })
</script>

<style scoped>
.annual-budget-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
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

.table-card {
  flex: 1;
  border-radius: 8px;
}

.search-area { margin-bottom: 16px; }
.action-area { margin-bottom: 16px; }
.danger { color: #ff4d4f; }

.empty-placeholder { color: transparent; }

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

.amount-cell.used {
  color: #faad14;
}

.rate-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
}

/* 表格网格边框 */
:deep(.ant-table-thead > tr > th) {
  border-top: 1px solid #d9d9d9 !important;
  border-right: 1px solid #d9d9d9 !important;
  border-bottom: 2px solid #b0b0b0 !important;
  background: #fafafa !important;
  padding: 8px 12px !important;
  font-weight: 600 !important;
}

:deep(.ant-table-thead > tr > th:first-child) {
  border-left: 1px solid #d9d9d9 !important;
}

:deep(.ant-table-tbody > tr > td) {
  border-right: 1px solid #e0e0e0 !important;
  border-bottom: 1px solid #e8e8e8 !important;
  padding: 8px 12px !important;
}

:deep(.ant-table-tbody > tr > td:first-child) {
  border-left: 1px solid #e0e0e0 !important;
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