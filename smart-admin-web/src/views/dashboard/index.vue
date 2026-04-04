<template>
  <div class="dashboard-page">
    <!-- 统计卡片区域 -->
    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :sm="12" :md="6" v-for="item in statsCards" :key="item.key">
        <a-card class="stats-card" :loading="statsLoading" hoverable>
          <div class="stats-content">
            <div class="stats-icon" :style="{ background: item.color }">
              <component :is="item.icon" />
            </div>
            <div class="stats-info">
              <div class="stats-title">{{ item.title }}</div>
              <div class="stats-value">
                <CountUp :end-val="item.value" :duration="1.5" />
                <span class="stats-suffix">{{ item.suffix }}</span>
              </div>
              <div class="stats-trend" :class="item.trend > 0 ? 'up' : 'down'">
                <RiseOutlined v-if="item.trend > 0" />
                <FallOutlined v-else />
                <span>{{ Math.abs(item.trend) }}%</span>
                <span class="trend-label">较昨日</span>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 图表区域第一行 -->
    <a-row :gutter="[16, 16]" class="chart-row">
      <a-col :xs="24" :lg="16">
        <a-card title="销售趋势" :loading="salesLoading" class="chart-card">
          <template #extra>
            <a-radio-group v-model:value="salesPeriod" size="small" @change="handleSalesPeriodChange">
              <a-radio-button value="week">本周</a-radio-button>
              <a-radio-button value="month">本月</a-radio-button>
              <a-radio-button value="year">全年</a-radio-button>
            </a-radio-group>
          </template>
          <div ref="salesChartRef" class="chart-container" style="height: 320px"></div>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="8">
        <a-card title="客户分布" :loading="customerLoading" class="chart-card">
          <template #extra>
            <a-button type="link" size="small">详情</a-button>
          </template>
          <div ref="customerChartRef" class="chart-container" style="height: 320px"></div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 图表区域第二行 -->
    <a-row :gutter="[16, 16]" class="chart-row">
      <a-col :xs="24" :lg="12">
        <a-card title="订单状态分布" :loading="orderLoading" class="chart-card">
          <div ref="orderChartRef" class="chart-container" style="height: 280px"></div>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="12">
        <a-card title="产品销售排行" :loading="productLoading" class="chart-card">
          <template #extra>
            <a-select v-model:value="productRankType" size="small" style="width: 100px" @change="handleProductTypeChange">
              <a-select-option value="quantity">按数量</a-select-option>
              <a-select-option value="amount">按金额</a-select-option>
            </a-select>
          </template>
          <div ref="productChartRef" class="chart-container" style="height: 280px"></div>
        </a-card>
      </a-col>
    </a-row>

    <!-- 实时数据区域 -->
    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="16">
        <a-card title="待办事项" :loading="todoLoading" class="todo-card">
          <template #extra>
            <a-space>
              <a-badge :count="todoStats.pending" :overflow-count="99">
                <a-button type="link" size="small">待处理</a-button>
              </a-badge>
              <a-button type="primary" size="small" @click="handleAddTodo">
                <PlusOutlined /> 新建
              </a-button>
            </a-space>
          </template>
          <a-list :data-source="todos" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta>
                  <template #avatar>
                    <a-avatar :style="{ backgroundColor: getPriorityColor(item.priority) }">
                      {{ item.title.charAt(0) }}
                    </a-avatar>
                  </template>
                  <template #title>
                    <a-space>
                      <span>{{ item.title }}</span>
                      <a-tag :color="getPriorityTagColor(item.priority)" size="small">
                        {{ getPriorityLabel(item.priority) }}
                      </a-tag>
                    </a-space>
                  </template>
                  <template #description>
                    <a-space split="">
                      <span><ClockCircleOutlined /> {{ item.time }}</span>
                      <span><UserOutlined /> {{ item.assignee }}</span>
                    </a-space>
                  </template>
                </a-list-item-meta>
                <template #actions>
                  <a-button type="link" size="small" @click="handleCompleteTodo(item)">完成</a-button>
                  <a-button type="link" size="small" @click="handleViewTodo(item)">查看</a-button>
                </template>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="8">
        <!-- 系统状态 -->
        <a-card title="系统状态" class="status-card">
          <a-descriptions :column="1" size="small">
            <a-descriptions-item label="API服务">
              <a-badge status="success" text="运行中" />
            </a-descriptions-item>
            <a-descriptions-item label="数据库">
              <a-badge status="success" text="正常" />
            </a-descriptions-item>
            <a-descriptions-item label="缓存">
              <a-badge status="success" text="已连接" />
            </a-descriptions-item>
            <a-descriptions-item label="CPU使用率">
              <a-progress :percent="35" :stroke-color=" '#52c41a'" size="small" />
            </a-descriptions-item>
            <a-descriptions-item label="内存使用率">
              <a-progress :percent="58" :stroke-color="'#1890ff'" size="small" />
            </a-descriptions-item>
          </a-descriptions>
        </a-card>

        <!-- 快捷操作 -->
        <a-card title="快捷操作" style="margin-top: 16px">
          <a-row :gutter="[8, 8]">
            <a-col :span="12" v-for="action in quickActions" :key="action.key">
              <a-button block @click="handleQuickAction(action)">
                <component :is="action.icon" />
                {{ action.label }}
              </a-button>
            </a-col>
          </a-row>
        </a-card>

        <!-- 实时通知 -->
        <a-card title="最新动态" style="margin-top: 16px" :loading="activityLoading">
          <a-timeline mode="left" :items="activities" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import type { ECharts } from 'echarts'
