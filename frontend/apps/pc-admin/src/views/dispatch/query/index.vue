<template>
  <ARReportPage
    title="配送查询"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    export-file-name="配送查询"
    row-key="id"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.dataIndex === 'status'">
        <a-tag :color="STATUS_MAP[record.status]?.color || 'default'">
          {{ STATUS_MAP[record.status]?.label || `状态${record.status}` }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'orderType'">
        <a-tag :color="ORDER_TYPE_MAP[record.orderType]?.color || 'default'">
          {{ ORDER_TYPE_MAP[record.orderType]?.label || '-' }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'priority'">
        <a-tag :color="PRIORITY_MAP[record.priority]?.color || 'default'">
          {{ PRIORITY_MAP[record.priority]?.label || '普通' }}
        </a-tag>
      </template>
      <template v-else-if="['goodsAmount', 'deliveryFee', 'collectOnDelivery'].includes(column.dataIndex as string)">
        {{ formatMoney(record[column.dataIndex as string]) }}
      </template>
      <template v-else-if="column.dataIndex === 'totalQuantity'">
        {{ record.totalQuantity != null ? Number(record.totalQuantity).toLocaleString('zh-CN') : '-' }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { taskApi } from '@/api/dms/task'

// ═══ 任务状态（与后端 TaskStatusEnum 一致） ═══
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待分配', color: 'orange' },
  1: { label: '已分配', color: 'blue' },
  2: { label: '已接单', color: 'cyan' },
  3: { label: '取货中', color: 'processing' },
  4: { label: '配送中', color: 'processing' },
  5: { label: '已签收', color: 'geekblue' },
  6: { label: '已完成', color: 'green' },
  7: { label: '已取消', color: 'red' },
  8: { label: '异常', color: 'magenta' }
}

const ORDER_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '销售配送', color: 'blue' },
  2: { label: '调拨', color: 'purple' },
  3: { label: '退货', color: 'orange' }
}

const PRIORITY_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '普通', color: 'default' },
  2: { label: '紧急', color: 'orange' },
  3: { label: '加急', color: 'red' }
}

const queryFields: ReportQueryField[] = [
  { key: 'orderNo', type: 'input', label: '订单号', placeholder: '关联订单号', width: 160 },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))
  },
  { key: 'riderId', type: 'input', label: '骑手ID', placeholder: '骑手ID', width: 120 }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '任务编号', dataIndex: 'taskNo', key: 'taskNo', width: 160 },
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 150 },
  { title: '类型', dataIndex: 'orderType', key: 'orderType', width: 100 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 130, ellipsis: true },
  { title: '联系电话', dataIndex: 'customerPhone', key: 'customerPhone', width: 120 },
  { title: '骑手ID', dataIndex: 'riderId', key: 'riderId', width: 80, align: 'center' },
  { title: '数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 90, align: 'right' },
  { title: '货品金额', dataIndex: 'goodsAmount', key: 'goodsAmount', width: 110, align: 'right' },
  { title: '配送费', dataIndex: 'deliveryFee', key: 'deliveryFee', width: 100, align: 'right' },
  { title: '代收货款', dataIndex: 'collectOnDelivery', key: 'collectOnDelivery', width: 110, align: 'right' },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求（DMS 配送任务，后端分页参数为 current/size） ═══
function fetcher(params: Record<string, any>) {
  const { page, size, ...rest } = params
  return taskApi.page({ current: page, size, ...rest })
}
</script>
