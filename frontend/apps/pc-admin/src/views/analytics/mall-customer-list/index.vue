<template>
  <ARReportPage
    title="商城客户列表"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="商城客户列表"
    row-key="id"
  >
    <template #bodyCell="{ column, text }">
      <template v-if="column.dataIndex === 'auditStatus'">
        <a-tag :color="AUDIT_STATUS_MAP[text]?.color || 'default'">
          {{ AUDIT_STATUS_MAP[text]?.label || '未知' }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'status'">
        <a-tag :color="text === 1 ? 'green' : 'red'">
          {{ text === 1 ? '正常' : '禁用' }}
        </a-tag>
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { shopUserApi } from '@/api/analytics'

// ═══ 审核状态（与后端 MallAdminController user/page 注释一致） ═══
const AUDIT_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待审核', color: 'orange' },
  1: { label: '已通过', color: 'green' },
  2: { label: '已驳回', color: 'red' }
}

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '用户名/昵称/手机号/公司', width: 220 },
  {
    key: 'auditStatus',
    type: 'select',
    label: '审核状态',
    placeholder: '全部',
    options: Object.entries(AUDIT_STATUS_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))
  },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部',
    options: [
      { label: '正常', value: 1 },
      { label: '禁用', value: 0 }
    ]
  }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '用户名', dataIndex: 'username', key: 'username', width: 130 },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 120 },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '公司', dataIndex: 'companyName', key: 'companyName', width: 170, ellipsis: true },
  { title: '来源', dataIndex: 'source', key: 'source', width: 90 },
  { title: '审核状态', dataIndex: 'auditStatus', key: 'auditStatus', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '最近登录', dataIndex: 'lastLoginTime', key: 'lastLoginTime', width: 160 },
  { title: '注册时间', dataIndex: 'createTime', key: 'createTime', width: 160 }
]

// ═══ 数据请求 ═══
function fetcher(params: Record<string, any>) {
  return shopUserApi.page(params)
}
</script>
