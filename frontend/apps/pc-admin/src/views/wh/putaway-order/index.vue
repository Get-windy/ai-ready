<template>
  <div>
    <ARReportPage
      title="上架单列表"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      export-file-name="上架单列表"
      row-key="id"
    >
      <template #headerExtra>
        <a-button type="primary" size="small" @click="handleAdd">新增上架单</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
        </template>
        <template v-else-if="['totalQuantity', 'putawayQuantity'].includes(String(column.dataIndex))">
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
import { putawayApi } from '@/api/wms/putaway'
import { WMS_STATUS_MAP, formatQty, formatTime } from '../whTask'
import { useRouter } from 'vue-router'

defineOptions({ name: 'WhPutawayOrderList' })

const router = useRouter()
const STATUS_OPTIONS = Object.entries(WMS_STATUS_MAP).map(([v, m]) => ({ label: m.text, value: Number(v) }))
function statusText(s: number) { return WMS_STATUS_MAP[s]?.text || '未知' }
function statusColor(s: number) { return WMS_STATUS_MAP[s]?.color || 'default' }

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '单号', placeholder: '单号', width: 200 },
  { key: 'sourceType', type: 'input', label: '来源类型', placeholder: '来源类型', width: 130 },
  { key: 'status', type: 'select', label: '状态', placeholder: '全部状态', options: STATUS_OPTIONS, width: 150 },
]

const columns: any[] = [
  { title: '单号', dataIndex: 'taskNo', key: 'taskNo', width: 170 },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 110 },
  { title: '来源类型', dataIndex: 'sourceType', key: 'sourceType', width: 100 },
  { title: '总数量', dataIndex: 'totalQuantity', key: 'totalQuantity', width: 100, align: 'right' },
  { title: '已上架', dataIndex: 'putawayQuantity', key: 'putawayQuantity', width: 90, align: 'right' },
  { title: '执行人', dataIndex: 'assigneeName', key: 'assigneeName', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 140, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 130 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 160, fixed: 'right' },
]

async function fetcher(params: Record<string, any>) {
  const { page, ...rest } = params
  return putawayApi.page({ ...rest, current: page })
}

function handleAdd() {
  router.push('/wms/putaway/form')
}

function handleEdit(record: any) {
  router.push('/wms/putaway/form')
}

async function handleDelete(record: any) {
  try {
    await putawayApi.remove(record.id)
    message.success('删除成功')
  } catch (e: any) {
    message.error(e?.data?.message || '删除失败')
  }
}
</script>
