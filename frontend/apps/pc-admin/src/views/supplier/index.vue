<template>
  <ErrorBoundary>
  <PageContainer full-height>
    <template #header>
      <div class="supplier-header">
        <div class="supplier-header-left">
          <a-breadcrumb class="supplier-breadcrumb">
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>供应商管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="supplier-header-title">供应商管理</h2>
        </div>
        <div class="supplier-header-right">
          <a-space>
        <a-button v-permission.disabled="'supplier:add'" type="primary" @click="handleCreate" title="快捷键 Ctrl+N">
          <template #icon><PlusOutlined /></template>
          新增供应商
        </a-button>
        <a-button v-permission.disabled="'supplier:import'" @click="handleImport">导入</a-button>
        <a-button v-permission.disabled="'supplier:export'" @click="handleExport">导出</a-button>
        <span v-if="lastUpdated" class="list-update-timestamp" :title="dayjs(lastUpdated).format('YYYY-MM-DD HH:mm:ss')">
          <ReloadOutlined :spin="loading" style="margin-right: 4px; font-size: 11px; vertical-align: middle; cursor: pointer;" @click="handleRefresh" />
          {{ relativeTimeText }}
          <template v-if="autoRefreshCountdown > 0">
            <span :class="{ 'countdown-warning': autoRefreshCountdown <= 5 }"> · {{ autoRefreshCountdown }}s 后刷新</span>
          </template>
        </span>
	      </a-space>
        </div>
      </div>
    </template>

    <!-- 搜索 -->
    <template #filter>
      <a-row :gutter="[12, 12]" align="middle">
        <a-col :span="6">
          <a-input
            v-model:value="searchKeyword"
            placeholder="搜索供应商编码 / 名称..."
            allow-clear
            size="small"
            @press-enter="handleSearch"
            @input="handleSearchInput"
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
            size="small"
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
            size="small"
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
              <div class="stat-card-value">
                <span v-if="statLoading" class="stat-skeleton" />
                <template v-else>{{ statData.totalSuppliers }}</template>
              </div>
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
              <div class="stat-card-value">
                <span v-if="statLoading" class="stat-skeleton" />
                <template v-else>{{ statData.levelACount }}</template>
              </div>
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
              <div class="stat-card-value">
                <span v-if="statLoading" class="stat-skeleton" />
                <template v-else>{{ statData.activeCooperationCount }}</template>
              </div>
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
              <div class="stat-card-value">
                <span v-if="statLoading" class="stat-skeleton" />
                <template v-else>{{ statData.portalActivatedCount }}</template>
              </div>
            </div>
          </div>
        </a-col>
      </a-row>
    </template>

    <div class="table-wrapper">
    <!-- 错误提示 -->
    <a-alert
      v-if="fetchError"
      message="数据加载失败"
      description="无法获取供应商数据，请检查网络连接后重试"
      type="error"
      show-icon
      closable
      style="margin-bottom: 16px"
      @close="fetchError = false"
    >
      <template #action>
        <a-button size="small" type="primary" @click="fetchData">重试</a-button>
      </template>
    </a-alert>

    <!-- 表格 -->
    <VxeTableList
      ref="tableRef"
      :columns="vxeColumns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      :show-search="false"
      :show-add="false"
      :selectable="true"
      :show-export="false"
      :show-summary="true"
      :summary-data="summaryData"
      @cell-dblclick="handleView"
      @page-change="handlePageChange"
      @selection-change="handleSelectionChange"
    >
      <template #batch-actions="{ selectedRows: rows }">
        <a-button v-permission.disabled="'supplier:delete'" danger size="small" :loading="batchDeleting" @click="handleBatchDelete">
          <template #icon><DeleteOutlined /></template>
          批量删除 ({{ rows.length }})
        </a-button>
        <a-button v-permission.disabled="'supplier:export'" size="small" @click="handleBatchExport">
          <template #icon><ExportOutlined /></template>
          导出选中
        </a-button>
      </template>
      <template #empty>
        <div class="table-empty">
          <template v-if="hasError">
            <WarningOutlined class="table-empty-icon" style="color: #faad14" />
            <p class="table-empty-text">加载失败</p>
            <a-button type="primary" size="small" @click="fetchData" class="table-empty-action">
              <ReloadOutlined /> 重试
            </a-button>
          </template>
          <template v-else>
            <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
            <InboxOutlined v-else class="table-empty-icon" />
            <p v-if="hasActiveFilters" class="table-empty-text">
              没有符合条件的供应商记录，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无供应商数据
            </p>
          </template>
        </div>
      </template>

      <template #supplierLevel="{ record }">
        <a-tag :color="getLevelColor(record.supplierLevel)">{{ record.supplierLevel }}级</a-tag>
      </template>
      <template #cooperationStatus="{ record }">
        <a-tag :color="getStatusColor(record.cooperationStatus)">
          {{ getStatusLabel(record.cooperationStatus) }}
        </a-tag>
      </template>
      <template #portalStatus="{ record }">
        <a-tag :color="record.portalStatus === 1 ? 'purple' : 'default'">
          {{ getPortalStatusLabel(record.portalStatus) }}
        </a-tag>
      </template>
      <template #comprehensiveScore="{ record }">
        <a-rate :value="Math.round(record.comprehensiveScore / 20)" disabled allow-half style="font-size: 14px" />
      </template>
      <template #action="{ record }">
        <a-space :size="0" class="action-cell-inner">
          <a-tooltip title="详情">
            <a-button v-permission.disabled="'supplier:view'" type="link" size="small" @click="handleDetail(record)">
              <template #icon><ProfileOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip title="绩效">
            <a-button v-permission.disabled="'supplier:performance'" type="link" size="small" @click="handlePerformance(record)">
              <template #icon><BarChartOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-tooltip title="编辑">
            <a-button v-permission.disabled="'supplier:edit'" type="link" size="small" @click="handleEdit(record)">
              <template #icon><EditOutlined /></template>
            </a-button>
          </a-tooltip>
          <a-dropdown trigger="click">
            <a-button type="link" size="small" class="action-more-btn">
              <template #icon><EllipsisOutlined /></template>
            </a-button>
            <template #overlay>
              <a-menu @click="(info: any) => handleActionMenuClick(String(info.key), record)">
                <a-menu-item v-permission.disabled="'supplier:portal'" key="portal">
                  <DesktopOutlined /> 门户管理
                </a-menu-item>
                <a-menu-item v-permission.disabled="'supplier:portal'" v-if="record.portalStatus !== 1" key="activate_portal">
                  <CheckCircleOutlined /> 激活门户
                </a-menu-item>
                <a-menu-item v-permission.disabled="'supplier:portal'" v-else key="disable_portal" danger>
                  <StopOutlined /> 禁用门户
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item v-permission.disabled="'supplier:delete'" key="delete" danger>
                  <DeleteOutlined /> 删除
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </a-space>
      </template>
    </VxeTableList>
    </div>
  </PageContainer>
  </ErrorBoundary>

  <!-- 门户管理弹窗 -->
  <a-modal
    v-model:open="portalModalVisible"
    title="门户管理"
    width="520px"
    centered
    :footer="null"
    @cancel="portalModalVisible = false"
  >
    <template v-if="currentPortalSupplier">
      <a-descriptions :column="1" bordered size="small">
        <a-descriptions-item label="供应商名称">{{ currentPortalSupplier.supplierName }}</a-descriptions-item>
        <a-descriptions-item label="供应商编码">{{ currentPortalSupplier.supplierCode }}</a-descriptions-item>
        <a-descriptions-item label="门户状态">
          <a-tag :color="currentPortalSupplier.portalStatus === 1 ? 'purple' : 'default'">
            {{ getPortalStatusLabel(currentPortalSupplier.portalStatus) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="门户账户ID">{{ currentPortalSupplier.portalAccountId || '-' }}</a-descriptions-item>
      </a-descriptions>
      <div style="margin-top: 24px; text-align: center;">
        <a-button
          v-if="currentPortalSupplier.portalStatus !== 1"
          type="primary"
          :loading="portalLoading"
          @click="handleActivatePortal(currentPortalSupplier)"
        >
          <template #icon><CheckCircleOutlined /></template>
          激活门户
        </a-button>
        <a-button
          v-else
          danger
          :loading="portalLoading"
          @click="handleDisablePortal(currentPortalSupplier)"
        >
          <template #icon><StopOutlined /></template>
          禁用门户
        </a-button>
      </div>
    </template>
  </a-modal>

  <!-- 导入弹窗 -->
  <a-modal
    v-model:open="importVisible"
    title="导入供应商"
    width="700px"
    centered
    :confirm-loading="importLoading"
    @ok="handleImportConfirm"
    @cancel="importVisible = false"
  >
    <a-upload-dragger
      name="file"
      :before-upload="handleBeforeUpload"
      :show-upload-list="false"
      accept=".xlsx,.xls,.csv"
    >
      <p class="ant-upload-drag-icon">
        <inbox-outlined />
      </p>
      <p class="ant-upload-text">点击或拖拽文件到此区域上传</p>
      <p class="ant-upload-hint">支持 .xlsx .xls .csv 格式文件</p>
    </a-upload-dragger>

    <div v-if="uploadFile" style="margin-top: 12px; padding: 8px 12px; background: #f6ffed; border: 1px solid #b7eb8f; border-radius: 4px;">
      已选择文件：{{ uploadFile.name }}
    </div>

    <a-divider>字段映射</a-divider>
    <VxeTableList
      :columns="importMappingColumns"
      :data-source="importMapping"
      :pagination="false as any"
      :show-toolbar="false"
      :selectable="false"
      :show-add="false"
      :show-search="false"
      :show-export="false"
      :show-batch-delete="false"
    >
      <template #csvFieldCell="{ record, index }">
        <a-input v-model:value="importMapping[index].csvField" placeholder="请输入CSV文件中的列名" size="small" />
      </template>
      <template #requiredCell="{ record }">
        <a-tag :color="record.required ? 'red' : 'default'">{{ record.required ? '是' : '否' }}</a-tag>
      </template>
    </VxeTableList>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import { message, Modal } from 'ant-design-vue'
import { debounce } from 'lodash-es'
import {
  PlusOutlined, InboxOutlined, SearchOutlined, DownOutlined,
  EditOutlined, EllipsisOutlined, DeleteOutlined,
  ProfileOutlined, BarChartOutlined, DesktopOutlined,
  CheckCircleOutlined, StopOutlined, TeamOutlined,
  StarOutlined, ExportOutlined, UploadOutlined, WarningOutlined, ReloadOutlined
} from '@ant-design/icons-vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { exportCsv } from '@/utils/exportCsv'
import { supplierApi, type Supplier } from '@/api/supplier'

// ── 防抖工具 ────────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const router = useRouter()

// ── 状态 ──
const loading = ref(false)
const portalLoading = ref(false)
const batchDeleting = ref(false)
const fetchError = ref(false)
const hasError = ref(false)
const autoRefreshCountdown = ref(0)
let autoRefreshTimer: ReturnType<typeof setInterval> | undefined
let countdownTimer: ReturnType<typeof setInterval> | undefined
const dataSource = ref<Supplier[]>([])
const selectedRowKeys = ref<(string | number)[]>([])
const selectedRows = ref<Supplier[]>([])
const searchKeyword = ref('')
const tableRef = ref()
const importVisible = ref(false)
const importLoading = ref(false)
const uploadFile = ref<File | null>(null)

// ── 统计数据 ──
const statData = reactive({
  totalSuppliers: 0,
  levelACount: 0,
  activeCooperationCount: 0,
  portalActivatedCount: 0,
})
const statLoading = ref(false)

// ── 门户管理弹窗 ──
const portalModalVisible = ref(false)
const currentPortalSupplier = ref<Supplier | null>(null)

const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true, showQuickJumper: true })
const lastUpdated = ref('')

