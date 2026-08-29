<template>
  <ARReportPage
    title="在线支付对账"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="在线支付对账"
    row-key="id"
  >
    <template #bodyCell="{ column, text, record }">
      <template v-if="column.dataIndex === 'reconcileFlag'">
        <a-tag
          :color="record.reconcileFlag === 1 ? 'green' : 'default'"
          style="cursor: pointer"
          @click="toggleReconcile(record)"
        >
          {{ record.reconcileFlag === 1 ? '✓ 已对账' : '未对账' }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'direction'">
        <a-tag :color="text === 'IN' ? 'green' : 'red'">
          {{ text === 'IN' ? '收款' : text === 'OUT' ? '退款' : text }}
        </a-tag>
      </template>
      <template v-else-if="['amount', 'balance'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { capitalFlowApi } from '@/api/finance'
import request from '@/utils/request'

// ═══ 查询条件 ═══
const queryFields = ref<ReportQueryField[]>([
  { key: 'keyword', type: 'input', label: '流水号/来源订单', placeholder: '流水号/来源订单/交易号/客户', width: 220 },
  {
    key: 'paymentType',
    type: 'select',
    label: '支付类型',
    placeholder: '全部',
    options: [
      { label: '收款', value: 1 },
      { label: '退款', value: 2 }
    ],
    width: 120
  },
  {
    key: 'reconcileFlag',
    type: 'select',
    label: '对账标记',
    placeholder: '全部',
    options: [
      { label: '未对账', value: 0 },
      { label: '已对账', value: 1 }
    ],
    width: 120
  },
  { key: 'occurDateRange', type: 'date-range', label: '提交日期' }
])

// ═══ 表格列（与后端 CapitalFlowDTO 字段一致，含对账标记） ═══
const columns: any[] = [
  { title: '对账标记', dataIndex: 'reconcileFlag', key: 'reconcileFlag', width: 90, align: 'center' },
  { title: '来源订单', dataIndex: 'refNo', key: 'refNo', width: 160 },
  { title: '提交日期', dataIndex: 'occurDate', key: 'occurDate', width: 150 },
  { title: '源单客户', dataIndex: 'partyName', key: 'partyName', width: 150, ellipsis: true },
  { title: '订单金额', dataIndex: 'amount', key: 'amount', width: 130, align: 'right' },
  { title: '流水号', dataIndex: 'flowNo', key: 'flowNo', width: 190, ellipsis: true },
  { title: '支付状态', dataIndex: 'direction', key: 'direction', width: 80, align: 'center' },
  { title: '关联交易号', dataIndex: 'transactionNo', key: 'transactionNo', width: 180, ellipsis: true },
  { title: '支付类型', dataIndex: 'businessType', key: 'businessType', width: 100 },
  { title: '账户余额', dataIndex: 'balance', key: 'balance', width: 130, align: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求：在线支付对账分页（/erp/capital-flow/reconcile/page） ═══
async function fetcher(params: Record<string, any>) {
  const res: any = await request.get('/erp/capital-flow/reconcile/page', {
    params: {
      ...params,
      startDate: params.startDate || undefined,
      endDate: params.endDate || undefined
    }
  })
  return res?.data || res
}

// ═══ 行级操作：切换对账标记 ═══
async function toggleReconcile(record: any) {
  const next = record.reconcileFlag === 1 ? 0 : 1
  try {
    await request.put(`/erp/capital-flow/reconcile/${record.id}`, null, { params: { flag: next } })
    record.reconcileFlag = next
    message.success(next === 1 ? '已标记对账' : '已取消对账')
  } catch {
    message.error('操作失败')
  }
}

onMounted(() => {})
</script>
