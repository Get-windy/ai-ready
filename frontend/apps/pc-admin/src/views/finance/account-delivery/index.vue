<template>
  <ARReportPage
    title="账款交账"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    :stat-cards="statCards"
    export-file-name="账款交账"
    :row-key="(record: any) => record.groupKey"
    empty-text="所选范围内暂无回款记录"
    @loaded="handleLoaded"
  >
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
import type { ReportQueryField, StatCardItem, ReportFetchResult } from '@/components/ARReportPage/types'
import { collectionStatsApi } from '@/api/finance'

// ═══ 分组维度（与后端 CollectionStatsController groupBy 一致） ═══
const queryFields: ReportQueryField[] = [
  {
    key: 'groupBy',
    type: 'select',
    label: '交账维度',
    placeholder: '默认按日',
    options: [
      { label: '按日', value: 'day' },
      { label: '按周', value: 'week' },
      { label: '按月', value: 'month' },
      { label: '按业务员', value: 'staff' },
      { label: '按客户', value: 'customer' }
    ],
    width: 140
  },
  { key: 'receiptDateRange', type: 'date-range', label: '收款日期' }
]

// ═══ 表格列（与后端 CollectionStatsDTO.Detail 字段一致） ═══
const columns: any[] = [
  { title: '分组', dataIndex: 'groupKey', key: 'groupKey', width: 140 },
  { title: '名称', dataIndex: 'groupName', key: 'groupName', width: 180, ellipsis: true },
  { title: '收款笔数', dataIndex: 'receiptCount', key: 'receiptCount', width: 100, align: 'right' },
  { title: '收款总额', dataIndex: 'totalAmount', key: 'totalAmount', width: 140, align: 'right' },
  { title: '现金', dataIndex: 'cashAmount', key: 'cashAmount', width: 130, align: 'right' },
  { title: '银行', dataIndex: 'bankAmount', key: 'bankAmount', width: 130, align: 'right' },
  { title: '其他方式', dataIndex: 'otherAmount', key: 'otherAmount', width: 130, align: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 汇总卡片（取自响应 summary 段） ═══
const summary = ref<Record<string, any>>({})
const statCards = computed<StatCardItem[]>(() => {
  if (!Object.keys(summary.value).length) return []
  return [
    { label: '收款笔数', value: Number(summary.value.receiptCount) || 0, suffix: '笔' },
    { label: '收款总额', value: Number(summary.value.totalAmount) || 0, precision: 2, prefix: '¥' },
    { label: '现金', value: Number(summary.value.cashAmount) || 0, precision: 2, prefix: '¥' },
    { label: '银行', value: Number(summary.value.bankAmount) || 0, precision: 2, prefix: '¥' },
    { label: '其他方式', value: Number(summary.value.otherAmount) || 0, precision: 2, prefix: '¥' }
  ]
})

function handleLoaded(result: ReportFetchResult) {
  summary.value = result.raw?.summary || {}
}

// ═══ 数据请求：回款统计（/erp/finance/collection-stats，汇总 + 分组明细） ═══
async function fetcher(params: Record<string, any>) {
  const res: any = await collectionStatsApi.getStats(params)
  const details = Array.isArray(res?.details) ? res.details : []
  return { records: details, total: details.length, summary: res?.summary || {} }
}
</script>