const hasActiveFilters = computed(() => {
  return Object.values(filters).some(v => v !== undefined && v !== null && v !== '') || !!searchKeyword.value
})

// ── 表格数据 ──

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
  { title: '综合评分', field: 'comprehensiveScore', width: 120, align: 'right' },
  { title: '积分', field: 'totalPoints', width: 80, align: 'right' },
  { title: '合作状态', field: 'cooperationStatus', width: 100, slotName: 'cooperationStatus' },
  { title: '门户状态', field: 'portalStatus', width: 100, slotName: 'portalStatus' },
  { title: '联系人', field: 'contactPerson', width: 100 },
  { title: '联系电话', field: 'contactPhone', width: 130 },
  { title: '操作', field: 'action', width: 240, fixed: 'right', type: 'action' }
])

// ── 相对时间 ──
const relativeTimeText = computed(() => {
  if (!lastUpdated.value) return ''
  return dayjs(lastUpdated.value).fromNow()
})

// ── 防抖搜索 ──
const handleSearchInput = debounce(() => {
  if (searchKeyword.value === '' && !hasActiveFilters.value) {
    // 搜索框无内容且无筛选条件时保留当前页，避免无意义刷新
    return
  }
  pagination.current = 1
  fetchData()
}, 300)

// ── 自动刷新 ──
function startAutoRefresh() {
  stopAutoRefresh()
  const interval = 60
  autoRefreshCountdown.value = interval
  autoRefreshTimer = setInterval(() => {
    fetchData()
    fetchStatistics()
    autoRefreshCountdown.value = interval
  }, interval * 1000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
}

function stopAutoRefresh() {
  if (autoRefreshTimer) { clearInterval(autoRefreshTimer); autoRefreshTimer = undefined }
  if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = undefined }
  autoRefreshCountdown.value = 0
}

