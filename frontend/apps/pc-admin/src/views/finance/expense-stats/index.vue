<template>
  <ErrorBoundary>
    <PageContainer title="费用统计">
      <!-- ═══ 查询区 ═══ -->
      <div class="search-area">
        <a-form layout="inline">
          <a-form-item label="申请日期">
            <a-range-picker
              v-model:value="dateRange"
              style="width: 240px"
            />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button
                type="primary"
                @click="loadData"
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

      <!-- ═══ 统计卡片 ═══ -->
      <ARStatCards
        :items="statCards"
        :loading="loading"
      />

      <!-- ═══ 图表区 ═══ -->
      <a-row
        :gutter="16"
        class="chart-row"
      >
        <a-col :span="12">
          <div class="chart-area">
            <ARReportChart
              title="按费用类型"
              :option="typeChartOption"
              :loading="loading"
              :height="320"
            />
          </div>
        </a-col>
        <a-col :span="12">
          <div class="chart-area">
            <ARReportChart
              title="按部门"
              :option="deptChartOption"
              :loading="loading"
              :height="320"
            />
          </div>
        </a-col>
      </a-row>

      <!-- ═══ 部门费用明细表 ═══ -->
      <div class="table-area">
        <div class="table-title">
          部门费用明细
        </div>
        <a-table
          :columns="columns"
          :data-source="deptTableData"
          :loading="loading"
          :pagination="false"
          row-key="departmentName"
          size="small"
        >
          <template #bodyCell="{ column, text }">
            <template v-if="column.dataIndex === 'amount'">
              {{ formatMoney(text) }}
            </template>
            <template v-else-if="column.dataIndex === 'ratio'">
              {{ text }}%
            </template>
          </template>
        </a-table>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { Dayjs } from 'dayjs'
import { SearchOutlined, ClearOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import ARReportChart from '@/components/ARReportChart/ARReportChart.vue'
import type { StatCardItem } from '@/components/ARReportPage/types'
import { expenseStatsApi } from '@/api/finance'

const loading = ref(false)
const dateRange = ref<[Dayjs, Dayjs] | null>(null)
const summary = ref<Record<string, any>>({})

function buildParams() {
  const params: Record<string, any> = {}
  if (dateRange.value?.[0]) params.startDate = dateRange.value[0].format('YYYY-MM-DD')
  if (dateRange.value?.[1]) params.endDate = dateRange.value[1].format('YYYY-MM-DD')
  return params
}

// ═══ 统计卡片（键与后端 getExpenseStatistics 一致） ═══
const statCards = computed<StatCardItem[]>(() => {
  if (!Object.keys(summary.value).length) return []
  return [
    { label: '费用总额', value: Number(summary.value.totalAmount) || 0, precision: 2, prefix: '¥' },
    { label: '已审批金额', value: Number(summary.value.approvedAmount) || 0, precision: 2, prefix: '¥' },
    { label: '待审批金额', value: Number(summary.value.pendingAmount) || 0, precision: 2, prefix: '¥' },
    { label: '已拒绝金额', value: Number(summary.value.rejectedAmount) || 0, precision: 2, prefix: '¥' },
    { label: '单据数', value: Number(summary.value.expenseCount) || 0, suffix: '单' },
    { label: '平均单额', value: Number(summary.value.averageAmount) || 0, precision: 2, prefix: '¥' }
  ]
})

// ═══ 图表：类型饼图 / 部门柱状图（byType/byDepartment 为 {名称: 金额} 映射） ═══
function mapToPairs(map: Record<string, number> | undefined): Array<{ name: string; value: number }> {
  return Object.entries(map || {}).map(([name, value]) => ({ name, value: Number(value) || 0 }))
}

const typeChartOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
  legend: { bottom: 0 },
  series: [
    {
      name: '费用类型',
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '46%'],
      data: mapToPairs(summary.value.byType)
    }
  ]
}))

const deptPairs = computed(() => mapToPairs(summary.value.byDepartment))

const deptChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 90, right: 24, top: 32, bottom: 60 },
  xAxis: { type: 'category', data: deptPairs.value.map(p => p.name), axisLabel: { rotate: 30 } },
  yAxis: { type: 'value' },
  series: [{ name: '费用金额', type: 'bar', data: deptPairs.value.map(p => p.value) }]
}))

// ═══ 部门明细表 ═══
const columns: any[] = [
  { title: '部门', dataIndex: 'departmentName', key: 'departmentName' },
  { title: '费用金额', dataIndex: 'amount', key: 'amount', width: 180, align: 'right' },
  { title: '占比', dataIndex: 'ratio', key: 'ratio', width: 120, align: 'right' }
]

const deptTableData = computed(() => {
  const total = Number(summary.value.totalAmount) || 0
  return deptPairs.value.map(p => ({
    departmentName: p.name,
    amount: p.value,
    ratio: total > 0 ? ((p.value / total) * 100).toFixed(1) : '0.0'
  }))
})

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求：费用统计汇总（/erp/expense/statistics/summary） ═══
async function loadData() {
  loading.value = true
  try {
    const res: any = await expenseStatsApi.getSummary(buildParams())
    summary.value = res && typeof res === 'object' ? res : {}
  } catch (e) {
    summary.value = {}
    console.warn('[费用统计] 获取失败', e)
  } finally {
    loading.value = false
  }
}

function handleReset() {
  dateRange.value = null
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.search-area {
  background: #fff;
  padding: 16px 20px 0;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.chart-row {
  margin-top: 16px;
}
.chart-area {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.table-area {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  margin-top: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.table-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}
</style>
