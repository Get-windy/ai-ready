<template>
  <div class="home-page">
    <van-pull-refresh v-model="refreshing" @refresh="onRefresh">
      <div class="home-content">
        <div class="user-info">
          <van-image
            round
            width="48"
            height="48"
            :src="userStore.userInfo?.avatar || defaultAvatar"
          />
          <div class="user-detail">
            <div class="user-name">{{ userStore.userInfo?.nickname || '用户' }}</div>
            <div class="user-role">{{ userStore.userInfo?.roleName || '管理员' }}</div>
          </div>
          <van-icon name="bell" size="24" :badge="notificationCount" @click="goNotification" />
        </div>

        <van-grid :column-num="4" class="quick-actions">
          <van-grid-item
            v-for="action in quickActions"
            :key="action.name"
            :icon="action.icon"
            :text="action.text"
            :badge="action.badge"
            @click="handleQuickAction(action)"
          />
        </van-grid>

        <div class="dashboard-cards">
          <div class="card-title">今日概览</div>
          <van-grid :column-num="2" :border="false">
            <van-grid-item v-for="item in dashboardData" :key="item.label">
              <div class="stat-card">
                <div class="stat-value">{{ item.value }}</div>
                <div class="stat-label">{{ item.label }}</div>
                <div class="stat-trend" :class="item.trend > 0 ? 'up' : 'down'">
                  <van-icon :name="item.trend > 0 ? 'arrow-up' : 'arrow-down'" />
                  {{ Math.abs(item.trend) }}%
                </div>
              </div>
            </van-grid-item>
          </van-grid>
        </div>

        <div class="pending-section">
          <div class="section-header">
            <span class="card-title">待办事项</span>
            <van-button size="small" plain type="primary" @click="goApproval">
              查看全部
            </van-button>
          </div>
          <van-list
            v-model:loading="pendingLoading"
            :finished="pendingFinished"
            finished-text="没有更多了"
            @load="loadPending"
          >
            <van-cell
              v-for="item in pendingList"
              :key="item.id"
              :title="item.title"
              :label="item.createTime"
              is-link
              @click="goApprovalDetail(item)"
            >
              <template #icon>
                <van-tag :type="getApprovalType(item.type)">{{ item.typeLabel }}</van-tag>
              </template>
            </van-cell>
          </van-list>
        </div>

        <div class="chart-section">
          <div class="card-title">销售趋势</div>
          <div ref="salesChartRef" class="chart-container"></div>
        </div>

        <div class="recent-orders">
          <div class="section-header">
            <span class="card-title">最近订单</span>
            <van-button size="small" plain type="primary" @click="goOrder">
              查看全部
            </van-button>
          </div>
          <van-cell-group inset>
            <van-cell
              v-for="order in recentOrders"
              :key="order.id"
              :title="order.orderNo"
              :label="order.customerName"
              is-link
              @click="goOrderDetail(order)"
            >
              <template #value>
                <span class="order-amount">¥{{ order.amount }}</span>
              </template>
              <template #icon>
                <van-tag :type="getOrderStatusType(order.status)">
                  {{ order.statusLabel }}
                </van-tag>
              </template>
            </van-cell>
          </van-cell-group>
        </div>
      </div>
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { api } from '@/api'
import * as echarts from 'echarts'

const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://fastly.jsdelivr.net/npm/@vant/assets/cat.jpeg'
const refreshing = ref(false)
const notificationCount = ref(0)
const pendingLoading = ref(false)
const pendingFinished = ref(false)
const salesChartRef = ref<HTMLElement>()

const quickActions = reactive([
  { name: 'approval', icon: 'todo-list-o', text: '审批', badge: 3 },
  { name: 'customer', icon: 'friends-o', text: '客户', badge: '' },
  { name: 'order', icon: 'orders-o', text: '订单', badge: '' },
  { name: 'report', icon: 'chart-trending-o', text: '报表', badge: '' },
  { name: 'scan', icon: 'scan', text: '扫码', badge: '' },
  { name: 'expense', icon: 'balance-list-o', text: '报销', badge: '' },
  { name: 'leave', icon: 'clock-o', text: '请假', badge: '' },
  { name: 'more', icon: 'apps-o', text: '更多', badge: '' }
])

const dashboardData = ref([
  { label: '今日销售额', value: '--', trend: 0 },
  { label: '今日订单数', value: '--', trend: 0 },
  { label: '新增客户', value: '--', trend: 0 },
  { label: '待审批', value: '--', trend: 0 }
])

const pendingList = ref<any[]>([])
const recentOrders = ref<any[]>([])

onMounted(() => {
  loadDashboardData()
  initSalesChart()
})

const onRefresh = async () => {
  await loadDashboardData()
  refreshing.value = false
}