function handleRefresh() {
  if (loading.value) return
  fetchData()
  fetchStatistics()
  autoRefreshCountdown.value = 60
}

// ── 数据加载 ──
const fetchData = async () => {
  loading.value = true
  fetchError.value = false
  hasError.value = false
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
    autoRefreshCountdown.value = 60
  } catch (err: any) {
    hasError.value = true
    console.warn('[供应商] 获取数据失败', err)
    fetchError.value = true
    message.error(err?.message || '获取数据失败')
  } finally {
    loading.value = false
  }
}

// ── 统计数据加载（后端全量，非当前页） ──
const fetchStatistics = async () => {
  statLoading.value = true
  try {
    const res = await supplierApi.getStatistics()
    if (res) {
      Object.assign(statData, {
        totalSuppliers: (res as any).totalSuppliers ?? (res as any).data?.totalSuppliers ?? 0,
        levelACount: (res as any).levelACount ?? (res as any).data?.levelACount ?? 0,
        activeCooperationCount: (res as any).activeCooperationCount ?? (res as any).data?.activeCooperationCount ?? 0,
        portalActivatedCount: (res as any).portalActivatedCount ?? (res as any).data?.portalActivatedCount ?? 0,
      })
    }
  } catch (err) {
    console.warn('[供应商] 加载统计数据失败', err)
  } finally {
    statLoading.value = false
  }
}

