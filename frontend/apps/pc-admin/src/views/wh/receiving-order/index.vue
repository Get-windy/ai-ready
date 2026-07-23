<template>
  <div>
    <ARReportPage
      title="收货单列表"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      export-file-name="收货单列表"
      row-key="id"
    >
      <template #headerExtra>
        <a-button type="primary" size="small" @click="handleAdd">新增收货单</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
        </template>
        <template v-else-if="['totalQuantity', 'receivedQuantity'].includes(String(column.dataIndex))">
          {{ formatQty(record[String(column.dataIndex)]) }}
        </template>
        <template v-else-if="column.dataIndex === 'sourceType'">
          {{ sourceTypeText(record.sourceType) }}
        </template>
        <template v-else-if="column.dataIndex === 'progress'">
          {{ formatQty(record.receivedQuantity) }} / {{ formatQty(record.totalQuantity) }}
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
import { receiptApi } from '@/api/wms/receipt'
import { WMS_STATUS_MAP, formatQty, formatTime } from '../whTask'
import { useRouter } from 'vue-router'

defineOptions({ name: 'WhReceivingOrderList' })

const router = useRouter()

const sourceTypeOptions = [
  { label: '采购入库', value: 0 }, { label: '生产入库', value: 1 },
  { label: '退货入库', value: 2 }, { label: '调拨入库', value: 3 }, { label: '其他', value: 4 },
]
function sourceTypeText(t: number) { return sourceTypeOptions.find(o => o.value === t)?.label || '其他' }
function statusText(s: number) { return WMS_STATUS_MAP[s]?.text || '未知' }
function statusColor(s: number) { return WMS_STATUS_MAP[s]?.color || 'default' }

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '单号/来源单号', placeholder: '单号或来源单号', width: 200 },
  { key: 'sourceType', type: 'select', label: '来源类型', placeholder: '全部类型', options: sourceTypeOptions, width: 150 },
  { key: 'status', type: 'select', label: '状态', placeholder: '全部状态', options: Object.entries(WMS_STATUS_MAP).map(([v, m]) => ({ label: m.text, value: Number(v) })), width: 150 },
  { key: 'warehouseId', type: 'input', label: '仓库ID', placeholder: '仓库ID', width: 130 },
]

const columns: any[] = [
  { title: '单号', dataIndex: 'taskNo', key: 'taskNo', width: 170 },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 110 },
  { title: '来源类型', dataIndex: 'sourceType', key: 'sourceType', width: 100 },
  { title: '来源单号', dataIndex: 'sourceOrderNo', key: 'sourceOrderNo', width: 150, ellipsis: true },
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 150, ellipsis: true },
  { title: '总数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 100, align: 'right' },
  { title: '已收货', dataIndex: 'receivedQuantity', key: 'receivedQuantity', width: 90, align: 'right' },
  { title: '进度', dataIndex: 'progress', key: 'progress', width: 130 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 80 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 140, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 130 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 160, fixed: 'right' },
]

async function fetcher(params: Record<string, any>) {
  const { page, ...rest } = params
  return receiptApi.page({ ...rest, current: page })
}

function handleAdd() {
  router.push('/wms/receive/form')
}

function handleEdit(record: any) {
  router.push('/wms/receive/form')
}

async function handleDelete(record: any) {
  try {
    await receiptApi.remove(record.id)
    message.success('删除成功')
  } catch (e: any) {
    message.error(e?.data?.message || '删除失败')
  }
}
</script>
