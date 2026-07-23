<template>
  <ARReportPage
    title="物流退货收货"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="物流退货收货"
    row-key="id"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.dataIndex === 'status'">
        <a-tag :color="STATUS_MAP[record.status]?.color || 'default'">
          {{ STATUS_MAP[record.status]?.label || '-' }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'totalAmount'">
        {{ formatMoney(record.totalAmount) }}
      </template>
      <template v-else-if="['totalQuantity', 'returnQuantityTotal'].includes(column.dataIndex as string)">
        {{ record[column.dataIndex as string] != null ? Number(record[column.dataIndex as string]).toLocaleString('zh-CN') : '-' }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { saleReturnDocApi } from '@/api/erp'

// ═══ 退货单状态（与后端 SaleReturnDoc 状态流转一致：0草稿→1审核中→2审核通过→3已完成，4已取消） ═══
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '审核中', color: 'orange' },
  2: { label: '审核通过', color: 'blue' },
  3: { label: '已完成', color: 'green' },
  4: { label: '已取消', color: 'red' }
}

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '退货单号/客户', width: 180 },
  { key: 'customerName', type: 'input', label: '客户名称', placeholder: '客户名称', width: 160 },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))
  },
  { key: 'orderDateRange', type: 'date-range', label: '单据日期' }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '退货单号', dataIndex: 'returnDocNo', key: 'returnDocNo', width: 170 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 150, ellipsis: true },
  { title: '收货仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 110 },
  { title: '退货数量', dataIndex: 'returnQuantityTotal', key: 'returnQuantityTotal', width: 100, align: 'right' },
  { title: '退货金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 110, align: 'right' },
  { title: '物流公司', dataIndex: 'logisticsCompany', key: 'logisticsCompany', width: 110, ellipsis: true },
  { title: '运单号', dataIndex: 'waybillNo', key: 'waybillNo', width: 130, ellipsis: true },
  { title: '司机', dataIndex: 'driverName', key: 'driverName', width: 90 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '单据日期', dataIndex: 'orderDate', key: 'orderDate', width: 110 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求（销售退货单 = 退货收货单，后端日期参数为 startDate/endDate） ═══
function fetcher(params: Record<string, any>) {
  return saleReturnDocApi.page(params)
}
</script>
