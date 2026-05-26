<template>
  <div class="payment-approval-page">
    <a-card title="付款审批">
      <!-- Tab切换 -->
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane
          key="pending"
          tab="待审批"
        >
          <PendingApproval
            @approve="handleApprove"
            @reject="handleReject"
          />
        </a-tab-pane>
        <a-tab-pane
          key="approved"
          tab="已审批"
        >
          <ApprovedApproval />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import PendingApproval from './components/PendingApproval.vue'
import ApprovedApproval from './components/ApprovedApproval.vue'

const activeTab = ref('pending')

const handleApprove = (record: any) => {
  message.success(`审批通过: ${record.supplierName}`)
}

const handleReject = (record: any, reason: string) => {
  message.success(`已拒绝: ${record.supplierName}，原因: ${reason}`)
}
</script>

<style scoped>
.payment-approval-page {
  padding: 24px;
}
</style>