<template>
  <ARReportPage
    title="采购明细查询"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    export-file-name="采购明细查询"
    row-key="itemId"
  >
    <template #bodyCell="{ column, text }">
      <template v-if="column.dataIndex === 'status'">
        <a-tag :color="STATUS_MAP[text as number]?.color">
          {{ STATUS_MAP[text as number]?.label || text }}
        </a-tag>
      </template>
      <template v-else-if="MONEY_COLUMNS.includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
      <template v-else-if="QTY_COLUMNS.includes(column.dataIndex as string)">
        {{ formatQty(text) }}
      </template>
      <template v-else-if="column.dataIndex === 'orderDate'">
        {{ formatDateTime(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { purchaseDocQueryApi } from '@/api/purchase'

// ═══ 单据状态（与后端 OrderStatus 枚举一致） ═══
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '待审批', color: 'orange' },
  2: { label: '已审批', color: 'blue' },
  3: { label: '已下达', color: 'blue' },
  4: { label: '执行中', color: 'blue' },
  5: { label: '部分入库', color: 'cyan' },
  6: { label: '已完成', color: 'green' },
  7: { label: '已取消', color: 'red' }
}

const MONEY_COLUMNS = ['unitPrice', 'amount', 'discountedUnitPrice', 'discountedAmount']
const QTY_COLUMNS = ['quantity', 'receivedQuantity', 'unreceiveQuantity']

const queryFields: ReportQueryField[] = [
  { key: 'orderNo', type: 'input', label: '单据编号', placeholder: '单据编号', width: 170 },
  { key: 'productName', type: 'input', label: '商品', placeholder: '商品名称', width: 160 },
  { key: 'supplierName', type: 'input', label: '供应商', placeholder: '供应商名称', width: 160 },
  {
    key: 'status',
    type: 'select',
    label: '单据状态',
    placeholder: '全部状态',
    options: Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))
  },
  { key: 'dateRange', type: 'date-range', label: '单据日期' }
]

// ═══ 表格列（后端 PurchaseDetailListDTO 59列取常用） ═══
const columns: any[] = [
  { title: '单据日期', dataIndex: 'orderDate', key: 'orderDate', width: 110 },
  { title: '单据编号', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 150, ellipsis: true },
  { title: '商品名称', dataIndex: 'productName', key: 'productName', width: 160, ellipsis: true },
  { title: '货号', dataIndex: 'itemCode', key: 'itemCode', width: 110 },
  { title: '规格', dataIndex: 'specification', key: 'specification', width: 100, ellipsis: true },
  { title: '品牌', dataIndex: 'brand', key: 'brand', width: 90 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 60 },
  { title: '订货数量', dataIndex: 'quantity', key: 'quantity', width: 100, align: 'right' },
  { title: '已收数量', dataIndex: 'receivedQuantity', key: 'receivedQuantity', width: 100, align: 'right' },
  { title: '未收数量', dataIndex: 'unreceiveQuantity', key: 'unreceiveQuantity', width: 100, align: 'right' },
  { title: '单价', dataIndex: 'unitPrice', key: 'unitPrice', width: 100, align: 'right' },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 110, align: 'right' },
  { title: '优惠后单价', dataIndex: 'discountedUnitPrice', key: 'discountedUnitPrice', width: 110, align: 'right' },
  { title: '优惠后金额', dataIndex: 'discountedAmount', key: 'discountedAmount', width: 110, align: 'right' },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 100 },
  { title: '经手人', dataIndex: 'purchaserName', key: 'purchaserName', width: 90 }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatQty(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

function formatDateTime(val: string | null | undefined): string {
  return val ? String(val).slice(0, 10) : '-'
}

// ═══ 数据请求（page/size → 后端 current/size；日期 → LocalDateTime ISO 格式） ═══
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
</script>
