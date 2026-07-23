<template>
  <ARReportPage
    ref="reportRef"
    title="买家账号"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="买家账号"
    row-key="id"
    empty-text="暂无买家账号"
  >
    <template #bodyCell="{ column, text, record }">
      <template v-if="column.dataIndex === 'auditStatus'">
        <a-tag :color="AUDIT_STATUS_MAP[text]?.color">
          {{ AUDIT_STATUS_MAP[text]?.label || '未知' }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'userType'">
        {{ text === 'ENTERPRISE' ? '企业客户' : text === 'MEMBER' ? '个人会员' : '-' }}
      </template>
      <template v-else-if="column.dataIndex === 'status'">
        <a-tag :color="text === 1 ? 'green' : 'red'">
          {{ text === 1 ? '正常' : '已禁用' }}
        </a-tag>
      </template>
      <template v-else-if="column.dataIndex === 'action'">
        <a-popconfirm
          :title="record.status === 1 ? '确认禁用该买家账号？禁用后无法登录商城。' : '确认启用该买家账号？'"
          :ok-text="record.status === 1 ? '禁用' : '启用'"
          cancel-text="取消"
          @confirm="toggleStatus(record as ShopUser)"
        >
          <a-button
            type="link"
            size="small"
            :danger="record.status === 1"
            :style="record.status === 1 ? '' : 'color: #52c41a'"
          >
            {{ record.status === 1 ? '禁用' : '启用' }}
          </a-button>
        </a-popconfirm>
      </template>
    </template>
  </ARReportPage>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { shopUserApi, type ShopUser } from '@/api/erp/mall'

defineOptions({ name: 'MallBuyerAccount' })

// ═══ 审核状态（与后端 ShopUser.auditStatus 一致：0待审核 1通过 2驳回） ═══
const AUDIT_STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '待审核', color: 'orange' },
  1: { label: '已通过', color: 'green' },
  2: { label: '已驳回', color: 'red' }
}

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '用户名/昵称/手机号/公司' },
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
    label: '账号状态',
    placeholder: '全部',
    options: [
      { label: '正常', value: 1 },
      { label: '已禁用', value: 0 }
    ]
  }
]

const columns: any[] = [
  { title: '用户名', dataIndex: 'username', key: 'username', width: 130 },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 120, ellipsis: true },
  { title: '公司', dataIndex: 'companyName', key: 'companyName', width: 170, ellipsis: true },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '身份', dataIndex: 'userType', key: 'userType', width: 100 },
  { title: '来源', dataIndex: 'source', key: 'source', width: 90 },
  { title: '审核状态', dataIndex: 'auditStatus', key: 'auditStatus', width: 100 },
  { title: '最后登录', dataIndex: 'lastLoginTime', key: 'lastLoginTime', width: 160 },
  { title: '账号状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '注册时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 90, fixed: 'right' }
]

// ═══ 数据请求 ═══
function fetcher(params: Record<string, any>) {
  return shopUserApi.page(params)
}

// ═══ 启用/禁用 ═══
const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)

async function toggleStatus(record: ShopUser) {
  const target = record.status === 1 ? 0 : 1
  try {
    await shopUserApi.toggleStatus(record.id, target)
    message.success(target === 1 ? `买家「${record.nickname || record.username}」已启用` : `买家「${record.nickname || record.username}」已禁用`)
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[买家账号] 状态切换失败', e)
  }
}
</script>
