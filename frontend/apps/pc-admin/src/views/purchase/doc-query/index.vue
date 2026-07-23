<template>
  <ARReportPage
    title="采购单据查询"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    export-file-name="采购单据查询"
    row-key="id"
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

// ═══ 单据状态（与后端 OrderStatus 枚举一致：0草稿/1待审批/2已审批/3已下达/4执行中/5部分入库/6已完成/7已取消） ═══
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

const MONEY_COLUMNS = ['productAmount', 'discountAmount', 'otherExpense', 'billAmount', 'settledAmount', 'returnAmount']
const QTY_COLUMNS = ['totalQuantity', 'receivedQuantity', 'unreceiveQuantity', 'returnQuantity']

const queryFields: ReportQueryField[] = [
  { key: 'orderNo', type: 'input', label: '单据编号', placeholder: '单据编号', width: 180 },
  { key: 'supplierName', type: 'input', label: '供应商', placeholder: '供应商名称', width: 180 },
  {
    key: 'status',
    type: 'select',
    label: '单据状态',
    placeholder: '全部状态',
    options: Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))
  },
  { key: 'purchaserName', type: 'input', label: '经手人', placeholder: '经手人姓名', width: 140 },
  { key: 'dateRange', type: 'date-range', label: '单据日期' }
]

// ═══ 表格列（后端 PurchaseOrderListDTO 39列取常用） ═══
const columns: any[] = [
  { title: '单据日期', dataIndex: 'orderDate', key: 'orderDate', width: 110 },
  { title: '单据编号', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '源单', dataIndex: 'sourceBillNo', key: 'sourceBillNo', width: 140, ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 150, ellipsis: true },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 110 },
  { title: '经手人', dataIndex: 'purchaserName', key: 'purchaserName', width: 90 },
  { title: '部门', dataIndex: 'deptName', key: 'deptName', width: 100 },
  { title: '商品金额', dataIndex: 'productAmount', key: 'productAmount', width: 110, align: 'right' },
  { title: '直接优惠', dataIndex: 'discountAmount', key: 'discountAmount', width: 100, align: 'right' },
  { title: '本单金额', dataIndex: 'billAmount', key: 'billAmount', width: 110, align: 'right' },
  { title: '已结金额', dataIndex: 'settledAmount', key: 'settledAmount', width: 110, align: 'right' },
  { title: '订货数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 100, align: 'right' },
  { title: '已收数量', dataIndex: 'receivedQuantity', key: 'receivedQuantity', width: 100, align: 'right' },
  { title: '未收数量', dataIndex: 'unreceiveQuantity', key: 'unreceiveQuantity', width: 100, align: 'right' },
  { title: '单据备注', dataIndex: 'remark', key: 'remark', width: 140, ellipsis: true },
  { title: '制单人', dataIndex: 'createByName', key: 'createByName', width: 90 }
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
  return purchaseDocQueryApi.docPage({
    ...rest,
    current: page,
    size,
    dateStart: startDate ? `${startDate}T00:00:00` : undefined,
    dateEnd: endDate ? `${endDate}T23:59:59` : undefined
  })
}
</script>
