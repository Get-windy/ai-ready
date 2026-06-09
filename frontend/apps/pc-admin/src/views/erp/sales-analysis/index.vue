<template>
  <PageContainer title="销售分析报表" full-height>
    <!-- 状态栏 -->
    <template #headerExtra>
      <a-space :size="12">
        <span class="data-status">
          <a-badge :status="loading ? 'processing' : 'success'" />
          <span v-if="lastUpdateTime" class="update-time">
            数据更新: {{ lastUpdateTime }}
          </span>
        </span>
        <a-tooltip title="自动刷新 (每60秒)">
          <a-switch v-model:checked="autoRefresh" size="small" />
        </a-tooltip>
        <a-button size="small" @click="loadData">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </template>

    <!-- 筛选区 -->
    <template #filter>
      <a-collapse v-model:activeKey="filterExpanded" class="filter-collapse">
        <a-collapse-panel key="1" header="筛选条件">
          <a-row :gutter="16">
            <a-col :span="6">
              <a-form-item label="日期范围">
                <a-range-picker
                  v-model:value="dateRange"
                  style="width: 100%"
                  :placeholder="['开始日期', '结束日期']"
                />
              </a-form-item>
            </a-col>
            <a-col :span="4">
              <a-form-item label="仓库">
                <a-select
                  v-model:value="selectedWarehouse"
                  placeholder="全部仓库"
                  allow-clear
                  style="width: 100%"
                >
                  <a-select-option v-for="wh in warehouses" :key="wh.id" :value="wh.id">
                    {{ wh.name }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="4">
              <a-form-item label="销售人员">
                <a-select
                  v-model:value="selectedSalesperson"
                  placeholder="全部人员"
                  allow-clear
                  style="width: 100%"
                >
                  <a-select-option v-for="sp in salespersons" :key="sp.id" :value="sp.id">
                    {{ sp.name }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="4">
              <a-form-item label="客户类型">
                <a-select
                  v-model:value="selectedCustomerType"
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
            <a-col :span="6" class="filter-actions">
              <a-space>
                <a-button type="primary" :loading="loading" @click="loadData">查询</a-button>
                <a-button @click="handleResetFilter">重置</a-button>
                <a-button @click="exportReport">
                  <template #icon><ExportOutlined /></template>
                  导出
                </a-button>
              </a-space>
            </a-col>
          </a-row>
        </a-collapse-panel>
      </a-collapse>
    </template>

    <!-- 汇总统计卡片 -->
    <div class="summary-cards">
      <a-row :gutter="16">
        <a-col :span="6">
          <div class="summary-card">
            <div class="summary-icon" style="background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);">
              <DollarOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">销售总额</div>
              <div class="summary-value">¥{{ formatAmount(summary.totalAmount) }}</div>
              <div class="summary-change positive" v-if="summary.totalGrowth">
                <ArrowUpOutlined /> {{ summary.totalGrowth }}%
              </div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="summary-card">
            <div class="summary-icon" style="background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);">
              <ShoppingOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">订单数量</div>
              <div class="summary-value">{{ summary.orderCount }} 单</div>
              <div class="summary-change positive" v-if="summary.orderGrowth">
                <ArrowUpOutlined /> {{ summary.orderGrowth }}%
              </div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="summary-card">
            <div class="summary-icon" style="background: linear-gradient(135deg, #722ed1 0%, #531dab 100%);">
              <TeamOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">平均客单价</div>
              <div class="summary-value">¥{{ formatAmount(summary.avgOrderAmount) }}</div>
              <div class="summary-change positive" v-if="summary.avgGrowth">
                <ArrowUpOutlined /> {{ summary.avgGrowth }}%
              </div>
            </div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="summary-card highlight">
            <div class="summary-icon" style="background: linear-gradient(135deg, #faad14 0%, #d48806 100%);">
              <PercentageOutlined />
            </div>
            <div class="summary-content">
              <div class="summary-title">毛利率</div>
              <div class="summary-value">{{ summary.grossMargin.toFixed(1) }}%</div>
              <div class="summary-change positive" v-if="summary.marginGrowth">
                <ArrowUpOutlined /> {{ summary.marginGrowth }}%
              </div>
            </div>
          </div>
        </a-col>
      </a-row>
    </div>

    <!-- Tab 内容区 -->
    <div class="tab-content">
      <a-tabs v-model:activeKey="activeTab" type="card" size="small">
        <a-tab-pane key="overview" tab="销售概览">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-card title="销售趋势" size="small" :loading="chartLoading">
                <div ref="trendChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card title="销售渠道分布" size="small" :loading="chartLoading">
                <div ref="channelChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
          </a-row>
          <a-row :gutter="16" style="margin-top: 16px">
            <a-col :span="12">
              <a-card title="月度对比" size="small" :loading="chartLoading">
                <div ref="monthlyChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card title="同比环比分析" size="small" :loading="chartLoading">
                <div ref="compareChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
          </a-row>
        </a-tab-pane>

        <a-tab-pane key="customer" tab="客户分析">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-card title="客户等级分布" size="small" :loading="chartLoading">
                <div ref="customerLevelChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="客户来源分析" size="small" :loading="chartLoading">
                <div ref="customerSourceChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="客户地区分布" size="small" :loading="chartLoading">
                <div ref="customerRegionChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
          </a-row>
          <a-card title="客户销售排行 TOP10" size="small" style="margin-top: 16px">
            <div class="ranking-table-container">
              <VxeTableList
                :columns="customerRankVxeColumns"
                :data-source="customerRankData"
                :pagination="false"
                row-key="rank"
                :show-toolbar="false"
                :selectable="false"
                :show-add="false"
                :show-search="false"
                :show-export="false"
                :show-batch-delete="false"
              >
                <template #rankCell="{ record }">
                  <a-tag v-if="record.rank <= 3" :color="getRankColor(record.rank)">
                    {{ record.rank }}
                  </a-tag>
                  <span v-else>{{ record.rank }}</span>
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
              </VxeTableList>
            </div>
          </a-card>
        </a-tab-pane>

        <a-tab-pane key="product" tab="产品分析">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-card title="产品类别销售" size="small" :loading="chartLoading">
                <div ref="productCategoryChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="产品销量分布" size="small" :loading="chartLoading">
                <div ref="productVolumeChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="产品毛利率分布" size="small" :loading="chartLoading">
                <div ref="productMarginChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
          </a-row>
          <a-card title="产品销售排行 TOP10" size="small" style="margin-top: 16px">
            <div class="ranking-table-container">
              <VxeTableList
                :columns="productRankVxeColumns"
                :data-source="productRankData"
                :pagination="false"
                row-key="rank"
                :show-toolbar="false"
                :selectable="false"
                :show-add="false"
                :show-search="false"
                :show-export="false"
                :show-batch-delete="false"
              >
                <template #rankCell="{ record }">
                  <a-tag v-if="record.rank <= 3" :color="getRankColor(record.rank)">
                    {{ record.rank }}
                  </a-tag>
                  <span v-else>{{ record.rank }}</span>
                </template>
                <template #totalAmountCell="{ record }">
                  <span class="amount-cell">¥{{ formatAmount(record.totalAmount) }}</span>
                </template>
                <template #marginCell="{ record }">
                  <a-progress
                    :percent="record.margin"
                    :stroke-color="getMarginColor(record.margin)"
                    size="small"
                    :show-info="true"
                    :format="(p: number) => `${p}%`"
                  />
                </template>
              </VxeTableList>
            </div>
          </a-card>
        </a-tab-pane>

        <a-tab-pane key="salesperson" tab="销售人员分析">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-card title="销售人员业绩" size="small" :loading="chartLoading">
                <div ref="salespersonChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card title="销售人员目标达成率" size="small" :loading="chartLoading">
                <div ref="targetChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
          </a-row>
          <a-card title="销售人员业绩排行" size="small" style="margin-top: 16px">
            <div class="ranking-table-container">
              <VxeTableList
                :columns="salespersonRankVxeColumns"
                :data-source="salespersonRankData"
                :pagination="false"
                row-key="rank"
                :show-toolbar="false"
                :selectable="false"
                :show-add="false"
                :show-search="false"
                :show-export="false"
                :show-batch-delete="false"
              >
                <template #rankCell="{ record }">
                  <a-tag v-if="record.rank <= 3" :color="getRankColor(record.rank)">
                    {{ record.rank }}
                  </a-tag>
                  <span v-else>{{ record.rank }}</span>
                </template>
                <template #totalAmountCell="{ record }">
                  <span class="amount-cell">¥{{ formatAmount(record.totalAmount) }}</span>
                </template>
                <template #targetRateCell="{ record }">
                  <a-progress
                    :percent="record.targetRate"
                    :stroke-color="getTargetColor(record.targetRate)"
                    size="small"
                    :show-info="true"
                    :format="(p: number) => `${p}%`"
                  />
                </template>
              </VxeTableList>
            </div>
          </a-card>
        </a-tab-pane>

        <a-tab-pane key="region" tab="区域分析">
          <a-row :gutter="16">
            <a-col :span="12">
              <a-card title="区域销售分布" size="small" :loading="chartLoading">
                <div ref="regionChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="12">
              <a-card title="区域增长对比" size="small" :loading="chartLoading">
                <div ref="regionGrowthChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
          </a-row>
          <a-card title="区域销售明细" size="small" style="margin-top: 16px">
            <div class="ranking-table-container">
              <VxeTableList
                :columns="regionVxeColumns"
                :data-source="regionData"
                :pagination="false"
                row-key="name"
                :show-toolbar="false"
                :selectable="false"
                :show-add="false"
                :show-search="false"
                :show-export="false"
                :show-batch-delete="false"
              >
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
              </VxeTableList>
            </div>
          </a-card>
        </a-tab-pane>

        <a-tab-pane key="time" tab="时间分析">
          <a-row :gutter="16">
            <a-col :span="8">
              <a-card title="每日销售趋势" size="small" :loading="chartLoading">
                <div ref="dailyChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="每周销售对比" size="small" :loading="chartLoading">
                <div ref="weeklyChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
            <a-col :span="8">
              <a-card title="时段销售分布" size="small" :loading="chartLoading">
                <div ref="hourlyChartRef" class="chart-container"></div>
              </a-card>
            </a-col>
          </a-row>
        </a-tab-pane>
      </a-tabs>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, watch, nextTick, computed } from 'vue'
import { message } from 'ant-design-vue'
import * as echarts from 'echarts'
import {
  ReloadOutlined,
  ExportOutlined,
  DollarOutlined,
  ShoppingOutlined,
  TeamOutlined,
  PercentageOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined
} from '@ant-design/icons-vue'
import { PageContainer } from '@/components'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { salesAnalysisApi, type SalesOverview, type CustomerRankItem, type ProductRankItem } from '@/api/sales-analysis'

const activeTab = ref('overview')
const filterExpanded = ref<string[]>([])
const dateRange = ref<any[]>([])
const selectedWarehouse = ref<number>()
const selectedSalesperson = ref<number>()
const selectedCustomerType = ref<string>()
const loading = ref(false)
const chartLoading = ref(false)
const autoRefresh = ref(false)
const lastUpdateTime = ref<string>('')

const warehouses = ref<{ id: number; name: string }[]>([])
const salespersons = ref<{ id: number; name: string }[]>([])

const summary = ref<SalesOverview & {
  totalGrowth?: number
  orderGrowth?: number
  avgGrowth?: number
  marginGrowth?: number
}>({
  totalAmount: 0,
  orderCount: 0,
  avgOrderAmount: 0,
  grossMargin: 0
})

let refreshTimer: ReturnType<typeof setInterval> | null = null

// 表格列配置
const customerRankVxeColumns = computed(() => [
  { field: 'rank', title: '排名', width: 80, align: 'center', slotName: 'rankCell' },
  { field: 'name', title: '客户名称', width: 180 },
  { field: 'orderCount', title: '订单数', width: 100, align: 'right' },
  { field: 'totalAmount', title: '销售总额', width: 140, align: 'right', slotName: 'totalAmountCell' },
  { field: 'growth', title: '同比增长', width: 100, align: 'right', slotName: 'growthCell' },
])

const productRankVxeColumns = computed(() => [
  { field: 'rank', title: '排名', width: 80, align: 'center', slotName: 'rankCell' },
  { field: 'name', title: '产品名称', width: 180 },
  { field: 'volume', title: '销量', width: 100, align: 'right' },
  { field: 'totalAmount', title: '销售总额', width: 140, align: 'right', slotName: 'totalAmountCell' },
  { field: 'margin', title: '毛利率', width: 150, slotName: 'marginCell' },
])

const salespersonRankVxeColumns = computed(() => [
  { field: 'rank', title: '排名', width: 80, align: 'center', slotName: 'rankCell' },
  { field: 'name', title: '销售人员', width: 140 },
  { field: 'orderCount', title: '订单数', width: 100, align: 'right' },
  { field: 'totalAmount', title: '销售总额', width: 140, align: 'right', slotName: 'totalAmountCell' },
  { field: 'targetRate', title: '目标达成率', width: 150, slotName: 'targetRateCell' },
])

const regionVxeColumns = computed(() => [
  { field: 'name', title: '区域', width: 120 },
  { field: 'orderCount', title: '订单数', width: 100, align: 'right' },
  { field: 'totalAmount', title: '销售总额', width: 140, align: 'right', slotName: 'totalAmountCell' },
  { field: 'growth', title: '同比增长', width: 100, align: 'right', slotName: 'growthCell' },
])

// 数据源
const customerRankData = ref<CustomerRankItem[]>([])
const productRankData = ref<ProductRankItem[]>([])
const salespersonRankData = ref<any[]>([])
const regionData = ref<any[]>([])



// 图表引用
const trendChartRef = ref<HTMLElement>()
const channelChartRef = ref<HTMLElement>()
const monthlyChartRef = ref<HTMLElement>()
const compareChartRef = ref<HTMLElement>()
const customerLevelChartRef = ref<HTMLElement>()
const customerSourceChartRef = ref<HTMLElement>()
const customerRegionChartRef = ref<HTMLElement>()
const productCategoryChartRef = ref<HTMLElement>()
const productVolumeChartRef = ref<HTMLElement>()
const productMarginChartRef = ref<HTMLElement>()
const salespersonChartRef = ref<HTMLElement>()
const targetChartRef = ref<HTMLElement>()
const regionChartRef = ref<HTMLElement>()
const regionGrowthChartRef = ref<HTMLElement>()
const dailyChartRef = ref<HTMLElement>()
const weeklyChartRef = ref<HTMLElement>()
const hourlyChartRef = ref<HTMLElement>()

let charts: echarts.ECharts[] = []

// 加载选项数据
const loadOptions = async () => {
  try {
    const whRes = await salesAnalysisApi.getWarehouses()
    warehouses.value = whRes.data || [{ id: 1, name: '北京仓库' }, { id: 2, name: '上海仓库' }]
    salespersons.value = [
      { id: 1, name: '张三' },
      { id: 2, name: '李四' },
      { id: 3, name: '王五' },
      { id: 4, name: '赵六' },
      { id: 5, name: '钱七' }
    ]
  } catch {
    warehouses.value = [{ id: 1, name: '北京仓库' }, { id: 2, name: '上海仓库' }]
    salespersons.value = [{ id: 1, name: '张三' }, { id: 2, name: '李四' }]
  }
}

// 构建查询参数
const buildParams = () => ({
  warehouseId: selectedWarehouse.value,
  salespersonId: selectedSalesperson.value ? String(selectedSalesperson.value) : undefined,
  customerType: selectedCustomerType.value,
  dateRange: dateRange.value?.length === 2
    ? [dateRange.value[0]?.format?.('YYYY-MM-DD') || dateRange.value[0], dateRange.value[1]?.format?.('YYYY-MM-DD') || dateRange.value[1]]
    : undefined
})

// 加载汇总数据
const loadSummary = async () => {
  try {
    const res = await salesAnalysisApi.getOverview(buildParams())
    if (res.data) {
      summary.value = res.data
      // 模拟增长率
      summary.value.totalGrowth = 15.2
      summary.value.orderGrowth = 12.5
      summary.value.avgGrowth = 8.3
      summary.value.marginGrowth = 2.1
    }
  } catch {
    // 使用默认数据
    summary.value = {
      totalAmount: 2520000,
      orderCount: 520,
      avgOrderAmount: 4846,
      grossMargin: 28.5,
      totalGrowth: 15.2,
      orderGrowth: 12.5,
      avgGrowth: 8.3,
      marginGrowth: 2.1
    }
  }
}

// 加载排行数据
const loadRankingData = async () => {
  const params = buildParams()
  try {
    const cr = await salesAnalysisApi.getCustomerRanking(params)
    customerRankData.value = cr.data || mockCustomerRank()
  } catch {
    customerRankData.value = mockCustomerRank()
  }
  try {
    const pr = await salesAnalysisApi.getProductRanking(params)
    productRankData.value = pr.data || mockProductRank()
  } catch {
    productRankData.value = mockProductRank()
  }
  try {
    const sr = await salesAnalysisApi.getSalespersonRanking(params)
    salespersonRankData.value = sr.data || mockSalespersonRank()
  } catch {
    salespersonRankData.value = mockSalespersonRank()
  }
  regionData.value = mockRegionData()
}

// 模拟数据
const mockCustomerRank = () => [
  { rank: 1, name: '北京科技有限公司', orderCount: 85, totalAmount: 520000, growth: 18.5 },
  { rank: 2, name: '上海贸易集团有限公司', orderCount: 72, totalAmount: 380000, growth: 12.3 },
  { rank: 3, name: '广州制造有限公司', orderCount: 65, totalAmount: 280000, growth: 8.2 },
  { rank: 4, name: '深圳创新科技有限公司', orderCount: 58, totalAmount: 220000, growth: 15.1 },
  { rank: 5, name: '杭州互联网有限公司', orderCount: 52, totalAmount: 180000, growth: -2.5 }
]

const mockProductRank = () => [
  { rank: 1, name: '高精度传感器', volume: 5200, totalAmount: 780000, margin: 35 },
  { rank: 2, name: '工业控制器', volume: 1800, totalAmount: 5040000, margin: 28 },
  { rank: 3, name: '连接线缆套装', volume: 8500, totalAmount: 722500, margin: 22 },
  { rank: 4, name: '智能仪表盘', volume: 1200, totalAmount: 360000, margin: 32 },
  { rank: 5, name: '液压阀门组件', volume: 3200, totalAmount: 256000, margin: 25 }
]

const mockSalespersonRank = () => [
  { rank: 1, name: '张三', orderCount: 120, totalAmount: 520000, targetRate: 95 },
  { rank: 2, name: '李四', orderCount: 95, totalAmount: 380000, targetRate: 88 },
  { rank: 3, name: '王五', orderCount: 80, totalAmount: 280000, targetRate: 75 },
  { rank: 4, name: '赵六', orderCount: 65, totalAmount: 180000, targetRate: 62 },
  { rank: 5, name: '钱七', orderCount: 55, totalAmount: 150000, targetRate: 55 }
]

const mockRegionData = () => [
  { name: '华北', orderCount: 180, totalAmount: 520000, growth: 12.5 },
  { name: '华东', orderCount: 150, totalAmount: 380000, growth: 8.3 },
  { name: '华南', orderCount: 120, totalAmount: 280000, growth: 15.2 },
  { name: '西南', orderCount: 50, totalAmount: 180000, growth: -2.1 },
  { name: '西北', orderCount: 40, totalAmount: 150000, growth: 5.8 }
]

// 加载所有数据
const loadData = async () => {
  loading.value = true
  chartLoading.value = true
  const hide = message.loading('正在加载数据...', 0)
  try {
    await Promise.allSettled([loadSummary(), loadRankingData()])
    lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
    message.success('数据已更新')
  } finally {
    loading.value = false
    hide()
    await nextTick()
    initAllCharts()
    chartLoading.value = false
  }
}

// 重置筛选
const handleResetFilter = () => {
  dateRange.value = []
  selectedWarehouse.value = undefined
  selectedSalesperson.value = undefined
  selectedCustomerType.value = undefined
  loadData()
}

// 导出报表
const exportReport = async () => {
  const hide = message.loading('正在生成报表...', 0)
  try {
    // 实际项目中应调用导出API
    await new Promise(resolve => setTimeout(resolve, 1500))
    const data = {
      汇总: {
        销售总额: summary.value.totalAmount,
        订单数量: summary.value.orderCount,
        平均客单价: summary.value.avgOrderAmount,
        毛利率: summary.value.grossMargin
      },
      客户排行: customerRankData.value,
      产品排行: productRankData.value
    }
    const csv = JSON.stringify(data, null, 2)
    const blob = new Blob(['\ufeff' + csv], { type: 'application/json;charset=utf-8' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `销售分析报表_${new Date().toISOString().slice(0, 10)}.json`
    a.click()
    window.URL.revokeObjectURL(url)
    message.success('报表导出成功')
  } catch (error: any) {
    message.error(error?.message || '导出失败')
  } finally {
    hide()
  }
}

// 格式化金额
const formatAmount = (amount: number) => {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
}

// 排名颜色
const getRankColor = (rank: number) => {
  if (rank === 1) return 'gold'
  if (rank === 2) return 'silver'
  if (rank === 3) return '#cd7f32'
  return 'default'
}

// 毛利率颜色
const getMarginColor = (margin: number) => {
  if (margin >= 30) return '#52c41a'
  if (margin >= 20) return '#1890ff'
  return '#faad14'
}

// 目标达成颜色
const getTargetColor = (rate: number) => {
  if (rate >= 90) return '#52c41a'
  if (rate >= 70) return '#1890ff'
  return '#faad14'
}

// 自动刷新
watch(autoRefresh, (enabled) => {
  if (enabled) {
    refreshTimer = setInterval(loadData, 60000)
  } else {
    if (refreshTimer) {
      clearInterval(refreshTimer)
      refreshTimer = null
    }
  }
})

// Tab切换刷新图表
watch(activeTab, () => nextTick(() => initAllCharts()))

// 初始化所有图表
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

// 图表初始化函数
const initTrendChart = () => {
  if (!trendChartRef.value) return
  disposeChart(trendChartRef.value)
  const chart = echarts.init(trendChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['销售额', '订单数'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月'], boundaryGap: false },
    yAxis: [
      { type: 'value', name: '销售额(万)', axisLabel: { formatter: (v: number) => `${v / 10000}` } },
      { type: 'value', name: '订单数', axisLabel: { formatter: '{value}' } }
    ],
    series: [
      { name: '销售额', type: 'line', smooth: true, yAxisIndex: 0, data: [280000, 320000, 380000, 420000, 480000, 520000], itemStyle: { color: '#1890ff' } },
      { name: '订单数', type: 'line', smooth: true, yAxisIndex: 1, data: [45, 52, 58, 65, 72, 80], itemStyle: { color: '#52c41a' } }
    ]
  })
}

const initChannelChart = () => {
  if (!channelChartRef.value) return
  disposeChart(channelChartRef.value)
  const chart = echarts.init(channelChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', top: 'center' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['60%', '50%'],
      avoidLabelOverlap: false,
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      data: [
        { name: '线上直销', value: 45, itemStyle: { color: '#1890ff' } },
        { name: '线下门店', value: 30, itemStyle: { color: '#52c41a' } },
        { name: '渠道代理', value: 20, itemStyle: { color: '#faad14' } },
        { name: '企业客户', value: 5, itemStyle: { color: '#722ed1' } }
      ]
    }]
  })
}

