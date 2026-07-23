<template>
  <ErrorBoundary>
    <PageContainer title="营销活动分析">
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
          <a-form-item label="类型">
            <a-select
              v-model:value="query.campaignType"
              placeholder="全部类型"
              allow-clear
              :options="typeOptions"
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
          title="活动状态分布"
          :option="statusPieOption"
          :loading="summaryLoading"
          :height="340"
        />
        <ARReportChart
          title="预算 vs 实际成本 vs 实际收入"
          :option="amountBarOption"
          :loading="summaryLoading"
          :height="340"
        />
      </div>

      <!-- ═══ 活动列表 ═══ -->
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
            <template v-else-if="['budget', 'actualCost', 'actualRevenue'].includes(column.dataIndex as string)">
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
const typeOptions = Object.entries(CAMPAIGN_TYPE).map(([value, label]) => ({ label, value: Number(value) }))

// ═══ 汇总（/crm/marketing/statistics 为裸 Map 响应被拦截器拒绝，改用分页接口聚合） ═══
const summaryLoading = ref(false)
const summary = ref<{ total: number; budget: number; cost: number; revenue: number; statusCount: Record<number, number> }>({
  total: 0, budget: 0, cost: 0, revenue: 0, statusCount: {}
})

const statCards = computed<StatCardItem[]>(() => {
  const s = summary.value
  const roi = s.cost > 0 ? ((s.revenue - s.cost) / s.cost) * 100 : 0
  return [
    { label: '活动总数', value: s.total, suffix: '个' },
    { label: '预算合计', value: s.budget, precision: 2, prefix: '¥' },
    { label: '实际成本', value: s.cost, precision: 2, prefix: '¥' },
    { label: '实际收入', value: s.revenue, precision: 2, prefix: '¥' },
    {
      label: '整体 ROI',
      value: roi,
      precision: 1,
      suffix: '%',
      valueStyle: { color: roi >= 0 ? '#52c41a' : '#f5222d' }
    }
  ]
})

// ═══ 图表 ═══
const statusPieOption = computed(() => {
  const data = Object.entries(summary.value.statusCount)
    .filter(([, count]) => count > 0)
    .map(([status, count]) => ({
      name: CAMPAIGN_STATUS[Number(status)]?.text || `状态${status}`,
      value: count
    }))
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} 个（{d}%）' },
    legend: { bottom: 0 },
    series: [
      {
        name: '活动状态',
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '45%'],
        label: { formatter: '{b}\n{c} 个' },
        data
      }
    ]
  }
})

const amountBarOption = computed(() => {
  const s = summary.value
  const hasData = s.budget > 0 || s.cost > 0 || s.revenue > 0
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 80, right: 30, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: ['预算合计', '实际成本', '实际收入'] },
    yAxis: { type: 'value', name: '金额(元)' },
    series: [
      {
        name: '金额',
        type: 'bar',
        barMaxWidth: 48,
        itemStyle: {
          color: (params: any) => ['#1890ff', '#fa8c16', '#52c41a'][params.dataIndex] || '#1890ff',
          borderRadius: [4, 4, 0, 0]
        },
        data: hasData ? [s.budget, s.cost, s.revenue] : []
      }
    ]
  }
})

// ═══ 活动列表（服务端分页） ═══
const loading = ref(false)
const query = reactive<{ keyword: string; status?: number; campaignType?: number }>({ keyword: '' })
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
  { title: '活动编码', dataIndex: 'campaignCode', key: 'campaignCode', width: 130 },
  { title: '活动名称', dataIndex: 'campaignName', key: 'campaignName', width: 170, ellipsis: true },
  { title: '类型', dataIndex: 'campaignType', key: 'campaignType', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '开始日期', dataIndex: 'startDate', key: 'startDate', width: 105 },
  { title: '结束日期', dataIndex: 'endDate', key: 'endDate', width: 105 },
  { title: '预算', dataIndex: 'budget', key: 'budget', width: 110, align: 'right' },
  { title: '实际成本', dataIndex: 'actualCost', key: 'actualCost', width: 110, align: 'right' },
  { title: '实际收入', dataIndex: 'actualRevenue', key: 'actualRevenue', width: 110, align: 'right' },
  { title: '负责人', dataIndex: 'ownerName', key: 'ownerName', width: 90 }
]

// ═══ 工具 ═══
function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求 ═══
async function loadSummary() {
  summaryLoading.value = true
  try {
    const res: any = await crmMarketingApi.page({ pageNum: 1, pageSize: 500 })
    const page = res?.records ? res : res?.data || { records: [], total: 0 }
    const list: MarketingCampaignItem[] = page.records || []
    const statusCount: Record<number, number> = {}
    for (const c of list) {
      const st = Number(c.status)
      statusCount[st] = (statusCount[st] || 0) + 1
    }
    summary.value = {
      total: Number(page.total) || list.length,
      budget: list.reduce((acc, c) => acc + (Number(c.budget) || 0), 0),
      cost: list.reduce((acc, c) => acc + (Number(c.actualCost) || 0), 0),
      revenue: list.reduce((acc, c) => acc + (Number(c.actualRevenue) || 0), 0),
      statusCount
    }
  } catch (e) {
    summary.value = { total: 0, budget: 0, cost: 0, revenue: 0, statusCount: {} }
    console.warn('[营销活动分析] 汇总获取失败', e)
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
      status: query.status,
      campaignType: query.campaignType
    })
    const page = res?.records ? res : res?.data || { records: [], total: 0 }
    rows.value = page.records || []
    pagination.total = Number(page.total) || 0
  } catch (e) {
    rows.value = []
    pagination.total = 0
    console.warn('[营销活动分析] 活动列表获取失败', e)
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
  query.campaignType = undefined
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