const loadDashboardData = async () => {
  try {
    const res = await api.order.getStatistics({})
    if (res?.todaySales !== undefined) dashboardData.value[0].value = `¥${res.todaySales?.toLocaleString?.() ?? '--'}`
    if (res?.todayOrders !== undefined) dashboardData.value[1].value = String(res.todayOrders ?? '--')
    if (res?.newCustomers !== undefined) dashboardData.value[2].value = String(res.newCustomers ?? '--')
    if (res?.pendingApprovals !== undefined) dashboardData.value[3].value = String(res.pendingApprovals ?? '--')
  } catch { /* keep placeholders */ }

  try {
    const pendingRes = await api.approval.getPendingCount()
    if (pendingRes) notificationCount.value = typeof pendingRes === 'number' ? pendingRes : (pendingRes as any)?.count ?? 0
    const listRes = await api.approval.getList({ pageNum: 1, pageSize: 5 })
    pendingList.value = Array.isArray(listRes?.records) ? listRes.records : (Array.isArray(listRes) ? listRes : [])
  } catch { pendingList.value = [] }

  try {
    const orderRes = await api.order.getList({ pageNum: 1, pageSize: 3 })
    recentOrders.value = Array.isArray(orderRes?.records) ? orderRes.records : (Array.isArray(orderRes) ? orderRes : [])
  } catch { recentOrders.value = [] }
}

const loadPending = () => {
  pendingLoading.value = false
  pendingFinished.value = true
}

let chart: echarts.ECharts | null = null

const handleResize = () => {
  chart?.resize()
}

const initSalesChart = () => {
  if (!salesChartRef.value) return

  chart = echarts.init(salesChartRef.value)
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
      boundaryGap: false,
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '销售额',
        type: 'line',
        smooth: true,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(25, 137, 250, 0.3)' },
            { offset: 1, color: 'rgba(25, 137, 250, 0.05)' }
          ])
        },
        lineStyle: {
          color: '#1989fa'
        },
        itemStyle: {
          color: '#1989fa'
        },
        data: [820, 932, 901, 934, 1290, 1330, 1320]
      }
    ]
  }
  chart.setOption(option)

  window.addEventListener('resize', handleResize)
}

const handleQuickAction = (action: any) => {
  switch (action.name) {
    case 'approval':
      router.push('/approval')
      break
    case 'customer':
      router.push('/customer')
      break
    case 'order':
      router.push('/order')
      break
    case 'report':
      router.push('/report')
      break
    case 'scan':
      router.push('/scan')
      break
    case 'expense':
      router.push('/expense')
      break
    case 'leave':
      router.push('/leave')
      break
    case 'more':
      router.push('/more')
      break
  }
}

const getApprovalType = (type: string) => {
  const types: Record<string, string> = {
    purchase: 'primary',
    expense: 'warning',
    contract: 'success',
    leave: 'default'
  }
  return types[type] || 'default'
}

const getOrderStatusType = (status: string) => {
  const types: Record<string, string> = {
    pending: 'warning',
    confirmed: 'primary',
    shipped: 'success',
    completed: 'success',
    cancelled: 'danger'
  }
  return types[status] || 'default'
}

const goNotification = () => router.push('/notification')
const goApproval = () => router.push('/approval')
const goApprovalDetail = (item: any) => router.push(`/approval/${item.id}`)
const goOrder = () => router.push('/order')
const goOrderDetail = (order: any) => router.push(`/order/${order.id}`)

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})
</script>

<style scoped lang="scss">
.home-page {
  min-height: 100vh;
  background: #f5f5f5;
}

.home-content {
  padding-bottom: 60px;
}

.user-info {
  display: flex;
  align-items: center;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;

  .user-detail {
    flex: 1;
    margin-left: 12px;

    .user-name {
      font-size: 16px;
      font-weight: 500;
    }

    .user-role {
      font-size: 12px;
      opacity: 0.8;
      margin-top: 4px;
    }
  }
}

.quick-actions {
  margin: 12px;
  border-radius: 8px;
  overflow: hidden;
}

.dashboard-cards {
  margin: 12px;
  background: #fff;
  border-radius: 8px;
  padding: 12px;

  .stat-card {
    text-align: center;
    padding: 8px 0;

    .stat-value {
      font-size: 20px;
      font-weight: 600;
      color: #333;
    }

    .stat-label {
      font-size: 12px;
      color: #999;
      margin-top: 4px;
    }

    .stat-trend {
      font-size: 12px;
      margin-top: 4px;

      &.up {
        color: #07c160;
      }

      &.down {
        color: #ee0a24;
      }
    }
  }
}

.card-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 12px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.pending-section,
.recent-orders {
  margin: 12px;
  background: #fff;
  border-radius: 8px;
  padding: 12px;
}

.chart-section {
  margin: 12px;
  background: #fff;
  border-radius: 8px;
  padding: 12px;

  .chart-container {
    height: 200px;
  }
}

.order-amount {
  color: #ee0a24;
  font-weight: 500;
}
</style>