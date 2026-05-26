<template>
  <div class="reconciliation-page">
    <a-card title="财务对账">
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
import { ref } from 'vue'
import BankReconciliation from './components/BankReconciliation.vue'
import CustomerReconciliation from './components/CustomerReconciliation.vue'
import SupplierReconciliation from './components/SupplierReconciliation.vue'
import DifferenceHandling from './components/DifferenceHandling.vue'

const activeTab = ref('bank')
</script>

<style scoped>
.reconciliation-page {
  padding: 24px;
}
</style>