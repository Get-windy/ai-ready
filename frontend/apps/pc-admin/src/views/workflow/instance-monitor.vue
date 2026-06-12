<template>
  <ErrorBoundary @error="handleError"><PageContainer full-height>
    <template #header>
      <div class="workflow-monitor-page-header">
        <div class="workflow-monitor-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>流程实例监控</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="workflow-monitor-page-header-title">流程实例监控</h2>
        </div>
        <div class="workflow-monitor-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', handleQuery)()">
            <ReloadOutlined /> 刷新
          </a-button>
          <span class="shortcut-hints">
            <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
          </span>
        </div>
      </div>
    </template>

    <div class="workflow-monitor">
      <!-- 统计卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ pagination.total }}</div>
            <div class="stat-card-label">实例总数</div>
          </div>
          <BranchesOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-running">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ runningCount }}</div>
            <div class="stat-card-label">运行中</div>
          </div>
          <LoadingOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-completed">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ completedCount }}</div>
            <div class="stat-card-label">已完成</div>
          </div>
          <CheckCircleOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-stopped">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ stoppedCount }}</div>
            <div class="stat-card-label">已终止</div>
          </div>
          <StopOutlined class="stat-card-icon" />
        </div>
      </div>

      <a-card title="流程实例监控">
        <!-- 查询表单 -->
        <a-form
          :model="queryForm"
          layout="inline"
          class="query-form"
        >
          <a-form-item label="流程名称">
            <a-input
              v-model:value="queryForm.processName"
              placeholder="请输入流程名称"
              allow-clear
              size="small"
            />
          </a-form-item>
          <a-form-item label="流程状态">
            <a-select
              v-model:value="queryForm.status"
              placeholder="请选择状态"
              allow-clear
              size="small"
              style="width: 120px"
            >
              <a-select-option value="running">
                运行中
              </a-select-option>
              <a-select-option value="completed">
                已完成
              </a-select-option>
              <a-select-option value="terminated">
                已终止
              </a-select-option>
              <a-select-option value="suspended">
                已挂起
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="开始时间">
            <a-range-picker
              v-model:value="queryForm.dateRange"
              size="small"
              value-format="YYYY-MM-DD"
            />
          </a-form-item>
          <a-form-item>
            <a-button
              type="primary"
              @click="handleQuery"
            >
              查询
            </a-button>
            <a-button
              style="margin-left: 8px"
              @click="handleReset"
            >
              重置
            </a-button>
          </a-form-item>
        </a-form>

        <!-- 数据表格 -->
        <div class="table-wrapper">
        <VxeTableList
          :columns="vxeColumns"
          :data-source="tableData"
          :loading="loading"
          :pagination="pagination"
          row-key="instanceId"
          :show-toolbar="false"
          :selectable="false"
          :show-add="false"
          :show-search="false"
          :show-export="false"
          :show-batch-delete="false"
          :min-empty-rows="12"
          @cell-dblclick="handleView"
          @page-change="handlePageChange"
        >
          <template #statusCell="{ record }">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusLabel(record.status) }}
            </a-tag>
          </template>
          <template #action="{ record }">
            <a-button v-permission="'workflow:instance:view'"
              type="link"
              size="small"
              @click="handleViewDetail(record)"
            >
              查看详情
            </a-button>
            <a-button v-permission="'workflow:instance:diagram'"
              type="link"
              size="small"
              @click="handleViewFlowChart(record)"
            >
              流程图
            </a-button>
            <a-dropdown v-if="record.status === 'running'">
              <a-button v-permission="'workflow:instance:intervene'"
                type="link"
                size="small"
              >
                流程干预 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu @click="(e) => handleIntervention(e.key as string, record)">
                  <a-menu-item key="terminate">
                    终止流程
                  </a-menu-item>
                  <a-menu-item key="suspend">
                    挂起流程
                  </a-menu-item>
                  <a-menu-item key="resume">
                    恢复流程
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </template>
          <template #empty>
            <div class="table-empty">
              <template v-if="hasError">
                <WarningOutlined class="table-empty-icon" style="color: #faad14" />
                <p class="table-empty-text">加载失败</p>
                <a-button type="primary" size="small" @click="debounceClick('refresh', handleQuery)()" class="table-empty-action">
                  <ReloadOutlined /> 重试
                </a-button>
              </template>
              <template v-else>
                <SearchOutlined v-if="hasActiveFilters" class="table-empty-icon" />
                <InboxOutlined v-else class="table-empty-icon" />
                <p v-if="hasActiveFilters" class="table-empty-text">
                  没有符合条件的流程实例，<a @click="handleReset">清除筛选</a>
                </p>
                <p v-else class="table-empty-text">暂无流程实例记录</p>
              </template>
            </div>
          </template>
        </VxeTableList>
        </div>
      </a-card>

      <!-- 详情对话框 -->
      <a-drawer
        v-model:open="detailVisible"
        title="流程实例详情"
        placement="right"
        width="80vw"
        :footer="null"
      >
        <template #extra>
          <a-button type="primary" size="small" @click="fetchDetail(detailRecord?.instanceId)" :loading="detailLoading" :disabled="!detailRecord">
            <template #icon><ReloadOutlined /></template>
          </a-button>
        </template>
        <a-skeleton active :loading="detailLoading" :paragraph="{ rows: 12 }">
          <template v-if="detailData">
            <a-descriptions bordered :column="2">
              <a-descriptions-item label="实例ID">
                {{ detailData.instanceId }}
              </a-descriptions-item>
              <a-descriptions-item label="流程名称">
                {{ detailData.processName }}
              </a-descriptions-item>
              <a-descriptions-item label="状态">
                <a-tag :color="getStatusColor(detailData.status)">
                  {{ getStatusLabel(detailData.status) }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="发起人">
                {{ detailData.initiator }}
              </a-descriptions-item>
              <a-descriptions-item label="开始时间">
                {{ detailData.startTime }}
              </a-descriptions-item>
              <a-descriptions-item label="结束时间">
                {{ detailData.endTime }}
              </a-descriptions-item>
              <a-descriptions-item
                label="当前节点"
                :span="2"
              >
                {{ detailData.currentNode }}
              </a-descriptions-item>
              <a-descriptions-item
                label="业务数据"
                :span="2"
              >
                <pre>{{ detailData.businessData }}</pre>
              </a-descriptions-item>
            </a-descriptions>
          </template>
          <a-result v-else-if="detailError" status="warning" title="加载失败" :sub-title="detailError">
            <template #extra>
              <a-button type="primary" size="small" @click="fetchDetail(detailRecord?.instanceId)">重试</a-button>
            </template>
          </a-result>
        </a-skeleton>
      </a-drawer>

      <!-- 流程图对话框 -->
      <a-modal
        v-model:open="flowChartVisible"
        title="流程图"
        :width="1000"
        :footer="null"
      >
        <div class="flow-chart-container">
          <a-spin :spinning="flowChartLoading" tip="流程图加载中...">
            <img v-if="flowChartImage" :src="flowChartImage" alt="流程图" style="max-width: 100%; max-height: 500px" />
            <a-empty v-else-if="!flowChartLoading" :description="flowChartError || '流程图加载中...'" />
          </a-spin>
        </div>
      </a-modal>
    </div>
  </PageContainer></ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { DownOutlined, BranchesOutlined, LoadingOutlined, CheckCircleOutlined, StopOutlined, SyncOutlined, ReloadOutlined, WarningOutlined, InboxOutlined, SearchOutlined } from '@ant-design/icons-vue'
import type { MenuInfo } from 'ant-design-vue/lib/menu/src/interface'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import request from '@/utils/request'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'

// ── 防抖工具 ────────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

// 查询表单
const queryForm = reactive({
  processName: '',
  status: undefined as string | undefined,
  dateRange: [] as any
})

// 表格数据
const tableData = ref<any[]>([])
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
const hasError = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null

function handleError(err: any) { console.warn('[工作流] 流程实例监控出错', err); hasError.value = true }
let countdownTimer: ReturnType<typeof setInterval> | null = null

// ── 统计数据 ────────────────────────────────────────────
const runningCount = computed(() => tableData.value.filter(r => r.status === 'running').length)
const completedCount = computed(() => tableData.value.filter(r => r.status === 'completed').length)
const stoppedCount = computed(() => tableData.value.filter(r => r.status === 'terminated' || r.status === 'suspended').length)

const hasActiveFilters = computed(() => {
  return queryForm.processName || queryForm.status || queryForm.dateRange?.length > 0
})

// 表格列定义
const vxeColumns = [
  { field: 'instanceId', title: '实例ID', width: 180 },
  { field: 'processName', title: '流程名称', minWidth: 150 },
  { field: 'status', title: '状态', width: 100, slotName: 'statusCell' },
  { field: 'currentNode', title: '当前节点', minWidth: 120 },
  { field: 'startTime', title: '开始时间', width: 180 },
  { field: 'endTime', title: '结束时间', width: 180 },
  { field: 'duration', title: '耗时', width: 100 },
  { field: 'action', title: '操作', width: 280, fixed: 'right', type: 'action' }
]

const loading = ref(false)

// 分页
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

// 详情对话框
const detailVisible = ref(false)
const detailRecord = ref<any>(null)
const detailData = ref<any>(null)
const detailLoading = ref(false)
const detailError = ref<string | null>(null)

// 流程图对话框
const flowChartVisible = ref(false)
const flowChartLoading = ref(false)
const flowChartImage = ref<string | null>(null)
const flowChartError = ref<string | null>(null)

// 查询
const handleQuery = async () => {
  loading.value = true
  hasError.value = false
  try {
    const res = await request.get('/workflow/instance/page', {
      params: {
        processName: queryForm.processName || undefined,
        status: queryForm.status,
        startDate: queryForm.dateRange?.[0],
        endDate: queryForm.dateRange?.[1],
        pageNum: pagination.current,
        pageSize: pagination.pageSize
      }
    })
    const records = res.data?.records || res.records || []
    const total = res.data?.total || res.total || 0
    tableData.value = records
    pagination.total = total
  } catch (err: any) {
    hasError.value = true
    tableData.value = []
    pagination.total = 0
    console.warn('[工作流] 查询流程实例失败', err)
    message.error('查询失败')
  } finally {
    loading.value = false
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

// 重置
const handleReset = () => {
  queryForm.processName = ''
  queryForm.status = undefined
  queryForm.dateRange = []
  pagination.current = 1
  handleQuery()
}

// 表格变化
const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
  handleQuery()
}

// 查看详情
const handleView = (record: any) => {
  const row = record?.row ?? record
  handleViewDetail(row)
}

const handleViewDetail = (record: any) => {
  detailRecord.value = record
  detailVisible.value = true
  fetchDetail(record.instanceId)
}

async function fetchDetail(instanceId: string) {
  detailLoading.value = true
  detailError.value = null
  try {
    const res = await request.get('/workflow/instance/detail', { params: { instanceId } })
    detailData.value = res.data || res
  } catch (err: any) {
    console.warn('[工作流] 获取流程实例详情失败', err)
    detailError.value = err?.message || '获取流程实例详情失败'
    detailData.value = null
  } finally {
    detailLoading.value = false
  }
}

// 查看流程图
const handleViewFlowChart = async (record: any) => {
  flowChartVisible.value = true
  flowChartLoading.value = true
  flowChartError.value = null
  flowChartImage.value = null
  try {
    // 尝试从后端获取流程图（支持 BPMN SVG 或图片 URL）
    const res = await request.get('/workflow/instance/diagram', {
      params: { instanceId: record.instanceId }
    })
    if (res.data?.svg) {
      flowChartImage.value = 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(res.svg)))
    } else if (res.data?.imageUrl) {
      flowChartImage.value = res.imageUrl
    } else {
      flowChartError.value = '暂无可用的流程图'
    }
  } catch (err) {
    console.warn('[工作流] 流程图加载失败', err)
    flowChartError.value = '流程图加载失败'
  } finally {
    flowChartLoading.value = false
  }
}

