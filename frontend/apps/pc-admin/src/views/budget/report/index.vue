<template>
  <div class="report-page">
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
          <a-table
            :data-source="varianceTableDataSource"
            :columns="varianceColumns"
            :loading="varianceLoading"
            :pagination="{ pageSize: 10 }"
            row-key="budgetId"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="record.__empty_row">
                <span class="empty-placeholder">&nbsp;</span>
              </template>
              <template v-else-if="column.key === 'totalAmount' || column.key === 'totalUsedAmount' || column.key === 'totalRemainingAmount' || column.key === 'variance'">
                <span class="amount-cell">¥{{ formatAmount(record[column.key]) }}</span>
              </template>
              <template v-else-if="column.key === 'executionRate'">
                <span class="rate-cell">{{ (record[column.key] || 0).toFixed(2) }}%</span>
              </template>
              <template v-else-if="column.key === 'varianceRate'">
                <span :style="{ color: record.varianceRate > 0 ? '#ff4d4f' : record.varianceRate < 0 ? '#52c41a' : undefined }">
                  {{ (record.varianceRate || 0).toFixed(2) }}%
                </span>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import {
  FileTextOutlined, DollarOutlined, PieChartOutlined, PercentageOutlined
} from '@ant-design/icons-vue'
import { budgetReportApi } from '@/api/budget'
import * as echarts from 'echarts'

const fiscalYear = ref(new Date().getFullYear())
const summary = ref<any>({})

const deptChartRef = ref<HTMLDivElement>()
const subjectChartRef = ref<HTMLDivElement>()
const trendChartRef = ref<HTMLDivElement>()

let deptChart: echarts.ECharts | null = null
let subjectChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

const varianceData = ref<any[]>([])
const varianceLoading = ref(false)

// ── 空行填充 ────────────────────────────────────────────
const MIN_TABLE_ROWS = 20
const varianceTableDataSource = computed(() => {
  const data = [...varianceData.value]
  const emptyCount = Math.max(0, MIN_TABLE_ROWS - data.length)
  for (let i = 0; i < emptyCount; i++) {
    data.push({ __empty_row: true, budgetId: `__empty_${i}` })
  }
  return data
})

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const varianceColumns = [
  { title: '预算单号', dataIndex: 'budgetNo', key: 'budgetNo', width: 150 },
  { title: '部门', dataIndex: 'departmentName', key: 'departmentName', width: 120 },
  { title: '预算金额', key: 'totalAmount', width: 130, align: 'right' as const },
  { title: '已使用', key: 'totalUsedAmount', width: 130, align: 'right' as const },
  { title: '剩余', key: 'totalRemainingAmount', width: 130, align: 'right' as const },
  { title: '执行率', key: 'executionRate', width: 80, align: 'right' as const },
  { title: '差异金额', key: 'variance', width: 130, align: 'right' as const },
  { title: '差异率', key: 'varianceRate', width: 80, align: 'right' as const },
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
  } catch { summary.value = mockSummary() }

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
    deptChart?.setOption({
      xAxis: { data: ['财务部', '市场部', '研发部', '人事部'] },
      series: [{ name: '预算总额', type: 'bar', data: [500000, 300000, 800000, 200000] }, { name: '已使用', type: 'bar', data: [200000, 100000, 400000, 150000] }],
    })
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
    subjectChart?.setOption({
      series: [{
        name: '科目预算',
        type: 'pie',
        radius: ['30%', '60%'],
        data: [
          { value: 300000, name: '办公费用' },
          { value: 500000, name: '人力成本' },
          { value: 200000, name: '设备采购' },
          { value: 150000, name: '营销推广' },
        ],
      }],
    })
  }

  // Trend
  try {
    const res = await budgetReportApi.trend(fiscalYear.value)
    if (res.success && res.data) {
      const amounts = res.data.map((d: any) => d.amount)
      trendChart?.setOption({ series: [{ data: amounts }] })
    }
  } catch {
    trendChart?.setOption({ series: [{ data: [50000, 80000, 100000, 120000, 150000, 180000, 200000, 220000, 250000, 280000, 300000, 350000] }] })
  }

  // Variance
  varianceLoading.value = true
  try {
    const res = await budgetReportApi.varianceAnalysis(fiscalYear.value)
    if (res.success) varianceData.value = res.data || mockVarianceData()
  } catch {
    varianceData.value = mockVarianceData()
  }
  varianceLoading.value = false
}

const mockSummary = (): any => ({
  totalBudgetCount: 10,
  totalBudgetAmount: 1800000,
  totalUsedAmount: 850000,
  executionRate: 47.22,
})

const mockVarianceData = (): any[] => [
  { budgetId: 1, budgetNo: 'BUD-2024-001', departmentName: '财务部', totalAmount: 500000, totalUsedAmount: 200000, totalRemainingAmount: 300000, executionRate: 40, variance: 0, varianceRate: 0 },
  { budgetId: 2, budgetNo: 'BUD-2024-002', departmentName: '市场部', totalAmount: 300000, totalUsedAmount: 350000, totalRemainingAmount: -50000, executionRate: 116.67, variance: -50000, varianceRate: -16.67 },
  { budgetId: 3, budgetNo: 'BUD-2024-003', departmentName: '研发部', totalAmount: 800000, totalUsedAmount: 200000, totalRemainingAmount: 600000, executionRate: 25, variance: 0, varianceRate: 0 },
  { budgetId: 4, budgetNo: 'BUD-2024-004', departmentName: '人事部', totalAmount: 200000, totalUsedAmount: 180000, totalRemainingAmount: 20000, executionRate: 90, variance: 0, varianceRate: 0 },
]

onMounted(() => {
  setTimeout(() => {
    initCharts()
    loadAllData()
  }, 100)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  deptChart?.dispose()
  subjectChart?.dispose()
  trendChart?.dispose()
})

const handleResize = () => {
  deptChart?.resize()
  subjectChart?.resize()
  trendChart?.resize()
}
</script>

<style scoped>
.report-page {
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

.empty-placeholder { color: transparent; }

/* 表格网格边框 */
:deep(.ant-table-thead > tr > th) {
  border-top: 1px solid #d9d9d9 !important;
  border-right: 1px solid #d9d9d9 !important;
  border-bottom: 2px solid #b0b0b0 !important;
  background: #fafafa !important;
  padding: 8px 12px !important;
  font-weight: 600 !important;
}

:deep(.ant-table-thead > tr > th:first-child) {
  border-left: 1px solid #d9d9d9 !important;
}

:deep(.ant-table-tbody > tr > td) {
  border-right: 1px solid #e0e0e0 !important;
  border-bottom: 1px solid #e8e8e8 !important;
  padding: 8px 12px !important;
}

:deep(.ant-table-tbody > tr > td:first-child) {
  border-left: 1px solid #e0e0e0 !important;
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