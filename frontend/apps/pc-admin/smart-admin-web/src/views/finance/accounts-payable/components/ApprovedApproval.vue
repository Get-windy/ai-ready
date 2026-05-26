<template>
  <div class="approved-approval">
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'

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
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName' },
  { title: '付款金额', dataIndex: 'amount', key: 'amount' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '审批日期', dataIndex: 'approveDate', key: 'approveDate' },
  { title: '审批人', dataIndex: 'approver', key: 'approver' }
]

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
  loading.value = false
}, 500)
</script>

<style scoped>
.approved-approval {
  padding: 16px;
}
</style>