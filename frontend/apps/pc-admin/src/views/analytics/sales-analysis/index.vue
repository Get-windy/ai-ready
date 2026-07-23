<template>
  <ErrorBoundary>
    <PageContainer title="销售分析">
      <!-- ═══ 统计卡片 ═══ -->
      <ARStatCards
        :items="statCards"
        :loading="loading"
      />

      <!-- ═══ 查询区 ═══ -->
      <div class="search-area">
        <a-form layout="inline">
          <a-form-item label="订单日期">
            <a-range-picker
              v-model:value="dateRange"
              :allow-clear="false"
              style="width: 240px"
            />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button
                type="primary"
                @click="handleSearch"
              >
                <template #icon>
                  <SearchOutlined />
                </template>查询
              </a-button>
              <a-button @click="handleReset">
                <template #icon>
                  <ClearOutlined />
                </template>重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- ═══ 图表区 ═══ -->
      <div class="chart-grid">
        <ARReportChart
          title="销售趋势"
          :option="trendOption"
          :loading="loading"
          :height="340"
        />
        <ARReportChart
          title="客户销售排行 TOP10"
          :option="rankOption"
          :loading="loading"
          :height="340"
        />
      </div>

      <!-- ═══ 客户明细表 ═══ -->
      <div class="table-area">
        <a-table
          :columns="columns"
          :data-source="customerRows"
          :loading="loading"
          :pagination="tablePagination"
          :row-key="(record: any) => String(record.customerId ?? record.customerName)"
          :locale="{ emptyText: '暂无数据' }"
          size="small"
        >
          <template #bodyCell="{ column, text, index }">
            <template v-if="column.dataIndex === 'totalAmount'">
              {{ formatMoney(text) }}
            </template>
            <template v-else-if="column.dataIndex === 'rank'">
              <a-tag :color="index < 3 ? 'gold' : 'default'">
                {{ index + 1 }}
              </a-tag>
            </template>
          </template>
        </a-table>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { SearchOutlined, ClearOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import ARReportChart from '@/components/ARReportChart/ARReportChart.vue'
import type { StatCardItem } from '@/components/ARReportPage/types'
import { saleOrderApi } from '@/api/analytics'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const tenantId = computed(() => userStore.tenantId || 1)

// ═══ 状态 ═══
const loading = ref(false)
const dateRange = ref<[Dayjs, Dayjs]>([dayjs().subtract(29, 'day'), dayjs()])
const centerStats = ref<Record<string, any>>({})
const dateRows = ref<any[]>([])
const customerRows = ref<any[]>([])

// ═══ 表格 ═══
const columns: any[] = [
  { title: '排名', dataIndex: 'rank', key: 'rank', width: 70 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', ellipsis: true },
  { title: '订单数', dataIndex: 'totalOrders', key: 'totalOrders', width: 90, align: 'right' },
  { title: '销售金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 130, align: 'right' },
  { title: '待审核', dataIndex: 'pendingReviewCount', key: 'pendingReviewCount', width: 90, align: 'right' },
  { title: '待出库', dataIndex: 'pendingOutboundCount', key: 'pendingOutboundCount', width: 90, align: 'right' },
  { title: '已出库', dataIndex: 'outboundCount', key: 'outboundCount', width: 90, align: 'right' },
  { title: '交易完成', dataIndex: 'completedCount', key: 'completedCount', width: 90, align: 'right' }
]

const tablePagination = {
  pageSize: 20,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`
}

// ═══ 统计卡片 ═══
const totalAmount = computed(() =>
  dateRows.value.reduce((acc, r) => acc + (Number(r.totalAmount) || 0), 0)
)

const statCards = computed<StatCardItem[]>(() => {
  const s = centerStats.value
  const orders = Number(s.totalOrders) || 0
  return [
    { label: '订单总数', value: orders, suffix: '单' },
    { label: '销售金额', value: totalAmount.value, precision: 2, prefix: '¥' },
    { label: '客单价', value: orders > 0 ? totalAmount.value / orders : 0, precision: 2, prefix: '¥' },
    { label: '待出库', value: Number(s.pendingOutbound) || 0, suffix: '单' },
    { label: '已出库', value: Number(s.outboundCount) || 0, suffix: '单' },
    { label: '交易完成', value: Number(s.completedCount) || 0, suffix: '单' }
  ]
})

// ═══ 图表 ═══
const trendOption = computed(() => {
  const rows = [...dateRows.value].sort((a, b) => String(a.orderDay).localeCompare(String(b.orderDay)))
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['销售金额', '订单数'] },
    grid: { left: 70, right: 50, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: rows.map(r => r.orderDay) },
    yAxis: [
      { type: 'value', name: '金额(元)' },
      { type: 'value', name: '订单数', minInterval: 1 }
    ],
    series: [
      {
        name: '销售金额',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.12 },
        data: rows.map(r => Number(r.totalAmount) || 0)
      },
      {
        name: '订单数',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: rows.map(r => Number(r.totalOrders) || 0)
      }
    ]
  }
})

const rankOption = computed(() => {
  const top = [...customerRows.value]
    .sort((a, b) => (Number(b.totalAmount) || 0) - (Number(a.totalAmount) || 0))
    .slice(0, 10)
    .reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 110, right: 40, top: 20, bottom: 30 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: top.map(r => r.customerName || `客户${r.customerId}`) },
    series: [
      {
        name: '销售金额',
        type: 'bar',
        barMaxWidth: 18,
        itemStyle: { color: '#1890ff', borderRadius: [0, 4, 4, 0] },
        data: top.map(r => Number(r.totalAmount) || 0)
      }
    ]
  }
})

// ═══ 工具 ═══
function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function unwrap(res: any): any {
  return res?.data ?? res
}

// ═══ 数据请求 ═══
async function loadData() {
  loading.value = true
  const params = {
    tenantId: tenantId.value,
    startDate: dateRange.value[0].format('YYYY-MM-DD'),
    endDate: dateRange.value[1].format('YYYY-MM-DD')
  }
  try {
    const [statsRes, dateRes, customerRes] = await Promise.all([
      saleOrderApi.getCenterStats(params),
      saleOrderApi.getCenterGroupByDate(params),
      saleOrderApi.getCenterGroupByCustomer(params)
    ])
    centerStats.value = unwrap(statsRes) || {}
    const dateList = unwrap(dateRes)
    dateRows.value = Array.isArray(dateList) ? dateList : []
    const customerList = unwrap(customerRes)
    customerRows.value = Array.isArray(customerList) ? customerList : []
  } catch (e) {
    centerStats.value = {}
    dateRows.value = []
    customerRows.value = []
    console.warn('[销售分析] 数据获取失败', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadData()
}

function handleReset() {
  dateRange.value = [dayjs().subtract(29, 'day'), dayjs()]
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.search-area {
  background: #fff;
  padding: 16px 20px 0;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(420px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.table-area {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
</style>
