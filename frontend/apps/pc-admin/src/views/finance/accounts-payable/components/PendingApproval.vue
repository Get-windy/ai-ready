<template>
  <div class="pending-approval">
    <a-table
      :columns="columns"
      :data-source="tableDataSource"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="record.__empty_row">
          <span class="empty-placeholder">&nbsp;</span>
        </template>
        <template v-else-if="column.key === 'amount'">
          <span class="amount-cell">¥{{ record.amount?.toFixed(2) }}</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button
              type="link"
              size="small"
              @click="$emit('approve', record)"
            >
              审批通过
            </a-button>
            <a-button
              type="link"
              size="small"
              danger
              @click="handleReject(record)"
            >
              拒绝
            </a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { Modal, message } from 'ant-design-vue'

defineEmits(['approve', 'reject'])

const MIN_TABLE_ROWS = 20

interface PaymentApproval {
  id: number
  supplierName: string
  amount: number
  dueDate: string
  applicant: string
}

const loading = ref(false)
const dataSource = ref<PaymentApproval[]>([])

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true
})

const columns = [
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName', width: 150 },
  { title: '付款金额', key: 'amount', width: 120, align: 'right' as const },
  { title: '到期日期', dataIndex: 'dueDate', key: 'dueDate', width: 120 },
  { title: '申请人', dataIndex: 'applicant', key: 'applicant', width: 100 },
  { title: '操作', key: 'action', width: 200, fixed: 'right' as const }
]

// ── 空行填充 ────────────────────────────────────────────
const tableDataSource = computed(() => {
  const data = [...dataSource.value]
  const emptyCount = Math.max(0, MIN_TABLE_ROWS - data.length)
  for (let i = 0; i < emptyCount; i++) {
    data.push({ __empty_row: true, id: `__empty_${i}` } as any)
  }
  return data
})

const handleReject = (record: PaymentApproval) => {
  Modal.confirm({
    title: '拒绝付款',
    content: '请输入拒绝原因：',
    okText: '确认',
    cancelText: '取消',
    onOk() {
      message.success(`已拒绝: ${record.supplierName}`)
    }
  })
}

loading.value = true
setTimeout(() => {
  dataSource.value = [
    {
      id: 1,
      supplierName: '供应商A',
      amount: 5000,
      dueDate: '2026-04-20',
      applicant: '张三'
    }
  ]
  pagination.total = 1
  loading.value = false
}, 500)
</script>

<style scoped>
.pending-approval {
  background: #fff;
  border-radius: 8px;
}

.empty-placeholder {
  color: transparent;
}

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}

/* 表格网格边框 */
:deep(.ant-table-thead > tr > th) {
  border-top: 1px solid #d9d9d9 !important;
  border-right: 1px solid #d9d9d9 !important;
  border-bottom: 2px solid #b0b0b0 !important;
  background: #fafafa !important;
  padding: 8px 12px !important;
  font-weight: 600 !important;
}

:deep(.ant-table-thead > tr > th:first-child) {
  border-left: 1px solid #d9d9d9 !important;
}

:deep(.ant-table-tbody > tr > td) {
  border-right: 1px solid #e0e0e0 !important;
  border-bottom: 1px solid #e8e8e8 !important;
  padding: 8px 12px !important;
}

:deep(.ant-table-tbody > tr > td:first-child) {
  border-left: 1px solid #e0e0e0 !important;
}
</style>