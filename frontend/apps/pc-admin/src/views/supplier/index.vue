<template>
  <PageContainer full-height>
    <!-- 操作栏 -->
    <template #headerExtra>
      <a-space>
        <a-button type="primary" @click="handleCreate">
          <template #icon><PlusOutlined /></template>
          新增供应商
        </a-button>
        <a-button @click="handleImport">导入</a-button>
        <a-button @click="handleExport">导出</a-button>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          更新 {{ dayjs(lastUpdated).format('HH:mm') }}
        </span>
      </a-space>
    </template>

    <!-- 搜索 -->
    <template #filter>
      <a-row :gutter="[12, 12]" align="middle">
        <a-col :span="6">
          <a-input
            v-model:value="searchKeyword"
            placeholder="搜索供应商编码 / 名称..."
            allow-clear
            @press-enter="handleSearch"
          >
            <template #prefix><SearchOutlined /></template>
          </a-input>
        </a-col>
        <a-col :span="4">
          <a-select
            v-model:value="filters.supplierLevel"
            placeholder="供应商等级"
            allow-clear
            style="width: 100%"
            @change="handleFilterChange"
          >
            <a-select-option value="A">A级</a-select-option>
            <a-select-option value="B">B级</a-select-option>
            <a-select-option value="C">C级</a-select-option>
            <a-select-option value="D">D级</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="4">
          <a-select
            v-model:value="filters.cooperationStatus"
            placeholder="合作状态"
            allow-clear
            style="width: 100%"
            @change="handleFilterChange"
          >
            <a-select-option :value="1">正常合作</a-select-option>
            <a-select-option :value="2">暂停合作</a-select-option>
            <a-select-option :value="3">终止合作</a-select-option>
            <a-select-option :value="4">潜在供应商</a-select-option>
          </a-select>
        </a-col>
        <a-col :span="4">
          <a-space>
            <a-button type="primary" @click="handleSearch">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-col>
        <a-col :span="6" style="text-align: right;">
          <span style="color: #909399; font-size: 13px;">共 {{ pagination.total }} 条记录</span>
        </a-col>
      </a-row>
    </template>

    <!-- 统计卡片 -->
    <template #headerContent>
      <a-row :gutter="12" style="flex: 1;">
        <a-col :span="6">
          <div class="stat-card stat-card--blue">
            <div class="stat-card-icon">
              <TeamOutlined />
            </div>
            <div class="stat-card-content">
              <div class="stat-card-title">供应商总数</div>
              <div class="stat-card-value">{{ pagination.total }}</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card stat-card--green">
            <div class="stat-card-icon">
              <StarOutlined />
            </div>
            <div class="stat-card-content">
              <div class="stat-card-title">A级供应商</div>
              <div class="stat-card-value">{{ dataSource.filter(s => s.supplierLevel === 'A').length }}</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card stat-card--orange">
            <div class="stat-card-icon">
              <CheckCircleOutlined />
            </div>
            <div class="stat-card-content">
              <div class="stat-card-title">正常合作</div>
              <div class="stat-card-value">{{ dataSource.filter(s => s.cooperationStatus === 1).length }}</div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card stat-card--purple">
            <div class="stat-card-icon">
              <DesktopOutlined />
            </div>
            <div class="stat-card-content">
              <div class="stat-card-title">门户已激活</div>
              <div class="stat-card-value">{{ dataSource.filter(s => s.portalStatus === 1).length }}</div>
            </div>
          </div>
        </a-col>
      </a-row>
    </template>

    <!-- 表格 -->
    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      :show-search="false"
      :show-add="false"
      :selectable="true"
      :show-export="false"
      :show-summary="true"
      :summary-data="summaryData"
      @page-change="handlePageChange"
      @selection-change="handleSelectionChange"
    >
      <template #empty>
        <a-empty v-if="hasActiveFilters" description="当前筛选条件下无匹配供应商">
          <template #image><SearchOutlined style="font-size: 48px; color: #faad14" /></template>
          <a-button @click="handleResetFilters">清除筛选</a-button>
        </a-empty>
        <a-empty v-else description="暂无供应商数据">
          <template #image><InboxOutlined style="font-size: 48px; color: #d9d9d9" /></template>
          <a-button type="primary" @click="handleCreate">新增供应商</a-button>
        </a-empty>
      </template>

      <template #supplierLevel="{ record }">
        <template v-if="record.__empty_row">
          <span class="empty-placeholder">&nbsp;</span>
        </template>
        <a-tag v-else :color="getLevelColor(record.supplierLevel)">{{ record.supplierLevel }}级</a-tag>
      </template>
      <template #cooperationStatus="{ record }">
        <template v-if="record.__empty_row">
          <span class="empty-placeholder">&nbsp;</span>
        </template>
        <a-tag v-else :color="getStatusColor(record.cooperationStatus)">
          {{ getStatusLabel(record.cooperationStatus) }}
        </a-tag>
      </template>
      <template #portalStatus="{ record }">
        <template v-if="record.__empty_row">
          <span class="empty-placeholder">&nbsp;</span>
        </template>
        <a-tag v-else :color="record.portalStatus === 1 ? 'purple' : 'default'">
          {{ getPortalStatusLabel(record.portalStatus) }}
        </a-tag>
      </template>
      <template #comprehensiveScore="{ record }">
        <template v-if="record.__empty_row">
          <span class="empty-placeholder">&nbsp;</span>
        </template>
        <a-rate v-else :value="Math.round(record.comprehensiveScore / 20)" disabled allow-half style="font-size: 14px" />
      </template>
      <template #action="{ record }">
        <template v-if="record.__empty_row">
          <span class="empty-placeholder">&nbsp;</span>
        </template>
        <a-space v-else :size="0" class="action-cell-inner">
          <a-tooltip title="详情">
            <a-button type="link" size="small" @click="handleDetail(record)">
              <template #icon><ProfileOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip title="绩效">
            <a-button type="link" size="small" @click="handlePerformance(record)">
              <template #icon><BarChartOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip title="编辑">
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
                <a-menu-item key="portal">
                  <DesktopOutlined /> 门户管理
                </a-menu-item>
                <a-menu-item v-if="record.portalStatus !== 1" key="activate_portal">
                  <CheckCircleOutlined /> 激活门户
                </a-menu-item>
                <a-menu-item v-else key="disable_portal" danger>
                  <StopOutlined /> 禁用门户
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="delete" danger>
                  <DeleteOutlined /> 删除
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>
    </VxeTableList>
  </PageContainer>

  <!-- 导入弹窗 -->
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
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import { PlusOutlined, InboxOutlined, SearchOutlined, DownOutlined, EditOutlined, EllipsisOutlined, DeleteOutlined, ProfileOutlined, BarChartOutlined, DesktopOutlined, CheckCircleOutlined, StopOutlined, TeamOutlined, StarOutlined } from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { exportCsv } from '@/utils/exportCsv'
import { executeBatch } from '@/utils/batchOperations'
import { supplierApi, type Supplier } from '@/api/supplier'

