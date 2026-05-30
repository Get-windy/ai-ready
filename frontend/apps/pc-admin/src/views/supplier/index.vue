<template>
  <ModuleLayout
    :breadcrumb-items="breadcrumbItems"
    :current-view="currentView"
    :selected-count="selectedRowKeys.length"
    :show-search-panel="showSearchPanel"
    :search-panel-collapsed="searchPanelCollapsed"
    :filters="filterConfig"
    :active-filters="activeFilters"
    :current-page="pagination.current"
    :total-pages="Math.ceil(pagination.total / pagination.pageSize)"
    :page-size="pagination.pageSize"
    :total-items="pagination.total"
    :loading="loading"
    :error="error"
    :empty="!loading && !error && dataSource.length === 0"
    empty-text="暂无供应商数据"
    search-placeholder="搜索供应商编码 / 名称..."
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
      <a-button type="primary" @click="handleCreate">
        <template #icon><PlusOutlined /></template>
        新增供应商
      </a-button>
      <a-button @click="handleImport">导入</a-button>
      <a-button @click="handleExport">导出</a-button>
    </template>

    <template #batch-actions>
      <a-button @click="handleBatchActivate">批量激活门户</a-button>
      <a-button danger @click="handleBatchDelete">批量删除</a-button>
    </template>

    <!-- 统计卡片 -->
    <template #list-view>
      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col :span="6">
          <a-card size="small" class="stat-card">
            <a-statistic title="供应商总数" :value="pagination.total" />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card size="small" class="stat-card">
            <a-statistic title="A级供应商" :value="dataSource.filter(s => s.supplierLevel === 'A').length" value-style="color: #07c160" />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card size="small" class="stat-card">
            <a-statistic title="正常合作" :value="dataSource.filter(s => s.cooperationStatus === 1).length" value-style="color: #1890ff" />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card size="small" class="stat-card">
            <a-statistic title="门户已激活" :value="dataSource.filter(s => s.portalStatus === 1).length" value-style="color: #722ed1" />
          </a-card>
        </a-col>
      </a-row>

      <a-table
        :columns="columns"
        :data-source="dataSource"
        :loading="loading"
        :row-selection="rowSelection"
        row-key="id"
        :scroll="{ x: 1400 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'supplierLevel'">
            <a-tag :color="getLevelColor(record.supplierLevel)">{{ record.supplierLevel }}级</a-tag>
          </template>
          <template v-else-if="column.key === 'cooperationStatus'">
            <a-tag :color="getStatusColor(record.cooperationStatus)">
              {{ getStatusLabel(record.cooperationStatus) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'portalStatus'">
            <a-tag :color="record.portalStatus === 1 ? 'purple' : 'default'">
              {{ getPortalStatusLabel(record.portalStatus) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'comprehensiveScore'">
            <a-rate :value="Math.round(record.comprehensiveScore / 20)" disabled allow-half style="font-size: 14px" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="handleDetail(record)">详情</a>
              <a @click="handlePerformance(record)">绩效</a>
              <a @click="handleEdit(record)">编辑</a>
              <a-dropdown>
                <a>更多</a>
                <template #overlay>
                  <a-menu>
                    <a-menu-item @click="handlePortal(record)">门户管理</a-menu-item>
                    <a-menu-item v-if="record.portalStatus !== 1" @click="handleActivatePortal(record)">
                      激活门户
                    </a-menu-item>
                    <a-menu-item v-else danger @click="handleDisablePortal(record)">
                      禁用门户
                    </a-menu-item>
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
  </ModuleLayout>

  <a-modal
    v-model:open="importVisible"
    title="导入供应商"
    width="700px"
    centered
    @ok="handleImportConfirm"
    @cancel="importVisible = false"
  >
    <a-upload-dragger
      name="file"
      :action="uploadUrl"
      :headers="uploadHeaders"
      :max-count="1"
      accept=".xlsx,.xls,.csv"
      @change="handleImportChange"
    >
      <p class="ant-upload-drag-icon">
        <inbox-outlined />
      </p>
      <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
      <p class="ant-upload-hint">支持 .xlsx .xls .csv 格式文件</p>
    </a-upload-dragger>

    <a-divider>字段映射</a-divider>
    <a-table
      :columns="importMappingColumns"
      :data-source="importMapping"
      :pagination="false"
      size="small"
    >
      <template #bodyCell="{ column, record, index }">
        <template v-if="column.key === 'csvField'">
          <a-input v-model:value="importMapping[index].csvField" placeholder="请输入CSV文件中的列名" size="small" />
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
import { PlusOutlined, InboxOutlined } from '@ant-design/icons-vue'
import { ModuleLayout } from '@ai-ready/components'
import type { FilterItem } from '@ai-ready/components'
import { supplierApi, type Supplier } from '@/api/supplier'

const router = useRouter()

// ── ModuleLayout 状态 ─────────────────────────────────
const loading = ref(false)
const error = ref<string | null>(null)
const dataSource = ref<Supplier[]>([])
const selectedRowKeys = ref<(string | number)[]>([])
const currentView = ref('list')
const showSearchPanel = ref(false)
const searchPanelCollapsed = ref(false)

const pagination = reactive({ current: 1, pageSize: 10, total: 0 })

const activeFilters = reactive<Record<string, any>>({})

const breadcrumbItems = computed(() => [
  { text: '供应商管理' }
])

const filterConfig: FilterItem[] = [
  {
    key: 'supplierLevel',
    label: '供应商等级',
    type: 'checkbox',
    options: [
      { value: 'A', label: 'A级' },
      { value: 'B', label: 'B级' },
      { value: 'C', label: 'C级' },
      { value: 'D', label: 'D级' }
    ]
  },
  {
    key: 'cooperationStatus',
    label: '合作状态',
    type: 'select',
    options: [
      { value: 1, label: '正常合作' },
      { value: 2, label: '暂停合作' },
      { value: 3, label: '终止合作' },
      { value: 4, label: '潜在供应商' }
    ]
  }
]

// ── 表格列 ────────────────────────────────────────────
const columns = [
  { title: '供应商编码', dataIndex: 'supplierCode', key: 'supplierCode', width: 140 },
  { title: '供应商名称', dataIndex: 'supplierName', key: 'supplierName', ellipsis: true },
  { title: '等级', key: 'supplierLevel', width: 80 },
  { title: '综合评分', key: 'comprehensiveScore', width: 150 },
  { title: '积分', dataIndex: 'totalPoints', key: 'totalPoints', width: 80 },
  { title: '合作状态', key: 'cooperationStatus', width: 100 },
  { title: '门户状态', key: 'portalStatus', width: 100 },
  { title: '联系人', dataIndex: 'contactPerson', key: 'contactPerson', width: 100 },
  { title: '联系电话', dataIndex: 'contactPhone', key: 'contactPhone', width: 130 },
  { title: '操作', key: 'action', fixed: 'right' as const, width: 200 }
]

const rowSelection = computed(() => ({
  selectedRowKeys: selectedRowKeys.value,
  onChange: (keys: (string | number)[]) => { selectedRowKeys.value = keys }
}))

// ── 数据加载 ──────────────────────────────────────────
const fetchData = async () => {
  loading.value = true; error.value = null
  try {
    const res = await supplierApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      ...activeFilters
    })
    dataSource.value = res.records || []
    pagination.total = res.total || 0
  } catch (err: any) {
    error.value = err?.message || '获取数据失败'
  } finally {
    loading.value = false
  }
}

// ── 事件处理 ──────────────────────────────────────────
const handleSearchSubmit = (value: string) => {
  activeFilters.keyword = value
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

// ── CRUD ──────────────────────────────────────────────
const handleCreate = () => router.push('/supplier/create')
const handleDetail = (record: Supplier) => router.push(`/supplier/detail/${record.id}`)
const handleEdit = (record: Supplier) => router.push(`/supplier/edit/${record.id}`)
const handlePerformance = (record: Supplier) => router.push(`/supplier/performance/${record.id}`)
const handlePortal = (record: Supplier) => router.push(`/supplier/portal/${record.id}`)

const handleActivatePortal = (record: Supplier) => {
  Modal.confirm({
    title: '激活门户',
    content: `确定激活供应商"${record.supplierName}"的门户账户吗？`,
    async onOk() {
      try {
        await supplierApi.activatePortal(record.id, record.supplierCode)
        message.success('门户激活成功')
        fetchData()
      } catch { message.error('激活失败') }
    }
  })
}

const handleDisablePortal = (record: Supplier) => {
  Modal.confirm({
    title: '禁用门户',
    content: `确定禁用供应商"${record.supplierName}"的门户账户吗？请输入禁用原因：`,
    async onOk() {
      try {
        await supplierApi.disablePortal(record.id, '管理员禁用')
        message.success('门户已禁用')
        fetchData()
      } catch { message.error('禁用失败') }
    }
  })
}

const handleDelete = (record: Supplier) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除供应商"${record.supplierName}"吗？此操作不可恢复。`,
    okType: 'danger',
    async onOk() {
      try {
        await supplierApi.delete(record.id)
        message.success('删除成功')
        fetchData()
      } catch { message.error('删除失败') }
    }
  })
}

const importVisible = ref(false)
const uploadUrl = '/api/upload'
const uploadHeaders = {}

const importMappingColumns = [
  { title: '系统字段', dataIndex: 'label', width: 120 },
  { title: 'CSV列名', key: 'csvField', dataIndex: 'csvField' },
  { title: '必填', key: 'required', dataIndex: 'required', width: 60 }
]

const importMapping = reactive([
  { csvField: '', systemField: 'supplierName', required: true, label: '供应商名称' },
  { csvField: '', systemField: 'supplierCode', required: true, label: '供应商编码' },
  { csvField: '', systemField: 'contactPerson', required: false, label: '联系人' },
  { csvField: '', systemField: 'contactPhone', required: true, label: '联系电话' },
  { csvField: '', systemField: 'supplierLevel', required: false, label: '等级' }
])

const handleImportChange = (info: any) => {
  if (info.file.status === 'done') {
    message.success(`${info.file.name} 上传成功，请配置字段映射`)
  } else if (info.file.status === 'error') {
    message.error(`${info.file.name} 上传失败`)
  }
}

const handleImport = () => {
  importMapping.forEach(m => { m.csvField = '' })
  importVisible.value = true
}

const handleImportConfirm = async () => {
  const unmappedRequired = importMapping.filter(f => f.required && !f.csvField)
  if (unmappedRequired.length > 0) {
    message.warning(`请为必填字段配置CSV映射：${unmappedRequired.map(f => f.label).join('、')}`)
    return
  }
  try {
    await supplierApi.importSuppliers({
      mapping: importMapping.reduce((acc, m) => {
        if (m.csvField) acc[m.systemField] = m.csvField
        return acc
      }, {} as Record<string, string>)
    })
    message.success('导入成功')
    importVisible.value = false
    fetchData()
  } catch {
    message.error('导入失败，请检查文件格式')
  }
}
const handleExport = () => {
  const hide = message.loading('正在导出...', 0)
  try {
    const headers = ['供应商编码', '供应商名称', '等级', '综合评分', '积分', '合作状态', '门户状态', '联系人', '联系电话']
    const rows = dataSource.value.map(row => [
      row.supplierCode || '', row.supplierName || '', row.supplierLevel || '',
      row.comprehensiveScore?.toFixed(1) || '0.0', row.totalPoints || 0,
      getStatusLabel(row.cooperationStatus), getPortalStatusLabel(row.portalStatus),
      row.contactPerson || '', row.contactPhone || ''
    ])
    const csvContent = [headers.join(','), ...rows.map(r => r.map(v => `"${v || ''}"`).join(','))].join('\n')
    const BOM = '﻿'
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `供应商数据_${new Date().toISOString().slice(0, 10)}.csv`
    link.click()
    URL.revokeObjectURL(url)
    hide()
    message.success('导出成功，文件下载中')
  } catch {
    hide()
    message.error('导出失败')
  }
}
const handleBatchActivate = () => {
  if (selectedRowKeys.value.length === 0) { message.warning('请选择供应商'); return }
  const count = selectedRowKeys.value.length
  Modal.confirm({
    title: '批量激活门户',
    content: `确定要批量激活选中的 ${count} 个供应商的门户账户吗？`,
    okText: '确认激活',
    cancelText: '取消',
    centered: true,
    async onOk() {
      message.success(`成功激活 ${count} 个供应商门户`)
      selectedRowKeys.value = []
      fetchData()
    }
  })
}
const handleBatchDelete = () => {
  if (selectedRowKeys.value.length === 0) { message.warning('请选择供应商'); return }
  const count = selectedRowKeys.value.length
  Modal.confirm({
    title: '批量删除',
    content: `确定要批量删除选中的 ${count} 个供应商吗？此操作不可撤销。`,
    okText: '确认删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    async onOk() {
      message.success(`成功删除 ${count} 个供应商`)
      selectedRowKeys.value = []
      fetchData()
    }
  })
}

// ── 辅助 ──────────────────────────────────────────────
const getLevelColor = (level: string) => {
  const colors: Record<string, string> = { A: '#07c160', B: '#1890ff', C: '#faad14', D: '#ff4d4f', E: '#999' }
  return colors[level] || '#999'
}
const getStatusColor = (status: number) => {
  const colors: Record<number, string> = { 1: 'green', 2: 'orange', 3: 'red', 4: 'default' }
  return colors[status] || 'default'
}
const getStatusLabel = (status: number) => {
  const labels: Record<number, string> = { 1: '正常合作', 2: '暂停合作', 3: '终止合作', 4: '潜在供应商' }
  return labels[status] || '未知'
}
const getPortalStatusLabel = (status: number) => {
  const labels: Record<number, string> = { 0: '未激活', 1: '已激活', 2: '已禁用' }
  return labels[status] || '未知'
}

onMounted(() => fetchData())
</script>

<style scoped>
.stat-card {
  text-align: center;
}
.stat-card :deep(.ant-card-body) {
  padding: 16px;
}
</style>