// 流程干预
const handleIntervention = async (command: string, record: any) => {
  const actions: Record<string, string> = {
    terminate: '终止',
    suspend: '挂起',
    resume: '恢复'
  }

  Modal.confirm({
    title: '确认操作',
    content: `确定要${actions[command]}流程"${record.processName}"吗？`,
    onOk: async () => {
      try {
        await request.post(`/workflow/instance/${record.instanceId}/intervene`, {
          action: command
        })
        console.warn('[工作流] 操作成功: 流程干预')
        message.success(`${actions[command]}成功`)
        handleQuery()
      } catch (err) {
        console.warn('[工作流] 流程干预失败', err)
        message.error(`${actions[command]}失败`)
      }
    }
  })
}

// 获取状态颜色
const getStatusColor = (status: string) => {
  const colors: Record<string, string> = {
    running: 'success',
    completed: 'default',
    terminated: 'error',
    suspended: 'warning'
  }
  return colors[status] || 'default'
}

// 获取状态标签
const getStatusLabel = (status: string) => {
  const labels: Record<string, string> = {
    running: '运行中',
    completed: '已完成',
    terminated: '已终止',
    suspended: '已挂起'
  }
  return labels[status] || status
}

// 初始加载
function handleParentCreate() { handleQuery() }

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) {
    e.preventDefault()
    debounceClick('refresh', handleQuery)()
  }
}