const initMonthlyChart = () => {
  if (!monthlyChartRef.value) return
  disposeChart(monthlyChartRef.value)
  const chart = echarts.init(monthlyChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['本月', '上月'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['第1周', '第2周', '第3周', '第4周'] },
    yAxis: { type: 'value', name: '销售额' },
    series: [
      { name: '本月', type: 'bar', data: [120000, 150000, 180000, 200000], itemStyle: { color: '#1890ff' } },
      { name: '上月', type: 'bar', data: [100000, 120000, 140000, 160000], itemStyle: { color: '#91d5ff' } }
    ]
  })
}

const initCompareChart = () => {
  if (!compareChartRef.value) return
  disposeChart(compareChartRef.value)
  const chart = echarts.init(compareChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['同比去年', '环比上月'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['销售额', '订单数', '客单价', '毛利率'] },
    yAxis: { type: 'value', name: '增长率(%)' },
    series: [
      { name: '同比去年', type: 'bar', data: [15.2, 12.5, 8.3, 2.1], itemStyle: { color: '#52c41a' } },
      { name: '环比上月', type: 'bar', data: [8.5, 6.2, 4.1, 1.5], itemStyle: { color: '#faad14' } }
    ]
  })
}

const initCustomerLevelChart = () => {
  if (!customerLevelChartRef.value) return
  disposeChart(customerLevelChartRef.value)
  const chart = echarts.init(customerLevelChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', top: 'center' },
    series: [{
      type: 'pie',
      radius: '60%',
      center: ['60%', '50%'],
      data: [
        { name: 'A类客户', value: 35, itemStyle: { color: '#f5222d' } },
        { name: 'B类客户', value: 28, itemStyle: { color: '#faad14' } },
        { name: 'C类客户', value: 22, itemStyle: { color: '#1890ff' } },
        { name: '潜在客户', value: 15, itemStyle: { color: '#8c8c8c' } }
      ]
    }]
  })
}

