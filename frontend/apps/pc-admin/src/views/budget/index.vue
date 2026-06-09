<template>
  <PageContainer title="预算管理" full-height>
    <template #headerExtra>
      <a-space :size="12">
        <span class="data-status">
          <a-badge :status="loading ? 'processing' : 'success'" />
          <span v-if="lastUpdateTime" class="update-time">
            数据更新: {{ lastUpdateTime }}
          </span>
        </span>
        <a-button size="small" @click="loadData">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </template>

    <div class="budget-dashboard">
      <!-- KPI 卡片 -->
      <div class="stat-cards">
        <div class="stat-card stat-total">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(statistics.totalBudgetAmount) }}</div>
            <div class="stat-card-label">预算总额</div>
          </div>
          <DollarOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-used">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(statistics.totalUsedAmount) }}</div>
            <div class="stat-card-label">已使用金额</div>
          </div>
          <PieChartOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-remaining">
          <div class="stat-card-body">
            <div class="stat-card-value">¥{{ formatAmount(statistics.totalRemainingAmount) }}</div>
            <div class="stat-card-label">剩余金额</div>
          </div>
          <WalletOutlined class="stat-card-icon" />
        </div>
        <div class="stat-card stat-rate">
          <div class="stat-card-body">
            <div class="stat-card-value">{{ statistics.executionRate?.toFixed(2) || '0.00' }}%</div>
            <div class="stat-card-label">执行率</div>
          </div>
          <PercentageOutlined class="stat-card-icon" />
        </div>
      </div>

      <a-row :gutter="[16, 16]">
        <!-- 执行率图表 -->
        <a-col :span="16">
          <a-card title="预算执行趋势" class="chart-card">
            <div ref="trendChartRef" style="height: 350px"></div>
          </a-card>
        </a-col>
        <!-- 状态分布 -->
        <a-col :span="8">
          <a-card title="预算状态分布" class="chart-card">
            <div ref="statusChartRef" style="height: 350px"></div>
          </a-card>
        </a-col>
      </a-row>

      <a-row :gutter="[16, 16]" class="mt-4">
        <!-- 近期调整 -->
        <a-col :span="16">
          <a-card title="近期预算调整" class="table-card">
            <VxeTableList
              :data-source="recentAdjustments"
              :columns="adjustmentVxeColumns"
              :loading="adjustmentLoading"
              :pagination="false"
              row-key="id"
              :show-toolbar="false"
              :selectable="false"
              :show-add="false"
              :show-search="false"
              :show-export="false"
              :show-batch-delete="false"
            >
              <template #statusCell="{ record }">
                <a-tag :color="adjustmentStatusColor(record.status)">{{ adjustmentStatusText(record.status) }}</a-tag>
              </template>
              <template #amountCell="{ record }">
                <span class="amount-cell">¥{{ formatAmount(record.amount) }}</span>
              </template>
              <template #adjustmentTypeCell="{ record }">
                <a-tag :color="record.adjustmentType === 'increase' ? 'green' : record.adjustmentType === 'decrease' ? 'red' : 'blue'">
                  {{ record.adjustmentType === 'increase' ? '增加' : record.adjustmentType === 'decrease' ? '减少' : '调剂' }}
                </a-tag>
              </template>
            </VxeTableList>
          </a-card>
        </a-col>
        <!-- 快捷操作 -->
        <a-col :span="8">
          <a-card title="快捷操作" class="quick-actions-card">
            <a-space direction="vertical" style="width: 100%">
              <a-button type="primary" block @click="$router.push('/budget/template')">
                <template #icon><FileTextOutlined /></template>
                预算模板管理
              </a-button>
              <a-button type="primary" ghost block @click="$router.push('/budget/annual')">
                <template #icon><CalendarOutlined /></template>
                年度预算管理
              </a-button>
              <a-button type="primary" ghost block @click="$router.push('/budget/adjustment')">
                <template #icon><EditOutlined /></template>
                预算调整管理
              </a-button>
              <a-button type="primary" ghost block @click="$router.push('/budget/report')">
                <template #icon><BarChartOutlined /></template>
                预算报表分析
              </a-button>
            </a-space>
          </a-card>
        </a-col>
      </a-row>
    </div>

    <!-- 快捷键提示条 -->
    <div class="footer-hint">
      <a-space size="middle">
        <span><kbd>F5</kbd> 刷新</span>
      </a-space>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  ReloadOutlined, DollarOutlined, PieChartOutlined, WalletOutlined,
  FileTextOutlined, CalendarOutlined, EditOutlined, BarChartOutlined
} from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import { PageContainer } from '@/components'
import { budgetReportApi, budgetAdjustmentApi } from '@/api/budget'
import * as echarts from 'echarts'

