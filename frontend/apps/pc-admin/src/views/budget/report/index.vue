<template>
  <PageContainer full-height>
    <template #header>
      <div class="report-page-header">
        <div class="report-page-header-left">
          <a-breadcrumb>
            <a-breadcrumb-item><router-link to="/">首页</router-link></a-breadcrumb-item>
            <a-breadcrumb-item>预算报表</a-breadcrumb-item>
          </a-breadcrumb>
          <h2 class="report-page-header-title">预算报表</h2>
        </div>
        <div class="report-page-header-right">
          <span v-if="lastUpdateTime" class="update-time">更新于 {{ lastUpdateTime }}</span>
          <span v-if="autoRefreshCountdown > 0" class="auto-refresh-badge">
            <SyncOutlined /> {{ autoRefreshCountdown }}s
          </span>
          <a-button size="small" :loading="refreshLoading" @click="loadAllData">
            <template #icon><ReloadOutlined /></template>
            刷新
          </a-button>
        </div>
      </div>
    </template>

    <div class="report-management">
      <!-- KPI 卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-count">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ summary.totalBudgetCount || 0 }}</div>
            <div class="stat-card-label">预算总数</div>
          </div>
          <FileTextOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-amount">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(summary.totalBudgetAmount) }}</div>
            <div class="stat-card-label">预算总额</div>
          </div>
          <DollarOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-used">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(summary.totalUsedAmount) }}</div>
            <div class="stat-card-label">已使用</div>
          </div>
          <PieChartOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-rate">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ (summary.executionRate || 0).toFixed(2) }}%</div>
            <div class="stat-card-label">整体执行率</div>
          </div>
          <PercentageOutlined class="stat-card-icon" />
        </div>
      </div>

      <a-card title="预算报表分析" class="filter-card">
        <div class="search-area">
          <a-form layout="inline">
            <a-form-item label="年度">
              <a-input-number v-model:value="fiscalYear" :min="2020" :max="2099" style="width: 120px" />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="loadAllData">查询</a-button>
            </a-form-item>
          </a-form>
        </div>
      </a-card>

      <a-row :gutter="[16, 16]" class="mt-2">
        <!-- 部门预算分布 -->
        <a-col :span="12">
          <a-card title="部门预算分布" class="chart-card">
            <div ref="deptChartRef" style="height: 350px"></div>
          </a-card>
        </a-col>
        <!-- 科目预算分布 -->
        <a-col :span="12">
          <a-card title="科目预算分布" class="chart-card">
            <div ref="subjectChartRef" style="height: 350px"></div>
          </a-card>
        </a-col>
      </a-row>

      <a-row :gutter="[16, 16]" class="mt-2">
        <!-- 月度趋势 -->
        <a-col :span="24">
          <a-card title="月度预算使用趋势" class="chart-card">
            <div ref="trendChartRef" style="height: 350px"></div>
          </a-card>
        </a-col>
      </a-row>

      <a-row :gutter="[16, 16]" class="mt-2">
        <!-- 差异分析表 -->
        <a-col :span="24">
          <a-card title="预算差异分析" class="table-card">
            <VxeTableList
              :data-source="varianceData"
              :columns="varianceVxeColumns"
              :loading="varianceLoading"
              :pagination="{ pageSize: 10 }"
              row-key="budgetId"
              :show-toolbar="false"
              :selectable="false"
              :show-add="false"
              :show-search="false"
              :show-export="false"
              :show-batch-delete="false"
            >
              <template #varianceRateCell="{ record }">
                <span :style="{ color: record.varianceRate > 0 ? '#ff4d4f' : record.varianceRate < 0 ? '#52c41a' : undefined }">
                  {{ (record.varianceRate || 0).toFixed(2) }}%
                </span>
              </template>
            </VxeTableList>
          </a-card>
        </a-col>
      </a-row>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  FileTextOutlined, DollarOutlined, PieChartOutlined, PercentageOutlined,
  SyncOutlined, ReloadOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { PageContainer } from '@/components'
import { budgetReportApi } from '@/api/budget'
import * as echarts from 'echarts'

const fiscalYear = ref(new Date().getFullYear())
const summary = ref<any>({})
const lastUpdateTime = ref('')
const autoRefreshCountdown = ref(0)
const refreshLoading = ref(false)
let refreshTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null

const deptChartRef = ref<HTMLDivElement>()
const subjectChartRef = ref<HTMLDivElement>()
const trendChartRef = ref<HTMLDivElement>()

let deptChart: echarts.ECharts | null = null
let subjectChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

const varianceData = ref<any[]>([])
const varianceLoading = ref(false)


const amountFmt = ({ cellValue }: any) => `¥${(cellValue ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`

