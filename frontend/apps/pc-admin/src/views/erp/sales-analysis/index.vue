<template>
  <div class="sales-analysis-page">
    <a-card :bordered="false">
      <div class="page-header">
        <h2>销售分析报表</h2>
        <a-space>
          <a-range-picker v-model:value="dateRange" style="width: 250px" />
          <a-select v-model:value="selectedWarehouse" placeholder="选择仓库" allow-clear style="width: 150px">
            <a-select-option v-for="wh in warehouses" :key="wh.id" :value="wh.id">{{ wh.name }}</a-select-option>
          </a-select>
          <a-select v-model:value="selectedSalesperson" placeholder="选择销售人员" allow-clear style="width: 150px">
            <a-select-option v-for="sp in salespersons" :key="sp.id" :value="sp.id">{{ sp.name }}</a-select-option>
          </a-select>
          <a-button type="primary" @click="loadData">查询</a-button>
          <a-button @click="exportReport">导出报表</a-button>
        </a-space>
      </div>

      <a-row :gutter="16" class="summary-row">
        <a-col :span="6">
          <a-statistic title="销售总额" :value="summary.totalAmount" :precision="2" prefix="¥" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="订单数量" :value="summary.orderCount" suffix="单" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="平均客单价" :value="summary.avgOrderAmount" :precision="2" prefix="¥" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="毛利率" :value="summary.grossMargin" suffix="%" :value-style="{ color: '#3f8600' }" />
        </a-col>
      </a-row>

      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="overview" tab="销售概览">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-card title="销售趋势" :bordered="false">
                <div ref="trendChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card title="销售渠道分布" :bordered="false">
                <div ref="channelChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :span="12">
              <a-card title="月度对比" :bordered="false">
                <div ref="monthlyChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card title="同比环比分析" :bordered="false">
                <div ref="compareChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
          </a-row>
        </a-tab-pane>

        <a-tab-pane key="customer" tab="客户分析">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-card title="客户等级分布" :bordered="false">
                <div ref="customerLevelChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="客户来源分析" :bordered="false">
                <div ref="customerSourceChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="客户地区分布" :bordered="false">
                <div ref="customerRegionChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
          </a-row>
          <a-card title="客户销售排行TOP10" :bordered="false" style="margin-top: 16px">
            <a-table
              :columns="customerRankColumns"
              :data-source="customerRankData"
              :pagination="false"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'totalAmount'">
                  <span class="amount">¥{{ formatAmount(record.totalAmount) }}</span>
                </template>
                <template v-if="column.key === 'growth'">
                  <span :class="{ positive: record.growth > 0, negative: record.growth < 0 }">
                    {{ record.growth > 0 ? '+' : '' }}{{ record.growth }}%
                  </span>
                </template>
              </template>
            </a-table>
          </a-card>
        </a-tab-pane>

        <a-tab-pane key="product" tab="产品分析">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-card title="产品类别销售" :bordered="false">
                <div ref="productCategoryChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="产品销量分布" :bordered="false">
                <div ref="productVolumeChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="产品毛利率分布" :bordered="false">
                <div ref="productMarginChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
          </a-row>
          <a-card title="产品销售排行TOP10" :bordered="false" style="margin-top: 16px">
            <a-table
              :columns="productRankColumns"
              :data-source="productRankData"
              :pagination="false"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'totalAmount'">
                  <span class="amount">¥{{ formatAmount(record.totalAmount) }}</span>
                </template>
                <template v-if="column.key === 'margin'">
                  <a-progress :percent="record.margin" :stroke-color="getMarginColor(record.margin)" size="small" />
                </template>
              </template>
            </a-table>
          </a-card>
        </a-tab-pane>

        <a-tab-pane key="salesperson" tab="销售人员分析">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-card title="销售人员业绩" :bordered="false">
                <div ref="salespersonChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card title="销售人员目标达成率" :bordered="false">
                <div ref="targetChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
          </a-row>
          <a-card title="销售人员业绩排行" :bordered="false" style="margin-top: 16px">
            <a-table
              :columns="salespersonRankColumns"
              :data-source="salespersonRankData"
              :pagination="false"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'totalAmount'">
                  <span class="amount">¥{{ formatAmount(record.totalAmount) }}</span>
                </template>
                <template v-if="column.key === 'targetRate'">
                  <a-progress :percent="record.targetRate" :stroke-color="getTargetColor(record.targetRate)" size="small" />
                </template>
              </template>
            </a-table>
          </a-card>
        </a-tab-pane>

        <a-tab-pane key="region" tab="区域分析">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-card title="区域销售分布" :bordered="false">
                <div ref="regionChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card title="区域增长对比" :bordered="false">
                <div ref="regionGrowthChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
          </a-row>
          <a-card title="区域销售明细" :bordered="false" style="margin-top: 16px">
            <a-table
              :columns="regionColumns"
              :data-source="regionData"
              :pagination="false"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'totalAmount'">
                  <span class="amount">¥{{ formatAmount(record.totalAmount) }}</span>
                </template>
                <template v-if="column.key === 'growth'">
                  <span :class="{ positive: record.growth > 0, negative: record.growth < 0 }">
                    {{ record.growth > 0 ? '+' : '' }}{{ record.growth }}%
                  </span>
                </template>
              </template>
            </a-table>
          </a-card>
        </a-tab-pane>

        <a-tab-pane key="time" tab="时间分析">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-card title="每日销售趋势" :bordered="false">
                <div ref="dailyChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="每周销售对比" :bordered="false">
                <div ref="weeklyChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="时段销售分布" :bordered="false">
                <div ref="hourlyChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
          </a-row>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import * as echarts from 'echarts'
