<template>
  <PageContainer full-height>
    <template #header>
      <div class="workflow-analysis-page-header">
        <div class="workflow-analysis-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>流程分析</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="workflow-analysis-page-header-title">流程分析</h2>
        </div>
        <div class="workflow-analysis-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="debounceClick('refresh', handleRefresh)()">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <div class="page-content">
    <div class="workflow-analysis">
      <!-- 统计卡片 -->
      <a-row :gutter="16">
        <a-col
          v-for="card in statisticCards"
          :key="card.title"
          :span="6"
        >
          <a-card>
            <a-statistic
              :title="card.title"
              :value="card.value"
            >
              <template #suffix>
                <span class="suffix">{{ card.suffix }}</span>
              </template>
            </a-statistic>
          </a-card>
        </a-col>
      </a-row>

      <a-row
        :gutter="16"
        style="margin-top: 16px"
      >
        <!-- 流程耗时统计 -->
        <a-col :span="12">
          <a-card>
            <template #title>
              <div class="card-header">
                <span>流程耗时统计</span>
                <a-button
                  type="link"
                  @click="handleRefresh"
                >
                  刷新
                </a-button>
              </div>
            </template>
            <VxeTableList
              :columns="processDurationVxeColumns"
              :data-source="processDurationData"
              :pagination="false"
              :show-toolbar="false"
              :selectable="false"
              :show-add="false"
              :show-search="false"
              :show-export="false"
              :show-batch-delete="false"
            >
              <template #empty>
                <div class="table-empty">
                  <template v-if="hasError">
                    <WarningOutlined class="table-empty-icon" style="color: #faad14" />
                    <p class="table-empty-text">加载失败</p>
                    <a-button type="primary" size="small" @click="handleRefresh" class="table-empty-action">
                      <ReloadOutlined /> 重试
                    </a-button>
                  </template>
                  <template v-else>
                    <InboxOutlined class="table-empty-icon" />
                    <p class="table-empty-text">暂无数据</p>
                  </template>
                </div>
              </template>
            </VxeTableList>
          </a-card>
        </a-col>

        <!-- 节点耗时分析 -->
        <a-col :span="12">
          <a-card>
            <template #title>
              <div class="card-header">
                <span>节点耗时分析</span>
                <a-select
                  v-model:value="selectedProcess"
                  placeholder="选择流程"
                  size="small"
                  style="width: 200px"
                >
                  <a-select-option
                    v-for="item in processOptions"
                    :key="item.value"
                    :value="item.value"
                  >
                    {{ item.label }}
                  </a-select-option>
                </a-select>
              </div>
            </template>
            <VxeTableList
              :columns="nodeDurationVxeColumns"
              :data-source="nodeDurationData"
              :pagination="false"
              :show-toolbar="false"
              :selectable="false"
              :show-add="false"
              :show-search="false"
              :show-export="false"
              :show-batch-delete="false"
            >
              <template #empty>
                <div class="table-empty">
                  <template v-if="hasError">
                    <WarningOutlined class="table-empty-icon" style="color: #faad14" />
                    <p class="table-empty-text">加载失败</p>
                    <a-button type="primary" size="small" @click="handleRefresh" class="table-empty-action">
                      <ReloadOutlined /> 重试
                    </a-button>
                  </template>
                  <template v-else>
                    <InboxOutlined class="table-empty-icon" />
                    <p class="table-empty-text">暂无数据</p>
                  </template>
                </div>
              </template>
            </VxeTableList>
          </a-card>
        </a-col>
      </a-row>

      <a-row
        :gutter="16"
        style="margin-top: 16px"
      >
        <!-- 审批效率报表 -->
        <a-col :span="24">
          <a-card>
            <template #title>
              <div class="card-header">
                <span>审批效率报表</span>
                <a-range-picker
                  v-model:value="reportDateRange"
                  value-format="YYYY-MM-DD"
                  size="small"
                  @change="handleReportDateChange"
                />
              </div>
            </template>
            <VxeTableList
              :columns="efficiencyVxeColumns"
              :data-source="efficiencyData"
              :pagination="false"
              :show-toolbar="false"
              :selectable="false"
              :show-add="false"
              :show-search="false"
              :show-export="false"
              :show-batch-delete="false"
            >
              <template #completionRateCell="{ record }">
                <a-progress
                  :percent="record.completionRate"
                  :stroke-color="getProgressColor(record.completionRate)"
                />
              </template>
              <template #efficiencyCell="{ record }">
                <a-rate
                  v-model:value="record.efficiency"
                  disabled
                />
              </template>
              <template #empty>
                <div class="table-empty">
                  <template v-if="hasError">
                    <WarningOutlined class="table-empty-icon" style="color: #faad14" />
                    <p class="table-empty-text">加载失败</p>
                    <a-button type="primary" size="small" @click="handleRefresh" class="table-empty-action">
                      <ReloadOutlined /> 重试
                    </a-button>
                  </template>
                  <template v-else>
                    <InboxOutlined class="table-empty-icon" />
                    <p class="table-empty-text">暂无数据</p>
                  </template>
                </div>
              </template>
            </VxeTableList>
          </a-card>
        </a-col>
      </a-row>

      <a-row
        :gutter="16"
        style="margin-top: 16px"
      >
        <!-- 流程趋势图 -->
        <a-col :span="12">
          <a-card title="流程实例趋势">
            <div class="chart-container">
              <a-empty description="图表组件开发中..." />
            </div>
          </a-card>
        </a-col>

        <!-- 节点分布图 -->
        <a-col :span="12">
          <a-card title="节点任务分布">
            <div class="chart-container">
              <a-empty description="图表组件开发中..." />
            </div>
          </a-card>
        </a-col>
      </a-row>
    </div>
  </div>
</PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { SyncOutlined, ReloadOutlined, WarningOutlined, SearchOutlined, InboxOutlined } from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import request from '@/utils/request'
import { PageContainer } from '@/components'

// ── 防抖工具 ────────────────────────────────────────────
const clickLocks = new Map<string, boolean>()
function debounceClick(key: string, fn: (...args: any[]) => any) {
  return (...args: any[]) => {
    if (clickLocks.get(key)) return
    clickLocks.set(key, true)
    try { fn(...args) } finally { setTimeout(() => clickLocks.delete(key), 300) }
  }
}

// ── 自动刷新 ────────────────────────────────────────────
const hasError = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

// 统计卡片数据
const statisticCards = ref([
  { title: '流程实例总数', value: 1234, suffix: '个' },
  { title: '运行中实例', value: 56, suffix: '个' },
  { title: '待办任务数', value: 234, suffix: '个' },
  { title: '平均处理时长', value: 2.5, suffix: '天' }
])

// 流程耗时数据
const processDurationVxeColumns = [
  { field: 'processName', title: '流程名称', width: 150 },
  { field: 'instanceCount', title: '实例数', width: 80, align: 'center' },
  { field: 'avgDuration', title: '平均耗时', width: 100, align: 'right' },
  { field: 'maxDuration', title: '最长耗时', width: 100, align: 'right' },
  { field: 'minDuration', title: '最短耗时', width: 100, align: 'right' }
]

const processDurationData = ref([
  {
    processName: '请假审批流程',
    instanceCount: 120,
    avgDuration: '1.2天',
    maxDuration: '3天',
    minDuration: '0.5天'
  },
  {
    processName: '报销审批流程',
    instanceCount: 89,
    avgDuration: '2.5天',
    maxDuration: '5天',
    minDuration: '1天'
  },
  {
    processName: '采购审批流程',
    instanceCount: 67,
    avgDuration: '3.8天',
    maxDuration: '7天',
    minDuration: '2天'
  },
  {
    processName: '出差审批流程',
    instanceCount: 45,
    avgDuration: '1.8天',
    maxDuration: '4天',
    minDuration: '0.8天'
  }
])

// 节点耗时数据
const selectedProcess = ref('leave')
const processOptions = ref([
  { label: '请假审批流程', value: 'leave' },
  { label: '报销审批流程', value: 'expense' },
  { label: '采购审批流程', value: 'purchase' }
])

const nodeDurationVxeColumns = [
  { field: 'nodeName', title: '节点名称', width: 120 },
  { field: 'taskCount', title: '任务数', width: 80, align: 'center' },
  { field: 'avgDuration', title: '平均耗时', width: 100, align: 'right' },
  { field: 'overdueCount', title: '超时数', width: 80, align: 'center' },
  { field: 'overdueRate', title: '超时率', width: 80, align: 'center' }
]

const nodeDurationData = ref([
  { nodeName: '发起人提交', taskCount: 120, avgDuration: '0.5h', overdueCount: 2, overdueRate: '1.7%' },
  { nodeName: '部门经理审批', taskCount: 118, avgDuration: '4.2h', overdueCount: 8, overdueRate: '6.8%' },
  { nodeName: '人事审批', taskCount: 110, avgDuration: '2.5h', overdueCount: 3, overdueRate: '2.7%' }
])

