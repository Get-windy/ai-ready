<template>
  <ErrorBoundary>
    <PageContainer title="营销推广分析">
      <!-- ═══ 统计卡片 ═══ -->
      <ARStatCards
        :items="statCards"
        :loading="summaryLoading"
      />

      <!-- ═══ 查询区 ═══ -->
      <div class="search-area">
        <a-form layout="inline">
          <a-form-item>
            <a-input
              v-model:value="query.keyword"
              placeholder="活动名称/编码"
              allow-clear
              style="width: 200px"
              @press-enter="handleSearch"
            />
          </a-form-item>
          <a-form-item label="状态">
            <a-select
              v-model:value="query.status"
              placeholder="全部状态"
              allow-clear
              :options="statusOptions"
              style="width: 150px"
            />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button
                type="primary"
                @click="handleSearch"
              >
                <template #icon>
                  <SearchOutlined />
                </template>查询
              </a-button>
              <a-button @click="handleReset">
                <template #icon>
                  <ClearOutlined />
                </template>重置
              </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- ═══ 图表区 ═══ -->
      <div class="chart-grid">
        <ARReportChart
          title="推广转化漏斗"
          :option="funnelOption"
          :loading="summaryLoading"
          :height="340"
        />
        <ARReportChart
          title="活动触达/转化对比 TOP10"
          :option="reachBarOption"
          :loading="summaryLoading"
          :height="340"
        />
      </div>

      <!-- ═══ 活动推广效果表 ═══ -->
      <div class="table-area">
        <a-table
          :columns="columns"
          :data-source="rows"
          :loading="loading"
          :pagination="tablePagination"
          row-key="id"
          :locale="{ emptyText: '暂无数据' }"
          size="small"
          @change="handleTableChange"
        >
          <template #bodyCell="{ column, text, record }">
            <template v-if="column.dataIndex === 'status'">
              <a-tag :color="CAMPAIGN_STATUS[record.status]?.color || 'default'">
                {{ CAMPAIGN_STATUS[record.status]?.text || record.statusDesc || record.status }}
              </a-tag>
            </template>
            <template v-else-if="column.dataIndex === 'campaignType'">
              {{ CAMPAIGN_TYPE[record.campaignType] || '-' }}
            </template>
            <template v-else-if="column.dataIndex === 'reachRate'">
              {{ formatRate(record.reachedCustomerCount, record.targetCustomerCount) }}
            </template>
            <template v-else-if="column.dataIndex === 'convertRate'">
              {{ formatRate(record.convertedCustomerCount, record.targetCustomerCount) }}
            </template>
            <template v-else-if="column.dataIndex === 'actualRevenue'">
              {{ formatMoney(text) }}
            </template>
          </template>
        </a-table>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { SearchOutlined, ClearOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import ARReportChart from '@/components/ARReportChart/ARReportChart.vue'
import type { StatCardItem } from '@/components/ARReportPage/types'
import { crmMarketingApi } from '@/api/analytics'
import type { MarketingCampaignItem } from '@/api/analytics'

// ═══ 活动状态/类型（与后端 CampaignStatus / CampaignType 枚举一致） ═══
const CAMPAIGN_STATUS: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'blue' },
  3: { text: '已排期', color: 'cyan' },
  4: { text: '进行中', color: 'processing' },
  5: { text: '已暂停', color: 'orange' },
  6: { text: '已完成', color: 'green' },
  7: { text: '已取消', color: 'red' }
}

const CAMPAIGN_TYPE: Record<number, string> = {
  1: '邮件营销', 2: '短信营销', 3: '微信营销', 4: '电话营销', 5: '活动营销',
  6: '线上推广', 7: '线下推广', 8: '内容营销', 9: '社交媒体', 10: '综合营销'
}

const statusOptions = Object.entries(CAMPAIGN_STATUS).map(([value, v]) => ({ label: v.text, value: Number(value) }))