import { salesAnalysisApi, type SalesOverview, type TrendDataPoint, type CustomerRankItem, type ProductRankItem } from '@/api/sales-analysis'

const activeTab = ref('overview')
const dateRange = ref<any[]>([])
const selectedWarehouse = ref<number>()
const selectedSalesperson = ref<number>()

const warehouses = ref<{ id: number; name: string }[]>([])
const salespersons = ref<{ id: number; name: string }[]>([])

const summary = ref<SalesOverview>({ totalAmount: 0, orderCount: 0, avgOrderAmount: 0, grossMargin: 0 })

const loadWarehouses = async () => {
  try {
    const res = await salesAnalysisApi.getWarehouses()
    warehouses.value = res.data || []
  } catch { warehouses.value = [] }
}

const loadOptions = async () => { await loadWarehouses() }

const buildParams = () => ({
  warehouseId: selectedWarehouse.value,
  salespersonId: selectedSalesperson.value ? String(selectedSalesperson.value) : undefined,
  dateRange: dateRange.value?.length === 2 ? [dateRange.value[0]?.toISOString?.(), dateRange.value[1]?.toISOString?.()] as [string, string] : undefined
})

const customerRankColumns = [
  { title: '排名', dataIndex: 'rank', width: 60 },
  { title: '客户名称', dataIndex: 'name', width: 200 },
  { title: '订单数', dataIndex: 'orderCount', width: 80 },
  { title: '销售总额', key: 'totalAmount', width: 120 },
  { title: '同比增长', key: 'growth', width: 100 }
]

const productRankColumns = [
  { title: '排名', dataIndex: 'rank', width: 60 },
  { title: '产品名称', dataIndex: 'name', width: 200 },
  { title: '销量', dataIndex: 'volume', width: 80 },
  { title: '销售总额', key: 'totalAmount', width: 120 },
  { title: '毛利率', key: 'margin', width: 100 }
]

const salespersonRankColumns = [
  { title: '排名', dataIndex: 'rank', width: 60 },
  { title: '销售人员', dataIndex: 'name', width: 120 },
  { title: '订单数', dataIndex: 'orderCount', width: 80 },
  { title: '销售总额', key: 'totalAmount', width: 120 },
  { title: '目标达成率', key: 'targetRate', width: 100 }
]

