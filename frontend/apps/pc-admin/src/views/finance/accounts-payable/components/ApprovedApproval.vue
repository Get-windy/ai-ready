<template>
  <div class="approved-approval">
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
        <template v-else-if="column.key === 'status'">
          <a-tag :color="record.status === '已通过' ? 'green' : 'red'">
            {{ record.status }}
          </a-tag>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'

const MIN_TABLE_ROWS = 20

interface PaymentApproval {
  id: number
  supplierName: string
  amount: number
  status: string
  approveDate: string
  approver: string
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
  { title: '状态', key: 'status', width: 100 },
  { title: '审批日期', dataIndex: 'approveDate', key: 'approveDate', width: 120 },
  { title: '审批人', dataIndex: 'approver', key: 'approver', width: 100 }
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

loading.value = true
setTimeout(() => {
  dataSource.value = [
    {
      id: 1,
      supplierName: '供应商A',
      amount: 5000,
      status: '已通过',
      approveDate: '2026-04-13',
      approver: '李四'
    }
  ]
  pagination.total = 1
  loading.value = false
}, 500)
</script>

<style scoped>
.approved-approval {
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