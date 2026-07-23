<template>
  <ARReportPage
    title="业务员提成"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    export-file-name="业务员提成"
    row-key="referrerId"
  >
    <template #bodyCell="{ column, text }">
      <template v-if="['orderCount', 'recordCount', 'settledCount', 'unsettledCount'].includes(column.dataIndex as string)">
        {{ formatNumber(text) }}
      </template>
      <template v-else-if="['totalOrderAmount', 'totalCommissionAmount', 'settledCommissionAmount', 'unsettledCommissionAmount'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { commissionApi } from '@/api/analytics'

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '业务员名称', width: 180 },
  { key: 'commissionDateRange', type: 'date-range', label: '提成日期' }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '业务员', dataIndex: 'staffName', key: 'staffName', width: 130 },
  { title: '提成记录数', dataIndex: 'recordCount', key: 'recordCount', width: 110, align: 'right' },
  { title: '成单数', dataIndex: 'orderCount', key: 'orderCount', width: 90, align: 'right' },
  { title: '订单金额合计', dataIndex: 'totalOrderAmount', key: 'totalOrderAmount', width: 130, align: 'right' },
  { title: '提成金额合计', dataIndex: 'totalCommissionAmount', key: 'totalCommissionAmount', width: 130, align: 'right' },
  { title: '已结提成', dataIndex: 'settledCommissionAmount', key: 'settledCommissionAmount', width: 120, align: 'right' },
  { title: '未结提成', dataIndex: 'unsettledCommissionAmount', key: 'unsettledCommissionAmount', width: 120, align: 'right' }
]

function formatNumber(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求 ═══
function fetcher(params: Record<string, any>) {
  return commissionApi.staffSummaryPage(params)
}
</script>
