<template>
  <ErrorBoundary @error="handleError">
  <PageContainer full-height>
    <template #header>
      <div class="log-page-header">
        <div class="log-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>操作日志</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="log-page-header-title">操作日志</h2>
        </div>
        <div class="log-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', fetchData)()">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <div class="log-management">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pagination.total }}</div>
            <div class="stat-card-label">日志总数</div>
          </div>
          <FileTextOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-success">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ successCount }}</div>
            <div class="stat-card-label">成功操作</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-error">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ errorCount }}</div>
            <div class="stat-card-label">失败操作</div>
          </div>
          <CloseCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-slow">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ slowCount }}</div>
            <div class="stat-card-label">慢请求</div>
          </div>
          <ClockCircleOutlined class="stat-card-icon" />
        </div>
      </div>

      <a-skeleton active v-if="loading && tableData.length === 0" :paragraph="{ rows: 8 }" style="padding: 24px;" />

      <VxeTableList
        ref="tableRef"
        :columns="vxeColumns"
        :data-source="tableDataSource"
        :loading="loading"
        :pagination="pagination"
        :row-key="'id'"
        :min-empty-rows="12"
        :filter-fields="filterFields"
        :show-search="false"
        :show-add="false"
        :show-edit="false"
        :show-delete="false"
        :show-batch-delete="false"
        :show-export="true"
        :selectable="false"
        @refresh="debounceClick('refresh', fetchData)"
        @page-change="handlePageChange"
        @cell-dblclick="handleView"
        @filter-change="handleFilterChange"
        @export="handleExport"
      >
        <template #toolbar-actions>
          <a-button danger v-permission="'log:audit:delete'" @click="handleClearLogs">
            <template #icon><DeleteOutlined /></template>
            清空日志
          </a-button>
        </template>

        <template #statusCell="{ record }">
          <a-tag :color="record.status === 0 ? 'success' : 'error'">
            {{ record.status === 0 ? '成功' : '失败' }}
          </a-tag>
        </template>
        <template #costTimeCell="{ record }">
          <span v-if="record.costTime > 1000" style="color: #ff4d4f">
            {{ record.costTime }}ms
          </span>
          <span v-else-if="record.costTime > 500" style="color: #faad14">
            {{ record.costTime }}ms
          </span>
          <span v-else>
            {{ record.costTime }}ms
          </span>
        </template>

        <template #empty>
          <a-empty v-if="!hasError" description="暂无数据" />
          <a-result v-else status="error" title="数据加载失败">
            <template #extra>
              <a-button type="primary" @click="debounceClick('refresh', fetchData)()">
                <template #icon><ReloadOutlined /></template>
                重新加载
              </a-button>
            </template>
          </a-result>
        </template>

        <template #action="{ record }">
          <a-button type="link" size="small" @click="handleDetail(record)">
            详情
          </a-button>
        </template>
      </VxeTableList>

      <!-- 详情抽屉 -->
      <a-drawer
        v-model:open="detailVisible"
        title="操作日志详情"
        width="700px"
        :destroy-on-close="true"
      >
        <a-descriptions :column="1" bordered size="small" v-if="currentLog">
          <a-descriptions-item label="模块">{{ currentLog.module }}</a-descriptions-item>
          <a-descriptions-item label="操作类型">{{ currentLog.operationType }}</a-descriptions-item>
          <a-descriptions-item label="操作描述">{{ currentLog.description }}</a-descriptions-item>
          <a-descriptions-item label="请求URL">{{ currentLog.requestUrl }}</a-descriptions-item>
          <a-descriptions-item label="请求方法">
            <a-tag :color="getMethodColor(currentLog.requestMethod)">
              {{ currentLog.requestMethod }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="操作人">{{ currentLog.operatorName }}</a-descriptions-item>
          <a-descriptions-item label="IP地址">{{ currentLog.ipAddress }}</a-descriptions-item>
          <a-descriptions-item label="操作时间">{{ currentLog.operationTime }}</a-descriptions-item>
          <a-descriptions-item label="耗时">{{ currentLog.costTime }}ms</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="currentLog.status === 0 ? 'success' : 'error'">
              {{ currentLog.status === 0 ? '成功' : '失败' }}
            </a-tag>
          </a-descriptions-item>
        </a-descriptions>
        <a-divider v-if="currentLog">请求/响应详情</a-divider>
        <a-tabs v-if="currentLog">
          <a-tab-pane key="request" tab="请求参数">
            <pre class="json-content">{{ formatJson(currentLog.requestParams) }}</pre>
          </a-tab-pane>
          <a-tab-pane key="response" tab="响应结果">
            <pre class="json-content">{{ formatJson(currentLog.responseResult) }}</pre>
          </a-tab-pane>
        </a-tabs>
      </a-drawer>
    </div>
  </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import { message, Modal } from 'ant-design-vue'
import {
  WarningOutlined,
  DeleteOutlined,
  FileTextOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  SyncOutlined,
  ReloadOutlined
} from '@ant-design/icons-vue'
import VxeTableList, { type FilterField } from '@/components/VxeTableList/VxeTableList.vue'
import { logApi, type OperationLog } from '@/api/log'
import PageContainer from '@/components/PageContainer/PageContainer.vue'

// ── 防抖工具 ────────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

// 搜索表单
const searchForm = reactive({
  module: undefined as string | undefined,
  operationType: undefined as string | undefined,
  operatorName: '',
  dateRange: undefined as [string, string] | undefined
})

const moduleOptions = ref<string[]>([])
const operationTypeOptions = ref<{ label: string; value: string }[]>([])

// 表格数据
const tableData = ref<OperationLog[]>([])
const loading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
const hasError = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ── 统计数据 ────────────────────────────────────────────
const successCount = computed(() => tableData.value.filter(r => r.status === 0).length)
const errorCount = computed(() => tableData.value.filter(r => r.status === 1).length)
const slowCount = computed(() => tableData.value.filter(r => r.costTime > 1000).length)

// ── 数据源 ────────────────────────────────────────────
const tableDataSource = tableData

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  pageSizeOptions: ['10', '20', '50', '100'],
  showTotal: (total: number) => `共 ${total} 条`
})

