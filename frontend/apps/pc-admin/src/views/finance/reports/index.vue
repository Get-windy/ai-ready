<template>
  <div class="reports-page">
    <a-card title="财务报表">
      <!-- Tab切换 -->
      <a-tabs v-model:active-key="activeTab">
        <a-tab-pane
          key="balance-sheet"
          tab="资产负债表"
        >
          <BalanceSheet />
        </a-tab-pane>
        <a-tab-pane
          key="profit-statement"
          tab="利润表"
        >
          <ProfitStatement />
        </a-tab-pane>
        <a-tab-pane
          key="cash-flow"
          tab="现金流量表"
        >
          <CashFlowStatement />
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import BalanceSheet from './components/BalanceSheet.vue'
import ProfitStatement from './components/ProfitStatement.vue'
import CashFlowStatement from './components/CashFlowStatement.vue'

const activeTab = ref('balance-sheet')
</script>

<style scoped>
.reports-page {
  padding: 24px;
}
</style>