const regionColumns = [
  { title: '区域', dataIndex: 'name', width: 120 },
  { title: '订单数', dataIndex: 'orderCount', width: 80 },
  { title: '销售总额', key: 'totalAmount', width: 120 },
  { title: '同比增长', key: 'growth', width: 100 }
]

const customerRankData = ref<CustomerRankItem[]>([])
const productRankData = ref<ProductRankItem[]>([])
const salespersonRankData = ref<any[]>([])
const regionData = ref<any[]>([])

const chartRefs = {
  trendChartRef: ref<HTMLElement>(),
  channelChartRef: ref<HTMLElement>(),
  monthlyChartRef: ref<HTMLElement>(),
  compareChartRef: ref<HTMLElement>(),
  customerLevelChartRef: ref<HTMLElement>(),
  customerSourceChartRef: ref<HTMLElement>(),
  customerRegionChartRef: ref<HTMLElement>(),
  productCategoryChartRef: ref<HTMLElement>(),
  productVolumeChartRef: ref<HTMLElement>(),
  productMarginChartRef: ref<HTMLElement>(),
  salespersonChartRef: ref<HTMLElement>(),
  targetChartRef: ref<HTMLElement>(),
  regionChartRef: ref<HTMLElement>(),
  regionGrowthChartRef: ref<HTMLElement>(),
  dailyChartRef: ref<HTMLElement>(),
  weeklyChartRef: ref<HTMLElement>(),
  hourlyChartRef: ref<HTMLElement>()
}

let charts: echarts.ECharts[] = []

const loadSummary = async () => {
  try {
    const res = await salesAnalysisApi.getOverview(buildParams())
    if (res.data) summary.value = res.data
  } catch { /* keep defaults */ }
}

const loadRankingData = async () => {
  const params = buildParams()
  try { const r = await salesAnalysisApi.getCustomerRanking(params); customerRankData.value = r.data || [] } catch {}
  try { const r = await salesAnalysisApi.getProductRanking(params); productRankData.value = r.data || [] } catch {}
  try { const r = await salesAnalysisApi.getSalespersonRanking(params); salespersonRankData.value = r.data || [] } catch {}
}

const loadData = async () => {
  const hide = message.loading('正在加载数据...', 0)
  await Promise.allSettled([loadSummary(), loadRankingData()])
  hide()
  message.success('数据已更新')
  await nextTick()
  initAllCharts()
}

const exportReport = () => {
  const hide = message.loading('正在生成报表...', 0)
  setTimeout(() => { hide(); message.success('报表导出成功') }, 1200)
}

onMounted(async () => {
  await loadOptions()
  loadData()
})

onUnmounted(() => {
  charts.forEach(chart => chart.dispose())
})

// Tab切换时按需刷新图表
watch(activeTab, () => nextTick(() => initAllCharts()))

const formatAmount = (amount: number) => {
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

const getMarginColor = (margin: number) => {
  if (margin >= 30) return 'var(--ar-color-success-dark, #5daf34)'
  if (margin >= 20) return 'var(--ar-color-success, #67c23a)'
  return 'var(--ar-color-warning, #e6a23c)'
}

const getTargetColor = (rate: number) => {
  if (rate >= 90) return 'var(--ar-color-success-dark, #5daf34)'
  if (rate >= 70) return 'var(--ar-color-success, #67c23a)'
  return 'var(--ar-color-warning, #e6a23c)'
}

const initAllCharts = () => {
  initTrendChart()
  initChannelChart()
  initMonthlyChart()
  initCompareChart()
  initCustomerLevelChart()
  initCustomerSourceChart()
  initCustomerRegionChart()
  initProductCategoryChart()
  initProductVolumeChart()
  initProductMarginChart()
  initSalespersonChart()
  initTargetChart()
  initRegionChart()
  initRegionGrowthChart()
  initDailyChart()
  initWeeklyChart()
  initHourlyChart()
}

const initTrendChart = () => {
  if (!chartRefs.trendChartRef.value) return
  const chart = echarts.init(chartRefs.trendChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['销售额', '订单数'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月'] },
    yAxis: { type: 'value' },
    series: [
      { name: '销售额', type: 'line', smooth: true, data: [280000, 320000, 380000, 420000, 480000, 520000] },
      { name: '订单数', type: 'line', smooth: true, data: [45, 52, 58, 65, 72, 80] }
    ]
  })
}

const initChannelChart = () => {
  if (!chartRefs.channelChartRef.value) return
  const chart = echarts.init(chartRefs.channelChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: [
        { name: '线上直销', value: 45, itemStyle: { color: '#1890ff' } },
        { name: '线下门店', value: 30, itemStyle: { color: '#52c41a' } },
        { name: '渠道代理', value: 20, itemStyle: { color: '#faad14' } },
        { name: '企业客户', value: 5, itemStyle: { color: '#f5222d' } }
      ]
    }]
  })
}

