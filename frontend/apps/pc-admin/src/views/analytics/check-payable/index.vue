<template>
  <ARReportPage
    title="查应付"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    :stat-cards="agingCards"
    export-file-name="查应付"
    row-key="id"
  >
    <template #bodyCell="{ column, text }">
      <template v-if="column.dataIndex === 'status'">
        <a-tag :color="STATUS_MAP[text]?.color">
          {{ STATUS_MAP[text]?.label || text }}
        </a-tag>
      </template>
      <template v-else-if="['totalAmount', 'paidAmount', 'remainingAmount'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField, StatCardItem } from '@/components/ARReportPage/types'
import { payableApi } from '@/api/analytics'

// ═══ 应付状态（与后端 PayableServiceImpl 状态值一致） ═══
const STATUS_MAP: Record<string, { label: string; color: string }> = {
  normal: { label: '正常', color: 'green' },
  overdue: { label: '逾期', color: 'red' },
  partial: { label: '部分核销', color: 'orange' },
  written_off: { label: '已核销', color: 'blue' }
}

const queryFields: ReportQueryField[] = [
  { key: 'supplierId', type: 'input', label: '供应商ID', placeholder: '供应商ID', width: 160 },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value }))
  }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 150, ellipsis: true },
  { title: '来源单号', dataIndex: 'sourceNo', key: 'sourceNo', width: 150 },
  { title: '发票号', dataIndex: 'invoiceNo', key: 'invoiceNo', width: 130 },
  { title: '应付总额', dataIndex: 'totalAmount', key: 'totalAmount', width: 110, align: 'right' },
  { title: '已付金额', dataIndex: 'paidAmount', key: 'paidAmount', width: 110, align: 'right' },
  { title: '未付余额', dataIndex: 'remainingAmount', key: 'remainingAmount', width: 110, align: 'right' },
  { title: '开票日期', dataIndex: 'invoiceDate', key: 'invoiceDate', width: 110 },
  { title: '到期日', dataIndex: 'dueDate', key: 'dueDate', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '备注', dataIndex: 'remark', key: 'remark', ellipsis: true }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求 ═══
function fetcher(params: Record<string, any>) {
  return payableApi.getPage(params)
}

// ═══ 账龄汇总卡片（独立请求 /aging） ═══
interface AgingBucket {
  agingPeriod: string
  count: number
  totalAmount: number
}

const agingData = ref<AgingBucket[]>([])

const agingCards = computed<StatCardItem[]>(() => {
  const totalAmount = agingData.value.reduce((acc, b) => acc + (Number(b.totalAmount) || 0), 0)
  const totalCount = agingData.value.reduce((acc, b) => acc + (Number(b.count) || 0), 0)
  const cards: StatCardItem[] = [
    { label: '应付余额合计', value: totalAmount, precision: 2, prefix: '¥', suffix: `${totalCount} 笔` }
  ]
  for (const bucket of agingData.value) {
    cards.push({
      label: `账龄 ${bucket.agingPeriod}`,
      value: Number(bucket.totalAmount) || 0,
      precision: 2,
      prefix: '¥',
      suffix: `${bucket.count} 笔`
    })
  }
  return cards
})

onMounted(async () => {
  try {
    const res: any = await payableApi.getAging()
    agingData.value = Array.isArray(res) ? res : []
  } catch (e) {
    console.warn('[查应付] 账龄汇总获取失败', e)
  }
})
</script>