// 表格列定义
const vxeColumns = computed(() => [
  { field: 'module', title: '模块', width: 120, showOverflow: 'tooltip' },
  { field: 'operationType', title: '操作类型', width: 100 },
  { field: 'description', title: '操作描述', width: 200, showOverflow: 'tooltip' },
  { field: 'requestUrl', title: '请求URL', width: 200, showOverflow: 'tooltip' },
  { field: 'requestMethod', title: '请求方法', width: 100 },
  { field: 'operatorName', title: '操作人', width: 100 },
  { field: 'ipAddress', title: 'IP地址', width: 130 },
  { field: 'operationTime', title: '操作时间', width: 160 },
  { field: 'costTime', title: '耗时(ms)', width: 100, slotName: 'costTimeCell' },
  { field: 'status', title: '状态', width: 80, slotName: 'statusCell' },
  { type: 'action', title: '操作', width: 80, fixed: 'right' }
])

// 筛选字段
const moduleSelectOptions = computed(() =>
  moduleOptions.value.map(m => ({ label: m, value: m }))
)

const filterFields = computed<FilterField[]>(() => [
  { key: 'module', label: '模块', type: 'select', options: moduleSelectOptions.value, placeholder: '请选择模块' },
  { key: 'operationType', label: '操作类型', type: 'select', options: operationTypeOptions.value, placeholder: '请选择操作类型' },
  { key: 'operatorName', label: '操作人', type: 'input', placeholder: '请输入操作人' },
  { key: 'dateRange', label: '日期范围', type: 'dateRange' },
])

// 详情弹窗
const detailVisible = ref(false)
const currentLog = ref<OperationLog | null>(null)

