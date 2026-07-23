<template>
  <PageContainer title="销售漏斗">
    <a-alert
      type="info"
      show-icon
      class="page-alert"
      message="数据口径说明"
      description="后端商机统计接口（/crm/opportunity/statistics）返回裸 Map 暂不可直接用，本页由商机全量列表（/crm/opportunity/export）实时聚合，数据真实有效。"
    />

    <ARStatCards
      :items="statCards"
      :loading="loading"
    />

    <div class="funnel-layout">
      <ARReportChart
        title="商机阶段漏斗"
        :option="funnelOption"
        :loading="loading"
        :height="420"
        empty-text="暂无商机数据"
      />
      <div class="stage-table">
        <div class="stage-table__title">
          阶段明细
        </div>
        <a-table
          :columns="stageColumns"
          :data-source="stageRows"
          :pagination="false"
          :loading="loading"
          row-key="stage"
          size="small"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'stageName'">
              <a-tag :color="record.color">
                {{ record.stageName }}
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'amount'">
              {{ formatMoney(record.amount) }}
            </template>
            <template v-else-if="column.dataIndex === 'percent'">
              {{ record.percent.toFixed(1) }}%
            </template>
          </template>
        </a-table>
      </div>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import ARReportChart from '@/components/ARReportChart/ARReportChart.vue'
import type { StatCardItem } from '@/components/ARReportPage/types'
import { opportunityStageApi, type OpportunityRecord } from '@/api/crm'

// ═══ 商机阶段（与后端 CustomerOpportunityServiceImpl 一致） ═══
const STAGES = [
  { stage: 1, name: '初步接触', color: '#91caff' },
  { stage: 2, name: '需求确认', color: '#69b1ff' },
  { stage: 3, name: '方案报价', color: '#4096ff' },
  { stage: 4, name: '商务谈判', color: '#1677ff' },
  { stage: 5, name: '成交', color: '#0958d9' }
]

const loading = ref(false)
const opportunities = ref<OpportunityRecord[]>([])

// ═══ 统计卡片 ═══
const statCards = computed<StatCardItem[]>(() => {
  const list = opportunities.value
  const winList = list.filter(o => o.status === 2)
  const loseCount = list.filter(o => o.status === 3).length
  const totalEstimated = list.reduce((acc, o) => acc + (Number(o.estimatedAmount) || 0), 0)
  const totalActual = winList.reduce((acc, o) => acc + (Number(o.actualAmount) || 0), 0)
  const closedCount = winList.length + loseCount
  const winRate = closedCount ? Math.round((winList.length / closedCount) * 1000) / 10 : 0
  return [
    { label: '商机总数', value: list.length, suffix: '个' },
    { label: '预计总金额', value: totalEstimated, precision: 2, prefix: '¥' },
    { label: '赢单金额', value: totalActual, precision: 2, prefix: '¥', valueStyle: { color: '#52c41a' } },
    { label: '赢单率', value: winRate, precision: 1, suffix: '%' }
  ]
})

// ═══ 漏斗图（echarts funnel） ═══
const funnelOption = computed(() => {
  const list = opportunities.value
  const hasData = list.length > 0
  const data = hasData
    ? STAGES.map(s => ({
      name: s.name,
      value: list.filter(o => o.opportunityStage === s.stage).length,
      itemStyle: { color: s.color }
    }))
    : [] // 无数据时走组件空态
  return {
    tooltip: {
      trigger: 'item' as const,
      formatter: '{b}：{c} 个商机'
    },
    legend: { bottom: 0 },
    series: [
      {
        type: 'funnel',
        left: '10%',
        width: '80%',
        top: 12,
        bottom: 36,
        sort: 'none',
        gap: 4,
        label: {
          show: true,
          position: 'inside',
          formatter: '{b}\n{c} 个'
        },
        data
      }
    ]
  }
})

// ═══ 阶段明细表 ═══
const stageColumns: any[] = [
  { title: '阶段', dataIndex: 'stageName', key: 'stageName', width: 110 },
  { title: '商机数', dataIndex: 'count', key: 'count', width: 90, align: 'right' },
  { title: '预计金额', dataIndex: 'amount', key: 'amount', width: 130, align: 'right' },
  { title: '数量占比', dataIndex: 'percent', key: 'percent', width: 100, align: 'right' }
]

interface StageRow {
  stage: number
  stageName: string
  color: string
  count: number
  amount: number
  percent: number
}

const stageRows = computed<StageRow[]>(() => {
  const list = opportunities.value
  const total = list.length
  return STAGES.map(s => {
    const rows = list.filter(o => o.opportunityStage === s.stage)
    return {
      stage: s.stage,
      stageName: s.name,
      color: s.color,
      count: rows.length,
      amount: rows.reduce((acc, o) => acc + (Number(o.estimatedAmount) || 0), 0),
      percent: total ? (rows.length / total) * 100 : 0
    }
  })
})

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据加载（/crm/opportunity/export 裸 List 全量，前端聚合） ═══
async function loadData() {
  loading.value = true
  try {
    const list = await opportunityStageApi.exportList()
    opportunities.value = Array.isArray(list) ? list : []
  } catch (e) {
    opportunities.value = []
    console.warn('[销售漏斗] 商机数据获取失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page-alert {
  margin-bottom: 16px;
}

.funnel-layout {
  display: grid;
  grid-template-columns: minmax(0, 3fr) minmax(0, 2fr);
  gap: 16px;
}

@media (max-width: 1200px) {
  .funnel-layout {
    grid-template-columns: 1fr;
  }
}

.stage-table {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.stage-table__title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 12px;
}
</style>


