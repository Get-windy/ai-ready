<template>
  <ARReportPage
    title="查资金"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="查资金"
    row-key="id"
  >
    <template #bodyCell="{ column, text }">
      <template v-if="column.dataIndex === 'direction'">
        <a-tag :color="text === 'IN' ? 'green' : 'red'">
          {{ text === 'IN' ? '收入' : '支出' }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'flowType'">
        {{ FLOW_TYPE_MAP[text] || text || '-' }}
      </template>
      <template v-else-if="['amount', 'balance'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { capitalFlowApi } from '@/api/analytics'

// ═══ 流水类型（与后端 CapitalFlowServiceImpl setFlowType 一致） ═══
const FLOW_TYPE_MAP: Record<string, string> = {
  RECEIPT: '收款',
  PAYMENT: '付款',
  PRE_RECEIPT: '预收',
  PRE_PAYMENT: '预付',
  OFFSET: '核销',
  PRE_RECEIPT_REFUND: '预收退还',
  PRE_RECEIPT_FORFEIT: '预收没收',
  PRE_PAYMENT_REFUND: '预付退还',
  PRE_PAYMENT_RECOVER: '预付收回'
}

const queryFields: ReportQueryField[] = [
  {
    key: 'flowType',
    type: 'select',
    label: '流水类型',
    placeholder: '全部类型',
    options: Object.entries(FLOW_TYPE_MAP).map(([value, label]) => ({ label, value }))
  },
  {
    key: 'direction',
    type: 'select',
    label: '方向',
    placeholder: '全部方向',
    options: [
      { label: '收入', value: 'IN' },
      { label: '支出', value: 'OUT' }
    ]
  },
  { key: 'occurDateRange', type: 'date-range', label: '发生日期' }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '流水号', dataIndex: 'flowNo', key: 'flowNo', width: 170 },
  { title: '类型', dataIndex: 'flowType', key: 'flowType', width: 100 },
  { title: '方向', dataIndex: 'direction', key: 'direction', width: 80 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 120, align: 'right' },
  { title: '余额', dataIndex: 'balance', key: 'balance', width: 120, align: 'right' },
  { title: '往来单位', dataIndex: 'partyName', key: 'partyName', width: 160, ellipsis: true },
  { title: '来源单号', dataIndex: 'refNo', key: 'refNo', width: 160 },
  { title: '发生时间', dataIndex: 'occurDate', key: 'occurDate', width: 160 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求 ═══
function fetcher(params: Record<string, any>) {
  return capitalFlowApi.getPage(params)
}
</script>