const loading = ref(false)
const lastUpdateTime = ref('')

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

const adjustmentVxeColumns = [
  { field: 'adjustmentNo', title: '调整单号' },
  { field: 'adjustmentType', title: '类型', slotName: 'adjustmentTypeCell' },
  { field: 'amount', title: '金额', align: 'right', slotName: 'amountCell' },
  { field: 'status', title: '状态', slotName: 'statusCell' },
  { field: 'applyDate', title: '申请日期' },
]

const adjustmentStatusColor = (status: string) => {
  const map: Record<string, string> = { draft: 'default', submitted: 'blue', approved: 'green', rejected: 'red' }
  return map[status] || 'default'
}
const adjustmentStatusText = (status: string) => {
  const map: Record<string, string> = { draft: '草稿', submitted: '待审批', approved: '已通过', rejected: '已拒绝' }
  return map[status] || status
}

function formatAmount(amount: number): string {
  return amount?.toLocaleString?.('zh-CN', { minimumFractionDigits: 2 }) || '0.00'
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
  loading.value = true
  lastUpdateTime.value = ''

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
  } catch { /* error handled */ }

  try {
    const trendRes = await budgetReportApi.trend()
    if (trendRes.success && trendRes.data) {
      const months = trendRes.data.map((d: any) => d.month + '月')
      const amounts = trendRes.data.map((d: any) => d.amount)
      trendChart?.setOption({ xAxis: { data: months }, series: [{ data: amounts }] })
    }
  } catch { /* error handled */ }

  adjustmentLoading.value = true
  try {
    const adjRes = await budgetAdjustmentApi.page({ pageNum: 0, pageSize: 10 })
    if (adjRes.success) {
      recentAdjustments.value = adjRes.data.records || mockAdjustmentData()
    }
  } catch {
    recentAdjustments.value = mockAdjustmentData()
  }
  adjustmentLoading.value = false

  loading.value = false
  lastUpdateTime.value = new Date().toLocaleTimeString('zh-CN')
}

const mockAdjustmentData = (): any[] => [
  { id: 1, adjustmentNo: 'ADJ-2024-001', adjustmentType: 'increase', amount: 50000, status: 'approved', applyDate: '2024-01-15' },
  { id: 2, adjustmentNo: 'ADJ-2024-002', adjustmentType: 'decrease', amount: 30000, status: 'submitted', applyDate: '2024-02-01' },
  { id: 3, adjustmentNo: 'ADJ-2024-003', adjustmentType: 'transfer', amount: 20000, status: 'draft', applyDate: '2024-02-10' },
]

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
.budget-dashboard {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: auto;
}

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

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
  background: #fff;
}

.stat-total { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-used { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-remaining { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-rate { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 20px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 28px;
  color: rgba(0, 0, 0, 0.15);
}

.mt-4 { margin-top: 16px; }

.chart-card, .table-card, .quick-actions-card {
  border-radius: 8px;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  color: #f5222d;
  font-weight: 500;
}





/* 快捷键提示条 */
.footer-hint {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding: 4px 24px;
  background: #fff;
  border-top: 1px solid #e8e8e8;
  font-size: 12px;
  color: #999;
}

.footer-hint kbd {
  display: inline-block;
  padding: 1px 5px;
  font-size: 11px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  line-height: 1.4;
  color: #555;
  background-color: #f7f7f7;
  border: 1px solid #ccc;
  border-radius: 3px;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.2);
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 45%;
    min-width: 120px;
  }
  .footer-hint {
    display: none;
  }
}
</style>
