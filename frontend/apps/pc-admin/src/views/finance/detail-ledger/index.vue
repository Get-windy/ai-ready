<template>
  <ARReportPage
    title="明细账"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    export-file-name="明细账"
    row-key="id"
  >
    <template #bodyCell="{ column, text }">
      <template v-if="['debitAmount', 'creditAmount'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { ledgerApi } from '@/api/finance'

// ═══ 会计期间选项 ═══
const currentYear = new Date().getFullYear()
const currentPeriod = new Date().getMonth() + 1
const YEAR_OPTIONS = [0, 1, 2, 3].map(i => {
  const y = currentYear - i
  return { label: `${y}年`, value: y }
})
const PERIOD_OPTIONS = Array.from({ length: 12 }, (_, i) => ({ label: `第${i + 1}期`, value: i + 1 }))

const queryFields: ReportQueryField[] = [
  { key: 'fiscalYear', type: 'select', label: '会计年度', placeholder: '全部年度', options: YEAR_OPTIONS, width: 140 },
  { key: 'fiscalPeriod', type: 'select', label: '会计期间', placeholder: '全部期间', options: PERIOD_OPTIONS, width: 140 },
  { key: 'subjectCode', type: 'input', label: '科目代码', placeholder: '按科目代码过滤', width: 160 }
]

// ═══ 表格列（与后端 LedgerDetailDTO 字段一致） ═══
const columns: any[] = [
  { title: '凭证号', dataIndex: 'voucherNo', key: 'voucherNo', width: 130 },
  { title: '凭证日期', dataIndex: 'voucherDate', key: 'voucherDate', width: 110 },
  { title: '科目代码', dataIndex: 'subjectCode', key: 'subjectCode', width: 110 },
  { title: '科目名称', dataIndex: 'subjectName', key: 'subjectName', width: 200, ellipsis: true },
  { title: '摘要', dataIndex: 'summary', key: 'summary', ellipsis: true },
  { title: '借方金额', dataIndex: 'debitAmount', key: 'debitAmount', width: 130, align: 'right' },
  { title: '贷方金额', dataIndex: 'creditAmount', key: 'creditAmount', width: 130, align: 'right' },
  { title: '来源单号', dataIndex: 'sourceNo', key: 'sourceNo', width: 130 }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求：明细账逐笔（/erp/finance/ledger/detail） ═══
async function fetcher(params: Record<string, any>) {
  const res: any = await ledgerApi.getDetail(params)
  return res?.data || res || []
}
</script>
