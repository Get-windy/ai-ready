<template>
  <div class="template-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-draft">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ draftCount }}</div>
          <div class="stat-card-label">草稿</div>
        </div>
        <FileOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-published">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ publishedCount }}</div>
          <div class="stat-card-label">已发布</div>
        </div>
        <SendOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-archived">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ archivedCount }}</div>
          <div class="stat-card-label">已归档</div>
        </div>
        <FolderOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-amount">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(totalAmount) }}</div>
          <div class="stat-card-label">模板总额</div>
        </div>
        <DollarOutlined class="stat-card-icon" />
      </div>
    </div>

    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableData"
      :loading="loading"
      :pagination="pagination"
      :row-key="'id'"
      :filter-fields="filterFields"
      :selectable="true"
      add-text="新建模板"
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
            没有符合条件的模板，<a @click="handleResetFilters">清除筛选</a>
          </p>
          <p v-else class="table-empty-text">
            暂无预算模板，点击「新建模板」开始创建
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
                  <a-menu-item v-if="record.status === 'draft'" key="publish">
                    <SendOutlined /> 发布
                  </a-menu-item>
                  <a-menu-divider v-if="record.status === 'draft'" />
                  <a-menu-item v-if="record.status === 'draft'" key="delete" danger>
                    <DeleteOutlined /> 删除
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
      :title="isEdit ? '编辑模板' : '新建模板'"
      :width="900"
      :confirm-loading="submitLoading"
      @ok="handleSubmit"
    >
      <a-form :model="formData" :rules="formRules" ref="formRef" layout="vertical">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="模板编码" name="templateCode">
              <a-input v-model:value="formData.templateCode" placeholder="自动生成" :disabled="isEdit" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="模板名称" name="templateName">
              <a-input v-model:value="formData.templateName" placeholder="请输入模板名称" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="财政年度" name="fiscalYear">
              <a-input-number v-model:value="formData.fiscalYear" :min="2020" :max="2099" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formData.description" :rows="2" placeholder="请输入模板描述" />
        </a-form-item>
      </a-form>

      <a-divider>预算科目</a-divider>
      <a-button type="dashed" @click="addItem" style="width: 100%; margin-bottom: 12px">
        <template #icon><PlusOutlined /></template>添加科目
      </a-button>
      <VxeTableList
        :data-source="formData.items"
        :columns="itemVxeColumns"
        :pagination="false"
        row-key="rowKey"
        :show-toolbar="false"
        :selectable="false"
        :show-add="false"
        :show-search="false"
        :show-export="false"
        :show-batch-delete="false"
      >
        <template #subjectCodeCell="{ record }">
          <a-input v-model:value="record.subjectCode" placeholder="科目编码" />
        </template>
        <template #subjectNameCell="{ record }">
          <a-input v-model:value="record.subjectName" placeholder="科目名称" />
        </template>
        <template #budgetAmountCell="{ record }">
          <a-input-number v-model:value="record.budgetAmount" :min="0" :precision="2" style="width: 100%" />
        </template>
        <template #sortOrderCell="{ record }">
          <a-input-number v-model:value="record.sortOrder" :min="0" style="width: 60px" />
        </template>
        <template #action="{ index }">
          <a-popconfirm title="确定删除？" @confirm="removeItem(index)">
            <a class="danger">删除</a>
          </a-popconfirm>
        </template>
      </VxeTableList>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import dayjs from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined, EyeOutlined, EditOutlined, EllipsisOutlined, DeleteOutlined, SendOutlined, SearchOutlined, InboxOutlined,
  FileOutlined, FolderOutlined, DollarOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { budgetTemplateApi, type BudgetTemplate, type BudgetTemplateItem } from '@/api/budget'

const searchFilters = reactive<Record<string, any>>({})

const tableData = ref<BudgetTemplate[]>([])
const loading = ref(false)
const tableRef = ref()
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const lastUpdated = ref('')
const selectedRows = ref<BudgetTemplate[]>([])
const selectedIds = ref<number[]>([])

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

// ── 统计数据 ────────────────────────────────────────────
const draftCount = computed(() => tableData.value.filter(r => r.status === 'draft').length)
const publishedCount = computed(() => tableData.value.filter(r => r.status === 'published').length)
const archivedCount = computed(() => tableData.value.filter(r => r.status === 'archived').length)
const totalAmount = computed(() => tableData.value.reduce((s, r) => s + (r.totalAmount || 0), 0))


function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const vxeColumns = computed(() => [
  { field: 'templateCode', title: '模板编码', width: 150 },
  { field: 'templateName', title: '模板名称', width: 180 },
  { field: 'fiscalYear', title: '年度', width: 80 },
  { field: 'totalAmount', title: '预算总额', width: 130, align: 'right', formatter: ({ cellValue }) => `¥${formatAmount(cellValue)}` },
  { field: 'status', title: '状态', width: 80, align: 'center', formatter: ({ cellValue }) => statusText(cellValue) },
  { field: 'createdAt', title: '创建时间', width: 170 },
  { type: 'action', title: '操作', width: 140, fixed: 'right' },
])

