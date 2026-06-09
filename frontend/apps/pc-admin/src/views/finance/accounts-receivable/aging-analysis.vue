<template>
  <div class="aging-analysis-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-30">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(stats.aging30) }}</div>
          <div class="stat-card-label">0-30天</div>
        </div>
        <ClockCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-60">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(stats.aging60) }}</div>
          <div class="stat-card-label">31-60天</div>
        </div>
        <WarningOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-90">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(stats.aging90) }}</div>
          <div class="stat-card-label">61-90天</div>
        </div>
        <ExclamationCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-90plus">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(stats.aging90plus) }}</div>
          <div class="stat-card-label">90天以上</div>
        </div>
        <CloseCircleOutlined class="stat-card-icon" />
      </div>
    </div>

    <!-- 筛选区域 -->
    <div class="filter-area">
      <a-form
        layout="inline"
        :model="queryParams"
      >
        <a-form-item label="客户名称">
          <a-input
            v-model:value="queryParams.customerName"
            placeholder="请输入客户名称"
            allow-clear
            style="width: 150px"
          />
        </a-form-item>
        <a-form-item label="统计截止">
          <a-date-picker
            v-model:value="queryParams.endDate"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button
              type="primary"
              @click="handleSearch"
            >
              查询
            </a-button>
            <a-button @click="handleReset">
              重置
            </a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </div>

    <!-- 账龄分析图表 -->
    <div class="chart-area">
      <a-card title="账龄分布图">
        <div
          ref="chartRef"
          style="height: 400px"
        />
      </a-card>
    </div>

    <!-- 账龄明细表 -->
    <div class="table-area">
      <a-card title="账龄明细">
        <VxeTableList
          ref="tableRef"
          :columns="vxeColumns"
          :data-source="tableData"
          :loading="loading"
          :pagination="pagination"
          row-key="id"
          :show-toolbar="false"
          :selectable="false"
          :show-add="false"
          :show-search="false"
          :show-export="false"
          :show-batch-delete="false"
          @page-change="handlePageChange"
        >
          <template #totalAmountCell="{ record }">
            <span class="amount-cell">¥{{ record.totalAmount?.toFixed(2) }}</span>
          </template>
          <template #agingDaysCell="{ record }">
            <a-tag :color="getAgingColor(record.agingDays)">
              {{ record.agingDays }}天
            </a-tag>
          </template>
        </VxeTableList>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { message } from 'ant-design-vue'
import { ClockCircleOutlined, WarningOutlined, ExclamationCircleOutlined, CloseCircleOutlined } from '@ant-design/icons-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'
import * as echarts from 'echarts'
import request from '@/utils/request'

interface AgingData {
  id: number
  customerName: string
  orderNo: string
  totalAmount: number
  dueDate: string
  agingDays: number
}

const chartRef = ref<HTMLElement>()
const loading = ref(false)
const tableData = ref<AgingData[]>([])
const tableRef = ref()

const queryParams = reactive({
  customerName: '',
  endDate: undefined as string | undefined
})

const stats = reactive({
  aging30: 125000,
  aging60: 85000,
  aging90: 42000,
  aging90plus: 18000
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})


const vxeColumns = computed(() => [
  { field: 'customerName', title: '客户名称', width: 150 },
  { field: 'orderNo', title: '订单号', width: 150 },
  { field: 'totalAmount', title: '应收金额', width: 120, align: 'right', slotName: 'totalAmountCell' },
  { field: 'dueDate', title: '到期日期', width: 120 },
  { field: 'agingDays', title: '账龄天数', width: 100, slotName: 'agingDaysCell' },
])

const getAgingColor = (days: number) => {
  if (days <= 30) return 'green'
  if (days <= 60) return 'blue'
  if (days <= 90) return 'orange'
  return 'red'
}

const formatAmount = (val: number) => {
  if (val === undefined || val === null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

let chart: echarts.ECharts | null = null

const handleResize = () => {
  chart?.resize()
}

const initChart = () => {
  nextTick(() => {
    if (!chartRef.value) return

    chart = echarts.init(chartRef.value)
    const option = {
      title: {
        text: '账龄分布'
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      legend: {
        data: ['0-30天', '31-60天', '61-90天', '90天以上']
      },
      xAxis: {
        type: 'category',
        data: ['0-30天', '31-60天', '61-90天', '90天以上']
      },
      yAxis: {
        type: 'value',
        name: '金额（元）'
      },
      series: [
        {
          name: '0-30天',
          type: 'bar',
          data: [stats.aging30],
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#1890ff' },
              { offset: 1, color: '#69c0ff' }
            ])
          }
        },
        {
          name: '31-60天',
          type: 'bar',
          data: [stats.aging60],
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#faad14' },
              { offset: 1, color: '#ffd666' }
            ])
          }
        },
        {
          name: '61-90天',
          type: 'bar',
          data: [stats.aging90],
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#fa8c16' },
              { offset: 1, color: '#ffc069' }
            ])
          }
        },
        {
          name: '90天以上',
          type: 'bar',
          data: [stats.aging90plus],
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#ff4d4f' },
              { offset: 1, color: '#ff7875' }
            ])
          }
        }
      ]
    }
    chart.setOption(option)

    window.addEventListener('resize', handleResize)
  })
}

const handleSearch = () => {
  fetchData()
}

const handleReset = () => {
  queryParams.customerName = ''
  queryParams.endDate = undefined
  handleSearch()
}

const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
  fetchData()
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/api/finance/accounts-receivable/aging', {
      params: {
        customerName: queryParams.customerName || undefined,
        endDate: queryParams.endDate || undefined,
        pageNum: pagination.current,
        pageSize: pagination.pageSize
      }
    })
    if (res.data?.records) {
      tableData.value = res.data.records
      pagination.total = res.data.total || 0
      stats.aging30 = res.data.aging30 || 0
      stats.aging60 = res.data.aging60 || 0
      stats.aging90 = res.data.aging90 || 0
      stats.aging90plus = res.data.aging90plus || 0
    } else {
      tableData.value = []
      pagination.total = 0
      stats.aging30 = 0
      stats.aging60 = 0
      stats.aging90 = 0
      stats.aging90plus = 0
    }
    initChart()
  } catch (error) {
    message.error('获取数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chart) {
    chart.dispose()
    chart = null
  }
})

defineExpose({ handleQuery: fetchData })
</script>

<style scoped>
.aging-analysis-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
  min-height: 0;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px;
  border-radius: 8px;
}

.stat-30 { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-60 { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-90 { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }
.stat-90plus { background: linear-gradient(135deg, #f9f0ff 0%, #efdbff 100%); }

.stat-card-value {
  font-size: 18px;
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
  font-size: 24px;
  color: rgba(0, 0, 0, 0.15);
}

.filter-area {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
}

.chart-area {
  background: #fff;
  border-radius: 8px;
}

.table-area {
  background: #fff;
  border-radius: 8px;
  flex: 1;
  overflow: hidden;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
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
}
</style>
