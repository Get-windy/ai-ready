<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      title="销售漏斗"
      full-height
    >
      <div class="report-scroll">
        <!-- ═══ 口径说明（如实标注数据来源与已知边界） ═══ -->
        <a-alert
          type="info"
          show-icon
          class="page-alert"
        >
          <template #message>
            数据口径说明
          </template>
          <template #description>
            <div>
              数据源：<code>GET /api/crm/opportunity/export</code>（全量商机），本页由前端实时聚合，
              <b>不含分页</b>；时段条件按下方所选时间字段过滤，参与全部卡片 / 漏斗图 / 阶段明细。
            </div>
            <div>
              未使用 <code>/crm/opportunity/statistics</code> 的真实原因：该接口在「有商机但无已关闭商机」时
              分母为 0 抛异常返回 500（后端 winRate 守卫用 totalCount、分母却为 winCount + loseCount），
              <b>并非响应结构不可用</b>。
            </div>
            <div>
              「预计总金额」为未加权裸求和（未乘阶段赢率 probability），属乐观上限；
              阶段明细仅覆盖阶段 1–5，未填阶段的商机不计入任何段但仍计入商机总数。
            </div>
          </template>
        </a-alert>

        <!-- ═══ 查询区：时段筛选（快捷项 + 自定义区间 + 时段字段） ═══ -->
        <div class="filter-area">
          <a-space
            :size="8"
            wrap
          >
            <span class="filter-label">时段</span>
            <a-radio-group
              v-model:value="quickRange"
              size="small"
              button-style="solid"
            >
              <a-radio-button value="all">
                全部
              </a-radio-button>
              <a-radio-button value="month">
                本月
              </a-radio-button>
              <a-radio-button value="quarter">
                本季度
              </a-radio-button>
              <a-radio-button value="year">
                本年
              </a-radio-button>
              <a-radio-button value="custom">
                自定义
              </a-radio-button>
            </a-radio-group>
            <a-range-picker
              v-model:value="customRange"
              size="small"
              :disabled="quickRange !== 'custom'"
              :style="{ width: '240px' }"
            />
            <span class="filter-label">时段字段</span>
            <a-select
              v-model:value="dateField"
              size="small"
              :options="DATE_FIELD_OPTIONS"
              :style="{ width: '130px' }"
            />
            <a-button
              size="small"
              :loading="loading"
              @click="loadData"
            >
              <template #icon>
                <ReloadOutlined />
              </template>刷新
            </a-button>
          </a-space>
        </div>

        <!-- ═══ 失败提示（不只 console，页面可见 + 可重试） ═══ -->
        <a-alert
          v-if="errorMsg"
          type="error"
          show-icon
          class="page-alert"
          message="商机数据加载失败"
          :description="errorMsg"
        >
          <template #action>
            <a-button
              size="small"
              @click="loadData"
            >
              重试
            </a-button>
          </template>
        </a-alert>

        <!-- ═══ 空态提示：有商机但当前时段无命中 ═══ -->
        <a-alert
          v-else-if="!loading && opportunities.length > 0 && filteredOpportunities.length === 0"
          type="warning"
          show-icon
          class="page-alert"
          message="当前时段无商机数据"
          description="请调整时段条件，或选择「全部」查看完整数据。"
        />

        <!-- ═══ 统计卡片（基于时段过滤后的商机） ═══ -->
        <ARStatCards
          :items="statCards"
          :loading="loading"
        />

        <!-- ═══ 当前口径摘要 ═══ -->
        <div class="filter-summary">
          当前时段：{{ rangeText }}｜命中 <b>{{ filteredOpportunities.length }}</b> 个商机
          <template v-if="quickDateRange">
            （全量 {{ opportunities.length }} 个）
          </template>
          ｜口径：全量商机前端实时聚合，不含分页
        </div>

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
                  {{ formatPercent(record.percent) }}
                </template>
              </template>
            </a-table>
          </div>
        </div>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import ARReportChart from '@/components/ARReportChart/ARReportChart.vue'
import type { StatCardItem } from '@/components/ARReportPage/types'
import { opportunityStageApi, type OpportunityRecord } from '@/api/crm'

defineOptions({ name: 'CrmFunnel' })

// ═══ 商机阶段（与后端 CustomerOpportunityServiceImpl 一致） ═══
const STAGES = [
  { stage: 1, name: '初步接触', color: '#91caff' },
  { stage: 2, name: '需求确认', color: '#69b1ff' },
  { stage: 3, name: '方案报价', color: '#4096ff' },
  { stage: 4, name: '商务谈判', color: '#1677ff' },
  { stage: 5, name: '成交', color: '#0958d9' }
]

// ═══ 时段筛选 ═══
type QuickRange = 'all' | 'month' | 'quarter' | 'year' | 'custom'
type DateField = 'createdAt' | 'expectedCloseDate' | 'actualCloseDate'

const DATE_FIELD_OPTIONS = [
  { label: '创建时间', value: 'createdAt' },
  { label: '预计关闭日', value: 'expectedCloseDate' },
  { label: '实际关闭日', value: 'actualCloseDate' }
]

