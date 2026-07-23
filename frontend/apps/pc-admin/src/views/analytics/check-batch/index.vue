<template>
  <ARReportPage
    title="查批次"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="查批次"
    row-key="id"
  >
    <template #bodyCell="{ column, text }">
      <template v-if="column.dataIndex === 'batchStatus'">
        <a-tag :color="STATUS_MAP[text]?.color || 'default'">
          {{ STATUS_MAP[text]?.label || text || '-' }}
        </a-tag>
      </template>
      <template v-else-if="['totalQuantity', 'availableQuantity', 'reservedQuantity'].includes(column.dataIndex as string)">
        {{ formatNumber(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { batchSnApi, stockApi } from '@/api/analytics'

// ═══ 批次状态（与后端 BatchStatusEnum 一致） ═══
const STATUS_MAP: Record<string, { label: string; color: string }> = {
  ACTIVE: { label: '活跃中', color: 'green' },
  EXPIRED: { label: '已过期', color: 'red' },
  QUARANTINED: { label: '隔离中', color: 'orange' }
}

// ═══ 仓库下拉选项 ═══
const warehouseOptions = ref<{ label: string; value: number }[]>([])

const queryFields = computed<ReportQueryField[]>(() => [
  { key: 'batchNo', type: 'input', label: '批次号', placeholder: '批次编号', width: 180 },
  { key: 'productCode', type: 'input', label: '商品', placeholder: '商品编码', width: 160 },
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
  { title: '批次号', dataIndex: 'batchNo', key: 'batchNo', width: 160 },
  { title: '商品编码', dataIndex: 'productCode', key: 'productCode', width: 120 },
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 180, ellipsis: true },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 120 },
  { title: '库存数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 100, align: 'right' },
  { title: '可用数量', dataIndex: 'availableQuantity', key: 'availableQuantity', width: 100, align: 'right' },
  { title: '预留数量', dataIndex: 'reservedQuantity', key: 'reservedQuantity', width: 100, align: 'right' },
  { title: '生产日期', dataIndex: 'productionDate', key: 'productionDate', width: 110 },
  { title: '有效期至', dataIndex: 'expirationDate', key: 'expirationDate', width: 110 },
  { title: '状态', dataIndex: 'batchStatus', key: 'batchStatus', width: 90 }
]

function formatNumber(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN')
}

// ═══ 数据请求 ═══
function fetcher(params: Record<string, any>) {
  return batchSnApi.page(params)
}

onMounted(async () => {
  try {
    const list: any = await stockApi.getWarehouses()
    warehouseOptions.value = (Array.isArray(list) ? list : []).map((w: any) => ({
      label: w.warehouseName,
      value: w.id
    }))
  } catch (e) {
    console.warn('[查批次] 仓库列表获取失败', e)
  }
})
</script>
