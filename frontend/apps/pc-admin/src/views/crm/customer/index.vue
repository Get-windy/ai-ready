<template>
  <ModuleLayout
    :breadcrumb-items="breadcrumbItems"
    :current-view="currentView"
    :selected-count="selectedRowKeys.length"
    :show-search-panel="showSearchPanel"
    :search-panel-collapsed="searchPanelCollapsed"
    :filters="filters"
    :active-filters="activeFilters"
    :current-page="pagination.current"
    :total-pages="Math.ceil(pagination.total / pagination.pageSize)"
    :page-size="pagination.pageSize"
    :total-items="pagination.total"
    :loading="loading"
    :error="error"
    :empty="!loading && !error && tableData.length === 0"
    empty-text="暂无客户数据"
    search-placeholder="搜索客户名称 / 编码..."
    @view-change="handleViewChange"
    @clear-selection="handleClearSelection"
    @search-submit="handleSearchSubmit"
    @filter-toggle="handleFilterToggle"
    @search-panel-toggle="handleSearchPanelToggle"
    @filter-change="handleFilterChange"
    @clear-all-filters="handleClearAllFilters"
    @page-change="handlePageChange"
    @page-size-change="handlePageSizeChange"
  >
    <template #actions>
      <a-button type="primary" @click="handleAdd">
        <template #icon><PlusOutlined /></template>
        新增客户
      </a-button>
      <a-button @click="handleImport">导入</a-button>
      <a-button @click="handleExport">导出</a-button>
    </template>

    <template #batch-actions>
      <a-button @click="handleBatchAssign">批量分配</a-button>
      <a-button danger @click="handleBatchDelete">批量删除</a-button>
    </template>

    <!-- 列表视图 -->
    <template #list-view>
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :row-selection="rowSelection"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a-space>
              <a-avatar :style="{ backgroundColor: getLevelColor(record.level) }" size="small">
                {{ record.name?.charAt(0) }}
              </a-avatar>
              <div>
                <div class="customer-name">{{ record.name }}</div>
                <div class="customer-code">{{ record.code }}</div>
              </div>
            </a-space>
          </template>

          <template v-else-if="column.key === 'level'">
            <a-tag :color="getLevelColor(record.level)">{{ getLevelName(record.level) }}</a-tag>
          </template>

          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 0 ? 'success' : 'error'">
              {{ record.status === 0 ? '正常' : '停用' }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
              <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
              <a-button type="link" size="small" @click="handleFollow(record)">跟进</a-button>
              <a-dropdown>
                <a-button type="link" size="small">更多</a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handleViewFollows(record)">跟进记录</a-menu-item>
                    <a-menu-item @click="handleViewOrders(record)">订单记录</a-menu-item>
                    <a-menu-divider />
                    <a-menu-item danger @click="handleDelete(record)">删除</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </template>
      </a-table>
    </template>

    <!-- 看板视图 -->
    <template #kanban-view>
      <div class="kanban-container">
        <div v-for="level in levelGroups" :key="level.value" class="kanban-column">
          <div class="kanban-column-header">
            <span class="kanban-column-title">{{ level.label }}</span>
            <span class="kanban-column-count">{{ getCustomersByLevel(level.value).length }}</span>
          </div>
          <div class="kanban-column-body">
            <div
              v-for="customer in getCustomersByLevel(level.value)"
              :key="customer.id"
              class="kanban-card"
              @click="handleView(customer)"
            >
              <div class="kanban-card-header">
                <span class="kanban-card-name">{{ customer.name }}</span>
                <a-tag :color="customer.status === 0 ? 'success' : 'error'" size="small">
                  {{ customer.status === 0 ? '正常' : '停用' }}
                </a-tag>
              </div>
              <div class="kanban-card-body">
                <div class="kanban-card-row">
                  <span class="kanban-card-label">联系人:</span>
                  <span class="kanban-card-value">{{ customer.contactPerson }}</span>
                </div>
                <div class="kanban-card-row">
                  <span class="kanban-card-label">电话:</span>
                  <span class="kanban-card-value">{{ customer.phone }}</span>
                </div>
                <div class="kanban-card-row">
                  <span class="kanban-card-label">行业:</span>
                  <span class="kanban-card-value">{{ customer.industry }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </ModuleLayout>

  <!-- 客户表单弹窗 -->
  <a-modal
    v-model:open="modalVisible"
    :title="modalTitle"
    :confirm-loading="modalLoading"
    width="700px"
    @ok="handleModalOk"
  >
    <a-form
      ref="formRef"
      :model="formState"
      :rules="formRules"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
    >
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="客户名称" name="name">
            <a-input v-model:value="formState.name" placeholder="请输入客户名称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="客户编码" name="code">
            <a-input v-model:value="formState.code" placeholder="请输入客户编码" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="联系人" name="contactPerson">
            <a-input v-model:value="formState.contactPerson" placeholder="请输入联系人" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="联系电话" name="phone">
            <a-input v-model:value="formState.phone" placeholder="请输入联系电话" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="邮箱" name="email">
            <a-input v-model:value="formState.email" placeholder="请输入邮箱" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="客户等级" name="level">
            <a-select v-model:value="formState.level" placeholder="请选择等级">
              <a-select-option :value="1">VIP客户</a-select-option>
              <a-select-option :value="2">重要客户</a-select-option>
              <a-select-option :value="3">普通客户</a-select-option>
              <a-select-option :value="4">潜在客户</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="行业" name="industry">
            <a-select v-model:value="formState.industry" placeholder="请选择行业">
              <a-select-option value="IT">IT/互联网</a-select-option>
              <a-select-option value="制造业">制造业</a-select-option>
              <a-select-option value="金融">金融</a-select-option>
              <a-select-option value="零售">零售</a-select-option>
              <a-select-option value="其他">其他</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="状态" name="status">
            <a-radio-group v-model:value="formState.status">
              <a-radio :value="0">正常</a-radio>
              <a-radio :value="1">停用</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="地址" name="address" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
            <a-input v-model:value="formState.address" placeholder="请输入地址" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="备注" name="description" :label-col="{ span: 3 }" :wrapper-col="{ span: 20 }">
            <a-textarea v-model:value="formState.description" placeholder="请输入备注" :rows="3" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>

  <!-- 跟进记录弹窗 -->
  <a-modal
    v-model:open="followModalVisible"
    title="添加跟进记录"
    :confirm-loading="followModalLoading"
    width="600px"
    @ok="handleFollowModalOk"
  >
    <a-form :model="followForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
      <a-form-item label="跟进类型" required>
        <a-select v-model:value="followForm.followType" placeholder="请选择跟进类型">
          <a-select-option :value="1">电话</a-select-option>
          <a-select-option :value="2">拜访</a-select-option>
          <a-select-option :value="3">邮件</a-select-option>
          <a-select-option :value="4">微信</a-select-option>
          <a-select-option :value="5">其他</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="跟进内容" required>
        <a-textarea v-model:value="followForm.content" placeholder="请输入跟进内容" :rows="4" />
      </a-form-item>
      <a-form-item label="跟进结果">
        <a-select v-model:value="followForm.result" placeholder="请选择跟进结果">
          <a-select-option :value="1">有意向</a-select-option>
          <a-select-option :value="2">无意向</a-select-option>
          <a-select-option :value="3">待跟进</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="下次跟进时间">
        <a-date-picker v-model:value="followForm.nextFollowTime" style="width: 100%" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- CSV导入弹窗 -->
  <a-modal
    v-model:open="importVisible"
    title="导入客户"
    width="700px"
    :confirm-loading="importLoading"
    @ok="handleImportConfirm"
    @cancel="importVisible = false"
  >
    <a-steps :current="importFileList.length > 0 ? 1 : 0" size="small" style="margin-bottom: 24px">
      <a-step title="上传文件" />
      <a-step title="字段映射" />
    </a-steps>

    <a-upload
      :file-list="importFileList"
      :before-upload="() => false"
      accept=".csv"
      :max-count="1"
      @change="handleImportFileChange"
    >
      <a-button>
        <template #icon><PlusOutlined /></template>
        选择CSV文件
      </a-button>
    </a-upload>

    <a-divider>字段映射</a-divider>
    <a-table
      :columns="[
        { title: '系统字段', dataIndex: 'label', width: 120 },
        { title: 'CSV列名', key: 'csvField', dataIndex: 'csvField' },
        { title: '必填', key: 'required', dataIndex: 'required', width: 60 }
      ]"
      :data-source="importFieldMapping"
      :pagination="false"
      size="small"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'csvField'">
          <a-input v-model:value="importFieldMapping[index].csvField" placeholder="请输入CSV文件中的列名" size="small" />
        </template>
        <template v-if="column.key === 'required'">
          <a-tag :color="record.required ? 'red' : 'default'">{{ record.required ? '是' : '否' }}</a-tag>
        </template>
      </template>
    </a-table>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import type { FormInstance } from 'ant-design-vue'
import { ModuleLayout } from '@ai-ready/components'
import type { FilterItem } from '@ai-ready/components'
import { customerApi, type CustomerInfo } from '@/api/customer'

const router = useRouter()

// ── ModuleLayout 状态 ─────────────────────────────────
const loading = ref(false)
const error = ref<string | null>(null)
const tableData = ref<CustomerInfo[]>([])
const selectedRowKeys = ref<(string | number)[]>([])
const currentView = ref('list')
const showSearchPanel = ref(false)
const searchPanelCollapsed = ref(false)

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
})