const initCustomerSourceChart = () => {
  if (!customerSourceChartRef.value) return
  disposeChart(customerSourceChartRef.value)
  const chart = echarts.init(customerSourceChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['线上推广', '线下活动', '客户转介', '主动咨询'] },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: [35, 25, 20, 18], itemStyle: { color: '#1890ff' } }]
  })
}

const initCustomerRegionChart = () => {
  if (!customerRegionChartRef.value) return
  disposeChart(customerRegionChartRef.value)
  const chart = echarts.init(customerRegionChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', top: 'center' },
    series: [{
      type: 'pie',
      radius: '60%',
      center: ['60%', '50%'],
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
  if (!productCategoryChartRef.value) return
  disposeChart(productCategoryChartRef.value)
  const chart = echarts.init(productCategoryChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: ['电脑', '办公家具', '打印设备', '配件', '其他'] },
    series: [{ type: 'bar', data: [520000, 280000, 180000, 80000, 50000], itemStyle: { color: '#1890ff' } }]
  })
}

const initProductVolumeChart = () => {
  if (!productVolumeChartRef.value) return
  disposeChart(productVolumeChartRef.value)
  const chart = echarts.init(productVolumeChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', top: 'center' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['60%', '50%'],
      data: [
        { name: '高销量', value: 30, itemStyle: { color: '#52c41a' } },
        { name: '中销量', value: 45, itemStyle: { color: '#1890ff' } },
        { name: '低销量', value: 25, itemStyle: { color: '#faad14' } }
      ]
    }]
  })
}