onMounted(() => {
  handleQuery()
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    handleQuery()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
  window.addEventListener('workflow:create', handleParentCreate)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  window.removeEventListener('workflow:create', handleParentCreate)
  document.removeEventListener('keydown', handleKeydown)
})

defineExpose({ handleQuery })
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

.workflow-monitor-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.workflow-monitor-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.workflow-monitor-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.workflow-monitor-page-header-right {
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

.workflow-monitor {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
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
.stat-running { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-completed { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }
.stat-stopped { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }

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

.query-form {
  margin-bottom: 20px;
}

.flow-chart-container {
  height: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 4px;
}

pre {
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 200px;
  overflow: auto;
}





/* 响应式 */
@media (max-width: 768px) {
  .stat-cards { flex-wrap: wrap; }
  .stat-card { flex: 1 1 45%; min-width: 120px; }
}

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

/* ── 空状态 ──────────────────────── */
.empty-state-wrapper { display: flex; flex-direction: column; align-items: center; padding: 48px 0; }
.empty-state-icon { font-size: 48px; color: #d9d9d9; }
.empty-state-text { color: #999; margin-top: 12px; }
.empty-state-action { margin-top: 12px; }

/* ── vxe-table 表头 2px 边框 ─────── */
:deep(.vxe-table .vxe-header--row) { border-top: 2px solid #e8e8e8; }

</style>