const varianceVxeColumns = [
  { field: 'budgetNo', title: '预算单号', width: 150 },
  { field: 'departmentName', title: '部门', width: 120 },
  { field: 'totalAmount', title: '预算金额', width: 130, align: 'right', formatter: amountFmt },
  { field: 'totalUsedAmount', title: '已使用', width: 130, align: 'right', formatter: amountFmt },
  { field: 'totalRemainingAmount', title: '剩余', width: 130, align: 'right', formatter: amountFmt },
  { field: 'executionRate', title: '执行率', width: 80, align: 'right', formatter: ({ cellValue }: any) => `${(cellValue || 0).toFixed(2)}%` },
  { field: 'variance', title: '差异金额', width: 130, align: 'right', formatter: amountFmt },
  { field: 'varianceRate', title: '差异率', width: 80, align: 'right', slotName: 'varianceRateCell' },
]

const initCharts = () => {
  if (deptChartRef.value) {
    deptChart = echarts.init(deptChartRef.value)
    deptChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value', name: '金额 (元)' },
      series: [{ name: '预算总额', type: 'bar', data: [], itemStyle: { color: '#1890ff' } }, { name: '已使用', type: 'bar', data: [], itemStyle: { color: '#52c41a' } }],
    })
  }
  if (subjectChartRef.value) {
    subjectChart = echarts.init(subjectChartRef.value)
    subjectChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: '0%' },
      series: [{ name: '科目预算', type: 'pie', radius: ['30%', '60%'], data: [], itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 } }],
    })
  }
  if (trendChartRef.value) {
    trendChart = echarts.init(trendChartRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['月度使用'] },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'] },
      yAxis: { type: 'value', name: '金额 (元)' },
      series: [{ name: '月度使用', type: 'line', smooth: true, data: [], areaStyle: { opacity: 0.3 }, itemStyle: { color: '#1890ff' } }],
    })
  }
}

const loadAllData = async () => {
  // Summary
  try {
    const res = await budgetReportApi.executionSummary(fiscalYear.value)
    if (res.success) summary.value = res.data
  } catch {
    console.warn('[预算报表] 加载汇总数据失败')
    summary.value = {}
  }

  // Department chart
  try {
    const res = await budgetReportApi.departmentSummary(fiscalYear.value)
    if (res.success && res.data) {
      const depts = res.data.map((d: any) => d.departmentName || d.departmentId)
      const budgets = res.data.map((d: any) => d.totalBudget || 0)
      const used = res.data.map((d: any) => d.totalUsed || 0)
      deptChart?.setOption({
        xAxis: { data: depts },
        series: [{ name: '预算总额', type: 'bar', data: budgets }, { name: '已使用', type: 'bar', data: used }],
      })
    }
  } catch {
    console.warn('[预算报表] 加载部门图表失败')
  }

  // Subject chart
  try {
    const res = await budgetReportApi.subjectSummary(fiscalYear.value)
    if (res.success && res.data) {
      const topSubjects = res.data.slice(0, 10)
      subjectChart?.setOption({
        series: [{
          name: '科目预算',
          type: 'pie',
          radius: ['30%', '60%'],
          data: topSubjects.map((d: any) => ({ value: d.totalBudget || 0, name: d.subjectName || d.subjectCode })),
        }],
      })
    }
  } catch {
    console.warn('[预算报表] 加载科目图表失败')
  }

  // Trend
  try {
    const res = await budgetReportApi.trend(fiscalYear.value)
    if (res.success && res.data) {
      const amounts = res.data.map((d: any) => d.amount)
      trendChart?.setOption({ series: [{ data: amounts }] })
    }
  } catch {
    console.warn('[预算报表] 加载趋势数据失败')
  }

  // Variance
  varianceLoading.value = true
  try {
    const res = await budgetReportApi.varianceAnalysis(fiscalYear.value)
    if (res.success) varianceData.value = res.data || []
  } catch {
    console.warn('[预算报表] 加载差异分析失败')
    varianceData.value = []
  }
  varianceLoading.value = false
  lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
  refreshLoading.value = false
}

const handleResize = () => {
  deptChart?.resize()
  subjectChart?.resize()
  trendChart?.resize()
}

onMounted(() => {
  setTimeout(() => {
    initCharts()
    loadAllData()
  }, 100)
  window.addEventListener('resize', handleResize)
  autoRefreshCountdown.value = 30
  refreshTimer = setInterval(() => {
    loadAllData()
    autoRefreshCountdown.value = 30
  }, 30000)
  countdownTimer = setInterval(() => {
    if (autoRefreshCountdown.value > 0) autoRefreshCountdown.value--
  }, 1000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (countdownTimer) clearInterval(countdownTimer)
  window.removeEventListener('resize', handleResize)
  deptChart?.dispose()
  subjectChart?.dispose()
  trendChart?.dispose()
})

defineExpose({ handleQuery: loadAllData })
</script>

<style scoped>
.report-page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}
.report-page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.report-page-header-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}
.report-page-header-right {
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

.report-management {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: auto;
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

.stat-count { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-amount { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }
.stat-used { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-rate { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }

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

.filter-card, .chart-card, .table-card {
  border-radius: 8px;
}

.search-area { margin-bottom: 0; }
.mt-2 { margin-top: 16px; }

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

.rate-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
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
</style>
