<template>
  <div class="budget-report-page">
    <a-card title="预算报表分析">
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

    <!-- KPI 概述 -->
    <a-row :gutter="[16, 16]" class="mt-2">
      <a-col :span="6">
        <a-card hoverable>
          <stat-card title="预算总数" :value="summary.totalBudgetCount" color="#1890ff" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable>
          <stat-card title="预算总额" :value="summary.totalBudgetAmount" :precision="2" prefix="¥" color="#722ed1" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable>
          <stat-card title="已使用" :value="summary.totalUsedAmount" :precision="2" prefix="¥" color="#faad14" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable>
          <stat-card title="整体执行率" :value="summary.executionRate" :precision="2" suffix="%" color="#52c41a" />
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]" class="mt-2">
      <!-- 部门预算分布 -->
      <a-col :span="12">
        <a-card title="部门预算分布">
          <div ref="deptChartRef" style="height: 350px"></div>
        </a-card>
      </a-col>
      <!-- 预算状态分布 -->
      <a-col :span="12">
        <a-card title="科目预算分布">
          <div ref="subjectChartRef" style="height: 350px"></div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]" class="mt-2">
      <!-- 月度趋势 -->
      <a-col :span="24">
        <a-card title="月度预算使用趋势">
          <div ref="trendChartRef" style="height: 350px"></div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]" class="mt-2">
      <!-- 差异分析表 -->
      <a-col :span="24">
        <a-card title="预算差异分析">
          <a-table
            :data-source="varianceData"
            :columns="varianceColumns"
            :loading="varianceLoading"
            :pagination="{ pageSize: 10 }"
            row-key="budgetId"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'totalAmount' || column.key === 'totalUsedAmount' || column.key === 'totalRemainingAmount' || column.key === 'variance'">
                ¥{{ record[column.key]?.toFixed(2) ?? '0.00' }}
              </template>
              <template v-else-if="column.key === 'executionRate' || column.key === 'varianceRate'">
                {{ record[column.key]?.toFixed(2) ?? '0.00' }}%
              </template>
              <template v-else-if="column.key === 'varianceRate'">
                <span :style="{ color: record.varianceRate > 0 ? '#ff4d4f' : record.varianceRate < 0 ? '#52c41a' : undefined }">
                  {{ record.varianceRate?.toFixed(2) }}%
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
import { ref, onMounted, onUnmounted } from 'vue'
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

const varianceColumns = [
  { title: '预算单号', dataIndex: 'budgetNo', key: 'budgetNo' },
  { title: '部门', dataIndex: 'departmentName', key: 'departmentName' },
  { title: '预算金额', dataIndex: 'totalAmount', key: 'totalAmount', align: 'right' as const },
  { title: '已使用', dataIndex: 'totalUsedAmount', key: 'totalUsedAmount', align: 'right' as const },
  { title: '剩余', dataIndex: 'totalRemainingAmount', key: 'totalRemainingAmount', align: 'right' as const },
  { title: '执行率', dataIndex: 'executionRate', key: 'executionRate', align: 'right' as const, width: 80 },
  { title: '差异金额', dataIndex: 'variance', key: 'variance', align: 'right' as const },
  { title: '差异率', dataIndex: 'varianceRate', key: 'varianceRate', align: 'right' as const, width: 80 },
]

const initCharts = () => {
  if (deptChartRef.value) {
    deptChart = echarts.init(deptChartRef.value)
    deptChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'category', data: [] },
      yAxis: { type: 'value', name: '金额 (元)' },
      series: [{ name: '预算总额', type: 'bar', data: [] }, { name: '已使用', type: 'bar', data: [] }],
    })
  }
  if (subjectChartRef.value) {
    subjectChart = echarts.init(subjectChartRef.value)
    subjectChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: '0%' },
      series: [{ name: '科目预算', type: 'pie', radius: ['30%', '60%'], data: [] }],
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
      series: [{ name: '月度使用', type: 'line', smooth: true, data: [], areaStyle: { opacity: 0.3 } }],
    })
  }
}

const loadAllData = async () => {
  // Summary
  try {
    const res = await budgetReportApi.executionSummary(fiscalYear.value)
    if (res.success) summary.value = res.data
  } catch { /* error already handled */ }

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
  } catch { /* error already handled */ }

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
  } catch { /* error already handled */ }

  // Trend
  try {
    const res = await budgetReportApi.trend(fiscalYear.value)
    if (res.success && res.data) {
      const amounts = res.data.map((d: any) => d.amount)
      trendChart?.setOption({ series: [{ data: amounts }] })
    }
  } catch { /* error already handled */ }

  // Variance
  varianceLoading.value = true
  try {
    const res = await budgetReportApi.varianceAnalysis(fiscalYear.value)
    if (res.success) varianceData.value = res.data || []
  } catch { /* error already handled */ }
  varianceLoading.value = false
}

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
.budget-report-page { padding: 16px; }
.search-area { margin-bottom: 16px; }
.mt-2 { margin-top: 16px; }
</style>
