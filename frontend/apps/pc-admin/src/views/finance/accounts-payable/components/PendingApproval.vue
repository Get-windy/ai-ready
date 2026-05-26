<template>
  <div class="pending-approval">
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'amount'">
          ¥{{ record.amount?.toFixed(2) }}
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
import { ref, reactive } from 'vue'
import { Modal, message } from 'ant-design-vue'

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

const columns = [
  { title: '供应商', dataIndex: 'supplierName', key: 'supplierName' },
  { title: '付款金额', key: 'amount' },
  { title: '到期日期', dataIndex: 'dueDate', key: 'dueDate' },
  { title: '申请人', dataIndex: 'applicant', key: 'applicant' },
  { title: '操作', key: 'action', width: 200 }
]

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
  loading.value = false
}, 500)
</script>

<style scoped>
.pending-approval {
  padding: 16px;
}
</style>