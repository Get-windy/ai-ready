<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="inventory-page-header">
        <div class="inventory-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>固定资产</a-breadcrumb-item>
            <a-breadcrumb-item>盘点管理</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="inventory-page-header-title">盘点管理</h2>
        </div>
        <div class="inventory-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <PrintButton business-type="fixed_asset_inventory" button-type="link" button-size="small" tooltip="打印盘点记录" />
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)()" v-permission="'erp:fixed-asset:inventory:list'">
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

    <div class="inventory-list-page">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-pending">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statusCounts.pending }}</div>
            <div class="stat-card-label">待盘点</div>
          </div>
          <ClockCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-completed">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statusCounts.completed }}</div>
            <div class="stat-card-label">已完成</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-mismatch">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ resultCounts.mismatch }}</div>
            <div class="stat-card-label">差异记录</div>
          </div>
          <ExclamationCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-count">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pagination.total }}</div>
            <div class="stat-card-label">盘点记录数</div>
          </div>
          <FileTextOutlined class="stat-card-icon" />
        </div>
      </div>

      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableDataSource"
        :loading="loading"
        :pagination="pagination"
        :table-key="'fixed-asset-inventory-list'"
        :filter-fields="filterFields"
        :selectable="true"
        add-text="新增盘点"
        add-permission="erp:fixed-asset:inventory:create"
        delete-permission="erp:fixed-asset:inventory:delete"
        @add="showCreateModal"
        @edit="editRecord"
        @cell-dblclick="viewDetail"
        :min-empty-rows="12"
        @refresh="debounceClick('refresh', fetchData)"
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
        <template #batch-actions="{ selectedRowKeys }: any">
          <span class="batch-info">已选择 {{ selectedRowKeys.length }} 项</span>
        </template>
        <template #empty>
          <div v-if="hasError" class="table-empty table-empty-error">
            <WarningOutlined class="table-empty-icon table-empty-icon-error" />
            <p class="table-empty-text">数据加载失败，请重试</p>
            <a-button size="small" @click="debounceClick('refresh', fetchData)()">
              <template #icon><ReloadOutlined /></template>
              重试
            </a-button>
          </div>
          <div v-else class="table-empty">
            <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
            <InboxOutlined v-else class="table-empty-icon" />
            <p v-if="hasActiveFilters" class="table-empty-text">
              没有符合条件的盘点记录，<a @click="handleResetFilters">清除筛选</a>
            </p>
            <p v-else class="table-empty-text">
              暂无盘点记录，点击右上角「新增盘点」开始创建
            </p>
          </div>
        </template>
        <template #inventoryNoCell="{ record }">
          <a @click="viewDetail(record)" class="inventory-no">{{ record.inventoryNo }}</a>
        </template>
        <template #statusCell="{ record }">
          <a-tag :color="record.status === 'completed' ? 'green' : 'orange'">
            {{ record.status === 'completed' ? '已完成' : '待盘点' }}
          </a-tag>
        </template>
        <template #checkResultCell="{ record }">
          <a-tag :color="resultColorMap[record.checkResult]">
            {{ resultMap[record.checkResult] || record.checkResult }}
          </a-tag>
        </template>

        <template #action="{ record }">
          <a-space :size="4">
            <a-tooltip title="查看详情">
              <a-button type="link" size="small" @click="viewDetail(record)">
                <template #icon><EyeOutlined /></template>
              </a-button>
            </a-tooltip>
            <a-tooltip v-if="record.status === 'pending'" title="编辑">
              <a-button type="link" size="small" @click="editRecord(record)" v-permission="'erp:fixed-asset:inventory:update'">
                <template #icon><EditOutlined /></template>
              </a-button>
            </a-tooltip>
            <PrintButton
              template-type="inventory"
              :business-id="record.id"
              business-type="fixed_asset_inventory"
              button-text=""
              button-size="small"
              button-type="link"
              tooltip="打印"
            />
            <a-dropdown trigger="click">
              <a-button type="link" size="small" class="action-more-btn">
                <template #icon><EllipsisOutlined /></template>
              </a-button>
              <template #overlay>
                <a-menu @click="({ key }: any) => handleActionMenuClick(key as string, record)">
                  <a-menu-item v-if="record.status === 'pending'" key="complete" v-permission="'erp:fixed-asset:inventory:update'">
                    <CheckCircleOutlined /> 完成盘点
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </a-space>
        </template>
      </VxeTableList>

      <!-- Create/Edit Modal -->
      <FullScreenDetail
        :visible="modalVisible"
        :title="isEdit ? '编辑盘点记录' : '新增盘点记录'"
        :save-loading="modalLoading"
        :show-save-and-new="!isEdit"
        @save="handleModalOk"
        @close="handleFormClose"
        @save-and-new="handleFormSaveAndNew"
      >
        <a-form :model="formData" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
          <a-form-item label="资产ID" required>
            <a-input-number v-model:value="formData.assetId" :min="1" style="width: 100%" size="small" />
          </a-form-item>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="资产编码">
                <a-input v-model:value="formData.assetCode" placeholder="资产编码" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="资产名称">
                <a-input v-model:value="formData.assetName" placeholder="资产名称" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="盘点日期">
                <a-date-picker v-model:value="formData.inventoryDate" style="width: 100%" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="部门">
                <a-input v-model:value="formData.departmentName" placeholder="部门名称" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="预期位置">
                <a-input v-model:value="formData.expectedLocation" placeholder="预期位置" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="实际位置">
                <a-input v-model:value="formData.actualLocation" placeholder="实际位置" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="预期状态">
                <a-select v-model:value="formData.expectedStatus" placeholder="预期状态" size="small">
                  <a-select-option value="in_use">使用中</a-select-option>
                  <a-select-option value="idle">闲置</a-select-option>
                  <a-select-option value="maintenance">维修中</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="实际状态">
                <a-select v-model:value="formData.actualStatus" placeholder="实际状态" size="small">
                  <a-select-option value="in_use">使用中</a-select-option>
                  <a-select-option value="idle">闲置</a-select-option>
                  <a-select-option value="maintenance">维修中</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item label="预期保管人">
                <a-input v-model:value="formData.expectedCustodian" placeholder="预期保管人" size="small" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="实际保管人">
                <a-input v-model:value="formData.actualCustodian" placeholder="实际保管人" size="small" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="盘点结果">
            <a-select v-model:value="formData.checkResult" placeholder="选择结果" size="small">
              <a-select-option value="consistent">一致</a-select-option>
              <a-select-option value="mismatch">不符</a-select-option>
              <a-select-option value="missing">盘亏</a-select-option>
              <a-select-option value="surplus">盘盈</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="备注">
            <a-textarea v-model:value="formData.remark" :rows="2" size="small" />
          </a-form-item>
        </a-form>
      </FullScreenDetail>
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { onBeforeRouteLeave } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  SearchOutlined, InboxOutlined, EyeOutlined, EditOutlined,
  ClockCircleOutlined, CheckCircleOutlined, ExclamationCircleOutlined,
  FileTextOutlined, EllipsisOutlined,
  SyncOutlined, ReloadOutlined, WarningOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { inventoryApi } from '@/api/fixed-asset'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import FullScreenDetail from '@/components/FullScreenDetail/FullScreenDetail.vue'

