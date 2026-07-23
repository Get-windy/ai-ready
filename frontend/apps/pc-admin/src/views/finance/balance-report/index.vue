<template>
  <ErrorBoundary>
    <PageContainer title="资产负债表">
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
          <a-form-item label="会计期间">
            <a-select
              v-model:value="fiscalPeriod"
              :options="PERIOD_OPTIONS"
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
          title="资产 / 负债 / 所有者权益"
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
              <span :style="{ paddingLeft: ((record.level || 1) - 1) * 16 + 'px', fontWeight: record.level === 1 ? 600 : 400 }">
                {{ text }}
              </span>
            </template>
            <template v-else-if="column.dataIndex === 'type'">
              <a-tag :color="TYPE_MAP[text]?.color || 'default'">
                {{ TYPE_MAP[text]?.label || text }}
              </a-tag>
            </template>
            <template v-else-if="['endBalance', 'beginBalance'].includes(column.dataIndex as string)">
              {{ formatMoney(text) }}
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
const PERIOD_OPTIONS = Array.from({ length: 12 }, (_, i) => ({ label: `第${i + 1}期`, value: i + 1 }))
const fiscalYear = ref(currentYear)
const fiscalPeriod = ref(new Date().getMonth() + 1)

const TYPE_MAP: Record<string, { label: string; color: string }> = {
  asset: { label: '资产', color: 'blue' },
  liability: { label: '负债', color: 'orange' },
  equity: { label: '权益', color: 'green' }
}

// ═══ 数据 ═══
const loading = ref(false)
const tableData = ref<any[]>([])

const columns: any[] = [
  { title: '项目编码', dataIndex: 'itemCode', key: 'itemCode', width: 120 },
  { title: '项目名称', dataIndex: 'itemName', key: 'itemName' },
  { title: '类别', dataIndex: 'type', key: 'type', width: 90, align: 'center' },
  { title: '期末余额', dataIndex: 'endBalance', key: 'endBalance', width: 160, align: 'right' },
  { title: '年初余额', dataIndex: 'beginBalance', key: 'beginBalance', width: 160, align: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 汇总卡片 ═══
function sumByType(type: string, field: 'endBalance' | 'beginBalance'): number {
  return tableData.value
    .filter(r => r.type === type)
    .reduce((acc, r) => acc + (Number(r[field]) || 0), 0)
}

const statCards = computed<StatCardItem[]>(() => {
  if (!tableData.value.length) return []
  const asset = sumByType('asset', 'endBalance')
  const liability = sumByType('liability', 'endBalance')
  const equity = sumByType('equity', 'endBalance')
  return [
    { label: '资产总计', value: asset, precision: 2, prefix: '¥' },
    { label: '负债总计', value: liability, precision: 2, prefix: '¥' },
    { label: '所有者权益总计', value: equity, precision: 2, prefix: '¥' },
    {
      label: '资产 - 负债 - 权益 校验',
      value: asset - liability - equity,
      precision: 2,
      prefix: '¥',
      valueStyle: { color: Math.abs(asset - liability - equity) < 0.01 ? '#52c41a' : '#f5222d' }
    }
  ]
})

// ═══ 图表 ═══
const chartOption = computed(() => {
  const types: Array<{ key: string; name: string }> = [
    { key: 'asset', name: '资产' },
    { key: 'liability', name: '负债' },
    { key: 'equity', name: '所有者权益' }
  ]
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['期末余额', '年初余额'] },
    grid: { left: 80, right: 24, top: 48, bottom: 32 },
    xAxis: { type: 'category', data: types.map(t => t.name) },
    yAxis: { type: 'value' },
    series: [
      { name: '期末余额', type: 'bar', data: types.map(t => sumByType(t.key, 'endBalance')) },
      { name: '年初余额', type: 'bar', data: types.map(t => sumByType(t.key, 'beginBalance')) }
    ]
  }
})

// ═══ 数据请求：资产负债表 v2 ═══
async function loadData() {
  loading.value = true
  try {
    const res: any = await reportApi.getBalanceSheet({ fiscalYear: fiscalYear.value, fiscalPeriod: fiscalPeriod.value })
    tableData.value = Array.isArray(res) ? res : []
  } catch (e) {
    tableData.value = []
    console.warn('[资产负债表] 获取失败', e)
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
