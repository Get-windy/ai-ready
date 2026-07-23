<template>
  <ErrorBoundary>
    <PageContainer title="预算执行">
      <!-- ═══ 查询区 ═══ -->
      <div class="search-area">
        <a-form layout="inline">
          <a-form-item label="财政年度">
            <a-select
              v-model:value="fiscalYear"
              :options="YEAR_OPTIONS"
              style="width: 140px"
            />
          </a-form-item>
          <a-form-item>
            <a-button
              type="primary"
              @click="loadData"
            >
              <template #icon>
                <SearchOutlined />
              </template>查询
            </a-button>
          </a-form-item>
        </a-form>
      </div>

      <!-- ═══ 统计卡片 ═══ -->
      <ARStatCards
        :items="statCards"
        :loading="loading"
      />

      <!-- ═══ 图表 ═══ -->
      <div class="chart-area">
        <ARReportChart
          title="部门预算执行对比"
          :option="chartOption"
          :loading="loading"
          :height="340"
        />
      </div>

      <!-- ═══ 部门执行明细表 ═══ -->
      <div class="table-area">
        <div class="table-title">
          部门执行明细
        </div>
        <a-table
          :columns="columns"
          :data-source="deptTableData"
          :loading="loading"
          :pagination="false"
          row-key="departmentId"
          size="small"
        >
          <template #bodyCell="{ column, record, text }">
            <template v-if="['totalBudget', 'totalUsed', 'remaining'].includes(column.dataIndex as string)">
              {{ formatMoney(text) }}
            </template>
            <template v-else-if="column.dataIndex === 'executionRate'">
              <a-tag :color="Number(record.executionRate) >= 90 ? 'red' : Number(record.executionRate) >= 70 ? 'orange' : 'green'">
                {{ formatRate(text) }}
              </a-tag>
            </template>
          </template>
        </a-table>
      </div>
    </PageContainer>
  </ErrorBoundary>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary.vue'
import PageContainer from '@/components/PageContainer/PageContainer.vue'
import ARStatCards from '@/components/ARStatCards/ARStatCards.vue'
import ARReportChart from '@/components/ARReportChart/ARReportChart.vue'
import type { StatCardItem } from '@/components/ARReportPage/types'
import { budgetReportApi } from '@/api/budget'

// ═══ 财政年度 ═══
const currentYear = new Date().getFullYear()
const YEAR_OPTIONS = [0, 1, 2, 3].map(i => {
  const y = currentYear - i
  return { label: `${y}年`, value: y }
})
const fiscalYear = ref(currentYear)

const loading = ref(false)
const summary = ref<Record<string, any>>({})
const deptList = ref<any[]>([])

// ═══ 统计卡片（键与后端 BudgetStatisticsDTO 一致） ═══
const statCards = computed<StatCardItem[]>(() => {
  if (!Object.keys(summary.value).length) return []
  return [
    { label: '预算总额', value: Number(summary.value.totalBudgetAmount) || 0, precision: 2, prefix: '¥' },
    { label: '已执行金额', value: Number(summary.value.totalUsedAmount) || 0, precision: 2, prefix: '¥' },
    { label: '剩余额度', value: Number(summary.value.totalRemainingAmount) || 0, precision: 2, prefix: '¥' },
    { label: '冻结金额', value: Number(summary.value.totalFrozenAmount) || 0, precision: 2, prefix: '¥' },
    { label: '执行率', value: Number(summary.value.executionRate) || 0, precision: 1, suffix: '%' },
    { label: '预算单数', value: Number(summary.value.totalBudgetCount) || 0, suffix: '份' },
    { label: '执行中', value: Number(summary.value.executingCount) || 0, suffix: '份' },
    { label: '已关闭', value: Number(summary.value.closedCount) || 0, suffix: '份' }
  ]
})

// ═══ 部门执行表（键与后端 getDepartmentSummary 一致） ═══
const columns: any[] = [
  { title: '部门', dataIndex: 'departmentName', key: 'departmentName' },
  { title: '预算总额', dataIndex: 'totalBudget', key: 'totalBudget', width: 160, align: 'right' },
  { title: '已执行', dataIndex: 'totalUsed', key: 'totalUsed', width: 160, align: 'right' },
  { title: '剩余', dataIndex: 'remaining', key: 'remaining', width: 160, align: 'right' },
  { title: '执行率', dataIndex: 'executionRate', key: 'executionRate', width: 120, align: 'center' }
]

const deptTableData = computed(() =>
  deptList.value.map(d => ({
    ...d,
    remaining: (Number(d.totalBudget) || 0) - (Number(d.totalUsed) || 0)
  }))
)

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatRate(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return `${Number(val).toFixed(1)}%`
}

// ═══ 图表：部门 预算 vs 已执行 ═══
const chartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['预算总额', '已执行'] },
  grid: { left: 90, right: 24, top: 48, bottom: 60 },
  xAxis: {
    type: 'category',
    data: deptTableData.value.map(d => d.departmentName),
    axisLabel: { rotate: 30 }
  },
  yAxis: { type: 'value' },
  series: [
    { name: '预算总额', type: 'bar', data: deptTableData.value.map(d => Number(d.totalBudget) || 0) },
    { name: '已执行', type: 'bar', data: deptTableData.value.map(d => Number(d.totalUsed) || 0) }
  ]
}))

// ═══ 数据请求：预算执行汇总 + 部门汇总 ═══
async function loadData() {
  loading.value = true
  try {
    const [sumRes, deptRes]: any[] = await Promise.all([
      budgetReportApi.executionSummary(fiscalYear.value),
      budgetReportApi.departmentSummary(fiscalYear.value)
    ])
    summary.value = sumRes && typeof sumRes === 'object' ? sumRes : {}
    deptList.value = Array.isArray(deptRes) ? deptRes : []
  } catch (e) {
    summary.value = {}
    deptList.value = []
    console.warn('[预算执行] 获取失败', e)
  } finally {
    loading.value = false
  }
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
.chart-area {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  margin: 16px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.table-area {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}
.table-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}
</style>
