<template>
  <div class="budget-dashboard">
    <!-- KPI 卡片 -->
    <a-row :gutter="[16, 16]">
      <a-col :span="6">
        <a-card hoverable>
          <statistic-card
            title="预算总额"
            :value="statistics.totalBudgetAmount"
            :precision="2"
            prefix="¥"
            color="#1890ff"
          />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable>
          <statistic-card
            title="已使用金额"
            :value="statistics.totalUsedAmount"
            :precision="2"
            prefix="¥"
            color="#faad14"
          />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable>
          <statistic-card
            title="剩余金额"
            :value="statistics.totalRemainingAmount"
            :precision="2"
            prefix="¥"
            color="#52c41a"
          />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card hoverable>
          <statistic-card
            title="执行率"
            :value="statistics.executionRate"
            :precision="2"
            suffix="%"
            color="#722ed1"
          />
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]" class="mt-4">
      <!-- 执行率图表 -->
      <a-col :span="16">
        <a-card title="预算执行趋势">
          <div ref="trendChartRef" style="height: 350px"></div>
        </a-card>
      </a-col>
      <!-- 状态分布 -->
      <a-col :span="8">
        <a-card title="预算状态分布">
          <div ref="statusChartRef" style="height: 350px"></div>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]" class="mt-4">
      <!-- 近期调整 -->
      <a-col :span="16">
        <a-card title="近期预算调整">
          <a-table
            :data-source="recentAdjustments"
            :columns="adjustmentColumns"
            :loading="adjustmentLoading"
            :pagination="false"
            row-key="id"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="adjustmentStatusColor(record.status)">{{ adjustmentStatusText(record.status) }}</a-tag>
              </template>
              <template v-else-if="column.key === 'amount'">
                ¥{{ record.amount?.toFixed(2) }}
              </template>
              <template v-else-if="column.key === 'adjustmentType'">
                <a-tag :color="record.adjustmentType === 'increase' ? 'green' : record.adjustmentType === 'decrease' ? 'red' : 'blue'">
                  {{ record.adjustmentType === 'increase' ? '增加' : record.adjustmentType === 'decrease' ? '减少' : '调剂' }}
                </a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
      <!-- 快捷操作 -->
      <a-col :span="8">
        <a-card title="快捷操作">
          <a-space direction="vertical" style="width: 100%">
            <a-button type="primary" block @click="$router.push('/budget/template')">
              预算模板管理
            </a-button>
            <a-button type="primary" block ghost @click="$router.push('/budget/annual')">
              年度预算管理
            </a-button>
            <a-button type="primary" block ghost @click="$router.push('/budget/adjustment')">
              预算调整管理
            </a-button>
            <a-button type="primary" block ghost @click="$router.push('/budget/report')">
              预算报表分析
            </a-button>
          </a-space>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { budgetReportApi, budgetAdjustmentApi } from '@/api/budget'
import * as echarts from 'echarts'

const statistics = ref<any>({
  totalBudgetAmount: 0,
  totalUsedAmount: 0,
  totalRemainingAmount: 0,
  executionRate: 0,
})

const recentAdjustments = ref<any[]>([])
const adjustmentLoading = ref(false)
const trendChartRef = ref<HTMLDivElement>()
const statusChartRef = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null
let statusChart: echarts.ECharts | null = null

const adjustmentColumns = [
  { title: '调整单号', dataIndex: 'adjustmentNo', key: 'adjustmentNo' },
  { title: '类型', dataIndex: 'adjustmentType', key: 'adjustmentType' },
  { title: '金额', dataIndex: 'amount', key: 'amount', align: 'right' as const },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '申请日期', dataIndex: 'applyDate', key: 'applyDate' },
]

const adjustmentStatusColor = (status: string) => {
  const map: Record<string, string> = { draft: 'default', submitted: 'blue', approved: 'green', rejected: 'red' }
  return map[status] || 'default'
}
const adjustmentStatusText = (status: string) => {
  const map: Record<string, string> = { draft: '草稿', submitted: '待审批', approved: '已通过', rejected: '已拒绝' }
  return map[status] || status
}

const initTrendChart = () => {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['预算使用金额'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'] },
    yAxis: { type: 'value', name: '金额 (元)' },
    series: [{ name: '预算使用金额', type: 'line', smooth: true, data: [], areaStyle: { opacity: 0.3 } }],
  })
}

const initStatusChart = () => {
  if (!statusChartRef.value) return
  statusChart = echarts.init(statusChartRef.value)
  statusChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: '0%' },
    series: [{
      name: '预算状态',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: '14', fontWeight: 'bold' } },
      data: [
        { value: 0, name: '执行中', itemStyle: { color: '#1890ff' } },
        { value: 0, name: '已关闭', itemStyle: { color: '#52c41a' } },
        { value: 0, name: '草稿', itemStyle: { color: '#d9d9d9' } },
        { value: 0, name: '其他', itemStyle: { color: '#faad14' } },
      ],
    }],
  })
}

const loadData = async () => {
  try {
    const summaryRes = await budgetReportApi.executionSummary()
    if (summaryRes.success) {
      statistics.value = summaryRes.data
      statusChart?.setOption({
        series: [{ data: [
          { value: summaryRes.data.executingCount || 0, name: '执行中' },
          { value: summaryRes.data.closedCount || 0, name: '已关闭' },
          { value: summaryRes.data.draftCount || 0, name: '草稿' },
          { value: Math.max(0, (summaryRes.data.totalBudgetCount || 0) - (summaryRes.data.executingCount || 0) - (summaryRes.data.closedCount || 0) - (summaryRes.data.draftCount || 0)), name: '其他' },
        ]}],
      })
    }
  } catch (_) { /* ignore */ }

  try {
    const trendRes = await budgetReportApi.trend()
    if (trendRes.success && trendRes.data) {
      const months = trendRes.data.map((d: any) => d.month + '月')
      const amounts = trendRes.data.map((d: any) => d.amount)
      trendChart?.setOption({ xAxis: { data: months }, series: [{ data: amounts }] })
    }
  } catch (_) { /* ignore */ }

  adjustmentLoading.value = true
  try {
    const adjRes = await budgetAdjustmentApi.page({ pageNum: 0, pageSize: 10 })
    if (adjRes.success) {
      recentAdjustments.value = adjRes.data.records || []
    }
  } catch (_) { /* ignore */ }
  adjustmentLoading.value = false
}

onMounted(() => {
  setTimeout(() => {
    initTrendChart()
    initStatusChart()
    loadData()
  }, 100)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  statusChart?.dispose()
})

const handleResize = () => {
  trendChart?.resize()
  statusChart?.resize()
}
</script>

<style scoped>
.budget-dashboard { padding: 16px; }
.mt-4 { margin-top: 16px; }
</style>
