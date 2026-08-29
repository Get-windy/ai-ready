<template>
  <ARReportPage
    title="总账"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    export-file-name="总账"
    :row-key="(record: any) => record.subjectCode"
  />
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
  { key: 'fiscalYear', type: 'select', label: '会计年度', placeholder: `默认${currentYear}年`, options: YEAR_OPTIONS, width: 140 },
  { key: 'fiscalPeriod', type: 'select', label: '会计期间', placeholder: `默认第${currentPeriod}期`, options: PERIOD_OPTIONS, width: 140 },
  { key: 'subjectCode', type: 'input', label: '科目代码', placeholder: '按科目代码过滤', width: 160 }
]

// ═══ 表格列（与后端 LedgerEntryDTO 字段一致） ═══
const moneyRender = ({ text }: { text: number }) => formatMoney(text)
const columns: any[] = [
  { title: '科目代码', dataIndex: 'subjectCode', key: 'subjectCode', width: 110 },
  { title: '科目名称', dataIndex: 'subjectName', key: 'subjectName', width: 200, ellipsis: true },
  { title: '会计期间', dataIndex: 'fiscalPeriod', key: 'fiscalPeriod', width: 90, align: 'center' },
  { title: '期初借方', dataIndex: 'openingDebit', key: 'openingDebit', width: 130, align: 'right', customRender: moneyRender },
  { title: '期初贷方', dataIndex: 'openingCredit', key: 'openingCredit', width: 130, align: 'right', customRender: moneyRender },
  { title: '本期借方', dataIndex: 'periodDebit', key: 'periodDebit', width: 130, align: 'right', customRender: moneyRender },
  { title: '本期贷方', dataIndex: 'periodCredit', key: 'periodCredit', width: 130, align: 'right', customRender: moneyRender },
  { title: '期末借方', dataIndex: 'closingDebit', key: 'closingDebit', width: 130, align: 'right', customRender: moneyRender },
  { title: '期末贷方', dataIndex: 'closingCredit', key: 'closingCredit', width: 130, align: 'right', customRender: moneyRender }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求：总账（/erp/finance/ledger/general，按科目汇总） ═══
async function fetcher(params: Record<string, any>) {
  const res: any = await ledgerApi.getGeneral({
    fiscalYear: params.fiscalYear ?? currentYear,
    fiscalPeriod: params.fiscalPeriod ?? currentPeriod,
    subjectCode: params.subjectCode || undefined
  })
  return res?.data || res || []
}
</script>