const quickRange = ref<QuickRange>('all')
const customRange = ref<[Dayjs, Dayjs] | null>(null)
const dateField = ref<DateField>('createdAt')

/** 快捷时段 → 具体区间；'all' 返回 null 表示不过滤 */
const quickDateRange = computed<[Dayjs, Dayjs] | null>(() => {
  const now = dayjs()
  if (quickRange.value === 'month') {
    return [now.startOf('month'), now.endOf('month')]
  }
  if (quickRange.value === 'quarter') {
    // 不引入 dayjs quarter 插件，按当前月推算季度起点
    const startMonth = Math.floor(now.month() / 3) * 3
    const start = now.month(startMonth).startOf('month')
    return [start, start.add(2, 'month').endOf('month')]
  }
  if (quickRange.value === 'year') {
    return [now.startOf('year'), now.endOf('year')]
  }
  if (quickRange.value === 'custom') {
    const r = customRange.value
    if (r && r[0] && r[1]) return [r[0], r[1]]
  }
  return null
})

/** 切到「自定义」时给一个默认区间，避免看起来像未生效 */
watch(quickRange, (val) => {
  if (val === 'custom' && !customRange.value) {
    const now = dayjs()
    customRange.value = [now.startOf('month'), now.endOf('month')]
  }
})

const rangeText = computed(() => {
  const r = quickDateRange.value
  if (!r) return '全部'
  return `${r[0].format('YYYY-MM-DD')} ~ ${r[1].format('YYYY-MM-DD')}`
})

const loading = ref(false)
const errorMsg = ref('')
const opportunities = ref<OpportunityRecord[]>([])

// ═══ 按所选时段字段过滤（真实参与全部聚合） ═══
const filteredOpportunities = computed<OpportunityRecord[]>(() => {
  const range = quickDateRange.value
  if (!range) return opportunities.value
  const field = dateField.value
  return opportunities.value.filter((o) => {
    const raw = o[field]
    if (!raw) return false
    const d = dayjs(raw)
    if (!d.isValid()) return false
    return !d.isBefore(range[0], 'day') && !d.isAfter(range[1], 'day')
  })
})

// ═══ 统计卡片（分母/分子均取自过滤后集合） ═══
const statCards = computed<StatCardItem[]>(() => {
  const list = filteredOpportunities.value
  const winList = list.filter(o => o.status === 2)
  const loseCount = list.filter(o => o.status === 3).length
  const totalEstimated = list.reduce((acc, o) => acc + (Number(o.estimatedAmount) || 0), 0)
  const totalActual = winList.reduce((acc, o) => acc + (Number(o.actualAmount) || 0), 0)
  const closedCount = winList.length + loseCount
  // 无已关闭商机时显示「-」，避免把「没有已关闭商机」误报成「赢单率 0%」
  const winRateCard: StatCardItem = closedCount
    ? {
        label: '赢单率',
        value: Math.round((winList.length / closedCount) * 1000) / 10,
        precision: 1,
        suffix: '%'
      }
    : { label: '赢单率', value: '-' }
  return [
    { label: '商机总数', value: list.length, suffix: '个' },
    { label: '预计总金额（未加权）', value: totalEstimated, precision: 2, prefix: '¥' },
    { label: '赢单金额', value: totalActual, precision: 2, prefix: '¥', valueStyle: { color: '#52c41a' } },
    winRateCard
  ]
})

// ═══ 漏斗图（echarts funnel） ═══
const funnelOption = computed(() => {
  const list = filteredOpportunities.value
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
  const list = filteredOpportunities.value
  const total = list.length
  return STAGES.map(s => {
    const rows = list.filter(o => o.opportunityStage === s.stage)
    return {
      stage: s.stage,
      stageName: s.name,
      color: s.color,
      count: rows.length,
      amount: rows.reduce((acc, o) => acc + (Number(o.estimatedAmount) || 0), 0),
      // total 为 0 时占比取 0，避免 NaN / Infinity 进入图表与表格
      percent: total ? (rows.length / total) * 100 : 0
    }
  })
})

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatPercent(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val)) || !isFinite(Number(val))) return '-'
  return `${Number(val).toFixed(1)}%`
}

// ═══ 数据加载（/crm/opportunity/export 裸 List 全量，前端聚合） ═══
async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const list = await opportunityStageApi.exportList()
    opportunities.value = Array.isArray(list) ? list : []
  } catch (e) {
    opportunities.value = []
    errorMsg.value = e instanceof Error && e.message
      ? `商机数据获取失败：${e.message}`
      : '商机数据获取失败，请检查网络后重试。'
    message.error('商机数据加载失败，请检查网络后重试')
    console.warn('[销售漏斗] 商机数据获取失败', e)
  } finally {
    loading.value = false
  }
}

/** ErrorBoundary 回调（兜底已由组件展示，这里只补充控制台定位信息） */
function handleError(err: Error) {
  console.error('[销售漏斗] 页面渲染异常', err)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.report-scroll {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 12px 16px 16px;
}

.page-alert {
  margin-bottom: 16px;
}

.filter-area {
  background: #fff;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.filter-label {
  color: #595959;
  font-size: 13px;
}

.filter-summary {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 12px;
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
