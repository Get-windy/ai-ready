<template>
  <ErrorBoundary @error="handleError">
    <PageContainer full-height>
      <!--
        流程分析（设置 → 工作流 → 流程分析，菜单 80611 / set:workflow-analysis）
        · 定位：只读分析台账（统计卡 + 流程耗时 / 节点耗时 / 按日审批效率三张表 + 两张图）
        · 对标：ql361 无工作流域 → 本系统独有页面，无对标目标
        · 数据源（core-api WorkflowController，全部为服务端真实聚合，页面内**无任何写死的假数据**）：
            GET /api/workflow/analysis/refresh?processName=  → statistics / processDuration / nodeDuration
            GET /api/workflow/analysis/report?startDate=&endDate= → records（按日聚合）
        · 本页为只读页：无新增 / 修改 / 删除动作，故无 v-permission 门控按钮；
          读端点在控制器上按本模块口径加了 workflow:analysis:view 方法级权限码。
        · 首屏立即加载（onMounted 直接拉一次），随后 30 秒轮询 + 手动刷新（按钮 / F5 / Ctrl+R）
      -->
      <template #header>
        <div class="workflow-analysis-page-header">
          <div class="workflow-analysis-page-header-left">
            <a-breadcrumb>
              <a-breadcrumb-item>
                <router-link to="/">
                  首页
                </router-link>
              </a-breadcrumb-item>
              <a-breadcrumb-item>流程分析</a-breadcrumb-item>
            </a-breadcrumb>
            <h2 class="workflow-analysis-page-header-title">
              流程分析
            </h2>
          </div>
          <div class="workflow-analysis-page-header-right">
            <span
              v-if="lastUpdateTime"
              class="update-time"
            >更新于 {{ lastUpdateTime }}</span>
            <span
              v-if="autoRefreshCountdown > 0"
              class="auto-refresh-badge"
            >
              <SyncOutlined /> {{ autoRefreshCountdown }}s
            </span>
            <a-button
              size="small"
              :loading="refreshLoading"
              @click="handleRefresh"
            >
              <template #icon>
                <ReloadOutlined />
              </template>
              刷新
            </a-button>
            <span class="shortcut-hints">
              <span class="shortcut-hint"><kbd>Ctrl+R</kbd> 刷新</span>
              <span class="shortcut-hint"><kbd>F5</kbd> 刷新</span>
            </span>
          </div>
        </div>
      </template>

      <div class="page-content">
        <div class="workflow-analysis">
          <!-- 统计卡：服务端全量聚合；首屏拿到数据前显示「-」，不用 0 / 假数字占位 -->
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
                  :formatter="formatStatValue"
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
            <!-- 流程耗时统计（按流程分组，全部流程） -->
            <a-col :span="12">
              <a-card>
                <template #title>
                  <div class="card-header">
                    <span>流程耗时统计</span>
                  </div>
                </template>
                <BillTableList
                  :columns="processDurationColumns"
                  :data-source="processDurationData"
                  :loading="summaryLoading"
                  :pagination="false"
                  :show-toolbar="false"
                  :selectable="false"
                  :show-add="false"
                  :show-search="false"
                  :show-export="false"
                  :show-batch-delete="false"
                  :min-empty-rows="12"
                >
                  <template #empty>
                    <div class="table-empty">
                      <template v-if="hasError">
                        <WarningOutlined
                          class="table-empty-icon"
                          style="color: #faad14"
                        />
                        <p class="table-empty-text">
                          加载失败
                        </p>
                        <a-button
                          type="primary"
                          size="small"
                          class="table-empty-action"
                          @click="handleRefresh"
                        >
                          <ReloadOutlined /> 重试
                        </a-button>
                      </template>
                      <template v-else>
                        <InboxOutlined class="table-empty-icon" />
                        <p class="table-empty-text">
                          暂无数据
                        </p>
                      </template>
                    </div>
                  </template>
                </BillTableList>
              </a-card>
            </a-col>

            <!-- 节点耗时分析：可选流程来自真实 processDuration，选中即按流程过滤（后端 processName 参数） -->
            <a-col :span="12">
              <a-card>
                <template #title>
                  <div class="card-header">
                    <span>节点耗时分析</span>
                    <a-select
                      v-model:value="selectedProcess"
                      placeholder="全部流程"
                      size="small"
                      style="width: 200px"
                      allow-clear
                      :options="processOptions"
                      @change="loadSummary(false)"
                    />
                  </div>
                </template>
                <BillTableList
                  :columns="nodeDurationColumns"
                  :data-source="nodeDurationData"
                  :loading="summaryLoading"
                  :pagination="false"
                  :show-toolbar="false"
                  :selectable="false"
                  :show-add="false"
                  :show-search="false"
                  :show-export="false"
                  :show-batch-delete="false"
                  :min-empty-rows="12"
                >
                  <template #empty>
                    <div class="table-empty">
                      <template v-if="hasError">
                        <WarningOutlined
                          class="table-empty-icon"
                          style="color: #faad14"
                        />
                        <p class="table-empty-text">
                          加载失败
                        </p>
                        <a-button
                          type="primary"
                          size="small"
                          class="table-empty-action"
                          @click="handleRefresh"
                        >
                          <ReloadOutlined /> 重试
                        </a-button>
                      </template>
                      <template v-else>
                        <InboxOutlined class="table-empty-icon" />
                        <p class="table-empty-text">
                          暂无数据
                        </p>
                      </template>
                    </div>
                  </template>
                </BillTableList>
              </a-card>
            </a-col>
          </a-row>

          <!-- 审批效率报表（按日聚合，日期范围默认近 7 天，与后端缺省口径一致） -->
          <a-row
            :gutter="16"
            style="margin-top: 16px"
          >
            <a-col :span="24">
              <a-card>
                <template #title>
                  <div class="card-header">
                    <span>审批效率报表</span>
                    <a-range-picker
                      v-model:value="reportDateRange"
                      value-format="YYYY-MM-DD"
                      size="small"
                      @change="loadReport(false)"
                    />
                  </div>
                </template>
                <BillTableList
                  :columns="efficiencyColumns"
                  :data-source="efficiencyData"
                  :loading="reportLoading"
                  :pagination="false"
                  :show-toolbar="false"
                  :selectable="false"
                  :show-add="false"
                  :show-search="false"
                  :show-export="false"
                  :show-batch-delete="false"
                  :min-empty-rows="12"
                >
                  <template #completionRateCell="{ record }">
                    <a-progress
                      :percent="record.completionRate"
                      :stroke-color="getProgressColor(record.completionRate)"
                    />
                  </template>
                  <template #efficiencyCell="{ record }">
                    <a-rate
                      :value="record.efficiency"
                      disabled
                      allow-half
                    />
                  </template>
                  <template #empty>
                    <div class="table-empty">
                      <template v-if="reportError">
                        <WarningOutlined
                          class="table-empty-icon"
                          style="color: #faad14"
                        />
                        <p class="table-empty-text">
                          加载失败
                        </p>
                        <a-button
                          type="primary"
                          size="small"
                          class="table-empty-action"
                          @click="loadReport(false)"
                        >
                          <ReloadOutlined /> 重试
                        </a-button>
                      </template>
                      <template v-else>
                        <InboxOutlined class="table-empty-icon" />
                        <p class="table-empty-text">
                          暂无数据
                        </p>
                      </template>
                    </div>
                  </template>
                </BillTableList>
              </a-card>
            </a-col>
          </a-row>

          <!-- 图表：均取自上方两张表的同一份真实聚合，无 series 时组件自身展示空态 -->
          <a-row
            :gutter="16"
            style="margin-top: 16px"
          >
            <a-col :span="12">
              <a-card>
                <ARReportChart
                  title="流程实例分布（按流程）"
                  :option="processDistOption"
                  :height="300"
                  :loading="summaryLoading"
                />
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card>
                <ARReportChart
                  title="节点任务分布"
                  :option="nodeDistOption"
                  :height="300"
                  :loading="summaryLoading"
                >
                  <!-- 标明口径：本图与「节点耗时分析」表同源，随该卡的流程下拉而变化 -->
                  <template #extra>
                    <span class="chart-scope">{{ selectedProcess || '全部流程' }}</span>
                  </template>
                </ARReportChart>
              </a-card>
            </a-col>
          </a-row>
        </div>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { SyncOutlined, ReloadOutlined, WarningOutlined, InboxOutlined } from '@ant-design/icons-vue'
