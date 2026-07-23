<template>
  <div>
    <a-alert
      type="warning"
      show-icon
      class="gap-alert"
      message="后端暂未提供在线支付对账专用端点（无支付渠道对账 Controller），当前展示资金流水（含支付方式与交易流水号）用于人工对账；自动对账能力需后端补建后接入。"
    />
    <ARReportPage
      title="在线支付对账"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      page-param-style="pageNum"
      export-file-name="在线支付对账"
      row-key="id"
    >
      <template #bodyCell="{ column, text }">
        <template v-if="column.dataIndex === 'direction'">
          <a-tag :color="text === 'IN' ? 'green' : 'red'">
            {{ text === 'IN' ? '收入' : text === 'OUT' ? '支出' : text }}
          </a-tag>
        </template>
        <template v-else-if="['amount', 'balance'].includes(column.dataIndex as string)">
          {{ formatMoney(text) }}
        </template>
      </template>
    </ARReportPage>
  </div>
</template>

<script setup lang="ts">
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { capitalFlowApi } from '@/api/finance'

const queryFields: ReportQueryField[] = [
  {
    key: 'direction',
    type: 'select',
    label: '收支方向',
    placeholder: '全部',
    options: [
      { label: '收入', value: 'IN' },
      { label: '支出', value: 'OUT' }
    ],
    width: 120
  },
  { key: 'occurDateRange', type: 'date-range', label: '发生日期' }
]

// ═══ 表格列（与后端 CapitalFlowDTO 字段一致） ═══
const columns: any[] = [
  { title: '流水号', dataIndex: 'flowNo', key: 'flowNo', width: 170 },
  { title: '方向', dataIndex: 'direction', key: 'direction', width: 80, align: 'center' },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 130, align: 'right' },
  { title: '账户余额', dataIndex: 'balance', key: 'balance', width: 130, align: 'right' },
  { title: '往来单位', dataIndex: 'partyName', key: 'partyName', width: 150, ellipsis: true },
  { title: '业务类型', dataIndex: 'businessType', key: 'businessType', width: 110 },
  { title: '支付方式', dataIndex: 'paymentMethod', key: 'paymentMethod', width: 110 },
  { title: '交易流水号', dataIndex: 'transactionNo', key: 'transactionNo', width: 180, ellipsis: true },
  { title: '关联单号', dataIndex: 'refNo', key: 'refNo', width: 160 },
  { title: '发生日期', dataIndex: 'occurDate', key: 'occurDate', width: 110 }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求：资金流水分页（/erp/capital-flow/page，pageNum/pageSize 风格） ═══
function fetcher(params: Record<string, any>) {
  return capitalFlowApi.getPage(params)
}
</script>

<style scoped>
.gap-alert {
  margin-bottom: 16px;
}
</style>
