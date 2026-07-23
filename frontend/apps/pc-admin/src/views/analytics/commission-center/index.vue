<template>
  <div class="commission-center">
    <div class="tabs-bar">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane
          key="records"
          tab="提成记录"
        />
        <a-tab-pane
          key="rules"
          tab="提成规则"
        />
      </a-tabs>
    </div>
    <ARReportPage
      :key="activeTab"
      title="业绩提成中心"
      :columns="activeTab === 'records' ? recordColumns : ruleColumns"
      :fetcher="activeTab === 'records' ? recordFetcher : ruleFetcher"
      page-param-style="pageNum"
      :export-file-name="activeTab === 'records' ? '提成记录' : '提成规则'"
      row-key="id"
    >
      <template #bodyCell="{ column, text }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="statusColor(activeTab, text)">
            {{ statusLabel(activeTab, text) }}
          </a-tag>
        </template>
        <template v-else-if="['orderAmount', 'commissionAmount', 'minOrderAmount', 'maxCommission'].includes(column.dataIndex as string)">
          {{ formatMoney(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'commissionRate'">
          {{ text !== null && text !== undefined ? `${text}%` : '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'tierLevel'">
          {{ TIER_MAP[text] || text || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'commissionType'">
          {{ COMMISSION_TYPE_MAP[text] || text || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'calcBasis'">
          {{ CALC_BASIS_MAP[text] || text || '-' }}
        </template>
        <template v-else-if="column.dataIndex === 'calcMethod'">
          {{ CALC_METHOD_MAP[text] || text || '-' }}
        </template>
      </template>
    </ARReportPage>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import { commissionApi } from '@/api/analytics'

const activeTab = ref<'records' | 'rules'>('records')

// ═══ 提成记录状态（与后端 CommissionRecord 注释一致） ═══
const RECORD_STATUS_MAP: Record<string, { label: string; color: string }> = {
  DRAFT: { label: '待确认', color: 'orange' },
  CONFIRMED: { label: '已确认', color: 'blue' },
  PAID: { label: '已结算', color: 'green' },
  CANCELLED: { label: '已取消', color: 'red' }
}

// ═══ 提成规则状态（启用/禁用/草稿） ═══
const RULE_STATUS_MAP: Record<string, { label: string; color: string }> = {
  ENABLED: { label: '启用', color: 'green' },
  ACTIVE: { label: '启用', color: 'green' },
  DISABLED: { label: '禁用', color: 'red' },
  INACTIVE: { label: '禁用', color: 'red' },
  DRAFT: { label: '草稿', color: 'default' }
}

const TIER_MAP: Record<number, string> = { 1: '一级', 2: '二级', 3: '三级' }
const COMMISSION_TYPE_MAP: Record<string, string> = { SALE: '销售佣金', PROMOTION: '推广佣金', SHARE: '分享佣金' }
const CALC_BASIS_MAP: Record<string, string> = { AMOUNT: '按金额', QUANTITY: '按数量', PROFIT: '按利润' }
const CALC_METHOD_MAP: Record<string, string> = { PERCENT: '百分比', FIXED: '固定金额' }

function statusLabel(tab: string, status: string): string {
  const map = tab === 'records' ? RECORD_STATUS_MAP : RULE_STATUS_MAP
  return map[status]?.label || status || '-'
}

function statusColor(tab: string, status: string): string {
  const map = tab === 'records' ? RECORD_STATUS_MAP : RULE_STATUS_MAP
  return map[status]?.color || 'blue'
}

// ═══ 提成记录列 ═══
const recordColumns: any[] = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '推荐人ID', dataIndex: 'partnerId', key: 'partnerId', width: 100 },
  { title: '订单ID', dataIndex: 'orderId', key: 'orderId', width: 100 },
  { title: '订单金额', dataIndex: 'orderAmount', key: 'orderAmount', width: 120, align: 'right' },
  { title: '佣金比例', dataIndex: 'commissionRate', key: 'commissionRate', width: 100, align: 'right' },
  { title: '佣金金额', dataIndex: 'commissionAmount', key: 'commissionAmount', width: 120, align: 'right' },
  { title: '层级', dataIndex: 'tierLevel', key: 'tierLevel', width: 80 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '确认时间', dataIndex: 'confirmTime', key: 'confirmTime', width: 160 },
  { title: '结算时间', dataIndex: 'payTime', key: 'payTime', width: 160 },
  { title: '备注', dataIndex: 'remark', key: 'remark', width: 120, ellipsis: true },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 }
]

// ═══ 提成规则列 ═══
const ruleColumns: any[] = [
  { title: '规则编码', dataIndex: 'ruleCode', key: 'ruleCode', width: 120 },
  { title: '规则名称', dataIndex: 'ruleName', key: 'ruleName', width: 160, ellipsis: true },
  { title: '佣金类型', dataIndex: 'commissionType', key: 'commissionType', width: 100 },
  { title: '计算基础', dataIndex: 'calcBasis', key: 'calcBasis', width: 90 },
  { title: '计算方式', dataIndex: 'calcMethod', key: 'calcMethod', width: 90 },
  { title: '佣金值', dataIndex: 'commissionValue', key: 'commissionValue', width: 100, align: 'right' },
  { title: '最低订单金额', dataIndex: 'minOrderAmount', key: 'minOrderAmount', width: 120, align: 'right' },
  { title: '单笔佣金上限', dataIndex: 'maxCommission', key: 'maxCommission', width: 120, align: 'right' },
  { title: '适用产品', dataIndex: 'applicableProducts', key: 'applicableProducts', width: 110, ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 160 }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求 ═══
function recordFetcher(params: Record<string, any>) {
  return commissionApi.recordPage(params)
}

function ruleFetcher(params: Record<string, any>) {
  return commissionApi.rulePage(params)
}
</script>

<style scoped>
.tabs-bar {
  background: #fff;
  padding: 0 20px;
  border-radius: 8px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.tabs-bar :deep(.ant-tabs-nav) {
  margin-bottom: 0;
}
</style>