import BillTableList from '@/components/BillTableList/BillTableList.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import ARReportChart from '@/components/ARReportChart/ARReportChart.vue'
import { workflowAnalysisApi, type WorkflowAnalysisSummary, type WorkflowAnalysisReportRow } from '@/api/workflow'

defineOptions({ name: 'WorkflowProcessAnalysis' })

// ── 状态 ────────────────────────────────────────────────
const hasError = ref(false)          // 汇总接口（统计卡 + 两张表 + 两张图）失败
const reportError = ref(false)       // 报表接口失败
const summaryLoading = ref(false)
const reportLoading = ref(false)
const refreshLoading = ref(false)
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

function handleError(err: any) {
  console.warn('[流程分析] 页面出错', err)
  hasError.value = true
}

// ── 统计卡 ──────────────────────────────────────────────
//
// value 初值为字符串「-」占位：a-statistic 会把字符串「-」当成数字解析（负号 + 空整数 = -0），
// 因此非数字展示串必须走 formatter（见本仓既有踩坑记录）。
const statisticCards = ref<Array<{ title: string; value: number | string; suffix: string }>>([
  { title: '流程实例总数', value: '-', suffix: '个' },
  { title: '运行中实例', value: '-', suffix: '个' },
  { title: '待办任务数', value: '-', suffix: '个' },
  { title: '平均处理时长', value: '-', suffix: '天' }
])