const filterFields = [
  { key: 'keyword', label: '模板名称', type: 'input' as const, placeholder: '请输入模板名称' },
  { key: 'fiscalYear', label: '年度', type: 'input' as const, placeholder: '年度' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 'draft' },
    { label: '已发布', value: 'published' },
    { label: '已归档', value: 'archived' },
  ]},
]

const itemVxeColumns = [
  { field: 'subjectCode', title: '科目编码', slotName: 'subjectCodeCell' },
  { field: 'subjectName', title: '科目名称', slotName: 'subjectNameCell' },
  { field: 'budgetAmount', title: '预算金额', slotName: 'budgetAmountCell' },
  { field: 'sortOrder', title: '排序', width: 80, slotName: 'sortOrderCell' },
  { field: 'action', title: '操作', width: 60, type: 'action' }
]

const formVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const editId = ref<number | null>(null)

const formData = reactive<BudgetTemplate>({
  id: 0,
  templateCode: '',
  templateName: '',
  fiscalYear: new Date().getFullYear(),
  totalAmount: 0,
  status: 'draft',
  description: '',
  createdBy: '',
  createdAt: '',
  updatedBy: '',
  updatedAt: '',
  items: [],
})

const formRules = {
  templateName: [{ required: true, message: '请输入模板名称' }],
  fiscalYear: [{ required: true, message: '请选择年度', type: 'number' as const }],
}

const statusColor = (s: string) => {
  const map: Record<string, string> = { draft: 'default', published: 'blue', archived: 'orange' }
  return map[s] || 'default'
}
const statusText = (s: string) => {
  const map: Record<string, string> = { draft: '草稿', published: '已发布', archived: '已归档' }
  return map[s] || s
}

let rowKeyCounter = 0
const genRowKey = () => `row_${++rowKeyCounter}`

const addItem = () => {
  formData.items.push({
    id: 0,
    templateId: 0,
    subjectCode: '',
    subjectName: '',
    budgetAmount: 0,
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
    const res = await budgetTemplateApi.page({
      keyword: searchFilters.keyword || undefined,
      fiscalYear: searchFilters.fiscalYear || undefined,
      status: searchFilters.status || undefined,
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

const mockData = (): BudgetTemplate[] => [
  { id: 1, templateCode: 'TPL-001', templateName: '标准预算模板', fiscalYear: 2024, totalAmount: 1000000, status: 'published', createdAt: '2024-01-01 10:00' },
  { id: 2, templateCode: 'TPL-002', templateName: '部门预算模板', fiscalYear: 2024, totalAmount: 500000, status: 'draft', createdAt: '2024-01-15 14:00' },
  { id: 3, templateCode: 'TPL-003', templateName: '项目预算模板', fiscalYear: 2023, totalAmount: 200000, status: 'archived', createdAt: '2023-12-01 09:00' },
  { id: 4, templateCode: 'TPL-004', templateName: '年度预算模板', fiscalYear: 2024, totalAmount: 800000, status: 'published', createdAt: '2024-02-01 16:00' },
]

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  loadData()
}

const handleSelectionChange = (rows: BudgetTemplate[], ids: number[]) => {
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
  formData.templateCode = ''
  formData.templateName = ''
  formData.fiscalYear = new Date().getFullYear()
  formData.totalAmount = 0
  formData.status = 'draft'
  formData.description = ''
  formData.items = []
  editId.value = null
}

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  formVisible.value = true
}

const handleEdit = async (record: BudgetTemplate) => {
  try {
    const res = await budgetTemplateApi.getById(record.id)
    if (res.success) {
      const d = res.data
      formData.id = d.id
      formData.templateCode = d.templateCode
      formData.templateName = d.templateName
      formData.fiscalYear = d.fiscalYear
      formData.totalAmount = d.totalAmount
      formData.description = d.description || ''
      formData.items = (d.items || []).map((item: any) => ({ ...item, rowKey: genRowKey() }))
      editId.value = d.id
      isEdit.value = true
      formVisible.value = true
    }
  } catch (_) { /* ignore */ }
}

const handleView = async (record: BudgetTemplate) => {
  handleEdit(record)
}

const handleSubmit = async () => {
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
      await budgetTemplateApi.update(editId.value, { ...formData })
      message.success('更新成功')
    } else {
      await budgetTemplateApi.create({ ...formData })
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

const handlePublish = async (record: BudgetTemplate) => {
  try {
    await budgetTemplateApi.publish(record.id)
    message.success('发布成功')
    loadData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '发布失败')
  }
}

const handleDelete = async (record: BudgetTemplate) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除模板"${record.templateName}"吗？删除后数据不可恢复。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      try {
        await budgetTemplateApi.delete(record.id)
        message.success('删除成功')
        loadData()
      } catch (e: any) {
        message.error(e?.response?.data?.message || '删除失败')
      }
    }
  })
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.current = 1
  loadData()
}

function handleActionMenuClick(key: string, record: BudgetTemplate) {
  switch (key) {
    case 'publish': handlePublish(record); break
    case 'delete': handleDelete(record); break
  }
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleAdd() }
}

onMounted(() => { loadData(); document.addEventListener('keydown', handleKeydown) })
onUnmounted(() => { document.removeEventListener('keydown', handleKeydown) })
</script>

<style scoped>
.template-page {
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
.stat-published { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-archived { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
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

.danger { color: #ff4d4f; }
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
