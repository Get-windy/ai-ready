<template>
  <div class="sale-module">
    <div class="sale-module-header">
      <div class="sale-module-header-left">
        <h1 class="sale-module-title">销售管理</h1>
      </div>
    </div>

    <a-tabs
      v-model:activeKey="activeTab"
      class="sale-module-tabs"
      @change="handleTabChange"
    >
      <a-tab-pane key="orders" tab="销售订单">
        <OrdersTab />
      </a-tab-pane>
      <a-tab-pane key="quotation" tab="报价单">
        <QuotationTab />
      </a-tab-pane>
      <a-tab-pane key="outbound" tab="出库单">
        <OutboundTab />
      </a-tab-pane>
      <a-tab-pane key="return" tab="退货单">
        <ReturnTab />
      </a-tab-pane>
      <a-tab-pane key="exchange" tab="换货单">
        <ExchangeTab />
      </a-tab-pane>
      <a-tab-pane key="receipt" tab="收款单">
        <ReceiptTab />
      </a-tab-pane>
      <a-tab-pane key="customers" tab="客户">
        <CustomersTab />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import OrdersTab from './tabs/Orders.vue'
import QuotationTab from './tabs/Quotation.vue'
import OutboundTab from './tabs/Outbound.vue'
import ReturnTab from './tabs/Return.vue'
import ExchangeTab from './tabs/Exchange.vue'
import ReceiptTab from './tabs/Receipt.vue'
import CustomersTab from './tabs/Customers.vue'

const router = useRouter()
const route = useRoute()

const VALID_TABS = ['orders', 'quotation', 'outbound', 'return', 'exchange', 'receipt', 'customers']
const activeTab = ref('orders')

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
.sale-module {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f0f2f5;
}

.sale-module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: #fff;
  border-bottom: 1px solid #e8e8e8;
}

.sale-module-header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.sale-module-title {
  font-size: 20px;
  font-weight: 500;
  color: #303133;
  margin: 0;
}

.sale-module-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  margin: 16px;
  border-radius: 4px;
}

:deep(.sale-module-tabs .ant-tabs-nav) {
  margin-bottom: 0;
  padding: 0 24px;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.sale-module-tabs .ant-tabs-content) {
  flex: 1;
  overflow: hidden;
}

:deep(.sale-module-tabs .ant-tabs-tabpane) {
  height: 100%;
  overflow: auto;
}
</style>