function formatStatValue(value: any): string {
  if (value === null || value === undefined || value === '') return '-'
  const num = Number(value)
  if (!Number.isFinite(num)) return String(value)
  return num.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

// ── 流程耗时统计（全部流程） ──────────────────────────────
const processDurationColumns = [
  { title: '流程名称', field: 'processName', key: 'processName', width: 150 },
  { title: '实例数', field: 'instanceCount', key: 'instanceCount', width: 80, align: 'center' },
  { title: '平均耗时', field: 'avgDuration', key: 'avgDuration', width: 100, align: 'right' },
  { title: '最长耗时', field: 'maxDuration', key: 'maxDuration', width: 100, align: 'right' },
  { title: '最短耗时', field: 'minDuration', key: 'minDuration', width: 100, align: 'right' }
]
const processDurationData = ref<Array<Record<string, any>>>([])

// ── 节点耗时分析（processName 为空 = 全部流程） ─────────────
const selectedProcess = ref<string | undefined>(undefined)
const nodeDurationColumns = [
  { title: '节点名称', field: 'nodeName', key: 'nodeName', width: 120 },
  { title: '任务数', field: 'taskCount', key: 'taskCount', width: 80, align: 'center' },
  { title: '平均耗时', field: 'avgDuration', key: 'avgDuration', width: 100, align: 'right' },
  { title: '超时数', field: 'overdueCount', key: 'overdueCount', width: 80, align: 'center' },
  { title: '超时率', field: 'overdueRate', key: 'overdueRate', width: 80, align: 'center' }
]
const nodeDurationData = ref<Array<Record<string, any>>>([])

// 流程下拉的真实选项来源：汇总接口返回的 processDuration（禁止写死选项）
const processOptions = computed(() => processDurationData.value
  .filter((row: any) => !!row.processName)
  .map((row: any) => ({ label: row.processName as string, value: row.processName as string })))

// ── 审批效率报表（按日） ─────────────────────────────────
// 默认近 7 天：与后端缺省口径（end = 今天，start = end - 6）保持一致
const reportDateRange = ref<[string, string]>([
  dayjs().subtract(6, 'day').format('YYYY-MM-DD'),
  dayjs().format('YYYY-MM-DD')
])
const efficiencyColumns = [
  { title: '日期', field: 'date', key: 'date', width: 120 },
  { title: '总任务数', field: 'totalTasks', key: 'totalTasks', width: 100, align: 'center' },
  { title: '完成数', field: 'completedTasks', key: 'completedTasks', width: 100, align: 'center' },
  // 插槽列必须显式 type: 'slot'：只写 slotName 会被当作普通单元格直出原值
  { title: '完成率', field: 'completionRate', key: 'completionRate', width: 150, type: 'slot', slotName: 'completionRateCell' },
  { title: '平均耗时', field: 'avgDuration', key: 'avgDuration', width: 100, align: 'right' },
  { title: '超时任务', field: 'overdueTasks', key: 'overdueTasks', width: 100, align: 'center' },
  { title: '效率评分', field: 'efficiency', key: 'efficiency', width: 150, type: 'slot', slotName: 'efficiencyCell' }
]
const efficiencyData = ref<WorkflowAnalysisReportRow[]>([])

// ── 图表 option（数据源与表格完全一致，不额外造数） ────────
const processDistOption = computed(() => {
  const rows = processDurationData.value
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 8, right: 16, top: 24, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      data: rows.map((row: any) => row.processName),
      axisLabel: { rotate: rows.length > 4 ? 20 : 0, interval: 0, hideOverlap: true }
    },
    yAxis: { type: 'value', name: '实例数', minInterval: 1 },
    series: [{
      type: 'bar',
      name: '实例数',
      barMaxWidth: 40,
      data: rows.map((row: any) => row.instanceCount)
    }]
  }
})

