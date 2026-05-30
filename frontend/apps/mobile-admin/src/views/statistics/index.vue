<template>
  <div class="statistics-page">
    <van-nav-bar title="统计分析">
      <template #right>
        <van-icon name="share-o" size="18" @click="shareReport" />
      </template>
    </van-nav-bar>

    <van-tabs v-model:active="activeTab" sticky>
      <van-tab title="销售分析">
        <div class="tab-content">
          <van-cell-group inset title="销售概览">
            <van-grid :column-num="3" :border="false">
              <van-grid-item>
                <div class="stat-item">
                  <div class="stat-value">¥128.5万</div>
                  <div class="stat-label">总销售额</div>
                </div>
              </van-grid-item>
              <van-grid-item>
                <div class="stat-item">
                  <div class="stat-value">56</div>
                  <div class="stat-label">订单数</div>
                </div>
              </van-grid-item>
              <van-grid-item>
                <div class="stat-item">
                  <div class="stat-value">¥2.3万</div>
                  <div class="stat-label">客单价</div>
                </div>
              </van-grid-item>
            </van-grid>
          </van-cell-group>

          <div class="chart-card">
            <div class="card-header">
              <span>销售趋势对比</span>
              <van-dropdown-menu direction="up">
                <van-dropdown-item v-model="compareType" :options="compareOptions" />
              </van-dropdown-menu>
            </div>
            <div ref="salesCompareChartRef" class="chart-container"></div>
          </div>

          <div class="chart-card">
            <div class="card-header">
              <span>销售渠道分布</span>
            </div>
            <div ref="channelChartRef" class="chart-container"></div>
          </div>

          <van-cell-group inset title="销售明细">
            <van-cell
              v-for="item in salesDetails"
              :key="item.id"
              :title="item.name"
              :value="item.value"
              :label="item.trend"
            >
              <template #icon>
                <van-icon :name="item.icon" class="detail-icon" />
              </template>
            </van-cell>
          </van-cell-group>
        </div>
      </van-tab>

      <van-tab title="客户分析">
        <div class="tab-content">
          <van-cell-group inset title="客户概览">
            <van-grid :column-num="3" :border="false">
              <van-grid-item>
                <div class="stat-item">
                  <div class="stat-value">128</div>
                  <div class="stat-label">总客户数</div>
                </div>
              </van-grid-item>
              <van-grid-item>
                <div class="stat-item">
                  <div class="stat-value">12</div>
                  <div class="stat-label">新增客户</div>
                </div>
              </van-grid-item>
              <van-grid-item>
                <div class="stat-item">
                  <div class="stat-value">85%</div>
                  <div class="stat-label">活跃率</div>
                </div>
              </van-grid-item>
            </van-grid>
          </van-cell-group>

          <div class="chart-card">
            <div class="card-header">
              <span>客户等级分布</span>
            </div>
            <div ref="customerLevelChartRef" class="chart-container"></div>
          </div>

          <div class="chart-card">
            <div class="card-header">
              <span>客户来源分析</span>
            </div>
            <div ref="customerSourceChartRef" class="chart-container"></div>
          </div>

          <van-cell-group inset title="客户明细">
            <van-cell
              v-for="item in customerDetails"
              :key="item.id"
              :title="item.name"
              :value="item.value"
              :label="item.count + '个'"
            />
          </van-cell-group>
        </div>
      </van-tab>

      <van-tab title="产品分析">
        <div class="tab-content">
          <van-cell-group inset title="产品概览">
            <van-grid :column-num="3" :border="false">
              <van-grid-item>
                <div class="stat-item">
                  <div class="stat-value">256</div>
                  <div class="stat-label">产品总数</div>
                </div>
              </van-grid-item>
              <van-grid-item>
                <div class="stat-item">
                  <div class="stat-value">45</div>
                  <div class="stat-label">热销产品</div>
                </div>
              </van-grid-item>
              <van-grid-item>
                <div class="stat-item">
                  <div class="stat-value">12</div>
                  <div class="stat-label">滞销产品</div>
                </div>
              </van-grid-item>
            </van-grid>
          </van-cell-group>

          <div class="chart-card">
            <div class="card-header">
              <span>产品类别销售</span>
            </div>
            <div ref="productCategoryChartRef" class="chart-container"></div>
          </div>

          <van-cell-group inset title="热销产品TOP10">
            <van-cell
              v-for="(item, index) in hotProducts"
              :key="item.id"
              :title="item.name"
              :value="item.sales"
              :label="'销量: ' + item.quantity"
            >
              <template #icon>
                <div class="rank-badge" :class="index < 3 ? 'top' : ''">{{ index + 1 }}</div>
              </template>
            </van-cell>
          </van-cell-group>
        </div>
      </van-tab>

      <van-tab title="财务分析">
        <div class="tab-content">
          <van-cell-group inset title="财务概览">
            <van-grid :column-num="3" :border="false">
              <van-grid-item>
                <div class="stat-item">
                  <div class="stat-value">¥128.5万</div>
                  <div class="stat-label">应收账款</div>
                </div>
              </van-grid-item>
              <van-grid-item>
                <div class="stat-item">
                  <div class="stat-value">¥85.2万</div>
                  <div class="stat-label">已收款</div>
                </div>
              </van-grid-item>
              <van-grid-item>
                <div class="stat-item">
                  <div class="stat-value">¥43.3万</div>
                  <div class="stat-label">待收款</div>
                </div>
              </van-grid-item>
            </van-grid>
          </van-cell-group>

          <div class="chart-card">
            <div class="card-header">
              <span>收款趋势</span>
            </div>
            <div ref="paymentChartRef" class="chart-container"></div>
          </div>

          <van-cell-group inset title="应收账款明细">
            <van-cell
              v-for="item in receivableDetails"
              :key="item.id"
              :title="item.customer"
              :value="item.amount"
              :label="item.days + '天'"
            >
              <template #icon>
                <van-tag :type="item.status === 'overdue' ? 'danger' : 'warning'">
                  {{ item.status === 'overdue' ? '逾期' : '正常' }}
                </van-tag>
              </template>
            </van-cell>
          </van-cell-group>
        </div>
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { showToast } from 'vant'
import * as echarts from 'echarts'

