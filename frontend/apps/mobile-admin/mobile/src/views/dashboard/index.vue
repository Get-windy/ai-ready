<template>
  <div class="dashboard-page">
    <van-nav-bar title="数据看板">
      <template #right>
        <van-icon name="calendar-o" size="18" @click="showDatePicker = true" />
      </template>
    </van-nav-bar>

    <van-dropdown-menu>
      <van-dropdown-item v-model="dateRange" :options="dateRangeOptions" />
      <van-dropdown-item v-model="department" :options="departmentOptions" />
    </van-dropdown-menu>

    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <div class="dashboard-content">
        <div class="summary-cards">
          <van-grid :column-num="2" :border="false">
            <van-grid-item v-for="item in summaryData" :key="item.label">
              <div class="summary-card" :class="item.type">
                <div class="summary-icon">
                  <van-icon :name="item.icon" size="24" />
                </div>
                <div class="summary-value">{{ item.value }}</div>
                <div class="summary-label">{{ item.label }}</div>
                <div class="summary-trend" :class="item.trend > 0 ? 'up' : 'down'">
                  <van-icon :name="item.trend > 0 ? 'arrow-up' : 'arrow-down'" />
                  {{ Math.abs(item.trend) }}%
                </div>
              </div>
            </van-grid-item>
          </van-grid>
        </div>

        <div class="chart-section">
          <div class="section-header">
            <span class="section-title">销售趋势</span>
            <van-button size="small" plain type="primary" @click="toggleChartType">
              {{ chartType === 'line' ? '柱状图' : '折线图' }}
            </van-button>
          </div>
          <div ref="salesChartRef" class="chart-container"></div>
        </div>

        <div class="chart-section">
          <div class="section-header">
            <span class="section-title">客户分布</span>
          </div>
          <div ref="customerChartRef" class="chart-container"></div>
        </div>

        <div class="chart-section">
          <div class="section-header">
            <span class="section-title">产品销售排行</span>
          </div>
          <div class="ranking-list">
            <div v-for="(item, index) in productRanking" :key="item.id" class="ranking-item">
              <div class="ranking-index" :class="index < 3 ? 'top' : ''">{{ index + 1 }}</div>
              <div class="ranking-name">{{ item.name }}</div>
              <div class="ranking-value">{{ item.value }}</div>
              <van-progress :percentage="item.percentage" :show-pivot="false" />
            </div>
          </div>
        </div>

        <div class="chart-section">
          <div class="section-header">
            <span class="section-title">订单状态分布</span>
          </div>
          <div ref="orderStatusChartRef" class="chart-container"></div>
        </div>

        <div class="chart-section">
          <div class="section-header">
            <span class="section-title">员工业绩排行</span>
          </div>
          <div class="ranking-list">
            <div v-for="(item, index) in employeeRanking" :key="item.id" class="ranking-item">
              <div class="ranking-index" :class="index < 3 ? 'top' : ''">{{ index + 1 }}</div>
              <van-image round width="32" height="32" :src="item.avatar || defaultAvatar" />
              <div class="ranking-name">{{ item.name }}</div>
              <div class="ranking-value">{{ item.value }}</div>
              <van-progress :percentage="item.percentage" :show-pivot="false" />
            </div>
          </div>
        </div>
      </div>
    </van-pull-refresh>

    <van-calendar
      v-model:show="showDatePicker"
      type="range"
      :min-date="minDate"
      :max-date="maxDate"
      @confirm="onDateConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'

const defaultAvatar = 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'
const refreshing = ref(false)
const showDatePicker = ref(false)
const dateRange = ref(0)
const department = ref(0)
const chartType = ref('line')

const minDate = new Date(2020, 0, 1)
const maxDate = new Date()

const dateRangeOptions = [
  { text: '今日', value: 0 },
  { text: '本周', value: 1 },
  { text: '本月', value: 2 },
  { text: '本季度', value: 3 },
  { text: '本年', value: 4 }
]

const departmentOptions = [
  { text: '全部部门', value: 0 },
  { text: '销售部', value: 1 },
  { text: '采购部', value: 2 },
  { text: '财务部', value: 3 }
]

const summaryData = ref([
  { label: '销售额', value: '¥128.5万', icon: 'balance-o', trend: 12.5, type: 'primary' },
  { label: '订单数', value: '56', icon: 'orders-o', trend: 8.3, type: 'success' },
  { label: '新增客户', value: '12', icon: 'friends-o', trend: -2.1, type: 'warning' },
  { label: '待审批', value: '8', icon: 'todo-list-o', trend: 0, type: 'danger' }
])

const productRanking = ref([
  { id: 1, name: '笔记本电脑', value: '¥45.2万', percentage: 85 },
  { id: 2, name: '办公桌椅', value: '¥32.8万', percentage: 65 },
  { id: 3, name: '打印机', value: '¥28.5万', percentage: 55 },
  { id: 4, name: '显示器', value: '¥18.2万', percentage: 35 },
  { id: 5, name: '键盘鼠标', value: '¥12.5万', percentage: 25 }
])

