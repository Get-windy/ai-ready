<template>
  <div class="aging-analysis-page">
    <a-card title="账龄分析">
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

      <!-- 统计卡片 -->
      <a-row
        :gutter="16"
        class="stats-area"
      >
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="0-30天"
              :value="stats.aging30"
              :precision="2"
              prefix="¥"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="31-60天"
              :value="stats.aging60"
              :precision="2"
              prefix="¥"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="61-90天"
              :value="stats.aging90"
              :precision="2"
              prefix="¥"
              :value-style="{ color: '#fa8c16' }"
            />
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic
              title="90天以上"
              :value="stats.aging90plus"
              :precision="2"
              prefix="¥"
              :value-style="{ color: '#ff4d4f' }"
            />
          </a-card>
        </a-col>
      </a-row>

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
          <a-table
            :columns="columns"
            :data-source="dataSource"
            :loading="loading"
            :pagination="pagination"
            row-key="id"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'totalAmount'">
                ¥{{ record.totalAmount?.toFixed(2) }}
              </template>
              <template v-else-if="column.key === 'agingDays'">
                <a-tag :color="getAgingColor(record.agingDays)">
                  {{ record.agingDays }}天
                </a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { message } from 'ant-design-vue'
import * as echarts from 'echarts'

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
const dataSource = ref<AgingData[]>([])

const queryParams = reactive({
  customerName: '',
  endDate: undefined as string | undefined
})

const stats = reactive({
  aging30: 0,
  aging60: 0,
  aging90: 0,
  aging90plus: 0
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`
})

const columns = [
  {
    title: '客户名称',
    dataIndex: 'customerName',
    key: 'customerName',
    width: 150
  },
  {
    title: '订单号',
    dataIndex: 'orderNo',
    key: 'orderNo',
    width: 150
  },
  {
    title: '应收金额',
    key: 'totalAmount',
    width: 120
  },
  {
    title: '到期日期',
    dataIndex: 'dueDate',
    key: 'dueDate',
    width: 120
  },
  {
    title: '账龄天数',
    key: 'agingDays',
    width: 100
  }
]

const getAgingColor = (days: number) => {
  if (days <= 30) return 'green'
  if (days <= 60) return 'blue'
  if (days <= 90) return 'orange'
  return 'red'
}

const initChart = () => {
  nextTick(() => {
    if (!chartRef.value) return

    const chart = echarts.init(chartRef.value)
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
          data: [stats.aging30]
        },
        {
          name: '31-60天',
          type: 'bar',
          data: [stats.aging60]
        },
        {
          name: '61-90天',
          type: 'bar',
          data: [stats.aging90]
        },
        {
          name: '90天以上',
          type: 'bar',
          data: [stats.aging90plus]
        }
      ]
    }
    chart.setOption(option)

    window.addEventListener('resize', () => chart.resize())
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

const fetchData = async () => {
  loading.value = true
  try {
    // TODO: 调用实际API
    setTimeout(() => {
      dataSource.value = [
        {
          id: 1,
          customerName: '客户A',
          orderNo: 'SO20260328001',
          totalAmount: 5000,
          dueDate: '2026-04-15',
          agingDays: 2
        },
        {
          id: 2,
          customerName: '客户B',
          orderNo: 'SO20260328002',
          totalAmount: 15000,
          dueDate: '2026-03-10',
          agingDays: 34
        },
        {
          id: 3,
          customerName: '客户C',
          orderNo: 'SO20260328003',
          totalAmount: 20000,
          dueDate: '2026-01-15',
          agingDays: 88
        }
      ]

      stats.aging30 = 5000
      stats.aging60 = 15000
      stats.aging90 = 20000
      stats.aging90plus = 10000

      pagination.total = dataSource.value.length
      loading.value = false

      initChart()
    }, 500)
  } catch (error) {
    message.error('获取数据失败')
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.aging-analysis-page {
  padding: 24px;
}

.filter-area {
  margin-bottom: 16px;
}

.stats-area {
  margin-bottom: 16px;
}

.chart-area {
  margin-bottom: 16px;
}

.table-area {
  margin-bottom: 16px;
}
</style>