// ── 事件处理 ──
const handleSearch = () => { handleSearchInput.cancel(); pagination.current = 1; fetchData() }
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
const handleSelectionChange = (keys: any[], rows: any[]) => {
  selectedRowKeys.value = keys
  selectedRows.value = rows
}

// ── CRUD ──
const handleCreate = () => router.push('/supplier/create')
const handleDetail = (record: Supplier) => router.push(`/supplier/detail/${record.id}`)
const handleEdit = (record: Supplier) => router.push(`/supplier/edit/${record.id}`)
const handlePerformance = (record: Supplier) => router.push(`/supplier/performance/${record.id}`)

const handleView = (record: any) => {
  const row = record?.row ?? record
  router.push(`/supplier/detail/${row.id}`)
}

// ── 门户管理（弹窗模式） ──
const handlePortal = (record: Supplier) => {
  currentPortalSupplier.value = record
  portalModalVisible.value = true
}

const handleActivatePortal = async (record: Supplier) => {
  portalLoading.value = true
  try {
    await supplierApi.activatePortal(record.id, record.supplierCode)
    console.warn('[供应商] 操作成功: 门户激活成功')
    message.success('门户激活成功')
    portalModalVisible.value = false
    fetchData()
    fetchStatistics()
  } catch (err) {
    console.warn('[供应商] 门户激活失败', err)
    message.error('激活失败')
  } finally {
    portalLoading.value = false
  }
}