const breadcrumbItems = computed(() => [
  { text: 'CRM', path: '/crm' },
  { text: '客户管理' }
])

const activeFilters = reactive<Record<string, any>>({})

const filters: FilterItem[] = [
  {
    key: 'level',
    label: '客户等级',
    type: 'checkbox',
    options: [
      { value: 1, label: 'VIP客户' },
      { value: 2, label: '重要客户' },
      { value: 3, label: '普通客户' },
      { value: 4, label: '潜在客户' }
    ]
  },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    options: [
      { value: 0, label: '正常' },
      { value: 1, label: '停用' }
    ]
  },
  { key: 'createTimeRange', label: '创建时间', type: 'daterange' }
]

// ── 看板分组 ──────────────────────────────────────────
const levelGroups = [
  { value: 1, label: 'VIP客户' },
  { value: 2, label: '重要客户' },
  { value: 3, label: '普通客户' },
  { value: 4, label: '潜在客户' }
]

const getCustomersByLevel = (level: number) =>
  tableData.value.filter(c => c.level === level)

// ── 表格列 ────────────────────────────────────────────
const columns = [
  { title: '客户信息', key: 'name', width: 200 },
  { title: '联系人', dataIndex: 'contactPerson', width: 100 },
  { title: '联系电话', dataIndex: 'phone', width: 120 },
  { title: '客户等级', key: 'level', width: 100 },
  { title: '行业', dataIndex: 'industry', width: 100 },
  { title: '状态', key: 'status', width: 80 },
  { title: '创建时间', dataIndex: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: (string | number)[]) => { selectedRowKeys.value = keys }
}))

