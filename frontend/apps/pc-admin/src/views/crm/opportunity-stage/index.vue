<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="商机阶段"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      page-param-style="pageNum"
      export-file-name="商机阶段"
      row-key="id"
    >
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'opportunityStage'">
          <a-tag :color="stageColor(text)">
            {{ stageText(record) }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'probability'">
          <a-progress
            :percent="text || 0"
            size="small"
            :stroke-color="text >= 70 ? '#52c41a' : '#1890ff'"
          />
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <a-tag :color="oppStatusColor(text)">
            {{ oppStatusText(record) }}
          </a-tag>
        </template>
        <template v-else-if="['estimatedAmount', 'actualAmount'].includes(column.dataIndex as string)">
          {{ formatMoney(text) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space :size="4">
            <a-button
              type="link"
              size="small"
              :disabled="!isActive(record) || (record.opportunityStage ?? 0) >= 5"
              @click="handleAdvance(record)"
            >
              推进
            </a-button>
            <a-button
              type="link"
              size="small"
              :disabled="!isActive(record)"
              @click="openWinModal(record)"
            >
              赢单
            </a-button>
            <a-button
              type="link"
              size="small"
              danger
              :disabled="!isActive(record)"
              @click="openLoseModal(record)"
            >
              输单
            </a-button>
          </a-space>
        </template>
      </template>
    </ARReportPage>

    <!-- ═══ 赢单弹窗 ═══ -->
    <a-modal
      v-model:open="winModalVisible"
      title="商机赢单"
      :confirm-loading="actionSaving"
      ok-text="确定赢单"
      cancel-text="取消"
      @ok="handleWin"
    >
      <a-form
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="商机">
          <span>{{ currentOpp?.opportunityName }}</span>
        </a-form-item>
        <a-form-item
          label="成交金额"
          required
        >
          <a-input-number
            v-model:value="winAmount"
            :min="0"
            :precision="2"
            style="width: 100%"
            placeholder="请输入实际成交金额"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ═══ 输单弹窗 ═══ -->
    <a-modal
      v-model:open="loseModalVisible"
      title="商机输单"
      :confirm-loading="actionSaving"
      ok-text="确定输单"
      ok-type="danger"
      cancel-text="取消"
      @ok="handleLose"
    >
      <a-form
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="商机">
          <span>{{ currentOpp?.opportunityName }}</span>
        </a-form-item>
        <a-form-item
          label="输单原因"
          required
        >
          <a-textarea
            v-model:value="loseReason"
            :rows="3"
            placeholder="请输入输单原因"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message, Modal } from 'ant-design-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField } from '@/components/ARReportPage/types'
import { opportunityApi, opportunityStageApi, type OpportunityRecord } from '@/api/crm'

// ═══ 商机阶段/状态（与后端 CustomerOpportunityServiceImpl 一致） ═══
const STAGE_OPTIONS = [
  { label: '初步接触', value: 1 },
  { label: '需求确认', value: 2 },
  { label: '方案报价', value: 3 },
  { label: '商务谈判', value: 4 },
  { label: '成交', value: 5 }
]
const STAGE_TEXT: Record<number, string> = { 1: '初步接触', 2: '需求确认', 3: '方案报价', 4: '商务谈判', 5: '成交' }
const STAGE_COLOR: Record<number, string> = { 1: 'default', 2: 'blue', 3: 'cyan', 4: 'purple', 5: 'green' }
const OPP_STATUS_OPTIONS = [
  { label: '跟进中', value: 1 },
  { label: '赢单', value: 2 },
  { label: '输单', value: 3 }
]
const OPP_STATUS_TEXT: Record<number, string> = { 1: '跟进中', 2: '赢单', 3: '输单' }
const OPP_STATUS_COLOR: Record<number, string> = { 1: 'orange', 2: 'green', 3: 'red' }

function stageText(record: any): string {
  return record.opportunityStageDesc || STAGE_TEXT[record.opportunityStage ?? -1] || '未知'
}
function stageColor(v: number | undefined): string {
  return (v && STAGE_COLOR[v]) || 'default'
}
function oppStatusText(record: any): string {
  return record.statusDesc || OPP_STATUS_TEXT[record.status ?? 1] || '跟进中'
}
function oppStatusColor(v: number | undefined): string {
  return OPP_STATUS_COLOR[v ?? 1] || 'default'
}
function isActive(record: any): boolean {
  return record.status !== 2 && record.status !== 3
}

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '商机名称/客户', width: 200 },
  { key: 'opportunityStage', type: 'select', label: '阶段', placeholder: '全部阶段', options: STAGE_OPTIONS },
  { key: 'status', type: 'select', label: '状态', placeholder: '全部状态', options: OPP_STATUS_OPTIONS }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '商机编号', dataIndex: 'opportunityCode', key: 'opportunityCode', width: 150 },
  { title: '商机名称', dataIndex: 'opportunityName', key: 'opportunityName', width: 170, ellipsis: true },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 150, ellipsis: true },
  { title: '阶段', dataIndex: 'opportunityStage', key: 'opportunityStage', width: 100 },
  { title: '赢单概率', dataIndex: 'probability', key: 'probability', width: 120 },
  { title: '预计金额', dataIndex: 'estimatedAmount', key: 'estimatedAmount', width: 110, align: 'right' },
  { title: '成交金额', dataIndex: 'actualAmount', key: 'actualAmount', width: 110, align: 'right' },
  { title: '预计成交日', dataIndex: 'expectedCloseDate', key: 'expectedCloseDate', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '负责人', dataIndex: 'salesPersonName', key: 'salesPersonName', width: 100 },
  { title: '操作', key: 'action', width: 170, fixed: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求 ═══
const reportRef = ref<InstanceType<typeof ARReportPage>>()

function fetcher(params: Record<string, any>) {
  return opportunityApi.page(params)
}

// ═══ 阶段操作 ═══
const actionSaving = ref(false)
const currentOpp = ref<OpportunityRecord | null>(null)

function handleAdvance(record: any) {
  Modal.confirm({
    title: '推进商机阶段',
    content: `确定将商机「${record.opportunityName}」推进到下一阶段（${STAGE_TEXT[(record.opportunityStage ?? 0) + 1]}）吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await opportunityStageApi.advance(record.id)
        message.success('商机阶段已推进')
        reportRef.value?.reload()
      } catch (e: any) {
        message.error(e?.message || '推进失败')
      }
    }
  })
}

// ── 赢单 ──
const winModalVisible = ref(false)
const winAmount = ref<number>()

function openWinModal(record: any) {
  currentOpp.value = record
  winAmount.value = record.estimatedAmount ? Number(record.estimatedAmount) : undefined
  winModalVisible.value = true
}

async function handleWin() {
  if (!currentOpp.value) return
  if (winAmount.value === undefined || winAmount.value === null) {
    message.warning('请输入成交金额')
    return
  }
  actionSaving.value = true
  try {
    await opportunityStageApi.win(currentOpp.value.id, winAmount.value)
    message.success('已标记为赢单')
    winModalVisible.value = false
    reportRef.value?.reload()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  } finally {
    actionSaving.value = false
  }
}

// ── 输单 ──
const loseModalVisible = ref(false)
const loseReason = ref('')

function openLoseModal(record: any) {
  currentOpp.value = record
  loseReason.value = ''
  loseModalVisible.value = true
}

async function handleLose() {
  if (!currentOpp.value) return
  if (!loseReason.value.trim()) {
    message.warning('请输入输单原因')
    return
  }
  actionSaving.value = true
  try {
    await opportunityStageApi.lose(currentOpp.value.id, loseReason.value.trim())
    message.success('已标记为输单')
    loseModalVisible.value = false
    reportRef.value?.reload()
  } catch (e: any) {
    message.error(e?.message || '操作失败')
  } finally {
    actionSaving.value = false
  }
}
</script>
