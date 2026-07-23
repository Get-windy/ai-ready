<template>
  <ARReportPage
    title="辅助核算余额表"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    export-file-name="辅助核算余额表"
    :row-key="(record: any) => `${record.partnerType}-${record.partnerId}`"
  >
    <template #bodyCell="{ column, text }">
      <template v-if="column.dataIndex === 'partnerType'">
        <a-tag :color="text === 'customer' ? 'blue' : 'purple'">
          {{ text === 'customer' ? '客户' : text === 'supplier' ? '供应商' : text }}
        </a-tag>
      </template>
      <template v-else-if="MONEY_FIELDS.includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { partnerBalanceApi } from '@/api/finance'

const MONEY_FIELDS = ['receivableBalance', 'payableBalance', 'preReceiptBalance', 'prePaymentBalance', 'netBalance']

const queryFields: ReportQueryField[] = [
  {
    key: 'partnerType',
    type: 'select',
    label: '单位类型',
    placeholder: '全部',
    options: [
      { label: '客户', value: 'customer' },
      { label: '供应商', value: 'supplier' }
    ],
    width: 140
  },
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '名称或编号', width: 180 },
  {
    key: 'onlyNonZero',
    type: 'select',
    label: '余额过滤',
    placeholder: '全部',
    options: [
      { label: '仅显示余额非零', value: '1' },
      { label: '显示全部', value: '0' }
    ],
    width: 160
  }
]

// ═══ 表格列（与后端 PartnerBalanceDTO 字段一致） ═══
const columns: any[] = [
  { title: '单位类型', dataIndex: 'partnerType', key: 'partnerType', width: 90, align: 'center' },
  { title: '往来单位', dataIndex: 'partnerName', key: 'partnerName', width: 180, ellipsis: true },
  { title: '应收余额', dataIndex: 'receivableBalance', key: 'receivableBalance', width: 120, align: 'right' },
  { title: '应付余额', dataIndex: 'payableBalance', key: 'payableBalance', width: 120, align: 'right' },
  { title: '预收余额', dataIndex: 'preReceiptBalance', key: 'preReceiptBalance', width: 120, align: 'right' },
  { title: '预付余额', dataIndex: 'prePaymentBalance', key: 'prePaymentBalance', width: 120, align: 'right' },
  { title: '净余额', dataIndex: 'netBalance', key: 'netBalance', width: 120, align: 'right' },
  { title: '最后业务日期', dataIndex: 'lastBizDate', key: 'lastBizDate', width: 120 }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求：往来余额表分页（/erp/finance/partner-balance/page） ═══
function fetcher(params: Record<string, any>) {
  return partnerBalanceApi.getPage({
    ...params,
    onlyNonZero: params.onlyNonZero === '1'
  })
}
</script>
