<template>
  <div class="product-ranking">
    <!-- 统计卡片 -->
    <div class="summary-cards">
      <div class="summary-card" style="--card-color: #1890ff;">
        <div class="summary-card-title">商品总数</div>
        <div class="summary-card-value">{{ summary.totalProducts }}</div>
      </div>
      <div class="summary-card" style="--card-color: #faad14;">
        <div class="summary-card-title">销售总额</div>
        <div class="summary-card-value">¥{{ formatAmount(summary.totalAmount) }}</div>
      </div>
      <div class="summary-card" style="--card-color: #52c41a;">
        <div class="summary-card-title">总销量</div>
        <div class="summary-card-value">{{ summary.totalVolume }}</div>
      </div>
      <div class="summary-card" style="--card-color: #722ed1;">
        <div class="summary-card-title">平均毛利率</div>
        <div class="summary-card-value">{{ summary.avgMargin }}%</div>
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
            <a-form-item label="商品类别">
              <a-select
                v-model:value="queryParams.category"
                placeholder="全部类别"
                allow-clear
                style="width: 100%"
              >
                <a-select-option value="electronics">电子产品</a-select-option>
                <a-select-option value="furniture">办公家具</a-select-option>
                <a-select-option value="supplies">办公耗材</a-select-option>
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
    <a-card title="商品销售排行榜" size="small">
      <template #extra>
        <a-space>
          <span class="total-info">共 {{ dataSource.length }} 个商品</span>
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
            <span>{{ record.name }}</span>
            <span v-if="record.code" class="product-code">({{ record.code }})</span>
          </template>
          <template #totalAmountCell="{ record }">
            <span class="amount-cell">¥{{ formatAmount(record.totalAmount) }}</span>
          </template>
          <template #volumeCell="{ record }">
            <span class="number-cell">{{ record.volume }}</span>
          </template>
          <template #marginCell="{ record }">
            <a-progress
              :percent="record.margin"
              :stroke-color="getMarginColor(record.margin)"
              size="small"
              :format="(p: number) => `${p}%`"
            />
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
import { ExportOutlined } from '@ant-design/icons-vue'
import { salesReportApi, type ProductRankItem } from '@/api/sales-report'

const loading = ref(false)
const filterExpanded = ref<string[]>([])
const showChart = ref(false)
const dataSource = ref<ProductRankItem[]>([])
const chartRef = ref<HTMLElement>()

let chart: echarts.ECharts | null = null

const queryParams = reactive({
  dateRange: [] as string[],
  category: undefined as string | undefined,
  rankLimit: 10
})

const vxeColumns = [
  { field: 'rank', title: '排名', width: 80, align: 'center', slotName: 'rankCell' },
  { field: 'name', title: '商品名称', width: 200, slotName: 'nameCell' },
  { field: 'totalAmount', title: '销售总额', width: 140, align: 'right', slotName: 'totalAmountCell' },
  { field: 'volume', title: '销量', width: 100, align: 'right', slotName: 'volumeCell' },
  { field: 'margin', title: '毛利率', width: 150, slotName: 'marginCell' }
]

// 统计数据
const summary = computed(() => {
  const data = dataSource.value
  const totalProducts = data.length
  const totalAmount = data.reduce((sum, item) => sum + (item.totalAmount || 0), 0)
  const totalVolume = data.reduce((sum, item) => sum + (item.volume || 0), 0)
  const avgMargin = totalProducts > 0 ? (data.reduce((sum, item) => sum + (item.margin || 0), 0) / totalProducts).toFixed(1) : '0.0'
  return { totalProducts, totalAmount, totalVolume, avgMargin }
})

// 数据源已直接使用 dataSource


const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

const getRankColor = (rank: number) => {
  if (rank === 1) return 'gold'
  if (rank === 2) return '#cd7f32'
  if (rank === 3) return '#b87333'
  return 'default'
}

const getMarginColor = (margin: number) => {
  if (margin >= 30) return '#52c41a'
  if (margin >= 20) return '#1890ff'
  return '#faad14'
}

const buildParams = () => {
  const params: any = {}
  if (queryParams.dateRange.length === 2) {
    params.startDate = queryParams.dateRange[0]
    params.endDate = queryParams.dateRange[1]
  }
  if (queryParams.category) params.category = queryParams.category
  params.limit = queryParams.rankLimit || 10
  return params
}

const handleQuery = async () => {
  loading.value = true
  try {
    const res = await salesReportApi.getProductRanking(buildParams())
    dataSource.value = res.data || []
  } catch (err) {
    dataSource.value = []
  } finally {
    loading.value = false
    if (showChart.value) {
      nextTick(() => initChart())
    }
  }
}

const handleReset = () => {
  queryParams.dateRange = []
  queryParams.category = undefined
  queryParams.rankLimit = 10
  handleQuery()
}

const handleExport = () => {
  const csvData = dataSource.value.map(item =>
    `${item.rank},${item.name},${item.code || ''},${item.totalAmount},${item.volume},${item.margin}`
  )
  const csv = ['排名,商品名称,商品编码,销售总额,销量,毛利率', ...csvData].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
  const url = window.URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `商品销售排行_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  window.URL.revokeObjectURL(url)
  message.success('导出成功')
}

const initChart = () => {
  if (!chartRef.value) return
  if (chart) chart.dispose()
  chart = echarts.init(chartRef.value)
  const names = dataSource.value.map(d => d.name)
  const amounts = dataSource.value.map(d => d.totalAmount)
  const volumes = dataSource.value.map(d => d.volume)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['销售总额', '销量'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: names, axisLabel: { interval: 0, rotate: 30, fontSize: 11 } },
    yAxis: [
      { type: 'value', name: '销售额(万)', axisLabel: { formatter: (v: number) => `${v / 10000}` } },
      { type: 'value', name: '销量' }
    ],
    series: [
      { name: '销售总额', type: 'bar', data: amounts, itemStyle: { color: '#1890ff' } },
      { name: '销量', type: 'line', yAxisIndex: 1, data: volumes, itemStyle: { color: '#52c41a' } }
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
.product-ranking {
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

.number-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}

.product-code {
  font-size: 12px;
  color: #999;
  margin-left: 4px;
}

</style>