const nodeDistOption = computed(() => {
  const rows = nodeDurationData.value
  return {
    tooltip: { trigger: 'item', formatter: '{b}：{c}（{d}%）' },
    legend: { bottom: 0, type: 'scroll' },
    series: [{
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '45%'],
      data: rows.map((row: any) => ({ name: row.nodeName, value: row.taskCount }))
    }]
  }
})

// ── 加载 ────────────────────────────────────────────────
//
// silent = true：首屏 / 轮询调用，不弹提示；silent = false：用户主动操作，给反馈。
async function loadSummary(silent = true) {
  summaryLoading.value = true
  try {
    const params = selectedProcess.value ? { processName: selectedProcess.value } : {}
    const res: WorkflowAnalysisSummary = await workflowAnalysisApi.refresh(params)
    hasError.value = false

    const stats: any = res?.statistics || res?.data || {}
    statisticCards.value = [
      { title: '流程实例总数', value: stats.totalInstances ?? '-', suffix: '个' },
      { title: '运行中实例', value: stats.runningInstances ?? '-', suffix: '个' },
      { title: '待办任务数', value: stats.todoTasks ?? '-', suffix: '个' },
      { title: '平均处理时长', value: stats.avgDuration ?? '-', suffix: '天' }
    ]

    processDurationData.value = res?.processDuration || []
    nodeDurationData.value = res?.nodeDuration || []
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  } catch (err) {
    hasError.value = true
    console.warn('[流程分析] 汇总数据加载失败', err)
    if (!silent) message.error('加载分析数据失败')
  } finally {
    summaryLoading.value = false
  }
}

async function loadReport(silent = true) {
  reportLoading.value = true
  try {
    const res: any = await workflowAnalysisApi.report({
      startDate: reportDateRange.value?.[0],
      endDate: reportDateRange.value?.[1]
    })
    reportError.value = false
    efficiencyData.value = res?.records || []
  } catch (err) {
    reportError.value = true
    console.warn('[流程分析] 报表数据加载失败', err)
    if (!silent) message.error('加载报表数据失败')
  } finally {
    reportLoading.value = false
  }
}

/** 用户主动刷新（按钮 / 快捷键 / 重试）：表与图一起刷，并给出结果提示 */
async function handleRefresh() {
  refreshLoading.value = true
  await Promise.all([loadSummary(false), loadReport(false)])
  refreshLoading.value = false
  autoRefreshCountdown.value = 30
  if (!hasError.value && !reportError.value) message.success('数据刷新成功')
}

/** 轮询刷新：静默，只刷汇总（报表按选定日期范围人工触发） */
async function loadByPolling() {
  await loadSummary(true)
  autoRefreshCountdown.value = 30
}

// 获取进度条颜色
const getProgressColor = (percentage: number) => {
  if (percentage >= 95) return '#52c41a'
  if (percentage >= 90) return '#faad14'
  return '#f5222d'
}

function handleParentCreate() { handleRefresh() }

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' || (e.ctrlKey && e.key === 'r')) {
    e.preventDefault()
    handleRefresh()
  }
}

onMounted(() => {
  // 首屏立即加载（原实现只在 setInterval 回调里刷新，首屏最多 30 秒展示写死的 2024 年假数据）
  loadSummary(true)
  loadReport(true)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    loadByPolling()
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
/* ── 让内容区填满剩余空间 ──────────────────────── */
.page-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.workflow-analysis {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: auto;
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

/* 图表口径标注（如「全部流程」/ 选中的流程名） */
.chart-scope {
  font-size: 12px;
  color: #909399;
  padding: 1px 6px;
  border-radius: 3px;
  background: #f5f7fa;
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

/* ── vxe-table 表头 2px 边框 ─────── */
:deep(.vxe-table .vxe-header--row) { border-top: 2px solid #e8e8e8; }
</style>
