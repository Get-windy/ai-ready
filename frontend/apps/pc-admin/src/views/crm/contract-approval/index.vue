<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="合同审批"
      :query-fields="queryFields"
      :columns="columns"
      :fetcher="fetcher"
      :stat-cards="approvalCards"
      page-param-style="pageNum"
      export-file-name="合同审批"
      row-key="id"
    >
      <template #bodyCell="{ column, text, record }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="contractStatusColor(text)">
            {{ record.statusDesc || contractStatusText(text) }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'contractAmount'">
          {{ formatMoney(text) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space :size="4">
            <template v-if="record.status === 1">
              <a-button
                type="link"
                size="small"
                @click="openApproveModal(record)"
              >
                通过
              </a-button>
              <a-button
                type="link"
                size="small"
                danger
                @click="openRejectModal(record)"
              >
                驳回
              </a-button>
            </template>
            <a-button
              v-else-if="record.status === 0"
              type="link"
              size="small"
              @click="handleSubmit(record)"
            >
              提交审批
            </a-button>
            <span
              v-else
              class="no-action"
            >—</span>
          </a-space>
        </template>
      </template>
    </ARReportPage>

    <!-- ═══ 审批通过弹窗 ═══ -->
    <a-modal
      v-model:open="approveModalVisible"
      title="审批通过"
      :confirm-loading="actionSaving"
      ok-text="确定通过"
      cancel-text="取消"
      @ok="handleApprove"
    >
      <a-form
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="合同">
          <span>{{ currentContract?.contractName }}（{{ currentContract?.contractNo }}）</span>
        </a-form-item>
        <a-form-item label="合同金额">
          <span>¥ {{ formatMoney(currentContract?.contractAmount) }}</span>
        </a-form-item>
        <a-form-item label="审批意见">
          <a-textarea
            v-model:value="approveNote"
            :rows="3"
            placeholder="审批意见（可选）"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- ═══ 审批驳回弹窗 ═══ -->
    <a-modal
      v-model:open="rejectModalVisible"
      title="审批驳回"
      :confirm-loading="actionSaving"
      ok-text="确定驳回"
      ok-type="danger"
      cancel-text="取消"
      @ok="handleReject"
    >
      <a-form
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="合同">
          <span>{{ currentContract?.contractName }}（{{ currentContract?.contractNo }}）</span>
        </a-form-item>
        <a-form-item
          label="驳回原因"
          required
        >
          <a-textarea
            v-model:value="rejectReason"
            :rows="3"
            placeholder="请输入驳回原因"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import type { ReportQueryField, StatCardItem } from '@/components/ARReportPage/types'
import { contractApi, contractApprovalApi, type ContractItem } from '@/api/crm'

// ═══ 合同状态（与后端 ContractStatus 枚举一致） ═══
const CONTRACT_STATUS_OPTIONS = [
  { label: '草稿', value: 0 },
  { label: '待审批', value: 1 },
  { label: '已审批', value: 2 },
  { label: '待签署', value: 3 },
  { label: '已签署', value: 4 },
  { label: '生效中', value: 5 },
  { label: '执行中', value: 6 },
  { label: '已完成', value: 7 },
  { label: '已终止', value: 8 },
  { label: '已过期', value: 9 },
  { label: '已取消', value: 10 }
]
const STATUS_TEXT: Record<number, string> = {
  0: '草稿', 1: '待审批', 2: '已审批', 3: '待签署', 4: '已签署',
  5: '生效中', 6: '执行中', 7: '已完成', 8: '已终止', 9: '已过期', 10: '已取消'
}
const STATUS_COLOR: Record<number, string> = {
  0: 'default', 1: 'orange', 2: 'blue', 3: 'cyan', 4: 'geekblue',
  5: 'green', 6: 'green', 7: 'green', 8: 'red', 9: 'default', 10: 'red'
}

function contractStatusText(v: number | undefined): string {
  return STATUS_TEXT[v ?? -1] || '未知'
}
function contractStatusColor(v: number | undefined): string {
  return STATUS_COLOR[v ?? -1] || 'default'
}

const queryFields: ReportQueryField[] = [
  { key: 'keyword', type: 'input', label: '关键字', placeholder: '合同编号/名称/客户', width: 200 },
  {
    key: 'status',
    type: 'select',
    label: '状态',
    placeholder: '全部状态',
    options: CONTRACT_STATUS_OPTIONS
  }
]

// ═══ 表格列 ═══
const columns: any[] = [
  { title: '合同编号', dataIndex: 'contractNo', key: 'contractNo', width: 150 },
  { title: '合同名称', dataIndex: 'contractName', key: 'contractName', width: 180, ellipsis: true },
  { title: '客户', dataIndex: 'customerName', key: 'customerName', width: 160, ellipsis: true },
  { title: '合同金额', dataIndex: 'contractAmount', key: 'contractAmount', width: 120, align: 'right' },
  { title: '签署日期', dataIndex: 'signDate', key: 'signDate', width: 110 },
  { title: '开始日期', dataIndex: 'startDate', key: 'startDate', width: 110 },
  { title: '结束日期', dataIndex: 'endDate', key: 'endDate', width: 110 },
  { title: '负责人', dataIndex: 'salesPersonName', key: 'salesPersonName', width: 100 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 130, fixed: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求 ═══
const reportRef = ref<InstanceType<typeof ARReportPage>>()

function fetcher(params: Record<string, any>) {
  return contractApi.page(params)
}

// ═══ 审批统计卡片（/crm/contract/statistics 为裸 Map 会被拦截器误判，
//      故由 /crm/contract/export 全量实时聚合） ═══
const allContracts = ref<ContractItem[]>([])

const approvalCards = computed<StatCardItem[]>(() => {
  const list = allContracts.value
  const pending = list.filter(c => c.status === 1).length
  const draft = list.filter(c => c.status === 0).length
  const effectiveAmount = list
    .filter(c => c.status === 5 || c.status === 6)
    .reduce((acc, c) => acc + (Number(c.contractAmount) || 0), 0)
  return [
    { label: '待审批', value: pending, suffix: '份', valueStyle: { color: '#fa8c16' } },
    { label: '草稿', value: draft, suffix: '份' },
    { label: '合同总数', value: list.length, suffix: '份' },
    { label: '生效/执行金额', value: effectiveAmount, precision: 2, prefix: '¥' }
  ]
})

async function loadApprovalStats() {
  try {
    const list = await contractApprovalApi.exportList()
    allContracts.value = Array.isArray(list) ? list : []
  } catch (e) {
    console.warn('[合同审批] 审批统计获取失败', e)
  }
}

// ═══ 审批操作 ═══
const actionSaving = ref(false)
const currentContract = ref<ContractItem | null>(null)

// ── 提交审批（草稿状态） ──
function handleSubmit(record: any) {
  Modal.confirm({
    title: '提交审批',
    content: `确定将合同「${record.contractName}」提交审批吗？`,
    okText: '确定',
    cancelText: '取消',
    onOk: async () => {
      try {
        await contractApprovalApi.submit(record.id)
        message.success('已提交审批')
        afterAction()
      } catch (e: any) {
        message.error(e?.message || '提交失败')
      }
    }
  })
}

// ── 通过 ──
const approveModalVisible = ref(false)
const approveNote = ref('')

function openApproveModal(record: any) {
  currentContract.value = record
  approveNote.value = ''
  approveModalVisible.value = true
}

async function handleApprove() {
  if (!currentContract.value) return
  actionSaving.value = true
  try {
    await contractApprovalApi.approve(currentContract.value.id, approveNote.value.trim() || undefined)
    message.success('审批已通过')
    approveModalVisible.value = false
    afterAction()
  } catch (e: any) {
    message.error(e?.message || '审批失败')
  } finally {
    actionSaving.value = false
  }
}

// ── 驳回 ──
const rejectModalVisible = ref(false)
const rejectReason = ref('')

function openRejectModal(record: any) {
  currentContract.value = record
  rejectReason.value = ''
  rejectModalVisible.value = true
}

async function handleReject() {
  if (!currentContract.value) return
  if (!rejectReason.value.trim()) {
    message.warning('请输入驳回原因')
    return
  }
  actionSaving.value = true
  try {
    await contractApprovalApi.reject(currentContract.value.id, rejectReason.value.trim())
    message.success('已驳回')
    rejectModalVisible.value = false
    afterAction()
  } catch (e: any) {
    message.error(e?.message || '驳回失败')
  } finally {
    actionSaving.value = false
  }
}

function afterAction() {
  reportRef.value?.reload()
  loadApprovalStats()
}

onMounted(() => {
  loadApprovalStats()
})
</script>

<style scoped>
.no-action {
  color: #bbb;
}
</style>
