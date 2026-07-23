<template>
  <ARReportPage
    title="发票统计"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    :normalize-response="normalizeResponse"
    :stat-cards="statCards"
    export-file-name="发票统计"
    row-key="day"
    empty-text="该区间暂无发票记录"
  >
    <template #header-extra>
      <span class="invoice-tip">按日明细由区间内发票列表按开票日期聚合；汇总口径见卡片</span>
    </template>
    <template #bodyCell="{ column, text }">
      <template v-if="['totalAmount', 'taxAmount', 'paidAmount', 'unpaidAmount'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField, ReportFetchResult, StatCardItem } from '@/components/ARReportPage/types'
import { invoiceApi, invoiceAnalyticsApi, type InvoiceStatsSummary, type InvoiceRecordItem } from '@/api/analytics'

// ═══ 付款状态（与后端 PaymentStatus 枚举一致） ═══
const PAYMENT_STATUS_OPTIONS = [
  { label: '待付款', value: 'PENDING' },
  { label: '部分付款', value: 'PARTIALLY_PAID' },
  { label: '已付款', value: 'PAID' },
  { label: '逾期', value: 'OVERDUE' },
  { label: '已退款', value: 'REFUNDED' }
]

const queryFields: ReportQueryField[] = [
  { key: 'invoiceDateRange', type: 'date-range', label: '开票日期' },
  {
    key: 'paymentStatus',
    type: 'select',
    label: '付款状态',
    placeholder: '全部状态',
    options: PAYMENT_STATUS_OPTIONS
  }
]

// ═══ 表格列（按日聚合明细，由发票列表前端汇总） ═══
const columns: any[] = [
  { title: '开票日期', dataIndex: 'day', key: 'day', width: 120 },
  { title: '发票张数', dataIndex: 'count', key: 'count', width: 100, align: 'right' },
  { title: '开票金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 130, align: 'right' },
  { title: '税额', dataIndex: 'taxAmount', key: 'taxAmount', width: 110, align: 'right' },
  { title: '已收金额', dataIndex: 'paidAmount', key: 'paidAmount', width: 130, align: 'right' },
  { title: '未收金额', dataIndex: 'unpaidAmount', key: 'unpaidAmount', width: 130, align: 'right' },
  {
    title: '收款率', key: 'paidRate', width: 90, align: 'right',
    customRender: ({ record }: { record: any }) => {
      const total = Number(record.totalAmount) || 0
      if (total <= 0) return '-'
      return `${(((Number(record.paidAmount) || 0) / total) * 100).toFixed(1)}%`
    }
  },
  {
    title: '平均单张', key: 'avgAmount', width: 110, align: 'right',
    customRender: ({ record }: { record: any }) => {
      const count = Number(record.count) || 0
      if (count <= 0) return '-'
      return formatMoney((Number(record.totalAmount) || 0) / count)
    }
  }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 后端 startDate/endDate 必填，未选择时默认本月至今 */
function resolveRange(params: Record<string, any>): { startDate: string; endDate: string } {
  return {
    startDate: params.startDate || dayjs().startOf('month').format('YYYY-MM-DD'),
    endDate: params.endDate || dayjs().format('YYYY-MM-DD')
  }
}

// ═══ 汇总卡片（独立请求 /erp/invoice/statistics） ═══
const summary = ref<InvoiceStatsSummary | null>(null)

const statCards = computed<StatCardItem[]>(() => {
  const s = summary.value
  return [
    { label: '发票张数', value: Number(s?.totalCount) || 0, suffix: '张' },
    { label: '开票总额', value: Number(s?.totalAmount) || 0, precision: 2, prefix: '¥' },
    { label: '税额合计', value: Number(s?.totalTax) || 0, precision: 2, prefix: '¥' },
    { label: '已收金额', value: Number(s?.totalPaid) || 0, precision: 2, prefix: '¥' },
    { label: '未收金额', value: Number(s?.totalUnpaid) || 0, precision: 2, prefix: '¥', valueStyle: { color: '#fa8c16' } },
    { label: '待收金额', value: Number(s?.pendingAmount) || 0, precision: 2, prefix: '¥', suffix: `${Number(s?.pendingCount) || 0} 笔` },
    { label: '逾期未收', value: Number(s?.overdueAmount) || 0, precision: 2, prefix: '¥', suffix: `${Number(s?.overdueCount) || 0} 笔`, valueStyle: { color: '#f5222d' } }
  ]
})

// ═══ 数据请求（日期范围发票列表 → 前端按付款状态过滤 + 按日聚合） ═══
async function fetcher(params: Record<string, any>): Promise<InvoiceRecordItem[]> {
  const range = resolveRange(params)
  const list = await invoiceAnalyticsApi.dateRangeList(range)
  const items = Array.isArray(list) ? list : []
  if (!params.paymentStatus) return items
  return items.filter(inv => inv.paymentStatus === params.paymentStatus)
}

interface DailyInvoiceRow {
  day: string
  count: number
  totalAmount: number
  taxAmount: number
  paidAmount: number
  unpaidAmount: number
}

function normalizeResponse(res: InvoiceRecordItem[]): ReportFetchResult<DailyInvoiceRow> {
  const byDay = new Map<string, DailyInvoiceRow>()
  for (const inv of Array.isArray(res) ? res : []) {
    const day = String(inv.invoiceDate || '').slice(0, 10) || '未知日期'
    const row = byDay.get(day) || { day, count: 0, totalAmount: 0, taxAmount: 0, paidAmount: 0, unpaidAmount: 0 }
    row.count += 1
    row.totalAmount += Number(inv.totalAmount) || 0
    row.taxAmount += Number(inv.taxAmount) || 0
    row.paidAmount += Number(inv.paidAmount) || 0
    row.unpaidAmount += Number(inv.unpaidAmount) || 0
    byDay.set(day, row)
  }
  const list = [...byDay.values()].sort((a, b) => (a.day < b.day ? 1 : -1))
  return { list, total: list.length, raw: res }
}

onMounted(async () => {
  try {
    const range = resolveRange({})
    const res = await invoiceApi.getStatistics(range.startDate, range.endDate)
    summary.value = (res as unknown as InvoiceStatsSummary) || null
  } catch (e) {
    console.warn('[发票统计] 汇总获取失败', e)
  }
})
</script>

<style scoped>
.invoice-tip {
  font-size: 12px;
  color: #999;
}
</style>