// 审批效率数据
const reportDateRange = ref<any[]>([])
const efficiencyVxeColumns = [
  { field: 'date', title: '日期', width: 120 },
  { field: 'totalTasks', title: '总任务数', width: 100, align: 'center' },
  { field: 'completedTasks', title: '完成数', width: 100, align: 'center' },
  { field: 'completionRate', title: '完成率', width: 150, slotName: 'completionRateCell' },
  { field: 'avgDuration', title: '平均耗时', width: 100, align: 'right' },
  { field: 'overdueTasks', title: '超时任务', width: 100, align: 'center' },
  { field: 'efficiency', title: '效率评分', width: 150, slotName: 'efficiencyCell' }
]

const efficiencyData = ref([
  {
    date: '2024-04-08',
    totalTasks: 50,
    completedTasks: 48,
    completionRate: 96,
    avgDuration: '1.8天',
    overdueTasks: 2,
    efficiency: 5
  },
  {
    date: '2024-04-09',
    totalTasks: 55,
    completedTasks: 52,
    completionRate: 95,
    avgDuration: '2.1天',
    overdueTasks: 3,
    efficiency: 4.5
  },
  {
    date: '2024-04-10',
    totalTasks: 60,
    completedTasks: 58,
    completionRate: 97,
    avgDuration: '1.9天',
    overdueTasks: 2,
    efficiency: 5
  },
  {
    date: '2024-04-11',
    totalTasks: 48,
    completedTasks: 45,
    completionRate: 94,
    avgDuration: '2.3天',
    overdueTasks: 3,
    efficiency: 4
  },
  {
    date: '2024-04-12',
    totalTasks: 52,
    completedTasks: 50,
    completionRate: 96,
    avgDuration: '2.0天',
    overdueTasks: 2,
    efficiency: 4.5
  },
  {
    date: '2024-04-13',
    totalTasks: 58,
    completedTasks: 56,
    completionRate: 97,
    avgDuration: '1.7天',
    overdueTasks: 2,
    efficiency: 5
  },
  {
    date: '2024-04-14',
    totalTasks: 65,
    completedTasks: 62,
    completionRate: 95,
    avgDuration: '2.2天',
    overdueTasks: 3,
    efficiency: 4.5
  }
])

// 刷新数据
const handleRefresh = async () => {
  hasError.value = false
  try {
    const res = await request.get('/workflow/analysis/refresh')
    if (res.data) {
      if (res.data.statistics) {
        const stats = res.data.statistics
        statisticCards.value = [
          { title: '流程实例总数', value: stats.totalInstances || 0, suffix: '个' },
          { title: '运行中实例', value: stats.runningInstances || 0, suffix: '个' },
          { title: '待办任务数', value: stats.todoTasks || 0, suffix: '个' },
          { title: '平均处理时长', value: stats.avgDuration || 0, suffix: '天' }
        ]
      }
      if (res.data.processDuration) {
        processDurationData.value = res.data.processDuration
      }
      if (res.data.nodeDuration) {
        nodeDurationData.value = res.data.nodeDuration
      }
    }
    console.warn('[工作流] 操作成功: 数据刷新成功')
    message.success('数据刷新成功')
  } catch (err) {
    hasError.value = true
    console.warn('[工作流] 刷新数据失败', err)
    message.error('刷新数据失败')
  } finally {
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    refreshLoading.value = false
  }
}

// 报表日期变化
const handleReportDateChange = async () => {
  try {
    const res = await request.get('/workflow/analysis/report', {
      params: {
        startDate: reportDateRange.value?.[0],
        endDate: reportDateRange.value?.[1]
      }
    })
    if (res.data?.records?.length) {
      efficiencyData.value = res.data.records
    }
    console.warn('[工作流] 操作成功: 报表数据加载成功')
    message.success('报表数据加载成功')
  } catch (err) {
    console.warn('[工作流] 加载报表数据失败', err)
    message.error('加载报表数据失败')
  }
}

// 获取进度条颜色
const getProgressColor = (percentage: number) => {
  if (percentage >= 95) return '#52c41a'
  if (percentage >= 90) return '#faad14'
  return '#f5222d'
}

function handleParentCreate() { handleAdd() }

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) {
    e.preventDefault()
    debounceClick('refresh', handleRefresh)()
  }
}

onMounted(() => {
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    handleRefresh()
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

defineExpose({ handleQuery: handleRefresh })
</script>

<style scoped>
/* ── 让 VxeTableList 填满剩余空间 ──────────────────────── */
.page-content {
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

.workflow-analysis-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.workflow-analysis-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.workflow-analysis-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.workflow-analysis-page-header-right {
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

.workflow-analysis {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: auto;
}

.stat-cards-row {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.suffix {
  font-size: 12px;
  color: #999;
}

.chart-container {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  border-radius: 4px;
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
</style>
