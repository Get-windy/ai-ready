<template>
  <ARReportPage
    ref="reportRef"
    title="待确认款项"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="待确认款项"
    :row-key="(record: any) => `${record.docType}-${record.id}`"
    empty-text="暂无待确认款项"
  >
    <template #bodyCell="{ column, record, text }">
      <template v-if="column.dataIndex === 'docType'">
        <a-tag :color="text === 'receipt' ? 'blue' : 'purple'">
          {{ text === 'receipt' ? '收款单' : '付款单' }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'status'">
        <a-tag :color="STATUS_MAP[text]?.color || 'default'">
          {{ record.statusDesc || STATUS_MAP[text]?.label || text }}
        </a-tag>
      </template>
      <template v-else-if="['amount', 'verifiedAmount', 'pendingAmount'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
      <template v-else-if="column.key === 'action'">
        <a-popconfirm
          v-if="record.status === 3"
          title="确认完成该笔款项的核销？"
          @confirm="handleConfirm(record)"
        >
          <a-button
            type="link"
            size="small"
          >
            确认核销
          </a-button>
        </a-popconfirm>
        <span v-else>-</span>
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { receiptApi, paymentApi } from '@/api/finance'

// ═══ 单据状态（与后端 ReceiptStatus/PaymentStatus 一致） ═══
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  [-1]: { label: '已取消', color: 'red' },
  0: { label: '草稿', color: 'default' },
  1: { label: '待审批', color: 'orange' },
  2: { label: '已审批', color: 'blue' },
  3: { label: '核销中', color: 'orange' },
  4: { label: '已核销', color: 'green' },
  5: { label: '已完成', color: 'green' }
}

const queryFields: ReportQueryField[] = [
  {
    key: 'docType',
    type: 'select',
    label: '款项类型',
    placeholder: '默认收款单',
    options: [
      { label: '收款单', value: 'receipt' },
      { label: '付款单', value: 'payment' }
    ],
    width: 140
  },
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '单号/往来单位', width: 180 },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '默认核销中',
    options: [
      { label: '待审批', value: 1 },
      { label: '已审批', value: 2 },
      { label: '核销中', value: 3 }
    ],
    width: 140
  }
]

// ═══ 表格列（收款单/付款单归一化字段） ═══
const columns: any[] = [
  { title: '单据类型', dataIndex: 'docType', key: 'docType', width: 90, align: 'center' },
  { title: '单号', dataIndex: 'docNo', key: 'docNo', width: 170 },
  { title: '往来单位', dataIndex: 'partyName', key: 'partyName', width: 160, ellipsis: true },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 120, align: 'right' },
  { title: '已核销', dataIndex: 'verifiedAmount', key: 'verifiedAmount', width: 110, align: 'right' },
  { title: '待核销', dataIndex: 'pendingAmount', key: 'pendingAmount', width: 110, align: 'right' },
  { title: '单据日期', dataIndex: 'docDate', key: 'docDate', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90, align: 'center' },
  { title: '操作', key: 'action', width: 110, fixed: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求：收款单/付款单分页，归一化为统一行结构 ═══
async function fetcher(params: Record<string, any>) {
  const docType = params.docType || 'receipt'
  const status = params.status ?? 3 // 默认查「核销中」的待确认款项
  const query: Record<string, any> = { ...params, status }
  delete query.docType
  const res: any = docType === 'payment'
    ? await paymentApi.getPage(query)
    : await receiptApi.getPage(query)
  const records = res?.records || res?.list || []
  const list = records.map((r: any) => ({
    id: r.id,
    docType,
    docNo: docType === 'payment' ? r.paymentNo : r.receiptNo,
    partyName: docType === 'payment' ? r.supplierName : r.customerName,
    amount: docType === 'payment' ? r.paymentAmount : r.receiptAmount,
    verifiedAmount: r.verifiedAmount,
    pendingAmount: r.pendingAmount,
    docDate: docType === 'payment' ? r.paymentDate : r.receiptDate,
    status: r.status,
    statusDesc: r.statusDesc
  }))
  return { records: list, total: Number(res?.total) || list.length }
}

// ═══ 确认核销 ═══
const reportRef = ref<InstanceType<typeof ARReportPage>>()

async function handleConfirm(record: any) {
  try {
    if (record.docType === 'payment') await paymentApi.completeVerify(record.id)
    else await receiptApi.completeVerify(record.id)
    message.success('核销确认成功')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[待确认款项] 确认核销失败', e)
  }
}
</script>
