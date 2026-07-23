<template>
  <ErrorBoundary>
    <PageContainer title="销售欠款分析">
      <!-- ═══ 账龄卡片 ═══ -->
      <ARStatCards
        :items="statCards"
        :loading="agingLoading"
      />

      <!-- ═══ 查询区 ═══ -->
      <div class="search-area">
        <a-form layout="inline">
          <a-form-item>
            <a-input
              v-model:value="keyword"
              placeholder="客户名称/ID"
              allow-clear
              style="width: 220px"
              @press-enter="handleSearch"
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
          title="客户欠款排行 TOP10"
          :option="debtBarOption"
          :loading="chartLoading"
          :height="360"
        />
      </div>

      <!-- ═══ 客户欠款表 ═══ -->
      <div class="table-area">
        <a-table
          :columns="columns"
          :data-source="rows"
          :loading="loading"
          :pagination="tablePagination"
          :row-key="(record: any) => String(record.partnerId)"
          :locale="{ emptyText: '暂无数据' }"
          size="small"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, text }">
            <template v-if="['receivableBalance', 'preReceiptBalance', 'netBalance'].includes(column.dataIndex as string)">
              {{ formatMoney(text) }}
            </template>
          </template>
        </a-table>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { SearchOutlined, ClearOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import ARReportChart from '@/components/ARReportChart/ARReportChart.vue'
import type { StatCardItem } from '@/components/ARReportPage/types'
import { receivableApi, financeAnalyticsApi } from '@/api/analytics'

// ═══ 账龄卡片（独立请求 /erp/finance/receivable/aging） ═══
interface AgingBucket {
  agingPeriod: string
  count: number
  totalAmount: number
}

const agingLoading = ref(false)
const agingData = ref<AgingBucket[]>([])

const statCards = computed<StatCardItem[]>(() => {
  const totalAmount = agingData.value.reduce((acc, b) => acc + (Number(b.totalAmount) || 0), 0)
  const totalCount = agingData.value.reduce((acc, b) => acc + (Number(b.count) || 0), 0)
  const cards: StatCardItem[] = [
    { label: '应收余额合计', value: totalAmount, precision: 2, prefix: '¥', suffix: `${totalCount} 笔` }
  ]
  for (const bucket of agingData.value) {
    cards.push({
      label: `账龄 ${bucket.agingPeriod}`,
      value: Number(bucket.totalAmount) || 0,
      precision: 2,
      prefix: '¥',
      suffix: `${bucket.count} 笔`
    })
  }
  return cards
})

// ═══ 客户欠款表（/erp/finance/partner-balance/page，客户口径） ═══
const loading = ref(false)
const keyword = ref('')
const rows = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const tablePagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`
}))

const columns: any[] = [
  { title: '客户', dataIndex: 'partnerName', key: 'partnerName', ellipsis: true },
  { title: '应收余额', dataIndex: 'receivableBalance', key: 'receivableBalance', width: 140, align: 'right' },
  { title: '预收余额', dataIndex: 'preReceiptBalance', key: 'preReceiptBalance', width: 140, align: 'right' },
  { title: '净余额', dataIndex: 'netBalance', key: 'netBalance', width: 140, align: 'right' },
  { title: '最后业务日期', dataIndex: 'lastBizDate', key: 'lastBizDate', width: 130 }
]

// ═══ 欠款排行图（取前 50 条按应收余额降序取 TOP10） ═══
const chartLoading = ref(false)
const topDebtors = ref<any[]>([])

const debtBarOption = computed(() => {
  const top = [...topDebtors.value].reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 120, right: 50, top: 20, bottom: 30 },
    xAxis: { type: 'value', name: '应收余额(元)' },
    yAxis: { type: 'category', data: top.map(r => r.partnerName || `客户${r.partnerId}`) },
    series: [
      {
        name: '应收余额',
        type: 'bar',
        barMaxWidth: 20,
        itemStyle: { color: '#f5222d', borderRadius: [0, 4, 4, 0] },
        data: top.map(r => Number(r.receivableBalance) || 0)
      }
    ]
  }
})

// ═══ 工具 ═══
function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求 ═══
async function loadAging() {
  agingLoading.value = true
  try {
    const res: any = await receivableApi.getAging()
    agingData.value = Array.isArray(res) ? res : []
  } catch (e) {
    agingData.value = []
    console.warn('[销售欠款分析] 账龄汇总获取失败', e)
  } finally {
    agingLoading.value = false
  }
}

async function loadTable() {
  loading.value = true
  try {
    const res: any = await financeAnalyticsApi.partnerBalancePage({
      partnerType: 'customer',
      onlyNonZero: true,
      keyword: keyword.value || undefined,
      page: pagination.current,
      size: pagination.pageSize
    })
    const page = res?.records ? res : res?.data || { records: [], total: 0 }
    rows.value = page.records || []
    pagination.total = Number(page.total) || 0
  } catch (e) {
    rows.value = []
    pagination.total = 0
    console.warn('[销售欠款分析] 客户欠款获取失败', e)
  } finally {
    loading.value = false
  }
}

async function loadTopDebtors() {
  chartLoading.value = true
  try {
    const res: any = await financeAnalyticsApi.partnerBalancePage({
      partnerType: 'customer',
      onlyNonZero: true,
      keyword: keyword.value || undefined,
      page: 1,
      size: 50
    })
    const page = res?.records ? res : res?.data || { records: [] }
    topDebtors.value = (page.records || [])
      .sort((a: any, b: any) => (Number(b.receivableBalance) || 0) - (Number(a.receivableBalance) || 0))
      .slice(0, 10)
  } catch (e) {
    topDebtors.value = []
    console.warn('[销售欠款分析] 欠款排行获取失败', e)
  } finally {
    chartLoading.value = false
  }
}

function handleTableChange(pag: { current?: number; pageSize?: number }) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 20
  loadTable()
}

function handleSearch() {
  pagination.current = 1
  loadTable()
  loadTopDebtors()
}

function handleReset() {
  keyword.value = ''
  pagination.current = 1
  loadTable()
  loadTopDebtors()
}

onMounted(() => {
  loadAging()
  loadTable()
  loadTopDebtors()
})
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
  grid-template-columns: 1fr;
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
