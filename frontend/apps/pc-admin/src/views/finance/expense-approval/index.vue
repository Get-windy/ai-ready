<template>
  <div>
    <ARReportPage
      ref="reportRef"
      title="费用审批"
      :columns="columns"
      :fetcher="fetcher"
      export-file-name="费用审批"
      row-key="id"
      empty-text="暂无待审批的费用单"
    >
      <template #bodyCell="{ column, record, text }">
        <template v-if="column.dataIndex === 'status'">
          <a-tag :color="statusColor(record.status)">
            {{ record.statusDesc || STATUS_MAP[record.status]?.label || record.status }}
          </a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'totalAmount'">
          {{ formatMoney(text) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button
              type="link"
              size="small"
              @click="openModal(record, 'APPROVE')"
            >
              通过
            </a-button>
            <a-button
              type="link"
              size="small"
              danger
              @click="openModal(record, 'REJECT')"
            >
              驳回
            </a-button>
          </a-space>
        </template>
      </template>
    </ARReportPage>

    <!-- ═══ 审批操作弹窗 ═══ -->
    <a-modal
      v-model:open="modalOpen"
      :title="modalAction === 'APPROVE' ? '审批通过' : '审批驳回'"
      :confirm-loading="submitting"
      @ok="handleSubmit"
      @cancel="resetModal"
    >
      <a-form layout="vertical">
        <a-form-item label="费用单">
          <span>{{ currentRecord?.applicationCode }} — {{ currentRecord?.applicantName }}（{{ formatMoney(currentRecord?.totalAmount) }}）</span>
        </a-form-item>
        <a-form-item
          :label="modalAction === 'APPROVE' ? '审批意见' : '驳回原因'"
          :required="modalAction === 'REJECT'"
        >
          <a-textarea
            v-model:value="comment"
            :rows="3"
            :placeholder="modalAction === 'APPROVE' ? '选填' : '必填'"
            :maxlength="200"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import ARReportPage from '@/components/ARReportPage/ARReportPage.vue'
import { expenseApprovalApi } from '@/api/finance'
import { useUserStore } from '@/stores/user'

// ═══ 费用单状态（与后端 ExpenseStatus 枚举一致） ═══
const STATUS_MAP: Record<string, { label: string; color: string }> = {
  DRAFT: { label: '草稿', color: 'default' },
  SUBMITTED: { label: '已提交', color: 'orange' },
  DEPARTMENT_APPROVING: { label: '部门审批中', color: 'orange' },
  FINANCE_APPROVING: { label: '财务审批中', color: 'orange' },
  GENERAL_MANAGER_APPROVING: { label: '总经理审批中', color: 'orange' },
  APPROVED: { label: '审批通过', color: 'green' },
  REJECTED: { label: '审批拒绝', color: 'red' },
  PAID: { label: '已支付', color: 'green' },
  REIMBURSED: { label: '已报销', color: 'green' },
  CANCELLED: { label: '已取消', color: 'red' }
}

function statusColor(status: string): string {
  return STATUS_MAP[status]?.color || 'default'
}

// ═══ 表格列（与后端 ExpenseApplication 字段一致） ═══
const columns: any[] = [
  { title: '申请编号', dataIndex: 'applicationCode', key: 'applicationCode', width: 170 },
  { title: '申请人', dataIndex: 'applicantName', key: 'applicantName', width: 100 },
  { title: '部门', dataIndex: 'departmentName', key: 'departmentName', width: 120 },
  { title: '费用类型', dataIndex: 'expenseTypeDesc', key: 'expenseTypeDesc', width: 100 },
  { title: '金额', dataIndex: 'totalAmount', key: 'totalAmount', width: 120, align: 'right' },
  { title: '申请日期', dataIndex: 'applyDate', key: 'applyDate', width: 110 },
  { title: '事由', dataIndex: 'purpose', key: 'purpose', ellipsis: true },
  { title: '当前审批人', dataIndex: 'currentApproverName', key: 'currentApproverName', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 110, align: 'center' },
  { title: '操作', key: 'action', width: 130, fixed: 'right' }
]

function formatMoney(val: number | null | undefined): string {
  if (val === null || val === undefined || isNaN(Number(val))) return '-'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ═══ 数据请求：待审批列表（/erp/expense/approval/pending，后端返回全量数组） ═══
function fetcher() {
  return expenseApprovalApi.getPending()
}

// ═══ 审批操作 ═══
const reportRef = ref<InstanceType<typeof ARReportPage>>()
const userStore = useUserStore()
const modalOpen = ref(false)
const submitting = ref(false)
const modalAction = ref<'APPROVE' | 'REJECT'>('APPROVE')
const currentRecord = ref<any>(null)
const comment = ref('')

function openModal(record: any, action: 'APPROVE' | 'REJECT') {
  currentRecord.value = record
  modalAction.value = action
  comment.value = ''
  modalOpen.value = true
}

function resetModal() {
  modalOpen.value = false
  currentRecord.value = null
  comment.value = ''
}

async function handleSubmit() {
  if (!currentRecord.value) return
  if (modalAction.value === 'REJECT' && !comment.value.trim()) {
    message.warning('请填写驳回原因')
    return
  }
  submitting.value = true
  try {
    await expenseApprovalApi.process({
      applicationId: currentRecord.value.id,
      action: modalAction.value,
      comment: comment.value.trim() || undefined,
      approverName: userStore.nickname || userStore.username || undefined
    })
    message.success(modalAction.value === 'APPROVE' ? '已通过' : '已驳回')
    resetModal()
    reportRef.value?.reload()
  } catch (e) {
    console.warn('[费用审批] 审批操作失败', e)
  } finally {
    submitting.value = false
  }
}
</script>
