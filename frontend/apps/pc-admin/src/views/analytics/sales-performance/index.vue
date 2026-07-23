<template>
  <ErrorBoundary>
    <PageContainer title="销售业绩">
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
          title="每日订单量"
          :option="orderBarOption"
          :loading="loading"
          :height="340"
        />
        <ARReportChart
          title="每日销售金额"
          :option="amountBarOption"
          :loading="loading"
          :height="340"
        />
      </div>

      <!-- ═══ 逐日明细表 ═══ -->
      <div class="table-area">
        <a-table
          :columns="columns"
          :data-source="dateRows"
          :loading="loading"
          :pagination="tablePagination"
          row-key="orderDay"
          :locale="{ emptyText: '暂无数据' }"
          size="small"
        >
          <template #bodyCell="{ column, text }">
            <template v-if="column.dataIndex === 'totalAmount'">
              {{ formatMoney(text) }}
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
const orderStats = ref<Record<string, any>>({})
const centerStats = ref<Record<string, any>>({})
const dateRows = ref<any[]>([])

// ═══ 统计卡片 ═══
const periodAmount = computed(() =>
  dateRows.value.reduce((acc, r) => acc + (Number(r.totalAmount) || 0), 0)
)

const statCards = computed<StatCardItem[]>(() => {
  const o = orderStats.value
  const c = centerStats.value
  return [
    { label: '今日订单', value: Number(o.todayOrderCount) || 0, suffix: '单' },
    { label: '本月订单', value: Number(o.monthOrderCount) || 0, suffix: '单' },
    { label: '待处理', value: Number(o.pendingProcessCount) || 0, suffix: '单' },
    { label: '待审批', value: Number(o.pendingApprovalCount) || 0, suffix: '单' },
    { label: '期间订单', value: Number(c.totalOrders) || 0, suffix: '单' },
    { label: '期间销售额', value: periodAmount.value, precision: 2, prefix: '¥' },
    { label: '期间完成', value: Number(c.completedCount) || 0, suffix: '单' }
  ]
})

// ═══ 表格 ═══
const columns: any[] = [
  { title: '日期', dataIndex: 'orderDay', key: 'orderDay', width: 120 },
  { title: '订单数', dataIndex: 'totalOrders', key: 'totalOrders', width: 100, align: 'right' },
  { title: '销售金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 140, align: 'right' },
  { title: '待审核', dataIndex: 'pendingReviewCount', key: 'pendingReviewCount', width: 100, align: 'right' },
  { title: '待出库', dataIndex: 'pendingOutboundCount', key: 'pendingOutboundCount', width: 100, align: 'right' },
  { title: '已出库', dataIndex: 'outboundCount', key: 'outboundCount', width: 100, align: 'right' },
  { title: '发货完成', dataIndex: 'shippedCount', key: 'shippedCount', width: 100, align: 'right' },
  { title: '交易完成', dataIndex: 'completedCount', key: 'completedCount', width: 100, align: 'right' }
]

const tablePagination = {
  pageSize: 20,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`
}

// ═══ 图表 ═══
const sortedRows = computed(() =>
  [...dateRows.value].sort((a, b) => String(a.orderDay).localeCompare(String(b.orderDay)))
)

const orderBarOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 60, right: 30, top: 30, bottom: 30 },
  xAxis: { type: 'category', data: sortedRows.value.map(r => r.orderDay) },
  yAxis: { type: 'value', minInterval: 1 },
  series: [
    {
      name: '订单数',
      type: 'bar',
      barMaxWidth: 22,
      itemStyle: { color: '#1890ff', borderRadius: [4, 4, 0, 0] },
      data: sortedRows.value.map(r => Number(r.totalOrders) || 0)
    }
  ]
}))

const amountBarOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 70, right: 30, top: 30, bottom: 30 },
  xAxis: { type: 'category', data: sortedRows.value.map(r => r.orderDay) },
  yAxis: { type: 'value', name: '金额(元)' },
  series: [
    {
      name: '销售金额',
      type: 'bar',
      barMaxWidth: 22,
      itemStyle: { color: '#52c41a', borderRadius: [4, 4, 0, 0] },
      data: sortedRows.value.map(r => Number(r.totalAmount) || 0)
    }
  ]
}))

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
    const [statsRes, centerRes, dateRes] = await Promise.all([
      saleOrderApi.getStats({ tenantId: tenantId.value }),
      saleOrderApi.getCenterStats(params),
      saleOrderApi.getCenterGroupByDate(params)
    ])
    orderStats.value = unwrap(statsRes) || {}
    centerStats.value = unwrap(centerRes) || {}
    const list = unwrap(dateRes)
    dateRows.value = Array.isArray(list) ? list : []
  } catch (e) {
    orderStats.value = {}
    centerStats.value = {}
    dateRows.value = []
    console.warn('[销售业绩] 数据获取失败', e)
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
