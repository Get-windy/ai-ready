<template>
  <ARReportPage
    title="回款统计"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    :normalize-response="normalizeResponse"
    :stat-cards="statCards"
    export-file-name="回款统计"
    :row-key="(record: any) => `${record.groupKey}-${record.groupName || ''}`"
    empty-text="该区间暂无回款记录"
    @loaded="onLoaded"
  >
    <template #header-extra>
      <span class="stats-tip">口径：已审核收款单，按收款日期统计；占比 = 分组回款金额 / 回款总额</span>
    </template>
    <template #bodyCell="{ column, text }">
      <template v-if="['totalAmount', 'cashAmount', 'bankAmount', 'otherAmount'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField, ReportFetchResult, StatCardItem } from '@/components/ARReportPage/types'
import { financeAnalyticsApi, type CollectionStats, type CollectionStatsSummary } from '@/api/analytics'

// ═══ 查询字段（分组维度与后端 groupBy 口径一致） ═══
const queryFields: ReportQueryField[] = [
  {
    key: 'groupBy',
    type: 'select',
    label: '分组维度',
    placeholder: '按日',
    allowClear: false,
    width: 140,
    options: [
      { label: '按日', value: 'day' },
      { label: '按周', value: 'week' },
      { label: '按月', value: 'month' },
      { label: '按业务员', value: 'staff' },
      { label: '按客户', value: 'customer' }
    ]
  },
  { key: 'receiptDateRange', type: 'date-range', label: '收款日期' }
]

// ═══ 表格列（分组列标题随维度切换） ═══
const GROUP_LABEL_MAP: Record<string, string> = {
  day: '日期',
  week: '周',
  month: '月份',
  staff: '业务员',
  customer: '客户'
}

const lastGroupBy = ref('day')

const columns = computed<any[]>(() => [
  {
    title: GROUP_LABEL_MAP[lastGroupBy.value] || '分组', key: 'group', width: 160, ellipsis: true,
    customRender: ({ record }: { record: any }) => record.groupName || record.groupKey || '-'
  },
  { title: '回款笔数', dataIndex: 'receiptCount', key: 'receiptCount', width: 100, align: 'right' },
  { title: '回款金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 130, align: 'right' },
  {
    title: '占比', key: 'percent', width: 90, align: 'right',
    customRender: ({ record }: { record: any }) => {
      const total = Number(summary.value?.totalAmount) || 0
      if (total <= 0) return '-'
      return `${(((Number(record.totalAmount) || 0) / total) * 100).toFixed(1)}%`
    }
  },
  {
    title: '单笔均额', key: 'avgAmount', width: 120, align: 'right',
    customRender: ({ record }: { record: any }) => {
      const count = Number(record.receiptCount) || 0
      if (count <= 0) return '-'
      return formatMoney((Number(record.totalAmount) || 0) / count)
    }
  },
  { title: '现金回款', dataIndex: 'cashAmount', key: 'cashAmount', width: 120, align: 'right' },
  { title: '银行回款', dataIndex: 'bankAmount', key: 'bankAmount', width: 120, align: 'right' },
  { title: '其他方式', dataIndex: 'otherAmount', key: 'otherAmount', width: 120, align: 'right' }
])

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 汇总卡片（来自同一响应的 summary 字段） ═══
const summary = ref<CollectionStatsSummary | null>(null)

const statCards = computed<StatCardItem[]>(() => {
  const s = summary.value
  return [
    { label: '回款笔数', value: Number(s?.receiptCount) || 0, suffix: '笔' },
    { label: '回款总额', value: Number(s?.totalAmount) || 0, precision: 2, prefix: '¥' },
    { label: '现金回款', value: Number(s?.cashAmount) || 0, precision: 2, prefix: '¥' },
    { label: '银行回款', value: Number(s?.bankAmount) || 0, precision: 2, prefix: '¥' },
    { label: '其他方式', value: Number(s?.otherAmount) || 0, precision: 2, prefix: '¥' }
  ]
})

function onLoaded(result: ReportFetchResult) {
  summary.value = (result.raw as CollectionStats)?.summary || null
}

// ═══ 数据请求（聚合端点，无后端分页，表格展示全部分组明细） ═══
function fetcher(params: Record<string, any>) {
  lastGroupBy.value = params.groupBy || 'day'
  return financeAnalyticsApi.collectionStats({
    startDate: params.startDate,
    endDate: params.endDate,
    groupBy: params.groupBy || 'day'
  })
}

function normalizeResponse(res: CollectionStats): ReportFetchResult {
  const details = res?.details || []
  return { list: details, total: details.length, raw: res }
}
</script>

<style scoped>
.stats-tip {
  font-size: 12px;
  color: #999;
}
</style>
