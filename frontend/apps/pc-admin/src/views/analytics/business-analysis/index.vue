<template>
  <ErrorBoundary>
    <PageContainer title="经营分析">
      <!-- ═══ 财务 KPI 卡片 ═══ -->
      <ARStatCards
        :items="statCards"
        :loading="kpiLoading"
      />

      <!-- ═══ 查询区（作用于回款趋势与明细） ═══ -->
      <div class="search-area">
        <a-form layout="inline">
          <a-form-item label="收款日期">
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
          title="回款趋势"
          :option="trendOption"
          :loading="loading"
          :height="340"
        />
        <ARReportChart
          title="回款方式构成"
          :option="payTypeOption"
          :loading="loading"
          :height="340"
        />
      </div>

      <!-- ═══ 回款明细表（按日） ═══ -->
      <div class="table-area">
        <a-table
          :columns="columns"
          :data-source="detailRows"
          :loading="loading"
          :pagination="tablePagination"
          row-key="groupKey"
          :locale="{ emptyText: '暂无数据' }"
          size="small"
        >
          <template #bodyCell="{ column, text }">
            <template v-if="['totalAmount', 'cashAmount', 'bankAmount', 'otherAmount'].includes(column.dataIndex as string)">
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
import { reportApi, financeAnalyticsApi } from '@/api/analytics'
import type { CollectionStats } from '@/api/analytics'

// ═══ 状态 ═══
const kpiLoading = ref(false)
const loading = ref(false)
const dateRange = ref<[Dayjs, Dayjs]>([dayjs().subtract(29, 'day'), dayjs()])
const kpi = ref<Record<string, any>>({})
const collectionStats = ref<CollectionStats | null>(null)

// ═══ KPI 卡片（/erp/finance/report/v2/dashboard，后端固定本年/本期口径） ═══
const statCards = computed<StatCardItem[]>(() => {
  const k = kpi.value
  const period = k.fiscalYear ? `${k.fiscalYear}年${k.fiscalPeriod}期` : ''
  return [
    { label: `总资产${period ? `（${period}）` : ''}`, value: Number(k.totalAssets) || 0, precision: 2, prefix: '¥' },
    { label: '总负债', value: Number(k.totalLiabilities) || 0, precision: 2, prefix: '¥' },
    { label: '营业收入（本年）', value: Number(k.totalRevenue) || 0, precision: 2, prefix: '¥' },
    { label: '营业成本（本年）', value: Number(k.totalCost) || 0, precision: 2, prefix: '¥' },
    { label: '期间费用（本年）', value: Number(k.totalExpense) || 0, precision: 2, prefix: '¥' },
    {
      label: '净利润（本年）',
      value: Number(k.netProfit) || 0,
      precision: 2,
      prefix: '¥',
      valueStyle: { color: (Number(k.netProfit) || 0) >= 0 ? '#52c41a' : '#f5222d' }
    }
  ]
})

// ═══ 回款明细 ═══
const detailRows = computed(() => collectionStats.value?.details || [])

const tablePagination = {
  pageSize: 20,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`
}

const columns: any[] = [
  { title: '日期', dataIndex: 'groupName', key: 'groupName', width: 120 },
  { title: '收款笔数', dataIndex: 'receiptCount', key: 'receiptCount', width: 100, align: 'right' },
  { title: '回款总额', dataIndex: 'totalAmount', key: 'totalAmount', width: 140, align: 'right' },
  { title: '现金', dataIndex: 'cashAmount', key: 'cashAmount', width: 130, align: 'right' },
  { title: '银行', dataIndex: 'bankAmount', key: 'bankAmount', width: 130, align: 'right' },
  { title: '其他', dataIndex: 'otherAmount', key: 'otherAmount', width: 130, align: 'right' }
]

// ═══ 图表 ═══
const trendOption = computed(() => {
  const rows = [...detailRows.value].sort((a, b) => String(a.groupKey).localeCompare(String(b.groupKey)))
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['回款金额', '收款笔数'] },
    grid: { left: 70, right: 50, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: rows.map(r => r.groupName || r.groupKey) },
    yAxis: [
      { type: 'value', name: '金额(元)' },
      { type: 'value', name: '笔数', minInterval: 1 }
    ],
    series: [
      {
        name: '回款金额',
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.12 },
        data: rows.map(r => Number(r.totalAmount) || 0)
      },
      {
        name: '收款笔数',
        type: 'line',
        smooth: true,
        yAxisIndex: 1,
        data: rows.map(r => Number(r.receiptCount) || 0)
      }
    ]
  }
})

const payTypeOption = computed(() => {
  const s = collectionStats.value?.summary
  const data = [
    { name: '现金', value: Number(s?.cashAmount) || 0 },
    { name: '银行', value: Number(s?.bankAmount) || 0 },
    { name: '其他', value: Number(s?.otherAmount) || 0 }
  ].filter(d => d.value > 0)
  return {
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c}（{d}%）' },
    legend: { bottom: 0 },
    series: [
      {
        name: '回款方式',
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '45%'],
        label: { formatter: '{b}\n{d}%' },
        data
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
async function loadKpi() {
  kpiLoading.value = true
  try {
    const res: any = await reportApi.getDashboard()
    kpi.value = res?.data ?? res ?? {}
  } catch (e) {
    kpi.value = {}
    console.warn('[经营分析] 财务KPI获取失败', e)
  } finally {
    kpiLoading.value = false
  }
}

async function loadCollection() {
  loading.value = true
  try {
    const res = await financeAnalyticsApi.collectionStats({
      startDate: dateRange.value[0].format('YYYY-MM-DD'),
      endDate: dateRange.value[1].format('YYYY-MM-DD'),
      groupBy: 'day'
    })
    collectionStats.value = res ?? null
  } catch (e) {
    collectionStats.value = null
    console.warn('[经营分析] 回款统计获取失败', e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loadCollection()
}

function handleReset() {
  dateRange.value = [dayjs().subtract(29, 'day'), dayjs()]
  loadCollection()
}

onMounted(() => {
  loadKpi()
  loadCollection()
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