// 数据加载
const fetchData = async () => {
  loading.value = true
  hasError.value = false
  try {
    const params: any = {
      ...searchForm,
      pageNum: pagination.current,
      pageSize: pagination.pageSize
    }
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = searchForm.dateRange[0]
      params.endDate = searchForm.dateRange[1]
    }
    delete params.dateRange

    const res = await logApi.getPage(params)
    if (res.data) {
      tableData.value = res.records
      pagination.total = res.total
    }
  } catch (error) {
    hasError.value = true
    tableData.value = []
    pagination.total = 0
    console.warn('[操作日志] 加载日志数据失败')
    message.error('加载数据失败')
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

const fetchModules = async () => {
  try {
    const res = await logApi.getModules()
    if (res.data) {
      moduleOptions.value = res.data
    }
  } catch (err) {
    console.warn('[系统管理] 加载模块列表失败', err)
  }
    message.error('加载模块列表失败')
}

const fetchOperationTypes = async () => {
  try {
    const res = await logApi.getOperationTypes()
    if (res.data) {
      operationTypeOptions.value = res.map((t: string) => ({ label: t, value: t }))
    }
  } catch (err) {
    console.warn('[系统管理] 加载操作类型列表失败', err)
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  fetchData()
}

const handleReset = () => {
  Object.assign(searchForm, {
    module: undefined,
    operationType: undefined,
    operatorName: '',
    dateRange: undefined
  })
  handleSearch()
}

// 筛选变化
const handleFilterChange = (filters: Record<string, any>) => {
  if (Object.keys(filters).length === 0) {
    Object.assign(searchForm, { module: undefined, operationType: undefined, operatorName: '', dateRange: undefined })
  } else {
    Object.assign(searchForm, filters)
  }
  pagination.current = 1
  fetchData()
}

// 分页变化
const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  fetchData()
}

// 详情
const handleDetail = async (record: OperationLog) => {
  detailVisible.value = true
  try {
    const res = await logApi.getById(record.id)
    if (res.data) {
      currentLog.value = res.data
    }
  } catch (err) {
    console.warn('[系统管理] 获取日志详情失败', err)
    currentLog.value = record
  }
}

// 清空日志
const handleClearLogs = () => {
  Modal.confirm({
    title: '确认清空',
    content: '确定要清空所有操作日志吗？此操作不可恢复。',
    okType: 'danger',
    async onOk() {
      try {
        await logApi.clearLogs()
        message.success('日志已清空')
        fetchData()
      } catch (err) {
        console.warn('[系统管理] 清空日志失败', err)
        message.error('清空失败')
      }
    }
  })
}

// 导出
const handleExport = async () => {
  try {
    message.loading({ content: '正在导出...', key: 'export' })
    const res = await logApi.exportLogs(searchForm as any)
    const blob = res as any
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `操作日志_${new Date().toISOString().slice(0, 10)}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    message.success({ content: '导出成功', key: 'export' })
  } catch (err) {
    message.error({ content: '导出失败', key: 'export' })
    console.warn('[系统管理] 导出日志失败', err)
  }
}

// 辅助函数
const getMethodColor = (method: string) => {
  const colors: Record<string, string> = {
    GET: 'green',
    POST: 'blue',
    PUT: 'orange',
    PATCH: 'purple',
    DELETE: 'red'
  }
  return colors[method] || 'default'
}

const formatJson = (jsonStr: string | undefined) => {
  if (!jsonStr) return '无数据'
  try {
    return JSON.stringify(JSON.parse(jsonStr), null, 2)
  } catch (err) {
    return jsonStr
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) {
    e.preventDefault()
    debounceClick('refresh', fetchData)()
  }
}

onMounted(() => {
  fetchData()
  fetchModules()
  fetchOperationTypes()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    fetchData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
})

defineExpose({ handleQuery: fetchData })

function handleError(err: any) { console.warn('[ErrorBoundary]', err) }
// 查看详情
const handleView = (record: any) => {}
</script>

<style scoped>
.log-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.log-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.log-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.log-page-header-right {
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

.log-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
  min-height: 0;
}

.log-management > :deep(.vxe-table-list-container) {
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

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-success { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-error { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }
.stat-slow { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }

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

.json-content {
  background: #f5f5f5;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 12px;
  max-height: 300px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}





/* ── VxeTable 表头边框线 2px ─────────────────────────── */
.log-management :deep(.vxe-table .vxe-header--row th) {
  border-bottom: 2px solid #e8e8e8 !important;
}
.log-management :deep(.vxe-table .vxe-header--row th:not(:last-child)) {
  border-right: 1px solid #e8e8e8 !important;
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
.log-management :deep(.ant-input-sm),
.log-management :deep(.ant-input-number-sm),
.log-management :deep(.ant-select-single.ant-select-sm .ant-select-selector),
.log-management :deep(.ant-picker-small),
.log-management :deep(.ant-btn-sm) {
  height: 28px; line-height: 28px;
}
.log-management :deep(.ant-select-single.ant-select-sm .ant-select-selector) { line-height: 26px; }
.log-management :deep(.ant-input-number-sm input) { height: 26px; }

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

</style>
