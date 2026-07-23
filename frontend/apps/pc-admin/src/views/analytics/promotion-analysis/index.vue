<template>
  <ARReportPage
    title="推广分析"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    :normalize-response="normalizeResponse"
    :stat-cards="statCards"
    export-file-name="推广分析"
    row-key="id"
    empty-text="该区间暂无促销活动"
    @loaded="onLoaded"
  >
    <template #header-extra>
      <div
        v-if="monthlyDistribution.length"
        class="monthly-distribution"
      >
        <span class="monthly-distribution__label">月度分布：</span>
        <a-tag
          v-for="item in monthlyDistribution"
          :key="item.month"
          color="blue"
        >
          {{ item.month }} · {{ item.count }} 场
        </a-tag>
      </div>
    </template>
    <template #bodyCell="{ column, text }">
      <template v-if="column.dataIndex === 'status'">
        <a-tag :color="STATUS_MAP[text]?.color">
          {{ STATUS_MAP[text]?.label || text }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'type'">
        {{ TYPE_MAP[text]?.label || text }}
      </template>
      <template v-else-if="column.dataIndex === 'discountRate'">
        {{ text !== null && text !== undefined ? `${text} 折` : '-' }}
      </template>
      <template v-else-if="column.dataIndex === 'reductionAmount'">
        {{ formatMoney(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField, ReportFetchResult, StatCardItem } from '@/components/ARReportPage/types'
import { saleAnalyticsApi, type PromotionAnalysis } from '@/api/analytics'

// ═══ 活动状态/类型（与后端 PromotionActivity 注释一致） ═══
const STATUS_MAP: Record<string, { label: string; color: string }> = {
  draft: { label: '草稿', color: 'default' },
  published: { label: '已发布', color: 'green' },
  expired: { label: '已过期', color: 'blue' },
  cancelled: { label: '已取消', color: 'red' }
}

const TYPE_MAP: Record<string, { label: string }> = {
  discount: { label: '折扣' },
  full_reduction: { label: '满减' },
  gift: { label: '赠品' },
  combo: { label: '组合' }
}

const queryFields: ReportQueryField[] = [
  { key: 'activityDateRange', type: 'date-range', label: '活动日期' }
]

// ═══ 表格列（区间内促销活动列表） ═══
const columns: any[] = [
  { title: '活动名称', dataIndex: 'name', key: 'name', width: 180, ellipsis: true },
  { title: '类型', dataIndex: 'type', key: 'type', width: 90 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 160 },
  { title: '结束时间', dataIndex: 'endTime', key: 'endTime', width: 160 },
  { title: '折扣率', dataIndex: 'discountRate', key: 'discountRate', width: 90, align: 'right' },
  { title: '满减金额', dataIndex: 'reductionAmount', key: 'reductionAmount', width: 110, align: 'right' },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 卡片区：按状态/类型计数 + 订单优惠概况 ═══
const analysis = ref<PromotionAnalysis | null>(null)

const statCards = computed<StatCardItem[]>(() => {
  const a = analysis.value
  const cards: StatCardItem[] = []
  for (const item of a?.countByStatus || []) {
    const meta = STATUS_MAP[String(item.status)]
    cards.push({
      label: `活动·${meta?.label || item.status}`,
      value: Number(item.count) || 0,
      suffix: '个'
    })
  }
  for (const item of a?.countByType || []) {
    const meta = TYPE_MAP[String(item.type)]
    cards.push({
      label: `类型·${meta?.label || item.type}`,
      value: Number(item.count) || 0,
      suffix: '个'
    })
  }
  const d = a?.discountOverview
  cards.push(
    { label: '区间订单数', value: Number(d?.orderCount) || 0, suffix: '单' },
    { label: '区间订单总额', value: Number(d?.totalOrderAmount) || 0, precision: 2, prefix: '¥' },
    { label: '优惠订单数', value: Number(d?.discountedOrderCount) || 0, suffix: '单' },
    { label: '优惠总额', value: Number(d?.totalDiscountAmount) || 0, precision: 2, prefix: '¥' }
  )
  return cards
})

function onLoaded(result: ReportFetchResult) {
  analysis.value = (result.raw as PromotionAnalysis) || null
}

/** 按月活动分布（[{month, count}]） */
const monthlyDistribution = computed<{ month: string; count: number }[]>(() => {
  return (analysis.value?.monthlyDistribution || []) as { month: string; count: number }[]
})

// ═══ 数据请求（聚合端点，表格展示区间内活动列表） ═══
function fetcher(params: Record<string, any>) {
  return saleAnalyticsApi.promotionAnalysis({
    startDate: params.startDate,
    endDate: params.endDate
  })
}

function normalizeResponse(res: PromotionAnalysis): ReportFetchResult {
  const activities = res?.activities || []
  return { list: activities, total: activities.length, raw: res }
}
</script>

<style scoped>
.monthly-distribution {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
}

.monthly-distribution__label {
  font-size: 13px;
  color: #888;
}
</style>
