<template>
  <div class="budget-template-page">
    <TableList
      ref="tableRef"
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      :table-key="'budget-template-list'"
      :filter-fields="filterFields"
      add-text="新建模板"
      @add="handleAdd"
      @edit="handleEdit"
      @delete="handleDelete"
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
        <a-empty v-if="hasActiveFilters" description="当前筛选条件下无匹配模板">
          <template #image><SearchOutlined style="font-size: 48px; color: #faad14" /></template>
          <a-button @click="handleResetFilters">清除筛选</a-button>
        </a-empty>
        <a-empty v-else description="暂无预算模板">
          <template #image><InboxOutlined style="font-size: 48px; color: #d9d9d9" /></template>
          <a-button type="primary" @click="handleAdd">新建模板</a-button>
        </a-empty>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'totalAmount'">
          ¥{{ record.totalAmount?.toFixed(2) ?? '0.00' }}
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
      </template>
    </TableList>

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
          <template v-else-if="column.key === 'sortOrder'">
            <a-input-number v-model:value="record.sortOrder" :min="0" style="width: 60px" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-popconfirm title="确定删除？" @confirm="removeItem(index)">
              <a class="danger">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import dayjs from 'dayjs'
import TableList from '@/components/TableList/TableList.vue'
import { budgetTemplateApi, type BudgetTemplate, type BudgetTemplateItem } from '@/api/budget'
import { PlusOutlined, EyeOutlined, EditOutlined, EllipsisOutlined, DeleteOutlined, SendOutlined, SearchOutlined, InboxOutlined } from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'

const searchFilters = reactive<Record<string, any>>({})

const dataSource = ref<BudgetTemplate[]>([])
const loading = ref(false)
const tableRef = ref()
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const columns = [
  { title: '模板编码', dataIndex: 'templateCode', key: 'templateCode' },
  { title: '模板名称', dataIndex: 'templateName', key: 'templateName' },
  { title: '年度', dataIndex: 'fiscalYear', key: 'fiscalYear', width: 80 },
  { title: '预算总额', dataIndex: 'totalAmount', key: 'totalAmount', align: 'right' as const },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 200 },
]

const filterFields = [
  { key: 'keyword', label: '模板名称', type: 'input' as const, placeholder: '请输入模板名称' },
  { key: 'fiscalYear', label: '年度', type: 'input' as const, placeholder: '年度' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '草稿', value: 'draft' },
    { label: '已发布', value: 'published' },
    { label: '已归档', value: 'archived' },
  ]},
]

const itemColumns = [
  { title: '科目编码', dataIndex: 'subjectCode', key: 'subjectCode' },
  { title: '科目名称', dataIndex: 'subjectName', key: 'subjectName' },
  { title: '预算金额', dataIndex: 'budgetAmount', key: 'budgetAmount' },
  { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder', width: 80 },
  { title: '操作', key: 'action', width: 60 },
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
      dataSource.value = res.data.records || []
      pagination.total = res.data.total || 0
    }
    lastUpdated.value = new Date().toISOString()
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

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

onMounted(() => {
  loadData()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.budget-template-page { padding: 16px; }
.danger { color: #ff4d4f; }
.action-more-btn { padding: 0 4px; font-size: 16px; vertical-align: middle; }
.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help; margin-left: 8px;
  line-height: 32px; vertical-align: middle;
}
.action-cell-inner { flex-wrap: nowrap; }
</style>
