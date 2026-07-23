<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="预算编制"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      export-file-name="预算编制"
      row-key="id"
    >
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="STATUS_MAP[text]?.color || 'default'">
            {{ STATUS_MAP[text]?.label || text }}
          </a-tag>
        </template>
        <template v-else-if="['totalAmount', 'totalUsedAmount', 'totalRemainingAmount'].includes(column.dataIndex as string)">
          {{ formatMoney(text) }}
        </template>
        <template v-else-if="column.dataIndex === 'executionRate'">
          {{ formatRate(text) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space :size="4">
            <a-button
              type="link"
              size="small"
              @click="openDetail(record)"
            >
              明细
            </a-button>
            <a-popconfirm
              v-if="record.status === 'draft'"
              title="确认提交该预算进入审批？"
              @confirm="handleAction(record, 'submit')"
            >
              <a-button
                type="link"
                size="small"
              >
                提交
              </a-button>
            </a-popconfirm>
            <template v-if="record.status === 'submitted'">
              <a-popconfirm
                title="确认审批通过该预算？"
                @confirm="handleAction(record, 'approve')"
              >
                <a-button
                  type="link"
                  size="small"
                >
                  通过
                </a-button>
              </a-popconfirm>
              <a-popconfirm
                title="确认驳回该预算？"
                @confirm="handleAction(record, 'reject')"
              >
                <a-button
                  type="link"
                  size="small"
                  danger
                >
                  驳回
                </a-button>
              </a-popconfirm>
            </template>
            <a-popconfirm
              v-if="record.status === 'approved'"
              title="确认开始执行该预算？"
              @confirm="handleAction(record, 'start-exec')"
            >
              <a-button
                type="link"
                size="small"
              >
                开始执行
              </a-button>
            </a-popconfirm>
            <a-popconfirm
              v-if="record.status === 'executing'"
              title="确认关闭该预算？"
              @confirm="handleAction(record, 'close')"
            >
              <a-button
                type="link"
                size="small"
              >
                  关闭
                </a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </ARReportPage>

    <!-- ═══ 预算科目明细抽屉 ═══ -->
    <a-drawer
      v-model:open="detailOpen"
      :title="`预算科目明细 — ${currentBudget?.budgetNo || ''}`"
      width="720"
    >
      <a-table
        :columns="itemColumns"
        :data-source="budgetItems"
        :loading="detailLoading"
        :pagination="false"
        row-key="id"
        size="small"
      >
        <template #bodyCell="{ column, text }">
          <template v-if="['budgetAmount', 'usedAmount', 'remainingAmount', 'frozenAmount'].includes(column.dataIndex as string)">
            {{ formatMoney(text) }}
          </template>
          <template v-else-if="column.dataIndex === 'executionRate'">
            {{ formatRate(text) }}
          </template>
        </template>
      </a-table>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { annualBudgetApi, budgetItemApi } from '@/api/budget'

// ═══ 预算状态（与后端 AnnualBudget.status 一致） ═══
const STATUS_MAP: Record<string, { label: string; color: string }> = {
  draft: { label: '草稿', color: 'default' },
  submitted: { label: '待审批', color: 'orange' },
  approved: { label: '已审批', color: 'blue' },
  rejected: { label: '已驳回', color: 'red' },
  executing: { label: '执行中', color: 'green' },
  closed: { label: '已关闭', color: 'default' }
}

// ═══ 查询区 ═══
const currentYear = new Date().getFullYear()
const YEAR_OPTIONS = [0, 1, 2, 3].map(i => {
  const y = currentYear - i
  return { label: `${y}年`, value: y }
})

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '预算编号/名称', width: 180 },
  { key: 'fiscalYear', type: 'select', label: '财政年度', placeholder: '全部年度', options: YEAR_OPTIONS, width: 140 },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value })),
    width: 140
  }
]

// ═══ 表格列（与后端 AnnualBudgetDTO 字段一致） ═══
const columns: any[] = [
  { title: '预算编号', dataIndex: 'budgetNo', key: 'budgetNo', width: 170 },
  { title: '财政年度', dataIndex: 'fiscalYear', key: 'fiscalYear', width: 90, align: 'center' },
  { title: '部门', dataIndex: 'departmentName', key: 'departmentName', width: 120 },
  { title: '预算总额', dataIndex: 'totalAmount', key: 'totalAmount', width: 130, align: 'right' },
  { title: '已执行', dataIndex: 'totalUsedAmount', key: 'totalUsedAmount', width: 120, align: 'right' },
  { title: '剩余', dataIndex: 'totalRemainingAmount', key: 'totalRemainingAmount', width: 120, align: 'right' },
  { title: '执行率', dataIndex: 'executionRate', key: 'executionRate', width: 90, align: 'right' },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90, align: 'center' },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '操作', key: 'action', width: 220, fixed: 'right' }
]

// ═══ 明细列（与后端 BudgetItemDTO 字段一致） ═══
const itemColumns: any[] = [
  { title: '科目代码', dataIndex: 'subjectCode', key: 'subjectCode', width: 110 },
  { title: '科目名称', dataIndex: 'subjectName', key: 'subjectName', ellipsis: true },
  { title: '预算金额', dataIndex: 'budgetAmount', key: 'budgetAmount', width: 120, align: 'right' },
  { title: '已用金额', dataIndex: 'usedAmount', key: 'usedAmount', width: 120, align: 'right' },
  { title: '冻结金额', dataIndex: 'frozenAmount', key: 'frozenAmount', width: 110, align: 'right' },
  { title: '剩余金额', dataIndex: 'remainingAmount', key: 'remainingAmount', width: 120, align: 'right' },
  { title: '执行率', dataIndex: 'executionRate', key: 'executionRate', width: 90, align: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatRate(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return `${Number(val).toFixed(1)}%`
}

// ═══ 数据请求：年度预算分页（/erp/budget/annual/page，后端 page 从 0 开始） ═══
function fetcher(params: Record<string, any>) {
  const { page, size, ...rest } = params
  return annualBudgetApi.page({ ...rest, page: (page || 1) - 1, size })
}

// ═══ 状态流转操作 ═══
const reportRef = ref<InstanceType<typeof ARReportPage>>()

async function handleAction(record: any, action: 'submit' | 'approve' | 'reject' | 'start-exec' | 'close') {
  try {
    if (action === 'submit') await annualBudgetApi.submit(record.id)
    else if (action === 'approve') await annualBudgetApi.approve(record.id)
    else if (action === 'reject') await annualBudgetApi.reject(record.id)
    else if (action === 'start-exec') await annualBudgetApi.startExec(record.id)
    else await annualBudgetApi.close(record.id)
    message.success('操作成功')
    reportRef.value?.reload()
  } catch (e) {
    console.warn(`[预算编制] ${action} 操作失败`, e)
  }
}

// ═══ 明细抽屉 ═══
const detailOpen = ref(false)
const detailLoading = ref(false)
const currentBudget = ref<any>(null)
const budgetItems = ref<any[]>([])

async function openDetail(record: any) {
  currentBudget.value = record
  detailOpen.value = true
  detailLoading.value = true
  try {
    const res: any = await budgetItemApi.listByBudget(record.id)
    budgetItems.value = Array.isArray(res) ? res : []
  } catch (e) {
    budgetItems.value = []
    console.warn('[预算编制] 预算科目明细获取失败', e)
  } finally {
    detailLoading.value = false
  }
}
</script>