const activeTab = ref(0)
const compareType = ref(0)

const compareOptions = [
  { text: '同比去年', value: 0 },
  { text: '环比上月', value: 1 },
  { text: '环比上周', value: 2 }
]

const salesDetails = ref([
  { id: 1, name: '线上销售', value: '¥68.5万', trend: '↑12.5%', icon: 'computer-o' },
  { id: 2, name: '线下销售', value: '¥45.2万', trend: '↑8.3%', icon: 'shop-o' },
  { id: 3, name: '渠道销售', value: '¥14.8万', trend: '↓2.1%', icon: 'share' }
])

const customerDetails = ref([
  { id: 1, name: 'A类客户', value: '35%', count: 45 },
  { id: 2, name: 'B类客户', value: '28%', count: 36 },
  { id: 3, name: 'C类客户', value: '22%', count: 28 },
  { id: 4, name: '潜在客户', value: '15%', count: 19 }
])

const hotProducts = ref([
  { id: 1, name: '笔记本电脑', sales: '¥45.2万', quantity: 52 },
  { id: 2, name: '办公桌椅', sales: '¥32.8万', quantity: 35 },
  { id: 3, name: '打印机', sales: '¥28.5万', quantity: 28 },
  { id: 4, name: '显示器', sales: '¥18.2万', quantity: 45 },
  { id: 5, name: '键盘鼠标', sales: '¥12.5万', quantity: 120 }
])

const receivableDetails = ref([
  { id: 1, customer: '北京科技有限公司', amount: '¥25.8万', days: 15, status: 'normal' },
  { id: 2, customer: '上海贸易公司', amount: '¥18.5万', days: 30, status: 'overdue' },
  { id: 3, customer: '广州制造企业', amount: '¥12.2万', days: 8, status: 'normal' }
])

const salesCompareChartRef = ref<HTMLElement>()
const channelChartRef = ref<HTMLElement>()
const customerLevelChartRef = ref<HTMLElement>()
const customerSourceChartRef = ref<HTMLElement>()
const productCategoryChartRef = ref<HTMLElement>()
const paymentChartRef = ref<HTMLElement>()

