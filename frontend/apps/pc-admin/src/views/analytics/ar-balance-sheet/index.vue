<template>
  <ARReportPage
    ref="reportRef"
    title="往来余额表"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    export-file-name="往来余额表"
    :row-key="(record: any) => `${record.partnerType}-${record.partnerId}`"
  >
    <template #extra-fields>
      <a-form-item label="仅非零余额">
        <a-switch
          v-model:checked="onlyNonZero"
          @change="reportRef?.reload()"
        />
      </a-form-item>
    </template>
    <template #bodyCell="{ column, text }">
      <template v-if="column.dataIndex === 'partnerType'">
        <a-tag :color="text === 'customer' ? 'blue' : 'purple'">
          {{ text === 'customer' ? '客户' : text === 'supplier' ? '供应商' : text }}
        </a-tag>
      </template>
      <template v-else-if="['receivableBalance', 'payableBalance', 'preReceiptBalance', 'prePaymentBalance', 'netBalance'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { financeAnalyticsApi } from '@/api/analytics'

const reportRef = ref<InstanceType<typeof ARReportPage>>()
const onlyNonZero = ref(false)

const queryFields: ReportQueryField[] = [
  {
    key: 'partnerType',
    type: 'select',
    label: '单位类型',
    placeholder: '全部',
    options: [
      { label: '客户', value: 'customer' },
      { label: '供应商', value: 'supplier' }
    ]
  },
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '单位名称/ID', width: 180 }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '往来单位', dataIndex: 'partnerName', key: 'partnerName', width: 180, ellipsis: true },
  { title: '类型', dataIndex: 'partnerType', key: 'partnerType', width: 90 },
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

// ═══ 数据请求 ═══
function fetcher(params: Record<string, any>) {
  return financeAnalyticsApi.partnerBalancePage({ ...params, onlyNonZero: onlyNonZero.value })
}
</script>