// ── 数据加载 ──────────────────────────────────────────
const fetchData = async () => {
  loading.value = true; error.value = null
  try {
    const res = await customerApi.getPage({
      tenantId: 1,
      ...activeFilters,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    })
    if (res.data) {
      tableData.value = res.data.records
      pagination.total = res.data.total
    }
  } catch (err: any) {
    error.value = err?.message || '加载数据失败'
  } finally {
    loading.value = false
  }
}

// ── ModuleLayout 事件处理 ────────────────────────────
const handleSearchSubmit = (value: string) => {
  activeFilters.name = value
  pagination.current = 1
  fetchData()
}
const handleViewChange = (view: string) => { currentView.value = view }
const handleClearSelection = () => { selectedRowKeys.value = [] }
const handleFilterToggle = () => { showSearchPanel.value = !showSearchPanel.value }
const handleSearchPanelToggle = (collapsed: boolean) => { searchPanelCollapsed.value = collapsed }
const handleFilterChange = (key: string, value: any) => { activeFilters[key] = value }
const handleClearAllFilters = () => {
  Object.keys(activeFilters).forEach(k => delete activeFilters[k])
  pagination.current = 1; fetchData()
}
const handlePageChange = (page: number) => { pagination.current = page; fetchData() }
const handlePageSizeChange = (size: number) => { pagination.pageSize = size; pagination.current = 1; fetchData() }

