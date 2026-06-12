<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="template-page-header">
        <div class="template-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>预算模板</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="template-page-header-title">预算模板</h2>
        </div>
        <div class="template-page-header-right">
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
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          </span>
        </div>
      </div>
    </template>

    <div class="template-management">
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
        add-text="新建模板"
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
          <a-button size="small" v-permission="'budget:plan:batchpublish'" @click="handleBatchPublish(rows)" :disabled="!canBatchPublish(rows)">
            <template #icon><SendOutlined /></template>
            批量发布
          </a-button>
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
                <a-button type="link" size="small" v-permission="'budget:plan:view'" @click="handleView(record)">
                  <template #icon><EyeOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip v-if="record.status === 'draft'" title="编辑">
                <a-button type="link" size="small" v-permission="'budget:plan:edit'" @click="handleEdit(record)">
                  <template #icon><EditOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-dropdown trigger="click">
                <a-button type="link" size="small" class="action-more-btn">
                  <template #icon><EllipsisOutlined /></template>
                </a-button>
                <template #overlay>
                  <a-menu @click="({ key }) => handleActionMenuClick(key as string, record)">
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

      <!-- 全屏详情抽屉（新建/编辑） -->
      <FullScreenDetail
        :visible="formVisible"
        :title="isEdit ? '编辑模板' : '新建模板'"
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
              <a-form-item label="模板编码" name="templateCode">
                <a-input v-model:value="formData.templateCode" placeholder="自动生成" :disabled="isEdit" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="模板名称" name="templateName">
                <a-input v-model:value="formData.templateName" placeholder="请输入模板名称" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="财政年度" name="fiscalYear">
                <a-input-number v-model:value="formData.fiscalYear" :min="2020" :max="2099" style="width: 100%" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="描述" name="description">
            <a-textarea v-model:value="formData.description" :rows="2" placeholder="请输入模板描述" size="small" />
          </a-form-item>
        </a-form>

        <a-divider>预算科目</a-divider>
        <a-button type="dashed" @click="debounceClick('addItem', addItem)" style="width: 100%; margin-bottom: 12px">
          <template #icon><PlusOutlined /></template>添加科目
        </a-button>
        <VxeTableList
          :data-source="formData.items"
          :columns="itemVxeColumns"
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
          <template #sortOrderCell="{ record }">
            <a-input-number v-model:value="record.sortOrder" :min="0" style="width: 60px" size="small" />
          </template>
          <template #action="{ index }">
            <a-popconfirm title="确定删除？" @confirm="removeItem(index as number)">
              <a class="danger">删除</a>
            </a-popconfirm>
          </template>
        </VxeTableList>
      </FullScreenDetail>
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined, EyeOutlined, EditOutlined, EllipsisOutlined, DeleteOutlined, SendOutlined, SearchOutlined, InboxOutlined,
  FileOutlined, FolderOutlined, DollarOutlined, SyncOutlined, ReloadOutlined, WarningOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'
import { budgetTemplateApi, type BudgetTemplate, type BudgetTemplateItem } from '@/api/budget'

// ── 防抖工具 ──────────────────────────────────────────
const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now()
  const last = debounceMap.get(key) || 0
  if (now - last < delay) return
  debounceMap.set(key, now)
  fn()
}

const searchFilters = reactive<Record<string, any>>({})

const tableData = ref<BudgetTemplate[]>([])
const loading = ref(false)
const tableRef = ref()
const pagination = ref({ current: 1, pageSize: 20, total: 0 })
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
const hasError = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null
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

// ── 汇总行 ──────────────────────────────────────────────
const summaryData = computed(() => {
  if (tableData.value.length === 0) return []
  const total = tableData.value.reduce((s, r) => s + (r.totalAmount || 0), 0)
  return [
    { label: '模板总额', value: `¥${formatAmount(total)}`, type: 'currency' },
  ]
})


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

