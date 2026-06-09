<template>
  <div class="customer-ranking">
    <!-- 统计卡片 -->
    <div class="summary-cards">
      <div class="summary-card" style="--card-color: #1890ff;">
        <div class="summary-card-title">客户总数</div>
        <div class="summary-card-value">{{ summary.totalCustomers }}</div>
      </div>
      <div class="summary-card" style="--card-color: #faad14;">
        <div class="summary-card-title">销售总额</div>
        <div class="summary-card-value">¥{{ formatAmount(summary.totalAmount) }}</div>
      </div>
      <div class="summary-card" style="--card-color: #52c41a;">
        <div class="summary-card-title">平均订单数</div>
        <div class="summary-card-value">{{ summary.avgOrders }}</div>
      </div>
      <div class="summary-card" style="--card-color: #722ed1;">
        <div class="summary-card-title">平均增长率</div>
        <div class="summary-card-value">{{ summary.avgGrowth }}%</div>
      </div>
    </div>

    <!-- 筛选区 -->
    <a-collapse v-model:activeKey="filterExpanded" class="filter-collapse">
      <a-collapse-panel key="1" header="筛选条件">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-form-item label="时间范围">
              <a-range-picker
                v-model:value="queryParams.dateRange"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="客户类型">
              <a-select
                v-model:value="queryParams.customerType"
                placeholder="全部类型"
                allow-clear
                style="width: 100%"
              >
                <a-select-option value="A">A类客户</a-select-option>
                <a-select-option value="B">B类客户</a-select-option>
                <a-select-option value="C">C类客户</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="排名范围">
              <a-select
                v-model:value="queryParams.rankLimit"
                placeholder="全部"
                style="width: 100%"
              >
                <a-select-option :value="10">TOP 10</a-select-option>
                <a-select-option :value="20">TOP 20</a-select-option>
                <a-select-option :value="50">TOP 50</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="6" class="filter-actions">
            <a-space>
              <a-button type="primary" :loading="loading" @click="handleQuery">查询</a-button>
              <a-button @click="handleReset">重置</a-button>
              <a-button @click="handleExport">
                <template #icon><ExportOutlined /></template>
                导出
              </a-button>
            </a-space>
          </a-col>
        </a-row>
      </a-collapse-panel>
    </a-collapse>

    <!-- 排行表格 -->
    <a-card title="客户销售排行榜" size="small">
      <template #extra>
        <a-space>
          <span class="total-info">共 {{ dataSource.length }} 个客户</span>
          <a-tooltip title="切换显示模式">
            <a-switch v-model:checked="showChart" size="small">
              <template #checkedChildren>图表</template>
              <template #unCheckedChildren>列表</template>
            </a-switch>
          </a-tooltip>
        </a-space>
      </template>

      <!-- 图表模式 -->
      <div v-if="showChart" ref="chartRef" class="chart-container"></div>

      <!-- 表格模式 -->
      <div v-else class="table-container">
        <VxeTableList
          :columns="vxeColumns"
          :data-source="dataSource"
          :loading="loading"
          :pagination="false"
          row-key="id"
          :show-toolbar="false"
          :selectable="false"
          :show-add="false"
          :show-search="false"
          :show-export="false"
          :show-batch-delete="false"
        >
          <template #rankCell="{ record }">
            <a-tag :color="getRankColor(record.rank)" size="small">
              TOP {{ record.rank }}
            </a-tag>
          </template>
          <template #nameCell="{ record }">
            <a @click="handleViewCustomer(record)">{{ record.name }}</a>
          </template>
          <template #totalAmountCell="{ record }">
            <span class="amount-cell">¥{{ formatAmount(record.totalAmount) }}</span>
          </template>
          <template #growthCell="{ record }">
            <span :class="['growth-cell', { positive: record.growth > 0, negative: record.growth < 0 }]">
              <ArrowUpOutlined v-if="record.growth > 0" />
              <ArrowDownOutlined v-if="record.growth < 0" />
              {{ Math.abs(record.growth) }}%
            </span>
          </template>
          <template #customerTypeCell="{ record }">
            <a-tag :color="getCustomerTypeColor(record.customerType)">
              {{ record.customerType }}类
            </a-tag>
          </template>
        </VxeTableList>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import * as echarts from 'echarts'
import {
  ExportOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined
} from '@ant-design/icons-vue'
import { salesReportApi, type CustomerRankItem } from '@/api/sales-report'

const loading = ref(false)
const filterExpanded = ref<string[]>([])
const showChart = ref(false)
const dataSource = ref<CustomerRankItem[]>([])
const chartRef = ref<HTMLElement>()

let chart: echarts.ECharts | null = null

const queryParams = reactive({
  dateRange: [] as string[],
  customerType: undefined as string | undefined,
  rankLimit: 10
})

const vxeColumns = [
  { field: 'rank', title: '排名', width: 80, align: 'center', slotName: 'rankCell' },
  { field: 'name', title: '客户名称', width: 180, slotName: 'nameCell' },
  { field: 'customerType', title: '客户类型', width: 100, align: 'center', slotName: 'customerTypeCell' },
  { field: 'totalAmount', title: '销售总额', width: 140, align: 'right', slotName: 'totalAmountCell' },
  { field: 'orderCount', title: '订单数量', width: 100, align: 'right' },
  { field: 'growth', title: '同比增长', width: 100, align: 'right', slotName: 'growthCell' }
]