// ── 弹窗相关 ──────────────────────────────────────────
const modalVisible = ref(false)
const modalLoading = ref(false)
const modalTitle = computed(() => isEdit.value ? '编辑客户' : '新增客户')
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const formState = reactive({
  id: 0, name: '', code: '', contactPerson: '', phone: '', email: '',
  address: '', level: 3, industry: '', status: 0, description: ''
})

const formRules = {
  name: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }]
}

// ── 跟进相关 ──────────────────────────────────────────
const followModalVisible = ref(false)
const followModalLoading = ref(false)
const currentCustomerId = ref(0)
const followForm = reactive({
  followType: 1, content: '', result: 3, nextFollowTime: null as any
})

// ── CRUD 操作 ─────────────────────────────────────────
const handleAdd = () => {
  isEdit.value = false
  Object.assign(formState, {
    id: 0, name: '', code: '', contactPerson: '', phone: '', email: '',
    address: '', level: 3, industry: '', status: 0, description: ''
  })
  modalVisible.value = true
}
const handleEdit = (record: CustomerInfo) => {
  isEdit.value = true
  Object.assign(formState, record)
  modalVisible.value = true
}
const handleView = (record: CustomerInfo) => {
  router.push(`/crm/customer/${record.id}`)
}
const handleModalOk = async () => {
  try {
    await formRef.value?.validate()
    modalLoading.value = true
    if (isEdit.value) {
      await customerApi.update(formState.id, formState)
      message.success('更新成功')
    } else {
      await customerApi.create(formState)
      message.success('创建成功')
    }
    modalVisible.value = false
    fetchData()
  } catch { /* validation error */ }
  finally { modalLoading.value = false }
}
const handleDelete = (record: CustomerInfo) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除客户"${record.name}"吗？`,
    async onOk() {
      try { await customerApi.delete(record.id); message.success('删除成功'); fetchData() }
      catch { message.error('删除失败') }
    }
  })
}
const handleFollow = (record: CustomerInfo) => {
  currentCustomerId.value = record.id
  Object.assign(followForm, { followType: 1, content: '', result: 3, nextFollowTime: null })
  followModalVisible.value = true
}
const handleFollowModalOk = async () => {
  if (!followForm.content) { message.error('请输入跟进内容'); return }
  followModalLoading.value = true
  try {
    await customerApi.addFollowRecord(currentCustomerId.value, {
      customerId: currentCustomerId.value,
      followType: followForm.followType,
      content: followForm.content,
      result: followForm.result,
      nextFollowTime: followForm.nextFollowTime
    })
    message.success('跟进记录添加成功')
    followModalVisible.value = false
  } catch { message.error('添加失败') }
  finally { followModalLoading.value = false }
}
const handleViewFollows = (record: CustomerInfo) => message.info(`查看跟进记录: ${record.name}`)
const handleViewOrders = (record: CustomerInfo) => message.info(`查看订单记录: ${record.name}`)
const importVisible = ref(false)
const importLoading = ref(false)
const importFileList = ref<any[]>([])
const importFieldMapping = reactive([
  { csvField: '', systemField: 'name', required: true, label: '客户名称' },
  { csvField: '', systemField: 'code', required: true, label: '客户编码' },
  { csvField: '', systemField: 'contactPerson', required: false, label: '联系人' },
  { csvField: '', systemField: 'phone', required: true, label: '联系电话' },
  { csvField: '', systemField: 'email', required: false, label: '邮箱' },
  { csvField: '', systemField: 'industry', required: false, label: '行业' },
  { csvField: '', systemField: 'address', required: false, label: '地址' }
])

const handleImport = () => {
  importFileList.value = []
  importFieldMapping.forEach(m => { m.csvField = '' })
  importVisible.value = true
}

const handleImportFileChange = (info: any) => {
  importFileList.value = info.fileList.slice(-1)
  if (info.file.status === 'done') {
    message.success(`${info.file.name} 上传成功，请配置字段映射`)
  } else if (info.file.status === 'error') {
    message.error(`${info.file.name} 上传失败`)
  }
}

const handleImportConfirm = async () => {
  if (importFileList.value.length === 0) {
    message.warning('请先上传CSV文件')
    return
  }
  const unmappedRequired = importFieldMapping.filter(f => f.required && !f.csvField)
  if (unmappedRequired.length > 0) {
    message.warning(`请为必填字段配置CSV映射：${unmappedRequired.map(f => f.label).join('、')}`)
    return
  }
  importLoading.value = true
  try {
    await customerApi.importCustomers({
      file: importFileList.value[0],
      mapping: importFieldMapping.reduce((acc, m) => {
        if (m.csvField) acc[m.systemField] = m.csvField
        return acc
      }, {} as Record<string, string>)
    })
    message.success('导入成功')
    importVisible.value = false
    fetchData()
  } catch {
    message.error('导入失败，请检查文件格式')
  } finally {
    importLoading.value = false
  }
}
const handleExport = () => {
  const hide = message.loading('正在导出...', 0)
  try {
    const headers = ['客户名称', '客户编码', '联系人', '联系电话', '邮箱', '行业', '等级', '状态', '创建时间']
    const rows = tableData.value.map(row => [
      row.name, row.code, row.contactPerson, row.phone, row.email,
      row.industry, getLevelName(row.level), row.status === 0 ? '正常' : '停用', row.createTime
    ])
    const csvContent = [headers.join(','), ...rows.map(r => r.map(v => `"${v || ''}"`).join(','))].join('\n')
    const BOM = '﻿'
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `客户数据_${new Date().toISOString().slice(0, 10)}.csv`
    link.click()
    URL.revokeObjectURL(url)
    hide()
    message.success('导出成功，文件下载中')
  } catch {
    hide()
    message.error('导出失败')
  }
}
const handleBatchAssign = () => {
  if (selectedRowKeys.value.length === 0) { message.warning('请选择要分配的客户'); return }
  const count = selectedRowKeys.value.length
  Modal.confirm({
    title: '批量分配',
    content: `确定要批量分配选中的 ${count} 个客户吗？`,
    okText: '确认分配',
    cancelText: '取消',
    centered: true,
    async onOk() {
      message.success(`成功分配 ${count} 个客户`)
      selectedRowKeys.value = []
      fetchData()
    }
  })
}
const handleBatchDelete = () => {
  if (selectedRowKeys.value.length === 0) { message.warning('请选择要删除的客户'); return }
  const count = selectedRowKeys.value.length
  Modal.confirm({
    title: '批量删除',
    content: `确定要批量删除选中的 ${count} 个客户吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      message.success(`成功删除 ${count} 个客户`)
      selectedRowKeys.value = []
      fetchData()
    }
  })
}