const employeeRanking = ref([
  { id: 1, name: '张三', value: '¥38.5万', percentage: 90, avatar: '' },
  { id: 2, name: '李四', value: '¥32.2万', percentage: 75, avatar: '' },
  { id: 3, name: '王五', value: '¥28.8万', percentage: 68, avatar: '' },
  { id: 4, name: '赵六', value: '¥22.5万', percentage: 52, avatar: '' },
  { id: 5, name: '钱七', value: '¥18.2万', percentage: 42, avatar: '' }
])

const salesChartRef = ref<HTMLElement>()
const customerChartRef = ref<HTMLElement>()
const orderStatusChartRef = ref<HTMLElement>()

let salesChart: echarts.ECharts | null = null
let customerChart: echarts.ECharts | null = null
let orderStatusChart: echarts.ECharts | null = null

onMounted(() => {
  initCharts()
})

onUnmounted(() => {
  salesChart?.dispose()
  customerChart?.dispose()
  orderStatusChart?.dispose()
})

const onRefresh = async () => {
  await loadDashboardData()
  refreshing.value = false
}

const loadDashboardData = () => {
  initCharts()
}

const initCharts = () => {
  initSalesChart()
  initCustomerChart()
  initOrderStatusChart()
}

const initSalesChart = () => {
  if (!salesChartRef.value) return

  if (salesChart) {
    salesChart.dispose()
  }

  salesChart = echarts.init(salesChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: chartType.value === 'bar',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '销售额',
        type: chartType.value,
        smooth: chartType.value === 'line',
        data: [18.5, 22.3, 19.8, 25.6, 28.2, 15.8, 18.5],
        itemStyle: {
          color: '#1989fa'
        },
        areaStyle: chartType.value === 'line' ? {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(25, 137, 250, 0.3)' },
            { offset: 1, color: 'rgba(25, 137, 250, 0.05)' }
          ])
        } : undefined
      }
    ]
  }

  salesChart.setOption(option)
}

const initCustomerChart = () => {
  if (!customerChartRef.value) return

  if (customerChart) {
    customerChart.dispose()
  }

  customerChart = echarts.init(customerChartRef.value)

  const option = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '客户分布',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { value: 35, name: 'A类客户', itemStyle: { color: '#ee0a24' } },
          { value: 28, name: 'B类客户', itemStyle: { color: '#ff976a' } },
          { value: 22, name: 'C类客户', itemStyle: { color: '#1989fa' } },
          { value: 15, name: '潜在客户', itemStyle: { color: '#969799' } }
        ]
      }
    ]
  }

  customerChart.setOption(option)
}

const initOrderStatusChart = () => {
  if (!orderStatusChartRef.value) return

  if (orderStatusChart) {
    orderStatusChart.dispose()
  }

  orderStatusChart = echarts.init(orderStatusChartRef.value)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value'
    },
    yAxis: {
      type: 'category',
      data: ['待确认', '已确认', '已发货', '已完成', '已取消']
    },
    series: [
      {
        name: '订单数',
        type: 'bar',
        data: [
          { value: 12, itemStyle: { color: '#ff976a' } },
          { value: 18, itemStyle: { color: '#1989fa' } },
          { value: 25, itemStyle: { color: '#07c160' } },
          { value: 45, itemStyle: { color: '#07c160' } },
          { value: 5, itemStyle: { color: '#ee0a24' } }
        ],
        barWidth: '60%'
      }
    ]
  }

  orderStatusChart.setOption(option)
}

const toggleChartType = () => {
  chartType.value = chartType.value === 'line' ? 'bar' : 'line'
  initSalesChart()
}

const onDateConfirm = (values: any) => {
  showDatePicker.value = false
  loadDashboardData()
}
</script>

<style scoped lang="scss">
.dashboard-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.dashboard-content {
  padding: 12px;
}

.summary-cards {
  margin-bottom: 12px;

  .summary-card {
    padding: 16px;
    border-radius: 8px;
    background: #fff;
    text-align: center;

    .summary-icon {
      margin-bottom: 8px;
    }

    .summary-value {
      font-size: 20px;
      font-weight: 600;
      color: #333;
    }

    .summary-label {
      font-size: 12px;
      color: #999;
      margin-top: 4px;
    }

    .summary-trend {
      font-size: 12px;
      margin-top: 4px;

      &.up {
        color: #07c160;
      }

      &.down {
        color: #ee0a24;
      }
    }

    &.primary .summary-icon {
      color: #1989fa;
    }

    &.success .summary-icon {
      color: #07c160;
    }

    &.warning .summary-icon {
      color: #ff976a;
    }

    &.danger .summary-icon {
      color: #ee0a24;
    }
  }
}

.chart-section {
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .section-title {
      font-size: 16px;
      font-weight: 500;
      color: #333;
    }
  }

  .chart-container {
    height: 200px;
  }
}

.ranking-list {
  .ranking-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 0;
    border-bottom: 1px solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }

    .ranking-index {
      width: 24px;
      height: 24px;
      border-radius: 4px;
      background: #f5f5f5;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      color: #999;

      &.top {
        background: linear-gradient(135deg, #ff976a 0%, #ff6b6b 100%);
        color: #fff;
      }
    }

    .ranking-name {
      flex: 1;
      font-size: 14px;
      color: #333;
    }

    .ranking-value {
      font-size: 14px;
      font-weight: 500;
      color: #ee0a24;
      margin-right: 12px;
    }

    :deep(.van-progress) {
      width: 80px;
    }
  }
}
</style>