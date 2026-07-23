<template>
  <div>
    <ARReportPage
      title="盘点作业单列表"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      export-file-name="盘点作业单列表"
      row-key="id"
    >
      <template #headerExtra>
        <a-button type="primary" size="small" @click="handleAdd">新增盘点单</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
        </template>
        <template v-else-if="['totalItems', 'checkedItems', 'diffItems'].includes(String(column.dataIndex))">
          {{ formatQty(record[String(column.dataIndex)]) }}
        </template>
        <template v-else-if="column.dataIndex === 'checkType'">
          {{ checkTypeText(record.checkType) }}
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
import { checkApi } from '@/api/wms/check'
import { CHECK_STATUS_MAP, formatQty, formatTime } from '../whTask'
import { useRouter } from 'vue-router'

defineOptions({ name: 'WhInventoryOrderList' })

const router = useRouter()
const STATUS_OPTIONS = Object.entries(CHECK_STATUS_MAP).map(([v, m]) => ({ label: m.text, value: Number(v) }))
function statusText(s: number) { return CHECK_STATUS_MAP[s]?.text || '未知' }
function statusColor(s: number) { return CHECK_STATUS_MAP[s]?.color || 'default' }

const checkTypeOptions = [
  { label: '全盘', value: 0 }, { label: '抽盘', value: 1 }, { label: '动态盘点', value: 2 },
]
function checkTypeText(t: number) { return checkTypeOptions.find(o => o.value === t)?.label || '其他' }

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '单号', placeholder: '单号', width: 200 },
  { key: 'status', type: 'select', label: '状态', placeholder: '全部状态', options: STATUS_OPTIONS, width: 150 },
  { key: 'warehouseId', type: 'input', label: '仓库ID', placeholder: '仓库ID', width: 130 },
]

const columns: any[] = [
  { title: '单号', dataIndex: 'taskNo', key: 'taskNo', width: 170 },
  { title: '仓库', dataIndex: 'warehouseName', key: 'warehouseName', width: 110 },
  { title: '盘点类型', dataIndex: 'checkType', key: 'checkType', width: 100 },
  { title: '总项数', dataIndex: 'totalItems', key: 'totalItems', width: 80, align: 'right' },
  { title: '已盘点', dataIndex: 'checkedItems', key: 'checkedItems', width: 80, align: 'right' },
  { title: '差异数', dataIndex: 'diffItems', key: 'diffItems', width: 80, align: 'right' },
  { title: '盘点人', dataIndex: 'assigneeName', key: 'assigneeName', width: 100 },
  { title: '审核人', dataIndex: 'checkerName', key: 'checkerName', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 140, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 130 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 160, fixed: 'right' },
]

async function fetcher(params: Record<string, any>) {
  const { page, ...rest } = params
  return checkApi.page({ ...rest, current: page })
}

function handleAdd() {
  router.push('/wms/check/form')
}

function handleEdit(record: any) {
  router.push('/wms/check/form')
}

async function handleDelete(record: any) {
  try {
    await checkApi.remove(record.id)
    message.success('删除成功')
  } catch (e: any) {
    message.error(e?.data?.message || '删除失败')
  }
}
</script>
