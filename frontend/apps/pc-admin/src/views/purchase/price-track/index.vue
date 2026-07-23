<template>
  <ARReportPage
    ref="reportRef"
    title="采购价格跟踪"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    export-file-name="采购价格跟踪"
    row-key="itemId"
    empty-text="暂无价格明细，请调整查询条件"
    @loaded="handleLoaded"
  >
    <template #chart>
      <div class="chart-wrapper">
        <ARReportChart
          title="采购单价走势（当前查询结果）"
          :option="chartOption"
          :loading="false"
          :height="300"
          empty-text="执行查询后按单据日期展示采购单价走势"
        />
      </div>
    </template>
    <template #bodyCell="{ column, text }">
      <template v-if="MONEY_COLUMNS.includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
      <template v-else-if="column.dataIndex === 'quantity'">
        {{ formatQty(text) }}
      </template>
      <template v-else-if="column.dataIndex === 'orderDate'">
        {{ formatDate(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import ARReportChart from '@/components/ARReportChart/ARReportChart.vue'
import type { ReportFetchResult, ReportQueryField } from '@/components/ARReportPage/types'
import { purchaseDocQueryApi } from '@/api/purchase'

const MONEY_COLUMNS = ['unitPrice', 'discountedUnitPrice', 'amount']

const queryFields: ReportQueryField[] = [
  { key: 'productName', type: 'input', label: '商品', placeholder: '商品名称', width: 180 },
  { key: 'supplierName', type: 'input', label: '供应商', placeholder: '供应商名称', width: 160 },
  { key: 'dateRange', type: 'date-range', label: '单据日期' }
]

// ═══ 表格列（采购明细价格口径） ═══
const columns: any[] = [
  { title: '单据日期', dataIndex: 'orderDate', key: 'orderDate', width: 110 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 170, ellipsis: true },
  { title: '货号', dataIndex: 'itemCode', key: 'itemCode', width: 110 },
  { title: '规格', dataIndex: 'specification', key: 'specification', width: 100, ellipsis: true },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 150, ellipsis: true },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 110, align: 'right' },
  { title: '优惠后单价', dataIndex: 'discountedUnitPrice', key: 'discountedUnitPrice', width: 110, align: 'right' },
  { title: '订货数量', dataIndex: 'quantity', key: 'quantity', width: 100, align: 'right' },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 110, align: 'right' },
  { title: '单据编号', dataIndex: 'orderNo', key: 'orderNo', width: 150 },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 100 }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatQty(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function formatDate(val: string | null | undefined): string {
  return val ? String(val).slice(0, 10) : '-'
}

// ═══ 数据请求（page/size → 后端 current/size；日期 → LocalDateTime ISO） ═══
function fetcher(params: Record<string, any>) {
  const { page, size, startDate, endDate, ...rest } = params
  return purchaseDocQueryApi.detailPage({
    ...rest,
    current: page,
    size,
    dateStart: startDate ? `${startDate}T00:00:00` : undefined,
    dateEnd: endDate ? `${endDate}T23:59:59` : undefined
  })
}

// ═══ 图表数据 ═══
const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)
const tableData = ref<any[]>([])

function handleLoaded(result: ReportFetchResult) {
  tableData.value = result.list || []
}

// ═══ 价格走势图（当前页结果按单据日期升序） ═══
const sortedTrend = computed(() =>
  [...tableData.value]
    .filter(r => r.orderDate)
    .sort((a, b) => String(a.orderDate).localeCompare(String(b.orderDate)))
)

const chartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    valueFormatter: (v: any) => (v === null || v === undefined ? '-' : `¥${Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}`)
  },
  legend: { data: ['单价', '优惠后单价'], top: 0 },
  grid: { left: 70, right: 30, top: 36, bottom: 30 },
  xAxis: { type: 'category', data: sortedTrend.value.map(r => formatDate(r.orderDate)) },
  yAxis: { type: 'value', name: '单价(元)' },
  series: [
    {
      name: '单价',
      type: 'line',
      smooth: true,
      symbolSize: 6,
      itemStyle: { color: '#1890ff' },
      data: sortedTrend.value.map(r => (r.unitPrice === null || r.unitPrice === undefined ? null : Number(r.unitPrice)))
    },
    {
      name: '优惠后单价',
      type: 'line',
      smooth: true,
      symbolSize: 6,
      itemStyle: { color: '#52c41a' },
      data: sortedTrend.value.map(r => (r.discountedUnitPrice === null || r.discountedUnitPrice === undefined ? null : Number(r.discountedUnitPrice)))
    }
  ]
}))
</script>
