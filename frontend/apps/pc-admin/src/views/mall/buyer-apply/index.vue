<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="买家申请管理"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      :stat-cards="statCards"
      page-param-style="pageNum"
      export-file-name="买家申请"
      row-key="id"
      empty-text="暂无买家申请"
      @loaded="refreshCounts"
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
        <template v-else-if="column.dataIndex === 'action'">
          <a-space v-if="record.auditStatus === 0">
            <a-popconfirm
              title="确认通过该买家的注册申请？"
              ok-text="通过"
              cancel-text="取消"
              @confirm="handleApprove(record as ShopUser)"
            >
              <a-button
                type="link"
                size="small"
                style="color: #52c41a"
              >
                通过
              </a-button>
            </a-popconfirm>
            <a-button
              type="link"
              size="small"
              danger
              @click="openReject(record as ShopUser)"
            >
              驳回
            </a-button>
          </a-space>
          <span v-else>-</span>
        </template>
      </template>
    </ARReportPage>

    <!-- 驳回原因弹窗 -->
    <a-modal
      v-model:open="rejectVisible"
      title="驳回买家申请"
      :confirm-loading="rejectLoading"
      @ok="handleRejectConfirm"
    >
      <a-textarea
        v-model:value="rejectReason"
        placeholder="请输入驳回原因（必填，将展示给买家）"
        :rows="3"
        :maxlength="500"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField, StatCardItem } from '@/components/ARReportPage/types'
import { shopUserApi, type ShopUser } from '@/api/erp/mall'

defineOptions({ name: 'MallBuyerApply' })

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
  }
]

const columns: any[] = [
  { title: '用户名', dataIndex: 'username', key: 'username', width: 130 },
  { title: '昵称', dataIndex: 'nickname', key: 'nickname', width: 120, ellipsis: true },
  { title: '公司', dataIndex: 'companyName', key: 'companyName', width: 180, ellipsis: true },
  { title: '手机号', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '身份', dataIndex: 'userType', key: 'userType', width: 100 },
  { title: '来源', dataIndex: 'source', key: 'source', width: 90 },
  { title: '审核状态', dataIndex: 'auditStatus', key: 'auditStatus', width: 100 },
  { title: '驳回原因', dataIndex: 'rejectReason', key: 'rejectReason', width: 150, ellipsis: true },
  { title: '申请时间', dataIndex: 'createTime', key: 'createTime', width: 160 },
  { title: '审核时间', dataIndex: 'auditTime', key: 'auditTime', width: 160 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 120, fixed: 'right' }
]

// ═══ 数据请求 ═══
function fetcher(params: Record<string, any>) {
  return shopUserApi.page(params)
}

// ═══ 审核统计卡片（真实分页接口 total 计数） ═══
const counts = ref({ pending: 0, approved: 0, rejected: 0 })

const statCards = computed<StatCardItem[]>(() => [
  { label: '待审核', value: counts.value.pending, suffix: '人', valueStyle: { color: '#fa8c16' } },
  { label: '已通过', value: counts.value.approved, suffix: '人', valueStyle: { color: '#52c41a' } },
  { label: '已驳回', value: counts.value.rejected, suffix: '人', valueStyle: { color: '#f5222d' } }
])

async function refreshCounts() {
  try {
    const [p, a, r] = await Promise.all([
      shopUserApi.page({ pageNum: 1, pageSize: 1, auditStatus: 0 }),
      shopUserApi.page({ pageNum: 1, pageSize: 1, auditStatus: 1 }),
      shopUserApi.page({ pageNum: 1, pageSize: 1, auditStatus: 2 })
    ])
    counts.value = {
      pending: Number(p?.total) || 0,
      approved: Number(a?.total) || 0,
      rejected: Number(r?.total) || 0
    }
  } catch (e) {
    console.warn('[买家申请管理] 审核统计获取失败', e)
  }
}

// ═══ 审核动作 ═══
const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)

async function handleApprove(record: ShopUser) {
  try {
    await shopUserApi.approve(record.id)
    message.success(`买家「${record.nickname || record.username}」已审核通过`)
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[买家申请管理] 审核通过失败', e)
  }
}

const rejectVisible = ref(false)
const rejectLoading = ref(false)
const rejectReason = ref('')
const rejectTarget = ref<ShopUser | null>(null)

function openReject(record: ShopUser) {
  rejectTarget.value = record
  rejectReason.value = ''
  rejectVisible.value = true
}

async function handleRejectConfirm() {
  if (!rejectReason.value.trim()) {
    message.warning('请输入驳回原因')
    return
  }
  if (!rejectTarget.value) return
  rejectLoading.value = true
  try {
    await shopUserApi.reject(rejectTarget.value.id, rejectReason.value.trim())
    message.success(`买家「${rejectTarget.value.nickname || rejectTarget.value.username}」已驳回`)
    rejectVisible.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[买家申请管理] 驳回失败', e)
  } finally {
    rejectLoading.value = false
  }
}
</script>
