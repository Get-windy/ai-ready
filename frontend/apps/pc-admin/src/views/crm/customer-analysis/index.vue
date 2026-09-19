<template>
  <ErrorBoundary @error="handleError">
    <PageContainer
      title="客户分析"
      full-height
    >
      <div class="report-scroll">
        <!-- ═══ 口径说明（如实标注数据来源、字段与已知边界） ═══ -->
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
              数据源：<code>GET /api/customer/export</code>（全量客户），本页由前端实时聚合，
              <b>不含分页</b>；时段条件按<b>创建时间 createdAt</b> 过滤，参与 4 张卡片与全部图表。
            </div>
            <div>
              后端<b>无客户统计端点</b>（CustomerController 无 <code>/statistics</code>），故只能前端聚合。
            </div>
            <div>
              「正常客户」口径为 <code>status === 1</code>（与客户列表、客户表单一致）；
              「累计交易额」取自 <code>crm_customer.trade_amount</code>，该列当前<b>无系统写入路径</b>，真实环境可能恒为 0。
              来源为空的客户归入「未知」，等级为空（或不在 1–4）的客户不计入柱状图。
            </div>
          </template>
        </a-alert>

        <!-- ═══ 查询区：时段筛选（快捷项 + 自定义区间） ═══ -->
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
          message="客户数据加载失败"
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

        <!-- ═══ 空态提示：有客户但当前时段无命中 ═══ -->
        <a-alert
          v-else-if="!loading && customers.length > 0 && filteredCustomers.length === 0"
          type="warning"
          show-icon
          class="page-alert"
          message="当前时段无新增客户"
          description="请调整时段条件，或选择「全部」查看完整数据。"
        />

        <!-- ═══ 统计卡片（基于时段过滤后的客户） ═══ -->
        <ARStatCards
          :items="statCards"
          :loading="loading"
        />

        <!-- ═══ 当前口径摘要 ═══ -->
        <div class="filter-summary">
          当前时段：{{ rangeText }}｜命中 <b>{{ filteredCustomers.length }}</b> 家客户
          <template v-if="quickDateRange">
            （全量 {{ customers.length }} 家）
          </template>
          ｜口径：全量客户前端实时聚合，不含分页；时段按创建时间过滤
        </div>

        <div class="chart-grid">
          <ARReportChart
            title="客户来源分布"
            :option="sourceOption"
            :loading="loading"
            :height="320"
            empty-text="当前时段暂无客户数据"
          />
          <ARReportChart
            title="客户等级分布"
            :option="levelOption"
            :loading="loading"
            :height="320"
            empty-text="当前时段暂无客户数据"
          />
          <ARReportChart
            :title="trendTitle"
            :option="trendOption"
            :loading="loading"
            :height="320"
            empty-text="当前时段暂无新增客户"
          />
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
import { crmCustomerApi, type CrmCustomer } from '@/api/crm'

defineOptions({ name: 'CrmCustomerAnalysis' })

// ═══ 等级/来源映射（来源字典以 crm/customer/form.vue 的录入字典为准） ═══
const LEVEL_TEXT: Record<number, string> = { 1: 'VIP客户', 2: '重要客户', 3: '普通客户', 4: '潜在客户' }
const LEVEL_COLORS = ['#faad14', '#fa8c16', '#1677ff', '#8c8c8c']
const SOURCE_TEXT: Record<number, string> = { 1: '自主开发', 2: '转介绍', 3: '网络推广', 4: '展会', 5: '其他' }

// ═══ 时段筛选（客户只有 createdAt 一个创建时间维度） ═══
type QuickRange = 'all' | 'month' | 'quarter' | 'year' | 'custom'

const quickRange = ref<QuickRange>('all')
const customRange = ref<[Dayjs, Dayjs] | null>(null)

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
const customers = ref<CrmCustomer[]>([])

// ═══ 按创建时间过滤（真实参与全部卡片与图表） ═══
const filteredCustomers = computed<CrmCustomer[]>(() => {
  const range = quickDateRange.value
  if (!range) return customers.value
  return customers.value.filter((c) => {
    if (!c.createdAt) return false
    const d = dayjs(c.createdAt)
    if (!d.isValid()) return false
    return !d.isBefore(range[0], 'day') && !d.isAfter(range[1], 'day')
  })
})