const initProductMarginChart = () => {
  if (!productMarginChartRef.value) return
  disposeChart(productMarginChartRef.value)
  const chart = echarts.init(productMarginChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['<20%', '20-25%', '25-30%', '>30%'] },
    yAxis: { type: 'value', name: '产品数' },
    series: [{ type: 'bar', data: [15, 35, 40, 10], itemStyle: { colors: ['#faad14', '#1890ff', '#52c41a', '#3f8600'] } }]
  })
}

const initSalespersonChart = () => {
  if (!salespersonChartRef.value) return
  disposeChart(salespersonChartRef.value)
  const chart = echarts.init(salespersonChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['销售额', '目标'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['张三', '李四', '王五', '赵六', '钱七'] },
    yAxis: { type: 'value', name: '销售额' },
    series: [
      { name: '销售额', type: 'bar', data: [520000, 380000, 280000, 180000, 150000], itemStyle: { color: '#1890ff' } },
      { name: '目标', type: 'line', data: [550000, 430000, 370000, 290000, 270000], itemStyle: { color: '#f5222d' } }
    ]
  })
}

const initTargetChart = () => {
  if (!targetChartRef.value) return
  disposeChart(targetChartRef.value)
  const chart = echarts.init(targetChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['张三', '李四', '王五', '赵六', '钱七'] },
    yAxis: { type: 'value', max: 100, name: '达成率(%)' },
    series: [{
      type: 'bar',
      data: [
        { value: 95, itemStyle: { color: '#52c41a' } },
        { value: 88, itemStyle: { color: '#52c41a' } },
        { value: 75, itemStyle: { color: '#1890ff' } },
        { value: 62, itemStyle: { color: '#faad14' } },
        { value: 55, itemStyle: { color: '#faad14' } }
      ]
    }]
  })
}