// ═══ 汇总（targets/executions 为按活动钻取接口，无聚合端点；改用活动分页聚合） ═══
interface PromoteSummary {
  total: number
  running: number
  target: number
  reached: number
  responded: number
  converted: number
  leads: number
  orders: number
  revenue: number
  campaigns: MarketingCampaignItem[]
}

const summaryLoading = ref(false)
const summary = ref<PromoteSummary>({
  total: 0, running: 0, target: 0, reached: 0, responded: 0,
  converted: 0, leads: 0, orders: 0, revenue: 0, campaigns: []
})

function rateOf(part: number, whole: number): number {
  return whole > 0 ? (part / whole) * 100 : 0
}

const statCards = computed<StatCardItem[]>(() => {
  const s = summary.value
  return [
    { label: '活动总数', value: s.total, suffix: '个' },
    { label: '进行中活动', value: s.running, suffix: '个' },
    { label: '目标客户', value: s.target, suffix: '人' },
    { label: '触达率', value: rateOf(s.reached, s.target), precision: 1, suffix: '%' },
    { label: '响应率', value: rateOf(s.responded, s.reached), precision: 1, suffix: '%' },
    { label: '转化率', value: rateOf(s.converted, s.target), precision: 1, suffix: '%' }
  ]
})

// ═══ 图表 ═══
const funnelOption = computed(() => {
  const s = summary.value
  const data = [
    { name: '目标客户', value: s.target },
    { name: '已触达', value: s.reached },
    { name: '已响应', value: s.responded },
    { name: '已转化', value: s.converted }
  ].filter(d => d.value > 0)
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} 人' },
    series: [
      {
        name: '转化漏斗',
        type: 'funnel',
        left: '10%',
        width: '80%',
        sort: 'none',
        gap: 4,
        label: { formatter: '{b}: {c}' },
        data
      }
    ]
  }
})

const reachBarOption = computed(() => {
  const top = [...summary.value.campaigns]
    .sort((a, b) => (Number(b.targetCustomerCount) || 0) - (Number(a.targetCustomerCount) || 0))
    .slice(0, 10)
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['已触达', '已转化'], bottom: 0 },
    grid: { left: 50, right: 20, top: 30, bottom: 60 },
    xAxis: {
      type: 'category',
      data: top.map(c => c.campaignName || c.campaignCode),
      axisLabel: { rotate: 30, interval: 0, width: 80, overflow: 'truncate' }
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '已触达',
        type: 'bar',
        barMaxWidth: 18,
        itemStyle: { color: '#1890ff', borderRadius: [4, 4, 0, 0] },
        data: top.map(c => Number(c.reachedCustomerCount) || 0)
      },
      {
        name: '已转化',
        type: 'bar',
        barMaxWidth: 18,
        itemStyle: { color: '#52c41a', borderRadius: [4, 4, 0, 0] },
        data: top.map(c => Number(c.convertedCustomerCount) || 0)
      }
    ]
  }
})