// 统计数据
const summary = computed(() => {
  const data = dataSource.value
  const totalCustomers = data.length
  const totalAmount = data.reduce((sum, item) => sum + (item.totalAmount || 0), 0)
  const avgOrders = totalCustomers > 0 ? (data.reduce((sum, item) => sum + (item.orderCount || 0), 0) / totalCustomers).toFixed(1) : '0.0'
  const avgGrowth = totalCustomers > 0 ? (data.reduce((sum, item) => sum + (item.growth || 0), 0) / totalCustomers).toFixed(1) : '0.0'
  return { totalCustomers, totalAmount, avgOrders, avgGrowth }
})

// 统计数据修正 - 使用 dataSource


const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const getRankColor = (rank: number) => {
  if (rank === 1) return 'gold'
  if (rank === 2) return '#cd7f32'
  if (rank === 3) return '#b87333'
  return 'default'
}

const getCustomerTypeColor = (type: string) => {
  if (type === 'A') return 'red'
  if (type === 'B') return 'orange'
  if (type === 'C') return 'blue'
  return 'default'
}

const buildParams = () => {
  const params: any = {}
  if (queryParams.dateRange.length === 2) {
    params.startDate = queryParams.dateRange[0]
    params.endDate = queryParams.dateRange[1]
  }
  if (queryParams.customerType) params.customerType = queryParams.customerType
  params.limit = queryParams.rankLimit || 10
  return params
}

const handleQuery = async () => {
  loading.value = true
  try {
    const res = await salesReportApi.getCustomerRanking(buildParams())
    dataSource.value = res.data || mockData()
  } catch {
    dataSource.value = mockData()
  } finally {
    loading.value = false
    if (showChart.value) {
      nextTick(() => initChart())
    }
  }
}

const handleReset = () => {
  queryParams.dateRange = []
  queryParams.customerType = undefined
  queryParams.rankLimit = 10
  handleQuery()
}

const handleExport = () => {
  const csvData = dataSource.value.map(item =>
    `${item.rank},${item.name},${item.customerType},${item.totalAmount},${item.orderCount},${item.growth}`
  )
  const csv = ['排名,客户名称,客户类型,销售总额,订单数量,同比增长', ...csvData].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `客户销售排行_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

const handleViewCustomer = (record: CustomerRankItem) => {
  message.info(`查看客户 ${record.name} 详情`)
}

const mockData = (): CustomerRankItem[] => [
  { id: 1, rank: 1, name: '北京科技有限公司', customerType: 'A', totalAmount: 520000, orderCount: 85, growth: 18.5 },
  { id: 2, rank: 2, name: '上海贸易集团有限公司', customerType: 'A', totalAmount: 380000, orderCount: 72, growth: 12.3 },
  { id: 3, rank: 3, name: '广州制造有限公司', customerType: 'B', totalAmount: 280000, orderCount: 65, growth: 8.2 },
  { id: 4, rank: 4, name: '深圳创新科技有限公司', customerType: 'B', totalAmount: 220000, orderCount: 58, growth: 15.1 },
  { id: 5, rank: 5, name: '杭州互联网有限公司', customerType: 'C', totalAmount: 180000, orderCount: 52, growth: -2.5 },
  { id: 6, rank: 6, name: '成都科技发展有限公司', customerType: 'B', totalAmount: 150000, orderCount: 45, growth: 5.8 },
  { id: 7, rank: 7, name: '武汉智能制造有限公司', customerType: 'C', totalAmount: 120000, orderCount: 38, growth: 3.2 },
  { id: 8, rank: 8, name: '南京电子科技有限公司', customerType: 'C', totalAmount: 95000, orderCount: 32, growth: -1.8 }
]

const initChart = () => {
  if (!chartRef.value) return
  if (chart) chart.dispose()
  chart = echarts.init(chartRef.value)
  const names = dataSource.value.map(d => d.name)
  const amounts = dataSource.value.map(d => d.totalAmount)
  const growths = dataSource.value.map(d => d.growth)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['销售总额', '同比增长'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: names, axisLabel: { interval: 0, rotate: 30, fontSize: 11 } },
    yAxis: [
      { type: 'value', name: '销售额(万)', axisLabel: { formatter: (v: number) => `${v / 10000}` } },
      { type: 'value', name: '增长率(%)' }
    ],
    series: [
      { name: '销售总额', type: 'bar', data: amounts, itemStyle: { color: '#1890ff' } },
      { name: '同比增长', type: 'line', yAxisIndex: 1, data: growths, itemStyle: { color: '#52c41a' } }
    ]
  })
  window.addEventListener('resize', () => chart?.resize())
}

onMounted(() => handleQuery())
onUnmounted(() => {
  chart?.dispose()
  chart = null
})

defineExpose({ handleQuery })
</script>

<style scoped>
.customer-ranking {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  overflow-y: auto;
}

/* 统计卡片 */
.summary-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  flex-shrink: 0;
}

.summary-card {
  background: linear-gradient(135deg, var(--card-color), color-mix(in srgb, var(--card-color) 70%, #fff));
  border-radius: 8px;
  padding: 16px 20px;
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.summary-card-title {
  font-size: 13px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.summary-card-value {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-size: 24px;
  font-weight: 600;
}

.filter-collapse {
  flex-shrink: 0;
}

.filter-actions {
  display: flex;
  align-items: flex-end;
  padding-bottom: 4px;
}

.total-info {
  font-size: 12px;
  color: #666;
}

.table-container {
  max-height: 400px;
  overflow-y: auto;
}

.chart-container {
  height: 350px;
}





.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

.growth-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.growth-cell.positive {
  color: #52c41a;
}

.growth-cell.negative {
  color: #f5222d;
}

</style>
