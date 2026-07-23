<template>
  <ARReportPage
    ref="reportRef"
    title="积分流水"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="积分流水"
    row-key="id"
  >
    <template #bodyCell="{ column, record, text }">
      <template v-if="column.dataIndex === 'memberName'">
        <a @click="handleViewMember(record)">{{ text || '-' }}</a>
      </template>
      <template v-else-if="column.dataIndex === 'points'">
        <span :style="{ color: (text || 0) > 0 ? '#52c41a' : '#f5222d', fontWeight: 500 }">
          {{ (text || 0) > 0 ? '+' : '' }}{{ text ?? 0 }}
        </span>
      </template>
      <template v-else-if="column.dataIndex === 'type'">
        <a-tag :color="typeColor(text)">{{ typeLabel(text) }}</a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'createTime'">
        {{ text ? dayjs(text).format('YYYY-MM-DD HH:mm') : '-' }}
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import dayjs from 'dayjs'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { pointsApi } from '@/api/marketing'

const reportRef = ref<InstanceType<typeof ARReportPage>>()

const queryFields: ReportQueryField[] = [
  { key: 'memberId', type: 'input', label: '会员ID', placeholder: '会员ID', width: 160 },
  { key: 'type', type: 'select', label: '类型', placeholder: '全部类型', options: [
    { label: '获得', value: 'earn' },
    { label: '消费', value: 'spend' },
    { label: '过期', value: 'expire' },
    { label: '调整', value: 'adjust' }
  ]}
]

const columns: any[] = [
  { title: '会员', dataIndex: 'memberName', key: 'memberName', width: 120, ellipsis: true },
  { title: '积分变动', dataIndex: 'points', key: 'points', width: 100, align: 'right' },
  { title: '积分余额', dataIndex: 'balance', key: 'balance', width: 100, align: 'right' },
  { title: '类型', dataIndex: 'type', key: 'type', width: 80 },
  { title: '来源单号', dataIndex: 'bizNo', key: 'bizNo', width: 150 },
  { title: '说明', dataIndex: 'remark', key: 'remark', width: 200, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 150 }
]

function typeColor(type: string): string {
  const m: Record<string, string> = { earn: 'green', spend: 'blue', expire: 'orange', adjust: 'purple' }
  return m[type] || 'default'
}

function typeLabel(type: string): string {
  const m: Record<string, string> = { earn: '获得', spend: '消费', expire: '过期', adjust: '调整' }
  return m[type] || type
}

function fetcher(params: Record<string, any>) {
  return pointsApi.page(params)
}

function handleViewMember(record: any) {
  if (record.memberId) {
    window.open(`/member/profile?id=${record.memberId}`, '_blank')
  }
}
</script>
