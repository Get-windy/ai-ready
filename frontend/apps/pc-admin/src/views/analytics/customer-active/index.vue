<template>
  <ARReportPage
    title="客户活跃分析"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    export-file-name="客户活跃分析"
    row-key="customerId"
  >
    <template #bodyCell="{ column, text }">
      <template v-if="column.dataIndex === 'activityLevel'">
        <a-tag :color="ACTIVITY_COLOR[text] || 'default'">
          {{ text || '-' }}
        </a-tag>
      </template>
      <template v-else-if="['recentOrderCount', 'totalOrderCount', 'followCount'].includes(column.dataIndex as string)">
        {{ formatNumber(text) }}
      </template>
      <template v-else-if="['recentOrderAmount', 'totalOrderAmount'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { saleAnalyticsApi } from '@/api/analytics'

// ═══ 活跃度分层着色（后端返回 活跃/一般/沉默） ═══
const ACTIVITY_COLOR: Record<string, string> = {
  活跃: 'green',
  一般: 'orange',
  沉默: 'default'
}

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '客户名称', width: 180 },
  { key: 'days', type: 'input', label: '统计天数', placeholder: '默认 30 天', width: 120 }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 170, ellipsis: true },
  { title: '近N天单数', dataIndex: 'recentOrderCount', key: 'recentOrderCount', width: 100, align: 'right' },
  { title: '近N天金额', dataIndex: 'recentOrderAmount', key: 'recentOrderAmount', width: 120, align: 'right' },
  { title: '最近下单时间', dataIndex: 'lastOrderTime', key: 'lastOrderTime', width: 160 },
  { title: '历史单数', dataIndex: 'totalOrderCount', key: 'totalOrderCount', width: 90, align: 'right' },
  { title: '历史总额', dataIndex: 'totalOrderAmount', key: 'totalOrderAmount', width: 120, align: 'right' },
  { title: '跟进次数', dataIndex: 'followCount', key: 'followCount', width: 90, align: 'right' },
  { title: '活跃度', dataIndex: 'activityLevel', key: 'activityLevel', width: 90 }
]

function formatNumber(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求（统计天数默认 30） ═══
function fetcher(params: Record<string, any>) {
  return saleAnalyticsApi.customerActivePage({ days: 30, ...params })
}
</script>
