<template>
  <div class="budget-template-page">
    <a-card title="预算模板管理">
      <div class="search-area">
        <a-form layout="inline" :model="queryParams">
          <a-form-item label="模板名称">
            <a-input v-model:value="queryParams.keyword" placeholder="请输入模板名称" allow-clear style="width: 180px" />
          </a-form-item>
          <a-form-item label="年度">
            <a-input-number v-model:value="queryParams.fiscalYear" :min="2020" :max="2099" placeholder="年度" style="width: 120px" />
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="queryParams.status" placeholder="请选择" allow-clear style="width: 120px">
              <a-select-option value="draft">草稿</a-select-option>
              <a-select-option value="published">已发布</a-select-option>
              <a-select-option value="archived">已归档</a-select-option>
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
            新建模板
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
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'totalAmount'">
            ¥{{ record.totalAmount?.toFixed(2) ?? '0.00' }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleView(record)">查看</a>
              <a v-if="record.status === 'draft'" @click="handleEdit(record)">编辑</a>
              <a v-if="record.status === 'draft'" @click="handlePublish(record)">发布</a>
              <a-popconfirm v-if="record.status === 'draft'" title="确定删除此模板？" @confirm="handleDelete(record)">
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
import { ref, reactive, onMounted } from 'vue'
import { budgetTemplateApi, type BudgetTemplate, type BudgetTemplateItem } from '@/api/budget'
import { PlusOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

const queryParams = reactive({
  keyword: '',
  fiscalYear: undefined as number | undefined,
  status: undefined as string | undefined,
  pageNum: 0,
  pageSize: 20,
})

const dataSource = ref<BudgetTemplate[]>([])
const loading = ref(false)
const pagination = reactive({ current: 1, pageSize: 20, total: 0, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` })

const columns = [
  { title: '模板编码', dataIndex: 'templateCode', key: 'templateCode' },
  { title: '模板名称', dataIndex: 'templateName', key: 'templateName' },
  { title: '年度', dataIndex: 'fiscalYear', key: 'fiscalYear', width: 80 },
  { title: '预算总额', dataIndex: 'totalAmount', key: 'totalAmount', align: 'right' as const },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 170 },
  { title: '操作', key: 'action', width: 200 },
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
      keyword: queryParams.keyword || undefined,
      fiscalYear: queryParams.fiscalYear || undefined,
      status: queryParams.status || undefined,
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

const handleSearch = () => {
  pagination.current = 1
  loadData()
}

const handleReset = () => {
  queryParams.keyword = ''
  queryParams.fiscalYear = undefined
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
  try {
    await budgetTemplateApi.delete(record.id)
    message.success('删除成功')
    loadData()
  } catch (e: any) {
    message.error(e?.response?.data?.message || '删除失败')
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.budget-template-page { padding: 16px; }
.search-area { margin-bottom: 16px; }
.action-area { margin-bottom: 16px; }
.danger { color: #ff4d4f; }
</style>