const router = useRouter()

// ── 状态 ──
const loading = ref(false)
const dataSource = ref<Supplier[]>([])
const selectedRowKeys = ref<(string | number)[]>([])
const searchKeyword = ref('')
const tableRef = ref()

const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showQuickJumper: true })
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(filters).some(v => v !== undefined && v !== null && v !== '') || !!searchKeyword.value
})

// 空行填充 - 确保表格最少显示20行
const MIN_TABLE_ROWS = 20
const tableDataSource = computed(() => {
  const data = [...dataSource.value]
  const emptyCount = Math.max(0, MIN_TABLE_ROWS - data.length)
  for (let i = 0; i < emptyCount; i++) {
    data.push({ __empty_row: true, id: `__empty_${i}` })
  }
  return data
})

const filters = reactive<Record<string, any>>({})

const breadcrumbItems = computed(() => [
  { text: '供应商管理' }
])

// ── 汇总 ──
const summaryData = computed(() => {
  if (dataSource.value.length === 0) return undefined
  return [
    { label: '本页合计', value: dataSource.value.reduce((s, r) => s + (r.totalPoints || 0), 0), type: 'default' as const }
  ]
})

// ── 表格列 ──
const vxeColumns = computed(() => [
  { title: '供应商编码', field: 'supplierCode', width: 140 },
  { title: '供应商名称', field: 'supplierName', width: 200 },
  { title: '等级', field: 'supplierLevel', width: 80, slotName: 'supplierLevel' },
  { title: '综合评分', field: 'comprehensiveScore', width: 150, slotName: 'comprehensiveScore' },
  { title: '积分', field: 'totalPoints', width: 80 },
  { title: '合作状态', field: 'cooperationStatus', width: 100, slotName: 'cooperationStatus' },
  { title: '门户状态', field: 'portalStatus', width: 100, slotName: 'portalStatus' },
  { title: '联系人', field: 'contactPerson', width: 100 },
  { title: '联系电话', field: 'contactPhone', width: 130 },
  { title: '操作', field: 'action', width: 220, fixed: 'right', type: 'action' }
])

