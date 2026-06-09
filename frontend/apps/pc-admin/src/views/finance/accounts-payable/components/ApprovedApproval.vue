<template>
  <div class="approved-approval">
    <VxeTableList
      :columns="vxeColumns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      @page-change="handlePageChange"
    >
      <template #amountCell="{ record }">
        <span class="amount-cell">¥{{ record.amount?.toFixed(2) }}</span>
      </template>
      <template #statusCell="{ record }">
        <a-tag :color="record.status === '已通过' ? 'green' : 'red'">
          {{ record.status }}
        </a-tag>
      </template>
    </VxeTableList>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'

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

const vxeColumns = [
  { field: 'supplierName', title: '供应商', width: 150 },
  { field: 'amount', title: '付款金额', width: 120, align: 'right', slotName: 'amountCell' },
  { field: 'status', title: '状态', width: 100, slotName: 'statusCell' },
  { field: 'approveDate', title: '审批日期', width: 120 },
  { field: 'approver', title: '审批人', width: 100 }
]

const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
}

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

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}




</style>