import {
  ShoppingCartOutlined,
  UserOutlined,
  TeamOutlined,
  DollarOutlined,
  RiseOutlined,
  FallOutlined,
  PlusOutlined,
  ClockCircleOutlined,
  FileTextOutlined,
  BarChartOutlined,
  SettingOutlined,
  BellOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

// 图表引用
const salesChartRef = ref<HTMLElement>()
const customerChartRef = ref<HTMLElement>()
const orderChartRef = ref<HTMLElement>()
const productChartRef = ref<HTMLElement>()

// 图表实例
let salesChart: ECharts | null = null
let customerChart: ECharts | null = null
let orderChart: ECharts | null = null
let productChart: ECharts | null = null

// 加载状态
const statsLoading = ref(false)
const salesLoading = ref(false)
const customerLoading = ref(false)
const orderLoading = ref(false)
const productLoading = ref(false)
const todoLoading = ref(false)
const activityLoading = ref(false)

// 数据状态
const salesPeriod = ref('month')
const productRankType = ref('amount')

// 统计卡片数据
const statsCards = ref([
  { key: 'orders', title: '今日订单', value: 128, suffix: '单', trend: 12.5, color: '#1890ff', icon: ShoppingCartOutlined },
  { key: 'customers', title: '新增客户', value: 32, suffix: '人', trend: 8.3, color: '#52c41a', icon: UserOutlined },
  { key: 'leads', title: '待处理线索', value: 15, suffix: '条', trend: -5.2, color: '#faad14', icon: TeamOutlined },
  { key: 'revenue', title: '本月销售额', value: 125600, suffix: '元', trend: 15.8, color: '#eb2f96', icon: DollarOutlined }
])

// 待办数据
const todoStats = ref({ pending: 8, completed: 12 })
const todos = ref([
  { id: 1, title: '审批采购订单', time: '10分钟前', assignee: '张三', priority: 'high' },
  { id: 2, title: '跟进客户询价', time: '30分钟前', assignee: '李四', priority: 'medium' },
  { id: 3, title: '处理库存预警', time: '1小时前', assignee: '王五', priority: 'high' },
  { id: 4, title: '确认订单发货', time: '2小时前', assignee: '赵六', priority: 'low' },
  { id: 5, title: '生成销售报告', time: '3小时前', assignee: '张三', priority: 'medium' }
])

// 快捷操作
const quickActions = ref([
  { key: 'order', label: '新建订单', icon: ShoppingCartOutlined },
  { key: 'customer', label: '添加客户', icon: UserOutlined },
  { key: 'report', label: '查看报表', icon: BarChartOutlined },
  { key: 'settings', label: '系统设置', icon: SettingOutlined }
])

// 动态数据
const activities = ref([
  { color: 'green', label: '新订单 #1234 已创建', children: '5分钟前' },
  { color: 'blue', label: '客户"张三"完成下单', children: '10分钟前' },
  { color: 'orange', label: '库存预警: 产品P001', children: '1小时前' },
  { color: 'gray', label: '系统备份完成', children: '2小时前' }
])

// 简单的数字动画组件
const CountUp = {
  props: ['endVal', 'duration'],
  template: '<span>{{ displayValue }}</span>',
  setup(props: any) {
    const displayValue = ref(0)
    const start = () => {
      const step = props.endVal / (props.duration * 60)
      let current = 0
      const timer = setInterval(() => {
        current += step
        if (current >= props.endVal) {
          displayValue.value = props.endVal
          clearInterval(timer)
        } else {
          displayValue.value = Math.floor(current)
        }
      }, 1000 / 60)
    }
    onMounted(start)
    return { displayValue }
  }
}

// 初始化销售趋势图表
const initSalesChart = () => {
  if (!salesChartRef.value) return
  salesChart = echarts.init(salesChartRef.value)
  
  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['销售额', '订单数'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: ['1日', '5日', '10日', '15日', '20日', '25日', '30日'] },
    yAxis: [
      { type: 'value', name: '销售额(万)', position: 'left' },
      { type: 'value', name: '订单数', position: 'right' }
    ],
    series: [
      { name: '销售额', type: 'line', smooth: true, areaStyle: { opacity: 0.3 }, data: [12, 15, 18, 22, 25, 28, 32], itemStyle: { color: '#1890ff' } },
      { name: '订单数', type: 'line', smooth: true, yAxisIndex: 1, data: [80, 95, 110, 130, 145, 160, 180], itemStyle: { color: '#52c41a' } }
    ]
  }
  salesChart.setOption(option)
}

