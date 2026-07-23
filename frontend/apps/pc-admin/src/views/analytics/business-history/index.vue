<template>
  <ARReportPage
    title="经营历程"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    export-file-name="经营历程"
    :row-key="(record: any) => `${record.docTypeCode}-${record.docNo}`"
  >
    <template #bodyCell="{ column, text }">
      <template v-if="column.dataIndex === 'statusText'">
        <a-tag :color="statusColor(text)">
          {{ text }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'amount'">
        {{ formatMoney(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { docQueryApi } from '@/api/analytics'

// ═══ 单据类型（与后端 DocQueryService 13 个分支一致） ═══
const DOC_TYPE_OPTIONS = [
  { label: '销售订单', value: 'SALE_ORDER' },
  { label: '销售出库单', value: 'SALE_OUTBOUND' },
  { label: '销售退货单', value: 'SALE_RETURN' },
  { label: '销售预订单', value: 'SALE_PRE_ORDER' },
  { label: '采购订单', value: 'PURCHASE_ORDER' },
  { label: '采购入库单', value: 'PURCHASE_INBOUND' },
  { label: '采购退货单', value: 'PURCHASE_RETURN' },
  { label: '收款单', value: 'RECEIPT' },
  { label: '付款单', value: 'PAYMENT' },
  { label: '调拨单', value: 'STOCK_TRANSFER' },
  { label: '报损单', value: 'STOCK_DAMAGE' },
  { label: '报溢单', value: 'STOCK_OVERFLOW' },
  { label: '费用申请单', value: 'EXPENSE' }
]

const queryFields: ReportQueryField[] = [
  { key: 'docType', type: 'select', label: '单据类型', placeholder: '全部单据', options: DOC_TYPE_OPTIONS },
  { key: 'docNo', type: 'input', label: '单号', placeholder: '单据编号', width: 180 },
  { key: 'partnerName', type: 'input', label: '往来单位', placeholder: '客户/供应商', width: 180 },
  { key: 'bizDateRange', type: 'date-range', label: '业务日期' }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '单据类型', dataIndex: 'docType', key: 'docType', width: 110 },
  { title: '单号', dataIndex: 'docNo', key: 'docNo', width: 160 },
  { title: '业务日期', dataIndex: 'bizDate', key: 'bizDate', width: 160 },
  { title: '往来单位', dataIndex: 'partnerName', key: 'partnerName', width: 160, ellipsis: true },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 120, align: 'right' },
  { title: '状态', dataIndex: 'statusText', key: 'statusText', width: 100 },
  { title: '操作人', dataIndex: 'createBy', key: 'createBy', width: 100 }
]

// ═══ 状态着色 ═══
function statusColor(statusText: string): string {
  if (!statusText) return 'default'
  if (statusText.includes('完成') || statusText.includes('已审核')) return 'green'
  if (statusText.includes('待')) return 'orange'
  if (statusText.includes('取消') || statusText.includes('作废')) return 'red'
  if (statusText.includes('草稿')) return 'default'
  return 'blue'
}

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求 ═══
function fetcher(params: Record<string, any>) {
  return docQueryApi.businessHistoryPage(params)
}
</script>