let charts: echarts.ECharts[] = []

onMounted(() => {
  initAllCharts()
})

onUnmounted(() => {
  charts.forEach(chart => chart.dispose())
})

const initAllCharts = () => {
  initSalesCompareChart()
  initChannelChart()
  initCustomerLevelChart()
  initCustomerSourceChart()
  initProductCategoryChart()
  initPaymentChart()
}

const initSalesCompareChart = () => {
  if (!salesCompareChartRef.value) return
  const chart = echarts.init(salesCompareChartRef.value)
  charts.push(chart)

  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['本期', '上期'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
    yAxis: { type: 'value' },
    series: [
      { name: '本期', type: 'line', data: [18.5, 22.3, 19.8, 25.6, 28.2, 15.8, 18.5] },
      { name: '上期', type: 'line', data: [15.2, 18.5, 16.8, 22.3, 25.5, 12.8, 15.2] }
    ]
  })
}

const initChannelChart = () => {
  if (!channelChartRef.value) return
  const chart = echarts.init(channelChartRef.value)
  charts.push(chart)

  chart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      data: [
        { value: 53, name: '线上', itemStyle: { color: '#1989fa' } },
        { value: 35, name: '线下', itemStyle: { color: '#07c160' } },
        { value: 12, name: '渠道', itemStyle: { color: '#ff976a' } }
      ]
    }]
  })
}

const initCustomerLevelChart = () => {
  if (!customerLevelChartRef.value) return
  const chart = echarts.init(customerLevelChartRef.value)
  charts.push(chart)

  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['A类', 'B类', 'C类', '潜在'] },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      data: [
        { value: 45, itemStyle: { color: '#ee0a24' } },
        { value: 36, itemStyle: { color: '#ff976a' } },
        { value: 28, itemStyle: { color: '#1989fa' } },
        { value: 19, itemStyle: { color: '#969799' } }
      ]
    }]
  })
}

const initCustomerSourceChart = () => {
  if (!customerSourceChartRef.value) return
  const chart = echarts.init(customerSourceChartRef.value)
  charts.push(chart)

  chart.setOption({
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: '60%',
      data: [
        { value: 35, name: '线上推广' },
        { value: 25, name: '线下活动' },
        { value: 20, name: '客户转介' },
        { value: 18, name: '主动咨询' }
      ]
    }]
  })
}

const initProductCategoryChart = () => {
  if (!productCategoryChartRef.value) return
  const chart = echarts.init(productCategoryChartRef.value)
  charts.push(chart)

  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: ['电脑', '办公家具', '打印设备', '配件', '其他'] },
    series: [{
      type: 'bar',
      data: [45.2, 32.8, 28.5, 18.2, 12.5]
    }]
  })
}

const initPaymentChart = () => {
  if (!paymentChartRef.value) return
  const chart = echarts.init(paymentChartRef.value)
  charts.push(chart)

  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: ['1月', '2月', '3月', '4月', '5月', '6月'] },
    yAxis: { type: 'value' },
    series: [
      { name: '应收', type: 'bar', data: [128, 135, 142, 138, 145, 152] },
      { name: '实收', type: 'bar', data: [85, 92, 98, 95, 102, 108] }
    ]
  })
}

const shareReport = () => {
  navigator.clipboard ? navigator.clipboard.writeText(window.location.href).then(() => showToast('报告链接已复制')) : showToast('分享功能已就绪')
}
</script>

<style scoped lang="scss">
.statistics-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.tab-content {
  padding: 12px;
}

.stat-item {
  text-align: center;
  padding: 8px 0;

  .stat-value {
    font-size: 18px;
    font-weight: 600;
    color: #333;
  }

  .stat-label {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
  }
}

.chart-card {
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 16px;
    font-weight: 500;
    color: #333;
    margin-bottom: 12px;
  }

  .chart-container {
    height: 200px;
  }
}

.detail-icon {
  margin-right: 8px;
  color: #1989fa;
}

.rank-badge {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #999;
  margin-right: 8px;

  &.top {
    background: linear-gradient(135deg, #ff976a 0%, #ff6b6b 100%);
    color: #fff;
  }
}
</style>