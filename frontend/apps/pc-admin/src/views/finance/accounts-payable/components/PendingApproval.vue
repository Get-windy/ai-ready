<template>
  <div class="pending-approval">
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
      <template #action="{ record }">
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
    </VxeTableList>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { Modal, message } from 'ant-design-vue'
import VxeTableList from '@/components/VxeTableList/VxeTableList.vue'

defineEmits(['approve', 'reject'])

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

const vxeColumns = [
  { field: 'supplierName', title: '供应商', width: 150 },
  { field: 'amount', title: '付款金额', width: 120, align: 'right', slotName: 'amountCell' },
  { field: 'dueDate', title: '到期日期', width: 120 },
  { field: 'applicant', title: '申请人', width: 100 },
  { field: 'action', title: '操作', width: 200, fixed: 'right', type: 'action' }
]

const handlePageChange = (page: number, size: number) => {
  pagination.current = page
  pagination.pageSize = size
}

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

.amount-cell {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}




</style>
