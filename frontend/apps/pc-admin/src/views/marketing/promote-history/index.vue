<template>
  <ARReportPage
    ref="reportRef"
    title="推广记录"
    :query-fields="queryFields"
    :columns="columns"
    :fetcher="fetcher"
    page-param-style="pageNum"
    export-file-name="推广记录"
    row-key="id"
  >
    <template #bodyCell="{ column, record, text }">
      <template v-if="column.dataIndex === 'campaignType'">
        {{ record.campaignTypeDesc || CAMPAIGN_TYPE_MAP[text] || '-' }}
      </template>
      <template v-else-if="column.dataIndex === 'period'">
        {{ fmtDate(record.startDate) }} ~ {{ fmtDate(record.endDate) }}
      </template>
      <template v-else-if="['budget', 'actualCost', 'actualRevenue'].includes(column.dataIndex as string)">
        {{ formatMoney(text) }}
      </template>
      <template v-else-if="column.dataIndex === 'funnel'">
        触达 {{ formatNum(record.reachedCustomerCount) }} / 转化 {{ formatNum(record.convertedCustomerCount) }}
      </template>
      <template v-else-if="column.dataIndex === 'status'">
        <a-tag :color="STATUS_MAP[text]?.color">
          {{ record.statusDesc || STATUS_MAP[text]?.label || '-' }}
        </a-tag>
      </template>
      <template v-else-if="column.key === 'action'">
        <a-space wrap>
          <a
            v-if="record.status === 0"
            @click="openEdit(record)"
          >编辑</a>
          <a-popconfirm
            v-if="record.status === 0"
            title="确认提交审批？"
            @confirm="runAction(record, 'submit')"
          >
            <a>提交审批</a>
          </a-popconfirm>
          <a-popconfirm
            v-if="record.status === 1"
            title="确认审批通过？"
            @confirm="runAction(record, 'approve')"
          >
            <a>审批通过</a>
          </a-popconfirm>
          <a
            v-if="record.status === 1"
            class="text-danger"
            @click="openReason(record, 'reject')"
          >拒绝</a>
          <a-popconfirm
            v-if="record.status === 2 || record.status === 3"
            title="确认开始执行该活动？"
            @confirm="runAction(record, 'start')"
          >
            <a>开始</a>
          </a-popconfirm>
          <a-popconfirm
            v-if="record.status === 4"
            title="确认暂停该活动？"
            @confirm="runAction(record, 'pause')"
          >
            <a>暂停</a>
          </a-popconfirm>
          <a-popconfirm
            v-if="record.status === 5"
            title="确认恢复执行？"
            @confirm="runAction(record, 'resume')"
          >
            <a>恢复</a>
          </a-popconfirm>
          <a-popconfirm
            v-if="record.status === 4 || record.status === 5"
            title="确认完成该活动？"
            @confirm="runAction(record, 'complete')"
          >
            <a>完成</a>
          </a-popconfirm>
          <a
            v-if="record.status !== 6 && record.status !== 7"
            class="text-danger"
            @click="openReason(record, 'cancel')"
          >取消</a>
        </a-space>
      </template>
    </template>
  </ARReportPage>

  <!-- 编辑活动弹窗（仅草稿可修改，后端约束） -->
  <a-modal
    v-model:open="editVisible"
    title="编辑推广活动"
    :confirm-loading="actionLoading"
    :width="560"
    @ok="handleEditOk"
    @cancel="editVisible = false"
  >
    <a-form
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
      style="margin-top: 16px"
    >
      <a-form-item
        label="活动名称"
        required
      >
        <a-input
          v-model:value="editForm.campaignName"
          placeholder="请输入活动名称"
        />
      </a-form-item>
      <a-form-item label="活动类型">
        <a-select
          v-model:value="editForm.campaignType"
          :options="campaignTypeOptions"
          placeholder="请选择"
        />
      </a-form-item>
      <a-form-item label="活动时间">
        <a-range-picker
          v-model:value="editForm.dateRange"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="活动预算">
        <a-input-number
          v-model:value="editForm.budget"
          :min="0"
          :precision="2"
          style="width: 100%"
        />
      </a-form-item>
      <a-form-item label="活动目标">
        <a-textarea
          v-model:value="editForm.objective"
          :rows="2"
        />
      </a-form-item>
      <a-form-item label="活动描述">
        <a-textarea
          v-model:value="editForm.description"
          :rows="3"
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 拒绝/取消原因弹窗 -->
  <a-modal
    v-model:open="reasonVisible"
    :title="reasonAction === 'reject' ? '审批拒绝' : '取消活动'"
    :confirm-loading="actionLoading"
    :width="420"
    @ok="handleReasonOk"
    @cancel="reasonVisible = false"
  >
    <a-form
      :label-col="{ span: 5 }"
      :wrapper-col="{ span: 18 }"
      style="margin-top: 16px"
    >
      <a-form-item
        label="原因"
        required
      >
        <a-textarea
          v-model:value="reasonText"
          :rows="3"
          placeholder="请输入原因"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import dayjs, { type Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { campaignApi, type MarketingCampaign, type CampaignCreatePayload } from '@/api/marketing'

// ═══ 活动类型/状态（与后端 CampaignType / CampaignStatus 枚举一致） ═══
const CAMPAIGN_TYPE_MAP: Record<number, string> = {
  1: '邮件营销', 2: '短信营销', 3: '微信营销', 4: '电话营销', 5: '活动营销',
  6: '线上推广', 7: '线下推广', 8: '内容营销', 9: '社交媒体', 10: '综合营销'
}
const STATUS_MAP: Record<number, { label: string; color: string }> = {
  0: { label: '草稿', color: 'default' },
  1: { label: '待审批', color: 'orange' },
  2: { label: '已审批', color: 'blue' },
  3: { label: '已排期', color: 'blue' },
  4: { label: '进行中', color: 'green' },
  5: { label: '已暂停', color: 'orange' },
  6: { label: '已完成', color: 'green' },
  7: { label: '已取消', color: 'red' }
}
const campaignTypeOptions = Object.entries(CAMPAIGN_TYPE_MAP).map(([value, label]) => ({ label, value: Number(value) }))

const reportRef = ref<InstanceType<typeof ARReportPage>>()

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '活动名称/编码' },
  { key: 'campaignType', type: 'select', label: '类型', placeholder: '全部类型', options: campaignTypeOptions },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: Object.entries(STATUS_MAP).map(([value, v]) => ({ label: v.label, value: Number(value) }))
  }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '活动编码', dataIndex: 'campaignCode', key: 'campaignCode', width: 130 },
  { title: '活动名称', dataIndex: 'campaignName', key: 'campaignName', width: 170, ellipsis: true },
  { title: '类型', dataIndex: 'campaignType', key: 'campaignType', width: 100 },
  { title: '活动时间', dataIndex: 'period', key: 'period', width: 200 },
  { title: '预算', dataIndex: 'budget', key: 'budget', width: 110, align: 'right' },
  { title: '实际花费', dataIndex: 'actualCost', key: 'actualCost', width: 110, align: 'right' },
  { title: '实际营收', dataIndex: 'actualRevenue', key: 'actualRevenue', width: 110, align: 'right' },
  { title: '触达/转化', dataIndex: 'funnel', key: 'funnel', width: 140 },
  { title: '负责人', dataIndex: 'ownerName', key: 'ownerName', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 230, fixed: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function formatNum(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '0'
  return Number(val).toLocaleString('zh-CN')
}

function fmtDate(val: string | null | undefined): string {
  return val ? dayjs(val).format('YYYY-MM-DD') : '-'
}

// ═══ 数据请求 ═══
function fetcher(params: Record<string, any>) {
  return campaignApi.page(params)
}

// ═══ 生命周期操作 ═══
const actionLoading = ref(false)

type LifecycleAction = 'submit' | 'approve' | 'start' | 'pause' | 'resume' | 'complete'

async function runAction(record: any, action: LifecycleAction) {
  actionLoading.value = true
  try {
    await campaignApi[action](record.id)
    message.success('操作成功')
    reportRef.value?.reload()
  } catch (e) {
    console.warn(`[推广记录] ${action} 失败`, e)
  } finally {
    actionLoading.value = false
  }
}

// ═══ 拒绝/取消（需填原因） ═══
const reasonVisible = ref(false)
const reasonAction = ref<'reject' | 'cancel'>('reject')
const reasonText = ref('')
const reasonRecord = ref<MarketingCampaign | null>(null)

function openReason(record: any, action: 'reject' | 'cancel') {
  reasonRecord.value = record
  reasonAction.value = action
  reasonText.value = ''
  reasonVisible.value = true
}

async function handleReasonOk() {
  if (!reasonText.value.trim()) {
    message.warning('请输入原因')
    return
  }
  if (!reasonRecord.value) return
  actionLoading.value = true
  try {
    if (reasonAction.value === 'reject') {
      await campaignApi.reject(reasonRecord.value.id, reasonText.value.trim())
    } else {
      await campaignApi.cancel(reasonRecord.value.id, reasonText.value.trim())
    }
    message.success('操作成功')
    reasonVisible.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[推广记录] 操作失败', e)
  } finally {
    actionLoading.value = false
  }
}

// ═══ 编辑（草稿） ═══
const editVisible = ref(false)
const editRecord = ref<MarketingCampaign | null>(null)
const editForm = reactive<{
  campaignName?: string
  campaignType?: number
  dateRange?: [Dayjs, Dayjs]
  budget?: number
  objective?: string
  description?: string
}>({})

function openEdit(record: any) {
  editRecord.value = record
  Object.assign(editForm, {
    campaignName: record.campaignName,
    campaignType: record.campaignType,
    dateRange: record.startDate && record.endDate ? [dayjs(record.startDate), dayjs(record.endDate)] : undefined,
    budget: record.budget,
    objective: record.objective,
    description: record.description
  })
  editVisible.value = true
}

async function handleEditOk() {
  if (!editForm.campaignName) {
    message.warning('请输入活动名称')
    return
  }
  if (!editRecord.value) return
  actionLoading.value = true
  try {
    const payload: CampaignCreatePayload = {
      campaignName: editForm.campaignName,
      campaignType: editForm.campaignType,
      budget: editForm.budget,
      objective: editForm.objective,
      description: editForm.description,
      startDate: editForm.dateRange?.[0] ? dayjs(editForm.dateRange[0]).format('YYYY-MM-DD') : undefined,
      endDate: editForm.dateRange?.[1] ? dayjs(editForm.dateRange[1]).format('YYYY-MM-DD') : undefined
    }
    await campaignApi.update(editRecord.value.id, payload)
    message.success('更新成功')
    editVisible.value = false
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[推广记录] 更新失败', e)
  } finally {
    actionLoading.value = false
  }
}
</script>

<style scoped>
.text-danger {
  color: #ff4d4f;
}
</style>
