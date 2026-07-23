<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="借进借出查询"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      :stat-cards="statCards"
      export-file-name="借进借出查询"
      row-key="id"
      empty-text="暂无借进借出单据"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'direction'">
          <a-tag :color="record.direction === 1 ? 'green' : 'orange'">{{ record.direction === 1 ? '借进' : '借出' }}</a-tag>
        </template>
        <template v-else-if="['totalQuantity', 'returnedQuantity'].includes(String(column.dataIndex))">
          {{ formatQty(record[String(column.dataIndex)]) }}
        </template>
        <template v-else-if="column.dataIndex === 'returnProgress'">
          <a-progress
            :percent="returnPercent(record)"
            size="small"
            style="width: 120px"
            :format="() => `${formatQty(record.returnedQuantity)} / ${formatQty(record.totalQuantity)}`"
          />
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatTime(record.createTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-button type="link" size="small" @click="viewDetail(record)">查看明细</a-button>
        </template>
      </template>
    </ARReportPage>

    <a-modal v-model:open="detailOpen" :title="detailTitle" width="800px" :footer="null">
      <a-table :columns="detailColumns" :data-source="detailItems" :loading="detailLoading"
        row-key="id" size="small" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'quantity' || column.dataIndex === 'returnedQuantity'">
            {{ formatQty(record[column.dataIndex]) }}
          </template>
          <template v-else-if="column.dataIndex === 'amount'">
            ¥{{ (record.amount || 0).toFixed(2) }}
          </template>
        </template>
      </a-table>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField, StatCardItem } from '@/components/ARReportPage/types'
import { borrowApi } from '@/api/wms/borrow'
import { formatQty, formatTime } from '../whTask'

defineOptions({ name: 'WhBorrowQuery' })

// ═══ 字典 ═══
const DIRECTION_OPTIONS = [
  { label: '借进', value: 1 }, { label: '借出', value: 2 },
]
const BORROW_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '草稿', color: 'default' },
  1: { text: '待审批', color: 'orange' },
  2: { text: '已审批', color: 'blue' },
  3: { text: '部分归还', color: 'gold' },
  4: { text: '已归还', color: 'green' },
  5: { text: '已取消', color: 'red' },
}
const STATUS_OPTIONS = Object.entries(BORROW_STATUS_MAP).map(([v, m]) => ({ label: m.text, value: Number(v) }))
function statusText(s: number) { return BORROW_STATUS_MAP[s]?.text || '未知' }
function statusColor(s: number) { return BORROW_STATUS_MAP[s]?.color || 'default' }
function returnPercent(r: any) {
  const total = Number(r.totalQuantity) || 0
  if (total <= 0) return 0
  return Math.min(100, Math.round(((Number(r.returnedQuantity) || 0) / total) * 100))
}

// ═══ 统计卡（由 loaded 事件更新） ═══
const totalData = ref({ borrowIn: 0, borrowOut: 0, overdue: 0, monthAmount: 0 })
const statCards = computed<StatCardItem[]>(() => [
  { label: '借进未还笔数', value: totalData.value.borrowIn, prefix: '' },
  { label: '借出未还笔数', value: totalData.value.borrowOut, prefix: '' },
  { label: '逾期未还', value: totalData.value.overdue, prefix: '', valueStyle: totalData.value.overdue > 0 ? 'color:red' : undefined },
  { label: '本月借进额', value: `¥${totalData.value.monthAmount.toFixed(2)}`, prefix: '' },
])

// ═══ 查询字段 ═══
const queryFields: ReportQueryField[] = [
  { key: 'direction', type: 'select', label: '方向', placeholder: '全部方向', options: DIRECTION_OPTIONS, width: 130 },
  { key: 'keyword', type: 'input', label: '单号/往来单位', placeholder: '单号或往来单位', width: 200 },
  { key: 'status', type: 'select', label: '状态', placeholder: '全部状态', options: STATUS_OPTIONS, width: 150 },
  { key: 'borrowDateRange', type: 'date-range', label: '日期', startKey: 'borrowDateStart', endKey: 'borrowDateEnd', width: 260 },
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '单号', dataIndex: 'orderNo', key: 'orderNo', width: 170 },
  { title: '方向', dataIndex: 'direction', key: 'direction', width: 80 },
  { title: '往来单位', dataIndex: 'partnerName', key: 'partnerName', width: 170, ellipsis: true },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 110 },
  { title: '日期', dataIndex: 'borrowDate', key: 'borrowDate', width: 110 },
  { title: '预计归还日', dataIndex: 'expectedReturnDate', key: 'expectedReturnDate', width: 110 },
  { title: '总数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 100, align: 'right' },
  { title: '已归还', dataIndex: 'returnedQuantity', key: 'returnedQuantity', width: 90, align: 'right' },
  { title: '归还进度', dataIndex: 'returnProgress', key: 'returnProgress', width: 200 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 140, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 130 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 100, fixed: 'right' },
]

// ═══ 数据请求 ═══
async function fetcher(params: Record<string, any>) {
  const { page, borrowDateStart, borrowDateEnd, ...rest } = params
  const res: any = await borrowApi.page({ ...rest, current: page })
  // 更新统计卡
  if (Array.isArray(res?.records)) {
    const now = new Date()
    const overdueRows = res.records.filter((r: any) =>
      r.status < 4 && r.expectedReturnDate && r.expectedReturnDate < now.toISOString().slice(0, 10)
    )
    const inRows = res.records.filter((r: any) => r.direction === 1 && r.status < 4)
    const outRows = res.records.filter((r: any) => r.direction === 2 && r.status < 4)
    totalData.value = {
      borrowIn: inRows.length,
      borrowOut: outRows.length,
      overdue: overdueRows.length,
      monthAmount: inRows.reduce((s: number, r: any) => s + Number(r.totalQuantity || 0) * 0, 0),
    }
  }
  return res
}

// ═══ 明细弹窗 ═══
const detailOpen = ref(false)
const detailTitle = ref('')
const detailItems = ref<any[]>([])
const detailLoading = ref(false)
const detailColumns = [
  { title: '行号', dataIndex: 'lineNo', key: 'lineNo', width: 60 },
  { title: '商品', dataIndex: 'productName', key: 'productName', width: 150, ellipsis: true },
  { title: '规格', dataIndex: 'productSpec', key: 'productSpec', width: 100 },
  { title: '单位', dataIndex: 'unit', key: 'unit', width: 60 },
  { title: '数量', dataIndex: 'quantity', key: 'quantity', width: 80, align: 'right' },
  { title: '单价', dataIndex: 'price', key: 'price', width: 80, align: 'right' },
  { title: '金额', dataIndex: 'amount', key: 'amount', width: 100, align: 'right' },
  { title: '已归还', dataIndex: 'returnedQuantity', key: 'returnedQuantity', width: 80, align: 'right' },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 120, ellipsis: true },
]

async function viewDetail(record: any) {
  detailTitle.value = `明细 - ${record.orderNo}`
  detailLoading.value = true
  detailOpen.value = true
  try {
    const vo: any = await borrowApi.getById(record.id)
    detailItems.value = vo?.items || []
  } catch {
    detailItems.value = []
  } finally {
    detailLoading.value = false
  }
}
</script>
