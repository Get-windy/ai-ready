<template>
  <ARReportPage
    title="销售履约分析"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="销售履约分析"
    row-key="id"
  >
    <template #bodyCell="{ column, text, record }">
      <template v-if="column.dataIndex === 'statusName'">
        <a-tag :color="STATUS_MAP[record.status]?.color || 'default'">
          {{ text || STATUS_MAP[record.status]?.text || '未知' }}
        </a-tag>
      </template>
      <template v-else-if="['totalQuantity', 'shippedQuantity', 'unshippedQuantity'].includes(column.dataIndex as string)">
        {{ formatNumber(text) }}
      </template>
      <template v-else-if="['billAmount', 'receivedAmount'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { saleOrderApi } from '@/api/analytics'
import { useUserStore } from '@/stores/user'

// ═══ 销售订单状态（与订单处理中心 STATUS_MAP 一致） ═══
const STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审核', color: 'orange' },
  2: { text: '待发货', color: 'processing' },
  3: { text: '部分发货', color: 'warning' },
  4: { text: '发货完成', color: 'success' },
  5: { text: '交易完成', color: 'green' },
  6: { text: '已取消', color: 'red' }
}

const userStore = useUserStore()

const queryFields: ReportQueryField[] = [
  { key: 'orderNo', type: 'input', label: '订单号', placeholder: '订单编号', width: 180 },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.text, value: Number(value) }))
  },
  { key: 'orderDateRange', type: 'date-range', label: '订单日期' }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 170 },
  { title: '订单日期', dataIndex: 'orderDate', key: 'orderDate', width: 110 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 160, ellipsis: true },
  { title: '业务员', dataIndex: 'salesmanName', key: 'salesmanName', width: 100 },
  { title: '订单金额', dataIndex: 'billAmount', key: 'billAmount', width: 120, align: 'right' },
  { title: '总数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 90, align: 'right' },
  { title: '已发货', dataIndex: 'shippedQuantity', key: 'shippedQuantity', width: 90, align: 'right' },
  { title: '未发货', dataIndex: 'unshippedQuantity', key: 'unshippedQuantity', width: 90, align: 'right' },
  { title: '状态', dataIndex: 'statusName', key: 'statusName', width: 100 },
  { title: '预计发货时间', dataIndex: 'expectedShipTime', key: 'expectedShipTime', width: 160 }
]

function formatNumber(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求（tenantId 为后端必传参数） ═══
function fetcher(params: Record<string, any>) {
  return saleOrderApi.getCenterFulfillmentPage({
    tenantId: userStore?.tenantId || 1,
    ...params
  })
}
</script>