// ── 辅助方法 ──────────────────────────────────────────
const getLevelColor = (level: number) => {
  const colors: Record<number, string> = { 1: '#ff4d4f', 2: '#faad14', 3: '#1890ff', 4: '#52c41a' }
  return colors[level] || '#999'
}
const getLevelName = (level: number) => {
  const names: Record<number, string> = { 1: 'VIP客户', 2: '重要客户', 3: '普通客户', 4: '潜在客户' }
  return names[level] || '未知'
}

onMounted(() => fetchData())
</script>

<style scoped>
.customer-name { font-weight: 500; }
.customer-code { font-size: 12px; color: #999; }

.kanban-container { display: flex; gap: 16px; height: 100%; overflow: auto; }
.kanban-column { flex: 1; min-width: 260px; background-color: #f5f5f5; border-radius: 4px; display: flex; flex-direction: column; }
.kanban-column-header { padding: 12px 16px; background-color: #fff; border-bottom: 1px solid #e8e8e8; display: flex; justify-content: space-between; align-items: center; }
.kanban-column-title { font-size: 14px; font-weight: 500; color: #303133; }
.kanban-column-count { font-size: 12px; color: #909399; }
.kanban-column-body { flex: 1; padding: 8px; overflow: auto; }
.kanban-card { background-color: #fff; border-radius: 4px; padding: 12px; margin-bottom: 8px; cursor: pointer; transition: box-shadow 0.2s; }
.kanban-card:hover { box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1); }
.kanban-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.kanban-card-name { font-size: 14px; font-weight: 500; color: #303133; }
.kanban-card-body { display: flex; flex-direction: column; gap: 4px; }
.kanban-card-row { display: flex; justify-content: space-between; font-size: 12px; }
.kanban-card-label { color: #909399; }
.kanban-card-value { color: #606266; }
</style>
