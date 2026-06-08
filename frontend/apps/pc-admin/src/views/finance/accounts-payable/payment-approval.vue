<template>
  <div class="payment-approval-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-pending">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ stats.pendingCount }}</div>
          <div class="stat-card-label">待审批</div>
        </div>
        <ClockCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-approved">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ stats.approvedCount }}</div>
          <div class="stat-card-label">已通过</div>
        </div>
        <CheckCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-rejected">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ stats.rejectedCount }}</div>
          <div class="stat-card-label">已拒绝</div>
        </div>
        <CloseCircleOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-amount">
        <div class="stat-card-body">
          <div class="stat-card-value">¥{{ formatAmount(stats.pendingAmount) }}</div>
          <div class="stat-card-label">待审批金额</div>
        </div>
        <DollarOutlined class="stat-card-icon" />
      </div>
    </div>

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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { ClockCircleOutlined, CheckCircleOutlined, CloseCircleOutlined, DollarOutlined } from '@ant-design/icons-vue'
import PendingApproval from './components/PendingApproval.vue'
import ApprovedApproval from './components/ApprovedApproval.vue'

const activeTab = ref('pending')

const stats = reactive({
  pendingCount: 5,
  approvedCount: 12,
  rejectedCount: 2,
  pendingAmount: 158000
})

const formatAmount = (val: number) => {
  if (val === undefined || val === null) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const handleApprove = (record: any) => {
  message.success(`审批通过: ${record.supplierName}`)
}

const handleReject = (record: any, reason: string) => {
  message.success(`已拒绝: ${record.supplierName}，原因: ${reason}`)
}
</script>

<style scoped>
.payment-approval-page {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 统计卡片 */
.stat-cards {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
}

.stat-card {
  flex: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px;
  border-radius: 8px;
}

.stat-pending { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-approved { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-rejected { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }
.stat-amount { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }

.stat-card-value {
  font-size: 18px;
  font-weight: 600;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  color: #333;
}

.stat-card-label {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

.stat-card-icon {
  font-size: 24px;
  color: rgba(0, 0, 0, 0.15);
}

/* 响应式 */
@media (max-width: 768px) {
  .stat-cards {
    flex-wrap: wrap;
  }
  .stat-card {
    flex: 1 1 45%;
    min-width: 120px;
  }
}
</style>