const loading = ref(false)
const modalVisible = ref(false)
const modalLoading = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const tableData = ref<any[]>([])
const tableRef = ref()
const lastUpdated = ref('')
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
const hasError = ref(false)
const selectedRowKeys = ref<number[]>([])
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const hasActiveFilters = computed(() => {
  return Object.values(searchFilters).some(v => v !== undefined && v !== null && v !== '')
})

const searchFilters = reactive<Record<string, any>>({})

// ── 防抖工具 ────────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

// ── 表单数据 ────────────────────────────────────────────
const formData = reactive({
  assetId: undefined as number | undefined,
  assetCode: '',
  assetName: '',
  inventoryDate: undefined as any,
  departmentId: '',
  departmentName: '',
  expectedLocation: '',
  actualLocation: '',
  expectedStatus: 'in_use',
  actualStatus: 'in_use',
  expectedCustodian: '',
  actualCustodian: '',
  checkResult: 'consistent',
  remark: '',
})

// ── 表单脏检测 ──────────────────────────────────────────
const initialFormSnapshot = ref('')
let watchReady = false
const formDirty = computed(() => {
  if (!watchReady) return false
  return JSON.stringify(formData) !== initialFormSnapshot.value
})
function saveFormSnapshot() { initialFormSnapshot.value = JSON.stringify(formData) }

