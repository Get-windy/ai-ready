<template>
  <PageContainer title="客户分析">
    <a-alert
      type="info"
      show-icon
      class="page-alert"
      message="数据口径说明"
      description="后端暂无客户统计专用接口，本页由客户全量列表（/customer/export）实时聚合：来源/等级分布与近12个月新增趋势均为真实数据。"
    />

    <ARStatCards
      :items="statCards"
      :loading="loading"
    />

    <div class="chart-grid">
      <ARReportChart
        title="客户来源分布"
        :option="sourceOption"
        :loading="loading"
        :height="320"
        empty-text="暂无客户数据"
      />
      <ARReportChart
        title="客户等级分布"
        :option="levelOption"
        :loading="loading"
        :height="320"
        empty-text="暂无客户数据"
      />
      <ARReportChart
        title="近12个月新增客户趋势"
        :option="trendOption"
        :loading="loading"
        :height="320"
        empty-text="暂无客户数据"
      />
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import ARReportChart from '@/components/ARReportChart/ARReportChart.vue'
import type { StatCardItem } from '@/components/ARReportPage/types'
import { crmCustomerApi, type CrmCustomer } from '@/api/crm'

// ═══ 等级/来源映射（后端为数值码，未提供枚举接口，按既有页面惯例映射） ═══
const LEVEL_TEXT: Record<number, string> = { 1: 'VIP客户', 2: '重要客户', 3: '普通客户', 4: '潜在客户' }
const LEVEL_COLORS = ['#faad14', '#fa8c16', '#1677ff', '#8c8c8c']
const SOURCE_TEXT: Record<number, string> = { 1: '电话营销', 2: '网络推广', 3: '客户介绍', 4: '展会活动', 5: '其他' }

const loading = ref(false)
const customers = ref<CrmCustomer[]>([])

// ═══ 统计卡片 ═══
const statCards = computed<StatCardItem[]>(() => {
  const list = customers.value
  const active = list.filter(c => c.status === 1).length
  const monthKey = dayjs().format('YYYY-MM')
  const newThisMonth = list.filter(c => c.createdAt && dayjs(c.createdAt).format('YYYY-MM') === monthKey).length
  const totalTrade = list.reduce((acc, c) => acc + (Number(c.tradeAmount) || 0), 0)
  return [
    { label: '客户总数', value: list.length, suffix: '家' },
    { label: '正常客户', value: active, suffix: '家', valueStyle: { color: '#52c41a' } },
    { label: '本月新增', value: newThisMonth, suffix: '家', valueStyle: { color: '#1677ff' } },
    { label: '累计交易额', value: totalTrade, precision: 2, prefix: '¥' }
  ]
})

// ═══ 来源分布（饼图） ═══
const sourceOption = computed(() => {
  const counter = new Map<string, number>()
  for (const c of customers.value) {
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
  const list = customers.value
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

// ═══ 新增趋势（近12个月折线） ═══
const trendOption = computed(() => {
  const months: string[] = []
  for (let i = 11; i >= 0; i--) {
    months.push(dayjs().subtract(i, 'month').format('YYYY-MM'))
  }
  const counter = new Map<string, number>(months.map(m => [m, 0]))
  for (const c of customers.value) {
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
  try {
    const list = await crmCustomerApi.exportList()
    customers.value = Array.isArray(list) ? list : []
  } catch (e) {
    customers.value = []
    console.warn('[客户分析] 客户数据获取失败', e)
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

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
  gap: 16px;
}
</style>
