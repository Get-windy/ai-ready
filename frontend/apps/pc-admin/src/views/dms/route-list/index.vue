<template>
  <ARReportPage
    ref="reportRef"
    title="线路列表"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    export-file-name="线路列表"
    row-key="id"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.dataIndex === 'status'">
        <a-tag :color="STATUS_MAP[record.status]?.color || 'default'">
          {{ STATUS_MAP[record.status]?.label || record.status || '-' }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'progress'">
        {{ record.completedPoints ?? 0 }} / {{ record.totalPoints ?? 0 }}
      </template>
      <template v-else-if="column.dataIndex === 'totalDistance'">
        {{ record.totalDistance != null ? `${Number(record.totalDistance).toLocaleString('zh-CN')} km` : '-' }}
      </template>
      <template v-else-if="column.dataIndex === 'totalDuration'">
        {{ record.totalDuration != null ? `${record.totalDuration} 分钟` : '-' }}
      </template>
      <template v-else-if="column.key === 'action'">
        <a-space :size="4">
          <a-button
            v-if="record.status === 'PLANNING' || record.status === 'READY'"
            type="link"
            size="small"
            @click="handleStart(record as any)"
          >
            开始配送
          </a-button>
          <a-button
            v-if="record.status === 'IN_PROGRESS'"
            type="link"
            size="small"
            @click="handleComplete(record as any)"
          >
            完成
          </a-button>
          <a-popconfirm
            v-if="record.status !== 'COMPLETED' && record.status !== 'CANCELLED'"
            title="确定取消该配送路线？"
            @confirm="handleCancel(record as any)"
          >
            <a-button
              type="link"
              size="small"
              danger
            >
              取消
            </a-button>
          </a-popconfirm>
        </a-space>
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { deliveryRouteApi, type DeliveryRoute } from '@/api/dms/route'

// ═══ 路线状态（与后端 RouteStatus 枚举一致） ═══
const STATUS_MAP: Record<string, { label: string; color: string }> = {
  PLANNING: { label: '规划中', color: 'default' },
  READY: { label: '待出发', color: 'orange' },
  IN_PROGRESS: { label: '配送中', color: 'processing' },
  COMPLETED: { label: '已完成', color: 'green' },
  CANCELLED: { label: '已取消', color: 'red' }
}

const queryFields: ReportQueryField[] = [
  { key: 'deliveryPersonId', type: 'input', label: '配送员ID', placeholder: '配送员ID', width: 140 },
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
  { title: '路线编号', dataIndex: 'routeCode', key: 'routeCode', width: 160 },
  { title: '配送员', dataIndex: 'deliveryPersonName', key: 'deliveryPersonName', width: 110 },
  { title: '完成进度', dataIndex: 'progress', key: 'progress', width: 100, align: 'center' },
  { title: '起点', dataIndex: 'startPoint', key: 'startPoint', width: 160, ellipsis: true },
  { title: '终点', dataIndex: 'endPoint', key: 'endPoint', width: 160, ellipsis: true },
  { title: '总里程', dataIndex: 'totalDistance', key: 'totalDistance', width: 100, align: 'right' },
  { title: '预计时长', dataIndex: 'totalDuration', key: 'totalDuration', width: 100, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 160 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' }
]

const reportRef = ref<any>(null)

// ═══ 数据请求（后端 /api/delivery/route/list 返回 { list, total }） ═══
function fetcher(params: Record<string, any>) {
  return deliveryRouteApi.list(params)
}

// ═══ 路线操作 ═══
async function handleStart(record: DeliveryRoute) {
  try {
    await deliveryRouteApi.start(record.id)
    message.success(`路线 ${record.routeCode} 已开始配送`)
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[线路列表] 开始配送失败', e)
  }
}

async function handleComplete(record: DeliveryRoute) {
  try {
    await deliveryRouteApi.complete(record.id)
    message.success(`路线 ${record.routeCode} 已完成`)
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[线路列表] 完成配送失败', e)
  }
}

async function handleCancel(record: DeliveryRoute) {
  try {
    await deliveryRouteApi.cancel(record.id)
    message.success(`路线 ${record.routeCode} 已取消`)
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[线路列表] 取消路线失败', e)
  }
}
</script>