const initRegionChart = () => {
  if (!regionChartRef.value) return
  disposeChart(regionChartRef.value)
  const chart = echarts.init(regionChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['华北', '华东', '华南', '西南', '西北'] },
    yAxis: { type: 'value', name: '销售额' },
    series: [{ type: 'bar', data: [520000, 380000, 280000, 180000, 150000], itemStyle: { color: '#1890ff' } }]
  })
}

const initRegionGrowthChart = () => {
  if (!regionGrowthChartRef.value) return
  disposeChart(regionGrowthChartRef.value)
  const chart = echarts.init(regionGrowthChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['同比增长', '环比增长'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['华北', '华东', '华南', '西南', '西北'] },
    yAxis: { type: 'value', name: '增长率(%)' },
    series: [
      { name: '同比增长', type: 'line', data: [12.5, 8.3, 15.2, -2.1, 5.8], itemStyle: { color: '#1890ff' } },
      { name: '环比增长', type: 'line', data: [5.2, 3.8, 6.5, -1.2, 2.5], itemStyle: { color: '#52c41a' } }
    ]
  })
}

const initDailyChart = () => {
  if (!dailyChartRef.value) return
  disposeChart(dailyChartRef.value)
  const chart = echarts.init(dailyChartRef.value)
  charts.push(chart)
  const days = Array.from({ length: 30 }, (_, i) => `${i + 1}日`)
  const values = Array.from({ length: 30 }, () => Math.floor(Math.random() * 50000) + 10000)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: days, axisLabel: { interval: 4 } },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, data: values, itemStyle: { color: '#1890ff' } }]
  })
}

