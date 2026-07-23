<template>
  <ARReportPage
    title="交易分析"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    :normalize-response="normalizeResponse"
    :stat-cards="statCards"
    export-file-name="交易分析"
    row-key="day"
    @loaded="onLoaded"
  >
    <template #header-extra>
      <div
        v-if="paymentDistribution.length"
        class="payment-distribution"
      >
        <span class="payment-distribution__label">支付状态分布：</span>
        <a-tag
          v-for="item in paymentDistribution"
          :key="item.paymentStatus"
          :color="paymentStatusColor(item.paymentStatus)"
        >
          {{ item.paymentStatusName }} {{ item.count }}
        </a-tag>
      </div>
    </template>
    <template #bodyCell="{ column, text, record }">
      <template v-if="['gmv', 'avgOrderAmount'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
      <template v-else-if="column.dataIndex === 'dayGrowth'">
        <span
          v-if="record.dayGrowth !== null && record.dayGrowth !== undefined"
          :style="{ color: record.dayGrowth >= 0 ? '#52c41a' : '#f5222d' }"
        >
          {{ record.dayGrowth >= 0 ? '+' : '' }}{{ Number(record.dayGrowth).toFixed(1) }}%
        </span>
        <span v-else>-</span>
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField, ReportFetchResult, StatCardItem } from '@/components/ARReportPage/types'
import { mallAnalyticsApi, type MallTradeAnalysis } from '@/api/analytics'

const queryFields: ReportQueryField[] = [
  { key: 'orderDateRange', type: 'date-range', label: '下单日期' }
]

// ═══ 表格列（按日交易明细） ═══
const columns: any[] = [
  { title: '日期', dataIndex: 'day', key: 'day', width: 120 },
  { title: '订单数', dataIndex: 'orderCount', key: 'orderCount', width: 100, align: 'right' },
  { title: '订单数环比', dataIndex: 'dayGrowth', key: 'dayGrowth', width: 110, align: 'right' },
  { title: 'GMV', dataIndex: 'gmv', key: 'gmv', width: 150, align: 'right' },
  { title: '客单价', dataIndex: 'avgOrderAmount', key: 'avgOrderAmount', width: 130, align: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 汇总卡片 + 支付状态分布 ═══
const analysis = ref<MallTradeAnalysis | null>(null)

const statCards = computed<StatCardItem[]>(() => {
  const s = analysis.value?.summary
  const daily = analysis.value?.daily || []
  const days = daily.length || 0
  return [
    { label: '总单数', value: Number(s?.totalOrderCount) || 0, suffix: '单' },
    { label: 'GMV', value: Number(s?.totalGmv) || 0, precision: 2, prefix: '¥' },
    { label: '客单价', value: Number(s?.avgOrderAmount) || 0, precision: 2, prefix: '¥' },
    { label: '退款单数', value: Number(s?.refundOrderCount) || 0, suffix: '单' },
    {
      label: '退款率',
      value: Number(s?.refundRate) || 0,
      precision: 2,
      suffix: '%',
      valueStyle: Number(s?.refundRate) > 5 ? { color: '#f5222d' } : undefined
    },
    { label: '日均单数', value: days > 0 ? Math.round((Number(s?.totalOrderCount) || 0) / days) : 0, suffix: '单' },
    { label: '日均GMV', value: days > 0 ? (Number(s?.totalGmv) || 0) / days : 0, precision: 2, prefix: '¥' }
  ]
})

const paymentDistribution = computed(() => analysis.value?.paymentStatusDistribution || [])

/** 支付状态: 0待支付,1支付中,2已支付,3部分支付,4已退款 */
function paymentStatusColor(status: number): string {
  const map: Record<number, string> = {
    0: 'orange',
    1: 'blue',
    2: 'green',
    3: 'gold',
    4: 'red'
  }
  return map[status] || 'default'
}

function onLoaded(result: ReportFetchResult) {
  analysis.value = (result.raw as MallTradeAnalysis) || null
}

// ═══ 数据请求（聚合端点，表格展示按日明细） ═══
function fetcher(params: Record<string, any>) {
  return mallAnalyticsApi.tradeAnalysis({
    startDate: params.startDate,
    endDate: params.endDate
  })
}

function normalizeResponse(res: MallTradeAnalysis): ReportFetchResult {
  const daily = res?.daily || []
  // 计算订单数日环比（列表按日期升序时与上一日比较）
  const list = daily.map((item, index) => {
    const prev = index > 0 ? daily[index - 1] : null
    const prevCount = Number(prev?.orderCount) || 0
    const dayGrowth = prev && prevCount > 0
      ? ((Number(item.orderCount) || 0) - prevCount) / prevCount * 100
      : null
    return { ...item, dayGrowth }
  })
  return { list, total: list.length, raw: res }
}
</script>

<style scoped>
.payment-distribution {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.payment-distribution__label {
  font-size: 13px;
  color: #888;
}
</style>
