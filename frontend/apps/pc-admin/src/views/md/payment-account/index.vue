<template>
  <div>
    <a-alert
      type="info"
      show-icon
      message="支付账户对接后端财务账户接口（/api/erp/finance/account）"
      description="后端当前仅提供 列表/统计/启用停用 接口，暂无新增/编辑/删除端点，本页支持查询与启用停用，账户开立待后端补充端点后接入。"
      style="margin-bottom: 16px"
    />
    <ARReportPage
      ref="reportRef"
      title="支付账户"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      :stat-cards="statCards"
      export-file-name="支付账户"
      row-key="id"
      empty-text="暂无支付账户"
      @loaded="onLoaded"
    >
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.dataIndex === 'accountType'">
          <a-tag :color="ACCOUNT_TYPE_MAP[text]?.color">
            {{ ACCOUNT_TYPE_MAP[text]?.label || text || '-' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'accountLevel'">
          <a-tag :color="ACCOUNT_LEVEL_MAP[text]?.color">
            {{ ACCOUNT_LEVEL_MAP[text]?.label || '-' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'balance'">
          {{ formatMoney(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tag :color="text === 1 ? 'green' : 'default'">
            {{ text === 1 ? '启用' : '停用' }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'action'">
          <a-button
            type="link"
            size="small"
            @click="toggleStatus(record)"
          >
            {{ record.status === 1 ? '停用' : '启用' }}
          </a-button>
        </template>
      </template>
    </ARReportPage>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField, ReportFetchResult, StatCardItem } from '@/components/ARReportPage/types'
import { financeAccountApi, type FinanceAccountInfo } from '@/api/md'

// ═══ 账户类型/等级（与后端 FinanceAccount 注释一致） ═══
const ACCOUNT_TYPE_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '银行账户', color: 'blue' },
  2: { label: '现金账户', color: 'green' },
  3: { label: '内部账户', color: 'purple' },
  4: { label: '外部账户', color: 'orange' }
}

const ACCOUNT_LEVEL_MAP: Record<number, { label: string; color: string }> = {
  1: { label: '基本账户', color: 'gold' },
  2: { label: '一般账户', color: 'default' },
  3: { label: '专用账户', color: 'cyan' }
}

const queryFields: ReportQueryField[] = [
  {
    key: 'accountType',
    type: 'select',
    label: '账户类型',
    placeholder: '全部类型',
    options: Object.entries(ACCOUNT_TYPE_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))
  },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: [
      { label: '启用', value: 1 },
      { label: '停用', value: 0 }
    ]
  }
]

const columns: any[] = [
  { title: '账户名称', dataIndex: 'accountName', key: 'accountName', width: 160, ellipsis: true },
  { title: '账户类型', dataIndex: 'accountType', key: 'accountType', width: 100 },
  { title: '开户银行', dataIndex: 'bankName', key: 'bankName', width: 140, ellipsis: true },
  { title: '银行账号', dataIndex: 'bankAccount', key: 'bankAccount', width: 180, ellipsis: true },
  { title: '账户余额', dataIndex: 'balance', key: 'balance', width: 130, align: 'right' },
  { title: '币种', dataIndex: 'currency', key: 'currency', width: 70 },
  { title: '账户等级', dataIndex: 'accountLevel', key: 'accountLevel', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
  { title: '操作', dataIndex: 'action', key: 'action', width: 90, fixed: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求：GET /api/erp/finance/account/list（不分页，返回数组） ═══
function fetcher(params: Record<string, any>) {
  return financeAccountApi.getList({ status: params.status, accountType: params.accountType })
}

// ═══ 统计卡片（由当前列表数据汇总） ═══
const accountList = ref<FinanceAccountInfo[]>([])

const statCards = computed<StatCardItem[]>(() => {
  const list = accountList.value
  const totalBalance = list.reduce((acc, a) => acc + (Number(a.balance) || 0), 0)
  const enabledCount = list.filter(a => a.status === 1).length
  return [
    { label: '账户总数', value: list.length, suffix: '个' },
    { label: '启用账户', value: enabledCount, suffix: '个' },
    { label: '余额合计', value: totalBalance, precision: 2, prefix: '¥' }
  ]
})

function onLoaded(result: ReportFetchResult) {
  accountList.value = (result.list || []) as FinanceAccountInfo[]
}

// ═══ 启用/停用 ═══
const reportRef = ref<InstanceType<typeof ARReportPage> | null>(null)

async function toggleStatus(record: any) {
  const target = record.status === 1 ? 0 : 1
  try {
    await financeAccountApi.updateStatus(record.id, target)
    message.success(target === 1 ? '账户已启用' : '账户已停用')
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[支付账户] 状态切换失败', e)
  }
}
</script>
