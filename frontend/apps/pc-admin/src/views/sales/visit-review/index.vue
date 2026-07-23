<template>
  <div>
    <ARReportPage
      title="拜访检视"
      :stat-cards="statCards"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      :normalize-response="normalizeResponse"
      export-file-name="拜访检视"
      row-key="id"
      empty-text="暂无拜访检视数据"
      @loaded="handleLoaded"
    >
      <template #bodyCell="{ column, text }">
        <template v-if="column.dataIndex === 'visitType'">
          <a-tag :color="VISIT_TYPE_MAP[text]?.color || 'default'">
            {{ VISIT_TYPE_MAP[text]?.label || '-' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'result'">
          <a-tag :color="RESULT_MAP[text]?.color || 'default'">
            {{ RESULT_MAP[text]?.label || '-' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'visitTime'">
          {{ formatTime(text) }}
        </template>
      </template>
    </ARReportPage>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportFetchResult, ReportQueryField, StatCardItem } from '@/components/ARReportPage/types'
import {
  visitReviewApi,
  type VisitReviewResponse, type VisitReviewSummary, type VisitStatsSummary
} from '@/api/crm'

// ═══ 拜访方式/结果（与后端 VisitRecord 注释一致） ═══
const VISIT_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '上门', color: 'blue' },
  2: { label: '电话', color: 'cyan' },
  3: { label: '其他', color: 'default' }
}
const RESULT_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '有意向', color: 'green' },
  2: { label: '一般', color: 'orange' },
  3: { label: '无意向', color: 'red' }
}
const resultOptions = Object.entries(RESULT_MAP).map(([value, v]) => ({
  label: v.label,
  value: Number(value)
}))

function formatTime(val: string | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD HH:mm') : '-'
}

// ═══ 统计卡片（/api/crm/visit/stats/summary + review/page 结果分布） ═══
const stats = ref<VisitStatsSummary | null>(null)
const reviewSummary = ref<VisitReviewSummary | null>(null)

const statCards = computed<StatCardItem[]>(() => [
  { label: '今日拜访', value: stats.value?.todayCount ?? 0, suffix: '次' },
  { label: '本周拜访', value: stats.value?.weekCount ?? 0, suffix: '次' },
  { label: '计划覆盖率', value: Number(stats.value?.coverage ?? 0), precision: 2, suffix: '%' },
  { label: '拜访总数', value: reviewSummary.value?.total ?? 0, suffix: '次' },
  { label: '有意向', value: reviewSummary.value?.interested ?? 0, suffix: '次', valueStyle: { color: '#52c41a' } },
  { label: '一般', value: reviewSummary.value?.normal ?? 0, suffix: '次', valueStyle: { color: '#faad14' } },
  { label: '无意向', value: reviewSummary.value?.noIntention ?? 0, suffix: '次', valueStyle: { color: '#ff4d4f' } }
])

// ═══ 查询字段（按结果 + 拜访日期筛选） ═══
const queryFields: ReportQueryField[] = [
  {
    key: 'result',
    type: 'select',
    label: '拜访结果',
    placeholder: '全部结果',
    width: 140,
    options: resultOptions
  },
  {
    key: 'visitDateRange',
    type: 'date-range',
    label: '拜访日期',
    startKey: 'visitDateStart',
    endKey: 'visitDateEnd',
    width: 240
  }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 170, ellipsis: true },
  { title: '负责人', dataIndex: 'salesPersonName', key: 'salesPersonName', width: 100 },
  { title: '拜访方式', dataIndex: 'visitType', key: 'visitType', width: 90 },
  { title: '打卡时间', dataIndex: 'visitTime', key: 'visitTime', width: 130 },
  { title: '拜访地点', dataIndex: 'location', key: 'location', ellipsis: true },
  { title: '拜访内容', dataIndex: 'content', key: 'content', ellipsis: true },
  { title: '拜访结果', dataIndex: 'result', key: 'result', width: 90 },
  { title: '下一步行动', dataIndex: 'nextAction', key: 'nextAction', width: 150, ellipsis: true },
  { title: '下次拜访', dataIndex: 'nextVisitDate', key: 'nextVisitDate', width: 110 }
]

// ═══ 数据请求（GET /api/crm/visit/review/page，裸 Map：summary+byDate+page） ═══
function fetcher(params: Record<string, any>) {
  return visitReviewApi.reviewPage(params)
}

function normalizeResponse(res: VisitReviewResponse): ReportFetchResult {
  const page = res?.page
  return {
    list: page?.records || [],
    total: Number(page?.total) || 0,
    raw: res
  }
}

/** 列表加载完成后同步结果分布，并刷新顶部统计 */
function handleLoaded(result: ReportFetchResult) {
  reviewSummary.value = (result.raw as VisitReviewResponse)?.summary || null
  loadStats()
}

async function loadStats() {
  try {
    stats.value = await visitReviewApi.statsSummary()
  } catch (e) {
    console.warn('[拜访检视] 统计汇总获取失败', e)
  }
}

onMounted(loadStats)
</script>