const initWeeklyChart = () => {
  if (!weeklyChartRef.value) return
  disposeChart(weeklyChartRef.value)
  const chart = echarts.init(weeklyChartRef.value)
  charts.push(chart)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['本周', '上周'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
    yAxis: { type: 'value' },
    series: [
      { name: '本周', type: 'line', data: [45000, 52000, 48000, 55000, 42000, 28000, 35000], itemStyle: { color: '#1890ff' } },
      { name: '上周', type: 'line', data: [38000, 45000, 42000, 48000, 35000, 22000, 28000], itemStyle: { color: '#91d5ff' } }
    ]
  })
}

const initHourlyChart = () => {
  if (!hourlyChartRef.value) return
  disposeChart(hourlyChartRef.value)
  const chart = echarts.init(hourlyChartRef.value)
  charts.push(chart)
  const hours = Array.from({ length: 24 }, (_, i) => `${i}:00`)
  const values = Array.from({ length: 24 }, (_, i) => {
    if (i >= 9 && i <= 18) return Math.floor(Math.random() * 8000) + 2000
    return Math.floor(Math.random() * 2000) + 500
  })
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '10%', top: '10%', containLabel: true },
    xAxis: { type: 'category', data: hours, axisLabel: { interval: 2 } },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: values, itemStyle: { color: '#1890ff' } }]
  })
}

