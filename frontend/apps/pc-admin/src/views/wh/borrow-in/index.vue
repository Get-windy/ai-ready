<template>
  <div>
    <ARReportPage
      title="借进单列表"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      export-file-name="借进单列表"
      row-key="id"
    >
      <template #headerExtra>
        <a-button type="primary" size="small" @click="handleAdd">新增借进单</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
        </template>
        <template v-else-if="['totalQuantity', 'returnedQuantity'].includes(String(column.dataIndex))">
          {{ formatQty(record[String(column.dataIndex)]) }}
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatTime(record.createTime) }}
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-space :size="4">
            <a-button size="small" type="link" @click="handleEdit(record)">查看</a-button>
            <a-button v-if="record.status === 0" size="small" type="link" @click="handleEdit(record)">编辑</a-button>
            <a-popconfirm v-if="record.status === 0" title="确认删除该单据？" @confirm="handleDelete(record)">
              <a-button size="small" type="link" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </ARReportPage>
  </div>
</template>

<script setup lang="ts">
import { message } from 'ant-design-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { borrowApi } from '@/api/wms/borrow'
import { formatQty, formatTime } from '../whTask'
import { useRouter } from 'vue-router'

defineOptions({ name: 'WhBorrowInList' })

const router = useRouter()
const DIRECTION = 1 // 借进

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

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '单号/往来单位', placeholder: '单号或往来单位', width: 200 },
  { key: 'status', type: 'select', label: '状态', placeholder: '全部状态', options: STATUS_OPTIONS, width: 150 },
  { key: 'warehouseId', type: 'input', label: '仓库ID', placeholder: '仓库ID', width: 130 },
  { key: 'borrowDateRange', type: 'date-range', label: '借进日期', startKey: 'borrowDateStart', endKey: 'borrowDateEnd', width: 260 },
]

const columns: any[] = [
  { title: '单号', dataIndex: 'orderNo', key: 'orderNo', width: 170 },
  { title: '往来单位', dataIndex: 'partnerName', key: 'partnerName', width: 170, ellipsis: true },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 110 },
  { title: '借进日期', dataIndex: 'borrowDate', key: 'borrowDate', width: 120 },
  { title: '预计归还日', dataIndex: 'expectedReturnDate', key: 'expectedReturnDate', width: 110 },
  { title: '总数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 100, align: 'right' },
  { title: '已归还', dataIndex: 'returnedQuantity', key: 'returnedQuantity', width: 90, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 140, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 130 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 160, fixed: 'right' },
]

async function fetcher(params: Record<string, any>) {
  const { page, ...rest } = params
  return borrowApi.page({ ...rest, direction: DIRECTION, current: page })
}

function handleAdd() {
  router.push('/wms/borrow-in/form')
}

function handleEdit(record: any) {
  router.push('/wms/borrow-in/form')
}

async function handleDelete(record: any) {
  try {
    await borrowApi.remove(record.id)
    message.success('删除成功')
  } catch (e: any) {
    message.error(e?.data?.message || '删除失败')
  }
}
</script>