const handleDisablePortal = async (record: Supplier) => {
  portalLoading.value = true
  try {
    await supplierApi.disablePortal(record.id, '管理员禁用')
    console.warn('[供应商] 操作成功: 门户已禁用')
    message.success('门户已禁用')
    portalModalVisible.value = false
    fetchData()
    fetchStatistics()
  } catch (err) {
    console.warn('[供应商] 门户禁用失败', err)
    message.error('禁用失败')
  } finally {
    portalLoading.value = false
  }
}

const handleDelete = (record: Supplier) => {
  Modal.confirm({
    title: '确认删除',
    content: `确定要删除供应商"${record.supplierName}"吗？此操作不可恢复。`,
    okType: 'danger',
    async onOk() {
      try {
        await supplierApi.delete(record.id)
        console.warn('[供应商] 操作成功: 删除成功')
        message.success('删除成功')
        fetchData()
        fetchStatistics()
      } catch (err) { console.warn('[供应商] 删除失败', err); message.error('删除失败') }
    }
  })
}

// ── 批量操作 ──
const handleBatchDelete = () => {
  if (selectedRows.value.length === 0) {
    message.warning('请先选择要删除的供应商')
    return
  }
  Modal.confirm({
    title: '批量删除',
    content: `确定要删除选中的 ${selectedRows.value.length} 个供应商吗？此操作不可恢复。`,
    okType: 'danger',
    async onOk() {
      try {
        for (const row of selectedRows.value) {
          await supplierApi.delete(row.id)
        }
        console.warn('[供应商] 操作成功: 批量删除')
        message.success(`成功删除 ${selectedRows.value.length} 个供应商`)
        selectedRows.value = []
        selectedRowKeys.value = []
        tableRef.value?.clearSelection()
        fetchData()
        fetchStatistics()
      } catch (err) { console.warn('[供应商] 批量删除失败', err); message.error('批量删除失败') }
    }
  })
}

// ── 导入 ──
const importMappingColumns = [
  { title: '系统字段', field: 'label', width: 120 },
  { title: 'CSV列名', field: 'csvField', slotName: 'csvFieldCell' },
  { title: '必填', field: 'required', width: 60, slotName: 'requiredCell' }
]

const importMapping = reactive([
  { csvField: '', systemField: 'supplierName', required: true, label: '供应商名称' },
  { csvField: '', systemField: 'supplierCode', required: true, label: '供应商编码' },
  { csvField: '', systemField: 'contactPerson', required: false, label: '联系人' },
  { csvField: '', systemField: 'contactPhone', required: true, label: '联系电话' },
  { csvField: '', systemField: 'supplierLevel', required: false, label: '等级' }
])

const handleBeforeUpload = (file: File) => {
  uploadFile.value = file
  // 尝试读取 CSV/JSON 文件，提取列名供字段映射
  if (file.name.endsWith('.csv') || file.name.endsWith('.json')) {
    const reader = new FileReader()
    reader.onload = (e) => {
      const text = e.target?.result as string
      if (!text) return
      if (file.name.endsWith('.csv')) {
        const firstLine = text.split('\n')[0]
        if (firstLine) {
          const headers = firstLine.split(',').map(h => h.trim().replace(/^"/, '').replace(/"$/, ''))
          importMapping.forEach((m) => {
            const match = headers.find(h => h.includes(m.label) || m.systemField.toLowerCase().includes(h.toLowerCase()))
            if (match) m.csvField = match
          })
          message.info(`检测到 ${headers.length} 列，请确认字段映射是否正确`)
        }
      }
    }
    reader.readAsText(file)
  }
  return false // 阻止自动上传
}

const handleImport = () => {
  uploadFile.value = null
  importMapping.forEach(m => { m.csvField = '' })
  importVisible.value = true
}

