<template>
  <div class="purchase-module">
    <div class="purchase-module-header">
      <div class="purchase-module-header-left">
        <h1 class="purchase-module-title">采购管理</h1>
      </div>
    </div>

    <a-tabs
      v-model:activeKey="activeTab"
      class="purchase-module-tabs"
      @change="handleTabChange"
    >
      <a-tab-pane key="orders" tab="采购订单">
        <OrdersTab />
      </a-tab-pane>
      <a-tab-pane key="inquiry" tab="询价单">
        <InquiryTab />
      </a-tab-pane>
      <a-tab-pane key="inbound" tab="入库单">
        <InboundTab />
      </a-tab-pane>
      <a-tab-pane key="return" tab="退货单">
        <ReturnTab />
      </a-tab-pane>
      <a-tab-pane key="exchange" tab="换货单">
        <ExchangeTab />
      </a-tab-pane>
      <a-tab-pane key="payment" tab="付款单">
        <PaymentTab />
      </a-tab-pane>
      <a-tab-pane key="suppliers" tab="供应商">
        <SuppliersTab />
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import OrdersTab from './tabs/Orders.vue'
import InquiryTab from './tabs/Inquiry.vue'
import InboundTab from './tabs/Inbound.vue'
import ReturnTab from './tabs/Return.vue'
import ExchangeTab from './tabs/Exchange.vue'
import PaymentTab from './tabs/Payment.vue'
import SuppliersTab from './tabs/Suppliers.vue'

const router = useRouter()
const route = useRoute()

const VALID_TABS = ['orders', 'inquiry', 'inbound', 'return', 'exchange', 'payment', 'suppliers']
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
.purchase-module {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f0f2f5;
}

.purchase-module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: #fff;
  border-bottom: 1px solid #e8e8e8;
}

.purchase-module-header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.purchase-module-title {
  font-size: 20px;
  font-weight: 500;
  color: #303133;
  margin: 0;
}

.purchase-module-tabs {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #fff;
  margin: 16px;
  border-radius: 4px;
}

:deep(.purchase-module-tabs .ant-tabs-nav) {
  margin-bottom: 0;
  padding: 0 24px;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
}

:deep(.purchase-module-tabs .ant-tabs-content) {
  flex: 1;
  overflow: hidden;
}

:deep(.purchase-module-tabs .ant-tabs-tabpane) {
  height: 100%;
  overflow: auto;
}
</style>
