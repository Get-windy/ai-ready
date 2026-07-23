<template>
  <ARReportPage
    title="按单付款"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="按单付款"
    row-key="id"
  >
    <template #bodyCell="{ column, text }">
      <template v-if="column.dataIndex === 'status'">
        <a-tag :color="STATUS_MAP[text]?.color || 'default'">
          {{ STATUS_MAP[text]?.label || text }}
        </a-tag>
      </template>
      <template v-else-if="['paymentAmount', 'verifiedAmount', 'pendingAmount'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { paymentApi } from '@/api/finance'

// ═══ 付款单状态（与后端 PaymentStatus 一致） ═══
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  [-1]: { label: '已取消', color: 'red' },
  0: { label: '草稿', color: 'default' },
  1: { label: '待审批', color: 'orange' },
  2: { label: '已审批', color: 'blue' },
  3: { label: '核销中', color: 'orange' },
  4: { label: '已核销', color: 'green' },
  5: { label: '已完成', color: 'green' }
}

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '付款单号/供应商', width: 180 },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) })),
    width: 140
  }
]

// ═══ 表格列（与后端 PaymentVO 字段一致） ═══
const columns: any[] = [
  { title: '付款单号', dataIndex: 'paymentNo', key: 'paymentNo', width: 170 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 150, ellipsis: true },
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '付款金额', dataIndex: 'paymentAmount', key: 'paymentAmount', width: 120, align: 'right' },
  { title: '已核销', dataIndex: 'verifiedAmount', key: 'verifiedAmount', width: 110, align: 'right' },
  { title: '待核销', dataIndex: 'pendingAmount', key: 'pendingAmount', width: 110, align: 'right' },
  { title: '付款日期', dataIndex: 'paymentDate', key: 'paymentDate', width: 110 },
  { title: '支付方式', dataIndex: 'paymentMethod', key: 'paymentMethod', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90, align: 'center' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求：付款单分页（/erp/payment/page，pageNum/pageSize 风格） ═══
function fetcher(params: Record<string, any>) {
  return paymentApi.getPage(params)
}
</script>