const handleImportConfirm = async () => {
  if (!uploadFile.value) {
    message.warning('请先上传文件')
    return
  }
  importLoading.value = true
  try {
    // 读取文件内容并解析为 SupplierDTO 数组
    const text = await uploadFile.value.text()
    let supplierList: any[]
    if (uploadFile.value.name.endsWith('.json')) {
      supplierList = JSON.parse(text)
    } else {
      // CSV 简单解析：首行表头，后续行数据
      const lines = text.split('\n').filter(Boolean)
      if (lines.length < 2) throw new Error('CSV 文件至少需要包含表头和一行数据')
      const headers = lines[0].split(',').map(h => h.trim())
      supplierList = lines.slice(1).map(line => {
        const values = line.split(',').map(v => v.trim())
        const item: Record<string, any> = {}
        headers.forEach((h, i) => { item[h] = values[i] || '' })
        return item
      })
    }
    if (!Array.isArray(supplierList) || supplierList.length === 0) {
      throw new Error('文件中未找到有效的供应商数据')
    }
    await supplierApi.importSuppliers(supplierList)
    console.warn('[供应商] 操作成功: 导入数据')
    message.success(`成功导入 ${supplierList.length} 条供应商数据`)
    importVisible.value = false
    fetchData()
    fetchStatistics()
  } catch (err: any) {
    console.warn('[供应商] 导入失败', err)
    message.error(err?.message || '导入失败，请检查文件格式')
  } finally {
    importLoading.value = false
  }
}

// ── 批量导出 ──
const handleBatchExport = () => {
  const rows = selectedRows.value.length > 0 ? selectedRows.value : dataSource.value
  const headers = ['供应商编码', '供应商名称', '等级', '综合评分', '积分', '合作状态', '门户状态', '联系人', '联系电话']
  const data = rows.map(row => [
    row.supplierCode || '', row.supplierName || '', row.supplierLevel || '',
    row.comprehensiveScore?.toFixed(1) || '0.0', row.totalPoints || 0,
    getStatusLabel(row.cooperationStatus), getPortalStatusLabel(row.portalStatus),
    row.contactPerson || '', row.contactPhone || ''
  ])
  exportCsv(headers, data, `供应商数据_${dayjs().format('YYYYMMDD_HHmm')}`)
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
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) { e.preventDefault(); debounceClick('refresh', fetchData)() }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); handleCreate() }
}

function handleParentCreate() { handleAdd() }

onMounted(() => {
  fetchData()
  fetchStatistics()
  startAutoRefresh()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('supplier:create', handleParentCreate)
})

onUnmounted(() => {
  stopAutoRefresh()
  handleSearchInput.cancel()
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('supplier:create', handleParentCreate)
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
/* ── 让 VxeTableList 填满剩余空间 ──────────────────────── */
.table-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* ── 空状态 ── */
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

.supplier-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.supplier-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.supplier-breadcrumb {
  font-size: 13px;
}
.supplier-breadcrumb :deep(li) {
  font-size: 13px;
}
.supplier-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.supplier-header-right {
  display: flex;
  align-items: center;
}

.list-update-timestamp {
  font-size: 12px; color: #bbb;
  white-space: nowrap; cursor: help;
  line-height: 32px; vertical-align: middle;
}
.list-update-timestamp :deep(.countdown-warning) {
  color: #faad14;
  font-weight: 600;
}
.action-cell-inner { flex-wrap: nowrap; }
.countdown-warning { color: #faad14; font-weight: 600; }

/* ── 骨架屏 ── */
@keyframes skeleton-pulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 1; }
}
.stat-skeleton {
  display: inline-block;
  width: 60px;
  height: 22px;
  border-radius: 4px;
  background: linear-gradient(90deg, rgba(255,255,255,0.3) 25%, rgba(255,255,255,0.6) 50%, rgba(255,255,255,0.3) 75%);
  background-size: 200% 100%;
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

/* 空行占位符 */




/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px; line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
:deep(.ant-input-number-sm input) { height: 26px; }

</style>
