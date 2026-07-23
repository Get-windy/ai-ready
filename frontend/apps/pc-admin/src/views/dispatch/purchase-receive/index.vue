<template>
  <ARReportPage
    ref="reportRef"
    title="采购订货收货"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="采购订货收货"
    row-key="id"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.dataIndex === 'status'">
        <a-tag :color="STATUS_MAP[record.status]?.color || 'default'">
          {{ record.statusDesc || STATUS_MAP[record.status]?.label || '-' }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'totalAmount'">
        {{ formatMoney(record.totalAmount) }}
      </template>
      <template v-else-if="column.dataIndex === 'totalQuantity'">
        {{ record.totalQuantity != null ? Number(record.totalQuantity).toLocaleString('zh-CN') : '-' }}
      </template>
      <template v-else-if="column.key === 'action'">
        <a-popconfirm
          v-if="record.status === 3"
          title="确认对该入库单执行收货？"
          @confirm="handleReceive(record as any)"
        >
          <a-button
            type="link"
            size="small"
          >
            收货
          </a-button>
        </a-popconfirm>
        <span v-else-if="record.status >= 4 && record.status <= 9" class="received-text">已收货</span>
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { inboundApi, type PurchaseInbound } from '@/api/erp'

// ═══ 入库单状态（与后端 InboundStatus 枚举一致） ═══
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '待审批', color: 'orange' },
  2: { label: '已审批', color: 'blue' },
  3: { label: '待收货', color: 'orange' },
  4: { label: '已收货', color: 'cyan' },
  5: { label: '待质检', color: 'orange' },
  6: { label: '已质检', color: 'cyan' },
  7: { label: '待入库', color: 'orange' },
  8: { label: '已入库', color: 'blue' },
  9: { label: '已完成', color: 'green' },
  10: { label: '已取消', color: 'red' }
}

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '入库单号/供应商/订单号', width: 200 },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))
  }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '收货单号', dataIndex: 'inboundNo', key: 'inboundNo', width: 160 },
  { title: '采购订单号', dataIndex: 'orderNo', key: 'orderNo', width: 150 },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 160, ellipsis: true },
  { title: '收货仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 110 },
  { title: '数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 90, align: 'right' },
  { title: '金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 110, align: 'right' },
  { title: '物流公司', dataIndex: 'logisticsCompany', key: 'logisticsCompany', width: 110, ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '入库日期', dataIndex: 'inboundDate', key: 'inboundDate', width: 110 },
  { title: '收货时间', dataIndex: 'receivedTime', key: 'receivedTime', width: 160 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' }
]

const reportRef = ref<any>(null)

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求（采购入库单） ═══
function fetcher(params: Record<string, any>) {
  return inboundApi.page(params)
}

// ═══ 收货操作 ═══
async function handleReceive(record: PurchaseInbound) {
  try {
    await inboundApi.receive(record.id)
    message.success(`入库单 ${record.inboundNo} 已收货`)
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[采购订货收货] 收货失败', e)
  }
}
</script>

<style scoped>
.received-text {
  color: #8c8c8c;
  font-size: 12px;
}
</style>
