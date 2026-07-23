<template>
  <ARReportPage
    title="进销存分析"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="进销存分析"
    :row-key="(record: any) => `${record.productId}-${record.warehouseId}`"
  >
    <template #bodyCell="{ column, text }">
      <template v-if="['openingQty', 'inQty', 'outQty', 'closingQty'].includes(column.dataIndex as string)">
        {{ formatNumber(text) }}
      </template>
      <template v-else-if="['openingAmt', 'inAmt', 'outAmt', 'closingAmt'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import dayjs from 'dayjs'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { stockReportApi, stockApi } from '@/api/analytics'

// ═══ 仓库下拉选项 ═══
const warehouseOptions = ref<{ label: string; value: number }[]>([])

const queryFields = computed<ReportQueryField[]>(() => [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '商品编码/名称', width: 180 },
  {
    key: 'warehouseId',
    type: 'select',
    label: '仓库',
    placeholder: '全部仓库',
    options: warehouseOptions.value
  },
  { key: 'periodRange', type: 'date-range', label: '期间（默认本月）' }
])

// ═══ 表格列（每商品+仓库一行：期初/入/出/结存） ═══
const columns: any[] = [
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 170, ellipsis: true },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 110 },
  { title: '期初数量', dataIndex: 'openingQty', key: 'openingQty', width: 100, align: 'right' },
  { title: '期初金额', dataIndex: 'openingAmt', key: 'openingAmt', width: 110, align: 'right' },
  { title: '入库数量', dataIndex: 'inQty', key: 'inQty', width: 100, align: 'right' },
  { title: '入库金额', dataIndex: 'inAmt', key: 'inAmt', width: 110, align: 'right' },
  { title: '出库数量', dataIndex: 'outQty', key: 'outQty', width: 100, align: 'right' },
  { title: '出库金额', dataIndex: 'outAmt', key: 'outAmt', width: 110, align: 'right' },
  { title: '结存数量', dataIndex: 'closingQty', key: 'closingQty', width: 100, align: 'right' },
  { title: '结存金额', dataIndex: 'closingAmt', key: 'closingAmt', width: 110, align: 'right' }
]

function formatNumber(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求（期间默认本月） ═══
function fetcher(params: Record<string, any>) {
  return stockReportApi.invSummaryPage({
    startDate: dayjs().startOf('month').format('YYYY-MM-DD'),
    endDate: dayjs().endOf('month').format('YYYY-MM-DD'),
    ...params
  })
}

onMounted(async () => {
  try {
    const list: any = await stockApi.getWarehouses()
    warehouseOptions.value = (Array.isArray(list) ? list : []).map((w: any) => ({
      label: w.warehouseName,
      value: w.id
    }))
  } catch (e) {
    console.warn('[进销存分析] 仓库列表获取失败', e)
  }
})
</script>