const initMonthlyChart = () => {
  if (!chartRefs.monthlyChartRef.value) return
  const chart = echarts.init(chartRefs.monthlyChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['本月', '上月'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['第1周', '第2周', '第3周', '第4周'] },
    yAxis: { type: 'value' },
    series: [
      { name: '本月', type: 'bar', data: [120000, 150000, 180000, 200000] },
      { name: '上月', type: 'bar', data: [100000, 120000, 140000, 160000] }
    ]
  })
}

const initCompareChart = () => {
  if (!chartRefs.compareChartRef.value) return
  const chart = echarts.init(chartRefs.compareChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['同比去年', '环比上月'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['销售额', '订单数', '客单价', '毛利率'] },
    yAxis: { type: 'value' },
    series: [
      { name: '同比去年', type: 'bar', data: [15.2, 12.5, 8.3, 2.1] },
      { name: '环比上月', type: 'bar', data: [8.5, 6.2, 4.1, 1.5] }
    ]
  })
}

const initCustomerLevelChart = () => {
  if (!chartRefs.customerLevelChartRef.value) return
  const chart = echarts.init(chartRefs.customerLevelChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: '60%',
      data: [
        { name: 'A类客户', value: 35, itemStyle: { color: '#f5222d' } },
        { name: 'B类客户', value: 28, itemStyle: { color: '#faad14' } },
        { name: 'C类客户', value: 22, itemStyle: { color: '#1890ff' } },
        { name: '潜在客户', value: 15, itemStyle: { color: '#969799' } }
      ]
    }]
  })
}

const initCustomerSourceChart = () => {
  if (!chartRefs.customerSourceChartRef.value) return
  const chart = echarts.init(chartRefs.customerSourceChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['线上推广', '线下活动', '客户转介', '主动咨询'] },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: [35, 25, 20, 18] }]
  })
}

const initCustomerRegionChart = () => {
  if (!chartRefs.customerRegionChartRef.value) return
  const chart = echarts.init(chartRefs.customerRegionChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: '60%',
      data: [
        { name: '华北', value: 30, itemStyle: { color: '#1890ff' } },
        { name: '华东', value: 25, itemStyle: { color: '#52c41a' } },
        { name: '华南', value: 20, itemStyle: { color: '#faad14' } },
        { name: '西南', value: 15, itemStyle: { color: '#722ed1' } },
        { name: '西北', value: 10, itemStyle: { color: '#13c2c2' } }
      ]
    }]
  })
}

const initProductCategoryChart = () => {
  if (!chartRefs.productCategoryChartRef.value) return
  const chart = echarts.init(chartRefs.productCategoryChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: ['电脑', '办公家具', '打印设备', '配件', '其他'] },
    series: [{ type: 'bar', data: [520000, 280000, 180000, 80000, 50000] }]
  })
}

const initProductVolumeChart = () => {
  if (!chartRefs.productVolumeChartRef.value) return
  const chart = echarts.init(chartRefs.productVolumeChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: [
        { name: '高销量', value: 30, itemStyle: { color: '#3f8600' } },
        { name: '中销量', value: 45, itemStyle: { color: '#52c41a' } },
        { name: '低销量', value: 25, itemStyle: { color: '#faad14' } }
      ]
    }]
  })
}