// ── 离开守卫 ────────────────────────────────────────────
onBeforeRouteLeave((to, from, next) => {
  if (!formDirty.value) { next(); return }
  Modal.confirm({
    title: '确认离开',
    content: '您有未保存的修改，确定要离开吗？',
    okText: '离开',
    cancelText: '继续编辑',
    onOk: () => next(),
    onCancel: () => next(false),
  })
})

const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})

function handleSelectionChange(keys: number[]) {
  selectedRowKeys.value = keys
}

// ── 统计数据 ────────────────────────────────────────────
const statusCounts = computed(() => {
  const pending = tableData.value.filter(r => r.status === 'pending').length
  const completed = tableData.value.filter(r => r.status === 'completed').length
  return { pending, completed }
})

const resultCounts = computed(() => {
  const mismatch = tableData.value.filter(r => r.checkResult === 'mismatch' || r.checkResult === 'missing' || r.checkResult === 'surplus').length
  return { mismatch }
})

// ── 数据源 ────────────────────────────────────────────
const tableDataSource = tableData

const vxeColumns = computed(() => [
  { field: 'inventoryNo', title: '盘点单号', width: 150, slotName: 'inventoryNoCell' },
  { field: 'assetCode', title: '资产编码', width: 120 },
  { field: 'assetName', title: '资产名称', width: 160 },
  { field: 'inventoryDate', title: '盘点日期', width: 120 },
  { field: 'departmentName', title: '部门', width: 120 },
  { field: 'expectedLocation', title: '预期位置', width: 130 },
  { field: 'actualLocation', title: '实际位置', width: 130 },
  { field: 'expectedStatus', title: '预期状态', width: 100 },
  { field: 'actualStatus', title: '实际状态', width: 100 },
  { field: 'checkResult', title: '盘点结果', width: 100, slotName: 'checkResultCell' },
  { field: 'status', title: '状态', width: 80, slotName: 'statusCell' },
  { type: 'action', title: '操作', width: 120, fixed: 'right' },
])

const filterFields = [
  { key: 'inventoryNo', label: '盘点单号', type: 'input' as const, placeholder: '盘点单号' },
  { key: 'status', label: '状态', type: 'select' as const, options: [
    { label: '待盘点', value: 'pending' },
    { label: '已完成', value: 'completed' },
  ]},
  { key: 'checkResult', label: '盘点结果', type: 'select' as const, options: [
    { label: '一致', value: 'consistent' },
    { label: '不符', value: 'mismatch' },
    { label: '盘亏', value: 'missing' },
    { label: '盘盈', value: 'surplus' },
  ]},
]

const resultMap: Record<string, string> = {
  consistent: '一致', mismatch: '不符', missing: '盘亏', surplus: '盘盈',
}
const resultColorMap: Record<string, string> = {
  consistent: 'green', mismatch: 'orange', missing: 'red', surplus: 'blue',
}

function handleParentCreate() {
  showCreateModal()
}

// ── 键盘快捷键 ──────────────────────────────────────────
function handleKeydown(e: KeyboardEvent) {
  const tag = (e.target as HTMLElement)?.tagName
  const isInput = tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT'
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !isInput) {
    e.preventDefault()
    debounceClick('refresh', fetchData)()
  }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n' && !isInput) {
    e.preventDefault()
    showCreateModal()
  }
}

onMounted(() => {
  fetchData()
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('fixed-asset:create', handleParentCreate)
  window.addEventListener('fixed-asset:refresh', fetchData)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('fixed-asset:create', handleParentCreate)
  window.removeEventListener('fixed-asset:refresh', fetchData)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchData })

async function fetchData() {
  loading.value = true
  hasError.value = false
  const params: any = {
    page: pagination.current - 1,
    size: pagination.pageSize,
  }
  if (searchFilters.inventoryNo) params.inventoryNo = searchFilters.inventoryNo
  if (searchFilters.status) params.status = searchFilters.status
  if (searchFilters.checkResult) params.checkResult = searchFilters.checkResult

  try {
    const res = await inventoryApi.getPage(params)
    tableData.value = res.data?.content || res.data?.records || []
    pagination.total = res.data?.totalElements || res.data?.total || 0
    lastUpdated.value = new Date().toISOString()
  } catch {
    hasError.value = true
    tableData.value = []
    pagination.total = 0
    console.warn('[盘点管理] 加载盘点数据失败')
    message.error('加载盘点数据失败')
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handlePageChange(page: number, size: number) {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

function handleFilterChange(filters: Record<string, any>) {
  Object.assign(searchFilters, filters)
  pagination.current = 1
  fetchData()
}

function showCreateModal() {
  isEdit.value = false
  editId.value = null
  Object.assign(formData, {
    assetId: undefined,
    assetCode: '',
    assetName: '',
    inventoryDate: undefined,
    departmentId: '',
    departmentName: '',
    expectedLocation: '',
    actualLocation: '',
    expectedStatus: 'in_use',
    actualStatus: 'in_use',
    expectedCustodian: '',
    actualCustodian: '',
    checkResult: 'consistent',
    remark: '',
  })
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

function editRecord(record: any) {
  isEdit.value = true
  editId.value = record.id
  Object.assign(formData, record)
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

function viewDetail(record: any) {
  isEdit.value = true
  editId.value = record.id
  Object.assign(formData, record)
  modalVisible.value = true
  nextTick(() => { saveFormSnapshot(); watchReady = true })
}

function handleModalOk() {
  modalLoading.value = true
  const apiCall = isEdit.value && editId.value
    ? inventoryApi.update(editId.value, formData)
    : inventoryApi.create(formData)

  apiCall.then(() => {
    message.success(isEdit.value ? '更新成功' : '创建成功')
    modalVisible.value = false
    fetchData()
  }).catch((err: any) => {
    console.warn('[盘点管理] 操作失败', err)
    message.error(err.message || '操作失败')
  }).finally(() => {
    modalLoading.value = false
  })
}

function handleFormClose() {
  if (formDirty.value) {
    Modal.confirm({
      title: '确认关闭',
      content: '您有未保存的修改，确定要关闭吗？',
      okText: '确定',
      cancelText: '取消',
      onOk: () => { modalVisible.value = false },
    })
  } else {
    modalVisible.value = false
  }
}

function handleFormSaveAndNew() {
  handleModalOk()
}

function handleResetFilters() {
  Object.keys(searchFilters).forEach(k => { searchFilters[k] = undefined })
  pagination.current = 1; fetchData()
}

function handleActionMenuClick(key: string, record: any) {
  switch (key) {
    case 'complete':
      Modal.confirm({
        title: '完成盘点',
        content: `确认完成盘点记录 "${record.inventoryNo}"？`,
        okText: '确认',
        centered: true,
        onOk: async () => {
          try {
            await inventoryApi.update(record.id, { ...record, status: 'completed' })
            message.success('盘点已完成')
            fetchData()
          } catch (err: any) {
            message.error(err.message || '操作失败')
          }
        }
      })
      break
  }
}

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
</script>

<style scoped>
.inventory-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.inventory-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.inventory-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.inventory-page-header-right {
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

.inventory-list-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.inventory-list-page :deep(.vxe-table) {
  flex: 1;
  min-height: 0;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
}

.stat-pending { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-completed { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-mismatch { background: linear-gradient(135deg, #fff1f0 0%, #ffa39e 100%); }
.stat-count { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
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

.inventory-no {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-weight: 500;
}

.action-more-btn {
  padding: 0 4px;
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

/* ── FullScreenDetail 内部紧凑样式 ────────────────────── */
:deep(.fsd-body .ant-form-item) { margin-bottom: 8px; }
:deep(.fsd-body .ant-form-item-label > label) { font-size: 12px; height: 28px; }
:deep(.fsd-body .ant-input), :deep(.fsd-body .ant-input-number), :deep(.fsd-body .ant-select), :deep(.fsd-body .ant-picker), :deep(.fsd-body .ant-cascader-picker) { font-size: 12px; }
:deep(.fsd-body .ant-input-number-input) { font-size: 12px; }
:deep(.fsd-body .ant-select-selection-item) { font-size: 12px; }
:deep(.fsd-body .ant-btn) { font-size: 12px; }

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

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

/* ── vxe-table 表头边框 ──────────────────────── */
:deep(.vxe-table--header-border) {
  border-bottom: 2px solid #e8e8e8 !important;
}

/* ── 空状态容器 ──────────────────────── */
:deep(.empty-state-wrapper) {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  min-height: 200px;
}

</style>