// ── 表单脏状态跟踪 ──────────────────────────────────────
const initialFormSnapshot = ref<string>('')
function saveFormSnapshot() {
  initialFormSnapshot.value = JSON.stringify({
    templateCode: formData.templateCode,
    templateName: formData.templateName,
    fiscalYear: formData.fiscalYear,
    description: formData.description,
    items: formData.items.map((i: any) => ({
      subjectCode: i.subjectCode,
      subjectName: i.subjectName,
      budgetAmount: i.budgetAmount,
      sortOrder: i.sortOrder,
    })),
  })
}
const formDirty = computed(() => {
  if (!formVisible.value) return false
  const current = JSON.stringify({
    templateCode: formData.templateCode,
    templateName: formData.templateName,
    fiscalYear: formData.fiscalYear,
    description: formData.description,
    items: formData.items.map((i: any) => ({
      subjectCode: i.subjectCode,
      subjectName: i.subjectName,
      budgetAmount: i.budgetAmount,
      sortOrder: i.sortOrder,
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
    hasError.value = false
    const res = await budgetTemplateApi.page({
      keyword: searchFilters.keyword || undefined,
      fiscalYear: searchFilters.fiscalYear || undefined,
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
    console.warn('[预算模板] 加载数据失败')
    tableData.value = []
    pagination.value.total = 0
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

const handleSearch = () => {
  pagination.value.current = 1
  loadData()
}

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.value.current = 1
  loadData()
}

const handleSelectionChange = (rows: BudgetTemplate[], ids: number[]) => {
  selectedRows.value = rows
  selectedIds.value = ids
}

const handlePageChange = (page: number, size: number) => {
  pagination.value.current = page
  pagination.value.pageSize = size
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

function handleParentCreate() { handleAdd() }

const handleAdd = () => {
  resetForm()
  isEdit.value = false
  formVisible.value = true
  nextTick(() => saveFormSnapshot())
}

const handleEdit = async (record: BudgetTemplate) => {
  try {
    const res = await budgetTemplateApi.getById(record.id)
    if (res.code === 200) {
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
      nextTick(() => saveFormSnapshot())
    }
  } catch {
    console.warn('[预算模板] 获取详情失败')
  }
}

const handleView = async (record: BudgetTemplate) => {
  handleEdit(record)
}

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

const handleFormSubmit = async (keepOpen?: boolean) => {
  try {
    await formRef.value?.validate()
  } catch {
    console.warn('[预算模板] 表单验证失败')
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
    if (!keepOpen) formVisible.value = false
    loadData()
  } catch (e: any) {
    console.warn('[预算模板] 操作失败', e)
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
    console.warn('[预算模板] 发布失败', e)
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
        console.warn('[预算模板] 删除失败', e)
        message.error(e?.response?.data?.message || '删除失败')
      }
    }
  })
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined as any })
  pagination.value.current = 1
  loadData()
}

function handleActionMenuClick(key: string, record: BudgetTemplate) {
  switch (key) {
    case 'publish': handlePublish(record); break
    case 'delete': handleDelete(record); break
  }
}

// ── 导出 ────────────────────────────────────────────────
async function handleExport() {
  try {
    const res = await budgetTemplateApi.page({
      keyword: searchFilters.keyword || undefined,
      fiscalYear: searchFilters.fiscalYear || undefined,
      status: searchFilters.status || undefined,
      pageNum: 0,
      pageSize: 9999,
    })
    if (res.success && res.data?.records?.length) {
      const data = res.records
      const csv = data.map((r: any) => `${r.templateCode},${r.templateName},${r.fiscalYear},${r.totalAmount},${r.status}`).join('\n')
      const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url; a.download = `预算模板_${new Date().toISOString().slice(0, 10)}.csv`
      a.click(); URL.revokeObjectURL(url)
    }
    message.success('导出成功')
  } catch {
    message.error('导出失败')
  }
}

// ── 批量操作 ────────────────────────────────────────────
function canBatchPublish(rows: BudgetTemplate[]) {
  return rows.some(r => r.status === 'draft')
}

async function handleBatchPublish(rows: BudgetTemplate[]) {
  const draftIds = rows.filter(r => r.status === 'draft').map(r => r.id)
  if (!draftIds.length) { message.warning('选中的记录中没有可发布的草稿'); return }
  Modal.confirm({
    title: '批量发布',
    content: `确定发布选中的 ${draftIds.length} 项草稿？`,
    onOk: async () => {
      try {
        await Promise.all(draftIds.map(id => budgetTemplateApi.publish(id)))
        message.success(`已发布 ${draftIds.length} 项`)
        loadData()
      } catch { message.error('批量发布失败') }
    },
  })
}

async function handleBatchDelete(ids: number[]) {
  if (!ids.length) { message.warning('请选择要删除的记录'); return }
  Modal.confirm({
    title: '批量删除',
    content: `确定删除 ${ids.length} 项模板？`,
    okType: 'danger',
    onOk: async () => {
      try {
        await Promise.all(ids.map(id => budgetTemplateApi.delete(id)))
        message.success(`已删除 ${ids.length} 项`)
        loadData()
      } catch { message.error('批量删除失败') }
    },
  })
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
}

onMounted(() => {
  loadData()
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
.template-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.template-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.template-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.template-page-header-right {
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

.template-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

.template-management :deep(.vxe-table) {
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

.table-empty-error {
  padding: 48px 0;
}

.table-empty-icon-error {
  color: #faad14;
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

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
.template-management :deep(.ant-input-sm),
.template-management :deep(.ant-input-number-sm),
.template-management :deep(.ant-select-single.ant-select-sm .ant-select-selector),
.template-management :deep(.ant-picker-small),
.template-management :deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
.template-management :deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
.template-management :deep(.ant-input-number-sm input) {
  height: 26px;
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
.template-management :deep(.vxe-table-list-container .vxe-header--row .vxe-header--column) {
  border-bottom: 2px solid #d0d5dd !important;
}

</style>
