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

// ═══ 表格列（对标 21 列：指定配送日期/任务编号/配送状态/开始结束时间/司机/车辆/送货员/配送单量/订金金额/退货单量/发货数量金额/退货数量金额/装箱数量/里程/备注/制单人/制单时间） ═══
const columns: any[] = [
  { title: '指定配送日期', dataIndex: 'planDate', key: 'planDate', width: 110 },
  { title: '任务编号', dataIndex: 'taskNo', key: 'taskNo', width: 160 },
  { title: '配送状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '配送开始时间', dataIndex: 'pickupTime', key: 'pickupTime', width: 160 },
  { title: '配送结束时间', dataIndex: 'deliveryTime', key: 'deliveryTime', width: 160 },
  { title: '司机名称', dataIndex: 'driverName', key: 'driverName', width: 90 },
  { title: '配送车辆', dataIndex: 'vehicleName', key: 'vehicleName', width: 100 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 130, ellipsis: true },
  { title: '联系电话', dataIndex: 'customerPhone', key: 'customerPhone', width: 120 },
  { title: '订单号', dataIndex: 'orderNo', key: 'orderNo', width: 150 },
  { title: '发货数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 90, align: 'right' },
  { title: '发货金额', dataIndex: 'goodsAmount', key: 'goodsAmount', width: 110, align: 'right' },
  { title: '配送费', dataIndex: 'deliveryFee', key: 'deliveryFee', width: 100, align: 'right' },
  { title: '代收货款', dataIndex: 'collectOnDelivery', key: 'collectOnDelivery', width: 110, align: 'right' },
  { title: '重量(kg)', dataIndex: 'totalWeight', key: 'totalWeight', width: 90, align: 'right' },
  { title: '体积(m³)', dataIndex: 'totalVolume', key: 'totalVolume', width: 90, align: 'right' },
  { title: '配送里程(km)', dataIndex: 'estimatedDistance', key: 'estimatedDistance', width: 110, align: 'right' },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 80 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 120, ellipsis: true },
  { title: '制单时间', dataIndex: 'createTime', key: 'createTime', width: 160 }
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
