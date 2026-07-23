<template>
  <ARReportPage
    title="查费用"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    :normalize-response="normalizeResponse"
    :stat-cards="statCards"
    export-file-name="查费用"
    row-key="expenseType"
  >
    <template #bodyCell="{ column, text, record }">
      <template v-if="column.dataIndex === 'amount'">
        {{ formatMoney(text) }}
      </template>
      <template v-else-if="column.dataIndex === 'ratio'">
        {{ record.ratio }}%
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField, ReportFetchResult, StatCardItem } from '@/components/ARReportPage/types'
import { expenseAnalyticsApi, type ExpenseStatisticsResult } from '@/api/analytics'

// ═══ 费用类型（与后端 ExpenseType 枚举一致） ═══
const EXPENSE_TYPE_OPTIONS = [
  { label: '差旅费', value: 'TRAVEL' },
  { label: '办公用品', value: 'OFFICE_SUPPLIES' },
  { label: '会议费', value: 'MEETING' },
  { label: '招待费', value: 'ENTERTAINMENT' },
  { label: '交通费', value: 'TRANSPORTATION' },
  { label: '通讯费', value: 'COMMUNICATION' },
  { label: '培训费', value: 'TRAINING' },
  { label: '咨询费', value: 'CONSULTING' },
  { label: '广告费', value: 'ADVERTISING' },
  { label: '研发费', value: 'R_D' },
  { label: '设备费', value: 'EQUIPMENT' },
  { label: '维修费', value: 'MAINTENANCE' },
  { label: '租赁费', value: 'RENTAL' },
  { label: '保险费', value: 'INSURANCE' },
  { label: '税费', value: 'TAX' },
  { label: '其他', value: 'OTHER' }
]

const queryFields: ReportQueryField[] = [
  {
    key: 'expenseType',
    type: 'select',
    label: '费用类型',
    placeholder: '全部类型',
    options: EXPENSE_TYPE_OPTIONS
  },
  { key: 'departmentId', type: 'input', label: '部门ID', placeholder: '部门ID', width: 140 },
  { key: 'applyDateRange', type: 'date-range', label: '申请日期' }
]

// ═══ 表格列（按费用类型构成） ═══
const columns: any[] = [
  { title: '费用类型', dataIndex: 'expenseType', key: 'expenseType', width: 200 },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 160, align: 'right' },
  { title: '占比', dataIndex: 'ratio', key: 'ratio', width: 120, align: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求 ═══
// 后端 statistics/page 为聚合口径（非单据分页）：总额/状态金额/按部门/按类型，
// page/size 仅回显。表格展示按类型的费用构成。
const rawStats = ref<ExpenseStatisticsResult | null>(null)

async function fetcher(params: Record<string, any>) {
  const res = await expenseAnalyticsApi.statisticsPage(params)
  rawStats.value = res
  return res
}

function normalizeResponse(res: ExpenseStatisticsResult): ReportFetchResult {
  const byType = res?.byType || {}
  const total = Number(res?.totalAmount) || 0
  const list = Object.entries(byType).map(([expenseType, amount]) => ({
    expenseType,
    amount: Number(amount) || 0,
    ratio: total > 0 ? (((Number(amount) || 0) / total) * 100).toFixed(1) : '0.0'
  }))
  list.sort((a, b) => b.amount - a.amount)
  return { list, total: list.length, raw: res }
}

// ═══ 汇总卡片 ═══
const statCards = computed<StatCardItem[]>(() => {
  const s = rawStats.value
  if (!s) return []
  return [
    { label: '费用总额', value: Number(s.totalAmount) || 0, precision: 2, prefix: '¥', suffix: `${Number(s.expenseCount) || 0} 单` },
    { label: '已审批金额', value: Number(s.approvedAmount) || 0, precision: 2, prefix: '¥' },
    { label: '待审批金额', value: Number(s.pendingAmount) || 0, precision: 2, prefix: '¥' },
    { label: '已拒绝金额', value: Number(s.rejectedAmount) || 0, precision: 2, prefix: '¥' }
  ]
})
</script>