// 初始化客户分布图表
const initCustomerChart = () => {
  if (!customerChartRef.value) return
  customerChart = echarts.init(customerChartRef.value)
  
  const option = {
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', right: '5%', top: 'center' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
      labelLine: { show: false },
      data: [
        { value: 1048, name: '企业客户', itemStyle: { color: '#1890ff' } },
        { value: 735, name: '个人客户', itemStyle: { color: '#52c41a' } },
        { value: 580, name: '渠道客户', itemStyle: { color: '#faad14' } },
        { value: 484, name: 'VIP客户', itemStyle: { color: '#eb2f96' } }
      ]
    }]
  }
  customerChart.setOption(option)
}

// 初始化订单状态图表
const initOrderChart = () => {
  if (!orderChartRef.value) return
  orderChart = echarts.init(orderChartRef.value)
  
  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: ['已取消', '待支付', '待发货', '已发货', '已完成'] },
    series: [{
      type: 'bar',
      data: [
        { value: 12, itemStyle: { color: '#ff4d4f' } },
        { value: 28, itemStyle: { color: '#faad14' } },
        { value: 45, itemStyle: { color: '#1890ff' } },
        { value: 68, itemStyle: { color: '#722ed1' } },
        { value: 156, itemStyle: { color: '#52c41a' } }
      ],
      barWidth: '60%',
      label: { show: true, position: 'right' }
    }]
  }
  orderChart.setOption(option)
}

// 初始化产品排行图表
const initProductChart = () => {
  if (!productChartRef.value) return
  productChart = echarts.init(productChartRef.value)
  
  const option = {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '3%', containLabel: true },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: ['产品A', '产品B', '产品C', '产品D', '产品E'] },
    series: [{
      type: 'bar',
      data: [320, 280, 220, 180, 150],
      barWidth: '50%',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#1890ff' },
          { offset: 1, color: '#36cfc9' }
        ])
      },
      label: { show: true, position: 'right' }
    }]
  }
  productChart.setOption(option)
}

// 处理窗口大小变化
const handleResize = () => {
  salesChart?.resize()
  customerChart?.resize()
  orderChart?.resize()
  productChart?.resize()
}

// 事件处理
const handleSalesPeriodChange = (e: any) => {
  salesLoading.value = true
  setTimeout(() => {
    salesLoading.value = false
    // 更新图表数据
  }, 500)
}

const handleProductTypeChange = () => {
  productLoading.value = true
  setTimeout(() => {
    productLoading.value = false
    // 更新图表数据
  }, 500)
}

const handleAddTodo = () => {
  message.info('新建待办')
}

const handleCompleteTodo = (item: any) => {
  message.success(`已完成: ${item.title}`)
}

const handleViewTodo = (item: any) => {
  message.info(`查看: ${item.title}`)
}

const handleQuickAction = (action: any) => {
  message.info(`快捷操作: ${action.label}`)
}

// 辅助方法
const getPriorityColor = (priority: string) => {
  const colors: Record<string, string> = { high: '#ff4d4f', medium: '#faad14', low: '#52c41a' }
  return colors[priority] || '#1890ff'
}

const getPriorityTagColor = (priority: string) => {
  const colors: Record<string, string> = { high: 'error', medium: 'warning', low: 'success' }
  return colors[priority] || 'default'
}

const getPriorityLabel = (priority: string) => {
  const labels: Record<string, string> = { high: '紧急', medium: '中等', low: '普通' }
  return labels[priority] || '未知'
}

// 生命周期
onMounted(async () => {
  await nextTick()
  initSalesChart()
  initCustomerChart()
  initOrderChart()
  initProductChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  salesChart?.dispose()
  customerChart?.dispose()
  orderChart?.dispose()
  productChart?.dispose()
})
</script>

<style scoped>
.dashboard-page {
  padding: 0;
}

.stats-card {
  height: 100%;
}

.stats-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stats-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
}

.stats-info {
  flex: 1;
}

.stats-title {
  color: #666;
  font-size: 14px;
  margin-bottom: 4px;
}

.stats-value {
  font-size: 28px;
  font-weight: 600;
  line-height: 1.2;
}

.stats-suffix {
  font-size: 14px;
  color: #999;
  margin-left: 4px;
}

.stats-trend {
  font-size: 12px;
  margin-top: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.stats-trend.up {
  color: #52c41a;
}

.stats-trend.down {
  color: #ff4d4f;
}

.trend-label {
  color: #999;
}

.chart-card {
  height: 100%;
}

.chart-container {
  width: 100%;
}

.chart-row {
  margin-top: 0;
}

.todo-card :deep(.ant-list-item) {
  padding: 12px 0;
}

.status-card :deep(.ant-descriptions-item-label) {
  width: 80px;
}

@media (max-width: 768px) {
  .stats-content {
    flex-direction: column;
    text-align: center;
  }
  
  .stats-info {
    text-align: center;
  }
}
</style>