// ═══ 统计卡片 ═══
const statCards = computed<StatCardItem[]>(() => {
  const list = filteredCustomers.value
  const active = list.filter(c => c.status === 1).length
  const totalTrade = list.reduce((acc, c) => acc + (Number(c.tradeAmount) || 0), 0)
  // 有时段条件时，过滤结果本身即「时段内新增」；无时段条件时按当月统计
  const newCount = quickDateRange.value
    ? list.length
    : customers.value.filter(c => c.createdAt && dayjs(c.createdAt).format('YYYY-MM') === dayjs().format('YYYY-MM')).length
  const newLabel = quickDateRange.value ? '时段内新增' : '本月新增'
  return [
    { label: '客户总数', value: list.length, suffix: '家' },
    { label: '正常客户', value: active, suffix: '家', valueStyle: { color: '#52c41a' } },
    { label: newLabel, value: newCount, suffix: '家', valueStyle: { color: '#1677ff' } },
    { label: '累计交易额', value: totalTrade, precision: 2, prefix: '¥' }
  ]
})

// ═══ 来源分布（饼图） ═══
const sourceOption = computed(() => {
  const counter = new Map<string, number>()
  for (const c of filteredCustomers.value) {
    const key = c.customerSource
      ? (SOURCE_TEXT[c.customerSource] || `来源${c.customerSource}`)
      : '未知'
    counter.set(key, (counter.get(key) || 0) + 1)
  }
  const data = [...counter.entries()].map(([name, value]) => ({ name, value }))
  return {
    tooltip: { trigger: 'item' as const, formatter: '{b}：{c} 家（{d}%）' },
    legend: { bottom: 0 },
    series: [
      {
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '45%'],
        label: { formatter: '{b} {c}' },
        data
      }
    ]
  }
})

// ═══ 等级分布（柱状图） ═══
const levelOption = computed(() => {
  const list = filteredCustomers.value
  const keys = [1, 2, 3, 4]
  const hasData = list.some(c => c.customerLevel)
  return {
    tooltip: { trigger: 'axis' as const },
    grid: { left: 40, right: 16, top: 24, bottom: 28 },
    xAxis: {
      type: 'category' as const,
      data: keys.map(k => LEVEL_TEXT[k])
    },
    yAxis: { type: 'value' as const, minInterval: 1 },
    series: [
      {
        type: 'bar',
        barWidth: 40,
        label: { show: true, position: 'top' },
        data: hasData
          ? keys.map((k, i) => ({
            value: list.filter(c => c.customerLevel === k).length,
            itemStyle: { color: LEVEL_COLORS[i] }
          }))
          : [] // 无等级数据时走组件空态
      }
    ]
  }
})

// ═══ 新增趋势：无时段条件看近 12 个月，有时段条件看所选区间（上限 36 个月） ═══
const trendMonths = computed<string[]>(() => {
  const range = quickDateRange.value
  const first = (range ? range[0] : dayjs().subtract(11, 'month')).startOf('month')
  const last = (range ? range[1] : dayjs()).startOf('month')
  const months: string[] = []
  let cur = first
  while ((cur.isBefore(last) || cur.isSame(last, 'month')) && months.length < 36) {
    months.push(cur.format('YYYY-MM'))
    cur = cur.add(1, 'month')
  }
  return months
})

const trendTitle = computed(() => (quickDateRange.value ? '时段内新增客户趋势' : '近12个月新增客户趋势'))

const trendOption = computed(() => {
  const months = trendMonths.value
  const counter = new Map<string, number>(months.map(m => [m, 0]))
  for (const c of filteredCustomers.value) {
    if (!c.createdAt) continue
    const m = dayjs(c.createdAt).format('YYYY-MM')
    if (counter.has(m)) counter.set(m, (counter.get(m) || 0) + 1)
  }
  const hasData = [...counter.values()].some(v => v > 0)
  return {
    tooltip: { trigger: 'axis' as const },
    grid: { left: 40, right: 16, top: 24, bottom: 28 },
    xAxis: {
      type: 'category' as const,
      data: months.map(m => dayjs(m).format('YY/MM'))
    },
    yAxis: { type: 'value' as const, minInterval: 1 },
    series: [
      {
        type: 'line',
        smooth: true,
        areaStyle: { opacity: 0.15 },
        itemStyle: { color: '#1677ff' },
        data: hasData ? months.map(m => counter.get(m) || 0) : [] // 无数据时走组件空态
      }
    ]
  }
})

// ═══ 数据加载（/customer/export 裸 List 全量，前端聚合） ═══
async function loadData() {
  loading.value = true
  errorMsg.value = ''
  try {
    const list = await crmCustomerApi.exportList()
    customers.value = Array.isArray(list) ? list : []
  } catch (e) {
    customers.value = []
    errorMsg.value = e instanceof Error && e.message
      ? `客户数据获取失败：${e.message}`
      : '客户数据获取失败，请检查网络后重试。'
    message.error('客户数据加载失败，请检查网络后重试')
    console.warn('[客户分析] 客户数据获取失败', e)
  } finally {
    loading.value = false
  }
}

/** ErrorBoundary 回调（兜底已由组件展示，这里只补充控制台定位信息） */
function handleError(err: Error) {
  console.error('[客户分析] 页面渲染异常', err)
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

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
  gap: 16px;
}
</style>
