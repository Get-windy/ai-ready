<template>
  <div>
    <a-alert
      type="warning"
      show-icon
      class="gap-alert"
      message="后端暂未提供明细账逐笔端点（无 LedgerController），当前展示所选会计期间内的记账凭证列表作为最近真实数据视图。"
    />
    <ARReportPage
      title="明细账"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      export-file-name="明细账"
      row-key="id"
    >
      <template #bodyCell="{ column, text }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="STATUS_MAP[text]?.color || 'default'">
            {{ STATUS_MAP[text]?.label || text }}
          </a-tag>
        </template>
        <template v-else-if="['totalDebit', 'totalCredit'].includes(column.dataIndex as string)">
          {{ formatMoney(text) }}
        </template>
      </template>
    </ARReportPage>
  </div>
</template>

<script setup lang="ts">
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { voucherApi } from '@/api/finance'

// ═══ 凭证状态（与后端 Voucher.status 一致：draft/audited/posted） ═══
const STATUS_MAP: Record<string, { label: string; color: string }> = {
  draft: { label: '草稿', color: 'default' },
  audited: { label: '已审核', color: 'orange' },
  posted: { label: '已过账', color: 'green' },
  reversed: { label: '已冲销', color: 'red' }
}

// ═══ 会计期间选项 ═══
const currentYear = new Date().getFullYear()
const currentPeriod = new Date().getMonth() + 1
const YEAR_OPTIONS = [0, 1, 2, 3].map(i => {
  const y = currentYear - i
  return { label: `${y}年`, value: y }
})
const PERIOD_OPTIONS = Array.from({ length: 12 }, (_, i) => ({ label: `第${i + 1}期`, value: i + 1 }))

const queryFields: ReportQueryField[] = [
  { key: 'fiscalYear', type: 'select', label: '会计年度', placeholder: '全部年度', options: YEAR_OPTIONS, width: 140 },
  { key: 'fiscalPeriod', type: 'select', label: '会计期间', placeholder: '全部期间', options: PERIOD_OPTIONS, width: 140 },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value })),
    width: 140
  }
]

// ═══ 表格列（与后端 VoucherDTO 字段一致） ═══
const columns: any[] = [
  { title: '凭证号', dataIndex: 'voucherNo', key: 'voucherNo', width: 130 },
  { title: '凭证日期', dataIndex: 'voucherDate', key: 'voucherDate', width: 110 },
  { title: '会计年度', dataIndex: 'fiscalYear', key: 'fiscalYear', width: 90, align: 'center' },
  { title: '会计期间', dataIndex: 'fiscalPeriod', key: 'fiscalPeriod', width: 90, align: 'center' },
  { title: '借方合计', dataIndex: 'totalDebit', key: 'totalDebit', width: 130, align: 'right' },
  { title: '贷方合计', dataIndex: 'totalCredit', key: 'totalCredit', width: 130, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90, align: 'center' },
  { title: '制单人', dataIndex: 'prepBy', key: 'prepBy', width: 100 },
  { title: '审核人', dataIndex: 'auditBy', key: 'auditBy', width: 100 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求：凭证分页列表（/erp/finance/voucher/list，page/size 风格） ═══
function fetcher(params: Record<string, any>) {
  return voucherApi.getPage(params)
}
</script>

<style scoped>
.gap-alert {
  margin-bottom: 16px;
}
</style>
