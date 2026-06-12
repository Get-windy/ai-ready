<template>
  <div class="payment-approval-page">
    <!-- 打印 -->
    <div style="padding: 0 0 12px 0; display: flex; justify-content: flex-end;">
      <PrintButton business-type="payment_approval" />
    </div>
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
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { message } from 'ant-design-vue'
import { ClockCircleOutlined, CheckCircleOutlined, CloseCircleOutlined, DollarOutlined } from '@ant-design/icons-vue'
import PrintButton from '@/components/business/print-button/PrintButton.vue'
import PendingApproval from './components/PendingApproval.vue'
import ApprovedApproval from './components/ApprovedApproval.vue'

const debounceMap = new Map<string, number>()
function debounceClick(key: string, fn: () => void, delay = 300) {
  const now = Date.now(); const last = debounceMap.get(key) || 0
  if (now - last < delay) return; debounceMap.set(key, now); fn()
}

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

const fetchData = async () => {
  // 组件级别无需加载数据，由子组件自行维护
}

function handleParentCreate() {
  handleAdd()
}

function isInput(el: Element | null): boolean {
  if (!el) return false
  const tag = el.tagName.toLowerCase()
  return tag === 'input' || tag === 'textarea' || tag === 'select' || (el as HTMLElement)?.isContentEditable
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'F5' && !e.ctrlKey && !e.metaKey && !isInput(e.target as Element | null)) { e.preventDefault(); debounceClick('refresh', fetchData); return }
  if ((e.ctrlKey || e.metaKey) && e.key === 'n') { e.preventDefault(); debounceClick('add', handleAdd); return }
}

function handleAdd() {
  // 子页面无需直接新增
}

const handleApprove = (record: any) => {
  console.warn('[付款审批] 审批通过', record)
  message.success(`审批通过: ${record.supplierName}`)
}

const handleReject = (record: any, reason: string) => {
  console.warn('[付款审批] 已拒绝', { record, reason })
  message.success(`已拒绝: ${record.supplierName}，原因: ${reason}`)
}

defineExpose({})

onMounted(() => {
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('finance:create', handleParentCreate)
  window.addEventListener('finance:refresh', fetchData)
})

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('finance:create', handleParentCreate)
  window.removeEventListener('finance:refresh', fetchData)
})
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

/* Compact mode overrides */
:deep(.ant-table-thead > tr > th) {
  padding: 6px 8px !important;
  font-size: 12px;
}
:deep(.ant-table-tbody > tr > td) {
  padding: 4px 8px !important;
  font-size: 12px;
}
:deep(.ant-card-body) {
  padding: 12px;
}
:deep(.ant-form-item) {
  margin-bottom: 8px;
}

/* ── 快捷键提示 ──────────────────────── */
.shortcut-hints {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  user-select: none;
}
.shortcut-hint {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 1px 4px;
  border-radius: 3px;
  background: #f5f7fa;
}
.shortcut-hint kbd {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 3px;
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 11px;
  color: #606266;
  background: #fff;
  border: 1px solid #d0d5dd;
  border-radius: 3px;
  box-shadow: 0 1px 0 #d0d5dd;
  line-height: 18px;
}

/* ── 紧凑尺寸覆盖：28px 输入框 ──────────────────────── */
:deep(.ant-input-sm),
:deep(.ant-input-number-sm),
:deep(.ant-select-single.ant-select-sm .ant-select-selector),
:deep(.ant-picker-small),
:deep(.ant-btn-sm) {
  height: 28px;
  line-height: 28px;
}
:deep(.ant-select-single.ant-select-sm .ant-select-selector) {
  line-height: 26px;
}
:deep(.ant-input-number-sm input) {
  height: 26px;
}

</style>