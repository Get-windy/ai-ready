<template>
  <ErrorBoundary>
    <PageContainer title="利润表">
      <!-- ═══ 查询区 ═══ -->
      <div class="search-area">
        <a-form layout="inline">
          <a-form-item label="会计年度">
            <a-select
              v-model:value="fiscalYear"
              :options="YEAR_OPTIONS"
              style="width: 140px"
            />
          </a-form-item>
          <a-form-item label="开始月份">
            <a-select
              v-model:value="startMonth"
              :options="MONTH_OPTIONS"
              style="width: 120px"
            />
          </a-form-item>
          <a-form-item label="结束月份">
            <a-select
              v-model:value="endMonth"
              :options="MONTH_OPTIONS"
              style="width: 120px"
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
          title="利润构成"
          :option="chartOption"
          :loading="loading"
          :height="320"
        />
      </div>

      <!-- ═══ 表格区 ═══ -->
      <div class="table-area">
        <a-table
          :columns="columns"
          :data-source="tableData"
          :loading="loading"
          :pagination="false"
          row-key="itemCode"
          size="small"
        >
          <template #bodyCell="{ column, record, text }">
            <template v-if="column.dataIndex === 'itemName'">
              <span :style="{ fontWeight: isTotalRow(record) ? 600 : 400 }">
                {{ text }}
              </span>
            </template>
            <template v-else-if="['currentAmount', 'cumulativeAmount'].includes(column.dataIndex as string)">
              <span :style="{ fontWeight: isTotalRow(record) ? 600 : 400 }">
                {{ formatMoney(text) }}
              </span>
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
import { reportApi } from '@/api/finance'

// ═══ 会计期间 ═══
const currentYear = new Date().getFullYear()
const YEAR_OPTIONS = [0, 1, 2, 3].map(i => {
  const y = currentYear - i
  return { label: `${y}年`, value: y }
})
const MONTH_OPTIONS = Array.from({ length: 12 }, (_, i) => ({ label: `${i + 1}月`, value: i + 1 }))
const fiscalYear = ref(currentYear)
const startMonth = ref(1)
const endMonth = ref(new Date().getMonth() + 1)

// ═══ 合计行（与后端 IncomeStatementDTO 合计 itemCode 一致） ═══
const TOTAL_ROWS: Array<{ code: string; name: string }> = [
  { code: 'TOTAL_REVENUE', name: '营业收入合计' },
  { code: 'TOTAL_COST', name: '营业成本合计' },
  { code: 'GROSS_PROFIT', name: '营业毛利' },
  { code: 'TOTAL_EXPENSE', name: '期间费用合计' },
  { code: 'NET_PROFIT', name: '净利润' }
]

function isTotalRow(record: any): boolean {
  return TOTAL_ROWS.some(t => t.code === record.itemCode)
}

// ═══ 数据 ═══
const loading = ref(false)
const tableData = ref<any[]>([])

const columns: any[] = [
  { title: '项目编码', dataIndex: 'itemCode', key: 'itemCode', width: 140 },
  { title: '项目名称', dataIndex: 'itemName', key: 'itemName' },
  { title: '本期金额', dataIndex: 'currentAmount', key: 'currentAmount', width: 180, align: 'right' },
  { title: '本年累计金额', dataIndex: 'cumulativeAmount', key: 'cumulativeAmount', width: 180, align: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function totalValue(code: string): number {
  const row = tableData.value.find(r => r.itemCode === code)
  return Number(row?.currentAmount) || 0
}

// ═══ 统计卡片 ═══
const statCards = computed<StatCardItem[]>(() => {
  if (!tableData.value.length) return []
  const netProfit = totalValue('NET_PROFIT')
  return [
    { label: '营业收入合计', value: totalValue('TOTAL_REVENUE'), precision: 2, prefix: '¥' },
    { label: '营业成本合计', value: totalValue('TOTAL_COST'), precision: 2, prefix: '¥' },
    { label: '营业毛利', value: totalValue('GROSS_PROFIT'), precision: 2, prefix: '¥' },
    { label: '期间费用合计', value: totalValue('TOTAL_EXPENSE'), precision: 2, prefix: '¥' },
    {
      label: '净利润',
      value: netProfit,
      precision: 2,
      prefix: '¥',
      valueStyle: { color: netProfit >= 0 ? '#52c41a' : '#f5222d' }
    }
  ]
})

// ═══ 图表 ═══
const chartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 90, right: 24, top: 32, bottom: 32 },
  xAxis: { type: 'category', data: TOTAL_ROWS.map(t => t.name) },
  yAxis: { type: 'value' },
  series: [
    {
      name: '本期金额',
      type: 'bar',
      data: TOTAL_ROWS.map(t => totalValue(t.code)),
      itemStyle: {
        color: (p: any) => (Number(p.value) >= 0 ? '#5470c6' : '#ee6666')
      }
    }
  ]
}))

// ═══ 数据请求：利润表 v2 ═══
async function loadData() {
  loading.value = true
  try {
    const res: any = await reportApi.getIncomeStatement({
      fiscalYear: fiscalYear.value,
      startMonth: startMonth.value,
      endMonth: endMonth.value
    })
    tableData.value = Array.isArray(res) ? res : []
  } catch (e) {
    tableData.value = []
    console.warn('[利润表] 获取失败', e)
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
</style>
