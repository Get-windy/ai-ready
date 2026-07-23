<template>
  <ARReportPage
    title="查库存"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="查库存"
    row-key="id"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { stockApi } from '@/api/analytics'

// ═══ 仓库下拉选项 ═══
const warehouseOptions = ref<{ label: string; value: number }[]>([])

const queryFields = computed<ReportQueryField[]>(() => [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '商品编码/名称' },
  {
    key: 'warehouseId',
    type: 'select',
    label: '仓库',
    placeholder: '全部仓库',
    options: warehouseOptions.value
  }
])

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 180, ellipsis: true },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 100, align: 'right' },
  { title: '可用数量', dataIndex: 'availableQuantity', key: 'availableQuantity', width: 100, align: 'right' },
  { title: '冻结数量', dataIndex: 'frozenQuantity', key: 'frozenQuantity', width: 100, align: 'right' },
  { title: '安全库存', dataIndex: 'safetyStock', key: 'safetyStock', width: 100, align: 'right' },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 70 },
  {
    title: '成本价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100, align: 'right',
    customRender: ({ text }: { text: number }) => formatMoney(text)
  },
  {
    title: '库存金额', key: 'stockAmount', width: 120, align: 'right',
    customRender: ({ record }: { record: any }) => formatMoney((Number(record.quantity) || 0) * (Number(record.unitPrice) || 0))
  }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求 ═══
function fetcher(params: Record<string, any>) {
  return stockApi.page(params)
}

onMounted(async () => {
  try {
    const list: any = await stockApi.getWarehouses()
    warehouseOptions.value = (Array.isArray(list) ? list : []).map((w: any) => ({
      label: w.warehouseName,
      value: w.id
    }))
  } catch (e) {
    console.warn('[查库存] 仓库列表获取失败', e)
  }
})
</script>
