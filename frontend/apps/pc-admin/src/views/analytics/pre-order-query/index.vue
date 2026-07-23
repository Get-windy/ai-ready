<template>
  <ARReportPage
    title="预订货查询"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="预订货查询"
    row-key="id"
  >
    <template #bodyCell="{ column, text }">
      <template v-if="column.dataIndex === 'status'">
        <a-tag :color="STATUS_MAP[text]?.color || 'default'">
          {{ STATUS_MAP[text]?.label || '未知' }}
        </a-tag>
      </template>
      <template v-else-if="['quantity', 'orderedQuantity', 'unOrderedQuantity'].includes(column.dataIndex as string)">
        {{ formatNumber(text) }}
      </template>
      <template v-else-if="['unitPrice', 'amount'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { preOrderApi } from '@/api/analytics'

// ═══ 预订单状态（与预订货列表页 STATUS_MAP 一致） ═══
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '审核中', color: 'processing' },
  2: { label: '待订货', color: 'orange' },
  3: { label: '部分订货', color: 'blue' },
  4: { label: '已订货', color: 'cyan' },
  5: { label: '已完成', color: 'green' },
  [-1]: { label: '已取消', color: 'red' }
}

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '单号/客户/商品', width: 200 },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value }))
  },
  { key: 'orderDateRange', type: 'date-range', label: '单据日期' }
]

// ═══ 表格列（明细级：每商品行一条） ═══
const columns: any[] = [
  { title: '单号', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '单据日期', dataIndex: 'orderDate', key: 'orderDate', width: 110 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 150, ellipsis: true },
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 180, ellipsis: true },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 70 },
  { title: '预订数量', dataIndex: 'quantity', key: 'quantity', width: 100, align: 'right' },
  { title: '已订货数量', dataIndex: 'orderedQuantity', key: 'orderedQuantity', width: 100, align: 'right' },
  { title: '未订货数量', dataIndex: 'unOrderedQuantity', key: 'unOrderedQuantity', width: 100, align: 'right' },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100, align: 'right' },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 110, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 }
]

function formatNumber(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求（按明细分页，含商品行） ═══
function fetcher(params: Record<string, any>) {
  return preOrderApi.pageDetail(params)
}
</script>
