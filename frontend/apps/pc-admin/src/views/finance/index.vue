<template>
  <div class="finance-module">
    <div class="finance-module-header">
      <div class="finance-module-header-left">
        <h1 class="finance-module-title">财务管理</h1>
      </div>
    </div>

    <a-tabs
      v-model:activeKey="activeTab"
      class="finance-module-tabs"
      @change="handleTabChange"
    >
      <a-tab-pane key="receivable" tab="应收账款">
        <AccountsReceivable />
      </a-tab-pane>
      <a-tab-pane key="payable" tab="应付账款">
        <AccountsPayable />
      </a-tab-pane>
      <a-tab-pane key="reconciliation" tab="对账管理">
        <Reconciliation />
      </a-tab-pane>
      <a-tab-pane key="reports" tab="财务报表">
        <Reports />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AccountsReceivable from './accounts-receivable/index.vue'
import AccountsPayable from './accounts-payable/index.vue'
import Reconciliation from './reconciliation/index.vue'
import Reports from './reports/index.vue'

const router = useRouter()
const route = useRoute()

const VALID_TABS = ['receivable', 'payable', 'reconciliation', 'reports']
const activeTab = ref('receivable')

const handleTabChange = (key: string) => {
  router.replace({ query: { ...route.query, tab: key } })
}

onMounted(() => {
  const tabFromQuery = route.query.tab as string
  if (tabFromQuery && VALID_TABS.includes(tabFromQuery)) {
    activeTab.value = tabFromQuery
  }
})
</script>

<style scoped>
.finance-module {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f0f2f5;
}

.finance-module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: #fff;
  border-bottom: 1px solid #e8e8e8;
}

.finance-module-header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.finance-module-title {
  font-size: 20px;
  font-weight: 500;
  color: #303133;
  margin: 0;
}

.finance-module-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  margin: 16px;
  border-radius: 4px;
}

:deep(.finance-module-tabs .ant-tabs-nav) {
  margin-bottom: 0;
  padding: 0 24px;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.finance-module-tabs .ant-tabs-content) {
  flex: 1;
  overflow: hidden;
}

:deep(.finance-module-tabs .ant-tabs-tabpane) {
  height: 100%;
  overflow: auto;
}
</style>
