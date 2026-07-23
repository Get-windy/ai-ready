<template>
  <ARReportPage
    title="利润表"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    :stat-cards="statCards"
    export-file-name="利润表"
    row-key="itemCode"
    @loaded="handleLoaded"
  >
    <template #bodyCell="{ column, record, text }">
      <template v-if="column.dataIndex === 'itemName'">
        <span :style="{ fontWeight: isTotalRow(record) ? 600 : 400 }">
          {{ text }}
        </span>
      </template>
      <template v-else-if="['currentAmount', 'cumulativeAmount'].includes(column.dataIndex as string)">
        <span :style="{ fontWeight: isTotalRow(record) ? 600 : 400 }">
          {{ formatMoney(text) }}
        </span>
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField, StatCardItem, ReportFetchResult } from '@/components/ARReportPage/types'
import { reportApi } from '@/api/finance'

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
  { key: 'fiscalPeriod', type: 'select', label: '会计期间', placeholder: `默认第${currentPeriod}期`, options: PERIOD_OPTIONS, width: 140 }
]

// ═══ 合计行（与后端 IncomeStatementDTO 合计 itemCode 一致） ═══
const TOTAL_ROWS: Array<{ code: string; name: string }> = [
  { code: 'TOTAL_REVENUE', name: '营业收入合计' },
  { code: 'TOTAL_COST', name: '营业成本合计' },
  { code: 'GROSS_PROFIT', name: '营业毛利' },
  { code: 'TOTAL_EXPENSE', name: '期间费用合计' },
  { code: 'NET_PROFIT', name: '净利润' }
]

function isTotalRow(record: any): boolean {
  return TOTAL_ROWS.some(t => t.code === record.itemCode)
}

// ═══ 表格列（与后端 IncomeStatementDTO 字段一致） ═══
const columns: any[] = [
  { title: '项目编码', dataIndex: 'itemCode', key: 'itemCode', width: 140 },
  { title: '项目名称', dataIndex: 'itemName', key: 'itemName' },
  { title: '本期金额', dataIndex: 'currentAmount', key: 'currentAmount', width: 180, align: 'right' },
  { title: '本年累计金额', dataIndex: 'cumulativeAmount', key: 'cumulativeAmount', width: 180, align: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 汇总卡片 ═══
const loadedList = ref<any[]>([])
const statCards = computed<StatCardItem[]>(() => {
  if (!loadedList.value.length) return []
  const totalValue = (code: string) => {
    const row = loadedList.value.find(r => r.itemCode === code)
    return Number(row?.currentAmount) || 0
  }
  const netProfit = totalValue('NET_PROFIT')
  return [
    { label: '营业收入合计', value: totalValue('TOTAL_REVENUE'), precision: 2, prefix: '¥' },
    { label: '营业成本合计', value: totalValue('TOTAL_COST'), precision: 2, prefix: '¥' },
    { label: '营业毛利', value: totalValue('GROSS_PROFIT'), precision: 2, prefix: '¥' },
    { label: '期间费用合计', value: totalValue('TOTAL_EXPENSE'), precision: 2, prefix: '¥' },
    {
      label: '净利润',
      value: netProfit,
      precision: 2,
      prefix: '¥',
      valueStyle: { color: netProfit >= 0 ? '#52c41a' : '#f5222d' }
    }
  ]
})

function handleLoaded(result: ReportFetchResult) {
  loadedList.value = result.list
}

// ═══ 数据请求：利润表 ═══
async function fetcher(params: Record<string, any>) {
  const fiscalYear = params.fiscalYear ?? currentYear
  const fiscalPeriod = params.fiscalPeriod ?? currentPeriod
  const res: any = await reportApi.getIncomeStatement({ fiscalYear, startMonth: 1, endMonth: fiscalPeriod })
  let list: any[] = Array.isArray(res) ? res : []
  return { records: list, total: list.length }
}
</script>
