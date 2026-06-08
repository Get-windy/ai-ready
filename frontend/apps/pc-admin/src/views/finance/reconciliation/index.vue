<template>
  <div class="reconciliation-page">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card stat-bank">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ stats.bankPending }}</div>
          <div class="stat-card-label">待银行对账</div>
        </div>
        <BankOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-customer">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ stats.customerPending }}</div>
          <div class="stat-card-label">待客户对账</div>
        </div>
        <UserOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-supplier">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ stats.supplierPending }}</div>
          <div class="stat-card-label">待供应商对账</div>
        </div>
        <TeamOutlined class="stat-card-icon" />
      </div>
      <div class="stat-card stat-difference">
        <div class="stat-card-body">
          <div class="stat-card-value">{{ stats.differenceCount }}</div>
          <div class="stat-card-label">差异待处理</div>
        </div>
        <WarningOutlined class="stat-card-icon" />
      </div>
    </div>

    <a-card title="财务对账" :bordered="false">
      <!-- Tab切换 -->
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane
          key="bank"
          tab="银行对账"
        >
          <BankReconciliation />
        </a-tab-pane>
        <a-tab-pane
          key="customer"
          tab="客户对账"
        >
          <CustomerReconciliation />
        </a-tab-pane>
        <a-tab-pane
          key="supplier"
          tab="供应商对账"
        >
          <SupplierReconciliation />
        </a-tab-pane>
        <a-tab-pane
          key="difference"
          tab="差异处理"
        >
          <DifferenceHandling />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { BankOutlined, UserOutlined, TeamOutlined, WarningOutlined } from '@ant-design/icons-vue'
import BankReconciliation from './components/BankReconciliation.vue'
import CustomerReconciliation from './components/CustomerReconciliation.vue'
import SupplierReconciliation from './components/SupplierReconciliation.vue'
import DifferenceHandling from './components/DifferenceHandling.vue'

const activeTab = ref('bank')

const stats = reactive({
  bankPending: 12,
  customerPending: 8,
  supplierPending: 5,
  differenceCount: 3
})

onMounted(() => {
  // 可以在这里加载统计数据
})
</script>

<style scoped>
.reconciliation-page {
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

.stat-bank { background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%); }
.stat-customer { background: linear-gradient(135deg, #f6ffed 0%, #d9f7be 100%); }
.stat-supplier { background: linear-gradient(135deg, #fff7e6 0%, #ffe7ba 100%); }
.stat-difference { background: linear-gradient(135deg, #fff1f0 0%, #ffccc7 100%); }

.stat-card-value {
  font-size: 18px;
  font-weight: 600;
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