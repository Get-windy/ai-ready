<template>
  <ARReportPage
    title="订单处理中心"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    :stat-cards="statCards"
    export-file-name="订单处理中心"
    row-key="id"
  >
    <template #bodyCell="{ column, text }">
      <template v-if="column.dataIndex === 'statusName'">
        <a-tag :color="statusColor(text)">
          {{ text || '-' }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'settlementStatus'">
        <a-tag :color="settlementColor(text)">
          {{ text || '-' }}
        </a-tag>
      </template>
      <template v-else-if="MONEY_COLUMNS.includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
      <template v-else-if="QTY_COLUMNS.includes(column.dataIndex as string)">
        {{ formatQty(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField, StatCardItem } from '@/components/ARReportPage/types'
import { saleOrderApi } from '@/api/erp'
import type { OrderCenterStats } from '@/api/erp'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const tenantId = computed(() => userStore.tenantId || 1)

// ═══ 单据状态（与后端 SaleOrderStatus 一致：0草稿/1待审核/2待发货/3部分发货/4已发货/5已完成/6已取消） ═══
const STATUS_OPTIONS = [
  { label: '草稿', value: 0 },
  { label: '待审核', value: 1 },
  { label: '待发货', value: 2 },
  { label: '部分发货', value: 3 },
  { label: '已发货', value: 4 },
  { label: '已完成', value: 5 },
  { label: '已取消', value: 6 }
]

const MONEY_COLUMNS = ['productAmount', 'discountAmount', 'billAmount', 'settledAmount', 'receivedAmount']
const QTY_COLUMNS = ['totalQuantity', 'shippedQuantity', 'unshippedQuantity']

const queryFields: ReportQueryField[] = [
  { key: 'orderNo', type: 'input', label: '单号', placeholder: '订单编号', width: 180 },
  { key: 'customerName', type: 'input', label: '客户', placeholder: '客户名称', width: 160 },
  { key: 'salesmanName', type: 'input', label: '经手人', placeholder: '经手人姓名', width: 140 },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: STATUS_OPTIONS
  },
  { key: 'dateRange', type: 'date-range', label: '订单日期' }
]

// ═══ 表格列（后端 SaleOrderListDTO 取常用） ═══
const columns: any[] = [
  { title: '订单日期', dataIndex: 'orderDate', key: 'orderDate', width: 110 },
  { title: '订单编号', dataIndex: 'orderNo', key: 'orderNo', width: 160 },
  { title: '状态', dataIndex: 'statusName', key: 'statusName', width: 90 },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 150, ellipsis: true },
  { title: '经手人', dataIndex: 'salesmanName', key: 'salesmanName', width: 90 },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 110 },
  { title: '商品金额', dataIndex: 'productAmount', key: 'productAmount', width: 110, align: 'right' },
  { title: '优惠', dataIndex: 'discountAmount', key: 'discountAmount', width: 100, align: 'right' },
  { title: '本单金额', dataIndex: 'billAmount', key: 'billAmount', width: 110, align: 'right' },
  { title: '已结金额', dataIndex: 'settledAmount', key: 'settledAmount', width: 110, align: 'right' },
  { title: '结算状态', dataIndex: 'settlementStatus', key: 'settlementStatus', width: 90 },
  { title: '订货数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 90, align: 'right' },
  { title: '已发数量', dataIndex: 'shippedQuantity', key: 'shippedQuantity', width: 90, align: 'right' },
  { title: '未发数量', dataIndex: 'unshippedQuantity', key: 'unshippedQuantity', width: 90, align: 'right' },
  { title: '配送方式', dataIndex: 'deliveryMethod', key: 'deliveryMethod', width: 90 },
  { title: '收货人', dataIndex: 'receiverName', key: 'receiverName', width: 90 },
  { title: '制单人', dataIndex: 'creatorName', key: 'creatorName', width: 90 }
]

// ═══ 状态着色（完成绿/待橙/取消红/草稿灰） ═══
function statusColor(statusName: string): string {
  if (!statusName) return 'default'
  if (statusName.includes('完成') || statusName.includes('已发货')) return 'green'
  if (statusName.includes('待') || statusName.includes('部分')) return 'orange'
  if (statusName.includes('取消') || statusName.includes('作废')) return 'red'
  if (statusName.includes('草稿')) return 'default'
  return 'blue'
}

function settlementColor(status: string): string {
  if (!status) return 'default'
  if (status.includes('已结') || status.includes('结清')) return 'green'
  if (status.includes('部分')) return 'orange'
  if (status.includes('未结')) return 'red'
  return 'default'
}

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatQty(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

// ═══ 数据请求（/erp/sale/order/center/page-by-doc，需 tenantId） ═══
function fetcher(params: Record<string, any>) {
  const { page, size, ...rest } = params
  // 首页查询时同步刷新统计卡片，口径与查询条件一致
  if (Number(page) === 1) loadStats(rest)
  return saleOrderApi.getCenterPageByDoc({
    ...rest,
    pageNum: page,
    pageSize: size,
    tenantId: tenantId.value
  })
}

// ═══ 统计卡片（/erp/sale/order/center/stats） ═══
const stats = ref<Partial<OrderCenterStats>>({})

async function loadStats(query: Record<string, any>) {
  try {
    const res: any = await saleOrderApi.getCenterStats({
      tenantId: tenantId.value,
      startDate: query.startDate,
      endDate: query.endDate,
      status: query.status
    })
    // 拦截器已解包 ApiResponse.data；防御性兼容未解包场景
    stats.value = (res?.data ?? res) || {}
  } catch (e) {
    stats.value = {}
    console.warn('[订单处理中心] 统计卡片获取失败', e)
  }
}

const statCards = computed<StatCardItem[]>(() => {
  const s = stats.value
  return [
    { label: '期间订单', value: Number(s.totalOrders) || 0, suffix: '单' },
    { label: '待出库', value: Number(s.pendingOutbound) || 0, suffix: '单', valueStyle: { color: '#fa8c16' } },
    { label: '待发货', value: Number(s.pendingShip) || 0, suffix: '单', valueStyle: { color: '#fa8c16' } },
    { label: '已出库', value: Number(s.outboundCount) || 0, suffix: '单' },
    { label: '已发货', value: Number(s.shippedCount) || 0, suffix: '单' },
    { label: '已完成', value: Number(s.completedCount) || 0, suffix: '单', valueStyle: { color: '#52c41a' } }
  ]
})
</script>