// ═══ 活动列表（服务端分页） ═══
const loading = ref(false)
const query = reactive<{ keyword: string; status?: number }>({ keyword: '' })
const rows = ref<MarketingCampaignItem[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

const tablePagination = computed(() => ({
  current: pagination.current,
  pageSize: pagination.pageSize,
  total: pagination.total,
  showSizeChanger: true,
  showTotal: (t: number) => `共 ${t} 条`
}))

const columns: any[] = [
  { title: '活动编码', dataIndex: 'campaignCode', key: 'campaignCode', width: 120 },
  { title: '活动名称', dataIndex: 'campaignName', key: 'campaignName', width: 160, ellipsis: true },
  { title: '类型', dataIndex: 'campaignType', key: 'campaignType', width: 95 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 85 },
  { title: '目标客户', dataIndex: 'targetCustomerCount', key: 'targetCustomerCount', width: 90, align: 'right' },
  { title: '已触达', dataIndex: 'reachedCustomerCount', key: 'reachedCustomerCount', width: 85, align: 'right' },
  { title: '已响应', dataIndex: 'respondedCustomerCount', key: 'respondedCustomerCount', width: 85, align: 'right' },
  { title: '已转化', dataIndex: 'convertedCustomerCount', key: 'convertedCustomerCount', width: 85, align: 'right' },
  { title: '触达率', dataIndex: 'reachRate', key: 'reachRate', width: 85, align: 'right' },
  { title: '转化率', dataIndex: 'convertRate', key: 'convertRate', width: 85, align: 'right' },
  { title: '实际线索', dataIndex: 'actualLeads', key: 'actualLeads', width: 85, align: 'right' },
  { title: '实际订单', dataIndex: 'actualOrders', key: 'actualOrders', width: 85, align: 'right' },
  { title: '实际收入', dataIndex: 'actualRevenue', key: 'actualRevenue', width: 110, align: 'right' }
]

// ═══ 工具 ═══
function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatRate(part: number | null | undefined, whole: number | null | undefined): string {
  const w = Number(whole) || 0
  if (w <= 0) return '-'
  return `${(((Number(part) || 0) / w) * 100).toFixed(1)}%`
}

// ═══ 数据请求 ═══
async function loadSummary() {
  summaryLoading.value = true
  try {
    const res: any = await crmMarketingApi.page({ pageNum: 1, pageSize: 500 })
    const page = res?.records ? res : res?.data || { records: [], total: 0 }
    const list: MarketingCampaignItem[] = page.records || []
    summary.value = {
      total: Number(page.total) || list.length,
      running: list.filter(c => Number(c.status) === 4).length,
      target: list.reduce((acc, c) => acc + (Number(c.targetCustomerCount) || 0), 0),
      reached: list.reduce((acc, c) => acc + (Number(c.reachedCustomerCount) || 0), 0),
      responded: list.reduce((acc, c) => acc + (Number(c.respondedCustomerCount) || 0), 0),
      converted: list.reduce((acc, c) => acc + (Number(c.convertedCustomerCount) || 0), 0),
      leads: list.reduce((acc, c) => acc + (Number(c.actualLeads) || 0), 0),
      orders: list.reduce((acc, c) => acc + (Number(c.actualOrders) || 0), 0),
      revenue: list.reduce((acc, c) => acc + (Number(c.actualRevenue) || 0), 0),
      campaigns: list
    }
  } catch (e) {
    summary.value = {
      total: 0, running: 0, target: 0, reached: 0, responded: 0,
      converted: 0, leads: 0, orders: 0, revenue: 0, campaigns: []
    }
    console.warn('[营销推广分析] 汇总获取失败', e)
  } finally {
    summaryLoading.value = false
  }
}

async function loadTable() {
  loading.value = true
  try {
    const res: any = await crmMarketingApi.page({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      keyword: query.keyword || undefined,
      status: query.status
    })
    const page = res?.records ? res : res?.data || { records: [], total: 0 }
    rows.value = page.records || []
    pagination.total = Number(page.total) || 0
  } catch (e) {
    rows.value = []
    pagination.total = 0
    console.warn('[营销推广分析] 活动列表获取失败', e)
  } finally {
    loading.value = false
  }
}

function handleTableChange(pag: { current?: number; pageSize?: number }) {
  pagination.current = pag.current || 1
  pagination.pageSize = pag.pageSize || 20
  loadTable()
}

function handleSearch() {
  pagination.current = 1
  loadTable()
}

function handleReset() {
  query.keyword = ''
  query.status = undefined
  pagination.current = 1
  loadTable()
}

onMounted(() => {
  loadSummary()
  loadTable()
})
</script>

<style scoped>
.search-area {
  background: #fff;
  padding: 16px 20px 0;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(420px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.table-area {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
</style>