// ── 数据加载 ──
const fetchData = async () => {
  loading.value = true
  try {
    const params: Record<string, any> = {
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    }
    if (searchKeyword.value) params.keyword = searchKeyword.value
    if (filters.supplierLevel) params.supplierLevel = filters.supplierLevel
    if (filters.cooperationStatus) params.cooperationStatus = filters.cooperationStatus

    const res = await supplierApi.page(params)
    dataSource.value = res.records || []
    pagination.total = res.total || 0
    lastUpdated.value = new Date().toISOString()
  } catch (err: any) {
    message.error(err?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

// ── 事件处理 ──
const handleSearch = () => { pagination.current = 1; fetchData() }
const handleReset = () => {
  searchKeyword.value = ''
  filters.supplierLevel = undefined
  filters.cooperationStatus = undefined
  pagination.current = 1
  fetchData()
}
const handleFilterChange = () => { pagination.current = 1; fetchData() }
const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}
const handleSelectionChange = (keys: any[]) => {
  selectedRowKeys.value = keys
}

// ── CRUD ──
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
    content: `确定禁用供应商"${record.supplierName}"的门户账户吗？`,
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

// ── 导入 ──
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

// ── 导出 ──
const handleExport = () => {
  const headers = ['供应商编码', '供应商名称', '等级', '综合评分', '积分', '合作状态', '门户状态', '联系人', '联系电话']
  const rows = dataSource.value.map(row => [
    row.supplierCode || '', row.supplierName || '', row.supplierLevel || '',
    row.comprehensiveScore?.toFixed(1) || '0.0', row.totalPoints || 0,
    getStatusLabel(row.cooperationStatus), getPortalStatusLabel(row.portalStatus),
    row.contactPerson || '', row.contactPhone || ''
  ])
  exportCsv(headers, rows, '供应商数据')
}

// ── 辅助函数 ──
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

function handleResetFilters() {
  searchKeyword.value = ''
  filters.supplierLevel = undefined
  filters.cooperationStatus = undefined
  pagination.current = 1
  fetchData()
}

function handleActionMenuClick(key: string, record: Supplier) {
  switch (key) {
    case 'portal': handlePortal(record); break
    case 'activate_portal': handleActivatePortal(record); break
    case 'disable_portal': handleDisablePortal(record); break
    case 'delete': handleDelete(record); break
  }
}

function handleKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleCreate() }
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
/* ── 统计卡片（渐变背景） ──────────────────────────────── */
.stat-card {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  border-radius: 12px;
  transition: all 0.3s ease;
  cursor: default;
  height: 100%;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  margin-right: 12px;
}

.stat-card-content {
  flex: 1;
}

.stat-card-title {
  font-size: 12px;
  margin-bottom: 2px;
}

.stat-card-value {
  font-family: 'SFMono-Regular', 'SF Mono', 'Fira Code', 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.2;
}

/* 渐变背景 */
.stat-card--blue {
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
}
.stat-card--blue .stat-card-icon { background: rgba(24, 144, 255, 0.2); color: #1890ff; }
.stat-card--blue .stat-card-title { color: rgba(24, 144, 255, 0.85); }
.stat-card--blue .stat-card-value { color: #1890ff; }

.stat-card--green {
  background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%);
}
.stat-card--green .stat-card-icon { background: rgba(82, 196, 26, 0.2); color: #52c41a; }
.stat-card--green .stat-card-title { color: rgba(82, 196, 26, 0.85); }
.stat-card--green .stat-card-value { color: #52c41a; }

.stat-card--orange {
  background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%);
}
.stat-card--orange .stat-card-icon { background: rgba(250, 173, 20, 0.2); color: #faad14; }
.stat-card--orange .stat-card-title { color: rgba(250, 173, 20, 0.85); }
.stat-card--orange .stat-card-value { color: #faad14; }

.stat-card--purple {
  background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%);
}
.stat-card--purple .stat-card-icon { background: rgba(114, 46, 209, 0.2); color: #722ed1; }
.stat-card--purple .stat-card-title { color: rgba(114, 46, 209, 0.85); }
.stat-card--purple .stat-card-value { color: #722ed1; }

.action-more-btn { padding: 0 4px; font-size: 16px; vertical-align: middle; }
.list-update-timestamp {
  font-size: 12px; color: var(--color-text-tertiary, #bbb);
  white-space: nowrap; cursor: help;
  line-height: 32px; vertical-align: middle;
}
.action-cell-inner { flex-wrap: nowrap; }

/* 空行占位符 */
.empty-placeholder {
  color: transparent;
}

/* 表格网格边框 */
:deep(.ant-table-thead > tr > th) {
  border-top: 2px solid #d9d9d9 !important;
  border-right: 1px solid #d9d9d9 !important;
  border-bottom: 2px solid #d9d9d9 !important;
  background: #fafafa !important;
  padding: 12px 16px !important;
  font-weight: 600 !important;
}

:deep(.ant-table-thead > tr > th:first-child) {
  border-left: 2px solid #d9d9d9 !important;
}

:deep(.ant-table-tbody > tr > td) {
  border-right: 1px solid #e8e8e8 !important;
  border-bottom: 1px solid #e8e8e8 !important;
  padding: 12px 16px !important;
}

:deep(.ant-table-tbody > tr > td:first-child) {
  border-left: 1px solid #e8e8e8 !important;
}
</style>