const disposeChart = (el: HTMLElement | undefined) => {
  if (!el) return
  const existing = charts.find(c => c.getDom() === el)
  if (existing) {
    existing.dispose()
    charts = charts.filter(c => c !== existing)
  }
}

// 窗口 resize 处理
const handleResize = () => {
  charts.forEach(chart => chart.resize())
}

onMounted(async () => {
  await loadOptions()
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  charts.forEach(chart => chart.dispose())
  charts = []
  if (refreshTimer) clearInterval(refreshTimer)
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.filter-collapse {
  background: #fff;
  border-radius: 4px;
}

.filter-actions {
  display: flex;
  align-items: flex-end;
  padding-bottom: 4px;
}

.summary-cards {
  padding: 16px 0;
}

.summary-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
}

.summary-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.summary-card.highlight {
  background: linear-gradient(135deg, #fff9e6 0%, #fff5d6 100%);
  border: 1px solid #ffe58f;
}

.summary-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  margin-right: 16px;
}

.summary-content {
  flex: 1;
}

.summary-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
}

.summary-change {
  font-size: 12px;
  margin-top: 4px;
}

.summary-change.positive {
  color: #52c41a;
}

.summary-change.negative {
  color: #f5222d;
}

.tab-content {
  flex: 1;
  overflow: hidden;
}

.chart-container {
  height: 280px;
}

.ranking-table-container {
  max-height: 400px;
  overflow-y: auto;
}







/* 金额单元格 */
.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, 'Courier New', monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}

/* 增长率单元格 */
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

/* 数据状态 */
.data-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #666;
}

.update-time {
  color: #999;
}

</style>