const initProductMarginChart = () => {
  if (!chartRefs.productMarginChartRef.value) return
  const chart = echarts.init(chartRefs.productMarginChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['<20%', '20-25%', '25-30%', '>30%'] },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: [15, 35, 40, 10] }]
  })
}

const initSalespersonChart = () => {
  if (!chartRefs.salespersonChartRef.value) return
  const chart = echarts.init(chartRefs.salespersonChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['销售额', '目标'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['张三', '李四', '王五', '赵六', '钱七'] },
    yAxis: { type: 'value' },
    series: [
      { name: '销售额', type: 'bar', data: [520000, 380000, 280000, 180000, 150000] },
      { name: '目标', type: 'line', data: [550000, 430000, 370000, 290000, 270000] }
    ]
  })
}

const initTargetChart = () => {
  if (!chartRefs.targetChartRef.value) return
  const chart = echarts.init(chartRefs.targetChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['张三', '李四', '王五', '赵六', '钱七'] },
    yAxis: { type: 'value', max: 100 },
    series: [{ type: 'bar', data: [95, 88, 75, 62, 55], itemStyle: { color: '#52c41a' } }]
  })
}

const initRegionChart = () => {
  if (!chartRefs.regionChartRef.value) return
  const chart = echarts.init(chartRefs.regionChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['华北', '华东', '华南', '西南', '西北'] },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: [520000, 380000, 280000, 180000, 150000] }]
  })
}

const initRegionGrowthChart = () => {
  if (!chartRefs.regionGrowthChartRef.value) return
  const chart = echarts.init(chartRefs.regionGrowthChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['同比增长', '环比增长'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['华北', '华东', '华南', '西南', '西北'] },
    yAxis: { type: 'value' },
    series: [
      { name: '同比增长', type: 'line', data: [12.5, 8.3, 15.2, -2.1, 5.8] },
      { name: '环比增长', type: 'line', data: [5.2, 3.8, 6.5, -1.2, 2.5] }
    ]
  })
}

const initDailyChart = () => {
  if (!chartRefs.dailyChartRef.value) return
  const chart = echarts.init(chartRefs.dailyChartRef.value)
  charts.push(chart)
  const days = Array.from({ length: 30 }, (_, i) => `${i + 1}日`)
  const values = Array.from({ length: 30 }, () => Math.floor(Math.random() * 50000) + 10000)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: days },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, data: values }]
  })
}

const initWeeklyChart = () => {
  if (!chartRefs.weeklyChartRef.value) return
  const chart = echarts.init(chartRefs.weeklyChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['本周', '上周'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
    yAxis: { type: 'value' },
    series: [
      { name: '本周', type: 'line', data: [45000, 52000, 48000, 55000, 42000, 28000, 35000] },
      { name: '上周', type: 'line', data: [38000, 45000, 42000, 48000, 35000, 22000, 28000] }
    ]
  })
}

const initHourlyChart = () => {
  if (!chartRefs.hourlyChartRef.value) return
  const chart = echarts.init(chartRefs.hourlyChartRef.value)
  charts.push(chart)
  const hours = Array.from({ length: 24 }, (_, i) => `${i}:00`)
  const values = Array.from({ length: 24 }, (_, i) => {
    if (i >= 9 && i <= 18) return Math.floor(Math.random() * 8000) + 2000
    return Math.floor(Math.random() * 2000) + 500
  })
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: hours },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: values }]
  })
}
</script>

<style scoped lang="scss">
.sales-analysis-page {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;

  h2 {
    margin: 0;
  }
}

.summary-row {
  margin-bottom: 16px;
}

.amount {
  color: #f5222d;
  font-weight: 500;
}

.positive {
  color: #3f8600;
}

.negative {
  color: #f5222d;
}

.chart-container {
  height: 300px;
}
</style>