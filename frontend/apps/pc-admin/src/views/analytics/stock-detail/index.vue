<template>
  <ARReportPage
    title="库存明细"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="库存明细"
    :row-key="(record: any) => `${record.docNo}-${record.productId}-${record.moveTime}`"
  >
    <template #bodyCell="{ column, text }">
      <template v-if="column.dataIndex === 'qty'">
        <span :style="{ color: Number(text) >= 0 ? '#52c41a' : '#f5222d' }">
          {{ Number(text) >= 0 ? '+' : '' }}{{ formatNumber(text) }}
        </span>
      </template>
      <template v-else-if="['balanceAfter'].includes(column.dataIndex as string)">
        {{ formatNumber(text) }}
      </template>
      <template v-else-if="['unitCost', 'amount'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { stockReportApi, stockApi } from '@/api/analytics'

// ═══ 单据类型（与后端 StockReportController flow/page docType 一致） ═══
const DOC_TYPE_OPTIONS = [
  { label: '采购入库', value: 'PURCHASE_IN' },
  { label: '销售出库', value: 'SALE_OUT' },
  { label: '调拨入库', value: 'TRANSFER_IN' },
  { label: '调拨出库', value: 'TRANSFER_OUT' },
  { label: '报溢入库', value: 'OVERFLOW_IN' },
  { label: '报损出库', value: 'DAMAGE_OUT' },
  { label: '盘点调整', value: 'CHECK_ADJUST' }
]

// ═══ 仓库下拉选项 ═══
const warehouseOptions = ref<{ label: string; value: number }[]>([])

const queryFields = computed<ReportQueryField[]>(() => [
  { key: 'productId', type: 'input', label: '商品ID', placeholder: '商品ID', width: 140 },
  {
    key: 'warehouseId',
    type: 'select',
    label: '仓库',
    placeholder: '全部仓库',
    options: warehouseOptions.value
  },
  {
    key: 'docType',
    type: 'select',
    label: '单据类型',
    placeholder: '全部类型',
    options: DOC_TYPE_OPTIONS
  },
  { key: 'moveDateRange', type: 'date-range', label: '变动日期' }
])

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '变动时间', dataIndex: 'moveTime', key: 'moveTime', width: 160 },
  { title: '单据类型', dataIndex: 'docTypeName', key: 'docTypeName', width: 100 },
  { title: '单号', dataIndex: 'docNo', key: 'docNo', width: 160 },
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 170, ellipsis: true },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 110 },
  { title: '变动数量', dataIndex: 'qty', key: 'qty', width: 100, align: 'right' },
  { title: '单位成本', dataIndex: 'unitCost', key: 'unitCost', width: 100, align: 'right' },
  { title: '变动金额', dataIndex: 'amount', key: 'amount', width: 110, align: 'right' },
  { title: '变动后结存', dataIndex: 'balanceAfter', key: 'balanceAfter', width: 110, align: 'right' },
  { title: '操作人', dataIndex: 'operatorName', key: 'operatorName', width: 90 }
]

function formatNumber(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求 ═══
function fetcher(params: Record<string, any>) {
  return stockReportApi.flowPage(params)
}

onMounted(async () => {
  try {
    const list: any = await stockApi.getWarehouses()
    warehouseOptions.value = (Array.isArray(list) ? list : []).map((w: any) => ({
      label: w.warehouseName,
      value: w.id
    }))
  } catch (e) {
    console.warn('[库存明细] 仓库列表获取失败', e)
  }
})
</script>
