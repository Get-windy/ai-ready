<template>
  <ARReportPage
    title="发货查询"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="发货查询"
    row-key="id"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.dataIndex === 'status'">
        <a-tag :color="STATUS_MAP[record.status]?.color || 'default'">
          {{ record.statusDesc || STATUS_MAP[record.status]?.label || '-' }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'totalAmount'">
        {{ formatMoney(record.totalAmount) }}
      </template>
      <template v-else-if="column.dataIndex === 'totalQuantity'">
        {{ record.totalQuantity != null ? Number(record.totalQuantity).toLocaleString('zh-CN') : '-' }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { outboundApi } from '@/api/erp'

// ═══ 出库单状态（与后端 OutboundStatus 枚举一致） ═══
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '待审批', color: 'orange' },
  2: { label: '已审批', color: 'blue' },
  3: { label: '待拣货', color: 'orange' },
  4: { label: '拣货中', color: 'processing' },
  5: { label: '已拣货', color: 'cyan' },
  6: { label: '待打包', color: 'orange' },
  7: { label: '打包中', color: 'processing' },
  8: { label: '已打包', color: 'orange' },
  9: { label: '待发货', color: 'geekblue' },
  10: { label: '已发货', color: 'blue' },
  11: { label: '已完成', color: 'green' },
  12: { label: '已取消', color: 'red' }
}

const queryFields: ReportQueryField[] = [
  { key: 'outboundNo', type: 'input', label: '发货单号', placeholder: '出库单号', width: 160 },
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '客户/单号/收货人', width: 180 },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))
  },
  { key: 'outboundDateRange', type: 'date-range', label: '出库日期', startKey: 'dateStart', endKey: 'dateEnd' }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '发货单号', dataIndex: 'outboundNo', key: 'outboundNo', width: 160 },
  { title: '来源订单', dataIndex: 'orderNo', key: 'orderNo', width: 150 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 140, ellipsis: true },
  { title: '数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 90, align: 'right' },
  { title: '发货金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 110, align: 'right' },
  { title: '物流公司', dataIndex: 'logisticsCompany', key: 'logisticsCompany', width: 120, ellipsis: true },
  { title: '物流单号', dataIndex: 'trackingNumber', key: 'trackingNumber', width: 140, ellipsis: true },
  { title: '司机', dataIndex: 'deliveryDriver', key: 'deliveryDriver', width: 90 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '发货时间', dataIndex: 'shippedTime', key: 'shippedTime', width: 160 },
  { title: '出库日期', dataIndex: 'outboundDate', key: 'outboundDate', width: 110 }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求（销售出库单） ═══
function fetcher(params: Record<string, any>) {
  return outboundApi.page(params)
}
</script>
