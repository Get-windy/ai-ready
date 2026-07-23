<template>
  <div class="sales-expense">
    <div class="tabs-bar">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane
          key="department"
          tab="按部门分布"
        />
        <a-tab-pane
          key="type"
          tab="按类型分布"
        />
      </a-tabs>
    </div>
    <ARReportPage
      :key="activeTab"
      title="销售费用分析"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      :normalize-response="normalizeResponse"
      :stat-cards="statCards"
      export-file-name="销售费用分析"
      row-key="name"
      empty-text="该区间暂无费用数据"
      @loaded="onLoaded"
    >
      <template #bodyCell="{ column, text }">
        <template v-if="column.dataIndex === 'amount'">
          {{ formatMoney(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'percent'">
          <a-progress
            :percent="Number(text) || 0"
            size="small"
            :stroke-width="6"
          />
        </template>
      </template>
    </ARReportPage>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField, ReportFetchResult, StatCardItem } from '@/components/ARReportPage/types'
import { expenseStatisticsApi, type FeeDistributionResult } from '@/api/analytics'

const activeTab = ref<'department' | 'type'>('department')

const queryFields: ReportQueryField[] = [
  { key: 'applyDateRange', type: 'date-range', label: '申请日期' }
]

// ═══ 表格列（分布维度 + 金额 + 占比） ═══
const columns = computed<any[]>(() => [
  {
    title: activeTab.value === 'department' ? '部门' : '费用类型',
    dataIndex: 'name',
    key: 'name',
    width: 200,
    ellipsis: true
  },
  { title: '费用金额', dataIndex: 'amount', key: 'amount', width: 160, align: 'right' },
  { title: '占比', dataIndex: 'percent', key: 'percent', width: 220 }
])

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 汇总卡片（来自同一响应） ═══
const distResult = ref<FeeDistributionResult | null>(null)
const distCount = ref(0)
const topRow = ref<{ name: string; amount: number } | null>(null)

const statCards = computed<StatCardItem[]>(() => {
  const r = distResult.value
  const total = Number(r?.totalAmount) || 0
  const count = Number(r?.expenseCount) || 0
  const cards: StatCardItem[] = [
    { label: '费用总额', value: total, precision: 2, prefix: '¥' },
    { label: '费用笔数', value: count, suffix: '笔' },
    { label: '平均单笔', value: count > 0 ? total / count : 0, precision: 2, prefix: '¥' },
    { label: activeTab.value === 'department' ? '涉及部门数' : '涉及类型数', value: distCount.value, suffix: '个' }
  ]
  if (topRow.value) {
    cards.push({
      label: `占比最高·${topRow.value.name}`,
      value: topRow.value.amount,
      precision: 2,
      prefix: '¥',
      valueStyle: { color: '#fa8c16' }
    })
  }
  return cards
})

function onLoaded(result: ReportFetchResult) {
  distResult.value = (result.raw as FeeDistributionResult) || null
  distCount.value = result.total
  topRow.value = (result.list[0] as { name: string; amount: number } | undefined) || null
}

// ═══ 数据请求（按当前 tab 调对应分布接口） ═══
function fetcher(params: Record<string, any>) {
  const range = { startDate: params.startDate, endDate: params.endDate }
  return activeTab.value === 'department'
    ? expenseStatisticsApi.byDepartment(range)
    : expenseStatisticsApi.byType(range)
}

interface DistributionRow {
  name: string
  amount: number
  percent: number
}

function normalizeResponse(res: FeeDistributionResult): ReportFetchResult<DistributionRow> {
  const source = activeTab.value === 'department' ? res?.byDepartment : res?.byType
  const total = Number(res?.totalAmount) || 0
  const list: DistributionRow[] = Object.entries(source || {})
    .map(([name, amount]) => ({
      name,
      amount: Number(amount) || 0,
      percent: total > 0 ? Math.round(((Number(amount) || 0) / total) * 1000) / 10 : 0
    }))
    .sort((a, b) => b.amount - a.amount)
  return { list, total: list.length, raw: res }
}
</script>

<style scoped>
.tabs-bar {
  background: #fff;
  padding: 0 20px;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.tabs-bar :deep(.ant-tabs-nav) {
  margin-bottom: 0;
}
</style>
